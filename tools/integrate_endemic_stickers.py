#!/usr/bin/env python3
"""Integrate the generated 12-species endemic sticker expansion."""
import io, json, re
from pathlib import Path
from PIL import Image
from rembg import remove

ROOT=Path(__file__).resolve().parents[1]
ANDROID=ROOT/'android'
CAND=ROOT/'.hermes/endemic-sticker-candidates'
DRAW=ANDROID/'feature-rewards/src/main/res/drawable-nodpi'
PHOTOS=ANDROID/'app/src/main/assets/badge_photos.json'
CATALOG=ANDROID/'app/src/main/assets/badge_catalog.json'
ART=ANDROID/'feature-rewards/src/main/java/com/maxinesworld/featurerewards/BadgePhotoArtwork.kt'
WILD=ANDROID/'feature-rewards/src/main/java/com/maxinesworld/featurerewards/WildlifeHabitatAffinity.kt'

S=[
 dict(id='mammal_palawan_stink_badger',name='Palawan stink badger',fil='Pantot',biome='forest_friends',rarity='rare',title='Stripe-Tailed Snuffler',fact='The Palawan stink badger is found only on Palawan and nearby islands and searches the forest floor for worms and insects!',factFil='Sa Palawan at mga kalapit na isla lamang matatagpuan ang pantot; naghahanap ito ng bulate at insekto sa sahig ng gubat.',behavior='stripe-tailed forest snuffler',period='NOCTURNAL',zone='EMERALD_FOREST_MEADOW',primary='sanctuary-meadow',alternate='sanctuary-path',emoji='🦨'),
 dict(id='mammal_dinagat_gymnure',name='Dinagat gymnure',fil='Moonrat ng Dinagat',biome='forest_friends',rarity='legendary',title='Moon-Nosed Explorer',fact='This long-snouted relative of hedgehogs is known only from a few Philippine islands, including Dinagat!',factFil='Ang mahabang-ngusong kamag-anak ng hedgehog na ito ay kilala lamang sa ilang isla ng Pilipinas, kabilang ang Dinagat.',behavior='long-nosed leaf-litter explorer',period='NOCTURNAL',zone='EMERALD_FOREST_MEADOW',primary='sanctuary-meadow',alternate='sanctuary-path',emoji='🐀'),
 dict(id='mammal_negros_fruit_bat',name='Negros naked-backed fruit bat',fil='Paniki ng Negros',biome='forest_friends',rarity='legendary',title='Negros Night Gardener',fact='By carrying fruit and seeds through the night forest, this rare bat helps new trees grow!',factFil='Sa pagdadala ng prutas at mga buto sa gabi, tinutulungan ng bihirang paniking ito na tumubo ang mga bagong puno.',behavior='nighttime seed gardener',period='NOCTURNAL',zone='EMERALD_FOREST_MEADOW',primary='sanctuary-meadow',alternate='sanctuary-path',emoji='🦇'),
 dict(id='mammal_panay_cloudrunner',name='Panay bushy-tailed cloud rat',fil='Dagang-ulap ng Panay',biome='forest_friends',rarity='legendary',title='Cloud-Forest Climber',fact='This bushy-tailed cloud rat lives high in the mountain forests of Panay and was described by scientists only in 1996!',factFil='Naninirahan ang mabalahibong dagang-ulap na ito sa matataas na kagubatan ng Panay at inilarawan lamang ng mga siyentista noong 1996.',behavior='bushy-tailed cloud climber',period='NOCTURNAL',zone='HIGH_CANOPY_CLOUD_SUMMIT',primary='sanctuary-tree',alternate='sanctuary-nest',emoji='🐀'),
 dict(id='mammal_tube_nosed_fruit_bat',name='Philippine tube-nosed fruit bat',fil='Tubong-ilong na paniki',biome='forest_friends',rarity='rare',title='Tube-Nosed Pollinator',fact='Its unusual tube-shaped nostrils help it breathe while its face is buried in juicy fruit!',factFil='Tinutulungan ito ng kakaibang tubong-ilong na huminga habang nakasubsob ang mukha sa makatas na prutas.',behavior='tube-nosed fruit pollinator',period='NOCTURNAL',zone='HIGH_CANOPY_CLOUD_SUMMIT',primary='sanctuary-tree',alternate='sanctuary-nest',emoji='🦇'),
 dict(id='bird_sulu_hornbill',name='Sulu hornbill',fil='Tariktik ng Sulu',biome='sky_scouts',rarity='legendary',title='Sulu Forest Trumpet',fact='One of the rarest hornbills in the world, it survives only on the Philippine island of Tawi-Tawi!',factFil='Isa ito sa pinakabihirang hornbill sa mundo at nabubuhay lamang sa isla ng Tawi-Tawi sa Pilipinas.',behavior='island forest trumpeter',period='DIURNAL',zone='HIGH_CANOPY_CLOUD_SUMMIT',primary='sanctuary-lookout',alternate='sanctuary-nest',emoji='🐦'),
 dict(id='bird_apo_myna',name='Apo myna',fil='Maya ng Apo',biome='songbird_grove',rarity='rare',title='Mountain Whistler',fact='This glossy mountain bird lives in high forests of Mindanao and chats with whistles, clicks, and calls!',factFil='Naninirahan ang makintab na ibong ito sa matataas na kagubatan ng Mindanao at nakikipag-usap sa sipol, klik, at tawag.',behavior='yellow-eyed mountain whistler',period='DIURNAL',zone='HIGH_CANOPY_CLOUD_SUMMIT',primary='sanctuary-lookout',alternate='sanctuary-nest',emoji='🐦'),
 dict(id='bird_luzon_water_redstart',name='Luzon water redstart',fil='Redstart ng Luzon',biome='songbird_grove',rarity='rare',title='River-Tail Dancer',fact='This little red-bellied bird bobs its tail beside clear, rushing mountain streams found only on Luzon!',factFil='Iwinawagayway ng munting ibong mapulang-tiyan ang buntot nito sa tabi ng malinaw at rumaragasang batis sa Luzon.',behavior='river-rock tail dancer',period='DIURNAL',zone='CORAL_LAGOON_FRESHWATER_STREAM',primary='sanctuary-pond',alternate='sanctuary-wildlife-sign',emoji='🐦'),
 dict(id='bird_palawan_flycatcher',name='Palawan flycatcher',fil='Tagahuli ng langaw ng Palawan',biome='songbird_grove',rarity='rare',title='Palawan Bug Catcher',fact='Quiet and well hidden, this small bird waits on low branches before darting out to catch insects!',factFil='Tahimik at mahusay magtago ang munting ibong ito; naghihintay ito sa mababang sanga bago biglang humuli ng insekto.',behavior='low-branch bug catcher',period='DIURNAL',zone='HIGH_CANOPY_CLOUD_SUMMIT',primary='sanctuary-lookout',alternate='sanctuary-nest',emoji='🐦'),
 dict(id='reptile_polillo_forest_dragon',name='Polillo forest dragon',fil='Butiking-gubat ng Polillo',biome='river_guardians',rarity='legendary',title='Emerald Forest Dragon',fact='This green lizard blends into leafy branches on Polillo and nearby eastern Luzon islands!',factFil='Sumasama sa kulay ng madahong sanga ang berdeng butiking ito sa Polillo at mga kalapit na isla sa silangang Luzon.',behavior='emerald branch hider',period='DIURNAL',zone='CORAL_LAGOON_FRESHWATER_STREAM',primary='sanctuary-wildlife-sign',alternate='sanctuary-shelter',emoji='🦎'),
 dict(id='reptile_cebu_small_worm_skink',name='Cebu small worm skink',fil='Munting skink ng Cebu',biome='river_guardians',rarity='legendary',title='Tiny Soil Swimmer',fact='This tiny legless skink moves through loose soil and leaf litter almost like a worm swimming underground!',factFil='Gumagalaw ang munting skink na walang paa sa maluwag na lupa at tuyong dahon na parang bulateng lumalangoy sa ilalim.',behavior='tiny soil swimmer',period='CREPUSCULAR',zone='EMERALD_FOREST_MEADOW',primary='sanctuary-shelter',alternate='sanctuary-path',emoji='🦎'),
 dict(id='amphibian_palawan_horned_frog',name='Palawan horned frog',fil='Sungayang palaka ng Palawan',biome='creek_coral',rarity='rare',title='Leaf-Look Horned Frog',fact='Pointed eyelids and leaf-brown colors help this Palawan frog disappear among fallen leaves!',factFil='Tinutulungan ng matulis na talukap at kulay-kayumangging parang dahon ang palakang ito na maglaho sa tuyong dahon.',behavior='leaf-litter camouflage hopper',period='NOCTURNAL',zone='CORAL_LAGOON_FRESHWATER_STREAM',primary='sanctuary-pond',alternate='sanctuary-shelter',emoji='🐸'),
]

# Background removal, transparent trim/pad, and lossless 256 WebP.
for s in S:
    src=CAND/(s['id']+'.png')
    removed = remove(src.read_bytes())
    if not isinstance(removed, bytes):
        raise TypeError(f'background remover returned {type(removed).__name__}, expected bytes')
    rgba=Image.open(io.BytesIO(removed)).convert('RGBA')
    a=rgba.getchannel('A'); box=a.getbbox()
    if not box: raise RuntimeError(f'empty cutout: {src}')
    obj=rgba.crop(box)
    obj.thumbnail((218,218),Image.Resampling.LANCZOS)
    canvas=Image.new('RGBA',(256,256),(0,0,0,0))
    canvas.alpha_composite(obj,((256-obj.width)//2,(256-obj.height)//2))
    canvas.save(DRAW/f"animal_photo_{s['id']}.webp",'WEBP',lossless=True,method=6)

catalog=json.loads(CATALOG.read_text())
known={x['id'] for x in catalog}
for s in S:
    if s['id'] not in known:
        catalog.append({k:s[k] for k in ('id','biome','name','title','fact','emoji')} | {'fun_fact':s['fact'],'rarity':s['rarity']})
        catalog[-1].pop('fact')
CATALOG.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n')

photos=json.loads(PHOTOS.read_text())
known={x['badge_id'] for x in photos}
for s in S:
    if s['id'] not in known:
        photos.append({'badge_id':s['id'],'asset':'animal_photo_'+s['id'],'provider':'ComfyUI Local Generative Engine (RTX 3070)','source_url':'https://github.com/ronrosolada/maxines-world','source_title':s['name']+' storybook illustration','credit':"Maxine's World Art Team & ComfyUI Engine",'license':'Proprietary / In-App Educational Asset','kind':'ai_storybook_sticker'})
PHOTOS.write_text(json.dumps(photos,ensure_ascii=False,indent=2)+'\n')

art=ART.read_text(); marker=')\n\n@Composable'
lines=''.join(f'    "animal_photo_{s["id"]}" to R.drawable.animal_photo_{s["id"]},\n' for s in S)
if lines.strip().splitlines()[0].strip() not in art:
    art=art.replace(marker,lines+')\n\n@Composable')
ART.write_text(art)

wild=WILD.read_text(); marker='        species("badge_milestone_first_steps"'
lines=''
for s in S:
    q=lambda x: json.dumps(x,ensure_ascii=False)
    lines+=f'        species("badge_{s["id"]}", "animal_photo_{s["id"]}", {q(s["name"])}, {q(s["fil"])}, "{s["primary"]}", "{s["alternate"]}", "Philippine endemic wildlife habitat", "Tirahan ng katutubong hayop sa Pilipinas", {q(s["fact"])}, {q(s["factFil"])}, {q(s["behavior"])}, WildlifeActivityPeriod.{s["period"]}, SanctuaryBiomeZone.{s["zone"]}),\n'
if f'"badge_{S[0]["id"]}"' not in wild:
    wild=wild.replace(marker,lines+marker).replace('require(species.size == 51)',f'require(species.size == {51+len(S)})').replace('all 51 badges',f'all {51+len(S)} badges')
WILD.write_text(wild)
print(f'Integrated {len(S)} stickers; catalog now {len(catalog)} entries')

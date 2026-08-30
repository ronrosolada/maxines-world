#!/usr/bin/env python3
"""Generate deterministic Maxine's World endemic-animal stickers via local ComfyUI."""
import json, time, urllib.parse, urllib.request
from pathlib import Path

HOST = "http://10.10.10.5:8188"
OUT = Path(".hermes/endemic-sticker-candidates")
SPECIES = {
    "mammal_palawan_stink_badger": (61001, "Palawan stink badger, small black mammal with a crisp white stripe running from crown down its back, short legs, fluffy tail"),
    "mammal_dinagat_gymnure": (61002, "Dinagat gymnure moonrat, small long-snouted shrew-like mammal, coarse golden-brown fur, pale face, long nearly hairless tail"),
    "mammal_negros_fruit_bat": (61003, "Negros naked-backed fruit bat, warm brown fruit bat with broad dark wings, fox-like muzzle, wings spread symmetrically"),
    "mammal_panay_cloudrunner": (61004, "Panay bushy-tailed cloud rat, large soft gray-brown forest rodent, rounded ears, very long bushy tail, pale belly"),
    "mammal_tube_nosed_fruit_bat": (61005, "Philippine tube-nosed fruit bat, tiny brown fruit bat with distinctive tubular nostrils, cream ear spots, wings spread"),
    "bird_sulu_hornbill": (61006, "Sulu hornbill, glossy black hornbill with long black tail, large ivory bill and casque, white tail tip"),
    "bird_apo_myna": (61007, "Apo myna, glossy black mountain starling with bright yellow bare eye patch, yellow bill and feet, neat crest"),
    "bird_luzon_water_redstart": (61008, "Luzon water redstart, small slate-blue river bird with bright rusty-red belly and tail"),
    "bird_palawan_flycatcher": (61009, "Palawan flycatcher, tiny warm rufous-brown forest bird with gray head, pale throat and delicate bill"),
    "reptile_polillo_forest_dragon": (61010, "Polillo forest dragon, slender emerald green arboreal lizard with textured scales, subtle back crest and very long curling tail"),
    "reptile_cebu_small_worm_skink": (61011, "Cebu small worm skink, tiny glossy bronze-brown legless lizard, gently coiled, small friendly face"),
    "amphibian_palawan_horned_frog": (61012, "Palawan horned frog, squat leaf-brown frog with pointed horn-like eyelids, mottled tan markings and wide friendly eyes"),
}
NEG = "photograph, photorealistic, 3d, CGI, vector art, flat fill, harsh outline, white outline, halo, border, scenery, habitat background, props, text, logo, watermark, extra animal, duplicate, cropped body, missing feet, deformed, frightening, aggressive, blurry"

def workflow(desc, seed, prefix):
    positive = f"one {desc}, full body centered, all extremities visible, compact cute chibi proportions, friendly oversized glossy eyes with catchlights, soft hand-painted children's storybook illustration, subtle painterly brush texture, muted Philippine forest earth tones with selective saturation, clean silhouette, isolated on plain pure white staging background, sticker asset, square composition, subject fills 75 percent of canvas, educational and child-friendly, high quality"
    return {
      "3":{"inputs":{"seed":seed,"steps":28,"cfg":7.0,"sampler_name":"dpmpp_2m","scheduler":"karras","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]},"class_type":"KSampler"},
      "4":{"inputs":{"ckpt_name":"dreamshaper_8.safetensors"},"class_type":"CheckpointLoaderSimple"},
      "5":{"inputs":{"width":512,"height":512,"batch_size":1},"class_type":"EmptyLatentImage"},
      "6":{"inputs":{"text":positive,"clip":["4",1]},"class_type":"CLIPTextEncode"},
      "7":{"inputs":{"text":NEG,"clip":["4",1]},"class_type":"CLIPTextEncode"},
      "8":{"inputs":{"samples":["3",0],"vae":["4",2]},"class_type":"VAEDecode"},
      "9":{"inputs":{"filename_prefix":prefix,"images":["8",0]},"class_type":"SaveImage"}}

def generate(ids):
    OUT.mkdir(parents=True, exist_ok=True)
    for sid in ids:
        seed, desc = SPECIES[sid]
        req=urllib.request.Request(HOST+"/prompt", data=json.dumps({"prompt":workflow(desc,seed,"endemic/"+sid)}).encode(), headers={"Content-Type":"application/json"})
        pid=json.load(urllib.request.urlopen(req))["prompt_id"]
        while True:
            h=json.load(urllib.request.urlopen(HOST+"/history/"+pid))
            if pid in h: break
            time.sleep(2)
        imgs=[]
        for output in h[pid]["outputs"].values(): imgs += output.get("images",[])
        if not imgs: raise RuntimeError(f"No output for {sid}: {h}")
        im=imgs[0]; q=urllib.parse.urlencode(im)
        (OUT/(sid+".png")).write_bytes(urllib.request.urlopen(HOST+"/view?"+q).read())
        print(sid, pid, im["filename"])

if __name__ == "__main__":
    import sys
    generate(sys.argv[1:] or list(SPECIES))

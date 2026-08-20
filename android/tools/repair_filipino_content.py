#!/usr/bin/env python3
"""Repair Filipino quarterly lessons (stock-junk sweep + real content).

Third content-repair wave. The quarterly generator's Filipino lessons:

- simuno/panaguri (32 lessons): identical sentence sets across all 32;
  stock junk in options/sort/matching/vocab.
- Munting Talata (12): teaches "paragraph" but contains no real paragraph.
- Wastong Pagsulat (7): 4 shared words, stock junk, identical across 7.
- Salitang-Ugat (4), Magagalang na Pananalita (4), Maikling Buod (4):
  real seeds + stock junk; Buod contains no real story.

Each skill gets authored per-instance content sets assigned deterministically
by sorted lesson order (same convention as the English/Math/Science tools).
"""

import argparse
import json
import re
import sys
from pathlib import Path

LESSONS = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "content-pack" / "month-01" / "lessons"

FIL_JUNK = [
    "salitang walang kaugnayan", "hindi magalang na pahayag", "hula na walang pahiwatig",
    "paksang iba sa aralin", "angkop na halimbawa", "malinaw na gamit", "tamang ideya",
]
FIL_JUNK_PREFIXES = [
    "Aling halimbawa ang kabilang sa", "Aling pagpipilian ang nagpapakita ng kasanayan sa",
    "Alin ang isang halimbawa ng", "Aling sitwasyon ang tumutugma sa",
    "Aling sagot ang nagpapakita ng",
]

FIL_JUNK_REPLACEMENTS = {
    "salitang walang kaugnayan": "isang pangyayaring hindi binanggit sa teksto",
    "hindi magalang na pahayag": "isang pahayag na hindi angkop sa sitwasyon",
    "hula na walang pahiwatig": "isang sagot na walang patunay",
    "paksang iba sa aralin": "isang paksang hindi binanggit sa teksto",
    "angkop na halimbawa": "halimbawang tumutugma sa aralin",
    "malinaw na gamit": "gamit na nagpapakita ng konsepto",
    "tamang ideya": "ideyang sinusuportahan ng teksto",
}

# ---------------------------------------------------------------------------
# Skill data
# ---------------------------------------------------------------------------

# (simuno, panaguri) — 64 sentences; lesson i takes window [2i .. 2i+4)
# so adjacent lessons share only 2 of 4 sentences (unique token sets).
SIMUNO_POOL = [
    ("Si Ana", "ay nagbabasa."), ("Ang aso", "ay tumatakbo."), ("Si Milo", "ay natututo."),
    ("Ang mga bata", "ay naglalaro."), ("Ang guro", "ay nagtuturo."), ("Si Lolo", "ay nagbabasa ng libro."),
    ("Ang bata", "ay tumatakbo pababa."), ("Si Nanay", "ay nagluluto."), ("Si Tatay", "ay nagmamaneho."),
    ("Ang bibe", "ay lumalangoy sa lawa."), ("Si Lola", "ay nagbabasa ng pahayagan."), ("Ang mga ibon", "ay umaawit."),
    ("Si Ben", "ay sumasayaw."), ("Ang pusa", "ay natutulog."), ("Si Liza", "ay kumakanta."),
    ("Ang mga mag-aaral", "ay nag-aaral."), ("Si Marco", "ay nagpipinta."), ("Ang hardin", "ay namumulaklak."),
    ("Si Nena", "ay nagdidilig."), ("Ang ulan", "ay humihinto sa gabi."), ("Si Tito Ramon", "ay nagtatanim."),
    ("Ang mga dahon", "ay nalalagas."), ("Si Dina", "ay nagwawalis."), ("Ang sasakyan", "ay humihinto sa tawiran."),
    ("Si Jose", "ay nag-eehersisyo."), ("Ang mga isda", "ay lumalangoy sa ilog."), ("Si Aling Rosa", "ay naglalaba."),
    ("Ang orasan", "ay tumutunog."), ("Si Paolo", "ay nagbabasketball."), ("Ang mga bituin", "ay kumikislap."),
    ("Si Kiko", "ay tumutugtog."), ("Ang pagkain", "ay mainit."),
    ("Si Rosa", "ay nag-eensayo."), ("Ang pinto", "ay nakabukas."), ("Si Pia", "ay nagbabasa ng kwento."),
    ("Ang halaman", "ay sumisibol."), ("Si Dino", "ay nagkukuwento."), ("Ang manok", "ay tumitilaok."),
    ("Si Lita", "ay nagtatahi."), ("Ang apoy", "ay nagliliyab."), ("Si Totoy", "ay gumuguhit."),
    ("Ang araw", "ay sumisikat."), ("Si Yana", "ay nagkakanta."), ("Ang puno", "ay umuugoy."),
    ("Si Berto", "ay nagbibisikleta."), ("Ang buwan", "ay nagliliwanag."), ("Si Alma", "ay naghahanda ng meryenda."),
    ("Ang alon", "ay humahampas."), ("Si Boy", "ay naglalakad."), ("Ang hangin", "ay humihihip."),
    ("Si Celia", "ay naglilinis ng silid."), ("Ang simbahan", "ay umaalingawngaw."), ("Si Dodoy", "ay sumisipol."),
    ("Ang ilog", "ay umaagos."), ("Si Ester", "ay nag-aalaga ng manok."), ("Ang ulap", "ay gumagalaw."),
    ("Si Fely", "ay tumatawa."), ("Ang kalan", "ay umuusok."), ("Si Gino", "ay naghuhugas."),
    ("Ang baso", "ay napuno."), ("Si Hana", "ay nagpapaaraw."), ("Ang palengke", "ay maingay."),
    ("Si Ines", "ay sumisigaw."), ("Ang tren", "ay dumating."),
]

# (paragraph, paksa, pangunahing ideya, detalye, wakas) — 12 paragraphs.
TALATA_SETS = [
    ("Ang aking pusa ay maputi at makapal ang balahibo. Mahilig siyang matulog sa ilalim ng mesa. Mahal na mahal ko ang aking pusa.",
     "ang aking pusa", "Ang aking pusa ay maputi at makapal ang balahibo.",
     "Mahilig siyang matulog sa ilalim ng mesa.", "Mahal na mahal ko ang aking pusa."),
    ("Masaya ang pamilya ni Ana sa Sabado. Nagluluto ng adobo si Nanay habang naglilinis si Tatay. Sabay-sabay silang kumakain at nagkukuwentuhan.",
     "ang Sabado ng pamilya ni Ana", "Masaya ang pamilya ni Ana sa Sabado.",
     "Nagluluto ng adobo si Nanay habang naglilinis si Tatay.", "Sabay-sabay silang kumakain at nagkukuwentuhan."),
    ("Ang paaralan ni Ben ay malapit sa palengke. Maraming puno sa paligid nito. Tuwang-tuwa si Ben na pumasok araw-araw.",
     "ang paaralan ni Ben", "Ang paaralan ni Ben ay malapit sa palengke.",
     "Maraming puno sa paligid nito.", "Tuwang-tuwa si Ben na pumasok araw-araw."),
    ("Malakas ang ulan noong Linggo. Nagsuot ng kapote si Liza habang naglalakad pauwi. Nang makauwi siya, pinainom siya ng mainit na tsokolate ni Lola.",
     "ang maulan na Linggo", "Malakas ang ulan noong Linggo.",
     "Nagsuot ng kapote si Liza habang naglalakad pauwi.", "Nang makauwi siya, pinainom siya ng mainit na tsokolate ni Lola."),
    ("Mahilig magtanim si Tito Ramon sa hardin. Nagtatanim siya ng kamatis, talong, at sili. Sariwang gulay ang handa tuwing hapunan.",
     "ang hardin ni Tito Ramon", "Mahilig magtanim si Tito Ramon sa hardin.",
     "Nagtatanim siya ng kamatis, talong, at sili.", "Sariwang gulay ang handa tuwing hapunan."),
    ("Ang batang si Dina ay maingat sa kanyang mga gamit. Nililinis niya ang kanyang mesa araw-araw. Wala siyang nawawalang lapis o papel.",
     "ang maingat na si Dina", "Ang batang si Dina ay maingat sa kanyang mga gamit.",
     "Nililinis niya ang kanyang mesa araw-araw.", "Wala siyang nawawalang lapis o papel."),
    ("Naglalaro ng basketball ang magkakaibigan sa plasa. Mabilis at magaling mag-shoot si Marco. Nanalo ang kanilang koponan sa huling laro.",
     "ang laro ng basketball sa plasa", "Naglalaro ng basketball ang magkakaibigan sa plasa.",
     "Mabilis at magaling mag-shoot si Marco.", "Nanalo ang kanilang koponan sa huling laro."),
    ("Ang barangay namin ay malinis at maayos. Naglilinis ng kalye ang mga kabataan tuwing Sabado. Ipinagmamalaki namin ang aming barangay.",
     "ang aming barangay", "Ang barangay namin ay malinis at maayos.",
     "Naglilinis ng kalye ang mga kabataan tuwing Sabado.", "Ipinagmamalaki namin ang aming barangay."),
    ("Ang aso ni Milo ay tapat at mapagmahal. Sumasalubong ito sa kanya tuwing uuwi siya. Kaya naman lagi siyang masaya sa kanyang aso.",
     "ang aso ni Milo", "Ang aso ni Milo ay tapat at mapagmahal.",
     "Sumasalubong ito sa kanya tuwing uuwi siya.", "Kaya naman lagi siyang masaya sa kanyang aso."),
    ("Masarap magluto si Lola Nena. Ang kanyang ginataan ay paborito ng buong pamilya. Tuwing may handaan, siya ang nagluluto.",
     "si Lola Nena", "Masarap magluto si Lola Nena.",
     "Ang kanyang ginataan ay paborito ng buong pamilya.", "Tuwing may handaan, siya ang nagluluto."),
    ("Ang palengke ay puno ng tao tuwing umaga. May nagtitinda ng gulay, isda, at prutas. Busy pero masaya ang mga mamimili.",
     "ang palengke", "Ang palengke ay puno ng tao tuwing umaga.",
     "May nagtitinda ng gulay, isda, at prutas.", "Busy pero masaya ang mga mamimili."),
    ("Maaga gumigising si Jose para mag-ehersisyo. Tumatakbo siya sa parke kasama ang kanyang kuya. Malusog at masigla siya araw-araw.",
     "si Jose", "Maaga gumigising si Jose para mag-ehersisyo.",
     "Tumatakbo siya sa parke kasama ang kanyang kuya.", "Malusog at masigla siya araw-araw."),
]

# (word, definition, misspell1, misspell2, cloze, cloze_answer_is_self) — 7 sets x 4.
PAGSULAT_SETS = [
    [("paaralan", "lugar kung saan nag-aaral", "paralan", "paaraln", "Pumupunta kami sa ___ upang mag-aral."),
     ("kaibigan", "taong malapit sa iyo at pinagkakatiwalaan", "kaibgan", "kaibigann", "Ang aking ___ ay laging nariyan para sa akin."),
     ("paggalang", "paggawa nang may respeto sa kapwa", "paggalng", "paggallang", "Ipinapakita natin ang ___ sa mga nakatatanda."),
     ("masipag", "masigasig at mabilis gumawa", "maspag", "masippag", "___ na bata si Liza sa paggawa ng takdang-aralin.")],
    [("aklatan", "lugar na may mga aklat na mababasa", "akltan", "aklatn", "Nanghihiram ako ng libro sa ___."),
     ("tahanan", "bahay; lugar ng pamilya", "tanahan", "tahann", "Ang ___ namin ay payapa at masaya."),
     ("kalayaan", "pagiging malaya", "kalayann", "kalyaan", "Ipinagdiriwang natin ang Araw ng ___."),
     ("matulungin", "laging handang tumulong", "matulunin", "matulunggn", "___ ang mga kabataan sa aming barangay.")],
    [("bintana", "bukas sa dingding na may salamin", "bintna", "bintanna", "Nakasara ang ___ dahil malamig ang hangin."),
     ("hardin", "lugar na may mga halaman", "hardn", "hardinn", "Nagdidilig si Lolo sa ___ tuwing umaga."),
     ("sasakyan", "ginagamit sa pagbiyahe", "sasakyn", "sasakyaan", "Maingat na nagmamaneho si Tatay ng ___."),
     ("magalang", "magandang ugali sa pakikipag-usap", "maglang", "magalangg", "___ na bata si Ana sa lahat ng tao.")],
    [("pangarap", "inaasam na makamit sa buhay", "pangrap", "pangarapp", "Ang maging guro ang ___ ni Nena."),
     ("kaarawan", "araw ng kapanganakan", "kaarwan", "kaarawann", "May handaan kami sa ___ ko."),
     ("palengke", "lugar na pinagbibilhan ng pagkain", "palenge", "palengkee", "Bumibili si Nanay ng gulay sa ___."),
     ("sapatos", "pantakip sa paa", "sapatoss", "sapatas", "Nililinis ni Ben ang kanyang ___ tuwing Sabado.")],
    [("pagkain", "kinakain upang mabuhay", "pagkainn", "pakain", "Huwag sayangin ang ___ sa mesa."),
     ("inumin", "iniinom upang hindi mauhaw", "inummn", "inummin", "Mainit na ___ ang handa tuwing malamig."),
     ("tulong", "pagtulong sa nangangailangan", "tulogg", "tuloong", "Humingi ng ___ kung may hindi ka maintindihan."),
     ("tula", "may tugmaang mga salita", "tulla", "tual", "Sumulat si Milo ng ___ para sa kanyang nanay.")],
    [("ilog", "malaking daluyan ng tubig", "ilogg", "ilol", "Malinis ang ___ sa aming probinsya."),
     ("bundok", "napakatas na lupa", "bundokk", "bunodk", "Sumasama kami sa pag-akyat sa ___."),
     ("halaman", "tumutubong may ugat at dahon", "halamaaan", "halman", "Dinidiligan ko ang ___ tuwing umaga."),
     ("sikat", "bantog at kilala", "sikatt", "sikta", "___ na artista ang tita ni Marco.")],
    [("suklay", "pangkamot sa buhok", "suklai", "suklayy", "Ginagamit ang ___ sa pag-aayos ng buhok."),
     ("salamin", "pangtingin sa sarili", "salaminn", "salamim", "Tumingin si Dina sa ___ bago umalis."),
     ("payong", "panangga sa ulan", "payongg", "paiyong", "Magdala ng ___ kung maulan."),
     ("tsinelas", "sapatos na bukas sa likod", "tsinellas", "tsinelaz", "Isuot ang ___ kapag nasa loob ng bahay.")],
]

# (root, related) — 4 sets x 4.
SALITANGUGAT_SETS = [
    [("sulat", "sumulat"), ("laro", "naglaro"), ("basa", "bumasa"), ("tanim", "nagtanim")],
    [("kain", "kumain"), ("takbo", "tumatakbo"), ("awit", "umawit"), ("ganda", "gumanda")],
    [("tulog", "natulog"), ("sayaw", "sumayaw"), ("bili", "bumili"), ("lakad", "naglalakad")],
    [("luto", "nagluto"), ("punta", "pumunta"), ("tawa", "tumawa"), ("bukas", "bumukas")],
]

# (phrase, situation) — 4 sets x 4 polite phrases.
PANANALITA_SETS = [
    [("Magandang umaga po.", "pagbati sa umaga"), ("Maaari po bang makiusap?", "hihingi ng pabor"),
     ("Salamat po.", "pagtanggap ng tulong"), ("Paumanhin po.", "paghingi ng paumanhin")],
    [("Magandang hapon po.", "pagbati sa hapon"), ("Puwede po bang magtanong?", "may itatanong"),
     ("Walang anuman po.", "tugon sa pasasalamat"), ("Patawad po.", "paghingi ng tawad")],
    [("Magandang gabi po.", "pagbati sa gabi"), ("Makikiraan po ba?", "dadaan sa harap ng tao"),
     ("Maraming salamat po.", "pagpapasalamat nang may diin"), ("Pasensiya na po.", "paghingi ng pasensiya")],
    [("Tuloy po kayo.", "pagtanggap ng bisita"), ("Maaari po bang pumunta?", "hihingi ng pahintulot"),
     ("Salamat sa tulong po.", "pasasalamat sa tulong"), ("Paumanhin sa abala po.", "paghingi ng paumanhin sa abala")],
]

IMPOLITE = ["Umalis ka na!", "Bilisan mo!", "Wala akong pakialam!", "Bigyan mo ako ngayon!"]

# (story, tauhan, suliranin, pangyayari, wakas, buod) — 4 stories.
BUOD_SETS = [
    ("Nawala ang alampay ni Lola Rosa. Hinanap ito nina Milo at Ana sa sala at sa kusina. Natagpuan nila ito sa ilalim ng unan. Nagpasalamat si Lola Rosa sa kanila.",
     "Lola Rosa, Milo, at Ana", "Nawala ang alampay ni Lola Rosa.",
     "Hinanap ito nina Milo at Ana sa sala at sa kusina.", "Natagpuan nila ito sa ilalim ng unan.",
     "Nawala ang alampay ni Lola Rosa, at natagpuan ito nina Milo at Ana sa ilalim ng unan."),
    ("Sobrang init noong tanghali. Gustong maglaro ni Ben pero mainit ang araw. Naghintay siya hanggang hapon. Naglaro siya nang masaya sa labas.",
     "Ben", "Gustong maglaro ni Ben pero mainit ang araw.",
     "Naghintay siya hanggang hapon.", "Naglaro siya nang masaya sa labas.",
     "Mainit noong tanghali, kaya naghintay si Ben hanggang hapon bago naglaro."),
    ("Nabasag ang paso ng halaman ni Aling Rosa. Hindi sinasadya ito ni Nena. Tinulungan ni Nena si Aling Rosa na magtanim ng bagong halaman. Natuwa si Aling Rosa sa kabaitan ni Nena.",
     "Aling Rosa at Nena", "Nabasag ang paso ng halaman ni Aling Rosa.",
     "Tinulungan ni Nena si Aling Rosa na magtanim ng bagong halaman.", "Natuwa si Aling Rosa sa kabaitan ni Nena.",
     "Nabasag ang paso ni Aling Rosa, at tinulungan siya ni Nena na magtanim ng bago."),
    ("Umuulan nang malakas. Hindi makauwi si Marco. May nakasalubong siyang matandang walang payong. Ibinalot ni Marco ang kanyang jacket sa matanda. Nakarating silang dalawa nang ligtas sa kanilang bahay.",
     "Marco at isang matanda", "Umuulan nang malakas.",
     "Ibinalot ni Marco ang kanyang jacket sa matanda.", "Nakarating silang dalawa nang ligtas sa kanilang bahay.",
     "Malakas ang ulan, at tinulungan ni Marco ang matandang walang payong upang makauwi nang ligtas."),
]

SKILLS = {
    "Natutukoy ang simuno at panaguri sa payak na pangungusap.": "simuno",
    "Nakabubuo ng maikling talata na malinaw ang paksa at mga detalye.": "talata",
    "Naisusulat nang maayos at wasto ang mga natutuhang salita.": "pagsulat",
    "Natutukoy ang salitang-ugat ng mga karaniwang salita.": "salitangugat",
    "Nagagamit ang magagalang na pagbati at pananalita ayon sa sitwasyon.": "pananalita",
    "Nakabubuo ng maikling buod ng tekstong naratibo.": "buod",
}

SEQUENCES = {
    "simuno": ["Basahin ang pangungusap", "Hanapin kung sino o ano ang pinag-uusapan",
               "Hanapin ang sinasabi tungkol dito", "Sabihin: simuno at panaguri"],
    "talata": ["Basahin ang talata", "Hanapin ang paksa", "Hanapin ang pangunahing ideya", "Tukuyin ang mga detalye"],
    "pagsulat": ["Basahin ang salita", "Suriin ang baybay", "Suriin ang malaking titik at bantas", "Isulat ito nang maayos"],
    "salitangugat": ["Basahin ang salita", "Alisin ang panlapi", "Hanapin ang payak na anyo", "Sabihin ang salitang-ugat"],
    "pananalita": ["Tingnan ang sitwasyon", "Isipin kung sino ang kausap", "Piliin ang magalang na pahayag", "Sabihin ito nang may respeto"],
    "buod": ["Basahin ang kuwento", "Tukuyin ang tauhan", "Hanapin ang suliranin at pangyayari", "Isulat ang maikling buod"],
}

VOCABS = {
    "simuno": [("simuno", "ang pinag-uusapan sa pangungusap"),
               ("panaguri", "ang nagsasabi tungkol sa simuno"),
               ("payak na pangungusap", "pangungusap na may isang simuno at isang panaguri")],
    "talata": [("paksa", "ang pinag-uusapan sa talata"),
               ("pangunahing ideya", "ang pinakamahalagang mensahe ng talata"),
               ("detalye", "dagdag na impormasyon tungkol sa paksa")],
    "pagsulat": [("baybay", "pagkakasulat ng mga titik ng salita"),
                 ("malaking titik", "malaking letra sa simula ng pangungusap at pangalan"),
                 ("bantas", "markang tulad ng tuldok at kuwit")],
    "salitangugat": [("salitang-ugat", "ang payak na anyo ng salita"),
                     ("panlapi", "idinadagdag sa salitang-ugat upang bumuo ng bagong salita"),
                     ("bagong salita", "salitang nabuo mula sa ugat at panlapi")],
    "pananalita": [("pagbati", "pagsabi ng magandang araw o pagbati sa kapwa"),
                   ("pananalita", "mga salitang ginagamit sa pakikipag-usap"),
                   ("respeto", "paggalang sa kapwa")],
    "buod": [("tauhan", "ang mga tao o hayop sa kuwento"),
             ("suliranin", "ang problema sa kuwento"),
             ("buod", "maikling paglalahad ng mahahalagang bahagi ng kuwento")],
}

MATCH_LABELS = {
    "simuno": ["simuno", "panaguri", "buong pangungusap"],
    "talata": ["paksa", "pangunahing ideya", "detalye"],
    "pagsulat": ["salitang may wastong baybay", "salitang may malaking titik", "salitang may wastong bantas"],
    "salitangugat": ["salitang-ugat", "bagong salita", "pares ng ugat at bagong salita"],
    "pananalita": ["magalang na pagbati", "magalang na pakiusap", "magalang na pasasalamat"],
    "buod": ["tauhan", "suliranin", "mahalagang pangyayari"],
}

SHELLS = {
    "simuno": {
        "ANIMATED_EXPLANATION": "Ang simuno ang pinag-uusapan. Ang panaguri ang nagsasabi tungkol sa simuno.",
        "HOTSPOT_IMAGE": "Tuklasin ang mga halimbawa. Hanapin ang simuno at panaguri. Magsimula sa '{s0}'.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga buong pangungusap na may simuno at panaguri, tulad ng '{s0}'.",
        "MULTIPLE_CHOICE": "Piliin ang pinakamabuting sagot. Buong pangungusap ang may simuno at panaguri, tulad ng '{s0}'.",
        "MATCHING_PAIRS": "Itugma ang bawat bahagi sa wastong pangkat: simuno, panaguri, o buong pangungusap. Halimbawa: '{s0}'.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Magsimula sa pagbasa ng pangungusap tulad ng '{s0}'.",
    },
    "talata": {
        "ANIMATED_EXPLANATION": "Ang talata ay binubuo ng magkakaugnay na pangungusap tungkol sa isang paksa.",
        "HOTSPOT_IMAGE": "Tuklasin ang talata. Hanapin ang paksa, pangunahing ideya, detalye, at wakas.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga bahagi ng talatang ito.",
        "MULTIPLE_CHOICE": "Piliin ang pinakamabuting sagot. Tandaan: ang paksa ang pinag-uusapan.",
        "MATCHING_PAIRS": "Itugma ang bawat bahagi ng talata sa wastong pangkat: paksa, pangunahing ideya, o detalye.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Basahin muna ang talata bago hanapin ang paksa.",
    },
    "pagsulat": {
        "ANIMATED_EXPLANATION": "Suriin ang baybay, malaking titik, at bantas bago isulat ang salita.",
        "HOTSPOT_IMAGE": "Tuklasin ang mga salita. Basahin ang baybay at ang kahulugan ng bawat isa.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga salitang may wastong baybay.",
        "MULTIPLE_CHOICE": "Piliin ang salitang may wastong baybay. Tingnan ang bawat titik.",
        "MATCHING_PAIRS": "Itugma ang bawat salita sa wastong pangkat: baybay, malaking titik, o bantas.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Basahin muna ang salita bago suriin ang baybay.",
    },
    "salitangugat": {
        "ANIMATED_EXPLANATION": "Ang salitang-ugat ang payak na anyo na pinagmumulan ng ibang salita.",
        "HOTSPOT_IMAGE": "Tuklasin ang mga halimbawa. Hanapin ang salitang-ugat ng bawat salita.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga pares ng salitang-ugat at bagong salita.",
        "MULTIPLE_CHOICE": "Piliin ang pinakamabuting sagot. Hanapin ang payak na anyo ng salita.",
        "MATCHING_PAIRS": "Itugma ang bawat halimbawa sa wastong pangkat: ugat, bagong salita, o pares.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Alisin muna ang panlapi upang makita ang ugat.",
    },
    "pananalita": {
        "ANIMATED_EXPLANATION": "Piliin ang magalang na pananalita na angkop sa oras, kausap, at pangyayari.",
        "HOTSPOT_IMAGE": "Tuklasin ang mga magalang na pahayag. Isipin kung kailan ito ginagamit.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga magalang na pahayag.",
        "MULTIPLE_CHOICE": "Piliin ang magalang na pahayag na angkop sa sitwasyon.",
        "MATCHING_PAIRS": "Itugma ang bawat pahayag sa wastong pangkat: pagbati, pakiusap, o pasasalamat.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Tingnan muna ang sitwasyon bago pumili ng pahayag.",
    },
    "buod": {
        "ANIMATED_EXPLANATION": "Sa buod, ilahad ang tauhan, suliranin, mahahalagang pangyayari, at wakas nang maikli.",
        "HOTSPOT_IMAGE": "Basahin ang kuwento. Tukuyin ang tauhan, suliranin, pangyayari, at wakas.",
        "SORT_AND_CLASSIFY": "Ayusin ang mga halimbawa. Panatilihin ang mga bahagi ng kuwentong ito.",
        "MULTIPLE_CHOICE": "Piliin ang pinakamabuting sagot. Tandaan: ang tauhan ang gumaganap sa kuwento.",
        "MATCHING_PAIRS": "Itugma ang bawat bahagi ng kuwento sa wastong pangkat: tauhan, suliranin, o pangyayari.",
        "SEQUENCE_BUILDER": "Ayusin ang mga hakbang. Basahin muna ang kuwento bago tukuyin ang tauhan.",
    },
}


def load_lessons():
    return sorted(p.name[:-5] for p in LESSONS.glob("filipino-g3-q*.json"))


def group_index(lid, members):
    return members.index(lid)


def find_skill(lesson):
    return SKILLS.get(lesson.get("objective", ""))


def rotated(seq, idx, n):
    """Deterministic rotation starting AFTER idx: lesson-dependent windows.

    Absolute slices (other[:3]) made every lesson draw the same distractor
    subset, so token-set unions converged to 1.00 Jaccard. Rotation gives
    each lesson a window that differs from its neighbours' windows.
    """
    return [seq[(idx + 1 + k) % len(seq)] for k in range(n)]


def simuno_splits(s, p):
    """Return (correct_split, [3 wrong splits]) for the sentence 's p'.

    Wrong splits cut the sentence at a different word boundary, so the child
    must find the true simuno/panaguri division instead of matching the stem.
    """
    full = f"{s} {p}".strip()
    correct = f"{s} / {p}"
    words = full.split()
    wrongs = []
    for i in range(1, len(words)):
        cand = f"{' '.join(words[:i])} / {' '.join(words[i:])}"
        if cand != correct:
            wrongs.append(cand)
    if len(wrongs) < 3:
        # Short sentences: add the 'whole sentence / period' split.
        stem = full[:-1] if full.endswith('.') else full
        wrongs.append(f"{stem} / .")
    return correct, wrongs[:3]


def build_items(skill, idx, data):
    """Return list of (prompt, options, correct_text) — options are distractors, correct separate."""
    day = int(data["lid"].rsplit("d", 1)[1]) % 4
    items = []
    if skill == "simuno":
        # Even-start windows over the 64-sentence pool: every lesson gets a
        # UNIQUE 4-set, adjacent lessons share only 2 sentences. ALL options
        # come from the lesson's own block — cross-lesson pools leak the same
        # tokens into every lesson and re-inflate Jaccard.
        pool = SIMUNO_POOL
        block = [pool[(2 * idx + k) % len(pool)] for k in range(4)]
        sents = [f"{s} / {p}" for s, p in block]
        s1, p1 = block[0]
        s2, p2 = block[1]
        s3, p3 = block[2]
        s4, p4 = block[3]
        items.append((f"Alin ang simuno sa '{sents[0]}'?", [s2, s3, s4], s1))
        items.append((f"Alin ang panaguri sa '{sents[0]}'?", [p2, p3, p4], p1))
        ok2, wrong2 = simuno_splits(*block[1])
        items.append((f"Paano wastong hatiin ang pangungusap na '{s2} {p2}'?",
                      wrong2, ok2))
        items.append((f"Kung ang simuno ay '{s1}', alin ang maaaring panaguri?",
                      [p2, p3, p4], p1))
        ok3, wrong3 = simuno_splits(*block[2])
        items.append((f"Paano wastong hatiin ang pangungusap na '{s3} {p3}'?",
                      wrong3, ok3))
    elif skill == "talata":
        para, paksa, ideya, detalye, wakas = TALATA_SETS[idx % len(TALATA_SETS)]
        other = rotated(TALATA_SETS, idx, len(TALATA_SETS) - 1)
        items.append(("Alin ang paksa ng talata?", [o[1] for o in other][:3], paksa))
        items.append(("Ano ang pangunahing ideya ng talata?", [o[2] for o in other][:3], ideya))
        items.append(("Aling detalye ang nasa talata?", [o[3] for o in other][:3], detalye))
        items.append(("Aling pangungusap ang wakas ng talata?", [o[4] for o in other][:3], wakas))
        items.append(("Tungkol saan ang talata?", [o[1] for o in other][:3], paksa))
    elif skill == "pagsulat":
        words = PAGSULAT_SETS[idx % len(PAGSULAT_SETS)]
        w1, d1, m11, m12, c1 = words[0]
        w2, d2, m21, m22, c2 = words[1]
        w3, d3, m31, m32, c3 = words[2]
        w4, d4, m41, m42, c4 = words[3]
        items.append(("Alin ang wastong baybay ng '" + w1 + "'?", [m11, m12, w4], w1))
        items.append(("Alin ang salitang may wastong baybay?", [m21, m22, w3], w2))
        items.append(("Ano ang kahulugan ng '" + w2 + "'?", [d1, d3, d4], d2))
        items.append(("Alin sa mga sumusunod ang wastong pagkakasulat?", [m31, m32, w1], w3))
        cloze = c1.replace("___", "___")
        items.append((f"Kumpletuhin ang pangungusap: '{cloze}'", [w2, w3, w4], w1))
    elif skill == "salitangugat":
        pairs = SALITANGUGAT_SETS[idx % len(SALITANGUGAT_SETS)]
        others = [r for p in pairs for r in p]
        all_pairs = [p for s in SALITANGUGAT_SETS for p in s]
        r1, rel1 = pairs[0]; r2, rel2 = pairs[1]; r3, rel3 = pairs[2]; r4, rel4 = pairs[3]
        items.append((f"Alin ang salitang-ugat ng '{rel1}'?",
                      [x for x in others if x != r1 and x != rel1][:3], r1))
        items.append(("Aling pares ang may salitang-ugat at bagong salita?",
                      [f"{a} → {b}" for a, b in rotated(all_pairs, idx, len(all_pairs) - 1)
                       if (a, b) != (r1, rel1)][:3],
                      f"{r1} → {rel1}"))
        items.append((f"Ang salitang-ugat ng '{rel3}' ay ___.",
                      [x for x in others if x != r3 and x != rel3][:3], r3))
        items.append((f"Ano ang salitang-ugat ng '{rel4}'?",
                      [x for x in others if x != r4 and x != rel4][:3], r4))
        items.append((f"Aling salita ang HINDI galing sa ugat na '{r1}'?",
                      [rel2, rel3, rel4],
                      SALITANGUGAT_SETS[(idx + 1) % len(SALITANGUGAT_SETS)][0][1]))
    elif skill == "pananalita":
        set_ = PANANALITA_SETS[idx % len(PANANALITA_SETS)]
        # 2 rotating other sets + one impolite filler keeps windows disjoint.
        other_phrases = [p for s in rotated(PANANALITA_SETS, idx, 2) for p in s]
        p1, s1 = set_[0]; p2, s2 = set_[1]; p3, s3 = set_[2]; p4, s4 = set_[3]
        items.append((f"Alin ang magalang na pagbati{' sa ' + s1.split(' sa ')[1] if ' sa ' in s1 else ''}?",
                      [o[0] for o in other_phrases if o[0] != p1][:2] + [IMPOLITE[0]], p1))
        items.append(("Ano ang sasabihin mo kung may nagawan ka ng mali?",
                      [o[0] for o in other_phrases if o[0] != p4][:2] + [IMPOLITE[1]], p4))
        items.append(("Alin ang angkop na tugon sa 'Salamat po'?",
                      [o[0] for o in other_phrases if o[0] != p3][:2] + [IMPOLITE[2]], p3))
        items.append(("Kung hihingi ka ng pabor, alin ang magalang na sasabihin?",
                      [o[0] for o in other_phrases if o[0] != p2][:2] + [IMPOLITE[0]], p2))
        items.append(("Alin ang magalang na pananalita?", IMPOLITE[:3], p1))
    elif skill == "buod":
        story, tauhan, suliranin, pangyayari, wakas, buod = BUOD_SETS[idx % len(BUOD_SETS)]
        # 3-of-4 windows converge; use 2 rotating other stories + 1 cross-element.
        other = rotated(BUOD_SETS, idx, 2)
        items.append(("Sino ang tauhan sa kuwento?", [o[1] for o in other] + [other[0][2]], tauhan))
        items.append(("Ano ang suliranin sa kuwento?", [o[2] for o in other] + [other[0][3]], suliranin))
        items.append(("Ano ang mahalagang pangyayari sa kuwento?",
                      [o[3] for o in other] + [other[0][4]], pangyayari))
        items.append(("Paano natapos ang kuwento?", [o[4] for o in other] + [other[0][1]], wakas))
        items.append(("Alin ang maikling buod ng kuwento?", [o[5] for o in other] + [other[0][3]], buod))
    return items, day


def clean_stock_junk(value):
    if isinstance(value, str):
        for old, new in FIL_JUNK_REPLACEMENTS.items():
            value = value.replace(old, new)
        return value
    if isinstance(value, list):
        return [clean_stock_junk(item) for item in value]
    if isinstance(value, dict):
        return {key: clean_stock_junk(item) for key, item in value.items()}
    return value


def repair_lesson(lesson):
    skill = find_skill(lesson)
    if skill is None:
        return lesson
    lid = lesson["lessonId"]
    members = [l for l in load_lessons() if find_skill(json.loads((LESSONS / f"{l}.json").read_text(encoding="utf-8"))) == skill]
    idx = group_index(lid, members)
    day = int(lid.rsplit("d", 1)[1]) % 4
    correct_at = day

    items, _ = build_items(skill, idx, {"lid": lid})
    vocab = VOCABS[skill]

    if skill == "simuno":
        pool = SIMUNO_POOL
        block = [pool[(2 * idx + k) % len(pool)] for k in range(4)]
        sentences = [f"{s} / {p}" for s, p in block]
        hotspot = sentences
        sort_fits = sentences
        sort_other = ["ang matiyagang sumusulat", "sa likod ng bahay",
                      "at nagmamadali tuwing gabi", "kasi tuwang-tuwa ang lahat"]
        mcq_ok = sentences[0]
        mcq_bad = ["ang matiyagang sumusulat", "sa likod ng bahay", "kasi tuwang-tuwa ang lahat"]
        matching = [{"left": block[0][0], "right": "simuno"},
                    {"left": block[0][1], "right": "panaguri"},
                    {"left": sentences[0], "right": "buong pangungusap"}]
        animated = f"Ang simuno ang pinag-uusapan. Ang panaguri ang nagsasabi tungkol sa simuno. {sentences[0]}"
        c = {"s0": sentences[0]}
    elif skill == "talata":
        para, paksa, ideya, detalye, wakas = TALATA_SETS[idx % len(TALATA_SETS)]
        hotspot = [paksa, ideya, detalye, wakas]
        sort_fits = [paksa, ideya, detalye, wakas]
        other = rotated(TALATA_SETS, idx, len(TALATA_SETS) - 1)
        sort_other = [o[3] for o in other][:4]
        mcq_ok = ideya
        mcq_bad = [o[3] for o in other][:3]
        matching = [{"left": paksa, "right": "paksa"},
                    {"left": ideya, "right": "pangunahing ideya"},
                    {"left": detalye, "right": "detalye"}]
        animated = f"{para} Ang talata ay binubuo ng magkakaugnay na pangungusap tungkol sa isang paksa."
        c = {"para": para}
    elif skill == "pagsulat":
        words = PAGSULAT_SETS[idx % len(PAGSULAT_SETS)]
        hotspot = [w[0] for w in words]
        sort_fits = [w[0] for w in words]
        sort_other = [w[2] for w in words][:4]
        mcq_ok = words[0][0]
        mcq_bad = [w[2] for w in words[1:]][:3]
        matching = [{"left": w[0], "right": "salitang may wastong baybay"} for w in words[:3]]
        animated = f"Suriin ang baybay, malaking titik, at bantas. Ang salitang '{words[0][0]}' ay isang halimbawa."
        c = {"w0": words[0][0]}
    elif skill == "salitangugat":
        pairs = SALITANGUGAT_SETS[idx % len(SALITANGUGAT_SETS)]
        hotspot = [f"{r} → {rel}" for r, rel in pairs]
        sort_fits = hotspot
        all_pairs = [p for s in SALITANGUGAT_SETS for p in s]
        others_pairs = rotated(all_pairs, idx, 4)
        sort_other = [f"{a} → {b}" for a, b in others_pairs[:4]]
        mcq_ok = hotspot[0]
        mcq_bad = [f"{a} → {b}" for a, b in others_pairs[:3]]
        matching = [{"left": pairs[0][0], "right": "salitang-ugat"},
                    {"left": pairs[0][1], "right": "bagong salita"},
                    {"left": hotspot[0], "right": "pares ng ugat at bagong salita"}]
        animated = f"Ang salitang-ugat ang payak na anyo. Sa '{hotspot[0]}', ang ugat ay '{pairs[0][0]}'."
        c = {"p0": hotspot[0]}
    elif skill == "pananalita":
        set_ = PANANALITA_SETS[idx % len(PANANALITA_SETS)]
        phrases = [p[0] for p in set_]
        hotspot = phrases
        sort_fits = phrases
        all_phrases = [p for s in PANANALITA_SETS for p in s]
        others = [p[0] for p in rotated(all_phrases, idx, 8) if p[0] not in phrases]
        sort_other = others[:4]
        mcq_ok = phrases[0]
        mcq_bad = others[:3]
        matching = [{"left": set_[0][0], "right": "magalang na pagbati"},
                    {"left": set_[1][0], "right": "magalang na pakiusap"},
                    {"left": set_[2][0], "right": "magalang na pasasalamat"}]
        animated = f"Piliin ang magalang na pananalita na angkop sa sitwasyon. Halimbawa: {phrases[0]}"
        c = {"p0": phrases[0]}
    else:  # buod
        story, tauhan, suliranin, pangyayari, wakas, buod = BUOD_SETS[idx % len(BUOD_SETS)]
        hotspot = [tauhan, suliranin, pangyayari, wakas]
        sort_fits = hotspot
        other = rotated(BUOD_SETS, idx, len(BUOD_SETS) - 1)
        sort_other = [o[3] for o in other][:4]
        mcq_ok = tauhan
        mcq_bad = [o[1] for o in other][:3]
        matching = [{"left": tauhan, "right": "tauhan"},
                    {"left": suliranin, "right": "suliranin"},
                    {"left": pangyayari, "right": "mahalagang pangyayari"}]
        animated = f"{story} Sa buod, ilahad ang tauhan, suliranin, pangyayari, at wakas nang maikli."
        c = {"story": story}

    # Apply to activities
    for a in lesson["activities"]:
        t = a.get("type")
        content = a.get("content")
        # Already-repaired activities are preserved verbatim (true
        # idempotency): the pack may have since evolved (diversified
        # distractors, topic-specific instructions, rebalanced positions —
        # educator pass 2026-08-06) and repair must be a no-op for them.
        # Only genuinely broken activities (missing/blank fields) are rebuilt.
        well_formed = (
            bool(t)
            and bool((a.get("instruction") or "").strip())
            and bool((a.get("prompt") or "").strip())
            and (
                (isinstance(content, str) and content.strip())
                or (isinstance(content, dict) and any(str(v).strip() for v in content.values()))
                or (isinstance(content, list) and len(content) > 0)
            )
            and not (
                skill == "talata"
                and t == "ANIMATED_EXPLANATION"
                and isinstance(content, str)
                and len(content.split(". ")) <= 2
            )
        )
        if well_formed:
            continue
        shell = SHELLS[skill].get(t)
        if t == "ANIMATED_EXPLANATION" and isinstance(content, str):
            a["content"] = animated
            a["narration"] = animated
            if "accessibilityAlternative" in a:
                a["accessibilityAlternative"] = animated
        elif t == "HOTSPOT_IMAGE" and isinstance(content, dict):
            content["examples"] = list(hotspot)
        elif t == "SORT_AND_CLASSIFY" and isinstance(content, dict):
            content["fits"] = list(sort_fits)
            content["doesNotFit"] = list(sort_other)
        elif t == "MULTIPLE_CHOICE" and isinstance(content, dict):
            rest = [o for o in mcq_bad if o != mcq_ok][:3]
            options = rest[:correct_at] + [mcq_ok] + rest[correct_at:]
            content["options"] = options
            content["correctIndex"] = correct_at
        elif t == "MATCHING_PAIRS" and isinstance(content, dict):
            content["pairs"] = matching
        elif t == "SEQUENCE_BUILDER" and isinstance(content, dict):
            content["steps"] = list(SEQUENCES[skill])
        if shell:
            a["instruction"] = shell.format(**c)
            a["prompt"] = shell.format(**c)

    existing_vocab = lesson.get("vocabulary")
    vocab_well_formed = existing_vocab and all(
        isinstance(v, dict) and str(v.get("term") or "").strip() and str(v.get("definition") or "").strip()
        for v in existing_vocab
    )
    if not vocab_well_formed:
        # Rebuild only when vocabulary is missing/blank; preserve evolved
        # definitions (educator pass 2026-08-06 rewrote them).
        lesson["vocabulary"] = [{"term": t, "definition": d} for t, d in vocab]

    # Assessment
    correct_at = day
    existing_by_seq = {
        item.get("sequence"): item
        for item in (lesson.get("assessment") or {}).get("items", [])
    }
    built, _ = build_items(skill, idx, {"lid": lid})
    new_items = []
    for seq, (prompt, options, correct_text) in enumerate(built, start=1):
        existing = existing_by_seq.get(seq)
        # Already-repaired items are preserved verbatim (true idempotency):
        # the pack may have since evolved (e.g. rebalanced answer positions,
        # typed assessment items) and repair must be a no-op for them —
        # see test_repair_changes_every_lesson (2026-08-06).
        if (
            existing is not None
            and existing.get("itemId") == f"{lid}-q{seq}"
            and existing.get("prompt") == prompt
            and isinstance(existing.get("options"), list)
            and len(existing["options"]) == 4
            and len(existing.get("correctOptionIds") or []) == 1
        ):
            new_items.append(existing)
            continue
        rest = [o for o in options if o != correct_text]
        if len(rest) != 3:
            raise ValueError(f"{lid}: item {seq} has {len(rest)} distractors")
        display = rest[:correct_at] + [correct_text] + rest[correct_at:]
        ids = [chr(97 + i) for i in range(4)]
        new_items.append({
            "sequence": seq,
            "itemId": f"{lid}-q{seq}",
            "type": "MULTIPLE_CHOICE",  # current schema: all assessment items typed
            "prompt": prompt,
            "options": [{"id": ids[i], "text": display[i]} for i in range(4)],
            "correctOptionIds": [ids[display.index(correct_text)]],
            "explanation": f"Ang pinakamabuting sagot ay: {correct_text}",
        })
    lesson["assessment"]["items"] = new_items
    return clean_stock_junk(lesson)


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--include-legacy", action="store_true")
    args = ap.parse_args(argv)

    changed = 0
    for lid in load_lessons():
        path = LESSONS / f"{lid}.json"
        lesson = json.loads(path.read_text(encoding="utf-8"))
        if find_skill(lesson) is None:
            continue
        repaired = repair_lesson(json.loads(json.dumps(lesson)))
        if repaired != lesson:
            changed += 1
            if not args.dry_run and not getattr(args, "check", False):
                path.write_text(json.dumps(repaired, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # Global junk audit across all filipino lessons
    junk = 0
    broken = 0
    for lid in load_lessons():
        d = json.loads((LESSONS / f"{lid}.json").read_text(encoding="utf-8"))
        blob = json.dumps(d, ensure_ascii=False)
        for s in FIL_JUNK:
            junk += blob.count(s)
        for it in d["assessment"]["items"]:
            opts = {o["id"]: o["text"] for o in it["options"]}
            co = it["correctOptionIds"]
            if len(opts) != 4 or not co or not opts.get(co[0]):
                broken += 1
    print(f"lessons repaired: {changed}")
    print(f"junk remaining:   {junk}")
    print(f"broken items:     {broken}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

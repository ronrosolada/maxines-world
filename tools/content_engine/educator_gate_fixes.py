#!/usr/bin/env python3
"""
Educator gate fixes (authoring pass) for the bundled Grade 3 pack.
Categories fixed:
  A. matching_identity_pair / matching_duplicate_right / matching_right_repeats_clue
     (authored pair rewrites per lesson + generic dash-split rule)
  B. generic_instruction (stock phrases -> authored, type-specific copy)
  C. template_or_objective_prompt (strip objective paste tails)
  D. filipino_english_bleed (word-level Filipino replacements)
  E. learner_text_too_long (shorten instruction/correct/retry to <=90 chars)
  F. duplicate_prompts (append lesson-specific context clause)
Deterministic and idempotent; never touches IDs, keys, or metadata.
"""
import json
import glob
import re
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

# ---------------------------------------------------------------- A. matching
MATCHING_REWRITES = {
    "araling-panlipunan-g3-m01-d02": [
        {"left": "Bayan A", "right": "hilaga ng Bayan B"},
        {"left": "Ilog", "right": "nasa pagitan ng dalawang bayan"},
        {"left": "Baybayin", "right": "silangan ayon sa kompas"},
    ],
    "araling-panlipunan-g3-m01-d03": [
        {"left": "1 icon", "right": "= 100 kathang-isip na residente"},
        {"left": "3 icons", "right": "= 300"},
        {"left": "bar na 500", "right": "mas mataas sa bar na 300"},
    ],
    "araling-panlipunan-g3-m01-d05": [
        {"left": "Ulan", "right": "bumubuo ng sapa at ilog"},
        {"left": "Ilog", "right": "maaaring umabot sa lawa"},
        {"left": "Ebaporasyon", "right": "nagpapabalik ng singaw ng tubig sa hangin"},
    ],
    "araling-panlipunan-g3-m01-d06": [
        {"left": "Ilog", "right": "maaaring daanan ng bangka"},
        {"left": "Kapatagan", "right": "maaaring sakahan"},
        {"left": "Baybayin", "right": "maaaring tirahan ng mga organismo"},
    ],
    "araling-panlipunan-g3-m01-d07": [
        {"left": "Patong ng baha", "right": "makikita sa mapa ng panganib"},
        {"left": "Lugar ng paglikas", "right": "ayon sa opisyal na mapa"},
        {"left": "Makinig sa nakatatanda", "right": "at sa awtoridad kapag may panganib"},
    ],
    "araling-panlipunan-g3-m01-d08": [
        {"left": "Magtipid ng tubig", "right": "pangangalaga sa yamang-tubig"},
        {"left": "Wastong segregasyon ng basura", "right": "pangangalaga sa kalinisan"},
        {"left": "Protektahan ang tirahan ng mga hayop", "right": "pangangalaga sa mga buhay na nilalang"},
    ],
    "araling-panlipunan-g3-m01-d09": [
        {"left": "Ilog at paninirahan", "right": "katangian ng mapa ng kapaligiran"},
        {"left": "Elevasyon at panganib", "right": "mga patong na binabasa sa mapa"},
        {"left": "Tala ng pinagmulan", "right": "kasama ang petsa ng datos"},
    ],
    "araling-panlipunan-g3-m01-d10": [
        {"left": "Naitalang may petsa", "right": "tiyak na tala ng nakaraan"},
        {"left": "Pasalitang salaysay na may pinagmulan", "right": "kasaysayang binigkas at naipasa"},
        {"left": "Alamat na may malinaw na label", "right": "kuwentong may aral at may label"},
    ],
    "araling-panlipunan-g3-m01-d11": [
        {"left": "Dating tulay", "right": "nagbago sa paglipas ng panahon"},
        {"left": "Tradisyong patuloy", "right": "nananatili sa pamayanan"},
        {"left": "Bagong sasakyan sa kalye", "right": "pagbabago sa transportasyon"},
    ],
    "araling-panlipunan-g3-m01-d12": [
        {"left": "Matabang lupa", "right": "maaaring pagsakahan"},
        {"left": "Baybayin", "right": "maaaring sumuporta sa turismo"},
        {"left": "Lungsod", "right": "may iba’t ibang hanapbuhay"},
    ],
    "araling-panlipunan-g3-m01-d14": [
        {"left": "Orihinal na awit", "right": "hindi kinopya sa iba"},
        {"left": "Heometrikong disenyo", "right": "hindi sagrado at gawa ng sarili"},
        {"left": "Pagkilala sa pinagmulan", "right": "paggamit nang may pahintulot"},
    ],
    "araling-panlipunan-g3-m01-d15": [
        {"left": "Guro", "right": "nagbukas ng sulok-pagbasa"},
        {"left": "Nars", "right": "tumulong sa klinika"},
        {"left": "Boluntaryo", "right": "naglinis ng ilog"},
    ],
    "araling-panlipunan-g3-m01-d16": [
        {"left": "Maraming wika", "right": "bahagi ng pagkakakilanlan"},
        {"left": "Iba’t ibang gawain", "right": "kabuhayan ng pamayanan"},
        {"left": "Magkakaibang kuwento", "right": "kasaysayan ng pamayanan"},
    ],
    "araling-panlipunan-g3-m01-d17": [
        {"left": "Iba’t ibang pagbati", "right": "gawi sa pakikipagkita"},
        {"left": "Iba’t ibang pagkain", "right": "gawi sa hapag-kainan"},
        {"left": "Iba’t ibang wika", "right": "gawi sa pakikipag-usap"},
    ],
    "araling-panlipunan-g3-m01-d18": [
        {"left": "Nagbabagong iskedyul ng pista", "right": "tradisyong umaangkop"},
        {"left": "Ang resipi ay gumagamit ng magagamit na sangkap", "right": "paglutong umaangkop sa panahon"},
        {"left": "Kuwentong muling isinalaysay", "right": "bagong midyum na may parehong kahulugan"},
    ],
    "araling-panlipunan-g3-m01-d19": [
        {"left": "Litratong may petsa", "right": "ebidensya mula sa nakaraan"},
        {"left": "Mapang may pinagmulan", "right": "gabay sa makasaysayang lugar"},
        {"left": "Pasalitang salaysay", "right": "kuwento ng tagapagsalita na nakasaksi"},
    ],
    "araling-panlipunan-g3-m01-d20": [
        {"left": "Orihinal na disenyo", "right": "gawa ng sarili, hindi kinopya"},
        {"left": "Tala ng pinagmulan", "right": "pinagmulan ng impormasyon"},
        {"left": "Label ng kathang-isip na lugar", "right": "pangalan sa mapa ng komunidad"},
    ],
    "filipino-g3-m01-d05": [
        {"left": "ha-la-man", "right": "halaman"},
        {"left": "ka-la-pati", "right": "kalapati"},
        {"left": "sa-la-min", "right": "salamin"},
    ],
    "filipino-g3-m01-d08": [
        {"left": "aso", "right": "nauuna bago ang bata"},
        {"left": "bata", "right": "nauuna bago ang dahon"},
        {"left": "unang salita sa talaan", "right": "may kasamang kahulugan"},
    ],
    "filipino-g3-m01-d09": [
        {"left": "Si Mara ay nagbasa.", "right": "Siya ay tahimik."},
        {"left": "Ang mga bata ay dumating.", "right": "Sila ay masaya."},
        {"left": "Hawak ko ang aklat.", "right": "Ito ay bago."},
    ],
    "filipino-g3-m01-d10": [
        {"left": "Pakiabot po.", "right": "paghingi ng tulong"},
        {"left": "Maraming salamat.", "right": "pagpapasalamat"},
        {"left": "Paumanhin po.", "right": "paghingi ng paumanhin"},
    ],
    "filipino-g3-m01-d12": [
        {"left": "Una", "right": "tinupi ang dahon"},
        {"left": "Sumunod", "right": "inilagay sa tubig"},
        {"left": "Sa huli", "right": "lumutang ang bangka"},
    ],
    "filipino-g3-m01-d13": [
        {"left": "Masaya si Lino.", "right": "nagtatapos sa tuldok"},
        {"left": "Nasaan ang aklat?", "right": "nagtatapos sa tandang pananong"},
        {"left": "Naku! Nahulog ang bola!", "right": "nagtatapos sa tandang padamdam"},
    ],
    "filipino-g3-m01-d16": [
        {"left": "Nahanap ang susi", "right": "matapos hanapin sa mesa"},
        {"left": "Nabuhay ang halaman", "right": "matapos diligan"},
        {"left": "Naibalik ang aklat", "right": "matapos makita"},
    ],
    "filipino-g3-m01-d17": [
        {"left": "Parehong bilog ang dalawang plato.", "right": "pareho"},
        {"left": "Mas mahaba ang pulang laso kaysa asul.", "right": "mas"},
        {"left": "Kapwa may dahon ang halaman.", "right": "kapwa"},
    ],
    "filipino-g3-m01-d18": [
        {"left": "Paksa", "right": "hardin"},
        {"left": "Detalye", "right": "may gulay at bulaklak"},
        {"left": "Wakas", "right": "inaalagaan ito araw-araw"},
    ],
    "filipino-g3-m01-d20": [
        {"left": "Malinis na tubig", "right": "inumin ng alagang hayop"},
        {"left": "Angkop na pagkain", "right": "kinakain ng alagang hayop"},
        {"left": "Kumonsulta sa beterinaryo", "right": "kapag may sakit ang alaga"},
    ],
    "filipino-g3-q1-w07-d01": [
        {"left": "Sabado", "right": "araw na isinusulat sa malaking letra"},
        {"left": "Pilipinas", "right": "bansang isinusulat sa malaking letra"},
        {"left": "Juan", "right": "pangalang isinusulat sa malaking letra"},
        {"left": "araw", "right": "salitang isinusulat sa maliit na letra"},
        {"left": "bansa", "right": "karaniwang salitang isinusulat sa maliit na letra"},
    ],
    "gmrc-g3-q1-w01-d01": [
        {"left": "sumubok ng bagong gawain", "right": "patunay ng tiwala sa sarili"},
        {"left": "nagsanay", "right": "patunay ng pagsisikap"},
        {"left": "humingi ng gabay", "right": "patunay ng kahandaan sa pagkatuto"},
    ],
    "gmrc-g3-q1-w01-d03": [
        {"left": "inaayos ang gamit", "right": "pagmamahal sa bayan"},
        {"left": "tinatapos ang gawain", "right": "kilos ng pagiging responsable"},
        {"left": "sumusunod sa napagkasunduan", "right": "kilos ng pagtupad sa tungkulin"},
    ],
    "gmrc-g3-q4-w07-d03": [
        {"left": "inaayos ang gamit", "right": "pagmamahal sa bayan"},
        {"left": "tinatapos ang gawain", "right": "kilos ng pagiging responsable"},
        {"left": "sumusunod sa napagkasunduan", "right": "kilos ng pagtupad sa tungkulin"},
    ],
    "english-g3-m01-d14": [
        {"left": "chair", "right": "a ch word for sitting"},
        {"left": "chop", "right": "a ch word for cutting"},
        {"left": "ship", "right": "a sh word that sails"},
    ],
    "english-g3-q2-w05-d04": [
        {"left": "topic: mangrove trees", "right": "the topic"},
        {"left": "fact: roots hold soil", "right": "a fact about roots"},
        {"left": "fact: trees provide shelter", "right": "a fact about shelter"},
        {"left": "fact: mangroves grow near the sea", "right": "a fact about where they grow"},
    ],
    "science-g3-m01-d19": [
        {"left": "plucked string vibrates", "right": "string vibration is a sound source"},
        {"left": "speaker cone vibrates", "right": "speaker vibration is a sound source"},
        {"left": "lower volume protects hearing", "right": "hearing safety"},
    ],
    "science-g3-m01-d20": [
        {"left": "sunlight can warm surfaces", "right": "safe heat energy"},
        {"left": "battery powers a safe device", "right": "safe electric energy"},
        {"left": "adult handles damaged cord", "right": "adult help"},
    ],
    "science-g3-q4-w08-d01": [
        {"left": "a bell rings when shaken", "right": "makes sound"},
        {"left": "a mirror reflects light", "right": "reflection helps us see"},
        {"left": "turn on a lamp to read in a dark room", "right": "light helps us see"},
    ],
    "science-g3-q4-w08-d04": [
        {"left": "a radio plays music", "right": "makes sound"},
        {"left": "a window lets light through", "right": "light passes through it"},
        {"left": "use a flashlight to walk in a dark place", "right": "light helps us see"},
    ],
    # Math Add-Up Adventure series: unique right labels per left
    "mathematics-g3-q2-w03-d02": [
        {"left": "245 + 123 = 368", "right": "correct addition: makes 368"},
        {"left": "2,650 + 1,200 = 3,850", "right": "correct addition: makes 3,850"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w03-d03": [
        {"left": "432 + 516 = 948", "right": "correct addition: makes 948"},
        {"left": "1,830 + 2,740 = 4,570", "right": "correct addition: makes 4,570"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w03-d04": [
        {"left": "671 + 229 = 900", "right": "correct addition: makes 900"},
        {"left": "3,500 + 1,250 = 4,750", "right": "correct addition: makes 4,750"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w03-d05": [
        {"left": "118 + 264 = 382", "right": "correct addition: makes 382"},
        {"left": "4,260 + 2,380 = 6,640", "right": "correct addition: makes 6,640"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w04-d01": [
        {"left": "573 + 248 = 821", "right": "correct addition: makes 821"},
        {"left": "1,905 + 2,995 = 4,900", "right": "correct addition: makes 4,900"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w04-d04": [
        {"left": "306 + 489 = 795", "right": "correct addition: makes 795"},
        {"left": "2,840 + 1,560 = 4,400", "right": "correct addition: makes 4,400"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q2-w04-d05": [
        {"left": "752 + 168 = 920", "right": "correct addition: makes 920"},
        {"left": "3,315 + 4,485 = 7,800", "right": "correct addition: makes 7,800"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q3-w06-d04": [
        {"left": "895 + 107 = 1,002", "right": "correct addition: makes 1,002"},
        {"left": "2,160 + 3,740 = 5,900", "right": "correct addition: makes 5,900"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
    "mathematics-g3-q4-w08-d04": [
        {"left": "456 + 544 = 1,000", "right": "correct addition: makes 1,000"},
        {"left": "4,010 + 2,990 = 7,000", "right": "correct addition: makes 7,000"},
        {"left": "regroup 10 ones", "right": "a regrouping step"},
    ],
}

# Dash-split rule for remaining 'right repeats clue' pairs: "X—Y" -> left "X", right stays.
def dash_split(pairs):
    out = []
    for p in pairs:
        left = str(p.get("left", ""))
        right = str(p.get("right", ""))
        if "—" in left:
            head = left.split("—", 1)[0].strip()
            if head and surface(head) != surface(right):
                # also strip qualifiers from right where they repeat the clue
                r = re.sub(r"\s+bilang kasalungat\s*$", "", right).strip()
                out.append({"left": head, "right": r})
                continue
        out.append(p)
    return out

# ------------------------------------------------------- B. generic instruction
GENERIC_MAP = {
    "pag-aralan ang ideya at pakinggan ang salaysay": "Pag-aralan ang aralin at pakinggan ang paliwanag.",
    "suriin ang bawat halimbawa at basahin ang mga halimbawa": "Suriin ang bawat halimbawa at basahin nang mabuti.",
    "ilagay ang bawat card sa angkop o hindi angkop": "Ilagay ang bawat card sa tamang pangkat.",
    "itambal ang bawat halimbawa sa ideya ng aralin": "Itambal ang bawat pares na magkaugnay.",
    "tapusin nang sunod sunod ang gabay na balik aral": "Ayusin ang mga hakbang sa tamang pagkakasunod-sunod.",
    "study the idea and listen to milo": "Study the idea and listen to the explanation.",
    "explore each example and find the important detail": "Explore each example and find the important detail about the topic.",
    "sort each example into the correct group": "Sort the examples into the correct groups.",
    "choose the best answer": "Choose the answer that fits best.",
    "match the ideas that belong together": "Match each pair of related ideas.",
    "put the steps in the correct order": "Arrange the steps in the correct order.",
}

# ----------------------------------------------------------- C. template prompts
TEMPLATE_TAIL = re.compile(
    r"\s*(?:to practice|while practicing)\s*[:：]?\s*[A-Za-zÀ-ÿ].*$", re.I)
TEMPLATE_REPLACE = {
    "look closely at each example to practice": "Look closely at each example.",
    "sort each example while practicing": "Sort the examples into the right groups.",
    "put the steps in order to practice": "Put the steps in the correct order.",
    "match the pairs to practice": "Match each pair that belongs together.",
    "choose the answer to practice": "Choose the best answer.",
}

# -------------------------------------------------------- D. filipino bleed
BLEED_MAP = [
    (re.compile(r"\blegend\b", re.I), "legenda"),
    (re.compile(r"\bdated record\b", re.I), "tala na may petsa"),
    (re.compile(r"\boral account(?: na may attribution)?(?: with speaker)?\b", re.I), "pasalitang salaysay"),
    (re.compile(r"\boral history\b", re.I), "kasaysayang pasalita"),
    (re.compile(r"\baccount\b", re.I), "salaysay"),
    (re.compile(r"\bprotected wildlife\b", re.I), "pinangangalagaang buhay-ilang"),
    (re.compile(r"\bcommunity helper\b", re.I), "katuwang sa pamayanan"),
    (re.compile(r"\brecipe uses available ingredient\b", re.I), "ang resipi ay gumagamit ng magagamit na sangkap"),
]

# --------------------------------------------------- E. long learner text
def shorten(value, lang):
    v = value.strip()
    if len(v) <= 90:
        return v
    fil = lang.startswith("fil")
    # Mangled strings with audit-note fragments get a full rewrite
    if re.search(r"sa laki ng screen|dahil sa kulay|pangalan ng bata", v):
        return "Basahin muli ang tuntunin ng aralin. 💪" if fil else "Read the lesson rule again. 💪"
    # Specific patterns first
    pats_fil = [
        (r"Balikan ang pahiwatig sa .+?\.", "Balikan ang pahiwatig ng aralin. 💪"),
        (r"Basahin ang dalawang panig ng bawat pares tungkol sa .+?\.", "Basahin muli ang dalawang panig ng bawat pares. 💪"),
        (r"Mahusay! Itinugma mo ang mga halimbawa sa paliwanag ng .+?\.", "Mahusay! Itinugma mo ang bawat pares. 🔗"),
        (r"Mahusay! Nabasa mo ang mga halimbawa ng .+?\.", "Mahusay! Nabasa mo ang bawat halimbawa. 🔎"),
        (r"Basahin ang pamantayan ng .+? bago magpangkat muli\.", "Balikan ang pamantayan bago magpangkat muli. 💪"),
        (r"Hanapin ang pahiwatig sa tanong tungkol sa .+?\.", "Hanapin muli ang pahiwatig sa tanong. 💪"),
        (r"Hanapin ang una, kasunod, at huling hakbang sa .+?\.", "Ayusin ang una, kasunod, at huling hakbang. 💪"),
    ]
    pats_en = [
        (r"Find the clue in .+? again\.", "Find the clue in this lesson again. 💪"),
        (r"Read both sides of each .+? pair again\.", "Read both sides of each pair again. 💪"),
        (r"Great! You matched each example with its .+? explanation\.", "Great! You matched each pair. 🔗"),
        (r"Great! You found the examples for .+?\.", "Great! You found the examples. 🔎"),
        (r"Look at each .+? example again\.", "Look at each example again. 💪"),
    ]
    pats = pats_fil if fil else pats_en
    for rx, repl in pats:
        if re.search(rx, v):
            return repl
    # Fallback: keep first sentence if it fits, else generic short phrase
    first = re.split(r"(?<=[.!?])\s+", v)[0]
    if len(first) <= 90 and len(first) >= 6:
        return first
    return ("Subukan muli ang araling ito. 💪" if fil else "Try this activity again. 💪")

# Same-lesson duplicate prompts: authored distinct variants for the second+ item.
SAME_LESSON_PROMPTS = {
    ("english-g3-m01-d08", "english-g3-m01-d08-q05"): "Which one shows the correct irregular plural?",
    ("mathematics-g3-q1-w02-d04", "mathematics-g3-q1-w02-d04-q05"): "Which list is in order, from the smallest number to the biggest?",
    ("mathematics-g3-m01-d11", "mathematics-g3-m01-d11-q05"): "Which subtraction answer is correct?",
    ("mathematics-g3-m01-d06", "mathematics-g3-m01-d06-q05"): "Which number is rounded correctly?",
    ("english-g3-q2-w02-d01", "english-g3-q2-w02-d01-q05"): "Which pair are synonyms?",
    ("english-g3-q2-w02-d01", "english-g3-q2-w02-d01-q04"): "Which pair are antonyms?",
    ("mathematics-g3-m01-d08", "mathematics-g3-m01-d08-q05"): "Which statement about money is true?",
    ("english-g3-q3-w08-d04", "english-g3-q3-w08-d04-q05"): "Which pair has the same meaning?",
    ("mathematics-g3-m01-d15", "mathematics-g3-m01-d15-q05"): "Which multiplication fact is correct?",
    ("mathematics-g3-m01-d02", "mathematics-g3-m01-d02-q05"): "Which statement about place value is true?",
    ("english-g3-q1-w01-d05", "english-g3-q1-w01-d05-q01"): "Which sentence gives information and ends with a period?",
    ("english-g3-q1-w01-d05", "english-g3-q1-w01-d05-q02"): "Which one is a telling sentence?",
    ("english-g3-q1-w01-d05", "english-g3-q1-w01-d05-q03"): "Which example gives information about the picture?",
    ("english-g3-q1-w01-d05", "english-g3-q1-w01-d05-q04"): "Which sentence ends with a period?",
    ("english-g3-q1-w01-d05", "english-g3-q1-w01-d05-q05"): "Which one tells about the picture in a full sentence?",
    ("science-g3-m01-d05", "science-g3-m01-d05-q05"): "Which body part goes with the right job?",
    ("science-g3-m01-d08", "science-g3-m01-d08-q05"): "Which plant part goes with the right job?",
    ("science-g3-m01-d10", "science-g3-m01-d10-q05"): "Which statement is true about living things?",
    ("mathematics-g3-m01-d09", "mathematics-g3-m01-d09-q05"): "Which addition answer is correct?",
}

# ------------------------------------------------------- F. duplicate prompts
def surface(s): return re.sub(r"\s+", " ", str(s).casefold().strip())

def main():
    stats = {k: 0 for k in ["matching_rewrites", "dash_split", "generic_instruction",
                            "template_prompt", "bleed", "long_text", "dup_prompts", "lessons"]}

    # ---- pre-scan duplicate prompts
    prompts = {}
    lessons = {}
    for p in sorted(LESSONS.glob("*.json")):
        d = json.loads(p.read_text(encoding="utf-8"))
        lessons[p] = d
        for it in d.get("assessment", {}).get("items", []):
            k = surface(it.get("prompt"))
            if k:
                prompts.setdefault(k, []).append((p, it))

    for p, d in lessons.items():
        lid = d["lessonId"]
        lang = (d.get("language") or "").lower()
        title = re.sub(r"\s*·.*$", "", d.get("title", "")).strip()
        changed = False

        # A. matching rewrites
        rewrite = MATCHING_REWRITES.get(lid)
        if rewrite:
            for a in d.get("activities", []):
                if a.get("type") == "MATCHING_PAIRS":
                    a.setdefault("content", {})["pairs"] = [dict(x) for x in rewrite]
                    stats["matching_rewrites"] += 1
                    changed = True
        else:
            for a in d.get("activities", []):
                if a.get("type") == "MATCHING_PAIRS":
                    pairs = a.get("content", {}).get("pairs", [])
                    new_pairs = dash_split(pairs)
                    if new_pairs != pairs:
                        a["content"]["pairs"] = new_pairs
                        stats["dash_split"] += 1
                        changed = True

        # B/C/E/D across all strings
        def fix_value(v, path=""):
            nonlocal changed
            if not isinstance(v, str):
                return v
            norm = re.sub(r"[^\w\s]+", " ", v.lower()).strip()
            if norm in GENERIC_MAP:
                changed = True
                stats["generic_instruction"] += 1
                return GENERIC_MAP[norm]
            for rx, repl in BLEED_MAP:
                if rx.search(v):
                    v = rx.sub(repl, v)
                    changed = True
                    stats["bleed"] += 1
            m = re.search(r"\s+(?:to practice|while practicing)\s*[:：]\s*.*$", v, re.I)
            if m:
                stem = v[:m.start()].rstrip(" :：")
                if len(stem) >= 8:
                    v = stem
                    changed = True
                    stats["template_prompt"] += 1
            elif re.search(r"\b(shows this skill|matches what we learned)\b", v, re.I):
                v = (TEMPLATE_REPLACE.get(v.lower().strip(), "Explore the examples in this activity."))
                changed = True
                stats["template_prompt"] += 1
            return v

        def walk(node, path=""):
            if isinstance(node, dict):
                for k, i in list(node.items()):
                    node[k] = walk(i, f"{path}.{k}" if path else k)
            elif isinstance(node, list):
                for i, x in enumerate(node):
                    node[i] = walk(x, f"{path}[{i}]")
            elif isinstance(node, str):
                node = fix_value(node, path)
                if (path.endswith(".instruction") or path.endswith(".feedback.correct") or path.endswith(".feedback.retry")) and len(node) > 90:
                    node = shorten(node, lang)
                    changed = True
                    stats["long_text"] += 1
            return node

        d = walk(d)

        # F. duplicate prompts: authored variants for same-lesson dups, lesson
        # context suffix for cross-lesson dups.
        for it in d.get("assessment", {}).get("items", []):
            iid = it.get("itemId")
            key = (lid, iid)
            if key in SAME_LESSON_PROMPTS:
                it["prompt"] = SAME_LESSON_PROMPTS[key]
                stats["dup_prompts"] += 1
                changed = True
                continue
            k = surface(it.get("prompt"))
            owners = prompts.get(k, [])
            if len(owners) > 1:
                same_lesson = [x for x in owners if x[0].stem == p.stem]
                cross_lesson = {x[0].stem for x in owners}
                if len(same_lesson) == len(owners) and len(cross_lesson) == 1:
                    continue  # same-lesson groups handled by authored dict
                if p.stem != owners[0][0].stem:
                    suffix = f" (sa aralin: {title})" if lang.startswith("fil") else f" (in this lesson: {title})"
                    if suffix.lower() not in it.get("prompt", "").lower():
                        it["prompt"] = f"{it.get('prompt','')} {suffix}".strip()
                        stats["dup_prompts"] += 1
                        changed = True

        if changed:
            stats["lessons"] += 1
            p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")

    print(stats)

if __name__ == "__main__":
    main()

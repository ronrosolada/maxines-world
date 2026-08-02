#!/usr/bin/env python3
"""
Enrich lesson vocabulary: ensure every playable lesson carries 3+ real
vocabulary terms drawn from the lesson's own text, with child-friendly
definitions (8-year-old level) in the lesson's language.

Existing vocabulary is kept untouched. New terms are matched against the
lesson title/objective/introduction via the curated dictionary below, so
the terms are ALWAYS authentic to the lesson content — never generic.

Usage:
  python3 tools/enrich_vocabulary.py --dry-run   # show what would change
  python3 tools/enrich_vocabulary.py             # apply (idempotent)
"""

import argparse
import json
import re
import sys
from pathlib import Path

PACK_DIR = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"
MIN_TERMS = 3
MAX_TERMS = 5

# ─── Child-friendly definitions (8yo level), by term ─────────────────────
# English definitions for english/mathematics/science lessons; Tagalog
# definitions for filipino/gmrc/makabansa lessons. Terms are matched
# against each lesson's own text, so only relevant terms get added.

EN_DEFS = {
    "addition": "Putting numbers together to find a total.",
    "air": "The invisible gas we breathe all around us.",
    "animal": "A living thing that can move and needs food.",
    "array": "Objects arranged in equal rows and columns.",
    "attract": "To pull toward, like a magnet pulling metal.",
    "bar graph": "A picture chart using bars to show how many.",
    "beginning": "The start of a story or sentence.",
    "blend": "To join letter sounds together to read a word.",
    "body": "All the parts that make up a person or animal.",
    "capacity": "How much a container can hold.",
    "capital letter": "A big letter used at the start of a sentence.",
    "cause": "Why something happens.",
    "centavo": "A small coin in Philippine money.",
    "character": "A person or animal in a story.",
    "classify": "To sort things into groups that are alike.",
    "compare": "To look at how things are the same or different.",
    "consonant": "A letter that is not a vowel, like b, c, or d.",
    "data": "Information collected to answer a question.",
    "detail": "A small piece of information in a story.",
    "difference": "The answer when you subtract.",
    "digit": "One symbol used to write a number, like 0-9.",
    "digraph": "Two letters that make one sound, like sh or ch.",
    "division": "Sharing things into equal groups.",
    "ecosystem": "Living things and their home working together.",
    "effect": "What happens because of a cause.",
    "electricity": "Energy that makes lights and machines work.",
    "ending": "The last part of a story or word.",
    "energy": "The power to do work, like light or heat.",
    "environment": "Everything around us — land, water, and air.",
    "equal groups": "Groups that have the same number of things.",
    "estimate": "A careful guess about a number or size.",
    "force": "A push or a pull on something.",
    "future": "The time that is still to come.",
    "gas": "Matter that spreads out and fills the air, like steam.",
    "geometry": "The study of shapes, lines, and angles.",
    "habitat": "The natural home of a plant or animal.",
    "hearing": "The sense that lets us listen with our ears.",
    "heat": "Energy that makes things warm.",
    "hundred": "Ten groups of ten — written as 100.",
    "length": "How long something is from end to end.",
    "light": "Energy that lets us see things.",
    "line": "A straight mark that goes on and on.",
    "liquid": "Matter that flows and takes the shape of its cup.",
    "magnet": "A metal object that pulls iron and steel.",
    "main idea": "What a story or paragraph is mostly about.",
    "material": "What something is made of, like wood or plastic.",
    "matter": "Anything that takes up space — solid, liquid, or gas.",
    "money": "Coins and bills we use to buy things.",
    "multiplication": "Adding the same number again and again.",
    "noun": "A word that names a person, place, or thing.",
    "number": "A symbol or word that tells how many.",
    "observe": "To look closely and notice what happens.",
    "one": "The number that comes first when counting.",
    "order": "Arranging things from first to last.",
    "past": "The time that has already happened.",
    "pattern": "A repeating order you can predict.",
    "period": "A dot that ends a sentence.",
    "peso": "The main money of the Philippines.",
    "place value": "What a digit is worth by its position.",
    "plant": "A living thing that grows in soil and makes food.",
    "plural": "More than one, like cats or boxes.",
    "point": "An exact spot in space, marked with a dot.",
    "possessive": "Showing who owns something, like Milo's.",
    "predicate": "The part of a sentence that tells the action.",
    "present": "The time happening right now.",
    "product": "The answer when you multiply.",
    "pull": "To bring something closer to you.",
    "punctuation": "Marks like . ! ? that help us read clearly.",
    "push": "To move something away from you.",
    "quotient": "The answer when you divide.",
    "ray": "A line with one endpoint that goes one way.",
    "reduce": "To use less of something.",
    "regroup": "To trade ten ones for one ten when adding or subtracting.",
    "repel": "To push away, like two same magnet ends.",
    "retell": "To tell a story again in your own words.",
    "round": "To find a number close to a friendly number like 10.",
    "sense": "How we learn — seeing, hearing, touching, tasting, smelling.",
    "sentence": "A group of words that tells a complete idea.",
    "sequence": "The order of events in a story.",
    "setting": "Where and when a story happens.",
    "shelter": "A safe place that protects living things.",
    "sight word": "A word you know by sight without sounding out.",
    "soil": "The dirt where plants grow.",
    "solid": "Matter with its own shape, like a rock.",
    "sound": "Something you hear with your ears.",
    "story": "A tale with characters and events.",
    "subject": "The part of a sentence that tells who or what.",
    "subtraction": "Taking away to find what is left.",
    "sum": "The answer when you add.",
    "sun": "The star that gives us light and warmth.",
    "syllable": "A part of a word with one vowel sound.",
    "temperature": "How hot or cold something is.",
    "ten": "The number after nine — written as 10.",
    "thousand": "Ten hundreds — written as 1,000.",
    "title": "The name of a book, story, or lesson.",
    "touch": "The sense that lets us feel with our skin.",
    "verb": "An action word, like run, jump, or sing.",
    "vowel": "The letters a, e, i, o, u.",
    "waste": "Things we throw away.",
    "water": "The liquid all living things need.",
    "weather": "What the sky and air are like each day.",
    "wind": "Moving air you can feel.",
    "smell": "The sense that lets us notice scents with our nose.",
    "taste": "The sense that lets us enjoy food with our tongue.",
    "shadow": "A dark shape made when light is blocked.",
    "season": "A time of year with its own weather.",
    "life cycle": "How a living thing is born, grows, and changes.",
    "seed": "The part of a plant that can grow into a new plant.",
    "root": "The part of a plant that holds it in the soil.",
    "stem": "The part of a plant that carries water up.",
    "leaf": "The green part of a plant that makes food.",
    "flower": "The colorful part of a plant that makes seeds.",
    "fruit": "The part of a plant that holds seeds.",
    "food chain": "Who eats what in nature, from plant to animal.",
    "predator": "An animal that hunts other animals.",
    "prey": "An animal that is hunted for food.",
    "recycle": "To make old things into new things.",
    "reuse": "To use something again instead of throwing it away.",
    "picture": "An image or drawing that shows something.",
    "detective": "A person who looks for clues to find the answer.",
    "clue": "A hint that helps you solve a mystery.",
    "diary": "A little book where you write about your day.",
    "write": "To put words and letters on paper.",
    "main idea": "What a story or paragraph is mostly about.",
    "place value": "The value of a digit by where it sits in a number.",
    "value": "What a number or a digit is worth.",
    "symbol": "A sign or mark that stands for something.",
    "word": "A group of letters with a meaning.",
    "home": "The place where a person or animal lives.",
    "need": "Something you must have to live, like food and water.",
    "motion": "The way something moves from one place to another.",
    "reference": "A starting point used to tell where something is.",
}

AP_DEFS = {
    "map": "A drawing that shows where places are.",
    "symbol": "A picture or sign that stands for something.",
    "province": "A place in the Philippines made of towns and cities.",
    "population": "The number of people who live in a place.",
    "physical features": "The land and water shapes of a place, like mountains and rivers.",
    "landform": "A shape on the land, like a mountain or a hill.",
    "water form": "A body of water, like a river, lake, or sea.",
    "natural resources": "Gifts of nature like water, trees, and soil.",
    "hero": "A brave person who helps others and serves the country.",
    "community": "The people living and working together in one place.",
    "culture": "The traditions, food, music, and way of life of a place.",
    "tradition": "A custom or practice passed from older to younger people.",
    "livelihood": "The work people do to earn a living.",
    "region": "A big part of the country with its own culture.",
    "evidence": "Something that shows a fact is true.",
    "hazard": "A danger that can harm people or property.",
    "continuity": "The things that stay the same through time.",
    "change": "The things that become different over time.",
    "identity": "What makes a person or a place special and unique.",
    "historical places": "Old places that tell stories of the past.",
    "hymn": "A song of praise, like a provincial song.",
    "arts": "Paintings, music, dances, and crafts of a place.",
    "service": "Work done to help other people.",
    "natural resources": "Gifts of nature like water, trees, and soil.",
    "land": "The solid part of the earth, not covered by water.",
    "water": "The clear liquid in rivers, lakes, and seas.",
    "location": "The place where something is found.",
    "protect": "To keep something safe from harm.",
    "environment": "The world around us — land, water, air, and living things.",
    "story": "A tale about people, places, or events.",
    "place": "A spot or area where something is.",
    "practice": "A custom or a habit done again and again.",
    "history": "The story of what happened in the past.",
    "heritage": "The treasures from the past, like old buildings and customs.",
    "picture": "An image or drawing that shows something.",
    "graph": "A drawing that shows numbers or amounts.",
    "provincial": "Belonging to a province.",
}

TL_DEFS = {"paghahambing": "Ang paghahambing ng dalawang bagay o tao.",
    "pareho": "Kaparehas o magkapantay ang dalawang bagay.",
    "kapwa": "Ang bawat isa o ang lahat ng tao.",
    "kaysa": "Salitang ginagamit sa paghahambing ng dalawang bagay.",
    "ngunit": "Salitang nag-uugnay ng dalawang magkaibang diwa.",
    "mamamayan": "Ang mga taong naninirahan sa isang bayan o bansa.",
    "disiplina": "Pagsunod sa mga alituntunin at tamang gawi.",
    "kabutihang asal": "Mabubuting ugali na dapat taglayin ng bawat bata.",
    "kasalungat": "Salitang may kabaligtaran o magkaibang kahulugan.",
    "kasaysayan": "Ang mga pangyayari sa nakaraan ng ating bayan.",
    "kasingkahulugan": "Salitang may pareho o katulad na kahulugan.",
    "komunidad": "Ang pamayanan kung saan tayo naninirahan.",
    "konteksto": "Ang mga salitang kasama na tumutulong sa kahulugan.",
    "kultura": "Ang mga tradisyon, sining, at gawi ng isang lugar.",
    "kuwento": "Isang salaysay tungkol sa mga tauhan at pangyayari.",
    "lalawigan": "Isang malaking bahagi ng bansa na may sariling pamahalaan.",
    "letra": "Mga titik na bumubuo ng mga salita.",
    "mabuti": "May mabuting puso at tama ang gawain.",
    "magalang": "Gumagamit ng po at opo at magandang pananalita.",
    "pagkakakilanlan": "Kung sino tayo — ang ating pangalan, kultura, at bayan.",
    "pagmamahal": "Ang pag-aalaga at pagpapahalaga sa kapwa.",
    "pahiwatig": "Mga palatandaan na tumutulong sa pag-unawa ng salita.",
    "paksa": "Ang pinag-uusapan o pinag-aaralan sa teksto.",
    "pamagat": "Ang pangalan ng kuwento o aklat.",
    "panghalip": "Salitang pamalit sa pangngalan, tulad ng siya, kami.",
    "pangngalan": "Salitang tumutukoy sa tao, bagay, lugar, o hayop.",
    "bagay": "Anumang bagay o gamit sa ating paligid.",
    "kaalaman": "Ang mga alam o natutuhang impormasyon.",
    "tunog": "Ang naririnig natin, tulad ng tunog ng boses o instrumento.",
    "sundin": "Ang pagsunod sa mga panuto o utos.",
    "hakbang": "Ang mga hakbang o sunod-sunod na bahagi ng isang gawain.",
    "pamalit": "Ang salitang ginagamit na kapalit ng pangngalan.",
    "pagsasalaysay": "Ang pagkukuwento o pagbabahagi ng mga pangyayari.",
    "tugma": "Ang pagkakapareho ng tunog sa dulo ng mga salita.",
    "paghinuha": "Ang paghuhula o pag-unawa batay sa mga pahiwatig.",
    "teksto": "Ang mga nakasulat o nababasang talata.",
    "sagot": "Ang tugon o sagot sa isang tanong.",
    "tanong": "Ang itinatanong o hinahanap na impormasyon.",
    "pangungusap": "Grupo ng mga salita na may buong diwa.",
    "pantig": "Bahagi ng salita na may isang tunog ng patinig.",
    "panuto": "Ang mga utos o gabay kung paano gawin ang isang bagay.",
    "payak": "Simple at hindi tambalan — isang diwa lamang.",
    "rehiyon": "Malaking bahagi ng bansa na may magkakatulad na kultura.",
    "responsable": "May pananagutan sa kanyang mga gawain.",
    "salita": "Mga tunog na may kahulugan na ating binabasa at sinusulat.",
    "nagagamit": "Nagagamit nang wasto ang salita sa pangungusap.",
    "natutuhang": "Ang mga salita o kaalamang natutuhan sa aralin.",
    "nababasa": "Ang mga salitang kayang basahin nang wasto.",
    "naisusulat": "Ang mga salitang kayang isulat nang tama.",
    "magagalang": "Mga salitang magalang, tulad ng po at opo.",
    "pagbati": "Ang mga bati tulad ng magandang umaga at kumusta.",
    "ekspresyon": "Ang mga salitang nagpapakita ng damdamin.",
    "sitwasyon": "Ang mga pangyayari o pagkakataon sa paligid.",
    "nakapagpapahayag": "Ang pagpapahayag ng sariling ideya o damdamin.",
    "huwaran": "Ang modelo o halimbawang sinusunod.",
    "organisasyon": "Ang maayos na pagkakaayos ng mga ideya o bahagi.",
    "impormatibo": "Nagbibigay ng impormasyon o kaalaman.",
    "natutukoy": "Ang pagkilala o pagtukoy sa isang bagay.",
    "maayos": "Ang pagiging malinis at tama ang ayos.",
    "damdamin": "Ang nararamdaman, tulad ng saya, lungkot, at galit.",
    "tamang": "Ang wasto at akma sa sitwasyon.",
    "pananalita": "Ang paraan ng pagsasalita o mga sinasabi.",
    "nakabubuod": "Ang pagbubuod o pagbubuod ng mga pangyayari.",
    "naratibo": "Ang uri ng teksto na nagkukuwento.",
    "nakikilala": "Ang pagkilala sa mga bagay o tao.",
    "kahulugan": "Ang ibig sabihin ng isang salita o pangungusap.",
    "sugnay": "Bahagi ng pangungusap na may simuno at panaguri.",
    "tagpuan": "Ang lugar at panahon kung saan naganap ang kuwento.",
    "talaan ng nilalaman": "Listahan ng mga paksa at pahina sa aklat.",
    "talasalitaan": "Ang mga salitang alam at ginagamit ng isang tao.",
    "talata": "Grupo ng mga pangungusap tungkol sa isang paksa.",
    "tambalang": "Dalawang payak na pangungusap na pinagsama.",
    "tauhan": "Ang mga tao o hayop sa kuwento.",
    "tiwala sa sarili": "Paniniwala sa sariling kakayahan.",
    "wakas": "Ang huling bahagi ng kuwento.",
    "simula": "Ang unang bahagi ng kuwento.",
    "hanap-salita": "Laro kung saan hinahanap ang mga nakatagong salita.",
    "pabalat": "Ang takip ng aklat na may pamagat.",
    "glosaryo": "Listahan ng mahihirap na salita at kahulugan nito.",
    "diksyunaryo": "Aklat na may mga salita at kahulugan, ayos ayon sa alpabeto.",
    "pandiwa": "Salitang kilos o galaw, tulad ng tumakbo, kumain.",
    "pang-uri": "Salitang naglalarawan sa pangngalan, tulad ng maganda, malaki.",
    "pang-abay": "Salitang naglalarawan sa pandiwa, pang-uri, o kapwa pang-abay.",
    "parirala": "Grupo ng mga salita na walang buong diwa.",
    "tula": "Mga salitang may tugma at ritmo.",
    "taludtod": "Isang linya sa isang tula.",
    "saknong": "Grupo ng mga taludtod sa isang tula.",
    "bugtong": "Palaisipan na may nakatagong sagot.",
    "salawikain": "Mga kasabihang may aral.",
    "sawikain": "Mga pahayag na may ibang kahulugan kaysa literal.",
    "palaisipan": "Mga tanong o laro na nagpapaisip.",
    "pabula": "Kuwento tungkol sa mga hayop na may aral.",
    "alamat": "Kuwento tungkol sa pinagmulan ng mga bagay.",
    "epiko": "Mahabang kuwento tungkol sa mga bayani.",
    "mito": "Kuwento tungkol sa mga diyos at diyosa.",
    "pamayanan": "Ang lugar na may mga taong magkakasamang naninirahan.",
    "karapatan": "Mga bagay na nararapat sa bawat bata at mamamayan.",
    "tungkulin": "Mga gawain na dapat gampanan ng bawat isa.",
    "tradisyon": "Mga kaugalian na ipinapasa mula sa mga ninuno.",
    "kaugalian": "Mga nakagawiang gawi ng isang pamilya o lugar.",
    "bayani": "Taong may nagawang kabutihan para sa bayan.",
    "kalayaan": "Ang kalagayan ng pagiging malaya.",
    "pamahalaan": "Ang pangkat na namamahala sa isang bayan o bansa.",
    "bayanihan": "Ang pagtutulungan ng mga tao sa pamayanan.",
    "watawat": "Ang bandila ng isang bansa.",
    "awit": "Isang kanta o himig.",
    "pambansang": "Para sa buong bansa, tulad ng pambansang awit.",
    "kasuotan": "Ang mga damit na isinusuot ng mga tao.",
    "pagkain": "Ang mga kinakain natin araw-araw.",
    "pamanang": "Mga yaman o gawi na minana natin.",
    "kagitingan": "Ang katapangan at kabayanihan.",
    "katapatan": "Pagsasabi ng totoo at pagiging tapat.",
    "pagtitimpi": "Pagkontrol sa sariling galit o kilos.",
    "pagkakaisa": "Ang pagkakasundo at pagtutulungan ng lahat.",
    "pagtulong": "Ang pagbibigay ng tulong sa nangangailangan.",
    "pamilya": "Ang mga taong magkakamag-anak at nagmamahalan.",
    "pananagutan": "Ang pagiging responsable sa sariling gawain.",
    "pag-aalaga": "Ang pag-iingat at pagprotekta sa iba.",
    "kalinisan": "Ang pagiging malinis ng katawan at paligid.",
    "kaayusan": "Ang pagiging maayos ng mga bagay-bagay.",
    "tapat": "Nagsasabi ng totoo at hindi nandaraya.",
    "masipag": "Masiglang gumagawa at hindi tamad.",
    "matiyaga": "Hindi sumusuko kahit mahirap ang gawain.",
    "kabaitan": "Ang pagiging mabait sa kapwa.",
    "matulungin": "Handang tumulong sa mga nangangailangan.",
    "pakikipagkapwa": "Ang pakikisama at paggalang sa kapwa tao.",
    "paggalang": "Ang pagpapakita ng respeto sa iba.",
    "ugnay": "Ang koneksyon o kaugnayan ng mga bagay.",
    "indeks": "Listahan sa hulihan ng aklat ng mga paksa at pahina.",
    "ponema": "Ang pinakamaliit na tunog ng isang salita.",
}

# Which language's definitions to use per pack subject
# Curated fallback for legacy template-shell lessons whose bodies are too
# generic for text-grounded extraction. Terms come from the lesson TITLE
# topic. Only used when a lesson would otherwise stay under MIN_TERMS.
CURATED_FALLBACK = {
    "araling-panlipunan-g3-m01-d02": ["map", "region", "province"],
    "araling-panlipunan-g3-m01-d03": ["population", "graph", "picture"],
    "araling-panlipunan-g3-m01-d05": ["land", "water", "map"],
    "araling-panlipunan-g3-m01-d07": ["hazard", "map", "location"],
    "araling-panlipunan-g3-m01-d08": ["natural resources", "protect", "environment"],
    "araling-panlipunan-g3-m01-d09": ["map", "environment", "hazard"],
    "araling-panlipunan-g3-m01-d13": ["symbol", "province", "story"],
    "araling-panlipunan-g3-m01-d14": ["hymn", "arts", "culture"],
    "araling-panlipunan-g3-m01-d17": ["culture", "practice", "tradition"],
    "araling-panlipunan-g3-m01-d19": ["historical places", "history", "evidence"],
    "english-g3-m01-d01": ["detective", "clue", "picture"],
    "english-g3-m01-d04": ["diary", "write", "story"],
    "english-g3-m01-d18": ["detail", "main idea", "clue"],
    "filipino-g3-m01-d01": ["pangngalan", "salita", "bagay"],
    "filipino-g3-m01-d02": ["salita", "kaalaman", "pangungusap"],
    "filipino-g3-m01-d03": ["teksto", "sagot", "tanong"],
    "filipino-g3-m01-d05": ["salita", "pantig", "tunog"],
    "filipino-g3-m01-d06": ["panuto", "sundin", "hakbang"],
    "filipino-g3-m01-d09": ["panghalip", "pangngalan", "pamalit"],
    "filipino-g3-m01-d12": ["salita", "pagsasalaysay", "kuwento"],
    "filipino-g3-m01-d14": ["salita", "tugma", "tunog"],
    "filipino-g3-m01-d19": ["pahiwatig", "paghinuha", "kuwento"],
    "mathematics-g3-m01-d02": ["digit", "place value", "value"],
    "mathematics-g3-m01-d03": ["number", "symbol", "word"],
    "science-g3-m01-d13": ["home", "need", "environment"],
    "science-g3-m01-d16": ["motion", "point", "reference"],
}

SUBJECT_DEFS = {
    "filipino": TL_DEFS,
    "gmrc": TL_DEFS,
    "makabansa": TL_DEFS,
    "english": EN_DEFS,
    "mathematics": EN_DEFS,
    "science": EN_DEFS,
    "ARALING_PANLIPUNAN": AP_DEFS,
    "FILIPINO": TL_DEFS,
    "ENGLISH": EN_DEFS,
    "MATHEMATICS": EN_DEFS,
    "SCIENCE": EN_DEFS,
}

# Per-subject term regex (terms that actually appear in lesson text)
SUBJECT_TERM_RE = {
    "english": r"\b(sight word|sentence|plural|possessive|noun|verb|adjective|syllable|digraph|blend|vowel|consonant|cause|effect|detail|sequence|ending|beginning|story|character|setting|retell|main idea|punctuation|past|present|future|subject|predicate|capital letter|title|paragraph)\b",
    "mathematics": r"\b(number|digit|place value|thousand|hundred|ten|one|addition|subtraction|multiplication|division|sum|difference|product|quotient|estimate|round|compare|order|capacity|length|pattern|geometry|point|line|ray|array|equal groups|regroup|money|peso|centavo|data|bar graph|temperature)\b",
    "science": r"\b(observe|classify|material|solid|liquid|gas|matter|float|sink|dissolve|mixture|living thing|non-living thing|plant|animal|habitat|body|sense|hearing|touch|taste|smell|light|shadow|sound|heat|force|push|pull|magnet|attract|repel|weather|temperature|cloud|rain|wind|sun|season|soil|water|air|energy|electricity|life cycle|seed|root|stem|leaf|flower|fruit|shelter|food chain|predator|prey|ecosystem|environment|recycle|reduce|reuse|waste)\b",
    "gmrc": r"\b(kabutihang asal|tiwala sa sarili|responsable|matulungin|magalang|mabait|paggalang|pakikipagkapwa|katapatan|pagtitimpi|disiplina|pagkakaisa|pagtulong|pamilya|komunidad|pananagutan|pagmamahal|pag-aalaga|kalinisan|kaayusan|tapat|mabuti|masipag|matiyaga|kabaitan)\b",
    "makabansa": r"\b(bayan|komunidad|mamamayan|kultura|kasaysayan|pamayanan|lalawigan|rehiyon|pambansa|watawat|awit|pambansang|karapatan|tungkulin|pagkakakilanlan|tradisyon|kaugalian|pamanang|kagitingan|bayani|kalayaan|pamahalaan|bayanihan|kasuotan|pagkain)\b",
    "filipino": r"\b(pangungusap|talasalitaan|sugnay|tambalang|payak|diwa|paksa|glosaryo|diksyunaryo|pabalat|talaan ng nilalaman|indeks|pamagat|talata|taludtod|saknong|tula|kuwento|tauhan|tagpuan|simula|wakas|pang-uri|pang-abay|panghalip|pandiwa|pangngalan|pantig|ponema|letra|salita|kasingkahulugan|kasalungat|pahiwatig|konteksto|panuto|hanap-salita|pabula|alamat|epiko|mito|bugtong|salawikain|sawikain|palaisipan|parirala)\b",
    "ARALING_PANLIPUNAN": r"\b(bayan|komunidad|kultura|kasaysayan|lalawigan|rehiyon|karapatan|tungkulin|bayani|pamahalaan|pagkakakilanlan)\b",
    "FILIPINO": r"\b(pangungusap|talasalitaan|paksa|talata|kuwento|tauhan|tagpuan|pangngalan|panghalip|pandiwa|pantig|salita|panuto)\b",
    "ENGLISH": r"\b(sentence|noun|verb|story|character|detail|sequence|ending|blend|digraph|vowel|consonant|syllable|plural|possessive)\b",
    "MATHEMATICS": r"\b(number|digit|addition|subtraction|multiplication|division|sum|difference|place value|estimate|round|compare|pattern|capacity|length|money|peso)\b",
    "SCIENCE": r"\b(observe|classify|material|solid|liquid|gas|plant|animal|habitat|sense|light|sound|heat|force|magnet|weather|water|soil|energy|environment)\b",
}


def extract_terms(subject: str, text: str) -> list[str]:
    """
    Dictionary-driven term extraction: every candidate comes from the
    lesson's own text, matched against the curated definitions with
    inflected-form variants (salitang/salita, regrouping/regroup,
    multiplied/multiply, pictures/picture). Ordered by first occurrence
    so terms follow the lesson flow.
    """
    low = text.lower()
    defs = SUBJECT_DEFS.get(subject, EN_DEFS)
    hits = []
    for term in defs:
        base = re.escape(term)
        # Variant suffixes: Tagalog linkers + English inflection
        variants = [base, base + "ng", base + "g", base + "na",
                    base + "s", base + "es", base + "ing", base + "ed"]
        for v in variants:
            m = re.search(rf"(?<![a-zñ]){v}(?![a-zñ])", low)
            if m:
                hits.append((m.start(), term))
                break
    return [t for _, t in sorted(hits)]


def lesson_text(lesson: dict) -> str:
    """
    All readable text of a lesson: title, objective, intro, activities,
    and assessment QUESTION prompts. Assessment OPTIONS are deliberately
    excluded — they contain distractors (wrong answers), which are not
    vocabulary the child is meant to learn.
    """
    parts = [
        lesson.get("title", ""),
        lesson.get("objective", ""),
        lesson.get("introduction", ""),
    ]
    for a in lesson.get("activities", []):
        parts += [a.get("prompt", ""), a.get("narration", "") or "", a.get("instruction", "") or ""]
    for it in (lesson.get("assessment", {}) or {}).get("items", []):
        parts.append(it.get("prompt", ""))
    return " ".join(parts)


def is_placeholder(term: dict) -> bool:
    """Legacy placeholders: 'A key word used in the lesson …' or empty/circular defs."""
    t = (term.get("term", "") or "").strip()
    d = (term.get("definition", "") or "").strip()
    if not t or not d:
        return True
    if d.lower() == t.lower():
        return True
    return "key word used in the lesson" in d.lower() or "placeholder" in d.lower()


def enrich_lesson(lesson: dict) -> list[str]:
    """Return the terms ADDED to this lesson (empty if none needed).

    Drops legacy placeholder entries first (they count toward the minimum
    but teach nothing), then adds real terms from the lesson's own text.
    Lessons still thin after that get a curated title-topic fallback that
    REPLACES the vocabulary (removes cross-language/template junk).
    """
    subject = lesson.get("subject", "")
    defs = SUBJECT_DEFS.get(subject, EN_DEFS)
    vocab = lesson.get("vocabulary", [])
    kept = [v for v in vocab if not is_placeholder(v)]
    if len(kept) != len(vocab):
        lesson["vocabulary"] = kept
    existing = {v.get("term", "").lower() for v in kept}
    if len(existing) >= MIN_TERMS:
        return []

    candidates = [t for t in extract_terms(subject, lesson_text(lesson)) if t in defs and t.lower() not in existing]

    added = []
    for term in candidates:
        if len(existing) + len(added) >= MIN_TERMS:
            break
        added.append({"term": term, "definition": defs[term]})

    # Curated fallback: title-topic terms, replaces the (junk) vocab entirely
    lesson_id = lesson.get("lessonId", "")
    if len(existing) + len(added) < MIN_TERMS and lesson_id in CURATED_FALLBACK:
        curated = [t for t in CURATED_FALLBACK[lesson_id] if t in defs]
        lesson["vocabulary"] = [{"term": t, "definition": defs[t]} for t in curated]
        added = curated
        return added

    if added:
        lesson.setdefault("vocabulary", []).extend(added)
    return [a["term"] for a in added]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not PACK_DIR.is_dir():
        print(f"ERROR: pack dir not found: {PACK_DIR}", file=sys.stderr)
        return 1

    changed = 0
    added_total = 0
    still_thin = []
    for json_file in sorted(PACK_DIR.glob("*.json")):
        lesson = json.loads(json_file.read_text())
        added = enrich_lesson(lesson)
        if added:
            changed += 1
            added_total += len(added)
            if not args.dry_run:
                json_file.write_text(json.dumps(lesson, indent=1, ensure_ascii=False) + "\n")
        if len(lesson.get("vocabulary", [])) < MIN_TERMS:
            still_thin.append((json_file.name, len(lesson.get("vocabulary", []))))

    print(f"[{'dry-run' if args.dry_run else 'applied'}]")
    print(f"  lessons enriched: {changed}")
    print(f"  terms added: {added_total}")
    print(f"  lessons still < {MIN_TERMS} terms: {len(still_thin)}")
    for name, n in still_thin[:10]:
        print(f"    {name}: {n} term(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Enrich generic boilerplate explanations in media-assessments.json with Grade-3 pedagogical explanations."""
from __future__ import annotations

import json
from pathlib import Path

MEDIA_PATH = Path("/opt/data/projects/maxines-world/android/app/src/main/assets/content-pack/media-assessments.json")

# Verified pedagogical explanations for all 78 prompt patterns
EXPLANATION_MAP = {
    # English & Grammar
    ("Choose the subject for: '___ are playing.'", "The children"):
        "'The children' is the plural subject performing the action of playing.",
    ("In 'The thirsty dog drank water,' what does thirsty mean?", "needing a drink"):
        "'Thirsty' describes the physical need to drink water or fluids.",
    ("What does the prefix un- usually mean?", "not or opposite of"):
        "The prefix 'un-' is added to the beginning of a root word to mean 'not' or the opposite (like unhappy or unlock).",
    ("What should a reader do when a word is unfamiliar?", "Use context clues and reread"):
        "Good readers look at nearby words for context clues and reread sentences to figure out unfamiliar words.",
    ("Which clue helps you infer how a character feels?", "The character's words and actions"):
        "A character's spoken words, facial expressions, and actions reveal their internal thoughts and feelings.",
    ("Which detail best shows the setting of a story?", "The story happens in a quiet garden"):
        "The setting tells where and when a story takes place, such as in a quiet garden.",
    ("Which sentence has a clear beginning, middle, and end?", "First we packed, then we traveled, and finally we arrived."):
        "Sequence transition words like 'First', 'then', and 'finally' organize events into beginning, middle, and end.",
    ("Which sentence is complete?", "The bird sings."):
        "A complete sentence contains both a subject ('The bird') and a predicate ('sings') expressing a complete thought.",
    ("Which sentence uses the pronoun correctly?", "She reads a book."):
        "'She' is the correct subject pronoun used to refer to a female person performing the action.",
    ("Which word best completes: 'The rabbit ___ quickly.'?", "hops"):
        "'Hops' is the singular present-tense action verb that agrees with the singular subject 'The rabbit'.",
    ("Which word is a noun in: 'The puppy runs.'?", "puppy"):
        "'Puppy' is a noun because it names a specific animal.",
    ("Which word is a verb in: 'Mia jumps high.'?", "jumps"):
        "'Jumps' is an action verb describing what Mia is physically doing.",
    ("Which word is an adjective in: 'The red kite flies.'?", "red"):
        "'Red' is a descriptive adjective that tells the color of the noun 'kite'.",
    ("Which word means the opposite of early?", "late"):
        "'Late' is the exact antonym (opposite) of 'early'.",
    ("Which word means the same as happy?", "glad"):
        "'Glad' and 'happy' are synonyms that express positive emotion and joy.",
    ("Which word names a person, place, animal, or thing?", "noun"):
        "A noun is a part of speech that names a person, place, animal, or thing.",
    ("Why do readers put events in sequence?", "To show the order in which they happen"):
        "Sequencing arranges story events in chronological order from beginning to end so readers understand what happened.",

    # Filipino & Makabansa / GMRC
    ("Para saan ginagamit ang mapa?", "Upang malaman ang lokasyon at direksyon"):
        "Ang mapa ay isang visual na representasyon na nagpapakita ng lokasyon, hangganan, at direksyon ng mga lugar.",
    ("Sino ang tumutulong sa pag-aalaga ng kalusugan sa komunidad?", "doktor o nars"):
        "Ang mga doktor at nars ay mga community health workers na gumagamot at nag-aalaga sa kalusugan ng mga mamamayan.",
    ("Lito planted a seed, watered it, and saw a sprout. What happened first?", "Lito planted a seed"):
        "The first chronological event in planting is placing the seed in the soil before watering and sprouting.",

    # Math
    ("If one toy costs ₱8 and you buy two, how much do you pay?", "₱16"):
        "Multiply the unit cost by the quantity: ₱8 × 2 = ₱16 total cost.",
    ("In 1,110, which digit is in the hundreds place?", "1"):
        "In the number 1,110, the digit in the hundreds place (third from the right) is 1 (representing 100).",
    ("In 1,112, which digit is in the hundreds place?", "1"):
        "In the number 1,112, the digit in the hundreds place (third from the right) is 1 (representing 100).",
    ("In 1,310, which digit is in the hundreds place?", "3"):
        "In the number 1,310, the digit in the hundreds place is 3 (representing 300).",
    ("In 1,331, which digit is in the hundreds place?", "3"):
        "In the number 1,331, the digit in the hundreds place is 3 (representing 300).",
    ("In 1,576, which digit is in the hundreds place?", "5"):
        "In the number 1,576, the digit in the hundreds place is 5 (representing 500).",
    ("In 1,645, which digit is in the hundreds place?", "6"):
        "In the number 1,645, the digit in the hundreds place is 6 (representing 600).",
    ("In 1,654, which digit is in the hundreds place?", "6"):
        "In the number 1,654, the digit in the hundreds place is 6 (representing 600).",
    ("In 1,763, which digit is in the hundreds place?", "7"):
        "In the number 1,763, the digit in the hundreds place is 7 (representing 700).",
    ("What should you do first when solving a word problem?", "Read and understand the problem"):
        "The essential first step in problem solving is reading carefully to identify what is given and what is being asked.",
    ("Which is the expanded form of 1,110?", "1,000 + 100 + 10 + 0"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 100 (hundreds) + 10 (tens) + 0 (ones).",
    ("Which is the expanded form of 1,112?", "1,000 + 100 + 10 + 2"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 100 (hundreds) + 10 (tens) + 2 (ones).",
    ("Which is the expanded form of 1,310?", "1,000 + 300 + 10 + 0"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 300 (hundreds) + 10 (tens) + 0 (ones).",
    ("Which is the expanded form of 1,331?", "1,000 + 300 + 30 + 1"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 300 (hundreds) + 30 (tens) + 1 (ones).",
    ("Which is the expanded form of 1,576?", "1,000 + 500 + 70 + 6"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 500 (hundreds) + 70 (tens) + 6 (ones).",
    ("Which is the expanded form of 1,645?", "1,000 + 600 + 40 + 5"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 600 (hundreds) + 40 (tens) + 5 (ones).",
    ("Which is the expanded form of 1,654?", "1,000 + 600 + 50 + 4"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 600 (hundreds) + 50 (tens) + 4 (ones).",
    ("Which is the expanded form of 1,763?", "1,000 + 700 + 60 + 3"):
        "Expanded form breaks the number into place values: 1,000 (thousands) + 700 (hundreds) + 60 (tens) + 3 (ones).",
    ("Which number has 1 hundreds?", "100"):
        "The number 100 consists of exactly 1 group of one hundred.",
    ("Which number has 3 hundreds?", "300"):
        "The number 300 consists of exactly 3 groups of one hundred.",
    ("Which number has 5 hundreds?", "500"):
        "The number 500 consists of exactly 5 groups of one hundred.",
    ("Which number has 6 hundreds?", "600"):
        "The number 600 consists of exactly 6 groups of one hundred.",
    ("Which number has 7 hundreds?", "700"):
        "The number 700 consists of exactly 7 groups of one hundred.",
    ("Which number is even: 21, 22, 24, or 26?", "22"):
        "An even number ends in 0, 2, 4, 6, or 8 and can be divided into 2 equal parts without a remainder.",
    ("Which number is even: 41, 42, 44, or 46?", "42"):
        "42 ends in the digit 2, making it an even number divisible by 2.",
    ("Which number is even: 53, 54, 56, or 58?", "54"):
        "54 ends in the digit 4, making it an even number.",
    ("Which number is even: 54, 55, 57, or 59?", "54"):
        "54 ends in 4, which is an even digit, while 55, 57, and 59 end in odd digits.",
    ("Which number is even: 59, 60, 62, or 64?", "60"):
        "60 ends in 0, making it an even number.",
    ("Which number is greater than 1,110?", "1,210"):
        "Comparing thousands and hundreds: 1,210 has 2 hundreds while 1,110 has only 1 hundred, so 1,210 > 1,110.",
    ("Which number is greater than 1,112?", "1,212"):
        "1,212 has 2 hundreds, which is greater than 1,112 which has 1 hundred.",
    ("Which number is greater than 1,310?", "1,410"):
        "1,410 has 4 hundreds, which is greater than 1,310 which has 3 hundreds.",
    ("Which number is greater than 1,331?", "1,431"):
        "1,431 has 4 hundreds, which is greater than 1,331 which has 3 hundreds.",
    ("Which number is greater than 1,576?", "1,676"):
        "1,676 has 6 hundreds, which is greater than 1,576 which has 5 hundreds.",
    ("Which number is greater than 1,645?", "1,745"):
        "1,745 has 7 hundreds, which is greater than 1,645 which has 6 hundreds.",
    ("Which number is greater than 1,654?", "1,754"):
        "1,754 has 7 hundreds, which is greater than 1,654 which has 6 hundreds.",
    ("Which number is greater than 1,763?", "1,863"):
        "1,863 has 8 hundreds, which is greater than 1,763 which has 7 hundreds.",
    ("Which operation checks an addition answer?", "subtraction"):
        "Subtraction is the inverse (opposite) operation of addition used to verify sums.",
    ("Which symbol makes this true: 21 __ 22?", "<"):
        "21 is less than 22, so the 'less than' symbol (<) makes the mathematical sentence true.",
    ("Which symbol makes this true: 41 __ 42?", "<"):
        "41 is less than 42, so the 'less than' symbol (<) makes the comparison true.",
    ("Which symbol makes this true: 53 __ 54?", "<"):
        "53 is smaller than 54, represented by the '<' symbol.",
    ("Which symbol makes this true: 54 __ 55?", "<"):
        "54 is smaller than 55, represented by the '<' symbol.",
    ("Which symbol makes this true: 59 __ 60?", "<"):
        "59 is smaller than 60, represented by the '<' symbol.",
    ("Which tool measures the mass of an object?", "weighing scale"):
        "A weighing scale or balance measures the mass and weight of physical objects.",
    ("Which unit is best for measuring the length of a pencil?", "centimeters"):
        "Centimeters (cm) are the standard metric unit suited for measuring small classroom objects like pencils.",
    ("Which unit is best for the amount of water in a bottle?", "liters"):
        "Liters (L) and milliliters (mL) are the standard metric units for measuring liquid capacity and volume.",

    # Science
    ("What happens when ice is warmed?", "It melts into liquid water"):
        "Adding heat energy causes solid ice to increase in temperature and melt into liquid water.",
    ("What kind of weather brings water drops from clouds?", "rainy"):
        "Rainy weather occurs when water droplets condensed in clouds become heavy enough to fall as precipitation.",
    ("Where does rainwater collect after falling?", "in rivers, lakes, or the ground"):
        "Rainwater flows as surface runoff into streams, rivers, and lakes, or infiltrates the ground as groundwater.",
    ("Which action helps reduce waste?", "Reuse a clean container"):
        "Reusing existing containers prevents unnecessary trash from entering landfills.",
    ("Which example is a gas?", "air"):
        "Air is a mixture of gases (such as nitrogen and oxygen) that has no definite shape or volume.",
    ("Which example shows a change that can be observed?", "A puddle dries in sunlight"):
        "Sunlight heats surface water, causing observable evaporation as the puddle dries up.",
    ("Which is a natural resource?", "water"):
        "Water is a vital natural resource provided by Earth that living organisms need to survive.",
    ("Which observation uses sight?", "The leaf is green"):
        "Observing the color of a leaf is a visual observation made using our eyes.",
    ("Which property can we observe without changing a material?", "color"):
        "Color is a physical property observable directly without chemically altering the substance.",
    ("Which state of matter keeps its own shape?", "solid"):
        "A solid has tightly packed particles that maintain a fixed, definite shape.",
    ("Which tool measures temperature?", "thermometer"):
        "A thermometer is a scientific instrument designed to measure thermal energy and temperature in degrees.",
    ("Why do scientists record observations?", "To compare evidence and explain what happened"):
        "Recording clear observations allows scientists to analyze factual evidence and formulate accurate scientific explanations.",
    ("Why do we observe weather?", "To prepare for conditions outside"):
        "Tracking weather patterns helps us choose appropriate clothing and stay safe during outdoor activities.",
}


def main():
    data = json.loads(MEDIA_PATH.read_text(encoding="utf-8"))
    
    replaced = 0
    missing = []
    
    for m in data.get("media", []):
        for it in m.get("items", []):
            expl = it.get("explanation", "")
            if "matches the concept being checked" in expl or "matches the skill" in expl:
                cid = it.get("correctOptionIds", [""])[0]
                corr_text = next((o["text"] for o in it.get("options", []) if o["id"] == cid), "").strip()
                prompt = it.get("prompt", "").strip()
                
                key = (prompt, corr_text)
                if key in EXPLANATION_MAP:
                    it["explanation"] = EXPLANATION_MAP[key]
                    replaced += 1
                else:
                    missing.append((m.get("mediaId"), it.get("itemId"), prompt, corr_text))

    print(f"Successfully replaced {replaced} generic explanations.")
    if missing:
        print(f"WARNING: {len(missing)} items not found in map:")
        for mis in missing:
            print(" ", mis)
        return 1

    MEDIA_PATH.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Saved updated {MEDIA_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

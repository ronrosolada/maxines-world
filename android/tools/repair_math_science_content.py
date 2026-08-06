#!/usr/bin/env python3
"""Repair mathematics and science quarterly lessons (stock-junk sweep).

Second content-repair wave (after repair_english_skill_content.py):
the quarterly generator duplicated every lesson instance within a skill
group and filled assessment options with stock junk — wrong-operation
strings for math ("write an unlabeled number", "compare symbols without
finding a product", ...) and generic safety strings for science ("taste
an unknown material", "look directly at the Sun", ...).

Each group gets authored content tables; math assessment items are
GENERATED from the lesson's own equations with near-miss numeric
distractors (real computation practice, not recognition). Science items
use cross-set real examples as distractors.

Groups covered (43 lessons):
  mathematics: addition ≤ 10,000 (9), multiplication (9)
  science: living/non-living (7), material properties (11), light/sound (7)

Idempotent: repairing an already-repaired lesson is a no-op (correct
strings come from the authored tables, so detection is stable).

Usage:
    python3 tools/repair_math_science_content.py [--dry-run|--check]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"

# --------------------------------------------------------------------------
# Mathematics: addition (9 lesson instances → 9 equation sets)
# --------------------------------------------------------------------------
ADD_SETS = [
    ("245 + 123 = 368", "2,650 + 1,200 = 3,850"),
    ("432 + 516 = 948", "1,830 + 2,740 = 4,570"),
    ("671 + 229 = 900", "3,500 + 1,250 = 4,750"),
    ("118 + 264 = 382", "4,260 + 2,380 = 6,640"),
    ("573 + 248 = 821", "1,905 + 2,995 = 4,900"),
    ("306 + 489 = 795", "2,840 + 1,560 = 4,400"),
    ("752 + 168 = 920", "3,315 + 4,485 = 7,800"),
    ("895 + 107 = 1,002", "2,160 + 3,740 = 5,900"),
    ("456 + 544 = 1,000", "4,010 + 2,990 = 7,000"),
]

# --------------------------------------------------------------------------
# Mathematics: multiplication
# --------------------------------------------------------------------------
MUL_SETS = [
    ("3 groups of 4 = 12", "6 × 5 = 30", "2 × 14 = 28"),
    ("4 groups of 3 = 12", "5 × 7 = 35", "3 × 12 = 36"),
    ("2 groups of 6 = 12", "8 × 4 = 32", "4 × 11 = 44"),
    ("5 groups of 3 = 15", "7 × 6 = 42", "2 × 23 = 46"),
    ("3 groups of 7 = 21", "9 × 3 = 27", "5 × 12 = 60"),
    ("4 groups of 5 = 20", "6 × 8 = 48", "3 × 15 = 45"),
    ("2 groups of 9 = 18", "7 × 7 = 49", "4 × 13 = 52"),
    ("5 groups of 6 = 30", "8 × 8 = 64", "6 × 11 = 66"),
    ("3 groups of 8 = 24", "9 × 4 = 36", "7 × 12 = 84"),
]

# --------------------------------------------------------------------------
# Science: living / non-living (7 lesson instances)
# --------------------------------------------------------------------------
LIVING_SETS = [
    ("a mango tree — living", "a dog — living", "a rock — non-living",
     "roots help a plant take in water", "What body part helps a plant take in water?",
     "roots", ["wings", "fur", "wheels"]),
    ("a bird — living", "a fish — living", "a pencil — non-living",
     "a fish uses gills to breathe", "What do fish use to breathe?",
     "gills", ["wings", "feet", "fur"]),
    ("a cat — living", "a banana plant — living", "a spoon — non-living",
     "leaves make food for the plant", "Which part makes food for the plant?",
     "leaves", ["roots", "fur", "wheels"]),
    ("an ant — living", "a duck — living", "a chair — non-living",
     "a bird uses wings to fly", "What do birds use to fly?",
     "wings", ["gills", "roots", "scales"]),
    ("a butterfly — living", "a cow — living", "a stone — non-living",
     "a dog needs food and water to live", "What does a dog need to live?",
     "food and water", ["a battery", "a plastic bag", "a remote control"]),
    ("a frog — living", "a bamboo — living", "a cup — non-living",
     "roots hold the plant in the ground", "What holds a plant in the ground?",
     "roots", ["leaves", "wings", "scales"]),
    ("a horse — living", "a rose — living", "a key — non-living",
     "a fish needs water to live", "What does a fish need to live?",
     "water", ["sand", "a blanket", "a toy"]),
]
LIVING_NONLIVING_POOL = [
    "a rock", "water", "a pencil", "a spoon", "a chair", "a stone",
    "a cup", "a key", "a toy car", "a book", "scissors", "a rubber band",
]

# --------------------------------------------------------------------------
# Science: material properties (11 lesson instances)
# --------------------------------------------------------------------------
MATERIAL_SETS = [
    [("metal spoon", "hard and shiny"), ("rubber band", "flexible"),
     ("paper towel", "absorbs water"), ("wooden ruler", "hard and useful for measuring")],
    [("glass jar", "see-through"), ("cotton cloth", "soft"),
     ("sponge", "absorbs water"), ("stone", "hard and heavy")],
    [("aluminum foil", "shiny and bends easily"), ("rope", "strong"),
     ("clay", "soft and moldable"), ("plastic cup", "light")],
    [("rubber ball", "bounces"), ("denim", "thick and tough"),
     ("wax paper", "does not absorb water"), ("metal key", "hard")],
    [("eggshell", "fragile and breaks easily"), ("cardboard", "stiff"),
     ("yarn", "soft and bendable"), ("glass window", "see-through")],
    [("wooden chair", "hard and sturdy"), ("cotton t-shirt", "soft"),
     ("plastic bottle", "keeps water in"), ("sandpaper", "rough")],
    [("mirror", "very shiny"), ("wool scarf", "warm and soft"),
     ("metal nail", "hard"), ("paper bag", "tears easily")],
    [("rubber duck", "floats in water"), ("clay pot", "heavy"),
     ("silk ribbon", "smooth"), ("chalk", "soft and dusty")],
    [("steel pan", "hard and strong"), ("bamboo stick", "strong and light"),
     ("tissue paper", "thin and tears easily"), ("crayon", "soft and waxy")],
    [("wooden spoon", "hard"), ("bubble wrap", "soft and poppy"),
     ("denim bag", "tough"), ("glass cup", "see-through")],
    [("rubber slippers", "flexible"), ("cotton towel", "absorbs water"),
     ("metal fork", "hard and shiny"), ("paper plate", "light and flimsy")],
]

# --------------------------------------------------------------------------
# Science: light and sound (7 lesson instances)
# --------------------------------------------------------------------------
LIGHT_SETS = [
    ("a vibrating guitar string makes sound", "a flashlight helps us see",
     "move away from very loud speakers", "never stare at the Sun",
     "Which makes sound?", "a vibrating guitar string",
     ["a silent rock", "a folded blanket", "a still chair"]),
    ("clapping hands makes sound", "a lamp lights a dark room",
     "cover your ears at a very loud concert", "do not point a laser at your eyes",
     "Which helps us see in a dark room?", "a lamp",
     ["a loud shout", "a warm blanket", "a fast car"]),
    ("a drum makes sound when struck", "sunlight helps plants grow",
     "use sunglasses to protect your eyes in strong sun", "do not look directly at a bright light",
     "What makes sound when struck?", "a drum",
     ["a pillow", "a sponge", "a folded towel"]),
    ("a bell rings when shaken", "a mirror reflects light",
     "turn on a lamp to read in a dark room", "stay far from loud machines",
     "Which reflects light?", "a mirror",
     ["a sponge", "a sock", "a paper bag"]),
    ("a whistle makes a high sound", "a shadow forms when light is blocked",
     "give your ears a rest from loud noise", "never shine a flashlight into someone's eyes",
     "What forms when light is blocked?", "a shadow",
     ["a shadow", "a loud echo", "a warm spot", "a bright flash"]),
    ("a radio plays music", "a window lets light through",
     "use a flashlight to walk in a dark place", "do not play with fire near paper",
     "Which lets light through?", "a window",
     ["a brick wall", "a wooden door", "a metal box"]),
    ("a dog barks loudly", "a rainbow appears when light bends",
     "move away if a sound hurts your ears", "do not point a laser at your eyes",
     "What appears when light bends?", "a rainbow",
     ["a shadow", "an echo", "a flash"]),
]

# --------------------------------------------------------------------------
# Shared scaffolding
# --------------------------------------------------------------------------
SEQUENCES = {
    "math-add": ["Read the problem: {c[a_w]} plus {c[b_w]}", "Add the ones",
                 "Add the tens and hundreds", "Check with an estimate"],
    "math-mul": ["Read the problem: {c[g_w]} groups of {c[x_w]}", "Find the equal groups",
                 "Multiply carefully", "Check the product"],
    "sci-living": ["Observe the example: {c[l1]}", "Ask: does it grow?", "Check what it needs", "Sort it as living or not"],
    "sci-materials": ["Observe the material safely", "Name its property", "Compare with other materials", "Choose the safe use"],
    "sci-light": ["Observe the light or sound", "Think about how it behaves", "Decide the safe action", "Protect your eyes and ears"],
}

# Per-skill, per-activity-type instruction shells (lesson-specific; format()
# receives the skill context dict `c`). Replaces the stock instructions the
# generator stamped onto every lesson.
SHELLS = {
    "math-add": {
        "ANIMATED_EXPLANATION": "Listen to Milo. He will show you how to add from ones to thousands.",
        "HOTSPOT_IMAGE": "Explore each example. {c[a_w]} plus {c[b_w]} equals {c[s_w]}.",
        "SORT_AND_CLASSIFY": "Sort the examples for {c[a_w]} plus {c[b_w]}. Keep the real addition steps and move the wrong operations out.",
        "MULTIPLE_CHOICE": "Choose the best answer. What is {c[a_w]} plus {c[b_w]}?",
        "MATCHING_PAIRS": "Match the steps for {c[a_w]} plus {c[b_w]}, like regrouping 10 ones as 1 ten.",
        "SEQUENCE_BUILDER": "Put the adding steps in order, starting with the ones place.",
    },
    "math-mul": {
        "ANIMATED_EXPLANATION": "Listen to Milo. He will show you how multiplication uses equal groups.",
        "HOTSPOT_IMAGE": "Explore each example. {c[g_w]} groups of {c[x_w]} equals {c[gx_w]}.",
        "SORT_AND_CLASSIFY": "Sort the multiplication ideas for {c[g_w]} groups of {c[x_w]}.",
        "MULTIPLE_CHOICE": "Choose the best answer. What is {c[f1_w]} times {c[f2_w]}?",
        "MATCHING_PAIRS": "Match the ideas for {c[g_w]} groups of {c[x_w]}: equal groups, the product, or multiplying tens and ones.",
        "SEQUENCE_BUILDER": "Put the steps in order. Read the problem first, then find the equal groups.",
    },
    "sci-living": {
        "ANIMATED_EXPLANATION": "Listen to Milo. He will help you tell living things from non-living things.",
        "HOTSPOT_IMAGE": "Explore each example. {c[l1]} and {c[l2]} are living, but {c[nl1]} is not.",
        "SORT_AND_CLASSIFY": "Sort the examples: {c[l1]} and {c[l2]} are living things.",
        "MULTIPLE_CHOICE": "Choose the best answer. Ask: does it grow, and does it need food and water?",
        "MATCHING_PAIRS": "Match {c[l1]}, {c[l2]}, and {c[nl1]} with their groups.",
        "SEQUENCE_BUILDER": "Put the steps in order. Observe first, then decide if the thing is living.",
    },
    "sci-materials": {
        "ANIMATED_EXPLANATION": "Listen to Milo. He will show you how to describe materials by their properties.",
        "HOTSPOT_IMAGE": "Explore each example. {c[mats0]} is {c[prop0]}, and {c[mats1]} is {c[prop1]}.",
        "SORT_AND_CLASSIFY": "Sort the examples. Keep the ones that describe a material's property.",
        "MULTIPLE_CHOICE": "Choose the best answer. Think about how the material looks, feels, or behaves.",
        "MATCHING_PAIRS": "Match each material with the property it shows.",
        "SEQUENCE_BUILDER": "Put the steps in order. Observe the material safely, then name its property.",
    },
    "sci-light": {
        "ANIMATED_EXPLANATION": "Listen to Milo. He will show you how light and sound behave, and how to use them safely.",
        "HOTSPOT_IMAGE": "Explore each example. {c[ex_sound]}. {c[ex_light]}.",
        "SORT_AND_CLASSIFY": "Sort the examples into ways we use light and sound safely.",
        "MULTIPLE_CHOICE": "Choose the best answer. Think about what makes sound and what helps us see.",
        "MATCHING_PAIRS": "Match each example with what it does: makes sound, helps us see, or protects hearing.",
        "SEQUENCE_BUILDER": "Put the steps in order. Observe the light or sound, then decide the safe action.",
    },
}

# Per-skill ANIMATED_EXPLANATION content (what Milo reads on step 1). Empty
# string keeps the existing text.
SHELL_INTROS = {
    "math-add": "Add from ones to thousands. {c[a_w]} plus {c[b_w]} equals {c[s_w]}. "
                "Regroup 10 ones as 1 ten when needed.",
    "math-mul": "Multiplication joins equal groups. {c[g_w]} groups of {c[x_w]} equals {c[gx_w]}. "
                "The multiplication sentence names the product.",
    "sci-living": "{c[l1]} is a living thing: it grows and needs food and water. "
                  "{c[nl1]} is non-living, so it does not grow.",
    "sci-materials": "Materials have properties. {c[mats0]} is {c[prop0]}, and "
                     "{c[mats1]} is {c[prop1]}.",
    "sci-light": "{c[ex_sound]}. {c[ex_light]}. Always use light and sound safely.",
}

# Stock strings that must not appear in options / prompts / vocabulary.
# (Wrong-operation statements like "subtract the addends" are INTENTIONAL
# sort/assessment content — they teach the operation boundary. Only the
# truly generic nonsense strings are flagged.)
MATH_JUNK = ["write an unlabeled number"]
SCIENCE_JUNK = [
    "taste an unknown material",
    "use a dangerous tool alone",
    "make a claim without observing",
]

ADD_VOCAB = [
    {"term": "addend", "definition": "a number that is added to another number"},
    {"term": "sum", "definition": "the answer when you add"},
    {"term": "regroup", "definition": "to trade 10 ones for 1 ten (or 10 tens for 1 hundred)"},
]
MUL_VOCAB = [
    {"term": "factor", "definition": "a number that is multiplied by another number"},
    {"term": "product", "definition": "the answer when you multiply"},
    {"term": "equal groups", "definition": "groups that all have the same number of items"},
]
LIVING_VOCAB = [
    {"term": "living thing", "definition": "something that grows, needs food and water, and can respond"},
    {"term": "non-living thing", "definition": "something that does not grow or need food and water"},
    {"term": "basic need", "definition": "something a living thing must have to live, like food and water"},
]
MATERIAL_VOCAB = [
    {"term": "property", "definition": "how a material looks, feels, or behaves"},
    {"term": "flexible", "definition": "able to bend without breaking"},
    {"term": "absorb", "definition": "to soak up a liquid"},
]
LIGHT_VOCAB = [
    {"term": "sound", "definition": "what you hear when something vibrates"},
    {"term": "light", "definition": "what helps us see"},
    {"term": "vibrate", "definition": "to move back and forth quickly"},
]


def find_skill(lesson):
    o = lesson["objective"]
    subj = lesson["subject"]
    if subj == "mathematics":
        if o.startswith("Add numbers with sums up to 10,000"):
            return "math-add"
        if o.startswith("Multiply numbers by using place value"):
            return "math-mul"
    if subj == "science":
        if o.startswith("Classify familiar examples as living"):
            return "sci-living"
        if o.startswith("Describe familiar materials by observable"):
            return "sci-materials"
        if o.startswith("Describe how light and sound behave"):
            return "sci-light"
    return None


def group_ids(skill):
    return sorted(
        p.stem for p in LESSONS.glob("*.json")
        if find_skill(json.loads(p.read_text())) == skill
    )


def near_miss_sums(correct):
    """3 wrong sums near `correct`: ±10, +100 (all ≥ 0, unique, positive)."""
    cands = [correct + 10, correct + 100, correct - 10, correct - 100, correct + 1]
    out = []
    for c in cands:
        if c != correct and c >= 0 and c not in out:
            out.append(c)
        if len(out) == 3:
            break
    return out


def near_miss_products(f1, f2, correct):
    cands = [(f1 + 1) * f2, f1 * (f2 + 1), (f1 - 1) * f2, f1 * (f2 - 1), correct + 10]
    out = []
    for c in cands:
        if c != correct and c > 0 and c not in out:
            out.append(c)
        if len(out) == 3:
            break
    return [fmt(c) for c in out]


def uniq3(correct, cands):
    """3 strings distinct from `correct` and from each other, from cands."""
    out = []
    for c in cands:
        s = str(c)
        if s != str(correct) and s not in out:
            out.append(s)
        if len(out) == 3:
            break
    return out


def fmt(n):
    if isinstance(n, str):
        return n
    s = f"{n:,}" if n >= 1000 else str(n)
    return s


_ONES = ["", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
         "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
         "seventeen", "eighteen", "nineteen"]
_TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]


def numwords(n: int) -> str:
    """Number in English words (≤ 9,999): 368 -> 'three hundred sixty eight'."""
    if n == 0:
        return "zero"
    if n < 20:
        return _ONES[n]
    if n < 100:
        return (_TENS[n // 10] + (" " + _ONES[n % 10] if n % 10 else "")).strip()
    if n < 1000:
        head = _ONES[n // 100] + " hundred"
        rest = numwords(n % 100)
        return head + (" " + rest if rest else "")
    head = numwords(n // 1000) + " thousand"
    rest = numwords(n % 1000)
    return head + (" " + rest if rest else "")


def repair_lesson(lesson):
    skill = find_skill(lesson)
    if skill is None:
        return None
    ids = group_ids(skill)
    idx = ids.index(lesson["lessonId"])
    day = int(lesson["lessonId"].rsplit("d", 1)[1])
    correct_at = (day - 1) % 4

    if skill == "math-add":
        eq1, eq2 = ADD_SETS[idx % len(ADD_SETS)]
        a, b, s = eq1.replace(",", "").replace(" ", "").replace("+", " ").replace("=", " ").split()
        sum1 = int(s)
        est_a, est_b = round(int(a), -2), round(int(b), -2)
        est = est_a + est_b
        est_dists = uniq3(fmt(est), [fmt(est + 100), fmt(est - 100), fmt(sum1 + 10),
                                     fmt(sum1 + 100), fmt(sum1 - 10)])
        items = [
            (f"What is {numwords(int(a))} plus {numwords(int(b))}?",
             uniq3(fmt(sum1), near_miss_sums(sum1)), fmt(sum1)),
            (f"What is the sum of {numwords(int(a))} and {numwords(int(b))}?",
             uniq3(fmt(sum1), near_miss_sums(sum1)), fmt(sum1)),
            ("Which number sentence is correct for {a_w} plus {b_w}?".format(a_w=numwords(int(a)), b_w=numwords(int(b))),
             [f"{a} + {b} = {fmt(n)}" for n in near_miss_sums(sum1)], eq1),
            (f"When you add {numwords(int(a))} and {numwords(int(b))}, which place do you add first?",
             ["tens", "hundreds", "thousands"], "ones"),
            (f"Which is a good estimate for {numwords(int(a))} plus {numwords(int(b))}?",
             est_dists, fmt(est)),
        ]
        matching = [
            {"left": eq1, "right": "adds from ones to thousands"},
            {"left": eq2, "right": "keeps place values aligned"},
            {"left": "regroup 10 ones", "right": "trades 10 ones for 1 ten"},
        ]
        vocab = ADD_VOCAB
        sort_fits = [eq1, eq2, "regroup 10 ones", "check with an estimate"]
        sort_other = ["subtract the addends", "multiply the numbers",
                      "compare without finding a sum", "divide the addends"]
        c = {"eq1": eq1, "eq2": eq2,
             "a_w": numwords(int(a)), "b_w": numwords(int(b)), "s_w": numwords(sum1)}
        hotspot = [eq1, eq2, "regroup 10 ones", "check with an estimate"]

    elif skill == "math-mul":
        eq_groups, eq_x, eq_2d = MUL_SETS[idx % len(MUL_SETS)]
        g, x = eq_groups.split(" groups of ")
        x = x.split(" = ")[0]
        f1, rest = eq_x.replace(" ", "").split("×")
        f2, prod = rest.split("=")
        prod = int(prod); f1, f2 = int(f1), int(f2)
        items = [
            (f"What is {numwords(f1)} times {numwords(f2)}?", near_miss_products(f1, f2, prod), fmt(prod)),
            (f"{numwords(int(g))} groups of {numwords(int(x))} is the same as which product?",
             near_miss_products(int(g), int(x), int(g) * int(x)), fmt(int(g) * int(x))),
            (("Which multiplication sentence is correct for {g} groups of {x}?").format(g=numwords(int(g)), x=numwords(int(x))),
             [eq_x] + [f"{f1} × {f2} = {fmt(n)}" for n in near_miss_products(f1, f2, prod)], eq_x),
            (f"'{numwords(int(g))} groups of {numwords(int(x))}' shows what?",
             [f"{x} objects in each of {g} groups", f"{g} objects in each of {x} groups",
              f"{g} objects in one group", f"{int(g) * int(x)} objects in one group"],
             f"{x} objects in each of {g} groups"),
            ("Which problem uses equal groups?",
             [f"{g} groups of {x}", f"{g} + {x}", f"{g} − {x}", f"half of {g}"],
             f"{g} groups of {x}"),
        ]
        matching = [
            {"left": eq_groups, "right": "shows equal groups"},
            {"left": eq_x, "right": "names the product"},
            {"left": eq_2d, "right": "multiplies tens and ones"},
        ]
        vocab = MUL_VOCAB
        sort_fits = [eq_groups, eq_x, eq_2d, "estimate before solving"]
        sort_other = ["subtract equal groups", "add a different number of groups",
                      "compare symbols without finding a product", "divide the factors before"]
        c = {"eq_groups": eq_groups, "eq_x": eq_x, "eq_2d": eq_2d,
             "g_w": numwords(int(g)), "x_w": numwords(int(x)),
             "gx_w": numwords(int(g) * int(x)),
             "f1_w": numwords(f1), "f2_w": numwords(f2)}
        hotspot = [eq_groups, eq_x, eq_2d, "estimate before solving"]

    elif skill == "sci-living":
        l1, l2, nl1, need_sentence, need_q, need_ans, need_wrongs = LIVING_SETS[idx % len(LIVING_SETS)]
        l1n, l2n, nl1n = l1.split(" — ")[0], l2.split(" — ")[0], nl1.split(" — ")[0]
        others = [x for x in LIVING_NONLIVING_POOL
                  if x not in (nl1n, l1n, l2n)][:3]
        items = [
            ("Which is a living thing?", [l1n] + others, l1n),
            ("Which is NOT a living thing?", [nl1n, l1n, l2n, others[0]], nl1n),
            ("What do all living things need to grow?",
             ["food and water", "a battery", "a plastic cover", "a quiet shelf"], "food and water"),
            (need_q, need_wrongs, need_ans),
            ("Which example shows a living thing?", [l2n, nl1n, others[1], others[2]], l2n),
        ]
        matching = [
            {"left": l1.split(" — ")[0], "right": "living thing"},
            {"left": nl1.split(" — ")[0], "right": "non-living thing"},
            {"left": need_sentence, "right": "a plant part"},
        ]
        vocab = LIVING_VOCAB
        sort_fits = [l1, l2, need_sentence]
        sort_other = [nl1, others[0], others[1], others[2]]
        c = {"l1": l1n, "l2": l2n, "nl1": nl1n}
        hotspot = [l1, l2, need_sentence, nl1]

    elif skill == "sci-materials":
        mats = MATERIAL_SETS[idx % len(MATERIAL_SETS)]
        flex = next((m for m in mats if "flexible" in m[1]), mats[0])
        absorb = next((m for m in mats if "absorb" in m[1]), mats[0])
        hard = next((m for m in mats if m[1].startswith("hard")), mats[0])
        others_flat = [m for s in MATERIAL_SETS for m in s if s != mats][:12]
        items = [
            ("Which material is flexible?", [flex[0]] + [o[0] for o in others_flat[:3]], flex[0]),
            ("Which material absorbs water?", [absorb[0]] + [o[0] for o in others_flat[3:6]], absorb[0]),
            ("Which material is hard?", [hard[0]] + [o[0] for o in others_flat[6:9]], hard[0]),
            (f"What property does a {mats[3][0]} have?",
             [mats[3][1]] + [mats[0][1], mats[1][1], mats[2][1]], mats[3][1]),
            ("Which is the SAFE way to learn about a material?",
             ["look at it and touch it gently", "taste it", "throw it", "hit it hard"],
             "look at it and touch it gently"),
        ]
        matching = [
            {"left": f"{mats[0][0]} — {mats[0][1]}", "right": "observable property"},
            {"left": f"{mats[1][0]} — {mats[1][1]}", "right": "property of the material"},
            {"left": f"{mats[2][0]} — {mats[2][1]}", "right": "what the material does"},
        ]
        vocab = MATERIAL_VOCAB
        sort_fits = [f"{m} — {p}" for m, p in mats]
        sort_other = ["a loud sound", "a happy feeling", "a fast race", "a tall mountain"]
        c = {"mats0": mats[0][0], "prop0": mats[0][1], "mats1": mats[1][0], "prop1": mats[1][1]}
        hotspot = [f"{m} — {p}" for m, p in mats]

    elif skill == "sci-light":
        ex_sound, ex_light, safe_sound, safe_light, q, ans, wrongs = LIGHT_SETS[idx % len(LIGHT_SETS)]
        other_sounds = [s[0] for s in LIGHT_SETS if s[0] != ex_sound][:3]
        other_lights = [s[1] for s in LIGHT_SETS if s[1] != ex_light][:3]
        items = [
            (q, wrongs, ans),
            ("Which is the SAFE way to use sound?",
             ["put your ear right next to a loud speaker",
              "scream close to your friend's ear", "play very loud music all day"], safe_sound),
            ("Which is the SAFE way to use light?",
             ["look directly at the Sun", "shine a laser at your eyes",
              "stare at a bright light"], safe_light),
            ("Which example shows how sound behaves?", other_sounds, ex_sound),
            ("Which example shows how light behaves?", other_lights, ex_light),
        ]
        matching = [
            {"left": ex_sound, "right": "makes sound"},
            {"left": ex_light, "right": "helps us see"},
            {"left": safe_sound, "right": "protects hearing"},
        ]
        vocab = LIGHT_VOCAB
        sort_fits = [ex_sound, ex_light, safe_sound, safe_light]
        sort_other = ["stare at the Sun", "put a loud speaker next to your ear",
                      "point a laser at your eyes", "play music at full volume all day"]
        c = {"ex_sound": ex_sound, "ex_light": ex_light}
        hotspot = [ex_sound, ex_light, safe_sound, safe_light]

    # --- apply shared repairs ---
    shells = SHELLS[skill]
    for a in lesson["activities"]:
        t = a["type"]
        content = a.get("content")
        shell = shells.get(t)
        if shell:
            instr = shell.format(lesson=lesson, c=c, skill=skill)
            a["instruction"] = instr
            a["prompt"] = instr
        if t == "ANIMATED_EXPLANATION" and isinstance(content, str):
            intro = SHELL_INTROS[skill].format(c=c)
            if intro:
                a["content"] = intro
                a["narration"] = intro
                a["accessibilityAlternative"] = intro
        if t == "HOTSPOT_IMAGE" and isinstance(content, dict):
            content["examples"] = list(hotspot)
        if t == "SORT_AND_CLASSIFY" and isinstance(content, dict):
            content["fits"] = list(sort_fits)
            content["doesNotFit"] = list(sort_other)
        elif t == "MULTIPLE_CHOICE" and isinstance(content, dict):
            correct = items[0][2]
            distractors = [o for o in items[0][1] if o != correct][:3]
            content["options"] = distractors[:correct_at] + [correct] + distractors[correct_at:]
            content["correctIndex"] = correct_at
        elif t == "MATCHING_PAIRS" and isinstance(content, dict):
            content["pairs"] = matching
        elif t == "SEQUENCE_BUILDER" and isinstance(content, dict):
            content["steps"] = [s.format(c=c) for s in SEQUENCES[skill]]

    lesson["vocabulary"] = vocab

    assessment = []
    for seq, (prompt, options, correct_text) in enumerate(items, start=1):
        rest = [o for o in options if o != correct_text]
        if len(rest) != 3:
            raise ValueError(
                f"{lesson['lessonId']} item {seq}: expected 3 distinct distractors, "
                f"got {len(rest)} (options={options!r}, correct={correct_text!r})"
            )
        display = rest[:correct_at] + [correct_text] + rest[correct_at:]
        assessment.append({
            "itemId": f"{lesson['lessonId']}-q0{seq}",
            "sequence": seq,
            "type": "MULTIPLE_CHOICE",
            "prompt": prompt,
            "options": [{"id": chr(97 + i), "text": t} for i, t in enumerate(display)],
            "correctOptionIds": [chr(97 + correct_at)],
            "explanation": f"The best answer is: {correct_text}",
        })
    lesson["assessment"]["items"] = assessment
    return lesson


def junk_in(lesson, junk_list):
    blob = json.dumps(lesson).lower()
    return [j for j in junk_list if j in blob]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)

    changed, junk_remaining, broken = [], [], []
    for path in sorted(LESSONS.glob("*.json")):
        lesson = json.loads(path.read_text())
        before = json.dumps(lesson)
        repaired = repair_lesson(lesson)
        if repaired is None:
            continue
        if json.dumps(repaired) != before:
            changed.append(path.stem)
        junk_list = MATH_JUNK if repaired["subject"] == "mathematics" else SCIENCE_JUNK
        for j in junk_in(repaired, junk_list):
            junk_remaining.append((path.stem, j))
        items = repaired["assessment"]["items"]
        if len(items) != 5 or any(
            len({o["id"] for o in it["options"]}) != 4
            or any(i not in {o["id"] for o in it["options"]} for i in it["correctOptionIds"])
            for it in items
        ):
            broken.append(path.stem)
        if not args.dry_run and not args.check and json.dumps(repaired) != before:
            path.write_text(json.dumps(repaired, ensure_ascii=False, indent=2) + "\n")

    print(f"lessons repaired: {len(changed)}")
    print(f"junk remaining:   {len(junk_remaining)}")
    for lid, j in junk_remaining[:10]:
        print(f"  {lid}: {j}")
    print(f"broken items:     {len(broken)}")
    if args.check and (junk_remaining or broken):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

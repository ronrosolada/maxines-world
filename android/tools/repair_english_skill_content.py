#!/usr/bin/env python3
"""Repair English quarterly lessons: kill stock junk, add real content.

Adversarial-review P0/P1 content fix (English subject). Replaces the
generator's stock distractors ("an unrelated guess", "a random symbol",
"a correct example", ...) and generic assessment prompts with real,
skill-faithful content authored per skill group, and differentiates the
byte-identical lesson instances that the quarterly conversion duplicated.

Skills covered (deterministic instance→content-set mapping, sorted by
lessonId within each objective group):
  1. "Use high-frequency and content-specific words in context."        (7)
  2. "Identify a base word, or root, in common related words."          (6)
  3. "Use details from a story to explain characters, events, and ideas."(5)
  4. "Tell whether a group of words expresses a complete idea."         (4)

Pattern (same as fix_filipino_simuno_panguri_assessment.py):
  convention → authored content tables → deterministic repair → audit/test.

Idempotent: re-running on an already-repaired lesson is a no-op for the
slots we own (we detect our own content via the authored tables' correct
strings).

Usage:
    python3 tools/repair_english_skill_content.py --dry-run
    python3 tools/repair_english_skill_content.py
    python3 tools/repair_english_skill_content.py --check
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"

# --------------------------------------------------------------------------
# Skill 1: high-frequency / content-specific words in context (word explorer)
# --------------------------------------------------------------------------
# Each set: (word, kid definition, example sentence, cloze sentence).
WORD_SETS = [
    [
        ("brave", "showing courage when something is hard or scary",
         "The brave dog saved the kitten from the rain.",
         "The ___ firefighter carried the cat down the tree."),
        ("huge", "very, very big",
         "The elephant is huge, but it walks quietly.",
         "The whale is ___ — bigger than a bus!"),
        ("whisper", "to speak very softly",
         "I whisper to my friend in the library.",
         "Please ___ so you do not wake the baby."),
        ("shiny", "bright and reflects light",
         "The new coin is shiny and clean.",
         "Her ___ shoes sparkled in the sun."),
    ],
    [
        ("gentle", "soft and kind, not rough",
         "Milo gives the puppy a gentle pat.",
         "Use a ___ touch with the flowers."),
        ("clever", "smart and quick to learn",
         "The clever fox found a way to reach the grapes.",
         "The ___ monkey opened the box with a stick."),
        ("cozy", "warm, comfortable, and snug",
         "The kitten sleeps in a cozy basket.",
         "Our little hut feels ___ when the rain falls."),
        ("nibble", "to take small bites",
         "The rabbit likes to nibble on carrots.",
         "Mice ___ on cheese in the story."),
    ],
    [
        ("curious", "wanting to know or learn",
         "The curious boy asked many questions.",
         "Milo is ___ about the box in the corner."),
        ("proud", "feeling happy about something you did",
         "Ana is proud of her neat handwriting.",
         "I feel ___ when I finish my chores."),
        ("silent", "very quiet, with no sound",
         "The library is silent during reading time.",
         "The night was ___ except for the crickets."),
        ("tiny", "very small",
         "The tiny ant carried a big leaf.",
         "A ___ seed can grow into a tall tree."),
    ],
    [
        ("delicious", "tastes very good",
         "Mama cooks delicious adobo for us.",
         "The mango shake is cold and ___."),
        ("eager", "excited and ready to do something",
         "The children are eager to open their new books.",
         "We are ___ to see the new kittens."),
        ("fresh", "new, clean, or just picked",
         "Fresh mangoes smell sweet.",
         "We picked ___ flowers for the table."),
        ("patient", "able to wait calmly",
         "The patient fisherman waited for a bite.",
         "Be ___ and wait for your turn."),
    ],
    [
        ("bright", "full of light or very smart",
         "The sun is bright this morning.",
         "The ___ light hurt my eyes."),
        ("careful", "giving attention so nothing gets hurt",
         "Be careful when you carry the glass.",
         "Milo is ___ with his new toy."),
        ("honest", "telling the truth",
         "An honest child returns the lost wallet.",
         "It is good to be ___ about your mistake."),
        ("strong", "having power or force",
         "The strong man lifted the heavy sack.",
         "The bridge is ___ enough for the truck."),
    ],
    [
        ("busy", "having many things to do",
         "The market is busy on Sunday.",
         "The ants are ___ carrying food."),
        ("cheerful", "happy and full of joy",
         "The cheerful girl sings on her way to school.",
         "Her ___ smile makes everyone happy."),
        ("deep", "going far down",
         "The well is very deep.",
         "Fish swim in the ___ part of the river."),
        ("soft", "not hard; gentle to touch",
         "The pillow is soft and fluffy.",
         "The cat's fur feels ___."),
    ],
    [
        ("silly", "funny and not serious",
         "The clown made a silly face.",
         "We laughed at the ___ dance."),
        ("warm", "a little hot, comfortable",
         "The blanket is warm on cold nights.",
         "The sun feels ___ on my skin."),
        ("quiet", "making little or no noise",
         "The mouse is quiet in the corner.",
         "Be ___ while the baby sleeps."),
        ("helpful", "giving aid; useful",
         "The helpful boy carried his Lola's bag.",
         "Milo is ___ when he shares his toys."),
    ],
]

# --------------------------------------------------------------------------
# Skill 2: root words
# --------------------------------------------------------------------------
ROOT_SETS = [
    ["play — played", "help — helpful", "teach — teacher", "kind — kindness"],
    ["jump — jumped", "read — reader", "walk — walking", "happy — happily"],
    ["clean — cleaned", "sing — singer", "slow — slowly", "care — careful"],
    ["look — looked", "paint — painter", "fast — faster", "hope — hopeful"],
    ["work — worked", "write — writer", "quick — quickly", "use — useful"],
    ["call — called", "build — builder", "loud — loudly", "soft — softly"],
]
ROOT_DEFS = {
    "play": "to have fun doing something", "help": "to give aid to someone",
    "teach": "to show someone how to do something", "kind": "nice and caring to others",
    "jump": "to push off the ground with your feet", "read": "to look at words and understand them",
    "walk": "to move on foot", "happy": "feeling glad and pleased",
    "clean": "to make something free of dirt", "sing": "to make music with your voice",
    "slow": "not fast", "care": "to watch over something or someone",
    "look": "to use your eyes to see", "paint": "to put color on something",
    "fast": "quick; moving quickly", "hope": "to wish for something good",
    "work": "to do a job or task", "write": "to form letters and words",
    "quick": "done in a short time", "use": "to put something to work",
    "call": "to shout or speak to someone", "build": "to make something by joining parts",
    "loud": "making a strong sound", "soft": "not hard; gentle to touch",
}
# Pairs that do NOT show a root word + a related word.
ROOT_NON_PAIRS = [
    "cat — dog", "big — small", "run — jump", "apple — orange",
    "book — read", "tree — leaf", "water — drink", "table — chair",
    "sun — moon", "fish — bird",
]
ROOT_OTHER_WORDS = ["cat", "desk", "water", "table", "sun", "tree", "fish", "moon"]

# --------------------------------------------------------------------------
# Skill 3: complete ideas (sentences vs fragments)
# --------------------------------------------------------------------------
SENTENCE_SETS = [
    (["The bird sings.", "We walk home.", "Milo drinks milk.", "Ana paints a picture."],
     ["under the table", "blue and shiny", "in the morning", "the big brown dog"]),
    (["The sun shines.", "Ben rides his bike.", "The fish swims.", "Lola bakes bread."],
     ["on the roof", "green and leafy", "after school", "my little sister"]),
    (["The dog barks.", "Nena waters the plants.", "We eat lunch.", "The bus stops here."],
     ["near the river", "loud and clear", "every Saturday", "the hungry cat"]),
    (["The stars twinkle.", "Tito John fixes the car.", "Maxine reads a book.", "The rain falls hard."],
     ["beside the window", "dark and cold", "before bedtime", "the sleepy boy"]),
]

# --------------------------------------------------------------------------
# Skill 4: story details
# --------------------------------------------------------------------------
# Each story: (text, character, setting, event, feeling, true detail,
#             4 wrong details).
STORY_SETS = [
    ("Milo found a lost kitten in the garden. The kitten was hiding under a yellow flower pot. Milo gave it milk, and the kitten purred happily.",
     "Milo and the kitten", "the garden",
     "Milo found a lost kitten and gave it milk", "happy",
     "The kitten was hiding under a yellow flower pot.",
     ["The kitten was hiding inside a blue box.", "Milo found a puppy in the kitchen.",
      "The kitten was sleeping on the roof.", "Milo gave the kitten a piece of bread."]),
    ("Ana lost her red pencil in the classroom. She looked under her desk and found it. She smiled because her drawing could continue.",
     "Ana", "the classroom",
     "Ana lost her red pencil and found it under her desk", "glad",
     "Ana found the pencil under her desk.",
     ["Ana found the pencil in her bag.", "Ana lost her blue crayon.",
      "The pencil was on the teacher's table.", "Ana stopped drawing."]),
    ("It rained all morning, so Maxine stayed inside. She built a tall tower with blocks. When the tower fell, she laughed and built it again.",
     "Maxine", "inside the house",
     "Maxine built a block tower and built it again after it fell", "happy",
     "Maxine built a tall tower with blocks.",
     ["Maxine played outside in the rain.", "She built a tower of pillows.",
      "The tower was made of paper.", "Maxine cried when the tower fell."]),
    ("Ben watched the ants march in a line to their home. Each ant carried a small piece of food. Ben said, 'They work together like one big family.'",
     "Ben and the ants", "the yard",
     "the ants marched in a line carrying food", "amazed",
     "Each ant carried a small piece of food.",
     ["The ants carried stones.", "Ben fed the ants bread.",
      "The ants were flying home.", "Ben stepped on the ants."]),
    ("Nina heard a soft knock on the door. A little bird had flown into the porch. Nina opened the window, and the bird flew out into the sky.",
     "Nina and the bird", "the porch",
     "a little bird flew into the porch and Nina set it free", "glad",
     "Nina opened the window so the bird could fly out.",
     ["Nina caught the bird in a cage.", "The bird knocked with its beak.",
      "Nina closed all the windows.", "The bird slept in the porch."]),
]

# --------------------------------------------------------------------------
# Sequences per skill
# --------------------------------------------------------------------------
SEQUENCES = {
    "word": ["Read the sentence", "Find the new word", "Look for clues", "Choose the meaning"],
    "root": ["Read the word pair", "Find the base word", "Look at the word ending", "Say the root word"],
    "complete": ["Read the group of words", "Look for who or what", "Look for the action", "Decide: complete or not"],
    "story": ["Read the story", "Find the character", "Find the setting", "Look for what happened"],
}

JUNK = [
    "an unrelated guess", "a random symbol", "a detail not in the lesson",
    "a different topic", "a correct example", "a related example",
    "another example", "a correct answer", "not in the lesson",
]


def find_skill(lesson):
    o = lesson["objective"]
    if o == "Use high-frequency and content-specific words in context.":
        return "word"
    if o == "Identify a base word, or root, in common related words.":
        return "root"
    if o == "Use details from a story to explain characters, events, and ideas.":
        return "story"
    if o == "Tell whether a group of words expresses a complete idea.":
        return "complete"
    return None


def instance_index(lesson_id, skill):
    """0-based index of this lesson within its skill group (sorted)."""
    ids = sorted(
        p.stem for p in LESSONS.glob("*.json")
        if find_skill(json.loads(p.read_text())) == skill
    )
    return ids.index(lesson_id)


def skill_content(lesson, skill, idx):
    """Build the authored content set for this lesson instance."""
    if skill == "word":
        words = WORD_SETS[idx % len(WORD_SETS)]
        return {
            "words": [w[0] for w in words], "defs": [w[1] for w in words],
            "sentences": [w[2] for w in words], "clozes": [w[3] for w in words],
        }
    if skill == "root":
        pairs = ROOT_SETS[idx % len(ROOT_SETS)]
        roots = [p.split(" — ")[0] for p in pairs]
        others = [p for p in ROOT_NON_PAIRS if p not in pairs][:4]
        return {"pairs": pairs, "roots": roots, "others": others,
                "defs": [ROOT_DEFS[r] for r in roots]}
    if skill == "complete":
        sents, frags = SENTENCE_SETS[idx % len(SENTENCE_SETS)]
        return {"sentences": sents, "fragments": frags}
    if skill == "story":
        text, char, setting, event, feeling, true_detail, wrongs = STORY_SETS[idx % len(STORY_SETS)]
        return {"text": text, "character": char, "setting": setting, "event": event,
                "feeling": feeling, "true": true_detail, "wrongs": wrongs}
    raise ValueError(skill)


def repair_lesson(lesson):
    """Mutate and return the lesson dict (or None when the skill is not ours)."""
    skill = find_skill(lesson)
    if skill is None:
        return None
    idx = instance_index(lesson["lessonId"], skill)
    day = int(lesson["lessonId"].rsplit("d", 1)[1])
    c = skill_content(lesson, skill, idx)
    correct_at = (day - 1) % 4  # vary correct position deterministically

    # --- activity content ---
    for a in lesson["activities"]:
        t = a["type"]
        content = a.get("content")
        if t == "ANIMATED_EXPLANATION":
            if skill == "story":
                a["content"] = c["text"]
                a["narration"] = c["text"]
                a["accessibilityAlternative"] = c["text"]
            elif skill == "word":
                a["content"] = ("Here are new words to learn! Read each sentence "
                                "and use the clues to understand the new word.")
                a["narration"] = a["content"]
                a["accessibilityAlternative"] = a["content"]
        elif t == "HOTSPOT_IMAGE" and isinstance(content, dict):
            if skill == "word":
                content["examples"] = list(c["sentences"])
            elif skill == "root":
                content["examples"] = list(c["pairs"])
            elif skill == "complete":
                content["examples"] = c["sentences"][:2] + c["fragments"][:2]
            elif skill == "story":
                content["examples"] = [
                    "the character: " + c["character"],
                    "the setting: " + c["setting"],
                    "the event: " + c["event"],
                    "the clue: " + c["true"],
                ]
        elif t == "SORT_AND_CLASSIFY" and isinstance(content, dict):
            if skill == "word":
                content["fits"] = list(c["sentences"])
                # Non-examples: sentences using other word sets' words.
                other = WORD_SETS[(idx + 1) % len(WORD_SETS)]
                content["doesNotFit"] = [w[2] for w in other]
            elif skill == "root":
                content["fits"] = list(c["pairs"])
                content["doesNotFit"] = list(c["others"])
            elif skill == "complete":
                content["fits"] = list(c["sentences"])
                content["doesNotFit"] = list(c["fragments"])
            elif skill == "story":
                content["fits"] = [
                    "the character: " + c["character"],
                    "the setting: " + c["setting"],
                    "the event: " + c["event"],
                    "the clue: " + c["true"],
                ]
                content["doesNotFit"] = list(c["wrongs"])
        elif t == "MULTIPLE_CHOICE" and isinstance(content, dict):
            if skill == "word":
                other = WORD_SETS[(idx + 1) % len(WORD_SETS)]
                correct = c["sentences"][0]
                dist = [other[i][2] for i in range(3)]
            elif skill == "root":
                correct = c["pairs"][0]; dist = c["others"][:3]
            elif skill == "complete":
                correct = c["sentences"][0]; dist = c["fragments"][:3]
            elif skill == "story":
                correct = "the character: " + c["character"]
                dist = c["wrongs"][:3]
            content["options"] = dist[:correct_at] + [correct] + dist[correct_at:]
            content["correctIndex"] = correct_at
        elif t == "MATCHING_PAIRS" and isinstance(content, dict):
            pairs = content.get("pairs", [])
            if skill == "word":
                rights = c["defs"][:3]
                new_pairs = [{"left": c["words"][i], "right": rights[i]} for i in range(3)]
            elif skill == "root":
                new_pairs = [{"left": c["pairs"][i], "right": "base word: " + c["roots"][i]} for i in range(3)]
            elif skill == "complete":
                new_pairs = [
                    {"left": c["sentences"][0], "right": "tells a complete idea"},
                    {"left": c["sentences"][1], "right": "has a subject and a verb"},
                    {"left": c["fragments"][0], "right": "is missing a verb"},
                ]
            elif skill == "story":
                new_pairs = [
                    {"left": c["character"], "right": "character"},
                    {"left": c["setting"], "right": "setting"},
                    {"left": c["event"], "right": "event"},
                ]
            content["pairs"] = new_pairs
        elif t == "SEQUENCE_BUILDER" and isinstance(content, dict):
            content["steps"] = list(SEQUENCES[skill])

    # --- vocabulary ---
    if skill == "word":
        lesson["vocabulary"] = [
            {"term": c["words"][i], "definition": c["defs"][i]} for i in range(3)
        ]
    elif skill == "root":
        lesson["vocabulary"] = [
            {"term": c["roots"][i], "definition": c["defs"][i]} for i in range(3)
        ]
    elif skill == "complete":
        lesson["vocabulary"] = [
            {"term": "sentence", "definition": "a group of words that tells a complete idea"},
            {"term": "subject", "definition": "who or what the sentence is about"},
            {"term": "verb", "definition": "what the subject does"},
        ]
    elif skill == "story":
        lesson["vocabulary"] = [
            {"term": "character", "definition": "the person or animal in the story"},
            {"term": "setting", "definition": "where and when the story happens"},
            {"term": "event", "definition": "something that happens in the story"},
        ]

    # --- assessment ---
    items = []
    if skill == "word":
        w, d, s = c["words"], c["defs"], c["sentences"]
        qs = [
            (f"What does '{w[0]}' mean?", [d[0], d[1], d[2], d[3]]),
            (f"Which word means '{d[1]}'?", [w[1], w[0], w[2], w[3]]),
            (f"Which sentence uses the word '{w[2]}'?", [s[2], s[0], s[1], s[3]]),
            (f"Which word fits in this sentence: '{c['clozes'][0]}'", [w[0], w[1], w[2], w[3]]),
            (f"Find the word that means '{d[3]}'.", [w[3], w[0], w[1], w[2]]),
        ]
    elif skill == "root":
        qs = [
            ("Which pair shows a root word and a related word?", [c["pairs"][0]] + c["others"][:3]),
            (f"Which is the base word in '{c['pairs'][0].split(' — ')[1]}'?",
             [c["roots"][0], c["roots"][1], c["roots"][2], ROOT_OTHER_WORDS[0]]),
            (f"Which word was made from the root '{c['roots'][0]}'?",
             [c["pairs"][0].split(' — ')[1], c["pairs"][1].split(' — ')[1],
              ROOT_OTHER_WORDS[1], ROOT_OTHER_WORDS[2]]),
            ("Which pair does NOT belong with the others?",
             [c["pairs"][1], c["pairs"][2], c["pairs"][3], c["others"][0]]),
            (f"What is the root word of '{c['pairs'][1].split(' — ')[1]}'?",
             [c["roots"][1], c["roots"][0], c["roots"][2], ROOT_OTHER_WORDS[3]]),
        ]
    elif skill == "complete":
        qs = [
            ("Which group of words is a complete sentence?",
             [c["sentences"][0]] + c["fragments"][:3]),
            ("Which group of words is NOT a complete sentence?",
             [c["fragments"][0]] + c["sentences"][:3]),
            (f"What is missing from '{c['fragments'][1]}'?",
             ["who or what is doing the action", "a longer word", "a new color", "a louder voice"]),
            ("Which group of words tells a complete idea?",
             [c["sentences"][1], c["fragments"][1], c["fragments"][2], c["fragments"][3]]),
            ("Which is a complete sentence?",
             [c["sentences"][2], c["fragments"][0], c["fragments"][1], c["fragments"][3]]),
        ]
    elif skill == "story":
        qs = [
            ("Who is in the story?", [c["character"], "the mailman", "the teacher", "the cook"]),
            ("Where does the story happen?",
             [c["setting"], "the market", "the beach", "the school"]),
            ("What happened in the story?", [c["event"]] + c["wrongs"][:3]),
            ("Which detail is TRUE from the story?", [c["true"]] + c["wrongs"][:3]),
            (f"How did {c['character'].split(' and ')[0]} feel at the end of the story?",
             [c["feeling"], "angry", "sleepy", "hungry"]),
        ]
    for seq, (prompt, options) in enumerate(qs, start=1):
        correct_text = options[0]
        rest = options[1:]
        display = rest[:correct_at] + [correct_text] + rest[correct_at:]
        items.append({
            "itemId": f"{lesson['lessonId']}-q0{seq}",
            "sequence": seq,
            "type": "MULTIPLE_CHOICE",
            "prompt": prompt,
            "options": [{"id": chr(97 + i), "text": t} for i, t in enumerate(display)],
            "correctOptionIds": [chr(97 + correct_at)],
            "explanation": f"The best answer is: {correct_text}",
        })
    lesson["assessment"]["items"] = items
    return lesson


def has_junk(lesson):
    blob = json.dumps(lesson).lower()
    return [j for j in JUNK if j in blob]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="report without writing")
    parser.add_argument("--check", action="store_true", help="verify pack is clean, exit 1 if not")
    args = parser.parse_args(argv)

    changed, junk_remaining, broken = [], [], []
    for path in sorted(LESSONS.glob("*.json")):
        lesson = json.loads(path.read_text())
        before = json.dumps(lesson)
        repaired = repair_lesson(lesson)
        if repaired is None:
            continue
        after = json.dumps(repaired)
        if after != before:
            changed.append(path.stem)
        if has_junk(repaired):
            junk_remaining.append((path.stem, has_junk(repaired)))
        if len(repaired["assessment"]["items"]) != 5:
            broken.append(path.stem)
        for it in repaired["assessment"]["items"]:
            ids = [o["id"] for o in it["options"]]
            if len(ids) != len(set(ids)) or any(i not in ids for i in it["correctOptionIds"]):
                broken.append(path.stem)
        if not args.dry_run and not args.check and after != before:
            path.write_text(json.dumps(repaired, ensure_ascii=False, indent=2) + "\n")

    print(f"skills repaired: {len(changed)} lessons")
    print(f"junk remaining:  {len(junk_remaining)}")
    for lid, hits in junk_remaining[:10]:
        print(f"  {lid}: {hits}")
    print(f"broken items:    {len(broken)}")
    for lid in broken[:10]:
        print(f"  {lid}")
    if args.check and (junk_remaining or broken):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

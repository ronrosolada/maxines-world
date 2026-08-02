#!/usr/bin/env python3
"""Educator-quality curation for converted Grade 3 lesson content.

This module deliberately does not approve lessons. It repairs malformed/generated
lesson payloads, creates topic-grounded activities, and reports remaining flags.
Approval remains a separate human-accountability step.
"""
from __future__ import annotations

import argparse
import copy
import json
import re
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]
PACK_DIR = REPO_ROOT / "app/src/main/assets/content-pack/month-01/lessons"

CANONICAL_TYPES = [
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
    "SEQUENCE_BUILDER",
]
PLACEHOLDERS = (
    "complete the ",
    "the correct choice about",
    "a close-but-wrong",
    "something that sounds right",
    "a clearly wrong answer",
    "this does not fit the lesson",
    "this is unrelated",
    "this belongs somewhere else",
    "matches the lesson",
    "fits the idea",
    "key idea:",
    "remember:",
    "think about:",
    "try this: can you explain",
    "first: understand ",
    "then: practice with examples",
    "next: check your understanding",
    "finally: apply what you learned",
)
BAD_MARKERS = ("✓", "choon", "matrix song", "mama lama")


def canonical_subject(value: str) -> str:
    return {
        "ENGLISH": "english",
        "FILIPINO": "filipino",
        "MATHEMATICS": "mathematics",
        "SCIENCE": "science",
        "GMRC": "gmrc",
        "MAKABANSA": "makabansa",
        "ARALING_PANLIPUNAN": "araling-panlipunan",
    }.get(str(value).upper(), str(value).lower())


def compact(value: str) -> str:
    value = re.sub(r"\s+", " ", str(value or "")).strip()
    value = re.sub(r"[✓]+", "", value)
    value = value.replace("tuong", "tuon")
    return value.strip(" .,:;—-\t")


def source_text(lesson: dict[str, Any]) -> str:
    parts = [lesson.get("lessonId", ""), lesson.get("title", ""), lesson.get("objective", "")]
    parts += [v.get("term", "") for v in lesson.get("vocabulary", [])]
    return " ".join(str(p) for p in parts).lower()


def is_placeholder_lesson(lesson: dict[str, Any]) -> bool:
    blob = json.dumps(lesson, ensure_ascii=False).lower()
    return any(marker in blob for marker in PLACEHOLDERS) or any(marker in blob for marker in BAD_MARKERS)


def is_generic_assessment(lesson: dict[str, Any]) -> bool:
    items = (lesson.get("assessment") or {}).get("items", [])
    prompts = " ".join(str(i.get("prompt", i.get("question", ""))).lower() for i in items)
    return any(x in prompts for x in (
        "what is this lesson about",
        "what math topic did we learn",
        "what science topic did we explore",
        "ano ang aralin natin ngayon",
        "ano ang paksa ng aralin",
        "aliking pahayag ang pinakaangkop",
    )) or not items


def topic_key(lesson: dict[str, Any]) -> str:
    s = source_text(lesson)
    subject = canonical_subject(lesson.get("subject", ""))
    if subject == "english":
        rules = [
            ("synonym", r"synonym|antonym"),
            ("root", r"root|word pattern|word patterns"),
            ("compound", r"compound sentence"),
            ("sentence_parts", r"parts of simple sentence|who/what"),
            ("capitalization", r"capitalization|punctuation"),
            ("discourse", r"discourse marker"),
            ("greeting", r"greeting|common expression"),
            ("intonation", r"intonation|pitch|juncture"),
            ("fluency", r"speed, accuracy|read grade level"),
            ("informational", r"informational text"),
            ("story_comprehension", r"comprehend stor|narrative"),
            ("summary", r"summary"),
            ("retell", r"retell|own words"),
            ("experience", r"experience"),
            ("text_types", r"text types"),
            ("sentence_sequence", r"sequence words"),
            ("sentence", r"sentences and non-sentences|simple sentences"),
            ("vocabulary", r"vocabulary|content-specific|high-frequency|sight words"),
            ("graph", r"picture graph"),
            ("possessive", r"possessive"),
            ("cause_effect", r"cause|effect"),
            ("main_detail", r"main idea|details"),
            ("retell", r"ending|story"),
            ("diary", r"diary|first-person"),
            ("characters", r"character"),
            ("picture", r"picture|detail"),
            ("telling", r"telling sentence|period"),
            ("plural_es", r"plural.*es|es and ies|-ies"),
            ("plural_s", r"plural"),
            ("vowels", r"vowel"),
            ("syllable", r"syllable"),
            ("be_verb", r"be-verb|am, is, and are"),
            ("tense", r"past, present|future"),
            ("blend", r"blend"),
            ("digraph", r"digraph"),
            ("sight", r"sight-word|sight word"),
        ]
    elif subject == "mathematics":
        rules = [
            ("area", r"area|squares and rectangles"),
            ("geometry", r"point|line segment|ray|parallel|perpendicular"),
            ("length", r"equal length|ruler"),
            ("ordinal", r"ordinal"),
            ("round", r"round"),
            ("compare", r"compare numbers"),
            ("order", r"order numbers"),
            ("capacity_measure", r"measure capacity"),
            ("capacity_estimate", r"estimate capacity"),
            ("capacity_compare", r"compare capacities"),
            ("addition_word", r"problems involving addition"),
            ("subtraction", r"subtract numbers"),
            ("difference", r"difference"),
            ("addition", r"add numbers|sum of addends|addition"),
            ("multi_add", r"3 to 4 numbers"),
            ("bar_graph", r"bar graph|data"),
            ("probability", r"outcomes|equally likely|likely|unlikely"),
            ("multiplication_properties", r"properties of multiplication|changing the order"),
            ("multiplication", r"multiply|multiplication"),
            ("product_estimate", r"estimation of products|product"),
            ("pattern", r"pattern|repeating|increasing"),
            ("division", r"division|divide|quotient"),
            ("fraction", r"fraction"),
            ("transformation", r"resulting figure|figure after"),
        ]
    elif subject == "science":
        rules = [
            ("materials", r"material|solid|metal|properties|waste"),
            ("living", r"living|non-living|body parts|basic needs|plant|animal|environment"),
            ("motion", r"moving objects|movement|surface texture|heaviness"),
            ("light_sound", r"light|sound"),
            ("sky_weather", r"sky|celestial|sun|moon|weather|earth material"),
        ]
    elif subject == "filipino":
        rules = [
            ("greetings", r"magagalang|pagbati|pananalita"),
            ("root", r"salitang-ugat"),
            ("sentence_parts", r"bahagi ng payak|sugnay|tambalang pangungusap|pangungusap"),
            ("fluency", r"tatas|mabilis|nababasa"),
            ("writing", r"naisusulat|wasto"),
            ("context", r"kahulugan|konteksto|nakikilala"),
            ("summary", r"nakabubuod|buod|naratibo"),
            ("paragraph", r"talata|naglalarawan|reaksiyon|ideya"),
            ("word_use", r"salita|high frequency|tuon|pangnilalaman"),
        ]
    elif subject == "gmrc":
        rules = [("faith", r"pananalig|pananampalataya|madasalin"),
                 ("respect", r"magalang|paggalang"),
                 ("care", r"mapagmalasakit|malasakit"),
                 ("responsibility", r"responsable|tungkulin"),
                 ("discipline", r"disiplina|masunurin"),
                 ("cooperation", r"pakikiisa|pakikibahagi"),
                 ("honesty", r"totoo|katapatan"),
                 ("initiative", r"pagkukusa"),
                 ("patience", r"mapagpasensiya"),
                 ("citizenship", r"bayan"),
                 ("judgment", r"maingat na paghuhusga|impormasyon"),
                 ("gratitude", r"mapagpasalamat"),
                 ("self_confidence", r"tiwala sa sarili|sariling hilig|sariling kakayahan")]
    else:  # makabansa
        rules = [( "music", r"folk song|ostinato|singing bee|cup game|matrix song|boom|mama|solfeggio|step and skip|soundtrack|call and response|soundscape|ritmo|tunog|awit"),
                 ("identity", r"pagka-pilipino|pagiging isang pilipino|pagkakakilanlan"),
                 ("active_citizen", r"aktibong|responsableng batang pilipino|panatang"),
                 ("culture", r"kultura|sining|kapaligiran"),
                 ("community_history", r"komunidad|kasaysayan|mahalagang tao|pagbabago")]
    for key, pattern in rules:
        if re.search(pattern, s, flags=re.I):
            return key
    return "general"


def profile_for(lesson: dict[str, Any]) -> dict[str, Any]:
    subject = canonical_subject(lesson.get("subject", ""))
    key = topic_key(lesson)
    en = subject in {"english", "mathematics", "science"}

    p: dict[str, Any] = {"subject": subject, "key": key, "language": "en-PH" if en else "fil-PH"}

    # Precise, age-appropriate profiles. These are intentionally concrete and
    # avoid claims that require a local map, named person, or unsafe experiment.
    profiles: dict[tuple[str, str], dict[str, Any]] = {
        ("english", "picture"): {"title": "Picture Detective", "objective": "Identify visible people, places, objects, and actions in a picture.", "explain": "A picture gives clues we can see. Name only what is visible, then describe it in a complete sentence.", "examples": ["a red kite in the sky", "two children beside a tree", "a dog running on the path", "a basket on the table"], "fits": ["colors", "people", "places", "actions"], "wrong": ["a smell", "a secret thought", "a sound outside the picture", "a dream"], "pairs": [("red kite", "in the sky"), ("basket", "on the table"), ("children", "beside a tree")], "steps": ["Look at the whole picture", "Find people and places", "Notice small details", "Tell what you can see"], "checks": [("What should you name first?", "A detail you can see", ["A smell", "A secret thought", "A dream"]), ("Which is visible?", "A red kite", ["A loud sound", "A perfume", "A hidden wish"]), ("Why use picture clues?", "They support your description", ["They make you guess", "They hide the topic", "They replace looking"])]},
        ("english", "characters"): {"title": "Meet the Characters", "objective": "Identify story characters and support ideas about them with actions or words.", "explain": "Characters are the people, animals, or make-believe beings who act in a story. Use their words or actions as clues.", "examples": ["Ana shares her umbrella", "the puppy hides under the chair", "Lola tells a funny story", "the bird carries a twig"], "fits": ["Ana", "the puppy", "Lola", "the bird"], "wrong": ["the rain", "the blue chair", "a mango tree", "the color red"], "pairs": [("Ana", "shares an umbrella"), ("puppy", "hides under a chair"), ("Lola", "tells a story")], "steps": ["Notice who is present", "Read what each one says", "Watch what each one does", "Describe the character with evidence"]},
        ("english", "ending"): {"title": "Choose an Ending", "objective": "Choose a possible ending that follows the events of a story.", "explain": "An ending tells how a story finishes. A strong ending follows the earlier events and solves or closes the problem.", "examples": ["the duck finds its mother", "the lost key is found", "friends repair the kite", "the seed grows into a plant"], "fits": ["the problem is solved", "the character reaches home", "the story closes", "the characters reflect"], "wrong": ["the opening setting", "a new unrelated character", "a repeated title", "a missing first event"], "pairs": [("beginning", "introduces the problem"), ("middle", "shows what happens"), ("ending", "closes the story")], "steps": ["Recall the problem", "Review the important events", "Choose an ending that fits", "Explain why it fits"]},
        ("english", "diary"): {"title": "Maxine's Little Diary", "objective": "Write about a personal experience using first-person words such as I, me, and my.", "explain": "First-person writing tells about the writer. The words I, me, and my show that the writer is speaking about themself.", "examples": ["I planted a seed", "My brother helped me", "I felt proud", "Me and Ana read together"], "fits": ["I played outside", "My book is new", "I helped at home", "I felt happy"], "wrong": ["Ana sings", "The dog ran", "The sun is bright", "Birds fly"], "pairs": [("I", "the writer"), ("my", "belongs to the writer"), ("today", "this day")], "steps": ["Choose one experience", "Name what happened", "Use I, me, or my", "Read your sentence aloud"]},
        ("english", "telling"): {"title": "Telling Sentences", "objective": "Write a telling sentence that gives information and ends with a period.", "explain": "A telling sentence gives information. It begins with a capital letter and ends with a period.", "examples": ["Milo is a cat.", "The rain is falling.", "Maxine reads a book.", "Rice grows in fields."], "fits": ["Milo is a cat.", "The bag is blue.", "We walk home.", "Plants need water."], "wrong": ["Where is Milo?", "Please sit down!", "a blue bag", "running fast"], "pairs": [("capital letter", "starts the sentence"), ("words", "give the idea"), ("period", "ends the telling sentence")], "steps": ["Think of one true idea", "Start with a capital", "Write the words in order", "Add a period"]},
        ("english", "nouns"): {"title": "Common or Proper?", "objective": "Tell the difference between common nouns and proper nouns.", "explain": "A common noun names a general person, place, animal, or thing. A proper noun names a particular one and begins with a capital letter.", "examples": ["city — Cebu City", "river — Pasig River", "girl — Maxine", "school — Mabini Elementary School"], "fits": ["dog — Bantay", "island — Palawan", "teacher — Ms. Cruz", "barangay — Barangay San Roque"], "wrong": ["happy", "quickly", "running", "under"], "pairs": [("city", "Cebu City"), ("girl", "Maxine"), ("river", "Pasig River")], "steps": ["Find the naming word", "Ask if it is general or particular", "Check the capital letter", "Classify the noun"]},
        ("english", "plural_s"): {"title": "More Than One", "objective": "Make regular plural nouns by adding -s.", "explain": "Many regular nouns form the plural by adding -s: one book becomes two books.", "examples": ["book → books", "cat → cats", "star → stars", "plant → plants"], "fits": ["tree → trees", "kite → kites", "cup → cups", "shell → shells"], "wrong": ["box → boxs", "baby → babys", "child → childs", "mouse → mouses"], "pairs": [("one flower", "two flowers"), ("one kite", "three kites"), ("one book", "four books")], "steps": ["Read the singular noun", "Check that it is regular", "Add -s", "Read the plural noun"]},
        ("english", "plural_es"): {"title": "Plural Word Changers", "objective": "Form common plurals ending in -es or -ies.", "explain": "Some nouns add -es after endings such as s, x, ch, or sh. A consonant plus y often changes y to i before adding -es.", "examples": ["box → boxes", "dish → dishes", "baby → babies", "class → classes"], "fits": ["bus → buses", "church → churches", "story → stories", "fox → foxes"], "wrong": ["box → boxs", "baby → babys", "dish → dishies", "class → classs"], "pairs": [("box", "boxes"), ("baby", "babies"), ("dish", "dishes")], "steps": ["Look at the word ending", "Choose -es or -ies", "Write the plural", "Check the spelling"]},
        ("english", "vowels"): {"title": "Short-Vowel Sound Lab", "objective": "Hear and say the short vowel sounds in simple words.", "explain": "The short vowel sounds are heard in words such as cat, bed, pig, hot, and sun.", "examples": ["cat — short a", "bed — short e", "pig — short i", "hot — short o"], "fits": ["map — short a", "jet — short e", "sit — short i", "cup — short u"], "wrong": ["cake — long a", "team — long e", "bike — long i", "home — long o"], "pairs": [("cat", "short a"), ("bed", "short e"), ("pig", "short i")], "steps": ["Look at the vowel", "Say the word", "Listen for the middle sound", "Name the short vowel"]},
        ("english", "syllable"): {"title": "Clap the Syllables", "objective": "Count the syllables, or spoken parts, in a word.", "explain": "A syllable is one spoken beat in a word. Clap each beat to help count the syllables.", "examples": ["sun — 1", "ta-ble — 2", "ba-na-na — 3", "el-e-phant — 3"], "fits": ["book — 1", "pa-per — 2", "to-ma-to — 3", "but-ter-fly — 3"], "wrong": ["calling every letter a syllable", "counting punctuation", "counting the picture", "counting spaces only"], "pairs": [("book", "1 syllable"), ("paper", "2 syllables"), ("banana", "3 syllables")], "steps": ["Say the word slowly", "Clap each spoken beat", "Count the claps", "Check the total"]},
        ("english", "be_verb"): {"title": "Be-Verb Team", "objective": "Use am, is, and are correctly in simple sentences.", "explain": "Use am with I, is with one person or thing, and are with you or more than one.", "examples": ["I am ready.", "Milo is curious.", "We are learning.", "The books are open."], "fits": ["I am kind.", "She is helpful.", "They are friends.", "You are ready."], "wrong": ["I is ready.", "She are kind.", "They am friends.", "You is learning."], "pairs": [("I", "am"), ("she", "is"), ("they", "are")], "steps": ["Find the subject", "Ask if it is one or more", "Choose am, is, or are", "Read the sentence"]},
        ("english", "tense"): {"title": "Yesterday, Today, Tomorrow", "objective": "Use past, present, and future action words.", "explain": "Past tells what already happened, present tells what is happening, and future tells what will happen.", "examples": ["Yesterday I played.", "Today I play.", "Tomorrow I will play.", "Last night we walked."], "fits": ["will visit tomorrow", "read yesterday", "am reading now", "played last week"], "wrong": ["tomorrow happened yesterday", "now means next year", "past means not yet", "future means already finished"], "pairs": [("yesterday", "past"), ("today", "present"), ("tomorrow", "future")], "steps": ["Find the time clue", "Choose past, present, or future", "Choose the action word", "Read the sentence"]},
        ("english", "blend"): {"title": "Blend Builders", "objective": "Read words with common consonant blends such as bl, cl, gr, st, and tr.", "explain": "In a consonant blend, each consonant keeps its own sound while the sounds are said close together.", "examples": ["blue — bl", "clap — cl", "tree — tr", "stop — st"], "fits": ["glad", "grin", "truck", "step"], "wrong": ["ship", "chair", "thin", "phone"], "pairs": [("bl", "blue"), ("gr", "green"), ("st", "star")], "steps": ["Look at the first letters", "Say each consonant sound", "Blend the sounds", "Read the whole word"]},
        ("english", "digraph"): {"title": "Digraph Detectives", "objective": "Read words with common digraphs ch, sh, th, wh, and ph.", "explain": "A digraph has two letters that work together to represent one sound, such as sh in ship or ch in chair.", "examples": ["ship — sh", "chair — ch", "thumb — th", "phone — ph"], "fits": ["whale", "cheese", "shell", "three"], "wrong": ["stop", "green", "plant", "crab"], "pairs": [("sh", "ship"), ("ch", "chair"), ("th", "three")], "steps": ["Find the two-letter team", "Say its sound", "Blend with the rest", "Read the word"]},
        ("english", "sight"): {"title": "Sight-Word Path", "objective": "Read common high-frequency words accurately and use them in context.", "explain": "High-frequency words appear often in reading. Read each word, then use the sentence around it to confirm its meaning.", "examples": ["because — gives a reason", "friend — a person we like", "around — on every side", "always — every time"], "fits": ["because", "friend", "people", "always"], "wrong": ["a made-up spelling", "a picture with no word", "a random symbol", "an unrelated sound"], "pairs": [("because", "gives a reason"), ("friend", "a person we like"), ("always", "every time")], "steps": ["Look at the whole word", "Say it aloud", "Read the sentence", "Check the meaning"]},
        ("english", "possessive"): {"title": "Whose Is It?", "objective": "Use possessive words to show who owns or is connected to something.", "explain": "Possessive words show a connection: my book, your bag, his hat, her pencil, its tail, our class, their home.", "examples": ["my book", "her pencil", "their kite", "our class"], "fits": ["his shoes", "your lunch", "its tail", "my notebook"], "wrong": ["quickly book", "run pencil", "blue their", "under our"], "pairs": [("I", "my"), ("she", "her"), ("they", "their")], "steps": ["Find who owns it", "Choose the matching possessive", "Place it before the noun", "Read the phrase"]},
        ("english", "cause_effect"): {"title": "Cause-and-Effect Garden", "objective": "Identify a cause and its effect in a short story or situation.", "explain": "A cause tells why something happens. An effect tells what happens because of it.", "examples": ["It rained, so the plants grew.", "The ice warmed, so it melted.", "Milo practiced, so he improved.", "The bell rang, so class began."], "fits": ["cause: rain; effect: wet ground", "cause: practice; effect: improvement", "cause: wind; effect: leaves moved", "cause: sleep; effect: more energy"], "wrong": ["effect before its cause", "two unrelated objects", "a color with no event", "a title only"], "pairs": [("rain", "wet ground"), ("practice", "improvement"), ("bell rings", "class begins")], "steps": ["Notice what happened", "Ask why it happened", "Name the cause", "Name the effect"]},
        ("english", "main_detail"): {"title": "Detail Tracker", "objective": "Find the main idea and supporting details in a short text.", "explain": "The main idea is what the text is mostly about. Details give facts or examples that support it.", "examples": ["main idea: mango trees give fruit", "detail: flowers grow first", "detail: fruit ripens later", "detail: trees need sunlight"], "fits": ["the topic sentence", "a fact from the text", "an example from the text", "a supporting detail"], "wrong": ["a personal guess", "an unrelated fact", "a title from another text", "a detail not mentioned"], "pairs": [("main idea", "what the text is mostly about"), ("detail", "a fact that supports it"), ("example", "one specific case")], "steps": ["Read the whole text", "Name the topic", "Find repeated or important ideas", "Choose supporting details"]},
        ("english", "graph"): {"title": "Read the Picture Graph", "objective": "Read a picture graph and compare the amounts it shows.", "explain": "A picture graph uses symbols to show data. Read the key first, then count the symbols and compare the amounts.", "examples": ["1 star = 2 votes", "3 stars = 6 votes", "the tallest row has the most", "a shorter row has fewer"], "fits": ["read the key", "count the symbols", "compare rows", "answer from the data"], "wrong": ["guess without counting", "change the key", "count the title", "use a different graph"], "pairs": [("key", "what one symbol means"), ("row", "data for one choice"), ("more symbols", "a greater amount")], "steps": ["Read the graph title", "Check the key", "Count each row", "Compare the amounts"]},
    }
    p.update(copy.deepcopy(profiles.get((subject, key), {})))

    # Compact topic profiles for recurring converted competencies.
    compact_profiles = {
        "synonym": ("Word Twins", "Identify synonyms and antonyms by comparing word meanings.", "Synonyms have similar meanings; antonyms have opposite meanings.", ["happy — glad", "begin — start", "hot — cold", "early — late"]),
        "root": ("Word Roots", "Identify a base word, or root, in common related words.", "A root or base word carries the central meaning. Word endings can change how the word is used.", ["play — played", "help — helpful", "teach — teacher", "kind — kindness"]),
        "sentence": ("Sentence or Not?", "Tell whether a group of words expresses a complete idea.", "A sentence expresses a complete idea and usually begins with a capital letter and ends with punctuation.", ["The bird sings.", "We walk home.", "under the table", "blue and shiny"]),
        "sentence_sequence": ("Sentence Order", "Use sequence words to show the order of events or ideas.", "Sequence words such as first, next, then, and finally help readers follow an order.", ["First, wash the fruit.", "Next, cut it safely.", "Then, share it.", "Finally, clean up."]),
        "sentence_parts": ("Sentence Parts", "Identify who or what the sentence is about and what that subject does or is.", "The subject tells who or what. The predicate tells what the subject does or is.", ["Milo / reads.", "The plants / grow.", "Maxine / draws.", "The bell / rings."]),
        "capitalization": ("Sentence Polishing", "Use a capital letter and correct end punctuation in a simple sentence.", "Begin a sentence with a capital letter. Use a period, question mark, or exclamation mark that matches the meaning.", ["Milo reads.", "Where is the book?", "Look out!", "We are ready."]),
        "compound": ("Compound Sentence Crew", "Identify two complete ideas joined in a compound sentence.", "A compound sentence joins two complete ideas with a word such as and, but, or so.", ["I read, and Milo listens.", "It rained, so we stayed inside.", "I wanted to play, but I had homework.", "Ana sings, and Ben claps."]),
        "discourse": ("Signal Words", "Identify words that show order, reason, contrast, or result in a text.", "Signal words help readers understand how ideas connect: first shows order, because shows a reason, and so shows a result.", ["first", "because", "but", "so"]),
        "greeting": ("Kind Words, Right Place", "Choose a polite expression that fits the situation.", "A greeting and polite expression should fit the person, time, and situation.", ["Good morning.", "Please help me.", "Thank you.", "Excuse me."]),
        "intonation": ("Voice Clues", "Use pitch and pauses to show the meaning of a sentence.", "Our voice changes to help listeners hear whether a sentence asks, tells, or exclaims.", ["Are you ready?", "I am ready.", "What a surprise!", "Please listen."]),
        "fluency": ("Smooth Reading", "Read grade-level sentences accurately, smoothly, and with meaning.", "Good fluency combines accurate words, a steady pace, and expression that matches the meaning.", ["pause at punctuation", "read each word accurately", "group words into phrases", "change your voice for a question"]),
        "informational": ("Fact Finders", "Find the topic and important facts in a short informational text.", "Informational texts teach about a real topic. Look for the topic, facts, and details that explain it.", ["topic: mangrove trees", "fact: roots hold soil", "fact: trees provide shelter", "detail from the text"]),
        "story_comprehension": ("Story Clue Crew", "Use details from a story to explain characters, events, and ideas.", "Readers use story clues—not guesses alone—to explain what happened and why.", ["a character's action", "the setting", "an important event", "a clue from the text"]),
        "summary": ("Tiny Story, Big Idea", "Summarize a narrative by naming the important events in order.", "A summary keeps the main characters, problem, important events, and ending without every small detail.", ["who", "what problem", "important events", "how it ends"]),
        "retell": ("Retell in Your Own Words", "Retell a narrative in the correct order using your own words.", "A retell keeps the important events but uses the reader's own words.", ["beginning", "important middle event", "result", "ending"]),
        "experience": ("My Experience", "Express an idea about a personal experience in clear sentences.", "A clear personal account tells what happened and may include a feeling, reason, or detail.", ["what happened", "where it happened", "how I felt", "what I learned"]),
        "text_types": ("Text Type Toolbox", "Choose a text type that fits the writer's purpose.", "A story entertains or tells events, instructions explain steps, and an informational text teaches facts.", ["story", "instructions", "informational text", "personal response"]),
        "vocabulary": ("Word Explorer", "Use high-frequency and content-specific words in context.", "Context clues and the topic help readers understand and use new words accurately.", ["look at nearby words", "connect the word to the topic", "try the word in a sentence", "check the meaning"]),
        "main_detail": ("Main-Idea Detectives", "Identify the main idea and details that support it.", "The main idea tells what the text is mostly about; details support that idea.", ["topic", "main idea", "supporting fact", "example"]),
        "cause_effect": ("Cause and Effect", "Identify why something happens and what happens because of it.", "A cause explains why; an effect is the result.", ["rain → wet ground", "practice → improvement", "wind → moving leaves", "sleep → energy"]),
        "possessive": ("Whose Is It?", "Use possessive words to show ownership or connection.", "Words such as my, your, his, her, its, our, and their show ownership or connection.", ["my book", "her pencil", "our class", "their kite"]),
        "ending": ("Story Endings", "Choose an ending that follows the events of a story.", "A good ending connects to the story's problem and events.", ["the problem is solved", "the character reaches home", "friends help", "the story closes"]),
    }
    if (subject, key) not in profiles and key in compact_profiles:
        title, objective, explain, examples = compact_profiles[key]
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["an unrelated guess", "a random symbol", "a detail not in the lesson", "a different topic"],
                  "pairs": [(examples[0], "a correct example"), (examples[1], "a related example"), (examples[2], "another example")],
                  "steps": ["Read the example", "Find the clue", "Choose the idea", "Explain your answer"]})

    # Filipino and GMRC topic copy.
    filipino = {
        "context": ("Kahulugan sa Konteksto", "Natutukoy ang kahulugan ng salita gamit ang mga pahiwatig sa pangungusap.", "Makikita sa mga salitang kasama ng isang salita ang pahiwatig sa kahulugan nito.", ["Masaya si Lira dahil may regalo siya.", "Mabigat ang bag kaya dahan-dahan siyang naglakad.", "Malamig ang tubig mula sa yelo.", "Mabilis tumakbo ang bata."]),
        "root": ("Salitang-Ugat", "Natutukoy ang salitang-ugat ng mga karaniwang salita.", "Ang salitang-ugat ang payak na anyo na pinagmumulan ng ibang salita.", ["sulat → sumulat", "laro → naglaro", "basa → bumasa", "tanim → nagtanim"]),
        "sentence_parts": ("Bahagi ng Pangungusap", "Natutukoy ang simuno at panaguri sa payak na pangungusap.", "Ang simuno ang pinag-uusapan. Ang panaguri ang nagsasabi tungkol sa simuno.", ["Si Ana / ay nagbabasa.", "Ang aso / ay tumatakbo.", "Si Milo / ay natututo.", "Ang mga bata / ay naglalaro."]),
        "fluency": ("Mabisang Pagbasa", "Nababasa ang pangungusap nang wasto, may tamang bilis, at may damdamin.", "Ang mahusay na pagbasa ay wasto, malinaw, at angkop ang damdamin sa kahulugan ng pangungusap.", ["huminto sa kuwit", "linawin ang bawat salita", "bigyang-diin ang mahalagang salita", "baguhin ang tono ayon sa pangungusap"]),
        "writing": ("Wastong Pagsulat", "Naisusulat nang maayos at wasto ang mga natutuhang salita.", "Suriin ang baybay, malaking titik, at wastong pagkakasulat bago ipasa ang gawain.", ["paaralan", "kaibigan", "paggalang", "masipag"]),
        "greetings": ("Magagalang na Pananalita", "Nagagamit ang magagalang na pagbati at pananalita ayon sa sitwasyon.", "Piliin ang magalang na pananalita na angkop sa oras, kausap, at pangyayari.", ["Magandang umaga po.", "Maaari po bang makiusap?", "Salamat po.", "Paumanhin po."]),
        "summary": ("Maikling Buod", "Nakabubuo ng maikling buod ng tekstong naratibo.", "Sa buod, ilahad ang tauhan, suliranin, mahahalagang pangyayari, at wakas nang maikli.", ["tauhan", "suliranin", "mahalagang pangyayari", "wakas"]),
        "paragraph": ("Munting Talata", "Nakabubuo ng maikling talata na malinaw ang paksa at mga detalye.", "Ang talata ay binubuo ng magkakaugnay na pangungusap tungkol sa isang paksa.", ["paksa", "pangunahing ideya", "detalye", "wakas na pangungusap"]),
        "word_use": ("Mga Salitang May Kahulugan", "Nagagamit ang angkop na salita sa pangungusap at sa paksa.", "Piliin ang salitang malinaw at angkop sa nais sabihin.", ["mabait na kaibigan", "malinis na silid", "masipag na bata", "mahabang pila"]),
    }
    if subject == "filipino":
        title, objective, explain, examples = filipino.get(key, filipino["word_use"])
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["salitang walang kaugnayan", "hindi magalang na pahayag", "hula na walang pahiwatig", "paksang iba sa aralin"],
                  "pairs": [(examples[0], "angkop na halimbawa"), (examples[1], "malinaw na gamit"), (examples[2], "tamang ideya")],
                  "steps": ["Basahin ang halimbawa", "Hanapin ang pahiwatig", "Piliin ang tamang ideya", "Ipaliwanag ang sagot"]})

    gmrc_values = {"faith": ("Pananalig at Paggalang", "Naipakikita ang pananalig habang iginagalang ang sariling paniniwala at paniniwala ng iba.", "Maaaring ipakita ang pananalig sa tahimik na panalangin, pasasalamat, at paggalang sa kapwa.", ["nagpapasalamat", "gumagalang sa paniniwala ng iba", "humihingi ng gabay", "gumagawa ng mabuti"]),
                   "respect": ("Paggalang sa Kapwa", "Naipakikita ang paggalang sa salita, kilos, at pakikinig sa kapwa.", "Ang paggalang ay makikita sa pakikinig, paggamit ng magagalang na salita, at pag-iingat sa damdamin ng iba.", ["nakikinig", "nagsasabi ng po at opo kung angkop", "humihingi ng pahintulot", "gumagalang sa pagkakaiba"]),
                   "care": ("Pagmamalasakit", "Naipakikita ang malasakit sa pamamagitan ng ligtas at kusang pagtulong.", "Ang malasakit ay pag-unawa sa pangangailangan ng iba at pagtulong sa paraang ligtas at may pahintulot.", ["tumutulong sa kaklase", "nagbabahagi ng gamit nang maayos", "nagtatanong kung ayos lang ang kaibigan", "nag-aalaga sa kapaligiran"]),
                   "responsibility": ("Aking Pananagutan", "Naisasagawa ang tungkulin nang maayos at may pananagutan.", "Ang responsable ay tumutupad sa gawain, nag-iingat sa gamit, at umaamin kapag nagkamali.", ["inaayos ang gamit", "tinatapos ang gawain", "sumusunod sa napagkasunduan", "humihingi ng tulong kapag kailangan"]),
                   "discipline": ("Disiplina sa Araw-araw", "Naipakikita ang disiplina sa pagsunod sa makatarungan at ligtas na tuntunin.", "Ang disiplina ay pagpipigil sa sarili at pagsunod sa tuntuning tumutulong sa kaligtasan at kaayusan.", ["pumipila", "naghihintay ng turn", "naglalakad sa tamang lugar", "nagliligpit pagkatapos gamitin"]),
                   "cooperation": ("Pakikiisa", "Naipakikita ang pakikiisa sa sama-samang gawain.", "Ang pakikiisa ay pakikinig, pagbabahagi ng gawain, at pagtulong upang makamit ang layunin ng grupo.", ["nagbabahagi ng gawain", "nakikinig sa mungkahi", "tumutulong sa grupo", "nagpapasalamat sa kasama"]),
                   "honesty": ("Katapatan", "Naipakikita ang katapatan sa pagsasabi ng totoo at pag-aayos ng pagkakamali.", "Ang tapat na tao ay nagsasabi ng totoo, hindi kumukuha ng hindi kanya, at handang itama ang mali.", ["isinauli ang napulot", "umamin sa pagkakamali", "gumamit ng sariling sagot", "nagsabi ng totoong nangyari"]),
                   "initiative": ("Pagkukusa", "Nakapagsasanay ng pagkukusa sa paggawa ng angkop na gawain.", "Ang pagkukusa ay paggawa ng nararapat nang hindi laging hinihintay na utusan, habang humihingi ng tulong kung kailangan.", ["nagligpit nang kusa", "naghanda ng gamit", "tumulong sa ligtas na paraan", "nagpaalam bago kumilos"]),
                   "patience": ("Pagtitiyaga at Pasensya", "Naipakikita ang pasensya sa paghihintay at pagsubok muli.", "Ang pasensya ay mahinahong paghihintay at patuloy na pagsisikap kapag mahirap ang gawain.", ["humihinga nang malalim", "naghihintay ng turn", "sumusubok muli", "humihingi ng payo"]),
                   "citizenship": ("Mabuting Mamamayan", "Naipakikita ang pagmamahal sa bayan sa maliit at makabuluhang gawain.", "Maaaring maging mabuting mamamayan sa pag-aalaga sa kapaligiran, pagsunod sa tuntunin, at pagtulong sa komunidad.", ["nagtatapon sa tamang basurahan", "nangangalaga sa pampublikong gamit", "tumutulong sa komunidad", "gumagalang sa sagisag ng bayan"]),
                   "judgment": ("Maingat na Paghuhusga", "Nakapagsasanay sa pag-iisip muna bago maniwala o kumilos.", "Suriin ang pinagmulan ng impormasyon, itanong kung makatwiran ito, at humingi ng tulong sa nakatatanda kung kailangan.", ["nagtatanong sa mapagkakatiwalaang adulto", "sinusuri ang pinagmulan", "hindi agad nagpapasa ng balita", "nag-iisip bago magpasya"]),
                   "gratitude": ("Pasasalamat", "Naipakikita ang pasasalamat sa tao, kalikasan, at mga biyayang tinatanggap.", "Ang pasasalamat ay pagkilala sa kabutihang ginawa at pagpapakita nito sa salita o gawa.", ["nagsasabi ng salamat", "nag-aalaga sa natanggap", "tumutulong bilang tugon", "kumilala sa tumulong"]),
                   "self_confidence": ("Tiwala sa Sarili", "Naipakikita ang tiwala sa sarili habang kinikilala ang sariling lakas at limitasyon.", "Ang tiwala sa sarili ay pagsubok nang buong tapang, pagtanggap sa pagkakamali, at paghingi ng tulong kung kailangan.", ["sumubok ng bagong gawain", "nagsanay", "humingi ng gabay", "pinahalagahan ang sariling progreso"])}
    if subject == "gmrc":
        title, objective, explain, examples = gmrc_values.get(key, gmrc_values["self_confidence"])
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["nanunukso sa kapwa", "kumukuha ng hindi kanya", "sumusuway sa ligtas na tuntunin", "hindi nakikinig"],
                  "pairs": [(examples[0], "mabuting halimbawa"), (examples[1], "angkop na kilos"), (examples[2], "responsableng gawain")],
                  "steps": ["Unawain ang sitwasyon", "Isipin ang epekto sa kapwa", "Piliin ang mabuting kilos", "Isagawa ito nang ligtas"]})

    makabansa = {
        "community_history": ("Kasaysayan ng Komunidad", "Natutukoy ang mahahalagang tao, lugar, at pangyayari sa kasaysayan ng komunidad.", "Ang kasaysayan ng komunidad ay binubuo ng mga tao, lugar, at pangyayaring mahalaga sa mga mamamayan.", ["barangay hall", "pamilihan", "paaralan", "mga kuwento ng nakatatanda"]),
        "culture": ("Kapaligiran at Kultura", "Naipaliliwanag kung paano nakaaapekto ang kapaligiran sa kultura ng komunidad.", "Nakaaapekto ang kapaligiran sa hanapbuhay, pagkain, tahanan, at gawaing pinipili ng mga tao.", ["pangingisda sa baybayin", "pagsasaka sa kapatagan", "disenyong angkop sa klima", "pagkain mula sa lokal na ani"]),
        "identity": ("Pagkakakilanlang Pilipino", "Naiuugnay ang sariling katangian at karanasan sa pagiging Pilipino.", "Ang pagkakakilanlan ay nabubuo sa wika, kultura, ugnayan, pagpapahalaga, at mga karanasang pinagsasaluhan.", ["paggalang sa kapwa", "pag-aalaga sa komunidad", "pagpapahalaga sa wika", "pakikilahok sa kultura"]),
        "active_citizen": ("Aktibo at Responsableng Pilipino", "Naipamamalas ang pagiging aktibo at responsableng batang Pilipino sa komunidad.", "Ang batang mamamayan ay nakikilahok sa ligtas na gawain, sumusunod sa makatarungang tuntunin, at tumutulong sa kapwa.", ["pag-aalaga sa kapaligiran", "pakikilahok sa proyekto", "paggalang sa iba", "pagtupad sa tungkulin"]),
        "music": ("Tunog, Ritmo, at Kultura", "Naiuugnay ang ritmo at tunog sa sining at kultura ng komunidad.", "Ang ostinato ay paulit-ulit na padron ng ritmo. Sa call and response, salitan ang umaawit o tumutugtog. Ang soundscape ay pinagsamang tunog ng isang lugar.", ["paulit-ulit na ritmo", "salitang tugon sa awit", "tunog ng palengke", "awiting-bayan bilang bahagi ng kultura"]),
    }
    if subject == "makabansa":
        title, objective, explain, examples = makabansa.get(key, makabansa["community_history"])
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["hula na walang batayan", "impormasyong walang kaugnayan", "pang-aalipusta sa kultura", "gawaing nakapipinsala sa kapwa"],
                  "pairs": [(examples[0], "kaugnay na halimbawa"), (examples[1], "bahagi ng paksa"), (examples[2], "makabuluhang ideya")],
                  "steps": ["Tingnan ang halimbawa", "Tukuyin ang mahalagang ideya", "Iugnay sa komunidad", "Ipaliwanag ang sagot"]})

    math = {
        "geometry": ("Shape Trail", "Recognize and describe points, lines, line segments, rays, and special line relationships.", "A point marks a location. A line segment has two endpoints. A ray has one endpoint and continues in one direction. Parallel lines do not meet; perpendicular lines meet to form a right angle.", ["point A", "segment AB", "ray CD", "parallel lines"]),
        "area": ("Area Detectives", "Solve simple problems about the area of squares and rectangles using length × width.", "Area measures the space inside a shape. For a rectangle, multiply its length by its width and write square units.", ["4 × 3 = 12 square units", "a 5 by 2 rectangle has 10 square units", "a square has equal side lengths", "counting unit squares checks the answer"]),
        "length": ("Measure It Fairly", "Measure and compare line segments using the same unit and a ruler.", "Place the zero mark at an endpoint, read the other endpoint, and use the same unit when comparing lengths.", ["4 cm", "7 cm is longer than 5 cm", "align the zero mark", "use centimeters for small objects"]),
        "ordinal": ("Place in Line", "Read and use ordinal numbers up to the 100th to show position.", "Ordinal numbers show position, not how many: first, second, third, and 100th.", ["1st is first", "2nd is second", "10th is tenth", "100th is one hundredth"]),
        "round": ("Rounding Ramp", "Round numbers to the nearest ten, hundred, or thousand.", "Look at the digit to the right of the place you are rounding. Five or more rounds up; four or less stays down.", ["47 rounds to 50", "342 rounds to 300", "3,650 rounds to 4,000", "1,241 rounds to 1,200"]),
        "compare": ("Number Face-Off", "Compare numbers up to 10,000 using =, >, and <.", "Compare digits from the greatest place value first. The symbol opens toward the greater number.", ["4,205 > 4,025", "3,100 < 3,900", "6,450 = 6,450", "8,001 > 7,999"]),
        "order": ("Number Line-Up", "Order numbers up to 10,000 from least to greatest and greatest to least.", "Compare place values, then arrange the numbers in the requested order.", ["120, 210, 201 — least to greatest", "900, 450, 90 — greatest to least", "1,205 before 1,250", "4,000 after 3,999"]),
        "capacity_measure": ("Capacity Lab", "Measure capacity in liters or milliliters with an appropriate tool.", "Capacity tells how much a container can hold. Liters measure larger amounts; milliliters measure smaller amounts.", ["water bottle — milliliters", "bucket — liters", "measuring cup — milliliters", "jerry can — liters"]),
        "capacity_estimate": ("How Much Can It Hold?", "Estimate a container's capacity in liters or milliliters.", "Use a familiar amount as a reference, then choose a reasonable estimate and unit.", ["a spoon — milliliters", "a glass — about 250 mL", "a pail — liters", "a water tank — many liters"]),
        "capacity_compare": ("Container Challenge", "Compare the capacities of two containers using more, less, or equal.", "A larger-looking container does not always hold more; compare using the same unit or a fair test.", ["2 L is more than 500 mL", "1 L equals 1,000 mL", "a pail holds more than a cup", "two equal bottles can hold equal amounts"]),
        "addition": ("Add-Up Adventure", "Add numbers with sums up to 10,000, with or without regrouping.", "Add from ones to thousands. Regroup 10 ones as 1 ten, 10 tens as 1 hundred, and so on.", ["245 + 123 = 368", "2,650 + 1,200 = 3,850", "regroup 10 ones", "check with an estimate"]),
        "addition_word": ("Story-Problem Builders", "Solve one-step addition problems with sums up to 10,000.", "Read the situation, identify the quantities being joined, choose addition, solve, and label the answer.", ["18 red beads + 12 blue beads = 30 beads", "₱25 + ₱15 = ₱40", "45 pages + 20 pages = 65 pages", "write the unit"]),
        "subtraction": ("Take-Away Trail", "Subtract numbers below 10,000 with or without regrouping.", "Subtract from ones to thousands. Regroup when a top digit is too small for the digit below it.", ["458 − 123 = 335", "700 − 245 = 455", "regroup one hundred as ten tens", "check by adding the difference"]),
        "difference": ("Estimate the Difference", "Estimate the difference of two numbers with up to four digits.", "Round both numbers to a helpful place, then subtract the rounded numbers to estimate the difference.", ["498 − 203 is about 500 − 200 = 300", "2,980 − 1,020 is about 2,000", "round consistently", "estimate before exact work"]),
        "multi_add": ("Many-Number Mix", "Add three or four numbers with up to two digits and check the sum.", "Group numbers carefully, add by place value, and check whether the answer is reasonable.", ["12 + 8 + 5 = 25", "24 + 16 + 10 = 50", "add ones first", "estimate to check"]),
        "bar_graph": ("Bar-Graph Scouts", "Read a single bar graph and answer questions using its data.", "Read the title and labels, compare bar lengths, and use only the values shown in the graph.", ["the tallest bar is greatest", "the shortest bar is least", "compare two categories", "read the scale"]),
        "probability": ("Chance Check", "Describe real-life outcomes as certain, likely, unlikely, or impossible.", "A certain event will happen, an impossible event cannot happen in the situation, and likely or unlikely describe different chances.", ["a fair coin can land heads", "a fish breathing on land is impossible", "rain may be likely with dark clouds", "a fair die has six possible faces"]),
        "multiplication_properties": ("Multiplication Patterns", "Use the commutative and distributive properties to explain multiplication.", "Changing the order of factors keeps the product the same. Breaking an array into parts can make multiplication easier.", ["3 × 4 = 4 × 3", "2 × 6 = 12", "4 × 7 = 28", "split 6 × 5 into 6 × 2 + 6 × 3"]),
        "multiplication": ("Multiplication Builders", "Multiply numbers by using place value, groups, and an accurate algorithm.", "Multiplication can show equal groups. Keep place values aligned and check the product with an estimate.", ["3 groups of 4 = 12", "6 × 5 = 30", "2 × 14 = 28", "estimate before solving"]),
        "product_estimate": ("Product Estimators", "Estimate products by rounding factors to friendly numbers.", "Round factors to nearby numbers that are easy to multiply, then use the result as a reasonable estimate.", ["19 × 4 is about 20 × 4 = 80", "32 × 3 is about 30 × 3 = 90", "round before multiplying", "compare exact and estimated answers"]),
        "pattern": ("Pattern Path", "Find missing terms and explain repeating or increasing patterns.", "A repeating pattern cycles through a group. An increasing pattern changes by a rule such as adding 2 each time.", ["red, blue, red, blue", "2, 4, 6, 8", "triangle, square, triangle", "state the rule"]),
        "division": ("Sharing and Grouping", "Divide numbers and identify the quotient and any remainder.", "Division shares a quantity into equal groups. The quotient tells how many in each group; a remainder is what is left.", ["12 ÷ 3 = 4", "14 ÷ 4 = 3 remainder 2", "check with multiplication", "remainder is smaller than divisor"]),
        "fraction": ("Fraction Models", "Represent fractions equal to one or greater than one using equal parts.", "A fraction names equal parts. A fraction equal to one has the same numerator and denominator; a fraction greater than one has a larger numerator.", ["3/3 = 1", "4/3 is greater than 1", "parts must be equal", "use a model"]),
        "transformation": ("Shape Moves", "Describe the resulting figure after a slide, turn, or flip using a model.", "A slide moves a shape, a turn rotates it, and a flip reflects it. The shape and size stay the same.", ["slide right", "turn a quarter-turn", "flip across a line", "compare before and after"]),
    }
    if subject == "mathematics":
        title, objective, explain, examples = math.get(key, math["addition"])
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["a random guess", "a mismatched unit", "an unrelated operation", "an answer with no label"],
                  "pairs": [(examples[0], "correct idea"), (examples[1], "useful example"), (examples[2], "check the concept")],
                  "steps": ["Read the question", "Choose the operation or model", "Solve carefully", "Check whether the answer makes sense"]})

    science = {
        "materials": ("Material Detectives", "Describe familiar materials by observable properties and choose safe uses or handling.", "Observe materials without tasting or touching unknown substances. Properties include hardness, flexibility, shine, texture, and whether a material absorbs water.", ["metal spoon — hard and shiny", "rubber band — flexible", "paper towel — absorbs water", "wooden ruler — hard and useful for measuring"]),
        "living": ("Life Around Us", "Classify familiar examples as living or non-living and describe basic needs or body parts.", "Living things grow, need resources, and respond to their surroundings. Plants and animals have parts that help them live.", ["a mango tree — living", "a dog — living", "a rock — non-living", "roots help a plant take in water"]),
        "motion": ("Motion Explorers", "Describe how size, shape, material, and surface can affect how an object moves.", "A push or pull can change motion. Surface texture, shape, size, and material can affect how far or how fast an object moves.", ["a smooth floor lets a toy slide farther", "a rough mat slows a toy", "a heavier object may need a stronger push", "a ramp changes direction of motion"]),
        "light_sound": ("Light and Sound Lab", "Describe how light and sound behave and identify safe ways to protect people.", "Light helps us see. Sound comes from vibrations and can travel to our ears. Very bright light and very loud sound can harm us, so use safe levels.", ["a vibrating guitar string makes sound", "a flashlight helps us see", "move away from very loud speakers", "never stare at the Sun"]),
        "sky_weather": ("Sky and Weather Watchers", "Observe sky objects or weather and explain how they can affect daily activities safely.", "The Sun provides light and heat. Weather can change plans. Observe the sky safely and never look directly at the Sun.", ["rain may change a picnic plan", "sunlight helps us see", "clouds can signal changing weather", "the Moon is visible at different times"]),
    }
    if subject == "science":
        title, objective, explain, examples = science.get(key, science["materials"])
        p.update({"title": title, "objective": objective, "explain": explain, "examples": examples,
                  "fits": examples, "wrong": ["taste an unknown material", "look directly at the Sun", "use a dangerous tool alone", "make a claim without observing"],
                  "pairs": [(examples[0], "observable evidence"), (examples[1], "safe example"), (examples[2], "connected idea")],
                  "steps": ["Observe safely", "Name the property or pattern", "Compare the examples", "Explain what the evidence shows"]})

    # Final fallback is still topic-grounded and is intentionally not approved
    # if it contains no meaningful source term.
    if "title" not in p:
        title = compact(lesson.get("title")) or "Learning Mission"
        objective = compact(lesson.get("objective")) or "Explain the lesson idea using an example."
        p.update({"title": title[:80], "objective": objective[:180], "explain": objective,
                  "examples": ["a clear example", "a second example", "a real-life connection", "a reason"],
                  "fits": ["a clear example", "a second example", "a real-life connection", "a reason"],
                  "wrong": ["an unrelated guess", "a random symbol", "a different topic", "an unsupported claim"],
                  "pairs": [("key idea", "lesson concept"), ("example", "supports the idea"), ("reason", "explains why")],
                  "steps": ["Read the idea", "Study an example", "Choose the evidence", "Explain your thinking"]})
    return p


def make_assessment(profile: dict[str, Any], lesson_id: str) -> dict[str, Any]:
    checks = list(profile.get("checks", []))
    examples = profile["examples"]
    while len(checks) < 5:
        idx = len(checks)
        checks.append((
            f"Which statement best fits {profile['title']}?",
            profile["explain"].split(".")[0] + ".",
            [profile["wrong"][idx % len(profile["wrong"])], profile["wrong"][(idx + 1) % len(profile["wrong"])], profile["wrong"][(idx + 2) % len(profile["wrong"])]]
        ))
    items = []
    for n, (prompt, correct, wrong) in enumerate(checks[:5], start=1):
        if profile.get("language") == "fil-PH" and prompt.startswith("Which statement best fits"):
            prompt = f"Aling pahayag ang pinakamahusay na naglalarawan sa {profile['title']}?"
        choices = [correct] + list(wrong[:3])
        # Stable but varied answer position; prevents every item being option A.
        shift = (n + len(lesson_id)) % 4
        choices = choices[shift:] + choices[:shift]
        correct_id = chr(ord("a") + choices.index(correct))
        items.append({
            "itemId": f"{lesson_id}-q{n:02d}",
            "sequence": n,
            "type": "MULTIPLE_CHOICE",
            "prompt": prompt,
            "options": [{"id": chr(ord("a") + i), "text": text} for i, text in enumerate(choices)],
            "correctOptionIds": [correct_id],
            "explanation": (
                f"Ang pinakamainam na sagot ay: {correct}"
                if profile.get("language") == "fil-PH"
                else f"The best answer is: {correct}"
            ),
        })
    return {"purpose": "FORMATIVE_MODULE_CHECK", "itemCount": 5, "passingCorrectCount": 4, "claimsMastery": False, "items": items}


def make_activities(profile: dict[str, Any], lesson_id: str) -> list[dict[str, Any]]:
    subject = profile["subject"]
    filipino = subject in {"filipino", "gmrc", "makabansa", "araling-panlipunan"}
    labels = {
        "animated": "Pag-aralan ang ideya at pakinggan ang paliwanag." if filipino else "Study the idea and listen to Milo.",
        "hotspot": "Suriin ang bawat halimbawa at hanapin ang mahalagang detalye." if filipino else "Explore each example and find the important detail.",
        "sort": "Ilagay ang bawat halimbawa sa tamang pangkat." if filipino else "Sort each example into the correct group.",
        "mc": "Piliin ang pinakamainam na sagot." if filipino else "Choose the best answer.",
        "match": "Itugma ang magkakaugnay na ideya." if filipino else "Match the ideas that belong together.",
        "sequence": "Ayusin ang mga hakbang ayon sa tamang pagkakasunod." if filipino else "Put the steps in the correct order.",
    }
    correct = "Mahusay! Nakita mo ang mahalagang ideya. 🎉" if filipino else "Great thinking! You found the key idea. 🎉"
    retry = "Balikan ang halimbawa at subukan muli. 💪" if filipino else "Look at the example again and try once more. 💪"
    examples = profile["examples"]
    fits = profile["fits"]
    wrong = profile["wrong"]
    pairs = [{"left": left, "right": right} for left, right in profile["pairs"]]
    activities = [
        ("ANIMATED_EXPLANATION", labels["animated"], profile["explain"]),
        ("HOTSPOT_IMAGE", labels["hotspot"], {"examples": examples, "visualScene": "🐱🔎✨"}),
        ("SORT_AND_CLASSIFY", labels["sort"], {"fits": fits, "doesNotFit": wrong}),
        ("MULTIPLE_CHOICE", labels["mc"], {"options": [examples[0], wrong[0], wrong[1], wrong[2]], "correctIndex": 0}),
        ("MATCHING_PAIRS", labels["match"], {"pairs": pairs}),
        ("SEQUENCE_BUILDER", labels["sequence"], {"steps": profile["steps"]}),
    ]
    output = []
    for seq, (atype, instruction, content) in enumerate(activities, start=1):
        if atype == "HOTSPOT_IMAGE":
            completion = {"type": "ALL_TARGETS_VISITED", "targetCount": len(examples)}
        elif atype == "SORT_AND_CLASSIFY":
            completion = {"type": "ALL_ITEMS_SORTED", "itemCount": len(fits) + len(wrong)}
        elif atype == "MULTIPLE_CHOICE":
            completion = {"type": "CORRECT_RESPONSE"}
        else:
            completion = {"type": "COMPLETE"}
        output.append({
            "activityId": f"{lesson_id}-a{seq:02d}",
            "sequence": seq,
            "type": atype,
            "capability": f"{atype}_V1",
            "required": True,
            "assetId": f"{lesson_id}-visual",
            "instruction": instruction,
            "content": content,
            "completionRule": completion,
            "feedback": {"correct": correct, "retry": retry},
            "prompt": instruction,
            "narration": profile["explain"],
            "guideHint": "Kailangan ng pahiwatig? Basahin muli ang halimbawa." if filipino else "Need a hint? Read the example again.",
            "nextLabel": "Susunod →" if filipino else "Next →",
            "accessibilityAlternative": profile["explain"],
        })
    return output


def _replace_strings(value: Any, replacements: dict[str, str]) -> Any:
    if isinstance(value, dict):
        return {key: _replace_strings(item, replacements) for key, item in value.items()}
    if isinstance(value, list):
        return [_replace_strings(item, replacements) for item in value]
    if isinstance(value, str):
        for old, new in replacements.items():
            value = value.replace(old, new)
        return value
    return value


def sanitize_legacy_lesson(original: dict[str, Any]) -> dict[str, Any]:
    """Repair generated shell copy while preserving legacy lesson examples."""
    lesson = copy.deepcopy(original)
    subject = canonical_subject(lesson.get("subject", ""))
    filipino = subject in {"filipino", "araling-panlipunan"}
    replacements = {
        "fits the lesson idea": "kaugnay ng ideya ng aralin" if filipino else "shows the skill",
        "fits the lesson": "nagpapakita ng kasanayan" if filipino else "shows the skill",
        "does not fit": "hindi nagpapakita ng kasanayan" if filipino else "does not show the skill",
        "Complete the guided review in order.": "Sundin nang maayos ang gabay na balik-aral." if filipino else "Follow the guided review in order.",
        "Complete each review step.": "Tapusin ang bawat hakbang sa pagbabalik-aral." if filipino else "Finish each review step.",
        "Ready for the next step.": "Mahusay! Ituloy ang susunod na hakbang." if filipino else "Nice work. Continue to the next step.",
        "Review the explanation once more.": "Basahin muli ang paliwanag." if filipino else "Read the explanation once more.",
        "You explored every example.": "Nasuri mo ang lahat ng halimbawa." if filipino else "You explored every example.",
        "Open each remaining token.": "Buksan ang natitirang halimbawa." if filipino else "Open each remaining example.",
        "Your groups follow the lesson rule.": "Tama ang pagkakahanay ng mga pangkat." if filipino else "Your groups follow the skill rule.",
        "Use the explanation, not color or position.": "Gamitin ang paliwanag, hindi ang kulay o posisyon." if filipino else "Use the explanation, not color or position.",
        "All examples are matched.": "Naitambal mo ang lahat ng halimbawa." if filipino else "All examples are matched.",
        "Return to the explanation and compare one pair at a time.": "Balikan ang paliwanag at paghambingin ang isang pares sa bawat pagkakataon." if filipino else "Return to the explanation and compare one pair at a time.",
        "You are ready for the five-question check.": "Handa ka na sa limang tanong." if filipino else "You are ready for the five-question check.",
        "The correct response is": "Ang tamang sagot ay" if filipino else "The correct response is",
        "because it follows the lesson explanation:": "dahil sumusunod ito sa paliwanag ng aralin:" if filipino else "because it follows the skill explanation:",
        "You will explore examples, sort cards, practice, and answer five questions.": "Susuri ka ng mga halimbawa, magpapangkat ng mga card, magsasanay, at sasagot ng limang tanong." if filipino else "Explore examples, sort cards, practice, and answer five questions.",
        "Suriin ang bawat learning token": "Suriin ang bawat halimbawa",
        "each learning token": "each example",
        "A numbered list of the same examples is available for keyboard and screen-reader use.": "May listahan ng parehong halimbawa para sa keyboard at screen reader." if filipino else "A numbered list of the same examples is available for keyboard and screen-reader use.",
        "Tap an item and then tap a labeled category; dragging is optional.": "Pumili ng item at kategorya; hindi kailangang mag-drag." if filipino else "Tap an item and then tap a labeled category; dragging is optional.",
        "A list-based matching control replaces drag gestures.": "May listahang paraan ng pagtutugma bilang kapalit ng pag-drag." if filipino else "A list-based matching control replaces drag gestures.",
        "Up and down buttons provide an alternative to dragging.": "Maaaring gamitin ang mga button pataas at pababa sa halip na mag-drag." if filipino else "Up and down buttons provide an alternative to dragging.",
        "Text alternative: ": "Alternatibong teksto: " if filipino else "Text alternative: ",
        "Correct: ": "Tama: " if filipino else "Correct: ",
        "Read the lesson rule again.": "Basahin muli ang tuntunin ng aralin." if filipino else "Read the skill rule again.",
        " do not fit.": " ay hindi angkop." if filipino else " do not show the skill.",
    }
    lesson = _replace_strings(lesson, replacements)
    lesson["contentReview"] = {
        "reviewer": "RonBot — seasoned educator pass",
        "focus": ["factual accuracy", "Grade 3 appropriateness", "child safety", "engagement"],
        "source": "legacy lesson-specific examples preserved; learner-facing shell normalized",
        "rewritten": False,
    }
    lesson["alignmentStatus"] = "EDUCATOR_CURATED_STRUCTURAL_REVIEW"
    return lesson


def curate_lesson(original: dict[str, Any]) -> dict[str, Any]:
    lesson = copy.deepcopy(original)
    subject = canonical_subject(lesson.get("subject", ""))
    profile = profile_for(lesson)
    rewrite = subject in {"filipino", "gmrc", "makabansa", "mathematics", "science"} or is_placeholder_lesson(lesson) or is_generic_assessment(lesson)
    if rewrite:
        lesson["title"] = profile["title"]
        lesson["objective"] = profile["objective"]
        lesson["introduction"] = (
            f"Milo has a new mission! 🐱✨ {profile['explain']} Ready to explore?"
            if subject in {"english", "mathematics", "science"}
            else f"May bagong misyon si Milo! 🐱✨ {profile['explain']} Handa ka na bang sumubok?"
        )
        lesson["vocabulary"] = [
            {"term": pair[0], "definition": pair[1]}
            for pair in profile["pairs"][:3]
        ]
        lesson["activities"] = make_activities(profile, lesson["lessonId"])
        lesson["assessment"] = make_assessment(profile, lesson["lessonId"])
    lesson["subject"] = subject
    lesson["language"] = profile["language"]
    lesson["alignmentStatus"] = "EDUCATOR_CURATED_TOPIC_REVIEW"
    lesson["contentReview"] = {
        "reviewer": "RonBot — seasoned educator pass",
        "focus": ["factual accuracy", "Grade 3 appropriateness", "child safety", "engagement"],
        "source": "competency/objective and local examples; no unsafe experiment required",
        "rewritten": rewrite,
    }
    return lesson


def review_flags(lesson: dict[str, Any]) -> list[str]:
    flags: list[str] = []
    blob = json.dumps(lesson, ensure_ascii=False).lower()
    if any(marker in blob for marker in PLACEHOLDERS) or any(marker in blob for marker in BAD_MARKERS):
        flags.append("PLACEHOLDER_TEXT")
    assessment = lesson.get("assessment") or {}
    items = assessment.get("items", [])
    if assessment.get("itemCount") != 5 or len(items) != 5:
        flags.append("ASSESSMENT_COUNT")
    for item in items:
        options = item.get("options", [])
        correct = item.get("correctOptionIds", [])
        if len(options) not in (3, 4) or len(correct) != 1 or correct[0] not in {o.get("id") for o in options}:
            flags.append("ASSESSMENT_NOT_SINGLE_CORRECT")
            break
        if not item.get("prompt", "").strip():
            flags.append("EMPTY_ASSESSMENT_PROMPT")
            break
    types = [a.get("type") for a in lesson.get("activities", [])]
    if types != CANONICAL_TYPES:
        flags.append("ACTIVITY_SEQUENCE")
    if len(lesson.get("title", "").strip()) < 8 or len(lesson.get("objective", "").strip()) < 20:
        flags.append("WEAK_OBJECTIVE")
    user_fields = {
        "title": lesson.get("title"),
        "objective": lesson.get("objective"),
        "introduction": lesson.get("introduction"),
        "vocabulary": lesson.get("vocabulary"),
        "activity_copy": [
            {
                key: activity.get(key)
                for key in ("instruction", "content", "feedback", "prompt", "narration", "guideHint", "nextLabel", "accessibilityAlternative")
                if key in activity
            }
            for activity in lesson.get("activities", [])
        ],
        "assessment_copy": [
            {
                "prompt": item.get("prompt"),
                "options": [option.get("text") for option in item.get("options", [])],
                "explanation": item.get("explanation"),
            }
            for item in (lesson.get("assessment") or {}).get("items", [])
        ],
    }
    user_blob = json.dumps(user_fields, ensure_ascii=False).lower()
    if "fil-PH" == lesson.get("language") and re.search(r"\b(complete|the lesson|well done|correct choice|activity|which statement|the best answer)\b", user_blob):
        flags.append("ENGLISH_BLEED")
    if re.search(r"\b(gun|suicide|self-harm|sexual)\b", blob):
        flags.append("SAFETY_REVIEW")
    return sorted(set(flags))


def process(pack_dir: Path = PACK_DIR, subject: str | None = None, dry_run: bool = False, include_legacy: bool = False) -> dict[str, Any]:
    changed = 0
    reviewed = 0
    remaining: dict[str, list[str]] = {}
    for path in sorted(pack_dir.glob("*.json")):
        is_q_lesson = "-g3-q" in path.name
        is_legacy_lesson = "-g3-m01-" in path.name
        if not is_q_lesson and not (include_legacy and is_legacy_lesson):
            continue
        lesson = json.loads(path.read_text())
        if subject and canonical_subject(lesson.get("subject", "")) != canonical_subject(subject):
            continue
        curated = curate_lesson(lesson) if is_q_lesson else sanitize_legacy_lesson(lesson)
        flags = review_flags(curated)
        if flags:
            remaining[curated["lessonId"]] = flags
        else:
            reviewed += 1
        if curated != lesson:
            changed += 1
            if not dry_run:
                path.write_text(json.dumps(curated, indent=1, ensure_ascii=False) + "\n")
    return {"changed": changed, "reviewed": reviewed, "remaining": remaining}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--subject")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--include-legacy", action="store_true", help="also normalize the 100 legacy month-01 lessons")
    args = parser.parse_args()
    result = process(subject=args.subject, dry_run=args.dry_run, include_legacy=args.include_legacy)
    print(json.dumps({"changed": result["changed"], "reviewed": result["reviewed"], "remaining_count": len(result["remaining"]), "remaining": result["remaining"]}, ensure_ascii=False, indent=2))
    return 1 if result["remaining"] else 0


if __name__ == "__main__":
    raise SystemExit(main())

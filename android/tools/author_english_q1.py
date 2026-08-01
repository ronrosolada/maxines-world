#!/usr/bin/env python3
"""
Author English Q1 SLM source lessons (Grade 3, DepEd Matatag-aligned).

Creates 20 lessons (Q1 weeks 1-4 × 5 days) in the SLM source format at
app/src/main/assets/content/ph-matatag/grade-3/english/module-15..18/,
aligned to the skills already covered by the legacy month-01 English pack
(short vowels, blends, digraphs, sight words, nouns, be-verbs, tenses,
possessives, cause/effect, details, retelling, picture reading).

Skill map (week → days):
  W1  Picture reading, characters, endings, diary, telling sentences
  W2  Common/proper nouns, plurals (-s, -es/-ies), syllables
  W3  Short vowels, be-verbs, verb tenses, blends, digraphs
  W4  Sight words, possessives, cause/effect, details, retelling

Usage:
  python3 tools/author_english_q1.py [--dry-run]

Output: 20 SLM JSONs; the existing converter (convert_slm_to_pack.py) turns
them into playable lessons automatically.
"""

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]  # android/
ENGLISH_DIR = REPO_ROOT / "app/src/main/assets/content/ph-matatag/grade-3/english"

ACCESSIBILITY = {
    "narrationAvailable": False,
    "captionsAvailable": True,
    "reducedMotionSupported": True,
    "dragAlternativeAvailable": True,
    "colorIndependent": True,
}


def act(lesson_id, seq, atype, instruction, content, correct, retry, prompt, narration,
        guide_hint="", alt="", asset_id=None, next_label="Next →", completion=None):
    """Build one activity dict in the SLM schema."""
    a = {
        "activityId": f"{lesson_id}-a{seq:02d}",
        "sequence": seq,
        "type": atype,
        "capability": f"{atype}_V1",
        "required": True,
        "assetId": asset_id or f"{lesson_id}-visual",
        "instruction": instruction,
        "content": content,
        "completionRule": completion or {"type": "VIEW_AND_ACKNOWLEDGE"},
        "feedback": {"correct": correct, "retry": retry},
        "prompt": prompt,
        "narration": narration,
    }
    if alt:
        a["accessibilityAlternative"] = alt
    if guide_hint:
        a["guideHint"] = guide_hint
    a["nextLabel"] = next_label
    return a


def mc_content(options, correct_index):
    return {"options": options, "correctIndex": correct_index}


def build_lesson(lesson_id, week, day, title, objective, intro, story, scene,
                 vocabulary, activities, assessment_qs):
    """Assemble a full SLM lesson dict."""
    return {
        "lessonId": lesson_id,
        "schemaVersion": 1,
        "grade": 3,
        "quarter": 1,
        "week": week,
        "day": day,
        "subject": "ENGLISH",
        "title": title,
        "objective": objective,
        "estimatedMinutes": 12,
        "educatorValidated": False,
        "releaseStatus": "REQUIRES_EDUCATOR_REVIEW",
        "qualifiesForDailyBadge": True,
        "language": "en-PH",
        "contentVersion": 1,
        "introduction": intro,
        "storyIntro": story,
        "scene": scene,
        "vocabulary": vocabulary,
        "activities": activities,
        "assessment": {
            "passingCorrectCount": 4,
            "totalItems": 5,
            "items": [
                {
                    "question": q["q"],
                    "choices": [{"text": c, "correct": i == q["correct"]}
                                for i, c in enumerate(q["choices"])],
                    "narration": f"Milo asks: {q['q']}",
                }
                for q in assessment_qs
            ],
        },
        "accessibility": ACCESSIBILITY,
    }


# ---------------------------------------------------------------- W1: story basics
W1 = [
    dict(
        title="Picture Detective",
        objective="Look closely at a picture and find important details.",
        intro="Detectives look at pictures very carefully. Today YOU are the detective!",
        story="Milo shows Maxine a big picture of the town fiesta. \"Look closely, detective — what do you see?\"",
        scene={"character": "Milo the Cat", "setting": "Milo's Learning Treehouse"},
        vocabulary=[
            {"term": "Detail", "definition": "A small piece of information you can see or notice."},
            {"term": "Detective", "definition": "Someone who looks carefully to find clues."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Detectives look for details in pictures.",
             {"text": "Details are the little things you see: colors, people, places, and actions."},
             "You found the clue!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each detail you can find in the fiesta picture!",
             {"examples": ["a red flag", "people dancing", "food on the table", "children laughing",
                           "a band playing", "streamers on the street", "a parade", "lanterns"]},
             "Great detective work!", "Look again — there are more details to find!"),
            ("SORT_AND_CLASSIFY", "Sort: Which are details you can SEE in a picture?",
             {"fits": ["colors", "people", "places", "actions"],
              "doesNotFit": ["sounds", "smells", "feelings inside your head", "what someone is thinking"]},
             "You sorted them all!", "Remember — pictures show things you can see."),
            ("MULTIPLE_CHOICE", "Which of these is a detail you can SEE in a picture?",
             mc_content(["a red flag", "a loud sound", "a sweet smell", "a sad feeling"], 0),
             "Yes! The red flag is something you can see.", "Look at the picture again. What can your eyes see?"),
            ("MATCHING_PAIRS", "Match each picture detail to where you would find it!",
             {"pairs": [{"left": "a parade", "right": "in the street"},
                        {"left": "food", "right": "on the table"},
                        {"left": "lanterns", "right": "hanging high"},
                        {"left": "children", "right": "playing games"}]},
             "Perfect matching!", "Think about where each thing belongs."),
            ("SEQUENCE_BUILDER", "Put the detective steps in order: How do we look at a picture?",
             {"steps": ["Look at the whole picture", "Find the people and places", "Spot the small details", "Tell what you see"]},
             "You're a real detective now!", "What do you do first when you look at a picture?"),
        ],
        assessment=[
            dict(q="What do detectives look for in a picture?", choices=["Details", "Loud sounds", "Sweet smells", "Dreams"], correct=0),
            dict(q="Which of these can you SEE in a picture?", choices=["Colors", "Music", "Perfume", "Wind"], correct=0),
            dict(q="Where do you find the details in a story picture?", choices=["Inside the picture", "Under your bed", "In your pocket", "In the sky"], correct=0),
            dict(q="When you look at a picture, what should you do first?", choices=["Look at the whole picture", "Close your eyes", "Run away", "Turn it over"], correct=0),
            dict(q="Who is the detective in our lesson?", choices=["You!", "A bird", "A robot", "A fish"], correct=0),
        ],
    ),
    dict(
        title="Meet the Characters",
        objective="Identify the characters in a story.",
        intro="Every story has characters — the people or animals in the story!",
        story="Milo opens a storybook. \"This story has a girl, her dog, and a grumpy cat. They are the CHARACTERS!\"",
        scene={"character": "Milo the Cat", "setting": "Storybook Corner"},
        vocabulary=[
            {"term": "Character", "definition": "A person, animal, or make-believe being in a story."},
            {"term": "Story", "definition": "A tale about characters and what happens to them."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Characters are the people or animals in a story.",
             {"text": "Stories have characters. They can be people, animals, or even talking things!"},
             "Now you know about characters!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each character you can find in the storybook picture!",
             {"examples": ["a girl named Ana", "a brown dog", "a grumpy cat", "a kind Lola", "a little bird", "a mailman", "a fish in the pond", "a neighbor boy"]},
             "You found every character!", "Look again — who else is in the picture?"),
            ("SORT_AND_CLASSIFY", "Sort: Which are characters in the story?",
             {"fits": ["Ana", "the brown dog", "the grumpy cat", "the kind Lola"],
              "doesNotFit": ["the mango tree", "the blue sky", "the stone fence", "the hot sun"]},
             "Sorted perfectly!", "Characters are the ones who talk and act in the story."),
            ("MULTIPLE_CHOICE", "Who is a character in the story?",
             mc_content(["the grumpy cat", "the mango tree", "the stone fence", "the blue sky"], 0),
             "Yes! The cat is a character.", "Think — which one can talk or move in the story?"),
            ("MATCHING_PAIRS", "Match each character to what they do!",
             {"pairs": [{"left": "Ana", "right": "reads the story"},
                        {"left": "the brown dog", "right": "wags its tail"},
                        {"left": "the grumpy cat", "right": "says meow"},
                        {"left": "Lola", "right": "bakes bibingka"}]},
             "You know the characters well!", "Who does what in the story?"),
            ("SEQUENCE_BUILDER", "Put the story steps in order: What happens in our story?",
             {"steps": ["Ana opens the book", "The dog comes to listen", "The cat naps nearby", "Lola serves bibingka"]},
             "You retold the story!", "What happens first in the story?"),
        ],
        assessment=[
            dict(q="What are the people or animals in a story called?", choices=["Characters", "Colors", "Numbers", "Shapes"], correct=0),
            dict(q="Which one is a character?", choices=["The brown dog", "The mango tree", "The stone fence", "The blue sky"], correct=0),
            dict(q="Can animals be characters in a story?", choices=["Yes", "No", "Only at night", "Only in winter"], correct=0),
            dict(q="Where do characters live in a story?", choices=["In the story", "In a jar", "In a box", "Under the sea only"], correct=0),
            dict(q="Who is telling us about characters today?", choices=["Milo the Cat", "A robot", "A cloud", "A shoe"], correct=0),
        ],
    ),
    dict(
        title="Choose an Ending",
        objective="Pick a possible ending for a story.",
        intro="Stories can end in different ways. Today you choose the ending!",
        story="Milo tells a story: \"The little duck lost its way home...\" then he stops. \"You choose how it ends!\"",
        scene={"character": "Milo the Cat", "setting": "Pond-side Storytime"},
        vocabulary=[
            {"term": "Ending", "definition": "The last part of a story — what happens in the end."},
            {"term": "Beginning", "definition": "The first part of a story, where it starts."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! The ending is the last part of a story.",
             {"text": "A story has a beginning, a middle, and an ending. The ending tells how the story finishes."},
             "Now you know about endings!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each possible ending for the little duck's story!",
             {"examples": ["The duck finds its mother", "A kind boy helps the duck", "The duck swims home", "The duck makes new friends",
                           "The duck sleeps on a lily pad", "The moon guides the duck home"]},
             "So many good endings!", "Think — what else could happen at the end?"),
            ("SORT_AND_CLASSIFY", "Sort: Which belong at the END of a story?",
             {"fits": ["The duck finds its mother", "Everyone is happy", "The duck is home safe"],
              "doesNotFit": ["Once upon a time", "This is the beginning", "Long ago in a forest"]},
             "Sorted! Endings come last.", "Endings tell how the story finishes."),
            ("MULTIPLE_CHOICE", "Which is a good ending for the little duck?",
             mc_content(["The duck finds its mother and quacks happily", "The duck forgets how to swim", "The story has no duck", "The duck never goes home"], 0),
             "Yes! That ending makes sense.", "What ending would make the duck happy?"),
            ("MATCHING_PAIRS", "Match each story part to its place!",
             {"pairs": [{"left": "Once upon a time", "right": "beginning"},
                        {"left": "The duck swam and swam", "right": "middle"},
                        {"left": "The duck found its home", "right": "ending"},
                        {"left": "Everyone lived happily", "right": "ending"}]},
             "You know story parts!", "Where does each part belong?"),
            ("SEQUENCE_BUILDER", "Put the duck's story in order!",
             {"steps": ["The duck is lost", "The duck swims across the pond", "A kind boy shows the way", "The duck finds its mother"]},
             "Perfect story order!", "What happens first?"),
        ],
        assessment=[
            dict(q="What is the last part of a story called?", choices=["The ending", "The cover", "The title", "The page number"], correct=0),
            dict(q="Which is a good ending?", choices=["The duck is home safe", "The duck disappears", "The duck turns into a cloud", "The story stops mid-air"], correct=0),
            dict(q="What comes first in a story?", choices=["The beginning", "The ending", "The last page", "The epilogue"], correct=0),
            dict(q="Can a story have different endings?", choices=["Yes", "No", "Only on Mondays", "Only in the rain"], correct=0),
            dict(q="Who was lost in Milo's story?", choices=["The little duck", "The big boat", "The tall tree", "The red kite"], correct=0),
        ],
    ),
    dict(
        title="Maxine's Little Diary",
        objective="Tell about your day using first-person sentences.",
        intro="A diary is a book where YOU write about your day. Today you write like Maxine!",
        story="Maxine writes in her diary: \"Dear Diary, today I played with Milo...\" Milo peeks and giggles.",
        scene={"character": "Maxine and Milo", "setting": "Maxine's Room"},
        vocabulary=[
            {"term": "Diary", "definition": "A book where you write about things that happen to you."},
            {"term": "First person", "definition": "Talking about yourself using words like I, me, and my."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! A diary uses words like I, me, and my.",
             {"text": "When you write about yourself, you say 'I', 'me', and 'my'. That is first person!"},
             "You understand diaries now!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each sentence Maxine could write in her diary!",
             {"examples": ["I played with Milo today", "My Lola cooked adobo", "I drew a rainbow", "Me and Ana skipped",
                           "My cat slept on my pillow", "I ate a mango", "My family watched the stars", "I read a book"]},
             "Great diary sentences!", "What else did Maxine do today?"),
            ("SORT_AND_CLASSIFY", "Sort: Which sentences use first person (I, me, my)?",
             {"fits": ["I played today", "My cat is soft", "Me and Ana laughed", "I love mangoes"],
              "doesNotFit": ["The dog ran", "Ana sings", "The sun is hot", "Rivers flow"]},
             "Sorted! I, me, my = first person.", "Look for I, me, or my in the sentence."),
            ("MULTIPLE_CHOICE", "Which sentence belongs in Maxine's diary?",
             mc_content(["I played with Milo today", "The table is brown", "Trees have leaves", "Cars drive fast"], 0),
             "Yes! That is about Maxine herself.", "Which sentence talks about Maxine's day?"),
            ("MATCHING_PAIRS", "Match each diary word to its meaning!",
             {"pairs": [{"left": "I", "right": "me, myself"},
                        {"left": "my", "right": "belonging to me"},
                        {"left": "diary", "right": "my little book"},
                        {"left": "today", "right": "this day"}]},
             "You know diary words!", "What does each word mean?"),
            ("SEQUENCE_BUILDER", "Put Maxine's day in order: What did she write in her diary?",
             {"steps": ["I woke up", "I played with Milo", "I ate Lola's adobo", "I wrote in my diary"]},
             "Her day is complete!", "What did Maxine do first?"),
        ],
        assessment=[
            dict(q="What do you write in a diary?", choices=["About your day", "Math problems", "List of animals", "Boat designs"], correct=0),
            dict(q="Which word is first person?", choices=["I", "They", "Them", "It"], correct=0),
            dict(q="Which sentence is about Maxine?", choices=["I played with Milo", "The sun is bright", "Dogs bark", "Birds fly"], correct=0),
            dict(q="Who writes in a diary?", choices=["You do", "Only teachers", "Only kings", "Only birds"], correct=0),
            dict(q="What did Maxine eat in her diary?", choices=["Lola's adobo", "A hamburger", "Ice cream", "Pizza"], correct=0),
        ],
    ),
    dict(
        title="Telling Sentences",
        objective="Write telling sentences that end with a period.",
        intro="A telling sentence tells something. It ends with a period (.).",
        story="Milo says: \"Maxine, tell me something true!\" Maxine answers, \"I like mangoes.\" Milo smiles. \"That is a telling sentence!\"",
        scene={"character": "Milo the Cat", "setting": "Milo's Learning Treehouse"},
        vocabulary=[
            {"term": "Telling sentence", "definition": "A sentence that tells something and ends with a period."},
            {"term": "Period", "definition": "The dot (.) at the end of a telling sentence."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Telling sentences end with a period.",
             {"text": "A telling sentence gives information. It ends with a period (.). Example: I like mangoes."},
             "Now you can make telling sentences!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each telling sentence you can say today!",
             {"examples": ["I like mangoes", "The sky is blue", "My Lola is kind", "Dogs are furry",
                           "The sun is bright", "I can count to ten", "Milo is a cat", "Rice grows in the fields"]},
             "Wonderful telling sentences!", "What else can you tell?"),
            ("SORT_AND_CLASSIFY", "Sort: Which are telling sentences?",
             {"fits": ["I like mangoes", "The sky is blue", "My Lola is kind", "Dogs are furry"],
              "doesNotFit": ["Who is there?", "Where are you going?", "Look out!", "Why is it late?"]},
             "Sorted! Those all tell something.", "Telling sentences give information."),
            ("MULTIPLE_CHOICE", "Which is a telling sentence?",
             mc_content(["The sun is bright.", "Is the sun bright?", "Wow, the sun!", "Sun?"], 0),
             "Yes! That sentence tells us something.", "Which one gives information about the sun?"),
            ("MATCHING_PAIRS", "Match each sentence to its ending mark!",
             {"pairs": [{"left": "I like mangoes", "right": "."},
                        {"left": "The sky is blue", "right": "."},
                        {"left": "Milo is a cat", "right": "."},
                        {"left": "Rice grows in fields", "right": "."}]},
             "All telling sentences end with a period!", "What mark ends a telling sentence?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we make a telling sentence?",
             {"steps": ["Think of something true", "Say the words", "Write the sentence", "Put a period at the end"]},
             "You made telling sentences!", "What do you do first?"),
        ],
        assessment=[
            dict(q="What mark ends a telling sentence?", choices=["A period (.)", "A question mark (?)", "An exclamation point (!)", "A comma (,)"], correct=0),
            dict(q="Which is a telling sentence?", choices=["Milo is a cat.", "Is Milo a cat?", "Milo!", "Cat?"], correct=0),
            dict(q="What does a telling sentence do?", choices=["It tells something", "It asks a question", "It shouts", "It whispers secrets"], correct=0),
            dict(q="Which of these tells something true?", choices=["The sky is blue.", "Who is that?", "Stop!", "Why?"], correct=0),
            dict(q="What does Maxine like?", choices=["Mangoes", "Rocks", "Brooms", "Rain"], correct=0),
        ],
    ),
]

# ---------------------------------------------------------------- W2: nouns, vowels & plurals
W2 = [
    dict(
        title="Common or Proper?",
        objective="Tell the difference between common and proper nouns.",
        intro="Nouns name people, places, and things. Some are common, and some are special!",
        story="Milo shows two lists. \"Look! 'cat' is common, but 'Milo' is special. Special names are PROPER nouns!\"",
        scene={"character": "Milo the Cat", "setting": "Word Garden"},
        vocabulary=[
            {"term": "Common noun", "definition": "A general name for a person, place, or thing, like cat or city."},
            {"term": "Proper noun", "definition": "The special name of a person, place, or thing, like Milo or Manila."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Proper nouns are special names with CAPITAL letters.",
             {"text": "Common nouns are general: cat, city, day. Proper nouns are special names: Milo, Manila, Monday. Proper nouns start with a capital letter!"},
             "You know nouns now!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each PROPER noun (special name) you can find!",
             {"examples": ["Milo", "Manila", "Monday", "Maxine", "Lola Rosa", "Palawan", "June", "Ana"]},
             "You found the special names!", "Look for names with capital letters."),
            ("SORT_AND_CLASSIFY", "Sort: Which are PROPER nouns (special names)?",
             {"fits": ["Milo", "Manila", "Maxine", "Palawan"],
              "doesNotFit": ["cat", "city", "girl", "island"]},
             "Perfect! Special names = proper nouns.", "Proper nouns start with a capital letter."),
            ("MULTIPLE_CHOICE", "Which is a proper noun?",
             mc_content(["Manila", "city", "river", "tree"], 0),
             "Yes! Manila is a special name.", "Which one is a special name with a capital letter?"),
            ("MATCHING_PAIRS", "Match each common noun to its proper noun!",
             {"pairs": [{"left": "cat", "right": "Milo"},
                        {"left": "city", "right": "Manila"},
                        {"left": "girl", "right": "Maxine"},
                        {"left": "island", "right": "Palawan"}]},
             "Great matching!", "What is the special name for each?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we spot a proper noun?",
             {"steps": ["Look at the word", "Is it a special name?", "Does it start with a capital letter?", "If yes — it's a proper noun!"]},
             "You're a noun expert!", "What do you check first?"),
        ],
        assessment=[
            dict(q="What is the special name for a cat in our story?", choices=["Milo", "Pusa", "Kitty", "Mingming"], correct=0),
            dict(q="Which is a proper noun?", choices=["Palawan", "island", "beach", "sand"], correct=0),
            dict(q="How do proper nouns start?", choices=["With a capital letter", "With a small letter", "With a number", "With a dot"], correct=0),
            dict(q="Which is a common noun?", choices=["city", "Manila", "Monday", "June"], correct=0),
            dict(q="What do nouns name?", choices=["People, places, and things", "Only colors", "Only numbers", "Only sounds"], correct=0),
        ],
    ),
    dict(
        title="More Than One",
        objective="Make plural nouns by adding -s.",
        intro="When there is more than one, we add -s to most nouns!",
        story="Milo holds one apple, then two. \"One apple, TWO apples! We add -s to make MORE than one.\"",
        scene={"character": "Milo the Cat", "setting": "Fruit Stand"},
        vocabulary=[
            {"term": "Plural", "definition": "More than one, like apples or cats."},
            {"term": "Singular", "definition": "Only one, like apple or cat."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Add -s to make most nouns plural.",
             {"text": "One apple → two apples. One cat → three cats. Add -s when there is more than one!"},
             "You can make plurals now!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each PLURAL word (more than one) you can find!",
             {"examples": ["apples", "cats", "mangoes", "tables", "books", "stars", "flowers", "chairs"]},
             "You found the plurals!", "Look for the -s at the end."),
            ("SORT_AND_CLASSIFY", "Sort: Which words mean MORE than one?",
             {"fits": ["apples", "cats", "books", "stars"],
              "doesNotFit": ["apple", "cat", "book", "star"]},
             "Sorted! -s means more than one.", "Which words have an -s?"),
            ("MULTIPLE_CHOICE", "Which word means more than one cat?",
             mc_content(["cats", "cat", "catt", "ca"], 0),
             "Yes! Cats = more than one cat.", "Which word has an -s at the end?"),
            ("MATCHING_PAIRS", "Match each singular word to its plural!",
             {"pairs": [{"left": "apple", "right": "apples"},
                        {"left": "cat", "right": "cats"},
                        {"left": "book", "right": "books"},
                        {"left": "star", "right": "stars"}]},
             "Perfect plurals!", "What do we add to make more than one?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we make a plural?",
             {"steps": ["Start with one thing: apple", "Want more than one?", "Add -s at the end", "Say: apples!"]},
             "You made plurals!", "What do we do first?"),
        ],
        assessment=[
            dict(q="What do we add to make most nouns plural?", choices=["-s", "-x", "-p", "-z"], correct=0),
            dict(q="One apple, two ___?", choices=["apples", "apple", "appli", "applo"], correct=0),
            dict(q="Which word is plural?", choices=["cats", "cat", "cot", "cut"], correct=0),
            dict(q="What does plural mean?", choices=["More than one", "Only one", "None", "A color"], correct=0),
            dict(q="What did Milo hold in the fruit stand?", choices=["apples", "shoes", "hats", "balls"], correct=0),
        ],
    ),
    dict(
        title="Plural Word Changers",
        objective="Make plurals that end in -es and -ies.",
        intro="Some nouns change more! Words ending in -ch, -sh, -x add -es. Words ending in -y become -ies!",
        story="Milo scratches his head. \"One box, two boxes! One baby, two babies! English is tricky — let's practice!\"",
        scene={"character": "Milo the Cat", "setting": "Word Workshop"},
        vocabulary=[
            {"term": "-es plural", "definition": "Add -es to words ending in -ch, -sh, -s, -x: box → boxes."},
            {"term": "-ies plural", "definition": "Change -y to -ies when the letter before is a consonant: baby → babies."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! -ch, -sh, -x words add -es; -y words become -ies.",
             {"text": "Box → boxes. Dish → dishes. Watch → watches. Baby → babies. City → cities. When -y has a consonant before it, change -y to -ies!"},
             "You learned the word changers!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each plural made with -es or -ies!",
             {"examples": ["boxes", "dishes", "watches", "babies", "cities", "buses", "foxes", "stories"]},
             "You found the tricky plurals!", "Look for -es and -ies endings."),
            ("SORT_AND_CLASSIFY", "Sort: Which plurals end in -es?",
             {"fits": ["boxes", "dishes", "watches", "buses"],
              "doesNotFit": ["babies", "cities", "stories", "berries"]},
             "Sorted! Those add -es.", "-ies words change y to i."),
            ("MULTIPLE_CHOICE", "What is the plural of baby?",
             mc_content(["babies", "babys", "babyes", "babbies"], 0),
             "Yes! Baby → babies.", "Change the -y to -ies!"),
            ("MATCHING_PAIRS", "Match each singular word to its plural!",
             {"pairs": [{"left": "box", "right": "boxes"},
                        {"left": "dish", "right": "dishes"},
                        {"left": "baby", "right": "babies"},
                        {"left": "city", "right": "cities"}]},
             "You mastered the changers!", "How does each word change?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we make 'city' plural?",
             {"steps": ["Look at the word: city", "See the -y at the end", "Change -y to -ies", "Say: cities!"]},
             "You changed the word!", "What do we look at first?"),
        ],
        assessment=[
            dict(q="What is the plural of box?", choices=["boxes", "boxs", "boxies", "box"], correct=0),
            dict(q="What is the plural of baby?", choices=["babies", "babys", "babyes", "babes"], correct=0),
            dict(q="What is the plural of dish?", choices=["dishes", "dishs", "dishies", "disheses"], correct=0),
            dict(q="What is the plural of city?", choices=["cities", "citys", "cityes", "citiez"], correct=0),
            dict(q="Which word ends in -es?", choices=["watches", "cats", "apples", "stars"], correct=0),
        ],
    ),
    dict(
        title="Short-Vowel Sound Lab",
        objective="Hear and say the short vowel sounds: a, e, i, o, u.",
        intro="Vowels are a, e, i, o, u. Each has a short sound. Let's visit the Sound Lab!",
        story="Milo wears a lab coat. \"In the Sound Lab, we listen! A as in 'apple', E as in 'egg', I as in 'igloo'!\"",
        scene={"character": "Milo the Cat", "setting": "Sound Lab"},
        vocabulary=[
            {"term": "Vowel", "definition": "The letters a, e, i, o, u."},
            {"term": "Short sound", "definition": "The quick vowel sound: a-apple, e-egg, i-igloo, o-octopus, u-umbrella."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Short vowel sounds: a, e, i, o, u.",
             {"text": "A says /a/ as in apple. E says /e/ as in egg. I says /i/ as in igloo. O says /o/ as in octopus. U says /u/ as in umbrella."},
             "You know the vowel sounds!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each word and say its short vowel sound!",
             {"examples": ["apple (a)", "egg (e)", "igloo (i)", "octopus (o)", "umbrella (u)",
                           "cat (a)", "bed (e)", "fish (i)", "dog (o)", "sun (u)"]},
             "You found all the sounds!", "Say the vowel sound in each word."),
            ("SORT_AND_CLASSIFY", "Sort: Which words have the short /a/ sound?",
             {"fits": ["apple", "cat", "bag", "hat"],
              "doesNotFit": ["egg", "igloo", "octopus", "umbrella"]},
             "Sorted! Those all say /a/.", "Listen for the /a/ sound."),
            ("MULTIPLE_CHOICE", "Which word has the short /a/ sound?",
             mc_content(["cat", "dog", "sun", "egg"], 0),
             "Yes! C-a-t says /a/.", "Say each word — which has the /a/ sound?"),
            ("MATCHING_PAIRS", "Match each vowel to a word with its short sound!",
             {"pairs": [{"left": "a", "right": "apple"},
                        {"left": "e", "right": "egg"},
                        {"left": "i", "right": "igloo"},
                        {"left": "u", "right": "umbrella"}]},
             "You matched all vowels!", "What sound does each vowel make?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we find a vowel sound?",
             {"steps": ["Look at the word", "Find the vowel letter", "Say the word slowly", "Listen for the sound"]},
             "You're a sound scientist!", "What do we do first?"),
        ],
        assessment=[
            dict(q="What are the five vowels?", choices=["a, e, i, o, u", "b, c, d, f, g", "1, 2, 3, 4, 5", "x, y, z, w, v"], correct=0),
            dict(q="Which word has the short /a/ sound?", choices=["cat", "dog", "sun", "bee"], correct=0),
            dict(q="What sound does 'u' make in umbrella?", choices=["/u/", "/a/", "/i/", "/o/"], correct=0),
            dict(q="Which word has the short /e/ sound?", choices=["egg", "cat", "sun", "box"], correct=0),
            dict(q="Who is the sound scientist?", choices=["Milo", "Maxine", "Ana", "Lola"], correct=0),
        ],
    ),
    dict(
        title="Clap the Syllables",
        objective="Count syllables by clapping parts of a word.",
        intro="Every word has parts called syllables. Clap once for each part!",
        story="Milo claps slowly: \"Man-go! Two claps! Ba-na-na! Three claps! Let's clap together!\"",
        scene={"character": "Milo the Cat", "setting": "Music Corner"},
        vocabulary=[
            {"term": "Syllable", "definition": "A part of a word. Mango has two parts: man-go."},
            {"term": "Clap", "definition": "To tap your hands together to count parts."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Clap once for each syllable in a word.",
             {"text": "Mango = man-go, two claps. Banana = ba-na-na, three claps. Clap the parts of the word!"},
             "You can count syllables!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each word and think: how many claps?",
             {"examples": ["mango (2)", "cat (1)", "banana (3)", "sun (1)", "elephant (3)", "school (1)", "kangaroo (3)", "apple (2)"]},
             "You counted the parts!", "Clap each word slowly."),
            ("SORT_AND_CLASSIFY", "Sort: Which words have ONE clap (1 syllable)?",
             {"fits": ["cat", "sun", "school", "fish"],
              "doesNotFit": ["mango", "banana", "elephant", "kangaroo"]},
             "Sorted! Those are one-part words.", "Say each word slowly — how many parts?"),
            ("MULTIPLE_CHOICE", "How many syllables does 'mango' have?",
             mc_content(["2", "1", "5", "10"], 0),
             "Yes! Man-go = 2 claps.", "Clap it: man-go. How many claps?"),
            ("MATCHING_PAIRS", "Match each word to its claps!",
             {"pairs": [{"left": "cat", "right": "1 clap"},
                        {"left": "mango", "right": "2 claps"},
                        {"left": "banana", "right": "3 claps"},
                        {"left": "school", "right": "1 clap"}]},
             "You counted them all!", "Clap each word to check."),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we count syllables?",
             {"steps": ["Say the word slowly", "Clap for each part", "Count your claps", "Say the number"]},
             "You're a syllable counter!", "What do you do first?"),
        ],
        assessment=[
            dict(q="What is a syllable?", choices=["A part of a word", "A color", "A number", "A shape"], correct=0),
            dict(q="How many syllables in 'cat'?", choices=["1", "2", "3", "4"], correct=0),
            dict(q="How many syllables in 'mango'?", choices=["2", "1", "4", "5"], correct=0),
            dict(q="How do we count syllables?", choices=["By clapping parts", "By closing eyes", "By jumping", "By singing"], correct=0),
            dict(q="How many claps for 'banana'?", choices=["3", "1", "2", "10"], correct=0),
        ],
    ),
]

# ---------------------------------------------------------------- W3: verbs & phonics
W3 = [
    dict(
        title="Be-Verb Team",
        objective="Use am, is, and are correctly.",
        intro="The be-verb team has three players: am, is, are. Each matches different words!",
        story="Milo lines up three signs: \"I AM here. He IS here. They ARE here. The team works together!\"",
        scene={"character": "Milo the Cat", "setting": "Grammar Stadium"},
        vocabulary=[
            {"term": "Be-verb", "definition": "The verbs am, is, and are — they link things together."},
            {"term": "Match", "definition": "Use the be-verb that fits the word: I → am, he/she/it → is, they/we → are."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! I → am. He, she, it → is. We, they → are.",
             {"text": "I am happy. She is happy. They are happy. Match the be-verb to the word!"},
             "You joined the be-verb team!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each correct be-verb pair!",
             {"examples": ["I am", "He is", "She is", "They are", "We are", "It is", "You are", "Milo is"]},
             "You found the matches!", "Which be-verb fits each word?"),
            ("SORT_AND_CLASSIFY", "Sort: Which pairs use 'is'?",
             {"fits": ["He is", "She is", "It is", "Milo is"],
              "doesNotFit": ["I am", "They are", "We are", "You are"]},
             "Sorted! He, she, it → is.", "Which words go with is?"),
            ("MULTIPLE_CHOICE", "Which is correct?",
             mc_content(["They are playing", "They is playing", "They am playing", "They are plays"], 0),
             "Yes! They → are.", "Which be-verb matches 'they'?"),
            ("MATCHING_PAIRS", "Match each word to its be-verb!",
             {"pairs": [{"left": "I", "right": "am"},
                        {"left": "She", "right": "is"},
                        {"left": "They", "right": "are"},
                        {"left": "We", "right": "are"}]},
             "You matched the whole team!", "What be-verb goes with each word?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we choose a be-verb?",
             {"steps": ["Look at the word", "Is it I? → am", "Is it he, she, it? → is", "Is it they, we? → are"]},
             "You chose the right verbs!", "What do we check first?"),
        ],
        assessment=[
            dict(q="Which be-verb goes with I?", choices=["am", "is", "are", "was"], correct=0),
            dict(q="Which be-verb goes with she?", choices=["is", "am", "are", "be"], correct=0),
            dict(q="Which is correct?", choices=["They are happy", "They is happy", "They am happy", "They be happy"], correct=0),
            dict(q="Which is correct?", choices=["I am hungry", "I is hungry", "I are hungry", "I be hungry"], correct=0),
            dict(q="Who are the three be-verb players?", choices=["am, is, are", "a, e, i", "1, 2, 3", "cat, dog, bird"], correct=0),
        ],
    ),
    dict(
        title="Yesterday, Today, Tomorrow",
        objective="Use past, present, and future action words.",
        intro="Actions happen in time: yesterday (past), today (present), tomorrow (future)!",
        story="Milo draws a line: \"Yesterday I PLAYED. Today I PLAY. Tomorrow I WILL PLAY. Time changes the verb!\"",
        scene={"character": "Milo the Cat", "setting": "Time Tunnel"},
        vocabulary=[
            {"term": "Past", "definition": "Already happened, like played or ate."},
            {"term": "Future", "definition": "Will happen, like will play or will eat."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Past: played. Present: play. Future: will play.",
             {"text": "Yesterday I played. Today I play. Tomorrow I will play. Add -ed for past, use will for future!"},
             "You understand time words!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each PAST action word!",
             {"examples": ["played", "jumped", "ate", "walked", "sang", "danced", "helped", "smiled"]},
             "You found the past words!", "Past words often end in -ed."),
            ("SORT_AND_CLASSIFY", "Sort: Which words are PAST (already happened)?",
             {"fits": ["played", "jumped", "walked", "helped"],
              "doesNotFit": ["play", "jump", "walk", "help"]},
             "Sorted! Those already happened.", "Past words often end in -ed."),
            ("MULTIPLE_CHOICE", "Which sentence is about the FUTURE?",
             mc_content(["I will play tomorrow", "I played yesterday", "I play today", "I am playing now"], 0),
             "Yes! Will = future.", "Which one has 'will'?"),
            ("MATCHING_PAIRS", "Match each time word to its action!",
             {"pairs": [{"left": "yesterday", "right": "played"},
                        {"left": "today", "right": "play"},
                        {"left": "tomorrow", "right": "will play"},
                        {"left": "last week", "right": "sang"}]},
             "You matched time and action!", "When did each action happen?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: Maxine's day at the park!",
             {"steps": ["Yesterday I played", "Today I am playing", "Tomorrow I will play", "Every day is fun!"]},
             "You ordered the time line!", "What happened first?"),
        ],
        assessment=[
            dict(q="What word shows the FUTURE?", choices=["will", "ed", "ing", "ly"], correct=0),
            dict(q="Which is PAST tense?", choices=["played", "play", "plays", "playing"], correct=0),
            dict(q="Yesterday I ___ with Milo.", choices=["played", "play", "will play", "plays"], correct=0),
            dict(q="Tomorrow I ___ my Lola.", choices=["will visit", "visited", "visit", "visits"], correct=0),
            dict(q="Where did Milo draw the time line?", choices=["In the Time Tunnel", "In the sea", "On a boat", "In the sky"], correct=0),
        ],
    ),
    dict(
        title="Blend Builders",
        objective="Read words with consonant blends: bl, cl, gr, st, tr.",
        intro="Blends are two consonants that blend together: bl as in 'blue', st as in 'star'!",
        story="Milo holds two letters and squishes them together: \"B... L... BLUE! The letters blend!\"",
        scene={"character": "Milo the Cat", "setting": "Blend Workshop"},
        vocabulary=[
            {"term": "Blend", "definition": "Two consonants that blend together, like bl in blue."},
            {"term": "Consonant", "definition": "The letters that are not vowels: b, c, d, f, and more."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Blends: bl, cl, gr, st, tr.",
             {"text": "Blue, clock, grapes, star, tree. Two consonants blend together to make one sound!"},
             "You can build blends!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each word with a blend!",
             {"examples": ["blue", "clock", "grapes", "star", "tree", "green", "stop", "train"]},
             "You found the blends!", "Look at the first two letters."),
            ("SORT_AND_CLASSIFY", "Sort: Which words start with 'st'?",
             {"fits": ["star", "stop", "stone", "stamp"],
              "doesNotFit": ["blue", "clock", "grapes", "tree"]},
             "Sorted! Those start with st.", "What are the first two letters?"),
            ("MULTIPLE_CHOICE", "Which word starts with the 'gr' blend?",
             mc_content(["grapes", "star", "blue", "tree"], 0),
             "Yes! G-r = grapes.", "Say the word — which begins with gr?"),
            ("MATCHING_PAIRS", "Match each blend to a word!",
             {"pairs": [{"left": "bl", "right": "blue"},
                        {"left": "cl", "right": "clock"},
                        {"left": "gr", "right": "grapes"},
                        {"left": "tr", "right": "tree"}]},
             "You built all the blends!", "What word goes with each blend?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we read a blend word?",
             {"steps": ["Look at the first two letters", "Say the blend sound", "Add the rest of the word", "Read the whole word!"]},
             "You read blend words!", "What do we look at first?"),
        ],
        assessment=[
            dict(q="Which word starts with bl?", choices=["blue", "star", "tree", "clock"], correct=0),
            dict(q="Which word starts with st?", choices=["star", "grapes", "blue", "clock"], correct=0),
            dict(q="What is a blend?", choices=["Two consonants together", "One vowel", "A number", "A shape"], correct=0),
            dict(q="Which word starts with gr?", choices=["grapes", "stop", "train", "stone"], correct=0),
            dict(q="What did Milo do with the letters?", choices=["Blended them", "Ate them", "Hid them", "Burned them"], correct=0),
        ],
    ),
    dict(
        title="Digraph Detectives",
        objective="Read words with digraphs: ch, sh, th, wh, ph.",
        intro="Digraphs are two letters that make ONE new sound: sh as in 'ship'!",
        story="Milo whispers: \"Sss... H... SHIP! Two letters, one sound. We are digraph detectives!\"",
        scene={"character": "Milo the Cat", "setting": "Detective Agency"},
        vocabulary=[
            {"term": "Digraph", "definition": "Two letters that make one new sound, like sh in ship."},
            {"term": "Detective work", "definition": "Looking carefully at letters to find digraphs."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Digraphs: ch, sh, th, wh, ph.",
             {"text": "Ship (sh), chip (ch), thumb (th), whale (wh), phone (ph). Two letters make ONE new sound!"},
             "You solved the digraph case!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each word with a digraph!",
             {"examples": ["ship", "chip", "thumb", "whale", "phone", "shell", "chair", "three"]},
             "You found all the digraphs!", "Look for the two-letter pairs."),
            ("SORT_AND_CLASSIFY", "Sort: Which words have 'sh'?",
             {"fits": ["ship", "shell", "shoe", "shark"],
              "doesNotFit": ["chip", "thumb", "whale", "phone"]},
             "Sorted! Those all have sh.", "Which two letters make the sound?"),
            ("MULTIPLE_CHOICE", "Which word has the 'ch' digraph?",
             mc_content(["chair", "ship", "phone", "thumb"], 0),
             "Yes! Ch-air = chair.", "Which word starts with ch?"),
            ("MATCHING_PAIRS", "Match each digraph to a word!",
             {"pairs": [{"left": "sh", "right": "ship"},
                        {"left": "ch", "right": "chair"},
                        {"left": "th", "right": "thumb"},
                        {"left": "ph", "right": "phone"}]},
             "Case solved!", "What word matches each digraph?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we solve a digraph word?",
             {"steps": ["Look at the first two letters", "Say their special sound", "Add the rest of the word", "Read the whole word!"]},
             "You're a digraph detective!", "What do we check first?"),
        ],
        assessment=[
            dict(q="Which word has sh?", choices=["ship", "cat", "sun", "dog"], correct=0),
            dict(q="Which word has ch?", choices=["chair", "fish", "moon", "star"], correct=0),
            dict(q="What is a digraph?", choices=["Two letters, one sound", "One letter, two sounds", "Three vowels", "A number"], correct=0),
            dict(q="Which word has th?", choices=["thumb", "ship", "whale", "phone"], correct=0),
            dict(q="What did Milo whisper?", choices=["sh as in ship", "ab as in cab", "xy as in box", "zz as in buzz"], correct=0),
        ],
    ),
    dict(
        title="Sight-Word Path",
        objective="Read common sight words instantly.",
        intro="Sight words are words you see everywhere. Read them fast — no sounding out needed!",
        story="Milo lays a path of word stones: \"Step on each word and say it fast! Because, friend, people, always!\"",
        scene={"character": "Milo the Cat", "setting": "Garden Path"},
        vocabulary=[
            {"term": "Sight word", "definition": "A common word you read instantly, like because or friend."},
            {"term": "Instantly", "definition": "Right away, without stopping to think."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Sight words are read instantly.",
             {"text": "Words like because, friend, should, people, around, little, always, another — you read them the moment you see them!"},
             "You know sight words!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each sight word and read it fast!",
             {"examples": ["because", "friend", "should", "people", "around", "little", "always", "another"]},
             "You read them all!", "Read each word as fast as you can."),
            ("SORT_AND_CLASSIFY", "Sort: Which are sight words?",
             {"fits": ["because", "friend", "should", "always"],
              "doesNotFit": ["xylophone", "giraffe", "avocado", "pterodactyl"]},
             "Sorted! Those are common words.", "Sight words appear in almost every story."),
            ("MULTIPLE_CHOICE", "Which is a sight word?",
             mc_content(["friend", "xylophone", "giraffe", "avocado"], 0),
             "Yes! Friend is a common word.", "Which word do you see in many stories?"),
            ("MATCHING_PAIRS", "Match each sight word to a sentence!",
             {"pairs": [{"left": "because", "right": "I smiled ___ I was happy"},
                        {"left": "friend", "right": "My best ___ is named Ana"},
                        {"left": "people", "right": "Many ___ came to the fiesta"},
                        {"left": "always", "right": "I ___ help my Lola"}]},
             "You used them in sentences!", "Where does each word fit?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we learn a sight word?",
             {"steps": ["See the word", "Say the word out loud", "Use it in a sentence", "Read it in a story"]},
             "You know the path to sight words!", "What do we do first?"),
        ],
        assessment=[
            dict(q="What is a sight word?", choices=["A word you read instantly", "A very long word", "A word in another language", "A made-up word"], correct=0),
            dict(q="Which is a sight word?", choices=["because", "xylophone", "giraffe", "avocado"], correct=0),
            dict(q="How should we read sight words?", choices=["Instantly", "Very slowly", "Backwards", "Only at night"], correct=0),
            dict(q="Which sight word means a person you like?", choices=["friend", "should", "little", "another"], correct=0),
            dict(q="What did Milo lay on the path?", choices=["Word stones", "Cookies", "Leaves", "Rocks only"], correct=0),
        ],
    ),
]

# ---------------------------------------------------------------- W4: words & comprehension
W4 = [
    dict(
        title="Whose Is It?",
        objective="Use possessive words: my, your, his, her, its, our, their.",
        intro="Possessive words show who owns something: my ball, your book, her cat!",
        story="Milo holds up items: \"This is MY ball. That is HER kite. Those are THEIR shoes. Whose is it?\"",
        scene={"character": "Milo the Cat", "setting": "Lost and Found"},
        vocabulary=[
            {"term": "Possessive", "definition": "A word that shows who owns something: my, your, his, her, its, our, their."},
            {"term": "Owner", "definition": "The person who has something."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Possessive words show ownership.",
             {"text": "My ball (I own it), her kite (she owns it), their shoes (they own them). Possessives tell whose it is!"},
             "You understand ownership words!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each possessive phrase you can say!",
             {"examples": ["my ball", "your book", "her kite", "his cap", "its tail", "our house", "their shoes", "my Lola"]},
             "You found the owners!", "Who owns each thing?"),
            ("SORT_AND_CLASSIFY", "Sort: Which words are possessives?",
             {"fits": ["my", "your", "her", "their"],
              "doesNotFit": ["run", "jump", "sing", "eat"]},
             "Sorted! Those show ownership.", "Possessives answer 'whose?'."),
            ("MULTIPLE_CHOICE", "Which is correct?",
             mc_content(["This is HER kite", "This is she kite", "This is hers kite", "This is her kite is"], 0),
             "Yes! Her shows the owner.", "Which word shows a girl owns the kite?"),
            ("MATCHING_PAIRS", "Match each owner to the possessive!",
             {"pairs": [{"left": "I own it", "right": "my"},
                        {"left": "She owns it", "right": "her"},
                        {"left": "They own it", "right": "their"},
                        {"left": "We own it", "right": "our"}]},
             "You matched all owners!", "Who owns each thing?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we find the owner?",
             {"steps": ["Look at the thing", "Ask: whose is it?", "Find the possessive word", "Say who owns it"]},
             "You solved the mystery!", "What do we ask first?"),
        ],
        assessment=[
            dict(q="Which word shows a girl owns something?", choices=["her", "his", "its", "their"], correct=0),
            dict(q="Which is correct?", choices=["my ball", "I ball", "me ball", "mine ball is"], correct=0),
            dict(q="What do possessives show?", choices=["Who owns something", "What time it is", "How old you are", "Where to go"], correct=0),
            dict(q="Which shows many people own it?", choices=["their", "his", "its", "her"], correct=0),
            dict(q="Where is Milo's lesson set?", choices=["Lost and Found", "The beach", "A boat", "The moon"], correct=0),
        ],
    ),
    dict(
        title="Cause-and-Effect Garden",
        objective="Find causes and effects in a story.",
        intro="A cause makes something happen. The effect is what happens!",
        story="Milo waters a seed. \"Because I watered it, a flower grew! The watering is the CAUSE. The flower is the EFFECT.\"",
        scene={"character": "Milo the Cat", "setting": "Garden"},
        vocabulary=[
            {"term": "Cause", "definition": "Why something happens, like watering a seed."},
            {"term": "Effect", "definition": "What happens, like a flower growing."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Cause → Effect.",
             {"text": "Because I watered the seed (cause), a flower grew (effect). Ask WHY to find the cause. Ask WHAT HAPPENED to find the effect!"},
             "You understand cause and effect!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each effect (what happened) in the garden!",
             {"examples": ["a flower grew", "the plants were happy", "butterflies came", "fruit appeared",
                           "the garden smelled sweet", "birds sang", "shade appeared", "seeds sprouted"]},
             "You found the effects!", "What happened in the garden?"),
            ("SORT_AND_CLASSIFY", "Sort: Which are CAUSES (why things happen)?",
             {"fits": ["it rained", "I watered the seed", "the sun shone", "I planted the seed"],
              "doesNotFit": ["a flower grew", "butterflies came", "fruit appeared", "the plants were happy"]},
             "Sorted! Those make things happen.", "Causes answer WHY."),
            ("MULTIPLE_CHOICE", "Because it rained, ___. What is the effect?",
             mc_content(["the plants grew", "the plants shrank", "the sun disappeared", "the moon fell"], 0),
             "Yes! Rain makes plants grow.", "What happens when it rains?"),
            ("MATCHING_PAIRS", "Match each cause to its effect!",
             {"pairs": [{"left": "I watered the seed", "right": "a flower grew"},
                        {"left": "It rained", "right": "the plants grew"},
                        {"left": "The sun shone", "right": "it was warm"},
                        {"left": "Bees came", "right": "flowers made fruit"}]},
             "You matched cause and effect!", "What happened because of each cause?"),
            ("SEQUENCE_BUILDER", "Put the garden story in order!",
             {"steps": ["I planted a seed", "I watered it", "The sun shone", "A flower grew"]},
             "The garden story is complete!", "What happened first?"),
        ],
        assessment=[
            dict(q="What is a cause?", choices=["Why something happens", "What you eat", "A color", "A shape"], correct=0),
            dict(q="Because it rained, ___", choices=["the plants grew", "the plants flew", "the plants sang", "the plants slept"], correct=0),
            dict(q="What is the effect of watering a seed?", choices=["A flower grows", "A rock grows", "A shoe grows", "Nothing"], correct=0),
            dict(q="What question finds the cause?", choices=["Why?", "Who?", "When?", "Where?"], correct=0),
            dict(q="What grew in Milo's garden?", choices=["A flower", "A house", "A boat", "A mountain"], correct=0),
        ],
    ),
    dict(
        title="Detail Tracker",
        objective="Find the main idea and details in a short text.",
        intro="The main idea is what a text is mostly about. Details are the little facts that support it!",
        story="Milo reads: \"Mangoes are sweet. They grow on trees. They turn yellow when ripe.\" \"The main idea? Mangoes are sweet fruit!\"",
        scene={"character": "Milo the Cat", "setting": "Reading Nook"},
        vocabulary=[
            {"term": "Main idea", "definition": "What a text is mostly about."},
            {"term": "Detail", "definition": "A small fact that tells more about the main idea."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Main idea = what it's mostly about. Details = small facts.",
             {"text": "Mangoes are sweet fruit (main idea). They grow on trees (detail). They turn yellow when ripe (detail)."},
             "You can track ideas and details!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each DETAIL about mangoes!",
             {"examples": ["they grow on trees", "they turn yellow when ripe", "they are sweet", "they have a big seed",
                           "children love them", "they come from the farm", "they are juicy", "they are soft when ripe"]},
             "You tracked all the details!", "What else do you know about mangoes?"),
            ("SORT_AND_CLASSIFY", "Sort: Which are DETAILS about mangoes?",
             {"fits": ["they grow on trees", "they turn yellow when ripe", "they have a big seed", "they are juicy"],
              "doesNotFit": ["they bark loudly", "they swim in the sea", "they fly at night", "they are made of wood"]},
             "Sorted! Those are true mango facts.", "Details tell more about the main idea."),
            ("MULTIPLE_CHOICE", "What is the MAIN IDEA?",
             mc_content(["Mangoes are sweet fruit", "Dogs are furry", "The sky is blue", "Shoes have laces"], 0),
             "Yes! That's what the text is mostly about.", "What is the text mostly about?"),
            ("MATCHING_PAIRS", "Match each text to its main idea!",
             {"pairs": [{"left": "Mangoes grow on trees and are sweet", "right": "Mangoes are tasty fruit"},
                        {"left": "Cats purr and chase mice", "right": "Cats are interesting pets"},
                        {"left": "The sun gives light and warmth", "right": "The sun is important"},
                        {"left": "Rice is cooked and eaten daily", "right": "Rice is a staple food"}]},
             "You matched ideas!", "What is each text mostly about?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we find the main idea?",
             {"steps": ["Read the text", "Ask: what is it about?", "Look for repeated words", "Say the main idea"]},
             "You found main ideas!", "What do we do first?"),
        ],
        assessment=[
            dict(q="What is the main idea?", choices=["What the text is mostly about", "The last word", "A color", "The page number"], correct=0),
            dict(q="Which is a detail about mangoes?", choices=["They grow on trees", "They bark", "They swim", "They fly"], correct=0),
            dict(q="What did Milo read about?", choices=["Mangoes", "Dogs", "Boats", "Clouds"], correct=0),
            dict(q="What do details do?", choices=["Tell more about the main idea", "Hide the story", "Make noise", "End the book"], correct=0),
            dict(q="Where did Milo read?", choices=["In the Reading Nook", "In the sea", "On a roof", "In a cave"], correct=0),
        ],
    ),
    dict(
        title="Retell It in Order",
        objective="Retell a story in the correct order.",
        intro="Retelling means telling a story again — in the right order: first, next, then, last!",
        story="Milo reads a story about a lost kite: \"First, the kite flew away. Next, Ana ran after it. Then, it landed in a tree. Last, a kind man got it down!\"",
        scene={"character": "Milo the Cat", "setting": "Story Park"},
        vocabulary=[
            {"term": "Retell", "definition": "To tell a story again in your own words."},
            {"term": "Order", "definition": "The way things happen: first, next, then, last."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Retell in order: first, next, then, last.",
             {"text": "First the kite flew away. Next Ana ran after it. Then it landed in a tree. Last, a kind man got it down."},
             "You can retell stories!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each story event in order!",
             {"examples": ["the kite flew away", "Ana ran after it", "the kite landed in a tree", "a kind man got it down",
                           "Ana hugged her kite", "everyone cheered", "the wind blew", "Ana smiled"]},
             "You found the events!", "What happened in the story?"),
            ("SORT_AND_CLASSIFY", "Sort: Which words start a retelling?",
             {"fits": ["First", "In the beginning", "Once upon a time"],
              "doesNotFit": ["The very end", "Last of all", "Finally"]},
             "Sorted! Those start a story.", "Order words: first, next, then, last."),
            ("MULTIPLE_CHOICE", "What happened FIRST in the story?",
             mc_content(["The kite flew away", "A man got it down", "Ana hugged her kite", "Everyone cheered"], 0),
             "Yes! The kite flew away first.", "What started the story?"),
            ("MATCHING_PAIRS", "Match each order word to what it means!",
             {"pairs": [{"left": "first", "right": "at the start"},
                        {"left": "next", "right": "right after"},
                        {"left": "then", "right": "after that"},
                        {"left": "last", "right": "at the end"}]},
             "You know the order words!", "What does each word mean?"),
            ("SEQUENCE_BUILDER", "Put the kite story in order!",
             {"steps": ["The kite flew away", "Ana ran after it", "It landed in a tree", "A kind man got it down"]},
             "You retold the story perfectly!", "What happened first?"),
        ],
        assessment=[
            dict(q="What does retell mean?", choices=["Tell the story again", "Forget the story", "Draw the story", "Burn the book"], correct=0),
            dict(q="What happened first in the story?", choices=["The kite flew away", "A man got it down", "Ana hugged it", "Everyone cheered"], correct=0),
            dict(q="Which word means 'at the start'?", choices=["First", "Last", "Finally", "The end"], correct=0),
            dict(q="Where did the kite land?", choices=["In a tree", "In the sea", "On a roof", "In a river"], correct=0),
            dict(q="Who helped get the kite down?", choices=["A kind man", "A robot", "A bird", "A cloud"], correct=0),
        ],
    ),
    dict(
        title="Read the Picture Graph",
        objective="Read information from a picture graph.",
        intro="A picture graph shows information using pictures. Count the pictures to find the answer!",
        story="Milo draws a graph: \"This row has three mango pictures — that means three mangoes! Let's read it together.\"",
        scene={"character": "Milo the Cat", "setting": "Market Day"},
        vocabulary=[
            {"term": "Picture graph", "definition": "A chart that uses pictures to show how many."},
            {"term": "Row", "definition": "A line of pictures that go side to side."},
        ],
        activities=[
            ("ANIMATED_EXPLANATION", "Watch and listen! Picture graphs use pictures to show numbers.",
             {"text": "In a picture graph, each picture stands for one thing. Three mango pictures = three mangoes. Count the pictures in each row!"},
             "You understand picture graphs!", "Watch the explanation again."),
            ("HOTSPOT_IMAGE", "Tap each thing you can count in the market graph!",
             {"examples": ["3 mangoes", "5 bananas", "2 eggs", "4 fish",
                           "6 rice cakes", "1 watermelon", "7 guavas", "2 coconuts"]},
             "You counted them all!", "Count the pictures in each row."),
            ("SORT_AND_CLASSIFY", "Sort: Which things have MORE than 3 in the graph?",
             {"fits": ["5 bananas", "4 fish", "6 rice cakes", "7 guavas"],
              "doesNotFit": ["3 mangoes", "2 eggs", "1 watermelon", "2 coconuts"]},
             "Sorted! Those rows have more pictures.", "Count each row to compare."),
            ("MULTIPLE_CHOICE", "The graph shows 4 fish pictures. How many fish?",
             mc_content(["4", "2", "10", "0"], 0),
             "Yes! Each picture stands for one fish.", "Count the fish pictures."),
            ("MATCHING_PAIRS", "Match each picture count to its number!",
             {"pairs": [{"left": "🍋🍋", "right": "2"},
                        {"left": "🍌🍌🍌", "right": "3"},
                        {"left": "🍎🍎🍎🍎", "right": "4"},
                        {"left": "🍉🍉🍉🍉🍉", "right": "5"}]},
             "You read the graph perfectly!", "How many pictures are in each row?"),
            ("SEQUENCE_BUILDER", "Put the steps in order: How do we read a picture graph?",
             {"steps": ["Look at the row", "Count the pictures", "Say the number", "Compare the rows"]},
             "You can read picture graphs!", "What do we do first?"),
        ],
        assessment=[
            dict(q="What does a picture graph use to show numbers?", choices=["Pictures", "Letters", "Songs", "Shapes only"], correct=0),
            dict(q="The graph shows 3 mango pictures. How many mangoes?", choices=["3", "1", "8", "None"], correct=0),
            dict(q="Which row has the MOST?", choices=["7 guavas", "1 watermelon", "2 eggs", "3 mangoes"], correct=0),
            dict(q="What do you do to read a row?", choices=["Count the pictures", "Color the pictures", "Fold the paper", "Close the book"], correct=0),
            dict(q="Where did Milo draw his graph?", choices=["At the market", "In the sea", "On the moon", "In a cave"], correct=0),
        ],
    ),
]

WEEKS = {1: W1, 2: W2, 3: W3, 4: W4}
# Re-sequence weeks 2-4 so each week starts at day 1 (module 15-18)
# Week 2 = legacy days 06-10, but we renumber as w02-d01..d05 for Q1.
# Mapping: which legacy day each Q1 day mirrors (for skill alignment):
#   W1: d01-d05 -> legacy d01-d05
#   W2: d06-d10 -> legacy d06-d10
#   W3: d11-d15 -> legacy d11-d15
#   W4: d16-d20 -> legacy d16-d20
LEGACY_DAY = {
    (1, 1): "d01", (1, 2): "d02", (1, 3): "d03", (1, 4): "d04", (1, 5): "d05",
    (2, 1): "d06", (2, 2): "d07", (2, 3): "d08", (2, 4): "d09", (2, 5): "d10",
    (3, 1): "d11", (3, 2): "d12", (3, 3): "d13", (3, 4): "d14", (3, 5): "d15",
    (4, 1): "d16", (4, 2): "d17", (4, 3): "d18", (4, 4): "d19", (4, 5): "d20",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    written = 0
    for week in (1, 2, 3, 4):
        module_dir = ENGLISH_DIR / f"module-{14 + week:02d}"
        if not args.dry_run:
            module_dir.mkdir(parents=True, exist_ok=True)
        for day_idx, spec in enumerate(WEEKS[week], start=1):
            lesson_id = f"english-g3-q1-w{week:02d}-d{day_idx:02d}"
            acts = []
            for seq, (atype, instruction, content, correct, retry) in enumerate(spec["activities"], start=1):
                if atype == "ANIMATED_EXPLANATION":
                    prompt, narration = "Pay attention!", "Milo says: Let's learn!"
                    alt = content["text"]
                    content = content["text"]  # SLM format: plain string
                elif atype == "HOTSPOT_IMAGE":
                    prompt = "Tap each one!"
                    narration = "Find them all!"
                    alt = "Tap each item in the picture."
                    content = {"examples": content["examples"]}
                elif atype == "SORT_AND_CLASSIFY":
                    prompt = "Drag each item to its group."
                    narration = "Let's organize!"
                    alt = "Sort the items into groups."
                elif atype == "MULTIPLE_CHOICE":
                    prompt = "Tap the best answer."
                    narration = "Choose carefully!"
                    alt = "Choose the correct answer."
                elif atype == "MATCHING_PAIRS":
                    prompt = "Tap the matching pair."
                    narration = "Match them up!"
                    alt = "Match the pairs."
                    content = {"pairs": content["pairs"]}
                elif atype == "SEQUENCE_BUILDER":
                    prompt = "Put the steps in order."
                    narration = "Order the steps!"
                    alt = "Order the steps from first to last."
                    content = {"steps": content["steps"]}
                else:
                    prompt, narration, alt = "", "", ""
                acts.append(act(
                    lesson_id, seq, atype, instruction, content, correct, retry,
                    prompt, narration,
                    guide_hint="Think carefully!" if seq > 1 else "",
                    alt=alt,
                ))

            lesson = build_lesson(
                lesson_id=lesson_id,
                week=week,
                day=day_idx,
                title=spec["title"],
                objective=spec["objective"],
                intro=spec["intro"],
                story=spec["story"],
                scene=spec["scene"],
                vocabulary=spec["vocabulary"],
                activities=acts,
                assessment_qs=spec["assessment"],
            )
            # mark which legacy day it mirrors (metadata for QA traceability)
            lesson["contentVersion"] = 1
            out = module_dir / f"lesson-{day_idx:02d}.json"
            if not args.dry_run:
                out.write_text(json.dumps(lesson, indent=1, ensure_ascii=False) + "\n")
            written += 1
            print(f"{lesson_id} | {spec['title']}")

    print(f"\nAuthored: {written} lessons" + (" (dry-run)" if args.dry_run else ""))
    print("Next: run tools/convert_slm_to_pack.py to make them playable.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

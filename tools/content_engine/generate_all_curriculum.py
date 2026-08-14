#!/usr/bin/env python3
"""
Maxine's World - Full Curriculum Pack Generator
Authors, illustrates, synthesizes Piper audio, validates, and packages Grade 3 core modules.
"""

import sys
import logging
from pathlib import Path
from typing import List, Dict

from tools.content_engine.audio_synthesizer import AudioSynthesizer
from tools.content_engine.svg_generator import SvgAssetGenerator
from tools.content_engine.lesson_author import LessonAuthor, VocabularyItem, ActivityStep, AssessmentItem
from tools.content_engine.packager_validator import ContentPackager

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("CurriculumGenerator")

CURRICULUM_MODULES = [
    # 1. SCIENCE: Living Things
    {
        "package_id": "ph-matatag-g3-science-q1-w01",
        "title": "Grade 3 Science: Living and Non-Living Things",
        "subject": "SCIENCE",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "science-g3-q1-w01-d01-living-things",
        "objective": "Identify the fundamental traits that distinguish living organisms from non-living objects.",
        "competency_code": "S3LT-IIa-b-1",
        "intro": "Welcome Batang Matalino! Today, Milo the Cat will help us discover living and non-living things in our surroundings.",
        "story_intro": "Milo is taking a morning stroll in Lola's garden. Let's see what is alive and what is not!",
        "vocab": [
            VocabularyItem("Organism", "Any living biological entity such as a plant or animal."),
            VocabularyItem("Nutrient", "A substance essential for growth and life."),
            VocabularyItem("Habitat", "The natural home of a plant or animal.")
        ],
        "activities": [
            ActivityStep(
                activityId="sci-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Listen to Milo explain what makes things alive.",
                prompt="Living things grow, breathe, and reproduce.",
                narration="Look at the garden! Trees grow from tiny seeds, birds sing in the branches, and kittens play. All living things need food and water to grow.",
                guideHint="Notice how plants and animals change and grow over time!",
                content={"explanationType": "DEMONSTRATION", "body": "Living organisms need food, air, and water."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="sci-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="sci-g3-q1-w01-d01-a02",
                type="SORT_AND_CLASSIFY_V1",
                instruction="Sort the items into Living and Non-Living.",
                prompt="Drag each item to the correct basket.",
                narration="Can you help me sort these items? Put living things in the green basket and non-living objects in the blue basket.",
                guideHint="Ask yourself: Does it breathe or grow?",
                content={
                    "categoryA": "Living Things",
                    "categoryB": "Non-Living Things",
                    "items": [
                        {"name": "Sunflower", "category": "A"},
                        {"name": "Wooden Chair", "category": "B"},
                        {"name": "Puppy", "category": "A"},
                        {"name": "Bicycle", "category": "B"}
                    ]
                },
                completionRule="ALL_ITEMS_SORTED",
                assetId="sci-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "Which of the following is a living thing?", ["A growing mango tree", "A plastic toy car", "A ceramic coffee mug", "A metal spoon"], 0, {"correct": "Great job! Mango trees grow and need sunlight.", "retry": "Think about what needs water and sunlight to grow."}),
            AssessmentItem("q2", "What do animals need to survive?", ["Air, water, and food", "Batteries and wires", "Paint and brushes", "Gasoline and motor oil"], 0, {"correct": "Spot on! Living things need nourishment and air.", "retry": "Living things need biological nourishment."}),
            AssessmentItem("q3", "Why is a stone considered a non-living thing?", ["It does not grow, eat, or breathe", "It is found outside in the soil", "It can roll down a steep hill", "It is very hard and heavy"], 0, {"correct": "Exactly! Non-living objects do not perform life processes.", "retry": "Remember the key traits of living organisms."}),
            AssessmentItem("q4", "Which organism makes its own food using sunlight?", ["A green fern plant", "A hungry honeybee", "A playful puppy dog", "A chirping sparrow"], 0, {"correct": "Excellent! Green plants produce food through photosynthesis.", "retry": "Which organism has green leaves that absorb sunlight?"}),
            AssessmentItem("q5", "What will happen to a potted plant if it gets no water or sunlight?", ["It will stop growing and wither", "It will turn into a rock", "It will run away to find a pond", "It will stay green forever"], 0, {"correct": "Correct! Living plants need water and sunlight to survive.", "retry": "Living organisms require essential nutrients to live."})
        ],
        "visuals": [
            {"icon": "🌻", "label": "Sunflower", "desc": "Grows & Needs Sun"},
            {"icon": "🐶", "label": "Playful Puppy", "desc": "Breathes & Eats"},
            {"icon": "🪨", "label": "River Stone", "desc": "Non-Living Object"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    },

    # 2. MATHEMATICS: Place Value & 3-Digit Numbers
    {
        "package_id": "ph-matatag-g3-math-q1-w01",
        "title": "Grade 3 Math: Place Value to 10,000",
        "subject": "MATHEMATICS",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "mathematics-g3-q1-w01-d01-place-value",
        "objective": "Identify the place value and value of a digit in numbers up to 10,000.",
        "competency_code": "M3NS-Ia-9.3",
        "intro": "Hello Math Wizard! Milo the Cat has a fun treasure hunt with hundreds, tens, and ones!",
        "story_intro": "Milo is counting shells on the beach. Let's group them into tens, hundreds, and thousands!",
        "vocab": [
            VocabularyItem("Place Value", "The numerical value that a digit has by virtue of its position in a number."),
            VocabularyItem("Digit", "Any of the numerals from 0 to 9, especially when forming part of a number."),
            VocabularyItem("Thousands", "The place value representing groups of 1,000.")
        ],
        "activities": [
            ActivityStep(
                activityId="math-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Watch how digits change value based on their position.",
                prompt="The digit 5 in 542 means 5 hundreds, or 500.",
                narration="In the number 542, the digit 2 is in the ones place, 4 is in the tens place, and 5 is in the hundreds place. That means 500 plus 40 plus 2!",
                guideHint="Focus on the place value order: Ones, Tens, Hundreds, Thousands!",
                content={"explanationType": "DEMONSTRATION", "body": "Digits in different places have different values."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="math-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="math-g3-q1-w01-d01-a02",
                type="MATCHING_PAIRS_V1",
                instruction="Match each number with its expanded form.",
                prompt="Tap matching pairs to connect them.",
                narration="Let's match each number to its expanded value! 345 is 300 plus 40 plus 5.",
                guideHint="Break the number down by its place values.",
                content={
                    "pairs": [
                        {"left": "345", "right": "300 + 40 + 5"},
                        {"left": "620", "right": "600 + 20 + 0"},
                        {"left": "807", "right": "800 + 0 + 7"}
                    ]
                },
                completionRule="ALL_PAIRS_MATCHED",
                assetId="math-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "What is the place value of 7 in the number 4,728?", ["Hundreds", "Thousands", "Tens", "Ones"], 0, {"correct": "Excellent! 7 represents 7 hundreds.", "retry": "Count from right: Ones, Tens, Hundreds..."}),
            AssessmentItem("q2", "What is the value of the digit 9 in 9,150?", ["9,000", "900", "90", "9"], 0, {"correct": "Spot on! In thousands place, 9 is 9,000.", "retry": "The 9 is in the thousands place."}),
            AssessmentItem("q3", "Which number has 6 hundreds, 3 tens, and 4 ones?", ["634", "643", "436", "364"], 0, {"correct": "Correct! 600 + 30 + 4 = 634.", "retry": "Combine 600, 30, and 4."}),
            AssessmentItem("q4", "How is 2,408 written in expanded form?", ["2,000 + 400 + 8", "2,000 + 40 + 8", "200 + 40 + 8", "2,000 + 400 + 80"], 0, {"correct": "Great job! Zero tens means we add 8 ones.", "retry": "Notice there are 0 tens in 2,408."}),
            AssessmentItem("q5", "In the number 8,852, which digit has the greatest value?", ["The first 8 (thousands)", "The second 8 (hundreds)", "The 5 (tens)", "The 2 (ones)"], 0, {"correct": "Awesome! The thousands place holds the highest value.", "retry": "Compare 8,000 vs 800 vs 50 vs 2."})
        ],
        "visuals": [
            {"icon": "🧱", "label": "Thousands Block", "desc": "1,000 units"},
            {"icon": "🟦", "label": "Hundreds Flat", "desc": "100 units"},
            {"icon": "🟩", "label": "Tens Rod & Ones", "desc": "10s and 1s"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    },

    # 3. ENGLISH: Story Elements & Character Traits
    {
        "package_id": "ph-matatag-g3-english-q1-w01",
        "title": "Grade 3 English: Story Characters and Setting",
        "subject": "ENGLISH",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "english-g3-q1-w01-d01-characters-setting",
        "objective": "Identify characters, setting, and plot elements in literary texts.",
        "competency_code": "EN3RC-Ia-b-2",
        "intro": "Welcome young reader! Milo the Story Cat is ready to explore wonderful story worlds with you!",
        "story_intro": "Milo opened an old storybook. Let's find out who the characters are and where the adventure happens!",
        "vocab": [
            VocabularyItem("Character", "A person, animal, or entity in a story."),
            VocabularyItem("Setting", "The time and location where a story takes place."),
            VocabularyItem("Plot", "The sequence of events that make up a story.")
        ],
        "activities": [
            ActivityStep(
                activityId="eng-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Learn about characters and setting.",
                prompt="Characters are WHO the story is about. Setting is WHERE it happens.",
                narration="Every great story needs characters and a setting! Characters can be people or animals, and the setting tells us where and when the story takes place.",
                guideHint="Look for clues about who is in the story and where they are.",
                content={"explanationType": "DEMONSTRATION", "body": "Characters are the people or animals. The setting is the place and time."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="eng-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="eng-g3-q1-w01-d01-a02",
                type="SORT_AND_CLASSIFY_V1",
                instruction="Classify each card as a Character or a Setting.",
                prompt="Drag each card to Character or Setting.",
                narration="Let's organize our story elements! Put people and animals under Characters, and places under Setting.",
                guideHint="Ask: Is it a person/animal, or is it a place?",
                content={
                    "categoryA": "Characters",
                    "categoryB": "Settings",
                    "items": [
                        {"name": "Brave Princess", "category": "A"},
                        {"name": "Enchanted Forest", "category": "B"},
                        {"name": "Clever Monkey", "category": "A"},
                        {"name": "Sunny Beach", "category": "B"}
                    ]
                },
                completionRule="ALL_ITEMS_SORTED",
                assetId="eng-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "Who is a character in a story?", ["A person or talking animal", "The wooden front door", "A rainy afternoon", "The castle garden"], 0, {"correct": "Great job! Characters are the individuals in the story.", "retry": "A character is someone who acts or speaks in the tale."}),
            AssessmentItem("q2", "Which of the following describes a story setting?", ["A noisy school playground at recess", "A friendly brown puppy", "The wise old grandmother", "A mischievous squirrel"], 0, {"correct": "Spot on! The playground at recess tells where and when.", "retry": "Look for the location and time of the event."}),
            AssessmentItem("q3", "In the story 'The Tortoise and the Hare', who are the main characters?", ["The Tortoise and the Hare", "The finish line ribbon", "The green meadow grass", "The bright morning sun"], 0, {"correct": "Exactly! The tortoise and the hare are the animals in the race.", "retry": "Who are the participants competing in the race?"}),
            AssessmentItem("q4", "Where does a story take place if the author writes 'Waves crashed against the sandy shore'?", ["At the beach", "Inside a library", "On top of a snowy mountain", "In outer space"], 0, {"correct": "Awesome reading! Waves and sand point to the beach setting.", "retry": "Where do you find crashing waves and sand?"}),
            AssessmentItem("q5", "Why is knowing the setting important when reading?", ["It helps us picture where the action takes place", "It tells us how many pages the book has", "It names the author of the book", "It gives the price of the storybook"], 0, {"correct": "Well done! The setting helps you visualize the scene.", "retry": "How does the place and time help your imagination?"})
        ],
        "visuals": [
            {"icon": "👸", "label": "Hero / Character", "desc": "Who does the action"},
            {"icon": "🏰", "label": "Castle Setting", "desc": "Where story happens"},
            {"icon": "📖", "label": "Story Plot", "desc": "What happens next"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    },

    # 4. FILIPINO: Pangngalan (Pantangi at Pambalana)
    {
        "package_id": "ph-matatag-g3-filipino-q1-w01",
        "title": "Baitang 3 Filipino: Pangngalan (Pantangi at Pambalana)",
        "subject": "FILIPINO",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "filipino-g3-q1-w01-d01-pangngalan",
        "objective": "Nagagamit ang pangngalan sa pagsasalaysay tungkol sa mga tao, lugar, at bagay sa paligid.",
        "competency_code": "F3WG-Ia-d-2",
        "intro": "Magandang araw, Batang Matalino! 🌸 Ngayon pag-aaralan natin ang Pangngalang Pantangi at Pambalana kasama si Milo.",
        "story_intro": "Namasyal si Milo sa Barangay San Jose. Marami siyang nakitang tao, hayop, at magagandang lugar!",
        "vocab": [
            VocabularyItem("Pangngalan", "Salitang tumutukoy sa ngalan ng tao, bagay, hayop, lugar, o pangyayari."),
            VocabularyItem("Pantangi", "Tiyak na ngalan na nagsisimula sa malaking titik tulad ng Pilipinas o Maxine."),
            VocabularyItem("Pambalana", "Karaniwang ngalan na nagsisimula sa maliit na titik tulad ng bata o pusa.")
        ],
        "activities": [
            ActivityStep(
                activityId="fil-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Pakinggan si Milo tungkol sa uri ng Pangngalan.",
                prompt="Ang Pantangi ay tiyak (Maxine). Ang Pambalana ay karaniwan (bata).",
                narration="Ang Pangngalang Pantangi ay tiyak na ngalan ng tao, hayop, o lugar at laging nagsisimula sa malaking titik. Ang Pambalana naman ay karaniwang ngalan.",
                guideHint="Tandaan: Ang tiyak na ngalan ay nagsisimula sa malaking titik!",
                content={"explanationType": "DEMONSTRATION", "body": "Pantangi: Tiyak (Maynila). Pambalana: Karaniwan (lungsod)."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="fil-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="fil-g3-q1-w01-d01-a02",
                type="SORT_AND_CLASSIFY_V1",
                instruction="Ibukod ang mga salita: Pantangi o Pambalana.",
                prompt="I-drag ang bawat salita sa tamang kahon.",
                narration="Tulungan si Milo na ihiwalay ang Pangngalang Pantangi sa Pangngalang Pambalana!",
                guideHint="Tingnan kung nagsisimula sa malaking titik ang salita.",
                content={
                    "categoryA": "Pantangi (Tiyak)",
                    "categoryB": "Pambalana (Karaniwan)",
                    "items": [
                        {"name": "Pilipinas", "category": "A"},
                        {"name": "aklat", "category": "B"},
                        {"name": "Guro Santos", "category": "A"},
                        {"name": "aso", "category": "B"}
                    ]
                },
                completionRule="ALL_ITEMS_SORTED",
                assetId="fil-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "Alin sa mga sumusunod ang halimbawa ng Pangngalang Pantangi?", ["Lungsod ng Maynila", "magandang bulaklak", "matalinong mag-aaral", "malaking paaralan"], 0, {"correct": "Mahusay! Ang Lungsod ng Maynila ay tiyak na ngalan ng lugar.", "retry": "Hanapin ang tiyak na ngalan na nagsisimula sa malaking titik."}),
            AssessmentItem("q2", "Alin ang karaniwang ngalan o Pangngalang Pambalana?", ["lapis", "Mongol", "G. Cruz", "Pilipinas"], 0, {"correct": "Tumpak! Ang lapis ay karaniwang ngalan ng bagay.", "retry": "Ang pambalana ay karaniwang tawag sa bagay."}),
            AssessmentItem("q3", "Bakit nagsisimula sa malaking titik ang 'Milo'?", ["Dahil ito ay tiyak na ngalan ng pusa (Pantangi)", "Dahil ito ay salitang kilos", "Dahil ito ay pambalana", "Dahil ito ay pangungusap"], 0, {"correct": "Napakagaling! Ang tiyak na ngalan ay nagsisimula sa malaking titik.", "retry": "Si Milo ay tiyak na pangalan ng alagang pusa."}),
            AssessmentItem("q4", "Alin sa mga pangungusap ang may wastong gamit ng pangngalan?", ["Pumunta si Ana sa parke.", "pumunta si ana sa Parke.", "Pumunta si ana Sa parke.", "pumunta Si Ana sa parke."], 0, {"correct": "Magaling! Ang Ana ay may malaking titik at ang parke ay maliit.", "retry": "Tiyaking malaking titik ang tiyak na ngalan ng tao."}),
            AssessmentItem("q5", "Ano ang tawag sa bahagi ng pananalita na tumutukoy sa ngalan ng tao, hayop, bagay, o lugar?", ["Pangngalan", "Pandiwa", "Pang-uri", "Pang-abay"], 0, {"correct": "Tumpak! Pangngalan ang tawag sa ngalan ng tao, bagay, at lugar.", "retry": "Ito ang salitang pantawag sa lahat ng bagay sa paligid."})
        ],
        "visuals": [
            {"icon": "🇵🇭", "label": "Pilipinas (Pantangi)", "desc": "Tiyak na Bansa"},
            {"icon": "🐱", "label": "Milo (Pantangi)", "desc": "Tiyak na Alaga"},
            {"icon": "✏️", "label": "lapis (Pambalana)", "desc": "Karaniwang Bagay"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    },

    # 5. MAKABANSA: Aking Komunidad
    {
        "package_id": "ph-matatag-g3-makabansa-q1-w01",
        "title": "Baitang 3 Makabansa: Ang Aking Komunidad",
        "subject": "MAKABANSA",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "makabansa-g3-q1-w01-d01-komunidad",
        "objective": "Nailalarawan ang sariling komunidad gamit ang mga payak na simbolo at mapa.",
        "competency_code": "AP3KLR-Ia-1",
        "intro": "Halina at tuklasin ang ating pamayanan kasama si Milo ang Matalinong Pusa!",
        "story_intro": "Sumakay si Milo sa bisikleta upang libutin ang barangay, palengke, at paaralan.",
        "vocab": [
            VocabularyItem("Komunidad", "Isang pamayanan kung saan sama-samang naninirahan ang mga tao."),
            VocabularyItem("Simbolo", "Mga sagisag o tanda na nagpapakita ng mahahalagang lugar sa mapa."),
            VocabularyItem("Tungkulin", "Mga responsibilidad at gampanin ng bawat mamamayan para sa kaayusan.")
        ],
        "activities": [
            ActivityStep(
                activityId="mak-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Pag-aralan ang mahahalagang bahagi ng komunidad.",
                prompt="Ang komunidad ay binubuo ng pamilya, paaralan, simbahan, at sentrong pangkalusugan.",
                narration="Ang bawat komunidad ay may mga pampublikong gusali tulad ng paaralan para sa edukasyon, health center para sa kalusugan, at barangay hall para sa kapayapaan.",
                guideHint="Tukuyin ang mga lugar kung saan tumutulong ang mga lider ng pamayanan.",
                content={"explanationType": "DEMONSTRATION", "body": "Ang komunidad ay binubuo ng paaralan, health center, at tahanan."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="mak-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="mak-g3-q1-w01-d01-a02",
                type="MATCHING_PAIRS_V1",
                instruction="Itugma ang gusali sa serbisyong ibinibigay nito.",
                prompt="I-tap ang magkapares na institusyon at serbisyo.",
                narration="Itugma natin ang bawat lugar sa komunidad sa tulong na ibinibigay nito sa mamamayan.",
                guideHint="Saan nag-aaral ang mga bata? Saan nagpapagamot ang may sakit?",
                content={
                    "pairs": [
                        {"left": "Paaralan", "right": "Edukasyon at Pagkatuto"},
                        {"left": "Health Center", "right": "Gamot at Bakuna"},
                        {"left": "Barangay Hall", "right": "Kapayapaan at Kaayusan"}
                    ]
                },
                completionRule="ALL_PAIRS_MATCHED",
                assetId="mak-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "Ano ang tawag sa lugar kung saan sama-samang naninirahan at nagtutulungan ang mga pamilya?", ["Komunidad", "Sasakyan", "Kagubatan", "Pabrika"], 0, {"correct": "Mahusay! Komunidad ang tawag sa ating pamayanan.", "retry": "Dito nagtitipon at namumuhay ang mga mamamayan."}),
            AssessmentItem("q2", "Saan pumupunta ang mga mamamayan kung kailangan ng libreng bakuna o konsultasyon sa doktor?", ["Health Center", "Palaruan", "Sinehan", "Palengke"], 0, {"correct": "Tumpak! Ang Health Center ang nagbibigay ng serbisyong pangkalusugan.", "retry": "Aling institusyon ang namamahala sa kalusugan?"}),
            AssessmentItem("q3", "Alin sa mga sumusunod ang nagpapakita ng pagtutulungan sa komunidad?", ["Bayanihan sa paglilinis ng kanal", "Pagtatapon ng basura sa kalsada", "Paninira ng halaman sa parke", "Pag-aaway ng magkakapitbahay"], 0, {"correct": "Napakagaling! Ang bayanihan ay nagpapakita ng pagtutulungan.", "retry": "Piliin ang kilos na nakatutulong sa kapwa."}),
            AssessmentItem("q4", "Sino ang namumuno sa kaayusan at kapayapaan sa loob ng barangay?", ["Kapitan ng Barangay", "Drayber ng Jeep", "Tindero sa Tindahan", "Mag-aaral"], 0, {"correct": "Tama! Ang Kapitan at mga kagawad ang namumuno sa barangay.", "retry": "Sino ang inihalal na pinuno ng pamayanan?"}),
            AssessmentItem("q5", "Bakit mahalaga ang paaralan sa isang pamayanan?", ["Dito natututong magbasa, sumulat, at magbilang ang mga bata", "Dito nagtitinda ng mga gulay at prutas", "Dito ipinaparada ang mga bus", "Dito nagpapalipas ng gabi ang mga turista"], 0, {"correct": "Eksakto! Ang paaralan ang sentro ng edukasyon ng kabataan.", "retry": "Ano ang pangunahing layunin ng paaralan?"})
        ],
        "visuals": [
            {"icon": "🏫", "label": "Paaralan", "desc": "Dunong at Pagkatuto"},
            {"icon": "🏥", "label": "Health Center", "desc": "Kalusugan ng Bayan"},
            {"icon": "🏛️", "label": "Barangay Hall", "desc": "Kaayusan at Serbisyo"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    },

    # 6. GMRC: Pagmamahal sa Pamilya at Magagalang na Pananalita
    {
        "package_id": "ph-matatag-g3-gmrc-q1-w01",
        "title": "Baitang 3 GMRC: Paggalang at Pagmamahal sa Pamilya",
        "subject": "GMRC",
        "grade": 3,
        "quarter": 1,
        "week": 1,
        "day": 1,
        "lesson_id": "gmrc-g3-q1-w01-d01-paggalang",
        "objective": "Naipakikita ang paggalang sa mga magulang at nakatatanda sa pamamagitan ng magagalang na pananalita at kilos.",
        "competency_code": "ESP3PKP-Ia-13",
        "intro": "Magandang araw! Sama-sama nating tuklasin ang kagandahang-asal at paggalang sa pamilya kasama si Milo.",
        "story_intro": "Bumisita si Milo kay Lolo at Lola. Nagmano siya at gumamit ng 'po' at 'opo'.",
        "vocab": [
            VocabularyItem("Paggalang", "Pagpapakita ng respeto at pagpapahalaga sa kapwa tao."),
            VocabularyItem("Pagmamano", "Tradisyon ng paglalapat ng noo sa kamay ng nakatatanda bilang paghingi ng basbas."),
            VocabularyItem("Kagandahang-Asal", "Mabubuting gawi at ugali na nagpapakita ng kabutihan ng puso.")
        ],
        "activities": [
            ActivityStep(
                activityId="gmr-g3-q1-w01-d01-a01",
                type="ANIMATED_EXPLANATION_V1",
                instruction="Matutong gumamit ng 'po' at 'opo'.",
                prompt="Ang paggamit ng 'po' at 'opo' at pagmamano ay tanda ng paggalang.",
                narration="Kapag kinakausap natin ang ating mga magulang, lolo, lola, o guro, gumagamit tayo ng 'po' at 'opo'. Ang pagmamano sa pagdating o pag-alis ay pagpapakita ng paggalang at pagmamahal.",
                guideHint="Laging gumamit ng magagalang na salita sa nakatatanda.",
                content={"explanationType": "DEMONSTRATION", "body": "Magmano sa nakatatanda at gumamit ng 'po' at 'opo'."},
                completionRule="VIEW_AND_ACKNOWLEDGE",
                assetId="gmr-g3-q1-w01-d01-visual.svg"
            ),
            ActivityStep(
                activityId="gmr-g3-q1-w01-d01-a02",
                type="SORT_AND_CLASSIFY_V1",
                instruction="Ibukod ang Magagalang na Kilos at Hindi Magagalang na Kilos.",
                prompt="I-drag ang bawat kilos sa tamang kahon.",
                narration="Tulungan si Milo na pumili ng mabubuting gawi na nagpapasaya sa pamilya!",
                guideHint="Aling mga kilos ang nagpapakita ng paggalang?",
                content={
                    "categoryA": "Magagalang na Kilos",
                    "categoryB": "Hindi Magagalang",
                    "items": [
                        {"name": "Pagmamano kay Lola", "category": "A"},
                        {"name": "Pagsagot nang pasigaw", "category": "B"},
                        {"name": "Pagsasabi ng 'Salamat po'", "category": "A"},
                        {"name": "Pang-aagaw ng laruan", "category": "B"}
                    ]
                },
                completionRule="ALL_ITEMS_SORTED",
                assetId="gmr-g3-q1-w01-d01-visual.svg"
            )
        ],
        "assessments": [
            AssessmentItem("q1", "Ano ang dapat sabihin kapag binigyan ka ng regalo o tulong ng iyong guro o magulang?", ["Maraming salamat po!", "Akin na yan!", "Bakit ito lang?", "Wala akong pakialam."], 0, {"correct": "Napakabait! Ang pagsasabi ng 'Salamat po' ay tanda ng pasasalamat.", "retry": "Ano ang magalang na tugon kapag tumanggap ng biyaya o tulong?"}),
            AssessmentItem("q2", "Paano ipinapakita ang paggalang sa lolo at lola kapag dumating sa kanilang tahanan?", ["Magalang na magmano at bumati", "Diretsong manood ng telebisyon nang walang bati", "Magtago sa ilalim ng kama", "Sumigaw nang malakas"], 0, {"correct": "Napakahusay! Ang pagmamano at pagbati ay kaugaliang Pilipino.", "retry": "Aling tradisyon ang humihingi ng basbas sa nakatatanda?"}),
            AssessmentItem("q3", "Alin sa mga sumusunod ang magalang na pananalita kapag may nais itanong sa guro?", ["Maaari po ba akong magtanong?", "Hoy, sagutin mo ako!", "Bilis, sabihin mo!", "Ano ba yan!"], 0, {"correct": "Tumpak! Ang 'Maaari po ba' ay napakagalang na panimula.", "retry": "Gamitin ang salitang 'po' at 'maaari po ba'."}),
            AssessmentItem("q4", "Ano ang dapat gawin kapag nag-uusap ang mga nakatatanda at kailangan mong dumaan sa pagitan nila?", ["Yumuko nang bahagya at sabihing 'Makikiraan po'", "Tumakbo nang mabilis at banggain sila", "Sumigaw para tumabi sila", "Iharang ang mga braso"], 0, {"correct": "Magaling! Ang pagyuko at pagsasabi ng 'Makikiraan po' ay paggalang.", "retry": "Paano tayo magalang na dumaraan sa pagitan ng mga tao?"}),
            AssessmentItem("q5", "Bakit mahalagang maging magalang at masunurin sa ating pamilya?", ["Upang maging masaya, mapayapa, at puno ng pagmamahalan ang tahanan", "Upang bigyan tayo ng maraming pera", "Upang matakot sa atin ang kapitbahay", "Upang makaiwas sa pag-aaral"], 0, {"correct": "Napakaganda! Ang paggalang ay nagbubunga ng kapayapaan at pagmamahal sa pamilya.", "retry": "Bakit masaya ang pamilyang may respeto sa isa't isa?"})
        ],
        "visuals": [
            {"icon": "🙏", "label": "Pagmamano", "desc": "Paggalang sa Matatanda"},
            {"icon": "💬", "label": "Po at Opo", "desc": "Magalang na Pananalita"},
            {"icon": "👨‍👩‍👧", "label": "Masayang Pamilya", "desc": "Pagmamahalan sa Bahay"}
        ],
        "hotspots": [{"x": 0.33, "y": 0.72}, {"x": 0.58, "y": 0.72}, {"x": 0.83, "y": 0.72}]
    }
]

def build_all_curriculum(output_dir: Path):
    logger.info(f"Starting Full Curriculum Generation for {len(CURRICULUM_MODULES)} modules...")
    synthesizer = AudioSynthesizer()
    svg_gen = SvgAssetGenerator()
    author = LessonAuthor()
    packager = ContentPackager(output_dir)

    for idx, mod in enumerate(CURRICULUM_MODULES, 1):
        pkg_id = mod["package_id"]
        logger.info(f"\n[{idx}/{len(CURRICULUM_MODULES)}] Processing {pkg_id} ({mod['subject']})...")
        
        build_temp = output_dir / "temp" / pkg_id
        audio_temp = build_temp / "audio"
        svg_temp = build_temp / "assets"
        audio_temp.mkdir(parents=True, exist_ok=True)
        svg_temp.mkdir(parents=True, exist_ok=True)

        # 1. Build Lesson
        lesson = author.create_lesson(
            lesson_id=mod["lesson_id"],
            title=mod["title"],
            subject=mod["subject"],
            grade=mod["grade"],
            quarter=mod["quarter"],
            week=mod["week"],
            day=mod["day"],
            objective=mod["objective"],
            competency_code=mod["competency_code"],
            introduction=mod["intro"],
            story_intro=mod["story_intro"],
            vocabulary=mod["vocab"],
            activities=mod["activities"],
            assessment_items=mod["assessments"],
            language="fil-PH" if mod["subject"] in ["FILIPINO", "MAKABANSA", "GMRC"] else "en-PH"
        )

        # 2. Validate Quality
        valid, errors = packager.validate_lesson(lesson)
        if not valid:
            logger.error(f"Validation failed for {pkg_id}: {errors}")
            sys.exit(1)

        # 3. Synthesize Audio Narration
        logger.info(f" -> Synthesizing offline audio narration prompts...")
        synthesizer.batch_synthesize_lesson(lesson, audio_temp)
        audio_files = list(audio_temp.glob("*.ogg"))
        logger.info(f" -> Synthesized {len(audio_files)} audio prompts.")

        # 4. Generate Illustrated SVG Board
        logger.info(f" -> Rendering illustrated SVG activity board with master Milo anchor...")
        board_name = f"{mod['lesson_id']}-visual.svg"
        board_path = svg_temp / board_name
        svg_gen.generate_activity_board(
            title=mod["title"],
            subject=mod["subject"],
            topic_visuals=mod["visuals"],
            output_svg_path=board_path,
            instruction=f"Milo says: {mod['intro'][:60]}...",
            hotspots=mod.get("hotspots")
        )
        svg_files = [board_path]

        # 5. Package Module ZIP
        logger.info(f" -> Packaging into verified ZIP bundle...")
        catalog_entry = packager.package_module(
            package_id=pkg_id,
            version="1.0.0",
            title=mod["title"],
            subject=mod["subject"],
            grade=mod["grade"],
            lessons=[lesson],
            asset_files=svg_files,
            audio_files=audio_files
        )
        logger.info(f" -> Packaged {catalog_entry['downloadUrl']} (SHA: {catalog_entry['sha256'][:12]}..., {catalog_entry['sizeBytes']/1024:.1f} KB)")

    logger.info("\n" + "=" * 60)
    logger.info("ALL CURRICULUM MODULES GENERATED & PACKAGED SUCCESSFULLY!")
    logger.info(f"Catalog Location: {output_dir / 'catalog.json'}")
    logger.info("=" * 60)

if __name__ == "__main__":
    out = Path("/home/ron/workspace/maxines-world/build/content_output")
    build_all_curriculum(out)

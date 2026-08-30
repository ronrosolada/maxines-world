#!/usr/bin/env python3
"""Build the authored future Grade 3–4 curriculum bank deterministically with rich, concept-specific pedagogy."""
import json
import re
from pathlib import Path
from collections import Counter

# Every row is a different per-unit key permutation. Repeating this 12-row
# cycle four times gives exactly 48 A/B/C/D keys while avoiding an ordinal cue.
KEY_PATTERNS = (
    "bcda", "cdab", "dabc", "badc", "cadb", "dbac",
    "bdac", "cbad", "dcba", "acdb", "adcb", "bacd",
)

# Terms prohibited by the strict Filipino-medium policy. Replacements are
# Filipino-first and are applied to every generated learner-facing field.
FILIPINO_REPLACEMENTS = {
    "Rice Granary of the Philippines": "Kamalig ng Palay ng Pilipinas",
    "Honesty is the best policy": "Ang katapatan ang pinakamabuting patakaran",
    "Reduce, Reuse, Recycle": "Bawasan, Gamitin Muli, at Iproseso Muli",
    "3Rs": "tatlong paraan ng pagbawas ng basura",
    "reusable": "nagagamit muli", "single-use plastic": "plastik na minsanan lamang gamitin",
    "Sustainable Development": "Likas-kayang Pag-unlad", "Renewable Energy": "Enerhiyang Napapalitan",
    "Open-Pit Mining": "Pagmiminang Bukas", "crop rotation": "salit-salit na pagtatanim",
    "open burning": "pagsusunog sa lantad na lugar", "greenhouse gas emissions": "buga ng gas na nagpapainit sa daigdig",
    "climate change": "pagbabago ng klima", "scam": "panlilinlang", "bank account": "kuwenta sa bangko",
    "Growth Mindset": "paniniwalang napauunlad ang kakayahan", "Critically Minded": "mapanuring pag-iisip",
    "I-verify bago i-share": "Suriin bago ibahagi", "finish line": "guhit ng pagtatapos",
    "Compass Rose": "pananda ng direksiyon", "Legend": "talaan ng mga pananda",
    "health center": "sentrong pangkalusugan", "social media": "midyang panlipunan",
    "online chat": "usap sa internet", "online": "sa internet", "chat": "usap",
    "netiquette": "magalang na asal sa internet", "vandalism": "paninira",
    "Intangible Cultural Heritage": "pamanang kulturang di-nahahawakan",
    "Pacific Ocean": "Karagatang Pasipiko", "West Philippine Sea": "Dagat Kanlurang Pilipinas",
    "Exclusive Economic Zone (EEZ)": "natatanging sonang pangkabuhayan",
    "grid coordinates": "mga guhit na panukat", "coordinates": "mga panukat na guhit",
    "Reforestation": "muling pagtatanim ng puno", "illegal logging": "ilegal na pagtotroso",
    "endangered": "nanganganib maubos", "coral reef": "bahura", "nursery": "palakihan",
    "Barangay Health Workers (BHW)": "mga manggagawang pangkalusugan sa barangay",
    "Barangay Health Workers": "mga manggagawang pangkalusugan sa barangay", "frontliners": "unang tumutugon",
}


def filipinize(value):
    """Recursively enforce Filipino-first terminology in generated records."""
    if isinstance(value, str):
        for english, filipino in sorted(FILIPINO_REPLACEMENTS.items(), key=lambda x: -len(x[0])):
            value = re.sub(re.escape(english), filipino, value, flags=re.IGNORECASE)
        return value
    if isinstance(value, (list, tuple)):
        converted = [filipinize(v) for v in value]
        return tuple(converted) if isinstance(value, tuple) else converted
    if isinstance(value, dict):
        return {k: filipinize(v) for k, v in value.items()}
    return value


def worked_script(language, prompt, explanation, focus):
    """Create a concrete Milo think-aloud rather than an objective summary."""
    if language == "English":
        return (f"Milo reads this example: {prompt} First, he marks the clue that tells what the question asks. "
                f"Next, he applies the rule: {explanation} Then he checks the result against the words and numbers in the example. "
                f"That check matters: {focus} Now pause and name the clue you would use on a similar problem.")
    return (f"Binasa ni Milo ang halimbawang ito: {prompt} Una, minarkahan niya ang salitang nagsasabi kung ano ang hinahanap. "
            f"Pagkatapos, ginamit niya ang tuntunin: {explanation} Inihambing niya ang sagot sa mga detalye ng halimbawa upang matiyak ito. "
            f"Mahalaga ang pagsusuring ito: {focus} Huminto at tukuyin ang pahiwatig na gagamitin mo sa kahawig na tanong.")


def balance_option_lengths(options, language):
    """Reduce answer-length cues with parallel, misconception-focused qualifiers."""
    lengths = [len(re.findall(r"\w+", text)) for text in options]
    target = max(6, min(max(lengths), 16))
    en = [" under the same conditions", " using the information given", " for the situation described"]
    fil = [" sa parehong kalagayan", " ayon sa ibinigay na detalye", " para sa sitwasyong inilalarawan"]
    tails = en if language == "English" else fil
    out = []
    for index, text in enumerate(options):
        # Extend short choices with neutral qualifiers until each reaches at
        # least 85% of the longest choice; this removes obvious length cues.
        suffix = 0
        while len(re.findall(r"\w+", text)) < target * .85:
            text = text.rstrip(".") + tails[(index + suffix) % len(tails)]
            suffix += 1
        if suffix:
            text += "."
        out.append(text)
    return out

from pathlib import Path
from collections import Counter


ROOT = Path(__file__).parent

# 48 Units (24 Grade 3, 24 Grade 4) across 6 subject domains
# Each subject contains 4 distinct units per grade level, totaling 48 units.
# Each unit has 4 distinct MCQs (192 total items), 1 concept-specific 30-40s video script,
# 1 actionable 5-8s hint, and 1 three-tier scaffolding remediation ladder.

SUBJECTS = {
    "mathematics": ("English", [
        (3, [
            ("Place Value to 10,000", "Read, write, and decompose whole numbers up to 10,000 using standard and expanded notation.", "math-g2-place-value",
             [
                 ("Which number has 6 in the thousands place and 4 in the tens place?", ["6,245", "2,654", "4,526", "5,462"], "Identify thousands (6,000) and tens (40)."),
                 ("What is the expanded form of 4,307?", ["4,000 + 300 + 7", "4,000 + 30 + 7", "400 + 300 + 7", "4,000 + 300 + 70"], "Break each digit into value: 4 thousands (4000), 3 hundreds (300), 0 tens, 7 ones (7)."),
                 ("Which comparison statement is mathematically true?", ["7,105 > 7,015", "3,908 > 3,980", "5,241 < 5,214", "6,800 < 6,080"], "Compare the hundreds place: 7,105 has 1 hundred, which is greater than 0 hundreds in 7,015."),
                 ("Which list of numbers is arranged from least to greatest?", ["2,145, 2,154, 2,451", "3,210, 3,201, 3,120", "4,090, 4,009, 4,900", "5,601, 5,610, 5,160"], "Check from left to right: 2,145 < 2,154 < 2,451.")
             ],
             "Watch the digits in each position: thousands, hundreds, tens, and ones.",
             "Find the column with the highest value first to compare.",
             ("Decompose the number into thousands, hundreds, tens, and ones.", "Use place-value blocks on the screen to physically count units.", "Review 3-digit place value from Grade 2 before trying 4-digit numbers.")
            ),
            ("2-Step Word Problems with Bar Models", "Model comparison word problems using Singapore heuristic bar units.", "math-g2-operations",
             [
                 ("Basket A has 14 mangoes. Basket B has 3 times as many as Basket A. How many mangoes are in Basket B?", ["42 mangoes", "17 mangoes", "28 mangoes", "34 mangoes"], "Multiply 1 unit (14) by 3 to find Basket B: 14 × 3 = 42."),
                 ("Milo has 25 stickers. Sara has 15 more stickers than Milo. How many stickers do they have altogether?", ["65 stickers", "40 stickers", "50 stickers", "55 stickers"], "Sara has 25 + 15 = 40. Together: 25 + 40 = 65."),
                 ("A box has 48 pencils shared equally among 6 tables. How many pencils does each table get?", ["8 pencils", "7 pencils", "9 pencils", "6 pencils"], "Divide total items by number of groups: 48 ÷ 6 = 8."),
                 ("Ken bought 3 notebooks at ₱20 each and gave ₱100. How much change did he receive?", ["₱40", "₱60", "₱30", "₱50"], "Total cost = 3 × ₱20 = ₱60. Change = ₱100 - ₱60 = ₱40.")
             ],
             "Draw bar units to represent the known and unknown values before computing.",
             "Count how many equal unit blocks make up the unknown amount.",
             ("Identify whether you need to add, subtract, multiply, or divide the unit blocks.", "Draw the bar brackets step-by-step for the first step.", "Review 1-step word problem models from Grade 2.")
            ),
            ("Understanding Unit Fractions", "Identify, compare, and model unit fractions on area models and number lines.", "math-g2-fractions",
             [
                 ("Which fraction represents one part of a pizza cut into 8 equal slices?", ["1/8", "1/4", "1/2", "8/1"], "One part out of 8 equal shares is written as 1/8."),
                 ("Which fraction is larger: 1/3 or 1/6 of the same size cake?", ["1/3", "1/6", "They are equal", "Cannot be compared"], "When dividing the same whole, fewer equal shares (3) mean each slice is larger than dividing into 6 shares."),
                 ("Where is the fraction 1/2 located on a number line from 0 to 1?", ["Exactly halfway between 0 and 1", "Closer to 0 than 1/4", "Past 1", "At 0"], "1/2 is the midpoint between 0 and 1."),
                 ("How many 1/4 unit fraction pieces are needed to make 1 whole?", ["4 pieces", "2 pieces", "3 pieces", "8 pieces"], "Four quarters make one whole: 1/4 + 1/4 + 1/4 + 1/4 = 4/4 = 1.")
             ],
             "Fractions represent equal shares of a single whole object or group.",
             "Remember: as the bottom number (denominator) gets bigger, the slice gets smaller.",
             ("Fold or shade a shape into equal parts to see unit fractions.", "Compare unit fractions side-by-side on a fraction strip.", "Review equal sharing from Grade 2.")
            ),
            ("Area as Tiling and Multiplication", "Calculate the area of rectangles by counting square units and multiplying length by width.", "math-g2-geometry",
             [
                 ("A rectangular garden is 5 meters long and 3 meters wide. What is its area?", ["15 sq meters", "16 sq meters", "8 sq meters", "18 sq meters"], "Multiply length by width: 5 × 3 = 15 square meters."),
                 ("A grid has 4 rows with 6 unit squares in each row. What is the total area?", ["24 square units", "10 square units", "20 square units", "28 square units"], "Total square units = rows × columns = 4 × 6 = 24."),
                 ("How does area differ from perimeter?", ["Area is space inside; perimeter is distance around", "Area is distance around; perimeter is space inside", "They are always the same number", "Area only applies to circles"], "Area measures the interior surface; perimeter measures the outer boundary."),
                 ("A square mat has sides of 4 feet. What is its area?", ["16 sq feet", "8 sq feet", "12 sq feet", "20 sq feet"], "For a square, multiply side by side: 4 × 4 = 16 square feet.")
             ],
             "Area measures the square units that cover a flat surface without gaps or overlaps.",
             "Multiply the length of the grid by the width of the grid.",
             ("Count individual square tiles on the grid before using multiplication.", "Trace the perimeter versus shading the area to see the difference.", "Review counting repeated rows from Grade 2 arrays.")
            )
        ]),
        (4, [
            ("Multi-Digit Multiplication Strategies", "Multiply 2-digit and 3-digit numbers using partial products and standard algorithms.", "math-g3-operations",
             [
                 ("What is 36 × 4 using partial products?", ["120 + 24 = 144", "120 + 20 = 140", "30 + 24 = 54", "100 + 44 = 144"], "Multiply tens (30 × 4 = 120) and ones (6 × 4 = 24), then add: 120 + 24 = 144."),
                 ("A school orders 15 packs of notebooks with 12 notebooks in each pack. How many notebooks total?", ["180", "150", "160", "175"], "Calculate 15 × 12 = 15 × 10 (150) + 15 × 2 (30) = 180."),
                 ("What is the product of 124 and 3?", ["372", "362", "352", "382"], "Multiply: 100 × 3 = 300, 20 × 3 = 60, 4 × 3 = 12. Sum = 372."),
                 ("Which estimate is closest to 48 × 19?", ["50 × 20 = 1,000", "40 × 10 = 400", "50 × 10 = 500", "40 × 20 = 800"], "Round each factor to the nearest ten: 48 rounds to 50, 19 rounds to 20. 50 × 20 = 1,000.")
             ],
             "Break complex factors into tens and ones before multiplying.",
             "Use the distributive property: multiply tens first, then ones.",
             ("Use an area model box to split numbers before multiplying.", "Practice multiplying single digits by multiples of 10.", "Review Grade 3 multiplication tables.")
            ),
            ("Long Division with Remainders", "Divide 2-digit and 3-digit dividends by 1-digit divisors and interpret remainders.", "math-g3-operations",
             [
                 ("What is 75 ÷ 4?", ["18 remainder 3", "18 remainder 2", "19 remainder 1", "17 remainder 3"], "4 × 18 = 72. 75 - 72 = 3 remainder."),
                 ("53 cookies are packed into bags of 5. How many full bags can be made?", ["10 bags", "11 bags", "9 bags", "8 bags"], "53 ÷ 5 = 10 full bags with 3 leftover cookies."),
                 ("What is the first step in solving 148 ÷ 4?", ["Divide 14 tens by 4", "Divide 1 hundred by 4", "Multiply 148 by 4", "Subtract 4 from 148"], "Since 4 cannot divide into 1 hundred, divide into the first two digits (14 tens)."),
                 ("If 84 children are split equally into 4 teams, how many are on each team?", ["21 children", "22 children", "20 children", "24 children"], "Divide: 80 ÷ 4 = 20, 4 ÷ 4 = 1. 20 + 1 = 21.")
             ],
             "Divide, multiply, subtract, and bring down the next place value.",
             "Make sure the remainder is always smaller than your divisor.",
             ("Use counters or group drawings to model equal sharing with leftovers.", "Check division answers using multiplication plus remainder.", "Review Grade 3 basic division facts.")
            ),
            ("Equivalent Fractions and Common Denominators", "Generate equivalent fractions and add fractions with like and related denominators.", "math-g3-fractions",
             [
                 ("Which fraction is equivalent to 2/3?", ["4/6", "3/4", "2/6", "4/9"], "Multiply numerator and denominator by 2: (2×2)/(3×2) = 4/6."),
                 ("What is 2/8 + 3/8?", ["5/8", "5/16", "6/8", "1/8"], "When denominators are the same, add the numerators: 2 + 3 = 5, keeping denominator 8."),
                 ("To add 1/2 and 1/4, how should 1/2 be rewritten?", ["2/4", "1/4", "2/2", "4/8"], "Convert 1/2 to fourths: (1×2)/(2×2) = 2/4."),
                 ("Which fraction is in simplest form?", ["3/5", "4/8", "6/9", "2/10"], "3 and 5 share no common factors other than 1.")
             ],
             "Multiply or divide the top and bottom by the exact same number to find equivalent fractions.",
             "You can only add or subtract fractions once their bottom numbers (denominators) match.",
             ("Use fraction visual bars to show that 2/4 covers the exact same area as 1/2.", "Practice finding common multiples of denominators.", "Review Grade 3 unit fractions.")
            ),
            ("Perimeter and Area of Composite Shapes", "Calculate area and perimeter for L-shaped and composite rectangular figures.", "math-g3-geometry",
             [
                 ("An L-shaped room is split into two rectangles: 4m × 3m and 2m × 2m. What is the total area?", ["16 sq meters", "12 sq meters", "20 sq meters", "14 sq meters"], "Add the areas of the two parts: (4 × 3) + (2 × 2) = 12 + 4 = 16 sq meters."),
                 ("A rectangular patio has sides 6m, 4m, 6m, 4m. What is its perimeter?", ["20 meters", "24 meters", "10 meters", "16 meters"], "Add all four sides: 6 + 4 + 6 + 4 = 20 meters."),
                 ("What is the first step in finding the area of an irregular polygon?", ["Split the shape into non-overlapping rectangles", "Multiply all outer dimensions", "Add all side lengths", "Measure the diagonal"], "Decompose composite figures into simpler rectangles first."),
                 ("If a composite shape has an area of 30 sq cm and one part is 18 sq cm, what is the missing part?", ["12 sq cm", "14 sq cm", "10 sq cm", "48 sq cm"], "Subtract the known part: 30 - 18 = 12 sq cm.")
             ],
             "Decompose complex shapes into smaller rectangles, find each area, and add them together.",
             "Perimeter is the fence around the outside; area is the tiles inside.",
             ("Color-code the separate rectangular pieces before calculating.", "Label all missing side lengths on the diagram.", "Review Grade 3 simple rectangular area.")
            )
        ])
    ]),
    "science": ("English", [
        (3, [
            ("Characteristics and Basic Needs of Living Things", "Classify living vs non-living things and identify essential habitat requirements.", "sci-g2-living-things",
             [
                 ("Which habitat provides a mudskipper with both water and muddy land?", ["Mangrove swamp", "Desert sand dune", "Deep ocean trench", "Dry mountain peak"], "Mangroves provide tidal mudflats and brackish water where mudskippers thrive."),
                 ("What do green plants need to produce their own food during photosynthesis?", ["Sunlight, water, and carbon dioxide", "Sugar and milk", "Darkness and dry soil", "Plastic and stones"], "Plants use sunlight energy, water from soil, and carbon dioxide from air."),
                 ("Which organ allows a tilapia to extract dissolved oxygen from water?", ["Gills", "Lungs", "Skin pores only", "Fins"], "Gills absorb dissolved oxygen as water flows over them."),
                 ("Why do Philippine eagles build large nests high in forest dipterocarp trees?", ["To protect eggs and eaglets from ground predators", "To catch fish easily", "To hide from sunlight", "To store rainwater"], "High canopy nests provide shelter, stability, and safety from predators.")
             ],
             "Watch how plants and animals interact with their native biomes.",
             "Think about what animals eat, breathe, and need for shelter in their native habitat.",
             ("Sort pictures into living things that need food/air versus non-living objects.", "Trace the food and water path for a local plant.", "Review Grade 2 plant and animal characteristics.")
            ),
            ("States of Matter: Solids, Liquids, and Gases", "Distinguish physical properties of matter and observe temperature phase changes.", "sci-g2-matter-materials",
             [
                 ("What happens to an ice cube left in a warm room at 30°C?", ["It absorbs heat and melts into liquid water.", "It turns into solid iron.", "It freezes into a larger ice block.", "It loses all mass."], "Thermal energy causes solid ice to melt into liquid water."),
                 ("Which state of matter takes both the shape and volume of its container?", ["Gas (water vapor)", "Solid (wood block)", "Liquid (coconut oil)", "Crystal (sugar)"], "Gases expand to fill the entire container volume and take its shape."),
                 ("Why do water droplets form on the outside of a cold glass of calamansi juice?", ["Water vapor in the warm air cools and condenses upon touching the glass.", "Liquid juice leaks through solid glass.", "The glass produces ice from inside.", "Dust turns into liquid."], "Warm air vapor condenses into liquid water droplets when it hits the cold glass surface."),
                 ("Which material maintains its own definite shape regardless of the container?", ["Wooden building block", "Soy sauce", "Cooking oil", "Water vapor"], "Solids have a rigid structure with a definite shape and volume.")
             ],
             "Solids keep their shape, liquids flow to fit their container, and gases expand everywhere.",
             "Heat makes particles move faster and spread apart (melting and evaporating).",
             ("Feel and observe real examples: ice (solid), water (liquid), and steam (gas).", "Use a temperature slider to watch ice melt and evaporate.", "Review Grade 2 physical textures.")
            ),
            ("Light, Heat, and Sound in Everyday Life", "Explore sources of energy, how light and sound travel, and daily safety practices.", "sci-g2-energy",
             [
                 ("Why should you never look directly at the midday sun even with sunglasses?", ["Intense solar radiation can cause permanent retinal damage to your eyes.", "It turns sunlight into ice.", "It changes the color of the sky instantly.", "It makes shadows disappear permanently."], "Direct solar rays can permanently burn the retina."),
                 ("How is sound produced when a guitar string is plucked?", ["The string vibrates, creating pressure waves in the air.", "Light bounces off the wood.", "Electricity is stored in the string.", "Heat melts the air around it."], "Vibrating strings create sound waves that travel to your ears."),
                 ("Which surface reflects light best to form a clear mirror image?", ["A smooth, polished shiny metal surface", "A rough wooden plank", "A black cloth blanket", "Dry garden soil"], "Smooth, reflective surfaces bounce light rays back predictably."),
                 ("Which action protects your hearing when near loud construction equipment?", ["Wearing protective ear muffs or earplugs", "Closing your eyes tightly", "Standing closer to the machine", "Shouting loudly"], "Ear protection dampens high-decibel sound waves.")
             ],
             "Vibrations create sound, light travels in straight lines until reflected, and heat moves from warm to cool.",
             "Light allows us to see; loud sounds and intense light require safety protection.",
             ("Pluck a rubber band to see vibrations create sound.", "Use a flashlight and mirror to observe light bouncing.", "Review Grade 2 five senses and safe habits.")
            ),
            ("Earth, Weather, and Storm Safety", "Identify local landforms, observe weather patterns, and practice typhoon preparedness.", "sci-g2-Earth-weather",
             [
                 ("Which Philippine landform is a flat, elevated landmass between mountains?", ["Plateau (e.g. Bukidnon Plateau)", "Volcano (e.g. Mayon)", "Valley (e.g. Cagayan Valley)", "Trench"], "A plateau is a high, elevated flat area surrounded by lower slopes."),
                 ("What items should be packed first in a family Typhoon Emergency Go-Bag?", ["Drinking water, canned food, flashlight, first-aid kit, and whistle", "Heavy textbooks and toys", "Metal cookware and chairs", "Garden soil and plants"], "Survival essentials: clean water, non-perishable food, lighting, and medical supplies."),
                 ("What does a dark, towering cumulonimbus cloud usually indicate?", ["An approaching thunderstorm with heavy rain and strong winds", "Clear sunny skies for 3 days", "Snowfall", "Dry drought conditions"], "Cumulonimbus clouds bring intense localized downpours and lightning."),
                 ("Why are mangrove forests along coastlines important during storms?", ["They absorb wave energy and reduce storm surge flooding.", "They make the wind blow faster.", "They dry out the ocean water.", "They stop earthquakes from occurring."], "Dense mangrove root systems act as natural breakwaters against storm surges.")
             ],
             "Weather changes daily; tracking clouds and rain helps us prepare for typhoons.",
             "Pack Go-Bags with essentials (water, flashlight, food) before storm signals rise.",
             ("Identify storm warning signals and explain what actions to take.", "Match landforms (mountain, valley, plateau) to local photos.", "Review Grade 2 sunny, rainy, and windy weather.")
            )
        ]),
        (4, [
            ("Internal and External Plant & Animal Structures", "Explain how internal organs and specialized structures support survival and growth.", "sci-g3-living-things",
             [
                 ("How do the broad, floating leaves of water lilies help them survive in freshwater ponds?", ["They maximize sunlight absorption and have air pockets that keep them buoyant.", "They absorb soil nutrients from deep underground.", "They prevent any water from touching the plant.", "They store dry sand inside."], "Air-filled spongy tissue keeps leaves afloat to capture sunlight."),
                 ("What is the primary function of the human skeletal and muscular systems working together?", ["To provide structural support, protect organs, and enable locomotion", "To pump oxygenated blood through veins only", "To digest solid food into nutrients", "To filter liquid waste from blood"], "Bones provide framework; muscles pull bones to produce movement."),
                 ("Why do Philippine tarsiers have exceptionally large eyes?", ["To gather maximum light while hunting insects at night (nocturnal adaptation)", "To swim underwater in rivers", "To absorb sunlight during the day", "To scare birds away from trees"], "Large retinas gather scarce moonlight for nocturnal vision."),
                 ("Which plant structure transports water and dissolved minerals from roots up to leaves?", ["Xylem vessels in the stem", "Pollen grains in flowers", "Bark on the outside", "Seeds inside fruit"], "Xylem tubes transport water upwards through capillary action.")
             ],
             "Specialized body parts and plant structures are adaptations that help organisms survive in their biomes.",
             "Look at how an animal's features (eyes, claws, gills) match its daily survival needs.",
             ("Dissect a simple flower or celery stalk in colored water to see transport vessels.", "Compare nocturnal animal eyes with diurnal animals.", "Review Grade 3 basic animal needs.")
            ),
            ("Chemical and Physical Changes in Materials", "Differentiate between reversible physical changes and irreversible chemical reactions.", "sci-g3-matter-materials",
             [
                 ("Which process is a chemical change that creates a completely new substance?", ["Rusting of an iron nail exposed to air and moisture", "Melting an ice cube in a cup", "Tearing a piece of paper into small strips", "Dissolving salt into warm water"], "Rust (iron oxide) is a chemically new compound formed by iron reacting with oxygen."),
                 ("Why is melting butter considered a physical change?", ["The chemical composition remains butter; it changes only from solid to liquid.", "It creates wood ash.", "It cannot ever be hardened again.", "It produces a toxic gas."], "Physical changes alter state or shape without forming new chemical bonds."),
                 ("What evidence indicates that burning wood in a campfire is an irreversible chemical change?", ["It produces ash, smoke, heat, and carbon dioxide that cannot be turned back into wood.", "The wood changes temperature slightly.", "The wood can be glued back together.", "The wood only changes color."], "Combustion breaks molecular bonds, producing ash and gases."),
                 ("When sugar dissolves in hot water, how can the original solid sugar be recovered?", ["By slowly evaporating or boiling off the water", "By cooling it with ice", "By filtering through paper", "It is gone forever and cannot be recovered"], "Evaporation removes the liquid solvent, leaving crystalline sugar solute behind.")
             ],
             "Physical changes alter form or state without creating new molecules; chemical changes create new substances.",
             "Ask: Can this easily go back to what it was, or did it turn into something new (like ash or rust)?",
             ("Compare tearing paper (physical) versus burning paper (chemical).", "Mix baking soda and vinegar to observe gas formation.", "Review Grade 3 states of matter.")
            ),
            ("Energy Transfer: Motion, Collisions, and Circuits", "Investigate how energy transfers through mechanical collisions, electric circuits, and heat.", "sci-g3-energy",
             [
                 ("When a rolling billiard ball strikes a stationary ball, what happens to the energy?", ["Kinetic energy transfers to the stationary ball, causing it to move.", "All energy is completely destroyed upon contact.", "The energy turns into cold ice.", "The stationary ball stays still forever."], "Energy is conserved and transferred from the moving object to the stationary one."),
                 ("What is required for electric current to flow and light up a bulb in a simple circuit?", ["A closed, continuous conductive loop connecting the power source to the bulb", "An open gap in the wire", "A plastic string connected to wood", "A dead battery without chemicals"], "Electrons need an unbroken conductive path from positive to negative terminals."),
                 ("Which material is an electrical conductor that safely carries current in home appliances?", ["Copper metal wire", "Rubber insulation", "Dry wooden stick", "Glass rod"], "Copper has free electrons that permit rapid electrical conduction."),
                 ("How does thermal heat transfer when a metal spoon is placed in hot soup?", ["Heat conducts from the hot soup through the metal handle to your fingers.", "Cold travels from the spoon into the soup.", "Light turns into sound waves.", "The soup loses all energy immediately."], "Conduction transfers heat from higher temperature to lower temperature regions.")
             ],
             "Energy cannot be created or destroyed; it transfers between objects through motion, heat, and electricity.",
             "A circuit must be completely closed with conductors for electric current to flow.",
             ("Build a simple battery, wire, and LED circuit on the simulator board.", "Roll marbles together to see collision momentum transfer.", "Review Grade 3 light, heat, and sound.")
            ),
            ("Earth Processes: Weathering, Erosion, and Ecosystems", "Analyze how water and wind shape landscapes, and examine food webs in Philippine biomes.", "sci-g3-Earth-weather",
             [
                 ("How does flowing river water gradually carve a canyon or valley over many years?", ["Water continuously weathers and erodes rock particles, carrying them downstream.", "Earthquakes push the rock down instantly in one day.", "Wind blows all water out of the river.", "Tree roots turn solid rock into water."], "Continuous hydraulic action and abrasion wear down rock strata over time."),
                 ("In a coral reef ecosystem food chain, what role do microscopic phytoplankton play?", ["Primary producers that convert sunlight into food energy", "Apex predators that hunt sharks", "Decomposers that only eat bones", "Herbivores that eat sea turtles"], "Phytoplankton form the base of marine food webs through photosynthesis."),
                 ("How do contour farming and terracing on mountain slopes prevent soil erosion?", ["They slow down rainwater runoff, allowing water to soak in rather than wash soil away.", "They make the mountain steeper.", "They remove all plant roots from the soil.", "They block all sunlight from reaching the ground."], "Step-like terraces reduce surface water velocity and hold fertile topsoil."),
                 ("What happens in a forest food web if the primary predator population disappears?", ["Herbivore populations may overgraze vegetation, leading to habitat degradation.", "Plants will grow without needing any water.", "All animals instantly multiply forever.", "The sun will stop providing energy."], "Predators maintain ecological balance by regulating herbivore numbers.")
             ],
             "Weathering breaks rocks down; erosion carries sediments away. Ecosystems rely on balanced food webs.",
             "Look at the sequence of food energy: Sun → Producer (plant) → Herbivore → Predator.",
             ("Trace a Philippine coral reef food chain from sun to reef fish to predator.", "Simulate rain on a dirt slope with and without grass cover to observe erosion.", "Review Grade 3 Philippine landforms.")
            )
        ])
    ]),
    "english": ("English", [
        (3, [
            ("Identifying Main Idea and Supporting Details", "Distinguish between the central thesis of a passage and supporting evidentiary details.", "eng-g2-reading-comprehension",
             [
                 ("Mina cleans her brushes, sorts her paints, and sketches her canvas carefully before painting. What is the main idea?", ["Mina prepares her art materials carefully.", "Canvas is made of cotton.", "Paint brushes are expensive.", "Water is blue."], "The details all show the steps Mina takes to prepare her art workspace."),
                 ("Which detail best supports the idea that 'The barangay library helps students learn'?", ["It provides free reference books, quiet study tables, and internet access.", "Its front door is painted green.", "There is a mango tree outside.", "The building was built on a Tuesday."], "Reference books and quiet study areas directly provide educational support."),
                 ("What is the most effective question to ask when searching for a paragraph's main idea?", ["What central point is the author making about the topic?", "How many adjectives appear in the paragraph?", "What is the very last word on the page?", "Who illustrated the cover art?"], "The main idea is the primary point or argument the author wants readers to understand."),
                 ("Which sentence does NOT support the main idea 'Honeybees are vital for agriculture'?", ["Bees transfer pollen that enables fruit trees to produce crops.", "Many commercial vegetables depend on bee pollination.", "Some bicycles have silver metal spokes.", "Without bees, farmer harvest yields drop significantly."], "Bicycle spokes have no connection to bee pollination or agriculture.")
             ],
             "The main idea is what the whole text is mostly about; supporting details prove or explain that idea.",
             "Ask: What big umbrella idea connects all the sentences together?",
             ("Circle the sentence that summarizes the whole story.", "List 2 facts that prove the summary sentence is true.", "Review Grade 2 basic reading recall.")
            ),
            ("Context Clues and Vocabulary Acquisition", "Determine the meaning of unfamiliar words using semantic and syntactic context clues.", "eng-g2-vocabulary",
             [
                 ("The path was treacherous; steep, slippery rocks made every step dangerous. What does 'treacherous' mean?", ["Hazardous and unsafe", "Smooth and easy", "Bright and sunny", "Short and wide"], "'Steep, slippery rocks' and 'dangerous' signal that the path is hazardous."),
                 ("Unlike her boisterous brother who shouted and ran, Lea was tranquil. What does 'tranquil' mean?", ["Calm and peaceful", "Loud and angry", "Fast and energetic", "Hungry"], "The contrast clue 'unlike her boisterous brother' indicates Lea is peaceful and quiet."),
                 ("The farmer used an arid plot where no rain had fallen for months. What does 'arid' mean?", ["Extremely dry", "Very muddy", "Full of rivers", "Frozen with ice"], "'No rain had fallen for months' defines arid as extremely dry."),
                 ("Which type of context clue is used in: 'Mammals, such as bats, dolphins, and horses, are warm-blooded'?", ["Example clue", "Antonym contrast clue", "Cause and effect clue", "Definition prefix clue"], "'Such as' introduces specific examples of the category.")
             ],
             "Look at the words around the unfamiliar word for clues, definitions, examples, or opposite meanings.",
             "Read the sentence before and after to figure out what mystery word makes sense.",
             ("Replace the unfamiliar word with a simple synonym and see if the sentence still makes sense.", "Identify clue words like 'such as', 'unlike', or 'because'.", "Review Grade 2 sight words.")
            ),
            ("Story Elements and Plot Sequencing", "Analyze narrative characters, settings, conflict, climax, and resolution.", "eng-g2-reading-comprehension",
             [
                 ("In a story about a lost puppy finding its way home in a storm, what is the central conflict?", ["The puppy is separated from its family and must navigate a dangerous storm.", "The puppy has brown fur.", "The rain makes puddles.", "The grass is wet."], "The problem that drives the plot is being lost in hazardous weather."),
                 ("Which element describes where and when a story takes place?", ["Setting", "Theme", "Climax", "Dialogue"], "Setting establishes both geographical location and historical/temporal time."),
                 ("What is the resolution of a narrative?", ["How the main problem is solved at the end", "The very first sentence introducing a character", "The most exciting turning point of action", "The list of vocabulary words"], "The resolution ties up loose ends and resolves the core conflict."),
                 ("Which event should happen first in a story about baking bread?", ["Measuring the flour and mixing the yeast dough", "Slicing the baked loaf for lunch", "Taking the golden bread out of the hot oven", "Spreading butter on warm toast"], "Ingredients must be measured and mixed before baking.")
             ],
             "Stories follow a structure: Setting and characters → Problem (conflict) → Exciting turning point → Solution (resolution).",
             "Pay attention to time words like first, next, suddenly, and finally.",
             ("Draw a story mountain: Beginning → Problem → Climax → Solution.", "Retell what the main character wanted and what stood in their way.", "Review Grade 2 character and setting identification.")
            ),
            ("Making Inferences from Textual Evidence", "Combine stated text facts with background knowledge to draw logical inferences.", "eng-g2-reading-comprehension",
             [
                 ("Leo looked out the window, grabbed his raincoat and umbrella, and put on rubber boots. What can you infer?", ["It is raining outside.", "Leo is going swimming in the pool.", "It is a hot, sunny summer afternoon.", "Leo is getting ready to sleep."], "Raincoats, umbrellas, and boots are used during rainy weather."),
                 ("The dog wagged its tail rapidly and leaped up happily when Anna opened the front door. What can you infer?", ["The dog was excited to see Anna return home.", "The dog was frightened by Anna.", "The dog wanted to sleep outside.", "The dog did not recognize Anna."], "Tail wagging and jumping excitedly indicate affectionate greeting."),
                 ("What is an inference?", ["A logical conclusion based on text clues plus what you already know", "A wild prediction without reading the text", "Copying the exact words from the page", "The title of a chapter"], "Inferences combine text evidence + schema to determine unstated facts."),
                 ("Maya’s alarm rang three times, and she frantically stuffed her homework into her bag while running to the bus stop. What can you infer?", ["Maya was running late for school.", "Maya was relaxing on the weekend.", "Maya did not want to go to school.", "Maya had already missed the entire day."], "Multiple alarms and frantic rushing indicate she was late.")
             ],
             "An inference is reading between the lines: Clues from the story + What you already know = Logical conclusion.",
             "Look at what characters do and say to understand what is happening without being told directly.",
             ("Underline 2 action clues in the sentence that give away the character's feelings.", "Practice 'I see... so I think...' statements.", "Review Grade 2 picture clues.")
            )
        ]),
        (4, [
            ("Citing Evidence in Informational Texts", "Cite explicit textual evidence and draw justified conclusions from expository passages.", "eng-g3-reading-comprehension",
             [
                 ("A passage states: 'Mangrove root networks trap silt, preventing coastal soil from washing into the sea.' Which statement is best supported?", ["Mangroves actively protect shorelines from coastal erosion.", "Mangroves only grow in dry desert soil.", "All marine life avoids mangrove forests.", "Ocean waves cannot touch mangroves."], "Trapping silt and stopping soil loss directly proves shoreline erosion protection."),
                 ("Which sentence from a text about exercise provides the strongest factual evidence?", ["Studies show regular aerobic exercise increases heart muscle efficiency by 15%.", "Exercise is the most fun thing to do after school.", "Everyone loves playing basketball in the park.", "Running shoes look very athletic."], "Specific quantitative study results provide factual, verifiable evidence."),
                 ("What makes a quotation relevant evidence for a research claim?", ["It directly addresses and supports the specific claim being argued.", "It is the longest sentence in the article.", "It contains poetic rhyming words.", "It was written in bold font."], "Relevance requires a direct logical link between evidence and claim."),
                 ("Which statement is an author's opinion rather than verifiable evidence?", ["Electric vehicles are the most beautiful cars ever built.", "Electric vehicles produce zero tailpipe emissions.", "The battery requires lithium and cobalt minerals.", "Charging stations are installed along public highways."], "Calling something 'most beautiful' is a subjective personal aesthetic judgment.")
             ],
             "Good readers back up their claims by pointing to exact facts and data in the text.",
             "Quote the author's exact words that prove your answer is correct.",
             ("Highlight the exact sentence in the passage that answers the question.", "Separate factual data from personal opinions.", "Review Grade 3 main idea and details.")
            ),
            ("Analyzing Text Structures: Cause, Effect, and Chronology", "Identify cause-and-effect, problem-solution, and chronological text organizational patterns.", "eng-g3-reading-comprehension",
             [
                 ("Because heavy monsoon rains saturated the mountain slope, a landslide blocked the national highway. What is the cause?", ["Heavy monsoon rains saturated the mountain slope.", "The national highway was blocked.", "Cars drove on the road.", "The sun came out."], "The rain saturation is the event that produced the landslide outcome."),
                 ("Which transition word signals a cause-and-effect organizational structure?", ["Consequently", "Similarly", "Furthermore", "Meanwhile"], "'Consequently' indicates that the following statement is the result of a prior action."),
                 ("A passage describes the life cycle of a monarch butterfly from egg, to caterpillar, to chrysalis, to adult. What structure is used?", ["Chronological / Sequential order", "Compare and contrast", "Problem and solution", "Spatial order"], "Tracking development across time follows a sequential timeline."),
                 ("Which text structure identifies a dilemma and proposes one or more methods to resolve it?", ["Problem and solution", "Chronological order", "Cause and effect", "Descriptive list"], "Problem-solution structures frame an obstacle and explain corrective actions.")
             ],
             "Authors organize writing using patterns: Cause & Effect (Why it happened), Sequence (Time order), and Problem & Solution.",
             "Look for signal words like 'because', 'therefore', 'first, next, last', and 'as a result'.",
             ("Create a cause-and-effect flowchart with arrows pointing from reason to result.", "Highlight chronological sequence transition words.", "Review Grade 3 story sequencing.")
            ),
            ("Evaluating Author's Purpose and Point of View", "Determine whether a text aims to inform, persuade, or entertain, and analyze author perspective.", "eng-g3-reading-comprehension",
             [
                 ("An article providing nutritional facts and daily recommended water intake is written primarily to do what?", ["Inform the reader with factual guidance", "Persuade the reader to buy a specific brand of juice", "Entertain the reader with a funny story", "Criticize cooking recipes"], "Providing objective health data demonstrates an informative purpose."),
                 ("An editorial titled 'Why Our City Must Build More Bike Lanes Now' is written with what primary intent?", ["To persuade readers and officials to support bike infrastructure", "To entertain with fictional cycling jokes", "To list the chemical elements of bicycles", "To describe a race from 1920"], "Urging specific civic policy action indicates persuasive writing."),
                 ("From what point of view is a narrative written if the narrator uses 'I', 'me', and 'my'?", ["First-person point of view", "Third-person omniscient", "Second-person instructional", "Objective journalistic"], "First-person narration tells the story from the speaker's direct perspective."),
                 ("How does an author's bias affect an informational text?", ["It may lead the author to emphasize favorable facts while omitting counter-evidence.", "It ensures the text is 100% objective.", "It makes the text impossible to print.", "It converts prose into poetry automatically."], "Bias skews presentation toward a preferred viewpoint.")
             ],
             "Authors write to Inform (teach facts), Persuade (convince you), or Entertain (tell an engaging story).",
             "Ask: Is the author trying to teach me, convince me, or tell me an entertaining story?",
             ("Sort articles into PIE categories: Persuade, Inform, Entertain.", "Identify whether the narrator is inside the story (I/we) or outside (he/she/they).", "Review Grade 3 fact vs opinion.")
            ),
            ("Figurative Language: Similes, Metaphors, and Personification", "Interpret figurative idioms, similes, metaphors, and personification in literary passages.", "eng-g3-vocabulary",
             [
                 ("'The morning sun smiled down upon the blooming sunflower garden.' Which literary device is used?", ["Personification", "Simile", "Literal description", "Hyperbole"], "Giving human actions ('smiled') to non-human elements (the sun) is personification."),
                 ("Which sentence contains a simile comparing two things using 'like' or 'as'?", ["The runner was as fast as a cheetah across the field.", "The runner is a cheetah.", "The runner wore blue shoes.", "The runner finished in first place."], "Comparing speed using 'as fast as' is the definition of a simile."),
                 ("What does the metaphor 'Knowledge is a key that unlocks countless doors' mean?", ["Learning gives you opportunities and abilities to succeed.", "You need metal keys to read books.", "Doors can only be opened with brass.", "Schools have many locks."], "Metaphors directly equate knowledge with unlocking future opportunities."),
                 ("What does the idiom 'raining cats and dogs' mean in conversation?", ["It is raining exceptionally hard and heavily.", "Animals are falling from clouds.", "Dogs are chasing cats outside.", "The weather is completely dry."], "Idioms have figurative meanings distinct from their literal words.")
             ],
             "Figurative language uses creative comparisons (similes with like/as, metaphors, personification) to paint visual pictures.",
             "Ask: Does the author mean this literally word-for-word, or as a creative visual comparison?",
             ("Highlight comparison words like 'like' or 'as'.", "Draw what the sentence literally says versus what it actually means.", "Review Grade 3 context clues.")
            )
        ])
    ]),
    "filipino": ("Filipino", [
        (3, [
            ("Pangunahing Diwa at mga Sumusuportang Detalye", "Pagtukoy sa paksang diwa at mahahalagang detalye sa binasang talata.", "fil-g2-pag-unawa-sa-binasa",
             [
                 ("Araw-araw na nagdidilig, nag-aalis ng damo, at naglalagay ng pataba si Mang Lino sa kaniyang mga tanim. Ano ang pangunahing diwa?", ["Masipag na inaalagaan ni Mang Lino ang kaniyang halamanan.", "Makulay ang mga paso sa bakuran.", "Malamig ang tubig mula sa poso.", "Malapad ang kalsada sa tapat."], "Lahat ng detalye ay nagpapakita ng masusing pag-aalaga sa mga halaman."),
                 ("Aling detalye ang sumusuporta sa diwang 'Mahalaga ang pagtutulungan sa barangay'?", ["Nagtulong-tulong ang mga magkakapitbahay sa paglilinis ng baradong kanal.", "Pula ang bubong ng barangay hall.", "May tatlong aso sa tapat ng tindahan.", "Mainit ang sikat ng araw kahapon."], "Ang sama-samang paglilinis ay direktang patunay ng pagtutulungan."),
                 ("Ano ang pinakamabisang itanong upang matukoy ang pangunahing diwa ng talata?", ["Tungkol saan ang kabuuan ng binasang talata?", "Ilang salita ang nagsisimula sa titik M?", "Anong kulay ng papel na pinaglimbagan?", "Sino ang nagbenta ng aklat?"], "Ang pangunahing diwa ay ang pinakapunto ng buong talata."),
                 ("Alin sa mga sumusunod ang HINDI sumusuporta sa diwang 'Masustansiya ang mga sariwang gulay'?", ["Mayaman sa bitamina at mineral ang malunggay at kangkong.", "Tumutulong ang gulay sa maayos na panunaw ng katawan.", "May mga sapatos na gawa sa balat ng hayop.", "Nagbibigay ng lakas at resistensiya ang pagkain ng gulay."], "Walang kinalaman ang sapatos sa nutrisyon ng gulay.")
             ],
             "Ang pangunahing diwa ang pinaka-ubod ng talata; ang mga detalye ang nagpapaliwanag dito.",
             "Itanong: Ano ang pinakamahalagang kaisipan na nais iparating ng sumulat?",
             ("Salungguhitan ang pangungusap na naglalaman ng buod ng talata.", "Magtala ng 2 patunay mula sa kwento.", "Balikan ang pag-unawa sa binasa mula sa Baitang 2.")
            ),
            ("Paggamit ng mga Bahagi ng Pananalita (Pangngalan at Panghalip)", "Wastong paggamit ng pangngalan, kailanan, at panghalip panao sa pangungusap.", "fil-g2-balarila",
             [
                 ("Alin ang angkop na panghalip panao para sa: 'Sina Ana at ako ay maagang pumasok sa paaralan'?", ["Kami", "Sila", "Kayo", "Tayo"], "Ang 'Sina Ana at ako' ay tumutukoy sa nagsasalita at kasama, kaya 'Kami' ang angkop."),
                 ("Anong uri ng pangngalan ang salitang 'Bulkang Mayon'?", ["Pangngalang Pantangi (tiyak na ngalan)", "Pangngalang Pambalana (karaniwang ngalan)", "Panghalip Pamatlig", "Pandiwa"], "Tiyak na ngalan ng bulkan na nagsisimula sa malaking titik."),
                 ("Alin ang pangungusap na may wastong kailanan ng pangngalan?", ["Ang mga mag-aaral ay masayang nagbabasa sa aklatan.", "Ang mga bata ay naglalaro ng isang mga bola.", "Ang kapatid ko ay maraming lapis na pula.", "Ang magkaibigan ay nagtulong sa gawain."], "Wasto ang pagtutugma ng maramihang panlapi at pangngalan."),
                 ("Aling panghalip pamatlig ang angkop kung hawak mo ang bagay na itinuturo?", ["Ito", "Iyan", "Iyon", "Doon"], "Ginagamit ang 'ito' kapag hawak o malapit sa nagsasalita ang bagay.")
             ],
             "Ang pangngalan ay ngalan ng tao, bagay, hayop, o pook; ang panghalip ang humahalili rito.",
             "Tingnan kung sino ang tinutukoy: ako/kami (nagsasalita), ikaw/kayo (kinakausap), o siya/sila (pinag-uusapan).",
             ("Piliin ang tamang panghalip panao sa bawat patlang.", "Tukuyin kung pantangi o pambalana ang salita.", "Balikan ang mga pangngalan mula sa Baitang 2.")
            ),
            ("Kayarian at Aspekto ng Pandiwa", "Pagtukoy sa salitang-ugat, panlapi, at aspekto ng pandiwa (naganap, nagaganap, magaganap).", "fil-g2-balarila",
             [
                 ("Ano ang salitang-ugat ng pandiwang 'naglaba'?", ["laba", "nag", "nagl", "labahan"], "Ang salitang-ugat ay ang payak na salita na walang panlapi."),
                 ("Aling pandiwa ang nasa aspektong nagaganap (pangkasalukuyan)?", ["nagsusulat", "sumulat", "susulat", "kasusulat"], "Ang pag-uulit ng unang pantig at panlaping nag- ay nagpapakita ng kilos na kasalukuyang ginagawa."),
                 ("Bukas ng umaga, si Lito ay __________ sa paligsahan sa pagtakbo.", ["tatakbo", "tumakbo", "tumatakbo", "katatakbo"], "Ang salitang 'bukas' ay nagpapahiwatig ng aspektong magaganap (hinaharap)."),
                 ("Alin ang wastong pandiwa sa aspektong naganap (perpektibo)?", ["Nagluto si Nanay ng masarap na sinigang kaninang tanghali.", "Nagluluto si Nanay ngayon.", "Magluluto si Nanay mamaya.", "Lulutuin ni Nanay bukas."], "Ang salitang 'kanina' ay nagpapakita na tapos na ang kilos.")
             ],
             "Ang pandiwa ay salitang nagsasaad ng kilos: Naganap (tapos na), Nagaganap (ginagawa pa), at Magaganap (gagawin pa).",
             "Hanapin ang pahiwatig ng panahon sa pangungusap tulad ng 'kanina', 'ngayon', o 'bukas'.",
             ("Tukuyin ang salitang-ugat at panlaping ikinabit.", "Gawing naganap, nagaganap, at magaganap ang salita.", "Balikan ang mga payak na pandiwa mula sa Baitang 2.")
            ),
            ("Pagsunod-sunod ng mga Pangyayari sa Kuwento", "Pagsasaayos ng mga pangyayari ayon sa tamang kronolohikal na pagkakasunod-sunod.", "fil-g2-pag-unawa-sa-binasa",
             [
                 ("Alin ang dapat maunang mangyari sa pagtatanim ng monggo?", ["Ihanda ang matabang lupa at itanim ang mga buto ng monggo.", "Pitasin ang mga hinog na bunga.", "Magbenta ng ani sa palengke.", "Lutuin ang naaning monggo."], "Kailangang itanim muna ang buto bago mamunga at maani."),
                 ("Aling salitang transisyon ang nagpapakita ng huling hakbang?", ["Sa wakas", "Noong una", "Kasunod nito", "Bago iyon"], "'Sa wakas' o 'Sa huli' ang naghuhudyat ng pagtatapos ng mga pangyayari."),
                 ("Bakit mahalagang isaayos ang mga pangyayari ayon sa tamang pagkakasunod-sunod?", ["Upang maging malinaw at madaling maunawaan ang takbo ng salaysay.", "Upang humaba ang aklat.", "Upang maging makulay ang larawan.", "Upang mawala ang problema sa kwento."], "Ang tamang banghay ay nagbibigay ng lohikal na daloy sa kwento."),
                 ("Kung naghugas ng kamay si Ben, kumain ng tanghalian, at nagsipilyo, ano ang ginawa niya pagkatapos kumain?", ["Nagsipilyo ng ngipin", "Naghugas ng kamay", "Nagluto ng pagkain", "Pumunta sa palengke"], "Ayon sa pagkakasunod-sunod, nagsipilyo siya pagkatapos kumain.")
             ],
             "Gamitin ang mga hudyat ng pagkakasunod-sunod: Una, Kasunod, Pagkatapos, at Sa Huli.",
             "Tingnan ang tamang daloy ng kwento mula simula hanggang wakas.",
             ("Lagyan ng bilang 1 hanggang 4 ang mga pangyayari sa larawan.", "Isalaysay muli ang kwento gamit ang sariling salita.", "Balikan ang pagsusunod-sunod mula sa Baitang 2.")
            )
        ]),
        (4, [
            ("Pagtukoy sa Sanhi at Bunga sa Binasang Teksto", "Pagsusuri sa ugnayan ng sanhi (dahilan) at bunga (resulta) gamit ang mga hudyat na salita.", "fil-g3-pag-unawa-sa-binasa",
             [
                 ("Dahil hindi nag-aral si Ramon kagabi, mababa ang kaniyang nakuha sa pagsusulit. Ano ang sanhi?", ["Hindi nag-aral si Ramon kagabi.", "Mababa ang kaniyang nakuha sa pagsusulit.", "Pumasok si Ramon sa paaralan.", "Nawala ang kaniyang lapis."], "Ang dahilan kung bakit mababa ang marka ay ang hindi pag-aaral."),
                 ("Aling pang-ugnay ang karaniwang nagpapakilala sa bunga o resulta?", ["kaya", "dahil sa", "sapagkat", "kasi"], "Ang 'kaya' at 'bunga nito' ay naghuhudyat ng kinalabasan ng pangyayari."),
                 ("Nagtanim ng maraming puno ang mga mamamayan sa kabundukan. Ano ang posibleng maging bunga nito?", ["Naiwasan ang malawakang pagguho ng lupa at baha.", "Namatay ang mga ibon sa gubat.", "Nawalan ng tubig ang ilog.", "Natuyo ang buong kagubatan."], "Ang mga ugat ng puno ay humahawak sa lupa upang maiwasan ang landslide."),
                 ("Alin ang pangungusap na may wastong ugnayang sanhi at bunga?", ["Matiyagang nag-ensayo ang koponan kaya nasungkit nila ang kampeonato.", "Sumikat ang araw kaya nagdilim ang buong langit.", "Natulog nang maaga si Bea kaya nahuli siya sa klase.", "Uminom siya ng gamot kaya lumala ang kaniyang sakit."], "Wasto at lohikal ang ugnayan ng pag-eensayo sa pagkapanalo.")
             ],
             "Ang SANHI ang dahilan (Bakit nangyari?); ang BUNGA ang resulta o kinalabasan (Ano ang nangyari?).",
             "Hanapin ang mga hudyat: 'dahil/sapagkat' para sa sanhi, 'kaya/bunga nito' para sa bunga.",
             ("Gumuhit ng arrow mula sa sanhi patungo sa bunga.", "Bumuo ng pangungusap gamit ang 'dahil' at 'kaya'.", "Balikan ang pangunahing diwa mula sa Baitang 3.")
            ),
            ("Pagtukoy sa Opinyon at Katotohanan", "Pagtitiyak kung ang pahayag ay mapatutunayang katotohanan o pansariling opinyon.", "fil-g3-pag-unawa-sa-binasa",
             [
                 ("Alin sa mga sumusunod ang isang napatutunayang katotohanan?", ["Ang Pilipinas ay isang kapuluan na binubuo ng mahigit 7,000 pulo.", "Para sa akin, ang mangga ang pinakamasarap na prutas sa buong mundo.", "Mas magandang manirahan sa lungsod kaysa sa probinsya.", "Lahat ng bata ay mahilig sa kulay asul."], "Ang bilang ng pulo sa Pilipinas ay heograpikal na datos na mapatutunayan."),
                 ("Aling salita o parirala ang nagpapahiwatig na ang pahayag ay isang opinyon lamang?", ["Sa aking palagay", "Ayon sa pagsasaliksik", "Batay sa talaan ng pamahalaan", "Napatunayan sa laboratoryo"], "Ang 'sa aking palagay' o 'para sa akin' ay nagpapakita ng personal na pananaw."),
                 ("Bakit mahalagang marunong kumilatis sa pagitan ng opinyon at katotohanan?", ["Upang hindi madaling malinlang ng maling impormasyon sa balita o social media.", "Upang maging pinakamabilis magbasa.", "Upang makapagsulat ng mahabang tula.", "Upang matandaan ang alpabeto."], "Ang mapanuring pag-iisip ay nagpoprotekta laban sa pekeng balita."),
                 ("Alin ang pangungusap na nagpapahayag ng opinyon?", ["Mas masarap mag-aral kapag nakikinig sa musika.", "Ang bulkang Taal ay matatagpuan sa lalawigan ng Batangas.", "May pitong araw sa loob ng isang linggo.", "Ang tubig ay binubuo ng hydrogen at oxygen."], "Ang pagiging 'mas masarap mag-aral' ay pansariling kagustuhan at hindi pareho sa lahat.")
             ],
             "Ang KATOTOHANAN ay may patunay at ebidensya; ang OPINYON ay sariling kuro-kuro o damdamin.",
             "Tingnan kung may panandang 'ayon sa datos' (katotohanan) o 'para sa akin' (opinyon).",
             ("Tukuyin kung K (Katotohanan) o O (Opinyon) ang bawat pahayag.", "Magbigay ng ebidensya para patunayan ang katotohanan.", "Balikan ang pagkilatis sa teksto mula sa Baitang 3.")
            ),
            ("Paggamit ng Pang-uri at Kaantasan Nito", "Wastong paglalarawan gamit ang kaantasang lantay, pahambing, at pasukdol.", "fil-g3-balarila",
             [
                 ("Si Rosa ay matangkad, ngunit __________ si Carla kaysa kay Rosa.", ["mas matangkad", "pinakamatangkad", "ubod ng tangkad", "matangkad"], "Naghahambing ng dalawang tao gamit ang 'kaysa kay', kaya 'mas matangkad' ang angkop."),
                 ("Alin ang pangungusap na nasa kaantasang pasukdol (pinakamataas na antas)?", ["Ang Bundok Apo ang pinakamataas na bundok sa buong Pilipinas.", "Mataas ang puno ng niyog.", "Mas mataas ang burol kaysa sa punso.", "Medyo mataas ang bakod."], "Ang panlaping 'pinaka-' ay nagpapakita ng sukdulang katangian sa lahat."),
                 ("Ano ang kaantasan ng pang-uri sa: 'Sariwa ang mga gulay na inani kanina'?", ["Lantay", "Pahambing na magkatulad", "Pahambing na di-magkatulad", "Pasukdol"], "Naglalarawan ng isang katangian nang walang paghahambing."),
                 ("Alin ang angkop na pahambing na magkatulad?", ["Magkasingganda ang mga bulaklak sa hardin.", "Mas maganda ang rosas kaysa gumamela.", "Pinakamaganda ang orkidyas sa lahat.", "Napakaganda ng bulaklak."], "Ang panlaping 'magkasing-' ay nagpapakita ng pantay na katangian.")
             ],
             "Kaantasan ng Pang-uri: Lantay (nag-iisa), Pahambing (naghahambing ng dalawa), at Pasukdol (namumukod sa lahat).",
             "Pansinin ang mga panlapi: mas/magkasing (dalawa) at pinaka/ubod ng (lahat).",
             ("Sumulat ng pangungusap sa bawat antas: lantay, pahambing, pasukdol.", "Ilarawan ang dalawang bagay gamit ang 'mas' o 'magkasing'.", "Balikan ang mga pang-uri mula sa Baitang 3.")
            ),
            ("Pang-abay na Pamaraan, Pamanahon, at Panlunan", "Pagtukoy at paggamit ng pang-abay na naglalarawan sa pandiwa, pang-uri, o kapwa pang-abay.", "fil-g3-balarila",
             [
                 ("Mabilis na tumakbo ang atleta upang maabot ang finish line. Alin ang pang-abay na pamaraan?", ["Mabilis", "tumakbo", "atleta", "finish line"], "Naglalarawan kung PAANO tumakbo ang atleta."),
                 ("Aling pangungusap ang naglalaman ng pang-abay na pamanahon (sumasagot sa kailan)?", ["Maagang gumising si Tatay upang pumunta sa sakahan.", "Sa plasa masayang naglaro ang mga bata.", "Mahinahong nakiusap ang mag-aaral.", "Mataas tumalon ang pusa."], "Ang 'maaga' ay nagsasaad ng panahon o oras ng pagkilos."),
                 ("Alin ang pang-abay na panlunan sa: 'Nagtago ang pusa sa ilalim ng mesa'?", ["sa ilalim ng mesa", "nagtago", "pusa", "mesa lamang"], "Nagsasaad kung SAAN naganap ang kilos ng pagtatago."),
                 ("Paano naiiba ang pang-abay sa pang-uri?", ["Ang pang-abay ay naglalarawan ng pandiwa/kilos; ang pang-uri ay naglalarawan ng pangngalan.", "Pareho lamang silang naglalarawan ng tao.", "Ang pang-abay ay kilos mismo.", "Ang pang-uri ay laging pamanahon."], "Magkaiba ang kanilang binibigyang-turing sa pangungusap.")
             ],
             "Pang-abay: Pamaraan (Paano?), Pamanahon (Kailan?), at Panlunan (Saan naganap ang kilos?).",
             "Itanong: Inilalarawan ba nito kung PAANO, KAILAN, o SAAN ginawa ang kilos?",
             ("Tukuyin kung pamaraan, pamanahon, o panlunan ang nakasalungguhit.", "Bumuo ng pangungusap gamit ang bawat uri ng pang-abay.", "Balikan ang mga pandiwa at pang-uri mula sa Baitang 3.")
            )
        ])
    ]),
    "makabansa": ("Filipino", [
        (3, [
            ("Mga Sagisag at Simbolo sa Mapa ng Pamayanan", "Pagpapaliwanag sa mga pananda at simbolo sa mapa ng sariling komunidad.", "mak-g2-pamayanan",
             [
                 ("Ano ang karaniwang ipinahihiwatig ng krus na simbolo sa mapa ng pamayanan?", ["Pangkalusugang pasilidad o ospital/klinika", "Pook-palaruan", "Pamilihang bayan", "Sapa o ilog"], "Ang krus na pananda ay ginagamit para sa ospital o health center."),
                 ("Bakit mahalaga ang kinalalagyan ng 'Compass Rose' (Direksyon) sa isang mapa?", ["Upang matukoy ang Hilaga, Timog, Silangan, at Kanluran ng mga lugar.", "Upang maging makulay ang mapa.", "Upang itago ang mga kalsada.", "Upang sukatin ang bigat ng lupa."], "Ang compass rose ay gabay sa apat na pangunahing direksyon."),
                 ("Aling simbolo ang angkop para sa isang pampublikong paaralan sa mapa?", ["Gusali na may bandila o nakabukas na aklat", "Isang bangka sa alon", "Puno ng niyog lamang", "Sasakyang panghimpapawid"], "Ang aklat at bandila ay sagisag ng edukasyon at paaralan."),
                 ("Ano ang unang dapat tingnan sa mapa bago maglakbay sa hindi pamilyar na lugar?", ["Ang 'Legend' o Talaan ng mga Simbolo at Pananda", "Ang kulay ng gilid ng papel", "Ang pangalan ng nag-imprenta", "Ang presyo ng mapa"], "Ipinapaliwanag ng Legend ang kahulugan ng bawat guhit at simbolo.")
             ],
             "Ang mga simbolo sa mapa ay nagbibigay ng mabilis at malinaw na impormasyon tungkol sa kinalalagyan ng mga gusali at pook.",
             "Gamitin ang Legend at Compass Rose upang matukoy ang direksyon at kahulugan ng bawat pananda.",
             ("Iguhit ang mapa ng iyong barangay at lagyan ng wastong simbolo ang simbahan, paaralan, at health center.", "Gamitin ang 4 na pangunahing direksyon.", "Balikan ang sariling komunidad mula sa Baitang 2.")
            ),
            ("Kultura, Tradisyon, at Pagkakakilanlan ng Pamayanan", "Pagpapahalaga sa mga tradisyon, pista, at sining na nagbubuklod sa komunidad.", "mak-g2-kultura-kasaysayan",
             [
                 ("Paano ipinapakita ng mga Pilipino ang diwa ng 'Bayanihan' sa pamayanan?", ["Sama-samang pagtutulungan sa panahon ng pangangailangan nang walang hinihintay na kapalit.", "Paniningil ng bayad sa bawat tulong.", "Panonood lamang habang nahihirapan ang kapitbahay.", "Pag-aalis sa sariling bayan."], "Ang Bayanihan ay kusang-loob na pagkakaisa para sa kabutihan ng kapwa."),
                 ("Aling tradisyon ang nagpapakita ng paggalang sa mga nakatatanda sa pamilya?", ["Pagmamano at pagsasabi ng 'po' at 'opo'", "Pagsigaw kapag tinatawag", "Hindi pakikinig sa payo", "Pagtalikod kapag kinakausap"], "Ang pagmamano ay katutubong pagpapahalaga sa karunungan at pagpapala ng nakatatanda."),
                 ("Bakit ipinagdiriwang ng mga pamayanan ang kapistahan ng kanilang patron o anibersaryo?", ["Upang magpasalamat sa masaganang ani at magkaisa ang buong komunidad.", "Upang mag-aksaya ng salapi at pagkain.", "Upang magkaroon ng dahilan para mag-away.", "Upang pigilan ang pagpasok ng mga bisita."], "Ang mga pista ay pasasalamat at pagdiriwang ng kultural na pamana."),
                 ("Ano ang kahalagahan ng pag-iingat sa mga katutubong laro tulad ng patintero at tumbang preso?", ["Pinapanatili nito ang ating pamanang kultura at nagpapalakas ng katawan at pagkakaibigan.", "Wala itong kabuluhan sa makabagong panahon.", "Mas mainam na maglaro lamang sa cellphone.", "Nakakasira ito sa pagsasamahan."], "Ang mga katutubong laro ay bahagi ng ating pambansang identidad.")
             ],
             "Ang kultura, tradisyon, at mga pagdiriwang ay nagbubuklod sa mga mamamayan at nagpapakilala sa ating pagka-Pilipino.",
             "Isaisip ang mga kaugaliang Pilipino tulad ng Bayanihan, pagmamano, at malasakit.",
             ("Magkuwento tungkol sa isang tradisyon o pista sa inyong bayan.", "Ilista ang 3 magagandang kaugalian sa inyong tahanan.", "Balikan ang pamilya at kapwa mula sa Baitang 2.")
            ),
            ("Kabuhayan at Likas na Yaman sa Rehiyon", "Pag-uugnay ng uri ng kabuhayan sa kapaligiran at yamang lupa, tubig, at gubat.", "mak-g2-kabuhayan-yaman",
             [
                 ("Anong pangunahing kabuhayan ang angkop sa mga pamayanang malapit sa baybayin at dagat?", ["Pangingisda at pag-aasin", "Pangangahoy sa gubat", "Pagtatanim ng trigo", "Paggawa ng mga sasakyang panghimpapawid"], "Ang yamang-dagat ay nagbibigay ng hanapbuhay sa mga mangingisda."),
                 ("Bakit pagsasaka ang pangunahing hanapbuhay sa Gitnang Luzon (Central Luzon)?", ["Dahil sa malalawak at matatabang kapatagan na angkop sa pagtatanim ng palay.", "Dahil napapaligiran ito ng matataas na yelo.", "Dahil puro buhangin ang lupa rito.", "Dahil walang ilog o ulan sa rehiyon."], "Ang kapatagan ng Gitnang Luzon ang tinaguriang 'Rice Granary of the Philippines'."),
                 ("Ano ang responsableng paraan ng paggamit sa ating yamang-gubat?", ["Pagtatanim ng mga bagong puno kapalit ng mga pinutol (Reforestation)", "Walang habas na pagtotroso o illegal logging", "Pagsusunog ng kagubatan (Kaingin)", "Panghuhuli sa lahat ng endangered na hayop"], "Ang muling pagtatanim ay nagtitiyak na may magagamit pa ang susunod na henerasyon."),
                 ("Paano nakatutulong ang wastong pangangalaga sa mga coral reef o bahura sa mga mangingisda?", ["Dito nangingitlog at lumalaki ang mga isda kaya nananatiling masagana ang huli.", "Ginagawang mababaw ang buong karagatan.", "Pinapatay nito ang mga halamang-dagat.", "Wala itong epekto sa dami ng isda."], "Ang malusog na bahura ang tirahan at nursery ng mga yamang-dagat.")
             ],
             "Ang uri ng hanapbuhay ng mga tao ay nakasalalay sa likas na yaman ng kanilang kapaligiran.",
             "Ang yamang lupa, tubig, at gubat ay dapat gamitin nang may pananagutan upang hindi maubos.",
             ("Itugma ang hanapbuhay (magsasaka, mangingisda, minero) sa tamang anyong lupa o tubig.", "Magmungkahi ng paraan para makatipid sa tubig.", "Balikan ang mga anyong lupa at tubig mula sa Baitang 2.")
            ),
            ("Mga Namumuno at Paglilingkod sa Pamayanan", "Pagkilala sa mga pinuno ng barangay at bayan at kanilang mga tungkulin sa paglilingkod.", "mak-g2-pamahalaan",
             [
                 ("Sino ang namumuno sa pamahalaang barangay at nagpapanatili ng kaayusan at kapayapaan dito?", ["Punong Barangay (Kapitan)", "Pangulo ng Bansa", "Guro sa Paaralan", "Doktor sa Ospital"], "Ang Punong Barangay ang punong ehekutibo ng pamahalaang barangay."),
                 ("Ano ang pangunahing tungkulin ng mga Barangay Health Workers (BHW) sa komunidad?", ["Magbigay ng pangunahing serbisyong medikal, bakuna, at payong pangkalusugan sa mga residente.", "Manghuli ng mga lumalabag sa batas trapiko sa highway.", "Magturo sa kolehiyo.", "Mag-ayos ng mga sirang tulay sa lungsod."], "Sila ang frontliners sa pangangalaga sa kalusugan ng mga mamamayan sa barangay."),
                 ("Bakit mahalagang makilahok ang mga mamamayan sa Barangay Assembly at mga gawaing pampamayanan?", ["Upang marinig ang kanilang boses at makatulong sa pagpapasya para sa ikabubuti ng lahat.", "Upang makipag-away sa mga kapitbahay.", "Upang maiwasan ang pagtatrabaho.", "Wala itong maitutulong sa barangay."], "Ang aktibong pakikilahok ay pundasyon ng demokratikong pamamahala."),
                 ("Paano maipakikita ng isang bata sa Grade 3 ang pagtulong sa mga pinuno ng barangay?", ["Pagsunod sa mga ordinansa sa kalinisan at tamang pagtatapon ng basura.", "Pagsira sa mga pananim sa plasa.", "Paggala sa gabi kapag may curfew.", "Pagtatapon ng balat ng kendi sa kanal."], "Ang disiplina at pagsunod sa tuntunin ay malaking tulong sa kaayusan.")
             ],
             "Ang mga pinuno sa pamayanan ay inihalal upang maglingkod at magpatupad ng mga proyektong pangkaunlaran.",
             "Ang bawat mamamayan, bata man o matanda, ay may tungkulin na sumunod sa mga alituntunin ng barangay.",
             ("Kilalanin ang mga pinuno sa inyong barangay at ilista ang kanilang mga serbisyo.", "Sumulat ng liham pasasalamat sa mga barangay tanod o health workers.", "Balikan ang tungkulin sa pamayanan mula sa Baitang 2.")
            )
        ]),
        (4, [
            ("Heograpiya at Teritoryo ng Pilipinas", "Pagsusuri sa lokasyon, hangganan, at teritoryo ng Pilipinas gamit ang grid at mapa.", "mak-g3-mapa-heograpiya",
             [
                 ("Ano ang tiyak na lokasyon ng Pilipinas sa globo batay sa mga guhit latitud at longhitud?", ["Sa pagitan ng 4° hanggang 21° Hilagang Latitud at 116° hanggang 127° Silangang Longhitud", "Nasa pinakatuktok ng Hilagang Polo", "Nasa Timog Amerika", "Nasa kontinente ng Europa"], "Ito ang eksaktong grid coordinates ng kapuluan ng Pilipinas."),
                 ("Aling anyong tubig ang nasa silangang bahagi ng teritoryo ng Pilipinas?", ["Karagatang Pasipiko (Pacific Ocean / Philippine Sea)", "Dagat Kanlurang Pilipinas (West Philippine Sea)", "Dagat Celebes", "Kipot ng Luzon"], "Ang Karagatang Pasipiko ang nasa silangan kung saan karaniwang nabubuo ang mga bagyo."),
                 ("Ayon sa Artikulo 1 ng 1987 Konstitusyon, ano ang bumubuo sa Pambansang Teritoryo ng Pilipinas?", ["Ang kapuluan ng Pilipinas kasama ang lahat ng mga pulo at mga karagatang nakapaloob dito.", "Ang kalupaan lamang nang walang karagatan.", "Ang mga bansa sa Timog Silangang Asya.", "Lahat ng kontinente sa Asya."], "Sakop ng teritoryo ang kalupaan, katubigan, at himpapawid na nasa hurisdiksiyon ng bansa."),
                 ("Bakit mahalagang malaman ang eksaktong hangganan ng teritoryo ng ating bansa?", ["Upang maipagtanggol ang soberanya at maprotektahan ang ating mga likas na yaman laban sa dayuhang panghihimasok.", "Upang magtayo ng pader sa gitna ng dagat.", "Upang ipagbawal ang paglalakbay ng mga Pilipino.", "Upang hindi na kailanganin ang mapa."], "Ang malinaw na teritoryo ang batayan ng pambansang soberanya at Exclusive Economic Zone (EEZ).")
             ],
             "Ang Pilipinas ay isang arkipelago sa Timog-Silangang Asya na napaliligiran ng Karagatang Pasipiko at Dagat Kanlurang Pilipinas.",
             "Gamitin ang grid coordinates (latitud at longhitud) upang matukoy ang tiyak na hangganan ng bansa.",
             ("Hanapin ang Pilipinas sa globo gamit ang latitud at longhitud.", "Tukuyin ang mga anyong tubig na nakapalibot sa apat na direksyon ng bansa.", "Balikan ang mga sagisag sa mapa mula sa Baitang 3.")
            ),
            ("Pambansang Pagkakakilanlan at Pamanang Kultural", "Pagsusuri sa mga pambansang sagisag, wika, at pamanang kultural ng lahing Pilipino.", "mak-g3-kultura-kasaysayan",
             [
                 ("Bakit idineklara ang Pambansang Wikang Filipino bilang opisyal na wika ng komunikasyon at edukasyon?", ["Upang magsilbing tulay ng pagkakaunawaan at pagkakaisa ng mga Pilipino mula sa iba't ibang rehiyon.", "Upang kalimutan ang mga rehiyonal na diyalekto.", "Upang hindi na matuto ng ibang wika.", "Dahil ito lamang ang alam sa Luzon."], "Ang pambansang wika ay sagisag ng pagkakaisa sa gitna ng ating pagkakaiba-iba."),
                 ("Ano ang sinasagisag ng walong sinag ng araw sa Pambansang Watawat ng Pilipinas?", ["Ang unang walong lalawigan na nag-alsa laban sa pananakop ng mga Espanyol.", "Ang walong pinakamalaking isla sa bansa.", "Ang walong wika sa Pilipinas.", "Ang walong pangulo ng bansa."], "Kinakatawan nito ang Maynila, Cavite, Bulacan, Pampanga, Nueva Ecija, Bataan, Laguna, at Batangas."),
                 ("Alin sa mga sumusunod ang halimbawa ng pamanang pangkultura na hindi nahahawakan (Intangible Cultural Heritage)?", ["Ang tradisyon ng pag-awit ng Pasyon at Hudhud Chants ng mga Ifugao", "Ang Lumang Simbahan ng San Agustin", "Ang Fort Santiago sa Intramuros", "Ang Pambansang Museo"], "Ang mga awit, epiko, at ritwal ay mga buhay na tradisyon na ipinapasa sa salinlahi."),
                 ("Paano dapat pangalagaan ng mga kabataan ang mga makasaysayang pook at monumento?", ["Igalang ang mga ito, huwag sulatan o sirain (vandalism), at alamin ang kasaysayan sa likod nito.", "Ukitan ng sariling pangalan ang mga bato.", "Magtapon ng basura sa paligid.", "Sirain ang mga lumang rebulto."], "Ang mga makasaysayang bantayog ay patunay ng kabayanihan at kasaysayan ng bansa.")
             ],
             "Ang ating mga pambansang sagisag, wika, at kasaysayan ang salamin ng ating kalayaan at pambansang dangal.",
             "Igalang ang watawat at awitin nang buong puso ang 'Lupang Hinirang'.",
             ("Ilarawan ang kahulugan ng bawat kulay at simbolo sa watawat ng Pilipinas.", "Magtala ng isang makasaysayang pook sa inyong lalawigan.", "Balikan ang kultura at tradisyon mula sa Baitang 3.")
            ),
            ("Likas na Yaman at Likas-Kayang Pag-unlad", "Pagtataguyod ng likas-kayang paggamit sa yamang mineral, enerhiya, at agrikultura.", "mak-g3-kabuhayan-yaman",
             [
                 ("Ano ang ibig sabihin ng 'Likas-Kayang Pag-unlad'?", ["Pagtugon sa mga pangangailangan ng kasalukuyan nang hindi isinasakripisyo ang kakayahan ng susunod na henerasyon na matugunan ang kanilang sariling pangangailangan.", "Paggamit sa lahat ng yaman ngayon upang yumaman agad.", "Pagbabawal sa anumang uri ng paggamit sa kalikasan.", "Pag-aangkat ng lahat ng pagkain mula sa ibang bansa."], "Balanse sa pagitan ng pag-unlad ng ekonomiya at pangangalaga sa kalikasan."),
                 ("Aling pinagkukunan ng enerhiya sa Pilipinas ang itinuturing na 'Renewable Energy' o napapalitan?", ["Enerhiyang Geothermal mula sa init ng bulkan at Solar mula sa araw", "Maka-uling na planta (Coal power plant)", "Diesel generator", "Petrolyo mula sa langis"], "Ang geothermal, solar, hydro, at wind ay hindi nauubos at malinis na enerhiya."),
                 ("Ano ang magiging masamang epekto ng 'Open-Pit Mining' kapag hindi maayos na pinangasiwaan?", ["Pagkawasak ng mga bundok, pagkalason ng mga ilog, at pagkawala ng tirahan ng mga katutubo.", "Pagdami ng mga punongkahoy.", "Paglinis ng hangin sa paligid.", "Pagdami ng sariwang isda sa ilog."], "Ang iresponsableng pagmimina ay nagdudulot ng permanenteng pinsala sa kalikasan."),
                 ("Paano makatutulong ang mga komunidad sa likas-kayang agrikultura?", ["Paggamit ng organikong pataba at pamamaraang crop rotation upang mapanatiling mataba ang lupa.", "Paggamit ng labis na kemikal na pestisidyo.", "Pagtatapon ng basurang plastik sa bukirin.", "Pagsunog sa dayami (open burning)."], "Ang organikong pagsasaka ay nagpapanatili sa sustansya ng lupa nang walang lason.")
             ],
             "Ang likas-kayang pag-unlad ay ang matalinong paggamit sa ating mga yaman upang may maabutan pa ang mga susunod na Pilipino.",
             "Suportahan ang malinis na enerhiya (solar, wind) at organikong agrikultura.",
             ("Maglista ng 3 paraan kung paano magiging likas-kaya ang paggamit ng kuryente sa tahanan.", "Ipaliwanag kung bakit mahalaga ang renewable energy.", "Balikan ang kabuhayan at likas na yaman mula sa Baitang 3.")
            ),
            ("Mga Karapatan at Tungkulin ng Mamamayang Pilipino", "Pagsusuri sa mga Saligang Karapatan at Pananagutan ng bawat mamamayan sa lipunan.", "mak-g3-pagkamamamayan",
             [
                 ("Ayon sa Katipunan ng mga Karapatan (Bill of Rights), alin ang pangunahing karapatan ng bawat tao?", ["Karapatan sa buhay, kalayaan, at kapanatagan ng sarili", "Karapatang gumawa ng krimen nang walang parusa", "Karapatang mang-agaw ng pag-aari ng iba", "Karapatang lumabag sa batas trapiko"], "Ang karapatan sa buhay at kalayaan ang pinakapundasyon ng lahat ng karapatang pantao."),
                 ("Kaakibat ng karapatan sa malayang pamamahayag, ano ang pananagutan ng isang responsableng mamamayan?", ["Magsabi ng totoo, huwag magpakalat ng kasinungalingan o manira ng puri ng kapwa.", "Magsalita ng masasamang salita sa lahat.", "Mag-imbento ng maling balita upang maging sikat.", "Huwag makinig sa katwiran ng iba."], "Ang kalayaan ay may hangganan kapag natatapakan na ang karapatan at dangal ng kapwa."),
                 ("Bakit mahalagang tuparin ng mga mamamayan ang tungkulin sa pagbabayad ng tamang buwis?", ["Upang may magamit ang pamahalaan sa pagpapatayo ng mga pampublikong paaralan, ospital, at kalsada.", "Upang yumaman ang mga pribadong kumpanya.", "Upang maubos ang pera ng mga tao.", "Wala itong kapakinabangan sa publiko."], "Ang buwis ang dugo ng bayan na nagpopondo sa mga pampublikong serbisyo."),
                 ("Paano maipapakita ng isang mag-aaral ang kaniyang tungkulin bilang mabuting mamamayan sa araw-araw?", ["Mag-aral nang mabuti, sumunod sa mga batas ng pamayanan, at tumulong sa kapwa.", "Balewalain ang mga tuntunin sa paaralan.", "Mag-aksaya ng tubig at kuryente.", "Makipag-away sa mga kalaro."], "Ang pagiging masipag na mag-aaral at masunuring mamamayan ay ambag sa bansa.")
             ],
             "Bawat karapatan na ating tinatamasa ay may katumbas na pananagutan at tungkulin sa bayan.",
             "Ang isang responsableng mamamayan ay sumusunod sa batas, nagbabayad ng buwis, at may malasakit sa kapwa.",
             ("Magbigay ng 2 halimbawa ng iyong karapatan bilang bata at ang katumbas nitong tungkulin.", "Ipaliwanag kung bakit mahalaga ang pagsunod sa batas trapiko.", "Balikan ang mga namumuno sa pamayanan mula sa Baitang 3.")
            )
        ])
    ]),
    "gmrc": ("Filipino", [
        (3, [
            ("Paggalang sa Sarili, Pamilya, at Kapwa", "Pagpapakita ng paggalang sa salita at kilos sa iba't ibang sitwasyon sa tahanan at paaralan.", "gmrc-g2-paggalang",
             [
                 ("Ano ang magalang na tugon kapag may matandang nagtatanong ng direksyon sa iyo?", ["Sagutin nang may paggalang gamit ang 'po' at 'opo' at ituro nang maayos ang daan.", "Tumakbo palayo nang walang imik.", "Pagtawanan ang kaniyang tanong.", "Sumigaw nang pabalang."], "Ang paggamit ng magagalang na pananalita ay pagpapakita ng mabuting asal."),
                 ("Paano mo igagalang ang isang kaklase na may ibang pananampalataya o relihiyon?", ["Makinig at igalang ang kaniyang paniniwala nang walang panghuhusga.", "Pilitin siyang sumunod sa iyong paniniwala.", "Tuksuhin siya habang nagdarasal.", "Iwasan siyang kausapin magpakailanman."], "Ang paggalang sa pagkakaiba-iba ay nagdudulot ng kapayapaan."),
                 ("Ano ang nararapat gawin habang nagsasalita ang guro o kaklase sa harap ng klase?", ["Makinig nang tahimik at maghintay ng tamang pagkakataon bago magtaas ng kamay.", "Makipagkwentuhan sa katabi nang malakas.", "Magbato ng papel sa sahig.", "Tumayo at lumabas nang walang paalam."], "Ang aktibong pakikinig ay tanda ng paggalang sa nagsasalita."),
                 ("Ano ang magalang na kilos kapag nakikipag-usap sa pamamagitan ng telepono o online chat?", ["Magpakilala nang maayos, gumamit ng mahinahong pananalita, at magpaalam nang magalang.", "Mag-type gamit ang puro malalaking titik upang magmukhang sumisigaw.", "Biglang ibaba ang tawag nang walang dahilan.", "Gumamit ng mga salitang nakasasakit ng damdamin."], "Ang netiquette at magalang na komunikasyon ay mahalaga online at offline.")
             ],
             "Ang paggalang ay ipinapakita sa pamamagitan ng magagalang na pananalita ('po' at 'opo'), pakikinig, at paggalang sa damdamin ng iba.",
             "Tratuhin ang iyong kapwa sa paraang nais mo ring tratuhin ka nila.",
             ("Magsanay sa paggamit ng magagalang na pananalita sa tahanan.", "Ilista ang 3 paraan ng pagpapakita ng paggalang sa guro at kaklase.", "Balikan ang paggalang mula sa Baitang 2.")
            ),
            ("Katapatan sa Salita at sa Gawa", "Pagsasabi ng totoo at pagiging matapat sa pag-aaral, tahanan, at pakikipagkaibigan.", "gmrc-g2-paggalang",
             [
                 ("Nakakita ka ng pitaka na nahulog ng iyong kaklase sa ilalim ng upuan. Ano ang matapat na kilos?", ["Pulutin ito at agad na isauli sa may-ari o ibigay sa guro.", "Itago ito sa iyong bag at gamitin ang pera.", "Iwanan ito upang makuha ng iba.", "Ipamigay ang laman sa ibang kaklase."], "Ang pagsasauli ng hindi sa iyo ay pagpapakita ng tunay na integridad."),
                 ("Nabasag mo nang hindi sinasadya ang paboritong baso ng iyong nanay. Ano ang dapat mong gawin?", ["Lumapit sa nanay, aminin ang totoo nang buong katapatan, at humingi ng paumanhin.", "Isisi ang pagkabasag sa alagang pusa.", "Itago ang mga bubog sa ilalim ng kama.", "Magkunwaring walang alam sa nangyari."], "Ang pag-amin sa pagkakamali ay nagpapakita ng tapang at katapatan."),
                 ("Bakit mahalagang maging matapat sa pagsagot sa mga pagsusulit sa paaralan?", ["Upang masukat ang tunay mong natutunan at mapanatili ang malinis na budhi.", "Upang maging pinakamataas sa klase kahit mandaya.", "Upang hindi na kailangang mag-aral kailanman.", "Wala itong halaga basta makapasa."], "Ang pandaraya ay panloloko sa sarili at kawalan ng integridad."),
                 ("Ano ang ibig sabihin ng kasabihang 'Ang katapatan ang pinakamahusay na patakaran' (Honesty is the best policy)?", ["Ang pagsasabi ng totoo ay nagdudulot ng tiwala, kapayapaan, at magandang samahan.", "Magsabi lamang ng totoo kapag may premyo.", "Magsinungaling kapag natatakot mapagalitan.", "Huwag magsalita upang hindi mahuli."], "Ang tiwala ng kapwa ay nabubuo sa pamamagitan ng tuloy-tuloy na katapatan.")
             ],
             "Ang katapatan sa salita at gawa ay nagtataguyod ng tiwala at nagpapatibay ng magandang samahan sa pamilya at kapwa.",
             "Laging piliin ang totoo kahit walang nakakakita sa iyo.",
             ("Magkwento ng isang pagkakataon kung kailan pinili mong magsabi ng totoo.", "Ipaliwanag kung bakit masama ang pandaraya sa pagsusulit.", "Balikan ang katapatan mula sa Baitang 2.")
            ),
            ("Pagkamasunurin at Disiplina sa Sarili", "Pagsunod sa mga alituntunin sa tahanan, paaralan, at pamayanan nang may kusa.", "gmrc-g2-pamamahala-ng-damdamin",
             [
                 ("Oras na ng pag-aaral ngunit nais mo pang maglaro ng video game. Ano ang nagpapakita ng disiplina sa sarili?", ["Tapusin muna ang mga takdang-aralin bago maglaro sa takdang oras.", "Maglaro buong gabi at kalimutan ang aralin.", "Magmaktol at sumigaw sa mga magulang.", "Punitin ang kwaderno."], "Ang pagtatakda ng prayoridad sa mahahalagang gawain ay tanda ng disiplina."),
                 ("Ano ang dapat gawin kapag nakita ang babalang 'Bawal Tumawid, Gamitin ang Footbridge' sa highway?", ["Umakyat at gumamit ng footbridge para sa sariling kaligtasan.", "Tumakbo sa gitna ng mabilis na mga sasakyan.", "Sirain ang karatula.", "Balewalain ang babala."], "Ang pagsunod sa batas trapiko ay nagliligtas ng buhay."),
                 ("Paano maipakikita ang disiplina sa paggamit ng oras (Punctuality)?", ["Paggising at pagpasok nang maaga upang hindi mahuli sa klase o usapan.", "Palaging pagdating nang huli ng isang oras.", "Pagpapaliban sa lahat ng gawain (Procrastination).", "Hindi pagsipot sa napagkasunduang oras."], "Ang pagpapahalaga sa oras ay paggalang sa oras ng iba."),
                 ("Bakit nagtatakda ang mga magulang at guro ng mga alituntunin sa tahanan at paaralan?", ["Upang gabayan tayo sa kaligtasan, kaayusan, at maayos na kinabukasan.", "Upang pahirapan ang mga bata.", "Upang mawalan ng kalayaan ang mag-aaral.", "Wala silang ibang magawa."], "Ang mga patakaran ay gabay tungo sa ligtas at maayos na pamumuhay.")
             ],
             "Ang disiplina sa sarili ay ang kakayahang gawin ang tama kahit walang nakabantay na magulang o guro.",
             "Unahin ang mahahalagang tungkulin bago ang paglilibang.",
             ("Gumawa ng pang-araw-araw na iskedyul ng iyong mga gawain.", "Magtala ng 3 alituntunin sa inyong tahanan na sinusunod mo araw-araw.", "Balikan ang pagsunod mula sa Baitang 2.")
            ),
            ("Pagmamalasakit at Pagtulong sa Kapwa (Empathy)", "Pagdama sa kalagayan ng kapwa at pag-abot ng tulong sa mga nangangailangan at may kapansanan.", "gmrc-g2-pakikipagkapuwa",
             [
                 ("Nakita mong nadapa ang isang batang mag-aaral at nagkalat ang kaniyang mga gamit. Ano ang dapat mong gawin?", ["Tulungan siyang tumayo, damayan siya, at tulungang pulutin ang mga gamit.", "Pagtawanan siya kasama ang mga kalaro.", "Kunin ang kaniyang baon at tumakbo.", "Magkunwaring walang nakita."], "Ang pag-alalay sa kapwa na nasa kagipitan ay tunay na pagmamalasakit."),
                 ("Paano mo maipadarama ang malasakit sa isang kaklaseng may kapansanan sa pandinig o paningin?", ["Maging maunawain, alalayan siya sa pag-aaral, at isama siya sa mga laro at kwentuhan.", "Ihiwalay siya at huwag kausapin.", "Gayahin at pagtawanan ang kaniyang kilos.", "Takutin siya sa madidilim na lugar."], "Lahat ng bata ay may karapatang maramdaman na sila ay kabilang at minamahal."),
                 ("Ano ang ibig sabihin ng 'Empathy' o Pakikipagkapwa-tao?", ["Ang kakayahang unawain at maramdaman ang pinagdadaanan ng ibang tao at kumilos para tumulong.", "Ang pag-iisip lamang sa sariling kapakanan.", "Ang pagbibigay ng tulong ngunit may kapalit na kabayaran.", "Ang paninisi sa iba kapag may problema."], "Ang empathy ay paglalagay ng sarili sa sitwasyon ng kapwa upang makatulong."),
                 ("Paano makatutulong ang pamilya sa mga nasalanta ng bagyo o kalamidad sa komunidad?", ["Mag-donate ng malilinis na damit, pagkain, at mag-alay ng panalangin at lakas sa relief operations.", "Itago ang labis na pagkain upang mabulok.", "Magbenta ng donasyon sa mataas na presyo.", "Pintasan ang kalagayan ng mga biktima."], "Ang pagbabahagi ng biyaya sa panahon ng sakuna ay pagpapakita ng malasakit.")
             ],
             "Ang pagmamalasakit ay ang bukal sa loob na pagtulong at pagdamay sa kapwa sa oras ng kanilang pangangailangan.",
             "Ilagay ang iyong sarili sa kalagayan ng iba bago magsalita o kumilos.",
             ("Magbahagi ng kwento kung paano ka tumulong sa isang kaibigan.", "Ilista ang 3 paraan ng pagtulong sa mga biktima ng kalamidad.", "Balikan ang pagtulong mula sa Baitang 2.")
            )
        ]),
        (4, [
            ("Mapanuring Pagpapasya at Pananagutan sa Bunga", "Pagsusuri sa sitwasyon bago magpasya at pag-ako sa pananagutan ng ginawang desisyon.", "gmrc-g3-pananagutan",
             [
                 ("Niyaya ka ng barkada na lumiban sa klase upang maglaro sa computer shop. Ano ang tamang pagpapasya?", ["Tanggihan ang aya nang mahinahon, pumasok sa klase, at unahin ang pag-aaral.", "Sumama agad upang hindi mapag-iwanan.", "Pumayag ngunit magsinungaling sa mga magulang.", "Utusan ang iba na sumama rin."], "Ang mapanuring desisyon ay nagtitimbang sa epekto ng kilos sa kinabukasan."),
                 ("Bago gumawa ng isang mabigat na desisyon, ano ang pinakamahalagang hakbang?", ["Pag-isipan ang maidudulot na bunga sa sarili, pamilya, at kapwa, at humingi ng payo sa nakatatanda.", "Gawin agad ang unang naisip nang walang pag-aalinlangan.", "Umasa lamang sa paboritong kulay o hula.", "Sundin ang sinasabi ng kahit sinong estranghero."], "Ang matalinong pasya ay pinag-iisipan at isinasangguni sa mga pinagkakatiwalaan."),
                 ("Nangako kang tatapusin ang pangkatang proyekto ngunit nakalimutan mo. Ano ang responsableng kilos?", ["Humingi ng paumanhin sa mga kagrupo, tapusin agad ang bahagi, at huwag nang ulitin.", "Isisi sa ibang miyembro ang pagkaantala.", "Huwag nang pumasok sa araw ng pasahan.", "Magtago sa ibang silid-aralan."], "Ang pananagutan ay pag-ako sa sariling pagkukulang at agarang pagwawasto."),
                 ("Bakit mahalagang pag-aralan ang sanhi at bunga bago magsagawa ng isang aksyon?", ["Upang maiwasan ang mga kapahamakan at pagsisisi sa huli.", "Upang maging pinakamatagal mag-isip sa klase.", "Upang walang magawang anumang desisyon.", "Wala itong kinalaman sa buhay."], "Ang bawat kilos ay may katumbas na pananagutan at epekto.")
             ],
             "Ang bawat pasya ay may kaakibat na pananagutan; mag-isip nang makailang ulit bago kumilos.",
             "Isaisip: Makabubuti ba ito sa akin at sa aking kapwa sa hinaharap?",
             ("Magtala ng 3 hakbang sa paggawa ng matalinong desisyon.", "Ibahagi ang isang sitwasyon kung saan nagpakita ka ng pananagutan.", "Balikan ang pagpapasya mula sa Baitang 3.")
            ),
            ("Pangangalaga sa Kalikasan at Likas na Yaman", "Pagsasagawa ng mga gawaing nagpapakita ng malasakit at pananagutan sa kapaligiran.", "gmrc-g3-paglilingkod-sa-pamayanan",
             [
                 ("Paano maipapakita ng mga mag-aaral ang prinsipyo ng '3Rs' (Reduce, Reuse, Recycle) sa paaralan?", ["Gumamit ng reusable na lalagyan ng tubig at baunan sa halip na single-use plastic.", "Magtapon ng plastik sa bawat sulok ng silid-aralan.", "Bumili ng bagong gamit araw-araw at itapon ang luma.", "Sunugin ang mga basurang papel sa likod-bahay."], "Ang pagbawas sa basurang plastik ay nagpoprotekta sa ating karagatan at hangin."),
                 ("Bakit itinuturing na moral na pananagutan ng tao ang pangangalaga sa mga hayop at halaman?", ["Sapagkat tayo ang tagapangalaga ng nilikha at nakasalalay ang ating buhay sa kalusugan ng ekolohiya.", "Upang maibenta natin ang lahat ng hayop.", "Dahil walang silbi ang mga hayop sa mundo.", "Upang maging sikat sa social media."], "Ang tao at kalikasan ay magkakaugnay sa isang malusog na tahanan."),
                 ("Ano ang tamang tugon kapag nakakita ka ng kapwa mag-aaral na nagkakalat sa tabing-ilog?", ["Pagsabihan siya nang mahinahon at ipaliwanag na ang basura ay nakalalason sa tubig at mga isda.", "Makipag-away at suntukin siya agad.", "Tularan siya at magtapon din ng sariling kalat.", "Magkunwaring walang nakita at lumayo."], "Ang magalang na pagwawasto sa kapwa ay nagtataguyod ng malasakit sa kapaligiran."),
                 ("Paano makatutulong ang pagtitipid sa paggamit ng kuryente at tubig sa ating kalikasan?", ["Nababaan nito ang konsumo sa mga likas na yaman at nababawasan ang greenhouse gas emissions.", "Wala itong maitutulong sa planeta.", "Nagpapadilim lamang ito sa bahay.", "Pinapataas nito ang bayarin sa kuryente."], "Ang bawat watt ng natipid na kuryente ay tulong sa pagpapabagal ng climate change.")
             ],
             "Ang kalikasan ay biyayang ipinagkatiwala sa atin; responsibilidad natin itong linisin, ingatan, at ipamana sa susunod na salinlahi.",
             "Maging tagapangalaga ng kalikasan: Bawasan ang plastik, magtipid sa tubig, at magtanim ng halaman.",
             ("Magsagawa ng simpleng paghihiwalay ng basura (Nabubulok, Di-nabubulok, Recyclable) sa bahay.", "Magtanim ng isang halamang gulay sa bakuran.", "Balikan ang kapaligiran mula sa Baitang 3.")
            ),
            ("Pagpapahalaga sa Karunungan at Pagiging Mapanuri", "Paghahangad sa patuloy na pagkatuto at pagsusuri sa katotohanan bago maniwala.", "gmrc-g3-mabuting-pagpapasiya",
             [
                 ("Nakatanggap ka ng text message na nanalo ka raw ng isang milyong piso kahit hindi ka sumali sa paligsahan. Ano ang matalinong tugon?", ["Huwag maniwala, huwag ibigay ang personal na impormasyon, at sabihin agad sa magulang dahil ito ay 'scam'.", "Ipadala agad ang lahat ng impormasyon ng pamilya at bank account.", "Magdiwang at ipamalita agad sa buong barangay.", "Umutang ng pera pambayad sa premyo."], "Ang mapanuring pag-iisip ay nagpoprotekta laban sa panloloko at cybercrime."),
                 ("Bakit mahalagang magbasa mula sa mapagkakatiwalaang aklat at opisyal na sanggunian kapag gumagawa ng takdang-aralin?", ["Upang matiyak na wasto, tumpak, at nakabatay sa katotohanan ang iyong mga impormasyon.", "Upang magmukhang mahaba ang sanggunian.", "Upang kopyahin ang gawa nang walang pagsusuri.", "Wala itong pinagkaiba sa kahit anong tsismis online."], "Ang karunungan ay nakasalalay sa paggamit ng lehitimo at napatunayang ebidensya."),
                 ("Paano maipapakita ang 'Growth Mindset' o bukas na kaisipan kapag nakatanggap ng mababang marka?", ["Tanggapin ang puna, alamin ang mga pagkukulang, magpatulong sa guro, at magsanay pa nang mabuti.", "Magalit sa guro at punitin ang papel.", "Sumuko na at huwag nang pumasok sa klase kailanman.", "Isisi sa mga kaklase ang kinalabasan."], "Ang pagkakamali ay pagkakataon upang matuto at maging mas mahusay."),
                 ("Ano ang ibig sabihin ng pagiging 'Critically Minded' o mapanuri sa impormasyon?", ["Ang pagsusuri sa pinagmulan ng balita, motibo ng sumulat, at katibayan ng datos bago ito paniwalaan at ibahagi.", "Ang pamimintas sa lahat ng bagay nang walang basehan.", "Ang paniniwala sa lahat ng nakikita sa internet.", "Ang pagtanggi sa lahat ng uri ng agham."], "Ang mapanuring mag-aaral ay naghahanap ng patunay at hindi nagpapadala sa sabi-sabi.")
             ],
             "Ang tunay na karunungan ay may kasamang kababaang-loob at mapanuring pag-iisip upang matukoy ang katotohanan.",
             "I-verify bago i-share: Siguraduhing totoo, kapaki-pakinabang, at hindi nakakasakit ang impormasyon.",
             ("Ilista ang 3 ligtas na paraan sa paggamit ng internet.", "Magbasa ng isang artikulo at suriin ang pinagmulan ng impormasyon.", "Balikan ang mapanuring pag-iisip mula sa Baitang 3.")
            ),
            ("Pambansang Pagkakaisa at Pagmamahal sa Bayan (Patriotism)", "Pagpapakita ng malasakit at pagmamalaki sa bansang Pilipinas sa isip, sa salita, at sa gawa.", "gmrc-g3-paglilingkod-sa-pamayanan",
             [
                 ("Paano maipapakita ng isang mag-aaral sa Grade 4 ang tunay na pagmamahal sa bayan?", ["Pag-aaral nang mabuti, paggalang sa watawat at batas, at pagbili at pagtangkilik sa sariling produktong Pilipino.", "Pangungutya sa sariling bansa sa harap ng dayuhan.", "Pagsira sa mga pampublikong pasilidad at parke.", "Pagtangging gumamit ng sariling wika."], "Ang disiplina, sipag, at pagtangkilik sa sariling atin ay pundasyon ng patriotismo."),
                 ("Ano ang nararapat gawin kapag narinig ang 'Lupang Hinirang' habang naglalakad sa kalsada?", ["Huminto, tumayo nang tuwid, ilagay ang kanang kamay sa kaliwang dibdib, at sumabay sa pag-awit nang buong paggalang.", "Magpatuloy sa pagtakbo at pagtawa.", "Takpan ang tainga at magtago.", "Makipagkwentuhan sa kasama."], "Ito ay pagpapakita ng mataas na paggalang sa pambansang awit at sa ating mga bayani."),
                 ("Bakit mahalagang tangkilikin ang mga produktong gawa ng ating mga lokal na magsasaka at manggagawa?", ["Upang suportahan ang kabuhayan ng kapwa Pilipino at palakasin ang pambansang ekonomiya.", "Upang maging mas mura ang bilihin sa ibang bansa.", "Upang mawalan ng hanapbuhay ang lokal na industriya.", "Wala itong maitutulong sa bansa."], "Ang pagtangkilik sa sariling atin ay nagbibigay ng trabaho at dangal sa mga manggagawa."),
                 ("Paano maipapakita ang diwa ng kabayanihan sa sariling komunidad nang hindi kailangang magbuwis ng buhay?", ["Pagtulong sa mga nangangailangan, pagiging tapat sa tungkulin, at pagtataguyod ng kapayapaan at kalinisan.", "Paghihintay ng digmaan bago kumilos.", "Panonood lamang habang naghihirap ang kapwa.", "Pang-aapi sa mga mahihina."], "Ang kabayanihan sa araw-araw ay ang paggawa ng kabutihan para sa bayan.")
             ],
             "Ang pagmamahal sa bayan ay naisasabuhay sa pamamagitan ng pagiging mabuting mamamayan, paggalang sa watawat, at pagmamalasakit sa kapwa Pilipino.",
             "Ipagmalaki ang ating kultura at tangkilikin ang produktong sariling atin.",
             ("Gumuhit ng poster na nagpapakita ng pagiging makabayan sa araw-araw.", "Awitin nang may buong paggalang at damdamin ang Lupang Hinirang.", "Balikan ang pambansang pagkakaisa mula sa Baitang 3.")
            )
        ])
    ])
}

KEYS = ['a', 'b', 'c', 'd']

def main():
    units = []
    assessments = []
    scripts = []
    ladders = []
    reports = []
    unit_number = 0

    for subject, (lang, grade_data) in SUBJECTS.items():
        for grade, unit_list in grade_data:
            actual_subject = "araling-panlipunan" if subject == "makabansa" and grade == 4 else subject
            for idx, (title, obj, prereq, mcqs, video_focus, hint_focus, ladder_steps) in enumerate(unit_list, start=1):
                uid = f"{subject}-g{grade}-future-{idx:02d}"
                unit_title = f"{title}"
                unit_obj = f"{obj}"
                mcqs = list(mcqs)

                # Correct the audit's high-risk civic/science claims directly.
                if uid == "makabansa-g4-future-02":
                    mcqs[0] = (
                        "Ano ang wastong paglalarawan sa Filipino at English ayon sa 1987 Konstitusyon?",
                        ["Filipino ang pambansang wika; Filipino at English ang mga opisyal na wika para sa komunikasyon at pagtuturo.",
                         "Filipino lamang ang opisyal na wika sa lahat ng paaralan at tanggapan.",
                         "English ang pambansang wika at Filipino ang wikang panrehiyon lamang.",
                         "Walang itinakdang pambansa o opisyal na wika ang Konstitusyon."],
                        "Itinatakda ng Artikulo XIV, Seksiyon 6–7 ang Filipino bilang pambansang wika at Filipino at English bilang mga opisyal na wika.")
                    mcqs[1] = (
                        "Ano ang sinasagisag ng walong sinag ng araw sa Pambansang Watawat?",
                        ["Ang unang walong lugar na inilagay sa batas militar noong 1896 dahil sa pag-aalsa laban sa Espanya.",
                         "Ang walong pinakamalaking pulo na bumuo sa unang pamahalaan.",
                         "Ang walong pangunahing wikang ginamit sa unang Konstitusyon.",
                         "Ang walong pangulong namuno bago nakamit ang kalayaan."],
                        "Ang mga sinag ay tumutukoy sa unang walong lugar—kabilang ang Maynila—na iniugnay sa pag-aalsa noong 1896.")
                elif uid == "makabansa-g4-future-03":
                    mcqs[1] = (
                        "Alin ang halimbawa ng pinagkukunan ng enerhiyang likas na napapalitan?",
                        ["Init mula sa ilalim ng lupa na muling napupunan ng mga prosesong likas.",
                         "Uling na nabuo sa napakahabang panahon at nauubos kapag ginagamit.",
                         "Diesel na pinoproseso mula sa limitadong imbak ng langis.",
                         "Gasolina na sinusunog at kailangang kunin mula sa ilalim ng lupa."],
                        "Ang enerhiyang napapalitan ay likas na napupunan sa makabuluhang panahon, bagaman maaari pa rin itong magkaroon ng epekto sa kapaligiran.")
                elif uid == "makabansa-g4-future-04":
                    mcqs[0] = (
                        "Aling proteksiyon ang malinaw na nakasaad sa Katipunan ng mga Karapatan ng 1987 Konstitusyon?",
                        ["Hindi maaaring alisan ang tao ng buhay, kalayaan, o ari-arian nang walang wastong proseso ng batas.",
                         "Maaaring kunin ang ari-arian ng sinuman kahit walang wastong proseso ng batas.",
                         "Maaaring ikulong ang tao dahil lamang sa hindi pagsang-ayon sa isang pinuno.",
                         "Maaaring alisin ang kalayaan ng tao nang walang paglilitis o batayan."],
                        "Sinasabi ng Artikulo III, Seksiyon 1 na walang taong aalisan ng buhay, kalayaan, o ari-arian nang walang wastong proseso ng batas.")

                if lang == "Filipino":
                    unit_title, unit_obj, mcqs, video_focus, hint_focus, ladder_steps = filipinize(
                        [unit_title, unit_obj, mcqs, video_focus, hint_focus, ladder_steps])

                # 1. Roadmap unit record
                units.append({
                    "id": uid,
                    "grade": grade,
                    "subject": actual_subject,
                    "subjectBalanceGroup": subject,
                    "language": lang,
                    "title": unit_title,
                    "objective": unit_obj,
                    "prerequisiteSkillId": prereq,
                    "remediationLadderId": f"{uid}-remediation",
                    "standards": {
                        "primary": {
                            "framework": "DepEd MATATAG",
                            "status": "sourced-framework-authored-alignment",
                            "reference": "DepEd MATATAG Curriculum Guides; exact competency-code confirmation required before release"
                        },
                        "crosswalks": [] if subject in {'filipino', 'makabansa', 'gmrc'} else [
                            {
                                "framework": "Singapore MOE / US standards",
                                "status": "authored-topic-level-crosswalk-not-release-claim",
                                "mapping": "Comparable topic only; verify against current official grade-level documents before release"
                            }
                        ]
                    },
                    "authorship": "Original Maxine's World draft; not copied from a source module",
                    "releaseStatus": "FUTURE_DRAFT_REQUIRES_EDUCATOR_REVIEW"
                })
                
                # 2. Four distinct MCQs with a diverse, corpus-balanced key pattern.
                key_pattern = KEY_PATTERNS[unit_number % len(KEY_PATTERNS)]
                example_prompt, _, example_explanation = mcqs[0]
                for q_idx, (prompt, raw_options, explanation) in enumerate(mcqs, start=1):
                    assigned_key = key_pattern[q_idx - 1]
                    raw_options = balance_option_lengths(raw_options, lang)
                    shift = KEYS.index(assigned_key)
                    rotated_opts = [None] * 4
                    for orig_idx in range(4):
                        rotated_opts[(orig_idx + shift) % 4] = raw_options[orig_idx]

                    assessments.append({
                        "id": f"{uid}-q{q_idx:02d}",
                        "unitId": uid,
                        "prompt": prompt,
                        "options": [{"id": k, "text": txt} for k, txt in zip(KEYS, rotated_opts)],
                        "correctOptionId": assigned_key,
                        "explanation": explanation,
                        "objective": unit_obj
                    })
                
                # 3. Worked 30–40 second Milo explainer and 5–8 second strategy cue.
                video_script = worked_script(lang, example_prompt, example_explanation, video_focus)
                hint_text = (f"Hint: {hint_focus}" if lang == "English"
                             else f"Pahiwatig: {hint_focus}")
                if lang == "Filipino":
                    video_script, hint_text = filipinize([video_script, hint_text])
                
                scripts.append({
                    "id": f"{uid}-video",
                    "unitId": uid,
                    "targetSeconds": 35,
                    "script": video_script,
                    "hint": {
                        "targetSeconds": 7,
                        "text": hint_text
                    }
                })
                
                # 4. Remediation ladder (3 structured, concept-specific scaffolding steps)
                ladders.append({
                    "id": f"{uid}-remediation",
                    "unitId": uid,
                    "prerequisiteSkillId": prereq,
                    "steps": [
                        {"level": 1, "action": ladder_steps[0]},
                        {"level": 2, "action": ladder_steps[1]},
                        {"level": 3, "action": ladder_steps[2]}
                    ]
                })
                
                # 5. Parent report data
                if lang == "English":
                    strength_p = f"What specific concept in {unit_title} did the learner demonstrate mastery of?"
                    support_p = f"Which foundational prerequisite or calculation step in {unit_title} needs additional practice?"
                else:
                    strength_p = f"Anong tiyak na konsepto sa {unit_title} ang naipamalas ng mag-aaral nang may kahusayan?"
                    support_p = f"Aling batayang kasanayan o hakbang sa {unit_title} ang nangangailangan pa ng karagdagang pagsasanay?"
                
                reports.append({
                    "unitId": uid,
                    "language": lang,
                    "metrics": ["itemsAttempted", "itemsCorrect", "hintUse", "remediationLevel"],
                    "strengthPrompt": strength_p,
                    "supportPrompt": support_p
                })
                unit_number += 1

    assert Counter(x["correctOptionId"] for x in assessments) == Counter({k: 48 for k in KEYS})
    docs = {
        "roadmap.json": {"schemaVersion": 1, "units": units},
        "assessment-bank.json": {"schemaVersion": 1, "items": assessments},
        "micro-lessons.json": {"schemaVersion": 1, "lessons": scripts},
        "remediation-ladders.json": {"schemaVersion": 1, "ladders": ladders},
        "parent-report-templates.json": {
            "schemaVersion": 1,
            "templates": [
                {
                    "id": "parent-weekly-en",
                    "language": "English",
                    "fields": ["learnerName", "week", "strengths", "skillsPracticed", "needsSupport", "nextSteps"],
                    "template": "{learnerName} practiced {skillsPracticed}. Strengths: {strengths}. Support needed: {needsSupport}. Next steps: {nextSteps}."
                },
                {
                    "id": "parent-weekly-fil",
                    "language": "Filipino",
                    "fields": ["pangalan", "linggo", "kalakasan", "kasanayangSinanay", "kailanganNgTulong", "susunodNaHakbang"],
                    "template": "Nagsanay si {pangalan} sa {kasanayangSinanay}. Kalakasan: {kalakasan}. Kailangan ng tulong: {kailanganNgTulong}. Susunod na hakbang: {susunodNaHakbang}."
                }
            ],
            "unitReportData": reports
        }
    }
    
    for filename, data in docs.items():
        (ROOT / filename).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    print(f"Generated {len(units)} units, {len(assessments)} MCQs, {len(scripts)} scripts, {len(ladders)} ladders, {len(reports)} report templates.")

if __name__ == "__main__":
    main()

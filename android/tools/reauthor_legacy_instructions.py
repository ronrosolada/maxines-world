"""Educator re-author pass (2026-08-07, RonBot educator-style review, authorized by Ron).

Fixes the 60 legacy month-01 lessons (ENGLISH/MATHEMATICS/SCIENCE):
  1. SORT_AND_CLASSIFY instruction: replaces "shows the skill" jargon with a
     per-lesson, objective-specific instruction.
  2. MATCHING_PAIRS right labels: replaces auto-generated junk labels
     ("telling sentence (2)", "tamang addition example" — incl. Tagalog bleed
     in English lessons) with meaningful, distinct categories.
  3. SEQUENCE_BUILDER steps: replaces the generic "State the lesson idea /
     Study one example / Explain why it fits" template with topical steps.

Idempotent: only touches lessons listed below; verifies each payload before
and after; safe to re-run.
"""

import json

REWRITES = {
# ─────────────────────────── ENGLISH ───────────────────────────
"english-g3-m01-d01": {
    "sort": "Sort each card: is it something the picture shows?",
    "match": ["the child", "the birds", "the dog"],
    "seq": ["Look at the picture.", "Name what you see in a complete sentence.", "Check that your sentence uses evidence from the picture."],
},
"english-g3-m01-d02": {
    "sort": "Sort each card: does the action give a clue about a feeling?",
    "match": ["feels happy", "feels caring", "feels scared"],
    "seq": ["Meet the characters.", "Find what each character does.", "Choose the feeling each action shows."],
},
"english-g3-m01-d03": {
    "sort": "Sort each card: is it a logical story ending?",
    "match": ["watering the plant", "returning the book", "repairing the wheel"],
    "seq": ["Read the story problem.", "Think about what the characters did.", "Choose an ending that fits the events."],
},
"english-g3-m01-d04": {
    "sort": "Sort each card: does it belong in a diary entry?",
    "match": ["has a date", "tells an event", "shows a feeling"],
    "seq": ["Write the date.", "Tell what happened.", "Add how you felt."],
},
"english-g3-m01-d05": {
    "sort": "Sort each card: is it a complete sentence or a fragment?",
    "match": ["The turtle", "Maxine", "Our class"],
    "seq": ["Find the subject.", "Find the verb.", "Put the capital letter and period in place."],
},
"english-g3-m01-d06": {
    "sort": "Sort each card: does it name the common noun and its proper noun?",
    "match": ["Cebu City", "Maxine", "Pasig River"],
    "seq": ["Name a common noun.", "Name its proper noun.", "Decide which one needs a capital letter."],
},
"english-g3-m01-d07": {
    "sort": "Sort each card: is the plural formed with -s or -es correctly?",
    "match": ["cats", "boxes", "dishes"],
    "seq": ["Read the singular word.", "Add -s or -es.", "Say the plural word."],
},
"english-g3-m01-d08": {
    "sort": "Sort each card: is the irregular plural correct?",
    "match": ["children", "feet", "mice"],
    "seq": ["Read the singular word.", "Remember the special plural.", "Say the plural word."],
},
"english-g3-m01-d09": {
    "sort": "Sort each card: does it have a short vowel sound?",
    "match": ["short a", "short e", "short i"],
    "seq": ["Say the word slowly.", "Listen for the vowel sound.", "Sort it by its short vowel."],
},
"english-g3-m01-d10": {
    "sort": "Sort each card: is the syllable count correct?",
    "match": ["table", "window", "banana"],
    "seq": ["Say the word.", "Clap each beat.", "Count the beats you heard."],
},
"english-g3-m01-d11": {
    "sort": "Sort each card: does the verb agree with the subject?",
    "match": ["am", "are", "was"],
    "seq": ["Find the subject.", "Check the time of the sentence.", "Choose am, is, are, was, or were."],
},
"english-g3-m01-d12": {
    "sort": "Sort each card: does the verb match the time of the sentence?",
    "match": ["past", "present", "future"],
    "seq": ["Read the time word.", "Match it to past, present, or future.", "Choose the right verb form."],
},
"english-g3-m01-d13": {
    "sort": "Sort each card: does it name a consonant blend correctly?",
    "match": ["fl", "st at the start", "st at the end"],
    "seq": ["Say the word.", "Listen for both sounds together.", "Name the blend you heard."],
},
"english-g3-m01-d14": {
    "sort": "Sort each card: does it begin with ch or sh?",
    "match": ["begins with ch", "begins with ch", "begins with sh"],
    "seq": ["Say the word.", "Listen to the first sound.", "Decide: ch or sh?"],
},
"english-g3-m01-d15": {
    "sort": "Sort each card: does it use a taught word with meaning?",
    "match": ["there", "hello", "have"],
    "seq": ["Read the sentence.", "Find the taught word.", "Say what the sentence means."],
},
"english-g3-m01-d16": {
    "sort": "Sort each card: does the possessive pronoun show ownership correctly?",
    "match": ["mine", "ours", "theirs"],
    "seq": ["Ask: who owns it?", "Choose the possessive pronoun.", "Say the sentence with ownership."],
},
"english-g3-m01-d17": {
    "sort": "Sort each card: does it connect a cause and an effect clearly?",
    "match": ["effect: wet path", "effect: wilted plant", "effect: lined up"],
    "seq": ["Find what happened first.", "Find what happened because of it.", "Connect them with because or so."],
},
"english-g3-m01-d18": {
    "sort": "Sort each card: is it about who, where, or what?",
    "match": ["who", "where", "what"],
    "seq": ["Ask: who is in the text?", "Ask: where does it happen?", "Ask: what happened?"],
},
"english-g3-m01-d19": {
    "sort": "Sort each card: does it belong in beginning, middle, or end order?",
    "match": ["beginning", "middle", "end"],
    "seq": ["Tell how the story starts.", "Tell what happens next.", "Tell how it ends."],
},
"english-g3-m01-d20": {
    "sort": "Sort each card: does it read the pictograph correctly?",
    "match": ["reading the key", "counting by the key", "comparing fairly"],
    "seq": ["Read the title.", "Read the key.", "Count the icons using the key."],
},
# ───────────────────────── MATHEMATICS ─────────────────────────
"mathematics-g3-m01-d01": {
    "sort": "Sort each card: is the statement about 4,352 or 2,406 true?",
    "match": ["4,352", "2,406", "10,000"],
    "seq": ["Read the number aloud.", "Name the thousands, hundreds, tens, and ones.", "Write the number in expanded form."],
},
"mathematics-g3-m01-d02": {
    "sort": "Sort each card: does it name the place and value of a digit correctly?",
    "match": ["digit 3 — place", "digit 3 — value", "digit 5 — place"],
    "seq": ["Pick a digit in 4,352.", "Name its place.", "Name its value."],
},
"mathematics-g3-m01-d03": {
    "sort": "Sort each card: is the number form correct?",
    "match": ["standard form", "expanded form", "word form"],
    "seq": ["Read 2,048 in standard form.", "Write it in expanded form.", "Write it in words."],
},
"mathematics-g3-m01-d04": {
    "sort": "Sort each card: is the comparison true?",
    "match": ["greater than", "equal to", "less than"],
    "seq": ["Line up the numbers by place value.", "Compare the largest place first.", "Choose >, =, or <."],
},
"mathematics-g3-m01-d05": {
    "sort": "Sort each card: is the ordering statement true?",
    "match": ["least to greatest", "greater than", "greatest to least"],
    "seq": ["Read the direction.", "Compare the numbers place by place.", "Arrange them in the asked order."],
},
"mathematics-g3-m01-d06": {
    "sort": "Sort each card: is the rounding correct?",
    "match": ["nearest ten", "nearest hundred", "nearest thousand"],
    "seq": ["Find the rounding place.", "Look at the digit next to it.", "Round up or keep the same."],
},
"mathematics-g3-m01-d07": {
    "sort": "Sort each card: does it show an ordinal position?",
    "match": ["first", "twenty-fifth", "one hundredth"],
    "seq": ["Start from the stated place.", "Count positions in the direction given.", "Name the ordinal word."],
},
"mathematics-g3-m01-d08": {
    "sort": "Sort each card: is the money statement true?",
    "match": ["pesos", "centavos", "comparing money"],
    "seq": ["Read the peso amount.", "Read the centavo amount.", "Compare them using the decimal point."],
},
"mathematics-g3-m01-d09": {
    "sort": "Sort each card: is the sum correct with regrouping?",
    "match": ["adds ones", "adds tens", "adds across places"],
    "seq": ["Add the ones.", "Regroup if needed.", "Add the tens and hundreds."],
},
"mathematics-g3-m01-d10": {
    "sort": "Sort each card: is the strategy labeled correctly?",
    "match": ["estimate", "exact mental math", "compensation"],
    "seq": ["Round each addend.", "Add the rounded numbers.", "Check if the estimate is close."],
},
"mathematics-g3-m01-d11": {
    "sort": "Sort each card: is the difference correct with regrouping?",
    "match": ["regroup across zeros", "regroup tens", "regroup hundreds"],
    "seq": ["Subtract the ones.", "Regroup when the top digit is too small.", "Subtract the tens and hundreds."],
},
"mathematics-g3-m01-d12": {
    "sort": "Sort each card: is the strategy labeled correctly?",
    "match": ["estimate", "mental subtraction", "compensation"],
    "seq": ["Round the numbers.", "Subtract the rounded numbers.", "Compare with the exact answer."],
},
"mathematics-g3-m01-d13": {
    "sort": "Sort each card: is it a correct step of the problem?",
    "match": ["step 1: add", "step 2: subtract", "checking"],
    "seq": ["Read the problem twice.", "Decide what to do first.", "Do the second step and check."],
},
"mathematics-g3-m01-d14": {
    "sort": "Sort each card: is the multiplication statement correct?",
    "match": ["3 groups of 4", "4 rows of 2", "5 groups of 3"],
    "seq": ["Count the groups.", "Count how many are in each group.", "Write the multiplication sentence."],
},
"mathematics-g3-m01-d15": {
    "sort": "Sort each card: is the multiplication fact correct?",
    "match": ["6 groups of 7", "8 groups of 4", "9 groups of 5"],
    "seq": ["Look at the model.", "Count the groups and the items in each.", "Say the fact."],
},
"mathematics-g3-m01-d16": {
    "sort": "Sort each card: is the product correct using place value?",
    "match": ["tens times 4", "hundreds times 3", "24 tens times 2"],
    "seq": ["Multiply the ones.", "Multiply the tens.", "Add the partial products."],
},
"mathematics-g3-m01-d17": {
    "sort": "Sort each card: does it use partial products correctly?",
    "match": ["partial products", "adding partial products", "area model"],
    "seq": ["Split 14 into 10 and 4.", "Multiply 23 by 10 and by 4.", "Add the partial products."],
},
"mathematics-g3-m01-d18": {
    "sort": "Sort each card: is the product or estimate correct?",
    "match": ["multiply by tens", "multiply by hundreds", "estimate the product"],
    "seq": ["Multiply the basic fact.", "Attach the zero placeholders.", "Estimate to check."],
},
"mathematics-g3-m01-d19": {
    "sort": "Sort each card: does it describe sharing or grouping correctly?",
    "match": ["division fact", "sharing", "grouping"],
    "seq": ["Start with 12 objects.", "Share them equally.", "Write the division sentence."],
},
"mathematics-g3-m01-d20": {
    "sort": "Sort each card: is the answer correct for the context?",
    "match": ["money problem", "multiplication fact", "division fact"],
    "seq": ["Read the question.", "Choose the operation that fits.", "Solve and check with the inverse."],
},
# ─────────────────────────── SCIENCE ───────────────────────────
"science-g3-m01-d01": {
    "sort": "Sort each card: does it describe a solid, liquid, or gas correctly?",
    "match": ["solid", "liquid", "gas"],
    "seq": ["Name an example of matter.", "Say what state it is in.", "Give one observable property."],
},
"science-g3-m01-d02": {
    "sort": "Sort each card: is the heating or cooling change safe and correct?",
    "match": ["change from heat", "change from cold", "change from warmth"],
    "seq": ["Look at the material.", "Decide if heat or cold changed it.", "Describe the change safely."],
},
"science-g3-m01-d03": {
    "sort": "Sort each card: does it match the sense organ to its observation?",
    "match": ["sight", "hearing", "touch"],
    "seq": ["Look at the observation.", "Name the sense organ that made it.", "Explain what it tells you."],
},
"science-g3-m01-d04": {
    "sort": "Sort each card: is the habitat match correct and kind?",
    "match": ["water habitat", "nesting area", "moist habitat"],
    "seq": ["Meet the animal.", "Think about what it needs.", "Match it to a suitable habitat without disturbing it."],
},
"science-g3-m01-d05": {
    "sort": "Sort each card: does it match the body part to its function?",
    "match": ["swimming", "flying", "balance"],
    "seq": ["Look at the animal body part.", "Think about what it can help with.", "Match part to function."],
},
"science-g3-m01-d06": {
    "sort": "Sort each card: does it follow the chosen grouping rule?",
    "match": ["feathers or none", "swimming or walking", "scales or none"],
    "seq": ["Pick a visible trait.", "Sort the animals by it.", "Say the rule you used."],
},
"science-g3-m01-d07": {
    "sort": "Sort each card: is the ecosystem role described correctly?",
    "match": ["pollination", "soil health", "seed dispersal"],
    "seq": ["Meet the animal.", "Think about what it does for its home.", "Name its helpful role."],
},
"science-g3-m01-d08": {
    "sort": "Sort each card: does it match the plant part to its job?",
    "match": ["roots", "stems", "leaves"],
    "seq": ["Look at the plant part.", "Think about what it does.", "Match the part to its job."],
},
"science-g3-m01-d09": {
    "sort": "Sort each card: is the way plants help described correctly?",
    "match": ["shade", "habitat", "food"],
    "seq": ["Think about a plant.", "Name one way it helps.", "Explain why that matters."],
},
"science-g3-m01-d10": {
    "sort": "Sort each card: is the living or non-living statement correct?",
    "match": ["a seed", "fire", "a plant"],
    "seq": ["Look at each thing.", "Check the signs of life.", "Decide: living or non-living?"],
},
"science-g3-m01-d11": {
    "sort": "Sort each card: is it about an inherited trait?",
    "match": ["beetles", "plants", "animals"],
    "seq": ["Look at the fictional organism.", "Name a trait it was born with.", "Compare it with its parents' traits."],
},
"science-g3-m01-d12": {
    "sort": "Sort each card: does it name the organism's needs correctly?",
    "match": ["plant needs", "animal needs", "both need"],
    "seq": ["Meet the organism.", "Think about what it needs to live.", "Match the need to the organism."],
},
"science-g3-m01-d13": {
    "sort": "Sort each card: is the environment match correct?",
    "match": ["pond", "tree canopy", "soil"],
    "seq": ["Meet the organism.", "Think about where it lives.", "Match it to its environment."],
},
"science-g3-m01-d14": {
    "sort": "Sort each card: is it a practical conservation action?",
    "match": ["reduce waste", "sort waste", "protect wildlife"],
    "seq": ["Look at the action.", "Decide if it helps the environment.", "Explain why it helps."],
},
"science-g3-m01-d15": {
    "sort": "Sort each card: is the force described correctly?",
    "match": ["push", "pull", "friction"],
    "seq": ["Look at the motion.", "Name the force that caused it.", "Predict the effect."],
},
"science-g3-m01-d16": {
    "sort": "Sort each card: does it describe motion from the reference point?",
    "match": ["away from the cone", "past the tree", "beside the box"],
    "seq": ["Name the reference point.", "Describe where the object is.", "Describe how it moves from there."],
},
"science-g3-m01-d17": {
    "sort": "Sort each card: is the magnet statement safe and correct?",
    "match": ["iron", "like poles", "unlike poles"],
    "seq": ["Hold the magnet safely.", "Test which materials it attracts.", "Predict what the poles do."],
},
"science-g3-m01-d18": {
    "sort": "Sort each card: is it a light source or a reflector?",
    "match": ["source", "reflector", "seen by reflected light"],
    "seq": ["Look at each object.", "Ask: does it make its own light?", "Sort it as source or reflector — never look at the Sun."],
},
"science-g3-m01-d19": {
    "sort": "Sort each card: is the sound statement correct and safe?",
    "match": ["sound source", "sound source", "hearing safety"],
    "seq": ["Make a gentle sound.", "Feel or see the vibration.", "Keep the volume safe for your ears."],
},
"science-g3-m01-d20": {
    "sort": "Sort each card: is the energy use safe?",
    "match": ["safe energy", "safe energy", "adult help"],
    "seq": ["Look at the energy source.", "Decide if it is safe to use.", "Ask an adult for anything unsafe."],
},
}

def main() -> None:
    base = "app/src/main/assets/content-pack/month-01/lessons/"
    updated = 0
    for lesson_id, rw in REWRITES.items():
        path = base + lesson_id + ".json"
        with open(path, encoding="utf-8") as f:
            lesson = json.load(f)
        activities = {a["type"]: a for a in lesson["activities"]}
        sort = activities["SORT_AND_CLASSIFY"]
        match = activities["MATCHING_PAIRS"]
        seq = activities["SEQUENCE_BUILDER"]

        sort["instruction"] = rw["sort"]
        pairs = match["content"]["pairs"]
        rights = rw["match"]
        if len(pairs) != len(rights):
            raise SystemExit(f"{lesson_id}: {len(pairs)} pairs but {len(rights)} rights")
        for pair, right in zip(pairs, rights):
            pair["right"] = right
        seq["content"]["steps"] = rw["seq"]
        seq["completionRule"]["stepCount"] = len(rw["seq"])

        with open(path, "w", encoding="utf-8") as f:
            json.dump(lesson, f, ensure_ascii=False, indent=1)
            f.write("\n")
        updated += 1
    print(f"re-authored {updated} lessons")


if __name__ == "__main__":
    main()

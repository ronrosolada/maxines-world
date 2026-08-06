#!/usr/bin/env python3
"""Repair confirmed educator-review defects in the bundled lesson pack.

This is intentionally narrower than the curation generator. It preserves
lesson-specific examples and changes only confirmed defects:

* stock English distractors and generic prompts in affected quarterly lessons;
* inverted explanations for negative assessment questions;
* non-discriminating matching labels and placeholder vocabulary definitions;
* unsafe Science distractors;
* Filipino learner-facing English bleed in legacy lessons;
* missing assessment item types; and
* deterministic answer-position bias in live MC and assessment options.

The script is idempotent. It does not set educator approval metadata; that is
an explicit final-accountability step after the review report is complete.
"""
from __future__ import annotations

import argparse
import copy
import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"

STOCK = {
    "an unrelated guess", "a random symbol", "a detail not in the lesson",
    "a different topic", "a correct example", "a related example",
    "another example", "a correct answer", "not in the lesson",
    "look directly at the sun", "look directly at the Sun",
}
GENERIC_RIGHTS = {
    "angkop na halimbawa", "malinaw na gamit", "tamang ideya", "mabuting asal",
    "paggalang sa kapwa", "observable evidence", "safe example", "connected idea",
    "shows equal groups", "names the product", "can use an array",
    "hanapbuhay sa baybayin", "hanapbuhay sa lupain", "angkop na disenyo sa kapaligiran",
    "shows the skill", "kaugnay ng ideya ng aralin", "a correct example",
    "a related example", "another example", "salitang may wastong baybay",
    "bahagi ng kultura ng komunidad", "sining ng pag-awit at tugtog",
    "living thing", "non-living thing", "a plant part", "observable property",
    "property of the material", "what the material does", "adds from ones to thousands",
    "keeps place values aligned", "trades 10 ones for 1 ten", "multiplies tens and ones",
    "pakikisubok nang may tapang", "pagsasanay upang mahusay",
    "humihingi ng tulong kapag kailangan",
}
GENERIC_DEFINITIONS = GENERIC_RIGHTS | {
    "a correct example", "a related example", "another example",
}

ENGLISH_WRONGS: dict[str, list[str]] = {
    "root": ["cat — dog", "book — read", "tree — leaf", "water — drink"],
    "capitalization": ["milo reads.", "where is the book", "look out", "we are ready"],
    "possessive": ["the book", "running quickly", "under the table", "blue and bright"],
    "experience": ["Ana plays outside.", "The dog ran home.", "The sun is bright.", "Birds fly."],
    "general": ["near the river", "after school", "the hungry cat", "blue and shiny"],
    "sentence": ["under the table", "blue and shiny", "after school", "the hungry cat"],
    "retell": ["a new unrelated character appears", "the opening setting only", "a repeated title", "an event from another story"],
    "cause_effect": ["two unrelated objects", "a color with no event", "a title only", "an effect with no cause"],
    "informational": ["a story about an imaginary dragon", "a shopping list", "a greeting only", "a personal dream"],
    "polite": ["Umalis ka na!", "Bilisan mo!", "Wala akong pakialam!", "Bigyan mo ako ngayon!"],
    "tense": ["tomorrow happened yesterday", "now means next year", "past means not yet", "future means already finished"],
    "sentence_sequence": ["a title only", "a final event placed first", "an unrelated detail", "a sentence with no time clue"],
    "sentence_parts": ["under the table", "running quickly", "blue and shiny", "after school"],
    "main_detail": ["a personal guess", "an unrelated fact", "a title from another text", "a detail not mentioned"],
    "picture": ["a smell outside the picture", "a secret thought", "a sound outside the picture", "a dream"],
    "compound": ["The bird sings.", "under the table", "a blue bag", "running fast"],
    "synonym": ["hot — table", "begin — window", "early — chair", "happy — river"],
    "diary": ["Ana sings.", "The dog ran.", "The sun is bright.", "Birds fly."],
    "be_verb": ["I is ready.", "She are kind.", "They am friends.", "You is learning."],
    "plural_es": ["box → boxs", "baby → babys", "dish → dishies", "class → classs"],
    "plural_s": ["box → boxs", "baby → babys", "child → childs", "mouse → mouses"],
    "vocabulary": ["a made-up spelling", "a picture with no word", "a sound with no meaning", "an unrelated object"],
    "nouns": ["happy", "quickly", "running", "under"],
    "blend": ["ship", "chair", "thin", "phone"],
    "digraph": ["stop", "green", "plant", "crab"],
    "syllable": ["calling every letter a syllable", "counting punctuation", "counting the picture", "counting spaces only"],
    "telling": ["Where is Milo?", "Please sit down!", "a blue bag", "running fast"],
    "graph": ["guess without counting", "change the key", "count the title", "use a different graph"],
    "vowels": ["cake — long a", "team — long e", "bike — long i", "home — long o"],
    "intonation": ["a question with a period", "a telling sentence with a question mark", "an exclamation with no punctuation", "a pause in the wrong place"],
}

ENGLISH_PROMPTS = {
    "root": "Which pair shows a base word and a related word?",
    "capitalization": "Which choice uses a capital letter and the correct end mark?",
    "possessive": "Which phrase shows who owns or is connected to something?",
    "experience": "Which sentence tells about a personal experience?",
    "general": "Which group of words expresses a complete idea?",
    "sentence": "Which group of words expresses a complete idea?",
    "retell": "Which ending follows the events of the story?",
    "cause_effect": "Which words show how the ideas are connected?",
    "informational": "Which choice is an informational text that teaches facts?",
    "polite": "Which expression fits the situation politely?",
    "tense": "Which sentence uses its time clue correctly?",
    "sentence_sequence": "Which choice shows events in the correct order?",
    "sentence_parts": "Which sentence shows who or what acts and what it does?",
    "main_detail": "Which detail supports the main idea or picture clue?",
    "picture": "Which detail can you see in the picture?",
    "compound": "Which sentence joins two complete ideas?",
    "synonym": "Which pair has the matching or opposite meanings asked for?",
    "diary": "Which sentence uses first-person words for a diary?",
    "be_verb": "Which sentence uses am, is, or are correctly?",
    "plural_es": "Which plural spelling follows the -es or -ies rule?",
    "plural_s": "Which plural noun correctly adds -s?",
    "vocabulary": "Which sentence uses the word meaningfully in context?",
    "nouns": "Which choice is a common or proper noun?",
    "blend": "Which word begins with a consonant blend?",
    "digraph": "Which word contains a common digraph?",
    "syllable": "Which answer counts the spoken parts of the word?",
    "telling": "Which choice is a telling sentence with a period?",
    "graph": "Which answer uses the picture-graph data?",
    "vowels": "Which word has the short vowel sound asked for?",
    "intonation": "Which sentence uses voice or punctuation to show its meaning?",
}

FILIPINO_PAIR_LABELS = {
    "guro—tao": "tao",
    "kalabaw—hayop": "hayop",
    "paaralan—lugar": "lugar",
    "asul na guhit—ilog ayon sa legend": "ilog na ipinakikita ng legend",
    "bituin—paaralan ayon sa legend": "paaralang ipinakikita ng legend",
    "n—hilaga": "hilaga",
    "una: kunin ang papel.": "unang hakbang",
    "ikalawa: gumuhit ng bilog.": "ikalawang hakbang",
    "huli: kulayan ang bilog.": "huling hakbang",
}
MAKABANSA_PAIR_LABELS = {
    "paggalang sa kapwa": "pagpapahalaga sa ugnayan",
    "pag-aalaga sa komunidad": "pagkilos para sa komunidad",
    "pagpapahalaga sa wika": "pagpapanatili ng wika",
    "paulit-ulit na ritmo": "ostinato",
    "salitang tugon sa awit": "call and response",
    "tunog ng palengke": "soundscape ng isang lugar",
    "pangingisda sa baybayin": "kabuhayan sa baybayin",
    "pagsasaka sa kapatagan": "kabuhayan sa kapatagan",
    "disenyong angkop sa klima": "disenyong tumutugon sa klima",
}
GMRC_PAIR_LABELS = {
    "nakikinig": "pakikinig nang may paggalang",
    "nagsasabi ng po at opo kung angkop": "magalang na pananalita",
    "humihingi ng pahintulot": "paggalang sa hangganan",
    "gumagalang sa paniniwala ng iba": "paggalang sa pagkakaiba",
    "humihingi ng gabay": "paghingi ng tulong",
}
SCIENCE_PAIR_LABELS = {
    "eyes—light and color": "nakikita ang liwanag at kulay",
    "ears—sound": "naririnig ang tunog",
    "skin—touch": "nakadarama ng haplos",
}


def clean_title(lesson: dict[str, Any]) -> str:
    return re.sub(r"\s*[·•]\s*M?\d+\s*D?\d+.*$", "", str(lesson.get("title", "aralin"))).strip()


def english_key(lesson: dict[str, Any]) -> str:
    objective = str(lesson.get("objective", "")).lower()
    if "complete idea" in objective:
        return "sentence"
    if "synonym" in objective or "antonym" in objective:
        return "synonym"
    if "characters" in objective and "story" in objective:
        return "story"
    if "ending" in objective:
        return "retell"
    try:
        import content_review  # type: ignore
        key = content_review.topic_key(lesson)
    except Exception:
        key = "general"
    return key if key in ENGLISH_WRONGS else "general"


def is_stock(value: Any) -> bool:
    return isinstance(value, str) and value.strip().lower() in {x.lower() for x in STOCK}


def rotate(values: list[Any], correct_index: int, target: int) -> tuple[list[Any], int]:
    if not values or correct_index < 0 or correct_index >= len(values):
        return values, correct_index
    target %= len(values)
    shift = (target - correct_index) % len(values)
    if shift == 0:
        return values, correct_index
    # Rotate right so the original correct value lands at target.
    return values[-shift:] + values[:-shift], target


def target_position(seed: str, option_count: int) -> int:
    return sum(ord(c) for c in seed) % option_count


def unique_choices(pool: list[str], count: int, excluded: set[str]) -> list[str]:
    out: list[str] = []
    for item in pool:
        if item in excluded or item in out:
            continue
        out.append(item)
        if len(out) == count:
            return out
    n = 1
    while len(out) < count:
        candidate = f"a different example of the skill ({n})"
        if candidate not in excluded and candidate not in out:
            out.append(candidate)
        n += 1
    return out


def current_examples(lesson: dict[str, Any]) -> list[str]:
    for activity in lesson.get("activities", []):
        if activity.get("type") == "HOTSPOT_IMAGE":
            examples = (activity.get("content") or {}).get("examples", [])
            if isinstance(examples, list):
                good = [x for x in examples if isinstance(x, str) and not is_stock(x)]
                if good:
                    return good
    return []


def pair_label(lesson: dict[str, Any], left: str, index: int, seen: set[str]) -> str:
    low = left.strip().lower()
    subject = str(lesson.get("subject", "")).lower()
    if low in FILIPINO_PAIR_LABELS:
        base = FILIPINO_PAIR_LABELS[low]
    elif low in MAKABANSA_PAIR_LABELS:
        base = MAKABANSA_PAIR_LABELS[low]
    elif low in GMRC_PAIR_LABELS:
        base = GMRC_PAIR_LABELS[low]
    elif low in SCIENCE_PAIR_LABELS:
        base = SCIENCE_PAIR_LABELS[low]
    elif "—" in left:
        base = left.split("—", 1)[1].strip()
        if base in {".", ""}:
            base = "kumpletong halimbawa"
    elif any(op in left for op in ("+", "−", "-", "×", "÷", "=")) and re.search(r"\d", left):
        if "×" in left or "x" in low:
            base = "tamang multiplication example"
        elif "÷" in left:
            base = "tamang division example"
        elif "+" in left:
            base = "tamang addition example"
        else:
            base = "tamang subtraction or estimate"
    elif subject in {"english", "enGLISH".lower()}:
        if left.endswith("?"):
            base = "question sentence"
        elif left.endswith("!"):
            base = "exclamation sentence"
        elif left.endswith("."):
            base = "telling sentence"
        else:
            base = "example with a useful detail"
    elif subject in {"filipino", "araling_panlipunan"}:
        if low.startswith("una:"):
            base = "unang hakbang"
        elif low.startswith("ikalawa:"):
            base = "ikalawang hakbang"
        elif low.startswith("huli:"):
            base = "huling hakbang"
        else:
            base = f"halimbawang nagpapakita ng kasanayan: {left}"
    else:
        base = f"evidence from the example: {left}"
    if base.lower() in seen:
        base = f"{base} ({index + 1})"
    seen.add(base.lower())
    return base


def generic_definition(lesson: dict[str, Any], term: str, index: int) -> str:
    subject = str(lesson.get("subject", "")).lower()
    low = term.lower()
    if "—" in term:
        suffix = term.split("—", 1)[1].strip()
        if suffix:
            return f"The example shows {suffix}."
    if term.endswith(".") or term.endswith("?") or term.endswith("!"):
        if term.endswith("?"):
            return "A question sentence that ends with a question mark."
        if term.endswith("!"):
            return "An exclamation sentence that ends with an exclamation mark."
        return "A telling sentence that gives information and ends with a period."
    if subject in {"filipino", "araling_panlipunan"}:
        return f"Halimbawa ito ng kasanayang pinag-aaralan sa aralin: {lesson.get('objective', '').lower()}"
    if subject == "gmrc":
        return f"Isang kilos na nagpapakita ng {lesson.get('objective', '').lower()}"
    if subject == "science":
        return f"Isang pamilyar na halimbawa upang obserbahan ang {lesson.get('objective', '').lower()}"
    if subject == "mathematics":
        return f"Halimbawa ng kasanayang pangmatematika: {lesson.get('objective', '').lower()}"
    return f"A word or phrase used to practice {lesson.get('objective', '').lower()}"


def repair_english_stock(lesson: dict[str, Any]) -> bool:
    if str(lesson.get("subject", "")).lower() != "english":
        return False
    blob = json.dumps(lesson, ensure_ascii=False).lower()
    if not any(marker.lower() in blob for marker in STOCK if marker.lower() not in {"look directly at the sun", "look directly at the sun"}):
        return False
    key = english_key(lesson)
    wrong = ENGLISH_WRONGS.get(key, ENGLISH_WRONGS["general"])
    examples = current_examples(lesson)
    changed = False
    # Replace stock sorting distractors with skill-specific distractors.
    for activity in lesson.get("activities", []):
        if activity.get("type") == "SORT_AND_CLASSIFY":
            content = activity.get("content") or {}
            old = content.get("doesNotFit", [])
            if any(is_stock(x) for x in old):
                content["doesNotFit"] = unique_choices(wrong, len(old), set(content.get("fits", [])))
                changed = True
        elif activity.get("type") == "MULTIPLE_CHOICE":
            content = activity.get("content") or {}
            options = list(content.get("options", []))
            idx = content.get("correctIndex", -1)
            if options and 0 <= idx < len(options) and any(is_stock(x) for x in options):
                correct = options[idx]
                if is_stock(correct):
                    correct = (examples or [wrong[0]])[0]
                replacements = unique_choices(wrong, len(options) - 1, {correct})
                new = []
                n = 0
                for i, value in enumerate(options):
                    if i == idx:
                        new.append(correct)
                    else:
                        new.append(replacements[n]); n += 1
                content["options"], content["correctIndex"] = rotate(new, idx, target_position(lesson["lessonId"] + "-activity", len(new)))
                changed = True
                prompt = ENGLISH_PROMPTS.get(key)
                if prompt:
                    activity["instruction"] = prompt
                    activity["prompt"] = prompt
                    changed = True
    # Fix generic vocabulary examples and matching labels in this lesson.
    for i, vocab in enumerate(lesson.get("vocabulary", [])):
        if is_stock(vocab.get("definition")):
            vocab["definition"] = generic_definition(lesson, str(vocab.get("term", "")), i)
            changed = True
    for activity in lesson.get("activities", []):
        if activity.get("type") == "MATCHING_PAIRS":
            seen: set[str] = set()
            for i, pair in enumerate((activity.get("content") or {}).get("pairs", [])):
                if str(pair.get("right", "")).strip().lower() in GENERIC_RIGHTS:
                    pair["right"] = pair_label(lesson, str(pair.get("left", "")), i, seen)
                    changed = True
    # Generic assessment prompts and stock options.
    prompt = ENGLISH_PROMPTS.get(key)
    for i, item in enumerate((lesson.get("assessment") or {}).get("items", [])):
        options = list(item.get("options", []))
        if options:
            ids = set(item.get("correctOptionIds", []))
            correct_indices = [j for j, o in enumerate(options) if o.get("id") in ids]
            if correct_indices and any(is_stock(o.get("text", "")) for o in options):
                ci = correct_indices[0]
                negative = bool(re.search(r"does not|not follow|except", str(item.get("prompt", "")).lower()))
                if negative:
                    correct = options[ci].get("text", "")
                    if not is_stock(correct):
                        candidates = unique_choices(wrong, 1, {correct})
                    else:
                        correct = wrong[0]
                        candidates = []
                    valid = unique_choices(examples or ["The learner applies the lesson skill."], len(options) - 1, {correct})
                    choices = [correct] + valid if negative else [correct] + candidates
                else:
                    correct = options[ci].get("text", "")
                    if is_stock(correct):
                        correct = (examples or [wrong[0]])[0]
                    candidates = unique_choices(wrong, len(options) - 1, {correct})
                    choices = [correct] + candidates
                # Keep stable IDs, but put the answer at a deterministic position.
                new_opts = []
                for j, text in enumerate(choices):
                    new_opts.append({"id": chr(ord("a") + j), "text": text})
                target = target_position(item.get("itemId", lesson["lessonId"] + str(i)), len(new_opts))
                new_opts, new_ci = rotate(new_opts, 0, target)
                item["options"] = new_opts
                item["correctOptionIds"] = [new_opts[new_ci]["id"]]
                changed = True
        if prompt and (str(item.get("prompt", "")).strip().lower() in {
            "which is a correct example?", "choose the best answer.",
            "which example belongs to", "which choice shows the skill in",
        } or any(marker in str(item.get("prompt", "")).lower() for marker in (
            "which example belongs to", "which choice shows the skill in",
            "what is one example from", "which situation matches", "which answer demonstrates",
        ))):
            item["prompt"] = prompt
            changed = True
    return changed


def repair_matching_and_vocab(lesson: dict[str, Any]) -> bool:
    changed = False
    for activity in lesson.get("activities", []):
        if activity.get("type") != "MATCHING_PAIRS":
            continue
        pairs = (activity.get("content") or {}).get("pairs", [])
        seen: set[str] = set()
        for i, pair in enumerate(pairs):
            right = str(pair.get("right", "")).strip().lower()
            if right in GENERIC_RIGHTS or right in seen:
                pair["right"] = pair_label(lesson, str(pair.get("left", "")), i, seen)
                changed = True
            else:
                seen.add(right)
    # Definitions copied from generic matching labels are not definitions.
    for i, vocab in enumerate(lesson.get("vocabulary", [])):
        if str(vocab.get("definition", "")).strip().lower() in GENERIC_DEFINITIONS:
            vocab["definition"] = generic_definition(lesson, str(vocab.get("term", "")), i)
            changed = True
    return changed


def repair_assessment_and_language(lesson: dict[str, Any]) -> bool:
    changed = False
    language = lesson.get("language")
    subject = str(lesson.get("subject", "")).lower()
    title = clean_title(lesson)
    negative_re = re.compile(r"does not|doesn't|not follow|which is not|alin ang hindi|hindi sumusunod", re.I)
    for i, item in enumerate((lesson.get("assessment") or {}).get("items", [])):
        if not item.get("type"):
            item["type"] = "MULTIPLE_CHOICE"
            changed = True
        options = item.get("options", [])
        if options:
            ids = set(item.get("correctOptionIds", []))
            ci = next((j for j, o in enumerate(options) if o.get("id") in ids), None)
            if ci is not None:
                target = target_position(item.get("itemId", lesson.get("lessonId", "")) + "-assessment", len(options))
                rotated, new_ci = rotate(list(options), ci, target)
                if rotated != options:
                    item["options"] = rotated
                    item["correctOptionIds"] = [rotated[new_ci]["id"]]
                    changed = True
        prompt = str(item.get("prompt", ""))
        if negative_re.search(prompt):
            correct = next((o.get("text", "") for o in item.get("options", []) if o.get("id") in set(item.get("correctOptionIds", []))), "")
            if language == "fil-PH":
                item["explanation"] = f"Ang tamang sagot ay “{correct}” dahil hindi ito sumusunod sa kasanayan ng aralin. Ang ibang pagpipilian ay nagpapakita ng kasanayang ito."
            else:
                item["explanation"] = f"The correct response is “{correct}” because it does not follow the lesson skill. The other choices show the skill."
            changed = True
        lower_prompt = prompt.strip().lower()
        if lower_prompt in {"which is a correct example?", "alin ang wastong halimbawa?", "alin ang tamang halimbawa?"} or "aling halimbawa ang dapat gamitin sa huling balik-aral" in lower_prompt:
            if language == "fil-PH":
                item["prompt"] = f"Aling halimbawa ang nagpapakita ng kasanayang “{title}”?"
            else:
                item["prompt"] = f"Which example shows the skill “{title}”?"
            changed = True
    if language == "fil-PH":
        def replace(value: Any) -> Any:
            nonlocal changed
            if isinstance(value, dict):
                return {k: replace(v) for k, v in value.items()}
            if isinstance(value, list):
                return [replace(v) for v in value]
            if isinstance(value, str):
                new = value
                new = new.replace("All choices are visible text and selectable with TalkBack.", "Makikita ang lahat ng pagpipilian at maaaring piliin gamit ang TalkBack.")
                new = new.replace("May listahan ng parehong halimbawa para sa keyboard at screen reader.", "May listahan ng parehong halimbawa bilang alternatibong paraan ng paggamit.")
                new = re.sub(r"\s+and\s+", " at ", new)
                if new != value:
                    changed = True
                return new
            return value
        repaired = replace(lesson)
        lesson.clear(); lesson.update(repaired)
    # Unsafe distractor: remove it even when it is intentionally incorrect.
    def scrub(value: Any) -> Any:
        nonlocal changed
        if isinstance(value, dict): return {k: scrub(v) for k, v in value.items()}
        if isinstance(value, list): return [scrub(v) for v in value]
        if isinstance(value, str) and value.lower() == "look directly at the sun":
            changed = True
            return "name a color in the room"
        return value
    repaired = scrub(lesson)
    lesson.clear(); lesson.update(repaired)
    return changed


def repair_generic_shell_copy(lesson: dict[str, Any]) -> bool:
    """Replace stock activity shells with one topic-specific learner sentence."""
    changed = False
    language = lesson.get("language")
    objective = str(lesson.get("objective", "")).strip().rstrip(".")
    if not objective:
        return False
    is_fil = language == "fil-PH"
    prompts = {
        "ANIMATED_EXPLANATION": (f"Pag-aralan ang kasanayan: {objective}.", f"Learn this skill: {objective}."),
        "HOTSPOT_IMAGE": (f"Suriin ang bawat halimbawa upang maisagawa ang: {objective}.", f"Look closely at each example to practice: {objective}."),
        "SORT_AND_CLASSIFY": (f"Ilagay ang bawat halimbawa sa pangkat habang isinasagawa ang: {objective}.", f"Sort each example while practicing: {objective}."),
        "MULTIPLE_CHOICE": (f"Piliin ang sagot na pinakamalinaw na nagpapakita ng: {objective}.", f"Choose the answer that best shows this skill: {objective}."),
        "MATCHING_PAIRS": (f"Itugma ang bawat halimbawa sa ideyang kaugnay ng: {objective}.", f"Match each example to the idea it shows about: {objective}."),
        "SEQUENCE_BUILDER": (f"Ayusin ang mga hakbang upang maisagawa ang: {objective}.", f"Put the steps in order to practice: {objective}."),
    }
    markers = (
        "Study the idea and listen to Milo", "Explore each example and find the important detail",
        "Sort each example into the correct group", "Choose the best answer",
        "Match the ideas that belong together", "Put the steps in the correct order",
        "Pag-aralan ang ideya at pakinggan ang paliwanag", "Suriin ang bawat halimbawa at hanapin ang mahalagang detalye",
        "Ilagay ang bawat halimbawa sa tamang pangkat", "Piliin ang pinakamainam na sagot",
        "Itugma ang magkakaugnay na ideya", "Ayusin ang mga hakbang ayon sa tamang pagkakasunod",
    )
    for activity in lesson.get("activities", []):
        kind = activity.get("type")
        if kind not in prompts:
            continue
        localized = prompts[kind][0 if is_fil else 1]
        for field in ("prompt", "instruction"):
            value = str(activity.get(field, ""))
            if any(marker in value for marker in markers):
                activity[field] = localized
                changed = True
    # Replace generic title-substituted assessment prompts with objective-grounded ones.
    template_markers = (
        "Which example belongs to", "Which choice shows the skill in", "What is one example from",
        "Which situation matches", "Which answer demonstrates", "Which example should be used in the final review",
        "Aling halimbawa ang kabilang sa", "Aling pagpipilian ang nagpapakita ng kasanayan sa",
        "Alin ang isang halimbawa ng", "Aling sitwasyon ang tumutugma sa", "Aling sagot ang nagpapakita ng",
        "Aling halimbawa ang dapat gamitin sa huling balik-aral",
    )
    en_variants = (
        f"Which example best shows this skill: {objective}?",
        f"Which choice applies this skill: {objective}?",
        f"Which situation shows this skill: {objective}?",
        f"Which answer best uses this skill: {objective}?",
        f"Which example gives evidence of this skill: {objective}?",
    )
    fil_variants = (
        f"Aling halimbawa ang pinakamalinaw na nagpapakita ng kasanayang ito: {objective}?",
        f"Aling pagpipilian ang gumagamit ng kasanayang ito: {objective}?",
        f"Aling sitwasyon ang nagpapakita ng kasanayang ito: {objective}?",
        f"Aling sagot ang wastong gumagamit ng kasanayang ito: {objective}?",
        f"Aling halimbawa ang nagbibigay ng patunay sa kasanayang ito: {objective}?",
    )
    variants = fil_variants if is_fil else en_variants
    for i, item in enumerate((lesson.get("assessment") or {}).get("items", [])):
        prompt = str(item.get("prompt", ""))
        if any(marker in prompt for marker in template_markers):
            item["prompt"] = variants[i % len(variants)]
            changed = True
    return changed


def repair_math_sequence_and_vocab(lesson: dict[str, Any]) -> bool:
    changed = False
    subject = str(lesson.get("subject", "")).lower()
    objective = str(lesson.get("objective", "")).lower()
    if subject == "mathematics":
        for activity in lesson.get("activities", []):
            if activity.get("type") != "SEQUENCE_BUILDER":
                continue
            steps = (activity.get("content") or {}).get("steps", [])
            if not any(step in {"Read the question", "Choose the operation or model", "Solve carefully", "Check whether the answer makes sense"} for step in steps):
                continue
            if any(word in objective for word in ("point", "line", "segment", "ray", "parallel", "perpendicular")):
                replacement = [
                    "Read the shape or line relationship in the question",
                    "Name the point, line, segment, ray, or relationship",
                    "Use the defining feature to describe it",
                    "Check that the description matches the diagram",
                ]
            elif "round" in objective:
                replacement = [
                    "Read the number and place value named in the question",
                    "Look at the digit immediately to the right",
                    "Round the named digit up or keep it the same",
                    "Check that the rounded number has the right place value",
                ]
            elif "area" in objective or "perimeter" in objective:
                replacement = [
                    "Read the shape and its measurements",
                    "Choose area or perimeter as the question asks",
                    "Use the matching formula or count the units",
                    "Check the unit and whether the answer fits the shape",
                ]
            else:
                replacement = [
                    f"Read the math question about {objective}",
                    "Choose the operation or model that matches the problem",
                    "Solve carefully using the given numbers",
                    "Check that the answer matches the question",
                ]
            (activity.get("content") or {})["steps"] = replacement
            changed = True
    if lesson.get("lessonId") in {"english-g3-q2-w04-d03", "english-g3-q3-w11-d02"}:
        lesson["vocabulary"] = [
            {"term": "compound sentence", "definition": "A sentence that joins two complete ideas."},
            {"term": "joining word", "definition": "A word such as and, but, or so that connects ideas."},
            {"term": "complete idea", "definition": "A group of words that tells who or what and what happened."},
        ]
        changed = True
    return changed


def repair_live_mc_bias(lesson: dict[str, Any]) -> bool:
    changed = False
    for activity in lesson.get("activities", []):
        if activity.get("type") != "MULTIPLE_CHOICE":
            continue
        content = activity.get("content") or {}
        options = list(content.get("options", []))
        idx = content.get("correctIndex", -1)
        if options and isinstance(idx, int) and 0 <= idx < len(options):
            target = target_position(activity.get("activityId", lesson.get("lessonId", "")), len(options))
            rotated, new_idx = rotate(options, idx, target)
            if rotated != options:
                content["options"] = rotated
                content["correctIndex"] = new_idx
                changed = True
    return changed


def repair_lesson(lesson: dict[str, Any]) -> tuple[dict[str, Any], bool]:
    original = copy.deepcopy(lesson)
    repair_english_stock(lesson)
    repair_matching_and_vocab(lesson)
    repair_generic_shell_copy(lesson)
    repair_math_sequence_and_vocab(lesson)
    repair_assessment_and_language(lesson)
    repair_live_mc_bias(lesson)
    return lesson, lesson != original


def remaining_defects(lesson: dict[str, Any]) -> list[str]:
    defects: list[str] = []
    blob = json.dumps(lesson, ensure_ascii=False).lower()
    for marker in STOCK:
        if marker.lower() in blob:
            defects.append(f"stock:{marker}")
    for activity in lesson.get("activities", []):
        if activity.get("type") == "MATCHING_PAIRS":
            rights = [str(p.get("right", "")).strip().lower() for p in (activity.get("content") or {}).get("pairs", [])]
            if len(rights) >= 2 and len(set(rights)) < len(rights):
                defects.append("matching:duplicate-right-label")
            if any(r in GENERIC_RIGHTS for r in rights):
                defects.append("matching:generic-right-label")
    for i in (lesson.get("assessment") or {}).get("items", []):
        if not i.get("type"):
            defects.append("assessment:missing-type")
        prompt = str(i.get("prompt", ""))
        expl = str(i.get("explanation", "")).lower()
        if re.search(r"does not|doesn't|not follow|which is not|alin ang hindi|hindi sumusunod", prompt, re.I) and re.search(r"because it follows|dahil sumusunod", expl):
            defects.append("assessment:negative-explanation")
    return defects


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    changed = 0
    defects: dict[str, list[str]] = {}
    for path in sorted(LESSONS.glob("*.json")):
        lesson = json.loads(path.read_text(encoding="utf-8"))
        fixed, did_change = repair_lesson(lesson)
        if did_change:
            changed += 1
            if not args.dry_run:
                path.write_text(json.dumps(fixed, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        found = remaining_defects(fixed)
        if found:
            defects[fixed.get("lessonId", path.stem)] = found
    print(json.dumps({"changed": changed, "lessons": len(list(LESSONS.glob('*.json'))), "remaining_count": len(defects), "remaining": defects}, ensure_ascii=False, indent=2))
    return 1 if args.check and defects else 0


if __name__ == "__main__":
    raise SystemExit(main())

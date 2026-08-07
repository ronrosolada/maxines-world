#!/usr/bin/env python3
"""Apply verified educator-review repairs to the Maxine's World lesson pack.

Default mode is a dry run. --apply writes only after the proposed canonical
and fallback packs pass the local integrity checks. This script intentionally
avoids approval metadata; educator sign-off remains a separate decision.
"""
from __future__ import annotations

import argparse
import copy
import json
import os
import re
import tempfile
from pathlib import Path
from typing import Any

REPO = Path(__file__).resolve().parents[1]
CANONICAL = REPO / "app/src/main/assets/content-pack/month-01/lessons"
LEGACY = REPO / "app/src/main/assets/content/ph-matatag/grade-3"

GENERIC_DEFINITION_MARKERS = (
    "a word or phrase used to practice",
    "halimbawa ito ng kasanayang pinag-aaralan",
    "isang kilos na nagpapakita ng",
    "may mahalagang papel sa komunidad",
    "bahagi ng pamumuhay",
)
LEGACY_APPROVAL_FIELDS = (
    "educatorValidated",
    "releaseStatus",
    "alignmentStatus",
    "contentReview",
    "reviewedAt",
    "reviewer",
    "reviewedBy",
)
GENERIC_CHILD_MARKERS = (
    "hula na walang batayan",
    "impormasyong walang kaugnayan",
    "pang-aalipusta sa kultura",
    "a random guess",
    "a mismatched unit",
    "an unrelated operation",
    "look directly at the sun",
)
UNSAFE_CHILD_TEXT = {
    "look directly at the sun": "count the books on a shelf",
    "stare at a bright light": "count the books on a shelf",
    "shine a laser at your eyes": "draw a picture",
    "put your ear right next to a loud speaker": "fold a towel",
    "scream close to your friend's ear": "read a book quietly",
    "play very loud music all day": "write in a notebook",
}

MAKABANSA_WRONGS = {
    "map": [
        "resipe ng paboritong pagkain",
        "kulay ng sapatos",
        "tunog ng kampana",
        "laro sa bakuran",
    ],
    "community": [
        "pagkuha ng gamit nang walang paalam",
        "pagtatago ng impormasyon sa lahat",
        "pagsulat ng lihim na password",
        "pag-iwan ng kalat sa daan",
    ],
    "culture": [
        "presyo ng isang laruan",
        "pangalan ng isang planeta",
        "numero sa isang resibo",
        "kulay ng isang kuwaderno",
    ],
    "environment": [
        "pagbilang ng mga lapis",
        "pagsagot ng bugtong tungkol sa hayop",
        "pagpili ng paboritong kulay",
        "pagsulat ng pangalan sa kuwaderno",
    ],
    "sound": [
        "pagsukat ng haba ng mesa",
        "pag-aayos ng mapa ng lalawigan",
        "pag-uuri ng mga halaman",
        "pagsulat ng resipe",
    ],
    "history": [
        "presyo ng meryenda",
        "laro sa palaruan",
        "kulay ng paboritong bag",
        "kuwento tungkol sa ibang planeta",
    ],
    "general": [
        "pagtiklop ng papel na walang kaugnayan",
        "pagbilang ng mga gamit sa mesa",
        "pagpili ng paboritong kulay",
        "pagsulat ng pangalan sa kuwaderno",
    ],
}
FILIPINO_WRONGS = {
    "magagalang": [
        "Umalis ka na!",
        "Bilisan mo!",
        "Wala akong pakialam!",
        "Bigyan mo ako!",
    ],
    "salitang-ugat": [
        "sulat → kumain",
        "laro → tumakbo",
        "basa → nagtanim",
        "tanim → bumasa",
    ],
    "general": [
        "Pahayag tungkol sa ibang paksa.",
        "Salitang walang kaugnayan sa aralin.",
        "Pangyayaring hindi kasama sa teksto.",
        "Sagot na hindi tumutugma sa tanong.",
    ],
}

TERM_DEFINITIONS = {
    # English signal words and reading terms.
    "first": "A signal word that tells which event comes before the others.",
    "because": "A signal word that introduces a reason.",
    "but": "A signal word that shows a contrast.",
    "pause at punctuation": "Stop briefly at a punctuation mark when reading aloud.",
    "read each word accurately": "Say each word correctly while reading.",
    "group words into phrases": "Read words together in meaningful groups.",
    "what happened": "The event that took place in a personal experience.",
    "where it happened": "The place where a personal experience took place.",
    "how i felt": "The feeling a writer experienced during an event.",
    "story": "A text that tells events with characters or a situation.",
    "instructions": "A text that gives steps for completing a task.",
    "informational text": "A text that gives facts about a topic.",
    "topic: mangrove trees": "The subject of the informational text is mangrove trees.",
    "fact: roots hold soil": "A fact explaining that mangrove roots help hold soil in place.",
    "fact: trees provide shelter": "A fact explaining that trees can provide shelter for living things.",
    # Filipino.
    "respeto": "Paggalang sa kapwa at sa kanilang damdamin.",
    "pagbati": "Magalang na pananalitang ginagamit sa pagbati sa ibang tao.",
    "pananalita": "Mga salitang ginagamit upang maipahayag ang iniisip o damdamin.",
    # GMRC.
    "sumubok ng bagong gawain": "Isang paraan ng pagkilala sa sariling lakas habang natututo.",
    "nagsanay": "Paulit-ulit na pagsisikap upang mapaunlad ang kakayahan.",
    "humingi ng gabay": "Paghingi ng tulong upang matuto nang ligtas at maayos.",
    "nakikinig": "Pagbibigay-pansin sa nagsasalita bago tumugon.",
    "nagsasabi ng po at opo kung angkop": "Magalang na pananalitang ginagamit ayon sa kausap at sitwasyon.",
    "humihingi ng pahintulot": "Pagtatanong muna bago gumamit o gumawa ng isang bagay.",
    "tinatapos ang gawain": "Pagtupad sa sinimulang gawain nang maayos.",
    "sumusunod sa napagkasunduan": "Pagtupad sa tuntuning napagkasunduan ng pangkat.",
    "gumagalang sa paniniwala ng iba": "Pagrespeto sa sariling paniniwala at sa paniniwala ng iba.",
    "nagtatanong kung ayos lang ang kaibigan": "Pag-aalala sa kaibigan sa paraang ligtas at kusang-loob.",
    "nagtatapon sa tamang basurahan": "Pagpapanatiling malinis ng kapaligiran sa pamamagitan ng wastong pagtatapon.",
    # Makabansa.
    "barangay hall": "Isang gusali kung saan nagbibigay ng serbisyo ang barangay.",
    "pamilihan": "Isang lugar kung saan bumibili at nagbebenta ng mga kailangan.",
    "paaralan": "Lugar kung saan nag-aaral at natututo ang mga bata.",
    "mga kuwento ng nakatatanda": "Salaysay mula sa nakatatanda tungkol sa mga pangyayari sa nakaraan.",
    "pangingisda sa baybayin": "Kabuhayang umaasa sa paghuli ng isda malapit sa dagat.",
    "pagsasaka sa kapatagan": "Kabuhayang umaasa sa pagtatanim sa patag na lupa.",
    "disenyong angkop sa klima": "Disenyong isinasaalang-alang ang init, ulan, o hangin sa lugar.",
    "paggalang sa kapwa": "Pagpapakita ng maayos na pakikitungo sa ibang tao.",
    "pag-aalaga sa komunidad": "Pagkilos upang mapanatiling ligtas at maayos ang lugar.",
    "pagpapahalaga sa wika": "Paggamit at pag-iingat sa wikang bahagi ng kultura.",
    "paulit-ulit na ritmo": "Ritmong inuulit habang umaawit o tumutugtog.",
    "salitang tugon sa awit": "Bahaging sinasagot ng pangkat sa isang awit.",
    "tunog ng palengke": "Mga tunog na naririnig sa isang pamilihan.",
}


def subject(lesson: dict[str, Any]) -> str:
    return str(lesson.get("subject", "")).lower()


def objective(lesson: dict[str, Any]) -> str:
    return str(lesson.get("objective", "")).strip()


def has_marker(value: Any, markers: tuple[str, ...]) -> bool:
    if not isinstance(value, str):
        return False
    low = value.lower()
    return any(marker in low for marker in markers)


def walk_strings(value: Any):
    if isinstance(value, dict):
        for child in value.values():
            yield from walk_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_strings(child)
    elif isinstance(value, str):
        yield value


def topic_key(lesson: dict[str, Any]) -> str:
    text = (objective(lesson) + " " + str(lesson.get("title", ""))).lower()
    if any(token in text for token in ("mapa", "lokasyon", "direksiyon", "lalawigan")):
        return "map"
    if any(token in text for token in ("kasaysayan", "nakaraan", "pinagmulan")):
        return "history"
    if any(token in text for token in ("komunidad", "serbisyo", "tungkulin")):
        return "community"
    if any(token in text for token in ("kultura", "pagiging pilipino", "katangian", "sining")):
        return "culture"
    if any(token in text for token in ("kapaligiran", "likas", "hanapbuhay", "pagkain", "tahanan")):
        return "environment"
    if any(token in text for token in ("ritmo", "tunog", "awit", "musika")):
        return "sound"
    return "general"


def wrong_pool(lesson: dict[str, Any], count: int) -> list[str]:
    subj = subject(lesson)
    text = objective(lesson).lower()
    if subj == "makabansa":
        pool = MAKABANSA_WRONGS[topic_key(lesson)]
    elif subj == "filipino":
        if "magalang" in text or "pagbati" in text:
            pool = FILIPINO_WRONGS["magagalang"]
        elif "ugat" in text:
            pool = FILIPINO_WRONGS["salitang-ugat"]
        else:
            pool = FILIPINO_WRONGS["general"]
    else:
        pool = [
            "Isang halimbawa mula sa ibang paksa.",
            "Isang sagot na hindi tumutugma sa tanong.",
            "Isang pangyayaring wala sa teksto.",
            "Isang paraan na hindi angkop sa sitwasyon.",
        ]
    return pool[:count]


def short_topic(lesson: dict[str, Any]) -> str:
    text = objective(lesson).lower()
    subj = subject(lesson)
    if subj == "english":
        choices = (
            ("capital", "capital letters and punctuation"),
            ("root", "base words"),
            ("synonym", "word meanings"),
            ("antonym", "opposite meanings"),
            ("punctuation", "punctuation"),
            ("sentence", "complete sentences"),
            ("personal experience", "personal experiences"),
            ("text type", "text types"),
            ("sequence", "event order"),
            ("cause", "cause and effect"),
            ("order, reason, contrast", "order, reason, and contrast words"),
            ("vocabulary", "new words"),
        )
        for needle, label in choices:
            if needle in text:
                return label
        return "reading and writing"
    if subj == "filipino":
        choices = (
            ("magalang", "magalang na pananalita"),
            ("ugat", "salitang-ugat"),
            ("diksyunaryo", "paggamit ng diksyunaryo"),
            ("baybay", "wastong pagbabaybay"),
            ("panghalip", "panghalip"),
            ("kuwento", "mga bahagi ng kuwento"),
            ("bantas", "malaking titik at bantas"),
        )
        for needle, label in choices:
            if needle in text:
                return label
        return "kasanayan sa Filipino"
    if subj == "mathematics":
        choices = (
            ("round", "rounding numbers"),
            ("multiply", "multiplication"),
            ("divide", "division"),
            ("fraction", "fractions"),
            ("area", "area"),
            ("perimeter", "perimeter"),
            ("order numbers", "number order"),
            ("place value", "place value"),
        )
        for needle, label in choices:
            if needle in text:
                return label
        return "number skills"
    if subj == "science":
        if "light" in text or "sound" in text:
            return "light and sound safety"
        if "living" in text:
            return "living things"
        if "material" in text:
            return "material properties"
        return "science ideas"
    if subj == "gmrc":
        return "mabuting pasiya at paggalang"
    if subj == "makabansa":
        return {
            "map": "maps and places",
            "community": "community life",
            "culture": "culture and identity",
            "environment": "environment and culture",
            "sound": "music and sound",
            "history": "community history",
        }.get(topic_key(lesson), "community life")
    return "today's skill"


def trim_intro(value: str, lesson: dict[str, Any]) -> str:
    if len(value) <= 180:
        return value
    for marker in ("Ready to explore?", "Handa ka na bang sumubok?", "Handa ka na ba?"):
        if marker in value:
            value = value[: value.index(marker) + len(marker)]
            break
    sentences = re.split(r"(?<=[.!?])\s+", value)
    result = ""
    for sentence in sentences:
        candidate = f"{result} {sentence}".strip()
        if result and len(candidate) > 180:
            break
        result = candidate
        if len(result) >= 180:
            break
    if len(result) > 180:
        result = result[:177].rstrip() + "..."
    return result


def short_instruction(lesson: dict[str, Any], activity: dict[str, Any]) -> str:
    topic = short_topic(lesson)
    lang = str(lesson.get("language", "")) == "fil-PH"
    typ = str(activity.get("type", ""))
    if lang:
        templates = {
            "ANIMATED_EXPLANATION": f"Pakinggan ang paliwanag tungkol sa {topic}.",
            "HOTSPOT_IMAGE": f"Tuklasin ang mga halimbawa ng {topic}.",
            "SORT_AND_CLASSIFY": f"Pagbukud-bukurin ang mga halimbawa ng {topic}.",
            "MULTIPLE_CHOICE": f"Piliin ang tamang halimbawa ng {topic}.",
            "MATCHING_PAIRS": f"Itapat ang mga halimbawa ng {topic}.",
            "SEQUENCE_BUILDER": f"Ayusin ang mga hakbang sa {topic}.",
        }
    else:
        templates = {
            "ANIMATED_EXPLANATION": f"Listen to the explanation about {topic}.",
            "HOTSPOT_IMAGE": f"Find examples of {topic}.",
            "SORT_AND_CLASSIFY": f"Sort the examples of {topic}.",
            "MULTIPLE_CHOICE": f"Choose the example of {topic}.",
            "MATCHING_PAIRS": f"Match the examples about {topic}.",
            "SEQUENCE_BUILDER": f"Put the {topic} steps in order.",
        }
    return templates.get(typ, f"Practice {topic}.")


def specific_definition(lesson: dict[str, Any], term: str, index: int) -> str:
    key = term.strip().lower()
    if key in TERM_DEFINITIONS:
        return TERM_DEFINITIONS[key]
    subj = subject(lesson)
    if subj == "makabansa":
        return f"Isang halimbawang may kaugnayan sa {term}."
    if subj == "gmrc":
        return f"Isang kilos o pagpapahalagang kaugnay ng {term}."
    if subj == "filipino":
        return f"Isang salitang ginagamit upang magsanay sa {term}."
    if subj == "english":
        return f"An example that practices {term}."
    if subj == "science":
        return f"An example used to observe {term}."
    if subj == "mathematics":
        return f"An example used to practice {term}."
    return f"Halimbawa na kaugnay ng {term}."


def repair_vocabulary(lesson: dict[str, Any]) -> bool:
    vocab = lesson.get("vocabulary")
    if not isinstance(vocab, list) or not vocab:
        return False
    definitions = [str(item.get("definition", "")) for item in vocab]
    duplicate = len(set(definitions)) != len(definitions)
    flagged = any(has_marker(value, GENERIC_DEFINITION_MARKERS) for value in definitions)
    if not duplicate and not flagged:
        return False
    changed = False
    used: set[str] = set()
    for index, item in enumerate(vocab):
        term = str(item.get("term", "")).strip()
        new_definition = specific_definition(lesson, term, index)
        if new_definition in used:
            new_definition = f"{new_definition} Halimbawa: {term}."
        used.add(new_definition)
        if item.get("definition") != new_definition:
            item["definition"] = new_definition
            changed = True
    return changed


def repair_makabansa(lesson: dict[str, Any]) -> bool:
    if subject(lesson) != "makabansa":
        return False
    changed = False
    pool = MAKABANSA_WRONGS[topic_key(lesson)]
    for activity in lesson.get("activities", []):
        content = activity.get("content")
        if activity.get("type") == "SORT_AND_CLASSIFY" and isinstance(content, dict):
            fits = content.get("fits", [])
            wrong = content.get("doesNotFit", [])
            if (set(fits) & set(wrong)) or any(has_marker(x, GENERIC_CHILD_MARKERS) for x in wrong):
                new_wrong = pool[: len(wrong) or 4]
                if wrong != new_wrong:
                    content["doesNotFit"] = new_wrong
                    changed = True
        elif activity.get("type") == "MULTIPLE_CHOICE" and isinstance(content, dict):
            options = content.get("options", [])
            ci = content.get("correctIndex")
            if isinstance(ci, int) and 0 <= ci < len(options) and any(has_marker(x, GENERIC_CHILD_MARKERS) for x in options):
                new_options = list(options)
                candidates = iter(pool)
                for index in range(len(new_options)):
                    if index == ci:
                        continue
                    new_options[index] = next(candidates, f"Isang sagot na hindi tumutugma sa {short_topic(lesson)}.")
                if new_options != options:
                    content["options"] = new_options
                    changed = True
        elif activity.get("type") == "MATCHING_PAIRS" and isinstance(content, dict):
            pair_rights = {
                "barangay hall": "serbisyong ibinibigay ng barangay",
                "pamilihan": "lugar ng bilihan at bentahan",
            }
            for pair in content.get("pairs", []):
                left = str(pair.get("left", "")).lower()
                right = str(pair.get("right", ""))
                if has_marker(right, GENERIC_DEFINITION_MARKERS) and left in pair_rights:
                    pair["right"] = pair_rights[left]
                    changed = True
    items = lesson.get("assessment", {}).get("items", [])
    for item in items:
        options = item.get("options", [])
        correct = set(item.get("correctOptionIds", []))
        if any(has_marker(o.get("text"), GENERIC_CHILD_MARKERS) for o in options):
            new_options = copy.deepcopy(options)
            candidates = iter(pool)
            for option in new_options:
                if option.get("id") in correct:
                    continue
                option["text"] = next(candidates, f"Isang sagot na hindi tumutugma sa {short_topic(lesson)}.")
            if new_options != options:
                item["options"] = new_options
                changed = True
    return changed


def repair_filipino(lesson: dict[str, Any]) -> bool:
    if subject(lesson) != "filipino":
        return False
    changed = False
    text = objective(lesson).lower()
    for activity in lesson.get("activities", []):
        if activity.get("type") != "SORT_AND_CLASSIFY":
            continue
        content = activity.get("content") or {}
        fits = list(content.get("fits", []))
        wrong = list(content.get("doesNotFit", []))
        if "magalang" in text or "pagbati" in text:
            new_wrong = FILIPINO_WRONGS["magagalang"][: len(wrong) or 4]
        elif "ugat" in text:
            new_wrong = FILIPINO_WRONGS["salitang-ugat"][: len(wrong) or 4]
        elif set(fits) & set(wrong):
            new_wrong = FILIPINO_WRONGS["general"][: len(wrong) or 4]
        else:
            continue
        if new_wrong != wrong:
            content["doesNotFit"] = new_wrong
            changed = True
    if lesson.get("lessonId") == "filipino-g3-q1-w02-d04":
        for item in lesson.get("assessment", {}).get("items", []):
            if "Salamat po" not in str(item.get("prompt", "")):
                continue
            for option in item.get("options", []):
                if option.get("text") == "Salamat po.":
                    option["text"] = "Walang anuman po."
                    item["correctOptionIds"] = [option.get("id")]
                    changed = True
                    break
    return changed


def repair_makabansa_similarity(lesson: dict[str, Any]) -> bool:
    lid = lesson.get("lessonId")
    variants = {
        "makabansa-g3-q4-w05-d04": {
            "intro": "May bagong misyon si Milo! 🐱✨ Sa mababang lugar at baybayin, umaayon ang pagkain at tahanan sa klima at likas na yaman. Handa ka na bang sumubok?",
            "focus": "mababang lugar at baybayin",
            "fits": [
                "pangingisda sa baybayin",
                "pagkain mula sa ani ng palay",
                "bahay na may maluwang na bintana sa mainit na lugar",
                "bangkang ginagamit sa paglalakbay sa ilog",
            ],
            "wrong": [
                "pagbibilang ng lapis sa silid-aralan",
                "pagpili ng paboritong kulay",
                "pagsulat ng lihim na password",
                "paglalaro ng board game sa bahay",
            ],
            "pairs": [
                {"left": "pangingisda sa baybayin", "right": "kabuhayan mula sa dagat"},
                {"left": "pagkain mula sa ani ng palay", "right": "pagkaing mula sa ani sa kapatagan"},
                {"left": "bahay na may maluwang na bintana sa mainit na lugar", "right": "disenyong nagpapalamig sa bahay"},
            ],
            "steps": [
                "Tingnan ang anyong lupa o tubig",
                "Hanapin ang likas na yaman",
                "Iugnay ito sa pagkain o tahanan",
                "Ipaliwanag ang ugnayan sa kultura",
            ],
            "prompts": [
                "Aling halimbawa ang nagpapakita ng kulturang naaangkop sa baybayin?",
                "Aling gawain ang naiimpluwensiyahan ng lugar na tinitirhan?",
                "Aling tahanan ang idinisenyo ayon sa mainit na klima?",
                "Aling pagkain ang maaaring magmula sa ani ng kapatagan?",
                "Aling halimbawa ang nag-uugnay sa kapaligiran at kultura?",
            ],
        },
        "makabansa-g3-q4-w07-d03": {
            "intro": "May bagong misyon si Milo! 🐱✨ Sa kabundukan, umaayon ang gawain, tahanan, at sining sa taas ng lugar at sa panahon. Handa ka na bang sumubok?",
            "focus": "kabundukan",
            "fits": [
                "pagtatanim ng gulay sa kabundukan",
                "bahay na may mataas na sahig sa lugar na binabaha",
                "hinabing banig mula sa lokal na halaman",
                "pagkaing gawa sa kamote mula sa taniman",
            ],
            "wrong": [
                "pagbibilang ng barya sa pitaka",
                "pagsulat ng pangalan sa kuwaderno",
                "paglalaro ng board game sa loob ng bahay",
                "pagbasa ng kuwento tungkol sa ibang planeta",
            ],
            "pairs": [
                {"left": "pagtatanim ng gulay sa kabundukan", "right": "kabuhayan sa mataas na lugar"},
                {"left": "bahay na may mataas na sahig sa lugar na binabaha", "right": "disenyong panlaban sa baha"},
                {"left": "hinabing banig mula sa lokal na halaman", "right": "sining mula sa likas na materyal"},
            ],
            "steps": [
                "Tingnan ang anyong lupa",
                "Pumili ng halimbawa sa komunidad",
                "Iugnay ito sa gawain o sining",
                "Ipaliwanag ang ugnayan sa kultura",
            ],
            "prompts": [
                "Aling halimbawa ang nagpapakita ng kulturang naaangkop sa kabundukan?",
                "Aling gawain ang gumagamit ng yaman sa mataas na lugar?",
                "Aling tahanan ang may disenyong tumutugon sa baha?",
                "Aling sining ang gumagamit ng materyal mula sa paligid?",
                "Aling halimbawa ang nag-uugnay sa kapaligiran at kultura?",
            ],
        },
    }
    variant = variants.get(str(lid))
    if not variant:
        return False
    changed = False
    if lesson.get("introduction") != variant["intro"]:
        lesson["introduction"] = variant["intro"]
        changed = True
    activities = lesson.get("activities", [])
    if len(activities) >= 6:
        hotspot = activities[1].get("content")
        if isinstance(hotspot, dict) and hotspot.get("examples") != variant["fits"]:
            hotspot["examples"] = list(variant["fits"])
            changed = True
        sort_content = activities[2].get("content")
        if isinstance(sort_content, dict):
            for key, value in (("fits", variant["fits"]), ("doesNotFit", variant["wrong"])):
                if sort_content.get(key) != value:
                    sort_content[key] = list(value)
                    changed = True
        mc_content = activities[3].get("content")
        if isinstance(mc_content, dict):
            options = [variant["wrong"][0], variant["fits"][0], variant["wrong"][1], variant["wrong"][2]]
            if str(lid).endswith("w05-d04"):
                options = [variant["wrong"][0], variant["fits"][0], variant["wrong"][1], variant["wrong"][2]]
                correct_index = 1
            else:
                options = [variant["wrong"][0], variant["wrong"][1], variant["fits"][0], variant["wrong"][2]]
                correct_index = 2
            if mc_content.get("options") != options or mc_content.get("correctIndex") != correct_index:
                mc_content["options"] = options
                mc_content["correctIndex"] = correct_index
                changed = True
        matching = activities[4].get("content")
        if isinstance(matching, dict) and matching.get("pairs") != variant["pairs"]:
            matching["pairs"] = copy.deepcopy(variant["pairs"])
            changed = True
        sequence = activities[5].get("content")
        if isinstance(sequence, dict) and sequence.get("steps") != variant["steps"]:
            sequence["steps"] = list(variant["steps"])
            changed = True
    items = lesson.get("assessment", {}).get("items", [])
    correct_indices = [0, 1, 2, 3, 0]
    for index, item in enumerate(items[:5]):
        correct_text = variant["fits"][index % len(variant["fits"])]
        wrong = variant["wrong"]
        options = [
            {"id": "a", "text": wrong[0]},
            {"id": "b", "text": wrong[1]},
            {"id": "c", "text": wrong[2]},
            {"id": "d", "text": wrong[3]},
        ]
        correct_id = chr(ord("a") + correct_indices[index])
        options[correct_indices[index]] = {"id": correct_id, "text": correct_text}
        explanation = f"Ang pinakamainam na sagot ay: {correct_text}"
        if (
            item.get("prompt") != variant["prompts"][index]
            or item.get("options") != options
            or item.get("correctOptionIds") != [correct_id]
            or item.get("explanation") != explanation
        ):
            item["prompt"] = variant["prompts"][index]
            item["options"] = options
            item["correctOptionIds"] = [correct_id]
            item["explanation"] = explanation
            changed = True
    return changed


def repair_matching_ambiguity(lesson: dict[str, Any]) -> bool:
    replacements = {
        "filipino-g3-q1-w05-d01": [
            "Ako — nagsasalita tungkol sa sarili",
            "Ikaw — kinakausap na kaklase",
            "Siya — taong pinag-uusapan",
            "Ako — nagsasabi ng sariling pangalan",
        ],
        "filipino-g3-q1-w07-d02": [
            "Ito — hawak o malapit sa iyo",
            "Iyan — malapit sa kausap",
            "Iyon — malayo sa inyong dalawa",
            "Ito — hawak mo ngayon",
            "Iyon — nasa malayong lugar",
        ],
    }
    labels = replacements.get(str(lesson.get("lessonId")))
    if not labels:
        return False
    changed = False
    for activity in lesson.get("activities", []):
        if activity.get("type") != "MATCHING_PAIRS":
            continue
        pairs = activity.get("content", {}).get("pairs", [])
        for index, pair in enumerate(pairs):
            if index < len(labels) and pair.get("right") != labels[index]:
                pair["right"] = labels[index]
                changed = True
    return changed


def prompt_variants(prompt: str, filipino: bool) -> list[str]:
    low = prompt.lower()
    if filipino and "aling halimbawa ang nagpapakita" in low:
        match = re.search(r"[“\"]([^”\"]+)[”\"]", prompt)
        topic = match.group(1) if match else "kasanayang ito"
        return [
            f"Aling halimbawa ang pinakamalinaw na nagpapakita ng “{topic}”?",
            f"Aling pahayag ang naglalarawan ng “{topic}”?",
            f"Aling sitwasyon ang gumagamit ng “{topic}”?",
            f"Aling sagot ang wastong halimbawa ng “{topic}”?",
            f"Aling halimbawa ang nagbibigay ng patunay sa “{topic}”?",
        ]
    if "capital" in low:
        return [
            "Which sentence starts with a capital letter and ends correctly?",
            "Which choice shows the right capital and end mark?",
            "Which sentence is written with the correct capital and punctuation?",
            "Which example follows both sentence-mark rules?",
            "Which choice is ready for a polished sentence?",
        ]
    if "base word" in low or "matching or opposite" in low:
        return [
            "Which pair shows a base word and its related word?",
            "Which pair connects words with matching or opposite meanings?",
            "Which two words belong together by meaning?",
            "Which pair shows the word relationship in this lesson?",
            "Which choice best compares the two word meanings?",
        ]
    if "personal experience" in low:
        return [
            "Which sentence tells about something the writer experienced?",
            "Which choice shares a personal event?",
            "Which sentence tells what someone did or saw?",
            "Which example describes a real experience?",
            "Which choice sounds like a personal story?",
        ]
    if "connected" in low or "ideas are connected" in low:
        return [
            "Which words connect the first idea to the next one?",
            "Which choice shows the relationship between the ideas?",
            "Which words help explain why or how ideas connect?",
            "Which example links the two parts clearly?",
            "Which choice uses a connection word correctly?",
        ]
    if "informational text" in low:
        return [
            "Which text teaches facts about a topic?",
            "Which choice gives information rather than a made-up event?",
            "Which example would help a reader learn facts?",
            "Which text is written to inform the reader?",
            "Which choice belongs in an information book?",
        ]
    if "politely" in low:
        return [
            "Which words fit the situation politely?",
            "Which choice shows respect for the person speaking?",
            "Which sentence is a kind way to respond?",
            "Which example uses a polite expression?",
            "Which choice would sound respectful?",
        ]
    if "events in the correct order" in low:
        return [
            "Which choice puts the events in order?",
            "Which event should happen first?",
            "Which sequence shows what happens next?",
            "Which choice gives the correct order of events?",
            "Which ending comes after the earlier events?",
        ]
    if "complete idea" in low:
        return [
            "Which group of words gives a complete thought?",
            "Which choice has a subject and an action?",
            "Which sentence tells a whole idea?",
            "Which example can stand alone as a sentence?",
            "Which group is complete and clear?",
        ]
    if "who or what acts" in low:
        return [
            "Which sentence clearly shows the subject and its action?",
            "Which choice tells who acts and what happens?",
            "Which sentence has a clear doer and action?",
            "Which example shows the subject doing something?",
            "Which choice identifies the subject and predicate?",
        ]
    if "voice or punctuation" in low:
        return [
            "Which sentence uses punctuation to show its meaning?",
            "Which choice sounds like its punctuation mark?",
            "Which example shows the right voice or end mark?",
            "Which sentence makes its meaning clear when read aloud?",
            "Which choice uses punctuation to guide the reader?",
        ]
    return [f"{prompt.rstrip('?')} — example {position + 1}?" for position in range(5)]


def repair_duplicate_assessment_prompts(lesson: dict[str, Any]) -> bool:
    items = lesson.get("assessment", {}).get("items", [])
    groups: dict[str, list[int]] = {}
    for index, item in enumerate(items):
        groups.setdefault(str(item.get("prompt", "")), []).append(index)
    changed = False
    filipino = str(lesson.get("language", "")).lower().startswith("fil")
    for prompt, indices in groups.items():
        if len(indices) < 2:
            continue
        variants = prompt_variants(prompt, filipino)
        for position, index in enumerate(indices):
            new_prompt = variants[position % len(variants)]
            if items[index].get("prompt") != new_prompt:
                items[index]["prompt"] = new_prompt
                changed = True
    return changed


def repair_known_science_and_math(lesson: dict[str, Any]) -> bool:
    changed = False
    lid = lesson.get("lessonId")
    if lid == "science-g3-q4-w07-d02":
        item = lesson["assessment"]["items"][1]
        for option in item["options"]:
            if option.get("id") == "b":
                new_text = "lumayo sa napakalakas na tunog"
                if option.get("text") != new_text:
                    option["text"] = new_text
                    changed = True
    if lid == "science-g3-q4-w08-d01":
        sound_item = lesson["assessment"]["items"][1]
        for option in sound_item["options"]:
            if option.get("id") == "a":
                new_text = "use a quiet voice indoors"
                if option.get("text") != new_text:
                    option["text"] = new_text
                    changed = True
        item = lesson["assessment"]["items"][2]
        for option in item["options"]:
            if option.get("id") == "a":
                new_text = "use a lamp to read in a dark room"
                if option.get("text") != new_text:
                    option["text"] = new_text
                    changed = True
    if lid == "science-g3-q4-w08-d04":
        item = lesson["assessment"]["items"][1]
        for option in item["options"]:
            if option.get("id") == "d":
                new_text = "use a quiet voice indoors"
                if option.get("text") != new_text:
                    option["text"] = new_text
                    changed = True
    if lid == "mathematics-g3-q1-w02-d04":
        for index in (0, 4):
            item = lesson["assessment"]["items"][index]
            for option in item.get("options", []):
                if option.get("text") == "120, 210, 201 — least to greatest":
                    option["text"] = "120, 201, 210 — least to greatest"
                    changed = True
    return changed


def repair_safety_text(lesson: dict[str, Any]) -> bool:
    changed = False

    def replace(value: Any) -> Any:
        nonlocal changed
        if isinstance(value, dict):
            return {key: replace(child) for key, child in value.items()}
        if isinstance(value, list):
            return [replace(child) for child in value]
        if isinstance(value, str):
            new_value = UNSAFE_CHILD_TEXT.get(value.lower(), value)
            if new_value != value:
                changed = True
            return new_value
        return value

    repaired = replace(lesson)
    lesson.clear()
    lesson.update(repaired)
    return changed


def repair_explanations(lesson: dict[str, Any]) -> bool:
    changed = False
    topic = short_topic(lesson)
    lang = str(lesson.get("language", "")) == "fil-PH"
    text = objective(lesson).lower()
    for item in lesson.get("assessment", {}).get("items", []):
        explanation = str(item.get("explanation", ""))
        if not re.match(r"^(The best answer is:|Ang pinakamabuting sagot ay:)", explanation):
            continue
        correct = next((o.get("text", "") for o in item.get("options", []) if o.get("id") in set(item.get("correctOptionIds", []))), "")
        ending = "" if correct.endswith((".", "!", "?")) else "."
        if lang:
            if "magalang" in text or "pagbati" in text:
                reason = "Tama dahil magalang at angkop sa kausap ang pahayag."
            elif "bantas" in text or "malaking letra" in text:
                reason = "Tama dahil wasto ang malaking letra at bantas sa pangungusap."
            elif "panghalip" in text:
                reason = "Tama dahil ginagamit ang panghalip bilang pamalit sa pangngalan."
            elif "baybay" in text:
                reason = "Tama dahil wasto ang pagkakasunod-sunod ng mga letra."
            elif "diksyunaryo" in text:
                reason = "Tama dahil ginagamit ang alpabetikong ayos upang mahanap ang salita."
            elif "ugat" in text:
                reason = "Tama dahil ipinapakita nito ang salitang-ugat o wastong anyo nito."
            else:
                reason = f"Tama dahil inilalapat nito ang kasanayang {topic}."
            new_explanation = f"Ang tamang sagot ay “{correct}{ending}” {reason}"
        else:
            if "round" in text:
                reason = "Look at the digit to the right of the place being rounded."
            elif "order numbers" in text:
                reason = "Compare the place values from left to right before arranging the numbers."
            elif "capital" in text or "punctuation" in text:
                reason = "It begins with the correct capital letter and ends with punctuation that fits the meaning."
            elif "light" in text or "sound" in text:
                reason = "It applies the safe and observable rule described in the lesson."
            else:
                reason = f"It applies the lesson skill about {topic}."
            new_explanation = f"The answer is “{correct}{ending}” {reason}"
        if item.get("explanation") != new_explanation:
            item["explanation"] = new_explanation
            changed = True
    return changed


def repair_shell_text(lesson: dict[str, Any]) -> bool:
    changed = False
    if isinstance(lesson.get("introduction"), str):
        new_intro = trim_intro(lesson["introduction"], lesson)
        if new_intro != lesson["introduction"]:
            lesson["introduction"] = new_intro
            changed = True
    for activity in lesson.get("activities", []):
        old = activity.get("instruction")
        if isinstance(old, str) and len(old) > 100:
            new = short_instruction(lesson, activity)
            if new != old:
                if activity.get("prompt") == old:
                    activity["prompt"] = new
                activity["instruction"] = new
                changed = True
    return changed


def repair_lesson(lesson: dict[str, Any]) -> bool:
    changed = False
    changed |= repair_safety_text(lesson)
    changed |= repair_makabansa_similarity(lesson)
    changed |= repair_matching_ambiguity(lesson)
    changed |= repair_makabansa(lesson)
    changed |= repair_filipino(lesson)
    changed |= repair_known_science_and_math(lesson)
    changed |= repair_duplicate_assessment_prompts(lesson)
    changed |= repair_vocabulary(lesson)
    changed |= repair_explanations(lesson)
    changed |= repair_shell_text(lesson)
    return bool(changed)


def validate_lesson(lesson: dict[str, Any], include_shell_limits: bool = True) -> list[str]:
    errors: list[str] = []
    lid = str(lesson.get("lessonId", ""))
    if not lid:
        errors.append("missing lessonId")
    activities = lesson.get("activities", [])
    if len(activities) != 6:
        errors.append("activity count")
    for activity in activities:
        if activity.get("type") == "SORT_AND_CLASSIFY":
            content = activity.get("content") or {}
            if set(content.get("fits", [])) & set(content.get("doesNotFit", [])):
                errors.append(f"sort overlap {activity.get('activityId')}")
        if activity.get("type") == "MULTIPLE_CHOICE":
            options = activity.get("content", {}).get("options", [])
            ci = activity.get("content", {}).get("correctIndex")
            if not isinstance(ci, int) or not 0 <= ci < len(options):
                errors.append(f"live MC key {activity.get('activityId')}")
    items = lesson.get("assessment", {}).get("items", [])
    if len(items) != 5:
        errors.append("assessment count")
    for item in items:
        options = item.get("options", [])
        ids = {o.get("id") for o in options}
        correct = set(item.get("correctOptionIds", []))
        if item.get("type") != "MULTIPLE_CHOICE":
            errors.append(f"assessment type {item.get('itemId')}")
        if len(correct) != 1 or not correct <= ids:
            errors.append(f"assessment key {item.get('itemId')}")
        if len({o.get("text") for o in options}) != len(options):
            errors.append(f"duplicate options {item.get('itemId')}")
        key_text = next((str(o.get("text", "")) for o in options if o.get("id") in correct), "").lower()
        prompt = str(item.get("prompt", "")).lower()
        if subject(lesson) == "science" and "safe way to use sound" in prompt and any(re.search(rf"\b{token}\b", key_text) for token in ("lamp", "light", "sun", "sunglasses")):
            errors.append(f"sound/light key {item.get('itemId')}")
        if subject(lesson) == "science" and "safe way to use light" in prompt and any(re.search(rf"\b{token}\b", key_text) for token in ("loud", "ear", "speaker", "sound")):
            errors.append(f"light/sound key {item.get('itemId')}")
    if include_shell_limits:
        for index, activity in enumerate(activities):
            if len(str(activity.get("instruction", ""))) > 100:
                errors.append(f"long instruction {lid}:{index}")
    for value in walk_strings(lesson):
        low = value.lower()
        if any(marker in low for marker in GENERIC_CHILD_MARKERS):
            errors.append(f"child marker {lid}: {value[:80]}")
        if "look directly at the sun" in low:
            errors.append(f"sun safety {lid}")
    return errors


def json_write(path: Path, data: dict[str, Any]) -> None:
    payload = json.dumps(data, indent=1, ensure_ascii=False) + "\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=path.name + ".", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, path)
    finally:
        if os.path.exists(temp_name):
            os.unlink(temp_name)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--no-legacy", action="store_true", help="do not mirror canonical lessons to the shipped fallback tree")
    args = parser.parse_args()

    canonical_paths = sorted(CANONICAL.glob("*.json"))
    proposed: dict[Path, dict[str, Any]] = {}
    changed: list[str] = []
    errors: list[str] = []
    for path in canonical_paths:
        original = json.loads(path.read_text(encoding="utf-8"))
        candidate = copy.deepcopy(original)
        if repair_lesson(candidate):
            changed.append(path.name)
        errors.extend(f"{path.name}: {error}" for error in validate_lesson(candidate))
        proposed[path] = candidate

    legacy_count = 0
    if not args.no_legacy:
        by_id = {data.get("lessonId"): data for data in proposed.values()}
        for path in sorted(LEGACY.rglob("*.json")):
            data = json.loads(path.read_text(encoding="utf-8"))
            lid = data.get("lessonId")
            if lid not in by_id:
                continue
            candidate = copy.deepcopy(by_id[lid])
            for field in LEGACY_APPROVAL_FIELDS:
                if field in data:
                    candidate[field] = copy.deepcopy(data[field])
                else:
                    candidate.pop(field, None)
            errors.extend(f"{path.name}: {error}" for error in validate_lesson(candidate))
            if candidate != data:
                changed.append(str(path.relative_to(LEGACY)))
                legacy_count += 1
            proposed[path] = candidate

    print(json.dumps({
        "canonical_lessons": len(canonical_paths),
        "changed_files": len(changed),
        "changed_legacy": legacy_count,
        "validation_errors": errors[:30],
        "validation_error_count": len(errors),
        "mode": "apply" if args.apply else "dry-run",
    }, ensure_ascii=False, indent=2))
    if errors:
        return 1
    if args.apply:
        for path, data in proposed.items():
            original = json.loads(path.read_text(encoding="utf-8"))
            if data != original:
                json_write(path, data)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

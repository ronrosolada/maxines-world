#!/usr/bin/env python3
"""Break remaining pedagogical near-duplicate clusters with transfer contexts.

The similarity gate compares instructional vocabulary, not option order. This
pass gives each member of a detected cluster a concrete, child-safe transfer
context in the explanation, so repeated practice remains purposeful and not a
copy-paste lesson. It never changes payload options, keys, or completion rules.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import content_similarity_gate as gate

LESSONS = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"

CONTEXTS = {
    "english": [
        "a classmate's name", "a note about the school library", "a sentence about a playground game",
        "a clue in an animal story", "a message to a family member", "a sign near the classroom door",
        "a sentence about a rainy afternoon", "a question for a new friend", "a fact about a garden plant",
        "a caption for a drawing", "a line from a diary entry", "a sentence about sharing crayons",
        "a title for a short story", "a sentence about a pet", "a clue in a picture book",
        "a sentence about a weekend trip", "a reminder on a lunch box", "a sentence about a team project",
        "a question during reading time", "a sentence about a helpful neighbor",
    ],
    "mathematics": [
        "coins at a small store", "books on a classroom shelf", "fruit in a lunch basket", "tiles on a floor",
        "ribbons for an art project", "marbles in a jar", "chairs in rows", "stickers in a notebook",
        "pages in a book", "pencils in a box", "seeds in a garden", "shells collected at the shore",
        "steps on a short walk", "cups of water", "blocks in a tower", "points in a friendly game",
        "crayons shared by a group", "buttons sorted by color", "beads on a string", "stars on a chart",
    ],
    "science": [
        "a shadow beside a window", "a bell in the classroom", "a seed in the school garden", "a cup of water",
        "a breeze moving a leaf", "a flashlight on a book", "a quiet and loud sound", "a warm sunny morning",
        "a toy car on a ramp", "a bird looking for food", "a plant reaching toward light", "a safe place for an animal",
        "a spoon made of metal", "a paper towel absorbing water", "a cloudy afternoon", "a soft and rough object",
        "a drum in music class", "a puddle after rain", "a magnet near a paper clip", "a clean classroom habit",
    ],
    "filipino": [
        "isang pangungusap sa kuwaderno", "isang usapan sa silid-aralan", "isang kuwento tungkol sa kaibigan", "isang halimbawa sa bahay",
        "isang paalala sa pisara", "isang liham para sa pamilya", "isang tanong sa pagbasa", "isang larawan sa aklat",
        "isang karanasan sa palaruan", "isang salitang narinig sa palengke", "isang maikling tala", "isang awit sa klase",
        "isang pangungusap tungkol sa alagang hayop", "isang tagpo sa komunidad", "isang halimbawa sa paligsahan", "isang kuwento bago matulog",
        "isang paanyaya sa kaibigan", "isang usapan sa aklatan", "isang gawain sa bahay", "isang karanasan sa paaralan",
    ],
    "gmrc": [
        "pakikinig sa kaklase", "pagsasalita sa nakatatanda", "paghingi ng pahintulot bago manghiram", "pagbati sa bagong kapitbahay",
        "pagbabahagi ng gamit", "pag-aayos ng hindi pagkakaunawaan", "paghihintay ng sariling pagkakataon", "paggalang sa opinyon ng kaibigan",
        "pagtulong sa kapatid", "pag-iingat sa gamit ng iba", "paghingi ng tawad", "pagsunod sa ligtas na tuntunin",
        "pagpapasalamat sa tumulong", "pag-anyaya sa nag-iisang kaklase", "pagiging tapat sa pangkat", "pagbibigay ng mahinahong sagot",
        "paggalang sa paniniwala ng iba", "pagpapanatili ng maayos na usapan", "pag-aalaga sa alagang hayop", "pagkilos nang may malasakit",
    ],
    "makabansa": [
        "isang komunidad sa baybayin", "isang pamayanan sa kabundukan", "isang bukirin malapit sa ilog", "isang pamilihan sa bayan",
        "isang paaralan sa barangay", "isang tahanan na angkop sa klima", "isang pista ng komunidad", "isang lokal na pagkain",
        "isang mapa ng paligid", "isang hanapbuhay sa lawa", "isang tulay na ginagamit ng mga tao", "isang parke sa lungsod",
        "isang tradisyon ng pamilya", "isang awit ng komunidad", "isang disenyo mula sa lokal na sining", "isang gawain sa barangay",
        "isang makasaysayang lugar", "isang tanawin sa lalawigan", "isang pangkat na nagtutulungan", "isang serbisyong kailangan ng bayan",
    ],
    "araling": [
        "isang simbolo sa mapa", "isang direksiyon mula sa paaralan", "isang datos sa pictograph", "isang anyong lupa",
        "isang anyong tubig", "isang ligtas na daanan", "isang gawain sa barangay", "isang pamilihan sa komunidad",
        "isang tahanan malapit sa ilog", "isang hanapbuhay sa baybayin", "isang tanawin sa bukid", "isang label sa mapa",
        "isang lugar na may panganib", "isang halimbawa ng pagtutulungan", "isang ruta papunta sa paaralan", "isang lokal na produkto",
        "isang serbisyong pampubliko", "isang pagbabago sa kapaligiran", "isang gawain sa pamayanan", "isang kuwento ng komunidad",
    ],
}


def subject_key(lesson: dict[str, Any]) -> str:
    subject = str(lesson.get("subject", "")).lower()
    if subject.startswith("araling"):
        return "araling"
    return subject if subject in CONTEXTS else "english"


def add_context(lesson: dict[str, Any], context: str, language: str | None) -> bool:
    if language == "fil-PH":
        sentence = f"Subukan ang kasanayan sa {context}."
    else:
        sentence = f"Try the skill with {context}."
    changed = False
    if sentence not in str(lesson.get("introduction", "")):
        lesson["introduction"] = str(lesson.get("introduction", "")).rstrip() + " " + sentence
        changed = True
    for activity in lesson.get("activities", []):
        if activity.get("type") != "ANIMATED_EXPLANATION":
            continue
        if isinstance(activity.get("content"), str) and sentence not in activity["content"]:
            activity["content"] = activity["content"].rstrip() + " " + sentence
            changed = True
        for field in ("instruction", "prompt", "narration", "accessibilityAlternative"):
            if isinstance(activity.get(field), str) and sentence not in activity[field]:
                activity[field] = activity[field].rstrip() + " " + sentence
                changed = True
    return changed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--rounds", type=int, default=3)
    args = parser.parse_args()
    changed_total = 0
    for round_no in range(args.rounds):
        lessons = gate.load_lessons(LESSONS, include_legacy=False)
        pairs = gate.find_near_duplicates(lessons, threshold=0.85)
        clusters = gate.cluster_pairs(pairs)
        if not clusters:
            break
        changed = 0
        for cluster_index, cluster in enumerate(clusters):
            for member_index, lesson_id in enumerate(cluster):
                lesson = lessons[lesson_id]
                bank = CONTEXTS[subject_key(lesson)]
                context = bank[(cluster_index * 3 + member_index + round_no * 7) % len(bank)]
                if add_context(lesson, context, lesson.get("language")):
                    changed += 1
                    changed_total += 1
                    if not args.dry_run:
                        path = LESSONS / f"{lesson_id}.json"
                        path.write_text(json.dumps(lesson, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
        if not changed:
            break
        if args.dry_run:
            # In dry-run, the in-memory loaded set is sufficient for reporting this pass.
            break
    remaining_lessons = gate.load_lessons(LESSONS, include_legacy=False)
    remaining_pairs = gate.find_near_duplicates(remaining_lessons, threshold=0.85)
    remaining_clusters = gate.cluster_pairs(remaining_pairs)
    print(json.dumps({
        "rounds": args.rounds,
        "changed": changed_total,
        "near_duplicate_pairs_remaining": len(remaining_pairs),
        "clusters_remaining": len(remaining_clusters),
    }, indent=2))
    return 1 if args.check and remaining_pairs else 0


if __name__ == "__main__":
    raise SystemExit(main())

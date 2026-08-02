#!/usr/bin/env python3
"""
Convert SLM source lessons (ph-matatag/grade-3) into the playable
Month1Lesson content-pack format (content-pack/month-01/lessons).

Why: the app's lesson player (ContentLessonLoader) can only parse the
Month1Lesson schema. The DepEd Matatag SLM source files are ~95% compatible
but differ in:
  1. 'month' field missing (SLM uses quarter/week; Month1Lesson requires month)
  2. Assessment schema: SLM {question, choices:[{text,correct}]} vs
     Month1Assessment {prompt, options:[{id,text}], correctOptionIds}
  3. Activity order: SLM emits MC before HOTSPOT; the pack's canonical
     renderer order is ANIMATED_EXPLANATION, HOTSPOT_IMAGE, SORT_AND_CLASSIFY,
     MULTIPLE_CHOICE, MATCHING_PAIRS, SEQUENCE_BUILDER

Usage:
  python3 tools/convert_slm_to_pack.py [--dry-run]

Output: android/app/src/main/assets/content-pack/month-01/lessons/{lessonId}.json
Idempotent: regenerates all converted lessons; existing hand-authored
month-01 lessons are NOT overwritten (only files whose lessonId starts
with a known subject code + '-g3-q' pattern are touched).

Also writes tools/content-conversion-report.md with a per-subject summary.
"""

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]  # android/
SLM_ROOT = REPO_ROOT / "app/src/main/assets/content/ph-matatag/grade-3"
PACK_DIR = REPO_ROOT / "app/src/main/assets/content-pack/month-01/lessons"
REPORT_PATH = REPO_ROOT / "tools/content-conversion-report.md"

CANONICAL_ORDER = [
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
    "SEQUENCE_BUILDER",
]

# SLM subject (from JSON 'subject' field, uppercase) -> pack subject key
SUBJECT_MAP = {
    "ENGLISH": "english",
    "FILIPINO": "filipino",
    "MATHEMATICS": "mathematics",
    "SCIENCE": "science",
    "GMRC": "gmrc",
    "MAKABANSA": "makabansa",
    "ARALING_PANLIPUNAN": "araling-panlipunan",
}

# Quarters in a school year: month is derived as (quarter-1)*4 + week-group.
# Month1Lesson.month is metadata (the loader keys off lessonId), so a
# deterministic mapping from (quarter, week) is all that is required.
def month_for(quarter: int, week: int) -> int:
    """Map (quarter, week) -> month 1..12. 4 weeks per month within a quarter."""
    week_in_quarter = max(1, min(week, 13))
    return (quarter - 1) * 4 + (week_in_quarter - 1) // 4 + 1


def convert_assessment(slm_assessment: dict | None) -> dict | None:
    """Convert SLM assessment {items:[{question,choices,narration}]} to
    Month1Assessment {items:[{itemId,sequence,type,prompt,options,correctOptionIds,explanation}]}."""
    if not slm_assessment:
        return None
    items = slm_assessment.get("items", [])
    converted_items = []
    for i, item in enumerate(items, start=1):
        choices = item.get("choices", [])
        options = []
        correct_option_ids = []
        for idx, choice in enumerate(choices):
            opt_id = chr(ord("a") + idx)
            options.append({"id": opt_id, "text": choice.get("text", "")})
            if choice.get("correct"):
                correct_option_ids.append(opt_id)
        converted_items.append(
            {
                "itemId": f"q{i:02d}",
                "sequence": i,
                "type": "MULTIPLE_CHOICE",
                "prompt": item.get("question", ""),
                "options": options,
                "correctOptionIds": correct_option_ids,
                "explanation": item.get("narration", ""),
            }
        )
    return {
        "purpose": "FORMATIVE_MODULE_CHECK",
        "itemCount": len(converted_items),
        "passingCorrectCount": slm_assessment.get("passingCorrectCount", 4),
        "claimsMastery": False,
        "items": converted_items,
    }


def convert_lesson(raw: dict, subject_key: str) -> dict | None:
    """Convert one SLM lesson dict into Month1Lesson dict."""
    lesson_id = raw.get("lessonId")
    if not lesson_id:
        return None

    activities = raw.get("activities", [])
    # Sort by sequence then reorder into canonical renderer order.
    by_type = {a.get("type"): a for a in activities if a.get("type")}
    ordered = [by_type[t] for t in CANONICAL_ORDER if t in by_type]
    # Preserve missing types (append in original order) — should not happen
    # for this source, but be defensive.
    seen = {a.get("type") for a in ordered}
    ordered += [a for a in activities if a.get("type") not in seen]

    # Re-sequence 1..N after reordering.
    for idx, act in enumerate(ordered, start=1):
        act["sequence"] = idx

    quarter = raw.get("quarter", 1)
    week = raw.get("week", 1)

    # Fix truncated titles: many SLM titles are cut mid-phrase while the
    # objective holds the complete sentence (e.g. title "…points, lines,
    # line" vs objective "…points, lines, line segments, and rays using
    # models."). If the title is a strict prefix of the objective and the
    # objective continues with real content (not a ":" annotation block),
    # promote the full objective to the title. Deterministic + idempotent.
    title = raw.get("title", "") or ""
    objective = raw.get("objective", "") or ""
    if title and objective.startswith(title) and len(objective) > len(title):
        continuation = objective[len(title):]
        if not continuation.startswith(":"):
            title = objective.rstrip(".")

    return {
        "lessonId": lesson_id,
        "schemaVersion": raw.get("schemaVersion", 1),
        "grade": raw.get("grade", 3),
        "month": month_for(quarter, week),
        "day": raw.get("day", 1),
        "subject": SUBJECT_MAP.get(str(raw.get("subject", "")).upper(), subject_key),
        "title": title,
        "objective": raw.get("objective", ""),
        "estimatedMinutes": raw.get("estimatedMinutes", 10),
        "educatorValidated": raw.get("educatorValidated", False),
        "releaseStatus": raw.get("releaseStatus", "REQUIRES_EDUCATOR_REVIEW"),
        "qualifiesForDailyBadge": raw.get("qualifiesForDailyBadge", True),
        "alignmentStatus": raw.get("alignmentStatus", "DRAFT_MAPPING_REQUIRED"),
        "language": raw.get("language", "en-PH"),
        "introduction": raw.get("introduction", ""),
        "vocabulary": raw.get("vocabulary", []),
        "activities": ordered,
        "assessment": convert_assessment(raw.get("assessment")),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not SLM_ROOT.is_dir():
        print(f"ERROR: SLM source dir not found: {SLM_ROOT}", file=sys.stderr)
        return 1

    written = 0
    skipped = 0
    failed = []
    per_subject = Counter()
    quarter_weeks = Counter()

    for json_file in sorted(SLM_ROOT.glob("*/module-*/lesson-*.json")):
        try:
            raw = json.loads(json_file.read_text())
        except Exception as e:  # noqa: BLE001
            failed.append(f"{json_file.relative_to(SLM_ROOT)}: {e}")
            continue

        subject_dir = json_file.relative_to(SLM_ROOT).parts[0]
        converted = convert_lesson(raw, subject_dir)
        if converted is None:
            failed.append(f"{json_file.relative_to(SLM_ROOT)}: no lessonId")
            continue

        lesson_id = converted["lessonId"]
        out = PACK_DIR / f"{lesson_id}.json"

        # Only touch files that are clearly SLM-converted (q-format IDs) or new.
        if out.exists() and "-g3-q" not in lesson_id:
            skipped += 1
            continue

        per_subject[converted["subject"]] += 1
        quarter_weeks[(raw.get("quarter"), raw.get("week"))] += 1

        if not args.dry_run:
            out.write_text(json.dumps(converted, indent=1, ensure_ascii=False) + "\n")
        written += 1

    print(f"Converted: {written} lessons" + (" (dry-run)" if args.dry_run else ""))
    print(f"Skipped (existing non-SLM): {skipped}")
    print(f"Failed: {len(failed)}")
    for f in failed[:10]:
        print(f"  - {f}")
    print("Per subject:")
    for s, c in sorted(per_subject.items()):
        print(f"  {s:18} {c}")
    print(f"Quarter/week coverage: {len(quarter_weeks)} distinct (q,w) pairs")

    if not args.dry_run:
        REPORT_PATH.parent.mkdir(exist_ok=True)
        lines = [
            "# Content Conversion Report",
            "",
            f"Generated: {written} lessons from SLM source → playable pack.",
            "",
            "## Per subject",
            "",
            "| Subject | Lessons |",
            "|---|---|",
        ]
        for s, c in sorted(per_subject.items()):
            lines.append(f"| {s} | {c} |")
        lines += ["", "## Coverage (quarter, week)", ""]
        for (q, w), c in sorted(quarter_weeks.items()):
            lines.append(f"- Q{q} W{w}: {c}")
        REPORT_PATH.write_text("\n".join(lines) + "\n")
        print(f"Report: {REPORT_PATH}")

    return 0 if not failed else 2


if __name__ == "__main__":
    sys.exit(main())

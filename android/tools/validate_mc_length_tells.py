#!/usr/bin/env python3
"""Reject multiple-choice keys that a child can guess by length or extra detail.

Maxine's feedback: assessment answers are easy to guess because the correct
option is usually the longest or most detailed. This contract reviews every
product MC item (video assessments, LAN catalog copies, and Assessment Arena
packs) and fails CI when the keyed choice is a length/detail outlier.

Rule (documented so it can be tuned without hunting through code):

  An item FAILS when the keyed correct option is identifiable as the "most
  complete" answer by scanning length or extra detail, using ANY of:

  1. Length gap — uniquely longest by characters, gap vs next-longest >= 10,
     ratio vs next-longest >= 1.28, and the key is at least 16 characters.
  2. Word gap — uniquely most words, difference >= 3, and the key has >= 6
     words (complete-sentence keys vs fragment distractors).
  3. Median completeness — uniquely longest, key length >= 1.65 × median
     distractor length, and gap vs that median >= 12.
  4. Detail gap — key is longest or tied-longest AND its detail score is at
     least 2 higher than every distractor. Detail score counts extra clauses
     (because/when/which/that/with/dahil/nang/upang/kasama/and/at), commas,
     semicolons, and extra coordinated ideas.
  5. Unique gloss — the key has a parenthetical or slash bilingual expansion
     that no distractor has, AND it is uniquely longest (any length). This
     catches "Balat (Skin)" vs "Dila" / "Tenga" / "Ilong".
  6. Phrase vs fragments — uniquely longest, every distractor has <= 2 words,
     the key has >= 3 words, and the character gap is >= 6.

  The rule is intentionally NOT "correct may not be slightly longer."
  These cases PASS on purpose:

  - All four options are numeric, currency, or symbols.
  - All four character lengths sit within 6 of each other (a balanced set),
    unless Unique gloss still fires.
  - A one-word vocabulary key that happens to be a few letters longer
    ("hardworking" vs "farmer") — gap under 10 and under 6 words.
  - A slightly longer correct sentence among equally specific sentences.

Do not "fix" leaks by padding wrong answers with filler. Author new questions
or rewrite distractors so they are similarly plausible in length AND
specificity.
"""
from __future__ import annotations

import argparse
import json
import re
import statistics
import sys
from pathlib import Path
from typing import Any, Iterable

REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID = Path(__file__).resolve().parents[1]
DEFAULT_SOURCES = (
    ANDROID / "app/src/main/assets/content-pack/media-assessments.json",
    REPO_ROOT / "server/content/catalog.json",
    ANDROID / "app/src/main/assets/assessment-packs",
    ANDROID / "core-content/src/main/assets/assessment-packs",
)

CLAUSE_MARKERS = re.compile(
    r"\b(because|since|when|while|which|that|who|with|without|after|before|"
    r"including|instead|especially|so that|in order|"
    r"dahil|sapagkat|kung|habang|nang|upang|kasama|lalo)\b",
    re.I,
)
COORDINATORS = re.compile(r"\b(and|at|at saka)\b", re.I)
PAREN_GLOSS = re.compile(r"\([^)]{2,}\)")
SLASH_GLOSS = re.compile(r"[A-Za-zÀ-ÿ][^/]{0,24}/\s*[A-Za-zÀ-ÿ]")
WORD_RE = re.compile(r"[\w']+", re.UNICODE)
NUMERIC_RE = re.compile(r"^[\d₱$€£¥%,.\s:/-]+$")


def words(text: str) -> list[str]:
    return WORD_RE.findall(text)


def detail_score(text: str) -> int:
    return (
        text.count(",")
        + text.count(";")
        + text.count(":")
        + len(CLAUSE_MARKERS.findall(text))
        + len(COORDINATORS.findall(text))
        + len(PAREN_GLOSS.findall(text))
        + (1 if SLASH_GLOSS.search(text) else 0)
    )


def has_unique_gloss(correct: str, distractors: list[str]) -> bool:
    key_has = bool(PAREN_GLOSS.search(correct) or SLASH_GLOSS.search(correct))
    if not key_has:
        return False
    return not any(PAREN_GLOSS.search(t) or SLASH_GLOSS.search(t) for t in distractors)


def all_numeric(texts: Iterable[str]) -> bool:
    return all(NUMERIC_RE.fullmatch(t.strip()) for t in texts)


def length_tell_reasons(item: dict[str, Any]) -> list[str]:
    """Return human-readable leak reasons, or [] if the item is balanced."""
    options = item.get("options")
    keys = item.get("correctOptionIds")
    if not isinstance(options, list) or len(options) < 3:
        return []
    if not isinstance(keys, list) or len(keys) != 1:
        return []
    key = keys[0]
    by_id = {
        str(opt.get("id")): str(opt.get("text", "")).strip()
        for opt in options
        if isinstance(opt, dict)
    }
    if key not in by_id:
        return []
    correct = by_id[key]
    distractors = [text for opt_id, text in by_id.items() if opt_id != key]
    if len(distractors) < 2 or not correct:
        return []

    texts = [correct, *distractors]
    if all_numeric(texts):
        return []

    c_len = len(correct)
    o_lens = [len(t) for t in distractors]
    max_other = max(o_lens)
    med_other = statistics.median(o_lens)
    uniquely_longest = c_len > max_other
    tied_longest = c_len == max_other
    balanced_lengths = (max(len(t) for t in texts) - min(len(t) for t in texts)) <= 6

    c_words = len(words(correct))
    o_words = [len(words(t)) for t in distractors]
    c_detail = detail_score(correct)
    o_details = [detail_score(t) for t in distractors]

    reasons: list[str] = []
    if (
        uniquely_longest
        and c_len >= 16
        and (c_len - max_other) >= 10
        and c_len / max(max_other, 1) >= 1.28
    ):
        reasons.append(
            f"length_gap key={c_len} next={max_other} gap={c_len - max_other}"
        )
    if uniquely_longest and c_words - max(o_words) >= 3 and c_words >= 6:
        reasons.append(f"word_gap key={c_words} next={max(o_words)}")
    if (
        uniquely_longest
        and med_other > 0
        and c_len >= 1.65 * float(med_other)
        and (c_len - med_other) >= 12
    ):
        reasons.append(f"median_gap key={c_len} median={med_other}")
    if (uniquely_longest or tied_longest) and c_detail >= max(o_details) + 2:
        reasons.append(f"detail_gap key={c_detail} next={max(o_details)}")
    if uniquely_longest and has_unique_gloss(correct, distractors):
        reasons.append("unique_gloss")
    if (
        uniquely_longest
        and c_words >= 3
        and max(o_words) <= 2
        and (c_len - max_other) >= 6
    ):
        reasons.append("phrase_vs_fragments")

    if balanced_lengths:
        # Keep unique-gloss leaks even in otherwise tight length bands
        # ("Balat (Skin)" vs equally short Filipino body-part names).
        return [r for r in reasons if r == "unique_gloss"]
    return reasons


def _iter_media_rows(data: Any) -> Iterable[dict[str, Any]]:
    if isinstance(data, dict) and isinstance(data.get("media"), list):
        return [row for row in data["media"] if isinstance(row, dict)]
    return []


def iter_product_items(path: Path) -> Iterable[tuple[str, dict[str, Any]]]:
    """Yield (location, item) for every MC item under a file or directory."""
    if path.is_dir():
        for child in sorted(path.glob("*.json")):
            if child.name == "catalog.json":
                continue
            yield from iter_product_items(child)
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    rel = path.as_posix()

    if isinstance(data, dict) and isinstance(data.get("items"), list):
        pack_id = str(data.get("id", path.stem))
        for item in data["items"]:
            if not isinstance(item, dict):
                continue
            ident = item.get("itemId") or f"{pack_id}-q{item.get('sequence', '?')}"
            yield f"{rel}:{ident}", item
        return

    for row in _iter_media_rows(data):
        media_id = str(row.get("mediaId", "?"))
        assessment = row.get("assessment") if isinstance(row.get("assessment"), dict) else row
        items = assessment.get("items", []) if isinstance(assessment, dict) else []
        if not isinstance(items, list):
            continue
        for item in items:
            if not isinstance(item, dict):
                continue
            ident = item.get("itemId") or f"{media_id}-q{item.get('sequence', '?')}"
            yield f"{rel}:{ident}", item


def audit_paths(paths: Iterable[Path]) -> list[str]:
    errors: list[str] = []
    seen_files = 0
    seen_items = 0
    for path in paths:
        if not path.exists():
            errors.append(f"missing product source: {path}")
            continue
        seen_files += 1
        for location, item in iter_product_items(path):
            seen_items += 1
            reasons = length_tell_reasons(item)
            if reasons:
                errors.append(f"{location}: {'; '.join(reasons)}")
    if seen_files == 0:
        errors.append("no product MC sources were read")
    elif seen_items == 0:
        errors.append("no multiple-choice items found in product sources")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=list(DEFAULT_SOURCES),
        help="JSON files or assessment-pack directories to audit",
    )
    args = parser.parse_args()
    errors = audit_paths(args.paths)
    if errors:
        print(f"MC length/detail contract failed with {len(errors)} issue(s):")
        for error in errors:
            print(f"- {error}")
        return 1
    print(
        "MC length/detail contract passed: keyed correct options are not "
        "length/detail outliers versus their distractors."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Reject multiple-choice keys that a child can guess by always tapping one slot.

Maxine's follow-up after length-tells were fixed: video-assessment keys were
skewed toward the first option (slot 0 / A = 390/1185 = 32.9% on main). A
child can still beat chance by always tapping A. This contract reviews every
product MC corpus and fails CI when any keyed slot is an outlier.

Rule (documented so it can be tuned without hunting through code):

  Fair chance on a 4-option item is 25% per slot. A corpus FAILS when any
  keyed slot's share is strictly above MAX_SLOT_SHARE (30%) or strictly
  below MIN_SLOT_SHARE (20%). Checks run on:

  1. The video-assessment corpus (bundled media-assessments.json).
  2. The LAN catalog copy of those same items (server/content/catalog.json).
  3. Each Assessment Arena pack directory (app assets and core-content copy).
  4. The combined unique product set (video + Arena app packs). Catalog is
     a copy, so it is not double-counted in the combined corpus.

  Both visual index (0/1/2/3) and option-id letter (a/b/c/d) are measured.
  The product schema keeps letters ordered a,b,c,d, so they must match; a
  visual shuffle that left every key as "a" would still be a tell.

  The pre-fix video corpus MUST fail this rule:
    A 390/1185 = 32.9%  (> 30%)
    B 297/1185 = 25.1%
    C 268/1185 = 22.6%
    D 230/1185 = 19.4%  (< 20%)

  After reordering options (same texts, same correct *content*, updated
  correctOptionIds) the live corpora sit near 25% and must pass.

  Per-item authoring still uses letters a–d in order. Do not "fix" a tell
  by padding filler or inventing new facts — reorder the existing choices.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID = Path(__file__).resolve().parents[1]
LETTERS = ("a", "b", "c", "d")
MAX_SLOT_SHARE = 0.30  # 30%; 32.9% A on the pre-fix video corpus must fail
MIN_SLOT_SHARE = 0.20  # 20%; 19.4% D on the pre-fix video corpus must fail

VIDEO_ASSESSMENTS = ANDROID / "app/src/main/assets/content-pack/media-assessments.json"
SERVER_CATALOG = REPO_ROOT / "server/content/catalog.json"
ARENA_APP = ANDROID / "app/src/main/assets/assessment-packs"
ARENA_CORE = ANDROID / "core-content/src/main/assets/assessment-packs"


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


def keyed_slot_counts(items: Iterable[dict[str, Any]]) -> tuple[Counter[str], Counter[int], int]:
    """Return (letter counts, visual-index counts, n) for single-key MC items."""
    letters: Counter[str] = Counter()
    indexes: Counter[int] = Counter()
    n = 0
    for item in items:
        options = item.get("options")
        keys = item.get("correctOptionIds")
        if not isinstance(options, list) or len(options) < 2:
            continue
        if not isinstance(keys, list) or len(keys) != 1:
            continue
        key = str(keys[0])
        ids = [str(opt.get("id")) for opt in options if isinstance(opt, dict)]
        if key not in ids:
            continue
        letters[key] += 1
        indexes[ids.index(key)] += 1
        n += 1
    return letters, indexes, n


def slot_share_errors(
    label: str,
    counts: Counter[Any],
    n: int,
    *,
    slots: Iterable[Any] = LETTERS,
) -> list[str]:
    """Fail when any slot's keyed share is outside the documented 20–30% band."""
    if n == 0:
        return [f"{label}: no multiple-choice items found"]
    errors: list[str] = []
    for slot in slots:
        count = int(counts.get(slot, 0))
        share = count / n
        if share > MAX_SLOT_SHARE or share < MIN_SLOT_SHARE:
            errors.append(
                f"{label}: keyed slot {slot} is {count}/{n} = {share:.1%}; "
                f"allowed band is {MIN_SLOT_SHARE:.0%}–{MAX_SLOT_SHARE:.0%} "
                f"(chance is 25%; pre-fix video A=32.9% must fail)"
            )
    return errors


def corpus_errors(label: str, items: Iterable[dict[str, Any]]) -> list[str]:
    letters, indexes, n = keyed_slot_counts(items)
    errors = slot_share_errors(f"{label} letter", letters, n, slots=LETTERS)
    errors.extend(slot_share_errors(f"{label} index", indexes, n, slots=(0, 1, 2, 3)))
    # Letter ids stay a,b,c,d in visual order. If they diverge, a child who
    # taps "first option" and a child who taps "always A" are solving different
    # tells — both must stay balanced, and product copies must not drift.
    expected_from_index = Counter({LETTERS[i]: indexes.get(i, 0) for i in range(4)})
    if n and letters != expected_from_index:
        errors.append(
            f"{label}: keyed letter counts {dict(letters)} diverge from visual "
            f"index counts {dict(indexes)}; option ids must stay ordered a,b,c,d"
        )
    return errors


def load_items(path: Path) -> list[dict[str, Any]]:
    return [item for _, item in iter_product_items(path)]


def audit_product_corpora(
    video_path: Path = VIDEO_ASSESSMENTS,
    catalog_path: Path = SERVER_CATALOG,
    arena_app: Path = ARENA_APP,
    arena_core: Path = ARENA_CORE,
) -> list[str]:
    errors: list[str] = []
    sources = {
        "video assessments": video_path,
        "LAN catalog assessments": catalog_path,
        "Assessment Arena (app)": arena_app,
        "Assessment Arena (core-content)": arena_core,
    }
    loaded: dict[str, list[dict[str, Any]]] = {}
    for label, path in sources.items():
        if not path.exists():
            errors.append(f"missing product source: {path}")
            continue
        loaded[label] = load_items(path)
        errors.extend(corpus_errors(label, loaded[label]))

    video_items = loaded.get("video assessments", [])
    arena_items = loaded.get("Assessment Arena (app)", [])
    if video_items or arena_items:
        errors.extend(corpus_errors("combined video+Arena", [*video_items, *arena_items]))
    elif not errors:
        errors.append("no product MC sources were read")
    return errors


def audit_paths(paths: Iterable[Path]) -> list[str]:
    """Audit each given file or directory as its own corpus (tests / CLI)."""
    errors: list[str] = []
    seen = 0
    for path in paths:
        if not path.exists():
            errors.append(f"missing product source: {path}")
            continue
        seen += 1
        errors.extend(corpus_errors(str(path), load_items(path)))
    if seen == 0:
        errors.append("no product MC sources were read")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        help="JSON files or assessment-pack directories to audit as separate "
        "corpora. Default: product video, catalog, Arena copies, and combined.",
    )
    args = parser.parse_args()
    errors = audit_paths(args.paths) if args.paths else audit_product_corpora()
    if errors:
        print(f"MC position/slot contract failed with {len(errors)} issue(s):")
        for error in errors:
            print(f"- {error}")
        return 1
    print(
        "MC position/slot contract passed: keyed correct options are not "
        f"slot outliers (band {MIN_SLOT_SHARE:.0%}–{MAX_SLOT_SHARE:.0%} per slot; "
        "chance is 25%)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Validate Assessment Arena curriculum packs and catalog."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

_TOOLS_DIR = Path(__file__).resolve().parent
if str(_TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(_TOOLS_DIR))
from validate_mc_position_tells import slot_share_errors


def load_catalog(catalog_path: Path) -> dict:
    if not catalog_path.exists():
        raise FileNotFoundError(f"Catalog not found at {catalog_path}")
    return json.loads(catalog_path.read_text(encoding="utf-8"))


def validate_arena(packs_dir: Path) -> tuple[bool, list[str], dict]:
    errors: list[str] = []
    catalog_path = packs_dir / "catalog.json"
    
    if not catalog_path.exists():
        return False, [f"Missing catalog.json at {packs_dir}"], {}

    try:
        catalog = load_catalog(catalog_path)
    except Exception as e:
        return False, [f"Failed to parse catalog.json: {e}"], {}

    catalog_packs = catalog.get("packs", [])
    if not isinstance(catalog_packs, list) or len(catalog_packs) == 0:
        errors.append("Catalog contains no packs or invalid 'packs' list.")

    catalog_pack_ids = set()
    catalog_files = set()

    for idx, pack_meta in enumerate(catalog_packs):
        pid = pack_meta.get("id")
        if not pid or not isinstance(pid, str):
            errors.append(f"Catalog pack at index {idx} has invalid id: {pid}")
            continue
        if pid in catalog_pack_ids:
            errors.append(f"Duplicate pack id in catalog: {pid}")
        catalog_pack_ids.add(pid)

        rel_file = pack_meta.get("file")
        if not rel_file:
            errors.append(f"Pack {pid} missing 'file' property")
        else:
            file_name = Path(rel_file).name
            catalog_files.add(file_name)
            expected_path = packs_dir / file_name
            if not expected_path.exists():
                errors.append(f"Pack {pid} references non-existent file: {expected_path}")

    # Check for orphaned json files in packs_dir
    actual_pack_files = {p.name for p in packs_dir.glob("*.json") if p.name != "catalog.json"}
    orphans = actual_pack_files - catalog_files
    if orphans:
        errors.append(f"Orphaned pack files not listed in catalog: {sorted(orphans)}")

    position_counts = Counter()
    total_questions = 0
    pack_stats = {}

    for file_name in sorted(catalog_files):
        pack_path = packs_dir / file_name
        if not pack_path.exists():
            continue

        try:
            pack_data = json.loads(pack_path.read_text(encoding="utf-8"))
        except Exception as e:
            errors.append(f"Failed to parse {file_name}: {e}")
            continue

        pid = pack_data.get("id")
        if not pid:
            errors.append(f"{file_name} missing 'id'")
        items = pack_data.get("items", [])
        if not isinstance(items, list):
            errors.append(f"{file_name}: 'items' must be a list")
            continue

        pack_pos_counts = Counter()
        seen_prompts = set()

        for exp_seq, item in enumerate(items, start=1):
            total_questions += 1
            seq = item.get("sequence")
            if seq != exp_seq:
                errors.append(f"{file_name} item {exp_seq}: sequence mismatch (got {seq})")

            prompt = str(item.get("prompt", "")).strip()
            if not prompt:
                errors.append(f"{file_name} item {exp_seq}: empty prompt")
            elif prompt in seen_prompts:
                errors.append(f"{file_name} item {exp_seq}: duplicate prompt in pack: '{prompt[:30]}...'")
            seen_prompts.add(prompt)

            options = item.get("options", [])
            if not isinstance(options, list) or len(options) != 4:
                errors.append(f"{file_name} item {exp_seq}: options must be a list of 4 items")
                continue

            opt_ids = [opt.get("id") for opt in options if isinstance(opt, dict)]
            if opt_ids != ["a", "b", "c", "d"]:
                errors.append(f"{file_name} item {exp_seq}: options ids must be ordered ['a', 'b', 'c', 'd'], got {opt_ids}")

            opt_texts = [str(opt.get("text", "")).strip() for opt in options if isinstance(opt, dict)]
            if any(not t for t in opt_texts):
                errors.append(f"{file_name} item {exp_seq}: empty option text")
            if len(set(opt_texts)) != 4:
                errors.append(f"{file_name} item {exp_seq}: duplicate option texts in {opt_texts}")

            correct_ids = item.get("correctOptionIds", [])
            if not isinstance(correct_ids, list) or len(correct_ids) != 1 or correct_ids[0] not in ["a", "b", "c", "d"]:
                errors.append(f"{file_name} item {exp_seq}: invalid correctOptionIds (must be single ['a'|'b'|'c'|'d'])")
            else:
                cid = correct_ids[0]
                position_counts[cid] += 1
                pack_pos_counts[cid] += 1

            expl = str(item.get("explanation", "")).strip()
            if not expl:
                errors.append(f"{file_name} item {exp_seq}: missing or empty explanation")

        pack_stats[pid or file_name] = {
            "total_items": len(items),
            "distribution": dict(pack_pos_counts)
        }

    # Corpus-level first-option tell: same 20–30% band as
    # validate_mc_position_tells.py. Per-pack 3/10 = 30% is allowed;
    # the pre-fix video A=32.9% leak is not an Arena concern here, but a
    # 180-item Arena corpus skewed the same way must fail.
    errors.extend(
        slot_share_errors(
            "Assessment Arena overall",
            position_counts,
            total_questions,
        )
    )

    metrics = {
        "total_packs": len(catalog_packs),
        "total_questions": total_questions,
        "overall_distribution": dict(position_counts),
        "pack_stats": pack_stats,
        "error_count": len(errors)
    }

    is_valid = len(errors) == 0
    return is_valid, errors, metrics


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Assessment Arena packs")
    parser.add_argument(
        "--dir",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "app/src/main/assets/assessment-packs",
        help="Path to assessment-packs directory"
    )
    parser.add_argument("--json", action="store_true", help="Output JSON results")
    args = parser.parse_args()

    is_valid, errors, metrics = validate_arena(args.dir)

    if args.json:
        output = {
            "valid": is_valid,
            "error_count": len(errors),
            "errors": errors,
            "metrics": metrics
        }
        print(json.dumps(output, indent=2))
    else:
        if is_valid:
            print(f"PASS: All {metrics['total_packs']} Assessment Arena packs ({metrics['total_questions']} questions) are valid.")
            print(f"Position Distribution: {metrics['overall_distribution']}")
        else:
            print(f"FAIL: Found {len(errors)} validation errors in Assessment Arena packs:")
            for err in errors:
                print(f"  - {err}")

    return 0 if is_valid else 1


if __name__ == "__main__":
    sys.exit(main())

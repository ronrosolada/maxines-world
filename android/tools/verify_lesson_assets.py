#!/usr/bin/env python3
"""Audit every lesson-to-graphic reference in the bundled content pack.

The check is intentionally read-only. It validates lesson assetId references,
legacy assetSpecs, SVG XML/viewBox data, orphan files, and structural duplicate
artwork. Use --render when CairoSVG is installed to verify that every SVG can
actually rasterize.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"
VECTORS = ROOT / "app/src/main/assets/content-pack/month-01/assets/vectors"
SVG_NS = "{http://www.w3.org/2000/svg}"
TITLE_DESC_RE = re.compile(r"<(title|desc)\b[^>]*>.*?</\1>", re.DOTALL)


def audit(render: bool = False) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    references: set[str] = set()
    subjects: Counter[str] = Counter()
    lesson_paths = sorted(LESSONS.glob("*.json"))
    hotspot_count = 0
    null_activity_asset_ids = 0
    asset_spec_count = 0
    missing_visual_scene = 0

    for lesson_path in lesson_paths:
        try:
            lesson = json.loads(lesson_path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"{lesson_path.name}: invalid JSON: {exc}")
            continue
        if not isinstance(lesson, dict):
            errors.append(f"{lesson_path.name}: lesson root is not an object")
            continue

        subject = str(lesson.get("subject", "")).lower().replace("_", "-")
        subjects[subject] += 1
        activities = lesson.get("activities", [])
        hotspots = [a for a in activities if isinstance(a, dict) and a.get("type") == "HOTSPOT_IMAGE"]
        hotspot_count += len(hotspots)
        if len(hotspots) != 1:
            errors.append(f"{lesson_path.name}: expected exactly one HOTSPOT_IMAGE, found {len(hotspots)}")
        for activity in activities:
            if not isinstance(activity, dict):
                errors.append(f"{lesson_path.name}: non-object activity")
                continue
            asset_id = activity.get("assetId")
            if asset_id is None:
                null_activity_asset_ids += 1
            elif not isinstance(asset_id, str) or not asset_id:
                errors.append(f"{lesson_path.name}: invalid activity assetId {asset_id!r}")
            else:
                references.add(asset_id)
            if activity.get("type") == "HOTSPOT_IMAGE":
                content = activity.get("content")
                if isinstance(content, dict) and "visualScene" not in content:
                    missing_visual_scene += 1

        specs = lesson.get("assetSpecs", [])
        if not isinstance(specs, list):
            errors.append(f"{lesson_path.name}: assetSpecs is not a list")
            specs = []
        for spec in specs:
            asset_spec_count += 1
            if not isinstance(spec, dict):
                errors.append(f"{lesson_path.name}: assetSpecs contains a non-object")
                continue
            asset_id = spec.get("assetId")
            if not isinstance(asset_id, str) or not asset_id:
                errors.append(f"{lesson_path.name}: assetSpecs has invalid assetId {asset_id!r}")
                continue
            references.add(asset_id)
            expected_path = f"assets/vectors/{asset_id}.svg"
            if spec.get("path") != expected_path:
                errors.append(f"{lesson_path.name}: assetSpec path mismatch for {asset_id}")
            for field in ("prompt", "contentDescription"):
                if not isinstance(spec.get(field), str) or not spec[field].strip():
                    errors.append(f"{lesson_path.name}: assetSpec {asset_id} lacks {field}")

    svg_paths = sorted(VECTORS.glob("*.svg"))
    svg_stems = {path.stem for path in svg_paths}
    missing = sorted(references - svg_stems)
    orphaned = sorted(svg_stems - references)
    errors.extend(f"missing SVG for assetId {asset_id}" for asset_id in missing)
    errors.extend(f"orphan SVG {asset_id}.svg" for asset_id in orphaned)

    malformed: list[str] = []
    render_failures: list[str] = []
    normalized: defaultdict[str, list[str]] = defaultdict(list)
    renderer = None
    if render:
        try:
            import cairosvg as renderer  # type: ignore[assignment]
        except ImportError:
            errors.append("--render requested but cairosvg is not installed")

    for path in svg_paths:
        try:
            root = ET.parse(path).getroot()
            if root.tag != f"{SVG_NS}svg":
                malformed.append(f"{path.name}: root element is not SVG")
            if not root.attrib.get("viewBox"):
                malformed.append(f"{path.name}: missing viewBox")
            if renderer is not None:
                renderer.svg2png(bytestring=path.read_bytes(), output_width=128, output_height=72)
        except Exception as exc:
            if renderer is not None and "cairosvg" in sys.modules:
                render_failures.append(f"{path.name}: {exc}")
            else:
                malformed.append(f"{path.name}: {exc}")
        normalized_text = TITLE_DESC_RE.sub("", path.read_text(encoding="utf-8"))
        normalized[hashlib.sha256(normalized_text.encode("utf-8")).hexdigest()].append(path.name)

    errors.extend(f"malformed SVG: {item}" for item in malformed)
    errors.extend(f"SVG render failure: {item}" for item in render_failures)
    duplicate_groups = [sorted(names) for names in normalized.values() if len(names) > 1]
    errors.extend("duplicate normalized SVGs: " + ", ".join(group) for group in duplicate_groups)

    report = {
        "lesson_count": len(lesson_paths),
        "subjects": dict(sorted(subjects.items())),
        "hotspot_image_count": hotspot_count,
        "hotspot_content_without_visualScene": missing_visual_scene,
        "null_activity_asset_ids": null_activity_asset_ids,
        "asset_spec_count": asset_spec_count,
        "referenced_asset_count": len(references),
        "svg_count": len(svg_paths),
        "missing_assets": missing,
        "orphan_assets": orphaned,
        "malformed_assets": malformed,
        "render_failures": render_failures,
        "normalized_duplicate_groups": duplicate_groups,
        "error_count": len(errors),
    }
    return report, errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--render", action="store_true", help="also rasterize every SVG with CairoSVG")
    parser.add_argument("--check", action="store_true", help="return non-zero when the audit finds errors")
    args = parser.parse_args()
    report, errors = audit(render=args.render)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    if errors and not args.check:
        print("\n".join(errors), file=sys.stderr)
    return 1 if args.check and errors else 0


if __name__ == "__main__":
    raise SystemExit(main())

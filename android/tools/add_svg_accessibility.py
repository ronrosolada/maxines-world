#!/usr/bin/env python3
"""Add meaningful screen-reader metadata to lesson SVGs without reformatting artwork."""
from __future__ import annotations

import argparse
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VECTOR_DIR = ROOT / "app/src/main/assets/content-pack/month-01/assets/vectors"
LESSON_DIR = ROOT / "app/src/main/assets/content-pack/month-01/lessons"


def source_metadata(svg_path: Path) -> tuple[str, str, str]:
    lesson_id = svg_path.stem.removesuffix("-visual")
    asset_id = svg_path.stem
    source = LESSON_DIR / f"{lesson_id}.json"
    payload = json.loads(source.read_text(encoding="utf-8"))
    title = str(payload.get("title") or lesson_id.replace("-", " "))
    objective = str(payload.get("objective") or "Lesson illustration")
    visual_context = ""
    for activity in payload.get("activities", []):
        if activity.get("assetId") != asset_id:
            continue
        if activity.get("type") == "HOTSPOT_IMAGE":
            instruction = str(activity.get("instruction") or "").strip()
            examples = activity.get("content", {}).get("examples", [])
            examples = [str(example).strip() for example in examples if str(example).strip()]
            details = ", ".join(examples[:8])
            visual_context = " ".join(
                part for part in (
                    instruction,
                    f"It shows: {details}." if details else "",
                ) if part
            )
            break
    return title, objective, visual_context


def accessible_svg(text: str, svg_path: Path) -> str:
    title, objective, visual_context = source_metadata(svg_path)
    key = re.sub(r"[^a-z0-9]+", "-", svg_path.stem.lower()).strip("-")
    title_id = f"svg-title-{key}"
    desc_id = f"svg-desc-{key}"
    escaped_title = html.escape(f"Lesson visual: {title}", quote=False)
    description = f"Illustration for {title}. {objective}"
    if visual_context:
        description += f" {visual_context}"
    escaped_desc = html.escape(description, quote=False)

    opening = re.search(r"<svg\b[^>]*>", text, flags=re.DOTALL)
    if not opening:
        raise ValueError(f"No SVG root element: {svg_path}")
    root = opening.group(0)
    root = re.sub(r"\s+role=(?:\"[^\"]*\"|'[^']*')", "", root)
    root = re.sub(r"\s+aria-labelledby=(?:\"[^\"]*\"|'[^']*')", "", root)
    root = root[:-1] + f' role="img" aria-labelledby="{title_id} {desc_id}">'

    body = text[opening.end():]
    body = re.sub(
        r"\s*<title\b[^>]*>.*?</title>\s*<desc\b[^>]*>.*?</desc>\s*",
        "",
        body,
        count=1,
        flags=re.DOTALL,
    ).lstrip()
    metadata = (
        f'<title id="{title_id}">{escaped_title}</title>\n'
        f'  <desc id="{desc_id}">{escaped_desc}</desc>\n'
    )
    return text[:opening.start()] + root + "\n  " + metadata + body


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if a file would change")
    args = parser.parse_args()
    changed = []
    for path in sorted(VECTOR_DIR.glob("*.svg")):
        original = path.read_text(encoding="utf-8")
        updated = accessible_svg(original, path)
        if updated != original:
            changed.append(path.name)
            if not args.check:
                path.write_text(updated, encoding="utf-8")
    if args.check and changed:
        print(f"{len(changed)} SVGs need accessibility metadata")
        return 1
    print(f"{'Would update' if args.check else 'Updated'} {len(changed)} SVGs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

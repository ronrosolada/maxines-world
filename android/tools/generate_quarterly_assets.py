#!/usr/bin/env python3
"""Generate deterministic topic-grounded SVG boards for quarterly lessons.

This generator creates the optional visual asset referenced by each quarterly
lesson's HOTSPOT_IMAGE activity. It does not modify lesson JSON or Android UI.
The lesson's text/options remain the accessible fallback; SVGs provide visual
anchors instead of the renderer's plain labeled boxes.

Usage:
    python3 tools/generate_quarterly_assets.py --dry-run
    python3 tools/generate_quarterly_assets.py
    python3 tools/generate_quarterly_assets.py --check
"""
from __future__ import annotations

import argparse
import html
import json
import re
from pathlib import Path
from typing import Callable

from content_review import canonical_subject, profile_for

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"
ASSETS = ROOT / "app/src/main/assets/content-pack/month-01/assets/vectors"
SVG_NS = "http://www.w3.org/2000/svg"

PALETTES = {
    "english": ("#FFF4D6", "#8DD3C7", "#2E6F6D", "#F2A65A"),
    "mathematics": ("#EAF6FF", "#80B1D3", "#315A7D", "#F2C14E"),
    "science": ("#F4FFE4", "#B3DE69", "#39734A", "#F28E8E"),
    "filipino": ("#FFF0F5", "#F6A6C1", "#7A3152", "#8EC6FF"),
    "araling-panlipunan": ("#FFF1E6", "#FFB26B", "#7A4528", "#6CB4A8"),
    "gmrc": ("#F1EEFF", "#B8A4E8", "#503B78", "#F4C95D"),
    "makabansa": ("#EFFFF8", "#72C9A3", "#24624A", "#F08A5D"),
}


def esc(value: object) -> str:
    return html.escape(str(value), quote=True)


def el(tag: str, attrs: dict[str, object], body: str = "") -> str:
    rendered = " ".join(f'{key}="{esc(value)}"' for key, value in attrs.items())
    return f"<{tag} {rendered}>{body}</{tag}>"


def circle(cx: int, cy: int, r: int, fill: str, stroke: str, width: int = 5) -> str:
    return el("circle", {"cx": cx, "cy": cy, "r": r, "fill": fill, "stroke": stroke, "stroke-width": width})


def rect(x: int, y: int, w: int, h: int, fill: str, stroke: str, radius: int = 16, width: int = 5) -> str:
    return el("rect", {"x": x, "y": y, "width": w, "height": h, "rx": radius, "fill": fill, "stroke": stroke, "stroke-width": width})


def line(x1: int, y1: int, x2: int, y2: int, stroke: str, width: int = 6, dash: str | None = None) -> str:
    attrs: dict[str, object] = {"x1": x1, "y1": y1, "x2": x2, "y2": y2, "stroke": stroke, "stroke-width": width, "stroke-linecap": "round"}
    if dash:
        attrs["stroke-dasharray"] = dash
    return el("line", attrs)


def path(d: str, fill: str, stroke: str, width: int = 5) -> str:
    return el("path", {"d": d, "fill": fill, "stroke": stroke, "stroke-width": width, "stroke-linejoin": "round", "stroke-linecap": "round"})


def polygon(points: str, fill: str, stroke: str, width: int = 5) -> str:
    return el("polygon", {"points": points, "fill": fill, "stroke": stroke, "stroke-width": width, "stroke-linejoin": "round"})


def panel(x: int, y: int, fill: str, stroke: str, body: str) -> str:
    return rect(x, y, 136, 136, fill, stroke, radius=24, width=4) + body


def icon_point(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx, cy, 12, accent, ink, 4) + line(cx - 45, cy + 32, cx + 45, cy + 32, ink, 5)


def icon_geometry(cx: int, cy: int, accent: str, ink: str) -> str:
    return line(cx - 48, cy + 28, cx + 48, cy - 28, ink, 7) + circle(cx - 48, cy + 28, 9, accent, ink, 3) + circle(cx + 48, cy - 28, 9, accent, ink, 3)


def icon_book(cx: int, cy: int, accent: str, ink: str) -> str:
    return path(f"M{cx},{cy-38} Q{cx-28},{cy-50} {cx-52},{cy-35} L{cx-52},{cy+38} Q{cx-25},{cy+25} {cx},{cy+38} Z", "#FFFDF5", ink, 4) + path(f"M{cx},{cy-38} Q{cx+28},{cy-50} {cx+52},{cy-35} L{cx+52},{cy+38} Q{cx+25},{cy+25} {cx},{cy+38} Z", "#FFFDF5", ink, 4) + line(cx, cy - 38, cx, cy + 38, accent, 4)


def icon_people(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx - 28, cy - 28, 15, accent, ink, 4) + circle(cx + 28, cy - 28, 15, accent, ink, 4) + path(f"M{cx-58},{cy+43} Q{cx-28},{cy-2} {cx+2},{cy+43} Z", accent, ink, 4) + path(f"M{cx-2},{cy+43} Q{cx+28},{cy-2} {cx+58},{cy+43} Z", accent, ink, 4)


def icon_tree(cx: int, cy: int, accent: str, ink: str) -> str:
    return rect(cx - 11, cy + 8, 22, 45, "#9A6A45", ink, radius=6, width=3) + circle(cx, cy - 20, 38, accent, ink, 5) + circle(cx - 30, cy - 4, 26, accent, ink, 5) + circle(cx + 30, cy - 4, 26, accent, ink, 5)


def icon_house(cx: int, cy: int, accent: str, ink: str) -> str:
    return polygon(f"{cx-52},{cy-4} {cx},{cy-48} {cx+52},{cy-4}", accent, ink, 5) + rect(cx - 42, cy - 4, 84, 56, "#FFFDF5", ink, radius=5, width=4) + rect(cx - 10, cy + 18, 20, 34, "#9A6A45", ink, radius=3, width=3)


def icon_sun(cx: int, cy: int, accent: str, ink: str) -> str:
    rays = "".join(line(cx + dx * 1, cy + dy * 1, cx + dx * 1.55, cy + dy * 1.55, ink, 5) for dx, dy in ((0, -44), (31, -31), (44, 0), (31, 31), (0, 44), (-31, 31), (-44, 0), (-31, -31)))
    return rays + circle(cx, cy, 30, accent, ink, 5)


def icon_leaf(cx: int, cy: int, accent: str, ink: str) -> str:
    return path(f"M{cx},{cy+48} Q{cx-50},{cy+5} {cx-18},{cy-40} Q{cx+32},{cy-42} {cx+42},{cy-5} Q{cx+30},{cy+28} {cx},{cy+48} Z", accent, ink, 5) + line(cx - 28, cy + 30, cx + 25, cy - 25, ink, 4)


def icon_bars(cx: int, cy: int, accent: str, ink: str) -> str:
    return line(cx - 54, cy + 45, cx + 54, cy + 45, ink, 6) + line(cx - 54, cy - 45, cx - 54, cy + 45, ink, 6) + rect(cx - 38, cy + 8, 20, 37, accent, ink, radius=4, width=3) + rect(cx - 8, cy - 18, 20, 63, accent, ink, radius=4, width=3) + rect(cx + 22, cy - 40, 20, 85, accent, ink, radius=4, width=3)


def icon_array(cx: int, cy: int, accent: str, ink: str) -> str:
    return "".join(circle(cx - 30 + col * 30, cy - 30 + row * 30, 9, accent, ink, 3) for row in range(3) for col in range(3))


def icon_fraction(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx, cy, 45, "#FFFDF5", ink, 5) + path(f"M{cx},{cy} L{cx},{cy-45} A45,45 0 0 1 {cx+45},{cy} Z", accent, ink, 4) + line(cx - 45, cy, cx + 45, cy, ink, 3)


def icon_container(cx: int, cy: int, accent: str, ink: str) -> str:
    return path(f"M{cx-35},{cy-40} L{cx+35},{cy-40} L{cx+27},{cy+45} L{cx-27},{cy+45} Z", "#D9F3FF", ink, 5) + path(f"M{cx-25},{cy+18} Q{cx},{cy+4} {cx+25},{cy+18} L{cx+27},{cy+45} L{cx-27},{cy+45} Z", accent, ink, 3)


def icon_coin(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx, cy, 42, accent, ink, 5) + circle(cx, cy, 28, "none", ink, 3) + line(cx, cy - 17, cx, cy + 17, ink, 4)


def icon_shapes(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx - 28, cy - 15, 22, accent, ink, 4) + rect(cx + 6, cy - 37, 44, 44, "#FFFDF5", ink, radius=5, width=4) + polygon(f"{cx-48},{cy+47} {cx-20},{cy+1} {cx+8},{cy+47}", accent, ink, 4)


def icon_arrows(cx: int, cy: int, accent: str, ink: str) -> str:
    return rect(cx - 45, cy - 25, 38, 38, accent, ink, radius=5, width=4) + line(cx + 5, cy - 5, cx + 50, cy - 5, ink, 6) + path(f"M{cx+35},{cy-22} L{cx+55},{cy-5} L{cx+35},{cy+12} Z", ink, ink, 2) + rect(cx + 18, cy + 20, 38, 38, "#FFFDF5", ink, radius=5, width=4)


def icon_sound(cx: int, cy: int, accent: str, ink: str) -> str:
    return polygon(f"{cx-50},{cy-14} {cx-22},{cy-14} {cx+5},{cy-40} {cx+5},{cy+40} {cx-22},{cy+14} {cx-50},{cy+14}", accent, ink, 4) + path(f"M{cx+22},{cy-30} Q{cx+55},{cy} {cx+22},{cy+30}", "none", ink, 6) + path(f"M{cx+40},{cy-48} Q{cx+85},{cy} {cx+40},{cy+48}", "none", ink, 5)


def icon_star(cx: int, cy: int, accent: str, ink: str) -> str:
    points = []
    import math
    for i in range(10):
        angle = -math.pi / 2 + i * math.pi / 5
        radius = 45 if i % 2 == 0 else 20
        points.append(f"{cx + radius * math.cos(angle):.1f},{cy + radius * math.sin(angle):.1f}")
    return polygon(" ".join(points), accent, ink, 5)


ICON_BY_KEY: dict[str, list[Callable[[int, int, str, str], str]]] = {
    "picture": [icon_tree, icon_house, icon_people, icon_shapes],
    "characters": [icon_people, icon_house, icon_book, icon_tree],
    "ending": [icon_book, icon_star, icon_house, icon_tree],
    "diary": [icon_book, icon_star, icon_people, icon_house],
    "telling": [icon_book, icon_shapes, icon_star, icon_people],
    "nouns": [icon_people, icon_house, icon_tree, icon_book],
    "plural_s": [icon_shapes, icon_people, icon_tree, icon_book],
    "plural_es": [icon_shapes, icon_book, icon_star, icon_people],
    "vowels": [icon_book, icon_shapes, icon_star, icon_sound],
    "syllable": [icon_sound, icon_book, icon_shapes, icon_star],
    "be_verb": [icon_people, icon_book, icon_star, icon_shapes],
    "tense": [icon_sun, icon_tree, icon_star, icon_book],
    "blend": [icon_book, icon_shapes, icon_sound, icon_star],
    "digraph": [icon_book, icon_sound, icon_shapes, icon_star],
    "sight": [icon_book, icon_star, icon_shapes, icon_people],
    "possessive": [icon_book, icon_people, icon_house, icon_star],
    "cause_effect": [icon_tree, icon_tree, icon_sun, icon_star],
    "main_detail": [icon_book, icon_shapes, icon_star, icon_tree],
    "graph": [icon_bars, icon_shapes, icon_star, icon_book],
    "geometry": [icon_point, icon_geometry, icon_shapes, icon_arrows],
    "area": [icon_shapes, icon_array, icon_geometry, icon_bars],
    "length": [icon_geometry, icon_shapes, icon_bars, icon_arrows],
    "ordinal": [icon_star, icon_shapes, icon_people, icon_bars],
    "round": [icon_shapes, icon_arrows, icon_bars, icon_star],
    "compare": [icon_arrows, icon_bars, icon_shapes, icon_star],
    "order": [icon_bars, icon_arrows, icon_shapes, icon_star],
    "capacity_measure": [icon_container, icon_container, icon_bars, icon_arrows],
    "capacity_estimate": [icon_container, icon_bars, icon_shapes, icon_star],
    "capacity_compare": [icon_container, icon_container, icon_arrows, icon_bars],
    "addition_word": [icon_array, icon_bars, icon_shapes, icon_star],
    "subtraction": [icon_bars, icon_arrows, icon_shapes, icon_star],
    "difference": [icon_arrows, icon_bars, icon_shapes, icon_star],
    "addition": [icon_array, icon_bars, icon_shapes, icon_star],
    "multi_add": [icon_array, icon_bars, icon_shapes, icon_star],
    "bar_graph": [icon_bars, icon_bars, icon_shapes, icon_star],
    "probability": [icon_coin, icon_shapes, icon_star, icon_arrows],
    "multiplication_properties": [icon_array, icon_arrows, icon_shapes, icon_bars],
    "multiplication": [icon_array, icon_array, icon_bars, icon_shapes],
    "product_estimate": [icon_array, icon_bars, icon_arrows, icon_star],
    "pattern": [icon_shapes, icon_shapes, icon_star, icon_arrows],
    "division": [icon_array, icon_arrows, icon_shapes, icon_bars],
    "fraction": [icon_fraction, icon_fraction, icon_shapes, icon_array],
    "transformation": [icon_arrows, icon_shapes, icon_geometry, icon_star],
    "materials": [icon_shapes, icon_container, icon_star, icon_arrows],
    "living": [icon_tree, icon_people, icon_leaf, icon_sun],
    "motion": [icon_arrows, icon_shapes, icon_star, icon_geometry],
    "light_sound": [icon_sun, icon_sound, icon_star, icon_shapes],
    "sky_weather": [icon_sun, icon_tree, icon_tree, icon_star],
    "greetings": [icon_people, icon_star, icon_book, icon_house],
    "root": [icon_tree, icon_book, icon_shapes, icon_star],
    "sentence_parts": [icon_book, icon_people, icon_shapes, icon_arrows],
    "fluency": [icon_book, icon_sound, icon_star, icon_arrows],
    "writing": [icon_book, icon_shapes, icon_star, icon_people],
    "context": [icon_book, icon_shapes, icon_people, icon_star],
    "summary": [icon_book, icon_shapes, icon_star, icon_arrows],
    "paragraph": [icon_book, icon_people, icon_house, icon_tree],
    "word_use": [icon_book, icon_shapes, icon_star, icon_people],
    "faith": [icon_star, icon_sun, icon_people, icon_house],
    "respect": [icon_people, icon_star, icon_house, icon_book],
    "care": [icon_people, icon_leaf, icon_house, icon_star],
    "responsibility": [icon_people, icon_house, icon_tree, icon_star],
    "discipline": [icon_arrows, icon_star, icon_book, icon_shapes],
    "cooperation": [icon_people, icon_house, icon_star, icon_arrows],
    "honesty": [icon_star, icon_book, icon_people, icon_shapes],
    "initiative": [icon_star, icon_arrows, icon_people, icon_tree],
    "patience": [icon_tree, icon_star, icon_book, icon_sun],
    "citizenship": [icon_house, icon_people, icon_tree, icon_star],
    "judgment": [icon_shapes, icon_arrows, icon_book, icon_star],
    "gratitude": [icon_star, icon_people, icon_sun, icon_house],
    "self_confidence": [icon_star, icon_people, icon_arrows, icon_book],
    "music": [icon_sound, icon_star, icon_people, icon_shapes],
    "identity": [icon_people, icon_star, icon_house, icon_tree],
    "active_citizen": [icon_people, icon_house, icon_tree, icon_star],
    "culture": [icon_house, icon_people, icon_star, icon_book],
    "community_history": [icon_house, icon_tree, icon_book, icon_people],
}


def icon_cloud(cx: int, cy: int, accent: str, ink: str) -> str:
    return circle(cx - 25, cy + 2, 23, "#FFFDF5", ink, 4) + circle(cx + 4, cy - 13, 32, "#FFFDF5", ink, 4) + circle(cx + 34, cy + 3, 22, "#FFFDF5", ink, 4) + rect(cx - 47, cy + 2, 94, 33, "#FFFDF5", ink, radius=12, width=4) + line(cx - 28, cy + 55, cx - 38, cy + 75, accent, 5) + line(cx, cy + 55, cx - 10, cy + 75, accent, 5) + line(cx + 28, cy + 55, cx + 18, cy + 75, accent, 5)

# Resolve the forward reference used in ICON_BY_KEY.
ICON_BY_KEY["cause_effect"] = [icon_cloud, icon_tree, icon_sun, icon_star]
ICON_BY_KEY["sky_weather"] = [icon_sun, icon_cloud, icon_tree, icon_star]


def board_svg(lesson: dict[str, object]) -> str:
    lesson_id = str(lesson["lessonId"])
    subject = canonical_subject(str(lesson.get("subject", "")))
    profile = profile_for(lesson)
    key = profile["key"]
    bg, accent, ink, secondary = PALETTES.get(subject, PALETTES["english"])
    icons = ICON_BY_KEY.get(key, [icon_book, icon_shapes, icon_star, icon_people])
    rotation = sum(ord(char) for char in lesson_id) % len(icons)
    icons = icons[rotation:] + icons[:rotation]
    positions = [(88, 84), (244, 84), (400, 84), (556, 84)]
    cards = []
    for index, (cx, cy) in enumerate(positions):
        card_fill = "#FFFDF5" if index % 2 == 0 else bg
        cards.append(panel(cx - 68, cy, card_fill, ink, icons[index](cx, cy + 68, accent if index % 2 == 0 else secondary, ink)))
    # The lower path visually connects the four observations without implying a
    # specific answer order; the activity renderer owns interaction order.
    connector = path("M72 286 Q180 238 288 286 T568 286", "none", secondary, 12)
    dots = "".join(circle(x, 286, 11, accent, ink, 3) for x in (88, 244, 400, 556))
    title = f"{lesson.get('title', lesson_id)} visual board"
    desc = f"Topic-grounded visual board for {lesson.get('title', lesson_id)}. Four visual clues support the lesson objective without replacing the accessible text content."
    return (
        f'<svg xmlns="{SVG_NS}" width="640" height="360" viewBox="0 0 640 360" role="img" aria-labelledby="title desc">'
        f"<title id=\"title\">{esc(title)}</title><desc id=\"desc\">{esc(desc)}</desc>"
        f"<rect width=\"640\" height=\"360\" rx=\"40\" fill=\"{bg}\"/>"
        f"<circle cx=\"590\" cy=\"42\" r=\"18\" fill=\"{secondary}\" opacity=\"0.65\"/>"
        f"<circle cx=\"52\" cy=\"44\" r=\"12\" fill=\"{accent}\" opacity=\"0.65\"/>"
        + connector + dots + "".join(cards)
        + "</svg>\n"
    )


def quarterly_lessons() -> list[dict[str, object]]:
    lessons = []
    for path in sorted(LESSONS.glob("*.json")):
        lesson = json.loads(path.read_text(encoding="utf-8"))
        if "-g3-q" in str(lesson.get("lessonId", "")):
            lessons.append(lesson)
    return lessons


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true", help="fail if any quarterly asset is missing or malformed")
    args = parser.parse_args()
    lessons = quarterly_lessons()
    missing = []
    malformed = []
    generated = []
    for lesson in lessons:
        asset_id = f"{lesson['lessonId']}-visual"
        path = ASSETS / f"{asset_id}.svg"
        if not path.exists():
            missing.append(path)
            if not args.dry_run and not args.check:
                path.write_text(board_svg(lesson), encoding="utf-8")
                generated.append(path)
        else:
            text = path.read_text(encoding="utf-8")
            if not text.startswith("<svg") and not text.startswith("<?xml"):
                malformed.append(path)
    print(json.dumps({
        "quarterly_lessons": len(lessons),
        "missing_before_run": len(missing),
        "generated": len(generated),
        "malformed_existing": len(malformed),
        "asset_dir": str(ASSETS),
    }, indent=2))
    if args.check and (missing or malformed):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

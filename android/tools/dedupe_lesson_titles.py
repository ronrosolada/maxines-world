#!/usr/bin/env python3
"""Make repeated lesson titles navigable without changing lesson content.

Only the JSON ``title`` field is changed. The qualifier is derived from the
stable lesson ID, so the operation is deterministic and idempotent.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path

LESSON_DIR = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"
TITLE_LINE = re.compile(r'^(?P<prefix>\s*"title"\s*:\s*)(?P<value>"(?:\\.|[^"\\])*")(?P<suffix>\s*,?\s*)$')
QUARTER_ID = re.compile(r"-q(?P<quarter>\d+)-w(?P<week>\d+)-d(?P<day>\d+)$")
MODULE_ID = re.compile(r"-m(?P<module>\d+)-d(?P<day>\d+)$")


def lesson_files() -> list[Path]:
    return sorted(LESSON_DIR.glob("*.json"))


def qualifier(lesson_id: str) -> str:
    match = QUARTER_ID.search(lesson_id)
    if match:
        return (
            f"Q{int(match['quarter'])} "
            f"W{int(match['week']):02d} "
            f"D{int(match['day']):02d}"
        )
    match = MODULE_ID.search(lesson_id)
    if match:
        return f"M{int(match['module']):02d} D{int(match['day']):02d}"
    raise ValueError(f"unsupported lesson ID format: {lesson_id}")


def load_lessons() -> list[tuple[Path, dict]]:
    return [(path, json.loads(path.read_text(encoding="utf-8"))) for path in lesson_files()]


def duplicate_titles(lessons: list[tuple[Path, dict]]) -> set[str]:
    counts = Counter(lesson.get("title", "") for _, lesson in lessons)
    return {title for title, count in counts.items() if title and count > 1}


def planned_titles(lessons: list[tuple[Path, dict]]) -> dict[Path, str]:
    duplicates = duplicate_titles(lessons)
    planned = {}
    for path, lesson in lessons:
        title = lesson.get("title", "")
        if title in duplicates and " · Q" not in title and " · M" not in title:
            planned[path] = f"{title} · {qualifier(lesson['lessonId'])}"
        else:
            planned[path] = title
    return planned


def rewrite_title(path: Path, new_title: str) -> None:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    replaced = False
    output = []
    for line in lines:
        match = TITLE_LINE.match(line.rstrip("\r\n"))
        if match and not replaced:
            newline = line[len(line.rstrip("\r\n")) :]
            encoded = json.dumps(new_title, ensure_ascii=False)
            line = f"{match['prefix']}{encoded}{match['suffix']}{newline}"
            replaced = True
        output.append(line)
    if not replaced:
        raise ValueError(f"title field not found in {path}")
    path.write_text("".join(output), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report planned changes without writing")
    args = parser.parse_args()

    lessons = load_lessons()
    planned = planned_titles(lessons)
    changes = [(path, lesson["title"], planned[path]) for path, lesson in lessons if lesson["title"] != planned[path]]
    remaining = Counter(planned.values())
    duplicate_after = {title: count for title, count in remaining.items() if count > 1}

    print(f"lesson files: {len(lessons)}")
    print(f"duplicate title groups before: {len(duplicate_titles(lessons))}")
    print(f"title fields to change: {len(changes)}")
    print(f"duplicate title groups after: {len(duplicate_after)}")
    if changes:
        for path, old, new in changes[:12]:
            print(f"{path.name}: {old} -> {new}")
        if len(changes) > 12:
            print(f"... {len(changes) - 12} more")

    if args.check:
        return 0 if not duplicate_after else 1

    for path, _, new in changes:
        rewrite_title(path, new)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

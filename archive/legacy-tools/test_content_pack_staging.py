#!/usr/bin/env python3
"""Tests for failure-safe, new-output content-pack staging."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from content_pack_staging import StageError, stage_lesson_transform  # noqa: E402


def write_lesson(root: Path, lesson_id: str, title: str) -> None:
    (root / f"{lesson_id}.json").write_text(
        json.dumps({"lessonId": lesson_id, "title": title}, indent=2) + "\n",
        encoding="utf-8",
    )


def tree_bytes(root: Path) -> dict[str, bytes]:
    return {
        str(path.relative_to(root)): path.read_bytes()
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


class ContentPackStagingTests(unittest.TestCase):
    def test_selection_counts_and_source_remains_unchanged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            output = root / "output"
            source.mkdir()
            write_lesson(source, "one", "One")
            write_lesson(source, "two", "Two")
            write_lesson(source, "three", "Three")
            before = tree_bytes(source)

            def transform(lesson: dict) -> dict:
                lesson["title"] += " changed"
                return lesson

            result = stage_lesson_transform(source, output, transform, selected_ids={"two"})

            self.assertEqual(3, result.source_count)
            self.assertEqual(1, result.selected_count)
            self.assertEqual(1, result.changed_count)
            self.assertEqual(before, tree_bytes(source))
            self.assertIn("changed", (output / "two.json").read_text(encoding="utf-8"))
            self.assertEqual(
                (source / "one.json").read_bytes(),
                (output / "one.json").read_bytes(),
            )

    def test_transform_failure_publishes_no_partial_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            output = root / "output"
            source.mkdir()
            write_lesson(source, "one", "One")
            write_lesson(source, "two", "Two")
            before = tree_bytes(source)

            def transform(lesson: dict) -> dict:
                if lesson["lessonId"] == "two":
                    raise ValueError("synthetic transform failure")
                lesson["title"] += " changed"
                return lesson

            with self.assertRaises(StageError):
                stage_lesson_transform(source, output, transform)

            self.assertFalse(output.exists())
            self.assertEqual(before, tree_bytes(source))

    def test_identity_transform_is_idempotent(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            first = root / "first"
            second = root / "second"
            source.mkdir()
            write_lesson(source, "one", "One")
            write_lesson(source, "two", "Two")

            stage_lesson_transform(source, first, lambda lesson: lesson)
            stage_lesson_transform(first, second, lambda lesson: lesson)

            self.assertEqual(tree_bytes(first), tree_bytes(second))

    def test_existing_destination_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            output = root / "output"
            source.mkdir()
            output.mkdir()
            marker = output / "marker"
            marker.write_text("keep", encoding="utf-8")
            write_lesson(source, "one", "One")

            with self.assertRaises(StageError):
                stage_lesson_transform(source, output, lambda lesson: lesson)

            self.assertEqual("keep", marker.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()

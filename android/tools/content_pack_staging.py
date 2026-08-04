#!/usr/bin/env python3
"""Failure-safe staging for deterministic content-pack transforms.

The live lesson tree is never written. A transform is evaluated for every
selected lesson in memory first, then the complete source tree is copied to a
temporary sibling directory and published with one atomic directory rename.
The destination must not already exist.
"""

from __future__ import annotations

import json
import os
import shutil
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable


class StageError(RuntimeError):
    """Raised when a pack cannot be staged without risking partial output."""


@dataclass(frozen=True)
class StageResult:
    source_count: int
    selected_count: int
    changed_count: int
    output_dir: Path


def _encode_json(value: dict) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def _encoded_transformed(value: dict | None) -> bytes:
    if value is None:
        raise StageError("selected lesson has no transformed value")
    try:
        return _encode_json(value)
    except (TypeError, ValueError) as exc:
        raise StageError(f"transformed lesson is not JSON-serializable: {exc}") from exc


def _lesson_files(source_dir: Path) -> list[Path]:
    if not source_dir.is_dir():
        raise StageError(f"source directory not found: {source_dir}")
    files = sorted(source_dir.glob("*.json"))
    if not files:
        raise StageError(f"source directory contains no JSON lessons: {source_dir}")
    return files


def stage_lesson_transform(
    source_dir: Path,
    output_dir: Path,
    transform: Callable[[dict], dict],
    selected_ids: Iterable[str] | None = None,
) -> StageResult:
    """Stage a transform into a new directory and publish it atomically.

    All source JSON is parsed and all selected transforms are completed before
    any temporary output is created. If parsing or transforming fails, the
    source and destination are unchanged. ``output_dir`` must be absent and
    its parent must already exist; refusing to create an ambiguous destination
    makes accidental in-place transforms harder.
    """
    source = Path(source_dir).resolve()
    output = Path(output_dir).resolve()
    files = _lesson_files(source)

    if source == output or source in output.parents:
        raise StageError("output directory must not be the source or inside it")
    if os.path.lexists(output):
        raise StageError(f"refusing to overwrite existing output: {output}")
    if not output.parent.is_dir():
        raise StageError(f"output parent directory not found: {output.parent}")

    requested = None if selected_ids is None else {str(value) for value in selected_ids}
    records: list[tuple[Path, bytes, dict, bool, dict | None]] = []
    seen_ids: set[str] = set()

    # Parse and transform everything before creating any output. This is the
    # important safety boundary for malformed input and partial transforms.
    for path in files:
        original = path.read_bytes()
        try:
            lesson = json.loads(original.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise StageError(f"invalid JSON in {path.name}: {exc}") from exc
        if not isinstance(lesson, dict):
            raise StageError(f"lesson root must be an object: {path.name}")
        lesson_id = lesson.get("lessonId")
        if not isinstance(lesson_id, str) or not lesson_id:
            raise StageError(f"lessonId missing or invalid: {path.name}")
        if lesson_id in seen_ids:
            raise StageError(f"duplicate lessonId: {lesson_id}")
        seen_ids.add(lesson_id)

        selected = requested is None or lesson_id in requested
        transformed = None
        if selected:
            try:
                transformed = transform(dict(lesson))
            except Exception as exc:  # noqa: BLE001 - wrap callback failures
                raise StageError(f"transform failed for {lesson_id}: {exc}") from exc
            if not isinstance(transformed, dict):
                raise StageError(f"transform returned non-object for {lesson_id}")
        records.append((path, original, lesson, selected, transformed))

    if requested is not None:
        missing = sorted(requested - seen_ids)
        if missing:
            raise StageError(f"selected lesson IDs not found: {', '.join(missing)}")

    changed_count = sum(
        1
        for _, original, _, selected, transformed in records
        if selected and _encoded_transformed(transformed) != original
    )
    temp_dir: Path | None = None
    try:
        temp_dir = Path(tempfile.mkdtemp(prefix=f".{output.name}.", dir=output.parent))
        shutil.copytree(source, temp_dir, dirs_exist_ok=True)
        for path, _, _, selected, transformed in records:
            if selected:
                staged_path = temp_dir / path.name
                staged_path.write_bytes(_encoded_transformed(transformed))

        # The destination was absent before staging. os.replace publishes the
        # complete tree in one filesystem operation; no partial lesson tree
        # is visible at the requested output path.
        os.replace(temp_dir, output)
        temp_dir = None
    except (OSError, shutil.Error) as exc:
        raise StageError(f"could not publish staged output {output}: {exc}") from exc
    finally:
        if temp_dir is not None and temp_dir.exists():
            shutil.rmtree(temp_dir, ignore_errors=True)

    return StageResult(
        source_count=len(records),
        selected_count=sum(1 for _, _, _, selected, _ in records if selected),
        changed_count=changed_count,
        output_dir=output,
    )


def atomic_write_text(path: Path, content: str) -> None:
    """Atomically replace one report file, leaving no truncated report."""
    destination = Path(path)
    if not destination.parent.is_dir():
        destination.parent.mkdir(parents=True, exist_ok=True)
    temp_path: Path | None = None
    try:
        fd, raw_temp = tempfile.mkstemp(prefix=f".{destination.name}.", dir=destination.parent)
        temp_path = Path(raw_temp)
        with os.fdopen(fd, "w", encoding="utf-8", newline="") as handle:
            handle.write(content)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_path, destination)
        temp_path = None
    finally:
        if temp_path is not None:
            temp_path.unlink(missing_ok=True)

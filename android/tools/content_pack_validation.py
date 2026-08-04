#!/usr/bin/env python3
"""Validate the Month 01 lesson pack without modifying source content.

Audit mode reports known soft issues (currently, missing assessment ``type``
fields) as warnings. ``--strict`` promotes those soft issues to errors. Both
modes fail on malformed JSON, broken lesson shape, invalid assessment answer
references, activity-order violations, and missing referenced vector assets.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any, Mapping

try:
    from content_pack_staging import atomic_write_text
except ImportError:  # pragma: no cover - supports package-style imports
    from .content_pack_staging import atomic_write_text


CANONICAL_ACTIVITY_TYPES = (
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
    "SEQUENCE_BUILDER",
)


@dataclass(frozen=True)
class Finding:
    severity: str
    category: str
    path: str
    message: str


@dataclass
class ValidationReport:
    findings: list[Finding] = field(default_factory=list)
    files_seen: int = 0
    lesson_count: int = 0

    def add(self, severity: str, category: str, path: Path | str, message: str) -> None:
        self.findings.append(Finding(severity, category, str(path), message))

    @property
    def errors(self) -> list[Finding]:
        return [finding for finding in self.findings if finding.severity == "error"]

    @property
    def warnings(self) -> list[Finding]:
        return [finding for finding in self.findings if finding.severity == "warning"]

    @property
    def error_count(self) -> int:
        return len(self.errors)

    @property
    def warning_count(self) -> int:
        return len(self.warnings)

    def as_dict(self) -> dict[str, Any]:
        return {
            "filesSeen": self.files_seen,
            "lessonCount": self.lesson_count,
            "errorCount": self.error_count,
            "warningCount": self.warning_count,
            "findings": [asdict(finding) for finding in self.findings],
        }


def _soft_severity(strict: bool) -> str:
    return "error" if strict else "warning"


def _expected(snapshot: Mapping[str, Any] | None, key: str, default: Any) -> Any:
    if snapshot is None:
        return default
    return snapshot.get(key, default)


def _validate_activity(
    report: ValidationReport,
    lesson_path: Path,
    lesson_id: str,
    activity: Any,
    index: int,
    expected_asset_dir: Path | None,
) -> None:
    prefix = f"{lesson_path.name}: activity {index + 1}"
    if not isinstance(activity, dict):
        report.add("error", "activity_shape", lesson_path, f"{prefix} must be an object")
        return

    activity_id = activity.get("activityId")
    if not isinstance(activity_id, str) or not activity_id:
        report.add("error", "activity_shape", lesson_path, f"{prefix} missing activityId")
    elif not activity_id.startswith(f"{lesson_id}-a"):
        report.add("error", "activity_shape", lesson_path, f"{prefix} activityId does not match lessonId")

    activity_type = activity.get("type")
    if not isinstance(activity_type, str) or not activity_type:
        report.add("error", "activity_shape", lesson_path, f"{prefix} missing type")
        return

    content = activity.get("content")
    if activity_type == "ANIMATED_EXPLANATION":
        if not isinstance(content, str) or not content.strip():
            report.add("error", "activity_payload", lesson_path, f"{prefix} explanation content is empty")
    elif activity_type == "HOTSPOT_IMAGE":
        if not isinstance(content, dict) or not isinstance(content.get("examples"), list) or not content["examples"]:
            report.add("error", "activity_payload", lesson_path, f"{prefix} needs a non-empty examples list")
    elif activity_type == "SORT_AND_CLASSIFY":
        if not isinstance(content, dict) or not (content.get("fits") or content.get("doesNotFit")):
            report.add("error", "activity_payload", lesson_path, f"{prefix} needs fits or doesNotFit items")
    elif activity_type == "MULTIPLE_CHOICE":
        options = content.get("options") if isinstance(content, dict) else None
        correct_index = content.get("correctIndex") if isinstance(content, dict) else None
        if not isinstance(options, list) or not options:
            report.add("error", "activity_payload", lesson_path, f"{prefix} needs non-empty options")
        elif not isinstance(correct_index, int) or isinstance(correct_index, bool) or not 0 <= correct_index < len(options):
            report.add("error", "activity_payload", lesson_path, f"{prefix} correctIndex is out of bounds")
    elif activity_type == "MATCHING_PAIRS":
        if not isinstance(content, dict) or not isinstance(content.get("pairs"), list) or not content["pairs"]:
            report.add("error", "activity_payload", lesson_path, f"{prefix} needs non-empty pairs")
    elif activity_type == "SEQUENCE_BUILDER":
        if not isinstance(content, dict) or not isinstance(content.get("steps"), list) or not content["steps"]:
            report.add("error", "activity_payload", lesson_path, f"{prefix} needs non-empty steps")

    if expected_asset_dir is not None:
        asset_id = activity.get("assetId")
        # Assessment activities in the legacy month-01 lessons intentionally
        # have assetId=null; validate references that are actually declared.
        if asset_id is None:
            return
        if not isinstance(asset_id, str) or not asset_id:
            report.add("error", "asset", lesson_path, f"{prefix} has an invalid assetId")
        elif not (expected_asset_dir / f"{asset_id}.svg").is_file():
            report.add("error", "asset", lesson_path, f"{prefix} references missing vector {asset_id}.svg")


def _validate_assessment(
    report: ValidationReport,
    lesson_path: Path,
    assessment: Any,
    expected_item_count: int | None,
    expected_passing_count: int | None,
    strict: bool,
) -> None:
    if not isinstance(assessment, dict):
        report.add("error", "assessment", lesson_path, "assessment must be an object")
        return
    items = assessment.get("items")
    if not isinstance(items, list):
        report.add("error", "assessment", lesson_path, "assessment.items must be a list")
        return
    if expected_item_count is not None and len(items) != expected_item_count:
        report.add("error", "assessment_shape", lesson_path, f"expected {expected_item_count} assessment items, found {len(items)}")
    if assessment.get("itemCount") != len(items):
        report.add("error", "assessment_shape", lesson_path, "assessment.itemCount does not match items length")
    passing = assessment.get("passingCorrectCount")
    if not isinstance(passing, int) or isinstance(passing, bool) or not 0 <= passing <= len(items):
        report.add("error", "assessment_shape", lesson_path, "passingCorrectCount is outside the item range")
    elif expected_passing_count is not None and passing != expected_passing_count:
        report.add(
            "error",
            "assessment_shape",
            lesson_path,
            f"expected passingCorrectCount={expected_passing_count}, found {passing}",
        )

    for index, item in enumerate(items, start=1):
        prefix = f"{lesson_path.name}: assessment item {index}"
        if not isinstance(item, dict):
            report.add("error", "assessment", lesson_path, f"{prefix} must be an object")
            continue
        if item.get("sequence") != index:
            report.add("error", "assessment_shape", lesson_path, f"{prefix} sequence must be {index}")
        item_type = item.get("type")
        if item_type is None:
            report.add(_soft_severity(strict), "assessment_type", lesson_path, f"{prefix} is missing type=MULTIPLE_CHOICE")
        elif item_type != "MULTIPLE_CHOICE":
            report.add("error", "assessment_type", lesson_path, f"{prefix} has unsupported type {item_type!r}")

        options = item.get("options")
        if not isinstance(options, list) or not options:
            report.add("error", "assessment", lesson_path, f"{prefix} needs non-empty options")
            option_ids: set[str] = set()
        else:
            option_ids = set()
            for option in options:
                if not isinstance(option, dict) or not isinstance(option.get("id"), str) or not isinstance(option.get("text"), str):
                    report.add("error", "assessment", lesson_path, f"{prefix} has an option without string id/text")
                    continue
                option_id = option["id"]
                if option_id in option_ids:
                    report.add("error", "assessment", lesson_path, f"{prefix} repeats option id {option_id!r}")
                option_ids.add(option_id)

        correct_ids = item.get("correctOptionIds")
        if not isinstance(correct_ids, list) or len(correct_ids) != 1:
            report.add("error", "assessment", lesson_path, f"{prefix} must have exactly one correctOptionId")
        else:
            correct_id = correct_ids[0]
            if correct_id not in option_ids:
                report.add("error", "assessment", lesson_path, f"{prefix} correct option {correct_id!r} is not present")


def validate_pack(
    pack_dir: Path,
    *,
    snapshot: Mapping[str, Any] | None = None,
    strict: bool = False,
    require_released: bool = False,
    asset_dir: Path | None = None,
) -> ValidationReport:
    """Return a complete validation report for a lesson directory."""
    pack = Path(pack_dir)
    report = ValidationReport()
    if not pack.is_dir():
        report.add("error", "pack", pack, "lesson directory does not exist")
        return report

    files = sorted(pack.glob("*.json"))
    report.files_seen = len(files)
    report.lesson_count = len(files)
    expected_count = _expected(snapshot, "lesson_count", None)
    if expected_count is not None and len(files) != expected_count:
        report.add("error", "snapshot", pack, f"expected {expected_count} lesson files, found {len(files)}")

    expected_activities = _expected(snapshot, "activities_per_lesson", len(CANONICAL_ACTIVITY_TYPES))
    expected_types = tuple(_expected(snapshot, "activity_types", list(CANONICAL_ACTIVITY_TYPES)))
    expected_assessments = _expected(snapshot, "assessment_items_per_lesson", 5)
    expected_passing_count = _expected(snapshot, "assessment_passing_correct_count", None)

    for path in files:
        try:
            lesson = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            report.add("error", "json", path, f"cannot parse lesson: {exc}")
            continue
        if not isinstance(lesson, dict):
            report.add("error", "schema", path, "lesson root must be an object")
            continue

        lesson_id = lesson.get("lessonId")
        if not isinstance(lesson_id, str) or not lesson_id:
            report.add("error", "schema", path, "lessonId is required")
            lesson_id = path.stem
        elif lesson_id != path.stem:
            report.add("error", "schema", path, "lessonId does not match filename")

        for required in ("subject", "title", "objective"):
            value = lesson.get(required)
            if not isinstance(value, str) or not value.strip():
                report.add("error", "schema", path, f"{required} must be a non-empty string")
        for required in ("activities", "assessment"):
            if required not in lesson:
                report.add("error", "schema", path, f"missing required field {required}")

        activities = lesson.get("activities")
        if not isinstance(activities, list):
            report.add("error", "activity_shape", path, "activities must be a list")
        else:
            if len(activities) != expected_activities:
                report.add("error", "activity_shape", path, f"expected {expected_activities} activities, found {len(activities)}")
            actual_types = [activity.get("type") if isinstance(activity, dict) else None for activity in activities]
            if tuple(actual_types) != expected_types:
                report.add("error", "activity_shape", path, f"activity type order differs from {list(expected_types)}")
            for index, activity in enumerate(activities):
                if isinstance(activity, dict) and activity.get("sequence") != index + 1:
                    report.add("error", "activity_shape", path, f"activity {index + 1} sequence must be {index + 1}")
                _validate_activity(report, path, lesson_id, activity, index, asset_dir)

        _validate_assessment(report, path, lesson.get("assessment"), expected_assessments, expected_passing_count, strict)

        if require_released and not (lesson.get("educatorValidated") is True and lesson.get("releaseStatus") == "RELEASED"):
            report.add("error", "educator", path, "lesson is not educatorValidated=true and RELEASED")

    return report


def _load_snapshot(path: Path) -> Mapping[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError("snapshot root must be an object")
    return value


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    default_pack = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"
    default_snapshot = Path(__file__).resolve().with_name("content_pack_baseline.json")
    parser.add_argument("--pack", type=Path, default=default_pack)
    parser.add_argument("--snapshot", type=Path, default=default_snapshot)
    parser.add_argument("--asset-dir", type=Path, default=None)
    parser.add_argument("--no-asset-check", action="store_true")
    parser.add_argument("--strict", action="store_true", help="Promote soft audit findings to errors")
    parser.add_argument("--require-released", action="store_true")
    parser.add_argument("--report", type=Path, default=None, help="Write an atomic JSON report")
    args = parser.parse_args(argv)

    try:
        snapshot = _load_snapshot(args.snapshot) if args.snapshot else None
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"error: invalid snapshot {args.snapshot}: {exc}", file=sys.stderr)
        return 2

    asset_dir = None
    if not args.no_asset_check:
        asset_dir = args.asset_dir
        if asset_dir is None:
            candidate = args.pack.parent / "assets" / "vectors"
            if candidate.is_dir():
                asset_dir = candidate

    report = validate_pack(
        args.pack,
        snapshot=snapshot,
        strict=args.strict,
        require_released=args.require_released,
        asset_dir=asset_dir,
    )
    print(
        f"Content pack: {report.lesson_count} lessons, {report.files_seen} files, "
        f"{report.error_count} errors, {report.warning_count} warnings"
    )
    for finding in report.findings[:20]:
        print(f"[{finding.severity.upper()}] {finding.category} | {finding.path} | {finding.message}")
    if len(report.findings) > 20:
        print(f"... {len(report.findings) - 20} additional findings omitted")

    if args.report:
        atomic_write_text(args.report, json.dumps(report.as_dict(), indent=2, ensure_ascii=False) + "\n")
        print(f"Report written to {args.report}")
    return 1 if report.errors else 0


if __name__ == "__main__":
    sys.exit(main())

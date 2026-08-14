#!/usr/bin/env python3
"""Objective pacing audit for Maxine's World content packs.

Maps each lesson's `objective` against the corpus and reports the fan-out
(objective -> number of files that share the exact same objective string).

CH-07 phase model (issue #76, educator finding M1):
  Target: <= 3 files per objective for G3 (spiral review), with each file
  naming ONE focus objective. Fan-out above 3 thins per-lesson focus.

Usage:
  python3 tools/objective_pacing_audit.py [--content-root DIR] [--check] [--json PATH]

  --check : return non-zero when any objective fans out to more than
            MAX_FANOUT files (CI gate mode).
"""
import argparse
import collections
import json
import os
import sys

MAX_FANOUT = 3

DEFAULT_ROOT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "assets", "content-pack", "month-01", "lessons",
)


def load_lessons(root: str) -> list[tuple[str, dict]]:
    out = []
    for name in sorted(os.listdir(root)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(root, name)
        try:
            with open(path, encoding="utf-8") as fh:
                data = json.load(fh)
        except Exception as exc:  # noqa: BLE001 - report and continue
            print(f"ERROR: cannot parse {name}: {exc}", file=sys.stderr)
            continue
        out.append((path, data))
    return out


def audit(root: str) -> dict:
    lessons = load_lessons(root)
    fanout: dict[str, list[str]] = collections.defaultdict(list)
    no_objective = 0
    for path, data in lessons:
        objective = (data.get("objective") or "").strip()
        if not objective:
            no_objective += 1
            continue
        fanout[objective].append(path)
    over = {obj: paths for obj, paths in fanout.items() if len(paths) > MAX_FANOUT}
    return {
        "lesson_count": len(lessons),
        "distinct_objectives": len(fanout),
        "objectives_over_fanout": len(over),
        "files_in_over_fanout": sum(len(p) for p in over.values()),
        "lessons_without_objective": no_objective,
        "over_fanout": {
            obj: {
                "count": len(paths),
                "files": [os.path.basename(p) for p in sorted(paths)],
            }
            for obj, paths in sorted(
                over.items(), key=lambda kv: (-len(kv[1]), kv[0])
            )
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--content-root", default=DEFAULT_ROOT)
    parser.add_argument(
        "--check", action="store_true",
        help="exit non-zero when any objective exceeds %d files" % MAX_FANOUT,
    )
    parser.add_argument("--json", help="write full report JSON to this path")
    args = parser.parse_args()

    report = audit(args.content_root)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(report, fh, indent=1, ensure_ascii=False)

    print(
        "Objective pacing audit:\n"
        f"  lessons:                  {report['lesson_count']}\n"
        f"  distinct objectives:      {report['distinct_objectives']}\n"
        f"  objectives over fan-out:  {report['objectives_over_fanout']} "
        f"(> {MAX_FANOUT} files)\n"
        f"  files in over fan-out:    {report['files_in_over_fanout']}\n"
        f"  lessons missing objective:{report['lessons_without_objective']}"
    )
    for objective, detail in report["over_fanout"].items():
        print(f"\n  [{detail['count']}] {objective}")
        for fname in detail["files"]:
            print(f"      {fname}")

    return 1 if args.check and report["objectives_over_fanout"] else 0


if __name__ == "__main__":
    sys.exit(main())

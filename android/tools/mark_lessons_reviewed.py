#!/usr/bin/env python3
"""
Approve playable lessons after educator review.

Before a release, the educator (parent/curriculum reviewer) reviews the
content. This script batch-marks the reviewed lessons as
educatorValidated=true / releaseStatus=RELEASED so the release gate
(verifyPlayableContent Gradle task in the tag-release CI workflow) passes.

Usage:
  # Review the pack first (e.g. via the QA audit + manual sampling), then:
  python3 tools/mark_lessons_reviewed.py            # approve ALL playable lessons
  python3 tools/mark_lessons_reviewed.py --subject gmrc   # approve one subject
  python3 tools/mark_lessons_reviewed.py --dry-run        # show what would change

Idempotent: already-approved lessons are untouched. Writes the approval
timestamp into each lesson JSON (reviewedAt) for provenance.

NOTE: this is the human accountability step — do not run it without an
actual review of the content. The point of the gate is that a release
must not ship un-reviewed curriculum to a child.
"""

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]  # android/
PACK_DIR = REPO_ROOT / "app/src/main/assets/content-pack/month-01/lessons"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--subject", help="Approve only one subject (e.g. gmrc)")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not PACK_DIR.is_dir():
        print(f"ERROR: pack dir not found: {PACK_DIR}", file=sys.stderr)
        return 1

    stamp = datetime.now(timezone.utc).isoformat(timespec="seconds")
    changed = 0
    already = 0
    skipped = []

    for json_file in sorted(PACK_DIR.glob("*.json")):
        lesson = json.loads(json_file.read_text())
        if args.subject and lesson.get("subject", "").lower() != args.subject.lower():
            skipped.append(json_file.name)
            continue
        if lesson.get("educatorValidated") and lesson.get("releaseStatus") == "RELEASED":
            already += 1
            continue
        lesson["educatorValidated"] = True
        lesson["releaseStatus"] = "RELEASED"
        lesson["reviewedAt"] = stamp
        lesson["qualifiesForDailyBadge"] = True
        if not args.dry_run:
            json_file.write_text(json.dumps(lesson, indent=1, ensure_ascii=False) + "\n")
        changed += 1

    scope = f"subject={args.subject}" if args.subject else "all subjects"
    print(f"[{'dry-run' if args.dry_run else 'approved'}] {scope}")
    print(f"  marked reviewed: {changed}")
    print(f"  already reviewed: {already}")
    print(f"  untouched (filtered out): {len(skipped)}")
    if changed and not args.dry_run:
        print(f"  reviewedAt: {stamp}")
        print("  NOTE: commit these changes, then push a release tag; the")
        print("        tag-release CI job will verify no unreviewed lesson ships.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

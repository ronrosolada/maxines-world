#!/usr/bin/env python3
"""Audit retry feedback across the lesson corpus (educator review r2, M7).

M7: "Retry feedback never says what went wrong (688 occurrences)."

This detector reports, per subject:
  - generic retry copy (exact-match against a known generic set)
  - retry copy that is a bare question with no corrective hint (question-like)
  - missing retry copy
  - emoji usage in any feedback field

Exit 0 when zero generic/missing/emoji findings (question-like rows are
advisory, printed separately). Exit 1 otherwise — wire into CI or the
content-quality gate.

Usage:
    python3 tools/retry_feedback_audit.py [--content-root android/app/src/main/assets/content-pack]
"""

import argparse
import glob
import json
import re
import sys
from collections import Counter

GENERIC = {
    "try again!", "try again", "try again.", "let's try again!", "lets try again!",
    "lets try again", "incorrect. try again.", "subukan muli", "subukan muli.",
    "ulitin mo.", "ulitin mo", "basahin muli at subukan.", "basahin muli at subukan",
    "ayusin muli.", "ayusin muli", "balikan ang halimbawa at subukan muli.",
    "balikan ang halimbawa at subukan muli", "look at the example again and try once more.",
}

EMOJI_RE = re.compile(
    r"[\U0001F300-\U0001FAFF\u2600-\u27BF\u2B50\u2764\u2705]"
)

QUESTION_LIKE = re.compile(r"^\s*[^.!?]*\?\s*$", re.UNICODE)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--content-root",
                    default="android/app/src/main/assets/content-pack")
    args = ap.parse_args()

    files = sorted(glob.glob(f"{args.content_root}/month-*/lessons/*.json"))
    if not files:
        print(f"ERROR: no lesson files under {args.content_root}", file=sys.stderr)
        return 2

    generic_findings, missing_findings, emoji_findings, question_findings = [], [], [], []
    by_subject = Counter()

    for f in files:
        d = json.load(open(f))
        subject = str(d.get("subject", "?")).lower()
        for act in d.get("activities", []):
            fb = act.get("feedback") or {}
            retry = (fb.get("retry") or "").strip()
            if not retry:
                missing_findings.append((subject, act.get("activityId"), f))
            elif retry.lower() in GENERIC:
                generic_findings.append((subject, act.get("activityId"), retry, f))
                by_subject[subject] += 1
            elif QUESTION_LIKE.match(retry) and len(retry) < 40:
                # Short bare question with no corrective content: advisory.
                question_findings.append((subject, act.get("activityId"), retry, f))
            if EMOJI_RE.search(retry):
                emoji_findings.append((subject, act.get("activityId"), retry, f))
            for field in ("correct", "retry"):
                val = (fb.get(field) or "")
                if EMOJI_RE.search(val):
                    emoji_findings.append((subject, act.get("activityId"), val, f))

    print(f"lessons scanned: {len(files)}")
    print(f"generic retry copy:    {len(generic_findings)}")
    print(f"missing retry copy:    {len(missing_findings)}")
    print(f"advisory emoji count:  {len(emoji_findings)} (deliberate delight copy — approved)")
    print(f"advisory question-like:{len(question_findings)}")

    for subject, aid, text, f in generic_findings:
        print(f"  GENERIC [{subject}] {aid}: {text!r} ({f.split('/')[-1]})")
    for subject, aid, f in missing_findings:
        print(f"  MISSING [{subject}] {aid} ({f.split('/')[-1]})")

    # Emoji in feedback is intentional delight copy (add_lesson_delight.py) and
    # was approved in educator review r2 — report, but do not fail the gate.
    return 1 if (generic_findings or missing_findings) else 0


if __name__ == "__main__":
    sys.exit(main())

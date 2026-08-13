#!/usr/bin/env python3
"""Detect same-keyed answer pairs in assessments (educator review r2, Minor).

122 answer pairs where different questions share coincidentally identical
correct text (e.g. simuno/panaguri items). Reduces assessment discrimination.

Classification:
  - same lesson file  -> items must differ in skill, not just wording (review)
  - literal duplicates (same lesson + identical prompt + same correct text)
    are flagged as action-required
  - cross-file repeats are advisory (spiral review reuses vocabulary)

Usage:
    python3 tools/same_keyed_answers.py [--content-root android/app/src/main/assets/content-pack]
    python3 tools/same_keyed_answers.py --action-required   # only literal duplicates

Exit 1 when --action-required is passed and literal duplicates exist.
"""

import argparse
import collections
import glob
import json
import sys
import unicodedata


def norm(s: str) -> str:
    return unicodedata.normalize("NFKC", s.lower().strip())


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--content-root",
                    default="android/app/src/main/assets/content-pack")
    ap.add_argument("--action-required", action="store_true")
    args = ap.parse_args()

    files = sorted(glob.glob(f"{args.content_root}/month-*/lessons/*.json"))
    if not files:
        print(f"ERROR: no lesson files under {args.content_root}", file=sys.stderr)
        return 2

    seen = collections.defaultdict(list)
    for f in files:
        d = json.load(open(f))
        subject = str(d.get("subject", "")).lower()
        fname = f.split("/")[-1]
        for item in d.get("assessment", {}).get("items", []):
            if item.get("type") != "MULTIPLE_CHOICE":
                continue
            opt_map = {o["id"]: o["text"] for o in item.get("options", [])}
            correct = [opt_map[c] for c in item.get("correctOptionIds", []) if c in opt_map]
            if len(correct) != 1:
                continue
            seen[(subject, norm(correct[0]))].append(
                (fname, item["itemId"], norm(item.get("prompt", ""))))

    groups = {k: v for k, v in seen.items() if len(v) > 1}
    literal = []
    same_file = []
    cross_file = []
    for (subject, txt), items in groups.items():
        files_in = {it[0] for it in items}
        if len(files_in) == 1:
            same_file.append((subject, txt, items))
            for it in items:
                # identical prompt text + identical correct text in one lesson
                dup = [o for o in items if o[2] == it[2] and o[1] != it[1]]
                if dup:
                    literal.append((subject, txt, it, dup[0]))
        else:
            cross_file.append((subject, txt, items))

    print(f"groups:           {len(groups)} ({sum(len(v) for v in groups.values())} items)")
    print(f"same-file groups: {len(same_file)}")
    print(f"cross-file:       {len(cross_file)} (advisory — spiral review)")
    print(f"literal dup pairs:{len(literal)} (action required)")

    for subject, txt, a, b in literal:
        print(f"  DUP [{subject}] correct={txt!r}:")
        print(f"      {a[0]} :: {a[1]} :: {a[2][:70]}")
        print(f"      {b[0]} :: {b[1]} :: {b[2][:70]}")

    return 1 if (args.action_required and literal) else 0


if __name__ == "__main__":
    sys.exit(main())

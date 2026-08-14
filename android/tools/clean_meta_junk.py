#!/usr/bin/env python3
"""Strip generator meta-junk and fix grammar in child-facing lesson copy.

Removes template artifacts that leaked into learner-visible strings:

1. Meta prefixes on assessment prompts (e.g. "Sagutin ito: ", "Answer this
   one: ", "Take a good look: ") — artifacts of a diversification pass that
   prefixed generic prompt templates with filler.
2. Meta suffixes (e.g. "  (sa aralin: Kapaligiran at Kultura)",
   "  (in this lesson: Material Detectives)") — parenthetical reminders
   intended for authors, not learners.
3. Verified grammar errors in HOTSPOT_IMAGE example instructions:
   - "rubber ball is bounces" -> "a rubber ball bounces"
   - "rubber duck is floats in water" -> "a rubber duck floats in water"
   - "rubber slippers is flexible, and cotton towel is absorbs water"
     -> "rubber slippers are flexible, and a cotton towel absorbs water"
   - "does a sandpaper have" -> "does sandpaper have" (uncountable)
   - "does a chalk have" -> "does a piece of chalk have" (uncountable)

Idempotent: safe to re-run. Report mode is default; --apply writes changes.
"""
import argparse
import json
import os
import re
import sys

DEFAULT_ROOT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "assets", "content-pack", "month-01", "lessons",
)

FIL_PREFIXES = (
    "Sagutin ito: ",
    "Pag-isipang mabuti: ",
    "Isa pang tanong: ",
    "Subukan mo ito: ",
    "Tingnan mo ito: ",
    "Suriin ito: ",
    "Isipin ito: ",
    "Heto ang tanong: ",
    "Balikan natin: ",
    "Pag-isipan natin: ",
    "Sagot mo ito: ",
    "Gawin natin: ",
    "Sagutin nang mabuti: ",
    "Ano sa palagay mo? ",
)

EN_PREFIXES = (
    "Answer this one: ",
    "Take a good look: ",
    "One more question: ",
    "Here is a question: ",
    "Look at this one: ",
    "Check this one: ",
    "Now answer this: ",
    "Here is another: ",
    "What do you think? ",
)

META_SUFFIX_RE = re.compile(
    r"\s*\((sa aralin|in this lesson): [^)]*\)\s*$", re.IGNORECASE
)

# (needle, replacement) exact-string grammar fixes.
GRAMMAR_FIXES = (
    ("rubber ball is bounces", "a rubber ball bounces"),
    ("rubber duck is floats in water", "a rubber duck floats in water"),
    ("rubber slippers is flexible, and cotton towel is absorbs water",
     "rubber slippers are flexible, and a cotton towel absorbs water"),
    ("What property does a sandpaper have?", "What property does sandpaper have?"),
    ("What property does a chalk have?", "What property does a piece of chalk have?"),
)


def clean_prompt(prompt: str) -> tuple[str, list[str]]:
    """Return (cleaned, changes) for one assessment prompt."""
    changes: list[str] = []
    text = prompt.strip()
    for prefix in FIL_PREFIXES + EN_PREFIXES:
        if text.startswith(prefix):
            rest = text[len(prefix):].strip()
            if rest:
                text, changes = rest, [f"prefix {prefix.strip()!r}"]
            break
    while True:
        new = META_SUFFIX_RE.sub("", text).strip()
        if new == text:
            break
        changes.append("meta suffix")
        text = new
    return text, changes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--content-root", default=DEFAULT_ROOT)
    parser.add_argument("--apply", action="store_true", help="write cleaned files")
    args = parser.parse_args()

    prefix_hits = 0
    suffix_hits = 0
    grammar_hits = 0
    files_touched: set[str] = set()

    for name in sorted(os.listdir(args.content_root)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(args.content_root, name)
        with open(path, encoding="utf-8") as fh:
            lesson = json.load(fh)
        changed = False

        for item in lesson.get("assessment", {}).get("items", []):
            prompt = item.get("prompt")
            if not isinstance(prompt, str) or not prompt:
                continue
            cleaned, changes = clean_prompt(prompt)
            if cleaned != prompt:
                for change in changes:
                    if change.startswith("prefix"):
                        prefix_hits += 1
                    else:
                        suffix_hits += 1
                item["prompt"] = cleaned
                changed = True
            for needle, replacement in GRAMMAR_FIXES:
                if needle in prompt:
                    item["prompt"] = prompt.replace(needle, replacement)
                    prompt = item["prompt"]
                    grammar_hits += 1
                    changed = True
                    print(f"  grammar(prompt): {name}: {needle!r} -> {replacement!r}")

        for activity in lesson.get("activities", []):
            instruction = activity.get("instruction")
            if not isinstance(instruction, str) or not instruction:
                continue
            for needle, replacement in GRAMMAR_FIXES:
                if needle in instruction:
                    activity["instruction"] = instruction.replace(needle, replacement)
                    instruction = activity["instruction"]
                    grammar_hits += 1
                    changed = True
                    print(f"  grammar: {name}: {needle!r} -> {replacement!r}")

        if changed:
            files_touched.add(name)
            if args.apply:
                with open(path, "w", encoding="utf-8") as fh:
                    json.dump(lesson, fh, indent=2, ensure_ascii=False)
                    fh.write("\n")

    print(
        "Meta-junk cleanup scan:\n"
        f"  prefix hits: {prefix_hits}\n"
        f"  suffix hits: {suffix_hits}\n"
        f"  grammar fixes: {grammar_hits}\n"
        f"  files touched: {len(files_touched)}"
    )
    if not args.apply:
        print("(report only — re-run with --apply to write)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Wave B1a: re-author the simuno/panaguri Filipino group (M1 pacing).

Grounds each lesson's objective, title, introduction, and explanation in the
lesson's own anchor sentence (the sentence its assessment actually tests).
This converts 32 identical template lessons into 32 distinct practice lessons
while preserving IDs, keys, and schema (objective/title/intro/explanation text
only). Idempotent via marker field.
"""
import json
import os
import re
import sys

ROOT = "/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons"
TARGET_OBJECTIVE = "Natutukoy ang simuno at panaguri sa payak na pangungusap."
MARKER = "pacing-b1a-2026-08-15"

ANCHOR_RE = re.compile(r"^Alin ang simuno sa '([^']+)'")

# hand-authored per-lesson meta: (title, intro)
# title from anchor; intro names the anchor character+action.
LESSON_META = {}


def anchor_to_title(sent: str) -> str:
    """'Si Ana / ay nagbabasa.' -> 'Si Ana ay Nagbabasa'."""
    body = sent.replace("/", "").strip().rstrip(".")
    body = re.sub(r"\s+", " ", body)
    # "Ang bata ay tumatakbo pababa" -> "Tumatakbo ang Bata"
    if body.startswith("Ang "):
        parts = body.split(" ay ")
        if len(parts) == 2:
            subject, predicate = parts
            words = subject.split()
            if len(words) >= 2:
                return f"{predicate[0].upper()}{predicate[1:]} ang {' '.join(w.capitalize() for w in words[1:])}"
    if body.startswith("Si "):
        parts = body.split(" ay ")
        if len(parts) == 2:
            subject, predicate = parts
            name = subject.replace("Si ", "")
            return f"{name} ay {predicate[0].upper()}{predicate[1:]}"
    words = body.split()
    return " ".join(w.capitalize() if i == 0 or w.lower() not in {
        "ang", "ng", "sa", "si", "mga", "ay", "at", "ni"
    } else w for i, w in enumerate(words))


def anchor_parts(sent: str) -> tuple[str, str]:
    """Return (simuno, panaguri) from 'Si Ana / ay nagbabasa.'"""
    parts = [p.strip() for p in sent.replace("/", "").split(" ay ")]
    if len(parts) == 2:
        return parts[0], parts[1].rstrip(".")
    return sent, ""


def build_intro(sent: str) -> str:
    simuno, panaguri = anchor_parts(sent)
    if simuno.startswith("Si "):
        who = simuno[3:]
        return (
            f"Kumusta, Maxine! 🐱✨ May kuwento si Milo tungkol kay {who} na "
            f"{panaguri}. Hatiin natin ang pangungusap: sino ang pinag-uusapan, "
            f"at ano ang ginagawa? Tara na!"
        )
    if simuno.startswith("Ang "):
        what = simuno[4:]
        return (
            f"Kumusta, Maxine! 🐱✨ May bagong tuklasin si Milo: {what} na "
            f"{panaguri}. Hatiin natin ang pangungusap: sino o ano ang "
            f"pinag-uusapan? Tara na!"
        )
    return f"Kumusta, Maxine! 🐱✨ Pag-aralan natin ang pangungusap: {sent.rstrip('.')}."


def build_objective(sent: str) -> str:
    return (
        "Natutukoy ang simuno at panaguri sa pangungusap na "
        f"'{sent.replace('/', '').strip().rstrip('.')}.'"
    )


def build_explanation(sent: str) -> str:
    simuno, panaguri = anchor_parts(sent)
    return (
        f"Ang simuno ang pinag-uusapan: {simuno}. Ang panaguri ang nagsasabi "
        f"tungkol sa simuno: {panaguri}. Hatiin sa dalawa: [{simuno}] ay ang "
        f"simuno, [ay {panaguri}] ay ang panaguri."
    )


def main() -> int:
    changed = 0
    skipped = 0
    for name in sorted(os.listdir(ROOT)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(ROOT, name)
        with open(path, encoding="utf-8") as fh:
            lesson = json.load(fh)
        if (lesson.get("objective") or "").strip() != TARGET_OBJECTIVE:
            continue
        items = lesson.get("assessment", {}).get("items", [])
        if not items:
            skipped += 1
            continue
        match = ANCHOR_RE.match(items[0].get("prompt", ""))
        if not match:
            print(f"  WARN {name}: no anchor sentence in q1", file=sys.stderr)
            skipped += 1
            continue
        anchor = match.group(1)

        lesson["objective"] = build_objective(anchor)
        lesson["title"] = anchor_to_title(anchor)
        lesson["introduction"] = build_intro(anchor)

        # embed the anchor in the explanation activity
        for activity in lesson.get("activities", []):
            if activity.get("type") == "ANIMATED_EXPLANATION":
                activity["content"] = build_explanation(anchor)
                activity["narration"] = build_explanation(anchor)
                activity["instruction"] = (
                    f"Pakinggan si Milo: hatiin ang pangungusap na "
                    f"'{anchor.replace('/', '').strip()}'."
                )
                activity["accessibilityAlternative"] = build_explanation(anchor)
                break

        lesson["pacingPass"] = MARKER
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(lesson, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        changed += 1

    print(f"B1a applied to {changed} lessons, {skipped} skipped")
    return 0


if __name__ == "__main__":
    sys.exit(main())

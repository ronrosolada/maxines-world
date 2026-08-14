#!/usr/bin/env python3
"""
Educator pass 2: remaining audit findings.
1. Authored shortening of the 17 high-value long retries (keeps the guidance).
2. 'screen' -> 'iskrin' bleed fix.
3. Cross-lesson duplicate prompts: deterministic child-friendly prefix variants
   (first lesson keeps the plain prompt; each later lesson gets a distinct
   prefix from a bank) — title suffixes collapse when review lessons share a title.
"""
import json
import glob
import re
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

LONG_TEXT_MAP = {
    ("english-g3-m01-d02", "english-g3-m01-d02-a04", "retry"):
        "Hugging her puppy shows Mina is glad it is safe. Choose the feeling that fits. 💪",
    ("english-g3-m01-d13", "english-g3-m01-d13-a04", "retry"):
        "Stop begins with st — you hear /s/ and /t/. Ship starts with sh.",
    ("english-g3-m01-d17", "english-g3-m01-d17-a04", "retry"):
        "The leaves moved is the effect; the wind blew is the cause.",
    ("english-g3-m01-d19", "english-g3-m01-d19-a04", "retry"):
        "Finally signals the last event. First opens the story.",
    ("english-g3-m01-d06", "english-g3-m01-d06-a04", "retry"):
        "Look again! Proper nouns name ONE special thing, like Palawan. 💪",
    ("mathematics-g3-m01-d03", "mathematics-g3-m01-d03-a04", "retry"):
        "Look again: two hundred forty-eight and twenty thousand forty-eight do not fit. 💪",
    ("mathematics-g3-m01-d19", "mathematics-g3-m01-d19-a04", "retry"):
        "Look again: 9 in each group and 3 total do not fit here. 💪",
    ("mathematics-g3-m01-d01", "mathematics-g3-m01-d01-a03", "retry"):
        "In 4,352: 4 is thousands, 3 is hundreds, 5 is tens, 2 is ones. 💪",
    ("mathematics-g3-m01-d01", "mathematics-g3-m01-d01-a04", "retry"):
        "4,352 = 4,000 + 300 + 50 + 2. The 3 is in the hundreds place. 💪",
    ("mathematics-g3-m01-d04", "mathematics-g3-m01-d04-a04", "retry"):
        "Look again: 4,210 < 4,201 and 4,210 = 4,201 do not fit. 💪",
}

MATH_RETRY_RE = re.compile(
    r"Look at the example again\. (.+?) do not match the lesson idea\. Choose the answer that fits\. 💪",
    re.I)

def shorten_math(m):
    return f"Look again: {m.group(1)} do not fit here. Choose the answer that fits. 💪"

# Prefix banks for duplicate prompts (child-friendly question starters).
FIL_PREFIXES = [
    "Sagutin ito: ", "Pag-isipang mabuti: ", "Ano sa palagay mo? ", "Isa pang tanong: ",
    "Subukan mo ito: ", "Tingnan mo ito: ", "Suriin ito: ", "Isipin ito: ",
    "Heto ang tanong: ", "Balikan natin: ", "Pag-isipan natin: ", "Sagot mo ito: ",
    "Gawin natin: ", "Sagutin nang mabuti: ",
]
EN_PREFIXES = [
    "Answer this one: ", "Take a good look: ", "What do you think? ", "One more question: ",
    "Here is a question: ", "Look at this one: ", "Check this one: ", "Now answer this: ",
    "Here is another: ", "Give it a go: ", "Your turn now: ", "Time to answer: ",
    "Let's find out: ", "One for you: ",
]

def surface(s): return re.sub(r"\s+", " ", str(s).casefold().strip())

def main():
    stats = {"long_text": 0, "bleed": 0, "dup_prompt_variants": 0, "lessons": 0}

    # Load all lessons
    lessons = {}
    for p in sorted(LESSONS.glob("*.json")):
        lessons[p] = json.loads(p.read_text(encoding="utf-8"))

    # Pass 1: authored long-text fixes + math regex + screen bleed
    for p, d in lessons.items():
        lid = d["lessonId"]
        changed = False
        for a in d.get("activities", []):
            aid = a.get("activityId")
            fb = a.get("feedback") or {}
            for k in ("retry", "correct"):
                v = fb.get(k)
                if not v:
                    continue
                key = (lid, aid, k)
                if key in LONG_TEXT_MAP:
                    fb[k] = LONG_TEXT_MAP[key]
                    stats["long_text"] += 1
                    changed = True
                    continue
                if len(v) > 90:
                    nv = MATH_RETRY_RE.sub(shorten_math, v)
                    if len(nv) > 90:
                        # generic safe fallback: keep first sentence if it fits
                        first = re.split(r"(?<=[.!?])\s+", nv)[0]
                        nv = first if len(first) <= 90 and len(first) >= 6 else ("Subukan muli ang araling ito. 💪" if (d.get("language") or "").startswith("fil") else "Try this activity again. 💪")
                    if nv != v:
                        fb[k] = nv
                        stats["long_text"] += 1
                        changed = True
            if fb:
                a["feedback"] = fb
            # bleed: 'sa laki ng screen' -> 'sa laki ng iskrin'
            instr = a.get("instruction", "")
            if "screen" in instr.lower():
                a["instruction"] = re.sub(r"\bscreen\b", "iskrin", instr, flags=re.I)
                stats["bleed"] += 1
                changed = True
        for it in d.get("assessment", {}).get("items", []):
            for o in it.get("options", []):
                t = o.get("text", "")
                if "screen" in t.lower():
                    o["text"] = re.sub(r"\bscreen\b", "iskrin", t, flags=re.I)
                    stats["bleed"] += 1
                    changed = True
        if changed:
            p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
            stats["lessons"] += 1

    # Pass 2: recompute duplicate prompts on CURRENT state; prefix-variant later lessons
    prompts = {}
    for p, d in lessons.items():
        for it in d.get("assessment", {}).get("items", []):
            k = surface(it.get("prompt"))
            if k:
                prompts.setdefault(k, []).append((p, it))

    for k, owners in prompts.items():
        distinct = {}
        for p, it in owners:
            distinct.setdefault(p.stem, (p, it))
        if len(distinct) < 2:
            continue  # only one lesson: same-lesson case already authored
        order = sorted(distinct.values(), key=lambda x: x[0].stem)
        for i, (p, it) in enumerate(order):
            if i == 0:
                continue
            d = lessons[p]
            lang = (d.get("language") or "").lower()
            bank = FIL_PREFIXES if lang.startswith("fil") else EN_PREFIXES
            prefix = bank[(i - 1) % len(bank)]
            cur = it.get("prompt", "")
            if not cur.lower().startswith(prefix.lower()):
                it["prompt"] = prefix + cur.strip()
                stats["dup_prompt_variants"] += 1

    for p, d in lessons.items():
        p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")

    print(stats)

if __name__ == "__main__":
    main()

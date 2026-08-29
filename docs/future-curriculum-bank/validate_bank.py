#!/usr/bin/env python3
"""Structural and pedagogical gates for the Grade 3–4 future bank."""
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

R = Path(__file__).parent
KEYS = "abcd"
FORBIDDEN_FILIPINO = (
    "rice granary", "honesty is the best policy", "3rs", "reduce", "reuse", "recycle",
    "reusable", "single-use", "sustainable development", "renewable energy", "open-pit mining",
    "crop rotation", "open burning", "greenhouse gas emissions", "climate change", "scam",
    "bank account", "growth mindset", "critically minded", "i-verify", "finish line",
    "compass rose", "legend", "health center", "social media", "online", "chat", "netiquette",
    "vandalism", "intangible cultural heritage", "exclusive economic zone", "reforestation",
    "illegal logging", "endangered", "coral reef", "nursery", "frontliners",
)
GENERIC_SCRIPT = ("today, milo explores", "ngayon, sasamahan tayo ni milo", "watch closely as we break down")


def load(name):
    return json.loads((R / name).read_text())


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for item in value:
            yield from strings(item)
    elif isinstance(value, dict):
        for item in value.values():
            yield from strings(item)


def word_count(text):
    return len(re.findall(r"\b[\w'-]+\b", text, re.UNICODE))


def validate():
    errors = []
    roadmap = load("roadmap.json")
    bank = load("assessment-bank.json")
    videos = load("micro-lessons.json")
    ladders = load("remediation-ladders.json")
    reports = load("parent-report-templates.json")
    units, items = roadmap["units"], bank["items"]
    unit_by_id = {u["id"]: u for u in units}
    ids = set(unit_by_id)
    skill = json.loads((R / "../../android/app/src/main/assets/content-pack/skill-graph.json").resolve().read_text())
    skills = {n["id"] for n in skill["nodes"]}

    if len(units) != 48:
        errors.append(f"expected 48 units, got {len(units)}")
    if Counter(u["grade"] for u in units) != {3: 24, 4: 24}:
        errors.append("grade balance")
    subject_counts = Counter((u["grade"], u["subjectBalanceGroup"]) for u in units)
    if len(subject_counts) != 12 or any(n != 4 for n in subject_counts.values()):
        errors.append(f"subject balance {subject_counts}")

    for unit in units:
        if unit["prerequisiteSkillId"] not in skills:
            errors.append(f"{unit['id']}: dangling prerequisite")
        expected = "English" if unit["subject"] in {"mathematics", "science", "english"} else "Filipino"
        if unit["language"] != expected:
            errors.append(f"{unit['id']}: language policy")
        if unit["grade"] == 4 and unit["subject"] == "makabansa":
            errors.append(f"{unit['id']}: Grade 4 must be araling-panlipunan")

    by_unit = defaultdict(list)
    prompts, key_counts = [], Counter()
    unique_longest = 0
    for item in items:
        by_unit[item["unitId"]].append(item)
        prompts.append(re.sub(r"\W+", " ", item["prompt"].lower()).strip())
        key_counts[item["correctOptionId"]] += 1
        if item["unitId"] not in ids:
            errors.append(f"{item['id']}: dangling unit")
        if len(item["options"]) != 4 or {o["id"] for o in item["options"]} != set(KEYS):
            errors.append(f"{item['id']}: options")
            continue
        lengths = [word_count(o["text"]) for o in item["options"]]
        keyed = KEYS.index(item["correctOptionId"])
        if lengths[keyed] == max(lengths) and lengths.count(max(lengths)) == 1:
            unique_longest += 1
        if max(lengths) >= 8 and min(lengths) / max(lengths) < 0.35:
            errors.append(f"{item['id']}: severely imbalanced option lengths {lengths}")

    if len(items) != 192 or set(by_unit) != ids or any(len(rows) != 4 for rows in by_unit.values()):
        errors.append("assessment scaffolding/count")
    if len(set(prompts)) != len(prompts):
        errors.append("duplicate normalized prompts")
    if key_counts != Counter({key: 48 for key in KEYS}):
        errors.append(f"unbalanced keys {key_counts}")
    sequences = ["".join(x["correctOptionId"] for x in by_unit[uid]) for uid in sorted(ids)]
    if len(set(sequences)) < 8 or max(Counter(sequences).values()) > 4 or "abcd" in sequences:
        errors.append(f"predictable per-unit key sequences {Counter(sequences)}")
    if unique_longest > 48:  # no more than 25% of the bank
        errors.append(f"keyed answer uniquely longest in {unique_longest}/192 items")

    collections = [("videos", videos["lessons"]), ("ladders", ladders["ladders"]),
                   ("reports", reports["unitReportData"])]
    for name, rows in collections:
        if {x["unitId"] for x in rows} != ids:
            errors.append(f"{name}: dangling/missing links")

    # Scan every learner-facing field belonging to a Filipino-medium unit.
    records = units + items + videos["lessons"] + ladders["ladders"] + reports["unitReportData"]
    for record in records:
        uid = record.get("unitId", record.get("id"))
        if uid in unit_by_id and unit_by_id[uid]["language"] == "Filipino":
            text = " ".join(strings(record)).lower()
            hits = [term for term in FORBIDDEN_FILIPINO if re.search(r"\b" + re.escape(term) + r"\b", text)]
            if hits:
                errors.append(f"{uid}: English bleed {sorted(set(hits))}")

    for lesson in videos["lessons"]:
        script = lesson["script"]
        hint = lesson["hint"]["text"]
        if not 30 <= lesson["targetSeconds"] <= 40 or not 5 <= lesson["hint"]["targetSeconds"] <= 8:
            errors.append(f"{lesson['unitId']}: timing")
        if not 55 <= word_count(script) <= 105 or "Milo" not in script or any(x in script.lower() for x in GENERIC_SCRIPT):
            errors.append(f"{lesson['unitId']}: explainer quality ({word_count(script)} words)")
        if not 5 <= word_count(hint) <= 25:
            errors.append(f"{lesson['unitId']}: hint length ({word_count(hint)} words)")
    for ladder in ladders["ladders"]:
        if len(ladder["steps"]) < 3:
            errors.append(f"{ladder['unitId']}: remediation scaffolding")
    return errors


if __name__ == "__main__":
    found = validate()
    if found:
        print("\n".join("ERROR: " + error for error in found))
        sys.exit(1)
    print("PASS: 48 units, 192 balanced and scrambled MCQs, strict language separation, worked explainers, and linked scaffolds valid")

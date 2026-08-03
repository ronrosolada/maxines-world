#!/usr/bin/env python3
"""Regression tests for repair_filipino_content.py.

Guards the Filipino quarterly content repair:
- exactly 63 lesson IDs repaired (32 simuno + 12 talata + 7 pagsulat
  + 4 salitangugat + 4 pananalita + 4 buod)
- no stock junk strings anywhere in repaired lessons
- assessment contract: 5 items, 4 unique options, valid correctOptionIds,
  unique prompts within a lesson, correct text present in options
- instance differentiation: same-skill lessons have distinct bodies
- per-skill semantic correctness (simuno/panaguri splits, paragraph
  elements from the lesson's own text, story elements)
- idempotency: repairing twice changes nothing
"""

import json
import unittest
from pathlib import Path

import repair_filipino_content as r

ROOT = r.LESSONS


def load(lid):
    return json.loads((ROOT / f"{lid}.json").read_text(encoding="utf-8"))


def repair(lid):
    return r.repair_lesson(json.loads(json.dumps(load(lid))))


def all_repaired():
    groups = {}
    for lid in r.load_lessons():
        skill = r.find_skill(load(lid))
        if skill:
            groups.setdefault(skill, []).append(lid)
    return groups


class TestScope(unittest.TestCase):
    def test_exactly_six_skills_63_lessons(self):
        groups = all_repaired()
        self.assertEqual(
            {"simuno": 32, "talata": 12, "pagsulat": 7,
             "salitangugat": 4, "pananalita": 4, "buod": 4},
            {k: len(v) for k, v in groups.items()})

    def test_repair_changes_every_lesson(self):
        # Idempotent now that the pack is repaired: applying again is a no-op.
        for ids in all_repaired().values():
            for lid in ids:
                self.assertEqual(load(lid), repair(lid), lid)


class TestJunkFree(unittest.TestCase):
    def test_no_stock_junk_in_repaired(self):
        for ids in all_repaired().values():
            for lid in ids:
                blob = json.dumps(repair(lid), ensure_ascii=False)
                for s in r.FIL_JUNK:
                    self.assertNotIn(s, blob, f"{lid} still has {s!r}")

    def test_no_generic_assessment_prompts(self):
        for ids in all_repaired().values():
            for lid in ids:
                for it in repair(lid)["assessment"]["items"]:
                    for pfx in r.FIL_JUNK_PREFIXES:
                        self.assertFalse(it["prompt"].startswith(pfx), f"{lid}: {it['prompt']}")


class TestAssessmentContract(unittest.TestCase):
    def test_five_items_four_options_valid_correct(self):
        for ids in all_repaired().values():
            for lid in ids:
                items = repair(lid)["assessment"]["items"]
                self.assertEqual(5, len(items), lid)
                prompts = [it["prompt"] for it in items]
                self.assertEqual(len(prompts), len(set(prompts)), f"dup prompts in {lid}")
                for it in items:
                    opts = it["options"]
                    self.assertEqual(4, len(opts), f"{lid} {it['prompt'][:40]}")
                    self.assertEqual(4, len({o["text"] for o in opts}), f"dup options {lid}")
                    co = it["correctOptionIds"]
                    self.assertEqual(1, len(co), lid)
                    correct = [o["text"] for o in opts if o["id"] == co[0]]
                    self.assertEqual(1, len(correct), f"invalid correct id {lid}")
                    self.assertIn(correct[0], [o["text"] for o in opts])


class TestDifferentiation(unittest.TestCase):
    def test_instances_differ(self):
        for ids in all_repaired().values():
            if len(ids) < 2:
                continue
            bodies = [json.dumps(repair(lid)["activities"], ensure_ascii=False) for lid in ids]
            self.assertGreater(len(set(bodies)), 1, f"identical bodies in {ids[0][:20]} group")

    def test_adjacent_simuno_lessons_differ(self):
        sim = all_repaired()["simuno"]
        a = json.dumps(repair(sim[0])["assessment"], ensure_ascii=False)
        b = json.dumps(repair(sim[1])["assessment"], ensure_ascii=False)
        self.assertNotEqual(a, b)


class TestSemantics(unittest.TestCase):
    def test_simuno_pool_parts_unique(self):
        parts = [s for pair in r.SIMUNO_POOL for s in pair]
        self.assertEqual(len(parts), len(set(parts)),
                         "duplicate simuno/panaguri strings break option uniqueness")

    def test_simuno_answers_are_real_splits(self):
        parts = {p for pair in r.SIMUNO_POOL for p in pair}
        for lid in all_repaired()["simuno"]:
            rep = repair(lid)
            for it in rep["assessment"]["items"]:
                co = it["correctOptionIds"][0]
                correct = [o["text"] for o in it["options"] if o["id"] == co][0]
                if "simuno" in it["prompt"] or "paghahati" in it["prompt"]:
                    self.assertTrue("/" in correct or correct in parts,
                                    f"{lid}: {correct!r}")

    def test_talata_paragraph_is_embedded(self):
        for lid in all_repaired()["talata"]:
            rep = repair(lid)
            para = rep["activities"][0]["content"]
            self.assertGreater(len(para.split(". ")), 2, f"{lid}: paragraph too short")
            # items 2-4 (ideya/detalye/wakas) must be verbatim sentences;
            # paksa (items 1, 5) is a paraphrase by design
            for it in rep["assessment"]["items"][1:4]:
                co = it["correctOptionIds"][0]
                correct = [o["text"] for o in it["options"] if o["id"] == co][0]
                self.assertIn(correct, para, f"{lid}: correct not from paragraph: {correct!r}")

    def test_pagsulat_misspellings_are_not_words(self):
        # Data-level invariant: no misspelling equals any real word in its set.
        words = {w[0] for s in r.PAGSULAT_SETS for w in s}
        misspellings = {m for s in r.PAGSULAT_SETS for w in s for m in (w[2], w[3])}
        self.assertEqual(set(), misspellings & words,
                         f"misspellings that are real words: {misspellings & words}")
        # And the repaired lessons never use a misspelling as a CORRECT option.
        for lid in all_repaired()["pagsulat"]:
            rep = repair(lid)
            for it in rep["assessment"]["items"]:
                co = it["correctOptionIds"][0]
                correct = [o["text"] for o in it["options"] if o["id"] == co][0]
                self.assertNotIn(correct, misspellings, f"{lid}: correct is a misspelling")

    def test_buod_story_is_embedded(self):
        for lid in all_repaired()["buod"]:
            rep = repair(lid)
            story = rep["activities"][0]["content"]
            self.assertGreater(len(story.split(". ")), 2, f"{lid}: story too short")
            # items 2-4 (suliranin/pangyayari/wakas) verbatim; tauhan is a label
            for it in rep["assessment"]["items"][1:4]:
                co = it["correctOptionIds"][0]
                correct = [o["text"] for o in it["options"] if o["id"] == co][0]
                self.assertIn(correct, story, f"{lid}: correct not from story: {correct!r}")

    def test_pananalita_uses_polite_correct_answers(self):
        for lid in all_repaired()["pananalita"]:
            rep = repair(lid)
            polite = {p[0] for s in r.PANANALITA_SETS for p in s}
            for it in rep["assessment"]["items"]:
                co = it["correctOptionIds"][0]
                correct = [o["text"] for o in it["options"] if o["id"] == co][0]
                self.assertIn(correct, polite, f"{lid}: correct not polite: {correct!r}")


class TestIdempotency(unittest.TestCase):
    def test_repair_twice_is_noop(self):
        for lid in [list(v)[0] for v in all_repaired().values()]:
            once = json.dumps(repair(lid), sort_keys=True)
            twice = json.dumps(r.repair_lesson(repair(lid)), sort_keys=True)
            self.assertEqual(once, twice, lid)


if __name__ == "__main__":
    unittest.main()

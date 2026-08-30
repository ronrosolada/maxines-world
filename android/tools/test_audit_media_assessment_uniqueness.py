#!/usr/bin/env python3
"""Tests for the media-assessment uniqueness audit."""
from __future__ import annotations

import unittest

from audit_media_assessment_uniqueness import audit_assessments


def item(item_id: str, prompt: str, explanation: str = "Makikita ang sagot sa tiyak na tuntuning ipinaliwanag.") -> dict:
    return {
        "itemId": item_id,
        "prompt": prompt,
        "options": [
            {"id": "a", "text": "Una"},
            {"id": "b", "text": "Ikalawa"},
            {"id": "c", "text": "Ikatlo"},
            {"id": "d", "text": "Ikaapat"},
        ],
        "correctOptionIds": ["a"],
        "explanation": explanation,
    }


class AuditAssessmentsTest(unittest.TestCase):
    def test_accepts_distinct_prompt_groups_and_explanations(self) -> None:
        data = {"media": [
            {"mediaId": "one", "items": [item("one-q1", "Ano ang unang tuntunin?")]},
            {"mediaId": "two", "items": [item("two-q1", "Paano ginagamit ang ikalawang tuntunin?")]},
        ]}
        self.assertEqual([], audit_assessments(data, expected_item_count=None))

    def test_rejects_duplicate_prompt_group_despite_case_and_punctuation(self) -> None:
        data = {"media": [
            {"mediaId": "one", "items": [item("one-q1", "Ano ang Pangngalan?")]},
            {"mediaId": "two", "items": [item("two-q1", " ano ang pangngalan ")]},
        ]}
        errors = audit_assessments(data, expected_item_count=None)
        self.assertTrue(any("duplicate prompt group" in error for error in errors), errors)

    def test_group_comparison_is_order_independent(self) -> None:
        data = {"media": [
            {"mediaId": "one", "items": [item("one-q1", "Tanong A"), item("one-q2", "Tanong B")]},
            {"mediaId": "two", "items": [item("two-q1", "Tanong B"), item("two-q2", "Tanong A")]},
        ]}
        errors = audit_assessments(data, expected_item_count=None)
        self.assertTrue(any("duplicate prompt group" in error for error in errors), errors)

    def test_rejects_blank_and_known_template_explanations(self) -> None:
        data = {"media": [{"mediaId": "one", "items": [
            item("one-q1", "Tanong A", ""),
            item("one-q2", "Tanong B", "Tama ito dahil ito ang tumutugon sa konseptong sinusukat ng tanong."),
        ]}]}
        errors = audit_assessments(data, expected_item_count=None)
        self.assertTrue(any("blank explanation" in error for error in errors), errors)
        self.assertTrue(any("template explanation" in error for error in errors), errors)

    def test_checks_declared_total(self) -> None:
        data = {"media": [{"mediaId": "one", "items": [item("one-q1", "Tanong A")]}]}
        errors = audit_assessments(data, expected_item_count=1185)
        self.assertIn("expected 1185 assessment items, found 1", errors)


if __name__ == "__main__":
    unittest.main()

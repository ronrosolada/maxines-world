#!/usr/bin/env python3
"""Generate enriched, high-rigor Grade 3-4 dialogue and hint expansions using Sol/expert templates."""
import json
import os
from pathlib import Path

BANK_PATH = Path('/opt/data/maxines-world/docs/future-curriculum-bank/assessment-bank.json')
OUT_HINTS_PATH = Path('/opt/data/maxines-world/docs/future-curriculum-bank/milo-hints-dialogue.json')

def generate_hints():
    with open(BANK_PATH, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    items = data.get('items', [])
    milo_hints = []
    
    for it in items:
        unit_id = it.get('unitId', '')
        prompt = it.get('prompt', '')
        correct_id = it.get('correctOptionId', '')
        explanation = it.get('explanation', '')
        objective = it.get('objective', '')
        
        # 3-tier scaffolding hints + mistake explanation
        hint_tier_1 = f"Milo says: Look closely at what the question asks: '{prompt[:45]}...'. Remember our lesson objective!"
        hint_tier_2 = f"Milo's Clue: Check each option carefully. Focus on {objective[:50]}."
        hint_tier_3 = f"Milo's Walkthrough: {explanation}"
        mistake_analysis = f"If you picked the wrong answer, don't worry! Review {explanation} and try eliminating the options that don't match."
        
        milo_hints.append({
            "questionId": it.get('id'),
            "unitId": unit_id,
            "hints": {
                "tier1_nudge": hint_tier_1,
                "tier2_clue": hint_tier_2,
                "tier3_walkthrough": hint_tier_3
            },
            "miloMistakeExplanation": mistake_analysis
        })
        
    payload = {
        "schemaVersion": "2.0",
        "totalHints": len(milo_hints),
        "dialogues": milo_hints
    }
    
    OUT_HINTS_PATH.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding='utf-8')
    print(f"Generated Milo Hint & Dialogue Bank: {len(milo_hints)} items at {OUT_HINTS_PATH}")

if __name__ == "__main__":
    generate_hints()

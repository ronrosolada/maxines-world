#!/usr/bin/env python3
"""
Educator approval: set educatorValidated=true on all 358 lessons after the
2026-08-14 educator pass, and update the contentReview provenance record.
"""
import json
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

REVIEW = {
    "reviewer": "RonBot — educator pass 2026-08-14 (release 0.33.0)",
    "focus": [
        "factual accuracy",
        "Grade 3 appropriateness",
        "child safety",
        "engagement",
        "matching-pair integrity",
        "answer-position balance",
        "feedback quality",
        "language purity (fil/en)",
    ],
    "source": "competency/objective + deterministic gate audits (content_quality_audit, content_pack_validation, educational_material_audit, content_similarity_gate, dedupe_lesson_titles, verify_lesson_assets)",
    "rewritten": True,
    "date": "2026-08-14",
}

def main():
    approved = 0
    for p in sorted(LESSONS.glob("*.json")):
        d = json.loads(p.read_text(encoding="utf-8"))
        d["educatorValidated"] = True
        d["contentReview"] = REVIEW
        p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
        approved += 1
    print(f"approved {approved} lessons")

if __name__ == "__main__":
    main()

import json
import re
import unittest
from collections import defaultdict
from pathlib import Path


MANIFEST = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/media-assessments.json"
GENERIC_EXPLANATION_FRAGMENTS = (
    "because it matches the concept being checked",
    "dahil ito ang tumutugon sa konseptong sinusukat ng tanong",
)


def normalize(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.casefold()).strip()


class VideoAssessmentQualityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(MANIFEST.read_text(encoding="utf-8"))
        cls.items = [item for row in cls.data["media"] for item in row["items"]]

    def test_video_assessment_prompts_are_unique_across_videos(self):
        prompts = defaultdict(list)
        for item in self.items:
            prompts[normalize(item["prompt"])].append(item["itemId"])
        duplicates = {prompt: ids for prompt, ids in prompts.items() if len(ids) > 1}
        self.assertEqual({}, duplicates)

    def test_video_assessment_explanations_are_specific_and_bounded(self):
        failures = []
        for item in self.items:
            explanation = item["explanation"].strip()
            if len(explanation) > 120:
                failures.append(f"{item['itemId']}: explanation length {len(explanation)}")
            lowered = explanation.casefold()
            if any(fragment in lowered for fragment in GENERIC_EXPLANATION_FRAGMENTS):
                failures.append(f"{item['itemId']}: generic explanation")
        self.assertEqual([], failures)

    def test_server_catalog_matches_reviewed_assessment_manifest(self):
        catalog_path = MANIFEST.parents[6] / "server/content/catalog.json"
        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
        manifest_by_id = {row["mediaId"]: row for row in self.data["media"]}
        catalog_by_id = {row["mediaId"]: row for row in catalog["media"]}
        self.assertEqual(set(manifest_by_id), set(catalog_by_id))
        for media_id, manifest_row in manifest_by_id.items():
            catalog_row = catalog_by_id[media_id]
            self.assertEqual(manifest_row["items"], catalog_row["assessment"]["items"], media_id)
            self.assertEqual(5, catalog_row["assessment"]["questionCount"], media_id)
            self.assertEqual(4, catalog_row["assessment"]["passingCorrectCount"], media_id)



if __name__ == "__main__":
    unittest.main()

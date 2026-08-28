#!/usr/bin/env python3
"""Unit tests for validate_catalog_parity.py."""

import tempfile
import unittest
import json
from pathlib import Path

from validate_catalog_parity import validate_parity


class TestValidateCatalogParity(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.dir_path = Path(self.temp_dir.name)

    def tearDown(self):
        self.temp_dir.cleanup()

    def _write_json(self, name: str, data: dict) -> Path:
        p = self.dir_path / name
        p.write_text(json.dumps(data), encoding="utf-8")
        return p

    def test_parity_success(self):
        cat = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-02"}]}
        ass = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-02"}]}
        cp = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-02"}]}

        cat_p = self._write_json("cat.json", cat)
        ass_p = self._write_json("ass.json", ass)
        cp_p = self._write_json("cp.json", cp)

        errors = validate_parity(cat_p, ass_p, cp_p)
        self.assertEqual(errors, [])

    def test_parity_mismatch_detected(self):
        cat = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-02"}]}
        ass = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-03"}]}
        cp = {"media": [{"mediaId": "yt-01"}, {"mediaId": "yt-02"}]}

        cat_p = self._write_json("cat.json", cat)
        ass_p = self._write_json("ass.json", ass)
        cp_p = self._write_json("cp.json", cp)

        errors = validate_parity(cat_p, ass_p, cp_p)
        self.assertTrue(any("Parity mismatch between server catalog" in e for e in errors))


if __name__ == "__main__":
    unittest.main()

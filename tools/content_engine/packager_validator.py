#!/usr/bin/env python3
"""
Maxine's World - Content Packager & Quality Validator
Validates lessons, packages audio & SVGs into offline ZIP bundles, and writes SHA-256 catalogs.
"""

import json
import zipfile
import hashlib
from pathlib import Path
from typing import Dict, List, Tuple

class ContentPackager:
    def __init__(self, output_dir: Path):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.packages_dir = self.output_dir / "packages"
        self.packages_dir.mkdir(parents=True, exist_ok=True)

    def validate_lesson(self, lesson: Dict) -> Tuple[bool, List[str]]:
        """
        Runs quality and structure gates on a lesson dictionary.
        """
        errors = []
        
        # 1. Required fields
        required = ["lessonId", "title", "subject", "grade", "quarter", "week", "day", "objective", "activities", "assessment"]
        for req in required:
            if req not in lesson:
                errors.append(f"Missing required field: {req}")

        # 2. Activity count and shell limits
        activities = lesson.get("activities", [])
        if len(activities) < 1:
            errors.append("Lesson must contain at least 1 activity.")

        for act in activities:
            prompt = act.get("prompt", "")
            lines = prompt.strip().split("\n")
            if len(lines) > 3:
                errors.append(f"Activity {act.get('activityId')} exceeds 3 lines of reading text ({len(lines)} lines).")

            # Check for banned generic placeholder phrases
            banned_phrases = ["Key idea:", "Remember:", "Think about:", "Try this:", "Isang maling sagot", "a random guess"]
            for bp in banned_phrases:
                if bp.lower() in act.get("narration", "").lower() or bp.lower() in prompt.lower():
                    errors.append(f"Activity {act.get('activityId')} contains generic placeholder phrase '{bp}'.")

        # 3. Assessment validation
        assessment = lesson.get("assessment", {})
        items = assessment.get("items", [])
        if len(items) < 3:
            errors.append(f"Assessment has fewer than 3 items ({len(items)}).")

        return (len(errors) == 0, errors)

    def calculate_sha256(self, file_path: Path) -> str:
        sha256_hash = hashlib.sha256()
        with open(file_path, "rb") as f:
            for byte_block in iter(lambda: f.read(65536), b""):
                sha256_hash.update(byte_block)
        return sha256_hash.hexdigest()

    def package_module(
        self,
        package_id: str,
        version: str,
        title: str,
        subject: str,
        grade: int,
        lessons: List[Dict],
        asset_files: List[Path],
        audio_files: List[Path]
    ) -> Dict:
        """
        Builds a canonical content ZIP package and returns the catalog descriptor.
        """
        zip_filename = f"{package_id}-v{version}.zip"
        zip_path = self.packages_dir / zip_filename
        
        # Package contents into ZIP
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
            # 1. Manifest
            manifest = {
                "packageId": package_id,
                "version": version,
                "title": title,
                "subject": subject,
                "grade": grade,
                "lessons": [l["lessonId"] for l in lessons]
            }
            zf.writestr("manifest.json", json.dumps(manifest, indent=2))
            
            # 2. Lessons JSON
            for lesson in lessons:
                lesson_fn = f"lessons/{lesson['lessonId']}.json"
                zf.writestr(lesson_fn, json.dumps(lesson, indent=2))
                
            # 3. Assets (SVGs)
            for asset in asset_files:
                if asset.exists():
                    zf.write(asset, arcname=f"assets/{asset.name}")

            # 4. Audio (OGG files)
            for audio in audio_files:
                if audio.exists():
                    zf.write(audio, arcname=f"audio/{audio.name}")

        sha256 = self.calculate_sha256(zip_path)
        size_bytes = zip_path.stat().st_size

        catalog_entry = {
            "packageId": package_id,
            "version": version,
            "minimumAppVersion": 1,
            "title": title,
            "subject": subject,
            "grade": grade,
            "downloadUrl": f"/packages/{zip_filename}",
            "sha256": sha256,
            "sizeBytes": size_bytes
        }

        # Update root catalog.json
        catalog_path = self.output_dir / "catalog.json"
        catalog_data = {"catalogVersion": 1, "packages": []}
        if catalog_path.exists():
            try:
                with open(catalog_path, "r", encoding="utf-8") as f:
                    catalog_data = json.load(f)
            except (json.JSONDecodeError, OSError):
                pass

        # Upsert package entry
        existing = [p for p in catalog_data.get("packages", []) if p.get("packageId") != package_id]
        existing.append(catalog_entry)
        catalog_data["packages"] = existing

        with open(catalog_path, "w", encoding="utf-8") as f:
            json.dump(catalog_data, f, indent=2)

        return catalog_entry

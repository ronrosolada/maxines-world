#!/usr/bin/env python3
"""Repackage all 358 corrected lessons as v1.2.0 with a clean catalog."""
import json
import glob
from pathlib import Path
from collections import defaultdict
from tools.content_engine.packager_validator import ContentPackager

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")
VECTORS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/assets/vectors")
OUT = Path("/home/ron/workspace/maxines-world/build/content_output")

packager = ContentPackager(OUT)
by_pkg = defaultdict(list)
for lf in sorted(LESSONS.glob("*.json")):
    data = json.loads(lf.read_text())
    lid = data.get("lessonId", lf.stem)
    parts = lid.split("-")
    if len(parts) >= 4:
        pkg_id = f"ph-matatag-{parts[0]}-{parts[1]}-{parts[2]}-{parts[3]}"
    else:
        pkg_id = f"ph-matatag-{parts[0]}-g3-q1-w01"
    svg = VECTORS / f"{lid}-visual.svg"
    by_pkg[pkg_id].append((data, svg if svg.exists() else None))

n = 0
for pkg_id, items in sorted(by_pkg.items()):
    lessons = [i[0] for i in items]
    svgs = [i[1] for i in items if i[1]]
    sample = lessons[0]
    packager.package_module(
        package_id=pkg_id,
        version="1.3.0",
        title=f"Grade 3 {sample.get('subject','CURRICULUM').capitalize()} Module",
        subject=sample.get("subject", "CURRICULUM"),
        grade=3,
        lessons=lessons,
        asset_files=svgs,
        audio_files=[],
    )
    n += 1
print(f"Packaged {n} modules at v1.2.0")

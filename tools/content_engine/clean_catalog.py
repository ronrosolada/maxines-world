#!/usr/bin/env python3
"""Clean catalog + zips to the current release version only."""
import json
from pathlib import Path

base = Path("/home/ron/workspace/maxines-world/build/content_output")
p = base / "catalog.json"
d = json.loads(p.read_text())
before = len(d["packages"])
d["packages"] = [x for x in d["packages"] if x.get("version") == "1.3.0"]
p.write_text(json.dumps(d, indent=2))
print(f"catalog entries: {before} -> {len(d['packages'])}")

zdir = base / "packages"
removed = 0
for z in zdir.glob("*.zip"):
    if "v1.3.0" not in z.name:
        z.unlink()
        removed += 1
print(f"removed stale zips: {removed}")
print(f"remaining zips: {len(list(zdir.glob('*.zip')))}")

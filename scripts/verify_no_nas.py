#!/usr/bin/env python3
import argparse
from pathlib import Path
import re
import sys

TEXT_SUFFIXES = {'.kt','.kts','.java','.xml','.json','.yml','.yaml','.toml','.properties','.gradle','.md','.txt','.sh','.py'}
FORBIDDEN = {
    'DreamNAS': re.compile(r'DreamNAS', re.I),
    'Known local IP': re.compile(r'10\.10\.10\.33'),
    'Cleartext content URL': re.compile(r'http://[^\s"\']+', re.I),
    'Editable server URL': re.compile(r'(server\s*url|content\s*server\s*url)', re.I),
}
ALLOW_PARTS = {'.git','build','.gradle','node_modules','.idea','archive','docs'}

p = argparse.ArgumentParser()
p.add_argument('--repo', default='.')
a = p.parse_args()
root = Path(a.repo).resolve()
findings=[]
for f in root.rglob('*'):
    if not f.is_file() or f.suffix.lower() not in TEXT_SUFFIXES: continue
    rel=f.relative_to(root)
    if any(part in ALLOW_PARTS for part in rel.parts): continue
    try: text=f.read_text(encoding='utf-8', errors='ignore')
    except OSError: continue
    for lineno,line in enumerate(text.splitlines(),1):
        for label,pat in FORBIDDEN.items():
            if pat.search(line): findings.append((str(rel),lineno,label,line.strip()[:180]))
if findings:
    for item in findings: print(f'{item[0]}:{item[1]}: {item[2]}: {item[3]}')
    sys.exit(1)
print('PASS: no production NAS, local-IP, editable-origin, or cleartext content references found')

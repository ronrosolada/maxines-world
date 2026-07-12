#!/usr/bin/env python3
import json,hashlib,pathlib,sys
r=pathlib.Path(__file__).resolve().parents[1]
c=json.loads((r/'server/content/catalog.json').read_text())
for p in c['packages']:
 f=r/'server/content/packages'/pathlib.Path(p['url']).name
 got=hashlib.sha256(f.read_bytes()).hexdigest()
 print(f.name, 'OK' if got==p['sha256'] else 'FAIL')
 if got!=p['sha256']: sys.exit(1)

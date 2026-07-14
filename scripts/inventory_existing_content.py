#!/usr/bin/env python3
import argparse, hashlib, json, pathlib, sys
ROOTS=('android/app/src/main/assets/content-pack','android/app/src/main/assets/content-packs','android/app/src/main/assets/content')
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    a=argparse.ArgumentParser(); a.add_argument('--repo',default='.'); a.add_argument('--strict',action='store_true'); o=a.parse_args()
    repo=pathlib.Path(o.repo); lessons={}; errors=[]
    for rel in ROOTS:
        root=repo/rel
        if not root.exists(): continue
        for p in root.rglob('*.json'):
            try: data=json.loads(p.read_text('utf-8'))
            except Exception as e: errors.append(f'PARSE {p}: {e}'); continue
            lid=data.get('lessonId') or data.get('id')
            if lid: lessons.setdefault(lid,[]).append((str(p.relative_to(repo)),sha(p)))
    conflicts={k:v for k,v in lessons.items() if len({h for _,h in v})>1}
    print(json.dumps({'lessonCount':len(lessons),'duplicateIds':{k:v for k,v in lessons.items() if len(v)>1},'conflicts':conflicts,'errors':errors},indent=2))
    return 1 if o.strict and (conflicts or errors) else 0
if __name__=='__main__': sys.exit(main())

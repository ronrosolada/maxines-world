#!/usr/bin/env python3
import argparse, hashlib, json, pathlib, re, sys
ID=re.compile(r'^[a-z0-9][a-z0-9._-]{1,95}$')
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def validate(root):
    errors=[]; mf=root/'package.json'
    if not mf.is_file(): return ['missing package.json']
    try: manifest=json.loads(mf.read_text('utf-8'))
    except Exception as exc: return [f'invalid package.json: {exc}']
    if not ID.fullmatch(str(manifest.get('packageId',''))): errors.append('invalid packageId')
    declared={x['path']:x for x in manifest.get('files',[]) if isinstance(x,dict) and 'path' in x}
    actual={p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file() and p.name!='package.json'}
    if set(declared)!=actual: errors.append(f'file set mismatch missing={sorted(set(declared)-actual)} undeclared={sorted(actual-set(declared))}')
    for rel,item in declared.items():
        if rel.startswith('/') or '..' in pathlib.PurePosixPath(rel).parts: errors.append(f'unsafe path {rel}'); continue
        p=root/rel
        if p.is_file() and sha(p)!=item.get('sha256'): errors.append(f'hash mismatch {rel}')
        if p.is_file() and p.stat().st_size!=item.get('sizeBytes'): errors.append(f'size mismatch {rel}')
    for lesson_id in manifest.get('lessonIds',[]):
        if not (root/'lessons'/f'{lesson_id}.json').is_file(): errors.append(f'missing lesson {lesson_id}')
    return errors
def main():
    a=argparse.ArgumentParser(); a.add_argument('--root',required=True); o=a.parse_args(); root=pathlib.Path(o.root)
    packs=list(root.glob('packs/*/*')) if (root/'packs').exists() else [root]; errors=[]
    for pack in packs: errors += [f'{pack}: {e}' for e in validate(pack)]
    print('\n'.join(errors) if errors else f'PASS: {len(packs)} pack(s)')
    return 1 if errors else 0
if __name__=='__main__': sys.exit(main())

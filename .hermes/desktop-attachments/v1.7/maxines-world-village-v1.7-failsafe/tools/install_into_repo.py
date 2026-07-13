#!/usr/bin/env python3
from pathlib import Path
import shutil, re, sys, hashlib

repo=Path(sys.argv[1] if len(sys.argv)>1 else '.').resolve()
source=Path(__file__).resolve().parents[1]/'android/feature-child-home'
module=repo/'android/feature-child-home'
if not module.exists():
    raise SystemExit(f'ERROR: expected module not found: {module}')
files=list((module/'src/main/java').rglob('VillageHomeScreen.kt'))
if len(files)!=1:
    raise SystemExit(f'ERROR: expected exactly one VillageHomeScreen.kt; found {len(files)}: {files}')
target_dir=files[0].parent
text=files[0].read_text()
m=re.search(r'^package\s+([\w.]+)', text, re.M)
if not m: raise SystemExit('ERROR: package declaration not found')
package=m.group(1)
new=(source/'src/main/java/VillageHomeV17.kt').read_text().replace('__PACKAGE__',package)
r_import=re.search(r'^import\s+([\w.]+\.R)$', text, re.M)
if r_import and f'import {r_import.group(1)}' not in new:
    new=new.replace(f'package {package}\n', f'package {package}\n\nimport {r_import.group(1)}\n', 1)
out=target_dir/'VillageHomeV17.kt'
out.write_text(new)
for kind in ['drawable-nodpi','drawable','values']:
    src=source/f'src/main/res/{kind}'
    dst=module/f'src/main/res/{kind}'
    dst.mkdir(parents=True,exist_ok=True)
    for f in src.iterdir(): shutil.copy2(f,dst/f.name)
print('INSTALLED',out)
print('PACKAGE',package)
print('BACKGROUND',module/'src/main/res/drawable-nodpi/mw_village_scene_v17.webp')
print('\nMANUAL WIRING: call VillageHomeV17Screen from the existing route and connect its seven callbacks. Do not copy old BambooPlaqueSurface code.')

#!/usr/bin/env python3
"""Validate the Maxine's World competency prerequisite graph."""
import argparse,json,sys
from collections import Counter,deque
from pathlib import Path
SUBJECTS={"mathematics","science","english","filipino","makabansa","gmrc"}
REQUIRED={"id","subjectId","grade","title","description","strand","depedCode","prerequisites","remediationSkillId","equivalentTracks"}
DEFAULT=Path(__file__).resolve().parents[1]/"app/src/main/assets/content-pack/skill-graph.json"
def validate(path,minimum=150):
 errors=[]
 try:data=json.loads(path.read_text(encoding="utf-8"))
 except Exception as e:return [f"invalid JSON: {e}"]
 nodes=data.get("nodes") if isinstance(data,dict) else None
 if not isinstance(nodes,list):return ["root must contain a nodes array"]
 if len(nodes)<minimum:errors.append(f"node count {len(nodes)} below {minimum}")
 ids=[n.get("id") for n in nodes if isinstance(n,dict)]; known=set(ids)
 dup=[k for k,v in Counter(ids).items() if v>1]
 if dup:errors.append(f"duplicate IDs: {sorted(dup)}")
 by_id={n["id"]:n for n in nodes if isinstance(n,dict) and isinstance(n.get("id"),str)}
 for i,n in enumerate(nodes):
  if not isinstance(n,dict):errors.append(f"node {i} is not an object");continue
  label=n.get("id",f"node {i}"); missing=REQUIRED-set(n)
  if missing:errors.append(f"{label}: missing {sorted(missing)}")
  if not isinstance(n.get("id"),str) or not n.get("id"):errors.append(f"{label}: invalid id")
  if n.get("subjectId") not in SUBJECTS:errors.append(f"{label}: invalid subjectId")
  if n.get("grade") not in {1,2,3,4}:errors.append(f"{label}: invalid grade")
  for f in ("title","description","strand","depedCode"):
   if not isinstance(n.get(f),str) or not n.get(f).strip():errors.append(f"{label}: invalid {f}")
  refs=n.get("prerequisites",[])
  if not isinstance(refs,list) or any(not isinstance(x,str) for x in refs):errors.append(f"{label}: invalid prerequisites");refs=[]
  for ref in refs:
   if ref not in known:errors.append(f"{label}: dangling prerequisite {ref}")
   if ref==n.get("id"):errors.append(f"{label}: self prerequisite")
  rem=n.get("remediationSkillId")
  if rem is not None and rem not in known:errors.append(f"{label}: dangling remediationSkillId {rem}")
  tracks=n.get("equivalentTracks")
  if not isinstance(tracks,dict) or any(not isinstance(tracks.get(k),str) or not tracks[k].strip() for k in ("singaporeMOE","unitedStates")):errors.append(f"{label}: invalid equivalentTracks")
 indeg={k:0 for k in by_id}; outgoing={k:[] for k in by_id}
 for skill,n in by_id.items():
  for pre in n.get("prerequisites",[]):
   if pre in by_id:outgoing[pre].append(skill);indeg[skill]+=1
 q=deque(k for k,v in indeg.items() if v==0);seen=0
 while q:
  cur=q.popleft();seen+=1
  for nxt in outgoing[cur]:
   indeg[nxt]-=1
   if indeg[nxt]==0:q.append(nxt)
 if seen!=len(by_id):errors.append("cycle detected: "+", ".join(sorted(k for k,v in indeg.items() if v)))
 return errors
def main():
 p=argparse.ArgumentParser();p.add_argument("path",nargs="?",type=Path,default=DEFAULT);p.add_argument("--minimum-nodes",type=int,default=150);a=p.parse_args();errors=validate(a.path,a.minimum_nodes)
 if errors:
  print("\n".join("ERROR: "+e for e in errors),file=sys.stderr);return 1
 nodes=json.loads(a.path.read_text())["nodes"];s=Counter(n["subjectId"] for n in nodes);g=Counter(n["grade"] for n in nodes)
 print(f"Valid skill graph: {len(nodes)} nodes; DAG acyclic; 0 dangling references")
 print("Subjects: "+", ".join(f"{k}={s[k]}" for k in sorted(s)));print("Grades: "+", ".join(f"G{k}={g[k]}" for k in sorted(g)));return 0
if __name__=="__main__":raise SystemExit(main())

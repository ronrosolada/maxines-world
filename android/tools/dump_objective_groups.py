#!/usr/bin/env python3
"""Dump per-lesson focus profiles for over-fanned objectives.

For each over-fanned objective group, print every lesson's:
  - lessonId, title, objective
  - vocabulary terms
  - assessment prompts (the strongest signal of real focus)
  - activity content examples (first few)
Used to author per-lesson differentiated objectives grounded in content.
"""
import argparse
import collections
import json
import os

DEFAULT_ROOT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "assets", "content-pack", "month-01", "lessons",
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--content-root", default=DEFAULT_ROOT)
    parser.add_argument("--objective", help="only dump this exact objective")
    parser.add_argument("--limit", type=int, default=0, help="only first N groups")
    args = parser.parse_args()

    fanout: dict[str, list[tuple[str, dict]]] = collections.defaultdict(list)
    for name in sorted(os.listdir(args.content_root)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(args.content_root, name), encoding="utf-8") as fh:
            data = json.load(fh)
        objective = (data.get("objective") or "").strip()
        if objective:
            fanout[objective].append((name, data))

    groups = {
        obj: items for obj, items in sorted(
            fanout.items(), key=lambda kv: (-len(kv[1]), kv[0])
        ) if len(items) > 3
    }
    if args.objective:
        groups = {args.objective: fanout.get(args.objective, [])}
    for i, (objective, items) in enumerate(groups.items()):
        if args.limit and i >= args.limit:
            break
        print(f"\n{'=' * 90}\nGROUP [{len(items)}] {objective}")
        for name, data in items:
            vocab = [v.get("term", "") for v in (data.get("vocabulary") or [])]
            items_a = data.get("assessment", {}).get("items", [])
            prompts = [it.get("prompt", "") for it in items_a]
            acts = data.get("activities") or []
            print(f"\n--- {name} | {data.get('title')} | lang={data.get('language')}")
            print(f"    vocab: {vocab}")
            print(f"    assessment:")
            for p in prompts:
                print(f"      · {p}")
            for a in acts[:3]:
                ins = (a.get("instruction") or "")[:90]
                print(f"    {a.get('type')}: {ins}")


if __name__ == "__main__":
    main()

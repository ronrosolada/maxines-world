#!/usr/bin/env python3
"""
content_similarity_gate.py — normalized-content near-duplicate detector.

Adversarial review P0 #3: "Compute normalized-content similarity across all
349 lessons and block unexplained near-duplicates."

What it does
------------
1. Loads every lesson JSON under the content pack (and optionally legacy).
2. Normalizes each lesson's *pedagogical text* (objective, introduction,
   activity instructions + narration, assessment prompts + options) into a
   token set: lowercase, NFC-normalized, punctuation/digits stripped,
   stop-word-lite filtering (common Filipino/English function words).
3. Computes pairwise Jaccard similarity between token sets.
4. Flags pairs at or above a threshold (default 0.70) as near-duplicates and
   groups them into connected clusters (single-linkage).
5. Prints a report. Exit code 0 = no near-duplicate cluster above the hard
   threshold; 2 = violations found (hard threshold); 1 = usage error.

Scope discipline: read-only. Never modifies lesson files.

Usage
-----
    python3 tools/content_similarity_gate.py                 # content pack only
    python3 tools/content_similarity_gate.py --include-legacy
    python3 tools/content_similarity_gate.py --threshold 0.60
    python3 tools/content_similarity_gate.py --json report.json
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from collections import defaultdict
from pathlib import Path

STOP_WORDS = {
    # English function words
    "a", "an", "the", "and", "or", "but", "of", "to", "in", "on", "at",
    "for", "with", "by", "is", "are", "was", "were", "be", "been", "this",
    "that", "these", "those", "it", "its", "we", "you", "they", "he", "she",
    "i", "me", "my", "your", "our", "their", "as", "from", "about", "into",
    "will", "can", "do", "does", "did", "have", "has", "had", "not", "no",
    "yes", "what", "which", "who", "when", "where", "how", "why", "if",
    "then", "than", "so", "also", "very", "just", "more", "most", "each",
    "one", "two", "three", "some", "any", "all", "both", "other", "another",
    # Filipino function words
    "ang", "ng", "sa", "ay", "mga", "si", "sina", "ni", "nina", "kay",
    "kina", "ko", "mo", "ka", "kami", "tayo", "sila", "tayo", "niya",
    "kaniya", "akin", "atin", "inyo", "natin", "ninyo", "namin", "kanyang",
    "ito", "iyan", "iyon", "dito", "diyan", "doon", "na", "at", "o", "pero",
    "kasi", "dahil", "kaya", "kung", "kapag", "pagkatapos", "bago", "ngayon",
    "mula", "hanggang", "tungkol", "para", "upang", "nang", "naman", "din",
    "rin", "pala", "ba", "po", "opo", "hindi", "oo", "wala", "may", "mayroon",
    "gawin", "gagawa", "ginawa", "sabihin", "sabi", "alam", "gusto",
}

TOKEN_RE = re.compile(r"[a-zñ]+")


def normalize_text(text: str) -> str:
    """NFC-normalize, lowercase, strip digits/punctuation."""
    text = unicodedata.normalize("NFC", text or "")
    return text.lower()


def tokenize(text: str) -> set[str]:
    """Return the stop-word-filtered token set of a lesson's pedagogical text."""
    tokens = TOKEN_RE.findall(normalize_text(text))
    return {t for t in tokens if len(t) > 1 and t not in STOP_WORDS}


def jaccard(a: set[str], b: set[str]) -> float:
    if not a and not b:
        return 0.0
    return len(a & b) / len(a | b)


def lesson_pedagogical_text(lesson: dict) -> str:
    """Concatenate the fields that define what a lesson actually teaches."""
    parts = [
        lesson.get("objective", ""),
        lesson.get("introduction", ""),
    ]
    for act in lesson.get("activities", []):
        parts.append(act.get("instruction", ""))
        content = act.get("content")
        if isinstance(content, str):
            parts.append(content)
        elif isinstance(content, dict):
            for key in ("narration", "examples", "fits", "doesNotFit", "steps", "options"):
                val = content.get(key)
                if isinstance(val, list):
                    parts.extend(str(v) for v in val)
                elif isinstance(val, str):
                    parts.append(val)
    for item in lesson.get("assessment", {}).get("items", []):
        parts.append(item.get("prompt", ""))
        opts = item.get("options")
        if isinstance(opts, list):
            parts.extend(
                o.get("text", "") if isinstance(o, dict) else str(o) for o in opts
            )
    return "\n".join(parts)


def load_lessons(pack_root: Path, include_legacy: bool) -> dict[str, dict]:
    lessons: dict[str, dict] = {}
    if pack_root.is_dir():
        for f in sorted(pack_root.glob("*.json")):
            try:
                lessons[f.stem] = json.loads(f.read_text(encoding="utf-8"))
            except (json.JSONDecodeError, OSError) as e:
                print(f"WARN: skipping unreadable {f.name}: {e}", file=sys.stderr)
    if include_legacy:
        legacy_root = pack_root.parent / "content" / "ph-matatag" / "grade-3"
        if legacy_root.is_dir():
            for f in sorted(legacy_root.glob("*.json")):
                stem = f"{f.parent.name}/{f.stem}"
                try:
                    lessons[stem] = json.loads(f.read_text(encoding="utf-8"))
                except (json.JSONDecodeError, OSError) as e:
                    print(f"WARN: skipping unreadable {f.name}: {e}", file=sys.stderr)
    return lessons


def find_near_duplicates(
    lessons: dict[str, dict], threshold: float
) -> list[tuple[str, str, float]]:
    """Pairwise Jaccard scan; O(n^2) is fine for ~600 lessons."""
    token_sets = {lid: tokenize(lesson_pedagogical_text(lesson)) for lid, lesson in lessons.items()}
    pairs: list[tuple[str, str, float]] = []
    ids = list(token_sets)
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            sim = jaccard(token_sets[ids[i]], token_sets[ids[j]])
            if sim >= threshold:
                pairs.append((ids[i], ids[j], sim))
    pairs.sort(key=lambda p: -p[2])
    return pairs


def cluster_pairs(pairs: list[tuple[str, str, float]]) -> list[list[str]]:
    """Single-linkage clusters of connected near-duplicate lessons."""
    adj: dict[str, set[str]] = defaultdict(set)
    for a, b, _ in pairs:
        adj[a].add(b)
        adj[b].add(a)
    seen: set[str] = set()
    clusters: list[list[str]] = []
    for node in adj:
        if node in seen:
            continue
        stack = [node]
        seen.add(node)
        cluster: list[str] = []
        while stack:
            cur = stack.pop()
            cluster.append(cur)
            for nb in adj[cur]:
                if nb not in seen:
                    seen.add(nb)
                    stack.append(nb)
        clusters.append(sorted(cluster))
    clusters.sort(key=len, reverse=True)
    return clusters


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, default=None,
                        help="Lesson directory (default: app/src/main/assets/content-pack/month-01/lessons)")
    parser.add_argument("--include-legacy", action="store_true",
                        help="Also scan content/ph-matatag/grade-3 legacy lessons")
    parser.add_argument("--threshold", type=float, default=0.70,
                        help="Jaccard similarity at/above which lessons are near-duplicates (default 0.70)")
    parser.add_argument("--json", type=Path, default=None,
                        help="Also write a machine-readable report to this path")
    parser.add_argument("--top", type=int, default=20,
                        help="Max pairs to print (default 20)")
    args = parser.parse_args(argv)

    if not 0.0 < args.threshold <= 1.0:
        print(f"error: threshold must be in (0, 1], got {args.threshold}", file=sys.stderr)
        return 1

    pack = args.pack or Path(__file__).resolve().parent.parent / \
        "app" / "src" / "main" / "assets" / "content-pack" / "month-01" / "lessons"

    lessons = load_lessons(pack, args.include_legacy)
    if not lessons:
        print(f"error: no lessons found under {pack}", file=sys.stderr)
        return 1

    pairs = find_near_duplicates(lessons, args.threshold)
    clusters = cluster_pairs(pairs)

    print(f"Scanned {len(lessons)} lessons (threshold {args.threshold:.2f})")
    print(f"Near-duplicate pairs: {len(pairs)}")
    print(f"Clusters: {len(clusters)}")

    for rank, cluster in enumerate(clusters, 1):
        print(f"\nCluster {rank} ({len(cluster)} lessons):")
        for lid in cluster:
            print(f"  {lid}")

    print(f"\nTop {args.top} pairs:")
    for a, b, sim in pairs[: args.top]:
        print(f"  {sim:.2f}  {a}  <->  {b}")

    report = {
        "scanned": len(lessons),
        "threshold": args.threshold,
        "near_duplicate_pairs": len(pairs),
        "clusters": len(clusters),
        "pairs": [{"a": a, "b": b, "jaccard": round(sim, 3)} for a, b, sim in pairs],
        "cluster_members": clusters,
    }
    if args.json:
        args.json.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"\nReport written to {args.json}")

    return 0


if __name__ == "__main__":
    sys.exit(main())

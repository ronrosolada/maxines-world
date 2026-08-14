# Content Engine

Deterministic tooling for Maxine's World Grade 3 content: lesson authoring,
SVG board generation, offline Piper TTS narration, validation, packaging, and
educator-review fixes.

## Voice models (not committed)

The Piper voice model is intentionally NOT committed (121 MB binary).
Download before generating narration:

```bash
mkdir -p voice_models
curl -L -o voice_models/en_US-amy-medium.onnx \
  https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/medium/en_US-amy-medium.onnx
curl -L -o voice_models/en_US-amy-medium.onnx.json \
  https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/amy/medium/en_US-amy-medium.onnx.json
```

## Scripts

| Script | Purpose |
|---|---|
| `svg_generator.py` | Subject-themed 640x360 SVG activity boards with the master Milo launcher icon anchor. |
| `make_avatar.py` | Extracts `milo_icon_anchor.png` from the master 1024x1024 launcher artwork. |
| `lesson_author.py` | Pedagogical lesson structure authoring (pilot modules). |
| `audio_synthesizer.py` | Piper TTS + Opus compression (offline narration). |
| `packager_validator.py` | SHA-256 ZIP packaging + `catalog.json` writer. |
| `cli.py` | Unified CLI (`python3 -m tools.content_engine.cli --demo`). |
| `batch_upgrade_all_curriculum.py` | Batch upgrade of all 358 bundled lessons. |
| `educator_audit_and_reauthor.py` | Educator alignment pass. |
| `educator_gate_fixes.py` | Authored fixes for the `educational_material_audit.py` finding categories (matching pairs, generic instructions, bleed, long text, duplicate prompts). |
| `educator_fix_remaining.py` | Pass 2: long-text shortening + duplicate-prompt prefix variants. |
| `educator_fix_final.py` / `educator_fix_prompts4.py` | Pass 3/4: final prompt variants + screen bleed. |
| `fix_mcq_position_bias.py` | Deterministic answer-position re-balancing on the real `correctOptionIds` schema. |
| `educator_approve.py` | Stamps `educatorValidated=true` + `contentReview` provenance after the review pass. |
| `run_quality_gates.py` | Title uniqueness, banned phrases, MCQ balance, similarity. |
| `repackage_v120.py` / `clean_catalog.py` | Package/catalog maintenance for the DreamNAS content server. |

## Repo gates (authoritative)

Run from `android/`:

```bash
python3 tools/content_quality_audit.py --check
python3 tools/content_pack_validation.py
python3 tools/educational_material_audit.py
python3 tools/content_similarity_gate.py
python3 tools/dedupe_lesson_titles.py --check
python3 tools/verify_lesson_assets.py
```

# Content Review Gap Analysis, 2026-08-15

## Finding

The previous `0.35.0` educator-effectiveness conclusion overstated the evidence. The release checks proved that lesson JSON, activity payloads, asset references, SVG XML, and rasterization were valid; they did **not** prove that an illustration depicted the lesson it was attached to.

The clearest example was `english-g3-q1-w01-d01`: its lesson asked children to find eight fiesta details, while its SVG rendered a generic Picture/Detective/Look focus board. The file was valid, accessible, and renderable, so the existing gates accepted it.

## Evidence

- `.github/workflows/ci.yml` ran the narrower `content_quality_audit.py`, similarity, duplicate, pacing, and asset checks. It did not run `educational_material_audit.py`.
- `content_quality_audit.py` checks renderer payloads and uses loose objective keyword overlap. It does not inspect SVG scene semantics.
- `verify_lesson_assets.py --render --check` confirmed that SVGs could rasterize. Its `hotspot_content_without_visualScene` count was informational, not a failure condition.
- The retired generic visual shell was present in ten English assets, including the Fiesta Picture, while all XML/accessibility/render checks remained green.
- A direct run of the stronger educational audit on the pre-follow-up tree reported 56 duplicate assessment-prompt groups, 5 Filipino language-bleed cases, 3 duplicate matching-label cases, and 180 overlong learner-facing strings. The targeted follow-up fixes reduced the first three categories to zero; the length findings remain a separate copy-edit backlog and are not silently reported as clean.
- One manual spot check found a Makabansa answer-key mismatch that the structural gates could not detect: the explanation supported option `b`, while `correctOptionIds` selected `d`.

## Corrective controls added in 0.35.1

- Every bundled SVG is checked for the retired generic focus-board markers.
- The Fiesta Picture test requires eight distinct scene groups and accessible metadata.
- Rendered asset verification remains enabled for all 358 SVGs.
- Matching right-side labels must be unique within each activity.
- Filipino lesson content is checked for the previously missed English password distractor.
- Fresh-install parent initialization is covered by auth tests and emulator smoke testing.
- The content-validation skill now requires inspecting visible SVG structure or rendered output; description text alone is not evidence of an illustrated clue.

## Remaining limitation

Automated checks still cannot replace a qualified educator reviewing every learner-facing sentence and answer explanation. The 56 repeated prompt groups and 180 length findings also show that the stronger audit needs a deliberate remediation pass before it can be promoted to a blocking CI gate. The release must report that limitation rather than manufacture a zero.

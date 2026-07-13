# DeepSeek Asset Integration Status — Bamboo v1.3

## Required action

Use `source-docs/design.md` as the controlling specification. Section 26 incorporates the compilation report and supersedes all v1.2 asset instructions.

## Current status

| Category | Status | Action |
|---|---|---|
| Sawali fill | PRODUCTION_READY | Use inside native Compose `BambooSurface` |
| Bamboo rails | PRODUCTION_READY | Use as fixed-thickness horizontal/vertical borders |
| Four rattan corners | PRODUCTION_READY | Overlay at fixed size above rails |
| Bamboo NinePatch files | REMOVED | Do not restore or regenerate for this iteration |
| Endemic placement JSON | PRODUCTION_READY | Use only after crop-coordinate transformation |
| Animal reference PNGs | REFERENCE_ONLY | Never copy into Android resources |
| Clean-alpha animal PNGs | PENDING_EXPORT | Hide entire animal layer until all six pass validation |
| Approved Maxine icon | PENDING_EXPORT | Export adaptive foreground, background, monochrome, and 48px proof |

## Icon identity lock

The approved icon shows Maxine with pink glasses, a pink backpack, and a pink necktie. These details are required. Older generic Milo launcher files in the original archive are superseded.

## Build gate

Run `processDebugResources`, `compileDebugKotlin`, and `assembleDebug`. Report exact commands and results. A successful Kotlin compilation alone does not prove that Android resources are valid.

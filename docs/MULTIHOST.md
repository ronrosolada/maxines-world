# Maxine’s World — Multi-Host Hermes Workflow

How to work on this project with **multiple Hermes agents on different machines** without stepping on each other.

## Principles

1. **GitHub is the source of truth for code.** Never leave meaningful work only on one host’s dirty tree.
2. **DreamNAS is the source of truth for large artifacts** (handoff zips, APKs, visual evidence).
3. **Tools run on the agent host.** A remote Desktop/UI does not move the Android SDK or Unraid shell to another machine.
4. **One writer per branch at a time** unless you use PR-based review (Agent A opens PR → Agent B reviews).
5. **Human merges, tags, and publishes releases** unless explicitly told otherwise.

## Host roles

| Host | Typical Hermes role | Prefer for | Avoid assuming |
|------|---------------------|------------|----------------|
| **Linux** (AIWorkstationV2 / ThinkPad) | content, NAS, review, PRs, implementation | DreamNAS, content server `10.10.10.33`, static analysis, Kotlin edits, unit tests | Windows-only paths (`C:\maxines-world`) |
| **Windows** | Android builder / device tester | `gradlew`, APK, adb, Xiaomi Pad 6S Pro | long content packaging on Unraid (prefer Linux) |

Both hosts may edit Kotlin. Final **device-proof** builds and tablet captures usually go through Windows when that is the tablet workflow host. Linux can also `assembleDebug` if its Android SDK is configured.

## Shared paths (DreamNAS)

SMB share: `GeneralNAS` → `Documents` → `maxines-world`

| Path on NAS | Purpose |
|-------------|---------|
| `/mnt/user/GeneralNAS/Documents/maxines-world/handoffs/` | Design / DeepSeek packets + `SHA256SUMS*` |
| `/mnt/user/GeneralNAS/Documents/maxines-world/evidence/` | Screenshots Captures A–F, emulator dumps |
| `/mnt/user/GeneralNAS/Documents/maxines-world/apk/` | Sideload APKs (`app-debug.apk`, named by version+SHA) |
| `/mnt/user/GeneralNAS/Documents/maxines-world/status/CURRENT.md` | **One-pager handoff between agents** |

Windows SMB example:

```text
\\10.10.10.5\GeneralNAS\Documents\maxines-world\
```

Linux:

```bash
ssh root@10.10.10.5 'ls -la /mnt/user/GeneralNAS/Documents/maxines-world/'
# or scp / rsync into the folders above
```

### Approved homepage handoff packet (v2.0.0)

- File: `handoffs/maxines_world_homepage_option23_deepseek_handoff_v2.0.0.zip`
- SHA-256: `5e2d4ff6aa5b56d0d4524884a12f776f8703a96fc62b5041b293592cb766307b`
- Always verify before implementing:

```bash
sha256sum maxines_world_homepage_option23_deepseek_handoff_v2.0.0.zip
# must equal 5e2d4ff6aa5b56d0d4524884a12f776f8703a96fc62b5041b293592cb766307b
```

Telegram bots often fail on ~20MB+ documents — **prefer DreamNAS** for zips.

## Required status file

Before switching agents or hosts, update:

`status/CURRENT.md` on DreamNAS (template below). Optionally mirror a short summary in Telegram.

```markdown
# CURRENT

BRANCH:
OWNER: linux | windows
STATUS: implementing | blocked | ready for review | done
HEAD SHA:
BASE / PR:
HANDOFF FILE:
HANDOFF SHA-256:
NEXT:
- 
- 
DON'T:
- merge main
- tag/release without human approval
- refactor Theme.kt mutable package vars (Xiaomi startup crash history)
NOTES:
```

## Daily loop

### Agent A (e.g. Linux) implements

1. Read `status/CURRENT.md` and this file.
2. `git fetch && git checkout <BRANCH> && git pull --ff-only`
3. Verify handoff SHA if using a design packet.
4. Implement; run local checks (`./gradlew test` / `assembleDebug` as available).
5. Commit → push.
6. Update `status/CURRENT.md` (HEAD SHA, NEXT, OWNER for the next host).
7. Drop evidence/APKs to NAS folders if produced.

### Agent B (e.g. Windows) builds / tests

1. Read `status/CURRENT.md`.
2. Pull the same branch.
3. Build, install on tablet/emulator, capture evidence → `evidence/`.
4. Push any code fixes; update CURRENT.md; hand OWNER back if needed.

### Human

- Review PR.
- Merge / tag / release only with explicit approval.
- `gh release` must use `--target <fullSHA>` when creating releases.

## Git conventions

- Feature/fix branches: `fix/…`, `feat/…` (example: `fix/playground-integration-integrity`)
- Do **not** force-push shared branches unless the human requests it.
- Do **not** commit: APKs, emulator dumps, huge zips, `.hermes/desktop-attachments/*`, local SDK paths secrets.
- Room schema JSON under `core-database/schemas/` **must** be committed when DB version changes.
- Project path must **not** contain spaces (KSP/Room failures).  
  - Windows: `C:\maxines-world`  
  - Linux: `~/projects/maxines-world`

## Build cheatsheet

### Linux

```bash
export JAVA_HOME=~/.sdkman/candidates/java/current
export ANDROID_HOME=~/android-sdk
cd ~/projects/maxines-world/android
./gradlew clean :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Windows

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd /c/maxines-world/android
./gradlew assembleDebug
```

## Content server (LAN-only)

| Item | Value |
|------|--------|
| Catalog | `http://10.10.10.33/catalog.json` |
| Container host | DreamNAS `10.10.10.5` |
| Content root | `/mnt/user/appdata/maxines-world-content/server/content/` |
| Security | LAN-only, no learner PII, child-safe curriculum |

Details: skill `maxines-world-homelab`.

## Hermes setup per host (once)

1. `gh auth login` as `ronrosolada` (private repo access).
2. Clone to the path above (no spaces).
3. SSH key to DreamNAS: `root@10.10.10.5` (`~/.ssh/id_ed25519` on Linux).
4. Install shared skills (from NAS or repo):
   - `maxines-world-homelab`
   - any project-specific maxines skills under GeneralNAS `hermes-skills/`
5. Confirm Android SDK only on hosts that must build/install.

### What is **not** shared across hosts

- Hermes session DB / chat history
- Local `~/.hermes` memory (unless you deliberately sync)
- Running Docker / adb device connections

Pass context via **CURRENT.md + git + NAS**, not by assuming the other agent “remembers.”

## Anti-patterns

- Two agents editing the same uncommitted working tree
- Shipping large design zips via Telegram instead of DreamNAS
- Claiming connected/device tests passed with no emulator/device
- Merging or tagging without human approval
- Theme.kt parameterized refactor without Xiaomi Pad verification
- Divergent “handoff zip” copies without SHA verification

## Quick paste for the next agent

```text
Continue Maxine’s World per docs/MULTIHOST.md and DreamNAS status/CURRENT.md:
  \\10.10.10.5\GeneralNAS\Documents\maxines-world\status\CURRENT.md

Repo: https://github.com/ronrosolada/maxines-world
Pull the BRANCH in CURRENT.md, verify HANDOFF SHA-256 if set, do NEXT only.
Do not merge, tag, or release.
```

---
name: maxines-world-homelab
description: Quick reference for Maxine's World content server and DreamNAS connectivity. Load when working on the Android app's content sync, testing package downloads, or deploying server updates.
---

# Maxine's World — Homelab Quick Reference

## Content Server (DreamNAS)

| Property | Value |
|---|---|
| **Catalog URL** | `http://10.10.10.33/catalog.json` |
| **Package URL** | `http://10.10.10.33/packages/maxines-world-g3-month-01-v1.zip` |
| **Package SHA-256** | `0df8d9e5173b3e268c84049a64fcd81c5aa0c0db6643066d4bffbbc241b916e4` |
| **Package size** | `349039` bytes |
| **`educatorValidated`** | `false` — `REQUIRES_EDUCATOR_REVIEW` |

The server is a read-only Caddy container on DreamNAS (Unraid, `10.10.10.5`), port `80` mapped to static ipvlan IP `10.10.10.33`. No auth, no write endpoints, no learner data. Content files live at `/mnt/user/appdata/maxines-world-content/server/content/` on the NAS.

## Network Layout

| Host | IP | Purpose |
|---|---|---|
| DreamNAS (Unraid) | `10.10.10.5` | NAS, Docker, storage |
| AIWorkstationV2 | `10.10.10.24` (WiFi) / `10.10.10.19` (wired) | Hermes host, Android builds |
| Content server | `10.10.10.33` | Maxine's World month-01 content |
| Gateway | `10.10.10.1` | UniFi, DHCP, DNS |
| DHCP pool | `10.10.10.50+` | Avoid for static containers |

## DreamNAS SSH

```bash
ssh root@10.10.10.5  # key auth (~/.ssh/id_ed25519)
```

Docker containers on `br0` ipvlan, static IPs below `.50` DHCP floor. Maxine's content container: `docker ps --filter name=maxines-world-content`.

## Test Commands

```bash
# Verify catalog
curl http://10.10.10.33/catalog.json

# Download package
curl -sI http://10.10.10.33/packages/maxines-world-g3-month-01-v1.zip

# Read a lesson
curl http://10.10.10.33/grade-3/month-01/v1/lessons/english-g3-m01-d01.json

# Stream an SVG
curl -sI http://10.10.10.33/grade-3/month-01/v1/assets/vectors/english-g3-m01-d01-visual.svg
```

## Updating Content

1. Place new files in `/mnt/user/appdata/maxines-world-content/server/content/` on DreamNAS
2. Caddy serves them instantly (static file server, no restart needed)
3. Update `catalog.json` if package version changes
4. Never overwrite an existing immutable package ZIP — publish a new version

## App Config

Parent-only settings screen for catalog URL. Default: `http://10.10.10.33/catalog.json`. Production should use HTTPS (Tailscale Funnel or reverse proxy). HTTP acceptable for LAN private testing.

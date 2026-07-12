#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/server"
docker compose up -d
echo "Content available locally at http://NAS_IP:8088/catalog.json"
echo "For remote private access, use Tailscale/WireGuard. For public access, terminate HTTPS with a valid certificate and expose only this container."

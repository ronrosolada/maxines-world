#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# bump_and_release.sh — SSOT Release Workflow for RonBot & RonBoto
#
# Ensures versioning strictly follows GitHub releases and Git tags.
# ─────────────────────────────────────────────────────────────────────────────

echo "🔍 1. Fetching latest tags and commits from GitHub..."
git fetch --tags origin
git pull origin main

LATEST_RELEASE=$(gh release list --limit 1 | awk '{print $1}')
echo "📌 Latest GitHub Release: ${LATEST_RELEASE:-"None"}"

CURRENT_GIT_TAG=$(git describe --tags --always)
echo "🏷️  Current Git Tag: ${CURRENT_GIT_TAG}"

TOTAL_COMMITS=$(git rev-list --count HEAD)
echo "🔢 Monotonic Git Commit Count (versionCode): ${TOTAL_COMMITS}"

if [ $# -eq 0 ]; then
    echo ""
    echo "Usage: ./tools/bump_and_release.sh <new_version_tag> [release_title]"
    echo "Example: ./tools/bump_and_release.sh v0.52.0 \"Maxine's World v0.52.0 — Feature update\""
    exit 1
fi

NEW_TAG="$1"
TITLE="${2:-"Maxine's World ${NEW_TAG}"}"

echo ""
echo "🚀 Building Release APK for ${NEW_TAG}..."
export JAVA_HOME="/home/ron/.sdkman/candidates/java/17.0.16-tem"
export ANDROID_HOME="/home/ron/android-sdk"
cd android
./gradlew assembleRelease assembleDebug --no-daemon

RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$RELEASE_APK" ]; then
    echo "❌ Error: $RELEASE_APK not found!"
    exit 1
fi

echo "📦 Creating Git tag and GitHub Release ${NEW_TAG}..."
cd ..
git tag -a "${NEW_TAG}" -m "Release ${NEW_TAG}"
git push origin "${NEW_TAG}"

gh release create "${NEW_TAG}" \
    "android/${RELEASE_APK}" \
    "android/${DEBUG_APK}" \
    --title "${TITLE}" \
    --generate-notes

echo "🌐 Deploying OTA APK to DreamNAS server (10.10.10.33)..."
scp "android/${RELEASE_APK}" root@10.10.10.5:/mnt/user/appdata/maxines-world-content/server/content/app-release.apk
ssh root@10.10.10.5 "cp /mnt/user/appdata/maxines-world-content/server/content/app-release.apk /mnt/user/appdata/maxines-world-content/server/content/media/app-release.apk"

echo "✅ Verifying OTA server response..."
curl -I http://10.10.10.33/app-release.apk

echo "🎉 Release ${NEW_TAG} complete and deployed!"

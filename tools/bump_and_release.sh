#!/usr/bin/env bash
set -euo pipefail

# SSOT release workflow. Never tag or publish until local and GitHub gates pass.

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 <new_version_tag> [release_title]" >&2
    exit 2
fi

NEW_TAG="$1"
if [[ $# -eq 2 ]]; then
    TITLE="$2"
else
    TITLE="Maxine's World ${NEW_TAG}"
fi
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="${ROOT_DIR}/android"
RELEASE_APK="${ANDROID_DIR}/app/build/outputs/apk/release/app-release.apk"
DEBUG_APK="${ANDROID_DIR}/app/build/outputs/apk/debug/app-debug.apk"
REMOTE_ROOT="/mnt/user/appdata/maxines-world-content/server/content"
REMOTE_RELEASE="${REMOTE_ROOT}/app-release.apk"
REMOTE_MEDIA="${REMOTE_ROOT}/media/app-release.apk"
OTA_URLS=("http://10.10.10.33/app-release.apk" "http://10.10.10.33/media/app-release.apk")

command -v git >/dev/null || { echo "git is required" >&2; exit 1; }
command -v gh >/dev/null || { echo "gh is required to wait for the GitHub release gate" >&2; exit 1; }
command -v sha256sum >/dev/null || { echo "sha256sum is required" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }

[[ "${NEW_TAG}" == v* ]] || { echo "release tags must start with v: ${NEW_TAG}" >&2; exit 2; }
git -C "${ROOT_DIR}" check-ref-format "refs/tags/${NEW_TAG}"
git -C "${ROOT_DIR}" fetch --tags --force origin

if [[ -n "$(git -C "${ROOT_DIR}" status --porcelain)" ]]; then
    echo "working tree must be clean before release" >&2
    exit 1
fi
if git -C "${ROOT_DIR}" rev-parse -q --verify "refs/tags/${NEW_TAG}" >/dev/null; then
    echo "tag already exists locally: ${NEW_TAG}" >&2
    exit 1
fi

export JAVA_HOME="${JAVA_HOME:-/home/ron/.sdkman/candidates/java/17.0.16-tem}"
export ANDROID_HOME="${ANDROID_HOME:-/home/ron/android-sdk}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools:${PATH}"

cd "${ROOT_DIR}"
echo "== Video content gates =="
PYTHONPATH=android:android/tools python3 -m unittest android.tools.test_video_assessment_quality

echo "== Android gates =="
cd "${ANDROID_DIR}"
./gradlew testDebugUnitTest lintDebug assembleRelease assembleDebug --no-daemon
cd "${ROOT_DIR}"

[[ -s "${RELEASE_APK}" ]] || { echo "release APK missing: ${RELEASE_APK}" >&2; exit 1; }
[[ -s "${DEBUG_APK}" ]] || { echo "debug APK missing: ${DEBUG_APK}" >&2; exit 1; }

LOCAL_SHA256="$(sha256sum "${RELEASE_APK}" | cut -d' ' -f1)"
echo "release APK SHA-256: ${LOCAL_SHA256}"

echo "== Tag and push candidate =="
git tag -a "${NEW_TAG}" -m "Release ${NEW_TAG}"
git push origin "${NEW_TAG}"

echo "== Wait for GitHub release gate =="
export NEW_TAG
GATE_RUN=""
for _ in $(seq 1 60); do
    GATE_RUN="$(gh run list --workflow release-gate.yml --limit 20 --json databaseId,headBranch,event | python3 -c 'import json, os, sys; rows = json.load(sys.stdin); tag = os.environ["NEW_TAG"]; matches = [r for r in rows if r.get("headBranch") == tag and r.get("event") == "push"]; print(matches[0]["databaseId"] if matches else "")')"
    [[ -n "${GATE_RUN}" ]] && break
    sleep 5
done
[[ -n "${GATE_RUN}" ]] || { echo "no release-gate run found for ${NEW_TAG}" >&2; exit 1; }
gh run watch "${GATE_RUN}" --exit-status

echo "== Create GitHub release =="
gh release create "${NEW_TAG}" "${RELEASE_APK}" "${DEBUG_APK}" --title "${TITLE}" --generate-notes

echo "== Deploy signed APK to DreamNAS =="
scp "${RELEASE_APK}" "root@10.10.10.5:${REMOTE_RELEASE}"
ssh root@10.10.10.5 "cp '${REMOTE_RELEASE}' '${REMOTE_MEDIA}'"
REMOTE_RELEASE_SHA256="$(ssh root@10.10.10.5 sha256sum ${REMOTE_RELEASE} | cut -d' ' -f1)"
REMOTE_MEDIA_SHA256="$(ssh root@10.10.10.5 sha256sum ${REMOTE_MEDIA} | cut -d' ' -f1)"
[[ "${REMOTE_RELEASE_SHA256}" == "${LOCAL_SHA256}" ]] || { echo "release OTA hash mismatch" >&2; exit 1; }
[[ "${REMOTE_MEDIA_SHA256}" == "${LOCAL_SHA256}" ]] || { echo "media OTA hash mismatch" >&2; exit 1; }

for url in "${OTA_URLS[@]}"; do
    curl -fsSI --max-time 30 "${url}" >/dev/null
    echo "verified HTTP 200: ${url}"
done

echo "Release ${NEW_TAG} complete: ${LOCAL_SHA256}"

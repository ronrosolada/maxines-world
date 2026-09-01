#!/usr/bin/env bash
# Repository bootstrap for the Maxine's World dev environment.
# Runs after the source tree is checked out. Must be idempotent: it only
# points Gradle at the SDK and warms the dependency/build caches.
set -euo pipefail

ANDROID_SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"

cd "$(dirname "$0")/../android"

# local.properties is gitignored; regenerate it so Gradle finds the SDK.
echo "sdk.dir=${ANDROID_SDK_DIR}" > local.properties

chmod +x ./gradlew

# Warm the Gradle distribution + dependency cache and prove the build wiring.
./gradlew --no-daemon assembleDebug

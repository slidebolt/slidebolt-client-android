#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_VERSION_NAME:?set ANDROID_VERSION_NAME}"
: "${ANDROID_VERSION_CODE:?set ANDROID_VERSION_CODE}"
: "${ANDROID_KEYSTORE_FILE:?set ANDROID_KEYSTORE_FILE}"
: "${ANDROID_KEYSTORE_PASSWORD:?set ANDROID_KEYSTORE_PASSWORD}"
: "${ANDROID_KEY_ALIAS:?set ANDROID_KEY_ALIAS}"
: "${ANDROID_KEY_PASSWORD:?set ANDROID_KEY_PASSWORD}"

./gradlew --no-daemon clean check bddTest :app-phone:assembleRelease :app-phone:bundleRelease :app-tv:assembleRelease :app-tv:bundleRelease


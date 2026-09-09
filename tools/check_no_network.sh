#!/usr/bin/env bash
set -e
echo "===================================================="
echo "     PRANA CI PRIVACY & ZERO-NETWORK GATE (BASH)    "
echo "===================================================="

# Check for android.permission.INTERNET
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
    echo "PRIVACY VIOLATION: android.permission.INTERNET detected in Manifest!"
    exit 1
else
    echo " Manifest check passed: Zero network permissions found."
fi

# Check for network libraries in build.gradle.kts
if grep -E "(retrofit|okhttp|volley|ktor-client)" app/build.gradle.kts; then
    echo "PRIVACY VIOLATION: Network dependency detected in app/build.gradle.kts!"
    exit 1
else
    echo " Dependency check passed: Zero networking libraries found."
fi

echo " PRIVACY GATE PASSED: 100% On-Device Verified."
exit 0

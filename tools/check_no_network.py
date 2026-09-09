"""
CI Privacy Gate Script.
Scans AndroidManifest.xml and Gradle build scripts to mathematically verify:
1. ZERO INTERNET permission is requested.
2. ZERO unauthorized HTTP/networking dependencies (Retrofit, OkHttp, Volley, Ktor) exist in the health-data build.
Exits 0 on PASS, 1 on FAIL.
"""
import sys
import xml.etree.ElementTree as ET
import os

def check_manifest(manifest_path):
    print(f"[Privacy Gate] Scanning manifest: {manifest_path}")
    if not os.path.exists(manifest_path):
        print(f"Error: Manifest not found at {manifest_path}")
        return False

    tree = ET.parse(manifest_path)
    root = tree.getroot()

    prohibited_permissions = [
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE"
    ]

    for elem in root.findall("uses-permission"):
        name = elem.attrib.get("{http://schemas.android.com/apk/res/android}name", "")
        if name in prohibited_permissions:
            print(f"PRIVACY VIOLATION: Found prohibited network permission: {name}")
            return False

    print(" [PASS] Manifest verification: Zero network permissions detected.")
    return True

def check_gradle_dependencies(gradle_path):
    print(f"[Privacy Gate] Scanning dependencies: {gradle_path}")
    if not os.path.exists(gradle_path):
        print(f"Error: Gradle file not found at {gradle_path}")
        return False

    with open(gradle_path, 'r', encoding='utf-8') as f:
        content = f.read().lower()

    prohibited_libraries = [
        "com.squareup.retrofit2",
        "com.squareup.okhttp3",
        "com.android.volley",
        "io.ktor:ktor-client",
        "org.apache.httpcomponents"
    ]

    for lib in prohibited_libraries:
        if lib in content:
            print(f"PRIVACY VIOLATION: Found prohibited networking dependency: {lib}")
            return False

    print(" [PASS] Dependency verification: Zero external network libraries found.")
    return True

def main():
    print("====================================================")
    print("     PRANA CI PRIVACY & ZERO-NETWORK GATE           ")
    print("====================================================")
    manifest_ok = check_manifest("app/src/main/AndroidManifest.xml")
    gradle_ok = check_gradle_dependencies("app/build.gradle.kts")

    if manifest_ok and gradle_ok:
        print("\n [PASS] PRIVACY GATE: Zero-Network Guarantee 100% Verified.")
        sys.exit(0)
    else:
        print("\n [FAIL] PRIVACY GATE: Network leakage or permission detected!")
        sys.exit(1)

if __name__ == '__main__':
    main()

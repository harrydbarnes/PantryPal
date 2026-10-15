#!/usr/bin/env bash
set -euo pipefail

stable=com.example.pantrypal
preview=com.example.pantrypal.preview

# Run on the disposable CI emulator before instrumentation. Never clear either package.
./gradlew :app:assembleDebug :app:assemblePreview --stacktrace
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/preview/app-preview.apk

for package in "$stable" "$preview"; do
  adb shell pm path "$package" | grep -q '^package:'
  launch="$(adb shell am start -W -n "$package/com.example.pantrypal.MainActivity")"
  printf '%s\n' "$launch"
  grep -q 'Status: ok' <<< "$launch"
  adb shell pidof "$package" | grep -Eq '[0-9]+'
done

# A matching private filename must have independent contents in each installation.
adb shell run-as "$stable" sh -c '"mkdir -p files; echo stable-only > files/coinstall-check"'
adb shell run-as "$preview" sh -c '"mkdir -p files; echo preview-only > files/coinstall-check"'
test "$(adb shell run-as "$stable" cat files/coinstall-check | tr -d '\r')" = stable-only
test "$(adb shell run-as "$preview" cat files/coinstall-check | tr -d '\r')" = preview-only
adb shell run-as "$stable" rm files/coinstall-check
adb shell run-as "$preview" rm files/coinstall-check
adb shell am force-stop "$stable"
adb shell am force-stop "$preview"
echo "Both PantryPal packages installed and launched with isolated private storage."

#!/usr/bin/env bash
# Capture exactly one flow on a connected phone. No app data or permissions are changed.
set -euo pipefail
serial="${1:?Usage: profile-android.sh DEVICE_SERIAL OUTPUT_DIRECTORY}"
output="${2:?Choose an output directory}"
package=com.example.pantrypal
mkdir -p "$output"
adb -s "$serial" shell getprop ro.build.fingerprint > "$output/device.txt"
adb -s "$serial" shell dumpsys package "$package" > "$output/package.txt"
adb -s "$serial" shell pidof "$package" >/dev/null
printf 'Open the shopping list, then press Enter to start a 30-second capture.\n'
read -r
adb -s "$serial" shell dumpsys gfxinfo "$package" reset >/dev/null
printf 'Scroll the shopping list and check/uncheck items for 30 seconds.\n'
adb -s "$serial" shell perfetto -o /data/misc/perfetto-traces/pantrypal-shopping.pftrace -t 30s --app "$package" sched freq idle am wm gfx view binder_driver hal dalvik
adb -s "$serial" pull /data/misc/perfetto-traces/pantrypal-shopping.pftrace "$output/shopping.pftrace"
adb -s "$serial" shell dumpsys gfxinfo "$package" > "$output/gfxinfo.txt"
adb -s "$serial" shell dumpsys gfxinfo "$package" framestats > "$output/frames.txt"
adb -s "$serial" shell dumpsys meminfo "$package" > "$output/memory.txt"
printf 'Saved capture. Repeat on the same device/build settings for comparisons.\n'

#!/usr/bin/env bash
set -uo pipefail
result=0
./firebase/node_modules/.bin/firebase emulators:exec --only auth,firestore --project demo-pantrypal --config firebase.json './gradlew :app:connectedDebugAndroidTest --stacktrace' || result=$?
# Preserve visual and performance evidence even if a different test failed.
adb pull /sdcard/Download/pantrypal-profile app/build/review-profile || true
exit "$result"

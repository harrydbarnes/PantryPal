#!/usr/bin/env bash
set -euo pipefail

tools="$(find "$ANDROID_HOME/build-tools" -name apksigner -type f | sort -V | tail -n 1)"
tools="$(dirname "$tools")"
apk=app/build/outputs/apk/preview/app-preview.apk

python3 - "$apk" "$tools" <<'PYTHON'
import json, pathlib, re, subprocess, sys
apk, tools = sys.argv[1], pathlib.Path(sys.argv[2])
badging = subprocess.check_output([str(tools / 'aapt2'), 'dump', 'badging', apk], text=True)
assert "package: name='com.example.pantrypal.preview'" in badging, 'Wrong package identity'
assert "application-label:'PantryPal Preview'" in badging, 'Wrong launcher label'
certificates = subprocess.check_output([str(tools / 'apksigner'), 'verify', '--print-certs', apk], text=True)
sha1 = re.search(r'certificate SHA-1 digest: ([0-9a-fA-F]+)', certificates).group(1).lower()
config = json.loads(pathlib.Path('app/src/preview/google-services.json').read_text())
client = next(c for c in config['client'] if c['client_info']['android_client_info']['package_name'] == 'com.example.pantrypal.preview')
registered = {c['android_info']['certificate_hash'].replace(':', '').lower()
              for c in client.get('oauth_client', []) if c['client_type'] == 1}
assert sha1 in registered, 'APK signer is not registered for preview Google sign-in'
print('Verified PantryPal Preview package, launcher label, APK signature and registered certificate:', sha1)
PYTHON

mkdir -p app/build/distribution/preview
cp "$apk" app/build/distribution/preview/PantryPal-Preview.apk
(cd app/build/distribution/preview && sha256sum PantryPal-Preview.apk > SHA256SUMS)

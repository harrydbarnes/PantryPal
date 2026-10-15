#!/usr/bin/env bash
set -euo pipefail

for name in KEYSTORE_BASE64 PANTRYPAL_RELEASE_KEYSTORE PANTRYPAL_RELEASE_STORE_PASSWORD PANTRYPAL_RELEASE_KEY_ALIAS PANTRYPAL_RELEASE_KEY_PASSWORD; do
  if [[ -z "${!name:-}" ]]; then
    echo "::error::Missing preview signing configuration: $name"
    exit 1
  fi
done
printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$PANTRYPAL_RELEASE_KEYSTORE"
test -s "$PANTRYPAL_RELEASE_KEYSTORE"

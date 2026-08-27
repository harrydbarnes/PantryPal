#!/usr/bin/env bash

set -o pipefail

if [[ "$#" -eq 0 ]]; then
  echo "Usage: gradle-with-bounded-retry.sh <gradle command> [args...]" >&2
  exit 2
fi

log_file="$(mktemp)"
trap 'rm -f "$log_file"' EXIT

for attempt in 1 2; do
  : > "$log_file"

  if "$@" 2>&1 | tee "$log_file"; then
    exit 0
  fi

  if [[ "$attempt" -eq 2 ]] || ! grep -Eqi \
      '(^|[^0-9])(403|429|5[0-9][0-9])([^0-9]|$)|timed out|timeout|connection (reset|refused|closed)|temporary failure|failed to connect|could not (get|head|download)|received status code' \
      "$log_file"; then
    exit 1
  fi

  echo "Transient dependency/network failure detected; retrying Gradle once (attempt 2 of 2)." >&2
  sleep 5
done

exit 1

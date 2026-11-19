#!/usr/bin/env bash
# Fails when Gradle emits deprecation warnings not listed in the allowlist. Usage: deprecation-gate.sh <build-log>
set -uo pipefail
log="${1:?usage: deprecation-gate.sh <build-log>}"
allowlist=".github/gradle-deprecation-allowlist.txt"
warnings=$(grep -i "has been deprecated\|This will fail with an error\|is scheduled to be removed" "$log" | sort -u || true)
if [ -z "$warnings" ]; then
  echo "✅ No deprecation warnings found."
  exit 0
fi
unmatched=""
while IFS= read -r line; do
  matched=false
  while IFS= read -r pattern || [ -n "$pattern" ]; do
    [[ "$pattern" =~ ^#.*$ || -z "$pattern" ]] && continue
    entry="${pattern%%#*}"
    entry="${entry%"${entry##*[![:space:]]}"}"
    if [[ "$line" == *"$entry"* ]]; then
      matched=true
      break
    fi
  done < "$allowlist"
  if [ "$matched" = false ]; then
    unmatched="${unmatched}${line}"$'\n'
  fi
done <<< "$warnings"
if [ -n "$unmatched" ]; then
  echo "❌ New (unallowlisted) deprecation warnings detected:"
  echo "$unmatched"
  exit 1
fi
echo "✅ All deprecation warnings are on the allowlist."

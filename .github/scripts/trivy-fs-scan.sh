#!/usr/bin/env bash
# Runs trivy fs over the shipped-only tree (see trivy-shipped-tree.sh), appends table report to job summary, returns trivy exit code.
# Usage: trivy-fs-scan.sh <title> <trivy args...>
set -uo pipefail
title="${1:?usage: trivy-fs-scan.sh <title> <trivy args...>}"
shift
here=$(dirname "$0")
tree=$(bash "$here/trivy-shipped-tree.sh") || exit 1
report=$(mktemp)
rc=0
trivy fs --format table --output "$report" "$@" "$tree" || rc=$?
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "### $title (exit $rc)"
    echo '```'
    cat "$report"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
fi
cat "$report"
exit "$rc"

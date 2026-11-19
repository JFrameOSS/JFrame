#!/usr/bin/env bash
# Mirrors the repo into a temp tree for shipped-only trivy scans; prints the tree path.
# Gradle lockfiles keep only lines whose configurations include a runtime classpath (tooling-only deps dropped).
set -euo pipefail
root=$(git rev-parse --show-toplevel)
tree=$(mktemp -d)
rsync -a \
  --exclude node_modules --exclude build --exclude .git --exclude .gradle \
  --exclude .codegraph --exclude .opencode --exclude '*buildscript-gradle.lockfile' \
  --exclude '*settings-gradle.lockfile' \
  "$root"/ "$tree"/
while IFS= read -r lock; do
  awk -F'=' '/^#/ || NF < 2 { print; next }
    { n = split($2, c, ",");
      for (i = 1; i <= n; i++)
        if (c[i] ~ /^(runtimeClasspath|productionRuntimeClasspath|quarkusProd[A-Za-z]*RuntimeClasspath[A-Za-z]*)$/) { print; next } }' \
    "$lock" > "$lock.tmp" && mv "$lock.tmp" "$lock"
done < <(find "$tree" -name gradle.lockfile -not -path '*/build/*')
echo "$tree"

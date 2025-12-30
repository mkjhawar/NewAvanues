#!/bin/bash
# Removes git lock files in MainAvanues repo
# Usage: source this file or run directly

REPO_ROOT="/Volumes/M-Drive/Coding/MainAvanues"
LOCK_FILES=(
    "$REPO_ROOT/.git/index.lock"
    "$REPO_ROOT/.git/HEAD.lock"
    "$REPO_ROOT/.git/config.lock"
    "$REPO_ROOT/.git/refs/heads/*.lock"
)

removed=0
for pattern in "${LOCK_FILES[@]}"; do
    for lock in $pattern; do
        if [ -f "$lock" ]; then
            rm -f "$lock"
            echo "Removed: $lock"
            ((removed++))
        fi
    done
done

if [ $removed -eq 0 ]; then
    echo "No lock files found"
else
    echo "Removed $removed lock file(s)"
fi

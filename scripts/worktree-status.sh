#!/bin/bash
# worktree-status.sh - Show health status of all worktrees
# Usage: ./scripts/worktree-status.sh [--no-fetch]

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

MAIN_BRANCHES=("main" "Avanues-Main" "master")

echo ""
echo "======================================"
echo " WORKTREE STATUS"
echo "======================================"
echo ""

# Fetch (with timeout, skip with --no-fetch)
if [[ "$1" != "--no-fetch" ]]; then
    echo "Fetching from remote... (use --no-fetch to skip)"
    timeout 10 git fetch --prune origin 2>/dev/null || echo "(fetch skipped/timed out)"
    echo ""
fi

# Simple worktree list with status
git worktree list --porcelain > /tmp/worktree-list.txt

TOTAL=0
ACTIVE=0
STALE=0

while IFS= read -r line; do
    if [[ "$line" == worktree* ]]; then
        WORKTREE_PATH="${line#worktree }"
    elif [[ "$line" == branch* ]]; then
        BRANCH="${line#branch refs/heads/}"
        TOTAL=$((TOTAL + 1))

        # Check status
        IS_STALE=""
        for main in "${MAIN_BRANCHES[@]}"; do
            if git branch --merged "$main" 2>/dev/null | grep -q "^[[:space:]]*$BRANCH$"; then
                IS_STALE="merged to $main"
                STALE=$((STALE + 1))
                break
            fi
        done

        if [ -z "$IS_STALE" ]; then
            if ! git show-ref --verify --quiet "refs/remotes/origin/$BRANCH" 2>/dev/null; then
                IS_STALE="deleted on remote"
                STALE=$((STALE + 1))
            else
                ACTIVE=$((ACTIVE + 1))
            fi
        fi

        # Print
        if [ -n "$IS_STALE" ]; then
            echo "[STALE] $BRANCH ($IS_STALE)"
        else
            echo "[OK]    $BRANCH"
        fi
        echo "        $WORKTREE_PATH"
        echo ""
    fi
done < /tmp/worktree-list.txt

echo "======================================"
echo " Total: $TOTAL | Active: $ACTIVE | Stale: $STALE"
echo "======================================"
echo ""

if [ "$STALE" -gt 0 ]; then
    echo "Run: ./scripts/worktree-cleanup.sh"
fi

rm -f /tmp/worktree-list.txt

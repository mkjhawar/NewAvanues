#!/bin/bash

# Remove old/stale worktrees
# Usage: worktree-cleanup.sh [--force] [--age HOURS]

WORKTREES_DIR="/Volumes/M-Drive/Worktrees"
FORCE=false
MAX_AGE_HOURS=24

# Parse args
while [[ $# -gt 0 ]]; do
    case $1 in
        --force|-f) FORCE=true; shift ;;
        --age) MAX_AGE_HOURS=$2; shift 2 ;;
        *) shift ;;
    esac
done

MAX_AGE_SEC=$((MAX_AGE_HOURS * 3600))
now=$(date +%s)

echo "Scanning worktrees older than ${MAX_AGE_HOURS}h..."
echo ""

removed=0
skipped=0

for wt in "$WORKTREES_DIR"/*__t*; do
    if [[ -d "$wt" ]]; then
        name=$(basename "$wt")

        # Get age
        if [[ "$(uname)" == "Darwin" ]]; then
            mod_time=$(stat -f %m "$wt" 2>/dev/null)
        else
            mod_time=$(stat -c %Y "$wt" 2>/dev/null)
        fi
        age_sec=$((now - mod_time))

        if [[ $age_sec -gt $MAX_AGE_SEC ]]; then
            # Check if clean
            changes=$(cd "$wt" && git status --short 2>/dev/null | wc -l | tr -d ' ')

            if [[ "$changes" != "0" ]]; then
                echo "SKIP: $name (has $changes uncommitted changes)"
                ((skipped++))
                continue
            fi

            # Check for unpushed commits
            unpushed=$(cd "$wt" && git log @{u}..HEAD --oneline 2>/dev/null | wc -l | tr -d ' ')
            if [[ "$unpushed" != "0" ]]; then
                echo "SKIP: $name (has $unpushed unpushed commits)"
                ((skipped++))
                continue
            fi

            if [[ "$FORCE" == "true" ]]; then
                # Get main repo path
                main_repo=$(cd "$wt" && cat .git | sed 's/gitdir: //' | sed 's/\.git\/worktrees.*//')

                echo "REMOVE: $name"
                (cd "$main_repo" && git worktree remove "$wt" 2>/dev/null) || rm -rf "$wt"
                ((removed++))
            else
                age_h=$((age_sec / 3600))
                echo "STALE: $name (${age_h}h old) - use --force to remove"
            fi
        fi
    fi
done

echo ""
echo "Removed: $removed | Skipped: $skipped"

# Prune orphaned worktree references
echo ""
echo "Pruning orphaned references..."
for repo in /Volumes/M-Drive/Coding/*/; do
    if [[ -d "$repo/.git" ]]; then
        (cd "$repo" && git worktree prune 2>/dev/null)
    fi
done
echo "Done."

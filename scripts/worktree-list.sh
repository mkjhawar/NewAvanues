#!/bin/bash

# List all worktrees across all repos
# Usage: worktree-list.sh [repo]

WORKTREES_DIR="/Volumes/M-Drive/Worktrees"
CODING_DIR="/Volumes/M-Drive/Coding"

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                         ACTIVE WORKTREES                                 ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

if [[ ! -d "$WORKTREES_DIR" ]]; then
    echo "No worktrees directory found at $WORKTREES_DIR"
    exit 0
fi

# Count worktrees
count=$(ls -d "$WORKTREES_DIR"/*__* 2>/dev/null | wc -l | tr -d ' ')

if [[ "$count" == "0" ]]; then
    echo "No active worktrees."
    exit 0
fi

printf "%-30s %-30s %-15s %-10s\n" "WORKTREE" "BRANCH" "STATUS" "AGE"
printf "%-30s %-30s %-15s %-10s\n" "--------" "------" "------" "---"

for wt in "$WORKTREES_DIR"/*__*; do
    if [[ -d "$wt" ]]; then
        name=$(basename "$wt")

        # Get branch
        branch=$(cd "$wt" && git branch --show-current 2>/dev/null)
        if [[ -z "$branch" ]]; then
            branch="(detached)"
        fi

        # Get status
        changes=$(cd "$wt" && git status --short 2>/dev/null | wc -l | tr -d ' ')
        if [[ "$changes" == "0" ]]; then
            status="Clean"
        else
            status="${changes} changes"
        fi

        # Get age (time since last modification)
        if [[ "$(uname)" == "Darwin" ]]; then
            mod_time=$(stat -f %m "$wt" 2>/dev/null)
            now=$(date +%s)
            age_sec=$((now - mod_time))
        else
            age_sec=$(( $(date +%s) - $(stat -c %Y "$wt" 2>/dev/null) ))
        fi

        if [[ $age_sec -lt 3600 ]]; then
            age="$((age_sec / 60))m"
        elif [[ $age_sec -lt 86400 ]]; then
            age="$((age_sec / 3600))h"
        else
            age="$((age_sec / 86400))d"
        fi

        printf "%-30s %-30s %-15s %-10s\n" "$name" "$branch" "$status" "$age"
    fi
done

echo ""
echo "Total: $count worktree(s)"
echo ""
echo "To close a worktree: cd <worktree> && /i.close .force"
echo "To prune orphaned:   git worktree prune"

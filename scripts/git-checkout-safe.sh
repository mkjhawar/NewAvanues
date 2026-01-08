#!/bin/bash
# git-checkout-safe.sh - Safe checkout wrapper that warns about worktree usage
# Usage: ./scripts/git-checkout-safe.sh <branch>
#
# Use this instead of 'git checkout' to get worktree warnings.

set -e

TARGET="$1"

if [ -z "$TARGET" ]; then
    echo "Usage: $0 <branch>"
    exit 1
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
CURRENT_BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo "detached")

# Check if this is a worktree
if [ -f "$REPO_ROOT/.git" ]; then
    echo ""
    echo "ERROR: Branch switching in a worktree is not allowed!"
    echo ""
    echo "  Current: $CURRENT_BRANCH"
    echo "  Target:  $TARGET"
    echo ""
    echo "Create a new worktree instead:"
    echo "  cd $(git rev-parse --git-common-dir | xargs dirname)"
    echo "  ./scripts/worktree-add.sh $TARGET"
    echo ""
    exit 1
fi

# Check if target branch is already in another worktree
if git worktree list 2>/dev/null | grep -q "\[$TARGET\]"; then
    echo ""
    echo "ERROR: Branch '$TARGET' is already checked out in another worktree!"
    echo ""
    git worktree list | grep "\[$TARGET\]"
    echo ""
    echo "Use that worktree instead."
    exit 1
fi

# Warn about worktree workflow
WORKTREE_COUNT=$(git worktree list 2>/dev/null | wc -l | tr -d ' ')
if [ "$WORKTREE_COUNT" -gt 1 ]; then
    echo ""
    echo "WARNING: This repo uses worktrees for parallel development."
    echo ""
    echo "Consider creating a worktree instead:"
    echo "  ./scripts/worktree-add.sh $TARGET"
    echo ""
    read -p "Proceed with checkout anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted. Use worktree-add.sh instead."
        exit 0
    fi
fi

# Proceed with checkout
git checkout "$TARGET"

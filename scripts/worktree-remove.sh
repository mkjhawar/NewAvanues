#!/bin/bash
# worktree-remove.sh - Safely remove a git worktree
# Usage: ./scripts/worktree-remove.sh <path-or-branch>
#
# Examples:
#   ./scripts/worktree-remove.sh ../NewAvanues__feature__new-ui
#   ./scripts/worktree-remove.sh feature/new-ui

set -e

TARGET="${1}"

if [ -z "$TARGET" ]; then
    echo "Usage: $0 <path-or-branch>"
    echo ""
    echo "Examples:"
    echo "  $0 ../NewAvanues__feature__new-ui"
    echo "  $0 feature/new-ui"
    echo ""
    echo "Current worktrees:"
    git worktree list
    exit 1
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
REPO_NAME="$(basename "$REPO_ROOT")"
PARENT_DIR="$(dirname "$REPO_ROOT")"

# If target looks like a branch name (contains /), convert to path
if [[ "$TARGET" == *"/"* ]] && [[ ! -d "$TARGET" ]]; then
    FOLDER_NAME="${REPO_NAME}__${TARGET//\//__}"
    TARGET="${PARENT_DIR}/${FOLDER_NAME}"
fi

# Resolve to absolute path
if [[ "$TARGET" != /* ]]; then
    TARGET="$(cd "$REPO_ROOT" && cd "$(dirname "$TARGET")" && pwd)/$(basename "$TARGET")"
fi

# Check if worktree exists
if ! git worktree list | grep -q "$TARGET"; then
    echo "Error: No worktree found at: $TARGET"
    echo ""
    echo "Available worktrees:"
    git worktree list
    exit 1
fi

# Check for uncommitted changes
if [ -d "$TARGET" ]; then
    cd "$TARGET"
    if ! git diff --quiet HEAD 2>/dev/null; then
        echo "Warning: Worktree has uncommitted changes!"
        echo ""
        git status --short
        echo ""
        read -p "Remove anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted."
            exit 1
        fi
    fi
    cd "$REPO_ROOT"
fi

echo "Removing worktree: $TARGET"
git worktree remove "$TARGET" --force

echo "Pruning stale worktree metadata..."
git worktree prune

echo ""
echo "Worktree removed successfully!"
echo ""
echo "Remaining worktrees:"
git worktree list

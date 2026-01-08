#!/bin/bash
# worktree-list.sh - List all git worktrees with their branches
# Usage: ./scripts/worktree-list.sh

set -e

echo "Git Worktrees for $(basename "$(git rev-parse --show-toplevel)")"
echo "=============================================="
echo ""

git worktree list --porcelain | while read line; do
    case "$line" in
        worktree*)
            path="${line#worktree }"
            ;;
        HEAD*)
            head="${line#HEAD }"
            ;;
        branch*)
            branch="${line#branch refs/heads/}"
            echo "  $branch"
            echo "    Path: $path"
            echo "    HEAD: ${head:0:8}"
            echo ""
            ;;
        detached)
            echo "  (detached HEAD)"
            echo "    Path: $path"
            echo "    HEAD: ${head:0:8}"
            echo ""
            ;;
    esac
done

echo "=============================================="
echo "Total: $(git worktree list | wc -l | tr -d ' ') worktrees"

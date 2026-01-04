#!/bin/bash
# setup-hooks.sh - Configure git hooks and aliases for worktree workflow
# Usage: ./scripts/setup-hooks.sh
#
# Run this once after cloning or creating a new worktree.

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_DIR="$REPO_ROOT/scripts/hooks"

echo "Setting up worktree workflow for NewAvanues..."
echo ""

# 1. Configure hooks path
if [ -d "$HOOKS_DIR" ]; then
    git config core.hooksPath scripts/hooks
    chmod +x "$HOOKS_DIR"/* 2>/dev/null || true
    echo "[OK] Hooks enabled (scripts/hooks/)"
    for hook in "$HOOKS_DIR"/*; do
        if [ -f "$hook" ]; then
            echo "     - $(basename "$hook")"
        fi
    done
else
    echo "[SKIP] No hooks directory found"
fi

echo ""

# 2. Add git alias for safe checkout
git config alias.co '!./scripts/git-checkout-safe.sh'
echo "[OK] Git alias: 'git co <branch>' = safe checkout with warnings"

echo ""
echo "Setup complete!"
echo ""
echo "Workflow:"
echo "  - Use 'git co <branch>' instead of 'git checkout'"
echo "  - Or use './scripts/worktree-add.sh <branch>' (recommended)"
echo ""
echo "After merge, you'll see cleanup reminders."
echo ""
echo "To undo setup:"
echo "  git config --unset core.hooksPath"
echo "  git config --unset alias.co"

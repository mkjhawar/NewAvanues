#!/bin/bash
# setup-hooks.sh - Configure git to use shared hooks
# Usage: ./scripts/setup-hooks.sh
#
# Run this once after cloning or creating a new worktree.
# Sets git to use scripts/hooks/ for all git hooks.

set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_DIR="$REPO_ROOT/scripts/hooks"

echo "Setting up git hooks for NewAvanues..."
echo ""

# Check if hooks directory exists
if [ ! -d "$HOOKS_DIR" ]; then
    echo "Error: $HOOKS_DIR not found"
    exit 1
fi

# Configure git to use our hooks directory
git config core.hooksPath scripts/hooks

echo "Configured: git config core.hooksPath = scripts/hooks"
echo ""

# Verify hooks are executable
chmod +x "$HOOKS_DIR"/* 2>/dev/null || true

# List installed hooks
echo "Available hooks:"
for hook in "$HOOKS_DIR"/*; do
    if [ -f "$hook" ]; then
        echo "  - $(basename "$hook")"
    fi
done

echo ""
echo "Setup complete!"
echo ""
echo "Hooks will now run automatically:"
echo "  - pre-checkout: Prevents branch switching (use worktrees instead)"
echo "  - post-merge: Notifies when worktrees can be cleaned up"
echo ""
echo "To disable hooks temporarily:"
echo "  git config --unset core.hooksPath"
echo ""
echo "To re-enable:"
echo "  ./scripts/setup-hooks.sh"

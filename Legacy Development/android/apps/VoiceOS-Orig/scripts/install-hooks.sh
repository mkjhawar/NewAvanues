#!/bin/bash
# Install pre-commit hooks for VoiceOS development
#
# This script installs git hooks that enforce coding standards:
# - Logging standards (no direct Log.* calls)
# - (Future: other checks can be added here)
#
# Usage:
#   ./scripts/install-hooks.sh
#
# Part of Phase 2: Logging Standardization

set -e

REPO_ROOT=$(git rev-parse --show-toplevel)
HOOK_SOURCE="$REPO_ROOT/scripts/pre-commit-logging-check.sh"
HOOK_DEST="$REPO_ROOT/.git/hooks/pre-commit"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 VoiceOS Pre-commit Hook Installer"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Verify hook source exists
if [ ! -f "$HOOK_SOURCE" ]; then
    echo "❌ Error: Hook source not found at:"
    echo "   $HOOK_SOURCE"
    echo ""
    echo "   Make sure you're running this from the repo root."
    exit 1
fi

# Check if hook already exists
if [ -f "$HOOK_DEST" ]; then
    echo "⚠️  Pre-commit hook already exists at:"
    echo "   $HOOK_DEST"
    echo ""
    read -p "   Overwrite? (y/N): " -n 1 -r
    echo ""
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo ""
        echo "❌ Installation cancelled"
        exit 1
    fi
    
    echo ""
fi

# Install hook
echo "📄 Installing pre-commit hook..."
cp "$HOOK_SOURCE" "$HOOK_DEST"
chmod +x "$HOOK_DEST"

echo "✅ Pre-commit hook installed successfully!"
echo ""

# Verify allowlist exists
ALLOWLIST_FILE="$REPO_ROOT/.logging-allowlist"
if [ ! -f "$ALLOWLIST_FILE" ]; then
    echo "⚠️  Warning: .logging-allowlist not found"
    echo "   All files will be checked (no legacy exceptions)"
    echo ""
else
    ALLOWLIST_COUNT=$(grep -v '^#' "$ALLOWLIST_FILE" | grep -v '^[[:space:]]*$' | wc -l | tr -d ' ')
    echo "📋 Allowlist loaded: $ALLOWLIST_COUNT legacy files excluded"
    echo ""
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "ℹ️  What this hook does:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "   ✅ Checks for direct Log.* calls before commit"
echo "   ✅ Allows files in .logging-allowlist (legacy code)"
echo "   ✅ Blocks commits with violations in NEW code"
echo "   ✅ Provides clear fix instructions"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📖 Documentation:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "   Guidelines:  docs/LOGGING-GUIDELINES.md"
echo "   Enforcement: docs/CI-ENFORCEMENT.md"
echo "   Checklist:   docs/CODE-REVIEW-CHECKLIST.md"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 Test it:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "   1. Create a test file with Log.d(TAG, \"test\")"
echo "   2. Try to commit it"
echo "   3. Should be blocked with clear instructions"
echo "   4. Fix it with ConditionalLogger.d(TAG) { \"test\" }"
echo "   5. Commit should succeed"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "⚠️  Emergency bypass:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "   git commit --no-verify -m \"message\""
echo ""
echo "   (Use sparingly! Creates technical debt)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Installation complete!"
echo ""

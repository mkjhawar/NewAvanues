#!/bin/bash
# Pre-commit hook to enforce logging standards
# Prevents direct Log.* calls in favor of ConditionalLogger/PIILoggingWrapper
#
# Installation: ./scripts/install-hooks.sh
# Bypass (emergencies): git commit --no-verify
#
# Part of Phase 2: Logging Standardization
# See: docs/LOGGING-GUIDELINES.md

set -e

echo "🔍 Checking logging standards..."

REPO_ROOT=$(git rev-parse --show-toplevel)
ALLOWLIST_FILE="$REPO_ROOT/.logging-allowlist"

# Get list of staged Kotlin files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.kt$' || true)

if [ -z "$STAGED_FILES" ]; then
    echo "   No Kotlin files to check"
    exit 0
fi

# Load allowlist (files with known violations - to be fixed later)
ALLOWED_FILES=""
if [ -f "$ALLOWLIST_FILE" ]; then
    ALLOWED_FILES=$(grep -v '^#' "$ALLOWLIST_FILE" | grep -v '^[[:space:]]*$' || true)
fi

# Track violations
VIOLATIONS_FOUND=false
VIOLATION_DETAILS=""

# Check each staged file
for FILE in $STAGED_FILES; do
    # Normalize file path (remove leading ./)
    NORMALIZED_FILE=$(echo "$FILE" | sed 's|^\./||')
    
    # Check if file is in allowlist
    IS_ALLOWED=false
    if [ -n "$ALLOWED_FILES" ]; then
        if echo "$ALLOWED_FILES" | grep -q "^$NORMALIZED_FILE$"; then
            IS_ALLOWED=true
            echo "   ⚠️  $FILE (allowlisted - legacy code)"
        fi
    fi
    
    # Skip if allowed
    if [ "$IS_ALLOWED" = true ]; then
        continue
    fi
    
    # Check for Log.* violations in the DIFF (only new/modified lines)
    # Look for lines starting with + (added) that contain Log.[diwev](
    DIFF_VIOLATIONS=$(git diff --cached "$FILE" | grep '^+' | grep -E 'Log\.(d|i|w|e|v|wtf)\(' || true)
    
    if [ -n "$DIFF_VIOLATIONS" ]; then
        VIOLATIONS_FOUND=true
        
        # Get line numbers from the actual file for better error messages
        LINE_NUMBERS=$(git diff --cached -U0 "$FILE" | grep '^@@' | sed 's/@@ -[0-9,]* +\([0-9]*\).*/\1/')
        
        VIOLATION_DETAILS="$VIOLATION_DETAILS\n\n📄 $FILE:"
        
        # Show each violation with context
        echo "$DIFF_VIOLATIONS" | while IFS= read -r line; do
            # Remove leading +
            CLEAN_LINE=$(echo "$line" | sed 's/^+//')
            VIOLATION_DETAILS="$VIOLATION_DETAILS\n   $CLEAN_LINE"
        done
        
        echo "   ❌ $FILE (violations detected)"
    else
        echo "   ✅ $FILE (clean)"
    fi
done

# If violations found, block commit
if [ "$VIOLATIONS_FOUND" = true ]; then
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "❌ COMMIT BLOCKED: Direct Log.* calls detected in NEW code"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Found logging violations in files being committed:"
    echo -e "$VIOLATION_DETAILS"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✅ How to fix:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "1. For SYSTEM logs (state, metrics, errors):"
    echo "   ❌ Log.d(TAG, \"message\")"
    echo "   ✅ ConditionalLogger.d(TAG) { \"message\" }"
    echo ""
    echo "   Add import: import com.augmentalis.voiceoscore.utils.ConditionalLogger"
    echo ""
    echo "2. For USER DATA (voice input, UI text, personal info):"
    echo "   ❌ Log.d(TAG, \"User input: \$userInput\")"
    echo "   ✅ PIILoggingWrapper.d(TAG, \"User input: \$userInput\")"
    echo ""
    echo "   Already imported in most files"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📖 Documentation:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "  Guidelines: docs/LOGGING-GUIDELINES.md"
    echo "  Enforcement: docs/CI-ENFORCEMENT.md"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "⚠️  Emergency bypass (use sparingly):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "  git commit --no-verify -m \"your message\""
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    exit 1
fi

echo ""
echo "✅ All logging standards checks passed!"
echo ""
exit 0

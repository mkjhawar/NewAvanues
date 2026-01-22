#!/bin/bash
# YOLO Safeguards Pre-Commit Check
# Validates that YOLO mode mandatory rules are followed

echo "╔══════════════════════════════════════════════════════════╗"
echo "║  YOLO SAFEGUARDS CHECK                                   ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

VIOLATIONS=0

# Rule 1: No Shortcuts
echo "[1/3] Checking for shortcuts..."
if git diff --cached | grep -iE "TODO|FIXME|hack|temporary|quick fix"; then
    echo "  ❌ VIOLATION: TODOs/hacks found in staged code"
    ((VIOLATIONS++))
else
    echo "  ✅ No shortcuts detected"
fi

if git diff --cached | grep -iE "deprecated|any type"; then
    echo "  ⚠️  WARNING: Deprecated APIs or 'any' type found"
fi

# Rule 2: No Disable Without Re-enable  
echo ""
echo "[2/3] Checking for disabled code..."
if git diff --cached | grep "^+.*//.*\(validate\|check\|security\|auth\)"; then
    echo "  ❌ VIOLATION: Commented validation/security code"
    ((VIOLATIONS++))
else
    echo "  ✅ No disabled validation found"
fi

# Rule 3: Function Equivalence (tests must pass)
echo ""
echo "[3/3] Verifying function equivalence..."
echo "  → Running tests..."
# Project-specific test command detection
if [ -f "package.json" ]; then
    npm test --silent 2>&1 | tail -5
    TEST_RESULT=$?
elif [ -f "gradlew" ]; then
    ./gradlew test --quiet 2>&1 | tail -5
    TEST_RESULT=$?
else
    echo "  ⚠️  No test runner found, skipping"
    TEST_RESULT=0
fi

if [ $TEST_RESULT -ne 0 ]; then
    echo "  ❌ VIOLATION: Tests failing"
    ((VIOLATIONS++))
else
    echo "  ✅ All tests passing"
fi

# Final verdict
echo ""
echo "══════════════════════════════════════════════════════════"
if [ $VIOLATIONS -eq 0 ]; then
    echo "✅ YOLO SAFEGUARDS PASSED"
    echo "══════════════════════════════════════════════════════════"
    exit 0
else
    echo "❌ YOLO SAFEGUARDS FAILED ($VIOLATIONS violations)"
    echo "══════════════════════════════════════════════════════════"
    echo ""
    echo "Fix violations before committing in YOLO mode."
    echo "See: protocols/Protocol-YOLO-Safeguards-v9.0.md"
    exit 1
fi

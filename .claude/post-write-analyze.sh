#!/bin/bash
# IDEACODE v16.1 - Post-Write Analysis
# Runs actual checks on written files

FILE="$1"
[ -z "$FILE" ] && exit 0
[ ! -f "$FILE" ] && exit 0

EXT="${FILE##*.}"

# Skip non-code files
case "$EXT" in
  md|txt|json|yml|yaml|xml|html|css|idc|sh)
    exit 0
    ;;
esac

# Run checks
echo "━━━ Post-Write: $(basename "$FILE") ━━━"

# 1. TODO/FIXME check
TODO_COUNT=$(grep -c -E "TODO|FIXME|XXX|HACK" "$FILE" 2>/dev/null | head -1)
TODO_COUNT=${TODO_COUNT:-0}
[ "$TODO_COUNT" -eq 0 ] 2>/dev/null && echo "  ✓ No TODO/FIXME" || echo "  ✗ $TODO_COUNT TODO/FIXME found"

# 2. Hardcoded secrets check
SECRETS=$(grep -c -iE "(password|secret|api_key|apikey).*=.*[\"']" "$FILE" 2>/dev/null | head -1)
SECRETS=${SECRETS:-0}
[ "$SECRETS" -eq 0 ] 2>/dev/null && echo "  ✓ No secrets" || echo "  ✗ $SECRETS potential secrets"

# 3. File size check
LINES=$(wc -l < "$FILE" 2>/dev/null | tr -d ' ')
LINES=${LINES:-0}
[ "$LINES" -lt 500 ] 2>/dev/null && echo "  ✓ Size OK ($LINES lines)" || echo "  ✗ Large file ($LINES lines)"

# 4. Wildcard imports (Kotlin/Java)
if [[ "$EXT" == "kt" || "$EXT" == "java" ]]; then
  WILD=$(grep -c "import.*\.\*" "$FILE" 2>/dev/null | head -1)
  WILD=${WILD:-0}
  [ "$WILD" -eq 0 ] 2>/dev/null && echo "  ✓ No wildcard imports" || echo "  ✗ $WILD wildcard imports"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
exit 0

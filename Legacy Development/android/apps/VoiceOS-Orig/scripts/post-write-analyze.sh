#!/bin/bash
# IDEACODE v9.0 - Mandatory Post-Write Analysis
# Triggers domain expert analysis after code changes

# Get the file that was written
FILE="$1"
EXT="${FILE##*.}"

# Skip non-code files
case "$EXT" in
  md|txt|json|yml|yaml|xml|html|css)
    exit 0
    ;;
esac

# Output analysis requirements
cat << 'EOF'
╔══════════════════════════════════════════════════════════════╗
║  🔍 MANDATORY POST-WRITE ANALYSIS TRIGGERED                 ║
╚══════════════════════════════════════════════════════════════╝

Run these checks before proceeding:

1. COMPLETENESS CHECK
   □ No TODO/FIXME left unaddressed
   □ All imports resolved
   □ No missing class/function implementations
   □ All interface methods implemented

2. TECHNIQUE VALIDATION
   □ Using current best practices (not deprecated APIs)
   □ Following language idioms (Kotlin conventions, etc.)
   □ Proper error handling patterns

3. SECURITY SCAN
   □ No hardcoded secrets
   □ Input validation present
   □ SQL injection prevention (if applicable)

4. QUALITY GATES
   □ Functions < 30 lines
   □ Cyclomatic complexity < 10
   □ No code duplication

5. DEVELOPER MANUAL (MANDATORY)
   □ API documentation for public methods
   □ Architecture docs for new components
   □ README updated with new features
   □ CHANGELOG entry added

6. USER MANUAL (if UI changes)
   □ User guide for new features
   □ Screenshots/mockups captured
   □ Error messages are user-friendly
   □ Help text updated

Run: /review .swarm for full expert analysis
EOF

exit 0

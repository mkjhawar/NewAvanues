#!/bin/bash

# VOS4 Git Hooks Setup Script
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
# Created: 2025-01-28
# 
# Installs mandatory testing hooks for the VOS4 project

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}       VOS4 Git Hooks Installation${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo

# Check if we're in a git repository
if [ ! -d .git ]; then
    echo -e "${RED}Error: Not in a git repository root${NC}"
    echo "Please run this script from the VOS4 project root"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p .git/hooks

# Install pre-commit hook
echo -e "${YELLOW}Installing pre-commit hook...${NC}"
if [ -f .githooks/pre-commit ]; then
    cp .githooks/pre-commit .git/hooks/pre-commit
    chmod +x .git/hooks/pre-commit
    echo -e "${GREEN}✓ Pre-commit hook installed${NC}"
else
    echo -e "${RED}✗ Pre-commit hook not found in .githooks/${NC}"
fi

# Install pre-push hook
echo -e "${YELLOW}Creating pre-push hook...${NC}"
cat > .git/hooks/pre-push << 'EOF'
#!/bin/bash

# VOS4 Pre-push Hook - Run tests before pushing
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC

set -e

echo "🔍 Running tests before push..."

# Run unit tests
./gradlew test --quiet

if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Push aborted."
    echo "Fix the failing tests and try again."
    exit 1
fi

echo "✅ All tests passed. Proceeding with push."
exit 0
EOF

chmod +x .git/hooks/pre-push
echo -e "${GREEN}✓ Pre-push hook installed${NC}"

# Install commit-msg hook for conventional commits
echo -e "${YELLOW}Creating commit-msg hook...${NC}"
cat > .git/hooks/commit-msg << 'EOF'
#!/bin/bash

# VOS4 Commit Message Validation Hook
# 
# Enforces conventional commit format

commit_regex='^(feat|fix|docs|style|refactor|test|chore|perf|build|ci)(\(.+\))?: .{1,50}'

if ! grep -qE "$commit_regex" "$1"; then
    echo "❌ Invalid commit message format!"
    echo ""
    echo "Commit message must follow conventional format:"
    echo "  <type>(<scope>): <subject>"
    echo ""
    echo "Types: feat, fix, docs, style, refactor, test, chore, perf, build, ci"
    echo ""
    echo "Example: feat(voice): add new recognition engine"
    echo ""
    exit 1
fi
EOF

chmod +x .git/hooks/commit-msg
echo -e "${GREEN}✓ Commit-msg hook installed${NC}"

# Create a hook to update tests on file changes
echo -e "${YELLOW}Creating post-checkout hook...${NC}"
cat > .git/hooks/post-checkout << 'EOF'
#!/bin/bash

# VOS4 Post-checkout Hook - Remind to update tests
# 
# Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC

CHANGED_FILES=$(git diff --name-only HEAD~1 HEAD --diff-filter=ACM | grep -E "\.kt$" | grep -v Test.kt || true)

if [ -n "$CHANGED_FILES" ]; then
    echo ""
    echo "📝 The following source files changed in this checkout:"
    echo "$CHANGED_FILES" | while read file; do
        echo "  - $file"
    done
    echo ""
    echo "💡 Remember to update or create tests for these files!"
    echo "   Use: ./scripts/select-test-template.sh <file> to generate tests"
    echo ""
fi
EOF

chmod +x .git/hooks/post-checkout
echo -e "${GREEN}✓ Post-checkout hook installed${NC}"

# Configure Git to use hooks from .githooks directory (alternative)
echo -e "${YELLOW}Configuring Git hooks path...${NC}"
git config core.hooksPath .githooks 2>/dev/null || true
echo -e "${GREEN}✓ Git configured to use .githooks directory${NC}"

# Verify installations
echo
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}         Installation Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo
echo -e "${GREEN}Installed hooks:${NC}"
echo "  ✓ pre-commit    - Validates test coverage for changed files"
echo "  ✓ pre-push      - Runs all tests before pushing"
echo "  ✓ commit-msg    - Enforces conventional commit format"
echo "  ✓ post-checkout - Reminds to update tests for changed files"
echo
echo -e "${YELLOW}To bypass hooks (NOT recommended):${NC}"
echo "  git commit --no-verify"
echo "  git push --no-verify"
echo
echo -e "${BLUE}Test generation tools available:${NC}"
echo "  ./scripts/generate-test.sh <file>        - Basic test generation"
echo "  ./scripts/select-test-template.sh <file> - Smart template selection"
echo
echo -e "${GREEN}Happy testing! 🚀${NC}"
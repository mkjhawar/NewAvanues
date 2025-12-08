#!/usr/bin/env bash
#
# IDEACODE Initialization Script
# Can be run directly or called from batch files
#
# Usage:
#   ./ideacode-init.sh --new <project-name>
#   ./ideacode-init.sh --existing <project-name>
#   ./ideacode-init.sh --convert <project-name>
#   ./ideacode-init.sh --here --new
#
# Copyright © 2024-2025 Manoj Jhawar, Aman Jhawar. All Rights Reserved.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Banner
print_banner() {
    echo -e "${BLUE}"
    cat << "EOF"
██╗██████╗ ███████╗ █████╗  ██████╗ ██████╗ ██████╗ ███████╗
██║██╔══██╗██╔════╝██╔══██╗██╔════╝██╔═══██╗██╔══██╗██╔════╝
██║██║  ██║█████╗  ███████║██║     ██║   ██║██║  ██║█████╗
██║██║  ██║██╔══╝  ██╔══██║██║     ██║   ██║██║  ██║██╔══╝
██║██████╔╝███████╗██║  ██║╚██████╗╚██████╔╝██████╔╝███████╗
╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝

    Integrated Development Evaluation & Analysis CODE
    Version 1.0.0 | © 2024-2025 Manoj Jhawar, Aman Jhawar
EOF
    echo -e "${NC}"
}

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
IDEACODE_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Default values
MODE="new"
PROJECT_NAME=""
HERE=false
FORCE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --new)
            MODE="new"
            shift
            ;;
        --existing)
            MODE="existing"
            shift
            ;;
        --convert)
            MODE="convert"
            shift
            ;;
        --here)
            HERE=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [--new|--existing|--convert] [--here] [--force] [project-name]"
            echo ""
            echo "Modes:"
            echo "  --new        Initialize new project (default)"
            echo "  --existing   Add IDEACODE to existing project"
            echo "  --convert    Convert IDEADEV project to IDEACODE"
            echo ""
            echo "Options:"
            echo "  --here       Initialize in current directory"
            echo "  --force      Force overwrite without prompting"
            echo ""
            echo "Examples:"
            echo "  $0 --new my-project"
            echo "  $0 --existing ."
            echo "  $0 --convert my-ideadev-project"
            exit 0
            ;;
        *)
            PROJECT_NAME="$1"
            shift
            ;;
    esac
done

print_banner

# Determine project path
if [ "$HERE" = true ] || [ "$PROJECT_NAME" = "." ]; then
    PROJECT_PATH="$(pwd)"
    PROJECT_NAME="$(basename "$PROJECT_PATH")"
elif [ -z "$PROJECT_NAME" ]; then
    echo -e "${RED}❌ ERROR: Project name required${NC}"
    echo "Usage: $0 [--new|--existing|--convert] <project-name>"
    exit 1
else
    PROJECT_PATH="$(pwd)/$PROJECT_NAME"
fi

echo -e "${BLUE}📦 Mode: $MODE${NC}"
echo -e "${BLUE}📁 Project: $PROJECT_NAME${NC}"
echo -e "${BLUE}📂 Path: $PROJECT_PATH${NC}"
echo ""

# Create project directory for new projects
if [ "$MODE" = "new" ] && [ "$HERE" = false ] && [ "$PROJECT_NAME" != "." ]; then
    mkdir -p "$PROJECT_PATH"
    echo -e "${GREEN}✓ Created project directory${NC}"
fi

# Create directory structure
create_structure() {
    echo -e "${YELLOW}Creating directory structure...${NC}"

    mkdir -p "$PROJECT_PATH/.ideacode/memory"
    mkdir -p "$PROJECT_PATH/.ideacode/scripts/bash"
    mkdir -p "$PROJECT_PATH/.ideacode/templates"
    mkdir -p "$PROJECT_PATH/.claude/commands"
    mkdir -p "$PROJECT_PATH/specs"
    mkdir -p "$PROJECT_PATH/docs/ProjectInstructions"

    if [ "$MODE" = "new" ]; then
        mkdir -p "$PROJECT_PATH/src"
        mkdir -p "$PROJECT_PATH/tests"
    fi

    echo -e "${GREEN}✓ Created directories${NC}"
}

# Copy templates
copy_templates() {
    echo -e "${YELLOW}Copying templates...${NC}"

    if [ -d "$IDEACODE_ROOT/templates" ]; then
        cp -r "$IDEACODE_ROOT/templates/"* "$PROJECT_PATH/.ideacode/templates/"
        echo -e "${GREEN}✓ Copied templates${NC}"
    fi

    if [ -d "$IDEACODE_ROOT/commands" ]; then
        cp "$IDEACODE_ROOT/commands/idea."*.md "$PROJECT_PATH/.claude/commands/"
        echo -e "${GREEN}✓ Copied slash commands${NC}"
    fi

    if [ -d "$IDEACODE_ROOT/scripts" ]; then
        cp -r "$IDEACODE_ROOT/scripts/"* "$PROJECT_PATH/.ideacode/scripts/"
        echo -e "${GREEN}✓ Copied scripts${NC}"
    fi
}

# Create principles template
create_principles() {
    echo -e "${YELLOW}Creating principles template...${NC}"

    cat > "$PROJECT_PATH/.ideacode/memory/principles.md" << EOF
# $PROJECT_NAME Principles

<!-- Run /idea.principles to fill this template -->

## Core Principles

### [PRINCIPLE_1_NAME]
[PRINCIPLE_1_DESCRIPTION]

### [PRINCIPLE_2_NAME]
[PRINCIPLE_2_DESCRIPTION]

### [PRINCIPLE_3_NAME]
[PRINCIPLE_3_DESCRIPTION]

## Technical Constraints

[TECHNICAL_CONSTRAINTS]

## Development Workflow

[WORKFLOW_DESCRIPTION]

## Governance

[GOVERNANCE_RULES]

**Version**: [VERSION] | **Ratified**: [DATE] | **Last Amended**: [DATE]
EOF

    echo -e "${GREEN}✓ Created principles template${NC}"
}

# Create living docs
create_living_docs() {
    echo -e "${YELLOW}Creating living documentation...${NC}"

    cat > "$PROJECT_PATH/docs/ProjectInstructions/notes.md" << 'EOF'
# Implementation Notes

## Quick TODOs
- [ ] Item 1
- [ ] Item 2

## Gotchas
- Important thing to remember

## Insights
- Useful discovery
EOF

    cat > "$PROJECT_PATH/docs/ProjectInstructions/decisions.md" << 'EOF'
# Architectural Decisions

## Decision Log

### [YYYY-MM-DD] Decision Title
**Context**: Why we needed to make this decision
**Decision**: What we decided
**Rationale**: Why we chose this option
**Consequences**: What this means for the project
EOF

    cat > "$PROJECT_PATH/docs/ProjectInstructions/bugs.md" << 'EOF'
# Known Issues

## Active Bugs

### [P0] Critical Bug Title
**Severity**: P0 (Critical)
**Status**: Active
**Description**: What's broken
**Reproduction**: Steps to reproduce
**Workaround**: Temporary fix if available
EOF

    cat > "$PROJECT_PATH/docs/ProjectInstructions/progress.md" << 'EOF'
# Progress Tracking

## Current Sprint

**Goals**:
- Goal 1
- Goal 2

**Completed**:
- ✅ Task 1
- ✅ Task 2

**In Progress**:
- 🔄 Task 3

**Blocked**:
- ⛔ Task 4 (waiting on X)
EOF

    cat > "$PROJECT_PATH/docs/ProjectInstructions/backlog.md" << 'EOF'
# Feature Backlog

## High Priority
- [ ] Feature 1
- [ ] Feature 2

## Medium Priority
- [ ] Feature 3

## Low Priority / Nice to Have
- [ ] Feature 4
EOF

    echo -e "${GREEN}✓ Created living documentation${NC}"
}

# Create CLAUDE.md
create_claude_md() {
    echo -e "${YELLOW}Creating CLAUDE.md...${NC}"

    cat > "$PROJECT_PATH/CLAUDE.md" << EOF
# $PROJECT_NAME - AI Agent Quick Reference

## IDEACODE Framework

This project uses **IDEACODE** methodology.

### Core Commands

1. \`/idea.principles\` - Establish/update project governance
2. \`/idea.specify <feature>\` - Create feature specification
3. \`/idea.clarify\` - Resolve ambiguities (optional)
4. \`/idea.plan\` - Create implementation plan
5. \`/idea.tasks\` - Generate task breakdown
6. \`/idea.implement\` - Execute with IDE Loop
7. \`/idea.analyze\` - Verify compliance
8. \`/idea.checklist\` - Validate completeness

### Principles Location

- **Principles**: \`.ideacode/memory/principles.md\`
- **Templates**: \`.ideacode/templates/\`
- **Commands**: \`.claude/commands/idea.*.md\`

### Project Structure

- **Specs**: \`specs/###-feature-name/\`
- **Living Docs**: \`docs/ProjectInstructions/\`
  - notes.md, decisions.md, bugs.md, progress.md, backlog.md

### Mandatory Rules

1. **Spec-First**: No code without approved spec
2. **IDE Loop**: Implement → Defend → Evaluate (mandatory)
3. **Testing**: 80%+ coverage in Defend phase (cannot skip)
4. **Documentation**: Update living docs continuously

### IDE Loop Pattern

\`\`\`
FOR EACH PHASE:
  1. IMPLEMENT - Write code
  2. DEFEND - Write tests (MANDATORY)
  3. EVALUATE - Verify criteria
  4. COMMIT - Lock in progress
\`\`\`

### Before Starting Work

1. Read \`.ideacode/memory/principles.md\`
2. Check \`docs/ProjectInstructions/notes.md\`
3. Review \`docs/ProjectInstructions/progress.md\`
4. Check \`docs/ProjectInstructions/bugs.md\`

---

**Generated by IDEACODE v1.0.0**
EOF

    echo -e "${GREEN}✓ Created CLAUDE.md${NC}"
}

# Create README for new projects
create_readme() {
    if [ "$MODE" = "new" ] && [ ! -f "$PROJECT_PATH/README.md" ]; then
        echo -e "${YELLOW}Creating README.md...${NC}"

        cat > "$PROJECT_PATH/README.md" << EOF
# $PROJECT_NAME

## Getting Started

This project uses IDEACODE methodology.

### Next Steps

1. Run \`/idea.principles\` to establish project governance
2. Run \`/idea.specify <feature>\` to create your first feature
3. Follow the IDEACODE workflow

### IDEACODE Workflow

\`\`\`
1. /idea.principles    → Establish governance
2. /idea.specify       → Define requirements
3. /idea.plan          → Create plan
4. /idea.tasks         → Break down tasks
5. /idea.implement     → Execute with IDE Loop
\`\`\`

See CLAUDE.md for AI agent quick reference.
EOF

        echo -e "${GREEN}✓ Created README.md${NC}"
    fi
}

# Create .gitignore
create_gitignore() {
    if [ ! -f "$PROJECT_PATH/.gitignore" ]; then
        echo -e "${YELLOW}Creating .gitignore...${NC}"

        cat > "$PROJECT_PATH/.gitignore" << 'EOF'
# IDEACODE
.claude/

# Python
__pycache__/
*.py[cod]
*.so
.Python
.venv/
venv/
ENV/

# IDE
.idea/
.vscode/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db
EOF

        echo -e "${GREEN}✓ Created .gitignore${NC}"
    else
        # Update existing .gitignore
        if ! grep -q ".claude/" "$PROJECT_PATH/.gitignore"; then
            echo "" >> "$PROJECT_PATH/.gitignore"
            echo "# IDEACODE" >> "$PROJECT_PATH/.gitignore"
            echo ".claude/" >> "$PROJECT_PATH/.gitignore"
            echo -e "${GREEN}✓ Updated .gitignore${NC}"
        fi
    fi
}

# Initialize git
init_git() {
    if [ "$MODE" = "new" ] && [ ! -d "$PROJECT_PATH/.git" ]; then
        echo -e "${YELLOW}Initializing git...${NC}"
        cd "$PROJECT_PATH"
        git init
        echo -e "${GREEN}✓ Initialized git repository${NC}"
    fi
}

# Convert from IDEADEV
convert_from_ideadev() {
    echo -e "${YELLOW}Converting from IDEADEV...${NC}"

    if [ ! -d "$PROJECT_PATH/ideadev" ]; then
        echo -e "${RED}❌ ERROR: No ideadev/ directory found${NC}"
        exit 1
    fi

    # Backup IDEADEV
    mkdir -p "$PROJECT_PATH/docs/ideadev.bak"
    cp -r "$PROJECT_PATH/ideadev" "$PROJECT_PATH/docs/ideadev.bak/"
    echo -e "${GREEN}✓ Backed up ideadev/ → docs/ideadev.bak/${NC}"

    # Migrate specs
    if [ -d "$PROJECT_PATH/ideadev/specs" ]; then
        for spec_file in "$PROJECT_PATH/ideadev/specs/"*-spec.md; do
            if [ -f "$spec_file" ]; then
                filename=$(basename "$spec_file" .md)
                # Extract number and name
                number=$(echo "$filename" | cut -d'-' -f1)
                feature_name=$(echo "$filename" | cut -d'-' -f2-4)

                # Create new structure
                spec_dir="$PROJECT_PATH/specs/$number-$feature_name"
                mkdir -p "$spec_dir"

                # Copy files
                cp "$spec_file" "$spec_dir/spec.md"

                plan_file="$PROJECT_PATH/ideadev/plans/$(basename "$spec_file" -spec.md)-plan.md"
                [ -f "$plan_file" ] && cp "$plan_file" "$spec_dir/plan.md"

                review_file="$PROJECT_PATH/ideadev/reviews/$(basename "$spec_file" -spec.md)-review.md"
                [ -f "$review_file" ] && cp "$review_file" "$spec_dir/review.md"
            fi
        done
        echo -e "${GREEN}✓ Migrated specs to new structure${NC}"
    fi
}

# Print next steps
print_next_steps() {
    echo ""
    echo -e "${GREEN}✅ Successfully initialized IDEACODE!${NC}"
    echo ""
    echo -e "${YELLOW}📋 Next Steps:${NC}"
    echo "1. Review the generated structure"
    echo "2. Run /idea.principles to establish project governance"
    echo "3. Run /idea.specify <feature> to create your first feature"
    echo ""
    echo -e "${BLUE}📚 Documentation: See README.md${NC}"
    echo -e "${BLUE}🤖 AI Agent: See CLAUDE.md${NC}"
    echo ""
}

# Main execution
case "$MODE" in
    new)
        create_structure
        copy_templates
        create_principles
        create_living_docs
        create_claude_md
        create_readme
        create_gitignore
        init_git
        print_next_steps
        ;;
    existing)
        create_structure
        copy_templates
        create_principles
        create_living_docs
        create_claude_md
        create_gitignore
        print_next_steps
        ;;
    convert)
        convert_from_ideadev
        create_structure
        copy_templates
        create_principles
        create_living_docs
        create_claude_md
        create_gitignore
        print_next_steps
        ;;
esac

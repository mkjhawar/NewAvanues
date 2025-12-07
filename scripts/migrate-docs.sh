#!/usr/bin/env bash

# MainAvanues Documentation Migration Script
# Consolidates documentation from separate repos into hybrid centralized structure
# Date: 2025-11-24
# Version: 1.0

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Paths
MONOREPO_ROOT="/Volumes/M-Drive/Coding/MainAvanues"
CODING_ROOT="/Volumes/M-Drive/Coding"
REGISTRY_FILE="$MONOREPO_ROOT/docs/ideacode/registries/DOCUMENTATION-REGISTRY.json"

# Source repositories
REPO_NAMES=("AVA" "VoiceOS" "AVAConnect" "Avanues" "WebAvanue")
REPO_PATHS=(
    "$CODING_ROOT/AVA AI"
    "$CODING_ROOT/VoiceOS"
    "$CODING_ROOT/AVAConnect"
    "$CODING_ROOT/Avanues"
    "$CODING_ROOT/WebAvanue"
)

# Documentation targets
DOC_TARGETS=(
    "docs/android/apps/ava"
    "docs/android/apps/voiceos"
    "docs/android/apps/avaconnect"
    "docs/android/apps/avanues"
    "docs/web/apps/webavanue"
)

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║           MainAvanues Documentation Migration & Consolidation            ║"
echo "║                    Hybrid Centralized Documentation                      ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Dry run flag
DRY_RUN=false
if [[ "$1" == "--dry-run" ]]; then
    DRY_RUN=true
    echo -e "${YELLOW}DRY RUN MODE - No files will be moved${NC}"
    echo ""
fi

echo -e "${BLUE}[1/5] Analyzing documentation in source repos...${NC}"
echo ""

# Function to analyze docs in a repo
analyze_docs() {
    local repo_name=$1
    local repo_path=$2

    echo -e "${CYAN}$repo_name:${NC}"

    if [ ! -d "$repo_path" ]; then
        echo -e "  ${RED}✗ Repository not found${NC}"
        return
    fi

    # Count markdown files
    local md_count=$(find "$repo_path" -name "*.md" -not -path "*/node_modules/*" -not -path "*/.git/*" 2>/dev/null | wc -l | xargs)

    # Find docs folder
    local has_docs_folder="No"
    if [ -d "$repo_path/docs" ]; then
        local docs_md=$(find "$repo_path/docs" -name "*.md" 2>/dev/null | wc -l | xargs)
        has_docs_folder="Yes ($docs_md files)"
    fi

    # Check for IDEACODE folder
    local has_ideacode="No"
    if [ -d "$repo_path/.ideacode" ]; then
        has_ideacode="Yes"
    elif [ -d "$repo_path/docs/ideacode" ]; then
        has_ideacode="Yes (in docs/)"
    fi

    # List key docs
    local key_docs=""
    for doc in README.md ARCHITECTURE.md API.md CONTRIBUTING.md SETUP.md; do
        if [ -f "$repo_path/$doc" ] || [ -f "$repo_path/docs/$doc" ]; then
            key_docs="$key_docs $doc"
        fi
    done

    echo "  Markdown files: $md_count"
    echo "  docs/ folder: $has_docs_folder"
    echo "  IDEACODE: $has_ideacode"
    if [ -n "$key_docs" ]; then
        echo "  Key docs:$key_docs"
    fi
    echo ""
}

for i in "${!REPO_NAMES[@]}"; do
    analyze_docs "${REPO_NAMES[$i]}" "${REPO_PATHS[$i]}"
done

echo ""
echo -e "${BLUE}[2/5] Planning documentation migration...${NC}"
echo ""

# Function to plan doc migration
plan_doc_migration() {
    local repo_name=$1
    local repo_path=$2
    local target=$3
    local target_path="$MONOREPO_ROOT/$target"

    echo -e "${MAGENTA}$repo_name → $target${NC}"

    if [ ! -d "$repo_path" ]; then
        echo "  Source not found, skipping."
        echo ""
        return
    fi

    # Check for docs folder
    if [ -d "$repo_path/docs" ]; then
        echo "  ✓ Will migrate: docs/ → $target/"
        local doc_count=$(find "$repo_path/docs" -type f -name "*.md" 2>/dev/null | wc -l | xargs)
        echo "    Files: $doc_count"
    fi

    # Check for root-level docs
    local root_docs=$(find "$repo_path" -maxdepth 1 -name "*.md" 2>/dev/null)
    if [ -n "$root_docs" ]; then
        echo "  ✓ Will migrate root *.md files → $target/"
    fi

    # Check for IDEACODE folder
    if [ -d "$repo_path/.ideacode" ]; then
        echo "  ✓ Will migrate: .ideacode/ → $target/ideacode/"

        # Check for specs, features, archive
        if [ -d "$repo_path/.ideacode/specs" ]; then
            local spec_count=$(find "$repo_path/.ideacode/specs" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    - specs/ ($spec_count files)"
        fi
        if [ -d "$repo_path/.ideacode/features" ]; then
            local feature_count=$(find "$repo_path/.ideacode/features" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    - features/ ($feature_count files)"
        fi
        if [ -d "$repo_path/.ideacode/archive" ]; then
            local archive_count=$(find "$repo_path/.ideacode/archive" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    - archive/ ($archive_count files) → docs/archive/2024/"
        fi
    fi

    # Check for API docs
    if [ -d "$repo_path/api" ] || [ -d "$repo_path/docs/api" ]; then
        echo "  ✓ Will consolidate API docs"
    fi

    echo ""
}

for i in "${!REPO_NAMES[@]}"; do
    plan_doc_migration "${REPO_NAMES[$i]}" "${REPO_PATHS[$i]}" "${DOC_TARGETS[$i]}"
done

echo ""
echo -e "${BLUE}[3/5] Detecting duplicates and conflicts...${NC}"
echo ""

# Find files with same names across repos
echo -e "${YELLOW}Checking for duplicate filenames...${NC}"
declare -A file_registry

for i in "${!REPO_NAMES[@]}"; do
    repo_path="${REPO_PATHS[$i]}"
    repo_name="${REPO_NAMES[$i]}"

    if [ -d "$repo_path/docs" ]; then
        while IFS= read -r file; do
            filename=$(basename "$file")
            if [ -n "${file_registry[$filename]}" ]; then
                echo -e "${YELLOW}  ⚠ Duplicate: $filename${NC}"
                echo "    - ${file_registry[$filename]}"
                echo "    - $repo_name"
            else
                file_registry[$filename]="$repo_name"
            fi
        done < <(find "$repo_path/docs" -name "*.md" 2>/dev/null)
    fi
done

echo ""
echo -e "${BLUE}[4/5] Documentation consolidation strategy...${NC}"
echo ""

echo "Hybrid Centralized Structure:"
echo ""
echo "1. App-Specific Docs → /docs/{platform}/apps/{app}/"
echo "   - dev-*.md (development guides)"
echo "   - api-*.md (API documentation)"
echo "   - user-*.md (user guides)"
echo ""
echo "2. App IDEACODE → /docs/{platform}/apps/{app}/ideacode/"
echo "   - specs/ (feature specifications)"
echo "   - features/ (active features)"
echo "   - archive/ → /docs/archive/2024/ (historical)"
echo ""
echo "3. Monorepo-Wide Docs → /docs/"
echo "   - README.md, ARCHITECTURE.md, CONTRIBUTING.md"
echo "   - SETUP.md, DEPLOYMENT.md, TESTING.md"
echo ""
echo "4. Monorepo IDEACODE → /docs/ideacode/"
echo "   - Cross-app features and specs"
echo "   - registries/DOCUMENTATION-REGISTRY.json"
echo ""
echo "5. Library Docs → /docs/common/libs/{scope}/{lib}/"
echo "   - api-overview.md"
echo "   - API documentation for KMP libraries"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - Review the plan above${NC}"
    echo ""
    echo "To execute migration, run:"
    echo "  ./scripts/migrate-docs.sh"
    exit 0
fi

# Prompt for confirmation
echo -e "${YELLOW}WARNING: This will move and consolidate documentation${NC}"
echo -e "${YELLOW}Make sure you have committed all changes in source repos${NC}"
echo ""
read -p "Continue with documentation migration? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Documentation migration cancelled."
    exit 0
fi

echo ""
echo -e "${BLUE}[5/5] Executing documentation migration...${NC}"
echo ""

# Function to validate and rename file
validate_filename() {
    local filepath=$1
    local filename=$(basename "$filepath")

    # Check if filename follows convention: {type}-{context}-{topic}.md
    if [[ $filename =~ ^(dev|api|user|spec|adr|rfc|test|fix)-.+-.+\.md$ ]]; then
        echo "$filename"
        return 0
    fi

    # Try to suggest a better name
    local basename_no_ext="${filename%.md}"

    # Common patterns
    if [[ $basename_no_ext =~ ^README$ ]]; then
        echo "dev-overview.md"
    elif [[ $basename_no_ext =~ ^API ]]; then
        echo "api-overview.md"
    elif [[ $basename_no_ext =~ ^ARCHITECTURE ]]; then
        echo "dev-architecture.md"
    elif [[ $basename_no_ext =~ ^CONTRIBUTING ]]; then
        echo "dev-contributing.md"
    elif [[ $basename_no_ext =~ ^SETUP|INSTALL ]]; then
        echo "dev-setup.md"
    else
        # Default: prefix with dev-
        echo "dev-$basename_no_ext.md"
    fi
}

# Function to add to registry
add_to_registry() {
    local doc_path=$1
    local doc_type=$2
    local app_name=$3
    local description=$4

    # Extract relative path from monorepo root
    local rel_path="${doc_path#$MONOREPO_ROOT/}"

    # This would update the JSON registry
    # For now, we'll create a migration log
    echo "$rel_path|$doc_type|$app_name|$description" >> "$MONOREPO_ROOT/.doc-migration.log"
}

# Function to migrate docs for a repo
migrate_docs() {
    local repo_name=$1
    local repo_path=$2
    local target=$3
    local target_path="$MONOREPO_ROOT/$target"

    echo -e "${GREEN}Migrating $repo_name documentation...${NC}"

    if [ ! -d "$repo_path" ]; then
        echo "  Source not found, skipping."
        return
    fi

    # Create target directory
    mkdir -p "$target_path"

    # Create backup
    local backup_path="$MONOREPO_ROOT/.migration-backups/docs-$repo_name-$(date +%Y%m%d-%H%M%S)"
    if [ -d "$target_path" ] && [ "$(ls -A $target_path)" ]; then
        echo "  Creating backup: $backup_path"
        mkdir -p "$backup_path"
        cp -r "$target_path"/* "$backup_path/" 2>/dev/null || true
    fi

    # Migrate docs folder
    if [ -d "$repo_path/docs" ]; then
        echo "  Migrating docs folder..."

        # Copy markdown files with validation
        find "$repo_path/docs" -name "*.md" -type f | while read -r file; do
            local suggested_name=$(validate_filename "$file")
            local target_file="$target_path/$suggested_name"

            if [ -f "$target_file" ]; then
                echo "    ⚠ Conflict: $suggested_name (will add suffix)"
                suggested_name="${suggested_name%.md}-$repo_name.md"
                target_file="$target_path/$suggested_name"
            fi

            cp "$file" "$target_file"
            add_to_registry "$target_file" "doc" "$repo_name" "Migrated from $repo_name/docs/"
            echo "    ✓ $suggested_name"
        done

        # Copy other assets (images, diagrams)
        if [ -d "$repo_path/docs/images" ]; then
            mkdir -p "$target_path/images"
            cp -r "$repo_path/docs/images"/* "$target_path/images/" 2>/dev/null || true
            echo "    ✓ Copied images/"
        fi
    fi

    # Migrate root-level docs
    for doc in README.md ARCHITECTURE.md CONTRIBUTING.md SETUP.md API.md; do
        if [ -f "$repo_path/$doc" ]; then
            local suggested_name=$(validate_filename "$repo_path/$doc")
            cp "$repo_path/$doc" "$target_path/$suggested_name"
            echo "    ✓ $doc → $suggested_name"
        fi
    done

    # Migrate IDEACODE folder
    if [ -d "$repo_path/.ideacode" ]; then
        echo "  Migrating IDEACODE folder..."
        local ideacode_target="$target_path/ideacode"
        mkdir -p "$ideacode_target"

        # Migrate specs
        if [ -d "$repo_path/.ideacode/specs" ]; then
            mkdir -p "$ideacode_target/specs"
            cp -r "$repo_path/.ideacode/specs"/* "$ideacode_target/specs/" 2>/dev/null || true
            local spec_count=$(find "$ideacode_target/specs" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    ✓ specs/ ($spec_count files)"
        fi

        # Migrate features
        if [ -d "$repo_path/.ideacode/features" ]; then
            mkdir -p "$ideacode_target/features"
            cp -r "$repo_path/.ideacode/features"/* "$ideacode_target/features/" 2>/dev/null || true
            local feature_count=$(find "$ideacode_target/features" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    ✓ features/ ($feature_count files)"
        fi

        # Migrate archive to central archive
        if [ -d "$repo_path/.ideacode/archive" ]; then
            local archive_target="$MONOREPO_ROOT/docs/archive/2024/$repo_name"
            mkdir -p "$archive_target"
            cp -r "$repo_path/.ideacode/archive"/* "$archive_target/" 2>/dev/null || true
            local archive_count=$(find "$archive_target" -name "*.md" 2>/dev/null | wc -l | xargs)
            echo "    ✓ archive/ → docs/archive/2024/$repo_name/ ($archive_count files)"
        fi
    fi

    echo -e "${GREEN}✓${NC} Completed: $repo_name"
    echo ""
}

# Clear migration log
> "$MONOREPO_ROOT/.doc-migration.log"

# Migrate each repo's documentation
for i in "${!REPO_NAMES[@]}"; do
    migrate_docs "${REPO_NAMES[$i]}" "${REPO_PATHS[$i]}" "${DOC_TARGETS[$i]}"
done

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                  Documentation Migration Complete!                       ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Documentation Structure:${NC}"
echo ""
echo "  /docs/"
echo "  ├── README.md, ARCHITECTURE.md, etc. (monorepo-wide)"
echo "  ├── ideacode/ (monorepo IDEACODE)"
echo "  │   ├── specs/"
echo "  │   ├── features/"
echo "  │   └── registries/DOCUMENTATION-REGISTRY.json"
echo "  ├── android/apps/{app}/"
echo "  │   ├── dev-*.md, api-*.md (app docs)"
echo "  │   └── ideacode/ (app-specific IDEACODE)"
echo "  ├── common/libs/{scope}/{lib}/"
echo "  │   └── api-overview.md"
echo "  └── archive/2024/{repo}/ (historical docs)"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Review migrated docs in docs/"
echo "  2. Check .doc-migration.log for migration details"
echo "  3. Update DOCUMENTATION-REGISTRY.json"
echo "  4. Validate filename conventions"
echo "  5. Remove duplicates if any"
echo "  6. Update cross-references in docs"
echo ""
echo -e "${YELLOW}Backups:${NC}"
echo "  Backups saved to: .migration-backups/docs-*"
echo ""
echo -e "${CYAN}Migration Log:${NC}"
echo "  See: .doc-migration.log"
echo ""

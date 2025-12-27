#!/usr/bin/env bash

# MainAvanues Repository Migration Script
# Migrates code from separate repos into monorepo structure
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

# Source repositories (as parallel arrays)
REPO_NAMES=("AVA" "VoiceOS" "AVAConnect" "Avanues" "WebAvanue")
REPO_PATHS=(
    "$CODING_ROOT/AVA AI"
    "$CODING_ROOT/VoiceOS"
    "$CODING_ROOT/AVAConnect"
    "$CODING_ROOT/Avanues"
    "$CODING_ROOT/WebAvanue"
)

# Target locations in monorepo
REPO_TARGETS=(
    "android/apps/ava"
    "android/apps/voiceos"
    "android/apps/avaconnect"
    "android/apps/avanues"
    "web/apps/webavanue"
)

# Helper function to get index
get_repo_index() {
    local name=$1
    for i in "${!REPO_NAMES[@]}"; do
        if [ "${REPO_NAMES[$i]}" == "$name" ]; then
            echo "$i"
            return
        fi
    done
    echo "-1"
}

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║              MainAvanues Repository Migration Script                    ║"
echo "║                     Migrate Separate Repos to Monorepo                   ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Dry run flag
DRY_RUN=false
if [[ "$1" == "--dry-run" ]]; then
    DRY_RUN=true
    echo -e "${YELLOW}DRY RUN MODE - No files will be moved${NC}"
    echo ""
fi

# Check if repos exist
echo -e "${BLUE}[1/5] Checking source repositories...${NC}"
echo ""

missing_repos=()
for repo in "${!REPOS[@]}"; do
    repo_path="${REPOS[$repo]}"
    if [ -d "$repo_path" ]; then
        echo -e "${GREEN}✓${NC} Found: $repo at $repo_path"
    else
        echo -e "${RED}✗${NC} Missing: $repo at $repo_path"
        missing_repos+=("$repo")
    fi
done

if [ ${#missing_repos[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}Error: Missing repositories: ${missing_repos[*]}${NC}"
    echo "Please ensure all repositories exist before running migration."
    exit 1
fi

echo ""
echo -e "${BLUE}[2/5] Analyzing repository contents...${NC}"
echo ""

# Function to analyze repo structure
analyze_repo() {
    local repo_name=$1
    local repo_path=$2

    echo -e "${CYAN}Analyzing $repo_name...${NC}"

    # Count files
    local kotlin_files=$(find "$repo_path" -name "*.kt" 2>/dev/null | wc -l | xargs)
    local java_files=$(find "$repo_path" -name "*.java" 2>/dev/null | wc -l | xargs)
    local xml_files=$(find "$repo_path" -name "*.xml" 2>/dev/null | wc -l | xargs)
    local total_files=$(find "$repo_path" -type f 2>/dev/null | wc -l | xargs)

    # Check for build files
    local has_gradle=""
    if [ -f "$repo_path/build.gradle" ] || [ -f "$repo_path/build.gradle.kts" ]; then
        has_gradle="✓"
    else
        has_gradle="✗"
    fi

    # Check for git
    local has_git=""
    if [ -d "$repo_path/.git" ]; then
        local commit_count=$(cd "$repo_path" && git rev-list --count HEAD 2>/dev/null || echo "0")
        has_git="✓ ($commit_count commits)"
    else
        has_git="✗"
    fi

    # Print summary
    echo "  Files: $total_files total ($kotlin_files Kotlin, $java_files Java, $xml_files XML)"
    echo "  Gradle: $has_gradle"
    echo "  Git: $has_git"
    echo ""
}

for repo in "${!REPOS[@]}"; do
    analyze_repo "$repo" "${REPOS[$repo]}"
done

echo ""
echo -e "${BLUE}[3/5] Planning migration...${NC}"
echo ""

# Function to plan migration
plan_migration() {
    local repo_name=$1
    local repo_path=$2
    local target="${TARGETS[$repo_name]}"
    local target_path="$MONOREPO_ROOT/$target"

    echo -e "${MAGENTA}$repo_name:${NC}"
    echo "  Source: $repo_path"
    echo "  Target: $target_path"

    # Check what exists
    if [ -d "$target_path" ]; then
        echo "  Status: Target exists (will merge)"
    else
        echo "  Status: Target will be created"
    fi

    # Migration strategy
    echo "  Strategy:"

    # Check for Android app structure
    if [ -d "$repo_path/app/src" ]; then
        echo "    - Copy app/src → $target/src"
    elif [ -d "$repo_path/src" ]; then
        echo "    - Copy src → $target/src"
    fi

    # Check for resources
    if [ -d "$repo_path/app/src/main/res" ]; then
        echo "    - Copy resources → $target/src/main/res"
    elif [ -d "$repo_path/res" ]; then
        echo "    - Copy resources → $target/src/main/res"
    fi

    # Check for build files
    if [ -f "$repo_path/build.gradle.kts" ] || [ -f "$repo_path/app/build.gradle.kts" ]; then
        echo "    - Adapt build.gradle.kts for monorepo"
    fi

    # Check for docs
    if [ -d "$repo_path/docs" ]; then
        echo "    - Migrate docs → docs/android/apps/$(basename $target)/"
    fi

    # Check for .ideacode
    if [ -d "$repo_path/.ideacode" ]; then
        echo "    - Migrate .ideacode → docs/android/apps/$(basename $target)/ideacode/"
    fi

    echo ""
}

for repo in "${!REPOS[@]}"; do
    plan_migration "$repo" "${REPOS[$repo]}"
done

echo ""
echo -e "${YELLOW}[4/5] Migration ready${NC}"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}DRY RUN MODE - Review the plan above${NC}"
    echo ""
    echo "To execute migration, run:"
    echo "  ./scripts/migrate-repos.sh"
    exit 0
fi

# Prompt for confirmation
echo -e "${YELLOW}WARNING: This will move code from separate repos to monorepo${NC}"
echo -e "${YELLOW}Make sure you have committed all changes in source repos${NC}"
echo ""
read -p "Continue with migration? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Migration cancelled."
    exit 0
fi

echo ""
echo -e "${BLUE}[5/5] Executing migration...${NC}"
echo ""

# Function to migrate a repo
migrate_repo() {
    local repo_name=$1
    local repo_path=$2
    local target="${TARGETS[$repo_name]}"
    local target_path="$MONOREPO_ROOT/$target"

    echo -e "${GREEN}Migrating $repo_name...${NC}"

    # Create backup
    local backup_path="$MONOREPO_ROOT/.migration-backups/$repo_name-$(date +%Y%m%d-%H%M%S)"
    if [ -d "$target_path" ]; then
        echo "  Creating backup: $backup_path"
        mkdir -p "$(dirname "$backup_path")"
        cp -r "$target_path" "$backup_path"
    fi

    # Ensure target exists
    mkdir -p "$target_path"

    # Copy source code
    if [ -d "$repo_path/app/src" ]; then
        echo "  Copying app/src..."
        cp -r "$repo_path/app/src"/* "$target_path/src/" 2>/dev/null || true
    elif [ -d "$repo_path/src" ]; then
        echo "  Copying src..."
        cp -r "$repo_path/src"/* "$target_path/src/" 2>/dev/null || true
    fi

    # Copy resources
    if [ -d "$repo_path/app/src/main/res" ]; then
        echo "  Copying resources..."
        mkdir -p "$target_path/src/main/res"
        cp -r "$repo_path/app/src/main/res"/* "$target_path/src/main/res/" 2>/dev/null || true
    fi

    # Copy build files (for reference)
    if [ -f "$repo_path/build.gradle.kts" ]; then
        echo "  Copying build.gradle.kts (for reference)..."
        cp "$repo_path/build.gradle.kts" "$target_path/build.gradle.kts.original" 2>/dev/null || true
    elif [ -f "$repo_path/app/build.gradle.kts" ]; then
        cp "$repo_path/app/build.gradle.kts" "$target_path/build.gradle.kts.original" 2>/dev/null || true
    fi

    # Migrate documentation
    local app_name=$(basename "$target")
    local doc_target="$MONOREPO_ROOT/docs/android/apps/$app_name"

    if [ -d "$repo_path/docs" ]; then
        echo "  Migrating documentation..."
        mkdir -p "$doc_target"
        # Copy docs but avoid duplicating ideacode folder
        find "$repo_path/docs" -maxdepth 1 -type f -exec cp {} "$doc_target/" \; 2>/dev/null || true
    fi

    # Migrate IDEACODE folder
    if [ -d "$repo_path/.ideacode" ]; then
        echo "  Migrating IDEACODE folder..."
        mkdir -p "$doc_target/ideacode"
        cp -r "$repo_path/.ideacode"/* "$doc_target/ideacode/" 2>/dev/null || true
    fi

    echo -e "${GREEN}✓${NC} Completed: $repo_name"
    echo ""
}

# Migrate each repo
for repo in AVA VoiceOS AVAConnect Avanues WebAvanue; do
    if [ -d "${REPOS[$repo]}" ]; then
        migrate_repo "$repo" "${REPOS[$repo]}"
    fi
done

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                       Migration Complete!                                ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Next Steps:${NC}"
echo "  1. Review migrated code in android/apps/"
echo "  2. Create build.gradle.kts files for each app"
echo "  3. Update settings.gradle.kts to include all apps"
echo "  4. Extract shared code to common/libs/"
echo "  5. Test builds: ./gradlew build"
echo "  6. Commit changes to monorepo"
echo ""
echo -e "${BLUE}Backups:${NC}"
echo "  Backups saved to: .migration-backups/"
echo ""
echo -e "${YELLOW}Documentation:${NC}"
echo "  See docs/MONOREPO-STRUCTURE.md for complete structure"
echo "  See docs/README.md for development workflow"
echo ""

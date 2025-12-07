#!/usr/bin/env bash

# MainAvanues Clean Project Migration Script
# Migrates projects from separate repos using clean naming principles
# Version: 2.0 (Updated with lessons from WebAvanue migration)
# Date: 2025-11-24

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

MONOREPO_ROOT="/Volumes/M-Drive/Coding/MainAvanues"

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║              MainAvanues Clean Project Migration Script                 ║"
echo "║                   Following Clean Naming Principles                      ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Usage
if [ $# -lt 2 ]; then
    echo "Usage: $0 <source-path> <project-type> [--dry-run]"
    echo ""
    echo "Project Types:"
    echo "  android-app          - Android application"
    echo "  kmp-lib              - Kotlin Multiplatform library"
    echo "  kmp-browser          - Cross-platform browser (special case)"
    echo "  web-app              - Web application"
    echo ""
    echo "Examples:"
    echo "  $0 /path/to/AVA android-app"
    echo "  $0 /path/to/WebAvanue kmp-browser --dry-run"
    exit 1
fi

SOURCE_PATH="$1"
PROJECT_TYPE="$2"
DRY_RUN=false

if [ "$3" == "--dry-run" ]; then
    DRY_RUN=true
    echo -e "${YELLOW}DRY RUN MODE - No files will be moved${NC}"
    echo ""
fi

# Validate source
if [ ! -d "$SOURCE_PATH" ]; then
    echo -e "${RED}Error: Source path not found: $SOURCE_PATH${NC}"
    exit 1
fi

PROJECT_NAME=$(basename "$SOURCE_PATH")

echo -e "${BLUE}Project:${NC} $PROJECT_NAME"
echo -e "${BLUE}Source:${NC} $SOURCE_PATH"
echo -e "${BLUE}Type:${NC} $PROJECT_TYPE"
echo ""

# ==============================================================================
# CLEAN NAMING PRINCIPLES (from Migration Lessons Learned)
# ==============================================================================
#
# 1. NO TYPE PREFIXES
#    ❌ feature-*, data-*, ui-*, util-*
#    ✅ recognition/, cache/, components/
#
# 2. PARENT/CHILD FOR "PART OF"
#    ❌ libs/myapp/ + libs/myapp-data/ (siblings)
#    ✅ libs/myapp/data/ (parent/child)
#
# 3. GROUP PLATFORM VARIANTS
#    ❌ webview-android/, webview-ios/
#    ✅ webview/android/, webview/ios/
#
# 4. NO SCOPE REDUNDANCY
#    ❌ libs/browser/webavanue/ ("browser" redundant)
#    ✅ libs/webavanue/ (clean)
#
# 5. MINIMIZE NESTING
#    Target: 3-4 levels maximum
#
# ==============================================================================

# Analyze project structure
analyze_project() {
    echo -e "${CYAN}Analyzing project structure...${NC}"
    echo ""

    # Count files
    local kt_files=$(find "$SOURCE_PATH" -name "*.kt" -not -path "*/build/*" -not -path "*/.gradle/*" 2>/dev/null | wc -l | xargs)
    local java_files=$(find "$SOURCE_PATH" -name "*.java" -not -path "*/build/*" 2>/dev/null | wc -l | xargs)
    local xml_files=$(find "$SOURCE_PATH" -name "*.xml" -not -path "*/build/*" 2>/dev/null | wc -l | xargs)
    local md_files=$(find "$SOURCE_PATH" -name "*.md" -not -path "*/node_modules/*" 2>/dev/null | wc -l | xargs)

    echo "Kotlin files: $kt_files"
    echo "Java files: $java_files"
    echo "XML files: $xml_files"
    echo "Docs: $md_files"
    echo ""

    # Check for key directories
    echo "Project structure:"
    [ -d "$SOURCE_PATH/app" ] && echo "  ✓ app/"
    [ -d "$SOURCE_PATH/src" ] && echo "  ✓ src/"
    [ -d "$SOURCE_PATH/universal" ] && echo "  ✓ universal/ (KMP shared)"
    [ -d "$SOURCE_PATH/docs" ] && echo "  ✓ docs/"
    [ -d "$SOURCE_PATH/.ideacode" ] && echo "  ✓ .ideacode/"
    [ -d "$SOURCE_PATH/.ideacode-v2" ] && echo "  ✓ .ideacode-v2/"
    echo ""
}

# Design clean structure based on project type
design_structure() {
    echo -e "${MAGENTA}Designing clean structure...${NC}"
    echo ""

    case "$PROJECT_TYPE" in
        android-app)
            echo "Target structure:"
            echo "  android/apps/${PROJECT_NAME,,}/"
            echo "  docs/android/apps/${PROJECT_NAME,,}/"
            echo "  docs/android/apps/${PROJECT_NAME,,}/ideacode/"
            ;;

        kmp-lib)
            echo "Target structure:"
            echo "  common/libs/${PROJECT_NAME,,}/"
            echo "  docs/common/libs/${PROJECT_NAME,,}/"
            ;;

        kmp-browser)
            echo "Target structure (WebAvanue pattern):"
            echo "  android/apps/${PROJECT_NAME,,}/           # Android app shell"
            echo "  common/libs/${PROJECT_NAME,,}/"
            echo "    ├── universal/                          # 95% shared code"
            echo "    └── coredata/                           # Data layer (if applicable)"
            echo "  common/libs/webview/                      # Platform implementations"
            echo "    ├── android/"
            echo "    ├── ios/"
            echo "    └── desktop/ (macos/windows/linux)"
            echo "  docs/android/apps/${PROJECT_NAME,,}/"
            echo "  docs/common/libs/${PROJECT_NAME,,}/"
            echo ""
            echo -e "${YELLOW}Note: Clean naming principles:${NC}"
            echo "  • No 'feature-' or 'data-' prefixes"
            echo "  • CoreData is CHILD of main lib (not sibling)"
            echo "  • WebView platforms grouped in folders"
            ;;

        web-app)
            echo "Target structure:"
            echo "  web/apps/${PROJECT_NAME,,}/"
            echo "  docs/web/apps/${PROJECT_NAME,,}/"
            ;;

        *)
            echo -e "${RED}Unknown project type: $PROJECT_TYPE${NC}"
            exit 1
            ;;
    esac
    echo ""
}

# Create backup
create_backup() {
    local timestamp=$(date +%Y%m%d-%H%M%S)
    local backup_dir="$MONOREPO_ROOT/.migration-backups/${PROJECT_NAME,,}-$timestamp"

    echo -e "${CYAN}Creating backup...${NC}"
    mkdir -p "$backup_dir"

    # Check if target already exists
    case "$PROJECT_TYPE" in
        android-app)
            if [ -d "$MONOREPO_ROOT/android/apps/${PROJECT_NAME,,}" ]; then
                cp -r "$MONOREPO_ROOT/android/apps/${PROJECT_NAME,,}" "$backup_dir/"
            fi
            ;;
        kmp-browser)
            if [ -d "$MONOREPO_ROOT/common/libs/${PROJECT_NAME,,}" ]; then
                cp -r "$MONOREPO_ROOT/common/libs/${PROJECT_NAME,,}" "$backup_dir/"
            fi
            ;;
    esac

    echo "✓ Backup: $backup_dir"
    echo ""
}

# Execute migration
migrate_project() {
    echo -e "${GREEN}Executing migration...${NC}"
    echo ""

    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}DRY RUN - No files will be moved${NC}"
        return
    fi

    case "$PROJECT_TYPE" in
        android-app)
            migrate_android_app
            ;;
        kmp-browser)
            migrate_kmp_browser
            ;;
        kmp-lib)
            migrate_kmp_lib
            ;;
        web-app)
            migrate_web_app
            ;;
    esac

    echo ""
    echo -e "${GREEN}✓ Migration complete!${NC}"
}

# Migrate Android app
migrate_android_app() {
    local target_dir="$MONOREPO_ROOT/android/apps/${PROJECT_NAME,,}"
    local doc_dir="$MONOREPO_ROOT/docs/android/apps/${PROJECT_NAME,,}"

    echo "1. Migrating Android app..."
    mkdir -p "$target_dir"

    if [ -d "$SOURCE_PATH/app" ]; then
        cp -r "$SOURCE_PATH/app"/* "$target_dir/"
    elif [ -d "$SOURCE_PATH/src" ]; then
        mkdir -p "$target_dir/src"
        cp -r "$SOURCE_PATH/src"/* "$target_dir/src/"
    fi

    echo "✓ Code migrated"

    echo "2. Migrating documentation..."
    mkdir -p "$doc_dir"
    mkdir -p "$doc_dir/ideacode/features"

    if [ -d "$SOURCE_PATH/docs" ]; then
        cp -r "$SOURCE_PATH/docs"/* "$doc_dir/" 2>/dev/null || true
    fi

    if [ -d "$SOURCE_PATH/.ideacode" ]; then
        cp -r "$SOURCE_PATH/.ideacode"/* "$doc_dir/ideacode/" 2>/dev/null || true
    fi

    echo "✓ Documentation migrated"
}

# Migrate KMP browser (WebAvanue pattern)
migrate_kmp_browser() {
    local app_name="${PROJECT_NAME,,}"

    echo "1. Migrating Android app shell..."
    mkdir -p "$MONOREPO_ROOT/android/apps/$app_name"
    if [ -d "$SOURCE_PATH/app" ]; then
        cp -r "$SOURCE_PATH/app"/* "$MONOREPO_ROOT/android/apps/$app_name/"
        echo "✓ Android app ($(find "$MONOREPO_ROOT/android/apps/$app_name/src" -name "*.kt" 2>/dev/null | wc -l | xargs) Kotlin files)"
    fi

    echo "2. Migrating shared code (universal)..."
    mkdir -p "$MONOREPO_ROOT/common/libs/$app_name/universal"
    if [ -d "$SOURCE_PATH/universal" ]; then
        cp -r "$SOURCE_PATH/universal"/* "$MONOREPO_ROOT/common/libs/$app_name/universal/"
        echo "✓ Universal ($(find "$MONOREPO_ROOT/common/libs/$app_name/universal" -name "*.kt" 2>/dev/null | wc -l | xargs) Kotlin files)"
    fi

    echo "3. Migrating data layer (coredata)..."
    mkdir -p "$MONOREPO_ROOT/common/libs/$app_name/coredata"
    if [ -d "$SOURCE_PATH/BrowserCoreData" ]; then
        cp -r "$SOURCE_PATH/BrowserCoreData"/* "$MONOREPO_ROOT/common/libs/$app_name/coredata/"
        echo "✓ CoreData ($(find "$MONOREPO_ROOT/common/libs/$app_name/coredata" -name "*.kt" 2>/dev/null | wc -l | xargs) Kotlin files)"
    fi

    echo "4. Migrating platform WebView implementations..."
    mkdir -p "$MONOREPO_ROOT/common/libs/webview/android"
    mkdir -p "$MONOREPO_ROOT/common/libs/webview/ios"
    mkdir -p "$MONOREPO_ROOT/common/libs/webview/desktop"

    if [ -d "$SOURCE_PATH/Android" ]; then
        cp -r "$SOURCE_PATH/Android"/* "$MONOREPO_ROOT/common/libs/webview/android/" 2>/dev/null || true
        echo "✓ Android WebView"
    fi

    if [ -d "$SOURCE_PATH/iOS" ]; then
        cp -r "$SOURCE_PATH/iOS"/* "$MONOREPO_ROOT/common/libs/webview/ios/" 2>/dev/null || true
        echo "✓ iOS WebView"
    fi

    if [ -d "$SOURCE_PATH/Desktop" ]; then
        cp -r "$SOURCE_PATH/Desktop"/* "$MONOREPO_ROOT/common/libs/webview/desktop/" 2>/dev/null || true
        echo "✓ Desktop WebView"
    fi

    echo "5. Migrating documentation..."
    mkdir -p "$MONOREPO_ROOT/docs/android/apps/$app_name"
    mkdir -p "$MONOREPO_ROOT/docs/common/libs/$app_name"
    mkdir -p "$MONOREPO_ROOT/docs/common/libs/$app_name/ideacode/features"

    if [ -d "$SOURCE_PATH/docs" ]; then
        find "$SOURCE_PATH/docs" -maxdepth 1 -name "*.md" -exec cp {} "$MONOREPO_ROOT/docs/android/apps/$app_name/" \; 2>/dev/null || true
        cp "$SOURCE_PATH/README.md" "$MONOREPO_ROOT/docs/common/libs/$app_name/" 2>/dev/null || true
    fi

    if [ -d "$SOURCE_PATH/.ideacode-v2/features" ]; then
        cp -r "$SOURCE_PATH/.ideacode-v2/features"/* "$MONOREPO_ROOT/docs/common/libs/$app_name/ideacode/features/" 2>/dev/null || true
        echo "✓ IDEACODE features migrated"
    fi
}

# Migrate KMP library
migrate_kmp_lib() {
    echo "KMP library migration not yet implemented"
}

# Migrate web app
migrate_web_app() {
    echo "Web app migration not yet implemented"
}

# Main execution
main() {
    analyze_project
    design_structure

    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}This is a dry run. No files will be moved.${NC}"
        echo "Run without --dry-run to execute migration."
        exit 0
    fi

    echo -e "${YELLOW}Ready to migrate $PROJECT_NAME${NC}"
    echo ""
    read -p "Continue? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        echo "Migration cancelled."
        exit 0
    fi

    create_backup
    migrate_project

    echo ""
    echo "╔══════════════════════════════════════════════════════════════════════════╗"
    echo "║                       Migration Complete!                                ║"
    echo "╚══════════════════════════════════════════════════════════════════════════╝"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "  1. Verify migrated code"
    echo "  2. Update build.gradle.kts files"
    echo "  3. Update settings.gradle.kts"
    echo "  4. Test builds"
    echo "  5. Update documentation links"
    echo ""
    echo -e "${GREEN}See also:${NC}"
    echo "  • docs/migration-analysis/MIGRATION-LESSONS-LEARNED.md"
    echo "  • /Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md"
    echo ""
}

main

#!/bin/bash

# MainAvanues Monorepo Structure Setup
# Creates platform-first folder structure based on research findings
# Date: 2025-11-24
# Version: 1.0

set -e

REPO_ROOT="/Volumes/M-Drive/Coding/MainAvanues"
cd "$REPO_ROOT"

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                  MainAvanues Monorepo Structure Setup                   ║"
echo "║                     Platform-First Organization                          ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

create_dir() {
    if [ ! -d "$1" ]; then
        mkdir -p "$1"
        echo -e "${GREEN}✓${NC} Created: $1"
    else
        echo -e "${BLUE}→${NC} Exists: $1"
    fi
}

create_file() {
    if [ ! -f "$1" ]; then
        touch "$1"
        echo -e "${GREEN}✓${NC} Created: $1"
    else
        echo -e "${BLUE}→${NC} Exists: $1"
    fi
}

echo -e "${YELLOW}[1/6] Creating platform directories...${NC}"
echo ""

# Platform directories
create_dir "android"
create_dir "ios"
create_dir "web"
create_dir "common"

echo ""
echo -e "${YELLOW}[2/6] Creating Android app structure...${NC}"
echo ""

# Android apps
create_dir "android/apps/ava"
create_dir "android/apps/ava/src/main/kotlin/net/ideahq/ava"
create_dir "android/apps/ava/src/main/res"

create_dir "android/apps/voiceos"
create_dir "android/apps/voiceos/src/main/kotlin/net/ideahq/voiceos"
create_dir "android/apps/voiceos/src/main/res"

create_dir "android/apps/avaconnect"
create_dir "android/apps/avaconnect/src/main/kotlin/net/ideahq/avaconnect"
create_dir "android/apps/avaconnect/src/main/res"

create_dir "android/apps/avanues"
create_dir "android/apps/avanues/src/main/kotlin/net/ideahq/avanues"
create_dir "android/apps/avanues/src/main/res"

# Android modules (platform-specific services)
create_dir "android/modules/accessibility"
create_dir "android/modules/accessibility/src/main/kotlin"

create_dir "android/modules/voice-engine"
create_dir "android/modules/voice-engine/src/main/kotlin"

echo ""
echo -e "${YELLOW}[3/6] Creating common (KMP) library structure...${NC}"
echo ""

# Common/KMP libraries with scope + type organization
# Voice scope
create_dir "common/libs/voice/feature-recognition/src/commonMain/kotlin"
create_dir "common/libs/voice/feature-recognition/src/androidMain/kotlin"
create_dir "common/libs/voice/feature-recognition/src/iosMain/kotlin"
create_dir "common/libs/voice/feature-recognition/src/commonTest/kotlin"

create_dir "common/libs/voice/feature-dsl/src/commonMain/kotlin"
create_dir "common/libs/voice/feature-dsl/src/androidMain/kotlin"
create_dir "common/libs/voice/feature-dsl/src/iosMain/kotlin"

create_dir "common/libs/voice/data-access-api/src/commonMain/kotlin"
create_dir "common/libs/voice/ui-waveform/src/commonMain/kotlin"
create_dir "common/libs/voice/util-audio/src/commonMain/kotlin"

# Accessibility scope
create_dir "common/libs/accessibility/feature-voice-cursor/src/commonMain/kotlin"
create_dir "common/libs/accessibility/feature-voice-cursor/src/androidMain/kotlin"
create_dir "common/libs/accessibility/feature-voice-cursor/src/iosMain/kotlin"

create_dir "common/libs/accessibility/feature-voice-keyboard/src/commonMain/kotlin"
create_dir "common/libs/accessibility/util-gestures/src/commonMain/kotlin"

# Browser scope
create_dir "common/libs/browser/feature-extension/src/commonMain/kotlin"
create_dir "common/libs/browser/data-access-tabs/src/commonMain/kotlin"
create_dir "common/libs/browser/util-dom/src/commonMain/kotlin"

# Cloud scope
create_dir "common/libs/cloud/feature-sync/src/commonMain/kotlin"
create_dir "common/libs/cloud/data-access-firebase/src/commonMain/kotlin"
create_dir "common/libs/cloud/util-auth/src/commonMain/kotlin"

# Shared scope (cross-cutting)
create_dir "common/libs/shared/ui-design-system/src/commonMain/kotlin"
create_dir "common/libs/shared/ui-design-system/src/androidMain/kotlin"
create_dir "common/libs/shared/ui-design-system/src/iosMain/kotlin"

create_dir "common/libs/shared/data-access-repository/src/commonMain/kotlin"
create_dir "common/libs/shared/util-logger/src/commonMain/kotlin"
create_dir "common/libs/shared/util-network/src/commonMain/kotlin"

echo ""
echo -e "${YELLOW}[4/6] Creating iOS structure (future)...${NC}"
echo ""

create_dir "ios/apps"
create_dir "ios/modules"

echo ""
echo -e "${YELLOW}[5/6] Creating web structure...${NC}"
echo ""

create_dir "web/apps/webavanue"
create_dir "web/apps/webavanue/src"
create_dir "web/apps/webavanue/public"

echo ""
echo -e "${YELLOW}[6/6] Creating documentation structure (hybrid centralized)...${NC}"
echo ""

# Root-level monorepo docs (FLAT)
create_dir "docs"
create_file "docs/README.md"
create_file "docs/ARCHITECTURE.md"
create_file "docs/CONTRIBUTING.md"
create_file "docs/SETUP.md"
create_file "docs/DEPLOYMENT.md"
create_file "docs/TESTING.md"

# Root IDEACODE folder (monorepo-level)
create_dir "docs/ideacode"
create_dir "docs/ideacode/specs"
create_dir "docs/ideacode/features"
create_dir "docs/ideacode/archive"
create_dir "docs/ideacode/registries"

# Create documentation registry
cat > "docs/ideacode/registries/DOCUMENTATION-REGISTRY.json" << 'EOF'
{
  "version": "1.0",
  "last_updated": "2025-11-24",
  "documents": {},
  "naming_convention": {
    "pattern": "{type}-{context}-{topic}.md",
    "types": ["dev", "api", "user", "spec", "adr", "rfc", "test", "fix"],
    "examples": [
      "dev-android-setup.md",
      "api-voice-recognition.md",
      "user-accessibility-features.md",
      "spec-voice-dsl.md",
      "adr-001-kmp-migration.md"
    ]
  }
}
EOF
echo -e "${GREEN}✓${NC} Created: docs/ideacode/registries/DOCUMENTATION-REGISTRY.json"

# Platform-specific docs (mirrors platform structure)
# Android
create_dir "docs/android/apps/ava"
create_file "docs/android/apps/ava/dev-overview.md"

create_dir "docs/android/apps/ava/ideacode"
create_dir "docs/android/apps/ava/ideacode/specs"
create_dir "docs/android/apps/ava/ideacode/features"
create_dir "docs/android/apps/ava/ideacode/archive"

create_dir "docs/android/apps/voiceos"
create_file "docs/android/apps/voiceos/dev-overview.md"

create_dir "docs/android/apps/voiceos/ideacode"
create_dir "docs/android/apps/voiceos/ideacode/specs"
create_dir "docs/android/apps/voiceos/ideacode/features"
create_dir "docs/android/apps/voiceos/ideacode/archive"

create_dir "docs/android/apps/avaconnect"
create_file "docs/android/apps/avaconnect/dev-overview.md"

create_dir "docs/android/apps/avaconnect/ideacode"

create_dir "docs/android/apps/avanues"
create_file "docs/android/apps/avanues/dev-overview.md"

create_dir "docs/android/apps/avanues/ideacode"

# Common/KMP libs
create_dir "docs/common/libs/voice/feature-recognition"
create_file "docs/common/libs/voice/feature-recognition/api-overview.md"

create_dir "docs/common/libs/accessibility/feature-voice-cursor"
create_file "docs/common/libs/accessibility/feature-voice-cursor/api-overview.md"

create_dir "docs/common/libs/shared/ui-design-system"
create_file "docs/common/libs/shared/ui-design-system/api-overview.md"

# Web
create_dir "docs/web/apps/webavanue"
create_file "docs/web/apps/webavanue/dev-overview.md"

create_dir "docs/web/apps/webavanue/ideacode"

# Archive (by year)
create_dir "docs/archive/2024"

# Root-level support folders
create_dir "examples"
create_dir "scripts"
create_dir "tools"

# Configuration files
create_dir ".ideacode"
create_file ".ideacode/config.yml"

# Write config file
cat > ".ideacode/config.yml" << 'EOF'
version: "8.5"
framework_path: "/Volumes/M-Drive/Coding/ideacode"
project_name: "MainAvanues"
profile: "android-app"
voice_first: true
monorepo: true
platforms:
  - android
  - ios
  - web
EOF
echo -e "${GREEN}✓${NC} Created: .ideacode/config.yml"

echo ""
echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║                          Setup Complete!                                 ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${GREEN}Next Steps:${NC}"
echo "  1. Review the created structure"
echo "  2. Run migration script to move existing code"
echo "  3. Create build.gradle.kts files for KMP modules"
echo "  4. Update settings.gradle.kts to include all modules"
echo ""
echo -e "${BLUE}Structure Summary:${NC}"
echo "  • Platform-first organization (android/, ios/, web/, common/)"
echo "  • 80/20 rule: Most code in common/libs (KMP)"
echo "  • Scope + Type library organization"
echo "  • Hybrid centralized documentation"
echo "  • Multiple IDEACODE folders (root + per app)"
echo "  • Documentation registry for AI validation"
echo ""

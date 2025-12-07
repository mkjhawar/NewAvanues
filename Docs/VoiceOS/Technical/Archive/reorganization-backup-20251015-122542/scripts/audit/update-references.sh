#!/bin/bash
#
# Filename: update-references.sh
# Created: 2025-10-15 12:06:03 PDT
# Purpose: Automatically update all file references after folder reorganization
# Usage: ./update-references.sh
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  VOS4 DOCUMENTATION REFERENCE UPDATE"
echo "  Updating ~735 file references"
echo "=========================================="
echo ""

# Change to vos4 root directory
cd "/Volumes/M Drive/Coding/Warp/vos4" || exit 1

# Create backup timestamp
BACKUP_TIMESTAMP=$(date "+%Y%m%d-%H%M%S")
BACKUP_DIR="/Volumes/M Drive/Coding/Warp/vos4/docs/archive/reference-update-backup-${BACKUP_TIMESTAMP}"

echo "${BLUE}=== STEP 1: Create Backup ===${NC}"
echo ""
echo "Creating backup of all markdown files before making changes..."
mkdir -p "$BACKUP_DIR"
rsync -a --include="*/" --include="*.md" --exclude="*" docs/ "$BACKUP_DIR/"
echo "${GREEN}✓ Backup created: $BACKUP_DIR${NC}"
echo ""

echo "${BLUE}=== STEP 2: Update Module Folder References ===${NC}"
echo ""
echo "Updating references to renamed module folders..."
echo ""

# Count total changes
TOTAL_CHANGES=0

# Function to update references and count changes
update_references() {
    local old_path="$1"
    local new_path="$2"
    local description="$3"

    echo -n "  ${YELLOW}$description${NC}: "

    # Count matches before replacement
    local count=$(grep -r "$old_path" docs --include="*.md" 2>/dev/null | wc -l | tr -d ' ')

    if [ "$count" -gt 0 ]; then
        # Perform replacement on all markdown files
        find docs -type f -name "*.md" -exec sed -i '' "s|${old_path}|${new_path}|g" {} \;
        echo "${GREEN}$count references updated${NC}"
        TOTAL_CHANGES=$((TOTAL_CHANGES + count))
    else
        echo "${GREEN}0 references (none found)${NC}"
    fi
}

# APPS (5 modules)
echo "${YELLOW}Apps (5):${NC}"
update_references "modules/learnapp" "modules/LearnApp" "learnapp → LearnApp"
update_references "modules/voice-cursor" "modules/VoiceCursor" "voice-cursor → VoiceCursor"
update_references "modules/voiceos-core" "modules/VoiceOSCore" "voiceos-core → VoiceOSCore"
update_references "modules/voice-recognition" "modules/VoiceRecognition" "voice-recognition → VoiceRecognition"
update_references "modules/voice-ui" "modules/VoiceUI" "voice-ui → VoiceUI"
echo ""

# LIBRARIES (9 modules)
echo "${YELLOW}Libraries (9):${NC}"
update_references "modules/device-manager" "modules/DeviceManager" "device-manager → DeviceManager"
update_references "modules/magicelements" "modules/MagicElements" "magicelements → MagicElements"
update_references "modules/magicui" "modules/MagicUI" "magicui → MagicUI"
update_references "modules/speech-recognition" "modules/SpeechRecognition" "speech-recognition → SpeechRecognition"
update_references "modules/translation" "modules/Translation" "translation → Translation"
update_references "modules/uuidcreator" "modules/UUIDCreator" "uuidcreator → UUIDCreator"
update_references "modules/uuid-manager" "modules/UUIDCreator" "uuid-manager → UUIDCreator"
update_references "modules/voice-keyboard" "modules/VoiceKeyboard" "voice-keyboard → VoiceKeyboard"
update_references "modules/voiceos-logger" "modules/VoiceOsLogger" "voiceos-logger → VoiceOsLogger"
update_references "modules/voice-ui-elements" "modules/VoiceUIElements" "voice-ui-elements → VoiceUIElements"
echo ""

# MANAGERS (5 modules)
echo "${YELLOW}Managers (5):${NC}"
update_references "modules/command-manager" "modules/CommandManager" "command-manager → CommandManager"
update_references "modules/hud-manager" "modules/HUDManager" "hud-manager → HUDManager"
update_references "modules/license-manager" "modules/LicenseManager" "license-manager → LicenseManager"
update_references "modules/localization-manager" "modules/LocalizationManager" "localization-manager → LocalizationManager"
update_references "modules/voice-data-manager" "modules/VoiceDataManager" "voice-data-manager → VoiceDataManager"
echo ""

echo "${BLUE}=== STEP 3: Update Root Folder References ===${NC}"
echo ""
echo "Updating references to merged root folders..."
echo ""

update_references "docs/voice-accessibility" "docs/modules/VoiceOSCore/accessibility" "voice-accessibility → VoiceOSCore/accessibility"
update_references "docs/voice-cursor" "docs/modules/VoiceCursor" "voice-cursor → VoiceCursor"
update_references "docs/device-manager" "docs/modules/DeviceManager" "device-manager → DeviceManager"
update_references "docs/speech-recognition" "docs/modules/SpeechRecognition" "speech-recognition → SpeechRecognition"
update_references "docs/data-manager" "docs/modules/VoiceDataManager" "data-manager → VoiceDataManager"
update_references "docs/magicui" "docs/modules/MagicUI" "magicui → MagicUI"
echo ""

echo "${BLUE}=== STEP 4: Verify Changes ===${NC}"
echo ""
echo "Verifying all old references have been updated..."
echo ""

# Check for any remaining old references
REMAINING=0

check_remaining() {
    local old_path="$1"
    local name="$2"
    local count=$(grep -r "$old_path" docs --include="*.md" 2>/dev/null | wc -l | tr -d ' ')
    if [ "$count" -gt 0 ]; then
        echo "  ${RED}⚠ $name: $count references still found${NC}"
        REMAINING=$((REMAINING + count))
    fi
}

# Check all old paths
check_remaining "modules/learnapp" "learnapp"
check_remaining "modules/voice-cursor" "voice-cursor"
check_remaining "modules/voiceos-core" "voiceos-core"
check_remaining "modules/voice-recognition" "voice-recognition"
check_remaining "modules/voice-ui" "voice-ui"
check_remaining "modules/device-manager" "device-manager"
check_remaining "modules/magicelements" "magicelements"
check_remaining "modules/magicui" "magicui"
check_remaining "modules/speech-recognition" "speech-recognition"
check_remaining "modules/translation" "translation"
check_remaining "modules/uuidcreator" "uuidcreator"
check_remaining "modules/uuid-manager" "uuid-manager"
check_remaining "modules/voice-keyboard" "voice-keyboard"
check_remaining "modules/voiceos-logger" "voiceos-logger"
check_remaining "modules/voice-ui-elements" "voice-ui-elements"
check_remaining "modules/command-manager" "command-manager"
check_remaining "modules/hud-manager" "hud-manager"
check_remaining "modules/license-manager" "license-manager"
check_remaining "modules/localization-manager" "localization-manager"
check_remaining "modules/voice-data-manager" "voice-data-manager"
check_remaining "docs/voice-accessibility" "voice-accessibility"
check_remaining "docs/device-manager" "device-manager (root)"
check_remaining "docs/speech-recognition" "speech-recognition (root)"
check_remaining "docs/data-manager" "data-manager"

if [ "$REMAINING" -eq 0 ]; then
    echo "${GREEN}✓ All old references successfully updated!${NC}"
else
    echo "${RED}⚠ Warning: $REMAINING old references still remain${NC}"
    echo "  Run verification script to investigate"
fi
echo ""

echo "${GREEN}==========================================${NC}"
echo "${GREEN}  REFERENCE UPDATE COMPLETE${NC}"
echo "${GREEN}==========================================${NC}"
echo ""
echo "Summary:"
echo "  - Total references updated: $TOTAL_CHANGES"
echo "  - Remaining old references: $REMAINING"
echo "  - Backup location: $BACKUP_DIR"
echo ""

if [ "$REMAINING" -gt 0 ]; then
    echo "${YELLOW}⚠ REVIEW REQUIRED:${NC}"
    echo "  Some old references remain. Review manually or investigate further."
    echo ""
fi

echo "Next steps:"
echo "  1. Verify file references are correct"
echo "  2. Run execute-reorganization.sh to rename actual folders"
echo "  3. Test documentation links"
echo ""

#!/bin/bash
#
# Filename: execute-reorganization.sh
# Created: 2025-10-15 12:06:03 PDT
# Purpose: Execute VOS4 documentation folder reorganization (REAL CHANGES)
# Usage: ./execute-reorganization.sh
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  VOS4 DOCUMENTATION REORGANIZATION"
echo "  ⚠️  REAL CHANGES WILL BE MADE"
echo "=========================================="
echo ""
echo "${RED}WARNING: This script will rename and merge folders.${NC}"
echo "${RED}Make sure you have a backup before proceeding!${NC}"
echo ""
read -p "Continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

# Change to docs directory
cd "/Volumes/M Drive/Coding/Warp/vos4/docs" || exit 1

# Create backup timestamp
BACKUP_TIMESTAMP=$(date "+%Y%m%d-%H%M%S")
BACKUP_DIR="/Volumes/M Drive/Coding/Warp/vos4/docs/archive/reorganization-backup-${BACKUP_TIMESTAMP}"

echo ""
echo "${BLUE}=== STEP 1: Create Full Backup ===${NC}"
echo ""
echo "Creating full backup of docs/ directory..."
mkdir -p "$BACKUP_DIR"
rsync -a . "$BACKUP_DIR/"
echo "${GREEN}✓ Backup created: $BACKUP_DIR${NC}"
echo ""

echo "${BLUE}=== STEP 2: Rename Module Folders ===${NC}"
echo ""
echo "Renaming module folders to PascalCase..."
echo ""

# Function to safely rename folder
rename_folder() {
    local old_name="$1"
    local new_name="$2"
    local description="$3"

    if [ -d "$old_name" ]; then
        echo -n "  ${YELLOW}$description${NC}: "
        mv "$old_name" "$new_name"
        echo "${GREEN}✓ Renamed${NC}"
    else
        echo "  ${YELLOW}$description${NC}: ${BLUE}Already renamed or doesn't exist${NC}"
    fi
}

# APPS (5 modules)
echo "${YELLOW}Apps (5):${NC}"
rename_folder "modules/learnapp" "modules/LearnApp" "learnapp → LearnApp"
rename_folder "modules/voice-cursor" "modules/VoiceCursor" "voice-cursor → VoiceCursor"
rename_folder "modules/voiceos-core" "modules/VoiceOSCore" "voiceos-core → VoiceOSCore"
rename_folder "modules/voice-recognition" "modules/VoiceRecognition" "voice-recognition → VoiceRecognition"
rename_folder "modules/voice-ui" "modules/VoiceUI" "voice-ui → VoiceUI"
echo ""

# LIBRARIES (9 modules)
echo "${YELLOW}Libraries (9):${NC}"
rename_folder "modules/device-manager" "modules/DeviceManager" "device-manager → DeviceManager"
rename_folder "modules/magicelements" "modules/MagicElements" "magicelements → MagicElements"
rename_folder "modules/magicui" "modules/MagicUI" "magicui → MagicUI"
rename_folder "modules/speech-recognition" "modules/SpeechRecognition" "speech-recognition → SpeechRecognition"
rename_folder "modules/translation" "modules/Translation" "translation → Translation"
rename_folder "modules/uuidcreator" "modules/UUIDCreator" "uuidcreator → UUIDCreator"
rename_folder "modules/voice-keyboard" "modules/VoiceKeyboard" "voice-keyboard → VoiceKeyboard"
rename_folder "modules/voiceos-logger" "modules/VoiceOsLogger" "voiceos-logger → VoiceOsLogger"
rename_folder "modules/voice-ui-elements" "modules/VoiceUIElements" "voice-ui-elements → VoiceUIElements"
echo ""

# MANAGERS (5 modules)
echo "${YELLOW}Managers (5):${NC}"
rename_folder "modules/command-manager" "modules/CommandManager" "command-manager → CommandManager"
rename_folder "modules/hud-manager" "modules/HUDManager" "hud-manager → HUDManager"
rename_folder "modules/license-manager" "modules/LicenseManager" "license-manager → LicenseManager"
rename_folder "modules/localization-manager" "modules/LocalizationManager" "localization-manager → LocalizationManager"
rename_folder "modules/voice-data-manager" "modules/VoiceDataManager" "voice-data-manager → VoiceDataManager"
echo ""

echo "${BLUE}=== STEP 3: Handle UUID-Manager ===${NC}"
echo ""
if [ -d "modules/uuid-manager" ]; then
    echo "Merging uuid-manager into UUIDCreator..."

    # Count files
    UUID_MANAGER_FILES=$(find modules/uuid-manager -type f | wc -l | tr -d ' ')
    UUIDCREATOR_FILES=$(find modules/UUIDCreator -type f | wc -l | tr -d ' ')

    echo "  Files in uuid-manager: $UUID_MANAGER_FILES"
    echo "  Files in UUIDCreator: $UUIDCREATOR_FILES"

    # Merge files (don't overwrite existing files in UUIDCreator)
    rsync -a --ignore-existing modules/uuid-manager/ modules/UUIDCreator/

    # Move uuid-manager to archive
    mv modules/uuid-manager "archive/uuid-manager-merged-${BACKUP_TIMESTAMP}/"

    echo "${GREEN}✓ uuid-manager merged into UUIDCreator and archived${NC}"
else
    echo "${GREEN}✓ No uuid-manager folder found${NC}"
fi
echo ""

echo "${BLUE}=== STEP 4: Merge Root Folders ===${NC}"
echo ""
echo "Merging root-level module folders into modules/..."
echo ""

# Function to safely merge folder
merge_folder() {
    local source="$1"
    local destination="$2"
    local description="$3"

    if [ -d "$source" ]; then
        echo -n "  ${YELLOW}$description${NC}: "

        # Create destination if it doesn't exist
        mkdir -p "$destination"

        # Merge files (don't overwrite existing)
        rsync -a --ignore-existing "$source/" "$destination/"

        # Count merged files
        local count=$(find "$source" -type f | wc -l | tr -d ' ')

        # Move original to archive
        mv "$source" "archive/${source##*/}-merged-${BACKUP_TIMESTAMP}/"

        echo "${GREEN}✓ Merged $count files${NC}"
    else
        echo "  ${YELLOW}$description${NC}: ${BLUE}Already merged or doesn't exist${NC}"
    fi
}

merge_folder "voice-accessibility" "modules/VoiceOSCore/accessibility" "voice-accessibility → VoiceOSCore/accessibility"
merge_folder "voice-cursor" "modules/VoiceCursor" "voice-cursor → VoiceCursor"
merge_folder "device-manager" "modules/DeviceManager" "device-manager → DeviceManager"
merge_folder "speech-recognition" "modules/SpeechRecognition" "speech-recognition → SpeechRecognition"
merge_folder "data-manager" "modules/VoiceDataManager" "data-manager → VoiceDataManager"
merge_folder "magicui" "modules/MagicUI" "magicui → MagicUI"
echo ""

echo "${BLUE}=== STEP 5: Verify Final Structure ===${NC}"
echo ""
echo "Verifying reorganization..."
echo ""

# Count modules in correct structure
MODULE_COUNT=$(ls -d modules/*/ 2>/dev/null | wc -l | tr -d ' ')
echo "  Modules in /modules/: $MODULE_COUNT (should be 20)"

# Check for PascalCase naming
echo ""
echo "  Checking PascalCase compliance..."
NON_COMPLIANT=$(ls modules/ 2>/dev/null | grep -E "^[a-z]|.*-.*|.*_.*" | wc -l | tr -d ' ')
if [ "$NON_COMPLIANT" -eq 0 ]; then
    echo "  ${GREEN}✓ All module folders use correct naming${NC}"
else
    echo "  ${RED}⚠ $NON_COMPLIANT folders still using incorrect naming${NC}"
    ls modules/ | grep -E "^[a-z]|.*-.*|.*_.*"
fi

# Check for remaining root-level module folders
echo ""
echo "  Checking for remaining root-level module folders..."
ROOT_MODULES=$(ls -d voice-* speech-* device-* data-* magicui 2>/dev/null | wc -l | tr -d ' ')
if [ "$ROOT_MODULES" -eq 0 ]; then
    echo "  ${GREEN}✓ All root-level module folders merged${NC}"
else
    echo "  ${RED}⚠ $ROOT_MODULES root-level module folders remain${NC}"
    ls -d voice-* speech-* device-* data-* magicui 2>/dev/null
fi

echo ""
echo "${GREEN}==========================================${NC}"
echo "${GREEN}  REORGANIZATION COMPLETE${NC}"
echo "${GREEN}==========================================${NC}"
echo ""
echo "Summary:"
echo "  - 19 module folders renamed to PascalCase"
echo "  - 1 folder merged and archived (uuid-manager)"
echo "  - 6 root folders merged into modules/"
echo "  - Backup location: $BACKUP_DIR"
echo ""
echo "Next steps:"
echo "  1. Run update-references.sh to update file references"
echo "  2. Verify documentation links work correctly"
echo "  3. Commit changes to git"
echo ""

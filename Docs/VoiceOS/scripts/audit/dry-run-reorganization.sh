#!/bin/bash
#
# Filename: dry-run-reorganization.sh
# Created: 2025-10-15 12:06:03 PDT
# Purpose: DRY RUN - Show what WOULD change without actually changing anything
# Usage: ./dry-run-reorganization.sh
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
echo "  DRY RUN - NO CHANGES WILL BE MADE"
echo "=========================================="
echo ""

# Change to docs directory
cd "/Volumes/M Drive/Coding/vos4/docs" || exit 1

echo "${BLUE}=== STEP 1: Module Folder Renames ===${NC}"
echo ""
echo "The following folders WOULD BE RENAMED:"
echo ""

# Apps
echo "${YELLOW}Apps (5):${NC}"
[ -d "modules/learnapp" ] && echo "  ✓ modules/learnapp → modules/LearnApp"
[ -d "modules/voice-cursor" ] && echo "  ✓ modules/voice-cursor → modules/VoiceCursor"
[ -d "modules/voiceos-core" ] && echo "  ✓ modules/voiceos-core → modules/VoiceOSCore"
[ -d "modules/voice-recognition" ] && echo "  ✓ modules/voice-recognition → modules/VoiceRecognition"
[ -d "modules/voice-ui" ] && echo "  ✓ modules/voice-ui → modules/VoiceUI"

echo ""
echo "${YELLOW}Libraries (9):${NC}"
[ -d "modules/device-manager" ] && echo "  ✓ modules/device-manager → modules/DeviceManager"
[ -d "modules/magicelements" ] && echo "  ✓ modules/magicelements → modules/MagicElements"
[ -d "modules/magicui" ] && echo "  ✓ modules/magicui → modules/MagicUI"
[ -d "modules/speech-recognition" ] && echo "  ✓ modules/speech-recognition → modules/SpeechRecognition"
[ -d "modules/translation" ] && echo "  ✓ modules/translation → modules/Translation"
[ -d "modules/uuidcreator" ] && echo "  ✓ modules/uuidcreator → modules/UUIDCreator"
[ -d "modules/voice-keyboard" ] && echo "  ✓ modules/voice-keyboard → modules/VoiceKeyboard"
[ -d "modules/voiceos-logger" ] && echo "  ✓ modules/voiceos-logger → modules/VoiceOsLogger"
[ -d "modules/voice-ui-elements" ] && echo "  ✓ modules/voice-ui-elements → modules/VoiceUIElements"

echo ""
echo "${YELLOW}Managers (5):${NC}"
[ -d "modules/command-manager" ] && echo "  ✓ modules/command-manager → modules/CommandManager"
[ -d "modules/hud-manager" ] && echo "  ✓ modules/hud-manager → modules/HUDManager"
[ -d "modules/license-manager" ] && echo "  ✓ modules/license-manager → modules/LicenseManager"
[ -d "modules/localization-manager" ] && echo "  ✓ modules/localization-manager → modules/LocalizationManager"
[ -d "modules/voice-data-manager" ] && echo "  ✓ modules/voice-data-manager → modules/VoiceDataManager"

echo ""
echo "${BLUE}=== STEP 2: UUID-Manager Handling ===${NC}"
echo ""
if [ -d "modules/uuid-manager" ]; then
    echo "${YELLOW}uuid-manager folder exists${NC}"
    echo "  Files in uuid-manager: $(find modules/uuid-manager -type f | wc -l)"
    echo "  Files in uuidcreator: $(find modules/uuidcreator -type f | wc -l)"
    echo "  ${RED}Action: WOULD MERGE uuid-manager into UUIDCreator and DELETE${NC}"
else
    echo "${GREEN}✓ No uuid-manager folder found${NC}"
fi

echo ""
echo "${BLUE}=== STEP 3: Root Folder Merges ===${NC}"
echo ""
echo "The following root folders WOULD BE MERGED:"
echo ""

[ -d "voice-accessibility" ] && echo "  ✓ voice-accessibility ($(find voice-accessibility -type f -name '*.md' | wc -l) files) → modules/VoiceOSCore/accessibility/"
[ -d "voice-cursor" ] && echo "  ✓ voice-cursor ($(find voice-cursor -type f -name '*.md' | wc -l) files) → modules/VoiceCursor/"
[ -d "device-manager" ] && echo "  ✓ device-manager ($(find device-manager -type f -name '*.md' | wc -l) files) → modules/DeviceManager/"
[ -d "speech-recognition" ] && echo "  ✓ speech-recognition ($(find speech-recognition -type f -name '*.md' | wc -l) files) → modules/SpeechRecognition/"
[ -d "data-manager" ] && echo "  ✓ data-manager ($(find data-manager -type f -name '*.md' | wc -l) files) → modules/VoiceDataManager/"
[ -d "magicui" ] && echo "  ✓ magicui ($(find magicui -type f -name '*.md' | wc -l) files) → modules/MagicUI/"

echo ""
echo "${BLUE}=== STEP 4: File Reference Updates ===${NC}"
echo ""
echo "Counting references that WOULD BE UPDATED..."
cd "/Volumes/M Drive/Coding/vos4"

COUNT_1=$(grep -r "modules/command-manager\|modules/device-manager\|modules/hud-manager\|modules/learnapp\|modules/license-manager" docs --include="*.md" 2>/dev/null | wc -l)
COUNT_2=$(grep -r "modules/localization-manager\|modules/magicelements\|modules/magicui\|modules/speech-recognition\|modules/translation" docs --include="*.md" 2>/dev/null | wc -l)
COUNT_3=$(grep -r "modules/uuidcreator\|modules/uuid-manager\|modules/voice-cursor\|modules/voice-data-manager\|modules/voice-keyboard" docs --include="*.md" 2>/dev/null | wc -l)
COUNT_4=$(grep -r "modules/voice-recognition\|modules/voice-ui\|modules/voice-ui-elements\|modules/voiceos-core\|modules/voiceos-logger" docs --include="*.md" 2>/dev/null | wc -l)
COUNT_5=$(grep -r "docs/voice-accessibility\|docs/voice-cursor\|docs/device-manager\|docs/speech-recognition\|docs/data-manager\|docs/magicui" docs --include="*.md" 2>/dev/null | wc -l)

TOTAL=$((COUNT_1 + COUNT_2 + COUNT_3 + COUNT_4 + COUNT_5))

echo "  ${YELLOW}~$TOTAL references WOULD BE UPDATED${NC}"
echo ""
echo "  Sample references:"
grep -r "modules/command-manager" docs --include="*.md" 2>/dev/null | head -3 | while read line; do
    echo "    $line"
done

echo ""
echo "${BLUE}=== VERIFICATION ===${NC}"
echo ""
cd "/Volumes/M Drive/Coding/vos4/docs"

echo "Current structure:"
echo "  Root module folders: $(ls -d voice-* speech-* device-* data-* magicui 2>/dev/null | wc -l) (should be 6)"
echo "  Module folders: $(ls modules/ 2>/dev/null | wc -l) (should be 20)"
echo ""
echo "After reorganization:"
echo "  Root module folders: 0 (all moved to modules/)"
echo "  Module folders: 20 (all with PascalCase names)"

echo ""
echo "${GREEN}=========================================="
echo "  DRY RUN COMPLETE"
echo "  NO CHANGES WERE MADE"
echo "==========================================${NC}"
echo ""
echo "Summary:"
echo "  - 19 module folders would be renamed"
echo "  - 1 folder would be merged/deleted (uuid-manager)"
echo "  - 6 root folders would be merged"
echo "  - ~$TOTAL file references would be updated"
echo ""
echo "To execute for real, run: ./execute-reorganization.sh"

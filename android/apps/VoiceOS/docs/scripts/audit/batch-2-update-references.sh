#!/bin/bash
# Batch 2: Remaining Libraries + Managers module references

cd "/Volumes/M Drive/Coding/vos4/docs" || exit 1

echo "Starting Batch 2 reference update..."
COUNTER=0

# Get all markdown files once
find . -name "*.md" > /tmp/md_files_batch2.txt

# Process each file once with all Batch 2 replacements
while IFS= read -r file; do
    sed -i '' \
        -e 's|modules/uuidcreator|modules/UUIDCreator|g' \
        -e 's|modules/uuid-manager|modules/UUIDCreator|g' \
        -e 's|modules/voice-keyboard|modules/VoiceKeyboard|g' \
        -e 's|modules/voiceos-logger|modules/VoiceOsLogger|g' \
        -e 's|modules/voice-ui-elements|modules/VoiceUIElements|g' \
        -e 's|modules/command-manager|modules/CommandManager|g' \
        -e 's|modules/hud-manager|modules/HUDManager|g' \
        -e 's|modules/license-manager|modules/LicenseManager|g' \
        -e 's|modules/localization-manager|modules/LocalizationManager|g' \
        -e 's|modules/voice-data-manager|modules/VoiceDataManager|g' \
        "$file"

    COUNTER=$((COUNTER + 1))
    if [ $((COUNTER % 500)) -eq 0 ]; then
        echo "Processed $COUNTER files (Batch 2 of 3)..."
    fi
done < /tmp/md_files_batch2.txt

echo "✓ Batch 2 complete: Libraries (5) + Managers (5) = 10 updates"
echo "Files processed: $COUNTER"

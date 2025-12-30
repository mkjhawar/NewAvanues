#!/bin/bash
# Batch 3: Root folder references

cd "/Volumes/M Drive/Coding/vos4/docs" || exit 1

echo "Starting Batch 3 reference update..."
COUNTER=0

# Get all markdown files once
find . -name "*.md" > /tmp/md_files_batch3.txt

# Process each file once with all Batch 3 replacements
while IFS= read -r file; do
    sed -i '' \
        -e 's|docs/voice-accessibility|docs/modules/VoiceOSCore/accessibility|g' \
        -e 's|docs/voice-cursor|docs/modules/VoiceCursor|g' \
        -e 's|docs/device-manager|docs/modules/DeviceManager|g' \
        -e 's|docs/speech-recognition|docs/modules/SpeechRecognition|g' \
        -e 's|docs/data-manager|docs/modules/VoiceDataManager|g' \
        -e 's|docs/magicui|docs/modules/MagicUI|g' \
        "$file"

    COUNTER=$((COUNTER + 1))
    if [ $((COUNTER % 500)) -eq 0 ]; then
        echo "Processed $COUNTER files (Batch 3 of 3)..."
    fi
done < /tmp/md_files_batch3.txt

echo "✓ Batch 3 complete: Root folders (6) = 6 updates"
echo "Files processed: $COUNTER"

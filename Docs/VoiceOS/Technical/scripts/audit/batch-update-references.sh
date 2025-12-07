#!/bin/bash
# Fast batch reference updater - single pass through all files

cd "/Volumes/M Drive/Coding/vos4/docs" || exit 1

echo "Starting batch reference update..."
COUNTER=0

# Get all markdown files once
find . -type f -name "*.md" > /tmp/md_files.txt

# Process each file once with all replacements
while IFS= read -r file; do
    sed -i '' \
        -e 's|modules/learnapp|modules/LearnApp|g' \
        -e 's|modules/voice-cursor|modules/VoiceCursor|g' \
        -e 's|modules/voiceos-core|modules/VoiceOSCore|g' \
        -e 's|modules/voice-recognition|modules/VoiceRecognition|g' \
        -e 's|modules/voice-ui|modules/VoiceUI|g' \
        -e 's|modules/device-manager|modules/DeviceManager|g' \
        -e 's|modules/magicelements|modules/MagicElements|g' \
        -e 's|modules/magicui|modules/MagicUI|g' \
        -e 's|modules/speech-recognition|modules/SpeechRecognition|g' \
        -e 's|modules/translation|modules/Translation|g' \
        "$file"

    COUNTER=$((COUNTER + 1))
    if [ $((COUNTER % 500)) -eq 0 ]; then
        echo "Processed $COUNTER files (Batch 1 of 3)..."
    fi
done < /tmp/md_files.txt

echo "✓ Batch 1 complete: Apps (5) + Libraries (5) = 10 updates"
echo "Files processed: $COUNTER"

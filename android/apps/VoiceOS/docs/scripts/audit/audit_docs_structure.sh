#!/bin/bash

# Standard structure that each module should have
STANDARD_FOLDERS=(
    "architecture"
    "changelog"
    "developer-manual"
    "diagrams"
    "implementation"
    "module-standards"
    "project-management"
    "reference"
    "reference/api"
    "roadmap"
    "status"
    "testing"
    "user-manual"
)

# Expected module docs folders based on code modules
EXPECTED_MODULES=(
    "learnapp"
    "voice-accessibility"
    "voice-cursor"
    "voice-recognition"
    "voice-ui"
    "device-manager"
    "speech-recognition"
    "translation"
    "uuidcreator"
    "voice-keyboard"
    "voiceos-logger"
    "voice-ui-elements"
    "command-manager"
    "hud-manager"
    "license-manager"
    "localization-manager"
    "voice-data-manager"
)

DOCS_PATH="/Volumes/M Drive/Coding/vos4/docs/modules"

echo "=== MODULE DOCUMENTATION STRUCTURE AUDIT ==="
echo

# Check for missing modules
echo "=== MISSING MODULE DOCUMENTATION FOLDERS ==="
for module in "${EXPECTED_MODULES[@]}"; do
    if [ ! -d "$DOCS_PATH/$module" ]; then
        echo "❌ MISSING: $module"
    fi
done
echo

# Check for extra/duplicate folders
echo "=== EXTRA/DUPLICATE DOCUMENTATION FOLDERS ==="
for folder in "$DOCS_PATH"/*; do
    if [ -d "$folder" ]; then
        basename_folder=$(basename "$folder")
        found=false
        for module in "${EXPECTED_MODULES[@]}"; do
            if [ "$module" = "$basename_folder" ]; then
                found=true
                break
            fi
        done
        if [ "$found" = false ]; then
            echo "⚠️  EXTRA: $basename_folder (not a current module)"
        fi
    fi
done
echo

# Check structure of each module
echo "=== MODULE FOLDER STRUCTURE COMPLIANCE ==="
for module in "${EXPECTED_MODULES[@]}"; do
    if [ -d "$DOCS_PATH/$module" ]; then
        echo "📁 $module"
        missing_folders=()
        for folder in "${STANDARD_FOLDERS[@]}"; do
            if [ ! -d "$DOCS_PATH/$module/$folder" ]; then
                missing_folders+=("$folder")
            fi
        done
        
        if [ ${#missing_folders[@]} -eq 0 ]; then
            echo "   ✅ COMPLETE - All standard folders present"
        else
            echo "   ❌ MISSING: ${missing_folders[*]}"
        fi
    fi
done

#!/bin/bash

# AVAMAGIC RENDERER UPDATE SCRIPT
# Updates Android, iOS, Desktop renderer imports to use new AvaMagic structure
# Version: 1.0
# Date: 2025-11-23

set -e

echo "========================================="
echo "AVAMAGIC RENDERER IMPORT UPDATES"
echo "========================================="
echo ""

BASE_DIR="/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements"
UPDATED_COUNT=0

echo "Step 1: Update Android Renderer Imports"
echo "---------------------------------------------"

ANDROID_DIR="$BASE_DIR/Renderers/Android/src/androidMain/kotlin"

if [ -d "$ANDROID_DIR" ]; then
    # Update package declarations
    find "$ANDROID_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|package com\.augmentalis\.magicelements\.renderers\.android|package com.augmentalis.AvaMagic.renderers.android|g' {} \;

    # Update imports
    find "$ANDROID_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.magicelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    find "$ANDROID_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.avaelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    # Update component references
    find "$ANDROID_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicTagComponent|Chip|g' {} \;

    find "$ANDROID_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicButton|Button|g' {} \;

    COUNT=$(find "$ANDROID_DIR" -name "*.kt" -type f | wc -l | tr -d ' ')
    echo "Updated $COUNT Android renderer files"
    UPDATED_COUNT=$((UPDATED_COUNT + COUNT))
else
    echo "Android renderer directory not found"
fi

echo ""
echo "Step 2: Update iOS Renderer Imports"
echo "---------------------------------------------"

IOS_DIR="$BASE_DIR/Renderers/iOS/src/iosMain/kotlin"

if [ -d "$IOS_DIR" ]; then
    # Update package declarations
    find "$IOS_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|package com\.augmentalis\.magicelements\.renderer\.ios|package com.augmentalis.AvaMagic.renderers.ios|g' {} \;

    # Update imports
    find "$IOS_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.magicelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    find "$IOS_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.avaelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    # Update component references
    find "$IOS_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicTagComponent|Chip|g' {} \;

    find "$IOS_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicButton|Button|g' {} \;

    COUNT=$(find "$IOS_DIR" -name "*.kt" -type f | wc -l | tr -d ' ')
    echo "Updated $COUNT iOS renderer files"
    UPDATED_COUNT=$((UPDATED_COUNT + COUNT))
else
    echo "iOS renderer directory not found"
fi

echo ""
echo "Step 3: Update Desktop Renderer Imports"
echo "---------------------------------------------"

DESKTOP_DIR="$BASE_DIR/Renderers/Desktop/src/desktopMain/kotlin"

if [ -d "$DESKTOP_DIR" ]; then
    # Update package declarations
    find "$DESKTOP_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|package com\.augmentalis\.magicelements\.renderer\.desktop|package com.augmentalis.AvaMagic.renderers.desktop|g' {} \;

    # Update imports
    find "$DESKTOP_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.magicelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    find "$DESKTOP_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.avaelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    # Update component references
    find "$DESKTOP_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicTagComponent|Chip|g' {} \;

    find "$DESKTOP_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|MagicButton|Button|g' {} \;

    COUNT=$(find "$DESKTOP_DIR" -name "*.kt" -type f | wc -l | tr -d ' ')
    echo "Updated $COUNT Desktop renderer files"
    UPDATED_COUNT=$((UPDATED_COUNT + COUNT))
else
    echo "Desktop renderer directory not found"
fi

echo ""
echo "Step 4: Update State Management Imports"
echo "---------------------------------------------"

STATE_DIR="$BASE_DIR/StateManagement/src/commonMain/kotlin"

if [ -d "$STATE_DIR" ]; then
    # Update imports
    find "$STATE_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.magicelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    find "$STATE_DIR" -name "*.kt" -type f -exec sed -i '' \
        's|import com\.augmentalis\.avaelements\.components|import com.augmentalis.AvaMagic.elements|g' {} \;

    COUNT=$(find "$STATE_DIR" -name "*.kt" -type f | wc -l | tr -d ' ')
    echo "Updated $COUNT State Management files"
    UPDATED_COUNT=$((UPDATED_COUNT + COUNT))
else
    echo "State Management directory not found"
fi

echo ""
echo "========================================="
echo "RENDERER UPDATE SUMMARY"
echo "========================================="
echo "Total files updated: $UPDATED_COUNT"
echo ""
echo "Next steps:"
echo "1. Review changes with git diff"
echo "2. Update build.gradle.kts files"
echo "3. Run tests: ./gradlew test"
echo "4. Build project: ./gradlew build"
echo ""
echo "Update complete!"

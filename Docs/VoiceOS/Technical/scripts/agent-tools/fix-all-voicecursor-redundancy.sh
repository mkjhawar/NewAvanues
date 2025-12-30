#!/bin/bash

# Fix ALL VoiceCursor redundancy issues
# Problems:
# 1. Path: /VoiceCursor/src/.../cursor/ - "cursor" repeated
# 2. Files: VoiceCursorSettingsActivity.kt - "VoiceCursor" when in cursor package
# 3. Classes: VoiceCursorCommandHandler - redundant when already in cursor context

echo "=== Fixing ALL VoiceCursor redundancy issues ==="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S %Z')"

BASE_DIR="/Volumes/M Drive/Coding/vos4"
CURSOR_DIR="$BASE_DIR/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor"

# Step 1: Rename files to remove redundant "VoiceCursor" prefix
echo ""
echo "Step 1: Renaming files to remove redundancy..."

declare -A FILE_RENAMES=(
    ["VoiceCursorSettingsActivity.kt"]="SettingsActivity.kt"
    ["VoiceCursorCommandHandler.kt"]="CommandHandler.kt"
    ["VoiceCursorOverlayService.kt"]="OverlayService.kt"
    ["VoiceCursorAccessibilityService.kt"]="AccessibilityService.kt"
    ["VoiceCursorInitializer.kt"]="Initializer.kt"
)

for old_name in "${!FILE_RENAMES[@]}"; do
    new_name="${FILE_RENAMES[$old_name]}"
    old_path=$(find "$CURSOR_DIR" -name "$old_name" -type f 2>/dev/null | head -1)
    
    if [ -n "$old_path" ]; then
        new_path="${old_path%/*}/$new_name"
        echo "  Renaming: $old_name -> $new_name"
        mv "$old_path" "$new_path"
    fi
done

# Step 2: Update class names in the renamed files
echo ""
echo "Step 2: Updating class names inside files..."

# Update class declarations
find "$CURSOR_DIR" -name "*.kt" -type f -exec sed -i '' \
    -e 's/class VoiceCursorSettingsActivity/class SettingsActivity/g' \
    -e 's/class VoiceCursorCommandHandler/class CommandHandler/g' \
    -e 's/class VoiceCursorOverlayService/class OverlayService/g' \
    -e 's/class VoiceCursorAccessibilityService/class AccessibilityService/g' \
    -e 's/class VoiceCursorInitializer/class Initializer/g' \
    -e 's/object VoiceCursorCommandHandler/object CommandHandler/g' {} \;

# Step 3: Update all imports across the entire codebase
echo ""
echo "Step 3: Updating imports across entire codebase..."

# Update imports for renamed classes
find "$BASE_DIR" -name "*.kt" -type f -exec sed -i '' \
    -e 's/import com\.augmentalis\.voiceos\.cursor\.ui\.VoiceCursorSettingsActivity/import com.augmentalis.voiceos.cursor.ui.SettingsActivity/g' \
    -e 's/import com\.augmentalis\.voiceos\.cursor\.commands\.VoiceCursorCommandHandler/import com.augmentalis.voiceos.cursor.commands.CommandHandler/g' \
    -e 's/import com\.augmentalis\.voiceos\.cursor\.service\.VoiceCursorOverlayService/import com.augmentalis.voiceos.cursor.service.OverlayService/g' \
    -e 's/import com\.augmentalis\.voiceos\.cursor\.service\.VoiceCursorAccessibilityService/import com.augmentalis.voiceos.cursor.service.AccessibilityService/g' \
    -e 's/import com\.augmentalis\.voiceos\.cursor\.VoiceCursorInitializer/import com.augmentalis.voiceos.cursor.Initializer/g' {} \;

# Step 4: Update references to the classes
echo ""
echo "Step 4: Updating class references..."

find "$BASE_DIR" -name "*.kt" -type f -exec sed -i '' \
    -e 's/VoiceCursorSettingsActivity/SettingsActivity/g' \
    -e 's/VoiceCursorCommandHandler/CommandHandler/g' \
    -e 's/VoiceCursorOverlayService/OverlayService/g' \
    -e 's/VoiceCursorAccessibilityService/AccessibilityService/g' \
    -e 's/VoiceCursorInitializer/Initializer/g' {} \;

# Step 5: Fix Android manifest references
echo ""
echo "Step 5: Updating AndroidManifest.xml references..."

MANIFEST="$BASE_DIR/apps/VoiceCursor/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST" ]; then
    sed -i '' \
        -e 's/\.VoiceCursorSettingsActivity/\.SettingsActivity/g' \
        -e 's/\.VoiceCursorOverlayService/\.OverlayService/g' \
        -e 's/\.VoiceCursorAccessibilityService/\.AccessibilityService/g' \
        "$MANIFEST"
    echo "  Updated AndroidManifest.xml"
fi

# Step 6: Update any XML layouts that might reference these
echo ""
echo "Step 6: Checking for XML layout references..."

find "$BASE_DIR/apps/VoiceCursor/src/main/res" -name "*.xml" -type f -exec sed -i '' \
    -e 's/VoiceCursorSettingsActivity/SettingsActivity/g' \
    -e 's/VoiceCursorOverlayService/OverlayService/g' \
    -e 's/VoiceCursorAccessibilityService/AccessibilityService/g' {} \; 2>/dev/null

# Step 7: Fix remaining "Cursor" redundancy in other file names
echo ""
echo "Step 7: Fixing other redundant 'Cursor' names..."

declare -A MORE_RENAMES=(
    ["CursorView.kt"]="View.kt"
    ["CursorMenuView.kt"]="MenuView.kt"
    ["CursorActions.kt"]="Actions.kt"
    ["CursorRenderer.kt"]="Renderer.kt"
    ["CursorPositionManager.kt"]="PositionManager.kt"
    ["CursorTypes.kt"]="Types.kt"
    ["CursorHelper.kt"]="Helper.kt"
)

for old_name in "${!MORE_RENAMES[@]}"; do
    new_name="${MORE_RENAMES[$old_name]}"
    old_path=$(find "$CURSOR_DIR" -name "$old_name" -type f 2>/dev/null | head -1)
    
    if [ -n "$old_path" ]; then
        new_path="${old_path%/*}/$new_name"
        echo "  Renaming: $old_name -> $new_name"
        mv "$old_path" "$new_path"
        
        # Update class name inside file
        base_old="${old_name%.kt}"
        base_new="${new_name%.kt}"
        sed -i '' "s/class $base_old/class $base_new/g" "$new_path"
        sed -i '' "s/object $base_old/object $base_new/g" "$new_path"
    fi
done

# Step 8: Update all references to these renamed classes
echo ""
echo "Step 8: Updating references to all renamed classes..."

find "$BASE_DIR" -name "*.kt" -type f -exec sed -i '' \
    -e 's/CursorView/View/g' \
    -e 's/CursorMenuView/MenuView/g' \
    -e 's/CursorActions/Actions/g' \
    -e 's/CursorRenderer/Renderer/g' \
    -e 's/CursorPositionManager/PositionManager/g' \
    -e 's/CursorTypes/Types/g' \
    -e 's/CursorHelper/Helper/g' {} \;

# Step 9: Fix imports for the renamed classes
echo ""
echo "Step 9: Fixing imports for renamed classes..."

find "$BASE_DIR" -name "*.kt" -type f -exec sed -i '' \
    -e 's/\.view\.CursorView/\.view\.View/g' \
    -e 's/\.view\.CursorMenuView/\.view\.MenuView/g' \
    -e 's/\.actions\.CursorActions/\.actions\.Actions/g' \
    -e 's/\.core\.CursorRenderer/\.core\.Renderer/g' \
    -e 's/\.core\.CursorPositionManager/\.core\.PositionManager/g' \
    -e 's/\.core\.CursorTypes/\.core\.Types/g' \
    -e 's/\.helper\.CursorHelper/\.helper\.Helper/g' {} \;

# Step 10: List all changes
echo ""
echo "Step 10: Summary of changes..."
echo "  - Removed 'VoiceCursor' prefix from class names in cursor package"
echo "  - Removed 'Cursor' prefix from class names already in cursor context"
echo "  - Updated all imports and references"
echo "  - Fixed AndroidManifest.xml"
echo ""
echo "=== Redundancy fixes complete! ==="
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S %Z')"
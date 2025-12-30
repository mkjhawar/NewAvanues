#!/bin/bash

# AVAMAGIC CLEAN PACKAGE RESTRUCTURE
# Automated migration script to restructure AvaElements to clean AvaMagic package structure
# Version: 1.0
# Date: 2025-11-23

set -e

echo "========================================="
echo "AVAMAGIC PACKAGE RESTRUCTURE"
echo "========================================="
echo ""

# Define base paths
BASE_DIR="/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements"
CORE_SRC="$BASE_DIR/Core/src/commonMain/kotlin/com/augmentalis"
OLD_PKG_CORE="$CORE_SRC/magicelements"
OLD_PKG_UNIFIED="$BASE_DIR/components/unified/src/commonMain/kotlin/com/augmentalis/avaelements"
NEW_PKG="$CORE_SRC/AvaMagic"

# Statistics
MOVED_COUNT=0
UPDATED_COUNT=0
ERROR_COUNT=0

echo "Step 1: Component Categorization and Mapping"
echo "---------------------------------------------"

# Define component mappings: source_file -> destination_category
declare -A COMPONENT_MAP=(
    # Tags/Chips (from data/MagicTag.kt)
    ["data/MagicTag.kt"]="elements/tags/Chip.kt"

    # Buttons (will come from unified)
    # MagicButton -> Button

    # Cards
    ["data/Paper.kt"]="elements/cards/Card.kt"

    # Inputs (from form/)
    ["form/DatePicker.kt"]="elements/inputs/DatePicker.kt"
    ["form/Dropdown.kt"]="elements/inputs/Dropdown.kt"
    ["form/FileUpload.kt"]="elements/inputs/FileUpload.kt"
    ["form/Radio.kt"]="elements/inputs/Radio.kt"
    ["form/Rating.kt"]="elements/inputs/Rating.kt"
    ["form/SearchBar.kt"]="elements/inputs/SearchBar.kt"
    ["form/Slider.kt"]="elements/inputs/Slider.kt"
    ["form/TimePicker.kt"]="elements/inputs/TimePicker.kt"

    # Display (from data/)
    ["data/Avatar.kt"]="elements/display/Avatar.kt"
    ["data/Skeleton.kt"]="elements/display/Skeleton.kt"
    ["data/Divider.kt"]="elements/display/Divider.kt"
    ["data/EmptyState.kt"]="elements/display/EmptyState.kt"

    # Navigation
    ["navigation/AppBar.kt"]="elements/navigation/AppBar.kt"
    ["navigation/BottomNav.kt"]="elements/navigation/BottomNav.kt"
    ["navigation/Breadcrumb.kt"]="elements/navigation/Breadcrumb.kt"
    ["navigation/Drawer.kt"]="elements/navigation/Drawer.kt"
    ["navigation/Pagination.kt"]="elements/navigation/Pagination.kt"
    ["navigation/Tabs.kt"]="elements/navigation/Tabs.kt"

    # Feedback
    ["feedback/Alert.kt"]="elements/feedback/Alert.kt"
    ["feedback/Badge.kt"]="elements/feedback/Badge.kt"
    ["feedback/Dialog.kt"]="elements/feedback/Dialog.kt"
    ["feedback/ProgressBar.kt"]="elements/feedback/ProgressBar.kt"
    ["feedback/Spinner.kt"]="elements/feedback/Spinner.kt"
    ["feedback/Toast.kt"]="elements/feedback/Toast.kt"
    ["feedback/Tooltip.kt"]="elements/feedback/Tooltip.kt"

    # Data components (complex visualizations)
    ["data/Accordion.kt"]="elements/data/Accordion.kt"
    ["data/Carousel.kt"]="elements/data/Carousel.kt"
    ["data/DataGrid.kt"]="elements/data/DataGrid.kt"
    ["data/List.kt"]="elements/data/List.kt"
    ["data/Stepper.kt"]="elements/data/Stepper.kt"
    ["data/Table.kt"]="elements/data/Table.kt"
    ["data/Timeline.kt"]="elements/data/Timeline.kt"
    ["data/TreeView.kt"]="elements/data/TreeView.kt"
)

echo "Mapped ${#COMPONENT_MAP[@]} components for migration"
echo ""

echo "Step 2: Moving Core Components"
echo "---------------------------------------------"

for src_file in "${!COMPONENT_MAP[@]}"; do
    dest_file="${COMPONENT_MAP[$src_file]}"
    src_path="$OLD_PKG_CORE/components/$src_file"
    dest_path="$NEW_PKG/$dest_file"

    if [ -f "$src_path" ]; then
        echo "Moving: $src_file -> $dest_file"

        # Create destination directory if needed
        dest_dir=$(dirname "$dest_path")
        mkdir -p "$dest_dir"

        # Copy file (we'll update package declarations separately)
        cp "$src_path" "$dest_path"
        MOVED_COUNT=$((MOVED_COUNT + 1))
    else
        echo "WARNING: Source file not found: $src_path"
        ERROR_COUNT=$((ERROR_COUNT + 1))
    fi
done

echo ""
echo "Step 3: Moving Unified Components"
echo "---------------------------------------------"

# Move MagicButton to Button
if [ -f "$OLD_PKG_UNIFIED/magic/buttons/MagicButton.kt" ]; then
    echo "Moving: MagicButton -> Button"
    cp "$OLD_PKG_UNIFIED/magic/buttons/MagicButton.kt" "$NEW_PKG/elements/buttons/Button.kt"
    MOVED_COUNT=$((MOVED_COUNT + 1))
fi

# Move layout components
for layout_file in Container.kt Row.kt Column.kt; do
    src="$OLD_PKG_UNIFIED/layout/$layout_file"
    if [ -f "$src" ]; then
        echo "Moving: layout/$layout_file"
        cp "$src" "$NEW_PKG/layout/$layout_file"
        MOVED_COUNT=$((MOVED_COUNT + 1))
    fi
done

echo ""
echo "Step 4: Package Declaration Updates"
echo "---------------------------------------------"

# Function to update package declaration in a file
update_package_declaration() {
    local file=$1
    local new_package=$2

    if [ -f "$file" ]; then
        # Update package declaration
        sed -i '' "s|^package com\.augmentalis\.avaelements\.components\.[^[:space:]]*|package $new_package|" "$file"
        sed -i '' "s|^package com\.augmentalis\.magicelements\.components\.[^[:space:]]*|package $new_package|" "$file"
        sed -i '' "s|^package com\.augmentalis\.avaelements\.magic\.[^[:space:]]*|package $new_package|" "$file"
        sed -i '' "s|^package com\.augmentalis\.avaelements\.layout|package $new_package|" "$file"

        UPDATED_COUNT=$((UPDATED_COUNT + 1))
        echo "Updated: $file"
    fi
}

# Update all element files
for category in tags buttons cards inputs display navigation feedback data; do
    for file in "$NEW_PKG/elements/$category"/*.kt; do
        if [ -f "$file" ]; then
            update_package_declaration "$file" "com.augmentalis.AvaMagic.elements.$category"
        fi
    done
done

# Update layout files
for file in "$NEW_PKG/layout"/*.kt; do
    if [ -f "$file" ]; then
        update_package_declaration "$file" "com.augmentalis.AvaMagic.layout"
    fi
done

echo ""
echo "Step 5: Rename MagicButton class to Button"
echo "---------------------------------------------"

BUTTON_FILE="$NEW_PKG/elements/buttons/Button.kt"
if [ -f "$BUTTON_FILE" ]; then
    # Rename data class from MagicButton to Button
    sed -i '' 's/data class MagicButton/data class Button/' "$BUTTON_FILE"
    # Update type string
    sed -i '' 's/override val type: String = "MagicButton"/override val type: String = "Button"/' "$BUTTON_FILE"
    # Update companion object constructors
    sed -i '' 's/) = MagicButton(/) = Button(/g' "$BUTTON_FILE"
    # Update doc comments
    sed -i '' 's/MagicButton component/Button component/g' "$BUTTON_FILE"
    sed -i '' 's/MagicButton(/Button(/g' "$BUTTON_FILE"

    echo "Renamed MagicButton to Button"
    UPDATED_COUNT=$((UPDATED_COUNT + 1))
fi

echo ""
echo "Step 6: Rename MagicTagComponent to Chip"
echo "---------------------------------------------"

CHIP_FILE="$NEW_PKG/elements/tags/Chip.kt"
if [ -f "$CHIP_FILE" ]; then
    # Rename data class from MagicTagComponent to Chip
    sed -i '' 's/data class MagicTagComponent/data class Chip/' "$CHIP_FILE"
    # Update type string
    sed -i '' 's/override val type: String = "MagicTag"/override val type: String = "Chip"/' "$CHIP_FILE"
    # Update doc comments
    sed -i '' 's/MagicTag Component/Chip Component/g' "$CHIP_FILE"
    sed -i '' 's/MagicTag(/Chip(/g' "$CHIP_FILE"
    sed -i '' 's/magic tag/chip/g' "$CHIP_FILE"
    sed -i '' 's/MagicTag label/Chip label/g' "$CHIP_FILE"

    echo "Renamed MagicTagComponent to Chip"
    UPDATED_COUNT=$((UPDATED_COUNT + 1))
fi

echo ""
echo "Step 7: Update Import Statements"
echo "---------------------------------------------"

# Function to update imports in a file
update_imports() {
    local file=$1

    if [ -f "$file" ]; then
        # Update core imports
        sed -i '' 's|import com\.augmentalis\.avaelements\.core|import com.augmentalis.AvaMagic.core|g' "$file"
        sed -i '' 's|import com\.augmentalis\.magicelements\.core|import com.augmentalis.AvaMagic.core|g' "$file"

        # Update component imports
        sed -i '' 's|import com\.augmentalis\.avaelements\.components|import com.augmentalis.AvaMagic.elements|g' "$file"
        sed -i '' 's|import com\.augmentalis\.magicelements\.components|import com.augmentalis.AvaMagic.elements|g' "$file"

        # Update layout imports
        sed -i '' 's|import com\.augmentalis\.avaelements\.layout|import com.augmentalis.AvaMagic.layout|g' "$file"

        echo "Updated imports: $file"
    fi
}

# Update imports in all AvaMagic files
find "$NEW_PKG" -name "*.kt" -type f | while read file; do
    update_imports "$file"
done

echo ""
echo "========================================="
echo "MIGRATION SUMMARY"
echo "========================================="
echo "Components moved: $MOVED_COUNT"
echo "Files updated: $UPDATED_COUNT"
echo "Errors: $ERROR_COUNT"
echo ""
echo "Next steps:"
echo "1. Run renderer updates (Android, iOS, Desktop, Web)"
echo "2. Update build.gradle.kts files"
echo "3. Create export/index files"
echo "4. Run tests"
echo ""
echo "Migration complete!"

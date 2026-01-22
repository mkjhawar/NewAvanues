#!/bin/bash

# Package Restructure Migration Script
# Migrates Actions module from com.augmentalis.ava.features.actions to com.augmentalis.actions

set -e

BASE_DIR="/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/Actions"
OLD_PKG_PATH="com/augmentalis/ava/features/actions"
NEW_PKG_PATH="com/augmentalis/actions"

echo "=== Actions Package Migration ==="
echo "Base directory: $BASE_DIR"
echo ""

# Step 1: Create new directory structure
echo "Step 1: Creating new directory structure..."
mkdir -p "$BASE_DIR/src/androidMain/kotlin/$NEW_PKG_PATH"
mkdir -p "$BASE_DIR/src/androidTest/kotlin/$NEW_PKG_PATH"
mkdir -p "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/entities"
mkdir -p "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/handlers"
mkdir -p "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/web"
mkdir -p "$BASE_DIR/src/test/kotlin/$NEW_PKG_PATH/entities"
mkdir -p "$BASE_DIR/src/test/kotlin/$NEW_PKG_PATH/handlers"
echo "✓ Directory structure created"
echo ""

# Step 2: Find all Kotlin files
echo "Step 2: Finding all Kotlin files..."
kt_files=$(find "$BASE_DIR" -type f -name "*.kt" -path "*/com/augmentalis/ava/features/actions/*")
file_count=$(echo "$kt_files" | wc -l | tr -d ' ')
echo "Found $file_count Kotlin files to migrate"
echo ""

# Step 3: Update package declarations and imports
echo "Step 3: Updating package declarations and imports..."
count=0
while IFS= read -r file; do
    if [ -f "$file" ]; then
        count=$((count + 1))
        echo "[$count/$file_count] Processing: $(basename $file)"

        # Update package declaration
        sed -i '' 's/^package com\.augmentalis\.ava\.features\.actions/package com.augmentalis.actions/' "$file"

        # Update imports
        sed -i '' 's/import com\.augmentalis\.ava\.features\.actions/import com.augmentalis.actions/g' "$file"
    fi
done <<< "$kt_files"
echo "✓ Updated $count files"
echo ""

# Step 4: Move files to new locations
echo "Step 4: Moving files to new directory structure..."
count=0

# Move androidMain files
if [ -d "$BASE_DIR/src/androidMain/kotlin/$OLD_PKG_PATH" ]; then
    for file in "$BASE_DIR/src/androidMain/kotlin/$OLD_PKG_PATH"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/androidMain/kotlin/$NEW_PKG_PATH/"
            echo "  Moved: $(basename $file) (androidMain)"
        fi
    done
fi

# Move androidTest files
if [ -d "$BASE_DIR/src/androidTest/kotlin/$OLD_PKG_PATH" ]; then
    for file in "$BASE_DIR/src/androidTest/kotlin/$OLD_PKG_PATH"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/androidTest/kotlin/$NEW_PKG_PATH/"
            echo "  Moved: $(basename $file) (androidTest)"
        fi
    done
fi

# Move main files (root level)
if [ -d "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH" ]; then
    for file in "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/"
            echo "  Moved: $(basename $file) (main)"
        fi
    done
fi

# Move main/entities files
if [ -d "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/entities" ]; then
    for file in "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/entities"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/entities/"
            echo "  Moved: $(basename $file) (main/entities)"
        fi
    done
fi

# Move main/handlers files
if [ -d "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/handlers" ]; then
    for file in "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/handlers"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/handlers/"
            echo "  Moved: $(basename $file) (main/handlers)"
        fi
    done
fi

# Move main/web files
if [ -d "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/web" ]; then
    for file in "$BASE_DIR/src/main/kotlin/$OLD_PKG_PATH/web"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/main/kotlin/$NEW_PKG_PATH/web/"
            echo "  Moved: $(basename $file) (main/web)"
        fi
    done
fi

# Move test files (root level)
if [ -d "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH" ]; then
    for file in "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/test/kotlin/$NEW_PKG_PATH/"
            echo "  Moved: $(basename $file) (test)"
        fi
    done
fi

# Move test/entities files
if [ -d "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH/entities" ]; then
    for file in "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH/entities"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/test/kotlin/$NEW_PKG_PATH/entities/"
            echo "  Moved: $(basename $file) (test/entities)"
        fi
    done
fi

# Move test/handlers files
if [ -d "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH/handlers" ]; then
    for file in "$BASE_DIR/src/test/kotlin/$OLD_PKG_PATH/handlers"/*.kt; do
        if [ -f "$file" ]; then
            count=$((count + 1))
            mv "$file" "$BASE_DIR/src/test/kotlin/$NEW_PKG_PATH/handlers/"
            echo "  Moved: $(basename $file) (test/handlers)"
        fi
    done
fi

echo "✓ Moved $count files"
echo ""

# Step 5: Update build.gradle.kts
echo "Step 5: Updating build.gradle.kts namespace..."
sed -i '' 's/namespace = "com\.augmentalis\.ava\.features\.actions"/namespace = "com.augmentalis.actions"/' "$BASE_DIR/build.gradle.kts"
echo "✓ Updated build.gradle.kts"
echo ""

# Step 6: Remove old empty directories
echo "Step 6: Cleaning up old empty directories..."
rm -rf "$BASE_DIR/src/androidMain/kotlin/com/augmentalis/ava"
rm -rf "$BASE_DIR/src/androidTest/kotlin/com/augmentalis/ava"
rm -rf "$BASE_DIR/src/main/kotlin/com/augmentalis/ava"
rm -rf "$BASE_DIR/src/test/kotlin/com/augmentalis/ava"
echo "✓ Old directories removed"
echo ""

# Step 7: Verify migration
echo "Step 7: Verifying migration..."
new_files=$(find "$BASE_DIR" -type f -name "*.kt" -path "*/com/augmentalis/actions/*")
new_count=$(echo "$new_files" | wc -l | tr -d ' ')
echo "Files in new location: $new_count"

old_files=$(find "$BASE_DIR" -type f -name "*.kt" -path "*/com/augmentalis/ava/features/actions/*" 2>/dev/null || true)
old_count=0
if [ -n "$old_files" ]; then
    old_count=$(echo "$old_files" | wc -l | tr -d ' ')
fi
echo "Files in old location: $old_count"
echo ""

if [ "$new_count" -eq "$file_count" ] && [ "$old_count" -eq 0 ]; then
    echo "=== ✓ MIGRATION SUCCESSFUL ==="
    echo "Migrated $new_count files from com.augmentalis.ava.features.actions to com.augmentalis.actions"
else
    echo "=== ⚠ MIGRATION WARNING ==="
    echo "Expected $file_count files, found $new_count in new location and $old_count in old location"
    echo "Please verify manually"
fi

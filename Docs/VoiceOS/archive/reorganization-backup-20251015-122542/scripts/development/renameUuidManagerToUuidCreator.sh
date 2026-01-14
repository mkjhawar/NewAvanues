#!/bin/bash

# File: tools/renameUuidManagerToUuidCreator.sh
# Author: Manoj Jhawar
# Code-Reviewed-By: CCA
# Purpose: Automated namespace and module rename from UUIDManager to UUIDCreator
# Version: 1.0.0
# Created: 2025-10-07

set -e  # Exit on error

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "========================================"
echo "UUIDManager → UUIDCreator Rename Script"
echo "========================================"
echo ""
echo "Project Root: $PROJECT_ROOT"
echo ""

# Confirmation
read -p "This will rename all UUIDManager references to UUIDCreator. Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "Step 1: Creating backup..."
BACKUP_DIR="$PROJECT_ROOT/../vos4-uuidcreator-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -r "$PROJECT_ROOT/modules/libraries/UUIDManager" "$BACKUP_DIR/" 2>/dev/null || true
echo "✓ Backup created at: $BACKUP_DIR"

echo ""
echo "Step 2: Renaming directory UUIDManager → UUIDCreator..."
if [ -d "$PROJECT_ROOT/modules/libraries/UUIDManager" ]; then
    mv "$PROJECT_ROOT/modules/libraries/UUIDManager" "$PROJECT_ROOT/modules/libraries/UUIDCreator"
    echo "✓ Directory renamed"
else
    echo "! Directory not found: modules/libraries/UUIDManager"
fi

echo ""
echo "Step 3: Updating package declarations in Kotlin files..."
# Find all .kt files and replace package declarations
find "$PROJECT_ROOT/modules/libraries/UUIDCreator" -type f -name "*.kt" -print0 | while IFS= read -r -d '' file; do
    if grep -q "package com.augmentalis.uuidmanager" "$file"; then
        sed -i '' 's/package com\.augmentalis\.uuidmanager/package com.augmentalis.uuidcreator/g' "$file"
        echo "  ✓ Updated: $(basename "$file")"
    fi
done

echo ""
echo "Step 4: Updating import statements in Kotlin files..."
# Update imports in all Kotlin files
find "$PROJECT_ROOT/modules/libraries/UUIDCreator" -type f -name "*.kt" -print0 | while IFS= read -r -d '' file; do
    if grep -q "import com.augmentalis.uuidmanager" "$file"; then
        sed -i '' 's/import com\.augmentalis\.uuidmanager/import com.augmentalis.uuidcreator/g' "$file"
        echo "  ✓ Updated imports: $(basename "$file")"
    fi
done

echo ""
echo "Step 5: Updating build.gradle.kts..."
BUILD_GRADLE="$PROJECT_ROOT/modules/libraries/UUIDCreator/build.gradle.kts"
if [ -f "$BUILD_GRADLE" ]; then
    # Update namespace
    sed -i '' 's/namespace = "com\.augmentalis\.uuidmanager"/namespace = "com.augmentalis.uuidcreator"/g' "$BUILD_GRADLE"
    # Update module name references
    sed -i '' 's/"uuidmanager"/"uuidcreator"/g' "$BUILD_GRADLE"
    sed -i '' 's/"uuidmanagerCompose"/"uuidcreatorCompose"/g' "$BUILD_GRADLE"
    sed -i '' 's/"uuidmanagerViews"/"uuidcreatorViews"/g' "$BUILD_GRADLE"
    echo "✓ Updated build.gradle.kts"
else
    echo "! build.gradle.kts not found"
fi

echo ""
echo "Step 6: Updating AndroidManifest.xml..."
MANIFEST="$PROJECT_ROOT/modules/libraries/UUIDCreator/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST" ]; then
    sed -i '' 's/com\.augmentalis\.uuidmanager/com.augmentalis.uuidcreator/g' "$MANIFEST"
    echo "✓ Updated AndroidManifest.xml"
else
    echo "! AndroidManifest.xml not found"
fi

echo ""
echo "Step 7: Updating settings.gradle.kts..."
SETTINGS_GRADLE="$PROJECT_ROOT/settings.gradle.kts"
if [ -f "$SETTINGS_GRADLE" ]; then
    sed -i '' 's/:UUIDManager/:UUIDCreator/g' "$SETTINGS_GRADLE"
    sed -i '' 's/"UUIDManager"/"UUIDCreator"/g' "$SETTINGS_GRADLE"
    echo "✓ Updated settings.gradle.kts"
else
    echo "! settings.gradle.kts not found"
fi

echo ""
echo "Step 8: Updating root build.gradle.kts dependencies..."
ROOT_BUILD_GRADLE="$PROJECT_ROOT/build.gradle.kts"
if [ -f "$ROOT_BUILD_GRADLE" ]; then
    sed -i '' 's/:libraries:UUIDManager/:libraries:UUIDCreator/g' "$ROOT_BUILD_GRADLE"
    sed -i '' 's/:UUIDManager/:UUIDCreator/g' "$ROOT_BUILD_GRADLE"
    echo "✓ Updated root build.gradle.kts"
else
    echo "! Root build.gradle.kts not found"
fi

echo ""
echo "Step 9: Updating all module dependencies..."
# Find all build.gradle.kts files and update UUIDManager dependencies
find "$PROJECT_ROOT/modules" -type f -name "build.gradle.kts" -print0 | while IFS= read -r -d '' file; do
    if grep -q "UUIDManager" "$file"; then
        sed -i '' 's/project(":libraries:UUIDManager")/project(":libraries:UUIDCreator")/g' "$file"
        sed -i '' 's/project(":UUIDManager")/project(":UUIDCreator")/g' "$file"
        echo "  ✓ Updated: $file"
    fi
done

echo ""
echo "Step 10: Updating documentation references..."
# Update docs
find "$PROJECT_ROOT/docs" -type f -name "*.md" -print0 2>/dev/null | while IFS= read -r -d '' file; do
    if grep -q "UUIDManager" "$file" || grep -q "uuidmanager" "$file"; then
        sed -i '' 's/UUIDManager/UUIDCreator/g' "$file"
        sed -i '' 's/uuidmanager/uuidcreator/g' "$file"
        echo "  ✓ Updated: $(basename "$file")"
    fi
done

echo ""
echo "Step 11: Renaming source directory structure..."
# Rename package directory structure
UUID_SRC_DIR="$PROJECT_ROOT/modules/libraries/UUIDCreator/src/main/java/com/augmentalis"
if [ -d "$UUID_SRC_DIR/uuidmanager" ]; then
    mv "$UUID_SRC_DIR/uuidmanager" "$UUID_SRC_DIR/uuidcreator"
    echo "✓ Renamed source directory: uuidmanager → uuidcreator"
else
    echo "! Source directory not found or already renamed"
fi

echo ""
echo "Step 12: Updating class name references (UUIDManager class → UUIDCreator)..."
# Rename the main class file
if [ -f "$UUID_SRC_DIR/uuidcreator/UUIDManager.kt" ]; then
    mv "$UUID_SRC_DIR/uuidcreator/UUIDManager.kt" "$UUID_SRC_DIR/uuidcreator/UUIDCreator.kt"
    # Update class name inside the file
    sed -i '' 's/class UUIDManager/class UUIDCreator/g' "$UUID_SRC_DIR/uuidcreator/UUIDCreator.kt"
    sed -i '' 's/object UUIDManager/object UUIDCreator/g' "$UUID_SRC_DIR/uuidcreator/UUIDCreator.kt"
    echo "✓ Renamed UUIDManager.kt → UUIDCreator.kt"
fi

# Update references to UUIDManager class in all Kotlin files
find "$PROJECT_ROOT/modules/libraries/UUIDCreator" -type f -name "*.kt" -print0 | while IFS= read -r -d '' file; do
    if grep -q "UUIDManager" "$file"; then
        sed -i '' 's/: UUIDManager()/: UUIDCreator()/g' "$file"
        sed -i '' 's/= UUIDManager()/= UUIDCreator()/g' "$file"
        sed -i '' 's/UUIDManager\./UUIDCreator./g' "$file"
        echo "  ✓ Updated class references: $(basename "$file")"
    fi
done

echo ""
echo "========================================"
echo "Rename Complete!"
echo "========================================"
echo ""
echo "Next steps:"
echo "1. Run: ./gradlew clean build"
echo "2. Run tests: ./gradlew :libraries:UUIDCreator:test"
echo "3. Review changes: git diff"
echo "4. Commit: git add . && git commit -m 'refactor: rename UUIDManager to UUIDCreator'"
echo ""
echo "Backup location: $BACKUP_DIR"
echo ""

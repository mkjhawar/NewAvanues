#!/usr/bin/env python3
"""
Package Restructure Migration Script for Actions Module
Migrates from com.augmentalis.ava.features.actions to com.augmentalis.actions
"""

import os
import re
import shutil
from pathlib import Path

# Constants
BASE_DIR = Path("/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/Actions")
OLD_PKG = "com.augmentalis.ava.features.actions"
NEW_PKG = "com.augmentalis.actions"
OLD_PKG_PATH = "com/augmentalis/ava/features/actions"
NEW_PKG_PATH = "com/augmentalis/actions"

def main():
    print("=== Actions Package Migration ===")
    print(f"Base directory: {BASE_DIR}")
    print()

    # Step 1: Find all Kotlin files
    print("Step 1: Finding all Kotlin files...")
    kt_files = list(BASE_DIR.rglob(f"*/{OLD_PKG_PATH}/*.kt"))
    print(f"Found {len(kt_files)} Kotlin files to migrate")
    print()

    # Step 2: Update package declarations and imports in all files
    print("Step 2: Updating package declarations and imports...")
    updated_count = 0
    for kt_file in kt_files:
        try:
            with open(kt_file, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content

            # Update package declaration
            content = re.sub(
                r'^package\s+com\.augmentalis\.ava\.features\.actions',
                f'package {NEW_PKG}',
                content,
                flags=re.MULTILINE
            )

            # Update imports
            content = re.sub(
                r'import\s+com\.augmentalis\.ava\.features\.actions',
                f'import {NEW_PKG}',
                content
            )

            if content != original_content:
                with open(kt_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                updated_count += 1
                print(f"  [{updated_count}/{len(kt_files)}] Updated: {kt_file.name}")

        except Exception as e:
            print(f"  ERROR processing {kt_file.name}: {e}")

    print(f"✓ Updated {updated_count} files")
    print()

    # Step 3: Create new directory structure
    print("Step 3: Creating new directory structure...")
    new_dirs = [
        BASE_DIR / f"src/androidMain/kotlin/{NEW_PKG_PATH}",
        BASE_DIR / f"src/androidTest/kotlin/{NEW_PKG_PATH}",
        BASE_DIR / f"src/main/kotlin/{NEW_PKG_PATH}/entities",
        BASE_DIR / f"src/main/kotlin/{NEW_PKG_PATH}/handlers",
        BASE_DIR / f"src/main/kotlin/{NEW_PKG_PATH}/web",
        BASE_DIR / f"src/test/kotlin/{NEW_PKG_PATH}/entities",
        BASE_DIR / f"src/test/kotlin/{NEW_PKG_PATH}/handlers",
    ]

    for new_dir in new_dirs:
        new_dir.mkdir(parents=True, exist_ok=True)
    print("✓ Directory structure created")
    print()

    # Step 4: Move files to new locations
    print("Step 4: Moving files to new directory structure...")
    moved_count = 0

    source_sets = [
        ("src/androidMain/kotlin", "androidMain"),
        ("src/androidTest/kotlin", "androidTest"),
        ("src/main/kotlin", "main"),
        ("src/test/kotlin", "test"),
    ]

    for source_set, name in source_sets:
        old_base = BASE_DIR / source_set / OLD_PKG_PATH
        new_base = BASE_DIR / source_set / NEW_PKG_PATH

        if not old_base.exists():
            continue

        # Move root level files
        for kt_file in old_base.glob("*.kt"):
            dest = new_base / kt_file.name
            shutil.move(str(kt_file), str(dest))
            moved_count += 1
            print(f"  Moved: {kt_file.name} ({name})")

        # Move subdirectory files (entities, handlers, web)
        for subdir in ["entities", "handlers", "web"]:
            old_subdir = old_base / subdir
            new_subdir = new_base / subdir

            if not old_subdir.exists():
                continue

            for kt_file in old_subdir.glob("*.kt"):
                dest = new_subdir / kt_file.name
                shutil.move(str(kt_file), str(dest))
                moved_count += 1
                print(f"  Moved: {kt_file.name} ({name}/{subdir})")

    print(f"✓ Moved {moved_count} files")
    print()

    # Step 5: Update build.gradle.kts
    print("Step 5: Updating build.gradle.kts namespace...")
    build_gradle = BASE_DIR / "build.gradle.kts"

    try:
        with open(build_gradle, 'r', encoding='utf-8') as f:
            content = f.read()

        content = re.sub(
            r'namespace\s*=\s*"com\.augmentalis\.ava\.features\.actions"',
            f'namespace = "{NEW_PKG}"',
            content
        )

        with open(build_gradle, 'w', encoding='utf-8') as f:
            f.write(content)

        print("✓ Updated build.gradle.kts")
    except Exception as e:
        print(f"ERROR updating build.gradle.kts: {e}")
    print()

    # Step 6: Remove old empty directories
    print("Step 6: Cleaning up old empty directories...")
    old_roots = [
        BASE_DIR / "src/androidMain/kotlin/com/augmentalis/ava",
        BASE_DIR / "src/androidTest/kotlin/com/augmentalis/ava",
        BASE_DIR / "src/main/kotlin/com/augmentalis/ava",
        BASE_DIR / "src/test/kotlin/com/augmentalis/ava",
    ]

    for old_root in old_roots:
        if old_root.exists():
            try:
                shutil.rmtree(old_root)
                print(f"  Removed: {old_root.relative_to(BASE_DIR)}")
            except Exception as e:
                print(f"  ERROR removing {old_root.relative_to(BASE_DIR)}: {e}")

    print("✓ Old directories removed")
    print()

    # Step 7: Verify migration
    print("Step 7: Verifying migration...")
    new_files = list(BASE_DIR.rglob(f"*/{NEW_PKG_PATH}/*.kt"))
    old_files = list(BASE_DIR.rglob(f"*/{OLD_PKG_PATH}/*.kt"))

    print(f"Files in new location: {len(new_files)}")
    print(f"Files in old location: {len(old_files)}")
    print()

    if len(new_files) == len(kt_files) and len(old_files) == 0:
        print("=== ✓ MIGRATION SUCCESSFUL ===")
        print(f"Migrated {len(new_files)} files from {OLD_PKG} to {NEW_PKG}")
    else:
        print("=== ⚠ MIGRATION WARNING ===")
        print(f"Expected {len(kt_files)} files, found {len(new_files)} in new location and {len(old_files)} in old location")
        print("Please verify manually")

if __name__ == "__main__":
    main()

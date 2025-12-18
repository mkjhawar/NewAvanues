#!/usr/bin/env python3
"""
Package restructure migration script for Chat module
Migrates from com.augmentalis.ava.features.chat to com.augmentalis.chat
"""

import os
import re
import shutil
from pathlib import Path
from typing import List, Tuple

# Base directory
BASE_DIR = Path("/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/Chat")

# Old and new package paths
OLD_PACKAGE = "com.augmentalis.ava.features.chat"
NEW_PACKAGE = "com.augmentalis.chat"
OLD_PATH = "com/augmentalis/ava/features/chat"
NEW_PATH = "com/augmentalis/chat"

# Package flattening mappings (ui.X -> X)
FLATTEN_MAP = {
    "ui.state": "state",
    "ui.components": "components",
    "ui.dialogs": "dialogs",
    "ui.settings": "settings",
    "ui": "",  # ui root files go to chat root
}

def get_new_package(old_package_line: str) -> str:
    """Convert old package declaration to new one with flattening."""
    # Extract the full package from package declaration
    match = re.match(r'package\s+([\w.]+)', old_package_line)
    if not match:
        return old_package_line

    pkg = match.group(1)

    # Apply flattening rules
    for old_suffix, new_suffix in FLATTEN_MAP.items():
        old_full = f"{OLD_PACKAGE}.{old_suffix}"
        if pkg == old_full:
            new_full = f"{NEW_PACKAGE}.{new_suffix}" if new_suffix else NEW_PACKAGE
            return old_package_line.replace(pkg, new_full)
        elif pkg.startswith(old_full + "."):
            # Handle deeper nesting (shouldn't exist but just in case)
            remainder = pkg[len(old_full)+1:]
            new_full = f"{NEW_PACKAGE}.{new_suffix}.{remainder}" if new_suffix else f"{NEW_PACKAGE}.{remainder}"
            return old_package_line.replace(pkg, new_full)

    # Standard replacement (no flattening needed)
    if pkg.startswith(OLD_PACKAGE):
        new_pkg = pkg.replace(OLD_PACKAGE, NEW_PACKAGE, 1)
        return old_package_line.replace(pkg, new_pkg)

    return old_package_line

def get_new_import(old_import_line: str) -> str:
    """Convert old import statement to new one with flattening."""
    # Apply flattening rules for imports
    for old_suffix, new_suffix in FLATTEN_MAP.items():
        old_import = f"{OLD_PACKAGE}.{old_suffix}"
        new_import = f"{NEW_PACKAGE}.{new_suffix}" if new_suffix else NEW_PACKAGE
        if old_import in old_import_line:
            old_import_line = old_import_line.replace(old_import, new_import)

    # Standard replacement
    return old_import_line.replace(OLD_PACKAGE, NEW_PACKAGE)

def get_new_file_path(old_relative_path: str, source_set: str) -> str:
    """Calculate new file path based on package flattening."""
    # Extract the part after the old package path
    old_pkg_path = OLD_PATH

    # Find where the old package path is in the file path
    path_str = str(old_relative_path)

    # Determine which subpackage this file is in
    if f"{OLD_PATH}/ui/state/" in path_str:
        new_rel_path = path_str.replace(f"{OLD_PATH}/ui/state/", f"{NEW_PATH}/state/")
    elif f"{OLD_PATH}/ui/components/" in path_str:
        new_rel_path = path_str.replace(f"{OLD_PATH}/ui/components/", f"{NEW_PATH}/components/")
    elif f"{OLD_PATH}/ui/dialogs/" in path_str:
        new_rel_path = path_str.replace(f"{OLD_PATH}/ui/dialogs/", f"{NEW_PATH}/dialogs/")
    elif f"{OLD_PATH}/ui/settings/" in path_str:
        new_rel_path = path_str.replace(f"{OLD_PATH}/ui/settings/", f"{NEW_PATH}/settings/")
    elif f"{OLD_PATH}/ui/" in path_str:
        # ui root files go to chat root
        new_rel_path = path_str.replace(f"{OLD_PATH}/ui/", f"{NEW_PATH}/")
    else:
        # Standard replacement (coordinator, di, tts, etc.)
        new_rel_path = path_str.replace(OLD_PATH, NEW_PATH)

    return new_rel_path

def update_file_content(file_path: Path) -> Tuple[str, int, int]:
    """Update package and import declarations in a Kotlin file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    lines = content.split('\n')
    new_lines = []
    package_updated = False
    import_count = 0

    for line in lines:
        new_line = line

        # Update package declaration
        if line.strip().startswith('package ') and OLD_PACKAGE in line:
            new_line = get_new_package(line)
            if new_line != line:
                package_updated = True

        # Update import statements
        elif line.strip().startswith('import ') and OLD_PACKAGE in line:
            new_line = get_new_import(line)
            if new_line != line:
                import_count += 1

        new_lines.append(new_line)

    new_content = '\n'.join(new_lines)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    return str(file_path), package_updated, import_count

def migrate_files():
    """Main migration function."""
    print("=" * 80)
    print("CHAT MODULE PACKAGE RESTRUCTURE MIGRATION")
    print("=" * 80)
    print(f"From: {OLD_PACKAGE}")
    print(f"To:   {NEW_PACKAGE}")
    print(f"Flattening ui.* subpackages to direct children")
    print("=" * 80)

    # Find all Kotlin files
    source_sets = ["main", "commonMain", "androidMain", "test", "androidTest"]
    all_files = []

    for source_set in source_sets:
        src_dir = BASE_DIR / "src" / source_set / "kotlin"
        if src_dir.exists():
            kt_files = list(src_dir.rglob("*.kt"))
            all_files.extend([(f, source_set) for f in kt_files if OLD_PATH in str(f)])

    print(f"\nFound {len(all_files)} files to migrate\n")

    # Step 1: Update package and import declarations
    print("STEP 1: Updating package and import declarations...")
    print("-" * 80)

    updated_files = []
    for file_path, source_set in all_files:
        rel_path = file_path.relative_to(BASE_DIR)
        result_path, pkg_updated, import_count = update_file_content(file_path)
        status = "PKG+IMP" if pkg_updated and import_count > 0 else "PKG" if pkg_updated else "IMP" if import_count > 0 else "SKIP"
        print(f"  [{status:7}] {rel_path} (imports: {import_count})")
        updated_files.append((file_path, source_set, pkg_updated, import_count))

    # Step 2: Create new directory structure and move files
    print("\n" + "=" * 80)
    print("STEP 2: Moving files to new directory structure...")
    print("-" * 80)

    moved_count = 0
    for file_path, source_set, pkg_updated, import_count in updated_files:
        old_rel = str(file_path.relative_to(BASE_DIR / "src" / source_set / "kotlin"))
        new_rel = get_new_file_path(old_rel, source_set)

        if old_rel != new_rel:
            new_path = BASE_DIR / "src" / source_set / "kotlin" / new_rel
            new_path.parent.mkdir(parents=True, exist_ok=True)

            print(f"  MOVE: {old_rel}")
            print(f"     -> {new_rel}")

            shutil.move(str(file_path), str(new_path))
            moved_count += 1

    print(f"\nMoved {moved_count} files")

    # Step 3: Update build.gradle.kts
    print("\n" + "=" * 80)
    print("STEP 3: Updating build.gradle.kts...")
    print("-" * 80)

    build_file = BASE_DIR / "build.gradle.kts"
    if build_file.exists():
        with open(build_file, 'r', encoding='utf-8') as f:
            build_content = f.read()

        # Update namespace
        old_namespace = 'namespace = "com.augmentalis.ava.features.chat"'
        new_namespace = 'namespace = "com.augmentalis.chat"'

        if old_namespace in build_content:
            build_content = build_content.replace(old_namespace, new_namespace)
            with open(build_file, 'w', encoding='utf-8') as f:
                f.write(build_content)
            print("  ✓ Updated namespace to com.augmentalis.chat")
        else:
            print("  ! Namespace not found or already updated")

    # Step 4: Clean up empty directories
    print("\n" + "=" * 80)
    print("STEP 4: Cleaning up empty directories...")
    print("-" * 80)

    for source_set in source_sets:
        old_base = BASE_DIR / "src" / source_set / "kotlin" / OLD_PATH
        if old_base.exists():
            # Remove empty directories from bottom up
            for root, dirs, files in os.walk(str(old_base), topdown=False):
                root_path = Path(root)
                if not files and not dirs:
                    print(f"  DELETE: {root_path.relative_to(BASE_DIR)}")
                    root_path.rmdir()

    # Try to remove the old base directory if empty
    for source_set in source_sets:
        old_base = BASE_DIR / "src" / source_set / "kotlin" / OLD_PATH
        if old_base.exists():
            try:
                if not list(old_base.rglob("*")):
                    old_base.rmdir()
                    print(f"  DELETE: {old_base.relative_to(BASE_DIR)}")
            except:
                pass

    # Summary
    print("\n" + "=" * 80)
    print("MIGRATION COMPLETE")
    print("=" * 80)
    print(f"Total files processed: {len(all_files)}")
    print(f"Files with package updates: {sum(1 for _, _, p, _ in updated_files if p)}")
    print(f"Files with import updates: {sum(1 for _, _, _, i in updated_files if i > 0)}")
    print(f"Total import updates: {sum(i for _, _, _, i in updated_files)}")
    print(f"Files moved: {moved_count}")
    print("=" * 80)

    return len(all_files), moved_count

if __name__ == "__main__":
    try:
        total, moved = migrate_files()
        print("\n✓ Migration completed successfully!")
        exit(0)
    except Exception as e:
        print(f"\n✗ Migration failed: {e}")
        import traceback
        traceback.print_exc()
        exit(1)

#!/usr/bin/env python3
"""
Fix handler files that still have old package declarations
"""

import re
from pathlib import Path

BASE_DIR = Path("/Volumes/M-Drive/Coding/NewAvanues-AVA/Modules/AVA/Actions")
OLD_PKG = "com.augmentalis.ava.features.actions"
NEW_PKG = "com.augmentalis.actions"

def main():
    print("=== Fixing Handler Package Declarations ===")

    # Find all Kotlin files in the new location
    kt_files = list(BASE_DIR.rglob("src/*/kotlin/com/augmentalis/actions/**/*.kt"))

    print(f"Found {len(kt_files)} files to check")

    updated_count = 0
    for kt_file in kt_files:
        try:
            with open(kt_file, 'r', encoding='utf-8') as f:
                content = f.read()

            original_content = content

            # Update package declaration for handlers subpackage
            content = re.sub(
                r'^package\s+com\.augmentalis\.ava\.features\.actions\.handlers',
                f'package {NEW_PKG}.handlers',
                content,
                flags=re.MULTILINE
            )

            # Update package declaration for entities subpackage
            content = re.sub(
                r'^package\s+com\.augmentalis\.ava\.features\.actions\.entities',
                f'package {NEW_PKG}.entities',
                content,
                flags=re.MULTILINE
            )

            # Update package declaration for web subpackage
            content = re.sub(
                r'^package\s+com\.augmentalis\.ava\.features\.actions\.web',
                f'package {NEW_PKG}.web',
                content,
                flags=re.MULTILINE
            )

            # Update any remaining package declaration
            content = re.sub(
                r'^package\s+com\.augmentalis\.ava\.features\.actions',
                f'package {NEW_PKG}',
                content,
                flags=re.MULTILINE
            )

            # Update imports - all variations
            content = re.sub(
                r'import\s+com\.augmentalis\.ava\.features\.actions',
                f'import {NEW_PKG}',
                content
            )

            if content != original_content:
                with open(kt_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                updated_count += 1
                print(f"  Updated: {kt_file.relative_to(BASE_DIR)}")

        except Exception as e:
            print(f"  ERROR: {kt_file.name}: {e}")

    print(f"\n✓ Fixed {updated_count} files")

if __name__ == "__main__":
    main()

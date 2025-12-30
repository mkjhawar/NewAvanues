#!/usr/bin/env python3
"""
Merge individual category .VOS files into single locale files

Input:  connectivity-en-US.VOS, navigation-en-US.VOS, etc.
Output: en-US.VOS (all commands combined)
"""

import json
from pathlib import Path
from datetime import datetime

# Paths
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
VOS_DIR = PROJECT_ROOT / "modules/managers/CommandManager/src/main/assets/localization/commands"

def merge_vos_files_by_locale(locale="en-US"):
    """
    Merge all category-specific .VOS files into single locale file

    Example:
        connectivity-en-US.VOS + navigation-en-US.VOS + ... → en-US.VOS
    """

    print(f"Merging .VOS files for locale: {locale}")
    print()

    # Find all VOS files for this locale
    vos_files = sorted(VOS_DIR.glob(f"*-{locale}.VOS"))

    if not vos_files:
        print(f"❌ No .VOS files found for locale {locale}")
        return

    print(f"Found {len(vos_files)} category files:")
    for vos_file in vos_files:
        print(f"  • {vos_file.name}")
    print()

    # Merge all commands
    all_commands = []
    total_commands = 0
    categories = []

    for vos_file in vos_files:
        try:
            with open(vos_file, 'r', encoding='utf-8') as f:
                data = json.load(f)

            commands = data.get("commands", [])
            category = data.get("metadata", {}).get("category", "unknown")

            all_commands.extend(commands)
            total_commands += len(commands)
            categories.append(category)

            print(f"  ✅ {vos_file.name:<30} {len(commands):>3} commands")

        except Exception as e:
            print(f"  ❌ {vos_file.name}: {e}")

    print()
    print(f"Total commands: {total_commands}")
    print()

    # Create merged file
    merged_data = {
        "version": "1.0",
        "locale": locale,
        "fallback": "en-US",
        "updated": datetime.now().strftime("%Y-%m-%d"),
        "author": "VOS4 Team",
        "metadata": {
            "schema": "4-element: [id, primary, synonyms, description]",
            "total_commands": total_commands,
            "categories": categories,
            "source": "Merged from category-specific .VOS files"
        },
        "commands": all_commands
    }

    # Save merged file
    output_path = VOS_DIR / f"{locale}.VOS"
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(merged_data, f, indent=2, ensure_ascii=False)

    print(f"✅ Created: {output_path.name}")
    print(f"   Commands: {total_commands}")
    print(f"   Categories: {len(categories)}")
    print()

def main():
    """Merge .VOS files for all available locales"""

    print("=" * 70)
    print("VOS File Merger")
    print("=" * 70)
    print()

    # Find all unique locales
    vos_files = VOS_DIR.glob("*-*.VOS")
    locales = set()

    for vos_file in vos_files:
        # Extract locale from filename: connectivity-en-US.VOS → en-US
        parts = vos_file.stem.split('-')
        if len(parts) >= 2:
            locale = '-'.join(parts[-2:])  # Get last 2 parts (en-US)
            locales.add(locale)

    print(f"Found locales: {', '.join(sorted(locales))}")
    print()

    # Merge each locale
    for locale in sorted(locales):
        merge_vos_files_by_locale(locale)

    print("=" * 70)
    print("Merge Complete")
    print("=" * 70)

if __name__ == "__main__":
    main()

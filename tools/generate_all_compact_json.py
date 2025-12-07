#!/usr/bin/env python3
"""
Multi-Language Compact JSON Generator

Generates compact JSON files for all supported languages (en-US, de-DE, es-ES, fr-FR).

Usage:
    python3 generate_all_compact_json.py
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Any
from datetime import datetime

# Base paths
COMMANDS_DIR = Path("modules/managers/CommandManager/src/main/assets/commands")

# Language configurations
LANGUAGES = {
    "en-US": {
        "vos_dir": "en-VOS",
        "locale": "en-US",
        "fallback": "en-US",
        "name": "English (United States)"
    },
    "de-DE": {
        "vos_dir": "de-VOS",
        "locale": "de-DE",
        "fallback": "en-US",
        "name": "Deutsch (Deutschland)"
    },
    "es-ES": {
        "vos_dir": "es-VOS",
        "locale": "es-ES",
        "fallback": "en-US",
        "name": "Español (España)"
    },
    "fr-FR": {
        "vos_dir": "fr-VOS",
        "locale": "fr-FR",
        "fallback": "en-US",
        "name": "Français (France)"
    }
}


def convert_vos_command_to_compact(vos_command: Dict[str, Any], category: str) -> List:
    """
    Convert a single VOS command to compact array format.

    Args:
        vos_command: Dict with keys: action, cmd, syn
        category: Category/display_name from file_info

    Returns:
        List: [action, primary_cmd, [synonyms], description]
    """
    action = vos_command["action"].lower()
    primary_cmd = vos_command["cmd"]
    synonyms = vos_command["syn"]

    # Generate description from action and category
    action_words = action.replace("_", " ").title()
    description = f"{action_words} ({category})"

    return [action, primary_cmd, synonyms, description]


def convert_vos_file_to_compact(vos_file_path: Path) -> List[List]:
    """
    Convert entire VOS file to compact format commands.

    Args:
        vos_file_path: Path to VOS file

    Returns:
        List of compact command arrays
    """
    with open(vos_file_path, 'r', encoding='utf-8') as f:
        vos_data = json.load(f)

    category = vos_data["file_info"]["display_name"]
    commands = vos_data["commands"]

    compact_commands = []
    for cmd in commands:
        compact_cmd = convert_vos_command_to_compact(cmd, category)
        compact_commands.append(compact_cmd)

    return compact_commands


def generate_compact_json(locale_code: str, config: Dict[str, str]) -> Dict[str, Any]:
    """
    Generate complete compact JSON file for a locale.

    Args:
        locale_code: Locale code (e.g., "en-US")
        config: Language configuration dict

    Returns:
        Dict with compact JSON structure
    """
    vos_dir = COMMANDS_DIR / config["vos_dir"]
    all_commands = []

    # Get all VOS files
    vos_files = sorted(vos_dir.glob("*.vos"))

    # Convert each VOS file
    for vos_file in vos_files:
        print(f"  Converting {vos_file.name}...")
        compact_commands = convert_vos_file_to_compact(vos_file)
        all_commands.extend(compact_commands)

    # Build compact JSON structure
    compact_json = {
        "version": "1.0",
        "locale": config["locale"],
        "fallback": config["fallback"],
        "updated": datetime.now().strftime("%Y-%m-%d"),
        "author": "VOS4 Team",
        "commands": all_commands
    }

    return compact_json


def save_compact_json(compact_data: Dict[str, Any], locale_code: str):
    """
    Save compact JSON to file.

    Args:
        compact_data: Compact JSON data
        locale_code: Locale code (e.g., "en-US")
    """
    output_file = COMMANDS_DIR / f"{locale_code}.json"

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(compact_data, f, indent=2, ensure_ascii=False)

    print(f"  ✓ Saved {output_file}")
    print(f"  Total commands: {len(compact_data['commands'])}\n")


def main():
    """Main conversion function."""
    print("=" * 60)
    print("Multi-Language Compact JSON Generator")
    print("=" * 60)
    print()

    total_commands_generated = 0

    # Convert all languages
    for locale_code, config in LANGUAGES.items():
        lang_name = config["name"]
        print(f"Converting {lang_name} ({locale_code})...")
        print("-" * 60)

        compact_data = generate_compact_json(locale_code, config)
        save_compact_json(compact_data, locale_code)

        total_commands_generated += len(compact_data['commands'])

    print("=" * 60)
    print("Conversion Complete!")
    print("=" * 60)
    print(f"\nGenerated {len(LANGUAGES)} compact JSON files:")
    for locale_code, config in LANGUAGES.items():
        print(f"  - {locale_code}.json ({config['name']})")
    print(f"\nTotal commands generated: {total_commands_generated}")


if __name__ == "__main__":
    # Change to VoiceOS directory
    os.chdir("/Volumes/M-Drive/Coding/VoiceOS")
    main()

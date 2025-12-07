#!/usr/bin/env python3
"""
VOS to Compact JSON Converter

Converts VOS command files from verbose JSON format to compact array format.

VOS Format:
    {"action": "X", "cmd": "y", "syn": ["z1", "z2"]}

Compact Format:
    ["action", "primary_cmd", ["synonym1", "synonym2"], "description"]

Usage:
    python3 convert_vos_to_compact.py
"""

import json
import os
from pathlib import Path
from typing import Dict, List, Any
from datetime import datetime

# Base paths
VOS_DIR = Path("modules/managers/CommandManager/src/main/assets/commands/vos")
COMPACT_DIR = Path("modules/managers/CommandManager/src/main/assets/commands")

# VOS files to convert
VOS_FILES = [
    "browser-commands.vos",
    "connectivity-commands.vos",
    "cursor-commands.vos",
    "dialog-commands.vos",
    "dictation-commands.vos",
    "drag-commands.vos",
    "editing-commands.vos",
    "gaze-commands.vos",
    "gesture-commands.vos",
    "keyboard-commands.vos",
    "menu-commands.vos",
    "navigation-commands.vos",
    "notifications-commands.vos",
    "overlays-commands.vos",
    "scroll-commands.vos",
    "settings-commands.vos",
    "swipe-commands.vos",
    "system-commands.vos",
    "volume-commands.vos",
]


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


def generate_compact_json(locale: str = "en-US") -> Dict[str, Any]:
    """
    Generate complete compact JSON file for a locale.

    Args:
        locale: Locale code (e.g., "en-US")

    Returns:
        Dict with compact JSON structure
    """
    all_commands = []

    # Convert each VOS file
    for vos_file in VOS_FILES:
        vos_path = VOS_DIR / vos_file

        if not vos_path.exists():
            print(f"Warning: {vos_file} not found, skipping")
            continue

        print(f"Converting {vos_file}...")
        compact_commands = convert_vos_file_to_compact(vos_path)
        all_commands.extend(compact_commands)

    # Build compact JSON structure
    compact_json = {
        "version": "1.0",
        "locale": locale,
        "fallback": "en-US",
        "updated": datetime.now().strftime("%Y-%m-%d"),
        "author": "VOS4 Team",
        "commands": all_commands
    }

    return compact_json


def save_compact_json(compact_data: Dict[str, Any], locale: str):
    """
    Save compact JSON to file.

    Args:
        compact_data: Compact JSON data
        locale: Locale code (e.g., "en-US")
    """
    output_file = COMPACT_DIR / f"{locale}.json"

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(compact_data, f, indent=2, ensure_ascii=False)

    print(f"\n✓ Saved {output_file}")
    print(f"  Total commands: {len(compact_data['commands'])}")


def main():
    """Main conversion function."""
    print("="*60)
    print("VOS to Compact JSON Converter")
    print("="*60)
    print()

    # Convert English
    print("Converting English (en-US)...")
    print("-"*60)
    en_compact = generate_compact_json("en-US")
    save_compact_json(en_compact, "en-US")

    print("\n" + "="*60)
    print("Conversion Complete!")
    print("="*60)
    print(f"\nConverted {len(VOS_FILES)} VOS files")
    print(f"Generated 1 compact JSON file (en-US.json)")
    print(f"Total commands in compact format: {len(en_compact['commands'])}")


if __name__ == "__main__":
    # Change to VoiceOS directory
    os.chdir("/Volumes/M-Drive/Coding/VoiceOS")
    main()

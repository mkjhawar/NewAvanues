#!/usr/bin/env python3
"""
Convert all legacy .vos files to compact .VOS format

This script:
1. Reads all legacy .vos files (verbose object format)
2. Converts to compact array format
3. Saves as .VOS files with new extension
4. Creates separate .VOS files per category (not combined)
"""

import json
import os
from pathlib import Path
from datetime import datetime

# Paths
SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent
VOS_INPUT_DIR = PROJECT_ROOT / "modules/managers/CommandManager/src/main/assets/commands/vos"
VOS_OUTPUT_DIR = PROJECT_ROOT / "modules/managers/CommandManager/src/main/assets/localization/commands"

def normalize_action_id(action_id):
    """
    Convert UPPERCASE_ACTION to lowercase_action

    Examples:
        TURN_ON_BLUETOOTH -> turn_on_bluetooth
        NAVIGATE_HOME -> navigate_home
    """
    return action_id.lower()

def create_description(cmd, category):
    """
    Create human-readable description from command and category

    Examples:
        ("turn on bluetooth", "connectivity") -> "Turn On Bluetooth (Network Connectivity)"
        ("navigate home", "navigation") -> "Navigate Home (Navigation)"
    """
    # Capitalize each word
    words = cmd.split()
    capitalized = " ".join(word.capitalize() for word in words)

    # Category display names
    category_names = {
        "connectivity": "Network Connectivity",
        "navigation": "Navigation",
        "volume": "Volume",
        "cursor": "Cursor Control",
        "dialog": "Dialogs",
        "dictation": "Dictation",
        "drag": "Drag & Drop",
        "editing": "Text Editing",
        "gaze": "Gaze Control",
        "gesture": "Gestures",
        "keyboard": "Keyboard",
        "menu": "Menus",
        "notifications": "Notifications",
        "overlays": "Overlays",
        "scroll": "Scrolling",
        "settings": "Settings",
        "swipe": "Swipe Gestures",
        "system": "System",
        "browser": "Browser"
    }

    category_display = category_names.get(category, category.capitalize())
    return f"{capitalized} ({category_display})"

def convert_vos_command_to_compact(command_obj, category):
    """
    Convert single VOS command from object to compact array format

    Input (object format):
    {
        "action": "TURN_ON_BLUETOOTH",
        "cmd": "turn on bluetooth",
        "syn": ["bluetooth on", "enable bluetooth", ...]
    }

    Output (compact array format):
    ["turn_on_bluetooth", "turn on bluetooth", ["bluetooth on", ...], "Turn On Bluetooth (Network)"]
    """
    action_id = normalize_action_id(command_obj.get("action", ""))
    primary_cmd = command_obj.get("cmd", "")
    synonyms = command_obj.get("syn", [])
    description = create_description(primary_cmd, category)

    return [action_id, primary_cmd, synonyms, description]

def convert_vos_file_to_compact(input_path):
    """
    Convert entire VOS file from object format to compact array format

    Returns dict with compact structure ready for JSON export
    """
    with open(input_path, 'r', encoding='utf-8') as f:
        vos_data = json.load(f)

    # Extract metadata
    file_info = vos_data.get("file_info", {})
    category = file_info.get("category", "unknown")
    locale = vos_data.get("locale", "en-US")

    # Convert commands
    compact_commands = []
    for command_obj in vos_data.get("commands", []):
        compact_cmd = convert_vos_command_to_compact(command_obj, category)
        compact_commands.append(compact_cmd)

    # Create compact structure
    compact_data = {
        "version": "1.0",
        "locale": locale,
        "fallback": "en-US",
        "updated": datetime.now().strftime("%Y-%m-%d"),
        "author": "VOS4 Team",
        "metadata": {
            "category": category,
            "display_name": file_info.get("display_name", category.capitalize()),
            "description": file_info.get("description", ""),
            "source_file": file_info.get("filename", ""),
            "schema": "4-element: [id, primary, synonyms, description]"
        },
        "commands": compact_commands
    }

    return compact_data, category, len(compact_commands)

def save_compact_vos(compact_data, output_path):
    """Save compact data to .VOS file with proper formatting"""
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(compact_data, f, indent=2, ensure_ascii=False)

def convert_all_vos_files():
    """Convert all .vos files in input directory to compact .VOS format"""

    print("=" * 70)
    print("VOS to Compact .VOS Converter")
    print("=" * 70)
    print()

    # Ensure output directory exists
    VOS_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Find all .vos files
    vos_files = sorted(VOS_INPUT_DIR.glob("*.vos"))

    if not vos_files:
        print(f"❌ No .vos files found in {VOS_INPUT_DIR}")
        return

    print(f"📂 Input directory:  {VOS_INPUT_DIR}")
    print(f"📂 Output directory: {VOS_OUTPUT_DIR}")
    print(f"📄 Found {len(vos_files)} .vos files to convert")
    print()

    total_commands = 0
    converted_files = []

    for vos_file in vos_files:
        print(f"Converting: {vos_file.name}")

        try:
            # Convert to compact format
            compact_data, category, command_count = convert_vos_file_to_compact(vos_file)

            # Create output filename: category-en-US.VOS
            output_filename = f"{category}-en-US.VOS"
            output_path = VOS_OUTPUT_DIR / output_filename

            # Save
            save_compact_vos(compact_data, output_path)

            total_commands += command_count
            converted_files.append({
                "input": vos_file.name,
                "output": output_filename,
                "commands": command_count
            })

            print(f"  ✅ {vos_file.name}")
            print(f"     → {output_filename}")
            print(f"     → {command_count} commands")
            print()

        except Exception as e:
            print(f"  ❌ Error: {e}")
            print()

    # Summary
    print("=" * 70)
    print("Conversion Complete")
    print("=" * 70)
    print()
    print(f"✅ Converted {len(converted_files)} files")
    print(f"✅ Total commands: {total_commands}")
    print()

    print("Files created:")
    for file_info in converted_files:
        print(f"  • {file_info['output']:<30} ({file_info['commands']:>3} commands)")
    print()

    print(f"Output location: {VOS_OUTPUT_DIR}")
    print()

    # Create index file
    create_index_file(converted_files, total_commands)

def create_index_file(converted_files, total_commands):
    """Create an index.json file listing all converted .VOS files"""

    index_data = {
        "version": "1.0",
        "updated": datetime.now().strftime("%Y-%m-%d"),
        "total_files": len(converted_files),
        "total_commands": total_commands,
        "files": [
            {
                "filename": f["output"],
                "command_count": f["commands"],
                "category": f["output"].replace("-en-US.VOS", "")
            }
            for f in converted_files
        ]
    }

    index_path = VOS_OUTPUT_DIR / "index.json"
    with open(index_path, 'w', encoding='utf-8') as f:
        json.dump(index_data, f, indent=2, ensure_ascii=False)

    print(f"📋 Created index file: {index_path.name}")
    print()

if __name__ == "__main__":
    convert_all_vos_files()

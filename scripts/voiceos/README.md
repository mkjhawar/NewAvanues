# VoiceOS Command Manager Conversion Tools

This directory contains automation scripts for converting and managing VoiceOS command files.

## Scripts

### 1. `convert_vos_to_compact.py`

Converts VOS command files from verbose JSON format to compact array format.

**Usage**:
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
python3 tools/convert_vos_to_compact.py
```

**Input**: VOS files in `modules/managers/CommandManager/src/main/assets/commands/vos/`

**Output**: `modules/managers/CommandManager/src/main/assets/commands/en-US.json`

**What it does**:
- Reads all 19 VOS files
- Converts from `{"action": "X", "cmd": "y", "syn": [...]}` to `["action", "cmd", ["synonyms"], "description"]`
- Generates single en-US.json with all commands
- Preserves all synonyms and metadata

---

### 2. `translate_vos_commands.py`

Translates VOS command files from English to German, Spanish, and French.

**Usage**:
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
python3 tools/translate_vos_commands.py
```

**Input**: English VOS files in `en-VOS/` directory

**Output**: Translated VOS files in `de-VOS/`, `es-VOS/`, `fr-VOS/` directories

**What it does**:
- Reads English VOS files
- Translates commands and synonyms using built-in dictionaries
- Supports 40+ common voice terms per language
- Preserves technical terms (Bluetooth, WiFi, etc.)
- Creates culturally appropriate variants

**Translation Coverage**:
- German (de-DE): 94 commands, 1,024 synonyms
- Spanish (es-ES): 94 commands, 1,024 synonyms
- French (fr-FR): 94 commands, 1,024 synonyms

---

### 3. `generate_all_compact_json.py`

Generates compact JSON files for all supported languages.

**Usage**:
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
python3 tools/generate_all_compact_json.py
```

**Input**: VOS files in `en-VOS/`, `de-VOS/`, `es-VOS/`, `fr-VOS/` directories

**Output**: Compact JSON files in `modules/managers/CommandManager/src/main/assets/localization/commands/`:
- `en-US.json` (English)
- `de-DE.json` (German)
- `es-ES.json` (Spanish)
- `fr-FR.json` (French)

**What it does**:
- Converts VOS files for all languages to compact format
- Generates complete compact JSON files
- Preserves all synonyms and metadata
- Total: 376 commands (94 × 4 languages)

---

## Workflow

### Initial Setup (One-time)

```bash
# 1. Convert English VOS to compact JSON
python3 tools/convert_vos_to_compact.py

# 2. Translate VOS files to other languages
python3 tools/translate_vos_commands.py

# 3. Generate compact JSON for all languages
python3 tools/generate_all_compact_json.py
```

### Adding New Commands

```bash
# 1. Edit VOS file in commands/vos/ directory
# 2. Copy to all language directories
cp modules/managers/CommandManager/src/main/assets/commands/vos/your-new-file.vos \
   modules/managers/CommandManager/src/main/assets/commands/en-VOS/

# 3. Translate manually or update translation script
# 4. Re-run translation
python3 tools/translate_vos_commands.py

# 5. Re-generate compact JSON
python3 tools/generate_all_compact_json.py
```

### Updating Existing Commands

```bash
# 1. Edit VOS file in appropriate language directory
# 2. Re-generate compact JSON
python3 tools/generate_all_compact_json.py
```

---

## File Format Reference

### VOS Format (Verbose)

```json
{
  "schema": "vos-1.0",
  "version": "1.0.0",
  "file_info": {
    "filename": "navigation-commands.vos",
    "category": "navigation",
    "display_name": "Navigation",
    "description": "Voice commands for navigating the system and apps",
    "command_count": 9
  },
  "locale": "en-US",
  "commands": [
    {
      "action": "NAVIGATE_HOME",
      "cmd": "navigate home",
      "syn": ["go home", "return home", "home screen"]
    }
  ]
}
```

### Compact Format (Array)

```json
{
  "version": "1.0",
  "locale": "en-US",
  "fallback": "en-US",
  "updated": "2025-11-13",
  "author": "VOS4 Team",
  "commands": [
    ["navigate_home", "navigate home", ["go home", "return home"], "Navigate Home (Navigation)"]
  ]
}
```

---

## Statistics

- **VOS Files**: 19
- **Total Commands**: 94
- **Synonyms per Command (avg)**: 13.65
- **Total Synonym Variants**: 1,024
- **Languages**: 4 (English, German, Spanish, French)
- **Total Commands Generated**: 376 (94 × 4)

---

## Requirements

- Python 3.6+
- Standard library only (no external dependencies)

---

## Documentation

For complete documentation, see:
- `/docs/implementation/COMMAND-MANAGER-FORMAT-CONVERSION.md`

---

## Troubleshooting

### Script fails with "No such file or directory"

**Solution**: Ensure you're running from VoiceOS root directory:
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
python3 tools/script_name.py
```

### Translation quality issues

**Solution**: Edit translation dictionaries in `translate_vos_commands.py`:
```python
TRANSLATIONS = {
    "de": {  # German
        "your term": "translated term",
        ...
    }
}
```

### Missing commands in output

**Solution**: Check VOS file `command_count` matches actual commands:
```json
"file_info": {
  "command_count": 9  // Must match actual command array length
}
```

---

**Last Updated**: 2025-11-13
**Version**: 1.0
**Status**: Production Ready ✅

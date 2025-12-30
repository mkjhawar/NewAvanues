# Universal Format v2.0 Migration Guide

**Last Updated:** 2025-11-24
**Author:** AVA AI Team
**Copyright:** © 2025 Intelligent Devices LLC / Manoj Jhawar
**Applies To:** All .ava intent training files

---

## Table of Contents

1. [Overview](#overview)
2. [AVA Format Ecosystem](#ava-format-ecosystem)
3. [Why Migrate?](#why-migrate)
4. [Format Comparison](#format-comparison)
5. [Migration Steps](#migration-steps)
6. [Automated Migration](#automated-migration)
7. [Manual Migration](#manual-migration)
8. [Validation](#validation)
9. [Troubleshooting](#troubleshooting)
10. [Tools](#tools)
11. [Related Documentation](#related-documentation)

---

## Overview

Universal Format v2.0 is a human-readable, text-based format for AVA intent training data. It replaces the deprecated v1.0 JSON format with a more maintainable, version-control-friendly structure.

This guide covers migration of **Avanues Universal Format (.ava)** files specifically. For other AVA proprietary formats, see the [AVA Format Ecosystem](#ava-format-ecosystem) section below.

### Key Improvements

✅ **Human Readable** - Plain text instead of nested JSON  
✅ **Git Friendly** - Easier to diff and merge  
✅ **Comment Support** - Add documentation inline  
✅ **Metadata Rich** - Structured file information  
✅ **Parser Efficient** - Faster loading and validation

---

## AVA Format Ecosystem

The AVA AI system uses three proprietary formats for different purposes:

### 1. Avanues Universal Format (.ava)
**Purpose:** Intent training data for NLU (Natural Language Understanding)
**This Guide:** You are here - learn how to migrate .ava files from v1.0 to v2.0

**Files:**
- `information.ava` - Weather, time, news queries
- `productivity.ava` - Alarms, reminders, calendar
- `media-control.ava` - Music, video playback
- `system-control.ava` - Volume, brightness, settings

**Format:** Text-based, YAML-like structure

### 2. AON Format (.aon)
**Purpose:** Ava Optimized NLU - Neural network models for semantic understanding
**Full Name:** Ava Optimized NLU
**Based On:** ONNX opset 14 with AVA metadata extensions

**Files:**
- `AVA-MobileBERT-384-INT8.aon` - Intent classification model
- `AVA-MiniLM-L6-384-BASE.aon` - Sentence embeddings
- TVM-compiled runtime libraries (`.so`)

**Documentation:** See [DeveloperTools/docs/AON-FORMAT-SPEC.md](../../DeveloperTools/docs/AON-FORMAT-SPEC.md)

### 3. ALM Format (.alm)
**Purpose:** Ava Language Model - Large language models optimized for mobile
**Full Name:** Ava Language Model
**Based On:** MLC-LLM with 4-bit/8-bit quantization

**Files:**
- `AVA-ALM-LLaMA-2-7B-Q4.alm` - Conversational AI model
- `AVA-ALM-Gemma-2B-Q4.alm` - Compact language model

**Documentation:** See [DeveloperTools/docs/ALM-FORMAT-SPEC.md](../../DeveloperTools/docs/ALM-PROCESSING-GUIDE.md)

### Format Registry

For a complete mapping of standard formats (ONNX, GGUF, etc.) to AVA formats, see:
**[DeveloperTools/FORMAT-REGISTRY.md](../../DeveloperTools/FORMAT-REGISTRY.md)**

### AVA Model Toolchain

The **AVA AI Model Toolchain** provides desktop applications (macOS and Linux) for:
- Converting .ava v1.0 → v2.0
- Compiling PyTorch/HuggingFace models → AON format
- Optimizing AON models with TVM 0.22
- Processing LLMs → ALM format
- Performance profiling and benchmarking

**Documentation:** See [DeveloperTools/README.md](../../DeveloperTools/README.md)

---

## Why Migrate?

### v1.0 JSON Format is Deprecated

The AvaFileReader explicitly rejects v1.0 files with error:

```
Invalid .ava file: Must use Universal Format v2.0 (start with # or ---). 
v1.0 JSON format is no longer supported.
```

###Impact of Not Migrating

❌ Intent files won't load  
❌ No semantic embeddings computed  
❌ Fallback to low-accuracy keyword matching  
❌ Natural language queries fail

### Migration Benefits

✅ Semantic similarity classification  
✅ High accuracy on conversational queries  
✅ Proper embedding computation  
✅ Future-proof format

---

## Format Comparison

### v1.0 JSON Format (Deprecated)

```json
{
  "s": "ava-1.0",
  "v": "1.0.0",
  "l": "en-US",
  "m": {
    "f": "information.ava",
    "c": "information",
    "n": "Information Queries",
    "d": "Weather, time, news, and general information",
    "cnt": 2
  },
  "i": [
    {
      "id": "check_weather",
      "c": "check weather",
      "s": [
        "what's the weather",
        "weather forecast",
        "will it rain"
      ],
      "cat": "information",
      "p": 1,
      "t": ["weather", "forecast", "rain"]
    },
    {
      "id": "show_time",
      "c": "show time",
      "s": [
        "what time is it",
        "current time"
      ],
      "cat": "information",
      "p": 1,
      "t": ["time", "clock"]
    }
  ],
  "syn": {
    "check": ["show", "display", "tell"],
    "weather": ["forecast", "temperature"]
  }
}
```

### v2.0 Universal Format (Current)

```
# Avanues Universal Format v2.0
# Type: AVA - Voice Intent Examples
# Extension: .ava
# Project: AVA (AI Voice Assistant)
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: information.ava
  category: information
  name: Information Queries
  description: Weather, time, news, and general information
  priority: 1
  count: 2
---
INFO:check_weather:what's the weather
INFO:check_weather:weather forecast
INFO:check_weather:will it rain

INFO:show_time:what time is it
INFO:show_time:current time
---
synonyms:
  check: [show, display, tell]
  weather: [forecast, temperature]
```

### Side-by-Side Comparison

| Feature | v1.0 JSON | v2.0 Universal |
|---------|-----------|----------------|
| **Format** | JSON | Text/YAML-like |
| **Readability** | Low | High |
| **Comments** | No | Yes (`#`) |
| **Diff-friendly** | No | Yes |
| **File Size** | Larger | Smaller |
| **Parse Speed** | Slower | Faster |
| **Validation** | Complex | Simple |
| **Status** | ❌ Deprecated | ✅ Current |

---

## Migration Steps

### Step 1: Identify v1.0 Files

```bash
# Find all .ava files
find apps/ava-standalone/src/main/assets -name "*.ava"

# Check format (v1.0 starts with "{", v2.0 starts with "#" or "---")
head -1 apps/ava-standalone/src/main/assets/ava-examples/en-US/information.ava
```

**v1.0 indicator:** First character is `{`  
**v2.0 indicator:** First line starts with `#` or `---`

### Step 2: Backup Original Files

```bash
# Create backup directory
mkdir -p /tmp/ava-backup

# Copy all .ava files
cp apps/ava-standalone/src/main/assets/ava-examples/en-US/*.ava /tmp/ava-backup/
```

### Step 3: Convert Files

Choose one:
- [Automated Migration](#automated-migration) (recommended)
- [Manual Migration](#manual-migration) (for complex files)

### Step 4: Update AssetExtractor

Ensure your converted files are in the extraction list:

```kotlin
// Universal/AVA/Features/NLU/src/androidMain/kotlin/.../AssetExtractor.kt

val avaFiles = listOf(
    "information.ava",      // Your converted file
    "productivity.ava",     // Your converted file
    "navigation.ava",
    "media-control.ava",
    "system-control.ava"
)
```

### Step 5: Rebuild & Test

```bash
# Build APK
./gradlew :apps:ava-standalone:assembleDebug

# Install
adb install -r apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk

# Clear data (force re-extraction)
adb shell pm clear com.augmentalis.ava.debug

# Launch and check logs
adb logcat | grep "Loaded.*intents from.*information"
```

### Step 6: Verify Embeddings

```bash
adb logcat | grep "Computing embeddings for check_weather"
adb logcat | grep "Computing embeddings for show_time"
adb logcat | grep "Pre-computation complete:"
```

Expected: Intent count should increase by number of intents in converted files.

---

## Automated Migration

### Using Python Script (Recommended)

```python
#!/usr/bin/env python3
"""
AVA Format Migration Tool
Converts v1.0 JSON .ava files to v2.0 Universal Format
"""

import json
import sys
from pathlib import Path
from datetime import datetime

def migrate_v1_to_v2(input_file: Path, output_file: Path = None):
    """Convert v1.0 JSON .ava to v2.0 Universal Format"""
    
    if output_file is None:
        output_file = input_file
    
    # Read v1.0 JSON
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    # Extract metadata
    meta = data.get('m', {})
    locale = data.get('l', 'en-US')
    intents = data.get('i', [])
    synonyms = data.get('syn', {})
    
    # Determine prefix from category
    category = meta.get('c', 'MISC')
    prefix = category[:4].upper()
    
    # Build v2.0 content
    lines = []
    
    # Header
    lines.append("# Avanues Universal Format v2.0")
    lines.append("# Type: AVA - Voice Intent Examples")
    lines.append("# Extension: .ava")
    lines.append("# Project: AVA (AI Voice Assistant)")
    lines.append(f"# Migrated: {datetime.now().strftime('%Y-%m-%d')}")
    lines.append("---")
    
    # Metadata section
    lines.append("schema: avu-1.0")
    lines.append(f"version: {data.get('v', '1.0.0')}")
    lines.append(f"locale: {locale}")
    lines.append("project: ava")
    lines.append("metadata:")
    lines.append(f"  file: {meta.get('f', input_file.name)}")
    lines.append(f"  category: {category}")
    lines.append(f"  name: {meta.get('n', 'Unknown')}")
    lines.append(f"  description: {meta.get('d', '')}")
    lines.append(f"  priority: {meta.get('p', 1)}")
    lines.append(f"  count: {meta.get('cnt', len(intents))}")
    lines.append("---")
    
    # Intent examples
    for intent in intents:
        intent_id = intent['id']
        examples = intent.get('s', [])
        
        # Group by intent
        for example in examples:
            lines.append(f"{prefix}:{intent_id}:{example}")
        
        # Add blank line between intents
        lines.append("")
    
    # Remove trailing blank line
    if lines[-1] == "":
        lines.pop()
    
    lines.append("---")
    
    # Synonyms section
    if synonyms:
        lines.append("synonyms:")
        for key, values in synonyms.items():
            lines.append(f"  {key}: {json.dumps(values)}")
    
    # Write v2.0 file
    with open(output_file, 'w') as f:
        f.write('\n'.join(lines))
        f.write('\n')  # Final newline
    
    print(f"✅ Migrated: {input_file} → {output_file}")
    print(f"   Intents: {len(intents)}")
    print(f"   Examples: {sum(len(i.get('s', [])) for i in intents)}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 migrate_ava.py <input.ava> [output.ava]")
        sys.exit(1)
    
    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2]) if len(sys.argv) > 2 else input_path
    
    migrate_v1_to_v2(input_path, output_path)
```

### Usage

```bash
# Make executable
chmod +x migrate_ava.py

# Convert single file
./migrate_ava.py apps/.../information.ava

# Convert with different output
./migrate_ava.py old/information.ava new/information.ava

# Batch convert
for file in apps/.../ava-examples/en-US/*.ava; do
    ./migrate_ava.py "$file"
done
```

---

## Manual Migration

### Template

```
# Avanues Universal Format v2.0
# Type: AVA - Voice Intent Examples
# Extension: .ava
# Project: AVA (AI Voice Assistant)
---
schema: avu-1.0
version: 1.0.0
locale: {LOCALE}
project: ava
metadata:
  file: {FILENAME}
  category: {CATEGORY}
  name: {NAME}
  description: {DESCRIPTION}
  priority: {PRIORITY}
  count: {INTENT_COUNT}
---
{PREFIX}:{intent_id}:{example_phrase_1}
{PREFIX}:{intent_id}:{example_phrase_2}
{PREFIX}:{intent_id}:{example_phrase_3}

{PREFIX}:{intent_id_2}:{example_phrase_1}
{PREFIX}:{intent_id_2}:{example_phrase_2}
---
synonyms:
  {word}: [{synonym1}, {synonym2}]
```

### Manual Conversion Example

**Input (v1.0 JSON):**
```json
{
  "i": [
    {
      "id": "play_music",
      "s": ["play song", "start music", "play track"]
    }
  ]
}
```

**Output (v2.0):**
```
VCM:play_music:play song
VCM:play_music:start music
VCM:play_music:play track
```

### Prefix Guidelines

| Category | Prefix | Example |
|----------|--------|---------|
| Information | INFO | INFO:check_weather |
| Productivity | PROD | PROD:set_alarm |
| Smart Home | HOME | HOME:turn_on_lights |
| Media Control | VCM | VCM:play_music |
| Navigation | NAV | NAV:go_home |
| System | SYS | SYS:increase_volume |

---

## Validation

### Format Validation Checklist

- [ ] File starts with `#` or `---`
- [ ] Has `schema: avu-1.0` line
- [ ] Has complete metadata section
- [ ] All examples use `PREFIX:intent:phrase` format
- [ ] Synonyms section formatted correctly
- [ ] File ends with newline

### Automated Validation

```bash
# Check file format
head -1 information.ava | grep -E '^(#|---)' && echo "✅ Valid v2.0" || echo "❌ Invalid format"

# Validate structure
grep -E '^[A-Z]{2,4}:[a-z_]+:.+$' information.ava && echo "✅ Valid examples" || echo "❌ Invalid example format"
```

### Runtime Validation

```bash
# Build and test
./gradlew :apps:ava-standalone:assembleDebug
adb install -r apps/.../ava-standalone-debug.apk
adb shell pm clear com.augmentalis.ava.debug

# Check logs for errors
adb logcat | grep -E "Failed to load.*\.ava|Invalid.*\.ava"
```

**Success indicators:**
- No "Failed to load" errors
- "Loaded X intents from {filename}.ava" appears
- "Computing embeddings for {intent}" appears
- Intent count increased

---

## Troubleshooting

### Error: "Invalid .ava file: Must use Universal Format v2.0"

**Cause:** File is still in v1.0 JSON format

**Fix:** Run migration script or manually convert

### Error: "Failed to load {filename}.ava"

**Causes:**
1. Syntax error in v2.0 format
2. Missing required sections
3. Incorrect delimiter (`---`)

**Debug:**
```bash
# Check first lines
head -10 {filename}.ava

# Validate delimiter count (should be 3 total)
grep -c '^---$' {filename}.ava

# Check for invalid characters
grep -v '^[#a-zA-Z0-9:_ \[\],{}".-]' {filename}.ava
```

### Intent Not Loaded

**Causes:**
1. File not in AssetExtractor list
2. Syntax error in intent examples
3. Wrong prefix format

**Fix:**
```kotlin
// Add to AssetExtractor.kt
val avaFiles = listOf(
    "your-file.ava",  // Add here
    // ...
)
```

### No Embeddings Computed

**Causes:**
1. Intent examples not in database
2. File loading failed silently
3. Wrong intent ID format

**Debug:**
```bash
# Check database insertion
adb logcat | grep "Inserted.*examples"

# Check embedding computation
adb logcat | grep "Computing embeddings for"

# Check final count
adb logcat | grep "Pre-computation complete"
```

---

## Tools

### 1. Command-Line Migration Script

**Location:** `DeveloperTools/python-tools/migrate_ava.py`

**Features:**
- ✅ Automatic v1.0 → v2.0 conversion
- ✅ Batch processing
- ✅ Format validation
- ✅ Automatic backup creation
- ✅ Detailed conversion reports

**Usage:**
```bash
# Convert single file
cd DeveloperTools/python-tools
./migrate_ava.py /path/to/information.ava

# Batch convert all .ava files
for file in apps/ava-standalone/src/main/assets/ava-examples/en-US/*.ava; do
    ./migrate_ava.py "$file"
done
```

**Documentation:** See [DeveloperTools/python-tools/README.md](../../DeveloperTools/python-tools/README.md)

### 2. AVA Model Toolchain (GUI Application)

**Platforms:** macOS 12.0+, Linux (Ubuntu 20.04+, Fedora 38+)

**Features:**
- 🖥️ Native desktop application with world-class UI
- 📁 Drag-and-drop batch conversion
- ✅ Real-time validation and preview
- 📊 Conversion statistics and reports
- 🚀 Integrated with AON/ALM compilation tools

**Tab: Intent Files (.ava Converter)**
- Converts .ava v1.0 → v2.0
- Batch processing with progress tracking
- Format validation with error highlighting
- Side-by-side preview (before/after)

**Installation:**
```bash
# macOS
curl -LO https://releases.ava-ai.com/toolchain/AVAToolchain-1.0.0.dmg
open AVAToolchain-1.0.0.dmg

# Linux (Debian/Ubuntu)
wget https://releases.ava-ai.com/toolchain/ava-toolchain_1.0.0_amd64.deb
sudo dpkg -i ava-toolchain_1.0.0_amd64.deb

# Linux (AppImage)
wget https://releases.ava-ai.com/toolchain/AVAToolchain-1.0.0-x86_64.AppImage
chmod +x AVAToolchain-1.0.0-x86_64.AppImage
./AVAToolchain-1.0.0-x86_64.AppImage
```

**Documentation:** See [DeveloperTools/README.md](../../DeveloperTools/README.md)

### 3. Validation Tool

```bash
#!/bin/bash
# validate_ava.sh - Validate v2.0 format

FILE="$1"

# Check file exists
if [ ! -f "$FILE" ]; then
    echo "❌ File not found: $FILE"
    exit 1
fi

# Check format
if ! head -1 "$FILE" | grep -qE '^(#|---)'; then
    echo "❌ Invalid header (must start with # or ---)"
    exit 1
fi

# Check delimiters
DELIM_COUNT=$(grep -c '^---$' "$FILE")
if [ "$DELIM_COUNT" -ne 3 ]; then
    echo "❌ Expected 3 delimiters (---), found $DELIM_COUNT"
    exit 1
fi

# Check schema
if ! grep -q '^schema: avu-1.0' "$FILE"; then
    echo "❌ Missing schema declaration"
    exit 1
fi

# Check examples format
if ! grep -qE '^[A-Z]{2,4}:[a-z_]+:.+$' "$FILE"; then
    echo "⚠️  No valid examples found"
fi

echo "✅ Valid v2.0 format: $FILE"
```

---

## Related Documentation

### AVA Format Documentation
- **[AON Format Specification](../../DeveloperTools/docs/AON-FORMAT-SPEC.md)** - Ava Optimized NLU format (.aon)
- **[ALM Processing Guide](../../DeveloperTools/docs/ALM-PROCESSING-GUIDE.md)** - Ava Language Model format (.alm)
- **[Format Registry](../../DeveloperTools/FORMAT-REGISTRY.md)** - Complete format mapping table
- **[TVM Compilation Guide](../../DeveloperTools/docs/TVM-COMPILATION-GUIDE.md)** - TVM 0.22 optimization

### AVA Model Toolchain
- **[Toolchain Overview](../../DeveloperTools/README.md)** - Main documentation
- **[Complete UI Design](../../DeveloperTools/COMPLETE-UI-DESIGN.md)** - GUI mockups
- **[Splash Screen Design](../../DeveloperTools/SPLASH-SCREEN-DESIGN.md)** - World-class splash screen
- **[Developer Guide](../../DeveloperTools/docs/DEVELOPER-GUIDE.md)** - Extending the toolchain
- **[Documentation Index](../../DeveloperTools/docs/INDEX.md)** - Complete doc catalog

### AVA Project Documentation
- **[ADR 0001](../decisions/ADR-0001-Universal-Format-v2.md)** - Universal Format v2.0 Migration Decision
- **[NLU README](../../Universal/AVA/Features/NLU/README.md)** - NLU module documentation
- **[RAG README](../../Universal/AVA/Features/RAG/README.md)** - RAG module with AON embeddings
- **[Architecture Overview](../ARCHITECTURE.md)** - AVA system architecture

### External Resources
- **[ONNX Documentation](https://onnx.ai/onnx/)** - ONNX format (basis for AON)
- **[TVM Documentation](https://tvm.apache.org/docs/)** - Apache TVM (used for AON optimization)
- **[MLC-LLM GitHub](https://github.com/mlc-ai/mlc-llm)** - MLC-LLM (basis for ALM)

---

**Copyright:** © 2025 Intelligent Devices LLC / Manoj Jhawar
**Questions?** Contact: manoj@ideahq.net
**Last Updated:** 2025-11-24
**Version:** 1.0

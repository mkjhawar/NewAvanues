# Chapter 48: AON 3.0 Semantic Ontology Format

**Version**: 3.0.0
**Author**: AVA AI Team
**Last Updated**: 2025-11-26

---

## Overview

AON (AVA Ontology) 3.0 is a semantic intent ontology format that enables zero-shot intent classification with GPU acceleration. It defines intents, their semantic descriptions, action sequences, and entity extraction patterns.

> **Note on Naming**: AON refers to the format specification (AVA Ontology), while `.aot` is the file extension (AVA Ontology Template/Text). Do not confuse `.aot` files with `.AON` files (uppercase), which are binary wrapper packages used for model distribution.

### Key Features

- **Zero-shot Classification**: Semantic descriptions enable classification without training
- **GPU Acceleration**: Works with Vulkan/OpenCL/NNAPI backends via InferenceBackendSelector
- **Multi-step Actions**: Complex intents can define action sequences
- **Entity Extraction**: Built-in patterns for extracting parameters
- **App Resolution**: Capability-based app package mapping

---

## File Structure

### Directory Layout

```
apps/ava-standalone/src/main/assets/
└── ontology/
    └── en-US/                      # Locale directory
        ├── communication.aot       # Email, text, call intents
        ├── device_control.aot      # Lights, volume, settings
        ├── media.aot               # Music, video playback
        ├── navigation.aot          # Maps, directions
        └── productivity.aot        # Calendar, reminders, notes
```

### File Naming Convention

```
{category}.aot
```

- All lowercase
- Category matches the `metadata.category` field
- `.aot` extension required (AVA Ontology Template/Text)

---

## AON 3.0 Schema

### Root Structure

```json
{
  "schema": "ava-ontology-3.0",
  "version": "3.0.0",
  "locale": "en-US",
  "metadata": { ... },
  "ontology": [ ... ],
  "global_synonyms": { ... },
  "capability_mappings": { ... }
}
```

### Metadata Object

```json
{
  "metadata": {
    "filename": "device_control.aot",
    "category": "device_control",
    "name": "Device Control Intents",
    "description": "Smart home, device settings, and system control",
    "ontology_count": 8,
    "author": "AVA AI",
    "created_at": "2025-11-26T00:00:00Z"
  }
}
```

### Ontology Entry

```json
{
  "id": "control_lights",
  "canonical_form": "adjust_lighting",
  "description": "User wants to control lights - turn on, off, dim, or change color",
  "synonyms": [
    "turn on lights",
    "turn off lights",
    "lights on",
    "dim the lights"
  ],
  "action_type": "single_step",
  "action_sequence": [
    {
      "step": 1,
      "action": "CONTROL_DEVICE",
      "device_type": "light",
      "extract_entities": ["state", "brightness", "location"]
    }
  ],
  "required_capabilities": ["smart_home"],
  "entity_schema": {
    "state": {
      "type": "ENUM",
      "values": ["on", "off"],
      "patterns": ["turn {state}", "{state} the lights"],
      "optional": false
    },
    "brightness": {
      "type": "PERCENTAGE",
      "patterns": ["to {brightness}", "dim to {brightness}"],
      "optional": true
    }
  },
  "priority": 1,
  "tags": ["lights", "smart_home", "control"]
}
```

---

## Field Reference

### Intent Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique intent identifier (snake_case) |
| `canonical_form` | string | Yes | Normalized intent representation |
| `description` | string | Yes | Semantic description for zero-shot |
| `synonyms` | string[] | Yes | Alternative phrasings |
| `action_type` | enum | Yes | `single_step` or `multi_step` |
| `action_sequence` | array | Yes | Steps to execute intent |
| `required_capabilities` | string[] | Yes | Required device capabilities |
| `entity_schema` | object | No | Entity extraction definitions |
| `priority` | int | No | Disambiguation priority (1=highest) |
| `tags` | string[] | No | Searchable tags |

### Action Types

| Type | Description | Example |
|------|-------------|---------|
| `single_step` | One action | Toggle flashlight |
| `multi_step` | Multiple actions | Open app, then create event |

### Entity Types

| Type | Description | Example |
|------|-------------|---------|
| `ENUM` | Fixed values | `["on", "off"]` |
| `TEXT` | Free-form text | Note content |
| `PERSON` | Contact name | "John" |
| `LOCATION` | Place name | "kitchen", "home" |
| `TIME` | Time value | "7:30 AM" |
| `DATE` | Date value | "tomorrow" |
| `DATETIME` | Date and time | "tomorrow at 3pm" |
| `DURATION` | Time span | "5 minutes" |
| `PERCENTAGE` | 0-100 value | "50%" |
| `MEDIA_TITLE` | Song/video name | "Bohemian Rhapsody" |

---

## GPU Acceleration

### How It Works

1. **Load AON files** at app startup via `AonLoader`
2. **Compute embeddings** for each intent's semantic description
3. **Store embeddings** in SQLite database
4. **At runtime**: Compute user query embedding → cosine similarity → best match

### Backend Selection

AON 3.0 works with `InferenceBackendSelector`:

```kotlin
val backend = InferenceBackendSelector.selectNLUBackend(context)
// Returns: QNN_HTP (Qualcomm) > NNAPI > CPU
```

### Performance

| Device | Backend | Intent Classification |
|--------|---------|----------------------|
| Snapdragon 8 Gen 2 | QNN/HTP | ~15ms |
| Snapdragon 625 | NNAPI | ~40ms |
| Any Android 8.1+ | NNAPI | ~25-50ms |
| Fallback | CPU | ~100-150ms |

---

## Creating New AON Files

### Step 1: Create File

```bash
# Create new category file
touch apps/ava-standalone/src/main/assets/ontology/en-US/{category}.aot
```

### Step 2: Add Schema Header

```json
{
  "schema": "ava-ontology-3.0",
  "version": "3.0.0",
  "locale": "en-US",
  "metadata": {
    "filename": "{category}.aot",
    "category": "{category}",
    "name": "{Category} Intents",
    "description": "Description of this category",
    "ontology_count": 0,
    "author": "Your Name",
    "created_at": "2025-11-26T00:00:00Z"
  },
  "ontology": [],
  "global_synonyms": {},
  "capability_mappings": {}
}
```

### Step 3: Add Intent Entries

Add entries to the `ontology` array. Each entry must have:
- Unique `id`
- Semantic `description` (critical for zero-shot)
- At least 5-10 `synonyms`
- Action sequence

### Step 4: Update Metadata

Update `ontology_count` to match array length.

### Step 5: Validate

```bash
# Build and run to validate parsing
./gradlew :apps:ava-standalone:assembleDebug
```

---

## Parsing AON Files

### Kotlin Usage

```kotlin
// Parse single file
val parser = AonFileParser(context)
val result = parser.parseAonFile("ontology/en-US/device_control.aot")

when (result) {
    is Result.Success -> {
        val aonFile = result.data
        Log.i(TAG, "Loaded ${aonFile.ontologies.size} intents")

        // Insert into database
        for (ontology in aonFile.ontologies) {
            database.semanticOntologyDao().insert(ontology)
        }
    }
    is Result.Error -> {
        Log.e(TAG, "Failed: ${result.message}")
    }
}
```

### Load All Files

```kotlin
// Load all .aot files in locale directory
val allFiles = parser.loadAllAonFiles("ontology/en-US")
when (allFiles) {
    is Result.Success -> {
        val totalIntents = allFiles.data.sumOf { it.ontologies.size }
        Log.i(TAG, "Loaded $totalIntents intents from ${allFiles.data.size} files")
    }
}
```

---

## Best Practices

### Writing Semantic Descriptions

**Good** (specific, action-oriented):
```
"User wants to control lights - turn on, off, dim, or change color"
```

**Bad** (vague, noun-only):
```
"Lights"
```

### Synonym Coverage

Include variations:
- Formal: "turn on the lights"
- Casual: "lights on"
- Imperative: "switch on light"
- Question: "can you turn on the lights"

### Entity Patterns

Use `{entity_name}` placeholder:
```json
"patterns": ["turn {state}", "{state} the lights", "lights {state}"]
```

### Priority Assignment

| Priority | Use Case |
|----------|----------|
| 1 | Primary/common intents |
| 2 | Secondary/less common |
| 3 | Fallback/rare intents |

---

## Troubleshooting

### Schema Validation Error

```
Invalid schema: ava-ontology-2.0, expected: ava-ontology-3.0
```

**Fix**: Update file to use `"schema": "ava-ontology-3.0"`

### Intent Not Recognized

1. Check semantic `description` is specific
2. Add more `synonyms`
3. Lower `priority` value
4. Rebuild embeddings: clear app data

### Slow Classification

1. Check backend: `InferenceBackendSelector.getDeviceSummary(context)`
2. Ensure NNAPI enabled in device settings
3. Pre-compute embeddings at install time

---

## Related Documentation

- [ADR-008: Hardware-Aware Inference Backend](architecture/android/ADR-008-Hardware-Aware-Inference-Backend.md)
- [Chapter 47: GPU Acceleration](Developer-Manual-Chapter47-GPU-Acceleration.md)
- [ADR-003: ONNX NLU Integration](architecture/android/ADR-003-ONNX-NLU-Integration.md)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2025-11-26 | Initial AON 3.0 release with GPU acceleration |

---

**Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis**

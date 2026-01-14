# Developer Manual - Chapter 51: 3-Letter JSON Schema Standard

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Related ADR**: ADR-011-3Letter-JSON-Schema-Standard

---

## Table of Contents

1. [Overview](#1-overview)
2. [Why 3-Letter Schema?](#2-why-3-letter-schema)
3. [Global Key Registry](#3-global-key-registry)
4. [Schema Types](#4-schema-types)
5. [Validation](#5-validation)
6. [Kotlin Integration](#6-kotlin-integration)
7. [Migration Guide](#7-migration-guide)
8. [Best Practices](#8-best-practices)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Overview

The AVA Ecosystem uses a standardized **3-Letter JSON Schema** format for all configuration files across AVA, VoiceOS, AVAConnect, and Avanues projects. This standard balances human readability with compact file sizes.

### Key Features

- **Human Readable**: 3-letter keys are intuitive (`sch` = schema, `ver` = version)
- **Compact**: ~50% size reduction vs verbose JSON
- **Ecosystem-Wide**: Same keys across all AVA projects
- **Standard JSON**: Works with any JSON library (no custom syntax)
- **Extensible**: Easy to add new 3-letter keys
- **Validated**: JSON Schema files for IDE autocomplete and validation

### Format Comparison

| Format | Example | Size | Readability | Ecosystem |
|--------|---------|------|-------------|-----------|
| **Verbose** | `"schema": "ava-llm-1.0"` | 100% | ⭐⭐⭐⭐⭐ | ❌ Inconsistent |
| **3-Letter** | `"sch": "ava-llm-1.0"` | 50% | ⭐⭐⭐⭐ | ✅ Standard |
| **1-Letter** | `"s": "ava-llm-1.0"` | 45% | ⭐ | ❌ Legacy only |

**Decision**: 3-letter provides optimal balance between size and readability.

---

## 2. Why 3-Letter Schema?

### The Problem

Before v1.1.0, AVA had **three different JSON formats**:

1. **Verbose JSON**: Full words (`"version": "1.0.0"`) - LLM configs
2. **Single-Letter**: Cryptic (`"v": "1.0.0"`) - .aot intent files
3. **Mixed**: No standard across ecosystem

**Result**: Confusion, parsing errors, maintenance burden

### The Solution

Establish a **3-letter standard** across the entire ecosystem:

```json
{
  "sch": "ava-llm-1.0",    // schema (obvious)
  "ver": "1.0.0",          // version (clear)
  "met": {                 // metadata (understandable)
    "nam": "Phi-2-Q4",     // name (intuitive)
    "typ": "phi",          // type (familiar)
    "siz": 1572864000      // size (concise)
  }
}
```

### Benefits

✅ **~50% Size Reduction** vs verbose JSON
✅ **Human Readable** - Developers understand keys without docs
✅ **Ecosystem Consistency** - Same keys across all projects
✅ **Debuggable** - Clear keys make troubleshooting easier
✅ **Professional** - Uses industry-standard abbreviations
✅ **Parseable** - Standard JSON (any library works)

### Why Not Single-Letter?

**Single-letter format** (used in legacy .aot files):
- ❌ Not human-readable: `"s"`, `"v"`, `"l"`, `"m"` require documentation
- ❌ Ambiguous: `"m"` could mean metadata, model, or message
- ❌ Debug-unfriendly: Troubleshooting JSON errors is painful

**3-letter is only 10% larger but vastly more usable.**

---

## 3. Global Key Registry

### Mandatory Root Keys

**All schemas MUST include:**

```json
{
  "sch": "ava-{type}-{version}",  // Schema identifier
  "ver": "X.Y.Z"                   // File version (semver)
}
```

**Example:**
```json
{
  "sch": "ava-llm-1.0",
  "ver": "1.2.3"
}
```

---

### Common Keys (30+ Defined)

| 3-Letter | Full Word | Type | Usage |
|----------|-----------|------|-------|
| **Core** ||||
| `sch` | schema | string | Schema identifier (e.g., "ava-llm-1.0") |
| `ver` | version | string | File version (semver: "1.0.0") |
| `met` | metadata | object | Metadata container |
| `cfg` | config | object | Configuration container |
| `sta` | stats | object | Statistics container |
| **Identifiers** ||||
| `nam` | name | string | Name/identifier |
| `typ` | type | string | Type/category |
| `src` | source | string | Source identifier |
| `cat` | category | string | Category/classification |
| `pri` | priority | number | Priority level (1-10) |
| **Model Metadata** ||||
| `arc` | architecture | string | Model architecture (e.g., "microsoft/phi-2") |
| `dim` | dimensions | number | Embedding dimensions (e.g., 384) |
| `siz` | size | number | Size in bytes |
| `qnt` | quantization | string | Quantization method (e.g., "INT8") |
| `ctx` | context | number | Context length/window |
| **LLM Parameters** ||||
| `tmp` | temperature | number | Temperature (0.0-2.0) |
| `top` | top_p | number | Top-p sampling (0.0-1.0) |
| `rep` | repetition_penalty | number | Repetition penalty (1.0-2.0) |
| `tok` | tokenizer | object | Tokenizer configuration |
| **Files & Batching** ||||
| `fil` | files | array | File list |
| `bat` | batch | number | Batch size |
| `seq` | sequence | number | Sequence length |
| **Localization** ||||
| `loc` | locale | string | Language locale (e.g., "en-US") |
| `lan` | languages | array | Language list |
| **Counting** ||||
| `cnt` | count | number | Count/number of items |
| `pag` | pages | number | Page count |
| **RAG-Specific** ||||
| `doc` | document | string | Document reference |
| `tit` | title | string | Title |
| `aut` | author | string | Author |
| `dat` | date | string | Date (ISO 8601) |
| `chu` | chunk | number | Chunk size (tokens) |
| `ovr` | overlap | number | Overlap size (tokens) |
| `emb` | embedding | string | Embedding model reference |
| `idx` | index | string | Index type/reference |
| **Intent-Specific** ||||
| `int` | intents | array | Intents array |
| `syn` | synonyms | array/object | Synonyms list |
| `can` | canonical | string | Canonical form |
| `tag` | tags | array | Tag list |
| **Other** ||||
| `dev` | device | string | Device type (e.g., "opencl") |
| `nor` | normalize | boolean | Normalize flag |
| `des` | description | string | Description text |

---

### Adding New Keys

**Process:**

1. **Check Registry**: Ensure key doesn't exist
2. **Choose 3 Letters**: Use first 3 letters of full word
3. **Document**: Add to AVA-3LETTER-JSON-SCHEMA.md
4. **Update JSON Schema**: Add to validation schema
5. **Announce**: Notify team via documentation update

**Example:**
```
Full Word: "threshold"
3-Letter: "thr"
Type: number
Usage: Threshold value for classification
```

---

## 4. Schema Types

AVA defines **4 primary schema types** for ecosystem configuration:

### 4.1. LLM Model Config (`ava-llm-1.0`)

**Purpose**: Local LLM model configuration

**Structure:**
```json
{
  "sch": "ava-llm-1.0",
  "ver": "1.0.0",
  "met": {
    "nam": "Phi-2-Q4",
    "arc": "microsoft/phi-2",
    "typ": "phi",
    "siz": 1572864000,
    "qnt": "Q4_K_M",
    "ctx": 2048
  },
  "cfg": {
    "tmp": 0.7,
    "top": 0.95,
    "rep": 1.1,
    "bat": 1,
    "dev": "opencl"
  },
  "tok": {
    "fil": ["tokenizer.model", "tokenizer.json"],
    "met": "byte_fallback"
  }
}
```

**File Location**: `.llm/{model-name}/ava-model-config.json`

**Size**: ~200 bytes (vs ~400 bytes verbose)

---

### 4.2. Embedding Model Config (`ava-emb-1.0`)

**Purpose**: Embedding model configuration (NLU + RAG)

**Structure:**
```json
{
  "sch": "ava-emb-1.0",
  "ver": "1.0.0",
  "met": {
    "nam": "MiniLM-Multi-INT8",
    "arc": "intfloat/multilingual-e5-small",
    "typ": "minilm",
    "dim": 384,
    "qnt": "INT8",
    "lan": ["en", "es", "fr", "de", "zh", "ja"]
  },
  "cfg": {
    "seq": 512,
    "bat": 32,
    "nor": true
  }
}
```

**File Location**: `.embeddings/{model-name}.json`

---

### 4.3. Intent Ontology (`ava-aot-3.0`)

**Purpose**: Semantic intent definitions

**Structure:**
```json
{
  "sch": "ava-aot-3.0",
  "ver": "3.0.0",
  "loc": "en-US",
  "met": {
    "fil": "device-control.aot",
    "cat": "device_control",
    "nam": "Device Control",
    "des": "Smart device control intents",
    "cnt": 8
  },
  "int": [
    {
      "id": "control_lights",
      "can": "turn on lights",
      "syn": ["lights on", "enable lights"],
      "cat": "device_control",
      "pri": 1,
      "tag": ["lights", "smart_home"]
    }
  ]
}
```

**File Location**: `assets/ontology/en-US/{category}.aot`

**Note**: .aot files currently use single-letter format for backward compatibility. Will migrate to 3-letter in v4.0.

---

### 4.4. RAG Document Metadata (`ava-rag-1.0`)

**Purpose**: Document metadata for RAG system

**Structure:**
```json
{
  "sch": "ava-rag-1.0",
  "ver": "1.0.0",
  "met": {
    "doc": "user-manual.pdf",
    "tit": "AVA User Manual",
    "aut": "Augmentalis",
    "dat": "2025-11-27T10:30:00Z",
    "siz": 5242880,
    "pag": 150
  },
  "cfg": {
    "chu": 512,
    "ovr": 128,
    "emb": "AVA-384-MiniLM-Multi-INT8.AON",
    "idx": "faiss"
  },
  "sta": {
    "chu_cnt": 450,
    "vec_cnt": 450,
    "idx_siz": 1728000
  }
}
```

**File Location**: `app-specific-storage/rag/documents/{doc-id}.json`

---

## 5. Validation

### JSON Schema Validation

AVA provides JSON Schema files for IDE validation and autocomplete.

**File:** `ava-llm-1.0.schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["sch", "ver"],
  "properties": {
    "sch": {
      "type": "string",
      "pattern": "^ava-llm-[0-9]+\\.[0-9]+$"
    },
    "ver": {
      "type": "string",
      "pattern": "^[0-9]+\\.[0-9]+\\.[0-9]+$"
    },
    "met": {
      "type": "object",
      "required": ["nam", "typ"],
      "properties": {
        "nam": { "type": "string" },
        "arc": { "type": "string" },
        "typ": { "type": "string" },
        "siz": { "type": "number" },
        "qnt": { "type": "string" },
        "ctx": { "type": "number" }
      }
    },
    "cfg": {
      "type": "object",
      "properties": {
        "tmp": { "type": "number", "minimum": 0, "maximum": 2 },
        "top": { "type": "number", "minimum": 0, "maximum": 1 },
        "rep": { "type": "number", "minimum": 1, "maximum": 2 },
        "bat": { "type": "integer", "minimum": 1 },
        "dev": { "type": "string" }
      }
    }
  }
}
```

**VS Code Setup:**

Add to `.vscode/settings.json`:
```json
{
  "json.schemas": [
    {
      "fileMatch": ["**/ava-model-config.json"],
      "url": "./schemas/ava-llm-1.0.schema.json"
    },
    {
      "fileMatch": ["**/.embeddings/*.json"],
      "url": "./schemas/ava-emb-1.0.schema.json"
    }
  ]
}
```

---

### Gradle Validation Task

**Add to `build.gradle.kts`:**

```kotlin
tasks.register("validate3LetterSchema") {
    doLast {
        val schemasDir = file("$projectDir/assets/ontology")
        val schemaFiles = schemasDir.walk()
            .filter { it.extension == "aot" || it.name.endsWith(".json") }
            .toList()

        schemaFiles.forEach { file ->
            val json = file.readText()
            val parsed = JSONObject(json)

            // Check mandatory keys
            require(parsed.has("sch")) { "${file.name}: Missing 'sch' key" }
            require(parsed.has("ver")) { "${file.name}: Missing 'ver' key" }

            // Validate schema format
            val schema = parsed.getString("sch")
            require(schema.matches(Regex("^ava-\\w+-\\d+\\.\\d+$"))) {
                "${file.name}: Invalid schema format: $schema"
            }

            // Validate version format
            val version = parsed.getString("ver")
            require(version.matches(Regex("^\\d+\\.\\d+\\.\\d+$"))) {
                "${file.name}: Invalid version format: $version"
            }

            println("✓ ${file.name}: Valid")
        }

        println("\n✅ All schemas validated successfully")
    }
}
```

**Run:**
```bash
./gradlew validate3LetterSchema
```

---

## 6. Kotlin Integration

### Data Classes

**LLM Config:**

```kotlin
@Serializable
data class AvaLLMConfig(
    val sch: String,  // Schema
    val ver: String,  // Version
    val met: Metadata,
    val cfg: Config,
    val tok: Tokenizer? = null
) {
    @Serializable
    data class Metadata(
        val nam: String,       // Name
        val arc: String,       // Architecture
        val typ: String,       // Type
        val siz: Long,         // Size
        val qnt: String,       // Quantization
        val ctx: Int           // Context
    )

    @Serializable
    data class Config(
        val tmp: Float,        // Temperature
        val top: Float,        // Top-p
        val rep: Float,        // Repetition penalty
        val bat: Int,          // Batch
        val dev: String        // Device
    )

    @Serializable
    data class Tokenizer(
        val fil: List<String>, // Files
        val met: String        // Method
    )
}
```

---

**Embedding Config:**

```kotlin
@Serializable
data class AvaEmbeddingConfig(
    val sch: String,
    val ver: String,
    val met: Metadata,
    val cfg: Config
) {
    @Serializable
    data class Metadata(
        val nam: String,       // Name
        val arc: String,       // Architecture
        val typ: String,       // Type
        val dim: Int,          // Dimensions
        val qnt: String,       // Quantization
        val lan: List<String>  // Languages
    )

    @Serializable
    data class Config(
        val seq: Int,          // Sequence length
        val bat: Int,          // Batch size
        val nor: Boolean       // Normalize
    )
}
```

---

**RAG Metadata:**

```kotlin
@Serializable
data class AvaRAGMetadata(
    val sch: String,
    val ver: String,
    val met: Metadata,
    val cfg: Config,
    val sta: Stats
) {
    @Serializable
    data class Metadata(
        val doc: String,       // Document filename
        val tit: String,       // Title
        val aut: String,       // Author
        val dat: String,       // Date (ISO 8601)
        val siz: Long,         // Size in bytes
        val pag: Int           // Pages
    )

    @Serializable
    data class Config(
        val chu: Int,          // Chunk size
        val ovr: Int,          // Overlap size
        val emb: String,       // Embedding model
        val idx: String        // Index type
    )

    @Serializable
    data class Stats(
        val chu_cnt: Int,      // Chunk count
        val vec_cnt: Int,      // Vector count
        val idx_siz: Long      // Index size
    )
}
```

---

### Parsing Example

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

// Parse LLM config
val jsonString = File("ava-model-config.json").readText()
val config = Json.decodeFromString<AvaLLMConfig>(jsonString)

println("Model: ${config.met.nam}")
println("Architecture: ${config.met.arc}")
println("Type: ${config.met.typ}")
println("Size: ${config.met.siz} bytes")
println("Temperature: ${config.cfg.tmp}")
```

---

### Writing Example

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

// Create config
val config = AvaLLMConfig(
    sch = "ava-llm-1.0",
    ver = "1.0.0",
    met = AvaLLMConfig.Metadata(
        nam = "Phi-2-Q4",
        arc = "microsoft/phi-2",
        typ = "phi",
        siz = 1572864000L,
        qnt = "Q4_K_M",
        ctx = 2048
    ),
    cfg = AvaLLMConfig.Config(
        tmp = 0.7f,
        top = 0.95f,
        rep = 1.1f,
        bat = 1,
        dev = "opencl"
    )
)

// Serialize to JSON
val json = Json {
    prettyPrint = true
    encodeDefaults = false
}
val jsonString = json.encodeToString(config)

// Write to file
File("ava-model-config.json").writeText(jsonString)
```

---

## 7. Migration Guide

### Migrating from Verbose JSON

**Before (Verbose):**
```json
{
  "schema": "ava-llm-1.0",
  "version": "1.0.0",
  "metadata": {
    "name": "Phi-2-Q4",
    "architecture": "microsoft/phi-2",
    "type": "phi",
    "size": 1572864000,
    "quantization": "Q4_K_M",
    "context_length": 2048
  },
  "config": {
    "temperature": 0.7,
    "top_p": 0.95,
    "repetition_penalty": 1.1,
    "batch_size": 1,
    "device": "opencl"
  }
}
```

**After (3-Letter):**
```json
{
  "sch": "ava-llm-1.0",
  "ver": "1.0.0",
  "met": {
    "nam": "Phi-2-Q4",
    "arc": "microsoft/phi-2",
    "typ": "phi",
    "siz": 1572864000,
    "qnt": "Q4_K_M",
    "ctx": 2048
  },
  "cfg": {
    "tmp": 0.7,
    "top": 0.95,
    "rep": 1.1,
    "bat": 1,
    "dev": "opencl"
  }
}
```

**Size Comparison**: 400 bytes → 200 bytes (50% reduction)

---

### Conversion Tool

**Script:** `scripts/convert-to-3letter.py`

```python
import json
import sys

MAPPING = {
    "schema": "sch",
    "version": "ver",
    "metadata": "met",
    "config": "cfg",
    "name": "nam",
    "type": "typ",
    "architecture": "arc",
    "dimensions": "dim",
    "size": "siz",
    "quantization": "qnt",
    "context_length": "ctx",
    "temperature": "tmp",
    "top_p": "top",
    "repetition_penalty": "rep",
    "batch_size": "bat",
    "device": "dev"
}

def convert_keys(obj):
    if isinstance(obj, dict):
        return {MAPPING.get(k, k): convert_keys(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [convert_keys(item) for item in obj]
    else:
        return obj

if __name__ == "__main__":
    input_file = sys.argv[1]
    output_file = sys.argv[2]

    with open(input_file, 'r') as f:
        data = json.load(f)

    converted = convert_keys(data)

    with open(output_file, 'w') as f:
        json.dump(converted, f, indent=2)

    print(f"✅ Converted: {input_file} → {output_file}")
```

**Usage:**
```bash
python scripts/convert-to-3letter.py old-config.json new-config.json
```

---

### Backward Compatibility

**Parser with Fallback:**

```kotlin
fun parseConfigWithFallback(jsonString: String): AvaLLMConfig {
    return try {
        // Try 3-letter format first
        Json.decodeFromString<AvaLLMConfig>(jsonString)
    } catch (e: Exception) {
        // Fallback to verbose format
        val verboseConfig = Json.decodeFromString<VerboseLLMConfig>(jsonString)
        verboseConfig.to3Letter()
    }
}

// Extension function
fun VerboseLLMConfig.to3Letter(): AvaLLMConfig {
    return AvaLLMConfig(
        sch = this.schema,
        ver = this.version,
        met = AvaLLMConfig.Metadata(
            nam = this.metadata.name,
            arc = this.metadata.architecture,
            // ... map all fields
        ),
        // ...
    )
}
```

---

## 8. Best Practices

### 1. Always Include Mandatory Keys

```json
{
  "sch": "ava-llm-1.0",   ✅ Required
  "ver": "1.0.0",         ✅ Required
  "met": { ... }
}
```

❌ **Bad:**
```json
{
  "met": { "nam": "Phi-2" }
  // Missing sch and ver
}
```

---

### 2. Use Semantic Versioning

```json
{
  "ver": "1.2.3"  ✅ semver (MAJOR.MINOR.PATCH)
}
```

❌ **Bad:**
```json
{
  "ver": "1.0"    ❌ Not semver
}
```

---

### 3. Omit Optional Keys When Default

```json
{
  "cfg": {
    "tmp": 0.7,
    "top": 0.95
    // Omit bat, dev if using defaults
  }
}
```

✅ **Good:** Smaller file size

❌ **Bad:**
```json
{
  "cfg": {
    "tmp": 0.7,
    "top": 0.95,
    "bat": 1,     // Default value, unnecessary
    "dev": "cpu"  // Default value, unnecessary
  }
}
```

---

### 4. Use Descriptive Values

```json
{
  "qnt": "Q4_K_M"    ✅ Clear quantization method
}
```

❌ **Bad:**
```json
{
  "qnt": "q4"        ❌ Ambiguous
}
```

---

### 5. Document Custom Keys

If adding new keys, document in code:

```kotlin
@Serializable
data class CustomConfig(
    val sch: String,
    val ver: String,
    val cus: String  // Custom key: customer ID
    // TODO: Add 'cus' to AVA-3LETTER-JSON-SCHEMA.md
)
```

---

## 9. Troubleshooting

### Issue: JSON Parsing Fails

**Error:**
```
kotlinx.serialization.MissingFieldException: Field 'sch' is required
```

**Cause**: Missing mandatory `sch` or `ver` keys

**Solution:**
```json
{
  "sch": "ava-llm-1.0",  ← Add this
  "ver": "1.0.0",        ← Add this
  "met": { ... }
}
```

---

### Issue: IDE Doesn't Autocomplete Keys

**Cause**: Missing JSON Schema validation setup

**Solution:**

1. Add schema file to project:
   ```
   schemas/ava-llm-1.0.schema.json
   ```

2. Configure VS Code (`.vscode/settings.json`):
   ```json
   {
     "json.schemas": [
       {
         "fileMatch": ["**/ava-model-config.json"],
         "url": "./schemas/ava-llm-1.0.schema.json"
       }
     ]
   }
   ```

3. Restart VS Code

---

### Issue: Unknown Key Warning

**Warning:**
```
Property 'xyz' is not expected here
```

**Cause**: Key not in schema definition

**Solution:**

1. Check if key is in global registry (Section 3)
2. If missing, add to `AVA-3LETTER-JSON-SCHEMA.md`
3. Update JSON Schema file
4. Commit changes

---

### Issue: Type Mismatch

**Error:**
```
Expected number, got string
```

**JSON:**
```json
{
  "siz": "1572864000"  ❌ String
}
```

**Fix:**
```json
{
  "siz": 1572864000    ✅ Number
}
```

---

## Related Documentation

- **ADR-011**: [3-Letter JSON Schema Standard](architecture/shared/ADR-011-3Letter-JSON-Schema-Standard.md)
- **Full Standard**: [docs/standards/AVA-3LETTER-JSON-SCHEMA.md](standards/AVA-3LETTER-JSON-SCHEMA.md)
- **Developer Manual Chapter 50**: [External Storage Migration](Developer-Manual-Chapter50-External-Storage-Migration.md)
- **Developer Manual Chapter 52**: [RAG System Architecture](Developer-Manual-Chapter52-RAG-System-Architecture.md)

---

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Maintained By**: AVA AI Team

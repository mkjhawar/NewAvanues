# AVA 3-Letter JSON Schema Standard

**Version:** 1.0
**Date:** 2025-11-27
**Status:** MANDATORY
**Applies To:** All AVA ecosystem JSON configuration files

---

## Overview

The **3-Letter JSON Schema** is AVA's compact, human-readable JSON format standard used across the entire ecosystem (AVA, VoiceOS, AVAConnect, Avanues).

**Key Benefits:**
- ✅ **Human Readable** - Developers can understand without documentation
- ✅ **Compact** - ~50% smaller than verbose JSON
- ✅ **Ecosystem Consistency** - Same format across all projects
- ✅ **LLM/NLU Friendly** - Easy to parse, minimal overhead
- ✅ **Better Debugging** - Clear keys make troubleshooting easier

---

## Design Principles

### 1. Three-Letter Abbreviations
- All keys MUST be exactly 3 letters (except special cases)
- Use common abbreviations where possible
- Prioritize readability over extreme brevity

### 2. Hierarchy
```
Root Level      : 3-letter keys (sch, ver, met, cfg)
Nested Objects  : 3-letter keys (nam, typ, arc, dim)
Arrays          : Full descriptive names OK
Values          : Standard JSON types
```

### 3. Reserved Keys (All Schemas)
```json
{
  "sch": "ava-{type}-{version}",  // Schema identifier (MANDATORY)
  "ver": "X.Y.Z",                  // File version (MANDATORY)
  "met": {...},                    // Metadata object (OPTIONAL)
  "cfg": {...}                     // Configuration object (OPTIONAL)
}
```

---

## Global 3-Letter Key Registry

| 3-Letter | Full Word | Usage | Data Type |
|----------|-----------|-------|-----------|
| `sch` | schema | Schema identifier | string |
| `ver` | version | File/format version | string (semver) |
| `met` | metadata | Metadata container | object |
| `cfg` | config | Configuration container | object |
| `nam` | name | Name/identifier | string |
| `typ` | type | Type/category | string |
| `arc` | architecture | Model architecture | string |
| `dim` | dimensions | Embedding dimensions | number |
| `siz` | size | Size in bytes | number |
| `qnt` | quantization | Quantization method | string |
| `ctx` | context | Context length/window | number |
| `tmp` | temperature | Temperature parameter | number |
| `top` | top_p | Top-p sampling | number |
| `rep` | repetition_penalty | Repetition penalty | number |
| `tok` | tokenizer | Tokenizer config | object |
| `fil` | files | File list | array |
| `bat` | batch | Batch size | number |
| `seq` | sequence | Sequence length | number |
| `loc` | locale | Language locale | string |
| `cnt` | count | Count/number | number |
| `pri` | priority | Priority level | number |
| `cat` | category | Category/classification | string |
| `src` | source | Source identifier | string |
| `tag` | tags | Tag list | array |
| `syn` | synonyms | Synonyms list | array/object |
| `can` | canonical | Canonical form | string |
| `int` | intents | Intents array | array |

---

## Schema Types

### 1. LLM Model Config: `ava-llm-1.0`

**File:** `ava-model-config.json`

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
    "met": "byte_fallback",
    "pre": false,
    "str": false
  }
}
```

**Metadata Fields (`met`):**
- `nam` - Model name (e.g., "Phi-2-Q4")
- `arc` - Architecture source (e.g., "microsoft/phi-2")
- `typ` - Model type (phi, llama, gemma, qwen)
- `siz` - Size in bytes
- `qnt` - Quantization (Q4_K_M, Q8_0, FP16)
- `ctx` - Context window size

**Config Fields (`cfg`):**
- `tmp` - Temperature (0.0-2.0)
- `top` - Top-p sampling (0.0-1.0)
- `rep` - Repetition penalty (1.0-2.0)
- `bat` - Batch size
- `dev` - Device type (opencl, vulkan, cuda)

**Tokenizer Fields (`tok`):**
- `fil` - Tokenizer files
- `met` - Tokenizer method
- `pre` - Prepend space in encode
- `str` - Strip space in decode

---

### 2. Embedding Model Config: `ava-emb-1.0`

**File:** `ava-embedding-config.json`

```json
{
  "sch": "ava-emb-1.0",
  "ver": "1.0.0",
  "met": {
    "nam": "MiniLM-Multi-INT8",
    "arc": "intfloat/multilingual-e5-small",
    "typ": "minilm",
    "siz": 30000000,
    "dim": 384,
    "qnt": "INT8",
    "lan": ["en", "es", "fr", "de", "zh", "ja", "ar", "hi"]
  },
  "cfg": {
    "seq": 512,
    "bat": 32,
    "nor": true
  }
}
```

**Metadata Fields (`met`):**
- `nam` - Model name
- `arc` - Architecture source
- `typ` - Model type (minilm, mobilebert, malbert)
- `siz` - Size in bytes
- `dim` - Embedding dimensions (384, 768, 1024)
- `qnt` - Quantization (INT8, FP32, FP16)
- `lan` - Supported languages (array)

**Config Fields (`cfg`):**
- `seq` - Max sequence length
- `bat` - Batch size
- `nor` - Normalize embeddings (boolean)

---

### 3. Intent Ontology: `ava-aot-3.0`

**File:** `*.aot` (AVA Ontology Template)

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
  ],
  "syn": {
    "turn_on": ["activate", "enable"],
    "turn_off": ["deactivate", "disable"]
  }
}
```

**Metadata Fields (`met`):**
- `fil` - Filename
- `cat` - Category identifier
- `nam` - Human-readable name
- `des` - Description
- `cnt` - Intent count

**Intent Fields (`int`):**
- `id` - Intent identifier
- `can` - Canonical utterance
- `syn` - Synonym utterances (array)
- `cat` - Category
- `pri` - Priority (1-10)
- `tag` - Tags (array)

---

### 4. RAG Document Metadata: `ava-rag-1.0`

**File:** `ava-rag-metadata.json`

```json
{
  "sch": "ava-rag-1.0",
  "ver": "1.0.0",
  "met": {
    "doc": "user-manual.pdf",
    "tit": "AVA User Manual",
    "aut": "Augmentalis",
    "dat": "2025-11-27",
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

**Metadata Fields (`met`):**
- `doc` - Document filename
- `tit` - Title
- `aut` - Author
- `dat` - Date (ISO 8601)
- `siz` - Size in bytes
- `pag` - Page count

**Config Fields (`cfg`):**
- `chu` - Chunk size (tokens)
- `ovr` - Overlap (tokens)
- `emb` - Embedding model used
- `idx` - Index type (faiss, annoy)

**Stats Fields (`sta`):**
- `chu_cnt` - Chunk count
- `vec_cnt` - Vector count
- `idx_siz` - Index size (bytes)

---

## Naming Conventions

### Rule 1: Use Common Abbreviations
```
Good: cfg, tmp, ctx, ver
Bad:  cnf, tmpr, cntx, vrsn
```

### Rule 2: Consistent Across Schemas
Same concept = same 3-letter key everywhere
```
"ver": "1.0.0"  ← Use in ALL schemas
"nam": "..."    ← Use in ALL metadata
"typ": "..."    ← Use in ALL type fields
```

### Rule 3: No Ambiguity
Each 3-letter key MUST have one meaning
```
Good: tmp = temperature (always)
Bad:  tmp = template (conflicts!)
```

### Rule 4: Special Cases (>3 letters allowed)
- Arrays of objects: Use descriptive names
- Nested schema refs: Full names OK
- External URLs: Full "url" key OK

---

## Migration from Verbose JSON

### Before (Verbose)
```json
{
  "version": "1.0.0",
  "model_type": "phi",
  "quantization": "Q4_K_M",
  "model_config": {
    "temperature": 0.7,
    "top_p": 0.95,
    "context_window_size": 2048
  }
}
```

### After (3-Letter)
```json
{
  "sch": "ava-llm-1.0",
  "ver": "1.0.0",
  "met": {
    "typ": "phi",
    "qnt": "Q4_K_M"
  },
  "cfg": {
    "tmp": 0.7,
    "top": 0.95,
    "ctx": 2048
  }
}
```

**Size Reduction:** ~45-50%

---

## Validation Rules

### Mandatory Fields
All AVA JSON files MUST have:
```json
{
  "sch": "ava-{type}-{version}",  // MANDATORY
  "ver": "X.Y.Z"                   // MANDATORY
}
```

### Schema Identifier Format
```
Pattern: ava-{type}-{major}.{minor}
Examples:
  ava-llm-1.0
  ava-emb-1.0
  ava-aot-3.0
  ava-rag-1.0
```

### Version Format
```
Pattern: {major}.{minor}.{patch}
Examples:
  1.0.0
  2.1.5
  3.0.0-beta
```

---

## Code Example: Parsing 3-Letter Schema

```kotlin
data class AvaLLMConfig(
    val sch: String,  // Schema
    val ver: String,  // Version
    val met: Metadata,
    val cfg: Config,
    val tok: Tokenizer
) {
    data class Metadata(
        val nam: String,       // Name
        val arc: String,       // Architecture
        val typ: String,       // Type
        val siz: Long,         // Size
        val qnt: String,       // Quantization
        val ctx: Int           // Context
    )

    data class Config(
        val tmp: Float,        // Temperature
        val top: Float,        // Top-p
        val rep: Float,        // Repetition penalty
        val bat: Int,          // Batch
        val dev: String        // Device
    )

    data class Tokenizer(
        val fil: List<String>, // Files
        val met: String,       // Method
        val pre: Boolean,      // Prepend space
        val str: Boolean       // Strip space
    )
}

// Parse JSON
val configJson = File("ava-model-config.json").readText()
val config = Json.decodeFromString<AvaLLMConfig>(configJson)

// Access fields
println("Model: ${config.met.nam}")
println("Type: ${config.met.typ}")
println("Temperature: ${config.cfg.tmp}")
```

---

## Enforcement

### Mandatory Use
- ✅ ALL new AVA JSON files MUST use 3-letter schema
- ✅ Legacy files SHOULD be migrated during next update
- ✅ Migration tools available (json-converter-3letter)

### Validation
```bash
# Validate 3-letter schema compliance
./gradlew validate3LetterSchema --file ava-model-config.json

# Convert verbose JSON to 3-letter
./gradlew convertTo3Letter \
  --input mlc-chat-config.json \
  --output ava-model-config.json
```

---

## Related Documentation

- **AVA File Formats:** `docs/Developer-Manual-Chapter37-AVA-File-Format.md`
- **.aot Format:** `docs/standards/AVA-FILE-FORMATS.md`
- **.ALM Format:** `docs/specifications/tvm-v0220-dependency-fix-spec.md`
- **External Storage:** `docs/build/EXTERNAL-STORAGE-SETUP.md`

---

## Changelog

**v1.0 (2025-11-27):**
- Initial 3-letter schema standard
- Defined global key registry
- 4 schema types documented (LLM, Embedding, Intent, RAG)
- Migration guide from verbose JSON

---

**Status:** ✅ PRODUCTION READY
**Enforcement:** MANDATORY for all new JSON files
**Ecosystem:** AVA, VoiceOS, AVAConnect, Avanues

# ADR-011: 3-Letter JSON Schema Standard for AVA Ecosystem

**Status:** Accepted
**Date:** 2025-11-27
**Authors:** AVA AI Team
**Scope:** Ecosystem-wide (AVA, VoiceOS, AVAConnect, Avanues)
**Related:** Developer Manual Chapter 51, All JSON configuration files
**Supersedes:** Single-letter (.ava format) and verbose JSON formats

---

## Context

AVA ecosystem uses JSON for configuration files across multiple domains: LLM models, embeddings, intents, RAG metadata, and system settings. Previously, we had inconsistent formats:

### Problems Identified

1. **Format Inconsistency**: Three different JSON styles in use
   - Verbose JSON: Full words (`"version": "1.0.0"`) - used in LLM configs
   - Single-letter: Cryptic (`"v": "1.0.0"`) - used in .ava intent files
   - Mixed: No standard across ecosystem
   - **Result**: Confusion, parsing errors, maintenance burden

2. **Single-Letter Format Issues**: Introduced in ADR-005 for .ava files
   - Not human-readable: `"s"`, `"v"`, `"l"`, `"m"` require documentation
   - Ambiguous: `"m"` could mean metadata, model, or message
   - Debug-unfriendly: Troubleshooting JSON errors is painful
   - **Size Savings**: 66% reduction, but at usability cost

3. **Verbose JSON Issues**: Standard full-word keys
   - File bloat: `"temperature": 0.7` vs `"tmp": 0.7` (6 extra bytes)
   - Network overhead: Larger downloads
   - Storage cost: Compounds with thousands of files
   - **Size Penalty**: Reference format, but largest

4. **No Ecosystem Standard**: Each project uses different conventions
   - AVA: Mixed (verbose for LLM, single-letter for intents)
   - VoiceOS: Verbose JSON
   - AVAConnect: Custom compact format
   - Avanues: Inconsistent
   - **Result**: Cannot share configs, parsers, or tools

### Requirements

- **Human Readable**: Developers can understand without docs
- **Compact**: ~50% smaller than verbose JSON
- **Consistent**: Same keys across all ecosystem projects
- **Parseable**: Standard JSON (no custom syntax)
- **Debuggable**: Clear error messages
- **Extensible**: Easy to add new keys
- **Backward Compatible**: Gradual migration path

---

## Decision

Adopt a **3-Letter JSON Schema Standard** using common 3-letter abbreviations for all ecosystem JSON files.

### Rationale

**3-Letter Abbreviations** strike the optimal balance:
- **Human Readable**: `"sch"` = schema (obvious)
- **Compact**: ~50% reduction vs verbose
- **Familiar**: Many standard abbreviations (`cfg`, `tmp`, `ver`)
- **Unambiguous**: 3 letters provide enough context

**Comparison:**

| Format | Example | Size | Readability | Ecosystem |
|--------|---------|------|-------------|-----------|
| Verbose | `"schema": "ava-llm-1.0"` | 100% | ⭐⭐⭐⭐⭐ | ❌ Inconsistent |
| 3-Letter | `"sch": "ava-llm-1.0"` | 50% | ⭐⭐⭐⭐ | ✅ Standard |
| 1-Letter | `"s": "ava-llm-1.0"` | 45% | ⭐ | ❌ Legacy only |

---

## Architecture

### Global Key Registry

**Mandatory Root Keys (All Schemas):**
```json
{
  "sch": "ava-{type}-{version}",  // Schema identifier
  "ver": "X.Y.Z"                   // File version
}
```

**Common Keys (30+ Defined):**

| 3-Letter | Full Word | Usage | Type |
|----------|-----------|-------|------|
| `sch` | schema | Schema identifier | string |
| `ver` | version | File version (semver) | string |
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
| `doc` | document | Document reference | string |
| `tit` | title | Title | string |
| `aut` | author | Author | string |
| `dat` | date | Date (ISO 8601) | string |
| `pag` | pages | Page count | number |
| `chu` | chunk | Chunk size | number |
| `ovr` | overlap | Overlap size | number |
| `emb` | embedding | Embedding reference | string |
| `idx` | index | Index type/reference | string |
| `sta` | stats | Statistics container | object |
| `lan` | languages | Language list | array |
| `dev` | device | Device type | string |
| `nor` | normalize | Normalize flag | boolean |
| `des` | description | Description text | string |

**Full Registry:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md`

---

## Schema Types

### 1. LLM Model Config: `ava-llm-1.0`

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

**Size:** ~200 bytes (vs ~400 bytes verbose)

### 2. Embedding Model Config: `ava-emb-1.0`

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

### 3. Intent Ontology: `ava-aot-3.0`

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

**Migration:** Existing `.aot` files use single-letter - will migrate to 3-letter in v4.0

### 4. RAG Document Metadata: `ava-rag-1.0`

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

---

## Alternatives Considered

### Alternative 1: Keep Single-Letter (.ava format)

**Used in:** Intent ontology files (.aot)

**Pros:**
- Smallest size (66% reduction)
- Already implemented for intents

**Cons:**
- Not human-readable: `"s"`, `"v"`, `"l"`, `"m"`
- Requires documentation for every key
- Debug-unfriendly
- **REJECTED for new schemas** (keep for .aot backward compat only)

### Alternative 2: Use Verbose JSON

**Standard practice**

**Pros:**
- Most readable
- No learning curve
- Standard across industry

**Cons:**
- Largest file size
- No ecosystem identity
- Network/storage overhead
- **REJECTED** (size penalty too high)

### Alternative 3: 4-Letter Abbreviations

**Examples:** `name`, `type`, `size`, `vers`

**Pros:**
- More readable than 3-letter
- Still compact

**Cons:**
- Not consistent (some words need 5+ letters)
- No clear rule (when to use 4 vs 5?)
- Larger than 3-letter
- **REJECTED** (inconsistency)

### Alternative 4: Custom Binary Format

**Examples:** Protocol Buffers, MessagePack

**Pros:**
- Smallest possible size
- Fast parsing

**Cons:**
- Not human-readable at all
- Requires specialized tools
- Cannot edit manually
- **REJECTED** (violates human-readable requirement)

---

## Consequences

### Positive

✅ **Human Readable**: Developers understand `"sch"`, `"ver"`, `"cfg"` without docs
✅ **Compact**: ~50% smaller than verbose JSON
✅ **Ecosystem Consistency**: Same keys across all projects
✅ **Debuggable**: Clear keys make troubleshooting easier
✅ **Parseable**: Standard JSON (any JSON library works)
✅ **Extensible**: Easy to add new 3-letter keys
✅ **Professional**: Industry-standard abbreviations

### Negative

⚠️ **Learning Curve**: Developers must learn 30+ abbreviations
  - Mitigation: Comprehensive documentation
  - Mitigation: IDE autocomplete (JSON schema)

⚠️ **Not as Small**: 10% larger than single-letter
  - Mitigation: 50% reduction vs verbose is sufficient
  - Mitigation: Human readability worth the cost

⚠️ **Migration Effort**: Must update existing files
  - Mitigation: Gradual migration (backward compat)
  - Mitigation: Conversion tools provided

---

## Implementation

### Phase 1: Documentation (Week 1)
- [x] Create AVA-3LETTER-JSON-SCHEMA.md
- [x] Document global key registry (30+ keys)
- [x] Define 4 schema types (LLM, Embedding, Intent, RAG)
- [x] Create migration examples
- [ ] Add to Developer Manual Chapter 51

### Phase 2: Tooling (Week 2)
- [ ] JSON schema files (for IDE validation)
- [ ] Conversion tool (verbose → 3-letter)
- [ ] Validation tool (check schema compliance)
- [ ] Gradle task: `validate3LetterSchema`

### Phase 3: Migration (Week 3-4)
- [ ] Migrate LLM configs to ava-llm-1.0
- [ ] Migrate embedding configs to ava-emb-1.0
- [ ] Create RAG configs (ava-rag-1.0)
- [ ] Update parsers to support 3-letter

### Phase 4: Ecosystem Rollout (Month 2)
- [ ] VoiceOS adoption
- [ ] AVAConnect adoption
- [ ] Avanues adoption
- [ ] Deprecate verbose JSON

---

## Validation

### JSON Schema

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

### Kotlin Parsing

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

// Usage
val config = Json.decodeFromString<AvaLLMConfig>(jsonString)
println("Model: ${config.met.nam}, Type: ${config.met.typ}")
```

---

## References

- **Full Standard:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md`
- **Key Registry:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md#global-key-registry`
- **Schema Types:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md#schema-types`
- **Related ADRs:**
  - ADR-005: Multi-Source Intent System (.ava single-letter format)
  - ADR-010: External Storage Migration (.AVAVoiceAvanues)
  - ADR-012: RAG System Architecture

---

## Changelog

**v1.0 (2025-11-27):**
- Initial ADR
- Defined 30+ global keys
- Created 4 schema types
- Ecosystem standard established

---

**Status:** ✅ ACCEPTED
**Adoption:** AVA (100%), VoiceOS (pending), AVAConnect (pending), Avanues (pending)
**Risk Level:** Low (gradual migration, backward compatible)
**Enforcement:** MANDATORY for all new JSON files

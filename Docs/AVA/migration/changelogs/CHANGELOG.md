# Changelog

All notable changes to the AVA project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Universal Format v2.0 support for .ava intent files
- ADR 0001: Documentation for v2.0 migration decision
- NLU module README with comprehensive guide
- Project status tracking document
- Changelog documentation

### Changed
- **[BREAKING]** Migrated information.ava to Universal Format v2.0
- **[BREAKING]** Migrated productivity.ava to Universal Format v2.0
- AssetExtractor now extracts conversational intent files
- Intent count increased from 116 to 120 intents

### Fixed
- Natural language classification failures for conversational queries
- Missing semantic embeddings for show_time, check_weather, set_alarm, set_reminder
- Keyword matching fallback incorrectly used for intents with training data

### Removed
- v1.0 JSON format support (deprecated)

## [Phase 3.0] - 2025-11-24

### Context
This release fixes critical NLU issues preventing natural language understanding.

### Technical Details

**Root Cause:** Intent training files in deprecated JSON v1.0 format were rejected by parser, causing semantic embeddings to not be computed for conversational intents.

**Impact:** 
- Before: 116 intents with embeddings
- After: 120 intents with embeddings
- Affected intents: show_time, check_weather, set_alarm, set_reminder

**Files Changed:**
```
apps/ava-standalone/src/main/assets/ava-examples/en-US/
├── information.ava (JSON → v2.0)
└── productivity.ava (JSON → v2.0)

Universal/AVA/Features/NLU/src/androidMain/kotlin/
└── com/augmentalis/ava/features/nlu/ava/AssetExtractor.kt
```

**Testing Results:**
```
Query: "Show time"
Before: ✅ 1.0 confidence (keyword match)
After:  ✅ High confidence (semantic)

Query: "What's the time now?"
Before: ❌ 0.35 confidence (below 0.6 threshold)
After:  ✅ High confidence (semantic)
```

### Migration Guide

For developers with custom .ava files:

**Old Format (v1.0 JSON):**
```json
{
  "s": "ava-1.0",
  "v": "1.0.0",
  "i": [
    {
      "id": "show_time",
      "s": ["what time is it", "current time"]
    }
  ]
}
```

**New Format (v2.0):**
```
# Avanues Universal Format v2.0
---
schema: avu-1.0
version: 1.0.0
---
INFO:show_time:what time is it
INFO:show_time:current time
---
```

See ADR 0001 for complete format specification.

---

## Version History

- **Phase 3.0** (2025-11-24): NLU v2.0 migration, natural language fix
- **Phase 2.0** (2025-11-XX): RAG integration, AON format
- **Phase 1.0** (2025-XX-XX): Initial release with basic NLU

---

**Note:** This project uses the development branch for active work. See `docs/STATUS.md` for current build status.

# ADR 0001: Universal Format v2.0 Migration for Intent Training Data

**Date:** 2025-11-24  
**Status:** Accepted  
**Decision Makers:** AVA AI Team  
**Tags:** #NLU #intent-classification #file-format

## Context

The NLU system was failing to classify natural language questions like "What's the time now?" despite successfully handling command-style phrases like "Show time". Investigation revealed that conversational intent files (information.ava, productivity.ava) were in the deprecated v1.0 JSON format, which the current AvaFileReader explicitly rejects.

### Problem Details

1. **Symptom**: Natural language questions failed with low confidence scores (0.35 vs 0.6 threshold)
2. **Root Cause**: Conversational intents had no semantic embeddings because their .ava files couldn't be loaded
3. **Fallback Behavior**: System fell back to keyword matching, which scored poorly for natural phrasing
4. **Format Issue**: v1.0 JSON format files rejected with error: "Must use Universal Format v2.0 (start with # or ---)"

### Investigation Timeline

- **Log Analysis**: Showed only 116/120 intents had embeddings computed
- **File Check**: Found information.ava and productivity.ava in v1.0 JSON format
- **Asset Loading**: VoiceOS control intents (media-control.ava, navigation.ava) already in v2.0 format
- **Verification**: After conversion, all 120 intents successfully loaded with embeddings

## Decision

**Migrate all intent training files to Universal Format v2.0** and enforce v2.0 as the only supported format.

### Changes Implemented

1. **Converted Files to v2.0:**
   - `information.ava` - Time and weather queries (2 intents, 19 examples)
   - `productivity.ava` - Alarms and reminders (2 intents, 19 examples)

2. **Updated AssetExtractor.kt:**
   - Added information.ava and productivity.ava to extraction list
   - Updated directory structure documentation

3. **Format Specification:**
   ```
   # Universal Format v2.0
   ---
   schema: avu-1.0
   version: 1.0.0
   locale: en-US
   metadata: { ... }
   ---
   PREFIX:intent_id:example_phrase
   PREFIX:intent_id:example_phrase
   ---
   synonyms: { ... }
   ```

## Consequences

### Positive

✅ **Natural Language Support**: Conversational queries now work with semantic similarity  
✅ **Consistent Format**: All .ava files use same v2.0 format  
✅ **Better Accuracy**: MobileBERT embeddings vs keyword matching  
✅ **120 Intents**: Complete coverage (was 116)  
✅ **Human Readable**: Text-based format easier to edit than JSON

### Negative

⚠️ **Breaking Change**: v1.0 JSON files no longer supported  
⚠️ **Migration Required**: Any custom .ava files must be converted  
⚠️ **No Backward Compatibility**: Explicit rejection of old format

### Neutral

- Extraction time unchanged (same file processing)
- Embedding computation time scales with examples (not file format)
- Database schema unchanged

## Alternatives Considered

### 1. Support Both Formats
**Rejected** - Adds parser complexity, delays migration, maintains legacy code

### 2. Auto-Convert on Load
**Rejected** - Hides format issues, creates ambiguity about source of truth

### 3. Keep v1.0 for Some Files
**Rejected** - Inconsistent developer experience, harder to maintain

## Implementation Notes

### File Format v2.0 Structure

```
# Header comments
---
schema: avu-1.0           # Universal format schema
version: 1.0.0            # File version
locale: en-US             # Language/locale
metadata:                 # File metadata
  file: information.ava
  category: information
  name: Information Queries
  description: Weather, time, news
  priority: 1
  count: 2
---
INFO:check_weather:what's the weather    # PREFIX:intent:example
INFO:check_weather:weather forecast
INFO:show_time:what time is it
INFO:show_time:current time
---
synonyms:                 # Optional synonyms
  check: [show, display, tell]
  weather: [forecast, temperature]
```

### Affected Intents

| Intent | Examples | Category | Priority |
|--------|----------|----------|----------|
| `check_weather` | 11 | information | 1 |
| `show_time` | 8 | information | 1 |
| `set_alarm` | 10 | productivity | 1 |
| `set_reminder` | 9 | productivity | 1 |

### Testing Results

```
Before Fix:
- "Show time" → ✅ 1.0 confidence (keyword match)
- "What's the time now?" → ❌ 0.35 confidence (below 0.6 threshold)

After Fix:
- "Show time" → ✅ High confidence (semantic embedding)
- "What's the time now?" → ✅ High confidence (semantic embedding)
```

## References

- **Issue**: Natural language NLU classification failures
- **Files Changed**: 
  - `apps/ava-standalone/src/main/assets/ava-examples/en-US/information.ava`
  - `apps/ava-standalone/src/main/assets/ava-examples/en-US/productivity.ava`
  - `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/AssetExtractor.kt`
- **Logs**: See `/tmp/ava_logs.txt` for original failure analysis

## Future Considerations

1. **Migration Tool**: Create converter for v1.0 → v2.0 for third-party developers
2. **Format Validation**: Add pre-commit hooks to validate .ava file format
3. **Documentation**: Update developer guide with v2.0 format specification
4. **Version Detection**: Consider graceful error messages with conversion hints

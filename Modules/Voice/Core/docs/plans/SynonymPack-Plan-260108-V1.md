# Implementation Plan: SynonymPack Module

**Feature:** Multi-Language Synonym Expansion for Voice Command Matching
**Module:** VoiceOSCoreNG
**Date:** 2026-01-08
**Version:** 1.0
**Flags:** `.tasks .cot .tot .yolo .swarm`

---

## Overview

| Attribute | Value |
|-----------|-------|
| Platforms | commonMain (shared), androidMain, iosMain, desktopMain |
| Swarm Recommended | Yes (4 platforms, 18+ tasks) |
| Estimated Tasks | 18 |
| Sequential Time | ~4 hours |
| Parallel Time | ~1.5 hours (swarm) |

---

## Chain of Thought: Design Reasoning

### Problem Statement
Current CommandMatcher uses Jaccard word similarity which fails when:
- User says "tap" but command is "click"
- User says "go back" but command is "back"
- User says "पीछे जाओ" (Hindi) but command is "back"

### Design Goals
1. **ASR-Agnostic** - Works with Vivoka, Vosk, Google STT, Apple STT, any future ASR
2. **Multi-Language** - Support Latin, CJK, Arabic, Indic scripts
3. **Hybrid Storage** - Built-in common languages, downloadable others
4. **Dual Format** - Human-readable .syn for dev, binary .qsyn for production
5. **Platform-Aware Tokenization** - Use native tokenizers for CJK

### Key Insight
Synonyms are language-specific, not ASR-specific. The same synonym map works regardless of which ASR recognizes "tap" - we just need to expand it to include "click".

---

## Tree of Thought: Approach Analysis

### Approach 1: Simple HashMap per Language
```kotlin
val synonyms = mapOf(
    "en" to mapOf("tap" to "click", "push" to "click"),
    "es" to mapOf("pulsar" to "click", "tocar" to "click")
)
```
**Pros:** Simple, fast
**Cons:** Not extensible, hard to maintain, no metadata

### Approach 2: Canonical Action Mapping (SELECTED)
```kotlin
// Each synonym maps to a canonical action
val synonymMap = SynonymMap(language = "en")
synonymMap.getCanonical("tap") // Returns "click"
synonymMap.getCanonical("push") // Returns "click"
synonymMap.getSynonyms("click") // Returns ["tap", "press", "push", "hit"]
```
**Pros:** Bidirectional lookup, canonical actions are consistent
**Cons:** Slightly more complex

### Approach 3: NLM-Based (Rejected for Now)
Use neural language model for semantic matching.
**Pros:** Best accuracy, handles novel synonyms
**Cons:** Heavy dependency, latency, not available offline

### Decision: Approach 2 with NLM Fallback Hook
Implement canonical action mapping now, design interface for NLM enhancement later.

---

## Architecture

```
VoiceOSCoreNG/
├── src/commonMain/
│   ├── kotlin/.../synonym/
│   │   ├── SynonymMap.kt           # Core synonym lookup
│   │   ├── SynonymLoader.kt        # Multi-tier loading
│   │   ├── SynonymParser.kt        # .syn text parser
│   │   ├── SynonymBinaryFormat.kt  # .qsyn binary format
│   │   └── ISynonymProvider.kt     # Abstraction for NLM hook
│   │
│   ├── kotlin/.../tokenizer/
│   │   └── Tokenizer.kt            # expect class
│   │
│   └── resources/synonyms/
│       ├── schema.syn              # Format documentation
│       ├── en.syn                  # English (built-in)
│       └── es.syn                  # Spanish (built-in)
│
├── src/androidMain/kotlin/.../tokenizer/
│   └── Tokenizer.kt                # actual - ICU4J
│
├── src/iosMain/kotlin/.../tokenizer/
│   └── Tokenizer.kt                # actual - CFStringTokenizer
│
├── src/desktopMain/kotlin/.../tokenizer/
│   └── Tokenizer.kt                # actual - ICU4J (JVM)
│
└── tools/
    └── syn2qsyn.kt                 # Build tool: .syn → .qsyn converter
```

---

## File Format Specifications

### Text Format (.syn)

```
# Language: English
# Version: 1.0
# Script: latin
# Tokenizer: whitespace

@meta
language = en
version = 1.0
script = latin
tokenizer = whitespace

@synonyms
# canonical_action | synonym1, synonym2, synonym3...
click | tap, press, push, hit, select, choose, pick
long_click | long press, hold, press and hold, long tap
double_click | double tap, tap twice, double press
scroll_up | swipe up, go up, move up, scroll upward
scroll_down | swipe down, go down, move down, scroll downward
back | go back, return, previous, navigate back
home | go home, main screen, home screen
type | enter, input, write, fill in
expand | open, show more, unfold
collapse | close, hide, fold, minimize
```

### Binary Format (.qsyn)

```
Header (16 bytes):
  - Magic: "QSYN" (4 bytes)
  - Version: uint16 (2 bytes)
  - Language code: 2 chars + null (3 bytes)
  - Flags: uint8 (1 byte) - RTL, CJK, etc.
  - Entry count: uint32 (4 bytes)
  - Reserved: 2 bytes

Index Section:
  - Sorted synonym hashes for binary search
  - Each entry: hash (4 bytes) + offset (4 bytes)

Data Section:
  - Canonical action strings (null-terminated)
  - Synonym strings (null-terminated)
```

---

## Phases

### Phase 1: Core Data Structures (commonMain)

| Task | File | Description |
|------|------|-------------|
| 1.1 | `synonym/SynonymMap.kt` | Core class: synonym→canonical, canonical→synonyms |
| 1.2 | `synonym/SynonymEntry.kt` | Data class for synonym entries |
| 1.3 | `synonym/LanguageMetadata.kt` | Language info: script, tokenizer type, RTL |

### Phase 2: Text Parser (commonMain)

| Task | File | Description |
|------|------|-------------|
| 2.1 | `synonym/SynonymParser.kt` | Parse .syn format to SynonymMap |
| 2.2 | `resources/synonyms/schema.syn` | Format documentation |
| 2.3 | Unit tests for parser | Test parsing edge cases |

### Phase 3: Binary Format (commonMain)

| Task | File | Description |
|------|------|-------------|
| 3.1 | `synonym/SynonymBinaryFormat.kt` | Read/write .qsyn format |
| 3.2 | `synonym/SynonymBinaryWriter.kt` | Write binary with hash index |
| 3.3 | Unit tests for binary format | Round-trip testing |

### Phase 4: Multi-Tier Loader (commonMain)

| Task | File | Description |
|------|------|-------------|
| 4.1 | `synonym/SynonymLoader.kt` | Tiered loading: custom→downloaded→builtin |
| 4.2 | `synonym/ISynonymProvider.kt` | Interface for NLM enhancement hook |
| 4.3 | `synonym/SynonymPaths.kt` | Platform path configuration |

### Phase 5: Platform Tokenizer (KMP expect/actual)

| Task | File | Description |
|------|------|-------------|
| 5.1 | `tokenizer/Tokenizer.kt` (common) | expect class with tokenize() |
| 5.2 | `tokenizer/Tokenizer.kt` (android) | actual using ICU4J BreakIterator |
| 5.3 | `tokenizer/Tokenizer.kt` (ios) | actual using CFStringTokenizer |
| 5.4 | `tokenizer/Tokenizer.kt` (desktop) | actual using ICU4J (JVM) |

### Phase 6: CommandMatcher Integration (commonMain)

| Task | File | Description |
|------|------|-------------|
| 6.1 | `common/CommandMatcher.kt` | Integrate synonym expansion |
| 6.2 | Integration tests | Test synonym-aware matching |

### Phase 7: Built-in Synonym Packs

| Task | File | Description |
|------|------|-------------|
| 7.1 | `resources/synonyms/en.syn` | English synonyms |
| 7.2 | `resources/synonyms/es.syn` | Spanish synonyms |
| 7.3 | Build tool for .qsyn | Convert .syn to .qsyn at build time |

---

## Task List (18 Tasks)

```
[ ] 1. Create SynonymMap.kt - Core synonym data structure
[ ] 2. Create SynonymEntry.kt - Synonym entry data class
[ ] 3. Create LanguageMetadata.kt - Language configuration
[ ] 4. Create SynonymParser.kt - Text format parser
[ ] 5. Create schema.syn - Format documentation
[ ] 6. Write SynonymParser unit tests
[ ] 7. Create SynonymBinaryFormat.kt - Binary reader
[ ] 8. Create SynonymBinaryWriter.kt - Binary writer
[ ] 9. Write binary format unit tests
[ ] 10. Create SynonymLoader.kt - Tiered loader
[ ] 11. Create ISynonymProvider.kt - NLM hook interface
[ ] 12. Create SynonymPaths.kt - Path configuration
[ ] 13. Create Tokenizer.kt (commonMain) - expect declaration
[ ] 14. Create Tokenizer.kt (androidMain) - ICU4J actual
[ ] 15. Create Tokenizer.kt (iosMain) - CFStringTokenizer actual
[ ] 16. Create Tokenizer.kt (desktopMain) - JVM ICU4J actual
[ ] 17. Integrate synonyms into CommandMatcher.kt
[ ] 18. Create en.syn and es.syn built-in packs
```

---

## Swarm Configuration

| Agent | Tasks | Parallel With |
|-------|-------|---------------|
| Agent 1 | 1-3 (Core) | - |
| Agent 2 | 4-6 (Parser) | Agent 1 |
| Agent 3 | 7-9 (Binary) | Waits for 1-3 |
| Agent 4 | 10-12 (Loader) | Waits for 4-9 |
| Agent 5 | 13-16 (Tokenizer) | Agent 1-4 |
| Agent 6 | 17-18 (Integration) | Waits for all |

---

## CJK Tokenization Strategy

For Japanese, Chinese, Korean:

### Android (ICU4J - Built-in)
```kotlin
actual class Tokenizer {
    actual fun tokenize(text: String, locale: Locale): List<String> {
        val iterator = BreakIterator.getWordInstance(locale)
        iterator.setText(text)
        return buildList {
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val word = text.substring(start, end).trim()
                if (word.isNotBlank()) add(word)
                start = end
                end = iterator.next()
            }
        }
    }
}
```

### iOS (CFStringTokenizer)
```kotlin
actual class Tokenizer {
    actual fun tokenize(text: String, locale: Locale): List<String> {
        return memScoped {
            val cfString = CFStringCreateWithCString(null, text, kCFStringEncodingUTF8)
            val cfLocale = CFLocaleCreate(null, locale.language)
            val tokenizer = CFStringTokenizerCreate(
                null, cfString, CFRangeMake(0, CFStringGetLength(cfString)),
                kCFStringTokenizerUnitWord, cfLocale
            )
            buildList {
                while (CFStringTokenizerAdvanceToNextToken(tokenizer) != 0) {
                    val range = CFStringTokenizerGetCurrentTokenRange(tokenizer)
                    val token = CFStringCreateWithSubstring(null, cfString, range)
                    add(CFStringGetCStringPtr(token, kCFStringEncodingUTF8)?.toKString() ?: "")
                }
            }
        }
    }
}
```

### Desktop (ICU4J via Maven)
Same as Android, ICU4J is available for JVM.

---

## NLM Enhancement Hook (Future)

```kotlin
interface ISynonymProvider {
    /**
     * Get canonical action for a word.
     * Returns null if no mapping found.
     */
    fun getCanonical(word: String, language: String): String?

    /**
     * Get all synonyms for a canonical action.
     */
    fun getSynonyms(canonical: String, language: String): List<String>

    /**
     * Check if NLM-based resolution is available.
     */
    fun isNlmAvailable(): Boolean

    /**
     * Resolve using NLM (for ambiguous cases).
     * Only called when isNlmAvailable() returns true.
     */
    suspend fun nlmResolve(input: String, candidates: List<String>): String?
}

// Default implementation uses static synonym maps
class StaticSynonymProvider(private val loader: SynonymLoader) : ISynonymProvider {
    override fun getCanonical(word: String, language: String): String? {
        return loader.load(language)?.getCanonical(word)
    }

    override fun isNlmAvailable() = false
    override suspend fun nlmResolve(input: String, candidates: List<String>) = null
}

// Future NLM implementation
class NlmSynonymProvider(
    private val staticProvider: StaticSynonymProvider,
    private val nlm: NluEngine
) : ISynonymProvider {
    override fun getCanonical(word: String, language: String): String? {
        return staticProvider.getCanonical(word, language)
            ?: nlm.resolveActionSync(word, language)
    }

    override fun isNlmAvailable() = nlm.isLoaded()

    override suspend fun nlmResolve(input: String, candidates: List<String>): String? {
        return nlm.semanticMatch(input, candidates)
    }
}
```

---

## Testing Strategy

### Unit Tests
- SynonymParser: Parse valid .syn, handle malformed files
- SynonymBinaryFormat: Round-trip encoding/decoding
- SynonymMap: Lookup, case insensitivity, missing entries
- Tokenizer: Latin, CJK, Arabic tokenization

### Integration Tests
- CommandMatcher with synonyms enabled
- Multi-language synonym loading
- Tiered loading priority

### Manual Tests
- Voice input "tap submit" matches "click submit"
- Japanese input tokenization
- Arabic RTL handling

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| ICU4J adds size to APK | Already included in Android, minimal impact |
| iOS CFStringTokenizer complexity | Well-documented Apple API |
| Binary format corruption | Header magic + checksum validation |
| Missing language packs | Graceful fallback to English |
| NLM not ready | Design with ISynonymProvider abstraction |

---

## Success Criteria

1. "tap Settings" matches "click Settings" command
2. Japanese tokenization works correctly
3. Binary .qsyn loads faster than text .syn
4. English/Spanish work offline (built-in)
5. Other languages downloadable and cacheable
6. NLM can be integrated later without refactoring

---

## Next Steps

After plan approval:
1. Generate TodoWrite tasks
2. Proceed to `/i.implement` with `.yolo .swarm`
3. Auto-commit after each phase

---

**Author:** Claude (IDEACODE)
**Reviewed:** Pending
**Approved:** Pending

# VoiceOSCore Handover: CommandGenerator Refactoring

**Date:** 2026-02-02
**Module:** VoiceOSCore
**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandGenerator.kt`
**Current Size:** 850 lines
**Priority:** Medium (code health)

---

## Executive Summary

The `CommandGenerator.kt` file has grown to 850 lines and now handles multiple distinct responsibilities. This document outlines a recommended refactoring to improve maintainability, testability, and code organization.

---

## Current State Analysis

### File Location
```
Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandGenerator.kt
```

### Current Responsibilities (Mixed)

1. **Garbage Text Filtering** (~50 lines) - Delegates to FilterFileLoader
2. **Icon Element Detection** (~100 lines) - Toolbar/navigation icons
3. **Icon Command Generation** (~80 lines) - Single-word commands
4. **List Command Generation** (~200 lines) - Index, numeric, label commands
5. **Core Command Generation** (~200 lines) - fromElement, fromElementWithPersistence
6. **Label Processing** (~100 lines) - deriveLabel, extractShortLabel, normalization
7. **Helper Functions** (~120 lines) - hash, confidence, action type derivation

### Recent Changes (2026-02-02)

- Externalized localization to AVU files via `FilterFileLoader.kt`
- Removed hardcoded `LOCALIZED_REPETITIVE_WORDS` and `LOCALIZED_NAVIGATION_ICONS`
- Added garbage filtering and icon command support

---

## Proposed Refactoring

### Target Structure

```
Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/
├── CommandGenerator.kt          (~400 lines) - Core command generation
├── ListCommandGenerator.kt      (~280 lines) - List/dynamic commands
├── IconCommandGenerator.kt      (~180 lines) - Icon button commands
├── FilterFileLoader.kt          (existing)   - AVU file loading
└── LabelProcessor.kt            (optional)   - Label extraction/normalization
```

---

## Extraction Details

### 1. ListCommandGenerator.kt (~280 lines)

**Purpose:** Handle all dynamic list navigation commands

**Functions to Extract:**

```kotlin
object ListCommandGenerator {
    /**
     * Generate index-based commands: "first", "second", "third"...
     * Creates ordinal commands for list items.
     */
    fun generateListIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand>

    /**
     * Generate numeric commands: "1", "2", "3"...
     * Creates raw number commands matching overlay badges.
     */
    fun generateNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand>

    /**
     * Generate label-based commands: "Arby's", "Lifemiles"...
     * Uses extracted sender/title for voice targeting.
     */
    fun generateListLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand>

    /**
     * Extract short sender/title from dynamic content.
     * E.g., "Unread, , , Arby's, , BOGO..." → "Arby's"
     */
    fun extractShortLabel(element: ElementInfo): String?
}
```

**Dependencies:**
- `ElementInfo` - Element data
- `QuantizedCommand` - Command output
- `CommandGenerator.generateAvid()` - May need to expose or duplicate
- `CommandGenerator.deriveElementHash()` - May need to expose or duplicate
- `CommandGenerator.isGarbageText()` - For label validation

**Notes:**
- All these commands are in-memory only (never persisted)
- Filter for actionable elements before generating
- Sort by position for consistent numbering

---

### 2. IconCommandGenerator.kt (~180 lines)

**Purpose:** Handle toolbar/navigation icon commands

**Functions to Extract:**

```kotlin
object IconCommandGenerator {
    /**
     * Check if element is an icon button.
     * Detection based on: size, class name, contentDescription.
     */
    fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean

    /**
     * Get label for icon element (single-word allowed).
     */
    fun getIconLabel(element: ElementInfo): String?

    /**
     * Generate commands for icon elements.
     * Returns: (labeled commands, numbered commands for unlabeled)
     */
    fun generateIconCommands(
        elements: List<ElementInfo>,
        packageName: String
    ): Pair<List<QuantizedCommand>, List<QuantizedCommand>>
}
```

**Dependencies:**
- `FilterFileLoader.getNavigationIcons()` - Icon label lookup
- `CommandGenerator.isGarbageText()` - Label validation
- `CommandGenerator.cleanLabel()` - Label cleaning

**Icon Detection Criteria:**
1. Must be clickable
2. Has contentDescription but no visible text
3. Matches known navigation icons OR
4. Small size (< 150x150 pixels) OR
5. Class contains: imagebutton, iconbutton, actionbutton, fab

---

### 3. Keep in CommandGenerator.kt (~400 lines)

**Core Generation:**
```kotlin
fun fromElement(element: ElementInfo, packageName: String): QuantizedCommand?

fun fromElementWithPersistence(
    element: ElementInfo,
    packageName: String,
    allElements: List<ElementInfo>
): GeneratedCommandResult?
```

**Garbage Filtering (delegates to FilterFileLoader):**
```kotlin
fun isGarbageText(text: String, locale: String = "en"): Boolean
fun cleanLabel(text: String, locale: String = "en"): String?
```

**Label Processing:**
```kotlin
private fun deriveLabel(element: ElementInfo, locale: String = "en"): String
private fun normalizeRealWearMlScript(text: String): String
```

**Helpers:**
```kotlin
private fun deriveElementHash(element: ElementInfo): String
private fun deriveActionType(element: ElementInfo): CommandActionType
private fun generateAvid(element: ElementInfo, packageName: String): String
private fun calculateConfidence(element: ElementInfo): Float
```

**Data Classes:**
```kotlin
data class GeneratedCommandResult(
    val command: QuantizedCommand,
    val shouldPersist: Boolean,
    val listIndex: Int = -1
)
```

---

## Refactoring Steps

### Phase 1: Extract ListCommandGenerator

1. Create `ListCommandGenerator.kt`
2. Move list command functions
3. Expose necessary helpers from CommandGenerator (or duplicate)
4. Update callers to use ListCommandGenerator
5. Run tests

### Phase 2: Extract IconCommandGenerator

1. Create `IconCommandGenerator.kt`
2. Move icon functions
3. Update callers
4. Run tests

### Phase 3: (Optional) Extract LabelProcessor

1. Create `LabelProcessor.kt`
2. Move `extractShortLabel`, `normalizeRealWearMlScript`
3. This is lower priority - only if CommandGenerator still feels too large

---

## Testing Considerations

### Unit Tests Needed

```kotlin
// ListCommandGeneratorTest.kt
class ListCommandGeneratorTest {
    @Test fun `generateListIndexCommands creates ordinal commands`()
    @Test fun `generateNumericCommands matches overlay badges`()
    @Test fun `generateListLabelCommands extracts sender names`()
    @Test fun `filters non-actionable elements`()
    @Test fun `deduplicates by listIndex`()
}

// IconCommandGeneratorTest.kt
class IconCommandGeneratorTest {
    @Test fun `isIconElement detects small clickable with contentDescription`()
    @Test fun `isIconElement recognizes navigation icons by locale`()
    @Test fun `getIconLabel filters garbage`()
    @Test fun `generateIconCommands separates labeled from unlabeled`()
}
```

### Integration Tests

- Verify end-to-end command generation still works
- Test with real accessibility trees from Gmail, Instagram, etc.

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Circular dependencies | Low | Medium | Careful interface design |
| Missed call sites | Medium | Low | Grep for function names |
| Test coverage gaps | Medium | Medium | Write tests before refactoring |
| Performance regression | Low | Low | Benchmark before/after |

---

## Files to Modify

### New Files
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/ListCommandGenerator.kt`
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/IconCommandGenerator.kt`

### Modified Files
- `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/CommandGenerator.kt`

### Potential Callers to Update
Search for usages of:
- `CommandGenerator.generateListIndexCommands`
- `CommandGenerator.generateNumericCommands`
- `CommandGenerator.generateListLabelCommands`
- `CommandGenerator.isIconElement`
- `CommandGenerator.getIconLabel`
- `CommandGenerator.generateIconCommands`

---

## Estimated Effort

| Task | Effort |
|------|--------|
| Extract ListCommandGenerator | 2-3 hours |
| Extract IconCommandGenerator | 1-2 hours |
| Update callers | 1 hour |
| Write unit tests | 2-3 hours |
| Integration testing | 1-2 hours |
| **Total** | **7-11 hours** |

---

## Related Documentation

- [VoiceOS-Garbage-Filtering-Icon-Commands-50202-V1.md](../modules/VoiceOSCore/developer-manual/VoiceOS-Garbage-Filtering-Icon-Commands-50202-V1.md)
- [AVU-Universal-Format-Spec-260122-V2.md](../../VoiceOS/Technical/specifications/AVU-Universal-Format-Spec-260122-V2.md)
- [FilterFileLoader.kt](../../../Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/FilterFileLoader.kt)

---

## Session Reference

Work completed in session: https://claude.ai/code/session_01Vmb6S3K6QL9Wij3KZsCqCn

**Changes Made This Session:**
1. Created AVU filter files for garbage words and navigation icons (en-US, de-DE, es-ES, fr-FR)
2. Created FilterFileLoader.kt for loading filter data from AVU files
3. Updated CommandGenerator.kt to use FilterFileLoader instead of hardcoded maps
4. Added developer documentation

---

**Author:** Augmentalis Engineering
**Status:** Ready for Implementation

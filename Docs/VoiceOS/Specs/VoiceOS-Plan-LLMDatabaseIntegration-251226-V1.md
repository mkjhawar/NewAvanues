# Implementation Plan: VoiceOS LLM Database Integration

**Spec Reference**: VoiceOS-Spec-LLMDatabaseIntegration-251226-V1.md
**Created**: 2025-12-26
**Platform**: Android (Kotlin)

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | Android |
| Swarm Recommended | No (single platform) |
| Estimated Tasks | 12 |
| Estimated Duration | 1 session |

---

## Phase 1: Repository Integration

### Task 1.1: Add Repository Dependencies to AVUQuantizerIntegration

**File**: `apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ai/quantized/AVUQuantizerIntegration.kt`

**Changes**:
```kotlin
class AVUQuantizerIntegration(
    private val context: Context,
    private val screenContextRepository: IScreenContextRepository,
    private val generatedCommandRepository: IGeneratedCommandRepository,
    private val learnAppRepository: LearnAppRepository
) : ExplorationDebugCallback {
```

**Acceptance**: Constructor accepts 3 new repository parameters

---

### Task 1.2: Implement getLearnedPackages()

**File**: `AVUQuantizerIntegration.kt:284-288`

**Replace**:
```kotlin
private fun getLearnedPackages(): List<String> {
    // Query database for all learned packages
    // Placeholder - in production, query database
    return emptyList()
}
```

**With**:
```kotlin
private suspend fun getLearnedPackages(): List<String> = withContext(Dispatchers.IO) {
    try {
        learnAppRepository.getCompletedApps()
            .map { it.packageName }
            .distinct()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get learned packages", e)
        emptyList()
    }
}
```

**Acceptance**: Returns list of package names from LearnedApp table

---

### Task 1.3: Implement hasLearnedData()

**File**: `AVUQuantizerIntegration.kt:278-282`

**Replace**:
```kotlin
private fun hasLearnedData(packageName: String): Boolean {
    // Check if database has exploration data for this package
    // Placeholder - in production, query database
    return false
}
```

**With**:
```kotlin
private suspend fun hasLearnedData(packageName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        learnAppRepository.getAppByPackage(packageName)?.status == LearnedAppStatus.COMPLETED
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check learned data for $packageName", e)
        false
    }
}
```

**Acceptance**: Returns true only for completed learned apps

---

## Phase 2: Screen Quantization

### Task 2.1: Implement buildQuantizedScreens()

**File**: `AVUQuantizerIntegration.kt:250-254`

**Replace**:
```kotlin
private fun buildQuantizedScreens(packageName: String): List<QuantizedScreen> {
    // In production, this queries the SQLDelight database for learned screens
    // For now, return empty list - will be populated when exploration data is available
    return emptyList()
}
```

**With**:
```kotlin
private suspend fun buildQuantizedScreens(packageName: String): List<QuantizedScreen> =
    withContext(Dispatchers.IO) {
        try {
            val screens = screenContextRepository.getScreensForPackage(packageName)
            screens.map { screen ->
                val elements = screenContextRepository.getElementsForScreen(screen.screenHash)
                    .filter { it.isClickable || it.isEditable || it.isCheckable }
                    .map { element -> convertToQuantizedElement(element) }

                QuantizedScreen(
                    screenHash = screen.screenHash,
                    screenTitle = screen.windowTitle ?: screen.activityName ?: "Unknown",
                    activityName = screen.activityName,
                    elements = elements
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build quantized screens for $packageName", e)
            emptyList()
        }
    }
```

**Acceptance**: Returns screens with actionable elements

---

### Task 2.2: Implement convertToQuantizedElement()

**File**: `AVUQuantizerIntegration.kt` (new method)

**Add**:
```kotlin
private fun convertToQuantizedElement(entity: ScrapedElementDTO): QuantizedElement {
    return QuantizedElement(
        vuid = entity.uuid ?: entity.elementHash,
        type = classifyElementType(entity.className),
        label = entity.text ?: entity.contentDescription ?: entity.viewIdResourceName ?: "unlabeled",
        aliases = buildAliases(entity)
    )
}

private fun buildAliases(entity: ScrapedElementDTO): List<String> {
    return listOfNotNull(
        entity.text,
        entity.contentDescription,
        entity.viewIdResourceName?.substringAfterLast("/")
    ).distinct().take(3)
}
```

**Acceptance**: Converts database entity to QuantizedElement

---

### Task 2.3: Implement classifyElementType()

**File**: `AVUQuantizerIntegration.kt` (new method)

**Add**:
```kotlin
private fun classifyElementType(className: String?): ElementType {
    if (className == null) return ElementType.OTHER

    return when {
        className.contains("Button", ignoreCase = true) -> ElementType.BUTTON
        className.contains("EditText", ignoreCase = true) -> ElementType.TEXT_FIELD
        className.contains("TextInput", ignoreCase = true) -> ElementType.TEXT_FIELD
        className.contains("CheckBox", ignoreCase = true) -> ElementType.CHECKBOX
        className.contains("Switch", ignoreCase = true) -> ElementType.SWITCH
        className.contains("Toggle", ignoreCase = true) -> ElementType.SWITCH
        className.contains("Spinner", ignoreCase = true) -> ElementType.DROPDOWN
        className.contains("Tab", ignoreCase = true) -> ElementType.TAB
        else -> ElementType.OTHER
    }
}
```

**Acceptance**: Correctly maps className to ElementType enum

---

## Phase 3: Navigation & Commands

### Task 3.1: Implement buildQuantizedNavigation()

**File**: `AVUQuantizerIntegration.kt:256-262`

**Replace**:
```kotlin
private fun buildQuantizedNavigation(
    packageName: String,
    screens: List<QuantizedScreen>
): List<QuantizedNavigation> {
    // In production, this queries navigation edges from database
    return emptyList()
}
```

**With**:
```kotlin
private suspend fun buildQuantizedNavigation(
    packageName: String,
    screens: List<QuantizedScreen>
): List<QuantizedNavigation> = withContext(Dispatchers.IO) {
    try {
        val screenHashes = screens.map { it.screenHash }.toSet()
        val transitions = screenContextRepository.getTransitionsForPackage(packageName)

        transitions
            .filter { it.fromScreenHash in screenHashes && it.toScreenHash in screenHashes }
            .map { transition ->
                QuantizedNavigation(
                    fromScreenHash = transition.fromScreenHash,
                    toScreenHash = transition.toScreenHash,
                    triggerElementVuid = transition.triggerElementUuid ?: "",
                    triggerLabel = transition.triggerElementLabel ?: "unknown"
                )
            }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to build navigation for $packageName", e)
        emptyList()
    }
}
```

**Acceptance**: Returns navigation edges between known screens

---

### Task 3.2: Implement buildKnownCommands()

**File**: `AVUQuantizerIntegration.kt:273-276`

**Replace**:
```kotlin
private fun buildKnownCommands(packageName: String): List<QuantizedCommand> {
    // In production, this queries discovered commands from database
    return emptyList()
}
```

**With**:
```kotlin
private suspend fun buildKnownCommands(packageName: String): List<QuantizedCommand> =
    withContext(Dispatchers.IO) {
        try {
            generatedCommandRepository.getCommandsForPackage(packageName)
                .filter { it.isActive }
                .sortedByDescending { it.usageCount }
                .map { cmd ->
                    QuantizedCommand(
                        phrase = cmd.phrase,
                        actionType = QuantizedActionType.valueOf(cmd.actionType.uppercase()),
                        targetElementVuid = cmd.targetElementUuid
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build commands for $packageName", e)
            emptyList()
        }
    }
```

**Acceptance**: Returns active commands sorted by usage

---

## Phase 4: Async Method Updates

### Task 4.1: Update hasQuantizedContext() to suspend

**File**: `AVUQuantizerIntegration.kt:85-93`

**Change signature and implementation**:
```kotlin
suspend fun hasQuantizedContext(packageName: String): Boolean {
    // Check cache
    if (contextCache.containsKey(packageName)) {
        return true
    }

    // Check if app has been learned (has exploration data)
    return hasLearnedData(packageName)
}
```

**Acceptance**: Method is now suspend, queries database

---

### Task 4.2: Update listQuantizedPackages() implementation

**File**: `AVUQuantizerIntegration.kt:100-103`

Already suspend, just needs implementation update (done in Task 1.2)

**Acceptance**: Uses getLearnedPackages() internally

---

## Phase 5: Cache Integration

### Task 5.1: Add Exploration Completion Listener

**File**: `AVUQuantizerIntegration.kt`

**Add method**:
```kotlin
/**
 * Called when exploration completes for a package
 * Invalidates cache to ensure fresh data on next query
 */
fun onExplorationCompleted(packageName: String) {
    scope.launch {
        invalidateCache(packageName)
        Log.d(TAG, "Cache invalidated for $packageName after exploration")
    }
}
```

**Wire in ExplorationEngine** when exploration completes.

**Acceptance**: Cache auto-invalidates on new learning

---

## Phase 6: Testing

### Task 6.1: Create Unit Tests

**File**: `apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/ai/quantized/AVUQuantizerIntegrationTest.kt`

**Tests**:
```kotlin
@Test fun classifyElementType_button_returnsButton()
@Test fun classifyElementType_editText_returnsTextField()
@Test fun classifyElementType_unknown_returnsOther()
@Test fun convertToQuantizedElement_withAllFields_mapsCorrectly()
@Test fun convertToQuantizedElement_withNulls_usesDefaults()
@Test fun buildAliases_deduplicates_andLimitsToThree()
```

**Acceptance**: All unit tests pass

---

### Task 6.2: Create Integration Tests

**File**: `apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/ai/quantized/AVUQuantizerIntegrationIT.kt`

**Tests**:
```kotlin
@Test fun getQuantizedContext_learnedApp_returnsData()
@Test fun getQuantizedContext_unknownApp_returnsNull()
@Test fun generateLLMPrompt_compact_createsValidPrompt()
@Test fun generateLLMPrompt_full_includesAllSections()
@Test fun cacheInvalidation_clearsCorrectPackage()
```

**Acceptance**: All integration tests pass

---

## Task Summary

| # | Task | Priority | Status |
|---|------|----------|--------|
| 1.1 | Add repository dependencies | P0 | Pending |
| 1.2 | Implement getLearnedPackages() | P0 | Pending |
| 1.3 | Implement hasLearnedData() | P0 | Pending |
| 2.1 | Implement buildQuantizedScreens() | P0 | Pending |
| 2.2 | Implement convertToQuantizedElement() | P0 | Pending |
| 2.3 | Implement classifyElementType() | P0 | Pending |
| 3.1 | Implement buildQuantizedNavigation() | P1 | Pending |
| 3.2 | Implement buildKnownCommands() | P1 | Pending |
| 4.1 | Update hasQuantizedContext() to suspend | P1 | Pending |
| 4.2 | Update listQuantizedPackages() | P1 | Pending |
| 5.1 | Add exploration completion listener | P1 | Pending |
| 6.1 | Create unit tests | P2 | Pending |
| 6.2 | Create integration tests | P2 | Pending |

---

## Verification Checklist

- [ ] All placeholder methods replaced with real implementations
- [ ] Database queries return actual data
- [ ] LLM prompts contain real app context
- [ ] Cache works correctly with TTL
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Build succeeds

---

**Next**: Execute implementation with `/i.implement` or start tasks manually

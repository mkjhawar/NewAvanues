# VoiceOS LearnApp Architecture - Developer Manual

**Version:** 1.0
**Date:** 2025-12-30
**Author:** VoiceOS Development Team
**Module:** VoiceOSCore LearnApp Integration

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Four Learning Methods](#four-learning-methods)
4. [Version-Based Hashing](#version-based-hashing)
5. [VUID Generation](#vuid-generation)
6. [Screen Hierarchy Tracking](#screen-hierarchy-tracking)
7. [NLU/LLM Integration](#nlullm-integration)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Overview

VoiceOS LearnApp provides intelligent app learning capabilities integrated directly into VoiceOSCore. It enables automatic discovery of UI elements and generation of voice commands for any Android application.

### Key Features

| Feature | Description |
|---------|-------------|
| **Just-In-Time Learning** | 64ms passive learning during normal app usage |
| **Version-Aware Caching** | Skip rescanning when app version unchanged |
| **VUID Generation** | Stable identifiers for UI elements across sessions |
| **Hierarchy Tracking** | Navigation graph with screen-to-screen transitions |
| **NLU-Ready Context** | Exports in formats suitable for LLM consumption |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ AccessibilityEvent                                          │
└─────────────┬───────────────────────────────────────────────┘
              │
    ┌─────────▼─────────┐     ┌────────────────────┐
    │ VoiceOSService    │────►│ Event Dispatch     │
    │                   │     │ Debouncing (300ms) │
    └─────────┬─────────┘     └────────────────────┘
              │
    ┌─────────▼─────────┐     ┌────────────────────┐
    │ JustInTimeLearner │────►│ Version Check      │
    │ (64ms processing) │     │ Hash Deduplication │
    └─────────┬─────────┘     └────────────────────┘
              │
    ┌─────────▼─────────┐     ┌────────────────────┐
    │ LearnAppCore      │────►│ VUID Generation    │
    │ (UUID + Commands) │     │ Voice Commands     │
    └─────────┬─────────┘     └────────────────────┘
              │
    ┌─────────▼─────────┐     ┌────────────────────┐
    │ NavigationGraph   │────►│ Screen Hierarchy   │
    │ (Builder)         │     │ Edge Tracking      │
    └─────────┬─────────┘     └────────────────────┘
              │
    ┌─────────▼─────────┐     ┌────────────────────┐
    │ AIContextSerializer│────►│ .vos File Export  │
    │ AVUQuantizer      │     │ LLM Prompts Ready  │
    └───────────────────┘     └────────────────────┘
```

### Key Classes

| Class | Location | Purpose |
|-------|----------|---------|
| `JustInTimeLearner` | `learnapp/jit/` | Passive 64ms screen learning |
| `LearnAppCore` | `learnapp/core/` | UUID + command generation |
| `LearnAppIntegration` | `learnapp/integration/` | VoiceOSService bridge |
| `NavigationGraph` | `learnapp/navigation/` | Screen hierarchy tracking |
| `AIContextSerializer` | `learnapp/ai/` | Export to .vos and LLM |
| `AVUQuantizerIntegration` | `learnapp/ai/quantized/` | NLU-optimized context |

---

## Four Learning Methods

### 1. Accessibility Scraping (UIScrapingEngine)

**Purpose:** Extract UI elements from accessibility tree
**Location:** `accessibility/extractors/UIScrapingEngine.kt`

```kotlin
// Extracts elements from AccessibilityNodeInfo
val elements = uiScrapingEngine.scrapeScreen(rootNode)
// Returns: List<ScrapedElement> with bounds, text, actions
```

### 2. JIT Learning (JustInTimeLearner)

**Purpose:** Passive learning during normal app usage
**Location:** `learnapp/jit/JustInTimeLearner.kt`

```kotlin
// Called on every accessibility event
fun onAccessibilityEvent(event: AccessibilityEvent) {
    // Debounce: 300ms between processing
    // Hash screen, check cache, learn if new
    learnCurrentScreen(event, packageName)
}
```

**Performance:**
- Processing time: ~64ms per screen
- Skip rate: ~80% for unchanged screens
- Memory: Minimal (deferred batch processing)

### 3. LearnAppLite (LearnAppCore)

**Purpose:** Batch element processing with VUID generation
**Location:** `learnapp/core/LearnAppCore.kt`

```kotlin
// Process element with mode selection
val result = core.processElement(
    element = elementInfo,
    packageName = "com.example.app",
    mode = ProcessingMode.IMMEDIATE  // or BATCH
)
// Returns: ElementProcessingResult with UUID and command
```

**Processing Modes:**
- `IMMEDIATE`: Insert to database now (~10ms/element)
- `BATCH`: Queue for batch insert (~0.1ms/element, 20x faster)

### 4. LearnApp Dev (ExplorationEngine)

**Purpose:** Deep app exploration with DFS navigation
**Location:** `learnapp/exploration/ExplorationEngine.kt`

```kotlin
// Full app exploration
explorationEngine.startExploration(packageName)
// DFS through all screens, clicking elements
// Builds complete NavigationGraph
```

---

## Version-Based Hashing

Prevents rescanning apps when version hasn't changed.

### Implementation

**File:** `JustInTimeLearner.kt` (lines 347-400)

```kotlin
private suspend fun learnCurrentScreen(event: AccessibilityEvent, packageName: String) {
    // 1. Calculate screen hash (cheap operation)
    val currentHash = calculateScreenHash(packageName)

    // 2. Check database for existing screen
    val existingScreen = getScreenByHash(currentHash, packageName)

    if (existingScreen != null) {
        // 3. Validate app version
        val currentVersion = versionDetector?.getVersion(packageName)?.versionName

        if (existingScreen.appVersion == currentVersion) {
            // FAST PATH: Load from cache, skip scraping
            Log.d(TAG, "Screen already learned (v${currentVersion}) - loading from DB")
            loadCommandsFromCache(existingScreen)
            return
        } else {
            // Version changed - rescan
            Log.i(TAG, "App version changed: ${existingScreen.appVersion} → $currentVersion")
        }
    }

    // 4. NEW SCREEN: Full scrape with VUID deduplication
    // ... proceed with learning
}
```

### Log Messages

| Log Level | Message | Meaning |
|-----------|---------|---------|
| `D` | `Screen X already learned (vY)` | Cache hit, skipping |
| `I` | `App version changed: X → Y` | Rescanning due to update |
| `I` | `Hash-based skip achieved Nms savings` | Performance gain |

---

## VUID Generation

Stable identifiers for UI elements across sessions.

### Format

```
{packageName}.{type}-{hash}
```

**Examples:**
- `com.android.settings.button-7f89458eeb44`
- `com.google.chrome.input-3a2b1c4d5e6f`

### Implementation

**File:** `LearnAppCore.kt` (lines 200-270)

```kotlin
private fun generateUUID(element: ElementInfo, packageName: String): String {
    // Calculate element hash from properties
    val elementHash = calculateElementHash(element)

    // Determine element type
    val elementType = when {
        element.isClickable -> "button"
        element.isEditText() -> "input"
        element.isScrollable -> "scroll"
        else -> "element"
    }

    // Create stable UUID
    return "$packageName.$elementType-$elementHash"
}

private fun calculateElementHash(element: ElementInfo): String {
    // Fingerprint from combined properties
    val fingerprint = buildString {
        append(element.className)
        append("|")
        append(element.resourceId)
        append("|")
        append(element.text)
        append("|")
        append(element.contentDescription)
        append("|")
        append("${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}")
    }

    // MD5 hash, first 12 characters
    val md = MessageDigest.getInstance("MD5")
    val hashBytes = md.digest(fingerprint.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }.take(12)
}
```

### Validation

**File:** `InputValidator.kt`

```kotlin
// UUID pattern allows package-prefixed VUIDs
private val UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$")

fun validateUuid(uuid: String?) {
    require(!uuid.isNullOrBlank()) { "UUID cannot be null or empty" }
    require(uuid.length <= 64) { "UUID too long" }
    require(UUID_PATTERN.matcher(uuid).matches()) { "Invalid UUID format" }
}
```

---

## Screen Hierarchy Tracking

Builds navigation graph during exploration.

### Data Structures

**File:** `NavigationGraph.kt`

```kotlin
data class NavigationGraph(
    val packageName: String,
    val nodes: Map<String, ScreenNode>,  // screenHash → node
    val edges: List<NavigationEdge>      // transitions
)

data class ScreenNode(
    val screenHash: String,
    val activityName: String? = null,
    val elements: List<String>,  // element UUIDs
    val timestamp: Long
)

data class NavigationEdge(
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long
)
```

### Builder Usage

**File:** `NavigationGraphBuilder.kt`

```kotlin
val builder = NavigationGraphBuilder("com.example.app")

// Add screen
builder.addScreen(screenState, listOf("uuid1", "uuid2"))

// Add navigation edge
builder.addEdge(fromHash, clickedUuid, toHash)

// Build final graph
val graph = builder.build()

// Query operations
val reachable = graph.getReachableScreens("screenHash")
val path = graph.findPath("startHash", "endHash")
val stats = graph.getStats()
```

### Statistics

```kotlin
data class GraphStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val averageOutDegree: Float,
    val maxDepth: Int
)
```

---

## NLU/LLM Integration

Infrastructure for AI-powered voice command understanding.

### Components

| Component | File | Purpose |
|-----------|------|---------|
| `AIContext` | `ai/AIContext.kt` | Data models for LLM |
| `AIContextSerializer` | `ai/AIContextSerializer.kt` | Export to .vos files |
| `AVUQuantizerIntegration` | `ai/quantized/` | NLU-optimized context |
| `LLMPromptFormat` | `ai/LLMPromptFormat.kt` | Prompt formats |

### Export Formats

#### 1. AVU Format (.vos)

```
# Avanues Universal Format v1.0
# Type: VOS
---
schema: avu-1.0
version: 1.0.0
---
APP:com.example.app:ExampleApp:1735548211000
STA:5:42:8:8.4:3:75.0
SCR:abc123:MainActivity:1735548200000:15
ELM:uuid1:Settings:Button:click:10,20,100,60
NAV:abc123:def456:uuid1:Settings:1735548201000
---
```

#### 2. LLM Prompt (Full)

```kotlin
val serializer = AIContextSerializer(context, databaseManager)
val aiContext = serializer.generateContext(navigationGraph)
val prompt = serializer.toLLMPrompt(aiContext, "Open settings")
```

Output:
```markdown
# App Navigation Context

**App**: com.example.app
**User Goal**: Open settings

## Statistics
- **Screens Discovered**: 5
- **Total Actionable Elements**: 42

## Available Screens and Actions
### Screen 1
**Activity**: MainActivity
**Actionable Elements**:
- **Settings** (Button)
  - UUID: uuid1
  - Actions: click
```

#### 3. Compact Prompt

```kotlin
val compact = serializer.toCompactPrompt(aiContext, "Open settings")
```

Output:
```
App: com.example.app
Goal: Open settings
Screens: 5, Elements: 42

Screen abc123: Settings, Profile, Help, Logout
```

### Action Prediction

```kotlin
val quantizer = AVUQuantizerIntegration(context, repositories...)
val prompt = quantizer.generateActionPredictionPrompt(
    packageName = "com.example.app",
    currentScreenHash = "abc123",
    userIntent = "go to settings"
)
```

---

## Testing

### Autonomous Test Script

**Location:** `/tmp/voiceos-autonomous-test.sh`

```bash
#!/bin/bash
# VoiceOS Autonomous Testing Regime

APPS=(
    "com.android.settings|android.settings.SETTINGS|Settings"
    "com.google.android.apps.messaging|android.intent.action.MAIN|Messages"
    "com.android.contacts|android.intent.action.MAIN|Contacts"
)

# Clear logs, ensure VoiceOS running
clear_logs
ensure_voiceos

# Test each app
for app_info in "${APPS[@]}"; do
    launch_and_learn "$pkg" "$action" "$name"
    capture_learning_logs "$name"
done

# Generate report
generate_report
```

### Test Results Interpretation

| Metric | Expected | Meaning |
|--------|----------|---------|
| JIT Captures | 10+ | Screens being processed |
| Commands Generated | 100+ | Voice commands created |
| Screens Learned | 5+ | Unique screens cached |
| Errors | 0 | No critical failures |

### Log Analysis

```bash
# JIT Learning logs
adb logcat | grep -E "JIT|JustInTime|JitElement"

# LearnApp logs
adb logcat | grep -E "LearnApp|LearnAppCore|Exploration"

# Command generation
adb logcat | grep -E "command.*gen|Generated.*command|SPEECH_TEST"

# Database operations
adb logcat | grep -E "database|SQLite|insert|persist"
```

---

## Troubleshooting

### Common Issues

#### 1. Invalid UUID Format Error

**Error:** `IllegalArgumentException: Invalid UUID format: com.app.button-abc123`

**Cause:** UUID pattern didn't allow dots for package-prefixed VUIDs

**Fix:** Updated `UUID_PATTERN` in both `InputValidator.kt` and `SecurityValidator.kt`:
```kotlin
// Before: "^[a-zA-Z0-9-]+$"
// After:
private val UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$")
```

#### 2. SQLite Constraint Violation

**Error:** `UNIQUE constraint failed: ScrapedElement.elementHash`

**Cause:** Attempting to insert duplicate elements

**Solution:** VUID deduplication handles this - elements are filtered before insert:
```kotlin
val newElements = deduplicateByVUID(capturedElements, packageName)
```

#### 3. LearnApp Not Initialized

**Error:** `LearnAppIntegration not initialized`

**Cause:** Deferred initialization not triggered

**Solution:** LearnApp initializes on first accessibility event:
```kotlin
// VoiceOSService.kt
if (!learnAppInitialized) {
    initializeLearnApp()
}
```

#### 4. No Commands in Database

**Log:** `No database commands found to register`

**Note:** This is expected if CommandDatabase migration is pending. Commands are still registered from static commands and runtime generation.

---

## Related Documentation

- [VoiceOS Developer Manual](VoiceOS-P2-Features-Developer-Manual-51211-V1.md)
- [Security Validation](../../plans/VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md)
- [Database Schema](../../../core/database/README.md)

---

**Last Updated:** 2025-12-30
**Tested On:** Android API 34, VoiceOS v2.2.0

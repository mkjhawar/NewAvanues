# Chapter 39: Intent Routing Architecture

**Author:** AI Development Team
**Date:** 2025-11-17
**Status:** ✅ IMPLEMENTED (Phase 1 + 1.5)
**Implementation Date:** 2025-11-17
**Related:** Chapter 36 (VoiceOS Delegation), Chapter 34 (Intent Management), Chapter 37 (.AVA Format)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Decision](#architecture-decision)
3. [Component Architecture](#component-architecture)
4. [Routing Decision Logic](#routing-decision-logic)
5. [Command Categories](#command-categories)
6. [Integration Points](#integration-points)
7. [Execution Flow](#execution-flow)
8. [Error Handling](#error-handling)
9. [Performance Characteristics](#performance-characteristics)
10. [Phase Roadmap](#phase-roadmap)
11. [Usage Examples](#usage-examples)
12. [Testing Strategy](#testing-strategy)

---

## Overview

### Problem Statement

AVA and VoiceOS both handle voice commands, but with different capabilities:
- **AVA**: Direct system APIs (WiFi, Bluetooth, Volume, Media)
- **VoiceOS**: Accessibility-based commands (Gestures, Cursor, UI Automation)

Without intelligent routing, jurisdiction conflicts occur when both systems attempt to handle the same command category.

### Solution: Category-Based Intent Routing

A unified NLU frontend (AVA) classifies all commands, then routes execution based on capability:

```
User Command (Voice OR Typed)
    ↓
AVA NLU Classification (Unified)
    ↓
IntentRouter (Category-Based Decision)
    ↓
    ├─→ AVA Execution (40 commands: connectivity, volume, media, system)
    │   └─→ ActionsManager.executeAction()
    │
    └─→ VoiceOS IPC (84 commands: gestures, cursor, accessibility)
        └─→ VoiceOSConnection.executeCommand() → AIDL
```

### Benefits

✅ **Zero Conflicts**: Clear jurisdiction for all 124 commands
✅ **Unified NLU**: Single classification engine, consistent confidence scoring
✅ **Graceful Degradation**: 40 AVA commands work even when VoiceOS unavailable
✅ **Future-Proof**: Easy to add new categories or reroute commands
✅ **Input Agnostic**: Same experience for voice commands AND typed text

---

## Architecture Decision

### Why Category-Based Routing?

**Considered Alternatives:**

1. **Dual NLU (Rejected)**
   - ❌ Duplicate classification overhead
   - ❌ Inconsistent confidence scores
   - ❌ Two learning systems to maintain

2. **Intent-Level Routing (Rejected)**
   - ❌ 124 routing rules to maintain
   - ❌ Hard to add new commands
   - ❌ Brittle and error-prone

3. **Category-Based Routing (CHOSEN)** ✅
   - ✅ 21 categories = 21 routing rules
   - ✅ Easy to understand and maintain
   - ✅ Natural grouping by capability
   - ✅ Scales well with new commands

### Architecture Principles

1. **Single Source of Truth**: AVA's NLU is the only classification engine
2. **Capability-Based**: Route by what each system CAN do, not what it should do
3. **Fail-Safe**: Always provide meaningful error messages
4. **Performance**: Routing decision <5ms overhead
5. **Consistency**: Typed and voice commands follow identical paths

---

## Component Architecture

### 1. IntentRouter

**Location:** `common/Actions/src/main/kotlin/com/augmentalis/ava/features/actions/IntentRouter.kt`

**Responsibility:** Make routing decisions based on command category

**P2 SOLID Fix:** IntentRouter now uses `CategoryCapabilityRegistry` instead of hardcoded category sets. This allows categories to be registered dynamically without modifying IntentRouter (Open/Closed Principle). See [Chapter 72: SOLID Architecture](Developer-Manual-Chapter72-SOLID-Architecture.md).

**Constructor:**
```kotlin
class IntentRouter(
    private val context: Context,
    private val voiceOSConnection: VoiceOSConnection? = null,
    private val categoryRegistry: CategoryCapabilityRegistry = CategoryCapabilityRegistry()
)
```

**CategoryCapabilityRegistry (Chapter 72):**
```kotlin
@Singleton
class CategoryCapabilityRegistry @Inject constructor() {
    enum class ExecutionTarget {
        AVA_LOCAL,      // Execute locally
        VOICEOS,        // Forward to VoiceOS
        FALLBACK_LLM    // Fall back to LLM
    }

    fun registerAVACategory(id: String, displayName: String, description: String)
    fun registerVoiceOSCategory(id: String, displayName: String, description: String)
    fun getExecutionTarget(categoryId: String): ExecutionTarget
    fun isAVACapable(categoryId: String): Boolean
    fun getAVACapableCategories(): Set<String>
}
```

**Default Categories:**

| Target | Categories |
|--------|------------|
| AVA_LOCAL | connectivity, volume, media, system, navigation, productivity, smart_home, information, calculation |
| VOICEOS | gesture, cursor, scroll, swipe, drag, keyboard, editing, gaze, overlays, dialog, menu, dictation, notifications |

**Key Methods:**

```kotlin
fun route(intent: String, category: String): RoutingDecision

fun getCategoryForIntent(intent: String): String

fun getStats(): Map<String, Any>
```

**Routing Decisions:**

```kotlin
sealed class RoutingDecision {
    // Execute locally in AVA
    data class ExecuteLocally(val intent: String, val category: String)

    // Forward to VoiceOS via IPC
    data class ForwardToVoiceOS(val intent: String, val category: String)

    // VoiceOS required but unavailable
    data class VoiceOSUnavailable(
        val intent: String,
        val category: String,
        val reason: String
    )

    // Unknown category, fallback to LLM
    data class FallbackToLLM(val intent: String, val category: String)
}
```

### 2. VoiceOSConnection

**Location:** `Universal/AVA/Features/Actions/src/main/kotlin/com/augmentalis/ava/features/actions/VoiceOSConnection.kt`

**Responsibility:** Manage IPC connection to VoiceOS accessibility service

**Connection States:**
```kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
```

**Key Methods:**

```kotlin
// Check if VoiceOS is installed
fun isVoiceOSInstalled(): Boolean

// Check if accessibility service is running
fun isAccessibilityServiceRunning(): Boolean

// Execute command via IPC
suspend fun executeCommand(intent: String, category: String): CommandResult

// Connection lifecycle
suspend fun connect(): Boolean
fun disconnect()
fun getConnectionState(): ConnectionState
```

**Command Results:**
```kotlin
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Failure(val error: String) : CommandResult()
}
```

### 3. ActionsManager (Enhanced)

**Location:** `Universal/AVA/Features/Actions/src/main/kotlin/com/augmentalis/ava/features/actions/ActionsManager.kt`

**Enhancements:**
- Added `IntentRouter` and `VoiceOSConnection` lazy properties
- New `executeActionWithRouting()` method
- New `getCategoryForIntent()` helper method
- New `getRoutingStats()` diagnostics method

**Key Method:**
```kotlin
suspend fun executeActionWithRouting(
    intent: String,
    category: String,
    utterance: String
): ActionResult {
    return when (val decision = intentRouter.route(intent, category)) {
        is RoutingDecision.ExecuteLocally -> {
            executeAction(intent, utterance)
        }
        is RoutingDecision.ForwardToVoiceOS -> {
            when (val result = voiceOSConnection.executeCommand(intent, category)) {
                is CommandResult.Success -> ActionResult.Success(result.message)
                is CommandResult.Failure -> ActionResult.Failure(result.error)
            }
        }
        is RoutingDecision.VoiceOSUnavailable -> {
            ActionResult.Failure("VoiceOS required: ${decision.reason}")
        }
        is RoutingDecision.FallbackToLLM -> {
            if (hasHandler(intent)) {
                executeAction(intent, utterance)
            } else {
                ActionResult.Failure("No handler found")
            }
        }
    }
}
```

### 4. VoiceOS AIDL Interfaces

**Location:** `Universal/AVA/Features/Actions/src/main/aidl/com/augmentalis/voiceoscore/accessibility/`

**Files:**
- `IVoiceOSService.aidl` - Main service interface
- `IVoiceOSCallback.aidl` - Callback interface for async results
- `CommandResult.aidl` - Parcelable result object

**IVoiceOSService Interface:**
```aidl
interface IVoiceOSService {
    boolean executeCommand(String commandText);
    boolean executeAccessibilityAction(String actionType, String parameters);
    String getServiceStatus();
    List<String> getAvailableCommands();
    void registerCallback(IVoiceOSCallback callback);
    void unregisterCallback(IVoiceOSCallback callback);
}
```

---

## Routing Decision Logic

### Decision Algorithm

```kotlin
fun route(intent: String, category: String): RoutingDecision {
    return when {
        // Step 1: Check if AVA can handle this
        category in AVA_CAPABLE_CATEGORIES -> {
            RoutingDecision.ExecuteLocally(intent, category)
        }

        // Step 2: Check if VoiceOS is required
        category in VOICEOS_ONLY_CATEGORIES -> {
            if (isVoiceOSAvailable()) {
                RoutingDecision.ForwardToVoiceOS(intent, category)
            } else {
                RoutingDecision.VoiceOSUnavailable(
                    intent,
                    category,
                    "VoiceOS accessibility service is not running"
                )
            }
        }

        // Step 3: Unknown category, attempt local fallback
        else -> {
            RoutingDecision.FallbackToLLM(intent, category)
        }
    }
}
```

### VoiceOS Availability Check

```kotlin
private fun isVoiceOSAvailable(): Boolean {
    // ADR-014 Phase 3: Use VoiceOSConnection to check actual accessibility service state
    // Returns false if voiceOSConnection is null (VoiceOS not integrated)
    return voiceOSConnection?.isReady() ?: false
}
```

**ADR-014 Update:** IntentRouter now uses `VoiceOSConnection.isReady()` for actual availability checks instead of a hardcoded `false`.

### Category Resolution (Phase 1 → Phase 2)

**Phase 1 (Original): Intent Name Pattern Matching**

```kotlin
fun getCategoryForIntent(intent: String): String {
    // Phase 1: Infer from intent name
    return when {
        intent.contains("wifi") || intent.contains("bluetooth") -> "connectivity"
        intent.contains("volume") -> "volume"
        intent.contains("cursor") -> "cursor"
        intent.contains("swipe") || intent.contains("gesture") -> "gesture"
        intent.contains("scroll") -> "scroll"
        intent.contains("keyboard") -> "keyboard"
        intent.contains("media") || intent.contains("play") || intent.contains("pause") -> "media"
        else -> "unknown"
    }
}
```

**Limitations:**
- Brittle string matching
- Doesn't handle edge cases
- Requires code changes to add new categories
- No metadata about category requirements

---

**Phase 2 (NEW: 2025-12-06): Database-Driven Category Lookup**

Categories are now stored in the database with full metadata, allowing dynamic category assignment without code changes.

**Database Schema:**

```kotlin
// IntentCategoryEntity.kt
@Entity(
    tableName = "intent_categories",
    indices = [Index(value = ["category"])]
)
data class IntentCategoryEntity(
    @PrimaryKey
    val intentName: String,          // e.g., "cursor_move_up"

    val category: String,             // e.g., "cursor"

    val requiresAccessibility: Boolean,  // true for VoiceOS commands

    val priority: Int = 0,            // 0 = normal, higher = override

    val description: String? = null   // Optional metadata
)
```

**Repository:**

```kotlin
// IntentCategoryRepository.kt
@Singleton
class IntentCategoryRepository @Inject constructor(
    private val db: AvaDatabase
) {
    // Get category for intent
    suspend fun getCategoryForIntent(intentName: String): String? {
        return db.intentCategoryDao().getCategoryForIntent(intentName)
    }

    // Get all intents in a category
    suspend fun getIntentsInCategory(category: String): List<String> {
        return db.intentCategoryDao().getIntentsByCategory(category)
    }

    // Check if intent requires accessibility
    suspend fun requiresAccessibility(intentName: String): Boolean {
        return db.intentCategoryDao().getIntent(intentName)?.requiresAccessibility ?: false
    }

    // Add or update intent category
    suspend fun setIntentCategory(
        intentName: String,
        category: String,
        requiresAccessibility: Boolean = false,
        priority: Int = 0
    ) {
        db.intentCategoryDao().insert(
            IntentCategoryEntity(intentName, category, requiresAccessibility, priority)
        )
    }
}
```

**Seeding Data:**

```kotlin
// CategorySeeder.kt
@Singleton
class CategorySeeder @Inject constructor(
    private val repository: IntentCategoryRepository
) {
    suspend fun seedCategories() {
        // Connectivity (4 intents)
        repository.setIntentCategory("turn_on_wifi", "connectivity")
        repository.setIntentCategory("turn_off_wifi", "connectivity")
        repository.setIntentCategory("bluetooth_on", "connectivity")
        repository.setIntentCategory("bluetooth_off", "connectivity")

        // Volume (19 intents)
        repository.setIntentCategory("volume_up", "volume")
        repository.setIntentCategory("volume_down", "volume")
        repository.setIntentCategory("volume_max", "volume")
        repository.setIntentCategory("mute", "volume")
        // ... 15 more volume intents

        // Cursor (7 intents, requires accessibility)
        repository.setIntentCategory("cursor_move_up", "cursor", requiresAccessibility = true)
        repository.setIntentCategory("cursor_move_down", "cursor", requiresAccessibility = true)
        repository.setIntentCategory("cursor_click", "cursor", requiresAccessibility = true)
        // ... 4 more cursor intents

        // Gesture (5 intents, requires accessibility)
        repository.setIntentCategory("swipe_up", "gesture", requiresAccessibility = true)
        repository.setIntentCategory("swipe_down", "gesture", requiresAccessibility = true)
        // ... 3 more gesture intents

        // ... Total: 124 intent mappings across 21 categories
    }
}
```

**Migration:**

```kotlin
// Migration from Phase 1 to Phase 2
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create intent_categories table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS intent_categories (
                intentName TEXT PRIMARY KEY NOT NULL,
                category TEXT NOT NULL,
                requiresAccessibility INTEGER NOT NULL DEFAULT 0,
                priority INTEGER NOT NULL DEFAULT 0,
                description TEXT
            )
        """)

        // Create index on category for fast lookups
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_intent_categories_category
            ON intent_categories(category)
        """)
    }
}
```

**Updated getCategoryForIntent():**

```kotlin
// ActionsManager.kt (Phase 2)
suspend fun getCategoryForIntent(intent: String): String {
    // Try database lookup first (Phase 2)
    val category = categoryRepository.getCategoryForIntent(intent)

    if (category != null) {
        return category
    }

    // Fallback to pattern matching (Phase 1 compatibility)
    return when {
        intent.contains("wifi") || intent.contains("bluetooth") -> "connectivity"
        intent.contains("volume") -> "volume"
        intent.contains("cursor") -> "cursor"
        // ... pattern matching as before
        else -> "unknown"
    }
}
```

**Benefits of Phase 2:**

| Feature | Phase 1 | Phase 2 |
|---------|---------|---------|
| **Data Storage** | Hardcoded in code | Database table |
| **Extensibility** | Code changes required | Add via database insert |
| **Metadata** | None | Accessibility requirement, priority, description |
| **Query Performance** | O(1) pattern match | O(1) indexed lookup |
| **Bulk Operations** | Not supported | Get all intents in category |
| **User Customization** | Not possible | Could allow user overrides |

**Category Statistics:**

```kotlin
// Get category distribution
suspend fun getCategoryStats(): Map<String, Int> {
    return categoryRepository.getIntentsByCategory()
        .groupBy { it.category }
        .mapValues { it.value.size }
}

// Example output:
// {
//   "connectivity": 4,
//   "volume": 19,
//   "media": 5,
//   "cursor": 7,
//   "gesture": 5,
//   "scroll": 2,
//   // ... 15 more categories
// }
```

---

## Command Categories

### AVA-Capable Categories (8)

| Category | Command Count | Examples | Permissions Required |
|----------|--------------|----------|---------------------|
| **connectivity** | 4 | turn_on_wifi, bluetooth_on | CHANGE_WIFI_STATE, BLUETOOTH_ADMIN |
| **volume** | 19 | volume_up, volume_max, mute | AUDIO_SERVICE |
| **media** | 5+ | play_music, pause_music, next_track | MEDIA_SESSION |
| **system** | 12+ | brightness_up, flashlight_on | SYSTEM_SETTINGS, WRITE_SETTINGS |
| **navigation** | 6 | open_app, go_home, recent_apps | PACKAGE_MANAGER |
| **productivity** | 8 | set_alarm, create_note | CALENDAR, CONTACTS |
| **smart_home** | 3 | control_lights (future) | IoT APIs |
| **information** | 5 | get_weather, show_time | NETWORK |

**Total AVA Commands:** ~40

### VoiceOS-Only Categories (13)

| Category | Command Count | Examples | Why VoiceOS? |
|----------|--------------|----------|-------------|
| **gesture** | 5 | swipe_up, swipe_down | AccessibilityService required |
| **cursor** | 7 | cursor_move_up, cursor_click | Overlay + touch injection |
| **scroll** | 2 | scroll_up, scroll_down | View traversal |
| **swipe** | 4 | swipe_left, swipe_right | Gesture recognition |
| **drag** | 3 | drag_start, drag_end | Multi-step gestures |
| **keyboard** | 9 | show_keyboard, keyboard_enter | IME control |
| **editing** | 8 | select_all, copy, paste | Text selection |
| **gaze** | 4 | gaze_cursor_on, gaze_click | Eye tracking hardware |
| **overlays** | 7 | show_overlay, hide_overlay | SYSTEM_ALERT_WINDOW |
| **dialog** | 4 | show_dialog, dismiss_dialog | AccessibilityNodeInfo |
| **menu** | 3 | open_menu, close_menu | Context menu control |
| **dictation** | 6 | start_dictation, stop_dictation | Speech-to-text overlay |
| **notifications** | 8 | read_notifications, dismiss | NotificationListenerService |

**Total VoiceOS Commands:** ~84

### Category Selection Rationale

**AVA handles commands that:**
- ✅ Use standard Android APIs (WiFi, Bluetooth, Audio)
- ✅ Require simple permissions (CHANGE_WIFI_STATE)
- ✅ Don't need UI automation

**VoiceOS handles commands that:**
- ✅ Require AccessibilityService permission
- ✅ Need UI traversal or automation
- ✅ Use overlay windows (SYSTEM_ALERT_WINDOW)
- ✅ Require gesture recognition
- ✅ Need specialized hardware (eye tracking)

---

## Integration Points

### 1. ChatViewModel Integration

**Location:** `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModel.kt`

**Change:** All action execution now uses routing (line 913-917)

```kotlin
// Before (Phase 0):
val actionResult = actionsManager.executeAction(
    intent = classifiedIntent,
    utterance = text.trim()
)

// After (Phase 1.5):
val category = actionsManager.getCategoryForIntent(classifiedIntent)

val actionResult = actionsManager.executeActionWithRouting(
    intent = classifiedIntent,
    category = category,
    utterance = text.trim()
)
```

**Impact:**
- ✅ Both typed AND voice commands use routing
- ✅ Consistent user experience across input methods
- ✅ Automatic decision-making (no user intervention)

### 2. NLU Classification Flow

**Unchanged:** NLU classification remains identical

```
User Input → IntentClassifier.classifyIntent()
    ↓
IntentClassification {
    intent: "cursor_move_up"
    confidence: 0.92
    inferenceTimeMs: 45
}
```

**New:** Category added during action execution

```
IntentClassification → getCategoryForIntent()
    ↓
category = "cursor"
    ↓
executeActionWithRouting(intent, category, utterance)
```

### 3. Voice Command Entry Points

**All entry points now use routing:**

1. **Chat Input** (typed text)
   - `ChatViewModel.sendMessage()` → `executeActionWithRouting()`

2. **Voice Recognition** (speech input)
   - `ChatViewModel.sendMessage()` → `executeActionWithRouting()`

3. **Overlay Service** (voice button)
   - `AvaChatOverlayService` → `ChatViewModel.sendMessage()` → routing

4. **Future: Gaze Integration** (Phase 4)
   - Gaze commands → NLU → routing

---

## Execution Flow

### Example 1: AVA-Capable Command

```
User: "turn on wifi"
    ↓
┌─────────────────────────────────────────┐
│ NLU Classification                      │
│ - Intent: turn_on_wifi                  │
│ - Confidence: 0.92                      │
│ - Category: <not yet in classification> │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ getCategoryForIntent("turn_on_wifi")    │
│ → Returns "connectivity"                │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ IntentRouter.route(                     │
│   "turn_on_wifi", "connectivity"        │
│ )                                       │
│ → Decision: ExecuteLocally              │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ ActionsManager.executeAction()          │
│ → WiFiActionHandler executes locally    │
│ → WiFi enabled ✅                        │
└─────────────────────────────────────────┘
    ↓
Result: ActionResult.Success("WiFi enabled")
```

**Performance:**
- NLU classification: 45-100ms
- Category inference: <1ms
- Routing decision: <5ms
- WiFi execution: 50-150ms
- **Total: 95-255ms**

### Example 2: VoiceOS-Only Command

```
User: "swipe up"
    ↓
┌─────────────────────────────────────────┐
│ NLU Classification                      │
│ - Intent: swipe_up                      │
│ - Confidence: 0.95                      │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ getCategoryForIntent("swipe_up")        │
│ → Returns "gesture"                     │
└─────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────┐
│ IntentRouter.route("swipe_up", "gesture")│
│ → Check: isVoiceOSAvailable() = false  │
│ → Decision: VoiceOSUnavailable          │
└─────────────────────────────────────────┘
    ↓
Result: ActionResult.Failure(
    "This command requires VoiceOS accessibility service. " +
    "Reason: VoiceOS accessibility service is not running"
)
```

**Phase 2 (IPC Implemented):**
```
User: "swipe up"
    ↓
[NLU Classification] → intent=swipe_up, confidence=0.95
    ↓
[Category Inference] → category=gesture
    ↓
[IntentRouter] → ForwardToVoiceOS
    ↓
[VoiceOSConnection] → executeCommand() via AIDL
    ↓
[IVoiceOSService] → executeCommand("swipe up")
    ↓
[VoiceOS Accessibility] → Perform swipe gesture
    ↓
Result: CommandResult.Success("Swipe executed")
```

**Performance (Phase 2 Estimate):**
- NLU classification: 45-100ms
- Category inference: <1ms
- Routing decision: <5ms
- IPC overhead: 20-50ms
- VoiceOS execution: 100-200ms
- **Total: 165-355ms**

### Example 3: Unknown Category

```
User: "do a backflip"
    ↓
[NLU Classification] → intent=unknown, confidence=0.25
    ↓
[Category Inference] → category=unknown
    ↓
[IntentRouter] → FallbackToLLM
    ↓
[Check hasHandler()] → false
    ↓
Result: ActionResult.Failure("No handler found for intent: unknown")
```

---

## Error Handling

### 1. VoiceOS Not Installed

```kotlin
// VoiceOSConnection checks package manager
fun isVoiceOSInstalled(): Boolean {
    return try {
        context.packageManager.getPackageInfo(VOICEOS_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

// Routing decision returns helpful error
RoutingDecision.VoiceOSUnavailable(
    intent,
    category,
    "VoiceOS app is not installed. Please install VoiceOS for accessibility commands."
)
```

**User Experience:**
```
User: "swipe up"
AVA: "This command requires VoiceOS accessibility service.
      VoiceOS app is not installed. Please install VoiceOS
      for accessibility commands."
```

### 2. Accessibility Service Not Running

```kotlin
// VoiceOSConnection checks accessibility settings
fun isAccessibilityServiceRunning(): Boolean {
    // Phase 1: Returns false (placeholder)
    // Phase 2: Check Settings.Secure for enabled services
    return false
}

// Error message guides user to enable service
RoutingDecision.VoiceOSUnavailable(
    intent,
    category,
    "VoiceOS accessibility service is not running.
     Please enable it in Settings > Accessibility."
)
```

### 3. IPC Connection Failure (Phase 2)

```kotlin
suspend fun executeCommand(intent: String, category: String): CommandResult {
    return try {
        val success = voiceOSService?.executeCommand(intent)
        if (success == true) {
            CommandResult.Success("Command executed via VoiceOS")
        } else {
            CommandResult.Failure("VoiceOS failed to execute command")
        }
    } catch (e: RemoteException) {
        CommandResult.Failure("IPC error: ${e.message}")
    } catch (e: SecurityException) {
        CommandResult.Failure("Permission denied: ${e.message}")
    }
}
```

### 4. Unknown Intent

```kotlin
// FallbackToLLM decision allows graceful degradation
is RoutingDecision.FallbackToLLM -> {
    if (hasHandler(intent)) {
        // Try local execution as fallback
        executeAction(intent, utterance)
    } else {
        // No handler available, return error
        ActionResult.Failure("No handler found for intent: $intent")
    }
}
```

---

## Performance Characteristics

### Routing Overhead

**Measured Performance (Phase 1):**
- Category inference: <1ms (simple string matching)
- Routing decision: <5ms (Set membership checks)
- Total overhead: **<6ms per command**

**Impact:** Negligible (0.6-2.4% of total execution time)

### Command Execution Times

| Command Type | AVA Local | VoiceOS IPC (Est.) | Difference |
|-------------|-----------|-------------------|-----------|
| WiFi On/Off | 50-150ms | N/A | - |
| Volume Up/Down | 20-50ms | N/A | - |
| Swipe Gesture | N/A | 150-250ms | - |
| Cursor Move | N/A | 100-200ms | - |

**Observations:**
- AVA commands: 45-100ms faster (no IPC overhead)
- VoiceOS commands: Acceptable latency for UI automation
- Routing adds negligible overhead (<6ms)

### Memory Footprint

**IntentRouter:**
- 2 static sets (21 categories total): ~500 bytes
- No runtime allocations during routing
- **Total: <1 KB**

**VoiceOSConnection:**
- Connection state: ~100 bytes
- AIDL stub reference: ~50 bytes
- **Total: <500 bytes**

**Overall Impact:** Negligible (<2 KB total)

---

## Phase Roadmap

### ✅ Phase 1: Foundation (COMPLETE)

**Completed:**
- [x] IntentRouter with category-based routing
- [x] VoiceOSConnection stub implementation
- [x] AIDL interfaces copied from VoiceOS
- [x] ActionsManager integration
- [x] Routing decision logic

**Artifacts:**
- `IntentRouter.kt` (180 lines)
- `VoiceOSConnection.kt` (150 lines, stub)
- `ActionsManager.kt` (+75 lines)
- `IVoiceOSService.aidl` (142 lines)
- `IVoiceOSCallback.aidl` (40 lines)
- `CommandResult.aidl` (10 lines)

### ✅ Phase 1.5: ChatViewModel Integration (COMPLETE)

**Completed:**
- [x] Update ChatViewModel to use executeActionWithRouting()
- [x] Add getCategoryForIntent() to ActionsManager
- [x] Category automatically inferred from intent name
- [x] Both typed and voice commands use unified routing

**Artifacts:**
- `ChatViewModel.kt` (updated line 913-917)
- `ActionsManager.kt` (+12 lines)

### 🔄 Phase 2: IPC Implementation (NEXT)

**Tasks:**
- [ ] Implement AIDL service binding in VoiceOSConnection
- [ ] Handle connection lifecycle (connect/disconnect)
- [ ] Implement command execution via IPC
- [ ] Add error handling and retry logic
- [ ] Test with VoiceOS service running

**Estimated Effort:** 8 hours

**Key Deliverables:**
1. Service binding with `ServiceConnection`
2. AIDL method invocation (`executeCommand()`)
3. Error handling for RemoteException, SecurityException
4. Connection state management (Connected/Disconnected/Error)
5. E2E test: AVA → VoiceOS → Gesture execution

### 🔄 Phase 3: Testing & Validation

**Tasks:**
- [ ] Unit tests for IntentRouter (routing logic)
- [ ] Integration tests for routing flow
- [ ] E2E tests with VoiceOS IPC
- [ ] Performance benchmarks (latency, throughput)
- [ ] Error scenario testing (VoiceOS unavailable, IPC failure)

**Estimated Effort:** 6 hours

**Coverage Target:** 90%+ for IntentRouter, ActionsManager, VoiceOSConnection

### 🔄 Phase 4: Enhanced NLU Integration

**Tasks:**
- [ ] Add category field to IntentExampleEntity
- [ ] Database migration to add category column
- [ ] Update .ava file parser to read "cat" field
- [ ] Update NLU classification to return category
- [ ] Remove category inference (use database category)
- [ ] Add UI indicator for routing decisions
- [ ] Add VoiceOS availability indicator

**Estimated Effort:** 10 hours

**Key Deliverables:**
1. IntentExampleEntity with category field
2. Room migration to add column
3. IntentClassification with category field
4. UI showing "Routed to: AVA" or "Routed to: VoiceOS"

### 🔄 Phase 5: Gaze Integration (Future)

**Tasks:**
- [ ] Add gaze tracking to AVA NLU/LLM assistance
- [ ] Integrate gaze commands with routing
- [ ] Test gaze + voice multimodal interaction
- [ ] Add gaze-specific routing rules

**Estimated Effort:** 16 hours (dependent on gaze hardware)

**Prerequisites:**
- Gaze tracking hardware available
- VoiceOS gaze support implemented

---

## Usage Examples

### Example 1: Check Routing Statistics

```kotlin
// In ChatViewModel or debug UI
val stats = actionsManager.getRoutingStats()

println(stats)
// Output:
// {
//   "ava_capable_categories": 8,
//   "voiceos_only_categories": 13,
//   "voiceos_available": false,
//   "voiceos_installed": false,
//   "voiceos_connection": "Disconnected"
// }
```

### Example 2: Manual Routing Decision

```kotlin
// If you need to make routing decision directly
val router = IntentRouter(context)

val decision = router.route("cursor_move_up", "cursor")

when (decision) {
    is IntentRouter.RoutingDecision.ExecuteLocally -> {
        println("Execute locally: ${decision.intent}")
    }
    is IntentRouter.RoutingDecision.ForwardToVoiceOS -> {
        println("Forward to VoiceOS: ${decision.intent}")
    }
    is IntentRouter.RoutingDecision.VoiceOSUnavailable -> {
        println("VoiceOS unavailable: ${decision.reason}")
    }
    is IntentRouter.RoutingDecision.FallbackToLLM -> {
        println("Fallback to LLM: ${decision.intent}")
    }
}
```

### Example 3: Test with Specific Command

```kotlin
// Test routing for WiFi command
@Test
fun testWiFiRouting() {
    val router = IntentRouter(mockContext)

    val decision = router.route("turn_on_wifi", "connectivity")

    assertTrue(decision is IntentRouter.RoutingDecision.ExecuteLocally)
    assertEquals("turn_on_wifi", decision.intent)
    assertEquals("connectivity", decision.category)
}

// Test routing for gesture command
@Test
fun testGestureRouting() {
    val router = IntentRouter(mockContext)

    val decision = router.route("swipe_up", "gesture")

    // Phase 1: VoiceOS not available
    assertTrue(decision is IntentRouter.RoutingDecision.VoiceOSUnavailable)

    // Phase 2: Would be ForwardToVoiceOS
}
```

### Example 4: Force Specific Routing (Override)

```kotlin
// Advanced: Override routing decision for testing
class TestActionsManager(context: Context) : ActionsManager(context) {
    var forceVoiceOSAvailable = false

    override fun executeActionWithRouting(
        intent: String,
        category: String,
        utterance: String
    ): ActionResult {
        // Override routing for testing
        if (forceVoiceOSAvailable && category in VOICEOS_ONLY_CATEGORIES) {
            return ActionResult.Success("Simulated VoiceOS execution")
        }
        return super.executeActionWithRouting(intent, category, utterance)
    }
}
```

---

## Testing Strategy

### Unit Tests

**IntentRouterTest.kt:**
```kotlin
class IntentRouterTest {
    @Test
    fun `route WiFi command to AVA`() {
        val decision = router.route("turn_on_wifi", "connectivity")
        assertTrue(decision is RoutingDecision.ExecuteLocally)
    }

    @Test
    fun `route gesture command to VoiceOS when available`() {
        // Mock VoiceOS available
        val decision = router.route("swipe_up", "gesture")
        assertTrue(decision is RoutingDecision.ForwardToVoiceOS)
    }

    @Test
    fun `route gesture command to unavailable when VoiceOS not running`() {
        // Mock VoiceOS unavailable
        val decision = router.route("swipe_up", "gesture")
        assertTrue(decision is RoutingDecision.VoiceOSUnavailable)
    }

    @Test
    fun `infer category from intent name`() {
        assertEquals("connectivity", router.getCategoryForIntent("turn_on_wifi"))
        assertEquals("volume", router.getCategoryForIntent("volume_up"))
        assertEquals("cursor", router.getCategoryForIntent("cursor_move_left"))
        assertEquals("unknown", router.getCategoryForIntent("do_backflip"))
    }
}
```

**VoiceOSConnectionTest.kt:**
```kotlin
class VoiceOSConnectionTest {
    @Test
    fun `check VoiceOS installation status`() {
        // Mock PackageManager
        val installed = connection.isVoiceOSInstalled()
        // Assert based on mock
    }

    @Test
    fun `execute command fails when VoiceOS not installed`() = runBlocking {
        val result = connection.executeCommand("swipe_up", "gesture")
        assertTrue(result is CommandResult.Failure)
        assertTrue(result.error.contains("not installed"))
    }

    @Test
    fun `execute command fails when accessibility service not running`() = runBlocking {
        // Mock VoiceOS installed but service not running
        val result = connection.executeCommand("swipe_up", "gesture")
        assertTrue(result is CommandResult.Failure)
        assertTrue(result.error.contains("not running"))
    }
}
```

**ActionsManagerTest.kt:**
```kotlin
class ActionsManagerTest {
    @Test
    fun `executeActionWithRouting calls executeAction for AVA commands`() = runBlocking {
        val result = actionsManager.executeActionWithRouting(
            "turn_on_wifi",
            "connectivity",
            "turn on wifi"
        )
        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `executeActionWithRouting returns error for VoiceOS commands when unavailable`() = runBlocking {
        val result = actionsManager.executeActionWithRouting(
            "swipe_up",
            "gesture",
            "swipe up"
        )
        assertTrue(result is ActionResult.Failure)
        assertTrue(result.message.contains("VoiceOS"))
    }

    @Test
    fun `getCategoryForIntent returns correct category`() {
        assertEquals("connectivity", actionsManager.getCategoryForIntent("turn_on_wifi"))
        assertEquals("cursor", actionsManager.getCategoryForIntent("cursor_move_up"))
    }
}
```

### Integration Tests

**RoutingIntegrationTest.kt:**
```kotlin
@HiltAndroidTest
class RoutingIntegrationTest {
    @Test
    fun `end-to-end routing for WiFi command`() = runBlocking {
        // Simulate user input
        val classification = nluClassifier.classifyIntent(
            "turn on wifi",
            candidateIntents
        )

        // Get category
        val category = actionsManager.getCategoryForIntent(classification.intent)

        // Execute with routing
        val result = actionsManager.executeActionWithRouting(
            classification.intent,
            category,
            "turn on wifi"
        )

        // Verify
        assertTrue(result is ActionResult.Success)
        // Verify WiFi is actually enabled
    }

    @Test
    fun `end-to-end routing for gesture command when VoiceOS unavailable`() = runBlocking {
        val classification = nluClassifier.classifyIntent(
            "swipe up",
            candidateIntents
        )

        val category = actionsManager.getCategoryForIntent(classification.intent)

        val result = actionsManager.executeActionWithRouting(
            classification.intent,
            category,
            "swipe up"
        )

        assertTrue(result is ActionResult.Failure)
        assertTrue(result.message.contains("VoiceOS"))
    }
}
```

### E2E Tests (Phase 2)

**VoiceOSIPCTest.kt:**
```kotlin
@RequiresDevice
@RequiresVoiceOSInstalled
class VoiceOSIPCTest {
    @Test
    fun `execute gesture command via IPC`() = runBlocking {
        // Ensure VoiceOS is running
        assumeTrue(voiceOSConnection.isVoiceOSInstalled())
        assumeTrue(voiceOSConnection.isAccessibilityServiceRunning())

        // Connect to VoiceOS
        voiceOSConnection.connect()

        // Execute command
        val result = voiceOSConnection.executeCommand("swipe_up", "gesture")

        // Verify
        assertTrue(result is CommandResult.Success)

        // Verify UI actually swiped (check screen state)
    }
}
```

### Performance Tests

**RoutingPerformanceTest.kt:**
```kotlin
class RoutingPerformanceTest {
    @Test
    fun `measure routing decision overhead`() {
        val iterations = 1000
        val startTime = System.nanoTime()

        repeat(iterations) {
            router.route("turn_on_wifi", "connectivity")
        }

        val endTime = System.nanoTime()
        val avgTime = (endTime - startTime) / iterations / 1_000_000.0 // ms

        // Assert routing overhead <5ms
        assertTrue(avgTime < 5.0, "Routing overhead: $avgTime ms")
    }

    @Test
    fun `measure end-to-end command execution time`() = runBlocking {
        val startTime = System.currentTimeMillis()

        val result = actionsManager.executeActionWithRouting(
            "turn_on_wifi",
            "connectivity",
            "turn on wifi"
        )

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Assert total execution <300ms
        assertTrue(totalTime < 300, "Execution time: $totalTime ms")
    }
}
```

---

## Troubleshooting

### Issue 1: Commands Not Routing Correctly

**Symptom:** WiFi command is being routed to VoiceOS instead of AVA

**Cause:** Category inference returns wrong category

**Solution:**
```kotlin
// Debug category inference
val category = actionsManager.getCategoryForIntent("turn_on_wifi")
println("Category: $category")  // Should be "connectivity"

// If wrong, check IntentRouter.getCategoryForIntent() logic
```

### Issue 2: VoiceOS Commands Always Fail

**Symptom:** All gesture/cursor commands return "VoiceOS unavailable"

**Cause:** VoiceOS not installed or accessibility service not enabled

**Solution:**
```kotlin
// Check VoiceOS status
val installed = voiceOSConnection.isVoiceOSInstalled()
val serviceRunning = voiceOSConnection.isAccessibilityServiceRunning()

println("VoiceOS installed: $installed")
println("Accessibility service: $serviceRunning")

// Guide user to:
// 1. Install VoiceOS app
// 2. Enable accessibility service in Settings
```

### Issue 3: Routing Decision Too Slow

**Symptom:** Noticeable lag before command execution

**Cause:** Routing overhead >5ms

**Solution:**
```kotlin
// Profile routing decision
val startTime = System.nanoTime()
val decision = router.route(intent, category)
val endTime = System.nanoTime()
println("Routing time: ${(endTime - startTime) / 1_000_000.0} ms")

// If >5ms, check:
// - isVoiceOSAvailable() implementation (should be cached)
// - Category inference complexity
```

### Issue 4: Category Unknown for New Commands

**Symptom:** New commands fallback to LLM instead of routing

**Cause:** Category not in inference logic

**Solution:**
```kotlin
// Phase 1: Add pattern to getCategoryForIntent()
fun getCategoryForIntent(intent: String): String {
    return when {
        // ... existing patterns ...
        intent.contains("new_keyword") -> "appropriate_category"
        else -> "unknown"
    }
}

// Phase 2: Add category to .ava file
{
  "id": "new_intent",
  "c": "new command",
  "cat": "appropriate_category",  // ← Add this
  "p": 1
}
```

---

## Best Practices

### 1. Always Use Routing for Command Execution

✅ **DO:**
```kotlin
val category = actionsManager.getCategoryForIntent(intent)
actionsManager.executeActionWithRouting(intent, category, utterance)
```

❌ **DON'T:**
```kotlin
// Bypasses routing, causes jurisdiction conflicts
actionsManager.executeAction(intent, utterance)
```

### 2. Handle All Routing Decisions

✅ **DO:**
```kotlin
when (val decision = intentRouter.route(intent, category)) {
    is ExecuteLocally -> { /* handle */ }
    is ForwardToVoiceOS -> { /* handle */ }
    is VoiceOSUnavailable -> { /* show helpful error */ }
    is FallbackToLLM -> { /* try local or show error */ }
}
```

❌ **DON'T:**
```kotlin
// Ignores VoiceOSUnavailable and FallbackToLLM cases
if (decision is ExecuteLocally) {
    executeAction()
}
```

### 3. Provide Helpful Error Messages

✅ **DO:**
```kotlin
ActionResult.Failure(
    "This command requires VoiceOS accessibility service. " +
    "Please enable it in Settings > Accessibility > VoiceOS."
)
```

❌ **DON'T:**
```kotlin
ActionResult.Failure("Command failed")
```

### 4. Add Categories to New .ava Files

✅ **DO:**
```json
{
  "id": "new_command",
  "c": "command text",
  "cat": "appropriate_category",
  "p": 1
}
```

❌ **DON'T:**
```json
{
  "id": "new_command",
  "c": "command text",
  "p": 1
}
// Missing category
```

### 5. Test Both Input Methods

✅ **DO:**
```kotlin
@Test
fun `test routing for typed command`() { /* ... */ }

@Test
fun `test routing for voice command`() { /* ... */ }
```

---

## Related Documentation

- **Chapter 36:** VoiceOS Command Delegation - Original delegation concept
- **Chapter 34:** Intent Management - Intent classification and handling
- **Chapter 37:** AVA File Format - .ava file structure with category field
- **Chapter 40:** NLU Learning System - Phase 2 automatic learning
- **Chapter 72:** SOLID Architecture - CategoryCapabilityRegistry (P2 SOLID fix)
- **INTENT-ROUTING-IMPLEMENTATION.md** - Implementation status and phase tracking
- **VOICEOS-COMMANDS-MIGRATION.md** - VoiceOS command migration details

---

## Conclusion

The Intent Routing Architecture provides a clean, scalable solution for routing voice and typed commands between AVA and VoiceOS based on capability. With category-based routing, graceful error handling, and negligible performance overhead, the system enables seamless integration while maintaining clear separation of concerns.

**Current Status:** Phase 1 + 1.5 + ADR-014 (Phases 3-5) Complete ✅
**Next Steps:** Implement VoiceOS IPC binding (Phase 2)
**Future:** Enhanced NLU integration with database-backed categories (Phase 4)

---

## ADR-014: Flow Gaps Fix (Phases 3-5)

### Overview

ADR-014 Phases 3-5 address integration gaps in the Intent Routing system, adding proper VoiceOS availability checks, accessibility permission handling, and init race condition fixes.

### Phase 3: VoiceOS/Accessibility Integration

**Problem:** `isVoiceOSAvailable()` in IntentRouter always returned `false`.

**Solution:** Wire VoiceOSConnection to IntentRouter for actual availability checks.

**Files Modified:**

| File | Change |
|------|--------|
| `IntentRouter.kt` | Now uses `voiceOSConnection?.isReady()` |
| `ActionsManager.kt` | Passes VoiceOSConnection to IntentRouter |

**Code:**
```kotlin
// IntentRouter.kt
private fun isVoiceOSAvailable(): Boolean {
    // ADR-014: Use VoiceOSConnection to check actual accessibility service state
    return voiceOSConnection?.isReady() ?: false
}

// ActionsManager.kt
private val voiceOSConnection by lazy { VoiceOSConnection(context) }
private val intentRouter by lazy { IntentRouter(context, voiceOSConnection) }
```

### Phase 4: Accessibility Action Fallback

**Problem:** VoiceOS commands failed silently when accessibility service not enabled.

**Solution:** Check accessibility permission before execution, show user-friendly prompt.

**Files Modified:**

| File | Change |
|------|--------|
| `ActionsManager.kt` | Added `isAccessibilityServiceEnabled()` check |
| `ChatViewModel.kt` | Added `showAccessibilityPrompt` StateFlow |

**ActionsManager Changes:**
```kotlin
is IntentRouter.RoutingDecision.ForwardToVoiceOS -> {
    // ADR-014 Phase 4: Check accessibility permission first
    if (!isAccessibilityServiceEnabled()) {
        return ActionResult.Success(
            message = "To use gesture and cursor commands, please enable AVA Accessibility Service in Settings.",
            data = mapOf(
                "needsAccessibility" to true,
                "intent" to intent,
                "category" to category
            )
        )
    }
    // Execute via VoiceOS connection
}

fun isAccessibilityServiceEnabled(): Boolean {
    return try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val packageName = context.packageName
        enabledServices.split(':').any { service ->
            service.startsWith(packageName) && service.contains("AccessibilityService")
        }
    } catch (e: Exception) {
        false
    }
}
```

**ChatViewModel Changes:**
```kotlin
private val _showAccessibilityPrompt = MutableStateFlow(false)
val showAccessibilityPrompt: StateFlow<Boolean> = _showAccessibilityPrompt.asStateFlow()

fun dismissAccessibilityPrompt() {
    _showAccessibilityPrompt.value = false
}

// In action result handling:
if (actionResult.data?.get("needsAccessibility") == true) {
    _showAccessibilityPrompt.value = true
}
```

### Phase 5: Fix Init Race Condition

**Problem:** ChatViewModel could attempt action execution before ActionsManager was initialized.

**Solution:** Add `isReady` StateFlow to ActionsManager, check before execution.

**Files Modified:**

| File | Change |
|------|--------|
| `ActionsManager.kt` | Added `isReady` StateFlow |
| `ChatViewModel.kt` | Check `actionsManager.isReady.value` before execution |

**ActionsManager Changes:**
```kotlin
private val _isReady = MutableStateFlow(false)
val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

fun initialize() {
    ActionsInitializer.initialize(context)
    val count = IntentActionHandlerRegistry.getRegisteredIntents().size
    _isReady.value = true
    Log.i(TAG, "ActionsManager ready with $count handlers")
}
```

**ChatViewModel Changes:**
```kotlin
// Check ready state before action execution
if (classifiedIntent != null &&
    confidenceScore != null &&
    confidenceScore > currentThreshold &&
    actionsManager.isReady.value &&  // ADR-014 Phase 5
    actionsManager.hasHandler(classifiedIntent)) {
    // Execute action
}
```

### ActionResult.NeedsResolution

**Related Update:** The `ActionResult` sealed class now includes a `NeedsResolution` case for Chapter 71 (Intelligent Resolution System):

```kotlin
sealed class ActionResult {
    data class Success(val message: String? = null, val data: Map<String, Any>? = null)
    data class Failure(val message: String, val exception: Throwable? = null)
    data class NeedsResolution(val capability: String, val data: Map<String, Any> = emptyMap())
}
```

ChatViewModel handles all three cases:
```kotlin
val actionFeedback = when (actionResult) {
    is ActionResult.Success -> {
        if (actionResult.data?.get("needsAccessibility") == true) {
            _showAccessibilityPrompt.value = true
        }
        actionResult.message ?: "Action completed successfully"
    }
    is ActionResult.Failure -> actionResult.message
    is ActionResult.NeedsResolution ->
        "I need to know which app you'd like to use for ${actionResult.capability}."
}
```

### Dependency Injection Updates

**AppModule.kt** now properly injects dependencies for ActionsManager:

```kotlin
@Provides
@Singleton
fun provideActionsManager(
    @ApplicationContext context: Context,
    appResolverService: AppResolverService,
    preferencePromptManager: PreferencePromptManager
): ActionsManager {
    return ActionsManager(context, appResolverService, preferencePromptManager)
}
```

### Testing ADR-014 Changes

```bash
# Verify VoiceOS availability check
adb logcat -s IntentRouter:V | grep "isVoiceOSAvailable"

# Verify accessibility check
adb logcat -s ActionsManager:V | grep "Accessibility"

# Verify isReady state
adb logcat -s ActionsManager:V | grep "ready"
```

---

**Last Updated:** 2025-12-06
**Author:** Manoj Jhawar
**Version:** 1.1

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-06 | Added Phase 2 database-driven category lookup with IntentCategoryRepository, CategorySeeder, and migration details |
| 1.0 | 2025-11-17 | Initial release with Phase 1 pattern matching |

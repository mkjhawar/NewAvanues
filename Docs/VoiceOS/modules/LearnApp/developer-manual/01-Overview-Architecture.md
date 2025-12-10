# Chapter 1: Overview & Architecture

**Module**: LearnApp
**Package**: `com.augmentalis.voiceoscore.learnapp`
**Last Updated**: 2025-12-08

---

## Overview

### Purpose

LearnApp is VoiceOS's intelligent third-party app learning system. It automatically explores third-party Android applications to enable voice control without requiring app developers to implement any integration code.

### Key Features

- **Automatic UI Discovery**: DFS-based exploration of app screens
- **Consent Management**: User approval flow with "don't ask again" option
- **Smart Element Detection**: Classifies UI elements and skips dangerous actions
- **Login Screen Handling**: Pauses exploration when login detected
- **Progress Tracking**: Real-time overlay showing exploration progress
- **UUID Integration**: Registers discovered elements with UUIDCreator
- **Navigation Graph**: Builds complete app navigation model
- **Persistent Storage**: SQLDelight database for learned app data

### Technology Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Database**: SQLDelight (KMP)
- **Concurrency**: Kotlin Coroutines + Flow
- **Architecture**: MVVM with Repository Pattern
- **Accessibility**: Android Accessibility Framework
- **DI**: Manual dependency injection (no framework)

---
## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceOSService                           │
│                 (AccessibilityService)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              LearnAppIntegration                            │
│          (Central Integration Point)                        │
└─┬───────┬────────┬─────────┬──────────┬────────────────────┘
  │       │        │         │          │
  ▼       ▼        ▼         ▼          ▼
┌────┐ ┌────┐  ┌────┐   ┌────┐    ┌─────────┐
│App │ │Con │  │Pro │   │Exp │    │Learned  │
│Lau │ │sent│  │gre │   │lor │    │AppTrack │
│nch │ │Mgr │  │Mgr │   │Eng │    │er       │
└────┘ └────┘  └────┘   └────┘    └─────────┘
```

### Component Diagram

```
┌──────────────────────────────────────────────────────────┐
│ Integration Layer                                        │
│  └─ LearnAppIntegration (singleton)                      │
└──────────────────────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Detection   │  │      UI      │  │ Exploration  │
│              │  │              │  │              │
│ • AppLaunch  │  │ • Consent    │  │ • Engine     │
│   Detector   │  │   Dialog     │  │ • Screen     │
│ • LearnedApp │  │   Manager    │  │   Explorer   │
│   Tracker    │  │ • Progress   │  │ • Strategy   │
│              │  │   Overlay    │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
        │                                  │
        │                                  │
        ▼                                  ▼
┌──────────────────────────────────────────────────────────┐
│ Data Layer                                               │
│  └─ LearnAppRepository                                   │
│      └─ LearnAppDao                                      │
│          └─ VoiceOSDatabaseManager (SQLDelight)          │
└──────────────────────────────────────────────────────────┘
```

### Data Flow

```
1. App Launch Detected
   AccessibilityEvent → AppLaunchDetector → emit(NewAppDetected)

2. Consent Flow
   NewAppDetected → ConsentDialogManager → show dialog
   User clicks "Yes" → emit(Approved) → start exploration

3. Exploration Flow
   ExplorationEngine.start() → DFS algorithm
   └─ For each screen:
       ├─ ScreenExplorer.exploreScreen()
       ├─ Register elements with UUIDCreator
       ├─ Add to NavigationGraph
       └─ Recurse into clickable elements

4. Completion
   ExplorationEngine → emit(Completed)
   → Save to database → Update LearnedAppTracker
```

---
## Core Components

### 1. LearnAppIntegration

**Path**: `integration/LearnAppIntegration.kt`

Central integration adapter that wires all LearnApp components together.

#### Functions

##### `initialize(context, accessibilityService): LearnAppIntegration`

Initializes the integration singleton.

**Parameters**:
- `context: Context` - Application context
- `accessibilityService: AccessibilityService` - VoiceOS accessibility service

**Returns**: LearnAppIntegration instance

**Thread Safety**: Thread-safe singleton with double-checked locking

**Example**:
```kotlin
// In VoiceOSService.onServiceConnected()
val integration = LearnAppIntegration.initialize(
    context = applicationContext,
    accessibilityService = this
)
```

##### `getInstance(): LearnAppIntegration`

Gets the singleton instance.

**Returns**: LearnAppIntegration instance
**Throws**: IllegalStateException if not initialized

##### `onAccessibilityEvent(event: AccessibilityEvent)`

Processes accessibility events. Call from `AccessibilityService.onAccessibilityEvent()`.

**Parameters**:
- `event: AccessibilityEvent` - Accessibility event

**Thread**: Any (internally dispatches to coroutine)

**Example**:
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    learnAppIntegration?.onAccessibilityEvent(event)
}
```

##### `pauseExploration()`

Pauses current exploration.

**State Transition**: `Running → PausedByUser`

##### `resumeExploration()`

Resumes paused exploration.

**State Transition**: `PausedByUser → Running`

##### `stopExploration()`

Stops exploration and saves results.

**State Transition**: `Running → Completed`

##### `getExplorationState(): StateFlow<ExplorationState>`

Gets exploration state flow for observing state changes.

**Returns**: StateFlow of ExplorationState

**Example**:
```kotlin
integration.getExplorationState().collect { state ->
    when (state) {
        is ExplorationState.Running -> updateUI(state.progress)
        is ExplorationState.Completed -> showResults(state.stats)
    }
}
```

##### `cleanup()`

Cleans up resources. Call in `onDestroy()`.

**Thread**: Any

---

### 2. ConsentDialogManager

**Path**: `ui/ConsentDialogManager.kt`

Manages consent dialog lifecycle and user responses.

#### Functions

##### `showConsentDialog(packageName: String, appName: String)`

Shows consent dialog overlay.

**Parameters**:
- `packageName: String` - Package name (e.g., "com.instagram.android")
- `appName: String` - Human-readable name (e.g., "Instagram")

**Preconditions**:
- SYSTEM_ALERT_WINDOW permission granted
- Called from coroutine (suspend function)

**Thread**: Switches to Main for UI operations

**Threading Fix (Recent)**: This function now properly uses `withContext(Dispatchers.Main)` when adding views to WindowManager, fixing crashes that occurred when called from background threads.

**Example**:
```kotlin
scope.launch {
    consentDialogManager.showConsentDialog(
        packageName = "com.instagram.android",
        appName = "Instagram"
    )
}
```

##### `hideConsentDialog()`

Hides consent dialog.

**Thread**: Switches to Main for UI operations

**Threading Fix (Recent)**: This function now properly uses `withContext(Dispatchers.Main)` when removing views from WindowManager.

##### `hasOverlayPermission(): Boolean`

Checks if SYSTEM_ALERT_WINDOW permission is granted.

**Returns**: true if permission granted

**API Level**: Requires permission on Android M+ (API 23+)

##### `isDialogShowing(): Boolean`

Checks if dialog is currently visible.

**Returns**: true if visible

##### `getCurrentPackage(): String?`

Gets package name of currently shown dialog.

**Returns**: Package name or null

##### `cleanup()`

Cleans up resources.

**Threading**: Launches coroutine to hide dialog

#### Properties

##### `consentResponses: SharedFlow<ConsentResponse>`

Flow of user consent responses.

**Type**: `SharedFlow<ConsentResponse>` (hot flow, no replay)

**Emissions**:
- `ConsentResponse.Approved` - User approved learning
- `ConsentResponse.Declined` - User declined learning

**Example**:
```kotlin
consentDialogManager.consentResponses.collect { response ->
    when (response) {
        is ConsentResponse.Approved -> {
            startExploration(response.packageName)
        }
        is ConsentResponse.Declined -> {
            // Do nothing
        }
    }
}
```

#### Internal Functions

##### `handleApproval(packageName: String, dontAskAgain: Boolean)`

**Private suspend function**

Handles user approval:
1. Hides dialog
2. Marks as learned if "don't ask again" checked
3. Emits Approved response

##### `handleDeclination(packageName: String, dontAskAgain: Boolean)`

**Private suspend function**

Handles user declination:
1. Hides dialog
2. Marks as dismissed if "don't ask again" checked
3. Emits Declined response

---

### 3. AppLaunchDetector

**Path**: `detection/AppLaunchDetector.kt`

Monitors accessibility events to detect app launches.

#### Functions

##### `onAccessibilityEvent(event: AccessibilityEvent)`

Processes accessibility event.

**Parameters**:
- `event: AccessibilityEvent` - Event to process

**Filters**:
- Only processes TYPE_WINDOW_STATE_CHANGED events
- Debounces rapid events (100ms window)
- Filters system apps
- Filters already learned apps
- Filters recently dismissed apps

**Example**:
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    appLaunchDetector.onAccessibilityEvent(event)
}
```

##### `isPackageInstalled(packageName: String): Boolean`

Checks if package is installed.

**Parameters**:
- `packageName: String` - Package to check

**Returns**: true if installed

##### `getPackageVersionCode(packageName: String): Long`

Gets package version code.

**Returns**: Version code or -1 if not found

**API Compatibility**: Handles API level differences (P vs older)

##### `getPackageVersionName(packageName: String): String?`

Gets package version name.

**Returns**: Version name (e.g., "1.2.3") or null

##### `resetDebounce()`

Resets debounce state (for testing).

#### Properties

##### `appLaunchEvents: SharedFlow<AppLaunchEvent>`

Flow of app launch events.

**Type**: `SharedFlow<AppLaunchEvent>` (hot flow)

**Events**:
- `AppLaunchEvent.NewAppDetected` - New unlearned app detected

---

### 4. LearnedAppTracker

**Path**: `detection/LearnedAppTracker.kt`

Tracks which apps have been learned or dismissed.

#### Functions

##### `isAppLearned(packageName: String): Boolean`

Checks if app has been learned.

**Performance**: O(1) in-memory lookup

**Thread Safety**: Thread-safe

**Returns**: true if learned

##### `markAsLearned(packageName: String, appName: String? = null)`

Marks app as learned.

**Parameters**:
- `packageName: String` - Package name
- `appName: String?` - Optional human-readable name

**Side Effects**:
- Adds to in-memory cache
- Saves to SharedPreferences
- Removes from dismissed list if present

**Thread**: Suspend function (uses mutex for thread safety)

##### `markAsDismissed(packageName: String)`

Marks app as dismissed by user.

**Effect**: Prevents showing consent dialog for 24 hours

**Thread**: Suspend function

##### `wasRecentlyDismissed(packageName: String): Boolean`

Checks if app was dismissed within last 24 hours.

**Returns**: true if dismissed recently

##### `clearDismissal(packageName: String)`

Clears dismissal (allows showing consent again).

**Thread**: Suspend function

##### `unmarkAsLearned(packageName: String)`

Removes app from learned list.

**Use Case**: Force re-learning after app update

**Thread**: Suspend function

##### `getAllLearnedApps(): Set<String>`

Gets all learned package names.

**Returns**: Set of package names

##### `getAllDismissedApps(): Set<String>`

Gets all dismissed package names (including expired).

**Returns**: Set of package names

##### `clearAllLearned()`

Clears all learned apps.

**Use Case**: Testing or factory reset

**Thread**: Suspend function

##### `clearAllDismissed()`

Clears all dismissed apps.

**Thread**: Suspend function

##### `getStats(): TrackerStats`

Gets tracker statistics.

**Returns**: TrackerStats with counts

---

### 5. ExplorationEngine

**Path**: `exploration/ExplorationEngine.kt`

Main DFS exploration engine.

#### Functions

##### `startExploration(packageName: String)`

Starts DFS exploration of app.

**Parameters**:
- `packageName: String` - Package to explore

**Algorithm**: Depth-First Search (DFS)

**State Updates**: Emits state changes to `explorationState` flow

**Error Handling**: Catches exceptions and emits Failed state

**Example**:
```kotlin
explorationEngine.startExploration("com.instagram.android")

// Observe progress
explorationEngine.explorationState.collect { state ->
    when (state) {
        is ExplorationState.Running -> {
            println("Explored ${state.progress.screensExplored} screens")
        }
        is ExplorationState.Completed -> {
            println("Done! Total: ${state.stats.totalScreens} screens")
        }
    }
}
```

##### `pauseExploration()`

Pauses exploration.

**State**: Sets state to PausedByUser

**Behavior**: DFS algorithm waits in `waitForResume()` loop

##### `resumeExploration()`

Resumes paused exploration.

**State**: Sets state back to Running

##### `stopExploration()`

Stops exploration immediately.

**State**: Sets state to Completed with current stats

#### Properties

##### `explorationState: StateFlow<ExplorationState>`

State flow of exploration state.

**Type**: `StateFlow<ExplorationState>` (hot flow with replay=1)

**States**:
- `Idle` - No exploration
- `Running` - Exploring
- `PausedForLogin` - Login screen detected
- `PausedByUser` - User paused
- `Completed` - Successfully completed
- `Failed` - Error occurred

#### Internal Functions

##### `exploreScreenRecursive(rootNode, packageName, depth)`

**Private suspend function**

Core DFS algorithm.

**Algorithm**:
1. Check depth limit
2. Check time limit
3. Explore current screen
4. Mark as visited
5. Register elements with UUIDCreator
6. Add to navigation graph
7. For each clickable element:
   - Click element
   - Wait for screen transition
   - Recurse into new screen
   - Backtrack (press back button)

**Parameters**:
- `rootNode: AccessibilityNodeInfo` - Root of screen hierarchy
- `packageName: String` - Package being explored
- `depth: Int` - Current depth in DFS tree

**Depth Limit**: Controlled by ExplorationStrategy (default: 10)

**Time Limit**: Controlled by ExplorationStrategy (default: 5 minutes)

##### `registerElements(elements, packageName): List<String>`

Registers elements with UUIDCreator.

**Returns**: List of generated UUIDs

**Side Effects**:
- Generates UUID for each element
- Registers with UUIDCreator
- Creates auto-alias
- Updates element.uuid property

##### `clickElement(node): Boolean`

Clicks accessibility node.

**Returns**: true if click successful

##### `pressBack()`

Presses global back button.

##### `waitForScreenChange(previousHash)`

Waits for screen to change (up to 60 seconds).

**Use Case**: Waiting for user to complete login

##### `waitForResume()`

Waits while paused by user.

**Behavior**: Polls state every 100ms

##### `updateProgress(packageName, depth, currentScreenHash)`

Updates progress and emits Running state.

##### `getCurrentProgress(packageName, depth): ExplorationProgress`

Creates ExplorationProgress snapshot.

##### `createExplorationStats(packageName): ExplorationStats`

Creates final ExplorationStats.

---

### 6. LearnAppRepository

**Path**: `database/repository/LearnAppRepository.kt`

Repository pattern abstraction over SQLDelight database.

#### Functions

##### Learned Apps

###### `saveLearnedApp(packageName, appName, versionCode, versionName, stats)`

Saves learned app to database.

**Parameters**:
- `packageName: String`
- `appName: String`
- `versionCode: Long`
- `versionName: String`
- `stats: ExplorationStats`

**Thread**: Suspend function

###### `getLearnedApp(packageName): LearnedAppEntity?`

Gets learned app entity.

**Returns**: Entity or null

###### `isAppLearned(packageName): Boolean`

Checks if app is in database.

###### `getAllLearnedApps(): List<LearnedAppEntity>`

Gets all learned apps.

###### `updateAppHash(packageName, newHash)`

Updates app hash (for version change detection).

###### `deleteLearnedApp(packageName)`

Deletes learned app from database.

##### Exploration Sessions

###### `createExplorationSession(packageName): String`

Creates new exploration session.

**Returns**: Session ID (UUID)

###### `completeExplorationSession(sessionId, stats)`

Marks session as completed.

###### `getExplorationSession(sessionId): ExplorationSessionEntity?`

Gets session by ID.

###### `getSessionsForPackage(packageName): List<ExplorationSessionEntity>`

Gets all sessions for package.

##### Navigation Graph

###### `saveNavigationGraph(graph, sessionId)`

Saves navigation graph to database.

**Saves**:
- Screen states
- Navigation edges

###### `getNavigationGraph(packageName): NavigationGraph?`

Reconstructs navigation graph from database.

###### `deleteNavigationGraph(packageName)`

Deletes navigation graph.

##### Screen States

###### `saveScreenState(screenState)`

Saves screen state.

###### `getScreenState(hash): ScreenStateEntity?`

Gets screen state by hash.

##### Statistics

###### `getAppStatistics(packageName): AppStatistics`

Gets statistics for app.

**Returns**: AppStatistics with counts

---

### 7. ProgressOverlayManager

**Path**: `ui/ProgressOverlayManager.kt`

Manages progress overlay lifecycle.

#### Functions

##### `showProgressOverlay(progress, onPause, onStop)`

Shows progress overlay.

**Parameters**:
- `progress: ExplorationProgress` - Initial progress
- `onPause: () -> Unit` - Callback for pause button
- `onStop: () -> Unit` - Callback for stop button

**Preconditions**: SYSTEM_ALERT_WINDOW permission

##### `updateProgress(progress: ExplorationProgress)`

Updates progress display.

**Thread**: Any (state is mutableStateOf)

##### `hideProgressOverlay()`

Hides overlay.

##### `isOverlayShowing(): Boolean`

Checks if overlay visible.

##### `cleanup()`

Cleans up resources.

---
## API Reference

### Data Models

#### ExplorationState

Sealed class representing exploration state machine.

```kotlin
sealed class ExplorationState {
    object Idle : ExplorationState()

    data class ConsentRequested(
        val packageName: String,
        val appName: String
    ) : ExplorationState()

    data class Running(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    data class PausedForLogin(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    data class PausedByUser(
        val packageName: String,
        val progress: ExplorationProgress
    ) : ExplorationState()

    data class Completed(
        val packageName: String,
        val stats: ExplorationStats
    ) : ExplorationState()

    data class Failed(
        val packageName: String,
        val error: Throwable,
        val partialProgress: ExplorationProgress?
    ) : ExplorationState()
}
```

#### ExplorationProgress

Real-time progress information.

```kotlin
data class ExplorationProgress(
    val appName: String,
    val screensExplored: Int,
    val estimatedTotalScreens: Int,
    val elementsDiscovered: Int,
    val currentDepth: Int,
    val currentScreen: String,
    val elapsedTimeMs: Long
)
```

#### ExplorationStats

Final exploration statistics.

```kotlin
data class ExplorationStats(
    val packageName: String,
    val appName: String,
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val durationMs: Long,
    val maxDepth: Int,
    val dangerousElementsSkipped: Int,
    val loginScreensDetected: Int,
    val scrollableContainersFound: Int
)
```

#### ConsentResponse

User consent response.

```kotlin
sealed class ConsentResponse {
    data class Approved(
        val packageName: String,
        val dontAskAgain: Boolean
    ) : ConsentResponse()

    data class Declined(
        val packageName: String,
        val reason: String
    ) : ConsentResponse()
}
```

#### AppLaunchEvent

App launch event.

```kotlin
sealed class AppLaunchEvent {
    data class NewAppDetected(
        val packageName: String,
        val appName: String
    ) : AppLaunchEvent()
}
```

---

## Integration Guide

### VoiceOSService Integration

```kotlin
class VoiceOSService : AccessibilityService() {

    private var learnAppIntegration: LearnAppIntegration? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize LearnApp
        learnAppIntegration = LearnAppIntegration.initialize(
            context = applicationContext,
            accessibilityService = this
        )

        // Observe exploration state (optional)
        lifecycleScope.launch {
            learnAppIntegration?.getExplorationState()?.collect { state ->
                handleExplorationState(state)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward to LearnApp
        learnAppIntegration?.onAccessibilityEvent(event)

        // Your other event handling...
    }

    override fun onDestroy() {
        learnAppIntegration?.cleanup()
        learnAppIntegration = null
        super.onDestroy()
    }

    private fun handleExplorationState(state: ExplorationState) {
        when (state) {
            is ExplorationState.Running -> {
                // Update notification, log progress, etc.
            }
            is ExplorationState.Completed -> {
                // Show completion notification
            }
            is ExplorationState.Failed -> {
                // Show error notification
            }
            else -> { }
        }
    }
}
```

---
## Event System

### Event Flows

LearnApp uses Kotlin SharedFlow for event propagation:

1. **App Launch Events** (`AppLaunchDetector.appLaunchEvents`)
   - Type: `SharedFlow<AppLaunchEvent>`
   - Replay: 0
   - Emits: NewAppDetected

2. **Consent Responses** (`ConsentDialogManager.consentResponses`)
   - Type: `SharedFlow<ConsentResponse>`
   - Replay: 0
   - Emits: Approved, Declined

3. **Exploration State** (`ExplorationEngine.explorationState`)
   - Type: `StateFlow<ExplorationState>`
   - Replay: 1 (latest state)
   - Emits: All ExplorationState variants

### Event Lifecycle

```
1. App Launch
   TYPE_WINDOW_STATE_CHANGED event
   → AppLaunchDetector.onAccessibilityEvent()
   → emit(NewAppDetected)

2. Consent Dialog
   NewAppDetected
   → ConsentDialogManager.showConsentDialog()
   → User clicks Yes/No
   → emit(ConsentResponse)

3. Exploration
   ConsentResponse.Approved
   → ExplorationEngine.startExploration()
   → emit(ExplorationState.Running)
   → ... exploration loop ...
   → emit(ExplorationState.Completed)
```

---
## Threading Model

### Coroutine Scopes

All components use proper coroutine scopes:

```kotlin
// Each component has its own scope
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

### Threading Rules

1. **UI Operations** (WindowManager, ComposeView)
   - MUST run on Main thread
   - Use `withContext(Dispatchers.Main)` for UI operations

2. **Database Operations** (SQLDelight)
   - Must run on IO thread
   - Use suspend functions with withContext(Dispatchers.IO)

3. **Accessibility Events**
   - Received on Main thread
   - Process in background coroutine

4. **Exploration Loop**
   - Runs on Default dispatcher
   - UI updates via `withContext(Dispatchers.Main)`

### Recent Threading Fixes

**ConsentDialogManager** (2025-10-23):
- Fixed crash when `showConsentDialog()` called from background thread
- Now uses `withContext(Dispatchers.Main)` for WindowManager operations
- Same fix applied to `hideConsentDialog()`

**Before**:
```kotlin
// Would crash if called from background thread
windowManager.addView(composeView, params)
```

**After**:
```kotlin
// Thread-safe
withContext(Dispatchers.Main) {
    windowManager.addView(composeView, params)
    currentDialogView = composeView
    isDialogVisible.value = true
}
```

---

---

**Navigation**: [Index](./00-Index.md) | [Next: Exploration Engine →](./02-Exploration-Engine.md)

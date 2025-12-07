# LearnApp: Automated UI Exploration System
## Comprehensive Developer Guide

**Version**: 1.0.0
**Date**: 2025-10-08
**Status**: Architectural Design
**Module**: LearnApp (Proposed)
**Platform**: Android API 26+

---

## Table of Contents

1. [Overview](#1-overview)
2. [User Experience Flow](#2-user-experience-flow)
3. [Architecture Overview](#3-architecture-overview)
4. [API Reference](#4-api-reference)
5. [Integration Guide](#5-integration-guide)
6. [Database Schema](#6-database-schema)
7. [Exploration Algorithm](#7-exploration-algorithm)
8. [Safety Systems](#8-safety-systems)
9. [Performance Considerations](#9-performance-considerations)
10. [Examples](#10-examples)
11. [Troubleshooting](#11-troubleshooting)
12. [Testing Guide](#12-testing-guide)

---

# 1. Overview

## What is LearnApp?

**LearnApp** is an intelligent, automated UI exploration system for VOS4 that systematically discovers and maps third-party Android applications by exploring all screens and clickable elements using Android's Accessibility Service.

### Core Purpose

Enable **automated UI discovery** for:
- Third-party apps without SDK integration
- Complex multi-screen application flows
- Dynamic UI elements and navigation patterns
- Voice command preparation and training data generation

### Key Features

| Feature | Description | Benefit |
|---------|-------------|---------|
| **DFS Exploration** | Depth-first search through UI tree | Complete screen coverage |
| **State Management** | Track visited screens and elements | Avoid redundant exploration |
| **Smart Backtracking** | Return to previous screens intelligently | Navigate complex flows |
| **Safety Detection** | Identify dangerous actions | Prevent data loss/corruption |
| **UUID Generation** | Automatic element identification | Voice control enablement |
| **Progress Tracking** | Real-time exploration progress | User feedback and monitoring |
| **Session Persistence** | Resume interrupted explorations | Handle large apps efficiently |
| **Element Classification** | Categorize UI components | Optimize interaction strategy |

### Use Cases

#### Use Case 1: New App Onboarding
```
User installs Instagram
→ Enables voice control
→ LearnApp explores Instagram automatically
→ Discovers all screens, buttons, inputs
→ Generates UUIDs for voice commands
→ User can now say "open Instagram, click explore button"
```

#### Use Case 2: App Update Adaptation
```
Instagram updates to new version
→ UI structure changes
→ LearnApp detects version change
→ Re-explores changed screens
→ Updates UUID mappings
→ Voice commands continue working
```

#### Use Case 3: Accessibility Enhancement
```
Visually impaired user installs new app
→ LearnApp explores app structure
→ Discovers all interactive elements
→ Creates voice command vocabulary
→ Enables complete voice-based navigation
```

### Why LearnApp?

**Problem**: Third-party apps don't integrate with VOS4 SDK, making voice control limited or impossible.

**Solution**: LearnApp automatically explores these apps via accessibility, mapping their entire UI structure so VOS4 can enable voice control for any app, without developer cooperation.

---

# 2. User Experience Flow

## High-Level User Journey

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER JOURNEY                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. INITIATION                                                      │
│     User: "Learn Instagram"                                         │
│     ↓                                                               │
│  2. CONFIRMATION                                                    │
│     LearnApp: "I'll explore Instagram. This will take 2-5 minutes." │
│     LearnApp: "I won't make purchases or delete data."              │
│     ↓                                                               │
│  3. EXPLORATION (Automated)                                         │
│     [Progress Bar: 23% - Exploring Profile Screen]                 │
│     ↓                                                               │
│  4. COMPLETION                                                      │
│     LearnApp: "Done! Found 47 screens and 324 interactive elements."│
│     LearnApp: "You can now use voice commands for Instagram."       │
│     ↓                                                               │
│  5. USAGE                                                           │
│     User: "Open Instagram, click explore"                           │
│     VOS4: [Executes command using learned UUIDs]                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Detailed Step-by-Step Flow

### Step 1: Initiation

**Trigger Options:**
1. Voice command: "Learn [app name]"
2. VOS4 HUD: Tap "Learn New App" button
3. Automatic: First launch of unlearned app

**UI Display:**
```
┌──────────────────────────────────────────┐
│  LearnApp                         [X]    │
├──────────────────────────────────────────┤
│                                          │
│  Ready to learn: Instagram               │
│                                          │
│  This will:                              │
│  ✓ Explore all screens                   │
│  ✓ Find interactive elements             │
│  ✓ Create voice commands                 │
│                                          │
│  Safety:                                 │
│  • Won't make purchases                  │
│  • Won't delete data                     │
│  • Won't send messages                   │
│                                          │
│  Estimated time: 2-5 minutes             │
│                                          │
│  [Start]            [Cancel]             │
│                                          │
└──────────────────────────────────────────┘
```

### Step 2: Active Exploration

**UI Display (Real-time):**
```
┌──────────────────────────────────────────┐
│  Learning Instagram...           [||]    │
├──────────────────────────────────────────┤
│                                          │
│  Progress: 23%                           │
│  ████████░░░░░░░░░░░░░░░░░░░░░░░░        │
│                                          │
│  Current: Profile Screen                 │
│  Elements found: 87                      │
│  Screens visited: 12 / ~50               │
│                                          │
│  Latest discoveries:                     │
│  • Edit Profile Button                   │
│  • Settings Menu                         │
│  • Share Button                          │
│                                          │
│  [Pause]            [Stop]               │
│                                          │
└──────────────────────────────────────────┘
```

**What's Happening (Invisible to User):**
1. Launch target app
2. Get root AccessibilityNodeInfo
3. Start DFS traversal
4. For each element:
   - Check if dangerous (skip if yes)
   - Generate UUID
   - Click element
   - Wait for screen change
   - Explore new screen (recursive)
   - Backtrack when dead-end reached

### Step 3: Completion Summary

**UI Display:**
```
┌──────────────────────────────────────────┐
│  Instagram Learned! ✓              [X]   │
├──────────────────────────────────────────┤
│                                          │
│  Exploration Complete                    │
│  Duration: 3m 42s                        │
│                                          │
│  Discovered:                             │
│  • 47 unique screens                     │
│  • 324 interactive elements              │
│  • 89 buttons                            │
│  • 34 text inputs                        │
│  • 18 menus                              │
│  • 183 other elements                    │
│                                          │
│  Top Commands Available:                 │
│  • "Click explore"                       │
│  • "Click profile"                       │
│  • "Click new post"                      │
│  • "Click messages"                      │
│                                          │
│  [Try Voice Commands]  [View Map]        │
│                                          │
└──────────────────────────────────────────┘
```

### Step 4: Visual App Map (Optional)

**Interactive UI Tree:**
```
┌──────────────────────────────────────────────────────────────┐
│  Instagram App Map                              [X]          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  🏠 Home Feed                                                │
│  ├─ 🔍 Search (33 elements)                                  │
│  ├─ ➕ New Post (21 elements)                                │
│  │  ├─ 📷 Camera                                             │
│  │  ├─ 🖼️ Gallery                                            │
│  │  └─ 📝 Caption Editor                                     │
│  ├─ ❤️ Activity (12 elements)                                │
│  ├─ 👤 Profile (45 elements)                                 │
│  │  ├─ ⚙️ Settings (67 elements)                             │
│  │  │  ├─ 🔐 Privacy                                         │
│  │  │  ├─ 🔔 Notifications                                   │
│  │  │  └─ ℹ️ About                                           │
│  │  ├─ ✏️ Edit Profile                                       │
│  │  └─ 📊 Insights                                           │
│  └─ 💬 Messages (89 elements)                                │
│                                                              │
│  [Export] [Share] [Close]                                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## User Controls

### Pause/Resume
```kotlin
// User taps "Pause" during exploration
learnApp.pause()
// Later: "Resume"
learnApp.resume()
```

### Stop Early
```kotlin
// User taps "Stop" during exploration
learnApp.stop()
// Saves partial results
```

### View Progress
```kotlin
// Real-time progress updates
val progress: LearnProgress = learnApp.getCurrentProgress()
// Shows:
// - Elements explored: 87
// - Screens visited: 12
// - Current screen: "Profile"
// - Percentage: 23%
```

---

# 3. Architecture Overview

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              LearnApp System                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
        ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
        │  LearnAppService │ │  UI Layer    │ │ Integration  │
        │  (Coordinator)   │ │  (HUD)       │ │ (VOS4)       │
        └────────┬─────────┘ └──────────────┘ └──────┬───────┘
                 │                                    │
                 │                                    │
    ┌────────────┼────────────────────────────────────┤
    │            │                                    │
    ▼            ▼                                    ▼
┌─────────┐  ┌─────────┐  ┌──────────┐  ┌─────────────────────┐
│Explorer │  │ State   │  │ Safety   │  │ UUIDCreator         │
│ Engine  │  │Manager  │  │ Guard    │  │ (Third-Party)       │
└────┬────┘  └────┬────┘  └────┬─────┘  └──────────┬──────────┘
     │            │            │                     │
     └────────────┴────────────┴─────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  LearnAppDatabase│
                    │  (Room)          │
                    └──────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
            ┌──────────────┐    ┌──────────────┐
            │ ExploredScreen│   │ExploredElement│
            │ (Table)       │    │ (Table)       │
            └───────────────┘    └───────────────┘
```

## Component Responsibilities

### 1. LearnAppService (Coordinator)

**Role**: Main orchestrator of the learning process

**Responsibilities**:
- Launches target app
- Coordinates ExplorerEngine, StateManager, SafetyGuard
- Manages exploration lifecycle
- Reports progress to UI layer
- Handles errors and recovery

**Key Methods**:
```kotlin
class LearnAppService {
    suspend fun learnApp(packageName: String): LearnResult
    fun pause()
    fun resume()
    fun stop()
    fun getCurrentProgress(): LearnProgress
}
```

### 2. ExplorerEngine (Core Algorithm)

**Role**: Executes DFS exploration of UI tree

**Responsibilities**:
- Depth-first traversal of accessibility tree
- Element interaction (clicks, scrolls)
- Screen change detection
- Backtracking when dead-ends reached
- UUID generation for elements

**Key Methods**:
```kotlin
class ExplorerEngine {
    suspend fun exploreFromRoot(rootNode: AccessibilityNodeInfo)
    suspend fun exploreNode(node: AccessibilityNodeInfo): ExploreResult
    suspend fun backtrack()
    fun detectScreenChange(): Boolean
}
```

### 3. StateManager (State Tracking)

**Role**: Track exploration state to avoid cycles

**Responsibilities**:
- Maintain visited screens set
- Maintain visited elements set
- Track navigation stack (for backtracking)
- Detect when exploration is complete
- Persist state for resume capability

**Key Methods**:
```kotlin
class StateManager {
    fun markScreenVisited(screenId: String)
    fun markElementVisited(elementId: String)
    fun isScreenVisited(screenId: String): Boolean
    fun pushNavigationState(state: NavigationState)
    fun popNavigationState(): NavigationState?
}
```

### 4. SafetyGuard (Danger Detection)

**Role**: Prevent dangerous actions during exploration

**Responsibilities**:
- Detect purchase buttons
- Detect delete actions
- Detect logout buttons
- Detect message send buttons
- Allow/block element interactions

**Key Methods**:
```kotlin
class SafetyGuard {
    fun isDangerous(node: AccessibilityNodeInfo): Boolean
    fun getDangerLevel(node: AccessibilityNodeInfo): DangerLevel
    fun shouldSkipElement(node: AccessibilityNodeInfo): Boolean
}
```

### 5. Database Layer

**Role**: Persist exploration results

**Responsibilities**:
- Store explored screens
- Store discovered elements
- Store UUID mappings
- Enable session resume
- Query learned data

**Entities**:
```kotlin
@Entity(tableName = "explored_screens")
data class ExploredScreen(
    @PrimaryKey val screenId: String,
    val packageName: String,
    val screenName: String,
    val depth: Int,
    val timestamp: Long
)

@Entity(tableName = "explored_elements")
data class ExploredElement(
    @PrimaryKey val elementId: String,
    val uuid: String,
    val screenId: String,
    val elementType: String,
    val text: String?,
    val contentDescription: String?,
    val isDangerous: Boolean
)
```

## Thread Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         Thread Architecture                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Main Thread                                                    │
│  ├─ UI Updates (HUD progress)                                   │
│  └─ User Input (pause/resume/stop)                              │
│                                                                 │
│  Background Thread (Coroutine - IO Dispatcher)                  │
│  ├─ App launching                                               │
│  ├─ Database operations                                         │
│  └─ UUID generation                                             │
│                                                                 │
│  Accessibility Thread (System)                                  │
│  ├─ AccessibilityNodeInfo queries                               │
│  ├─ Click actions (performAction)                               │
│  └─ Screen change events                                        │
│                                                                 │
│  Exploration Thread (Coroutine - Default Dispatcher)            │
│  ├─ DFS algorithm execution                                     │
│  ├─ State management                                            │
│  └─ Safety checks                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Synchronization Strategy**:
- Use Kotlin Coroutines with structured concurrency
- Shared mutable state protected by `Mutex`
- Flow-based UI updates (StateFlow)
- Channel-based event communication

---

# 4. API Reference

## Core API

### LearnAppService

Main entry point for app learning operations.

#### initialize

```kotlin
/**
 * Initialize LearnApp service.
 *
 * MUST be called once during application startup, typically in
 * Application.onCreate().
 *
 * @param context Application context
 * @throws IllegalStateException if already initialized
 */
fun initialize(context: Context)
```

**Example**:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LearnAppService.initialize(this)
    }
}
```

#### learnApp

```kotlin
/**
 * Learn a third-party app by exploring all screens and elements.
 *
 * This is a suspending function that runs asynchronously. It will:
 * 1. Launch the target app
 * 2. Explore all reachable screens using DFS
 * 3. Generate UUIDs for all interactive elements
 * 4. Store results in database
 *
 * The exploration can be paused/resumed/stopped via other methods.
 *
 * @param packageName Target app package name (e.g., "com.instagram.android")
 * @param config Optional configuration for exploration behavior
 * @return LearnResult containing exploration statistics and status
 * @throws PackageNotFoundException if app not installed
 * @throws AccessibilityException if accessibility service not enabled
 * @throws ExplorationException if exploration fails
 */
suspend fun learnApp(
    packageName: String,
    config: LearnConfig = LearnConfig.DEFAULT
): LearnResult
```

**Parameters**:
- `packageName`: Android package name of target app
- `config`: Configuration object (optional)

**Returns**: `LearnResult` object containing:
```kotlin
data class LearnResult(
    val status: LearnStatus,           // SUCCESS, PARTIAL, FAILED
    val screensExplored: Int,          // Number of unique screens found
    val elementsFound: Int,            // Total interactive elements
    val durationMs: Long,              // Time taken in milliseconds
    val errors: List<ExplorationError>,// Any errors encountered
    val metadata: LearnMetadata        // Additional data
)
```

**Example - Basic Usage**:
```kotlin
class MainActivity : ComponentActivity() {

    private val learnAppService = LearnAppService.getInstance()

    fun startLearning() {
        lifecycleScope.launch {
            try {
                val result = learnAppService.learnApp("com.instagram.android")

                when (result.status) {
                    LearnStatus.SUCCESS -> {
                        Log.d(TAG, "✓ Learned ${result.screensExplored} screens")
                        Log.d(TAG, "✓ Found ${result.elementsFound} elements")
                    }
                    LearnStatus.PARTIAL -> {
                        Log.w(TAG, "⚠ Partial exploration: ${result.errors}")
                    }
                    LearnStatus.FAILED -> {
                        Log.e(TAG, "✗ Failed: ${result.errors}")
                    }
                }

            } catch (e: PackageNotFoundException) {
                Log.e(TAG, "App not installed")
            } catch (e: AccessibilityException) {
                Log.e(TAG, "Enable accessibility service first")
            }
        }
    }
}
```

**Example - Advanced Configuration**:
```kotlin
val config = LearnConfig(
    maxDepth = 10,                    // Max screen depth to explore
    maxElements = 1000,               // Stop after 1000 elements
    timeoutMs = 300_000,              // 5 minute timeout
    enableDangerDetection = true,     // Skip dangerous elements
    resumeFromLastSession = false     // Start fresh (ignore previous data)
)

val result = learnAppService.learnApp(
    packageName = "com.instagram.android",
    config = config
)
```

#### pause

```kotlin
/**
 * Pause the current exploration.
 *
 * Saves current state to database for resumption later.
 * The app being explored will be left in its current state.
 *
 * Safe to call multiple times (no-op if already paused).
 *
 * @throws IllegalStateException if no exploration in progress
 */
fun pause()
```

**Example**:
```kotlin
// User taps "Pause" button
pauseButton.setOnClickListener {
    learnAppService.pause()
    Toast.makeText(this, "Exploration paused", Toast.LENGTH_SHORT).show()
}
```

#### resume

```kotlin
/**
 * Resume a paused exploration.
 *
 * Restores state from database and continues from where it left off.
 *
 * Safe to call multiple times (no-op if already running).
 *
 * @throws IllegalStateException if no paused exploration exists
 */
fun resume()
```

**Example**:
```kotlin
// User taps "Resume" button
resumeButton.setOnClickListener {
    learnAppService.resume()
    Toast.makeText(this, "Exploration resumed", Toast.LENGTH_SHORT).show()
}
```

#### stop

```kotlin
/**
 * Stop the current exploration.
 *
 * Saves partial results to database. The exploration cannot be resumed
 * after stopping (unlike pause).
 *
 * Safe to call multiple times (no-op if not running).
 */
fun stop()
```

**Example**:
```kotlin
// User taps "Stop" button
stopButton.setOnClickListener {
    learnAppService.stop()
    Toast.makeText(this, "Exploration stopped", Toast.LENGTH_SHORT).show()
}
```

#### getCurrentProgress

```kotlin
/**
 * Get real-time progress of current exploration.
 *
 * Returns current state including screens explored, elements found,
 * and estimated completion percentage.
 *
 * @return LearnProgress snapshot, or null if no exploration active
 */
fun getCurrentProgress(): LearnProgress?
```

**Returns**: `LearnProgress` object:
```kotlin
data class LearnProgress(
    val currentScreen: String,        // Current screen name
    val screensExplored: Int,         // Screens visited so far
    val elementsFound: Int,           // Elements discovered so far
    val percentageComplete: Float,    // Estimated % (0.0 to 1.0)
    val elapsedTimeMs: Long,          // Time elapsed
    val estimatedRemainingMs: Long    // Estimated time left
)
```

**Example - Real-time Progress Display**:
```kotlin
class LearnProgressFragment : Fragment() {

    private val learnAppService = LearnAppService.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update progress every 500ms
        lifecycleScope.launch {
            while (isActive) {
                val progress = learnAppService.getCurrentProgress()

                if (progress != null) {
                    // Update UI
                    progressBar.progress = (progress.percentageComplete * 100).toInt()
                    screenText.text = "Current: ${progress.currentScreen}"
                    elementsText.text = "Elements: ${progress.elementsFound}"

                    val remaining = progress.estimatedRemainingMs / 1000
                    timeText.text = "Est. remaining: ${remaining}s"
                }

                delay(500)
            }
        }
    }
}
```

#### observeProgress

```kotlin
/**
 * Observe exploration progress as a Flow.
 *
 * Emits LearnProgress updates in real-time as exploration proceeds.
 * Flow completes when exploration finishes.
 *
 * @return Flow of LearnProgress updates
 */
fun observeProgress(): Flow<LearnProgress>
```

**Example - Compose UI**:
```kotlin
@Composable
fun LearnProgressScreen() {
    val learnAppService = remember { LearnAppService.getInstance() }
    val progress by learnAppService.observeProgress()
        .collectAsState(initial = null)

    Column(modifier = Modifier.padding(16.dp)) {
        progress?.let {
            Text("Current: ${it.currentScreen}")
            LinearProgressIndicator(progress = it.percentageComplete)
            Text("${it.elementsFound} elements found")
        }
    }
}
```

### LearnConfig

Configuration object for customizing exploration behavior.

```kotlin
/**
 * Configuration for app learning behavior.
 *
 * Use LearnConfig.DEFAULT for standard exploration, or customize
 * individual properties for specific needs.
 */
data class LearnConfig(
    /**
     * Maximum depth of screen navigation to explore.
     *
     * Depth is measured from the app's home screen. For example:
     * - Home screen = depth 0
     * - Screen reached from home = depth 1
     * - Screen reached from depth-1 screen = depth 2
     *
     * Higher values = more thorough but slower.
     *
     * Default: 15
     * Range: 1-50
     */
    val maxDepth: Int = 15,

    /**
     * Maximum number of elements to explore before stopping.
     *
     * Useful for very large apps to prevent extremely long explorations.
     * Set to Int.MAX_VALUE for unlimited.
     *
     * Default: 10,000
     */
    val maxElements: Int = 10_000,

    /**
     * Maximum time (in milliseconds) for entire exploration.
     *
     * Exploration stops when timeout reached, saving partial results.
     * Set to Long.MAX_VALUE for unlimited.
     *
     * Default: 600,000 (10 minutes)
     */
    val timeoutMs: Long = 600_000,

    /**
     * Enable danger detection to skip risky elements.
     *
     * When true, SafetyGuard prevents clicking:
     * - Purchase buttons
     * - Delete buttons
     * - Logout buttons
     * - Send message buttons
     * - Etc.
     *
     * Default: true (recommended)
     */
    val enableDangerDetection: Boolean = true,

    /**
     * Resume from last session if available.
     *
     * When true and a previous incomplete exploration exists,
     * continues from where it left off instead of starting fresh.
     *
     * Default: false
     */
    val resumeFromLastSession: Boolean = false,

    /**
     * Wait time (in milliseconds) after each click for UI to settle.
     *
     * Increase for apps with slow animations or network requests.
     *
     * Default: 1000 (1 second)
     */
    val clickDelayMs: Long = 1000,

    /**
     * Include system UI elements in exploration.
     *
     * When true, explores system dialogs, notifications, etc.
     * Usually not needed for app learning.
     *
     * Default: false
     */
    val includeSystemUI: Boolean = false,

    /**
     * Generate verbose logs for debugging.
     *
     * Default: false
     */
    val verboseLogging: Boolean = false
) {
    companion object {
        /**
         * Default configuration suitable for most apps.
         */
        val DEFAULT = LearnConfig()

        /**
         * Quick exploration for testing (shallow depth, short timeout).
         */
        val QUICK = LearnConfig(
            maxDepth = 5,
            maxElements = 100,
            timeoutMs = 60_000  // 1 minute
        )

        /**
         * Deep exploration for complex apps (high depth, long timeout).
         */
        val DEEP = LearnConfig(
            maxDepth = 25,
            maxElements = 50_000,
            timeoutMs = 1_800_000  // 30 minutes
        )

        /**
         * Safe mode with all danger detection enabled.
         */
        val SAFE = LearnConfig(
            enableDangerDetection = true,
            includeSystemUI = false
        )
    }
}
```

**Example - Using Presets**:
```kotlin
// Quick exploration (1 minute)
val result = learnAppService.learnApp(
    packageName = "com.example.app",
    config = LearnConfig.QUICK
)

// Deep exploration (30 minutes)
val result = learnAppService.learnApp(
    packageName = "com.complex.app",
    config = LearnConfig.DEEP
)
```

**Example - Custom Configuration**:
```kotlin
// Exploration for app with slow animations
val config = LearnConfig(
    maxDepth = 12,
    clickDelayMs = 2000,  // Wait 2 seconds after each click
    timeoutMs = 900_000,  // 15 minutes
    verboseLogging = true // Enable debug logs
)

val result = learnAppService.learnApp("com.slow.app", config)
```

---

## Query API

### LearnAppDatabase

Query interface for learned app data.

#### findLearnedApp

```kotlin
/**
 * Find learned app by package name.
 *
 * @param packageName App package name
 * @return LearnedApp object, or null if not learned
 */
suspend fun findLearnedApp(packageName: String): LearnedApp?
```

**Example**:
```kotlin
val learnedApp = LearnAppDatabase.getInstance(context)
    .findLearnedApp("com.instagram.android")

if (learnedApp != null) {
    Log.d(TAG, "Instagram learned on ${learnedApp.learnedAt}")
    Log.d(TAG, "Found ${learnedApp.screensExplored} screens")
} else {
    Log.d(TAG, "Instagram not learned yet")
}
```

#### getScreens

```kotlin
/**
 * Get all explored screens for an app.
 *
 * @param packageName App package name
 * @return List of ExploredScreen objects
 */
suspend fun getScreens(packageName: String): List<ExploredScreen>
```

**Example**:
```kotlin
val screens = LearnAppDatabase.getInstance(context)
    .getScreens("com.instagram.android")

screens.forEach { screen ->
    Log.d(TAG, "Screen: ${screen.screenName} (depth ${screen.depth})")
}
```

#### getElements

```kotlin
/**
 * Get all elements for a specific screen.
 *
 * @param screenId Screen identifier
 * @return List of ExploredElement objects
 */
suspend fun getElements(screenId: String): List<ExploredElement>
```

**Example**:
```kotlin
val elements = LearnAppDatabase.getInstance(context)
    .getElements("com.instagram.android.screen.profile")

elements.forEach { element ->
    Log.d(TAG, "Element: ${element.text} (${element.elementType})")
}
```

#### searchElements

```kotlin
/**
 * Search for elements by text or description.
 *
 * Performs case-insensitive substring matching.
 *
 * @param packageName App package name
 * @param query Search query
 * @return List of matching ExploredElement objects
 */
suspend fun searchElements(
    packageName: String,
    query: String
): List<ExploredElement>
```

**Example**:
```kotlin
// Find all "submit" buttons in Instagram
val submitButtons = LearnAppDatabase.getInstance(context)
    .searchElements("com.instagram.android", "submit")

submitButtons.forEach { button ->
    Log.d(TAG, "Found: ${button.text} on ${button.screenId}")
}
```

---

# 5. Integration Guide

## Integration with VOS4

LearnApp integrates with multiple VOS4 systems:

### 1. VoiceAccessibility Integration

LearnApp uses VoiceAccessibility's AccessibilityService for UI exploration.

**Setup**:

```kotlin
// VoiceAccessibilityService.kt
class VoiceAccessibilityService : AccessibilityService() {

    private lateinit var learnAppService: LearnAppService

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize LearnApp with this service
        learnAppService = LearnAppService.getInstance()
        learnAppService.setAccessibilityService(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward events to LearnApp if exploration active
        if (learnAppService.isExploring()) {
            learnAppService.onAccessibilityEvent(event)
        }

        // ... handle other events
    }
}
```

### 2. UUIDCreator Integration

LearnApp generates UUIDs for discovered elements using ThirdPartyUuidGenerator.

**Automatic Integration**:

```kotlin
// LearnAppService.kt (internal)
class LearnAppService(private val context: Context) {

    private val uuidCreator = UUIDCreator.getInstance()
    private val thirdPartyGenerator = ThirdPartyUuidGenerator(context)

    private suspend fun registerElement(
        node: AccessibilityNodeInfo,
        packageName: String
    ) {
        // Generate UUID for element
        val uuid = thirdPartyGenerator.generateUuid(node, packageName)

        // Create UUIDElement
        val element = UUIDElement(
            uuid = uuid,
            name = node.text?.toString() ?: node.contentDescription?.toString(),
            type = determineElementType(node),
            description = node.contentDescription?.toString(),
            position = UUIDPosition.fromAccessibilityNode(node),
            metadata = createMetadata(node)
        )

        // Register with UUIDCreator
        uuidCreator.register(element)

        // Store in LearnApp database
        database.insertElement(ExploredElement.fromUUIDElement(element))
    }
}
```

**Manual Integration (Advanced)**:

```kotlin
// Custom UUID generation during learning
class CustomLearnAppService {

    fun configureUuidGeneration(
        generator: (AccessibilityNodeInfo) -> String
    ) {
        this.customUuidGenerator = generator
    }
}

// Usage
customLearnAppService.configureUuidGeneration { node ->
    // Custom UUID format
    "myapp.${node.className}.${node.viewIdResourceName}"
}
```

### 3. HUDManager Integration

LearnApp displays progress in VOS4's HUD system.

**Integration**:

```kotlin
// HUDManager integration
class LearnAppHUDIntegration(
    private val hudManager: HUDManager,
    private val learnAppService: LearnAppService
) {

    fun startLearningWithHUD(packageName: String) {
        // Show HUD notification
        hudManager.showNotification(
            title = "Learning App",
            message = "Exploring $packageName...",
            icon = R.drawable.ic_learn
        )

        // Launch learning
        CoroutineScope(Dispatchers.Main).launch {
            try {
                learnAppService.observeProgress().collect { progress ->
                    // Update HUD with progress
                    hudManager.updateNotification(
                        title = "Learning: ${progress.percentageComplete.toInt()}%",
                        message = "Current: ${progress.currentScreen}"
                    )
                }

                // On completion
                hudManager.showNotification(
                    title = "Learning Complete",
                    message = "$packageName is now voice-enabled!",
                    icon = R.drawable.ic_check
                )

            } catch (e: Exception) {
                hudManager.showError("Learning failed: ${e.message}")
            }
        }
    }
}
```

### 4. CommandManager Integration

Enable voice commands for learned apps.

**Integration**:

```kotlin
// CommandManager integration
class LearnAppCommandIntegration(
    private val commandManager: CommandManager,
    private val learnAppDatabase: LearnAppDatabase
) {

    /**
     * Register voice commands for learned app.
     *
     * Creates commands like:
     * - "learn [app name]"
     * - "show app map for [app name]"
     * - "what can I do in [app name]"
     */
    fun registerCommands() {
        // "Learn [app]" command
        commandManager.registerCommand(
            pattern = "learn {appName}",
            handler = { params ->
                val appName = params["appName"] as String
                val packageName = resolvePackageName(appName)

                learnAppService.learnApp(packageName)
            }
        )

        // "Show map" command
        commandManager.registerCommand(
            pattern = "show app map for {appName}",
            handler = { params ->
                val appName = params["appName"] as String
                val packageName = resolvePackageName(appName)

                showAppMap(packageName)
            }
        )

        // "What can I do" command
        commandManager.registerCommand(
            pattern = "what can I do in {appName}",
            handler = { params ->
                val appName = params["appName"] as String
                val packageName = resolvePackageName(appName)

                listAvailableCommands(packageName)
            }
        )
    }

    private fun resolvePackageName(appName: String): String {
        // Map user-friendly name to package name
        return when (appName.lowercase()) {
            "instagram" -> "com.instagram.android"
            "twitter" -> "com.twitter.android"
            "gmail" -> "com.google.android.gm"
            else -> throw UnknownAppException(appName)
        }
    }

    private suspend fun showAppMap(packageName: String) {
        val screens = learnAppDatabase.getScreens(packageName)

        // Display in HUD
        hudManager.showAppMap(screens)
    }

    private suspend fun listAvailableCommands(packageName: String) {
        val elements = learnAppDatabase.getAllElements(packageName)

        // Extract clickable elements
        val clickableElements = elements.filter { it.elementType == "button" }

        // Generate voice commands
        val commands = clickableElements.mapNotNull { element ->
            element.text?.let { "click $it" }
        }

        // Show in HUD
        hudManager.showCommandList(commands)
    }
}
```

## Initialization Sequence

Complete initialization for LearnApp in VOS4:

```kotlin
// Application.kt
class VOS4Application : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize dependencies first
        UUIDCreator.initialize(this)
        HUDManager.initialize(this)
        CommandManager.initialize(this)

        // 2. Initialize LearnApp
        LearnAppService.initialize(this)

        // 3. Register LearnApp commands
        registerLearnAppCommands()

        // 4. Setup HUD integration
        setupLearnAppHUD()
    }

    private fun registerLearnAppCommands() {
        val commandManager = CommandManager.getInstance()
        val learnAppService = LearnAppService.getInstance()
        val learnAppDatabase = LearnAppDatabase.getInstance(this)

        val integration = LearnAppCommandIntegration(
            commandManager,
            learnAppDatabase
        )

        integration.registerCommands()
    }

    private fun setupLearnAppHUD() {
        val hudManager = HUDManager.getInstance()
        val learnAppService = LearnAppService.getInstance()

        val hudIntegration = LearnAppHUDIntegration(
            hudManager,
            learnAppService
        )

        // Add "Learn App" button to HUD
        hudManager.addAction(
            title = "Learn New App",
            icon = R.drawable.ic_learn,
            handler = {
                // Show app selector
                showAppSelector { selectedPackage ->
                    hudIntegration.startLearningWithHUD(selectedPackage)
                }
            }
        )
    }
}
```

---

# 6. Database Schema

## Database Overview

LearnApp uses Room database for persistence.

**Database Name**: `learnapp.db`
**Version**: 1
**Tables**: 3 (explored_apps, explored_screens, explored_elements)

## Schema Diagram

```
┌──────────────────────────┐
│    explored_apps         │
├──────────────────────────┤
│ PK package_name          │
│    app_name              │
│    learned_at            │
│    screens_explored      │
│    elements_found        │
│    exploration_version   │
└────────────┬─────────────┘
             │ 1
             │
             │ N
┌────────────┴─────────────┐
│    explored_screens      │
├──────────────────────────┤
│ PK screen_id             │
│ FK package_name          │
│    screen_name           │
│    depth                 │
│    parent_screen_id      │
│    timestamp             │
└────────────┬─────────────┘
             │ 1
             │
             │ N
┌────────────┴─────────────┐
│    explored_elements     │
├──────────────────────────┤
│ PK element_id            │
│ FK screen_id             │
│    uuid                  │
│    element_type          │
│    text                  │
│    content_description   │
│    is_clickable          │
│    is_dangerous          │
│    bounds_json           │
│    metadata_json         │
└──────────────────────────┘
```

## Table Definitions

### 1. explored_apps

Stores high-level information about learned apps.

```kotlin
@Entity(tableName = "explored_apps")
data class ExploredApp(
    /**
     * Android package name (e.g., "com.instagram.android").
     * Primary key.
     */
    @PrimaryKey
    val packageName: String,

    /**
     * User-friendly app name (e.g., "Instagram").
     * Retrieved from PackageManager.
     */
    val appName: String,

    /**
     * Timestamp when exploration completed.
     * Unix epoch milliseconds.
     */
    val learnedAt: Long,

    /**
     * Number of unique screens explored.
     */
    val screensExplored: Int,

    /**
     * Total number of interactive elements found.
     */
    val elementsFound: Int,

    /**
     * App version at time of exploration.
     * Used to detect when re-exploration needed.
     */
    val appVersion: String,

    /**
     * LearnApp version used for exploration.
     * For migration/compatibility purposes.
     */
    val explorationVersion: Int,

    /**
     * Exploration status: COMPLETE, PARTIAL, IN_PROGRESS.
     */
    val status: ExplorationStatus,

    /**
     * Configuration used for exploration (JSON).
     */
    val configJson: String
)
```

**Indexes**:
```kotlin
@Entity(
    tableName = "explored_apps",
    indices = [
        Index(value = ["learned_at"]),
        Index(value = ["status"])
    ]
)
```

### 2. explored_screens

Stores information about each screen explored.

```kotlin
@Entity(
    tableName = "explored_screens",
    foreignKeys = [
        ForeignKey(
            entity = ExploredApp::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExploredScreen(
    /**
     * Unique screen identifier.
     * Format: "{packageName}.screen.{hash}"
     * Primary key.
     */
    @PrimaryKey
    val screenId: String,

    /**
     * Parent app package name.
     * Foreign key to explored_apps.
     */
    val packageName: String,

    /**
     * Human-readable screen name.
     * Derived from activity name or window title.
     */
    val screenName: String,

    /**
     * Depth from home screen.
     * Home screen = 0, screens reachable from home = 1, etc.
     */
    val depth: Int,

    /**
     * Parent screen ID (null for home screen).
     * Used for navigation tree reconstruction.
     */
    val parentScreenId: String?,

    /**
     * Timestamp when screen first visited.
     */
    val timestamp: Long,

    /**
     * Activity class name (if available).
     */
    val activityName: String?,

    /**
     * Number of clickable elements on this screen.
     */
    val clickableCount: Int,

    /**
     * Screen hash for change detection.
     * Hash of screen structure.
     */
    val screenHash: String
)
```

**Indexes**:
```kotlin
@Entity(
    tableName = "explored_screens",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["parentScreenId"]),
        Index(value = ["depth"])
    ]
)
```

### 3. explored_elements

Stores information about each interactive element.

```kotlin
@Entity(
    tableName = "explored_elements",
    foreignKeys = [
        ForeignKey(
            entity = ExploredScreen::class,
            parentColumns = ["screenId"],
            childColumns = ["screenId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExploredElement(
    /**
     * Unique element identifier.
     * Format: "{screenId}.element.{index}"
     * Primary key.
     */
    @PrimaryKey
    val elementId: String,

    /**
     * Parent screen ID.
     * Foreign key to explored_screens.
     */
    val screenId: String,

    /**
     * UUID generated by ThirdPartyUuidGenerator.
     * Used for voice command targeting.
     */
    val uuid: String,

    /**
     * Element type: button, textfield, image, etc.
     */
    val elementType: String,

    /**
     * Visible text of element (null if no text).
     */
    val text: String?,

    /**
     * Accessibility content description.
     */
    val contentDescription: String?,

    /**
     * Whether element is clickable.
     */
    val isClickable: Boolean,

    /**
     * Whether element is focusable.
     */
    val isFocusable: Boolean,

    /**
     * Whether element is editable (text input).
     */
    val isEditable: Boolean,

    /**
     * Whether element flagged as dangerous by SafetyGuard.
     */
    val isDangerous: Boolean,

    /**
     * Element bounds on screen (JSON).
     * Format: {"left": 100, "top": 200, "right": 300, "bottom": 400}
     */
    val boundsJson: String,

    /**
     * Additional metadata (JSON).
     * Includes resourceId, className, hierarchy path, etc.
     */
    val metadataJson: String,

    /**
     * Timestamp when element discovered.
     */
    val discoveredAt: Long
)
```

**Indexes**:
```kotlin
@Entity(
    tableName = "explored_elements",
    indices = [
        Index(value = ["screenId"]),
        Index(value = ["uuid"], unique = true),
        Index(value = ["elementType"]),
        Index(value = ["isDangerous"])
    ]
)
```

## DAO Interfaces

### ExploredAppDao

```kotlin
@Dao
interface ExploredAppDao {

    @Query("SELECT * FROM explored_apps WHERE packageName = :packageName")
    suspend fun findByPackage(packageName: String): ExploredApp?

    @Query("SELECT * FROM explored_apps ORDER BY learnedAt DESC")
    suspend fun getAllApps(): List<ExploredApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ExploredApp)

    @Update
    suspend fun update(app: ExploredApp)

    @Query("DELETE FROM explored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT COUNT(*) FROM explored_apps")
    suspend fun getCount(): Int
}
```

### ExploredScreenDao

```kotlin
@Dao
interface ExploredScreenDao {

    @Query("SELECT * FROM explored_screens WHERE packageName = :packageName")
    suspend fun getScreens(packageName: String): List<ExploredScreen>

    @Query("SELECT * FROM explored_screens WHERE screenId = :screenId")
    suspend fun findByScreenId(screenId: String): ExploredScreen?

    @Query("SELECT * FROM explored_screens WHERE parentScreenId = :parentScreenId")
    suspend fun getChildScreens(parentScreenId: String): List<ExploredScreen>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(screen: ExploredScreen)

    @Query("DELETE FROM explored_screens WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
```

### ExploredElementDao

```kotlin
@Dao
interface ExploredElementDao {

    @Query("SELECT * FROM explored_elements WHERE screenId = :screenId")
    suspend fun getElements(screenId: String): List<ExploredElement>

    @Query("SELECT * FROM explored_elements WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): ExploredElement?

    @Query("""
        SELECT * FROM explored_elements
        WHERE screenId IN (
            SELECT screenId FROM explored_screens WHERE packageName = :packageName
        )
        AND (text LIKE '%' || :query || '%' OR contentDescription LIKE '%' || :query || '%')
    """)
    suspend fun searchElements(packageName: String, query: String): List<ExploredElement>

    @Query("""
        SELECT COUNT(*) FROM explored_elements
        WHERE screenId IN (
            SELECT screenId FROM explored_screens WHERE packageName = :packageName
        )
    """)
    suspend fun getElementCount(packageName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ExploredElement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elements: List<ExploredElement>)

    @Query("DELETE FROM explored_elements WHERE screenId = :screenId")
    suspend fun deleteByScreen(screenId: String)
}
```

## Database Queries - Examples

### Query 1: Get All Learned Apps

```kotlin
val dao = LearnAppDatabase.getInstance(context).exploredAppDao()
val apps = dao.getAllApps()

apps.forEach { app ->
    println("${app.appName}: ${app.elementsFound} elements")
}
```

### Query 2: Get App Exploration Tree

```kotlin
// Get root screens (depth = 0)
val screenDao = LearnAppDatabase.getInstance(context).exploredScreenDao()
val rootScreens = screenDao.getScreens(packageName).filter { it.depth == 0 }

// Recursively build tree
fun buildTree(parentScreen: ExploredScreen): ScreenNode {
    val children = screenDao.getChildScreens(parentScreen.screenId)
    return ScreenNode(
        screen = parentScreen,
        children = children.map { buildTree(it) }
    )
}

val tree = rootScreens.map { buildTree(it) }
```

### Query 3: Find All Buttons in App

```kotlin
val elementDao = LearnAppDatabase.getInstance(context).exploredElementDao()
val buttons = elementDao.searchElements(packageName, "")
    .filter { it.elementType == "button" }

buttons.forEach { button ->
    println("Button: ${button.text} on ${button.screenId}")
}
```

### Query 4: Get Dangerous Elements

```kotlin
val dangerousElements = elementDao.searchElements(packageName, "")
    .filter { it.isDangerous }

dangerousElements.forEach { element ->
    println("Dangerous: ${element.text} (${element.elementType})")
}
```

---

# 7. Exploration Algorithm

## DFS Strategy

LearnApp uses **Depth-First Search (DFS)** to explore the UI tree.

### Why DFS?

| Strategy | Pros | Cons | Chosen |
|----------|------|------|--------|
| **BFS** | Finds all shallow elements first | High memory usage, doesn't explore deep flows | ❌ No |
| **DFS** | Low memory, explores complete flows | May miss elements in early screens | ✅ YES |
| **Random** | Simple to implement | Incomplete coverage, unpredictable | ❌ No |

**DFS chosen because**:
- Natural fit for Android screen navigation (opening screens = going deeper)
- Low memory footprint (only track current path)
- Explores complete user flows (important for voice control)

## Algorithm Pseudocode

```
FUNCTION exploreApp(packageName):
    // 1. Initialize
    launchApp(packageName)
    rootNode = getRootAccessibilityNode()
    visited = Set()
    stack = Stack()

    // 2. Start DFS from root
    exploreNode(rootNode, visited, stack, depth=0)

    // 3. Save results
    saveExplorationResults()

FUNCTION exploreNode(node, visited, stack, depth):
    // Base cases
    IF depth > maxDepth THEN RETURN
    IF node.isVisited(visited) THEN RETURN
    IF safetyGuard.isDangerous(node) THEN RETURN

    // Mark visited
    screenId = getScreenId(node)
    visited.add(screenId)

    // Generate UUID and register element
    uuid = generateUUID(node)
    registerElement(uuid, node)

    // Get all clickable children
    clickableChildren = node.getChildren()
        .filter(child => child.isClickable())

    // Explore each child
    FOR EACH child IN clickableChildren:
        // Click and wait for screen to stabilize
        click(child)
        wait(clickDelayMs)

        // Check if new screen appeared
        newNode = getRootAccessibilityNode()
        newScreenId = getScreenId(newNode)

        IF newScreenId != screenId THEN
            // New screen - push current to stack and explore
            stack.push(screenId)
            exploreNode(newNode, visited, stack, depth + 1)

            // Backtrack
            backtrack(stack)
        END IF
    END FOR

FUNCTION backtrack(stack):
    IF stack.isEmpty() THEN RETURN

    // Go back to previous screen
    previousScreenId = stack.pop()

    // Use back button or navigation
    pressBack()
    wait(clickDelayMs)

    // Verify we're on previous screen
    currentScreenId = getScreenId(getRootAccessibilityNode())
    IF currentScreenId != previousScreenId THEN
        // Navigation failed, try alternative
        handleBacktrackFailure(previousScreenId)
    END IF
```

## Detailed Algorithm Implementation

### Step 1: Initialization

```kotlin
class ExplorerEngine(
    private val context: Context,
    private val config: LearnConfig,
    private val stateManager: StateManager,
    private val safetyGuard: SafetyGuard
) {

    /**
     * Start exploration from app launch.
     */
    suspend fun explore(packageName: String): ExploreResult = withContext(Dispatchers.IO) {
        // Launch app
        launchApp(packageName)
        delay(2000) // Wait for app to start

        // Get accessibility service
        val accessibilityService = getAccessibilityService()
            ?: throw AccessibilityException("Accessibility service not available")

        // Get root node
        val rootNode = accessibilityService.rootInActiveWindow
            ?: throw ExplorationException("Cannot get root node")

        // Initialize state
        stateManager.reset()
        stateManager.setPackageName(packageName)

        // Start DFS
        val startTime = System.currentTimeMillis()
        try {
            exploreNode(
                node = rootNode,
                depth = 0,
                parentScreenId = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exploration error", e)
        }

        val duration = System.currentTimeMillis() - startTime

        // Build result
        ExploreResult(
            status = ExplorationStatus.COMPLETE,
            screensExplored = stateManager.getVisitedScreens().size,
            elementsFound = stateManager.getVisitedElements().size,
            durationMs = duration
        )
    }

    private fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw PackageNotFoundException("Cannot launch $packageName")

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }
}
```

### Step 2: Node Exploration (DFS Core)

```kotlin
/**
 * Explore a single node and its children using DFS.
 *
 * This is the core of the exploration algorithm.
 */
private suspend fun exploreNode(
    node: AccessibilityNodeInfo,
    depth: Int,
    parentScreenId: String?
) {
    // Check termination conditions
    if (depth > config.maxDepth) {
        Log.d(TAG, "Max depth reached: $depth")
        return
    }

    if (stateManager.totalElementsVisited() >= config.maxElements) {
        Log.d(TAG, "Max elements reached")
        return
    }

    // Generate screen ID
    val screenId = generateScreenId(node)

    // Check if screen already visited
    if (stateManager.isScreenVisited(screenId)) {
        Log.d(TAG, "Screen already visited: $screenId")
        return
    }

    // Mark screen as visited
    stateManager.markScreenVisited(screenId)

    // Extract screen info
    val screenInfo = extractScreenInfo(node, depth, parentScreenId)

    // Save screen to database
    database.saveScreen(screenInfo)

    // Report progress
    reportProgress(screenId, depth)

    // Get all clickable elements on this screen
    val clickableElements = findClickableElements(node)

    Log.d(TAG, "Screen $screenId: ${clickableElements.size} clickable elements")

    // Explore each clickable element
    for ((index, element) in clickableElements.withIndex()) {
        // Check termination
        if (stateManager.totalElementsVisited() >= config.maxElements) break

        // Generate element ID
        val elementId = "$screenId.element.$index"

        // Check if element already visited
        if (stateManager.isElementVisited(elementId)) continue

        // Check safety
        if (config.enableDangerDetection && safetyGuard.isDangerous(element)) {
            Log.w(TAG, "Skipping dangerous element: ${element.text}")
            continue
        }

        // Mark element as visited
        stateManager.markElementVisited(elementId)

        // Generate UUID
        val uuid = uuidGenerator.generateUuid(element)

        // Extract element info
        val elementInfo = extractElementInfo(element, screenId, uuid, elementId)

        // Save element to database
        database.saveElement(elementInfo)

        // Try clicking the element
        val clickSuccess = tryClick(element)

        if (clickSuccess) {
            // Wait for UI to stabilize
            delay(config.clickDelayMs)

            // Check if screen changed
            val newRootNode = getAccessibilityService()?.rootInActiveWindow

            if (newRootNode != null) {
                val newScreenId = generateScreenId(newRootNode)

                if (newScreenId != screenId) {
                    // New screen appeared - explore it
                    Log.d(TAG, "Navigated to new screen: $newScreenId")

                    // Push current screen to stack
                    stateManager.pushNavigationState(
                        NavigationState(
                            screenId = screenId,
                            elementId = elementId
                        )
                    )

                    // Recursively explore new screen
                    exploreNode(
                        node = newRootNode,
                        depth = depth + 1,
                        parentScreenId = screenId
                    )

                    // Backtrack to this screen
                    backtrack()
                }
            }
        }
    }

    Log.d(TAG, "Finished exploring screen: $screenId")
}
```

### Step 3: Element Detection

```kotlin
/**
 * Find all clickable elements in node tree.
 *
 * Uses DFS to traverse the entire accessibility tree.
 */
private fun findClickableElements(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
    val clickableElements = mutableListOf<AccessibilityNodeInfo>()

    fun traverse(node: AccessibilityNodeInfo) {
        // Check if clickable
        if (node.isClickable || node.isFocusable) {
            clickableElements.add(node)
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverse(child)
            }
        }
    }

    traverse(rootNode)

    return clickableElements
}
```

### Step 4: Backtracking

```kotlin
/**
 * Backtrack to previous screen.
 *
 * Uses multiple strategies for reliability.
 */
private suspend fun backtrack() {
    val navigationState = stateManager.popNavigationState()
        ?: run {
            Log.d(TAG, "No state to backtrack to")
            return
        }

    val targetScreenId = navigationState.screenId

    Log.d(TAG, "Backtracking to: $targetScreenId")

    // Strategy 1: Use back button
    val backSuccess = tryBackButton()
    delay(config.clickDelayMs)

    // Verify we're on target screen
    val currentScreenId = getCurrentScreenId()

    if (currentScreenId == targetScreenId) {
        Log.d(TAG, "Backtrack successful via back button")
        return
    }

    // Strategy 2: Use up navigation
    val upSuccess = tryUpNavigation()
    delay(config.clickDelayMs)

    if (getCurrentScreenId() == targetScreenId) {
        Log.d(TAG, "Backtrack successful via up navigation")
        return
    }

    // Strategy 3: Re-launch app and navigate directly
    Log.w(TAG, "Standard backtrack failed, re-navigating")
    reNavigateToScreen(targetScreenId)
}

/**
 * Press Android back button.
 */
private fun tryBackButton(): Boolean {
    return try {
        val accessibilityService = getAccessibilityService() ?: return false
        accessibilityService.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "Back button failed", e)
        false
    }
}

/**
 * Click up/back arrow in app toolbar.
 */
private fun tryUpNavigation(): Boolean {
    return try {
        val rootNode = getAccessibilityService()?.rootInActiveWindow ?: return false

        // Find "Navigate up" button (common in toolbars)
        val upButton = findNodeByContentDescription(rootNode, "Navigate up")
            ?: findNodeByText(rootNode, "Back")
            ?: return false

        upButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        true
    } catch (e: Exception) {
        Log.e(TAG, "Up navigation failed", e)
        false
    }
}
```

### Step 5: Screen Change Detection

```kotlin
/**
 * Generate unique screen ID from current UI state.
 *
 * Used to detect screen changes.
 */
private fun generateScreenId(rootNode: AccessibilityNodeInfo): String {
    val packageName = rootNode.packageName?.toString() ?: "unknown"

    // Use activity name if available
    val activityName = getActivityName(rootNode)

    if (activityName != null) {
        return "$packageName.screen.$activityName"
    }

    // Fallback: Hash of screen structure
    val screenStructure = buildString {
        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 3) return // Only hash top 3 levels

            append(node.className)
            append(node.viewIdResourceName ?: "")
            append(node.text ?: "")
            append("|")

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it, depth + 1) }
            }
        }

        traverse(rootNode, 0)
    }

    val hash = screenStructure.hashCode()
    return "$packageName.screen.$hash"
}

/**
 * Get current activity name from accessibility info.
 */
private fun getActivityName(rootNode: AccessibilityNodeInfo): String? {
    // Try getting from window title
    rootNode.window?.title?.toString()?.let { title ->
        if (title.isNotBlank()) return title
    }

    // Try getting from package manager
    val accessibilityService = getAccessibilityService() ?: return null

    return try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        runningTasks.firstOrNull()?.topActivity?.className
    } catch (e: Exception) {
        null
    }
}
```

## State Management

### State Tracking

```kotlin
/**
 * Manages exploration state to avoid cycles and enable backtracking.
 */
class StateManager {

    // Visited screens
    private val visitedScreens = mutableSetOf<String>()

    // Visited elements
    private val visitedElements = mutableSetOf<String>()

    // Navigation stack for backtracking
    private val navigationStack = Stack<NavigationState>()

    // Current package being explored
    private var packageName: String? = null

    /**
     * Navigation state for backtracking.
     */
    data class NavigationState(
        val screenId: String,
        val elementId: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun reset() {
        visitedScreens.clear()
        visitedElements.clear()
        navigationStack.clear()
        packageName = null
    }

    fun setPackageName(pkg: String) {
        this.packageName = pkg
    }

    fun markScreenVisited(screenId: String) {
        visitedScreens.add(screenId)
    }

    fun markElementVisited(elementId: String) {
        visitedElements.add(elementId)
    }

    fun isScreenVisited(screenId: String): Boolean {
        return screenId in visitedScreens
    }

    fun isElementVisited(elementId: String): Boolean {
        return elementId in visitedElements
    }

    fun pushNavigationState(state: NavigationState) {
        navigationStack.push(state)
    }

    fun popNavigationState(): NavigationState? {
        return if (navigationStack.isNotEmpty()) {
            navigationStack.pop()
        } else {
            null
        }
    }

    fun getVisitedScreens(): Set<String> = visitedScreens.toSet()
    fun getVisitedElements(): Set<String> = visitedElements.toSet()

    fun totalElementsVisited(): Int = visitedElements.size
}
```

---

# 8. Safety Systems

## Danger Detection

LearnApp includes robust safety mechanisms to prevent destructive actions.

### SafetyGuard Component

```kotlin
/**
 * Safety guard to prevent dangerous actions during exploration.
 *
 * Analyzes elements before interaction to identify risky operations.
 */
class SafetyGuard {

    enum class DangerLevel {
        SAFE,        // No danger
        CAUTION,     // Potentially risky, proceed with care
        DANGEROUS    // Do not interact
    }

    /**
     * Check if element is dangerous.
     *
     * @param node AccessibilityNodeInfo to check
     * @return true if dangerous, false if safe
     */
    fun isDangerous(node: AccessibilityNodeInfo): Boolean {
        return getDangerLevel(node) == DangerLevel.DANGEROUS
    }

    /**
     * Analyze danger level of element.
     *
     * Uses multiple detection strategies:
     * - Text pattern matching
     * - Resource ID analysis
     * - Context analysis
     * - Button type detection
     *
     * @param node AccessibilityNodeInfo to analyze
     * @return DangerLevel enum value
     */
    fun getDangerLevel(node: AccessibilityNodeInfo): DangerLevel {
        // Check text content
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val resourceId = node.viewIdResourceName?.lowercase() ?: ""

        // DANGEROUS: Purchase/payment
        if (isPurchaseAction(text, desc, resourceId)) {
            return DangerLevel.DANGEROUS
        }

        // DANGEROUS: Delete/remove
        if (isDeleteAction(text, desc, resourceId)) {
            return DangerLevel.DANGEROUS
        }

        // DANGEROUS: Send message/email
        if (isSendAction(text, desc, resourceId)) {
            return DangerLevel.DANGEROUS
        }

        // DANGEROUS: Logout/sign out
        if (isLogoutAction(text, desc, resourceId)) {
            return DangerLevel.DANGEROUS
        }

        // DANGEROUS: Grant permission
        if (isPermissionGrant(text, desc, resourceId)) {
            return DangerLevel.DANGEROUS
        }

        // CAUTION: Settings/preferences
        if (isSettingsAction(text, desc, resourceId)) {
            return DangerLevel.CAUTION
        }

        // SAFE
        return DangerLevel.SAFE
    }

    /**
     * Detect purchase/payment buttons.
     */
    private fun isPurchaseAction(text: String, desc: String, resourceId: String): Boolean {
        val dangerPatterns = listOf(
            "buy", "purchase", "checkout", "pay", "payment",
            "order now", "add to cart", "subscribe", "upgrade",
            "complete purchase", "confirm payment"
        )

        return dangerPatterns.any { pattern ->
            text.contains(pattern) || desc.contains(pattern) || resourceId.contains(pattern)
        }
    }

    /**
     * Detect delete/remove buttons.
     */
    private fun isDeleteAction(text: String, desc: String, resourceId: String): Boolean {
        val dangerPatterns = listOf(
            "delete", "remove", "clear", "erase",
            "uninstall", "cancel", "discard",
            "permanently delete", "remove all"
        )

        return dangerPatterns.any { pattern ->
            text.contains(pattern) || desc.contains(pattern) || resourceId.contains(pattern)
        }
    }

    /**
     * Detect send message/email buttons.
     */
    private fun isSendAction(text: String, desc: String, resourceId: String): Boolean {
        val dangerPatterns = listOf(
            "send", "post", "publish", "share",
            "submit", "upload", "message"
        )

        return dangerPatterns.any { pattern ->
            text.contains(pattern) || desc.contains(pattern) || resourceId.contains(pattern)
        }
    }

    /**
     * Detect logout/sign out buttons.
     */
    private fun isLogoutAction(text: String, desc: String, resourceId: String): Boolean {
        val dangerPatterns = listOf(
            "logout", "log out", "sign out", "signout"
        )

        return dangerPatterns.any { pattern ->
            text.contains(pattern) || desc.contains(pattern) || resourceId.contains(pattern)
        }
    }

    /**
     * Detect permission grant buttons.
     */
    private fun isPermissionGrant(text: String, desc: String, resourceId: String): Boolean {
        val dangerPatterns = listOf(
            "allow", "grant", "enable", "accept",
            "ok", "yes", "continue"
        )

        // Only dangerous in permission dialog context
        val isPermissionDialog = resourceId.contains("permission") ||
                                 text.contains("permission") ||
                                 desc.contains("permission")

        if (!isPermissionDialog) return false

        return dangerPatterns.any { pattern ->
            text == pattern || desc == pattern
        }
    }

    /**
     * Detect settings/preference screens.
     */
    private fun isSettingsAction(text: String, desc: String, resourceId: String): Boolean {
        val cautionPatterns = listOf(
            "settings", "preferences", "options", "configuration"
        )

        return cautionPatterns.any { pattern ->
            text.contains(pattern) || desc.contains(pattern) || resourceId.contains(pattern)
        }
    }
}
```

## Login Screen Handling

```kotlin
/**
 * Detect and handle login screens.
 *
 * Login screens require special handling:
 * - Don't fill credentials
 * - Don't click login buttons
 * - May need to skip/dismiss
 */
class LoginScreenDetector {

    /**
     * Check if current screen is a login screen.
     */
    fun isLoginScreen(rootNode: AccessibilityNodeInfo): Boolean {
        // Look for common login indicators
        val indicators = findLoginIndicators(rootNode)

        // Require at least 2 indicators for positive detection
        return indicators.size >= 2
    }

    /**
     * Find login screen indicators.
     */
    private fun findLoginIndicators(rootNode: AccessibilityNodeInfo): List<String> {
        val indicators = mutableListOf<String>()

        // Check for username/email fields
        if (hasUsernameField(rootNode)) {
            indicators.add("username_field")
        }

        // Check for password fields
        if (hasPasswordField(rootNode)) {
            indicators.add("password_field")
        }

        // Check for login button
        if (hasLoginButton(rootNode)) {
            indicators.add("login_button")
        }

        // Check for "Sign in" text
        if (hasSignInText(rootNode)) {
            indicators.add("sign_in_text")
        }

        return indicators
    }

    private fun hasUsernameField(rootNode: AccessibilityNodeInfo): Boolean {
        val usernamePatterns = listOf("username", "email", "user", "login")
        return hasFieldWithPattern(rootNode, usernamePatterns)
    }

    private fun hasPasswordField(rootNode: AccessibilityNodeInfo): Boolean {
        return hasNodeWithProperty(rootNode) { node ->
            node.isPassword || node.className?.contains("EditText") == true &&
                    (node.hintText?.toString()?.lowercase()?.contains("password") == true)
        }
    }

    private fun hasLoginButton(rootNode: AccessibilityNodeInfo): Boolean {
        val loginPatterns = listOf("log in", "sign in", "login", "signin")
        return hasButtonWithPattern(rootNode, loginPatterns)
    }

    private fun hasSignInText(rootNode: AccessibilityNodeInfo): Boolean {
        return hasNodeWithProperty(rootNode) { node ->
            val text = node.text?.toString()?.lowercase() ?: ""
            text.contains("sign in") || text.contains("log in")
        }
    }

    /**
     * Attempt to skip/dismiss login screen.
     *
     * Looks for "Skip", "Later", "Not now" buttons.
     */
    fun trySkipLogin(rootNode: AccessibilityNodeInfo): Boolean {
        val skipPatterns = listOf("skip", "later", "not now", "maybe later", "dismiss")

        // Look for skip buttons
        val skipButton = findButtonWithPattern(rootNode, skipPatterns)

        if (skipButton != null) {
            skipButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        return false
    }

    // Helper methods
    private fun hasFieldWithPattern(rootNode: AccessibilityNodeInfo, patterns: List<String>): Boolean {
        return hasNodeWithProperty(rootNode) { node ->
            node.className?.contains("EditText") == true &&
                    patterns.any { pattern ->
                        node.hintText?.toString()?.lowercase()?.contains(pattern) == true ||
                                node.contentDescription?.toString()?.lowercase()?.contains(pattern) == true
                    }
        }
    }

    private fun hasButtonWithPattern(rootNode: AccessibilityNodeInfo, patterns: List<String>): Boolean {
        return hasNodeWithProperty(rootNode) { node ->
            node.isClickable &&
                    patterns.any { pattern ->
                        node.text?.toString()?.lowercase()?.contains(pattern) == true ||
                                node.contentDescription?.toString()?.lowercase()?.contains(pattern) == true
                    }
        }
    }

    private fun hasNodeWithProperty(
        rootNode: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (predicate(rootNode)) return true

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                if (hasNodeWithProperty(child, predicate)) return true
            }
        }

        return false
    }

    private fun findButtonWithPattern(
        rootNode: AccessibilityNodeInfo,
        patterns: List<String>
    ): AccessibilityNodeInfo? {
        if (rootNode.isClickable &&
            patterns.any { pattern ->
                rootNode.text?.toString()?.lowercase()?.contains(pattern) == true
            }
        ) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                findButtonWithPattern(child, patterns)?.let { return it }
            }
        }

        return null
    }
}
```

## Permission Handling

```kotlin
/**
 * Handle Android permission dialogs during exploration.
 *
 * Strategy: Grant non-sensitive permissions, skip sensitive ones.
 */
class PermissionHandler {

    private val sensitivePermissions = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.READ_SMS"
    )

    /**
     * Detect permission dialog.
     */
    fun isPermissionDialog(rootNode: AccessibilityNodeInfo): Boolean {
        // Look for system permission dialog indicators
        val packageName = rootNode.packageName?.toString() ?: ""

        // System permission dialogs use specific package names
        if (!packageName.startsWith("com.android") && !packageName.startsWith("com.google.android")) {
            return false
        }

        // Look for "Allow" and "Deny" buttons
        val hasAllowButton = hasButtonWithText(rootNode, listOf("allow", "ok"))
        val hasDenyButton = hasButtonWithText(rootNode, listOf("deny", "don't allow", "cancel"))

        return hasAllowButton && hasDenyButton
    }

    /**
     * Handle permission dialog.
     *
     * @return true if handled, false otherwise
     */
    fun handlePermissionDialog(rootNode: AccessibilityNodeInfo): Boolean {
        // Get permission type from dialog text
        val permissionType = extractPermissionType(rootNode)

        return if (permissionType != null && isSensitivePermission(permissionType)) {
            // Sensitive permission - deny
            clickDenyButton(rootNode)
        } else {
            // Non-sensitive permission - allow
            clickAllowButton(rootNode)
        }
    }

    private fun extractPermissionType(rootNode: AccessibilityNodeInfo): String? {
        // Look for permission description text
        val dialogText = extractAllText(rootNode).lowercase()

        return when {
            dialogText.contains("camera") -> "android.permission.CAMERA"
            dialogText.contains("microphone") || dialogText.contains("record audio") -> "android.permission.RECORD_AUDIO"
            dialogText.contains("contacts") -> "android.permission.READ_CONTACTS"
            dialogText.contains("location") -> "android.permission.ACCESS_FINE_LOCATION"
            dialogText.contains("sms") || dialogText.contains("messages") -> "android.permission.READ_SMS"
            dialogText.contains("storage") -> "android.permission.READ_EXTERNAL_STORAGE"
            else -> null
        }
    }

    private fun isSensitivePermission(permission: String): Boolean {
        return permission in sensitivePermissions
    }

    private fun clickAllowButton(rootNode: AccessibilityNodeInfo): Boolean {
        val allowButton = findButtonWithText(rootNode, listOf("allow", "ok"))
        return allowButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun clickDenyButton(rootNode: AccessibilityNodeInfo): Boolean {
        val denyButton = findButtonWithText(rootNode, listOf("deny", "don't allow", "cancel"))
        return denyButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun hasButtonWithText(rootNode: AccessibilityNodeInfo, patterns: List<String>): Boolean {
        return findButtonWithText(rootNode, patterns) != null
    }

    private fun findButtonWithText(
        rootNode: AccessibilityNodeInfo,
        patterns: List<String>
    ): AccessibilityNodeInfo? {
        if (rootNode.isClickable) {
            val text = rootNode.text?.toString()?.lowercase() ?: ""
            if (patterns.any { text.contains(it) }) {
                return rootNode
            }
        }

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                findButtonWithText(child, patterns)?.let { return it }
            }
        }

        return null
    }

    private fun extractAllText(rootNode: AccessibilityNodeInfo): String {
        val textBuilder = StringBuilder()

        fun traverse(node: AccessibilityNodeInfo) {
            node.text?.toString()?.let { textBuilder.append(it).append(" ") }
            node.contentDescription?.toString()?.let { textBuilder.append(it).append(" ") }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(rootNode)
        return textBuilder.toString()
    }
}
```

## Error Recovery

```kotlin
/**
 * Handle errors and crashes during exploration.
 */
class ErrorRecovery(
    private val context: Context,
    private val stateManager: StateManager
) {

    /**
     * Recover from app crash.
     */
    suspend fun recoverFromCrash(packageName: String): Boolean {
        Log.w(TAG, "Attempting crash recovery for $packageName")

        // Wait for crash dialog to appear
        delay(2000)

        // Try to dismiss crash dialog
        val dismissed = dismissCrashDialog()

        if (!dismissed) {
            Log.e(TAG, "Could not dismiss crash dialog")
            return false
        }

        // Re-launch app
        try {
            launchApp(packageName)
            delay(2000)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Could not re-launch app", e)
            return false
        }
    }

    /**
     * Dismiss Android crash dialog.
     */
    private fun dismissCrashDialog(): Boolean {
        val accessibilityService = getAccessibilityService() ?: return false
        val rootNode = accessibilityService.rootInActiveWindow ?: return false

        // Look for "Close app" or "OK" buttons
        val closeButton = findButtonWithText(rootNode, listOf("close app", "ok", "dismiss"))

        return closeButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    /**
     * Handle ANR (Application Not Responding) dialog.
     */
    fun handleANRDialog(): Boolean {
        val accessibilityService = getAccessibilityService() ?: return false
        val rootNode = accessibilityService.rootInActiveWindow ?: return false

        // Look for "Wait" button (prefer waiting over closing)
        val waitButton = findButtonWithText(rootNode, listOf("wait"))
        if (waitButton != null) {
            waitButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }

        // No wait button - click OK to close
        val okButton = findButtonWithText(rootNode, listOf("ok", "close"))
        return okButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
}
```

---

# 9. Performance Considerations

## Memory Management

### Accessibility Node Recycling

```kotlin
/**
 * Properly recycle AccessibilityNodeInfo to prevent memory leaks.
 *
 * CRITICAL: AccessibilityNodeInfo instances must be recycled after use.
 */
class NodeManager {

    /**
     * Process node and recycle when done.
     */
    inline fun <T> useNode(
        node: AccessibilityNodeInfo,
        block: (AccessibilityNodeInfo) -> T
    ): T {
        try {
            return block(node)
        } finally {
            node.recycle()
        }
    }

    /**
     * Get children and recycle parent.
     */
    fun getChildrenAndRecycle(parent: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        return try {
            (0 until parent.childCount).mapNotNull { parent.getChild(it) }
        } finally {
            parent.recycle()
        }
    }
}

// Usage
nodeManager.useNode(accessibilityNodeInfo) { node ->
    val text = node.text?.toString()
    val isClickable = node.isClickable
    // ... process node
}
// node is automatically recycled
```

### Caching Strategy

```kotlin
/**
 * Cache frequently accessed data to reduce database queries.
 */
class ExplorationCache {

    // Screen ID cache (LRU, max 100 entries)
    private val screenIdCache = LruCache<String, ExploredScreen>(100)

    // Element cache (LRU, max 500 entries)
    private val elementCache = LruCache<String, ExploredElement>(500)

    // UUID cache for quick lookups
    private val uuidCache = ConcurrentHashMap<String, ExploredElement>()

    fun cacheScreen(screen: ExploredScreen) {
        screenIdCache.put(screen.screenId, screen)
    }

    fun getScreen(screenId: String): ExploredScreen? {
        return screenIdCache.get(screenId)
    }

    fun cacheElement(element: ExploredElement) {
        elementCache.put(element.elementId, element)
        uuidCache[element.uuid] = element
    }

    fun findByUuid(uuid: String): ExploredElement? {
        return uuidCache[uuid]
    }

    fun clear() {
        screenIdCache.evictAll()
        elementCache.evictAll()
        uuidCache.clear()
    }
}
```

## Thread Safety

### Concurrent Access Protection

```kotlin
/**
 * Thread-safe state management using Mutex.
 */
class ThreadSafeStateManager {

    private val visitedScreens = mutableSetOf<String>()
    private val visitedElements = mutableSetOf<String>()
    private val mutex = Mutex()

    suspend fun markScreenVisited(screenId: String) {
        mutex.withLock {
            visitedScreens.add(screenId)
        }
    }

    suspend fun isScreenVisited(screenId: String): Boolean {
        return mutex.withLock {
            screenId in visitedScreens
        }
    }

    suspend fun getVisitedCount(): Pair<Int, Int> {
        return mutex.withLock {
            Pair(visitedScreens.size, visitedElements.size)
        }
    }
}
```

### Flow-Based Progress Updates

```kotlin
/**
 * Thread-safe progress reporting using StateFlow.
 */
class ProgressReporter {

    private val _progress = MutableStateFlow<LearnProgress?>(null)
    val progress: StateFlow<LearnProgress?> = _progress.asStateFlow()

    fun updateProgress(progress: LearnProgress) {
        _progress.value = progress
    }

    fun clearProgress() {
        _progress.value = null
    }
}

// Collect from UI
lifecycleScope.launch {
    progressReporter.progress.collect { progress ->
        progress?.let { updateUI(it) }
    }
}
```

## Database Optimization

### Batch Inserts

```kotlin
/**
 * Insert elements in batches for better performance.
 */
class BatchInserter(
    private val database: LearnAppDatabase,
    private val batchSize: Int = 50
) {

    private val pendingElements = mutableListOf<ExploredElement>()

    suspend fun add(element: ExploredElement) {
        pendingElements.add(element)

        if (pendingElements.size >= batchSize) {
            flush()
        }
    }

    suspend fun flush() {
        if (pendingElements.isEmpty()) return

        database.exploredElementDao().insertAll(pendingElements)
        pendingElements.clear()
    }
}
```

### Index Usage

Ensure queries use indexes:

```kotlin
// GOOD: Uses index on packageName
@Query("SELECT * FROM explored_screens WHERE packageName = :pkg")
suspend fun getScreens(pkg: String): List<ExploredScreen>

// BAD: Table scan (no index)
@Query("SELECT * FROM explored_elements WHERE text LIKE '%:query%'")
suspend fun searchByText(query: String): List<ExploredElement>

// GOOD: Add index
@Entity(
    indices = [Index(value = ["text"], name = "idx_text")]
)
```

## Optimization Tips

### 1. Limit Exploration Depth

```kotlin
// Shallow exploration (faster)
val config = LearnConfig(maxDepth = 5)

// Deep exploration (slower but thorough)
val config = LearnConfig(maxDepth = 20)
```

### 2. Set Element Limits

```kotlin
// Stop after 500 elements (quick scan)
val config = LearnConfig(maxElements = 500)

// Unlimited (full exploration)
val config = LearnConfig(maxElements = Int.MAX_VALUE)
```

### 3. Adjust Click Delay

```kotlin
// Fast apps: reduce delay
val config = LearnConfig(clickDelayMs = 500)

// Slow apps: increase delay
val config = LearnConfig(clickDelayMs = 2000)
```

### 4. Use Background Processing

```kotlin
// Launch exploration in background
lifecycleScope.launch(Dispatchers.IO) {
    learnAppService.learnApp(packageName)
}
```

### 5. Monitor Memory Usage

```kotlin
/**
 * Monitor memory during exploration.
 */
class MemoryMonitor {

    fun checkMemory(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return MemoryInfo(
            used = usedMemory,
            free = freeMemory,
            max = maxMemory,
            percentUsed = (usedMemory.toFloat() / maxMemory) * 100
        )
    }

    fun shouldPauseExploration(): Boolean {
        val memInfo = checkMemory()
        return memInfo.percentUsed > 80 // Pause if >80% memory used
    }
}
```

---

# 10. Examples

## Novice Examples

### Example 1: Basic Learning

```kotlin
/**
 * NOVICE: Learn an app with default settings.
 *
 * This is the simplest way to use LearnApp.
 */
class SimpleLearnExample : ComponentActivity() {

    private val learnAppService = LearnAppService.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.learnButton).setOnClickListener {
            learnInstagram()
        }
    }

    private fun learnInstagram() {
        lifecycleScope.launch {
            try {
                // Learn Instagram with default config
                val result = learnAppService.learnApp("com.instagram.android")

                // Show result
                Toast.makeText(
                    this@SimpleLearnExample,
                    "Learned ${result.screensExplored} screens!",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@SimpleLearnExample,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
```

### Example 2: Show Progress

```kotlin
/**
 * NOVICE: Display exploration progress.
 */
@Composable
fun LearnProgressScreen() {
    val learnAppService = remember { LearnAppService.getInstance() }
    var progress by remember { mutableStateOf<LearnProgress?>(null) }

    // Observe progress
    LaunchedEffect(Unit) {
        learnAppService.observeProgress().collect {
            progress = it
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        progress?.let {
            Text("Learning in progress...", style = MaterialTheme.typography.h5)

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = it.percentageComplete,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("${(it.percentageComplete * 100).toInt()}%")

            Spacer(modifier = Modifier.height(24.dp))

            Text("Current: ${it.currentScreen}")
            Text("Elements found: ${it.elementsFound}")
            Text("Screens explored: ${it.screensExplored}")
        } ?: run {
            Text("No exploration in progress")
        }
    }
}
```

## Intermediate Examples

### Example 3: Custom Configuration

```kotlin
/**
 * INTERMEDIATE: Learn with custom configuration.
 */
class CustomConfigExample {

    private val learnAppService = LearnAppService.getInstance()

    suspend fun learnWithCustomConfig(packageName: String) {
        // Create custom config
        val config = LearnConfig(
            maxDepth = 10,            // Explore up to 10 screens deep
            maxElements = 5000,        // Stop after 5000 elements
            timeoutMs = 600_000,       // 10 minute timeout
            clickDelayMs = 1500,       // 1.5 second delay after clicks
            enableDangerDetection = true,
            verboseLogging = true      // Enable debug logs
        )

        // Learn with config
        val result = learnAppService.learnApp(packageName, config)

        Log.d(TAG, "Exploration complete:")
        Log.d(TAG, "  Screens: ${result.screensExplored}")
        Log.d(TAG, "  Elements: ${result.elementsFound}")
        Log.d(TAG, "  Duration: ${result.durationMs}ms")
        Log.d(TAG, "  Status: ${result.status}")
    }
}
```

### Example 4: Query Learned Data

```kotlin
/**
 * INTERMEDIATE: Query and display learned app data.
 */
class QueryExampleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LearnAppDatabase.getInstance(application)

    /**
     * Get all learned apps.
     */
    fun getLearnedApps(): LiveData<List<ExploredApp>> {
        return liveData {
            val apps = database.exploredAppDao().getAllApps()
            emit(apps)
        }
    }

    /**
     * Get screens for an app.
     */
    fun getAppScreens(packageName: String): LiveData<List<ExploredScreen>> {
        return liveData {
            val screens = database.exploredScreenDao().getScreens(packageName)
            emit(screens)
        }
    }

    /**
     * Search for elements by text.
     */
    fun searchElements(packageName: String, query: String): LiveData<List<ExploredElement>> {
        return liveData {
            val elements = database.exploredElementDao().searchElements(packageName, query)
            emit(elements)
        }
    }
}

// Usage in Fragment
class AppDetailsFragment : Fragment() {

    private val viewModel: QueryExampleViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val packageName = arguments?.getString("packageName") ?: return

        // Observe screens
        viewModel.getAppScreens(packageName).observe(viewLifecycleOwner) { screens ->
            displayScreens(screens)
        }

        // Search for buttons
        viewModel.searchElements(packageName, "button").observe(viewLifecycleOwner) { buttons ->
            displayButtons(buttons)
        }
    }
}
```

## Expert Examples

### Example 5: Custom Safety Rules

```kotlin
/**
 * EXPERT: Implement custom safety detection rules.
 */
class CustomSafetyGuard : SafetyGuard() {

    /**
     * Override danger detection with custom rules.
     */
    override fun isDangerous(node: AccessibilityNodeInfo): Boolean {
        // First check base implementation
        if (super.isDangerous(node)) return true

        // Add custom rules
        val text = node.text?.toString()?.lowercase() ?: ""
        val resourceId = node.viewIdResourceName?.lowercase() ?: ""

        // Custom rule 1: Skip all buttons in payment section
        if (resourceId.contains("payment") && node.isClickable) {
            return true
        }

        // Custom rule 2: Skip social sharing
        if (text.contains("share to") || text.contains("post to")) {
            return true
        }

        // Custom rule 3: App-specific rules
        val packageName = node.packageName?.toString() ?: ""
        when (packageName) {
            "com.instagram.android" -> {
                // Don't like/comment on posts
                if (text.contains("like") || text.contains("comment")) {
                    return true
                }
            }
            "com.twitter.android" -> {
                // Don't tweet/retweet
                if (text.contains("tweet") || text.contains("retweet")) {
                    return true
                }
            }
        }

        return false
    }
}

// Use custom safety guard
class CustomLearnAppService(context: Context) {

    private val explorerEngine = ExplorerEngine(
        context = context,
        config = LearnConfig.DEFAULT,
        stateManager = StateManager(),
        safetyGuard = CustomSafetyGuard() // Use custom guard
    )
}
```

### Example 6: Export Learned Data

```kotlin
/**
 * EXPERT: Export learned app data to JSON.
 */
class LearnDataExporter(private val context: Context) {

    private val database = LearnAppDatabase.getInstance(context)
    private val json = Json { prettyPrint = true }

    /**
     * Export learned app to JSON file.
     */
    suspend fun exportToJson(packageName: String): File {
        // Get app data
        val app = database.exploredAppDao().findByPackage(packageName)
            ?: throw IllegalArgumentException("App not learned: $packageName")

        val screens = database.exploredScreenDao().getScreens(packageName)

        val elementsMap = screens.associate { screen ->
            screen.screenId to database.exploredElementDao().getElements(screen.screenId)
        }

        // Build export data
        val exportData = ExportData(
            app = app,
            screens = screens,
            elements = elementsMap
        )

        // Serialize to JSON
        val jsonString = json.encodeToString(exportData)

        // Write to file
        val outputFile = File(context.filesDir, "$packageName-learned.json")
        outputFile.writeText(jsonString)

        return outputFile
    }

    @Serializable
    data class ExportData(
        val app: ExploredApp,
        val screens: List<ExploredScreen>,
        val elements: Map<String, List<ExploredElement>>
    )
}

// Usage
val exporter = LearnDataExporter(context)
val file = exporter.exportToJson("com.instagram.android")
Log.d(TAG, "Exported to: ${file.absolutePath}")
```

### Example 7: Resume After Crash

```kotlin
/**
 * EXPERT: Implement crash recovery and resumption.
 */
class ResilientLearner(
    private val context: Context,
    private val learnAppService: LearnAppService
) {

    /**
     * Learn app with automatic crash recovery.
     */
    suspend fun learnWithRecovery(
        packageName: String,
        maxRetries: Int = 3
    ): LearnResult {
        var attempt = 0
        var lastError: Exception? = null

        while (attempt < maxRetries) {
            try {
                // Try learning
                val result = learnAppService.learnApp(
                    packageName = packageName,
                    config = LearnConfig(
                        resumeFromLastSession = attempt > 0 // Resume on retry
                    )
                )

                return result

            } catch (e: ExplorationException) {
                lastError = e
                attempt++

                Log.w(TAG, "Attempt $attempt failed: ${e.message}")

                if (attempt < maxRetries) {
                    // Wait before retrying
                    delay(5000)

                    // Try to recover
                    val recovered = recoverFromError(packageName, e)

                    if (!recovered) {
                        throw e
                    }
                }
            }
        }

        throw lastError ?: ExplorationException("Learning failed after $maxRetries attempts")
    }

    private suspend fun recoverFromError(
        packageName: String,
        error: Exception
    ): Boolean {
        return when (error) {
            is AppCrashException -> {
                // App crashed, try restarting
                ErrorRecovery(context, StateManager()).recoverFromCrash(packageName)
            }
            is AccessibilityException -> {
                // Accessibility service disconnected, wait and retry
                delay(5000)
                true
            }
            else -> false
        }
    }
}
```

---

# 11. Troubleshooting

## Common Issues

### Issue 1: Accessibility Service Not Enabled

**Symptom**: `AccessibilityException` when starting exploration

**Cause**: VoiceAccessibility service not enabled in system settings

**Solution**:
```kotlin
fun checkAccessibilityEnabled(): Boolean {
    val service = "com.augmentalis.voiceos/.VoiceAccessibilityService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )

    return enabledServices?.contains(service) == true
}

fun promptEnableAccessibility() {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)

    Toast.makeText(
        context,
        "Please enable VoiceOS Accessibility",
        Toast.LENGTH_LONG
    ).show()
}
```

### Issue 2: Exploration Gets Stuck

**Symptom**: Progress stops updating, exploration hangs

**Causes**:
1. App showing loading spinner
2. Network request waiting
3. Animation not completing
4. Modal dialog blocking interaction

**Solutions**:

```kotlin
// Increase click delay for slow apps
val config = LearnConfig(clickDelayMs = 2000)

// Set timeout to prevent infinite waiting
val config = LearnConfig(timeoutMs = 600_000) // 10 minutes

// Implement stuck detection
class StuckDetector {
    private var lastProgressTime = System.currentTimeMillis()
    private var lastElementCount = 0

    fun checkStuck(currentElementCount: Int): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceProgress = now - lastProgressTime

        if (currentElementCount > lastElementCount) {
            // Progress made
            lastProgressTime = now
            lastElementCount = currentElementCount
            return false
        }

        // No progress for 60 seconds = stuck
        return timeSinceProgress > 60_000
    }

    fun reset() {
        lastProgressTime = System.currentTimeMillis()
        lastElementCount = 0
    }
}
```

### Issue 3: Elements Not Clickable

**Symptom**: Clicks don't register, UI doesn't respond

**Causes**:
1. Element behind overlay
2. Animation in progress
3. Element not truly clickable

**Solutions**:

```kotlin
/**
 * Retry click with multiple strategies.
 */
suspend fun tryClickWithRetry(
    node: AccessibilityNodeInfo,
    maxAttempts: Int = 3
): Boolean {
    for (attempt in 1..maxAttempts) {
        // Strategy 1: Regular click
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            delay(500)
            return true
        }

        // Strategy 2: Focus then click
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        delay(200)
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            delay(500)
            return true
        }

        // Strategy 3: Click center coordinates
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        if (performGlobalClick(centerX, centerY)) {
            delay(500)
            return true
        }

        // Wait before retry
        delay(1000)
    }

    return false
}
```

### Issue 4: Database Corruption

**Symptom**: SQLite errors, data inconsistencies

**Cause**: App killed during database write

**Solution**:

```kotlin
/**
 * Verify and repair database.
 */
class DatabaseHealthCheck(private val context: Context) {

    suspend fun checkAndRepair(): Boolean = withContext(Dispatchers.IO) {
        val database = LearnAppDatabase.getInstance(context)

        try {
            // Test database integrity
            val appCount = database.exploredAppDao().getCount()
            Log.d(TAG, "Database OK: $appCount apps")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Database corrupted: ${e.message}")

            // Close database
            database.close()

            // Delete database file
            val dbFile = context.getDatabasePath("learnapp.db")
            val deleted = dbFile.delete()

            if (deleted) {
                Log.d(TAG, "Corrupted database deleted, will recreate")
                true
            } else {
                Log.e(TAG, "Could not delete database")
                false
            }
        }
    }
}
```

### Issue 5: High Memory Usage

**Symptom**: OutOfMemoryError, app slowdown

**Cause**: AccessibilityNodeInfo not recycled, large cache

**Solutions**:

```kotlin
/**
 * Monitor and manage memory usage.
 */
class MemoryManager {

    fun checkMemory(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory

        return MemoryStats(
            usedMB = usedMemory / (1024 * 1024),
            freeMB = freeMemory / (1024 * 1024),
            maxMB = maxMemory / (1024 * 1024),
            percentUsed = (usedMemory.toFloat() / maxMemory) * 100
        )
    }

    fun shouldReduceCache(stats: MemoryStats): Boolean {
        return stats.percentUsed > 75 // Above 75% memory usage
    }

    fun forceGarbageCollection() {
        System.gc()
        Thread.sleep(100)
    }
}

// Use in exploration
if (memoryManager.shouldReduceCache(memoryManager.checkMemory())) {
    // Clear caches
    explorationCache.clear()
    memoryManager.forceGarbageCollection()
}
```

## Debug Logging

### Enable Verbose Logging

```kotlin
// Enable verbose logs
val config = LearnConfig(verboseLogging = true)
val result = learnAppService.learnApp(packageName, config)
```

### Custom Logger

```kotlin
/**
 * Custom logger for debugging.
 */
class LearnAppLogger {

    private val logFile = File(context.filesDir, "learnapp-debug.log")

    fun log(level: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date())

        val logLine = "$timestamp [$level] $message\n"

        // Write to file
        logFile.appendText(logLine)

        // Also log to Logcat
        when (level) {
            "DEBUG" -> Log.d(TAG, message)
            "INFO" -> Log.i(TAG, message)
            "WARN" -> Log.w(TAG, message)
            "ERROR" -> Log.e(TAG, message)
        }
    }

    fun debug(message: String) = log("DEBUG", message)
    fun info(message: String) = log("INFO", message)
    fun warn(message: String) = log("WARN", message)
    fun error(message: String) = log("ERROR", message)

    fun getLogFile(): File = logFile
}
```

---

# 12. Testing Guide

## Unit Testing

### Test 1: SafetyGuard Detection

```kotlin
@RunWith(AndroidJUnit4::class)
class SafetyGuardTest {

    private lateinit var safetyGuard: SafetyGuard

    @Before
    fun setup() {
        safetyGuard = SafetyGuard()
    }

    @Test
    fun `detect purchase button as dangerous`() {
        val node = createMockNode(text = "Buy Now", isClickable = true)

        assertTrue(safetyGuard.isDangerous(node))
        assertEquals(DangerLevel.DANGEROUS, safetyGuard.getDangerLevel(node))
    }

    @Test
    fun `detect delete button as dangerous`() {
        val node = createMockNode(text = "Delete Account", isClickable = true)

        assertTrue(safetyGuard.isDangerous(node))
    }

    @Test
    fun `regular button is safe`() {
        val node = createMockNode(text = "View Profile", isClickable = true)

        assertFalse(safetyGuard.isDangerous(node))
        assertEquals(DangerLevel.SAFE, safetyGuard.getDangerLevel(node))
    }

    private fun createMockNode(
        text: String,
        isClickable: Boolean = false
    ): AccessibilityNodeInfo {
        return mock(AccessibilityNodeInfo::class.java).apply {
            `when`(this.text).thenReturn(text)
            `when`(this.isClickable).thenReturn(isClickable)
        }
    }
}
```

### Test 2: StateManager

```kotlin
@Test
class StateManagerTest {

    private lateinit var stateManager: StateManager

    @Before
    fun setup() {
        stateManager = StateManager()
    }

    @Test
    fun `screen marked as visited is visited`() {
        val screenId = "com.example.app.screen.home"

        assertFalse(stateManager.isScreenVisited(screenId))

        stateManager.markScreenVisited(screenId)

        assertTrue(stateManager.isScreenVisited(screenId))
    }

    @Test
    fun `navigation stack works correctly`() {
        val state1 = NavigationState("screen1", "element1")
        val state2 = NavigationState("screen2", "element2")

        stateManager.pushNavigationState(state1)
        stateManager.pushNavigationState(state2)

        assertEquals(state2, stateManager.popNavigationState())
        assertEquals(state1, stateManager.popNavigationState())
        assertNull(stateManager.popNavigationState())
    }

    @Test
    fun `reset clears all state`() {
        stateManager.markScreenVisited("screen1")
        stateManager.markElementVisited("element1")
        stateManager.pushNavigationState(NavigationState("screen1", "element1"))

        stateManager.reset()

        assertFalse(stateManager.isScreenVisited("screen1"))
        assertFalse(stateManager.isElementVisited("element1"))
        assertNull(stateManager.popNavigationState())
    }
}
```

## Integration Testing

### Test 3: End-to-End Exploration

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class LearnAppIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var learnAppService: LearnAppService
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        learnAppService = LearnAppService.getInstance()
    }

    @Test
    fun `learn calculator app successfully`() = runTest {
        // Calculator is a simple app, good for testing
        val packageName = "com.android.calculator2"

        val result = learnAppService.learnApp(
            packageName = packageName,
            config = LearnConfig.QUICK // Use quick config for testing
        )

        // Verify success
        assertEquals(ExplorationStatus.COMPLETE, result.status)
        assertTrue(result.screensExplored > 0)
        assertTrue(result.elementsFound > 0)

        // Verify data saved to database
        val database = LearnAppDatabase.getInstance(context)
        val savedApp = database.exploredAppDao().findByPackage(packageName)

        assertNotNull(savedApp)
        assertEquals(packageName, savedApp?.packageName)
    }

    @Test
    fun `exploration respects max depth config`() = runTest {
        val config = LearnConfig(maxDepth = 2)

        val result = learnAppService.learnApp("com.android.calculator2", config)

        // Check that no screens beyond depth 2 were explored
        val database = LearnAppDatabase.getInstance(context)
        val screens = database.exploredScreenDao().getScreens("com.android.calculator2")

        assertTrue(screens.all { it.depth <= 2 })
    }
}
```

## Manual Testing

### Manual Test Checklist

```markdown
# LearnApp Manual Test Checklist

## Prerequisites
- [ ] VoiceAccessibility service enabled
- [ ] Test app installed (e.g., Instagram)
- [ ] Device has sufficient storage (>500MB free)
- [ ] Battery >50%

## Basic Functionality
- [ ] Launch learning via voice command
- [ ] Launch learning via HUD button
- [ ] Progress updates display correctly
- [ ] Percentage increases over time
- [ ] Current screen name updates
- [ ] Element count increases

## Pause/Resume
- [ ] Pause button works
- [ ] Progress saves when paused
- [ ] Resume continues from pause point
- [ ] No duplicate elements after resume

## Safety
- [ ] Purchase buttons skipped
- [ ] Delete buttons skipped
- [ ] Logout buttons skipped
- [ ] Send message buttons skipped
- [ ] Settings screens handled correctly

## Special Screens
- [ ] Login screen detected and skipped
- [ ] Permission dialogs handled
- [ ] Splash screens don't block exploration
- [ ] Loading screens don't cause hang

## Backtracking
- [ ] Back button works
- [ ] Up navigation works
- [ ] Returns to correct screen
- [ ] No crashes during backtrack

## Completion
- [ ] Exploration completes successfully
- [ ] Summary shows correct counts
- [ ] Data saved to database
- [ ] App map displays correctly
- [ ] UUIDs generated for all elements

## Error Handling
- [ ] App crash detected and recovered
- [ ] ANR handled correctly
- [ ] Network errors don't stop exploration
- [ ] Memory warnings handled

## Performance
- [ ] No memory leaks
- [ ] CPU usage reasonable (<50%)
- [ ] Battery drain acceptable
- [ ] Database size reasonable
```

---

# Conclusion

LearnApp is a powerful system for automated UI exploration that enables universal voice control for Android apps. This comprehensive guide covers:

- **Architecture**: Modular design with clear responsibilities
- **Algorithm**: DFS-based exploration with intelligent backtracking
- **Safety**: Multi-layered protection against dangerous actions
- **Integration**: Seamless connection with VOS4 systems
- **Performance**: Optimized for memory and speed
- **Testing**: Complete coverage with unit and integration tests

## Next Steps

1. **Implement Core Components**: Start with ExplorerEngine and StateManager
2. **Add Safety Systems**: Implement SafetyGuard and LoginScreenDetector
3. **Database Setup**: Create Room database with proper indexes
4. **Integration**: Connect to VoiceAccessibility and UUIDCreator
5. **Testing**: Write comprehensive tests for all components
6. **UI Development**: Build HUD interface for user interaction
7. **Optimization**: Profile and optimize performance
8. **Documentation**: Keep this guide updated as features evolve

## Additional Resources

- [UUIDCreator Developer Manual](/docs/modules/UUIDCreator/COMPREHENSIVE-DEVELOPER-MANUAL.md)
- [Third-Party UUID Generation Guide](/docs/modules/UUIDCreator/architecture/thirdPartyAppUuidGeneration.md)
- [VoiceAccessibility Integration](/docs/modules/voice-accessibility/)
- [Android Accessibility Service Guide](https://developer.android.com/guide/topics/ui/accessibility/service)

---

**Version**: 1.0.0
**Last Updated**: 2025-10-08
**Maintainer**: VOS4 Development Team
**License**: Proprietary - Augmentalis


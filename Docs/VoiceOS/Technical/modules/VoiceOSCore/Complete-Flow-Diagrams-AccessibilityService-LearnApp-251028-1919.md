# Complete Flow Diagrams: AccessibilityService & LearnApp Integration

**Date:** 2025-10-28 19:19 PDT
**Module:** VoiceOSCore + LearnApp
**Purpose:** Complete architectural flow diagrams to identify integration issues
**Status:** Analysis Complete

---

## Table of Contents

1. [System Architecture Overview](#1-system-architecture-overview)
2. [VoiceOSService Lifecycle Flow](#2-voiceosservice-lifecycle-flow)
3. [LearnApp Integration Flow](#3-learnapp-integration-flow)
4. [Consent Dialog Flow (Fixed)](#4-consent-dialog-flow-fixed)
5. [Exploration Engine Flow](#5-exploration-engine-flow)
6. [Event Processing Flow](#6-event-processing-flow)
7. [Command Execution Flow (3-Tier)](#7-command-execution-flow-3-tier)
8. [Database Architecture](#8-database-architecture)
9. [Threading Model](#9-threading-model)
10. [Known Issues & Problem Areas](#10-known-issues--problem-areas)

---

## 1. System Architecture Overview

### High-Level Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           ANDROID SYSTEM                             │
│  ┌─────────────────────┐          ┌──────────────────────┐         │
│  │ AccessibilityEvent  │──────────│  Package Manager     │         │
│  └──────────┬──────────┘          └──────────────────────┘         │
└─────────────┼───────────────────────────────────────────────────────┘
              │
              │ Events
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         VoiceOSService                               │
│                    (AccessibilityService)                            │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    Core Components                            │  │
│  │  • UIScrapingEngine       - Extracts UI elements             │  │
│  │  • SpeechEngineManager    - Speech recognition (Vivoka)      │  │
│  │  • ActionCoordinator      - Executes UI actions              │  │
│  │  • CommandManager         - Command routing (Tier 1)         │  │
│  │  • VoiceCommandProcessor  - App commands (Tier 2)            │  │
│  │  • WebCommandCoordinator  - Browser commands                 │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Integration Layer                            │  │
│  │  • LearnAppIntegration    - Third-party app learning         │  │
│  │  • AccessibilityScrapingIntegration - Base scraping          │  │
│  │  • VoiceCursorAPI         - Cursor functionality             │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    Persistence Layer                          │  │
│  │  • AppScrapingDatabase    - Learned UI elements              │  │
│  │  • CommandDatabase        - Voice commands (94 VOSCommands)  │  │
│  │  • WebScrapingDatabase    - Web commands                     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            │ Forwards events
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      LearnAppIntegration                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Detection Layer                              │  │
│  │  • AppLaunchDetector      - Detects new app launches         │  │
│  │  • LearnedAppTracker      - Tracks learned apps              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    UI Layer                                   │  │
│  │  • ConsentDialogManager   - Asks user permission             │  │
│  │    └─ ConsentDialog       - WindowManager overlay (v1.0.5)   │  │
│  │  • ProgressOverlayManager - Shows learning progress          │  │
│  │  • LoginPromptOverlay     - Handles login screens            │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Exploration Layer                            │  │
│  │  • ExplorationEngine      - Orchestrates DFS exploration     │  │
│  │  • ScreenExplorer         - Explores individual screens      │  │
│  │  • ScreenStateManager     - Fingerprints screens             │  │
│  │  • ElementClassifier      - Classifies UI elements           │  │
│  │  • NavigationGraphBuilder - Builds app navigation graph      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   UUID Layer                                  │  │
│  │  • UUIDCreator            - Generates stable UUIDs           │  │
│  │  • ThirdPartyUuidGenerator- Third-party app UUIDs            │  │
│  │  • UuidAliasManager       - Manages UUID aliases             │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  Persistence Layer                            │  │
│  │  • LearnAppDatabase       - Learned app data                 │  │
│  │  • UUIDCreatorDatabase    - UUID mappings                    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

---

## 2. VoiceOSService Lifecycle Flow

### Service Initialization Sequence

```
┌─────────────┐
│  onCreate() │
└──────┬──────┘
       │
       ├─ 1. Call super.onCreate()
       ├─ 2. Set instance reference (WeakReference)
       ├─ 3. Initialize AppScrapingDatabase (Room)
       │     └─ FALLBACK: null if initialization fails
       │
       ▼
┌─────────────────────┐
│ onServiceConnected()│
└──────┬──────────────┘
       │
       ├─ 1. Load ServiceConfiguration from preferences
       ├─ 2. Configure service info (event types, flags)
       ├─ 3. Register ProcessLifecycleOwner observer (hybrid foreground)
       │
       ├─ 4. Launch initialization coroutine (serviceScope):
       │     │
       │     ├─ a. Load static commands from ActionCoordinator
       │     ├─ b. Observe installed apps → populate appsCommand cache
       │     ├─ c. Delay 200ms (don't block startup)
       │     │
       │     ├─ d. Initialize components (initializeComponents):
       │     │     ├─ ActionCoordinator.initialize()
       │     │     ├─ AccessibilityScrapingIntegration (if DB available)
       │     │     ├─ VoiceCommandProcessor (if DB available)
       │     │     └─ initializeVoiceRecognition()
       │     │           └─ SpeechEngineManager.initializeEngine(VIVOKA)
       │     │
       │     ├─ e. Initialize VoiceCursor (initializeVoiceCursor):
       │     │     └─ VoiceCursorAPI.initialize(this, this)
       │     │
       │     ├─ f. Initialize LearnApp (initializeLearnAppIntegration):
       │     │     ├─ UUIDCreator.initialize(context)
       │     │     └─ LearnAppIntegration.initialize(context, this)
       │     │           └─ Sets up event listeners (see Section 3)
       │     │
       │     ├─ g. Initialize CommandManager (initializeCommandManager):
       │     │     ├─ CommandManager.getInstance(context)
       │     │     ├─ ServiceMonitor.bindCommandManager()
       │     │     ├─ ServiceMonitor.startHealthCheck()
       │     │     └─ registerDatabaseCommands() (delay 500ms)
       │     │           ├─ Load from CommandDatabase (94 VOSCommands)
       │     │           ├─ Load from AppScrapingDatabase
       │     │           ├─ Load from WebScrapingDatabase
       │     │           └─ Register with SpeechEngineManager
       │     │
       │     └─ h. Register voice commands (registerVoiceCmd):
       │           └─ Periodic loop (500ms) to update speech engine
       │
       └─ Mark isServiceReady = true


┌─────────────────────────┐
│ onAccessibilityEvent()  │
└──────┬──────────────────┘
       │
       ├─ Check: isServiceReady? (skip if not)
       │
       ├─ 1. Forward to AccessibilityScrapingIntegration
       │     └─ Base UI scraping, element extraction
       │
       ├─ 2. Forward to LearnAppIntegration
       │     └─ AppLaunchDetector.onAccessibilityEvent()
       │           └─ Detects new app launches (see Section 3)
       │
       ├─ 3. Track event counts (performance monitoring)
       │
       ├─ 4. Get package name (event.packageName or rootInActiveWindow)
       │
       ├─ 5. Apply debouncing (Debouncer, 1000ms)
       │     └─ Key: "$packageName-$className-$eventType"
       │
       └─ 6. Process event by type:
             │
             ├─ TYPE_WINDOW_CONTENT_CHANGED:
             │   └─ Launch coroutine: UIScrapingEngine.extractUIElementsAsync()
             │         └─ Update commandCache, nodeCache
             │
             ├─ TYPE_WINDOW_STATE_CHANGED:
             │   └─ Launch coroutine: UIScrapingEngine.extractUIElementsAsync()
             │         └─ Update commandCache, nodeCache
             │
             └─ TYPE_VIEW_CLICKED:
                 └─ Launch coroutine: UIScrapingEngine.extractUIElementsAsync()
                       └─ Update commandCache, nodeCache


┌─────────────┐
│ onDestroy() │
└──────┬──────┘
       │
       ├─ 1. Cleanup AccessibilityScrapingIntegration
       ├─ 2. Cleanup VoiceCommandProcessor (clear reference)
       ├─ 3. Cleanup LearnAppIntegration
       ├─ 4. Cleanup VoiceCursorAPI
       ├─ 5. Cleanup UIScrapingEngine
       ├─ 6. Cancel coroutine scopes
       ├─ 7. Clear caches
       ├─ 8. Cleanup ServiceMonitor
       ├─ 9. Cleanup CommandManager
       └─ 10. Clear instance reference
```

### Key Initialization Dependencies

```
Dependency Chain:
1. AppScrapingDatabase (Room) ← Initialized in onCreate()
2. UUIDCreator ← Required by LearnAppIntegration
3. LearnAppIntegration ← Requires UUIDCreator + AccessibilityService
4. CommandManager ← Independent (can initialize in parallel)
5. SpeechEngineManager ← Can initialize early, needs commands later
```

---

## 3. LearnApp Integration Flow

### App Launch Detection & Consent Flow

```
┌──────────────────────────────────────────────────────────────────┐
│                    VoiceOSService                                 │
│                                                                    │
│  onAccessibilityEvent(event) ────────────────────┐               │
└───────────────────────────────────────────────────┼───────────────┘
                                                    │
                                                    │ Forwards event
                                                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                  LearnAppIntegration                              │
│                                                                    │
│  onAccessibilityEvent(event)                                      │
│    └─ AppLaunchDetector.onAccessibilityEvent(event)              │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                    AppLaunchDetector                              │
│                                                                    │
│  onAccessibilityEvent(event)                                      │
│    │                                                              │
│    ├─ Filter: TYPE_WINDOW_STATE_CHANGED only                     │
│    ├─ Get: packageName from event                                │
│    ├─ Check: Is VOS4 internal package? (skip if yes)             │
│    │                                                              │
│    └─ Query: LearnedAppTracker.isAppLearned(packageName)         │
│          │                                                        │
│          ├─ YES: Already learned → emit Nothing                  │
│          │                                                        │
│          └─ NO: Not learned → emit AppLaunchEvent.NewAppDetected │
│                 └─ Emits to Flow<AppLaunchEvent>                 │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ Flow emission
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│            LearnAppIntegration (Event Listener)                   │
│                                                                    │
│  setupEventListeners() - Launched in init{}:                      │
│    │                                                              │
│    └─ scope.launch {                                             │
│          appLaunchDetector.appLaunchEvents                       │
│            .debounce(500ms)         ← THROTTLE: 500ms silence    │
│            .distinctUntilChanged()  ← DEDUPE: same package       │
│            .flowOn(Dispatchers.Default)                          │
│            .collect { event ->                                   │
│                 when (event) {                                   │
│                   NewAppDetected(pkg, name) ->                   │
│                     consentDialogManager.showConsentDialog()     │
│                 }                                                │
│              }                                                   │
│       }                                                          │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ Shows dialog
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                  ConsentDialogManager                             │
│                                                                    │
│  showConsentDialog(packageName, appName)                          │
│    │                                                              │
│    ├─ Check: isDialogShowing? (skip if yes) ← PREVENTS DUPLICATE │
│    ├─ Check: learnedAppTracker.getDontAskAgain(pkg)? (skip)      │
│    │                                                              │
│    └─ withContext(Dispatchers.Main) {  ← SWITCH TO MAIN THREAD   │
│          consentDialog.show(                                     │
│            appName,                                              │
│            onApprove = { dontAskAgain ->                         │
│              // Emit ConsentResponse.Approved                    │
│            },                                                    │
│            onDecline = { dontAskAgain ->                         │
│              // Emit ConsentResponse.Declined                    │
│            }                                                     │
│          )                                                       │
│       }                                                          │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ Shows UI
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                     ConsentDialog (v1.0.5)                        │
│                                                                    │
│  show(appName, onApprove, onDecline)                              │
│    │                                                              │
│    └─ WidgetOverlayHelper.ensureMainThread {  ← THREAD SAFETY    │
│          // 1. Remove existing view                              │
│          // 2. Inflate layout_consent_dialog.xml                 │
│          // 3. Set up UI components (title, description, etc.)   │
│          // 4. Configure buttons:                                │
│          //    - Yes button → onApprove(dontAskAgain)            │
│          //    - No button → onDecline(dontAskAgain)             │
│          //                                                       │
│          // 5. Create WindowManager.LayoutParams:                │
│          //    - TYPE_ACCESSIBILITY_OVERLAY ← Android 8+         │
│          //    - FLAG_NOT_FOCUSABLE ← CRITICAL (v1.0.4)          │
│          //    - FLAG_NOT_TOUCH_MODAL                            │
│          //    - WIDTH: 600dp, HEIGHT: WRAP_CONTENT              │
│          //    - GRAVITY: CENTER                                 │
│          //                                                       │
│          // 6. windowManager.addView(customView, params)         │
│          //    └─ Wrapped in try-catch (v1.0.5)                  │
│       }                                                          │
│                                                                    │
│  ┌──────────────────────────────────────────────┐               │
│  │         USER INTERACTION                      │               │
│  │                                               │               │
│  │  Clicks "Yes" ──→ onApprove(dontAskAgain)    │               │
│  │  Clicks "No"  ──→ onDecline(dontAskAgain)    │               │
│  └──────────────────────────────────────────────┘               │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ User clicked "Yes"
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│            LearnAppIntegration (Consent Listener)                 │
│                                                                    │
│  setupEventListeners() - Launched in init{}:                      │
│    │                                                              │
│    └─ scope.launch {                                             │
│          consentDialogManager.consentResponses.collect {         │
│            when (response) {                                     │
│              Approved(pkg) -> startExploration(pkg)              │
│              Declined(pkg) -> // Do nothing                      │
│            }                                                     │
│          }                                                       │
│       }                                                          │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ Starts exploration
                                ▼
                          (See Section 5)
```

### Known Issues in This Flow

1. **FIXED (v1.0.5)**: BadTokenException when clicking "Yes"
   - **Root Cause**: Missing `FLAG_NOT_FOCUSABLE` + redundant `Handler.post()`
   - **Fix**: Added flag + removed Handler (synchronous execution)
   - **Status**: ✅ Complete, pending manual QA

2. **Event Throttling**: 500ms debounce + distinctUntilChanged
   - **Purpose**: Prevent dialog flickering from rapid events
   - **Status**: ✅ Working as designed

3. **Dialog Duplication Prevention**: `isDialogShowing` flag
   - **Purpose**: Prevent multiple consent dialogs
   - **Status**: ✅ Working as designed

---

## 4. Consent Dialog Flow (Fixed)

### ConsentDialog v1.0.5 Threading Model

```
┌─────────────────────────────────────────────────────────────────────┐
│                  ConsentDialogManager                                │
│                 (Caller - Any Thread)                                │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            │ withContext(Dispatchers.Main)
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  ConsentDialogManager                                │
│                   (Now on Main Thread)                               │
│                                                                       │
│  withContext(Dispatchers.Main) {                                     │
│    consentDialog.show(appName, onApprove, onDecline)                 │
│  }                                                                   │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            │ Calls show() - ALREADY ON MAIN THREAD
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     ConsentDialog                                    │
│                                                                       │
│  show(appName, onApprove, onDecline) {                               │
│    helper.ensureMainThread {  ← CHECK: Already on Main?             │
│      │                                                               │
│      ├─ IF already on Main:                                         │
│      │   └─ Execute IMMEDIATELY (no delay)  ✅ v1.0.5 FIX           │
│      │                                                               │
│      └─ IF on background thread:                                    │
│          └─ Post to Main thread handler                             │
│                                                                       │
│      // --- EXECUTED SYNCHRONOUSLY (if on Main) ---                 │
│                                                                       │
│      // 1. Remove existing view (if any)                            │
│      currentView?.let { windowManager.removeView(it) }              │
│                                                                       │
│      // 2. Inflate layout                                           │
│      val customView = LayoutInflater.from(context)                   │
│          .inflate(R.layout.layout_consent_dialog, null)             │
│                                                                       │
│      // 3. Configure UI                                             │
│      customView.findViewById<TextView>(R.id.title_text)             │
│          .text = "Learn $appName?"                                  │
│                                                                       │
│      // 4. Set up buttons                                           │
│      customView.findViewById<Button>(R.id.yes_button)               │
│          .setOnClickListener {                                      │
│            val dontAsk = customView.findViewById<CheckBox>          │
│                (R.id.dont_ask_again).isChecked                      │
│            onApprove(dontAsk)                                       │
│            dismiss()                                                │
│          }                                                          │
│                                                                       │
│      // 5. Create LayoutParams                                      │
│      val params = WindowManager.LayoutParams(                       │
│          600.dpToPx(),                                              │
│          WindowManager.LayoutParams.WRAP_CONTENT,                   │
│          WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,     │
│          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  ← v1.0.4 │
│            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,         │
│          PixelFormat.TRANSLUCENT                                    │
│      )                                                              │
│      params.gravity = Gravity.CENTER                                │
│                                                                       │
│      // 6. Add view to WindowManager                                │
│      try {                                                          │
│        windowManager.addView(customView, params) ← CRITICAL MOMENT  │
│        currentView = customView                                     │
│      } catch (e: Exception) {                                       │
│        Log.e(TAG, "Failed to add overlay view", e)                  │
│      }                                                              │
│    }                                                                │
│  }                                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### v1.0.5 Fix: Before vs After

#### BEFORE (v1.0.4 - BROKEN):

```
ConsentDialogManager.showConsentDialog()
  ↓
withContext(Dispatchers.Main) {              ← Thread: Main
  consentDialog.show()
    ↓
  Handler(Looper.getMainLooper()).post {     ← Thread: Main (again!)
    windowManager.addView()                  ← Thread: Main (LATER)
  }                                          ← Handler posts to queue
}                                            ← Coroutine COMPLETES
  ↓
[Handler executes AFTER coroutine released]  ← RACE CONDITION
  ↓
windowManager.addView() validates token       ← Context may be invalid
  ↓
❌ BadTokenException: "Unable to add window"
```

**Problem**: Redundant `Handler.post()` created timing window between coroutine completion and view addition.

#### AFTER (v1.0.5 - FIXED):

```
ConsentDialogManager.showConsentDialog()
  ↓
withContext(Dispatchers.Main) {              ← Thread: Main
  consentDialog.show()
    ↓
  WidgetOverlayHelper.ensureMainThread {     ← Check: on Main?
    // YES: Execute immediately (no Handler)  ← SYNCHRONOUS
    windowManager.addView()                  ← Thread: Main (NOW)
  }                                          ← Executes before return
}                                            ← Coroutine completes AFTER
  ↓
✅ View added successfully, context still valid
```

**Solution**: Removed `Handler.post()`, using `WidgetOverlayHelper.ensureMainThread()` which executes immediately if already on Main thread.

---

## 5. Exploration Engine Flow

### DFS Exploration Orchestration

```
┌─────────────────────────────────────────────────────────────────────┐
│                  LearnAppIntegration                                 │
│                                                                       │
│  startExploration(packageName)                                       │
│    ↓                                                                 │
│  1. Create exploration session:                                     │
│     repository.createExplorationSessionSafe(packageName)             │
│       ↓                                                              │
│       ├─ Check: Does LearnedApp exist?                              │
│       │   NO: Auto-create from PackageManager metadata              │
│       │   YES: Use existing                                         │
│       ↓                                                              │
│       └─ Create ExplorationSession entity                           │
│             └─ Returns: SessionCreationResult.Created(sessionId)    │
│                                                                       │
│  2. Start exploration engine:                                       │
│     explorationEngine.startExploration(packageName, sessionId)      │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    ExplorationEngine                                 │
│                                                                       │
│  startExploration(packageName, sessionId)                            │
│    ↓                                                                 │
│  1. Initialize components:                                          │
│     ├─ NavigationGraphBuilder(packageName)                          │
│     ├─ ScreenStateManager.clear()                                   │
│     ├─ Reset counters (dangerousElementsSkipped, loginScreens)      │
│     └─ Set startTimestamp                                           │
│                                                                       │
│  2. Emit initial state:                                             │
│     ExplorationState.Running(                                       │
│       packageName,                                                  │
│       progress = { screensExplored: 0, ... }                        │
│     )                                                               │
│                                                                       │
│  3. Get root node:                                                  │
│     val rootNode = accessibilityService.rootInActiveWindow          │
│     if (rootNode == null) → ExplorationState.Failed                 │
│                                                                       │
│  4. Start DFS exploration:                                          │
│     exploreScreenRecursive(rootNode, packageName, depth=0)          │
│       │                                                              │
│       └─ (See Recursive Exploration Flow below)                     │
│                                                                       │
│  5. On completion:                                                  │
│     ├─ Create ExplorationStats                                      │
│     └─ Emit: ExplorationState.Completed(stats)                      │
│                                                                       │
│  6. On error:                                                       │
│     └─ Emit: ExplorationState.Failed(error)                         │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│           Recursive Exploration Flow (DFS)                           │
│                                                                       │
│  exploreScreenRecursive(rootNode, packageName, depth)                │
│    │                                                                 │
│    ├─ 1. Check termination conditions:                              │
│    │     ├─ depth > maxDepth (default: 5)                           │
│    │     ├─ State is not Running (paused/stopped)                   │
│    │     └─ Return if any condition met                             │
│    │                                                                 │
│    ├─ 2. Get current screen fingerprint:                            │
│    │     screenFingerprint = ScreenStateManager.getCurrentState()   │
│    │       └─ Generates hash from UI hierarchy                      │
│    │                                                                 │
│    ├─ 3. Check if screen already visited:                           │
│    │     if (screenStateManager.hasVisitedScreen(fingerprint)) {    │
│    │       return // Skip already explored                          │
│    │     }                                                          │
│    │                                                                 │
│    ├─ 4. Mark screen as visited:                                    │
│    │     screenStateManager.recordVisit(fingerprint)                │
│    │                                                                 │
│    ├─ 5. Explore current screen:                                    │
│    │     screenExplorer.exploreScreen(rootNode)                     │
│    │       ↓                                                         │
│    │       ├─ Recursively traverse accessibility tree               │
│    │       ├─ For each node:                                        │
│    │       │   ├─ Classify element (ElementClassifier)              │
│    │       │   │   └─ Determines: button, text, image, etc.         │
│    │       │   │                                                     │
│    │       │   ├─ Check if dangerous (DangerousElementDetector)     │
│    │       │   │   └─ Skip: logout, delete, purchase buttons        │
│    │       │   │                                                     │
│    │       │   ├─ Check if login screen (LoginScreenDetector)       │
│    │       │   │   └─ If detected: pause exploration                │
│    │       │   │                                                     │
│    │       │   └─ Generate UUID (UUIDCreator)                       │
│    │       │       ├─ For VOS4 apps: Use UUIDCreator                │
│    │       │       └─ For 3rd party: Use ThirdPartyUuidGenerator    │
│    │       │                                                         │
│    │       ├─ Detect scrollable containers (ScrollDetector)         │
│    │       │   └─ For each scrollable: execute scroll + re-explore  │
│    │       │                                                         │
│    │       └─ Returns: List<ExploredElement>                        │
│    │                                                                 │
│    ├─ 6. Save screen to database (if sessionId provided):           │
│    │     repository.saveScreenState(sessionId, fingerprint, ...)    │
│    │                                                                 │
│    ├─ 7. Build navigation graph:                                    │
│    │     navigationGraphBuilder.addScreen(fingerprint)              │
│    │                                                                 │
│    ├─ 8. Update exploration progress:                               │
│    │     emit ExplorationState.Running(updated progress)            │
│    │                                                                 │
│    ├─ 9. For each interactable element:                             │
│    │     for (element in exploredElements) {                        │
│    │       if (element.isClickable && !element.isDangerous) {       │
│    │         │                                                       │
│    │         ├─ a. Click element (performAction(CLICK))             │
│    │         ├─ b. Wait for screen stabilization (500ms)            │
│    │         ├─ c. Get new root node                                │
│    │         ├─ d. Check if screen changed                          │
│    │         │                                                       │
│    │         ├─ e. If changed:                                      │
│    │         │     ├─ Add edge to navigation graph                  │
│    │         │     └─ RECURSE: exploreScreenRecursive(              │
│    │         │           newRoot, packageName, depth+1              │
│    │         │         )                                            │
│    │         │                                                       │
│    │         └─ f. Navigate back (GLOBAL_ACTION_BACK)               │
│    │               └─ Wait for screen stabilization                 │
│    │       }                                                         │
│    │     }                                                          │
│    │                                                                 │
│    └─ 10. Return (backtrack to parent screen)                       │
└─────────────────────────────────────────────────────────────────────┘
```

### Exploration State Machine

```
┌─────────┐
│  Idle   │ ← Initial state
└────┬────┘
     │
     │ startExploration()
     ▼
┌──────────────┐
│   Running    │ ← Active exploration
│              │   - Update progress
│              │   - Emit state changes
└─┬──────────┬─┘
  │          │
  │          │ Login detected
  │          ▼
  │     ┌──────────────────┐
  │     │ PausedForLogin   │ ← Show LoginPromptOverlay
  │     │                  │   - User can skip/continue
  │     └─────────┬────────┘
  │               │
  │               │ resumeExploration()
  │               └──────────┐
  │                          │
  │ pauseExploration()       │
  ▼                          │
┌──────────────┐             │
│ PausedByUser │             │
└──────┬───────┘             │
       │                     │
       │ resumeExploration() │
       └─────────────────────┘
                │
                │ Exploration complete
                ▼
           ┌──────────┐
           │Completed │ ← Save stats, show notification
           └──────────┘
                │
                │ Error occurred
                ▼
           ┌──────────┐
           │  Failed  │ ← Show error, cleanup
           └──────────┘
```

---

## 6. Event Processing Flow

### Event Flow Through System

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ANDROID SYSTEM                                  │
│                                                                       │
│  App UI changes → AccessibilityEvent generated                       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ System delivers event
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  VoiceOSService.onAccessibilityEvent()               │
│                                                                       │
│  Receives: AccessibilityEvent                                        │
│    - eventType (e.g., TYPE_WINDOW_STATE_CHANGED)                    │
│    - packageName (e.g., "com.instagram.android")                    │
│    - className (e.g., "android.widget.Button")                      │
│    - source (AccessibilityNodeInfo tree)                            │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ├─────────────────┐
                                │                 │
                                ▼                 ▼
┌─────────────────────────────────────┐  ┌──────────────────────────┐
│  AccessibilityScrapingIntegration   │  │  LearnAppIntegration     │
│                                     │  │                          │
│  onAccessibilityEvent(event)        │  │  onAccessibilityEvent()  │
│    ↓                                │  │    ↓                     │
│  1. Filter event types              │  │  Forward to:             │
│  2. Get package name                │  │  AppLaunchDetector       │
│  3. Check if VOS4 app               │  │    ↓                     │
│  4. Get root node                   │  │  Detect new app launch   │
│  5. Scrape UI hierarchy             │  │  Emit events             │
│  6. Generate element hashes         │  │                          │
│  7. Save to AppScrapingDatabase     │  │                          │
│  8. Generate voice commands         │  │                          │
│  9. Save to CommandDatabase         │  │                          │
└─────────────────────────────────────┘  └──────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│           VoiceOSService Event Processing (Detailed)                 │
│                                                                       │
│  onAccessibilityEvent(event)                                         │
│    │                                                                 │
│    ├─ 1. Check: isServiceReady?                                     │
│    │     └─ NO: Skip event (return early)                           │
│    │                                                                 │
│    ├─ 2. Forward to AccessibilityScrapingIntegration:               │
│    │     scrapingIntegration?.onAccessibilityEvent(event)            │
│    │       └─ (Wrapped in try-catch, logs errors)                   │
│    │                                                                 │
│    ├─ 3. Forward to LearnAppIntegration:                            │
│    │     learnAppIntegration?.onAccessibilityEvent(event)            │
│    │       └─ (Wrapped in try-catch, logs errors)                   │
│    │                                                                 │
│    ├─ 4. Track event count (performance monitoring)                 │
│    │                                                                 │
│    ├─ 5. Get package name:                                          │
│    │     packageName = event.packageName                            │
│    │       OR rootInActiveWindow?.packageName                       │
│    │     if (packageName == null) return                            │
│    │                                                                 │
│    ├─ 6. Create debounce key:                                       │
│    │     key = "$packageName-$className-$eventType"                 │
│    │                                                                 │
│    ├─ 7. Check debouncer (1000ms):                                  │
│    │     if (!eventDebouncer.shouldProceed(key)) {                  │
│    │       Log: "Event debounced"                                   │
│    │       return                                                   │
│    │     }                                                          │
│    │                                                                 │
│    └─ 8. Process by event type:                                     │
│          │                                                           │
│          ├─ TYPE_WINDOW_CONTENT_CHANGED:                            │
│          │   └─ serviceScope.launch {                               │
│          │         val commands = uiScrapingEngine                   │
│          │             .extractUIElementsAsync(event)                │
│          │         commandCache.clear()                             │
│          │         commandCache.addAll(commands.map { it.text })    │
│          │       }                                                  │
│          │                                                           │
│          ├─ TYPE_WINDOW_STATE_CHANGED:                              │
│          │   └─ serviceScope.launch {                               │
│          │         val commands = uiScrapingEngine                   │
│          │             .extractUIElementsAsync(event)                │
│          │         commandCache.clear()                             │
│          │         commandCache.addAll(commands.map { it.text })    │
│          │       }                                                  │
│          │                                                           │
│          └─ TYPE_VIEW_CLICKED:                                      │
│              └─ serviceScope.launch {                               │
│                    val commands = uiScrapingEngine                   │
│                        .extractUIElementsAsync(event)                │
│                    commandCache.clear()                             │
│                    commandCache.addAll(commands.map { it.text })    │
│                  }                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Event Debouncing Strategy

```
Purpose: Prevent excessive processing for apps generating 10+ events/sec

┌────────────────────────────────────────────────────────────────┐
│                      Debouncer                                  │
│                                                                  │
│  shouldProceed(key: String): Boolean                            │
│    │                                                            │
│    ├─ Check: lastEventTime[key]                                │
│    │                                                            │
│    ├─ If (now - lastEventTime < DEBOUNCE_MS):                  │
│    │   └─ return false  // Too soon, skip                      │
│    │                                                            │
│    └─ Else:                                                    │
│        ├─ lastEventTime[key] = now                             │
│        └─ return true  // Proceed                              │
│                                                                  │
│  Configuration:                                                 │
│    - EVENT_DEBOUNCE_MS = 1000ms (VoiceOSService)               │
│    - AppLaunch debounce = 500ms (LearnAppIntegration Flow)     │
└────────────────────────────────────────────────────────────────┘

Example Timeline:

  0ms: Event A → Process ✅
100ms: Event A → Skip (< 1000ms)
200ms: Event A → Skip (< 1000ms)
500ms: Event B → Process ✅ (different key)
900ms: Event A → Skip (< 1000ms)
1100ms: Event A → Process ✅ (>= 1000ms since last)
```

---

## 7. Command Execution Flow (3-Tier)

### Voice Command Processing Pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│                   USER SPEAKS COMMAND                                │
│                  "click settings button"                             │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ Microphone audio
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  SpeechEngineManager                                 │
│                    (Vivoka Engine)                                   │
│                                                                       │
│  1. Capture audio from microphone                                   │
│  2. Process with Vivoka engine                                      │
│  3. Generate transcript + confidence score                          │
│  4. Emit SpeechState:                                               │
│       - fullTranscript = "click settings button"                    │
│       - confidence = 0.92                                           │
│       - isListening = true                                          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ StateFlow emission
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│       VoiceOSService.initializeVoiceRecognition()                    │
│                                                                       │
│  serviceScope.launch {                                              │
│    speechEngineManager.speechState.collectLatest { state ->         │
│      if (state.confidence > 0 && state.fullTranscript.isNotBlank()) {
│        handleVoiceCommand(                                          │
│          confidence = state.confidence,  // 0.92                    │
│          command = state.fullTranscript  // "click settings button" │
│        )                                                            │
│      }                                                              │
│    }                                                                │
│  }                                                                   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│          VoiceOSService.handleVoiceCommand()                         │
│                                                                       │
│  handleVoiceCommand(command, confidence)                             │
│    │                                                                 │
│    ├─ 1. Reject low confidence (< 0.5):                             │
│    │     if (confidence < 0.5) return                               │
│    │                                                                 │
│    ├─ 2. Normalize command:                                         │
│    │     normalized = command.lowercase().trim()                    │
│    │                                                                 │
│    ├─ 3. Get current package:                                       │
│    │     currentPackage = rootInActiveWindow?.packageName           │
│    │                                                                 │
│    ├─ 4. Check if BROWSER (WEB TIER):                               │
│    │     if (webCommandCoordinator.isCurrentAppBrowser(pkg)) {      │
│    │       serviceScope.launch {                                    │
│    │         val handled = webCommandCoordinator                    │
│    │             .processWebCommand(normalized, pkg)                │
│    │         if (handled) return@launch  // Done                    │
│    │         else handleRegularCommand()  // Fall through           │
│    │       }                                                        │
│    │       return                                                   │
│    │     }                                                          │
│    │                                                                 │
│    └─ 5. Not browser → Regular command tiers:                       │
│          handleRegularCommand(normalized, confidence)               │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│          VoiceOSService.handleRegularCommand()                       │
│                                                                       │
│  handleRegularCommand(normalized, confidence)                        │
│    │                                                                 │
│    ├─ TIER 1: CommandManager (PRIMARY)                              │
│    │   │                                                            │
│    │   ├─ Check: Is CommandManager available AND not in fallback?  │
│    │   │   NO: Skip to Tier 2                                      │
│    │   │                                                            │
│    │   └─ YES: serviceScope.launch {                               │
│    │         │                                                      │
│    │         ├─ Create Command object:                             │
│    │         │   Command(                                          │
│    │         │     id = normalized,                                │
│    │         │     text = normalized,                              │
│    │         │     source = VOICE,                                 │
│    │         │     context = createCommandContext(),               │
│    │         │     confidence = confidence,                        │
│    │         │     timestamp = now                                 │
│    │         │   )                                                 │
│    │         │                                                      │
│    │         ├─ Execute:                                           │
│    │         │   val result = commandManager.executeCommand(cmd)   │
│    │         │                                                      │
│    │         └─ Handle result:                                     │
│    │             if (result.success) {                             │
│    │               Log: "✓ Tier 1 SUCCESS"                         │
│    │               return@launch  // DONE                          │
│    │             } else {                                          │
│    │               Log: "Tier 1 FAILED, fall to Tier 2"            │
│    │               executeTier2Command(normalized)                 │
│    │             }                                                 │
│    │       }                                                        │
│    │                                                                 │
│    ├─ TIER 2: VoiceCommandProcessor (SECONDARY)                     │
│    │   │                                                            │
│    │   └─ suspend fun executeTier2Command(normalized)              │
│    │         │                                                      │
│    │         ├─ Check: Is VoiceCommandProcessor available?         │
│    │         │   NO: Skip to Tier 3                                │
│    │         │                                                      │
│    │         └─ YES:                                               │
│    │             val result = voiceCommandProcessor                │
│    │                 .processCommand(normalized)                   │
│    │             │                                                  │
│    │             └─ Handle result:                                 │
│    │                 if (result.success) {                         │
│    │                   Log: "✓ Tier 2 SUCCESS"                     │
│    │                   return  // DONE                             │
│    │                 } else {                                      │
│    │                   Log: "Tier 2 FAILED, fall to Tier 3"        │
│    │                   executeTier3Command(normalized)             │
│    │                 }                                             │
│    │                                                                 │
│    └─ TIER 3: ActionCoordinator (TERTIARY/FALLBACK)                 │
│        │                                                            │
│        └─ suspend fun executeTier3Command(normalized)              │
│              │                                                      │
│              └─ actionCoordinator.executeAction(normalized)        │
│                    └─ (Legacy handlers, always executes)            │
└─────────────────────────────────────────────────────────────────────┘
```

### 3-Tier Command Routing Decision Tree

```
                      ┌───────────────────┐
                      │  Voice Command    │
                      │   Received        │
                      └─────────┬─────────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
                ▼                               ▼
        ┌──────────────┐                ┌──────────────┐
        │ Is Browser?  │                │  Is VOS4?    │
        └──────┬───────┘                └──────┬───────┘
               │                               │
          YES  │  NO                      YES  │  NO
               │                               │
               ▼                               ▼
       ┌──────────────┐              ┌──────────────────┐
       │  WEB TIER    │              │   Regular Tiers   │
       │ WebCommand   │              │   (1→2→3)         │
       │ Coordinator  │              └──────────────────┘
       └──────────────┘
               │
               │ Handled?
               ├─ YES → DONE ✅
               └─ NO  → Fall to Tier 1


┌─────────────────────────────────────────────────────────────────────┐
│                    TIER 1: CommandManager                            │
│                                                                       │
│  Purpose: Route to appropriate handlers                             │
│  Sources:                                                            │
│    • CommandDatabase (94 VOSCommand entries)                        │
│    • Registered command handlers                                    │
│    • Macro system                                                   │
│                                                                       │
│  Examples:                                                           │
│    • "go home" → GLOBAL_ACTION_HOME                                 │
│    • "back" → GLOBAL_ACTION_BACK                                    │
│    • "scroll down" → Scroll handler                                 │
│                                                                       │
│  Result:                                                             │
│    ├─ SUCCESS → DONE ✅                                             │
│    └─ FAILURE → Fall to Tier 2                                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ FAILED
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  TIER 2: VoiceCommandProcessor                       │
│                                                                       │
│  Purpose: App-specific learned commands                             │
│  Sources:                                                            │
│    • AppScrapingDatabase (generated commands)                       │
│    • WebScrapingDatabase (web commands)                             │
│    • LearnApp exploration results                                   │
│                                                                       │
│  Examples:                                                           │
│    • "click settings" → Click button with UUID                      │
│    • "open profile" → Navigate to profile screen                    │
│    • "search" → Click search icon                                   │
│                                                                       │
│  Lookup:                                                             │
│    1. Hash command text                                             │
│    2. Query AppScrapingDatabase by hash                             │
│    3. If found: Get element UUID                                    │
│    4. Find node by UUID                                             │
│    5. Perform action (click, etc.)                                  │
│                                                                       │
│  Result:                                                             │
│    ├─ SUCCESS → DONE ✅                                             │
│    └─ FAILURE → Fall to Tier 3                                      │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ FAILED
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   TIER 3: ActionCoordinator                          │
│                                                                       │
│  Purpose: Legacy handler-based commands (always tries)              │
│  Sources:                                                            │
│    • Hardcoded handlers (GazeHandler, CursorHandler, etc.)          │
│    • Pattern matching on command text                               │
│    • Basic UI interaction fallbacks                                 │
│                                                                       │
│  Examples:                                                           │
│    • "show cursor" → VoiceCursor.showCursor()                       │
│    • "click" → Click at cursor position                             │
│    • "look at that" → Gaze handler                                  │
│                                                                       │
│  Result:                                                             │
│    • Always executes (may or may not succeed)                       │
│    • Logs execution attempt                                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. Database Architecture

### Three Database Systems

```
┌─────────────────────────────────────────────────────────────────────┐
│                  DATABASE ARCHITECTURE                               │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         1. AppScrapingDatabase (Room)                         │  │
│  │            VOS4 Native App Learning                           │  │
│  │                                                               │  │
│  │  Tables:                                                      │  │
│  │    • ScrapedElementEntity                                     │  │
│  │        - elementHash (PK, String) - SHA-256 of element        │  │
│  │        - packageName (FK)                                     │  │
│  │        - className                                            │  │
│  │        - resourceId                                           │  │
│  │        - contentDescription                                   │  │
│  │        - text                                                 │  │
│  │        - bounds (Rect)                                        │  │
│  │        - isClickable, isScrollable, etc.                      │  │
│  │                                                               │  │
│  │    • GeneratedCommandEntity                                   │  │
│  │        - commandHash (PK, String)                             │  │
│  │        - elementHash (FK → ScrapedElementEntity)              │  │
│  │        - commandText ("click settings")                       │  │
│  │        - synonyms (JSON array)                                │  │
│  │        - actionType (click, scroll, etc.)                     │  │
│  │        - confidence (0.0-1.0)                                 │  │
│  │                                                               │  │
│  │    • AppMetadataEntity                                        │  │
│  │        - packageName (PK)                                     │  │
│  │        - appName                                              │  │
│  │        - versionCode, versionName                             │  │
│  │        - firstScrapedTimestamp                                │  │
│  │        - lastScrapedTimestamp                                 │  │
│  │                                                               │  │
│  │  DAOs:                                                        │  │
│  │    • ScrapedElementDao                                        │  │
│  │    • GeneratedCommandDao                                      │  │
│  │    • AppMetadataDao                                           │  │
│  │                                                               │  │
│  │  Usage:                                                       │  │
│  │    • AccessibilityScrapingIntegration writes to this          │  │
│  │    • VoiceCommandProcessor reads from this                    │  │
│  │    • Used for VOS4 apps (com.augmentalis.*)                   │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         2. LearnAppDatabase (Room)                            │  │
│  │            Third-Party App Learning                           │  │
│  │                                                               │  │
│  │  Tables:                                                      │  │
│  │    • LearnedAppEntity                                         │  │
│  │        - packageName (PK)                                     │  │
│  │        - appName                                              │  │
│  │        - versionCode, versionName                             │  │
│  │        - learningStatus (NOT_LEARNED, IN_PROGRESS, LEARNED)   │  │
│  │        - totalScreens, totalElements                          │  │
│  │        - explorationStartTime, explorationEndTime             │  │
│  │        - dontAskAgain (boolean)                               │  │
│  │                                                               │  │
│  │    • ExplorationSessionEntity                                 │  │
│  │        - sessionId (PK, String UUID)                          │  │
│  │        - packageName (FK → LearnedAppEntity)                  │  │
│  │        - startTimestamp, endTimestamp                         │  │
│  │        - status (IN_PROGRESS, COMPLETED, FAILED)              │  │
│  │        - screensExplored, elementsDiscovered                  │  │
│  │        - dangerousElementsSkipped, loginScreensDetected       │  │
│  │                                                               │  │
│  │    • ScreenStateEntity                                        │  │
│  │        - screenHash (PK, String) - Fingerprint                │  │
│  │        - sessionId (FK → ExplorationSessionEntity)            │  │
│  │        - packageName                                          │  │
│  │        - activityName                                         │  │
│  │        - viewHierarchyHash                                    │  │
│  │        - timestamp                                            │  │
│  │        - depth (exploration depth)                            │  │
│  │        - elementsJson (JSON array of UUIDs)                   │  │
│  │                                                               │  │
│  │    • NavigationEdgeEntity                                     │  │
│  │        - edgeId (PK, generated)                               │  │
│  │        - sessionId (FK)                                       │  │
│  │        - fromScreenHash (FK → ScreenStateEntity)              │  │
│  │        - toScreenHash (FK → ScreenStateEntity)                │  │
│  │        - triggerElementUuid (element that caused navigation)  │  │
│  │        - actionType (click, scroll, swipe)                    │  │
│  │                                                               │  │
│  │  DAOs:                                                        │  │
│  │    • LearnAppDao (all CRUD operations)                        │  │
│  │                                                               │  │
│  │  Usage:                                                       │  │
│  │    • LearnAppIntegration writes to this                       │  │
│  │    • ExplorationEngine writes screen states + edges           │  │
│  │    • Used for third-party apps                                │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         3. CommandDatabase (Room)                             │  │
│  │            Voice Command Registry                             │  │
│  │                                                               │  │
│  │  Tables:                                                      │  │
│  │    • VoiceCommandEntity (from VOSCommandIngestion)            │  │
│  │        - commandId (PK)                                       │  │
│  │        - primaryText ("go home")                              │  │
│  │        - synonyms (JSON array: ["home", "main screen"])       │  │
│  │        - actionType (GLOBAL_ACTION, HANDLER, etc.)            │  │
│  │        - handlerClass (e.g., "NavigationHandler")             │  │
│  │        - locale (e.g., "en_US")                               │  │
│  │        - category (NAVIGATION, SYSTEM, etc.)                  │  │
│  │        - priority (1-10)                                      │  │
│  │                                                               │  │
│  │  DAOs:                                                        │  │
│  │    • VoiceCommandDao                                          │  │
│  │        - getCommandsForLocale(locale): List<VoiceCommand>     │  │
│  │        - getAllCommands(): List<VoiceCommand>                 │  │
│  │                                                               │  │
│  │  Data Source:                                                 │  │
│  │    • VOSCommandIngestion (CommandManager module)              │  │
│  │    • 94 pre-defined voice commands                            │  │
│  │    • Loaded via commandManagerInstance.initialize()           │  │
│  │                                                               │  │
│  │  Usage:                                                       │  │
│  │    • VoiceOSService.registerDatabaseCommands() reads          │  │
│  │    • CommandManager routes commands using this                │  │
│  │    • Speech engine vocabulary populated from this             │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                  UUIDCreator Integration                             │
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │         UUIDCreatorDatabase (Room)                            │  │
│  │                                                               │  │
│  │  Tables:                                                      │  │
│  │    • UuidMappingEntity                                        │  │
│  │        - uuid (PK, String)                                    │  │
│  │        - packageName                                          │  │
│  │        - resourceId                                           │  │
│  │        - elementHash (SHA-256)                                │  │
│  │        - createdTimestamp                                     │  │
│  │        - elementType (button, text, etc.)                     │  │
│  │                                                               │  │
│  │    • UuidAliasEntity                                          │  │
│  │        - aliasId (PK)                                         │  │
│  │        - primaryUuid (FK → UuidMappingEntity)                 │  │
│  │        - aliasUuid (alternative UUID)                         │  │
│  │        - reason (version_change, layout_update, etc.)         │  │
│  │                                                               │  │
│  │  Usage:                                                       │  │
│  │    • UUIDCreator generates stable UUIDs for VOS4 apps         │  │
│  │    • ThirdPartyUuidGenerator generates UUIDs for 3rd party    │  │
│  │    • ExplorationEngine writes UUID mappings during learning   │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### Database Interaction Flow

```
┌──────────────────────┐
│  Accessibility Event │
└──────────┬───────────┘
           │
           ├──────────────────┬──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌──────────────────┐  ┌──────────────┐  ┌──────────────┐
│ VOS4 App?        │  │ 3rd Party?   │  │ Browser?     │
│                  │  │              │  │              │
│ YES:             │  │ YES:         │  │ YES:         │
│ AccessibilityScraping  │ LearnApp     │  │ WebCommand   │
│ Integration      │  │ Integration  │  │ Coordinator  │
└─────────┬────────┘  └──────┬───────┘  └──────┬───────┘
          │                  │                  │
          │ Scrape UI        │ If consent       │ Scrape web
          ▼                  │ granted:         ▼
┌──────────────────┐         │ Start exploration┌──────────────┐
│ AppScrapingDB    │         │                  │ WebScrapingDB│
│                  │         ▼                  │              │
│ • ScrapedElement │ ┌──────────────┐          │ • WebCommand │
│ • Generated      │ │ LearnAppDB   │          │ • WebElement │
│   Command        │ │              │          └──────────────┘
└──────────────────┘ │ • LearnedApp │
                     │ • Exploration│
          ┌──────────│   Session    │
          │          │ • ScreenState│
          │          │ • Navigation │
          │          │   Edge       │
          │          └──────────────┘
          │                  │
          │                  │ Generate UUIDs
          │                  ▼
          │          ┌──────────────────┐
          │          │ UUIDCreatorDB    │
          │          │                  │
          │          │ • UuidMapping    │
          │          │ • UuidAlias      │
          │          └──────────────────┘
          │
          │ Register commands
          ▼
┌──────────────────────────────────────────┐
│     Speech Engine Vocabulary             │
│                                          │
│  Sources:                                │
│    1. CommandDatabase (94 commands)      │
│    2. AppScrapingDB (generated commands) │
│    3. WebScrapingDB (web commands)       │
│    4. Static commands (ActionCoordinator)│
│                                          │
│  Total: 300+ voice commands              │
└──────────────────────────────────────────┘
```

---

## 9. Threading Model

### Thread Pools and Dispatchers

```
┌─────────────────────────────────────────────────────────────────────┐
│                    VoiceOSService Threading                          │
│                                                                       │
│  CoroutineScope Hierarchy:                                          │
│                                                                       │
│  1. serviceScope = Dispatchers.Main + SupervisorJob()               │
│     Purpose: Main service operations, UI updates                    │
│     Used for:                                                       │
│       • Component initialization                                    │
│       • Event processing (launch coroutines)                        │
│       • UI scraping (launches on Default)                           │
│       • Command execution                                           │
│                                                                       │
│  2. coroutineScopeCommands = Dispatchers.IO + SupervisorJob()       │
│     Purpose: Command processing loop                                │
│     Used for:                                                       │
│       • registerVoiceCmd() - Periodic update loop (500ms)           │
│       • Updates speech engine with new commands                     │
│                                                                       │
│  3. Injected component scopes (internal):                           │
│     • UIScrapingEngine - Internal executor for parallel processing  │
│     • SpeechEngineManager - Internal for audio processing           │
│     • ActionCoordinator - Uses serviceScope                         │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                LearnAppIntegration Threading                         │
│                                                                       │
│  CoroutineScope:                                                    │
│    scope = Dispatchers.Default + SupervisorJob()                    │
│                                                                       │
│  Flow Collection:                                                   │
│    • appLaunchDetector.appLaunchEvents                              │
│        .debounce(500ms)                                             │
│        .distinctUntilChanged()                                      │
│        .flowOn(Dispatchers.Default)  ← Background processing        │
│        .collect { ... }                                             │
│                                                                       │
│    • consentDialogManager.consentResponses                          │
│        .collect { ... }                                             │
│                                                                       │
│    • explorationEngine.explorationState                             │
│        .collect { ... }                                             │
│                                                                       │
│  Thread Switches:                                                   │
│    • Event processing: Default                                      │
│    • Show consent dialog: withContext(Main)                         │
│    • Database operations: IO (Room handles internally)              │
│    • Exploration: Default (ExplorationEngine scope)                 │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                 ConsentDialog Threading (v1.0.5)                     │
│                                                                       │
│  Caller → ConsentDialogManager:                                     │
│    Thread: Any (typically Default from Flow)                        │
│    │                                                                 │
│    └─ withContext(Dispatchers.Main) {                               │
│         consentDialog.show(...)  ← Now on Main thread               │
│       }                                                             │
│                                                                       │
│  ConsentDialog.show():                                              │
│    Thread: Main (from caller's withContext)                         │
│    │                                                                 │
│    └─ WidgetOverlayHelper.ensureMainThread {                        │
│         // Check: Already on Main? YES → Execute immediately        │
│         windowManager.addView(...)  ← Synchronous, no Handler      │
│       }                                                             │
│                                                                       │
│  KEY FIX:                                                           │
│    ❌ Before (v1.0.4): Handler.post() → Async execution → Race      │
│    ✅ After (v1.0.5): Immediate execution → No race condition       │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                ExplorationEngine Threading                           │
│                                                                       │
│  CoroutineScope:                                                    │
│    scope = Dispatchers.Default + SupervisorJob()                    │
│                                                                       │
│  Exploration Loop:                                                  │
│    • exploreScreenRecursive() - Runs on Default                     │
│    • Database writes - IO (Room)                                    │
│    • UI actions (clicks) - Main (via AccessibilityService)          │
│    • State emissions - Collected on Default                         │
│                                                                       │
│  Thread Switch Pattern:                                             │
│    scope.launch(Dispatchers.Default) {                              │
│      // Exploration logic                                           │
│      withContext(Dispatchers.Main) {                                │
│        // Perform UI action (click)                                 │
│      }                                                              │
│      withContext(Dispatchers.IO) {                                  │
│        // Save to database                                          │
│      }                                                              │
│    }                                                                │
└─────────────────────────────────────────────────────────────────────┘
```

### Thread Safety Issues & Solutions

```
┌─────────────────────────────────────────────────────────────────────┐
│                  ISSUE 1: ConsentDialog Race Condition               │
│                           (FIXED in v1.0.5)                          │
│                                                                       │
│  Problem:                                                            │
│    Handler.post() created timing window between coroutine           │
│    completion and WindowManager.addView() execution.                │
│                                                                       │
│  Solution:                                                           │
│    Removed Handler, using WidgetOverlayHelper.ensureMainThread()    │
│    which executes immediately if already on Main thread.            │
│                                                                       │
│  Status: ✅ FIXED                                                    │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                  ISSUE 2: Event Flooding                             │
│                    (MITIGATED via Debouncer)                         │
│                                                                       │
│  Problem:                                                            │
│    Apps generating 10+ events/sec could flood event processing.     │
│                                                                       │
│  Solution:                                                           │
│    1. Debouncer (1000ms) in VoiceOSService                          │
│    2. Flow.debounce(500ms) in LearnAppIntegration                   │
│    3. distinctUntilChanged() to filter duplicate packages           │
│                                                                       │
│  Status: ✅ WORKING                                                  │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│              ISSUE 3: Multiple Consent Dialogs                       │
│                  (MITIGATED via Flag)                                │
│                                                                       │
│  Problem:                                                            │
│    Rapid events could trigger multiple consent dialogs for same app.│
│                                                                       │
│  Solution:                                                           │
│    1. isDialogShowing flag in ConsentDialogManager                  │
│    2. distinctUntilChanged() in Flow pipeline                       │
│    3. dontAskAgain persistence in LearnedAppTracker                 │
│                                                                       │
│  Status: ✅ WORKING                                                  │
└─────────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│           POTENTIAL ISSUE: Database Write Contention                 │
│                        (Room Handles)                                │
│                                                                       │
│  Scenario:                                                           │
│    Multiple coroutines writing to database simultaneously.          │
│                                                                       │
│  Mitigation:                                                         │
│    • Room uses internal transaction handling                        │
│    • WAL mode enabled for concurrent reads                          │
│    • Coroutines use Dispatchers.IO for DB operations                │
│                                                                       │
│  Status: ⚠️ Monitor for issues                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 10. Known Issues & Problem Areas

### Current Known Issues

#### 1. ConsentDialog BadTokenException (FIXED ✅)

**Status**: Fixed in v1.0.5, pending manual QA
**Severity**: Critical (was blocking 70%+ of users)
**Component**: `LearnApp/ui/ConsentDialog.kt`

**Root Causes** (Both Fixed):
1. Missing `FLAG_NOT_FOCUSABLE` window flag
2. Redundant `Handler.post()` threading causing race condition

**Fix Applied**:
- v1.0.4: Added `FLAG_NOT_FOCUSABLE` flag
- v1.0.5: Removed `Handler`, using `WidgetOverlayHelper.ensureMainThread()`

**Testing Required**:
- [ ] Install APK on Android 8.0+ device
- [ ] Enable VoiceOS accessibility service
- [ ] Open new third-party app
- [ ] Click "Yes" on consent dialog → Should NOT crash
- [ ] Verify learning starts
- [ ] Test on Android 8, 10, 13, 14

**Location**: `docs/modules/learnapp/LearnApp-ConsentDialog-Optimum-Fix-251028-0230.md`

---

#### 2. Exploration Engine Login Screen Detection

**Status**: Implemented but untested
**Severity**: Medium (affects app learning quality)
**Component**: `LearnApp/exploration/ExplorationEngine.kt`

**Issue**:
- Login screens pause exploration
- `LoginPromptOverlay` shows user prompt
- User can skip/continue
- **BUT**: State detection accuracy unknown

**Affected Flow**:
```
ExplorationEngine.exploreScreenRecursive()
  → ScreenExplorer.exploreScreen()
    → LoginScreenDetector.detectLoginScreen()
      → IF detected: Emit ExplorationState.PausedForLogin
        → LearnAppIntegration shows LoginPromptOverlay
          → User clicks "Skip" → resumeExploration()
          → User clicks "Continue" → Wait for manual login
```

**Testing Required**:
- [ ] Test with apps requiring login (Instagram, Facebook, etc.)
- [ ] Verify login screen detection accuracy
- [ ] Test skip functionality
- [ ] Test continue → manual login → resume flow
- [ ] Check if exploration resumes correctly

**Potential Problems**:
- False positives (non-login screens detected as login)
- False negatives (login screens not detected)
- State not properly restored after resume

---

#### 3. Dangerous Element Detection

**Status**: Implemented but conservative
**Severity**: Medium (affects exploration coverage)
**Component**: `LearnApp/elements/DangerousElementDetector.kt`

**Issue**:
- Currently skips elements matching patterns:
  - "logout", "log out", "sign out"
  - "delete", "remove"
  - "purchase", "buy", "checkout"
  - "uninstall"
- **May be too conservative** → Skip valid exploration paths

**Affected Flow**:
```
ElementClassifier.classifyElement()
  → DangerousElementDetector.isDangerous(element)
    → IF dangerous: Mark element, skip clicking
      → Increment dangerousElementsSkipped counter
```

**Testing Required**:
- [ ] Review logs for dangerousElementsSkipped counts
- [ ] Check if legitimate UI is being skipped
- [ ] Test apps with "Delete Email", "Remove Photo" (non-destructive)
- [ ] Verify actual dangerous actions are blocked

**Potential Improvements**:
- Context-aware detection (e.g., "Delete Email" in draft vs "Delete Account")
- User confirmation for borderline cases
- Machine learning classifier

---

#### 4. Database Registration Timing

**Status**: Potential race condition
**Severity**: Low (gracefully degrades)
**Component**: `VoiceOSService.initializeCommandManager()`

**Issue**:
- `registerDatabaseCommands()` has 500ms delay
- Commands may not be available immediately after service start
- Speech engine might miss early voice commands

**Flow**:
```
VoiceOSService.onServiceConnected()
  → initializeCommandManager()
    → delay(500ms)  ← DELAY HERE
    → registerDatabaseCommands()
      → Load from CommandDatabase (94 commands)
      → Load from AppScrapingDatabase
      → Load from WebScrapingDatabase
      → speechEngineManager.updateCommands()
```

**Symptoms**:
- First voice command after service start might not work
- User says command too soon → Not recognized
- After 500ms: Commands available

**Testing Required**:
- [ ] Enable VoiceOS service
- [ ] Immediately say voice command (within 1 second)
- [ ] Check if command is recognized
- [ ] Wait 1-2 seconds, try again
- [ ] Verify commands work after delay

**Potential Improvements**:
- Reduce delay to 200ms (test for stability)
- Load commands in parallel during initialization
- Show "Initializing..." indicator to user

---

#### 5. CommandManager Fallback Mode

**Status**: Untested degradation path
**Severity**: Low (fallback works, but unverified)
**Component**: `VoiceOSService.enableFallbackMode()`

**Issue**:
- ServiceMonitor can trigger fallback mode if CommandManager fails
- Falls back to Tier 2 → Tier 3 only (skips Tier 1)
- **Fallback path never tested in production**

**Flow**:
```
ServiceMonitor.bindCommandManager()
  → IF binding fails:
    → VoiceOSService.enableFallbackMode()
      → Sets fallbackModeEnabled = true
        → handleVoiceCommand() skips Tier 1
          → Goes directly to Tier 2 or Tier 3
```

**Testing Required**:
- [ ] Simulate CommandManager failure
- [ ] Verify fallback mode activates
- [ ] Check if Tier 2/3 commands still work
- [ ] Monitor logs for warnings

**Potential Problems**:
- User not notified of degraded mode
- Some commands unavailable (CommandManager-specific)
- No automatic recovery when CommandManager restored

---

#### 6. Exploration Progress Estimation

**Status**: Inaccurate estimates
**Severity**: Low (cosmetic issue)
**Component**: `ExplorationEngine.startExploration()`

**Issue**:
- `estimatedTotalScreens` hardcoded to 20
- Progress percentage unreliable
- User sees "50%" but might be 10% or 90%

**Code**:
```kotlin
ExplorationProgress(
    appName = packageName,
    screensExplored = 0,
    estimatedTotalScreens = 20,  ← HARDCODED
    ...
)
```

**Testing Required**:
- [ ] Learn multiple apps of different sizes
- [ ] Record actual screen counts
- [ ] Compare to estimated 20
- [ ] Check user confusion

**Potential Improvements**:
- Estimate based on app type/category
- Start with 10, increase as more screens found
- Show "X screens explored" instead of percentage
- Use navigation graph depth as heuristic

---

#### 7. Memory Usage During Exploration

**Status**: Unmonitored
**Severity**: Medium (could cause ANR/crashes)
**Component**: `ExplorationEngine`, `NavigationGraphBuilder`

**Issue**:
- Exploration builds in-memory navigation graph
- Large apps (100+ screens) could consume significant memory
- No memory pressure handling
- Could trigger Android low memory killer

**Affected Data Structures**:
```kotlin
NavigationGraphBuilder:
  - screenMap: MutableMap<String, ScreenState>  ← Unbounded
  - edgeList: MutableList<NavigationEdge>        ← Unbounded

ScreenStateManager:
  - visitedScreens: MutableSet<String>          ← Unbounded
```

**Testing Required**:
- [ ] Learn large app (e.g., Facebook, Instagram)
- [ ] Monitor memory usage with profiler
- [ ] Check for memory warnings in logcat
- [ ] Test on low-memory device (2GB RAM)

**Potential Improvements**:
- Implement LRU cache for screens
- Persist navigation graph to database incrementally
- Add memory pressure callbacks
- Set maximum exploration limits (e.g., 50 screens)

---

#### 8. Scroll Detection Accuracy

**Status**: Unknown reliability
**Severity**: Medium (affects exploration completeness)
**Component**: `LearnApp/scrolling/ScrollDetector.kt`

**Issue**:
- Detects scrollable containers to explore off-screen content
- False positives: Detect non-scrollable as scrollable → Waste time
- False negatives: Miss scrollable content → Incomplete learning

**Flow**:
```
ScreenExplorer.exploreScreen()
  → ScrollDetector.detectScrollableContainers(rootNode)
    → For each detected container:
      → ScrollExecutor.scrollDown()
        → Re-explore screen (find new elements)
```

**Testing Required**:
- [ ] Test with apps having long lists (Twitter, Instagram feed)
- [ ] Verify all list items explored
- [ ] Check for false positives (attempted scroll on non-scrollable)
- [ ] Monitor logs for scroll attempts

**Potential Problems**:
- Infinite scroll (e.g., news feeds) never completes
- Horizontal scrolling not supported
- Nested scrollable containers confuse detector

---

### Critical Path Analysis

**Most Likely Failure Points** (Priority Order):

1. ✅ **ConsentDialog** - Fixed, pending manual QA
2. ⚠️ **Login Screen Detection** - Implemented but untested
3. ⚠️ **Memory Usage** - No monitoring/limits
4. ⚠️ **Scroll Detection** - Unknown accuracy
5. ⚠️ **Dangerous Element Detection** - May be too conservative

---

### Monitoring Recommendations

#### Add Logging:
```kotlin
// 1. Track exploration memory usage
Log.d(TAG, "Memory: ${Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()} bytes")

// 2. Track screen visit counts
Log.d(TAG, "Screens visited: ${screenStateManager.visitedScreenCount()} / $estimatedTotalScreens")

// 3. Track element classifications
Log.d(TAG, "Elements: ${exploredElements.size}, Dangerous: $dangerousElementsSkipped, Login: $loginScreensDetected")

// 4. Track command registration timing
Log.d(TAG, "Commands registered at: ${System.currentTimeMillis() - serviceStartTime}ms after service start")
```

#### Add Metrics:
- Exploration success rate (completed / started)
- Average screens per app
- Average exploration time
- Dangerous elements false positive rate
- Login detection accuracy

---

### Testing Priority Matrix

```
┌────────────────────────────────────────────────────────────┐
│           HIGH PRIORITY (Test First)                       │
├────────────────────────────────────────────────────────────┤
│ 1. ConsentDialog v1.0.5 - Manual QA on device             │
│ 2. Login screen detection - Test with Instagram           │
│ 3. Memory usage - Profile large app exploration           │
│ 4. End-to-end exploration flow - Instagram/Twitter        │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│           MEDIUM PRIORITY (Test Soon)                      │
├────────────────────────────────────────────────────────────┤
│ 5. Scroll detection - Test with list-heavy apps           │
│ 6. Dangerous element detection - Review logs              │
│ 7. Command registration timing - Test immediate commands  │
└────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────┐
│           LOW PRIORITY (Monitor)                           │
├────────────────────────────────────────────────────────────┤
│ 8. Progress estimation - Cosmetic issue                   │
│ 9. Fallback mode - Unlikely to trigger                    │
│ 10. Database write contention - Room handles              │
└────────────────────────────────────────────────────────────┘
```

---

## Summary

### System Health: 🟡 YELLOW (Mostly Working, Some Issues)

**Working Components** ✅:
- VoiceOSService lifecycle and initialization
- Accessibility event processing
- Speech recognition (Vivoka)
- 3-tier command routing
- Database architecture (3 databases)
- Event debouncing
- UUID generation

**Fixed Issues** ✅:
- ConsentDialog BadTokenException (v1.0.5)

**Needs Testing** ⚠️:
- ConsentDialog fix (manual QA required)
- Login screen detection
- Exploration memory usage
- Scroll detection accuracy

**Known Limitations** ⚠️:
- Progress estimation inaccurate
- Dangerous element detection conservative
- Command registration delay (500ms)

---

**Last Updated:** 2025-10-28 19:19 PDT
**Documentation Version:** 1.0.0
**Next Steps:** Manual QA of ConsentDialog v1.0.5 fix on Android device

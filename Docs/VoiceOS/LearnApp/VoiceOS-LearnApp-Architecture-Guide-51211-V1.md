# VoiceOS LearnApp Architecture Guide

**Document ID:** VoiceOS-LearnApp-Architecture-Guide-51211-V1
**Version:** 1.0
**Created:** 2025-12-11
**Author:** Manoj Jhawar
**Related:** VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Analysis](#architecture-analysis)
3. [Module Separation](#module-separation)
4. [AIDL Interface](#aidl-interface)
5. [UI Design](#ui-design)
6. [Theme System](#theme-system)
7. [Data Flow](#data-flow)
8. [Gaps to 10/10 Architecture](#gaps-to-1010-architecture)
9. [File Reference](#file-reference)

---

## Overview

LearnApp is a dual-edition application for learning app interfaces through accessibility tree exploration. It operates as a standalone app that coordinates with the JIT (Just-In-Time) Learning Service running in VoiceOSCore via AIDL IPC.

### Key Principles

| Principle | Implementation |
|-----------|----------------|
| **Separation of Concerns** | JIT (scraper) vs LearnApp (orchestrator) |
| **Interface-Based Design** | AIDL defines the contract |
| **Dual Edition** | User (simple) vs Developer (full debugging) |
| **Safety First** | DoNotClick, login detection, loop prevention |
| **Ocean Blue XR Theme** | Consistent UI per IDEACODE guidelines |

---

## Architecture Analysis

### Current Rating: 9/10

| Layer | Status | Score |
|-------|--------|-------|
| Functional Correctness | PASS | 10/10 |
| Static Analysis | PASS | 9/10 |
| Runtime Analysis | PASS | 9/10 |
| Dependencies | PASS | 10/10 |
| Error Handling | PASS | 8/10 |
| Architecture (SOLID) | PASS | 9/10 |
| Performance | PASS | 9/10 |

### Module Dependency Graph

```
+-------------------------------------------------------------------------+
|                    VoiceOSCore (AccessibilityService)                   |
|  +-------------------------------------------------------------------+  |
|  |           JITLearningService (IElementCaptureService.Stub)        |  |
|  |  - Always running as part of AccessibilityService                 |  |
|  |  - Implements IElementCaptureService AIDL interface               |  |
|  |  - Owns: screen capture, element discovery, event streaming       |  |
|  +-------------------------------------------------------------------+  |
+--------------------------------+----------------------------------------+
                                 | AIDL IPC (Binder)
                                 v
+--------------------------------------------------------------------------+
|                 JITLearning Library (Interface Module)                   |
|  +--------------------------------------------------------------------+  |
|  | AIDL Interfaces:                                                   |  |
|  |  - IElementCaptureService.aidl (service contract)                  |  |
|  |  - IAccessibilityEventListener.aidl (callback interface)           |  |
|  |  - JITState.aidl, ScreenChangeEvent.aidl (parcelables)            |  |
|  |  - ParcelableNodeInfo.aidl, ExplorationCommand.aidl               |  |
|  +--------------------------------------------------------------------+  |
+--------------------------------+----------------------------------------+
                                 | Dependency
          +----------------------+----------------------+
          |                                             |
          v                                             v
+---------------------------------+   +---------------------------------+
|     LearnApp (User Edition)     |   |  LearnAppDev (Developer)        |
|  +---------------------------+  |   |  +---------------------------+  |
|  | LearnAppActivity          |  |   |  | LearnAppDevActivity       |  |
|  | - Ocean Blue Light Theme  |  |   |  | - Ocean Blue Dark Theme   |  |
|  | - Basic exploration UI    |  |   |  | - Cyan accent (#22D3EE)   |  |
|  | - Safety indicators       |  |   |  | - Full logging console    |  |
|  | - AVU export (encrypted)  |  |   |  | - Element tree inspector  |  |
|  | - AIDL binding to JIT     |  |   |  | - Event stream viewer     |  |
|  +---------------------------+  |   |  | - Neo4j graph viz (WIP)   |  |
|  Depends on: LearnAppCore      |   |  | - AVU export (unencrypted)|  |
|              JITLearning        |   |  +---------------------------+  |
+---------------------------------+   |  Depends on: LearnAppCore      |
                                      |              JITLearning        |
                                      |              Neo4j Driver       |
                                      +---------------------------------+
```

---

## Module Separation

### Component Responsibilities

| Component | Location | Responsibility |
|-----------|----------|----------------|
| **JITLearningService** | VoiceOSCore | AccessibilityService scraper, always running |
| **JITLearning** | Shared library | AIDL interfaces only (no implementation) |
| **LearnAppCore** | Shared library | Business logic: ExplorationState, SafetyManager, AVUExporter |
| **LearnApp** | Standalone APK | User-friendly exploration UI |
| **LearnAppDev** | Standalone APK | Developer debugging tools |
| **CommandManager** | Manager module | AVU file ingestion, command database |

### Dependency Rules

```
LearnApp -------> LearnAppCore -------> database
    |                  |
    +-------> JITLearning (AIDL interfaces)

LearnAppDev ---> LearnAppCore -------> database
    |                  |
    +-------> JITLearning (AIDL interfaces)
    |
    +-------> Neo4j Driver
```

**Key Rule:** LearnApp NEVER depends on VoiceOSCore directly. All communication via AIDL.

---

## AIDL Interface

### IElementCaptureService.aidl

The primary interface for LearnApp-JIT communication.

#### Core Methods (v1.0)

| Method | Purpose | Performance |
|--------|---------|-------------|
| `pauseCapture()` | Pause JIT during exploration | ~2-5ms |
| `resumeCapture()` | Resume JIT after exploration | ~2-5ms |
| `queryState()` | Get JIT statistics | ~2-5ms |
| `getLearnedScreenHashes()` | Skip known screens | ~5-15ms |

#### Event Streaming (v2.0)

| Method | Purpose | Performance |
|--------|---------|-------------|
| `registerEventListener()` | Real-time event streaming | ~2-5ms |
| `unregisterEventListener()` | Stop streaming | ~2-5ms |

#### Screen/Element Queries (v2.0)

| Method | Purpose | Performance |
|--------|---------|-------------|
| `getCurrentScreenInfo()` | Full screen tree | ~10-50ms |
| `getFullMenuContent()` | Complete menu items | ~5-20ms |
| `queryElements()` | Selector-based query | ~5-30ms |

#### Exploration Commands (v2.0)

| Method | Purpose | Performance |
|--------|---------|-------------|
| `performClick()` | Click element by UUID | ~5-20ms |
| `performScroll()` | Scroll in direction | ~10-30ms |
| `performAction()` | Generic exploration action | varies |
| `performBack()` | Navigate back | ~5-15ms |

### Event Listener Callbacks

```kotlin
interface IAccessibilityEventListener {
    fun onScreenChanged(event: ScreenChangeEvent)
    fun onElementAction(elementUuid: String, actionType: String, success: Boolean)
    fun onScrollDetected(direction: String, distance: Int, newElementsCount: Int)
    fun onDynamicContentDetected(screenHash: String, regionId: String)
    fun onMenuDiscovered(menuId: String, totalItems: Int, visibleItems: Int)
    fun onLoginScreenDetected(packageName: String, screenHash: String)
    fun onError(errorCode: String, message: String)
}
```

---

## UI Design

### User Edition (LearnApp)

```
+------------------------------------------------+
| ========= LearnApp Explorer ================= | <- Ocean Blue TopAppBar
+------------------------------------------------+
|                                                |
| +--------------------------------------------+ |
| | JIT Learning Status         [*] Active     | | <- JITStatusCard
| +--------------------------------------------+ |   (surfaceVariant bg)
| | Screens Learned            42              | |
| | Elements Discovered        187             | |
| | Current Package    com.example.app         | |
| +--------------------------------------------+ |
| |  [ Pause ]    [ Resume ]    [Refresh]      | | <- OutlinedButtons
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | App Exploration           [IDLE]           | | <- ExplorationStatusCard
| +--------------------------------------------+ |   (primaryContainer when active)
| |   +----+  +----+  +----+  +----+           | |
| |   | 5  |  | 23 |  | 12 |  |50% |           | | <- StatBox grid
| |   |Scrn|  |Elem|  |Clck|  |Covr|           | |
| |   +----+  +----+  +----+  +----+           | |
| +--------------------------------------------+ |
| |  [      Start Exploration      ]           | | <- Primary Button
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | Safety Status                              | | <- SafetyIndicatorsCard
| +--------------------------------------------+ |
| |  +---------+ +---------+ +---------+       | |
| |  |   3     | |   2     | |   4     |       | | <- SafetyStatBox
| |  | DNC     | | Dynamic | | Menus   |       | |
| |  |Skipped  | | Regions | | Found   |       | |
| |  +---------+ +---------+ +---------+       | |
| +--------------------------------------------+ |
| | ! Login Screen Detected                    | | <- Warning banner
| |   Type: PASSWORD                           | |
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | Export                                     | | <- ExportCard
| | Last export: com.example.app.vos           | |
| |  [     Export to AVU (.vos)     ]          | |
| +--------------------------------------------+ |
|                                                |
+------------------------------------------------+
```

### Developer Edition (LearnAppDev)

```
+------------------------------------------------+
| ====== LearnApp Dev [DEV] =================== | <- Dark TopAppBar
|                        +-----+                 |   with cyan "DEV" badge
|                        | DEV |                 |
+------------------------+-----+-----------------+
|  +---------+  +---------+  +----------+        | <- TabRow
|  | Status  |  |  Logs   |  | Elements |        |
|  +----*----+  +---------+  +----------+        |
+------------------------------------------------+
|                                                |
| TAB: STATUS                                    |
| +--------------------------------------------+ |
| | JIT Service                    (Cyan)      | | <- DevCard (dark)
| +--------------------------------------------+ |
| | Status              ACTIVE     (green)     | |
| | Screens             42                     | |
| | Elements            187                    | |
| |  [ Pause ]    [ Resume ]    [Refresh]      | |
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | Exploration                    (Cyan)      | | <- DevCard
| +--------------------------------------------+ |
| | Phase              EXPLORING   (blue)      | |
| | Screens            5                       | |
| | Elements           23                      | |
| | Coverage           50.5%                   | |
| |  [        START (green)        ]           | |
| +--------------------------------------------+ |
|                                                |
| TAB: LOGS                                      |
| +--------------------------------------------+ |
| | 156 entries              [ Clear ]         | |
| +--------------------------------------------+ |
| |############################################| | <- Console (#0D0D0D)
| |[14:32:05.123] I SCREEN: Hash: a1b2c3d4     | |   Monospace font
| |[14:32:05.089] E ACTION: click on btn1: OK  | |   Color-coded
| |[14:32:04.967] W LOGIN: Detected            | |
| +--------------------------------------------+ |
|                                                |
| TAB: ELEMENTS                                  |
| +--------------------------------------------+ |
| | 23 elements              [ Query ]         | |
| +--------------------------------------------+ |
| | +----------------------------------------+ | | <- ElementCard
| | | Login Button                (bold)     | | |
| | | Button                      (gray)     | | |
| | | com.example:id/btn_login    (green)    | | |
| | | +------+ +------+                      | | | <- ActionChips
| | | |click | |long  |                      | | |
| | | +------+ +------+                      | | |
| | | [0,540,1080,640]            (gray)     | | |
| | +----------------------------------------+ | |
| +--------------------------------------------+ |
+------------------------------------------------+
```

---

## Theme System

### Ocean Blue XR Theme (from UI Guidelines V2)

#### User Edition (Light Mode)

```kotlin
object OceanTheme {
    // Primary Colors
    val Primary = Color(0xFF3B82F6)         // Ocean Blue
    val PrimaryDark = Color(0xFF60A5FA)
    val PrimaryContainer = Color(0xFFDBEAFE)

    // Secondary
    val Secondary = Color(0xFF06B6D4)       // Cyan

    // Semantic
    val Success = Color(0xFF10B981)         // Green
    val Error = Color(0xFFEF4444)           // Red
    val Warning = Color(0xFFF59E0B)         // Amber

    // Surface
    val Surface = Color(0xFFF0F9FF)
    val SurfaceVariant = Color(0xFFE0F2FE)

    // Status
    val StatusActive = Color(0xFF10B981)
    val StatusPaused = Color(0xFFF59E0B)
    val StatusIdle = Color(0xFF6B7280)
    val StatusExploring = Color(0xFF3B82F6)
}
```

#### Developer Edition (Dark Mode)

```kotlin
object OceanDevTheme {
    // Primary Colors (Dark Mode)
    val Primary = Color(0xFF60A5FA)
    val PrimaryContainer = Color(0xFF1E3A5F)

    // Developer Accent (Cyan - distinguishes from User Edition)
    val Accent = Color(0xFF22D3EE)

    // Semantic (Dark Mode variants)
    val Success = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Warning = Color(0xFFFBBF24)

    // Surface (Glassmorphic Dark)
    val Surface = Color(0xFF0F172A)
    val SurfaceVariant = Color(0xFF1E293B)
    val Background = Color(0xFF0C1929)

    // Console
    val ConsoleBackground = Color(0xFF0D0D0D)
}
```

---

## Data Flow

### Service Binding Flow

```
LearnAppActivity                    JITLearningService
     |                                     |
     | -------- bindService() -----------> | (AIDL IPC)
     |                                     |
     | <---- onServiceConnected() -------- |
     |        IElementCaptureService       |
     |                                     |
     | -------- queryState() ------------> |
     | <------- JITState ------------------ |
     |                                     |
     | -------- pauseCapture() ----------> |
     |         (start exploration)         |
     |                                     |
     | -------- getCurrentScreenInfo() --> |
     | <------- ParcelableNodeInfo -------- |
     |                                     |
     | -------- performClick(uuid) ------> |
     | <------- boolean success ----------- |
     |                                     |
     | -------- resumeCapture() ---------> |
     |         (end exploration)           |
     +-------------------------------------+
```

### Event Streaming Flow (Developer Edition)

```
LearnAppDevActivity                 JITLearningService
     |                                     |
     | --- registerEventListener() ------> |
     |     (IAccessibilityEventListener)   |
     |                                     |
     | <--- onScreenChanged() ------------ | (callback)
     | <--- onElementAction() ------------ |
     | <--- onScrollDetected() ----------- |
     | <--- onDynamicContentDetected() --- |
     | <--- onMenuDiscovered() ----------- |
     | <--- onLoginScreenDetected() ------ |
     |                                     |
     | --- unregisterEventListener() ----> |
     +-------------------------------------+
```

### AVU Export Flow

```
LearnApp                    LearnAppCore                  CommandManager
   |                             |                              |
   | -- exportToAvu() ---------> |                              |
   |                             |                              |
   |                    AVUExporter.export()                    |
   |                             |                              |
   |                    Generate AVU format                     |
   |                             |                              |
   |                    Write to learned_apps/                  |
   |                             |                              |
   |                             | ---- FileObserver ---------> |
   |                             |                              |
   |                             |                    AVUFileWatcher detects
   |                             |                              |
   |                             |                    AVUFileParser.parse()
   |                             |                              |
   |                             |                    VOSCommandIngestion
   |                             |                              |
   |                             |                    Database insert
   +-----------------------------+------------------------------+
```

---

## Gaps to 10/10 Architecture

### Current Score: 9/10

| Gap | Impact | Effort | Priority |
|-----|--------|--------|----------|
| **Interface Segregation** | Medium | Medium | P2 |
| **Error Handling** | Medium | Low | P1 |
| **Unit Tests** | High | Medium | P1 |
| **Theme Module** | Low | Low | P3 |
| **Dependency Injection** | Medium | High | P2 |
| **ViewModel Architecture** | Medium | Medium | P2 |

### Detailed Gap Analysis

#### 1. Interface Segregation Principle (ISP)

**Issue:** `IElementCaptureService` is a "fat interface" with 15+ methods.

**Solution:** Split into smaller interfaces:
```kotlin
interface IScraper {
    fun pauseCapture()
    fun resumeCapture()
    fun queryState(): JITState
}

interface IExplorer {
    fun performClick(uuid: String): Boolean
    fun performScroll(direction: String, distance: Int): Boolean
    fun performBack(): Boolean
}

interface IEventStreamer {
    fun registerEventListener(listener: IAccessibilityEventListener)
    fun unregisterEventListener(listener: IAccessibilityEventListener)
}

interface IElementCaptureService : IScraper, IExplorer, IEventStreamer
```

#### 2. Standardized Error Handling

**Issue:** AIDL methods return primitive types; errors handled via exceptions.

**Solution:** Use Result wrapper:
```kotlin
// Parcelable result type
@Parcelize
data class AidlResult<T>(
    val success: Boolean,
    val data: T?,
    val errorCode: String?,
    val errorMessage: String?
) : Parcelable
```

#### 3. Unit Test Coverage

**Issue:** Activities lack unit tests; only integration tests exist.

**Solution:**
- Extract UI logic to ViewModels
- Use Robolectric for Activity tests
- Mock AIDL service in tests

#### 4. Theme Module

**Issue:** Theme colors duplicated in both LearnApp and LearnAppDev.

**Solution:** Create shared `OceanTheme` module:
```
Modules/VoiceOS/libraries/OceanTheme/
├── src/main/java/com/augmentalis/oceantheme/
│   ├── OceanTheme.kt
│   ├── OceanDevTheme.kt
│   └── OceanColors.kt
```

#### 5. Dependency Injection

**Issue:** Manual dependency creation in Activities.

**Solution:** Introduce Hilt:
```kotlin
@HiltAndroidApp
class LearnAppApplication : Application()

@AndroidEntryPoint
class LearnAppActivity : ComponentActivity() {
    @Inject lateinit var explorationState: ExplorationState
    @Inject lateinit var safetyManager: SafetyManager
}
```

#### 6. ViewModel Architecture

**Issue:** State managed directly in Activity with `mutableStateOf`.

**Solution:** Use ViewModel + StateFlow:
```kotlin
@HiltViewModel
class LearnAppViewModel @Inject constructor(
    private val explorationState: ExplorationState,
    private val safetyManager: SafetyManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorationUiState())
    val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

    fun startExploration() { ... }
    fun stopExploration() { ... }
}
```

---

## File Reference

### Core Files

| File | Location | Purpose |
|------|----------|---------|
| `IElementCaptureService.aidl` | JITLearning | AIDL service interface |
| `IAccessibilityEventListener.aidl` | JITLearning | Event callback interface |
| `LearnAppActivity.kt` | LearnApp | User Edition main activity |
| `LearnAppDevActivity.kt` | LearnAppDev | Developer Edition main activity |
| `ExplorationState.kt` | LearnAppCore | Exploration state management |
| `SafetyManager.kt` | LearnAppCore | Safety coordination |
| `AVUExporter.kt` | LearnAppCore | AVU file generation |
| `AVUFileParser.kt` | CommandManager | AVU file parsing |
| `AVUFileWatcher.kt` | CommandManager | Folder monitoring |

### Configuration Files

| File | Location | Purpose |
|------|----------|---------|
| `colors.xml` | LearnApp/res/values | Ocean Blue Light theme |
| `colors.xml` | LearnAppDev/res/values | Ocean Blue Dark theme |
| `themes.xml` | Both apps | Theme definitions |
| `build.gradle.kts` | Both apps | Build configuration |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-11 | Initial architecture documentation |

---

**End of Document**

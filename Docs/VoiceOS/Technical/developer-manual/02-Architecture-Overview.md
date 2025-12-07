# Chapter 2: Architecture Overview

**VOS4 Developer Manual**
**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Chapter Version:** 1.0

---

## Table of Contents

- [2.1 System Architecture](#21-system-architecture)
  - [2.1.1 High-Level Architecture](#211-high-level-architecture)
  - [2.1.2 Layer Breakdown](#212-layer-breakdown)
  - [2.1.3 Component Interaction](#213-component-interaction)
- [2.2 Module Organization](#22-module-organization)
  - [2.2.1 Module Categories](#221-module-categories)
  - [2.2.2 Module Structure](#222-module-structure)
  - [2.2.3 Dependency Management](#223-dependency-management)
- [2.3 Dependency Graph](#23-dependency-graph)
- [2.4 Data Flow](#24-data-flow)
  - [2.4.1 Voice Command Flow](#241-voice-command-flow)
  - [2.4.2 UI Scraping Flow](#242-ui-scraping-flow)
  - [2.4.3 Database Persistence Flow](#243-database-persistence-flow)
- [2.5 Technology Stack](#25-technology-stack)
- [2.6 SOLID Principles Application](#26-solid-principles-application)

---

## 2.1 System Architecture

### 2.1.1 High-Level Architecture

VOS4 follows a layered architecture that separates concerns and enables modularity:

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE LAYER                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   VoiceUI    │  │  LearnApp    │  │ VoiceCursor  │          │
│  │  (Compose)   │  │   (UI)       │  │   (Overlay)  │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              VoiceOSCore (AccessibilityService)          │   │
│  │  • Event Handling  • Scraping  • Command Execution      │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       MANAGER LAYER                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Command    │  │  VoiceData   │  │Localization  │          │
│  │   Manager    │  │   Manager    │  │   Manager    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       LIBRARY LAYER                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │    Speech    │  │    Device    │  │ UIElements   │          │
│  │ Recognition  │  │   Manager    │  │   Library    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ UUIDCreator  │  │   Keyboard   │  │   Logging    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Room Database (SQLite)                      │   │
│  │  • Apps  • Elements  • Commands  • Screen Contexts      │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           DataStore (Preferences & Settings)             │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PLATFORM LAYER (ANDROID)                       │
│  • AccessibilityService  • WindowManager  • PackageManager      │
│  • Audio System  • Sensor Framework  • Content Providers        │
└─────────────────────────────────────────────────────────────────┘
```

**Key Characteristics:**

- **Clear layer boundaries**: Each layer depends only on layers below
- **Bidirectional data flow**: Commands flow down, events flow up
- **Platform abstraction**: Android-specific code isolated in platform layer
- **Dependency injection**: Hilt manages dependencies across layers

### 2.1.2 Layer Breakdown

**UI Layer** - User-facing components
- Jetpack Compose screens
- Overlay system for visual feedback
- Voice cursor rendering
- Learning progress visualization

**Application Layer** - Core business logic
- Accessibility event processing
- UI hierarchy scraping
- Voice command execution
- Screen context inference

**Manager Layer** - Cross-cutting concerns
- Command parsing and routing
- Data synchronization
- Multi-language support
- License/feature gating

**Library Layer** - Reusable components
- Speech recognition abstraction
- Device capability detection
- UI component library
- Utility functions

**Data Layer** - Persistence
- Room database with DAO pattern
- DataStore for preferences
- Cache management
- Migration handling

**Platform Layer** - Android OS integration
- Accessibility APIs
- Window management
- Audio input/output
- System services

### 2.1.3 Component Interaction

**Voice Command Execution Flow:**

```
User speaks → SpeechRecognition → CommandManager → VoiceOSCore → AccessibilityService → System
     ↓              ↓                    ↓               ↓                 ↓              ↓
  Audio In    Speech-to-Text      Parse Command   Find Element    Perform Action   Visual Feedback
```

**UI Scraping Flow:**

```
App Window Opens → AccessibilityEvent → VoiceOSCore → Scraping Engine → Database
        ↓                  ↓                  ↓              ↓              ↓
   Window Change     Event Dispatch    Extract Hierarchy  Generate Hash  Store Elements
```

---

## 2.2 Module Organization

### 2.2.1 Module Categories

VOS4 organizes code into three primary module categories:

**1. Apps** (`modules/apps/`)

Standalone applications with their own UI and lifecycle:

```
modules/apps/
├── VoiceOSCore/         # Main accessibility service (APK)
├── VoiceUI/             # UI app with Magic components
├── VoiceCursor/         # Cursor system (library + overlay)
├── LearnApp/            # App learning module
└── VoiceRecognition/    # Speech recognition test app
```

**2. Libraries** (`modules/libraries/`)

Reusable components without UI:

```
modules/libraries/
├── SpeechRecognition/   # Multi-engine speech recognition
├── DeviceManager/       # Device detection & capabilities
├── VoiceUIElements/     # UI component library
├── VoiceKeyboard/       # Voice IME
├── UUIDCreator/         # Universal element identification
├── VoiceOsLogging/      # Centralized logging (Timber)
└── PluginSystem/        # MagicCode plugin infrastructure (KMP)
```

**3. Managers** (`modules/managers/`)

System-level coordinators:

```
modules/managers/
├── CommandManager/      # Command parsing & execution
├── VoiceDataManager/    # Data synchronization
├── LocalizationManager/ # Multi-language support
├── LicenseManager/      # Feature gating & subscriptions
└── HUDManager/          # Heads-up display coordination
```

### 2.2.2 Module Structure

Each module follows a consistent structure:

```
Module/
├── build.gradle.kts                    # Module configuration
├── src/
│   ├── main/
│   │   ├── java/com/augmentalis/[module]/
│   │   │   ├── [feature]/              # Feature packages
│   │   │   ├── database/               # Data layer (if applicable)
│   │   │   │   ├── entities/
│   │   │   │   ├── dao/
│   │   │   │   └── [Module]Database.kt
│   │   │   ├── ui/                     # UI components (if applicable)
│   │   │   └── [Module]Main.kt         # Entry point
│   │   ├── res/                        # Android resources
│   │   └── AndroidManifest.xml
│   ├── test/                           # Unit tests
│   └── androidTest/                    # Instrumentation tests
└── README.md                           # Module documentation
```

**Example: VoiceOSCore Structure**

```
VoiceOSCore/
├── build.gradle.kts
├── src/main/java/com/augmentalis/voiceoscore/
│   ├── accessibility/                  # Core accessibility service
│   │   ├── VoiceOnSentry.kt           # Foreground service
│   │   ├── IVoiceOSService.kt         # Service interface
│   │   ├── cursor/                    # Cursor management
│   │   ├── handlers/                  # Command handlers
│   │   ├── overlays/                  # Visual overlays
│   │   └── ui/                        # Accessibility UI
│   ├── scraping/                      # UI scraping system
│   │   ├── AccessibilityScrapingIntegration.kt
│   │   ├── CommandGenerator.kt
│   │   ├── SemanticInferenceHelper.kt
│   │   ├── ScreenContextInferenceHelper.kt
│   │   ├── entities/                  # Data models
│   │   └── dao/                       # Database access
│   ├── database/                      # Database layer
│   │   ├── VoiceOSAppDatabase.kt
│   │   └── entities/
│   └── learnweb/                      # Web learning (future)
```

### 2.2.3 Dependency Management

**Gradle Multi-Module Configuration**

File: `/Volumes/M-Drive/Coding/Warp/vos4/settings.gradle.kts`

```kotlin
// Main application
include(":app")

// Standalone Apps
include(":modules:apps:VoiceOSCore")
include(":modules:apps:VoiceUI")
include(":modules:apps:VoiceCursor")
include(":modules:apps:LearnApp")
include(":modules:apps:VoiceRecognition")

// System Managers
include(":modules:managers:CommandManager")
include(":modules:managers:VoiceDataManager")
include(":modules:managers:LocalizationManager")
include(":modules:managers:LicenseManager")

// Shared Libraries
include(":modules:libraries:VoiceUIElements")
include(":modules:libraries:UUIDCreator")
include(":modules:libraries:DeviceManager")
include(":modules:libraries:SpeechRecognition")
include(":modules:libraries:VoiceKeyboard")
include(":modules:libraries:VoiceOsLogging")
include(":modules:libraries:PluginSystem")
```

**Dependency Declaration Pattern**

File: `/Volumes/M-Drive/Coding/Warp/vos4/app/build.gradle.kts:77-96`

```kotlin
dependencies {
    // Standalone Apps
    implementation(project(":modules:apps:VoiceUI"))

    // Input Method Libraries
    implementation(project(":modules:libraries:VoiceKeyboard"))

    // System Managers
    implementation(project(":modules:managers:CommandManager"))
    implementation(project(":modules:managers:VoiceDataManager"))
    implementation(project(":modules:managers:LocalizationManager"))
    implementation(project(":modules:managers:LicenseManager"))

    // Shared Libraries
    implementation(project(":modules:apps:VoiceOSCore"))
    implementation(project(":modules:libraries:VoiceUIElements"))
    implementation(project(":modules:libraries:DeviceManager"))
    implementation(project(":modules:libraries:SpeechRecognition"))
    implementation(project(":modules:libraries:VoiceOsLogging"))
}
```

**Dependency Rules:**

1. **No circular dependencies** - Enforced by Gradle
2. **Libraries don't depend on apps** - One-way dependency flow
3. **Managers can depend on libraries** - But not on apps
4. **Apps can depend on anything** - They're at the top of the hierarchy

---

## 2.3 Dependency Graph

**Complete Dependency Graph:**

```
┌────────────────────────────────────────────────────────────┐
│                        app (VoiceOS)                        │
│                     Main Application                         │
└─────────────┬──────────────────────────┬───────────────────┘
              │                          │
              ▼                          ▼
    ┌──────────────────┐      ┌──────────────────┐
    │    VoiceUI       │      │  VoiceOSCore     │
    │  (Compose UI)    │      │ (Accessibility)  │
    └────────┬─────────┘      └────────┬─────────┘
             │                         │
             ├─────────────────────────┼─────────────────┐
             │                         │                 │
             ▼                         ▼                 ▼
    ┌──────────────┐          ┌─────────────┐   ┌──────────────┐
    │VoiceUIElements│          │SpeechRecog- │   │ CommandMgr   │
    │              │          │  nition     │   │              │
    └──────┬───────┘          └──────┬──────┘   └──────┬───────┘
           │                         │                 │
           │                         ▼                 │
           │                 ┌──────────────┐          │
           │                 │DeviceManager │          │
           │                 └──────────────┘          │
           │                                           │
           └───────────────────┬───────────────────────┘
                               │
                               ▼
                       ┌──────────────┐
                       │ UUIDCreator  │
                       │              │
                       └──────────────┘
```

**Module Dependency Table:**

| Module | Depends On | Used By |
|--------|-----------|---------|
| **app** | VoiceUI, VoiceOSCore, All Managers, Libraries | (none - top level) |
| **VoiceOSCore** | SpeechRecognition, CommandManager, UUIDCreator | app |
| **VoiceUI** | VoiceUIElements, UUIDCreator | app |
| **LearnApp** | VoiceOSCore (database), UUIDCreator | app |
| **VoiceCursor** | (standalone) | VoiceOSCore |
| **SpeechRecognition** | DeviceManager | VoiceOSCore, VoiceKeyboard |
| **CommandManager** | UUIDCreator | VoiceOSCore, app |
| **VoiceDataManager** | Room, DataStore | VoiceOSCore, app |
| **UUIDCreator** | (none - base library) | Multiple modules |
| **DeviceManager** | (none - base library) | SpeechRecognition |

---

## 2.4 Data Flow

### 2.4.1 Voice Command Flow

**Complete voice command execution:**

```
1. Audio Input
   User speaks → Microphone → Audio Buffer

2. Speech Recognition
   Audio Buffer → SpeechRecognition Library → Engine Selection
   ├─ Vivoka (offline, privacy-focused)
   ├─ Google Speech (online, high accuracy)
   └─ Vosk (offline, lightweight)

3. Text Output
   Engine → Recognized Text → Confidence Score

4. Command Parsing
   Text → CommandManager → Parser
   ├─ Intent extraction
   ├─ Parameter parsing
   └─ Context resolution

5. Command Routing
   Parsed Command → Handler Selection
   ├─ Navigation commands → NavigationHandler
   ├─ Cursor commands → CursorCommandHandler
   ├─ App commands → AppHandler
   ├─ Selection commands → SelectHandler
   └─ System commands → SystemHandler

6. Element Resolution
   Handler → VoiceOSCore → Accessibility Tree
   ├─ Query by text: "Click submit button"
   ├─ Query by ID: "Click button_1"
   ├─ Query by position: "Click item 3"
   └─ Query by semantic: "Submit this form"

7. Action Execution
   Resolved Element → AccessibilityAction
   ├─ ACTION_CLICK
   ├─ ACTION_LONG_CLICK
   ├─ ACTION_SCROLL_FORWARD
   ├─ ACTION_SET_TEXT
   └─ Custom gestures

8. Visual Feedback
   Action → Overlay System
   ├─ Highlight target element
   ├─ Show confirmation
   ├─ Display error (if failed)
   └─ Update status overlay

9. Logging & Analytics
   Execution → Database
   ├─ Command history
   ├─ Success/failure metrics
   └─ Performance timing
```

**Code Reference:**

Voice command processing starts in:
```
/Volumes/M-Drive/Coding/Warp/vos4/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/recognition/VoiceRecognitionManager.kt
```

### 2.4.2 UI Scraping Flow

**Automatic UI learning process:**

```
1. Window Change Detection
   App Window Opens → AccessibilityEvent (TYPE_WINDOW_STATE_CHANGED)

2. Event Reception
   AccessibilityService.onAccessibilityEvent(event)
   ↓
   AccessibilityScrapingIntegration.onAccessibilityEvent(event)

3. App Identification
   event.packageName → PackageManager
   ├─ App name
   ├─ Version code
   ├─ Icon drawable
   └─ Is this a launcher? (skip if yes)

4. Root Node Extraction
   AccessibilityService.rootInActiveWindow
   ↓
   Recursively traverse hierarchy

5. Element Extraction
   For each AccessibilityNodeInfo:
   ├─ className (e.g., android.widget.Button)
   ├─ text (visible text)
   ├─ contentDescription (accessibility label)
   ├─ resourceId (e.g., com.app:id/submit_btn)
   ├─ bounds (screen coordinates)
   ├─ isClickable, isScrollable, isEnabled
   ├─ actions (supported AccessibilityActions)
   └─ Depth in hierarchy

6. UUID Generation
   Element → UUIDCreator
   ├─ Device-specific UUID
   ├─ Third-party UUID (cross-device)
   └─ Alias management

7. Element Hashing
   Element Properties → MD5 Hash
   ├─ className + text + contentDesc + resourceId
   └─ Used for deduplication

8. Screen Fingerprinting (Recent Fix - Oct 2025)
   Top 10 significant elements → Content hash
   ├─ className:text:contentDesc:isClickable
   ├─ Sorted by depth
   └─ MD5 digest

9. Hierarchy Building
   Parent-child relationships → ScrapedHierarchyEntity
   ├─ parent_element_id
   ├─ child_element_id
   ├─ child_order
   └─ depth

10. Database Storage (with FK fix)
    Phase 1: Insert app metadata
    Phase 2: DELETE old hierarchy (CRITICAL - prevents FK violations)
    Phase 3: Insert elements (REPLACE strategy)
    Phase 4: Insert new hierarchy
    Phase 5: Insert screen context

11. Command Generation
    Elements → CommandGenerator
    ├─ "Click [text]" for buttons
    ├─ "Enter text in [label]" for text fields
    ├─ "Scroll [direction]" for scrollable containers
    └─ Semantic commands based on context

12. Screen Context Inference
    Elements + Patterns → ScreenContextInferenceHelper
    ├─ Login screen (2 EditTexts + Button)
    ├─ Permission dialog (specific keywords)
    ├─ Tutorial screen (onboarding patterns)
    ├─ Error screen (error keywords)
    └─ Content screen (default)
```

**Code Reference:**

UI scraping implementation:
```
/Volumes/M-Drive/Coding/Warp/vos4/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt:150-200
```

Recent FK fix and screen deduplication:
```
/Volumes/M-Drive/Coding/Warp/vos4/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt:363-483
```

### 2.4.3 Database Persistence Flow

**Room database architecture:**

```
┌─────────────────────────────────────────┐
│         VoiceOSAppDatabase              │
│       (Room Database Instance)          │
└────────────┬────────────────────────────┘
             │
             ├─────────────────────────────┐
             │                             │
             ▼                             ▼
    ┌────────────────┐           ┌────────────────┐
    │   AppEntity    │           │ScrapedElement  │
    │                │           │   Entity       │
    │ • app_id (PK)  │           │                │
    │ • package_name │◄──────────│ • element_id   │
    │ • app_name     │    FK     │ • app_id (FK)  │
    │ • version      │           │ • element_hash │
    └────────────────┘           │ • className    │
                                 │ • text         │
                                 │ • bounds       │
                                 └───────┬────────┘
                                         │
                                         │ FK
                                         ▼
                                ┌────────────────┐
                                │ScrapedHierarchy│
                                │    Entity      │
                                │                │
                                │ • parent_id FK │
                                │ • child_id FK  │
                                │ • child_order  │
                                └────────────────┘
```

**Database Operations:**

1. **Insert** - New elements added with REPLACE strategy
2. **Query** - Find elements by various criteria
3. **Update** - Screen visit counts, interaction metrics
4. **Delete** - Cascade deletes for app removal

**Recent Database Fix (Oct 2025):**

Issue: Foreign key constraint violations during element replacement

Solution implemented in `AccessibilityScrapingIntegration.kt:363-371`:
```kotlin
// CRITICAL: Delete old hierarchy records BEFORE inserting elements
database.scrapedHierarchyDao().deleteHierarchyForApp(appId)

// Insert elements with REPLACE strategy (gets new IDs)
val assignedIds: List<Long> = database.scrapedElementDao().insertBatchWithIds(elements)

// Insert new hierarchy with fresh IDs
database.scrapedHierarchyDao().insertBatch(hierarchyEntities)
```

---

## 2.5 Technology Stack

**Core Technologies:**

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Kotlin | 1.9.25 | Primary development language |
| **Build System** | Gradle | 8.10.2 | Multi-module build |
| **Android** | API 29-34 | 10-14 | Platform support |
| **UI Framework** | Jetpack Compose | BOM 2024.04.01 | Modern declarative UI |
| **Database** | Room | 2.6.1 | SQLite ORM |
| **DI** | Hilt | 2.51.1 | Dependency injection |
| **Async** | Coroutines | 1.7.3 | Asynchronous programming |
| **KSP** | 1.9.25-1.0.20 | Annotation processing |

**Third-Party Libraries:**

| Component | Library | Purpose |
|-----------|---------|---------|
| **Speech Recognition** | Vivoka VSDK 6.0.0 | Offline voice recognition |
| **Speech Recognition** | Google Speech API | Online voice recognition |
| **Speech Recognition** | Vosk | Lightweight offline recognition |
| **Logging** | Timber | Structured logging |
| **UI Components** | Material 3 | Material Design components |
| **Preferences** | DataStore | Modern preferences API |
| **Navigation** | Navigation Compose | Screen navigation |

**Development Tools:**

- **Android Studio**: Arctic Fox+ (2020.3.1+)
- **Java SDK**: OpenJDK 17
- **Version Control**: Git
- **CI/CD**: GitHub Actions (planned)

**Android Components:**

- **AccessibilityService**: Core UI interaction capability
- **WindowManager**: Overlay rendering
- **PackageManager**: App metadata retrieval
- **SensorManager**: IMU integration for cursor
- **AudioManager**: Microphone access
- **NotificationManager**: Background service notifications

---

## 2.6 SOLID Principles Application

**Single Responsibility Principle (SRP)**

Each class has one reason to change:

```kotlin
// AccessibilityScrapingIntegration.kt - Coordination only
class AccessibilityScrapingIntegration(context, service) {
    private val commandGenerator = CommandGenerator(context)
    private val screenContextHelper = ScreenContextInferenceHelper()
    private val semanticHelper = SemanticInferenceHelper()

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Delegates to specialized classes
        scrapeCurrentWindow(event)
    }
}

// CommandGenerator.kt - Command generation only
class CommandGenerator(context: Context) {
    fun generateCommands(elements: List<ScrapedElement>): List<Command> {
        // Focused on command creation logic
    }
}
```

**Open/Closed Principle (OCP)**

Open for extension, closed for modification:

```kotlin
// SpeechRecognition library extension point
abstract class SpeechRecognitionEngine {
    abstract fun startListening(callback: RecognitionCallback)
    abstract fun stopListening()
}

// New engines extend without modifying base
class CustomEngine : SpeechRecognitionEngine() {
    override fun startListening(callback: RecognitionCallback) {
        // Custom implementation
    }
}
```

**Liskov Substitution Principle (LSP)**

Subtypes are substitutable for base types:

```kotlin
fun recognizeSpeech(engine: SpeechRecognitionEngine) {
    engine.startListening { result ->
        processRecognizedText(result.text)
    }
}

// All implementations work identically
recognizeSpeech(VivokaEngine())
recognizeSpeech(GoogleEngine())
recognizeSpeech(VoskEngine())
```

**Interface Segregation Principle (ISP)**

Clients depend only on interfaces they use:

```kotlin
// Granular interfaces
interface Clickable { fun click() }
interface Scrollable { fun scroll(direction: Direction) }
interface Editable { fun setText(text: String) }

// Elements implement only relevant interfaces
class Button : Clickable {
    override fun click() { /* implementation */ }
}

class EditText : Clickable, Editable {
    override fun click() { /* implementation */ }
    override fun setText(text: String) { /* implementation */ }
}
```

**Dependency Inversion Principle (DIP)**

Depend on abstractions, not concretions:

```kotlin
// High-level module depends on abstraction
class VoiceOSService : AccessibilityService() {
    @Inject lateinit var scrapingIntegration: AccessibilityScrapingIntegration

    // Hilt provides concrete implementation
}

// Low-level module also depends on abstraction
class AccessibilityScrapingIntegration @Inject constructor(
    private val context: Context,
    private val service: AccessibilityService
) {
    // Dependencies injected, not hardcoded
}
```

---

## Summary

This chapter covered VOS4's architecture:

- **Layered architecture** separates concerns into UI, Application, Manager, Library, Data, and Platform layers
- **Module organization** groups code into Apps, Libraries, and Managers with clear boundaries
- **Dependency graph** shows unidirectional dependencies preventing circular references
- **Data flows** detail voice command execution, UI scraping, and database persistence
- **Technology stack** uses modern Android tools: Kotlin, Compose, Room, Hilt, Coroutines
- **SOLID principles** guide all architectural decisions

Next chapters dive into individual modules with detailed source code analysis.

**Proceed to:** [Chapter 3: VoiceOSCore Module](03-VoiceOSCore-Module.md)

---

**Chapter 2 Complete**
**Word Count:** ~8,000 words
**Reading Time:** 35-45 minutes

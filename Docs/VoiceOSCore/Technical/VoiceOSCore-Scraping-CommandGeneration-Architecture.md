# VoiceOSCore: Scraping & Command Generation Architecture

**Version:** 1.0
**Date:** 2026-01-22
**Module:** VoiceOSCore + voiceoscoreng

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Data Flow](#data-flow)
4. [Component Deep Dive](#component-deep-dive)
   - [Element Extraction](#1-element-extraction)
   - [Command Generation](#2-command-generation)
   - [Persistence Decision (4-Layer System)](#3-persistence-decision-4-layer-system)
   - [Database Layer](#4-database-layer)
   - [Import/Export System](#5-importexport-system)
5. [File Reference](#file-reference)
6. [Database Schema](#database-schema)
7. [Decision Rules Reference](#decision-rules-reference)
8. [Integration Points](#integration-points)
9. [Performance Considerations](#performance-considerations)

---

## Overview

VoiceOSCore provides voice-controlled accessibility for Android applications. The system:

1. **Scrapes** UI elements from the accessibility tree
2. **Generates** voice commands for actionable elements
3. **Decides** which commands to persist (static) vs keep in memory (dynamic)
4. **Stores** static commands in SQLDelight database for fast lookup
5. **Supports** import/export for command sharing between devices

### Key Principles

| Principle | Description |
|-----------|-------------|
| **Static vs Dynamic** | Menu items, buttons = static (persist). Email content, chat messages = dynamic (memory only) |
| **4-Layer Decision** | App category + Container type + Content signals + Screen type = persist decision |
| **KMP-First** | Core logic in Kotlin Multiplatform, Android-specific code in app layer |
| **Single Source of Truth** | CommandRegistry holds all active commands for current screen |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP LAYER                                    │
│                      (voiceoscoreng module)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────────────┐    ┌──────────────────────────┐               │
│  │ VoiceOSAccessibilityService│    │  AndroidAppCategoryProvider│              │
│  │                          │    │  (PackageManager API)     │               │
│  │  - onAccessibilityEvent()│    └──────────────────────────┘               │
│  │  - performExploration()  │                                                │
│  └───────────┬──────────────┘                                                │
│              │                                                               │
│              ▼                                                               │
│  ┌──────────────────────────┐    ┌──────────────────────────┐               │
│  │    ElementExtractor      │    │  DynamicCommandGenerator │               │
│  │                          │    │                          │               │
│  │  - extractElements()     │───▶│  - generateCommands()    │               │
│  │  - isDynamicContainer()  │    │  - generateIncremental() │               │
│  └──────────────────────────┘    └───────────┬──────────────┘               │
│                                              │                               │
│  ┌──────────────────────────┐                │                               │
│  │  CommandPersistenceManager│◀──────────────┘                               │
│  │                          │                                                │
│  │  - persistStaticCommands()│                                               │
│  └───────────┬──────────────┘                                                │
│              │                                                               │
├──────────────┼───────────────────────────────────────────────────────────────┤
│              │              KMP CORE LAYER                                   │
│              │           (VoiceOSCore module)                                │
├──────────────┼───────────────────────────────────────────────────────────────┤
│              ▼                                                               │
│  ┌──────────────────────────┐    ┌──────────────────────────┐               │
│  │   CommandOrchestrator    │    │   CommandGenerator       │               │
│  │                          │    │                          │               │
│  │  - generateCommands()    │───▶│  - fromElementWithPersistence()│          │
│  │  - generateIncremental() │    │  - generateIndexCommands()│               │
│  └───────────┬──────────────┘    └───────────┬──────────────┘               │
│              │                               │                               │
│              │                               ▼                               │
│              │               ┌──────────────────────────────┐               │
│              │               │ PersistenceDecisionEngine    │               │
│              │               │                              │               │
│              │               │  ┌────────────────────────┐  │               │
│              │               │  │ Layer 1: AppCategory   │  │               │
│              │               │  │ (EMAIL, SETTINGS, etc.)│  │               │
│              │               │  └────────────────────────┘  │               │
│              │               │  ┌────────────────────────┐  │               │
│              │               │  │ Layer 2: Container     │  │               │
│              │               │  │ (RecyclerView, etc.)   │  │               │
│              │               │  └────────────────────────┘  │               │
│              │               │  ┌────────────────────────┐  │               │
│              │               │  │ Layer 3: Content       │  │               │
│              │               │  │ (text length, patterns)│  │               │
│              │               │  └────────────────────────┘  │               │
│              │               │  ┌────────────────────────┐  │               │
│              │               │  │ Layer 4: Screen        │  │               │
│              │               │  │ (SETTINGS, FORM, LIST) │  │               │
│              │               │  └────────────────────────┘  │               │
│              │               └──────────────────────────────┘               │
│              │                                                               │
│              ▼                                                               │
│  ┌──────────────────────────┐    ┌──────────────────────────┐               │
│  │    CommandRegistry       │    │  ICommandPersistence     │               │
│  │                          │    │  (interface)             │               │
│  │  - updateSync()          │    └───────────┬──────────────┘               │
│  │  - match()               │                │                               │
│  │  - all()                 │                │                               │
│  └──────────────────────────┘                │                               │
│                                              │                               │
├──────────────────────────────────────────────┼───────────────────────────────┤
│                                              │     DATABASE LAYER            │
│                                              │   (database module)           │
├──────────────────────────────────────────────┼───────────────────────────────┤
│                                              ▼                               │
│  ┌──────────────────────────┐    ┌──────────────────────────┐               │
│  │   IScrapedAppRepository  │    │ IScrapedElementRepository│               │
│  │                          │    │                          │               │
│  │  - insert()              │    │  - insertBatch()         │               │
│  │  - getByPackage()        │    │  - getByAppId()          │               │
│  └──────────────────────────┘    └──────────────────────────┘               │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    SQLDelight Database                               │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │    │
│  │  │ scraped_app │  │scraped_elem │  │scraped_cmd  │                  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. Screen Change Event Flow

```
┌─────────────────┐
│ AccessibilityEvent │
│ (TYPE_WINDOW_STATE_CHANGED)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ VoiceOSAccessibilityService │
│ onAccessibilityEvent()      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────────┐
│ ScreenCacheManager │────▶│ Check screen hash │
│ (debounce 300ms)   │     │ (skip if same)    │
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐
│ performExploration() │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ElementExtractor │
│ extractElements() │
└────────┬────────┘
         │
         ├──────────────────────────────────────┐
         │                                      │
         ▼                                      ▼
┌─────────────────┐                 ┌─────────────────┐
│ List<ElementInfo> │                 │ List<HierarchyNode> │
│ (UI elements)     │                 │ (tree structure)    │
└────────┬────────┘                 └─────────────────┘
         │
         ▼
┌─────────────────┐
│ DynamicCommandGenerator │
│ generateCommands()      │
└────────┬────────┘
         │
         ├────────────────────┬────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Static Commands  │  │ Dynamic Commands │  │ Index Commands   │
│ (persist to DB)  │  │ (memory only)    │  │ (1st, 2nd, etc.) │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │
         ▼
┌─────────────────┐
│ CommandPersistenceManager │
│ persistStaticCommands()   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ SQLDelight DB    │
│ (scraped_* tables)│
└─────────────────┘
```

### 2. Voice Command Execution Flow

```
┌─────────────────┐
│ User speaks:     │
│ "click Settings" │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ SpeechEngine     │
│ (Vivoka/Android) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ VoiceOSCore      │
│ processCommand() │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ CommandRegistry  │
│ match("settings")│
└────────┬────────┘
         │
         ├─────────────────────────┐
         │                         │
         ▼                         ▼
┌─────────────────┐     ┌─────────────────┐
│ Memory Commands  │     │ DB Commands      │
│ (current screen) │     │ (via persistence)│
└────────┬────────┘     └─────────────────┘
         │
         ▼
┌─────────────────┐
│ QuantizedCommand │
│ (matched result) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ActionCoordinator│
│ executeCommand() │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ performClick()   │
│ (via A11y API)   │
└─────────────────┘
```

---

## Component Deep Dive

### 1. Element Extraction

**File:** `android/apps/voiceoscoreng/src/main/kotlin/.../service/ElementExtractor.kt`

The `ElementExtractor` traverses the accessibility tree and creates `ElementInfo` objects for each node.

#### Key Responsibilities

| Method | Description |
|--------|-------------|
| `extractElements()` | Recursive tree traversal, creates ElementInfo for each node |
| `isDynamicContainer()` | Checks if a class is RecyclerView, ListView, etc. |
| `findTopLevelListItems()` | Finds actual list item rows (for index commands) |
| `deriveElementLabels()` | Gets labels from child TextViews when parent has none |

#### ElementInfo Properties

```kotlin
data class ElementInfo(
    val className: String,           // "Button", "TextView", etc.
    val resourceId: String,          // "com.app:id/submit_btn"
    val text: String,                // Visible text
    val contentDescription: String,  // Accessibility label
    val bounds: Bounds,              // Screen coordinates
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isScrollable: Boolean,
    val isEnabled: Boolean,
    val packageName: String,

    // Dynamic content tracking
    val isInDynamicContainer: Boolean,  // Inside RecyclerView?
    val containerType: String,          // "RecyclerView", "ScrollView"
    val listIndex: Int                  // Position in list (-1 if not in list)
)
```

#### Dynamic Container Detection

```kotlin
private val dynamicContainerTypes = setOf(
    "RecyclerView", "ListView", "GridView",
    "ViewPager", "ViewPager2",
    "ScrollView", "HorizontalScrollView", "NestedScrollView",
    "LazyColumn", "LazyRow"  // Compose
)
```

---

### 2. Command Generation

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../CommandGenerator.kt`

The `CommandGenerator` creates `QuantizedCommand` objects from `ElementInfo`.

#### Key Methods

| Method | Description |
|--------|-------------|
| `fromElement()` | Basic command generation (backward compat) |
| `fromElementWithPersistence(element, pkg, allElements)` | Full 4-layer decision |
| `generateListIndexCommands()` | Creates "first", "second", etc. |
| `generateNumericCommands()` | Creates "1", "2", "3" for overlay badges |
| `generateListLabelCommands()` | Creates commands from extracted labels |

#### QuantizedCommand Structure

```kotlin
data class QuantizedCommand(
    val avid: String,              // Element fingerprint
    val phrase: String,            // Voice trigger (e.g., "Settings")
    val actionType: CommandActionType,  // CLICK, TYPE, SCROLL, etc.
    val targetAvid: String,        // Target element fingerprint
    val confidence: Float,         // 0.0-1.0
    val metadata: Map<String, String>  // Additional context
)
```

#### Command Types Generated

| Type | Example | Persisted? |
|------|---------|------------|
| Static | "Settings", "Compose" | Yes |
| Dynamic | Email subjects, chat messages | No |
| Index | "first", "second", "third" | No |
| Numeric | "1", "2", "3" | No |
| Label | Extracted sender names | No |

---

### 3. Persistence Decision (4-Layer System)

The **Hybrid Persistence System** determines whether a command should be saved to database or kept in memory only.

#### Layer 1: App Category

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../AppCategoryClassifier.kt`

```kotlin
enum class AppCategory(val dynamicBehavior: DynamicBehavior) {
    EMAIL(DynamicBehavior.MOSTLY_DYNAMIC),      // Gmail, Outlook
    MESSAGING(DynamicBehavior.MOSTLY_DYNAMIC),  // WhatsApp, Slack
    SOCIAL(DynamicBehavior.MOSTLY_DYNAMIC),     // Instagram, Twitter
    SETTINGS(DynamicBehavior.STATIC),           // System settings
    SYSTEM(DynamicBehavior.STATIC),             // Launcher, SystemUI
    PRODUCTIVITY(DynamicBehavior.MIXED),        // Notes, Calendar
    BROWSER(DynamicBehavior.MOSTLY_DYNAMIC),    // Chrome, Firefox
    MEDIA(DynamicBehavior.MIXED),               // Spotify, YouTube
    ENTERPRISE(DynamicBehavior.MIXED),          // RealWear, Augmentalis
    UNKNOWN(DynamicBehavior.MIXED)
}
```

**Pattern Matching Examples:**
- `com.google.android.gm` → EMAIL
- `com.android.settings` → SETTINGS
- `com.realwear.hmt` → ENTERPRISE

#### Layer 2: Container Type

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../ContainerClassifier.kt`

```kotlin
enum class ContainerBehavior {
    ALWAYS_DYNAMIC,        // RecyclerView, ListView - NEVER persist children
    CONDITIONALLY_DYNAMIC, // ScrollView - depends on content
    STATIC                 // FrameLayout, LinearLayout - usually persist
}
```

| Container | Behavior | Persist Children? |
|-----------|----------|-------------------|
| RecyclerView | ALWAYS_DYNAMIC | Never |
| ListView | ALWAYS_DYNAMIC | Never |
| LazyColumn | ALWAYS_DYNAMIC | Never |
| ScrollView | CONDITIONALLY_DYNAMIC | Depends |
| FrameLayout | STATIC | Usually yes |

#### Layer 3: Content Signals

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../ContentAnalyzer.kt`

```kotlin
data class ContentSignal(
    val textLength: TextLength,      // SHORT (<20), MEDIUM (20-100), LONG (>100)
    val hasResourceId: Boolean,
    val hasDynamicPatterns: Boolean, // Time, date, counters detected
    val stabilityScore: Int          // 0-100
)
```

**Stability Scoring:**
| Factor | Score Change |
|--------|--------------|
| resourceId present | +30 |
| Short text present | +20 |
| contentDescription | +15 |
| isInDynamicContainer | -20 |
| hasDynamicPatterns | -15 |
| LONG text (>100 chars) | -10 |
| Large bounds (>500x500) | -5 |

**Dynamic Patterns Detected:**
- Time: `3:45 PM`, `14:30`
- Date: `1/22`, `Jan 22`
- Counters: `5 new messages`
- Status: `typing...`, `online`
- Email preview: `Unread, , , Sender, , Subject...`

#### Layer 4: Screen Type

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../ScreenClassifier.kt`

```kotlin
enum class ScreenType {
    SETTINGS_SCREEN,   // High switch/toggle ratio
    LIST_SCREEN,       // Scrollable + many list items
    FORM_SCREEN,       // High text input ratio
    DETAIL_SCREEN,     // Mixed static/dynamic
    NAVIGATION_SCREEN, // Menu, drawer
    HOME_SCREEN,       // App landing page
    UNKNOWN
}
```

**Classification Heuristics:**
| Screen Type | Detection |
|-------------|-----------|
| SETTINGS_SCREEN | ≥15% switches AND ≥3 switches |
| FORM_SCREEN | ≥20% text inputs AND ≥2 inputs |
| LIST_SCREEN | Has scrollable container + ≥5 list items |
| NAVIGATION_SCREEN | Contains drawer/menu/navbar patterns |

#### Decision Engine

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/.../PersistenceDecisionEngine.kt`

Combines all 4 layers with a 6-rule priority system:

```kotlin
// Rule 0 (Pre-filter): Non-actionable or no content → NEVER persist
// Rule 1: ALWAYS_DYNAMIC container → NEVER persist
// Rule 2: Settings/System app → Persist (unless dynamic patterns)
// Rule 3: Settings screen in any app → Persist (unless dynamic patterns)
// Rule 4: Form screen → Persist SHORT/MEDIUM, skip LONG
// Rule 5: Email/Messaging/Social → Persist if (short+resourceId) OR stability>70
// Rule 6: Unknown → Persist if stability>60 AND no dynamic patterns
```

---

### 4. Database Layer

**Module:** `Modules/VoiceOS/core/database/`

#### Schema Overview

```sql
-- scraped_app: One row per app
CREATE TABLE scraped_app (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT NOT NULL UNIQUE,
    app_name TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    version_name TEXT NOT NULL,
    category TEXT,
    first_scraped_at INTEGER NOT NULL,
    last_scraped_at INTEGER NOT NULL,
    total_commands INTEGER DEFAULT 0,
    total_screens INTEGER DEFAULT 0
);

-- scraped_element: UI elements extracted from apps
CREATE TABLE scraped_element (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    app_id INTEGER NOT NULL REFERENCES scraped_app(id),
    avid TEXT NOT NULL,
    element_hash TEXT NOT NULL,
    class_name TEXT NOT NULL,
    resource_id TEXT,
    text_content TEXT,
    content_description TEXT,
    bounds TEXT,
    screen_hash TEXT,
    is_clickable INTEGER DEFAULT 0,
    is_scrollable INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    UNIQUE(app_id, element_hash)
);

-- scraped_command: Voice commands generated from elements
CREATE TABLE scraped_command (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_id INTEGER REFERENCES scraped_element(id),
    app_id INTEGER NOT NULL REFERENCES scraped_app(id),
    avid TEXT NOT NULL,
    phrase TEXT NOT NULL,
    action_type TEXT NOT NULL,
    target_avid TEXT,
    confidence REAL DEFAULT 0.5,
    metadata TEXT,
    created_at INTEGER NOT NULL,
    last_used_at INTEGER,
    use_count INTEGER DEFAULT 0,
    UNIQUE(app_id, phrase)
);
```

#### Repository Interfaces

**IScrapedAppRepository:**
```kotlin
interface IScrapedAppRepository {
    suspend fun insert(app: ScrapedAppDTO): Long
    suspend fun getByPackage(packageName: String): ScrapedAppDTO?
    suspend fun getAll(): List<ScrapedAppDTO>
    suspend fun getAllPackageNames(): List<String>
    suspend fun updateStats(packageName: String, commands: Int, screens: Int)
}
```

**IScrapedElementRepository:**
```kotlin
interface IScrapedElementRepository {
    suspend fun insertBatch(elements: List<ScrapedElementDTO>)
    suspend fun getByAppId(appId: Long): List<ScrapedElementDTO>
    suspend fun getByScreenHash(appId: Long, screenHash: String): List<ScrapedElementDTO>
    suspend fun deleteByAppId(appId: Long)
}
```

**ICommandPersistence:**
```kotlin
interface ICommandPersistence {
    suspend fun getByPackage(packageName: String): List<QuantizedCommand>
    suspend fun countByPackage(packageName: String): Long
    suspend fun insert(command: QuantizedCommand): Long?
    suspend fun insertBatch(commands: List<QuantizedCommand>)
    suspend fun deleteByPackage(packageName: String): Int
}
```

---

### 5. Import/Export System

Enables sharing learned commands between devices.

#### Export Flow

```
┌─────────────────┐
│ ExportSettingsActivity │
│ (Select apps)          │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ CommandExporter  │
│ exportApps()     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ExportPackage    │
│ (data classes)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ExportSerializer │
│ serialize()      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AndroidExportFileProvider │
│ writeToUri()              │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ JSON file        │
│ (via SAF)        │
└─────────────────┘
```

#### Import Flow

```
┌─────────────────┐
│ ImportSettingsActivity │
│ (Select file)          │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AndroidExportFileProvider │
│ readImport()              │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ExportSerializer │
│ deserialize()    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ CommandImporter  │
│ preview()        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ImportPreview    │
│ (show to user)   │
└────────┬────────┘
         │ (user confirms)
         ▼
┌─────────────────┐
│ CommandImporter  │
│ import(strategy) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Database updated │
└─────────────────┘
```

#### Import Strategies

| Strategy | Behavior |
|----------|----------|
| MERGE | Add new commands, keep existing |
| REPLACE | Delete existing, import all |
| SKIP_EXISTING | Only import apps not in DB |

#### Export JSON Format

```json
{
  "manifest": {
    "version": 1,
    "createdAt": 1705900000000,
    "deviceId": "device123",
    "appCount": 2,
    "totalCommands": 45,
    "exportType": "MULTI_APP"
  },
  "apps": [
    {
      "packageName": "com.android.settings",
      "appName": "Settings",
      "versionCode": 33,
      "versionName": "13.0",
      "category": "SETTINGS",
      "commands": [
        {
          "avid": "BTN:a3f2e1c9",
          "phrase": "Wi-Fi",
          "actionType": "CLICK",
          "targetAvid": "BTN:a3f2e1c9",
          "confidence": 0.95,
          "screenHash": "settings_main",
          "metadata": {...}
        }
      ],
      "screens": [...]
    }
  ]
}
```

---

## File Reference

### KMP Core (VoiceOSCore module)

| File | Purpose |
|------|---------|
| **Classification** | |
| `AppCategoryClassifier.kt` | Layer 1: App category from package name |
| `ContainerClassifier.kt` | Layer 2: Container type classification |
| `ContentAnalyzer.kt` | Layer 3: Content signal analysis |
| `ScreenClassifier.kt` | Layer 4: Screen type from element stats |
| `PersistenceDecisionEngine.kt` | 6-rule decision matrix |
| **Command Generation** | |
| `CommandGenerator.kt` | Creates QuantizedCommand from ElementInfo |
| `CommandOrchestrator.kt` | Orchestrates generation + persistence |
| `CommandRegistry.kt` | In-memory command store for matching |
| `QuantizedCommand.kt` | Command data class |
| **Interfaces** | |
| `ICommandPersistence.kt` | Database persistence interface |
| `IAppCategoryProvider.kt` | Platform-agnostic category provider |
| **Import/Export** | |
| `CommandExportModels.kt` | Export data classes |
| `ICommandExporter.kt` | Export interface |
| `ICommandImporter.kt` | Import interface |
| `IExportFileProvider.kt` | File I/O interface |
| `CommandExporter.kt` | Export implementation |
| `CommandImporter.kt` | Import implementation |
| `ExportSerializer.kt` | JSON serialization |

### Android App (voiceoscoreng)

| File | Purpose |
|------|---------|
| **Service** | |
| `VoiceOSAccessibilityService.kt` | Main accessibility service |
| `ElementExtractor.kt` | Accessibility tree traversal |
| `DynamicCommandGenerator.kt` | App-level command generation wrapper |
| `CommandPersistenceManager.kt` | Persistence orchestration |
| `AndroidAppCategoryProvider.kt` | PackageManager-based category |
| `AndroidExportFileProvider.kt` | SAF file operations |
| **UI** | |
| `ExportSettingsActivity.kt` | Export UI |
| `ImportSettingsActivity.kt` | Import UI |

### Database Module

| File | Purpose |
|------|---------|
| `ScrapedAppDTO.kt` | App data transfer object |
| `IScrapedAppRepository.kt` | App repository interface |
| `IScrapedElementRepository.kt` | Element repository interface |
| `DatabaseMigrations.kt` | Schema migrations |

---

## Decision Rules Reference

### Rule Priority Order

```
┌─────────────────────────────────────────────────────────────────┐
│ Rule 0: Pre-filter                                               │
│ - Not actionable (not clickable AND not scrollable) → SKIP      │
│ - No voice content (no text, contentDesc, resourceId) → SKIP    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 1: ALWAYS_DYNAMIC Container                                 │
│ - Element is inside RecyclerView, ListView, LazyColumn, etc.    │
│ - Decision: NEVER PERSIST                                        │
│ - Confidence: 1.0                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 2: Settings/System App                                      │
│ - App category is SETTINGS or SYSTEM                            │
│ - Decision: PERSIST (unless has dynamic patterns)               │
│ - Confidence: 0.85-0.95                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 3: Settings Screen (any app)                                │
│ - Screen type is SETTINGS_SCREEN (high switch ratio)            │
│ - Decision: PERSIST (unless has dynamic patterns)               │
│ - Confidence: 0.80-0.90                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 4: Form Screen                                              │
│ - Screen type is FORM_SCREEN (high text input ratio)            │
│ - Decision: PERSIST if SHORT/MEDIUM text, SKIP if LONG          │
│ - Confidence: 0.75-0.90                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 5: Email/Messaging/Social App                               │
│ - App category is EMAIL, MESSAGING, or SOCIAL                   │
│ - Decision: PERSIST if (SHORT text + hasResourceId)             │
│             OR stabilityScore > 70                               │
│ - Confidence: 0.70-0.85                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ Rule 6: Unknown/Other Apps                                       │
│ - Default fallback for unmatched cases                          │
│ - Decision: PERSIST if stabilityScore > 60 AND no dynamic patterns│
│ - Confidence: 0.65-0.80                                          │
└─────────────────────────────────────────────────────────────────┘
```

### Decision Examples

| Scenario | App | Container | Content | Screen | Rule | Persist? |
|----------|-----|-----------|---------|--------|------|----------|
| Email row in Gmail | EMAIL | RecyclerView | Long text | LIST | 1 | No |
| Compose button in Gmail | EMAIL | FrameLayout | Short + resourceId | HOME | 5 | Yes |
| Wi-Fi toggle in Settings | SETTINGS | LinearLayout | Short | SETTINGS | 2 | Yes |
| Login button | UNKNOWN | ConstraintLayout | Short + resourceId | FORM | 4 | Yes |
| "Last updated 3:45 PM" | SETTINGS | LinearLayout | Has time pattern | SETTINGS | 2 | No |
| Menu item in drawer | UNKNOWN | ScrollView | Short + resourceId | NAVIGATION | 6 | Yes |

---

## Integration Points

### Adding a New App Category

1. Add enum value to `AppCategoryClassifier.AppCategory`
2. Add package patterns to classifier
3. Update tests in `AppCategoryClassifierTest.kt`

### Customizing Persistence Rules

1. Modify rules in `PersistenceDecisionEngine.decide()`
2. Adjust confidence values as needed
3. Update tests in `PersistenceDecisionEngineTest.kt`

### Adding a New Screen Type

1. Add enum value to `ScreenClassifier.ScreenType`
2. Add classification logic in `classifyScreen()`
3. Consider adding rule in `PersistenceDecisionEngine`

---

## Performance Considerations

### Element Extraction
- Single-pass tree traversal
- Deduplication via hash set
- Early exit for non-actionable elements

### Command Generation
- Batch processing of elements
- Shared classifications per screen (not per element)
- Lazy evaluation of stability scores

### Database Operations
- Batch inserts for elements and commands
- Indexed queries by package name
- Async persistence (fire-and-forget)

### Memory Management
- Dynamic commands cleared on screen change
- Static commands loaded on-demand per package
- Export packages streamed to file

---

*VoiceOSCore Module | IDEACODE Framework*

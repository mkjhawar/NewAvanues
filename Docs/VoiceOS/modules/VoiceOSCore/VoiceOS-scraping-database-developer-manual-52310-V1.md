# App Scraping Database System - Developer Manual

**Created**: 2025-10-23 20:52 PDT
**Module**: VoiceOSCore
**Location**: `/app/src/main/java/com/augmentalis/voiceoscore/scraping/`
**Database Version**: 8
**Technology**: Room Database with KSP

---

## Quick Links
- [VoiceOSCore Module README](../README.md)
- [VoiceOSCore Architecture](./architecture/overview.md)
- [Accessibility System Overview](../../voiceos-master/architecture/accessibility-system.md)

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Database Schema](#database-schema)
5. [DAO Reference](#dao-reference)
6. [Helper Classes](#helper-classes)
7. [Data Flow](#data-flow)
8. [Usage Examples](#usage-examples)
9. [Foreign Key Relationships](#foreign-key-relationships)
10. [Migration History](#migration-history)
11. [Performance Considerations](#performance-considerations)

---

## System Overview

The App Scraping Database system provides automated UI element discovery, voice command generation, and user interaction tracking for VoiceOS. It leverages Android's Accessibility Service to analyze third-party app interfaces and build a comprehensive database of actionable elements.

### Key Features

- **Automatic UI Scraping**: Captures accessibility tree on window changes
- **Hash-Based Deduplication**: Prevents duplicate element storage (Phase 1)
- **Voice Command Generation**: NLP-based command creation with synonyms
- **AI Context Inference**: Semantic role detection, input type classification (Phase 1 & 2)
- **Screen Context Tracking**: Screen type detection, navigation patterns (Phase 2)
- **User Interaction Learning**: Tracks clicks, state changes for confidence scoring (Phase 3)
- **UUID Integration**: Universal element identification via UUIDCreator library
- **State-Aware Commands**: Generates context-appropriate commands ("check" vs "uncheck")

### Version History

| Phase | Version | Features |
|-------|---------|----------|
| **Phase 1** | v4-5 | Hash deduplication, semantic inference, UUID support |
| **Phase 2** | v5-6 | Screen contexts, element relationships, form grouping |
| **Phase 2.5** | v6-7 | Screen transitions, navigation flow tracking |
| **Phase 3** | v7-8 | User interactions, state history, battery optimization |
| **Audit 2025-11-03** | Current | Data integrity validation, hierarchy fixes, UUID monitoring |

### Recent Improvements (November 2025 Audit)

A comprehensive audit of the scraping and database system was conducted on 2025-11-03, resulting in **10 major improvements** to data integrity and validation:

#### Data Integrity & Validation
- **P1-1: Database Count Validation** - Added verification that scraped elements match persisted count, preventing silent data loss
- **P1-2: Cached Element Hierarchy** - Fixed orphaned children when parent elements are cached (see [P1-2 Resolution](./audit/VoiceOSCore-P1-2-Resolution-2511032213.md))
- **P1-3: UUID Generation Metrics** - Track UUID generation/registration success rates with warnings if <90%
- **P1-4: UUID Uniqueness Validation** - Added queries to detect duplicate UUIDs
- **P1-5: Enhanced Metrics** - Metrics now include actual database count ("Persisted") not just scraped count

#### System Reliability
- **P2-1: Count Update Timing** - Element/command counts updated after all operations complete (not mid-flow)
- **P2-2: FK Constraint Verification** - Foreign keys explicitly enabled and verified on database open
- **P2-3: Orphaned Element Detection** - Query to detect hierarchy integrity issues
- **P2-4: Cycle Detection** - Recursive CTE query to detect circular parent-child relationships
- **P2-5: UUID Documentation** - Comprehensive KDoc explaining UUID optional behavior

**Status**: ✅ 10/10 issues resolved | **Documentation**: See [Audit Report](./audit/VoiceOSCore-Audit-2511032014.md) | **Test Coverage**: 41 validation test cases

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  VoiceAccessibilityService                  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │     AccessibilityScrapingIntegration (Main Entry)   │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                     │                                        │
│  ┌──────────────────┴──────────────────┐                   │
│  │   Scraping Components                │                   │
│  ├──────────────────────────────────────┤                   │
│  │ • ElementHasher (deprecated)         │                   │
│  │ • CommandGenerator                   │                   │
│  │ • SemanticInferenceHelper            │                   │
│  │ • ScreenContextInferenceHelper       │                   │
│  │ • VoiceCommandProcessor              │                   │
│  └──────────────────┬──────────────────┘                   │
│                     │                                        │
│  ┌──────────────────┴──────────────────┐                   │
│  │   AppScrapingDatabase (Room)         │                   │
│  ├──────────────────────────────────────┤                   │
│  │ • 9 Entities                         │                   │
│  │ • 9 DAOs                             │                   │
│  │ • 8 Migrations                       │                   │
│  └──────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### Database Architecture

```
┌─────────────────┐
│  ScrapedApp     │ (1)
└────────┬────────┘
         │
         ├─── (N) ───┐
         │           │
         ▼           ▼
┌─────────────────┐  ┌──────────────────┐
│ ScrapedElement  │  │  ScreenContext   │ (screen-level data)
└────────┬────────┘  └────────┬─────────┘
         │                    │
         │                    │
    ┌────┴──────┬────────────┼─────────────────┐
    │           │            │                 │
    ▼           ▼            ▼                 ▼
┌─────────┐ ┌────────────┐ ┌──────────┐ ┌──────────┐
│Generated│ │  Scraped   │ │  User    │ │ Element  │
│ Command │ │ Hierarchy  │ │Interaction│ │  State   │
└─────────┘ └────────────┘ └──────────┘ └──────────┘
                                │
                                ▼
                      ┌──────────────────┐
                      │Element Relationship│
                      └──────────────────┘
                                │
                                ▼
                      ┌──────────────────┐
                      │Screen Transition │
                      └──────────────────┘
```

---

## Core Components

### 1. AccessibilityScrapingIntegration

**Location**: `AccessibilityScrapingIntegration.kt`
**Role**: Main integration layer between VoiceAccessibilityService and database

#### Public Methods

##### `onAccessibilityEvent(event: AccessibilityEvent)`
**Purpose**: Handle accessibility events from service

**Events Handled**:
- `TYPE_WINDOW_STATE_CHANGED` → Triggers window scraping
- `TYPE_VIEW_CLICKED` → Records click interaction (Phase 3)
- `TYPE_VIEW_LONG_CLICKED` → Records long press interaction
- `TYPE_VIEW_FOCUSED` → Records focus event
- `TYPE_VIEW_SCROLLED` → Records scroll event
- `TYPE_VIEW_SELECTED` → Records selection state change
- `TYPE_WINDOW_CONTENT_CHANGED` → Tracks content/state changes

**Usage**:
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    scrapingIntegration.onAccessibilityEvent(event)
}
```

##### `processVoiceCommand(voiceInput: String): CommandResult`
**Purpose**: Process voice command and execute corresponding action

**Parameters**:
- `voiceInput`: Spoken command text (e.g., "click submit button")

**Returns**: `CommandResult` with success status and message

**Example**:
```kotlin
val result = scrapingIntegration.processVoiceCommand("click login")
if (result.success) {
    Log.d(TAG, "Command executed: ${result.message}")
}
```

##### `processTextInput(targetCommand: String, text: String): CommandResult`
**Purpose**: Execute text input command on identified field

**Parameters**:
- `targetCommand`: Command to find input field (e.g., "type in email")
- `text`: Text to input into the field

**Example**:
```kotlin
scrapingIntegration.processTextInput("type in username", "john@example.com")
```

##### `learnApp(packageName: String): LearnAppResult`
**Purpose**: Perform comprehensive UI traversal and element discovery

**Use Case**: Initial app learning before voice control is enabled

**Returns**:
```kotlin
LearnAppResult(
    success: Boolean,
    message: String,
    elementsDiscovered: Int,
    newElements: Int,
    updatedElements: Int
)
```

**Example**:
```kotlin
val result = scrapingIntegration.learnApp("com.example.targetapp")
Log.i(TAG, "Learned ${result.newElements} new elements")
```

##### `setInteractionLearningEnabled(enabled: Boolean)`
**Purpose**: Enable/disable user interaction tracking (Phase 3)

**Note**: Respects battery level (disabled below 20%) and user preferences

**Example**:
```kotlin
// Disable interaction learning
scrapingIntegration.setInteractionLearningEnabled(false)
```

##### `isInteractionLearningUserEnabled(): Boolean`
**Purpose**: Check current interaction learning setting

#### Private Methods (Internal Implementation)

##### `scrapeCurrentWindow(event: AccessibilityEvent, filterNonActionable: Boolean = false)`
**Purpose**: Main scraping logic - traverses accessibility tree and stores elements

**Process** (Updated 2025-11-03 with validation):
1. Get root node from accessibility event
2. Calculate app hash for deduplication
3. Check if app already exists in database
4. Recursively scrape element tree (`scrapeNode()`)
5. Insert elements and capture database IDs
6. **P1-1: Validate database count** - Verify persisted count matches scraped count (throws exception if mismatch)
7. Build hierarchy relationships (P1-2: uses cached element IDs for complete hierarchy)
8. Register UUIDs with UUIDCreator
9. **P1-3: Track UUID metrics** - Log generation/registration rates, warn if <90%
10. Generate voice commands
11. Create/update screen context
12. Infer element relationships (Phase 2.5)
13. Track screen transitions
14. **P2-1: Update app metadata** - Element/command counts updated AFTER all operations complete
15. **P1-5: Log enhanced metrics** - Includes Persisted count from database validation

**Metrics Tracked** (Enhanced 2025-11-03):
- Elements found vs cached vs newly scraped
- **Persisted count** (actual database count after validation)
- **UUID generation rate** (% of elements with UUIDs)
- **UUID registration rate** (% of UUIDs successfully registered)
- Cache hit rate percentage
- Scraping time in milliseconds

**Validation Checks** (Added 2025-11-03):
- ✅ **P1-1**: Database count validation prevents silent data loss
- ✅ **P1-2**: Cached parent hierarchy ensures no orphaned children
- ✅ **P1-3**: UUID health monitoring with automatic warnings
- ✅ **P2-2**: Foreign key constraints verified on database open

##### `scrapeNode(node, appId, parentIndex, depth, indexInParent, elements, hierarchyBuildInfo, filterNonActionable, metrics): Int`
**Purpose**: Recursively scrape accessibility node and children

**Parameters**:
- `node`: AccessibilityNodeInfo to scrape
- `appId`: App identifier
- `parentIndex`: Index of parent in elements list (null for root)
- `depth`: Current depth in tree (0 for root)
- `indexInParent`: Index among siblings
- `elements`: Mutable list to collect scraped elements
- `hierarchyBuildInfo`: List to track hierarchy relationships
- `filterNonActionable`: Skip non-actionable elements if true
- `metrics`: Scraping metrics tracker

**Returns**: Index in elements list, or -1 if skipped

**Hash Deduplication** (Phase 1):
- Calculates `AccessibilityFingerprint` for each element
- Checks database for existing element with same hash
- **P1-2 Fix (2025-11-03)**: Queries full cached element entity (not just boolean) to retrieve database ID
- Skips scraping if element exists (marks as cached)
- Proceeds to scrape children even if parent is cached
- **P1-2 Fix**: Returns cached element's database ID (not -1) so children can build hierarchy correctly
- **Result**: No orphaned children when parent is cached

**Before P1-2 Fix**: Cached parents returned -1, causing children to become orphans (no hierarchy relationship)
**After P1-2 Fix**: Cached parents return database ID, children build complete hierarchy

**AI Context Inference**:
- Semantic role (e.g., "submit_login", "input_email")
- Input type for editable fields
- Visual weight for buttons
- Required field detection
- Placeholder text extraction
- Validation pattern inference

**UUID Generation**:
- Uses `ThirdPartyUuidGenerator` from UUIDCreator library
- Registers element with UUIDCreator for cross-system identification
- Creates auto-aliases for easier voice commands

**Metadata Validation** (Phase 1):
- Quality scoring for element metadata
- Logs warnings for poor metadata quality
- Priority suggestions for improvements

##### `recordInteraction(event: AccessibilityEvent, interactionType: String)`
**Purpose**: Record user interaction with UI element (Phase 3)

**Checks**:
- Interaction learning enabled (user setting)
- Battery level > 20%

**Data Collected**:
- Element hash
- Screen hash
- Interaction type (click, long_press, etc.)
- Visibility duration (time element was visible before interaction)
- Success status

**Foreign Key Validation**:
- Verifies element exists in `scraped_elements`
- Verifies screen exists in `screen_contexts`
- Aborts if parent records missing

##### `recordStateChange(event: AccessibilityEvent, stateType: String)`
**Purpose**: Record element state change (checked, selected, etc.)

**State Types Tracked**:
- `CHECKED`: Checkbox/switch state
- `SELECTED`: Selection state
- `ENABLED`: Enabled/disabled state
- `FOCUSED`: Focus state
- `VISIBLE`: Visibility state

**Only Records**: Actual state changes (old value != new value)

##### `trackContentChanges(event: AccessibilityEvent)`
**Purpose**: Monitor content changes for element state tracking

**Tracks**:
- Element visibility (for interaction timing)
- Checked state changes
- Enabled state changes
- Visible state changes

##### `isActionable(node: AccessibilityNodeInfo): Boolean`
**Purpose**: Determine if node should be scraped

**Returns True If**:
- Clickable, long-clickable, editable, scrollable, OR checkable
- Has text or content description

**Use Case**: Filtered scraping mode (actionable elements only)

##### `calculateNodePath(node: AccessibilityNodeInfo): String`
**Purpose**: Calculate hierarchy path from root to node

**Format**: `/0/1/3` (root → 1st child → 2nd child → 4th child)

**Memory Management**: Properly recycles AccessibilityNodeInfo instances

**Example Output**: `/0/2/1`

##### `getAppVersion(packageName: String): String`
**Purpose**: Get app version name for fingerprinting

**Returns**: Version name string or "unknown"

##### `boundsToJson(bounds: Rect): String`
**Purpose**: Convert element bounds to JSON string

**Format**: `{"left":0,"top":0,"right":100,"bottom":50}`

---

### 2. AppScrapingDatabase

**Location**: `database/AppScrapingDatabase.kt`
**Type**: Room Database (abstract class)
**Version**: 8

#### Database Configuration

```kotlin
@Database(
    entities = [
        ScrapedAppEntity::class,
        ScrapedElementEntity::class,
        ScrapedHierarchyEntity::class,
        GeneratedCommandEntity::class,
        ScreenContextEntity::class,
        ElementRelationshipEntity::class,
        ScreenTransitionEntity::class,
        UserInteractionEntity::class,
        ElementStateHistoryEntity::class
    ],
    version = 8,
    exportSchema = true
)
```

#### DAO Accessors

```kotlin
abstract fun scrapedAppDao(): ScrapedAppDao
abstract fun scrapedElementDao(): ScrapedElementDao
abstract fun scrapedHierarchyDao(): ScrapedHierarchyDao
abstract fun generatedCommandDao(): GeneratedCommandDao
abstract fun screenContextDao(): ScreenContextDao
abstract fun elementRelationshipDao(): ElementRelationshipDao
abstract fun screenTransitionDao(): ScreenTransitionDao
abstract fun userInteractionDao(): UserInteractionDao
abstract fun elementStateHistoryDao(): ElementStateHistoryDao
```

#### Singleton Access

```kotlin
val database = AppScrapingDatabase.getInstance(context)
```

#### Automatic Cleanup

**Triggered**: On database open
**Retention**: 7 days (configurable via `RETENTION_DAYS`)

**Cleanup Strategy**:
1. Delete apps not scraped in 7 days (cascades to all related data)
2. Delete low-quality commands (unused, confidence < 0.3)

**Manual Cleanup**:
```kotlin
AppScrapingDatabase.performCleanup(context)
```

**Clear All Data** (testing/reset):
```kotlin
AppScrapingDatabase.clearAllData(context)
```

#### Database Statistics

```kotlin
val stats = AppScrapingDatabase.getDatabaseStats(context)
// Returns: DatabaseStats(appCount, totalElements, totalCommands, totalRelationships)
```

---

### 3. CommandGenerator

**Location**: `CommandGenerator.kt`
**Role**: NLP-based voice command generation

#### Public Methods

##### `generateCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity>`
**Purpose**: Generate all applicable commands for an element

**Command Types Generated**:
- Click commands (if `isClickable`)
- Long click commands (if `isLongClickable`)
- Input commands (if `isEditable`)
- Scroll commands (if `isScrollable`)
- Focus commands (if `isFocusable && !isClickable`)

**Minimum Confidence**: 0.2 (commands below threshold filtered out)

**Example Output**:
```kotlin
// Button with text "Submit"
[
    GeneratedCommandEntity(
        commandText = "click submit",
        actionType = "click",
        confidence = 0.95,
        synonyms = ["tap submit", "press submit", "send", "submit button"]
    )
]
```

##### `generateCommandsForElements(elements: List<ScrapedElementEntity>): List<GeneratedCommandEntity>`
**Purpose**: Batch command generation for multiple elements

**Usage**:
```kotlin
val commands = commandGenerator.generateCommandsForElements(allElements)
database.generatedCommandDao().insertBatch(commands)
```

##### `generateStateAwareCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity>` (Phase 3)
**Purpose**: Generate commands based on current element state

**Examples**:
- Checkbox **checked** → generates "uncheck [text]"
- Checkbox **unchecked** → generates "check [text]"
- Expandable **expanded** → generates "collapse [text]"
- Expandable **collapsed** → generates "expand [text]"

**State Types Supported**:
- Checkable elements (checkboxes, radio buttons, toggles)
- Expandable elements (expandable lists, accordions)
- Selectable elements (list items, tabs)

##### `generateInteractionWeightedCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity>` (Phase 3)
**Purpose**: Adjust command confidence based on interaction history

**Confidence Boosts**:

| Interaction Count | Frequency Boost |
|-------------------|-----------------|
| > 100 | +0.15 (very frequently used) |
| > 50 | +0.10 (frequently used) |
| > 20 | +0.05 (moderately used) |
| > 5 | +0.02 (occasionally used) |
| ≤ 5 | +0.00 (rarely used) |

**Success Rate Adjustment**:

| Success Rate | Boost/Penalty |
|--------------|---------------|
| ≥ 90% | +0.05 (very reliable) |
| ≥ 70% | +0.00 (normally reliable) |
| ≥ 50% | -0.05 (somewhat unreliable) |
| < 50% | -0.10 (very unreliable) |

#### Private Methods

##### `extractElementText(element: ScrapedElementEntity): String?`
**Purpose**: Extract meaningful text for command generation

**Priority**: `text` > `contentDescription` > `viewIdResourceName`

**View ID Processing**: Extracts readable part from resource ID
- Input: `"com.example:id/submit_button"`
- Output: `"submit button"`

##### `generateClickCommands(element, text): List<GeneratedCommandEntity>`
**Purpose**: Generate click command variations

**Verbs Used**: "click", "tap", "press", "select", "activate"

**Synonym Generation**:
- Verb variations: "click submit", "tap submit", "press submit"
- Semantic synonyms: "send", "post", "confirm" (for "submit")
- Simplified: "submit" (just the text without verb)

##### `generateLongClickCommands(element, text): List<GeneratedCommandEntity>`
**Purpose**: Generate long press commands

**Verbs**: "long press", "hold", "long click"

**Confidence**: Base confidence × 0.9 (slightly lower than click)

##### `generateInputCommands(element, text): List<GeneratedCommandEntity>`
**Purpose**: Generate text input commands

**Primary Command**: "type in [text]"

**Synonyms**: "enter [text]", "input [text]", "write [text]"

##### `generateScrollCommands(element, text): List<GeneratedCommandEntity>`
**Purpose**: Generate scroll commands

**Verbs**: "scroll", "swipe", "move"

**Confidence**: Base confidence × 0.8

##### `generateFocusCommands(element, text): List<GeneratedCommandEntity>`
**Purpose**: Generate focus commands (non-clickable focusable elements)

**Verbs**: "focus", "highlight", "go to"

**Confidence**: Base confidence × 0.7

##### `generateClickSynonyms(text: String): List<String>`
**Purpose**: Generate synonym variations using NLP rules

**Rules**:
1. Add all verb variations
2. Check button synonym dictionary for semantic matches
3. Add simplified version (text only)
4. Remove duplicates

**Button Synonym Dictionary** (partial):
```kotlin
mapOf(
    "submit" to listOf("send", "post", "confirm", "ok"),
    "cancel" to listOf("close", "dismiss", "exit", "back"),
    "next" to listOf("continue", "forward", "proceed"),
    "save" to listOf("store", "keep", "preserve"),
    "delete" to listOf("remove", "erase", "clear"),
    "search" to listOf("find", "look for", "locate"),
    "login" to listOf("sign in", "log in", "enter")
)
```

##### `calculateConfidence(text: String, element: ScrapedElementEntity): Float`
**Purpose**: Calculate command quality score (0.0 to 1.0)

**Base Confidence**: 0.5

**Factors**:

| Factor | Adjustment |
|--------|------------|
| **Text Source** | |
| Direct text label | +0.3 |
| Content description | +0.2 |
| View ID fallback | +0.1 |
| **Text Length** | |
| 5-20 characters (ideal) | +0.2 |
| 3-4 characters (short) | +0.1 |
| > 20 characters (too long) | -0.1 |
| < 3 characters (too short) | -0.2 |
| **Element Type** | |
| Button | +0.2 |
| ImageButton | +0.15 |
| EditText | +0.15 |
| Clickable TextView | +0.1 |
| **Quality Penalties** | |
| Special characters | -0.05 each |
| Numbers | -0.02 each |

**Example**:
```kotlin
// Button with text "Submit" (8 chars)
// Base: 0.5
// + Text source: +0.3 (direct text)
// + Length: +0.2 (ideal length)
// + Element type: +0.2 (button)
// = 1.2 (clamped to 1.0)
```

##### `generateCheckableCommands(element, text, isChecked): List<GeneratedCommandEntity>` (Phase 3)
**Purpose**: Generate state-aware commands for checkboxes/switches

**If Checked**:
- Primary: "uncheck [text]"
- Synonyms: "untick", "deselect", "turn off", "disable"

**If Unchecked**:
- Primary: "check [text]"
- Synonyms: "tick", "select", "turn on", "enable"

##### `generateExpandableCommands(element, text, isExpanded): List<GeneratedCommandEntity>` (Phase 3)
**Purpose**: Generate state-aware commands for expandable elements

**If Expanded**:
- Primary: "collapse [text]"
- Synonyms: "close", "fold", "minimize", "hide"

**If Collapsed**:
- Primary: "expand [text]"
- Synonyms: "open", "unfold", "show", "reveal"

##### `generateSelectableCommands(element, text, isSelected): List<GeneratedCommandEntity>` (Phase 3)
**Purpose**: Generate state-aware commands for selectable elements

**If Selected**:
- Primary: "deselect [text]"
- Synonyms: "unselect", "clear selection"

**If Not Selected**:
- Primary: "select [text]"
- Synonyms: "choose", "pick"

---

### 4. SemanticInferenceHelper

**Location**: `SemanticInferenceHelper.kt`
**Role**: AI context inference (Phase 1)

#### Public Methods

##### `inferSemanticRole(node, resourceId, text, contentDescription, className): String?`
**Purpose**: Infer element's semantic role/purpose

**Button Roles**:
- `"submit_login"`: Login buttons
- `"submit_signup"`: Registration buttons
- `"submit_payment"`: Payment/checkout buttons
- `"submit_form"`: Generic form submission
- `"navigate_back"`: Back navigation
- `"navigate_next"`: Forward navigation
- `"navigate_home"`: Home navigation
- `"navigate_menu"`: Menu button
- `"toggle_like"`: Like/favorite buttons
- `"share_content"`: Share buttons
- `"add_comment"`: Comment buttons
- `"delete_item"`: Delete buttons
- `"cancel_action"`: Cancel buttons
- `"submit_search"`: Search buttons

**EditText Roles**:
- `"input_email"`: Email fields
- `"input_password"`: Password fields
- `"input_phone"`: Phone number fields
- `"input_name"`: Name/username fields
- `"input_address"`: Address fields
- `"input_url"`: URL fields
- `"input_search"`: Search fields
- `"input_comment"`: Comment fields
- `"input_text"`: Generic text input

**CheckBox/Switch Roles**:
- `"toggle_remember"`: Remember me checkboxes
- `"toggle_agreement"`: Terms/conditions checkboxes
- `"toggle_subscription"`: Notification/subscription toggles
- `"toggle_option"`: Generic toggle

**ImageButton/ImageView Roles**:
- Clickable images analyzed same as buttons

**Keyword Matching**:
- Combines `resourceId`, `text`, and `contentDescription`
- Checks against keyword sets for each role
- Case-insensitive matching

##### `inferInputType(node, resourceId, text, contentDescription): String?`
**Purpose**: Infer input field type (editable fields only)

**Types**:
- `"password"`: Password fields (uses `node.isPassword`)
- `"email"`: Email fields
- `"phone"`: Phone number fields
- `"url"`: URL fields
- `"number"`: Numeric fields
- `"date"`: Date fields
- `"search"`: Search fields
- `"text"`: Generic text (default)

**Detection Strategy**:
1. Check `AccessibilityNodeInfo.isPassword`
2. Match keywords in resourceId and contentDescription

##### `inferVisualWeight(resourceId, text, className): String?`
**Purpose**: Infer button emphasis level (buttons only)

**Weights**:
- `"danger"`: Destructive actions (delete, remove, cancel, logout)
- `"primary"`: Main CTAs (submit, confirm, continue, save)
- `"secondary"`: Cancel/alternative actions
- `"secondary"`: Default for unclassified buttons

##### `inferIsRequired(contentDescription, text, resourceId): Boolean?`
**Purpose**: Detect if field is required

**Indicators**:
- Explicit: "required", "mandatory", "*", "必須"
- Asterisk in text/description
- Email/password fields in login/signup forms

**Returns**: `true` if required, `null` if uncertain

---

### 5. ScreenContextInferenceHelper

**Location**: `ScreenContextInferenceHelper.kt`
**Role**: Screen-level context inference (Phase 2)

#### Public Methods

##### `inferScreenType(windowTitle, activityName, elements): String?`
**Purpose**: Classify screen type from context

**Screen Types**:
- `"login"`: Login screens
- `"signup"`: Registration screens
- `"checkout"`: Checkout/payment screens
- `"cart"`: Shopping cart screens
- `"settings"`: Settings/preferences
- `"home"`: Home/main screen
- `"search"`: Search screens
- `"profile"`: User profile
- `"detail"`: Detail/info screens
- `"list"`: List/browse screens
- `"form"`: Generic forms (multiple input fields)

**Detection Strategy**:
1. Analyze window title and activity name
2. Extract all element text and content descriptions
3. Match against keyword sets
4. Return most specific match

##### `inferFormContext(elements): String?`
**Purpose**: Identify form-specific context

**Form Types**:
- `"registration"`: Registration forms
- `"payment"`: Payment/billing forms
- `"address"`: Address forms
- `"contact"`: Contact forms
- `"feedback"`: Feedback/review forms
- `"search"`: Search forms

**Detection**: Keyword matching on element text, descriptions, and resource IDs

##### `inferPrimaryAction(elements): String?`
**Purpose**: Determine primary user action on screen

**Actions**:
- `"submit"`: Form submission
- `"search"`: Search operation
- `"purchase"`: Shopping/checkout
- `"browse"`: Scrolling/browsing
- `"view"`: Viewing content (default)

**Detection Strategy**:
1. Analyze button text for action keywords
2. Check for scrollable elements (browse)
3. Default to "view"

##### `inferNavigationLevel(hasBackButton, windowTitle): Int`
**Purpose**: Calculate navigation depth

**Returns**:
- `0`: Main screen (no back button, or "home" in title)
- `1`: Nested screen (has back button)

##### `extractPlaceholderText(node): String?`
**Purpose**: Get placeholder/hint text from input field

**Returns**: `node.hintText` value

##### `inferValidationPattern(node, resourceId, inputType, className): String?`
**Purpose**: Detect expected input format (EditText only)

**Validation Patterns**:
- `"email"`: Email format
- `"password"`: Password format
- `"phone"`: Phone format
- `"url"`: URL format
- `"zip_code"`: ZIP/postal code
- `"credit_card"`: Credit card number
- `"ssn"`: Social Security Number
- `"date"`: Date format
- `"number"`: Numeric only

**Detection Strategy** (3 levels):
1. **Android inputType flags** (most reliable) - Phase 2.5
2. **Inferred inputType** from Phase 1
3. **Resource ID keywords** (fallback)

**Android InputType Constants**:
```kotlin
TYPE_TEXT_VARIATION_EMAIL_ADDRESS → "email"
TYPE_TEXT_VARIATION_PASSWORD → "password"
TYPE_NUMBER_VARIATION_PASSWORD → "password"
TYPE_TEXT_VARIATION_URI → "url"
TYPE_TEXT_VARIATION_POSTAL_ADDRESS → "zip_code"
TYPE_CLASS_PHONE → "phone"
TYPE_CLASS_NUMBER → "number"
TYPE_CLASS_DATETIME → "date"
```

##### `extractBackgroundColor(node): String?`
**Purpose**: Extract background color (future enhancement)

**Currently**: Returns `null` (AccessibilityNodeInfo doesn't expose background color)

##### `generateFormGroupId(packageName, screenHash, elementDepth, formContext): String?`
**Purpose**: Generate stable ID for form element grouping

**Format**: `{packageName}_{screenHash8}_{formContext}_depth{depth}`

**Example**: `"com.example_a1b2c3d4_registration_depth3"`

**Returns**: `null` if no formContext

---

### 6. ElementHasher (DEPRECATED)

**Location**: `ElementHasher.kt`
**Status**: Deprecated in favor of `AccessibilityFingerprint`
**Deprecation Level**: WARNING
**Planned Removal**: v3.0.0

**Replacement**:
```kotlin
// OLD (deprecated)
val hash = ElementHasher.calculateHash(node)

// NEW (recommended)
val fingerprint = AccessibilityFingerprint.fromNode(
    node = node,
    packageName = packageName,
    appVersion = appVersion,
    calculateHierarchyPath = { calculateNodePath(it) }
)
val hash = fingerprint.generateHash()
```

**Why Deprecated**:
- Lacks hierarchy path awareness
- No version scoping
- Can produce hash collisions
- No stability scoring

**Still Available Methods** (for backward compatibility):
- `calculateHash(node)`: Generate MD5 hash from node
- `calculateHash(className, viewIdResourceName, text, contentDescription)`: Generate hash from properties
- `calculateSecureHash(node)`: SHA-256 variant
- `isValidHash(hash)`: Validate hash format
- `calculateHashWithPosition(node, includePosition)`: Include bounds in hash
- `hashesMatch(hash1, hash2)`: Compare hashes
- `calculateSimilarity(hash1, hash2)`: Hamming distance similarity

---

## Database Schema

### Entity 1: ScrapedAppEntity

**Table**: `scraped_apps`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `app_id` | TEXT | PRIMARY KEY | UUID for app |
| `package_name` | TEXT | NOT NULL, INDEXED | Android package name |
| `app_name` | TEXT | NOT NULL | Human-readable app name |
| `version_code` | INTEGER | NOT NULL | App version code |
| `version_name` | TEXT | NOT NULL | App version string |
| `app_hash` | TEXT | NOT NULL, UNIQUE, INDEXED | MD5 hash of package+version |
| `first_scraped` | INTEGER | NOT NULL | First scrape timestamp (ms) |
| `last_scraped` | INTEGER | NOT NULL | Most recent scrape timestamp |
| `scrape_count` | INTEGER | NOT NULL, DEFAULT 0 | Number of times scraped |
| `element_count` | INTEGER | NOT NULL, DEFAULT 0 | Total elements discovered |
| `command_count` | INTEGER | NOT NULL, DEFAULT 0 | Total commands generated |
| `is_fully_learned` | INTEGER | NOT NULL, DEFAULT 0 | LearnApp mode completed (v3+) |
| `learn_completed_at` | INTEGER | NULL | LearnApp completion timestamp (v3+) |
| `scraping_mode` | TEXT | NOT NULL, DEFAULT 'DYNAMIC' | Current scraping mode (v3+) |

**Scraping Modes** (v3+):
- `DYNAMIC`: Automatic scraping on window changes
- `LEARN_APP`: Comprehensive UI traversal mode
- `MANUAL`: User-triggered scraping only

**Indexes**:
- `index_scraped_apps_package_name`
- `index_scraped_apps_app_hash` (UNIQUE)

---

### Entity 2: ScrapedElementEntity

**Table**: `scraped_elements`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `element_hash` | TEXT | NOT NULL, UNIQUE, INDEXED | MD5 hash (unique identifier) |
| `app_id` | TEXT | NOT NULL, INDEXED, FK → scraped_apps | Parent app |
| `uuid` | TEXT | NULL, INDEXED | Universal UUID (v4+) |
| `class_name` | TEXT | NOT NULL | Android widget class |
| `view_id_resource_name` | TEXT | NULL, INDEXED | Resource ID |
| `text` | TEXT | NULL | Visible text content |
| `content_description` | TEXT | NULL | Accessibility description |
| `bounds` | TEXT | NOT NULL | JSON bounds object |
| `is_clickable` | INTEGER | NOT NULL | Click capability flag |
| `is_long_clickable` | INTEGER | NOT NULL | Long click capability |
| `is_editable` | INTEGER | NOT NULL | Text input capability |
| `is_scrollable` | INTEGER | NOT NULL | Scroll capability |
| `is_checkable` | INTEGER | NOT NULL | Check capability |
| `is_focusable` | INTEGER | NOT NULL | Focus capability |
| `is_enabled` | INTEGER | NOT NULL | Enabled state |
| `depth` | INTEGER | NOT NULL | Hierarchy depth |
| `index_in_parent` | INTEGER | NOT NULL | Sibling index |
| `scraped_at` | INTEGER | NOT NULL, DEFAULT now | Scrape timestamp |
| `semantic_role` | TEXT | NULL | AI-inferred role (v5+) |
| `input_type` | TEXT | NULL | AI-inferred input type (v5+) |
| `visual_weight` | TEXT | NULL | AI-inferred emphasis (v5+) |
| `is_required` | INTEGER | NULL | Required field flag (v5+) |
| `form_group_id` | TEXT | NULL | Form grouping ID (v6+) |
| `placeholder_text` | TEXT | NULL | Placeholder/hint text (v6+) |
| `validation_pattern` | TEXT | NULL | Expected input pattern (v6+) |
| `background_color` | TEXT | NULL | Background color hex (v6+) |

**Foreign Keys**:
- `app_id` → `scraped_apps(app_id)` ON DELETE CASCADE

**Indexes**:
- `index_scraped_elements_app_id`
- `index_scraped_elements_element_hash` (UNIQUE)
- `index_scraped_elements_view_id_resource_name`
- `index_scraped_elements_uuid`

**Bounds JSON Format**:
```json
{
  "left": 0,
  "top": 0,
  "right": 100,
  "bottom": 50
}
```

---

### Entity 3: ScrapedHierarchyEntity

**Table**: `scraped_hierarchy`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `parent_element_id` | INTEGER | NOT NULL, INDEXED, FK → scraped_elements | Parent element DB ID |
| `child_element_id` | INTEGER | NOT NULL, INDEXED, FK → scraped_elements | Child element DB ID |
| `child_order` | INTEGER | NOT NULL | Order among siblings |
| `depth` | INTEGER | NOT NULL, DEFAULT 1 | Relative depth |
| `created_at` | INTEGER | NOT NULL, DEFAULT now | Creation timestamp |

**Foreign Keys**:
- `parent_element_id` → `scraped_elements(id)` ON DELETE CASCADE
- `child_element_id` → `scraped_elements(id)` ON DELETE CASCADE

**Indexes**:
- `index_scraped_hierarchy_parent_element_id`
- `index_scraped_hierarchy_child_element_id`

**Note**: Uses database IDs (Long) instead of element hashes for performance. See migration analysis: `/coding/DECISIONS/ScrapedHierarchy-Migration-Analysis-251010-0220.md`

---

### Entity 4: GeneratedCommandEntity

**Table**: `generated_commands`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `element_hash` | TEXT | NOT NULL, INDEXED, FK → scraped_elements | Target element hash |
| `command_text` | TEXT | NOT NULL, INDEXED | Primary command phrase |
| `action_type` | TEXT | NOT NULL, INDEXED | Action: click/long_click/type/scroll/focus |
| `confidence` | REAL | NOT NULL | AI confidence (0.0-1.0) |
| `synonyms` | TEXT | NOT NULL | JSON array of synonyms |
| `is_user_approved` | INTEGER | NOT NULL, DEFAULT 0 | User approval flag |
| `usage_count` | INTEGER | NOT NULL, DEFAULT 0 | Execution count |
| `last_used` | INTEGER | NULL | Last execution timestamp |
| `generated_at` | INTEGER | NOT NULL, DEFAULT now | Generation timestamp |

**Foreign Keys**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE

**Indexes**:
- `index_generated_commands_element_hash`
- `index_generated_commands_command_text`
- `index_generated_commands_action_type`

**Synonyms JSON Format**:
```json
["tap submit", "press submit", "send", "submit button"]
```

**Migration Note**: v1→v2 migrated from `element_id` (Long FK) to `element_hash` (String FK)

---

### Entity 5: ScreenContextEntity (v6+)

**Table**: `screen_contexts`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `screen_hash` | TEXT | NOT NULL, UNIQUE, INDEXED | MD5 screen signature |
| `app_id` | TEXT | NOT NULL, INDEXED, FK → scraped_apps | Parent app |
| `package_name` | TEXT | NOT NULL, INDEXED | App package name |
| `activity_name` | TEXT | NULL | Activity class name |
| `window_title` | TEXT | NULL | Window/header title |
| `screen_type` | TEXT | NULL, INDEXED | AI-inferred type |
| `form_context` | TEXT | NULL | Form-specific context |
| `navigation_level` | INTEGER | NOT NULL, DEFAULT 0 | Navigation depth |
| `primary_action` | TEXT | NULL | Primary user action |
| `element_count` | INTEGER | NOT NULL, DEFAULT 0 | Interactive element count |
| `has_back_button` | INTEGER | NOT NULL, DEFAULT 0 | Back button presence |
| `first_scraped` | INTEGER | NOT NULL, DEFAULT now | First scrape timestamp |
| `last_scraped` | INTEGER | NOT NULL, DEFAULT now | Last scrape timestamp |
| `visit_count` | INTEGER | NOT NULL, DEFAULT 1 | Visit frequency |

**Foreign Keys**:
- `app_id` → `scraped_apps(app_id)` ON DELETE CASCADE

**Indexes**:
- `index_screen_contexts_screen_hash` (UNIQUE)
- `index_screen_contexts_app_id`
- `index_screen_contexts_package_name`
- `index_screen_contexts_screen_type`

**Screen Hash Formula**: MD5(`packageName` + `activityName` + `windowId`)

---

### Entity 6: ElementRelationshipEntity (v6+)

**Table**: `element_relationships`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `source_element_hash` | TEXT | NOT NULL, INDEXED, FK → scraped_elements | Source element |
| `target_element_hash` | TEXT | NULL, INDEXED, FK → scraped_elements | Target element (nullable) |
| `relationship_type` | TEXT | NOT NULL, INDEXED | Relationship type |
| `relationship_data` | TEXT | NULL | Additional JSON data |
| `confidence` | REAL | NOT NULL, DEFAULT 1.0 | Inference confidence |
| `inferred_by` | TEXT | NOT NULL, DEFAULT 'accessibility_tree' | Inference method |
| `created_at` | INTEGER | NOT NULL, DEFAULT now | Creation timestamp |

**Foreign Keys**:
- `source_element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `target_element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE

**Indexes**:
- `index_element_relationships_source_element_hash`
- `index_element_relationships_target_element_hash`
- `index_element_relationships_relationship_type`
- `index_element_relationships_unique` (UNIQUE on source + target + type)

**Relationship Types**:
- `"label_for"`: Label describes input field
- `"button_submits_form"`: Button submits form inputs
- `"form_group_member"`: Elements in same form
- `"parent_child"`: Hierarchy relationship

**Inference Methods**:
- `"accessibility_tree"`: From accessibility hierarchy
- `"heuristic_proximity"`: Spatial proximity heuristic
- `"heuristic_sequence"`: Sequential order heuristic
- `"heuristic_parent_container"`: Parent container heuristic

---

### Entity 7: ScreenTransitionEntity (v7+)

**Table**: `screen_transitions`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `from_screen_hash` | TEXT | NOT NULL, INDEXED, FK → screen_contexts | Source screen |
| `to_screen_hash` | TEXT | NOT NULL, INDEXED, FK → screen_contexts | Destination screen |
| `transition_count` | INTEGER | NOT NULL, DEFAULT 1 | Number of transitions |
| `first_transition` | INTEGER | NOT NULL, DEFAULT now | First transition time |
| `last_transition` | INTEGER | NOT NULL, DEFAULT now | Most recent transition |
| `avg_transition_time` | INTEGER | NULL | Average transition duration (ms) |

**Foreign Keys**:
- `from_screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE
- `to_screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

**Indexes**:
- `index_screen_transitions_from_screen_hash`
- `index_screen_transitions_to_screen_hash`
- `index_screen_transitions_unique` (UNIQUE on from + to)

**Use Cases**:
- Navigation flow analysis
- User journey mapping
- Screen frequency analysis
- Performance monitoring

---

### Entity 8: UserInteractionEntity (v8+)

**Table**: `user_interactions`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `element_hash` | TEXT | NOT NULL, INDEXED, FK → scraped_elements | Interacted element |
| `screen_hash` | TEXT | NOT NULL, INDEXED, FK → screen_contexts | Screen context |
| `interaction_type` | TEXT | NOT NULL, INDEXED | Interaction type |
| `interaction_time` | INTEGER | NOT NULL, INDEXED, DEFAULT now | Interaction timestamp |
| `visibility_start` | INTEGER | NULL | When element became visible |
| `visibility_duration` | INTEGER | NULL | Time visible before interaction (ms) |
| `success` | INTEGER | NOT NULL, DEFAULT 1 | Success flag |
| `created_at` | INTEGER | NOT NULL, DEFAULT now | Record creation time |

**Foreign Keys**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

**Indexes**:
- `index_user_interactions_element_hash`
- `index_user_interactions_screen_hash`
- `index_user_interactions_interaction_type`
- `index_user_interactions_interaction_time`

**Interaction Types** (see `InteractionType` object):
- `"click"`: Standard tap/click
- `"long_press"`: Press and hold
- `"swipe"`: Swipe gesture
- `"focus"`: Element received focus
- `"scroll"`: Scroll event
- `"double_tap"`: Double tap
- `"voice_command"`: Voice command executed

**Battery Optimization**: Only records when battery > 20% and user setting enabled

---

### Entity 9: ElementStateHistoryEntity (v8+)

**Table**: `element_state_history`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Database ID |
| `element_hash` | TEXT | NOT NULL, INDEXED, FK → scraped_elements | Element that changed |
| `screen_hash` | TEXT | NOT NULL, INDEXED, FK → screen_contexts | Screen context |
| `state_type` | TEXT | NOT NULL, INDEXED | State type |
| `old_value` | TEXT | NULL | Previous state value |
| `new_value` | TEXT | NULL | New state value |
| `changed_at` | INTEGER | NOT NULL, INDEXED, DEFAULT now | Change timestamp |
| `triggered_by` | TEXT | NULL | Trigger source |

**Foreign Keys**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

**Indexes**:
- `index_element_state_history_element_hash`
- `index_element_state_history_screen_hash`
- `index_element_state_history_state_type`
- `index_element_state_history_changed_at`

**State Types** (see `StateType` object):
- `"checked"`: Checkbox/switch state
- `"selected"`: Selection state
- `"enabled"`: Enabled/disabled state
- `"focused"`: Focus state
- `"visible"`: Visibility state
- `"expanded"`: Expand/collapse state

**Trigger Sources** (see `TriggerSource` object):
- `"user_click"`: User click/tap
- `"user_keyboard"`: User text input
- `"user_gesture"`: User gesture
- `"user_voice"`: Voice command
- `"system"`: System-triggered
- `"app_event"`: App-triggered
- `"unknown"`: Unknown source

---

## DAO Reference

### ScrapedAppDao

**Location**: `dao/ScrapedAppDao.kt`

#### Insert/Update Operations

```kotlin
suspend fun insert(app: ScrapedAppEntity): Long
suspend fun update(app: ScrapedAppEntity)
suspend fun incrementScrapeCount(appId: String)
suspend fun updateElementCount(appId: String, count: Int)
suspend fun updateCommandCount(appId: String, count: Int)
suspend fun updateScrapingMode(appId: String, mode: String)
suspend fun markAsFullyLearned(appId: String, timestamp: Long)
```

#### Query Operations

```kotlin
suspend fun getAppById(appId: String): ScrapedAppEntity?
suspend fun getAppByHash(appHash: String): ScrapedAppEntity?
suspend fun getAppByPackageName(packageName: String): List<ScrapedAppEntity>
suspend fun getAllApps(): List<ScrapedAppEntity>
suspend fun getRecentApps(limit: Int): List<ScrapedAppEntity>
suspend fun getAppCount(): Int
```

#### Delete Operations

```kotlin
suspend fun deleteApp(appId: String)
suspend fun deleteAppsOlderThan(timestamp: Long): Int
```

---

### ScrapedElementDao

**Location**: `dao/ScrapedElementDao.kt`

#### Insert/Update Operations

```kotlin
suspend fun insert(element: ScrapedElementEntity): Long
suspend fun insertBatch(elements: List<ScrapedElementEntity>)
suspend fun insertBatchWithIds(elements: List<ScrapedElementEntity>): List<Long>
suspend fun update(element: ScrapedElementEntity)
suspend fun upsertElement(element: ScrapedElementEntity): String
suspend fun updateFormGroupId(elementHash: String, formGroupId: String?)
suspend fun updateFormGroupIdBatch(elementHashes: List<String>, formGroupId: String?)
```

**`insertBatchWithIds()`**: Returns database-assigned IDs in same order as input. Critical for hierarchy insertion with valid foreign keys.

**`upsertElement()`**: Insert or update based on hash. Used for LearnApp mode merging.

#### Query Operations

```kotlin
suspend fun getElementById(id: Long): ScrapedElementEntity?
suspend fun getElementByHash(hash: String): ScrapedElementEntity?
suspend fun getElementsByAppId(appId: String): List<ScrapedElementEntity>
suspend fun getClickableElements(appId: String): List<ScrapedElementEntity>
suspend fun getEditableElements(appId: String): List<ScrapedElementEntity>
suspend fun getScrollableElements(appId: String): List<ScrapedElementEntity>
suspend fun getElementsByClassName(appId: String, className: String): List<ScrapedElementEntity>
suspend fun getElementsByViewId(appId: String, viewId: String): List<ScrapedElementEntity>
suspend fun getElementsByTextContaining(appId: String, text: String): List<ScrapedElementEntity>
suspend fun getElementsByContentDescription(appId: String, description: String): List<ScrapedElementEntity>
suspend fun getElementsByDepth(appId: String, depth: Int): List<ScrapedElementEntity>
suspend fun getElementCountForApp(appId: String): Int
suspend fun elementHashExists(hash: String): Boolean
```

#### Delete Operations

```kotlin
suspend fun deleteElementsForApp(appId: String)
suspend fun deleteElementsOlderThan(timestamp: Long): Int
```

---

### ScrapedHierarchyDao

**Location**: `dao/ScrapedHierarchyDao.kt`

#### Insert Operations

```kotlin
suspend fun insert(hierarchy: ScrapedHierarchyEntity): Long
suspend fun insertBatch(hierarchy: List<ScrapedHierarchyEntity>)
```

#### Query Operations

```kotlin
suspend fun getChildrenForElement(parentElementId: Long): List<ScrapedHierarchyEntity>
suspend fun getParentForElement(childElementId: Long): ScrapedHierarchyEntity?
suspend fun getSiblingsForElement(elementId: Long): List<ScrapedHierarchyEntity>
suspend fun getHierarchyDepth(elementId: Long): Int
suspend fun getRelationshipCount(): Int
```

#### Delete Operations

```kotlin
suspend fun deleteHierarchyForElement(elementId: Long)
suspend fun deleteAll()
```

---

### GeneratedCommandDao

**Location**: `dao/GeneratedCommandDao.kt`

#### Insert/Update Operations

```kotlin
suspend fun insert(command: GeneratedCommandEntity): Long
suspend fun insertBatch(commands: List<GeneratedCommandEntity>)
suspend fun update(command: GeneratedCommandEntity)
suspend fun incrementUsage(commandId: Long, timestamp: Long = System.currentTimeMillis())
suspend fun markAsUserApproved(commandId: Long)
suspend fun updateConfidence(commandId: Long, confidence: Float)
```

#### Query Operations

```kotlin
suspend fun getCommandById(id: Long): GeneratedCommandEntity?
suspend fun getAll(): List<GeneratedCommandEntity>
suspend fun getAllCommands(): List<GeneratedCommandEntity>
suspend fun getCommandsForElement(elementHash: String): List<GeneratedCommandEntity>
suspend fun getCommandByText(commandText: String): GeneratedCommandEntity?
suspend fun searchCommandsByText(searchText: String): List<GeneratedCommandEntity>
suspend fun getCommandsByActionType(actionType: String): List<GeneratedCommandEntity>
suspend fun getCommandsForApp(appId: String): List<GeneratedCommandEntity>
suspend fun getUserApprovedCommands(): List<GeneratedCommandEntity>
suspend fun getMostUsedCommands(limit: Int): List<GeneratedCommandEntity>
suspend fun getHighConfidenceCommands(threshold: Float): List<GeneratedCommandEntity>
suspend fun getRecentlyUsedCommands(limit: Int): List<GeneratedCommandEntity>
suspend fun getCommandCountForElement(elementHash: String): Int
suspend fun getCommandCountForApp(appId: String): Int
suspend fun getTotalCommandCount(): Int
```

#### Delete Operations

```kotlin
suspend fun deleteCommandsForElement(elementHash: String)
suspend fun deleteCommandsForApp(appId: String)
suspend fun deleteCommandsOlderThan(timestamp: Long): Int
suspend fun deleteLowQualityCommands(threshold: Float = 0.3f): Int
```

---

### ScreenContextDao (v6+)

**Location**: `dao/ScreenContextDao.kt`

#### Insert/Update Operations

```kotlin
suspend fun insert(screenContext: ScreenContextEntity): Long
suspend fun update(screenContext: ScreenContextEntity)
suspend fun incrementVisitCount(screenHash: String, timestamp: Long)
```

#### Query Operations

```kotlin
suspend fun getByScreenHash(screenHash: String): ScreenContextEntity?
suspend fun getScreensForApp(appId: String): List<ScreenContextEntity>
suspend fun getScreensByType(screenType: String): List<ScreenContextEntity>
suspend fun getMostVisitedScreens(appId: String, limit: Int = 10): List<ScreenContextEntity>
suspend fun getRecentScreens(limit: Int = 20): List<ScreenContextEntity>
suspend fun getScreenCount(): Int
suspend fun getScreenCountForApp(appId: String): Int
```

#### Delete Operations

```kotlin
suspend fun deleteScreensForApp(appId: String): Int
suspend fun deleteOldScreens(timestamp: Long): Int
```

---

### ElementRelationshipDao (v6+)

**Location**: `dao/ElementRelationshipDao.kt`

#### Insert Operations

```kotlin
suspend fun insert(relationship: ElementRelationshipEntity): Long
suspend fun insertAll(relationships: List<ElementRelationshipEntity>): List<Long>
```

#### Query Operations

```kotlin
suspend fun getRelationshipsForElement(elementHash: String): List<ElementRelationshipEntity>
suspend fun getRelationshipsByType(elementHash: String, type: String): List<ElementRelationshipEntity>
suspend fun getIncomingRelationships(elementHash: String): List<ElementRelationshipEntity>
suspend fun getFormGroupMembers(elementHash: String): List<ElementRelationshipEntity>
suspend fun getSubmitButtonForForm(formElementHash: String): ElementRelationshipEntity?
suspend fun getLabelForInput(inputElementHash: String): ElementRelationshipEntity?
suspend fun getRelationshipCount(): Int
suspend fun getRelationshipCountByType(type: String): Int
```

#### Delete Operations

```kotlin
suspend fun deleteRelationshipsForElement(elementHash: String): Int
suspend fun deleteRelationshipsByType(type: String): Int
suspend fun deleteLowConfidenceRelationships(threshold: Float): Int
```

---

### ScreenTransitionDao (v7+)

**Location**: `dao/ScreenTransitionDao.kt`

#### Insert/Update Operations

```kotlin
suspend fun recordTransition(fromHash: String, toHash: String, transitionTime: Long?)
```

**Note**: Automatically creates or updates transition record. Increments count and updates average time.

#### Query Operations

```kotlin
suspend fun getTransitionsFrom(screenHash: String): List<ScreenTransitionEntity>
suspend fun getTransitionsTo(screenHash: String): List<ScreenTransitionEntity>
suspend fun getTransition(fromHash: String, toHash: String): ScreenTransitionEntity?
suspend fun getMostFrequentTransitions(limit: Int = 20): List<ScreenTransitionEntity>
suspend fun getTransitionCount(): Int
```

#### Delete Operations

```kotlin
suspend fun deleteTransitionsForScreen(screenHash: String): Int
suspend fun deleteOldTransitions(timestamp: Long): Int
```

---

### UserInteractionDao (v8+)

**Location**: `dao/UserInteractionDao.kt`

#### Insert Operations

```kotlin
suspend fun insert(interaction: UserInteractionEntity): Long
suspend fun insertAll(interactions: List<UserInteractionEntity>)
```

**IMPORTANT**: Parent records (`element_hash` in `scraped_elements`, `screen_hash` in `screen_contexts`) must exist before inserting. FK constraint enforced with `OnConflictStrategy.ABORT`.

#### Query Operations

```kotlin
suspend fun getInteractionsForElement(elementHash: String): List<UserInteractionEntity>
suspend fun getInteractionsForScreen(screenHash: String): List<UserInteractionEntity>
suspend fun getInteractionsByType(type: String, limit: Int = 100): List<UserInteractionEntity>
suspend fun getRecentInteractions(limit: Int = 100): List<UserInteractionEntity>
suspend fun getInteractionsInTimeRange(startTime: Long, endTime: Long): List<UserInteractionEntity>
suspend fun getInteractionCount(elementHash: String): Int
suspend fun getInteractionCountByType(elementHash: String, type: String): Int
suspend fun getLastInteraction(elementHash: String): UserInteractionEntity?
suspend fun getMostInteractedElements(screenHash: String, limit: Int = 20): List<ElementInteractionCount>
suspend fun getSuccessFailureRatio(elementHash: String): InteractionRatio?
suspend fun getAverageVisibilityDuration(elementHash: String): Long?
```

**Data Classes**:
```kotlin
data class ElementInteractionCount(val element_hash: String, val interaction_count: Int)
data class InteractionRatio(val successful: Int, val failed: Int)
```

#### Delete Operations

```kotlin
suspend fun deleteOldInteractions(cutoffTime: Long): Int
```

---

### ElementStateHistoryDao (v8+)

**Location**: `dao/ElementStateHistoryDao.kt`

#### Insert Operations

```kotlin
suspend fun insert(stateChange: ElementStateHistoryEntity): Long
suspend fun insertAll(stateChanges: List<ElementStateHistoryEntity>)
```

**IMPORTANT**: Parent records (`element_hash` in `scraped_elements`, `screen_hash` in `screen_contexts`) must exist before inserting. FK constraint enforced with `OnConflictStrategy.ABORT`.

#### Query Operations

```kotlin
suspend fun getStateHistoryForElement(elementHash: String): List<ElementStateHistoryEntity>
suspend fun getStateHistoryByType(elementHash: String, stateType: String): List<ElementStateHistoryEntity>
suspend fun getCurrentState(elementHash: String, stateType: String): ElementStateHistoryEntity?
suspend fun getStateHistoryForScreen(screenHash: String): List<ElementStateHistoryEntity>
suspend fun getStateChangesInTimeRange(startTime: Long, endTime: Long): List<ElementStateHistoryEntity>
suspend fun getStateChangesByTrigger(triggerSource: String, limit: Int = 100): List<ElementStateHistoryEntity>
suspend fun getUserTriggeredStateChanges(elementHash: String): List<ElementStateHistoryEntity>
suspend fun getStateChangeCount(elementHash: String): Int
suspend fun getStateChangeCountByType(elementHash: String, stateType: String): Int
suspend fun getVolatileElements(threshold: Int = 5, limit: Int = 20): List<ElementStateChangeCount>
suspend fun getToggleFrequency(elementHash: String, stateType: String): Int
suspend fun getLastStateChangeTime(elementHash: String, stateType: String): Long?
suspend fun hasStateChangedRecently(elementHash: String, stateType: String, cutoffTime: Long): Boolean
suspend fun getStateChangePattern(elementHash: String, stateType: String, limit: Int = 50): List<String?>
```

**Data Classes**:
```kotlin
data class ElementStateChangeCount(val element_hash: String, val change_count: Int)
```

**CRITICAL**: `getCurrentState()` is essential for state-aware command generation.

#### Delete Operations

```kotlin
suspend fun deleteOldStateChanges(cutoffTime: Long): Int
```

---

## Helper Classes

### AppHashCalculator

**Location**: `AppHashCalculator.kt`

**Purpose**: Calculate unique hash for app identification

```kotlin
fun calculateAppHash(packageName: String, versionCode: Int): String
```

**Formula**: MD5(`packageName` + `versionCode`)

**Example**:
```kotlin
val appHash = AppHashCalculator.calculateAppHash("com.example.app", 42)
// Result: "a1b2c3d4e5f6..."
```

---

### VoiceCommandProcessor

**Location**: `VoiceCommandProcessor.kt`

**Purpose**: Execute voice commands on UI elements

#### Public Methods

```kotlin
suspend fun processCommand(voiceInput: String): CommandResult
suspend fun executeTextInput(targetCommand: String, text: String): CommandResult
```

**Command Processing**:
1. Normalize voice input (lowercase, trim)
2. Search database for matching command
3. Retrieve target element by hash
4. Find element in current accessibility tree
5. Perform action (click, type, scroll, etc.)
6. Increment usage count
7. Return result

**CommandResult**:
```kotlin
data class CommandResult(
    val success: Boolean,
    val message: String,
    val executedCommand: GeneratedCommandEntity? = null
)
```

---

### ScrapingMode

**Location**: `ScrapingMode.kt`

**Purpose**: Define scraping operation modes

```kotlin
enum class ScrapingMode {
    DYNAMIC,      // Automatic on window changes
    LEARN_APP,    // Comprehensive traversal
    MANUAL        // User-triggered only
}
```

---

## Data Flow

### 1. Window Change → Element Storage

```
┌──────────────────┐
│  Window Change   │ (TYPE_WINDOW_STATE_CHANGED)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ onAccessibilityEvent()           │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ scrapeCurrentWindow()            │
│ • Get root node                  │
│ • Calculate app hash             │
│ • Check if app exists            │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ scrapeNode() (recursive)         │
│ • Calculate element hash         │
│ • Check database cache           │
│ • Infer AI context               │
│ • Generate UUID                  │
│ • Traverse children              │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ insertBatchWithIds()             │
│ • Insert elements                │
│ • Capture database IDs           │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Build Hierarchy                  │
│ • Map list indices → DB IDs      │
│ • Insert hierarchy relationships │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Generate Commands                │
│ • Create voice commands          │
│ • Insert into database           │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Screen Context (Phase 2)         │
│ • Infer screen type              │
│ • Detect form context            │
│ • Create/update screen record    │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Element Relationships (Phase 2.5)│
│ • Infer button→form links        │
│ • Infer label→input links        │
│ • Insert relationships           │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Screen Transitions (Phase 2.5)   │
│ • Track screen→screen navigation │
│ • Record transition time         │
└──────────────────────────────────┘
```

---

### 2. Voice Command → Action Execution

```
┌──────────────────┐
│  Voice Input     │ ("click submit button")
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ processVoiceCommand()            │
│ • Normalize input                │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Query Database                   │
│ • Search by command_text         │
│ • Find matching command          │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Retrieve Element                 │
│ • Get element by hash            │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Find in Accessibility Tree       │
│ • Search current screen          │
│ • Match by hash/UUID             │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Execute Action                   │
│ • performAction(CLICK/TYPE/etc.) │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Update Statistics                │
│ • Increment usage count          │
│ • Update last_used timestamp     │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Return Result                    │
│ • Success/failure message        │
└──────────────────────────────────┘
```

---

### 3. User Interaction → Learning (Phase 3)

```
┌──────────────────┐
│  User Click      │ (TYPE_VIEW_CLICKED)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ onAccessibilityEvent()           │
│ • Check learning enabled         │
│ • Check battery level > 20%      │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ recordInteraction()              │
│ • Calculate element hash         │
│ • Get screen hash                │
│ • Retrieve visibility start time │
│ • Calculate duration             │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Validate Foreign Keys            │
│ • Check element exists           │
│ • Check screen exists            │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Insert UserInteractionEntity     │
│ • Save to database               │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Future: Confidence Scoring       │
│ • Boost frequently used elements │
│ • Adjust based on success rate   │
└──────────────────────────────────┘
```

---

### 4. State Change → History Tracking (Phase 3)

```
┌──────────────────┐
│  State Change    │ (TYPE_VIEW_SELECTED)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────┐
│ onAccessibilityEvent()           │
│ • Check learning enabled         │
│ • Check battery level > 20%      │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ recordStateChange()              │
│ • Get previous state from tracker│
│ • Get new state from node        │
│ • Compare old vs new             │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Only If Changed                  │
│ • Validate foreign keys          │
│ • Insert state history record    │
│ • Update state tracker           │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ Future: State-Aware Commands     │
│ • Query current state            │
│ • Generate appropriate command   │
│ • ("check" vs "uncheck")         │
└──────────────────────────────────┘
```

---

## Usage Examples

### Example 1: Basic Integration Setup

```kotlin
class VoiceAccessibilityService : AccessibilityService() {

    private lateinit var scrapingIntegration: AccessibilityScrapingIntegration

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize scraping integration
        scrapingIntegration = AccessibilityScrapingIntegration(this, this)

        Log.i(TAG, "Scraping integration initialized")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Forward events to scraping integration
        scrapingIntegration.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        scrapingIntegration.cleanup()
    }
}
```

---

### Example 2: Voice Command Processing

```kotlin
class VoiceCommandActivity : AppCompatActivity() {

    private lateinit var scrapingIntegration: AccessibilityScrapingIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scrapingIntegration = AccessibilityScrapingIntegration(
            context = this,
            accessibilityService = getAccessibilityService()
        )
    }

    private suspend fun handleVoiceInput(spokenText: String) {
        val result = scrapingIntegration.processVoiceCommand(spokenText)

        if (result.success) {
            showToast("Command executed: ${result.message}")
            Log.d(TAG, "Executed: ${result.executedCommand?.commandText}")
        } else {
            showToast("Command failed: ${result.message}")
        }
    }
}
```

---

### Example 3: LearnApp Mode

```kotlin
suspend fun learnTargetApp(packageName: String) {
    // Trigger LearnApp mode
    val result = scrapingIntegration.learnApp(packageName)

    if (result.success) {
        Log.i(TAG, """
            LearnApp Complete:
            - Total elements: ${result.elementsDiscovered}
            - New elements: ${result.newElements}
            - Updated elements: ${result.updatedElements}
            - Message: ${result.message}
        """.trimIndent())
    } else {
        Log.e(TAG, "LearnApp failed: ${result.message}")
    }
}
```

---

### Example 4: Query Commands for App

```kotlin
suspend fun getCommandsForCurrentApp(appId: String) {
    val database = AppScrapingDatabase.getInstance(context)

    // Get all commands for app
    val commands = database.generatedCommandDao().getCommandsForApp(appId)

    Log.d(TAG, "Found ${commands.size} commands for app")

    // Filter high-confidence commands
    val highQuality = commands.filter { it.confidence >= 0.7f }

    Log.d(TAG, "High confidence: ${highQuality.size}")

    // Get most used commands
    val popular = database.generatedCommandDao().getMostUsedCommands(10)

    popular.forEach { command ->
        Log.d(TAG, "Popular: ${command.commandText} (used ${command.usageCount} times)")
    }
}
```

---

### Example 5: Interaction Learning Control

```kotlin
class SettingsActivity : AppCompatActivity() {

    private lateinit var scrapingIntegration: AccessibilityScrapingIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scrapingIntegration = AccessibilityScrapingIntegration(this, getService())

        // Load current setting
        val isEnabled = scrapingIntegration.isInteractionLearningUserEnabled()

        // Setup toggle
        interactionLearningSwitch.isChecked = isEnabled
        interactionLearningSwitch.setOnCheckedChangeListener { _, enabled ->
            scrapingIntegration.setInteractionLearningEnabled(enabled)

            if (enabled) {
                showMessage("Interaction learning enabled (requires battery > 20%)")
            } else {
                showMessage("Interaction learning disabled")
            }
        }
    }
}
```

---

### Example 6: Query Interaction History

```kotlin
suspend fun analyzeElementUsage(elementHash: String) {
    val database = AppScrapingDatabase.getInstance(context)

    // Get interaction count
    val count = database.userInteractionDao().getInteractionCount(elementHash)
    Log.d(TAG, "Element interacted with $count times")

    // Get success/failure ratio
    val ratio = database.userInteractionDao().getSuccessFailureRatio(elementHash)
    if (ratio != null) {
        val successRate = ratio.successful.toFloat() / (ratio.successful + ratio.failed)
        Log.d(TAG, "Success rate: ${(successRate * 100).toInt()}%")
    }

    // Get average decision time
    val avgDuration = database.userInteractionDao().getAverageVisibilityDuration(elementHash)
    if (avgDuration != null) {
        Log.d(TAG, "Average visibility before interaction: ${avgDuration}ms")
    }

    // Get recent interactions
    val recent = database.userInteractionDao().getInteractionsForElement(elementHash)
    Log.d(TAG, "Recent interactions: ${recent.take(5).map { it.interactionType }}")
}
```

---

### Example 7: State-Aware Command Generation

```kotlin
suspend fun generateSmartCommands(element: ScrapedElementEntity) {
    val generator = CommandGenerator(context)

    // Generate state-aware commands
    val stateAwareCommands = generator.generateStateAwareCommands(element)

    stateAwareCommands.forEach { command ->
        Log.d(TAG, """
            State-aware command:
            - Text: ${command.commandText}
            - Confidence: ${command.confidence}
            - Synonyms: ${JSONArray(command.synonyms).join(", ")}
        """.trimIndent())
    }

    // Generate interaction-weighted commands
    val weightedCommands = generator.generateInteractionWeightedCommands(element)

    weightedCommands.forEach { command ->
        Log.d(TAG, "Weighted confidence: ${command.confidence}")
    }
}
```

---

### Example 8: Screen Context Analysis

```kotlin
suspend fun analyzeCurrentScreen(screenHash: String) {
    val database = AppScrapingDatabase.getInstance(context)

    // Get screen context
    val screen = database.screenContextDao().getByScreenHash(screenHash)

    if (screen != null) {
        Log.d(TAG, """
            Screen Analysis:
            - Type: ${screen.screenType}
            - Form context: ${screen.formContext}
            - Navigation level: ${screen.navigationLevel}
            - Primary action: ${screen.primaryAction}
            - Element count: ${screen.elementCount}
            - Visit count: ${screen.visitCount}
        """.trimIndent())

        // Get most interacted elements on this screen
        val topElements = database.userInteractionDao()
            .getMostInteractedElements(screenHash, limit = 5)

        topElements.forEach { (elementHash, count) ->
            Log.d(TAG, "Top element: $elementHash (${count} interactions)")
        }

        // Get outgoing transitions
        val transitions = database.screenTransitionDao().getTransitionsFrom(screenHash)
        Log.d(TAG, "Can navigate to ${transitions.size} screens")
    }
}
```

---

## Foreign Key Relationships

### Cascade Delete Hierarchy

```
ScrapedApp (deleted)
    ↓ CASCADE
    ├─→ ScrapedElement (all deleted)
    │       ↓ CASCADE
    │       ├─→ ScrapedHierarchy (all deleted)
    │       ├─→ GeneratedCommand (all deleted)
    │       ├─→ UserInteraction (all deleted)
    │       ├─→ ElementStateHistory (all deleted)
    │       └─→ ElementRelationship (all deleted)
    │
    └─→ ScreenContext (all deleted)
            ↓ CASCADE
            ├─→ UserInteraction (all deleted)
            ├─→ ElementStateHistory (all deleted)
            └─→ ScreenTransition (all deleted)
```

### Foreign Key Constraints

**`scraped_elements`**:
- `app_id` → `scraped_apps(app_id)` ON DELETE CASCADE

**`scraped_hierarchy`**:
- `parent_element_id` → `scraped_elements(id)` ON DELETE CASCADE
- `child_element_id` → `scraped_elements(id)` ON DELETE CASCADE

**`generated_commands`**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE

**`screen_contexts`**:
- `app_id` → `scraped_apps(app_id)` ON DELETE CASCADE

**`element_relationships`**:
- `source_element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `target_element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE

**`screen_transitions`**:
- `from_screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE
- `to_screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

**`user_interactions`**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

**`element_state_history`**:
- `element_hash` → `scraped_elements(element_hash)` ON DELETE CASCADE
- `screen_hash` → `screen_contexts(screen_hash)` ON DELETE CASCADE

### FK Constraint Enforcement

**Phase 3 Entities** (`user_interactions`, `element_state_history`):
- Use `OnConflictStrategy.ABORT` (throws exception on FK violation)
- Requires explicit parent record validation before insert
- See `recordInteraction()` and `recordStateChange()` for validation examples

---

## Migration History

### Migration 1 → 2 (Hash-Based Foreign Keys)

**Purpose**: Migrate GeneratedCommand from element ID (Long) to element hash (String)

**Changes**:
1. Add UNIQUE constraint to `scraped_elements.element_hash`
2. Create new `generated_commands` table with `element_hash` FK
3. Migrate data by joining with `scraped_elements`
4. Drop old table, rename new table
5. Recreate indexes

**Why**: Enable hash-based command lookup (O(1) performance)

**Note**: ScrapedHierarchy remains with Long ID FKs for performance

---

### Migration 2 → 3 (LearnApp Mode Support)

**Purpose**: Add columns for LearnApp tracking

**Changes**:
1. Add `is_fully_learned` column (default: false)
2. Add `learn_completed_at` column (nullable)
3. Add `scraping_mode` column (default: "DYNAMIC")

**Use Case**: Track which apps have been fully learned vs dynamically scraped

---

### Migration 3 → 4 (UUID Integration)

**Purpose**: Add UUID support for universal element identification

**Changes**:
1. Add `uuid` column to `scraped_elements` (nullable)
2. Create index on `uuid` column

**Integration**: UUIDs generated on next scrape for existing elements

---

### Migration 4 → 5 (AI Context - Phase 1)

**Purpose**: Add AI context inference fields

**Changes**:
1. Add `semantic_role` column (nullable)
2. Add `input_type` column (nullable)
3. Add `visual_weight` column (nullable)
4. Add `is_required` column (nullable)

**Use Case**: Enable semantic understanding of UI elements

---

### Migration 5 → 6 (AI Context - Phase 2)

**Purpose**: Add screen-level context and element relationships

**Changes**:
1. Add Phase 2 fields to `scraped_elements`:
   - `form_group_id`
   - `placeholder_text`
   - `validation_pattern`
   - `background_color`
2. Create `screen_contexts` table
3. Create `element_relationships` table
4. Create appropriate indexes

**Use Case**: Screen type detection, form grouping, element relationships

---

### Migration 6 → 7 (Screen Transitions - Phase 2.5)

**Purpose**: Add screen transition tracking

**Changes**:
1. Create `screen_transitions` table
2. Create indexes for from/to screen hashes
3. Create unique constraint on from+to pair

**Use Case**: Navigation flow analysis, user journey mapping

---

### Migration 7 → 8 (User Interactions - Phase 3)

**Purpose**: Add user interaction and state tracking

**Changes**:
1. Create `user_interactions` table
2. Create `element_state_history` table
3. Create indexes for efficient querying
4. Add FK constraints to parent tables

**Use Case**: Confidence scoring, state-aware commands, interaction learning

---

## Performance Considerations

### 1. Hash-Based Lookups

**Optimization**: Element lookup by hash is O(1) using unique index

```kotlin
// Fast lookup (indexed)
val element = dao.getElementByHash(hash)  // O(1)

// Slow lookup (sequential scan)
val elements = dao.getElementsByTextContaining(appId, "button")  // O(n)
```

**Best Practice**: Always use hash-based lookups when possible

---

### 2. Batch Operations

**Optimization**: Use batch insert for multiple elements

```kotlin
// ❌ Slow (N database transactions)
elements.forEach { dao.insert(it) }

// ✅ Fast (1 database transaction)
dao.insertBatch(elements)
```

**Use Cases**:
- Scraping window (100+ elements)
- Command generation (100+ commands)
- Relationship creation (50+ relationships)

---

### 3. Cache Hit Rate

**Phase 1 Deduplication**: Hash-based cache checking prevents redundant scraping

```
Metrics Example:
- Elements found: 156
- Cached (skipped): 132 (85% cache hit rate)
- Newly scraped: 24
- Time saved: ~200ms
```

**Monitoring**:
```kotlin
Log.i(TAG, "📊 METRICS: Found=${metrics.elementsFound}, " +
    "Cached=${metrics.elementsCached}, Scraped=${metrics.elementsScraped}")
```

---

### 4. Foreign Key Performance

**ScrapedHierarchy Design**: Uses Long ID FKs instead of String hash FKs

**Why**:
- Integer joins faster than string joins
- Smaller index size
- Better query performance

**Trade-off**: Requires ID capture during insertion (`insertBatchWithIds()`)

---

### 5. Index Strategy

**Indexed Columns**:
- All foreign keys (enables fast joins)
- All hash columns (enables fast lookups)
- Frequently queried columns (screen_type, interaction_type, etc.)

**Non-Indexed**:
- AI context fields (used for filtering, not lookups)
- JSON columns (not queryable)

---

### 6. Query Optimization

**Use LIMIT**: Prevent excessive memory usage

```kotlin
// ❌ Could return thousands of records
val all = dao.getAllCommands()

// ✅ Limited result set
val recent = dao.getMostUsedCommands(limit = 100)
```

**Use Indexes**:
```kotlin
// ✅ Uses index on command_text
val command = dao.getCommandByText("click submit")

// ❌ Sequential scan (LIKE operator)
val fuzzy = dao.searchCommandsByText("submit")
```

---

### 7. Automatic Cleanup

**Retention Strategy**: 7-day retention prevents database bloat

**Cleanup Triggers**:
- Database open (automatic)
- Manual cleanup call
- User-initiated data clear

**What's Cleaned**:
- Apps not scraped in 7 days (cascades to all related data)
- Unused low-confidence commands (confidence < 0.3, usage_count = 0)

**Performance Impact**: Cleanup runs in background coroutine (non-blocking)

---

### 8. Interaction Learning Battery Optimization

**Phase 3**: Only records interactions when battery > 20%

**Implementation**:
```kotlin
private fun isInteractionLearningEnabled(): Boolean {
    val userEnabled = preferences.getBoolean(PREF_INTERACTION_LEARNING_ENABLED, true)
    if (!userEnabled) return false

    val batteryLevel = getBatteryLevel()
    return batteryLevel > MIN_BATTERY_LEVEL_FOR_LEARNING  // 20%
}
```

**Impact**: Reduces database writes during low battery

---

## Troubleshooting

### Issue 1: FK Constraint Failure on UserInteractionEntity

**Error**: `SQLiteConstraintException: FOREIGN KEY constraint failed`

**Cause**: Parent record missing in `scraped_elements` or `screen_contexts`

**Solution**: Validate parent records before insert

```kotlin
// ✅ Validate before insert
val elementExists = database.scrapedElementDao().getElementByHash(elementHash) != null
val screenExists = database.screenContextDao().getByScreenHash(screenHash) != null

if (!elementExists || !screenExists) {
    Log.w(TAG, "Cannot record interaction: missing parent records")
    return
}

database.userInteractionDao().insert(interaction)
```

---

### Issue 2: Hash Collisions (Rare)

**Symptom**: Elements with different properties have same hash

**Cause**: Using deprecated `ElementHasher` without hierarchy path

**Solution**: Use `AccessibilityFingerprint` instead

```kotlin
// ✅ Includes hierarchy path (prevents collisions)
val fingerprint = AccessibilityFingerprint.fromNode(
    node, packageName, appVersion, ::calculateNodePath
)
val hash = fingerprint.generateHash()
```

---

### Issue 3: Memory Leaks from AccessibilityNodeInfo

**Symptom**: OutOfMemoryError during scraping

**Cause**: Not recycling AccessibilityNodeInfo instances

**Solution**: Always call `.recycle()` in finally blocks

```kotlin
val node = parent.getChild(i) ?: continue
try {
    scrapeNode(node, ...)
} finally {
    node.recycle()  // ✅ Always recycle
}
```

---

### Issue 4: Slow Command Lookup

**Symptom**: Voice command response time > 500ms

**Cause**: Using text search instead of exact match

**Solution**: Use `getCommandByText()` for exact match

```kotlin
// ✅ Fast (indexed lookup)
val command = dao.getCommandByText(normalizedInput)

// ❌ Slow (sequential scan with LIKE)
val results = dao.searchCommandsByText(partialInput)
```

---

### Issue 5: Interaction Learning Not Working

**Symptom**: No interactions recorded despite user clicks

**Possible Causes**:
1. User setting disabled
2. Battery level < 20%
3. Parent records not in database
4. Event type not tracked

**Debugging**:
```kotlin
Log.d(TAG, "Learning enabled: ${isInteractionLearningEnabled()}")
Log.d(TAG, "Battery level: ${getBatteryLevel()}%")
Log.d(TAG, "Element exists: ${dao.getElementByHash(hash) != null}")
Log.d(TAG, "Screen exists: ${dao.getByScreenHash(hash) != null}")
```

---

## Related Documentation

**Migration Analysis**:
- `/coding/DECISIONS/ScrapedHierarchy-Migration-Analysis-251010-0220.md`

**Architecture Decisions**:
- `/docs/planning/architecture/decisions/` (ADRs)

**Module Documentation**:
- `/docs/modules/VoiceOSCore/` (this module)
- `/docs/modules/UUIDCreator/` (UUID integration)

**Implementation Plans**:
- `/docs/planning/implementation/` (phase plans)

---

## Appendix: Constants and Enums

### InteractionType Constants

```kotlin
object InteractionType {
    const val CLICK = "click"
    const val LONG_PRESS = "long_press"
    const val SWIPE = "swipe"
    const val FOCUS = "focus"
    const val SCROLL = "scroll"
    const val DOUBLE_TAP = "double_tap"
    const val VOICE_COMMAND = "voice_command"
}
```

### StateType Constants

```kotlin
object StateType {
    const val CHECKED = "checked"
    const val SELECTED = "selected"
    const val ENABLED = "enabled"
    const val FOCUSED = "focused"
    const val VISIBLE = "visible"
    const val EXPANDED = "expanded"
}
```

### TriggerSource Constants

```kotlin
object TriggerSource {
    const val USER_CLICK = "user_click"
    const val USER_KEYBOARD = "user_keyboard"
    const val USER_GESTURE = "user_gesture"
    const val USER_VOICE = "user_voice"
    const val SYSTEM = "system"
    const val APP_EVENT = "app_event"
    const val UNKNOWN = "unknown"
}
```

### RelationshipType Constants

```kotlin
object RelationshipType {
    const val LABEL_FOR = "label_for"
    const val BUTTON_SUBMITS_FORM = "button_submits_form"
    const val FORM_GROUP_MEMBER = "form_group_member"
    const val PARENT_CHILD = "parent_child"
}
```

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-23 20:52 PDT
**Author**: Claude Code (VOS4 Documentation Specialist)
**Status**: Complete

**Changelog**:
- v1.0.0 (2025-10-23): Initial comprehensive developer manual
  - Complete architecture documentation
  - Function-by-function reference for all components
  - Database schema with all 9 entities
  - DAO method reference (9 DAOs)
  - Data flow diagrams
  - Usage examples
  - Foreign key relationships
  - Migration history (v1-v8)
  - Performance considerations
  - Troubleshooting guide

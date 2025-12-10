# AccessibilityScrapingIntegration - Developer Documentation

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/AccessibilityScrapingIntegration.kt`
**Package:** `com.augmentalis.voiceaccessibility.scraping`
**Purpose:** Primary integration layer between VoiceOSService and the scraping subsystem
**Dependencies:** Room Database, AccessibilityService, UUIDCreator (AccessibilityFingerprint)
**Last Analyzed:** 2025-10-10 10:34:00 PDT

## Overview

`AccessibilityScrapingIntegration` is the main entry point for the scraping subsystem. It orchestrates the entire workflow of capturing UI elements, storing them in the database, generating voice commands, and processing user voice input to execute actions. This class serves as the bridge between Android's AccessibilityService and the VOS4 scraping database.

### Key Responsibilities

1. **Automatic UI Scraping**: Responds to `TYPE_WINDOW_STATE_CHANGED` events to automatically scrape app UIs as users navigate
2. **Database Management**: Coordinates with Room database DAOs to store scraped data efficiently
3. **Command Generation**: Integrates with `CommandGenerator` to create voice commands from UI elements
4. **Command Processing**: Uses `VoiceCommandProcessor` to execute voice commands on discovered elements
5. **Duplicate Prevention**: Implements hash-based deduplication to avoid re-scraping unchanged apps
6. **LearnApp Mode**: Supports comprehensive app learning for complete UI coverage

### Architecture Position

```
VoiceOSService
         ↓
AccessibilityScrapingIntegration (THIS CLASS)
         ↓
    ┌────┴────┬─────────────┬──────────────┐
    ↓         ↓             ↓              ↓
Database  CommandGen  VoiceProcessor  AppHashCalc
```

## Class Documentation

### Class: `AccessibilityScrapingIntegration`

**Purpose:** Integrate accessibility scraping functionality with VoiceOSService
**Lifecycle:** Created in `VoiceOSService.onServiceConnected()`, cleaned up in `onServiceDisconnected()`
**Thread Safety:** Uses coroutines with `Dispatchers.IO` for database operations

**Design Decisions:**
- Uses hash-based element identification for persistent references across app restarts
- Implements 3-phase hierarchy insertion to handle Room's auto-generated IDs correctly
- Excludes system packages (launcher, systemui) to reduce database clutter
- Tracks last scraped app hash to prevent redundant scraping

**Usage Pattern:**
```kotlin
// In VoiceOSService
private lateinit var scrapingIntegration: AccessibilityScrapingIntegration

override fun onServiceConnected() {
    scrapingIntegration = AccessibilityScrapingIntegration(this, this)
}

override fun onAccessibilityEvent(event: AccessibilityEvent) {
    scrapingIntegration.onAccessibilityEvent(event)
}
```

## Public API

### `onAccessibilityEvent(event: AccessibilityEvent): Unit`

**Purpose:** Handle accessibility events from the system
**Parameters:**
- `event: AccessibilityEvent` - Accessibility event from Android system

**Returns:** `Unit`
**Threading:** Called on main thread, launches coroutine for async processing
**Called By:** `VoiceOSService.onAccessibilityEvent()`

**Behavior:**
- Filters for `TYPE_WINDOW_STATE_CHANGED` events
- Launches background coroutine to scrape window without blocking UI
- Logs package name changes

**Usage Example:**
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    scrapingIntegration.onAccessibilityEvent(event)
    // Processing happens asynchronously
}
```

---

### `processVoiceCommand(voiceInput: String): CommandResult`

**Purpose:** Process voice command and execute UI action
**Parameters:**
- `voiceInput: String` - Raw voice input from speech recognition (e.g., "click submit button")

**Returns:** `CommandResult` - Success/failure status with details
**Threading:** Suspending function, runs on `Dispatchers.IO`
**Called By:** VoiceOSService when speech recognition completes

**Behavior:**
1. Passes input to `VoiceCommandProcessor`
2. Processor matches command to database entries
3. Finds target element by hash
4. Executes appropriate action (click, type, etc.)
5. Updates usage statistics

**Usage Example:**
```kotlin
lifecycleScope.launch {
    val result = scrapingIntegration.processVoiceCommand("click login")
    if (result.success) {
        Log.i(TAG, "Command executed: ${result.message}")
    } else {
        Log.w(TAG, "Command failed: ${result.message}")
    }
}
```

---

### `processTextInput(targetCommand: String, text: String): CommandResult`

**Purpose:** Execute text input command
**Parameters:**
- `targetCommand: String` - Command to identify input field (e.g., "type in email")
- `text: String` - Text to input into the field

**Returns:** `CommandResult` - Success/failure status
**Threading:** Suspending function, runs on `Dispatchers.IO`
**Called By:** VoiceOSService for two-step text input commands

**Behavior:**
1. Identifies target element using targetCommand
2. Sets focus on the element
3. Inputs text using `ACTION_SET_TEXT`

**Usage Example:**
```kotlin
// User says: "type in email"
// User says: "john@example.com"
val result = scrapingIntegration.processTextInput("type in email", "john@example.com")
```

---

### `learnApp(packageName: String): LearnAppResult`

**Purpose:** Perform comprehensive app learning (LearnApp mode)
**Parameters:**
- `packageName: String` - Package name of app to learn (e.g., "com.example.app")

**Returns:** `LearnAppResult` - Statistics about discovered elements
**Threading:** Suspending function, runs on `Dispatchers.IO`
**Called By:** User-triggered from VoiceOSService settings/menu

**Behavior:**
1. Checks if app is already in database (creates if not)
2. Sets scraping mode to `LEARN_APP`
3. Scrapes all visible elements
4. Merges with existing data using hash-based UPSERT
5. Marks app as fully learned
6. Generates commands for new elements
7. Restores scraping mode to `DYNAMIC`

**LearnAppResult Properties:**
- `success: Boolean` - Whether operation completed successfully
- `message: String` - Human-readable result message
- `elementsDiscovered: Int` - Total elements found during scan
- `newElements: Int` - New elements inserted (not in DB before)
- `updatedElements: Int` - Existing elements updated (already in DB)

**Usage Example:**
```kotlin
lifecycleScope.launch {
    val result = scrapingIntegration.learnApp("com.spotify.music")
    Log.i(TAG, result.message)
    Log.i(TAG, "New: ${result.newElements}, Updated: ${result.updatedElements}")
}
```

---

### `cleanup(): Unit`

**Purpose:** Clean up resources when service is destroyed
**Parameters:** None
**Returns:** `Unit`
**Threading:** Can be called from any thread
**Called By:** `VoiceOSService.onServiceDisconnected()`

**Behavior:**
- Logs cleanup message
- Coroutine scope is cancelled automatically by service lifecycle

**Usage Example:**
```kotlin
override fun onServiceDisconnected() {
    scrapingIntegration.cleanup()
    super.onServiceDisconnected()
}
```

## Private Implementation

### `scrapeCurrentWindow(event: AccessibilityEvent, filterNonActionable: Boolean = false): Unit`

**Purpose:** Core scraping logic for current window
**Algorithm:**
1. **Validation Phase**
   - Get root node from event or service
   - Extract and validate package name
   - Check if package is excluded (systemui, launcher)

2. **Hash Calculation Phase**
   - Get app version from PackageManager
   - Calculate app hash (MD5 of packageName + versionCode)
   - Check if already scraped (compare with lastScrapedAppHash)

3. **Database Check Phase**
   - Query database for existing app by hash
   - If exists: increment scrape count and exit
   - If new: proceed to full scraping

4. **Scraping Phase**
   - Create app entity with UUID
   - Scrape element tree recursively
   - Track hierarchy relationships by list index

5. **Database Insertion Phase (3 Sub-phases)**
   - **Phase 1**: Insert app entity
   - **Phase 2**: Insert elements and capture database IDs
   - **Phase 3**: Map list indices to real DB IDs
   - **Phase 4**: Insert hierarchy with valid foreign keys

6. **Command Generation Phase**
   - Map elements with real database IDs
   - Generate voice commands
   - Validate all commands have element hashes
   - Insert commands to database

7. **Finalization Phase**
   - Update element count on app entity
   - Update command count on app entity
   - Set lastScrapedAppHash to prevent duplicates
   - Recycle accessibility nodes

**Complexity:** O(n) where n = number of UI elements
**Error Handling:** Try-catch wraps entire method, logs errors without crashing

---

### `scrapeNode(...): Int`

**Purpose:** Recursively scrape accessibility tree node and its children
**Parameters:**
- `node: AccessibilityNodeInfo` - Current node to scrape
- `appId: String` - App identifier
- `parentIndex: Int?` - Index of parent in elements list (null for root)
- `depth: Int` - Depth in tree (0 for root)
- `indexInParent: Int` - Index among siblings
- `elements: MutableList<ScrapedElementEntity>` - Output list
- `hierarchyBuildInfo: MutableList<HierarchyBuildInfo>` - Output hierarchy list
- `filterNonActionable: Boolean` - Skip non-actionable elements

**Returns:** `Int` - Index of this element in elements list, or -1 if skipped

**Algorithm:**
1. Check depth limit (MAX_DEPTH = 50) to prevent stack overflow
2. Optional: Skip non-actionable elements if filtering enabled
3. Extract node properties (bounds, text, className, etc.)
4. Calculate element hash using `AccessibilityFingerprint`
5. Create `ScrapedElementEntity`
6. Add to elements list and capture index
7. Track hierarchy relationship (if has parent)
8. Recursively scrape children
9. Recycle child nodes after processing

**Key Design:** Uses list indices (not database IDs) because IDs aren't assigned yet during scraping

---

### `isActionable(node: AccessibilityNodeInfo): Boolean`

**Purpose:** Determine if node is worth scraping for voice commands
**Returns:** `true` if node has interactive capability or meaningful content

**Criteria:**
- Is clickable OR
- Is long clickable OR
- Is editable OR
- Is scrollable OR
- Is checkable OR
- Has non-blank text OR
- Has non-blank content description

---

### `calculateNodePath(node: AccessibilityNodeInfo): String`

**Purpose:** Calculate hierarchy path for AccessibilityFingerprint
**Returns:** Path string (e.g., "/0/1/3" for root→1st child→2nd child→4th child)

**Algorithm:**
1. Start with current node
2. Walk up tree to root, prepending indices
3. Properly recycle AccessibilityNodeInfo objects
4. Return "/" for root, or "/x/y/z" for nested nodes

**Memory Safety:** Uses `nodesToRecycle` list to ensure all nodes are recycled

---

### `findChildIndex(parent: AccessibilityNodeInfo, child: AccessibilityNodeInfo): Int`

**Purpose:** Find index of child within parent
**Returns:** Child index (0-based) or -1 if not found
**Note:** Immediately recycles child nodes after comparison

---

### `getAppVersion(packageName: String): String`

**Purpose:** Get app version name for fingerprinting
**Returns:** Version name string, or "unknown" if not found
**Error Handling:** Catches exceptions, returns "unknown" on failure

---

### `boundsToJson(bounds: Rect): String`

**Purpose:** Convert Android Rect to JSON string
**Returns:** JSON string like `{"left":0,"top":10,"right":100,"bottom":50}`

## Data Structures

### `HierarchyBuildInfo` (Private Data Class)

**Purpose:** Track hierarchy relationships during scraping phase
**Properties:**
- `childListIndex: Int` - Index in elements list (maps to DB ID later)
- `parentListIndex: Int` - Index in elements list (maps to DB ID later)
- `childOrder: Int` - Order among siblings
- `depth: Int` - Depth in tree (always 1 for direct parent-child)

**Why Needed:** During scraping, database IDs aren't assigned yet. This tracks relationships by list indices, which are later mapped to real database IDs after insertion.

### `LearnAppResult` (Public Data Class)

**Purpose:** Return value for learnApp() operation
**Properties:**
- `success: Boolean` - Operation success status
- `message: String` - Human-readable message
- `elementsDiscovered: Int` - Total elements found
- `newElements: Int` - New insertions
- `updatedElements: Int` - Updated existing elements

## Database Interactions

### Tables Accessed

1. **scraped_apps** (via `ScrapedAppDao`)
   - INSERT: New app entries
   - SELECT: Check if app exists by hash
   - UPDATE: Increment scrape count, update element/command counts

2. **scraped_elements** (via `ScrapedElementDao`)
   - INSERT: Batch insert elements
   - SELECT: Get element by hash (for command processing)
   - UPSERT: Merge elements in LearnApp mode

3. **scraped_hierarchy** (via `ScrapedHierarchyDao`)
   - INSERT: Batch insert parent-child relationships

4. **generated_commands** (via `GeneratedCommandDao`)
   - INSERT: Batch insert generated commands
   - UPDATE: Increment usage count when command executed

### Transaction Boundaries

- **scrapeCurrentWindow()**: Single implicit transaction per app scrape
  - App insert
  - Elements batch insert
  - Hierarchy batch insert
  - Commands batch insert

- **learnApp()**: Single transaction for UPSERT operations
  - Each element UPSERT is atomic
  - Overall operation wrapped in withContext(Dispatchers.IO)

### Data Flow

```
AccessibilityEvent → scrapeCurrentWindow() →
  ↓
App Entity → Database (scraped_apps)
  ↓
Elements List → insertBatchWithIds() → Capture DB IDs
  ↓
Map Indices to IDs → Hierarchy Entities → Database (scraped_hierarchy)
  ↓
Generate Commands → Database (generated_commands)
```

## Threading Model

### Coroutine Usage

- **Scope:** `integrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`
- **Purpose:** Background processing without blocking accessibility service

**Thread Assignments:**
- `onAccessibilityEvent()` - Called on main thread, launches IO coroutine
- `scrapeCurrentWindow()` - Runs on Dispatchers.IO
- `processVoiceCommand()` - Suspending, runs on Dispatchers.IO
- `processTextInput()` - Suspending, runs on Dispatchers.IO
- `learnApp()` - Suspending, runs on Dispatchers.IO

**Synchronization:**
- Database operations are thread-safe (Room handles synchronization)
- `lastScrapedAppHash` is volatile (implicit for coroutines)
- No explicit locks needed

### Dispatcher Details

- **Dispatchers.IO**: All database operations, file I/O
- **Dispatchers.Main**: UI actions (performed via VoiceCommandProcessor)
- **SupervisorJob**: Ensures one failure doesn't cancel all coroutines

## Error Handling

### Exception Strategy

**Top-Level Try-Catch:**
- `scrapeCurrentWindow()` - Catches all exceptions, logs error, returns gracefully
- `learnApp()` - Catches exceptions, returns `LearnAppResult(success=false, message=error)`

**Specific Error Cases:**

1. **Null Root Node**
   - Log warning, return early
   - Does not throw exception

2. **Package Name Null**
   - Log warning, recycle node, return early

3. **Excluded Package**
   - Log debug message, recycle node, return early

4. **PackageManager Error**
   - Catch exception, log error, recycle node, return early

5. **Database Error**
   - Room throws exception
   - Caught by top-level try-catch
   - Logged with full stack trace

6. **Stack Overflow Prevention**
   - MAX_DEPTH = 50 limit
   - Prevents infinite recursion on malformed UI trees

### Recovery Strategies

- **Failed Scrape**: Log error, service continues normally
- **Missing Elements**: Command processing returns "Element not found"
- **Hash Collision**: Unique constraint prevents duplicates, logs warning

## Integration Points

### How VoiceOSService Uses This

```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var scrapingIntegration: AccessibilityScrapingIntegration

    override fun onServiceConnected() {
        scrapingIntegration = AccessibilityScrapingIntegration(this, this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Automatic scraping
        scrapingIntegration.onAccessibilityEvent(event)
    }

    private fun onSpeechRecognitionResult(text: String) {
        lifecycleScope.launch {
            // Voice command processing
            val result = scrapingIntegration.processVoiceCommand(text)
            showToast(result.message)
        }
    }

    private fun onLearnAppCommand(packageName: String) {
        lifecycleScope.launch {
            // Comprehensive learning
            val result = scrapingIntegration.learnApp(packageName)
            showDialog(result.message)
        }
    }

    override fun onServiceDisconnected() {
        scrapingIntegration.cleanup()
    }
}
```

### Dependencies on Other Components

**Required:**
- `AppScrapingDatabase` - Data persistence
- `CommandGenerator` - Voice command generation
- `VoiceCommandProcessor` - Command execution
- `AppHashCalculator` - App fingerprinting (deprecated, uses AccessibilityFingerprint)
- `AccessibilityFingerprint` - Modern element hashing from UUIDCreator

**Optional:**
- None (all dependencies are hard requirements)

### Callback/Listener Patterns

- **None** - This class does not use callbacks or listeners
- Uses direct method calls and suspending functions
- Results returned via return values or data classes

## Examples

### Example 1: Basic Integration

```kotlin
class MyAccessibilityService : AccessibilityService() {
    private lateinit var scraping: AccessibilityScrapingIntegration

    override fun onServiceConnected() {
        scraping = AccessibilityScrapingIntegration(this, this)
        Log.i(TAG, "Scraping integration ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        scraping.onAccessibilityEvent(event)
    }
}
```

### Example 2: Processing Voice Commands

```kotlin
private fun handleVoiceInput(userSpeech: String) {
    lifecycleScope.launch {
        val result = scrapingIntegration.processVoiceCommand(userSpeech)

        when {
            result.success -> {
                speakFeedback("Done: ${result.message}")
                playSuccessSound()
            }
            else -> {
                speakFeedback("Failed: ${result.message}")
                playErrorSound()
            }
        }
    }
}
```

### Example 3: LearnApp Mode with Progress

```kotlin
private fun learnCurrentApp() {
    val currentPackage = getCurrentPackageName() ?: return

    lifecycleScope.launch {
        showProgressDialog("Learning app...")

        val result = scrapingIntegration.learnApp(currentPackage)

        hideProgressDialog()

        if (result.success) {
            showDialog("""
                App learned successfully!

                Elements discovered: ${result.elementsDiscovered}
                New elements: ${result.newElements}
                Updated elements: ${result.updatedElements}
            """.trimIndent())
        } else {
            showDialog("Failed to learn app: ${result.message}")
        }
    }
}
```

### Example 4: Text Input with Two-Step Command

```kotlin
private var pendingTextInputCommand: String? = null

private fun handleVoiceInput(userSpeech: String) {
    lifecycleScope.launch {
        if (pendingTextInputCommand != null) {
            // Second step: user provided the text
            val result = scrapingIntegration.processTextInput(
                targetCommand = pendingTextInputCommand!!,
                text = userSpeech
            )
            pendingTextInputCommand = null
            speakFeedback(result.message)
        } else {
            // First step: identify the input field
            val result = scrapingIntegration.processVoiceCommand(userSpeech)
            if (result.actionType == "type") {
                pendingTextInputCommand = userSpeech
                speakFeedback("What text do you want to enter?")
            }
        }
    }
}
```

### Example 5: Edge Case Handling

```kotlin
private fun safelyProcessCommand(userSpeech: String) {
    lifecycleScope.launch {
        try {
            val result = scrapingIntegration.processVoiceCommand(userSpeech)

            when {
                result.success -> handleSuccess(result)
                result.message.contains("not yet learned") -> promptLearnApp()
                result.message.contains("not recognized") -> suggestAlternatives(userSpeech)
                result.message.contains("not found") -> promptRescrape()
                else -> showGenericError(result.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command processing error", e)
            speakFeedback("An error occurred. Please try again.")
        }
    }
}
```

## TODOs and Future Work

### Known Limitations

1. **Hash Collision Risk**: Using MD5 hashing (via deprecated AppHashCalculator) has collision risk. Migrated to `AccessibilityFingerprint` which uses SHA-256 with hierarchy paths.

2. **No Screen Navigation**: LearnApp mode only scrapes currently visible UI. Future enhancement could add automated navigation to discover hidden screens.

3. **No Element Staleness Detection**: If UI changes after scraping, stale elements remain in database. Could add validation before command execution.

4. **Limited Action Types**: Only supports click, long_click, type, scroll, focus. Missing: swipe, drag, pinch, rotate gestures.

5. **No Multilingual Support**: Command generation only supports English synonyms. Future: i18n support.

### Planned Improvements

1. **Smart Re-scraping**: Detect UI changes automatically instead of relying on app version changes
2. **Partial Updates**: Update only changed elements instead of full re-scrape
3. **Performance Metrics**: Track scraping time, element count, command accuracy
4. **User Feedback Loop**: Learn from user corrections to improve command generation
5. **Context-Aware Commands**: Use hierarchy to understand "click the button in the dialog"

### Technical Debt

1. **Deprecation Cleanup**: Remove `AppHashCalculator` and `ElementHasher` after full migration to `AccessibilityFingerprint` (planned for v3.0.0)
2. **Thread Safety Review**: Audit `lastScrapedAppHash` for race conditions
3. **Memory Optimization**: Large UI trees could cause OOM - implement element batching
4. **Test Coverage**: Add unit tests for edge cases (null nodes, circular hierarchies, etc.)

### Migration Notes

**For Developers Maintaining This Code:**

- **AccessibilityFingerprint Migration**: This class now uses `AccessibilityFingerprint` from UUIDCreator library instead of deprecated `AppHashCalculator`. The old calculator is still referenced for app-level hashing but should be replaced.

- **Database Schema Version**: Currently on v3 with LearnApp mode support. Ensure migrations run cleanly before making schema changes.

- **Coroutine Scope**: Uses `SupervisorJob` to prevent cascading failures. Do not replace with regular Job.

---

**Documentation Generated:** 2025-10-10 10:34:00 PDT
**VOS4 Version:** 4.0.0
**Author:** VOS4 Development Team
**Reviewed:** Pending

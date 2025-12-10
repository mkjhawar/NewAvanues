# VOS4 CommandManager Implementation - Detailed TODO

**Created:** 2025-10-09 19:34:28 PDT
**Status:** Ready for Implementation
**Priority:** HIGH
**Total Work:** 74 hours remaining
**Approach:** Manual implementation with TodoWrite tracking

---

## 📊 Quick Status

| Phase | Tasks | Hours | Status | Priority |
|-------|-------|-------|--------|----------|
| **Phase 1: Dynamic Commands** | 4 | 38 | ⏸️ | HIGH |
| **Phase 2: JSON Architecture** | 4 | 18 | 🟢 58% (7/12h base) | HIGH |
| **Phase 2.4: Critical Fixes** | 3 | 6 | ⏸️ **CANNOT IGNORE** | CRITICAL |
| **Phase 3: Scraping Integration** | 3 | 16 | ⏸️ | CRITICAL |
| **Phase 4: Testing** | 2 | 8 | ⏸️ | MEDIUM |
| **TOTAL** | **16** | **86** | **8.1% complete (7/86h)** | - |

---

## 🎯 PHASE 1: CommandManager Dynamic Features (38 hours)

### Task 1.1: Dynamic Command Registration (8 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** HIGH
**Dependencies:** None
**Estimated Lines:** ~1,200 lines

#### Files to Create:

**1. DynamicCommandRegistry.kt** (~400 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/DynamicCommandRegistry.kt
```

**Purpose:** Central registry for runtime command management

**Key Features:**
- Thread-safe command registration/unregistration
- ConcurrentHashMap for command storage
- StateFlow<List<VoiceCommand>> for reactive updates
- Priority-based command resolution
- Namespace isolation
- Registration callbacks

**Public API:**
```kotlin
class DynamicCommandRegistry {
    // Registration
    suspend fun registerCommand(command: VoiceCommand): Result<Unit>
    suspend fun unregisterCommand(commandId: String): Result<Unit>
    suspend fun registerBatch(commands: List<VoiceCommand>): Result<Unit>

    // Query
    fun resolveCommand(phrase: String): List<VoiceCommand>
    fun getCommands(namespace: String): List<VoiceCommand>
    fun getAllCommands(): StateFlow<List<VoiceCommand>>

    // Conflict detection
    fun detectConflicts(command: VoiceCommand): List<ConflictInfo>

    // Callbacks
    fun addRegistrationListener(listener: RegistrationListener)
    fun removeRegistrationListener(listener: RegistrationListener)
}

data class VoiceCommand(
    val id: String,
    val phrases: List<String>,
    val priority: Int,  // 1-100, higher = higher priority
    val namespace: String,
    val context: List<String> = emptyList(),
    val action: suspend (CommandContext) -> Unit
)

sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error<T>(val message: String, val cause: Throwable? = null) : Result<T>()
}
```

**Implementation Details:**
- Use `ConcurrentHashMap<String, VoiceCommand>` for thread safety
- Priority sorting: `commands.sortedByDescending { it.priority }`
- Phrase matching: case-insensitive, fuzzy matching with Levenshtein distance
- StateFlow updates on registration/unregistration
- Mutex for thread-safe operations

**Unit Tests Required:**
- ✅ testRegisterCommand_Success
- ✅ testRegisterCommand_DuplicateId_Fails
- ✅ testUnregisterCommand_Success
- ✅ testResolveCommand_ByPhrase_ReturnsCorrect
- ✅ testResolveCommand_MultipleMatches_SortedByPriority
- ✅ testRegisterCommand_ThreadSafety_ConcurrentRegistrations
- ✅ testStateFlowUpdates_OnRegistration
- ✅ testNamespaceIsolation_DifferentNamespaces

---

**2. CommandPriority.kt** (~200 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/CommandPriority.kt
```

**Purpose:** Priority scoring and resolution algorithms

**Key Features:**
- Priority calculation based on multiple factors
- Conflict resolution rules
- Score normalization (0.0-1.0)

**Public API:**
```kotlin
object CommandPriority {
    fun calculateScore(command: VoiceCommand, phrase: String, context: CommandContext?): Float
    fun resolvePriorityConflict(commands: List<VoiceCommand>): VoiceCommand
    fun normalizePriority(priority: Int): Float  // 1-100 → 0.0-1.0
}

data class PriorityScore(
    val basePriority: Float,      // From command.priority
    val contextMatch: Float,       // How well context matches
    val phraseMatch: Float,        // Similarity to spoken phrase
    val usageFrequency: Float,     // How often used
    val totalScore: Float          // Weighted sum
)
```

**Priority Calculation Algorithm:**
```kotlin
totalScore = (
    basePriority * 0.40 +      // 40% weight
    contextMatch * 0.30 +      // 30% weight
    phraseMatch * 0.20 +       // 20% weight
    usageFrequency * 0.10      // 10% weight
)
```

**Unit Tests Required:**
- ✅ testCalculateScore_PerfectMatch_Returns100Percent
- ✅ testCalculateScore_PartialMatch_ReturnsPartialScore
- ✅ testResolvePriorityConflict_HighestScoreWins
- ✅ testNormalizePriority_Range1to100

---

**3. ConflictDetector.kt** (~300 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/ConflictDetector.kt
```

**Purpose:** Detect and report command phrase conflicts

**Key Features:**
- Exact phrase duplicates
- Similar phrases (Levenshtein distance < 3)
- Namespace-aware detection
- Conflict resolution suggestions

**Public API:**
```kotlin
class ConflictDetector(private val registry: DynamicCommandRegistry) {
    fun detectConflicts(newCommand: VoiceCommand): List<ConflictInfo>
    fun findSimilarPhrases(phrase: String, threshold: Int = 3): List<VoiceCommand>
    fun suggestResolution(conflicts: List<ConflictInfo>): List<ResolutionSuggestion>
}

data class ConflictInfo(
    val newCommand: VoiceCommand,
    val existingCommand: VoiceCommand,
    val conflictingPhrase: String,
    val similarity: Float,  // 0.0-1.0
    val conflictType: ConflictType
)

enum class ConflictType {
    EXACT_DUPLICATE,      // Same phrase, same namespace
    SIMILAR_PHRASE,       // Levenshtein < threshold
    NAMESPACE_COLLISION,  // Same phrase, different namespace
    PRIORITY_AMBIGUITY    // Same phrase, same priority
}

data class ResolutionSuggestion(
    val type: SuggestionType,
    val description: String,
    val action: () -> Unit
)

enum class SuggestionType {
    INCREASE_PRIORITY,
    CHANGE_PHRASE,
    CHANGE_NAMESPACE,
    MERGE_COMMANDS
}
```

**Levenshtein Distance Implementation:**
```kotlin
private fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    // Standard dynamic programming implementation
}
```

**Unit Tests Required:**
- ✅ testDetectConflicts_ExactDuplicate_Detected
- ✅ testDetectConflicts_SimilarPhrase_Detected
- ✅ testDetectConflicts_NoConflict_ReturnsEmpty
- ✅ testFindSimilarPhrases_WithinThreshold_ReturnsMatches
- ✅ testSuggestResolution_DuplicatePhrase_SuggestsIncreasePriority

---

**4. NamespaceManager.kt** (~300 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/dynamic/NamespaceManager.kt
```

**Purpose:** Manage command namespaces for module isolation

**Key Features:**
- Namespace creation/deletion
- Permission-based access control
- Cross-namespace command visibility
- Namespace-scoped queries

**Public API:**
```kotlin
class NamespaceManager {
    fun createNamespace(name: String, permissions: NamespacePermissions): Result<Unit>
    fun deleteNamespace(name: String): Result<Unit>
    fun getCommands(namespace: String): List<VoiceCommand>
    fun canAccess(namespace: String, requester: String): Boolean
    fun setVisibility(namespace: String, visibility: NamespaceVisibility)
}

data class NamespacePermissions(
    val owner: String,
    val readers: Set<String> = emptySet(),
    val writers: Set<String> = emptySet()
)

enum class NamespaceVisibility {
    PUBLIC,     // All modules can see
    PRIVATE,    // Only owner
    RESTRICTED  // Only permitted modules
}

// Built-in namespaces
object Namespaces {
    const val SYSTEM = "system"
    const val NAVIGATION = "navigation"
    const val INPUT = "input"
    const val MEDIA = "media"
    const val ACCESSIBILITY = "accessibility"
    const val CUSTOM = "custom"
}
```

**Unit Tests Required:**
- ✅ testCreateNamespace_Success
- ✅ testCreateNamespace_Duplicate_Fails
- ✅ testDeleteNamespace_Success
- ✅ testCanAccess_Public_AllowsAll
- ✅ testCanAccess_Private_OnlyOwner
- ✅ testSetVisibility_ChangesAccess

---

### Task 1.2: Custom Command Editor UI (10 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** MEDIUM
**Dependencies:** DynamicCommandRegistry
**Estimated Lines:** ~2,000 lines (Compose)

#### Files to Create:

**1. CommandEditorScreen.kt** (~500 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandEditorScreen.kt
```

**Purpose:** Main screen for command management

**Jetpack Compose Structure:**
```kotlin
@Composable
fun CommandEditorScreen(
    viewModel: CommandEditorViewModel = hiltViewModel()
) {
    val commands by viewModel.commands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = { CommandEditorTopBar() },
        floatingActionButton = { AddCommandFab() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(query = searchQuery, onQueryChange = viewModel::updateSearch)
            FilterChips(selectedFilters = viewModel.filters)
            LazyColumn {
                items(commands) { command ->
                    CommandListItem(
                        command = command,
                        onEdit = viewModel::editCommand,
                        onDelete = viewModel::deleteCommand,
                        onTest = viewModel::testCommand
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandListItem(
    command: VoiceCommand,
    onEdit: (VoiceCommand) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (VoiceCommand) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.phrases.first(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Priority: ${command.priority} • Namespace: ${command.namespace}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { onTest(command) }) {
                    Icon(Icons.Default.PlayArrow, "Test")
                }
                IconButton(onClick = { onEdit(command) }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = { onDelete(command.id) }) {
                    Icon(Icons.Default.Delete, "Delete")
                }
            }
        }
    }
}
```

**Material 3 Design:**
- Color scheme: Material You dynamic colors
- Typography: Material 3 type scale
- Components: Card, TopAppBar, FAB, LazyColumn
- Dark mode: Automatic with system preference

---

**2. CommandCreationWizard.kt** (~600 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandCreationWizard.kt
```

**Purpose:** Step-by-step command creation wizard

**Wizard Steps:**
1. **Basic Info** - Name, primary phrase
2. **Synonyms** - Add alternative phrases
3. **Priority** - Set priority 1-100
4. **Context** - App/screen/time requirements
5. **Action** - Choose action type
6. **Review** - Preview and save

**Compose Structure:**
```kotlin
@Composable
fun CommandCreationWizard(
    onComplete: (VoiceCommand) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(WizardStep.BASIC_INFO) }
    var commandBuilder by remember { mutableStateOf(CommandBuilder()) }

    Column {
        WizardProgressIndicator(currentStep = currentStep)

        when (currentStep) {
            WizardStep.BASIC_INFO -> BasicInfoStep(
                builder = commandBuilder,
                onNext = { currentStep = WizardStep.SYNONYMS }
            )
            WizardStep.SYNONYMS -> SynonymsStep(
                builder = commandBuilder,
                onNext = { currentStep = WizardStep.PRIORITY },
                onBack = { currentStep = WizardStep.BASIC_INFO }
            )
            // ... other steps
        }
    }
}

enum class WizardStep {
    BASIC_INFO, SYNONYMS, PRIORITY, CONTEXT, ACTION, REVIEW
}
```

---

**3. CommandTestingPanel.kt** (~500 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandTestingPanel.kt
```

**Purpose:** Real-time command testing interface

**Features:**
- Record voice input
- Show matched commands with scores
- Execute command in test mode
- Display results and debug info

---

**4. CommandLibraryBrowser.kt** (~400 lines)
```
Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/ui/editor/CommandLibraryBrowser.kt
```

**Purpose:** Browse and add command templates

**15+ Command Templates:**

**Navigation Templates:**
1. "go forward" → navigate forward
2. "go back" → navigate backward
3. "scroll up" → scroll up
4. "scroll down" → scroll down
5. "go to top" → jump to first

**Text Editing Templates:**
6. "select all" → select all text
7. "copy" → copy selected
8. "paste" → paste clipboard
9. "delete" → delete selected

**System Templates:**
10. "volume up" → increase volume
11. "volume down" → decrease volume
12. "brightness up" → increase brightness
13. "take screenshot" → capture screen

**App-Specific Templates:**
14. "open [app]" → launch app
15. "close app" → close current

**JSON Import/Export:**
```kotlin
fun exportToJson(commands: List<VoiceCommand>): String {
    return JSONObject().apply {
        put("version", "1.0")
        put("commands", JSONArray(commands.map { it.toJsonArray() }))
    }.toString(2)
}
```

---

### Task 1.3: Command Macros (8 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** MEDIUM
**Dependencies:** DynamicCommandRegistry
**Estimated Lines:** ~1,500 lines

#### Files to Create:

**1. CommandMacro.kt** (~200 lines)
**2. MacroStep.kt** (~300 lines)
**3. MacroContext.kt** (~200 lines)
**4. MacroExecutor.kt** (~500 lines)
**5. MacroDSL.kt** (~300 lines)

**See detailed specifications in VOS4-CommandManager-Implementation-TODO-251009-1902.md**

---

### Task 1.4: Context-Aware Commands (12 hours)

**Status:** 🟡 PARTIALLY STARTED (ContextManager.kt exists)
**Priority:** HIGH
**Dependencies:** None (enhance existing)
**Estimated Lines:** ~1,800 lines

#### Files to Create:

**1. ContextDetector.kt** (~500 lines)
**2. ContextMatcher.kt** (~400 lines)
**3. PreferenceLearner.kt** (~500 lines)
**4. ContextSuggester.kt** (~400 lines)

**See detailed specifications in VOS4-CommandManager-Implementation-TODO-251009-1902.md**

---

## 🎯 PHASE 2: JSON Architecture (12 hours)

### Task 2.1: Array-Based JSON Creation (4 hours) ✅ COMPLETE

**Status:** ✅ COMPLETED 2025-10-09
**Priority:** HIGH (enables all other work)
**Dependencies:** NONE
**Estimated Lines:** N/A (JSON files)

**COMPLETED:** Created 4 locale files (en-US, es-ES, fr-FR, de-DE) + UI strings

#### Files to Create:

**1. en-US.json** (English commands)
```
Path: modules/managers/CommandManager/src/main/assets/localization/commands/en-US.json
```

**Structure:**
```json
{
  "version": "1.0",
  "locale": "en-US",
  "fallback": "en-US",
  "updated": "2025-10-09",
  "author": "VOS4 Team",
  "commands": [
    ["navigate_forward", "forward", ["next", "advance", "go forward", "onward"], "Move to next element"],
    ["navigate_backward", "backward", ["previous", "back", "go back", "prior", "rewind"], "Move to previous element"],
    ["navigate_left", "left", ["go left", "move left", "westward"], "Move cursor left"],
    ["navigate_right", "right", ["go right", "move right", "eastward"], "Move cursor right"],
    ["navigate_up", "up", ["go up", "move up", "upward", "above"], "Move cursor up"],
    ["navigate_down", "down", ["go down", "move down", "downward", "below"], "Move cursor down"],
    ["navigate_first", "first", ["beginning", "start", "top", "initial"], "Jump to first element"],
    ["navigate_last", "last", ["end", "final", "bottom"], "Jump to last element"],

    ["action_click", "click", ["tap", "select", "press", "activate"], "Activate element"],
    ["action_open", "open", ["launch", "start", "run"], "Open application"],
    ["action_close", "close", ["exit", "quit", "dismiss", "cancel"], "Close application"],
    ["action_focus", "focus", ["highlight", "select", "go to"], "Focus on element"],

    ["cursor_enable", "show cursor", ["enable cursor", "cursor on", "activate cursor"], "Enable voice cursor"],
    ["cursor_disable", "hide cursor", ["disable cursor", "cursor off", "deactivate cursor"], "Disable voice cursor"],

    ["shape_circle", "draw circle", ["make circle", "create circle"], "Draw circular shape"],
    ["shape_square", "draw square", ["make square", "create square"], "Draw square shape"],
    ["shape_rectangle", "draw rectangle", ["make rectangle"], "Draw rectangular shape"],

    ["size_increase", "bigger", ["increase size", "larger", "expand", "grow"], "Increase size"],
    ["size_decrease", "smaller", ["decrease size", "reduce", "shrink"], "Decrease size"],
    ["size_lock", "done", ["lock size", "finish", "complete"], "Lock size and finish"],

    ["help", "help", ["what can I say", "show commands", "voice help"], "Show available commands"],
    ["settings", "settings", ["options", "preferences", "configure"], "Open settings"]
  ]
}
```

**Requirements:**
- Minimum 20 commands
- Array format: `["id", "primary", ["syn1", "syn2"], "description"]`
- 1 line per command
- Alphabetical by category

**Validation:**
```bash
# Check JSON validity
cat en-US.json | jq . > /dev/null && echo "Valid JSON" || echo "Invalid JSON"

# Count commands
cat en-US.json | jq '.commands | length'

# File size
ls -lh en-US.json
```

---

**2. es-ES.json** (Spanish)
**3. fr-FR.json** (French)
**4. de-DE.json** (German)
**5. ui/en-US.json** (UI strings)

**Translation Requirements:**
- Professional translations (not machine-translated)
- Cultural appropriateness
- Same action_id across all locales

**File Size Comparison:**
- Old format: ~450 bytes per command
- New format: ~120 bytes per command
- **Savings: 73% (330 bytes per command)**

---

### Task 2.2: English Fallback Database (3 hours) ✅ COMPLETE

**Status:** ✅ COMPLETED 2025-10-09
**Priority:** HIGH
**Dependencies:** Task 2.1 (JSON files)
**Estimated Lines:** ~1,031 lines (actual)

**COMPLETED:** Created 6 files - CommandDatabase, Entity, DAO, Parser, Loader, Resolver

#### Files to Create:

**1. CommandDatabase.kt** (~150 lines)
**2. VoiceCommandEntity.kt** (~100 lines)
**3. VoiceCommandDao.kt** (~200 lines)
**4. CommandLoader.kt** (~250 lines)
**5. ArrayJsonParser.kt** (~150 lines)
**6. CommandResolver.kt** (~150 lines)

**See VOS4-CommandManager-Implementation-TODO-251009-1902.md for full specifications**

---

### Task 2.3: Number Overlay Aesthetics (5 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** MEDIUM
**Dependencies:** None
**Estimated Lines:** ~600 lines

#### Files to Create:

**1. NumberOverlayRenderer.kt** (~400 lines)
**2. NumberOverlayStyle.kt** (~200 lines)

**Update:** NumberedSelectionOverlay.kt

**Design Specifications from User:**
- Circular badge (32dp diameter)
- Top-right OR top-left (user configurable)
- 4px offset from element edge
- Material 3 colors: Green/Orange/Grey
- White 14sp bold number
- 4px drop shadow

**See Command-JSON-Architecture-251009-1208.md section 3 for complete design**

---

### Task 2.4: Critical Fixes (6 hours) 🚨 CANNOT BE IGNORED

**Status:** ⏸️ NOT STARTED
**Priority:** CRITICAL
**Dependencies:** Tasks 2.1, 2.2
**Estimated Lines:** ~870 lines

**⚠️ USER REQUIREMENT: These issues from implementation CANNOT be ignored**

**See full details in:** `/coding/TODO/CommandManager-Critical-Fixes-TODO-251009-1957.md`

#### Subtask 2.4a: Database Persistence Check (2 hours)

**Problem:** Database recreated on every app restart (~500ms wasted)

**Solution:**
- Add DatabaseVersionEntity to track loaded JSON version
- Check version before loading commands
- Only reload if version mismatch or database empty
- Add force reload method for manual updates

**Files to Create:**
1. `DatabaseVersionEntity.kt` (~50 lines)
2. `DatabaseVersionDao.kt` (~40 lines)

**Files to Update:**
3. `CommandDatabase.kt` (add version entity, increment to v2)
4. `CommandLoader.kt` (add persistence check logic)

**Success Criteria:**
- ✅ App startup time reduced by ~500ms after first launch
- ✅ Database only loads once unless version changes
- ✅ Force reload available for developers

#### Subtask 2.4b: Command Usage Statistics (2 hours)

**Problem:** No tracking of which commands are used (cannot learn preferences)

**Solution:**
- Track every command execution with timestamp, success/fail
- Store in CommandUsageEntity table
- Provide analytics: most used, success rates
- Auto-delete old records for privacy (30 days)

**Files to Create:**
1. `CommandUsageEntity.kt` (~80 lines)
2. `CommandUsageDao.kt` (~150 lines)
3. `UsageAnalyticsScreen.kt` (~200 lines)

**Files to Update:**
4. `CommandDatabase.kt` (add usage entity, increment to v3)
5. `CommandResolver.kt` (add usage tracking on resolve)

**Success Criteria:**
- ✅ Every command execution tracked (<5ms overhead)
- ✅ Analytics UI shows most used commands
- ✅ Success rates calculated correctly
- ✅ Privacy controls functional

#### Subtask 2.4c: Dynamic Command Updates (2 hours)

**Problem:** JSON changes require app restart

**Solution:**
- Add settings UI with "Reload Commands" button
- Add database stats display
- Add developer mode file watcher (optional)
- Expose forceReload() method

**Files to Create:**
1. `CommandManagerSettingsFragment.kt` (~200 lines)
2. `CommandFileWatcher.kt` (~150 lines) - optional developer mode

**Files to Update:**
3. `CommandLoader.kt` (ensure forceReload() method exists)

**Success Criteria:**
- ✅ Reload button works without app restart
- ✅ Database stats display correctly
- ✅ Developer mode file watching works
- ✅ No crashes during reload

---

## 🎯 PHASE 3: Scraping Integration (16 hours) ⭐⭐ CRITICAL

### Task 3.1: App Scraping Database (6 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** CRITICAL
**Dependencies:** None
**Estimated Lines:** ~1,200 lines

#### Files to Create:

**1. AppScrapingDatabase.kt** (~200 lines)
**2. ScrapedAppEntity.kt** (~100 lines)
**3. ScrapedElementEntity.kt** (~150 lines)
**4. ScrapedHierarchyEntity.kt** (~100 lines)
**5. GeneratedCommandEntity.kt** (~150 lines)
**6. ScrapedAppDao.kt** (~150 lines)
**7. ScrapedElementDao.kt** (~150 lines)
**8. ScrapedHierarchyDao.kt** (~100 lines)
**9. GeneratedCommandDao.kt** (~150 lines)

**Database Location:** `modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/scraping/`

**See Command-JSON-Architecture-251009-1208.md section 5 for complete schema**

---

### Task 3.2: Scraping Integration (6 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** CRITICAL
**Dependencies:** Task 3.1
**Estimated Lines:** ~800 lines

#### Files to Create:

**1. AccessibilityTreeScraper.kt** (~400 lines)
**2. ElementHasher.kt** (~200 lines)
**3. ScrapingCoordinator.kt** (~200 lines)

**Update:** VoiceAccessibilityService.kt

---

### Task 3.3: Voice Recognition Integration (4 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** CRITICAL
**Dependencies:** Tasks 3.1, 3.2
**Estimated Lines:** ~600 lines

#### Files to Create:

**1. VoiceCommandProcessor.kt** (~400 lines)
**2. NodeFinder.kt** (~200 lines)

---

## 🎯 PHASE 4: Testing (8 hours)

### Task 4.1: Unit Tests (4 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** MEDIUM
**Dependencies:** All implementation tasks
**Estimated Lines:** ~3,000+ lines (85+ tests)

#### Test Files to Create:

**1. DynamicCommandRegistryTest.kt** (20+ tests)
**2. MacroExecutorTest.kt** (15+ tests)
**3. ContextDetectorTest.kt** (25+ tests)
**4. CommandLoaderTest.kt** (10+ tests)
**5. AccessibilityTreeScraperTest.kt** (15+ tests)

---

### Task 4.2: Integration Tests (4 hours)

**Status:** ⏸️ NOT STARTED
**Priority:** MEDIUM
**Dependencies:** All implementation + unit tests
**Estimated Lines:** ~1,000 lines

#### Test Files to Create:

**1. CommandManagerIntegrationTest.kt**
**2. ScrapingIntegrationTest.kt**

---

## 📈 Progress Tracking

### Use TodoWrite Tool Throughout:

**Before Starting Any Task:**
```kotlin
TodoWrite(todos = [
    Todo(content = "Create en-US.json", status = "in_progress", activeForm = "Creating en-US.json"),
    Todo(content = "Create es-ES.json", status = "pending", activeForm = "Creating es-ES.json"),
    // ... all tasks
])
```

**After Completing Task:**
```kotlin
TodoWrite(todos = [
    Todo(content = "Create en-US.json", status = "completed", activeForm = "Created en-US.json"),
    Todo(content = "Create es-ES.json", status = "in_progress", activeForm = "Creating es-ES.json"),
    // ... update list
])
```

---

## ✅ Success Criteria Checklist

### Phase 1 Complete When:
- [ ] Commands can be registered/unregistered at runtime
- [ ] Priority conflicts automatically resolved
- [ ] Custom command editor functional with Material 3 UI
- [ ] Command macros execute multi-step sequences
- [ ] Context detection working (app/screen/time/location)
- [ ] User preferences learned and applied
- [ ] 45+ unit tests passing
- [ ] Build successful (0 errors)

### Phase 2 Complete When:
- [ ] All JSON files in array format (4 locales + UI strings)
- [ ] File size 73% smaller than old format
- [ ] English fallback always loaded
- [ ] Command resolution working (user locale → English → null)
- [ ] Number overlays use circular badge design
- [ ] Material 3 colors (Green/Orange/Grey)
- [ ] Top-right/left positioning works
- [ ] 10+ unit tests passing

### Phase 3 Complete When:
- [ ] Apps automatically scraped on first window change
- [ ] Elements stored with accessibility properties
- [ ] Hierarchical relationships preserved
- [ ] Element hashing identifies UI nodes correctly
- [ ] Commands generated from scraped data
- [ ] Voice recognition queries scraping database
- [ ] Actions executed on correct elements
- [ ] Usage statistics tracked
- [ ] 15+ unit tests passing

### Phase 4 Complete When:
- [ ] 85+ unit tests created
- [ ] All unit tests passing
- [ ] Integration tests cover end-to-end workflows
- [ ] Test coverage >80%
- [ ] No memory leaks detected
- [ ] Performance benchmarks acceptable

---

## 🚀 Recommended Implementation Order

### Week 1 (Quick Wins - 16 hours):
1. ✅ **Array-Based JSON** (4h) - START HERE
2. ✅ English Fallback Database (3h)
3. ✅ Number Overlay Aesthetics (5h)
4. ✅ Unit tests for above (4h)

**Why:** Low risk, visible progress, enables other work

### Week 2 (Critical Path - 22 hours):
1. ✅ App Scraping Database (6h)
2. ✅ Scraping Integration (6h)
3. ✅ Voice Recognition Integration (4h)
4. ✅ Dynamic Command Registration (6h)

**Why:** Addresses critical user requirement, high value

### Week 3 (Advanced Features - 20 hours):
1. ✅ Context-Aware Commands (12h)
2. ✅ Command Macros (8h)

**Why:** Build on foundation, add power features

### Week 4 (UI & Testing - 16 hours):
1. ✅ Custom Command Editor (10h)
2. ✅ Integration Tests (4h)
3. ✅ Final polish (2h)

**Why:** User-facing features last, comprehensive testing

---

**Last Updated:** 2025-10-09 19:34:28 PDT
**Ready to Start:** YES ✅
**Recommended First Task:** Task 2.1 - Array-Based JSON Creation (4 hours)
**Next Steps:** Create TodoWrite list, start implementation

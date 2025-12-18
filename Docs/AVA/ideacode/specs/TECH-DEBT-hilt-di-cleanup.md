# Tech Debt Ticket: Hilt DI Migration Cleanup

**Issue ID:** TECH-001
**Created:** 2025-11-13
**Priority:** Medium
**Type:** Refactoring / Code Quality
**Related Spec:** SPEC-hilt-di-implementation.md (Phase 7)
**Framework:** IDEACODE v8.0

---

## Overview

Following the successful Phase 3 completion of Hilt DI migration (ChatViewModel), several tech debt items have been identified that require cleanup. These items don't block functionality but improve code quality, maintainability, and follow Android best practices.

**Source:** Code evaluation report from `/ideacode.evaluatecode` command (2025-11-13)

---

## Tech Debt Items

### 1. Remove Unnecessary Nullable Repository Checks

**Priority:** LOW
**Effort:** 1-2 hours
**Impact:** Code clarity, reduced LOC (~20-30 lines)

#### Problem

ChatViewModel checks repositories for null despite being non-nullable:

**File:** `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt`

**Locations:**
- Line 341: `conversationRepository?.let { repo -> ... }`
- Line 481: `messageRepository?.let { repo -> ... }`
- Line 697: `trainExampleRepository?.let { repo -> ... }`
- Line 1030: `conversationRepository?.let { repo -> ... }`
- Line 1100: `conversationRepository?.let { repo -> ... }`
- Line 1176: `messageRepository?.let { repo -> ... }`
- Line 1335: `trainExampleRepository?.let { repo -> ... }`
- Line 1413: `conversationRepository?.let { repo -> ... }`

#### Why It Exists

Legacy defensive programming from pre-Hilt era when repositories were nullable:

```kotlin
// Old constructor (before Hilt)
class ChatViewModel(
    private val conversationRepository: ConversationRepository? = null,  // Nullable
    ...
)

// Code used nullable checks
conversationRepository?.let { ... } ?: run {
    _errorMessage.value = "Repository not available"
}
```

#### Current State

After Hilt migration, repositories are guaranteed non-null:

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,  // NON-nullable
    ...
)
```

#### Solution

Replace nullable checks with direct calls:

**Before:**
```kotlin
conversationRepository?.let { repo ->
    when (val result = repo.getConversationById(id)) {
        is Result.Success -> { ... }
        is Result.Error -> { ... }
    }
} ?: run {
    _errorMessage.value = "Repository not available"
    Log.e(TAG, "ConversationRepository is null")
}
```

**After:**
```kotlin
when (val result = conversationRepository.getConversationById(id)) {
    is Result.Success -> { ... }
    is Result.Error -> { ... }
}
```

#### Files to Update

1. `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt`

#### Acceptance Criteria

- [ ] All `repository?.let { }` patterns removed
- [ ] No `?: run { _errorMessage.value = "Repository not available" }` blocks
- [ ] All tests still pass (19/19)
- [ ] No functional changes (behavior identical)

---

### 2. Refactor Context Injection from ChatViewModel

**Priority:** HIGH
**Effort:** 2-3 hours
**Impact:** Follows Android best practices, removes anti-pattern

#### Problem

ChatViewModel injects `ApplicationContext` directly, which is generally discouraged:

**File:** `ChatViewModel.kt:65`

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,  // ⚠️  Anti-pattern
    ...
) : ViewModel()
```

#### Why It's Used

Context is needed for:
1. `ActionsInitializer.initialize(context)` (line 444)
2. `IntentActionHandlerRegistry.executeAction(context, ...)` (line 889)

#### Why It's a Problem

- **Memory leak risk** (mitigated by `@ApplicationContext` but still not ideal)
- **Violates separation of concerns** (ViewModel shouldn't know about Android framework)
- **Hard to test** (requires Android context in tests)
- **Against Android Architecture best practices**

#### Solution Options

##### Option 1: Inject ActionsInitializer Wrapper (Recommended)

Create a wrapper that encapsulates context dependency:

**Step 1:** Create wrapper class

```kotlin
// File: com/augmentalis/ava/features/actions/ActionsManager.kt
@Singleton
class ActionsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var initialized = false

    fun initialize() {
        if (!initialized) {
            ActionsInitializer.initialize(context)
            initialized = true
        }
    }

    fun executeAction(intent: String, utterance: String): ActionResult {
        return IntentActionHandlerRegistry.executeAction(context, intent, utterance)
    }
}
```

**Step 2:** Provide via Hilt module

```kotlin
// File: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/AppModule.kt
@Provides
@Singleton
fun provideActionsManager(
    @ApplicationContext context: Context
): ActionsManager {
    return ActionsManager(context)
}
```

**Step 3:** Update ChatViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // @ApplicationContext private val context: Context,  // ❌ REMOVED
    private val actionsManager: ActionsManager,  // ✅ ADDED
    ...
) : ViewModel() {

    init {
        actionsManager.initialize()  // Instead of ActionsInitializer.initialize(context)
    }

    fun sendMessage(text: String) {
        // ...
        val actionResult = actionsManager.executeAction(intent, utterance)
        // Instead of: IntentActionHandlerRegistry.executeAction(context, intent, utterance)
    }
}
```

##### Option 2: Keep for Now, Document as Tech Debt

If Option 1 is too invasive, keep Context injection but:
1. Add comment explaining why it's needed
2. Add TODO comment linking to this ticket
3. Plan removal in future refactor

**Implementation:**
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // TODO(TECH-001): Remove context injection, use ActionsManager wrapper instead
    // Context is currently needed for ActionsInitializer and IntentActionHandler
    // See: .ideacode/specs/TECH-DEBT-hilt-di-cleanup.md
    @ApplicationContext private val context: Context,
    ...
) : ViewModel()
```

#### Recommended Approach

**Option 1** (ActionsManager wrapper) for these reasons:
- Follows Android best practices
- Easier to test (mock ActionsManager)
- Better separation of concerns
- Future-proof (can swap implementations)

#### Files to Create/Update

1. **NEW:** `Universal/AVA/Features/Actions/src/main/kotlin/.../ActionsManager.kt`
2. **UPDATE:** `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/AppModule.kt`
3. **UPDATE:** `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt`
4. **NEW:** `Universal/AVA/Features/Actions/src/test/kotlin/.../ActionsManagerTest.kt` (tests)

#### Acceptance Criteria

- [ ] ActionsManager class created with Hilt injection
- [ ] AppModule provides ActionsManager
- [ ] ChatViewModel constructor removes Context parameter
- [ ] ChatViewModel uses actionsManager instead of direct calls
- [ ] All existing functionality works (19/19 tests pass)
- [ ] New tests added for ActionsManager

---

### 3. Make Cache Configuration Values User-Configurable

**Priority:** LOW
**Effort:** 2-3 hours
**Impact:** User customization, performance tuning

#### Problem

Cache sizes and TTLs are hard-coded magic numbers:

**File:** `ChatViewModel.kt`

**Locations:**
- Line 102: `MESSAGE_PAGE_SIZE = 50`
- Line 125: `CONVERSATIONS_CACHE_TTL_MS = 5000L` (5 seconds)
- Line 133: `INTENTS_CACHE_TTL_MS = 10000L` (10 seconds)
- Line 148: LRU cache max size = 100

#### Why It's a Problem

- Values are arbitrary (work for most cases, but not optimal for all)
- High-end devices could use larger caches for better performance
- Low-end devices might benefit from smaller caches
- Users can't tune for their usage patterns
- Developers can't easily experiment with different values

#### Solution

Move to ChatPreferences (DataStore):

**Step 1:** Add preferences to ChatPreferences

```kotlin
// File: com/augmentalis/ava/core/data/prefs/ChatPreferences.kt
class ChatPreferences(context: Context) {

    // Existing preferences
    val confidenceThreshold: StateFlow<Float> = ...
    val conversationMode: StateFlow<ConversationMode> = ...

    // NEW: Cache configuration
    val messagePageSize: StateFlow<Int> = ...  // Default: 50
    val conversationsCacheTTL: StateFlow<Long> = ...  // Default: 5000ms
    val intentsCacheTTL: StateFlow<Long> = ...  // Default: 10000ms
    val nluCacheMaxSize: StateFlow<Int> = ...  // Default: 100

    suspend fun setMessagePageSize(size: Int) { ... }
    suspend fun setConversationsCacheTTL(ttl: Long) { ... }
    suspend fun setIntentsCacheTTL(ttl: Long) { ... }
    suspend fun setNLUCacheMaxSize(size: Int) { ... }
}
```

**Step 2:** Update ChatViewModel to use preferences

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatPreferences: ChatPreferences,
    ...
) : ViewModel() {

    // Replace constants with reactive StateFlow
    private val messagePageSize: StateFlow<Int> = chatPreferences.messagePageSize
    private val conversationsCacheTTL: StateFlow<Long> = chatPreferences.conversationsCacheTTL
    private val intentsCacheTTL: StateFlow<Long> = chatPreferences.intentsCacheTTL
    private val nluCacheMaxSize: StateFlow<Int> = chatPreferences.nluCacheMaxSize

    // Use values from preferences
    private val classificationCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, IntentClassification>(
            nluCacheMaxSize.value,  // Dynamic size
            0.75f,
            true
        ) {
            override fun removeEldestEntry(eldest: Map.Entry<String, IntentClassification>): Boolean {
                return size > nluCacheMaxSize.value  // Dynamic threshold
            }
        }
    )
}
```

**Step 3:** Add UI controls in SettingsScreen

```kotlin
@Composable
fun PerformanceSettings(viewModel: SettingsViewModel) {
    Section(title = "Performance Tuning") {
        SliderSetting(
            label = "Message Page Size",
            value = messagePageSize,
            range = 25..100,
            onValueChange = { viewModel.setMessagePageSize(it) }
        )

        SliderSetting(
            label = "Cache TTL (seconds)",
            value = conversationsCacheTTL / 1000,
            range = 1..30,
            onValueChange = { viewModel.setConversationsCacheTTL(it * 1000) }
        )

        // ... other settings
    }
}
```

#### Files to Update

1. `Universal/AVA/Core/Data/src/main/java/.../ChatPreferences.kt`
2. `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt`
3. `apps/ava-standalone/src/main/kotlin/.../SettingsViewModel.kt`
4. `apps/ava-standalone/src/main/kotlin/.../SettingsScreen.kt`

#### Acceptance Criteria

- [ ] Cache values moved to ChatPreferences
- [ ] ChatViewModel reads from reactive StateFlows
- [ ] SettingsScreen has performance tuning section
- [ ] Users can adjust cache sizes via UI
- [ ] Changes take effect immediately (reactive)
- [ ] Tests updated for configurable values

---

### 4. Extract Use Cases from ChatViewModel

**Priority:** LOW (Nice to have)
**Effort:** 8-10 hours
**Impact:** Better separation of concerns, easier testing

#### Problem

ChatViewModel is large (1453 lines) with many responsibilities:

**Responsibilities:**
1. Message sending and NLU classification
2. Teach-AVA bottom sheet management
3. Conversation history overlay
4. Message pagination
5. Cache management
6. Action execution
7. State management

**Metrics:**
- Lines of code: 1453
- Methods: 25+
- Complexity: Moderate-High

#### Why It's a Problem

- **Violates Single Responsibility Principle** (debatable - it's a ViewModel)
- **Hard to maintain** (large file, many concerns)
- **Harder to test** (many paths through single class)
- **Difficult to reuse** logic outside ChatViewModel

#### Solution

Extract business logic into use cases:

```kotlin
// File: com/augmentalis/ava/features/chat/domain/SendMessageUseCase.kt
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val intentClassifier: IntentClassifier,
    private val actionsManager: ActionsManager
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String
    ): Result<MessageSendResult> {
        // All message sending logic here
        // Extracted from ChatViewModel.sendMessage()
    }
}

// File: com/augmentalis/ava/features/chat/domain/TeachAvaUseCase.kt
class TeachAvaUseCase @Inject constructor(
    private val trainExampleRepository: TrainExampleRepository
) {
    suspend operator fun invoke(
        utterance: String,
        intent: String
    ): Result<Unit> {
        // All teach logic here
        // Extracted from ChatViewModel.handleTeachAva()
    }
}

// File: com/augmentalis/ava/features/chat/domain/LoadMessagesUseCase.kt
class LoadMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        offset: Int,
        limit: Int
    ): Result<List<Message>> {
        // Message loading with pagination
        // Extracted from ChatViewModel.observeMessages() and loadMoreMessages()
    }
}
```

**Updated ChatViewModel (Simplified):**

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val teachAvaUseCase: TeachAvaUseCase,
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val chatPreferences: ChatPreferences
) : ViewModel() {

    // Just state management and UI coordination
    fun sendMessage(text: String) {
        viewModelScope.launch {
            when (val result = sendMessageUseCase(activeConversationId.value, text)) {
                is Result.Success -> _messages.value += result.data
                is Result.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun handleTeachAva(messageId: String, intent: String) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            when (val result = teachAvaUseCase(message.content, intent)) {
                is Result.Success -> dismissTeachBottomSheet()
                is Result.Error -> _errorMessage.value = result.message
            }
        }
    }
}
```

#### Files to Create

1. `Universal/AVA/Features/Chat/src/main/kotlin/.../domain/SendMessageUseCase.kt`
2. `Universal/AVA/Features/Chat/src/main/kotlin/.../domain/TeachAvaUseCase.kt`
3. `Universal/AVA/Features/Chat/src/main/kotlin/.../domain/LoadMessagesUseCase.kt`
4. `Universal/AVA/Features/Chat/src/main/kotlin/.../domain/ManageConversationsUseCase.kt`
5. Tests for each use case

#### Files to Update

1. `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt` (simplified)
2. Hilt module to provide use cases

#### Acceptance Criteria

- [ ] Use cases created for major operations
- [ ] ChatViewModel delegates to use cases
- [ ] All tests still pass (no regressions)
- [ ] ChatViewModel reduced to <600 lines
- [ ] Each use case has comprehensive tests
- [ ] Business logic moved out of ViewModel

---

## Priority Ranking

| Priority | Item | Effort | Impact | Recommended Phase |
|----------|------|--------|--------|-------------------|
| 🔴 HIGH | #2: Refactor Context injection | 2-3h | High | Phase 7 |
| 🟡 MEDIUM | #1: Remove nullable checks | 1-2h | Medium | Phase 7 |
| 🟢 LOW | #3: Configurable cache values | 2-3h | Low | Post-Phase 7 |
| 🔵 NICE TO HAVE | #4: Extract use cases | 8-10h | Medium | Separate epic |

---

## Implementation Plan

### Phase 7 (Current Hilt DI Migration)

**Include in Phase 7:**
- ✅ Item #2: Refactor Context injection (HIGH priority)
- ✅ Item #1: Remove nullable checks (cleanup task)

**Total Effort:** ~4 hours (aligns with Phase 7 estimate of 2 hours → updated to 4 hours)

### Post-Phase 7 (Future Improvements)

**Separate tickets:**
- Item #3: Configurable cache values (new feature, not cleanup)
- Item #4: Extract use cases (major refactor, separate epic)

---

## Acceptance Criteria (Overall)

### Must Complete (Phase 7):
- [ ] Item #1: All nullable repository checks removed
- [ ] Item #2: ActionsManager wrapper created and integrated
- [ ] All existing tests pass (19/19)
- [ ] No functional regressions
- [ ] Code coverage maintained or improved

### Nice to Have (Future):
- [ ] Item #3: Cache values configurable via settings
- [ ] Item #4: Use cases extracted from ChatViewModel

---

## Testing Strategy

### Regression Testing
- Run full test suite after each item (19/19 must pass)
- Manual testing of ChatScreen functionality
- Performance benchmarks (ensure no degradation)

### New Tests Required

**For Item #2 (ActionsManager):**
```kotlin
@Test
fun `ActionsManager should initialize once`()

@Test
fun `ActionsManager should execute actions correctly`()

@Test
fun `ActionsManager should handle execution failures`()
```

**For Item #3 (Configurable caches):**
```kotlin
@Test
fun `cache size should update when preference changes`()

@Test
fun `TTL should update when preference changes`()
```

**For Item #4 (Use cases):**
```kotlin
@Test
fun `SendMessageUseCase should classify and send`()

@Test
fun `TeachAvaUseCase should save training example`()

@Test
fun `LoadMessagesUseCase should paginate correctly`()
```

---

## Related Documentation

**Primary References:**
- `.ideacode/specs/SPEC-hilt-di-implementation.md` - Phase 7 description
- `docs/Developer-Manual-Chapter32-Hilt-DI.md` - Hilt usage guide
- Code evaluation report (2025-11-13) - Original findings

**Code Locations:**
- `Universal/AVA/Features/Chat/src/main/kotlin/.../ChatViewModel.kt`
- `Universal/AVA/Features/Actions/` (to be created)
- `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/`

---

## Rollback Plan

If any item causes issues:

1. **Git revert** to before the change
2. Run test suite to verify rollback
3. Document why the change failed
4. Update this ticket with lessons learned
5. Plan alternative approach if needed

---

## Notes

### Why Not Block Hilt Migration?

These items are tech debt, not blockers because:
- ✅ All tests pass (19/19)
- ✅ Functionality is correct
- ✅ No crashes or memory leaks detected
- ✅ Performance is acceptable

They're about **code quality and maintainability**, not **functionality**.

### When to Address

- **Phase 7** (Part of Hilt migration cleanup)
- **Post-release** (Items #3 and #4 can wait)

### Success Metrics

- Code reduced by ~50-100 lines (nullable checks removed)
- Zero Context injections in ViewModels (best practice)
- Maintainability improved (use cases extracted)
- User satisfaction increased (configurable caching)

---

**Ticket Status:** Open
**Assigned To:** TBD
**Target Completion:** Phase 7 (Items #1, #2) + Post-Phase 7 (Items #3, #4)
**Last Updated:** 2025-11-13

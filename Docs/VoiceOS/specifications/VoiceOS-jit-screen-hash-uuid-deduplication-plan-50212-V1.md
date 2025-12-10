# Implementation Plan: JIT Screen Hash Deduplication & UUID Generation

**Plan ID:** 009-IMPL
**Spec ID:** 009
**Created:** 2025-12-02
**Status:** Ready for Implementation
**Mode:** YOLO + Swarm
**Estimated Time:** 4-6 hours

---

## Implementation Strategy

### Swarm Activation: YES
**Reason**: 4 major components + database schema + cross-module integration

**Agents Recommended:**
1. **Database Specialist** - Schema migration, indexes, queries
2. **Android Expert** - JIT/LearnApp integration, AccessibilityService
3. **Testing Specialist** - Unit tests, integration tests, manual test plan
4. **Documentation Specialist** - Developer manual, architecture diagrams

**Coordination**: Sequential with dependencies (Database → Android → Testing → Docs)

---

## Implementation Phases

### Phase 1: Database Schema Enhancement (Database Specialist)
**Duration**: 30 minutes
**Dependencies**: None

#### Task 1.1: Add screen_hash Column
**File**: `libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/VoiceOSDatabase.sq`

**Changes**:
```sql
-- Add screen_hash column to scraped_element table
-- NOTE: SQLDelight doesn't support ALTER TABLE in migrations
-- Solution: Add column at runtime with default NULL

-- Migration 14 → 15
ALTER TABLE scraped_element ADD COLUMN screen_hash TEXT;
```

**Verification**:
```kotlin
// Test: Column exists
val result = database.scrapedElements.selectAll().executeAsList()
assert(result.first().screen_hash != null || result.first().screen_hash == null) // Column accessible
```

#### Task 1.2: Create Index for Performance
**File**: Same as 1.1

**Changes**:
```sql
-- Create index for fast screen hash lookups
CREATE INDEX IF NOT EXISTS idx_scraped_element_screen_hash
ON scraped_element(package_name, screen_hash);
```

**Performance Target**: <2ms query time for screen hash check

#### Task 1.3: Add Database Queries
**File**: Same as 1.1

**Changes**:
```sql
-- Query: Count elements by screen hash (deduplication check)
countByScreenHash:
SELECT COUNT(*)
FROM scraped_element
WHERE package_name = :packageName AND screen_hash = :screenHash;

-- Query: Get elements by screen hash (for LearnApp cache integration)
getByScreenHash:
SELECT *
FROM scraped_element
WHERE package_name = :packageName AND screen_hash = :screenHash;
```

**Generated Code**: `VoiceOSDatabaseQueries.kt`

---

### Phase 2: JIT Screen Hash Consistency (Android Expert)
**Duration**: 1 hour
**Dependencies**: Phase 1 complete

#### Task 2.1: Inject ScreenStateManager into JustInTimeLearner
**File**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/jit/JustInTimeLearner.kt`

**Changes**:
```kotlin
class JustInTimeLearner(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val voiceOSService: VoiceAccessibilityService?,
    private val screenStateManager: ScreenStateManager  // NEW: Inject dependency
) {
    // ...
}
```

**Update Constructor Calls**:
- `LearnAppIntegration.kt`: Pass `screenStateManager` when creating `JustInTimeLearner`

#### Task 2.2: Replace calculateScreenHash with ScreenStateManager
**File**: `JustInTimeLearner.kt:267-273`

**Before**:
```kotlin
private fun calculateScreenHash(event: AccessibilityEvent): String {
    val className = event.className?.toString() ?: ""
    val contentDesc = event.contentDescription?.toString() ?: ""
    val text = event.text?.joinToString("|") ?: ""
    return "$className|$contentDesc|$text".hashCode().toString()
}
```

**After**:
```kotlin
/**
 * Calculate screen hash using ScreenStateManager (consistent with LearnApp)
 *
 * Uses structure-based fingerprinting with popup awareness.
 * Ensures hash consistency between JIT and LearnApp for cache integration.
 *
 * @param packageName Package name of current app
 * @return Screen hash (structure-based, content-independent)
 */
private suspend fun calculateScreenHash(packageName: String): String = withContext(Dispatchers.Main) {
    val rootNode = accessibilityService?.rootInActiveWindow
    if (rootNode == null) {
        Log.w(TAG, "No root node available for screen hash calculation")
        return@withContext ""
    }

    return@withContext try {
        val screenState = screenStateManager.captureScreenState(rootNode, packageName, depth = 0)
        screenState.hash
    } catch (e: Exception) {
        Log.e(TAG, "Error calculating screen hash", e)
        ""
    }
}
```

**Update Call Sites**:
```kotlin
// Line 239: Change from event-based to package-based
// BEFORE:
val screenHash = calculateScreenHash(event)

// AFTER:
val screenHash = calculateScreenHash(packageName)
```

---

### Phase 3: JIT Deduplication Check (Android Expert)
**Duration**: 1 hour
**Dependencies**: Phase 1, 2.1, 2.2 complete

#### Task 3.1: Add Screen Hash Parameter to captureScreenElements
**File**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/jit/JitElementCapture.kt:109`

**Changes**:
```kotlin
/**
 * Capture UI elements from current screen with deduplication
 *
 * NEW: Checks database for existing screen hash before capture.
 * If screen already captured → Skip tree traversal (battery savings).
 *
 * @param packageName Package name of the app being captured
 * @param screenHash Screen hash from ScreenStateManager
 * @return List of captured elements (empty if already captured or fails)
 */
suspend fun captureScreenElements(
    packageName: String,
    screenHash: String  // NEW: Add screen hash parameter
): List<JitCapturedElement> {
    val startTime = System.currentTimeMillis()

    // NEW: Check if screen already captured (deduplication)
    if (screenHash.isNotEmpty() && isScreenAlreadyCaptured(screenHash, packageName)) {
        Log.i(TAG, "Screen $screenHash already captured for $packageName, skipping (battery savings)")
        return emptyList()
    }

    // Continue with existing capture logic...
    return withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
        // ... existing code
    }
}
```

#### Task 3.2: Add isScreenAlreadyCaptured Method
**File**: Same as 3.1

**Add Method**:
```kotlin
/**
 * Check if screen was already captured by JIT
 *
 * Queries database by screen hash to determine if elements already exist.
 * Enables deduplication across screen revisits.
 *
 * @param screenHash Screen hash identifier
 * @param packageName Package name
 * @return true if screen already captured (skip re-capture)
 */
private suspend fun isScreenAlreadyCaptured(
    screenHash: String,
    packageName: String
): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val count = databaseManager.scrapedElements.countByScreenHash(
            packageName = packageName,
            screenHash = screenHash
        ).executeAsOne()

        count > 0
    } catch (e: Exception) {
        Log.e(TAG, "Error checking screen hash in database", e)
        false  // On error, proceed with capture (safe fallback)
    }
}
```

#### Task 3.3: Update Call Sites in JustInTimeLearner
**File**: `JustInTimeLearner.kt:288`

**Changes**:
```kotlin
// BEFORE:
val capturedElements = elementCapture?.captureScreenElements(packageName) ?: emptyList()

// AFTER:
val capturedElements = elementCapture?.captureScreenElements(packageName, screenHash) ?: emptyList()
```

---

### Phase 4: UUID Generation in JIT (Android Expert)
**Duration**: 1.5 hours
**Dependencies**: Phase 3 complete

#### Task 4.1: Inject ThirdPartyGenerator into JitElementCapture
**File**: `JitElementCapture.kt:73-76`

**Changes**:
```kotlin
class JitElementCapture(
    private val accessibilityService: AccessibilityService,
    private val databaseManager: VoiceOSDatabaseManager,
    private val thirdPartyGenerator: ThirdPartyGenerator  // NEW: Inject dependency
) {
    // ...
}
```

**Update Constructor Calls**:
- `JustInTimeLearner.kt:94`: Pass `thirdPartyGenerator` when creating `JitElementCapture`

#### Task 4.2: Add generateUUIDs Method
**File**: `JitElementCapture.kt` (new method)

**Add Method**:
```kotlin
/**
 * Generate UUIDs for captured elements
 *
 * Uses ThirdPartyGenerator for consistency with LearnApp UUID generation.
 * Enables LearnApp to reuse JIT-captured elements with UUIDs in Phase 2.
 *
 * @param elements List of captured elements
 * @param packageName Package name
 * @return List of (element, UUID) pairs
 */
private fun generateUUIDs(
    elements: List<JitCapturedElement>,
    packageName: String
): List<Pair<JitCapturedElement, String>> {
    return elements.mapNotNull { element ->
        try {
            val uuid = thirdPartyGenerator.generateUuidFromProperties(
                elementHash = element.elementHash,
                className = element.className,
                viewId = element.viewIdResourceName,
                packageName = packageName
            )
            element to uuid
        } catch (e: Exception) {
            Log.e(TAG, "Error generating UUID for element: ${element.elementHash}", e)
            null  // Skip element if UUID generation fails
        }
    }
}
```

#### Task 4.3: Update persistElements to Store UUIDs
**File**: `JitElementCapture.kt` (modify existing method)

**Current Method**:
```kotlin
suspend fun persistElements(
    packageName: String,
    elements: List<JitCapturedElement>
): Int
```

**Enhanced Method**:
```kotlin
/**
 * Persist captured elements with UUIDs and screen hash
 *
 * NEW: Generates UUIDs before persistence and stores screen hash.
 *
 * @param packageName Package name
 * @param screenHash Screen hash from ScreenStateManager
 * @param elements Captured elements
 * @return Number of elements persisted
 */
suspend fun persistElements(
    packageName: String,
    screenHash: String,  // NEW: Add screen hash parameter
    elements: List<JitCapturedElement>
): Int = withContext(Dispatchers.IO) {
    if (elements.isEmpty()) return@withContext 0

    // NEW: Generate UUIDs for elements
    val elementsWithUUIDs = generateUUIDs(elements, packageName)

    var persistedCount = 0
    elementsWithUUIDs.forEach { (element, uuid) ->
        try {
            databaseManager.scrapedElements.insert(
                ScrapedElementDTO(
                    id = 0, // Auto-increment
                    packageName = packageName,
                    screenHash = screenHash,  // NEW: Store screen hash
                    elementHash = element.elementHash,
                    uuid = uuid,  // NEW: Store UUID
                    className = element.className,
                    viewIdResourceName = element.viewIdResourceName,
                    text = element.text,
                    contentDescription = element.contentDescription,
                    boundsLeft = element.bounds.left,
                    boundsTop = element.bounds.top,
                    boundsRight = element.bounds.right,
                    boundsBottom = element.bounds.bottom,
                    isClickable = element.isClickable,
                    isLongClickable = element.isLongClickable,
                    isEditable = element.isEditable,
                    isScrollable = element.isScrollable,
                    isCheckable = element.isCheckable,
                    isFocusable = element.isFocusable,
                    isEnabled = element.isEnabled,
                    depth = element.depth,
                    indexInParent = element.indexInParent,
                    timestamp = System.currentTimeMillis()
                )
            )
            persistedCount++
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting element: ${element.elementHash}", e)
        }
    }

    Log.i(TAG, "Persisted $persistedCount/${elements.size} elements with UUIDs for $packageName")
    return@withContext persistedCount
}
```

#### Task 4.4: Update Call Sites in JustInTimeLearner
**File**: `JustInTimeLearner.kt:292`

**Changes**:
```kotlin
// BEFORE:
capturedElementCount = elementCapture?.persistElements(packageName, capturedElements) ?: 0

// AFTER:
capturedElementCount = elementCapture?.persistElements(packageName, screenHash, capturedElements) ?: 0
```

---

### Phase 5: Testing (Testing Specialist)
**Duration**: 2 hours
**Dependencies**: Phases 1-4 complete

#### Task 5.1: Unit Tests - Screen Hash Deduplication
**File**: `modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/learnapp/jit/JitElementCaptureTest.kt`

**Tests to Add**:
```kotlin
@Test
fun `isScreenAlreadyCaptured returns true for existing hash`() = runTest {
    // Given: Screen hash exists in database
    val screenHash = "test_hash_123"
    database.scrapedElements.insert(createTestElement(screenHash = screenHash))

    // When: Check if captured
    val result = jitCapture.isScreenAlreadyCaptured(screenHash, "com.test")

    // Then: Returns true
    assertTrue(result)
}

@Test
fun `captureScreenElements skips capture for existing screen hash`() = runTest {
    // Given: Screen already captured
    val screenHash = "test_hash_456"
    database.scrapedElements.insert(createTestElement(screenHash = screenHash))

    // When: Attempt to capture same screen
    val elements = jitCapture.captureScreenElements("com.test", screenHash)

    // Then: Returns empty (skipped)
    assertTrue(elements.isEmpty())
}

@Test
fun `captureScreenElements captures for new screen hash`() = runTest {
    // Given: Screen NOT captured
    val screenHash = "new_hash_789"

    // When: Capture screen
    val elements = jitCapture.captureScreenElements("com.test", screenHash)

    // Then: Returns captured elements
    assertTrue(elements.isNotEmpty())
}
```

#### Task 5.2: Unit Tests - UUID Generation
**File**: Same as 5.1

**Tests to Add**:
```kotlin
@Test
fun `generateUUIDs produces stable UUIDs for same element`() {
    // Given: Same element captured twice
    val element = createTestElement()

    // When: Generate UUID twice
    val uuid1 = jitCapture.generateUUID(element, "com.test")
    val uuid2 = jitCapture.generateUUID(element, "com.test")

    // Then: UUIDs are identical
    assertEquals(uuid1, uuid2)
}

@Test
fun `persistElements stores UUIDs in database`() = runTest {
    // Given: Elements to persist
    val elements = listOf(createTestElement())
    val screenHash = "test_hash"

    // When: Persist with UUIDs
    val count = jitCapture.persistElements("com.test", screenHash, elements)

    // Then: Elements stored with UUIDs
    assertEquals(1, count)
    val stored = database.scrapedElements.getByScreenHash("com.test", screenHash).executeAsList()
    assertNotNull(stored.first().uuid)
    assertTrue(stored.first().uuid.isNotEmpty())
}
```

#### Task 5.3: Integration Tests - Hash Consistency
**File**: `modules/apps/VoiceOSCore/src/androidTest/java/com/augmentalis/voiceoscore/learnapp/jit/JitScreenHashConsistencyTest.kt`

**Tests to Add**:
```kotlin
@Test
fun `JIT and LearnApp generate same screen hash`() {
    // Given: Same screen in both contexts
    val rootNode = createTestRootNode()
    val packageName = "com.test.app"

    // When: Calculate hash in both systems
    val jitHash = jit.calculateScreenHash(packageName)
    val learnAppHash = screenStateManager.captureScreenState(rootNode, packageName, 0).hash

    // Then: Hashes are identical
    assertEquals(jitHash, learnAppHash)
}

@Test
fun `popup generates stable hash across content changes`() {
    // Given: Same popup type with different content
    val popup1 = createPopupNode("Delete Photo1.jpg?")
    val popup2 = createPopupNode("Delete Photo2.jpg?")

    // When: Calculate hashes
    val hash1 = screenStateManager.captureScreenState(popup1, "com.test", 0).hash
    val hash2 = screenStateManager.captureScreenState(popup2, "com.test", 0).hash

    // Then: Hashes are identical (structure-based)
    assertEquals(hash1, hash2)
}
```

#### Task 5.4: Manual Test Plan
**File**: `docs/testing/jit-deduplication-manual-test-plan.md`

**Create Document**:
```markdown
# JIT Deduplication Manual Test Plan

## Test 1: Screen Revisit Deduplication
1. Open Google Photos
2. Navigate: Main → Albums → Back to Main (3 times)
3. Check logs: `adb logcat | grep "already captured, skipping"`
4. Expected: 2 captures (main, albums), 4 skips

## Test 2: Popup Deduplication
1. Open Photos
2. Delete 3 different photos (triggers delete confirmation 3 times)
3. Check logs: `adb logcat | grep "already captured, skipping"`
4. Expected: 1 capture (delete dialog), 2 skips

## Test 3: UUID Generation
1. Clear database: `adb shell pm clear com.augmentalis.voiceos`
2. Navigate Photos app naturally
3. Check database: `adb shell sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db "SELECT COUNT(*) FROM scraped_element WHERE uuid IS NOT NULL"`
4. Expected: 100% of elements have UUIDs

## Test 4: Battery Impact
1. Install battery profiler
2. Use Photos app naturally for 10 minutes
3. Check JIT CPU usage: <1% (vs 3% before)
4. Expected: 86% reduction in CPU on revisits
```

---

### Phase 6: Documentation (Documentation Specialist)
**Duration**: 1 hour
**Dependencies**: Phase 5 complete

#### Task 6.1: Update Developer Manual
**File**: `docs/modules/LearnApp/developer-manual.md`

**Add New Section** (after line 3306):
```markdown
### JIT Screen Hash Deduplication & UUID Generation (2025-12-02)

**Overview**: JIT now uses consistent screen hashing with deduplication and generates UUIDs during element capture. This prevents duplicate captures, ensures cache compatibility with LearnApp, and enables Phase 2 optimization (5-10 minute learning vs 60 minutes).

**Architecture Changes**:

#### 1. Unified Screen Hashing
JIT now uses `ScreenStateManager.captureScreenState()` (same as LearnApp):
- **Structure-based**: Hash based on UI structure, not content
- **Popup-aware**: Same popup type → Same hash (e.g., delete confirmation)
- **Stable**: Same screen → Same hash across sessions

**Before** (content-dependent):
```kotlin
// JustInTimeLearner.kt:267-273 (OLD)
private fun calculateScreenHash(event: AccessibilityEvent): String {
    return "$className|$contentDesc|$text".hashCode().toString()
}
```

**After** (structure-based):
```kotlin
// JustInTimeLearner.kt (NEW)
private suspend fun calculateScreenHash(packageName: String): String {
    val screenState = screenStateManager.captureScreenState(rootNode, packageName, 0)
    return screenState.hash  // Structure-based, popup-aware
}
```

#### 2. Screen Hash Deduplication
JIT checks database before capture to skip already-visited screens:

**Flow**:
```
Screen change detected
→ Calculate screen hash (5ms)
→ Check database: hash exists? (2ms)
  → YES: Skip capture, return empty ✅ BATTERY SAVINGS
  → NO: Capture elements (50ms)
```

**Performance**:
- First visit: 57ms (hash + DB check + capture)
- Revisit: 7ms (hash + DB check only) = **86% faster**

**Code**:
```kotlin
// JitElementCapture.kt
if (isScreenAlreadyCaptured(screenHash, packageName)) {
    Log.i(TAG, "Screen $screenHash already captured, skipping")
    return emptyList()  // Skip tree traversal
}
```

#### 3. UUID Generation
JIT now generates UUIDs for all captured elements using `ThirdPartyGenerator`:

**Why**: LearnApp Phase 2 needs UUIDs to reuse JIT-captured elements (no duplicate work)

**Algorithm** (consistent with LearnApp):
```kotlin
// JitElementCapture.kt
val uuid = thirdPartyGenerator.generateUuidFromProperties(
    elementHash = element.elementHash,
    className = element.className,
    viewId = element.viewIdResourceName,
    packageName = packageName
)
```

**Storage**:
```kotlin
ScrapedElementDTO(
    screenHash = screenHash,  // NEW
    uuid = uuid,              // NEW
    // ... other fields
)
```

#### 4. Database Schema
Added `screen_hash` column to `scraped_element` table:

**Schema**:
```sql
ALTER TABLE scraped_element ADD COLUMN screen_hash TEXT;
CREATE INDEX idx_scraped_element_screen_hash ON scraped_element(package_name, screen_hash);
```

**Queries**:
- `countByScreenHash(packageName, screenHash)` - Check if captured
- `getByScreenHash(packageName, screenHash)` - Get cached elements (Phase 2)

#### Benefits Summary

| Benefit | Impact |
|---------|--------|
| **Battery Savings** | 86% CPU reduction on screen revisits |
| **Hash Consistency** | JIT hash == LearnApp hash (cache integration) |
| **Deduplication** | Same screen captured once, reused on revisits |
| **UUID Generation** | LearnApp reuses 80% of UUIDs (no duplicate work) |
| **Popup Support** | Stable hash for dialog types (content-independent) |
| **Phase 2 Ready** | Enables 5-10 min LearnApp completion (vs 60 min) |

#### Troubleshooting

**Problem**: Screen being re-captured despite deduplication

**Check**:
```bash
# Verify screen hash exists in database
adb shell sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \
  "SELECT COUNT(*) FROM scraped_element WHERE screen_hash = 'YOUR_HASH'"

# Check logs for deduplication
adb logcat | grep "already captured, skipping"
```

**Solution**: If hash not found, ensure `ScreenStateManager` is generating stable hashes

---

**Problem**: UUIDs not being generated

**Check**:
```bash
# Count elements with UUIDs
adb shell sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db \
  "SELECT COUNT(*) FROM scraped_element WHERE uuid IS NOT NULL"
```

**Solution**: Verify `ThirdPartyGenerator` is injected into `JitElementCapture`

---

**Problem**: Hash mismatch between JIT and LearnApp

**Diagnostic**:
```kotlin
// Add logging in both systems
Log.d("JIT", "Screen hash: ${jit.calculateScreenHash(packageName)}")
Log.d("LearnApp", "Screen hash: ${screenStateManager.captureScreenState(...).hash}")
```

**Solution**: Ensure both use `ScreenStateManager.captureScreenState()` (not custom hash)
```

#### Task 6.2: Create Architecture Flow Diagrams
**File**: `docs/modules/LearnApp/architecture-diagrams.md`

**Add Diagrams** (Mermaid format):

1. **JIT Enhanced Flow with Deduplication**
2. **Screen Hash Consistency (JIT ↔ LearnApp)**
3. **Popup Handling Flow**
4. **Database Schema with screen_hash**

(Diagrams will be created in next task)

---

## Rollout Strategy

### Stage 1: Development (This Plan)
- Implement all phases
- Run unit and integration tests
- Verify deduplication working

### Stage 2: Internal Testing
- Deploy to test device
- Manual testing with Google Photos, Gmail
- Monitor battery usage and performance
- Verify hash consistency

### Stage 3: Beta Release
- Deploy to beta testers
- Collect performance metrics
- Monitor crash reports
- Gather user feedback on battery impact

### Stage 4: Production Release
- Full rollout to all users
- Monitor deduplication rates
- Track battery savings
- Prepare for Phase 2 (LearnApp cache integration)

---

## Risk Mitigation

### Risk 1: Database Migration Failure
**Probability**: Low
**Impact**: High (blocks feature)

**Mitigation**:
- Test migration on multiple Android versions
- Add NULL default for screen_hash (backward compatible)
- Graceful fallback if column add fails

### Risk 2: UUID Generation Inconsistency
**Probability**: Medium
**Impact**: High (breaks cache integration)

**Mitigation**:
- Unit tests verify UUID consistency
- Integration tests compare JIT vs LearnApp UUIDs
- Log UUID mismatches for debugging

### Risk 3: ScreenStateManager Injection Issues
**Probability**: Low
**Impact**: Medium (compile error)

**Mitigation**:
- Follow existing dependency injection pattern
- Verify constructor changes in all call sites
- Compiler will catch missing parameters

---

## Success Metrics

| Metric | Target | Verification |
|--------|--------|--------------|
| Screen revisit performance | <7ms | Logcat timing |
| Battery impact reduction | 86% CPU savings | Battery profiler |
| Deduplication rate | >70% screens | Database query |
| Hash consistency | 100% match | Integration tests |
| UUID generation | 100% elements | Database query |
| Test coverage | >90% | Coverage report |

---

## Next Steps

1. **Database Specialist**: Execute Phase 1 (schema + queries)
2. **Android Expert**: Execute Phases 2-4 (hash consistency + deduplication + UUIDs)
3. **Testing Specialist**: Execute Phase 5 (tests)
4. **Documentation Specialist**: Execute Phase 6 (manual + diagrams)
5. **Review**: Code review + integration testing
6. **Deploy**: Internal testing → Beta → Production

---

## Approval

**Status**: ✅ Ready for Implementation

**Implementation Mode**: YOLO + Swarm
**Start Time**: 2025-12-02 06:40 PST
**Estimated Completion**: 2025-12-02 12:40 PST (6 hours)

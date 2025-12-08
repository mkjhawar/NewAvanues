# JIT Element Deduplication Fix - Specification

**Feature ID:** VOS-JIT-006
**Date:** 2025-12-03
**Status:** Draft
**Priority:** P0 CRITICAL
**Platforms:** Android (VoiceOS)
**Estimated Effort:** 4-6 hours

---

## Executive Summary

Fix critical deduplication bug in JIT (Just-In-Time) element capture that causes RealWear apps (My Control, My Camera, My Files, RealWear launcher) to show 0 persisted elements despite successful capture. Root cause: `getByHash()` query doesn't filter by `appId`, causing cross-app element hash collisions when VoiceOS overlays are visible during initial capture.

**Impact:** JIT learning completely broken for apps that show VoiceOS consent/overlay dialogs before capture.

---

## Problem Statement

### Current State
- JIT captures 4-25 elements successfully for RealWear apps
- Database persistence reports "0 new elements persisted"
- Elements ARE in database but with wrong `appId` (`com.augmentalis.voiceos` instead of `com.realwear.launcher`)
- Subsequent captures skip insertion due to hash collision

### Root Cause Analysis

**Issue 1: Cross-App Deduplication (P0 CRITICAL)**
```kotlin
// Current broken logic:
val existing = databaseManager.scrapedElements.getByHash(element.elementHash)
if (existing != null) {
    continue  // Skips even if appId is different!
}
```

**SQL Query (BROKEN):**
```sql
getByHash:
SELECT * FROM scraped_element WHERE elementHash = ? LIMIT 1;
-- Missing: AND appId = ?
```

**Why This Happens:**
1. VoiceOS consent dialog appears over RealWear launcher
2. Accessibility tree includes BOTH VoiceOS UI + underlying RealWear elements
3. First capture saves RealWear elements with `appId = "com.augmentalis.voiceos"`
4. Second capture (actual RealWear app) finds existing hashes → skips all

**Issue 2: Aggressive Timeout (P1 HIGH)**
- 50ms timeout too short for 25 elements with overlays
- Causes 134-175ms captures to log warnings (non-blocking)

**Issue 3: Silent Failure (P2 MEDIUM)**
- No error logged when element skipped due to hash collision
- No indication that cross-app collision occurred

---

## Functional Requirements

### FR-001: App-Scoped Element Deduplication (P0)
**Platform:** Android (VoiceOS)
**Description:** Element deduplication MUST check both `elementHash` AND `appId` to prevent cross-app collisions.

**Acceptance Criteria:**
- [ ] New SQL query `getByHashAndApp(elementHash, appId)` added to `ScrapedElement.sq`
- [ ] Repository interface adds `getByHashAndApp()` method
- [ ] SQLDelight implementation added
- [ ] `JitElementCapture.persistElements()` uses new query
- [ ] Elements with same hash but different appIds can coexist in database

**Technical Details:**
- Query must use composite WHERE clause: `elementHash = ? AND appId = ?`
- Index on `(appId, elementHash)` for performance
- Backward compatible (existing `getByHash` unchanged)

---

### FR-002: Increased Capture Timeout (P1)
**Platform:** Android (VoiceOS)
**Description:** Increase capture timeout from 50ms to 200ms to handle overlay scenarios.

**Acceptance Criteria:**
- [ ] `CAPTURE_TIMEOUT_MS` increased to 200ms in `JitElementCapture.kt`
- [ ] Timeout warnings logged only when >200ms
- [ ] No false-positive warnings for normal overlay scenarios

**Rationale:**
- RealWear apps with overlays: 134-175ms observed
- 200ms allows 4x safety margin
- Timeout is fail-safe, not hard limit

---

### FR-003: Enhanced Diagnostic Logging (P2)
**Platform:** Android (VoiceOS)
**Description:** Add detailed logging for deduplication decisions to aid debugging.

**Acceptance Criteria:**
- [ ] Log when element skipped due to existing hash
- [ ] **ERROR** log when cross-app collision detected (`existing.appId != packageName`)
- [ ] Log includes both old and new appId for comparison
- [ ] Logs use structured format for filtering

**Example Output:**
```
D/JitElementCapture: Checking element abc123 for app com.realwear.launcher
W/JitElementCapture: Element abc123 already exists with appId=com.augmentalis.voiceos
E/JitElementCapture: APP ID MISMATCH: Element from com.augmentalis.voiceos blocking com.realwear.launcher
```

---

## Non-Functional Requirements

### NFR-001: Performance
- Database query with `(appId, elementHash)` index: <5ms
- No regression in capture performance
- Total persistence time: <100ms for 25 elements

### NFR-002: Data Integrity
- No orphaned elements with mismatched appIds
- Existing database entries remain valid
- Migration not required (new column already exists)

### NFR-003: Backward Compatibility
- Existing `getByHash()` query remains unchanged
- Old code paths continue to work
- No breaking changes to public APIs

---

## Technical Implementation

### Component 1: Database Layer (SQLDelight)

**File:** `libraries/core/database/.../ScrapedElement.sq`

**Changes:**
```sql
-- Add new query (after existing getByHash)
getByHashAndApp:
SELECT * FROM scraped_element
WHERE appId = ? AND elementHash = ?
LIMIT 1;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_scraped_element_app_hash
ON scraped_element(appId, elementHash);
```

**Verification:**
- Run SQLDelight code generation: `./gradlew generateCommonMainDatabaseInterface`
- Verify generated query methods exist

---

### Component 2: Repository Interface

**File:** `libraries/core/database/.../IScrapedElementRepository.kt`

**Changes:**
```kotlin
interface IScrapedElementRepository {

    // Existing method (keep for backward compatibility)
    suspend fun getByHash(elementHash: String): ScrapedElementDTO?

    // NEW: App-scoped lookup
    /**
     * Get element by hash for a specific app.
     * Prevents cross-app hash collisions.
     *
     * @param elementHash Element fingerprint hash
     * @param appId Package name of the app
     * @return Element DTO if found, null otherwise
     */
    suspend fun getByHashAndApp(elementHash: String, appId: String): ScrapedElementDTO?

    // ... other methods
}
```

---

### Component 3: Repository Implementation

**File:** `libraries/core/database/.../impl/SQLDelightScrapedElementRepository.kt`

**Changes:**
```kotlin
class SQLDelightScrapedElementRepository(
    private val queries: ScrapedElementQueries
) : IScrapedElementRepository {

    // Existing method (unchanged)
    override suspend fun getByHash(elementHash: String): ScrapedElementDTO? =
        withContext(Dispatchers.Default) {
            queries.getByHash(elementHash).executeAsOneOrNull()?.toScrapedElementDTO()
        }

    // NEW: App-scoped implementation
    override suspend fun getByHashAndApp(elementHash: String, appId: String): ScrapedElementDTO? =
        withContext(Dispatchers.Default) {
            queries.getByHashAndApp(appId, elementHash).executeAsOneOrNull()?.toScrapedElementDTO()
        }

    // ... other methods
}
```

**Note:** Parameter order matches SQL query: `(appId, elementHash)`

---

### Component 4: JIT Element Capture Logic

**File:** `modules/apps/VoiceOSCore/.../jit/JitElementCapture.kt`

**Changes:**

```kotlin
class JitElementCapture(...) {
    companion object {
        private const val TAG = "JitElementCapture"

        // CHANGE: Increase timeout
        private const val CAPTURE_TIMEOUT_MS = 200L  // Was 50L

        // ... rest unchanged
    }

    suspend fun persistElements(
        packageName: String,
        elements: List<JitCapturedElement>,
        screenHash: String? = null
    ): Int = withContext(Dispatchers.IO) {
        var newCount = 0
        val timestamp = System.currentTimeMillis()

        for (element in elements) {
            try {
                // CHANGE: Use app-scoped lookup
                val existing = databaseManager.scrapedElements.getByHashAndApp(
                    element.elementHash,
                    packageName
                )

                if (existing != null) {
                    // CHANGE: Add diagnostic logging
                    Log.v(TAG, "Element already exists for $packageName: ${element.elementHash}")
                    continue
                }

                // NEW: Check for cross-app collision
                val globalExisting = databaseManager.scrapedElements.getByHash(element.elementHash)
                if (globalExisting != null && globalExisting.appId != packageName) {
                    Log.e(TAG, "APP ID MISMATCH: Element ${element.elementHash} from " +
                               "${globalExisting.appId} blocking $packageName")
                }

                // Insert new element (unchanged)
                val elementDTO = ScrapedElementDTO(
                    id = 0L,
                    elementHash = element.elementHash,
                    appId = packageName,
                    uuid = element.uuid,
                    className = element.className,
                    viewIdResourceName = element.viewIdResourceName,
                    text = element.text,
                    contentDescription = element.contentDescription,
                    bounds = "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    isClickable = if (element.isClickable) 1L else 0L,
                    isLongClickable = if (element.isLongClickable) 1L else 0L,
                    isEditable = if (element.isEditable) 1L else 0L,
                    isScrollable = if (element.isScrollable) 1L else 0L,
                    isCheckable = if (element.isCheckable) 1L else 0L,
                    isFocusable = if (element.isFocusable) 1L else 0L,
                    isEnabled = if (element.isEnabled) 1L else 0L,
                    depth = element.depth.toLong(),
                    indexInParent = element.indexInParent.toLong(),
                    scrapedAt = timestamp,
                    semanticRole = null,
                    inputType = null,
                    visualWeight = null,
                    isRequired = null,
                    formGroupId = null,
                    placeholderText = null,
                    validationPattern = null,
                    backgroundColor = null,
                    screen_hash = screenHash
                )
                databaseManager.scrapedElements.insert(elementDTO)

                newCount++
                Log.v(TAG, "Persisted element for $packageName: ${element.elementHash}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist element: ${element.elementHash}", e)
            }
        }

        Log.i(TAG, "Persisted $newCount new elements out of ${elements.size} captured for $packageName")
        newCount
    }
}
```

---

## User Stories

### US-001: Developer - RealWear App JIT Learning
**As a** VoiceOS developer
**I want** JIT element capture to work correctly for RealWear apps
**So that** users can control these apps with voice commands

**Acceptance Criteria:**
- [ ] RealWear launcher captures 4+ elements successfully
- [ ] All captured elements persist to database with correct appId
- [ ] No cross-app hash collisions
- [ ] Element count > 0 in JustInTimeLearner logs

---

### US-002: QA - Debugging Failed Captures
**As a** QA engineer
**I want** detailed logs when elements are skipped
**So that** I can identify deduplication issues quickly

**Acceptance Criteria:**
- [ ] ERROR log when cross-app collision detected
- [ ] Log includes both appIds for comparison
- [ ] Logs filterable by `APP ID MISMATCH` string

---

## Testing Requirements

### Unit Tests

**File:** `JitElementCaptureTest.kt`

```kotlin
@Test
fun `persistElements should allow same hash for different apps`() = runTest {
    // Arrange
    val element = createTestElement(hash = "abc123")

    // Act: Persist for app1
    val count1 = capture.persistElements("com.app1", listOf(element))

    // Act: Persist for app2 with same hash
    val count2 = capture.persistElements("com.app2", listOf(element))

    // Assert
    assertEquals(1, count1)
    assertEquals(1, count2)  // Should NOT be 0!

    // Verify both exist in database
    val app1Element = repository.getByHashAndApp("abc123", "com.app1")
    val app2Element = repository.getByHashAndApp("abc123", "com.app2")

    assertNotNull(app1Element)
    assertNotNull(app2Element)
    assertEquals("com.app1", app1Element!!.appId)
    assertEquals("com.app2", app2Element!!.appId)
}

@Test
fun `persistElements should skip duplicate for same app`() = runTest {
    // Arrange
    val element = createTestElement(hash = "abc123")

    // Act: Persist twice for same app
    val count1 = capture.persistElements("com.app1", listOf(element))
    val count2 = capture.persistElements("com.app1", listOf(element))

    // Assert
    assertEquals(1, count1)
    assertEquals(0, count2)  // Duplicate skipped
}

@Test
fun `getByHashAndApp should return null for different app`() = runTest {
    // Arrange
    val element = createTestElement(hash = "abc123")
    capture.persistElements("com.app1", listOf(element))

    // Act
    val result = repository.getByHashAndApp("abc123", "com.app2")

    // Assert
    assertNull(result)  // Different app, should not find
}
```

### Integration Tests

**File:** `JitElementCaptureIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class JitElementCaptureIntegrationTest {

    @Test
    fun `real RealWear launcher capture after VoiceOS overlay`() = runTest {
        // Simulate the actual failure scenario:

        // 1. Capture with VoiceOS overlay visible
        val overlayElements = captureWithOverlay("com.augmentalis.voiceos")
        val overlayCount = capture.persistElements("com.augmentalis.voiceos", overlayElements)
        assertTrue(overlayCount > 0)

        // 2. Capture real RealWear launcher (same elements)
        val launcherElements = captureRealWearLauncher("com.realwear.launcher")
        val launcherCount = capture.persistElements("com.realwear.launcher", launcherElements)

        // Assert: Should NOT be 0!
        assertTrue(launcherCount > 0, "RealWear launcher elements should persist")

        // Verify both sets exist
        val voiceosElements = repository.getByApp("com.augmentalis.voiceos")
        val launcherDbElements = repository.getByApp("com.realwear.launcher")

        assertTrue(voiceosElements.isNotEmpty())
        assertTrue(launcherDbElements.isNotEmpty())
    }
}
```

### Manual Testing

**Test Case 1: RealWear Apps**
1. Reset database: `adb shell run-as com.augmentalis.voiceos rm -rf databases/`
2. Launch VoiceOS
3. Open RealWear launcher → Consent dialog appears
4. Allow learning
5. Check logs: `adb logcat | grep JitElementCapture`
6. **Expected:** `Persisted 4 new elements` (NOT 0)

**Test Case 2: Cross-App Verification**
1. Capture app1 with specific element
2. Capture app2 with same element (e.g., standard "Settings" button)
3. Query database: `SELECT COUNT(*) FROM scraped_element WHERE elementHash = 'xyz'`
4. **Expected:** 2 rows (one per app)

---

## Dependencies

### Upstream
- SQLDelight database schema (already includes `screen_hash` column)
- VoiceOSDatabaseManager singleton
- ThirdPartyUuidGenerator for element UUIDs

### Downstream
- JustInTimeLearner (consumes element count from persistElements)
- Voice command generation (depends on persisted elements)
- ExplorationEngine (uses same repository)

### External
- None (internal VoiceOS changes only)

---

## Success Criteria

**MUST (Required for Done):**
- [ ] RealWear launcher captures and persists 4+ elements consistently
- [ ] Database contains elements for correct appId (`com.realwear.launcher`)
- [ ] No cross-app hash collisions (elements with same hash, different appIds coexist)
- [ ] All unit tests pass (3 new tests)
- [ ] Integration test passes (RealWear overlay scenario)
- [ ] Build successful with no warnings

**SHOULD (Highly Recommended):**
- [ ] Manual testing on physical RealWear device
- [ ] Logs include diagnostic information for cross-app collisions
- [ ] Performance benchmark: <100ms for 25 elements

**COULD (Nice to Have):**
- [ ] Automated test using RealWear launcher APK
- [ ] Database migration to fix existing mismatched appIds

---

## Risk Assessment

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Existing data with wrong appIds | MEDIUM | 100% | Accept - new captures will be correct; OR create migration script |
| Performance regression with composite index | LOW | 20% | Benchmark before/after; index is on primary lookup path |
| Breaking changes to repository API | LOW | 10% | New method added, existing methods unchanged |

---

## Timeline Estimate

| Phase | Duration | Owner |
|-------|----------|-------|
| Database query + index | 30 min | Developer |
| Repository interface + impl | 30 min | Developer |
| JitElementCapture changes | 1 hour | Developer |
| Unit tests | 1 hour | Developer |
| Integration test | 1 hour | Developer |
| Manual testing | 1 hour | QA |
| Code review + fixes | 1 hour | Team |

**Total:** 6 hours (conservative)
**Optimistic:** 4 hours

---

## Rollout Strategy

### Phase 1: Development (Day 1)
- Implement database changes
- Implement repository changes
- Update JitElementCapture logic
- Write unit tests

### Phase 2: Testing (Day 1-2)
- Run unit tests
- Run integration tests
- Manual testing on RealWear device
- Log analysis

### Phase 3: Deployment (Day 2)
- Code review
- Merge to main branch
- Build release candidate
- Deploy to beta testers

### Phase 4: Monitoring (Day 3+)
- Monitor crash logs
- Monitor capture success rate
- Verify element counts in production database

---

## Monitoring & Metrics

**Key Metrics:**
- Element capture success rate (target: >95%)
- Cross-app collision rate (target: 0)
- Average persistence time (target: <100ms)
- RealWear app element count (target: >4 per capture)

**Logging:**
- INFO: Successful persistence counts
- WARN: Timeouts (>200ms)
- ERROR: Cross-app collisions detected

**Alerts:**
- Element count = 0 for known apps (RealWear)
- Cross-app collision rate > 1%
- Persistence time > 500ms

---

## Related Documentation

- **Analysis:** `/analyze .cot` output from 2025-12-03
- **Logs:** `/Users/manoj_mbpm14/Downloads/junk/realwar_apps_logs.txt`
- **Database Dump:** `/Users/manoj_mbpm14/Downloads/junk/voiceos_db.sql`
- **Original Spec:** `docs/specifications/jit-screen-hash-uuid-deduplication-spec.md`
- **Developer Manual:** `docs/modules/LearnApp/developer-manual.md`

---

**Version:** 1.0.0
**Created:** 2025-12-03
**Last Updated:** 2025-12-03
**Author:** VoiceOS Development Team
**Status:** Ready for Implementation

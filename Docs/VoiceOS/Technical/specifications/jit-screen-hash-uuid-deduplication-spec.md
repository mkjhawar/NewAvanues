# Feature Specification: JIT Screen Hash Deduplication & UUID Generation

**Spec ID:** 009
**Created:** 2025-12-02
**Status:** Approved
**Platforms:** Android
**Priority:** P1 (High)
**Author:** User Requirements + AI Analysis

---

## Executive Summary

Enhance Just-In-Time (JIT) learning to use consistent screen hashing with deduplication and generate UUIDs during element capture. This prevents duplicate captures of the same screen during natural usage (battery savings), ensures hash consistency between JIT and LearnApp for proper cache integration, and enables Phase 2 of two-phase learning optimization (5-10 minute LearnApp completion vs 60 minutes).

**Key Benefits:**
- **86% faster** on screen revisits (50ms → 7ms)
- **Battery savings** - Skip re-capturing visited screens during natural usage
- **Hash consistency** - JIT and LearnApp use same algorithm (structure-based, popup-aware)
- **UUID generation** - JIT generates UUIDs (LearnApp reuses 80% of them)
- **Enables Phase 2** - Two-phase learning optimization (Spec 008)

---

## Problem Statement

### Current State - Hash Inconsistency

**JIT Hashing** (content-dependent):
```kotlin
// JustInTimeLearner.kt:267-273
private fun calculateScreenHash(event: AccessibilityEvent): String {
    val className = event.className?.toString() ?: ""
    val contentDesc = event.contentDescription?.toString() ?: ""
    val text = event.text?.joinToString("|") ?: ""
    return "$className|$contentDesc|$text".hashCode().toString()
}
```

**LearnApp Hashing** (structure-based, popup-aware):
```kotlin
// ScreenStateManager.kt:119-125
val windowInfo = detectPopupWindow(rootNode, packageName)
val hash = if (windowInfo.isPopup) {
    fingerprinter.calculatePopupFingerprint(rootNode, windowInfo.popupType)
} else {
    fingerprinter.calculateScreenFingerprint(rootNode)
}
```

**Pain Points:**

| Issue | Impact | Example |
|-------|--------|---------|
| **Different hashes for same screen** | JIT hash ≠ LearnApp hash → No cache reuse | Delete dialog: JIT="AlertDialog\|Delete Photo1.jpg\|..." vs LearnApp="popup_type_delete_confirmation" |
| **Content-dependent hashing** | Same popup type → Different hashes | "Delete Photo1" (hash A) ≠ "Delete Photo2" (hash B) |
| **No deduplication** | Re-captures same screen multiple times | User sees Photos main screen 10 times → JIT captures 10 times (battery waste) |
| **No UUID generation** | LearnApp must generate UUIDs for 80% of elements | Duplicate work, slows Phase 2 optimization |
| **Popup inconsistency** | Popups captured differently | JIT might see popup first → LearnApp can't reuse |

### Desired State - Consistent Hashing with Deduplication

**JIT Enhanced Flow:**
```
1. Screen change detected
2. Calculate screen hash using ScreenStateManager (structure-based)
3. Check database: Screen hash exists?
   → YES: Skip capture (already captured) ✅ BATTERY SAVINGS
   → NO: Continue to step 4
4. Capture elements from accessibility tree
5. Generate UUIDs for elements ✅ NEW
6. Store elements with screen hash + UUIDs
7. Generate voice commands
```

**Benefits:**
- ✅ **Consistent hashing**: JIT and LearnApp use same algorithm
- ✅ **Deduplication**: Screen captured once, reused on revisits
- ✅ **Popup support**: Same hash for same popup type (content-independent)
- ✅ **UUID generation**: Done once by JIT, reused by LearnApp
- ✅ **Battery efficient**: Skip tree traversal for known screens

---

## Functional Requirements

### FR-001: Unified Screen Hashing (Android)
**Priority**: P0 (Critical)

JIT must use `ScreenStateManager.captureScreenState()` for screen hash calculation (same as LearnApp).

**Acceptance Criteria:**
- [ ] JIT calculates screen hash via `ScreenStateManager` instead of custom `calculateScreenHash()`
- [ ] Screen hash is structure-based (not content-dependent)
- [ ] Popup windows generate stable hash based on dialog type (not content)
- [ ] Hash algorithm identical to LearnApp's algorithm

**Test Cases:**
```kotlin
// Test 1: Same screen, different content
val screen1Hash = jit.calculateScreenHash("Delete Photo1.jpg?")
val screen2Hash = jit.calculateScreenHash("Delete Photo2.jpg?")
assert(screen1Hash == screen2Hash) // Same popup type → Same hash

// Test 2: JIT and LearnApp consistency
val jitHash = jit.getScreenHash(rootNode, packageName)
val learnAppHash = screenStateManager.captureScreenState(rootNode, packageName, 0).hash
assert(jitHash == learnAppHash) // Same algorithm → Same hash
```

### FR-002: Screen Hash Deduplication (Android)
**Priority**: P0 (Critical)

JIT must check database before capturing to skip already-visited screens.

**Acceptance Criteria:**
- [ ] Query database by screen hash before element capture
- [ ] If screen hash exists → Skip capture, return empty list
- [ ] If screen hash NOT exists → Continue with capture
- [ ] Log: "Screen {hash} already captured, skipping (battery savings)"

**Performance Target:**
- Screen hash check: <2ms (database index query)
- Total overhead on revisit: <7ms (vs 50ms full capture)
- Battery savings: 86% on revisited screens

**Test Cases:**
```kotlin
// Test: First visit captures, second visit skips
val elements1 = jit.captureScreenElements("com.photos") // Captures 20 elements
val elements2 = jit.captureScreenElements("com.photos") // Same screen, returns empty
assert(elements1.size == 20)
assert(elements2.isEmpty()) // Skipped due to deduplication
```

### FR-003: UUID Generation in JIT (Android)
**Priority**: P1 (High)

JIT must generate UUIDs for captured elements using `ThirdPartyGenerator`.

**Acceptance Criteria:**
- [ ] Generate UUID for each captured element using `ThirdPartyGenerator.generateUuidFromProperties()`
- [ ] UUID based on: elementHash, className, viewId, packageName
- [ ] Store UUID in `scraped_element.uuid` column
- [ ] UUIDs available for LearnApp Phase 2 cache reuse

**Algorithm Consistency:**
```kotlin
// JIT UUID generation (NEW)
val uuid = thirdPartyGenerator.generateUuidFromProperties(
    elementHash = element.elementHash,
    className = element.className,
    viewId = element.viewIdResourceName,
    packageName = packageName
)

// LearnApp UUID generation (EXISTING)
val uuid = thirdPartyGenerator.generateUuidFromNode(
    node = element.node,
    packageName = packageName
)

// Must produce IDENTICAL UUIDs for same element
```

**Test Cases:**
```kotlin
// Test: JIT-generated UUID matches LearnApp-generated UUID
val jitElement = jit.captureElement(node, packageName)
val learnAppElement = learnApp.registerElement(node, packageName)
assert(jitElement.uuid == learnAppElement.uuid) // Same UUID algorithm
```

### FR-004: Popup Window Handling (Android)
**Priority**: P1 (High)

JIT must correctly identify and hash popup/dialog windows.

**Acceptance Criteria:**
- [ ] Detect popup windows using `ScreenStateManager.detectPopupWindow()`
- [ ] Generate stable hash for popup type (not content)
- [ ] Capture popup elements with UUIDs
- [ ] Deduplication works across popup invocations

**Popup Types:**
- AlertDialog (confirmation, error, info)
- BottomSheet dialogs
- Custom dialogs
- System permission dialogs (filtered out)

**Test Cases:**
```kotlin
// Test: Same popup type → Same hash
jit.onScreenChange() // Shows "Delete Photo1.jpg?"
val hash1 = jit.getLastScreenHash()

jit.onScreenChange() // Shows "Delete Photo2.jpg?" (same dialog type)
val hash2 = jit.getLastScreenHash()

assert(hash1 == hash2) // Structure-based hash, ignores content
```

### FR-005: Database Schema Enhancement (Android)
**Priority**: P1 (High)

Add `screen_hash` column to `scraped_element` table for efficient queries.

**Schema Changes:**
```sql
-- Add screen_hash column
ALTER TABLE scraped_element ADD COLUMN screen_hash TEXT;

-- Create index for fast lookups
CREATE INDEX idx_scraped_element_screen_hash
ON scraped_element(package_name, screen_hash);
```

**Migration Strategy:**
- [ ] Add column with default NULL
- [ ] Populate existing rows with NULL (acceptable)
- [ ] New captures include screen_hash
- [ ] Index created for query performance

---

## Non-Functional Requirements

### NFR-001: Performance
**Target**: <7ms screen hash check (vs 50ms full capture)

| Operation | Before | After | Improvement |
|-----------|--------|-------|-------------|
| First visit | 50ms capture | 5ms hash + 2ms DB check + 50ms capture = 57ms | -7ms (negligible) |
| Revisit | 50ms capture | 5ms hash + 2ms DB check = 7ms | **86% faster** ✅ |

### NFR-002: Battery Impact
**Target**: 86% reduction in CPU usage on screen revisits

- **Before**: Traverse full accessibility tree (50ms CPU) every screen change
- **After**: Hash + DB check (7ms CPU) on revisits, skip tree traversal
- **Typical Session**: 50 screen changes, 70% revisits → 35 full traversals saved

### NFR-003: Memory Footprint
**Target**: <100KB additional memory for screen hash cache

- Screen hash: 64 bytes × 100 screens = 6.4KB
- UUID: 36 bytes × 2000 elements = 72KB
- **Total**: ~78KB (within target)

### NFR-004: Backward Compatibility
**Target**: Zero impact on existing learned apps

- Existing `scraped_element` rows: `screen_hash` = NULL (acceptable)
- New captures: Include `screen_hash`
- Database queries: Handle NULL gracefully
- No migration required for old data

---

## Technical Architecture

### Component Changes

#### 1. JustInTimeLearner.kt
**Changes:**
- Replace `calculateScreenHash(event)` with `screenStateManager.captureScreenState()`
- Add `ScreenStateManager` dependency injection
- Store `screenHash` in database with elements

**Before:**
```kotlin
private fun calculateScreenHash(event: AccessibilityEvent): String {
    return "$className|$contentDesc|$text".hashCode().toString()
}
```

**After:**
```kotlin
private suspend fun calculateScreenHash(packageName: String): String {
    val rootNode = accessibilityService.rootInActiveWindow ?: return ""
    val screenState = screenStateManager.captureScreenState(rootNode, packageName, 0)
    return screenState.hash
}
```

#### 2. JitElementCapture.kt
**Changes:**
- Add screen hash deduplication check before capture
- Add UUID generation after capture
- Store screen hash with elements in database

**New Methods:**
```kotlin
// Check if screen already captured
private suspend fun isScreenAlreadyCaptured(screenHash: String, packageName: String): Boolean

// Generate UUIDs for elements
private fun generateUUIDs(elements: List<JitCapturedElement>, packageName: String): List<Pair<JitCapturedElement, String>>

// Store elements with screen hash and UUIDs
suspend fun persistElements(packageName: String, screenHash: String, elements: List<JitCapturedElement>): Int
```

**Flow:**
```
captureScreenElements(packageName, screenHash) {
    1. Check: isScreenAlreadyCaptured(screenHash)?
       → YES: return empty (skip capture)
       → NO: continue
    2. Traverse accessibility tree (existing)
    3. Generate UUIDs for captured elements (NEW)
    4. Persist with screen hash + UUIDs (NEW)
    5. Return elements
}
```

#### 3. Database Schema (SQLDelight)
**File**: `VoiceOSDatabase.sq`

**Changes:**
```sql
-- scraped_element table enhancement
ALTER TABLE scraped_element ADD COLUMN screen_hash TEXT;

-- Index for fast screen hash lookups
CREATE INDEX IF NOT EXISTS idx_scraped_element_screen_hash
ON scraped_element(package_name, screen_hash);

-- New query: Check if screen captured
countByScreenHash:
SELECT COUNT(*)
FROM scraped_element
WHERE package_name = ? AND screen_hash = ?;

-- New query: Get elements by screen hash
getByScreenHash:
SELECT *
FROM scraped_element
WHERE package_name = ? AND screen_hash = ?;
```

#### 4. LearnApp Integration (Phase 2)
**File**: `ExplorationEngine.kt`

**Changes** (for Phase 2 - not in this spec):
```kotlin
// Check JIT cache before capture
val jitCachedElements = repository.getElementsForScreen(packageName, screenHash)
if (jitCachedElements.isNotEmpty()) {
    // Use JIT-captured elements with UUIDs (skip capture)
    Log.i(TAG, "Using ${jitCachedElements.size} JIT-cached elements")
}
```

---

## User Stories

### US-001: Battery-Efficient Passive Learning
**As a** VoiceOS user
**I want** JIT to skip re-capturing screens I've already visited
**So that** my phone battery lasts longer during natural app usage

**Acceptance Criteria:**
- [ ] User navigates to same screen 10 times during day
- [ ] JIT captures elements on first visit only
- [ ] Subsequent 9 visits: Skip capture (7ms vs 50ms)
- [ ] Battery usage for JIT: <1% per day (vs 3% before)

### US-002: Quick Learning Completion
**As a** VoiceOS user
**I want** LearnApp to complete learning in 5-10 minutes instead of 60 minutes
**So that** I can start using voice commands faster

**Acceptance Criteria:**
- [ ] User uses Photos app naturally for 1 week (JIT passive learning)
- [ ] JIT captures 80% of screens during usage
- [ ] User clicks "Complete Learning" button
- [ ] LearnApp uses JIT cache → Completes in 8 minutes (vs 60 minutes)

### US-003: Popup Voice Control
**As a** VoiceOS user
**I want** voice commands for dialog buttons ("click OK", "click Cancel")
**So that** I can control confirmation popups hands-free

**Acceptance Criteria:**
- [ ] JIT encounters delete confirmation dialog
- [ ] JIT captures dialog buttons with UUIDs
- [ ] Voice commands generated: "click OK", "click Cancel"
- [ ] Dialog appears again → JIT skips re-capture (already cached)
- [ ] User says "click OK" → Dialog button clicked

---

## Technical Constraints

### Constraint 1: Accessibility Service Dependency
JIT requires AccessibilityService for rootNode access → Must be enabled by user.

**Mitigation**: Already handled, AccessibilityService is core VoiceOS requirement.

### Constraint 2: SQLDelight KMP Limitations
SQLDelight doesn't support ALTER TABLE in migrations for Android.

**Mitigation**: Add column at runtime with default NULL, create index separately.

### Constraint 3: ThirdPartyGenerator Availability
UUID generation requires `ThirdPartyGenerator` instance in JIT context.

**Mitigation**: Pass via dependency injection in `JitElementCapture` constructor.

### Constraint 4: Screen Hash Stability
Screen hash must be stable across sessions for same screen.

**Mitigation**: Use ScreenStateManager's existing fingerprinting (already stable).

---

## Dependencies

### Internal Dependencies
| Component | Dependency | Reason |
|-----------|-----------|--------|
| JustInTimeLearner | ScreenStateManager | Screen hash calculation |
| JitElementCapture | ThirdPartyGenerator | UUID generation |
| JitElementCapture | VoiceOSDatabaseManager | Deduplication check |
| Database | SQLDelight schema migration | Add screen_hash column |

### External Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| SQLDelight | 2.0.+ | Database queries |
| UUIDCreator | Internal | UUID generation |
| AccessibilityService | Android API 21+ | Tree access |

### Cross-Module Dependencies
- **None** - All changes contained within LearnApp module

---

## Testing Strategy

### Unit Tests
**File**: `JitElementCaptureTest.kt`

```kotlin
@Test
fun `screen hash deduplication skips recapture`() {
    // First capture
    val elements1 = jitCapture.captureScreenElements(packageName, screenHash)
    assert(elements1.isNotEmpty())

    // Second capture (same hash)
    val elements2 = jitCapture.captureScreenElements(packageName, screenHash)
    assert(elements2.isEmpty()) // Skipped due to deduplication
}

@Test
fun `UUID generation produces stable UUIDs`() {
    val element = createTestElement()
    val uuid1 = jitCapture.generateUUID(element, packageName)
    val uuid2 = jitCapture.generateUUID(element, packageName)
    assert(uuid1 == uuid2) // Stable UUID for same element
}

@Test
fun `popup hash consistency with LearnApp`() {
    val rootNode = createPopupNode()
    val jitHash = jit.calculateScreenHash(packageName)
    val learnAppHash = screenStateManager.captureScreenState(rootNode, packageName, 0).hash
    assert(jitHash == learnAppHash) // Same algorithm
}
```

### Integration Tests
**File**: `JustInTimeLearnerIntegrationTest.kt`

```kotlin
@Test
fun `full flow - screen revisit deduplication`() {
    // Visit screen 1
    jit.onScreenChange(event1)
    verify { database.scrapedElements.insert(any()) } // Elements captured

    // Revisit screen 1 (same hash)
    jit.onScreenChange(event1)
    verify(exactly = 1) { database.scrapedElements.insert(any()) } // NOT captured again
}

@Test
fun `popup appears multiple times - single capture`() {
    // First popup appearance
    jit.onPopupAppear("Delete Photo1.jpg?")
    val elementsInDB = database.scrapedElements.count()

    // Second popup appearance (different content, same type)
    jit.onPopupAppear("Delete Photo2.jpg?")
    val elementsAfter = database.scrapedElements.count()

    assert(elementsInDB == elementsAfter) // No duplicate capture
}
```

### Manual Testing
**Test Plan**: `docs/testing/jit-deduplication-test-plan.md`

1. **Install VoiceOS** on test device
2. **Open Google Photos** app
3. **Navigate main screen → Albums → Back to main** (3 times)
4. **Check logs**: `adb logcat | grep "already captured, skipping"`
5. **Verify**: Only 2 screen captures (main, albums), not 6
6. **Trigger delete dialog** (3 times with different photos)
7. **Verify**: Only 1 dialog capture, not 3
8. **Check database**: `SELECT DISTINCT screen_hash FROM scraped_element`
9. **Verify**: 3 unique hashes (main, albums, delete_dialog)

---

## Success Criteria

### Measurable Goals
- [ ] **Performance**: Screen revisit check <7ms (86% faster than 50ms capture)
- [ ] **Battery**: CPU usage reduced by 86% on revisited screens
- [ ] **Deduplication**: 0 duplicate captures for same screen hash
- [ ] **Hash Consistency**: JIT hash == LearnApp hash for 100% of screens
- [ ] **UUID Generation**: 100% of JIT elements have UUIDs
- [ ] **Popup Support**: Same popup type → Same hash (content-independent)
- [ ] **Database**: screen_hash column added, index created
- [ ] **Tests**: 90%+ coverage for new code paths

### Quality Gates
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual testing confirms deduplication
- [ ] No performance regression on first-visit capture
- [ ] Documentation updated (developer manual, architecture diagrams)

---

## Documentation Requirements

### Developer Manual Updates
**File**: `docs/modules/LearnApp/developer-manual.md`

**New Sections:**
1. **JIT Screen Hash Deduplication Architecture**
   - Hash algorithm consistency (JIT vs LearnApp)
   - Deduplication flow diagram
   - Popup handling strategy
   - Performance metrics

2. **UUID Generation in JIT**
   - UUID generation algorithm
   - Consistency with LearnApp
   - Database storage model

3. **Troubleshooting: Hash Mismatches**
   - Diagnosing hash inconsistencies
   - Verifying deduplication is working
   - Performance monitoring

### Architecture Diagrams
**File**: `docs/modules/LearnApp/architecture-diagrams.md`

**Diagrams to Create:**
1. **JIT Enhanced Flow** (with deduplication)
2. **Screen Hash Consistency** (JIT ↔ LearnApp)
3. **Popup Handling Flow**
4. **Database Schema** (with screen_hash column)
5. **Phase 2 Integration** (JIT cache → LearnApp)

---

## Implementation Plan Reference

This specification enables:
- **Immediate**: JIT deduplication and UUID generation (this spec)
- **Phase 2**: LearnApp cache integration (Spec 008 - two-phase optimization)
- **Future**: AI context enhancement (navigation graph + element semantics)

**Next Steps:**
1. `/plan` - Create detailed implementation plan
2. `/implement .yolo .swarm` - Execute implementation
3. Update developer manual with new architecture
4. Create architecture flow diagrams

---

## Approval

**Status**: ✅ Approved for Implementation

**Approver**: User Requirements Analysis
**Date**: 2025-12-02

**Implementation Mode**: YOLO + Swarm (multi-component changes)

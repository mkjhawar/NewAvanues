# VoiceOSCoreNG Comprehensive Fix Plan

**Date:** 2026-01-08
**Version:** 1.0
**Status:** Ready for Implementation
**Priority:** Critical

---

## Executive Summary

This plan addresses 52 identified architecture gaps to achieve production readiness. The fixes are organized into 5 phases with the most critical blocking issues resolved first.

---

## Phase 1: Critical Blocking Issues (10 items)

### Issue 1.1: FOREIGN KEY Constraint Violation
**Priority:** P0 | **Effort:** 4 hours | **Files:** 4

**Problem:**
- `commands_generated` insert fails because:
  1. `scraped_app` not populated first
  2. `scraped_element` not populated first
  3. `appId` is empty string (not in scraped_app)
  4. `elementHash` doesn't exist in scraped_element

**Fix:**

**File 1:** `CommandGenerator.kt` - Add metadata to QuantizedCommand
```kotlin
// BEFORE (line 45-51):
return QuantizedCommand(
    uuid = "",
    phrase = "$verb $label",
    actionType = actionType,
    targetVuid = vuid,
    confidence = calculateConfidence(element)
)

// AFTER:
return QuantizedCommand(
    uuid = "",
    phrase = "$verb $label",
    actionType = actionType,
    targetVuid = vuid,
    confidence = calculateConfidence(element),
    metadata = mapOf(
        "packageName" to packageName,
        "elementHash" to elementHash,  // Pass the hash used in VUID
        "createdAt" to System.currentTimeMillis().toString()
    )
)
```

**File 2:** `VoiceOSAccessibilityService.kt` - Populate scraped_app and scraped_element BEFORE commands
```kotlin
// In exploreNode() or generateCommands(), BEFORE commandPersistence.insertBatch():

// Step 1: Ensure scraped_app exists
val appDTO = ScrapedAppDTO(
    appId = packageName,
    packageName = packageName,
    versionCode = getAppVersionCode(packageName),
    versionName = getAppVersionName(packageName),
    appHash = HashUtils.generateHash(packageName, 8),
    isFullyLearned = 0,
    learnCompletedAt = null,
    scrapingMode = "DYNAMIC",
    scrapeCount = 1,
    elementCount = elements.size.toLong(),
    commandCount = quantizedCommands.size.toLong(),
    firstScrapedAt = System.currentTimeMillis(),
    lastScrapedAt = System.currentTimeMillis()
)
scrapedAppRepository.insertOrUpdate(appDTO)

// Step 2: Insert scraped_elements
val elementDTOs = elements.map { element ->
    ScrapedElementDTO(
        elementHash = HashUtils.generateHash(element.uniqueId, 8),
        appId = packageName,
        className = element.className,
        text = element.text,
        contentDescription = element.contentDescription,
        // ... other fields
    )
}
scrapedElementRepository.insertBatch(elementDTOs)

// Step 3: NOW insert commands (with valid FK references)
commandPersistence.insertBatch(quantizedCommands)
```

**File 3:** `AndroidCommandPersistence.kt` - Use elementHash from metadata
```kotlin
// BEFORE (line 89-91):
elementHash = targetVuid ?: uuid,
appId = metadata["packageName"] ?: "",

// AFTER:
elementHash = metadata["elementHash"] ?: targetVuid ?: uuid,
appId = metadata["packageName"] ?: throw IllegalStateException("packageName required"),
```

**File 4:** Add repository dependencies to VoiceOSAccessibilityService
```kotlin
// In VoiceOSAccessibilityService class:
private val scrapedAppRepository: IScrapedAppRepository by lazy {
    databaseManager.scrapedApps
}
private val scrapedElementRepository: IScrapedElementRepository by lazy {
    databaseManager.scrapedElements
}
```

---

### Issue 1.2: Database Persistence Not Initialized
**Priority:** P0 | **Effort:** 2 hours | **Files:** 2

**Problem:** `AndroidCommandPersistence` never instantiated in VoiceOSAccessibilityService

**Fix:**

**File:** `VoiceOSAccessibilityService.kt`
```kotlin
// Add to class properties (around line 46):
private val databaseManager: VoiceOSDatabaseManager by lazy {
    val driverFactory = AndroidDatabaseDriverFactory(applicationContext)
    VoiceOSDatabaseManager.getInstance(driverFactory)
}

private val commandPersistence: ICommandPersistence by lazy {
    AndroidCommandPersistence(databaseManager.generatedCommands)
}
```

---

### Issue 1.3: Voice Engine Not Initialized
**Priority:** P0 | **Effort:** 2 hours | **Files:** 2

**Problem:** Two `VoiceOSCoreNG` classes causing confusion; voice engine never starts

**Fix:**

**Option A:** Rename to avoid confusion
```kotlin
// Rename handlers/VoiceOSCoreNG.kt → VoiceOSCoreNGConfig.kt
object VoiceOSCoreNGConfig { ... }

// Rename common/VoiceOSCoreNG.kt → VoiceOSCoreNGBuilder.kt
class VoiceOSCoreNGBuilder { ... }
```

**Option B:** Use correct class in initialization
```kotlin
// In VoiceOSAccessibilityService.onCreate():
import com.augmentalis.voiceoscoreng.common.VoiceOSCoreNG as VoiceOSBuilder

val voiceOS = VoiceOSBuilder.Builder()
    .setEngineType(EngineType.VOSK)
    .setMode(SpeechMode.COMBINED_COMMAND)
    .build()

voiceOS.initialize()  // Actually starts the speech engine
```

---

### Issue 1.4: Screen Change Trigger Missing
**Priority:** P0 | **Effort:** 3 hours | **Files:** 2

**Problem:** Element cache never invalidated; commands not regenerated on screen change

**Fix:**

**File:** `VoiceOSAccessibilityService.kt`
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
            // Invalidate element cache
            actionExecutor.clearCache()

            // Regenerate commands for new screen
            val rootNode = rootInActiveWindow ?: return
            serviceScope.launch {
                exploreNode(rootNode)
            }
        }
    }
}
```

**File:** `AndroidActionExecutor.kt` - Add cache TTL
```kotlin
// Add TTL to cache
private data class CachedElement(
    val node: AccessibilityNodeInfo,
    val timestamp: Long
)

private val elementCache = mutableMapOf<String, CachedElement>()
private val CACHE_TTL_MS = 5000L  // 5 seconds

private fun findNodeByVuid(vuid: String): AccessibilityNodeInfo? {
    // Check cache with TTL
    elementCache[vuid]?.let { cached ->
        if (System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.node
        }
        elementCache.remove(vuid)
    }
    // ... rest of lookup
}
```

---

### Issue 1.5-1.10: Platform Stubs, NLU, Thread Safety, Error Handling, Tests, Docs
**Priority:** P1-P2 | **Effort:** See detailed breakdown below

| Issue | Priority | Effort | Summary |
|-------|----------|--------|---------|
| 1.5 Platform Stubs | P1 | 40h | Implement iOS UIAccessibility, Desktop AWT Robot |
| 1.6 NLU Integration | P1 | 16h | Connect Common/NLU module |
| 1.7 Thread Safety | P0 | 4h | Add Mutex to caches and registries |
| 1.8 Error Handling | P1 | 8h | Add depth limits, retry logic, logging |
| 1.9 Test Coverage | P1 | 24h | Fix 980 test errors, add integration tests |
| 1.10 Documentation | P2 | 16h | Add ADRs, API reference, troubleshooting |

---

## Phase 2: SOLID Principle Fixes (8 items)

### Issue 2.1: Single Responsibility (VoiceOSCoreNG object)
**Effort:** 4 hours

**Current:** Single object with 20+ responsibilities
**Fix:** Split into:
- `LearnAppConfigManager` - tier and limit management
- `FeatureFlagManager` - feature toggles
- `TestModeController` - test mode settings

### Issue 2.2: Interface Segregation (IActionExecutor)
**Effort:** 4 hours

**Current:** 40+ methods in single interface
**Fix:** Split into:
```kotlin
interface ITapExecutor { suspend fun tap(vuid: String): ActionResult }
interface IScrollExecutor { suspend fun scroll(...): ActionResult }
interface ISystemExecutor { suspend fun back(): ActionResult; ... }
interface IMediaExecutor { suspend fun mediaPlayPause(): ActionResult; ... }
interface IAppExecutor { suspend fun openApp(...): ActionResult; ... }

interface IActionExecutor : ITapExecutor, IScrollExecutor, ISystemExecutor, IMediaExecutor, IAppExecutor
```

### Issue 2.3-2.8: OCP, LSP, DIP, DRY violations
**Total Effort:** 16 hours

See detailed refactoring guide in implementation phase.

---

## Phase 3: Missing Features (12 items)

| Feature | Effort | Implementation |
|---------|--------|----------------|
| Dependency Injection | 8h | Add Koin modules |
| Service Lifecycle | 4h | Add lifecycle callbacks |
| Config Persistence | 4h | SharedPreferences backing |
| Logging/Observability | 4h | Integrate Timber |
| Metrics Collection | 4h | Add analytics pipeline |
| Retry/Circuit Breaker | 8h | Add resilience4j or custom |
| Accessibility Service Template | 4h | Add template class |
| Web Platform | 40h | Add jsMain implementation |
| Element Cache TTL | 2h | Time-based eviction |
| VUID Validation | 2h | Format validation |
| Dangerous Element Check | 2h | Safety gate before execution |
| NLU Package Integration | 8h | Connect Common/NLU |

---

## Phase 4: Performance Fixes (7 items)

| Fix | Effort | Implementation |
|-----|--------|----------------|
| Bounded Recursion | 2h | Add max depth parameter |
| Accessibility Tree Cache | 8h | TreeCache snapshot |
| Async Persistence | 2h | Non-blocking with flow |
| Batch Limits | 2h | Enforce element limits |
| Engine Pooling | 4h | Speech engine lifecycle |
| Command Compression | 4h | gzip serialization |
| Memory Leak Fix | 2h | node.recycle() calls |

---

## Phase 5: Testing & Documentation (11 items)

### Testing (5 items)
- Fix 980 test compilation errors
- Add persistence integration tests
- Add E2E initialization tests
- Add accessibility tree traversal tests
- Add network/connectivity tests

### Documentation (6 items)
- ARCHITECTURE.md with diagrams
- ADR documents for key decisions
- API-REFERENCE.md
- MIGRATION-GUIDE.md
- TROUBLESHOOTING.md
- Platform implementation guide

---

## Implementation Priority

### Immediate (Today)
1. ✅ Fix FK constraint (Issue 1.1) - 4 hours
2. ✅ Initialize database persistence (Issue 1.2) - 2 hours
3. ✅ Fix voice engine initialization (Issue 1.3) - 2 hours
4. ✅ Add screen change trigger (Issue 1.4) - 3 hours

### This Week
5. Add thread safety (Issue 1.7) - 4 hours
6. Add error handling (Issue 1.8) - 8 hours
7. Split interfaces (Issue 2.2) - 4 hours

### Next Week
8. Fix test compilation (Issue 1.9) - 24 hours
9. Add DI framework (Phase 3) - 8 hours
10. Performance fixes (Phase 4) - 24 hours

---

## Success Criteria

| Metric | Before | Target |
|--------|--------|--------|
| Architecture Score | 3-4/10 | 10/10 |
| Test Pass Rate | ~50% | 100% |
| FK Violations | Crash | 0 |
| Commands Persisted | 0 | All |
| Platform Support | Android only | Android + iOS + Desktop |

---

## Files to Modify

### Critical Path (Phase 1.1)
1. `Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/CommandGenerator.kt`
2. `android/apps/voiceoscoreng/src/main/kotlin/.../service/VoiceOSAccessibilityService.kt`
3. `Modules/VoiceOSCoreNG/src/androidMain/kotlin/.../persistence/AndroidCommandPersistence.kt`
4. `Common/VoiceOS/database/src/commonMain/kotlin/.../repositories/impl/SQLDelightScrapedAppRepository.kt`
5. `Common/VoiceOS/database/src/commonMain/kotlin/.../repositories/impl/SQLDelightScrapedElementRepository.kt`

### Database Schema (verify no changes needed)
- `Common/VoiceOS/database/src/commonMain/sqldelight/.../ScrapedApp.sq`
- `Common/VoiceOS/database/src/commonMain/sqldelight/.../ScrapedElement.sq`
- `Common/Database/src/commonMain/sqldelight/.../GeneratedCommand.sq`

---

**Estimated Total Effort:** 150-180 developer-hours
**Critical Path Effort:** 11 hours (Issues 1.1-1.4)

---

*Plan created by VoiceOSCoreNG Analysis Swarm*

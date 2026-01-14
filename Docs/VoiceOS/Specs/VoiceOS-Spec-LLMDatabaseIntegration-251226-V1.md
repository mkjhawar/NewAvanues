# VoiceOS LLM Database Integration Specification

**Document ID**: VoiceOS-Spec-LLMDatabaseIntegration-251226-V1
**Status**: Draft
**Platform**: Android (Kotlin)
**Author**: IDEACODE /i.spec
**Created**: 2025-12-26

---

## 1. Executive Summary

Complete the `AVUQuantizerIntegration` placeholder implementations to enable real LLM prompt generation from learned app data stored in SQLDelight database. This bridges the gap between LearnApp exploration and LLM-powered voice command understanding.

---

## 2. Problem Statement

### Current State
- `AVUQuantizerIntegration.kt` has 4 placeholder methods returning empty data
- LLM prompts are generated with no real app context
- Cannot detect which apps have been learned
- Action prediction fails due to missing screen/element data

### Desired State
- Quantization layer queries real data from SQLDelight repositories
- LLM prompts contain actual learned screens, elements, and navigation
- Apps with learned data are correctly identified
- Action prediction works with real context

### Impact
- **User Impact**: Voice commands fail for learned apps
- **System Impact**: LLM integration is non-functional
- **Business Impact**: Core LearnApp value proposition blocked

---

## 3. Functional Requirements

### FR-1: Query Learned Packages
**Priority**: P0 (Critical)
**Method**: `getLearnedPackages(): List<String>`

| Requirement | Description |
|-------------|-------------|
| FR-1.1 | Query `LearnedApp` table for all packages with `status = COMPLETED` |
| FR-1.2 | Return distinct package names |
| FR-1.3 | Cache results with 60-second TTL |
| FR-1.4 | Handle empty database gracefully |

**Acceptance Criteria**:
- [ ] Returns list of package names from LearnedApp table
- [ ] Only includes apps with completed exploration
- [ ] Returns empty list (not null) when no apps learned
- [ ] Performance: < 50ms for up to 100 apps

### FR-2: Check Learned Data Exists
**Priority**: P0 (Critical)
**Method**: `hasLearnedData(packageName: String): Boolean`

| Requirement | Description |
|-------------|-------------|
| FR-2.1 | Query `LearnedApp` table for specific package |
| FR-2.2 | Check `status = COMPLETED` for positive result |
| FR-2.3 | Return false for unknown/in-progress packages |
| FR-2.4 | Use cached data when available |

**Acceptance Criteria**:
- [ ] Returns true for completed learned apps
- [ ] Returns false for unknown packages
- [ ] Returns false for in-progress exploration
- [ ] Performance: < 20ms per check

### FR-3: Build Quantized Screens
**Priority**: P0 (Critical)
**Method**: `buildQuantizedScreens(packageName: String): List<QuantizedScreen>`

| Requirement | Description |
|-------------|-------------|
| FR-3.1 | Query `ScreenContext` table for package screens |
| FR-3.2 | Query `ScrapedElement` table for screen elements |
| FR-3.3 | Filter to actionable elements only (isClickable, isEditable) |
| FR-3.4 | Convert to `QuantizedScreen` with `QuantizedElement` list |
| FR-3.5 | Include screen hash, title, activity name |
| FR-3.6 | Classify element types (BUTTON, TEXT_FIELD, etc.) |

**Acceptance Criteria**:
- [ ] Returns all screens for the package
- [ ] Each screen includes its actionable elements
- [ ] Element types correctly classified from className
- [ ] Handles screens with 0 elements gracefully
- [ ] Performance: < 200ms for app with 50 screens

### FR-4: Build Quantized Navigation
**Priority**: P1 (High)
**Method**: `buildQuantizedNavigation(packageName: String, screens: List<QuantizedScreen>): List<QuantizedNavigation>`

| Requirement | Description |
|-------------|-------------|
| FR-4.1 | Query `ScreenTransition` table for navigation edges |
| FR-4.2 | Map transitions to `QuantizedNavigation` objects |
| FR-4.3 | Include trigger element label and VUID |
| FR-4.4 | Only include transitions between known screens |

**Acceptance Criteria**:
- [ ] Returns navigation edges between screens
- [ ] Each edge includes trigger element info
- [ ] Orphan transitions (to unknown screens) excluded
- [ ] Performance: < 100ms for app with 100 transitions

### FR-5: Build Known Commands
**Priority**: P1 (High)
**Method**: `buildKnownCommands(packageName: String): List<QuantizedCommand>`

| Requirement | Description |
|-------------|-------------|
| FR-5.1 | Query `GeneratedCommand` table for package |
| FR-5.2 | Include phrase, action type, target element |
| FR-5.3 | Filter to active/validated commands only |
| FR-5.4 | Sort by usage frequency (most used first) |

**Acceptance Criteria**:
- [ ] Returns generated commands for package
- [ ] Commands sorted by usage frequency
- [ ] Inactive commands excluded
- [ ] Performance: < 50ms for 500 commands

### FR-6: Cache Invalidation
**Priority**: P1 (High)
**Method**: Automatic cache invalidation

| Requirement | Description |
|-------------|-------------|
| FR-6.1 | Invalidate package cache when exploration completes |
| FR-6.2 | Invalidate on manual learning trigger |
| FR-6.3 | Invalidate all caches on database clear |
| FR-6.4 | Provide manual invalidation API |

**Acceptance Criteria**:
- [ ] Cache invalidated after exploration completion
- [ ] `invalidateCache(packageName)` clears specific package
- [ ] `clearCache()` clears all packages
- [ ] New queries reflect updated data

---

## 4. Non-Functional Requirements

### NFR-1: Performance

| Metric | Target |
|--------|--------|
| getLearnedPackages() | < 50ms |
| hasLearnedData() | < 20ms |
| buildQuantizedScreens() | < 200ms |
| getQuantizedContext() (full) | < 500ms |
| Cache hit | < 5ms |

### NFR-2: Memory

| Metric | Target |
|--------|--------|
| Cache per package | < 500KB |
| Total cache | < 10MB |
| Peak during generation | < 20MB |

### NFR-3: Reliability

| Metric | Target |
|--------|--------|
| Query success rate | 99.9% |
| Graceful degradation | Empty data on error |
| No crashes | Database errors caught |

### NFR-4: Maintainability

| Metric | Target |
|--------|--------|
| Test coverage | 90%+ |
| Documentation | All public methods |
| Logging | Debug-level queries |

---

## 5. Technical Design

### 5.1 Dependencies

| Dependency | Purpose |
|------------|---------|
| IScreenContextRepository | Query screen data |
| IGeneratedCommandRepository | Query commands |
| VoiceOSDatabaseManager | Database access |
| LearnAppRepository | Query learned apps |

### 5.2 Database Tables Used

| Table | Data |
|-------|------|
| LearnedApp | Package status |
| ScreenContext | Screen metadata |
| ScrapedElement | Element data |
| ScrapedHierarchy | Parent-child relations |
| ScreenTransition | Navigation edges |
| GeneratedCommand | Voice commands |

### 5.3 Type Mappings

**Element Type Classification**:
```kotlin
className → ElementType:
  "Button", "ImageButton" → BUTTON
  "EditText", "TextInputEditText" → TEXT_FIELD
  "CheckBox" → CHECKBOX
  "Switch", "ToggleButton" → SWITCH
  "Spinner" → DROPDOWN
  "Tab", "TabLayout" → TAB
  * → OTHER
```

### 5.4 Cache Strategy

```
┌─────────────────────────────────────────┐
│     ConcurrentHashMap<String, CachedContext>     │
│     Key: packageName                             │
│     Value: CachedContext(context, timestamp)     │
│     TTL: 5 minutes (CACHE_EXPIRY_MS)            │
└─────────────────────────────────────────┘
```

---

## 6. API Changes

### 6.1 Constructor Change

**Before**:
```kotlin
class AVUQuantizerIntegration(private val context: Context)
```

**After**:
```kotlin
class AVUQuantizerIntegration(
    private val context: Context,
    private val screenContextRepository: IScreenContextRepository,
    private val generatedCommandRepository: IGeneratedCommandRepository,
    private val databaseManager: VoiceOSDatabaseManager
)
```

### 6.2 New Internal Methods

```kotlin
private suspend fun queryScreensFromDatabase(packageName: String): List<ScreenContextEntity>
private suspend fun queryElementsForScreen(screenHash: String): List<ScrapedElementEntity>
private suspend fun queryNavigationEdges(packageName: String): List<ScreenTransitionEntity>
private fun classifyElementType(className: String): ElementType
private fun convertToQuantizedElement(entity: ScrapedElementEntity): QuantizedElement
```

---

## 7. Acceptance Criteria

### Integration Tests

| Test | Description |
|------|-------------|
| IT-1 | getQuantizedContext returns data for learned app |
| IT-2 | getQuantizedContext returns null for unknown app |
| IT-3 | generateLLMPrompt creates valid COMPACT prompt |
| IT-4 | generateLLMPrompt creates valid FULL prompt |
| IT-5 | Cache is used on second request |
| IT-6 | Cache invalidation triggers re-query |

### Unit Tests

| Test | Description |
|------|-------------|
| UT-1 | classifyElementType maps correctly |
| UT-2 | convertToQuantizedElement handles nulls |
| UT-3 | buildVocabulary extracts unique labels |
| UT-4 | Empty database returns empty lists |

---

## 8. Out of Scope

- LLM inference execution
- Cloud LLM provider integration
- LLM → Action execution bridge
- Real-time quantization during exploration
- Differential context updates

---

## 9. Dependencies

| Dependency | Status |
|------------|--------|
| SQLDelight repositories | ✅ Exist |
| Database schema | ✅ Complete |
| QuantizedContext models | ✅ Exist |
| ExplorationEngine integration | ✅ Works |

---

## 10. Risks

| Risk | Mitigation |
|------|------------|
| Large app data | Pagination, lazy loading |
| Memory pressure | Cache size limits |
| Database corruption | Graceful degradation |
| Schema changes | Version migration |

---

## 11. Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Implementation | 1 session | All FR methods |
| Testing | 1 session | Unit + integration tests |
| Integration | 1 session | Wire to DI, verify |

---

**Next Steps**: `/i.plan` to create implementation plan

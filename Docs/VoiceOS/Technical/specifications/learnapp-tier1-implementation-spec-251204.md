# LearnApp Tier 1 - 90% Coverage Implementation Specification

**Document**: Tier 1 Implementation Specification
**Date**: 2025-12-04
**Status**: APPROVED FOR IMPLEMENTATION
**Priority**: HIGH
**Target**: 90% coverage for 72% of Android apps

---

## Executive Summary

Implement comprehensive exploration engine enhancements to achieve 90%+ element coverage for Native Android Views, React Native, Jetpack Compose, and Flutter apps with semantics. This represents 72% of all Android apps and provides the highest ROI (42 hours incremental work for 60% coverage gain).

**Key Deliverables:**
1. Scrollable content exploration (RecyclerView, ListView, ScrollView)
2. Hidden UI pattern handling (drawers, menus, expandables)
3. Cross-platform framework detection and routing
4. Platform-specific optimizations

**Expected Results:**
- Microsoft Teams: 70 → 150+ elements (**+114%**)
- Instagram (React Native): 30 → 200+ elements (**+567%**)
- Gmail: 40 → 150+ elements (**+275%**)

---

## Problem Statement

### Current State

LearnApp's exploration engine captures only **30% of app elements** due to three critical gaps:

1. **Scrollable Content Invisibility** (40% gap)
   - RecyclerView/ListView children not explored
   - Only visible elements captured
   - Example: Teams app - left sidebar captured, main content list missed

2. **Hidden UI Pattern Blindness** (20% gap)
   - Navigation drawers never opened
   - Overflow menus dismissed before capturing items
   - Expandable lists never expanded
   - Bottom sheets remain collapsed

3. **Cross-Platform Framework Confusion** (10% gap)
   - Flutter/React Native apps mishandled
   - No detection of unsupported frameworks
   - Wasted exploration attempts on Unity games

### Desired State

Achieve **90% element coverage** for supported platforms:
- Native Android Views
- React Native (bridges to native Views)
- Jetpack Compose (with semantics)
- Flutter apps with accessibility semantics

---

## Functional Requirements

### FR-1: Scrollable Content Exploration

**Priority**: CRITICAL
**Complexity**: MEDIUM
**Effort**: 12 hours

**Description**: Extract and explore elements hidden inside scrollable containers.

**Acceptance Criteria:**
- ✅ Detect RecyclerView, ListView, GridView, ScrollView, NestedScrollView
- ✅ Extract clickable children from scrollable containers
- ✅ Handle nested scrollables (max depth 2)
- ✅ Implement element limits (20 per scrollable, 50 per screen)
- ✅ Recycle AccessibilityNodeInfo to prevent memory leaks
- ✅ Handle empty lists gracefully
- ✅ Teams app: Discover 80+ main content elements

**Behavior:**
```kotlin
// Before (OLD)
RecyclerView container → isClickable=false → SKIP → 0 children extracted ❌

// After (NEW)
RecyclerView container → isScrollable=true →
  Extract children → 20 clickable items found ✅
```

---

### FR-2: Hidden UI Pattern Handling

**Priority**: HIGH
**Complexity**: MEDIUM
**Effort**: 12 hours

**Description**: Systematically discover and explore hidden UI patterns.

**Acceptance Criteria:**
- ✅ Open navigation drawers and capture drawer items
- ✅ Click overflow menus and extract menu items before dismissal
- ✅ Expand expandable list items
- ✅ Expand bottom sheets
- ✅ Trigger long-click menus (context menus)
- ✅ Teams app: Discover 20+ drawer + menu items

**Supported Patterns:**

| Pattern | Detection | Action | Expected Gain |
|---------|-----------|--------|---------------|
| Navigation Drawer | `DrawerLayout` className | Open → Extract → Close | +20-30 elements |
| Overflow Menu | contentDesc="More options" | Click → Window detect → Extract | +10-15 elements |
| Expandable Items | `ACTION_EXPAND` available | Expand → Extract children | +15-30 elements |
| Bottom Sheets | `BottomSheet` className | Expand → Extract | +10-20 elements |
| Long-Click Menus | `ACTION_LONG_CLICK` | Long press → Extract menu | +5-10 elements |

---

### FR-3: Cross-Platform Framework Detection

**Priority**: HIGH
**Complexity**: LOW
**Effort**: 5 hours

**Description**: Detect UI framework and route to appropriate exploration strategy.

**Acceptance Criteria:**
- ✅ Detect Native Views, Flutter, React Native, Unity, Compose, WebView
- ✅ Check semantic availability (childCount > 0 for Flutter/Compose)
- ✅ Warn user when framework unsupported
- ✅ Store framework metadata in database
- ✅ Skip exploration for Unity/Unreal games (0% success rate)

**Framework Support Matrix:**

| Framework | Detection Method | Tier 1 Support | User Warning |
|-----------|-----------------|----------------|--------------|
| Native Views | Default | ✅ Full | None |
| React Native | `ReactRootView` | ✅ Full | None |
| Jetpack Compose | `AndroidComposeView` + childCount > 0 | ✅ Full | None if has semantics, warn otherwise |
| Flutter | `FlutterView` + childCount > 0 | ✅ If has semantics | Warn if no semantics |
| WebView | `WebView` | ✅ Separate engine | None |
| Unity/Unreal | `UnityPlayer` or `SurfaceView` + childCount=0 | ❌ Skip | "Game not supported (Tier 3)" |

**User Warnings:**
```kotlin
// Flutter without semantics
"This Flutter app doesn't support accessibility.
 Only basic exploration possible.
 Please contact the developer to add accessibility support."

// Unity game
"This is a game (Unity). Visual UI exploration not yet supported.
 Game UI elements cannot be detected automatically."
```

---

### FR-4: Native View Enhancements

**Priority**: MEDIUM
**Complexity**: MEDIUM
**Effort**: 11 hours

**Description**: Platform-specific optimizations for Native Android Views.

**Acceptance Criteria:**
- ✅ Detect custom canvas views (Views with manual drawing)
- ✅ Recover accessibility-disabled views (importantForAccessibility=no)
- ✅ Wait for delayed/dynamic content (5-second timeout)
- ✅ Handle accessibility node staleness (15ms JIT refresh)

**Target Coverage**: 90% → 95% (+5%)

---

### FR-5: React Native Enhancements

**Priority**: MEDIUM
**Complexity**: MEDIUM
**Effort**: 15 hours

**Description**: Handle React Native apps with missing accessibility labels.

**Acceptance Criteria:**
- ✅ Infer element labels from sibling text elements
- ✅ Detect nested touchables (TouchableOpacity inside TouchableOpacity)
- ✅ Handle React Native Gesture Handler library
- ✅ Generate visual UUIDs for label-free elements

**Target Coverage**: 75% → 90% (+15%)

**Label Inference Strategy:**
```kotlin
// Element without label:
TouchableOpacity (no accessibilityLabel) ❌

// Nearby sibling:
Text("Submit")

// Inferred label:
"Button: Submit" ✅
```

---

### FR-6: Jetpack Compose Enhancements

**Priority**: MEDIUM
**Complexity**: LOW
**Effort**: 10 hours

**Description**: Force semantic extraction from Compose apps.

**Acceptance Criteria:**
- ✅ Extract elements even without contentDescription
- ✅ Handle LazyColumn items with merged semantics
- ✅ Infer labels from child Text composables
- ✅ Support Box + clickable without explicit semantics

**Target Coverage**: 90% → 95% (+5%)

---

### FR-7: Flutter (with Semantics) Enhancements

**Priority**: LOW
**Complexity**: LOW
**Effort**: 6 hours

**Description**: Optimize exploration of Flutter apps that have semantics.

**Acceptance Criteria:**
- ✅ Extract elements from Flutter semantic tree
- ✅ Infer labels from parent/sibling context
- ✅ Handle partial semantic implementation

**Target Coverage**: 85% → 90% (+5%)

---

## Non-Functional Requirements

### NFR-1: Performance

| Metric | Current | Target | Max Acceptable |
|--------|---------|--------|----------------|
| Screen exploration time | 10-20s | 60s | 90s |
| Element extraction | 50ms | 100ms | 200ms |
| Memory per screen | 50KB | 150KB | 300KB |
| Framework detection | N/A | 10ms | 20ms |

### NFR-2: Memory Safety

**Requirement**: Zero memory leaks from AccessibilityNodeInfo
**Implementation**: Mandatory `child.recycle()` in try-finally blocks
**Verification**: Android Profiler leak detection + unit tests

### NFR-3: Reliability

| Scenario | Target | Mitigation |
|----------|--------|------------|
| App crash during exploration | <1% | Try-catch all node operations |
| Stuck in infinite loop | 0% | Max iteration limits (10 scrolls, 50 elements) |
| Out of memory | 0% | Element limits + node recycling |
| Node staleness | <5% | JIT 15ms refresh before click |

### NFR-4: Compatibility

**Minimum SDK**: API 21 (Android 5.0)
**Target SDK**: API 34 (Android 14)
**Tested APIs**: 21, 23, 26, 29, 31, 33, 34

---

## Technical Architecture

### Enhanced Element Classification

```kotlin
enum class ExplorationBehavior {
    CLICKABLE,          // Priority 1: Direct click
    MENU_TRIGGER,       // Priority 1: Opens menu/overlay
    TAB,                // Priority 1: Switches content

    DRAWER,             // Priority 2: Open drawer
    DROPDOWN,           // Priority 2: Expand dropdown
    BOTTOM_SHEET,       // Priority 2: Expand sheet

    SCROLLABLE,         // Priority 3: Scroll to reveal
    CHIP_GROUP,         // Priority 3: Horizontal scroll
    COLLAPSING_TOOLBAR, // Priority 3: Scroll up

    EXPANDABLE,         // Priority 4: Expand item
    LONG_CLICKABLE,     // Priority 5: Long press
    CONTAINER,          // Priority 6: Explore children
    SKIP                // Priority 7: Ignore
}
```

**Classification Logic:**
```kotlin
private fun classifyElement(element: ElementInfo): ExplorationBehavior {
    return when {
        // PRIORITY 1: Clickable (highest)
        element.isClickable && element.isEnabled -> {
            when {
                isOverflowMenu(element) -> MENU_TRIGGER
                isTabElement(element) -> TAB
                else -> CLICKABLE
            }
        }

        // PRIORITY 2: Interactive reveals
        isDrawerLayout(element) -> DRAWER
        isSpinner(element) -> DROPDOWN
        isBottomSheet(element) -> BOTTOM_SHEET

        // PRIORITY 3: Scrollables
        element.isScrollable && isScrollableContainer(element.className) -> {
            when {
                isChipGroup(element) -> CHIP_GROUP
                isCollapsingToolbar(element) -> COLLAPSING_TOOLBAR
                else -> SCROLLABLE
            }
        }

        // PRIORITY 4-7: Lower priority
        hasExpandAction(element) -> EXPANDABLE
        hasLongClickAction(element) -> LONG_CLICKABLE
        element.node?.childCount ?: 0 > 0 -> CONTAINER
        else -> SKIP
    }
}
```

### Framework Detection System

```kotlin
class FrameworkDetector {
    fun detectFramework(rootNode: AccessibilityNodeInfo): FrameworkInfo {
        val className = rootNode.className?.toString() ?: ""

        val framework = when {
            className.contains("FlutterView") -> UIFramework.FLUTTER
            className.contains("ReactRootView") -> UIFramework.REACT_NATIVE
            className.contains("AndroidComposeView") -> UIFramework.COMPOSE
            className.contains("UnityPlayer") -> UIFramework.UNITY
            className.contains("SurfaceView") && rootNode.childCount == 0 -> UIFramework.UNITY
            className.contains("WebView") -> UIFramework.WEBVIEW
            else -> UIFramework.NATIVE_VIEWS
        }

        val hasSemantics = when (framework) {
            UIFramework.FLUTTER, UIFramework.COMPOSE, UIFramework.WEBVIEW ->
                rootNode.childCount > 0
            UIFramework.UNITY, UIFramework.UNREAL -> false
            else -> true
        }

        return FrameworkInfo(framework, hasSemantics)
    }
}
```

### Memory-Safe Child Extraction

```kotlin
private fun extractClickableChildren(
    node: AccessibilityNodeInfo,
    scrollableDepth: Int = 0
): List<ElementInfo> {
    val children = mutableListOf<ElementInfo>()
    val startTime = System.currentTimeMillis()

    if (node.childCount == 0) return emptyList()

    fun traverse(n: AccessibilityNodeInfo, depth: Int) {
        // Timeout protection
        if (System.currentTimeMillis() - startTime > 5000) {
            Log.w(TAG, "Child extraction timeout")
            return
        }

        for (i in 0 until n.childCount) {
            val child = n.getChild(i) ?: continue

            try {
                // Handle nested scrollables
                if (isScrollableContainer(child.className?.toString() ?: "")) {
                    if (scrollableDepth < MAX_SCROLLABLE_DEPTH) {
                        val nested = extractClickableChildren(child, scrollableDepth + 1)
                        children.addAll(nested)
                    }
                } else if (child.isClickable && child.isEnabled) {
                    children.add(createElementInfo(child))
                } else {
                    traverse(child, depth + 1)
                }
            } finally {
                // CRITICAL: Always recycle
                child.recycle()
            }
        }
    }

    traverse(node, 0)
    return children.take(MAX_ELEMENTS_PER_SCROLLABLE)
}
```

---

## Implementation Phases

### Phase 1: Scrollable Content (12 hours)

**Sub-phases:**
1. Element classification (2h)
2. Child extraction (3h)
3. Scroll support (4h)
4. Testing (3h)

**Deliverables:**
- `ElementInfo.kt` - Add ExplorationBehavior enum
- `ScreenExplorer.kt` - Add classifyElement(), extractClickableChildren()
- `ScrollExecutor.kt` - Scroll integration
- Tests: ElementClassificationTest, ChildExtractionTest, ScrollIntegrationTest

---

### Phase 2: Hidden UI Patterns (12 hours)

**Sub-phases:**
1. Menu trigger handling (3h)
2. Drawer handling (3h)
3. Expandable items (2h)
4. Bottom sheets & long-click (2h)
5. Testing (2h)

**Deliverables:**
- `HiddenUIHandler.kt` - Drawer/menu/expandable logic
- `WindowTracker.kt` - Track menu window appearances
- `ExplorationEngine.kt` - Integrate handlers
- Tests: HiddenUIHandlerTest, WindowTrackerTest

---

### Phase 3: Framework Detection (5 hours)

**Sub-phases:**
1. Framework detector (2h)
2. Database schema (1h)
3. User warnings (1h)
4. Testing (1h)

**Deliverables:**
- `FrameworkDetector.kt` - Detection logic
- `FrameworkInfo.kt` - Data class
- Database migration: app_framework_info table
- Tests: FrameworkDetectionTest

---

### Phase 4: Native View Enhancements (11 hours)

**Sub-phases:**
1. Custom canvas detection (3h)
2. Accessibility override (2h)
3. Delayed content waiting (3h)
4. Testing (3h)

**Deliverables:**
- `CustomViewDetector.kt` - Canvas view detection
- `AccessibilityOverride.kt` - Force accessibility
- `DynamicContentWaiter.kt` - Delayed content
- Tests: CustomViewDetectionTest, DynamicContentTest

---

### Phase 5: React Native Enhancements (15 hours)

**Sub-phases:**
1. Label inference (5h)
2. Nested touchables (4h)
3. Gesture handlers (3h)
4. Testing (3h)

**Deliverables:**
- `ReactNativeLabelInference.kt` - Context-based labels
- `NestedTouchableDetector.kt` - Nested element detection
- `GestureHandlerDetector.kt` - RN Gesture Handler support
- Tests: LabelInferenceTest, NestedTouchableTest

---

### Phase 6: Compose Enhancements (10 hours)

**Sub-phases:**
1. Force semantic extraction (4h)
2. LazyColumn items (3h)
3. Testing (3h)

**Deliverables:**
- `ComposeSemanticExtractor.kt` - Force extraction
- `LazyColumnItemExtractor.kt` - Handle merged semantics
- Tests: ComposeSemanticTest, LazyColumnTest

---

### Phase 7: Flutter Enhancements (6 hours)

**Sub-phases:**
1. Context inference (3h)
2. Partial semantics (2h)
3. Testing (1h)

**Deliverables:**
- `FlutterContextInference.kt` - Infer from parent/siblings
- Tests: FlutterContextTest

---

## Database Schema Changes

```sql
-- New table for framework metadata
CREATE TABLE app_framework_info (
    package_name TEXT PRIMARY KEY,
    framework TEXT NOT NULL,  -- 'native', 'flutter', 'react_native', 'unity', 'compose', 'webview'
    has_semantics INTEGER DEFAULT 1,  -- 1=yes, 0=no
    detected_at INTEGER NOT NULL,
    last_explored_at INTEGER,
    element_count INTEGER DEFAULT 0,
    coverage_estimate REAL DEFAULT 0.0  -- Estimated % coverage
);

-- Add to scraped_element table
ALTER TABLE scraped_element
ADD COLUMN exploration_behavior TEXT;  -- 'clickable', 'scrollable', 'drawer', etc.

ALTER TABLE scraped_element
ADD COLUMN framework TEXT;  -- 'native', 'flutter', etc.

ALTER TABLE scraped_element
ADD COLUMN inferred_label TEXT;  -- For elements without native labels
```

---

## Testing Plan

### Unit Tests (50+ tests)

**Phase 1 Tests:**
- Element classification (10 tests)
- Child extraction (8 tests)
- Memory leak prevention (5 tests)
- Scroll integration (5 tests)

**Phase 2 Tests:**
- Menu trigger handling (6 tests)
- Drawer handling (4 tests)
- Expandable items (3 tests)

**Phase 3 Tests:**
- Framework detection (8 tests)
- Semantic availability (4 tests)

**Phase 4-7 Tests:**
- Platform-specific tests (15 tests)

### Integration Tests (10 apps)

**Test Apps:**
1. Microsoft Teams (Native) - Target: 150+ elements
2. Instagram (React Native) - Target: 200+ elements
3. Google Pay (Flutter) - Target: 80+ elements
4. Gmail (Native) - Target: 150+ elements
5. Discord (React Native) - Target: 180+ elements
6. Settings (Native) - Target: 120+ elements
7. Shopify (React Native) - Target: 150+ elements
8. Reflectly (Flutter) - Target: 90+ elements
9. Twitter (Compose) - Target: 160+ elements
10. Temple Run (Unity) - Verify detection + warning

**Success Criteria:**
- ✅ 90%+ coverage for supported apps (1-9)
- ✅ Proper detection + warning for Unity (10)
- ✅ No memory leaks
- ✅ Exploration time < 60s per screen

---

## Success Metrics

### Coverage Improvement

| App | Current | Target | Improvement |
|-----|---------|--------|-------------|
| Microsoft Teams | 70 | 150+ | **+114%** |
| Instagram (RN) | 30 | 200+ | **+567%** |
| Gmail | 40 | 150+ | **+275%** |
| Google Pay (Flutter) | 60 | 80+ | **+33%** |
| Discord (RN) | 50 | 180+ | **+260%** |

### Market Coverage

| Metric | Current | Target |
|--------|---------|--------|
| Apps fully explorable | 45% | 72% |
| Average element coverage | 30% | 90% |
| Wasted exploration attempts | 20% | 5% |

---

## Risk Mitigation

### Risk 1: Performance Degradation

**Likelihood**: MEDIUM
**Impact**: HIGH

**Mitigation:**
- Element limits (20 per scrollable, 50 per screen)
- Timeout protection (5s per container, 60s per screen)
- Memory limits (300KB max per screen)

### Risk 2: Memory Leaks

**Likelihood**: HIGH (if not careful)
**Impact**: CRITICAL

**Mitigation:**
- Mandatory node recycling with try-finally
- Unit tests with leak detector
- Android Profiler monitoring
- Code review checklist

### Risk 3: App Crashes

**Likelihood**: MEDIUM
**Impact**: HIGH

**Mitigation:**
- Try-catch all node operations
- Graceful degradation on errors
- Max iteration limits
- Timeout protection

---

## User Documentation Updates

### 1. User Guide Updates

**Section**: "How LearnApp Works"
**Add:**
- Explanation of framework detection
- What to do if app is unsupported
- Expected coverage by app type

### 2. Troubleshooting Guide

**Add Section**: "Why didn't LearnApp find all elements?"
- Check app framework (Settings → App Info)
- Flutter apps need accessibility support
- Games are not supported (yet)

### 3. Developer Guide

**Add Chapter**: "Exploration Architecture" (already created)

---

## Rollout Strategy

### Phase 1: Internal Testing (Week 1)

- Deploy to internal devices
- Test on 20+ apps
- Performance profiling
- Bug fixes

### Phase 2: Beta Testing (Week 2)

- Deploy to beta testers (50 users)
- Gather feedback
- Monitor crash reports
- Coverage analysis

### Phase 3: Gradual Rollout (Week 3)

- 25% of users
- Monitor performance metrics
- A/B testing vs old version

### Phase 4: Full Rollout (Week 4)

- 100% of users
- Announce new coverage improvements
- Update app store listing

---

## Dependencies

### Internal Dependencies

1. VoiceOSDatabaseManager - Database access
2. AccessibilityScrapingService - Node access
3. VoiceCommandProcessor - Command execution
4. ChecklistManager - Exploration tracking

### External Dependencies

- Android AccessibilityService API (API 21+)
- SQLDelight (database)
- Kotlin Coroutines
- Android Testing Library

---

## Future Considerations

### Tier 2: Flutter Vision AI (280 hours)

**Trigger**: >20% of users have Flutter apps without semantics
**Decision Point**: 3 months after Tier 1 completion
**Document**: `learnapp-tier2-flutter-vision-ai-strategy-251204.md` (to be created)

### Tier 3: Game Vision AI (240 hours)

**Trigger**: >15% of users want to learn mobile games
**Decision Point**: 6 months after Tier 2 completion
**Document**: `learnapp-tier3-game-vision-ai-strategy-251204.md` (to be created)

---

## Approval

**Specification Status**: APPROVED FOR IMPLEMENTATION
**Approved By**: [Your approval]
**Date**: 2025-12-04

**Next Steps:**
1. Review and approve specification
2. Begin Phase 1 implementation (scrollable content)
3. Create implementation plan with task breakdown
4. Set up swarm for parallel development

---

**Version**: 1.0
**Document Type**: Implementation Specification
**Effort Estimate**: 66 hours (42h incremental from Phases 1-6 base)
**Target Completion**: 2 weeks (with swarm)
**Expected Coverage**: 90% for 72% of apps

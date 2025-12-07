# LearnApp Exploration Architecture - Developer Manual Chapter

**Module**: LearnApp
**Package**: `com.augmentalis.learnapp`
**Chapter**: Exploration Engine & Cross-Platform Support
**Date**: 2025-12-04
**Status**: Active Development

---

## Table of Contents

1. [Overview](#overview)
2. [Architectural Decisions](#architectural-decisions)
3. [Problem Analysis](#problem-analysis)
4. [Implementation Tiers](#implementation-tiers)
5. [Technical Deep Dive](#technical-deep-dive)
6. [Cross-Platform Framework Support](#cross-platform-framework-support)
7. [Future Roadmap](#future-roadmap)

---

## Overview

### Purpose

The Exploration Engine is responsible for discovering and cataloging all interactive elements in Android applications through the AccessibilityService API. This chapter documents the architectural decisions, technical challenges, and implementation strategy for achieving 90%+ element coverage across multiple UI frameworks.

### Scope

- **Tier 1 (Production)**: Native Views, React Native, Jetpack Compose, Flutter with semantics
- **Tier 2 (Research)**: Flutter without semantics (vision AI)
- **Tier 3 (Research)**: Unity/Unreal games (game vision AI)

### Goals

| Platform | Target Coverage | Tier |
|----------|----------------|------|
| Native Android Views | 95% | 1 |
| React Native | 90% | 1 |
| Jetpack Compose | 95% | 1 |
| Flutter (with semantics) | 90% | 1 |
| Flutter (no semantics) | 90% | 2 |
| Unity/Unreal Games | 90% | 3 |

---

## Architectural Decisions

### Decision 1: Iterative DFS Over Recursive DFS

**Date**: 2025-12-03
**Status**: IMPLEMENTED
**Document**: `iterative-dfs-architecture-251204.md`

**Problem**: Recursive DFS caused stack overflows on complex app screens (500+ elements).

**Solution**: Switched to iterative DFS with explicit stack management using `ExplorationFrame` data class.

**Benefits:**
- No stack overflow risk
- Predictable memory usage
- Pausable/resumable exploration
- Better error recovery

**Trade-offs:**
- More complex code
- Requires manual stack management
- Slightly slower (10-15% overhead)

**Verdict**: APPROVED - Stability is worth the complexity.

---

### Decision 2: Hybrid Element Collection Strategy

**Date**: 2025-12-04
**Status**: APPROVED FOR IMPLEMENTATION
**Documents**:
- `learnapp-scrollable-content-fix-plan-251204.md`
- `learnapp-hidden-ui-patterns-analysis-251204.md`

**Problem**: AccessibilityService only sees clickable elements, missing 60% of app content hidden in:
- ScrollView/RecyclerView children (invisible until scrolled)
- Navigation drawers (closed by default)
- Overflow menus (appear on click)
- Expandable list items (collapsed by default)

**Solution**: Enhanced element classification with 9 exploration behaviors:

```kotlin
enum class ExplorationBehavior {
    CLICKABLE,          // Direct click (buttons, links)
    SCROLLABLE,         // Scroll to reveal content
    DRAWER,             // Open drawer to reveal navigation
    DROPDOWN,           // Expand to reveal options
    EXPANDABLE,         // Expand to reveal children
    MENU_TRIGGER,       // Opens menu/overlay
    TAB,                // Switches content
    BOTTOM_SHEET,       // Expand sheet
    LONG_CLICKABLE,     // Long-press reveals menu
    CONTAINER,          // Non-interactive container
    SKIP                // Ignore
}
```

**Benefits:**
- Coverage improvement: 30% → 90% for apps with lists
- Discovers hidden UI patterns systematically
- Handles 10 common UI patterns

**Trade-offs:**
- Increased exploration time (20s → 60s per screen)
- More complex state management
- Risk of triggering unintended actions (mitigated by BACK navigation)

**Verdict**: APPROVED - 60% coverage gain justifies increased complexity.

---

### Decision 3: Cross-Platform Framework Detection

**Date**: 2025-12-04
**Status**: APPROVED FOR IMPLEMENTATION
**Document**: `learnapp-cross-platform-frameworks-analysis-251204.md`

**Problem**: 55% of modern Android apps use cross-platform frameworks (Flutter, React Native, Unity) that bypass Android's View system, rendering UIs to canvas. AccessibilityService sees 1 node with 0 children.

**Solution**: Framework detection + semantic availability checks

```kotlin
enum class UIFramework {
    NATIVE_VIEWS,      // Full support
    FLUTTER,           // Conditional (needs semantics)
    REACT_NATIVE,      // Good support
    UNITY,             // No support (Tier 3)
    UNREAL,            // No support (Tier 3)
    COMPOSE,           // Excellent support
    WEBVIEW            // Has separate scraping engine
}
```

**Strategy:**
1. Detect framework from root node className
2. Check semantic availability (childCount > 0)
3. Route to appropriate handler
4. Warn user if framework unsupported

**Benefits:**
- Know immediately if app is explorable
- Skip futile exploration attempts
- Store framework metadata in database
- Inform future Tier 2/3 development

**Trade-offs:**
- Additional detection overhead (5-10ms)
- Need framework-specific handlers
- Some apps may be misclassified

**Verdict**: APPROVED - Essential for proper coverage reporting.

---

### Decision 4: Three-Tier Implementation Strategy

**Date**: 2025-12-04
**Status**: APPROVED
**Document**: `learnapp-90-percent-coverage-roadmap-251204.md`

**Problem**: Achieving 90% coverage across ALL platforms requires vastly different effort levels.

**Solution**: Tiered implementation based on ROI:

| Tier | Platforms | Effort | Coverage | ROI | Status |
|------|-----------|--------|----------|-----|--------|
| **Tier 1** | Native, RN, Compose, Flutter (with sem) | 66h | 90% for 72% of apps | ⭐⭐⭐⭐⭐ | APPROVED |
| **Tier 2** | Flutter (no semantics) | 280h | 90% for 80% of apps | ⭐⭐⭐ | RESEARCH |
| **Tier 3** | Unity/Unreal games | 240h | 90% for 93% of apps | ⭐⭐ | RESEARCH |

**Rationale:**

**Tier 1 (Production):**
- 66 hours effort (1.5 weeks)
- Covers 72% of apps (Native 45%, RN 15%, Compose 8%, Flutter 4%)
- Best ROI: 42h incremental work for 60% coverage gain
- Uses existing AccessibilityService APIs
- No ML complexity

**Tier 2 (Research):**
- 280 hours effort (7 weeks)
- Adds 8% app coverage (Flutter without semantics)
- Requires ML model training:
  - Object detection (YOLOv8)
  - Clickability classifier
  - User interaction learning
  - Pattern recognition
- Expected accuracy: 85-90%

**Tier 3 (Research):**
- 240 hours effort (6 weeks)
- Adds 13% app coverage (Unity/Unreal games)
- Requires game-specific ML models
- Touch heatmap analysis
- Expected accuracy: 75-90%

**Decision**: Implement Tier 1 FIRST, then evaluate if Tier 2/3 are worth the investment based on user demand.

**Verdict**: APPROVED - Phased approach minimizes risk and maximizes early ROI.

---

### Decision 5: Memory Management Strategy

**Date**: 2025-12-04
**Status**: CRITICAL REQUIREMENT
**Document**: `learnapp-scrollable-fix-cot-validation-251204.md`

**Problem**: AccessibilityNodeInfo objects are native resources that MUST be recycled to prevent memory leaks. Each node consumes ~2KB of native memory. A screen with 100 elements = 200KB. Exploration of 50 screens without recycling = 10MB leak.

**Solution**: Mandatory node recycling with try-finally blocks:

```kotlin
fun extractClickableChildren(node: AccessibilityNodeInfo): List<ElementInfo> {
    val children = mutableListOf<ElementInfo>()

    fun traverse(n: AccessibilityNodeInfo, depth: Int) {
        for (i in 0 until n.childCount) {
            val child = n.getChild(i) ?: continue

            try {
                // Process child
                if (child.isClickable) {
                    children.add(createElementInfo(child))
                }
                traverse(child, depth + 1)
            } finally {
                // CRITICAL: Always recycle
                child.recycle()
            }
        }
    }

    traverse(node, 0)
    return children
}
```

**Requirements:**
- EVERY `getChild()` call MUST have corresponding `recycle()`
- Use try-finally to ensure recycling even on exceptions
- Never store node references beyond scope
- Copy node data to ElementInfo immediately

**Enforcement:**
- Code review checklist item
- Unit test to verify recycling (leak detector)
- Android Profiler monitoring during testing

**Verdict**: ZERO TOLERANCE - Memory leaks are unacceptable.

---

## Problem Analysis

### Problem 1: Scrollable Content Invisibility

**Evidence**: Microsoft Teams exploration
- Left sidebar: 6 tabs (captured) ✓
- Main content RecyclerView: 0 items (missed) ❌
- Actual content: 50+ channels/teams (invisible)

**Root Cause**:
```kotlin
// OLD CODE - Only clicks on clickable elements
val clickableElements = allElements.filter { it.isClickable }

// RecyclerView container:
// - isClickable: false ❌
// - isScrollable: true ✓
// - childCount: 20+ (NEVER explored!)
```

**Impact:**
- Teams: 70% coverage gap (70/150 elements)
- Any app with lists: 60-80% coverage gap
- RecyclerView, ListView, GridView: systematically missed

**Solution**: Extract children from scrollable containers BEFORE filtering by clickability.

---

### Problem 2: Hidden UI Pattern Blindness

**Evidence**: Teams overflow menu
```
Element 120: "More options" button
- Action: CLICKED ✓
- Result: Menu appeared ✓
- Menu items extracted: NO ❌
- Why: BACK dismissed menu before extraction
```

**Impact:**
- Overflow menus: ~10-15 items per screen (lost)
- Navigation drawers: ~20-30 navigation items (never opened)
- Expandable lists: ~15-30 sub-items (never expanded)

**Solution**: Special handling for menu triggers:
1. Detect menu window appearance after click
2. Extract menu items BEFORE BACK navigation
3. Add items to exploration queue

---

### Problem 3: Cross-Platform Framework Invisibility

**Evidence**: Flutter apps without semantics

```kotlin
// What AccessibilityService sees:
FlutterView (1080x1920)
└─ childCount: 0  ❌

// What's actually rendered (invisible):
FlutterView
├─ AppBar ("Home", Search button)
├─ ListView (20+ items)
└─ BottomNav (3 tabs)

// Elements found: 1 (FlutterView container)
// Actual elements: 50+ (ALL INVISIBLE)
```

**Market Impact:**
- Flutter: 12% of apps, 70% without semantics = 8.4% of ALL apps
- Unity/Unreal: 13% of apps
- **Total impact: 21% of apps have 0% coverage**

**Solution (Tier 1)**: Detect framework, warn user if unsupported
**Solution (Tier 2)**: Vision AI for Flutter without semantics
**Solution (Tier 3)**: Game vision AI for Unity/Unreal

---

## Implementation Tiers

### Tier 1: Production (66 hours)

**Goal**: 90% coverage for 72% of apps (Native, React Native, Compose, Flutter with semantics)

**Phases**:

1. **Phase 1-3: Scrollable Content** (12 hours)
   - Element classification (SCROLLABLE behavior)
   - Child extraction from containers
   - Scroll action support
   - End detection + duplicate handling

2. **Phase 4-5: Hidden UI Patterns** (12 hours)
   - Menu trigger handling
   - Drawer opening
   - Expandable items
   - Bottom sheets
   - Long-click menus

3. **Phase 6: Framework Detection** (5 hours)
   - Detect Flutter, RN, Unity, Compose
   - Semantic availability checks
   - User warnings for unsupported apps
   - Database schema for framework metadata

4. **Phase 7: Platform-Specific Enhancements** (42 hours)
   - **Native Views** (11h): Custom canvas detection, delayed content, accessibility override
   - **React Native** (15h): Label inference, nested touchables, gesture handlers
   - **Compose** (10h): Force semantic extraction, LazyColumn items
   - **Flutter** (6h): Context-based inference for partial semantics

**Deliverables:**
- Production-ready exploration engine
- 90% coverage for 72% of apps
- Framework detection + warnings
- Comprehensive test suite

**Success Criteria:**
- Teams app: 150+ elements (vs current 70)
- Instagram (RN): 200+ elements
- Compose apps: 90%+ coverage
- Flutter with semantics: 90%+ coverage

---

### Tier 2: Research - Flutter Vision AI (280 hours)

**Goal**: 90% coverage for Flutter apps WITHOUT semantics (8% of all apps)

**Status**: RESEARCH PHASE - Implementation contingent on Tier 1 success and user demand

**Approach**: Hybrid Vision AI + User Learning

```
Phase 1: Object Detection (60-70% coverage)
↓
Phase 2: User Learning (+10-15% coverage)
↓
Phase 3: Pattern Recognition (+5-10% coverage)
↓
Result: 85-90% coverage
```

**Technical Requirements:**

1. **Object Detection Model** (60 hours)
   - Train YOLOv8 on Flutter UI elements
   - Dataset: 10,000+ annotated screenshots
   - Classes: Button, TextField, Icon, List, Card, Image
   - Target accuracy: 75-85%

2. **Clickability Classifier** (20 hours)
   - Binary classifier: Clickable vs non-clickable
   - Features: Size, shape, color, text, position
   - Dataset: 5,000+ examples
   - Target accuracy: 70-80%

3. **OCR Integration** (10 hours)
   - Google ML Kit Text Recognition
   - Extract text from detected elements
   - Target accuracy: 85-95%

4. **User Learning System** (40 hours)
   - Record user touch events
   - Build clickable area map per screen
   - Database: (screenHash, touchCoords) → ElementInfo
   - Confidence: 90%+ (user actually clicked)

5. **Pattern Recognizer** (30 hours)
   - Detect common Flutter patterns:
     - Bottom navigation bar (screen bottom)
     - FAB (bottom-right corner)
     - App bar back button (top-left)
   - Heuristic-based detection
   - Confidence: 80-90%

6. **Integration & Testing** (30 hours)
   - End-to-end testing
   - Performance tuning
   - Edge case handling

7. **Model Training** (90 hours)
   - Data collection: Screenshot 100+ Flutter apps
   - Annotation: Label 10,000+ UI elements
   - Train object detection model
   - Train clickability classifier
   - Validate on test set

**Expected Results:**
- First-time exploration: 60-70% coverage
- After user interactions: 85-90% coverage
- Unusual custom UIs: 50-60% coverage

**Limitations:**
- Can't detect semantic meaning (what button does)
- Struggles with similar-looking elements
- Small buttons/icons hard to detect
- Custom widgets with unusual appearance

**Decision Point**: Implement if >20% of user base has Flutter apps without semantics.

---

### Tier 3: Research - Game Vision AI (240 hours)

**Goal**: 90% coverage for Unity/Unreal games (13% of all apps)

**Status**: RESEARCH PHASE - Implementation contingent on Tier 2 success and user demand

**Approach**: Game-Specific Vision AI + Touch Heatmap

```
Phase 1: Game UI Detection (60-75% coverage)
↓
Phase 2: Touch Heatmap Learning (+10-15% coverage)
↓
Result: 75-90% coverage
```

**Technical Requirements:**

1. **Game UI Detection Model** (80 hours)
   - Train YOLOv8 on game UI elements
   - Dataset: 10,000+ screenshots from mobile games
   - Classes: Button, Joystick, HealthBar, Minimap, MenuItem, InventorySlot
   - Target accuracy: 65-75% (games vary more than apps)

2. **Game OCR** (20 hours)
   - Tesseract OCR (better for stylized fonts)
   - Detect damage numbers, score, HUD text
   - Target accuracy: 70-80%

3. **Interactive Element Heuristics** (30 hours)
   - Button patterns: Rectangular, text inside, visual effects
   - Joystick patterns: Circular, bottom corners
   - Health bar patterns: Top-left, elongated rectangles
   - Minimap patterns: Corner squares

4. **Touch Heatmap System** (40 hours)
   - Record user touch coordinates per screen
   - Cluster touch points to find "hot zones"
   - Generate interactive area rectangles
   - Database: (screenHash, touchClusters) → InteractiveAreas

5. **Integration & Testing** (30 hours)
   - Test on popular games
   - Performance optimization
   - Edge case handling

6. **Model Training** (100 hours)
   - Data collection: Screenshot 50+ mobile games (diverse genres)
   - Annotation: Label game UI elements
   - Train detection model
   - Validate on test set

**Expected Results:**
- Vision AI alone: 60-75% coverage
- + Touch heatmap: 75-90% coverage
- Unusual game UIs: 40-60% coverage

**Limitations:**
- Can't understand game mechanics
- 3D UI elements difficult to detect
- Overlapping UI elements cause confusion
- Stylized/minimal UIs hard to detect

**Decision Point**: Implement if >15% of user base wants to learn mobile games.

---

## Technical Deep Dive

### Element Classification Algorithm

```kotlin
private fun classifyElement(element: ElementInfo): ExplorationBehavior {
    return when {
        // PRIORITY 1: Clickable elements (highest priority)
        element.isClickable && element.isEnabled -> {
            // Check for special clickable types
            when {
                isOverflowMenu(element) -> ExplorationBehavior.MENU_TRIGGER
                isTabElement(element) -> ExplorationBehavior.TAB
                else -> ExplorationBehavior.CLICKABLE
            }
        }

        // PRIORITY 2: Drawers and dropdowns (interactive reveals)
        isDrawerLayout(element) -> ExplorationBehavior.DRAWER
        isSpinner(element) -> ExplorationBehavior.DROPDOWN
        isBottomSheet(element) -> ExplorationBehavior.BOTTOM_SHEET

        // PRIORITY 3: Scrollable containers
        element.isScrollable && isScrollableContainer(element.className) -> {
            when {
                isChipGroup(element) -> ExplorationBehavior.CHIP_GROUP
                isCollapsingToolbar(element) -> ExplorationBehavior.COLLAPSING_TOOLBAR
                else -> ExplorationBehavior.SCROLLABLE
            }
        }

        // PRIORITY 4: Expandable items
        hasExpandAction(element) -> ExplorationBehavior.EXPANDABLE

        // PRIORITY 5: Long-clickable items
        hasLongClickAction(element) -> ExplorationBehavior.LONG_CLICKABLE

        // PRIORITY 6: Containers
        element.node?.childCount ?: 0 > 0 -> ExplorationBehavior.CONTAINER

        // Skip everything else
        else -> ExplorationBehavior.SKIP
    }
}
```

**Priority Rationale:**
1. **Clickable first** - Direct interactions are most important
2. **Drawers/dropdowns** - Must be explicitly opened
3. **Scrollables** - Require scroll actions
4. **Expandables** - Require expand actions
5. **Long-clickables** - Secondary interactions
6. **Containers** - Explore children only

---

### Child Extraction with Memory Safety

```kotlin
private fun extractClickableChildren(
    node: AccessibilityNodeInfo,
    scrollableDepth: Int = 0
): List<ElementInfo> {
    val children = mutableListOf<ElementInfo>()
    val startTime = System.currentTimeMillis()
    val EXTRACTION_TIMEOUT_MS = 5000

    // Check for empty state
    if (node.childCount == 0) {
        Log.d(TAG, "Container is empty (childCount=0)")
        return emptyList()
    }

    fun traverse(n: AccessibilityNodeInfo, depth: Int) {
        // Timeout protection
        if (System.currentTimeMillis() - startTime > EXTRACTION_TIMEOUT_MS) {
            Log.w(TAG, "Child extraction timeout reached")
            return
        }

        for (i in 0 until n.childCount) {
            val child = n.getChild(i) ?: continue

            try {
                // Check if child is a nested scrollable
                if (isScrollableContainer(child.className?.toString() ?: "")) {
                    if (scrollableDepth < MAX_SCROLLABLE_DEPTH) {
                        // Extract from nested scrollable
                        val nestedChildren = extractClickableChildren(child, scrollableDepth + 1)
                        children.addAll(nestedChildren)
                    } else {
                        Log.w(TAG, "Max scrollable depth reached, skipping nested")
                    }
                } else if (child.isClickable && child.isEnabled) {
                    children.add(createElementInfo(child))
                } else {
                    // Continue traversing non-scrollable containers
                    traverse(child, depth + 1)
                }
            } finally {
                // CRITICAL: Recycle to prevent memory leaks
                child.recycle()
            }
        }
    }

    traverse(node, 0)
    return children.take(MAX_ELEMENTS_PER_SCROLLABLE)
}
```

**Key Features:**
- **Memory safe**: `finally { child.recycle() }`
- **Timeout protection**: 5-second max per container
- **Nested scrollable handling**: Max depth of 2
- **Element limit**: 20 per scrollable, 50 per screen

---

### Framework Detection

```kotlin
enum class UIFramework {
    NATIVE_VIEWS,
    FLUTTER,
    REACT_NATIVE,
    UNITY,
    UNREAL,
    COMPOSE,
    WEBVIEW,
    UNKNOWN
}

fun detectFramework(rootNode: AccessibilityNodeInfo, packageName: String): UIFramework {
    val className = rootNode.className?.toString() ?: ""

    return when {
        className.contains("FlutterView") ||
        className.contains("io.flutter.embedding") -> UIFramework.FLUTTER

        className.contains("ReactRootView") ||
        className.contains("com.facebook.react") -> UIFramework.REACT_NATIVE

        className.contains("UnityPlayer") -> UIFramework.UNITY

        className.contains("SurfaceView") &&
        rootNode.childCount == 0 -> UIFramework.UNITY  // Games typically use SurfaceView

        className.contains("WebView") -> UIFramework.WEBVIEW

        className.contains("AndroidComposeView") -> UIFramework.COMPOSE

        else -> UIFramework.NATIVE_VIEWS
    }
}

fun hasSemantics(rootNode: AccessibilityNodeInfo, framework: UIFramework): Boolean {
    return when (framework) {
        UIFramework.FLUTTER, UIFramework.COMPOSE, UIFramework.WEBVIEW -> {
            rootNode.childCount > 0
        }
        UIFramework.UNITY, UIFramework.UNREAL -> false
        else -> true  // Native and RN always have semantics
    }
}
```

---

## Cross-Platform Framework Support

### Market Distribution (2024)

| Framework | % of Apps | Tier 1 Support | Coverage |
|-----------|-----------|----------------|----------|
| Native Views | 45% | ✅ Full | 95% |
| React Native | 15% | ✅ Full | 90% |
| Jetpack Compose | 8% | ✅ Full | 95% |
| Flutter (with semantics) | 4% | ✅ Full | 90% |
| Flutter (no semantics) | 8% | ❌ Tier 2 | 0% → 90% |
| Unity | 10% | ❌ Tier 3 | 0% → 90% |
| Unreal | 3% | ❌ Tier 3 | 0% → 90% |
| WebView | 5% | ✅ Separate | 90% |
| Other | 2% | ⚠️ Varies | 50% |

### Tier 1 Coverage

**With Tier 1 implementation:**
- 72% of apps: 90%+ coverage ✅
- 28% of apps: 0-30% coverage ❌

**By platform:**
- Native (45%): 95% coverage
- React Native (15%): 90% coverage
- Compose (8%): 95% coverage
- Flutter with semantics (4%): 90% coverage

### Detection Examples

**Flutter with Semantics:**
```kotlin
FlutterView (childCount=15)
├─ SemanticsNode (Button: "Login")
├─ SemanticsNode (TextField: "Email")
├─ SemanticsNode (TextField: "Password")
└─ ...

// Detection: Framework=FLUTTER, hasSemantics=true
// Action: Standard exploration ✅
```

**Flutter without Semantics:**
```kotlin
FlutterView (childCount=0)

// Detection: Framework=FLUTTER, hasSemantics=false
// Action: Warn user + skip ❌
// Message: "This Flutter app doesn't support accessibility.
//          Only basic exploration possible."
```

**Unity Game:**
```kotlin
SurfaceView (childCount=0)

// Detection: Framework=UNITY
// Action: Warn user + skip ❌
// Message: "This is a game (Unity/Unreal). Visual exploration
//          not yet supported. UI elements cannot be detected."
```

---

## Future Roadmap

### Short-term (Next 3 months)

1. **Complete Tier 1 Implementation** (66 hours)
   - Scrollable content exploration
   - Hidden UI pattern handling
   - Framework detection
   - Platform-specific enhancements

2. **Production Testing** (40 hours)
   - Test on 50+ popular apps
   - Performance profiling
   - Memory leak detection
   - Edge case handling

3. **Documentation** (20 hours)
   - API documentation
   - User guide updates
   - Troubleshooting guide
   - Performance tuning guide

### Mid-term (3-6 months)

4. **Evaluate Tier 2 Necessity** (8 hours)
   - Analyze user feedback
   - Survey Flutter app usage
   - Cost-benefit analysis
   - Decision: Proceed or defer

5. **Tier 2 Research (if approved)** (280 hours)
   - Flutter vision AI prototype
   - Model training
   - User learning system
   - Pattern recognition

### Long-term (6-12 months)

6. **Evaluate Tier 3 Necessity** (8 hours)
   - Analyze game learning demand
   - Survey Unity/Unreal usage
   - Cost-benefit analysis
   - Decision: Proceed or defer

7. **Tier 3 Research (if approved)** (240 hours)
   - Game vision AI prototype
   - Model training
   - Touch heatmap system
   - Integration testing

---

## Performance Targets

### Tier 1 Performance

| Operation | Current | Target | Tier 1 |
|-----------|---------|--------|--------|
| Screen exploration | 10-20s | 60s | 60s |
| Element extraction | 50ms | 100ms | 100ms |
| Framework detection | N/A | 10ms | 10ms |
| Memory per screen | 50KB | 200KB | 150KB |
| Elements per screen | 5-10 | 30-50 | 40 |

### Tier 2 Performance (Research)

| Operation | Target |
|-----------|--------|
| Vision AI inference | 200-300ms |
| OCR per element | 50-100ms |
| User learning lookup | 10-20ms |
| Pattern detection | 50ms |
| Total per screen | 2-3s (first time), 500ms (learned) |

### Tier 3 Performance (Research)

| Operation | Target |
|-----------|--------|
| Game UI detection | 300-500ms |
| Game OCR | 100-200ms |
| Touch heatmap lookup | 20ms |
| Total per screen | 3-5s (first time), 1s (learned) |

---

## Testing Strategy

### Unit Tests

1. **Element Classification** (ElementClassificationTest.kt)
   - Test each ExplorationBehavior
   - Priority ordering validation
   - Edge cases (null checks, empty states)

2. **Child Extraction** (ChildExtractionTest.kt)
   - Memory leak prevention (verify recycle())
   - Nested scrollables
   - Empty containers
   - Timeout handling
   - Element limit enforcement

3. **Framework Detection** (FrameworkDetectionTest.kt)
   - Each framework type
   - Semantic availability
   - Edge cases (hybrid apps, custom views)

### Integration Tests

4. **Real App Testing**
   - Microsoft Teams: 150+ elements target
   - Instagram (RN): 200+ elements target
   - Google Pay (Flutter): 80+ elements target
   - Gmail: 150+ elements target

5. **Performance Testing**
   - Memory profiling (no leaks)
   - Time per screen (< 60s)
   - Element extraction (< 100ms per element)

---

## Database Schema Changes

### New Tables

```sql
-- Framework metadata
CREATE TABLE app_framework_info (
    package_name TEXT PRIMARY KEY,
    framework TEXT NOT NULL,  -- 'native', 'flutter', 'react_native', 'unity', 'compose', 'webview'
    has_semantics INTEGER DEFAULT 1,  -- 1=yes, 0=no
    detected_at INTEGER NOT NULL,
    last_explored_at INTEGER
);

-- Element exploration metadata
ALTER TABLE scraped_element
ADD COLUMN exploration_behavior TEXT;  -- 'clickable', 'scrollable', 'drawer', etc.

ALTER TABLE scraped_element
ADD COLUMN framework TEXT;  -- 'native', 'flutter', etc.
```

---

## References

### Specifications

1. `learnapp-scrollable-content-fix-plan-251204.md` - Scrollable content fix (Phases 1-3)
2. `learnapp-hidden-ui-patterns-analysis-251204.md` - Hidden UI patterns (Phases 4-5)
3. `learnapp-cross-platform-frameworks-analysis-251204.md` - Framework detection (Phase 6)
4. `learnapp-90-percent-coverage-roadmap-251204.md` - Complete roadmap (Phases 7-9)
5. `learnapp-scrollable-fix-cot-validation-251204.md` - CoT validation report

### Architecture Documents

6. `iterative-dfs-architecture-251204.md` - Iterative DFS design
7. `learnapp-consent-dialog-misattribution-251204.md` - Consent dialog analysis

---

**Version**: 1.0
**Last Updated**: 2025-12-04
**Status**: Active Development
**Next Review**: After Tier 1 completion

# LearnApp 90%+ Coverage Roadmap - By Platform

**Document:** 90% Coverage Technical Roadmap
**Date:** 2025-12-04
**Status:** TECHNICAL ANALYSIS
**Priority:** HIGH
**Goal:** Achieve 90%+ element coverage on ALL major platforms

---

## Current vs Target Coverage

| Platform | Current | With Phases 1-6 | Target | Gap |
|----------|---------|-----------------|--------|-----|
| **Native Views** | 30% | 90% | **95%** | **5%** ⭐ |
| **React Native** | 60% | 75% | **90%** | **15%** |
| **Compose** | 70% | 90% | **95%** | **5%** ⭐ |
| **Flutter (with semantics)** | 80% | 85% | **90%** | **5%** ⭐ |
| **Flutter (no semantics)** | 0% | 0% | **90%** | **90%** ❌ |
| **Unity/Unreal** | 0% | 0% | **90%** | **90%** ❌ |
| **WebView** | 50% | 70% | **90%** | SKIPPED (existing engine) |

---

## 1. Native Views → 95% Coverage

**Current with Phases 1-6:** 90%
**Gap:** 5%
**Difficulty:** ⭐ Easy

### Remaining Gaps

1. **Custom Views with Canvas Drawing** (2% gap)
   - Apps that extend View and draw UI manually
   - Example: Custom charts, game-like UIs in regular apps

2. **Accessibility-Disabled Views** (2% gap)
   - Developers explicitly set `importantForAccessibility="no"`
   - Hidden elements that ARE clickable

3. **Dynamic Views (Late Binding)** (1% gap)
   - Views created after exploration
   - Lazy-loaded content that appears on scroll/delay

### Solution: Advanced Detection Strategies

#### 1a. Custom Canvas View Detection
```kotlin
fun hasCustomDrawing(node: AccessibilityNodeInfo): Boolean {
    // Custom views often have:
    // - No children
    // - Large bounds
    // - Generic className (View, FrameLayout)
    // - No text/contentDescription

    return node.childCount == 0 &&
           node.className?.toString() == "android.view.View" &&
           node.boundsInScreen.width() > 200 &&
           node.boundsInScreen.height() > 200
}

suspend fun handleCustomCanvasView(node: AccessibilityNodeInfo): List<ElementInfo> {
    // Strategy 1: Screenshot + Vision AI
    val screenshot = takeScreenshotOfBounds(node.boundsInScreen)
    val detectedElements = visionAI.detectUIElements(screenshot)

    // Strategy 2: Touch exploration
    // Programmatically touch various points and detect responses
    val touchPoints = generateTouchGrid(node.boundsInScreen)
    val responsivePoints = testTouchPoints(touchPoints)

    return detectedElements + responsivePoints
}
```

**Effort:** 6 hours
**Coverage gain:** +2%

#### 1b. Accessibility-Disabled View Recovery
```kotlin
fun forceEnableAccessibility(node: AccessibilityNodeInfo): Boolean {
    // Some views are hidden from accessibility but still interactive
    // Check if view is ACTUALLY clickable even if marked as unimportant

    if (node.importantForAccessibility == View.IMPORTANT_FOR_ACCESSIBILITY_NO) {
        // Try to click anyway
        val wasClickable = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return wasClickable
    }
    return false
}
```

**Effort:** 2 hours
**Coverage gain:** +2%

#### 1c. Delayed Content Detection
```kotlin
suspend fun waitForDynamicContent(screenHash: String, timeout: Long = 5000) {
    val startTime = System.currentTimeMillis()
    val initialElements = extractAllElements()

    while (System.currentTimeMillis() - startTime < timeout) {
        delay(500)
        val currentElements = extractAllElements()

        if (currentElements.size > initialElements.size) {
            // New elements appeared!
            val newElements = currentElements - initialElements
            Log.i(TAG, "Found ${newElements.size} delayed elements")
            return newElements
        }
    }
}
```

**Effort:** 3 hours
**Coverage gain:** +1%

**Total to reach 95%:** 11 hours

---

## 2. React Native → 90% Coverage

**Current with Phases 1-6:** 75%
**Gap:** 15%
**Difficulty:** ⭐⭐ Medium

### Remaining Gaps

1. **Elements Without Accessibility Labels** (10% gap)
   - Developers forget `accessibilityLabel`
   - Custom TouchableOpacity components
   - Image-based buttons without labels

2. **Nested Touchables** (3% gap)
   - TouchableOpacity inside TouchableOpacity
   - Only outer element detected

3. **Gesture Handlers** (2% gap)
   - React Native Gesture Handler library
   - Swipeable components without accessibility

### Solution: Visual + Context Inference

#### 2a. Label-Free Element Detection
```kotlin
fun inferElementPurpose(node: AccessibilityNodeInfo): String? {
    // Strategy 1: Check siblings for context
    val siblings = getSiblings(node)
    val nearbyText = siblings.mapNotNull { it.text?.toString() }

    // Strategy 2: Check visual properties
    val bounds = node.boundsInScreen
    val isButtonSized = bounds.width() in 40..200 && bounds.height() in 40..80

    // Strategy 3: Check icon patterns
    val hasImage = hasImageChild(node)

    return when {
        isButtonSized && hasImage && nearbyText.isNotEmpty() -> {
            // Likely button with nearby label
            "Button: ${nearbyText.first()}"
        }
        bounds.width() > screenWidth * 0.8 && bounds.height() < 100 -> {
            // Likely list item or card
            "Interactive card"
        }
        else -> {
            // Generate UUID from visual properties
            generateVisualUUID(node)
        }
    }
}
```

**Effort:** 8 hours
**Coverage gain:** +10%

#### 2b. Nested Touchable Detection
```kotlin
fun findNestedTouchables(parent: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
    val touchables = mutableListOf<AccessibilityNodeInfo>()

    fun traverse(node: AccessibilityNodeInfo) {
        if (node.isClickable) {
            touchables.add(node)
        }

        // Don't stop at first clickable - keep searching
        for (i in 0 until node.childCount) {
            traverse(node.getChild(i))
        }
    }

    traverse(parent)
    return touchables.distinctBy { it.boundsInScreen }
}
```

**Effort:** 3 hours
**Coverage gain:** +3%

#### 2c. Gesture Handler Detection
```kotlin
fun detectGestureHandlers(node: AccessibilityNodeInfo): List<GestureInfo> {
    // React Native Gesture Handler uses specific View names
    val className = node.className?.toString() ?: ""

    return when {
        className.contains("GestureHandler") -> {
            listOf(GestureInfo(
                type = "swipe",
                directions = listOf("left", "right"),
                node = node
            ))
        }
        className.contains("Swipeable") -> {
            listOf(GestureInfo(
                type = "swipe",
                directions = listOf("left", "right"),
                node = node
            ))
        }
        else -> emptyList()
    }
}
```

**Effort:** 4 hours
**Coverage gain:** +2%

**Total to reach 90%:** 15 hours

---

## 3. Compose → 95% Coverage

**Current with Phases 1-6:** 90%
**Gap:** 5%
**Difficulty:** ⭐ Easy

### Remaining Gaps

1. **Compose Without Semantics** (3% gap)
   - Developers use `Box` + `clickable` without semantics
   - Custom composables without proper modifiers

2. **LazyColumn with Incomplete Semantics** (2% gap)
   - Items without individual semantics
   - Merged semantics that hide children

### Solution: Compose-Specific Semantic Enhancement

#### 3a. Force Semantic Extraction
```kotlin
fun extractComposeSemantics(composeView: AccessibilityNodeInfo): List<ElementInfo> {
    val elements = mutableListOf<ElementInfo>()

    fun traverse(node: AccessibilityNodeInfo) {
        // Even if node lacks contentDescription, check for:
        // 1. Click action availability
        // 2. Text content in children
        // 3. Bounds (size indicates interactivity)

        val hasClickAction = node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_CLICK
        }

        if (hasClickAction) {
            val inferredLabel = inferComposeLabel(node)
            elements.add(ElementInfo(
                node = node,
                contentDescription = inferredLabel,
                isClickable = true
            ))
        }

        for (i in 0 until node.childCount) {
            traverse(node.getChild(i))
        }
    }

    traverse(composeView)
    return elements
}

fun inferComposeLabel(node: AccessibilityNodeInfo): String {
    // Check for text in children
    val textChildren = findTextChildren(node)
    if (textChildren.isNotEmpty()) {
        return textChildren.joinToString(" ")
    }

    // Check for role
    val role = node.roleDescription
    if (role != null) {
        return role.toString()
    }

    // Fallback to bounds-based ID
    return "compose_element_${node.boundsInScreen.hashCode()}"
}
```

**Effort:** 6 hours
**Coverage gain:** +3%

#### 3b. LazyColumn Item Extraction
```kotlin
fun extractLazyColumnItems(lazyColumn: AccessibilityNodeInfo): List<ElementInfo> {
    // LazyColumn items may have merged semantics
    // Need to unmerge and extract individual items

    val items = mutableListOf<ElementInfo>()

    // Strategy 1: Scroll and collect unique items
    val seenHashes = mutableSetOf<String>()

    repeat(10) { iteration ->
        val visibleItems = extractVisibleItems(lazyColumn)

        visibleItems.forEach { item ->
            val hash = computeItemHash(item)
            if (hash !in seenHashes) {
                seenHashes.add(hash)
                items.add(item)
            }
        }

        // Scroll down
        lazyColumn.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        delay(300)
    }

    return items
}
```

**Effort:** 4 hours
**Coverage gain:** +2%

**Total to reach 95%:** 10 hours

---

## 4. Flutter (WITH Semantics) → 90% Coverage

**Current with Phases 1-6:** 85%
**Gap:** 5%
**Difficulty:** ⭐ Easy

### Remaining Gaps

1. **Incomplete Semantics Implementation** (3% gap)
   - Developer adds semantics to some widgets but not all
   - Nested widgets with partial semantics

2. **Custom Flutter Widgets** (2% gap)
   - Custom-built widgets without semantic wrappers

### Solution: Flutter Semantic Enhancement

#### 4a. Partial Semantic Recovery
```kotlin
fun enhanceFlutterSemantics(flutterView: AccessibilityNodeInfo): List<ElementInfo> {
    val elements = extractClickableChildren(flutterView)

    // For elements without labels, try to infer from:
    // 1. Parent semantic node
    // 2. Sibling semantic nodes
    // 3. Visual properties (bounds, position)

    elements.forEach { element ->
        if (element.contentDescription.isNullOrEmpty()) {
            element.contentDescription = inferFromContext(element)
        }
    }

    return elements
}

fun inferFromContext(element: ElementInfo): String {
    val parent = element.node?.parent
    val parentLabel = parent?.contentDescription

    val siblings = getSiblings(element.node)
    val siblingLabels = siblings.mapNotNull { it.contentDescription }

    return when {
        parentLabel != null -> "$parentLabel item"
        siblingLabels.isNotEmpty() -> "Related to ${siblingLabels.first()}"
        else -> "flutter_element_${element.boundsInScreen.hashCode()}"
    }
}
```

**Effort:** 6 hours
**Coverage gain:** +5%

**Total to reach 90%:** 6 hours

---

## 5. Flutter (NO Semantics) → 90% Coverage

**Current with Phases 1-6:** 0%
**Gap:** 90%
**Difficulty:** ⭐⭐⭐⭐⭐ VERY HARD (requires advanced vision AI)

### The Challenge

Flutter apps without semantics render everything to a single canvas. Android sees:
```
FlutterView (1080x1920)
└─ [No children]  ❌
```

**Traditional AccessibilityService approach:** IMPOSSIBLE

### Solution Approaches

#### Approach 5a: Flutter Engine Hooking (INVASIVE)
```kotlin
// Requires rooted device or instrumented Flutter engine
fun hookFlutterEngine(): FlutterWidgetTree {
    // Hook into Flutter's rendering pipeline
    // Extract widget tree directly from Flutter runtime

    val flutterEngine = findFlutterEngine()
    val widgetTree = flutterEngine.getWidgetTree()  // Hypothetical API

    // Convert Flutter widgets to our ElementInfo
    return widgetTree.map { widget ->
        ElementInfo(
            bounds = widget.bounds,
            text = widget.text,
            type = widget.type,
            onClick = widget.onTap
        )
    }
}
```

**Requirements:**
- Root access OR instrumented Flutter SDK
- Modify Flutter engine to expose widget tree
- Custom Flutter build for target app (IMPOSSIBLE for 3rd party apps)

**Effort:** 100+ hours (research + implementation)
**Coverage gain:** +90%
**Feasibility:** ❌ Not viable for general apps

---

#### Approach 5b: Computer Vision AI (REALISTIC)

**Architecture:**
```
1. Screenshot Flutter canvas
2. ML model detects UI elements
3. OCR extracts text
4. Heuristics determine clickability
5. Generate synthetic ElementInfo
```

**ML Model Requirements:**

1. **Object Detection Model**
   - Trained on Android UI elements
   - Detects: buttons, text fields, lists, icons
   - YOLOv8 or Detectron2

2. **OCR Engine**
   - Google ML Kit Text Recognition
   - Extracts text from detected elements

3. **Clickability Classifier**
   - Determines if detected element is interactive
   - Features: size, shape, color, text content, position

**Implementation:**
```kotlin
class FlutterVisionExplorer {
    private val objectDetector = YOLOv8Model("flutter_ui_detector.tflite")
    private val ocrEngine = GoogleMLKit.getClient(TextRecognition.getClient())
    private val clickabilityClassifier = TFLiteModel("clickability.tflite")

    suspend fun exploreFlutterCanvas(flutterView: AccessibilityNodeInfo): List<ElementInfo> {
        // 1. Take screenshot of Flutter canvas
        val bounds = flutterView.boundsInScreen
        val screenshot = takeScreenshotOfBounds(bounds)

        // 2. Detect UI elements
        val detectedObjects = objectDetector.detect(screenshot)
        // Returns: List<DetectedObject(bounds, class, confidence)>

        // 3. Extract text from each element
        val elementsWithText = detectedObjects.map { obj ->
            val elementScreenshot = screenshot.crop(obj.bounds)
            val text = ocrEngine.process(elementScreenshot).text

            ElementInfo(
                bounds = obj.bounds,
                text = text,
                className = obj.class,  // "Button", "TextField", "Icon", etc.
                confidence = obj.confidence
            )
        }

        // 4. Determine clickability
        val interactiveElements = elementsWithText.filter { element ->
            clickabilityClassifier.predict(
                features = extractFeatures(element)
            ) > 0.7  // 70% confidence threshold
        }

        // 5. Generate UUIDs based on visual properties
        return interactiveElements.map { element ->
            element.copy(
                uuid = generateVisualUUID(element.bounds, element.text, element.className)
            )
        }
    }

    private fun extractFeatures(element: ElementInfo): FloatArray {
        return floatArrayOf(
            element.bounds.width().toFloat(),
            element.bounds.height().toFloat(),
            element.bounds.centerX().toFloat(),
            element.bounds.centerY().toFloat(),
            element.text?.length?.toFloat() ?: 0f,
            if (element.className == "Button") 1f else 0f,
            element.confidence
        )
    }
}
```

**Training Data Requirements:**

1. **Object Detection Training Set**
   - 10,000+ annotated screenshots of Flutter apps
   - Labels: Button, TextField, Image, Icon, List, Card, etc.
   - Diverse apps (Material, Cupertino, custom themes)

2. **Clickability Training Set**
   - 5,000+ examples of clickable vs non-clickable elements
   - Features: size, shape, color, text, position
   - Ground truth from Flutter apps WITH semantics

**Effort Breakdown:**

| Task | Hours | Description |
|------|-------|-------------|
| Data collection | 40h | Screenshot 100+ Flutter apps, annotate UI elements |
| Model training (object detection) | 60h | Train YOLOv8 on Flutter UI dataset |
| Model training (clickability) | 20h | Train classifier on interactive elements |
| Integration | 30h | Integrate models into exploration engine |
| Testing & tuning | 30h | Test on real apps, tune thresholds |
| **Total** | **180h** | ~4-5 weeks full-time |

**Expected Results:**
- Object detection accuracy: 75-85%
- Text extraction accuracy: 85-95% (OCR is mature)
- Clickability prediction: 70-80%
- **Overall coverage: 60-70%** (not 90%!)

**Why not 90%?**
- Vision AI can't detect semantic meaning
- Can't distinguish similar-looking elements
- Small buttons/icons hard to detect
- Custom widgets with unusual appearance
- Overlapping elements cause confusion

**To reach 90%:** Need hybrid approach

---

#### Approach 5c: HYBRID - Vision AI + User Learning (BEST)

**Concept:** Combine vision AI with user interaction learning

**Phase 1: Initial Vision Detection (60-70% coverage)**
- Use vision AI to detect obvious elements
- Mark detected elements with confidence scores

**Phase 2: User-Assisted Learning (→ 85% coverage)**
- As user interacts with app, record:
  - Touch coordinates
  - Element clicked (from vision AI)
  - Screen state before/after
- Learn: "At position (X,Y) with screen hash ABC, this is a button"

**Phase 3: Pattern Recognition (→ 90% coverage)**
- Detect patterns across apps:
  - Bottom navigation bars at screen bottom
  - Top app bars at screen top
  - FABs in bottom-right corner
- Apply heuristics even when vision fails

**Implementation:**
```kotlin
class HybridFlutterExplorer {
    private val visionExplorer = FlutterVisionExplorer()
    private val userLearningDB = UserInteractionDatabase()
    private val patternRecognizer = UIPatternRecognizer()

    suspend fun exploreFlutter(flutterView: AccessibilityNodeInfo): List<ElementInfo> {
        val screenHash = computeScreenHash(takeScreenshot(flutterView))

        // 1. Vision AI detection (60-70% coverage)
        val visionElements = visionExplorer.exploreFlutterCanvas(flutterView)

        // 2. Check learned interactions for this screen
        val learnedElements = userLearningDB.getElementsForScreen(screenHash)

        // 3. Apply UI patterns
        val patternElements = patternRecognizer.detectCommonPatterns(
            screenshot = takeScreenshot(flutterView),
            knownElements = visionElements + learnedElements
        )

        // 4. Merge and deduplicate
        val allElements = (visionElements + learnedElements + patternElements)
            .distinctBy { it.bounds }
            .sortedBy { it.confidence }
            .take(50)  // Limit to top 50 by confidence

        return allElements
    }
}

class UserInteractionDatabase {
    // Store: (screenHash, touchCoords) → ElementInfo
    fun recordUserClick(screenHash: String, x: Int, y: Int, afterScreenHash: String) {
        val element = ElementInfo(
            bounds = Rect(x-50, y-50, x+50, y+50),  // Assume 100x100 tap target
            text = null,  // Unknown
            isClickable = true,
            confidence = 0.9f  // High confidence - user actually clicked it
        )

        database.insert(screenHash, element)
    }

    fun getElementsForScreen(screenHash: String): List<ElementInfo> {
        return database.query(screenHash)
    }
}

class UIPatternRecognizer {
    fun detectCommonPatterns(screenshot: Bitmap, knownElements: List<ElementInfo>): List<ElementInfo> {
        val patterns = mutableListOf<ElementInfo>()

        // Pattern 1: Bottom navigation bar (common in Flutter)
        if (hasBottomNavPattern(screenshot)) {
            val bottomNavBounds = Rect(0, screenHeight - 80, screenWidth, screenHeight)
            patterns.add(ElementInfo(
                bounds = bottomNavBounds,
                text = "Bottom Navigation",
                confidence = 0.8f
            ))
        }

        // Pattern 2: Floating Action Button (bottom-right)
        if (hasFABPattern(screenshot)) {
            val fabBounds = Rect(screenWidth - 80, screenHeight - 160, screenWidth - 16, screenHeight - 96)
            patterns.add(ElementInfo(
                bounds = fabBounds,
                text = "Floating Action Button",
                confidence = 0.85f
            ))
        }

        // Pattern 3: App bar with back button (top-left)
        if (hasAppBarPattern(screenshot)) {
            val backButtonBounds = Rect(16, 16, 72, 72)
            patterns.add(ElementInfo(
                bounds = backButtonBounds,
                text = "Back button",
                confidence = 0.9f
            ))
        }

        return patterns
    }
}
```

**Effort Breakdown:**

| Task | Hours | Description |
|------|-------|-------------|
| Vision AI (from 5b) | 180h | Object detection + clickability |
| User learning system | 40h | Database + recording + playback |
| Pattern recognizer | 30h | Common Flutter UI patterns |
| Integration & testing | 30h | End-to-end testing |
| **Total** | **280h** | ~7 weeks full-time |

**Expected Coverage:**
- Vision AI: 60-70%
- + User learning: 75-85%
- + Pattern recognition: 85-90%
- **Final: 90%** ✓

**Limitations:**
- First-time users get 60-70% coverage
- Requires user interactions to improve
- Unusual custom UIs may still be missed

---

## 6. Unity/Unreal Games → 90% Coverage

**Current with Phases 1-6:** 0%
**Gap:** 90%
**Difficulty:** ⭐⭐⭐⭐⭐ VERY HARD

### The Challenge

Unity/Unreal games render to OpenGL/Vulkan canvas. **No semantic information at all.**

### Solution Approaches

#### Approach 6a: Game-Specific Vision AI (REALISTIC)

**Similar to Flutter vision AI, but game-optimized:**

1. **Game UI Detection Model**
   - Trained specifically on game UIs
   - Detects: buttons, health bars, joysticks, minimap, inventory slots
   - Different visual patterns than productivity apps

2. **Text Detection**
   - OCR for game text (often stylized fonts)
   - Detect damage numbers, score, HUD text

3. **Interactive Element Heuristics**
   - Buttons typically have:
     - Rectangular or rounded bounds
     - Text inside
     - Visual effects (shadows, borders)
   - Joysticks: circular, bottom-left/right
   - Health bars: top-left, elongated rectangles

**Implementation:**
```kotlin
class GameVisionExplorer {
    private val gameUIDetector = YOLOv8Model("game_ui_detector.tflite")
    private val gameOCR = TesseractOCR()  // Handles stylized fonts better

    suspend fun exploreGameCanvas(surfaceView: AccessibilityNodeInfo): List<ElementInfo> {
        val screenshot = takeScreenshot(surfaceView)

        // 1. Detect game UI elements
        val detectedElements = gameUIDetector.detect(screenshot)
        // Returns: Button, Joystick, HealthBar, Minimap, MenuItem, etc.

        // 2. Filter interactive elements
        val interactiveTypes = setOf("Button", "Joystick", "MenuItem", "InventorySlot")
        val interactive = detectedElements.filter { it.class in interactiveTypes }

        // 3. Extract text where applicable
        val elementsWithText = interactive.map { element ->
            val text = if (element.class == "Button" || element.class == "MenuItem") {
                gameOCR.extract(screenshot.crop(element.bounds))
            } else null

            element.copy(text = text)
        }

        // 4. Generate UUIDs
        return elementsWithText.map { element ->
            ElementInfo(
                bounds = element.bounds,
                text = element.text,
                className = "GameElement",
                gameElementType = element.class,
                uuid = generateGameElementUUID(element)
            )
        }
    }
}
```

**Training Data:**
- 10,000+ screenshots from popular mobile games
- Annotated UI elements: buttons, joysticks, menus, etc.
- Diverse game genres: puzzle, action, RPG, strategy

**Effort:** 200-250 hours (similar to Flutter, but game UI is more varied)

**Expected Coverage:** 60-75%

---

#### Approach 6b: Touch Heatmap Analysis (COMPLEMENTARY)

**Concept:** Learn clickable areas from user behavior

```kotlin
class GameTouchHeatmap {
    private val touchHistory = mutableMapOf<String, MutableList<Point>>()

    fun recordTouch(screenHash: String, x: Int, y: Int) {
        touchHistory.getOrPut(screenHash) { mutableListOf() }
            .add(Point(x, y))
    }

    fun generateInteractiveAreas(screenHash: String): List<Rect> {
        val touches = touchHistory[screenHash] ?: return emptyList()

        // Cluster touch points to find "hot zones"
        val clusters = clusterTouches(touches, radius = 50)

        // Convert clusters to rectangles
        return clusters.map { cluster ->
            val bounds = computeBounds(cluster)
            Rect(
                bounds.left - 25,
                bounds.top - 25,
                bounds.right + 25,
                bounds.bottom + 25
            )
        }
    }
}
```

**Effort:** 40 hours
**Coverage gain:** +15% (when combined with vision AI → 75-90%)

---

#### Approach 6c: Unity Plugin (INVASIVE - requires developer cooperation)

**Concept:** Create Unity plugin that bridges to Android accessibility

```csharp
// Unity C# plugin
public class UnityAccessibilityBridge : MonoBehaviour {
    public void RegisterButton(GameObject button, string label) {
        // Send to Android accessibility service
        AndroidJNI.CallStatic<void>("registerAccessibilityNode",
            button.transform.position,
            button.GetComponent<RectTransform>().rect,
            label
        );
    }
}
```

**Requirements:**
- Developer must install plugin
- Developer must register UI elements
- Developer must rebuild game

**Feasibility:** ❌ Not viable for 3rd party games

---

### Recommendation for Games: Hybrid Vision + Heatmap

**Phase 1:** Vision AI (60-75% coverage) - 200h
**Phase 2:** Touch heatmap learning (+10-15%) - 40h
**Final:** 75-90% coverage - 240h total

**Note:** 90% is achievable but requires significant ML work

---

## Summary: Roadmap to 90%+ Coverage

| Platform | Current | Phases 1-6 | 90% Solution | Effort | Total Hours |
|----------|---------|------------|--------------|--------|-------------|
| **Native Views** | 30% | 90% | +5% (custom views, delayed content) | 11h | **11h** |
| **React Native** | 60% | 75% | +15% (label inference, nested touchables) | 15h | **15h** |
| **Compose** | 70% | 90% | +5% (force semantics, lazy items) | 10h | **10h** |
| **Flutter (with sem)** | 80% | 85% | +5% (context inference) | 6h | **6h** |
| **Flutter (no sem)** | 0% | 0% | +90% (vision AI + user learning) | 280h | **280h** |
| **Unity/Unreal** | 0% | 0% | +90% (game vision AI + heatmap) | 240h | **240h** |

---

## Prioritization Recommendation

### Tier 1: Quick Wins (42 hours → 90% coverage for 75% of apps)
1. Native Views → 95% (11h)
2. React Native → 90% (15h)
3. Compose → 95% (10h)
4. Flutter (with semantics) → 90% (6h)

**Impact:** Covers Native (45%), React Native (15%), Compose (8%), Flutter with semantics (4%) = **72% of apps**

### Tier 2: High-Value ML Work (280 hours → 90% coverage for 85% of apps)
5. Flutter (no semantics) → 90% (280h)

**Impact:** Adds Flutter without semantics (8%) = **80% of apps total**

### Tier 3: Game Support (240 hours → 90% coverage for 93% of apps)
6. Unity/Unreal → 90% (240h)

**Impact:** Adds games (13%) = **93% of apps total**

---

## Implementation Roadmap

**Phase 1-6:** Foundation (24 hours)
- Scrollable content
- Hidden UI patterns
- Framework detection
- Current: 45% → 75% coverage

**Phase 7: Tier 1 Quick Wins** (42 hours)
- Native Views enhancements
- React Native label inference
- Compose semantic forcing
- Flutter semantic enhancement
- **Result: 75% → 85% coverage for 72% of apps**

**Phase 8: Flutter Vision AI** (280 hours)
- Object detection model training
- Clickability classifier
- User learning system
- Pattern recognition
- **Result: 85% → 90% coverage for 80% of apps**

**Phase 9: Game Vision AI** (240 hours)
- Game UI detection model
- Touch heatmap system
- **Result: 90% → 92% coverage for 93% of apps**

---

## Budget & Timeline

| Phase | Hours | Weeks (1 dev) | Apps Covered | Coverage |
|-------|-------|---------------|--------------|----------|
| Phases 1-6 (base) | 24h | 0.5 weeks | 45% | 75% |
| Phase 7 (quick wins) | 42h | 1 week | 72% | 90% |
| Phase 8 (Flutter AI) | 280h | 7 weeks | 80% | 90% |
| Phase 9 (Game AI) | 240h | 6 weeks | 93% | 90% |
| **Total** | **586h** | **14.5 weeks** | **93%** | **90%** |

---

## ROI Analysis

**Option A: Stop at Phase 7** (66 hours total)
- Coverage: 90% for 72% of apps
- Effort: 1.5 weeks
- **ROI: HIGH** ⭐⭐⭐⭐⭐

**Option B: Include Flutter AI (Phase 8)** (346 hours total)
- Coverage: 90% for 80% of apps
- Effort: 8.5 weeks
- **ROI: MEDIUM** ⭐⭐⭐ (8% more apps for 7 weeks work)

**Option C: Full Coverage (Phase 9)** (586 hours total)
- Coverage: 90% for 93% of apps
- Effort: 14.5 weeks
- **ROI: LOW** ⭐⭐ (13% more apps for 13.5 weeks work)

---

## Recommendation

**Start with Phase 7 (Tier 1 Quick Wins)**
- 42 hours of work
- 90% coverage for 72% of apps
- **Best ROI**

**Then evaluate:**
- Is 72% app coverage sufficient?
- Is Flutter/Game support worth the ML investment?
- Can we partner with Flutter/Unity for better APIs?

---

**Version:** 1.0
**Status:** TECHNICAL ROADMAP COMPLETE
**Next Action:** Get approval for Phase 7 (42 hours)

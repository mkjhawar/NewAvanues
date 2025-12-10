# LearnApp Cross-Platform Framework Support - Critical Gap Analysis

**Document:** Cross-Platform Framework Accessibility Gap
**Date:** 2025-12-04
**Status:** CRITICAL ANALYSIS
**Priority:** CRITICAL
**Related:**
- learnapp-scrollable-content-fix-plan-251204.md
- learnapp-hidden-ui-patterns-analysis-251204.md

---

## Problem Statement

**CRITICAL LIMITATION DISCOVERED:**

Native Android exploration (AccessibilityNodeInfo) **CANNOT see UI elements** in apps built with:
- Flutter
- React Native
- Unity (games)
- Unreal Engine (games)
- WebView (web content)
- Jetpack Compose (partial support)

**Why?** These frameworks bypass Android's View system and render directly to canvas/surface.

---

## How Cross-Platform Frameworks Render

### 1. Flutter

**Architecture:**
- Uses **Skia rendering engine**
- Entire UI rendered to single `FlutterView` or `SurfaceView`
- Flutter manages its own widget tree internally
- Android sees: **1 node, 0 children**

**AccessibilityNodeInfo view:**
```
Node 1: FlutterView (bounds: 0,0,1080,1920)
  - className: "io.flutter.embedding.android.FlutterView"
  - childCount: 0  ❌
  - isClickable: false
  - text: null
  - No semantic information
```

**What Android sees:**
```
┌────────────────────────┐
│                        │
│   FlutterView          │
│   (single canvas)      │
│                        │
│   [All UI is pixels]   │
│                        │
└────────────────────────┘
```

**What's actually inside (invisible to AccessibilityService):**
```
Flutter Widget Tree:
- Scaffold
  - AppBar
    - Text("Home")
    - IconButton (search)
  - ListView
    - ListTile 1
    - ListTile 2
    - ListTile 3
  - BottomNavigationBar
    - Tab 1
    - Tab 2
    - Tab 3
```

**Current Exploration Result:** 1 element (the FlutterView itself)
**Actual UI Elements:** 50+ buttons, lists, tabs, etc. - **ALL INVISIBLE**

**Popular Flutter Apps:**
- Google Pay
- Alibaba
- eBay Motors
- Reflectly
- Philips Hue

---

### 2. React Native

**Architecture:**
- Bridges JavaScript to native Android views
- **Better than Flutter** - creates actual Android Views
- BUT: Custom components may lack accessibility labels

**AccessibilityNodeInfo view (GOOD case):**
```
Node 1: ReactRootView
  - Node 2: ViewGroup
    - Node 3: TextView (text: "Welcome")
    - Node 4: Button (text: "Login")
```

**AccessibilityNodeInfo view (BAD case - custom components):**
```
Node 1: ReactRootView
  - Node 2: View (no text, no contentDescription)  ❌
    - Node 3: View (button rendered as image)  ❌
```

**Issue:** Developers often use:
```jsx
<TouchableOpacity onPress={...}>
  <Image source={require('./icon.png')} />
</TouchableOpacity>
```

Creates a View with Image - no accessible name, no button role.

**Should have:**
```jsx
<TouchableOpacity
  accessible={true}
  accessibilityLabel="Search button"
  accessibilityRole="button">
  <Image source={require('./icon.png')} />
</TouchableOpacity>
```

**Popular React Native Apps:**
- Facebook
- Instagram
- Discord
- Shopify
- Bloomberg

**Exploration Result:** Varies
- Well-implemented: 70-80% coverage ✓
- Poorly implemented: 20-30% coverage ❌

---

### 3. Unity (Games)

**Architecture:**
- Entire game rendered to `GLSurfaceView` or `SurfaceView`
- Unity manages scene graph internally
- Android sees: **1 node, 0 children**

**AccessibilityNodeInfo view:**
```
Node 1: SurfaceView (bounds: 0,0,1080,1920)
  - className: "android.view.SurfaceView"
  - childCount: 0  ❌
  - isClickable: false
  - text: null
```

**What's actually inside (invisible):**
```
Unity Scene:
- Main Menu
  - Start Button
  - Settings Button
  - Quit Button
- HUD
  - Health Bar
  - Score Text
  - Pause Button
```

**Current Exploration Result:** 1 element (the SurfaceView)
**Actual UI Elements:** Buttons, menus, HUDs - **ALL INVISIBLE**

**Unity Accessibility API:**
Unity has its own accessibility system (`UnityEngine.Accessibility`), but:
- Developers must explicitly implement
- Rarely used (games prioritize visuals over accessibility)
- Does NOT bridge to Android AccessibilityService by default

**Popular Unity Apps:**
- Pokémon GO
- Temple Run
- Subway Surfers
- Monument Valley

---

### 4. Unreal Engine (Games)

**Architecture:**
- Similar to Unity
- Renders to single surface
- No Android View hierarchy

**AccessibilityNodeInfo view:**
```
Node 1: NativeActivity or SurfaceView
  - childCount: 0  ❌
```

**Popular Unreal Engine Mobile Games:**
- PUBG Mobile
- Fortnite
- ARK: Survival Evolved
- Mortal Kombat

---

### 5. WebView (Web Content)

**Architecture:**
- Embeds web content in Android
- Has **internal accessibility tree** (HTML semantics)
- BUT: Requires special API to access

**AccessibilityNodeInfo view (without proper setup):**
```
Node 1: WebView
  - childCount: 0  ❌
```

**AccessibilityNodeInfo view (with WebView accessibility enabled):**
```
Node 1: WebView
  - Node 2: WebView$WebViewAccessibility$WebViewNode (title)
  - Node 3: WebView$WebViewAccessibility$WebViewNode (button)
  - Node 4: WebView$WebViewAccessibility$WebViewNode (link)
```

**Key:** WebView must have:
```java
webView.getSettings().setJavaScriptEnabled(true);
webView.getSettings().setAccessibilityEnabled(true);  // Not always set!
```

**Popular WebView Apps:**
- Hybrid apps (Ionic, Cordova)
- In-app browsers
- Many e-commerce apps

---

### 6. Jetpack Compose

**Architecture:**
- Native Android UI toolkit (replaces Views)
- Renders to canvas/surface
- **GOOD NEWS:** Has built-in accessibility support via `Modifier.semantics`

**AccessibilityNodeInfo view (properly implemented):**
```
Node 1: AndroidComposeView
  - Node 2: SemanticsNode (role: Button, text: "Login")  ✓
  - Node 3: SemanticsNode (role: Text, text: "Welcome")  ✓
```

**AccessibilityNodeInfo view (poorly implemented - no semantics):**
```
Node 1: AndroidComposeView
  - childCount: 0  ❌
```

**Issue:** Developers must add semantics:
```kotlin
// BAD (no accessibility)
Box(modifier = Modifier.clickable { ... }) {
    Text("Button")
}

// GOOD (accessible)
Button(
    onClick = { ... },
    modifier = Modifier.semantics {
        contentDescription = "Login button"
        role = Role.Button
    }
) {
    Text("Login")
}
```

**Popular Compose Apps:**
- Many new Android apps (2021+)
- Transitioning from Views to Compose

**Exploration Result:** Varies
- Well-implemented: 90%+ coverage ✓
- Poorly implemented: 10-20% coverage ❌

---

## Detection: How to Identify Framework

### Flutter Detection

```kotlin
fun isFlutterApp(rootNode: AccessibilityNodeInfo): Boolean {
    val className = rootNode.className?.toString() ?: ""
    return className.contains("FlutterView") ||
           className.contains("io.flutter.embedding")
}

fun findFlutterView(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    if (isFlutterApp(rootNode)) return rootNode

    // Search descendants
    for (i in 0 until rootNode.childCount) {
        val child = rootNode.getChild(i) ?: continue
        val result = findFlutterView(child)
        if (result != null) return result
    }
    return null
}
```

### React Native Detection

```kotlin
fun isReactNativeApp(rootNode: AccessibilityNodeInfo): Boolean {
    val className = rootNode.className?.toString() ?: ""
    return className.contains("ReactRootView") ||
           className.contains("com.facebook.react")
}
```

### Unity Detection

```kotlin
fun isUnityApp(packageName: String, rootNode: AccessibilityNodeInfo): Boolean {
    // Check for Unity signature
    val hasUnityPlayer = rootNode.className?.toString()?.contains("UnityPlayer") == true

    // Check for GLSurfaceView/SurfaceView with game characteristics
    val isSurfaceView = rootNode.className?.toString()?.contains("SurfaceView") == true
    val hasNoChildren = rootNode.childCount == 0

    return hasUnityPlayer || (isSurfaceView && hasNoChildren)
}
```

### WebView Detection

```kotlin
fun isWebView(node: AccessibilityNodeInfo): Boolean {
    val className = node.className?.toString() ?: ""
    return className.contains("WebView")
}
```

### Compose Detection

```kotlin
fun isComposeApp(rootNode: AccessibilityNodeInfo): Boolean {
    val className = rootNode.className?.toString() ?: ""
    return className.contains("AndroidComposeView")
}
```

---

## Accessibility APIs by Framework

### Flutter Accessibility Support

**Flutter has built-in semantics:**
```dart
// Flutter code (developer side)
Semantics(
  label: 'Search',
  button: true,
  onTap: () { ... },
  child: Icon(Icons.search),
)
```

**Bridges to Android AccessibilityNodeInfo:**
```
Node: SemanticsNode
  - contentDescription: "Search"
  - className: "android.widget.Button"
  - isClickable: true
  - actions: [ACTION_CLICK]
```

**BUT:** Only if developer adds `Semantics` widget!

**Problem:** Many Flutter apps DON'T implement semantics:
- Developers focus on visuals
- Semantics considered "extra work"
- Testing doesn't catch missing semantics

**Our Detection:**
```kotlin
fun hasFlutterSemantics(flutterView: AccessibilityNodeInfo): Boolean {
    // Flutter with semantics: childCount > 0
    // Flutter without semantics: childCount == 0
    return flutterView.childCount > 0
}
```

**Statistics (estimated):**
- 30% of Flutter apps: Good semantics ✓
- 70% of Flutter apps: No/poor semantics ❌

---

### React Native Accessibility Support

**React Native bridges to native:**
```jsx
<View
  accessible={true}
  accessibilityLabel="User profile"
  accessibilityRole="button"
  onPress={...}>
  ...
</View>
```

Creates actual Android View with contentDescription.

**Problem:** Optional - developers must add explicitly

**Statistics (estimated):**
- 50% of RN apps: Good accessibility ✓
- 50% of RN apps: Missing labels ❌

---

### Unity Accessibility Support

**Unity Accessibility API (EXPERIMENTAL):**
```csharp
// Unity code
AccessibilityNode node = new AccessibilityNode(
    label: "Start Button",
    isButton: true,
    onClick: StartGame
);
```

**Android Bridge:** Unity SDK must implement Android AccessibilityService bridge

**Problem:**
- Rarely implemented (games = visual)
- No standard approach
- Requires custom Unity plugin

**Statistics (estimated):**
- 5% of Unity games: Accessibility support
- 95% of Unity games: No accessibility

---

### WebView Accessibility Support

**WebView reads HTML semantics:**
```html
<button aria-label="Search">🔍</button>
<a href="/page">Link</a>
<h1>Title</h1>
```

**Bridges to AccessibilityNodeInfo automatically** IF:
- WebView accessibility enabled
- HTML has proper ARIA labels
- JavaScript doesn't break semantics

**Problem:**
- WebView accessibility often disabled by default
- Web content may lack ARIA labels
- SPAs (React/Vue) may use `<div>` instead of semantic HTML

**Statistics (estimated):**
- 40% of WebView content: Accessible ✓
- 60% of WebView content: Poor accessibility ❌

---

## Solutions by Framework

### Solution 1: Flutter - Semantic Tree Extraction (FEASIBLE)

**Approach:** Use Flutter's semantic tree API

**Flutter exposes semantics to AccessibilityService:**
1. If app has `Semantics` widgets → Android sees children
2. We can traverse like normal Views
3. Same element extraction logic works!

**Detection & Handling:**
```kotlin
fun handleFlutterApp(flutterView: AccessibilityNodeInfo): List<ElementInfo> {
    // Check if semantics available
    if (flutterView.childCount == 0) {
        Log.w(TAG, "Flutter app without semantics - CANNOT EXPLORE")
        return emptyList()
    }

    // Semantics available - treat like normal View hierarchy
    val elements = extractClickableChildren(flutterView)
    Log.i(TAG, "Flutter app with semantics - found ${elements.size} elements")
    return elements
}
```

**Success Rate:**
- Apps WITH semantics: 90%+ coverage ✓
- Apps WITHOUT semantics: 0% coverage (UNSOLVABLE without semantics) ❌

**Recommendation:**
- Log warning for apps without semantics
- Provide user feedback: "This app doesn't support accessibility"

---

### Solution 2: React Native - Standard Approach (ALREADY WORKS)

**Good News:** React Native uses actual Android Views

**Detection & Handling:**
```kotlin
fun handleReactNativeApp(rootNode: AccessibilityNodeInfo): List<ElementInfo> {
    // React Native = normal Android Views
    // Use standard extraction
    return extractClickableChildren(rootNode)
}
```

**Problem:** Poor accessibility labels

**Mitigation:**
- Extract elements even without labels
- Use bounds, className, sibling context as fallback
- Create UUID from visual properties

**Success Rate:** 60-80% coverage (depends on app quality)

---

### Solution 3: Unity/Unreal - Visual Analysis (FUTURE WORK)

**Problem:** No semantic information AT ALL

**Approach 1: Screenshot + Vision AI (FUTURE)**
```kotlin
suspend fun handleUnityGame(surfaceView: AccessibilityNodeInfo): List<ElementInfo> {
    Log.w(TAG, "Unity/Unreal game detected - semantic exploration IMPOSSIBLE")

    // Take screenshot
    val screenshot = takeScreenshot()

    // Use computer vision to detect UI elements
    val detectedElements = visionAI.detectGameUI(screenshot)
    // - Detect buttons by visual patterns
    // - Detect text by OCR
    // - Detect clickable areas by UI conventions

    return detectedElements
}
```

**Challenges:**
- Requires ML model trained on game UIs
- High computational cost
- 60-70% accuracy at best
- Can't detect semantic meaning (what does button do?)

**Approach 2: User Recording (ALTERNATIVE)**
```kotlin
// Record user interactions
// Learn UI locations from user clicks
// Build map of clickable areas
```

**Recommendation:**
- Phase 1: Skip Unity/Unreal games (log warning)
- Phase 2: Implement vision-based detection (future)

---

### Solution 4: WebView - HTML Semantic Extraction (FEASIBLE)

**Approach:** Enable WebView accessibility + extract HTML semantics

**Detection & Handling:**
```kotlin
fun handleWebView(webView: AccessibilityNodeInfo): List<ElementInfo> {
    // Check if WebView has accessibility enabled
    val hasAccessibility = webView.childCount > 0

    if (!hasAccessibility) {
        Log.w(TAG, "WebView without accessibility - limited exploration")
        return emptyList()
    }

    // Extract web elements (buttons, links, inputs)
    val webElements = extractClickableChildren(webView)

    // Filter by role
    val semanticElements = webElements.filter { element ->
        val className = element.className ?: ""
        className.contains("Button") ||
        className.contains("Link") ||
        className.contains("EditText") ||
        element.isClickable
    }

    return semanticElements
}
```

**Success Rate:** 50-70% (depends on web content quality)

---

### Solution 5: Compose - Semantic Modifier Support (ALREADY WORKS)

**Good News:** Compose has excellent accessibility support (when used)

**Detection & Handling:**
```kotlin
fun handleComposeApp(composeView: AccessibilityNodeInfo): List<ElementInfo> {
    // Check if semantics available
    if (composeView.childCount == 0) {
        Log.w(TAG, "Compose app without semantics - CANNOT EXPLORE")
        return emptyList()
    }

    // Semantics available - standard extraction
    return extractClickableChildren(composeView)
}
```

**Success Rate:** 80-95% (better than Flutter because Compose is newer + Android-native)

---

## Implementation Strategy

### Phase 1: Detection (1 hour)

```kotlin
enum class UIFramework {
    NATIVE_VIEWS,      // Standard Android Views ✓
    FLUTTER,           // Flutter with/without semantics
    REACT_NATIVE,      // React Native Views
    UNITY,             // Unity games ❌
    UNREAL,            // Unreal games ❌
    WEBVIEW,           // Web content
    COMPOSE,           // Jetpack Compose
    UNKNOWN
}

fun detectFramework(rootNode: AccessibilityNodeInfo, packageName: String): UIFramework {
    val className = rootNode.className?.toString() ?: ""

    return when {
        className.contains("FlutterView") -> UIFramework.FLUTTER
        className.contains("ReactRootView") -> UIFramework.REACT_NATIVE
        className.contains("UnityPlayer") -> UIFramework.UNITY
        className.contains("SurfaceView") && rootNode.childCount == 0 -> UIFramework.UNITY
        className.contains("WebView") -> UIFramework.WEBVIEW
        className.contains("AndroidComposeView") -> UIFramework.COMPOSE
        else -> UIFramework.NATIVE_VIEWS
    }
}
```

### Phase 2: Framework-Specific Handling (3 hours)

```kotlin
suspend fun exploreWithFrameworkSupport(
    rootNode: AccessibilityNodeInfo,
    packageName: String
): List<ElementInfo> {
    val framework = detectFramework(rootNode, packageName)

    Log.i(TAG, "📱 Detected UI framework: $framework")

    return when (framework) {
        UIFramework.NATIVE_VIEWS -> {
            // Standard exploration (existing code)
            extractClickableChildren(rootNode)
        }

        UIFramework.FLUTTER -> {
            if (rootNode.childCount == 0) {
                Log.w(TAG, "⚠️ Flutter app WITHOUT semantics - cannot explore")
                // Store in database: framework=flutter, semantics=false
                emptyList()
            } else {
                Log.i(TAG, "✓ Flutter app WITH semantics - exploring")
                extractClickableChildren(rootNode)
            }
        }

        UIFramework.REACT_NATIVE -> {
            // Treat like native views
            Log.i(TAG, "✓ React Native - using standard exploration")
            extractClickableChildren(rootNode)
        }

        UIFramework.COMPOSE -> {
            if (rootNode.childCount == 0) {
                Log.w(TAG, "⚠️ Compose app WITHOUT semantics - cannot explore")
                emptyList()
            } else {
                Log.i(TAG, "✓ Compose app WITH semantics - exploring")
                extractClickableChildren(rootNode)
            }
        }

        UIFramework.WEBVIEW -> {
            if (rootNode.childCount == 0) {
                Log.w(TAG, "⚠️ WebView WITHOUT accessibility - limited exploration")
                emptyList()
            } else {
                Log.i(TAG, "✓ WebView WITH accessibility - exploring")
                extractClickableChildren(rootNode)
            }
        }

        UIFramework.UNITY, UIFramework.UNREAL -> {
            Log.w(TAG, "❌ Unity/Unreal game - semantic exploration NOT SUPPORTED")
            Log.w(TAG, "   Consider: Screenshot + Vision AI (future)")
            // Store in database: framework=unity, explorable=false
            emptyList()
        }

        UIFramework.UNKNOWN -> {
            Log.w(TAG, "❓ Unknown framework - attempting standard exploration")
            extractClickableChildren(rootNode)
        }
    }
}
```

### Phase 3: Database Schema Update (30 minutes)

Add framework detection to database:

```sql
ALTER TABLE scraped_element ADD COLUMN ui_framework TEXT;
ALTER TABLE scraped_element ADD COLUMN framework_semantics_available INTEGER DEFAULT 1;

-- Values:
-- ui_framework: 'native', 'flutter', 'react_native', 'unity', 'webview', 'compose'
-- framework_semantics_available: 1 (has semantics), 0 (no semantics)
```

### Phase 4: User Feedback (1 hour)

Display warnings for unsupported frameworks:

```kotlin
fun showFrameworkWarning(framework: UIFramework, hasSemantics: Boolean) {
    val message = when {
        framework == UIFramework.FLUTTER && !hasSemantics ->
            "This Flutter app doesn't support accessibility. Only basic exploration possible."

        framework == UIFramework.UNITY || framework == UIFramework.UNREAL ->
            "This is a game (Unity/Unreal). Visual exploration not yet supported. " +
            "UI elements cannot be detected automatically."

        framework == UIFramework.WEBVIEW && !hasSemantics ->
            "This web content has limited accessibility. Some elements may be missed."

        else -> null
    }

    if (message != null) {
        // Show toast or notification
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
```

---

## Success Rates by Framework

| Framework | Detection | Exploration | Expected Coverage |
|-----------|-----------|-------------|-------------------|
| **Native Views** | 100% | 100% | **90-95%** ✓ |
| **React Native** | 95% | 90% | **70-80%** ✓ |
| **Compose (with semantics)** | 100% | 95% | **85-95%** ✓ |
| **Flutter (with semantics)** | 100% | 90% | **80-90%** ✓ |
| **Flutter (no semantics)** | 100% | 0% | **0%** ❌ |
| **WebView (accessible)** | 95% | 70% | **50-70%** ⚠️ |
| **WebView (not accessible)** | 95% | 0% | **0%** ❌ |
| **Unity/Unreal** | 90% | 0% | **0%** ❌ |

---

## Market Impact Analysis

### App Distribution by Framework (2024 estimates)

| Framework | % of Apps | Top Apps |
|-----------|-----------|----------|
| **Native Views** | 45% | Most enterprise/utility apps |
| **React Native** | 15% | Facebook, Instagram, Discord |
| **Flutter** | 12% | Google Pay, Alibaba, eBay Motors |
| **Compose** | 8% | New Android apps (2021+) |
| **Unity** | 10% | Pokémon GO, Temple Run |
| **Unreal** | 3% | PUBG, Fortnite |
| **WebView** | 5% | Hybrid apps |
| **Other** | 2% | Xamarin, etc. |

### Coverage Impact

**With native-only support (current):**
- 45% of apps: Full coverage
- 55% of apps: Limited/no coverage

**With cross-platform support (Phase 1-4):**
- 80% of apps: Good coverage (Native + RN + Flutter + Compose + WebView)
- 13% of apps: No coverage (Unity + Unreal games)
- 7% of apps: Partial coverage (poorly implemented)

---

## Recommendations

### Immediate (High Priority)

1. **Implement framework detection** (1 hour)
   - Detect Flutter, React Native, Unity, Compose, WebView
   - Log framework type to database

2. **Add semantics checks** (1 hour)
   - Flutter: Check childCount > 0
   - Compose: Check childCount > 0
   - WebView: Check childCount > 0

3. **User feedback** (1 hour)
   - Warn when exploring app without semantics
   - Display framework type in UI

### Medium Priority

4. **Enhanced WebView support** (2 hours)
   - Better HTML semantic extraction
   - Handle SPAs (single-page apps)

5. **React Native optimization** (2 hours)
   - Fallback strategies for elements without labels
   - Use visual properties for UUID generation

### Future Work

6. **Unity/Unreal vision AI** (40+ hours)
   - Screenshot-based UI detection
   - ML model training
   - Game UI pattern recognition

7. **User-assisted learning** (10 hours)
   - Record user interactions in games
   - Build clickable area map from user behavior

---

## Testing Plan

### Framework Detection Tests

```kotlin
@Test
fun testFlutterDetection() {
    val mockNode = createMockNode(className = "io.flutter.embedding.android.FlutterView")
    val framework = detectFramework(mockNode, "com.example.app")
    assertEquals(UIFramework.FLUTTER, framework)
}

@Test
fun testReactNativeDetection() {
    val mockNode = createMockNode(className = "com.facebook.react.ReactRootView")
    val framework = detectFramework(mockNode, "com.example.app")
    assertEquals(UIFramework.REACT_NATIVE, framework)
}
```

### Semantic Availability Tests

```kotlin
@Test
fun testFlutterWithSemantics() {
    val mockNode = createMockNode(
        className = "FlutterView",
        childCount = 10
    )
    val elements = handleFlutterApp(mockNode)
    assertTrue(elements.isNotEmpty())
}

@Test
fun testFlutterWithoutSemantics() {
    val mockNode = createMockNode(
        className = "FlutterView",
        childCount = 0
    )
    val elements = handleFlutterApp(mockNode)
    assertTrue(elements.isEmpty())
}
```

### Integration Tests

1. **Test with Flutter app** (Google Pay)
   - Verify framework detection
   - Check semantic availability
   - Measure coverage

2. **Test with React Native app** (Discord)
   - Verify standard exploration works
   - Measure coverage

3. **Test with Unity game** (Temple Run)
   - Verify detection
   - Confirm 0 elements found
   - Verify user warning displayed

---

## Files to Modify

### New Files

1. **UIFrameworkDetector.kt**
   - Framework detection logic
   - Semantic availability checks

2. **CrossPlatformHandler.kt**
   - Framework-specific exploration strategies
   - Fallback handling

### Modified Files

3. **ExplorationEngine.kt**
   - Add framework detection before exploration
   - Route to appropriate handler

4. **Database Schema**
   - Add ui_framework column
   - Add framework_semantics_available column

5. **UI (LearnApp)**
   - Display framework warnings
   - Show framework type in exploration results

---

## Next Steps

1. ⏳ Implement Phase 1-3 (scrollable + hidden UI)
2. ⏳ Implement framework detection (4 hours)
3. ⏳ Test with popular apps across frameworks
4. ⏳ Add user warnings for unsupported frameworks
5. ⏳ Document limitations in user guide

---

**Version:** 1.0
**Status:** CRITICAL ANALYSIS COMPLETE
**Priority:** HIGH - Affects 55% of modern apps
**Recommendation:** Implement detection + handling immediately after hidden UI patterns

**Key Insight:** We can achieve 80% app coverage with proper framework support. Unity/Unreal games (13%) require vision AI (future work).

---

**Related Documents:**
- learnapp-scrollable-content-fix-plan-251204.md (Phase 1-3)
- learnapp-hidden-ui-patterns-analysis-251204.md (Phase 4-5)
- This document (Phase 6: Cross-platform frameworks)

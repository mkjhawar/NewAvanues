# Android Accessibility Service API Research for LearnApp

**Module:** LearnApp
**Purpose:** Automated UI exploration using Android Accessibility Service
**Date:** 2025-10-08
**Status:** Research & Technical Reference

---

## Table of Contents

1. [Overview](#overview)
2. [Core Accessibility APIs](#core-accessibility-apis)
3. [Performing Actions](#performing-actions)
4. [Screen State Detection](#screen-state-detection)
5. [Element Classification](#element-classification)
6. [Safety Patterns](#safety-patterns)
7. [Kotlin Best Practices](#kotlin-best-practices)
8. [Complete Code Examples](#complete-code-examples)
9. [Memory Management](#memory-management)
10. [Testing & Debugging](#testing--debugging)
11. [References](#references)

---

## Overview

### What is AccessibilityService?

Android's `AccessibilityService` is a system service that allows apps to observe and interact with the UI on behalf of users. Originally designed for accessibility features (screen readers, switch access), it provides powerful APIs for:

- Monitoring app launches and screen changes
- Traversing the entire UI hierarchy
- Performing actions (click, scroll, type)
- Extracting UI element properties

### LearnApp Use Case

LearnApp uses AccessibilityService to:
1. Detect when new apps are launched (`TYPE_WINDOW_STATE_CHANGED`)
2. Request user permission to explore the app
3. Systematically traverse all UI elements
4. Build a navigation tree with screen fingerprints
5. Generate UUIDs and voice command aliases
6. Store mappings for voice control

---

## Core Accessibility APIs

### 1. AccessibilityService Lifecycle

#### Service Declaration (AndroidManifest.xml)

```xml
<service
    android:name=".services.LearnAppAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

#### Service Configuration (res/xml/accessibility_service_config.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="" />
```

**Key Configuration Options:**

- `accessibilityEventTypes`: Which events to receive
  - `typeWindowStateChanged`: New window/screen appears
  - `typeWindowContentChanged`: Content changes within window
  - `typeViewClicked`: Element clicked
  - `typeViewFocused`: Element focused

- `accessibilityFlags`:
  - `flagReportViewIds`: Include view resource IDs
  - `flagRetrieveInteractiveWindows`: Access window hierarchy
  - `flagRequestTouchExplorationMode`: Enable touch exploration

- `canRetrieveWindowContent`: **Must be true** to access UI tree
- `packageNames`: Empty string = monitor all apps

#### Service Implementation

```kotlin
class LearnAppAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure service at runtime
        val info = AccessibilityServiceInfo().apply {
            // Event types we want to receive
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED

            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Flags
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            // We can retrieve window content
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            // No specific package filter (monitor all)
            packageNames = null
        }

        serviceInfo = info

        Log.d(TAG, "LearnApp AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle accessibility events
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
        }
    }

    override fun onInterrupt() {
        // Called when service is interrupted
        Log.w(TAG, "LearnApp AccessibilityService interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "LearnApp AccessibilityService unbound")
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "LearnAppAccessibility"
    }
}
```

### 2. AccessibilityEvent Types

#### TYPE_WINDOW_STATE_CHANGED

**When fired:** A new window/screen appears (most useful for detecting app launches)

```kotlin
private fun handleWindowStateChanged(event: AccessibilityEvent) {
    val packageName = event.packageName?.toString() ?: return
    val className = event.className?.toString() ?: return

    Log.d(TAG, "Window changed: $packageName/$className")

    // Check if this is a new app we should learn
    if (shouldLearnApp(packageName)) {
        // Show permission dialog
        requestLearningPermission(packageName, className)
    }
}

private fun shouldLearnApp(packageName: String): Boolean {
    // Ignore system apps and our own app
    val systemPackages = setOf(
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.augmentalis.vos4" // Our app
    )

    return packageName !in systemPackages
}
```

#### TYPE_WINDOW_CONTENT_CHANGED

**When fired:** Content within the current window changes

```kotlin
private fun handleWindowContentChanged(event: AccessibilityEvent) {
    // Content changed - useful for detecting when exploration action completes
    // Be careful: this fires VERY frequently

    if (isCurrentlyExploring) {
        // Debounce: wait for content to stabilize
        contentChangeHandler.removeCallbacksAndMessages(null)
        contentChangeHandler.postDelayed({
            continueExploration()
        }, CONTENT_STABILIZATION_DELAY)
    }
}

companion object {
    private const val CONTENT_STABILIZATION_DELAY = 500L // ms
}
```

### 3. AccessibilityNodeInfo Tree Traversal

#### Getting the Root Node

```kotlin
/**
 * Get the root node of the currently active window.
 *
 * CRITICAL: Must call recycle() when done to prevent memory leaks!
 */
private fun getRootNode(): AccessibilityNodeInfo? {
    return rootInActiveWindow
}
```

#### Recursive Tree Traversal

```kotlin
/**
 * Traverse entire UI tree depth-first, visiting all nodes.
 *
 * @param node Current node to process
 * @param depth Current depth in tree (for logging)
 * @param visitor Callback to process each node
 */
private fun traverseTree(
    node: AccessibilityNodeInfo?,
    depth: Int = 0,
    visitor: (AccessibilityNodeInfo, Int) -> Unit
) {
    if (node == null) return

    try {
        // Visit current node
        visitor(node, depth)

        // Recursively visit children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseTree(child, depth + 1, visitor)
            child?.recycle() // CRITICAL: Recycle child nodes
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error traversing tree at depth $depth", e)
    }
}

/**
 * Usage example: Find all clickable elements
 */
private fun findClickableElements(): List<ElementInfo> {
    val rootNode = getRootNode() ?: return emptyList()
    val clickableElements = mutableListOf<ElementInfo>()

    try {
        traverseTree(rootNode) { node, depth ->
            if (node.isClickable) {
                clickableElements.add(ElementInfo.fromNode(node, depth))
            }
        }
    } finally {
        rootNode.recycle() // CRITICAL: Recycle root node
    }

    return clickableElements
}
```

#### Breadth-First Traversal (Alternative)

```kotlin
/**
 * Breadth-first traversal - useful for exploring by layers
 */
private fun traverseTreeBFS(
    rootNode: AccessibilityNodeInfo,
    visitor: (AccessibilityNodeInfo, Int) -> Unit
) {
    val queue = LinkedList<Pair<AccessibilityNodeInfo, Int>>()
    queue.add(rootNode to 0)

    while (queue.isNotEmpty()) {
        val (node, depth) = queue.poll()

        try {
            visitor(node, depth)

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                queue.add(child to depth + 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in BFS at depth $depth", e)
        }
    }
}
```

#### Node Recycling Best Practices

```kotlin
/**
 * CRITICAL MEMORY MANAGEMENT PATTERN
 *
 * AccessibilityNodeInfo instances are backed by native memory.
 * Failure to recycle causes memory leaks and eventual crashes.
 */

// ✅ CORRECT: Use try-finally
fun processNode() {
    val node = getRootNode()
    try {
        // Use node
        val text = node?.text
    } finally {
        node?.recycle()
    }
}

// ✅ CORRECT: Use with extension function
inline fun <T> AccessibilityNodeInfo.use(block: (AccessibilityNodeInfo) -> T): T {
    try {
        return block(this)
    } finally {
        recycle()
    }
}

// Usage
getRootNode()?.use { node ->
    // Use node safely
    val text = node.text
}

// ❌ WRONG: Forgetting to recycle
fun badExample() {
    val node = getRootNode()
    val text = node?.text
    // Missing recycle() - MEMORY LEAK!
}

// ❌ WRONG: Recycling too early
fun anotherBadExample() {
    val node = getRootNode()
    node?.recycle()
    val text = node?.text // Crashes! Node already recycled
}
```

---

## Performing Actions

### 1. Click Actions

#### Basic Click

```kotlin
/**
 * Perform click action on a node.
 *
 * @return true if action was successfully dispatched
 */
private fun clickNode(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
}

/**
 * Click element with retry logic
 */
private suspend fun clickNodeWithRetry(
    node: AccessibilityNodeInfo,
    maxRetries: Int = 3
): Boolean {
    repeat(maxRetries) { attempt ->
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d(TAG, "Click succeeded on attempt ${attempt + 1}")

            // Wait for screen to transition
            delay(SCREEN_TRANSITION_DELAY)
            return true
        }

        Log.w(TAG, "Click failed, attempt ${attempt + 1}/$maxRetries")
        delay(100)
    }

    return false
}

companion object {
    private const val SCREEN_TRANSITION_DELAY = 1000L // ms
}
```

#### Click by Coordinates

```kotlin
/**
 * For elements that don't respond to ACTION_CLICK,
 * use gesture dispatch (requires Android 7.0+)
 */
@RequiresApi(Build.VERSION_CODES.N)
private fun clickAtCoordinates(x: Float, y: Float): Boolean {
    val path = Path().apply {
        moveTo(x, y)
    }

    val gesture = GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        .build()

    return dispatchGesture(
        gesture,
        object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Gesture completed")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Gesture cancelled")
            }
        },
        null
    )
}

/**
 * Click node by getting its screen coordinates
 */
@RequiresApi(Build.VERSION_CODES.N)
private fun clickNodeByCoordinates(node: AccessibilityNodeInfo): Boolean {
    val rect = Rect()
    node.getBoundsInScreen(rect)

    val centerX = rect.centerX().toFloat()
    val centerY = rect.centerY().toFloat()

    return clickAtCoordinates(centerX, centerY)
}
```

### 2. Scroll Actions

#### Basic Scrolling

```kotlin
/**
 * Scroll forward (down for vertical, right for horizontal)
 */
private fun scrollForward(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
}

/**
 * Scroll backward (up for vertical, left for horizontal)
 */
private fun scrollBackward(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
}

/**
 * Check if node is scrollable
 */
private fun isScrollable(node: AccessibilityNodeInfo): Boolean {
    return node.isScrollable ||
           node.actionList.any {
               it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
               it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
           }
}
```

#### Exhaustive Scrolling

```kotlin
/**
 * Scroll through entire scrollable container to reveal all elements.
 * Uses screen fingerprinting to detect when we've reached the end.
 */
private suspend fun exhaustiveScroll(
    scrollableNode: AccessibilityNodeInfo
): List<String> {
    val seenFingerprints = mutableSetOf<String>()
    val allFingerprints = mutableListOf<String>()
    var consecutiveDuplicates = 0

    while (consecutiveDuplicates < 3) {
        // Capture current screen state
        val fingerprint = getScreenFingerprint()

        if (fingerprint in seenFingerprints) {
            consecutiveDuplicates++
        } else {
            consecutiveDuplicates = 0
            seenFingerprints.add(fingerprint)
            allFingerprints.add(fingerprint)
        }

        // Try to scroll
        if (!scrollForward(scrollableNode)) {
            Log.d(TAG, "Cannot scroll further")
            break
        }

        // Wait for scroll to complete
        delay(SCROLL_DELAY)
    }

    Log.d(TAG, "Exhaustive scroll found ${allFingerprints.size} unique screens")
    return allFingerprints
}

companion object {
    private const val SCROLL_DELAY = 500L // ms
}
```

### 3. Global Actions

#### Back Button

```kotlin
/**
 * Perform global back action (equivalent to pressing Back button)
 */
private fun performBack(): Boolean {
    return performGlobalAction(GLOBAL_ACTION_BACK)
}
```

#### Home Button

```kotlin
/**
 * Perform global home action (equivalent to pressing Home button)
 */
private fun performHome(): Boolean {
    return performGlobalAction(GLOBAL_ACTION_HOME)
}
```

#### Other Global Actions

```kotlin
/**
 * Show notifications
 */
private fun showNotifications(): Boolean {
    return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
}

/**
 * Show recent apps
 */
private fun showRecents(): Boolean {
    return performGlobalAction(GLOBAL_ACTION_RECENTS)
}

/**
 * Show quick settings
 */
private fun showQuickSettings(): Boolean {
    return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
}
```

### 4. Text Input Actions

#### Set Text

```kotlin
/**
 * Set text in an EditText field (Android 5.0+)
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
    val arguments = Bundle().apply {
        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    }

    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
}
```

#### Focus and Type

```kotlin
/**
 * Focus an EditText and type text using IME
 */
private suspend fun focusAndType(node: AccessibilityNodeInfo, text: String): Boolean {
    // First, focus the field
    if (!node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
        return false
    }

    delay(100) // Wait for focus

    // Then set text
    return setText(node, text)
}
```

### 5. Waiting for Screen Transitions

#### Simple Delay

```kotlin
/**
 * Simple delay after action (least reliable)
 */
private suspend fun clickAndWait(node: AccessibilityNodeInfo) {
    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    delay(1000) // Fixed delay
}
```

#### Fingerprint-Based Detection

```kotlin
/**
 * Wait until screen fingerprint changes (more reliable)
 */
private suspend fun clickAndWaitForTransition(
    node: AccessibilityNodeInfo,
    timeout: Long = 5000L
): Boolean {
    val initialFingerprint = getScreenFingerprint()
    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    val startTime = System.currentTimeMillis()

    while (System.currentTimeMillis() - startTime < timeout) {
        delay(100)

        val currentFingerprint = getScreenFingerprint()
        if (currentFingerprint != initialFingerprint) {
            Log.d(TAG, "Screen transition detected")
            return true
        }
    }

    Log.w(TAG, "Screen transition timeout")
    return false
}
```

#### Event-Based Detection

```kotlin
/**
 * Wait for WINDOW_STATE_CHANGED event (most reliable)
 */
private class TransitionWaiter {
    private val transitionLatch = CountDownLatch(1)

    fun onWindowStateChanged() {
        transitionLatch.countDown()
    }

    suspend fun waitForTransition(timeout: Long = 5000L): Boolean {
        return withContext(Dispatchers.IO) {
            transitionLatch.await(timeout, TimeUnit.MILLISECONDS)
        }
    }
}

private suspend fun clickAndWaitForEvent(
    node: AccessibilityNodeInfo
): Boolean {
    val waiter = TransitionWaiter()
    // Register waiter to be notified on window state change

    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    return waiter.waitForTransition()
}
```

---

## Screen State Detection

### 1. Building Screen Fingerprints

#### SHA-256 Hash-Based Fingerprint

```kotlin
import java.security.MessageDigest

/**
 * Generate unique fingerprint for current screen state.
 * Uses SHA-256 hash of all visible text and structure.
 */
private fun getScreenFingerprint(): String {
    val rootNode = getRootNode() ?: return ""

    try {
        val elements = mutableListOf<String>()

        traverseTree(rootNode) { node, depth ->
            // Include text content
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            val className = node.className?.toString() ?: ""

            // Build element signature
            val elementSignature = "$className|$resourceId|$text|$contentDesc"
            elements.add(elementSignature)
        }

        // Hash all elements together
        val combined = elements.joinToString("\n")
        return sha256(combined)
    } finally {
        rootNode.recycle()
    }
}

/**
 * Compute SHA-256 hash
 */
private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
```

#### Lightweight Fingerprint (Faster)

```kotlin
/**
 * Lightweight fingerprint using visible text only.
 * Faster but less precise than full hash.
 */
private fun getLightweightFingerprint(): String {
    val rootNode = getRootNode() ?: return ""

    try {
        val texts = mutableListOf<String>()

        traverseTree(rootNode) { node, _ ->
            // Only include non-empty visible text
            if (node.isVisibleToUser) {
                node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
                    texts.add(it)
                }
            }
        }

        return texts.sorted().joinToString("|")
    } finally {
        rootNode.recycle()
    }
}
```

### 2. Getting Package and Activity Names

#### From AccessibilityEvent

```kotlin
/**
 * Extract package and activity from window state changed event
 */
private fun getActivityInfo(event: AccessibilityEvent): Pair<String, String>? {
    val packageName = event.packageName?.toString() ?: return null
    val className = event.className?.toString() ?: return null

    return packageName to className
}
```

#### From Root Node

```kotlin
/**
 * Get current package and activity from root node
 */
private fun getCurrentActivityInfo(): Pair<String, String>? {
    val rootNode = getRootNode() ?: return null

    try {
        val packageName = rootNode.packageName?.toString() ?: return null
        val className = rootNode.className?.toString() ?: return null

        return packageName to className
    } finally {
        rootNode.recycle()
    }
}
```

#### Using ActivityManager (Requires Permission)

```kotlin
/**
 * Get current activity using ActivityManager.
 * Requires GET_TASKS permission (deprecated) or USAGE_STATS.
 */
@Suppress("DEPRECATION")
private fun getCurrentActivity(): String? {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val tasks = activityManager.getRunningTasks(1)

    return tasks.firstOrNull()?.topActivity?.className
}
```

### 3. Detecting Screen Changes

#### Polling-Based Detection

```kotlin
/**
 * Monitor for screen changes using periodic fingerprint comparison
 */
private class ScreenChangeMonitor(
    private val service: AccessibilityService,
    private val onScreenChanged: (String) -> Unit
) {
    private var lastFingerprint: String = ""
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun start() {
        scope.launch {
            while (isActive) {
                val currentFingerprint = service.getScreenFingerprint()

                if (currentFingerprint != lastFingerprint) {
                    onScreenChanged(currentFingerprint)
                    lastFingerprint = currentFingerprint
                }

                delay(500) // Check every 500ms
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
```

#### Event-Based Detection (Preferred)

```kotlin
/**
 * React to screen changes via accessibility events
 */
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
            // New screen/dialog appeared
            val fingerprint = getScreenFingerprint()
            handleScreenChange(fingerprint)
        }

        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
            // Content within screen changed
            // Debounce to avoid excessive calls
            debounceContentChange()
        }
    }
}

private val contentChangeHandler = Handler(Looper.getMainLooper())

private fun debounceContentChange() {
    contentChangeHandler.removeCallbacksAndMessages(null)
    contentChangeHandler.postDelayed({
        val fingerprint = getScreenFingerprint()
        handleContentChange(fingerprint)
    }, 500)
}
```

---

## Element Classification

### 1. Clickable Elements

```kotlin
/**
 * Detect if element is clickable (multiple checks for reliability)
 */
private fun isClickable(node: AccessibilityNodeInfo): Boolean {
    // Check clickable flag
    if (node.isClickable) return true

    // Check if it has click action
    if (node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
        return true
    }

    // Check class name (some elements don't set clickable flag)
    val className = node.className?.toString()
    val clickableClasses = setOf(
        "android.widget.Button",
        "android.widget.ImageButton",
        "android.widget.CheckBox",
        "android.widget.RadioButton",
        "android.widget.ToggleButton",
        "android.widget.Switch",
        "androidx.appcompat.widget.AppCompatButton"
    )

    return className in clickableClasses
}

/**
 * Find all clickable elements on screen
 */
private fun findClickableElements(): List<AccessibilityNodeInfo> {
    val rootNode = getRootNode() ?: return emptyList()
    val clickableNodes = mutableListOf<AccessibilityNodeInfo>()

    try {
        traverseTree(rootNode) { node, _ ->
            if (isClickable(node) && node.isVisibleToUser) {
                // Clone node for later use (don't store original)
                clickableNodes.add(AccessibilityNodeInfo.obtain(node))
            }
        }
    } finally {
        rootNode.recycle()
    }

    return clickableNodes
}
```

### 2. Scrollable Containers

```kotlin
/**
 * Detect scrollable containers
 */
private fun isScrollableContainer(node: AccessibilityNodeInfo): Boolean {
    // Direct scrollable flag
    if (node.isScrollable) return true

    // Has scroll actions
    if (node.actionList.any {
        it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
        it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
    }) {
        return true
    }

    // Common scrollable classes
    val className = node.className?.toString()
    val scrollableClasses = setOf(
        "android.widget.ScrollView",
        "android.widget.HorizontalScrollView",
        "android.widget.ListView",
        "android.widget.GridView",
        "androidx.recyclerview.widget.RecyclerView",
        "androidx.core.widget.NestedScrollView"
    )

    return className in scrollableClasses
}

/**
 * Find all scrollable containers on screen
 */
private fun findScrollableContainers(): List<AccessibilityNodeInfo> {
    val rootNode = getRootNode() ?: return emptyList()
    val scrollableNodes = mutableListOf<AccessibilityNodeInfo>()

    try {
        traverseTree(rootNode) { node, _ ->
            if (isScrollableContainer(node) && node.isVisibleToUser) {
                scrollableNodes.add(AccessibilityNodeInfo.obtain(node))
            }
        }
    } finally {
        rootNode.recycle()
    }

    return scrollableNodes
}
```

### 3. EditText Fields

```kotlin
/**
 * Detect text input fields
 */
private fun isEditText(node: AccessibilityNodeInfo): Boolean {
    // Check if editable
    if (node.isEditable) return true

    // Check class name
    val className = node.className?.toString()
    val editTextClasses = setOf(
        "android.widget.EditText",
        "android.widget.AutoCompleteTextView",
        "android.widget.MultiAutoCompleteTextView",
        "androidx.appcompat.widget.AppCompatEditText"
    )

    return className in editTextClasses
}

/**
 * Detect password fields
 */
private fun isPasswordField(node: AccessibilityNodeInfo): Boolean {
    if (!isEditText(node)) return false

    // Check if input type is password
    val inputType = node.inputType
    val isPassword = (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                    (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                    (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0

    if (isPassword) return true

    // Check hints and text
    val hint = node.hintText?.toString()?.lowercase() ?: ""
    val text = node.text?.toString()?.lowercase() ?: ""
    val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

    val passwordKeywords = setOf("password", "passcode", "pin")

    return passwordKeywords.any {
        it in hint || it in text || it in contentDesc
    }
}
```

### 4. Element Property Extraction

```kotlin
/**
 * Data class representing UI element properties
 */
data class ElementInfo(
    val text: String,
    val contentDescription: String,
    val resourceId: String,
    val className: String,
    val packageName: String,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isPassword: Boolean,
    val bounds: Rect,
    val depth: Int
) {
    companion object {
        fun fromNode(node: AccessibilityNodeInfo, depth: Int): ElementInfo {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            return ElementInfo(
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                resourceId = node.viewIdResourceName ?: "",
                className = node.className?.toString() ?: "",
                packageName = node.packageName?.toString() ?: "",
                isClickable = node.isClickable,
                isScrollable = node.isScrollable,
                isEditable = node.isEditable,
                isPassword = isPasswordField(node),
                bounds = bounds,
                depth = depth
            )
        }
    }
}

/**
 * Extract all element properties from screen
 */
private fun extractAllElements(): List<ElementInfo> {
    val rootNode = getRootNode() ?: return emptyList()
    val elements = mutableListOf<ElementInfo>()

    try {
        traverseTree(rootNode) { node, depth ->
            if (node.isVisibleToUser) {
                elements.add(ElementInfo.fromNode(node, depth))
            }
        }
    } finally {
        rootNode.recycle()
    }

    return elements
}
```

---

## Safety Patterns

### 1. Dangerous Element Detection

#### Text-Based Detection

```kotlin
/**
 * Detect dangerous actions based on text content
 */
private fun isDangerousElement(node: AccessibilityNodeInfo): Boolean {
    val text = node.text?.toString()?.lowercase() ?: ""
    val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
    val resourceId = node.viewIdResourceName?.lowercase() ?: ""

    val combined = "$text $contentDesc $resourceId"

    // Dangerous action patterns
    val dangerousPatterns = listOf(
        // Account management
        Regex("delete\\s+account"),
        Regex("close\\s+account"),
        Regex("deactivate\\s+account"),
        Regex("remove\\s+account"),

        // Authentication
        Regex("sign\\s+out"),
        Regex("log\\s+out"),
        Regex("logout"),

        // Purchases
        Regex("buy\\s+now"),
        Regex("purchase"),
        Regex("pay\\s+now"),
        Regex("subscribe"),
        Regex("upgrade\\s+to\\s+premium"),

        // Destructive actions
        Regex("delete\\s+all"),
        Regex("clear\\s+data"),
        Regex("factory\\s+reset"),
        Regex("uninstall"),

        // Permissions
        Regex("allow\\s+notifications"),
        Regex("enable\\s+location"),
        Regex("grant\\s+permission")
    )

    return dangerousPatterns.any { it.containsMatchIn(combined) }
}
```

#### Class-Based Detection

```kotlin
/**
 * Detect dangerous elements by class or context
 */
private fun isDangerousContext(node: AccessibilityNodeInfo): Boolean {
    val rootNode = getRootNode() ?: return false

    try {
        var isDangerous = false

        traverseTree(rootNode) { currentNode, _ ->
            val text = currentNode.text?.toString()?.lowercase() ?: ""

            // Check for payment context
            if ("payment" in text || "credit card" in text || "billing" in text) {
                isDangerous = true
            }

            // Check for settings context
            if ("settings" in text && "delete" in text) {
                isDangerous = true
            }
        }

        return isDangerous
    } finally {
        rootNode.recycle()
    }
}
```

### 2. Login Screen Detection

```kotlin
/**
 * Detect if current screen is a login/signup screen
 */
private fun isLoginScreen(): Boolean {
    val rootNode = getRootNode() ?: return false

    try {
        var hasPasswordField = false
        var hasLoginButton = false
        var hasUsernameField = false

        traverseTree(rootNode) { node, _ ->
            // Check for password field
            if (isPasswordField(node)) {
                hasPasswordField = true
            }

            // Check for username/email field
            val hint = node.hintText?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            if ("username" in hint || "email" in hint ||
                "username" in text || "email" in text) {
                hasUsernameField = true
            }

            // Check for login button
            val buttonText = node.text?.toString()?.lowercase() ?: ""
            if (node.isClickable &&
                ("login" in buttonText || "sign in" in buttonText)) {
                hasLoginButton = true
            }
        }

        // Login screen has password + (username or login button)
        return hasPasswordField && (hasUsernameField || hasLoginButton)
    } finally {
        rootNode.recycle()
    }
}
```

### 3. Safe Exploration Strategy

```kotlin
/**
 * Determine if element is safe to click during exploration
 */
private fun isSafeToClick(node: AccessibilityNodeInfo): Boolean {
    // Check if dangerous
    if (isDangerousElement(node)) {
        Log.w(TAG, "Skipping dangerous element: ${node.text}")
        return false
    }

    // Check if on login screen
    if (isLoginScreen()) {
        Log.w(TAG, "Skipping element on login screen")
        return false
    }

    // Check if in payment context
    if (isDangerousContext(node)) {
        Log.w(TAG, "Skipping element in dangerous context")
        return false
    }

    // Must be visible and clickable
    if (!node.isVisibleToUser || !isClickable(node)) {
        return false
    }

    return true
}

/**
 * Filter elements for safe exploration
 */
private fun getSafeClickableElements(): List<AccessibilityNodeInfo> {
    return findClickableElements().filter { isSafeToClick(it) }
}
```

### 4. Blacklist Management

```kotlin
/**
 * Maintain blacklist of elements to never click
 */
private class ElementBlacklist {
    private val blacklistedIds = mutableSetOf<String>()
    private val blacklistedTexts = mutableSetOf<String>()

    fun addById(resourceId: String) {
        blacklistedIds.add(resourceId)
    }

    fun addByText(text: String) {
        blacklistedTexts.add(text.lowercase())
    }

    fun isBlacklisted(node: AccessibilityNodeInfo): Boolean {
        // Check resource ID
        val resourceId = node.viewIdResourceName
        if (resourceId in blacklistedIds) {
            return true
        }

        // Check text
        val text = node.text?.toString()?.lowercase()
        if (text in blacklistedTexts) {
            return true
        }

        return false
    }

    companion object {
        fun createDefault(): ElementBlacklist {
            return ElementBlacklist().apply {
                // Common dangerous elements
                addByText("sign out")
                addByText("log out")
                addByText("delete account")
                addByText("buy now")
                addByText("purchase")

                // Common resource IDs to avoid
                addById("com.android.settings:id/delete_button")
            }
        }
    }
}
```

---

## Kotlin Best Practices

### 1. Coroutines for Async Exploration

#### Structured Concurrency

```kotlin
/**
 * Exploration engine using coroutines
 */
class ExplorationEngine(
    private val service: AccessibilityService
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("ExplorationEngine")
    )

    fun startExploration(packageName: String) {
        scope.launch {
            try {
                exploreApp(packageName)
            } catch (e: CancellationException) {
                Log.d(TAG, "Exploration cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exploration error", e)
            }
        }
    }

    fun stopExploration() {
        scope.coroutineContext.cancelChildren()
    }

    fun shutdown() {
        scope.cancel()
    }

    private suspend fun exploreApp(packageName: String) {
        // Exploration logic
        withContext(Dispatchers.Main) {
            // UI interactions must happen on main thread
            exploreCurrentScreen()
        }
    }
}
```

#### Suspending Functions

```kotlin
/**
 * Suspending function for screen exploration
 */
private suspend fun exploreCurrentScreen(): List<ElementInfo> = withContext(Dispatchers.Main) {
    val elements = mutableListOf<ElementInfo>()

    // Get all safe clickable elements
    val clickableElements = getSafeClickableElements()

    for (element in clickableElements) {
        try {
            // Click and wait for transition
            val transitioned = clickAndWaitForTransition(element)

            if (transitioned) {
                // Recursively explore new screen
                val childElements = exploreCurrentScreen()
                elements.addAll(childElements)

                // Navigate back
                performBack()
                delay(500)
            }

            // Record element
            elements.add(ElementInfo.fromNode(element, 0))
        } finally {
            element.recycle()
        }
    }

    elements
}
```

### 2. Flow for Progress Updates

```kotlin
/**
 * Emit exploration progress using Flow
 */
class ExplorationEngine(
    private val service: AccessibilityService
) {
    private val _progressFlow = MutableStateFlow<ExplorationProgress>(ExplorationProgress.Idle)
    val progressFlow: StateFlow<ExplorationProgress> = _progressFlow.asStateFlow()

    sealed class ExplorationProgress {
        object Idle : ExplorationProgress()
        data class Exploring(
            val currentScreen: String,
            val elementsFound: Int,
            val depth: Int
        ) : ExplorationProgress()
        data class Complete(val totalElements: Int) : ExplorationProgress()
        data class Error(val message: String) : ExplorationProgress()
    }

    private suspend fun exploreApp(packageName: String) {
        _progressFlow.value = ExplorationProgress.Exploring(
            currentScreen = packageName,
            elementsFound = 0,
            depth = 0
        )

        try {
            val elements = exploreRecursively(0)
            _progressFlow.value = ExplorationProgress.Complete(elements.size)
        } catch (e: Exception) {
            _progressFlow.value = ExplorationProgress.Error(e.message ?: "Unknown error")
        }
    }
}

/**
 * Collect progress in UI
 */
class ExplorationViewModel(
    private val engine: ExplorationEngine
) : ViewModel() {

    val progressState = engine.progressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExplorationEngine.ExplorationProgress.Idle
        )
}
```

### 3. Sealed Classes for State Management

```kotlin
/**
 * Sealed class for exploration state
 */
sealed class ExplorationState {
    object Idle : ExplorationState()

    data class RequestingPermission(
        val packageName: String,
        val appName: String
    ) : ExplorationState()

    data class Exploring(
        val packageName: String,
        val currentDepth: Int,
        val totalElements: Int,
        val visitedScreens: Set<String>
    ) : ExplorationState()

    data class Paused(
        val reason: String
    ) : ExplorationState()

    data class Complete(
        val packageName: String,
        val navigationTree: NavigationTree,
        val totalElements: Int,
        val duration: Long
    ) : ExplorationState()

    data class Failed(
        val error: ExplorationError
    ) : ExplorationState()
}

/**
 * Sealed class for errors
 */
sealed class ExplorationError {
    object ServiceNotEnabled : ExplorationError()
    object PermissionDenied : ExplorationError()
    data class AppCrashed(val packageName: String) : ExplorationError()
    data class Timeout(val lastScreen: String) : ExplorationError()
    data class Unknown(val throwable: Throwable) : ExplorationError()
}

/**
 * State machine using sealed classes
 */
class ExplorationStateMachine {
    private val _state = MutableStateFlow<ExplorationState>(ExplorationState.Idle)
    val state: StateFlow<ExplorationState> = _state.asStateFlow()

    fun requestPermission(packageName: String, appName: String) {
        _state.value = ExplorationState.RequestingPermission(packageName, appName)
    }

    fun startExploration(packageName: String) {
        val currentState = _state.value
        if (currentState is ExplorationState.RequestingPermission) {
            _state.value = ExplorationState.Exploring(
                packageName = packageName,
                currentDepth = 0,
                totalElements = 0,
                visitedScreens = emptySet()
            )
        }
    }

    fun updateProgress(depth: Int, totalElements: Int, visitedScreens: Set<String>) {
        val currentState = _state.value
        if (currentState is ExplorationState.Exploring) {
            _state.value = currentState.copy(
                currentDepth = depth,
                totalElements = totalElements,
                visitedScreens = visitedScreens
            )
        }
    }

    fun complete(navigationTree: NavigationTree, duration: Long) {
        val currentState = _state.value
        if (currentState is ExplorationState.Exploring) {
            _state.value = ExplorationState.Complete(
                packageName = currentState.packageName,
                navigationTree = navigationTree,
                totalElements = currentState.totalElements,
                duration = duration
            )
        }
    }

    fun fail(error: ExplorationError) {
        _state.value = ExplorationState.Failed(error)
    }
}
```

### 4. Memory-Efficient Data Structures

#### Navigation Tree

```kotlin
/**
 * Memory-efficient navigation tree using lazy initialization
 */
data class NavigationNode(
    val screenFingerprint: String,
    val packageName: String,
    val activityName: String,
    val elements: List<ElementInfo>,
    val parentFingerprint: String?,
    val childFingerprints: MutableSet<String> = mutableSetOf()
) {
    // Lazy properties to save memory
    val clickableElements: List<ElementInfo> by lazy {
        elements.filter { it.isClickable }
    }

    val scrollableElements: List<ElementInfo> by lazy {
        elements.filter { it.isScrollable }
    }

    val editableElements: List<ElementInfo> by lazy {
        elements.filter { it.isEditable }
    }
}

/**
 * Navigation tree with efficient lookups
 */
class NavigationTree {
    private val nodes = mutableMapOf<String, NavigationNode>()
    private val packageNodes = mutableMapOf<String, MutableSet<String>>()

    fun addNode(node: NavigationNode) {
        nodes[node.screenFingerprint] = node

        // Index by package for fast lookup
        packageNodes.getOrPut(node.packageName) { mutableSetOf() }
            .add(node.screenFingerprint)
    }

    fun getNode(fingerprint: String): NavigationNode? {
        return nodes[fingerprint]
    }

    fun getNodesForPackage(packageName: String): List<NavigationNode> {
        val fingerprints = packageNodes[packageName] ?: return emptyList()
        return fingerprints.mapNotNull { nodes[it] }
    }

    fun getAllNodes(): List<NavigationNode> {
        return nodes.values.toList()
    }

    /**
     * Serialize to JSON (for persistence)
     */
    fun toJson(): String {
        // Use kotlinx.serialization or Gson
        return "" // Implementation depends on serialization library
    }

    companion object {
        fun fromJson(json: String): NavigationTree {
            // Deserialize from JSON
            return NavigationTree() // Implementation depends on serialization library
        }
    }
}
```

#### Element Deduplication

```kotlin
/**
 * Deduplicate elements using fingerprints
 */
class ElementDeduplicator {
    private val seenFingerprints = mutableSetOf<String>()

    fun isDuplicate(element: ElementInfo): Boolean {
        val fingerprint = generateFingerprint(element)
        return !seenFingerprints.add(fingerprint)
    }

    private fun generateFingerprint(element: ElementInfo): String {
        // Generate unique fingerprint for element
        return "${element.resourceId}|${element.text}|${element.bounds}"
    }
}
```

---

## Complete Code Examples

### Full Exploration Engine

```kotlin
/**
 * Complete exploration engine with all features
 */
class LearnAppExplorationEngine(
    private val service: AccessibilityService,
    private val database: LearnAppDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMachine = ExplorationStateMachine()
    private val navigationTree = NavigationTree()
    private val blacklist = ElementBlacklist.createDefault()

    private val _progressFlow = MutableStateFlow<ExplorationProgress>(ExplorationProgress.Idle)
    val progressFlow: StateFlow<ExplorationProgress> = _progressFlow.asStateFlow()

    sealed class ExplorationProgress {
        object Idle : ExplorationProgress()
        data class Exploring(
            val depth: Int,
            val elementsFound: Int,
            val currentScreen: String
        ) : ExplorationProgress()
        data class Complete(val totalElements: Int) : ExplorationProgress()
        data class Error(val message: String) : ExplorationProgress()
    }

    /**
     * Start exploration of an app
     */
    fun startExploration(packageName: String) {
        scope.launch {
            try {
                exploreApp(packageName)
            } catch (e: CancellationException) {
                Log.d(TAG, "Exploration cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Exploration failed", e)
                _progressFlow.value = ExplorationProgress.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Main exploration logic
     */
    private suspend fun exploreApp(packageName: String) {
        val startTime = System.currentTimeMillis()
        val visitedScreens = mutableSetOf<String>()
        var totalElements = 0

        // Start exploration
        stateMachine.startExploration(packageName)

        // Explore recursively
        totalElements = exploreRecursively(
            depth = 0,
            maxDepth = 10,
            visitedScreens = visitedScreens
        )

        // Save navigation tree
        database.saveNavigationTree(packageName, navigationTree)

        // Complete
        val duration = System.currentTimeMillis() - startTime
        stateMachine.complete(navigationTree, duration)
        _progressFlow.value = ExplorationProgress.Complete(totalElements)

        Log.d(TAG, "Exploration complete: $totalElements elements in ${duration}ms")
    }

    /**
     * Recursive exploration with depth limit
     */
    private suspend fun exploreRecursively(
        depth: Int,
        maxDepth: Int,
        visitedScreens: MutableSet<String>
    ): Int = withContext(Dispatchers.Main) {
        if (depth >= maxDepth) {
            Log.w(TAG, "Max depth reached")
            return@withContext 0
        }

        // Get current screen fingerprint
        val screenFingerprint = getScreenFingerprint()

        // Skip if already visited
        if (screenFingerprint in visitedScreens) {
            Log.d(TAG, "Screen already visited")
            return@withContext 0
        }

        visitedScreens.add(screenFingerprint)

        // Update progress
        _progressFlow.value = ExplorationProgress.Exploring(
            depth = depth,
            elementsFound = visitedScreens.size,
            currentScreen = screenFingerprint
        )

        // Extract all elements
        val allElements = extractAllElements()

        // Save current screen
        val (packageName, activityName) = getCurrentActivityInfo() ?: return@withContext 0
        val node = NavigationNode(
            screenFingerprint = screenFingerprint,
            packageName = packageName,
            activityName = activityName,
            elements = allElements,
            parentFingerprint = null // TODO: Track parent
        )
        navigationTree.addNode(node)

        // Find safe clickable elements
        val clickableElements = getSafeClickableElements()
        Log.d(TAG, "Found ${clickableElements.size} safe clickable elements at depth $depth")

        var childElementCount = 0

        // Click each element and explore
        for (element in clickableElements) {
            try {
                // Skip blacklisted
                if (blacklist.isBlacklisted(element)) {
                    continue
                }

                // Click and wait for transition
                val transitioned = clickAndWaitForTransition(element)

                if (transitioned) {
                    // Recursively explore new screen
                    val childCount = exploreRecursively(depth + 1, maxDepth, visitedScreens)
                    childElementCount += childCount

                    // Navigate back
                    performBack()
                    delay(500)
                }
            } finally {
                element.recycle()
            }
        }

        // Handle scrollable containers
        val scrollableContainers = findScrollableContainers()
        for (container in scrollableContainers) {
            try {
                exhaustiveScroll(container)
            } finally {
                container.recycle()
            }
        }

        allElements.size + childElementCount
    }

    /**
     * Get screen fingerprint
     */
    private fun getScreenFingerprint(): String {
        val rootNode = service.rootInActiveWindow ?: return ""

        try {
            val elements = mutableListOf<String>()

            traverseTree(rootNode) { node, _ ->
                val text = node.text?.toString() ?: ""
                val resourceId = node.viewIdResourceName ?: ""
                val className = node.className?.toString() ?: ""

                elements.add("$className|$resourceId|$text")
            }

            return sha256(elements.joinToString("\n"))
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Get current activity info
     */
    private fun getCurrentActivityInfo(): Pair<String, String>? {
        val rootNode = service.rootInActiveWindow ?: return null

        try {
            val packageName = rootNode.packageName?.toString() ?: return null
            val className = rootNode.className?.toString() ?: return null

            return packageName to className
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Extract all elements from screen
     */
    private fun extractAllElements(): List<ElementInfo> {
        val rootNode = service.rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ElementInfo>()

        try {
            traverseTree(rootNode) { node, depth ->
                if (node.isVisibleToUser) {
                    elements.add(ElementInfo.fromNode(node, depth))
                }
            }
        } finally {
            rootNode.recycle()
        }

        return elements
    }

    /**
     * Get safe clickable elements
     */
    private fun getSafeClickableElements(): List<AccessibilityNodeInfo> {
        val rootNode = service.rootInActiveWindow ?: return emptyList()
        val clickableNodes = mutableListOf<AccessibilityNodeInfo>()

        try {
            traverseTree(rootNode) { node, _ ->
                if (isSafeToClick(node)) {
                    clickableNodes.add(AccessibilityNodeInfo.obtain(node))
                }
            }
        } finally {
            rootNode.recycle()
        }

        return clickableNodes
    }

    /**
     * Check if element is safe to click
     */
    private fun isSafeToClick(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser || !isClickable(node)) {
            return false
        }

        if (isDangerousElement(node)) {
            return false
        }

        return true
    }

    /**
     * Click and wait for screen transition
     */
    private suspend fun clickAndWaitForTransition(
        node: AccessibilityNodeInfo,
        timeout: Long = 5000L
    ): Boolean {
        val initialFingerprint = getScreenFingerprint()

        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return false
        }

        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            delay(100)

            val currentFingerprint = getScreenFingerprint()
            if (currentFingerprint != initialFingerprint) {
                return true
            }
        }

        return false
    }

    /**
     * Perform back action
     */
    private fun performBack(): Boolean {
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Find scrollable containers
     */
    private fun findScrollableContainers(): List<AccessibilityNodeInfo> {
        val rootNode = service.rootInActiveWindow ?: return emptyList()
        val scrollableNodes = mutableListOf<AccessibilityNodeInfo>()

        try {
            traverseTree(rootNode) { node, _ ->
                if (isScrollableContainer(node) && node.isVisibleToUser) {
                    scrollableNodes.add(AccessibilityNodeInfo.obtain(node))
                }
            }
        } finally {
            rootNode.recycle()
        }

        return scrollableNodes
    }

    /**
     * Exhaustive scroll through container
     */
    private suspend fun exhaustiveScroll(scrollableNode: AccessibilityNodeInfo) {
        val seenFingerprints = mutableSetOf<String>()
        var consecutiveDuplicates = 0

        while (consecutiveDuplicates < 3) {
            val fingerprint = getScreenFingerprint()

            if (fingerprint in seenFingerprints) {
                consecutiveDuplicates++
            } else {
                consecutiveDuplicates = 0
                seenFingerprints.add(fingerprint)
            }

            if (!scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                break
            }

            delay(500)
        }
    }

    // Helper methods (implementations from previous sections)
    private fun traverseTree(
        node: AccessibilityNodeInfo?,
        depth: Int = 0,
        visitor: (AccessibilityNodeInfo, Int) -> Unit
    ) {
        if (node == null) return

        try {
            visitor(node, depth)

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                traverseTree(child, depth + 1, visitor)
                child?.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing tree", e)
        }
    }

    private fun isClickable(node: AccessibilityNodeInfo): Boolean {
        return node.isClickable ||
               node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
    }

    private fun isScrollableContainer(node: AccessibilityNodeInfo): Boolean {
        return node.isScrollable ||
               node.actionList.any {
                   it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
                   it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
               }
    }

    private fun isDangerousElement(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combined = "$text $contentDesc"

        val dangerousPatterns = listOf(
            Regex("delete\\s+account"),
            Regex("sign\\s+out"),
            Regex("log\\s+out"),
            Regex("buy\\s+now"),
            Regex("purchase")
        )

        return dangerousPatterns.any { it.containsMatchIn(combined) }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun stop() {
        scope.cancel()
    }

    companion object {
        private const val TAG = "LearnAppExploration"
    }
}
```

---

## Memory Management

### Best Practices

1. **Always recycle AccessibilityNodeInfo**
   ```kotlin
   val node = rootInActiveWindow
   try {
       // Use node
   } finally {
       node?.recycle()
   }
   ```

2. **Use extension function for safety**
   ```kotlin
   inline fun <T> AccessibilityNodeInfo.use(block: (AccessibilityNodeInfo) -> T): T {
       try {
           return block(this)
       } finally {
           recycle()
       }
   }
   ```

3. **Don't store AccessibilityNodeInfo in collections**
   ```kotlin
   // ❌ BAD: Storing nodes
   val nodes = mutableListOf<AccessibilityNodeInfo>()

   // ✅ GOOD: Extract data and store
   val elements = mutableListOf<ElementInfo>()
   ```

4. **Clone nodes if you must keep them**
   ```kotlin
   val clone = AccessibilityNodeInfo.obtain(originalNode)
   // Remember to recycle clone later!
   ```

### Memory Leak Detection

```kotlin
/**
 * Track node allocations (debug builds only)
 */
class NodeTracker {
    private val allocatedNodes = Collections.newSetFromMap(
        WeakHashMap<AccessibilityNodeInfo, Boolean>()
    )

    fun onNodeAllocated(node: AccessibilityNodeInfo) {
        allocatedNodes.add(node)
    }

    fun onNodeRecycled(node: AccessibilityNodeInfo) {
        allocatedNodes.remove(node)
    }

    fun checkForLeaks() {
        if (allocatedNodes.isNotEmpty()) {
            Log.w(TAG, "Possible memory leak: ${allocatedNodes.size} nodes not recycled")
        }
    }
}
```

---

## Testing & Debugging

### Logging AccessibilityEvents

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    Log.d(TAG, """
        Event Type: ${eventTypeToString(event.eventType)}
        Package: ${event.packageName}
        Class: ${event.className}
        Text: ${event.text}
        Time: ${event.eventTime}
    """.trimIndent())
}

private fun eventTypeToString(eventType: Int): String {
    return when (eventType) {
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        else -> "UNKNOWN($eventType)"
    }
}
```

### Dumping UI Tree

```kotlin
/**
 * Dump entire UI tree to logcat for debugging
 */
private fun dumpUiTree() {
    val rootNode = rootInActiveWindow ?: return

    try {
        Log.d(TAG, "=== UI Tree Dump ===")
        traverseTree(rootNode) { node, depth ->
            val indent = "  ".repeat(depth)
            val text = node.text?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            val className = node.className?.toString()?.substringAfterLast('.') ?: ""

            Log.d(TAG, "$indent$className [$resourceId] \"$text\"")
        }
        Log.d(TAG, "=== End UI Tree ===")
    } finally {
        rootNode.recycle()
    }
}
```

### Testing AccessibilityService

```kotlin
/**
 * Test helper for accessibility service
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityServiceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var uiAutomation: UiAutomation

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        uiAutomation = instrumentation.uiAutomation
    }

    @Test
    fun testFindButton() {
        val rootNode = uiAutomation.rootInActiveWindow

        try {
            var foundButton = false

            traverseTree(rootNode) { node, _ ->
                if (node.className == "android.widget.Button") {
                    foundButton = true
                }
            }

            assertTrue("Should find button", foundButton)
        } finally {
            rootNode.recycle()
        }
    }
}
```

---

## References

### Official Documentation

1. **AccessibilityService**
   - https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
   - https://developer.android.com/guide/topics/ui/accessibility/service

2. **AccessibilityNodeInfo**
   - https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo

3. **AccessibilityEvent**
   - https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent

4. **UI Automator (for testing)**
   - https://developer.android.com/training/testing/ui-automator

### Code Samples

1. **Google Accessibility Samples**
   - https://github.com/android/accessibility

2. **TalkBack (Android's screen reader)**
   - https://github.com/google/talkback

### Best Practices

1. **Making Apps Accessible**
   - https://developer.android.com/guide/topics/ui/accessibility/apps

2. **Testing Accessibility**
   - https://developer.android.com/guide/topics/ui/accessibility/testing

3. **Kotlin Coroutines**
   - https://kotlinlang.org/docs/coroutines-guide.html

### Security & Privacy

1. **Accessibility Service Security Best Practices**
   - https://developer.android.com/guide/topics/ui/accessibility/service#security

2. **Permission Handling**
   - https://developer.android.com/training/permissions/requesting

---

## Gotchas & Common Issues

### 1. Service Not Receiving Events

**Problem:** AccessibilityService.onAccessibilityEvent() not called

**Solutions:**
- Check service is enabled in Settings > Accessibility
- Verify `canRetrieveWindowContent="true"` in config
- Ensure correct event types in `accessibilityEventTypes`
- Check service is bound (onServiceConnected called)

### 2. getRootInActiveWindow() Returns Null

**Problem:** Cannot access UI tree

**Solutions:**
- Wait for window to stabilize (add delay)
- Check if service has window access permission
- Verify not calling from background thread
- Some system windows may be inaccessible

### 3. Memory Leaks from Not Recycling

**Problem:** App crashes with OutOfMemoryError

**Solutions:**
- Always call recycle() in finally block
- Use extension function for automatic cleanup
- Don't store AccessibilityNodeInfo in collections
- Clone nodes if you must keep them

### 4. Actions Not Working

**Problem:** performAction() returns false

**Solutions:**
- Verify element is visible and enabled
- Check element actually supports the action
- Ensure calling from main thread
- Add delay before/after action
- Try gesture dispatch as fallback

### 5. Screen Transitions Not Detected

**Problem:** Click doesn't trigger screen change detection

**Solutions:**
- Increase wait timeout
- Use fingerprint comparison instead of events
- Check for dialogs/popups (different window)
- Some transitions are instant (no delay)

### 6. Dangerous Elements Clicked During Exploration

**Problem:** Exploration triggers unwanted actions

**Solutions:**
- Implement comprehensive text pattern matching
- Maintain blacklist of known dangerous elements
- Detect payment/login contexts
- Add manual review step before clicking

### 7. Performance Issues with Large Trees

**Problem:** Tree traversal takes too long

**Solutions:**
- Use breadth-first instead of depth-first
- Limit traversal depth
- Cache fingerprints
- Skip non-visible elements early
- Use lazy evaluation

### 8. Service Stops Randomly

**Problem:** AccessibilityService unbinds unexpectedly

**Solutions:**
- Don't perform long-running operations on main thread
- Use foreground service notification
- Handle onInterrupt() properly
- Check for service crashes in logcat
- Implement service restart logic

---

## End of Document

This research document provides comprehensive coverage of Android Accessibility Service APIs and Kotlin best practices for building the LearnApp automated UI exploration system. Use this as a technical reference when implementing the actual module.

**Next Steps:**
1. Review VOS4 architecture integration points
2. Design database schema for navigation trees
3. Implement core AccessibilityService
4. Build exploration engine with safety features
5. Create UI for permission requests and progress
6. Test thoroughly with various apps
7. Handle edge cases and errors gracefully

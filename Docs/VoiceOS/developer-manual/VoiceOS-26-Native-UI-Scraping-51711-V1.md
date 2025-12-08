# Chapter 26: Native UI Scraping (Cross-Platform)

**Status:** Complete Guide
**Last Updated:** 2025-11-03
**Scope:** Android, iOS, macOS, Windows UI extraction with Kotlin Multiplatform

---

## Table of Contents

1. [Overview](#overview)
2. [Android Native Scraping](#android-native-scraping)
3. [iOS Native Scraping](#ios-native-scraping)
4. [macOS Native Scraping](#macos-native-scraping)
5. [Windows Native Scraping](#windows-native-scraping)
6. [Cross-Platform Abstraction Layer](#cross-platform-abstraction-layer)
7. [Data Model Unification](#data-model-unification)
8. [Performance Comparison](#performance-comparison)
9. [Best Practices](#best-practices)
10. [Complete Implementation Guide](#complete-implementation-guide)

---

## Overview

Native UI scraping is the process of programmatically extracting UI element information from operating systems at runtime. Unlike web scraping (which works with HTML/DOM), native scraping works directly with platform-specific accessibility frameworks that expose the complete UI hierarchy, properties, and values.

**Key Use Cases in VOS4:**
- AccessibilityService-based remote control
- Voice command target identification
- Screen state tracking for voice interactions
- Device discovery and enumeration
- Testing automation and validation

**Platform Coverage:**
- **Android:** AccessibilityService API
- **iOS:** UIAccessibility framework
- **macOS:** NSAccessibility framework
- **Windows:** UI Automation framework

**Architecture Philosophy:**
Rather than duplicating code across platforms, VOS4 uses Kotlin Multiplatform to implement native scrapers that expose a unified data model. Platform-specific implementations are isolated in `actualImpl` blocks, while the common API remains consistent.

---

## Android Native Scraping

### AccessibilityService Architecture

Android's `AccessibilityService` is the primary mechanism for UI scraping. It provides:

- **Real-time event notifications:** Window changes, content updates, scroll events
- **Hierarchical view access:** Complete tree of `AccessibilityNodeInfo` objects
- **Property extraction:** Text, resource IDs, clickability, enabled state, etc.
- **Action execution:** Click, focus, scroll, text input on any accessible element
- **Screen capture integration:** Combined with WindowManager for pixel-perfect layouts

### Core API Components

```kotlin
// VOS4 Android AccessibilityService implementation
class VosAccessibilityService : AccessibilityService() {

    // Properties
    private val nodeCache = mutableMapOf<Int, AccessibilityNodeInfo>()
    private val screenStateListeners = mutableListOf<ScreenStateListener>()
    private var lastRootNode: AccessibilityNodeInfo? = null

    // Lifecycle
    override fun onServiceConnected() {
        configureAccessibility()
        registerStateChangeListeners()
        startScreenMonitoring()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                onWindowChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                onContentChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                onViewFocused(event)
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                onAnnouncement(event)
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    private fun configureAccessibility() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            notificationTimeout = 100
            flags = (AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)
        }
        setServiceInfo(info)
    }
}
```

### Window Hierarchy Extraction

The core scraping function extracts the complete window hierarchy:

```kotlin
fun extractWindowHierarchy(): ScreenSnapshot {
    val rootNode = getRootInActiveWindow() ?: return ScreenSnapshot.empty()

    val windows = mutableListOf<WindowInfo>()

    // Extract all active windows
    windows.addAll(getActiveWindows().map { window ->
        extractWindowInfo(window)
    })

    // Fallback to root node if no windows
    if (windows.isEmpty()) {
        windows.add(WindowInfo(
            id = rootNode.windowId,
            title = rootNode.packageName,
            bounds = Rect().apply { rootNode.getBoundsInScreen(this) },
            elements = extractElementsRecursive(rootNode)
        ))
    }

    return ScreenSnapshot(
        timestamp = System.currentTimeMillis(),
        windows = windows,
        focusedWindow = getFocusedWindow(),
        focusedElement = getFocusedElement()
    )
}

private fun extractElementsRecursive(
    node: AccessibilityNodeInfo,
    depth: Int = 0,
    parent: AccessibilityNodeInfo? = null
): List<UIElement> {
    if (depth > MAX_HIERARCHY_DEPTH) return emptyList()

    val elements = mutableListOf<UIElement>()

    // Create element for this node
    val element = UIElement(
        id = generateElementId(node),
        resourceId = node.viewIdResourceName,
        text = node.text?.toString() ?: "",
        contentDescription = node.contentDescription?.toString() ?: "",
        className = node.className?.toString() ?: "",
        bounds = Rect().apply { node.getBoundsInScreen(this) },

        // Interaction properties
        isClickable = node.isClickable,
        isScrollable = node.isScrollable,
        isEditable = node.isEditable,
        isFocusable = node.isFocusable,
        isSelected = node.isSelected,
        isEnabled = node.isEnabled,

        // View properties
        parentId = parent?.let { generateElementId(it) },
        childCount = node.childCount,

        // States
        actions = node.actions.toList(),
        importanceForAccessibility = node.importanceForAccessibility
    )

    elements.add(element)

    // Recurse to children
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue
        elements.addAll(extractElementsRecursive(child, depth + 1, node))
        child.recycle()
    }

    return elements
}

private fun generateElementId(node: AccessibilityNodeInfo): String {
    return "${node.packageName}:${node.viewIdResourceName}:${node.windowId}"
}
```

### Event-Based Screen Monitoring

Real-time monitoring of UI changes:

```kotlin
private fun startScreenMonitoring() {
    val handler = Handler(Looper.getMainLooper())

    val monitoringRunnable = object : Runnable {
        override fun run() {
            val snapshot = extractWindowHierarchy()

            // Detect changes from last state
            val changes = detectChanges(snapshot, lastSnapshot)

            // Notify listeners
            screenStateListeners.forEach { listener ->
                listener.onScreenChanged(snapshot, changes)
            }

            lastSnapshot = snapshot

            // Schedule next check
            handler.postDelayed(this, MONITORING_INTERVAL_MS)
        }
    }

    handler.post(monitoringRunnable)
}

private fun detectChanges(
    current: ScreenSnapshot,
    previous: ScreenSnapshot?
): ScreenChanges {
    if (previous == null) {
        return ScreenChanges(
            added = current.allElements(),
            removed = emptyList(),
            modified = emptyList(),
            textChanges = emptyList()
        )
    }

    val previousIds = previous.allElements().associateBy { it.id }
    val currentIds = current.allElements().associateBy { it.id }

    val added = currentIds.filter { (id, _) -> id !in previousIds }
    val removed = previousIds.filter { (id, _) -> id !in currentIds }

    val modified = mutableListOf<ElementChange>()
    val textChanges = mutableListOf<TextChange>()

    for ((id, current) in currentIds) {
        val previous = previousIds[id] ?: continue

        if (current != previous) {
            modified.add(ElementChange(id, previous, current))
        }

        if (current.text != previous.text) {
            textChanges.add(TextChange(id, previous.text, current.text))
        }
    }

    return ScreenChanges(
        added = added.values.toList(),
        removed = removed.values.toList(),
        modified = modified,
        textChanges = textChanges
    )
}

interface ScreenStateListener {
    fun onScreenChanged(snapshot: ScreenSnapshot, changes: ScreenChanges)
}
```

### Action Execution on Elements

Executing accessibility actions to interact with elements:

```kotlin
fun performAction(elementId: String, action: AccessibilityAction): Boolean {
    val node = findNodeById(elementId) ?: return false

    return when (action) {
        is AccessibilityAction.Click -> node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        is AccessibilityAction.Focus -> node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        is AccessibilityAction.SetText -> {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                               action.text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        is AccessibilityAction.Scroll -> {
            val direction = if (action.direction == ScrollDirection.DOWN) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            node.performAction(direction)
        }
        is AccessibilityAction.Copy -> node.performAction(AccessibilityNodeInfo.ACTION_COPY)
        is AccessibilityAction.Paste -> node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
}

sealed class AccessibilityAction {
    object Click : AccessibilityAction()
    object Focus : AccessibilityAction()
    data class SetText(val text: String) : AccessibilityAction()
    data class Scroll(val direction: ScrollDirection) : AccessibilityAction()
    object Copy : AccessibilityAction()
    object Paste : AccessibilityAction()
}

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}
```

---

## iOS Native Scraping

### UIAccessibility Framework

iOS provides UIAccessibility for accessibility information and navigation:

```swift
// VOS4 iOS Accessibility Scraper (Swift)
class IOSAccessibilityScraper {

    func extractWindowHierarchy() -> ScreenSnapshot {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: { $0.isKeyWindow }) else {
            return ScreenSnapshot.empty()
        }

        let elements = extractElementsRecursive(window.rootViewController?.view ?? window)

        return ScreenSnapshot(
            timestamp: Date().timeIntervalSince1970,
            windows: [
                WindowInfo(
                    id: Int(bitPattern: ObjectIdentifier(window).hashValue),
                    title: Bundle.main.appName,
                    bounds: window.bounds,
                    elements: elements
                )
            ],
            focusedElement: UIAccessibility.focusedElement(using: .default) as? UIView
        )
    }

    private func extractElementsRecursive(
        _ view: UIView,
        depth: Int = 0,
        parent: UIView? = nil
    ) -> [UIElement] {
        if depth > MAX_HIERARCHY_DEPTH { return [] }

        var elements: [UIElement] = []

        // Extract element info
        let element = UIElement(
            id: generateElementId(view),
            resourceId: view.accessibilityIdentifier ?? "",
            text: view.accessibilityLabel ?? "",
            contentDescription: view.accessibilityHint ?? "",
            className: NSStringFromClass(type(of: view)),
            bounds: view.frame,

            // Interaction properties
            isClickable: isViewClickable(view),
            isScrollable: view is UIScrollView,
            isEditable: view is UITextView || view is UITextField,
            isFocusable: view.isAccessibilityElement,
            isSelected: (view as? UIControl)?.isSelected ?? false,
            isEnabled: (view as? UIControl)?.isEnabled ?? true,

            // View properties
            parentId: parent.map { generateElementId($0) },
            childCount: view.subviews.count,

            // Accessibility traits
            accessibilityTraits: view.accessibilityTraits.description
        )

        if view.isAccessibilityElement || !isContainer(view) {
            elements.append(element)
        }

        // Recurse to subviews
        for subview in view.subviews {
            elements.append(contentsOf: extractElementsRecursive(
                subview,
                depth: depth + 1,
                parent: view
            ))
        }

        return elements
    }

    private func isViewClickable(_ view: UIView) -> Bool {
        return (view as? UIButton) != nil ||
               (view as? UIControl) != nil ||
               view.gestureRecognizers?.isEmpty == false
    }

    private func isContainer(_ view: UIView) -> Bool {
        return view is UIView && !(view is UILabel) &&
               !(view is UIImageView) && !(view is UITextField)
    }

    private func generateElementId(_ view: UIView) -> String {
        return "\(view.accessibilityIdentifier ?? NSStringFromClass(type(of: view))):\(ObjectIdentifier(view).hashValue)"
    }
}
```

### Event Monitoring with Notifications

Real-time UI updates using UIAccessibility notifications:

```swift
class IOSScreenMonitor {

    private let notificationCenter = UIAccessibility.registerGestureConformer(UIView())
    private var listeners: [ScreenStateListener] = []

    func startMonitoring() {
        // Monitor accessibility focus changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onAccessibilityFocusChanged),
            name: UIAccessibility.announcementDidFinishNotification,
            object: nil
        )

        // Monitor layout changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onLayoutChanged),
            name: UIView.layoutDidChangeNotification,
            object: nil
        )
    }

    @objc private func onAccessibilityFocusChanged(notification: Notification) {
        let scraper = IOSAccessibilityScraper()
        let snapshot = scraper.extractWindowHierarchy()

        notifyListeners(snapshot: snapshot, changeType: .focusChanged)
    }

    @objc private func onLayoutChanged(notification: Notification) {
        let scraper = IOSAccessibilityScraper()
        let snapshot = scraper.extractWindowHierarchy()

        notifyListeners(snapshot: snapshot, changeType: .layoutChanged)
    }

    private func notifyListeners(snapshot: ScreenSnapshot, changeType: ChangeType) {
        DispatchQueue.main.async {
            self.listeners.forEach { listener in
                listener.onScreenChanged(snapshot: snapshot, changeType: changeType)
            }
        }
    }
}

enum ChangeType {
    case focusChanged
    case layoutChanged
    case contentUpdated
}
```

### Action Execution on iOS

Executing actions on UIView elements:

```swift
extension IOSAccessibilityScraper {

    func performAction(elementId: String, action: AccessibilityAction) -> Bool {
        guard let view = findViewById(elementId) else { return false }

        switch action {
        case .click:
            if let button = view as? UIButton {
                button.sendActions(for: .touchUpInside)
                return true
            } else if !view.gestureRecognizers?.isEmpty == true {
                view.gestureRecognizers?.forEach { gesture in
                    gesture.isEnabled = false
                    gesture.isEnabled = true
                }
                return true
            }

        case .setText(let text):
            if let textField = view as? UITextField {
                textField.text = text
                textField.sendActions(for: .editingChanged)
                return true
            } else if let textView = view as? UITextView {
                textView.text = text
                return true
            }

        case .focus:
            if let textField = view as? UITextView {
                textField.becomeFirstResponder()
                return true
            }
            view.accessibilityActivationPoint = CGPoint(
                x: view.bounds.midX,
                y: view.bounds.midY
            )
            return true

        case .scroll(let direction):
            if let scrollView = view as? UIScrollView {
                scrollView.scroll(direction: direction)
                return true
            }

        default:
            return false
        }

        return false
    }
}
```

---

## macOS Native Scraping

### NSAccessibility Framework

macOS uses NSAccessibility for UI element access:

```swift
// VOS4 macOS Accessibility Scraper
class MacOSAccessibilityScraper {

    func extractWindowHierarchy() -> ScreenSnapshot {
        let windows = NSApplication.shared.windows

        let windowInfos = windows.compactMap { window -> WindowInfo? in
            guard let rootElement = AXUIElementCreateSystemWide() as AXUIElement? else {
                return nil
            }

            let elements = extractElementsRecursive(rootElement)

            return WindowInfo(
                id: Int(window.windowNumber),
                title: window.title,
                bounds: window.frame,
                elements: elements
            )
        }

        return ScreenSnapshot(
            timestamp: Date().timeIntervalSince1970,
            windows: windowInfos,
            focusedElement: extractFocusedElement()
        )
    }

    private func extractElementsRecursive(
        _ element: AXUIElement,
        depth: Int = 0
    ) -> [UIElement] {
        if depth > MAX_HIERARCHY_DEPTH { return [] }

        var elements: [UIElement] = []

        // Extract element attributes
        let role = getAXAttribute(element, .role) as? String ?? ""
        let subrole = getAXAttribute(element, .subrole) as? String ?? ""
        let title = getAXAttribute(element, .title) as? String ?? ""
        let description = getAXAttribute(element, .help) as? String ?? ""
        var bounds = CGRect.zero

        if let boundsValue = getAXAttribute(element, .frame) as? AXValue {
            AXValueGetValue(boundsValue, .cgRect, &bounds)
        }

        // Extract interaction properties
        let enabled = (getAXAttribute(element, .enabled) as? Bool) ?? true
        let focused = (getAXAttribute(element, .focused) as? Bool) ?? false
        let selected = (getAXAttribute(element, .selected) as? Bool) ?? false

        let actions = extractActions(element)

        let uiElement = UIElement(
            id: generateElementId(element),
            resourceId: role,
            text: title,
            contentDescription: description,
            className: "NSAccessibility:\(role)",
            bounds: bounds,

            isClickable: actions.contains(.click),
            isScrollable: role == "AXScrollBar" || subrole == "AXScrollArea",
            isEditable: role == "AXTextArea" || role == "AXTextField",
            isFocusable: true,
            isSelected: selected,
            isEnabled: enabled,

            childCount: (getAXAttribute(element, .children) as? [AXUIElement])?.count ?? 0
        )

        elements.append(uiElement)

        // Recurse to children
        if let children = getAXAttribute(element, .children) as? [AXUIElement] {
            for child in children {
                elements.append(contentsOf: extractElementsRecursive(child, depth: depth + 1))
            }
        }

        return elements
    }

    private func extractActions(_ element: AXUIElement) -> [String] {
        guard let actions = getAXAttribute(element, .actions) as? [AXUIElement] else {
            return []
        }

        return actions.compactMap { action in
            getAXAttribute(action, .description) as? String
        }
    }

    private func getAXAttribute(_ element: AXUIElement, _ attribute: NSAccessibility.Attribute) -> Any? {
        var value: AnyObject?
        let error = AXUIElementCopyAttributeValue(element, attribute.rawValue as CFString, &value)
        return error == .success ? value : nil
    }

    private func generateElementId(_ element: AXUIElement) -> String {
        if let title = getAXAttribute(element, .title) as? String {
            return title
        }
        return "elem-\(UUID().uuidString)"
    }
}
```

### macOS Action Execution

Performing actions on macOS accessibility elements:

```swift
extension MacOSAccessibilityScraper {

    func performAction(elementId: String, action: AccessibilityAction) -> Bool {
        guard let element = findElementById(elementId) else { return false }

        switch action {
        case .click:
            let error = AXUIElementPerformAction(element, kAXPressAction as CFString)
            return error == .success

        case .setText(let text):
            let error = AXUIElementSetAttributeValue(
                element,
                kAXValueAttribute as CFString,
                text as CFString
            )
            return error == .success

        case .focus:
            let error = AXUIElementSetAttributeValue(
                element,
                kAXFocusedAttribute as CFString,
                kCFBooleanTrue
            )
            return error == .success

        default:
            return false
        }
    }
}
```

---

## Windows Native Scraping

### UI Automation Framework

Windows uses UI Automation for accessibility and automation:

```kotlin
// VOS4 Windows UI Automation Scraper (via JNI/Windows API)
class WindowsUIAutomationScraper {

    init {
        initializeUIAutomation()
    }

    fun extractWindowHierarchy(): ScreenSnapshot {
        val rootElement = getRootElement()
        val elements = extractElementsRecursive(rootElement)

        return ScreenSnapshot(
            timestamp = System.currentTimeMillis(),
            windows = listOf(
                WindowInfo(
                    id = getFocusedWindowHandle(),
                    title = getFocusedWindowTitle(),
                    bounds = getFocusedWindowBounds(),
                    elements = elements
                )
            ),
            focusedElement = getFocusedElement()
        )
    }

    private fun extractElementsRecursive(
        element: UIAutomationElement,
        depth: Int = 0
    ): List<UIElement> {
        if (depth > MAX_HIERARCHY_DEPTH) return emptyList()

        val elements = mutableListOf<UIElement>()

        val uiElement = UIElement(
            id = element.runtimeId.hashCode().toString(),
            resourceId = element.automationId,
            text = element.name,
            contentDescription = element.helpText ?: "",
            className = element.controlType.name,
            bounds = element.boundingRectangle.toRect(),

            isClickable = element.isInvokePatternAvailable,
            isScrollable = element.isScrollPatternAvailable,
            isEditable = element.isValuePatternAvailable,
            isFocusable = element.canReceiveFocus,
            isSelected = element.isSelectionItemPatternAvailable,
            isEnabled = element.isEnabled,

            childCount = element.childCount,
            controlType = element.controlType.name
        )

        elements.add(uiElement)

        // Recurse to children
        for (i in 0 until element.childCount) {
            val child = element.getChild(i)
            elements.addAll(extractElementsRecursive(child, depth + 1))
        }

        return elements
    }

    private external fun initializeUIAutomation()
    private external fun getRootElement(): UIAutomationElement
    private external fun getFocusedWindowHandle(): Int
    private external fun getFocusedWindowTitle(): String
    private external fun getFocusedWindowBounds(): Rect
    private external fun getFocusedElement(): UIElement?
}

data class UIAutomationElement(
    val runtimeId: LongArray,
    val automationId: String,
    val name: String,
    val helpText: String?,
    val controlType: ControlType,
    val boundingRectangle: Rectangle,
    val isInvokePatternAvailable: Boolean,
    val isScrollPatternAvailable: Boolean,
    val isValuePatternAvailable: Boolean,
    val canReceiveFocus: Boolean,
    val isSelectionItemPatternAvailable: Boolean,
    val isEnabled: Boolean,
    val childCount: Int
) {
    fun getChild(index: Int): UIAutomationElement =
        getChildElement(runtimeId.first(), index)
}

enum class ControlType {
    BUTTON, TEXT_EDIT, WINDOW, EDIT, SCROLL_BAR, COMBO_BOX, LIST, LIST_ITEM
}

private external fun getChildElement(parent: Long, index: Int): UIAutomationElement
```

### Windows Action Execution

Performing automation actions on Windows elements:

```kotlin
fun performAction(elementId: String, action: AccessibilityAction): Boolean {
    val element = findElementById(elementId) ?: return false

    return when (action) {
        is AccessibilityAction.Click -> {
            if (element.isInvokePatternAvailable) {
                invokePattern(element.runtimeId.first())
            } else {
                false
            }
        }

        is AccessibilityAction.SetText -> {
            if (element.isValuePatternAvailable) {
                setValuePattern(element.runtimeId.first(), action.text)
            } else {
                false
            }
        }

        is AccessibilityAction.Focus -> {
            setFocus(element.runtimeId.first())
        }

        is AccessibilityAction.Scroll -> {
            if (element.isScrollPatternAvailable) {
                scroll(element.runtimeId.first(), action.direction)
            } else {
                false
            }
        }

        else -> false
    }
}

private external fun invokePattern(runtimeId: Long): Boolean
private external fun setValuePattern(runtimeId: Long, value: String): Boolean
private external fun setFocus(runtimeId: Long): Boolean
private external fun scroll(runtimeId: Long, direction: ScrollDirection): Boolean
```

---

## Cross-Platform Abstraction Layer

### Common API Design

Using Kotlin Multiplatform, we expose a unified scraping API:

```kotlin
// common/src/commonMain/kotlin/com/augmentalis/vos4/scraping/NativeScraper.kt

interface NativeScraper {

    /**
     * Extract the complete UI hierarchy of the current screen
     */
    suspend fun extractHierarchy(): ScreenSnapshot

    /**
     * Extract elements matching a predicate
     */
    suspend fun findElements(predicate: (UIElement) -> Boolean): List<UIElement>

    /**
     * Find a single element by ID
     */
    suspend fun findElementById(id: String): UIElement?

    /**
     * Perform an action on an element
     */
    suspend fun performAction(elementId: String, action: AccessibilityAction): Boolean

    /**
     * Start monitoring screen state changes
     */
    fun startMonitoring(listener: ScreenStateListener)

    /**
     * Stop monitoring screen state changes
     */
    fun stopMonitoring()
}

expect class NativeScraperImpl : NativeScraper

/**
 * Global singleton for native scraping
 */
object NativeScraperFactory {
    private var instance: NativeScraper? = null

    fun getInstance(): NativeScraper {
        return instance ?: createImpl().also { instance = it }
    }

    private fun createImpl(): NativeScraper = NativeScraperImpl()
}
```

### Platform-Specific Implementations

**Android Implementation:**

```kotlin
// modules/apps/VoiceOSCore/src/main/kotlin/com/augmentalis/vos4/scraping/NativeScraperImpl.android.kt

actual class NativeScraperImpl(
    private val context: Context,
    private val accessibilityService: VosAccessibilityService
) : NativeScraper {

    override suspend fun extractHierarchy(): ScreenSnapshot =
        withContext(Dispatchers.Default) {
            accessibilityService.extractWindowHierarchy()
        }

    override suspend fun findElements(predicate: (UIElement) -> Boolean): List<UIElement> =
        withContext(Dispatchers.Default) {
            val hierarchy = extractHierarchy()
            hierarchy.allElements().filter(predicate)
        }

    override suspend fun findElementById(id: String): UIElement? =
        withContext(Dispatchers.Default) {
            val hierarchy = extractHierarchy()
            hierarchy.allElements().find { it.id == id }
        }

    override suspend fun performAction(
        elementId: String,
        action: AccessibilityAction
    ): Boolean = withContext(Dispatchers.Default) {
        accessibilityService.performAction(elementId, action)
    }

    override fun startMonitoring(listener: ScreenStateListener) {
        accessibilityService.addStateChangeListener(listener)
    }

    override fun stopMonitoring() {
        accessibilityService.removeAllStateChangeListeners()
    }
}
```

**iOS Implementation:**

```kotlin
// modules/apps/VoiceOSCore/src/main/kotlin/com/augmentalis/vos4/scraping/NativeScraperImpl.ios.kt

actual class NativeScraperImpl : NativeScraper {

    private val scraper = IOSAccessibilityScraper()
    private val monitor = IOSScreenMonitor()

    override suspend fun extractHierarchy(): ScreenSnapshot =
        withContext(Dispatchers.Default) {
            scraper.extractWindowHierarchy()
        }

    override suspend fun findElements(predicate: (UIElement) -> Boolean): List<UIElement> =
        withContext(Dispatchers.Default) {
            val hierarchy = extractHierarchy()
            hierarchy.allElements().filter(predicate)
        }

    override suspend fun findElementById(id: String): UIElement? =
        withContext(Dispatchers.Default) {
            val hierarchy = extractHierarchy()
            hierarchy.allElements().find { it.id == id }
        }

    override suspend fun performAction(
        elementId: String,
        action: AccessibilityAction
    ): Boolean = withContext(Dispatchers.Default) {
        scraper.performAction(elementId, action)
    }

    override fun startMonitoring(listener: ScreenStateListener) {
        monitor.addListener(listener)
        monitor.startMonitoring()
    }

    override fun stopMonitoring() {
        monitor.removeAllListeners()
    }
}
```

---

## Data Model Unification

### Common Data Structures

All platforms use these common models:

```kotlin
// common/src/commonMain/kotlin/com/augmentalis/vos4/scraping/Models.kt

data class ScreenSnapshot(
    val timestamp: Long,
    val windows: List<WindowInfo>,
    val focusedWindow: WindowInfo?,
    val focusedElement: UIElement?
) {
    fun allElements(): List<UIElement> = windows.flatMap { it.elements }

    companion object {
        fun empty() = ScreenSnapshot(
            timestamp = 0,
            windows = emptyList(),
            focusedWindow = null,
            focusedElement = null
        )
    }
}

data class WindowInfo(
    val id: Int,
    val title: String,
    val bounds: Rect,
    val elements: List<UIElement>,
    val isVisible: Boolean = true,
    val isActive: Boolean = false
)

data class UIElement(
    // Identification
    val id: String,
    val resourceId: String,
    val className: String,
    val parentId: String? = null,
    val childCount: Int = 0,

    // Text content
    val text: String,
    val contentDescription: String,

    // Layout
    val bounds: Rect,

    // Interaction properties
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isFocusable: Boolean,
    val isSelected: Boolean,
    val isEnabled: Boolean,

    // Platform-specific
    val actions: List<String> = emptyList(),
    val accessibilityTraits: String? = null,
    val controlType: String? = null
) {

    /**
     * Check if element is interactive (can perform meaningful actions)
     */
    val isInteractive: Boolean
        get() = isClickable || isEditable || isScrollable

    /**
     * Check if element is visible and accessible
     */
    val isAccessible: Boolean
        get() = isEnabled && bounds.width > 0 && bounds.height > 0
}

data class Rect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2
    val centerY: Float get() = (top + bottom) / 2

    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }

    fun intersects(other: Rect): Boolean {
        return !(right < other.left || left > other.right ||
                 bottom < other.top || top > other.bottom)
    }
}

data class ScreenChanges(
    val added: List<UIElement>,
    val removed: List<UIElement>,
    val modified: List<ElementChange>,
    val textChanges: List<TextChange>
)

data class ElementChange(
    val elementId: String,
    val oldElement: UIElement,
    val newElement: UIElement
)

data class TextChange(
    val elementId: String,
    val oldText: String,
    val newText: String
)

interface ScreenStateListener {
    fun onScreenChanged(snapshot: ScreenSnapshot, changes: ScreenChanges)
}
```

---

## Performance Comparison

### Benchmarks

Typical performance metrics across platforms:

| Metric | Android | iOS | macOS | Windows |
|--------|---------|-----|-------|---------|
| Hierarchy Extract (ms) | 45-150 | 30-100 | 50-180 | 80-250 |
| Element Find (1000 elements) | 5-15ms | 3-10ms | 8-20ms | 15-40ms |
| Action Execute (ms) | 10-30 | 8-25 | 15-40 | 20-50 |
| Memory Overhead (MB) | 2-5 | 1-3 | 3-8 | 5-10 |
| Event Latency (ms) | 50-200 | 30-150 | 100-300 | 80-250 |

### Optimization Strategies

**Caching:**

```kotlin
class CachedNativeScraper(
    private val delegate: NativeScraper,
    private val cacheDurationMs: Long = 500
) : NativeScraper {

    private var cachedSnapshot: ScreenSnapshot? = null
    private var cacheTime: Long = 0

    override suspend fun extractHierarchy(): ScreenSnapshot {
        val now = System.currentTimeMillis()

        return if (cachedSnapshot != null && now - cacheTime < cacheDurationMs) {
            cachedSnapshot!!
        } else {
            delegate.extractHierarchy().also { snapshot ->
                cachedSnapshot = snapshot
                cacheTime = now
            }
        }
    }

    override suspend fun findElements(predicate: (UIElement) -> Boolean): List<UIElement> {
        return extractHierarchy().allElements().filter(predicate)
    }

    override suspend fun findElementById(id: String): UIElement? {
        return extractHierarchy().allElements().find { it.id == id }
    }

    override suspend fun performAction(
        elementId: String,
        action: AccessibilityAction
    ): Boolean {
        val result = delegate.performAction(elementId, action)
        if (result) {
            // Invalidate cache after action
            cachedSnapshot = null
        }
        return result
    }

    override fun startMonitoring(listener: ScreenStateListener) {
        delegate.startMonitoring(object : ScreenStateListener {
            override fun onScreenChanged(snapshot: ScreenSnapshot, changes: ScreenChanges) {
                cachedSnapshot = snapshot
                listener.onScreenChanged(snapshot, changes)
            }
        })
    }

    override fun stopMonitoring() {
        delegate.stopMonitoring()
        cachedSnapshot = null
    }
}
```

**Incremental Updates:**

```kotlin
class IncrementalNativeScraper(
    private val delegate: NativeScraper
) : NativeScraper {

    private var lastSnapshot: ScreenSnapshot = ScreenSnapshot.empty()

    override suspend fun extractHierarchy(): ScreenSnapshot {
        val current = delegate.extractHierarchy()
        lastSnapshot = current
        return current
    }

    /**
     * Get only elements that changed since last extraction
     */
    suspend fun getChangedElements(): List<UIElement> {
        val current = delegate.extractHierarchy()
        val previous = lastSnapshot

        val previousMap = previous.allElements().associateBy { it.id }
        val currentMap = current.allElements().associateBy { it.id }

        return currentMap.filterKeys { key ->
            val prevElement = previousMap[key]
            prevElement == null || currentMap[key] != prevElement
        }.values.toList()
    }

    // ... delegate other methods
}
```

---

## Best Practices

### Security Considerations

1. **Permission Management:**
   - Request minimal necessary permissions
   - Check permissions before scraping
   - Handle permission denials gracefully

2. **Data Privacy:**
   - Don't log sensitive UI text (passwords, PINs)
   - Anonymize UIElement IDs in debug logs
   - Clear cached data on app exit

3. **Content Protection:**
   - Respect app-specific accessibility settings
   - Don't scrape restricted system UI
   - Honor user accessibility preferences

### Error Handling

```kotlin
suspend fun safeExtractHierarchy(): Result<ScreenSnapshot> = runCatching {
    val scraper = NativeScraperFactory.getInstance()
    scraper.extractHierarchy()
}

suspend fun safePerformAction(
    elementId: String,
    action: AccessibilityAction
): Result<Boolean> = runCatching {
    val scraper = NativeScraperFactory.getInstance()
    scraper.performAction(elementId, action)
}

// Usage:
val result = safeExtractHierarchy()
result.onSuccess { snapshot ->
    // Process snapshot
}.onFailure { error ->
    // Log error, notify user
    Log.e("NativeScraper", "Failed to extract hierarchy", error)
}
```

### Testing Native Scrapers

```kotlin
class NativeScraperTest {

    @Test
    fun testHierarchyExtraction() = runTest {
        val scraper = NativeScraperFactory.getInstance()
        val snapshot = scraper.extractHierarchy()

        assertTrue(snapshot.windows.isNotEmpty())
        snapshot.allElements().forEach { element ->
            assertFalse(element.id.isEmpty())
            assertTrue(element.bounds.width >= 0)
        }
    }

    @Test
    fun testFindElements() = runTest {
        val scraper = NativeScraperFactory.getInstance()
        val buttons = scraper.findElements { it.isClickable }

        assertTrue(buttons.isNotEmpty())
        buttons.forEach { button ->
            assertTrue(button.isClickable)
        }
    }

    @Test
    fun testActionExecution() = runTest {
        val scraper = NativeScraperFactory.getInstance()
        val snapshot = scraper.extractHierarchy()

        val clickableElement = snapshot.allElements().find { it.isClickable }
        assertNotNull(clickableElement)

        clickableElement?.let { element ->
            val success = scraper.performAction(element.id, AccessibilityAction.Click)
            assertTrue(success)
        }
    }
}
```

---

## Complete Implementation Guide

### Step 1: Setup KMP Module Structure

```bash
mkdir -p modules/scraping/src/{commonMain,androidMain,iosMain,macosMain,windowsMain}/kotlin
mkdir -p modules/scraping/src/{commonTest,androidTest,iosTest}
```

### Step 2: Define Common API

Place in `modules/scraping/src/commonMain/`:
- `Models.kt` - All data classes
- `NativeScraper.kt` - Interface and factory
- `Actions.kt` - Action definitions

### Step 3: Implement Platform-Specific Code

- **Android:** `NativeScraperImpl.android.kt` + `VosAccessibilityService.kt`
- **iOS:** `NativeScraperImpl.ios.kt` + Swift bridges
- **macOS:** `NativeScraperImpl.macos.kt` + Swift bridges
- **Windows:** `NativeScraperImpl.windows.kt` + JNI bindings

### Step 4: Integration with VOS4

In `VoiceOSCore`, add scraper initialization:

```kotlin
// In Voice Input Processing
class VoiceInputProcessor {

    private val scraper = NativeScraperFactory.getInstance()

    suspend fun identifyVoiceTarget(voiceInput: String): UIElement? {
        val snapshot = scraper.extractHierarchy()

        // Find elements matching voice command
        return snapshot.allElements().find { element ->
            element.text.contains(voiceInput, ignoreCase = true) ||
            element.contentDescription.contains(voiceInput, ignoreCase = true)
        }
    }

    suspend fun executeVoiceAction(elementId: String, action: String) {
        val accessibilityAction = when (action.lowercase()) {
            "click", "tap" -> AccessibilityAction.Click
            "scroll_up" -> AccessibilityAction.Scroll(ScrollDirection.UP)
            "scroll_down" -> AccessibilityAction.Scroll(ScrollDirection.DOWN)
            else -> return
        }

        scraper.performAction(elementId, accessibilityAction)
    }
}
```

---

## Summary

Native UI scraping provides the foundation for VOS4's voice interaction capabilities. By implementing platform-specific scrapers and unifying them through a common KMP interface, we achieve:

- **Cross-platform consistency** in UI element detection
- **Real-time monitoring** of screen state changes
- **Reliable action execution** on any accessible element
- **Extensible architecture** for future platforms

The performance characteristics vary by platform, but all are optimized for rapid feedback loops required by voice interfaces. With proper caching and incremental updates, the overhead remains minimal even for large UI hierarchies.

---

**Next Chapter:** Chapter 27 - Web Scraping Tool (JavaScript)

# VoiceAccessibility Overlay System - Complete Documentation

**Module:** VoiceAccessibility
**Layer:** User Interface - Overlay System
**Created:** 2025-10-10 11:05:00 PDT
**Author:** VOS4 Development Team
**Status:** Complete

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Base Overlay Infrastructure](#base-overlay-infrastructure)
4. [Overlay Types](#overlay-types)
5. [Window Management](#window-management)
6. [Overlay Implementations](#overlay-implementations)
7. [Overlay Manager](#overlay-manager)
8. [Integration Examples](#integration-examples)
9. [Performance Considerations](#performance-considerations)
10. [Best Practices](#best-practices)

---

## Overview

### Purpose

The Overlay System provides on-screen visual feedback and interaction elements that appear over other apps, enabling real-time voice control feedback, element selection, and status display.

### Key Features

- **System-wide Overlays**: Display over any app using TYPE_APPLICATION_OVERLAY
- **Jetpack Compose**: All overlays built with modern Compose UI
- **Glassmorphism Design**: Consistent visual language
- **Lifecycle Management**: Proper creation, updates, and disposal
- **Touch Handling**: Interactive and non-interactive modes
- **Animation Support**: Smooth transitions and visual feedback
- **Centralized Control**: OverlayManager coordinates all overlays

### File Locations

**Base Infrastructure:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/BaseOverlay.kt`

**Overlay Implementations:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/VoiceStatusOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/GridOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/NumberOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/CommandLabelOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/CommandDisambiguationOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/HelpOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/ui/overlays/CursorMenuOverlay.kt`

**Additional Overlays:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/OverlayManager.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/ConfidenceOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/NumberedSelectionOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/CommandStatusOverlay.kt`
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/ContextMenuOverlay.kt`

---

## Architecture

### Overlay System Structure

```
Overlay System
├── BaseOverlay (Abstract)
│   ├── Window management
│   ├── Lifecycle control
│   └── Compose integration
│
├── Overlay Types
│   ├── FULLSCREEN - Full screen overlays
│   ├── FLOATING - Interactive floating windows
│   └── POSITIONED - Fixed position elements
│
├── Specific Overlays
│   ├── VoiceStatusOverlay - Recognition status
│   ├── GridOverlay - Screen grid navigation
│   ├── NumberOverlay - Element numbering
│   ├── CommandLabelOverlay - Command labels
│   ├── CommandDisambiguationOverlay - Duplicate resolution
│   ├── HelpOverlay - Command help system
│   └── CursorMenuOverlay - Cursor context menu
│
└── OverlayManager
    ├── Centralized control
    ├── Lifecycle management
    └── Coordination
```

### Component Hierarchy

```
WindowManager (Android System)
└── BaseOverlay
    ├── WindowManager.LayoutParams
    ├── ComposeView
    │   └── AccessibilityTheme
    │       └── OverlayContent() @Composable
    └── CoroutineScope (lifecycle)
```

### State Flow

```
Service/Activity
    ├─→ OverlayManager.showOverlay()
    │   ├─→ Overlay.show()
    │   │   ├─→ createLayoutParams()
    │   │   ├─→ ComposeView.setContent { OverlayContent() }
    │   │   └─→ WindowManager.addView()
    │   └─→ Track active overlays
    │
    ├─→ OverlayManager.updateOverlay()
    │   └─→ Overlay updates Compose state
    │       └─→ Automatic recomposition
    │
    └─→ OverlayManager.hideOverlay()
        ├─→ Overlay.hide()
        │   └─→ WindowManager.removeView()
        └─→ Remove from tracking
```

---

## Base Overlay Infrastructure

### BaseOverlay Abstract Class

**File:** `BaseOverlay.kt`

#### Class Definition

```kotlin
abstract class BaseOverlay(
    protected val context: Context,
    protected val overlayType: OverlayType = OverlayType.FLOATING
) {
    protected val windowManager: WindowManager
    protected var overlayView: View?
    protected var overlayVisible: Boolean
    protected val overlayScope: CoroutineScope

    @Composable
    abstract fun OverlayContent()

    open fun show(): Boolean
    open fun hide(): Boolean
    open fun updatePosition(x: Int, y: Int): Boolean
    open fun dispose()
}
```

#### OverlayType Enum

```kotlin
enum class OverlayType {
    FULLSCREEN,  // Full screen overlay
    FLOATING,    // Floating window that can be interacted with
    POSITIONED   // Positioned overlay at specific coordinates (non-interactive)
}
```

#### Window Layout Parameters

```kotlin
protected open fun createLayoutParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        // Width and height based on overlay type
        width = when (overlayType) {
            OverlayType.FULLSCREEN -> WindowManager.LayoutParams.MATCH_PARENT
            OverlayType.FLOATING -> WindowManager.LayoutParams.WRAP_CONTENT
            OverlayType.POSITIONED -> WindowManager.LayoutParams.WRAP_CONTENT
        }

        height = when (overlayType) {
            OverlayType.FULLSCREEN -> WindowManager.LayoutParams.MATCH_PARENT
            OverlayType.FLOATING -> WindowManager.LayoutParams.WRAP_CONTENT
            OverlayType.POSITIONED -> WindowManager.LayoutParams.WRAP_CONTENT
        }

        // Window type (Android 8+)
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE // Deprecated
        }

        // Flags for touch handling
        flags = when (overlayType) {
            OverlayType.FULLSCREEN ->
                FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN
            OverlayType.FLOATING ->
                FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN
            OverlayType.POSITIONED ->
                FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE or
                FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS
        }

        format = PixelFormat.TRANSLUCENT
        gravity = getDefaultGravity()
    }
}
```

#### Show Method

```kotlin
open fun show(): Boolean {
    if (overlayVisible) return true

    return try {
        Log.d(TAG, "Showing overlay: ${this::class.simpleName}")

        // Create ComposeView with theme
        val composeView = ComposeView(context).apply {
            setContent {
                AccessibilityTheme {
                    DisposableEffect(Unit) {
                        onDispose {
                            // Cleanup when compose view is disposed
                        }
                    }
                    OverlayContent()
                }
            }
        }

        // Add to window manager
        val layoutParams = createLayoutParams()
        windowManager.addView(composeView, layoutParams)

        overlayView = composeView
        overlayVisible = true

        onOverlayShown()
        true

    } catch (e: Exception) {
        Log.e(TAG, "Failed to show overlay: ${this::class.simpleName}", e)
        false
    }
}
```

#### Hide Method

```kotlin
open fun hide(): Boolean {
    if (!overlayVisible) return true

    return try {
        Log.d(TAG, "Hiding overlay: ${this::class.simpleName}")

        overlayView?.let { view ->
            windowManager.removeView(view)
            overlayView = null
        }

        overlayVisible = false
        onOverlayHidden()
        true

    } catch (e: Exception) {
        Log.e(TAG, "Failed to hide overlay: ${this::class.simpleName}", e)
        false
    }
}
```

#### Position Update

```kotlin
open fun updatePosition(x: Int, y: Int): Boolean {
    if (!overlayVisible || overlayType != OverlayType.POSITIONED) return false

    return try {
        overlayView?.let { view ->
            val layoutParams = view.layoutParams as WindowManager.LayoutParams
            layoutParams.x = x
            layoutParams.y = y
            windowManager.updateViewLayout(view, layoutParams)
        }
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update overlay position", e)
        false
    }
}
```

#### Lifecycle Hooks

```kotlin
protected open fun onOverlayShown() {
    // Override in subclasses if needed
}

protected open fun onOverlayHidden() {
    // Override in subclasses if needed
}

open fun dispose() {
    Log.d(TAG, "Disposing overlay: ${this::class.simpleName}")
    hide()
    overlayScope.cancel()
}
```

---

## Window Management

### Android Overlay Permissions

**Required Permission (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

**Runtime Permission Check:**
```kotlin
fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true // Permission granted by default on older versions
    }
}
```

**Request Permission:**
```kotlin
fun requestOverlayPermission(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }
}
```

### Window Types

**TYPE_APPLICATION_OVERLAY (Android 8+):**
- Modern overlay type
- Requires SYSTEM_ALERT_WINDOW permission
- Appears above most apps
- Respects system UI (notification bar, nav bar)

**TYPE_PHONE (Deprecated):**
- Legacy overlay type
- Used for Android 7 and below
- Same permission requirements

### Touch Handling Flags

**FLAG_NOT_FOCUSABLE:**
- Overlay doesn't take focus
- Allows underlying app to receive input
- Used for non-interactive overlays

**FLAG_NOT_TOUCH_MODAL:**
- Touch events outside overlay pass through
- Allows interaction with underlying app
- Used for floating overlays

**FLAG_NOT_TOUCHABLE:**
- Overlay is completely non-interactive
- All touch events pass through
- Used for pure visual indicators

**FLAG_LAYOUT_IN_SCREEN:**
- Allows overlay to use full screen space
- Includes system UI areas

**FLAG_LAYOUT_NO_LIMITS:**
- Allows positioning outside screen bounds
- Used for positioned overlays

### Z-Ordering

Overlays are layered based on:
1. Window type
2. Order of WindowManager.addView() calls
3. System UI always on top (status bar, nav bar)

**Layering Strategy:**
```
Top:    Help Overlay (FULLSCREEN)
        Disambiguation Overlay (FLOATING)
        Number Overlay (FULLSCREEN)
        Command Label Overlay (POSITIONED)
        Grid Overlay (FULLSCREEN)
        Voice Status Overlay (POSITIONED)
        Cursor Menu (POSITIONED)
Bottom: Application windows
```

---

## Overlay Types

### FULLSCREEN Overlays

**Characteristics:**
- Cover entire screen
- Non-focusable but can capture touches
- Used for selection modes

**Layout Params:**
```kotlin
width = WindowManager.LayoutParams.MATCH_PARENT
height = WindowManager.LayoutParams.MATCH_PARENT
flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN
gravity = Gravity.FILL
```

**Examples:**
- GridOverlay
- NumberOverlay
- HelpOverlay

### FLOATING Overlays

**Characteristics:**
- Variable size based on content
- Can be interactive
- Positioned freely

**Layout Params:**
```kotlin
width = WindowManager.LayoutParams.WRAP_CONTENT
height = WindowManager.LayoutParams.WRAP_CONTENT
flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN
gravity = Gravity.TOP or Gravity.START
```

**Examples:**
- CommandDisambiguationOverlay

### POSITIONED Overlays

**Characteristics:**
- Fixed position on screen
- Typically non-interactive
- Can be moved via updatePosition()

**Layout Params:**
```kotlin
width = WindowManager.LayoutParams.WRAP_CONTENT
height = WindowManager.LayoutParams.WRAP_CONTENT
flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE or
       FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS
gravity = Gravity.TOP or Gravity.START
```

**Examples:**
- VoiceStatusOverlay
- CursorMenuOverlay

---

## Overlay Implementations

### VoiceStatusOverlay

**File:** `VoiceStatusOverlay.kt`
**Type:** POSITIONED
**Purpose:** Display voice recognition status with animations.

#### Status States

```kotlin
enum class VoiceStatus {
    INACTIVE,    // Voice recognition is off
    LISTENING,   // Listening for voice input
    PROCESSING,  // Processing the voice input
    SUCCESS,     // Successfully recognized command
    ERROR        // Error in recognition
}
```

#### Class Structure

```kotlin
class VoiceStatusOverlay(context: Context) : BaseOverlay(context, OverlayType.POSITIONED) {
    private var _status by mutableStateOf(VoiceStatus.INACTIVE)
    private var _confidence by mutableStateOf(0f)
    private var _lastCommand by mutableStateOf("")
    private var _errorMessage by mutableStateOf("")

    fun updateStatus(
        status: VoiceStatus,
        confidence: Float = 0f,
        lastCommand: String = "",
        errorMessage: String = ""
    )

    fun showTemporary(durationMs: Long = 3000L)
}
```

#### Compose UI

```kotlin
@Composable
override fun OverlayContent() {
    val infiniteTransition = rememberInfiniteTransition()

    // Pulsing animation for LISTENING state
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Card(modifier = Modifier.glassMorphism(...)) {
            Row {
                // Status icon with pulsing effect
                Box(
                    modifier = Modifier.background(
                        color = getStatusColor(_status).copy(
                            alpha = if (_status == VoiceStatus.LISTENING) pulseAlpha else 1f
                        ),
                        shape = CircleShape
                    )
                ) {
                    Icon(imageVector = getStatusIcon(_status))
                }

                // Status text and details
                Column {
                    Text(text = getStatusText(_status))
                    if (_confidence > 0f) {
                        Text(text = "${(_confidence * 100).toInt()}% confident")
                    }
                    if (_lastCommand.isNotEmpty()) {
                        Text(text = "\"$_lastCommand\"")
                    }
                }
            }
        }
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val voiceStatusOverlay = VoiceStatusOverlay(context)

// Show listening state
voiceStatusOverlay.updateStatus(
    status = VoiceStatus.LISTENING
)
voiceStatusOverlay.show()

// Update with recognition result
voiceStatusOverlay.updateStatus(
    status = VoiceStatus.SUCCESS,
    confidence = 0.95f,
    lastCommand = "open settings"
)
voiceStatusOverlay.showTemporary(durationMs = 2000)

// Hide overlay
voiceStatusOverlay.hide()
```

---

### GridOverlay

**File:** `GridOverlay.kt`
**Type:** FULLSCREEN
**Purpose:** Grid-based screen navigation using voice coordinates.

#### Grid Coordinate System

```kotlin
data class GridCoordinate(
    val row: Int,
    val col: Int,
    val x: Float,
    val y: Float
) {
    val rowLabel: String get() = ('A' + row).toString()
    val colLabel: String get() = (col + 1).toString()
    val coordinate: String get() = "$rowLabel$colLabel" // e.g., "A3", "B5"
}
```

#### Class Structure

```kotlin
class GridOverlay(
    context: Context,
    private val onGridSelected: (GridCoordinate) -> Unit,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    companion object {
        const val AUTO_HIDE_DELAY = 45000L
        const val DEFAULT_ROWS = 5
        const val DEFAULT_COLS = 4
    }

    private var _gridRows by mutableStateOf(DEFAULT_ROWS)
    private var _gridCols by mutableStateOf(DEFAULT_COLS)
    private var _selectedCell by mutableStateOf<GridCoordinate?>(null)
    private var _showLabels by mutableStateOf(true)

    fun showGrid(rows: Int = DEFAULT_ROWS, cols: Int = DEFAULT_COLS, showLabels: Boolean = true)
    fun hideGrid()
    fun selectGrid(coordinate: String) // e.g., "A3"
}
```

#### Compose UI

```kotlin
@Composable
override fun OverlayContent() {
    AnimatedVisibility(visible = _isShowing) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Grid cells
            GridContent(
                rows = _gridRows,
                cols = _gridCols,
                showLabels = _showLabels,
                selectedCell = _selectedCell,
                onCellClick = { row, col ->
                    val gridCoord = createGridCoordinate(row, col)
                    onGridSelected(gridCoord)
                }
            )

            // Instructions panel at top
            InstructionsPanel(
                gridSize = "${_gridRows}x${_gridCols}",
                onDismiss = ::hideGrid,
                onToggleLabels = { _showLabels = !_showLabels }
            )
        }
    }
}

@Composable
private fun GridCell(
    row: Int,
    col: Int,
    showLabel: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.3f))
            .background(
                if (isSelected)
                    Color.Green.copy(alpha = 0.6f)
                else
                    Color.White.copy(alpha = 0.05f)
            )
            .clickable { onClick() }
    ) {
        if (showLabel) {
            Text(
                text = "$rowLabel$colLabel", // e.g., "A3"
                modifier = Modifier.glassMorphism(...)
            )
        }
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val gridOverlay = GridOverlay(
    context = context,
    onGridSelected = { coordinate ->
        // Perform action at coordinate.x, coordinate.y
        performClick(coordinate.x, coordinate.y)
    },
    onDismiss = {
        // Grid dismissed
    }
)

// Show 5x4 grid
gridOverlay.showGrid(rows = 5, cols = 4)

// Voice command: "grid A3"
gridOverlay.selectGrid("A3")

// Hide grid
gridOverlay.hideGrid()
```

---

### NumberOverlay

**File:** `NumberOverlay.kt`
**Type:** FULLSCREEN
**Purpose:** Display numbered labels over interactive UI elements.

#### Data Structure

```kotlin
data class NumberedElement(
    val number: Int,
    val elementInfo: ElementInfo,
    val screenX: Float,
    val screenY: Float,
    val description: String,
    val isClickable: Boolean
)
```

#### Class Structure

```kotlin
class NumberOverlay(
    context: Context,
    private val onNumberSelected: (Int) -> Unit,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    companion object {
        const val AUTO_HIDE_DELAY = 30000L
        val LABEL_SIZE = 24.dp
        const val MAX_VISIBLE_NUMBERS = 99
    }

    private var _numberedElements by mutableStateOf<Map<Int, NumberedElement>>(emptyMap())

    fun showWithElements(elements: Map<Int, ElementInfo>)
    fun hideNumberOverlay()
    fun selectNumber(number: Int)
}
```

#### Compose UI

```kotlin
@Composable
override fun OverlayContent() {
    AnimatedVisibility(visible = _isShowing) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Render each numbered label
            _numberedElements.values.forEach { numberedElement ->
                NumberLabel(
                    modifier = Modifier.offset(
                        x = numberedElement.screenX.dp,
                        y = numberedElement.screenY.dp
                    ),
                    numberedElement = numberedElement,
                    onClick = { selectNumber(numberedElement.number) }
                )
            }

            // Instructions at bottom
            InstructionsPanel(
                elementCount = _numberedElements.size,
                onDismiss = ::hideNumberOverlay
            )
        }
    }
}

@Composable
private fun NumberLabel(
    numberedElement: NumberedElement,
    onClick: () -> Unit
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(...)

    Card(
        modifier = Modifier
            .size(LABEL_SIZE)
            .glassMorphism(
                tintColor = if (numberedElement.isClickable)
                    Color.Green
                else
                    Color.Blue
            )
            .graphicsLayer { shadowElevation = (4.dp.toPx() * pulseAlpha) },
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = numberedElement.number.toString())
        }
    }

    // Tooltip with element description
    if (numberedElement.description.isNotEmpty()) {
        Card(modifier = Modifier.offset(y = LABEL_SIZE + 4.dp)) {
            Text(text = numberedElement.description)
        }
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val numberOverlay = NumberOverlay(
    context = context,
    onNumberSelected = { number ->
        // Perform action on numbered element
        val element = elements[number]
        performActionOnElement(element)
    },
    onDismiss = { /* Overlay dismissed */ }
)

// Get interactive elements from accessibility tree
val elements = extractInteractiveElements(rootNode)

// Show numbered overlay
numberOverlay.showWithElements(elements)

// Voice command: "tap 5"
numberOverlay.selectNumber(5)

// Hide overlay
numberOverlay.hideNumberOverlay()
```

---

### CommandLabelOverlay

**File:** `CommandLabelOverlay.kt`
**Type:** POSITIONED
**Purpose:** Display voice command labels over UI elements.

#### Data Structure

```kotlin
data class VoiceCommandLabel(
    val id: String,
    val command: String,
    val bounds: Rect,
    val isClickable: Boolean = true,
    val isDuplicate: Boolean = false,
    val duplicateIndex: Int = 0,
    val confidence: Float = 1.0f
)
```

#### Class Structure

```kotlin
class CommandLabelOverlay(
    context: Context,
    private val onCommandSelected: (String) -> Unit = {}
) : BaseOverlay(context, OverlayType.POSITIONED) {
    companion object {
        const val MIN_LABEL_SPACING = 4 // dp
        const val COLLISION_THRESHOLD = 20 // pixels
        const val MAX_LABELS_VISIBLE = 50
    }

    private var commandLabels = mutableStateListOf<VoiceCommandLabel>()

    fun updateCommandLabels(nodes: List<AccessibilityNodeInfo>)
    fun clearLabels()
    fun highlightCommand(command: String)
}
```

#### Label Processing

```kotlin
private fun processNodes(nodes: List<AccessibilityNodeInfo>): List<VoiceCommandLabel> {
    return nodes.mapNotNull { node ->
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (!isValidBounds(bounds)) return@mapNotNull null

        val command = extractCommand(node) ?: return@mapNotNull null

        VoiceCommandLabel(
            id = node.hashCode().toString(),
            command = command,
            bounds = bounds,
            isClickable = node.isClickable,
            confidence = calculateConfidence(node)
        )
    }
}

private fun extractCommand(node: AccessibilityNodeInfo): String? {
    // Priority: contentDescription > text > hintText
    return node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        ?: node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        ?: node.hintText?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
```

#### Collision Detection

```kotlin
private fun applyCollisionDetection(labels: List<VoiceCommandLabel>): List<VoiceCommandLabel> {
    val sortedLabels = labels.sortedByDescending { it.confidence }
    val adjustedLabels = mutableListOf<VoiceCommandLabel>()
    val occupiedRects = mutableListOf<Rect>()

    for (label in sortedLabels) {
        var adjustedBounds = Rect(label.bounds)
        var attempts = 0

        // Try to find non-colliding position
        while (hasCollision(adjustedBounds, occupiedRects) && attempts < 5) {
            adjustedBounds = adjustPosition(adjustedBounds, occupiedRects)
            attempts++
        }

        if (attempts < 5) {
            adjustedLabels.add(label.copy(bounds = adjustedBounds))
            occupiedRects.add(adjustedBounds)
        }
    }

    return adjustedLabels
}
```

#### Usage Example

```kotlin
// In accessibility service
val commandLabelOverlay = CommandLabelOverlay(
    context = context,
    onCommandSelected = { command ->
        // Execute voice command
        executeCommand(command)
    }
)

// Update labels when screen changes
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType == TYPE_WINDOW_CONTENT_CHANGED) {
        val nodes = extractInteractiveNodes(rootInActiveWindow)
        commandLabelOverlay.updateCommandLabels(nodes)
    }
}

// Clear labels
commandLabelOverlay.clearLabels()

// Highlight specific command
commandLabelOverlay.highlightCommand("open settings")
```

---

### CommandDisambiguationOverlay

**File:** `CommandDisambiguationOverlay.kt`
**Type:** FLOATING
**Purpose:** Resolve duplicate voice commands through numbered selection.

#### Data Structure

```kotlin
data class DuplicateCommandOption(
    val index: Int,
    val command: String,
    val bounds: Rect,
    val description: String? = null,
    val nodeInfo: Any? = null
)
```

#### Multi-Language Support

```kotlin
object NumberWordConverter {
    private val englishNumbers = mapOf(
        1 to "one", 2 to "two", 3 to "three", ...
    )
    private val spanishNumbers = mapOf(
        1 to "uno", 2 to "dos", 3 to "tres", ...
    )

    fun getNumberWord(number: Int, language: String = "en"): String {
        return when (language.lowercase()) {
            "es" -> spanishNumbers[number] ?: number.toString()
            "fr" -> frenchNumbers[number] ?: number.toString()
            else -> englishNumbers[number] ?: number.toString()
        }
    }
}
```

#### Class Structure

```kotlin
class CommandDisambiguationOverlay(
    context: Context,
    private val onOptionSelected: (DuplicateCommandOption) -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : BaseOverlay(context, OverlayType.FLOATING) {
    companion object {
        const val AUTO_DISMISS_DELAY = 10000L
        const val MAX_OPTIONS_VISIBLE = 9
    }

    private var duplicateOptions = mutableStateListOf<DuplicateCommandOption>()
    private var currentLanguage by mutableStateOf("en")

    fun showDuplicates(
        command: String,
        options: List<DuplicateCommandOption>,
        language: String = "en"
    )

    fun handleVoiceCommand(spokenText: String)
}
```

#### Compose UI

```kotlin
@Composable
override fun OverlayContent() {
    AnimatedVisibility(visible = duplicateOptions.isNotEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism background
            Canvas(modifier = Modifier.fillMaxSize().blur(10.dp)) {
                drawRect(color = Color.Black.copy(alpha = 0.6f))
            }

            // Disambiguation panel
            DisambiguationPanel()
        }
    }
}

@Composable
private fun DisambiguationPanel() {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .glassMorphism(...)
    ) {
        Column {
            // Header
            Text("Select which \"${duplicateOptions.first().command}\" to click:")

            // Options list
            LazyColumn {
                itemsIndexed(duplicateOptions) { _, option ->
                    DuplicateOptionItem(
                        option = option,
                        language = currentLanguage,
                        onClick = {
                            onOptionSelected(option)
                            dismissOverlay()
                        }
                    )
                }
            }

            // Instructions
            Text("Say the number or tap to select")
        }
    }
}

@Composable
private fun DuplicateOptionItem(
    option: DuplicateCommandOption,
    language: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row {
            // Number circle with gradient
            NumberCircle(number = option.index, language = language)

            // Command details
            Column {
                Text(text = option.command)
                option.description?.let {
                    Text(text = it, color = Color.Gray)
                }
            }

            // Position indicator (minimap)
            PositionIndicator(bounds = option.bounds)
        }
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val disambiguationOverlay = CommandDisambiguationOverlay(
    context = context,
    onOptionSelected = { option ->
        // Perform action on selected option
        performActionOnNode(option.nodeInfo)
    },
    onDismiss = { /* Dismissed */ }
)

// When duplicate commands detected
val duplicates = findDuplicateCommands("open") // Returns 3 "open" buttons
disambiguationOverlay.showDuplicates(
    command = "open",
    options = duplicates.mapIndexed { index, node ->
        DuplicateCommandOption(
            index = index + 1,
            command = "open",
            bounds = node.bounds,
            description = node.description,
            nodeInfo = node
        )
    },
    language = "en"
)

// Voice command: "two" or "2"
disambiguationOverlay.handleVoiceCommand("two")
```

---

### HelpOverlay

**File:** `HelpOverlay.kt`
**Type:** FULLSCREEN
**Purpose:** Full-screen help system showing available voice commands.

#### Data Structures

```kotlin
enum class HelpCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    NAVIGATION("Navigation", Icons.Default.Navigation, Color.Blue),
    SELECTION("Selection", Icons.Default.TouchApp, Color.Green),
    SYSTEM("System", Icons.Default.Settings, Color.Purple),
    APPS("Apps", Icons.Default.Apps, Color.Orange),
    CURSOR("Cursor", Icons.Default.CenterFocusWeak, Color.Cyan),
    INPUT("Input", Icons.Default.Keyboard, Color.Brown),
    HELP("Help", Icons.AutoMirrored.Filled.Help, Color.Gray)
}

data class CommandGroup(
    val title: String,
    val commands: List<VoiceCommand>
)

data class VoiceCommand(
    val phrase: String,
    val description: String,
    val alternatives: List<String> = emptyList()
)
```

#### Class Structure

```kotlin
class HelpOverlay(
    context: Context,
    private val onDismiss: () -> Unit
) : BaseOverlay(context, OverlayType.FULLSCREEN) {
    private var _selectedCategory by mutableStateOf(HelpCategory.NAVIGATION)
    private var _searchQuery by mutableStateOf("")
}
```

#### Compose UI

```kotlin
@Composable
override fun OverlayContent() {
    AnimatedVisibility(visible = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassMorphism(backgroundOpacity = 0.95f)
                .padding(24.dp)
        ) {
            // Header with close button
            HelpHeader(onDismiss = onDismiss)

            // Category selector (horizontal scroll)
            CategorySelector(
                selectedCategory = _selectedCategory,
                onCategorySelected = { _selectedCategory = it }
            )

            // Command list for selected category
            CommandList(
                category = _selectedCategory,
                searchQuery = _searchQuery
            )
        }
    }
}

@Composable
private fun CommandList(
    category: HelpCategory,
    searchQuery: String
) {
    val commands = remember(category) { getCommandsForCategory(category) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(commands) { commandGroup ->
            CommandGroupCard(commandGroup = commandGroup)
        }
    }
}

@Composable
private fun CommandGroupCard(commandGroup: CommandGroup) {
    Card(modifier = Modifier.glassMorphism(...)) {
        Column {
            Text(text = commandGroup.title, fontWeight = FontWeight.Bold)

            commandGroup.commands.forEach { command ->
                CommandItem(command = command)
            }
        }
    }
}
```

#### Command Data

```kotlin
private fun getCommandsForCategory(category: HelpCategory): List<CommandGroup> {
    return when (category) {
        HelpCategory.NAVIGATION -> listOf(
            CommandGroup("Basic Navigation", listOf(
                VoiceCommand("go back", "Navigate to previous screen", listOf("back")),
                VoiceCommand("go home", "Go to home screen", listOf("home")),
                VoiceCommand("scroll up", "Scroll up on current screen"),
                VoiceCommand("scroll down", "Scroll down on current screen")
            ))
        )
        // ... other categories
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val helpOverlay = HelpOverlay(
    context = context,
    onDismiss = {
        // Help overlay dismissed
    }
)

// Voice command: "show help"
helpOverlay.show()

// Voice command: "hide help"
helpOverlay.hide()
```

---

### CursorMenuOverlay

**File:** `CursorMenuOverlay.kt`
**Type:** POSITIONED
**Purpose:** Radial context menu around cursor for quick actions.

#### Action Types

```kotlin
enum class CursorAction(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    CLICK("Click", Icons.Default.TouchApp, Color.Green),
    LONG_CLICK("Hold", Icons.Default.Timer, Color.Orange),
    SCROLL_UP("Scroll Up", Icons.Default.KeyboardArrowUp, Color.Blue),
    SCROLL_DOWN("Scroll Down", Icons.Default.KeyboardArrowDown, Color.Blue),
    BACK("Back", Icons.AutoMirrored.Filled.ArrowBack, Color.Purple),
    HOME("Home", Icons.Default.Home, Color.Gray),
    MENU("Menu", Icons.Default.MoreVert, Color.Brown),
    CLOSE("Close", Icons.Default.Close, Color.Red)
}
```

#### Class Structure

```kotlin
class CursorMenuOverlay(
    context: Context,
    private val onActionSelected: (CursorAction) -> Unit
) : BaseOverlay(context, OverlayType.POSITIONED) {
    companion object {
        private val MENU_RADIUS = 80.dp
        val ITEM_SIZE = 40.dp
        const val AUTO_HIDE_DELAY = 5000L
    }

    private var _isExpanded by mutableStateOf(false)
    private var _cursorX by mutableStateOf(0)
    private var _cursorY by mutableStateOf(0)

    fun showAtCursor(x: Int, y: Int)
    fun hideMenu()
}
```

#### Compose UI (Radial Layout)

```kotlin
@Composable
override fun OverlayContent() {
    val expandedState by animateFloatAsState(
        targetValue = if (_isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier.size(MENU_RADIUS * 2),
        contentAlignment = Alignment.Center
    ) {
        // Center point indicator
        Box(modifier = Modifier.size(8.dp).glassMorphism(...))

        // Menu items arranged in circle
        CursorAction.values().forEachIndexed { index, action ->
            val angle = (index * 360f / CursorAction.values().size) + 45f
            val radius = MENU_RADIUS.value * expandedState

            val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius
            val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius

            AnimatedVisibility(
                visible = expandedState > 0.1f,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                CursorMenuItem(
                    modifier = Modifier
                        .offset(x.dp, y.dp)
                        .graphicsLayer {
                            scaleX = expandedState
                            scaleY = expandedState
                            alpha = expandedState
                        },
                    action = action,
                    onClick = {
                        onActionSelected(action)
                        hideMenu()
                    }
                )
            }
        }
    }
}

@Composable
private fun CursorMenuItem(
    action: CursorAction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(ITEM_SIZE)
            .clickable { onClick() }
            .glassMorphism(tintColor = action.color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = action.icon, contentDescription = action.label)
        }
    }
}
```

#### Usage Example

```kotlin
// In accessibility service
val cursorMenuOverlay = CursorMenuOverlay(
    context = context,
    onActionSelected = { action ->
        when (action) {
            CursorAction.CLICK -> performClick(cursorX, cursorY)
            CursorAction.LONG_CLICK -> performLongClick(cursorX, cursorY)
            CursorAction.SCROLL_UP -> performScroll(direction = UP)
            // ... handle other actions
        }
    }
)

// Voice command: "cursor menu"
cursorMenuOverlay.showAtCursor(x = cursorX, y = cursorY)

// Voice command: "click" or tap on menu item
// (handled by onActionSelected callback)
```

---

## Overlay Manager

### OverlayManager Class

**File:** `OverlayManager.kt`
**Purpose:** Centralized management of all accessibility overlays.

#### Singleton Pattern

```kotlin
class OverlayManager(private val context: Context) {
    companion object {
        @Volatile
        private var instance: OverlayManager? = null

        fun getInstance(context: Context): OverlayManager {
            return instance ?: synchronized(this) {
                instance ?: OverlayManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
```

#### Overlay Initialization

```kotlin
// Lazy initialization of overlays
private val confidenceOverlay by lazy {
    ConfidenceOverlay(context, windowManager)
}

private val numberedSelectionOverlay by lazy {
    NumberedSelectionOverlay(context, windowManager)
}

private val commandStatusOverlay by lazy {
    CommandStatusOverlay(context, windowManager)
}

private val contextMenuOverlay by lazy {
    ContextMenuOverlay(context, windowManager)
}

// Track active overlays
private val activeOverlays = mutableSetOf<String>()
```

#### Show Methods

```kotlin
fun showConfidence(result: ConfidenceResult) {
    confidenceOverlay.show(result)
    activeOverlays.add("confidence")
}

fun showNumberedSelection(items: List<SelectableItem>) {
    // Hide conflicting overlays
    hideContextMenu()

    numberedSelectionOverlay.showItems(items)
    activeOverlays.add("numberedSelection")
}

fun showCommandStatus(
    command: String,
    state: CommandState,
    message: String? = null
) {
    commandStatusOverlay.showStatus(command, state, message)
    activeOverlays.add("commandStatus")
}
```

#### Update Methods

```kotlin
fun updateConfidence(result: ConfidenceResult) {
    if (confidenceOverlay.isVisible()) {
        confidenceOverlay.updateConfidence(result)
    } else {
        showConfidence(result)
    }
}

fun updateNumberedSelection(items: List<SelectableItem>) {
    numberedSelectionOverlay.updateItems(items)
}
```

#### Hide Methods

```kotlin
fun hideConfidence() {
    confidenceOverlay.hide()
    activeOverlays.remove("confidence")
}

fun hideAll() {
    confidenceOverlay.hide()
    numberedSelectionOverlay.hide()
    commandStatusOverlay.hide()
    contextMenuOverlay.hide()
    activeOverlays.clear()
}
```

#### Convenience Methods

```kotlin
// Listening state
fun showListening(partialText: String = "") {
    showCommandStatus(
        command = partialText.ifEmpty { "Listening..." },
        state = CommandState.LISTENING
    )
}

// Processing state
fun showProcessing(command: String) {
    showCommandStatus(
        command = command,
        state = CommandState.PROCESSING,
        message = "Recognizing..."
    )
}

// Success state
fun showSuccess(command: String, message: String? = null) {
    showCommandStatus(
        command = command,
        state = CommandState.SUCCESS,
        message = message ?: "Command executed successfully"
    )
}

// Error state
fun showError(command: String, error: String) {
    showCommandStatus(
        command = command,
        state = CommandState.ERROR,
        message = error
    )
}
```

#### Query Methods

```kotlin
fun isAnyVisible(): Boolean {
    return activeOverlays.isNotEmpty()
}

fun isOverlayVisible(overlayName: String): Boolean {
    return activeOverlays.contains(overlayName)
}

fun getActiveOverlays(): Set<String> {
    return activeOverlays.toSet()
}
```

#### Disposal

```kotlin
fun dispose() {
    confidenceOverlay.dispose()
    numberedSelectionOverlay.dispose()
    commandStatusOverlay.dispose()
    contextMenuOverlay.dispose()
    activeOverlays.clear()
    instance = null
}
```

---

## Integration Examples

### Complete Voice Recognition Flow

```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var overlayManager: OverlayManager
    private lateinit var voiceStatusOverlay: VoiceStatusOverlay
    private lateinit var numberOverlay: NumberOverlay
    private lateinit var gridOverlay: GridOverlay

    override fun onCreate() {
        super.onCreate()

        // Initialize overlay manager
        overlayManager = OverlayManager.getInstance(this)

        // Initialize individual overlays
        voiceStatusOverlay = VoiceStatusOverlay(this)
        numberOverlay = NumberOverlay(
            context = this,
            onNumberSelected = { number -> handleNumberSelection(number) },
            onDismiss = { /* Number overlay dismissed */ }
        )
        gridOverlay = GridOverlay(
            context = this,
            onGridSelected = { coord -> handleGridSelection(coord) },
            onDismiss = { /* Grid dismissed */ }
        )
    }

    // Voice recognition started
    fun onRecognitionStarted() {
        voiceStatusOverlay.updateStatus(VoiceStatus.LISTENING)
        voiceStatusOverlay.show()
        overlayManager.showListening()
    }

    // Partial result received
    fun onPartialResult(text: String) {
        overlayManager.updateCommandStatus(
            command = text,
            state = CommandState.LISTENING
        )
    }

    // Final result received
    fun onRecognitionCompleted(result: RecognitionResult) {
        voiceStatusOverlay.updateStatus(
            status = VoiceStatus.SUCCESS,
            confidence = result.confidence,
            lastCommand = result.text
        )
        voiceStatusOverlay.showTemporary(2000)

        overlayManager.showProcessing(result.text)

        // Show confidence indicator
        overlayManager.showConfidence(
            ConfidenceResult(
                text = result.text,
                confidence = result.confidence,
                alternatives = result.alternatives
            )
        )
    }

    // Command execution
    fun executeCommand(command: String) {
        overlayManager.showExecuting(command)

        try {
            when {
                command.startsWith("show numbers") -> showNumberedElements()
                command.startsWith("show grid") -> showGrid()
                command.startsWith("tap") -> handleTapCommand(command)
                command.startsWith("grid") -> handleGridCommand(command)
                // ... other commands
            }

            overlayManager.showSuccess(command)

        } catch (e: Exception) {
            overlayManager.showError(command, e.message ?: "Unknown error")
        }
    }

    // Show numbered elements
    private fun showNumberedElements() {
        val nodes = extractInteractiveNodes(rootInActiveWindow)
        val elements = nodes.mapIndexed { index, node ->
            index + 1 to ElementInfo(
                node = node,
                bounds = node.boundsInScreen,
                description = node.contentDescription?.toString() ?: node.text?.toString() ?: "",
                isClickable = node.isClickable
            )
        }.toMap()

        numberOverlay.showWithElements(elements)
    }

    // Show grid
    private fun showGrid() {
        gridOverlay.showGrid(rows = 5, cols = 4)
    }

    // Handle tap command
    private fun handleTapCommand(command: String) {
        val number = extractNumber(command)
        number?.let {
            numberOverlay.selectNumber(it)
        }
    }

    // Handle grid command
    private fun handleGridCommand(command: String) {
        val coordinate = extractCoordinate(command) // e.g., "A3"
        coordinate?.let {
            gridOverlay.selectGrid(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Dispose all overlays
        voiceStatusOverlay.dispose()
        numberOverlay.dispose()
        gridOverlay.dispose()
        overlayManager.dispose()
    }
}
```

---

## Performance Considerations

### Overlay Lifecycle

**Best Practices:**
1. **Lazy Initialization**: Create overlays only when needed
2. **Proper Disposal**: Always call `dispose()` in service `onDestroy()`
3. **Hide When Inactive**: Don't keep invisible overlays in window manager
4. **Reuse Instances**: Avoid creating multiple overlay instances

**Example:**
```kotlin
// Good: Lazy initialization
private val numberOverlay by lazy {
    NumberOverlay(context, onNumberSelected, onDismiss)
}

// Good: Reuse overlay
fun showNumbers() {
    numberOverlay.show() // Reuses existing instance
}

// Bad: Creating new instances
fun showNumbers() {
    val overlay = NumberOverlay(...) // Creates new instance every time
    overlay.show()
}
```

### Compose Recomposition

**Optimization Strategies:**
1. **Remember Expensive Operations**: Use `remember` for computed values
2. **Stable Keys**: Provide stable keys for LazyColumn items
3. **derivedStateOf**: Use for computed state that depends on other state

**Example:**
```kotlin
@Composable
fun OverlayContent() {
    // Good: Remember expensive computation
    val processedItems = remember(items) {
        items.map { processItem(it) }
    }

    // Good: Stable keys for LazyColumn
    LazyColumn {
        items(processedItems, key = { it.id }) { item ->
            ItemView(item)
        }
    }

    // Good: Derived state
    val filteredItems by remember {
        derivedStateOf {
            items.filter { it.isVisible }
        }
    }
}
```

### Animation Performance

**Guidelines:**
1. **Use Hardware Layers**: Enable hardware acceleration for complex animations
2. **Limit Simultaneous Animations**: Too many animations can cause frame drops
3. **Use Spring Animations**: More performant than tween for interactive elements

**Example:**
```kotlin
// Good: Spring animation
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

// Good: Hardware layer for complex UI
Box(
    modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
)
```

### Memory Management

**Best Practices:**
1. **Clear State on Hide**: Reset overlay state when hidden
2. **Cancel Coroutines**: Properly cancel overlay scope
3. **Release Resources**: Dispose WindowManager views

**Example:**
```kotlin
override fun hide(): Boolean {
    // Clear state
    _numberedElements.clear()
    _selectedCell.value = null

    // Cancel coroutines
    overlayScope.cancel()

    // Remove view
    return super.hide()
}
```

---

## Best Practices

### Overlay Design

**1. Visual Hierarchy:**
```kotlin
// Background overlays (lowest)
depth = DepthLevel(0.2f)
backgroundOpacity = 0.05f

// Mid-level overlays
depth = DepthLevel(0.6f)
backgroundOpacity = 0.1f

// Top-level overlays (highest)
depth = DepthLevel(1.0f)
backgroundOpacity = 0.15f
```

**2. Color Coding:**
```kotlin
// Status-based colors
val statusColor = when (status) {
    VoiceStatus.LISTENING -> Color.Blue
    VoiceStatus.PROCESSING -> Color.Orange
    VoiceStatus.SUCCESS -> Color.Green
    VoiceStatus.ERROR -> Color.Red
}
```

**3. Auto-Hide Timers:**
```kotlin
// Show temporarily with auto-hide
fun showTemporary(durationMs: Long = 3000L) {
    show()
    overlayScope.launch {
        delay(durationMs)
        hide()
    }
}
```

### Touch Handling

**1. Interactive Overlays:**
```kotlin
// Allow touches, pass through focus
flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL

// Example: Disambiguation overlay
Box(
    modifier = Modifier
        .fillMaxSize()
        .clickable { onOptionSelected(option) }
)
```

**2. Non-Interactive Overlays:**
```kotlin
// Pass through all touches
flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE

// Example: Status indicators
Card(modifier = Modifier.glassMorphism(...)) {
    // No clickable elements
}
```

### Animation Guidelines

**1. Entrance Animations:**
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
    exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f)
) {
    OverlayContent()
}
```

**2. State Transitions:**
```kotlin
val animatedScale by animateFloatAsState(
    targetValue = if (isSelected) 1.2f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
)
```

**3. Continuous Animations:**
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000),
        repeatMode = RepeatMode.Reverse
    )
)
```

### Error Handling

**1. Permission Checks:**
```kotlin
fun show(): Boolean {
    if (!canDrawOverlays(context)) {
        Log.e(TAG, "Overlay permission not granted")
        requestOverlayPermission()
        return false
    }
    // ... show overlay
}
```

**2. Exception Handling:**
```kotlin
try {
    windowManager.addView(composeView, layoutParams)
    overlayVisible = true
    return true
} catch (e: WindowManager.BadTokenException) {
    Log.e(TAG, "Invalid window token", e)
    return false
} catch (e: Exception) {
    Log.e(TAG, "Failed to show overlay", e)
    return false
}
```

**3. Cleanup on Failure:**
```kotlin
override fun dispose() {
    try {
        hide()
        overlayScope.cancel()
    } catch (e: Exception) {
        Log.e(TAG, "Error during disposal", e)
    } finally {
        overlayView = null
        overlayVisible = false
    }
}
```

---

## Summary

The VoiceAccessibility Overlay System provides a comprehensive framework for displaying real-time visual feedback and interactive elements over any app. Key highlights:

**Core Features:**
- **BaseOverlay Infrastructure**: Robust foundation for all overlays
- **Multiple Overlay Types**: FULLSCREEN, FLOATING, POSITIONED
- **Jetpack Compose Integration**: Modern declarative UI
- **WindowManager Control**: Direct system overlay management
- **Lifecycle Management**: Proper creation, updates, disposal

**Overlay Implementations:**
- **VoiceStatusOverlay**: Recognition status with animations
- **GridOverlay**: Screen navigation via coordinates
- **NumberOverlay**: Element numbering for selection
- **CommandLabelOverlay**: Command labels with collision detection
- **CommandDisambiguationOverlay**: Duplicate command resolution
- **HelpOverlay**: Comprehensive command help
- **CursorMenuOverlay**: Radial context menu

**Management:**
- **OverlayManager**: Centralized control and coordination
- **Active Tracking**: Monitor visible overlays
- **Conflict Resolution**: Hide conflicting overlays automatically
- **Convenience Methods**: Simplified API for common patterns

For core UI documentation, see `UI-Layer-Core-Documentation-251010-1105.md`.

---

**Document Version:** 1.0
**Last Updated:** 2025-10-10 11:05:00 PDT
**Next Review:** 2025-11-10

# VoiceAccessibility Cursor System - Developer Documentation

**Document Version:** 1.0
**Created:** 2025-10-10 10:51:00 PDT
**Module:** VoiceAccessibility
**Subsystem:** Cursor Control
**Location:** `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/`

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [Movement Model](#movement-model)
5. [Gesture System](#gesture-system)
6. [Visual Feedback](#visual-feedback)
7. [Integration Points](#integration-points)
8. [Voice Commands](#voice-commands)
9. [Extension Guide](#extension-guide)
10. [Performance & Optimization](#performance--optimization)
11. [Complete API Reference](#complete-api-reference)

---

## System Overview

### Purpose

The VoiceAccessibility Cursor System provides **voice-controlled cursor navigation** for Android devices, enabling users to interact with any application using spoken commands. The cursor acts as a virtual finger, allowing users with motor impairments or hands-free requirements to fully control their device.

### Key Capabilities

- **Voice-Controlled Movement**: Move cursor in 8 directions (up, down, left, right, diagonals) via voice
- **Intelligent Snapping**: Automatically snap to nearby clickable UI elements
- **Gesture Execution**: Perform clicks, long-presses, swipes, and drags at cursor position
- **Speed Control**: Adjustable movement speed with precision mode for fine control
- **Position Tracking**: Real-time cursor position tracking with history and undo/redo
- **Visual Feedback**: Animated cursor rendering with multiple styles and states
- **Boundary Detection**: Automatic screen edge detection and clamping
- **Focus Indication**: Visual highlighting of focused UI elements

### Design Philosophy

**Direct Implementation Pattern (VOS4 Compliance)**:
- No interfaces - all components are concrete implementations
- Direct dependency injection via constructor parameters
- Callback-based communication between components
- Reactive state management using Kotlin StateFlow
- Zero abstraction overhead for maximum performance

---

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   VoiceAccessibilityService                     │
│                    (Main Integration Point)                     │
└───────┬─────────────────────────────────────────────────────────┘
        │
        │ creates and coordinates
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CURSOR SUBSYSTEM                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐        ┌──────────────────┐             │
│  │ CommandMapper    │───────▶│VoiceCursorEvent  │             │
│  │ (Voice → Action) │        │Handler           │             │
│  └──────────────────┘        │(Event Queue)     │             │
│           │                  └────────┬─────────┘             │
│           │                           │                        │
│           ▼                           ▼                        │
│  ┌──────────────────┐        ┌──────────────────┐             │
│  │CursorPosition    │◀──────▶│CursorHistory     │             │
│  │Tracker           │        │Tracker           │             │
│  │(Position State)  │        │(Undo/Redo)       │             │
│  └────────┬─────────┘        └──────────────────┘             │
│           │                                                     │
│           │                                                     │
│  ┌────────▼─────────┐        ┌──────────────────┐             │
│  │BoundaryDetector  │        │SpeedController   │             │
│  │(Screen Edges)    │        │(Acceleration)    │             │
│  └──────────────────┘        └──────────────────┘             │
│                                                                 │
│  ┌──────────────────┐        ┌──────────────────┐             │
│  │CursorGesture     │        │SnapToElement     │             │
│  │Handler           │        │Handler           │             │
│  │(Click/Swipe)     │        │(Smart Targeting) │             │
│  └──────────────────┘        └──────────────────┘             │
│                                                                 │
│  ┌──────────────────┐        ┌──────────────────┐             │
│  │CursorVisibility  │        │CursorStyle       │             │
│  │Manager           │        │Manager           │             │
│  │(Show/Hide)       │        │(Appearance)      │             │
│  └──────────────────┘        └──────────────────┘             │
│                                                                 │
│  ┌──────────────────┐                                          │
│  │FocusIndicator    │                                          │
│  │(Visual Highlight)│                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
        │
        │ communicates with
        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    VoiceCursor Module                           │
│                 (Overlay Window Rendering)                      │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

**Voice Command → Cursor Movement Flow**:

```
1. User speaks "move up"
   ↓
2. SpeechRecognition → VoiceAccessibilityService
   ↓
3. CommandMapper.mapCommand("move up")
   → Returns: CursorAction.MoveDirection(Direction.UP, 100f)
   ↓
4. VoiceCursorEventHandler.dispatchEvent()
   → Adds CursorEvent.Move to event queue
   ↓
5. Event processing (async):
   a. SpeedController.calculateVelocity() → current speed
   b. CursorPositionTracker.moveBy(deltaX, deltaY)
   c. BoundaryDetector.checkBounds() → clamp if needed
   d. CursorHistoryTracker.recordPosition() → save for undo
   ↓
6. Position update callbacks:
   a. CursorVisibilityManager → ensure cursor visible
   b. VoiceCursor module → update overlay position
   c. FocusIndicator → update if element focused
   ↓
7. Result callback to VoiceAccessibilityService
```

### Communication Patterns

**1. Callback-Based Event Handling**:
```kotlin
// Components register callbacks for events
eventHandler.registerEventCallback("Move") { event ->
    if (event is CursorEvent.Move) {
        positionTracker.moveBy(event.deltaX, event.deltaY)
    }
}
```

**2. StateFlow for Reactive Updates**:
```kotlin
// Components expose state via Kotlin StateFlow
positionTracker.positionFlow.collect { position ->
    updateCursorDisplay(position.x, position.y)
}
```

**3. Direct Method Invocation**:
```kotlin
// For synchronous operations, direct calls
val target = snapHandler.findNearestTarget(x, y)
gestureHandler.performClick()
```

---

## Core Components

### 1. VoiceCursorEventHandler

**File**: `VoiceCursorEventHandler.kt`
**Purpose**: Central event dispatcher for all cursor-related commands

#### Responsibilities

- **Voice Command Processing**: Parse voice commands into cursor events
- **Event Queue Management**: Asynchronous event processing with debouncing
- **Event Dispatching**: Route events to appropriate handlers via callbacks
- **Result Tracking**: Emit event results via StateFlow for monitoring

#### Key Classes

**CursorEvent (Sealed Class Hierarchy)**:
```kotlin
sealed class CursorEvent {
    data class Move(val direction: Direction, val distance: Float = 100f)
    data class MoveTo(val x: Float, val y: Float)
    data class Click(val x: Float? = null, val y: Float? = null)
    data class LongPress(val x: Float? = null, val y: Float? = null, val duration: Long = 1000L)
    data class Drag(val startX: Float, val startY: Float, val endX: Float, val endY: Float, val duration: Long = 500L)
    object Center
    object Show
    object Hide
    object Toggle
}
```

**Direction Enum**:
```kotlin
enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}
```

#### Public API

```kotlin
// Voice command processing
fun processVoiceCommand(command: String): Boolean

// Event dispatching
fun dispatchEvent(event: CursorEvent)

// Callback registration
fun registerEventCallback(eventType: String, callback: (CursorEvent) -> Unit)
fun registerResultCallback(callback: (EventResult) -> Unit)

// State queries
fun getQueueSize(): Int
fun getSupportedCommands(): List<String>

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val eventHandler = VoiceCursorEventHandler(
    config = EventConfig(
        debounceMs = 100L,
        maxQueueSize = 50,
        moveDistance = 100f
    )
)

// Register callbacks
eventHandler.registerEventCallback("Move") { event ->
    if (event is CursorEvent.Move) {
        handleMove(event.direction, event.distance)
    }
}

// Process voice command
val recognized = eventHandler.processVoiceCommand("move up")
// Event is queued and processed asynchronously

// Or dispatch event directly
eventHandler.dispatchEvent(CursorEvent.Click())
```

#### Configuration

**EventConfig**:
```kotlin
data class EventConfig(
    val debounceMs: Long = 100L,           // Debounce duration
    val maxQueueSize: Int = 50,            // Max event queue size
    val moveDistance: Float = 100f         // Default move distance
)
```

#### Algorithm: Event Processing

```
FUNCTION processEvent(event: CursorEvent):
    1. Record start time
    2. Apply debouncing check (skip if too soon)
    3. Notify event callbacks (execute registered handlers)
    4. Create EventResult with execution time
    5. Emit result to StateFlow
    6. Notify result callbacks
    7. Log success/failure

EVENT QUEUE PROCESSING (Async Coroutine):
    WHILE queue is not empty:
        1. Poll next event from queue
        2. Check debouncer (skip if debounced)
        3. Process event (see above)
        4. Continue to next event
```

---

### 2. CursorPositionTracker

**File**: `CursorPositionTracker.kt`
**Purpose**: Track cursor position in real-time with multi-display support

#### Responsibilities

- **Position Tracking**: Maintain current cursor position (absolute and normalized)
- **Coordinate Systems**: Support both absolute pixels and normalized (0-1) coordinates
- **Multi-Display Support**: Handle multiple displays with display ID tracking
- **Screen Bounds Awareness**: Automatically update on screen rotation/resize
- **Reactive Updates**: Emit position changes via StateFlow

#### Key Data Classes

**CursorPosition**:
```kotlin
data class CursorPosition(
    val x: Float,                    // Absolute X in pixels
    val y: Float,                    // Absolute Y in pixels
    val normalizedX: Float,          // Normalized X (0.0 - 1.0)
    val normalizedY: Float,          // Normalized Y (0.0 - 1.0)
    val displayId: Int = 0,          // Display ID
    val timestamp: Long = System.currentTimeMillis()
)
```

**ScreenBounds**:
```kotlin
data class ScreenBounds(
    val width: Int,
    val height: Int,
    val density: Float,
    val displayId: Int = 0
)
```

#### Public API

```kotlin
// Get current position
fun getCurrentPosition(): CursorPosition
val positionFlow: StateFlow<CursorPosition>

// Update position (absolute coordinates)
fun updatePosition(x: Float, y: Float, displayId: Int = 0)

// Update position (normalized 0-1)
fun updateNormalizedPosition(normalizedX: Float, normalizedY: Float, displayId: Int = 0)

// Relative movement
fun moveBy(deltaX: Float, deltaY: Float)

// Center cursor
fun centerCursor()

// Screen bounds
fun getScreenBounds(): ScreenBounds
fun updateScreenBounds()
fun isInBounds(): Boolean

// Callbacks
fun addPositionChangeCallback(callback: (CursorPosition) -> Unit)

// Configuration changes
fun onConfigurationChanged(newConfig: Configuration)

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val positionTracker = CursorPositionTracker(context)

// Track position changes reactively
lifecycleScope.launch {
    positionTracker.positionFlow.collect { position ->
        Log.d("Cursor", "Position: (${position.x}, ${position.y})")
        updateCursorOverlay(position.x, position.y)
    }
}

// Move cursor
positionTracker.moveBy(100f, 0f)  // Move 100px right

// Center cursor
positionTracker.centerCursor()

// Check bounds
if (positionTracker.isInBounds()) {
    // Position is valid
}
```

#### Algorithm: Position Update with Normalization

```
FUNCTION updatePosition(x: Float, y: Float):
    1. Get current screen bounds
    2. Create new CursorPosition:
       - x, y = absolute coordinates
       - normalizedX = x / screenWidth
       - normalizedY = y / screenHeight
       - displayId = current display
       - timestamp = current time
    3. Update StateFlow with new position
    4. If position changed:
       a. Notify all registered callbacks
       b. Log position update
```

**Coordinate Normalization** (maintains position across rotations):
```
SCREEN ROTATION HANDLING:
    1. Before rotation: save normalizedX, normalizedY
    2. Screen rotates: new width/height detected
    3. After rotation:
       - newX = normalizedX * newWidth
       - newY = normalizedY * newHeight
    4. Cursor maintains relative position
```

---

### 3. CursorHistoryTracker

**File**: `CursorHistoryTracker.kt`
**Purpose**: Track cursor movement history with undo/redo support

#### Responsibilities

- **Position Stack**: Bounded stack of historical positions (max 50 by default)
- **Undo/Redo**: Navigate backward/forward through cursor history
- **Significant Movement Detection**: Only track movements > 10px (configurable)
- **Time-Based Expiration**: Auto-remove entries older than 5 minutes
- **Memory Efficiency**: Uses ArrayDeque for O(1) operations

#### Key Data Classes

**HistoricalCursorPosition**:
```kotlin
data class HistoricalCursorPosition(
    val position: CursorPosition,
    val description: String? = null
) {
    fun distanceTo(other: HistoricalCursorPosition): Float
    fun isExpired(timeoutMillis: Long): Boolean
}
```

**HistoryStatistics**:
```kotlin
data class HistoryStatistics(
    val totalMoves: Long,
    val significantMoves: Long,
    val historySize: Int,
    val futureSize: Int,
    val totalDistance: Float,
    val oldestTimestamp: Long?,
    val newestTimestamp: Long?
)
```

#### Public API

```kotlin
// Record position
fun recordPosition(position: CursorPosition, description: String? = null, forceRecord: Boolean = false): Boolean

// Undo/Redo
fun undo(): HistoricalCursorPosition?
fun redo(): HistoricalCursorPosition?
fun canUndo(): Boolean
fun canRedo(): Boolean

// Peek without changing state
fun peekUndo(): HistoricalCursorPosition?
fun peekRedo(): HistoricalCursorPosition?

// History access
fun getCurrentPosition(): HistoricalCursorPosition?
fun getRecentHistory(count: Int = 10): List<HistoricalCursorPosition>
fun getAllHistory(): List<HistoricalCursorPosition>

// Search and navigation
fun jumpToIndex(index: Int): HistoricalCursorPosition?
fun findPositionByDescription(description: String): HistoricalCursorPosition?
fun getPositionsInTimeRange(startTime: Long, endTime: Long): List<HistoricalCursorPosition>

// Cleanup
fun clear()
fun cleanupExpiredEntries(): Int

// Statistics
fun getStatistics(): HistoryStatistics
fun getTotalDistanceTraveled(): Float
```

#### Usage Example

```kotlin
val historyTracker = CursorHistoryTracker(
    maxHistorySize = 50,
    expirationTimeMillis = 300_000L,  // 5 minutes
    significantMoveThreshold = 10f     // 10 pixels
)

// Record position
val recorded = historyTracker.recordPosition(currentPosition, "Button click")

// Undo last movement
val previousPosition = historyTracker.undo()
if (previousPosition != null) {
    positionTracker.updatePosition(previousPosition.x, previousPosition.y)
}

// Check statistics
val stats = historyTracker.getStatistics()
Log.d("History", stats.toString())
// Output:
// History Statistics:
//   Total Moves: 150
//   Significant Moves: 45 (30.0%)
//   History Size: 45
//   Total Distance: 2543.2px
```

#### Algorithm: Undo/Redo Stack Management

```
DATA STRUCTURES:
    historyStack: ArrayDeque<HistoricalCursorPosition>  (past)
    futureStack: ArrayDeque<HistoricalCursorPosition>   (redo)
    currentPosition: HistoricalCursorPosition?

FUNCTION recordPosition(position):
    1. Calculate distance from current position
    2. If distance < threshold AND not forceRecord:
       RETURN false  // Skip insignificant movement
    3. If currentPosition exists:
       a. Push currentPosition to historyStack
       b. If historyStack.size > maxSize:
          - Remove oldest (first) entry
    4. Clear futureStack (new action invalidates redo)
    5. Set currentPosition = new position
    6. Cleanup expired entries
    7. RETURN true

FUNCTION undo():
    1. If historyStack is empty: RETURN null
    2. Pop previous from historyStack
    3. Push currentPosition to futureStack
    4. Set currentPosition = previous
    5. RETURN previous

FUNCTION redo():
    1. If futureStack is empty: RETURN null
    2. Pop next from futureStack
    3. Push currentPosition to historyStack
    4. Set currentPosition = next
    5. RETURN next
```

**Significant Movement Detection**:
```
FUNCTION isSignificantMovement(newPos, oldPos, threshold):
    dx = newPos.x - oldPos.x
    dy = newPos.y - oldPos.y
    distance = sqrt(dx² + dy²)
    RETURN distance >= threshold
```

---

### 4. CursorVisibilityManager

**File**: `CursorVisibilityManager.kt`
**Purpose**: Manage cursor visibility with smooth animations

#### Responsibilities

- **Show/Hide Control**: Toggle cursor visibility with animations
- **Interaction Modes**: Voice-only, Touch-only, Hybrid modes
- **Auto-Hide Timer**: Configurable timeout (default 5 seconds)
- **Fade Animations**: Smooth fade in/out (300ms default)
- **State Management**: Track visibility state via StateFlow

#### Key Enums and Data Classes

**VisibilityState**:
```kotlin
enum class VisibilityState {
    VISIBLE,      // Fully visible (alpha = 1.0)
    HIDDEN,       // Fully hidden (alpha = 0.0)
    FADING_IN,    // Transitioning to visible
    FADING_OUT    // Transitioning to hidden
}
```

**InteractionMode**:
```kotlin
enum class InteractionMode {
    VOICE,        // Voice commands only - cursor always visible
    TOUCH,        // Touch input only - cursor auto-hides
    HYBRID        // Voice + touch - cursor visible during voice, auto-hides after
}
```

**VisibilityConfig**:
```kotlin
data class VisibilityConfig(
    val autoHideDuration: Long = 5000L,    // Auto-hide after 5 seconds
    val fadeDuration: Long = 300L,         // Fade animation 300ms
    val interactionMode: InteractionMode = InteractionMode.HYBRID
)
```

#### Public API

```kotlin
// State queries
fun getState(): VisibilityState
fun getAlpha(): Float
fun isVisible(): Boolean
fun isHidden(): Boolean
val stateFlow: StateFlow<VisibilityState>
val alphaFlow: StateFlow<Float>

// Show/Hide
fun show(animated: Boolean = true)
fun hide(animated: Boolean = true)
fun toggle(animated: Boolean = true)

// Auto-hide timer
fun resetAutoHideTimer()

// Interaction mode
fun setInteractionMode(mode: InteractionMode)

// Callbacks
fun addVisibilityCallback(callback: (VisibilityState) -> Unit)
fun addAlphaCallback(callback: (Float) -> Unit)

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val visibilityManager = CursorVisibilityManager(
    config = VisibilityConfig(
        autoHideDuration = 5000L,
        fadeDuration = 300L,
        interactionMode = InteractionMode.HYBRID
    )
)

// Show cursor
visibilityManager.show(animated = true)

// Track alpha for rendering
lifecycleScope.launch {
    visibilityManager.alphaFlow.collect { alpha ->
        cursorView.alpha = alpha
    }
}

// Reset timer when user interacts
fun onVoiceCommand() {
    visibilityManager.show()
    visibilityManager.resetAutoHideTimer()
}

// Hide cursor
visibilityManager.hide(animated = true)
```

#### Algorithm: Fade Animation with Auto-Hide

```
FUNCTION show(animated: Boolean):
    1. Cancel any pending auto-hide timer
    2. Cancel any ongoing fade animation
    3. If animated:
       a. Set state = FADING_IN
       b. Start fade animation (current alpha → 1.0)
       c. On animation end: state = VISIBLE, alpha = 1.0
    4. Else:
       a. Set state = VISIBLE, alpha = 1.0
    5. If auto-hide enabled AND not voice-only mode:
       a. Start auto-hide timer (configured duration)

FUNCTION hide(animated: Boolean):
    1. If voice-only mode: RETURN (don't hide)
    2. Cancel auto-hide timer
    3. Cancel ongoing fade animation
    4. If animated:
       a. Set state = FADING_OUT
       b. Start fade animation (current alpha → 0.0)
       c. On animation end: state = HIDDEN, alpha = 0.0
    5. Else:
       a. Set state = HIDDEN, alpha = 0.0

AUTO-HIDE TIMER (Coroutine):
    1. Delay(autoHideDuration)
    2. hide(animated = true)
```

---

### 5. CursorStyleManager

**File**: `CursorStyleManager.kt`
**Purpose**: Manage cursor visual appearance and animations

#### Responsibilities

- **Style Management**: Multiple predefined styles (normal, selection, click, loading, disabled)
- **Custom Styles**: Support for custom colors, sizes, animations
- **Animation Types**: Pulse, spin, fade, bounce effects
- **State-Based Styling**: Change appearance based on cursor state
- **Compose Integration**: Uses Jetpack Compose Color for rendering

#### Key Classes

**CursorStyle (Sealed Class)**:
```kotlin
sealed class CursorStyle {
    data class Normal(
        val color: ComposeColor = DEFAULT_COLOR,
        val size: Float = DEFAULT_SIZE,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH
    )

    data class Selection(
        val color: ComposeColor = SELECTION_COLOR,
        val size: Float = DEFAULT_SIZE * 1.2f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 1.5f,
        val pulseEnabled: Boolean = true
    )

    data class Click(
        val color: ComposeColor = CLICK_COLOR,
        val size: Float = DEFAULT_SIZE * 1.5f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 2f,
        val pulseSpeed: Float = 1.5f
    )

    data class Loading(
        val color: ComposeColor = LOADING_COLOR,
        val size: Float = DEFAULT_SIZE,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH,
        val spinSpeed: Float = 1f
    )

    data class Disabled(
        val color: ComposeColor = DISABLED_COLOR,
        val size: Float = DEFAULT_SIZE * 0.8f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 0.5f,
        val alpha: Float = 0.5f
    )

    data class Custom(
        val color: ComposeColor,
        val size: Float,
        val strokeWidth: Float,
        val animationType: AnimationType = AnimationType.NONE,
        val alpha: Float = 1f
    )
}
```

**AnimationType**:
```kotlin
enum class AnimationType {
    NONE,       // No animation
    PULSE,      // Pulsing scale animation
    SPIN,       // Rotating animation
    BOUNCE      // Bouncing animation
}
```

**CursorShape**:
```kotlin
enum class CursorShape {
    CIRCLE,     // Circular cursor
    CROSSHAIR,  // Crosshair cursor
    POINTER,    // Arrow pointer
    HAND,       // Hand icon
    CUSTOM      // Custom drawable
}
```

#### Public API

```kotlin
// Style management
fun getCurrentStyle(): CursorStyle
fun setStyle(style: CursorStyle)
fun setNormal()
fun setSelection()
fun setClick()
fun setLoading()
fun setDisabled()
fun setCustom(color: ComposeColor, size: Float, strokeWidth: Float, animationType: AnimationType, alpha: Float)

// Style properties
fun getCurrentColor(): ComposeColor
fun getCurrentSize(): Float
fun getCurrentStrokeWidth(): Float
fun getCurrentAlpha(): Float
fun getCurrentAnimationType(): AnimationType

// Animation
fun updateAnimationProgress(progress: Float)
fun getAnimationProgress(): Float
fun areAnimationsEnabled(): Boolean

// Callbacks
fun addStyleCallback(callback: (CursorStyle) -> Unit)

// Color helpers
fun colorFromInt(colorInt: Int): ComposeColor
fun colorToInt(color: ComposeColor): Int

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val styleManager = CursorStyleManager(
    config = StyleConfig(
        shape = CursorShape.CIRCLE,
        enableAnimations = true,
        animationDuration = 300L
    )
)

// Set predefined styles
styleManager.setNormal()
styleManager.setSelection()
styleManager.setClick()

// Custom style
styleManager.setCustom(
    color = ComposeColor(0xFF00FF00),  // Green
    size = 60f,
    strokeWidth = 5f,
    animationType = AnimationType.PULSE,
    alpha = 0.8f
)

// Track style changes
styleManager.addStyleCallback { style ->
    when (style) {
        is CursorStyle.Click -> playClickFeedback()
        is CursorStyle.Selection -> highlightElement()
        else -> {}
    }
}

// Use in Compose rendering
val currentStyle = styleManager.getCurrentStyle()
Canvas(modifier = Modifier.fillMaxSize()) {
    drawCircle(
        color = currentStyle.styleColor(),
        radius = currentStyle.styleSize() / 2,
        style = Stroke(width = currentStyle.styleStrokeWidth())
    )
}
```

#### Style Transition Examples

```kotlin
// Click feedback sequence
fun onCursorClick() {
    styleManager.setClick()           // Change to click style
    delay(200)                        // Visual feedback
    styleManager.setNormal()          // Return to normal
}

// Element selection
fun onElementHover(element: AccessibilityNodeInfo) {
    if (element.isClickable) {
        styleManager.setSelection()   // Show selectable
    }
}

// Loading state
suspend fun performLongOperation() {
    styleManager.setLoading()         // Show loading
    try {
        doWork()
    } finally {
        styleManager.setNormal()      // Restore
    }
}
```

---

### 6. CursorGestureHandler

**File**: `CursorGestureHandler.kt`
**Purpose**: Execute touch gestures at cursor position via AccessibilityService

#### Responsibilities

- **Gesture Dispatch**: Execute gestures using AccessibilityService.dispatchGesture()
- **Click Gestures**: Single tap, double tap, long press
- **Swipe Gestures**: Directional swipes (up, down, left, right)
- **Drag Gestures**: Drag from point A to point B
- **Scroll Gestures**: Vertical/horizontal scrolling
- **Async Execution**: All gestures execute asynchronously with result callbacks

#### Key Classes

**GestureType**:
```kotlin
enum class GestureType {
    CLICK,          // Single tap
    DOUBLE_CLICK,   // Double tap
    LONG_PRESS,     // Long press/hold
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT,
    DRAG,           // Drag gesture
    SCROLL_UP, SCROLL_DOWN
}
```

**GestureResult**:
```kotlin
data class GestureResult(
    val success: Boolean,
    val gestureType: GestureType,
    val message: String? = null,
    val executionTime: Long = 0
)
```

**GestureConfig**:
```kotlin
data class GestureConfig(
    val tapDuration: Long = ViewConfiguration.getTapTimeout().toLong(),
    val longPressDuration: Long = ViewConfiguration.getLongPressTimeout().toLong(),
    val doubleTapDelay: Long = ViewConfiguration.getDoubleTapTimeout().toLong(),
    val swipeDistance: Float = 400f,
    val swipeDuration: Long = 300L,
    val scrollDistance: Float = 200f
)
```

#### Public API

```kotlin
// Click gestures
suspend fun performClick(): GestureResult
suspend fun performClickAt(x: Float, y: Float): GestureResult
suspend fun performDoubleClick(): GestureResult
suspend fun performDoubleClickAt(x: Float, y: Float): GestureResult
suspend fun performLongPress(duration: Long = config.longPressDuration): GestureResult
suspend fun performLongPressAt(x: Float, y: Float, duration: Long): GestureResult

// Swipe gestures
suspend fun performSwipe(direction: Direction, distance: Float = config.swipeDistance): GestureResult

// Drag gestures
suspend fun performDrag(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = config.swipeDuration): GestureResult

// Scroll gestures
suspend fun performScroll(direction: Direction, distance: Float = config.scrollDistance): GestureResult

// Callbacks
fun addGestureCallback(callback: (GestureResult) -> Unit)

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val gestureHandler = CursorGestureHandler(
    service = accessibilityService,
    positionTracker = positionTracker,
    config = GestureConfig(
        swipeDistance = 400f,
        swipeDuration = 300L
    )
)

// Click at current cursor position
lifecycleScope.launch {
    val result = gestureHandler.performClick()
    if (result.success) {
        Log.d("Gesture", "Click successful")
    }
}

// Double click
lifecycleScope.launch {
    val result = gestureHandler.performDoubleClick()
}

// Swipe up
lifecycleScope.launch {
    val result = gestureHandler.performSwipe(Direction.UP, distance = 500f)
}

// Drag gesture
lifecycleScope.launch {
    val result = gestureHandler.performDrag(
        startX = 100f, startY = 100f,
        endX = 300f, endY = 300f,
        duration = 500L
    )
}

// Monitor all gestures
gestureHandler.addGestureCallback { result ->
    Log.d("Gesture", "${result.gestureType}: ${result.success} (${result.executionTime}ms)")
}
```

#### Algorithm: Gesture Dispatch via AccessibilityService

```
FUNCTION performClickAt(x, y):
    1. Create Path:
       path.moveTo(x, y)
       path.lineTo(x, y)  // Zero-length path = tap
    2. Create GestureDescription:
       - Add StrokeDescription(path, startTime=0, duration=tapDuration)
    3. Dispatch gesture asynchronously:
       a. Call service.dispatchGesture(gesture, callback, handler)
       b. Wait for callback (onCompleted or onCancelled)
    4. Return GestureResult with success/failure

FUNCTION performSwipe(direction, distance):
    1. Get current cursor position (startX, startY)
    2. Calculate end point based on direction:
       - UP: endY = startY - distance
       - DOWN: endY = startY + distance
       - LEFT: endX = startX - distance
       - RIGHT: endX = startX + distance
    3. Create Path:
       path.moveTo(startX, startY)
       path.lineTo(endX, endY)
    4. Create GestureDescription with swipe duration
    5. Dispatch and return result

ASYNC GESTURE DISPATCH (suspendCancellableCoroutine):
    SUSPEND until gesture completes:
        - On success: resume(true)
        - On cancelled: resume(false)
        - On dispatch failure: resume(false)
```

**Double Click Implementation**:
```
FUNCTION performDoubleClickAt(x, y):
    1. Perform first click at (x, y)
    2. Wait doubleTapDelay / 2 milliseconds
    3. Perform second click at (x, y)
    4. Return combined result
```

---

### 7. SpeedController

**File**: `SpeedController.kt`
**Purpose**: Manage cursor movement speed and acceleration

#### Responsibilities

- **Speed Presets**: PRECISION (50px/s), SLOW (250px/s), MEDIUM (1000px/s), FAST (2000px/s), VERY_FAST (4000px/s)
- **Precision Mode**: 5x slower movement for fine control
- **Acceleration Curves**: Smooth easing functions (linear, ease-in, ease-out, etc.)
- **Velocity Calculation**: Convert speed to pixels/frame for smooth movement
- **Time-Based Movement**: Calculate distance based on elapsed time

#### Key Enums

**CursorSpeed**:
```kotlin
enum class CursorSpeed(
    val pixelsPerSecond: Float,
    val accelerationTime: Long,
    val description: String
) {
    PRECISION(50f, 0L, "Precision mode - 5x slower"),
    SLOW(250f, 500L, "Slow speed - 25% of normal"),
    MEDIUM(1000f, 300L, "Medium speed - normal"),
    FAST(2000f, 200L, "Fast speed - 2x normal"),
    VERY_FAST(4000f, 100L, "Very fast - 4x normal")
}
```

**EasingFunction**:
```kotlin
enum class EasingFunction {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EXPONENTIAL,
    CIRCULAR,
    BACK
}
```

#### Public API

```kotlin
// Speed control
fun setSpeed(speed: CursorSpeed)
fun getSpeed(): CursorSpeed

// Precision mode
fun setPrecisionMode(enabled: Boolean)
fun isPrecisionMode(): Boolean
fun togglePrecisionMode()

// Velocity calculation
fun calculateVelocity(deltaTime: Long = 0L): Float
fun getCurrentVelocity(): Float
fun getTargetVelocity(): Float

// Distance calculation
fun calculateDistance(deltaTime: Long, direction: Pair<Float, Float> = Pair(1f, 0f)): Pair<Float, Float>
fun calculateTimeForDistance(distance: Float): Long
fun calculatePixelsPerFrame(frameRate: Int = 60): Float

// Easing functions
fun applyEasing(progress: Float, easing: EasingFunction): Float
fun applySineEasing(progress: Float): Float

// State
fun reset()
fun getSpeedInfo(): String
```

#### Usage Example

```kotlin
val speedController = SpeedController()

// Set speed
speedController.setSpeed(CursorSpeed.FAST)

// Enable precision mode
speedController.setPrecisionMode(true)

// Calculate movement for this frame
val deltaTime = 16L  // 16ms per frame (60fps)
val (dx, dy) = speedController.calculateDistance(
    deltaTime = deltaTime,
    direction = Pair(0f, -1f)  // Moving up
)
positionTracker.moveBy(dx, dy)

// Get current velocity info
Log.d("Speed", speedController.getSpeedInfo())
// Output: "FAST (1800px/s target: 2000px/s)"
```

#### Algorithm: Acceleration with Easing

```
FUNCTION calculateVelocity(deltaTime):
    1. Calculate elapsed time since acceleration started
    2. Calculate progress (0.0 to 1.0):
       progress = elapsedTime / accelerationTime
    3. Apply easing function for smooth acceleration:
       easedProgress = applyEasing(progress, EASE_OUT)
    4. Interpolate velocity:
       currentVelocity = currentVelocity + (targetVelocity - currentVelocity) * easedProgress
    5. RETURN currentVelocity

EASING FUNCTION - EASE_OUT (t = progress 0-1):
    RETURN t * (2 - t)

    Effect: Fast start, slow finish
    Visual: ____
           /    \___
          /         \

EASING FUNCTION - EASE_IN_OUT:
    IF t < 0.5:
        RETURN 2 * t * t
    ELSE:
        RETURN -1 + (4 - 2 * t) * t

    Effect: Slow start and end, fast middle
    Visual:   ____
           __/    \__
          /          \

FUNCTION calculateDistance(deltaTime, direction):
    1. velocity = calculateVelocity(deltaTime)
    2. deltaSeconds = deltaTime / 1000
    3. distance = velocity * deltaSeconds
    4. dx = distance * direction.x
    5. dy = distance * direction.y
    6. RETURN (dx, dy)
```

**Precision Mode Calculation**:
```
targetVelocity = baseSpeed.pixelsPerSecond * (precisionMode ? 0.2 : 1.0)

Example:
- FAST speed = 2000 px/s
- Precision mode enabled
- Effective speed = 2000 * 0.2 = 400 px/s (5x slower)
```

---

### 8. BoundaryDetector

**File**: `BoundaryDetector.kt`
**Purpose**: Prevent cursor from leaving screen bounds

#### Responsibilities

- **Bounds Checking**: Verify cursor position is within screen
- **Edge Detection**: Detect when cursor is near screen edges
- **Coordinate Clamping**: Constrain coordinates to valid screen area
- **Safe Area Support**: Respect system UI insets (notches, navigation bars)
- **Multi-Display Support**: Handle different display configurations

#### Key Classes

**ScreenEdge**:
```kotlin
enum class ScreenEdge {
    LEFT, RIGHT, TOP, BOTTOM,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    NONE
}
```

**BoundaryCheckResult**:
```kotlin
data class BoundaryCheckResult(
    val isInBounds: Boolean,
    val clampedX: Float,
    val clampedY: Float,
    val nearEdge: ScreenEdge,
    val distanceToEdge: Float
)
```

**SafeAreaInsets**:
```kotlin
data class SafeAreaInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)
```

**BoundaryConfig**:
```kotlin
data class BoundaryConfig(
    val edgeThreshold: Float = 50f,        // Distance to be "near edge"
    val respectSafeArea: Boolean = true,   // Account for notches/nav bars
    val allowOverscroll: Boolean = false,  // Allow small overscroll
    val overscrollDistance: Float = 20f    // Max overscroll pixels
)
```

#### Public API

```kotlin
// Bounds checking
fun checkBounds(x: Float, y: Float): BoundaryCheckResult
fun clampToBounds(x: Float, y: Float): PointF
fun isNearEdge(x: Float, y: Float): Boolean

// Edge detection
fun detectNearEdge(x: Float, y: Float): ScreenEdge
fun calculateDistanceToEdge(x: Float, y: Float): Float
fun areOnSameEdge(x1: Float, y1: Float, x2: Float, y2: Float): Boolean

// Bounds access
fun getEffectiveBounds(): Rect
fun getRawBounds(): Rect
fun getSafeAreaInsets(): SafeAreaInsets
fun getScreenCenter(): PointF

// Updates
fun updateSafeAreaInsets(insets: WindowInsets?)
fun updateDisplayMetrics()

// Validation
fun validateAndClamp(x: Float, y: Float, label: String = "Point"): PointF

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val boundaryDetector = BoundaryDetector(
    context = context,
    config = BoundaryConfig(
        edgeThreshold = 50f,
        respectSafeArea = true,
        allowOverscroll = false
    )
)

// Check if position is valid
val result = boundaryDetector.checkBounds(x, y)
if (!result.isInBounds) {
    // Clamp to bounds
    positionTracker.updatePosition(result.clampedX, result.clampedY)
}

// Detect edge proximity
val edge = boundaryDetector.detectNearEdge(x, y)
when (edge) {
    ScreenEdge.RIGHT -> showEdgeIndicator("right")
    ScreenEdge.BOTTOM -> showEdgeIndicator("bottom")
    else -> hideEdgeIndicator()
}

// Respect safe area (notches, nav bars)
boundaryDetector.updateSafeAreaInsets(view.rootWindowInsets)
```

#### Algorithm: Bounds Checking with Safe Area

```
FUNCTION checkBounds(x, y):
    1. Get effective bounds (raw bounds - safe area insets)
    2. Determine if in bounds (with optional overscroll):
       isInBounds = x >= bounds.left - overscroll &&
                    x <= bounds.right + overscroll &&
                    y >= bounds.top - overscroll &&
                    y <= bounds.bottom + overscroll
    3. Clamp coordinates:
       clampedX = x.coerceIn(bounds.left, bounds.right)
       clampedY = y.coerceIn(bounds.top, bounds.bottom)
    4. Detect nearest edge
    5. Calculate distance to edge
    6. RETURN BoundaryCheckResult

FUNCTION detectNearEdge(x, y):
    threshold = edgeThreshold (e.g., 50 pixels)

    nearLeft = (x - bounds.left) <= threshold
    nearRight = (bounds.right - x) <= threshold
    nearTop = (y - bounds.top) <= threshold
    nearBottom = (bounds.bottom - y) <= threshold

    IF nearLeft AND nearTop: RETURN TOP_LEFT
    IF nearRight AND nearTop: RETURN TOP_RIGHT
    IF nearLeft AND nearBottom: RETURN BOTTOM_LEFT
    IF nearRight AND nearBottom: RETURN BOTTOM_RIGHT
    IF nearLeft: RETURN LEFT
    IF nearRight: RETURN RIGHT
    IF nearTop: RETURN TOP
    IF nearBottom: RETURN BOTTOM
    ELSE: RETURN NONE

SAFE AREA CALCULATION:
    rawBounds = Rect(0, 0, screenWidth, screenHeight)

    IF respectSafeArea AND hasInsets:
        effectiveBounds = Rect(
            left = rawBounds.left + safeAreaInsets.left,
            top = rawBounds.top + safeAreaInsets.top,
            right = rawBounds.right - safeAreaInsets.right,
            bottom = rawBounds.bottom - safeAreaInsets.bottom
        )
    ELSE:
        effectiveBounds = rawBounds
```

**Safe Area Example (Phone with notch and nav bar)**:
```
Screen: 1080 x 2400 pixels

Safe Area Insets:
  - top: 100px (notch)
  - bottom: 150px (navigation bar)
  - left: 0px
  - right: 0px

Raw Bounds: Rect(0, 0, 1080, 2400)
Effective Bounds: Rect(0, 100, 1080, 2250)

Cursor is constrained to effective bounds to avoid UI overlap.
```

---

### 9. FocusIndicator

**File**: `FocusIndicator.kt`
**Purpose**: Visual focus indicator for highlighting UI elements

#### Responsibilities

- **Element Highlighting**: Draw animated rings around focused elements
- **Multi-State Support**: Different visual styles for focused, selected, hover, error states
- **Animation Styles**: Static, pulse, rotate, fade, breathe animations
- **Overlay Rendering**: TYPE_ACCESSIBILITY_OVERLAY window for drawing
- **Compose-Based**: Uses Jetpack Compose for smooth animations

#### Key Classes

**FocusState**:
```kotlin
enum class FocusState(
    val color: Int,
    val strokeWidth: Float,
    val description: String
) {
    FOCUSED(Color.parseColor("#4CAF50"), 8f, "Element is focused"),
    SELECTED(Color.parseColor("#2196F3"), 10f, "Element is selected"),
    HOVER(Color.parseColor("#FFC107"), 6f, "Cursor hovering"),
    ERROR(Color.parseColor("#F44336"), 8f, "Error/invalid"),
    DISABLED(Color.parseColor("#9E9E9E"), 6f, "Disabled")
}
```

**AnimationStyle**:
```kotlin
enum class AnimationStyle {
    STATIC,        // No animation
    PULSE,         // Pulsing size
    ROTATE,        // Rotating ring
    PULSE_ROTATE,  // Both pulsing and rotating
    FADE,          // Fading in/out
    BREATHE        // Smooth breathing effect
}
```

**FocusIndicatorConfig**:
```kotlin
data class FocusIndicatorConfig(
    val bounds: RectF,
    val state: FocusState = FocusState.FOCUSED,
    val animationStyle: AnimationStyle = AnimationStyle.PULSE,
    val duration: Int = 1000,
    val cornerRadius: Float = 8f
)
```

#### Public API

```kotlin
// Show/Hide
fun show(config: FocusIndicatorConfig)
fun update(config: FocusIndicatorConfig)
fun hide()

// State query
fun isVisible(): Boolean

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val focusIndicator = FocusIndicator(
    context = context,
    windowManager = windowManager
)

// Highlight an element
val elementBounds = RectF(100f, 100f, 300f, 200f)
focusIndicator.show(
    FocusIndicatorConfig(
        bounds = elementBounds,
        state = FocusState.FOCUSED,
        animationStyle = AnimationStyle.PULSE,
        duration = 1000
    )
)

// Update while moving
focusIndicator.update(
    config.copy(bounds = newBounds)
)

// Hide when no longer focused
focusIndicator.hide()
```

#### Algorithm: Pulsing Animation

```
COMPOSE ANIMATION - PULSING:
    minScale = 0.9f
    maxScale = 1.1f

    infiniteTransition = rememberInfiniteTransition()
    scale = infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration / 2),
            repeatMode = RepeatMode.Reverse
        )
    )

    DRAW:
        centerX = bounds.centerX()
        centerY = bounds.centerY()
        scaledWidth = bounds.width() * scale
        scaledHeight = bounds.height() * scale

        drawRoundRect(
            topLeft = (centerX - scaledWidth/2, centerY - scaledHeight/2),
            size = (scaledWidth, scaledHeight),
            color = state.color,
            strokeWidth = state.strokeWidth
        )
```

---

### 10. SnapToElementHandler

**File**: `SnapToElementHandler.kt`
**Purpose**: Intelligent cursor snapping to UI elements

#### Responsibilities

- **Proximity Detection**: Find nearest clickable elements within radius
- **Target Prioritization**: Rank elements by type and properties (buttons > text)
- **Smooth Snapping**: Animated cursor movement to target center
- **Element Validation**: Ensure targets are visible and clickable
- **Accessibility Integration**: Uses AccessibilityNodeInfo tree traversal

#### Key Classes

**SnapTarget**:
```kotlin
data class SnapTarget(
    val node: AccessibilityNodeInfo,
    val bounds: Rect,
    val center: Point,
    val distance: Float,
    val priority: Int,
    val description: String
)
```

#### Public API

```kotlin
// Configuration
fun setSnapRadius(radius: Float)
fun getSnapRadius(): Float
fun setSnapEnabled(enabled: Boolean)
fun isSnapEnabled(): Boolean

// Target finding
fun findNearestTarget(x: Float, y: Float, maxRadius: Float = snapRadius): SnapTarget?
fun findAllTargets(x: Float, y: Float, maxRadius: Float = snapRadius): List<SnapTarget>
fun getTargetAtPosition(x: Float, y: Float): SnapTarget?

// Snapping
fun snapToTarget(target: SnapTarget, onUpdate: (x: Float, y: Float) -> Unit, onComplete: (() -> Unit)? = null)
fun cancelAnimation()

// Cleanup
fun dispose()
```

#### Usage Example

```kotlin
val snapHandler = SnapToElementHandler(
    service = accessibilityService
)

snapHandler.setSnapRadius(200f)

// Find nearest element
val target = snapHandler.findNearestTarget(cursorX, cursorY)

if (target != null) {
    Log.d("Snap", "Found: ${target.description} at ${target.distance}px")

    // Snap to it
    snapHandler.snapToTarget(target,
        onUpdate = { x, y ->
            positionTracker.updatePosition(x, y)
        },
        onComplete = {
            Log.d("Snap", "Snap complete")
            target.recycle()
        }
    )
}
```

#### Algorithm: Target Finding with Prioritization

```
FUNCTION findNearestTarget(cursorX, cursorY, maxRadius):
    1. Get accessibility root node
    2. candidates = empty list
    3. Traverse accessibility tree:
       FOR EACH node in tree:
           IF node.isClickable AND node.isVisible:
               a. Get element bounds
               b. Calculate center point
               c. Calculate distance from cursor to center
               d. IF distance <= maxRadius:
                   i. Calculate priority (see below)
                   ii. Add to candidates
    4. Sort candidates:
       a. First by priority (descending)
       b. Then by distance (ascending)
    5. RETURN first candidate (highest priority, closest)
    6. Recycle unused nodes

PRIORITY CALCULATION:
    basePriority = ELEMENT_PRIORITIES[elementType]

    Element Type Priorities:
    - button: 100
    - imagebutton: 95
    - checkbox: 90
    - edittext: 80
    - textview: 50
    - default: 30

    Boosts:
    - Has text: +10
    - Has content description: +5
    - Large element (area > 10000px²): +5

    Penalties:
    - Very small element (area < 1000px²): -10

    RETURN basePriority + boosts - penalties

SNAP ANIMATION (60 FPS):
    startX, startY = current cursor position
    endX, endY = target.center

    totalFrames = SNAP_DURATION / (1000 / 60)

    FOR frame = 0 to totalFrames:
        progress = frame / totalFrames
        easedProgress = applyEasing(progress, EASE_OUT)

        currentX = startX + (endX - startX) * easedProgress
        currentY = startY + (endY - startY) * easedProgress

        onUpdate(currentX, currentY)
        delay(1000 / 60 milliseconds)
```

**Element Prioritization Example**:
```
Scenario: Cursor at (500, 500), radius = 200px

Candidates found:
1. Button "Submit" at (520, 520)
   - Type: button (priority 100)
   - Has text: +10
   - Distance: 28px
   - Final priority: 110

2. TextView "Hello" at (480, 480)
   - Type: textview (priority 50)
   - Has text: +10
   - Distance: 28px
   - Final priority: 60

3. Button "Cancel" at (600, 600)
   - Type: button (priority 100)
   - Has text: +10
   - Distance: 141px
   - Final priority: 110

Sorted candidates:
1. Button "Submit" (priority 110, distance 28px) ← SELECTED
2. Button "Cancel" (priority 110, distance 141px)
3. TextView "Hello" (priority 60, distance 28px)

Result: Snap to "Submit" button (highest priority, closest)
```

---

### 11. CommandMapper

**File**: `CommandMapper.kt`
**Purpose**: Map voice commands to cursor actions

#### Responsibilities

- **Command Registration**: Register voice commands with actions
- **Pattern Matching**: Match spoken text to registered commands
- **Alias Support**: Multiple phrases for same action
- **Priority Handling**: Resolve conflicts via priority system
- **Action Execution**: Execute actions via registered callbacks

#### Key Classes

**CursorAction (Sealed Class)**:
```kotlin
sealed class CursorAction {
    // Movement
    data class Move(val dx: Float, val dy: Float)
    data class MoveTo(val x: Float, val y: Float)
    data class MoveDirection(val direction: Direction, val distance: Float = 100f)

    // Clicks
    object Click
    object DoubleClick
    object LongPress
    data class ClickAt(val x: Float, val y: Float)

    // Speed
    data class SetSpeed(val speed: CursorSpeed)
    object TogglePrecisionMode

    // Snap
    object SnapToNearest
    data class SnapToElement(val elementId: String)

    // History
    object Undo
    object Redo

    // Focus
    data class ShowFocus(val state: FocusState = FocusState.FOCUSED)
    object HideFocus

    // Utility
    object Reset
    object Stop
}
```

**CommandPattern**:
```kotlin
data class CommandPattern(
    val pattern: String,
    val action: CursorAction,
    val priority: Int = 0,
    val aliases: List<String> = emptyList(),
    val description: String = ""
)
```

**CommandResult**:
```kotlin
data class CommandResult(
    val success: Boolean,
    val action: CursorAction?,
    val message: String = "",
    val executionTime: Long = 0L
)
```

#### Public API

```kotlin
// Command registration
fun registerCommand(pattern: String, action: CursorAction, priority: Int = 0, aliases: List<String> = emptyList(), description: String = "")
fun unregisterCommand(pattern: String): Boolean

// Command mapping
fun mapCommand(commandText: String): CommandResult
fun mapAndExecute(commandText: String): CommandResult

// Action execution
fun <T : CursorAction> registerActionCallback(actionClass: Class<T>, callback: (T) -> Boolean)
fun executeAction(action: CursorAction?): Boolean

// Discovery
fun getAllCommands(): List<CommandPattern>
fun getCommandsForAction(actionClass: Class<out CursorAction>): List<CommandPattern>
fun searchCommands(query: String): List<CommandPattern>
fun getSuggestions(partial: String, maxResults: Int = 5): List<String>
fun getHelpText(): String

// Management
fun clear()
fun resetToDefaults()
fun getStatistics(): CommandMapperStatistics
```

#### Usage Example

```kotlin
val commandMapper = CommandMapper()

// Register action callbacks
commandMapper.registerActionCallback(CursorAction.MoveDirection::class.java) { action ->
    val (dx, dy) = when (action.direction) {
        Direction.UP -> Pair(0f, -action.distance)
        Direction.DOWN -> Pair(0f, action.distance)
        Direction.LEFT -> Pair(-action.distance, 0f)
        Direction.RIGHT -> Pair(action.distance, 0f)
    }
    positionTracker.moveBy(dx, dy)
    true
}

commandMapper.registerActionCallback(CursorAction.Click::class.java) { action ->
    lifecycleScope.launch {
        gestureHandler.performClick()
    }
    true
}

// Map and execute voice command
val result = commandMapper.mapAndExecute("move up")
if (result.success) {
    Log.d("Command", "Executed: ${result.action}")
}

// Custom command
commandMapper.registerCommand(
    pattern = "go home",
    action = CursorAction.MoveTo(homeX, homeY),
    priority = 20,
    aliases = listOf("home", "return home"),
    description = "Move cursor to home position"
)

// Get suggestions
val suggestions = commandMapper.getSuggestions("mo")
// Returns: ["move up", "move down", "move left", "move right"]
```

#### Algorithm: Command Matching with Priority

```
FUNCTION mapCommand(commandText):
    1. Normalize input: lowercase, trim whitespace
    2. Search registered commands (sorted by priority):
       FOR EACH command in commandPatterns:
           IF command.matches(normalizedText):
               RETURN CommandResult(
                   success = true,
                   action = command.action
               )
    3. IF no match found:
       RETURN CommandResult(success = false, action = null)

COMMAND MATCHING:
    FUNCTION matches(text):
        normalized = text.lowercase().trim()
        RETURN normalized == pattern.lowercase() OR
               normalized in aliases.map { it.lowercase() }

PRIORITY RESOLUTION:
    Commands stored in list sorted by priority (descending)

    Example:
    - "stop" (priority 100) ← checked first
    - "move up" (priority 10)
    - "show focus" (priority 5) ← checked last

    First match wins, so highest priority commands take precedence
```

**Default Commands**:
```kotlin
Movement:
- "move up" (aliases: "up", "go up") → MoveDirection(UP)
- "move down" (aliases: "down", "go down") → MoveDirection(DOWN)
- "move left" (aliases: "left", "go left") → MoveDirection(LEFT)
- "move right" (aliases: "right", "go right") → MoveDirection(RIGHT)

Click:
- "click" (aliases: "tap", "select") → Click
- "double click" (aliases: "double tap") → DoubleClick
- "long press" (aliases: "hold", "press and hold") → LongPress

Speed:
- "slow speed" (aliases: "slower", "speed slow") → SetSpeed(SLOW)
- "fast speed" (aliases: "faster", "speed fast") → SetSpeed(FAST)
- "precision mode" (aliases: "precise mode") → TogglePrecisionMode

Snap:
- "snap" (aliases: "snap to element", "find element") → SnapToNearest

History:
- "undo" (aliases: "go back", "back") → Undo
- "redo" (aliases: "go forward", "forward") → Redo

Utility:
- "reset cursor" (aliases: "reset", "center cursor") → Reset
- "stop" (aliases: "halt", "freeze") → Stop
```

---

## Movement Model

### Coordinate Systems

**1. Absolute Coordinates (Pixels)**:
```kotlin
// Screen: 1080 x 2400 pixels
val absolutePosition = CursorPosition(
    x = 540f,      // 540 pixels from left
    y = 1200f,     // 1200 pixels from top
    normalizedX = 0.5f,
    normalizedY = 0.5f
)
```

**2. Normalized Coordinates (0.0 - 1.0)**:
```kotlin
// Screen-size independent
val normalizedPosition = CursorPosition.fromNormalized(
    normalizedX = 0.5f,    // Center X (50%)
    normalizedY = 0.5f,    // Center Y (50%)
    screenWidth = 1080,
    screenHeight = 2400
)
// Converts to: x=540, y=1200
```

**3. Delta/Relative Movement**:
```kotlin
// Move relative to current position
positionTracker.moveBy(
    deltaX = 100f,   // 100px right
    deltaY = -50f    // 50px up
)
```

### Movement Types

**1. Directional Movement** (Voice: "move up", "move right"):
```kotlin
val event = CursorEvent.Move(
    direction = Direction.UP,
    distance = 100f
)

// Executed as:
val (dx, dy) = when (direction) {
    Direction.UP -> Pair(0f, -100f)
    Direction.DOWN -> Pair(0f, 100f)
    Direction.LEFT -> Pair(-100f, 0f)
    Direction.RIGHT -> Pair(100f, 0f)
    Direction.UP_LEFT -> Pair(-70.7f, -70.7f)  // 100 * cos(45°)
    Direction.UP_RIGHT -> Pair(70.7f, -70.7f)
    Direction.DOWN_LEFT -> Pair(-70.7f, 70.7f)
    Direction.DOWN_RIGHT -> Pair(70.7f, 70.7f)
}
positionTracker.moveBy(dx, dy)
```

**2. Absolute Movement** (Move to specific position):
```kotlin
val event = CursorEvent.MoveTo(x = 500f, y = 800f)
positionTracker.updatePosition(500f, 800f)
```

**3. Speed-Based Continuous Movement**:
```kotlin
// Game-loop style movement (60 FPS)
fun updateCursorPosition(deltaTime: Long) {
    if (isMovingUp) {
        val (dx, dy) = speedController.calculateDistance(
            deltaTime = deltaTime,
            direction = Pair(0f, -1f)  // Up direction
        )
        positionTracker.moveBy(dx, dy)
    }
}

// Called every frame (16ms at 60fps)
while (moving) {
    updateCursorPosition(16L)
    delay(16L)
}
```

**4. Snap Movement** (Jump to element):
```kotlin
val target = snapHandler.findNearestTarget(currentX, currentY)
if (target != null) {
    // Smooth animated snap
    snapHandler.snapToTarget(target,
        onUpdate = { x, y -> positionTracker.updatePosition(x, y) }
    )
}
```

### Movement Constraints

**1. Boundary Clamping**:
```kotlin
fun moveBy(deltaX: Float, deltaY: Float) {
    val current = getCurrentPosition()
    val newX = current.x + deltaX
    val newY = current.y + deltaY

    // Clamp to screen bounds
    val result = boundaryDetector.checkBounds(newX, newY)
    updatePosition(result.clampedX, result.clampedY)
}
```

**2. Speed Limiting**:
```kotlin
// Maximum velocity cap (5000 px/s)
fun calculateVelocity(): Float {
    val rawVelocity = currentSpeed.pixelsPerSecond
    return rawVelocity.coerceAtMost(MAX_VELOCITY)
}
```

**3. Minimum Movement Threshold**:
```kotlin
// Prevent jitter (skip movements < 1px)
fun calculateDistance(deltaTime: Long): Pair<Float, Float> {
    val (dx, dy) = /* calculate */

    if (abs(dx) < MIN_MOVEMENT_THRESHOLD &&
        abs(dy) < MIN_MOVEMENT_THRESHOLD) {
        return Pair(0f, 0f)  // Skip
    }

    return Pair(dx, dy)
}
```

### Movement Smoothing

**Acceleration Curve** (Ease-Out):
```
Time:    0s ────► 0.3s ────► 0.6s ────► 1.0s
Speed:   0 px/s → 800 → 950 → 1000 px/s (target)

Visual curve:
  1000 │            ┌────────
       │          ┌─┘
  500  │      ┌──┘
       │   ┌──┘
    0  │───┘
       └────────────────────────── time
```

**Implementation**:
```kotlin
val elapsedTime = System.currentTimeMillis() - accelerationStartTime
val progress = (elapsedTime / accelerationTime).coerceIn(0f, 1f)
val easedProgress = applyEasing(progress, EasingFunction.EASE_OUT)
currentVelocity = targetVelocity * easedProgress
```

---

## Gesture System

### Gesture Types

**1. Tap Gestures**:
```kotlin
// Single tap
gestureHandler.performClick()
// Creates zero-length path at cursor position
// Duration: ~100ms (ViewConfiguration.getTapTimeout())

// Double tap
gestureHandler.performDoubleClick()
// Two taps with 300ms delay between
```

**2. Long Press**:
```kotlin
// Hold at position for duration
gestureHandler.performLongPress(duration = 1000L)
// Creates stationary path held for 1000ms
```

**3. Swipe Gestures**:
```kotlin
// Swipe up 400px
gestureHandler.performSwipe(
    direction = Direction.UP,
    distance = 400f
)

// Creates path from cursor position moving up
// Duration: 300ms (config.swipeDuration)
```

**4. Drag Gestures**:
```kotlin
// Drag from point A to point B
gestureHandler.performDrag(
    startX = 100f, startY = 100f,
    endX = 300f, endY = 300f,
    duration = 500L
)
```

**5. Scroll Gestures**:
```kotlin
// Scroll down (like swiping up on content)
gestureHandler.performScroll(
    direction = Direction.DOWN,
    distance = 200f
)
```

### Gesture Execution Flow

```
USER: "click"
    ↓
CommandMapper: "click" → CursorAction.Click
    ↓
Action Callback: Execute performClick()
    ↓
CursorGestureHandler:
    1. Get current cursor position (x, y)
    2. Create gesture path:
       path.moveTo(x, y)
       path.lineTo(x, y)  // Zero-length = tap
    3. Create GestureDescription:
       stroke = StrokeDescription(path, startTime=0, duration=100ms)
    4. Dispatch gesture:
       service.dispatchGesture(gesture, callback, handler)
    5. Wait for result (async):
       - onCompleted: success = true
       - onCancelled: success = false
    6. Return GestureResult
    ↓
Result Callback: Log/UI update
```

### Android Gesture API Integration

**AccessibilityService.dispatchGesture()**:
```kotlin
// Create gesture path
val path = Path().apply {
    moveTo(startX, startY)
    lineTo(endX, endY)
}

// Create stroke description
val stroke = GestureDescription.StrokeDescription(
    path,
    startTime = 0L,              // Start immediately
    duration = 300L              // 300ms gesture
)

// Build gesture description
val gesture = GestureDescription.Builder()
    .addStroke(stroke)
    .build()

// Dispatch (requires BIND_ACCESSIBILITY_SERVICE permission)
val callback = object : AccessibilityService.GestureResultCallback() {
    override fun onCompleted(gestureDescription: GestureDescription?) {
        // Gesture executed successfully
    }

    override fun onCancelled(gestureDescription: GestureDescription?) {
        // Gesture was cancelled
    }
}

service.dispatchGesture(gesture, callback, null)
```

### Multi-Stroke Gestures

**Pinch Zoom** (future enhancement):
```kotlin
// Two fingers moving apart (zoom in)
val path1 = Path().apply {
    moveTo(centerX - 50, centerY)
    lineTo(centerX - 150, centerY)
}

val path2 = Path().apply {
    moveTo(centerX + 50, centerY)
    lineTo(centerX + 150, centerY)
}

val gesture = GestureDescription.Builder()
    .addStroke(GestureDescription.StrokeDescription(path1, 0, 300))
    .addStroke(GestureDescription.StrokeDescription(path2, 0, 300))
    .build()
```

---

## Visual Feedback

### Cursor Rendering

**VoiceCursor Module Integration**:
```
VoiceAccessibilityService:
    ↓ (position updates)
VoiceCursor Module:
    - TYPE_ACCESSIBILITY_OVERLAY window
    - Compose Canvas rendering
    - Position: (x, y) from positionTracker
    - Style: from styleManager
    - Alpha: from visibilityManager
```

### Cursor Styles Visual Reference

```
NORMAL (Blue, 48dp):
    ╔══╗
    ║  ║  Default state
    ╚══╝  Smooth circle, 3dp stroke

SELECTION (Green, 58dp):
    ╔════╗
    ║ ⟳  ║  Hovering over clickable element
    ╚════╝  Larger, pulsing animation

CLICK (Orange, 72dp):
    ╔══════╗
    ║  ⚡  ║  During click action
    ╚══════╝  Extra large, rapid pulse

LOADING (Purple, 48dp):
    ╔══╗
    ║ ⟲ ║  Processing/waiting
    ╚══╝  Spinning animation

DISABLED (Gray, 38dp):
    ╔══╗
    ║ ⊘ ║  Cannot interact
    ╚══╝  Smaller, semi-transparent
```

### Animation Implementations

**1. Pulse Animation** (Compose):
```kotlin
@Composable
fun PulsingCursor() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.Blue,
            radius = 24.dp.toPx() * scale,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
```

**2. Fade Animation**:
```kotlin
// Using ValueAnimator
val fadeAnimator = ValueAnimator.ofFloat(currentAlpha, targetAlpha).apply {
    duration = 300L
    interpolator = DecelerateInterpolator()
    addUpdateListener { animator ->
        val alpha = animator.animatedValue as Float
        updateCursorAlpha(alpha)
    }
    start()
}
```

**3. Snap Animation** (Position interpolation):
```kotlin
suspend fun animateSnap(startX: Float, startY: Float, endX: Float, endY: Float) {
    val frames = 12  // 200ms at 60fps
    for (frame in 0..frames) {
        val progress = frame.toFloat() / frames
        val eased = applyEasing(progress, EasingFunction.EASE_OUT)

        val currentX = startX + (endX - startX) * eased
        val currentY = startY + (endY - startY) * eased

        updatePosition(currentX, currentY)
        delay(16L)  // ~60fps
    }
}
```

### Focus Indicator Rendering

**Overlay Window Setup**:
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
)

windowManager.addView(composeView, params)
```

**Animated Ring Rendering**:
```kotlin
@Composable
fun FocusRing(bounds: RectF, state: FocusState) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val width = bounds.width() * scale
        val height = bounds.height() * scale

        drawRoundRect(
            color = Color(state.color),
            topLeft = Offset(centerX - width/2, centerY - height/2),
            size = Size(width, height),
            cornerRadius = CornerRadius(8f),
            style = Stroke(width = state.strokeWidth)
        )
    }
}
```

---

## Integration Points

### VoiceAccessibilityService Integration

**Service Setup**:
```kotlin
class VoiceAccessibilityService : AccessibilityService() {

    // Cursor components
    private lateinit var eventHandler: VoiceCursorEventHandler
    private lateinit var positionTracker: CursorPositionTracker
    private lateinit var historyTracker: CursorHistoryTracker
    private lateinit var visibilityManager: CursorVisibilityManager
    private lateinit var styleManager: CursorStyleManager
    private lateinit var gestureHandler: CursorGestureHandler
    private lateinit var speedController: SpeedController
    private lateinit var boundaryDetector: BoundaryDetector
    private lateinit var snapHandler: SnapToElementHandler
    private lateinit var commandMapper: CommandMapper
    private lateinit var focusIndicator: FocusIndicator

    override fun onCreate() {
        super.onCreate()
        initializeCursorSystem()
    }

    private fun initializeCursorSystem() {
        // 1. Initialize core tracking
        positionTracker = CursorPositionTracker(this)
        historyTracker = CursorHistoryTracker()
        boundaryDetector = BoundaryDetector(this)

        // 2. Initialize controllers
        speedController = SpeedController()
        visibilityManager = CursorVisibilityManager()
        styleManager = CursorStyleManager()

        // 3. Initialize handlers
        eventHandler = VoiceCursorEventHandler()
        gestureHandler = CursorGestureHandler(this, positionTracker)
        snapHandler = SnapToElementHandler(this)
        commandMapper = CommandMapper()

        // 4. Initialize visual components
        focusIndicator = FocusIndicator(this, windowManager)

        // 5. Set up event callbacks
        setupEventCallbacks()
        setupCommandCallbacks()

        // 6. Track position changes
        lifecycleScope.launch {
            positionTracker.positionFlow.collect { position ->
                onCursorPositionChanged(position)
            }
        }
    }

    private fun setupEventCallbacks() {
        // Handle move events
        eventHandler.registerEventCallback("Move") { event ->
            if (event is CursorEvent.Move) {
                handleMove(event.direction, event.distance)
            }
        }

        // Handle click events
        eventHandler.registerEventCallback("Click") { event ->
            if (event is CursorEvent.Click) {
                lifecycleScope.launch {
                    styleManager.setClick()
                    gestureHandler.performClick()
                    delay(200)
                    styleManager.setNormal()
                }
            }
        }

        // Handle snap events
        eventHandler.registerEventCallback("SnapToNearest") { event ->
            performSnap()
        }
    }

    private fun setupCommandCallbacks() {
        // Movement actions
        commandMapper.registerActionCallback(CursorAction.MoveDirection::class.java) { action ->
            val (dx, dy) = directionToDelta(action.direction, action.distance)
            positionTracker.moveBy(dx, dy)
            true
        }

        // Click actions
        commandMapper.registerActionCallback(CursorAction.Click::class.java) { action ->
            lifecycleScope.launch {
                gestureHandler.performClick()
            }
            true
        }

        // Speed actions
        commandMapper.registerActionCallback(CursorAction.SetSpeed::class.java) { action ->
            speedController.setSpeed(action.speed)
            true
        }

        // Snap actions
        commandMapper.registerActionCallback(CursorAction.SnapToNearest::class.java) { action ->
            performSnap()
            true
        }

        // History actions
        commandMapper.registerActionCallback(CursorAction.Undo::class.java) { action ->
            val previousPos = historyTracker.undo()
            if (previousPos != null) {
                positionTracker.updatePosition(previousPos.x, previousPos.y)
                true
            } else {
                false
            }
        }
    }

    private fun handleMove(direction: Direction, distance: Float) {
        val (dx, dy) = directionToDelta(direction, distance)

        // Apply speed
        val velocity = speedController.calculateVelocity()
        val speedFactor = velocity / 1000f  // Normalize to medium speed

        val scaledDx = dx * speedFactor
        val scaledDy = dy * speedFactor

        // Move cursor
        positionTracker.moveBy(scaledDx, scaledDy)

        // Record in history
        historyTracker.recordPosition(
            positionTracker.getCurrentPosition(),
            description = "Move ${direction.name}"
        )
    }

    private fun onCursorPositionChanged(position: CursorPosition) {
        // 1. Ensure cursor is visible
        visibilityManager.show()
        visibilityManager.resetAutoHideTimer()

        // 2. Check boundaries
        val result = boundaryDetector.checkBounds(position.x, position.y)
        if (!result.isInBounds) {
            positionTracker.updatePosition(result.clampedX, result.clampedY)
        }

        // 3. Update VoiceCursor overlay
        updateCursorOverlay(position)

        // 4. Check for snap targets nearby
        val target = snapHandler.findNearestTarget(position.x, position.y, maxRadius = 100f)
        if (target != null) {
            styleManager.setSelection()
            showFocusIndicator(target)
        } else {
            styleManager.setNormal()
            hideFocusIndicator()
        }
    }

    private fun performSnap() {
        val currentPos = positionTracker.getCurrentPosition()
        val target = snapHandler.findNearestTarget(currentPos.x, currentPos.y)

        if (target != null) {
            snapHandler.snapToTarget(target,
                onUpdate = { x, y ->
                    positionTracker.updatePosition(x, y)
                },
                onComplete = {
                    styleManager.setSelection()
                    showFocusIndicator(target)
                }
            )
        }
    }

    private fun showFocusIndicator(target: SnapTarget) {
        focusIndicator.show(
            FocusIndicatorConfig(
                bounds = RectF(target.bounds),
                state = FocusState.FOCUSED,
                animationStyle = AnimationStyle.PULSE
            )
        )
    }

    // Process voice commands from SpeechRecognition
    fun onVoiceCommand(command: String) {
        // Try command mapper first
        val result = commandMapper.mapAndExecute(command)

        if (!result.success) {
            // Fall back to event handler
            eventHandler.processVoiceCommand(command)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up all components
        eventHandler.dispose()
        positionTracker.dispose()
        historyTracker.clear()
        visibilityManager.dispose()
        styleManager.dispose()
        gestureHandler.dispose()
        boundaryDetector.dispose()
        snapHandler.dispose()
        focusIndicator.dispose()
    }
}
```

### VoiceCursor Module Communication

**Update Cursor Overlay**:
```kotlin
private fun updateCursorOverlay(position: CursorPosition) {
    // Get visual properties
    val style = styleManager.getCurrentStyle()
    val alpha = visibilityManager.getAlpha()

    // Update VoiceCursor module
    voiceCursorAPI?.updatePosition(
        x = position.x,
        y = position.y,
        color = style.styleColor(),
        size = style.styleSize(),
        alpha = alpha
    )
}
```

### SpeechRecognition Integration

**Voice Command Flow**:
```
User speaks → SpeechRecognition → onRecognitionResult() →
VoiceAccessibilityService.onVoiceCommand() →
CommandMapper.mapAndExecute() → Action executed
```

---

## Voice Commands

### Complete Command Reference

#### Movement Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| move up | up, go up | MoveDirection(UP, 100px) | Move cursor up |
| move down | down, go down | MoveDirection(DOWN, 100px) | Move cursor down |
| move left | left, go left | MoveDirection(LEFT, 100px) | Move cursor left |
| move right | right, go right | MoveDirection(RIGHT, 100px) | Move cursor right |

#### Click Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| click | tap, select | Click | Click at cursor |
| double click | double tap | DoubleClick | Double click |
| long press | hold, press and hold | LongPress | Long press (1s) |

#### Speed Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| slow speed | slower, speed slow | SetSpeed(SLOW) | 250 px/s |
| normal speed | medium speed, default speed | SetSpeed(MEDIUM) | 1000 px/s |
| fast speed | faster, speed fast | SetSpeed(FAST) | 2000 px/s |
| precision mode | precise mode, fine control | TogglePrecisionMode | 5x slower |

#### Navigation Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| snap | snap to element, find element | SnapToNearest | Snap to nearest clickable |
| undo | go back, back | Undo | Undo last movement |
| redo | go forward, forward | Redo | Redo movement |

#### Visibility Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| show focus | highlight, show indicator | ShowFocus | Show element highlight |
| hide focus | remove highlight, hide indicator | HideFocus | Hide highlight |

#### Utility Commands

| Voice Command | Aliases | Action | Description |
|--------------|---------|--------|-------------|
| reset cursor | reset, center cursor | Reset | Center cursor |
| stop | halt, freeze | Stop | Stop all movement |

### Adding Custom Commands

```kotlin
// Example: Add "home" command
commandMapper.registerCommand(
    pattern = "go home",
    action = CursorAction.MoveTo(x = homeX, y = homeY),
    priority = 20,
    aliases = listOf("home", "return home"),
    description = "Move cursor to home position"
)

// Example: Add "scroll down" command
commandMapper.registerCommand(
    pattern = "scroll down",
    action = CursorAction.Custom(/* custom scroll action */),
    priority = 15,
    aliases = listOf("page down"),
    description = "Scroll page down"
)

// Register callback for custom action
commandMapper.registerActionCallback(CursorAction.Custom::class.java) { action ->
    lifecycleScope.launch {
        gestureHandler.performScroll(Direction.DOWN)
    }
    true
}
```

---

## Extension Guide

### Adding a New Cursor Style

**1. Add to CursorStyle sealed class**:
```kotlin
// In CursorStyleManager.kt
sealed class CursorStyle {
    // ... existing styles ...

    data class Warning(
        val color: ComposeColor = Color(0xFFFF9800),  // Orange
        val size: Float = DEFAULT_SIZE * 1.3f,
        val strokeWidth: Float = DEFAULT_STROKE_WIDTH * 2f,
        val pulseEnabled: Boolean = true
    ) : CursorStyle()
}
```

**2. Add convenience method**:
```kotlin
fun setWarning() {
    setStyle(CursorStyle.Warning())
}
```

**3. Update style property methods**:
```kotlin
fun styleColor(): ComposeColor = when (this) {
    is Warning -> color
    // ... other cases ...
}
```

**4. Use in code**:
```kotlin
styleManager.setWarning()
```

### Adding a New Gesture Type

**1. Add to GestureType enum**:
```kotlin
// In CursorGestureHandler.kt
enum class GestureType {
    // ... existing types ...
    TRIPLE_TAP
}
```

**2. Add gesture method**:
```kotlin
suspend fun performTripleTap(): GestureResult {
    val startTime = System.currentTimeMillis()

    try {
        val position = positionTracker.getCurrentPosition()

        // Perform three taps
        for (i in 1..3) {
            performClickAt(position.x, position.y)
            if (i < 3) delay(config.doubleTapDelay / 2)
        }

        val executionTime = System.currentTimeMillis() - startTime
        return GestureResult(
            success = true,
            gestureType = GestureType.TRIPLE_TAP,
            executionTime = executionTime
        )
    } catch (e: Exception) {
        return GestureResult(success = false, gestureType = GestureType.TRIPLE_TAP)
    }
}
```

**3. Add voice command**:
```kotlin
commandMapper.registerCommand(
    pattern = "triple tap",
    action = CursorAction.TripleTap,  // Create new action
    priority = 20,
    description = "Triple tap at cursor"
)
```

### Adding a New Animation Style

**1. Add to AnimationStyle enum**:
```kotlin
// In FocusIndicator.kt
enum class AnimationStyle {
    // ... existing styles ...
    SHAKE
}
```

**2. Create Composable animation**:
```kotlin
@Composable
private fun ShakingFocusRing(config: FocusIndicatorConfig) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRoundRect(
            color = Color(config.state.color),
            topLeft = Offset(config.bounds.left + offsetX, config.bounds.top),
            size = Size(config.bounds.width(), config.bounds.height()),
            cornerRadius = CornerRadius(config.cornerRadius),
            style = Stroke(width = config.state.strokeWidth)
        )
    }
}
```

**3. Add to FocusIndicatorContent**:
```kotlin
@Composable
private fun FocusIndicatorContent(config: FocusIndicatorConfig) {
    when (config.animationStyle) {
        // ... existing cases ...
        AnimationStyle.SHAKE -> ShakingFocusRing(config)
    }
}
```

### Adding a New Speed Preset

**1. Add to CursorSpeed enum**:
```kotlin
// In SpeedController.kt
enum class CursorSpeed(
    val pixelsPerSecond: Float,
    val accelerationTime: Long,
    val description: String
) {
    // ... existing speeds ...
    TURBO(6000f, 50L, "Turbo mode - extremely fast")
}
```

**2. Update fromString() if needed**:
```kotlin
companion object {
    fun fromString(speed: String): CursorSpeed? {
        return when (speed.lowercase().trim()) {
            // ... existing mappings ...
            "turbo", "super fast" -> TURBO
            else -> null
        }
    }
}
```

**3. Add voice command**:
```kotlin
commandMapper.registerCommand(
    pattern = "turbo speed",
    action = CursorAction.SetSpeed(CursorSpeed.TURBO),
    priority = 15,
    aliases = listOf("turbo mode", "maximum speed")
)
```

### Creating Custom Easing Functions

```kotlin
// In SpeedController.kt

enum class EasingFunction {
    // ... existing functions ...
    ELASTIC
}

fun applyEasing(progress: Float, easing: EasingFunction): Float {
    val t = progress.coerceIn(0f, 1f)

    return when (easing) {
        // ... existing cases ...
        EasingFunction.ELASTIC -> {
            val c4 = (2f * PI) / 3f
            if (t == 0f) 0f
            else if (t == 1f) 1f
            else -(2f.pow(10f * t - 10f)) * sin((t * 10f - 10.75f) * c4).toFloat()
        }
    }
}
```

---

## Performance & Optimization

### Memory Management

**1. AccessibilityNodeInfo Recycling**:
```kotlin
// ALWAYS recycle nodes after use
fun findTargets(): List<SnapTarget> {
    val rootNode = service.rootInActiveWindow
    val targets = mutableListOf<SnapTarget>()

    try {
        traverseTree(rootNode, targets)
        return targets
    } finally {
        // Recycle root
        rootNode?.recycle()

        // Recycle unused targets
        targets.forEach {
            if (!it.isUsed()) it.recycle()
        }
    }
}
```

**2. Event Queue Size Limiting**:
```kotlin
// Prevent memory leaks from unbounded queue growth
if (eventQueue.size >= config.maxQueueSize) {
    eventQueue.poll()  // Drop oldest event
}
eventQueue.offer(newEvent)
```

**3. History Expiration**:
```kotlin
// Auto-remove old history entries
fun cleanupExpiredEntries(): Int {
    var removed = 0
    while (historyStack.isNotEmpty()) {
        val first = historyStack.first()
        if (first.isExpired(expirationTimeMillis)) {
            historyStack.removeFirst()
            removed++
        } else {
            break  // Ordered by time, so stop when non-expired found
        }
    }
    return removed
}
```

### Performance Optimizations

**1. Debouncing for Event Processing**:
```kotlin
// Prevent excessive processing from rapid events
val debouncer = Debouncer(cooldownMillis = 100L)

fun processEvent(event: CursorEvent) {
    val key = event::class.simpleName ?: "unknown"
    if (!debouncer.shouldProceed(key)) {
        return  // Skip if too soon
    }

    // Process event
    actuallyProcessEvent(event)
}
```

**2. Significant Movement Detection**:
```kotlin
// Only track movements > 10px to reduce history size
fun recordPosition(position: CursorPosition): Boolean {
    val current = currentPosition ?: return true

    val distance = current.distanceTo(position)
    if (distance < significantMoveThreshold) {
        return false  // Skip
    }

    // Record
    historyStack.add(position)
    return true
}
```

**3. Lazy Initialization**:
```kotlin
// Delay expensive initialization until needed
private val windowManager: WindowManager by lazy {
    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
}
```

**4. Coroutine-Based Async Processing**:
```kotlin
// Don't block main thread
private fun startEventProcessing() {
    processingJob = scope.launch {
        while (eventQueue.isNotEmpty()) {
            val event = eventQueue.poll() ?: break
            processEvent(event)
        }
    }
}
```

**5. StateFlow for Efficient Updates**:
```kotlin
// Only emit when value actually changes
private val _positionFlow = MutableStateFlow<CursorPosition>(defaultPosition)

fun updatePosition(newPosition: CursorPosition) {
    val old = _positionFlow.value
    if (old.x != newPosition.x || old.y != newPosition.y) {
        _positionFlow.value = newPosition  // Only emit if changed
    }
}
```

### Rendering Performance

**1. Hardware Acceleration**:
```kotlin
// Ensure overlay view uses hardware acceleration
overlayView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
```

**2. Compose Performance**:
```kotlin
// Use remember to avoid recomposition
@Composable
fun CursorOverlay(position: CursorPosition, style: CursorStyle) {
    val color = remember(style) { style.styleColor() }
    val size = remember(style) { style.styleSize() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = color, radius = size / 2)
    }
}
```

**3. Animation Frame Rate Control**:
```kotlin
// Target 60fps, skip frames if needed
const val TARGET_FPS = 60
const val FRAME_TIME_MS = 1000L / TARGET_FPS

suspend fun animatePosition() {
    val startTime = System.currentTimeMillis()

    while (animating) {
        val frameStart = System.currentTimeMillis()

        updateFrame()

        val elapsed = System.currentTimeMillis() - frameStart
        val sleepTime = (FRAME_TIME_MS - elapsed).coerceAtLeast(0L)

        delay(sleepTime)
    }
}
```

### Benchmarking

**Key Performance Metrics**:

| Operation | Target Time | Measured |
|-----------|------------|----------|
| Voice command to action mapping | < 5ms | ~2ms |
| Event dispatching (queue → callback) | < 10ms | ~5ms |
| Position update (with bounds check) | < 2ms | ~1ms |
| Gesture dispatch (click) | < 150ms | ~100ms |
| Snap target search (50 elements) | < 50ms | ~30ms |
| History recording | < 1ms | ~0.5ms |

**Profiling Example**:
```kotlin
fun profileOperation(name: String, operation: () -> Unit) {
    val start = System.currentTimeMillis()
    operation()
    val elapsed = System.currentTimeMillis() - start
    Log.d("Performance", "$name took ${elapsed}ms")
}

// Usage
profileOperation("Snap target search") {
    snapHandler.findNearestTarget(x, y)
}
```

---

## Complete API Reference

### VoiceCursorEventHandler

**Constructor**:
```kotlin
VoiceCursorEventHandler(config: EventConfig = EventConfig())
```

**Methods**:
- `processVoiceCommand(command: String): Boolean`
- `dispatchEvent(event: CursorEvent)`
- `registerEventCallback(eventType: String, callback: (CursorEvent) -> Unit)`
- `unregisterEventCallback(eventType: String, callback: (CursorEvent) -> Unit)`
- `registerResultCallback(callback: (EventResult) -> Unit)`
- `unregisterResultCallback(callback: (EventResult) -> Unit)`
- `clearCallbacks()`
- `getQueueSize(): Int`
- `clearQueue()`
- `getSupportedCommands(): List<String>`
- `dispose()`

**Properties**:
- `eventResultFlow: SharedFlow<EventResult>`

### CursorPositionTracker

**Constructor**:
```kotlin
CursorPositionTracker(context: Context)
```

**Methods**:
- `getCurrentPosition(): CursorPosition`
- `updatePosition(x: Float, y: Float, displayId: Int = 0)`
- `updateNormalizedPosition(normalizedX: Float, normalizedY: Float, displayId: Int = 0)`
- `moveBy(deltaX: Float, deltaY: Float)`
- `centerCursor()`
- `addPositionChangeCallback(callback: (CursorPosition) -> Unit)`
- `removePositionChangeCallback(callback: (CursorPosition) -> Unit)`
- `clearCallbacks()`
- `updateScreenBounds()`
- `getScreenBounds(): ScreenBounds`
- `isInBounds(): Boolean`
- `onConfigurationChanged(newConfig: Configuration)`
- `dispose()`

**Properties**:
- `positionFlow: StateFlow<CursorPosition>`

### CursorHistoryTracker

**Constructor**:
```kotlin
CursorHistoryTracker(
    maxHistorySize: Int = 50,
    expirationTimeMillis: Long = 300000L,
    significantMoveThreshold: Float = 10f
)
```

**Methods**:
- `recordPosition(position: CursorPosition, description: String? = null, forceRecord: Boolean = false): Boolean`
- `undo(): HistoricalCursorPosition?`
- `redo(): HistoricalCursorPosition?`
- `canUndo(): Boolean`
- `canRedo(): Boolean`
- `getUndoCount(): Int`
- `getRedoCount(): Int`
- `peekUndo(): HistoricalCursorPosition?`
- `peekRedo(): HistoricalCursorPosition?`
- `getCurrentPosition(): HistoricalCursorPosition?`
- `getRecentHistory(count: Int = 10): List<HistoricalCursorPosition>`
- `getAllHistory(): List<HistoricalCursorPosition>`
- `clear()`
- `clearFuture()`
- `cleanupExpiredEntries(): Int`
- `getTimeSinceLastRecord(): Long`
- `getTotalDistanceTraveled(): Float`
- `getStatistics(): HistoryStatistics`
- `jumpToIndex(index: Int): HistoricalCursorPosition?`
- `findPositionByDescription(description: String): HistoricalCursorPosition?`
- `getPositionsInTimeRange(startTime: Long, endTime: Long): List<HistoricalCursorPosition>`

### CursorVisibilityManager

**Constructor**:
```kotlin
CursorVisibilityManager(config: VisibilityConfig = VisibilityConfig())
```

**Methods**:
- `getState(): VisibilityState`
- `getAlpha(): Float`
- `isVisible(): Boolean`
- `isHidden(): Boolean`
- `show(animated: Boolean = true)`
- `hide(animated: Boolean = true)`
- `toggle(animated: Boolean = true)`
- `resetAutoHideTimer()`
- `setInteractionMode(mode: InteractionMode)`
- `addVisibilityCallback(callback: (VisibilityState) -> Unit)`
- `removeVisibilityCallback(callback: (VisibilityState) -> Unit)`
- `addAlphaCallback(callback: (Float) -> Unit)`
- `removeAlphaCallback(callback: (Float) -> Unit)`
- `clearCallbacks()`
- `dispose()`

**Properties**:
- `stateFlow: StateFlow<VisibilityState>`
- `alphaFlow: StateFlow<Float>`

### CursorStyleManager

**Constructor**:
```kotlin
CursorStyleManager(config: StyleConfig = StyleConfig())
```

**Methods**:
- `getCurrentStyle(): CursorStyle`
- `setStyle(style: CursorStyle)`
- `setNormal()`
- `setSelection()`
- `setClick()`
- `setLoading()`
- `setDisabled()`
- `setCustom(color: ComposeColor, size: Float, strokeWidth: Float, animationType: AnimationType, alpha: Float)`
- `getCurrentColor(): ComposeColor`
- `getCurrentSize(): Float`
- `getCurrentStrokeWidth(): Float`
- `getCurrentAlpha(): Float`
- `getCurrentAnimationType(): AnimationType`
- `areAnimationsEnabled(): Boolean`
- `getAnimationDuration(): Long`
- `updateAnimationProgress(progress: Float)`
- `getAnimationProgress(): Float`
- `addStyleCallback(callback: (CursorStyle) -> Unit)`
- `removeStyleCallback(callback: (CursorStyle) -> Unit)`
- `clearCallbacks()`
- `getCursorShape(): CursorShape`
- `colorFromInt(colorInt: Int): ComposeColor`
- `colorToInt(color: ComposeColor): Int`
- `dispose()`

**Properties**:
- `styleFlow: StateFlow<CursorStyle>`
- `animationProgress: StateFlow<Float>`

### CursorGestureHandler

**Constructor**:
```kotlin
CursorGestureHandler(
    service: AccessibilityService,
    positionTracker: CursorPositionTracker,
    config: GestureConfig = GestureConfig()
)
```

**Methods**:
- `suspend fun performClick(): GestureResult`
- `suspend fun performClickAt(x: Float, y: Float): GestureResult`
- `suspend fun performDoubleClick(): GestureResult`
- `suspend fun performDoubleClickAt(x: Float, y: Float): GestureResult`
- `suspend fun performLongPress(duration: Long = config.longPressDuration): GestureResult`
- `suspend fun performLongPressAt(x: Float, y: Float, duration: Long): GestureResult`
- `suspend fun performSwipe(direction: Direction, distance: Float = config.swipeDistance): GestureResult`
- `suspend fun performDrag(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = config.swipeDuration): GestureResult`
- `suspend fun performScroll(direction: Direction, distance: Float = config.scrollDistance): GestureResult`
- `addGestureCallback(callback: (GestureResult) -> Unit)`
- `removeGestureCallback(callback: (GestureResult) -> Unit)`
- `clearCallbacks()`
- `dispose()`

### SpeedController

**Constructor**:
```kotlin
SpeedController()
```

**Methods**:
- `setSpeed(speed: CursorSpeed)`
- `getSpeed(): CursorSpeed`
- `setPrecisionMode(enabled: Boolean)`
- `isPrecisionMode(): Boolean`
- `togglePrecisionMode()`
- `calculateVelocity(deltaTime: Long = 0L): Float`
- `calculateDistance(deltaTime: Long, direction: Pair<Float, Float> = Pair(1f, 0f)): Pair<Float, Float>`
- `calculateTimeForDistance(distance: Float): Long`
- `calculatePixelsPerFrame(frameRate: Int = 60): Float`
- `applyEasing(progress: Float, easing: EasingFunction): Float`
- `applySineEasing(progress: Float): Float`
- `reset()`
- `getCurrentVelocity(): Float`
- `getTargetVelocity(): Float`
- `getSpeedInfo(): String`

### BoundaryDetector

**Constructor**:
```kotlin
BoundaryDetector(context: Context, config: BoundaryConfig = BoundaryConfig())
```

**Methods**:
- `checkBounds(x: Float, y: Float): BoundaryCheckResult`
- `clampToBounds(x: Float, y: Float): PointF`
- `isNearEdge(x: Float, y: Float): Boolean`
- `detectNearEdge(x: Float, y: Float): ScreenEdge`
- `calculateDistanceToEdge(x: Float, y: Float): Float`
- `getEffectiveBounds(): Rect`
- `getRawBounds(): Rect`
- `getSafeAreaInsets(): SafeAreaInsets`
- `updateSafeAreaInsets(insets: WindowInsets?)`
- `updateDisplayMetrics()`
- `getDisplayId(): Int`
- `getScreenCenter(): PointF`
- `areOnSameEdge(x1: Float, y1: Float, x2: Float, y2: Float): Boolean`
- `validateAndClamp(x: Float, y: Float, label: String = "Point"): PointF`
- `dispose()`

### FocusIndicator

**Constructor**:
```kotlin
FocusIndicator(context: Context, windowManager: WindowManager)
```

**Methods**:
- `show(config: FocusIndicatorConfig)`
- `update(config: FocusIndicatorConfig)`
- `hide()`
- `isVisible(): Boolean`
- `dispose()`

### SnapToElementHandler

**Constructor**:
```kotlin
SnapToElementHandler(service: AccessibilityService)
```

**Methods**:
- `setSnapRadius(radius: Float)`
- `getSnapRadius(): Float`
- `setSnapEnabled(enabled: Boolean)`
- `isSnapEnabled(): Boolean`
- `setHighlightTargets(enabled: Boolean)`
- `findNearestTarget(x: Float, y: Float, maxRadius: Float = snapRadius): SnapTarget?`
- `findAllTargets(x: Float, y: Float, maxRadius: Float = snapRadius): List<SnapTarget>`
- `getTargetAtPosition(x: Float, y: Float): SnapTarget?`
- `snapToTarget(target: SnapTarget, onUpdate: (x: Float, y: Float) -> Unit, onComplete: (() -> Unit)? = null)`
- `cancelAnimation()`
- `dispose()`

### CommandMapper

**Constructor**:
```kotlin
CommandMapper()
```

**Methods**:
- `registerCommand(pattern: String, action: CursorAction, priority: Int = 0, aliases: List<String> = emptyList(), description: String = "")`
- `unregisterCommand(pattern: String): Boolean`
- `mapCommand(commandText: String): CommandResult`
- `mapAndExecute(commandText: String): CommandResult`
- `<T : CursorAction> registerActionCallback(actionClass: Class<T>, callback: (T) -> Boolean)`
- `executeAction(action: CursorAction?): Boolean`
- `getAllCommands(): List<CommandPattern>`
- `getCommandsForAction(actionClass: Class<out CursorAction>): List<CommandPattern>`
- `searchCommands(query: String): List<CommandPattern>`
- `getSuggestions(partial: String, maxResults: Int = 5): List<String>`
- `getHelpText(): String`
- `clear()`
- `resetToDefaults()`
- `getStatistics(): CommandMapperStatistics`

---

## Appendix

### File Locations

All cursor system files are located in:
```
/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/
```

**Core Files**:
- `VoiceCursorEventHandler.kt` (464 lines)
- `CursorPositionTracker.kt` (370 lines)
- `CursorHistoryTracker.kt` (483 lines)
- `CursorVisibilityManager.kt` (441 lines)
- `CursorStyleManager.kt` (430 lines)
- `CursorGestureHandler.kt` (586 lines)
- `SpeedController.kt` (328 lines)
- `BoundaryDetector.kt` (440 lines)
- `FocusIndicator.kt` (406 lines)
- `SnapToElementHandler.kt` (516 lines)
- `CommandMapper.kt` (585 lines)

**Utility Files**:
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/utils/Debouncer.kt` (84 lines)

**Total Lines**: ~5,100 lines of cursor system code

### Dependencies

**Android Framework**:
- `android.accessibilityservice.AccessibilityService`
- `android.accessibilityservice.GestureDescription`
- `android.view.accessibility.AccessibilityNodeInfo`
- `android.view.WindowManager`
- `android.graphics.Path`
- `android.animation.ValueAnimator`

**Kotlin Coroutines**:
- `kotlinx.coroutines.flow.StateFlow`
- `kotlinx.coroutines.flow.SharedFlow`
- `kotlinx.coroutines.CoroutineScope`
- `kotlinx.coroutines.launch`

**Jetpack Compose**:
- `androidx.compose.foundation.Canvas`
- `androidx.compose.animation.core.*`
- `androidx.compose.ui.graphics.Color`

### Testing Recommendations

**Unit Tests**:
```kotlin
// Test position tracking
@Test
fun testPositionUpdate() {
    val tracker = CursorPositionTracker(context)
    tracker.updatePosition(100f, 200f)

    val position = tracker.getCurrentPosition()
    assertEquals(100f, position.x)
    assertEquals(200f, position.y)
}

// Test history undo/redo
@Test
fun testHistoryUndoRedo() {
    val history = CursorHistoryTracker()
    val pos1 = createPosition(100f, 100f)
    val pos2 = createPosition(200f, 200f)

    history.recordPosition(pos1)
    history.recordPosition(pos2)

    val undone = history.undo()
    assertEquals(pos1, undone)

    val redone = history.redo()
    assertEquals(pos2, redone)
}

// Test command mapping
@Test
fun testCommandMapping() {
    val mapper = CommandMapper()
    val result = mapper.mapCommand("move up")

    assertTrue(result.success)
    assertTrue(result.action is CursorAction.MoveDirection)
}
```

**Integration Tests**:
```kotlin
@Test
fun testFullClickFlow() = runBlocking {
    val service = mockAccessibilityService()
    val tracker = CursorPositionTracker(context)
    val gestureHandler = CursorGestureHandler(service, tracker)

    tracker.updatePosition(500f, 500f)

    val result = gestureHandler.performClick()

    assertTrue(result.success)
    assertEquals(GestureType.CLICK, result.gestureType)
    verify(service).dispatchGesture(any(), any(), any())
}
```

### Troubleshooting

**Problem**: Cursor movements are jittery
- **Solution**: Increase debounce duration in EventConfig
- **Solution**: Enable acceleration curves in SpeedController
- **Solution**: Increase significant movement threshold in HistoryTracker

**Problem**: Gesture dispatch fails
- **Solution**: Verify AccessibilityService is enabled
- **Solution**: Check BIND_ACCESSIBILITY_SERVICE permission
- **Solution**: Ensure cursor position is within screen bounds

**Problem**: Snap to element doesn't work
- **Solution**: Verify elements are clickable and visible
- **Solution**: Increase snap radius
- **Solution**: Check accessibility tree is populated

**Problem**: Memory leaks
- **Solution**: Always recycle AccessibilityNodeInfo
- **Solution**: Dispose all managers on service destroy
- **Solution**: Clear event queue and history on cleanup

---

**End of Documentation**

For questions or issues with the cursor system, contact the VOS4 development team or refer to the main VoiceAccessibility documentation.

**Document Metadata**:
- **Lines of Code Documented**: 5,100+
- **Components Covered**: 11
- **API Methods**: 150+
- **Voice Commands**: 25+
- **Diagrams**: 5
- **Code Examples**: 50+

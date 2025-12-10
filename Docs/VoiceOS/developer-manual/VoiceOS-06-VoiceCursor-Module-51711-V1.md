# Chapter 6: VoiceCursor Module

**VOS4 Developer Manual**
**Version:** 1.0.0
**Last Updated:** 2025-11-03
**Module Version:** 3.0.0
**Target:** Android API 28+

---

## Table of Contents

1. [Overview](#1-overview)
2. [Architecture](#2-architecture)
3. [Cursor Rendering System](#3-cursor-rendering-system)
4. [Movement System](#4-movement-system)
5. [Snap-to-Element Intelligence](#5-snap-to-element-intelligence)
6. [Gesture Management](#6-gesture-management)
7. [Gaze Click Support](#7-gaze-click-support)
8. [IMU Integration](#8-imu-integration)
9. [Boundary Detection](#9-boundary-detection)
10. [Performance Optimization](#10-performance-optimization)
11. [Voice Command Integration](#11-voice-command-integration)
12. [Best Practices](#12-best-practices)

---

## 1. Overview

### 1.1 Introduction

VoiceCursor provides a sophisticated voice-controlled cursor system designed specifically for AR/XR devices where traditional touch input is limited or unavailable. The module enables precise screen interaction through head tracking (IMU), voice commands, and gaze-based control.

**Key Features:**
- **IMU-based head tracking** for hands-free cursor control
- **Gaze-click support** for AR/XR devices with dwell-time activation
- **Intelligent snap-to-element** targeting for improved accuracy
- **Adaptive jitter filtering** for smooth movement
- **Multiple cursor types** (hand pointer, circular, custom)
- **Gesture recognition** (tap, long-press, drag, swipe, pinch)
- **Voice command integration** via CommandManager
- **Edge detection** with visual feedback

### 1.2 Use Cases

| Use Case | Description |
|----------|-------------|
| **AR Headsets** | Hands-free interaction with screen content |
| **Smart Glasses** | Lightweight cursor control for compact displays |
| **Accessibility** | Alternative input for users with limited mobility |
| **Voice-first UIs** | Pure voice control augmented with visual cursor |
| **XR Experiences** | Immersive spatial interaction paradigms |

### 1.3 Module Location

```
/modules/apps/VoiceCursor/
├── src/main/java/com/augmentalis/voiceos/cursor/
│   ├── VoiceCursorAPI.kt          # Public API (modern)
│   ├── VoiceCursor.kt              # Legacy wrapper (deprecated)
│   ├── core/                       # Core functionality
│   ├── view/                       # UI components
│   ├── manager/                    # Overlay & gesture management
│   ├── filter/                     # Jitter filtering
│   ├── helper/                     # IMU integration
│   └── commands/                   # Command handlers (deprecated)
```

### 1.4 Dependencies

**Required Modules:**
- `DeviceManager` - IMU/sensor integration
- `CommandManager` - Voice command routing
- `VoiceAccessibility` - Accessibility service integration

**Android Components:**
- `AccessibilityService` - System-level gesture dispatch
- `WindowManager` - Overlay rendering
- `SensorManager` - IMU access (via DeviceManager)

---

## 2. Architecture

### 2.1 System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    VoiceCursor Module                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────┐  │
│  │ VoiceCursorAPI│───▶│CursorOverlay │───▶│  CursorView │  │
│  │   (Public)    │    │   Manager    │    │  (Renderer) │  │
│  └──────────────┘    └──────────────┘    └─────────────┘  │
│         │                    │                    │         │
│         │                    ▼                    │         │
│         │           ┌──────────────┐             │         │
│         │           │  Gesture     │             │         │
│         │           │  Manager     │             │         │
│         │           └──────────────┘             │         │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────┐  │
│  │   Position   │◀───│ IMU          │───▶│  Renderer   │  │
│  │   Manager    │    │ Integration  │    │             │  │
│  └──────────────┘    └──────────────┘    └─────────────┘  │
│         │                    │                              │
│         ▼                    ▼                              │
│  ┌──────────────┐    ┌──────────────┐                     │
│  │ Cursor       │    │ Gaze Click   │                     │
│  │ Filter       │    │ Manager      │                     │
│  └──────────────┘    └──────────────┘                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
           │                      │
           ▼                      ▼
    ┌──────────────┐      ┌──────────────┐
    │ DeviceManager│      │ CommandManager│
    │   (IMU)      │      │  (Voice Cmd)  │
    └──────────────┘      └──────────────┘
```

### 2.2 Core Components

| Component | File | Purpose |
|-----------|------|---------|
| **VoiceCursorAPI** | `VoiceCursorAPI.kt` | Modern public API for external modules |
| **CursorOverlayManager** | `manager/CursorOverlayManager.kt` | Manages overlay lifecycle and window params |
| **CursorRenderer** | `core/CursorRenderer.kt` | Thread-safe bitmap rendering with animations |
| **CursorPositionManager** | `core/CursorPositionManager.kt` | Position calculations from IMU data |
| **CursorFilter** | `filter/CursorFilter.kt` | Adaptive jitter elimination (<0.1ms overhead) |
| **GestureManager** | `core/GestureManager.kt` | Multi-gesture detection (tap, drag, swipe, pinch) |
| **GazeClickManager** | `core/GazeClickManager.kt` | Dwell-time gaze activation for AR/XR |
| **VoiceCursorIMU** | `helper/VoiceCursorIMUIntegration.kt` | DeviceManager IMU adapter |
| **CursorAnimator** | `core/CursorAnimator.kt` | Smooth animations for cursor transitions |

### 2.3 Architecture Principles

**1. Direct Implementation**
- No interfaces unless strategic value (VOS4 principle)
- Concrete classes with clear responsibilities
- Minimal abstraction overhead

**2. Thread Safety**
- `@Volatile` fields for visibility
- `synchronized` blocks for atomic operations
- Lock objects for complex state updates

**3. Performance First**
- Zero-allocation value classes (`CursorOffset`)
- Integer math for filters (<0.1ms processing)
- Frame rate limiting (~60-120 FPS)
- Bitmap caching with proper disposal

**4. Separation of Concerns**
- Rendering separate from position logic
- IMU integration delegated to DeviceManager
- Command handling moved to CommandManager

### 2.4 API Versions

#### Modern API (VoiceCursorAPI.kt)

**Status:** ✅ Active, Current
**Created:** 2025-09-26
**Purpose:** Singleton object API for external module integration

**Key Methods:**
```kotlin
VoiceCursorAPI.initialize(context, accessibilityService)
VoiceCursorAPI.showCursor(config)
VoiceCursorAPI.hideCursor()
VoiceCursorAPI.moveTo(position, animate)
VoiceCursorAPI.executeAction(action, position)
VoiceCursorAPI.centerCursor()
```

#### Legacy API (VoiceCursor.kt)

**Status:** ⚠️ Deprecated (Oct 2025)
**Removal:** Planned Nov 2025
**Purpose:** Backward compatibility wrapper around VoiceCursorAPI

**Migration Note:** All new code should use `VoiceCursorAPI` directly. Legacy `VoiceCursor` class will be removed in v4.0.0.

---

## 3. Cursor Rendering System

### 3.1 CursorRenderer Architecture

The `CursorRenderer` class handles all cursor drawing operations with thread-safe bitmap management and animation support.

**Location:** `core/CursorRenderer.kt`
**Version:** 2.1.0

#### Key Features

- **Thread-safe rendering** with synchronized blocks
- **Bitmap caching** to prevent memory leaks
- **Animation support** (scale, alpha, glow effects)
- **Multiple cursor types** (hand, round, custom)
- **Error recovery** with fallback cursors
- **Frame rate limiting** (~60 FPS max)

### 3.2 Cursor Types

```kotlin
@Parcelize
sealed class CursorType : Parcelable {
    @Parcelize
    object Hand : CursorType()      // Hand pointer for AR/XR

    @Parcelize
    object Normal : CursorType()    // Circular cursor

    @Parcelize
    object Custom : CursorType()    // User-defined cursor
}
```

#### Hand Cursor

**Use Case:** AR/XR devices where hand metaphor is intuitive
**Resource:** `R.drawable.cursor_hand`
**Center Offset:** `(41.3%, 7.2%)` - optimized for visual pointer tip

```kotlin
// Create hand cursor configuration
val config = CursorConfig(
    type = CursorType.Hand,
    handCursorSize = 48,
    color = 0xFF007AFF.toInt()
)
```

#### Normal Cursor

**Use Case:** General-purpose circular cursor
**Resource:** `R.drawable.cursor_round`
**Center Offset:** `(50%, 50%)` - centered

```kotlin
// Create normal cursor configuration
val config = CursorConfig(
    type = CursorType.Normal,
    size = 48,
    color = 0xFF007AFF.toInt()
)
```

### 3.3 Rendering Pipeline

```
┌─────────────────────┐
│ Update Cursor       │
│ Configuration       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Load/Create Bitmap  │
│ from Resources      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Apply Color Tint    │
│ & Scale             │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Cache Bitmap with   │
│ Disposal Tracking   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Draw on Canvas with │
│ Animation Effects   │
└─────────────────────┘
```

### 3.4 Core Rendering Methods

#### updateCursor()

Updates cursor bitmap based on configuration.

```kotlin
fun updateCursor(
    config: CursorConfig,
    resourceProvider: ResourceProvider
)
```

**Thread Safety:** Synchronized with `renderLock`
**Memory Management:** Disposes old cursor before creating new

**Example:**
```kotlin
val renderer = CursorRenderer(context)
val resourceProvider = ResourceProvider(context)
val config = CursorConfig(
    type = CursorType.Normal,
    size = 48,
    color = Color.RED
)

renderer.updateCursor(config, resourceProvider)
```

#### drawCursor()

Renders cursor on canvas with optional animations.

```kotlin
fun drawCursor(
    canvas: Canvas,
    x: Float,
    y: Float,
    type: CursorType = cursorType
)
```

**Features:**
- Applies scale transformation for animations
- Handles center offset for cursor type
- Gracefully handles recycled bitmaps

**Example:**
```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    renderer.drawCursor(canvas, cursorX, cursorY)
}
```

#### drawCursorWithEffects()

Enhanced rendering with visual effects.

```kotlin
fun drawCursorWithEffects(
    canvas: Canvas,
    x: Float,
    y: Float,
    type: CursorType = cursorType,
    showGlow: Boolean = false,
    showDragFeedback: Boolean = false
)
```

**Effects:**
- **Glow Effect:** Radial gradient halo around cursor
- **Drag Feedback:** Visual indicator during drag operations
- **Animation Support:** Scale and alpha transformations

### 3.5 Animation Support

The renderer integrates with `CursorAnimator` for smooth transitions.

#### Animation Values

```kotlin
// Set animation scale
renderer.setScale(1.2f)  // 120% size

// Set animation alpha
renderer.setAlpha(0.8f)  // 80% opacity

// Apply both from animator
renderer.applyAnimationState(scale, alpha)

// Reset to defaults
renderer.resetAnimationValues()
```

#### Frame Rate Control

```kotlin
// Check if redraw is needed (frame rate limiting)
if (renderer.shouldRedraw()) {
    invalidate()  // Trigger onDraw()
}
```

**Frame Limit:** 16ms (~60 FPS) to prevent excessive redraws

### 3.6 Resource Management

#### ResourceProvider

Validates and loads drawable resources with fallback support.

```kotlin
class ResourceProvider(context: Context) {
    fun getHandCursorResource(): Int
    fun getRoundCursorResource(): Int
    fun getCursorBitmap(size: Int): Bitmap
    fun getCustomCursorResource(type: CursorType): Int
}
```

**Features:**
- Resource validation before loading
- Automatic fallback to default cursor
- Error logging for missing resources

#### Bitmap Lifecycle

```kotlin
// Bitmaps are tracked in cache
private val bitmapCache = mutableListOf<Bitmap>()

// Automatic disposal on update
oldCursor?.let {
    if (it != currentCursor && !it.isRecycled) {
        it.recycle()
        bitmapCache.remove(it)
    }
}

// Manual cleanup
renderer.dispose()  // Recycles all cached bitmaps
```

### 3.7 Visual Effects

#### Glow Effect

```kotlin
private fun drawGlowEffect(canvas: Canvas, x: Float, y: Float) {
    val glowRadius = 30f * currentScale
    val shader = RadialGradient(
        x, y, glowRadius,
        intArrayOf(
            Color.argb(80, 255, 255, 255),
            Color.argb(40, 255, 255, 255),
            Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.7f, 1f),
        Shader.TileMode.CLAMP
    )
    glowPaint.shader = shader
    canvas.drawCircle(x, y, glowRadius, glowPaint)
}
```

#### Drag Effect

```kotlin
private fun drawDragEffect(canvas: Canvas, x: Float, y: Float) {
    val dragRadius = 25f * currentScale
    dragPaint.alpha = (128 * currentAlpha).toInt()

    // Draw indicator circle
    canvas.drawCircle(x, y, dragRadius, dragPaint)

    // Draw crosshair in drag circle
    val halfSize = dragRadius * 0.6f
    canvas.drawLine(x - halfSize, y, x + halfSize, y, dragPaint)
    canvas.drawLine(x, y - halfSize, x, y + halfSize, dragPaint)
}
```

### 3.8 Configuration

```kotlin
data class CursorConfig(
    val type: CursorType = CursorType.Normal,
    val color: Int = 0xFF007AFF.toInt(),  // ARVision systemBlue
    val size: Int = 48,                    // Standard touch target
    val handCursorSize: Int = 48,
    val strokeWidth: Float = 2.0f,
    val cornerRadius: Float = 20.0f,
    val glassOpacity: Float = 0.8f,
    val showCoordinates: Boolean = false
)
```

**ARVision Theme Integration:**
- `systemBlue` (0xFF007AFF) - default color
- Glass morphism opacity (0.8)
- Rounded corners (20.0f)
- Standard touch targets (48dp)

---

## 4. Movement System

### 4.1 Position Management Architecture

The movement system converts IMU sensor data into smooth, precise cursor movements through a multi-stage pipeline.

```
IMU Sensors → Sensor Fusion → Position Calc → Filtering → Edge Detection → Rendering
     │              │               │              │             │
     ▼              ▼               ▼              ▼             ▼
Accelerometer  DeviceManager  PositionManager  CursorFilter  BoundaryDetector
Gyroscope      CursorAdapter
Magnetometer
```

### 4.2 CursorPositionManager

**Location:** `core/CursorPositionManager.kt`
**Version:** 2.2.0

Manages cursor position calculations from orientation data with edge detection and bounce-back effects.

#### Core Features

- **Orientation-based movement** from alpha/beta/gamma angles
- **Moving averages** for sensor smoothing (4-sample window, 300ms)
- **Speed scaling** with configurable displacement factor
- **Edge detection** with 8 edge types (4 sides + 4 corners)
- **Bounce-back physics** with exponential decay
- **Edge resistance** for natural feel at boundaries

### 4.3 Position Calculation

```kotlin
fun calculatePosition(
    alpha: Float,      // Z-axis rotation
    beta: Float,       // X-axis rotation
    gamma: Float,      // Y-axis rotation
    timestamp: Long,
    speedFactor: Int = 8
): PositionResult
```

**Algorithm:**

1. **Apply Moving Averages**
```kotlin
val smoothAlpha = alphaAverage.getAvg(alpha, timestamp)
val smoothBeta = betaAverage.getAvg(beta, timestamp)
val smoothGamma = gammaAverage.getAvg(gamma, timestamp)
```

2. **Calculate Deltas**
```kotlin
val dx = smoothAlpha - previousAlpha
val dy = smoothBeta - previousBeta
val dz = smoothGamma - previousGamma
```

3. **Compute Displacement**
```kotlin
val speedMultiplier = speedFactor * 0.8
val disX = dx * screenWidth * cursorScaleX * speedMultiplier
val disY = dy * screenHeight * cursorScaleY * speedMultiplier
```

4. **Apply Fine Tuning**
```kotlin
// Dampen small movements for precision
val finalDisX = if (abs(disX) < (screenWidth / 80))
    disX * 0.6 else disX
```

5. **Edge Detection & Constraints**
```kotlin
val edgeResult = detectEdges(intendedX, intendedY, currentTime)
val (constrainedX, constrainedY) = applyEdgeConstraints(
    intendedX, intendedY, edgeResult
)
```

6. **Jitter Filtering**
```kotlin
val (filteredX, filteredY) = cursorFilter.filter(
    constrainedX, constrainedY, System.nanoTime()
)
```

**Result:**
```kotlin
data class PositionResult(
    val x: Float,                    // Final X position
    val y: Float,                    // Final Y position
    val distance: Float,             // Movement distance
    val moved: Boolean,              // Did position change?
    val edgeDetected: Boolean,       // At screen edge?
    val edgeType: EdgeType           // Which edge (if any)
)
```

### 4.4 Moving Average Filter

**Class:** `MovingAverage`
**Purpose:** Smooth sensor input by averaging recent values

```kotlin
class MovingAverage(
    private val windowSize: Int,      // Number of samples (typically 4)
    private val timeWindowNs: Long    // Time window (300ms)
)
```

**Algorithm:**
```kotlin
fun getAvg(value: Float, timestamp: Long): Float {
    synchronized(lock) {
        // Store value in circular buffer
        values[index] = value
        timestamps[index] = timestamp
        index = (index + 1) % windowSize

        // Average only recent values
        val cutoffTime = timestamp - timeWindowNs
        var sum = 0f
        var validCount = 0

        for (i in 0 until count) {
            if (timestamps[i] >= cutoffTime) {
                sum += values[i]
                validCount++
            }
        }

        return if (validCount > 0) sum / validCount else value
    }
}
```

**Benefits:**
- Eliminates high-frequency sensor noise
- Time-based window prevents stale data
- Thread-safe with synchronized access

### 4.5 CursorFilter - Adaptive Jitter Elimination

**Location:** `filter/CursorFilter.kt`
**Performance:** <0.1ms processing overhead

Ultra-efficient adaptive filter that eliminates jitter while maintaining responsiveness.

#### Filter Modes

| Motion Level | Threshold | Filter Strength | Use Case |
|--------------|-----------|-----------------|----------|
| **Stationary** | <20 px/s | 75% | Cursor at rest |
| **Slow** | <100 px/s | 30% | Precise movements |
| **Fast** | ≥100 px/s | 5% | Quick navigation |

#### Algorithm

```kotlin
fun filter(x: Float, y: Float, timestamp: Long): Pair<Float, Float> {
    // 1. Calculate instantaneous motion (px/s)
    val deltaTimeSeconds = (timestamp - lastTime) / 1_000_000_000f
    val dx = abs(x - lastX)
    val dy = abs(y - lastY)
    val instantMotion = (dx + dy) / deltaTimeSeconds

    // 2. Update smoothed motion estimate (90% old, 10% new)
    motionLevel = (motionLevel * 90 + instantMotion * 10) / 100

    // 3. Select filter strength based on motion
    val strength = when {
        motionLevel < stationaryThreshold -> stationaryStrength  // 75%
        motionLevel < slowThreshold -> slowStrength              // 30%
        else -> fastStrength                                     // 5%
    }

    // 4. Apply filter using integer math
    val filteredX = ((x * (100 - strength) + lastX * strength) / 100)
    val filteredY = ((y * (100 - strength) + lastY * strength) / 100)

    return Pair(filteredX, filteredY)
}
```

#### Configuration

```kotlin
data class CursorFilterConfig(
    val enabled: Boolean = true,
    val stationaryThreshold: Int = 50,   // px/s
    val slowThreshold: Int = 200,        // px/s
    val stationaryStrength: Int = 90,    // 0-100
    val slowStrength: Int = 50,
    val fastStrength: Int = 10
)
```

#### Usage Example

```kotlin
val filter = CursorFilter()

// Configure sensitivity
filter.updateConfig(
    stationaryThreshold = 30,
    slowThreshold = 150,
    motionSensitivity = 0.8f
)

// Apply filter
val (filteredX, filteredY) = filter.filter(rawX, rawY, timestamp)

// Monitor motion level
val motionLevel = filter.getMotionLevel()  // px/s
val strength = filter.getCurrentStrength() // 0-100
```

### 4.6 Speed Control

The position manager supports configurable speed factors:

```kotlin
// Speed factor: 1-20 (default: 8)
val result = positionManager.calculatePosition(
    alpha, beta, gamma, timestamp,
    speedFactor = 12  // Faster movement
)
```

**Speed Mapping:**
| Factor | Multiplier | Use Case |
|--------|------------|----------|
| 1-4 | 0.8x-3.2x | Slow, precise control |
| 5-8 | 4.0x-6.4x | Normal speed |
| 9-15 | 7.2x-12.0x | Fast navigation |
| 16-20 | 12.8x-16.0x | Rapid movement |

### 4.7 CursorAnimator

**Location:** `core/CursorAnimator.kt`
**Version:** 1.0.0

Provides smooth animations for cursor transitions and visual feedback.

#### Animation Types

```kotlin
enum class AnimationType {
    POSITION_SMOOTH,    // Smooth movement
    POSITION_INSTANT,   // Instant jump
    CLICK_PULSE,        // Click feedback
    HOVER_GLOW,         // Hover state
    DRAG_FEEDBACK,      // Drag indicator
    VISIBILITY_FADE,    // Fade in/out
    STATE_TRANSITION    // General transitions
}
```

#### Position Animation

```kotlin
fun animateToPosition(
    targetPosition: CursorOffset,
    config: AnimationConfig = AnimationConfig.SMOOTH_MOVEMENT,
    force: Boolean = false
)
```

**Example:**
```kotlin
val animator = CursorAnimator()

// Smooth movement to new position
animator.animateToPosition(
    targetPosition = CursorOffset(500f, 300f),
    config = AnimationConfig.SMOOTH_MOVEMENT
)

// Listen for position updates
animator.onPositionUpdate = { position ->
    updateCursorPosition(position)
}
```

#### Click Pulse Animation

```kotlin
// Animate click feedback
animator.animateClickPulse(
    config = AnimationConfig.CLICK_PULSE
)

// Custom pulse configuration
val customPulse = AnimationConfig(
    duration = 150L,
    scaleFrom = 1.0f,
    scaleTo = 1.4f,
    repeatCount = 1,
    repeatMode = ValueAnimator.REVERSE
)
animator.animateClickPulse(customPulse)
```

#### Drag Feedback

```kotlin
// Start drag feedback
animator.animateDragFeedback(
    isDragging = true,
    config = AnimationConfig.DRAG_FEEDBACK
)

// Stop drag feedback
animator.animateDragFeedback(isDragging = false)
```

### 4.8 Integration Example

Complete movement system integration:

```kotlin
class CursorMovementController(context: Context) {
    private val positionManager = CursorPositionManager(
        screenWidth = 1920,
        screenHeight = 1080
    )
    private val animator = CursorAnimator()
    private val renderer = CursorRenderer(context)

    init {
        // Connect animator to renderer
        animator.onScaleUpdate = { scale ->
            renderer.setScale(scale)
        }
        animator.onAlphaUpdate = { alpha ->
            renderer.setAlpha(alpha)
        }
        animator.setEnabled(true)
    }

    fun onOrientationChanged(
        alpha: Float,
        beta: Float,
        gamma: Float,
        timestamp: Long
    ) {
        // Calculate new position
        val result = positionManager.calculatePosition(
            alpha, beta, gamma, timestamp,
            speedFactor = 8
        )

        if (result.moved) {
            // Animate to new position
            animator.animateToPosition(
                CursorOffset(result.x, result.y)
            )

            // Show edge feedback if at boundary
            if (result.edgeDetected) {
                showEdgeFeedback(result.edgeType)
            }
        }
    }

    fun dispose() {
        animator.dispose()
        renderer.dispose()
        positionManager.dispose()
    }
}
```

---

## 5. Snap-to-Element Intelligence

**Status:** Planned Feature
**Target Release:** v3.1.0

Snap-to-element functionality will enable intelligent cursor targeting to improve accuracy when selecting UI elements.

### 5.1 Planned Features

- **Proximity detection** to nearby clickable elements
- **Magnetic snapping** with configurable attraction radius
- **Priority targeting** based on element type (buttons > text > backgrounds)
- **Visual indicators** showing snap target
- **Configurable sensitivity** for different use cases

### 5.2 Future API (Preview)

```kotlin
// Enable snap-to-element
VoiceCursorAPI.enableSnapToElement(
    attractionRadius = 50f,  // pixels
    priority = SnapPriority.BUTTONS_FIRST
)

// Configure snapping behavior
val snapConfig = SnapConfig(
    enabled = true,
    attractionRadius = 50f,
    snapSpeed = 0.3f,
    showIndicator = true
)
VoiceCursorAPI.updateSnapConfig(snapConfig)
```

### 5.3 Implementation Notes

The snap-to-element system will integrate with VoiceAccessibility's element scraping to identify clickable targets. Stay tuned for implementation in v3.1.0.

---

## 6. Gesture Management

### 6.1 GestureManager Architecture

**Location:** `core/GestureManager.kt`
**Version:** 1.0.0

Comprehensive gesture detection system supporting 10+ gesture types with configurable parameters.

```
Touch Events → GestureDetector → GestureManager → Callbacks
                      │
                      ├─ SimpleGestureListener
                      │  ├─ onDown()
                      │  ├─ onSingleTapUp()
                      │  ├─ onLongPress()
                      │  ├─ onScroll()
                      │  └─ onFling()
                      │
                      └─ ScaleGestureDetector
                         ├─ onScaleBegin()
                         ├─ onScale()
                         └─ onScaleEnd()
```

### 6.2 Supported Gestures

```kotlin
enum class GestureType {
    SWIPE_LEFT,      // Horizontal swipe left
    SWIPE_RIGHT,     // Horizontal swipe right
    SWIPE_UP,        // Vertical swipe up
    SWIPE_DOWN,      // Vertical swipe down
    PINCH_IN,        // Pinch to zoom out
    PINCH_OUT,       // Pinch to zoom in
    DRAG_START,      // Drag operation begins
    DRAG_MOVE,       // Drag in progress
    DRAG_END,        // Drag operation ends
    TAP,             // Single tap
    LONG_PRESS,      // Long press (>500ms)
    DOUBLE_TAP,      // Two quick taps
    UNKNOWN          // Unrecognized gesture
}
```

### 6.3 Gesture Events

```kotlin
data class GestureEvent(
    val type: GestureType,
    val startPosition: CursorOffset,
    val currentPosition: CursorOffset,
    val endPosition: CursorOffset = currentPosition,
    val velocity: Float = 0f,         // For swipes
    val scaleFactor: Float = 1f,      // For pinch
    val distance: Float = 0f,         // For drags
    val duration: Long = 0L,          // Gesture duration
    val timestamp: Long = System.currentTimeMillis()
)
```

### 6.4 Configuration

```kotlin
data class GestureConfig(
    val swipeMinDistance: Float = 100f,      // Min pixels for swipe
    val swipeMinVelocity: Float = 300f,      // Min velocity (px/s)
    val pinchMinSpan: Float = 50f,           // Min pinch span
    val dragMinDistance: Float = 10f,        // Min drag distance
    val longPressTimeout: Long = 500L,       // Long press duration
    val doubleTapTimeout: Long = 300L,       // Double tap window
    val enableSwipeGestures: Boolean = true,
    val enablePinchGestures: Boolean = true,
    val enableDragGestures: Boolean = true,
    val enableTapGestures: Boolean = true
)
```

### 6.5 Usage Example

```kotlin
// Initialize gesture manager
val gestureManager = GestureManager(context)

// Configure gestures
gestureManager.config = GestureConfig(
    swipeMinDistance = 80f,
    swipeMinVelocity = 250f,
    enablePinchGestures = true
)

// Set event listener
gestureManager.onGestureEvent = { event ->
    when (event.type) {
        GestureType.TAP -> handleTap(event.startPosition)
        GestureType.DOUBLE_TAP -> handleDoubleTap(event.startPosition)
        GestureType.LONG_PRESS -> handleLongPress(event.startPosition)
        GestureType.SWIPE_UP -> handleSwipeUp(event.velocity)
        GestureType.DRAG_START -> startDrag(event.startPosition)
        GestureType.DRAG_MOVE -> updateDrag(event.currentPosition)
        GestureType.DRAG_END -> endDrag(event.endPosition)
        GestureType.PINCH_OUT -> handleZoomIn(event.scaleFactor)
        else -> { }
    }
}

// Set state listener
gestureManager.onGestureStateChanged = { state ->
    if (state.isDragging) {
        showDragFeedback()
    }
}

// Process touch events
override fun onTouchEvent(event: MotionEvent): Boolean {
    return gestureManager.onTouchEvent(event) || super.onTouchEvent(event)
}
```

### 6.6 Gesture State

```kotlin
data class GestureState(
    val isActive: Boolean = false,
    val currentGesture: GestureType = GestureType.UNKNOWN,
    val startTime: Long = 0L,
    val startPosition: CursorOffset = CursorOffset(0f, 0f),
    val currentPosition: CursorOffset = CursorOffset(0f, 0f),
    val isDragging: Boolean = false,
    val isScaling: Boolean = false,
    val currentScale: Float = 1f,
    val accumulatedDistance: Float = 0f
)

// Query current state
val state = gestureManager.getCurrentState()
if (state.isDragging) {
    val distance = state.accumulatedDistance
    updateDragVisuals(distance)
}
```

---

## 7. Gaze Click Support

### 7.1 GazeClickManager Architecture

**Location:** `core/GazeClickManager.kt`
**Version:** 2.1.0

Provides dwell-time based gaze activation for AR/XR devices where hands-free clicking is essential.

```
Cursor Position → Stability Check → Dwell Timer → Auto-Click
       │               │                 │
       ▼               ▼                 ▼
   Track Last     Check Distance    Count Elapsed
   Position       from Center       Time at Center
```

### 7.2 Gaze Click Mechanism

**Principle:** When cursor remains stable within a small radius for a specified duration, trigger automatic click.

**Parameters:**
- **Auto-click time:** 1500ms (configurable)
- **Cancel distance:** 50px (cursor must stay within)
- **Lock cancel distance:** 420px (larger radius when locked)
- **Center tolerance:** 6px (stability threshold)

### 7.3 Configuration

```kotlin
data class GazeConfig(
    val autoClickTimeMs: Long = 1500L,        // Dwell time for click
    val cancelDistance: Double = 50.0,        // Movement threshold
    val lockCancelDistance: Double = 420.0,   // Threshold when locked
    val centerDistanceTolerance: Double = 6.0,// Stability radius
    val timeTolerance: Long = 200_000_000L    // Time tolerance (ns)
)
```

### 7.4 Core Methods

#### checkGazeClick()

Main gaze detection logic called on each cursor update.

```kotlin
fun checkGazeClick(
    currentX: Float,
    currentY: Float,
    timestamp: Long,
    isOverlayShown: Boolean
): GazeResult
```

**Algorithm:**

1. **Distance Check** - Reset if moved too far
2. **Timer Check** - Trigger click if dwell time reached
3. **Gaze Tracking** - Update gaze center if steady

**Result:**
```kotlin
data class GazeResult(
    val shouldClick: Boolean,  // True if dwell time reached
    val isTracking: Boolean    // True if gaze is active
)
```

### 7.5 Integration Example

```kotlin
class GazeClickController(
    private val gazeManager: GazeClickManager,
    private val gazeView: GazeClickView
) {
    private var gazeStartTime = 0L

    fun onCursorUpdate(x: Float, y: Float, timestamp: Long) {
        val result = gazeManager.checkGazeClick(
            currentX = x,
            currentY = y,
            timestamp = timestamp,
            isOverlayShown = true
        )

        if (result.isTracking) {
            // Update visual feedback
            if (gazeStartTime == 0L) {
                gazeStartTime = System.currentTimeMillis()
                gazeView.showGazeIndicator(x, y, 0f)
            }

            // Calculate progress
            val elapsed = System.currentTimeMillis() - gazeStartTime
            val progress = (elapsed / gazeConfig.autoClickTimeMs)
                .coerceIn(0f, 1f)

            gazeView.updateProgress(progress)
            gazeView.setPosition(x, y)

            // Check for click
            if (result.shouldClick) {
                performClick(x, y)
                gazeView.hideGazeIndicator()
                gazeStartTime = 0L
            }
        } else {
            // Reset visual feedback
            if (gazeStartTime != 0L) {
                gazeView.hideGazeIndicator()
                gazeStartTime = 0L
            }
        }
    }

    private fun performClick(x: Float, y: Float) {
        VoiceCursorAPI.executeAction(
            CursorAction.SINGLE_CLICK,
            CursorOffset(x, y)
        )
    }
}
```

---

## 8. IMU Integration

### 8.1 VoiceCursorIMUIntegration Architecture

**Location:** `helper/VoiceCursorIMUIntegration.kt`
**Version:** 1.0.0

Integration layer connecting VoiceCursor with DeviceManager's centralized IMU system.

```
VoiceCursor ──▶ IMUIntegration ──▶ DeviceManager
                     │                    │
                     │                    ├─ IMUManager
                     │                    ├─ CursorAdapter
                     │                    └─ SensorFusion
                     │
                     └─ Position Callbacks ──▶ CursorView
```

### 8.2 DeviceManager Integration

VoiceCursor delegates all IMU handling to DeviceManager's `CursorAdapter`, which provides:

- **Sensor fusion** (accelerometer + gyroscope + magnetometer)
- **Calibration** with user-guided routines
- **Adaptive filtering** for motion smoothness
- **Physics-based tracking** with inertia and damping
- **Multi-consumer support** via consumer IDs

### 8.3 API Overview

```kotlin
class VoiceCursorIMUIntegration private constructor(
    private val context: Context
) {
    companion object {
        fun createModern(context: Context): VoiceCursorIMUIntegration
        fun createLegacyCompatible(context: Context): VoiceCursorIMUIntegration
    }

    // Lifecycle
    fun start()
    fun stop()
    fun dispose()

    // Configuration
    fun setSensitivity(sensitivity: Float)
    fun updateScreenDimensions(width: Int, height: Int)
    fun centerCursor()

    // Callbacks
    fun setOnPositionUpdate(callback: (CursorOffset) -> Unit)
    fun setOnOrientationUpdate(callback: (Float, Float, Float, Long) -> Unit)

    // Calibration
    suspend fun calibrate(): Boolean
    fun getSensorInfo(): String
}
```

### 8.4 Modern Position-Based Tracking

**Recommended approach** for new implementations.

```kotlin
// Create IMU integration
val imuIntegration = VoiceCursorIMUIntegration.createModern(context)

// Configure sensitivity (0.1 to 3.0)
imuIntegration.setSensitivity(1.2f)

// Set screen dimensions
imuIntegration.updateScreenDimensions(
    width = displayMetrics.widthPixels,
    height = displayMetrics.heightPixels
)

// Set position callback
imuIntegration.setOnPositionUpdate { position ->
    // Update cursor UI
    updateCursorPosition(position.x, position.y)
}

// Start tracking
imuIntegration.start()
```

---

## 9. Boundary Detection

### 9.1 Edge Detection System

The VoiceCursor module includes intelligent boundary detection to prevent cursor from leaving screen and provide visual feedback at edges.

#### Edge Types

```kotlin
enum class EdgeType {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
```

### 9.2 Detection Algorithm

**Location:** `CursorPositionManager.detectEdges()`

**Constants:**
```kotlin
private const val EDGE_BUFFER = 20f        // Detection zone (px from edge)
private const val BOUNCE_FACTOR = 0.3f     // Bounce-back strength
private const val EDGE_RESISTANCE = 0.5f   // Movement damping at edges
```

---

## 10. Performance Optimization

### 10.1 Rendering Optimizations

#### Frame Rate Limiting

```kotlin
companion object {
    private const val FRAME_RATE_LIMIT_MS = 16  // ~60fps max
}

private var lastInvalidateTime = 0L

fun shouldRedraw(): Boolean {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastInvalidateTime >= FRAME_RATE_LIMIT_MS) {
        lastInvalidateTime = currentTime
        return true
    }
    return false
}
```

---

## 11. Voice Command Integration

### 11.1 Architecture

**Note:** Voice command handling has been migrated from VoiceCursor to CommandManager module (Oct 2025).

### 11.2 Supported Commands

#### Movement Commands
- `cursor up [distance]`
- `cursor down [distance]`
- `cursor left [distance]`
- `cursor right [distance]`

#### Action Commands
- `cursor click`
- `cursor double click`
- `cursor long press`
- `click` (standalone)

#### System Commands
- `cursor center`
- `cursor show`
- `cursor hide`
- `cursor settings`

---

## 12. Best Practices

### 12.1 Initialization

**Always initialize in correct order:**

```kotlin
class CursorSetup(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) {
    fun initialize() {
        // 1. Initialize VoiceCursorAPI
        val success = VoiceCursorAPI.initialize(context, accessibilityService)
        if (!success) {
            Log.e(TAG, "Failed to initialize VoiceCursorAPI")
            return
        }

        // 2. Configure cursor
        val config = CursorConfig(
            type = CursorType.Normal,
            size = 48,
            speed = 8,
            jitterFilterEnabled = true
        )

        // 3. Show cursor
        VoiceCursorAPI.showCursor(config)
    }
}
```

### 12.2 Lifecycle Management

**Properly manage resources:**

```kotlin
class VoiceCursorFragment : Fragment() {
    private lateinit var imuIntegration: VoiceCursorIMUIntegration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imuIntegration = VoiceCursorIMUIntegration.createModern(requireContext())
    }

    override fun onResume() {
        super.onResume()
        imuIntegration.start()
        VoiceCursorAPI.showCursor()
    }

    override fun onPause() {
        super.onPause()
        imuIntegration.stop()
        VoiceCursorAPI.hideCursor()
    }

    override fun onDestroy() {
        super.onDestroy()
        imuIntegration.dispose()
        VoiceCursorAPI.dispose()
    }
}
```

---

## Appendix A: Complete API Reference

### VoiceCursorAPI

```kotlin
object VoiceCursorAPI {
    // Initialization
    fun initialize(context: Context, accessibilityService: AccessibilityService): Boolean
    fun dispose()

    // Visibility
    fun showCursor(config: CursorConfig = CursorConfig()): Boolean
    fun hideCursor(): Boolean
    fun toggleCursor(): Boolean
    fun isVisible(): Boolean

    // Position
    fun centerCursor(): Boolean
    fun moveTo(position: CursorOffset, animate: Boolean = true): Boolean
    fun getCurrentPosition(): CursorOffset?

    // Actions
    fun executeAction(action: CursorAction, position: CursorOffset? = null): Boolean
    fun click(): Boolean
    fun doubleClick(): Boolean
    fun longPress(): Boolean

    // Configuration
    fun updateConfiguration(config: CursorConfig): Boolean

    // State
    fun isInitialized(): Boolean
}
```

---

## Appendix B: Version History

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2025-10-10 | Migrated command handling to CommandManager |
| 2.1.0 | 2025-09-26 | Added VoiceCursorAPI public interface |
| 2.0.0 | 2025-01-27 | Complete rewrite with DeviceManager integration |
| 1.2.0 | 2025-01-26 | Added gaze click support |
| 1.1.0 | 2025-01-23 | Improved thread safety and filtering |
| 1.0.0 | 2025-01-23 | Initial release |

---

**End of Chapter 6: VoiceCursor Module**

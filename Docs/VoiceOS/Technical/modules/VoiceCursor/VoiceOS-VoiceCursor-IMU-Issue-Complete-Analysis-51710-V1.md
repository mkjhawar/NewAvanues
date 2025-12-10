# VoiceCursor Module: IMU Issue Complete Analysis

**Module:** VoiceCursor
**Date:** 2025-10-17 06:05 PDT
**Purpose:** Comprehensive analysis of cursor movement failure (IMU not responding)
**Issue:** Issue #3 - Cursor Does Not Move (Priority 3)
**Status:** Analysis Complete, Fix Plan Ready

---

## Table of Contents

1. [Module Overview](#module-overview)
2. [Problem Statement](#problem-statement)
3. [Root Cause Analysis](#root-cause-analysis)
4. [Architecture Analysis](#architecture-analysis)
5. [Fix Implementation Plan](#fix-implementation-plan)
6. [Testing Procedures](#testing-procedures)
7. [IMU System Deep Dive](#imu-system-deep-dive)

---

## Module Overview

### VoiceCursor Description

**Location:** `/Volumes/M Drive/Coding/vos4/modules/apps/VoiceCursor/`

**Purpose:** Overlay cursor controlled by device motion (IMU sensors) for accessibility navigation

**Key Components:**
- `CursorOverlayManager` - Manages overlay window and cursor lifecycle
- `CursorView` - Custom view rendering the cursor
- `VoiceCursorIMUIntegration` - IMU sensor integration
- `IMUSensorHandler` - Raw sensor data processing
- `CursorPositionCalculator` - Converts IMU data to screen coordinates

**Namespace:** `com.augmentalis.voiceos.cursor.*`

**Sensor Dependencies:**
- Accelerometer - Detects device orientation changes
- Gyroscope - Detects rotation rate
- Magnetometer - Provides absolute orientation reference

---

## Problem Statement

### User Report

**Symptom:** "The cursor does not move, so its not responding to the imu"

**Expected Behavior:**
1. User tilts device
2. IMU sensors detect motion
3. Cursor moves on screen accordingly
4. User can navigate to UI elements

**Actual Behavior:**
1. User tilts device
2. IMU sensors detect motion ✅
3. ❌ Cursor does NOT move
4. ❌ Cannot navigate

**Impact:**
- Core accessibility feature broken
- Users cannot control cursor
- Voice commands require cursor for element selection
- Accessibility service unusable for motion-based navigation

---

## Root Cause Analysis

### Investigation Process

#### Step 1: Verify IMU Sensors Working

**Test:** Check if IMU sensors are producing data

**File:** `IMUSensorHandler.kt`
**Location:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/sensors/IMUSensorHandler.kt`

**Added Logging:**
```kotlin
override fun onSensorChanged(event: SensorEvent) {
    when (event.sensor.type) {
        Sensor.TYPE_ACCELEROMETER -> {
            Log.v(TAG, "Accel: x=${event.values[0]}, y=${event.values[1]}, z=${event.values[2]}")
            // ... processing
        }
        Sensor.TYPE_GYROSCOPE -> {
            Log.v(TAG, "Gyro: x=${event.values[0]}, y=${event.values[1]}, z=${event.values[2]}")
            // ... processing
        }
    }
}
```

**Result:**
```
// Logcat when device tilted
V/IMUSensorHandler: Accel: x=0.245, y=9.672, z=1.023
V/IMUSensorHandler: Gyro: x=0.012, y=-0.003, z=0.087
V/IMUSensorHandler: Accel: x=0.289, y=9.651, z=1.056
V/IMUSensorHandler: Gyro: x=0.015, y=-0.002, z=0.091
```

**✅ Finding:** IMU sensors ARE working and producing data correctly.

---

#### Step 2: Verify Position Calculation

**Test:** Check if position calculation is working

**File:** `CursorPositionCalculator.kt`
**Location:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/position/CursorPositionCalculator.kt`

**Added Logging:**
```kotlin
fun calculateNewPosition(
    currentPosition: PointF,
    imuData: IMUData,
    deltaTimeMs: Long
): PointF {
    val newPosition = calculatePositionDelta(imuData, deltaTimeMs)

    Log.d(TAG, "Position update: from=$currentPosition to=$newPosition (delta=${deltaTimeMs}ms)")

    return newPosition
}
```

**Result:**
```
// Logcat when device tilted
D/CursorPositionCalculator: Position update: from=PointF(540.0, 960.0) to=PointF(545.3, 958.1) (delta=16ms)
D/CursorPositionCalculator: Position update: from=PointF(545.3, 958.1) to=PointF(550.8, 956.4) (delta=16ms)
D/CursorPositionCalculator: Position update: from=PointF(550.8, 956.4) to=PointF(556.2, 954.9) (delta=16ms)
```

**✅ Finding:** Position calculation IS working and producing new coordinates.

---

#### Step 3: Verify IMU Integration

**Test:** Check if VoiceCursorIMUIntegration is receiving and emitting position updates

**File:** `VoiceCursorIMUIntegration.kt`
**Location:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/integration/VoiceCursorIMUIntegration.kt`

**Added Logging:**
```kotlin
private fun processSensorData(imuData: IMUData) {
    val newPosition = positionCalculator.calculateNewPosition(
        currentPosition = currentPosition,
        imuData = imuData,
        deltaTimeMs = calculateDelta()
    )

    currentPosition = newPosition

    Log.d(TAG, "Emitting position: $newPosition")

    // Emit to callback
    positionUpdateCallback?.invoke(newPosition)
}
```

**Result:**
```
// Logcat when device tilted
D/VoiceCursorIMUIntegration: Emitting position: PointF(545.3, 958.1)
D/VoiceCursorIMUIntegration: Emitting position: PointF(550.8, 956.4)
D/VoiceCursorIMUIntegration: Emitting position: PointF(556.2, 954.9)
```

**✅ Finding:** IMU Integration IS emitting position updates correctly.

---

#### Step 4: Check CursorOverlayManager Callback

**Test:** Verify callback is receiving position updates

**File:** `CursorOverlayManager.kt`
**Location:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/manager/CursorOverlayManager.kt`

**Code Review:**
```kotlin
private fun initializeIMU() {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
        setOnPositionUpdate { position ->  // ← CALLBACK DEFINED
            cursorView?.let { view ->
                serviceScope.launch {
                    view.post {
                        // ❌ PROBLEM: Position is IGNORED!
                        // Position update handled internally by View
                        Log.d(TAG, "Position callback invoked: $position")
                    }
                }
            }
        }
        start()
    }
}
```

**Added Logging:**
```kotlin
setOnPositionUpdate { position ->
    Log.w(TAG, "*** CALLBACK INVOKED: position=$position ***")
    cursorView?.let { view ->
        serviceScope.launch {
            view.post {
                Log.w(TAG, "*** INSIDE POST BLOCK ***")
                // Position update handled internally by View
            }
        }
    }
}
```

**Result:**
```
// Logcat when device tilted
D/VoiceCursorIMUIntegration: Emitting position: PointF(545.3, 958.1)
W/CursorOverlayManager: *** CALLBACK INVOKED: position=PointF(545.3, 958.1) ***
W/CursorOverlayManager: *** INSIDE POST BLOCK ***
D/VoiceCursorIMUIntegration: Emitting position: PointF(550.8, 956.4)
W/CursorOverlayManager: *** CALLBACK INVOKED: position=PointF(550.8, 956.4) ***
W/CursorOverlayManager: *** INSIDE POST BLOCK ***
```

**❌ Finding:** Callback IS invoked, but position is NOT passed to CursorView!

**The Problem:**
```kotlin
view.post {
    // Position update handled internally by View
    // ← THIS IS A COMMENT, NOT CODE!
    // Nothing actually happens here!
}
```

---

#### Step 5: Check CursorView Update Method

**Test:** Does CursorView have a method to update cursor position?

**File:** `CursorView.kt`
**Location:** `modules/apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/view/CursorView.kt`

**Code Review:**
```kotlin
class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cursorX: Float = 0f
    private var cursorY: Float = 0f

    private var imuIntegration: VoiceCursorIMUIntegration? = null

    init {
        // ❌ PROBLEM #2: CursorView creates its OWN IMU instance!
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { position ->
                // Update internal position
                cursorX = position.x
                cursorY = position.y
                invalidate()  // Trigger redraw
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw cursor at current position
        canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)
    }

    // ❌ MISSING: No public method to update cursor position from external source!
    // Should have:
    // fun updateCursorPositionFromIMU(position: PointF) {
    //     cursorX = position.x
    //     cursorY = position.y
    //     invalidate()
    // }
}
```

**❌ Finding:** CursorView creates its own IMU instance and has no method to receive position from CursorOverlayManager!

---

### Root Cause Summary

**Two Critical Problems:**

#### Problem 1: Broken Event Chain in CursorOverlayManager

**File:** `CursorOverlayManager.kt` (Lines 145-158)

```kotlin
private fun initializeIMU() {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
        setOnPositionUpdate { position ->  // ← Callback receives position
            cursorView?.let { view ->
                serviceScope.launch {
                    view.post {
                        // ❌ DOES NOTHING!
                        // Comment says "Position update handled internally by View"
                        // But position is never passed to view!
                    }
                }
            }
        }
        start()
    }
}
```

**Impact:** Position updates from IMU are received but thrown away.

---

#### Problem 2: Dual IMU Instances

**CursorOverlayManager creates IMU instance:**
```kotlin
// CursorOverlayManager.kt
private fun initializeIMU() {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context)  // ← Instance #1
    // ...
}
```

**CursorView ALSO creates IMU instance:**
```kotlin
// CursorView.kt
init {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context)  // ← Instance #2
    // ...
}
```

**Result:** Two separate IMU instances running simultaneously:
- **Instance #1** (CursorOverlayManager) - Receives sensor data, but position thrown away
- **Instance #2** (CursorView) - NEVER STARTED (CursorView never calls `.start()`)

**Impact:** CursorView's IMU instance is inactive, so cursor never receives position updates.

---

### Visual Representation

**Current (BROKEN) Architecture:**

```
IMU Sensors (Hardware)
    ↓
IMUSensorHandler
    ↓
[Instance #1] VoiceCursorIMUIntegration (CursorOverlayManager)
    ↓
CursorPositionCalculator
    ↓
Position: PointF(545.3, 958.1)
    ↓
Callback to CursorOverlayManager
    ↓
view.post {
    // ❌ NOTHING HAPPENS
}
    ↓
❌ Position LOST

[Instance #2] VoiceCursorIMUIntegration (CursorView)
    ↓
❌ NEVER STARTED (no .start() call)
    ↓
❌ CursorView position NEVER UPDATED
    ↓
❌ Cursor DOES NOT MOVE
```

---

**Required (FIXED) Architecture:**

```
IMU Sensors (Hardware)
    ↓
IMUSensorHandler
    ↓
[Single Instance] VoiceCursorIMUIntegration
    ↓
CursorPositionCalculator
    ↓
Position: PointF(545.3, 958.1)
    ↓
Callback to CursorOverlayManager
    ↓
view.post {
    ✅ view.updateCursorPositionFromIMU(position)
}
    ↓
CursorView.cursorX = 545.3
CursorView.cursorY = 958.1
    ↓
invalidate() → onDraw()
    ↓
✅ Cursor MOVES on screen
```

---

## Architecture Analysis

### CursorOverlayManager Lifecycle

**File:** `CursorOverlayManager.kt`

**Initialization Flow:**
```
Constructor
    ↓
init {}
    ↓
createCursorView()
    ├── CursorView instantiated
    └── CursorView.init {} creates IMU Instance #2 ❌
    ↓
showCursor()
    ├── addView(cursorView)
    ├── initializeIMU()
    │   └── Creates IMU Instance #1 ❌
    └── imuIntegration.start()
```

**Problem:** Two IMU instances created, one never started, other throws away position data.

---

### CursorView Architecture

**File:** `CursorView.kt`

**Key Properties:**
```kotlin
class CursorView : View {
    // Cursor position (screen coordinates)
    private var cursorX: Float = 0f
    private var cursorY: Float = 0f

    // Visual properties
    private val cursorRadius: Float = 20f
    private val cursorPaint: Paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }

    // ❌ Own IMU instance (unused)
    private var imuIntegration: VoiceCursorIMUIntegration? = null
}
```

**Draw Method:**
```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    // Draw cursor circle at current position
    canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)

    // Draw crosshair
    canvas.drawLine(
        cursorX - 15f, cursorY,
        cursorX + 15f, cursorY,
        cursorPaint
    )
    canvas.drawLine(
        cursorX, cursorY - 15f,
        cursorX, cursorY + 15f,
        cursorPaint
    )
}
```

**Why Cursor Doesn't Move:**
- `onDraw()` draws at `(cursorX, cursorY)`
- `(cursorX, cursorY)` initialized to `(0, 0)`
- No method updates `(cursorX, cursorY)` from external source
- CursorView's internal IMU instance never started
- Result: Cursor stuck at `(0, 0)` (top-left corner)

---

### VoiceCursorIMUIntegration Architecture

**File:** `VoiceCursorIMUIntegration.kt`

**Key Methods:**
```kotlin
class VoiceCursorIMUIntegration private constructor(
    private val context: Context,
    private val config: IMUConfig
) {
    private var imuSensorHandler: IMUSensorHandler? = null
    private var positionCalculator: CursorPositionCalculator? = null
    private var positionUpdateCallback: ((PointF) -> Unit)? = null

    companion object {
        fun createModern(context: Context): VoiceCursorIMUIntegration {
            return VoiceCursorIMUIntegration(context, IMUConfig.DEFAULT)
        }
    }

    fun setOnPositionUpdate(callback: (PointF) -> Unit) {
        this.positionUpdateCallback = callback
    }

    fun start() {
        // Initialize sensor handler
        imuSensorHandler = IMUSensorHandler(context) { imuData ->
            processSensorData(imuData)
        }

        // Register sensors
        imuSensorHandler?.registerSensors()
    }

    fun stop() {
        imuSensorHandler?.unregisterSensors()
    }

    private fun processSensorData(imuData: IMUData) {
        val newPosition = positionCalculator?.calculateNewPosition(
            currentPosition = currentPosition,
            imuData = imuData,
            deltaTimeMs = calculateDelta()
        ) ?: return

        currentPosition = newPosition

        // Emit to callback
        positionUpdateCallback?.invoke(newPosition)
    }
}
```

**Key Points:**
- Must call `.start()` to begin sensor processing
- Must call `.setOnPositionUpdate()` to receive position updates
- Each instance is independent (separate sensor registrations)

---

### Data Flow Analysis

**Successful Data Flow (Instance #1):**
```
Device Tilted
    ↓
Accelerometer + Gyroscope Sensors
    ↓
IMUSensorHandler.onSensorChanged()
    ↓
IMUData(accelX, accelY, accelZ, gyroX, gyroY, gyroZ)
    ↓
VoiceCursorIMUIntegration.processSensorData()
    ↓
CursorPositionCalculator.calculateNewPosition()
    ↓
PointF(newX, newY)
    ↓
positionUpdateCallback?.invoke(position)
    ↓
CursorOverlayManager callback
    ↓
❌ Position IGNORED
```

**Failed Data Flow (Instance #2):**
```
CursorView.imuIntegration created
    ↓
❌ .start() NEVER CALLED
    ↓
❌ Sensors NEVER REGISTERED
    ↓
❌ No sensor data
    ↓
❌ Cursor position never updated
```

---

## Fix Implementation Plan

### Effort Estimate

**Total Time:** 2-3 hours
**Priority:** 3 (After UUID and Voice fixes)
**Files Modified:** 2 files
**New Files:** 0 files

---

### Step 1: Remove Dual IMU Instances

**File:** `CursorView.kt`
**Changes:** Remove internal IMU instance creation

**Current Code:**
```kotlin
class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var imuIntegration: VoiceCursorIMUIntegration? = null

    init {
        // ❌ REMOVE THIS: Don't create own IMU instance
        imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
            setOnPositionUpdate { position ->
                cursorX = position.x
                cursorY = position.y
                invalidate()
            }
        }
    }
}
```

**Fixed Code:**
```kotlin
class CursorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ✅ REMOVED: No internal IMU instance
    // IMU will be managed externally by CursorOverlayManager

    private var cursorX: Float = 0f
    private var cursorY: Float = 0f

    private val cursorRadius: Float = 20f
    private val cursorPaint: Paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }

    // ✅ NEW: Public method to update cursor position
    fun updateCursorPositionFromIMU(position: PointF) {
        cursorX = position.x
        cursorY = position.y
        invalidate()  // Trigger redraw
    }

    // ✅ NEW: Allow external IMU integration setup
    fun setIMUIntegration(integration: VoiceCursorIMUIntegration) {
        // IMU integration managed externally
        // This method exists for future extensibility
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw cursor at current position
        canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)

        // Draw crosshair
        canvas.drawLine(
            cursorX - 15f, cursorY,
            cursorX + 15f, cursorY,
            cursorPaint
        )
        canvas.drawLine(
            cursorX, cursorY - 15f,
            cursorX, cursorY + 15f,
            cursorPaint
        )
    }
}
```

**Time:** 15 minutes

---

### Step 2: Fix CursorOverlayManager Callback

**File:** `CursorOverlayManager.kt`
**Changes:** Pass position to CursorView in callback

**Current Code:**
```kotlin
private fun initializeIMU() {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
        setOnPositionUpdate { position ->  // ← Position received
            cursorView?.let { view ->
                serviceScope.launch {
                    view.post {
                        // ❌ BROKEN: Position ignored!
                        // Position update handled internally by View
                    }
                }
            }
        }
        start()
    }
}
```

**Fixed Code:**
```kotlin
private fun initializeIMU() {
    imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
        setOnPositionUpdate { position ->
            // ✅ FIXED: Pass position to CursorView
            cursorView?.let { view ->
                view.post {
                    view.updateCursorPositionFromIMU(position)
                }
            }
        }
        start()
    }

    // ✅ NEW: Pass IMU integration to view
    cursorView?.setIMUIntegration(imuIntegration!!)
}
```

**Time:** 10 minutes

---

### Step 3: Add Logging for Debugging

**File:** `CursorOverlayManager.kt`
**Changes:** Add comprehensive logging

**Add Logging:**
```kotlin
private fun initializeIMU() {
    Log.d(TAG, "Initializing IMU integration")

    imuIntegration = VoiceCursorIMUIntegration.createModern(context).apply {
        setOnPositionUpdate { position ->
            Log.v(TAG, "Position update: (${"%.1f".format(position.x)}, ${"%.1f".format(position.y)})")

            cursorView?.let { view ->
                view.post {
                    view.updateCursorPositionFromIMU(position)
                }
            } ?: run {
                Log.w(TAG, "CursorView is null, cannot update position")
            }
        }
        start()
    }

    cursorView?.setIMUIntegration(imuIntegration!!)

    Log.d(TAG, "IMU integration started successfully")
}
```

**File:** `CursorView.kt`
**Add Logging:**
```kotlin
fun updateCursorPositionFromIMU(position: PointF) {
    Log.v(TAG, "Updating cursor: ($cursorX, $cursorY) → (${position.x}, ${position.y})")

    cursorX = position.x
    cursorY = position.y
    invalidate()
}
```

**Time:** 15 minutes

---

### Step 4: Add Bounds Checking

**File:** `CursorView.kt`
**Changes:** Ensure cursor stays within screen bounds

**Enhanced Update Method:**
```kotlin
fun updateCursorPositionFromIMU(position: PointF) {
    // Get screen dimensions
    val screenWidth = width.toFloat()
    val screenHeight = height.toFloat()

    // ✅ Clamp position to screen bounds
    cursorX = position.x.coerceIn(cursorRadius, screenWidth - cursorRadius)
    cursorY = position.y.coerceIn(cursorRadius, screenHeight - cursorRadius)

    Log.v(TAG, "Cursor position: (${cursorX.toInt()}, ${cursorY.toInt()})")

    invalidate()
}
```

**Time:** 15 minutes

---

### Step 5: Add IMU State Validation

**File:** `CursorOverlayManager.kt`
**Changes:** Validate IMU is working correctly

**Add Validation:**
```kotlin
private fun validateIMU() {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Check accelerometer
    val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    if (accel == null) {
        Log.e(TAG, "❌ Accelerometer not available")
        showIMUError("Accelerometer not found")
        return
    }

    // Check gyroscope
    val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    if (gyro == null) {
        Log.e(TAG, "❌ Gyroscope not available")
        showIMUError("Gyroscope not found")
        return
    }

    Log.d(TAG, "✅ IMU sensors validated:")
    Log.d(TAG, "  - Accelerometer: ${accel.name}")
    Log.d(TAG, "  - Gyroscope: ${gyro.name}")
}

private fun showIMUError(message: String) {
    // Show toast or notification
    Toast.makeText(context, "IMU Error: $message", Toast.LENGTH_LONG).show()
}
```

**Call in showCursor():**
```kotlin
fun showCursor() {
    if (cursorView == null) {
        createCursorView()
    }

    // ✅ Validate IMU before starting
    validateIMU()

    windowManager.addView(cursorView, layoutParams)
    isShowing = true

    initializeIMU()

    Log.d(TAG, "Cursor overlay shown")
}
```

**Time:** 20 minutes

---

### Step 6: Testing

**Test Cases:**

**Test 1: Cursor Movement**
```kotlin
@Test
fun `cursor moves when device tilted`() {
    // Given: Cursor shown
    cursorOverlayManager.showCursor()
    delay(100)

    // Record initial position
    val initialPosition = cursorView.getCursorPosition()

    // When: Simulate device tilt
    val imuData = IMUData(
        accelX = 1.0f,
        accelY = 9.8f,
        accelZ = 0.0f,
        gyroX = 0.1f,
        gyroY = 0.0f,
        gyroZ = 0.0f
    )
    imuIntegration.simulateIMUData(imuData)
    delay(100)

    // Then: Cursor moved
    val finalPosition = cursorView.getCursorPosition()
    assertThat(finalPosition).isNotEqualTo(initialPosition)
}
```

**Test 2: Single IMU Instance**
```kotlin
@Test
fun `only one IMU instance exists`() {
    // Given: Cursor shown
    cursorOverlayManager.showCursor()

    // Then: Only CursorOverlayManager has IMU instance
    assertThat(cursorOverlayManager.imuIntegration).isNotNull()
    assertThat(cursorView.imuIntegration).isNull()  // Should be null
}
```

**Test 3: Bounds Checking**
```kotlin
@Test
fun `cursor stays within screen bounds`() {
    // Given: Cursor at edge
    cursorView.updateCursorPositionFromIMU(PointF(-100f, -100f))

    // Then: Cursor clamped to screen
    val position = cursorView.getCursorPosition()
    assertThat(position.x).isGreaterThan(0f)
    assertThat(position.y).isGreaterThan(0f)
}
```

**Time:** 1 hour

---

**Total Time for Fix: 2.5 hours**

---

## Testing Procedures

### Manual Testing

#### Test 1: Basic Cursor Movement

**Setup:**
1. Install debug build
2. Enable VoiceCursor accessibility service
3. Open any app

**Steps:**
1. Enable cursor overlay (via voice command or button)
2. Hold device flat (parallel to ground)
3. Slowly tilt device forward (away from you)
4. Observe cursor movement

**Expected Result:**
- Cursor visible on screen
- Cursor moves down when device tilted forward
- Cursor moves up when device tilted backward
- Cursor moves left/right when device tilted left/right
- Cursor stays within screen bounds

**Pass Criteria:**
- Cursor responds to device motion within 100ms
- Cursor movement is smooth (60 FPS)
- Cursor does not flicker or disappear

---

#### Test 2: Edge Cases

**Test 2a: Rapid Movement**
1. Tilt device rapidly in multiple directions
2. Verify cursor follows motion without lag
3. Verify no crashes or freezing

**Test 2b: Screen Rotation**
1. Enable cursor
2. Rotate device 90° (portrait → landscape)
3. Verify cursor remains visible
4. Verify cursor still responds to motion

**Test 2c: App Switching**
1. Enable cursor in App A
2. Switch to App B (home button → open App B)
3. Verify cursor still visible
4. Verify cursor still responds to motion

---

### Integration Testing

#### Test 3: IMU Sensor Validation

**Check Available Sensors:**
```bash
adb shell "dumpsys sensorservice"
```

**Expected Output:**
```
Active sensors:
  0x00000001) Accelerometer (LIS3DH Accelerometer)
  0x00000004) Gyroscope (BMI160 Gyroscope)
  0x00000002) Magnetometer (AK09918 Magnetometer)
```

**Pass Criteria:** All three sensors available and active

---

#### Test 4: Position Update Frequency

**Add Logging:**
```kotlin
private var lastUpdateTime = 0L
private var updateCount = 0

fun updateCursorPositionFromIMU(position: PointF) {
    val now = System.currentTimeMillis()
    updateCount++

    if (now - lastUpdateTime >= 1000) {
        Log.d(TAG, "Position updates per second: $updateCount")
        updateCount = 0
        lastUpdateTime = now
    }

    // ... update cursor
}
```

**Expected Result:**
```
D/CursorView: Position updates per second: 58
D/CursorView: Position updates per second: 61
D/CursorView: Position updates per second: 59
```

**Pass Criteria:** 50-60 updates per second (close to 60 FPS)

---

### Performance Testing

#### Test 5: CPU and Battery Usage

**Measure CPU Usage:**
```bash
# Start cursor
adb shell am start -n com.augmentalis.voicecursor/.MainActivity

# Monitor CPU
adb shell top -d 1 | grep voicecursor
```

**Expected CPU Usage:**
- Idle (cursor shown, no movement): 1-2%
- Active (device tilting): 3-5%
- Peak (rapid movement): 5-8%

**Pass Criteria:** CPU usage < 10% during active movement

---

**Measure Battery Drain:**
```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use cursor for 10 minutes
# ... (manual testing)

# Check battery drain
adb shell dumpsys batterystats | grep voicecursor
```

**Expected Battery Drain:**
- 10 minutes of active cursor use: < 2% battery

**Pass Criteria:** Battery drain < 3% per 10 minutes

---

#### Test 6: Memory Usage

**Monitor Memory:**
```bash
adb shell dumpsys meminfo com.augmentalis.voicecursor
```

**Expected Memory Usage:**
```
Native Heap:     18432 kB
Dalvik Heap:     12345 kB
Total PSS:       42123 kB
```

**Pass Criteria:**
- Total PSS < 50 MB
- No memory leaks (stable over time)

---

### Regression Testing

#### Test 7: Existing Functionality

**Verify these features still work:**

1. ✅ Cursor show/hide (voice command)
2. ✅ Cursor click (tap screen)
3. ✅ Cursor customization (size, color)
4. ✅ Cursor auto-hide (after inactivity)
5. ✅ Multiple app compatibility
6. ✅ Screen recording (cursor visible in recordings)

**Pass Criteria:** All existing features work as before fix

---

## IMU System Deep Dive

### Sensor Types and Usage

#### Accelerometer

**Purpose:** Measures linear acceleration in 3 axes

**Data Format:**
```kotlin
data class AccelData(
    val x: Float,  // Left/right acceleration (m/s²)
    val y: Float,  // Forward/backward acceleration (m/s²)
    val z: Float   // Up/down acceleration (m/s²)
)
```

**Example Values:**
- Device flat on table: `AccelData(0.0, 0.0, 9.8)` (gravity)
- Device tilted forward 30°: `AccelData(0.0, 4.9, 8.5)`
- Device tilted left 30°: `AccelData(4.9, 0.0, 8.5)`

**Usage in VoiceCursor:**
- Calculate device tilt angle
- Determine cursor movement direction
- Normalize for gravity

---

#### Gyroscope

**Purpose:** Measures rotation rate in 3 axes

**Data Format:**
```kotlin
data class GyroData(
    val x: Float,  // Pitch rate (rad/s)
    val y: Float,  // Roll rate (rad/s)
    val z: Float   // Yaw rate (rad/s)
)
```

**Example Values:**
- Device stationary: `GyroData(0.0, 0.0, 0.0)`
- Device rotating clockwise: `GyroData(0.0, 0.0, 1.5)`
- Device tilting forward: `GyroData(1.2, 0.0, 0.0)`

**Usage in VoiceCursor:**
- Detect rotation gestures
- Smooth cursor movement
- Calculate angular velocity

---

#### Magnetometer (Optional)

**Purpose:** Measures magnetic field strength (provides absolute orientation)

**Usage in VoiceCursor:**
- Calibrate accelerometer/gyroscope drift
- Provide absolute orientation reference
- Improve accuracy over time

---

### Position Calculation Algorithm

**File:** `CursorPositionCalculator.kt`

**Algorithm Overview:**

```kotlin
fun calculateNewPosition(
    currentPosition: PointF,
    imuData: IMUData,
    deltaTimeMs: Long
): PointF {
    // 1. Extract tilt angles from accelerometer
    val tiltX = atan2(imuData.accelX, imuData.accelZ)
    val tiltY = atan2(imuData.accelY, imuData.accelZ)

    // 2. Apply sensitivity multiplier
    val sensitivityX = config.sensitivityX  // Default: 10.0
    val sensitivityY = config.sensitivityY  // Default: 10.0

    // 3. Calculate velocity from tilt
    val velocityX = tiltX * sensitivityX
    val velocityY = tiltY * sensitivityY

    // 4. Integrate gyroscope for smoother movement
    val gyroContribution = config.gyroWeight  // Default: 0.3
    val adjustedVelocityX = velocityX + (imuData.gyroY * gyroContribution)
    val adjustedVelocityY = velocityY + (imuData.gyroX * gyroContribution)

    // 5. Calculate displacement
    val deltaTimeSec = deltaTimeMs / 1000.0f
    val deltaX = adjustedVelocityX * deltaTimeSec
    val deltaY = adjustedVelocityY * deltaTimeSec

    // 6. Apply smoothing (exponential moving average)
    val smoothing = config.smoothingFactor  // Default: 0.7
    val smoothedDeltaX = (deltaX * (1 - smoothing)) + (lastDeltaX * smoothing)
    val smoothedDeltaY = (deltaY * (1 - smoothing)) + (lastDeltaY * smoothing)

    lastDeltaX = smoothedDeltaX
    lastDeltaY = smoothedDeltaY

    // 7. Calculate new position
    val newX = currentPosition.x + smoothedDeltaX
    val newY = currentPosition.y + smoothedDeltaY

    return PointF(newX, newY)
}
```

**Key Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `sensitivityX` | 10.0 | Horizontal cursor speed multiplier |
| `sensitivityY` | 10.0 | Vertical cursor speed multiplier |
| `gyroWeight` | 0.3 | Gyroscope contribution (0.0 = none, 1.0 = full) |
| `smoothingFactor` | 0.7 | Smoothing (0.0 = none, 1.0 = max) |

---

### IMU Configuration

**File:** `IMUConfig.kt`

**Configuration Options:**

```kotlin
data class IMUConfig(
    val sensitivityX: Float = 10.0f,
    val sensitivityY: Float = 10.0f,
    val gyroWeight: Float = 0.3f,
    val smoothingFactor: Float = 0.7f,
    val updateRateHz: Int = 60,
    val deadzone: Float = 0.05f  // Ignore tilt < 0.05 rad
) {
    companion object {
        val DEFAULT = IMUConfig()

        val HIGH_SENSITIVITY = IMUConfig(
            sensitivityX = 20.0f,
            sensitivityY = 20.0f
        )

        val LOW_SENSITIVITY = IMUConfig(
            sensitivityX = 5.0f,
            sensitivityY = 5.0f
        )

        val SMOOTH = IMUConfig(
            smoothingFactor = 0.9f  // More smoothing
        )

        val RESPONSIVE = IMUConfig(
            smoothingFactor = 0.3f,  // Less smoothing
            gyroWeight = 0.5f        // More gyro influence
        )
    }
}
```

---

### Sensor Fusion

**Combining Accelerometer + Gyroscope:**

**Why Sensor Fusion?**
- **Accelerometer alone:** Noisy, affected by device movement
- **Gyroscope alone:** Drifts over time, no absolute reference
- **Combined:** Smooth, accurate, no drift

**Fusion Algorithm:**
```
1. Use accelerometer for absolute tilt angle
2. Use gyroscope for smooth movement
3. Weight: 70% accel, 30% gyro (configurable)
4. Apply complementary filter for stability
```

---

## Summary

### Root Cause

**Dual IMU instances + broken event chain:**

1. **CursorOverlayManager** creates IMU instance, starts it, receives position updates
2. Position callback **ignores** the position (does nothing)
3. **CursorView** creates second IMU instance, **never starts it**
4. CursorView draws cursor at static position `(0, 0)`
5. Result: Cursor never moves

---

### Fix Summary

**Changes Required:**

1. ✅ Remove IMU instance from CursorView
2. ✅ Add `updateCursorPositionFromIMU()` method to CursorView
3. ✅ Fix CursorOverlayManager callback to call `updateCursorPositionFromIMU()`
4. ✅ Add bounds checking
5. ✅ Add logging for debugging
6. ✅ Add IMU sensor validation

**Time:** 2-3 hours

**Impact:** Cursor will respond to device motion, core accessibility feature restored

---

**Generated:** 2025-10-17 06:05 PDT
**Status:** Analysis Complete
**Next Steps:** Implement fix after Issue #1 (UUID) and Issue #2 (Voice) are resolved

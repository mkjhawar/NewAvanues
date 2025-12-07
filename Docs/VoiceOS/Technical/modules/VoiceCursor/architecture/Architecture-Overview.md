# VoiceCursor Architecture Overview
**Last Updated:** 2025-01-28  
**Version:** 2.1.0

## System Architecture

### High-Level Design
VoiceCursor implements a multi-layered architecture for head-tracking cursor control:

```
┌─────────────────────────────────────────────────┐
│                User Interface Layer              │
│  ┌──────────────────────────────────────────┐   │
│  │         CursorView (Rendering)           │   │
│  │  • Glass morphism effects                │   │
│  │  • Animation system                      │   │
│  │  • Gesture recognition                   │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│              Processing Layer                    │
│  ┌──────────────────────────────────────────┐   │
│  │      CursorAdapter (Transformation)      │   │
│  │  • Delta-based processing                │   │
│  │  • Tangent scaling                       │   │
│  │  • Coordinate mapping                    │   │
│  └──────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────┐   │
│  │    CursorFilter (Signal Processing)      │   │
│  │  • Jitter reduction                      │   │
│  │  • Motion smoothing                      │   │
│  │  • Dead zone filtering                   │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────┐
│                 Sensor Layer                     │
│  ┌──────────────────────────────────────────┐   │
│  │        IMUManager (Data Collection)      │   │
│  │  • Accelerometer sampling                │   │
│  │  • Gyroscope sampling                    │   │
│  │  • Magnetometer fusion                   │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

## Core Components

### 1. CursorView (UI Layer)
**Responsibility**: Visual rendering and user interaction
- Renders cursor with ARVision theme
- Handles touch gestures
- Manages animations (pulse, glow, transitions)
- Coordinates with accessibility service

### 2. CursorAdapter (Processing Layer)
**Responsibility**: Transform sensor data to screen coordinates
- **Fixed Algorithm** (2025-01-28):
  ```kotlin
  // Delta-based processing
  deltaRotation = previousOrientation⁻¹ × currentOrientation
  deltaEuler = toEulerAngles(deltaRotation)
  
  // Tangent-based displacement
  deltaX = tan(deltaEuler.yaw) × screenWidth × sensitivityX
  deltaY = -tan(deltaEuler.pitch) × screenHeight × sensitivityY
  
  // Cumulative positioning
  position = clamp(position + delta, screenBounds)
  ```

### 3. IMUManager (Sensor Layer)
**Responsibility**: Collect and process raw sensor data
- Samples sensors at 120Hz
- Applies sensor fusion for stable orientation
- Broadcasts via Flow<OrientationData>

## Data Flow Architecture

### Sensor to Screen Pipeline
```
1. Raw Sensor Data (120Hz)
   ├── Accelerometer: [ax, ay, az]
   ├── Gyroscope: [ωx, ωy, ωz]
   └── Magnetometer: [mx, my, mz]
        ↓
2. Sensor Fusion (IMUManager)
   └── Quaternion: [w, x, y, z]
        ↓
3. Delta Processing (CursorAdapter)
   ├── Previous Frame: Qt-1
   ├── Current Frame: Qt
   └── Delta: ΔQ = Qt-1⁻¹ × Qt
        ↓
4. Euler Conversion
   └── [Δyaw, Δpitch, Δroll]
        ↓
5. Tangent Scaling
   ├── Δx = tan(Δyaw) × width × sx
   └── Δy = -tan(Δpitch) × height × sy
        ↓
6. Position Update
   └── P(t) = P(t-1) + [Δx, Δy]
        ↓
7. Screen Rendering (CursorView)
   └── Canvas.drawCircle(x, y, radius)
```

## State Management

### CursorState
```kotlin
data class CursorState(
    val position: CursorOffset,      // Current (x,y)
    val isVisible: Boolean,           // Visibility flag
    val isLocked: Boolean,            // Lock state
    val isGazeActive: Boolean,        // Gaze tracking
    val lockedPosition: CursorOffset  // Lock position
)
```

### Initialization Sequence
```kotlin
1. onCreate()
   ├── Initialize screen dimensions
   ├── Set cursor to center (width/2, height/2)
   └── Create IMU connection

2. onResume()
   ├── Start sensor sampling
   ├── Capture base orientation
   └── Begin position updates

3. Runtime Loop
   ├── Receive orientation data
   ├── Calculate delta from previous
   ├── Apply tangent transformation
   ├── Update position
   ├── Render at 60 FPS
   └── Store as previous orientation
```

## Key Algorithms

### 1. Delta-Based Orientation Processing
```kotlin
fun processOrientation(current: Quaternion) {
    if (previous == null) {
        previous = current
        base = current
        return
    }
    
    val delta = previous.inverse * current
    val euler = delta.toEulerAngles()
    
    // Apply dead zone
    val yaw = if (abs(euler.yaw) > 0.001f) euler.yaw else 0f
    val pitch = if (abs(euler.pitch) > 0.001f) euler.pitch else 0f
    
    // Calculate movement
    updatePosition(yaw, pitch)
    previous = current
}
```

### 2. Stuck Detection & Recovery
```kotlin
fun checkStuckCursor() {
    val timeSinceMove = now() - lastMovementTime
    
    if (timeSinceMove > 5000L) {
        // Auto-recalibrate
        centerCursor()
        baseOrientation = null
        previousOrientation = null
    }
}
```

### 3. Jitter Filtering
```kotlin
fun filterPosition(raw: CursorOffset): CursorOffset {
    val distance = distance(raw, filtered)
    
    return when {
        distance < deadZone -> filtered  // No movement
        distance < smoothZone -> {
            // Smooth small movements
            lerp(filtered, raw, 0.3f)
        }
        else -> raw  // Direct pass-through
    }
}
```

## Performance Characteristics

### Latency Budget (16ms frame)
```
Sensor Read:        1-2ms
Quaternion Math:    1-2ms
Delta Calculation:  1ms
Euler Conversion:   1ms
Tangent Scaling:    <1ms
Filter Processing:  1ms
Position Update:    <1ms
Render:            5-8ms
─────────────────────────
Total:             11-16ms ✓
```

### Memory Usage
- CursorAdapter: ~2KB state
- IMUManager: ~4KB buffers
- CursorView: ~10KB (including animations)
- Total Module: ~20KB runtime

## Error Recovery Mechanisms

### 1. Invalid Orientation Data
- Detect: Check quaternion normalization
- Recover: Skip frame, use previous valid

### 2. Stuck Cursor
- Detect: No movement for 5 seconds
- Recover: Auto-recalibrate to center

### 3. Screen Boundary Violations
- Detect: Position outside [0, width] × [0, height]
- Recover: Clamp to nearest valid position

### 4. Sensor Disconnection
- Detect: No data for 100ms
- Recover: Pause updates, show warning

## Testing Strategy

### Unit Tests (Implemented)
- Mathematical transformations ✅
- Quaternion operations ✅
- Tangent scaling accuracy ✅
- Boundary clamping ✅
- Dead zone filtering ✅

### Integration Tests (Planned)
- [ ] Full sensor-to-screen pipeline
- [ ] Multi-device compatibility
- [ ] Performance under load
- [ ] Recovery mechanisms

### UI Tests (Planned)
- [ ] Visual rendering accuracy
- [ ] Animation smoothness
- [ ] Gesture recognition
- [ ] Accessibility integration

## Security Considerations
- No network communication required
- No sensitive data storage
- Sensor permissions: BODY_SENSORS
- Accessibility permissions required for click dispatch

## Future Enhancements
1. **Machine Learning**: Predict movement patterns
2. **Multi-Device**: Support AR glasses natively
3. **3D Cursor**: Z-axis depth control
4. **Voice Integration**: "Move cursor to [target]"
5. **AI Assistance**: Smart target snapping

---
**Architecture Version:** 2.1.0  
**Last Review:** 2025-01-28  
**Approved By:** VOS4 Architecture Team
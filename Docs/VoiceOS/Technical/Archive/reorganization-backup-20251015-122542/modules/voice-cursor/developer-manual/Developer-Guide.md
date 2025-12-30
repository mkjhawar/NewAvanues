# VoiceCursor Developer Guide
**Last Updated:** 2025-01-28  
**Module Version:** 2.1.0

## Quick Start

### 1. Module Setup
```kotlin
// Add to your app's build.gradle.kts
dependencies {
    implementation(project(":modules:apps:VoiceCursor"))
    implementation(project(":modules:libraries:DeviceManager"))
}
```

### 2. Basic Integration
```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var cursorView: CursorView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create cursor view
        cursorView = CursorView(this).apply {
            // Set callbacks
            onCursorMove = { position ->
                Log.d("Cursor", "Moved to: ${position.x}, ${position.y}")
            }
            
            onGazeAutoClick = { position ->
                Log.d("Cursor", "Gaze click at: ${position.x}, ${position.y}")
            }
        }
        
        // Add to your layout
        addContentView(cursorView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }
    
    override fun onResume() {
        super.onResume()
        cursorView.startTracking()
    }
    
    override fun onPause() {
        super.onPause()
        cursorView.stopTracking()
    }
}
```

## Core Components

### CursorView
The main UI component that renders the cursor and handles interactions.

```kotlin
// Key methods
cursorView.centerCursor()                    // Reset to center
cursorView.setVisible(visible: Boolean)      // Show/hide
cursorView.updateCursorStyle(config)         // Change appearance
cursorView.toggleCoordinateDisplay()         // Show X,Y coords
cursorView.enableGaze()                      // Enable gaze click
```

### CursorAdapter (Fixed 2025-01-28)
Transforms IMU sensor data into screen coordinates using delta-based processing.

**Critical Implementation Details:**
```kotlin
// CORRECT implementation (after fix)
class CursorAdapter {
    private var previousOrientation: Quaternion? = null
    private var currentX = screenWidth / 2f    // Start at center!
    private var currentY = screenHeight / 2f   // Not at (0,0)!
    
    suspend fun processOrientationForCursor(data: OrientationData) {
        // Calculate delta from previous frame
        val delta = previousOrientation?.inverse?.times(data.quaternion)
            ?: return
        
        // Convert to Euler angles
        val euler = delta.toEulerAngles()
        
        // Use TANGENT scaling (not linear!)
        val deltaX = tan(euler.yaw) * screenWidth * 2.0f
        val deltaY = -tan(euler.pitch) * screenHeight * 3.0f
        
        // Update position (no 0.1x killer!)
        currentX = (currentX + deltaX).coerceIn(0f, screenWidth)
        currentY = (currentY + deltaY).coerceIn(0f, screenHeight)
        
        // Save for next frame
        previousOrientation = data.quaternion
    }
}
```

### Common Pitfalls to Avoid

#### ❌ DON'T: Use absolute orientation
```kotlin
// WRONG - causes drift and wrong movement
val euler = orientationData.quaternion.toEulerAngles()
val x = euler.yaw * screenWidth  // Absolute position
```

#### ✅ DO: Use delta-based processing
```kotlin
// CORRECT - frame-to-frame changes
val delta = previous.inverse * current
val deltaEuler = delta.toEulerAngles()
```

#### ❌ DON'T: Scale linearly
```kotlin
// WRONG - unnatural movement
val movement = angle * screenSize * sensitivity
```

#### ✅ DO: Use tangent scaling
```kotlin
// CORRECT - natural movement
val movement = tan(angle) * screenSize * sensitivity
```

#### ❌ DON'T: Start at origin
```kotlin
// WRONG - cursor stuck in corner
private var x = 0f
private var y = 0f
```

#### ✅ DO: Start at screen center
```kotlin
// CORRECT - visible immediately
private var x = screenWidth / 2f
private var y = screenHeight / 2f
```

## Configuration Options

### CursorConfig
```kotlin
data class CursorConfig(
    val type: CursorType = CursorType.Normal,
    val size: Int = 48,                    // Diameter in pixels
    val color: Int = 0xFF4CAF50.toInt(),   // ARGB color
    val strokeWidth: Float = 2.0f,         // Border width
    val glassOpacity: Float = 0.3f,        // Glass effect
    val showCoordinates: Boolean = false,   // Display X,Y
    val showCrosshair: Boolean = false,     // Crosshair overlay
    val speed: Float = 1.0f,                // Movement sensitivity
    val jitterFilterEnabled: Boolean = true,
    val filterStrength: FilterStrength = FilterStrength.Medium
)
```

### Sensitivity Tuning
```kotlin
// In CursorAdapter
private val sensitivityX = 2.0f  // Horizontal sensitivity
private val sensitivityY = 3.0f  // Vertical sensitivity (higher for portrait)
private val deadZoneThreshold = 0.001f  // Ignore micro-movements

// Adjust based on device/user preference
fun setSensitivity(x: Float, y: Float) {
    sensitivityX = x.coerceIn(0.5f, 5.0f)
    sensitivityY = y.coerceIn(0.5f, 5.0f)
}
```

## Debugging & Troubleshooting

### Enable Debug Logging
```kotlin
// In CursorAdapter
companion object {
    private const val TAG = "CursorAdapter"
    private const val DEBUG = true  // Set to true for verbose logs
}

// Logs will show:
// D/CursorAdapter: === CURSOR DEBUG ===
// D/CursorAdapter: Delta: yaw=0.052, pitch=-0.031
// D/CursorAdapter: Movement: deltaX=98.5, deltaY=-59.2
// D/CursorAdapter: Position: (1058.5, 480.8)
```

### Common Issues & Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| Cursor at (0,0) | Stuck in top-left corner | Ensure initialization calls `centerCursor()` |
| No movement | Cursor doesn't respond | Check sensor permissions, verify IMU data flow |
| Jumpy cursor | Erratic movement | Increase filter strength, adjust dead zone |
| Drift | Cursor moves when still | Implement dead zone, check base orientation |
| Stuck cursor | No movement for 5s | Auto-recalibration triggers, check logs |

### Testing Cursor Behavior
```kotlin
// Test methods in CursorView
cursorView.testSmoothMovement(targetX = 500f, targetY = 300f)
cursorView.testClickPulse()
cursorView.testHoverGlow(enable = true)
cursorView.testGazeClickAnimation()

// Get current state for debugging
val animState = cursorView.getAnimationState()
val gestureState = cursorView.getGestureState()
```

## Performance Optimization

### Frame Rate Target
- **Goal**: 60 FPS (16ms per frame)
- **Sensor Rate**: 120Hz (8ms sampling)
- **Processing Budget**: 11-16ms total

### Optimization Tips
1. **Use Hardware Acceleration**
   ```kotlin
   setLayerType(View.LAYER_TYPE_HARDWARE, null)
   ```

2. **Batch Sensor Updates**
   ```kotlin
   // Process every 2nd sample to reduce load
   if (frameCount % 2 == 0) {
       processOrientation(data)
   }
   ```

3. **Optimize Rendering**
   ```kotlin
   // Only invalidate changed region
   invalidate(
       (x - radius).toInt(), (y - radius).toInt(),
       (x + radius).toInt(), (y + radius).toInt()
   )
   ```

## Testing

### Unit Tests (Implemented)
Run the comprehensive test suite:
```bash
./gradlew :modules:libraries:DeviceManager:test
```

Key test files:
- `CursorAdapterTest.kt` - Integration tests
- `CursorAdapterMathTest.kt` - Mathematical validation

### Manual Testing Checklist
- [ ] Cursor initializes at screen center
- [ ] Smooth movement in all directions
- [ ] No drift when device is stationary
- [ ] Boundaries properly constrained
- [ ] Auto-recalibration after 5s stuck
- [ ] Gaze click triggers after 1.5s
- [ ] Coordinates display correctly
- [ ] Animations play smoothly

## Integration with Other Modules

### With VoiceAccessibility
```kotlin
// In your AccessibilityService
class MyAccessibilityService : AccessibilityService() {
    private lateinit var cursorHelper: VoiceCursorHelper
    
    override fun onServiceConnected() {
        cursorHelper = VoiceCursorHelper(this).apply {
            initialize()
            setOnClickListener { position ->
                // Dispatch click at cursor position
                performGlobalAction(position.x, position.y)
            }
        }
    }
}
```

### With VoiceUI
```kotlin
// Show menu at cursor position
cursorView.onMenuRequest = { position ->
    voiceUIMenu.showAt(position.x, position.y)
}
```

## Permissions Required
```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.BODY_SENSORS" />
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
<uses-permission android:name="android.permission.VIBRATE" />
```

## Migration from Legacy
If migrating from the old VoiceOsCursor:
1. Replace `VoiceOsCursor` with `CursorView`
2. Update package imports to `com.augmentalis.voiceos.cursor`
3. Change from `setOrientation(α,β,γ)` to IMU-based tracking
4. Update sensitivity values (legacy used 8.0, new uses 2.0-3.0)

## Troubleshooting Flowchart
```
Cursor not moving?
├── Check initialization
│   └── Is centerCursor() called?
├── Check sensor data
│   └── Is IMUManager providing data?
├── Check math
│   └── Are deltas being calculated?
├── Check scaling
│   └── Is tangent function used?
└── Check rendering
    └── Is invalidate() called?
```

## Support & Resources
- **Module Owner**: VOS4 Development Team
- **Documentation**: `/docs/modules/voicecursor/`
- **Tests**: `/modules/libraries/DeviceManager/src/test/`
- **Issues**: Create ticket with `voicecursor` tag

---
**Guide Version:** 1.0.0  
**For Module:** 2.1.0  
**Last Updated:** 2025-01-28
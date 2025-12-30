# GazeHandler Documentation

## Overview

The `GazeHandler` is a critical component of the VOS4 VoiceAccessibility system that provides gaze tracking and eye-based interaction capabilities. It has been migrated from Legacy Avenue's `GazeActions.kt` while maintaining 100% backward compatibility and adding advanced VOS4 features.

## Features

### Legacy Avenue Compatibility
- **GAZE_ON/GAZE_OFF** commands - Original functionality preserved
- **Cursor visibility integration** - Respects cursor state like original
- **Same failure modes** - Maintains original error handling behavior

### VOS4 Enhancements
- **Advanced gaze tracking** via HUDManager integration
- **Auto-click on dwell** with configurable timing
- **Gaze calibration system** with user feedback learning
- **Voice-gaze fusion** commands for natural interaction
- **Performance monitoring** and success rate tracking
- **Comprehensive error handling** and recovery

## Architecture

### Core Components

```
GazeHandler
├── HUDManager Integration
│   └── GazeTracker (ML Kit face detection)
├── VoiceCursor Integration  
│   └── GazeClickManager (dwell timing)
├── Voice Command Processing
│   └── ActionCoordinator integration
└── Gesture Dispatch
    └── VoiceAccessibilityService
```

### Dependencies
- **HUDManager**: Provides `GazeTracker` for eye tracking
- **VoiceAccessibilityService**: Gesture dispatch and cursor manager access
- **ActionCoordinator**: Voice command routing and interpretation
- **VoiceCursor**: Optional integration for cursor-based gaze

## Supported Commands

### Legacy Avenue Commands (Backward Compatibility)
| Command | Description | Original Behavior |
|---------|-------------|------------------|
| `gaze_on` | Enable gaze tracking | ✅ Requires cursor visible |
| `gaze_off` | Disable gaze tracking | ✅ Requires cursor visible |

### Enhanced VOS4 Commands
| Command | Description | Parameters |
|---------|-------------|------------|
| `enable_gaze` | Enable gaze tracking | - |
| `disable_gaze` | Disable gaze tracking | - |
| `gaze_click` | Click at gaze position | `force: Boolean` |
| `dwell_click` | Same as gaze_click | - |
| `look_and_click` | Immediate gaze click | - |
| `gaze_tap` | Same as look_and_click | - |
| `gaze_calibrate` | Calibrate gaze system | `targetX: Float, targetY: Float` |
| `calibrate_gaze` | Same as gaze_calibrate | - |
| `gaze_center` | Reset gaze to center | - |
| `center_gaze` | Same as gaze_center | - |
| `toggle_dwell` | Toggle auto-click on dwell | - |
| `dwell_toggle` | Same as toggle_dwell | - |
| `gaze_reset` | Reset gaze tracking system | - |
| `reset_gaze` | Same as gaze_reset | - |
| `gaze_status` | Show gaze information | - |
| `where_am_i_looking` | Same as gaze_status | - |
| `gaze_help` | Show help information | - |

## Voice Command Integration

The GazeHandler integrates with ActionCoordinator for natural language processing:

```kotlin
// Natural language examples that get interpreted:
"gaze on" -> "gaze_on"
"enable gaze" -> "enable_gaze" 
"look and click" -> "look_and_click"
"calibrate gaze" -> "gaze_calibrate"
"where am I looking" -> "gaze_status"
```

## Configuration

### Gaze Tracking Parameters
```kotlin
companion object {
    private const val DWELL_TIME_MS = 1500L           // Auto-click delay
    private const val CALIBRATION_THRESHOLD = 0.8f    // Min confidence for click
    private const val GAZE_STABILITY_WINDOW_MS = 300L // Stability requirement
}
```

### Performance Tuning
- **Monitoring Rate**: 20 FPS (50ms intervals)
- **Gaze Stability**: 300ms window for dwell detection
- **Click Confidence**: 80% minimum (configurable)
- **Coordinate System**: Normalized [-1.0, 1.0] range

## Integration Patterns

### ActionCoordinator Registration
```kotlin
// In ActionCoordinator.initialize()
registerHandler(ActionCategory.GAZE, GazeHandler(service))

// Priority order (GAZE has high priority)
val priorityOrder = listOf(
    ActionCategory.SYSTEM,
    ActionCategory.NAVIGATION, 
    ActionCategory.APP,
    ActionCategory.GAZE,        // High priority for gaze
    ActionCategory.GESTURE,
    // ...
)
```

### Voice Command Interpretation
```kotlin
// In ActionCoordinator.interpretVoiceCommand()
command.contains("gaze on") || command.contains("enable gaze") -> "gaze_on"
command.contains("look and click") || command.contains("gaze tap") -> "look_and_click"
// ... additional patterns
```

## Error Handling

### Graceful Degradation
- **HUDManager unavailable**: Gaze features disabled, voice commands return false
- **GazeTracker initialization fails**: Limited functionality with graceful fallbacks
- **Cursor manager unavailable**: Gaze works but cursor integration disabled
- **Gesture dispatch fails**: Click attempts logged and reported

### Legacy Compatibility Failures
- **Cursor not visible**: `gaze_on`/`gaze_off` return false (preserves original behavior)
- **Service unavailable**: All commands fail gracefully with appropriate logging

## Performance Monitoring

### Metrics Tracked
```kotlin
// Internal metrics for performance optimization
private var clickSuccessCount = 0
private var totalClickAttempts = 0
private var averageGazeAccuracy = 0.85f

// Accessible via gaze_status command
fun getSuccessRate(): Float = 
    if (totalClickAttempts > 0) clickSuccessCount.toFloat() / totalClickAttempts else 0f
```

### Memory Management
- **Coroutine scopes**: Properly managed and cancelled on disposal
- **Resource cleanup**: All tracking stopped and resources released
- **Memory leaks**: Prevented through proper lifecycle management

## Testing

### Test Coverage
- **Legacy compatibility**: All original GazeActions functionality tested
- **Enhanced features**: Comprehensive test suite for VOS4 additions
- **Error scenarios**: Edge cases and failure modes covered
- **Integration tests**: End-to-end workflow validation

### Key Test Classes
```kotlin
@Nested
@DisplayName("Legacy Avenue Compatibility Tests")
inner class LegacyCompatibilityTests {
    // Tests for gaze_on/gaze_off with cursor visibility
}

@Nested  
@DisplayName("Gaze Click Tests")
inner class GazeClickTests {
    // Tests for gaze-based clicking functionality
}
```

## Migration Notes

### From Legacy Avenue
1. **100% Backward Compatibility**: All original commands work identically
2. **Enhanced Error Handling**: Better logging and graceful degradation
3. **Performance Improvements**: Modern coroutine-based architecture
4. **Additional Features**: New commands and capabilities added

### Breaking Changes
- **None**: Complete compatibility maintained with Legacy Avenue behavior

### Recommended Upgrades
- Use `enable_gaze`/`disable_gaze` for new implementations
- Leverage `look_and_click` for immediate gaze interaction
- Implement `gaze_calibrate` for improved accuracy
- Monitor performance with `gaze_status`

## Troubleshooting

### Common Issues

#### "Gaze not working"
1. Check cursor visibility: `gaze_status` 
2. Verify HUDManager initialization
3. Confirm camera permissions
4. Test with `gaze_reset`

#### "Low gaze accuracy"
1. Run `gaze_calibrate` with known targets
2. Check lighting conditions
3. Verify face detection working
4. Use `gaze_center` to reset

#### "Dwell clicks not firing"
1. Check dwell enabled: `toggle_dwell`
2. Verify gaze stability requirements
3. Adjust `DWELL_TIME_MS` if needed
4. Monitor with `gaze_status`

### Debug Commands
```kotlin
// Get comprehensive status
"gaze status" -> Shows all current state

// Reset if stuck
"gaze reset" -> Reinitializes system

// Show available commands  
"gaze help" -> Lists all supported actions
```

## Future Enhancements

### Planned Features
- **Dynamic calibration**: Continuous learning from user behavior
- **Multi-target tracking**: Support for multiple simultaneous gaze targets
- **Gesture integration**: Combine gaze with head gestures
- **AR/VR support**: Extended reality gaze interaction

### Performance Optimization
- **Hardware acceleration**: GPU-based eye tracking
- **Reduced latency**: Sub-50ms gaze-to-click response
- **Battery optimization**: Efficient camera usage
- **Accuracy improvements**: Advanced ML models

## API Reference

### Public Methods
```kotlin
class GazeHandler(private val service: VoiceAccessibilityService) : ActionHandler {
    
    // ActionHandler implementation
    fun initialize()
    fun canHandle(action: String): Boolean
    fun execute(category: ActionCategory, action: String, params: Map<String, Any>): Boolean
    fun getSupportedActions(): List<String>
    fun dispose()
}
```

### Internal Architecture
```kotlin
// Core gaze system integration
private var gazeTracker: GazeTracker?
private var hudManager: HUDManager?
private var isGazeEnabled: Boolean

// State management  
private var currentTarget: GazeTarget?
private var dwellStartTime: Long
private var isCalibrationMode: Boolean

// Performance tracking
private var clickSuccessCount: Int
private var averageGazeAccuracy: Float
```

## Related Documentation

- **[ActionHandler Interface](ActionHandler.md)**: Base handler architecture
- **[ActionCoordinator](ActionCoordinator.md)**: Command routing system  
- **[HUDManager](../HUDManager/HUDManager-Documentation.md)**: Spatial and gaze tracking
- **[VoiceAccessibility](VoiceAccessibility-Documentation.md)**: Main service architecture
- **[Legacy Avenue Migration](../Migration/LegacyAvenue-Migration-Guide.md)**: Migration guidelines

---

**Version**: 1.0.0  
**Created**: 2025-09-03  
**Author**: VOS4 Development Team  
**Status**: Production Ready  
**Legacy Compatibility**: 100% ✅
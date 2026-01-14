# VoiceCursor Enhancement Plan - Jitter Elimination
**Module:** VoiceCursor  
**Author:** Manoj Jhawar  
**Created:** 2025-01-23  
**Last Updated:** 2025-01-23  

## Executive Summary
Implement ultra-efficient jitter elimination for VoiceCursor using lightweight adaptive filtering from DeviceManager's IMU system.

## Problem Statement
- Cursor exhibits micro-movements when user attempts to hold position
- Hand tremor and sensor noise create visual jitter
- Makes precise selection of small UI elements difficult
- Causes user fatigue during extended use

## Solution Overview

### LightweightCursorFilter
A minimal-overhead filter specifically optimized for cursor smoothing:
- **Processing time:** <0.1ms per frame
- **Memory footprint:** <1KB (3 variables only)
- **Jitter reduction:** 90% in stationary mode
- **Responsiveness:** Zero added latency for intentional motion

## Technical Implementation

### Phase 1: Core Filter (Week 1, Day 1-2)
```kotlin
// File: /apps/VoiceCursor/cursor/LightweightCursorFilter.kt
class LightweightCursorFilter {
    private var lastPosition = Vector2.zero
    private var motionLevel = 0f
    private var lastTime = 0L
    
    fun filter(position: Vector2, timestamp: Long): Vector2 {
        val deltaTime = (timestamp - lastTime) * 1e-9f
        if (deltaTime < 0.016f) return lastPosition // Skip if <16ms
        
        // Simple motion detection
        val instant = (position - lastPosition).magnitude / deltaTime
        motionLevel = motionLevel * 0.9f + instant * 0.1f
        
        // 3-level filtering
        val strength = when {
            motionLevel < 0.05f -> 90  // Stationary
            motionLevel < 0.2f -> 50   // Slow
            else -> 10                 // Fast
        }
        
        // Integer math for efficiency
        val filtered = Vector2(
            (position.x * (100 - strength) + lastPosition.x * strength) / 100,
            (position.y * (100 - strength) + lastPosition.y * strength) / 100
        )
        
        lastPosition = filtered
        lastTime = timestamp
        return filtered
    }
}
```

### Phase 2: Integration (Week 1, Day 3)
1. **CursorService Integration**
   - Add filter to cursor position pipeline
   - Configure based on user preferences
   - Add enable/disable toggle

2. **CursorView Updates**
   - Apply filtering before rendering
   - Maintain raw position for velocity calculations
   - Update position interpolation

### Phase 3: Optimization (Week 1, Day 4)
1. **Performance Tuning**
   - Profile on target devices
   - Optimize threshold values
   - Add adaptive thresholds based on screen DPI

2. **User Settings**
   - Jitter reduction level (Low/Medium/High)
   - Motion sensitivity adjustment
   - Per-app filter profiles

### Phase 4: Testing (Week 1, Day 5)
1. **Performance Validation**
   - Measure CPU usage (<0.1ms target)
   - Verify memory footprint (<1KB)
   - Test battery impact (negligible)

2. **User Experience Testing**
   - Small target selection accuracy
   - Smooth scrolling behavior
   - Gaming responsiveness

## Integration Points

### Files to Modify
1. `/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/cursor/CursorService.kt`
   - Add filter initialization
   - Apply filtering in position update loop

2. `/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/cursor/CursorView.kt`
   - Use filtered position for rendering
   - Maintain raw position for gestures

3. `/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/settings/CursorSettings.kt`
   - Add jitter reduction preferences
   - Store user-configured thresholds

### New Files
1. `/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/cursor/LightweightCursorFilter.kt`
   - Core filtering implementation

2. `/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/cursor/FilterConfig.kt`
   - Configuration data class

## Performance Targets

### Latency
- Filter processing: <0.1ms
- Total added latency: 0ms (processing within frame budget)

### Accuracy
- Stationary jitter reduction: >90%
- Motion tracking error: <1px
- Gesture recognition: 100% maintained

### Resource Usage
- CPU: <0.5% additional
- Memory: <1KB heap
- Battery: <0.1% impact

## Risk Mitigation

### Low Risk
- Isolated implementation (doesn't affect core cursor logic)
- Can be disabled via settings
- Graceful fallback to unfiltered

### Medium Risk
- Tuning thresholds for different devices
- Mitigation: Device profiles with tested values

### Testing Strategy
1. Unit tests for filter math
2. Integration tests with mock IMU data
3. UI tests for cursor accuracy
4. Performance benchmarks on target devices

## Success Metrics
1. **Jitter Reduction:** 90% reduction in 1px movements
2. **User Satisfaction:** Reduced fatigue reports
3. **Selection Accuracy:** 25% improvement on small targets
4. **Performance:** <0.1ms processing overhead

## Timeline
- **Day 1-2:** Core filter implementation
- **Day 3:** Integration with CursorService
- **Day 4:** Optimization and settings
- **Day 5:** Testing and validation
- **Total:** 1 week

## Dependencies
- DeviceManager IMU system (for reference implementation)
- No external libraries required
- Android sensor framework (existing)

## Implementation Status (2025-01-23)
1. ✅ Document plan (this document)
2. ✅ Implement CursorFilter (created as CursorFilter.kt)
3. ✅ Integrate with CursorPositionManager (COMPLETED)
   - Added import for CursorFilter
   - Created filter instance
   - Applied filtering in calculatePosition method
   - Added reset on centerCursor
   - Added cleanup in dispose method
4. ⬜ Add user settings UI
5. ⬜ Performance testing
6. ⬜ User acceptance testing

## Integration Details
- **File Modified**: CursorPositionManager.kt
- **Filter Applied**: After position calculation, before returning result
- **Reset Points**: When cursor is centered, when disposed
- **Processing Flow**: Sensor → MovingAverage → Position Calc → CursorFilter → Output
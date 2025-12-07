# VoiceAccessibility Integration Tests

This directory contains comprehensive integration tests for the VoiceAccessibility app, focusing on voice command processing, action coordination, and handler execution.

## Test Structure

### `/integration/VoiceCommandIntegrationTest.kt`
- End-to-end voice command processing pipeline
- Command routing through ActionCoordinator
- Confidence filtering and thresholds
- Command variations and interpretation
- Handler execution verification
- Multi-step command sequences
- Performance and concurrency testing

### `/mocks/MockActionCoordinator.kt`
- Mock implementation of ActionCoordinator
- Tracks all command processing with metrics
- Simulates realistic action execution
- Provides comprehensive verification methods
- Thread-safe implementation with timing data

### `/mocks/MockVoiceAccessibilityService.kt`
- Mock AccessibilityService for testing handlers
- Simulates gesture dispatch and global actions
- Tracks all accessibility operations
- Provides gesture creation utilities
- No accessibility permissions required for testing

## Running the Tests

### Prerequisites
1. VoiceAccessibility app must be available
2. ActionCoordinator and handlers must be implemented
3. AIDL interfaces for VoiceRecognition integration
4. Test environment with proper Android SDK

### Command Line
```bash
# Run all integration tests
./gradlew :VoiceAccessibility:connectedAndroidTest

# Run specific test class
./gradlew :VoiceAccessibility:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.voiceaccessibility.integration.VoiceCommandIntegrationTest

# Run with verbose logging
./gradlew :VoiceAccessibility:connectedAndroidTest --info --stacktrace
```

### Android Studio
1. Right-click on test class or method
2. Select "Run 'TestName'"
3. View results in Run window with detailed logs

## Test Coverage

### Voice Command Processing
- ✅ Basic voice command routing
- ✅ Confidence-based filtering
- ✅ Command variations and interpretation
- ✅ Natural language processing
- ✅ Handler execution verification
- ✅ Error handling and recovery

### Integration Testing
- ✅ End-to-end AIDL communication
- ✅ VoiceRecognition service integration
- ✅ ActionCoordinator routing
- ✅ Handler execution chains
- ✅ Multi-step command sequences
- ✅ Concurrent command processing

### Performance Testing
- ✅ Command processing timing
- ✅ Handler execution performance
- ✅ Memory usage patterns
- ✅ Concurrent operation handling
- ✅ Resource cleanup verification

## Test Configuration

### Confidence Thresholds
- `HIGH_CONFIDENCE_THRESHOLD`: 0.8 (80%)
- `MIN_CONFIDENCE_THRESHOLD`: 0.5 (50%)
- `LOW_CONFIDENCE_THRESHOLD`: 0.3 (30%)

### Timeouts
- `COMMAND_TIMEOUT_MS`: 5000ms for command processing
- `BINDING_TIMEOUT_MS`: 10000ms for service binding
- Performance tests: <1000ms for command execution

### Test Commands
```kotlin
// Navigation commands
"go back", "go home", "scroll up", "scroll down"

// System commands  
"volume up", "volume down", "mute"

// App commands
"open settings", "launch camera", "start calculator"

// UI commands
"tap", "click", "swipe left", "swipe right"

// Input commands
"type hello world", "say goodbye"
```

## Mock Object Details

### MockActionCoordinator
- **Command Processing**: Simulates realistic command interpretation
- **Action Execution**: Maps commands to appropriate handler categories
- **Performance Metrics**: Tracks timing, success rates, and execution patterns
- **Thread Safety**: Concurrent access support with atomic operations
- **Verification**: Comprehensive query methods for test assertions

Key Methods:
```kotlin
fun processCommand(commandText: String): Boolean
fun processVoiceCommand(text: String, confidence: Float): Boolean
fun executeAction(action: String, params: Map<String, Any>): Boolean
fun hasProcessedCommand(command: String): Boolean
fun getMetricsForAction(action: String): MetricData?
```

### MockVoiceAccessibilityService
- **Gesture Simulation**: Creates and tracks gesture descriptions
- **Action Tracking**: Records all accessibility service calls
- **State Management**: Maintains service lifecycle state
- **No Permissions**: Works without accessibility service permissions
- **Utility Methods**: Gesture creation helpers for testing

Key Methods:
```kotlin
fun mockDispatchGesture(gesture: GestureDescription, callback: GestureResultCallback?, handler: Handler?): Boolean
fun mockPerformGlobalAction(action: Int): Boolean
fun hasPerformedGesture(gestureType: String): Boolean
fun createTapGesture(x: Float, y: Float, duration: Long): GestureDescription
```

## Command Processing Pipeline

### 1. Voice Input
```
Voice Recognition → AIDL Callback → ActionCoordinator
```

### 2. Command Interpretation
```
Raw Text → Normalization → Pattern Matching → Action Mapping
```

### 3. Handler Routing
```
Action → Handler Selection → Category Mapping → Execution
```

### 4. Result Processing
```
Handler Result → Metrics Recording → Callback Notification
```

## Handler Categories Tested

### System Handler
- Navigation: back, home, recents
- Volume: up, down, mute
- Device: power, settings access

### Navigation Handler  
- Scrolling: up, down, left, right
- Swiping: directional gestures
- Page navigation: next, previous

### App Handler
- App launching: settings, camera, phone
- Package-based launching
- Intent-based operations

### Device Handler
- Brightness control
- WiFi/Bluetooth toggles
- System setting modifications

### Input Handler
- Text input: typing, dictation
- Keyboard operations
- Text manipulation

### UI Handler
- Element interaction: tap, click, long press
- Gesture dispatch: swipe, pinch, zoom
- Coordinate-based operations

## Debugging Test Failures

### Command Processing Issues
1. Check command pattern matching in ActionCoordinator
2. Verify handler registration and initialization
3. Monitor confidence threshold effects
4. Review command interpretation logs

### Handler Execution Issues
1. Verify mock service is properly configured
2. Check action category mapping
3. Monitor gesture creation and dispatch
4. Review accessibility service simulation

### Performance Issues
1. Profile command processing timing
2. Check for resource leaks in mocks
3. Monitor concurrent operation handling
4. Verify proper cleanup in tearDown

### Integration Issues
1. Verify AIDL interface compatibility
2. Check VoiceRecognition service availability
3. Monitor callback registration/unregistration
4. Review cross-process communication

## Test Data Management

### Command Variations
Tests include multiple ways to express the same command:
- "go back" / "back" / "navigate back" / "return"
- "volume up" / "increase volume" / "turn up volume" / "louder"
- "open settings" / "launch settings" / "start settings"

### Confidence Testing
Different confidence levels are tested:
- High confidence (0.8+): Should always be processed
- Medium confidence (0.5-0.8): Normal processing
- Low confidence (<0.5): May be filtered or flagged

### Multi-Step Sequences
Complex command sequences are tested:
- App navigation: open → scroll → interact → back
- Volume control: up → up → down → mute
- UI interaction: scroll → tap → navigate

## Performance Benchmarks

### Expected Performance
- Single command processing: <100ms
- Handler execution: <50ms
- AIDL communication: <20ms per call
- Batch operations: <500ms for 10 commands

### Memory Usage
- Mock objects should use <10MB total
- No memory leaks between test runs
- Proper cleanup of all resources

## Best Practices

### Test Organization
- Group related test methods logically
- Use descriptive test method names
- Include both positive and negative test cases
- Test edge cases and error conditions

### Mock Usage
- Reset mock state between tests
- Verify expected interactions occurred
- Use realistic simulation timing
- Provide meaningful debug information

### Error Handling
- Test graceful degradation scenarios
- Verify error recovery mechanisms
- Ensure proper exception handling
- Maintain system stability under errors

## Integration with VoiceRecognition Tests

These tests complement the VoiceRecognition service tests:
- Shared AIDL interface contracts
- Compatible mock callback implementations
- Coordinated testing strategies
- End-to-end validation coverage

## Troubleshooting Guide

### Test Setup Issues
```bash
# Verify app installation
adb shell pm list packages | grep voiceaccessibility

# Check service availability
adb shell dumpsys activity services | grep VoiceAccessibility

# Clear app data
adb shell pm clear com.augmentalis.voiceaccessibility
```

### Common Failures
1. **Mock not responding**: Check reset() calls in setUp()
2. **Timing issues**: Adjust timeout values for slower devices
3. **Command not recognized**: Verify pattern matching logic
4. **Handler not found**: Check handler registration in coordinator

### Debug Logging
```kotlin
// Enable verbose logging in tests
mockActionCoordinator.getDebugInfo()
mockService.getDebugInfo()

// Monitor test execution
Log.d(TAG, "Test state: ${testDescription}")
```

This test suite provides comprehensive coverage of the VoiceAccessibility command processing pipeline with realistic mocks and thorough verification capabilities.
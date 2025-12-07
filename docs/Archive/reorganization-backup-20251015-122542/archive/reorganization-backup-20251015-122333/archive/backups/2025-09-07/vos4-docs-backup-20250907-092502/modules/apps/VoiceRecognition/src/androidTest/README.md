# VoiceRecognition Integration Tests

This directory contains comprehensive integration tests for the VoiceRecognition app, focusing on AIDL service communication and functionality verification.

## Test Structure

### `/service/ServiceBindingTest.kt`
- Tests service binding and unbinding lifecycle
- Verifies callback registration and communication
- Tests recognition start/stop operations
- Multi-client scenarios and service death recovery
- Service interface contract compliance

### `/integration/AidlCommunicationTest.kt`
- AIDL interface method signatures and return types
- Cross-process communication verification
- Performance and responsiveness testing
- Error handling in AIDL calls
- State consistency across method calls

### `/mocks/MockRecognitionCallback.kt`
- Mock implementation of `IRecognitionCallback.Stub()`
- Tracks all callback invocations with timestamps
- Provides verification methods with timeout handling
- Supports async operation testing
- Comprehensive debug information

## Running the Tests

### Prerequisites
1. VoiceRecognition service must be available on the device/emulator
2. App must have necessary permissions
3. Service must be properly configured in AndroidManifest.xml

### Command Line
```bash
# Run all integration tests
./gradlew :VoiceRecognition:connectedAndroidTest

# Run specific test class
./gradlew :VoiceRecognition:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.augmentalis.voicerecognition.service.ServiceBindingTest

# Run with logging
./gradlew :VoiceRecognition:connectedAndroidTest --info
```

### Android Studio
1. Right-click on test class or method
2. Select "Run 'TestName'"
3. View results in Run window

## Test Coverage

### Service Binding Tests
- ✅ Basic service binding/unbinding
- ✅ Callback registration/unregistration
- ✅ Recognition lifecycle (start/stop)
- ✅ Multi-client scenarios
- ✅ Service death and recovery
- ✅ Callback error handling
- ✅ Engine availability and status

### AIDL Communication Tests  
- ✅ Interface contract compliance
- ✅ Method signature verification
- ✅ Callback communication
- ✅ Multiple callback handling
- ✅ Error handling and edge cases
- ✅ State consistency
- ✅ Performance benchmarks

## Test Configuration

### Timeouts
- `BINDING_TIMEOUT_MS`: 10-15 seconds for service binding
- `CALLBACK_TIMEOUT_MS`: 5 seconds for callback responses
- `INTERFACE_TIMEOUT_MS`: 5 seconds for AIDL method calls

### Test Data
- Test engines: Uses available engines from service
- Test languages: "en-US" (default), configurable
- Test modes: 0 (continuous), 1 (single shot), 2 (streaming)

## Debugging Test Failures

### Service Binding Issues
1. Check if VoiceRecognitionService is properly declared
2. Verify AIDL files are compiled correctly
3. Ensure service has correct intent filters
4. Check logcat for service startup errors

### Callback Communication Issues
1. Verify callback interface implementation
2. Check for RemoteException in logs
3. Ensure callbacks are registered before testing
4. Monitor thread safety in callback handling

### Performance Issues
1. Check device/emulator performance
2. Monitor memory usage during tests
3. Verify garbage collection isn't affecting timing
4. Use profiler for detailed analysis

## Mock Objects

### MockRecognitionCallback
- Implements all IRecognitionCallback methods
- Provides synchronous and asynchronous verification
- Tracks metrics and timing information
- Thread-safe implementation
- Comprehensive debug output

## Integration with VoiceAccessibility Tests

These tests are designed to work with the VoiceAccessibility integration tests:
- Shared AIDL interfaces ensure compatibility
- Mock objects can be used across test suites  
- Common test patterns and utilities
- Consistent error handling approaches

## Best Practices

### Test Isolation
- Each test method starts fresh
- Services are bound/unbound per test
- Callbacks are registered/unregistered properly
- State is reset between test runs

### Error Handling
- Tests handle service unavailability gracefully
- Timeout mechanisms prevent hanging tests
- Clear failure messages for debugging
- Comprehensive logging for troubleshooting

### Performance Considerations
- Tests run efficiently on slower devices
- Reasonable timeouts for CI/CD environments
- Minimal resource usage during testing
- Proper cleanup prevents resource leaks

## Troubleshooting

### Common Issues
1. **Service won't bind**: Check manifest configuration and permissions
2. **Timeouts**: Adjust timeout values for slower devices/emulators
3. **Callback not called**: Verify service is actually performing operations
4. **State inconsistency**: Check for threading issues in service implementation

### Debug Commands
```bash
# Check if service is running
adb shell dumpsys activity services | grep VoiceRecognition

# Monitor service logs
adb logcat | grep VoiceRecognitionService

# Clear app data between test runs
adb shell pm clear com.augmentalis.voicerecognition
```
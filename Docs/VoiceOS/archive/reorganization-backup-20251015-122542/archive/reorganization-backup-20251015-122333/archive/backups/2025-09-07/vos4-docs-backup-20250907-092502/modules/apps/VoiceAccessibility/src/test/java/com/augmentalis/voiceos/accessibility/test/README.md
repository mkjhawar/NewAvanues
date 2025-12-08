# Voice Accessibility Test Framework

This directory contains comprehensive testing frameworks for the VoiceAccessibility module, covering all aspects of voice command processing from end-to-end integration to performance testing.

## Test Files Overview

### 1. VoiceCommandTestScenarios.kt
**Purpose**: Defines comprehensive test scenarios for all voice command handlers

**Features**:
- Test commands for each handler type (App, Navigation, System, UI, Device, Input, Action)
- Natural language variations for each command
- Confidence threshold test cases
- Invalid command test cases  
- Partial command test cases
- Expected result mappings

**Key Test Data**:
```kotlin
// Example app handler scenarios
val appHandlerScenarios = listOf(
    VoiceCommandScenario(
        category = ActionCategory.APP,
        command = "open settings",
        variations = listOf("open settings", "launch settings", "start settings"),
        expectedAction = "open settings",
        description = "Basic app launch - Settings"
    )
)

// Confidence threshold tests
val confidenceTestCases = listOf(
    ConfidenceTestCase(
        command = "open settings",
        confidenceLevel = 0.95f,
        shouldSucceed = true,
        description = "High confidence - should succeed"
    )
)
```

### 2. CommandExecutionVerifier.kt
**Purpose**: Mock system for testing command execution with comprehensive verification

**Features**:
- Mock AccessibilityNodeInfo for UI command testing
- Handler invocation tracking system
- Parameter verification with type and value checking
- Execution result verification
- Mock handlers for isolated testing

**Key Components**:
```kotlin
// Mock UI elements
val mockButton = MockAccessibilityNodeInfo.createMock(
    text = "Test Button",
    className = "android.widget.Button",
    isClickable = true
)

// Track handler invocations
val tracker = HandlerInvocationTracker()
tracker.trackInvocation(handler, category, action, params, result)

// Verify parameters
val verification = parameterVerifier.verifyParameters(
    actualParams,
    listOf(ParameterExpectation("text", "hello", String::class.java))
)
```

### 3. EndToEndVoiceTest.kt
**Purpose**: Complete end-to-end testing that simulates full voice command flow

**Features**:
- Mock voice recognition service
- Service binding simulation
- Complete flow testing: Voice Input → Service Binding → Callback → Routing → Execution
- Test suite execution with comprehensive results
- Timeout handling and error tracking

**Usage Example**:
```kotlin
val endToEndTest = EndToEndVoiceTest()
endToEndTest.setupTestEnvironment(context)

val result = endToEndTest.testCompleteVoiceFlow(
    voiceInput = "open settings",
    expectedCategory = ActionCategory.APP,
    expectedAction = "open settings"
)

// Run complete test suite
val suiteResult = endToEndTest.runCompleteTestSuite(context)
```

### 4. PerformanceTest.kt
**Purpose**: Comprehensive performance testing framework

**Features**:
- **Binding Latency Testing**: Measures service connection times
- **Command Processing Performance**: Throughput and latency under load
- **Memory Usage Monitoring**: Heap usage tracking during operations
- **Resource Cleanup Verification**: Memory leak detection

**Performance Metrics**:
```kotlin
// Binding latency test
val bindingTest = BindingLatencyTest()
val result = bindingTest.testServiceBinding(context, iterations = 100)

// Command processing performance
val processingTest = CommandProcessingTest()
val perfResult = processingTest.testCommandProcessing(
    context, 
    iterations = 1000, 
    concurrentThreads = 10
)

// Memory monitoring
val memoryTest = MemoryUsageTest()
val memResult = memoryTest.monitorMemoryUsage(context, operations)
```

## Test Data Examples

### Command Scenarios by Handler Type

**App Handler**:
- "open settings", "launch chrome", "start camera"
- Variations: "please open settings", "can you launch chrome"

**Navigation Handler**:
- "go back", "go home", "scroll up", "scroll down"
- Variations: "navigate back", "back button", "swipe up"

**System Handler**:
- "volume up", "volume down", "mute", "recent apps"
- Variations: "increase volume", "make it louder", "turn volume up"

**UI Handler**:
- "tap", "click", "swipe left", "swipe right"
- Variations: "touch", "press", "slide left"

**Device Handler**:
- "brightness up", "wifi on", "bluetooth off"
- Variations: "increase brightness", "enable wifi", "turn bluetooth off"

**Input Handler**:
- "type hello world", "enter", "delete"
- Variations: "say hello world", "write hello world", "input text"

### Confidence Thresholds

| Confidence Level | Expected Result | Description |
|-----------------|----------------|-------------|
| 0.95f | Success | High confidence |
| 0.85f | Success | Good confidence |
| 0.70f | Success | Minimum threshold |
| 0.60f | Failure | Below threshold |
| 0.40f | Failure | Low confidence |

### Performance Benchmarks

**Expected Performance Targets**:
- Service binding: < 1000ms average
- Command processing: < 100ms average
- Memory growth: < 5MB during operations
- Resource cleanup: 100% successful

## Usage Instructions

### Running Individual Tests

```kotlin
// Test voice command scenarios
val scenarios = VoiceCommandTestScenarios.appHandlerScenarios
scenarios.forEach { scenario ->
    // Test each command variation
    scenario.variations.forEach { command ->
        // Execute test logic
    }
}

// Verify command execution
val verifier = CommandExecutionVerifier()
verifier.setupMockHandlers()
val result = verifier.verifyCommandExecution(
    ActionCategory.APP,
    "open settings",
    mapOf("confidence" to 0.85f)
)

// End-to-end testing
val e2eTest = EndToEndVoiceTest()
e2eTest.setupTestEnvironment(context)
val testResult = e2eTest.testCompleteVoiceFlow(
    "open settings",
    ActionCategory.APP,
    "open settings"
)

// Performance testing
val perfController = PerformanceTestController()
val perfResult = perfController.runCompletePerformanceTest(context)
```

### Integration with JUnit

```kotlin
@RunWith(AndroidJUnit4::class)
class VoiceAccessibilityTests {
    
    private lateinit var endToEndTest: EndToEndVoiceTest
    private lateinit var verifier: CommandExecutionVerifier
    
    @Before
    fun setup() {
        endToEndTest = EndToEndVoiceTest()
        verifier = CommandExecutionVerifier()
        verifier.setupMockHandlers()
    }
    
    @Test
    fun testAllAppCommands() {
        VoiceCommandTestScenarios.appHandlerScenarios.forEach { scenario ->
            val result = endToEndTest.testCompleteVoiceFlow(
                scenario.command,
                scenario.category,
                scenario.expectedAction
            )
            assertTrue("Failed: ${scenario.command}", result.overallSuccess)
        }
    }
    
    @Test
    fun testPerformanceBenchmarks() {
        val perfController = PerformanceTestController()
        val result = perfController.runCompletePerformanceTest(context)
        
        assertTrue("Binding too slow", result.bindingLatencyResult.averageLatencyMs < 1000)
        assertTrue("Processing too slow", result.commandProcessingResult.overallAverageMs < 100)
        assertTrue("Memory growth", result.memoryUsageResult.memoryGrowthMB < 5.0)
        assertTrue("Cleanup failed", result.resourceCleanupResult.cleanupSuccessful)
    }
}
```

## Test Results Analysis

### Success Metrics
- **Command Recognition**: % of commands correctly interpreted
- **Handler Routing**: % of commands routed to correct handler
- **Execution Success**: % of commands executed successfully
- **Performance Compliance**: Meeting latency and throughput targets
- **Resource Management**: Successful cleanup without leaks

### Failure Analysis
- **Invalid Commands**: Proper rejection of malformed input
- **Low Confidence**: Appropriate handling of uncertain input
- **Timeout Handling**: Graceful degradation on slow operations
- **Error Recovery**: System stability after failures

## Continuous Integration

These tests are designed to be run in CI/CD pipelines:

```bash
# Run all voice accessibility tests
./gradlew :apps:VoiceAccessibility:testDebugUnitTest

# Run specific test categories
./gradlew :apps:VoiceAccessibility:testDebugUnitTest --tests "*VoiceCommand*"
./gradlew :apps:VoiceAccessibility:testDebugUnitTest --tests "*Performance*"
```

## Contributing

When adding new voice commands or handlers:

1. Add test scenarios to `VoiceCommandTestScenarios.kt`
2. Update mock handlers in `CommandExecutionVerifier.kt`
3. Include new commands in end-to-end test cases
4. Update performance benchmarks if needed
5. Document expected behavior and edge cases

## Notes

- All tests use mock services to avoid dependency on actual voice recognition
- Performance tests should be run on consistent hardware for reliable benchmarks
- Memory tests may need adjustment based on target device specifications
- Test data includes realistic voice recognition variations and edge cases
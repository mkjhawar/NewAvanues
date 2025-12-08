# AppStateDetector Advanced Features - Quick Reference Guide

**Created:** 2025-10-13 01:46:00 PDT
**For:** Developers integrating advanced state detection features

## Quick Start

### Import Statements

```kotlin
import com.augmentalis.learnapp.state.AppState
import com.augmentalis.learnapp.state.AppStateDetector
import com.augmentalis.learnapp.state.StateDetectionResult
import com.augmentalis.learnapp.state.advanced.*
```

---

## 1. Material Design Pattern Matcher

### Basic Usage

```kotlin
val materialMatcher = MaterialDesignPatternMatcher()

// Detect all Material components
val components = materialMatcher.detectMaterialComponents(rootNode)

// Check specific node
val match = materialMatcher.detectMaterialComponent(node)
if (match != null && match.confidence > 0.8f) {
    println("Found ${match.component}: ${match.indicators}")
}
```

### Component Types
- `BUTTON` - Material Button
- `FAB` - Floating Action Button
- `TEXT_INPUT` - TextInputLayout
- `PROGRESS_LINEAR` - Linear progress
- `PROGRESS_CIRCULAR` - Circular progress
- `DIALOG` - Material Dialog
- `SNACKBAR` - Snackbar
- `CHIP` - Material Chip

---

## 2. Negative Indicator Analyzer

### Basic Usage

```kotlin
val negativeAnalyzer = NegativeIndicatorAnalyzer()

val indicators = negativeAnalyzer.analyzeNegativeIndicators(
    rootNode = rootNode,
    detectedState = AppState.LOGIN,
    textContent = textList,
    classNames = classList
)

val totalPenalty = negativeAnalyzer.calculateTotalPenalty(indicators)
val adjustedConfidence = negativeAnalyzer.applyPenalties(
    baseConfidence = 0.9f,
    indicators = indicators
)
```

### Indicator Types
- `COMPLEX_UI_ON_SIMPLE_STATE` - RecyclerView on login
- `INCONSISTENT_HIERARCHY` - Too deep/shallow
- `CONFLICTING_INDICATORS` - Loading + Error
- `ABNORMAL_ELEMENT_COUNT` - Wrong element count
- `MISMATCHED_INTERACTION` - Non-clickable button
- `TEMPORAL_INCONSISTENCY` - Duration mismatch

### Penalty Weights
- Major: 0.3 (strong contradiction)
- Moderate: 0.15 (moderate contradiction)
- Minor: 0.05 (weak contradiction)
- Max total: 0.8

---

## 3. Temporal State Validator

### Basic Usage

```kotlin
val temporalValidator = TemporalStateValidator()

// Track state changes
temporalValidator.trackStateChange(AppState.LOADING)

// Validate current state
val validation = temporalValidator.validateState(
    state = AppState.LOADING,
    baseConfidence = 0.85f
)

println("Valid: ${validation.isValid}")
println("Adjustment: ${validation.confidenceAdjustment}")
println("Reason: ${validation.reason}")

// Check for flicker
val flickerPatterns = temporalValidator.detectFlickerPatterns()
```

### Thresholds
- Transient: <500ms (penalty -0.3)
- Flicker: <200ms (penalty -0.4)
- Stable: >2000ms (bonus +0.1)

---

## 4. Multi-State Detection Engine

### Basic Usage

```kotlin
val multiStateEngine = MultiStateDetectionEngine()

// Detect multiple states
val multiResult = multiStateEngine.detectMultipleStates(detectionList)

println("Primary: ${multiResult.primaryState.state}")
println("Secondary: ${multiResult.secondaryStates.map { it.state }}")

// Check for specific state
if (multiResult.hasState(AppState.DIALOG)) {
    val confidence = multiResult.getConfidenceFor(AppState.DIALOG)
    println("Dialog confidence: $confidence")
}

// Validate combination
val isValid = multiStateEngine.isValidCombination(
    listOf(AppState.ERROR, AppState.DIALOG)
) // true
```

### Valid Combinations
- ERROR + DIALOG ✅
- LOADING + DIALOG ✅
- READY + DIALOG ✅
- LOADING + READY ❌
- LOGIN + TUTORIAL ❌

---

## 5. Hierarchy Pattern Matcher

### Basic Usage

```kotlin
val hierarchyMatcher = HierarchyPatternMatcher()

// Analyze entire hierarchy
val analysis = hierarchyMatcher.analyzeHierarchy(rootNode)
println("Max depth: ${analysis.maxDepth}")
println("Total nodes: ${analysis.totalNodes}")

// Get context for specific node
val context = hierarchyMatcher.getHierarchyContext(node)
println("Depth: ${context.depth}")
println("Is root level: ${context.isRootLevel}")
println("Has dialog ancestor: ${context.hasDialogAncestor}")

// Determine scope
val scope = hierarchyMatcher.determinePatternScope(context)

// Match pattern with hierarchy
val match = hierarchyMatcher.matchPattern(
    node = node,
    state = AppState.LOADING,
    baseConfidence = 0.8f,
    indicators = listOf("Progress bar")
)
```

### Pattern Scopes
- `FULL_SCREEN` - Root level (multiplier 1.0x)
- `DIALOG` - Within dialog (multiplier 0.9x)
- `SECTION` - Within section (multiplier 0.7x)
- `COMPONENT` - Individual component (multiplier 0.5x)
- `NESTED` - Deeply nested (multiplier 0.6x)

---

## 6. Confidence Calibrator

### Basic Usage

```kotlin
val calibrator = ConfidenceCalibrator()

// Calibrate confidence
val calibrated = calibrator.calibrateConfidence(
    result = detectionResult,
    temporalAdjustment = 0.1f,
    hierarchyAdjustment = -0.05f,
    negativePenalty = 0.2f
)

// Record detection for metrics
calibrator.recordDetection(
    result = detectionResult,
    actualState = AppState.LOGIN,
    correct = true
)

// Get metrics
val metrics = calibrator.getMetrics(AppState.LOGIN)
println("Accuracy: ${metrics.accuracyRate}")
println("Precision: ${metrics.getPrecision()}")
println("Recall: ${metrics.getRecall()}")
println("F1 Score: ${metrics.getF1Score()}")
```

### A/B Testing

```kotlin
// Create variant
val experimentalProfiles = mapOf(
    AppState.LOGIN to StateCalibrationProfile(
        state = AppState.LOGIN,
        baseThreshold = 0.75f,
        indicatorWeights = mapOf(
            "text_keywords" to 0.4f,
            "input_fields" to 0.5f,
            "button" to 0.1f
        )
    )
)

calibrator.createVariant("experiment_v1", experimentalProfiles)
calibrator.activateVariant("experiment_v1")

// Reset to defaults
calibrator.resetToDefaults()
```

---

## 7. State Metadata Extractor

### Basic Usage

```kotlin
val metadataExtractor = StateMetadataExtractor()

val metadata = metadataExtractor.extractMetadata(
    rootNode = rootNode,
    state = AppState.LOGIN
)

println("UI Framework: ${metadata.getFrameworkDescription()}")
println("Total elements: ${metadata.totalElements}")
println("Interactive: ${metadata.interactiveElements}")
println("Hierarchy depth: ${metadata.hierarchyDepth}")
println("Complexity: ${metadata.getComplexityScore()}")
println("Stable: ${metadata.isUIStable()}")
```

### UI Frameworks Detected
- `NATIVE_ANDROID` - Android Views
- `JETPACK_COMPOSE` - Compose
- `REACT_NATIVE` - React Native
- `FLUTTER` - Flutter
- `WEBVIEW` - WebView
- `UNITY` - Unity
- `MIXED` - Multiple frameworks
- `UNKNOWN` - Not detected

### Material Versions
- `MATERIAL_2` - Material Design 2
- `MATERIAL_3` - Material Design 3
- `NONE` - No Material Design
- `UNKNOWN` - Not detected

---

## Complete Integration Example

```kotlin
class EnhancedStateDetector {
    private val baseDetector = AppStateDetector()
    private val materialMatcher = MaterialDesignPatternMatcher()
    private val hierarchyMatcher = HierarchyPatternMatcher()
    private val negativeAnalyzer = NegativeIndicatorAnalyzer()
    private val temporalValidator = TemporalStateValidator()
    private val multiStateEngine = MultiStateDetectionEngine()
    private val calibrator = ConfidenceCalibrator()
    private val metadataExtractor = StateMetadataExtractor()

    fun detectState(rootNode: AccessibilityNodeInfo): EnhancedResult {
        // 1. Extract metadata
        val metadata = metadataExtractor.extractMetadata(rootNode, AppState.UNKNOWN)

        // 2. Base detection (all states)
        val baseResult = baseDetector.detectState(rootNode)

        // 3. Get all detection results (simplified)
        val allDetections = listOf(baseResult)

        // 4. Hierarchy context
        val hierarchyContext = hierarchyMatcher.getHierarchyContext(rootNode)
        val scope = hierarchyMatcher.determinePatternScope(hierarchyContext)
        val hierarchyMultiplier = when (scope) {
            PatternScope.FULL_SCREEN -> 1.0f
            PatternScope.DIALOG -> 0.9f
            PatternScope.SECTION -> 0.7f
            PatternScope.COMPONENT -> 0.5f
            PatternScope.NESTED -> 0.6f
        }

        // 5. Negative indicators
        val textContent = mutableListOf<String>()
        val classNames = mutableListOf<String>()
        // ... collect from tree traversal ...

        val negativeIndicators = negativeAnalyzer.analyzeNegativeIndicators(
            rootNode, baseResult.state, textContent, classNames
        )
        val negativePenalty = negativeAnalyzer.calculateTotalPenalty(negativeIndicators)

        // 6. Temporal validation
        temporalValidator.trackStateChange(baseResult.state)
        val temporalResult = temporalValidator.validateState(
            baseResult.state, baseResult.confidence
        )

        // 7. Multi-state detection
        val multiStateResult = multiStateEngine.detectMultipleStates(allDetections)

        // 8. Final calibration
        val finalConfidence = calibrator.calibrateConfidence(
            result = baseResult,
            temporalAdjustment = temporalResult.confidenceAdjustment,
            hierarchyAdjustment = (hierarchyMultiplier - 1.0f) * 0.2f,
            negativePenalty = negativePenalty
        )

        return EnhancedResult(
            primaryState = multiStateResult.primaryState,
            secondaryStates = multiStateResult.secondaryStates,
            metadata = metadata,
            calibratedConfidence = finalConfidence,
            negativeIndicators = negativeIndicators,
            temporalValidation = temporalResult,
            hierarchyContext = hierarchyContext
        )
    }
}

data class EnhancedResult(
    val primaryState: StateDetectionResult,
    val secondaryStates: List<StateDetectionResult>,
    val metadata: StateMetadata,
    val calibratedConfidence: Float,
    val negativeIndicators: List<NegativeIndicator>,
    val temporalValidation: TemporalValidationResult,
    val hierarchyContext: HierarchyContext
)
```

---

## Performance Tips

1. **Lazy Initialization:** Only create analyzers when needed
2. **Caching:** Cache Material patterns and hierarchy analysis
3. **Parallel Processing:** Run independent analyzers in parallel using coroutines
4. **Pruning:** Skip deep hierarchy analysis for simple UIs
5. **Batching:** Batch metrics collection for better performance

### Example: Parallel Processing

```kotlin
import kotlinx.coroutines.*

suspend fun detectStateParallel(rootNode: AccessibilityNodeInfo): EnhancedResult {
    return coroutineScope {
        val metadataDeferred = async { metadataExtractor.extractMetadata(rootNode, AppState.UNKNOWN) }
        val baseResultDeferred = async { baseDetector.detectState(rootNode) }
        val hierarchyDeferred = async { hierarchyMatcher.analyzeHierarchy(rootNode) }

        val metadata = metadataDeferred.await()
        val baseResult = baseResultDeferred.await()
        val hierarchyAnalysis = hierarchyDeferred.await()

        // ... continue with rest of detection ...
    }
}
```

---

## Common Patterns

### Pattern 1: Simple Detection with Calibration

```kotlin
val detector = AppStateDetector()
val calibrator = ConfidenceCalibrator()

val result = detector.detectState(rootNode)
val calibrated = calibrator.calibrateConfidence(result)
```

### Pattern 2: Multi-State with Temporal Validation

```kotlin
val detector = AppStateDetector()
val temporalValidator = TemporalStateValidator()
val multiStateEngine = MultiStateDetectionEngine()

val detections = /* get all state detections */
temporalValidator.trackStateChange(detections.first().state)
val temporal = temporalValidator.validateState(detections.first().state, detections.first().confidence)
val multiResult = multiStateEngine.detectMultipleStates(detections)
```

### Pattern 3: Full Pipeline with All Features

```kotlin
// See Complete Integration Example above
```

---

## Debugging

### Enable Logging

```kotlin
val config = StateDetectorConfig(
    logDetections = true
)
val detector = AppStateDetector(config)
```

### Export Metrics

```kotlin
val metrics = calibrator.exportMetrics()
metrics.forEach { (state, records) ->
    println("$state: ${records.size} detections")
    records.forEach { record ->
        println("  Detected: ${record.detectedState}, " +
                "Actual: ${record.actualState}, " +
                "Correct: ${record.correct}, " +
                "Confidence: ${record.confidence}")
    }
}
```

### Analyze Flicker Patterns

```kotlin
val patterns = temporalValidator.detectFlickerPatterns()
patterns.forEach { pattern ->
    println("Flicker: ${pattern.states} " +
            "(${pattern.occurrences} times, " +
            "avg ${pattern.avgDuration}ms)")
}
```

---

## Testing Utilities

### Mock Nodes for Testing

```kotlin
// Create mock AccessibilityNodeInfo for testing
fun createMockNode(
    className: String,
    text: String? = null,
    isClickable: Boolean = false,
    children: List<AccessibilityNodeInfo> = emptyList()
): AccessibilityNodeInfo {
    // ... implementation ...
}
```

### Test Scenarios

```kotlin
@Test
fun testLoginDetection() {
    val mockRoot = createMockNode(
        className = "LinearLayout",
        children = listOf(
            createMockNode("EditText", "Username"),
            createMockNode("EditText", "Password"),
            createMockNode("Button", "Sign In", isClickable = true)
        )
    )

    val result = detector.detectState(mockRoot)
    assertEquals(AppState.LOGIN, result.state)
    assertTrue(result.confidence > 0.7f)
}
```

---

## Troubleshooting

### Low Confidence Scores

1. Check negative indicators
2. Verify temporal stability
3. Review hierarchy context
4. Adjust calibration profile

### Incorrect State Detection

1. Review indicator weights
2. Check state combination rules
3. Analyze negative indicators
4. Increase logging

### Performance Issues

1. Profile with Android Profiler
2. Check for memory leaks in node traversal
3. Implement caching
4. Use parallel processing

---

## References

- Main Documentation: `AppStateDetector-Advanced-Features-251013-0146.md`
- Core Detector: `AppStateDetector.kt`
- Test Examples: (to be created)

---

**Last Updated:** 2025-10-13 01:46:00 PDT
**Version:** 1.0

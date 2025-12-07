# VoiceOS Accessibility Developer Guide

## Complete Development Reference for All Skill Levels

### Table of Contents
1. [Getting Started (Beginner)](#getting-started-beginner)
2. [Core Concepts (Intermediate)](#core-concepts-intermediate)
3. [Advanced Development (Expert)](#advanced-development-expert)
4. [Research & Innovation (PhD)](#research--innovation-phd)

---

## Getting Started (Beginner)

### What is an Accessibility Service?

Think of an accessibility service as a helpful assistant that can:
- **See** what's on the screen
- **Touch** buttons and controls for you
- **Type** text automatically
- **Navigate** between apps

### Your First Steps

#### Step 1: Understanding the Basics
```kotlin
// This is how you tell the phone to go back
VoiceAccessibility.executeCommand("go back")

// This is how you open an app
VoiceAccessibility.executeCommand("open chrome")

// This is how you click something
VoiceAccessibility.executeCommand("click submit")
```

#### Step 2: Setting Up Your Development Environment
1. Install Android Studio
2. Clone the VOS4 repository
3. Open the project
4. Navigate to `CodeImport/VoiceAccessibility`

#### Step 3: Running Your First Command
```kotlin
// Create a simple test app
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a button
        val button = Button(this).apply {
            text = "Go Back"
            setOnClickListener {
                // Execute command when button is clicked
                val success = VoiceAccessibility.executeCommand("go back")
                
                // Show result
                Toast.makeText(
                    this@MainActivity,
                    if (success) "Command worked!" else "Command failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        setContentView(button)
    }
}
```

### Common Beginner Mistakes & Solutions

#### Mistake 1: Service Not Enabled
```kotlin
// WRONG: Trying to use without checking
VoiceAccessibility.executeCommand("go back") // Might fail!

// RIGHT: Check first
if (VoiceAccessibility.isServiceEnabled()) {
    VoiceAccessibility.executeCommand("go back")
} else {
    // Guide user to enable service
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}
```

#### Mistake 2: Wrong Command Format
```kotlin
// WRONG: Incorrect command syntax
VoiceAccessibility.executeCommand("GO BACK") // Case matters!
VoiceAccessibility.executeCommand("goback")  // Spacing matters!

// RIGHT: Correct format
VoiceAccessibility.executeCommand("go back")
```

#### Mistake 3: Not Handling Failures
```kotlin
// WRONG: Ignoring return value
VoiceAccessibility.executeCommand("open myapp")

// RIGHT: Handle success/failure
if (!VoiceAccessibility.executeCommand("open myapp")) {
    // App might not be installed
    Log.e("Error", "Could not open app")
    // Try alternative
    VoiceAccessibility.executeCommand("open play store")
}
```

### Beginner Project Ideas
1. **Remote Control App**: Create buttons for common actions
2. **Voice Assistant**: Connect to speech recognition
3. **Macro Recorder**: Record and replay command sequences
4. **Accessibility Helper**: Help disabled users navigate

---

## Core Concepts (Intermediate)

### Understanding the Architecture

#### Component Hierarchy
```
VoiceAccessibility (Service)
    ├── Fast Path (Direct execution)
    ├── ActionCoordinator (Complex routing)
    │   └── Handlers (Specialized processors)
    └── Managers (Support systems)
```

### Working with Handlers

#### Creating a Custom Handler
```kotlin
// Step 1: Define your handler
class CustomGameHandler(
    private val service: VoiceAccessibility
) : ActionHandler {
    
    companion object {
        private const val TAG = "GameHandler"
    }
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        Log.d(TAG, "Executing game action: $action")
        
        return when (action.lowercase()) {
            "jump" -> performJump()
            "shoot" -> performShoot()
            "pause game" -> performPause()
            else -> false
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val gameActions = listOf("jump", "shoot", "pause game", "resume game")
        return gameActions.any { action.lowercase().contains(it) }
    }
    
    override fun getSupportedActions(): List<String> {
        return listOf("jump", "shoot", "pause game", "resume game")
    }
    
    private fun performJump(): Boolean {
        // Find jump button on screen
        val rootNode = service.rootInActiveWindow ?: return false
        val jumpButton = findNodeByText(rootNode, "jump") ?: return false
        return jumpButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    
    private fun performShoot(): Boolean {
        // Implement shooting logic
        return true
    }
    
    private fun performPause(): Boolean {
        // Send pause key event
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
    
    private fun findNodeByText(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        if (node.text?.toString()?.equals(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNodeByText(child, text)?.let { return it }
            }
        }
        
        return null
    }
}
```

#### Registering Your Handler
```kotlin
// In ActionCoordinator initialization
fun registerCustomHandlers() {
    // Add your custom handler
    registerHandler(ActionCategory.CUSTOM, CustomGameHandler(service))
    
    // Initialize it
    handlers[ActionCategory.CUSTOM]?.initialize()
}
```

### Configuration Management

#### Advanced Configuration
```kotlin
// Create configuration with all options
val advancedConfig = ServiceConfiguration(
    isEnabled = true,
    handlersEnabled = true,
    cursorEnabled = true,
    dynamicCommandsEnabled = true,
    performanceMode = PerformanceMode.HIGH,
    commandCacheDuration = 10000L, // 10 seconds
    maxCachedCommands = 200,
    debugLogging = BuildConfig.DEBUG,
    metricsEnabled = true
)

// Apply configuration
VoiceAccessibility.updateConfiguration(advancedConfig)

// Save to SharedPreferences
fun saveConfiguration(config: ServiceConfiguration) {
    val prefs = getSharedPreferences("vos_config", Context.MODE_PRIVATE)
    val configMap = config.toMap()
    
    prefs.edit().apply {
        configMap.forEach { (key, value) ->
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is String -> putString(key, value)
                else -> putString(key, value.toString())
            }
        }
        apply()
    }
}

// Load configuration
fun loadConfiguration(): ServiceConfiguration {
    val prefs = getSharedPreferences("vos_config", Context.MODE_PRIVATE)
    val configMap = mutableMapOf<String, Any>()
    
    prefs.all.forEach { (key, value) ->
        configMap[key] = value
    }
    
    return ServiceConfiguration.fromMap(configMap)
}
```

### Performance Monitoring

#### Implementing Custom Metrics
```kotlin
class PerformanceMonitor {
    private val metrics = mutableMapOf<String, MutableList<Long>>()
    
    fun trackCommand(command: String, executionTime: Long) {
        metrics.getOrPut(command) { mutableListOf() }.add(executionTime)
    }
    
    fun getStatistics(command: String): CommandStats? {
        val times = metrics[command] ?: return null
        
        return CommandStats(
            command = command,
            count = times.size,
            avgTime = times.average(),
            minTime = times.minOrNull() ?: 0,
            maxTime = times.maxOrNull() ?: 0,
            p50 = percentile(times, 50),
            p95 = percentile(times, 95),
            p99 = percentile(times, 99)
        )
    }
    
    private fun percentile(times: List<Long>, p: Int): Long {
        val sorted = times.sorted()
        val index = (sorted.size * p / 100).coerceIn(0, sorted.size - 1)
        return sorted[index]
    }
}

data class CommandStats(
    val command: String,
    val count: Int,
    val avgTime: Double,
    val minTime: Long,
    val maxTime: Long,
    val p50: Long,
    val p95: Long,
    val p99: Long
)
```

### Error Handling & Recovery

#### Robust Command Execution
```kotlin
class RobustCommandExecutor {
    private val maxRetries = 3
    private val retryDelay = 500L // milliseconds
    
    suspend fun executeWithRetry(
        command: String,
        params: Map<String, Any> = emptyMap()
    ): CommandResult {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                // Try to execute
                val success = VoiceAccessibility.executeCommand(command, params)
                
                if (success) {
                    return CommandResult.Success(
                        command = command,
                        attempts = attempt + 1
                    )
                }
                
                // If failed but no exception, delay and retry
                if (attempt < maxRetries - 1) {
                    delay(retryDelay * (attempt + 1))
                }
                
            } catch (e: Exception) {
                lastException = e
                Log.e("Executor", "Attempt ${attempt + 1} failed", e)
                
                if (attempt < maxRetries - 1) {
                    delay(retryDelay * (attempt + 1))
                }
            }
        }
        
        return CommandResult.Failure(
            command = command,
            reason = lastException?.message ?: "Unknown error",
            attempts = maxRetries
        )
    }
}

sealed class CommandResult {
    data class Success(
        val command: String,
        val attempts: Int
    ) : CommandResult()
    
    data class Failure(
        val command: String,
        val reason: String,
        val attempts: Int
    ) : CommandResult()
}
```

---

## Advanced Development (Expert)

### Custom Command Router

#### Implementing Intelligent Routing
```kotlin
class IntelligentRouter {
    private val routingTable = mutableMapOf<Pattern, HandlerInfo>()
    private val cache = LRUCache<String, HandlerInfo>(100)
    
    init {
        // Register patterns
        registerPattern("^open\\s+(.+)$", AppHandler::class, Priority.HIGH)
        registerPattern("^click\\s+(.+)$", UIHandler::class, Priority.NORMAL)
        registerPattern("^type\\s+(.+)$", InputHandler::class, Priority.NORMAL)
        registerPattern("^scroll\\s+(up|down|left|right)$", NavigationHandler::class, Priority.HIGH)
    }
    
    fun route(command: String): HandlerInfo? {
        // Check cache first
        cache.get(command)?.let { return it }
        
        // Find matching pattern
        val matches = routingTable.entries
            .filter { (pattern, _) -> pattern.matcher(command).matches() }
            .sortedByDescending { it.value.priority.value }
        
        val result = matches.firstOrNull()?.value
        
        // Cache result
        result?.let { cache.put(command, it) }
        
        return result
    }
    
    private fun registerPattern(
        regex: String,
        handlerClass: KClass<out ActionHandler>,
        priority: Priority
    ) {
        routingTable[Pattern.compile(regex)] = HandlerInfo(handlerClass, priority)
    }
}

data class HandlerInfo(
    val handlerClass: KClass<out ActionHandler>,
    val priority: Priority
)

enum class Priority(val value: Int) {
    LOW(1), NORMAL(5), HIGH(10), CRITICAL(100)
}
```

### Memory-Efficient Node Processing

#### Streaming Node Processor
```kotlin
class StreamingNodeProcessor {
    
    fun processNodesStreaming(
        root: AccessibilityNodeInfo,
        processor: (AccessibilityNodeInfo) -> Boolean
    ) {
        val queue = LinkedList<AccessibilityNodeInfo>()
        queue.offer(root)
        
        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: continue
            
            // Process node
            val shouldContinue = processor(node)
            
            if (!shouldContinue) {
                // Early termination
                return
            }
            
            // Add children to queue
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.offer(it) }
            }
        }
    }
    
    fun findNodesMatching(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Flow<AccessibilityNodeInfo> = flow {
        processNodesStreaming(root) { node ->
            if (predicate(node)) {
                emit(node)
            }
            true // Continue processing
        }
    }
    
    // Usage example
    suspend fun findAllButtons(): List<AccessibilityNodeInfo> {
        val root = VoiceAccessibility.getRootNode() ?: return emptyList()
        
        return findNodesMatching(root) { node ->
            node.className == "android.widget.Button"
        }.toList()
    }
}
```

### Performance Optimization Techniques

#### Command Batching
```kotlin
class CommandBatcher {
    private val batchQueue = mutableListOf<BatchedCommand>()
    private val batchLock = Mutex()
    private val batchSize = 10
    private val batchTimeout = 100L // milliseconds
    
    data class BatchedCommand(
        val command: String,
        val params: Map<String, Any>,
        val callback: (Boolean) -> Unit
    )
    
    suspend fun addCommand(
        command: String,
        params: Map<String, Any> = emptyMap(),
        callback: (Boolean) -> Unit = {}
    ) {
        batchLock.withLock {
            batchQueue.add(BatchedCommand(command, params, callback))
            
            if (batchQueue.size >= batchSize) {
                processBatch()
            } else {
                // Schedule batch processing
                GlobalScope.launch {
                    delay(batchTimeout)
                    processBatch()
                }
            }
        }
    }
    
    private suspend fun processBatch() {
        batchLock.withLock {
            if (batchQueue.isEmpty()) return
            
            val batch = batchQueue.toList()
            batchQueue.clear()
            
            // Process in parallel
            coroutineScope {
                batch.map { cmd ->
                    async(Dispatchers.Default) {
                        val result = VoiceAccessibility.executeCommand(
                            cmd.command,
                            cmd.params
                        )
                        cmd.callback(result)
                        result
                    }
                }.awaitAll()
            }
        }
    }
}
```

### Advanced UI Analysis

#### Semantic UI Understanding
```kotlin
class SemanticUIAnalyzer {
    private val nlpProcessor = NLPProcessor()
    private val visualAnalyzer = VisualAnalyzer()
    
    fun analyzeUI(): UISemantics {
        val root = VoiceAccessibility.getRootNode() ?: return UISemantics.Empty
        
        val elements = extractElements(root)
        val layout = analyzeLayout(elements)
        val context = determineContext(elements)
        val actions = generatePossibleActions(elements, context)
        
        return UISemantics(
            elements = elements,
            layout = layout,
            context = context,
            possibleActions = actions
        )
    }
    
    private fun extractElements(root: AccessibilityNodeInfo): List<UIElement> {
        val elements = mutableListOf<UIElement>()
        
        processNode(root) { node ->
            elements.add(
                UIElement(
                    id = node.viewIdResourceName,
                    className = node.className?.toString(),
                    text = node.text?.toString(),
                    description = node.contentDescription?.toString(),
                    bounds = node.getBounds(),
                    isClickable = node.isClickable,
                    isScrollable = node.isScrollable,
                    isEditable = node.isEditable
                )
            )
        }
        
        return elements
    }
    
    private fun analyzeLayout(elements: List<UIElement>): LayoutInfo {
        // Analyze spatial relationships
        val clusters = clusterElements(elements)
        val hierarchy = buildHierarchy(elements)
        val focusFlow = determineFocusFlow(elements)
        
        return LayoutInfo(clusters, hierarchy, focusFlow)
    }
    
    private fun determineContext(elements: List<UIElement>): AppContext {
        // Use NLP to understand app context
        val texts = elements.mapNotNull { it.text }
        val descriptions = elements.mapNotNull { it.description }
        
        val keywords = nlpProcessor.extractKeywords(texts + descriptions)
        val appType = nlpProcessor.classifyAppType(keywords)
        val currentScreen = nlpProcessor.identifyScreen(elements)
        
        return AppContext(appType, currentScreen, keywords)
    }
}

data class UISemantics(
    val elements: List<UIElement>,
    val layout: LayoutInfo,
    val context: AppContext,
    val possibleActions: List<SemanticAction>
) {
    companion object {
        val Empty = UISemantics(emptyList(), LayoutInfo.Empty, AppContext.Unknown, emptyList())
    }
}
```

---

## Research & Innovation (PhD)

### Machine Learning Integration

#### Predictive Command System
```kotlin
class MLCommandPredictor {
    private val model = TensorFlowLiteModel("command_predictor.tflite")
    private val tokenizer = CommandTokenizer()
    private val historyWindow = 10
    
    fun predictNextCommand(
        commandHistory: List<String>,
        uiContext: UIContext
    ): List<PredictedCommand> {
        // Prepare input features
        val historyFeatures = extractHistoryFeatures(commandHistory)
        val contextFeatures = extractContextFeatures(uiContext)
        val timeFeatures = extractTimeFeatures()
        
        // Combine features
        val inputTensor = combineFeatures(
            historyFeatures,
            contextFeatures,
            timeFeatures
        )
        
        // Run inference
        val output = model.predict(inputTensor)
        
        // Convert output to predictions
        return output.topK(5).map { prediction ->
            PredictedCommand(
                command = tokenizer.decode(prediction.tokenIds),
                confidence = prediction.confidence,
                reasoning = explainPrediction(prediction)
            )
        }
    }
    
    private fun extractHistoryFeatures(history: List<String>): FloatArray {
        // N-gram analysis
        val unigrams = extractUnigrams(history)
        val bigrams = extractBigrams(history)
        val trigrams = extractTrigrams(history)
        
        // Command patterns
        val patterns = identifyPatterns(history)
        
        // Temporal patterns
        val temporal = analyzeTemporalPatterns(history)
        
        return concatenate(unigrams, bigrams, trigrams, patterns, temporal)
    }
    
    private fun explainPrediction(prediction: Prediction): String {
        // Generate human-readable explanation
        val attention = prediction.attentionWeights
        val importantFeatures = attention.getTopFeatures(3)
        
        return buildString {
            append("Predicted based on: ")
            importantFeatures.forEach { feature ->
                append("${feature.name} (${feature.importance}%), ")
            }
        }
    }
}
```

### Novel Interaction Patterns

#### Gesture-Based Command System
```kotlin
class GestureCommandSystem {
    private val gestureRecognizer = GestureRecognizer()
    private val commandMapper = GestureToCommandMapper()
    
    fun processGesture(path: Path): GestureCommand? {
        // Recognize gesture
        val gesture = gestureRecognizer.recognize(path)
        
        // Map to command
        val command = commandMapper.map(gesture)
        
        // Validate in current context
        if (!isValidInContext(command)) {
            return null
        }
        
        return GestureCommand(
            gesture = gesture,
            command = command,
            confidence = gesture.confidence
        )
    }
    
    class GestureRecognizer {
        private val model = loadModel("gesture_model.tflite")
        
        fun recognize(path: Path): Gesture {
            // Extract features from path
            val features = extractPathFeatures(path)
            
            // Classify gesture
            val prediction = model.predict(features)
            
            return Gesture(
                type = prediction.type,
                confidence = prediction.confidence,
                path = path
            )
        }
        
        private fun extractPathFeatures(path: Path): Features {
            return Features(
                length = path.length,
                direction = path.dominantDirection,
                curvature = path.curvature,
                velocity = path.averageVelocity,
                acceleration = path.acceleration,
                corners = path.cornerCount,
                loops = path.loopCount
            )
        }
    }
}
```

### Accessibility Research Topics

#### Cognitive Load Optimization
```kotlin
class CognitiveLoadOptimizer {
    private val userModel = UserCognitiveModel()
    private val taskComplexityAnalyzer = TaskComplexityAnalyzer()
    
    fun optimizeCommandSequence(
        tasks: List<Task>,
        userProfile: UserProfile
    ): OptimizedSequence {
        // Analyze task complexity
        val complexities = tasks.map { task ->
            taskComplexityAnalyzer.analyze(task)
        }
        
        // Model user cognitive state
        val cognitiveState = userModel.getCurrentState(userProfile)
        
        // Optimize sequence
        val optimizedOrder = optimizeOrder(tasks, complexities, cognitiveState)
        
        // Insert breaks if needed
        val withBreaks = insertCognitiveBreaks(optimizedOrder, cognitiveState)
        
        // Generate guidance
        val guidance = generateAdaptiveGuidance(withBreaks, userProfile)
        
        return OptimizedSequence(
            tasks = withBreaks,
            estimatedCognitiveLoad = calculateTotalLoad(withBreaks),
            guidance = guidance
        )
    }
    
    private fun optimizeOrder(
        tasks: List<Task>,
        complexities: List<Complexity>,
        state: CognitiveState
    ): List<Task> {
        // Use dynamic programming to find optimal order
        val dp = Array(tasks.size) { IntArray(state.capacity + 1) }
        
        // ... DP implementation
        
        return reorderTasks(tasks, dp)
    }
}
```

### Performance Research

#### Quantum-Inspired Optimization
```kotlin
class QuantumInspiredOptimizer {
    private val quantumSimulator = QuantumSimulator()
    
    fun optimizeHandlerSelection(
        command: String,
        handlers: List<ActionHandler>
    ): ActionHandler? {
        // Create quantum state
        val qubits = handlers.size.toBitCount()
        val quantumState = quantumSimulator.createSuperposition(qubits)
        
        // Apply quantum gates based on command features
        applyCommandGates(quantumState, command)
        
        // Measure to collapse to best handler
        val measurement = quantumSimulator.measure(quantumState)
        
        return handlers.getOrNull(measurement)
    }
    
    private fun applyCommandGates(state: QuantumState, command: String) {
        // Apply Hadamard gates for superposition
        state.applyHadamard()
        
        // Apply rotation gates based on command similarity
        command.forEach { char ->
            val angle = char.toInt() * Math.PI / 256
            state.applyRotation(angle)
        }
        
        // Apply entanglement for correlation
        state.applyCNOT()
    }
}
```

---

## Best Practices Summary

### For All Levels

1. **Always Check Service Status**
   ```kotlin
   if (VoiceAccessibility.isServiceEnabled()) {
       // Safe to use
   }
   ```

2. **Handle Errors Gracefully**
   ```kotlin
   try {
       val result = VoiceAccessibility.executeCommand(cmd)
       if (!result) {
           // Handle failure
       }
   } catch (e: Exception) {
       Log.e(TAG, "Command failed", e)
   }
   ```

3. **Use Appropriate Performance Mode**
   ```kotlin
   // For battery-sensitive apps
   config.performanceMode = PerformanceMode.POWER_SAVER
   
   // For responsive apps
   config.performanceMode = PerformanceMode.HIGH
   ```

4. **Document Your Code**
   ```kotlin
   /**
    * Executes a voice command with retry logic
    * @param command The command to execute
    * @param retries Number of retry attempts
    * @return true if successful
    */
   fun executeWithRetry(command: String, retries: Int = 3): Boolean
   ```

5. **Test Thoroughly**
   - Unit tests for logic
   - Integration tests for service
   - UI tests for accessibility
   - Performance benchmarks

---

## Resources & Further Reading

### Documentation
- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [VOS4 Standards](/Agent-Instructions/MASTER-STANDARDS.md)
- [Module Architecture](README.md)

### Research Papers
- "Optimizing Accessibility Service Performance" - IEEE 2024
- "Machine Learning for Adaptive UI Interaction" - ACM CHI 2023
- "Cognitive Load in Voice-Controlled Interfaces" - ASSETS 2024

### Community
- VOS4 Developer Forum
- Android Accessibility Developers
- Stack Overflow: [android-accessibility]

---

*This guide is designed to grow with you as you develop from beginner to expert, providing appropriate depth at each level of your journey.*
---

## Chapter 7: VoiceOS Core Libraries (KMP)

### Overview

VoiceOS provides comprehensive Kotlin Multiplatform (KMP) libraries that work across Android, iOS, JVM, and JavaScript. These libraries were systematically extracted from VoiceOSCore modules to eliminate code duplication across all VoiceOS projects (VoiceAvanue, NewAvanue, MagicUI, AVA AI, BrowserAvanue, AVAConnect).

**Key Benefits:**
- ✅ **Cross-platform:** Works on Android, iOS, JVM, JS
- ✅ **Type-safe:** Compile-time error checking
- ✅ **Zero overhead:** No runtime performance cost
- ✅ **Well-tested:** 400+ test cases across all libraries
- ✅ **Production-ready:** Used in all VoiceOS apps
- ✅ **Zero dependencies:** Core libraries have no external dependencies

### Complete Library Catalog (10 Libraries)

For comprehensive documentation of all 10 KMP libraries, see:
**[📖 DEVELOPER_MANUAL_KMP.md](../docs/DEVELOPER_MANUAL_KMP.md)**

The manual includes:
- Complete API references for all 10 libraries
- Migration guides from old code
- Platform compatibility matrix
- Usage examples and best practices
- Build configuration templates
- Testing strategies

### Quick Reference - All 10 Libraries

1. **voiceos-types** (216 LOC) - Core data types and enums
2. **voiceos-uuid** (142 LOC) - UUID generation and validation
3. **voiceos-permissions** (143 LOC) - Permission management
4. **voiceos-sql-utils** (120 LOC) - SQL injection prevention
5. **voiceos-intent-utils** (127 LOC) - Intent creation utilities
6. **voiceos-command-models** (175 LOC) - Voice command models
7. **voiceos-accessibility-types** (226 LOC) - Accessibility structures
8. **text-utils** (215 LOC) - Text sanitization and manipulation
9. **voiceos-logging** (485 LOC) - Logging with PII redaction
10. **json-utils** (275 LOC) - JSON utilities

#### 1. voiceos-types - Core Data Types and Enums

Essential enums and types used throughout VoiceOS:

```kotlin
import com.augmentalis.voiceos.types.*

// Component classification
val component = ComponentType.BUTTON
val input = InputType.PASSWORD
val action = AccessibilityAction.CLICK

// Voice commands
val command = VoiceCommandType.NAVIGATE
val state = ElementState.ENABLED
```

**Includes:** ComponentType (20 types), InputType (15 types), AccessibilityAction (25 types), InteractionType (12 types), NavigationDirection (8 types), ElementState (10 states), VoiceCommandType (8 types), FilterType (6 types)

#### 2. voiceos-uuid - UUID Generation

Cross-platform UUID generation and validation:

```kotlin
import com.augmentalis.voiceos.uuid.*

// Generate random UUID
val uuid = UUIDGenerator.generateUUID()

// Deterministic UUID from seed
val seededUuid = UUIDGenerator.generateFromSeed("user-123")

// Validate UUID string
val isValid = UUIDValidator.isValid("550e8400-e29b-41d4-a716-446655440000")
```

#### 3. voiceos-permissions - Permission Management

Platform-aware permission handling:

```kotlin
import com.augmentalis.voiceos.permissions.*

// Check permission state
val state = PermissionChecker.checkPermission(PermissionType.CAMERA)

// Request permissions
val request = PermissionRequest(
    permissions = listOf(PermissionType.CAMERA, PermissionType.MICROPHONE),
    rationale = "Required for video calls"
)
```

#### 4. voiceos-sql-utils - SQL Injection Prevention

Safe SQL query building and escaping:

```kotlin
import com.augmentalis.voiceos.sql.SqlEscapeUtils

// Escape LIKE pattern
val safePattern = SqlEscapeUtils.escapeLike("user_input%")
// Result: "user\\_input\\%"

// Safe query building
val query = SqlQueryBuilder()
    .select("id", "name")
    .from("users")
    .where("email LIKE ?", SqlEscapeUtils.escapeLike(userInput))
    .build()
```

#### 5. voiceos-intent-utils - Intent Utilities

Android intent creation helpers:

```kotlin
import com.augmentalis.voiceos.intent.*

// Build safe intent
val intent = IntentBuilder()
    .action(IntentAction.VIEW)
    .category(IntentCategory.DEFAULT)
    .data("https://example.com")
    .flag(IntentFlag.ACTIVITY_NEW_TASK)
    .build()
```

#### 6. voiceos-command-models - Voice Command Models

Data models for voice commands:

```kotlin
import com.augmentalis.voiceos.command.*

// Define voice command
val command = VoiceCommand(
    id = UUIDGenerator.generateUUID(),
    phrase = "open settings",
    action = VoiceCommandType.NAVIGATE,
    parameters = mapOf("target" to "settings_screen"),
    confidence = 0.95f
)
```

#### 7. voiceos-accessibility-types - Accessibility Types

Accessibility-related data structures:

```kotlin
import com.augmentalis.voiceos.accessibility.*

// Create accessibility node
val node = AccessibilityNodeInfo(
    id = nodeId,
    role = AccessibilityRole.BUTTON,
    text = "Submit",
    bounds = AccessibilityBounds(0, 0, 100, 50),
    isClickable = true
)
```

#### 8. text-utils - Text Manipulation

XSS prevention and text utilities:

```kotlin
import com.augmentalis.voiceos.text.*

// Sanitize for web
val safeHtml = TextSanitizers.sanitizeHtml(userInput)
val safeXPath = TextSanitizers.sanitizeXPath(xpathExpression)

// Text manipulation
val truncated = TextUtils.truncate(longText, 100, "...")
val camelCase = TextUtils.toCamelCase("hello_world")
```

#### 9. voiceos-logging - Logging with PII Redaction

Automatic PII detection and redaction in logs:

```kotlin
import com.augmentalis.voiceos.logging.*

// Get logger instance
val logger = LoggerFactory.getLogger("MyComponent")

// PII automatically redacted
logger.d { "Processing user: ${user.email}" }
// Output: "Processing user: [REDACTED-EMAIL]"

// Backward compatible wrapper
PIILoggingWrapper.d("TAG", "SSN: 123-45-6789")
// Output: "SSN: [REDACTED-SSN]"
```

**Redacts:** Emails, phone numbers, SSN, credit cards, IP addresses, URLs, dates, names, addresses, financial data

#### 10. json-utils - JSON Utilities

JSON manipulation without external dependencies:

```kotlin
import com.augmentalis.voiceos.json.*

// Create JSON objects
val json = JsonUtils.createJsonObject(
    "name" to "John Doe",
    "age" to 30,
    "active" to true
)

// Specialized converters
val bounds = JsonConverters.boundsToJson(0, 0, 100, 200)
val point = JsonConverters.pointToJson(50, 75)
```

---

### Integration Guide

#### Step 1: Add Dependencies

For local development, use project dependencies:

```kotlin
// settings.gradle.kts
include(":libraries:core:voiceos-types")
include(":libraries:core:voiceos-uuid")
// ... include other libraries as needed

// build.gradle.kts
dependencies {
    implementation(project(":libraries:core:voiceos-types"))
    implementation(project(":libraries:core:voiceos-logging"))
    // Add others as needed
}
```

#### Step 2: Import and Use

```kotlin
import com.augmentalis.voiceos.types.*
import com.augmentalis.voiceos.uuid.UUIDGenerator
import com.augmentalis.voiceos.logging.LoggerFactory
// Import other libraries as needed

// Use immediately
val uuid = UUIDGenerator.generateUUID()
val logger = LoggerFactory.getLogger("MyComponent")
```

#### Step 3: Migration from Old Code

```kotlin
// Old: VoiceOSCore internal utilities
import com.augmentalis.voiceoscore.utils.PIIRedactionHelper

// New: KMP library (just change import)
import com.augmentalis.voiceos.logging.PIIRedactionHelper

// Usage stays the same - 100% backward compatible!
```

### Best Practices

1. **Use Type-Safe Enums** - Replace magic strings with enums from voiceos-types
2. **Always Escape SQL** - Use SqlEscapeUtils for all LIKE patterns
3. **Automatic PII Redaction** - Use PIILoggingWrapper for sensitive data
4. **Platform Agnostic Code** - Write in commonMain when possible
5. **Lazy Logging** - Use lambda expressions to avoid string concatenation

### Testing

All libraries include comprehensive tests:

```bash
# Run all library tests
./gradlew :libraries:core:test

# Run specific library tests
./gradlew :libraries:core:voiceos-logging:test
```

### More Information

For complete documentation including:
- Full API references for all 10 libraries
- Detailed migration guides
- Platform compatibility details
- Advanced usage examples
- Troubleshooting guides

See: **[📖 DEVELOPER_MANUAL_KMP.md](../docs/DEVELOPER_MANUAL_KMP.md)**

---

**Chapter Summary:**

You've learned how to use VoiceOS's comprehensive Kotlin Multiplatform libraries suite with 10 production-ready libraries totaling 2,124 lines of cross-platform code. These libraries provide type-safe utilities for all VoiceOS projects with zero external dependencies and 100% backward compatibility.

**Next Steps:**
1. Review the complete [DEVELOPER_MANUAL_KMP.md](../docs/DEVELOPER_MANUAL_KMP.md) for detailed API documentation
2. Start migrating your code to use the KMP libraries
3. Leverage the cross-platform capabilities for iOS and Web support

---

*End of VoiceOS Accessibility Developer Guide*

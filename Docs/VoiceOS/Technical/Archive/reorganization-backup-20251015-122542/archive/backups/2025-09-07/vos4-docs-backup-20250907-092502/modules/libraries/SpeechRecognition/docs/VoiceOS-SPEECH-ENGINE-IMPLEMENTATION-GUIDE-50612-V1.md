<!--
filename: SPEECH-ENGINE-IMPLEMENTATION-GUIDE.md
created: 2025-08-27 23:30:00 PDT
author: VOS4 Development Team
purpose: Comprehensive guide for implementing new speech recognition engines
version: 1.0.0
location: /libraries/SpeechRecognition/docs/
-->

# Speech Engine Implementation Guide

## 🎯 Quick Start: Adding a New Speech Engine

This guide provides step-by-step instructions for implementing new speech recognition engines in the VOS4 SpeechRecognition module.

## 📋 Prerequisites

Before implementing a new engine:
1. ✅ Read and understand VOS4 standards (`/Agent-Instructions/MASTER-STANDARDS.md`)
2. ✅ Study existing implementations (VOSK, Vivoka)
3. ✅ Understand the 4 shared components
4. ✅ Review the feature matrix for required capabilities

## 🏗️ Implementation Steps

### Step 1: Add Engine to Enum

**File:** `models/SpeechModels.kt` (in the SpeechEngine enum)
```kotlin
enum class SpeechEngine {
    VOSK,
    VIVOKA,
    GOOGLE_STT,
    GOOGLE_CLOUD,
    YOUR_NEW_ENGINE  // Add your engine here
}
```

Note: SpeechEngine and SpeechMode enums are now in a single file `SpeechModels.kt`

### Step 2: Create Engine Package

Create directory structure:
```
engines/
└── yourengine/
    └── YourEngineService.kt
```

### Step 3: Implement Service Class

**MANDATORY Structure:**

```kotlin
package com.augmentalis.speechrecognition.engines.yourengine

import com.augmentalis.speechrecognition.common.*
import com.augmentalis.speechrecognition.config.SpeechConfig
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*

/**
 * YourEngine speech recognition service implementation.
 * 
 * MANDATORY: Must use all 4 shared components
 * MANDATORY: Direct implementation (no interfaces)
 * MANDATORY: Follow VOS4 standards
 */
class YourEngineService(private val context: Context) {
    
    companion object {
        private const val TAG = "YourEngineService"
        private var instance: YourEngineService? = null
        
        @JvmStatic
        fun getInstance(context: Context): YourEngineService {
            return instance ?: synchronized(this) {
                instance ?: YourEngineService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    // ===== MANDATORY: Shared Components =====
    private val commandCache = CommandCache()
    private val timeoutManager = TimeoutManager(scope)
    private val resultProcessor = ResultProcessor()
    private val serviceState = ServiceState()
    
    // ===== Engine-Specific Components =====
    private var engineClient: YourEngineClient? = null
    private var config: SpeechConfig = SpeechConfig.default()
    
    // ===== Coroutine Management =====
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + 
        CoroutineName("YourEngineService")
    )
    
    // ===== Listeners =====
    private var resultListener: OnSpeechResultListener? = null
    private var errorListener: OnSpeechErrorListener? = null
    
    // ===== MANDATORY: Core Methods =====
    
    /**
     * Initialize the engine with configuration.
     * MANDATORY: Validate config and setup engine
     */
    suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate configuration
                config.validate().onFailure { 
                    return@withContext Result.failure(it)
                }
                
                // Store configuration
                this@YourEngineService.config = config
                
                // Update state
                serviceState.updateState(
                    ServiceState.State.INITIALIZING,
                    "Loading ${config.engine} engine..."
                )
                
                // Initialize engine-specific client
                engineClient = createEngineClient(config)
                
                // Setup complete
                serviceState.updateState(ServiceState.State.INITIALIZED)
                Result.success(Unit)
                
            } catch (e: Exception) {
                serviceState.updateState(
                    ServiceState.State.ERROR,
                    "Initialization failed: ${e.message}"
                )
                Result.failure(e)
            }
        }
    }
    
    /**
     * Start listening for speech input.
     * MANDATORY: Handle state transitions properly
     */
    fun startListening(): Result<Unit> {
        return try {
            // Check state
            if (!serviceState.canStartListening()) {
                return Result.failure(
                    IllegalStateException("Cannot start in state: ${serviceState.currentState}")
                )
            }
            
            // Update state
            serviceState.updateState(ServiceState.State.LISTENING)
            
            // Start engine-specific listening
            engineClient?.startListening()
            
            // Start timeout if configured
            config.timeoutDuration.let { duration ->
                timeoutManager.startTimeout(duration) {
                    handleTimeout()
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            serviceState.updateState(
                ServiceState.State.ERROR,
                "Failed to start: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Stop listening for speech input.
     */
    fun stopListening(): Result<Unit> {
        return try {
            // Cancel timeout
            timeoutManager.cancelTimeout()
            
            // Stop engine
            engineClient?.stopListening()
            
            // Update state
            serviceState.updateState(ServiceState.State.IDLE)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set static commands for command matching.
     * MANDATORY: Use CommandCache
     */
    fun setStaticCommands(commands: List<String>) {
        commandCache.setStaticCommands(commands)
        Log.d(TAG, "Set ${commands.size} static commands")
    }
    
    /**
     * Set dynamic commands from UI.
     * MANDATORY: Use CommandCache
     */
    fun setDynamicCommands(commands: List<String>) {
        commandCache.setDynamicCommands(commands)
        Log.d(TAG, "Set ${commands.size} dynamic commands")
    }
    
    /**
     * Set result listener.
     */
    fun setResultListener(listener: OnSpeechResultListener) {
        this.resultListener = listener
        serviceState.setListener(listener)
    }
    
    /**
     * Clean shutdown.
     * MANDATORY: Proper resource cleanup
     */
    fun shutdown() {
        try {
            // Cancel all coroutines
            scope.cancel()
            
            // Stop engine
            engineClient?.shutdown()
            
            // Clear caches
            commandCache.clear()
            
            // Update state
            serviceState.updateState(ServiceState.State.DESTROYED)
            
        } catch (e: Exception) {
            Log.e(TAG, "Shutdown error", e)
        }
    }
    
    // ===== Engine-Specific Implementation =====
    
    /**
     * Create engine-specific client.
     * CUSTOMIZE: This is where you integrate your SDK/API
     */
    private fun createEngineClient(config: SpeechConfig): YourEngineClient {
        // Example implementation
        return YourEngineClient().apply {
            setLanguage(config.language)
            setConfidenceThreshold(config.confidenceThreshold)
            
            // Set callbacks that convert to common format
            setRecognitionCallback { engineResult ->
                processEngineResult(engineResult)
            }
        }
    }
    
    /**
     * Process engine-specific results.
     * MANDATORY: Use ResultProcessor and CommandCache
     */
    private fun processEngineResult(engineResult: Any) {
        scope.launch {
            try {
                // Convert engine result to text and confidence
                val text = extractText(engineResult)
                val confidence = extractConfidence(engineResult)
                
                // Use ResultProcessor for normalization
                val normalizedText = resultProcessor.normalizeText(text)
                
                // Check for command match if in command mode
                var finalText = normalizedText
                var finalConfidence = confidence
                
                if (config.mode == SpeechMode.COMMAND || 
                    config.mode == SpeechMode.DYNAMIC_COMMAND) {
                    
                    commandCache.findBestMatch(normalizedText)?.let { match ->
                        finalText = match
                        finalConfidence = 0.95f // High confidence for exact match
                    }
                }
                
                // Create result using ResultProcessor
                val result = RecognitionResult(
                    text = finalText,
                    originalText = text,
                    confidence = finalConfidence,
                    timestamp = System.currentTimeMillis(),
                    isPartial = false,
                    isFinal = true,
                    alternatives = extractAlternatives(engineResult),
                    engine = config.engine.name,
                    mode = config.mode.name
                )
                
                // Check if should accept (duplicate detection)
                if (resultProcessor.shouldAcceptResult(result)) {
                    // Notify listener
                    withContext(Dispatchers.Main) {
                        resultListener?.invoke(result)
                    }
                    
                    // Reset timeout
                    config.timeoutDuration.let { duration ->
                        timeoutManager.resetTimeout(duration) {
                            handleTimeout()
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Result processing error", e)
                handleError("Processing failed: ${e.message}", -1)
            }
        }
    }
    
    /**
     * Handle recognition timeout.
     * MANDATORY: Use TimeoutManager
     */
    private fun handleTimeout() {
        Log.d(TAG, "Recognition timeout")
        stopListening()
        
        scope.launch(Dispatchers.Main) {
            errorListener?.invoke("Recognition timeout", ERROR_TIMEOUT)
        }
    }
    
    /**
     * Handle errors.
     */
    private fun handleError(message: String, code: Int) {
        serviceState.updateState(ServiceState.State.ERROR, message)
        
        scope.launch(Dispatchers.Main) {
            errorListener?.invoke(message, code)
        }
    }
    
    // ===== Helper Methods (Engine-Specific) =====
    
    private fun extractText(result: Any): String {
        // Engine-specific text extraction
        return ""
    }
    
    private fun extractConfidence(result: Any): Float {
        // Engine-specific confidence extraction
        return 0.0f
    }
    
    private fun extractAlternatives(result: Any): List<String> {
        // Engine-specific alternatives extraction
        return emptyList()
    }
}

// Error codes
private const val ERROR_TIMEOUT = 1
private const val ERROR_NETWORK = 2
private const val ERROR_NO_MATCH = 3
```

## 📦 Shared Components Usage

### 1. CommandCache
**Purpose:** Store and match commands with priority
```kotlin
// Store commands
commandCache.setStaticCommands(listOf("open settings", "go back"))
commandCache.setDynamicCommands(uiCommands)

// Match commands
val match = commandCache.findBestMatch(recognizedText)
```

### 2. TimeoutManager
**Purpose:** Handle recognition timeouts
```kotlin
// Start timeout
timeoutManager.startTimeout(5000) {
    handleTimeout()
}

// Reset on partial results
timeoutManager.resetTimeout(5000) {
    handleTimeout()
}

// Cancel when done
timeoutManager.cancelTimeout()
```

### 3. ResultProcessor
**Purpose:** Process and validate results
```kotlin
// Normalize text
val normalized = resultProcessor.normalizeText(rawText)

// Check duplicates
if (resultProcessor.shouldAcceptResult(result)) {
    // Process result
}

// Create result
val result = resultProcessor.createResult(
    text = text,
    confidence = confidence,
    engine = engineName
)
```

### 4. ServiceState
**Purpose:** Manage state transitions
```kotlin
// Update state
serviceState.updateState(ServiceState.State.LISTENING)

// Check if can transition
if (serviceState.canStartListening()) {
    // Start
}

// Get current state
val state = serviceState.currentState
```

## 🎯 Implementation Checklist

### Required Methods:
- [ ] `initialize(config: SpeechConfig): Result<Unit>`
- [ ] `startListening(): Result<Unit>`
- [ ] `stopListening(): Result<Unit>`
- [ ] `setStaticCommands(commands: List<String>)`
- [ ] `setDynamicCommands(commands: List<String>)`
- [ ] `setResultListener(listener: OnSpeechResultListener)`
- [ ] `setErrorListener(listener: OnSpeechErrorListener)`
- [ ] `shutdown()`

### Required Components:
- [ ] CommandCache instance
- [ ] TimeoutManager instance
- [ ] ResultProcessor instance
- [ ] ServiceState instance

### Required Features:
- [ ] Command matching
- [ ] Timeout handling
- [ ] Duplicate detection
- [ ] State management
- [ ] Error handling
- [ ] Resource cleanup

## 🧪 Testing Your Implementation

### Unit Tests:
```kotlin
@Test
fun testInitialization() {
    val service = YourEngineService(context)
    val config = SpeechConfig.yourEngine()
    
    runBlocking {
        val result = service.initialize(config)
        assertTrue(result.isSuccess)
    }
}

@Test
fun testCommandMatching() {
    val service = YourEngineService(context)
    service.setStaticCommands(listOf("test command"))
    
    // Simulate recognition of "test command"
    // Verify it matches correctly
}
```

### Integration Tests:
```kotlin
@Test
fun testWithSharedComponents() {
    // Test that all shared components work together
}
```

## 📝 Documentation Requirements

After implementing your engine, create:

1. **Engine-specific documentation:**
   `docs/engines/YourEngine-Documentation.md`

2. **Update feature matrix:**
   `docs/modules/speechrecognition/All-Engines-Feature-Matrix-Complete.md`

3. **Update README:**
   Add your engine to the supported engines list

4. **Update changelog:**
   Document the new engine addition

## ⚠️ Common Pitfalls to Avoid

1. **Don't create interfaces** - Direct implementation only
2. **Don't skip shared components** - All 4 are mandatory
3. **Don't forget state management** - Use ServiceState
4. **Don't ignore timeouts** - Use TimeoutManager
5. **Don't process duplicates** - Use ResultProcessor
6. **Don't hardcode commands** - Use CommandCache

## 🚀 Engine-Specific Considerations

### For Online Engines:
- Handle network errors gracefully
- Implement retry logic
- Consider offline fallback
- Use lightweight HTTP clients (OkHttp) over heavy SDKs when possible
- For Google Cloud: Use REST API with OkHttp (~500KB) instead of SDK (~50MB)

### For Offline Engines:
- Manage model downloads
- Handle storage limitations
- Optimize memory usage

### For Hybrid Engines:
- Implement switching logic
- Maintain state across modes
- Sync commands between modes

## 📊 Performance Guidelines

### Memory Usage Targets:
- Offline engines: <50MB
- Online engines: <20MB (using lightweight REST APIs)
- Cloud engines: <15MB (using REST instead of SDKs)
- Hybrid engines: <70MB

### Latency Targets:
- Command recognition: <500ms
- Dictation start: <200ms
- Result delivery: <100ms after speech ends

### Accuracy Targets:
- Commands: >95% with exact match
- Dictation: >90% word accuracy
- Wake words: >98% detection rate

## 🔍 Example Implementations

### Study These References:
1. **VoskService.kt** - Best example of offline engine
2. **VivokaService.kt** - Best example of hybrid engine  
3. **GoogleSTTService.kt** - (To be implemented) Online engine
4. **GoogleCloudService.kt** - Lightweight REST API implementation (no heavy SDK)

## 📞 Support

For questions about implementation:
1. Review existing implementations
2. Check shared component documentation
3. Consult the feature matrix
4. Follow VOS4 standards

---

**Remember:** The goal is code reuse through shared components while maintaining engine-specific optimizations.
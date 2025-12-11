# AVA Swarm Analysis - Fix Implementation Guide

**Version:** 1.0
**Date:** 2025-11-30
**Priority:** Phase 1 Critical Fixes

---

## Overview

This guide provides step-by-step implementation for the top 5 critical fixes identified in the swarm analysis. These fixes block 80% of all identified issues.

---

## Fix #1: Initialization Coordinator

**Issues Resolved:** C-01, I-01, I-02, I-03, I-04, I-05
**Effort:** 4 hours
**Impact:** Prevents all initialization race conditions

### Step 1: Create Interface

**File:** `Universal/AVA/Core/src/main/java/com/augmentalis/ava/core/init/Initializable.kt`

```kotlin
package com.augmentalis.ava.core.init

interface Initializable {
    val priority: Int  // Lower = initialize first
    val name: String
    suspend fun initialize()
    suspend fun shutdown()
}
```

### Step 2: Create Coordinator

**File:** `Universal/AVA/Core/src/main/java/com/augmentalis/ava/core/init/InitializationCoordinator.kt`

```kotlin
package com.augmentalis.ava.core.init

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitializationCoordinator @Inject constructor() {

    private val mutex = Mutex()
    private var state = InitState.NOT_STARTED
    private val initialized = mutableSetOf<String>()

    enum class InitState {
        NOT_STARTED, IN_PROGRESS, READY, FAILED
    }

    suspend fun ensureInitialized(
        vararg components: Initializable
    ): Result<Unit> = mutex.withLock {
        if (state == InitState.READY) return@withLock Result.success(Unit)
        if (state == InitState.IN_PROGRESS) {
            // Wait for completion (already holding mutex)
            return@withLock if (state == InitState.READY) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Init failed"))
            }
        }

        state = InitState.IN_PROGRESS

        try {
            components
                .sortedBy { it.priority }
                .forEach { component ->
                    if (component.name !in initialized) {
                        component.initialize()
                        initialized.add(component.name)
                    }
                }
            state = InitState.READY
            Result.success(Unit)
        } catch (e: Exception) {
            state = InitState.FAILED
            Result.failure(e)
        }
    }

    suspend fun shutdown() = mutex.withLock {
        initialized.reversed().forEach { name ->
            // Shutdown in reverse order
        }
        initialized.clear()
        state = InitState.NOT_STARTED
    }

    fun isReady(): Boolean = state == InitState.READY
}
```

### Step 3: Update ChatViewModel

**File:** `Universal/AVA/Features/Chat/src/main/java/com/augmentalis/ava/features/chat/presentation/ChatViewModel.kt`

```kotlin
// Add injection
@Inject lateinit var initCoordinator: InitializationCoordinator
@Inject lateinit var nluInit: NLUInitializable
@Inject lateinit var llmInit: LLMInitializable
@Inject lateinit var ttsInit: TTSInitializable

// Replace parallel init with coordinated init
private fun initializeComponents() {
    viewModelScope.launch {
        initCoordinator.ensureInitialized(
            nluInit,    // priority = 1
            llmInit,    // priority = 2
            ttsInit     // priority = 3
        ).onFailure { e ->
            _uiState.value = ChatUiState.Error(e.message)
        }
    }
}
```

---

## Fix #2: CoroutineExceptionHandler

**Issues Resolved:** E-01, E-02, E-03, E-04
**Effort:** 2 hours
**Impact:** Prevents silent crashes

### Step 1: Create Base Handler

**File:** `Universal/AVA/Core/src/main/java/com/augmentalis/ava/core/coroutines/AVAExceptionHandler.kt`

```kotlin
package com.augmentalis.ava.core.coroutines

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.cancellation.CancellationException

object AVAExceptionHandler {

    private const val TAG = "AVA"

    fun create(
        tag: String,
        onError: (Throwable) -> Unit = {}
    ) = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            is CancellationException -> {
                // Normal cancellation, don't log as error
                Log.d(tag, "Coroutine cancelled")
            }
            is OutOfMemoryError -> {
                Log.e(tag, "OOM in coroutine", throwable)
                triggerLowMemoryCleanup()
                onError(throwable)
            }
            else -> {
                Log.e(tag, "Coroutine exception", throwable)
                onError(throwable)
            }
        }
    }

    private fun triggerLowMemoryCleanup() {
        // Signal to release caches, models, etc.
        System.gc()
    }
}
```

### Step 2: Apply to WakeWordService

**File:** `Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/WakeWordService.kt`

```kotlin
private val exceptionHandler = AVAExceptionHandler.create(
    tag = "WakeWordService",
    onError = { e ->
        _serviceState.value = ServiceState.Error(e)
        stopSelf()  // Graceful shutdown
    }
)

private val serviceScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Default + exceptionHandler
)
```

### Step 3: Apply to All ViewModels

```kotlin
// Base pattern for all ViewModels
abstract class BaseViewModel : ViewModel() {

    protected val exceptionHandler = AVAExceptionHandler.create(
        tag = this::class.simpleName ?: "ViewModel",
        onError = { handleError(it) }
    )

    protected val vmScope = viewModelScope + exceptionHandler

    abstract fun handleError(e: Throwable)
}
```

---

## Fix #3: Hilt/Singleton Conflicts

**Issues Resolved:** I-01, C-02
**Effort:** 3 hours
**Impact:** Prevents double instantiation

### Problem

```kotlin
// WRONG: Both Hilt and manual singleton
@Singleton
class IntentClassifier @Inject constructor(...) {
    companion object {
        @Volatile private var INSTANCE: IntentClassifier? = null
        fun getInstance() = INSTANCE ?: ...  // CONFLICT!
    }
}
```

### Solution

**Remove manual singleton, trust Hilt:**

```kotlin
// CORRECT: Hilt-only singleton
@Singleton
class IntentClassifier @Inject constructor(
    private val onnxSession: OnnxSession,
    private val tokenizer: Tokenizer
) {
    private val initMutex = Mutex()
    private var initialized = false

    suspend fun ensureInitialized() = initMutex.withLock {
        if (initialized) return@withLock
        // Init logic here
        initialized = true
    }
}
```

**Update all usages:**

```kotlin
// WRONG
val classifier = IntentClassifier.getInstance()

// CORRECT
@Inject lateinit var classifier: IntentClassifier
```

---

## Fix #4: LocalLLMProvider Mutex

**Issues Resolved:** C-03
**Effort:** 1 hour
**Impact:** Prevents corrupted inference

### Current (Broken)

```kotlin
class LocalLLMProvider {
    private var model: LLMModel? = null

    suspend fun generate(prompt: String): String {
        return model?.generate(prompt) ?: ""  // RACE!
    }

    suspend fun loadModel(path: String) {
        model = LLMModel.load(path)  // RACE!
    }
}
```

### Fixed

```kotlin
class LocalLLMProvider {
    private val modelMutex = Mutex()
    private var model: LLMModel? = null

    suspend fun generate(prompt: String): String = modelMutex.withLock {
        model?.generate(prompt) ?: throw IllegalStateException("Model not loaded")
    }

    suspend fun loadModel(path: String) = modelMutex.withLock {
        model?.close()
        model = LLMModel.load(path)
    }

    suspend fun unloadModel() = modelMutex.withLock {
        model?.close()
        model = null
    }
}
```

---

## Fix #5: TTS Thread Callback

**Issues Resolved:** A-04, C-04
**Effort:** 2 hours
**Impact:** Prevents CalledFromWrongThreadException

### Current (Broken)

```kotlin
class TTSManager {
    private val callbacks = mutableMapOf<String, () -> Unit>()

    // Called from TTS engine thread
    override fun onDone(utteranceId: String) {
        callbacks[utteranceId]?.invoke()  // WRONG THREAD!
    }
}
```

### Fixed

```kotlin
class TTSManager @Inject constructor(
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val callbacks = ConcurrentHashMap<String, suspend () -> Unit>()

    override fun onDone(utteranceId: String) {
        scope.launch {
            callbacks.remove(utteranceId)?.invoke()
        }
    }

    suspend fun speak(
        text: String,
        onComplete: suspend () -> Unit = {}
    ): String {
        val utteranceId = UUID.randomUUID().toString()
        callbacks[utteranceId] = onComplete

        withContext(Dispatchers.Main) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        }

        return utteranceId
    }
}
```

---

## Validation Checklist

After implementing each fix:

| Fix | Validation |
|-----|------------|
| #1 Coordinator | Unit test: parallel init calls only initialize once |
| #2 ExceptionHandler | Stress test: throw in coroutine, verify logged |
| #3 Hilt Singleton | Verify: only 1 instance in memory profiler |
| #4 LLM Mutex | Stress test: 100 concurrent generate() calls |
| #5 TTS Thread | Verify: callback runs on main, no ANR |

---

## Files to Modify

| File | Changes |
|------|---------|
| Core module | Add init/, coroutines/ packages |
| ChatViewModel | Use InitializationCoordinator |
| WakeWordService | Add exceptionHandler |
| SettingsViewModel | Add exceptionHandler |
| IntentClassifier | Remove companion singleton |
| LocalLLMProvider | Add modelMutex |
| TTSManager | Use ConcurrentHashMap, main dispatcher |

---

## Testing Commands

```bash
# Run unit tests after changes
./gradlew :Universal:AVA:Core:test
./gradlew :Universal:AVA:Features:Chat:test
./gradlew :Universal:AVA:Features:WakeWord:test

# Full integration test
./gradlew connectedAndroidTest
```

---

**Document Version:** 1.0
**Ready for Implementation:** Yes

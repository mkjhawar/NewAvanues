/**
 * LiveDataFlowBridge.kt - Bridge utilities for LiveData to Flow conversion
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides utilities for converting Android LiveData to Kotlin Flow,
 * enabling the plugin system to work with existing VoiceOSCore repositories
 * that may use LiveData.
 */
package com.augmentalis.magiccode.plugins.android.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bridge utilities for converting Android LiveData to Kotlin Flow.
 *
 * This object provides extension functions and utilities for bridging
 * the gap between Android's LiveData (commonly used in existing VoiceOSCore
 * repositories) and Kotlin Flow (used by the plugin system).
 *
 * ## Why This Bridge?
 * - VoiceOSCore repositories may expose LiveData for reactive updates
 * - The plugin system uses Kotlin Flow for cross-platform compatibility
 * - LiveData has lifecycle awareness that needs proper handling
 * - Flow provides more operators and better coroutine integration
 *
 * ## Usage Example
 * ```kotlin
 * import com.augmentalis.magiccode.plugins.android.data.LiveDataFlowBridge.asFlow
 *
 * // Convert LiveData to Flow
 * val elementsFlow: Flow<List<Element>> = elementsLiveData.asFlow()
 *
 * // With default value
 * val commandsFlow = commandsLiveData.asFlowWithDefault(emptyList())
 *
 * // With debouncing
 * val debouncedFlow = liveData.asFlow().debounceUpdates(200)
 * ```
 *
 * ## Thread Safety
 * All conversions are thread-safe and properly handle LiveData's main
 * thread requirements for observer registration.
 *
 * @since 1.0.0
 */
object LiveDataFlowBridge {

    /**
     * Convert a LiveData to a cold Flow.
     *
     * Creates a Flow that observes the LiveData and emits values as they change.
     * The observation starts when the Flow is collected and stops when collection
     * is cancelled.
     *
     * ## Important Notes
     * - Uses `observeForever` internally (no lifecycle owner)
     * - The Flow is cold - observation only starts on collection
     * - Observer is removed when the Flow collection is cancelled
     *
     * ## Example
     * ```kotlin
     * val elementsLiveData: LiveData<List<Element>> = repository.getElements()
     * val elementsFlow: Flow<List<Element>> = elementsLiveData.asFlow()
     *
     * // Collect the flow
     * elementsFlow.collect { elements ->
     *     println("Received ${elements.size} elements")
     * }
     * ```
     *
     * @receiver The LiveData to convert
     * @return Flow that emits values from the LiveData
     */
    @JvmStatic
    fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {
        val observer = Observer<T> { value ->
            if (value != null && isActive) {
                trySend(value)
            }
        }

        // Observe on main thread as required by LiveData
        withContext(Dispatchers.Main.immediate) {
            observeForever(observer)
        }

        // Cleanup when collection is cancelled
        awaitClose {
            // Remove observer on main thread
            kotlinx.coroutines.MainScope().launch(Dispatchers.Main.immediate) {
                removeObserver(observer)
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Convert a nullable LiveData to a Flow with a default value.
     *
     * Similar to [asFlow] but handles null values by emitting a default value
     * instead. Useful when the LiveData might emit null or when you want
     * to guarantee non-null emissions.
     *
     * ## Example
     * ```kotlin
     * val nullableLiveData: LiveData<String?> = repository.getValue()
     * val flow: Flow<String> = nullableLiveData.asFlowWithDefault("default")
     * ```
     *
     * @receiver The nullable LiveData to convert
     * @param default Default value to emit when LiveData value is null
     * @return Flow that emits non-null values
     */
    @JvmStatic
    fun <T> LiveData<T?>.asFlowWithDefault(default: T): Flow<T> = callbackFlow {
        val observer = Observer<T?> { value ->
            if (isActive) {
                trySend(value ?: default)
            }
        }

        withContext(Dispatchers.Main.immediate) {
            observeForever(observer)
        }

        awaitClose {
            kotlinx.coroutines.MainScope().launch(Dispatchers.Main.immediate) {
                removeObserver(observer)
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Convert a LiveData of nullable list to a Flow, handling null as empty list.
     *
     * Convenience method specifically for LiveData that emits lists, treating
     * null emissions as empty lists.
     *
     * ## Example
     * ```kotlin
     * val elementsLiveData: LiveData<List<Element>?> = repository.observeElements()
     * val flow: Flow<List<Element>> = elementsLiveData.asListFlow()
     * ```
     *
     * @receiver The nullable list LiveData to convert
     * @return Flow that emits non-null lists
     */
    @JvmStatic
    fun <T> LiveData<List<T>?>.asListFlow(): Flow<List<T>> = callbackFlow {
        val observer = Observer<List<T>?> { value ->
            if (isActive) {
                trySend(value ?: emptyList())
            }
        }

        withContext(Dispatchers.Main.immediate) {
            observeForever(observer)
        }

        awaitClose {
            kotlinx.coroutines.MainScope().launch(Dispatchers.Main.immediate) {
                removeObserver(observer)
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Convert a LiveData to a Flow with initial value emission.
     *
     * Similar to [asFlow] but immediately emits the current LiveData value
     * (if available) before waiting for changes.
     *
     * ## Example
     * ```kotlin
     * val liveData: LiveData<Int> = repository.getCount()
     * val flow = liveData.asFlowWithCurrentValue()
     * ```
     *
     * @receiver The LiveData to convert
     * @return Flow that emits current value immediately, then changes
     */
    @JvmStatic
    fun <T> LiveData<T>.asFlowWithCurrentValue(): Flow<T> = callbackFlow {
        // Emit current value if available
        value?.let { currentValue ->
            trySend(currentValue)
        }

        val observer = Observer<T> { newValue ->
            if (newValue != null && isActive) {
                trySend(newValue)
            }
        }

        withContext(Dispatchers.Main.immediate) {
            observeForever(observer)
        }

        awaitClose {
            kotlinx.coroutines.MainScope().launch(Dispatchers.Main.immediate) {
                removeObserver(observer)
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Coroutine launch helper (internal use).
     */
    private fun mainScope() = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate
    )
}

// =============================================================================
// Flow Extension Functions
// =============================================================================

/**
 * Map a Flow of items to domain objects using a mapper function.
 *
 * Transforms each emitted list by applying the mapper to each item.
 *
 * ## Example
 * ```kotlin
 * val dtoFlow: Flow<List<ElementDTO>> = repository.observeElements()
 * val domainFlow: Flow<List<Element>> = dtoFlow.mapToDomain { dto ->
 *     dto.toElement()
 * }
 * ```
 *
 * @param mapper Function to transform each item
 * @return Flow of transformed lists
 */
fun <T, R> Flow<List<T>>.mapToDomain(mapper: (T) -> R): Flow<List<R>> {
    return this.map { list ->
        list.map(mapper)
    }
}

/**
 * Map a Flow of items to domain objects, filtering out null results.
 *
 * Similar to [mapToDomain] but allows the mapper to return null to filter
 * out items that cannot be converted.
 *
 * ## Example
 * ```kotlin
 * val dtoFlow: Flow<List<ElementDTO>> = repository.observeElements()
 * val domainFlow: Flow<List<Element>> = dtoFlow.mapToDomainNotNull { dto ->
 *     dto.toElementOrNull()
 * }
 * ```
 *
 * @param mapper Function to transform each item, may return null
 * @return Flow of transformed lists with null items filtered out
 */
fun <T, R : Any> Flow<List<T>>.mapToDomainNotNull(mapper: (T) -> R?): Flow<List<R>> {
    return this.map { list ->
        list.mapNotNull(mapper)
    }
}

/**
 * Debounce rapid updates from a Flow.
 *
 * Useful when LiveData emits many rapid updates and you want to reduce
 * processing by waiting for a quiet period.
 *
 * ## Example
 * ```kotlin
 * val rapidFlow: Flow<List<Element>> = elementsLiveData.asFlow()
 * val debouncedFlow = rapidFlow.debounceUpdates(200) // 200ms debounce
 * ```
 *
 * @param timeoutMillis Debounce timeout in milliseconds (default 100ms)
 * @return Debounced Flow
 */
fun <T> Flow<T>.debounceUpdates(timeoutMillis: Long = 100): Flow<T> {
    return this.debounce(timeoutMillis)
}

/**
 * Filter distinct emissions based on equality.
 *
 * Only emits values that are different from the previous emission.
 *
 * @return Flow with duplicate consecutive emissions filtered out
 */
fun <T> Flow<T>.distinctUpdates(): Flow<T> {
    return this.distinctUntilChanged()
}

/**
 * Filter distinct emissions based on a key selector.
 *
 * Only emits values when the key differs from the previous emission's key.
 *
 * ## Example
 * ```kotlin
 * val elementsFlow: Flow<List<Element>> = ...
 * val distinctByCount = elementsFlow.distinctBy { it.size }
 * ```
 *
 * @param keySelector Function to extract comparison key
 * @return Flow with duplicates (by key) filtered out
 */
fun <T, K> Flow<T>.distinctBy(keySelector: (T) -> K): Flow<T> {
    return this.distinctUntilChanged { old, new ->
        keySelector(old) == keySelector(new)
    }
}

/**
 * Add logging for debugging Flow emissions.
 *
 * Logs each emission with a tag for debugging purposes.
 *
 * ## Example
 * ```kotlin
 * val flow = elementsLiveData.asFlow()
 *     .logEmissions("Elements")
 *     .collect { ... }
 * ```
 *
 * @param tag Tag to prefix log messages
 * @param logger Optional custom logger function
 * @return Flow with logging side effect
 */
fun <T> Flow<T>.logEmissions(
    tag: String,
    logger: (String) -> Unit = { android.util.Log.d("LiveDataFlowBridge", it) }
): Flow<T> {
    return this.onEach { value ->
        logger("[$tag] Emitted: $value")
    }
}

/**
 * Filter list emissions to only non-empty lists.
 *
 * @return Flow that only emits non-empty lists
 */
fun <T> Flow<List<T>>.filterNonEmpty(): Flow<List<T>> {
    return this.filter { it.isNotEmpty() }
}

/**
 * Combine multiple LiveData sources into a single Flow.
 *
 * Emits a combined result whenever any source emits.
 *
 * ## Example
 * ```kotlin
 * val combined = combineLiveData(
 *     elementsLiveData,
 *     commandsLiveData
 * ) { elements, commands ->
 *     CombinedData(elements, commands)
 * }
 * ```
 *
 * @param liveData1 First LiveData source
 * @param liveData2 Second LiveData source
 * @param combine Function to combine values
 * @return Flow of combined values
 */
fun <T1, T2, R> combineLiveData(
    liveData1: LiveData<T1>,
    liveData2: LiveData<T2>,
    combine: (T1?, T2?) -> R
): Flow<R> = callbackFlow {
    var value1: T1? = liveData1.value
    var value2: T2? = liveData2.value

    val observer1 = Observer<T1> { newValue ->
        value1 = newValue
        if (isActive) {
            trySend(combine(value1, value2))
        }
    }

    val observer2 = Observer<T2> { newValue ->
        value2 = newValue
        if (isActive) {
            trySend(combine(value1, value2))
        }
    }

    // Emit initial combined value
    trySend(combine(value1, value2))

    withContext(Dispatchers.Main.immediate) {
        liveData1.observeForever(observer1)
        liveData2.observeForever(observer2)
    }

    awaitClose {
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate).launch {
            liveData1.removeObserver(observer1)
            liveData2.removeObserver(observer2)
        }
    }
}.flowOn(Dispatchers.Default)

/**
 * Transform a Flow with error handling.
 *
 * Catches exceptions and transforms them to a fallback value.
 *
 * ## Example
 * ```kotlin
 * val safeFlow = riskyFlow.withFallback(emptyList()) { error ->
 *     Log.e("Flow", "Error: ${error.message}")
 * }
 * ```
 *
 * @param fallback Value to emit on error
 * @param onError Optional callback for error handling
 * @return Flow with error handling
 */
fun <T> Flow<T>.withFallback(
    fallback: T,
    onError: ((Throwable) -> Unit)? = null
): Flow<T> {
    return this.catch { error ->
        onError?.invoke(error)
        emit(fallback)
    }
}

/**
 * Add a startup delay to a Flow.
 *
 * Useful when you need to wait for initialization before processing.
 *
 * @param delayMillis Delay in milliseconds
 * @return Flow with initial delay
 */
fun <T> Flow<T>.withStartupDelay(delayMillis: Long): Flow<T> {
    return this.onStart {
        delay(delayMillis)
    }
}

// =============================================================================
// Type Aliases for Common Patterns
// =============================================================================

/**
 * Type alias for element list flows.
 */
typealias ElementFlow = Flow<List<com.augmentalis.commandmanager.QuantizedElement>>

/**
 * Type alias for command list flows.
 */
typealias CommandFlow = Flow<List<com.augmentalis.commandmanager.QuantizedCommand>>

// =============================================================================
// Convenience Functions
// =============================================================================

/**
 * Create a Flow from a suspend function that returns a single value.
 *
 * Useful for converting one-shot repository calls to Flows.
 *
 * ## Example
 * ```kotlin
 * val flow = flowOfSingle { repository.getElement(avid) }
 * ```
 *
 * @param block Suspend function to execute
 * @return Flow that emits the single value
 */
fun <T> flowOfSingle(block: suspend () -> T): Flow<T> = kotlinx.coroutines.flow.flow {
    emit(block())
}.flowOn(Dispatchers.IO)

/**
 * Create a polling Flow from a suspend function.
 *
 * Repeatedly calls the function at the specified interval.
 *
 * ## Example
 * ```kotlin
 * val pollingFlow = pollingFlow(5000) { repository.getLatestData() }
 * ```
 *
 * @param intervalMillis Polling interval in milliseconds
 * @param block Suspend function to poll
 * @return Flow that emits polled values
 */
fun <T> pollingFlow(
    intervalMillis: Long,
    block: suspend () -> T
): Flow<T> = kotlinx.coroutines.flow.flow {
    while (true) {
        emit(block())
        delay(intervalMillis)
    }
}.flowOn(Dispatchers.IO)

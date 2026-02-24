/**
 * MockRecognitionCallback.kt - Mock implementation of IRecognitionCallback for testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-08-28
 * 
 * Provides a mock implementation of the AIDL callback interface with tracking
 * and verification capabilities for integration testing.
 */
package com.augmentalis.voicerecognition.mocks

import android.util.Log
import com.augmentalis.voicerecognition.IRecognitionCallback
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Mock implementation of IRecognitionCallback for testing
 * 
 * Tracks all callback invocations and provides verification methods
 * with timeout handling for async operations.
 */
class MockRecognitionCallback(
    private val callbackName: String
) : IRecognitionCallback.Stub() {
    
    companion object {
        private const val TAG = "MockRecognitionCallback"
        private const val DEFAULT_TIMEOUT_MS = 5000L
    }
    
    // Result tracking
    private val results = ConcurrentLinkedQueue<RecognitionResult>()
    private val errors = ConcurrentLinkedQueue<RecognitionError>()
    private val stateChanges = ConcurrentLinkedQueue<StateChange>()
    private val partialResults = ConcurrentLinkedQueue<String>()
    
    // Counters for verification
    private val resultCount = AtomicInteger(0)
    private val errorCount = AtomicInteger(0)
    private val stateChangeCount = AtomicInteger(0)
    private val partialResultCount = AtomicInteger(0)
    
    // Synchronization latches
    private var resultLatch: CountDownLatch? = null
    private var errorLatch: CountDownLatch? = null
    private var stateChangeLatch: CountDownLatch? = null
    private var partialResultLatch: CountDownLatch? = null
    
    // Last received data
    private val lastResult = AtomicReference<RecognitionResult?>()
    private val lastError = AtomicReference<RecognitionError?>()
    private val lastStateChange = AtomicReference<StateChange?>()
    private val lastPartialResult = AtomicReference<String?>()
    
    // Data classes for tracking
    data class RecognitionResult(
        val text: String,
        val confidence: Float,
        val isFinal: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class RecognitionError(
        val errorCode: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class StateChange(
        val state: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun getStateString(): String = when (state) {
            0 -> "IDLE"
            1 -> "LISTENING"
            2 -> "PROCESSING"
            3 -> "ERROR"
            else -> "UNKNOWN($state)"
        }
    }
    
    // AIDL Callback Implementation
    
    override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
        val resultText = text ?: ""
        Log.d(TAG, "[$callbackName] onRecognitionResult: '$resultText', confidence=$confidence, isFinal=$isFinal")
        
        val result = RecognitionResult(resultText, confidence, isFinal)
        results.offer(result)
        lastResult.set(result)
        resultCount.incrementAndGet()
        
        resultLatch?.countDown()
    }
    
    override fun onError(errorCode: Int, message: String?) {
        val errorMessage = message ?: "Unknown error"
        Log.d(TAG, "[$callbackName] onError: code=$errorCode, message='$errorMessage'")
        
        val error = RecognitionError(errorCode, errorMessage)
        errors.offer(error)
        lastError.set(error)
        errorCount.incrementAndGet()
        
        errorLatch?.countDown()
    }
    
    override fun onStateChanged(state: Int, message: String?) {
        val stateMessage = message ?: ""
        Log.d(TAG, "[$callbackName] onStateChanged: state=$state (${getStateString(state)}), message='$stateMessage'")
        
        val stateChange = StateChange(state, stateMessage)
        stateChanges.offer(stateChange)
        lastStateChange.set(stateChange)
        stateChangeCount.incrementAndGet()
        
        stateChangeLatch?.countDown()
    }
    
    override fun onPartialResult(partialText: String?) {
        val text = partialText ?: ""
        Log.d(TAG, "[$callbackName] onPartialResult: '$text'")
        
        partialResults.offer(text)
        lastPartialResult.set(text)
        partialResultCount.incrementAndGet()
        
        partialResultLatch?.countDown()
    }
    
    // Verification Methods
    
    /**
     * Wait for a recognition result within the specified timeout
     */
    fun waitForResult(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        resultLatch = CountDownLatch(1)
        return resultLatch!!.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Wait for an error within the specified timeout
     */
    fun waitForError(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        errorLatch = CountDownLatch(1)
        return errorLatch!!.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Wait for a state change within the specified timeout
     */
    fun waitForStateChange(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        stateChangeLatch = CountDownLatch(1)
        return stateChangeLatch!!.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Wait for a partial result within the specified timeout
     */
    fun waitForPartialResult(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        partialResultLatch = CountDownLatch(1)
        return partialResultLatch!!.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Wait for a specific state change within the specified timeout
     */
    fun waitForState(expectedState: Int, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentState = lastStateChange.get()
            if (currentState?.state == expectedState) {
                return true
            }
            Thread.sleep(100) // Poll every 100ms
        }
        return false
    }
    
    /**
     * Wait for a final recognition result within the specified timeout
     */
    fun waitForFinalResult(timeoutMs: Long = DEFAULT_TIMEOUT_MS): RecognitionResult? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val result = lastResult.get()
            if (result?.isFinal == true) {
                return result
            }
            Thread.sleep(100) // Poll every 100ms
        }
        return null
    }
    
    // Query Methods
    
    /**
     * Get total number of results received
     */
    fun getResultCount(): Int = resultCount.get()
    
    /**
     * Get total number of errors received
     */
    fun getErrorCount(): Int = errorCount.get()
    
    /**
     * Get total number of state changes received
     */
    fun getStateChangeCount(): Int = stateChangeCount.get()
    
    /**
     * Get total number of partial results received
     */
    fun getPartialResultCount(): Int = partialResultCount.get()
    
    /**
     * Get the last recognition result received
     */
    fun getLastResult(): RecognitionResult? = lastResult.get()
    
    /**
     * Get the last error received
     */
    fun getLastError(): RecognitionError? = lastError.get()
    
    /**
     * Get the last state change received
     */
    fun getLastStateChange(): StateChange? = lastStateChange.get()
    
    /**
     * Get the last partial result received
     */
    fun getLastPartialResult(): String? = lastPartialResult.get()
    
    /**
     * Get all recognition results received
     */
    fun getAllResults(): List<RecognitionResult> = results.toList()
    
    /**
     * Get all errors received
     */
    fun getAllErrors(): List<RecognitionError> = errors.toList()
    
    /**
     * Get all state changes received
     */
    fun getAllStateChanges(): List<StateChange> = stateChanges.toList()
    
    /**
     * Get all partial results received
     */
    fun getAllPartialResults(): List<String> = partialResults.toList()
    
    // Verification Helpers
    
    /**
     * Check if any final results were received
     */
    fun hasFinalResults(): Boolean = results.any { it.isFinal }
    
    /**
     * Check if any partial results were received
     */
    fun hasPartialResults(): Boolean = partialResultCount.get() > 0
    
    /**
     * Check if any errors were received
     */
    fun hasErrors(): Boolean = errorCount.get() > 0
    
    /**
     * Check if a specific state was received
     */
    fun hasState(state: Int): Boolean = stateChanges.any { it.state == state }
    
    /**
     * Check if listening state was reached
     */
    fun hasListeningState(): Boolean = hasState(1)
    
    /**
     * Check if processing state was reached
     */
    fun hasProcessingState(): Boolean = hasState(2)
    
    /**
     * Check if idle state was reached
     */
    fun hasIdleState(): Boolean = hasState(0)
    
    /**
     * Check if error state was reached
     */
    fun hasErrorState(): Boolean = hasState(3)
    
    /**
     * Get results with minimum confidence
     */
    fun getResultsWithMinConfidence(minConfidence: Float): List<RecognitionResult> {
        return results.filter { it.confidence >= minConfidence }
    }
    
    /**
     * Get the most confident result
     */
    fun getMostConfidentResult(): RecognitionResult? {
        return results.maxByOrNull { it.confidence }
    }
    
    // Reset Methods
    
    /**
     * Reset all tracking data
     */
    fun reset() {
        results.clear()
        errors.clear()
        stateChanges.clear()
        partialResults.clear()
        
        resultCount.set(0)
        errorCount.set(0)
        stateChangeCount.set(0)
        partialResultCount.set(0)
        
        lastResult.set(null)
        lastError.set(null)
        lastStateChange.set(null)
        lastPartialResult.set(null)
        
        resultLatch = null
        errorLatch = null
        stateChangeLatch = null
        partialResultLatch = null
    }
    
    /**
     * Get debug information about callback state
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("MockRecognitionCallback Debug Info [$callbackName]")
            appendLine("Results: ${resultCount.get()}")
            appendLine("Errors: ${errorCount.get()}")
            appendLine("State Changes: ${stateChangeCount.get()}")
            appendLine("Partial Results: ${partialResultCount.get()}")
            
            lastResult.get()?.let { result ->
                appendLine("Last Result: '${result.text}' (${result.confidence}, final=${result.isFinal})")
            }
            
            lastError.get()?.let { error ->
                appendLine("Last Error: ${error.errorCode} - '${error.message}'")
            }
            
            lastStateChange.get()?.let { state ->
                appendLine("Last State: ${state.getStateString()} - '${state.message}'")
            }
            
            lastPartialResult.get()?.let { partial ->
                appendLine("Last Partial: '$partial'")
            }
        }
    }
    
    // Private helper methods
    
    private fun getStateString(state: Int): String = when (state) {
        0 -> "IDLE"
        1 -> "LISTENING"
        2 -> "PROCESSING"
        3 -> "ERROR"
        else -> "UNKNOWN($state)"
    }
}
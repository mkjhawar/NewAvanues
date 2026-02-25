/**
 * JitProcessor.kt - Just-In-Time element processor for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-08
 *
 * Processes UI elements in real-time or batch mode, generating VUIDs
 * and preparing elements for voice command registration.
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile

import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ProcessingMode
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Result of processing a single element.
 *
 * @property isSuccess Whether processing succeeded
 * @property vuid Generated VUID if successful
 * @property processingMode Mode used for processing
 * @property error Error message if failed
 * @property elementInfo Original element (for batch tracking)
 */
data class JitProcessingResult(
    val isSuccess: Boolean,
    val vuid: String? = null,
    val processingMode: ProcessingMode = ProcessingMode.IMMEDIATE,
    val error: String? = null,
    val elementInfo: ElementInfo? = null
) {
    companion object {
        /**
         * Create a success result.
         */
        fun success(vuid: String, mode: ProcessingMode, element: ElementInfo? = null): JitProcessingResult {
            return JitProcessingResult(
                isSuccess = true,
                vuid = vuid,
                processingMode = mode,
                elementInfo = element
            )
        }

        /**
         * Create a failure result.
         */
        fun failure(error: String, mode: ProcessingMode, element: ElementInfo? = null): JitProcessingResult {
            return JitProcessingResult(
                isSuccess = false,
                error = error,
                processingMode = mode,
                elementInfo = element
            )
        }
    }
}

/**
 * Just-In-Time element processor.
 *
 * Processes UI elements to generate VUIDs and prepare them for
 * voice command registration. Supports two modes:
 *
 * - IMMEDIATE: Process elements as they arrive
 * - BATCH: Queue elements and process together
 *
 * Usage:
 * ```kotlin
 * val processor = JitProcessor()
 *
 * // Immediate processing
 * val result = processor.processElement(element)
 * if (result.isSuccess) {
 *     registerCommand(result.avid!!)
 * }
 *
 * // Batch processing
 * processor.setProcessingMode(ProcessingMode.BATCH)
 * processor.queueElement(element1)
 * processor.queueElement(element2)
 * val results = processor.processQueue()
 * ```
 */
class JitProcessor {

    @Volatile
    private var processingMode: ProcessingMode = ProcessingMode.IMMEDIATE
    private val queue = mutableListOf<ElementInfo>()
    private val queueLock = SynchronizedObject()
    @Volatile
    private var processedCount = 0

    /**
     * Get the current processing mode.
     */
    fun getProcessingMode(): ProcessingMode = processingMode

    /**
     * Set the processing mode.
     *
     * @param mode IMMEDIATE or BATCH
     */
    fun setProcessingMode(mode: ProcessingMode) {
        processingMode = mode
    }

    /**
     * Process a single element.
     *
     * @param element The element to process
     * @return JitProcessingResult with VUID if successful
     */
    fun processElement(element: ElementInfo): JitProcessingResult {
        // Validate element
        if (!isValidElement(element)) {
            return JitProcessingResult.failure(
                error = "Invalid element: missing required fields",
                mode = processingMode,
                element = element
            )
        }

        // Generate VUID
        val vuid = generateVuid(element)
        processedCount++

        return JitProcessingResult.success(
            vuid = vuid,
            mode = processingMode,
            element = element
        )
    }

    /**
     * Process multiple elements.
     *
     * @param elements List of elements to process
     * @return List of processing results
     */
    fun processElements(elements: List<ElementInfo>): List<JitProcessingResult> {
        return elements.map { processElement(it) }
    }

    /**
     * Get the current queue size.
     */
    fun getQueueSize(): Int = synchronized(queueLock) { queue.size }

    /**
     * Add an element to the processing queue.
     *
     * @param element Element to queue
     */
    fun queueElement(element: ElementInfo) {
        synchronized(queueLock) {
            queue.add(element)
        }
    }

    /**
     * Clear the processing queue.
     */
    fun clearQueue() {
        synchronized(queueLock) {
            queue.clear()
        }
    }

    /**
     * Process all queued elements.
     *
     * @return List of processing results
     */
    fun processQueue(): List<JitProcessingResult> {
        val elements = synchronized(queueLock) {
            val copy = queue.toList()
            queue.clear()
            copy
        }
        return elements.map { processElement(it) }
    }

    /**
     * Get the total number of elements processed.
     */
    fun getProcessedCount(): Int = processedCount

    /**
     * Reset processor state.
     */
    fun reset() {
        processedCount = 0
        synchronized(queueLock) { queue.clear() }
        processingMode = ProcessingMode.IMMEDIATE
    }

    /**
     * Check if an element is valid for processing.
     *
     * An element is valid if it has at least one identifying characteristic:
     * - Non-blank className
     * - Non-blank resourceId
     * - Non-blank text
     * - Non-blank contentDescription
     */
    private fun isValidElement(element: ElementInfo): Boolean {
        return element.className.isNotBlank() ||
               element.resourceId.isNotBlank() ||
               element.text.isNotBlank() ||
               element.contentDescription.isNotBlank()
    }

    /**
     * Generate an element fingerprint.
     * Format: {TypeCode}:{hash8} e.g., "BTN:a3f2e1c9"
     */
    private fun generateVuid(element: ElementInfo): String {
        val packageName = element.packageName.ifBlank { "unknown" }
        return ElementFingerprint.fromElementInfo(element, packageName)
    }
}

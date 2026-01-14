package com.augmentalis.voiceoscoreng.features

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ElementProcessingResult
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Just-In-Time processor for UI elements.
 *
 * Processes elements as they are encountered during app usage,
 * generating VUIDs and preparing them for voice command mapping.
 */
class JitProcessor {

    private var processingMode: ProcessingMode = ProcessingMode.IMMEDIATE
    private val elementQueue = mutableListOf<ElementInfo>()
    private var processedCount = 0

    /**
     * Get the current processing mode.
     */
    fun getProcessingMode(): ProcessingMode = processingMode

    /**
     * Set the processing mode.
     */
    fun setProcessingMode(mode: ProcessingMode) {
        processingMode = mode
    }

    /**
     * Process a single element immediately.
     */
    fun processElement(element: ElementInfo): JitProcessingResult {
        if (!isValidElement(element)) {
            return JitProcessingResult(
                isSuccess = false,
                vuid = null,
                processingMode = processingMode,
                errorMessage = "Invalid element: missing required properties"
            )
        }

        val vuid = generateVUID(element)
        processedCount++

        return JitProcessingResult(
            isSuccess = true,
            vuid = vuid,
            processingMode = processingMode,
            errorMessage = null
        )
    }

    /**
     * Process multiple elements.
     */
    fun processElements(elements: List<ElementInfo>): List<JitProcessingResult> {
        return elements.map { processElement(it) }
    }

    /**
     * Queue an element for later batch processing.
     */
    fun queueElement(element: ElementInfo) {
        elementQueue.add(element)
    }

    /**
     * Get the current queue size.
     */
    fun getQueueSize(): Int = elementQueue.size

    /**
     * Clear the element queue.
     */
    fun clearQueue() {
        elementQueue.clear()
    }

    /**
     * Process all queued elements.
     */
    fun processQueue(): List<JitProcessingResult> {
        val savedMode = processingMode
        processingMode = ProcessingMode.BATCH

        val results = elementQueue.map { processElement(it) }
        elementQueue.clear()

        processingMode = savedMode
        return results
    }

    /**
     * Get the total count of processed elements.
     */
    fun getProcessedCount(): Int = processedCount

    /**
     * Reset the processor state.
     */
    fun reset() {
        elementQueue.clear()
        processedCount = 0
        processingMode = ProcessingMode.IMMEDIATE
    }

    /**
     * Check if an element is valid for processing.
     */
    private fun isValidElement(element: ElementInfo): Boolean {
        return element.className.isNotEmpty() &&
               (element.isClickable || element.text.isNotEmpty())
    }

    /**
     * Generate a VUID for the element.
     */
    private fun generateVUID(element: ElementInfo): String {
        val typeCode = VUIDGenerator.getTypeCode(element.className)
        val elementIdentifier = element.resourceId.ifEmpty { element.text }

        return VUIDGenerator.generate(
            packageName = "com.augmentalis.voiceoscoreng", // Default package
            typeCode = typeCode,
            elementHash = elementIdentifier
        )
    }
}

/**
 * Result of JIT processing for an element.
 */
data class JitProcessingResult(
    val isSuccess: Boolean,
    val vuid: String?,
    val processingMode: ProcessingMode,
    val errorMessage: String? = null
)

/**
 * IElementProcessorInterface.kt - Focused interface for element processing
 *
 * Part of ISP refactoring to split fat ILearnAppCore interface.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring - ISP Compliance)
 */

package com.augmentalis.learnappcore.core

import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.learnappcore.models.ElementInfo

/**
 * Element Processing Operations Interface
 *
 * Focused interface for processing individual elements and generating commands.
 *
 * Single Responsibility: Element to command conversion
 *
 * This interface follows Interface Segregation Principle (ISP) by exposing
 * only element processing operations, avoiding the fat interface problem.
 */
interface IElementProcessorInterface {
    /**
     * Process element and generate command.
     *
     * @param element Element to process
     * @param packageName App package name
     * @param mode Processing mode (IMMEDIATE or BATCH)
     * @return Processing result with UUID and command
     */
    suspend fun processElement(
        element: ElementInfo,
        packageName: String,
        mode: ProcessingMode
    ): ElementProcessingResult

    /**
     * Process batch of elements.
     *
     * More efficient than processing individually.
     *
     * @param elements Elements to process
     * @param packageName App package name
     * @return List of processing results
     */
    suspend fun processBatch(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementProcessingResult>
}

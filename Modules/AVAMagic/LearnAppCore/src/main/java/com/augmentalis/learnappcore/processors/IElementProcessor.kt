/**
 * IElementProcessor.kt - Interface for element processing operations
 *
 * Handles processing of individual elements to generate commands.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.processors

import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.learnappcore.models.ElementInfo

/**
 * Element Processor Interface
 *
 * Responsibilities:
 * - Process single elements
 * - Generate voice commands
 * - Calculate element hashes
 * - Determine action types
 *
 * Single Responsibility: Element to command conversion
 */
interface IElementProcessor {
    /**
     * Process element and generate voice command.
     *
     * @param element Element to process
     * @param packageName App package name
     * @return Generated command DTO or null if not actionable
     */
    suspend fun processElement(
        element: ElementInfo,
        packageName: String
    ): GeneratedCommandDTO?

    /**
     * Calculate element hash for database storage.
     *
     * @param element Element to hash
     * @return Hash string
     */
    fun calculateElementHash(element: ElementInfo): String

    /**
     * Determine action type for element.
     *
     * @param element Element to analyze
     * @return Action type string (click, type, scroll, etc.)
     */
    fun determineActionType(element: ElementInfo): String
}

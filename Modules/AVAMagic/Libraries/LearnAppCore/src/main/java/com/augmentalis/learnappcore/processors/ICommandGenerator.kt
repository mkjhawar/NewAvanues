/**
 * ICommandGenerator.kt - Interface for voice command generation
 *
 * Handles voice command and synonym generation from element labels.
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
import com.augmentalis.learnappcore.detection.AppFramework
import com.augmentalis.learnappcore.models.ElementInfo

/**
 * Command Generator Interface
 *
 * Responsibilities:
 * - Generate voice commands from element labels
 * - Create synonym lists
 * - Handle fallback label generation
 * - Calculate command confidence
 *
 * Single Responsibility: Voice command generation
 */
interface ICommandGenerator {
    /**
     * Generate voice command from element.
     *
     * @param element Element to generate command for
     * @param vuid Pre-generated UUID
     * @param packageName Package name for framework detection
     * @param framework Detected app framework
     * @return Generated command DTO or null if not actionable
     */
    fun generateVoiceCommand(
        element: ElementInfo,
        uuid: String,
        packageName: String,
        framework: AppFramework
    ): GeneratedCommandDTO?

    /**
     * Generate synonyms for action and label.
     *
     * @param actionType Action type (click, type, scroll, etc.)
     * @param label Element label
     * @return JSON array string of synonyms
     */
    fun generateSynonyms(actionType: String, label: String): String

    /**
     * Generate fallback label for unlabeled elements.
     *
     * @param element Element to generate label for
     * @param framework Detected app framework
     * @return Generated label
     */
    fun generateFallbackLabel(element: ElementInfo, framework: AppFramework): String

    /**
     * Generate last-resort label for actionable elements.
     *
     * Uses position-based coordinates.
     *
     * @param element Element to generate label for
     * @return Coordinate-based label
     */
    fun generateLastResortLabel(element: ElementInfo): String

    /**
     * Calculate command confidence score.
     *
     * @param element Element analyzed
     * @param label Generated label
     * @return Confidence score (0.0 - 1.0)
     */
    fun calculateConfidence(element: ElementInfo, label: String): Double
}

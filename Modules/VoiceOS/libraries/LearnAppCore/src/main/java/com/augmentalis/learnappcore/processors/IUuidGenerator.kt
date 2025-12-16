/**
 * IUuidGenerator.kt - Interface for UUID generation
 *
 * Handles deterministic UUID generation from element properties.
 * Extracted from LearnAppCore as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 1.2.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.processors

import com.augmentalis.learnappcore.models.ElementInfo

/**
 * UUID Generator Interface
 *
 * Responsibilities:
 * - Generate deterministic UUIDs from element properties
 * - Ensure UUID stability across app launches
 * - Support third-party UUID format
 *
 * Single Responsibility: UUID generation logic
 */
interface IUuidGenerator {
    /**
     * Generate UUID for element.
     *
     * Format: {packageName}.{type}-{hash}
     *
     * @param element Element to generate UUID for
     * @param packageName App package name
     * @return Generated UUID string
     */
    fun generateUuid(element: ElementInfo, packageName: String): String

    /**
     * Get element type for UUID.
     *
     * @param element Element to analyze
     * @return Element type (button, input, scroll, element)
     */
    fun getElementType(element: ElementInfo): String
}

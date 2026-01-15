/**
 * IElementRegistrar.kt - Element registration interface
 *
 * Defines the contract for registering UI elements with UUIDs and aliases.
 * Platform implementations handle the actual UUID generation and persistence.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Result of element registration
 */
data class RegistrationResult(
    val registeredCount: Int,
    val skippedCount: Int,
    val commandsGenerated: Int,
    val deduplicatedAliases: Int,
    val errors: List<String>
)

/**
 * Interface for element registration during exploration.
 *
 * Implementations handle:
 * - UUID generation for elements
 * - Alias generation and deduplication
 * - Voice command generation
 * - Persistence to database
 */
interface IElementRegistrar {

    /**
     * Pre-generate UUIDs for elements (before clicking).
     *
     * This is called before the click loop to ensure UUIDs are available
     * while nodes are still fresh. Does NOT persist to database.
     *
     * @param elements Elements to generate UUIDs for
     * @param packageName Target app package name
     * @return Elements with UUIDs populated
     */
    suspend fun preGenerateUuids(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementInfo>

    /**
     * Register elements with batch deduplication.
     *
     * This is called after exploring a screen to:
     * 1. Generate UUIDs
     * 2. Create and deduplicate aliases
     * 3. Generate voice commands
     * 4. Persist to database
     *
     * @param elements Elements to register
     * @param packageName Target app package
     * @param alreadyRegistered Set of already-registered stable IDs (for skip optimization)
     * @return Registration result with statistics
     */
    suspend fun registerElements(
        elements: List<ElementInfo>,
        packageName: String,
        alreadyRegistered: MutableSet<String>? = null
    ): RegistrationResult

    /**
     * Generate alias from element with fallbacks.
     *
     * Priority:
     * 1. Element text
     * 2. Content description
     * 3. Resource ID (last component)
     * 4. Generic: "element_{type}_{counter}"
     *
     * @param element Element to generate alias for
     * @return Generated alias (sanitized, 3-50 characters)
     */
    fun generateAliasFromElement(element: ElementInfo): String

    /**
     * Sanitize alias to valid format.
     *
     * Requirements:
     * - Lowercase only
     * - Alphanumeric + underscores
     * - Must start with letter
     * - Length 3-50 characters
     *
     * @param alias Raw alias string
     * @return Sanitized alias
     */
    fun sanitizeAlias(alias: String): String

    /**
     * Clear generic alias counters (call at start of new exploration)
     */
    fun clearCounters()
}

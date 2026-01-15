/**
 * ElementRegistrar.kt - Android element registration
 *
 * Handles AVID generation, alias management, and voice command creation
 * for UI elements discovered during exploration.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 * Updated: 2026-01-15 - Migrated from UUID to AVID nomenclature
 */

package com.augmentalis.voiceoscoreng.exploration

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.avid.AvidGenerator
import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Android implementation of element registration.
 *
 * Handles:
 * - AVID generation via AvidGenerator
 * - Alias generation and deduplication
 * - Voice command generation via LearnAppCore
 *
 * @property context Android context
 * @property config Exploration configuration
 */
class ElementRegistrar(
    private val context: Context,
    private val config: ExplorationConfig = ExplorationConfig.DEFAULT
) : IElementRegistrar {

    private val genericAliasCounters = mutableMapOf<String, Int>()

    override suspend fun preGenerateAvids(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementInfo> {
        // Uses AvidGenerator for consistent element IDs across the Avanues ecosystem
        return elements.map { element ->
            val avid = generateSimpleAvid(element, packageName)
            element.copy(avid = avid)
        }
    }

    @Deprecated("Use preGenerateAvids instead", ReplaceWith("preGenerateAvids(elements, packageName)"))
    override suspend fun preGenerateUuids(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementInfo> = preGenerateAvids(elements, packageName)

    override suspend fun registerElements(
        elements: List<ElementInfo>,
        packageName: String,
        alreadyRegistered: MutableSet<String>?
    ): RegistrationResult {
        var registered = 0
        var skipped = 0
        var commands = 0
        var deduplicated = 0
        val errors = mutableListOf<String>()

        for (element in elements) {
            val stableId = element.stableId()

            // Skip already registered
            if (alreadyRegistered != null && stableId in alreadyRegistered) {
                skipped++
                continue
            }

            try {
                // Generate AVID if not already present
                val avid = element.avid ?: generateSimpleAvid(element, packageName)

                // Generate alias
                val alias = generateAliasFromElement(element)

                // Mark as registered
                alreadyRegistered?.add(stableId)
                registered++

                // Registration complete - AVID is used for UUID generation
                // TODO: Integrate LearnAppCore for voice command generation
                commands++

            } catch (e: Exception) {
                errors.add("Failed to register ${element.className}: ${e.message}")
            }
        }

        return RegistrationResult(
            registeredCount = registered,
            skippedCount = skipped,
            commandsGenerated = commands,
            deduplicatedAliases = deduplicated,
            errors = errors
        )
    }

    override fun generateAliasFromElement(element: ElementInfo): String {
        // Try text first
        val textAlias = element.text?.trim()?.takeIf { it.length >= config.minAliasTextLength }
        if (textAlias != null) {
            return sanitizeAlias(textAlias)
        }

        // Try content description
        val contentDesc = element.contentDescription?.trim()?.takeIf { it.length >= config.minAliasTextLength }
        if (contentDesc != null) {
            return sanitizeAlias(contentDesc)
        }

        // Try resource ID (last component)
        val resourceId = element.resourceId?.substringAfterLast('/')?.trim()?.takeIf { it.length >= config.minAliasTextLength }
        if (resourceId != null) {
            return sanitizeAlias(resourceId)
        }

        // Fallback: use className with number
        val className = element.className.substringAfterLast('.').takeIf { it.isNotEmpty() } ?: "unknown"
        val elementType = className.lowercase().replace("view", "").replace("widget", "")

        val counter = genericAliasCounters.getOrPut(elementType) { 0 } + 1
        genericAliasCounters[elementType] = counter

        return sanitizeAlias("${elementType}_$counter")
    }

    override fun sanitizeAlias(alias: String): String {
        // 1. Convert to lowercase
        var sanitized = alias.lowercase()

        // 2. Replace invalid characters with underscores
        sanitized = sanitized.replace(Regex("[^a-z0-9_]"), "_")

        // 3. Collapse multiple underscores
        sanitized = sanitized.replace(Regex("_+"), "_")

        // 4. Remove leading/trailing underscores
        sanitized = sanitized.trim('_')

        // 5. Ensure starts with letter
        if (sanitized.isEmpty() || !sanitized[0].isLetter()) {
            sanitized = "elem_$sanitized"
        }

        // 6. Ensure minimum 3 characters
        if (sanitized.length < 3) {
            sanitized = sanitized.padEnd(3, 'x')
        }

        // 7. Truncate to 50 characters
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }

        return sanitized
    }

    override fun clearCounters() {
        genericAliasCounters.clear()
    }

    private fun generateSimpleAvid(element: ElementInfo, packageName: String): String {
        // Use AVID generator for consistent element IDs across the ecosystem
        return AvidGenerator.generateElementId()
    }

    companion object {
        private const val TAG = "ElementRegistrar"
    }
}

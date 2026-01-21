/**
 * ElementRegistrar.kt - Element registration and alias management
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ElementRegistrar.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-04
 * Refactored: 2026-01-15 (SOLID extraction from ExplorationEngine.kt)
 *
 * Single Responsibility: Handles UUID generation, element registration,
 * alias generation and management during exploration.
 *
 * Extracted from ExplorationEngine.kt to improve maintainability and testability.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.content.Context
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.core.LearnAppCore
import com.augmentalis.voiceoscore.learnapp.core.ProcessingMode
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator

/**
 * Handles element registration with UUID generation and alias management.
 *
 * This class is responsible for:
 * - Pre-generating UUIDs for elements before clicking
 * - Registering elements in the UUID system
 * - Generating and deduplicating aliases
 * - Generating voice commands via LearnAppCore
 *
 * ## Performance Optimizations
 *
 * - Batch alias deduplication (2 DB ops instead of O(N) per element)
 * - Pre-generation of UUIDs (fast, no DB)
 * - Voice command batch processing
 *
 * ## Usage Example
 *
 * ```kotlin
 * val registrar = ElementRegistrar(
 *     context, uuidCreator, thirdPartyGenerator, aliasManager, learnAppCore
 * )
 *
 * // Pre-generate UUIDs (before clicking)
 * val elementsWithUuids = registrar.preGenerateUuids(elements, packageName)
 *
 * // Register elements (after clicking)
 * val uuids = registrar.registerElements(elements, packageName)
 * ```
 *
 * @property context Android context
 * @property uuidCreator UUID creator instance
 * @property thirdPartyGenerator Third-party UUID generator
 * @property aliasManager Alias manager for deduplication
 * @property learnAppCore Optional LearnAppCore for voice command generation
 * @property notifier Optional notifier for generic alias notifications
 * @property metrics Optional metrics collector
 */
class ElementRegistrar(
    private val context: Context,
    private val uuidCreator: UUIDCreator,
    private val thirdPartyGenerator: ThirdPartyUuidGenerator,
    private val aliasManager: UuidAliasManager,
    private val learnAppCore: LearnAppCore? = null,
    private val notifier: ExplorationNotifier? = null,
    private val metrics: ExplorationMetrics? = null
) {
    /**
     * Developer settings for tunable parameters
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * Track generic alias counters per element type
     */
    private val genericAliasCounters = mutableMapOf<String, Int>()

    /**
     * Pre-generate UUIDs for a list of elements (before clicking).
     *
     * This is called before the click loop to ensure UUIDs are available
     * while nodes are still fresh.
     *
     * @param elements Elements to generate UUIDs for
     * @param packageName Target app package name
     * @return Elements with UUIDs populated
     */
    suspend fun preGenerateUuids(
        elements: List<ElementInfo>,
        packageName: String
    ): List<ElementInfo> {
        val startTime = System.currentTimeMillis()

        // Track element detection
        elements.forEach { _ ->
            metrics?.onElementDetected()
        }

        val elementsWithUuids = elements.map { element ->
            element.node?.let { node ->
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)

                // Track VUID creation
                metrics?.onVUIDCreated()

                element.copy(uuid = uuid)
            } ?: element
        }

        val elapsed = System.currentTimeMillis() - startTime

        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d(TAG, "Pre-generated ${elements.size} UUIDs in ${elapsed}ms")
        }

        return elementsWithUuids
    }

    /**
     * Register elements with batch deduplication (PERFORMANCE OPTIMIZATION)
     *
     * **Before (Individual Operations):**
     * - 315 DB operations for 63 elements
     * - 1351ms latency
     *
     * **After (Batch Operations):**
     * - 2 DB operations (1 query + 1 batch insert)
     * - <100ms latency (13x+ faster)
     *
     * @param elements List of elements to register
     * @param packageName Target app package
     * @param registeredUuids Optional set to track already-registered UUIDs
     * @return List of UUIDs
     */
    suspend fun registerElements(
        elements: List<ElementInfo>,
        packageName: String,
        registeredUuids: MutableSet<String>? = null
    ): List<String> {
        val startTime = System.currentTimeMillis()

        // Track element detection
        elements.forEach { _ ->
            metrics?.onElementDetected()
        }

        // Step 1: Generate UUIDs and register elements (fast, no DB)
        val uuidElementMap = mutableMapOf<String, ElementInfo>()
        var skippedCount = 0

        for (element in elements) {
            element.node?.let { node ->
                // Skip already-registered elements
                val stableId = element.stableId()
                if (registeredUuids != null && stableId in registeredUuids) {
                    skippedCount++
                    metrics?.onElementFiltered(element, "Already registered")
                    return@let
                }

                // Generate UUID
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)
                metrics?.onVUIDCreated()

                // Mark as registered
                registeredUuids?.add(stableId)

                // Create UUIDElement
                val uuidElement = UUIDElement(
                    vuid = uuid,
                    name = element.getDisplayName(),
                    type = element.extractElementType(),
                    metadata = UUIDMetadata(
                        attributes = mapOf(
                            "thirdPartyApp" to "true",
                            "packageName" to packageName,
                            "className" to element.className,
                            "resourceId" to element.resourceId
                        ),
                        accessibility = UUIDAccessibility(
                            isClickable = element.isClickable,
                            isFocusable = element.isEnabled
                        )
                    )
                )

                // Register with UUIDCreator (no DB yet)
                uuidCreator.registerElement(uuidElement)

                // Store UUID in element
                element.uuid = uuid
                uuidElementMap[uuid] = element
            }
        }

        // Step 2: Generate base aliases (no DB)
        val uuidAliasMap = mutableMapOf<String, String>()
        val hasNoMetadataMap = mutableMapOf<String, Boolean>()

        for ((uuid, element) in uuidElementMap) {
            try {
                val baseAlias = generateAliasFromElement(element)

                // Check if element has no metadata
                val hasNoMetadata = (element.text.isNullOrBlank() &&
                                    element.contentDescription.isNullOrBlank() &&
                                    element.resourceId.isNullOrBlank())

                if (baseAlias.length in MIN_ALIAS_LENGTH..MAX_ALIAS_LENGTH) {
                    uuidAliasMap[uuid] = baseAlias
                    hasNoMetadataMap[uuid] = hasNoMetadata
                } else {
                    android.util.Log.w(TAG,
                        "Alias invalid for $uuid: '$baseAlias' (${baseAlias.length} chars)")
                }
            } catch (aliasError: Exception) {
                android.util.Log.w(TAG,
                    "Failed to generate alias for $uuid: ${aliasError.message}")
            }
        }

        // Step 3: Batch deduplicate and insert (2 DB ops total)
        var deduplicationCount = 0
        if (uuidAliasMap.isNotEmpty()) {
            try {
                val deduplicatedAliases = aliasManager.setAliasesBatch(uuidAliasMap)

                // Log only actual deduplications (reduce noise)
                deduplicatedAliases.forEach { (uuid, actualAlias) ->
                    val baseAlias = uuidAliasMap[uuid]
                    if (actualAlias != baseAlias) {
                        android.util.Log.v(TAG, "Deduplicated alias: '$baseAlias' -> '$actualAlias'")
                        deduplicationCount++
                    }

                    // Notify user if generic alias was used
                    if (hasNoMetadataMap[uuid] == true) {
                        val element = uuidElementMap[uuid]
                        if (element != null) {
                            notifier?.notifyGenericAlias(uuid, actualAlias, element)
                        }
                    }
                }
            } catch (batchError: Exception) {
                android.util.Log.e(TAG,
                    "Batch alias registration failed: ${batchError.message}", batchError)
            }
        }

        // Step 4: Generate voice commands using LearnAppCore
        var commandCount = 0
        learnAppCore?.let { core ->
            try {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d(TAG,
                        "Generating voice commands for ${elements.size} elements")
                }

                for (element in elements) {
                    val result = core.processElement(
                        element = element,
                        packageName = packageName,
                        mode = ProcessingMode.BATCH
                    )

                    if (result.success) {
                        commandCount++
                    } else {
                        android.util.Log.v(TAG, "Command generation skipped: ${result.error}")
                    }
                }

                // Flush batch to database
                core.flushBatch()

                android.util.Log.i(TAG, "Generated $commandCount voice commands via LearnAppCore")
            } catch (commandError: Exception) {
                android.util.Log.e(TAG,
                    "Voice command generation failed: ${commandError.message}", commandError)
            }
        } ?: run {
            android.util.Log.w(TAG,
                "LearnAppCore not provided - voice commands will NOT be generated (legacy mode)")
        }

        // Performance metrics
        val elapsedMs = System.currentTimeMillis() - startTime
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d(TAG,
                "PERF: element_registration duration_ms=$elapsedMs elements=${elements.size} " +
                "skipped=$skippedCount commands=$commandCount deduplications=$deduplicationCount " +
                "rate=${if (elapsedMs > 0) elements.size * 1000 / elapsedMs else 0}/sec")
        }

        if (skippedCount > 0) {
            android.util.Log.i(TAG,
                "Skipped $skippedCount already-registered elements (ViewPager/scroll optimization)")
        }

        return uuidElementMap.keys.toList()
    }

    /**
     * Generate alias from element with fallbacks
     *
     * Tries multiple sources:
     * 1. Element text
     * 2. Content description
     * 3. Resource ID (last component)
     * 4. Fallback: "element_${className}"
     *
     * @param element Element to generate alias for
     * @return Generated alias (3-50 characters)
     */
    fun generateAliasFromElement(element: ElementInfo): String {
        // Try text first
        val textAlias = element.text?.trim()?.takeIf { it.isNotEmpty() }
        if (textAlias != null && textAlias.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(textAlias)
        }

        // Try content description
        val contentDesc = element.contentDescription?.trim()?.takeIf { it.isNotEmpty() }
        if (contentDesc != null && contentDesc.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(contentDesc)
        }

        // Try resource ID (last component)
        val resourceId = element.resourceId?.substringAfterLast('/')?.trim()?.takeIf { it.isNotEmpty() }
        if (resourceId != null && resourceId.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(resourceId)
        }

        // Fallback: use className with number
        val className = element.className.substringAfterLast('.').takeIf { it.isNotEmpty() } ?: "unknown"
        val elementType = className.lowercase().replace("view", "").replace("widget", "")

        // Get next number for this element type
        val counter = genericAliasCounters.getOrPut(elementType) { 0 } + 1
        genericAliasCounters[elementType] = counter

        val numberedAlias = "${elementType}_$counter"

        android.util.Log.w(TAG,
            "Element has no metadata (text/contentDesc/resourceId). " +
            "Assigned generic alias: $numberedAlias for ${element.className}")

        return sanitizeAlias(numberedAlias)
    }

    /**
     * Sanitize alias to valid format
     *
     * AliasManager Requirements:
     * - Must start with a letter (a-z)
     * - Must contain only lowercase alphanumeric + underscores
     * - No hyphens allowed
     * - Length 3-50 characters
     *
     * @param alias Raw alias string
     * @return Sanitized alias meeting all requirements
     */
    fun sanitizeAlias(alias: String): String {
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
        if (sanitized.length < MIN_ALIAS_LENGTH) {
            sanitized = sanitized.padEnd(MIN_ALIAS_LENGTH, 'x')
        }

        // 7. Truncate to 50 characters
        if (sanitized.length > MAX_ALIAS_LENGTH) {
            sanitized = sanitized.substring(0, MAX_ALIAS_LENGTH)
        }

        // 8. Final validation - ensure still starts with letter
        if (!sanitized[0].isLetter()) {
            sanitized = "elem" + sanitized.substring(4)
        }

        return sanitized
    }

    /**
     * Clear generic alias counters (call at start of new exploration)
     */
    fun clearCounters() {
        genericAliasCounters.clear()
    }

    companion object {
        private const val TAG = "ElementRegistrar"
        private const val MIN_ALIAS_LENGTH = 3
        private const val MAX_ALIAS_LENGTH = 50
    }
}

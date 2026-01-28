/**
 * JITLearner.kt - Just-In-Time Learner for VoiceOSCoreNG (Phase 14)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-07 - Add database persistence via ICommandPersistence
 *
 * This class handles on-demand learning of UI elements, generating voice commands
 * for elements that are encountered during user interaction. It supports consent-based
 * learning and tracks which elements have already been learned to avoid duplicates.
 *
 * Commands are persisted to database via ICommandPersistence interface.
 */
package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.CommandActionType
import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.QuantizedCommand
import com.augmentalis.commandmanager.currentTimeMillis
import com.augmentalis.commandmanager.ICommandPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Represents a request to learn a UI element.
 *
 * @property element The UI element to be learned
 * @property sessionId The session identifier for grouping learning events
 * @property timestamp When the learning was requested (milliseconds since epoch)
 */
data class LearningRequest(
    val element: ElementInfo,
    val sessionId: String,
    val timestamp: Long = currentTimeMillis()
)

/**
 * Result of a learning operation.
 *
 * @property success Whether the learning operation succeeded
 * @property element The element that was processed
 * @property generatedCommands List of voice commands generated for the element
 * @property vuid The generated Voice UID for the element (if successful)
 * @property error Error message if learning failed
 */
data class LearningResult(
    val success: Boolean,
    val element: ElementInfo,
    val generatedCommands: List<String> = emptyList(),
    val vuid: String? = null,
    val error: String? = null
)

/**
 * Interface for providing user consent for learning operations.
 *
 * Implementations can prompt users for consent, check stored consent preferences,
 * or use package-level consent grants.
 */
interface IConsentProvider {
    /**
     * Request consent from the user to learn the given element.
     *
     * @param element The element that would be learned
     * @return true if user grants consent, false otherwise
     */
    fun requestConsent(element: ElementInfo): Boolean

    /**
     * Check if consent has already been granted for a package.
     *
     * @param packageName The package name to check consent for
     * @return true if consent has been granted for this package
     */
    fun hasConsent(packageName: String): Boolean
}

/**
 * Just-In-Time Learner for on-demand UI element learning.
 *
 * The JITLearner tracks UI elements that can be learned for voice command generation.
 * It maintains a set of already-learned elements to avoid duplicates and supports
 * consent-based learning through an optional IConsentProvider.
 *
 * **Database Persistence:**
 * When a persistence provider is configured, generated commands are automatically
 * persisted via ICommandPersistence.insertBatch().
 *
 * Usage:
 * ```kotlin
 * // Android: Use AndroidCommandPersistence wrapper
 * val persistence = AndroidCommandPersistence(repository)
 * val learner = JITLearner(persistence = persistence)
 *
 * // Check if element should be learned
 * if (learner.shouldLearn(element)) {
 *     val request = learner.requestLearning(element, sessionId)
 *     // ... show UI to user, get consent ...
 *     val result = learner.onUserConsent(request)
 *     if (result.success) {
 *         // Commands are auto-persisted to database
 *     }
 * }
 * ```
 *
 * @property persistence Optional persistence provider for database storage (recommended)
 * @property consentProvider Optional provider for consent management
 * @property scope CoroutineScope for persistence operations (defaults to Default dispatcher)
 */
class JITLearner(
    private val persistence: ICommandPersistence? = null,
    private val consentProvider: IConsentProvider? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    /**
     * Set of VUIDs (resourceIds) for elements that have already been learned.
     * Using resourceId as the key since it's the most stable identifier.
     */
    private val learnedElements = mutableSetOf<String>()

    /**
     * List of pending learning requests awaiting user consent or processing.
     */
    private val pendingRequests = mutableListOf<LearningRequest>()

    /**
     * Whether the JIT learner is enabled.
     */
    private var isEnabled: Boolean = true

    /**
     * Enable or disable the JIT learner.
     *
     * When disabled, shouldLearn() will always return false and requestLearning()
     * will return null for all elements.
     *
     * @param enabled true to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Check if the JIT learner is currently enabled.
     *
     * @return true if enabled, false if disabled
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Determine if an element should be learned.
     *
     * An element should be learned if:
     * - The learner is enabled
     * - The element hasn't already been learned
     * - The element has voice content (text or contentDescription)
     * - The element is interactive (clickable or scrollable)
     *
     * @param element The element to check
     * @return true if the element should be learned, false otherwise
     */
    fun shouldLearn(element: ElementInfo): Boolean {
        // Check if learner is enabled
        if (!isEnabled) return false

        // Don't learn if already learned (check by resourceId)
        if (element.resourceId.isNotBlank() && element.resourceId in learnedElements) {
            return false
        }

        // Don't learn if no voice content
        if (element.text.isBlank() && element.contentDescription.isBlank()) {
            return false
        }

        // Don't learn non-interactive elements
        if (!element.isClickable && !element.isScrollable) {
            return false
        }

        return true
    }

    /**
     * Request learning for an element.
     *
     * Creates a LearningRequest and adds it to the pending queue if the element
     * passes the shouldLearn() check.
     *
     * @param element The element to learn
     * @param sessionId Session identifier for grouping related learning events
     * @return A LearningRequest if the element should be learned, null otherwise
     */
    fun requestLearning(element: ElementInfo, sessionId: String): LearningRequest? {
        if (!shouldLearn(element)) return null

        val request = LearningRequest(element, sessionId)
        pendingRequests.add(request)
        return request
    }

    /**
     * Process a learning request after user consent.
     *
     * This method:
     * 1. Checks consent via the provider (if configured)
     * 2. Generates voice commands from the element's text and contentDescription
     * 3. Persists commands to database (if repository is configured)
     * 4. Marks the element as learned
     * 5. Removes the request from pending
     *
     * @param request The learning request to process
     * @return A LearningResult with success status and generated commands
     */
    fun onUserConsent(request: LearningRequest): LearningResult {
        // Check consent via provider if available
        val hasConsent = consentProvider?.hasConsent(request.element.packageName) ?: true
        if (!hasConsent) {
            return LearningResult(
                success = false,
                element = request.element,
                error = "User consent not granted"
            )
        }

        // Generate VUID for the element
        val vuid = generateVuid(request.element)

        // Generate QuantizedCommands for the element
        val quantizedCommands = generateQuantizedCommands(request.element, vuid)

        // Persist commands to database
        if (persistence != null && quantizedCommands.isNotEmpty()) {
            scope.launch {
                try {
                    persistence.insertBatch(quantizedCommands)
                } catch (e: Exception) {
                    // Log error but don't fail the learning result
                    // Commands are still returned for immediate use
                }
            }
        }

        // Mark as learned using resourceId as key
        if (request.element.resourceId.isNotBlank()) {
            learnedElements.add(request.element.resourceId)
        }

        // Remove from pending
        pendingRequests.remove(request)

        return LearningResult(
            success = true,
            element = request.element,
            generatedCommands = quantizedCommands.map { it.phrase },
            vuid = vuid
        )
    }

    /**
     * Cancel a pending learning request.
     *
     * The element will NOT be marked as learned, so it can be learned later.
     *
     * @param request The request to cancel
     */
    fun cancelLearning(request: LearningRequest) {
        pendingRequests.remove(request)
    }

    /**
     * Get the count of pending learning requests.
     *
     * @return Number of requests awaiting processing
     */
    fun getPendingCount(): Int = pendingRequests.size

    /**
     * Get the count of learned elements.
     *
     * @return Number of elements that have been learned
     */
    fun getLearnedCount(): Int = learnedElements.size

    /**
     * Clear all learned elements.
     *
     * After calling this, previously learned elements can be learned again.
     * Pending requests are not affected.
     */
    fun clearLearned() {
        learnedElements.clear()
    }

    /**
     * Reset all state.
     *
     * Clears both learned elements and pending requests.
     */
    fun reset() {
        learnedElements.clear()
        pendingRequests.clear()
    }

    /**
     * Generate QuantizedCommand objects for an element.
     *
     * Creates proper domain objects that can be:
     * - Used immediately for command matching
     * - Persisted to database via toGeneratedCommandDTO()
     *
     * @param element The element to generate commands for
     * @param vuid The VUID for the element
     * @return List of QuantizedCommand objects
     */
    private fun generateQuantizedCommands(element: ElementInfo, vuid: String): List<QuantizedCommand> {
        val commands = mutableListOf<QuantizedCommand>()
        val timestamp = currentTimeMillis()

        // Generate from text
        // Commands are stored WITHOUT verbs - user provides verb at runtime
        // e.g., "4" (user says "click 4" or "tap 4" or just "4")
        if (element.text.isNotBlank()) {
            commands.add(
                QuantizedCommand(
                    avid = "$vuid-text",
                    phrase = element.text.lowercase(),  // No verb - just the label
                    actionType = CommandActionType.CLICK,
                    targetAvid = vuid,
                    confidence = 0.95f,
                    metadata = mapOf(
                        "packageName" to element.packageName,
                        "createdAt" to timestamp.toString(),
                        "source" to "text"
                    )
                )
            )
        }

        // Generate from contentDescription
        // Commands are stored WITHOUT verbs - user provides verb at runtime
        if (element.contentDescription.isNotBlank()) {
            commands.add(
                QuantizedCommand(
                    avid = "$vuid-desc",
                    phrase = element.contentDescription.lowercase(),  // No verb - just the label
                    actionType = CommandActionType.CLICK,
                    targetAvid = vuid,
                    confidence = 0.90f, // Slightly lower since contentDescription may be less precise
                    metadata = mapOf(
                        "packageName" to element.packageName,
                        "createdAt" to timestamp.toString(),
                        "source" to "contentDescription"
                    )
                )
            )
        }

        // Add scroll commands for scrollable elements
        if (element.isScrollable) {
            commands.add(
                QuantizedCommand(
                    avid = "$vuid-scroll",
                    phrase = "scroll ${element.text.ifBlank { element.contentDescription }.lowercase()}",
                    actionType = CommandActionType.SCROLL,
                    targetAvid = vuid,
                    confidence = 0.85f,
                    metadata = mapOf(
                        "packageName" to element.packageName,
                        "createdAt" to timestamp.toString(),
                        "source" to "scrollable"
                    )
                )
            )
        }

        return commands
    }

    /**
     * Generate a Voice UID (VUID) for an element.
     *
     * The VUID format is: vuid_{packageName}_{identifier}
     * where identifier is either the resourceId or a hash of the text.
     *
     * @param element The element to generate a VUID for
     * @return The generated VUID string
     */
    private fun generateVuid(element: ElementInfo): String {
        val identifier = when {
            element.resourceId.isNotBlank() -> element.resourceId
            else -> element.text.hashCode().toString()
        }
        return "vuid_${element.packageName}_$identifier"
    }
}

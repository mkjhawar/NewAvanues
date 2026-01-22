package com.augmentalis.actions

import android.content.Context
import android.util.Log
import com.augmentalis.ava.core.data.repository.IntentCategoryRepository

/**
 * Intent Router
 *
 * Routes voice command intents to appropriate execution backends based on capability:
 * - AVA-capable commands → Execute locally (connectivity, volume, media, system)
 * - VoiceOS-only commands → Forward via IPC (gestures, cursor, accessibility)
 *
 * P2 SOLID Fix: Now uses CategoryCapabilityRegistry instead of hardcoded sets.
 * Categories can be added/modified without changing this class.
 *
 * Architecture:
 * ```
 * User Command → NLU Classification → Intent Router
 *                                          ↓
 *                    ┌────────────────────┴────────────────────┐
 *                    ↓                                          ↓
 *           AVA Execution                              VoiceOS IPC
 *        (WiFi, Volume, etc.)                    (Gestures, Cursor, etc.)
 * ```
 *
 * Benefits:
 * - Single NLU engine (AVA) for all commands
 * - Smart routing based on capability
 * - No conflicts between AVA and VoiceOS
 * - Graceful degradation when VoiceOS unavailable
 * - Open for extension (add categories without modifying IntentRouter)
 *
 * Created: 2025-11-17 (Phase 3)
 * Updated: 2025-12-05 (P2 SOLID refactoring)
 * Author: Manoj Jhawar
 */
class IntentRouter(
    private val context: Context,
    private val voiceOSConnection: VoiceOSConnection? = null,
    private val categoryRegistry: CategoryCapabilityRegistry = CategoryCapabilityRegistry(),
    private val intentCategoryRepository: com.augmentalis.ava.core.data.repository.IntentCategoryRepository? = null
) {

    companion object {
        private const val TAG = "IntentRouter"
    }

    /**
     * Routing decision result
     */
    sealed class RoutingDecision {
        /**
         * Execute command locally in AVA
         */
        data class ExecuteLocally(
            val intent: String,
            val category: String
        ) : RoutingDecision()

        /**
         * Forward command to VoiceOS via IPC
         */
        data class ForwardToVoiceOS(
            val intent: String,
            val category: String
        ) : RoutingDecision()

        /**
         * VoiceOS required but unavailable
         */
        data class VoiceOSUnavailable(
            val intent: String,
            val category: String,
            val reason: String
        ) : RoutingDecision()

        /**
         * Unknown category, fallback to LLM
         */
        data class FallbackToLLM(
            val intent: String,
            val category: String
        ) : RoutingDecision()
    }

    /**
     * Determine routing for an intent based on its category
     *
     * P2 SOLID Fix: Uses CategoryCapabilityRegistry instead of hardcoded sets.
     *
     * @param intent Intent identifier (e.g., "wifi_on", "cursor_move_up")
     * @param category Intent category (e.g., "connectivity", "cursor")
     * @return Routing decision (execute locally, forward to VoiceOS, or fallback)
     */
    fun route(intent: String, category: String): RoutingDecision {
        Log.d(TAG, "Routing intent: $intent (category: $category)")

        return when (categoryRegistry.getExecutionTarget(category)) {
            // AVA can handle this locally
            CategoryCapabilityRegistry.ExecutionTarget.AVA_LOCAL -> {
                Log.i(TAG, "Routing to AVA local execution: $intent")
                RoutingDecision.ExecuteLocally(intent, category)
            }

            // VoiceOS required
            CategoryCapabilityRegistry.ExecutionTarget.VOICEOS -> {
                // Check if VoiceOS is available
                if (isVoiceOSAvailable()) {
                    Log.i(TAG, "Routing to VoiceOS via IPC: $intent")
                    RoutingDecision.ForwardToVoiceOS(intent, category)
                } else {
                    Log.w(TAG, "VoiceOS required but unavailable for: $intent")
                    RoutingDecision.VoiceOSUnavailable(
                        intent,
                        category,
                        "VoiceOS accessibility service is not running"
                    )
                }
            }

            // Unknown category - fallback to LLM
            CategoryCapabilityRegistry.ExecutionTarget.FALLBACK_LLM -> {
                Log.d(TAG, "Unknown category '$category', falling back to LLM: $intent")
                RoutingDecision.FallbackToLLM(intent, category)
            }
        }
    }

    /**
     * Check if VoiceOS is available for IPC
     *
     * Checks:
     * 1. VoiceOS app is installed
     * 2. VoiceOS accessibility service is running
     * 3. IPC connection can be established
     *
     * ADR-014: Now wired to VoiceOSConnection for actual availability check.
     * When VoiceOS is integrated into AVA, this checks accessibility service state.
     *
     * @return True if VoiceOS is available, false otherwise
     */
    private fun isVoiceOSAvailable(): Boolean {
        // ADR-014: Use VoiceOSConnection to check actual accessibility service state
        // Returns false if voiceOSConnection is null (VoiceOS not integrated)
        return voiceOSConnection?.isReady() ?: false
    }

    /**
     * Get category for an intent
     *
     * Phase 2: Database-driven category lookup with fallback to registry.
     * First attempts database lookup, then falls back to hardcoded mappings.
     *
     * Benefits:
     * - Runtime category configuration without code changes
     * - Fast lookups with database indices
     * - Graceful degradation if database empty
     * - Support for priority-based classification
     *
     * @param intent Intent identifier
     * @return Category string, or "unknown" if not found
     */
    suspend fun getCategoryForIntent(intent: String): String {
        Log.v(TAG, "Looking up category for intent: $intent")

        // Phase 2: Try database lookup first
        if (intentCategoryRepository != null) {
            try {
                val dbCategory = intentCategoryRepository.getCategoryForIntent(intent)
                if (dbCategory != null) {
                    Log.v(TAG, "Found in database: $intent -> $dbCategory")
                    return dbCategory
                }
            } catch (e: Exception) {
                Log.w(TAG, "Database lookup failed for $intent, falling back to registry", e)
            }
        }

        // Fallback: Use registry (original hardcoded mappings)
        val fallbackCategory = inferCategoryFromName(intent)
        Log.v(TAG, "Using fallback category for $intent: $fallbackCategory")
        return fallbackCategory
    }

    /**
     * Infer category from intent name (original hardcoded logic)
     *
     * Used as fallback if database lookup fails or repository not available.
     * Matches intent names to categories based on keywords.
     *
     * @param intent Intent identifier
     * @return Category string, or "unknown" if not found
     */
    private fun inferCategoryFromName(intent: String): String {
        return when {
            // Information queries (AVA handles locally)
            intent.contains("time") || intent.contains("date") ||
            intent.contains("weather") || intent.contains("question") -> "information"

            // VoiceOS Expanded Categories (Specific checks before generic system/media)
            intent.contains("copy") || intent.contains("paste") || 
            intent.contains("cut") || intent.contains("select") -> "clipboard"
            
            intent.contains("screen_describe") || intent.contains("screen_find") -> "vision"
            
            intent.contains("app_search") -> "app_interaction"
            
            intent.contains("media_cast") -> "media_casting"

            // System commands (AVA handles locally via standard intents)
            intent.contains("settings") || intent.contains("app") ||
            intent.contains("launch") || intent.contains("open") -> "system"

            // Connectivity (AVA handles locally)
            intent.contains("wifi") || intent.contains("bluetooth") ||
            intent.contains("airplane") || intent.contains("network") -> "connectivity"

            // Volume (AVA handles locally)
            intent.contains("volume") || intent.contains("mute") ||
            intent.contains("sound") -> "volume"

            // Media control (AVA handles locally)
            intent.contains("media") || intent.contains("play") ||
            intent.contains("pause") || intent.contains("skip") ||
            intent.contains("music") || intent.contains("stop") -> "media"

            // Navigation (AVA handles locally)
            intent.contains("navigate") || intent.contains("back") ||
            intent.contains("home") || intent.contains("recent") -> "navigation"

            // Productivity (AVA handles locally)
            intent.contains("alarm") || intent.contains("timer") ||
            intent.contains("reminder") || intent.contains("note") ||
            intent.contains("calendar") -> "productivity"

            // Calculation (AVA handles locally)
            intent.contains("calculation") || intent.contains("calculate") ||
            intent.contains("math") || intent == "perform_calculation" -> "calculation"

            // VoiceOS-only commands (require AccessibilityService)
            intent.contains("cursor") -> "cursor"
            intent.contains("swipe") || intent.contains("gesture") -> "gesture"
            intent.contains("scroll") -> "scroll"
            intent.contains("keyboard") || intent.contains("type") -> "keyboard"
            intent.contains("drag") -> "drag"

            else -> "unknown"
        }
    }

    /**
     * Get routing statistics
     *
     * @return Map with routing stats
     */
    fun getStats(): Map<String, Any> {
        return categoryRegistry.getStats() + mapOf(
            "voiceos_available" to isVoiceOSAvailable()
        )
    }

    /**
     * Get the category registry for direct access.
     */
    fun getCategoryRegistry(): CategoryCapabilityRegistry = categoryRegistry
}

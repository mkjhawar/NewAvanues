package com.augmentalis.magicui.components.voicerouter

import com.augmentalis.magicui.components.argscanner.ARGRegistry

/**
 * Voice Command Router
 *
 * High-level API for routing voice commands to registered apps.
 *
 * ## Complete Workflow
 * ```kotlin
 * // Setup
 * val registry = ARGRegistry()
 * val router = VoiceCommandRouter(registry)
 *
 * // Discover apps
 * val scanner = ARGScanner(ARGParser(), registry)
 * scanner.scanAll()
 *
 * // Route voice command
 * val result = router.route("open google.com")
 * when (result) {
 *     is RouteResult.Success -> {
 *         context.startActivity(result.intent as Intent)
 *     }
 *     is RouteResult.Ambiguous -> {
 *         // Show user choice dialog
 *         showChoiceDialog(result.matches)
 *     }
 *     is RouteResult.NoMatch -> {
 *         speak("I don't know how to do that")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
class VoiceCommandRouter(
    private val registry: ARGRegistry,
    private val config: RouterConfig = RouterConfig()
) {

    private val matcher = VoiceCommandMatcher(registry)
    private val intentRouter = IntentRouter(registry)

    /**
     * Route a voice command to the best matching app
     *
     * @param voiceInput Natural language voice input
     * @return Routing result with intent or error
     */
    fun route(voiceInput: String): RouteResult {
        // Find matching capabilities
        val matches = matcher.match(voiceInput)

        return when {
            matches.isEmpty() -> {
                RouteResult.NoMatch(voiceInput)
            }

            matches.size == 1 -> {
                // Single match - create intent
                val match = matches[0]
                val intent = intentRouter.createIntent(match)
                RouteResult.Success(
                    match = match,
                    intent = intent
                )
            }

            else -> {
                // Multiple matches
                val topScore = matches[0].score
                val topMatches = matches.filter {
                    (topScore - it.score) < config.ambiguityThreshold
                }

                if (topMatches.size == 1) {
                    // One clearly best match
                    val match = topMatches[0]
                    val intent = intentRouter.createIntent(match)
                    RouteResult.Success(
                        match = match,
                        intent = intent
                    )
                } else {
                    // Ambiguous - user needs to choose
                    RouteResult.Ambiguous(
                        voiceInput = voiceInput,
                        matches = topMatches
                    )
                }
            }
        }
    }

    /**
     * Route with explicit app selection
     *
     * @param voiceInput Voice input
     * @param packageName Specific app package to use
     * @return Routing result
     */
    fun routeToApp(voiceInput: String, packageName: String): RouteResult {
        val matches = matcher.match(voiceInput)
            .filter { it.app.packageName == packageName }

        return when {
            matches.isEmpty() -> RouteResult.NoMatch(voiceInput)
            else -> {
                val match = matches[0]
                val intent = intentRouter.createIntent(match)
                RouteResult.Success(match, intent)
            }
        }
    }

    /**
     * Get all possible matches for a voice command (for UI display)
     *
     * @param voiceInput Voice input
     * @return List of all matches sorted by score
     */
    fun getAllMatches(voiceInput: String): List<VoiceCommandMatch> {
        return matcher.match(voiceInput)
    }

    /**
     * Test if a voice command can be routed
     *
     * @param voiceInput Voice input to test
     * @return true if at least one match exists
     */
    fun canRoute(voiceInput: String): Boolean {
        return matcher.match(voiceInput).isNotEmpty()
    }
}

/**
 * Router configuration
 */
data class RouterConfig(
    /**
     * Score difference threshold for ambiguity detection.
     * If multiple matches are within this threshold, they are considered ambiguous.
     */
    val ambiguityThreshold: Float = 0.2f,

    /**
     * Minimum confidence score to consider a match valid
     */
    val minimumScore: Float = 0.5f,

    /**
     * Maximum number of matches to return in ambiguous results
     */
    val maxAmbiguousMatches: Int = 5,

    /**
     * Enable fuzzy matching for typos/variations
     */
    val enableFuzzyMatching: Boolean = true
)

/**
 * Voice command routing result
 */
sealed class RouteResult {
    /**
     * Successful routing to a single app
     *
     * @property match The matched capability
     * @property intent Platform-specific intent object (Android Intent, etc.)
     */
    data class Success(
        val match: VoiceCommandMatch,
        val intent: Any
    ) : RouteResult()

    /**
     * Multiple equally good matches - user needs to choose
     *
     * @property voiceInput Original voice input
     * @property matches List of possible matches
     */
    data class Ambiguous(
        val voiceInput: String,
        val matches: List<VoiceCommandMatch>
    ) : RouteResult()

    /**
     * No matching capability found
     *
     * @property voiceInput Original voice input
     */
    data class NoMatch(
        val voiceInput: String
    ) : RouteResult()
}

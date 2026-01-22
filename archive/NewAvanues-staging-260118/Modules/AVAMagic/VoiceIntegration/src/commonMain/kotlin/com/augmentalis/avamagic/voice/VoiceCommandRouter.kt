package com.augmentalis.avamagic.voice

import kotlinx.serialization.Serializable

/**
 * Voice Command Router
 *
 * Routes voice commands to appropriate handlers based on:
 * - Exact matches
 * - Pattern matching (with parameters)
 * - Intent-based routing (via NLU)
 * - Fallback handlers
 *
 * Usage:
 * ```kotlin
 * val router = VoiceCommandRouter()
 *
 * // Exact match
 * router.route("open settings") { params ->
 *     openSettings()
 *     VoiceResponse.success("Opening settings")
 * }
 *
 * // Pattern with parameter
 * router.route("set volume to {level}") { params ->
 *     val level = params["level"]?.toIntOrNull() ?: 50
 *     setVolume(level)
 *     VoiceResponse.success("Volume set to $level")
 * }
 *
 * // Process command
 * val result = router.process("set volume to 80")
 * // result.parameters = {level: "80"}
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-11-19
 */
class VoiceCommandRouter {

    private val routes = mutableListOf<Route>()
    private var fallbackHandler: CommandHandler? = null

    /**
     * Add a route for exact or pattern match
     */
    fun route(pattern: String, handler: CommandHandler): VoiceCommandRouter {
        routes.add(Route(
            pattern = pattern,
            regex = patternToRegex(pattern),
            parameters = extractParameters(pattern),
            handler = handler
        ))
        return this
    }

    /**
     * Add a route for specific intent
     */
    fun intentRoute(intentName: String, handler: IntentHandler): VoiceCommandRouter {
        // Convert intent handler to command handler
        val commandHandler: CommandHandler = { params ->
            val intent = Intent(
                name = intentName,
                confidence = params["confidence"]?.toString()?.toFloatOrNull() ?: 1.0f,
                entities = params.filterKeys { it.startsWith("entity.") }
                    .mapKeys { it.key.removePrefix("entity.") }
                    .mapValues { it.value.toString() },
                parameters = params.mapValues { it.value.toString() }
            )
            handler(intent)
        }

        routes.add(Route(
            pattern = "@intent:$intentName",
            regex = null,
            parameters = emptyList(),
            handler = commandHandler,
            isIntent = true,
            intentName = intentName
        ))
        return this
    }

    /**
     * Set fallback handler for unmatched commands
     */
    fun fallback(handler: CommandHandler): VoiceCommandRouter {
        fallbackHandler = handler
        return this
    }

    /**
     * Process a command and route to handler
     */
    suspend fun process(text: String): RouteResult {
        val normalizedText = text.trim().lowercase()

        // Try each route
        for (route in routes) {
            if (route.isIntent) continue // Intent routes handled separately

            val match = route.regex?.find(normalizedText)
            if (match != null) {
                // Extract parameters
                val params = mutableMapOf<String, Any>("text" to text)
                route.parameters.forEachIndexed { index, paramName ->
                    val value = match.groupValues.getOrNull(index + 1) ?: ""
                    params[paramName] = value
                }

                return try {
                    val response = route.handler.invoke(params)
                    RouteResult(
                        matched = true,
                        pattern = route.pattern,
                        parameters = params,
                        response = response
                    )
                } catch (e: Exception) {
                    RouteResult(
                        matched = true,
                        pattern = route.pattern,
                        parameters = params,
                        error = e.message
                    )
                }
            }
        }

        // Try fallback
        fallbackHandler?.let { handler ->
            return try {
                val response = handler.invoke(mapOf("text" to text))
                RouteResult(
                    matched = false,
                    fallback = true,
                    response = response
                )
            } catch (e: Exception) {
                RouteResult(
                    matched = false,
                    fallback = true,
                    error = e.message
                )
            }
        }

        // No match
        return RouteResult(matched = false)
    }

    /**
     * Process an NLU intent
     */
    suspend fun processIntent(intent: Intent): RouteResult {
        // Find matching intent route
        val route = routes.find { it.isIntent && it.intentName == intent.name }

        if (route != null) {
            val params = mutableMapOf<String, Any>(
                "confidence" to intent.confidence
            )
            intent.entities.forEach { (key, value) ->
                params["entity.$key"] = value
            }
            intent.parameters.forEach { (key, value) ->
                params[key] = value
            }

            return try {
                val response = route.handler.invoke(params)
                RouteResult(
                    matched = true,
                    pattern = route.pattern,
                    parameters = params,
                    response = response
                )
            } catch (e: Exception) {
                RouteResult(
                    matched = true,
                    pattern = route.pattern,
                    parameters = params,
                    error = e.message
                )
            }
        }

        return RouteResult(matched = false)
    }

    /**
     * Get all registered patterns
     */
    fun getPatterns(): List<String> {
        return routes.map { it.pattern }
    }

    /**
     * Clear all routes
     */
    fun clear() {
        routes.clear()
        fallbackHandler = null
    }

    // Convert pattern to regex
    private fun patternToRegex(pattern: String): Regex {
        val escaped = pattern
            .replace(".", "\\.")
            .replace("?", "\\?")
            .replace("*", ".*")

        val withParams = escaped.replace(Regex("\\{([^}]+)\\}")) { match ->
            "(.+)"
        }

        return Regex("^$withParams$", RegexOption.IGNORE_CASE)
    }

    // Extract parameter names from pattern
    private fun extractParameters(pattern: String): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(pattern).map { it.groupValues[1] }.toList()
    }

    private data class Route(
        val pattern: String,
        val regex: Regex?,
        val parameters: List<String>,
        val handler: CommandHandler,
        val isIntent: Boolean = false,
        val intentName: String? = null
    )
}

/**
 * Route result
 */
@Serializable
data class RouteResult(
    val matched: Boolean,
    val pattern: String? = null,
    val parameters: Map<String, @Serializable(with = AnyToStringSerializer::class) Any> = emptyMap(),
    val response: VoiceResponse? = null,
    val fallback: Boolean = false,
    val error: String? = null
)

/**
 * Serializer for Any to String
 */
private object AnyToStringSerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "Any", kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        return decoder.decodeString()
    }
}

/**
 * DSL builder for voice commands
 */
class VoiceCommandBuilder {
    private val router = VoiceCommandRouter()

    fun command(pattern: String, handler: CommandHandler) {
        router.route(pattern, handler)
    }

    fun intent(name: String, handler: IntentHandler) {
        router.intentRoute(name, handler)
    }

    fun fallback(handler: CommandHandler) {
        router.fallback(handler)
    }

    fun build(): VoiceCommandRouter = router
}

/**
 * DSL function for building voice commands
 */
fun voiceCommands(block: VoiceCommandBuilder.() -> Unit): VoiceCommandRouter {
    return VoiceCommandBuilder().apply(block).build()
}

/**
 * PluginStateSerializer.kt - Plugin state serialization interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Provides serialization/deserialization for plugin state during hot reload.
 * Enables plugins to save their state before reload and restore it afterward.
 *
 * ## Overview
 * During hot reload, a plugin's in-memory state would be lost when the plugin
 * is unloaded. The PluginStateSerializer provides a way to serialize the
 * plugin's state to bytes and deserialize it back, enabling state preservation
 * across reloads.
 *
 * ## Usage Example
 * ```kotlin
 * val serializer = JsonPluginStateSerializer()
 *
 * // Before reload
 * val state = mapOf(
 *     "counter" to 42,
 *     "username" to "john",
 *     "preferences" to mapOf("theme" to "dark")
 * )
 * val bytes = serializer.serializeState(state)
 *
 * // After reload
 * val restoredState = serializer.deserializeState(bytes)
 * ```
 */
package com.augmentalis.magiccode.plugins.universal

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * Interface for serializing and deserializing plugin state.
 *
 * Implementations of this interface provide mechanisms for converting
 * plugin state (represented as Map<String, Any>) to bytes for storage
 * and back to Map for restoration.
 *
 * ## Thread Safety
 * Implementations should be thread-safe as serialization may occur
 * concurrently with plugin operations.
 *
 * ## Supported Types
 * The state map should contain only serializable types:
 * - Primitives: String, Int, Long, Float, Double, Boolean
 * - Collections: List<Any>, Set<Any>, Map<String, Any>
 * - Null values
 *
 * Complex objects should be serialized to JSON strings or broken down
 * into primitive components before being stored in the state map.
 *
 * @since 1.0.0
 * @see JsonPluginStateSerializer
 */
interface PluginStateSerializer {

    /**
     * Serialize plugin state to bytes.
     *
     * Converts a state map to a byte array for storage. The state map
     * should contain only serializable types (primitives, collections,
     * nested maps).
     *
     * @param state The state map to serialize
     * @return ByteArray containing the serialized state
     * @throws SerializationException if the state contains non-serializable types
     */
    suspend fun serializeState(state: Map<String, Any>): ByteArray

    /**
     * Deserialize plugin state from bytes.
     *
     * Restores a state map from a previously serialized byte array.
     *
     * @param data The byte array to deserialize
     * @return Map containing the restored state
     * @throws SerializationException if the data is invalid or corrupted
     */
    suspend fun deserializeState(data: ByteArray): Map<String, Any>
}

/**
 * JSON-based implementation of PluginStateSerializer.
 *
 * Uses kotlinx.serialization JSON to serialize state maps to JSON strings,
 * which are then encoded to UTF-8 bytes. This provides human-readable
 * serialization useful for debugging and inspection.
 *
 * ## Features
 * - Human-readable JSON format
 * - Pretty printing for debugging (optional)
 * - Handles nested maps and lists
 * - Supports all JSON-compatible primitive types
 *
 * ## Limitations
 * - Custom objects must be converted to primitives/maps before serialization
 * - Large binary data should be Base64 encoded as strings
 *
 * @param prettyPrint Whether to format JSON with indentation (default: false)
 * @since 1.0.0
 */
class JsonPluginStateSerializer(
    prettyPrint: Boolean = false
) : PluginStateSerializer {

    private val json = Json {
        this.prettyPrint = prettyPrint
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Serialize state map to JSON bytes.
     *
     * @param state The state map to serialize
     * @return UTF-8 encoded JSON bytes
     */
    override suspend fun serializeState(state: Map<String, Any>): ByteArray {
        val jsonElement = mapToJsonElement(state)
        val jsonString = json.encodeToString(jsonElement)
        return jsonString.encodeToByteArray()
    }

    /**
     * Deserialize JSON bytes to state map.
     *
     * @param data UTF-8 encoded JSON bytes
     * @return Restored state map
     */
    override suspend fun deserializeState(data: ByteArray): Map<String, Any> {
        val jsonString = data.decodeToString()
        val jsonElement = json.parseToJsonElement(jsonString)
        return jsonElementToMap(jsonElement)
    }

    /**
     * Convert a Map<String, Any> to JsonElement for serialization.
     *
     * @param map The map to convert
     * @return JsonElement representation
     */
    private fun mapToJsonElement(map: Map<String, Any>): JsonElement {
        val content = map.mapValues { (_, value) -> anyToJsonElement(value) }
        return JsonObject(content)
    }

    /**
     * Convert any value to JsonElement.
     *
     * @param value The value to convert
     * @return JsonElement representation
     * @throws IllegalArgumentException for unsupported types
     */
    @Suppress("UNCHECKED_CAST")
    private fun anyToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                val stringMap = value.entries.associate { (k, v) ->
                    k.toString() to anyToJsonElement(v)
                }
                JsonObject(stringMap)
            }
            is List<*> -> JsonArray(value.map { anyToJsonElement(it) })
            is Set<*> -> JsonArray(value.map { anyToJsonElement(it) })
            is Array<*> -> JsonArray(value.map { anyToJsonElement(it) })
            else -> {
                // Attempt to serialize as string for unknown types
                JsonPrimitive(value.toString())
            }
        }
    }

    /**
     * Convert JsonElement back to Map<String, Any>.
     *
     * @param element The JsonElement to convert
     * @return Map representation
     * @throws IllegalArgumentException if element is not a JsonObject
     */
    private fun jsonElementToMap(element: JsonElement): Map<String, Any> {
        require(element is JsonObject) { "Root element must be a JsonObject" }
        return element.entries.associate { (key, value) ->
            key to jsonElementToAny(value)
        }.filterValues { it != Unit } // Filter out null-like values
    }

    /**
     * Convert JsonElement to Any.
     *
     * @param element The JsonElement to convert
     * @return Converted value
     */
    private fun jsonElementToAny(element: JsonElement): Any {
        return when (element) {
            is JsonNull -> Unit // Use Unit as placeholder for null
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.floatOrNull != null -> element.float
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }
            is JsonObject -> {
                element.entries.associate { (key, value) ->
                    key to jsonElementToAny(value)
                }.filterValues { it != Unit }
            }
            is JsonArray -> {
                element.map { jsonElementToAny(it) }.filter { it != Unit }
            }
        }
    }

    companion object {
        /**
         * Default serializer instance with compact JSON output.
         */
        val Default = JsonPluginStateSerializer(prettyPrint = false)

        /**
         * Serializer instance with pretty-printed JSON output.
         * Useful for debugging and development.
         */
        val PrettyPrint = JsonPluginStateSerializer(prettyPrint = true)
    }
}

/**
 * Exception thrown when state serialization or deserialization fails.
 *
 * @param message Error message describing the failure
 * @param cause Underlying cause of the failure
 */
class StateSerializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

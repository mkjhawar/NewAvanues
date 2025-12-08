package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * State type definitions for plugin state variables.
 *
 * Supports primitive types (STRING, NUMBER, BOOLEAN, NULL) and
 * complex types (ARRAY, OBJECT) for hierarchical state structures.
 */
@Serializable
enum class StateType {
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY,
    OBJECT,
    NULL;

    /**
     * Validates that a JsonElement matches this type.
     */
    fun validate(value: JsonElement): Boolean {
        return when (this) {
            STRING -> value is JsonPrimitive && value.isString
            NUMBER -> value is JsonPrimitive && (value.intOrNull != null || value.doubleOrNull != null)
            BOOLEAN -> value is JsonPrimitive && value.booleanOrNull != null
            ARRAY -> value is JsonArray
            OBJECT -> value is JsonObject
            NULL -> value is JsonNull
        }
    }

    /**
     * Infers the StateType from a JsonElement.
     */
    companion object {
        fun from(value: JsonElement): StateType {
            return when (value) {
                is JsonNull -> NULL
                is JsonArray -> ARRAY
                is JsonObject -> OBJECT
                is JsonPrimitive -> when {
                    value.isString -> STRING
                    value.booleanOrNull != null -> BOOLEAN
                    value.intOrNull != null || value.doubleOrNull != null -> NUMBER
                    else -> STRING
                }
            }
        }
    }
}

/**
 * Definition of a state variable with type, default value, and persistence flag.
 *
 * @property type The data type of this variable
 * @property default The default value (must match type)
 * @property persist Whether this variable should be persisted across sessions
 * @property description Optional documentation for this variable
 */
@Serializable
data class StateVariable(
    val type: StateType,
    val default: JsonElement,
    val persist: Boolean = false,
    val description: String? = null
) {
    init {
        require(type.validate(default)) {
            "Default value $default does not match declared type $type"
        }
    }

    /**
     * Creates a copy of the default value for initialization.
     */
    fun createDefault(): JsonElement = default
}

/**
 * Complete state schema for a plugin, mapping variable names to their definitions.
 *
 * @property variables Map of variable names to their definitions
 */
@Serializable
data class StateSchema(
    val variables: Map<String, StateVariable>
) {
    /**
     * Validates that a complete state object conforms to this schema.
     *
     * @param state The state object to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(state: Map<String, JsonElement>): List<String> {
        val errors = mutableListOf<String>()

        // Check for missing required variables
        variables.forEach { (name, variable) ->
            if (name !in state) {
                errors.add("Missing required variable: $name")
            }
        }

        // Check for type mismatches
        state.forEach { (name, value) ->
            val variable = variables[name]
            if (variable == null) {
                errors.add("Unknown variable: $name")
            } else if (!variable.type.validate(value)) {
                errors.add("Variable $name has wrong type: expected ${variable.type}, got ${StateType.from(value)}")
            }
        }

        return errors
    }

    /**
     * Creates initial state from schema defaults.
     */
    fun createInitialState(): Map<String, JsonElement> {
        return variables.mapValues { (_, variable) -> variable.createDefault() }
    }

    /**
     * Gets all variables marked for persistence.
     */
    fun getPersistentVariables(): Map<String, StateVariable> {
        return variables.filterValues { it.persist }
    }

    /**
     * Gets the type of a variable by path (supports nested access like "config.theme").
     *
     * @param path Dot-separated path to the variable
     * @return The StateType if found, null otherwise
     */
    fun getTypeByPath(path: String): StateType? {
        val parts = path.split(".")
        val rootVar = variables[parts[0]] ?: return null

        if (parts.size == 1) {
            return rootVar.type
        }

        // For nested paths, we can't determine type from schema alone
        // (would need to traverse the default value)
        return null
    }

    companion object {
        /**
         * Creates a schema from a simple map of variable names to default values.
         * Types are inferred from the values.
         */
        fun fromDefaults(defaults: Map<String, JsonElement>): StateSchema {
            val variables = defaults.mapValues { (_, value) ->
                StateVariable(
                    type = StateType.from(value),
                    default = value,
                    persist = false
                )
            }
            return StateSchema(variables)
        }

        /**
         * Creates an empty schema (useful for testing or dynamic plugins).
         */
        fun empty(): StateSchema = StateSchema(emptyMap())
    }
}

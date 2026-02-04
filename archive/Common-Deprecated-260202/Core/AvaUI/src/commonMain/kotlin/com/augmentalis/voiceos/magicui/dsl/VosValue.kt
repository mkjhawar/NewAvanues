package com.augmentalis.avanues.avaui.dsl

import kotlinx.serialization.Serializable

/**
 * Type alias for ArrayValue. Used in parser for compatibility.
 */
typealias ListValue = VosValue.ArrayValue

/**
 * Represents runtime values in the VoiceOS DSL.
 *
 * This sealed class hierarchy covers all value types that can appear in DSL
 * property assignments, function arguments, and expressions. Values can be
 * primitive types (String, Int, Float, Boolean), collections (Array, Object),
 * or null.
 *
 * Example DSL values:
 * ```
 * text = "Hello World"           // StringValue
 * count = 42                      // IntValue
 * opacity = 0.75                  // FloatValue
 * enabled = true                  // BoolValue
 * colors = ["red", "green"]       // ArrayValue
 * config = { theme: "dark" }      // ObjectValue
 * placeholder = null              // NullValue
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:12:37 PDT
 *
 * @see VosAstNode for AST structure
 */
@Serializable
sealed class VosValue {

    /**
     * String literal value.
     *
     * Represents text values in the DSL. Strings can be single-quoted or
     * double-quoted in the DSL source.
     *
     * @property value The string content
     */
    @Serializable
    data class StringValue(val value: String) : VosValue() {
        override fun toString(): String = "\"$value\""

        companion object {
            /**
             * Creates an empty string value.
             * @return StringValue containing empty string
             */
            fun empty(): StringValue = StringValue("")
        }
    }

    /**
     * Integer numeric value.
     *
     * Represents whole numbers in the DSL (e.g., counts, indices, IDs).
     *
     * @property value The integer value
     */
    @Serializable
    data class IntValue(val value: Int) : VosValue() {
        override fun toString(): String = value.toString()

        companion object {
            /**
             * Integer value representing zero.
             */
            val ZERO = IntValue(0)

            /**
             * Integer value representing one.
             */
            val ONE = IntValue(1)
        }
    }

    /**
     * Floating-point numeric value.
     *
     * Represents decimal numbers in the DSL (e.g., opacity, scale, dimensions).
     *
     * @property value The floating-point value
     */
    @Serializable
    data class FloatValue(val value: Float) : VosValue() {
        override fun toString(): String = value.toString()

        companion object {
            /**
             * Float value representing zero.
             */
            val ZERO = FloatValue(0.0f)

            /**
             * Float value representing one (100%).
             */
            val ONE = FloatValue(1.0f)
        }
    }

    /**
     * Boolean value (true or false).
     *
     * Represents logical/boolean values in the DSL (e.g., enabled, visible).
     *
     * @property value The boolean value
     */
    @Serializable
    data class BoolValue(val value: Boolean) : VosValue() {
        override fun toString(): String = value.toString()

        companion object {
            /**
             * Boolean true value.
             */
            val TRUE = BoolValue(true)

            /**
             * Boolean false value.
             */
            val FALSE = BoolValue(false)
        }
    }

    /**
     * Array/list value.
     *
     * Represents ordered collections in the DSL. Arrays can contain mixed types.
     *
     * Example DSL:
     * ```
     * items = ["apple", "banana", "cherry"]
     * numbers = [1, 2, 3, 4, 5]
     * mixed = [42, "text", true]
     * ```
     *
     * @property items The array elements
     */
    @Serializable
    data class ArrayValue(val items: List<VosValue>) : VosValue() {
        override fun toString(): String = "[${items.joinToString(", ")}]"

        /**
         * Returns the number of elements in this array.
         */
        val size: Int get() = items.size

        /**
         * Returns true if this array is empty.
         */
        val isEmpty: Boolean get() = items.isEmpty()

        companion object {
            /**
             * Creates an empty array value.
             * @return ArrayValue with no elements
             */
            fun empty(): ArrayValue = ArrayValue(emptyList())

            /**
             * Creates an array from vararg values.
             * @param values Variable number of values
             * @return ArrayValue containing the provided values
             */
            fun of(vararg values: VosValue): ArrayValue = ArrayValue(values.toList())
        }
    }

    /**
     * Object/map value.
     *
     * Represents key-value structures in the DSL. Objects can contain nested
     * values and are commonly used for configuration.
     *
     * Example DSL:
     * ```
     * theme = {
     *     primaryColor: "#FF5733",
     *     fontSize: 16,
     *     darkMode: true
     * }
     * ```
     *
     * @property properties Map of property names to values
     */
    @Serializable
    data class ObjectValue(val properties: Map<String, VosValue>) : VosValue() {
        override fun toString(): String {
            val entries = properties.entries.joinToString(", ") { (key, value) ->
                "$key: $value"
            }
            return "{ $entries }"
        }

        /**
         * Returns the number of properties in this object.
         */
        val size: Int get() = properties.size

        /**
         * Returns true if this object has no properties.
         */
        val isEmpty: Boolean get() = properties.isEmpty()

        /**
         * Gets a property value by name, or null if not found.
         *
         * @param key Property name
         * @return Value associated with the key, or null
         */
        operator fun get(key: String): VosValue? = properties[key]

        companion object {
            /**
             * Creates an empty object value.
             * @return ObjectValue with no properties
             */
            fun empty(): ObjectValue = ObjectValue(emptyMap())

            /**
             * Creates an object from pairs.
             * @param pairs Variable number of key-value pairs
             * @return ObjectValue containing the provided properties
             */
            fun of(vararg pairs: Pair<String, VosValue>): ObjectValue =
                ObjectValue(pairs.toMap())
        }
    }

    /**
     * Null value.
     *
     * Represents the absence of a value in the DSL. Used for optional properties
     * or explicitly cleared values.
     *
     * Example DSL:
     * ```
     * placeholder = null
     * ```
     */
    @Serializable
    object NullValue : VosValue() {
        override fun toString(): String = "null"
    }
}

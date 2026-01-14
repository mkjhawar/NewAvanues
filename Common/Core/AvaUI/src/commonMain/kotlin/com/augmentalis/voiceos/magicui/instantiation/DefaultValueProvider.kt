package com.augmentalis.avanues.avaui.instantiation

import com.augmentalis.avanues.avaui.registry.PropertyType

/**
 * Provides default values for missing properties.
 *
 * The DefaultValueProvider supplies sensible default values for each property
 * type when no explicit default is specified in the property descriptor. These
 * defaults ensure that components can be instantiated even with minimal
 * configuration in the DSL.
 *
 * **Design Philosophy:**
 * Defaults are chosen to be:
 * - **Safe**: Won't cause crashes or unexpected behavior
 * - **Neutral**: Minimal visual/functional impact
 * - **Conventional**: Match common expectations (e.g., black text, opaque)
 *
 * **Type Defaults:**
 * - STRING → "" (empty string)
 * - INT → 0 (zero)
 * - FLOAT → 0.0f (zero)
 * - BOOLEAN → false (disabled/hidden by default)
 * - COLOR → "#000000" (black, fully opaque)
 * - ENUM → "" (empty string, component should validate)
 * - OBJECT → null (no object)
 *
 * Example usage:
 * ```kotlin
 * val defaultSize = DefaultValueProvider.getDefault(PropertyType.FLOAT) as Float  // 0.0f
 * val defaultColor = DefaultValueProvider.getDefault(PropertyType.COLOR) as String  // "#000000"
 * val defaultText = DefaultValueProvider.getDefault(PropertyType.STRING) as String  // ""
 * ```
 *
 * @see PropertyType for supported property types
 *
 * @since 1.0.0
 * Created: 2025-10-27 12:18:37 PDT
 * Created by Manoj Jhawar, manoj@ideahq.net
 */
object DefaultValueProvider {

    /**
     * Get default value for a property type.
     *
     * Returns a type-appropriate default value that can be safely used when a
     * property is not specified in the DSL and no explicit default is provided
     * in the property descriptor.
     *
     * **Return Types:**
     * - STRING → String
     * - INT → Int
     * - FLOAT → Float
     * - BOOLEAN → Boolean
     * - COLOR → String (hex format)
     * - ENUM → String (empty, should be validated by component)
     * - OBJECT → null
     *
     * **Usage Notes:**
     * - For BOOLEAN, we default to `false` (safe default for enable/visible flags)
     * - For COLOR, we return a hex string "#000000" rather than ColorRGBA to avoid
     *   premature type coercion
     * - For OBJECT, we return null rather than an empty map, allowing components
     *   to distinguish "not provided" from "empty object"
     *
     * @param type Property type from PropertyDescriptor
     * @return Default value appropriate for the type, or null for OBJECT
     */
    fun getDefault(type: PropertyType): Any? {
        return when (type) {
            PropertyType.STRING -> ""
            PropertyType.INT -> 0
            PropertyType.FLOAT -> 0f
            PropertyType.BOOLEAN -> false
            PropertyType.COLOR -> "#000000"
            PropertyType.ENUM -> ""
            PropertyType.OBJECT -> null
            PropertyType.LIST -> emptyList<Any>()
            PropertyType.ANY -> null
        }
    }

    /**
     * Get a default value with semantic context.
     *
     * This method provides context-aware defaults based on property name hints.
     * For example, a property named "opacity" might default to 1.0f (fully opaque)
     * rather than 0.0f, and "backgroundColor" might default to white instead of black.
     *
     * This is an advanced feature that improves DSL ergonomics by choosing more
     * appropriate defaults based on common naming patterns.
     *
     * Example:
     * ```kotlin
     * getDefaultWithContext(PropertyType.FLOAT, "opacity")  // 1.0f (fully opaque)
     * getDefaultWithContext(PropertyType.FLOAT, "fontSize")  // 16.0f (readable size)
     * getDefaultWithContext(PropertyType.COLOR, "backgroundColor")  // "#FFFFFF" (white)
     * ```
     *
     * @param type Property type
     * @param propertyName Property name for context
     * @return Context-aware default value
     */
    fun getDefaultWithContext(type: PropertyType, propertyName: String): Any? {
        // Context-aware defaults based on property name
        return when {
            // Opacity/alpha defaults to fully opaque
            propertyName.contains("opacity", ignoreCase = true) ||
                    propertyName.contains("alpha", ignoreCase = true) -> {
                when (type) {
                    PropertyType.FLOAT -> 1.0f
                    PropertyType.INT -> 255
                    else -> getDefault(type)
                }
            }

            // Background colors default to white
            propertyName.contains("background", ignoreCase = true) && type == PropertyType.COLOR -> {
                "#FFFFFF"
            }

            // Text colors default to black (already default)
            propertyName.contains("text", ignoreCase = true) ||
                    propertyName.contains("foreground", ignoreCase = true) -> {
                when (type) {
                    PropertyType.COLOR -> "#000000"
                    else -> getDefault(type)
                }
            }

            // Font sizes default to readable sizes
            propertyName.contains("size", ignoreCase = true) ||
                    propertyName.contains("fontSize", ignoreCase = true) -> {
                when (type) {
                    PropertyType.FLOAT -> 16.0f
                    PropertyType.INT -> 16
                    else -> getDefault(type)
                }
            }

            // Enabled/visible flags default to true (show by default)
            propertyName.contains("enabled", ignoreCase = true) ||
                    propertyName.contains("visible", ignoreCase = true) -> {
                when (type) {
                    PropertyType.BOOLEAN -> true
                    else -> getDefault(type)
                }
            }

            // Padding/margin defaults to reasonable spacing
            propertyName.contains("padding", ignoreCase = true) ||
                    propertyName.contains("margin", ignoreCase = true) ||
                    propertyName.contains("spacing", ignoreCase = true) -> {
                when (type) {
                    PropertyType.FLOAT -> 8.0f
                    PropertyType.INT -> 8
                    else -> getDefault(type)
                }
            }

            // Fall back to basic type defaults
            else -> getDefault(type)
        }
    }
}

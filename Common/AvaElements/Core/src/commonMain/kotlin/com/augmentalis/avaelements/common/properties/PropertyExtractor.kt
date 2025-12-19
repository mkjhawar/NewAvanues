package com.augmentalis.avaelements.common.properties

/**
 * Universal Property Extraction Utilities
 *
 * Platform-agnostic property extraction and default value handling
 * for component properties. Eliminates duplicate null-coalescing
 * and type conversion code across platform renderers.
 *
 * Used by: Android, iOS, Desktop, and Web renderers
 *
 * ARCHITECTURE: Facade Pattern
 * ═══════════════════════════════════════════════════════════════
 * This class serves as a facade that delegates to focused extractors:
 * - BasicPropertyExtractor: Primitives and enums
 * - CollectionPropertyExtractor: Lists and maps
 * - ColorPropertyExtractor: Color parsing
 * - DimensionPropertyExtractor: Dimensional values with units
 * - CallbackPropertyExtractor: Function callbacks
 *
 * BACKWARD COMPATIBILITY: 100%
 * All existing code using PropertyExtractor continues to work unchanged.
 * Extension functions at the bottom maintain the same API surface.
 *
 * SOLID PRINCIPLES:
 * - SRP: Each extractor has a single, focused responsibility
 * - ISP: Clients can depend on specific extractors instead of the entire interface
 * - DIP: Extractors are independent, facade coordinates them
 */
object PropertyExtractor {

    // ═══════════════════════════════════════════════════════════════
    // Basic Type Extraction (Delegation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract string property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getString(
        props: Map<String, Any?>,
        key: String,
        default: String = ""
    ): String = BasicPropertyExtractor.getString(props, key, default)

    /**
     * Extract nullable string
     * Delegates to: BasicPropertyExtractor
     */
    fun getStringOrNull(props: Map<String, Any?>, key: String): String? =
        BasicPropertyExtractor.getStringOrNull(props, key)

    /**
     * Extract boolean property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getBoolean(
        props: Map<String, Any?>,
        key: String,
        default: Boolean = false
    ): Boolean = BasicPropertyExtractor.getBoolean(props, key, default)

    /**
     * Extract int property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getInt(
        props: Map<String, Any?>,
        key: String,
        default: Int = 0
    ): Int = BasicPropertyExtractor.getInt(props, key, default)

    /**
     * Extract float property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getFloat(
        props: Map<String, Any?>,
        key: String,
        default: Float = 0f
    ): Float = BasicPropertyExtractor.getFloat(props, key, default)

    /**
     * Extract double property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getDouble(
        props: Map<String, Any?>,
        key: String,
        default: Double = 0.0
    ): Double = BasicPropertyExtractor.getDouble(props, key, default)

    /**
     * Extract long property with default
     * Delegates to: BasicPropertyExtractor
     */
    fun getLong(
        props: Map<String, Any?>,
        key: String,
        default: Long = 0L
    ): Long = BasicPropertyExtractor.getLong(props, key, default)

    /**
     * Extract enum property with default
     * Delegates to: BasicPropertyExtractor
     */
    inline fun <reified T : Enum<T>> getEnum(
        props: Map<String, Any?>,
        key: String,
        default: T
    ): T = BasicPropertyExtractor.getEnum(props, key, default)

    // ═══════════════════════════════════════════════════════════════
    // Collection Extraction (Delegation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract string list
     * Delegates to: CollectionPropertyExtractor
     */
    fun getStringList(
        props: Map<String, Any?>,
        key: String,
        default: List<String> = emptyList()
    ): List<String> = CollectionPropertyExtractor.getStringList(props, key, default)

    /**
     * Extract int list
     * Delegates to: CollectionPropertyExtractor
     */
    fun getIntList(
        props: Map<String, Any?>,
        key: String,
        default: List<Int> = emptyList()
    ): List<Int> = CollectionPropertyExtractor.getIntList(props, key, default)

    /**
     * Extract float list
     * Delegates to: CollectionPropertyExtractor
     */
    fun getFloatList(
        props: Map<String, Any?>,
        key: String,
        default: List<Float> = emptyList()
    ): List<Float> = CollectionPropertyExtractor.getFloatList(props, key, default)

    /**
     * Extract generic list with transformer
     * Delegates to: CollectionPropertyExtractor
     */
    fun <T> getList(
        props: Map<String, Any?>,
        key: String,
        transform: (Any) -> T?,
        default: List<T> = emptyList()
    ): List<T> = CollectionPropertyExtractor.getList(props, key, transform, default)

    /**
     * Extract nested map
     * Delegates to: CollectionPropertyExtractor
     */
    fun getMap(
        props: Map<String, Any?>,
        key: String,
        default: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> = CollectionPropertyExtractor.getMap(props, key, default)

    /**
     * Extract string map
     * Delegates to: CollectionPropertyExtractor
     */
    fun getStringMap(
        props: Map<String, Any?>,
        key: String,
        default: Map<String, String> = emptyMap()
    ): Map<String, String> = CollectionPropertyExtractor.getStringMap(props, key, default)

    // ═══════════════════════════════════════════════════════════════
    // Color Extraction (Delegation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract color as ARGB int
     * Delegates to: ColorPropertyExtractor
     */
    fun getColorArgb(
        props: Map<String, Any?>,
        key: String,
        default: Int = 0xFF000000.toInt()
    ): Int = ColorPropertyExtractor.getColorArgb(props, key, default)

    // ═══════════════════════════════════════════════════════════════
    // Dimension Extraction (Delegation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parse dimension value (supports dp, sp, px, %)
     * Delegates to: DimensionPropertyExtractor
     */
    fun getDimension(
        props: Map<String, Any?>,
        key: String,
        default: Float = 0f
    ): DimensionValue = DimensionPropertyExtractor.getDimension(props, key, default)

    // ═══════════════════════════════════════════════════════════════
    // Callback Extraction (Delegation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Extract callback function
     * Delegates to: CallbackPropertyExtractor
     */
    fun getCallback(
        props: Map<String, Any?>,
        key: String
    ): (() -> Unit)? = CallbackPropertyExtractor.getCallback(props, key)

    /**
     * Extract callback with single parameter
     * Delegates to: CallbackPropertyExtractor
     */
    fun <T> getCallback1(
        props: Map<String, Any?>,
        key: String
    ): ((T) -> Unit)? = CallbackPropertyExtractor.getCallback1(props, key)

    /**
     * Extract callback with two parameters
     * Delegates to: CallbackPropertyExtractor
     */
    fun <T1, T2> getCallback2(
        props: Map<String, Any?>,
        key: String
    ): ((T1, T2) -> Unit)? = CallbackPropertyExtractor.getCallback2(props, key)
}

// ═══════════════════════════════════════════════════════════════
// Extension Functions
// ═══════════════════════════════════════════════════════════════
// These maintain 100% backward compatibility with existing code

fun Map<String, Any?>.getString(key: String, default: String = "") =
    PropertyExtractor.getString(this, key, default)

fun Map<String, Any?>.getBoolean(key: String, default: Boolean = false) =
    PropertyExtractor.getBoolean(this, key, default)

fun Map<String, Any?>.getInt(key: String, default: Int = 0) =
    PropertyExtractor.getInt(this, key, default)

fun Map<String, Any?>.getFloat(key: String, default: Float = 0f) =
    PropertyExtractor.getFloat(this, key, default)

fun Map<String, Any?>.getDouble(key: String, default: Double = 0.0) =
    PropertyExtractor.getDouble(this, key, default)

inline fun <reified T : Enum<T>> Map<String, Any?>.getEnum(key: String, default: T) =
    PropertyExtractor.getEnum(this, key, default)

fun Map<String, Any?>.getStringList(key: String, default: List<String> = emptyList()) =
    PropertyExtractor.getStringList(this, key, default)

fun Map<String, Any?>.getColorArgb(key: String, default: Int = 0xFF000000.toInt()) =
    PropertyExtractor.getColorArgb(this, key, default)

fun Map<String, Any?>.getDimension(key: String, default: Float = 0f) =
    PropertyExtractor.getDimension(this, key, default)

fun Map<String, Any?>.getCallback(key: String) =
    PropertyExtractor.getCallback(this, key)

package com.augmentalis.avaelements.common.properties

/**
 * Callback Property Extractor
 *
 * Handles extraction of callback functions from property maps.
 * Part of the SOLID refactoring of PropertyExtractor.
 *
 * Responsibilities:
 * - Zero-parameter callback extraction
 * - Single-parameter callback extraction
 * - Two-parameter callback extraction
 *
 * Type-safe callback extraction with generic parameter support.
 *
 * SRP: Single responsibility - callback extraction only
 * ISP: Clients depend only on callback extraction methods
 */
object CallbackPropertyExtractor {

    // ─────────────────────────────────────────────────────────────
    // Callback Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract callback function with no parameters
     *
     * @param props Property map
     * @param key Property key
     * @return Callback function or null if not found/invalid
     */
    @Suppress("UNCHECKED_CAST")
    fun getCallback(
        props: Map<String, Any?>,
        key: String
    ): (() -> Unit)? {
        return props[key] as? (() -> Unit)
    }

    /**
     * Extract callback with single parameter
     *
     * @param T Parameter type
     * @param props Property map
     * @param key Property key
     * @return Callback function or null if not found/invalid
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCallback1(
        props: Map<String, Any?>,
        key: String
    ): ((T) -> Unit)? {
        return props[key] as? ((T) -> Unit)
    }

    /**
     * Extract callback with two parameters
     *
     * @param T1 First parameter type
     * @param T2 Second parameter type
     * @param props Property map
     * @param key Property key
     * @return Callback function or null if not found/invalid
     */
    @Suppress("UNCHECKED_CAST")
    fun <T1, T2> getCallback2(
        props: Map<String, Any?>,
        key: String
    ): ((T1, T2) -> Unit)? {
        return props[key] as? ((T1, T2) -> Unit)
    }
}

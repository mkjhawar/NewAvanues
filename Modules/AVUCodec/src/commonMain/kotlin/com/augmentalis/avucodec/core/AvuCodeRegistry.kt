package com.augmentalis.avucodec.core

/**
 * AVU Code Registry - Central registry for all AVU format codes
 *
 * This registry allows modules to register their codes, enabling:
 * - Self-documenting headers with code legends
 * - Runtime code lookup and validation
 * - Cross-module code discovery
 * - Documentation generation
 *
 * Usage:
 * ```kotlin
 * // Register codes at module initialization
 * AvuCodeRegistry.register(AvuCodeInfo(
 *     code = "SCR",
 *     name = "Sync Create",
 *     category = AvuCodeCategory.SYNC,
 *     format = "msgId:entityType:entityId:version:data"
 * ))
 *
 * // Generate legend for file header
 * val legend = AvuCodeRegistry.generateLegend(setOf("SCR", "SUP", "SDL"))
 * ```
 *
 * @author Augmentalis Engineering
 * @since AVU 2.2
 */
object AvuCodeRegistry {

    private val codes = mutableMapOf<String, AvuCodeInfo>()
    private val codesByCategory = mutableMapOf<AvuCodeCategory, MutableList<AvuCodeInfo>>()

    /**
     * Register a code with the registry.
     *
     * @param info The code metadata
     * @throws IllegalArgumentException if code is already registered with different info
     */
    fun register(info: AvuCodeInfo) {
        val existing = codes[info.code]
        if (existing != null && existing != info) {
            throw IllegalArgumentException(
                "Code ${info.code} already registered with different definition"
            )
        }
        codes[info.code] = info
        codesByCategory.getOrPut(info.category) { mutableListOf() }.add(info)
    }

    /**
     * Register multiple codes at once.
     *
     * @param infos List of code metadata
     */
    fun registerAll(vararg infos: AvuCodeInfo) {
        infos.forEach { register(it) }
    }

    /**
     * Get code info by code.
     *
     * @param code The 3-letter code
     * @return Code info or null if not registered
     */
    fun get(code: String): AvuCodeInfo? = codes[code]

    /**
     * Get all codes in a category.
     *
     * @param category The code category
     * @return List of codes in that category
     */
    fun getByCategory(category: AvuCodeCategory): List<AvuCodeInfo> {
        return codesByCategory[category]?.toList() ?: emptyList()
    }

    /**
     * Get all registered codes.
     *
     * @return Map of code to info
     */
    fun getAll(): Map<String, AvuCodeInfo> = codes.toMap()

    /**
     * Check if a code is registered.
     *
     * @param code The 3-letter code
     * @return true if registered
     */
    fun isRegistered(code: String): Boolean = codes.containsKey(code)

    /**
     * Generate a code legend for file headers.
     *
     * @param filter Optional set of codes to include (null = all)
     * @param includeDescriptions Whether to include descriptions
     * @return Formatted legend string
     */
    fun generateLegend(
        filter: Set<String>? = null,
        includeDescriptions: Boolean = false
    ): String = buildString {
        appendLine("codes:")
        val filtered = if (filter != null) {
            codes.filterKeys { it in filter }
        } else {
            codes
        }

        // Group by category for readability
        val grouped = filtered.values.groupBy { it.category }
        grouped.forEach { (category, categoryInfos) ->
            if (grouped.size > 1) {
                appendLine("  # ${category.displayName}")
            }
            categoryInfos.sortedBy { it.code }.forEach { info ->
                append("  ")
                appendLine(info.toLegendEntry(includeDescriptions))
            }
        }
    }

    /**
     * Generate a compact legend (codes and formats only).
     *
     * @param codes Set of codes to include
     * @return Compact legend string
     */
    fun generateCompactLegend(codes: Set<String>): String = buildString {
        appendLine("codes:")
        codes.sorted().forEach { code ->
            this@AvuCodeRegistry.codes[code]?.let { info ->
                appendLine("  ${info.toCompactLegend()}")
            }
        }
    }

    /**
     * Generate documentation for all registered codes.
     *
     * @return Markdown-formatted documentation
     */
    fun generateDocumentation(): String = buildString {
        appendLine("# AVU Code Reference")
        appendLine()

        val grouped = codes.values.groupBy { it.category }
        grouped.forEach { (category, categoryInfos) ->
            appendLine("## ${category.displayName} Codes")
            appendLine()
            appendLine("| Code | Name | Format | Description |")
            appendLine("|------|------|--------|-------------|")
            categoryInfos.sortedBy { it.code }.forEach { info ->
                appendLine("| `${info.code}` | ${info.name} | `${info.format}` | ${info.description} |")
            }
            appendLine()
        }
    }

    /**
     * Validate a message against registered code formats.
     *
     * @param message The AVU message string
     * @return Validation result
     */
    fun validate(message: String): ValidationResult {
        if (message.length < 4) {
            return ValidationResult.Invalid("Message too short")
        }

        val parts = message.split(":", limit = 2)
        if (parts.isEmpty()) {
            return ValidationResult.Invalid("No code found")
        }

        val code = parts[0]
        if (code.length != 3 || !code.all { it.isUpperCase() }) {
            return ValidationResult.Invalid("Invalid code format: $code")
        }

        val info = codes[code]
            ?: return ValidationResult.UnknownCode(code)

        // Count expected fields from format
        val expectedFields = info.format.split(":").size
        val actualFields = message.split(":").size - 1 // -1 for code itself

        return if (actualFields >= info.fields.count { it.required }) {
            ValidationResult.Valid(info)
        } else {
            ValidationResult.Invalid(
                "Expected at least ${info.fields.count { it.required }} fields, got $actualFields"
            )
        }
    }

    /**
     * Clear all registered codes (useful for testing).
     */
    fun clear() {
        codes.clear()
        codesByCategory.clear()
    }

    /**
     * Get count of registered codes.
     */
    fun count(): Int = codes.size
}

/**
 * Result of message validation.
 */
sealed class ValidationResult {
    data class Valid(val info: AvuCodeInfo) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
    data class UnknownCode(val code: String) : ValidationResult()
}

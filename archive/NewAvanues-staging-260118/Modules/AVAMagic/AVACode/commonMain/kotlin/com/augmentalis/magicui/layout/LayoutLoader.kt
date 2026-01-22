package com.augmentalis.avamagic.layout

import com.augmentalis.avamagic.core.ComponentModel

/**
 * Interface for loading UI layouts from various sources and formats.
 *
 * Implementations must support:
 * - Multiple input formats (YAML, JSON, DSL)
 * - Format auto-detection
 * - Schema validation
 * - Error recovery with detailed messages
 *
 * ## Implementation Contract
 *
 * All implementations MUST:
 * 1. Validate against schema before parsing
 * 2. Return `Result<List<ComponentModel>>` (never throw exceptions)
 * 3. Provide clear error messages with line numbers for syntax errors
 * 4. Enforce read-only file access (FR-109)
 *
 * @since 3.1.0
 */
interface LayoutLoader {
    /**
     * Loads a layout from the given source string.
     *
     * @param source Layout definition (YAML, JSON, or DSL)
     * @param format Expected format (use [LayoutFormat.AUTO] for detection)
     * @return Result containing component list or error details
     */
    fun load(source: String, format: LayoutFormat): Result<List<ComponentModel>>

    /**
     * Automatically detects the format of the given source.
     *
     * Detection heuristics:
     * - **DSL**: Single-line or contains `:` and `[` without `{`
     * - **YAML**: Multi-line with `-` or `:` with indentation
     * - **JSON**: Starts with `{` or `[`
     *
     * @param source Layout source to analyze
     * @return Detected format (never returns [LayoutFormat.AUTO])
     */
    fun autoDetect(source: String): LayoutFormat
}

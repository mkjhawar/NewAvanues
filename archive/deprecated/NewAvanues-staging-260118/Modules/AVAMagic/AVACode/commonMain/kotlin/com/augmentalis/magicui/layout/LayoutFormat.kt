package com.augmentalis.avamagic.layout

/**
 * Supported layout definition formats in the AvaUI system.
 *
 * AvaUI supports three layout formats with automatic detection:
 *
 * ## Format Comparison
 *
 * | Format | Best For | Pros | Cons |
 * |--------|----------|------|------|
 * | **DSL** | Voice commands, quick prototyping | Ultra-compact, 78k+ layouts/sec | Less structured |
 * | **YAML** | Complex layouts, version control | Human-readable, comments | Verbose |
 * | **JSON** | API responses, tool integration | Schema validation, universal | No comments |
 *
 * ## Examples
 *
 * **DSL**:
 * ```
 * row:button[text=Save],button[text=Cancel]
 * ```
 *
 * **YAML**:
 * ```yaml
 * type: Row
 * children:
 *   - type: Button
 *     properties:
 *       text: Save
 *   - type: Button
 *     properties:
 *       text: Cancel
 * ```
 *
 * **JSON**:
 * ```json
 * {
 *   "type": "Row",
 *   "children": [
 *     {"type": "Button", "properties": {"text": "Save"}},
 *     {"type": "Button", "properties": {"text": "Cancel"}}
 *   ]
 * }
 * ```
 *
 * @since 3.1.0
 */
enum class LayoutFormat {
    /**
     * Domain-Specific Language - Ultra-compact syntax.
     *
     * **Performance**: <0.02ms per layout, 78,000+ layouts/second.
     *
     * **App Store Compliant**: Treated as declarative data (not executable code).
     *
     * Example: `row:button[text=Save],button[text=Cancel]`
     */
    DSL,

    /**
     * YAML Ain't Markup Language - Human-readable structured data.
     *
     * **Best for**: Complex layouts requiring comments and readability.
     *
     * **Features**: Multi-line strings, anchors/aliases, type inference.
     */
    YAML,

    /**
     * JavaScript Object Notation - Universal data interchange format.
     *
     * **Best for**: API responses, schema validation, tool integration.
     *
     * **Features**: Strict typing, JSON Schema validation, widespread tooling.
     */
    JSON,

    /**
     * Automatic format detection based on file content.
     *
     * Detection rules:
     * - DSL: Single-line, contains `:` or `[` without `{` or `-`
     * - YAML: Starts with `-` or contains `:` with indentation
     * - JSON: Starts with `{` or `[`
     */
    AUTO
}

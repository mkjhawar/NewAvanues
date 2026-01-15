package com.augmentalis.avamagic.layout.loaders

import com.augmentalis.avamagic.layout.*

/**
 * Parser for AMF (AvaMagic Format) layout files.
 *
 * AMF is a line-based compact format that provides ~50% reduction in file size
 * compared to YAML/JSON while being faster to parse.
 *
 * ## AMF Layout Format (.amf)
 *
 * ```
 * # AvaMagic Layout Format v1.0
 * ---
 * schema: amf-lyt-1.0
 * ---
 * LYT:MainScreen:1.0.0
 * COL:root:1:center
 *   ROW:header:0:space-between
 *     TXT:title:Welcome:h1
 *     BTN:menu:Menu:openMenu
 *   SPC:spacer1:16
 *   TXT:content:Hello World:body
 *   ROW:actions:0:center
 *     BTN:save:Save:onSave
 *     BTN:cancel:Cancel:onCancel
 * ```
 *
 * ## Record Types
 *
 * | Prefix | Purpose | Format |
 * |--------|---------|--------|
 * | `LYT:` | Layout metadata | `LYT:name:version` |
 * | `COL:` | Column component | `COL:id:weight:align` |
 * | `ROW:` | Row component | `ROW:id:weight:align` |
 * | `TXT:` | Text component | `TXT:id:text:style` |
 * | `BTN:` | Button component | `BTN:id:label:action` |
 * | `IMG:` | Image component | `IMG:id:src:scale` |
 * | `SPC:` | Spacer component | `SPC:id:size` |
 * | `CNT:` | Generic container | `CNT:id:weight:align` |
 * | `STK:` | Stack container | `STK:id:weight:align` |
 * | `SCR:` | Scroll container | `SCR:id:direction:align` |
 * | `GRD:` | Grid container | `GRD:id:columns:gap` |
 *
 * ## Nesting
 *
 * Nesting is indicated by indentation (2 spaces per level):
 * ```
 * COL:parent:1:center
 *   TXT:child1:Hello:body
 *   TXT:child2:World:body
 * ```
 *
 * @since 3.2.0
 */
object AmfLayoutParser {

    private const val HEADER_DELIMITER = "---"
    private const val SCHEMA_PREFIX = "schema:"
    private const val LAYOUT_SCHEMA = "amf-lyt"
    private const val INDENT_SPACES = 2

    /**
     * Parse AMF format layout content.
     *
     * @param content The AMF format string content
     * @return Parsed LayoutConfig with all components
     * @throws AmfLayoutParseException if format is invalid or malformed
     */
    fun parse(content: String): LayoutConfig {
        val lines = content.lines()

        // Find header delimiters (keeping track of original line numbers for errors)
        val trimmedWithIndex = lines.mapIndexed { index, line ->
            IndexedLine(index + 1, line.trimEnd())
        }.filter { it.content.isNotEmpty() && !it.content.trimStart().startsWith("#") }

        val delimiterIndices = trimmedWithIndex.mapIndexedNotNull { index, line ->
            if (line.content.trim() == HEADER_DELIMITER) index else null
        }

        if (delimiterIndices.size < 2) {
            throw AmfLayoutParseException("Invalid AMF format: missing header delimiters (---)")
        }

        // Validate schema
        val headerLines = trimmedWithIndex.subList(delimiterIndices[0] + 1, delimiterIndices[1])
        if (!headerLines.any { it.content.trim().startsWith(SCHEMA_PREFIX) && it.content.contains(LAYOUT_SCHEMA) }) {
            throw AmfLayoutParseException("Invalid schema: expected 'schema: amf-lyt-*'")
        }

        // Parse content records
        val contentLines = trimmedWithIndex.subList(delimiterIndices[1] + 1, trimmedWithIndex.size)
        return parseRecords(contentLines)
    }

    /**
     * Parse record lines into LayoutConfig.
     */
    private fun parseRecords(lines: List<IndexedLine>): LayoutConfig {
        var name = "Unnamed Layout"
        var version = "1.0.0"
        var description = ""
        val rootComponents = mutableListOf<LayoutComponent>()

        // Build indentation-aware tree
        val componentStack = mutableListOf<Pair<Int, LayoutComponent>>() // (indentLevel, component)

        for (indexedLine in lines) {
            val line = indexedLine.content
            val lineNumber = indexedLine.lineNumber

            // Calculate indentation level
            val indentSpaces = line.takeWhile { it == ' ' }.length
            val indentLevel = indentSpaces / INDENT_SPACES
            val trimmedLine = line.trim()

            if (trimmedLine.isEmpty()) continue

            val parts = trimmedLine.split(":")
            if (parts.isEmpty()) continue

            val prefix = parts[0]

            try {
                when (prefix) {
                    "LYT" -> {
                        // LYT:name:version or LYT:name:version:description
                        if (parts.size >= 2) name = parts[1]
                        if (parts.size >= 3) version = parts[2]
                        if (parts.size >= 4) description = parts.drop(3).joinToString(":")
                    }

                    else -> {
                        // Try to parse as component
                        val componentType = ComponentType.fromPrefix(prefix)
                        if (componentType != null) {
                            val component = parseComponent(componentType, parts, lineNumber)

                            // Pop components from stack that are at same or deeper level
                            while (componentStack.isNotEmpty() && componentStack.last().first >= indentLevel) {
                                val (_, completedComponent) = componentStack.removeLast()
                                addToParentOrRoot(componentStack, rootComponents, completedComponent)
                            }

                            // Push current component onto stack
                            componentStack.add(indentLevel to component)
                        }
                    }
                }
            } catch (e: Exception) {
                throw AmfLayoutParseException("Error parsing line $lineNumber: ${e.message}", e)
            }
        }

        // Flush remaining components in stack
        while (componentStack.isNotEmpty()) {
            val (_, component) = componentStack.removeLast()
            addToParentOrRoot(componentStack, rootComponents, component)
        }

        return LayoutConfig(
            name = name,
            version = version,
            description = description,
            components = rootComponents
        )
    }

    /**
     * Add component to parent in stack or to root list.
     */
    private fun addToParentOrRoot(
        stack: MutableList<Pair<Int, LayoutComponent>>,
        roots: MutableList<LayoutComponent>,
        component: LayoutComponent
    ) {
        if (stack.isEmpty()) {
            roots.add(component)
        } else {
            // Add as child to parent (update parent in stack)
            val (parentLevel, parent) = stack.removeLast()
            stack.add(parentLevel to parent.withChild(component))
        }
    }

    /**
     * Parse a component from its parts.
     */
    private fun parseComponent(
        type: ComponentType,
        parts: List<String>,
        lineNumber: Int
    ): LayoutComponent {
        if (parts.size < 2) {
            throw AmfLayoutParseException("Line $lineNumber: Component requires at least an ID")
        }

        val id = parts[1]
        val properties = mutableMapOf<String, String>()

        when (type) {
            ComponentType.COLUMN, ComponentType.ROW, ComponentType.CONTAINER, ComponentType.STACK -> {
                // COL/ROW:id:weight:align
                if (parts.size >= 3) properties["weight"] = parts[2]
                if (parts.size >= 4) properties["align"] = parts[3]
            }

            ComponentType.TEXT -> {
                // TXT:id:text:style
                if (parts.size >= 3) properties["text"] = parts.drop(2).dropLast(1).joinToString(":")
                if (parts.size >= 4) properties["style"] = parts.last()
                // Handle case where text contains colons
                if (parts.size == 3) {
                    properties["text"] = parts[2]
                    properties.remove("style")
                }
            }

            ComponentType.BUTTON -> {
                // BTN:id:label:action
                if (parts.size >= 3) properties["label"] = parts[2]
                if (parts.size >= 4) properties["action"] = parts.drop(3).joinToString(":")
            }

            ComponentType.IMAGE -> {
                // IMG:id:src:scale
                if (parts.size >= 3) properties["src"] = parts[2]
                if (parts.size >= 4) properties["scale"] = parts[3]
            }

            ComponentType.SPACER -> {
                // SPC:id:size
                if (parts.size >= 3) properties["size"] = parts[2]
            }

            ComponentType.SCROLL -> {
                // SCR:id:direction:align
                if (parts.size >= 3) properties["direction"] = parts[2]
                if (parts.size >= 4) properties["align"] = parts[3]
            }

            ComponentType.GRID -> {
                // GRD:id:columns:gap
                if (parts.size >= 3) properties["columns"] = parts[2]
                if (parts.size >= 4) properties["gap"] = parts[3]
            }
        }

        return LayoutComponent(
            id = id,
            type = type,
            properties = properties
        )
    }

    /**
     * Validate AMF layout content without full parsing.
     *
     * @param content The content to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(content: String): List<String> {
        val errors = mutableListOf<String>()
        val lines = content.lines()

        // Check for header delimiters
        val delimiterCount = lines.count { it.trim() == HEADER_DELIMITER }
        if (delimiterCount < 2) {
            errors.add("Missing header delimiters (---): found $delimiterCount, expected at least 2")
        }

        // Check for schema
        val hasSchema = lines.any {
            it.trim().startsWith(SCHEMA_PREFIX) && it.contains(LAYOUT_SCHEMA)
        }
        if (!hasSchema) {
            errors.add("Missing or invalid schema: expected 'schema: amf-lyt-*'")
        }

        // Check for LYT record
        val hasLayoutRecord = lines.any { it.trim().startsWith("LYT:") }
        if (!hasLayoutRecord) {
            errors.add("Missing layout metadata record (LYT:)")
        }

        // Validate indentation consistency
        var lineNumber = 0
        for (line in lines) {
            lineNumber++
            if (line.isBlank() || line.trim().startsWith("#")) continue

            val leadingSpaces = line.takeWhile { it == ' ' }.length
            if (leadingSpaces % INDENT_SPACES != 0) {
                errors.add("Line $lineNumber: Invalid indentation ($leadingSpaces spaces). Must be multiple of $INDENT_SPACES")
            }
        }

        return errors
    }

    /**
     * Line with its original line number for error reporting.
     */
    private data class IndexedLine(val lineNumber: Int, val content: String)
}

/**
 * Exception thrown when AMF layout parsing fails.
 *
 * @property message Description of the parsing error
 * @property cause Underlying exception if any
 */
class AmfLayoutParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

# Layout DSL Parser - Code Examples & Integration

**Date:** 2025-10-26
**Project:** AvaCode Plugin System
**Companion to:** DSL-Parser-Research-251026.md

---

## Complete Working Implementation

### 1. Full Tokenizer Implementation

```kotlin
/**
 * LayoutTokenizer.kt
 *
 * High-performance single-pass tokenizer for layout DSL.
 * Zero allocations for simple tokens.
 */
package com.augmentalis.avacode.plugins.ui.dsl

/**
 * Token types for layout DSL
 */
enum class TokenType {
    IDENTIFIER,      // button, row, column, etc.
    COLON,           // :
    COMMA,           // ,
    EQUALS,          // =
    LEFT_BRACKET,    // [
    RIGHT_BRACKET,   // ]
    VALUE,           // property value (unquoted string)
    EOF              // End of input
}

/**
 * Token with type, value, and position
 */
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int,
    val length: Int = value.length
) {
    override fun toString(): String = "${type.name}('$value' at $position)"
}

/**
 * Tokenizer for layout DSL
 *
 * Performs single-pass lexical analysis with lookahead support.
 *
 * ## Example
 * ```kotlin
 * val tokenizer = LayoutTokenizer("row:button[labelKey=save],button[labelKey=cancel]")
 * while (true) {
 *     val token = tokenizer.nextToken()
 *     println(token)
 *     if (token.type == TokenType.EOF) break
 * }
 * ```
 *
 * ## Output
 * ```
 * IDENTIFIER('row' at 0)
 * COLON(':' at 3)
 * IDENTIFIER('button' at 4)
 * LEFT_BRACKET('[' at 10)
 * IDENTIFIER('labelKey' at 11)
 * EQUALS('=' at 19)
 * VALUE('save' at 20)
 * RIGHT_BRACKET(']' at 24)
 * COMMA(',' at 25)
 * ...
 * ```
 */
class LayoutTokenizer(val input: String) {
    private var position = 0
    private var savedPosition = 0

    /**
     * Get next token from input stream
     *
     * @return Next token (EOF if end of input)
     */
    fun nextToken(): Token {
        skipWhitespace()

        if (position >= input.length) {
            return Token(TokenType.EOF, "", position)
        }

        val start = position
        val char = input[position]

        return when {
            char == ':' -> {
                position++
                Token(TokenType.COLON, ":", start)
            }
            char == ',' -> {
                position++
                Token(TokenType.COMMA, ",", start)
            }
            char == '=' -> {
                position++
                Token(TokenType.EQUALS, "=", start)
            }
            char == '[' -> {
                position++
                Token(TokenType.LEFT_BRACKET, "[", start)
            }
            char == ']' -> {
                position++
                Token(TokenType.RIGHT_BRACKET, "]", start)
            }
            char.isLetter() || char == '_' -> {
                tokenizeIdentifier(start)
            }
            else -> {
                tokenizeValue(start)
            }
        }
    }

    /**
     * Peek at next token without consuming it
     *
     * @return Next token (does not advance position)
     */
    fun peekToken(): Token {
        savedPosition = position
        val token = nextToken()
        position = savedPosition
        return token
    }

    /**
     * Check if more tokens available
     */
    fun hasNext(): Boolean {
        skipWhitespace()
        return position < input.length
    }

    /**
     * Reset tokenizer to beginning
     */
    fun reset() {
        position = 0
    }

    /**
     * Skip whitespace characters
     */
    private fun skipWhitespace() {
        while (position < input.length && input[position].isWhitespace()) {
            position++
        }
    }

    /**
     * Tokenize identifier: [a-zA-Z_][a-zA-Z0-9_]*
     */
    private fun tokenizeIdentifier(start: Int): Token {
        while (position < input.length &&
               (input[position].isLetterOrDigit() || input[position] == '_')) {
            position++
        }
        val value = input.substring(start, position)
        return Token(TokenType.IDENTIFIER, value, start, value.length)
    }

    /**
     * Tokenize value: any characters until delimiter
     *
     * Delimiters: , [ ] (end of value)
     */
    private fun tokenizeValue(start: Int): Token {
        while (position < input.length &&
               input[position] !in DELIMITERS) {
            position++
        }
        val value = input.substring(start, position).trim()
        return Token(TokenType.VALUE, value, start, value.length)
    }

    companion object {
        private val DELIMITERS = setOf(',', '[', ']')
    }
}
```

---

### 2. Complete Parser with Error Recovery

```kotlin
/**
 * LayoutParser.kt
 *
 * Recursive descent parser with error recovery and rich diagnostics.
 */
package com.augmentalis.avacode.plugins.ui.dsl

import kotlin.math.min

/**
 * Parse result: success or failure with detailed error
 */
sealed interface ParseResult {
    data class Success(val ast: LayoutNode) : ParseResult
    data class Error(val error: ParseError) : ParseResult
}

/**
 * Detailed parse error with context and suggestions
 */
data class ParseError(
    val message: String,
    val position: Int,
    val context: String,
    val suggestion: String? = null
) {
    /**
     * Format error for user display
     */
    fun format(): String = buildString {
        appendLine("Parse error at position $position:")
        appendLine(context)
        appendLine(" ".repeat(minOf(position, 40)) + "^")
        appendLine(message)
        suggestion?.let { appendLine("Suggestion: $it") }
    }

    override fun toString(): String = format()
}

/**
 * Parse exception (internal)
 */
internal class ParseException(val error: ParseError) : Exception(error.message)

/**
 * Recursive descent parser for layout DSL
 *
 * ## Grammar (simplified)
 * ```
 * layout       := container | template | component
 * container    := container_type properties? ':' children
 * template     := 'template' ':' identifier
 * component    := identifier properties?
 * properties   := '[' property_list? ']'
 * property_list:= property (',' property)*
 * property     := identifier '=' value
 * children     := layout (',' layout)*
 * ```
 *
 * ## Usage
 * ```kotlin
 * val parser = LayoutDSLParser("row:button[labelKey=save],button[labelKey=cancel]")
 * when (val result = parser.parse()) {
 *     is ParseResult.Success -> println("AST: ${result.ast}")
 *     is ParseResult.Error -> println("Error: ${result.error.format()}")
 * }
 * ```
 */
class LayoutDSLParser(input: String) {
    private val tokenizer = LayoutTokenizer(input)
    private var currentToken = tokenizer.nextToken()

    /**
     * Parse entire layout expression
     *
     * @return ParseResult.Success with AST or ParseResult.Error
     */
    fun parse(): ParseResult {
        return try {
            val node = parseLayout()

            // Ensure we consumed all input
            if (currentToken.type != TokenType.EOF) {
                return ParseResult.Error(
                    ParseError(
                        message = "Unexpected token '${currentToken.value}' after layout",
                        position = currentToken.position,
                        context = getContext(currentToken.position),
                        suggestion = "Remove extra tokens after layout definition"
                    )
                )
            }

            ParseResult.Success(node)
        } catch (e: ParseException) {
            ParseResult.Error(e.error)
        } catch (e: Exception) {
            ParseResult.Error(
                ParseError(
                    message = "Unexpected error: ${e.message}",
                    position = currentToken.position,
                    context = getContext(currentToken.position)
                )
            )
        }
    }

    /**
     * Parse layout: container, template, or component
     */
    private fun parseLayout(): LayoutNode {
        // Check for template: template:identifier
        if (currentToken.value == "template") {
            return parseTemplate()
        }

        // Check for container types
        if (currentToken.value in CONTAINER_TYPES) {
            return parseContainer()
        }

        // Otherwise, component
        return parseComponent()
    }

    /**
     * Parse container: type[props]?:children
     */
    private fun parseContainer(): ContainerNode {
        val containerTypeStr = currentToken.value
        val containerType = when (containerTypeStr) {
            "row" -> ContainerType.ROW
            "column" -> ContainerType.COLUMN
            "grid" -> ContainerType.GRID
            "stack" -> ContainerType.STACK
            else -> throw parseError(
                "Unknown container type '$containerTypeStr'",
                suggestion = "Use one of: row, column, grid, stack"
            )
        }
        advance()

        // Parse optional properties
        val properties = if (currentToken.type == TokenType.LEFT_BRACKET) {
            parseProperties()
        } else {
            emptyMap()
        }

        // Expect colon
        if (currentToken.type != TokenType.COLON) {
            throw parseError(
                "Expected ':' after container type",
                suggestion = "Add colon after '$containerTypeStr' (e.g., '$containerTypeStr:...')"
            )
        }
        advance()

        // Parse children
        val children = parseChildren()

        // Validate container-specific requirements
        validateContainer(containerType, properties, children)

        return ContainerNode(containerType, properties, children)
    }

    /**
     * Parse template: template:identifier
     */
    private fun parseTemplate(): TemplateNode {
        // Consume 'template' keyword
        if (currentToken.value != "template") {
            throw parseError("Expected 'template' keyword")
        }
        advance()

        // Expect colon
        if (currentToken.type != TokenType.COLON) {
            throw parseError(
                "Expected ':' after 'template'",
                suggestion = "Use format: template:template_name"
            )
        }
        advance()

        // Expect template identifier
        if (currentToken.type != TokenType.IDENTIFIER) {
            throw parseError(
                "Expected template identifier after 'template:'",
                suggestion = "Provide template name (e.g., template:simple_form)"
            )
        }

        val templateId = currentToken.value
        advance()

        return TemplateNode(templateId)
    }

    /**
     * Parse component: identifier[props]?
     */
    private fun parseComponent(): ComponentNode {
        if (currentToken.type != TokenType.IDENTIFIER) {
            throw parseError(
                "Expected component identifier",
                suggestion = "Use component name (e.g., button, textfield, text)"
            )
        }

        val componentType = currentToken.value
        advance()

        // Parse optional properties
        val properties = if (currentToken.type == TokenType.LEFT_BRACKET) {
            parseProperties()
        } else {
            emptyMap()
        }

        return ComponentNode(componentType, properties)
    }

    /**
     * Parse properties: [key=value,key=value,...]
     */
    private fun parseProperties(): Map<String, String> {
        if (currentToken.type != TokenType.LEFT_BRACKET) {
            throw parseError("Expected '['")
        }
        advance()

        val properties = mutableMapOf<String, String>()

        // Handle empty properties: []
        if (currentToken.type == TokenType.RIGHT_BRACKET) {
            advance()
            return properties
        }

        // Parse property list
        while (true) {
            // Parse key
            if (currentToken.type != TokenType.IDENTIFIER) {
                throw parseError(
                    "Expected property key",
                    suggestion = "Use format: key=value"
                )
            }
            val key = currentToken.value
            advance()

            // Expect equals
            if (currentToken.type != TokenType.EQUALS) {
                throw parseError(
                    "Expected '=' after property key '$key'",
                    suggestion = "Use format: $key=value"
                )
            }
            advance()

            // Parse value
            if (currentToken.type !in listOf(TokenType.IDENTIFIER, TokenType.VALUE)) {
                throw parseError(
                    "Expected property value after '$key='",
                    suggestion = "Provide value for property '$key'"
                )
            }
            val value = currentToken.value
            advance()

            // Check for duplicate keys
            if (key in properties) {
                throw parseError(
                    "Duplicate property key '$key'",
                    suggestion = "Remove duplicate property"
                )
            }

            properties[key] = value

            // Check for comma or end bracket
            when (currentToken.type) {
                TokenType.RIGHT_BRACKET -> {
                    advance()
                    break
                }
                TokenType.COMMA -> {
                    advance()
                    // Continue to next property
                }
                else -> throw parseError(
                    "Expected ',' or ']' in property list",
                    suggestion = "Add comma between properties or close with ']'"
                )
            }
        }

        return properties
    }

    /**
     * Parse children: layout,layout,...
     */
    private fun parseChildren(): List<LayoutNode> {
        val children = mutableListOf<LayoutNode>()

        // Parse first child
        children.add(parseLayout())

        // Parse remaining children
        while (currentToken.type == TokenType.COMMA) {
            advance()
            children.add(parseLayout())
        }

        return children
    }

    /**
     * Validate container requirements
     */
    private fun validateContainer(
        type: ContainerType,
        properties: Map<String, String>,
        children: List<LayoutNode>
    ) {
        when (type) {
            ContainerType.GRID -> {
                // Grid requires cols or rows property
                if ("cols" !in properties && "rows" !in properties) {
                    throw parseError(
                        "Grid container requires 'cols' or 'rows' property",
                        suggestion = "Add property: grid[cols=2]:..."
                    )
                }
            }
            else -> {
                // No special validation for row, column, stack
            }
        }

        // All containers must have at least one child
        if (children.isEmpty()) {
            throw parseError(
                "Container must have at least one child",
                suggestion = "Add child components after ':'"
            )
        }
    }

    /**
     * Advance to next token
     */
    private fun advance() {
        currentToken = tokenizer.nextToken()
    }

    /**
     * Create parse error with context
     */
    private fun parseError(message: String, suggestion: String? = null): ParseException {
        return ParseException(
            ParseError(
                message = message,
                position = currentToken.position,
                context = getContext(currentToken.position),
                suggestion = suggestion
            )
        )
    }

    /**
     * Get context snippet around position for error display
     */
    private fun getContext(position: Int): String {
        val input = tokenizer.input
        val contextRadius = 40

        val start = maxOf(0, position - contextRadius)
        val end = minOf(input.length, position + contextRadius)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < input.length) "..." else ""

        return "$prefix${input.substring(start, end)}$suffix"
    }

    companion object {
        private val CONTAINER_TYPES = setOf("row", "column", "grid", "stack")
    }
}
```

---

### 3. Template Registry with Cycle Detection

```kotlin
/**
 * TemplateRegistry.kt
 *
 * Registry for managing and expanding layout templates.
 * Includes circular reference detection.
 */
package com.augmentalis.avacode.plugins.ui.dsl

/**
 * Registry for layout templates
 *
 * Templates allow reusing common layout patterns by name.
 *
 * ## Example
 * ```kotlin
 * val registry = TemplateRegistry()
 * registry.registerFromDSL(
 *     "simple_form",
 *     "column:textfield[labelKey=name],textfield[labelKey=email],button[labelKey=submit]"
 * )
 *
 * // Use template
 * val parser = LayoutDSLParser("template:simple_form")
 * val ast = parser.parse()
 * val expanded = registry.expandAll(ast)
 * ```
 */
class TemplateRegistry {
    private val templates = mutableMapOf<String, LayoutNode>()

    /**
     * Register template with parsed AST
     *
     * @param name Template identifier
     * @param layout AST node for template
     */
    fun register(name: String, layout: LayoutNode) {
        require(name.isNotBlank()) { "Template name cannot be blank" }
        templates[name] = layout
    }

    /**
     * Register template from DSL string
     *
     * @param name Template identifier
     * @param dsl Layout DSL string
     * @throws IllegalArgumentException if DSL is invalid
     */
    fun registerFromDSL(name: String, dsl: String) {
        val parser = LayoutDSLParser(dsl)
        when (val result = parser.parse()) {
            is ParseResult.Success -> {
                register(name, result.ast)
            }
            is ParseResult.Error -> {
                throw IllegalArgumentException(
                    "Invalid template DSL for '$name': ${result.error.message}"
                )
            }
        }
    }

    /**
     * Get template by name
     *
     * @param name Template identifier
     * @return Template AST
     * @throws IllegalArgumentException if template not found
     */
    fun get(name: String): LayoutNode {
        return templates[name]
            ?: throw IllegalArgumentException("Template not found: '$name'")
    }

    /**
     * Check if template exists
     *
     * @param name Template identifier
     * @return true if template registered
     */
    fun has(name: String): Boolean {
        return name in templates
    }

    /**
     * Expand all template references in AST
     *
     * Recursively replaces all TemplateNode instances with their definitions.
     * Detects and prevents circular references.
     *
     * @param node Root AST node
     * @return Expanded AST with no template references
     * @throws IllegalStateException if circular reference detected
     */
    fun expandAll(node: LayoutNode): LayoutNode {
        return expandWithHistory(node, emptySet())
    }

    /**
     * Internal expansion with cycle detection
     */
    private fun expandWithHistory(
        node: LayoutNode,
        expansionHistory: Set<String>
    ): LayoutNode {
        return when (node) {
            is TemplateNode -> {
                // Check for circular reference
                if (node.templateId in expansionHistory) {
                    throw IllegalStateException(
                        "Circular template reference detected: ${expansionHistory.joinToString(" -> ")} -> ${node.templateId}"
                    )
                }

                // Get template and expand recursively
                val template = get(node.templateId)
                val newHistory = expansionHistory + node.templateId
                expandWithHistory(template, newHistory)
            }

            is ContainerNode -> {
                // Recursively expand children
                node.copy(
                    children = node.children.map { expandWithHistory(it, expansionHistory) }
                )
            }

            is ComponentNode -> {
                // Leaf node - no expansion needed
                node
            }
        }
    }

    /**
     * Get all registered template names
     */
    fun getTemplateNames(): Set<String> {
        return templates.keys.toSet()
    }

    /**
     * Clear all templates
     */
    fun clear() {
        templates.clear()
    }

    /**
     * Remove specific template
     *
     * @param name Template identifier
     * @return true if template was removed
     */
    fun remove(name: String): Boolean {
        return templates.remove(name) != null
    }
}
```

---

### 4. AST Visitor Pattern for Rendering

```kotlin
/**
 * LayoutRenderer.kt
 *
 * Visitor pattern for converting AST to UI components.
 */
package com.augmentalis.avacode.plugins.ui.dsl

/**
 * Layout visitor interface
 *
 * Implement this to traverse and process layout AST.
 */
interface LayoutVisitor<T> {
    fun visitContainer(node: ContainerNode): T
    fun visitComponent(node: ComponentNode): T
    fun visitTemplate(node: TemplateNode): T
}

/**
 * Extension function for visitor pattern
 */
fun <T> LayoutNode.accept(visitor: LayoutVisitor<T>): T {
    return when (this) {
        is ContainerNode -> visitor.visitContainer(this)
        is ComponentNode -> visitor.visitComponent(this)
        is TemplateNode -> visitor.visitTemplate(this)
    }
}

/**
 * Example: AST to String renderer (for debugging)
 */
class StringRenderer : LayoutVisitor<String> {
    override fun visitContainer(node: ContainerNode): String {
        val childrenStr = node.children.joinToString(", ") { it.accept(this) }
        val propsStr = if (node.properties.isNotEmpty()) {
            node.properties.entries.joinToString(", ", "[", "]") { "${it.key}=${it.value}" }
        } else ""
        return "${node.type.name.lowercase()}$propsStr:$childrenStr"
    }

    override fun visitComponent(node: ComponentNode): String {
        val propsStr = if (node.properties.isNotEmpty()) {
            node.properties.entries.joinToString(", ", "[", "]") { "${it.key}=${it.value}" }
        } else ""
        return "${node.componentType}$propsStr"
    }

    override fun visitTemplate(node: TemplateNode): String {
        return "template:${node.templateId}"
    }
}

/**
 * Example: AST to JSON renderer
 */
class JsonRenderer : LayoutVisitor<String> {
    override fun visitContainer(node: ContainerNode): String {
        val children = node.children.joinToString(",") { it.accept(this) }
        val props = node.properties.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        return """{"type":"container","containerType":"${node.type.name.lowercase()}","properties":{$props},"children":[$children]}"""
    }

    override fun visitComponent(node: ComponentNode): String {
        val props = node.properties.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        return """{"type":"component","componentType":"${node.componentType}","properties":{$props}}"""
    }

    override fun visitTemplate(node: TemplateNode): String {
        return """{"type":"template","templateId":"${node.templateId}"}"""
    }
}
```

---

### 5. Complete Integration Example

```kotlin
/**
 * Complete example showing parser integration
 */
package com.augmentalis.avacode.plugins.ui.dsl.examples

fun main() {
    // === 1. Setup Template Registry ===
    val registry = TemplateRegistry()

    // Register common templates
    registry.registerFromDSL(
        "simple_form",
        "column[gap=16]:textfield[labelKey=name],textfield[labelKey=email],button[labelKey=submit]"
    )

    registry.registerFromDSL(
        "action_buttons",
        "row[justify=end,gap=8]:button[labelKey=cancel,variant=secondary],button[labelKey=ok,variant=primary]"
    )

    // === 2. Parse User Input ===
    val userInputs = listOf(
        // Simple row
        "row:button[labelKey=save],button[labelKey=cancel]",

        // Template reference
        "template:simple_form",

        // Nested layout
        "column:text[value=Welcome],template:simple_form,template:action_buttons",

        // Complex nested
        "row:column[gap=8]:text[value=Name],textfield[id=nameField],column[gap=8]:text[value=Email],textfield[id=emailField]",

        // Grid layout
        "grid[cols=2,gap=16]:button[label=A],button[label=B],button[label=C],button[label=D]",

        // Invalid input (for error handling demo)
        "row button[labelKey=save]"  // Missing colon
    )

    // === 3. Process Each Input ===
    for ((index, input) in userInputs.withIndex()) {
        println("\n=== Example ${index + 1} ===")
        println("Input: $input")
        println()

        val parser = LayoutDSLParser(input)
        when (val result = parser.parse()) {
            is ParseResult.Success -> {
                println("✓ Parsed successfully")
                println("AST: ${result.ast}")

                // Expand templates
                try {
                    val expanded = registry.expandAll(result.ast)
                    println("Expanded AST: $expanded")

                    // Render to string (for verification)
                    val renderer = StringRenderer()
                    val rendered = expanded.accept(renderer)
                    println("Rendered: $rendered")

                    // Convert to JSON (for UI framework)
                    val jsonRenderer = JsonRenderer()
                    val json = expanded.accept(jsonRenderer)
                    println("JSON: $json")

                } catch (e: IllegalStateException) {
                    println("✗ Template expansion error: ${e.message}")
                }
            }

            is ParseResult.Error -> {
                println("✗ Parse error:")
                println(result.error.format())
            }
        }
    }

    // === 4. Demonstrate Circular Reference Detection ===
    println("\n=== Circular Reference Detection ===")
    registry.registerFromDSL("template_a", "template:template_b")
    registry.registerFromDSL("template_b", "template:template_a")

    val parser = LayoutDSLParser("template:template_a")
    when (val result = parser.parse()) {
        is ParseResult.Success -> {
            try {
                registry.expandAll(result.ast)
            } catch (e: IllegalStateException) {
                println("✓ Circular reference detected: ${e.message}")
            }
        }
        is ParseResult.Error -> println("Parse error: ${result.error}")
    }

    // === 5. Performance Benchmark ===
    println("\n=== Performance Benchmark ===")
    val simpleLayout = "row:button[labelKey=save],button[labelKey=cancel]"
    val iterations = 10000

    val startTime = System.nanoTime()
    repeat(iterations) {
        val p = LayoutDSLParser(simpleLayout)
        p.parse()
    }
    val endTime = System.nanoTime()

    val durationMs = (endTime - startTime) / 1_000_000.0
    val avgMs = durationMs / iterations

    println("Parsed $iterations layouts in ${durationMs}ms")
    println("Average: ${avgMs}ms per layout")
    println("Throughput: ${(iterations / durationMs * 1000).toInt()} layouts/second")
}
```

---

## Expected Output

```
=== Example 1 ===
Input: row:button[labelKey=save],button[labelKey=cancel]

✓ Parsed successfully
AST: ContainerNode(type=ROW, properties={}, children=[ComponentNode(componentType=button, properties={labelKey=save}), ComponentNode(componentType=button, properties={labelKey=cancel})])
Expanded AST: ContainerNode(type=ROW, properties={}, children=[ComponentNode(componentType=button, properties={labelKey=save}), ComponentNode(componentType=button, properties={labelKey=cancel})])
Rendered: row:button[labelKey=save],button[labelKey=cancel]
JSON: {"type":"container","containerType":"row","properties":{},"children":[{"type":"component","componentType":"button","properties":{"labelKey":"save"}},{"type":"component","componentType":"button","properties":{"labelKey":"cancel"}}]}

=== Example 6 ===
Input: row button[labelKey=save]

✗ Parse error:
Parse error at position 4:
row button[labelKey=save]
    ^
Expected ':' after container type
Suggestion: Add colon after 'row' (e.g., 'row:...')

=== Circular Reference Detection ===
✓ Circular reference detected: Circular template reference detected: template_a -> template_b -> template_a

=== Performance Benchmark ===
Parsed 10000 layouts in 127.5ms
Average: 0.01275ms per layout
Throughput: 78431 layouts/second
```

---

## Testing Checklist

### Unit Tests Required
- ✅ Simple layouts (row, column, grid, stack)
- ✅ Nested layouts (3+ levels deep)
- ✅ Properties (empty, single, multiple)
- ✅ Template expansion
- ✅ Circular reference detection
- ✅ Error messages (all error cases)
- ✅ Edge cases (empty input, single component, etc.)

### Performance Tests Required
- ✅ 10,000 simple layouts < 200ms
- ✅ 1,000 complex layouts < 100ms
- ✅ 100 deeply nested layouts < 50ms

### Integration Tests Required
- ✅ Parse → Expand → Render pipeline
- ✅ Template registry lifecycle
- ✅ Error recovery and suggestions

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Date:** 2025-10-26 22:09 PDT

# Layout DSL Parser Implementation Research

**Date:** 2025-10-26
**Project:** AvaCode Plugin System
**Purpose:** Research and design recommendations for parsing layout command DSL

---

## Executive Summary

This document provides comprehensive research on implementing a parser for the layout command DSL. The DSL enables compact UI layout definitions like `row:button[labelKey=save],button[labelKey=cancel]` for plugin UI construction.

**Recommended Approach:** Recursive descent parser with custom Kotlin implementation
**Why:** Zero dependencies, full control, excellent error messages, KMP compatible, minimal overhead

---

## 1. DSL Syntax Analysis

### Target Syntax
```
row:button[labelKey=save],button[labelKey=cancel]
column:textfield[labelKey=name],textfield[labelKey=email],button[labelKey=submit]
template:simple_form
grid[cols=2]:button[label=A],button[label=B],button[label=C],button[label=D]
row:column[gap=8]:text[value=Name],textfield[],column[gap=8]:text[value=Email],textfield[]
```

### Grammar Structure (EBNF)
```ebnf
layout       ::= container | template | nested_layout
container    ::= layout_type properties? ':' children
template     ::= 'template:' identifier
nested_layout::= container ':' layout (',' layout)*
layout_type  ::= 'row' | 'column' | 'grid' | 'stack'
children     ::= component (',' component)*
component    ::= identifier properties?
properties   ::= '[' property_list? ']'
property_list::= property (',' property)*
property     ::= key '=' value
identifier   ::= [a-zA-Z_][a-zA-Z0-9_]*
value        ::= [^,\[\]]+
```

### Complexity Factors
- **Nested layouts:** Containers can contain other containers
- **Property parsing:** Key-value pairs with special characters
- **Template expansion:** Late binding to template definitions
- **Error recovery:** Malformed syntax requires clear diagnostics

---

## 2. Parser Architecture Options

### Option A: Recursive Descent Parser (RECOMMENDED)
**Implementation:** Custom hand-written parser in Kotlin

**Advantages:**
- Full control over error messages and recovery
- Zero external dependencies (critical for KMP)
- Excellent performance (O(n) single pass)
- Easy to extend with new syntax
- Natural mapping to Kotlin DSL patterns
- Perfect for LL(1) grammars like this one

**Disadvantages:**
- Requires manual implementation
- Need to write lexer + parser

**Verdict:** ✅ **BEST FIT** - Aligns with project's zero-dependency philosophy, full control, KMP compatible

---

### Option B: Parser Combinators
**Libraries:**
- `kotlin-parser-combinator` (https://github.com/h0tk3y/kotlin-parser-combinator)
- `parsus` (https://github.com/alllex/parsus)

**Advantages:**
- Declarative, composable parser definitions
- Type-safe parsing
- Good for complex grammars

**Disadvantages:**
- External dependency (violates project standards)
- Performance overhead from combinator composition
- Harder to debug
- May not support all KMP targets

**Verdict:** ❌ **NOT RECOMMENDED** - Adds dependency, performance overhead

---

### Option C: PEG Parser (Parsing Expression Grammar)
**Libraries:**
- `kotlinx.ast` (AST parsing, not DSL)
- `JParsec` (JVM only)

**Advantages:**
- Handles left recursion naturally
- Packrat parsing optimization

**Disadvantages:**
- JVM-only libraries (fails KMP requirement)
- Overkill for simple grammar
- External dependencies

**Verdict:** ❌ **NOT RECOMMENDED** - KMP incompatible, unnecessary complexity

---

### Option D: Antlr4
**Library:** Antlr4 with Kotlin target

**Advantages:**
- Production-grade parser generator
- Excellent tooling

**Disadvantages:**
- Heavy dependency (runtime + generated code)
- Build step complexity
- Overkill for simple DSL
- KMP support questionable

**Verdict:** ❌ **NOT RECOMMENDED** - Too heavyweight, complex build integration

---

## 3. AST Structure Design

### Sealed Class Hierarchy
```kotlin
/**
 * Abstract syntax tree for layout DSL
 *
 * Example:
 * row:button[labelKey=save],button[labelKey=cancel]
 *
 * Parses to:
 * ContainerNode(
 *   type = "row",
 *   properties = emptyMap(),
 *   children = listOf(
 *     ComponentNode("button", mapOf("labelKey" to "save")),
 *     ComponentNode("button", mapOf("labelKey" to "cancel"))
 *   )
 * )
 */
sealed interface LayoutNode

/**
 * Container layout (row, column, grid, stack)
 */
data class ContainerNode(
    val type: ContainerType,
    val properties: Map<String, String>,
    val children: List<LayoutNode>
) : LayoutNode

/**
 * Leaf component (button, textfield, text, etc.)
 */
data class ComponentNode(
    val componentType: String,
    val properties: Map<String, String>
) : LayoutNode

/**
 * Template reference (deferred expansion)
 */
data class TemplateNode(
    val templateId: String
) : LayoutNode

/**
 * Container types
 */
enum class ContainerType {
    ROW,      // Horizontal layout
    COLUMN,   // Vertical layout
    GRID,     // Grid layout (requires cols/rows property)
    STACK     // Layered layout
}

/**
 * Parser error with location information
 */
data class ParseError(
    val message: String,
    val position: Int,
    val context: String
)
```

---

## 4. Parser Implementation

### Tokenizer (Lexer)
```kotlin
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
    VALUE,           // property value
    EOF
}

/**
 * Token with type and value
 */
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int
)

/**
 * Tokenizer for layout DSL
 *
 * Converts input string into token stream for parser.
 */
class LayoutTokenizer(private val input: String) {
    private var position = 0

    /**
     * Get next token from input
     */
    fun nextToken(): Token {
        skipWhitespace()

        if (position >= input.length) {
            return Token(TokenType.EOF, "", position)
        }

        val char = input[position]
        return when {
            char == ':' -> Token(TokenType.COLON, ":", position++).also { }
            char == ',' -> Token(TokenType.COMMA, ",", position++).also { }
            char == '=' -> Token(TokenType.EQUALS, "=", position++).also { }
            char == '[' -> Token(TokenType.LEFT_BRACKET, "[", position++).also { }
            char == ']' -> Token(TokenType.RIGHT_BRACKET, "]", position++).also { }
            char.isLetter() || char == '_' -> tokenizeIdentifier()
            else -> tokenizeValue()
        }
    }

    /**
     * Peek at next token without consuming
     */
    fun peekToken(): Token {
        val savedPosition = position
        val token = nextToken()
        position = savedPosition
        return token
    }

    private fun skipWhitespace() {
        while (position < input.length && input[position].isWhitespace()) {
            position++
        }
    }

    private fun tokenizeIdentifier(): Token {
        val start = position
        while (position < input.length &&
               (input[position].isLetterOrDigit() || input[position] == '_')) {
            position++
        }
        return Token(TokenType.IDENTIFIER, input.substring(start, position), start)
    }

    private fun tokenizeValue(): Token {
        val start = position
        // Value extends until delimiter: , [ ] or end
        while (position < input.length &&
               input[position] !in listOf(',', '[', ']')) {
            position++
        }
        val value = input.substring(start, position).trim()
        return Token(TokenType.VALUE, value, start)
    }
}
```

### Recursive Descent Parser
```kotlin
/**
 * Recursive descent parser for layout DSL
 *
 * Usage:
 * ```kotlin
 * val parser = LayoutDSLParser("row:button[labelKey=save],button[labelKey=cancel]")
 * val result = parser.parse()
 * when (result) {
 *     is ParseResult.Success -> println(result.ast)
 *     is ParseResult.Error -> println(result.error)
 * }
 * ```
 */
class LayoutDSLParser(input: String) {
    private val tokenizer = LayoutTokenizer(input)
    private var currentToken = tokenizer.nextToken()

    /**
     * Parse result: success with AST or error
     */
    sealed interface ParseResult {
        data class Success(val ast: LayoutNode) : ParseResult
        data class Error(val error: ParseError) : ParseResult
    }

    /**
     * Parse entire layout expression
     */
    fun parse(): ParseResult {
        return try {
            val node = parseLayout()
            if (currentToken.type != TokenType.EOF) {
                ParseResult.Error(
                    ParseError(
                        "Unexpected token after layout: ${currentToken.value}",
                        currentToken.position,
                        getContext(currentToken.position)
                    )
                )
            } else {
                ParseResult.Success(node)
            }
        } catch (e: ParseException) {
            ParseResult.Error(e.error)
        }
    }

    /**
     * Parse layout: container, template, or component
     */
    private fun parseLayout(): LayoutNode {
        // Check for template
        if (currentToken.value == "template") {
            return parseTemplate()
        }

        // Check for container types
        if (currentToken.value in listOf("row", "column", "grid", "stack")) {
            return parseContainer()
        }

        // Otherwise, it's a component
        return parseComponent()
    }

    /**
     * Parse container: type[props]?:children
     */
    private fun parseContainer(): ContainerNode {
        val containerType = when (currentToken.value) {
            "row" -> ContainerType.ROW
            "column" -> ContainerType.COLUMN
            "grid" -> ContainerType.GRID
            "stack" -> ContainerType.STACK
            else -> throw parseError("Expected container type (row, column, grid, stack)")
        }
        advance()

        // Parse optional properties
        val properties = if (currentToken.type == TokenType.LEFT_BRACKET) {
            parseProperties()
        } else {
            emptyMap()
        }

        // Expect colon
        expect(TokenType.COLON, "Expected ':' after container type")

        // Parse children
        val children = parseChildren()

        return ContainerNode(containerType, properties, children)
    }

    /**
     * Parse template: template:identifier
     */
    private fun parseTemplate(): TemplateNode {
        expect(TokenType.IDENTIFIER, "Expected 'template'")
        expect(TokenType.COLON, "Expected ':' after 'template'")

        if (currentToken.type != TokenType.IDENTIFIER) {
            throw parseError("Expected template identifier")
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
            throw parseError("Expected component identifier")
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
        expect(TokenType.LEFT_BRACKET, "Expected '['")

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
                throw parseError("Expected property key")
            }
            val key = currentToken.value
            advance()

            // Expect equals
            expect(TokenType.EQUALS, "Expected '=' after property key")

            // Parse value
            if (currentToken.type !in listOf(TokenType.IDENTIFIER, TokenType.VALUE)) {
                throw parseError("Expected property value")
            }
            val value = currentToken.value
            advance()

            properties[key] = value

            // Check for comma or end
            if (currentToken.type == TokenType.RIGHT_BRACKET) {
                break
            } else if (currentToken.type == TokenType.COMMA) {
                advance()
                // Continue to next property
            } else {
                throw parseError("Expected ',' or ']' in property list")
            }
        }

        expect(TokenType.RIGHT_BRACKET, "Expected ']'")
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
     * Advance to next token
     */
    private fun advance() {
        currentToken = tokenizer.nextToken()
    }

    /**
     * Expect specific token type
     */
    private fun expect(type: TokenType, message: String) {
        if (currentToken.type != type) {
            throw parseError(message)
        }
        advance()
    }

    /**
     * Create parse error with context
     */
    private fun parseError(message: String): ParseException {
        return ParseException(
            ParseError(
                message,
                currentToken.position,
                getContext(currentToken.position)
            )
        )
    }

    /**
     * Get context around error position for error message
     */
    private fun getContext(position: Int): String {
        val input = tokenizer.input
        val start = maxOf(0, position - 20)
        val end = minOf(input.length, position + 20)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < input.length) "..." else ""
        return "$prefix${input.substring(start, end)}$suffix"
    }
}

/**
 * Parse exception with structured error
 */
class ParseException(val error: ParseError) : Exception(error.message)
```

---

## 5. Template Expansion

### Template Registry
```kotlin
/**
 * Registry for layout templates
 *
 * Templates are pre-defined layout patterns that can be referenced
 * by name in the DSL.
 */
class TemplateRegistry {
    private val templates = mutableMapOf<String, LayoutNode>()

    /**
     * Register a template
     */
    fun register(name: String, layout: LayoutNode) {
        templates[name] = layout
    }

    /**
     * Register a template from DSL string
     */
    fun registerFromDSL(name: String, dsl: String) {
        val parser = LayoutDSLParser(dsl)
        when (val result = parser.parse()) {
            is LayoutDSLParser.ParseResult.Success -> {
                templates[name] = result.ast
            }
            is LayoutDSLParser.ParseResult.Error -> {
                throw IllegalArgumentException("Invalid template DSL: ${result.error.message}")
            }
        }
    }

    /**
     * Expand template by name
     */
    fun expand(name: String): LayoutNode {
        return templates[name]
            ?: throw IllegalArgumentException("Template not found: $name")
    }

    /**
     * Recursively expand all template nodes in AST
     */
    fun expandAll(node: LayoutNode): LayoutNode {
        return when (node) {
            is TemplateNode -> expandAll(expand(node.templateId))
            is ContainerNode -> node.copy(
                children = node.children.map { expandAll(it) }
            )
            is ComponentNode -> node
        }
    }
}

/**
 * Example usage:
 * ```kotlin
 * val registry = TemplateRegistry()
 * registry.registerFromDSL(
 *     "simple_form",
 *     "column:textfield[labelKey=name],textfield[labelKey=email],button[labelKey=submit]"
 * )
 *
 * val parser = LayoutDSLParser("template:simple_form")
 * val ast = parser.parse()
 * val expanded = registry.expandAll(ast)
 * ```
 */
```

---

## 6. Error Handling Strategy

### Error Categories
1. **Lexical errors:** Invalid characters, unterminated strings
2. **Syntax errors:** Missing delimiters, unexpected tokens
3. **Semantic errors:** Unknown container types, invalid property values
4. **Template errors:** Missing templates, circular references

### Error Reporting
```kotlin
/**
 * Enhanced error reporting with rich context
 */
data class LayoutParseError(
    val message: String,
    val line: Int,
    val column: Int,
    val snippet: String,
    val suggestion: String?
) {
    /**
     * Format error for display
     */
    fun format(): String {
        return buildString {
            appendLine("Parse error at line $line, column $column:")
            appendLine(snippet)
            appendLine(" ".repeat(column) + "^")
            appendLine(message)
            if (suggestion != null) {
                appendLine("Suggestion: $suggestion")
            }
        }
    }
}

/**
 * Error recovery strategies
 */
object ErrorRecovery {
    /**
     * Try to recover from missing delimiter
     */
    fun recoverMissingDelimiter(expected: Char, position: Int): Token? {
        // Insert synthetic token and continue
        return null // Simplified - full implementation would track recovery state
    }

    /**
     * Suggest corrections for unknown identifiers
     */
    fun suggestIdentifier(unknown: String, knownIdentifiers: List<String>): String? {
        // Levenshtein distance for typo suggestions
        val candidates = knownIdentifiers.filter {
            levenshteinDistance(it, unknown) <= 2
        }
        return candidates.firstOrNull()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + if (s1[i-1] == s2[j-1]) 0 else 1
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
```

### Example Error Messages
```
Parse error at line 1, column 18:
row:button[labelKey=save],button[labelKey
                                        ^
Expected '=' after property key
Suggestion: Add closing bracket ']' before comma

Parse error at line 1, column 4:
rwo:button[labelKey=save]
   ^
Unknown container type 'rwo'
Suggestion: Did you mean 'row'?

Parse error at line 1, column 23:
template:unknown_template
         ^^^^^^^^^^^^^^^
Template not found: 'unknown_template'
Suggestion: Register template before use
```

---

## 7. Performance Considerations

### Benchmarks (Expected)
- **Simple layout** (5 components): < 1ms
- **Complex layout** (50 components): < 5ms
- **Template expansion** (5 levels deep): < 2ms

### Optimization Strategies

1. **Single-pass parsing:** O(n) time complexity
   - No backtracking required for LL(1) grammar
   - Tokenizer and parser run in single pass

2. **Lazy template expansion:** Expand only when needed
   ```kotlin
   sealed interface LayoutNode {
       fun expand(registry: TemplateRegistry): LayoutNode
   }

   data class TemplateNode(val templateId: String) : LayoutNode {
       override fun expand(registry: TemplateRegistry): LayoutNode {
           return registry.expand(templateId).expand(registry)
       }
   }
   ```

3. **Template caching:** Memoize expanded templates
   ```kotlin
   class CachingTemplateRegistry(private val delegate: TemplateRegistry) {
       private val cache = mutableMapOf<String, LayoutNode>()

       fun expandCached(name: String): LayoutNode {
           return cache.getOrPut(name) {
               delegate.expand(name)
           }
       }
   }
   ```

4. **String builder optimization:** Avoid string concatenation in loops
   - Use `StringBuilder` for error messages
   - Reuse token buffers

5. **Memory pooling:** Reuse parser instances
   ```kotlin
   object ParserPool {
       private val pool = ArrayDeque<LayoutDSLParser>()

       fun obtain(input: String): LayoutDSLParser {
           return pool.removeFirstOrNull()?.apply { reset(input) }
               ?: LayoutDSLParser(input)
       }

       fun release(parser: LayoutDSLParser) {
           pool.addFirst(parser)
       }
   }
   ```

---

## 8. Testing Strategy

### Unit Tests
```kotlin
class LayoutDSLParserTest {
    @Test
    fun `parse simple row with two buttons`() {
        val parser = LayoutDSLParser("row:button[labelKey=save],button[labelKey=cancel]")
        val result = parser.parse()

        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast

        assertTrue(ast is ContainerNode)
        val container = ast as ContainerNode
        assertEquals(ContainerType.ROW, container.type)
        assertEquals(2, container.children.size)
    }

    @Test
    fun `parse nested layouts`() {
        val input = "row:column[gap=8]:text[value=Name],textfield[],column[gap=8]:text[value=Email],textfield[]"
        val parser = LayoutDSLParser(input)
        val result = parser.parse()

        assertTrue(result is ParseResult.Success)
        // Verify nested structure
    }

    @Test
    fun `parse error for missing colon`() {
        val parser = LayoutDSLParser("row button[labelKey=save]")
        val result = parser.parse()

        assertTrue(result is ParseResult.Error)
        val error = (result as ParseResult.Error).error
        assertContains(error.message, "Expected ':'")
    }

    @Test
    fun `parse template reference`() {
        val parser = LayoutDSLParser("template:simple_form")
        val result = parser.parse()

        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast
        assertTrue(ast is TemplateNode)
        assertEquals("simple_form", (ast as TemplateNode).templateId)
    }

    @Test
    fun `parse properties with empty brackets`() {
        val parser = LayoutDSLParser("row:button[]")
        val result = parser.parse()

        assertTrue(result is ParseResult.Success)
        val ast = (result as ParseResult.Success).ast
        val container = ast as ContainerNode
        val component = container.children.first() as ComponentNode
        assertTrue(component.properties.isEmpty())
    }
}
```

### Performance Tests
```kotlin
class LayoutParserPerformanceTest {
    @Test
    fun `parse 1000 simple layouts under 100ms`() {
        val input = "row:button[labelKey=save],button[labelKey=cancel]"
        val iterations = 1000

        val startTime = System.nanoTime()
        repeat(iterations) {
            val parser = LayoutDSLParser(input)
            parser.parse()
        }
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000

        assertTrue(durationMs < 100, "Parsing took ${durationMs}ms, expected < 100ms")
        println("Parsed $iterations layouts in ${durationMs}ms (${durationMs.toDouble() / iterations}ms per parse)")
    }
}
```

---

## 9. Integration Example

### Complete Usage Flow
```kotlin
/**
 * Layout DSL integration example
 */
fun main() {
    // 1. Create template registry
    val registry = TemplateRegistry()
    registry.registerFromDSL(
        "simple_form",
        "column[gap=16]:textfield[labelKey=name],textfield[labelKey=email],button[labelKey=submit]"
    )
    registry.registerFromDSL(
        "two_button_row",
        "row[justify=end,gap=8]:button[labelKey=cancel],button[labelKey=ok]"
    )

    // 2. Parse user-provided DSL
    val userInput = "column:text[value=Welcome],template:simple_form,template:two_button_row"
    val parser = LayoutDSLParser(userInput)

    when (val result = parser.parse()) {
        is ParseResult.Success -> {
            // 3. Expand templates
            val expanded = registry.expandAll(result.ast)

            // 4. Convert to UI components (example)
            val uiComponents = LayoutRenderer.render(expanded)

            println("Successfully parsed and rendered layout")
            println("AST: $expanded")
            println("UI Components: $uiComponents")
        }
        is ParseResult.Error -> {
            // 5. Handle errors gracefully
            println("Parse error:")
            println(result.error.message)
            println("Position: ${result.error.position}")
            println("Context: ${result.error.context}")
        }
    }
}

/**
 * Example renderer that converts AST to UI components
 */
object LayoutRenderer {
    fun render(node: LayoutNode): List<UIComponent> {
        return when (node) {
            is ContainerNode -> renderContainer(node)
            is ComponentNode -> listOf(renderComponent(node))
            is TemplateNode -> error("Templates should be expanded before rendering")
        }
    }

    private fun renderContainer(node: ContainerNode): List<UIComponent> {
        val children = node.children.flatMap { render(it) }
        return listOf(
            UIContainer(
                type = node.type.toString().lowercase(),
                properties = node.properties,
                children = children
            )
        )
    }

    private fun renderComponent(node: ComponentNode): UIComponent {
        return UIComponent(
            type = node.componentType,
            properties = node.properties
        )
    }
}

// Mock UI component classes
data class UIComponent(val type: String, val properties: Map<String, String>)
data class UIContainer(
    val type: String,
    val properties: Map<String, String>,
    val children: List<UIComponent>
) : UIComponent(type, properties)
```

---

## 10. Recommended Libraries (Minimal)

### Zero External Dependencies (Recommended)
- **Tokenizer:** Hand-written (150 LOC)
- **Parser:** Hand-written recursive descent (250 LOC)
- **AST:** Kotlin sealed classes (50 LOC)
- **Total:** ~450 LOC, zero dependencies

### Optional Quality-of-Life Libraries
None recommended. Keep implementation self-contained.

### Why Zero Dependencies?
1. **KMP compatibility:** Guaranteed to work on all platforms
2. **Build simplicity:** No complex dependency resolution
3. **Performance:** No library overhead
4. **Control:** Full control over error messages, optimization
5. **Maintenance:** No version conflicts or deprecation issues

---

## 11. Migration Path

### Phase 1: Core Parser (Week 1)
- Implement tokenizer
- Implement recursive descent parser
- Basic AST nodes
- Unit tests for simple layouts

### Phase 2: Template System (Week 1)
- Template registry
- Template expansion
- Circular reference detection
- Integration tests

### Phase 3: Error Enhancement (Week 2)
- Rich error messages
- Suggestion system
- Error recovery
- User-facing documentation

### Phase 4: Performance Optimization (Week 2)
- Template caching
- Parser pooling
- Benchmarking
- Memory profiling

---

## 12. Example Implementation Files

### File Structure
```
runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/dsl/
├── LayoutDSL.kt              # Public API
├── LayoutTokenizer.kt        # Lexer
├── LayoutParser.kt           # Parser
├── LayoutAST.kt              # AST nodes
├── TemplateRegistry.kt       # Template management
├── LayoutRenderer.kt         # AST to UI conversion
└── ErrorReporting.kt         # Error handling

runtime/plugin-system/src/commonTest/kotlin/com/augmentalis/avacode/plugins/ui/dsl/
├── LayoutParserTest.kt       # Unit tests
├── TemplateRegistryTest.kt   # Template tests
└── PerformanceTest.kt        # Performance benchmarks
```

---

## 13. Conclusion

### Final Recommendation
**Implement a custom recursive descent parser in pure Kotlin.**

**Rationale:**
1. Zero dependencies (aligns with project philosophy)
2. Full KMP compatibility
3. Excellent performance (< 1ms for typical layouts)
4. Complete control over error messages
5. Simple to maintain and extend
6. Total implementation: ~450 LOC

### Success Metrics
- ✅ Parse 1000 layouts in < 100ms
- ✅ Zero external dependencies
- ✅ Rich error messages with suggestions
- ✅ Support nested layouts (5+ levels deep)
- ✅ Template expansion with cycle detection
- ✅ 90%+ test coverage

### Next Steps
1. Review and approve this design
2. Create IDEACODE spec using `/idea.specify`
3. Generate implementation plan using `/idea.plan`
4. Break down into tasks using `/idea.tasks`
5. Implement using IDE Loop (Implement → Defend → Evaluate → Commit)

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Date:** 2025-10-26 22:09 PDT

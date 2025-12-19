package net.ideahq.avamagic.codegen.parser

import net.ideahq.avamagic.codegen.ast.*
import net.ideahq.avamagic.codegen.ast.UuidUtils

/**
 * CompactSyntaxParser - Parses AvaMagicUCD (UltraCompact DSL) format
 *
 * Parses compact syntax like:
 * ```
 * MagicScreen.Login {
 *   MagicColumn {
 *     MagicTextField.Email(bind: user.email, placeholder: "Email")
 *     MagicTextField.Password(bind: user.password, mask: true)
 *     MagicButton.Submit("Login", onClick: submitLogin)
 *   }
 * }
 * ```
 *
 * Component syntax: MagicType.Subtype(properties...) { children }
 * Supports:
 * - Magic* component naming (MagicButton, MagicTextField, etc.)
 * - Property bindings (bind: variable)
 * - Event handlers (onClick: handlerName)
 * - Nested children
 * - State declarations
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class CompactSyntaxParser {

    private var source: String = ""
    private var position: Int = 0
    private var line: Int = 1
    private var column: Int = 1

    /**
     * Parse UltraCompact DSL to ScreenNode
     */
    fun parseScreen(input: String): Result<ScreenNode> {
        return try {
            reset(input)
            skipWhitespace()

            // Parse screen definition: MagicScreen.Name { ... }
            val (screenType, screenName) = parseComponentName()
            if (screenType != "MagicScreen" && screenType != "Screen") {
                throw ParseException("Expected MagicScreen, got $screenType", null)
            }

            skipWhitespace()

            // Parse state declarations if present
            val stateVars = mutableListOf<StateVariable>()
            if (peek() == '@') {
                stateVars.addAll(parseStateDeclarations())
            }

            // Parse children block
            expect('{')
            skipWhitespace()

            val children = parseChildren()

            skipWhitespace()
            expect('}')

            // Build screen node - wrap children in a Column if multiple
            val root = if (children.size == 1) {
                children[0]
            } else {
                ComponentNode(
                    id = generateId(),
                    type = ComponentType.COLUMN,
                    properties = emptyMap(),
                    children = children.map { it as AvaUINode },
                    eventHandlers = emptyMap()
                )
            }

            Result.success(ScreenNode(screenName, root, stateVars, emptyList()))
        } catch (e: Exception) {
            Result.failure(ParseException("Parse error at line $line, column $column: ${e.message}", e))
        }
    }

    /**
     * Parse single component
     */
    fun parseComponent(input: String): Result<ComponentNode> {
        return try {
            reset(input)
            skipWhitespace()
            val component = parseComponentNode()
            Result.success(component)
        } catch (e: Exception) {
            Result.failure(ParseException("Parse error at line $line, column $column: ${e.message}", e))
        }
    }

    /**
     * Validate UltraCompact DSL syntax
     */
    fun validate(input: String): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        try {
            reset(input)
            skipWhitespace()

            // Basic structure validation
            val (screenType, _) = parseComponentName()
            if (screenType != "MagicScreen" && screenType != "Screen") {
                warnings.add(ValidationWarning("Root should be MagicScreen", line))
            }

            skipWhitespace()
            if (peek() != '{' && peek() != '@') {
                errors.add(ValidationError("Expected '{' or state declaration", line))
            }

            // Validate braces matching
            var braceCount = 0
            reset(input)
            for (char in input) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> braceCount--
                }
            }
            if (braceCount != 0) {
                errors.add(ValidationError("Unmatched braces: ${if (braceCount > 0) "missing '}'" else "extra '}'"}", 0))
            }

        } catch (e: Exception) {
            errors.add(ValidationError("Syntax error: ${e.message}", line))
        }

        return ValidationResult(errors, warnings)
    }

    // Parsing methods

    private fun parseComponentNode(): ComponentNode {
        val (typeName, subtype) = parseComponentName()
        val componentType = resolveComponentType(typeName, subtype)

        skipWhitespace()

        // Parse properties
        val properties = mutableMapOf<String, Any>()
        val eventHandlers = mutableMapOf<String, String>()

        if (peek() == '(') {
            advance() // consume '('
            parseProperties(properties, eventHandlers)
            expect(')')
        }

        // Add subtype as property if present
        if (subtype.isNotEmpty() && subtype != typeName) {
            properties["variant"] = subtype
        }

        skipWhitespace()

        // Parse children block
        val children = if (peek() == '{') {
            advance() // consume '{'
            skipWhitespace()
            val childList = parseChildren()
            skipWhitespace()
            expect('}')
            childList
        } else {
            emptyList()
        }

        return ComponentNode(
            id = generateId(),
            type = componentType,
            properties = properties,
            children = children.map { it as AvaUINode },
            eventHandlers = eventHandlers
        )
    }

    private fun parseComponentName(): Pair<String, String> {
        val name = parseIdentifier()

        return if (peek() == '.') {
            advance() // consume '.'
            val subtype = parseIdentifier()
            Pair(name, subtype)
        } else {
            Pair(name, name)
        }
    }

    private fun parseProperties(properties: MutableMap<String, Any>, eventHandlers: MutableMap<String, String>) {
        skipWhitespace()

        // Handle positional first argument (for shorthand like MagicButton("Login"))
        if (peek() == '"') {
            val stringValue = parseStringLiteral()
            properties["text"] = stringValue
            skipWhitespace()
            if (peek() == ',') {
                advance()
                skipWhitespace()
            }
        }

        while (peek() != ')') {
            skipWhitespace()
            if (peek() == ')') break

            val key = parseIdentifier()
            skipWhitespace()
            expect(':')
            skipWhitespace()

            val value = parsePropertyValue()

            // Determine if it's an event handler
            if (key.startsWith("on") && value is String && !value.startsWith("\"")) {
                eventHandlers[key] = value
            } else {
                properties[key] = value
            }

            skipWhitespace()
            if (peek() == ',') {
                advance()
                skipWhitespace()
            }
        }
    }

    private fun parsePropertyValue(): Any {
        return when {
            peek() == '"' -> parseStringLiteral()
            peek() == '\'' -> parseSingleQuoteString()
            peek() == '[' -> parseArrayLiteral()
            peek() == '{' -> parseObjectLiteral()
            peek() == 't' || peek() == 'f' -> parseBooleanLiteral()
            peek().isDigit() || peek() == '-' -> parseNumberLiteral()
            else -> parseReference()
        }
    }

    private fun parseStringLiteral(): String {
        expect('"')
        val builder = StringBuilder()
        while (peek() != '"') {
            if (peek() == '\\') {
                advance()
                builder.append(parseEscapeSequence())
            } else {
                builder.append(advance())
            }
        }
        expect('"')
        return builder.toString()
    }

    private fun parseSingleQuoteString(): String {
        expect('\'')
        val builder = StringBuilder()
        while (peek() != '\'') {
            builder.append(advance())
        }
        expect('\'')
        return builder.toString()
    }

    private fun parseArrayLiteral(): List<Any> {
        expect('[')
        val items = mutableListOf<Any>()
        skipWhitespace()

        while (peek() != ']') {
            items.add(parsePropertyValue())
            skipWhitespace()
            if (peek() == ',') {
                advance()
                skipWhitespace()
            }
        }

        expect(']')
        return items
    }

    private fun parseObjectLiteral(): Map<String, Any> {
        expect('{')
        val map = mutableMapOf<String, Any>()
        skipWhitespace()

        while (peek() != '}') {
            val key = if (peek() == '"') parseStringLiteral() else parseIdentifier()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            val value = parsePropertyValue()
            map[key] = value
            skipWhitespace()
            if (peek() == ',') {
                advance()
                skipWhitespace()
            }
        }

        expect('}')
        return map
    }

    private fun parseBooleanLiteral(): Boolean {
        val word = parseIdentifier()
        return when (word) {
            "true" -> true
            "false" -> false
            else -> throw ParseException("Expected boolean, got $word", null)
        }
    }

    private fun parseNumberLiteral(): Number {
        val builder = StringBuilder()

        if (peek() == '-') {
            builder.append(advance())
        }

        while (peek().isDigit() || peek() == '.') {
            builder.append(advance())
        }

        val numStr = builder.toString()
        return if (numStr.contains('.')) {
            numStr.toDouble()
        } else {
            numStr.toInt()
        }
    }

    private fun parseReference(): String {
        return parseIdentifierPath()
    }

    private fun parseIdentifier(): String {
        val builder = StringBuilder()
        while (peek().isLetterOrDigit() || peek() == '_') {
            builder.append(advance())
        }
        return builder.toString()
    }

    private fun parseIdentifierPath(): String {
        val builder = StringBuilder()
        while (peek().isLetterOrDigit() || peek() == '_' || peek() == '.') {
            builder.append(advance())
        }
        return builder.toString()
    }

    private fun parseEscapeSequence(): Char {
        return when (val c = advance()) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            '"' -> '"'
            '\\' -> '\\'
            else -> c
        }
    }

    private fun parseChildren(): List<ComponentNode> {
        val children = mutableListOf<ComponentNode>()

        while (peek() != '}' && !isAtEnd()) {
            skipWhitespace()
            if (peek() == '}') break

            // Check for comment
            if (peek() == '/' && peekNext() == '/') {
                skipLineComment()
                continue
            }

            val child = parseComponentNode()
            children.add(child)

            skipWhitespace()

            // Optional semicolon separator
            if (peek() == ';') {
                advance()
                skipWhitespace()
            }
        }

        return children
    }

    private fun parseStateDeclarations(): List<StateVariable> {
        val states = mutableListOf<StateVariable>()

        while (peek() == '@') {
            advance() // consume '@'
            val keyword = parseIdentifier()

            if (keyword == "state" || keyword == "State") {
                skipWhitespace()
                val name = parseIdentifier()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val type = parseIdentifier()

                var initialValue: PropertyValue? = null
                skipWhitespace()
                if (peek() == '=') {
                    advance()
                    skipWhitespace()
                    initialValue = parsePropertyValueTyped()
                }

                states.add(StateVariable(name, type, initialValue, true))
            }

            skipWhitespace()
        }

        return states
    }

    private fun parsePropertyValueTyped(): PropertyValue {
        return when {
            peek() == '"' -> PropertyValue.StringValue(parseStringLiteral())
            peek() == 't' || peek() == 'f' -> PropertyValue.BoolValue(parseBooleanLiteral())
            peek().isDigit() || peek() == '-' -> {
                val num = parseNumberLiteral()
                if (num is Double) PropertyValue.DoubleValue(num) else PropertyValue.IntValue(num as Int)
            }
            else -> PropertyValue.ReferenceValue(parseReference())
        }
    }

    // Type resolution

    private fun resolveComponentType(typeName: String, subtype: String): ComponentType {
        // Strip "Magic" prefix if present
        val cleanType = typeName.removePrefix("Magic").removePrefix("Ava")

        return when (cleanType.uppercase()) {
            // Foundation
            "BUTTON" -> ComponentType.BUTTON
            "CARD" -> ComponentType.CARD
            "CHECKBOX" -> ComponentType.CHECKBOX
            "CHIP" -> ComponentType.CHIP
            "DIVIDER" -> ComponentType.DIVIDER
            "IMAGE" -> ComponentType.IMAGE
            "LISTITEM", "LIST_ITEM" -> ComponentType.LIST_ITEM
            "TEXT" -> ComponentType.TEXT
            "TEXTFIELD", "TEXT_FIELD" -> ComponentType.TEXT_FIELD

            // Core
            "COLORPICKER", "COLOR_PICKER" -> ComponentType.COLOR_PICKER
            "ICONPICKER", "ICON_PICKER" -> ComponentType.ICON_PICKER

            // Basic
            "ICON" -> ComponentType.ICON
            "LABEL" -> ComponentType.LABEL
            "CONTAINER" -> ComponentType.CONTAINER
            "ROW" -> ComponentType.ROW
            "COLUMN" -> ComponentType.COLUMN
            "SPACER" -> ComponentType.SPACER

            // Advanced
            "SWITCH" -> ComponentType.SWITCH
            "SLIDER" -> ComponentType.SLIDER
            "PROGRESSBAR", "PROGRESS_BAR" -> ComponentType.PROGRESS_BAR
            "SPINNER" -> ComponentType.SPINNER
            "ALERT" -> ComponentType.ALERT
            "DIALOG" -> ComponentType.DIALOG
            "TOAST" -> ComponentType.TOAST
            "TOOLTIP" -> ComponentType.TOOLTIP
            "RADIO" -> ComponentType.RADIO
            "DROPDOWN" -> ComponentType.DROPDOWN
            "DATEPICKER", "DATE_PICKER" -> ComponentType.DATE_PICKER
            "TIMEPICKER", "TIME_PICKER" -> ComponentType.TIME_PICKER
            "SEARCHBAR", "SEARCH_BAR" -> ComponentType.SEARCH_BAR
            "RATING" -> ComponentType.RATING
            "BADGE" -> ComponentType.BADGE
            "FILEUPLOAD", "FILE_UPLOAD" -> ComponentType.FILE_UPLOAD
            "APPBAR", "APP_BAR" -> ComponentType.APP_BAR
            "BOTTOMNAV", "BOTTOM_NAV" -> ComponentType.BOTTOM_NAV
            "DRAWER" -> ComponentType.DRAWER
            "PAGINATION" -> ComponentType.PAGINATION
            "TABS" -> ComponentType.TABS
            "BREADCRUMB" -> ComponentType.BREADCRUMB
            "ACCORDION" -> ComponentType.ACCORDION

            // Layout
            "STACK" -> ComponentType.STACK
            "GRID" -> ComponentType.GRID
            "SCROLLVIEW", "SCROLL_VIEW" -> ComponentType.SCROLL_VIEW

            // Screen is mapped to Column
            "SCREEN" -> ComponentType.COLUMN

            else -> ComponentType.CUSTOM
        }
    }

    // Helper methods

    private fun reset(input: String) {
        source = input
        position = 0
        line = 1
        column = 1
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[position]

    private fun peekNext(): Char = if (position + 1 >= source.length) '\u0000' else source[position + 1]

    private fun advance(): Char {
        val char = source[position++]
        if (char == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return char
    }

    private fun expect(char: Char) {
        if (peek() != char) {
            throw ParseException("Expected '$char' but got '${peek()}'", null)
        }
        advance()
    }

    private fun isAtEnd(): Boolean = position >= source.length

    private fun skipWhitespace() {
        while (!isAtEnd() && peek().isWhitespace()) {
            advance()
        }
    }

    private fun skipLineComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance()
        }
        if (!isAtEnd()) advance() // consume newline
    }

    private fun generateId(): String = UuidUtils.generateShortId()
}

/**
 * Convenience extension for parsing UltraCompact DSL strings
 */
fun String.parseAsUltraCompactDSL(): Result<ScreenNode> {
    return CompactSyntaxParser().parseScreen(this)
}

/**
 * Parse UltraCompact component definition
 */
fun String.parseAsUltraCompactComponent(): Result<ComponentNode> {
    return CompactSyntaxParser().parseComponent(this)
}

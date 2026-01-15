package com.augmentalis.avamagic.dsl

/**
 * Recursive descent parser for the VoiceOS DSL.
 *
 * Transforms a token stream into an Abstract Syntax Tree (AST) representing
 * the structure of a AvaUI application.
 *
 * Grammar (simplified):
 * ```
 * app        → "App" "{" appBody "}"
 * appBody    → property* component* voiceCommands?
 * component  → IDENTIFIER "{" componentBody "}"
 * componentBody → property* callback* component*
 * property   → IDENTIFIER ":" value
 * callback   → IDENTIFIER ":" lambda
 * lambda     → "(" params ")" "=>" "{" statements "}"
 * value      → STRING | NUMBER | BOOLEAN | IDENTIFIER | lambda
 * ```
 *
 * Example Usage:
 * ```kotlin
 * val tokens = tokenizer.tokenize(dslSource)
 * val parser = VosParser(tokens)
 * val ast = parser.parse()
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27
 */
class VosParser(private val tokens: List<Token>) {
    /**
     * Current position in the token stream.
     */
    private var current = 0

    /**
     * Parses the token stream into an AST.
     *
     * @return The root App node of the AST
     * @throws ParserException if the input is malformed
     */
    fun parse(): VosAstNode.App {
        return parseApp()
    }

    /**
     * Parses the root App node.
     *
     * Expects: App { id: "...", name: "...", ... }
     */
    private fun parseApp(): VosAstNode.App {
        expect(TokenType.IDENTIFIER, "App")
        expect(TokenType.LBRACE)

        val properties = mutableMapOf<String, VosValue>()
        val components = mutableListOf<VosAstNode.Component>()
        val voiceCommands = mutableMapOf<String, String>()

        var id: String? = null
        var name: String? = null
        var runtime = "AvaUI"

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // Skip newlines
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            when {
                check(TokenType.IDENTIFIER) -> {
                    val ident = peek().value
                    when (ident) {
                        "id", "name", "runtime" -> {
                            val prop = parseProperty()
                            when (prop.first) {
                                "id" -> id = extractStringValue(
                                    prop.second,
                                    "id must be a string"
                                )
                                "name" -> name = extractStringValue(
                                    prop.second,
                                    "name must be a string"
                                )
                                "runtime" -> runtime = extractStringValue(
                                    prop.second,
                                    "runtime must be a string"
                                )
                                else -> properties[prop.first] = prop.second
                            }
                        }
                        "VoiceCommands" -> {
                            voiceCommands.putAll(parseVoiceCommands())
                        }
                        else -> {
                            // It's a component - check if next token is LBRACE
                            if (peekNext().type == TokenType.LBRACE) {
                                components.add(parseComponent())
                            } else {
                                // It's a property
                                val prop = parseProperty()
                                properties[prop.first] = prop.second
                            }
                        }
                    }
                }
                else -> throw ParserException(
                    "Unexpected token ${peek()} at ${peek().line}:${peek().column}"
                )
            }
        }

        expect(TokenType.RBRACE)

        if (id == null) {
            throw ParserException("App must have 'id' property")
        }
        if (name == null) {
            throw ParserException("App must have 'name' property")
        }

        return VosAstNode.App(
            id = id,
            name = name,
            runtime = runtime,
            components = components,
            voiceCommands = voiceCommands,
            properties = properties
        )
    }

    /**
     * Parses a UI component.
     *
     * Expects: ComponentType { id: "...", property: value, ... }
     */
    private fun parseComponent(): VosAstNode.Component {
        val type = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LBRACE)

        val properties = mutableMapOf<String, VosValue>()
        val children = mutableListOf<VosAstNode.Component>()
        val callbacks = mutableMapOf<String, VosLambda>()
        var id: String? = null

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // Skip newlines
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            if (!check(TokenType.IDENTIFIER)) {
                throw ParserException(
                    "Expected identifier at ${peek().line}:${peek().column}, got ${peek().type}"
                )
            }

            val identToken = peek()
            val ident = identToken.value
            val nextToken = peekNext()

            when (nextToken.type) {
                TokenType.COLON -> {
                    // Property or callback
                    val prop = parseProperty()

                    if (prop.first == "id") {
                        id = extractStringValue(prop.second, "Component id must be a string")
                    } else if (isCallback(prop.first)) {
                        // It's a callback - value should be a lambda
                        callbacks[prop.first] = parseLambdaFromValue(prop.second)
                    } else {
                        properties[prop.first] = prop.second
                    }
                }
                TokenType.LBRACE -> {
                    // Nested component
                    children.add(parseComponent())
                }
                else -> throw ParserException(
                    "Unexpected token ${nextToken.type} after identifier '${ident}' " +
                            "at ${nextToken.line}:${nextToken.column}"
                )
            }
        }

        expect(TokenType.RBRACE)

        return VosAstNode.Component(
            type = type,
            id = id,
            properties = properties,
            children = children,
            callbacks = callbacks
        )
    }

    /**
     * Parses a property assignment.
     *
     * Expects: propertyName: value
     *
     * @return Pair of property name and value
     */
    private fun parseProperty(): Pair<String, VosValue> {
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.COLON)
        val value = parseValue()
        return name to value
    }

    /**
     * Parses a value (string, number, boolean, identifier, or lambda).
     *
     * @return The parsed value
     */
    private fun parseValue(): VosValue {
        return when {
            check(TokenType.STRING) -> {
                VosValue.StringValue(advance().value)
            }
            check(TokenType.NUMBER) -> {
                val num = advance().value
                if (num.contains('.')) {
                    VosValue.FloatValue(num.toFloat())
                } else {
                    VosValue.IntValue(num.toInt())
                }
            }
            check(TokenType.TRUE) -> {
                advance()
                VosValue.BoolValue(true)
            }
            check(TokenType.FALSE) -> {
                advance()
                VosValue.BoolValue(false)
            }
            check(TokenType.IDENTIFIER) -> {
                val ident = advance().value
                when (ident) {
                    "true" -> VosValue.BoolValue(true)
                    "false" -> VosValue.BoolValue(false)
                    "null" -> VosValue.NullValue
                    else -> VosValue.StringValue(ident)  // Treat as string/reference
                }
            }
            check(TokenType.LPAREN) -> {
                // It's a lambda
                parseLambdaAsValue()
            }
            check(TokenType.LBRACKET) -> {
                // It's a list
                parseListValue()
            }
            else -> throw ParserException(
                "Expected value at ${peek().line}:${peek().column}, got ${peek().type}"
            )
        }
    }

    /**
     * Parses a list value.
     *
     * Expects: [value1, value2, ...]
     */
    private fun parseListValue(): VosValue {
        expect(TokenType.LBRACKET)
        val items = mutableListOf<VosValue>()

        while (!check(TokenType.RBRACKET) && !isAtEnd()) {
            // Skip newlines
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            items.add(parseValue())

            if (!check(TokenType.RBRACKET)) {
                expect(TokenType.COMMA)
            }
        }

        expect(TokenType.RBRACKET)
        return ListValue(items)
    }

    /**
     * Parses a lambda expression and wraps it in a VosValue.
     *
     * Expects: (param1, param2) => { statements }
     */
    private fun parseLambdaAsValue(): VosValue {
        val lambda = parseLambda()
        // Wrap lambda in ObjectValue for type compatibility
        return VosValue.ObjectValue(
            mapOf(
                "_type" to VosValue.StringValue("lambda"),
                "_lambda" to VosValue.StringValue(lambda.toString())
            )
        )
    }

    /**
     * Parses a lambda expression.
     *
     * Expects: (param1, param2) => { statements }
     */
    private fun parseLambda(): VosLambda {
        expect(TokenType.LPAREN)
        val params = mutableListOf<String>()

        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            params.add(expect(TokenType.IDENTIFIER).value)
            if (!check(TokenType.RPAREN)) {
                expect(TokenType.COMMA)
            }
        }

        expect(TokenType.RPAREN)
        expect(TokenType.ARROW)
        expect(TokenType.LBRACE)

        val statements = mutableListOf<VosStatement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // Skip newlines
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            statements.add(parseStatement())
        }

        expect(TokenType.RBRACE)

        return VosLambda(params, statements)
    }

    /**
     * Converts a VosValue containing a lambda back to VosLambda.
     *
     * Used when a callback property is parsed as a value first.
     */
    private fun parseLambdaFromValue(value: VosValue): VosLambda {
        return when (value) {
            is VosValue.ObjectValue -> {
                if (value.properties["_type"] is VosValue.StringValue &&
                    (value.properties["_type"] as VosValue.StringValue).value == "lambda"
                ) {
                    // For now, return empty lambda - full implementation would deserialize
                    VosLambda(emptyList(), emptyList())
                } else {
                    throw ParserException("Expected lambda value, got object")
                }
            }
            else -> throw ParserException("Expected lambda value, got ${value::class.simpleName}")
        }
    }

    /**
     * Parses a statement in a lambda body.
     *
     * Supports:
     * - Function calls: target.method(args)
     * - Assignments: variable = value
     * - Return statements: return value
     */
    private fun parseStatement(): VosStatement {
        if (check(TokenType.IDENTIFIER)) {
            val firstToken = peek().value

            // Check for return statement
            if (firstToken == "return") {
                advance()
                val returnValue = if (!check(TokenType.NEWLINE) && !check(TokenType.RBRACE)) {
                    parseValue()
                } else {
                    null
                }
                return VosStatement.Return(returnValue)
            }

            // Otherwise, parse as identifier
            val target = advance().value

            return when {
                check(TokenType.DOT) -> {
                    // Function call: target.method(args)
                    advance()
                    val method = expect(TokenType.IDENTIFIER).value
                    expect(TokenType.LPAREN)

                    val args = mutableListOf<VosValue>()
                    while (!check(TokenType.RPAREN) && !isAtEnd()) {
                        // Skip newlines
                        if (check(TokenType.NEWLINE)) {
                            advance()
                            continue
                        }

                        args.add(parseValue())

                        if (!check(TokenType.RPAREN)) {
                            expect(TokenType.COMMA)
                        }
                    }

                    expect(TokenType.RPAREN)
                    VosStatement.FunctionCall("$target.$method", args)
                }
                check(TokenType.EQUALS) -> {
                    // Assignment: target = value
                    advance()
                    val value = parseValue()
                    VosStatement.Assignment(target, value)
                }
                else -> {
                    // Standalone identifier - treat as zero-arg function call
                    VosStatement.FunctionCall(target, emptyList())
                }
            }
        } else {
            throw ParserException(
                "Expected statement at ${peek().line}:${peek().column}, got ${peek().type}"
            )
        }
    }

    /**
     * Parses voice commands block.
     *
     * Expects: VoiceCommands { "command" => action, ... }
     */
    private fun parseVoiceCommands(): Map<String, String> {
        expect(TokenType.IDENTIFIER, "VoiceCommands")
        expect(TokenType.LBRACE)
        val commands = mutableMapOf<String, String>()

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            // Skip newlines
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            val command = expect(TokenType.STRING).value
            expect(TokenType.ARROW)
            val action = expect(TokenType.IDENTIFIER).value
            commands[command] = action

            // Optional comma
            if (check(TokenType.COMMA)) {
                advance()
            }
        }

        expect(TokenType.RBRACE)
        return commands
    }

    /**
     * Determines if a property name represents a callback.
     *
     * Callbacks typically start with "on" or end with "Changed"/"Clicked".
     */
    private fun isCallback(name: String): Boolean {
        return name.startsWith("on") ||
                name.endsWith("Changed") ||
                name.endsWith("Clicked") ||
                name.endsWith("Listener") ||
                name.endsWith("Handler")
    }

    /**
     * Extracts a string value from a VosValue.
     *
     * @throws ParserException if value is not a string
     */
    private fun extractStringValue(value: VosValue, errorMsg: String): String {
        return when (value) {
            is VosValue.StringValue -> value.value
            else -> throw ParserException(errorMsg)
        }
    }

    // Token navigation helpers

    /**
     * Returns the current token without advancing.
     */
    private fun peek(): Token {
        return if (current < tokens.size) tokens[current] else tokens.last()
    }

    /**
     * Returns the next token without advancing.
     */
    private fun peekNext(): Token {
        return if (current + 1 < tokens.size) {
            tokens[current + 1]
        } else {
            tokens.last()
        }
    }

    /**
     * Consumes and returns the current token, advancing position.
     */
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return tokens[current - 1]
    }

    /**
     * Checks if current token matches the given type.
     */
    private fun check(type: TokenType): Boolean {
        return !isAtEnd() && peek().type == type
    }

    /**
     * Checks if we've reached the end of the token stream.
     */
    private fun isAtEnd(): Boolean {
        return current >= tokens.size || peek().type == TokenType.EOF
    }

    /**
     * Expects a specific token type and optionally a specific value.
     *
     * @throws ParserException if expectation is not met
     */
    private fun expect(type: TokenType, value: String? = null): Token {
        val token = peek()

        if (token.type != type) {
            throw ParserException(
                "Expected ${type} but got ${token.type} at ${token.line}:${token.column}"
            )
        }

        if (value != null && token.value != value) {
            throw ParserException(
                "Expected '${value}' but got '${token.value}' at ${token.line}:${token.column}"
            )
        }

        return advance()
    }
}

/**
 * Exception thrown when parsing fails.
 *
 * @property message Detailed error message including location information
 */
class ParserException(message: String) : Exception(message)

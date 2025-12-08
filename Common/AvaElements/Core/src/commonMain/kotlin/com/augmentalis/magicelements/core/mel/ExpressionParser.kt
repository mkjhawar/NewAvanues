package com.augmentalis.magicelements.core.mel

/**
 * Recursive descent parser for MagicUI Expression Language (MEL).
 *
 * Parses a stream of tokens into an Abstract Syntax Tree (AST).
 *
 * Grammar (simplified):
 * ```
 * expression     → logicalOr
 * logicalOr      → logicalAnd ( "||" logicalAnd )*
 * logicalAnd     → equality ( "&&" equality )*
 * equality       → comparison ( ( "==" | "!=" ) comparison )*
 * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
 * term           → factor ( ( "+" | "-" ) factor )*
 * factor         → unary ( ( "*" | "/" | "%" ) unary )*
 * unary          → ( "!" | "-" ) unary | primary
 * primary        → literal | stateRef | functionCall | paramRef | arrayLiteral | objectLiteral | "(" expression ")"
 * literal        → NUMBER | STRING | BOOLEAN | NULL
 * stateRef       → "$state" ( "." IDENTIFIER | "[" NUMBER "]" )*
 * functionCall   → "$" IDENTIFIER "." IDENTIFIER "(" arguments? ")"
 * paramRef       → "$" IDENTIFIER
 * arrayLiteral   → "[" ( expression ( "," expression )* )? "]"
 * objectLiteral  → "{" ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )? "}"
 * arguments      → expression ( "," expression )*
 * ```
 *
 * Example:
 * ```
 * val lexer = ExpressionLexer("$math.add($state.count, 1)")
 * val tokens = lexer.tokenize()
 * val parser = ExpressionParser(tokens)
 * val ast = parser.parse()
 * ```
 */
class ExpressionParser(private val tokens: List<Token>) {

    private var current = 0

    /**
     * Parse the token stream into an AST.
     */
    fun parse(): ExpressionNode {
        return expression()
    }

    /**
     * Parse a logical OR expression.
     * logicalOr → logicalAnd ( "||" logicalAnd )*
     */
    private fun expression(): ExpressionNode {
        return logicalOr()
    }

    /**
     * Parse a logical OR expression.
     * logicalOr → logicalAnd ( "||" logicalAnd )*
     */
    private fun logicalOr(): ExpressionNode {
        var left = logicalAnd()

        while (match(TokenType.OR)) {
            val op = "||"
            val right = logicalAnd()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse a logical AND expression.
     * logicalAnd → equality ( "&&" equality )*
     */
    private fun logicalAnd(): ExpressionNode {
        var left = equality()

        while (match(TokenType.AND)) {
            val op = "&&"
            val right = equality()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse an equality expression.
     * equality → comparison ( ( "==" | "!=" ) comparison )*
     */
    private fun equality(): ExpressionNode {
        var left = comparison()

        while (matchAny(TokenType.EQUALS, TokenType.NOT_EQUALS)) {
            val op = previous().value
            val right = comparison()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse a comparison expression.
     * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
     */
    private fun comparison(): ExpressionNode {
        var left = term()

        while (matchAny(
                TokenType.GREATER_THAN,
                TokenType.GREATER_THAN_OR_EQUAL,
                TokenType.LESS_THAN,
                TokenType.LESS_THAN_OR_EQUAL
            )) {
            val op = previous().value
            val right = term()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse an additive expression.
     * term → factor ( ( "+" | "-" ) factor )*
     */
    private fun term(): ExpressionNode {
        var left = factor()

        while (matchAny(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous().value
            val right = factor()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse a multiplicative expression.
     * factor → unary ( ( "*" | "/" | "%" ) unary )*
     */
    private fun factor(): ExpressionNode {
        var left = unary()

        while (matchAny(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val op = previous().value
            val right = unary()
            left = ExpressionNode.BinaryOp(op, left, right)
        }

        return left
    }

    /**
     * Parse a unary expression.
     * unary → ( "!" | "-" ) unary | primary
     */
    private fun unary(): ExpressionNode {
        if (matchAny(TokenType.BANG, TokenType.MINUS)) {
            val op = previous().value
            val operand = unary()
            return ExpressionNode.UnaryOp(op, operand)
        }

        return primary()
    }

    /**
     * Parse a primary expression.
     * primary → literal | stateRef | functionCall | paramRef | arrayLiteral | objectLiteral | "(" expression ")"
     */
    private fun primary(): ExpressionNode {
        // Literals
        if (match(TokenType.NUMBER)) {
            val value = previous().value.toDouble()
            return ExpressionNode.Literal(LiteralValue.NumberValue(value))
        }

        if (match(TokenType.STRING)) {
            val value = previous().value
            return ExpressionNode.Literal(LiteralValue.StringValue(value))
        }

        if (match(TokenType.BOOLEAN)) {
            val value = previous().value == "true"
            return ExpressionNode.Literal(LiteralValue.BooleanValue(value))
        }

        if (match(TokenType.NULL)) {
            return ExpressionNode.Literal(LiteralValue.NullValue)
        }

        // Array literal: [...]
        if (match(TokenType.LBRACKET)) {
            return parseArrayLiteral()
        }

        // Object literal: {...}
        if (match(TokenType.LBRACE)) {
            return parseObjectLiteral()
        }

        // Dollar expressions: $state, $math.add(), $param
        if (match(TokenType.DOLLAR)) {
            val identifier = previous().value

            // State reference: $state.path.to.value
            if (identifier == "state") {
                return parseStateRef()
            }

            // Function call: $math.add(1, 2)
            if (peek().type == TokenType.DOT) {
                return parseFunctionCall(identifier)
            }

            // Parameter reference: $param
            return ExpressionNode.ParamRef(identifier)
        }

        // Module call: @voice.listen(), @device.screen.width()
        if (match(TokenType.AT)) {
            val moduleName = previous().value
            return parseModuleCall(moduleName)
        }

        // Grouped expression: (...)
        if (match(TokenType.LPAREN)) {
            val expr = expression()
            consume(TokenType.RPAREN, "Expected ')' after expression")
            return expr
        }

        // Identifier (for object literal keys or potential future extensions)
        if (match(TokenType.IDENTIFIER)) {
            val identifier = previous().value
            // This could be expanded for more complex scenarios
            return ExpressionNode.Literal(LiteralValue.StringValue(identifier))
        }

        throw ParserException("Unexpected token: ${peek()}")
    }

    /**
     * Parse a state reference.
     * stateRef → "$state" ( "." IDENTIFIER | "[" NUMBER "]" )*
     */
    private fun parseStateRef(): ExpressionNode {
        val path = mutableListOf<String>()

        while (match(TokenType.DOT)) {
            if (match(TokenType.IDENTIFIER)) {
                path.add(previous().value)
            } else {
                throw ParserException("Expected identifier after '.' in state reference")
            }
        }

        // Support array indexing: $state.items[0]
        while (match(TokenType.LBRACKET)) {
            if (match(TokenType.NUMBER)) {
                val index = previous().value.toInt().toString()
                path.add(index)
            } else {
                throw ParserException("Expected number in array index")
            }
            consume(TokenType.RBRACKET, "Expected ']' after array index")
        }

        if (path.isEmpty()) {
            throw ParserException("State reference must have at least one property")
        }

        return ExpressionNode.StateRef(path)
    }

    /**
     * Parse a function call.
     * functionCall → "$" IDENTIFIER "." IDENTIFIER "(" arguments? ")"
     */
    private fun parseFunctionCall(category: String): ExpressionNode {
        consume(TokenType.DOT, "Expected '.' after category in function call")

        if (!match(TokenType.IDENTIFIER)) {
            throw ParserException("Expected function name after '.'")
        }
        val functionName = previous().value

        consume(TokenType.LPAREN, "Expected '(' after function name")

        val args = mutableListOf<ExpressionNode>()
        if (!check(TokenType.RPAREN)) {
            do {
                args.add(expression())
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RPAREN, "Expected ')' after function arguments")

        return ExpressionNode.FunctionCall(category, functionName, args)
    }

    /**
     * Parse a module call.
     * moduleCall → "@" IDENTIFIER ( "." IDENTIFIER )+ "(" arguments? ")"
     *
     * Examples:
     * - @voice.listen()
     * - @device.screen.width()
     * - @browser.open("https://example.com")
     */
    private fun parseModuleCall(moduleName: String): ExpressionNode {
        consume(TokenType.DOT, "Expected '.' after module name '@$moduleName'")

        // Read method path (may contain dots: device.screen.width)
        val methodParts = mutableListOf<String>()

        if (!match(TokenType.IDENTIFIER)) {
            throw ParserException("Expected method name after '@$moduleName.'")
        }
        methodParts.add(previous().value)

        // Continue reading dotted path until we hit a paren
        while (peek().type == TokenType.DOT && peekNext().type == TokenType.IDENTIFIER) {
            consume(TokenType.DOT, "Expected '.'")
            consume(TokenType.IDENTIFIER, "Expected identifier")
            methodParts.add(previous().value)
        }

        val methodPath = methodParts.joinToString(".")

        // Parse arguments
        consume(TokenType.LPAREN, "Expected '(' after method name")

        val args = mutableListOf<ExpressionNode>()
        if (!check(TokenType.RPAREN)) {
            do {
                args.add(expression())
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RPAREN, "Expected ')' after module call arguments")

        return ExpressionNode.ModuleCall(moduleName, methodPath, args)
    }

    /**
     * Parse an array literal.
     * arrayLiteral → "[" ( expression ( "," expression )* )? "]"
     */
    private fun parseArrayLiteral(): ExpressionNode {
        val elements = mutableListOf<ExpressionNode>()

        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(expression())
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RBRACKET, "Expected ']' after array elements")

        return ExpressionNode.ArrayLiteral(elements)
    }

    /**
     * Parse an object literal.
     * objectLiteral → "{" ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )? "}"
     */
    private fun parseObjectLiteral(): ExpressionNode {
        val properties = mutableMapOf<String, ExpressionNode>()

        if (!check(TokenType.RBRACE)) {
            do {
                val key = if (match(TokenType.IDENTIFIER)) {
                    previous().value
                } else if (match(TokenType.STRING)) {
                    previous().value
                } else {
                    throw ParserException("Expected property name in object literal")
                }

                consume(TokenType.COLON, "Expected ':' after property name")
                val value = expression()
                properties[key] = value
            } while (match(TokenType.COMMA))
        }

        consume(TokenType.RBRACE, "Expected '}' after object properties")

        return ExpressionNode.ObjectLiteral(properties)
    }

    // ========== Helper Methods ==========

    /**
     * Check if the current token matches the given type, and consume it if so.
     */
    private fun match(type: TokenType): Boolean {
        if (check(type)) {
            advance()
            return true
        }
        return false
    }

    /**
     * Check if the current token matches any of the given types, and consume it if so.
     */
    private fun matchAny(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    /**
     * Check if the current token matches the given type.
     */
    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    /**
     * Consume the current token and return it, or throw an error if it doesn't match the expected type.
     */
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw ParserException("$message at position ${peek().position}")
    }

    /**
     * Advance to the next token and return the previous one.
     */
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    /**
     * Check if we've reached the end of the token stream.
     */
    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }

    /**
     * Get the current token without consuming it.
     */
    private fun peek(): Token {
        return tokens[current]
    }

    /**
     * Get the next token without consuming it.
     */
    private fun peekNext(): Token {
        return if (current + 1 < tokens.size) tokens[current + 1] else tokens[current]
    }

    /**
     * Get the previous token.
     */
    private fun previous(): Token {
        return tokens[current - 1]
    }
}

/**
 * Exception thrown during parsing.
 */
class ParserException(message: String) : Exception(message)

package com.avanues.avu.dsl.parser

import com.avanues.avu.codec.core.AvuHeader
import com.avanues.avu.dsl.ast.*
import com.avanues.avu.dsl.lexer.Token
import com.avanues.avu.dsl.lexer.TokenType

/**
 * Recursive descent parser for AVU DSL files.
 *
 * Transforms a token stream (from [com.augmentalis.voiceoscore.dsl.lexer.AvuDslLexer])
 * into an [AvuDslFile] AST. Supports collect-all error recovery with synchronization
 * at top-level declarations.
 *
 * Usage:
 * ```kotlin
 * val tokens = AvuDslLexer(source).tokenize()
 * val result = AvuDslParser(tokens).parse()
 * if (result.hasErrors) { /* report errors */ }
 * val ast = result.file
 * ```
 */
class AvuDslParser(private val tokens: List<Token>) {

    private var pos = 0
    private val errors = mutableListOf<ParseError>()

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    fun parse(): ParseResult {
        val headerLoc = currentLocation()
        val header = parseHeader()
        val declarations = mutableListOf<AvuAstNode.Declaration>()

        while (!isAtEnd()) {
            skipNewlines()
            if (isAtEnd()) break
            try {
                declarations.add(parseDeclaration())
            } catch (e: ParseException) {
                errors.add(e.error)
                synchronize()
            }
        }

        val file = AvuDslFile(header, declarations, headerLoc)
        return ParseResult(file, errors)
    }

    // =========================================================================
    // HEADER PARSING
    // =========================================================================

    /**
     * Parse header by collecting HEADER_LINE tokens, reconstructing content,
     * and delegating to [AvuHeader.parse]. DSL-specific sections (permissions,
     * triggers) are extracted from [AvuHeader.HeaderData.sections].
     */
    private fun parseHeader(): AvuDslHeader {
        val loc = currentLocation()

        if (!check(TokenType.HEADER_SEPARATOR)) {
            errors.add(ParseError("Expected header separator '---'", current().line, current().column))
            return emptyHeader(loc)
        }
        advance() // consume first ---
        skipNewlines()

        // Collect raw header lines between --- markers
        val headerLines = mutableListOf<String>()
        while (!check(TokenType.HEADER_SEPARATOR) && !isAtEnd()) {
            if (check(TokenType.HEADER_LINE)) {
                headerLines.add(current().lexeme)
            }
            advance()
        }

        if (check(TokenType.HEADER_SEPARATOR)) {
            advance() // consume closing ---
        } else {
            errors.add(ParseError("Expected closing header separator '---'", current().line, current().column))
        }
        skipNewlines()

        // Reconstruct content for AvuHeader.parse() with --- delimiters
        val headerContent = buildString {
            appendLine("---")
            headerLines.forEach { appendLine(it) }
            appendLine("---")
        }

        val (headerData, _) = AvuHeader.parse(headerContent)

        // Extract DSL-specific sections from generic sections map
        val permissions = headerData.sections["permissions"] ?: emptyList()
        val triggers = headerData.sections["triggers"] ?: emptyList()

        return AvuDslHeader(
            schema = headerData.schema,
            version = headerData.version,
            type = when (headerData.type.lowercase()) {
                "plugin" -> AvuDslFileType.PLUGIN
                else -> AvuDslFileType.WORKFLOW
            },
            metadata = headerData.metadata,
            codes = headerData.codes,
            permissions = permissions,
            triggers = triggers,
            location = loc
        )
    }

    private fun emptyHeader(loc: SourceLocation) = AvuDslHeader(
        schema = "avu-2.2",
        version = "1.0.0",
        type = AvuDslFileType.WORKFLOW,
        metadata = emptyMap(),
        codes = emptyMap(),
        permissions = emptyList(),
        triggers = emptyList(),
        location = loc
    )

    // =========================================================================
    // DECLARATION PARSING
    // =========================================================================

    private fun parseDeclaration(): AvuAstNode.Declaration {
        skipNewlines()
        return when {
            check(TokenType.AT_WORKFLOW) -> parseWorkflow()
            check(TokenType.AT_DEFINE) -> parseFunctionDef()
            check(TokenType.AT_ON) -> parseTriggerHandler()
            else -> throw parseError("Expected @workflow, @define, or @on, got ${current().type}")
        }
    }

    private fun parseWorkflow(): AvuAstNode.Declaration.Workflow {
        val loc = currentLocation()
        expect(TokenType.AT_WORKFLOW)
        val name = expectStringLiteral()
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Declaration.Workflow(name, body, loc)
    }

    private fun parseFunctionDef(): AvuAstNode.Declaration.FunctionDef {
        val loc = currentLocation()
        expect(TokenType.AT_DEFINE)
        val name = expectIdentifier()
        expect(TokenType.LPAREN)
        val params = parseParameterNames()
        expect(TokenType.RPAREN)
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Declaration.FunctionDef(name, params, body, loc)
    }

    private fun parseTriggerHandler(): AvuAstNode.Declaration.TriggerHandler {
        val loc = currentLocation()
        expect(TokenType.AT_ON)
        val pattern = expectStringLiteral()
        val captureVars = extractCaptureVars(pattern)
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Declaration.TriggerHandler(pattern, captureVars, body, loc)
    }

    /**
     * Extract {variable} captures from a trigger pattern string.
     * e.g., "call {contact}" -> ["contact"]
     */
    private fun extractCaptureVars(pattern: String): List<String> {
        val vars = mutableListOf<String>()
        var i = 0
        while (i < pattern.length) {
            if (pattern[i] == '{') {
                val end = pattern.indexOf('}', i + 1)
                if (end > i + 1) {
                    vars.add(pattern.substring(i + 1, end).trim())
                    i = end + 1
                } else {
                    i++
                }
            } else {
                i++
            }
        }
        return vars
    }

    // =========================================================================
    // BLOCK PARSING
    // =========================================================================

    private fun parseBlock(): List<AvuAstNode.Statement> {
        if (!check(TokenType.INDENT)) {
            errors.add(ParseError("Expected indented block", current().line, current().column))
            return emptyList()
        }
        advance() // consume INDENT

        val statements = mutableListOf<AvuAstNode.Statement>()
        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            skipNewlines()
            if (check(TokenType.DEDENT) || isAtEnd()) break
            try {
                statements.add(parseStatement())
            } catch (e: ParseException) {
                errors.add(e.error)
                synchronizeInBlock()
            }
            skipNewlines()
        }

        if (check(TokenType.DEDENT)) {
            advance() // consume DEDENT
        }
        return statements
    }

    // =========================================================================
    // STATEMENT PARSING
    // =========================================================================

    private fun parseStatement(): AvuAstNode.Statement {
        return when {
            check(TokenType.CODE_NAME) -> parseCodeInvocation()
            check(TokenType.AT_IF) -> parseIfElse()
            check(TokenType.AT_WAIT) -> parseWait()
            check(TokenType.AT_REPEAT) -> parseRepeat()
            check(TokenType.AT_WHILE) -> parseWhile()
            check(TokenType.AT_SEQUENCE) -> parseSequence()
            check(TokenType.AT_SET) -> parseAssignment()
            check(TokenType.AT_LOG) -> parseLog()
            check(TokenType.AT_RETURN) -> parseReturn()
            check(TokenType.AT_EMIT) -> parseEmit()
            check(TokenType.IDENTIFIER) -> parseFunctionCallStatement()
            else -> throw parseError("Unexpected token: ${current().type} '${current().lexeme}'")
        }
    }

    private fun parseCodeInvocation(): AvuAstNode.Statement.CodeInvocation {
        val loc = currentLocation()
        val code = expect(TokenType.CODE_NAME).lexeme
        expect(TokenType.LPAREN)
        val args = parseNamedArguments()
        expect(TokenType.RPAREN)
        return AvuAstNode.Statement.CodeInvocation(code, args, loc)
    }

    private fun parseFunctionCallStatement(): AvuAstNode.Statement.FunctionCall {
        val loc = currentLocation()
        val name = expectIdentifier()
        expect(TokenType.LPAREN)
        val args = parseNamedArguments()
        expect(TokenType.RPAREN)
        return AvuAstNode.Statement.FunctionCall(name, args, loc)
    }

    private fun parseIfElse(): AvuAstNode.Statement.IfElse {
        val loc = currentLocation()
        expect(TokenType.AT_IF)
        val condition = parseExpression()
        expectNewline()
        val thenBody = parseBlock()

        val elseBody = if (check(TokenType.AT_ELSE)) {
            advance() // consume @else
            skipNewlines()
            if (check(TokenType.INDENT)) {
                parseBlock()
            } else {
                expectNewline()
                parseBlock()
            }
        } else {
            emptyList()
        }

        return AvuAstNode.Statement.IfElse(condition, thenBody, elseBody, loc)
    }

    private fun parseWait(): AvuAstNode.Statement {
        val loc = currentLocation()
        expect(TokenType.AT_WAIT)

        // Disambiguate: @wait <number> vs @wait <condition> timeout <number>
        val expr = parseExpression()

        return if (check(TokenType.KW_TIMEOUT)) {
            advance() // consume 'timeout'
            val timeout = parseExpression()
            AvuAstNode.Statement.WaitCondition(expr, timeout, loc)
        } else {
            AvuAstNode.Statement.WaitDelay(expr, loc)
        }
    }

    private fun parseRepeat(): AvuAstNode.Statement.Repeat {
        val loc = currentLocation()
        expect(TokenType.AT_REPEAT)
        val count = parseExpression()
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Statement.Repeat(count, body, loc)
    }

    private fun parseWhile(): AvuAstNode.Statement.While {
        val loc = currentLocation()
        expect(TokenType.AT_WHILE)
        val condition = parseExpression()
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Statement.While(condition, body, loc)
    }

    private fun parseSequence(): AvuAstNode.Statement.Sequence {
        val loc = currentLocation()
        expect(TokenType.AT_SEQUENCE)
        expectNewline()
        val body = parseBlock()
        return AvuAstNode.Statement.Sequence(body, loc)
    }

    private fun parseAssignment(): AvuAstNode.Statement.Assignment {
        val loc = currentLocation()
        expect(TokenType.AT_SET)
        val name = expectIdentifier()
        expect(TokenType.ASSIGN)
        val value = parseExpression()
        return AvuAstNode.Statement.Assignment(name, value, loc)
    }

    private fun parseLog(): AvuAstNode.Statement.Log {
        val loc = currentLocation()
        expect(TokenType.AT_LOG)
        val message = parseExpression()
        return AvuAstNode.Statement.Log(message, loc)
    }

    private fun parseReturn(): AvuAstNode.Statement.Return {
        val loc = currentLocation()
        expect(TokenType.AT_RETURN)
        val value = if (!check(TokenType.NEWLINE) && !check(TokenType.DEDENT) && !isAtEnd()) {
            parseExpression()
        } else {
            null
        }
        return AvuAstNode.Statement.Return(value, loc)
    }

    private fun parseEmit(): AvuAstNode.Statement.Emit {
        val loc = currentLocation()
        expect(TokenType.AT_EMIT)
        val eventName = expectStringLiteral()
        val data = if (!check(TokenType.NEWLINE) && !check(TokenType.DEDENT) && !isAtEnd()) {
            parseExpression()
        } else {
            null
        }
        return AvuAstNode.Statement.Emit(eventName, data, loc)
    }

    // =========================================================================
    // ARGUMENT PARSING
    // =========================================================================

    private fun parseNamedArguments(): List<AvuAstNode.NamedArgument> {
        val args = mutableListOf<AvuAstNode.NamedArgument>()
        if (check(TokenType.RPAREN)) return args

        args.add(parseNamedArgument())
        while (check(TokenType.COMMA)) {
            advance() // consume comma
            args.add(parseNamedArgument())
        }
        return args
    }

    private fun parseNamedArgument(): AvuAstNode.NamedArgument {
        val loc = currentLocation()

        // Try to detect named argument: identifier followed by colon
        if ((check(TokenType.IDENTIFIER) || check(TokenType.CODE_NAME)) && peekNext()?.type == TokenType.COLON) {
            val name = current().lexeme
            advance() // consume name
            advance() // consume colon
            val value = parseExpression()
            return AvuAstNode.NamedArgument(name, value, loc)
        }

        // Positional argument
        val value = parseExpression()
        return AvuAstNode.NamedArgument(null, value, loc)
    }

    private fun parseParameterNames(): List<String> {
        val params = mutableListOf<String>()
        if (check(TokenType.RPAREN)) return params

        params.add(expectIdentifier())
        while (check(TokenType.COMMA)) {
            advance() // consume comma
            params.add(expectIdentifier())
        }
        return params
    }

    // =========================================================================
    // EXPRESSION PARSING (Precedence Climbing)
    // =========================================================================

    private fun parseExpression(): AvuAstNode.Expression = parseOr()

    // Level 1: or
    private fun parseOr(): AvuAstNode.Expression {
        var left = parseAnd()
        while (check(TokenType.OR)) {
            val loc = currentLocation()
            advance()
            val right = parseAnd()
            left = AvuAstNode.Expression.BinaryOp(left, BinaryOperator.OR, right, loc)
        }
        return left
    }

    // Level 2: and
    private fun parseAnd(): AvuAstNode.Expression {
        var left = parseEquality()
        while (check(TokenType.AND)) {
            val loc = currentLocation()
            advance()
            val right = parseEquality()
            left = AvuAstNode.Expression.BinaryOp(left, BinaryOperator.AND, right, loc)
        }
        return left
    }

    // Level 3: ==, !=
    private fun parseEquality(): AvuAstNode.Expression {
        var left = parseComparison()
        while (check(TokenType.EQ) || check(TokenType.NEQ)) {
            val loc = currentLocation()
            val op = if (current().type == TokenType.EQ) BinaryOperator.EQ else BinaryOperator.NEQ
            advance()
            val right = parseComparison()
            left = AvuAstNode.Expression.BinaryOp(left, op, right, loc)
        }
        return left
    }

    // Level 4: <, >, <=, >=
    private fun parseComparison(): AvuAstNode.Expression {
        var left = parseAddition()
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LTE) || check(TokenType.GTE)) {
            val loc = currentLocation()
            val op = when (current().type) {
                TokenType.LT -> BinaryOperator.LT
                TokenType.GT -> BinaryOperator.GT
                TokenType.LTE -> BinaryOperator.LTE
                TokenType.GTE -> BinaryOperator.GTE
                else -> throw parseError("Unexpected comparison operator")
            }
            advance()
            val right = parseAddition()
            left = AvuAstNode.Expression.BinaryOp(left, op, right, loc)
        }
        return left
    }

    // Level 5: +, -
    private fun parseAddition(): AvuAstNode.Expression {
        var left = parseMultiplication()
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            val loc = currentLocation()
            val op = if (current().type == TokenType.PLUS) BinaryOperator.PLUS else BinaryOperator.MINUS
            advance()
            val right = parseMultiplication()
            left = AvuAstNode.Expression.BinaryOp(left, op, right, loc)
        }
        return left
    }

    // Level 6: *, /
    private fun parseMultiplication(): AvuAstNode.Expression {
        var left = parseUnary()
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            val loc = currentLocation()
            val op = if (current().type == TokenType.STAR) BinaryOperator.STAR else BinaryOperator.SLASH
            advance()
            val right = parseUnary()
            left = AvuAstNode.Expression.BinaryOp(left, op, right, loc)
        }
        return left
    }

    // Level 7: not, - (negate)
    private fun parseUnary(): AvuAstNode.Expression {
        if (check(TokenType.NOT)) {
            val loc = currentLocation()
            advance()
            return AvuAstNode.Expression.UnaryOp(UnaryOperator.NOT, parseUnary(), loc)
        }
        if (check(TokenType.MINUS)) {
            val loc = currentLocation()
            advance()
            return AvuAstNode.Expression.UnaryOp(UnaryOperator.NEGATE, parseUnary(), loc)
        }
        return parsePostfix()
    }

    // Level 8: .member, (args)
    private fun parsePostfix(): AvuAstNode.Expression {
        var expr = parsePrimary()
        while (true) {
            when {
                check(TokenType.DOT) -> {
                    advance() // consume dot
                    val member = expectIdentifier()
                    expr = AvuAstNode.Expression.MemberAccess(expr, member, expr.location)
                }
                check(TokenType.LPAREN) -> {
                    advance() // consume (
                    val args = parseExpressionList()
                    expect(TokenType.RPAREN)
                    expr = AvuAstNode.Expression.CallExpression(expr, args, expr.location)
                }
                else -> break
            }
        }
        return expr
    }

    // Level 9: Primary
    private fun parsePrimary(): AvuAstNode.Expression {
        val token = current()
        return when (token.type) {
            TokenType.STRING_LITERAL -> {
                advance()
                AvuAstNode.Expression.StringLiteral(token.literal as String, tokenLocation(token))
            }
            TokenType.INT_LITERAL -> {
                advance()
                AvuAstNode.Expression.IntLiteral(token.literal as Int, tokenLocation(token))
            }
            TokenType.FLOAT_LITERAL -> {
                advance()
                AvuAstNode.Expression.FloatLiteral(token.literal as Double, tokenLocation(token))
            }
            TokenType.BOOLEAN_LITERAL -> {
                advance()
                AvuAstNode.Expression.BooleanLiteral(token.literal as Boolean, tokenLocation(token))
            }
            TokenType.VARIABLE_REF -> {
                advance()
                val name = (token.literal as? String) ?: token.lexeme.removePrefix("$")
                AvuAstNode.Expression.VariableRef(name, tokenLocation(token))
            }
            TokenType.IDENTIFIER -> {
                advance()
                AvuAstNode.Expression.Identifier(token.lexeme, tokenLocation(token))
            }
            TokenType.CODE_NAME -> {
                // CODE_NAME can appear as identifier in expressions (rare but possible)
                advance()
                AvuAstNode.Expression.Identifier(token.lexeme, tokenLocation(token))
            }
            TokenType.LPAREN -> {
                advance() // consume (
                val inner = parseExpression()
                expect(TokenType.RPAREN)
                AvuAstNode.Expression.Grouped(inner, tokenLocation(token))
            }
            else -> throw parseError("Expected expression, got ${token.type} '${token.lexeme}'")
        }
    }

    private fun parseExpressionList(): List<AvuAstNode.Expression> {
        val exprs = mutableListOf<AvuAstNode.Expression>()
        if (check(TokenType.RPAREN)) return exprs

        exprs.add(parseExpression())
        while (check(TokenType.COMMA)) {
            advance()
            exprs.add(parseExpression())
        }
        return exprs
    }

    // =========================================================================
    // ERROR RECOVERY
    // =========================================================================

    private fun synchronize() {
        while (!isAtEnd()) {
            val type = current().type
            if (type == TokenType.AT_WORKFLOW || type == TokenType.AT_DEFINE ||
                type == TokenType.AT_ON || type == TokenType.EOF
            ) return
            advance()
        }
    }

    private fun synchronizeInBlock() {
        while (!isAtEnd()) {
            val type = current().type
            if (type == TokenType.NEWLINE) {
                advance()
                return
            }
            if (type == TokenType.DEDENT || type == TokenType.AT_WORKFLOW ||
                type == TokenType.AT_DEFINE || type == TokenType.AT_ON
            ) return
            advance()
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun current(): Token = tokens[pos.coerceAtMost(tokens.size - 1)]

    private fun peekNext(): Token? = if (pos + 1 < tokens.size) tokens[pos + 1] else null

    private fun isAtEnd(): Boolean = pos >= tokens.size || current().type == TokenType.EOF

    private fun check(type: TokenType): Boolean = !isAtEnd() && current().type == type

    private fun advance(): Token {
        val token = current()
        if (!isAtEnd()) pos++
        return token
    }

    private fun expect(type: TokenType): Token {
        if (check(type)) return advance()
        throw parseError("Expected $type, got ${current().type} '${current().lexeme}'")
    }

    private fun expectStringLiteral(): String {
        val token = expect(TokenType.STRING_LITERAL)
        return token.literal as? String ?: token.lexeme.removeSurrounding("\"")
    }

    private fun expectIdentifier(): String {
        val token = expect(TokenType.IDENTIFIER)
        return token.lexeme
    }

    private fun expectNewline() {
        if (check(TokenType.NEWLINE)) {
            advance()
        }
        // Also acceptable: INDENT (block follows immediately), DEDENT, EOF
    }

    private fun skipNewlines() {
        while (check(TokenType.NEWLINE)) advance()
    }

    private fun currentLocation(): SourceLocation {
        val t = current()
        return SourceLocation(t.line, t.column)
    }

    private fun tokenLocation(token: Token): SourceLocation =
        SourceLocation(token.line, token.column, token.lexeme.length)

    private fun parseError(message: String): ParseException {
        val t = current()
        return ParseException(ParseError(message, t.line, t.column))
    }
}

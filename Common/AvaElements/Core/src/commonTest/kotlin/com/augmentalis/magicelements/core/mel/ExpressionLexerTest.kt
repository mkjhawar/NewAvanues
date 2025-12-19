package com.augmentalis.magicelements.core.mel

import kotlin.test.*

/**
 * Unit tests for ExpressionLexer.
 * Tests tokenization of all token types, literals, operators, and error cases.
 */
class ExpressionLexerTest {

    // ========== Basic Token Types ==========

    @Test
    fun `tokenizes dollar state reference`() {
        val lexer = ExpressionLexer("\$state")
        val tokens = lexer.tokenize()
        assertEquals(2, tokens.size)
        assertEquals(TokenType.DOLLAR, tokens[0].type)
        assertEquals("state", tokens[0].value)
        assertEquals(TokenType.EOF, tokens[1].type)
    }

    @Test
    fun `tokenizes dollar function reference`() {
        val lexer = ExpressionLexer("\$math")
        val tokens = lexer.tokenize()
        assertEquals(2, tokens.size)
        assertEquals(TokenType.DOLLAR, tokens[0].type)
        assertEquals("math", tokens[0].value)
    }

    @Test
    fun `tokenizes dot operator`() {
        val lexer = ExpressionLexer(".")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.DOT, tokens[0].type)
    }

    @Test
    fun `tokenizes parentheses`() {
        val lexer = ExpressionLexer("()")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LPAREN, tokens[0].type)
        assertEquals(TokenType.RPAREN, tokens[1].type)
    }

    @Test
    fun `tokenizes brackets`() {
        val lexer = ExpressionLexer("[]")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LBRACKET, tokens[0].type)
        assertEquals(TokenType.RBRACKET, tokens[1].type)
    }

    @Test
    fun `tokenizes braces`() {
        val lexer = ExpressionLexer("{}")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LBRACE, tokens[0].type)
        assertEquals(TokenType.RBRACE, tokens[1].type)
    }

    @Test
    fun `tokenizes comma`() {
        val lexer = ExpressionLexer(",")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.COMMA, tokens[0].type)
    }

    @Test
    fun `tokenizes colon`() {
        val lexer = ExpressionLexer(":")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.COLON, tokens[0].type)
    }

    // ========== String Literals ==========

    @Test
    fun `tokenizes double quoted string`() {
        val lexer = ExpressionLexer("\"hello world\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello world", tokens[0].value)
    }

    @Test
    fun `tokenizes single quoted string`() {
        val lexer = ExpressionLexer("'hello world'")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello world", tokens[0].value)
    }

    @Test
    fun `tokenizes empty string`() {
        val lexer = ExpressionLexer("\"\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("", tokens[0].value)
    }

    @Test
    fun `tokenizes string with newline escape`() {
        val lexer = ExpressionLexer("\"hello\\nworld\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello\nworld", tokens[0].value)
    }

    @Test
    fun `tokenizes string with tab escape`() {
        val lexer = ExpressionLexer("\"hello\\tworld\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello\tworld", tokens[0].value)
    }

    @Test
    fun `tokenizes string with backslash escape`() {
        val lexer = ExpressionLexer("\"hello\\\\world\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello\\world", tokens[0].value)
    }

    @Test
    fun `tokenizes string with quote escape`() {
        val lexer = ExpressionLexer("\"hello\\\"world\"")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STRING, tokens[0].type)
        assertEquals("hello\"world", tokens[0].value)
    }

    @Test
    fun `throws on unterminated string`() {
        val lexer = ExpressionLexer("\"hello")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    // ========== Number Literals ==========

    @Test
    fun `tokenizes integer`() {
        val lexer = ExpressionLexer("42")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("42", tokens[0].value)
    }

    @Test
    fun `tokenizes float`() {
        val lexer = ExpressionLexer("3.14")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("3.14", tokens[0].value)
    }

    @Test
    fun `tokenizes zero`() {
        val lexer = ExpressionLexer("0")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("0", tokens[0].value)
    }

    @Test
    fun `tokenizes scientific notation lowercase e`() {
        val lexer = ExpressionLexer("1e10")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("1e10", tokens[0].value)
    }

    @Test
    fun `tokenizes scientific notation uppercase E`() {
        val lexer = ExpressionLexer("2.5E-3")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("2.5E-3", tokens[0].value)
    }

    @Test
    fun `tokenizes scientific notation with positive exponent`() {
        val lexer = ExpressionLexer("1.5e+20")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals("1.5e+20", tokens[0].value)
    }

    // ========== Boolean and Null ==========

    @Test
    fun `tokenizes true`() {
        val lexer = ExpressionLexer("true")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.BOOLEAN, tokens[0].type)
        assertEquals("true", tokens[0].value)
    }

    @Test
    fun `tokenizes false`() {
        val lexer = ExpressionLexer("false")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.BOOLEAN, tokens[0].type)
        assertEquals("false", tokens[0].value)
    }

    @Test
    fun `tokenizes null`() {
        val lexer = ExpressionLexer("null")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NULL, tokens[0].type)
        assertEquals("null", tokens[0].value)
    }

    // ========== Identifiers ==========

    @Test
    fun `tokenizes identifier`() {
        val lexer = ExpressionLexer("myVariable")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("myVariable", tokens[0].value)
    }

    @Test
    fun `tokenizes identifier with underscore`() {
        val lexer = ExpressionLexer("my_variable_123")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.IDENTIFIER, tokens[0].type)
        assertEquals("my_variable_123", tokens[0].value)
    }

    // ========== Arithmetic Operators ==========

    @Test
    fun `tokenizes plus`() {
        val lexer = ExpressionLexer("+")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.PLUS, tokens[0].type)
    }

    @Test
    fun `tokenizes minus`() {
        val lexer = ExpressionLexer("-")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.MINUS, tokens[0].type)
    }

    @Test
    fun `tokenizes star`() {
        val lexer = ExpressionLexer("*")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.STAR, tokens[0].type)
    }

    @Test
    fun `tokenizes slash`() {
        val lexer = ExpressionLexer("/")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.SLASH, tokens[0].type)
    }

    @Test
    fun `tokenizes percent`() {
        val lexer = ExpressionLexer("%")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.PERCENT, tokens[0].type)
    }

    // ========== Comparison Operators ==========

    @Test
    fun `tokenizes equals`() {
        val lexer = ExpressionLexer("==")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.EQUALS, tokens[0].type)
        assertEquals("==", tokens[0].value)
    }

    @Test
    fun `tokenizes not equals`() {
        val lexer = ExpressionLexer("!=")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.NOT_EQUALS, tokens[0].type)
        assertEquals("!=", tokens[0].value)
    }

    @Test
    fun `tokenizes greater than`() {
        val lexer = ExpressionLexer(">")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.GREATER_THAN, tokens[0].type)
    }

    @Test
    fun `tokenizes greater than or equal`() {
        val lexer = ExpressionLexer(">=")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.GREATER_THAN_OR_EQUAL, tokens[0].type)
        assertEquals(">=", tokens[0].value)
    }

    @Test
    fun `tokenizes less than`() {
        val lexer = ExpressionLexer("<")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LESS_THAN, tokens[0].type)
    }

    @Test
    fun `tokenizes less than or equal`() {
        val lexer = ExpressionLexer("<=")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LESS_THAN_OR_EQUAL, tokens[0].type)
        assertEquals("<=", tokens[0].value)
    }

    // ========== Logical Operators ==========

    @Test
    fun `tokenizes logical AND`() {
        val lexer = ExpressionLexer("&&")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.AND, tokens[0].type)
        assertEquals("&&", tokens[0].value)
    }

    @Test
    fun `tokenizes logical OR`() {
        val lexer = ExpressionLexer("||")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.OR, tokens[0].type)
        assertEquals("||", tokens[0].value)
    }

    @Test
    fun `tokenizes bang`() {
        val lexer = ExpressionLexer("!")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.BANG, tokens[0].type)
    }

    // ========== Complex Expressions ==========

    @Test
    fun `tokenizes state reference with path`() {
        val lexer = ExpressionLexer("\$state.display")
        val tokens = lexer.tokenize()
        assertEquals(5, tokens.size) // $state, ., display, EOF
        assertEquals(TokenType.DOLLAR, tokens[0].type)
        assertEquals("state", tokens[0].value)
        assertEquals(TokenType.DOT, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals("display", tokens[2].value)
    }

    @Test
    fun `tokenizes nested state reference`() {
        val lexer = ExpressionLexer("\$state.user.name")
        val tokens = lexer.tokenize()
        assertEquals(7, tokens.size) // $state, ., user, ., name, EOF
        assertEquals("state", tokens[0].value)
        assertEquals("user", tokens[2].value)
        assertEquals("name", tokens[4].value)
    }

    @Test
    fun `tokenizes function call`() {
        val lexer = ExpressionLexer("\$math.add(1, 2)")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.DOLLAR, tokens[0].type)
        assertEquals("math", tokens[0].value)
        assertEquals(TokenType.DOT, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals("add", tokens[2].value)
        assertEquals(TokenType.LPAREN, tokens[3].type)
        assertEquals(TokenType.NUMBER, tokens[4].type)
        assertEquals("1", tokens[4].value)
        assertEquals(TokenType.COMMA, tokens[5].type)
        assertEquals(TokenType.NUMBER, tokens[6].type)
        assertEquals("2", tokens[6].value)
        assertEquals(TokenType.RPAREN, tokens[7].type)
    }

    @Test
    fun `tokenizes binary operation`() {
        val lexer = ExpressionLexer("\$state.a + 1")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.DOLLAR, tokens[0].type)
        assertEquals(TokenType.DOT, tokens[1].type)
        assertEquals(TokenType.IDENTIFIER, tokens[2].type)
        assertEquals(TokenType.PLUS, tokens[3].type)
        assertEquals(TokenType.NUMBER, tokens[4].type)
    }

    @Test
    fun `tokenizes array literal`() {
        val lexer = ExpressionLexer("[1, 2, 3]")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LBRACKET, tokens[0].type)
        assertEquals(TokenType.NUMBER, tokens[1].type)
        assertEquals(TokenType.COMMA, tokens[2].type)
        assertEquals(TokenType.NUMBER, tokens[3].type)
        assertEquals(TokenType.COMMA, tokens[4].type)
        assertEquals(TokenType.NUMBER, tokens[5].type)
        assertEquals(TokenType.RBRACKET, tokens[6].type)
    }

    @Test
    fun `tokenizes object literal`() {
        val lexer = ExpressionLexer("{x: 10, y: 20}")
        val tokens = lexer.tokenize()
        assertEquals(TokenType.LBRACE, tokens[0].type)
        assertEquals(TokenType.IDENTIFIER, tokens[1].type)
        assertEquals("x", tokens[1].value)
        assertEquals(TokenType.COLON, tokens[2].type)
        assertEquals(TokenType.NUMBER, tokens[3].type)
        assertEquals("10", tokens[3].value)
        assertEquals(TokenType.COMMA, tokens[4].type)
    }

    // ========== Whitespace Handling ==========

    @Test
    fun `skips whitespace`() {
        val lexer = ExpressionLexer("  1   +   2  ")
        val tokens = lexer.tokenize()
        assertEquals(4, tokens.size) // 1, +, 2, EOF
        assertEquals(TokenType.NUMBER, tokens[0].type)
        assertEquals(TokenType.PLUS, tokens[1].type)
        assertEquals(TokenType.NUMBER, tokens[2].type)
    }

    @Test
    fun `skips newlines and tabs`() {
        val lexer = ExpressionLexer("1\n+\t2")
        val tokens = lexer.tokenize()
        assertEquals(4, tokens.size) // 1, +, 2, EOF
    }

    // ========== Error Cases ==========

    @Test
    fun `throws on dollar without identifier`() {
        val lexer = ExpressionLexer("$")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    @Test
    fun `throws on dollar with number`() {
        val lexer = ExpressionLexer("\$123")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    @Test
    fun `throws on single equals`() {
        val lexer = ExpressionLexer("=")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    @Test
    fun `throws on single ampersand`() {
        val lexer = ExpressionLexer("&")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    @Test
    fun `throws on single pipe`() {
        val lexer = ExpressionLexer("|")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    @Test
    fun `throws on unexpected character`() {
        val lexer = ExpressionLexer("@")
        assertFailsWith<LexerException> {
            lexer.tokenize()
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun `tokenizes empty string input`() {
        val lexer = ExpressionLexer("")
        val tokens = lexer.tokenize()
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens[0].type)
    }

    @Test
    fun `tokenizes only whitespace`() {
        val lexer = ExpressionLexer("   ")
        val tokens = lexer.tokenize()
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens[0].type)
    }

    @Test
    fun `tracks token positions`() {
        val lexer = ExpressionLexer("1 + 2")
        val tokens = lexer.tokenize()
        assertEquals(0, tokens[0].position) // 1
        assertEquals(2, tokens[1].position) // +
        assertEquals(4, tokens[2].position) // 2
    }
}

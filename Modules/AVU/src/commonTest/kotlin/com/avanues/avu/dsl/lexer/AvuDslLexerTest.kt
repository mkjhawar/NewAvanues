package com.avanues.avu.dsl.lexer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AvuDslLexer — AVU DSL tokenizer.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class AvuDslLexerTest {

    private fun tokenize(source: String): List<Token> = AvuDslLexer(source).tokenize()

    private fun types(source: String): List<TokenType> =
        tokenize(source).map { it.type }

    // ── Basics ────────────────────────────────────────────────────────────────

    @Test
    fun emptySource_produces_only_EOF() {
        val tokens = tokenize("")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.EOF, tokens.first().type)
    }

    @Test
    fun comment_is_discarded() {
        // Comments must NOT appear in the token stream
        val tokens = tokenize("# This is a comment\n")
        val nonEof = tokens.filter { it.type != TokenType.EOF && it.type != TokenType.NEWLINE }
        assertTrue(nonEof.isEmpty())
    }

    @Test
    fun string_literal_double_quotes_is_tokenized() {
        val tokens = tokenize("\"hello world\"")
        val str = tokens.first { it.type == TokenType.STRING_LITERAL }
        assertEquals("hello world", str.literal as String)
    }

    @Test
    fun integer_literal_is_tokenized() {
        val tokens = tokenize("42")
        val int = tokens.first { it.type == TokenType.INT_LITERAL }
        assertEquals(42, int.literal as Int)
    }

    @Test
    fun float_literal_is_tokenized() {
        val tokens = tokenize("3.14")
        val float = tokens.first { it.type == TokenType.FLOAT_LITERAL }
        assertEquals(3.14, float.literal as Double, 0.0001)
    }

    @Test
    fun boolean_literals_are_tokenized() {
        val t = tokenize("true false")
        val boolTokens = t.filter { it.type == TokenType.BOOLEAN_LITERAL }
        assertEquals(2, boolTokens.size)
        assertEquals(true, boolTokens[0].literal)
        assertEquals(false, boolTokens[1].literal)
    }

    // ── Identifiers and Keywords ──────────────────────────────────────────────

    @Test
    fun three_uppercase_letters_produce_CODE_NAME() {
        val tokens = tokenize("VCM")
        assertTrue(tokens.any { it.type == TokenType.CODE_NAME && it.lexeme == "VCM" })
    }

    @Test
    fun lowercase_word_produces_IDENTIFIER() {
        val tokens = tokenize("myVar")
        assertTrue(tokens.any { it.type == TokenType.IDENTIFIER && it.lexeme == "myVar" })
    }

    @Test
    fun variable_ref_dollar_prefix() {
        val tokens = tokenize("\$myVar")
        val varRef = tokens.first { it.type == TokenType.VARIABLE_REF }
        assertEquals("myVar", varRef.literal as String)
    }

    // ── Directives ────────────────────────────────────────────────────────────

    @Test
    fun at_workflow_directive_is_recognized() {
        val tokens = tokenize("@workflow")
        assertTrue(tokens.any { it.type == TokenType.AT_WORKFLOW })
    }

    @Test
    fun at_if_and_at_else_are_recognized() {
        val typeList = types("@if @else")
        assertTrue(TokenType.AT_IF in typeList)
        assertTrue(TokenType.AT_ELSE in typeList)
    }

    // ── Operators ────────────────────────────────────────────────────────────

    @Test
    fun equality_operators_are_tokenized() {
        val typeList = types("== !=")
        assertTrue(TokenType.EQ in typeList)
        assertTrue(TokenType.NEQ in typeList)
    }

    @Test
    fun comparison_operators_lte_gte() {
        val typeList = types("<= >=")
        assertTrue(TokenType.LTE in typeList)
        assertTrue(TokenType.GTE in typeList)
    }

    // ── Header Section ────────────────────────────────────────────────────────

    @Test
    fun header_separator_is_tokenized() {
        val tokens = tokenize("---\n")
        assertTrue(tokens.any { it.type == TokenType.HEADER_SEPARATOR })
    }

    @Test
    fun lines_inside_header_produce_HEADER_LINE_tokens() {
        val source = "---\nschema: avu-2.2\nversion: 1.0.0\n---\n"
        val tokens = tokenize(source)
        val headerLines = tokens.filter { it.type == TokenType.HEADER_LINE }
        assertTrue(headerLines.any { it.lexeme.startsWith("schema") })
        assertTrue(headerLines.any { it.lexeme.startsWith("version") })
    }

    // ── Indentation ───────────────────────────────────────────────────────────

    @Test
    fun indented_block_produces_INDENT_and_DEDENT() {
        // Simulated indented body after header close
        val source = "---\n---\n@workflow \"test\"\n    VCM(action: \"CLICK\")\n"
        val tokens = tokenize(source)
        assertTrue(tokens.any { it.type == TokenType.INDENT })
        assertTrue(tokens.any { it.type == TokenType.DEDENT })
    }
}

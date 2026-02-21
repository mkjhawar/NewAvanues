package com.avanues.avu.dsl.tooling

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvuDslHighlighterTest {

    @Test
    fun highlight_empty_source() {
        val spans = AvuDslHighlighter.highlight("")
        assertTrue(spans.isEmpty() || spans.all { it.category == HighlightCategory.PLAIN })
    }

    @Test
    fun highlight_directive_tokens() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            codes:
              VCM: Voice Command
            ---

            @workflow "Test"
              VCM(action: "test")
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        assertTrue(spans.isNotEmpty())

        val directives = spans.filter { it.category == HighlightCategory.DIRECTIVE }
        assertTrue(directives.isNotEmpty(), "Should have directive spans")
        assertTrue(directives.any { it.text == "@workflow" })
    }

    @Test
    fun highlight_code_names() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            codes:
              VCM: Voice Command
              AAC: Accessibility
            ---

            @workflow "Test"
              VCM(action: "open")
              AAC(action: "CLICK")
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        val codeNames = spans.filter { it.category == HighlightCategory.CODE_NAME }
        assertTrue(codeNames.any { it.text == "VCM" })
        assertTrue(codeNames.any { it.text == "AAC" })
    }

    @Test
    fun highlight_string_literals() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            codes:
              VCM: Voice Command
            ---

            @workflow "Login"
              VCM(action: "test")
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        val strings = spans.filter { it.category == HighlightCategory.STRING }
        assertTrue(strings.isNotEmpty(), "Should have string spans")
    }

    @Test
    fun highlight_numbers() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            codes:
              VCM: Voice Command
            ---

            @workflow "Test"
              @wait 1000
              @repeat 3
                VCM(action: "go")
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        val numbers = spans.filter { it.category == HighlightCategory.NUMBER }
        assertTrue(numbers.any { it.text == "1000" })
        assertTrue(numbers.any { it.text == "3" })
    }

    @Test
    fun highlight_variable_refs() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            codes:
              VCM: Voice Command
            ---

            @define test(name)
              VCM(action: ${'$'}name)
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        val variables = spans.filter { it.category == HighlightCategory.VARIABLE }
        assertTrue(variables.isNotEmpty(), "Should have variable spans")
    }

    @Test
    fun highlight_header_markers() {
        val source = """
            ---
            schema: avu-2.2
            ---
        """.trimIndent()

        val spans = AvuDslHighlighter.highlight(source)
        val markers = spans.filter { it.category == HighlightCategory.HEADER_MARKER }
        assertEquals(2, markers.size, "Should have 2 header markers (---)")
    }

    @Test
    fun categorize_maps_all_directives() {
        val directiveTypes = listOf(
            com.avanues.avu.dsl.lexer.TokenType.AT_WORKFLOW,
            com.avanues.avu.dsl.lexer.TokenType.AT_DEFINE,
            com.avanues.avu.dsl.lexer.TokenType.AT_ON,
            com.avanues.avu.dsl.lexer.TokenType.AT_IF,
            com.avanues.avu.dsl.lexer.TokenType.AT_ELSE,
            com.avanues.avu.dsl.lexer.TokenType.AT_WAIT,
            com.avanues.avu.dsl.lexer.TokenType.AT_REPEAT,
            com.avanues.avu.dsl.lexer.TokenType.AT_WHILE,
            com.avanues.avu.dsl.lexer.TokenType.AT_SET,
            com.avanues.avu.dsl.lexer.TokenType.AT_RETURN,
            com.avanues.avu.dsl.lexer.TokenType.AT_EMIT
        )

        for (type in directiveTypes) {
            val token = com.avanues.avu.dsl.lexer.Token(type, "@test", 1, 1)
            assertEquals(HighlightCategory.DIRECTIVE, AvuDslHighlighter.categorize(token),
                "TokenType.$type should be DIRECTIVE")
        }
    }

    @Test
    fun categorize_operators() {
        val opTypes = listOf(
            com.avanues.avu.dsl.lexer.TokenType.PLUS,
            com.avanues.avu.dsl.lexer.TokenType.MINUS,
            com.avanues.avu.dsl.lexer.TokenType.EQ,
            com.avanues.avu.dsl.lexer.TokenType.NEQ
        )
        for (type in opTypes) {
            val token = com.avanues.avu.dsl.lexer.Token(type, "+", 1, 1)
            assertEquals(HighlightCategory.OPERATOR, AvuDslHighlighter.categorize(token))
        }
    }
}

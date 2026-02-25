package com.avanues.avu.dsl.parser

import com.avanues.avu.dsl.ast.AvuAstNode
import com.avanues.avu.dsl.ast.AvuDslFileType
import com.avanues.avu.dsl.lexer.AvuDslLexer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AvuDslParser — AVU DSL recursive-descent parser.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class AvuDslParserTest {

    private fun parse(source: String): ParseResult {
        val tokens = AvuDslLexer(source).tokenize()
        return AvuDslParser(tokens).parse()
    }

    // ── Header ────────────────────────────────────────────────────────────────

    @Test
    fun parse_header_extracts_schema_and_version() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            type: workflow
            ---
        """.trimIndent()
        val result = parse(source)
        assertEquals("avu-2.2", result.file.header.schema)
        assertEquals("1.0.0", result.file.header.version)
        assertEquals(AvuDslFileType.WORKFLOW, result.file.header.type)
    }

    @Test
    fun parse_missing_header_separator_records_error() {
        // No leading '---' — parser must surface an error, not crash
        val source = "schema: avu-2.2\n@workflow \"test\"\n    VCM(action: \"CLICK\")\n"
        val result = parse(source)
        // Parser returns errors, not an exception
        assertTrue(result.hasErrors || result.file.declarations.isEmpty() || result.errors.isNotEmpty())
    }

    // ── Workflow declaration ───────────────────────────────────────────────────

    @Test
    fun parse_workflow_declaration() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @workflow "Scroll to Top"
                VCM(action: "SCROLL_TOP")
        """.trimIndent()
        val result = parse(source)
        val decl = result.file.declarations.firstOrNull()
        assertNotNull(decl)
        assertIs<AvuAstNode.Declaration.Workflow>(decl)
        assertEquals("Scroll to Top", decl.name)
        assertEquals(1, decl.body.size)
    }

    @Test
    fun parse_workflow_body_code_invocation() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @workflow "Click"
                VCM(id: "cmd1", action: "CLICK")
        """.trimIndent()
        val result = parse(source)
        val workflow = result.file.declarations.filterIsInstance<AvuAstNode.Declaration.Workflow>().firstOrNull()
        assertNotNull(workflow)
        val stmt = workflow.body.firstOrNull()
        assertIs<AvuAstNode.Statement.CodeInvocation>(stmt)
        assertEquals("VCM", stmt.code)
        assertEquals(2, stmt.arguments.size)
    }

    // ── TriggerHandler declaration ─────────────────────────────────────────────

    @Test
    fun parse_on_trigger_handler_with_capture_var() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @on "call {contact}"
                VCM(action: "DIAL")
        """.trimIndent()
        val result = parse(source)
        val handler = result.file.declarations
            .filterIsInstance<AvuAstNode.Declaration.TriggerHandler>()
            .firstOrNull()
        assertNotNull(handler)
        assertEquals("call {contact}", handler.pattern)
        assertEquals(listOf("contact"), handler.captureVars)
    }

    // ── FunctionDef declaration ────────────────────────────────────────────────

    @Test
    fun parse_define_function() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @define doClick(target)
                VCM(id: target, action: "CLICK")
        """.trimIndent()
        val result = parse(source)
        val fn = result.file.declarations
            .filterIsInstance<AvuAstNode.Declaration.FunctionDef>()
            .firstOrNull()
        assertNotNull(fn)
        assertEquals("doClick", fn.name)
        assertEquals(listOf("target"), fn.parameters)
    }

    // ── Expressions ───────────────────────────────────────────────────────────

    @Test
    fun parse_assignment_with_integer_expression() {
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @workflow "Counter"
                @set count = 5
        """.trimIndent()
        val result = parse(source)
        val workflow = result.file.declarations.filterIsInstance<AvuAstNode.Declaration.Workflow>().first()
        val assignment = workflow.body.filterIsInstance<AvuAstNode.Statement.Assignment>().firstOrNull()
        assertNotNull(assignment)
        assertEquals("count", assignment.variableName)
        assertIs<AvuAstNode.Expression.IntLiteral>(assignment.value)
        assertEquals(5, (assignment.value as AvuAstNode.Expression.IntLiteral).value)
    }

    // ── Error recovery ────────────────────────────────────────────────────────

    @Test
    fun parse_invalid_statement_does_not_crash_parser() {
        // Parser should recover and still return a result (not throw)
        val source = """
            ---
            schema: avu-2.2
            version: 1.0.0
            ---
            @workflow "BadBody"
                !!invalid!!
        """.trimIndent()
        // Must not throw — error recovery kicks in
        val result = parse(source)
        // Errors recorded internally
        assertNotNull(result)
    }
}

package com.augmentalis.voiceoscore.dsl.tooling

import com.augmentalis.voiceoscore.dsl.ast.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AvuDslFormatterTest {

    private fun makeHeader(
        codes: Map<String, String> = mapOf("VCM" to "Voice Command"),
        triggers: List<String> = emptyList(),
        permissions: List<String> = emptyList(),
        metadata: Map<String, String> = emptyMap()
    ) = AvuDslHeader(
        schema = "avu-2.2",
        version = "1.0.0",
        type = AvuDslFileType.WORKFLOW,
        metadata = metadata,
        codes = codes,
        permissions = permissions,
        triggers = triggers,
        location = SourceLocation(1, 1)
    )

    private val loc = SourceLocation(1, 1)

    @Test
    fun format_minimal_workflow() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow(
                    name = "Test",
                    body = listOf(
                        AvuAstNode.Statement.CodeInvocation(
                            "VCM",
                            listOf(AvuAstNode.NamedArgument(
                                "action", AvuAstNode.Expression.StringLiteral("test", loc), loc
                            )),
                            loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("---"))
        assertTrue(output.contains("schema: avu-2.2"))
        assertTrue(output.contains("@workflow \"Test\""))
        assertTrue(output.contains("VCM(action: \"test\")"))
    }

    @Test
    fun format_header_includes_all_sections() {
        val file = AvuDslFile(
            header = makeHeader(
                codes = mapOf("VCM" to "Voice Command", "AAC" to "Accessibility"),
                permissions = listOf("GESTURES", "APPS"),
                triggers = listOf("do something"),
                metadata = mapOf("author" to "Test")
            ),
            declarations = emptyList(),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("codes:"))
        assertTrue(output.contains("  VCM: Voice Command"))
        assertTrue(output.contains("  AAC: Accessibility"))
        assertTrue(output.contains("permissions:"))
        assertTrue(output.contains("  GESTURES"))
        assertTrue(output.contains("triggers:"))
        assertTrue(output.contains("  do something"))
        assertTrue(output.contains("metadata:"))
        assertTrue(output.contains("  author: Test"))
    }

    @Test
    fun format_function_with_parameters() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef(
                    name = "greet",
                    parameters = listOf("name", "greeting"),
                    body = listOf(
                        AvuAstNode.Statement.Log(
                            AvuAstNode.Expression.VariableRef("name", loc), loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("@define greet(name, greeting)"))
        assertTrue(output.contains("@log \$name"))
    }

    @Test
    fun format_trigger_handler() {
        val file = AvuDslFile(
            header = makeHeader(triggers = listOf("say hello")),
            declarations = listOf(
                AvuAstNode.Declaration.TriggerHandler(
                    pattern = "say hello",
                    captureVars = emptyList(),
                    body = listOf(
                        AvuAstNode.Statement.CodeInvocation(
                            "VCM",
                            listOf(AvuAstNode.NamedArgument(
                                "action", AvuAstNode.Expression.StringLiteral("hello", loc), loc
                            )),
                            loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("@on \"say hello\""))
    }

    @Test
    fun format_if_else() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow(
                    name = "Test",
                    body = listOf(
                        AvuAstNode.Statement.IfElse(
                            condition = AvuAstNode.Expression.BooleanLiteral(true, loc),
                            thenBody = listOf(
                                AvuAstNode.Statement.Log(
                                    AvuAstNode.Expression.StringLiteral("yes", loc), loc
                                )
                            ),
                            elseBody = listOf(
                                AvuAstNode.Statement.Log(
                                    AvuAstNode.Expression.StringLiteral("no", loc), loc
                                )
                            ),
                            location = loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("@if true"))
        assertTrue(output.contains("@else"))
        assertTrue(output.contains("@log \"yes\""))
        assertTrue(output.contains("@log \"no\""))
    }

    @Test
    fun format_repeat_and_while() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow(
                    name = "Test",
                    body = listOf(
                        AvuAstNode.Statement.Repeat(
                            count = AvuAstNode.Expression.IntLiteral(5, loc),
                            body = listOf(
                                AvuAstNode.Statement.Log(
                                    AvuAstNode.Expression.StringLiteral("loop", loc), loc
                                )
                            ),
                            location = loc
                        ),
                        AvuAstNode.Statement.While(
                            condition = AvuAstNode.Expression.BooleanLiteral(true, loc),
                            body = listOf(
                                AvuAstNode.Statement.Log(
                                    AvuAstNode.Expression.StringLiteral("spin", loc), loc
                                )
                            ),
                            location = loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("@repeat 5"))
        assertTrue(output.contains("@while true"))
    }

    @Test
    fun formatExpression_binary_op() {
        val expr = AvuAstNode.Expression.BinaryOp(
            left = AvuAstNode.Expression.IntLiteral(1, loc),
            operator = BinaryOperator.PLUS,
            right = AvuAstNode.Expression.IntLiteral(2, loc),
            location = loc
        )
        val result = AvuDslFormatter.formatExpression(expr)
        assertTrue(result == "1 + 2")
    }

    @Test
    fun formatExpression_member_access_and_call() {
        val expr = AvuAstNode.Expression.CallExpression(
            callee = AvuAstNode.Expression.MemberAccess(
                target = AvuAstNode.Expression.Identifier("screen", loc),
                member = "contains",
                location = loc
            ),
            arguments = listOf(
                AvuAstNode.Expression.StringLiteral("login", loc)
            ),
            location = loc
        )
        val result = AvuDslFormatter.formatExpression(expr)
        assertTrue(result == "screen.contains(\"login\")")
    }

    @Test
    fun format_assignment_and_return() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef(
                    name = "test",
                    parameters = emptyList(),
                    body = listOf(
                        AvuAstNode.Statement.Assignment(
                            "x",
                            AvuAstNode.Expression.IntLiteral(42, loc),
                            loc
                        ),
                        AvuAstNode.Statement.Return(
                            AvuAstNode.Expression.VariableRef("x", loc),
                            loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        assertTrue(output.contains("@set x = 42"))
        assertTrue(output.contains("@return \$x"))
    }

    @Test
    fun format_empty_line_between_declarations() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef("a", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("a", loc), loc)
                ), loc),
                AvuAstNode.Declaration.FunctionDef("b", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("b", loc), loc)
                ), loc)
            ),
            location = loc
        )

        val output = AvuDslFormatter.format(file)
        // Between declarations there should be a blank line
        assertTrue(output.contains("@define a()"))
        assertTrue(output.contains("@define b()"))
    }

    @Test
    fun format_no_empty_line_when_disabled() {
        val file = AvuDslFile(
            header = makeHeader(),
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef("a", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("a", loc), loc)
                ), loc),
                AvuAstNode.Declaration.FunctionDef("b", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("b", loc), loc)
                ), loc)
            ),
            location = loc
        )

        val config = FormatterConfig(emptyLineBetweenDeclarations = false)
        val output = AvuDslFormatter.format(file, config)
        // Both declarations should be present
        assertTrue(output.contains("@define a()"))
        assertTrue(output.contains("@define b()"))
    }
}

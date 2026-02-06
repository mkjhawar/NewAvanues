package com.augmentalis.voiceoscore.dsl.tooling

import com.augmentalis.voiceoscore.dsl.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvuDslValidatorTest {

    private val loc = SourceLocation(1, 1)

    private fun makeFile(
        codes: Map<String, String> = mapOf("VCM" to "Voice Command"),
        triggers: List<String> = emptyList(),
        declarations: List<AvuAstNode.Declaration> = emptyList()
    ) = AvuDslFile(
        header = AvuDslHeader(
            schema = "avu-2.2",
            version = "1.0.0",
            type = AvuDslFileType.WORKFLOW,
            metadata = emptyMap(),
            codes = codes,
            permissions = emptyList(),
            triggers = triggers,
            location = loc
        ),
        declarations = declarations,
        location = loc
    )

    @Test
    fun valid_file_passes() {
        val file = makeFile(
            codes = mapOf("VCM" to "Voice Command"),
            triggers = listOf("test"),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow(
                    "Test",
                    listOf(AvuAstNode.Statement.CodeInvocation(
                        "VCM",
                        listOf(AvuAstNode.NamedArgument(
                            "action", AvuAstNode.Expression.StringLiteral("go", loc), loc
                        )),
                        loc
                    )),
                    loc
                ),
                AvuAstNode.Declaration.TriggerHandler(
                    "test", emptyList(),
                    listOf(AvuAstNode.Statement.Log(
                        AvuAstNode.Expression.StringLiteral("triggered", loc), loc
                    )),
                    loc
                )
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.isValid, "Report: ${report.summary()}")
        assertEquals(0, report.errors.size)
    }

    @Test
    fun E001_undeclared_code() {
        val file = makeFile(
            codes = mapOf("VCM" to "Voice Command"),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("AAC", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.code == "E001" })
        assertTrue(report.errors.any { it.message.contains("AAC") })
    }

    @Test
    fun E002_undefined_function() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.FunctionCall("nonexistent", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.code == "E002" })
    }

    @Test
    fun E003_empty_workflow_body() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Empty", emptyList(), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertFalse(report.isValid)
        assertTrue(report.errors.any { it.code == "E003" })
    }

    @Test
    fun W001_unused_declared_code() {
        val file = makeFile(
            codes = mapOf("VCM" to "Voice Command", "AAC" to "Accessibility"),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.warnings.any { it.code == "W001" && it.message.contains("AAC") })
    }

    @Test
    fun W002_duplicate_trigger_pattern() {
        val file = makeFile(
            triggers = listOf("hello"),
            declarations = listOf(
                AvuAstNode.Declaration.TriggerHandler("hello", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("a", loc), loc)
                ), loc),
                AvuAstNode.Declaration.TriggerHandler("hello", emptyList(), listOf(
                    AvuAstNode.Statement.Log(AvuAstNode.Expression.StringLiteral("b", loc), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.warnings.any { it.code == "W002" })
    }

    @Test
    fun W003_unused_variable() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.Assignment(
                        "unused_var",
                        AvuAstNode.Expression.IntLiteral(42, loc),
                        loc
                    ),
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.warnings.any { it.code == "W003" && it.message.contains("unused_var") })
    }

    @Test
    fun W004_unreachable_code_after_return() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef("test", emptyList(), listOf(
                    AvuAstNode.Statement.Return(
                        AvuAstNode.Expression.IntLiteral(1, loc), loc
                    ),
                    AvuAstNode.Statement.Log(
                        AvuAstNode.Expression.StringLiteral("unreachable", loc), loc
                    )
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.warnings.any { it.code == "W004" })
    }

    @Test
    fun I001_no_triggers() {
        val file = makeFile(
            triggers = emptyList(),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.infos.any { it.code == "I001" })
    }

    @Test
    fun I002_unused_function() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef("helper", emptyList(), listOf(
                    AvuAstNode.Statement.Log(
                        AvuAstNode.Expression.StringLiteral("x", loc), loc
                    )
                ), loc),
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.infos.any { it.code == "I002" && it.message.contains("helper") })
    }

    @Test
    fun defined_and_called_function_no_error() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.FunctionDef("helper", emptyList(), listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc),
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.FunctionCall("helper", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.errors.none { it.code == "E002" })
        assertTrue(report.infos.none { it.code == "I002" })
    }

    @Test
    fun variable_set_and_read_no_warning() {
        val file = makeFile(
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.Assignment(
                        "x", AvuAstNode.Expression.IntLiteral(1, loc), loc
                    ),
                    AvuAstNode.Statement.Log(
                        AvuAstNode.Expression.VariableRef("x", loc), loc
                    )
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.warnings.none { it.code == "W003" })
    }

    @Test
    fun summary_is_readable() {
        val file = makeFile(
            codes = mapOf("VCM" to "Voice"),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("AAC", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        val summary = report.summary()
        assertTrue(summary.contains("FAIL"))
        assertTrue(summary.contains("error"))
    }

    @Test
    fun no_declared_codes_skips_E001_check() {
        // When header declares no codes, skip undeclared code check
        val file = makeFile(
            codes = emptyMap(),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc)
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        assertTrue(report.errors.none { it.code == "E001" })
    }

    @Test
    fun collects_codes_from_nested_structures() {
        val file = makeFile(
            codes = mapOf("VCM" to "Voice", "AAC" to "Access", "CHT" to "Chat"),
            declarations = listOf(
                AvuAstNode.Declaration.Workflow("Test", listOf(
                    AvuAstNode.Statement.CodeInvocation("VCM", emptyList(), loc),
                    AvuAstNode.Statement.IfElse(
                        condition = AvuAstNode.Expression.BooleanLiteral(true, loc),
                        thenBody = listOf(
                            AvuAstNode.Statement.CodeInvocation("AAC", emptyList(), loc)
                        ),
                        elseBody = listOf(
                            AvuAstNode.Statement.CodeInvocation("CHT", emptyList(), loc)
                        ),
                        location = loc
                    )
                ), loc)
            )
        )

        val report = AvuDslValidator.validate(file)
        // All 3 codes are used, none should be W001
        assertTrue(report.warnings.none { it.code == "W001" })
    }
}

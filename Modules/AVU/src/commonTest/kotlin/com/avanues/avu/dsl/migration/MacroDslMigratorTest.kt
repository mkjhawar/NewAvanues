package com.avanues.avu.dsl.migration

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MacroDslMigratorTest {

    @Test
    fun migrate_simple_action_macro() {
        val macro = MigrationMacro(
            name = "Open App",
            trigger = "open app",
            steps = listOf(
                MigrationStep.Action("VCM", mapOf("action" to "open_app", "package" to "com.example"))
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("schema: avu-2.2"))
        assertTrue(content.contains("type: workflow"))
        assertTrue(content.contains("VCM"))
        assertTrue(content.contains("@workflow"))
    }

    @Test
    fun migrate_macro_with_delay() {
        val macro = MigrationMacro(
            name = "Wait Test",
            steps = listOf(
                MigrationStep.Action("VCM", mapOf("action" to "test")),
                MigrationStep.Delay(1000),
                MigrationStep.Action("CHT", mapOf("text" to "Done"))
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@wait 1000"))
    }

    @Test
    fun migrate_macro_with_conditional() {
        val macro = MigrationMacro(
            name = "Conditional Test",
            steps = listOf(
                MigrationStep.Conditional(
                    condition = "screen.contains(\"login\")",
                    thenSteps = listOf(
                        MigrationStep.Action("AAC", mapOf("action" to "CLICK", "target" to "login_btn"))
                    ),
                    elseSteps = listOf(
                        MigrationStep.Action("CHT", mapOf("text" to "Already logged in"))
                    )
                )
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@if"))
        assertTrue(content.contains("@else"))
    }

    @Test
    fun migrate_macro_with_loop() {
        val macro = MigrationMacro(
            name = "Loop Test",
            steps = listOf(
                MigrationStep.Loop(3, listOf(
                    MigrationStep.Action("VCM", mapOf("action" to "scroll"))
                ))
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@repeat 3"))
    }

    @Test
    fun migrate_macro_with_while_loop() {
        val macro = MigrationMacro(
            name = "While Test",
            steps = listOf(
                MigrationStep.LoopWhile(
                    condition = "screen.contains(\"more\")",
                    steps = listOf(MigrationStep.Action("VCM", mapOf("action" to "scroll")))
                )
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@while"))
    }

    @Test
    fun migrate_macro_with_wait_for_condition() {
        val macro = MigrationMacro(
            name = "WaitFor Test",
            steps = listOf(
                MigrationStep.Action("VCM", mapOf("action" to "navigate")),
                MigrationStep.WaitFor("screen.contains(\"Welcome\")", 5000)
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@wait"))
        assertTrue(content.contains("timeout 5000"))
    }

    @Test
    fun migrate_macro_with_variable() {
        val macro = MigrationMacro(
            name = "Variable Test",
            steps = listOf(
                MigrationStep.Variable("username", "john"),
                MigrationStep.Action("AAC", mapOf("action" to "SET_TEXT", "text" to "john"))
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@set username"))
    }

    @Test
    fun migrate_generates_trigger_handler_when_trigger_defined() {
        val macro = MigrationMacro(
            name = "Login Flow",
            trigger = "login",
            steps = listOf(
                MigrationStep.Action("VCM", mapOf("action" to "login"))
            )
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("@on \"login\""))
        assertTrue(content.contains("triggers:"))
        assertTrue(content.contains("@define login_flow()"))
    }

    @Test
    fun migrate_warns_when_no_trigger() {
        val macro = MigrationMacro(
            name = "No Trigger",
            steps = listOf(MigrationStep.Action("VCM", mapOf("action" to "test")))
        )

        val result = MacroDslMigrator.migrate(macro) as MigrationResult.Success
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings.first().contains("No trigger"))
    }

    @Test
    fun migrate_empty_macro_fails() {
        val macro = MigrationMacro(name = "Empty", steps = emptyList())
        val result = MacroDslMigrator.migrate(macro)
        assertIs<MigrationResult.Error>(result)
    }

    @Test
    fun migrate_collects_all_codes() {
        val macro = MigrationMacro(
            name = "Multi Code",
            steps = listOf(
                MigrationStep.Action("VCM", mapOf("action" to "open")),
                MigrationStep.Conditional(
                    condition = "true",
                    thenSteps = listOf(MigrationStep.Action("AAC", mapOf("action" to "CLICK"))),
                    elseSteps = listOf(MigrationStep.Action("CHT", mapOf("text" to "fallback")))
                )
            )
        )

        val result = MacroDslMigrator.migrate(macro) as MigrationResult.Success
        assertTrue("VCM" in result.codesUsed)
        assertTrue("AAC" in result.codesUsed)
        assertTrue("CHT" in result.codesUsed)
    }

    @Test
    fun migrateMultiple_combines_macros() {
        val macros = listOf(
            MigrationMacro(
                name = "Action A",
                trigger = "do a",
                steps = listOf(MigrationStep.Action("VCM", mapOf("action" to "a")))
            ),
            MigrationMacro(
                name = "Action B",
                trigger = "do b",
                steps = listOf(MigrationStep.Action("CHT", mapOf("text" to "b")))
            )
        )

        val result = MacroDslMigrator.migrateMultiple(macros)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("action_a"))
        assertTrue(content.contains("action_b"))
        assertTrue(content.contains("VCM"))
        assertTrue(content.contains("CHT"))
    }

    @Test
    fun migrateMultiple_empty_list_fails() {
        val result = MacroDslMigrator.migrateMultiple(emptyList())
        assertIs<MigrationResult.Error>(result)
    }

    @Test
    fun migrate_includes_metadata_when_present() {
        val macro = MigrationMacro(
            name = "Full Macro",
            description = "A test macro",
            author = "Test Author",
            tags = listOf("automation", "test"),
            steps = listOf(MigrationStep.Action("VCM", mapOf("action" to "test")))
        )

        val result = MacroDslMigrator.migrate(macro)
        assertTrue(result.isSuccess)
        val content = result.contentOrNull()!!
        assertTrue(content.contains("metadata:"))
        assertTrue(content.contains("description: A test macro"))
        assertTrue(content.contains("author: Test Author"))
        assertTrue(content.contains("tags: automation, test"))
    }
}

package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for PluginValidator.
 * Tests metadata, state, reducer, UI, and tier validation.
 */
class PluginValidatorTest {

    private val validator = PluginValidator()

    // ========== Metadata Validation ==========

    @Test
    fun `validates valid metadata`() {
        val metadata = PluginMetadataJson(
            id = "com.example.plugin",
            name = "Test Plugin",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val errors = validator.validate(
            createMinimalDefinition(metadata = metadata),
            Platform.ANDROID
        )

        val criticalErrors = validator.getErrors(errors)
        assertTrue(criticalErrors.isEmpty())
    }

    @Test
    fun `rejects invalid ID format`() {
        val metadata = PluginMetadataJson(
            id = "InvalidID",
            name = "Test",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val errors = validator.validate(
            createMinimalDefinition(metadata = metadata),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field == "metadata.id" && it.severity == ErrorSeverity.ERROR })
    }

    @Test
    fun `rejects empty name`() {
        val metadata = PluginMetadataJson(
            id = "com.example.plugin",
            name = "",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val errors = validator.validate(
            createMinimalDefinition(metadata = metadata),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field == "metadata.name" && it.severity == ErrorSeverity.ERROR })
    }

    @Test
    fun `rejects invalid version format`() {
        val metadata = PluginMetadataJson(
            id = "com.example.plugin",
            name = "Test",
            version = "1.0",
            minSdkVersion = "2.0.0"
        )

        val errors = validator.validate(
            createMinimalDefinition(metadata = metadata),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field == "metadata.version" && it.severity == ErrorSeverity.ERROR })
    }

    // ========== State Validation ==========

    @Test
    fun `validates valid state variables`() {
        val state = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0)),
            "display" to StateVariable(StateType.STRING, JsonPrimitive(""))
        )

        val errors = validator.validate(
            createMinimalDefinition(state = state),
            Platform.ANDROID
        )

        val stateErrors = errors.filter { it.field.startsWith("state.") }
        assertTrue(stateErrors.isEmpty())
    }

    @Test
    fun `rejects reserved state variable names`() {
        val state = mapOf(
            "length" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        )

        val errors = validator.validate(
            createMinimalDefinition(state = state),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field == "state.length" && it.severity == ErrorSeverity.ERROR })
    }

    @Test
    fun `rejects invalid variable names`() {
        val state = mapOf(
            "123invalid" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        )

        val errors = validator.validate(
            createMinimalDefinition(state = state),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field.startsWith("state.") && it.severity == ErrorSeverity.ERROR })
    }

    // ========== Reducer Validation ==========

    @Test
    fun `validates valid reducers`() {
        val state = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        )
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(state = state, reducers = reducers),
            Platform.ANDROID
        )

        val reducerErrors = errors.filter { it.field.startsWith("reducers.") && it.severity == ErrorSeverity.ERROR }
        assertTrue(reducerErrors.isEmpty())
    }

    @Test
    fun `rejects invalid reducer names`() {
        val reducers = mapOf(
            "123invalid" to Reducer(emptyList(), mapOf("count" to "1"))
        )

        val errors = validator.validate(
            createMinimalDefinition(reducers = reducers),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field.startsWith("reducers.") && it.severity == ErrorSeverity.ERROR })
    }

    @Test
    fun `rejects invalid parameter names`() {
        val reducers = mapOf(
            "update" to Reducer(
                params = listOf("123invalid"),
                next_state = mapOf("count" to "1")
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(reducers = reducers),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field.contains("params") && it.severity == ErrorSeverity.ERROR })
    }

    @Test
    fun `warns on tier 2 reducer features`() {
        val reducers = mapOf(
            "update" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "1"),
                effects = listOf(Effect("haptic", emptyMap()))
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(reducers = reducers, tier = PluginTier.DATA),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field.contains("effects") && it.severity == ErrorSeverity.WARNING })
    }

    // ========== UI Validation ==========

    @Test
    fun `validates valid UI tree`() {
        val state = mapOf(
            "display" to StateVariable(StateType.STRING, JsonPrimitive(""))
        )
        val reducers = mapOf(
            "clear" to Reducer(emptyList(), mapOf("display" to "\"\""))
        )
        val ui = UINode(
            type = "Column",
            children = listOf(
                UINode(
                    type = "Text",
                    bindings = mapOf("value" to "\$state.display")
                ),
                UINode(
                    type = "Button",
                    events = mapOf("onTap" to "clear")
                )
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(state = state, reducers = reducers, ui = ui),
            Platform.ANDROID
        )

        val uiErrors = errors.filter { it.field.startsWith("ui.") && it.severity == ErrorSeverity.ERROR }
        assertTrue(uiErrors.isEmpty())
    }

    @Test
    fun `rejects empty component type`() {
        val ui = UINode(type = "")

        val errors = validator.validate(
            createMinimalDefinition(ui = ui),
            Platform.ANDROID
        )

        assertTrue(errors.any { it.field == "ui" && it.severity == ErrorSeverity.ERROR })
    }

    // ========== Tier Validation ==========

    @Test
    fun `warns on tier downgrade for iOS`() {
        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC),
            Platform.IOS
        )

        assertTrue(errors.any {
            it.field == "tier" && it.severity == ErrorSeverity.WARNING
        })
    }

    @Test
    fun `does not warn on tier downgrade for Android`() {
        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC),
            Platform.ANDROID
        )

        assertFalse(errors.any {
            it.field == "tier" && it.severity == ErrorSeverity.WARNING && it.message.contains("downgraded")
        })
    }

    @Test
    fun `warns about scripts on iOS`() {
        val scripts = mapOf(
            "myScript" to Script(
                params = emptyList(),
                body = "console.log('test')"
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC, scripts = scripts),
            Platform.IOS
        )

        assertTrue(errors.any {
            it.field == "scripts" && it.severity == ErrorSeverity.WARNING
        })
    }

    // ========== Script Validation ==========

    @Test
    fun `rejects scripts in tier 1`() {
        val scripts = mapOf(
            "myScript" to Script(
                params = emptyList(),
                body = "console.log('test')"
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.DATA, scripts = scripts),
            Platform.ANDROID
        )

        assertTrue(errors.any {
            it.field == "scripts" && it.severity == ErrorSeverity.ERROR
        })
    }

    @Test
    fun `validates valid script`() {
        val scripts = mapOf(
            "myScript" to Script(
                params = listOf("param1"),
                body = "return param1 + 1;"
            )
        )

        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC, scripts = scripts),
            Platform.ANDROID
        )

        val scriptErrors = errors.filter {
            it.field.startsWith("scripts.") && it.severity == ErrorSeverity.ERROR
        }
        assertTrue(scriptErrors.isEmpty())
    }

    @Test
    fun `rejects invalid script names`() {
        val scripts = mapOf(
            "123invalid" to Script(emptyList(), "body")
        )

        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC, scripts = scripts),
            Platform.ANDROID
        )

        assertTrue(errors.any {
            it.field.startsWith("scripts.") && it.severity == ErrorSeverity.ERROR
        })
    }

    @Test
    fun `rejects empty script body`() {
        val scripts = mapOf(
            "myScript" to Script(emptyList(), "")
        )

        val errors = validator.validate(
            createMinimalDefinition(tier = PluginTier.LOGIC, scripts = scripts),
            Platform.ANDROID
        )

        assertTrue(errors.any {
            it.field.contains("body") && it.severity == ErrorSeverity.ERROR
        })
    }

    // ========== Validation Result Methods ==========

    @Test
    fun `isValid returns true when no errors`() {
        val errors = listOf(
            ValidationError("field", "message", ErrorSeverity.WARNING)
        )

        assertTrue(validator.isValid(errors))
    }

    @Test
    fun `isValid returns false when errors present`() {
        val errors = listOf(
            ValidationError("field", "message", ErrorSeverity.ERROR)
        )

        assertFalse(validator.isValid(errors))
    }

    @Test
    fun `getErrors filters error level`() {
        val errors = listOf(
            ValidationError("field1", "error", ErrorSeverity.ERROR),
            ValidationError("field2", "warning", ErrorSeverity.WARNING)
        )

        val filtered = validator.getErrors(errors)

        assertEquals(1, filtered.size)
        assertEquals(ErrorSeverity.ERROR, filtered[0].severity)
    }

    @Test
    fun `getWarnings filters warning level`() {
        val errors = listOf(
            ValidationError("field1", "error", ErrorSeverity.ERROR),
            ValidationError("field2", "warning", ErrorSeverity.WARNING)
        )

        val filtered = validator.getWarnings(errors)

        assertEquals(1, filtered.size)
        assertEquals(ErrorSeverity.WARNING, filtered[0].severity)
    }

    // ========== Helper Methods ==========

    private fun createMinimalDefinition(
        metadata: PluginMetadataJson = PluginMetadataJson(
            id = "com.example.test",
            name = "Test",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        ),
        tier: PluginTier = PluginTier.DATA,
        state: Map<String, StateVariable> = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        ),
        reducers: Map<String, Reducer> = emptyMap(),
        scripts: Map<String, Script>? = null,
        ui: UINode = UINode(type = "Text")
    ): PluginDefinition {
        return PluginDefinition(
            metadata = metadata,
            tier = tier,
            state = state,
            reducers = reducers,
            scripts = scripts,
            ui = ui
        )
    }
}

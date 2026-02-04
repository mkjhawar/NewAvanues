package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Integration tests for PluginRuntime.
 * Tests full plugin lifecycle: loading, dispatch flow, render cycle, and tier detection.
 */
class PluginRuntimeIntegrationTest {

    // ========== Basic Plugin Lifecycle ==========

    @Test
    fun `loads and initializes simple plugin`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertNotNull(runtime)
        assertEquals(PluginTier.DATA, runtime.effectiveTier)
        assertEquals(0, runtime.getStats().dispatchCount)
    }

    @Test
    fun `initializes state from schema`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        val state = runtime.getState()
        assertEquals(JsonPrimitive("0"), state["display"])
    }

    // ========== Dispatch Flow ==========

    @Test
    fun `dispatches action and updates state`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "7"))

        val state = runtime.getState()
        assertEquals(JsonPrimitive("07"), state["display"])
        assertEquals(1, runtime.getStats().dispatchCount)
    }

    @Test
    fun `dispatches multiple actions sequentially`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "1"))
        runtime.dispatch("appendDigit", mapOf("digit" to "2"))
        runtime.dispatch("appendDigit", mapOf("digit" to "3"))

        val state = runtime.getState()
        assertEquals(JsonPrimitive("0123"), state["display"])
        assertEquals(3, runtime.getStats().dispatchCount)
    }

    @Test
    fun `dispatch without parameters works`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "5"))
        runtime.dispatch("clear")

        val state = runtime.getState()
        assertEquals(JsonPrimitive("0"), state["display"])
    }

    @Test
    fun `throws on missing reducer`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertFailsWith<PluginRuntimeException> {
            runtime.dispatch("nonexistent")
        }
    }

    @Test
    fun `throws on missing parameter`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertFailsWith<PluginRuntimeException> {
            runtime.dispatch("appendDigit")
        }
    }

    // ========== State Management ==========

    @Test
    fun `getState returns immutable snapshot`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        val snapshot1 = runtime.getState()
        runtime.dispatch("appendDigit", mapOf("digit" to "5"))
        val snapshot2 = runtime.getState()

        assertNotEquals(snapshot1, snapshot2)
        assertEquals(JsonPrimitive("0"), snapshot1["display"])
        assertEquals(JsonPrimitive("05"), snapshot2["display"])
    }

    @Test
    fun `updateState directly modifies state`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.updateState(mapOf("display" to JsonPrimitive("999")))

        val state = runtime.getState()
        assertEquals(JsonPrimitive("999"), state["display"])
    }

    @Test
    fun `resetState restores defaults`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "5"))
        runtime.resetState()

        val state = runtime.getState()
        assertEquals(JsonPrimitive("0"), state["display"])
    }

    @Test
    fun `resetState with paths resets specific variables`() {
        val definition = createCounterDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("increment")
        runtime.dispatch("increment")
        runtime.resetState(listOf("count"))

        val state = runtime.getState()
        assertEquals(JsonPrimitive(0.0), state["count"])
    }

    // ========== Undo/Redo ==========

    @Test
    fun `undo reverts last change`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "1"))
        runtime.dispatch("appendDigit", mapOf("digit" to "2"))

        assertTrue(runtime.undo())

        val state = runtime.getState()
        assertEquals(JsonPrimitive("01"), state["display"])
    }

    @Test
    fun `canUndo returns correct status`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertFalse(runtime.canUndo())

        runtime.dispatch("appendDigit", mapOf("digit" to "1"))

        assertTrue(runtime.canUndo())
    }

    @Test
    fun `undo returns false when no history`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertFalse(runtime.undo())
    }

    // ========== Tier Detection and Enforcement ==========

    @Test
    fun `detects DATA tier on iOS`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.IOS)

        assertEquals(PluginTier.DATA, runtime.effectiveTier)
    }

    @Test
    fun `detects LOGIC tier on Android`() {
        val definition = createLogicTierDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        assertEquals(PluginTier.LOGIC, runtime.effectiveTier)
    }

    @Test
    fun `downgrades LOGIC to DATA on iOS`() {
        val definition = createLogicTierDefinition()
        val runtime = PluginRuntime(definition, Platform.IOS)

        assertEquals(PluginTier.DATA, runtime.effectiveTier)

        val tierInfo = runtime.getTierInfo()
        assertTrue(tierInfo.downgraded)
        assertEquals(PluginTier.LOGIC, tierInfo.requested)
    }

    // ========== Effects (Tier 2) ==========

    @Test
    fun `executes effects on Android`() {
        val definition = createLogicTierDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        // Should not throw
        runtime.dispatch("incrementWithEffect")

        assertEquals(1, runtime.getStats().dispatchCount)
    }

    @Test
    fun `ignores effects on iOS`() {
        val definition = createLogicTierDefinition()
        val runtime = PluginRuntime(definition, Platform.IOS)

        // Should not throw, effects ignored
        runtime.dispatch("incrementWithEffect")

        assertEquals(1, runtime.getStats().dispatchCount)
    }

    // ========== Metadata and Statistics ==========

    @Test
    fun `getMetadata returns plugin metadata`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        val metadata = runtime.getMetadata()

        assertEquals("com.example.calculator", metadata.id)
        assertEquals("Simple Calculator", metadata.name)
    }

    @Test
    fun `getTierInfo returns tier information`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        val tierInfo = runtime.getTierInfo()

        assertEquals(PluginTier.DATA, tierInfo.requested)
        assertEquals(PluginTier.DATA, tierInfo.effective)
        assertEquals(Platform.ANDROID, tierInfo.platform)
        assertFalse(tierInfo.downgraded)
    }

    @Test
    fun `getStats returns runtime statistics`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.dispatch("appendDigit", mapOf("digit" to "5"))

        val stats = runtime.getStats()

        assertEquals(1, stats.dispatchCount)
        assertTrue(stats.stateSize > 0)
        assertTrue(stats.reducerCount > 0)
        assertFalse(stats.isDestroyed)
    }

    // ========== Lifecycle Management ==========

    @Test
    fun `destroy marks runtime as destroyed`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.destroy()

        assertTrue(runtime.getStats().isDestroyed)
    }

    @Test
    fun `throws on dispatch after destroy`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.destroy()

        assertFailsWith<PluginRuntimeException> {
            runtime.dispatch("clear")
        }
    }

    @Test
    fun `throws on render after destroy`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        runtime.destroy()

        assertFailsWith<PluginRuntimeException> {
            runtime.render()
        }
    }

    // ========== Complex Scenarios ==========

    @Test
    fun `calculator workflow integration`() {
        val definition = createSimpleCalculatorDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        // Simulate user typing "123"
        runtime.dispatch("appendDigit", mapOf("digit" to "1"))
        runtime.dispatch("appendDigit", mapOf("digit" to "2"))
        runtime.dispatch("appendDigit", mapOf("digit" to "3"))

        var state = runtime.getState()
        assertEquals(JsonPrimitive("0123"), state["display"])

        // Clear
        runtime.dispatch("clear")

        state = runtime.getState()
        assertEquals(JsonPrimitive("0"), state["display"])

        // Type "7"
        runtime.dispatch("appendDigit", mapOf("digit" to "7"))

        state = runtime.getState()
        assertEquals(JsonPrimitive("07"), state["display"])

        assertEquals(5, runtime.getStats().dispatchCount)
    }

    @Test
    fun `counter workflow integration`() {
        val definition = createCounterDefinition()
        val runtime = PluginRuntime(definition, Platform.ANDROID)

        // Increment 3 times
        runtime.dispatch("increment")
        runtime.dispatch("increment")
        runtime.dispatch("increment")

        var state = runtime.getState()
        assertEquals(JsonPrimitive(3.0), state["count"])

        // Decrement once
        runtime.dispatch("decrement")

        state = runtime.getState()
        assertEquals(JsonPrimitive(2.0), state["count"])

        // Reset
        runtime.dispatch("reset")

        state = runtime.getState()
        assertEquals(JsonPrimitive(0.0), state["count"])
    }

    // ========== Helper Methods ==========

    private fun createSimpleCalculatorDefinition(): PluginDefinition {
        val metadata = PluginMetadataJson(
            id = "com.example.calculator",
            name = "Simple Calculator",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val state = mapOf(
            "display" to StateVariable(StateType.STRING, JsonPrimitive("0"))
        )

        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf("display" to "\$string.concat(\$state.display, \$digit)")
            ),
            "clear" to Reducer(
                params = emptyList(),
                next_state = mapOf("display" to "\"0\"")
            )
        )

        val ui = UINode(
            type = "Column",
            children = listOf(
                UINode(
                    type = "Text",
                    bindings = mapOf("value" to "\$state.display")
                )
            )
        )

        return PluginDefinition(
            metadata = metadata,
            tier = PluginTier.DATA,
            state = state,
            reducers = reducers,
            scripts = null,
            ui = ui
        )
    }

    private fun createCounterDefinition(): PluginDefinition {
        val metadata = PluginMetadataJson(
            id = "com.example.counter",
            name = "Counter",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val state = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        )

        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            ),
            "decrement" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.subtract(\$state.count, 1)")
            ),
            "reset" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "0")
            )
        )

        val ui = UINode(type = "Text")

        return PluginDefinition(
            metadata = metadata,
            tier = PluginTier.DATA,
            state = state,
            reducers = reducers,
            scripts = null,
            ui = ui
        )
    }

    private fun createLogicTierDefinition(): PluginDefinition {
        val metadata = PluginMetadataJson(
            id = "com.example.logic",
            name = "Logic Plugin",
            version = "1.0.0",
            minSdkVersion = "2.0.0"
        )

        val state = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0))
        )

        val effect = Effect("haptic", mapOf("intensity" to JsonPrimitive("medium")))

        val reducers = mapOf(
            "incrementWithEffect" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)"),
                effects = listOf(effect)
            )
        )

        val ui = UINode(type = "Text")

        return PluginDefinition(
            metadata = metadata,
            tier = PluginTier.LOGIC,
            state = state,
            reducers = reducers,
            scripts = null,
            ui = ui
        )
    }
}

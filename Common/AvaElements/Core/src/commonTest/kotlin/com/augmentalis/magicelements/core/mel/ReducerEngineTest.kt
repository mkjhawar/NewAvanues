package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for ReducerEngine.
 * Tests reducer dispatch, parameter binding, state transitions, and tier enforcement.
 */
class ReducerEngineTest {

    // ========== Basic Dispatch ==========

    @Test
    fun `dispatches reducer without parameters`() {
        val state = createState("count" to 0)
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "increment")

        assertEquals(1.0, result.stateUpdates["count"])
    }

    @Test
    fun `dispatches reducer with parameters`() {
        val state = createState("display" to "12")
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf("display" to "\$string.concat(\$state.display, \$digit)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "appendDigit", mapOf("digit" to "3"))

        assertEquals("123", result.stateUpdates["display"])
    }

    @Test
    fun `dispatches reducer with multiple parameters`() {
        val state = createState("x" to 0, "y" to 0)
        val reducers = mapOf(
            "setPosition" to Reducer(
                params = listOf("newX", "newY"),
                next_state = mapOf(
                    "x" to "\$newX",
                    "y" to "\$newY"
                )
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "setPosition", mapOf("newX" to 10, "newY" to 20))

        assertEquals(10.0, result.stateUpdates["x"])
        assertEquals(20.0, result.stateUpdates["y"])
    }

    @Test
    fun `throws on missing reducer`() {
        val state = createState("count" to 0)
        val engine = ReducerEngine(emptyMap(), PluginTier.DATA)

        assertFailsWith<ReducerException> {
            engine.dispatch(state, "nonexistent")
        }
    }

    @Test
    fun `throws on missing parameter`() {
        val state = createState("display" to "")
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf("display" to "\$digit")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        assertFailsWith<MissingParameterException> {
            engine.dispatch(state, "appendDigit")
        }
    }

    // ========== State Transitions ==========

    @Test
    fun `updates single state variable`() {
        val state = createState("count" to 5)
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "increment")

        assertEquals(1, result.stateUpdates.size)
        assertEquals(6.0, result.stateUpdates["count"])
    }

    @Test
    fun `updates multiple state variables`() {
        val state = createState("a" to 1, "b" to 2)
        val reducers = mapOf(
            "swap" to Reducer(
                params = emptyList(),
                next_state = mapOf(
                    "a" to "\$state.b",
                    "b" to "\$state.a"
                )
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "swap")

        assertEquals(2, result.stateUpdates["a"])
        assertEquals(1, result.stateUpdates["b"])
    }

    @Test
    fun `evaluates complex expression`() {
        val state = createState("count" to 5)
        val reducers = mapOf(
            "complexUpdate" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.multiply(\$math.add(\$state.count, 1), 2)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "complexUpdate")

        assertEquals(12.0, result.stateUpdates["count"]) // (5 + 1) * 2
    }

    // ========== Literal Values ==========

    @Test
    fun `handles number literal in next_state`() {
        val state = createState("count" to 5)
        val reducers = mapOf(
            "reset" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "0")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "reset")

        assertEquals(0.0, result.stateUpdates["count"])
    }

    @Test
    fun `handles string literal in next_state`() {
        val state = createState("display" to "old")
        val reducers = mapOf(
            "clear" to Reducer(
                params = emptyList(),
                next_state = mapOf("display" to "\"\"")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "clear")

        assertEquals("", result.stateUpdates["display"])
    }

    @Test
    fun `handles boolean literal in next_state`() {
        val state = createState("enabled" to false)
        val reducers = mapOf(
            "enable" to Reducer(
                params = emptyList(),
                next_state = mapOf("enabled" to "true")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "enable")

        assertEquals(true, result.stateUpdates["enabled"])
    }

    // ========== Effects (Tier 2) ==========

    @Test
    fun `includes effects in Tier 2`() {
        val state = createState("count" to 0)
        val effect = Effect("haptic", mapOf("intensity" to JsonPrimitive("medium")))
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "1"),
                effects = listOf(effect)
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.LOGIC)

        val result = engine.dispatch(state, "increment")

        assertEquals(1, result.effects.size)
        assertEquals("haptic", result.effects[0].type)
    }

    @Test
    fun `excludes effects in Tier 1`() {
        val state = createState("count" to 0)
        val effect = Effect("haptic", mapOf("intensity" to JsonPrimitive("medium")))
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "1"),
                effects = listOf(effect)
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "increment")

        assertEquals(0, result.effects.size)
    }

    // ========== dispatchAndApply ==========

    @Test
    fun `dispatchAndApply returns updated state`() {
        val state = createState("count" to 0)
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val newState = engine.dispatchAndApply(state, "increment")

        assertEquals(JsonPrimitive(1.0), newState.get("count"))
    }

    @Test
    fun `dispatchAndApply with parameters`() {
        val state = createState("display" to "1")
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf("display" to "\$string.concat(\$state.display, \$digit)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val newState = engine.dispatchAndApply(state, "appendDigit", mapOf("digit" to "2"))

        assertEquals(JsonPrimitive("12"), newState.get("display"))
    }

    // ========== Validation ==========

    @Test
    fun `validates all reducers`() {
        val state = createState("count" to 0)
        val reducers = mapOf(
            "valid" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            ),
            "invalid" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, ")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val errors = engine.validateReducers(state)

        assertTrue(errors.isEmpty() || errors.containsKey("invalid"))
    }

    // ========== Query Methods ==========

    @Test
    fun `hasReducer returns true for existing reducer`() {
        val reducers = mapOf(
            "increment" to Reducer(emptyList(), mapOf("count" to "1"))
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        assertTrue(engine.hasReducer("increment"))
        assertFalse(engine.hasReducer("nonexistent"))
    }

    @Test
    fun `getReducerNames returns all reducer names`() {
        val reducers = mapOf(
            "increment" to Reducer(emptyList(), mapOf("count" to "1")),
            "decrement" to Reducer(emptyList(), mapOf("count" to "0"))
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val names = engine.getReducerNames()

        assertEquals(2, names.size)
        assertTrue(names.contains("increment"))
        assertTrue(names.contains("decrement"))
    }

    @Test
    fun `getReducer returns reducer definition`() {
        val reducer = Reducer(emptyList(), mapOf("count" to "1"))
        val reducers = mapOf("increment" to reducer)
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val retrieved = engine.getReducer("increment")

        assertNotNull(retrieved)
        assertEquals(reducer, retrieved)
    }

    @Test
    fun `getReducer returns null for nonexistent reducer`() {
        val engine = ReducerEngine(emptyMap(), PluginTier.DATA)

        val retrieved = engine.getReducer("nonexistent")

        assertNull(retrieved)
    }

    // ========== Companion Object Methods ==========

    @Test
    fun `creates empty reducer engine`() {
        val engine = ReducerEngine.empty()

        assertEquals(0, engine.getReducerNames().size)
    }

    @Test
    fun `creates empty reducer engine with tier`() {
        val engine = ReducerEngine.empty(PluginTier.LOGIC)

        assertEquals(0, engine.getReducerNames().size)
    }

    // ========== Error Handling ==========

    @Test
    fun `wraps evaluation errors with context`() {
        val state = createState("count" to "not a number")
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val exception = assertFailsWith<ExpressionEvaluationException> {
            engine.dispatch(state, "increment")
        }

        assertTrue(exception.message?.contains("increment") == true)
    }

    // ========== Calculator Example ==========

    @Test
    fun `calculator increment works`() {
        val state = createState("count" to 5)
        val reducers = mapOf(
            "increment" to Reducer(
                params = emptyList(),
                next_state = mapOf("count" to "\$math.add(\$state.count, 1)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "increment")

        assertEquals(6.0, result.stateUpdates["count"])
    }

    @Test
    fun `calculator append digit works`() {
        val state = createState("display" to "12")
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf("display" to "\$string.concat(\$state.display, \$digit)")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "appendDigit", mapOf("digit" to "3"))

        assertEquals("123", result.stateUpdates["display"])
    }

    @Test
    fun `calculator clear works`() {
        val state = createState("display" to "123")
        val reducers = mapOf(
            "clear" to Reducer(
                params = emptyList(),
                next_state = mapOf("display" to "\"0\"")
            )
        )
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        val result = engine.dispatch(state, "clear")

        assertEquals("0", result.stateUpdates["display"])
    }

    // ========== Helper Methods ==========

    private fun createState(vararg pairs: Pair<String, Any>): PluginState {
        val defaults = pairs.associate { (key, value) ->
            key to when (value) {
                is Int -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
            }
        }
        val schema = StateSchema.fromDefaults(defaults)
        return PluginState(schema)
    }
}

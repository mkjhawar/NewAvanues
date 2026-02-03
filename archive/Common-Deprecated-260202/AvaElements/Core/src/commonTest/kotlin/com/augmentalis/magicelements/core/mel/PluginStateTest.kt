package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for PluginState.
 * Tests initialization, get/set operations, nested paths, immutability, and undo/redo.
 */
class PluginStateTest {

    // ========== Initialization ==========

    @Test
    fun `initializes from schema defaults`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "count" to JsonPrimitive(0),
            "display" to JsonPrimitive("")
        ))
        val state = PluginState(schema)

        assertEquals(JsonPrimitive(0), state.get("count"))
        assertEquals(JsonPrimitive(""), state.get("display"))
    }

    @Test
    fun `initializes with provided data`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "count" to JsonPrimitive(0)
        ))
        val data = mapOf("count" to JsonPrimitive(42))
        val state = PluginState(schema, data)

        assertEquals(JsonPrimitive(42), state.get("count"))
    }

    @Test
    fun `throws on invalid initial state`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "count" to JsonPrimitive(0)
        ))
        val invalidData = mapOf("count" to JsonPrimitive("not a number"))

        assertFailsWith<IllegalArgumentException> {
            PluginState(schema, invalidData)
        }
    }

    // ========== Get Operations ==========

    @Test
    fun `gets top-level value`() {
        val state = createSimpleState("count" to 42)
        assertEquals(JsonPrimitive(42), state.get("count"))
    }

    @Test
    fun `gets nested value`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "user" to JsonObject(mapOf(
                "name" to JsonPrimitive("John")
            ))
        ))
        val state = PluginState(schema)

        val name = state.get("user.name")
        assertEquals(JsonPrimitive("John"), name)
    }

    @Test
    fun `gets deep nested value`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "config" to JsonObject(mapOf(
                "theme" to JsonObject(mapOf(
                    "primaryColor" to JsonPrimitive("#FF0000")
                ))
            ))
        ))
        val state = PluginState(schema)

        val color = state.get("config.theme.primaryColor")
        assertEquals(JsonPrimitive("#FF0000"), color)
    }

    @Test
    fun `gets array element by index`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "items" to JsonArray(listOf(
                JsonPrimitive("a"),
                JsonPrimitive("b"),
                JsonPrimitive("c")
            ))
        ))
        val state = PluginState(schema)

        assertEquals(JsonPrimitive("b"), state.get("items.1"))
    }

    @Test
    fun `returns JsonNull for missing path`() {
        val state = createSimpleState("count" to 42)
        assertEquals(JsonNull, state.get("missing"))
    }

    @Test
    fun `returns JsonNull for invalid nested path`() {
        val state = createSimpleState("count" to 42)
        assertEquals(JsonNull, state.get("count.invalid"))
    }

    @Test
    fun `gets multiple values at once`() {
        val state = createSimpleState(
            "count" to 42,
            "display" to "12"
        )

        val values = state.getAll(listOf("count", "display"))
        assertEquals(2, values.size)
        assertEquals(JsonPrimitive(42), values["count"])
        assertEquals(JsonPrimitive("12"), values["display"])
    }

    // ========== Update Operations ==========

    @Test
    fun `updates single value`() {
        val state = createSimpleState("count" to 0)
        val newState = state.update("count", JsonPrimitive(42))

        assertEquals(JsonPrimitive(0), state.get("count")) // Original unchanged
        assertEquals(JsonPrimitive(42), newState.get("count")) // New state updated
    }

    @Test
    fun `updates multiple values`() {
        val state = createSimpleState(
            "count" to 0,
            "display" to ""
        )

        val newState = state.update(mapOf(
            "count" to JsonPrimitive(42),
            "display" to JsonPrimitive("42")
        ))

        assertEquals(JsonPrimitive(42), newState.get("count"))
        assertEquals(JsonPrimitive("42"), newState.get("display"))
    }

    @Test
    fun `updates nested value`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "user" to JsonObject(mapOf(
                "name" to JsonPrimitive("John"),
                "age" to JsonPrimitive(30)
            ))
        ))
        val state = PluginState(schema)

        val newState = state.update("user.name", JsonPrimitive("Jane"))
        assertEquals(JsonPrimitive("Jane"), newState.get("user.name"))
        assertEquals(JsonPrimitive(30), newState.get("user.age"))
    }

    @Test
    fun `throws on invalid update`() {
        val state = createSimpleState("count" to 0)

        assertFailsWith<IllegalArgumentException> {
            state.update("count", JsonPrimitive("not a number"))
        }
    }

    // ========== Immutability ==========

    @Test
    fun `update returns new instance`() {
        val state = createSimpleState("count" to 0)
        val newState = state.update("count", JsonPrimitive(42))

        assertNotSame(state, newState)
    }

    @Test
    fun `original state unchanged after update`() {
        val state = createSimpleState("count" to 0)
        val newState = state.update("count", JsonPrimitive(42))

        assertEquals(JsonPrimitive(0), state.get("count"))
        assertEquals(JsonPrimitive(42), newState.get("count"))
    }

    // ========== Snapshot and Restore ==========

    @Test
    fun `creates snapshot`() {
        val state = createSimpleState("count" to 42)
        val snapshot = state.snapshot()

        assertEquals(1, snapshot.size)
        assertEquals(JsonPrimitive(42), snapshot["count"])
    }

    @Test
    fun `restores from snapshot`() {
        val state1 = createSimpleState("count" to 0)
        val state2 = state1.update("count", JsonPrimitive(42))
        val snapshot1 = state1.snapshot()

        val restored = state2.restore(snapshot1)
        assertEquals(JsonPrimitive(0), restored.get("count"))
    }

    @Test
    fun `throws on invalid snapshot`() {
        val state = createSimpleState("count" to 0)
        val invalidSnapshot = mapOf("count" to JsonPrimitive("not a number"))

        assertFailsWith<IllegalArgumentException> {
            state.restore(invalidSnapshot)
        }
    }

    // ========== Undo/Redo ==========

    @Test
    fun `undo reverts to previous state`() {
        val state1 = createSimpleState("count" to 0)
        val state2 = state1.update("count", JsonPrimitive(1))
        val state3 = state2.update("count", JsonPrimitive(2))

        val undone = state3.undo()
        assertEquals(JsonPrimitive(1), undone.get("count"))
    }

    @Test
    fun `undo returns same state when no history`() {
        val state = createSimpleState("count" to 0)
        val undone = state.undo()

        assertSame(state, undone)
    }

    @Test
    fun `canUndo returns true when history exists`() {
        val state1 = createSimpleState("count" to 0)
        val state2 = state1.update("count", JsonPrimitive(1))

        assertFalse(state1.canUndo())
        assertTrue(state2.canUndo())
    }

    @Test
    fun `undo maintains history chain`() {
        val state1 = createSimpleState("count" to 0)
        val state2 = state1.update("count", JsonPrimitive(1))
        val state3 = state2.update("count", JsonPrimitive(2))
        val state4 = state3.update("count", JsonPrimitive(3))

        val undo1 = state4.undo()
        assertEquals(JsonPrimitive(2), undo1.get("count"))

        val undo2 = undo1.undo()
        assertEquals(JsonPrimitive(1), undo2.get("count"))

        val undo3 = undo2.undo()
        assertEquals(JsonPrimitive(0), undo3.get("count"))
    }

    // ========== Reset Operations ==========

    @Test
    fun `reset restores all defaults`() {
        val state1 = createSimpleState("count" to 0, "display" to "")
        val state2 = state1.update(mapOf(
            "count" to JsonPrimitive(42),
            "display" to JsonPrimitive("42")
        ))

        val reset = state2.reset()
        assertEquals(JsonPrimitive(0), reset.get("count"))
        assertEquals(JsonPrimitive(""), reset.get("display"))
    }

    @Test
    fun `reset specific variables`() {
        val state1 = createSimpleState("count" to 0, "display" to "")
        val state2 = state1.update(mapOf(
            "count" to JsonPrimitive(42),
            "display" to JsonPrimitive("42")
        ))

        val reset = state2.reset(listOf("count"))
        assertEquals(JsonPrimitive(0), reset.get("count"))
        assertEquals(JsonPrimitive("42"), reset.get("display"))
    }

    // ========== Persistence ==========

    @Test
    fun `gets only persistent variables`() {
        val variables = mapOf(
            "count" to StateVariable(StateType.NUMBER, JsonPrimitive(0), persist = true),
            "tempData" to StateVariable(StateType.STRING, JsonPrimitive(""), persist = false)
        )
        val schema = StateSchema(variables)
        val state = PluginState(schema).update(mapOf(
            "count" to JsonPrimitive(42),
            "tempData" to JsonPrimitive("temp")
        ))

        val persistent = state.getPersistentState()
        assertEquals(1, persistent.size)
        assertTrue(persistent.containsKey("count"))
        assertFalse(persistent.containsKey("tempData"))
    }

    // ========== Merge Operations ==========

    @Test
    fun `merges two states`() {
        val schema = StateSchema.fromDefaults(mapOf(
            "a" to JsonPrimitive(1),
            "b" to JsonPrimitive(2)
        ))

        val state1 = PluginState(schema, mapOf(
            "a" to JsonPrimitive(10),
            "b" to JsonPrimitive(2)
        ))

        val state2 = PluginState(schema, mapOf(
            "a" to JsonPrimitive(1),
            "b" to JsonPrimitive(20)
        ))

        val merged = state1.merge(state2)
        assertEquals(JsonPrimitive(1), merged.get("a"))
        assertEquals(JsonPrimitive(20), merged.get("b"))
    }

    @Test
    fun `throws on merge with different schemas`() {
        val schema1 = StateSchema.fromDefaults(mapOf("a" to JsonPrimitive(1)))
        val schema2 = StateSchema.fromDefaults(mapOf("b" to JsonPrimitive(2)))

        val state1 = PluginState(schema1)
        val state2 = PluginState(schema2)

        assertFailsWith<IllegalArgumentException> {
            state1.merge(state2)
        }
    }

    // ========== Utility Methods ==========

    @Test
    fun `toMap returns all state`() {
        val state = createSimpleState("count" to 42, "display" to "12")
        val map = state.toMap()

        assertEquals(2, map.size)
        assertEquals(JsonPrimitive(42), map["count"])
        assertEquals(JsonPrimitive("12"), map["display"])
    }

    @Test
    fun `equals compares state content`() {
        val state1 = createSimpleState("count" to 42)
        val state2 = createSimpleState("count" to 42)
        val state3 = createSimpleState("count" to 0)

        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    @Test
    fun `hashCode consistent with equals`() {
        val state1 = createSimpleState("count" to 42)
        val state2 = createSimpleState("count" to 42)

        assertEquals(state1.hashCode(), state2.hashCode())
    }

    // ========== Companion Object Methods ==========

    @Test
    fun `creates state from defaults`() {
        val state = PluginState.fromDefaults(mapOf(
            "count" to JsonPrimitive(0)
        ))

        assertEquals(JsonPrimitive(0), state.get("count"))
    }

    @Test
    fun `creates empty state`() {
        val state = PluginState.empty()

        assertEquals(0, state.toMap().size)
    }

    // ========== Helper Methods ==========

    private fun createSimpleState(vararg pairs: Pair<String, Any>): PluginState {
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

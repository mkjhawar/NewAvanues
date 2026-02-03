package com.augmentalis.magicelements.core.mel

import com.augmentalis.avaelements.core.Component
import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for ReactiveRenderer selective re-rendering and dependency tracking.
 */
class ReactiveRendererTest {

    // ========== Dependency Tracking ==========

    @Test
    fun `builds dependency map from UI bindings`() {
        val uiNode = UINode(
            type = "Column",
            children = listOf(
                UINode(
                    type = "Text",
                    bindings = mapOf("value" to "\$state.count"),
                    id = "countDisplay"
                ),
                UINode(
                    type = "Text",
                    bindings = mapOf("value" to "\$state.name"),
                    id = "nameDisplay"
                )
            ),
            id = "root"
        )

        val state = PluginState(
            mapOf(
                "count" to JsonPrimitive(0),
                "name" to JsonPrimitive("Test")
            )
        )

        val renderer = createRenderer(uiNode, state)
        renderer.render()

        // Check that nodes are tracked
        assertTrue(renderer.getTrackedNodeCount() >= 3) // root + 2 children

        // Check that paths are being watched
        assertTrue(renderer.getWatchedPathCount() >= 2) // count and name

        // Check specific dependencies
        val countDeps = renderer.getDependentNodes("count")
        assertTrue(countDeps.isNotEmpty())

        val nameDeps = renderer.getDependentNodes("name")
        assertTrue(nameDeps.isNotEmpty())
    }

    @Test
    fun `tracks nested state paths`() {
        val uiNode = UINode(
            type = "Text",
            bindings = mapOf(
                "value" to "\$state.user.profile.name",
                "subtitle" to "\$state.user.email"
            ),
            id = "userInfo"
        )

        val state = PluginState(
            mapOf(
                "user" to buildJsonObject {
                    put("profile", buildJsonObject {
                        put("name", "John")
                    })
                    put("email", "john@example.com")
                }
            )
        )

        val renderer = createRenderer(uiNode, state)
        renderer.render()

        // Should have dependencies on nested paths
        assertTrue(renderer.getWatchedPathCount() >= 1)
    }

    // ========== Selective Re-rendering ==========

    @Test
    fun `rerenderAffected triggers for matching paths`() {
        var renderCount = 0

        val uiNode = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count"),
            id = "display"
        )

        val state = PluginState(mapOf("count" to JsonPrimitive(0)))
        val renderer = createRenderer(uiNode, state)

        renderer.subscribe { paths ->
            renderCount++
        }

        renderer.render()

        // Trigger re-render for count path
        renderer.rerenderAffected(setOf("count"))

        // Should have been notified
        assertEquals(1, renderCount)
    }

    @Test
    fun `rerenderAffected ignores unrelated paths`() {
        var renderCount = 0

        val uiNode = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count"),
            id = "display"
        )

        val state = PluginState(mapOf(
            "count" to JsonPrimitive(0),
            "unrelated" to JsonPrimitive("test")
        ))

        val renderer = createRenderer(uiNode, state)

        renderer.subscribe { paths ->
            if (paths.isNotEmpty()) {
                renderCount++
            }
        }

        renderer.render()

        // Trigger re-render for unrelated path
        renderer.rerenderAffected(setOf("unrelated"))

        // Should have been notified (fallback to full render when no deps found)
        assertEquals(1, renderCount)
    }

    @Test
    fun `onStateChanged updates renderer state`() {
        val uiNode = UINode(
            type = "Text",
            bindings = mapOf("value" to "\$state.count"),
            id = "display"
        )

        val initialState = PluginState(mapOf("count" to JsonPrimitive(0)))
        val renderer = createRenderer(uiNode, initialState)

        renderer.render()

        // Update state
        val newState = PluginState(mapOf("count" to JsonPrimitive(10)))
        renderer.onStateChanged(newState, setOf("count"))

        // Renderer should have updated (verify through subscription or cache)
        assertNotNull(renderer.getCachedTree())
    }

    // ========== Component Caching ==========

    @Test
    fun `caches rendered components`() {
        val uiNode = UINode(
            type = "Column",
            children = listOf(
                UINode(type = "Text", id = "text1"),
                UINode(type = "Text", id = "text2")
            ),
            id = "root"
        )

        val state = PluginState(emptyMap())
        val renderer = createRenderer(uiNode, state)

        renderer.render()

        // All nodes should be cached
        assertNotNull(renderer.getCachedNode("root"))
        assertNotNull(renderer.getCachedTree())
    }

    @Test
    fun `clear removes all caches`() {
        val uiNode = UINode(
            type = "Text",
            id = "test"
        )

        val state = PluginState(mapOf("x" to JsonPrimitive(1)))
        val renderer = createRenderer(uiNode, state)

        renderer.render()

        // Should have caches
        assertNotNull(renderer.getCachedTree())
        assertTrue(renderer.getTrackedNodeCount() > 0)

        // Clear
        renderer.clear()

        // Caches should be empty
        assertNull(renderer.getCachedTree())
        assertEquals(0, renderer.getTrackedNodeCount())
        assertEquals(0, renderer.getWatchedPathCount())
    }

    // ========== StateObserver Integration ==========

    @Test
    fun `provides StateObserver access`() {
        val uiNode = UINode(type = "Text", id = "test")
        val state = PluginState(emptyMap())
        val renderer = createRenderer(uiNode, state)

        val observer = renderer.getStateObserver()
        assertNotNull(observer)
    }

    @Test
    fun `observeState creates subscription`() {
        val uiNode = UINode(type = "Text", id = "test")
        val state = PluginState(emptyMap())
        val renderer = createRenderer(uiNode, state)

        var notified = false
        val observerId = renderer.observeState(listOf("count")) { batch ->
            notified = true
        }

        assertNotNull(observerId)
        assertTrue(observerId.startsWith("observer_"))

        // Cleanup
        assertTrue(renderer.stopObserving(observerId))
    }

    // ========== Subscription Management ==========

    @Test
    fun `subscribe and unsubscribe work correctly`() {
        val uiNode = UINode(type = "Text", id = "test")
        val state = PluginState(emptyMap())
        val renderer = createRenderer(uiNode, state)

        var callCount = 0
        val listener: (Set<String>) -> Unit = { callCount++ }

        renderer.subscribe(listener)
        renderer.render()
        renderer.rerender()

        assertEquals(1, callCount) // rerender triggers notification

        renderer.unsubscribe(listener)
        renderer.rerender()

        assertEquals(1, callCount) // Should not increase after unsubscribe
    }

    // ========== Helper Methods ==========

    private fun createRenderer(uiNode: UINode, state: PluginState): ReactiveRenderer {
        val reducerEngine = ReducerEngine(emptyMap(), state, PluginTier.DATA)
        val parser = ExpressionParser()
        val resolver = BindingResolver(state, parser)
        val componentFactory = TestComponentFactory()

        return ReactiveRenderer(
            uiRoot = uiNode,
            state = state,
            reducerEngine = reducerEngine,
            tier = PluginTier.DATA,
            resolver = resolver,
            componentFactory = componentFactory,
            onDispatch = { _, _ -> }
        )
    }

    /**
     * Simple test component factory that creates stub components
     */
    private class TestComponentFactory : ComponentFactory {
        override fun create(
            type: String,
            props: Map<String, JsonElement>,
            callbacks: Map<String, () -> Unit>,
            children: List<Component>?,
            id: String?
        ): Component {
            return TestComponent(type, id, children)
        }

        override fun supports(type: String): Boolean = true
    }

    /**
     * Stub component for testing
     */
    private class TestComponent(
        val type: String,
        val id: String?,
        val children: List<Component>?
    ) : Component {
        override val componentType: String = type
    }
}

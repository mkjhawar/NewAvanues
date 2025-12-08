package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.JsonPrimitive

/**
 * Example usage of the Phase 5 UI Binding System
 *
 * Demonstrates:
 * - UINode tree construction
 * - Binding resolution
 * - Reactive rendering
 * - Event binding
 *
 * @since 2.0.0
 */
object UIBindingExample {

    /**
     * Example 1: Simple calculator button with state binding
     */
    fun simpleButtonExample(): UINode {
        return UINode(
            type = "Button",
            props = mapOf(
                "label" to JsonPrimitive("7"),
                "style" to JsonPrimitive("primary")
            ),
            events = mapOf(
                "onTap" to "appendDigit('7')"
            ),
            id = "button-7"
        )
    }

    /**
     * Example 2: Text with state binding
     */
    fun textWithBindingExample(): UINode {
        return UINode(
            type = "Text",
            props = mapOf(
                "fontSize" to JsonPrimitive(48),
                "textAlign" to JsonPrimitive("end")
            ),
            bindings = mapOf(
                "content" to "$state.display"
            ),
            id = "display-text"
        )
    }

    /**
     * Example 3: Calculator UI tree
     */
    fun calculatorUIExample(): UINode {
        return UINode.column(
            children = listOf(
                // Display
                UINode(
                    type = "Text",
                    props = mapOf(
                        "fontSize" to JsonPrimitive(48),
                        "textAlign" to JsonPrimitive("end"),
                        "fontFamily" to JsonPrimitive("monospace")
                    ),
                    bindings = mapOf(
                        "content" to "$state.display"
                    ),
                    id = "display"
                ),

                // Divider
                UINode(
                    type = "Divider",
                    id = "divider"
                ),

                // Button row: 7 8 9 ÷
                UINode.row(
                    spacing = 8,
                    children = listOf(
                        UINode.button("7", "appendDigit('7')"),
                        UINode.button("8", "appendDigit('8')"),
                        UINode.button("9", "appendDigit('9')"),
                        UINode.button("÷", "setOperator('/')")
                    )
                ),

                // Button row: 4 5 6 ×
                UINode.row(
                    spacing = 8,
                    children = listOf(
                        UINode.button("4", "appendDigit('4')"),
                        UINode.button("5", "appendDigit('5')"),
                        UINode.button("6", "appendDigit('6')"),
                        UINode.button("×", "setOperator('*')")
                    )
                ),

                // Button row: 1 2 3 −
                UINode.row(
                    spacing = 8,
                    children = listOf(
                        UINode.button("1", "appendDigit('1')"),
                        UINode.button("2", "appendDigit('2')"),
                        UINode.button("3", "appendDigit('3')"),
                        UINode.button("−", "setOperator('-')")
                    )
                ),

                // Button row: C 0 = +
                UINode.row(
                    spacing = 8,
                    children = listOf(
                        UINode.button("C", "clear"),
                        UINode.button("0", "appendDigit('0')"),
                        UINode.button("=", "calculate"),
                        UINode.button("+", "setOperator('+')")
                    )
                )
            ),
            spacing = 16,
            id = "calculator-root"
        )
    }

    /**
     * Example 4: Complete binding resolution workflow
     */
    fun bindingResolutionWorkflow() {
        // 1. Create state schema
        val schema = StateSchema.fromDefaults(
            mapOf(
                "display" to JsonPrimitive("0"),
                "buffer" to JsonPrimitive(""),
                "operator" to JsonPrimitive("")
            )
        )

        // 2. Initialize plugin state
        val state = PluginState(schema)

        // 3. Create expression parser
        val parser = ExpressionParser()

        // 4. Create binding resolver
        val resolver = BindingResolver(state, parser)

        // 5. Create UI node
        val node = textWithBindingExample()

        // 6. Resolve bindings
        val resolvedProps = resolver.resolve(node)

        println("Resolved props: $resolvedProps")
        // Output: Resolved props: {fontSize=48, textAlign="end", content="0"}
    }

    /**
     * Example 5: Reactive rendering workflow
     */
    fun reactiveRenderingWorkflow() {
        // 1. Setup state
        val schema = StateSchema.fromDefaults(
            mapOf("display" to JsonPrimitive("0"))
        )
        val state = PluginState(schema)

        // 2. Create parser and renderer
        val parser = ExpressionParser()
        val factory = DefaultComponentFactory()
        val renderer = ReactiveRenderer.create(state, parser, factory)

        // 3. Initial render
        val uiTree = textWithBindingExample()
        val component = renderer.render(uiTree)

        println("Initial component: $component")

        // 4. Subscribe to updates
        renderer.subscribe { changedPaths ->
            println("State changed: $changedPaths")
        }

        // 5. Update state (would trigger re-render)
        val newState = state.update("display", JsonPrimitive("42"))

        // 6. Re-render with new state
        val newRenderer = ReactiveRenderer.create(newState, parser, factory)
        val newComponent = newRenderer.render(uiTree)

        println("Updated component: $newComponent")
    }

    /**
     * Example 6: Event binding workflow
     */
    fun eventBindingWorkflow() {
        // 1. Setup reducers
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf(
                    "display" to "\$string.concat(\$state.display, \$digit)"
                )
            ),
            "clear" to Reducer(
                params = emptyList(),
                next_state = mapOf(
                    "display" to "\"0\""
                )
            )
        )

        // 2. Setup state
        val schema = StateSchema.fromDefaults(
            mapOf("display" to JsonPrimitive("0"))
        )
        val state = PluginState(schema)

        // 3. Create reducer engine
        val engine = ReducerEngine(reducers, PluginTier.DATA)

        // 4. Create event binder
        val binder = EventBinder(engine)

        // 5. Create UI node with events
        val node = simpleButtonExample()

        // 6. Bind events
        val handlers = binder.bind(node)

        println("Event handlers: ${handlers.keys}")
        // Output: Event handlers: [onTap]

        // 7. Simulate button tap
        val onTap = handlers["onTap"]
        if (onTap != null) {
            println("Executing onTap handler...")
            try {
                onTap()
                println("Handler executed successfully")
            } catch (e: Exception) {
                println("Handler execution failed: ${e.message}")
            }
        }
    }

    /**
     * Example 7: Full integration - calculator
     */
    fun fullCalculatorIntegration() {
        println("=== Full Calculator Integration Example ===\n")

        // 1. Define reducers
        val reducers = mapOf(
            "appendDigit" to Reducer(
                params = listOf("digit"),
                next_state = mapOf(
                    "display" to "\$logic.if(\$logic.equals(\$state.display, \"0\"), \$digit, \$string.concat(\$state.display, \$digit))"
                )
            ),
            "setOperator" to Reducer(
                params = listOf("op"),
                next_state = mapOf(
                    "buffer" to "\$state.display",
                    "operator" to "\$op",
                    "display" to "\"0\""
                )
            ),
            "calculate" to Reducer(
                params = emptyList(),
                next_state = mapOf(
                    "display" to "\"42\"",  // Simplified - real impl would compute
                    "buffer" to "\"\"",
                    "operator" to "\"\""
                )
            ),
            "clear" to Reducer(
                params = emptyList(),
                next_state = mapOf(
                    "display" to "\"0\"",
                    "buffer" to "\"\"",
                    "operator" to "\"\""
                )
            )
        )

        // 2. Initialize state
        val schema = StateSchema.fromDefaults(
            mapOf(
                "display" to JsonPrimitive("0"),
                "buffer" to JsonPrimitive(""),
                "operator" to JsonPrimitive("")
            )
        )
        var state = PluginState(schema)

        // 3. Create UI tree
        val uiTree = calculatorUIExample()

        // 4. Create rendering components
        val parser = ExpressionParser()
        val factory = DefaultComponentFactory()
        val engine = ReducerEngine(reducers, PluginTier.DATA)
        val binder = EventBinder(engine)

        // 5. Initial render
        var renderer = ReactiveRenderer.create(state, parser, factory)
        var component = renderer.render(uiTree)

        println("Initial render complete")
        println("State: ${state.toMap()}")
        println("Component tree rendered with ${countComponents(uiTree)} nodes\n")

        // 6. Simulate user interactions
        println("Simulating button press: 7")
        val button7 = findNodeById(uiTree, "button-7")
        if (button7 != null) {
            val handlers = binder.bind(button7)
            // In real app, this would dispatch and update state
            println("Would dispatch: appendDigit('7')")
        }

        println("\n=== Integration Example Complete ===")
    }

    /**
     * Count total nodes in UI tree
     */
    private fun countComponents(node: UINode): Int {
        var count = 1
        node.children?.forEach { child ->
            count += countComponents(child)
        }
        return count
    }

    /**
     * Find node by ID in tree
     */
    private fun findNodeById(node: UINode, id: String): UINode? {
        if (node.id == id) return node
        node.children?.forEach { child ->
            val found = findNodeById(child, id)
            if (found != null) return found
        }
        return null
    }
}

/**
 * Main function to run examples
 */
fun main() {
    println("=== UI Binding System Examples ===\n")

    println("Example 1: Simple Button")
    println(UIBindingExample.simpleButtonExample())
    println()

    println("Example 2: Text with Binding")
    println(UIBindingExample.textWithBindingExample())
    println()

    println("Example 3: Calculator UI Tree")
    val calc = UIBindingExample.calculatorUIExample()
    println("Calculator has ${calc.children?.size} rows")
    println()

    println("Example 4: Binding Resolution")
    UIBindingExample.bindingResolutionWorkflow()
    println()

    println("Example 5: Reactive Rendering")
    UIBindingExample.reactiveRenderingWorkflow()
    println()

    println("Example 6: Event Binding")
    UIBindingExample.eventBindingWorkflow()
    println()

    println("Example 7: Full Integration")
    UIBindingExample.fullCalculatorIntegration()
}

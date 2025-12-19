package com.augmentalis.magicelements.renderer.ios

import com.augmentalis.avaelements.core.runtime.PluginLoader
import com.augmentalis.avaelements.core.runtime.PluginSource
import com.augmentalis.avaelements.renderer.ios.bridge.SwiftUIView

/**
 * iOS MEL Plugin Integration Example
 *
 * Demonstrates how to load and render MEL-based plugins on iOS.
 *
 * ## Example 1: Load from YAML file
 *
 * ```kotlin
 * suspend fun loadCalculatorPlugin(): SwiftUIView {
 *     val loader = PluginLoader()
 *     val runtime = loader.loadMELPlugin(
 *         PluginSource.File("/path/to/calculator.yaml")
 *     )
 *
 *     val renderer = MELPluginRenderer(runtime)
 *     return renderer.render()
 * }
 * ```
 *
 * ## Example 2: Load from YAML string
 *
 * ```kotlin
 * suspend fun loadInlinePlugin(): SwiftUIView {
 *     val yamlPlugin = """
 *         plugin:
 *           id: counter
 *           name: Counter
 *           version: 1.0.0
 *           tier: data
 *
 *         state:
 *           count:
 *             type: number
 *             default: 0
 *
 *         reducers:
 *           increment:
 *             nextState:
 *               count: ${'$'}math.add(${'$'}state.count, 1)
 *
 *           decrement:
 *             nextState:
 *               count: ${'$'}math.subtract(${'$'}state.count, 1)
 *
 *           reset:
 *             nextState:
 *               count: 0
 *
 *         ui:
 *           type: Column
 *           props:
 *             spacing: 16
 *             alignment: center
 *           children:
 *             - type: Text
 *               bindings:
 *                 content: ${'$'}state.count
 *               props:
 *                 fontSize: 48
 *
 *             - type: Row
 *               props:
 *                 spacing: 8
 *               children:
 *                 - type: Button
 *                   props:
 *                     label: "-"
 *                   events:
 *                     onTap: decrement
 *
 *                 - type: Button
 *                   props:
 *                     label: "Reset"
 *                   events:
 *                     onTap: reset
 *
 *                 - type: Button
 *                   props:
 *                     label: "+"
 *                   events:
 *                     onTap: increment
 *     """.trimIndent()
 *
 *     val loader = PluginLoader()
 *     val runtime = loader.loadMELPlugin(
 *         PluginSource.Data(yamlPlugin, PluginSource.Data.Format.YAML)
 *     )
 *
 *     val renderer = MELPluginRenderer(runtime)
 *     return renderer.render()
 * }
 * ```
 *
 * ## Example 3: Handle events
 *
 * ```kotlin
 * class PluginViewController {
 *     private lateinit var renderer: MELPluginRenderer
 *
 *     suspend fun setup() {
 *         val loader = PluginLoader()
 *         val runtime = loader.loadMELPlugin(
 *             PluginSource.File("/path/to/plugin.yaml")
 *         )
 *
 *         renderer = MELPluginRenderer(runtime)
 *
 *         // Subscribe to UI updates
 *         renderer.onUpdate { updatedView ->
 *             // Refresh UI with updated view
 *             refreshUI(updatedView)
 *         }
 *
 *         // Initial render
 *         val view = renderer.render()
 *         showUI(view)
 *     }
 *
 *     fun handleUserTap(actionExpr: String) {
 *         // User tapped a button with action "increment"
 *         renderer.handleEvent("onTap", actionExpr)
 *         // UI automatically updates via subscription
 *     }
 *
 *     fun cleanup() {
 *         renderer.destroy()
 *     }
 * }
 * ```
 *
 * ## Example 4: Observe state changes
 *
 * ```kotlin
 * suspend fun observePluginState() {
 *     val loader = PluginLoader()
 *     val runtime = loader.loadMELPlugin(
 *         PluginSource.File("/path/to/plugin.yaml")
 *     )
 *
 *     // Observe state flow
 *     runtime.stateFlow.collect { state ->
 *         println("State changed: ${state.snapshot()}")
 *     }
 * }
 * ```
 *
 * ## Tier 1 Enforcement on iOS
 *
 * All plugins are automatically downgraded to Tier 1 (DATA) on iOS:
 * - Only whitelisted functions allowed
 * - No arbitrary expressions
 * - No scripts
 * - No HTTP/storage/navigation APIs
 *
 * This ensures Apple App Store compliance while maintaining functionality
 * through declarative templates.
 *
 * @since 2.0.0
 */
object MELPluginExample {

    /**
     * Create a simple counter plugin programmatically
     */
    fun createCounterPlugin(): String {
        return """
            plugin:
              id: counter
              name: Counter
              version: 1.0.0
              tier: data

            state:
              count:
                type: number
                default: 0

            reducers:
              increment:
                nextState:
                  count: ${'$'}math.add(${'$'}state.count, 1)

              decrement:
                nextState:
                  count: ${'$'}math.subtract(${'$'}state.count, 1)

              reset:
                nextState:
                  count: 0

            ui:
              type: Column
              props:
                spacing: 16
                alignment: center
              children:
                - type: Text
                  bindings:
                    content: ${'$'}state.count
                  props:
                    fontSize: 48
                    fontWeight: bold

                - type: Row
                  props:
                    spacing: 8
                  children:
                    - type: Button
                      props:
                        label: "-"
                      events:
                        onTap: decrement

                    - type: Button
                      props:
                        label: "Reset"
                      events:
                        onTap: reset

                    - type: Button
                      props:
                        label: "+"
                      events:
                        onTap: increment
        """.trimIndent()
    }
}

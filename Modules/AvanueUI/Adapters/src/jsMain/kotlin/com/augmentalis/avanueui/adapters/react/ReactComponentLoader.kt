package com.augmentalis.avanueui.adapters.react

import com.augmentalis.avanueui.foundation.*
import com.augmentalis.avanueui.core.*
import kotlinx.browser.document
import org.w3c.dom.Element

/**
 * ReactComponentLoader - Dynamic React component loading and rendering
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

@JsExport
object ReactComponentLoader {

    private val componentCache = mutableMapOf<String, dynamic>()

    /**
     * Load React component dynamically
     */
    fun loadComponent(componentName: String): Promise<dynamic> {
        // Check cache first
        if (componentCache.containsKey(componentName)) {
            return Promise.resolve(componentCache[componentName])
        }

        // Get component path from registry
        val componentPath = ReactComponentRegistry.getComponentPath(componentName)
            ?: return Promise.reject(Exception("Component not found: $componentName"))

        // Dynamic import
        return js("import(componentPath)").then { module ->
            val component = module.default ?: module[componentName]
            componentCache[componentName] = component
            component
        }
    }

    /**
     * Render component to DOM element
     */
    fun renderComponent(
        componentName: String,
        props: dynamic,
        containerId: String
    ): Promise<Unit> {
        return loadComponent(componentName).then { component ->
            val container = document.getElementById(containerId) as? Element
                ?: throw Exception("Container not found: $containerId")

            // Use React to render
            val React = js("require('react')")
            val ReactDOM = js("require('react-dom/client')")

            val root = ReactDOM.createRoot(container)
            val element = React.createElement(component, props)
            root.render(element)
        }
    }

    /**
     * Unmount component from DOM
     */
    fun unmountComponent(containerId: String) {
        val container = document.getElementById(containerId) as? Element ?: return
        val ReactDOM = js("require('react-dom/client')")
        val root = ReactDOM.createRoot(container)
        root.unmount()
    }
}

/**
 * Component-specific loaders
 */
@JsExport
object MagicButtonLoader {
    fun load(component: MagicButton, containerId: String): Promise<Unit> {
        val bridge = ReactButtonBridge(component)
        val props = bridge.toReactProps()
        return ReactComponentLoader.renderComponent("MagicButton", props, containerId)
    }
}

@JsExport
object MagicCardLoader {
    fun load(component: MagicCard, containerId: String): Promise<Unit> {
        val bridge = ReactCardBridge(component)
        val props = bridge.toReactProps()
        return ReactComponentLoader.renderComponent("MagicCard", props, containerId)
    }
}

@JsExport
object MagicTextFieldLoader {
    fun load(component: MagicTextField, containerId: String): Promise<Unit> {
        val bridge = ReactTextFieldBridge(component)
        val props = bridge.toReactProps()
        return ReactComponentLoader.renderComponent("MagicTextField", props, containerId)
    }
}

/**
 * Screen renderer
 */
@JsExport
class ReactScreenRenderer(private val containerId: String) {

    fun renderScreen(screenData: dynamic): Promise<Unit> {
        val components = screenData.components as Array<dynamic>

        return Promise.all(
            components.map { componentData ->
                val componentType = componentData.type as String
                val componentId = componentData.id as String
                val props = componentData.props

                ReactComponentLoader.renderComponent(componentType, props, componentId)
            }.toTypedArray()
        ).then { }
    }

    fun clear() {
        ReactComponentLoader.unmountComponent(containerId)
    }
}

/**
 * JSX-like builder (Kotlin DSL for React)
 */
@JsExport
class ReactComponentBuilder {

    private val elements = mutableListOf<dynamic>()

    fun button(props: dynamic, block: ReactComponentBuilder.() -> Unit = {}) {
        val element = js("{}")
        element.type = "MagicButton"
        element.props = props
        val builder = ReactComponentBuilder()
        builder.block()
        element.children = builder.build()
        elements.add(element)
    }

    fun text(props: dynamic) {
        val element = js("{}")
        element.type = "MagicText"
        element.props = props
        elements.add(element)
    }

    fun textField(props: dynamic) {
        val element = js("{}")
        element.type = "MagicTextField"
        element.props = props
        elements.add(element)
    }

    fun card(props: dynamic, block: ReactComponentBuilder.() -> Unit) {
        val element = js("{}")
        element.type = "MagicCard"
        element.props = props
        val builder = ReactComponentBuilder()
        builder.block()
        element.children = builder.build()
        elements.add(element)
    }

    fun column(props: dynamic, block: ReactComponentBuilder.() -> Unit) {
        val element = js("{}")
        element.type = "MagicColumn"
        element.props = props
        val builder = ReactComponentBuilder()
        builder.block()
        element.children = builder.build()
        elements.add(element)
    }

    fun row(props: dynamic, block: ReactComponentBuilder.() -> Unit) {
        val element = js("{}")
        element.type = "MagicRow"
        element.props = props
        val builder = ReactComponentBuilder()
        builder.block()
        element.children = builder.build()
        elements.add(element)
    }

    fun build(): Array<dynamic> {
        return elements.toTypedArray()
    }
}

/**
 * React hooks bridge
 */
@JsExport
object ReactHooks {

    fun useState(initialValue: dynamic): Pair<dynamic, (dynamic) -> Unit> {
        val React = js("require('react')")
        val result = React.useState(initialValue)
        return Pair(result[0], result[1] as (dynamic) -> Unit)
    }

    fun useEffect(effect: () -> Unit, dependencies: Array<dynamic>? = null) {
        val React = js("require('react')")
        React.useEffect(effect, dependencies)
    }

    fun useMemo(factory: () -> dynamic, dependencies: Array<dynamic>): dynamic {
        val React = js("require('react')")
        return React.useMemo(factory, dependencies)
    }

    fun useCallback(callback: () -> Unit, dependencies: Array<dynamic>): () -> Unit {
        val React = js("require('react')")
        return React.useCallback(callback, dependencies)
    }
}

/**
 * Material-UI theme integration
 */
@JsExport
object MaterialUIThemeProvider {

    fun createTheme(themeConfig: dynamic): dynamic {
        val MUI = js("require('@mui/material')")
        return MUI.createTheme(themeConfig)
    }

    fun applyTheme(theme: dynamic, containerId: String) {
        val MUI = js("require('@mui/material')")
        val React = js("require('react')")
        val ReactDOM = js("require('react-dom/client')")

        val container = document.getElementById(containerId) as? Element
            ?: throw Exception("Container not found: $containerId")

        val ThemeProvider = MUI.ThemeProvider
        val root = ReactDOM.createRoot(container)

        val element = React.createElement(
            ThemeProvider,
            js("{ theme: theme }"),
            container.innerHTML
        )

        root.render(element)
    }
}

/**
 * Event handler utilities
 */
@JsExport
object ReactEventUtils {

    fun createClickHandler(handler: () -> Unit): dynamic {
        return { _: dynamic -> handler() }
    }

    fun createChangeHandler(handler: (String) -> Unit): dynamic {
        return { event: dynamic ->
            val value = event.target.value as String
            handler(value)
        }
    }

    fun createCheckedChangeHandler(handler: (Boolean) -> Unit): dynamic {
        return { event: dynamic ->
            val checked = event.target.checked as Boolean
            handler(checked)
        }
    }
}

/**
 * Component lifecycle manager
 */
@JsExport
class ReactComponentLifecycle {

    private val mountedComponents = mutableMapOf<String, dynamic>()

    fun onMount(componentId: String, callback: () -> Unit) {
        ReactHooks.useEffect({
            callback()
        }, emptyArray())
    }

    fun onUnmount(componentId: String, callback: () -> Unit) {
        ReactHooks.useEffect({
            object {
                fun cleanup() = callback()
            }
        }, emptyArray())
    }

    fun track(componentId: String, component: dynamic) {
        mountedComponents[componentId] = component
    }

    fun untrack(componentId: String) {
        mountedComponents.remove(componentId)
    }

    fun unmountAll() {
        mountedComponents.keys.forEach { componentId ->
            ReactComponentLoader.unmountComponent(componentId)
        }
        mountedComponents.clear()
    }
}

/**
 * External declarations for TypeScript definitions
 */
external interface Promise<T> {
    fun then(onFulfilled: (T) -> dynamic): Promise<dynamic>
    fun catch(onRejected: (Throwable) -> Unit): Promise<T>

    companion object {
        fun <T> resolve(value: T): Promise<T>
        fun <T> reject(reason: Throwable): Promise<T>
        fun <T> all(promises: Array<Promise<T>>): Promise<Array<T>>
    }
}

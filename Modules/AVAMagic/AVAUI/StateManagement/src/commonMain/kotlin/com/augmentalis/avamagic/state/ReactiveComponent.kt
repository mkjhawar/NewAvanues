package com.augmentalis.avamagic.state

import com.augmentalis.avamagic.core.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Component that automatically reacts to state changes.
 *
 * ReactiveComponent rebuilds its content whenever the observed state changes,
 * providing a declarative way to create dynamic UIs.
 *
 * Usage:
 * ```kotlin
 * val counter = mutableStateOf(0)
 * ReactiveComponent(counter) { count ->
 *     Text("Count: $count")
 * }
 * ```
 */
class ReactiveComponent<T>(
    private val state: StateFlow<T>,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    private val builder: (T) -> Component
) : Component {

    private var currentComponent: Component? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Initial build
        currentComponent = builder(state.value)

        // Observe state changes
        scope.launch {
            state.collect { value ->
                currentComponent = builder(value)
                // In a real implementation, you'd trigger a recomposition here
            }
        }
    }

    override fun render(renderer: Renderer): Any {
        return currentComponent?.render(renderer)
            ?: throw IllegalStateException("ReactiveComponent has no content")
    }
}

/**
 * Component that observes multiple states
 */
class MultiStateReactiveComponent(
    private val states: List<StateFlow<*>>,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    private val builder: (List<Any?>) -> Component
) : Component {

    private var currentComponent: Component? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Initial build
        currentComponent = builder(states.map { it.value })

        // Observe all state changes
        states.forEach { state ->
            scope.launch {
                state.collect {
                    currentComponent = builder(states.map { s -> s.value })
                }
            }
        }
    }

    override fun render(renderer: Renderer): Any {
        return currentComponent?.render(renderer)
            ?: throw IllegalStateException("MultiStateReactiveComponent has no content")
    }
}

/**
 * Conditional component that shows/hides based on state
 */
class ConditionalComponent(
    private val condition: StateFlow<Boolean>,
    private val content: Component,
    private val elseContent: Component? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return if (condition.value) {
            content.render(renderer)
        } else {
            elseContent?.render(renderer) ?: EmptyComponent().render(renderer)
        }
    }
}

/**
 * Empty component that renders nothing
 */
class EmptyComponent : Component {
    override val id: String? = null
    override val style: ComponentStyle? = null
    override val modifiers: List<Modifier> = emptyList()

    override fun render(renderer: Renderer): Any {
        // Platform-specific empty implementation
        return when (renderer.platform) {
            Renderer.Platform.Android -> Unit
            Renderer.Platform.iOS -> Unit
            else -> Unit
        }
    }
}

/**
 * List component that reacts to collection changes
 */
class ReactiveListComponent<T>(
    private val items: StateFlow<List<T>>,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    private val itemBuilder: (T, Int) -> Component
) : Component {

    private var currentComponents: List<Component> = emptyList()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Initial build
        currentComponents = items.value.mapIndexed { index, item ->
            itemBuilder(item, index)
        }

        // Observe changes
        scope.launch {
            items.collect { newItems ->
                currentComponents = newItems.mapIndexed { index, item ->
                    itemBuilder(item, index)
                }
            }
        }
    }

    override fun render(renderer: Renderer): Any {
        // Render as a column by default
        return ColumnComponent(
            id = id,
            style = style,
            modifiers = modifiers,
            children = currentComponents
        ).render(renderer)
    }
}

/**
 * Switch component that renders different content based on state value
 */
class SwitchComponent<T>(
    private val state: StateFlow<T>,
    private val cases: Map<T, Component>,
    private val defaultCase: Component? = null,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        val component = cases[state.value] ?: defaultCase ?: EmptyComponent()
        return component.render(renderer)
    }
}

/**
 * Animated component that transitions between states
 */
class AnimatedComponent<T>(
    private val state: StateFlow<T>,
    private val transition: Transition,
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    private val builder: (T) -> Component
) : Component {

    private var currentComponent: Component? = null

    init {
        currentComponent = builder(state.value)
    }

    override fun render(renderer: Renderer): Any {
        val component = currentComponent ?: builder(state.value)

        // Apply animation modifier
        val animatedComponent = object : Component {
            override val id = component.id
            override val style = component.style
            override val modifiers = component.modifiers + Modifier.Animated(transition)

            override fun render(renderer: Renderer): Any = component.render(renderer)
        }

        return animatedComponent.render(renderer)
    }
}

// ==================== DSL Builders ====================

/**
 * Create a reactive component
 */
fun <T> reactive(
    state: StateFlow<T>,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList(),
    builder: (T) -> Component
): ReactiveComponent<T> {
    return ReactiveComponent(state, id, style, modifiers, builder)
}

/**
 * Create a reactive component from MagicState
 */
fun <T> reactive(
    state: MagicState<T>,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList(),
    builder: (T) -> Component
): ReactiveComponent<T> {
    return ReactiveComponent(state.value, id, style, modifiers, builder)
}

/**
 * Create a conditional component
 */
fun conditional(
    condition: StateFlow<Boolean>,
    content: Component,
    elseContent: Component? = null,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList()
): ConditionalComponent {
    return ConditionalComponent(condition, content, elseContent, id, style, modifiers)
}

/**
 * Create a reactive list
 */
fun <T> reactiveList(
    items: StateFlow<List<T>>,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList(),
    itemBuilder: (T, Int) -> Component
): ReactiveListComponent<T> {
    return ReactiveListComponent(items, id, style, modifiers, itemBuilder)
}

/**
 * Create a switch component
 */
fun <T> switch(
    state: StateFlow<T>,
    cases: Map<T, Component>,
    defaultCase: Component? = null,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList()
): SwitchComponent<T> {
    return SwitchComponent(state, cases, defaultCase, id, style, modifiers)
}

/**
 * Create an animated component
 */
fun <T> animated(
    state: StateFlow<T>,
    transition: Transition,
    id: String? = null,
    style: ComponentStyle? = null,
    modifiers: List<Modifier> = emptyList(),
    builder: (T) -> Component
): AnimatedComponent<T> {
    return AnimatedComponent(state, transition, id, style, modifiers, builder)
}

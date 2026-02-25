package com.augmentalis.avanueui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Base interface for MagicElements components
 */
interface Identifiable {
    val id: String?
    val className: String?
}

/**
 * Wraps a component with state management capabilities
 */
open class StatefulComponent<T : Identifiable>(
    val component: T,
    val state: ComponentStateHolder
) {
    /**
     * Get the component's ID for state tracking
     */
    val componentId: String
        get() = component.id ?: component.hashCode().toString()
}

/**
 * Holds and manages state for a single component
 */
class ComponentStateHolder {
    private val properties = mutableMapOf<String, MutableState<Any?>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    /**
     * Get or create a state property
     */
    fun <T> getProperty(name: String, default: T): MutableState<T> {
        @Suppress("UNCHECKED_CAST")
        return properties.getOrPut(name) {
            val state = mutableStateOf<Any?>(default)

            // Set up listener notifications
            state.observe().onEach { value ->
                listeners[name]?.forEach { listener ->
                    listener(value)
                }
            }.launchIn(coroutineScope)

            state
        } as MutableState<T>
    }

    /**
     * Set a property value
     */
    fun <T> setProperty(name: String, value: T) {
        @Suppress("UNCHECKED_CAST")
        val state = properties[name] as? MutableState<T>
        state?.update(value)
    }

    /**
     * Add a listener for property changes
     */
    fun addPropertyListener(name: String, listener: (Any?) -> Unit) {
        listeners.getOrPut(name) { mutableListOf() }.add(listener)
    }

    /**
     * Remove a property listener
     */
    fun removePropertyListener(name: String, listener: (Any?) -> Unit) {
        listeners[name]?.remove(listener)
    }

    /**
     * Get all property names
     */
    fun propertyNames(): Set<String> {
        return properties.keys.toSet()
    }

    /**
     * Check if a property exists
     */
    fun hasProperty(name: String): Boolean {
        return properties.containsKey(name)
    }

    /**
     * Clear all properties
     */
    fun clear() {
        properties.clear()
        listeners.clear()
    }
}

/**
 * Example: Basic component interface
 */
interface TextFieldComponent : Identifiable {
    val value: String
    val placeholder: String
    val label: String?
    val leadingIcon: String?
    val trailingIcon: String?
    val onValueChange: ((String) -> Unit)?
}

/**
 * Example: Data class implementation
 */
data class TextFieldComponentImpl(
    override val id: String? = null,
    override val className: String? = null,
    override val value: String = "",
    override val placeholder: String = "",
    override val label: String? = null,
    override val leadingIcon: String? = null,
    override val trailingIcon: String? = null,
    override val onValueChange: ((String) -> Unit)? = null
) : TextFieldComponent

/**
 * Stateful TextField with built-in state management
 */
class StatefulTextField(
    component: TextFieldComponent,
    state: ComponentStateHolder
) : StatefulComponent<TextFieldComponent>(component, state) {

    /**
     * Text state - automatically syncs with component
     */
    val text: MutableState<String> = state.getProperty("text", component.value)

    /**
     * Error state for validation
     */
    val error: MutableState<String?> = state.getProperty("error", null)

    /**
     * Validity state
     */
    val isValid: MutableState<Boolean> = state.getProperty("isValid", true)

    /**
     * Focus state
     */
    val isFocused: MutableState<Boolean> = state.getProperty("isFocused", false)

    /**
     * Update text and trigger change handler
     */
    fun updateText(newText: String) {
        text.update(newText)
        component.onValueChange?.invoke(newText)
    }

    /**
     * Set error message
     */
    fun setError(message: String?) {
        error.update(message)
        isValid.update(message == null)
    }

    /**
     * Clear error
     */
    fun clearError() {
        setError(null)
    }

    /**
     * Set focus state
     */
    fun setFocus(focused: Boolean) {
        isFocused.update(focused)
    }
}

/**
 * Example: Button component
 */
interface ButtonComponent : Identifiable {
    val text: String
    val enabled: Boolean
    val onClick: (() -> Unit)?
}

/**
 * Stateful Button
 */
class StatefulButton(
    component: ButtonComponent,
    state: ComponentStateHolder
) : StatefulComponent<ButtonComponent>(component, state) {

    val enabled: MutableState<Boolean> = state.getProperty("enabled", component.enabled)
    val isPressed: MutableState<Boolean> = state.getProperty("isPressed", false)
    val clickCount: MutableState<Int> = state.getProperty("clickCount", 0)

    fun click() {
        if (enabled.value) {
            clickCount.updateWith { it + 1 }
            component.onClick?.invoke()
        }
    }

    fun setEnabled(isEnabled: Boolean) {
        enabled.update(isEnabled)
    }
}

/**
 * Factory functions for creating stateful components
 */
fun createStatefulTextField(
    value: String = "",
    placeholder: String = "",
    label: String? = null,
    onValueChange: ((String) -> Unit)? = null
): StatefulTextField {
    val component = TextFieldComponentImpl(
        value = value,
        placeholder = placeholder,
        label = label,
        onValueChange = onValueChange
    )
    return StatefulTextField(component, ComponentStateHolder())
}

/**
 * Extension to add state to any component
 */
fun <T : Identifiable> T.withState(): StatefulComponent<T> {
    return StatefulComponent(this, ComponentStateHolder())
}

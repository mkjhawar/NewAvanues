package com.augmentalis.avanueui.adapters.react

import com.augmentalis.avanueui.foundation.*
import com.augmentalis.avanueui.core.*

/**
 * ReactBridge - Kotlin/JS bridge to React components
 *
 * Provides expect/actual pattern for Web platform
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

/**
 * Foundation Component Bridges
 */
@JsExport
class ReactButtonBridge(private val component: MagicButton) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.text = component.text
        props.variant = component.variant.name
        props.size = component.size.name
        props.enabled = component.enabled
        props.fullWidth = component.fullWidth
        props.icon = component.icon
        props.iconPosition = component.iconPosition.name
        props.onClick = component.onClick
        return props
    }
}

@JsExport
class ReactCardBridge(private val component: MagicCard) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.content = component.content.toTypedArray()
        props.elevated = component.elevated
        props.variant = component.variant.name
        props.onClick = component.onClick
        return props
    }
}

@JsExport
class ReactCheckboxBridge(private val component: MagicCheckbox) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.checked = component.checked
        props.onCheckedChange = component.onCheckedChange
        props.label = component.label
        props.enabled = component.enabled
        props.state = component.state.name
        return props
    }
}

@JsExport
class ReactChipBridge(private val component: MagicChip) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.label = component.label
        props.variant = component.variant.name
        props.icon = component.icon
        props.deletable = component.deletable
        props.onClick = component.onClick
        props.onDelete = component.onDelete
        return props
    }
}

@JsExport
class ReactDividerBridge(private val component: MagicDivider) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.orientation = component.orientation.name
        props.thickness = component.thickness
        props.color = component.color
        props.inset = component.inset
        return props
    }
}

@JsExport
class ReactImageBridge(private val component: MagicImage) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.source = component.source
        props.alt = component.alt
        props.fit = component.fit.name
        props.width = component.width
        props.height = component.height
        props.onClick = component.onClick
        return props
    }
}

@JsExport
class ReactListItemBridge(private val component: MagicListItem) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.title = component.title
        props.subtitle = component.subtitle
        props.leadingIcon = component.leadingIcon
        props.trailingIcon = component.trailingIcon
        props.onClick = component.onClick
        props.showDivider = component.showDivider
        return props
    }
}

@JsExport
class ReactTextBridge(private val component: MagicText) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.content = component.content
        props.variant = component.variant.name
        props.color = component.color
        props.align = component.align.name
        props.bold = component.bold
        props.italic = component.italic
        props.underline = component.underline
        props.maxLines = component.maxLines
        return props
    }
}

@JsExport
class ReactTextFieldBridge(private val component: MagicTextField) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.value = component.value
        props.onValueChange = component.onValueChange
        props.label = component.label
        props.placeholder = component.placeholder
        props.enabled = component.enabled
        props.readOnly = component.readOnly
        props.type = component.type.name
        props.error = component.error
        props.helperText = component.helperText
        props.leadingIcon = component.leadingIcon
        props.trailingIcon = component.trailingIcon
        props.maxLines = component.maxLines
        return props
    }
}

/**
 * Core Component Bridges
 */
@JsExport
class ReactColorPickerBridge(private val component: MagicColorPicker) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.selectedColor = component.selectedColor
        props.onColorChange = component.onColorChange
        props.mode = component.mode.name
        props.showAlpha = component.showAlpha
        props.presetColors = component.presetColors?.toTypedArray()
        props.label = component.label
        return props
    }
}

@JsExport
class ReactIconPickerBridge(private val component: MagicIconPicker) {
    fun toReactProps(): dynamic {
        val props = js("{}")
        props.selectedIcon = component.selectedIcon
        props.onIconChange = component.onIconChange
        props.library = component.library.name
        props.searchQuery = component.searchQuery
        props.category = component.category
        props.iconSize = component.iconSize.name
        props.columns = component.columns
        props.label = component.label
        return props
    }
}

/**
 * Bridge Factory
 */
@JsExport
object ReactBridgeFactory {
    fun createButtonBridge(component: MagicButton) = ReactButtonBridge(component)
    fun createCardBridge(component: MagicCard) = ReactCardBridge(component)
    fun createCheckboxBridge(component: MagicCheckbox) = ReactCheckboxBridge(component)
    fun createChipBridge(component: MagicChip) = ReactChipBridge(component)
    fun createDividerBridge(component: MagicDivider) = ReactDividerBridge(component)
    fun createImageBridge(component: MagicImage) = ReactImageBridge(component)
    fun createListItemBridge(component: MagicListItem) = ReactListItemBridge(component)
    fun createTextBridge(component: MagicText) = ReactTextBridge(component)
    fun createTextFieldBridge(component: MagicTextField) = ReactTextFieldBridge(component)
    fun createColorPickerBridge(component: MagicColorPicker) = ReactColorPickerBridge(component)
    fun createIconPickerBridge(component: MagicIconPicker) = ReactIconPickerBridge(component)
}

/**
 * React Component Registry
 * Maps Kotlin components to React TypeScript components
 */
@JsExport
object ReactComponentRegistry {
    private val componentMap = mutableMapOf<String, String>()

    init {
        // Register Foundation components
        registerComponent("MagicButton", "./components/foundation/MagicButton")
        registerComponent("MagicCard", "./components/foundation/MagicCard")
        registerComponent("MagicCheckbox", "./components/foundation/MagicCheckbox")
        registerComponent("MagicChip", "./components/foundation/MagicChip")
        registerComponent("MagicDivider", "./components/foundation/MagicDivider")
        registerComponent("MagicImage", "./components/foundation/MagicImage")
        registerComponent("MagicListItem", "./components/foundation/MagicListItem")
        registerComponent("MagicText", "./components/foundation/MagicText")
        registerComponent("MagicTextField", "./components/foundation/MagicTextField")

        // Register Core components
        registerComponent("MagicColorPicker", "./components/core/MagicColorPicker")
        registerComponent("MagicIconPicker", "./components/core/MagicIconPicker")
    }

    fun registerComponent(name: String, path: String) {
        componentMap[name] = path
    }

    fun getComponentPath(name: String): String? {
        return componentMap[name]
    }

    fun getAllComponents(): Map<String, String> {
        return componentMap.toMap()
    }
}

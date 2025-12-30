package com.augmentalis.magicui.registry

/**
 * Registers all built-in components.
 */
object BuiltInComponents {

    suspend fun registerAll(registry: ComponentRegistry) {
        registry.register(colorPickerDescriptor())
        registry.register(preferencesDescriptor())
        registry.register(textDescriptor())
        registry.register(buttonDescriptor())
        registry.register(containerDescriptor())
        registry.register(textFieldDescriptor())
        registry.register(checkboxDescriptor())
        registry.register(listViewDescriptor())
        registry.register(databaseDescriptor())
        registry.register(dialogDescriptor())
    }

    private fun colorPickerDescriptor() = ComponentDescriptor(
        type = "ColorPicker",
        displayName = "Color Picker",
        description = "Interactive color selection component",
        properties = mapOf(
            "id" to PropertyDescriptor(
                name = "id",
                type = PropertyType.STRING,
                required = false,
                description = "Unique component identifier"
            ),
            "initialColor" to PropertyDescriptor(
                name = "initialColor",
                type = PropertyType.COLOR,
                required = false,
                defaultValue = "#FFFFFF",
                description = "Initial selected color"
            ),
            "mode" to PropertyDescriptor(
                name = "mode",
                type = PropertyType.ENUM,
                required = false,
                defaultValue = "FULL",
                enumValues = listOf("FULL", "COMPACT", "PRESETS_ONLY", "HEX_ONLY", "WHEEL", "HSV_SLIDERS", "RGB_SLIDERS", "DESIGNER"),
                description = "Color picker display mode"
            ),
            "showAlpha" to PropertyDescriptor(
                name = "showAlpha",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = true,
                description = "Show alpha channel control"
            )
        ),
        callbacks = mapOf(
            "onColorChanged" to CallbackDescriptor(
                name = "onColorChanged",
                parameters = listOf(
                    CallbackParameter("color", PropertyType.COLOR)
                ),
                description = "Called when color changes"
            ),
            "onConfirm" to CallbackDescriptor(
                name = "onConfirm",
                parameters = listOf(
                    CallbackParameter("color", PropertyType.COLOR)
                ),
                description = "Called when user confirms color"
            ),
            "onCancel" to CallbackDescriptor(
                name = "onCancel",
                parameters = emptyList(),
                description = "Called when user cancels"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.INPUT
    )

    private fun preferencesDescriptor() = ComponentDescriptor(
        type = "Preferences",
        displayName = "Preferences",
        description = "Key-value preference storage",
        properties = emptyMap(),
        callbacks = emptyMap(),
        supportsChildren = false,
        category = ComponentCategory.GENERAL
    )

    private fun textDescriptor() = ComponentDescriptor(
        type = "Text",
        displayName = "Text",
        description = "Display text",
        properties = mapOf(
            "text" to PropertyDescriptor(
                name = "text",
                type = PropertyType.STRING,
                required = true,
                description = "Text content"
            ),
            "size" to PropertyDescriptor(
                name = "size",
                type = PropertyType.FLOAT,
                required = false,
                defaultValue = 16f,
                description = "Font size in sp"
            ),
            "color" to PropertyDescriptor(
                name = "color",
                type = PropertyType.COLOR,
                required = false,
                defaultValue = "#000000",
                description = "Text color"
            )
        ),
        callbacks = emptyMap(),
        supportsChildren = false,
        category = ComponentCategory.DISPLAY
    )

    private fun buttonDescriptor() = ComponentDescriptor(
        type = "Button",
        displayName = "Button",
        description = "Clickable button",
        properties = mapOf(
            "text" to PropertyDescriptor(
                name = "text",
                type = PropertyType.STRING,
                required = true,
                description = "Button text"
            ),
            "enabled" to PropertyDescriptor(
                name = "enabled",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = true,
                description = "Button enabled state"
            )
        ),
        callbacks = mapOf(
            "onClick" to CallbackDescriptor(
                name = "onClick",
                parameters = emptyList(),
                description = "Called when button clicked"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.INPUT
    )

    private fun containerDescriptor() = ComponentDescriptor(
        type = "Container",
        displayName = "Container",
        description = "Layout container",
        properties = mapOf(
            "orientation" to PropertyDescriptor(
                name = "orientation",
                type = PropertyType.ENUM,
                required = false,
                defaultValue = "vertical",
                enumValues = listOf("vertical", "horizontal"),
                description = "Layout orientation"
            )
        ),
        callbacks = emptyMap(),
        supportsChildren = true,
        category = ComponentCategory.CONTAINER
    )

    private fun textFieldDescriptor() = ComponentDescriptor(
        type = "TextField",
        displayName = "Text Field",
        description = "Text input component with validation",
        properties = mapOf(
            "id" to PropertyDescriptor(
                name = "id",
                type = PropertyType.STRING,
                required = false,
                description = "Unique component identifier"
            ),
            "text" to PropertyDescriptor(
                name = "text",
                type = PropertyType.STRING,
                required = false,
                defaultValue = "",
                description = "Current text value"
            ),
            "placeholder" to PropertyDescriptor(
                name = "placeholder",
                type = PropertyType.STRING,
                required = false,
                description = "Placeholder text"
            ),
            "maxLength" to PropertyDescriptor(
                name = "maxLength",
                type = PropertyType.INT,
                required = false,
                description = "Maximum character length"
            ),
            "inputType" to PropertyDescriptor(
                name = "inputType",
                type = PropertyType.ENUM,
                required = false,
                defaultValue = "TEXT",
                enumValues = listOf("TEXT", "NUMBER", "EMAIL", "PASSWORD", "PHONE", "URL"),
                description = "Input type"
            ),
            "enabled" to PropertyDescriptor(
                name = "enabled",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = true,
                description = "Enabled state"
            ),
            "readOnly" to PropertyDescriptor(
                name = "readOnly",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = false,
                description = "Read-only state"
            )
        ),
        callbacks = mapOf(
            "onTextChanged" to CallbackDescriptor(
                name = "onTextChanged",
                parameters = listOf(CallbackParameter("text", PropertyType.STRING)),
                description = "Called when text changes"
            ),
            "onSubmit" to CallbackDescriptor(
                name = "onSubmit",
                parameters = listOf(CallbackParameter("text", PropertyType.STRING)),
                description = "Called when user submits (Enter key)"
            ),
            "onFocusChanged" to CallbackDescriptor(
                name = "onFocusChanged",
                parameters = listOf(CallbackParameter("focused", PropertyType.BOOLEAN)),
                description = "Called when focus changes"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.INPUT
    )

    private fun checkboxDescriptor() = ComponentDescriptor(
        type = "Checkbox",
        displayName = "Checkbox",
        description = "Checkbox input component",
        properties = mapOf(
            "id" to PropertyDescriptor(
                name = "id",
                type = PropertyType.STRING,
                required = false,
                description = "Unique component identifier"
            ),
            "checked" to PropertyDescriptor(
                name = "checked",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = false,
                description = "Checked state"
            ),
            "label" to PropertyDescriptor(
                name = "label",
                type = PropertyType.STRING,
                required = false,
                description = "Label text"
            ),
            "enabled" to PropertyDescriptor(
                name = "enabled",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = true,
                description = "Enabled state"
            ),
            "triState" to PropertyDescriptor(
                name = "triState",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = false,
                description = "Support indeterminate state"
            )
        ),
        callbacks = mapOf(
            "onCheckedChange" to CallbackDescriptor(
                name = "onCheckedChange",
                parameters = listOf(CallbackParameter("checked", PropertyType.BOOLEAN)),
                description = "Called when checked state changes"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.INPUT
    )

    private fun listViewDescriptor() = ComponentDescriptor(
        type = "ListView",
        displayName = "List View",
        description = "Scrollable list component with dynamic items",
        properties = mapOf(
            "items" to PropertyDescriptor(
                name = "items",
                type = PropertyType.LIST,
                required = false,
                description = "List of items to display"
            ),
            "selectedIndices" to PropertyDescriptor(
                name = "selectedIndices",
                type = PropertyType.LIST,
                required = false,
                description = "Indices of selected items"
            ),
            "selectionMode" to PropertyDescriptor(
                name = "selectionMode",
                type = PropertyType.ENUM,
                required = false,
                defaultValue = "NONE",
                enumValues = listOf("NONE", "SINGLE", "MULTIPLE"),
                description = "Selection mode for items"
            ),
            "emptyMessage" to PropertyDescriptor(
                name = "emptyMessage",
                type = PropertyType.STRING,
                required = false,
                defaultValue = "No items",
                description = "Message displayed when list is empty"
            )
        ),
        callbacks = mapOf(
            "onItemClick" to CallbackDescriptor(
                name = "onItemClick",
                parameters = listOf(
                    CallbackParameter("item", PropertyType.ANY),
                    CallbackParameter("index", PropertyType.INT)
                ),
                description = "Called when an item is clicked"
            ),
            "onItemLongPress" to CallbackDescriptor(
                name = "onItemLongPress",
                parameters = listOf(
                    CallbackParameter("item", PropertyType.ANY),
                    CallbackParameter("index", PropertyType.INT)
                ),
                description = "Called when an item is long pressed"
            ),
            "onSelectionChanged" to CallbackDescriptor(
                name = "onSelectionChanged",
                parameters = listOf(
                    CallbackParameter("indices", PropertyType.LIST)
                ),
                description = "Called when selection changes"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.LAYOUT
    )

    private fun databaseDescriptor() = ComponentDescriptor(
        type = "Database",
        displayName = "Database",
        description = "Persistent storage with key-value and collections",
        properties = mapOf(
            "name" to PropertyDescriptor(
                name = "name",
                type = PropertyType.STRING,
                required = true,
                description = "Database name"
            ),
            "version" to PropertyDescriptor(
                name = "version",
                type = PropertyType.INT,
                required = false,
                defaultValue = 1,
                description = "Database schema version"
            )
        ),
        callbacks = emptyMap(),
        supportsChildren = false,
        category = ComponentCategory.DATA
    )

    private fun dialogDescriptor() = ComponentDescriptor(
        type = "Dialog",
        displayName = "Dialog",
        description = "Alert, confirm, and input dialogs",
        properties = mapOf(
            "title" to PropertyDescriptor(
                name = "title",
                type = PropertyType.STRING,
                required = false,
                description = "Dialog title"
            ),
            "message" to PropertyDescriptor(
                name = "message",
                type = PropertyType.STRING,
                required = false,
                description = "Dialog message content"
            ),
            "isVisible" to PropertyDescriptor(
                name = "isVisible",
                type = PropertyType.BOOLEAN,
                required = false,
                defaultValue = false,
                description = "Dialog visibility state"
            ),
            "type" to PropertyDescriptor(
                name = "type",
                type = PropertyType.ENUM,
                required = false,
                defaultValue = "ALERT",
                enumValues = listOf("ALERT", "CONFIRM", "INPUT", "CUSTOM"),
                description = "Dialog type"
            )
        ),
        callbacks = mapOf(
            "onButtonClick" to CallbackDescriptor(
                name = "onButtonClick",
                parameters = listOf(
                    CallbackParameter("button", PropertyType.STRING)
                ),
                description = "Called when dialog button is clicked"
            ),
            "onDismiss" to CallbackDescriptor(
                name = "onDismiss",
                parameters = emptyList(),
                description = "Called when dialog is dismissed"
            ),
            "onShow" to CallbackDescriptor(
                name = "onShow",
                parameters = emptyList(),
                description = "Called when dialog is shown"
            )
        ),
        supportsChildren = false,
        category = ComponentCategory.OVERLAY
    )
}

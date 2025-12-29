package com.augmentalis.avamagic.registry

/**
 * Describes a component's metadata.
 */
data class ComponentDescriptor(
    val type: String,
    val displayName: String,
    val description: String,
    val properties: Map<String, PropertyDescriptor>,
    val callbacks: Map<String, CallbackDescriptor>,
    val supportsChildren: Boolean = false,
    val category: ComponentCategory = ComponentCategory.GENERAL
)

/**
 * Property descriptor.
 */
data class PropertyDescriptor(
    val name: String,
    val type: PropertyType,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val description: String = "",
    val enumValues: List<String>? = null
)

/**
 * Callback descriptor.
 */
data class CallbackDescriptor(
    val name: String,
    val parameters: List<CallbackParameter>,
    val description: String = ""
)

data class CallbackParameter(
    val name: String,
    val type: PropertyType
)

enum class PropertyType {
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    COLOR,
    ENUM,
    OBJECT,
    LIST,
    ANY
}

enum class ComponentCategory {
    INPUT,
    DISPLAY,
    CONTAINER,
    NAVIGATION,
    FEEDBACK,
    GENERAL,
    LAYOUT,
    DATA,
    OVERLAY
}

package com.augmentalis.magiccode.generator.ast

/**
 * AvaUI AST Node Types
 * Abstract Syntax Tree representation for AvaUI DSL
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

sealed class AvaUINode {
    abstract val id: String
    abstract val type: ComponentType
    abstract val properties: Map<String, Any>
    abstract val children: List<AvaUINode>
}

enum class ComponentType {
    // Foundation
    BUTTON, CARD, CHECKBOX, CHIP, DIVIDER, IMAGE, LIST_ITEM, TEXT, TEXT_FIELD,

    // Core
    COLOR_PICKER, ICON_PICKER,

    // Basic
    ICON, LABEL, CONTAINER, ROW, COLUMN, SPACER,

    // Advanced
    SWITCH, SLIDER, PROGRESS_BAR, SPINNER, ALERT, DIALOG, TOAST, TOOLTIP,
    RADIO, DROPDOWN, DATE_PICKER, TIME_PICKER, SEARCH_BAR, RATING, BADGE,
    FILE_UPLOAD, APP_BAR, BOTTOM_NAV, DRAWER, PAGINATION, TABS, BREADCRUMB, ACCORDION,

    // Layout
    STACK, GRID, SCROLL_VIEW,

    // Custom
    CUSTOM
}

/**
 * Component Node - represents a AvaUI component
 */
data class ComponentNode(
    override val id: String,
    override val type: ComponentType,
    override val properties: Map<String, Any> = emptyMap(),
    override val children: List<AvaUINode> = emptyList(),
    val eventHandlers: Map<String, String> = emptyMap()
) : AvaUINode()

/**
 * Property Value Types
 */
sealed class PropertyValue {
    data class StringValue(val value: String) : PropertyValue()
    data class IntValue(val value: Int) : PropertyValue()
    data class DoubleValue(val value: Double) : PropertyValue()
    data class BoolValue(val value: Boolean) : PropertyValue()
    data class EnumValue(val type: String, val value: String) : PropertyValue()
    data class ListValue(val items: List<PropertyValue>) : PropertyValue()
    data class MapValue(val items: Map<String, PropertyValue>) : PropertyValue()
    data class ReferenceValue(val ref: String) : PropertyValue()
}

/**
 * Event Handler
 */
data class EventHandler(
    val event: String,
    val handler: String,
    val parameters: List<String> = emptyList()
)

/**
 * Screen/View Definition
 */
data class ScreenNode(
    val name: String,
    val root: ComponentNode,
    val stateVariables: List<StateVariable> = emptyList(),
    val imports: List<String> = emptyList()
)

/**
 * State Variable
 */
data class StateVariable(
    val name: String,
    val type: String,
    val initialValue: PropertyValue?,
    val mutable: Boolean = true
)

/**
 * Theme Configuration
 */
data class ThemeNode(
    val name: String,
    val colors: Map<String, String>,
    val typography: Map<String, TypographyStyle>,
    val spacing: Map<String, Int>,
    val shapes: Map<String, ShapeStyle>
)

data class TypographyStyle(
    val fontFamily: String,
    val fontSize: Int,
    val fontWeight: String,
    val lineHeight: Double
)

data class ShapeStyle(
    val cornerRadius: Int,
    val borderWidth: Int? = null,
    val borderColor: String? = null
)

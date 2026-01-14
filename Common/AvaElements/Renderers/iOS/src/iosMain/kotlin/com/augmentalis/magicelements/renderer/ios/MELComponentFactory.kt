package com.augmentalis.magicelements.renderer.ios

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.magicelements.core.mel.ComponentFactory
import kotlinx.serialization.json.*

/**
 * iOS MEL Component Factory
 *
 * Creates SwiftUI components from MEL UINode definitions. Maps MEL component types
 * to their SwiftUI equivalents, handling prop conversion and event binding.
 *
 * Supported MEL components:
 * - Layout: Column, Row, Container, ScrollView, Spacer
 * - Display: Text, Icon, Image, Divider, Avatar, Badge
 * - Input: Button, TextField, Toggle, Slider, DatePicker, Dropdown
 * - Feedback: Alert, Toast, Spinner, ProgressBar, Tooltip
 * - Data: Table, List, Accordion, Stepper, Timeline
 *
 * ## Usage
 *
 * ```kotlin
 * val factory = iOSMELComponentFactory()
 * val component = factory.create(
 *     type = "Text",
 *     props = mapOf("content" to JsonPrimitive("Hello")),
 *     children = null,
 *     id = "text-1"
 * )
 * ```
 *
 * @since 2.0.0
 */
class iOSMELComponentFactory : ComponentFactory {

    /**
     * Create a SwiftUIView component from MEL UINode definition
     *
     * @param type Component type name (e.g., "Text", "Button", "Column")
     * @param props Resolved props as JSON elements
     * @param children Child components (for containers)
     * @param id Optional component ID
     * @return SwiftUIView bridge model
     */
    override fun create(
        type: String,
        props: Map<String, JsonElement>,
        children: List<Component>?,
        id: String?
    ): Component {
        // Convert Component children to SwiftUIView children
        val swiftUIChildren = children?.mapNotNull { it as? SwiftUIView } ?: emptyList()

        return when (type) {
            // Layout
            "Column" -> createColumn(props, swiftUIChildren, id)
            "Row" -> createRow(props, swiftUIChildren, id)
            "Container" -> createContainer(props, swiftUIChildren, id)
            "ScrollView" -> createScrollView(props, swiftUIChildren, id)
            "Spacer" -> createSpacer(props, id)

            // Display
            "Text" -> createText(props, id)
            "Icon" -> createIcon(props, id)
            "Image" -> createImage(props, id)
            "Divider" -> createDivider(props, id)
            "Avatar" -> createAvatar(props, id)
            "Badge" -> createBadge(props, id)

            // Input
            "Button" -> createButton(props, id)
            "TextField" -> createTextField(props, id)
            "Toggle" -> createToggle(props, id)
            "Slider" -> createSlider(props, id)
            "DatePicker" -> createDatePicker(props, id)
            "Dropdown" -> createDropdown(props, id)

            // Feedback
            "Alert" -> createAlert(props, id)
            "Toast" -> createToast(props, id)
            "Spinner" -> createSpinner(props, id)
            "ProgressBar" -> createProgressBar(props, id)
            "Tooltip" -> createTooltip(props, id)

            else -> createUnknown(type, props, id)
        }
    }

    /**
     * Check if a component type is supported
     */
    override fun supports(type: String): Boolean {
        return type in setOf(
            // Layout
            "Column", "Row", "Container", "ScrollView", "Spacer",
            // Display
            "Text", "Icon", "Image", "Divider", "Avatar", "Badge",
            // Input
            "Button", "TextField", "Toggle", "Slider", "DatePicker", "Dropdown",
            // Feedback
            "Alert", "Toast", "Spinner", "ProgressBar", "Tooltip"
        )
    }

    // ========== Layout Components ==========

    private fun createColumn(props: Map<String, JsonElement>, children: List<SwiftUIView>, id: String?): SwiftUIView {
        val spacing = props["spacing"]?.jsonPrimitive?.floatOrNull
        val alignment = props["alignment"]?.jsonPrimitive?.contentOrNull?.let {
            when (it) {
                "leading" -> HorizontalAlignment.Leading
                "center" -> HorizontalAlignment.Center
                "trailing" -> HorizontalAlignment.Trailing
                else -> HorizontalAlignment.Center
            }
        } ?: HorizontalAlignment.Center

        val modifiers = extractModifiers(props)

        return SwiftUIView.vStack(
            spacing = spacing,
            alignment = alignment,
            children = children,
            modifiers = modifiers,
            id = id
        )
    }

    private fun createRow(props: Map<String, JsonElement>, children: List<SwiftUIView>, id: String?): SwiftUIView {
        val spacing = props["spacing"]?.jsonPrimitive?.floatOrNull
        val alignment = props["alignment"]?.jsonPrimitive?.contentOrNull?.let {
            when (it) {
                "top" -> VerticalAlignment.Top
                "center" -> VerticalAlignment.Center
                "bottom" -> VerticalAlignment.Bottom
                else -> VerticalAlignment.Center
            }
        } ?: VerticalAlignment.Center

        val modifiers = extractModifiers(props)

        return SwiftUIView.hStack(
            spacing = spacing,
            alignment = alignment,
            children = children,
            modifiers = modifiers,
            id = id
        )
    }

    private fun createContainer(props: Map<String, JsonElement>, children: List<SwiftUIView>, id: String?): SwiftUIView {
        val modifiers = extractModifiers(props)

        return SwiftUIView(
            type = ViewType.Group,
            id = id,
            properties = emptyMap(),
            modifiers = modifiers,
            children = children
        )
    }

    private fun createScrollView(props: Map<String, JsonElement>, children: List<SwiftUIView>, id: String?): SwiftUIView {
        val modifiers = extractModifiers(props)

        return SwiftUIView(
            type = ViewType.ScrollView,
            id = id,
            properties = emptyMap(),
            modifiers = modifiers,
            children = children
        )
    }

    private fun createSpacer(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Spacer,
            id = id,
            properties = emptyMap(),
            modifiers = extractModifiers(props)
        )
    }

    // ========== Display Components ==========

    private fun createText(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val content = props["content"]?.jsonPrimitive?.contentOrNull
            ?: props["value"]?.jsonPrimitive?.contentOrNull
            ?: props["text"]?.jsonPrimitive?.contentOrNull
            ?: ""

        val modifiers = mutableListOf<SwiftUIModifier>()

        // Font size
        props["fontSize"]?.jsonPrimitive?.floatOrNull?.let {
            modifiers.add(SwiftUIModifier.fontSize(it))
        }

        // Font weight
        props["fontWeight"]?.jsonPrimitive?.contentOrNull?.let { weight ->
            val fontWeight = when (weight) {
                "thin" -> FontWeight.Thin
                "light" -> FontWeight.Light
                "regular" -> FontWeight.Regular
                "medium" -> FontWeight.Medium
                "semibold" -> FontWeight.Semibold
                "bold" -> FontWeight.Bold
                "heavy" -> FontWeight.Heavy
                "black" -> FontWeight.Black
                else -> FontWeight.Regular
            }
            modifiers.add(SwiftUIModifier.fontWeight(fontWeight))
        }

        // Color
        props["color"]?.jsonPrimitive?.contentOrNull?.let { colorStr ->
            val color = parseColor(colorStr)
            modifiers.add(SwiftUIModifier.foregroundColor(color))
        }

        // Add common modifiers
        modifiers.addAll(extractModifiers(props))

        return SwiftUIView.text(content, modifiers)
    }

    private fun createIcon(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val iconName = props["name"]?.jsonPrimitive?.contentOrNull ?: "questionmark.circle"
        val size = props["size"]?.jsonPrimitive?.floatOrNull ?: 24f

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.fontSize(size))

        props["color"]?.jsonPrimitive?.contentOrNull?.let { colorStr ->
            modifiers.add(SwiftUIModifier.foregroundColor(parseColor(colorStr)))
        }

        modifiers.addAll(extractModifiers(props))

        return SwiftUIView(
            type = ViewType.Image,
            id = id,
            properties = mapOf(
                "systemName" to iconName
            ),
            modifiers = modifiers
        )
    }

    private fun createImage(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val url = props["url"]?.jsonPrimitive?.contentOrNull
        val assetName = props["asset"]?.jsonPrimitive?.contentOrNull

        val modifiers = extractModifiers(props)

        return when {
            url != null -> SwiftUIView(
                type = ViewType.AsyncImage,
                id = id,
                properties = mapOf("url" to url),
                modifiers = modifiers
            )
            assetName != null -> SwiftUIView(
                type = ViewType.Image,
                id = id,
                properties = mapOf("name" to assetName),
                modifiers = modifiers
            )
            else -> SwiftUIView(
                type = ViewType.Image,
                id = id,
                properties = mapOf("systemName" to "photo"),
                modifiers = modifiers
            )
        }
    }

    private fun createDivider(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Divider,
            id = id,
            properties = emptyMap(),
            modifiers = extractModifiers(props)
        )
    }

    private fun createAvatar(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val size = props["size"]?.jsonPrimitive?.floatOrNull ?: 40f
        val imageName = props["image"]?.jsonPrimitive?.contentOrNull ?: "person.circle.fill"

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(size),
            height = SizeValue.Fixed(size)
        ))
        modifiers.add(SwiftUIModifier.cornerRadius(size / 2))
        modifiers.addAll(extractModifiers(props))

        return SwiftUIView(
            type = ViewType.Image,
            id = id,
            properties = mapOf("systemName" to imageName),
            modifiers = modifiers
        )
    }

    private fun createBadge(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val content = props["value"]?.jsonPrimitive?.contentOrNull ?: "0"

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.padding(4f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.red))
        modifiers.add(SwiftUIModifier.foregroundColor(SwiftUIColor.white))
        modifiers.add(SwiftUIModifier.cornerRadius(8f))
        modifiers.add(SwiftUIModifier.fontSize(12f))
        modifiers.addAll(extractModifiers(props))

        return SwiftUIView.text(content, modifiers)
    }

    // ========== Input Components ==========

    private fun createButton(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val label = props["label"]?.jsonPrimitive?.contentOrNull
            ?: props["text"]?.jsonPrimitive?.contentOrNull
            ?: "Button"
        val action = props["action"]?.jsonPrimitive?.contentOrNull
            ?: props["onClick"]?.jsonPrimitive?.contentOrNull

        return SwiftUIView.button(
            label = label,
            action = action,
            modifiers = extractModifiers(props)
        )
    }

    private fun createTextField(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val placeholder = props["placeholder"]?.jsonPrimitive?.contentOrNull ?: ""
        val value = props["value"]?.jsonPrimitive?.contentOrNull ?: ""

        return SwiftUIView(
            type = ViewType.TextField,
            id = id,
            properties = mapOf(
                "placeholder" to placeholder,
                "text" to value
            ),
            modifiers = extractModifiers(props)
        )
    }

    private fun createToggle(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val label = props["label"]?.jsonPrimitive?.contentOrNull ?: ""
        val isOn = props["value"]?.jsonPrimitive?.booleanOrNull ?: false

        return SwiftUIView(
            type = ViewType.Toggle,
            id = id,
            properties = mapOf(
                "label" to label,
                "isOn" to isOn
            ),
            modifiers = extractModifiers(props)
        )
    }

    private fun createSlider(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val value = props["value"]?.jsonPrimitive?.floatOrNull ?: 0f
        val min = props["min"]?.jsonPrimitive?.floatOrNull ?: 0f
        val max = props["max"]?.jsonPrimitive?.floatOrNull ?: 100f

        return SwiftUIView(
            type = ViewType.Slider,
            id = id,
            properties = mapOf(
                "value" to value,
                "min" to min,
                "max" to max
            ),
            modifiers = extractModifiers(props)
        )
    }

    private fun createDatePicker(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Date"

        return SwiftUIView(
            type = ViewType.DatePicker,
            id = id,
            properties = mapOf("label" to label),
            modifiers = extractModifiers(props)
        )
    }

    private fun createDropdown(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Select"

        return SwiftUIView(
            type = ViewType.Picker,
            id = id,
            properties = mapOf("label" to label),
            modifiers = extractModifiers(props)
        )
    }

    // ========== Feedback Components ==========

    private fun createAlert(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val title = props["title"]?.jsonPrimitive?.contentOrNull ?: ""
        val message = props["message"]?.jsonPrimitive?.contentOrNull ?: ""

        return SwiftUIView(
            type = ViewType.Alert,
            id = id,
            properties = mapOf(
                "title" to title,
                "message" to message
            ),
            modifiers = extractModifiers(props)
        )
    }

    private fun createToast(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val message = props["message"]?.jsonPrimitive?.contentOrNull ?: ""

        val modifiers = mutableListOf<SwiftUIModifier>()
        modifiers.add(SwiftUIModifier.padding(12f))
        modifiers.add(SwiftUIModifier.background(SwiftUIColor.systemGray))
        modifiers.add(SwiftUIModifier.cornerRadius(8f))
        modifiers.addAll(extractModifiers(props))

        return SwiftUIView.text(message, modifiers)
    }

    private fun createSpinner(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.ProgressView,
            id = id,
            properties = emptyMap(),
            modifiers = extractModifiers(props)
        )
    }

    private fun createProgressBar(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val value = props["value"]?.jsonPrimitive?.floatOrNull ?: 0f

        return SwiftUIView(
            type = ViewType.ProgressView,
            id = id,
            properties = mapOf("value" to value),
            modifiers = extractModifiers(props)
        )
    }

    private fun createTooltip(props: Map<String, JsonElement>, id: String?): SwiftUIView {
        val text = props["text"]?.jsonPrimitive?.contentOrNull ?: ""

        return SwiftUIView.text(text, extractModifiers(props))
    }

    // ========== Unknown Component ==========

    private fun createUnknown(type: String, props: Map<String, JsonElement>, id: String?): SwiftUIView {
        return SwiftUIView(
            type = ViewType.Text,
            id = id,
            properties = mapOf("text" to "[Unknown: $type]"),
            modifiers = listOf(SwiftUIModifier.foregroundColor(SwiftUIColor.red))
        )
    }

    // ========== Helper Functions ==========

    /**
     * Extract common modifiers from props
     */
    private fun extractModifiers(props: Map<String, JsonElement>): List<SwiftUIModifier> {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Padding
        props["padding"]?.jsonPrimitive?.floatOrNull?.let {
            modifiers.add(SwiftUIModifier.padding(it))
        }

        // Background
        props["backgroundColor"]?.jsonPrimitive?.contentOrNull?.let { colorStr ->
            modifiers.add(SwiftUIModifier.background(parseColor(colorStr)))
        }

        // Corner radius
        props["cornerRadius"]?.jsonPrimitive?.floatOrNull?.let {
            modifiers.add(SwiftUIModifier.cornerRadius(it))
        }

        // Opacity
        props["opacity"]?.jsonPrimitive?.floatOrNull?.let {
            modifiers.add(SwiftUIModifier.opacity(it))
        }

        // Width/Height
        val width = props["width"]?.jsonPrimitive?.floatOrNull
        val height = props["height"]?.jsonPrimitive?.floatOrNull
        if (width != null || height != null) {
            modifiers.add(SwiftUIModifier.frame(
                width = width?.let { SizeValue.Fixed(it) },
                height = height?.let { SizeValue.Fixed(it) }
            ))
        }

        return modifiers
    }

    /**
     * Parse color string to SwiftUIColor
     */
    private fun parseColor(colorStr: String): SwiftUIColor {
        return when {
            colorStr.startsWith("#") -> SwiftUIColor.hex(colorStr)
            colorStr.startsWith("rgb(") -> {
                // Parse rgb(r, g, b) format
                val values = colorStr.removePrefix("rgb(").removeSuffix(")")
                    .split(",").map { it.trim().toIntOrNull() ?: 0 }
                if (values.size >= 3) {
                    SwiftUIColor.rgba(values[0], values[1], values[2])
                } else {
                    SwiftUIColor.primary
                }
            }
            else -> SwiftUIColor.system(colorStr)
        }
    }
}

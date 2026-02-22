/**
 * YamlComponentParser.kt - YAML parser for component definitions
 *
 * Simple KMP-compatible YAML parser for parsing component definition files.
 * Converts YAML structure to ComponentDefinition instances.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 * Refactored: 2026-01-08 (SRP extraction from ComponentFactory.kt)
 */
package com.augmentalis.voiceoscore

/**
 * Simple YAML parser for component definitions.
 *
 * This is a basic KMP-compatible parser. For production use,
 * consider integrating a full YAML library like kaml.
 */
class YamlComponentParser {

    /**
     * Parse YAML string to ComponentDefinition.
     */
    fun parse(yaml: String): ComponentDefinition {
        val map = parseYamlToMap(yaml)
        return parseComponentDefinition(map)
    }

    /**
     * Parse YAML string to nested Map structure.
     *
     * This is a simplified parser - for complex YAML, use a proper library.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseYamlToMap(yaml: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val lines = yaml.lines()
        var currentSection: String? = null
        var currentIndent = 0
        val sectionStack = mutableListOf<Pair<String, MutableMap<String, Any>>>()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed == "---") {
                continue
            }

            val indent = line.takeWhile { it == ' ' }.length

            // Parse key-value
            if (trimmed.contains(':')) {
                val colonIndex = trimmed.indexOf(':')
                val key = trimmed.substring(0, colonIndex).trim()
                val value = trimmed.substring(colonIndex + 1).trim()

                if (value.isEmpty()) {
                    // Start of new section
                    if (indent == 0) {
                        currentSection = key
                        result[key] = mutableMapOf<String, Any>()
                        sectionStack.clear()
                        sectionStack.add(key to (result[key] as MutableMap<String, Any>))
                    } else {
                        // Nested section
                        while (sectionStack.isNotEmpty() && indent <= currentIndent) {
                            sectionStack.removeLastOrNull()
                            currentIndent -= 2
                        }
                        val parent = sectionStack.lastOrNull()?.second ?: result
                        val newSection = mutableMapOf<String, Any>()
                        parent[key] = newSection
                        sectionStack.add(key to newSection)
                    }
                    currentIndent = indent
                } else {
                    // Key with value
                    val currentMap = sectionStack.lastOrNull()?.second ?: result
                    currentMap[key] = parseValue(value)
                }
            }
        }

        return result
    }

    /**
     * Parse a YAML value string to appropriate type.
     */
    private fun parseValue(value: String): Any {
        val trimmed = value.trim()

        // Remove quotes
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        // Boolean
        if (trimmed.lowercase() == "true") return true
        if (trimmed.lowercase() == "false") return false

        // Number
        trimmed.toIntOrNull()?.let { return it }
        trimmed.toDoubleOrNull()?.let { return it }

        // List (simple inline)
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length - 1)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        return trimmed
    }

    /**
     * Parse Map to ComponentDefinition.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseComponentDefinition(map: Map<String, Any>): ComponentDefinition {
        val componentMap = map["component"] as? Map<String, Any> ?: emptyMap()
        val themeMap = map["theme"] as? Map<String, Any>
        val layoutMap = map["layout"] as? Map<String, Any> ?: emptyMap()
        val dataMap = map["data"] as? Map<String, Any> ?: emptyMap()
        val functionsMap = map["functions"] as? Map<String, Any> ?: emptyMap()
        val statesList = map["states"] as? List<Map<String, Any>> ?: emptyList()
        val animationsMap = map["animations"] as? Map<String, Any> ?: emptyMap()
        val accessibilityMap = map["accessibility"] as? Map<String, Any>
        val modesMap = map["modes"] as? Map<String, Any> ?: emptyMap()

        return ComponentDefinition(
            component = parseComponentMetadata(componentMap),
            theme = themeMap?.let { parseThemeConfig(it) },
            layout = parseLayoutDefinition(layoutMap),
            data = parseDataBindings(dataMap),
            functions = parseFunctions(functionsMap),
            states = parseStates(statesList),
            animations = parseAnimations(animationsMap),
            accessibility = accessibilityMap?.let { parseAccessibilityConfig(it) },
            modes = parseModes(modesMap)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseComponentMetadata(map: Map<String, Any>): ComponentMetadata {
        val name = map["name"]?.toString() ?: "Unknown"
        val typeStr = map["type"]?.toString() ?: "widget"
        val platform = map["platform"]?.toString() ?: "all"
        val description = map["description"]?.toString() ?: ""

        val type = when (typeStr.lowercase()) {
            "overlay" -> ComponentType.OVERLAY
            "screen" -> ComponentType.SCREEN
            "dialog" -> ComponentType.DIALOG
            else -> ComponentType.WIDGET
        }

        return ComponentMetadata(name, type, platform, description)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseThemeConfig(map: Map<String, Any>): ThemeConfig {
        val inherit = map["inherit"]?.toString() ?: "VoiceOSCoreNGTheme"
        val tokens = (map["tokens"] as? Map<String, Any>)
            ?.mapValues { it.value.toString() }
            ?: emptyMap()

        return ThemeConfig(inherit, tokens)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLayoutDefinition(map: Map<String, Any>): LayoutDefinition {
        val typeStr = map["type"]?.toString() ?: "stack"
        val id = map["id"]?.toString() ?: ""
        val propsMap = map["props"] as? Map<String, Any> ?: emptyMap()
        val childrenList = map["children"] as? List<Map<String, Any>> ?: emptyList()
        val templateMap = map["template"] as? Map<String, Any>

        val type = when (typeStr.lowercase()) {
            "stack" -> LayoutType.STACK
            "column" -> LayoutType.COLUMN
            "row" -> LayoutType.ROW
            "box" -> LayoutType.BOX
            "absolute" -> LayoutType.ABSOLUTE
            else -> LayoutType.STACK
        }

        return LayoutDefinition(
            type = type,
            id = id,
            props = parseWidgetProps(propsMap),
            children = childrenList.map { parseWidgetDefinition(it) },
            template = templateMap?.let { parseTemplate(it) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWidgetDefinition(map: Map<String, Any>): WidgetDefinition {
        val widgetStr = map["widget"]?.toString() ?: "Container"
        val id = map["id"]?.toString() ?: ""
        val condition = map["condition"]?.toString()
        val propsMap = map["props"] as? Map<String, Any> ?: emptyMap()
        val childrenList = map["children"] as? List<Map<String, Any>> ?: emptyList()
        val positionMap = map["position"] as? Map<String, Any>
        val accessibilityMap = map["accessibility"] as? Map<String, Any>

        return WidgetDefinition(
            widget = WidgetType.fromString(widgetStr),
            id = id,
            condition = condition,
            props = parseWidgetProps(propsMap),
            children = childrenList.map { parseWidgetDefinition(it) },
            position = positionMap?.let { parsePosition(it) },
            accessibility = accessibilityMap?.let { parseWidgetAccessibility(it) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseWidgetProps(map: Map<String, Any>): WidgetProps {
        return WidgetProps(
            width = map["width"]?.let { DimensionValue(it.toString()) },
            height = map["height"]?.let { DimensionValue(it.toString()) },
            minWidth = map["minWidth"]?.let { DimensionValue(it.toString()) },
            maxWidth = map["maxWidth"]?.let { DimensionValue(it.toString()) },
            minHeight = map["minHeight"]?.let { DimensionValue(it.toString()) },
            maxHeight = map["maxHeight"]?.let { DimensionValue(it.toString()) },
            fillMaxWidth = map["fillMaxWidth"] as? Boolean ?: false,
            fillMaxHeight = map["fillMaxHeight"] as? Boolean ?: false,
            fillMaxSize = map["fillMaxSize"] as? Boolean ?: false,
            weight = (map["weight"] as? Number)?.toFloat(),
            padding = PaddingValue.fromYaml(map["padding"]),
            margin = PaddingValue.fromYaml(map["margin"]),
            spacing = map["spacing"]?.toString(),
            background = map["background"]?.toString(),
            cornerRadius = map["cornerRadius"]?.toString(),
            elevation = map["elevation"]?.toString(),
            shadowColor = map["shadowColor"]?.toString(),
            shadowRadius = map["shadowRadius"]?.toString(),
            shadowOffsetY = map["shadowOffsetY"]?.toString(),
            borderWidth = map["borderWidth"]?.toString(),
            borderColor = map["borderColor"]?.toString(),
            shape = map["shape"]?.toString(),
            clipToBounds = map["clipToBounds"] as? Boolean,
            opacity = (map["opacity"] as? Number)?.toFloat(),
            alignment = map["alignment"]?.toString(),
            horizontalAlignment = map["horizontalAlignment"]?.toString(),
            verticalAlignment = map["verticalAlignment"]?.toString(),
            text = map["text"]?.toString(),
            color = map["color"]?.toString(),
            fontSize = map["fontSize"]?.toString(),
            fontWeight = map["fontWeight"]?.toString(),
            textAlign = map["textAlign"]?.toString(),
            maxLines = (map["maxLines"] as? Number)?.toInt(),
            style = map["style"]?.toString(),
            icon = map["icon"]?.toString(),
            size = map["size"]?.toString(),
            animated = map["animated"] as? Boolean,
            progress = map["progress"]?.toString(),
            backgroundColor = map["backgroundColor"]?.toString(),
            progressColor = map["progressColor"]?.toString(),
            state = map["state"]?.toString(),
            number = map["number"]?.toString(),
            label = map["label"]?.toString(),
            showLabel = map["showLabel"] as? Boolean,
            value = map["value"]?.toString(),
            duration = (map["duration"] as? Number)?.toInt(),
            repeat = map["repeat"]?.toString(),
            extra = map.filterKeys { key ->
                key !in listOf(
                    "width", "height", "minWidth", "maxWidth", "minHeight", "maxHeight",
                    "fillMaxWidth", "fillMaxHeight", "fillMaxSize", "weight",
                    "padding", "margin", "spacing", "background", "cornerRadius",
                    "elevation", "shadowColor", "shadowRadius", "shadowOffsetY",
                    "borderWidth", "borderColor", "shape", "clipToBounds", "opacity",
                    "alignment", "horizontalAlignment", "verticalAlignment",
                    "text", "color", "fontSize", "fontWeight", "textAlign", "maxLines", "style",
                    "icon", "size", "animated", "progress", "backgroundColor", "progressColor",
                    "state", "number", "label", "showLabel", "value", "duration", "repeat"
                )
            }
        )
    }

    private fun parsePosition(map: Map<String, Any>): PositionDefinition {
        return PositionDefinition(
            x = map["x"]?.toString(),
            y = map["y"]?.toString(),
            offsetX = map["offsetX"]?.toString(),
            offsetY = map["offsetY"]?.toString()
        )
    }

    private fun parseWidgetAccessibility(map: Map<String, Any>): WidgetAccessibility {
        return WidgetAccessibility(
            role = map["role"]?.toString(),
            contentDescription = map["contentDescription"]?.toString(),
            liveRegion = map["liveRegion"]?.toString(),
            enabled = map["enabled"]?.toString(),
            minTouchTarget = map["minTouchTarget"]?.toString()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTemplate(map: Map<String, Any>): TemplateDefinition {
        val forEach = map["forEach"]?.toString() ?: ""
        val asVar = map["as"]?.toString() ?: "item"
        val renderList = map["render"] as? List<Map<String, Any>> ?: emptyList()

        return TemplateDefinition(
            forEach = forEach,
            `as` = asVar,
            render = renderList.map { parseWidgetDefinition(it) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDataBindings(map: Map<String, Any>): Map<String, DataBinding> {
        return map.mapValues { (_, value) ->
            when (value) {
                is Map<*, *> -> {
                    val bindingMap = value as Map<String, Any>
                    DataBinding(
                        type = bindingMap["type"]?.toString() ?: "Any",
                        required = bindingMap["required"] as? Boolean ?: false,
                        default = bindingMap["default"],
                        enum = (bindingMap["enum"] as? List<*>)?.map { it.toString() },
                        description = bindingMap["description"]?.toString() ?: "",
                        computed = bindingMap["computed"]?.toString(),
                        min = bindingMap["min"] as? Number,
                        max = bindingMap["max"] as? Number
                    )
                }
                else -> DataBinding(type = "Any", default = value)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFunctions(map: Map<String, Any>): Map<String, FunctionDefinition> {
        return map.mapValues { (_, value) ->
            val funcMap = value as? Map<String, Any> ?: emptyMap()
            FunctionDefinition(
                params = (funcMap["params"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                returns = funcMap["returns"]?.toString() ?: "Any",
                logic = funcMap["logic"]?.toString() ?: ""
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStates(list: List<Map<String, Any>>): List<StateDefinition> {
        return list.map { map ->
            StateDefinition(
                name = map["name"]?.toString() ?: "",
                description = map["description"]?.toString() ?: "",
                props = map["props"] as? Map<String, Any> ?: emptyMap()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAnimations(map: Map<String, Any>): Map<String, AnimationDefinition> {
        return map.filterValues { it is Map<*, *> }.mapValues { (_, value) ->
            val animMap = value as Map<String, Any>
            val propsMap = animMap["properties"] as? Map<String, Any> ?: emptyMap()

            AnimationDefinition(
                duration = animMap["duration"]?.toString() ?: "200",
                easing = animMap["easing"]?.toString(),
                repeat = animMap["repeat"]?.toString(),
                staggerDelay = (animMap["staggerDelay"] as? Number)?.toInt(),
                properties = propsMap.mapValues { (_, propValue) ->
                    val propMap = propValue as? Map<String, Any> ?: emptyMap()
                    AnimationProperty(
                        from = propMap["from"],
                        to = propMap["to"],
                        keyframes = propMap["keyframes"] as? List<Any>,
                        transition = propMap["transition"] as? Boolean
                    )
                }
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAccessibilityConfig(map: Map<String, Any>): AccessibilityConfig {
        val navMap = map["navigation"] as? Map<String, Any>

        return AccessibilityConfig(
            role = map["role"]?.toString(),
            liveRegion = map["liveRegion"]?.toString(),
            contentDescription = map["contentDescription"]?.toString(),
            announcements = (map["announcements"] as? Map<String, Any>)
                ?.mapValues { it.value.toString() }
                ?: emptyMap(),
            navigation = navMap?.let {
                AccessibilityNavigation(
                    supportsFocus = it["supportsFocus"] as? Boolean ?: true,
                    trapFocus = it["trapFocus"] as? Boolean ?: false,
                    focusOrder = it["focusOrder"]?.toString() ?: "sequential",
                    focusIndicatorWidth = it["focusIndicatorWidth"]?.toString(),
                    focusIndicatorColor = it["focusIndicatorColor"]?.toString()
                )
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseModes(map: Map<String, Any>): Map<String, ModeConfig> {
        return map.mapValues { (_, value) ->
            val modeMap = value as? Map<String, Any> ?: emptyMap()
            val animMap = modeMap["animations"] as? Map<String, Any>

            ModeConfig(
                maxItems = (modeMap["maxItems"] as? Number)?.toInt(),
                showInstructions = modeMap["showInstructions"] as? Boolean,
                badgeStyle = modeMap["badgeStyle"]?.toString(),
                animations = animMap?.let {
                    AnimationModeConfig(enabled = it["enabled"] as? Boolean ?: true)
                },
                enableDebugInfo = modeMap["enableDebugInfo"] as? Boolean,
                showConfidence = modeMap["showConfidence"] as? Boolean,
                autoDismissDelay = (modeMap["autoDismissDelay"] as? Number)?.toInt(),
                showTimestamp = modeMap["showTimestamp"] as? Boolean
            )
        }
    }
}

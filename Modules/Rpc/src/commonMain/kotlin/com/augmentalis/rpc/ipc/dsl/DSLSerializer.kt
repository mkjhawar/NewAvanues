package com.augmentalis.rpc.ipc.dsl
import com.augmentalis.rpc.ipc.currentTimeMillis

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DSL Serializer for IPC UI Component Transfer
 *
 * Provides efficient serialization of UI component trees for inter-process
 * communication. Optimized for the Avanues/AVAMagic ecosystem with:
 *
 * - Ultracompact DSL format (60%+ smaller than JSON)
 * - 3-letter type aliases for minimal size
 * - Component tree serialization
 * - Event callback serialization
 * - Theme token embedding
 * - Incremental updates (delta encoding)
 *
 * Performance Targets:
 * - Serialization: <1ms for typical UI tree
 * - Deserialization: <1ms for typical UI tree
 * - Size reduction: 50-65% smaller than equivalent JSON
 *
 * Compact Format Example:
 * ```
 * Col#main{spacing:16;@p(16),bg(#FFFFFF);Text{text:"Hello"};Btn#b1{label:"Click";@onClick->h}}
 * ```
 *
 * Usage:
 * ```kotlin
 * val serializer = DSLSerializer()
 *
 * // Serialize component tree
 * val dsl = serializer.serialize(component)
 *
 * // Deserialize back
 * val component = serializer.deserialize(dsl)
 *
 * // Create IPC message
 * val message = serializer.toIPCMessage(
 *     component = component,
 *     targetAppId = "com.avanue.renderer"
 * )
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-11-19
 * IDEACODE Version: 8.4
 */
class DSLSerializer {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    // Readable type aliases for compact format
    private val typeToAlias = mapOf(
        // Layout - short but readable
        "Column" to "Col", "Row" to "Row", "Box" to "Box", "Stack" to "Stack",
        "Container" to "Cont", "Card" to "Card", "Surface" to "Surf",
        "Scaffold" to "Scaffold", "ScrollView" to "Scroll", "LazyColumn" to "LazyCol",
        "LazyRow" to "LazyRow", "Grid" to "Grid", "Spacer" to "Spacer", "Divider" to "Div",

        // Basic - recognizable
        "Text" to "Text", "Button" to "Btn", "TextField" to "Field", "Image" to "Img",
        "Icon" to "Icon", "Checkbox" to "Check", "Switch" to "Switch", "Radio" to "Radio",

        // Input
        "Slider" to "Slider", "Dropdown" to "Drop", "DatePicker" to "DatePick",
        "TimePicker" to "TimePick", "SearchBar" to "Search", "Rating" to "Rating",
        "Stepper" to "Stepper", "Toggle" to "Toggle", "ColorPicker" to "ColorPick",

        // Display
        "Avatar" to "Avatar", "Badge" to "Badge", "Chip" to "Chip", "Tooltip" to "Tip",
        "ProgressBar" to "Progress", "ProgressCircle" to "Spinner", "Spinner" to "Spin",
        "Skeleton" to "Skel", "StatCard" to "Stat",

        // Navigation
        "AppBar" to "AppBar", "BottomNav" to "BotNav", "TabBar" to "Tabs", "Drawer" to "Drawer",
        "NavigationRail" to "NavRail", "Breadcrumb" to "Crumb", "Pagination" to "Page",

        // Feedback
        "Alert" to "Alert", "Toast" to "Toast", "Snackbar" to "Snack", "Modal" to "Modal",
        "Dialog" to "Dialog", "BottomSheet" to "Sheet", "Banner" to "Banner",

        // Data
        "ListTile" to "Tile", "Accordion" to "Accord", "Timeline" to "Timeline",
        "DataGrid" to "DataGrid", "Table" to "Table", "TreeView" to "Tree",

        // Button variants
        "TextButton" to "TextBtn", "OutlinedButton" to "OutBtn", "FilledButton" to "FillBtn",
        "IconButton" to "IconBtn", "FAB" to "FAB", "SegmentedButton" to "SegBtn"
    )

    private val aliasToType = typeToAlias.entries.associate { it.value to it.key }

    // Readable property keys - keep full names for clarity
    private val propToShort = mapOf(
        "text" to "text", "label" to "label", "value" to "value", "title" to "title",
        "subtitle" to "subtitle", "description" to "desc", "placeholder" to "placeholder",
        "icon" to "icon", "src" to "src", "checked" to "checked", "selected" to "selected",
        "enabled" to "enabled", "visible" to "visible", "spacing" to "spacing", "alignment" to "align",
        "color" to "color", "backgroundColor" to "bgColor", "width" to "width", "height" to "height",
        "padding" to "padding", "margin" to "margin", "fontSize" to "fontSize", "fontWeight" to "fontWeight",
        "maxLines" to "maxLines", "min" to "min", "max" to "max", "step" to "step"
    )

    private val shortToProp = propToShort.entries.associate { it.value to it.key }

    /**
     * Serialize a UI component to compact DSL format
     */
    fun serialize(component: UIComponent): String {
        return serializeComponent(component, 0)
    }

    /**
     * Serialize an entire UI tree with metadata
     */
    fun serializeTree(tree: UITree): String {
        val builder = StringBuilder()

        // Header
        builder.append("@avaui/${tree.version}\n")
        builder.append("id:${tree.id}\n")
        builder.append("name:${tree.name}\n")

        // Theme reference
        tree.theme?.let {
            builder.append("theme:$it\n")
        }

        builder.append("\n")

        // Components
        tree.root?.let {
            builder.append(serializeComponent(it, 0))
        }

        return builder.toString()
    }

    /**
     * Deserialize to full UI tree with metadata
     */
    fun deserializeTree(dsl: String): UITree {
        val lines = dsl.lines()
        var id = ""
        var name = ""
        var version = "1.0"
        var theme: String? = null
        var lineIndex = 0

        // Parse header
        while (lineIndex < lines.size) {
            val line = lines[lineIndex].trim()
            when {
                line.startsWith("@avaui/") -> {
                    version = line.substringAfter("@avaui/")
                }
                line.startsWith("id:") -> {
                    id = line.substringAfter("id:")
                }
                line.startsWith("name:") -> {
                    name = line.substringAfter("name:")
                }
                line.startsWith("theme:") -> {
                    theme = line.substringAfter("theme:")
                }
                line.isEmpty() -> {
                    lineIndex++
                    break
                }
            }
            lineIndex++
        }

        // Parse root component
        val remainingLines = lines.drop(lineIndex)
        val root = parseComponent(remainingLines, 0).first

        return UITree(
            id = id,
            name = name,
            version = version,
            theme = theme,
            root = root
        )
    }

    /**
     * Create IPC message from component
     */
    fun toIPCMessage(
        component: UIComponent,
        targetAppId: String,
        action: String = "ui.render"
    ): IPCUIMessage {
        return IPCUIMessage(
            targetAppId = targetAppId,
            action = action,
            payload = serialize(component),
            format = "dsl",
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create IPC message for incremental update
     */
    fun toUpdateMessage(
        componentId: String,
        updates: Map<String, Any>,
        targetAppId: String
    ): IPCUIMessage {
        val payload = buildString {
            append("@update $componentId\n")
            updates.forEach { (key, value) ->
                append("$key:${serializeValue(value)}\n")
            }
        }

        return IPCUIMessage(
            targetAppId = targetAppId,
            action = "ui.update",
            payload = payload,
            format = "dsl-delta",
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create IPC message for event
     */
    fun toEventMessage(
        componentId: String,
        eventType: String,
        eventData: Map<String, Any>,
        targetAppId: String
    ): IPCUIMessage {
        val payload = buildString {
            append("@event $componentId.$eventType\n")
            eventData.forEach { (key, value) ->
                append("$key:${serializeValue(value)}\n")
            }
        }

        return IPCUIMessage(
            targetAppId = targetAppId,
            action = "ui.event",
            payload = payload,
            format = "dsl-event",
            timestamp = currentTimeMillis()
        )
    }

    // Internal serialization helpers

    private fun serializeComponent(component: UIComponent, indent: Int): String {
        val builder = StringBuilder()

        // Component type alias and optional ID (no whitespace)
        val typeAlias = typeToAlias[component.type] ?: component.type
        builder.append(typeAlias)
        component.id?.let { builder.append("#$it") }
        builder.append("{")

        val parts = mutableListOf<String>()

        // Properties (short keys, semicolon separated)
        component.properties.forEach { (key, value) ->
            val shortKey = propToShort[key] ?: key
            parts.add("$shortKey:${serializeValue(value)}")
        }

        // Modifiers (compact format)
        if (component.modifiers.isNotEmpty()) {
            parts.add("@" + component.modifiers.joinToString(",") { serializeModifier(it) })
        }

        // Callbacks (short format)
        component.callbacks.forEach { (event, handler) ->
            parts.add("@$event->$handler")
        }

        builder.append(parts.joinToString(";"))

        // Children (nested, no newlines)
        if (component.children.isNotEmpty()) {
            if (parts.isNotEmpty()) builder.append(";")
            builder.append(component.children.joinToString(";") { serializeComponent(it, indent + 1) })
        }

        builder.append("}")
        return builder.toString()
    }

    private fun serializeValue(value: Any): String {
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> "[${value.joinToString(",") { serializeValue(it ?: "") }}]"
            is Map<*, *> -> {
                val entries = value.entries.joinToString(",") {
                    "${it.key}=${serializeValue(it.value ?: "")}"
                }
                "{$entries}"
            }
            else -> value.toString()
        }
    }

    private fun serializeModifier(modifier: UIModifier): String {
        return when (modifier) {
            is UIModifier.Padding -> "p(${modifier.all})"
            is UIModifier.PaddingSymmetric -> "p(${modifier.horizontal},${modifier.vertical})"
            is UIModifier.Background -> "bg(${modifier.color})"
            is UIModifier.ForegroundColor -> "fg(${modifier.color})"
            is UIModifier.CornerRadius -> "r(${modifier.radius})"
            is UIModifier.Frame -> "f(${modifier.width ?: "_"},${modifier.height ?: "_"})"
            is UIModifier.Shadow -> "sh(${modifier.radius})"
            is UIModifier.Opacity -> "op(${modifier.value})"
            is UIModifier.ClipShape -> "clip(${modifier.shape})"
        }
    }

    /**
     * Deserialize DSL format back to UIComponent
     */
    fun deserialize(dsl: String): UIComponent? {
        return parseUltracompact(dsl)
    }

    private fun parseUltracompact(dsl: String): UIComponent? {
        if (dsl.isBlank()) return null

        var pos = 0
        return parseComponentAt(dsl, pos).first
    }

    private fun parseComponentAt(dsl: String, startPos: Int): Pair<UIComponent?, Int> {
        var pos = startPos

        // Skip whitespace
        while (pos < dsl.length && dsl[pos].isWhitespace()) pos++
        if (pos >= dsl.length) return null to pos

        // Parse type alias
        val typeStart = pos
        while (pos < dsl.length && dsl[pos] != '#' && dsl[pos] != '{') pos++
        val typeAlias = dsl.substring(typeStart, pos).trim()
        val type = aliasToType[typeAlias] ?: typeAlias

        // Parse optional ID
        var id: String? = null
        if (pos < dsl.length && dsl[pos] == '#') {
            pos++
            val idStart = pos
            while (pos < dsl.length && dsl[pos] != '{') pos++
            id = dsl.substring(idStart, pos).trim()
        }

        // Expect opening brace
        if (pos >= dsl.length || dsl[pos] != '{') return null to pos
        pos++

        val properties = mutableMapOf<String, Any>()
        val modifiers = mutableListOf<UIModifier>()
        val callbacks = mutableMapOf<String, String>()
        val children = mutableListOf<UIComponent>()

        // Parse body
        while (pos < dsl.length && dsl[pos] != '}') {
            // Skip whitespace and semicolons
            while (pos < dsl.length && (dsl[pos].isWhitespace() || dsl[pos] == ';')) pos++
            if (pos >= dsl.length || dsl[pos] == '}') break

            // Check for nested component (starts with letter)
            if (dsl[pos].isLetter() && !dsl[pos].isLowerCase()) {
                val (child, newPos) = parseComponentAt(dsl, pos)
                child?.let { children.add(it) }
                pos = newPos
                continue
            }

            // Check for modifier/callback (starts with @)
            if (dsl[pos] == '@') {
                pos++
                val contentStart = pos
                var depth = 0
                while (pos < dsl.length) {
                    when (dsl[pos]) {
                        '(' -> depth++
                        ')' -> depth--
                        ';', '}' -> if (depth == 0) break
                    }
                    pos++
                }
                val content = dsl.substring(contentStart, pos)

                if (content.contains("->")) {
                    // Callback
                    val (event, handler) = content.split("->", limit = 2)
                    callbacks[event.trim()] = handler.trim()
                } else {
                    // Modifiers
                    modifiers.addAll(parseModifiers(content))
                }
                continue
            }

            // Parse property (key:value)
            val keyStart = pos
            while (pos < dsl.length && dsl[pos] != ':') pos++
            if (pos >= dsl.length) break
            val shortKey = dsl.substring(keyStart, pos).trim()
            val key = shortToProp[shortKey] ?: shortKey
            pos++ // skip :

            // Parse value
            val valueStart = pos
            var depth = 0
            var inString = false
            while (pos < dsl.length) {
                val c = dsl[pos]
                if (c == '"' && (pos == 0 || dsl[pos - 1] != '\\')) inString = !inString
                if (!inString) {
                    when (c) {
                        '{', '[' -> depth++
                        '}', ']' -> {
                            if (depth == 0) break
                            depth--
                        }
                        ';' -> if (depth == 0) break
                    }
                }
                pos++
            }
            val valueStr = dsl.substring(valueStart, pos).trim()
            properties[key] = parseValue(valueStr)
        }

        // Skip closing brace
        if (pos < dsl.length && dsl[pos] == '}') pos++

        return UIComponent(
            type = type,
            id = id,
            properties = properties,
            modifiers = modifiers,
            callbacks = callbacks,
            children = children
        ) to pos
    }

    // Legacy line-based parser (kept for backwards compatibility)
    private fun parseComponent(lines: List<String>, startIndent: Int): Pair<UIComponent?, Int> {
        // Join lines and use ultracompact parser
        val dsl = lines.joinToString("")
        return parseUltracompact(dsl) to lines.size
    }

    private fun parseValue(value: String): Any {
        return when {
            value.startsWith("\"") && value.endsWith("\"") ->
                value.drop(1).dropLast(1)
            value == "true" -> true
            value == "false" -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toFloatOrNull() != null -> value.toFloat()
            value.startsWith("[") && value.endsWith("]") -> {
                val items = value.drop(1).dropLast(1).split(",")
                items.map { parseValue(it.trim()) }
            }
            value.startsWith("{") && value.endsWith("}") -> {
                val entries = value.drop(1).dropLast(1).split(",")
                entries.associate { entry ->
                    val (k, v) = entry.split("=", limit = 2)
                    k.trim() to parseValue(v.trim())
                }
            }
            else -> value
        }
    }

    private fun parseModifiers(modString: String): List<UIModifier> {
        return modString.split(",").mapNotNull { mod ->
            val name = mod.substringBefore("(")
            val args = mod.substringAfter("(").substringBefore(")").split(",")

            when (name.trim()) {
                "p" -> {
                    if (args.size == 1) {
                        UIModifier.Padding(args[0].toFloatOrNull() ?: 0f)
                    } else {
                        UIModifier.PaddingSymmetric(
                            args[0].toFloatOrNull() ?: 0f,
                            args[1].toFloatOrNull() ?: 0f
                        )
                    }
                }
                "bg" -> UIModifier.Background(args[0].trim())
                "fg" -> UIModifier.ForegroundColor(args[0].trim())
                "r" -> UIModifier.CornerRadius(args[0].toFloatOrNull() ?: 0f)
                "f" -> UIModifier.Frame(
                    args.getOrNull(0)?.takeIf { it != "_" }?.toFloatOrNull(),
                    args.getOrNull(1)?.takeIf { it != "_" }?.toFloatOrNull()
                )
                "sh" -> UIModifier.Shadow(args[0].toFloatOrNull() ?: 0f)
                "op" -> UIModifier.Opacity(args[0].toFloatOrNull() ?: 1f)
                "clip" -> UIModifier.ClipShape(args[0].trim())
                else -> null
            }
        }
    }

    companion object {
        /**
         * Calculate size comparison between DSL and JSON formats
         */
        fun compareSizes(component: UIComponent): SizeComparison {
            val serializer = DSLSerializer()
            val dslSize = serializer.serialize(component).encodeToByteArray().size
            val jsonSize = Json.encodeToString(component).encodeToByteArray().size

            return SizeComparison(
                dslBytes = dslSize,
                jsonBytes = jsonSize,
                savings = jsonSize - dslSize,
                savingsPercent = ((jsonSize - dslSize).toFloat() / jsonSize * 100).toInt()
            )
        }
    }
}

/**
 * UI Component for serialization
 */
@Serializable
data class UIComponent(
    val type: String,
    val id: String? = null,
    val properties: Map<String, @Serializable(with = AnySerializer::class) Any> = emptyMap(),
    val modifiers: List<UIModifier> = emptyList(),
    val callbacks: Map<String, String> = emptyMap(),
    val children: List<UIComponent> = emptyList()
)

/**
 * UI Tree with metadata
 */
@Serializable
data class UITree(
    val id: String,
    val name: String,
    val version: String = "1.0",
    val theme: String? = null,
    val root: UIComponent? = null
)

/**
 * UI Modifier sealed class
 */
@Serializable
sealed class UIModifier {
    @Serializable
    data class Padding(val all: Float) : UIModifier()

    @Serializable
    data class PaddingSymmetric(val horizontal: Float, val vertical: Float) : UIModifier()

    @Serializable
    data class Background(val color: String) : UIModifier()

    @Serializable
    data class ForegroundColor(val color: String) : UIModifier()

    @Serializable
    data class CornerRadius(val radius: Float) : UIModifier()

    @Serializable
    data class Frame(val width: Float? = null, val height: Float? = null) : UIModifier()

    @Serializable
    data class Shadow(val radius: Float) : UIModifier()

    @Serializable
    data class Opacity(val value: Float) : UIModifier()

    @Serializable
    data class ClipShape(val shape: String) : UIModifier()
}

/**
 * IPC Message for UI transfer
 */
@Serializable
data class IPCUIMessage(
    val targetAppId: String,
    val action: String,
    val payload: String,
    val format: String,
    val timestamp: Long
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): IPCUIMessage = Json.decodeFromString(json)
    }
}

/**
 * Size comparison result
 */
data class SizeComparison(
    val dslBytes: Int,
    val jsonBytes: Int,
    val savings: Int,
    val savingsPercent: Int
)

/**
 * Custom serializer for Any type
 */
object AnySerializer : kotlinx.serialization.KSerializer<Any> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Any")

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Any) {
        val jsonEncoder = encoder as? kotlinx.serialization.json.JsonEncoder
            ?: throw kotlinx.serialization.SerializationException("Can only serialize to JSON")

        val element = when (value) {
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is List<*> -> kotlinx.serialization.json.JsonArray(value.map {
                when (it) {
                    is String -> kotlinx.serialization.json.JsonPrimitive(it)
                    is Number -> kotlinx.serialization.json.JsonPrimitive(it)
                    is Boolean -> kotlinx.serialization.json.JsonPrimitive(it)
                    else -> kotlinx.serialization.json.JsonPrimitive(it.toString())
                }
            })
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Any {
        val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder
            ?: throw kotlinx.serialization.SerializationException("Can only deserialize from JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content.contains(".") -> element.content.toFloat()
                    else -> element.content.toIntOrNull() ?: element.content
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.map {
                when (it) {
                    is kotlinx.serialization.json.JsonPrimitive -> it.content
                    else -> it.toString()
                }
            }
            else -> element.toString()
        }
    }
}

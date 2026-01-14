package com.augmentalis.magicelements.core.mel

import com.augmentalis.avaelements.core.PluginException

/**
 * Compact MEL Parser
 *
 * Parses the compact MEL format which combines:
 * - Compact DSL syntax for UI
 * - Shorthand function names ($add, $if, etc.)
 * - Inline reducer assignments
 *
 * Example:
 * ```
 * @mel/1.0
 * id:com.example.counter
 * tier:data
 *
 * state:
 *   count:0
 *
 * reducers:
 *   increment: count=$add($count,1)
 *
 * ui:Col{@p(16);Text{text:$str($count)}}
 * ```
 */
class CompactMELParser {

    companion object {
        /**
         * Parse compact MEL content and return a PluginRuntime
         */
        fun parse(content: String, platform: Platform? = null): PluginRuntime {
            val parser = CompactMELParser()
            val definition = parser.parseContent(content)
            return PluginRuntime.create(definition, platform)
        }

        /**
         * Shorthand to full function name mapping
         */
        val SHORTHAND_MAP = mapOf(
            // Math functions
            "add" to "math.add",
            "sub" to "math.subtract",
            "mul" to "math.multiply",
            "div" to "math.divide",
            "num" to "math.parse",
            "abs" to "math.abs",
            "round" to "math.round",
            "floor" to "math.floor",
            "ceil" to "math.ceil",
            "min" to "math.min",
            "max" to "math.max",
            "pow" to "math.pow",
            "sqrt" to "math.sqrt",
            "mod" to "math.mod",

            // Logic functions
            "if" to "logic.if",
            "eq" to "logic.equals",
            "ne" to "logic.notEquals",
            "gt" to "logic.gt",
            "lt" to "logic.lt",
            "gte" to "logic.gte",
            "lte" to "logic.lte",
            "and" to "logic.and",
            "or" to "logic.or",
            "not" to "logic.not",
            "switch" to "logic.switch",
            "coalesce" to "logic.coalesce",

            // String functions
            "cat" to "string.concat",
            "str" to "string.toString",
            "len" to "string.length",
            "slice" to "string.substring",
            "has" to "string.contains",
            "upper" to "string.upper",
            "lower" to "string.lower",
            "trim" to "string.trim",
            "split" to "string.split",
            "join" to "string.join",
            "replace" to "string.replace",
            "starts" to "string.startsWith",
            "ends" to "string.endsWith",
            "pad" to "string.padStart",
            "format" to "string.format",

            // Array functions
            "push" to "array.append",
            "pop" to "array.removeLast",
            "shift" to "array.removeFirst",
            "splice" to "array.splice",
            "at" to "array.get",
            "size" to "array.length",
            "map" to "array.map",
            "filter" to "array.filter",
            "find" to "array.find",
            "indexOf" to "array.indexOf",
            "includes" to "array.contains",
            "reverse" to "array.reverse",
            "sort" to "array.sort",
            "first" to "array.first",
            "last" to "array.last",

            // Object functions
            "get" to "object.get",
            "set" to "object.set",
            "keys" to "object.keys",
            "values" to "object.values",
            "merge" to "object.merge",
            "pick" to "object.pick",
            "omit" to "object.omit",

            // Date functions
            "now" to "date.now",
            "today" to "date.today",
            "formatDate" to "date.format",
            "parseDate" to "date.parse"
        )

        /**
         * Type alias mapping (compact -> full)
         */
        val TYPE_ALIAS_MAP = mapOf(
            "Col" to "Column",
            "Row" to "Row",
            "Box" to "Box",
            "Stack" to "Stack",
            "Cont" to "Container",
            "Card" to "Card",
            "Surf" to "Surface",
            "Scaffold" to "Scaffold",
            "Scroll" to "ScrollView",
            "LazyCol" to "LazyColumn",
            "LazyRow" to "LazyRow",
            "Grid" to "Grid",
            "Spacer" to "Spacer",
            "Div" to "Divider",
            "Text" to "Text",
            "Btn" to "Button",
            "Field" to "TextField",
            "Img" to "Image",
            "Icon" to "Icon",
            "Check" to "Checkbox",
            "Switch" to "Switch",
            "Radio" to "Radio",
            "Slider" to "Slider",
            "Drop" to "Dropdown",
            "DatePick" to "DatePicker",
            "TimePick" to "TimePicker",
            "Search" to "SearchBar",
            "Rating" to "Rating",
            "Stepper" to "Stepper",
            "Toggle" to "Toggle",
            "ColorPick" to "ColorPicker",
            "Avatar" to "Avatar",
            "Badge" to "Badge",
            "Chip" to "Chip",
            "Tip" to "Tooltip",
            "Progress" to "ProgressBar",
            "Spinner" to "ProgressCircle",
            "Spin" to "Spinner",
            "Skel" to "Skeleton",
            "Stat" to "StatCard",
            "AppBar" to "AppBar",
            "BotNav" to "BottomNav",
            "Tabs" to "TabBar",
            "Drawer" to "Drawer",
            "NavRail" to "NavigationRail",
            "Crumb" to "Breadcrumb",
            "Page" to "Pagination",
            "Alert" to "Alert",
            "Toast" to "Toast",
            "Snack" to "Snackbar",
            "Modal" to "Modal",
            "Dialog" to "Dialog",
            "Sheet" to "BottomSheet",
            "Banner" to "Banner",
            "Tile" to "ListTile",
            "Accord" to "Accordion",
            "Timeline" to "Timeline",
            "DataGrid" to "DataGrid",
            "Table" to "Table",
            "Tree" to "TreeView",
            "TextBtn" to "TextButton",
            "OutBtn" to "OutlinedButton",
            "FillBtn" to "FilledButton",
            "IconBtn" to "IconButton",
            "FAB" to "FloatingActionButton"
        )

        /**
         * Modifier name mapping
         */
        val MODIFIER_MAP = mapOf(
            "p" to "padding",
            "m" to "margin",
            "gap" to "gap",
            "f" to "frame",
            "bg" to "background",
            "fg" to "foreground",
            "r" to "cornerRadius",
            "sh" to "shadow",
            "op" to "opacity",
            "clip" to "clip",
            "border" to "border",
            "alignCenter" to "alignment.center",
            "alignStart" to "alignment.start",
            "alignEnd" to "alignment.end"
        )

        /**
         * Event mapping
         */
        val EVENT_MAP = mapOf(
            "tap" to "onTap",
            "click" to "onClick",
            "change" to "onChange",
            "submit" to "onSubmit",
            "focus" to "onFocus",
            "blur" to "onBlur",
            "longPress" to "onLongPress",
            "swipe" to "onSwipe"
        )
    }

    private var position = 0
    private var content = ""
    private var lines = listOf<String>()

    /**
     * Parse content and return PluginDefinition
     */
    fun parseContent(input: String): PluginDefinition {
        content = input
        lines = input.lines()
        position = 0

        val metadata = parseHeader()
        val state = parseState()
        val reducers = parseReducers()
        val ui = parseUI()

        return PluginDefinition(
            metadata = metadata,
            state = state,
            reducers = reducers,
            ui = ui,
            tier = metadata.tier
        )
    }

    /**
     * Parse header block
     */
    private fun parseHeader(): PluginMetadata {
        var id = "unknown"
        var name = "Unnamed Plugin"
        var version = "1.0.0"
        var tier = PluginTier.DATA
        var description = ""

        while (position < lines.size) {
            val line = lines[position].trim()

            when {
                line.startsWith("@mel/") -> {
                    // Version line, skip
                    position++
                }
                line.startsWith("id:") -> {
                    id = line.substringAfter("id:").trim()
                    position++
                }
                line.startsWith("name:") -> {
                    name = line.substringAfter("name:").trim()
                    position++
                }
                line.startsWith("version:") -> {
                    version = line.substringAfter("version:").trim()
                    position++
                }
                line.startsWith("tier:") -> {
                    val tierStr = line.substringAfter("tier:").trim().lowercase()
                    tier = when (tierStr) {
                        "data" -> PluginTier.DATA
                        "logic" -> PluginTier.LOGIC
                        else -> PluginTier.DATA
                    }
                    position++
                }
                line.startsWith("description:") -> {
                    description = line.substringAfter("description:").trim()
                    position++
                }
                line.startsWith("state:") || line.startsWith("reducers:") || line.startsWith("ui:") -> {
                    break
                }
                line.isEmpty() -> {
                    position++
                }
                else -> {
                    position++
                }
            }
        }

        return PluginMetadata(
            id = id,
            name = name,
            version = version,
            tier = tier,
            description = description
        )
    }

    /**
     * Parse state block
     */
    private fun parseState(): StateSchema {
        val properties = mutableMapOf<String, StateProperty>()

        // Find state: line
        while (position < lines.size && !lines[position].trim().startsWith("state:")) {
            position++
        }

        if (position >= lines.size) {
            return StateSchema(properties)
        }

        position++ // Skip "state:" line

        // Parse indented state entries
        while (position < lines.size) {
            val line = lines[position]
            val trimmed = line.trim()

            // Check for end of state block
            if (!line.startsWith("  ") && !line.startsWith("\t") && trimmed.isNotEmpty()) {
                if (trimmed.startsWith("reducers:") || trimmed.startsWith("ui:")) {
                    break
                }
            }

            if (trimmed.isEmpty()) {
                position++
                continue
            }

            // Parse state entry: name:value
            if (trimmed.contains(":")) {
                val colonIndex = trimmed.indexOf(':')
                val propName = trimmed.substring(0, colonIndex).trim()
                val valueStr = trimmed.substring(colonIndex + 1).trim()

                val (type, defaultValue) = parseValue(valueStr)
                properties[propName] = StateProperty(
                    name = propName,
                    type = type,
                    defaultValue = defaultValue
                )
            }

            position++
        }

        return StateSchema(properties)
    }

    /**
     * Parse reducers block
     */
    private fun parseReducers(): Map<String, Reducer> {
        val reducers = mutableMapOf<String, Reducer>()

        // Find reducers: line
        while (position < lines.size && !lines[position].trim().startsWith("reducers:")) {
            position++
        }

        if (position >= lines.size) {
            return reducers
        }

        position++ // Skip "reducers:" line

        // Parse reducer entries
        while (position < lines.size) {
            val line = lines[position]
            val trimmed = line.trim()

            // Check for end of reducers block
            if (!line.startsWith("  ") && !line.startsWith("\t") && trimmed.isNotEmpty()) {
                if (trimmed.startsWith("ui:")) {
                    break
                }
            }

            if (trimmed.isEmpty()) {
                position++
                continue
            }

            // Parse reducer: name(params): assignments
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val nameWithParams = trimmed.substring(0, colonIndex).trim()
                val assignmentsStr = trimmed.substring(colonIndex + 1).trim()

                // Extract name and params
                val parenIndex = nameWithParams.indexOf('(')
                val name: String
                val params: List<String>

                if (parenIndex > 0) {
                    name = nameWithParams.substring(0, parenIndex)
                    val paramsStr = nameWithParams.substring(parenIndex + 1, nameWithParams.lastIndexOf(')'))
                    params = paramsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    name = nameWithParams
                    params = emptyList()
                }

                // Parse assignments
                val assignments = parseAssignments(assignmentsStr)

                reducers[name] = Reducer(
                    params = params,
                    nextState = assignments.mapValues { (_, expr) -> Expression.of(expr) },
                    effects = null
                )
            }

            position++
        }

        return reducers
    }

    /**
     * Parse assignment expressions
     */
    private fun parseAssignments(assignmentsStr: String): Map<String, String> {
        val assignments = mutableMapOf<String, String>()

        // Split by semicolon, handling nested expressions
        val parts = splitBySemicolon(assignmentsStr)

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue

            val eqIndex = trimmed.indexOf('=')
            if (eqIndex > 0) {
                val key = trimmed.substring(0, eqIndex).trim()
                val value = trimmed.substring(eqIndex + 1).trim()
                // Expand shorthands in value
                assignments[key] = expandShorthands(value)
            }
        }

        return assignments
    }

    /**
     * Split by semicolon while respecting nested parens
     */
    private fun splitBySemicolon(str: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0

        for (c in str) {
            when {
                c == '(' || c == '[' || c == '{' -> {
                    depth++
                    current.append(c)
                }
                c == ')' || c == ']' || c == '}' -> {
                    depth--
                    current.append(c)
                }
                c == ';' && depth == 0 -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }

    /**
     * Expand shorthand function names to full names
     */
    private fun expandShorthands(expr: String): String {
        var result = expr

        // Find all $name( patterns and replace with $full.name(
        val regex = Regex("""\$([a-zA-Z]+)\(""")
        result = regex.replace(result) { match ->
            val shortName = match.groupValues[1]
            val fullName = SHORTHAND_MAP[shortName] ?: shortName
            "\$$fullName("
        }

        // Also expand $varName to $state.varName if not a function call
        val varRegex = Regex("""\$([a-zA-Z][a-zA-Z0-9_]*)(?!\()""")
        result = varRegex.replace(result) { match ->
            val varName = match.groupValues[1]
            // Check if it's a known shorthand (already expanded above)
            if (SHORTHAND_MAP.containsKey(varName)) {
                match.value
            } else {
                "\$state.$varName"
            }
        }

        return result
    }

    /**
     * Parse UI block
     */
    private fun parseUI(): UINode {
        // Find ui: line
        while (position < lines.size && !lines[position].trim().startsWith("ui:")) {
            position++
        }

        if (position >= lines.size) {
            throw PluginException.LoadException("No ui: block found in compact MEL")
        }

        // Get everything after "ui:"
        val startLine = lines[position].substringAfter("ui:").trim()

        // Collect remaining lines
        val uiContent = StringBuilder(startLine)
        position++

        while (position < lines.size) {
            uiContent.append(lines[position])
            position++
        }

        // Parse compact DSL
        return parseCompactDSL(uiContent.toString().trim())
    }

    /**
     * Parse compact DSL into UINode
     */
    private fun parseCompactDSL(dsl: String): UINode {
        val tokenizer = CompactDSLTokenizer(dsl)
        return parseComponent(tokenizer)
    }

    /**
     * Parse a single component
     */
    private fun parseComponent(tokenizer: CompactDSLTokenizer): UINode {
        // Get type alias
        val typeAlias = tokenizer.readIdentifier()
            ?: throw PluginException.LoadException("Expected component type")

        val fullType = TYPE_ALIAS_MAP[typeAlias] ?: typeAlias

        // Check for ID (#id)
        var id: String? = null
        if (tokenizer.peek() == '#') {
            tokenizer.consume('#')
            id = tokenizer.readIdentifier()
        }

        // Expect opening brace
        tokenizer.expect('{')

        // Parse body: properties, modifiers, callbacks, children
        val properties = mutableMapOf<String, Any?>()
        val modifiers = mutableListOf<ModifierData>()
        val callbacks = mutableMapOf<String, String>()
        val children = mutableListOf<UINode>()
        var style: Map<String, Any?> = emptyMap()

        while (tokenizer.peek() != '}' && !tokenizer.isAtEnd()) {
            tokenizer.skipWhitespace()

            when {
                // Modifier: @name(args)
                tokenizer.peek() == '@' -> {
                    tokenizer.consume('@')
                    val modName = tokenizer.readIdentifier()!!

                    // Check if it's a callback (@tap->handler)
                    if (tokenizer.peek() == '-' && tokenizer.peekAhead(1) == '>') {
                        tokenizer.consume('-')
                        tokenizer.consume('>')
                        val handler = tokenizer.readIdentifier()
                        val eventName = EVENT_MAP[modName] ?: modName

                        // Check for params: @tap->handler(arg)
                        if (tokenizer.peek() == '(') {
                            tokenizer.consume('(')
                            val params = tokenizer.readUntil(')')
                            tokenizer.consume(')')
                            callbacks[eventName] = "$handler($params)"
                        } else {
                            callbacks[eventName] = handler ?: ""
                        }
                    } else if (tokenizer.peek() == '(') {
                        // Modifier with args
                        tokenizer.consume('(')
                        val args = tokenizer.readUntil(')')
                        tokenizer.consume(')')

                        val fullModName = MODIFIER_MAP[modName] ?: modName
                        modifiers.add(ModifierData(fullModName, parseModifierArgs(args)))
                    }

                    // Skip comma or semicolon
                    if (tokenizer.peek() == ',' || tokenizer.peek() == ';') {
                        tokenizer.advance()
                    }
                }

                // Child component: starts with uppercase
                tokenizer.peek()?.isUpperCase() == true -> {
                    children.add(parseComponent(tokenizer))
                    if (tokenizer.peek() == ';') {
                        tokenizer.consume(';')
                    }
                }

                // Property: name:value
                else -> {
                    val propName = tokenizer.readIdentifier()
                    if (propName != null && tokenizer.peek() == ':') {
                        tokenizer.consume(':')
                        val value = tokenizer.readValue()

                        // Expand shorthands in string values
                        val expandedValue = if (value is String && value.contains('$')) {
                            expandShorthands(value)
                        } else {
                            value
                        }

                        properties[propName] = expandedValue

                        if (tokenizer.peek() == ';') {
                            tokenizer.consume(';')
                        }
                    } else {
                        // Unknown token, skip
                        tokenizer.advance()
                    }
                }
            }
        }

        tokenizer.expect('}')

        // Build style from modifiers and inline style properties
        val styleBuilder = mutableMapOf<String, Any?>()
        for (mod in modifiers) {
            when (mod.name) {
                "padding" -> {
                    if (mod.args.size == 1) {
                        styleBuilder["padding"] = mod.args[0]
                    } else if (mod.args.size == 4) {
                        styleBuilder["paddingTop"] = mod.args[0]
                        styleBuilder["paddingRight"] = mod.args[1]
                        styleBuilder["paddingBottom"] = mod.args[2]
                        styleBuilder["paddingLeft"] = mod.args[3]
                    }
                }
                "margin" -> {
                    if (mod.args.size == 1) {
                        styleBuilder["margin"] = mod.args[0]
                    }
                }
                "gap" -> {
                    styleBuilder["gap"] = mod.args.firstOrNull() ?: 0
                }
                "frame" -> {
                    if (mod.args.size >= 2) {
                        styleBuilder["width"] = mod.args[0]
                        styleBuilder["height"] = mod.args[1]
                    }
                }
                "background" -> {
                    styleBuilder["backgroundColor"] = mod.args.firstOrNull()
                }
                "cornerRadius" -> {
                    styleBuilder["borderRadius"] = mod.args.firstOrNull() ?: 0
                }
                "opacity" -> {
                    styleBuilder["opacity"] = mod.args.firstOrNull() ?: 1.0
                }
                else -> {
                    // Store as-is
                    styleBuilder[mod.name] = mod.args.firstOrNull()
                }
            }
        }

        // Extract style properties from properties
        val styleProps = listOf("fontSize", "fontWeight", "fontFamily", "color", "backgroundColor",
            "textAlign", "width", "height", "flex", "borderRadius", "padding", "margin", "gap")
        for (prop in styleProps) {
            if (properties.containsKey(prop)) {
                styleBuilder[prop] = properties.remove(prop)
            }
        }

        style = styleBuilder

        // Convert to UINode format from PluginDefinition
        // Separate static props from binding expressions
        val staticProps = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        val bindingMap = mutableMapOf<String, Expression>()

        // Process regular properties
        properties.forEach { (key, value) ->
            when (value) {
                is String -> {
                    if (value.startsWith("$")) {
                        // This is a binding expression
                        bindingMap[key] = Expression.of(value)
                    } else {
                        // Static string value
                        staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                    }
                }
                is Number -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                is Boolean -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                null -> staticProps[key] = kotlinx.serialization.json.JsonNull
                else -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value.toString())
            }
        }

        // Process style as static props
        style.forEach { (key, value) ->
            when (value) {
                is String -> {
                    if (value.startsWith("$")) {
                        bindingMap[key] = Expression.of(value)
                    } else {
                        staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                    }
                }
                is Number -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                is Boolean -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value)
                null -> staticProps[key] = kotlinx.serialization.json.JsonNull
                else -> staticProps[key] = kotlinx.serialization.json.JsonPrimitive(value.toString())
            }
        }

        // Convert callbacks to EventHandler objects
        val eventHandlers = callbacks.mapValues { (_, handlerStr) ->
            // Parse handler string: "reducerName" or "reducerName(param1, param2)"
            val parenIndex = handlerStr.indexOf('(')
            if (parenIndex > 0) {
                val reducerName = handlerStr.substring(0, parenIndex)
                val paramsStr = handlerStr.substring(parenIndex + 1, handlerStr.lastIndexOf(')'))
                val paramList = paramsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                // For now, assume positional params map to reducer params in order
                // In a more complete implementation, we'd parse named params
                val paramMap = paramList.mapIndexed { index, value ->
                    "arg$index" to Expression.of(value)
                }.toMap()

                EventHandler(reducer = reducerName, params = paramMap)
            } else {
                EventHandler.simple(handlerStr)
            }
        }

        return UINode(
            type = fullType,
            props = staticProps,
            bindings = bindingMap,
            events = eventHandlers,
            children = children
        )
    }

    /**
     * Parse modifier arguments
     */
    private fun parseModifierArgs(argsStr: String): List<Any> {
        if (argsStr.isBlank()) return emptyList()

        val args = mutableListOf<Any>()
        var current = StringBuilder()
        var depth = 0

        for (c in argsStr) {
            when {
                c == '(' -> {
                    depth++
                    current.append(c)
                }
                c == ')' -> {
                    depth--
                    current.append(c)
                }
                c == ',' && depth == 0 -> {
                    args.add(parseArgValue(current.toString().trim()))
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }

        if (current.isNotEmpty()) {
            args.add(parseArgValue(current.toString().trim()))
        }

        return args
    }

    /**
     * Parse a single argument value
     */
    private fun parseArgValue(str: String): Any {
        return when {
            str.isEmpty() -> ""
            str.startsWith("#") -> str // Color
            str.toDoubleOrNull() != null -> {
                if (str.contains('.')) str.toDouble() else str.toInt()
            }
            str == "true" -> true
            str == "false" -> false
            str == "null" -> Unit
            str.startsWith("\"") && str.endsWith("\"") -> str.drop(1).dropLast(1)
            else -> str
        }
    }

    /**
     * Parse a value and return (type, default)
     */
    private fun parseValue(valueStr: String): Pair<StateValueType, Any?> {
        return when {
            valueStr == "null" -> StateValueType.NULL to null
            valueStr == "true" -> StateValueType.BOOLEAN to true
            valueStr == "false" -> StateValueType.BOOLEAN to false
            valueStr.startsWith("\"") && valueStr.endsWith("\"") -> {
                StateValueType.STRING to valueStr.drop(1).dropLast(1)
            }
            valueStr.startsWith("[") -> StateValueType.ARRAY to emptyList<Any>()
            valueStr.startsWith("{") -> StateValueType.OBJECT to emptyMap<String, Any>()
            valueStr.toIntOrNull() != null -> StateValueType.NUMBER to valueStr.toInt()
            valueStr.toDoubleOrNull() != null -> StateValueType.NUMBER to valueStr.toDouble()
            else -> StateValueType.STRING to valueStr
        }
    }
}

/**
 * Modifier data holder
 */
private data class ModifierData(
    val name: String,
    val args: List<Any>
)

/**
 * Compact DSL Tokenizer
 */
private class CompactDSLTokenizer(private val input: String) {
    private var pos = 0

    fun isAtEnd(): Boolean = pos >= input.length

    fun peek(): Char? = if (pos < input.length) input[pos] else null

    fun peekAhead(offset: Int): Char? = if (pos + offset < input.length) input[pos + offset] else null

    fun advance(): Char? {
        return if (pos < input.length) input[pos++] else null
    }

    fun consume(expected: Char) {
        skipWhitespace()
        if (peek() != expected) {
            throw PluginException.LoadException("Expected '$expected' but got '${peek()}' at position $pos")
        }
        advance()
    }

    fun expect(expected: Char) = consume(expected)

    fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) {
            pos++
        }
    }

    fun readIdentifier(): String? {
        skipWhitespace()
        if (isAtEnd() || !peek()!!.isLetter() && peek() != '_') return null

        val start = pos
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_')) {
            pos++
        }
        return input.substring(start, pos)
    }

    fun readUntil(delimiter: Char): String {
        val result = StringBuilder()
        var depth = 0

        while (pos < input.length) {
            val c = input[pos]
            when {
                c == '(' || c == '[' || c == '{' -> {
                    depth++
                    result.append(c)
                    pos++
                }
                c == ')' || c == ']' || c == '}' -> {
                    if (depth == 0 && c == delimiter) {
                        return result.toString()
                    }
                    depth--
                    result.append(c)
                    pos++
                }
                c == delimiter && depth == 0 -> {
                    return result.toString()
                }
                else -> {
                    result.append(c)
                    pos++
                }
            }
        }

        return result.toString()
    }

    fun readValue(): Any? {
        skipWhitespace()

        return when {
            peek() == '"' -> readString()
            peek() == '\'' -> readSingleQuoteString()
            peek() == '#' -> readColor()
            peek() == '$' -> readExpression()
            peek() == '@' -> readModuleExpression()
            peek()?.isDigit() == true || peek() == '-' -> readNumber()
            peek()?.isLetter() == true -> {
                val word = readIdentifier()
                when (word) {
                    "true" -> true
                    "false" -> false
                    "null" -> null
                    else -> word
                }
            }
            else -> null
        }
    }

    private fun readString(): String {
        advance() // Skip opening quote
        val result = StringBuilder()
        while (pos < input.length && input[pos] != '"') {
            if (input[pos] == '\\' && pos + 1 < input.length) {
                pos++
                when (input[pos]) {
                    'n' -> result.append('\n')
                    't' -> result.append('\t')
                    'r' -> result.append('\r')
                    '"' -> result.append('"')
                    '\\' -> result.append('\\')
                    else -> result.append(input[pos])
                }
            } else {
                result.append(input[pos])
            }
            pos++
        }
        if (pos < input.length) pos++ // Skip closing quote
        return result.toString()
    }

    private fun readSingleQuoteString(): String {
        advance() // Skip opening quote
        val result = StringBuilder()
        while (pos < input.length && input[pos] != '\'') {
            result.append(input[pos])
            pos++
        }
        if (pos < input.length) pos++ // Skip closing quote
        return result.toString()
    }

    private fun readColor(): String {
        val start = pos
        pos++ // Skip #
        while (pos < input.length && (input[pos].isLetterOrDigit())) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun readExpression(): String {
        val start = pos
        var depth = 0

        while (pos < input.length) {
            val c = input[pos]
            when {
                c == '(' -> {
                    depth++
                    pos++
                }
                c == ')' -> {
                    if (depth == 0) break
                    depth--
                    pos++
                }
                c == ';' || c == '}' || c == ',' -> {
                    if (depth == 0) break
                    pos++
                }
                else -> pos++
            }
        }

        return input.substring(start, pos)
    }

    /**
     * Read a module expression: @module.method() or @module.path.method()
     * Returns the full expression string for later parsing.
     */
    private fun readModuleExpression(): String {
        val start = pos
        var depth = 0

        while (pos < input.length) {
            val c = input[pos]
            when {
                c == '(' -> {
                    depth++
                    pos++
                }
                c == ')' -> {
                    if (depth == 0) break
                    depth--
                    pos++
                    // If we've closed all parens, we're done with the module call
                    if (depth == 0) break
                }
                c == ';' || c == '}' || c == ',' -> {
                    if (depth == 0) break
                    pos++
                }
                else -> pos++
            }
        }

        return input.substring(start, pos)
    }

    private fun readNumber(): Any {
        val start = pos
        if (input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) {
            pos++
        }
        if (pos < input.length && input[pos] == '.') {
            pos++
            while (pos < input.length && input[pos].isDigit()) {
                pos++
            }
            return input.substring(start, pos).toDouble()
        }
        return input.substring(start, pos).toInt()
    }
}

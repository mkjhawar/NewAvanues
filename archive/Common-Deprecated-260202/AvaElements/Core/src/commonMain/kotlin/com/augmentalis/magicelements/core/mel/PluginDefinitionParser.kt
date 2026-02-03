package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*

/**
 * Parser for plugin definitions from YAML/JSON format.
 *
 * Supports parsing complete plugin definitions including metadata, state schema,
 * reducers, scripts (Tier 2), and UI trees. Handles both YAML and JSON inputs
 * through a common JSON intermediate representation.
 *
 * Usage:
 * ```kotlin
 * val parser = PluginDefinitionParser()
 * val definition = parser.parse(yamlString)
 * ```
 */
class PluginDefinitionParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
) {
    /**
     * Parse plugin definition from JSON string.
     *
     * @param jsonString JSON plugin definition
     * @return Parsed plugin definition
     * @throws PluginParseException on parsing errors
     */
    fun parseJson(jsonString: String): Result<PluginDefinition> {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            if (jsonElement !is JsonObject) {
                return Result.failure(PluginParseException("Root element must be an object"))
            }
            parseJsonObject(jsonElement)
        } catch (e: Exception) {
            Result.failure(PluginParseException("Failed to parse JSON: ${e.message}", e))
        }
    }

    /**
     * Parse plugin definition from JsonObject.
     *
     * @param obj Root JSON object
     * @return Parsed plugin definition
     */
    private fun parseJsonObject(obj: JsonObject): Result<PluginDefinition> {
        return try {
            // Parse metadata (required)
            val metadataObj = obj["metadata"]?.jsonObject
                ?: obj["plugin"]?.jsonObject  // Support both "metadata" and "plugin" keys
                ?: return Result.failure(PluginParseException("Missing 'metadata' or 'plugin' field"))

            val metadata = parseMetadata(metadataObj)

            // Parse tier (optional, defaults to DATA)
            val tierString = obj["tier"]?.jsonPrimitive?.contentOrNull
                ?: metadataObj["tier"]?.jsonPrimitive?.contentOrNull
                ?: "data"
            val tier = parseTier(tierString)

            // Parse state schema (optional)
            val stateObj = obj["state"]?.jsonObject
            val state = if (stateObj != null) parseState(stateObj) else emptyMap()

            // Parse reducers (optional)
            val reducersObj = obj["reducers"]?.jsonObject
            val reducers = if (reducersObj != null) parseReducers(reducersObj) else emptyMap()

            // Parse scripts (Tier 2 only, optional)
            val scriptsObj = obj["scripts"]?.jsonObject
            val scripts = if (scriptsObj != null && tier == PluginTier.LOGIC) {
                parseScripts(scriptsObj)
            } else null

            // Parse UI (required)
            val uiElement = obj["ui"]
                ?: return Result.failure(PluginParseException("Missing 'ui' field"))
            val ui = parseUINode(uiElement)

            // Create plugin definition
            val definition = PluginDefinition(
                metadata = metadata,
                tier = tier,
                state = state,
                reducers = reducers,
                scripts = scripts,
                ui = ui
            )

            Result.success(definition)
        } catch (e: PluginParseException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(PluginParseException("Unexpected error: ${e.message}", e))
        }
    }

    /**
     * Parse plugin metadata.
     */
    private fun parseMetadata(obj: JsonObject): PluginMetadataJson {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Missing required field: metadata.id")
        val name = obj["name"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Missing required field: metadata.name")
        val version = obj["version"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Missing required field: metadata.version")
        val author = obj["author"]?.jsonPrimitive?.contentOrNull
        val description = obj["description"]?.jsonPrimitive?.contentOrNull
        val minSdkVersion = obj["minSdkVersion"]?.jsonPrimitive?.contentOrNull ?: "1.0.0"

        return PluginMetadataJson(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            minSdkVersion = minSdkVersion
        )
    }

    /**
     * Parse plugin tier.
     */
    private fun parseTier(tierString: String): PluginTier {
        return when (tierString.lowercase()) {
            "data", "tier1", "1" -> PluginTier.DATA
            "logic", "tier2", "2" -> PluginTier.LOGIC
            else -> throw PluginParseException("Invalid tier: $tierString (must be 'data' or 'logic')")
        }
    }

    /**
     * Parse state schema.
     */
    private fun parseState(obj: JsonObject): Map<String, StateVariable> {
        return obj.mapValues { (name, value) ->
            parseStateVariable(name, value)
        }
    }

    /**
     * Parse a single state variable.
     */
    private fun parseStateVariable(name: String, value: JsonElement): StateVariable {
        return when (value) {
            // Simple default value (infer type)
            is JsonPrimitive, is JsonArray, is JsonObject -> {
                StateVariable(
                    type = StateType.from(value),
                    default = value,
                    persist = false
                )
            }
            // Object with explicit type and options
            else -> throw PluginParseException("Invalid state variable: $name")
        }
    }

    /**
     * Parse reducers.
     */
    private fun parseReducers(obj: JsonObject): Map<String, Reducer> {
        return obj.mapValues { (name, value) ->
            if (value !is JsonObject) {
                throw PluginParseException("Reducer '$name' must be an object")
            }
            parseReducer(name, value)
        }
    }

    /**
     * Parse a single reducer.
     */
    private fun parseReducer(name: String, obj: JsonObject): Reducer {
        // Parse params (optional)
        val params = obj["params"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: emptyList()

        // Parse next_state (required)
        val nextStateObj = obj["next_state"]?.jsonObject
            ?: obj["nextState"]?.jsonObject  // Support both snake_case and camelCase
            ?: throw PluginParseException("Reducer '$name' missing 'next_state' field")

        val nextState = nextStateObj.mapValues { (_, value) ->
            parseExpression(value)
        }

        // Parse effects (Tier 2 only, optional)
        val effectsArray = obj["effects"]?.jsonArray
        val effects = effectsArray?.map { parseEffect(it.jsonObject) }

        return Reducer(
            params = params,
            nextState = nextState,
            effects = effects
        )
    }

    /**
     * Parse an expression.
     */
    private fun parseExpression(value: JsonElement): Expression {
        return when (value) {
            is JsonPrimitive -> Expression.of(value.content)
            is JsonObject, is JsonArray -> Expression.of(value.toString())
            else -> Expression.of("")
        }
    }

    /**
     * Parse an effect.
     */
    private fun parseEffect(obj: JsonObject): Effect {
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Effect missing 'type' field")
        val action = obj["action"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Effect missing 'action' field")
        val params = obj["params"]?.jsonObject?.toMap() ?: emptyMap()

        return Effect(
            type = type,
            action = action,
            params = params
        )
    }

    /**
     * Parse scripts.
     */
    private fun parseScripts(obj: JsonObject): Map<String, Script> {
        return obj.mapValues { (name, value) ->
            if (value !is JsonObject) {
                throw PluginParseException("Script '$name' must be an object")
            }
            parseScript(name, value)
        }
    }

    /**
     * Parse a single script.
     */
    private fun parseScript(name: String, obj: JsonObject): Script {
        val params = obj["params"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: emptyList()

        val body = obj["body"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("Script '$name' missing 'body' field")

        return Script(params = params, body = body)
    }

    /**
     * Parse UI node.
     */
    private fun parseUINode(element: JsonElement): UINode {
        if (element !is JsonObject) {
            throw PluginParseException("UI node must be an object")
        }

        // UI node can be either:
        // 1. Single component: { type: "Button", props: { ... } }
        // 2. Component with children: { Column: { children: [...] } }

        // Case 2: Component as key
        if (element.containsKey("type")) {
            return parseComponentNode(element)
        }

        // Case 1: Single component name as key
        val componentEntry = element.entries.firstOrNull()
            ?: throw PluginParseException("Empty UI node")

        val componentType = componentEntry.key
        val componentValue = componentEntry.value

        return if (componentValue is JsonObject) {
            parseComponentWithProps(componentType, componentValue)
        } else {
            // Simple component with no props
            UINode(type = componentType)
        }
    }

    /**
     * Parse component node with explicit "type" field.
     */
    private fun parseComponentNode(obj: JsonObject): UINode {
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
            ?: throw PluginParseException("UI node missing 'type' field")

        val props = obj["props"]?.jsonObject?.toMap() ?: emptyMap()
        val bindings = obj["bindings"]?.jsonObject?.mapValues { parseExpression(it.value) } ?: emptyMap()
        val events = obj["events"]?.jsonObject?.mapValues { parseEventHandler(it.value) } ?: emptyMap()
        val children = obj["children"]?.jsonArray?.map { parseUINode(it) }

        return UINode(
            type = type,
            props = props,
            bindings = bindings,
            events = events,
            children = children
        )
    }

    /**
     * Parse component with props object.
     */
    private fun parseComponentWithProps(type: String, obj: JsonObject): UINode {
        val props = mutableMapOf<String, JsonElement>()
        val bindings = mutableMapOf<String, Expression>()
        val events = mutableMapOf<String, EventHandler>()
        var children: List<UINode>? = null

        obj.forEach { (key, value) ->
            when (key) {
                "children" -> {
                    children = value.jsonArray.map { parseUINode(it) }
                }
                // Event handlers (start with "on")
                else -> if (key.startsWith("on")) {
                    events[key] = parseEventHandler(value)
                } else if (value is JsonPrimitive && value.content.startsWith("$")) {
                    // Binding (starts with $)
                    bindings[key] = parseExpression(value)
                } else {
                    // Static prop
                    props[key] = value
                }
            }
        }

        return UINode(
            type = type,
            props = props,
            bindings = bindings,
            events = events,
            children = children
        )
    }

    /**
     * Parse event handler.
     */
    private fun parseEventHandler(value: JsonElement): EventHandler {
        return when (value) {
            // Simple string: "reducerName" or "reducerName(param1, param2)"
            is JsonPrimitive -> {
                val content = value.content
                if (content.contains("(")) {
                    // Parse function call style
                    val parts = content.split("(", ")", limit = 3)
                    val reducerName = parts[0].trim()
                    val paramsStr = parts.getOrNull(1)?.trim() ?: ""

                    if (paramsStr.isEmpty()) {
                        EventHandler.simple(reducerName)
                    } else {
                        // Parse parameters (simple implementation, assumes literal values)
                        val params = paramsStr.split(",").mapIndexed { idx, param ->
                            "arg$idx" to param.trim()
                        }
                        EventHandler.withParams(reducerName, *params.toTypedArray())
                    }
                } else {
                    // Simple reducer name
                    EventHandler.simple(content)
                }
            }
            // Object: { reducer: "name", params: { ... } }
            is JsonObject -> {
                val reducer = value["reducer"]?.jsonPrimitive?.contentOrNull
                    ?: throw PluginParseException("Event handler missing 'reducer' field")
                val params = value["params"]?.jsonObject?.mapValues { parseExpression(it.value) } ?: emptyMap()
                EventHandler(reducer = reducer, params = params)
            }
            else -> throw PluginParseException("Invalid event handler format")
        }
    }
}

/**
 * Exception thrown during plugin parsing.
 */
class PluginParseException(
    message: String,
    cause: Throwable? = null
) : Exception("Plugin parse error: $message", cause)

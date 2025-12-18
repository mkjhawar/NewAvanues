# Developer Manual - Chapter 69: MagicUI & MagicCode Implementation Guide

## Overview

This chapter outlines the implementation requirements to make AVA MagicUI (UI generation) and MagicCode (code generation) functional. These systems allow users to create UI and code through natural language.

---

## Implementation Checklist

| Component | Priority | Status | Description |
|-----------|----------|--------|-------------|
| MagicUI Parser | P0 | TODO | Parse DSL → AST |
| MagicUI Renderer | P0 | TODO | AST → Compose UI |
| MagicCode Parser | P1 | TODO | Parse DSL → AST |
| MagicCode Generator | P1 | TODO | AST → Kotlin code |
| Plugin Runtime APIs | P1 | TODO | storage, network, etc. |
| Plugin Loader | P2 | TODO | Load/sandbox plugins |
| LLM Integration | P0 | TODO | Route intents → LLM |
| Prompt Template Loader | P0 | TODO | Load .avp files |

---

## 1. MagicUI Parser

### Purpose
Parse MagicUI DSL strings into an Abstract Syntax Tree (AST).

### Input
```
Row#settingRow{@pad:16;spacing:12;Text#title{text:"Hello"};Switch#toggle{checked:true}}
```

### Output (AST)
```kotlin
MagicUINode(
    type = "Row",
    id = "settingRow",
    properties = mapOf("padding" to 16, "spacing" to 12),
    children = listOf(
        MagicUINode(type = "Text", id = "title", properties = mapOf("text" to "Hello")),
        MagicUINode(type = "Switch", id = "toggle", properties = mapOf("checked" to true))
    )
)
```

### Implementation Location
```
common/MagicUI/
├── src/commonMain/kotlin/com/augmentalis/ava/magicui/
│   ├── parser/
│   │   ├── MagicUIParser.kt       # DSL → AST
│   │   ├── MagicUILexer.kt        # Tokenizer
│   │   └── MagicUINode.kt         # AST node data class
│   ├── model/
│   │   ├── ComponentType.kt       # Enum of all components
│   │   ├── PropertyType.kt        # Property definitions
│   │   └── ThemeTokens.kt         # Ocean Glass colors
│   └── MagicUIEngine.kt           # Main entry point
```

### Parser Implementation

```kotlin
// MagicUINode.kt
@Serializable
data class MagicUINode(
    val type: String,
    val id: String? = null,
    val properties: Map<String, Any> = emptyMap(),
    val children: List<MagicUINode> = emptyList()
)

// MagicUIParser.kt
class MagicUIParser {

    fun parse(dsl: String): MagicUINode {
        val lexer = MagicUILexer(dsl)
        return parseNode(lexer)
    }

    private fun parseNode(lexer: MagicUILexer): MagicUINode {
        // Parse: ComponentType#id{properties;children}
        val type = lexer.readIdentifier()
        val id = if (lexer.peek() == '#') {
            lexer.consume('#')
            lexer.readIdentifier()
        } else null

        val properties = mutableMapOf<String, Any>()
        val children = mutableListOf<MagicUINode>()

        if (lexer.peek() == '{') {
            lexer.consume('{')
            while (lexer.peek() != '}') {
                when {
                    lexer.isPropertyStart() -> {
                        val (key, value) = parseProperty(lexer)
                        properties[expandShorthand(key)] = value
                    }
                    lexer.isNodeStart() -> {
                        children.add(parseNode(lexer))
                    }
                }
                if (lexer.peek() == ';') lexer.consume(';')
            }
            lexer.consume('}')
        }

        return MagicUINode(type, id, properties, children)
    }

    private fun expandShorthand(key: String): String = when (key) {
        "@pad" -> "padding"
        "@m" -> "margin"
        "@align" -> "alignment"
        "@size" -> "size"
        "@radius" -> "cornerRadius"
        "@bg" -> "backgroundColor"
        else -> key
    }
}
```

---

## 2. MagicUI Renderer (Android)

### Purpose
Convert AST nodes to Jetpack Compose UI.

### Implementation Location
```
common/MagicUI/
└── src/androidMain/kotlin/com/augmentalis/ava/magicui/
    └── renderer/
        ├── MagicUIRenderer.kt     # Main renderer
        ├── ComponentRenderers.kt   # Per-component renderers
        └── ThemeProvider.kt        # Ocean Glass theme
```

### Renderer Implementation

```kotlin
// MagicUIRenderer.kt
@Composable
fun MagicUIRenderer(
    node: MagicUINode,
    bindings: Map<String, Any> = emptyMap(),
    onAction: (String, Map<String, Any>) -> Unit = { _, _ -> }
) {
    when (node.type) {
        "Col" -> ColumnRenderer(node, bindings, onAction)
        "Row" -> RowRenderer(node, bindings, onAction)
        "Text" -> TextRenderer(node, bindings)
        "Btn" -> ButtonRenderer(node, bindings, onAction)
        "IconBtn" -> IconButtonRenderer(node, bindings, onAction)
        "Card" -> CardRenderer(node, bindings, onAction)
        "Switch" -> SwitchRenderer(node, bindings, onAction)
        "Input" -> InputRenderer(node, bindings, onAction)
        "Img" -> ImageRenderer(node, bindings)
        "LazyCol" -> LazyColumnRenderer(node, bindings, onAction)
        "Spacer" -> SpacerRenderer(node)
        "Divider" -> DividerRenderer(node)
        // ... other components
        else -> UnknownComponentRenderer(node)
    }
}

// ComponentRenderers.kt
@Composable
fun ColumnRenderer(
    node: MagicUINode,
    bindings: Map<String, Any>,
    onAction: (String, Map<String, Any>) -> Unit
) {
    Column(
        modifier = Modifier
            .then(node.paddingModifier())
            .then(node.backgroundModifier())
            .then(node.sizeModifier()),
        verticalArrangement = node.spacingArrangement(),
        horizontalAlignment = node.horizontalAlignment()
    ) {
        node.children.forEach { child ->
            MagicUIRenderer(child, bindings, onAction)
        }
    }
}

@Composable
fun TextRenderer(node: MagicUINode, bindings: Map<String, Any>) {
    val text = node.resolveBinding("text", bindings) as? String ?: ""
    val size = (node.properties["size"] as? Number)?.toInt() ?: 14
    val weight = when (node.properties["weight"]) {
        "bold" -> FontWeight.Bold
        "medium" -> FontWeight.Medium
        else -> FontWeight.Normal
    }
    val color = node.resolveColor("color")

    Text(
        text = text,
        fontSize = size.sp,
        fontWeight = weight,
        color = color
    )
}

@Composable
fun SwitchRenderer(
    node: MagicUINode,
    bindings: Map<String, Any>,
    onAction: (String, Map<String, Any>) -> Unit
) {
    val checked = node.resolveBinding("checked", bindings) as? Boolean ?: false
    val onToggle = node.properties["onToggle"] as? String

    Switch(
        checked = checked,
        onCheckedChange = { newValue ->
            onToggle?.let { action ->
                onAction(action, mapOf("value" to newValue))
            }
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = OceanGlassColors.coralBlue,
            checkedTrackColor = OceanGlassColors.coralBlue.copy(alpha = 0.5f)
        )
    )
}
```

### Theme Provider

```kotlin
// ThemeProvider.kt
object OceanGlassColors {
    val oceanDeep = Color(0xFF0A1628)
    val oceanMid = Color(0xFF1E3A5F)
    val surface5 = Color.White.copy(alpha = 0.05f)
    val surface10 = Color.White.copy(alpha = 0.10f)
    val surface20 = Color.White.copy(alpha = 0.20f)
    val border10 = Color.White.copy(alpha = 0.10f)
    val border20 = Color.White.copy(alpha = 0.20f)
    val coralBlue = Color(0xFF3B82F6)
    val seafoamGreen = Color(0xFF10B981)
    val coralRed = Color(0xFFEF4444)
    val sunsetOrange = Color(0xFFF59E0B)
    val textPrimary = Color.White.copy(alpha = 0.90f)
    val textSecondary = Color.White.copy(alpha = 0.70f)
    val textTertiary = Color.White.copy(alpha = 0.50f)
}

fun MagicUINode.resolveColor(property: String): Color {
    return when (properties[property] as? String) {
        "oceanDeep" -> OceanGlassColors.oceanDeep
        "oceanMid" -> OceanGlassColors.oceanMid
        "surface5" -> OceanGlassColors.surface5
        "surface10" -> OceanGlassColors.surface10
        "surface20" -> OceanGlassColors.surface20
        "coralBlue" -> OceanGlassColors.coralBlue
        "seafoamGreen" -> OceanGlassColors.seafoamGreen
        "coralRed" -> OceanGlassColors.coralRed
        "sunsetOrange" -> OceanGlassColors.sunsetOrange
        "textPrimary" -> OceanGlassColors.textPrimary
        "textSecondary" -> OceanGlassColors.textSecondary
        "textTertiary" -> OceanGlassColors.textTertiary
        else -> OceanGlassColors.textPrimary
    }
}
```

---

## 3. MagicCode Parser

### Purpose
Parse MagicCode DSL into executable structures.

### DSL Elements

```
DATA:model:Todo:{id:String,title:String,completed:Boolean}
DATA:list:todos:Todo[]
FN:addTodo:title:String:{storage.add("todos",Todo(uuid(),title,false))}
EVT:onEnable:{todos=storage.getAll("todos");ui.show("main")}
ACT:schedule:refresh:interval:15min
```

### Implementation

```kotlin
// MagicCodeParser.kt
sealed class MagicCodeElement {
    data class DataModel(
        val name: String,
        val fields: Map<String, String>
    ) : MagicCodeElement()

    data class DataList(
        val name: String,
        val itemType: String
    ) : MagicCodeElement()

    data class Function(
        val name: String,
        val params: List<Pair<String, String>>,
        val body: String
    ) : MagicCodeElement()

    data class EventHandler(
        val event: String,
        val handler: String
    ) : MagicCodeElement()

    data class ScheduledAction(
        val type: String,
        val name: String,
        val config: Map<String, String>
    ) : MagicCodeElement()
}

class MagicCodeParser {
    fun parse(dsl: String): List<MagicCodeElement> {
        return dsl.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { parseLine(it) }
    }

    private fun parseLine(line: String): MagicCodeElement? {
        val parts = line.split(":", limit = 4)
        return when (parts[0]) {
            "DATA" -> parseData(parts)
            "FN" -> parseFunction(parts)
            "EVT" -> parseEvent(parts)
            "ACT" -> parseAction(parts)
            else -> null
        }
    }

    private fun parseData(parts: List<String>): MagicCodeElement {
        return when (parts[1]) {
            "model" -> {
                val name = parts[2]
                val fieldsStr = parts[3].removeSurrounding("{", "}")
                val fields = fieldsStr.split(",").associate {
                    val (k, v) = it.split(":")
                    k.trim() to v.trim()
                }
                MagicCodeElement.DataModel(name, fields)
            }
            "list" -> {
                val name = parts[2]
                val itemType = parts[3].removeSuffix("[]")
                MagicCodeElement.DataList(name, itemType)
            }
            else -> throw IllegalArgumentException("Unknown DATA type: ${parts[1]}")
        }
    }
}
```

---

## 4. Plugin Runtime APIs

### Purpose
Provide APIs that plugins can call from MagicCode.

### Implementation Location
```
common/PluginRuntime/
└── src/commonMain/kotlin/com/augmentalis/ava/plugin/
    ├── api/
    │   ├── PluginStorage.kt       # Database operations
    │   ├── PluginNetwork.kt       # HTTP requests
    │   ├── PluginLocation.kt      # GPS/geofencing
    │   ├── PluginNotification.kt  # Notifications
    │   ├── PluginUI.kt            # UI control
    │   └── PluginEventBus.kt      # Inter-plugin events
    ├── sandbox/
    │   ├── PluginSandbox.kt       # Security sandbox
    │   └── PermissionEnforcer.kt  # Permission checks
    └── PluginContext.kt           # Main plugin context
```

### Storage API

```kotlin
// PluginStorage.kt
interface PluginStorage {
    suspend fun createTable(name: String, schema: Map<String, String>)
    suspend fun add(table: String, item: Map<String, Any>): String
    suspend fun get(table: String, id: String): Map<String, Any>?
    suspend fun getAll(table: String): List<Map<String, Any>>
    suspend fun query(table: String, where: String): List<Map<String, Any>>
    suspend fun update(table: String, id: String, updates: Map<String, Any>)
    suspend fun remove(table: String, id: String)
    suspend fun clear(table: String)
}

// PluginStorageImpl.kt (Android)
class PluginStorageImpl(
    private val pluginId: String,
    private val database: PluginDatabase
) : PluginStorage {

    override suspend fun add(table: String, item: Map<String, Any>): String {
        val id = item["id"] as? String ?: UUID.randomUUID().toString()
        val json = Json.encodeToString(item)
        database.pluginDataQueries.insert(
            plugin_id = pluginId,
            table_name = table,
            item_id = id,
            data = json,
            created_at = System.currentTimeMillis()
        )
        return id
    }

    override suspend fun getAll(table: String): List<Map<String, Any>> {
        return database.pluginDataQueries
            .selectAll(pluginId, table)
            .executeAsList()
            .map { Json.decodeFromString(it.data) }
    }

    // ... other methods
}
```

### Network API

```kotlin
// PluginNetwork.kt
interface PluginNetwork {
    suspend fun get(url: String, headers: Map<String, String>? = null): NetworkResponse
    suspend fun post(url: String, body: Any, headers: Map<String, String>? = null): NetworkResponse
    suspend fun put(url: String, body: Any, headers: Map<String, String>? = null): NetworkResponse
    suspend fun delete(url: String, headers: Map<String, String>? = null): NetworkResponse
}

data class NetworkResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String>
)

// PluginNetworkImpl.kt
class PluginNetworkImpl(
    private val pluginId: String,
    private val permissionEnforcer: PermissionEnforcer,
    private val httpClient: HttpClient
) : PluginNetwork {

    override suspend fun get(url: String, headers: Map<String, String>?): NetworkResponse {
        permissionEnforcer.require(pluginId, Permission.NETWORK)

        val response = httpClient.get(url) {
            headers?.forEach { (key, value) ->
                header(key, value)
            }
        }

        return NetworkResponse(
            statusCode = response.status.value,
            body = response.bodyAsText(),
            headers = response.headers.toMap()
        )
    }
}
```

### UI API

```kotlin
// PluginUI.kt
interface PluginUI {
    fun show(screenId: String, data: Map<String, Any>? = null)
    fun update()
    fun navigate(screenId: String, params: Map<String, Any>? = null)
    fun bind(data: Any)
    fun showDialog(config: DialogConfig)
    fun showSnackbar(message: String, action: String? = null)
}

// PluginUIImpl.kt
class PluginUIImpl(
    private val pluginId: String,
    private val uiStateFlow: MutableStateFlow<PluginUIState>
) : PluginUI {

    override fun show(screenId: String, data: Map<String, Any>?) {
        uiStateFlow.value = PluginUIState(
            currentScreen = screenId,
            bindings = data ?: emptyMap()
        )
    }

    override fun update() {
        uiStateFlow.value = uiStateFlow.value.copy(
            updateTrigger = System.currentTimeMillis()
        )
    }
}
```

---

## 5. LLM Integration for MagicUI/MagicCode

### Purpose
Route MagicUI/MagicCode intents to LLM with proper prompt templates.

### Implementation

```kotlin
// MagicIntentHandler.kt
class MagicIntentHandler(
    private val promptTemplateLoader: PromptTemplateLoader,
    private val llmService: LlmActionService,
    private val magicUIParser: MagicUIParser,
    private val magicCodeParser: MagicCodeParser
) {

    suspend fun handleMagicUIIntent(
        intentId: String,
        userInput: String
    ): MagicUIResult {
        // Load prompt template
        val template = promptTemplateLoader.load(intentId)

        // Generate DSL via LLM
        val response = llmService.generate(
            systemPrompt = template.systemPrompt,
            userPrompt = userInput,
            outputSchema = template.outputSchema
        )

        // Parse response to extract DSL
        val dsl = extractDSL(response.content)

        // Parse DSL to AST
        val ast = magicUIParser.parse(dsl)

        return MagicUIResult(
            rawDsl = dsl,
            ast = ast,
            metadata = extractMetadata(response.content)
        )
    }

    suspend fun handleMagicCodeIntent(
        intentId: String,
        userInput: String
    ): MagicCodeResult {
        val template = promptTemplateLoader.load(intentId)

        val response = llmService.generate(
            systemPrompt = template.systemPrompt,
            userPrompt = userInput,
            outputSchema = template.outputSchema
        )

        val code = extractCode(response.content)

        return MagicCodeResult(
            rawCode = code,
            elements = magicCodeParser.parse(code),
            metadata = extractMetadata(response.content)
        )
    }

    private fun extractDSL(content: String): String {
        // Extract JSN:component_id:... line
        val jsnLine = content.lines().find { it.startsWith("JSN:") }
        return jsnLine?.substringAfter("JSN:")?.substringAfter(":") ?: ""
    }
}
```

### Prompt Template Loader

```kotlin
// PromptTemplateLoader.kt
class PromptTemplateLoader(private val context: Context) {

    private val cache = mutableMapOf<String, PromptTemplate>()

    fun load(intentId: String, locale: String = "en-US"): PromptTemplate {
        val cacheKey = "$locale/$intentId"
        return cache.getOrPut(cacheKey) {
            val path = ".ava/prompts/$locale/$intentId.avp"
            val content = context.assets.open(path).bufferedReader().use { it.readText() }
            parseAvpTemplate(content)
        }
    }

    private fun parseAvpTemplate(content: String): PromptTemplate {
        val sections = content.split("---").filter { it.isNotBlank() }

        // Parse YAML frontmatter
        val frontmatter = Yaml.default.decodeFromString<AvpFrontmatter>(sections[0])

        // Parse system prompt and other sections
        val systemPrompt = extractSection(sections[1], "system")
        val outputSchema = extractSection(sections[1], "output_schema")
        val examples = parseExamples(sections[1])

        return PromptTemplate(
            intentId = frontmatter.metadata.intent,
            name = frontmatter.metadata.name,
            processingType = frontmatter.metadata.processing_type,
            systemPrompt = systemPrompt,
            outputSchema = outputSchema,
            examples = examples,
            minContext = frontmatter.metadata.model_requirements?.min_context ?: 512
        )
    }
}
```

---

## 6. Integration with IntentRouter

### Update IntentRouter

```kotlin
// IntentRouter.kt
class IntentRouter(
    private val intentClassifier: IntentClassifier,
    private val actionHandlers: Map<String, ActionHandler>,
    private val llmActionService: LlmActionService,
    private val magicIntentHandler: MagicIntentHandler  // NEW
) {

    suspend fun route(userInput: String): ActionResult {
        val classification = intentClassifier.classify(userInput)

        return when {
            // MagicUI intents
            classification.intentId in MAGIC_UI_INTENTS -> {
                val result = magicIntentHandler.handleMagicUIIntent(
                    classification.intentId, userInput
                )
                ActionResult.MagicUI(result)
            }

            // MagicCode intents
            classification.intentId in MAGIC_CODE_INTENTS -> {
                val result = magicIntentHandler.handleMagicCodeIntent(
                    classification.intentId, userInput
                )
                ActionResult.MagicCode(result)
            }

            // Standard NLU intents
            classification.processingType == ProcessingType.NLU_ONLY -> {
                actionHandlers[classification.intentId]?.handle(userInput)
                    ?: ActionResult.Error("No handler")
            }

            // LLM-required intents
            else -> {
                llmActionService.process(classification.intentId, userInput)
            }
        }
    }

    companion object {
        val MAGIC_UI_INTENTS = setOf(
            "create_ui_component", "create_ui_screen", "modify_ui",
            "create_form", "create_list", "create_dialog"
        )

        val MAGIC_CODE_INTENTS = setOf(
            "generate_data_class", "generate_viewmodel", "generate_repository",
            "generate_usecase", "generate_screen", "create_plugin"
        )
    }
}
```

---

## 7. File Structure Summary

```
common/
├── MagicUI/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/augmentalis/ava/magicui/
│       │   ├── parser/
│       │   │   ├── MagicUIParser.kt
│       │   │   ├── MagicUILexer.kt
│       │   │   └── MagicUINode.kt
│       │   ├── model/
│       │   │   ├── ComponentType.kt
│       │   │   └── ThemeTokens.kt
│       │   └── MagicUIEngine.kt
│       └── androidMain/kotlin/com/augmentalis/ava/magicui/
│           └── renderer/
│               ├── MagicUIRenderer.kt
│               ├── ComponentRenderers.kt
│               └── ThemeProvider.kt
│
├── MagicCode/
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/augmentalis/ava/magiccode/
│       ├── parser/
│       │   ├── MagicCodeParser.kt
│       │   └── MagicCodeElement.kt
│       └── generator/
│           └── KotlinCodeGenerator.kt
│
├── PluginRuntime/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/augmentalis/ava/plugin/
│       │   ├── api/
│       │   │   ├── PluginStorage.kt
│       │   │   ├── PluginNetwork.kt
│       │   │   ├── PluginLocation.kt
│       │   │   ├── PluginNotification.kt
│       │   │   └── PluginUI.kt
│       │   ├── sandbox/
│       │   │   └── PluginSandbox.kt
│       │   └── PluginContext.kt
│       └── androidMain/kotlin/com/augmentalis/ava/plugin/
│           └── impl/
│               ├── PluginStorageImpl.kt
│               ├── PluginNetworkImpl.kt
│               └── PluginUIImpl.kt
│
└── NLU/
    └── src/androidMain/kotlin/com/augmentalis/ava/features/nlu/
        ├── MagicIntentHandler.kt      # NEW
        └── PromptTemplateLoader.kt    # NEW
```

---

## 8. Implementation Order

| Phase | Components | Effort |
|-------|------------|--------|
| **Phase 1** | MagicUIParser, MagicUIRenderer, ThemeProvider | 2-3 days |
| **Phase 2** | PromptTemplateLoader, MagicIntentHandler | 1-2 days |
| **Phase 3** | MagicCodeParser, basic code generation | 2-3 days |
| **Phase 4** | PluginStorage, PluginUI APIs | 2-3 days |
| **Phase 5** | PluginNetwork, PluginLocation, PluginNotification | 2-3 days |
| **Phase 6** | Plugin sandbox, permission enforcement | 2-3 days |
| **Phase 7** | Plugin loader, packaging format | 2-3 days |

**Total estimated effort: 2-3 weeks**

---

## Author

Manoj Jhawar

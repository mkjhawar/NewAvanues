# Chapter 30: MagicCode Integration

## Overview

MagicCode is the code generation pipeline within the IDEAMagic ecosystem. It transforms VoiceOS DSL (.vos files) into native platform code - Kotlin Compose for Android, SwiftUI for iOS, and TypeScript React for web. This chapter explores MagicCode's architecture, generation pipeline, AST manipulation, and integration with VOS4.

**Location:** `/Volumes/M-Drive/Coding/voiceavanue/Universal/IDEAMagic/MagicCode/`

**Key Features:**
- Multi-target code generation (Kotlin, Swift, TypeScript)
- Complete lexer/parser/AST pipeline (reuses MagicUI parser)
- Template-based code emission
- Code optimization passes
- CLI tool for standalone usage
- VOS4 build system integration

---

## 30.1 MagicCode Architecture

### 30.1.1 System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    MagicCode Pipeline                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. INPUT                                                   │
│     .vos DSL Source Files                                   │
│            ↓                                                │
│  2. PARSING (uses MagicUI parser)                          │
│     VosTokenizer → VosParser → AST                         │
│            ↓                                                │
│  3. VALIDATION                                              │
│     Schema Validation → Type Checking                       │
│            ↓                                                │
│  4. TRANSFORMATION                                          │
│     State Extraction → Component Mapping → AST Transform    │
│            ↓                                                │
│  5. GENERATION                                              │
│     Template Processing → Code Emission                     │
│            ↓                                                │
│  6. OPTIMIZATION                                            │
│     Dead Code Elimination → Minification                    │
│            ↓                                                │
│  7. OUTPUT                                                  │
│     .kt / .swift / .tsx files                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 30.1.2 Module Structure

```
MagicCode/
└── src/
    └── commonMain/
        └── kotlin/
            └── com/augmentalis/voiceos/magiccode/
                ├── MagicCodeGenerator.kt       # Main API
                ├── core/
                │   ├── CodeGenerator.kt        # Generator interface
                │   ├── GeneratorConfig.kt      # Configuration
                │   ├── GeneratedCode.kt        # Output model
                │   ├── GeneratorTarget.kt      # Target platforms
                │   └── ValidationResult.kt     # Validation
                ├── generators/
                │   ├── kotlin/
                │   │   ├── KotlinComposeGenerator.kt
                │   │   ├── KotlinStateExtractor.kt
                │   │   ├── KotlinComponentMapper.kt
                │   │   ├── ThemeGenerator.kt
                │   │   ├── LifecycleGenerator.kt
                │   │   └── VoiceCommandGenerator.kt
                │   ├── swift/
                │   │   └── SwiftUIGenerator.kt
                │   └── react/
                │       └── ReactTypeScriptGenerator.kt
                ├── optimizer/
                │   └── CodeOptimizer.kt
                ├── benchmark/
                │   └── PerformanceBenchmark.kt
                └── cli/
                    └── MagicCodeCLI.kt
```

---

## 30.2 Core Generator API

### 30.2.1 MagicCodeGenerator

**File:** `MagicCodeGenerator.kt`

The main entry point for code generation:

```kotlin
class MagicCodeGenerator {
    private val generators = mutableMapOf<GeneratorTarget, CodeGenerator>()

    init {
        // Register all available generators
        registerGenerator(KotlinComposeGenerator())
        registerGenerator(SwiftUIGenerator())
        registerGenerator(ReactTypeScriptGenerator())
    }

    /**
     * Generate native code from a .vos file
     */
    fun generate(vosFile: File, config: GeneratorConfig): GenerationResult {
        val startTime = System.currentTimeMillis()

        try {
            // Validate inputs
            if (!vosFile.exists()) {
                throw GenerationException("VOS file not found: ${vosFile.absolutePath}")
            }

            val configErrors = config.validate()
            if (configErrors.isNotEmpty()) {
                throw GenerationException.invalidConfig(configErrors.joinToString("; "))
            }

            // Parse .vos file (reuses MagicUI parser)
            val vosContent = vosFile.readText()
            val tokenizer = VosTokenizer(vosContent)
            val tokens = tokenizer.tokenize()
            val parser = VosParser(tokens)
            val ast = parser.parse()

            // Get generator for target
            val generator = generators[config.target]
                ?: throw GenerationException(
                    "No generator registered for target ${config.target.displayName}"
                )

            // Validate AST if enabled
            if (config.validateSchema) {
                val validation = generator.validate(ast)
                if (!validation.isValid) {
                    if (config.strictMode) {
                        throw GenerationException("Validation failed:\n${validation.summary()}")
                    } else {
                        if (validation.hasWarnings) {
                            println("WARNINGS during validation:\n${validation.summary()}")
                        }
                    }
                }
            }

            // Generate code
            val generatedCode = generator.generate(ast, config)

            val duration = System.currentTimeMillis() - startTime

            return GenerationResult.success(
                vosFile = vosFile,
                generatedCode = generatedCode,
                durationMs = duration
            )
        } catch (e: GenerationException) {
            val duration = System.currentTimeMillis() - startTime
            return GenerationResult.failure(
                vosFile = vosFile,
                error = e.message ?: "Unknown error",
                durationMs = duration
            )
        }
    }

    /**
     * Generate code from multiple .vos files in batch
     */
    fun generateBatch(vosFiles: List<File>, config: GeneratorConfig): BatchResult {
        val startTime = System.currentTimeMillis()
        val results = vosFiles.map { file ->
            generate(file, config)
        }
        val duration = System.currentTimeMillis() - startTime

        return BatchResult(
            results = results,
            totalDurationMs = duration
        )
    }

    /**
     * Validate a .vos file without generating code
     */
    fun validate(vosFile: File, target: GeneratorTarget): ValidationResult {
        try {
            if (!vosFile.exists()) {
                return ValidationResult.error("File not found: ${vosFile.absolutePath}")
            }

            val vosContent = vosFile.readText()
            val tokenizer = VosTokenizer(vosContent)
            val tokens = tokenizer.tokenize()
            val parser = VosParser(tokens)
            val ast = parser.parse()

            val generator = generators[target]
                ?: return ValidationResult.error(
                    "No generator registered for target ${target.displayName}"
                )

            return generator.validate(ast)
        } catch (e: Exception) {
            return ValidationResult.error("Validation failed: ${e.message}")
        }
    }

    /**
     * List all registered generators
     */
    fun listGenerators(): List<GeneratorInfo> {
        return generators.values.map { it.info() }
    }
}
```

### 30.2.2 CodeGenerator Interface

**File:** `core/CodeGenerator.kt`

All platform-specific generators implement this interface:

```kotlin
interface CodeGenerator {
    /**
     * Target platform/framework this generator produces code for
     */
    val target: GeneratorTarget

    /**
     * Generate native code from VoiceOS DSL AST
     *
     * Process:
     * 1. Validate AST (if config.validateSchema is true)
     * 2. Extract state variables from AST
     * 3. Map DSL components to native components
     * 4. Transform callbacks to native event handlers
     * 5. Generate code using templates + builders
     * 6. Apply optimizations (if enabled)
     * 7. Write output files
     */
    fun generate(ast: VosAstNode.App, config: GeneratorConfig): GeneratedCode

    /**
     * Validate an AST before generation
     *
     * Checks for:
     * - Unknown component types
     * - Invalid properties
     * - Missing required properties
     * - Type mismatches
     * - Invalid callbacks
     */
    fun validate(ast: VosAstNode.App): ValidationResult

    /**
     * Return information about this generator
     */
    fun info(): GeneratorInfo
}
```

### 30.2.3 Generator Configuration

**File:** `core/GeneratorConfig.kt`

```kotlin
data class GeneratorConfig(
    val target: GeneratorTarget,
    val packageName: String,
    val outputDir: File,
    val style: CodeStyle = CodeStyle.MATERIAL3,
    val enableOptimization: Boolean = true,
    val generateComments: Boolean = true,
    val validateSchema: Boolean = true,
    val strictMode: Boolean = false,
    val minifyOutput: Boolean = false,
    val generateTests: Boolean = false
) {
    /**
     * Validate configuration
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        if (packageName.isBlank()) {
            errors.add("Package name cannot be empty")
        }

        if (!packageName.matches(Regex("^[a-z][a-z0-9.]*[a-z0-9]$"))) {
            errors.add("Invalid package name format: $packageName")
        }

        if (!outputDir.exists()) {
            errors.add("Output directory does not exist: ${outputDir.absolutePath}")
        }

        if (!outputDir.isDirectory) {
            errors.add("Output path is not a directory: ${outputDir.absolutePath}")
        }

        return errors
    }

    /**
     * Get output file path for a specific file name
     */
    fun outputFile(fileName: String): File {
        val packagePath = packageName.replace('.', '/')
        return File(outputDir, "$packagePath/$fileName")
    }

    companion object {
        /**
         * Create config for Kotlin Compose target
         */
        fun forKotlinCompose(
            packageName: String,
            outputDir: File
        ): GeneratorConfig =
            GeneratorConfig(
                target = GeneratorTarget.KOTLIN_COMPOSE,
                packageName = packageName,
                outputDir = outputDir,
                style = CodeStyle.MATERIAL3
            )

        /**
         * Create config for SwiftUI target
         */
        fun forSwiftUI(
            packageName: String,
            outputDir: File
        ): GeneratorConfig =
            GeneratorConfig(
                target = GeneratorTarget.SWIFT_UI,
                packageName = packageName,
                outputDir = outputDir,
                style = CodeStyle.CUPERTINO
            )
    }
}

enum class GeneratorTarget(val displayName: String) {
    KOTLIN_COMPOSE("Kotlin + Jetpack Compose"),
    SWIFT_UI("Swift + SwiftUI"),
    REACT_TYPESCRIPT("TypeScript + React");

    companion object {
        fun fromDisplayName(name: String): GeneratorTarget? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

enum class CodeStyle {
    MATERIAL3,      // Material Design 3 (Android)
    MATERIAL2,      // Material Design 2 (Android)
    CUPERTINO,      // iOS native style
    FLUENT          // Windows Fluent Design
}
```

### 30.2.4 Generated Code Model

**File:** `core/GeneratedCode.kt`

```kotlin
data class GeneratedCode(
    val files: List<GeneratedFile>,
    val metadata: GenerationMetadata
) {
    val totalLines: Int
        get() = files.sumOf { it.lineCount }

    /**
     * Write all generated files to disk
     */
    fun writeToDisk() {
        files.forEach { file ->
            file.writeToDisk()
        }
    }

    companion object {
        fun single(file: GeneratedFile, metadata: GenerationMetadata): GeneratedCode =
            GeneratedCode(listOf(file), metadata)
    }
}

data class GeneratedFile(
    val file: File,
    val content: String,
    val language: String,
    val isMain: Boolean = false
) {
    val fileName: String
        get() = file.name

    val lineCount: Int
        get() = content.lines().size

    /**
     * Write this file to disk
     */
    fun writeToDisk() {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
}

data class GenerationMetadata(
    val componentCount: Int,
    val stateVariableCount: Int,
    val callbackCount: Int,
    val generationTimeMs: Long
) {
    fun summary(): String = buildString {
        appendLine("Generation Metadata:")
        appendLine("  Components: $componentCount")
        appendLine("  State Variables: $stateVariableCount")
        appendLine("  Callbacks: $callbackCount")
        appendLine("  Generation Time: ${generationTimeMs}ms")
    }
}
```

---

## 30.3 Kotlin Compose Generator

### 30.3.1 Generator Implementation

**File:** `generators/kotlin/KotlinComposeGenerator.kt`

```kotlin
class KotlinComposeGenerator : CodeGenerator {
    override val target: GeneratorTarget = GeneratorTarget.KOTLIN_COMPOSE

    private val validator = KotlinComposeValidator()
    private val stateExtractor = KotlinStateExtractor()
    private val componentMapper = KotlinComponentMapper()

    override fun generate(ast: VosAstNode.App, config: GeneratorConfig): GeneratedCode {
        val startTime = System.currentTimeMillis()

        try {
            // Validate first if enabled
            if (config.validateSchema) {
                val validation = validate(ast)
                if (!validation.isValid) {
                    if (config.strictMode) {
                        throw GenerationException("Validation failed: ${validation.summary()}")
                    }
                }
            }

            // Extract state variables from AST
            val stateVars = stateExtractor.extract(ast)

            // Build imports
            val imports = buildImports(ast, config)

            // Generate composable function
            val composable = generateComposableFunction(ast, stateVars, config)

            // Combine into complete file
            val content = buildString {
                // Package declaration
                appendLine("package ${config.packageName}")
                appendLine()

                // Imports
                appendLine(imports)
                appendLine()

                // Generated code comment
                if (config.generateComments) {
                    appendLine("/**")
                    appendLine(" * Generated by MagicCode from ${ast.name}.vos")
                    appendLine(" * DO NOT EDIT - This file is auto-generated")
                    appendLine(" */")
                }

                // Main composable
                appendLine(composable)
            }

            // Create output file
            val fileName = "${ast.name}Screen.kt"
            val outputFile = config.outputFile(fileName)

            val generatedFile = GeneratedFile(
                file = outputFile,
                content = content,
                language = "Kotlin",
                isMain = true
            )

            val duration = System.currentTimeMillis() - startTime
            val metadata = GenerationMetadata(
                componentCount = ast.components.size,
                stateVariableCount = stateVars.size,
                callbackCount = countCallbacks(ast),
                generationTimeMs = duration
            )

            return GeneratedCode.single(generatedFile, metadata)
        } catch (e: Exception) {
            throw GenerationException("Failed to generate Kotlin Compose code", e)
        }
    }

    override fun validate(ast: VosAstNode.App): ValidationResult {
        return validator.validate(ast)
    }

    override fun info(): GeneratorInfo {
        return GeneratorInfo(
            name = "KotlinComposeGenerator",
            version = "1.0.0",
            target = GeneratorTarget.KOTLIN_COMPOSE,
            supportedComponents = listOf(
                "ColorPicker",
                "Preferences",
                "Text",
                "Button",
                "Container",
                "TextField",
                "Checkbox",
                "ListView",
                "Database",
                "Dialog"
            ),
            description = "Generates Kotlin + Jetpack Compose code for Android"
        )
    }

    private fun buildImports(ast: VosAstNode.App, config: GeneratorConfig): String {
        return buildString {
            // Compose imports
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.foundation.layout.*")

            // Material3 imports
            if (config.style == CodeStyle.MATERIAL3) {
                appendLine("import androidx.compose.material3.*")
            }

            // Component-specific imports
            val componentTypes = ast.components.map { it.type }.toSet()
            if ("ColorPicker" in componentTypes) {
                appendLine("import com.augmentalis.voiceos.colorpicker.ColorPickerView")
                appendLine("import com.augmentalis.voiceos.colorpicker.ColorRGBA")
            }
            if ("Preferences" in componentTypes) {
                appendLine("import com.augmentalis.voiceos.preferences.Preferences")
            }
        }
    }

    private fun generateComposableFunction(
        ast: VosAstNode.App,
        stateVars: List<StateVariable>,
        config: GeneratorConfig
    ): String {
        return buildString {
            appendLine("@Composable")
            appendLine("fun ${ast.name}Screen() {")

            // State variables
            if (stateVars.isNotEmpty()) {
                stateVars.forEach { stateVar ->
                    appendLine("    var ${stateVar.name} by remember { mutableStateOf(${stateVar.initialValue}) }")
                }
                appendLine()
            }

            // Root container
            appendLine("    Column(")
            appendLine("        modifier = Modifier.fillMaxSize()")
            appendLine("    ) {")

            // Generate components
            ast.components.forEach { component ->
                val componentCode = componentMapper.map(component, stateVars, indent = 8)
                appendLine(componentCode)
            }

            appendLine("    }")
            appendLine("}")
        }
    }

    private fun countCallbacks(ast: VosAstNode.App): Int {
        return ast.components.sumOf { it.callbacks.size }
    }
}

data class StateVariable(
    val name: String,
    val type: String,
    val initialValue: String
)
```

### 30.3.2 State Extraction

**File:** `generators/kotlin/KotlinStateExtractor.kt`

```kotlin
class KotlinStateExtractor {
    /**
     * Extract state variables from AST
     */
    fun extract(ast: VosAstNode.App): List<StateVariable> {
        val stateVars = mutableListOf<StateVariable>()

        for (component in ast.components) {
            // Extract from component properties
            component.properties.forEach { (name, value) ->
                if (isStateProperty(name)) {
                    val stateVar = createStateVariable(name, value, component.type)
                    if (stateVar != null) {
                        stateVars.add(stateVar)
                    }
                }
            }

            // Extract from callbacks
            component.callbacks.forEach { (callbackName, lambda) ->
                val extractedStates = extractFromLambda(lambda)
                stateVars.addAll(extractedStates)
            }
        }

        return stateVars.distinctBy { it.name }
    }

    private fun isStateProperty(propertyName: String): Boolean {
        return propertyName in listOf(
            "value", "text", "checked", "selectedIndex",
            "selectedColor", "isOpen", "isVisible"
        )
    }

    private fun createStateVariable(
        propertyName: String,
        value: VosValue,
        componentType: String
    ): StateVariable? {
        return when (propertyName) {
            "value", "text" -> {
                val initialValue = when (value) {
                    is VosValue.StringValue -> "\"${value.value}\""
                    else -> "\"\""
                }
                StateVariable(
                    name = "${componentType.lowercase()}Text",
                    type = "String",
                    initialValue = initialValue
                )
            }
            "checked" -> {
                val initialValue = when (value) {
                    is VosValue.BoolValue -> value.value.toString()
                    else -> "false"
                }
                StateVariable(
                    name = "${componentType.lowercase()}Checked",
                    type = "Boolean",
                    initialValue = initialValue
                )
            }
            "selectedColor" -> {
                val initialValue = when (value) {
                    is VosValue.StringValue -> "Color(0xFF${value.value.removePrefix("#")})"
                    else -> "Color.White"
                }
                StateVariable(
                    name = "selectedColor",
                    type = "Color",
                    initialValue = initialValue
                )
            }
            else -> null
        }
    }

    private fun extractFromLambda(lambda: VosLambda): List<StateVariable> {
        val stateVars = mutableListOf<StateVariable>()

        for (statement in lambda.statements) {
            if (statement is VosStatement.Assignment) {
                val stateVar = StateVariable(
                    name = statement.target,
                    type = inferType(statement.value),
                    initialValue = getDefaultValue(statement.value)
                )
                stateVars.add(stateVar)
            }
        }

        return stateVars
    }

    private fun inferType(value: VosValue): String {
        return when (value) {
            is VosValue.StringValue -> "String"
            is VosValue.IntValue -> "Int"
            is VosValue.FloatValue -> "Float"
            is VosValue.BoolValue -> "Boolean"
            is VosValue.ListValue -> "List<Any>"
            is VosValue.ObjectValue -> "Map<String, Any>"
            is VosValue.NullValue -> "Any?"
        }
    }

    private fun getDefaultValue(value: VosValue): String {
        return when (value) {
            is VosValue.StringValue -> "\"${value.value}\""
            is VosValue.IntValue -> value.value.toString()
            is VosValue.FloatValue -> "${value.value}f"
            is VosValue.BoolValue -> value.value.toString()
            is VosValue.ListValue -> "emptyList()"
            is VosValue.ObjectValue -> "emptyMap()"
            is VosValue.NullValue -> "null"
        }
    }
}
```

### 30.3.3 Component Mapping

**File:** `generators/kotlin/KotlinComponentMapper.kt`

```kotlin
class KotlinComponentMapper {
    /**
     * Map DSL component to Kotlin Compose code
     */
    fun map(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int = 0
    ): String {
        return when (component.type) {
            "ColorPicker" -> mapColorPicker(component, stateVars, indent)
            "Button" -> mapButton(component, stateVars, indent)
            "TextField" -> mapTextField(component, stateVars, indent)
            "Text" -> mapText(component, stateVars, indent)
            "Row" -> mapRow(component, stateVars, indent)
            "Column" -> mapColumn(component, stateVars, indent)
            else -> throw GenerationException.unsupportedComponent(
                component.type,
                GeneratorTarget.KOTLIN_COMPOSE
            )
        }
    }

    private fun mapColorPicker(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)
        val initialColor = component.properties["initialColor"]
        val showAlpha = component.properties["showAlpha"]
        val showHex = component.properties["showHex"]

        return buildString {
            appendLine("${indentStr}ColorPickerView(")
            appendLine("${indentStr}    selectedColor = selectedColor,")
            if (showAlpha is VosValue.BoolValue) {
                appendLine("${indentStr}    showAlpha = ${showAlpha.value},")
            }
            if (showHex is VosValue.BoolValue) {
                appendLine("${indentStr}    showHex = ${showHex.value},")
            }

            // Callback
            val onColorChange = component.callbacks["onColorChange"]
            if (onColorChange != null) {
                appendLine("${indentStr}    onColorChanged = { color ->")
                appendLine("${indentStr}        selectedColor = color")

                // Generate callback statements
                onColorChange.statements.forEach { statement ->
                    val statementCode = mapStatement(statement, indent + 8)
                    appendLine(statementCode)
                }

                appendLine("${indentStr}    }")
            }

            appendLine("${indentStr})")
        }
    }

    private fun mapButton(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)
        val text = component.properties["text"] as? VosValue.StringValue
        val enabled = component.properties["enabled"] as? VosValue.BoolValue

        return buildString {
            appendLine("${indentStr}Button(")

            // onClick callback
            val onClick = component.callbacks["onClick"]
            if (onClick != null) {
                appendLine("${indentStr}    onClick = {")
                onClick.statements.forEach { statement ->
                    val statementCode = mapStatement(statement, indent + 8)
                    appendLine(statementCode)
                }
                appendLine("${indentStr}    },")
            }

            if (enabled != null) {
                appendLine("${indentStr}    enabled = ${enabled.value}")
            }

            appendLine("${indentStr}) {")
            appendLine("${indentStr}    Text(\"${text?.value ?: ""}\")")
            appendLine("${indentStr}}")
        }
    }

    private fun mapTextField(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)
        val placeholder = component.properties["placeholder"] as? VosValue.StringValue
        val label = component.properties["label"] as? VosValue.StringValue

        return buildString {
            appendLine("${indentStr}TextField(")
            appendLine("${indentStr}    value = textfieldText,")
            appendLine("${indentStr}    onValueChange = { textfieldText = it },")

            if (label != null) {
                appendLine("${indentStr}    label = { Text(\"${label.value}\") },")
            }

            if (placeholder != null) {
                appendLine("${indentStr}    placeholder = { Text(\"${placeholder.value}\") }")
            }

            appendLine("${indentStr})")
        }
    }

    private fun mapText(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)
        val text = component.properties["text"] as? VosValue.StringValue
        val fontSize = component.properties["fontSize"] as? VosValue.IntValue

        return buildString {
            appendLine("${indentStr}Text(")
            appendLine("${indentStr}    text = \"${text?.value ?: ""}\",")

            if (fontSize != null) {
                appendLine("${indentStr}    fontSize = ${fontSize.value}.sp")
            }

            appendLine("${indentStr})")
        }
    }

    private fun mapRow(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)

        return buildString {
            appendLine("${indentStr}Row(")
            appendLine("${indentStr}    modifier = Modifier.fillMaxWidth()")
            appendLine("${indentStr}) {")

            // Render children
            component.children.forEach { child ->
                val childCode = map(child, stateVars, indent + 4)
                appendLine(childCode)
            }

            appendLine("${indentStr}}")
        }
    }

    private fun mapColumn(
        component: VosAstNode.Component,
        stateVars: List<StateVariable>,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)

        return buildString {
            appendLine("${indentStr}Column(")
            appendLine("${indentStr}    modifier = Modifier.fillMaxWidth()")
            appendLine("${indentStr}) {")

            // Render children
            component.children.forEach { child ->
                val childCode = map(child, stateVars, indent + 4)
                appendLine(childCode)
            }

            appendLine("${indentStr}}")
        }
    }

    private fun mapStatement(
        statement: VosStatement,
        indent: Int
    ): String {
        val indentStr = " ".repeat(indent)

        return when (statement) {
            is VosStatement.FunctionCall -> {
                val args = statement.args.joinToString(", ") { arg ->
                    when (arg) {
                        is VosValue.StringValue -> "\"${arg.value}\""
                        is VosValue.IntValue -> arg.value.toString()
                        is VosValue.FloatValue -> "${arg.value}f"
                        is VosValue.BoolValue -> arg.value.toString()
                        else -> "null"
                    }
                }
                "${indentStr}${statement.target}($args)"
            }
            is VosStatement.Assignment -> {
                val valueStr = when (statement.value) {
                    is VosValue.StringValue -> "\"${(statement.value as VosValue.StringValue).value}\""
                    is VosValue.IntValue -> (statement.value as VosValue.IntValue).value.toString()
                    else -> "null"
                }
                "${indentStr}${statement.target} = $valueStr"
            }
            is VosStatement.Return -> {
                "${indentStr}return"
            }
        }
    }
}
```

---

## 30.4 CLI Integration

**File:** `cli/MagicCodeCLI.kt`

MagicCode provides a standalone CLI tool for code generation:

```kotlin
class MagicCodeCLI {
    private val generator = MagicCodeGenerator()

    fun run(args: Array<String>): Int {
        if (args.isEmpty()) {
            printHelp()
            return 1
        }

        return try {
            when (args[0]) {
                "generate" -> handleGenerate(args.drop(1))
                "validate" -> handleValidate(args.drop(1))
                "batch" -> handleBatch(args.drop(1))
                "info" -> handleInfo()
                "help", "--help", "-h" -> {
                    printHelp()
                    0
                }
                "version", "--version", "-v" -> {
                    printVersion()
                    0
                }
                else -> {
                    println("Unknown command: ${args[0]}")
                    printHelp()
                    1
                }
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            1
        }
    }

    private fun handleGenerate(args: List<String>): Int {
        val options = parseOptions(args)

        val inputFile = options["input"]?.let { File(it) }
            ?: return error("Missing required option: --input")

        val outputDir = options["output"]?.let { File(it) }
            ?: return error("Missing required option: --output")

        val packageName = options["package"]
            ?: return error("Missing required option: --package")

        val targetName = options["target"] ?: "kotlin"
        val target = GeneratorTarget.fromDisplayName(targetName)
            ?: return error("Unknown target: $targetName")

        println("Generating code from ${inputFile.name}...")
        println("Target: ${target.displayName}")
        println("Package: $packageName")
        println("Output: ${outputDir.absolutePath}")

        val config = GeneratorConfig(
            target = target,
            packageName = packageName,
            outputDir = outputDir,
            enableOptimization = options.containsKey("optimize"),
            generateComments = !options.containsKey("no-comments"),
            validateSchema = !options.containsKey("no-validate"),
            strictMode = options.containsKey("strict")
        )

        val result = generator.generate(inputFile, config)

        if (result.success) {
            result.generatedCode?.writeToDisk()
            println()
            println("✓ Success!")
            println("Generated files:")
            result.generatedCode?.files?.forEach { file ->
                println("  - ${file.fileName} (${file.lineCount} lines)")
            }
            println()
            println(result.generatedCode?.metadata?.summary())
            return 0
        } else {
            println()
            println("✗ Generation failed:")
            println("  ${result.error}")
            return 1
        }
    }

    private fun printHelp() {
        println("""
            MagicCode - VoiceOS Code Generator

            USAGE:
                magiccode <COMMAND> [OPTIONS]

            COMMANDS:
                generate        Generate code from a .vos file
                validate        Validate a .vos file
                batch           Generate code from multiple .vos files
                info            Show generator information
                help            Show this help message
                version         Show version information

            GENERATE OPTIONS:
                --input <FILE>      Input .vos file (required)
                --output <DIR>      Output directory (required)
                --package <NAME>    Package name (required)
                --target <TARGET>   Target platform (default: kotlin)
                                    Options: kotlin, swiftui, react
                --optimize          Enable code optimization
                --no-comments       Disable comment generation
                --no-validate       Skip validation
                --strict            Fail on warnings

            EXAMPLES:
                # Generate Kotlin code
                magiccode generate --input app.vos --output src/main/kotlin --package com.example.app

                # Generate with optimization
                magiccode generate --input app.vos --output src/ --package com.example --optimize

                # Validate only
                magiccode validate --input app.vos --target kotlin

                # Batch generation
                magiccode batch --input-dir vos/ --output-dir src/ --package com.example
        """.trimIndent())
    }
}

fun main(args: Array<String>) {
    val cli = MagicCodeCLI()
    val exitCode = cli.run(args)
    System.exit(exitCode)
}
```

### CLI Usage Examples

```bash
# Generate Kotlin Compose code
magiccode generate \
  --input colorpicker.vos \
  --output src/main/kotlin \
  --package com.voiceos.colorpicker

# Generate with optimization
magiccode generate \
  --input app.vos \
  --output src/ \
  --package com.example \
  --optimize

# Validate without generating
magiccode validate --input app.vos --target kotlin

# Batch generation from directory
magiccode batch \
  --input-dir vos/ \
  --output-dir src/ \
  --package com.example

# Show available generators
magiccode info
```

---

## 30.5 VOS4 Integration

### 30.5.1 Build System Integration

MagicCode integrates with VOS4's Gradle build system:

```kotlin
// File: VOS4/buildSrc/src/main/kotlin/MagicCodePlugin.kt

class MagicCodePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateFromVos", MagicCodeTask::class.java) {
            group = "codegen"
            description = "Generate Kotlin code from .vos files"
        }
    }
}

abstract class MagicCodeTask : DefaultTask() {
    @InputDirectory
    val vosDir = project.layout.projectDirectory.dir("src/main/vos")

    @OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("generated/source/vos")

    @TaskAction
    fun generate() {
        val generator = MagicCodeGenerator()

        val vosFiles = vosDir.asFileTree.matching {
            include("**/*.vos")
        }.files.toList()

        val config = GeneratorConfig.forKotlinCompose(
            packageName = "com.augmentalis.voiceoscore.generated",
            outputDir = outputDir.get().asFile
        )

        val result = generator.generateBatch(vosFiles, config)

        println(result.summary())

        if (!result.allSucceeded) {
            throw GradleException("Code generation failed")
        }
    }
}
```

### 30.5.2 Gradle Configuration

```kotlin
// File: VOS4/modules/apps/VoiceOSCore/build.gradle.kts

plugins {
    id("com.android.application")
    kotlin("android")
    id("magiccode-plugin")
}

android {
    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/source/vos")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("generateFromVos")
}
```

### 30.5.3 Runtime Code Generation

VOS4 can also generate code at runtime for voice-created UIs:

```kotlin
// File: VOS4/modules/apps/VoiceOSCore/src/main/kotlin/com/augmentalis/voiceoscore/codegen/RuntimeCodegen.kt

object RuntimeCodegen {
    private val generator = MagicCodeGenerator()

    /**
     * Generate code at runtime and compile dynamically
     */
    suspend fun generateAndCompile(dslSource: String, appId: String): CompiledApp {
        // Create temporary .vos file
        val vosFile = File.createTempFile("app_$appId", ".vos")
        vosFile.writeText(dslSource)

        // Generate code
        val config = GeneratorConfig.forKotlinCompose(
            packageName = "com.voiceos.dynamic.$appId",
            outputDir = File(filesDir, "dynamic/$appId")
        )

        val result = generator.generate(vosFile, config)

        if (!result.success) {
            throw RuntimeException("Code generation failed: ${result.error}")
        }

        // Compile generated code (requires dynamic compilation support)
        val compiledClass = compileKotlin(result.generatedCode!!)

        return CompiledApp(
            appId = appId,
            compiledClass = compiledClass,
            generatedFiles = result.generatedCode.files
        )
    }

    private fun compileKotlin(code: GeneratedCode): KClass<*> {
        // Dynamic compilation using kotlin-compiler-embeddable
        // Implementation omitted for brevity
        throw NotImplementedError("Dynamic compilation not yet implemented")
    }
}

data class CompiledApp(
    val appId: String,
    val compiledClass: KClass<*>,
    val generatedFiles: List<GeneratedFile>
)
```

---

## 30.6 Complete Example

### 30.6.1 Input: Color Picker DSL

```
# File: colorpicker.vos

App {
    id: "com.voiceos.colorpicker"
    name: "ColorPicker"
    runtime: "MagicUI"

    ColorPicker {
        id: "mainPicker"
        initialColor: "#FF5733"
        showAlpha: true
        showHex: true

        onColorChange: (color) => {
            VoiceOS.speak("Color changed")
            Logger.log("Color: " + color)
        }
    }

    VoiceCommands {
        "change color" => openColorPicker
        "reset" => resetColor
    }
}
```

### 30.6.2 Output: Generated Kotlin Code

```kotlin
// File: ColorPickerScreen.kt (generated)

package com.voiceos.colorpicker

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.augmentalis.voiceos.colorpicker.ColorPickerView
import com.augmentalis.voiceos.colorpicker.ColorRGBA

/**
 * Generated by MagicCode from ColorPicker.vos
 * DO NOT EDIT - This file is auto-generated
 */

@Composable
fun ColorPickerScreen() {
    var selectedColor by remember { mutableStateOf(Color(0xFFFF5733)) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ColorPickerView(
            selectedColor = selectedColor,
            showAlpha = true,
            showHex = true,
            onColorChanged = { color ->
                selectedColor = color
                VoiceOS.speak("Color changed")
                Logger.log("Color: " + color)
            }
        )
    }
}
```

### 30.6.3 Generation Command

```bash
magiccode generate \
  --input colorpicker.vos \
  --output src/main/kotlin \
  --package com.voiceos.colorpicker \
  --target kotlin \
  --optimize
```

### 30.6.4 Generation Output

```
Generating code from colorpicker.vos...
Target: Kotlin + Jetpack Compose
Package: com.voiceos.colorpicker
Output: /path/to/src/main/kotlin

✓ Success!
Generated files:
  - ColorPickerScreen.kt (28 lines)

Generation Metadata:
  Components: 1
  State Variables: 1
  Callbacks: 1
  Generation Time: 127ms
```

---

## 30.7 Summary

MagicCode provides a complete code generation pipeline with:

1. **Multi-Target Support**: Kotlin Compose, SwiftUI, TypeScript React
2. **Reusable Parser**: Leverages MagicUI's DSL parser (VosTokenizer, VosParser)
3. **AST Transformation**: State extraction, component mapping, callback translation
4. **Template System**: Code builders for each target platform
5. **Validation**: Schema validation with errors and warnings
6. **Optimization**: Optional code optimization passes
7. **CLI Tool**: Standalone command-line interface
8. **Build Integration**: Gradle plugin for VOS4
9. **Runtime Generation**: Dynamic code generation for voice-created UIs

**Key Files:**
- Main API: `MagicCodeGenerator.kt`
- Generator Interface: `core/CodeGenerator.kt`
- Kotlin Generator: `generators/kotlin/KotlinComposeGenerator.kt`
- State Extraction: `generators/kotlin/KotlinStateExtractor.kt`
- Component Mapping: `generators/kotlin/KotlinComponentMapper.kt`
- CLI Tool: `cli/MagicCodeCLI.kt`

**Next Chapter:** Chapter 31 will explore AVA & AVAConnect integration - the connectivity layer enabling VOS4 to communicate with AVA AI platform and remote devices.

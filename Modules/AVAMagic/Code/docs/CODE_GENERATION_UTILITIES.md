# AvaCode Code Generation Utilities

**Technical Implementation Guide**

Version: 1.0.0
Last Updated: 2025-10-28

---

## Table of Contents

1. [Code Generator Interface](#code-generator-interface)
2. [Template Engine Implementation](#template-engine-implementation)
3. [Component Mapper Implementation](#component-mapper-implementation)
4. [Type System](#type-system)
5. [Validation Framework](#validation-framework)
6. [Code Formatting](#code-formatting)
7. [File Structure Generation](#file-structure-generation)
8. [Build System Integration](#build-system-integration)

---

## Code Generator Interface

### Core Interface

```kotlin
package com.augmentalis.avacode.codegen

/**
 * Base interface for all code generators.
 */
interface CodeGenerator {
    /**
     * Target platform for this generator.
     */
    val target: TargetPlatform

    /**
     * Generate code for a component model.
     *
     * @param component Component to generate code for
     * @param context Generation context with configuration
     * @return Generated code result
     */
    fun generate(
        component: ComponentModel,
        context: GenerationContext
    ): CodeGenResult<GeneratedFile>

    /**
     * Generate code for multiple components.
     *
     * @param components Components to generate
     * @param context Generation context
     * @return List of generated files or errors
     */
    fun generateMultiple(
        components: List<ComponentModel>,
        context: GenerationContext
    ): CodeGenResult<List<GeneratedFile>> {
        val results = components.map { generate(it, context) }
        val errors = results.filterIsInstance<CodeGenResult.Failure>()
            .flatMap { it.errors }

        return if (errors.isEmpty()) {
            val files = results.filterIsInstance<CodeGenResult.Success<GeneratedFile>>()
                .map { it.value }
            CodeGenResult.Success(files)
        } else {
            CodeGenResult.Failure(errors)
        }
    }

    /**
     * Validate component before generation.
     */
    fun validate(component: ComponentModel): ValidationResult
}

/**
 * Target platform enum.
 */
enum class TargetPlatform {
    KOTLIN_COMPOSE,
    SWIFT_UI,
    REACT_TYPESCRIPT
}

/**
 * Generation context with configuration.
 */
data class GenerationContext(
    val packageName: String,
    val outputDirectory: File,
    val componentRegistry: ComponentRegistry,
    val formatCode: Boolean = true,
    val generateTests: Boolean = false,
    val generateDocumentation: Boolean = false,
    val minifyOutput: Boolean = false,
    val targetVersion: String? = null,
    val customTemplates: Map<String, String> = emptyMap()
)

/**
 * Generated file result.
 */
data class GeneratedFile(
    val path: String,
    val content: String,
    val language: FileLanguage,
    val imports: Set<String> = emptySet(),
    val metadata: FileMetadata = FileMetadata()
)

enum class FileLanguage {
    KOTLIN,
    SWIFT,
    TYPESCRIPT,
    CSS,
    JSON
}

data class FileMetadata(
    val generatedAt: Long = System.currentTimeMillis(),
    val generator: String = "AvaCode",
    val version: String = "1.0.0",
    val sourceComponent: String? = null
)

/**
 * Code generation result.
 */
sealed class CodeGenResult<out T> {
    data class Success<T>(val value: T) : CodeGenResult<T>()
    data class Failure(val errors: List<CodeGenError>) : CodeGenResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw CodeGenerationException(errors)
    }
}

/**
 * Code generation error.
 */
sealed class CodeGenError {
    data class InvalidComponent(
        val componentType: String,
        val message: String
    ) : CodeGenError()

    data class InvalidProperty(
        val componentType: String,
        val propertyName: String,
        val expectedType: PropertyType,
        val actualValue: String,
        val hint: String? = null
    ) : CodeGenError()

    data class MissingRequiredProperty(
        val componentType: String,
        val propertyName: String
    ) : CodeGenError()

    data class UnsupportedCallback(
        val componentType: String,
        val callbackName: String,
        val target: TargetPlatform
    ) : CodeGenError()

    data class TemplateError(
        val templateName: String,
        val message: String
    ) : CodeGenError()

    data class FormattingError(
        val fileName: String,
        val message: String
    ) : CodeGenError()
}

class CodeGenerationException(val errors: List<CodeGenError>) :
    Exception("Code generation failed with ${errors.size} error(s):\n${errors.joinToString("\n")}")

/**
 * Validation result.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(vararg errors: ValidationError) =
            ValidationResult(false, errors.toList())
    }
}

data class ValidationError(
    val message: String,
    val path: String? = null,
    val hint: String? = null
)

data class ValidationWarning(
    val message: String,
    val path: String? = null
)
```

---

## Template Engine Implementation

### Template System

```kotlin
package com.augmentalis.avacode.codegen.template

/**
 * Template engine for code generation.
 */
interface TemplateEngine {
    /**
     * Render template with data.
     */
    fun render(template: String, data: Map<String, Any?>): String

    /**
     * Load template from file.
     */
    fun loadTemplate(name: String): String?

    /**
     * Register helper function.
     */
    fun registerHelper(name: String, helper: TemplateHelper)
}

/**
 * Template helper function.
 */
fun interface TemplateHelper {
    fun apply(context: Any?, options: Map<String, Any?>): Any?
}

/**
 * Simple template engine implementation using string replacement.
 */
class SimpleTemplateEngine(
    private val templateRoot: File
) : TemplateEngine {

    private val helpers = mutableMapOf<String, TemplateHelper>()

    init {
        // Register built-in helpers
        registerHelper("uppercase") { value, _ ->
            value?.toString()?.uppercase()
        }

        registerHelper("lowercase") { value, _ ->
            value?.toString()?.lowercase()
        }

        registerHelper("capitalize") { value, _ ->
            value?.toString()?.replaceFirstChar { it.uppercase() }
        }

        registerHelper("camelCase") { value, _ ->
            value?.toString()?.toCamelCase()
        }

        registerHelper("snakeCase") { value, _ ->
            value?.toString()?.toSnakeCase()
        }
    }

    override fun render(template: String, data: Map<String, Any?>): String {
        var result = template

        // Replace simple variables: {{variable}}
        val variableRegex = """\{\{(\w+)\}\}""".toRegex()
        result = variableRegex.replace(result) { match ->
            val key = match.groupValues[1]
            data[key]?.toString() ?: ""
        }

        // Replace helper functions: {{helper variable}}
        val helperRegex = """\{\{(\w+)\s+(\w+)\}\}""".toRegex()
        result = helperRegex.replace(result) { match ->
            val helperName = match.groupValues[1]
            val variableName = match.groupValues[2]
            val value = data[variableName]
            val helper = helpers[helperName]

            helper?.apply(value, emptyMap())?.toString() ?: ""
        }

        // Replace conditionals: {{#if variable}}...{{/if}}
        val ifRegex = """\{\{#if\s+(\w+)\}\}(.*?)\{\{/if\}\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        result = ifRegex.replace(result) { match ->
            val variableName = match.groupValues[1]
            val content = match.groupValues[2]
            val value = data[variableName]

            if (value != null && value != false) content else ""
        }

        // Replace loops: {{#each items}}...{{/each}}
        val eachRegex = """\{\{#each\s+(\w+)\}\}(.*?)\{\{/each\}\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        result = eachRegex.replace(result) { match ->
            val variableName = match.groupValues[1]
            val itemTemplate = match.groupValues[2]
            val items = data[variableName] as? List<*> ?: emptyList<Any>()

            items.joinToString("\n") { item ->
                when (item) {
                    is Map<*, *> -> render(itemTemplate, item as Map<String, Any?>)
                    else -> itemTemplate.replace("{{this}}", item.toString())
                }
            }
        }

        return result
    }

    override fun loadTemplate(name: String): String? {
        val file = File(templateRoot, name)
        return if (file.exists()) file.readText() else null
    }

    override fun registerHelper(name: String, helper: TemplateHelper) {
        helpers[name] = helper
    }
}

// String extensions for case conversion
private fun String.toCamelCase(): String {
    return split("_", "-", " ")
        .mapIndexed { index, word ->
            if (index == 0) word.lowercase()
            else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
}

private fun String.toSnakeCase(): String {
    return replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
```

### Template Examples

#### Kotlin Component Template

```kotlin
// templates/kotlin/component.kt.template
package {{packageName}}

{{#each imports}}
import {{this}}
{{/each}}

/**
 * {{componentName}} - {{description}}
 */
@Composable
fun {{componentName}}(
    modifier: Modifier = Modifier{{#each properties}},
    {{name}}: {{type}}{{#if hasDefault}} = {{defaultValue}}{{/if}}{{/each}}{{#each callbacks}},
    {{name}}: {{signature}}{{#if hasDefault}} = {}{{/if}}{{/each}}
) {
    {{#each stateVars}}
    var {{name}} by remember { mutableStateOf({{initialValue}}) }
    {{/each}}

    {{#each effects}}
    {{type}}({{key}}) {
        {{body}}
    }
    {{/each}}

    {{containerType}}(modifier = modifier) {
        {{body}}
    }
}

{{#if generatePreview}}
@Preview(showBackground = true)
@Composable
fun {{componentName}}Preview() {
    MaterialTheme {
        {{componentName}}()
    }
}
{{/if}}
```

#### SwiftUI Component Template

```swift
// templates/swift/component.swift.template
import SwiftUI

/// {{componentName}} - {{description}}
struct {{componentName}}: View {
    {{#each properties}}
    {{#if isState}}
    @State private var {{name}}: {{type}}{{#if hasDefault}} = {{defaultValue}}{{/if}}
    {{else}}
    let {{name}}: {{type}}
    {{/if}}
    {{/each}}

    {{#each callbacks}}
    var {{name}}: {{signature}}
    {{/each}}

    {{#each computed}}
    private var {{name}}: {{type}} {
        {{body}}
    }
    {{/each}}

    var body: some View {
        {{containerType}} {
            {{body}}
        }
        {{#each modifiers}}
        .{{name}}({{args}})
        {{/each}}
    }
}

{{#if generatePreview}}
#Preview {
    {{componentName}}()
}
{{/if}}
```

#### React Component Template

```typescript
// templates/typescript/component.tsx.template
import React, { {{hooks}} } from 'react';
{{#each imports}}
import {{this}};
{{/each}}

/**
 * {{componentName}} - {{description}}
 */
interface {{componentName}}Props {
  {{#each properties}}
  /** {{description}} */
  {{name}}{{#if optional}}?{{/if}}: {{type}};
  {{/each}}
  {{#each callbacks}}
  /** {{description}} */
  {{name}}?: {{signature}};
  {{/each}}
}

export const {{componentName}}: React.FC<{{componentName}}Props> = ({
  {{#each properties}}{{name}}{{#if hasDefault}} = {{defaultValue}}{{/if}},
  {{/each}}{{#each callbacks}}{{name}},
  {{/each}}
}) => {
  {{#each stateVars}}
  const [{{name}}, set{{capitalize name}}] = useState<{{type}}>({{initialValue}});
  {{/each}}

  {{#each effects}}
  useEffect(() => {
    {{body}}
  }, [{{dependencies}}]);
  {{/each}}

  {{#each handlers}}
  const {{name}} = useCallback({{params}} => {
    {{body}}
  }, [{{dependencies}}]);
  {{/each}}

  return (
    <{{containerType}}{{#if hasClassName}} className="{{className}}"{{/if}}>
      {{body}}
    </{{containerType}}>
  );
};
```

---

## Component Mapper Implementation

```kotlin
package com.augmentalis.avacode.codegen.mapper

/**
 * Maps ComponentModel to target-specific representations.
 */
interface ComponentMapper {
    val target: TargetPlatform

    fun mapComponent(component: ComponentModel): TargetComponent
    fun mapProperty(name: String, value: String, type: PropertyType): TargetProperty
    fun mapCallback(callback: CallbackDescriptor): TargetCallback
    fun mapChildren(children: List<ComponentModel>): List<TargetComponent>
}

/**
 * Target-specific component representation.
 */
data class TargetComponent(
    val name: String,
    val properties: List<TargetProperty>,
    val callbacks: List<TargetCallback>,
    val children: List<TargetComponent>,
    val imports: Set<String>,
    val stateVariables: List<StateVariable>,
    val effects: List<Effect>,
    val containerType: String? = null
)

data class TargetProperty(
    val name: String,
    val type: String,
    val value: String,
    val isRequired: Boolean = false,
    val defaultValue: String? = null
)

data class TargetCallback(
    val name: String,
    val signature: String,
    val parameters: List<CallbackParameter>
)

data class StateVariable(
    val name: String,
    val type: String,
    val initialValue: String,
    val mutable: Boolean = true
)

data class Effect(
    val type: EffectType,
    val key: String?,
    val body: String
)

enum class EffectType {
    ON_MOUNT,      // LaunchedEffect(Unit) / .onAppear / useEffect([], ...)
    ON_CHANGE,     // LaunchedEffect(key) / .onChange / useEffect([key], ...)
    ON_DISPOSE     // DisposableEffect / .onDisappear / cleanup
}

/**
 * Kotlin Compose mapper implementation.
 */
class KotlinComposeMapper(
    private val registry: ComponentRegistry
) : ComponentMapper {

    override val target = TargetPlatform.KOTLIN_COMPOSE

    override fun mapComponent(component: ComponentModel): TargetComponent {
        val descriptor = registry.get(component.type)
            ?: error("Unknown component type: ${component.type}")

        return when (component.type) {
            "Button" -> mapButton(component, descriptor)
            "Text" -> mapText(component, descriptor)
            "Container" -> mapContainer(component, descriptor)
            "ColorPicker" -> mapColorPicker(component, descriptor)
            else -> error("Unsupported component: ${component.type}")
        }
    }

    private fun mapButton(
        component: ComponentModel,
        descriptor: ComponentDescriptor
    ): TargetComponent {
        val text = component.properties["text"] ?: "Button"
        val enabled = component.properties["enabled"]?.toBoolean() ?: true

        return TargetComponent(
            name = "Button",
            properties = listOf(
                TargetProperty("onClick", "() -> Unit", "{}", false),
                TargetProperty("enabled", "Boolean", enabled.toString(), false)
            ),
            callbacks = descriptor.callbacks.map { (name, cb) ->
                TargetCallback(
                    name = name,
                    signature = mapCallbackSignature(cb),
                    parameters = cb.parameters
                )
            }.toList(),
            children = listOf(
                TargetComponent(
                    name = "Text",
                    properties = listOf(
                        TargetProperty("text", "String", "\"$text\"", true)
                    ),
                    callbacks = emptyList(),
                    children = emptyList(),
                    imports = setOf("androidx.compose.material3.Text"),
                    stateVariables = emptyList(),
                    effects = emptyList()
                )
            ),
            imports = setOf(
                "androidx.compose.material3.Button",
                "androidx.compose.material3.Text"
            ),
            stateVariables = emptyList(),
            effects = emptyList()
        )
    }

    private fun mapText(
        component: ComponentModel,
        descriptor: ComponentDescriptor
    ): TargetComponent {
        val text = component.properties["text"] ?: ""
        val size = component.properties["size"]?.toFloat() ?: 16f
        val color = component.properties["color"] ?: "#000000"

        return TargetComponent(
            name = "Text",
            properties = listOf(
                TargetProperty("text", "String", "\"$text\"", true),
                TargetProperty("fontSize", "TextUnit", "${size}.sp", false),
                TargetProperty("color", "Color", "Color(0x${color.removePrefix("#")})", false)
            ),
            callbacks = emptyList(),
            children = emptyList(),
            imports = setOf(
                "androidx.compose.material3.Text",
                "androidx.compose.ui.graphics.Color",
                "androidx.compose.ui.unit.sp"
            ),
            stateVariables = emptyList(),
            effects = emptyList()
        )
    }

    private fun mapContainer(
        component: ComponentModel,
        descriptor: ComponentDescriptor
    ): TargetComponent {
        val orientation = component.properties["orientation"] ?: "vertical"
        val containerType = if (orientation == "vertical") "Column" else "Row"

        return TargetComponent(
            name = containerType,
            properties = listOf(
                TargetProperty("modifier", "Modifier", "modifier", false)
            ),
            callbacks = emptyList(),
            children = component.children.map { mapComponent(it) },
            imports = setOf(
                "androidx.compose.foundation.layout.$containerType",
                "androidx.compose.ui.Modifier"
            ),
            stateVariables = emptyList(),
            effects = emptyList(),
            containerType = containerType
        )
    }

    private fun mapColorPicker(
        component: ComponentModel,
        descriptor: ComponentDescriptor
    ): TargetComponent {
        val initialColor = component.properties["initialColor"] ?: "#FFFFFF"
        val mode = component.properties["mode"] ?: "FULL"
        val showAlpha = component.properties["showAlpha"]?.toBoolean() ?: true

        return TargetComponent(
            name = "ColorPickerComponent",
            properties = listOf(
                TargetProperty("initialColor", "String", "\"$initialColor\"", false),
                TargetProperty("mode", "ColorPickerMode", "ColorPickerMode.$mode", false),
                TargetProperty("showAlpha", "Boolean", showAlpha.toString(), false)
            ),
            callbacks = descriptor.callbacks.map { (name, cb) ->
                TargetCallback(
                    name = name,
                    signature = mapCallbackSignature(cb),
                    parameters = cb.parameters
                )
            }.toList(),
            children = emptyList(),
            imports = setOf(
                "androidx.compose.runtime.*",
                "androidx.compose.ui.graphics.Color",
                "com.augmentalis.voiceos.colorpicker.*"
            ),
            stateVariables = listOf(
                StateVariable("selectedColor", "Color", "Color(0xFF${initialColor.removePrefix("#")})", true),
                StateVariable("showPicker", "Boolean", "false", true)
            ),
            effects = listOf(
                Effect(EffectType.ON_CHANGE, "showPicker", "if (showPicker) picker.show() else picker.hide()"),
                Effect(EffectType.ON_DISPOSE, null, "picker.dispose()")
            )
        )
    }

    override fun mapProperty(
        name: String,
        value: String,
        type: PropertyType
    ): TargetProperty {
        val kotlinType = when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Float"
            PropertyType.BOOLEAN -> "Boolean"
            PropertyType.COLOR -> "Color"
            PropertyType.ENUM -> "String" // Could be more specific
        }

        val kotlinValue = when (type) {
            PropertyType.STRING -> "\"$value\""
            PropertyType.COLOR -> "Color(0xFF${value.removePrefix("#")})"
            PropertyType.BOOLEAN -> value
            else -> value
        }

        return TargetProperty(name, kotlinType, kotlinValue, false)
    }

    override fun mapCallback(callback: CallbackDescriptor): TargetCallback {
        return TargetCallback(
            name = callback.name,
            signature = mapCallbackSignature(callback),
            parameters = callback.parameters
        )
    }

    override fun mapChildren(children: List<ComponentModel>): List<TargetComponent> {
        return children.map { mapComponent(it) }
    }

    private fun mapCallbackSignature(callback: CallbackDescriptor): String {
        if (callback.parameters.isEmpty()) {
            return "() -> Unit"
        }

        val params = callback.parameters.joinToString(", ") { param ->
            "${param.name}: ${mapCallbackParameterType(param.type)}"
        }

        return "($params) -> Unit"
    }

    private fun mapCallbackParameterType(type: PropertyType): String {
        return when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Float"
            PropertyType.BOOLEAN -> "Boolean"
            PropertyType.COLOR -> "ColorRGBA"
            PropertyType.ENUM -> "String"
        }
    }
}

/**
 * SwiftUI mapper implementation.
 */
class SwiftUIMapper(
    private val registry: ComponentRegistry
) : ComponentMapper {

    override val target = TargetPlatform.SWIFT_UI

    override fun mapComponent(component: ComponentModel): TargetComponent {
        // Similar implementation for SwiftUI
        TODO("Implement SwiftUI mapping")
    }

    override fun mapProperty(name: String, value: String, type: PropertyType): TargetProperty {
        val swiftType = when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Double"
            PropertyType.BOOLEAN -> "Bool"
            PropertyType.COLOR -> "Color"
            PropertyType.ENUM -> "String"
        }

        val swiftValue = when (type) {
            PropertyType.STRING -> "\"$value\""
            PropertyType.COLOR -> "Color(hex: \"$value\")"
            PropertyType.BOOLEAN -> value
            else -> value
        }

        return TargetProperty(name, swiftType, swiftValue, false)
    }

    override fun mapCallback(callback: CallbackDescriptor): TargetCallback {
        return TargetCallback(
            name = callback.name,
            signature = mapCallbackSignature(callback),
            parameters = callback.parameters
        )
    }

    override fun mapChildren(children: List<ComponentModel>): List<TargetComponent> {
        return children.map { mapComponent(it) }
    }

    private fun mapCallbackSignature(callback: CallbackDescriptor): String {
        if (callback.parameters.isEmpty()) {
            return "() -> Void"
        }

        val params = callback.parameters.joinToString(", ") { param ->
            "${param.name}: ${mapCallbackParameterType(param.type)}"
        }

        return "($params) -> Void"
    }

    private fun mapCallbackParameterType(type: PropertyType): String {
        return when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Double"
            PropertyType.BOOLEAN -> "Bool"
            PropertyType.COLOR -> "Color"
            PropertyType.ENUM -> "String"
        }
    }
}
```

---

## Type System

```kotlin
package com.augmentalis.avacode.codegen.types

/**
 * Type converter for cross-platform type mapping.
 */
class TypeConverter {

    fun convertType(
        type: PropertyType,
        target: TargetPlatform,
        nullable: Boolean = false
    ): String {
        val baseType = when (target) {
            TargetPlatform.KOTLIN_COMPOSE -> convertToKotlin(type)
            TargetPlatform.SWIFT_UI -> convertToSwift(type)
            TargetPlatform.REACT_TYPESCRIPT -> convertToTypeScript(type)
        }

        return if (nullable) {
            when (target) {
                TargetPlatform.KOTLIN_COMPOSE -> "$baseType?"
                TargetPlatform.SWIFT_UI -> "$baseType?"
                TargetPlatform.REACT_TYPESCRIPT -> "$baseType | null"
            }
        } else {
            baseType
        }
    }

    private fun convertToKotlin(type: PropertyType): String {
        return when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Float"
            PropertyType.BOOLEAN -> "Boolean"
            PropertyType.COLOR -> "Color"
            PropertyType.ENUM -> "String" // Or specific enum type
        }
    }

    private fun convertToSwift(type: PropertyType): String {
        return when (type) {
            PropertyType.STRING -> "String"
            PropertyType.INT -> "Int"
            PropertyType.FLOAT -> "Double"
            PropertyType.BOOLEAN -> "Bool"
            PropertyType.COLOR -> "Color"
            PropertyType.ENUM -> "String"
        }
    }

    private fun convertToTypeScript(type: PropertyType): String {
        return when (type) {
            PropertyType.STRING -> "string"
            PropertyType.INT -> "number"
            PropertyType.FLOAT -> "number"
            PropertyType.BOOLEAN -> "boolean"
            PropertyType.COLOR -> "string"
            PropertyType.ENUM -> "string"
        }
    }

    fun convertValue(
        value: String,
        type: PropertyType,
        target: TargetPlatform
    ): String {
        return when (type) {
            PropertyType.STRING -> "\"$value\""
            PropertyType.COLOR -> convertColorValue(value, target)
            PropertyType.BOOLEAN, PropertyType.INT, PropertyType.FLOAT -> value
            PropertyType.ENUM -> when (target) {
                TargetPlatform.KOTLIN_COMPOSE -> value // EnumClass.VALUE
                TargetPlatform.SWIFT_UI -> value
                TargetPlatform.REACT_TYPESCRIPT -> "\"$value\""
            }
        }
    }

    private fun convertColorValue(hex: String, target: TargetPlatform): String {
        val cleanHex = hex.removePrefix("#")
        return when (target) {
            TargetPlatform.KOTLIN_COMPOSE -> "Color(0xFF$cleanHex)"
            TargetPlatform.SWIFT_UI -> "Color(hex: \"$hex\")"
            TargetPlatform.REACT_TYPESCRIPT -> "\"$hex\""
        }
    }
}
```

---

## Validation Framework

```kotlin
package com.augmentalis.avacode.codegen.validation

/**
 * Component validator.
 */
class ComponentValidator(
    private val registry: ComponentRegistry
) {

    fun validate(component: ComponentModel): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Get component descriptor
        val descriptor = registry.get(component.type)
        if (descriptor == null) {
            errors.add(
                ValidationError(
                    "Unknown component type: ${component.type}",
                    path = component.uuid,
                    hint = "Available types: ${registry.getAllTypes().joinToString(", ")}"
                )
            )
            return ValidationResult(false, errors, warnings)
        }

        // Validate required properties
        descriptor.properties.values
            .filter { it.required }
            .forEach { prop ->
                if (!component.properties.containsKey(prop.name)) {
                    errors.add(
                        ValidationError(
                            "Missing required property '${prop.name}' in ${component.type}",
                            path = component.uuid,
                            hint = prop.description
                        )
                    )
                }
            }

        // Validate property types
        component.properties.forEach { (name, value) ->
            val propDescriptor = descriptor.properties[name]
            if (propDescriptor == null) {
                warnings.add(
                    ValidationWarning(
                        "Unknown property '$name' in ${component.type}",
                        path = component.uuid
                    )
                )
            } else {
                val typeValid = validatePropertyType(value, propDescriptor.type)
                if (!typeValid) {
                    errors.add(
                        ValidationError(
                            "Invalid value for property '$name': expected ${propDescriptor.type}, got '$value'",
                            path = component.uuid,
                            hint = propDescriptor.description
                        )
                    )
                }
            }
        }

        // Validate enum values
        component.properties.forEach { (name, value) ->
            val propDescriptor = descriptor.properties[name]
            if (propDescriptor?.type == PropertyType.ENUM) {
                val enumValues = propDescriptor.enumValues ?: emptyList()
                if (value !in enumValues) {
                    errors.add(
                        ValidationError(
                            "Invalid enum value '$value' for property '$name'. Valid values: ${enumValues.joinToString(", ")}",
                            path = component.uuid
                        )
                    )
                }
            }
        }

        // Validate children support
        if (component.children.isNotEmpty() && !descriptor.supportsChildren) {
            errors.add(
                ValidationError(
                    "${component.type} does not support children",
                    path = component.uuid
                )
            )
        }

        // Recursively validate children
        component.children.forEach { child ->
            val childResult = validate(child)
            errors.addAll(childResult.errors)
            warnings.addAll(childResult.warnings)
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }

    private fun validatePropertyType(value: String, type: PropertyType): Boolean {
        return when (type) {
            PropertyType.STRING -> true
            PropertyType.INT -> value.toIntOrNull() != null
            PropertyType.FLOAT -> value.toFloatOrNull() != null
            PropertyType.BOOLEAN -> value.toBooleanStrictOrNull() != null
            PropertyType.COLOR -> isValidColorHex(value)
            PropertyType.ENUM -> true // Validated separately
        }
    }

    private fun isValidColorHex(value: String): Boolean {
        val hex = value.removePrefix("#")
        return hex.length in listOf(6, 8) && hex.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
    }
}
```

This utilities document provides the foundational code for implementing the code generation system. Would you like me to continue with additional sections on code formatting, file structure generation, or build system integration?

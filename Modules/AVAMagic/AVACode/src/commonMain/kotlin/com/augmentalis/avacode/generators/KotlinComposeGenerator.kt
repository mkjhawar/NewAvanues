package com.augmentalis.avacode.generators

import com.augmentalis.avacode.dsl.*

/**
 * KotlinComposeGenerator - Generates Jetpack Compose code from AvaUI AST
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class KotlinComposeGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = StringBuilder()

        // Package declaration
        code.appendLine("package com.augmentalis.voiceos.ui.screens")
        code.appendLine()

        // Imports
        generateImports(screen, code)
        code.appendLine()

        // Composable function
        code.appendLine("@Composable")
        code.appendLine("fun ${screen.name}Screen() {")

        // State variables
        screen.stateVariables.forEach { stateVar ->
            generateStateVariable(stateVar, code)
        }
        if (screen.stateVariables.isNotEmpty()) {
            code.appendLine()
        }

        // Root component
        generateComponent(screen.root, code, indent = 1)

        code.appendLine("}")

        return GeneratedCode(
            code = code.toString(),
            language = Language.KOTLIN,
            platform = Platform.ANDROID
        )
    }

    override fun generateComponent(component: ComponentNode): String {
        val code = StringBuilder()
        generateComponent(component, code, indent = 0)
        return code.toString()
    }

    private fun generateImports(screen: ScreenNode, code: StringBuilder) {
        code.appendLine("import androidx.compose.runtime.*")
        code.appendLine("import androidx.compose.material3.*")
        code.appendLine("import androidx.compose.foundation.layout.*")
        code.appendLine("import androidx.compose.ui.Modifier")
        code.appendLine("import androidx.compose.ui.unit.dp")

        screen.imports.forEach { import ->
            code.appendLine("import $import")
        }
    }

    private fun generateStateVariable(stateVar: StateVariable, code: StringBuilder) {
        val modifier = if (stateVar.mutable) "var" else "val"
        val initialValue = stateVar.initialValue?.let { formatPropertyValue(it) } ?: "null"

        code.append("    $modifier ${stateVar.name} by ")
        if (stateVar.mutable) {
            code.appendLine("remember { mutableStateOf($initialValue) }")
        } else {
            code.appendLine("remember { $initialValue }")
        }
    }

    private fun generateComponent(component: ComponentNode, code: StringBuilder, indent: Int) {
        val indentStr = "    ".repeat(indent)

        when (component.type) {
            ComponentType.BUTTON -> generateButton(component, code, indentStr)
            ComponentType.TEXT -> generateText(component, code, indentStr)
            ComponentType.TEXT_FIELD -> generateTextField(component, code, indentStr)
            ComponentType.CARD -> generateCard(component, code, indentStr)
            ComponentType.CHECKBOX -> generateCheckbox(component, code, indentStr)
            ComponentType.COLUMN -> generateColumn(component, code, indentStr, indent)
            ComponentType.ROW -> generateRow(component, code, indentStr, indent)
            ComponentType.CONTAINER -> generateContainer(component, code, indentStr, indent)
            else -> generateGenericComponent(component, code, indentStr, indent)
        }
    }

    private fun generateButton(component: ComponentNode, code: StringBuilder, indent: String) {
        val text = component.properties["text"] ?: "Button"
        val onClick = component.eventHandlers["onClick"] ?: "{}"

        code.append("${indent}Button(")
        code.appendLine("onClick = $onClick) {")
        code.appendLine("$indent    Text(\"$text\")")
        code.appendLine("$indent}")
    }

    private fun generateText(component: ComponentNode, code: StringBuilder, indent: String) {
        val content = component.properties["content"] ?: ""
        val variant = component.properties["variant"] ?: "BODY1"

        val style = when (variant) {
            "H1" -> "MaterialTheme.typography.headlineLarge"
            "H2" -> "MaterialTheme.typography.headlineMedium"
            "H3" -> "MaterialTheme.typography.headlineSmall"
            "BODY1" -> "MaterialTheme.typography.bodyLarge"
            "BODY2" -> "MaterialTheme.typography.bodyMedium"
            "CAPTION" -> "MaterialTheme.typography.bodySmall"
            else -> "MaterialTheme.typography.bodyLarge"
        }

        code.appendLine("${indent}Text(")
        code.appendLine("$indent    text = \"$content\",")
        code.appendLine("$indent    style = $style")
        code.appendLine("$indent)")
    }

    private fun generateTextField(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: ""
        val label = component.properties["label"] ?: ""
        val placeholder = component.properties["placeholder"] ?: ""
        val onValueChange = component.eventHandlers["onValueChange"] ?: "{}"

        code.appendLine("${indent}OutlinedTextField(")
        code.appendLine("$indent    value = $value,")
        code.appendLine("$indent    onValueChange = $onValueChange,")
        if (label.toString().isNotEmpty()) {
            code.appendLine("$indent    label = { Text(\"$label\") },")
        }
        if (placeholder.toString().isNotEmpty()) {
            code.appendLine("$indent    placeholder = { Text(\"$placeholder\") },")
        }
        code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
        code.appendLine("$indent)")
    }

    private fun generateCard(component: ComponentNode, code: StringBuilder, indent: String) {
        code.appendLine("${indent}Card(")
        code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
        code.appendLine("$indent) {")

        component.children.forEach { child ->
            generateComponent(child, code, indent.length / 4 + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateCheckbox(component: ComponentNode, code: StringBuilder, indent: String) {
        val checked = component.properties["checked"] ?: "false"
        val label = component.properties["label"]
        val onCheckedChange = component.eventHandlers["onCheckedChange"] ?: "{}"

        if (label != null) {
            code.appendLine("${indent}Row(")
            code.appendLine("$indent    verticalAlignment = Alignment.CenterVertically")
            code.appendLine("$indent) {")
            code.appendLine("$indent    Checkbox(")
            code.appendLine("$indent        checked = $checked,")
            code.appendLine("$indent        onCheckedChange = $onCheckedChange")
            code.appendLine("$indent    )")
            code.appendLine("$indent    Text(\"$label\")")
            code.appendLine("$indent}")
        } else {
            code.appendLine("${indent}Checkbox(")
            code.appendLine("$indent    checked = $checked,")
            code.appendLine("$indent    onCheckedChange = $onCheckedChange")
            code.appendLine("$indent)")
        }
    }

    private fun generateColumn(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}Column(")
        code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
        code.appendLine("$indent) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateRow(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}Row(")
        code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
        code.appendLine("$indent) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateContainer(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}Box(")
        code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
        code.appendLine("$indent) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateGenericComponent(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("$indent// TODO: Implement ${component.type}")
    }

    private fun formatPropertyValue(value: PropertyValue): String {
        return when (value) {
            is PropertyValue.StringValue -> "\"${value.value}\""
            is PropertyValue.IntValue -> value.value.toString()
            is PropertyValue.DoubleValue -> value.value.toString()
            is PropertyValue.BoolValue -> value.value.toString()
            is PropertyValue.EnumValue -> "${value.type}.${value.value}"
            is PropertyValue.ListValue -> value.items.joinToString(", ", "listOf(", ")") { formatPropertyValue(it) }
            is PropertyValue.MapValue -> value.items.entries.joinToString(", ", "mapOf(", ")") {
                "\"${it.key}\" to ${formatPropertyValue(it.value)}"
            }
            is PropertyValue.ReferenceValue -> value.ref
        }
    }
}

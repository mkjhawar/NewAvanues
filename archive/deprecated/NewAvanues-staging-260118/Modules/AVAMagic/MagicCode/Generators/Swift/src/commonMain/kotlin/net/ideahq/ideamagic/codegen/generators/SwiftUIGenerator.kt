package com.augmentalis.magiccode.generator.generators

import com.augmentalis.magiccode.generator.ast.*

/**
 * SwiftUIGenerator - Generates SwiftUI code from AvaUI AST
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class SwiftUIGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = StringBuilder()

        // Imports
        code.appendLine("import SwiftUI")
        code.appendLine()

        // Struct declaration
        code.appendLine("struct ${screen.name}View: View {")

        // State variables
        screen.stateVariables.forEach { stateVar ->
            generateStateVariable(stateVar, code)
        }
        if (screen.stateVariables.isNotEmpty()) {
            code.appendLine()
        }

        // Body
        code.appendLine("    var body: some View {")
        generateComponent(screen.root, code, indent = 2)
        code.appendLine("    }")

        code.appendLine("}")
        code.appendLine()

        // Preview
        code.appendLine("#if DEBUG")
        code.appendLine("struct ${screen.name}View_Previews: PreviewProvider {")
        code.appendLine("    static var previews: some View {")
        code.appendLine("        ${screen.name}View()")
        code.appendLine("    }")
        code.appendLine("}")
        code.appendLine("#endif")

        return GeneratedCode(
            code = code.toString(),
            language = Language.SWIFT,
            platform = Platform.IOS
        )
    }

    override fun generateComponent(component: ComponentNode): String {
        val code = StringBuilder()
        generateComponent(component, code, indent = 0)
        return code.toString()
    }

    private fun generateStateVariable(stateVar: StateVariable, code: StringBuilder) {
        val annotation = if (stateVar.mutable) "@State" else "let"
        val varKeyword = if (stateVar.mutable) "var" else "let"
        val initialValue = stateVar.initialValue?.let { formatPropertyValue(it) } ?: "nil"
        val type = mapKotlinTypeToSwift(stateVar.type)

        code.appendLine("    $annotation private $varKeyword ${stateVar.name}: $type = $initialValue")
    }

    private fun generateComponent(component: ComponentNode, code: StringBuilder, indent: Int) {
        val indentStr = "    ".repeat(indent)

        when (component.type) {
            // Foundation components
            ComponentType.BUTTON -> generateButton(component, code, indentStr)
            ComponentType.TEXT -> generateText(component, code, indentStr)
            ComponentType.TEXT_FIELD -> generateTextField(component, code, indentStr)
            ComponentType.CARD -> generateCard(component, code, indentStr, indent)
            ComponentType.CHECKBOX -> generateCheckbox(component, code, indentStr)
            ComponentType.IMAGE -> generateImage(component, code, indentStr)
            ComponentType.ICON -> generateIcon(component, code, indentStr)
            ComponentType.DIVIDER -> generateDivider(component, code, indentStr)
            ComponentType.CHIP -> generateChip(component, code, indentStr)
            ComponentType.LIST_ITEM -> generateListItem(component, code, indentStr, indent)

            // Layout components
            ComponentType.COLUMN -> generateVStack(component, code, indentStr, indent)
            ComponentType.ROW -> generateHStack(component, code, indentStr, indent)
            ComponentType.CONTAINER -> generateZStack(component, code, indentStr, indent)
            ComponentType.STACK -> generateZStack(component, code, indentStr, indent)
            ComponentType.SPACER -> generateSpacer(component, code, indentStr)
            ComponentType.SCROLL_VIEW -> generateScrollView(component, code, indentStr, indent)
            ComponentType.GRID -> generateGrid(component, code, indentStr, indent)

            // Advanced components
            ComponentType.SWITCH -> generateSwitch(component, code, indentStr)
            ComponentType.SLIDER -> generateSlider(component, code, indentStr)
            ComponentType.PROGRESS_BAR -> generateProgressBar(component, code, indentStr)
            ComponentType.SPINNER -> generateSpinner(component, code, indentStr)
            ComponentType.ALERT -> generateAlert(component, code, indentStr)
            ComponentType.DIALOG -> generateDialog(component, code, indentStr, indent)
            ComponentType.DROPDOWN -> generateDropdown(component, code, indentStr)
            ComponentType.DATE_PICKER -> generateDatePicker(component, code, indentStr)
            ComponentType.TIME_PICKER -> generateTimePicker(component, code, indentStr)
            ComponentType.SEARCH_BAR -> generateSearchBar(component, code, indentStr)

            // Navigation components
            ComponentType.APP_BAR -> generateAppBar(component, code, indentStr, indent)
            ComponentType.BOTTOM_NAV -> generateBottomNav(component, code, indentStr, indent)
            ComponentType.TABS -> generateTabs(component, code, indentStr, indent)

            else -> generateGenericComponent(component, code, indentStr, indent)
        }
    }

    private fun generateButton(component: ComponentNode, code: StringBuilder, indent: String) {
        val text = component.properties["text"] ?: "Button"
        val onClick = component.eventHandlers["onClick"] ?: "{}"

        code.appendLine("${indent}Button(\"$text\") $onClick")
    }

    private fun generateText(component: ComponentNode, code: StringBuilder, indent: String) {
        val content = component.properties["content"] ?: ""
        val variant = component.properties["variant"] ?: "BODY1"

        code.append("${indent}Text(\"$content\")")

        when (variant) {
            "H1" -> code.appendLine(".font(.largeTitle)")
            "H2" -> code.appendLine(".font(.title)")
            "H3" -> code.appendLine(".font(.title2)")
            "BODY1" -> code.appendLine(".font(.body)")
            "BODY2" -> code.appendLine(".font(.callout)")
            "CAPTION" -> code.appendLine(".font(.caption)")
            else -> code.appendLine()
        }
    }

    private fun generateTextField(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "text"
        val label = component.properties["label"] ?: ""
        val placeholder = component.properties["placeholder"] ?: ""

        val promptText = if (placeholder.toString().isNotEmpty()) placeholder else label
        code.appendLine("${indent}TextField(\"$promptText\", text: \$$value)")
    }

    private fun generateCard(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}VStack(alignment: .leading, spacing: 8) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
        code.appendLine("$indent.padding()")
        code.appendLine("$indent.background(Color(.systemBackground))")
        code.appendLine("$indent.cornerRadius(12)")
        code.appendLine("$indent.shadow(radius: 4)")
    }

    private fun generateCheckbox(component: ComponentNode, code: StringBuilder, indent: String) {
        val checked = component.properties["checked"] ?: "isChecked"
        val label = component.properties["label"]

        code.appendLine("${indent}Toggle(\"${label ?: ""}\", isOn: \$$checked)")
    }

    private fun generateVStack(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val spacing = component.properties["spacing"] ?: "8"

        code.appendLine("${indent}VStack(spacing: $spacing) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateHStack(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val spacing = component.properties["spacing"] ?: "8"

        code.appendLine("${indent}HStack(spacing: $spacing) {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateZStack(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}ZStack {")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent}")
    }

    private fun generateImage(component: ComponentNode, code: StringBuilder, indent: String) {
        val src = component.properties["src"] ?: ""
        val contentMode = component.properties["contentMode"] ?: "fit"

        if (src.toString().startsWith("http")) {
            code.appendLine("${indent}AsyncImage(url: URL(string: \"$src\")) { image in")
            code.appendLine("$indent    image.resizable().aspectRatio(contentMode: .${contentMode})")
            code.appendLine("$indent} placeholder: {")
            code.appendLine("$indent    ProgressView()")
            code.appendLine("$indent}")
        } else {
            code.appendLine("${indent}Image(\"$src\")")
            code.appendLine("$indent    .resizable()")
            code.appendLine("$indent    .aspectRatio(contentMode: .${contentMode})")
        }
    }

    private fun generateIcon(component: ComponentNode, code: StringBuilder, indent: String) {
        val name = component.properties["name"] ?: "star"
        val size = component.properties["size"] ?: "24"
        val color = component.properties["color"]

        code.append("${indent}Image(systemName: \"$name\")")
        code.append(".font(.system(size: $size))")
        if (color != null) {
            code.append(".foregroundColor(Color(\"$color\"))")
        }
        code.appendLine()
    }

    private fun generateDivider(component: ComponentNode, code: StringBuilder, indent: String) {
        code.appendLine("${indent}Divider()")
    }

    private fun generateChip(component: ComponentNode, code: StringBuilder, indent: String) {
        val label = component.properties["label"] ?: ""
        val selected = component.properties["selected"] ?: "false"

        code.appendLine("${indent}Text(\"$label\")")
        code.appendLine("$indent    .padding(.horizontal, 12)")
        code.appendLine("$indent    .padding(.vertical, 6)")
        code.appendLine("$indent    .background($selected ? Color.accentColor : Color.secondary.opacity(0.2))")
        code.appendLine("$indent    .foregroundColor($selected ? .white : .primary)")
        code.appendLine("$indent    .cornerRadius(16)")
    }

    private fun generateListItem(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val title = component.properties["title"] ?: ""
        val subtitle = component.properties["subtitle"]
        val onClick = component.eventHandlers["onClick"]

        if (onClick != null) {
            code.appendLine("${indent}Button(action: $onClick) {")
        }

        code.appendLine("${indent}${if (onClick != null) "    " else ""}HStack {")
        code.appendLine("${indent}${if (onClick != null) "    " else ""}    VStack(alignment: .leading) {")
        code.appendLine("${indent}${if (onClick != null) "    " else ""}        Text(\"$title\").font(.headline)")
        if (subtitle != null) {
            code.appendLine("${indent}${if (onClick != null) "    " else ""}        Text(\"$subtitle\").font(.subheadline).foregroundColor(.secondary)")
        }
        code.appendLine("${indent}${if (onClick != null) "    " else ""}    }")
        code.appendLine("${indent}${if (onClick != null) "    " else ""}    Spacer()")
        code.appendLine("${indent}${if (onClick != null) "    " else ""}    Image(systemName: \"chevron.right\").foregroundColor(.secondary)")
        code.appendLine("${indent}${if (onClick != null) "    " else ""}}")

        if (onClick != null) {
            code.appendLine("${indent}}")
        }
    }

    private fun generateSpacer(component: ComponentNode, code: StringBuilder, indent: String) {
        val size = component.properties["size"]
        if (size != null) {
            code.appendLine("${indent}Spacer().frame(height: $size)")
        } else {
            code.appendLine("${indent}Spacer()")
        }
    }

    private fun generateScrollView(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val axis = component.properties["axis"] ?: "vertical"
        val scrollAxis = if (axis == "horizontal") ".horizontal" else ".vertical"

        code.appendLine("${indent}ScrollView($scrollAxis) {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent}")
    }

    private fun generateGrid(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val columns = component.properties["columns"] ?: "2"

        code.appendLine("${indent}LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: $columns)) {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent}")
    }

    private fun generateSwitch(component: ComponentNode, code: StringBuilder, indent: String) {
        val checked = component.properties["checked"] ?: "isOn"
        val label = component.properties["label"] ?: ""

        code.appendLine("${indent}Toggle(\"$label\", isOn: \$$checked)")
    }

    private fun generateSlider(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "value"
        val min = component.properties["min"] ?: "0"
        val max = component.properties["max"] ?: "100"

        code.appendLine("${indent}Slider(value: \$$value, in: ${min}...${max})")
    }

    private fun generateProgressBar(component: ComponentNode, code: StringBuilder, indent: String) {
        val progress = component.properties["progress"] ?: "0.5"

        code.appendLine("${indent}ProgressView(value: $progress)")
    }

    private fun generateSpinner(component: ComponentNode, code: StringBuilder, indent: String) {
        code.appendLine("${indent}ProgressView()")
    }

    private fun generateAlert(component: ComponentNode, code: StringBuilder, indent: String) {
        val message = component.properties["message"] ?: ""
        val severity = component.properties["severity"] ?: "info"

        val color = when (severity) {
            "error" -> "Color.red"
            "warning" -> "Color.orange"
            "success" -> "Color.green"
            else -> "Color.blue"
        }

        code.appendLine("${indent}HStack {")
        code.appendLine("$indent    Image(systemName: \"info.circle.fill\").foregroundColor($color)")
        code.appendLine("$indent    Text(\"$message\")")
        code.appendLine("$indent}")
        code.appendLine("$indent.padding()")
        code.appendLine("$indent.background($color.opacity(0.1))")
        code.appendLine("$indent.cornerRadius(8)")
    }

    private fun generateDialog(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val title = component.properties["title"] ?: ""
        val isPresented = component.properties["isPresented"] ?: "showDialog"

        code.appendLine("${indent}.alert(\"$title\", isPresented: \$$isPresented) {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent}")
    }

    private fun generateDropdown(component: ComponentNode, code: StringBuilder, indent: String) {
        val selection = component.properties["selection"] ?: "selected"
        val label = component.properties["label"] ?: "Select"

        code.appendLine("${indent}Picker(\"$label\", selection: \$$selection) {")
        code.appendLine("$indent    // Options here")
        code.appendLine("$indent}")
    }

    private fun generateDatePicker(component: ComponentNode, code: StringBuilder, indent: String) {
        val selection = component.properties["selection"] ?: "date"
        val label = component.properties["label"] ?: "Date"

        code.appendLine("${indent}DatePicker(\"$label\", selection: \$$selection, displayedComponents: .date)")
    }

    private fun generateTimePicker(component: ComponentNode, code: StringBuilder, indent: String) {
        val selection = component.properties["selection"] ?: "time"
        val label = component.properties["label"] ?: "Time"

        code.appendLine("${indent}DatePicker(\"$label\", selection: \$$selection, displayedComponents: .hourAndMinute)")
    }

    private fun generateSearchBar(component: ComponentNode, code: StringBuilder, indent: String) {
        val text = component.properties["text"] ?: "searchText"
        val placeholder = component.properties["placeholder"] ?: "Search"

        code.appendLine("${indent}TextField(\"$placeholder\", text: \$$text)")
        code.appendLine("$indent    .textFieldStyle(RoundedBorderTextFieldStyle())")
    }

    private fun generateAppBar(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val title = component.properties["title"] ?: ""

        code.appendLine("${indent}NavigationView {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent    .navigationTitle(\"$title\")")
        code.appendLine("$indent}")
    }

    private fun generateBottomNav(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}TabView {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent}")
    }

    private fun generateTabs(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val selection = component.properties["selection"] ?: "selectedTab"

        code.appendLine("${indent}TabView(selection: \$$selection) {")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent}")
    }

    private fun generateGenericComponent(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        // Try to render children in a VStack as fallback
        if (component.children.isNotEmpty()) {
            code.appendLine("${indent}VStack { // ${component.type}")
            component.children.forEach { child ->
                generateComponent(child as ComponentNode, code, indentLevel + 1)
            }
            code.appendLine("$indent}")
        } else {
            code.appendLine("$indent// TODO: Implement ${component.type}")
        }
    }

    private fun formatPropertyValue(value: PropertyValue): String {
        return when (value) {
            is PropertyValue.StringValue -> "\"${value.value}\""
            is PropertyValue.IntValue -> value.value.toString()
            is PropertyValue.DoubleValue -> value.value.toString()
            is PropertyValue.BoolValue -> value.value.toString()
            is PropertyValue.EnumValue -> ".${value.value.lowercase()}"
            is PropertyValue.ListValue -> "[${value.items.joinToString(", ") { formatPropertyValue(it) }}]"
            is PropertyValue.MapValue -> "[${value.items.entries.joinToString(", ") {
                "\"${it.key}\": ${formatPropertyValue(it.value)}"
            }}]"
            is PropertyValue.ReferenceValue -> value.ref
        }
    }

    private fun mapKotlinTypeToSwift(kotlinType: String): String {
        return when (kotlinType) {
            "String" -> "String"
            "Int" -> "Int"
            "Double" -> "Double"
            "Float" -> "CGFloat"
            "Boolean" -> "Bool"
            "List" -> "Array"
            "Map" -> "Dictionary"
            else -> kotlinType
        }
    }
}

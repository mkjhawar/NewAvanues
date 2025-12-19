package net.ideahq.avamagic.codegen.generators

import net.ideahq.avamagic.codegen.ast.*

/**
 * ReactTypeScriptGenerator - Generates React/TypeScript code from AvaUI AST
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class ReactTypeScriptGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = StringBuilder()

        // Imports
        generateImports(screen, code)
        code.appendLine()

        // Interface for props (if needed)
        code.appendLine("interface ${screen.name}Props {}")
        code.appendLine()

        // Component function
        code.appendLine("export const ${screen.name}: React.FC<${screen.name}Props> = () => {")

        // State variables
        screen.stateVariables.forEach { stateVar ->
            generateStateVariable(stateVar, code)
        }
        if (screen.stateVariables.isNotEmpty()) {
            code.appendLine()
        }

        // Return JSX
        code.appendLine("  return (")
        generateComponent(screen.root, code, indent = 2)
        code.appendLine("  );")

        code.appendLine("};")
        code.appendLine()

        // Export default
        code.appendLine("export default ${screen.name};")

        return GeneratedCode(
            code = code.toString(),
            language = Language.TYPESCRIPT,
            platform = Platform.WEB
        )
    }

    override fun generateComponent(component: ComponentNode): String {
        val code = StringBuilder()
        generateComponent(component, code, indent = 0)
        return code.toString()
    }

    private fun generateImports(screen: ScreenNode, code: StringBuilder) {
        code.appendLine("import React, { useState } from 'react';")
        code.appendLine("import {")
        code.appendLine("  Button, TextField, Typography, Card, Checkbox,")
        code.appendLine("  FormControlLabel, Box, Stack, Avatar, Badge, Chip,")
        code.appendLine("  Divider, IconButton, LinearProgress, CircularProgress,")
        code.appendLine("  Switch, Slider, Alert, Dialog, DialogTitle, DialogContent,")
        code.appendLine("  DialogActions, Select, MenuItem, InputLabel, FormControl,")
        code.appendLine("  Tabs, Tab, AppBar, Toolbar, BottomNavigation, BottomNavigationAction,")
        code.appendLine("  List, ListItem, ListItemText, ListItemIcon, Grid, Paper")
        code.appendLine("} from '@mui/material';")
        code.appendLine("import { DatePicker, TimePicker } from '@mui/x-date-pickers';")

        screen.imports.forEach { import ->
            code.appendLine("import $import;")
        }
    }

    private fun generateStateVariable(stateVar: StateVariable, code: StringBuilder) {
        val initialValue = stateVar.initialValue?.let { formatPropertyValue(it) } ?: "null"
        val type = mapKotlinTypeToTypeScript(stateVar.type)

        code.append("  const [${stateVar.name}, set")
        code.append(stateVar.name.replaceFirstChar { it.uppercase() })
        code.appendLine("] = useState<$type>($initialValue);")
    }

    private fun generateComponent(component: ComponentNode, code: StringBuilder, indent: Int) {
        val indentStr = "  ".repeat(indent)

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
            ComponentType.LIST_ITEM -> generateListItem(component, code, indentStr)

            // Layout components
            ComponentType.COLUMN -> generateStack(component, code, indentStr, indent, "column")
            ComponentType.ROW -> generateStack(component, code, indentStr, indent, "row")
            ComponentType.CONTAINER -> generateBox(component, code, indentStr, indent)
            ComponentType.STACK -> generateBox(component, code, indentStr, indent)
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
            ComponentType.BADGE -> generateBadge(component, code, indentStr, indent)

            // Navigation components
            ComponentType.APP_BAR -> generateAppBar(component, code, indentStr, indent)
            ComponentType.BOTTOM_NAV -> generateBottomNav(component, code, indentStr, indent)
            ComponentType.TABS -> generateTabs(component, code, indentStr, indent)

            else -> generateGenericComponent(component, code, indentStr, indent)
        }
    }

    private fun generateButton(component: ComponentNode, code: StringBuilder, indent: String) {
        val text = component.properties["text"] ?: "Button"
        val variant = component.properties["variant"] ?: "PRIMARY"
        val onClick = component.eventHandlers["onClick"] ?: "() => {}"

        val muiVariant = when (variant) {
            "PRIMARY" -> "contained"
            "SECONDARY" -> "outlined"
            "OUTLINED" -> "outlined"
            "TEXT" -> "text"
            else -> "contained"
        }

        code.appendLine("${indent}<Button")
        code.appendLine("$indent  variant=\"$muiVariant\"")
        code.appendLine("$indent  onClick={$onClick}")
        code.appendLine("$indent>")
        code.appendLine("$indent  $text")
        code.appendLine("$indent</Button>")
    }

    private fun generateText(component: ComponentNode, code: StringBuilder, indent: String) {
        val content = component.properties["content"] ?: ""
        val variant = component.properties["variant"] ?: "BODY1"

        val muiVariant = when (variant) {
            "H1" -> "h1"
            "H2" -> "h2"
            "H3" -> "h3"
            "BODY1" -> "body1"
            "BODY2" -> "body2"
            "CAPTION" -> "caption"
            else -> "body1"
        }

        code.appendLine("${indent}<Typography variant=\"$muiVariant\">")
        code.appendLine("$indent  $content")
        code.appendLine("$indent</Typography>")
    }

    private fun generateTextField(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: ""
        val label = component.properties["label"] ?: ""
        val placeholder = component.properties["placeholder"] ?: ""
        val onChange = component.eventHandlers["onValueChange"] ?: "(e) => {}"

        code.appendLine("${indent}<TextField")
        code.appendLine("$indent  value={$value}")
        code.appendLine("$indent  onChange={$onChange}")
        if (label.toString().isNotEmpty()) {
            code.appendLine("$indent  label=\"$label\"")
        }
        if (placeholder.toString().isNotEmpty()) {
            code.appendLine("$indent  placeholder=\"$placeholder\"")
        }
        code.appendLine("$indent  fullWidth")
        code.appendLine("$indent/>")
    }

    private fun generateCard(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}<Card>")
        code.appendLine("$indent  <Box p={2}>")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 2)
        }

        code.appendLine("$indent  </Box>")
        code.appendLine("$indent</Card>")
    }

    private fun generateCheckbox(component: ComponentNode, code: StringBuilder, indent: String) {
        val checked = component.properties["checked"] ?: "false"
        val label = component.properties["label"]
        val onChange = component.eventHandlers["onCheckedChange"] ?: "(e) => {}"

        if (label != null) {
            code.appendLine("${indent}<FormControlLabel")
            code.appendLine("$indent  control={")
            code.appendLine("$indent    <Checkbox")
            code.appendLine("$indent      checked={$checked}")
            code.appendLine("$indent      onChange={$onChange}")
            code.appendLine("$indent    />")
            code.appendLine("$indent  }")
            code.appendLine("$indent  label=\"$label\"")
            code.appendLine("$indent/>")
        } else {
            code.appendLine("${indent}<Checkbox")
            code.appendLine("$indent  checked={$checked}")
            code.appendLine("$indent  onChange={$onChange}")
            code.appendLine("$indent/>")
        }
    }

    private fun generateStack(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int, direction: String) {
        val spacing = component.properties["spacing"] ?: "2"

        code.appendLine("${indent}<Stack direction=\"$direction\" spacing={$spacing}>")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent</Stack>")
    }

    private fun generateBox(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        code.appendLine("${indent}<Box>")

        component.children.forEach { child ->
            generateComponent(child, code, indentLevel + 1)
        }

        code.appendLine("$indent</Box>")
    }

    private fun generateImage(component: ComponentNode, code: StringBuilder, indent: String) {
        val src = component.properties["src"] ?: ""
        val alt = component.properties["alt"] ?: ""
        val width = component.properties["width"]
        val height = component.properties["height"]

        code.append("${indent}<img src=\"$src\" alt=\"$alt\"")
        if (width != null) code.append(" width={$width}")
        if (height != null) code.append(" height={$height}")
        code.appendLine(" />")
    }

    private fun generateIcon(component: ComponentNode, code: StringBuilder, indent: String) {
        val name = component.properties["name"] ?: "Star"
        val size = component.properties["size"] ?: "24"
        val color = component.properties["color"]

        code.append("${indent}<${name}Icon")
        if (color != null) code.append(" sx={{ color: '$color' }}")
        code.appendLine(" fontSize=\"${if (size.toString().toInt() > 24) "large" else "medium"}\" />")
    }

    private fun generateDivider(component: ComponentNode, code: StringBuilder, indent: String) {
        code.appendLine("${indent}<Divider />")
    }

    private fun generateChip(component: ComponentNode, code: StringBuilder, indent: String) {
        val label = component.properties["label"] ?: ""
        val onClick = component.eventHandlers["onClick"]
        val variant = component.properties["variant"] ?: "filled"

        code.append("${indent}<Chip label=\"$label\" variant=\"$variant\"")
        if (onClick != null) code.append(" onClick={$onClick}")
        code.appendLine(" />")
    }

    private fun generateListItem(component: ComponentNode, code: StringBuilder, indent: String) {
        val title = component.properties["title"] ?: ""
        val subtitle = component.properties["subtitle"]
        val onClick = component.eventHandlers["onClick"]

        code.append("${indent}<ListItem")
        if (onClick != null) code.append(" button onClick={$onClick}")
        code.appendLine(">")
        code.append("$indent  <ListItemText primary=\"$title\"")
        if (subtitle != null) code.append(" secondary=\"$subtitle\"")
        code.appendLine(" />")
        code.appendLine("$indent</ListItem>")
    }

    private fun generateSpacer(component: ComponentNode, code: StringBuilder, indent: String) {
        val size = component.properties["size"] ?: "16"
        code.appendLine("${indent}<Box sx={{ height: $size }} />")
    }

    private fun generateScrollView(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val maxHeight = component.properties["maxHeight"] ?: "400px"

        code.appendLine("${indent}<Box sx={{ maxHeight: '$maxHeight', overflow: 'auto' }}>")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent</Box>")
    }

    private fun generateGrid(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val columns = component.properties["columns"] ?: "12"
        val spacing = component.properties["spacing"] ?: "2"

        code.appendLine("${indent}<Grid container spacing={$spacing}>")
        component.children.forEach { child ->
            code.appendLine("$indent  <Grid item xs={${12 / columns.toString().toInt()}}>")
            generateComponent(child as ComponentNode, code, indentLevel + 2)
            code.appendLine("$indent  </Grid>")
        }
        code.appendLine("$indent</Grid>")
    }

    private fun generateSwitch(component: ComponentNode, code: StringBuilder, indent: String) {
        val checked = component.properties["checked"] ?: "false"
        val label = component.properties["label"]
        val onChange = component.eventHandlers["onChange"] ?: "(e) => {}"

        if (label != null) {
            code.appendLine("${indent}<FormControlLabel")
            code.appendLine("$indent  control={<Switch checked={$checked} onChange={$onChange} />}")
            code.appendLine("$indent  label=\"$label\"")
            code.appendLine("$indent/>")
        } else {
            code.appendLine("${indent}<Switch checked={$checked} onChange={$onChange} />")
        }
    }

    private fun generateSlider(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "50"
        val min = component.properties["min"] ?: "0"
        val max = component.properties["max"] ?: "100"
        val onChange = component.eventHandlers["onChange"] ?: "(e, v) => {}"

        code.appendLine("${indent}<Slider")
        code.appendLine("$indent  value={$value}")
        code.appendLine("$indent  min={$min}")
        code.appendLine("$indent  max={$max}")
        code.appendLine("$indent  onChange={$onChange}")
        code.appendLine("$indent/>")
    }

    private fun generateProgressBar(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: component.properties["progress"]
        val variant = if (value != null) "determinate" else "indeterminate"

        code.append("${indent}<LinearProgress variant=\"$variant\"")
        if (value != null) code.append(" value={${value.toString().toDouble() * 100}}")
        code.appendLine(" />")
    }

    private fun generateSpinner(component: ComponentNode, code: StringBuilder, indent: String) {
        code.appendLine("${indent}<CircularProgress />")
    }

    private fun generateAlert(component: ComponentNode, code: StringBuilder, indent: String) {
        val message = component.properties["message"] ?: ""
        val severity = component.properties["severity"] ?: "info"

        code.appendLine("${indent}<Alert severity=\"$severity\">")
        code.appendLine("$indent  $message")
        code.appendLine("$indent</Alert>")
    }

    private fun generateDialog(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val open = component.properties["open"] ?: "open"
        val title = component.properties["title"] ?: ""
        val onClose = component.eventHandlers["onClose"] ?: "() => {}"

        code.appendLine("${indent}<Dialog open={$open} onClose={$onClose}>")
        if (title.toString().isNotEmpty()) {
            code.appendLine("$indent  <DialogTitle>$title</DialogTitle>")
        }
        code.appendLine("$indent  <DialogContent>")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 2)
        }
        code.appendLine("$indent  </DialogContent>")
        code.appendLine("$indent</Dialog>")
    }

    private fun generateDropdown(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "value"
        val label = component.properties["label"] ?: "Select"
        val onChange = component.eventHandlers["onChange"] ?: "(e) => {}"

        code.appendLine("${indent}<FormControl fullWidth>")
        code.appendLine("$indent  <InputLabel>$label</InputLabel>")
        code.appendLine("$indent  <Select value={$value} label=\"$label\" onChange={$onChange}>")
        code.appendLine("$indent    {/* MenuItem options */}")
        code.appendLine("$indent  </Select>")
        code.appendLine("$indent</FormControl>")
    }

    private fun generateDatePicker(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "date"
        val label = component.properties["label"] ?: "Date"
        val onChange = component.eventHandlers["onChange"] ?: "(v) => {}"

        code.appendLine("${indent}<DatePicker")
        code.appendLine("$indent  label=\"$label\"")
        code.appendLine("$indent  value={$value}")
        code.appendLine("$indent  onChange={$onChange}")
        code.appendLine("$indent/>")
    }

    private fun generateTimePicker(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "time"
        val label = component.properties["label"] ?: "Time"
        val onChange = component.eventHandlers["onChange"] ?: "(v) => {}"

        code.appendLine("${indent}<TimePicker")
        code.appendLine("$indent  label=\"$label\"")
        code.appendLine("$indent  value={$value}")
        code.appendLine("$indent  onChange={$onChange}")
        code.appendLine("$indent/>")
    }

    private fun generateSearchBar(component: ComponentNode, code: StringBuilder, indent: String) {
        val value = component.properties["value"] ?: "searchText"
        val placeholder = component.properties["placeholder"] ?: "Search..."
        val onChange = component.eventHandlers["onChange"] ?: "(e) => {}"

        code.appendLine("${indent}<TextField")
        code.appendLine("$indent  value={$value}")
        code.appendLine("$indent  onChange={$onChange}")
        code.appendLine("$indent  placeholder=\"$placeholder\"")
        code.appendLine("$indent  variant=\"outlined\"")
        code.appendLine("$indent  size=\"small\"")
        code.appendLine("$indent  fullWidth")
        code.appendLine("$indent/>")
    }

    private fun generateBadge(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val content = component.properties["content"] ?: "0"
        val color = component.properties["color"] ?: "primary"

        code.appendLine("${indent}<Badge badgeContent={$content} color=\"$color\">")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent</Badge>")
    }

    private fun generateAppBar(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val title = component.properties["title"] ?: ""

        code.appendLine("${indent}<AppBar position=\"static\">")
        code.appendLine("$indent  <Toolbar>")
        code.appendLine("$indent    <Typography variant=\"h6\" component=\"div\" sx={{ flexGrow: 1 }}>")
        code.appendLine("$indent      $title")
        code.appendLine("$indent    </Typography>")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 2)
        }
        code.appendLine("$indent  </Toolbar>")
        code.appendLine("$indent</AppBar>")
    }

    private fun generateBottomNav(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val value = component.properties["value"] ?: "0"
        val onChange = component.eventHandlers["onChange"] ?: "(e, v) => {}"

        code.appendLine("${indent}<BottomNavigation value={$value} onChange={$onChange}>")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent</BottomNavigation>")
    }

    private fun generateTabs(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        val value = component.properties["value"] ?: "0"
        val onChange = component.eventHandlers["onChange"] ?: "(e, v) => {}"

        code.appendLine("${indent}<Tabs value={$value} onChange={$onChange}>")
        component.children.forEach { child ->
            generateComponent(child as ComponentNode, code, indentLevel + 1)
        }
        code.appendLine("$indent</Tabs>")
    }

    private fun generateGenericComponent(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
        // Try to render children in a Box as fallback
        if (component.children.isNotEmpty()) {
            code.appendLine("${indent}<Box> {/* ${component.type} */}")
            component.children.forEach { child ->
                generateComponent(child as ComponentNode, code, indentLevel + 1)
            }
            code.appendLine("$indent</Box>")
        } else {
            code.appendLine("$indent{/* TODO: Implement ${component.type} */}")
        }
    }

    private fun formatPropertyValue(value: PropertyValue): String {
        return when (value) {
            is PropertyValue.StringValue -> "\"${value.value}\""
            is PropertyValue.IntValue -> value.value.toString()
            is PropertyValue.DoubleValue -> value.value.toString()
            is PropertyValue.BoolValue -> value.value.toString()
            is PropertyValue.EnumValue -> "\"${value.value}\""
            is PropertyValue.ListValue -> "[${value.items.joinToString(", ") { formatPropertyValue(it) }}]"
            is PropertyValue.MapValue -> "{${value.items.entries.joinToString(", ") {
                "${it.key}: ${formatPropertyValue(it.value)}"
            }}}"
            is PropertyValue.ReferenceValue -> value.ref
        }
    }

    private fun mapKotlinTypeToTypeScript(kotlinType: String): String {
        return when (kotlinType) {
            "String" -> "string"
            "Int" -> "number"
            "Double" -> "number"
            "Float" -> "number"
            "Boolean" -> "boolean"
            "List" -> "Array<any>"
            "Map" -> "Record<string, any>"
            else -> "any"
        }
    }
}

/**
 * VoiceUIConverter.kt - Automated conversion helper for Android UI to VoiceUI
 * 
 * This tool helps convert XML layouts and identify conversion points
 */

package com.augmentalis.tools

import java.io.File
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converter tool to help migrate Android UI to VoiceUI
 */
class VoiceUIConverter {
    
    companion object {
        private const val INDENT = "    "
    }
    
    // Mapping of XML elements to VoiceUI components
    private val componentMap = mapOf(
        "Button" to "VoiceUIButton",
        "TextView" to "VoiceUIText",
        "EditText" to "VoiceUITextField",
        "Switch" to "VoiceUISwitch",
        "CheckBox" to "VoiceUICheckbox",
        "RadioButton" to "VoiceUIRadioButton",
        "ImageButton" to "VoiceUIIconButton",
        "RecyclerView" to "VoiceUIList",
        "androidx.recyclerview.widget.RecyclerView" to "VoiceUIList",
        "ScrollView" to "VoiceUIScrollView",
        "Toolbar" to "VoiceUITopBar",
        "com.google.android.material.appbar.MaterialToolbar" to "VoiceUITopBar",
        "ProgressBar" to "VoiceUIProgress",
        "SeekBar" to "VoiceUISlider"
    )
    
    // Common attributes to convert
    private val attributeMap = mapOf(
        "android:text" to "text",
        "android:hint" to "label",
        "android:contentDescription" to "voiceLabel",
        "android:enabled" to "enabled"
    )
    
    /**
     * Convert XML layout file to VoiceUI Compose
     */
    fun convertXmlToVoiceUI(xmlFile: File): String {
        val doc = parseXml(xmlFile)
        val root = doc.documentElement
        
        val composableCode = StringBuilder()
        composableCode.appendLine("@Composable")
        composableCode.appendLine("fun ${xmlFile.nameWithoutExtension.toComposableName()}() {")
        composableCode.appendLine("${INDENT}val voiceUI = LocalVoiceUI.current")
        composableCode.appendLine()
        
        // Add voice command registration
        composableCode.appendLine("${INDENT}// Register voice commands")
        composableCode.appendLine("${INDENT}LaunchedEffect(Unit) {")
        composableCode.appendLine("${INDENT}${INDENT}voiceUI.voiceCommandSystem.apply {")
        
        val voiceCommands = extractVoiceCommands(root)
        voiceCommands.forEach { command ->
            composableCode.appendLine("${INDENT}${INDENT}${INDENT}registerCommand(\"$command\", \"${command.toActionName()}\")")
        }
        
        composableCode.appendLine("${INDENT}${INDENT}}")
        composableCode.appendLine("${INDENT}}")
        composableCode.appendLine()
        
        // Convert layout
        val layoutCode = convertElement(root, 1)
        composableCode.append(layoutCode)
        
        composableCode.appendLine("}")
        
        return composableCode.toString()
    }
    
    /**
     * Convert XML element to Compose code
     */
    private fun convertElement(element: Element, indentLevel: Int): String {
        val indent = INDENT.repeat(indentLevel)
        val code = StringBuilder()
        
        val elementType = element.tagName
        val voiceUIComponent = componentMap[elementType] ?: elementType
        
        when (elementType) {
            "LinearLayout" -> {
                val orientation = element.getAttribute("android:orientation")
                val composable = if (orientation == "horizontal") "Row" else "Column"
                code.appendLine("$indent$composable(")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent) {")
                code.append(convertChildren(element, indentLevel + 1))
                code.appendLine("$indent}")
            }
            
            "RelativeLayout", "FrameLayout", "ConstraintLayout" -> {
                code.appendLine("${indent}Box(")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent) {")
                code.append(convertChildren(element, indentLevel + 1))
                code.appendLine("$indent}")
            }
            
            "Button" -> {
                val text = element.getAttribute("android:text").cleanResource()
                val id = element.getAttribute("android:id").extractId()
                
                code.appendLine("$indent$voiceUIComponent(")
                code.appendLine("${indent}${INDENT}text = \"$text\",")
                code.appendLine("${indent}${INDENT}voiceCommand = \"${text.toLowerCase()}\",")
                code.appendLine("${indent}${INDENT}onClick = { /* TODO: Handle $id click */ },")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent)")
            }
            
            "EditText" -> {
                val hint = element.getAttribute("android:hint").cleanResource()
                val inputType = element.getAttribute("android:inputType")
                val id = element.getAttribute("android:id").extractId()
                
                code.appendLine("${indent}var ${id}Value by remember { mutableStateOf(\"\") }")
                code.appendLine()
                code.appendLine("$indent$voiceUIComponent(")
                code.appendLine("${indent}${INDENT}value = ${id}Value,")
                code.appendLine("${indent}${INDENT}onValueChange = { ${id}Value = it },")
                code.appendLine("${indent}${INDENT}label = \"$hint\",")
                code.appendLine("${indent}${INDENT}voiceCommand = \"enter ${hint.toLowerCase()}\",")
                
                if (inputType.contains("Password")) {
                    code.appendLine("${indent}${INDENT}voiceDictation = false, // No voice for passwords")
                    code.appendLine("${indent}${INDENT}visualTransformation = PasswordVisualTransformation(),")
                } else {
                    code.appendLine("${indent}${INDENT}voiceDictation = true,")
                }
                
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent)")
            }
            
            "TextView" -> {
                val text = element.getAttribute("android:text").cleanResource()
                
                code.appendLine("$indent$voiceUIComponent(")
                code.appendLine("${indent}${INDENT}text = \"$text\",")
                code.appendLine("${indent}${INDENT}voiceLabel = \"$text\",")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent)")
            }
            
            "RecyclerView", "androidx.recyclerview.widget.RecyclerView" -> {
                val id = element.getAttribute("android:id").extractId()
                
                code.appendLine("$indent$voiceUIComponent(")
                code.appendLine("${indent}${INDENT}items = ${id}Items, // TODO: Provide items")
                code.appendLine("${indent}${INDENT}voiceNavigable = true,")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent) { item ->")
                code.appendLine("${indent}${INDENT}// TODO: Define item composable")
                code.appendLine("${indent}${INDENT}VoiceUIListItem(item)")
                code.appendLine("$indent}")
            }
            
            else -> {
                // Generic conversion
                code.appendLine("$indent// TODO: Convert $elementType")
                code.appendLine("${indent}Box(")
                code.append(convertModifiers(element, indentLevel + 1))
                code.appendLine("$indent) {")
                code.append(convertChildren(element, indentLevel + 1))
                code.appendLine("$indent}")
            }
        }
        
        return code.toString()
    }
    
    /**
     * Convert XML attributes to Compose modifiers
     */
    private fun convertModifiers(element: Element, indentLevel: Int): String {
        val indent = INDENT.repeat(indentLevel)
        val modifiers = mutableListOf<String>()
        
        // Layout params
        val width = element.getAttribute("android:layout_width")
        val height = element.getAttribute("android:layout_height")
        
        when (width) {
            "match_parent" -> modifiers.add("fillMaxWidth()")
            "wrap_content" -> {} // Default
            else -> if (width.isNotEmpty()) modifiers.add("width(${width.toDp()})")
        }
        
        when (height) {
            "match_parent" -> modifiers.add("fillMaxHeight()")
            "wrap_content" -> {} // Default
            else -> if (height.isNotEmpty()) modifiers.add("height(${height.toDp()})")
        }
        
        // Padding
        val padding = element.getAttribute("android:padding")
        if (padding.isNotEmpty()) {
            modifiers.add("padding(${padding.toDp()})")
        }
        
        // Margins
        val margin = element.getAttribute("android:layout_margin")
        if (margin.isNotEmpty()) {
            modifiers.add("padding(${margin.toDp()}) // margin")
        }
        
        // Visibility
        val visibility = element.getAttribute("android:visibility")
        if (visibility == "gone" || visibility == "invisible") {
            modifiers.add("// TODO: Handle visibility = $visibility")
        }
        
        // Voice UI specific modifiers
        modifiers.add("voiceUIGestures()")
        
        // Build modifier chain
        return if (modifiers.isNotEmpty()) {
            "${indent}modifier = Modifier\n${indent}${INDENT}.${modifiers.joinToString("\n${indent}${INDENT}.")},\n"
        } else {
            ""
        }
    }
    
    /**
     * Convert child elements
     */
    private fun convertChildren(element: Element, indentLevel: Int): String {
        val code = StringBuilder()
        val children = element.childNodes
        
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element) {
                code.append(convertElement(child, indentLevel))
            }
        }
        
        return code.toString()
    }
    
    /**
     * Extract potential voice commands from the layout
     */
    private fun extractVoiceCommands(root: Element): List<String> {
        val commands = mutableListOf<String>()
        
        findElementsByTag(root, "Button").forEach { button ->
            val text = button.getAttribute("android:text").cleanResource()
            if (text.isNotEmpty()) {
                commands.add(text.toLowerCase())
            }
        }
        
        findElementsByTag(root, "EditText").forEach { editText ->
            val hint = editText.getAttribute("android:hint").cleanResource()
            if (hint.isNotEmpty()) {
                commands.add("enter ${hint.toLowerCase()}")
            }
        }
        
        return commands
    }
    
    /**
     * Find all elements by tag name
     */
    private fun findElementsByTag(root: Element, tagName: String): List<Element> {
        val elements = mutableListOf<Element>()
        val nodeList = root.getElementsByTagName(tagName)
        for (i in 0 until nodeList.length) {
            elements.add(nodeList.item(i) as Element)
        }
        return elements
    }
    
    /**
     * Parse XML file
     */
    private fun parseXml(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }
    
    // Extension functions
    private fun String.extractId(): String {
        return if (startsWith("@+id/") || startsWith("@id/")) {
            substring(indexOf('/') + 1)
        } else this
    }
    
    private fun String.cleanResource(): String {
        return if (startsWith("@string/")) {
            "getString(R.string.${substring(8)})"
        } else this
    }
    
    private fun String.toDp(): String {
        return if (endsWith("dp")) {
            "${removeSuffix("dp")}.dp"
        } else if (endsWith("sp")) {
            "${removeSuffix("sp")}.sp"
        } else {
            "${this}.dp"
        }
    }
    
    private fun String.toComposableName(): String {
        return split("_").joinToString("") { 
            it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
        } + "Screen"
    }
    
    private fun String.toActionName(): String {
        return replace(" ", "_").toUpperCase()
    }
}

/**
 * Usage example
 */
fun main() {
    val converter = VoiceUIConverter()
    
    // Convert a single file
    val xmlFile = File("res/layout/activity_main.xml")
    val composableCode = converter.convertXmlToVoiceUI(xmlFile)
    
    println("Generated VoiceUI Compose code:")
    println(composableCode)
    
    // Save to file
    File("converted/MainScreen.kt").writeText(composableCode)
    
    // Batch convert all layouts
    File("res/layout").listFiles { file -> 
        file.extension == "xml" 
    }?.forEach { xmlFile ->
        val converted = converter.convertXmlToVoiceUI(xmlFile)
        val outputFile = File("converted/${xmlFile.nameWithoutExtension}.kt")
        outputFile.writeText(converted)
        println("Converted: ${xmlFile.name} -> ${outputFile.name}")
    }
}
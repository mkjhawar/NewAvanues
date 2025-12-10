# MagicUI Editor System - Complete Specification

**Date:** 2025-10-19 12:11:00 PDT
**Author:** Manoj Jhawar
**Purpose:** Unified visual designer and code editor for MagicUI with Unity-inspired features
**Status:** DESIGN COMPLETE - READY FOR IMPLEMENTATION

---

## Executive Summary

This document specifies a comprehensive MagicUI Editor system that combines:
1. **Visual Designer** - Drag-and-drop UI creation (Unity-inspired)
2. **Code Editor** - Reusable code editor component with undo/redo/clear
3. **Template System** - Hybrid approach (simple quick templates + advanced options)
4. **Code Generation** - Integration with MagicElements library

**Key Features:**
- Unity-inspired visual scripting/design workflow
- Command Pattern for undo/redo (industry standard)
- Stack-based state management (efficient memory usage)
- Clear/reset functionality
- Live preview
- Code generation to MagicUI DSL
- Integration with VOS4 systems (UUID, Commands, etc.)

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Reusable Code Editor Component](#reusable-code-editor-component)
3. [Visual Designer](#visual-designer)
4. [Hybrid Template System](#hybrid-template-system)
5. [Code Generation Integration](#code-generation-integration)
6. [Implementation Plan](#implementation-plan)

---

## System Architecture

### 3-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 3: Visual Designer (UI)                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Component Palette  │  Design Canvas  │  Properties   │ │
│  │  ┌───────────────┐  │  ┌───────────┐  │  ┌──────────┐│ │
│  │  │ • Text        │  │  │           │  │  │ Text:    ││ │
│  │  │ • Button      │  │  │  [Button] │  │  │ "Hello"  ││ │
│  │  │ • Input       │  │  │           │  │  │          ││ │
│  │  │ • Slider      │  │  │           │  │  │ Color:   ││ │
│  │  │ • Image       │  │  │           │  │  │ Blue     ││ │
│  │  │ ...           │  │  │           │  │  │          ││ │
│  │  └───────────────┘  │  └───────────┘  │  └──────────┘│ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│  Layer 2: Code Editor (Reusable Component)                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Toolbar: [Undo] [Redo] [Clear] [Copy] [Export]       │ │
│  │  ┌────────────────────────────────────────────────────┐│ │
│  │  │ MagicScreen("login") {                            ││ │
│  │  │     text("Welcome")                               ││ │
│  │  │     button("Sign In") { login() }                 ││ │
│  │  │ }                                                 ││ │
│  │  └────────────────────────────────────────────────────┘│ │
│  │  Status: [Modified] [Line: 3, Col: 5]                 │ │
│  └────────────────────────────────────────────────────────┘ │
│  • Undo/Redo (Command Pattern)                              │
│  • Clear/Reset functionality                                │
│  • Syntax highlighting (optional)                           │
│  • Line numbers                                             │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│  Layer 1: Code Generation & Template System                │
│  • Hybrid Templates (simple + advanced)                     │
│  • MagicElements integration                                │
│  • VOS4 UUID/Command registration                           │
│  • Live preview rendering                                   │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

| Component | Purpose | Location |
|-----------|---------|----------|
| **MagicCodeEditor** | Reusable code editor with undo/redo/clear | `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/editor/` |
| **MagicVisualDesigner** | Visual drag-and-drop designer | `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/designer/` |
| **MagicTemplateEngine** | Hybrid template system | `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/templates/` (already exists) |
| **CodeGenerator** | Generate MagicUI DSL from visual design | `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/codegen/` |

---

## Reusable Code Editor Component

### Design Goals

1. **Reusable** - Can be used throughout VOS4 for any code editing needs
2. **Full-featured** - Undo, redo, clear, copy, export
3. **Efficient** - Command pattern (store actions, not states)
4. **Android-native** - Compose UI, no web dependencies

### Component Structure

**File:** `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/editor/MagicCodeEditor.kt`

```kotlin
/**
 * MagicCodeEditor - Reusable code editor component
 *
 * Features:
 * - Undo/Redo (Command Pattern with stack-based history)
 * - Clear/Reset functionality
 * - Syntax highlighting (optional, future)
 * - Line numbers
 * - Copy/Export
 *
 * Usage:
 * ```kotlin
 * val editorState = rememberMagicCodeEditorState(
 *     initialCode = "MagicScreen(\"home\") { }"
 * )
 *
 * MagicCodeEditor(
 *     state = editorState,
 *     modifier = Modifier.fillMaxSize(),
 *     onCodeChange = { newCode ->
 *         // Handle code changes
 *     }
 * )
 *
 * // Programmatic control
 * Button("Undo") { editorState.undo() }
 * Button("Redo") { editorState.redo() }
 * Button("Clear") { editorState.clear() }
 * ```
 */
@Composable
fun MagicCodeEditor(
    state: MagicCodeEditorState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    showToolbar: Boolean = true,
    onCodeChange: ((String) -> Unit)? = null
)
```

### State Management

```kotlin
/**
 * MagicCodeEditorState - Manages editor state and undo/redo history
 *
 * Uses Command Pattern for efficient undo/redo
 */
class MagicCodeEditorState(
    initialCode: String = "",
    maxHistorySize: Int = 100
) {
    // Current code
    var code by mutableStateOf(initialCode)
        private set

    // Undo/Redo stacks
    private val undoStack = mutableStateListOf<EditorCommand>()
    private val redoStack = mutableStateListOf<EditorCommand>()

    // State properties
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val isModified: Boolean get() = code != initialCode

    /**
     * Execute a command (edit operation)
     */
    fun executeCommand(command: EditorCommand) {
        command.execute()
        undoStack.add(command)
        redoStack.clear() // Clear redo stack on new edit

        // Limit history size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
    }

    /**
     * Undo last command
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        val command = undoStack.removeLast()
        command.undo()
        redoStack.add(command)
    }

    /**
     * Redo last undone command
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        val command = redoStack.removeLast()
        command.execute()
        undoStack.add(command)
    }

    /**
     * Clear all content (with undo support)
     */
    fun clear() {
        executeCommand(ReplaceAllCommand(this, code, ""))
    }

    /**
     * Reset to initial code (with undo support)
     */
    fun reset() {
        executeCommand(ReplaceAllCommand(this, code, initialCode))
    }

    /**
     * Insert text at cursor position
     */
    fun insertText(text: String, position: Int) {
        executeCommand(InsertTextCommand(this, text, position))
    }

    /**
     * Delete text range
     */
    fun deleteText(start: Int, end: Int) {
        executeCommand(DeleteTextCommand(this, start, end, code.substring(start, end)))
    }

    /**
     * Replace text range
     */
    fun replaceText(start: Int, end: Int, newText: String) {
        executeCommand(ReplaceTextCommand(this, start, end, code.substring(start, end), newText))
    }
}

/**
 * Remember editor state across recompositions
 */
@Composable
fun rememberMagicCodeEditorState(
    initialCode: String = "",
    maxHistorySize: Int = 100
): MagicCodeEditorState {
    return remember(initialCode, maxHistorySize) {
        MagicCodeEditorState(initialCode, maxHistorySize)
    }
}
```

### Command Pattern Implementation

```kotlin
/**
 * EditorCommand - Base interface for all edit operations
 *
 * Command Pattern: Encapsulates edit operations as objects
 * that can be executed, undone, and stored in history
 */
interface EditorCommand {
    fun execute()
    fun undo()
}

/**
 * InsertTextCommand - Insert text at position
 */
class InsertTextCommand(
    private val state: MagicCodeEditorState,
    private val text: String,
    private val position: Int
) : EditorCommand {
    override fun execute() {
        val currentCode = state.code
        state.code = currentCode.substring(0, position) +
                     text +
                     currentCode.substring(position)
    }

    override fun undo() {
        val currentCode = state.code
        state.code = currentCode.substring(0, position) +
                     currentCode.substring(position + text.length)
    }
}

/**
 * DeleteTextCommand - Delete text range
 */
class DeleteTextCommand(
    private val state: MagicCodeEditorState,
    private val start: Int,
    private val end: Int,
    private val deletedText: String
) : EditorCommand {
    override fun execute() {
        val currentCode = state.code
        state.code = currentCode.substring(0, start) +
                     currentCode.substring(end)
    }

    override fun undo() {
        val currentCode = state.code
        state.code = currentCode.substring(0, start) +
                     deletedText +
                     currentCode.substring(start)
    }
}

/**
 * ReplaceTextCommand - Replace text range
 */
class ReplaceTextCommand(
    private val state: MagicCodeEditorState,
    private val start: Int,
    private val end: Int,
    private val oldText: String,
    private val newText: String
) : EditorCommand {
    override fun execute() {
        val currentCode = state.code
        state.code = currentCode.substring(0, start) +
                     newText +
                     currentCode.substring(end)
    }

    override fun undo() {
        val currentCode = state.code
        state.code = currentCode.substring(0, start) +
                     oldText +
                     currentCode.substring(start + newText.length)
    }
}

/**
 * ReplaceAllCommand - Replace entire content
 * Used for clear/reset operations
 */
class ReplaceAllCommand(
    private val state: MagicCodeEditorState,
    private val oldCode: String,
    private val newCode: String
) : EditorCommand {
    override fun execute() {
        state.code = newCode
    }

    override fun undo() {
        state.code = oldCode
    }
}
```

### UI Implementation

```kotlin
/**
 * MagicCodeEditor - Main editor composable
 */
@Composable
fun MagicCodeEditor(
    state: MagicCodeEditorState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    showLineNumbers: Boolean = true,
    showToolbar: Boolean = true,
    syntaxHighlighting: Boolean = false, // Future feature
    onCodeChange: ((String) -> Unit)? = null
) {
    Column(modifier = modifier) {
        // Toolbar
        if (showToolbar) {
            CodeEditorToolbar(
                state = state,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Editor content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2B2B2B)) // Dark background
        ) {
            // Line numbers
            if (showLineNumbers) {
                LineNumbers(
                    code = state.code,
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(Color(0xFF3C3F41))
                        .padding(horizontal = 8.dp)
                )
            }

            // Text input field
            BasicTextField(
                value = state.code,
                onValueChange = { newCode ->
                    if (!readOnly) {
                        // Calculate diff and create command
                        val command = createDiffCommand(state, state.code, newCode)
                        state.executeCommand(command)
                        onCodeChange?.invoke(newCode)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFFA9B7C6) // Light gray text
                ),
                readOnly = readOnly
            )
        }

        // Status bar
        StatusBar(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * CodeEditorToolbar - Toolbar with action buttons
 */
@Composable
private fun CodeEditorToolbar(
    state: MagicCodeEditorState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF3C3F41))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Undo button
        IconButton(
            onClick = { state.undo() },
            enabled = state.canUndo
        ) {
            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = "Undo",
                tint = if (state.canUndo) Color.White else Color.Gray
            )
        }

        // Redo button
        IconButton(
            onClick = { state.redo() },
            enabled = state.canRedo
        ) {
            Icon(
                imageVector = Icons.Default.Redo,
                contentDescription = "Redo",
                tint = if (state.canRedo) Color.White else Color.Gray
            )
        }

        Divider(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp),
            color = Color.Gray
        )

        // Clear button
        IconButton(
            onClick = { state.clear() },
            enabled = state.code.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = if (state.code.isNotEmpty()) Color.White else Color.Gray
            )
        }

        // Reset button
        IconButton(
            onClick = { state.reset() }
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset",
                tint = Color.White
            )
        }

        Divider(
            modifier = Modifier
                .width(1.dp)
                .height(24.dp),
            color = Color.Gray
        )

        // Copy button
        IconButton(
            onClick = { /* Copy to clipboard */ }
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = Color.White
            )
        }

        // Export button (future feature)
        IconButton(
            onClick = { /* Export code */ }
        ) {
            Icon(
                imageVector = Icons.Default.SaveAlt,
                contentDescription = "Export",
                tint = Color.White
            )
        }
    }
}

/**
 * LineNumbers - Display line numbers
 */
@Composable
private fun LineNumbers(
    code: String,
    modifier: Modifier = Modifier
) {
    val lineCount = code.count { it == '\n' } + 1

    Column(modifier = modifier) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFF606366) // Gray line numbers
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

/**
 * StatusBar - Display editor status
 */
@Composable
private fun StatusBar(
    state: MagicCodeEditorState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF3C3F41))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (state.isModified) "Modified" else "Unchanged",
            style = TextStyle(
                fontSize = 12.sp,
                color = if (state.isModified) Color.Yellow else Color.Gray
            )
        )

        Text(
            text = "Lines: ${state.code.count { it == '\n' } + 1}",
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.Gray
            )
        )
    }
}

/**
 * Create diff command from old and new code
 */
private fun createDiffCommand(
    state: MagicCodeEditorState,
    oldCode: String,
    newCode: String
): EditorCommand {
    // Simple implementation: replace all
    // Future: implement smart diffing for better undo granularity
    return ReplaceAllCommand(state, oldCode, newCode)
}
```

### Key Features

1. **Undo/Redo**
   - Command Pattern (industry standard)
   - Stack-based history (memory efficient)
   - Configurable history size (default: 100 operations)
   - Clear redo stack on new edit (standard UX)

2. **Clear/Reset**
   - Clear: Empty all content (with undo support)
   - Reset: Return to initial code (with undo support)
   - Both operations are undoable

3. **Toolbar Actions**
   - Undo/Redo buttons (disabled when stack empty)
   - Clear button (disabled when empty)
   - Reset button (always enabled)
   - Copy/Export buttons (future features)

4. **Visual Features**
   - Dark theme (IntelliJ-inspired)
   - Monospace font
   - Line numbers
   - Status bar (modified state, line count)
   - Disabled state visual feedback

---

## Visual Designer

### Design Goals

1. **Unity-inspired** - Familiar workflow for Unity developers
2. **Drag-and-drop** - Component palette → design canvas
3. **Property inspector** - Edit component properties
4. **Live code generation** - See MagicUI DSL in real-time
5. **Live preview** - See actual UI rendering

### Component Structure

**File:** `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/designer/MagicVisualDesigner.kt`

```kotlin
/**
 * MagicVisualDesigner - Unity-inspired visual UI designer
 *
 * Layout:
 * ┌──────────────┬──────────────────────┬──────────────┐
 * │  Component   │   Design Canvas      │  Properties  │
 * │  Palette     │   (Drag & Drop)      │  Inspector   │
 * │              │                      │              │
 * │ • Text       │    ┌───────────┐    │ Component:   │
 * │ • Button     │    │  [Button] │    │ Button       │
 * │ • Input      │    └───────────┘    │              │
 * │ • Slider     │                      │ Text:        │
 * │ • Image      │    ┌───────────┐    │ "Click me"   │
 * │ • Card       │    │   [Text]  │    │              │
 * │ ...          │    └───────────┘    │ Color:       │
 * │              │                      │ [Blue ▼]     │
 * └──────────────┴──────────────────────┴──────────────┘
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  Generated Code (Live)                                  │
 * │  [Undo] [Redo] [Clear] [Copy] [Export]                 │
 * │  ┌───────────────────────────────────────────────────┐ │
 * │  │ MagicScreen("design") {                           │ │
 * │  │     text("Hello")                                 │ │
 * │  │     button("Click me").backgroundColor(Blue)      │ │
 * │  │ }                                                 │ │
 * │  └───────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  Live Preview                                           │
 * │  ┌───────────────────────────────────────────────────┐ │
 * │  │  Hello                                            │ │
 * │  │  ┌─────────────┐                                 │ │
 * │  │  │  Click me   │                                 │ │
 * │  │  └─────────────┘                                 │ │
 * │  └───────────────────────────────────────────────────┘ │
 * └─────────────────────────────────────────────────────────┘
 */
@Composable
fun MagicVisualDesigner(
    modifier: Modifier = Modifier,
    onCodeGenerated: ((String) -> Unit)? = null
) {
    val designerState = rememberMagicDesignerState()

    Column(modifier = modifier) {
        // Main designer (3-panel layout)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left: Component Palette
            ComponentPalette(
                onComponentDragged = { component ->
                    designerState.addComponent(component)
                },
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
            )

            // Center: Design Canvas
            DesignCanvas(
                state = designerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Right: Properties Inspector
            PropertiesInspector(
                selectedComponent = designerState.selectedComponent,
                onPropertyChanged = { property, value ->
                    designerState.updateProperty(property, value)
                },
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
            )
        }

        // Bottom: Generated Code (with MagicCodeEditor)
        CodeSection(
            designerState = designerState,
            onCodeGenerated = onCodeGenerated,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // Bottom: Live Preview
        LivePreview(
            code = designerState.generatedCode,
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        )
    }
}
```

### Designer State Management

```kotlin
/**
 * MagicDesignerState - Manages visual designer state
 */
class MagicDesignerState {
    // Components on canvas
    var components by mutableStateOf<List<DesignComponent>>(emptyList())
        private set

    // Selected component for property editing
    var selectedComponent by mutableStateOf<DesignComponent?>(null)
        private set

    // Generated MagicUI code
    var generatedCode by mutableStateOf("")
        private set

    /**
     * Add component to canvas
     */
    fun addComponent(componentType: ComponentType) {
        val newComponent = DesignComponent(
            id = UUID.randomUUID().toString(),
            type = componentType,
            properties = componentType.defaultProperties()
        )
        components = components + newComponent
        regenerateCode()
    }

    /**
     * Remove component from canvas
     */
    fun removeComponent(componentId: String) {
        components = components.filterNot { it.id == componentId }
        if (selectedComponent?.id == componentId) {
            selectedComponent = null
        }
        regenerateCode()
    }

    /**
     * Select component for editing
     */
    fun selectComponent(componentId: String) {
        selectedComponent = components.find { it.id == componentId }
    }

    /**
     * Update component property
     */
    fun updateProperty(property: String, value: Any) {
        selectedComponent?.let { selected ->
            components = components.map { component ->
                if (component.id == selected.id) {
                    component.copy(
                        properties = component.properties + (property to value)
                    )
                } else {
                    component
                }
            }
            selectedComponent = components.find { it.id == selected.id }
            regenerateCode()
        }
    }

    /**
     * Regenerate MagicUI code from components
     */
    private fun regenerateCode() {
        generatedCode = CodeGenerator.generate(components)
    }
}

/**
 * DesignComponent - Represents a component on the design canvas
 */
data class DesignComponent(
    val id: String,
    val type: ComponentType,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * ComponentType - Available component types
 */
enum class ComponentType {
    TEXT, BUTTON, INPUT, SLIDER, IMAGE, CARD, TOGGLE, CHECKBOX;

    fun defaultProperties(): Map<String, Any> {
        return when (this) {
            TEXT -> mapOf("text" to "Hello", "style" to "BODY")
            BUTTON -> mapOf("text" to "Button", "color" to "Primary")
            INPUT -> mapOf("placeholder" to "Enter text", "label" to "Input")
            SLIDER -> mapOf("value" to 0.5f, "range" to "0..1")
            IMAGE -> mapOf("source" to "placeholder.png", "width" to 100)
            CARD -> mapOf("title" to "Card", "elevated" to true)
            TOGGLE -> mapOf("label" to "Toggle", "checked" to false)
            CHECKBOX -> mapOf("label" to "Checkbox", "checked" to false)
        }
    }
}

@Composable
fun rememberMagicDesignerState(): MagicDesignerState {
    return remember { MagicDesignerState() }
}
```

---

## Hybrid Template System

### Design Philosophy

**Hybrid Approach:**
- **Simple quick templates** - For 90% of use cases, one-liners
- **Advanced options** - For power users, optional builder pattern

### Simple Templates (Primary)

**File:** `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/templates/SimpleTemplates.kt`

```kotlin
/**
 * Simple MagicUI Templates - Quick one-line component creation
 *
 * Design: Extension functions on MagicUIScope for simplicity
 * Target: 10-20 lines per component (vs 100+ for class-based)
 */

// ========== BASIC COMPONENTS ==========

/**
 * Text component - Display text
 *
 * Quick usage:
 * ```
 * text("Hello")
 * text("Title", style = TextStyle.HEADLINE)
 * ```
 */
@Composable
fun MagicUIScope.text(
    content: String,
    style: TextStyle = TextStyle.BODY
) {
    // Auto-translate
    val translated = localizationIntegration.translate(content)

    // Auto-register with UUID
    uuidIntegration.registerComponent(
        name = translated,
        type = "text"
    )

    // Render
    when (style) {
        TextStyle.HEADLINE -> Text(translated, style = MaterialTheme.typography.headlineMedium)
        TextStyle.TITLE -> Text(translated, style = MaterialTheme.typography.titleLarge)
        TextStyle.BODY -> Text(translated, style = MaterialTheme.typography.bodyLarge)
        TextStyle.CAPTION -> Text(translated, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Button component - Interactive button
 *
 * Quick usage:
 * ```
 * button("Click me") { doSomething() }
 * button("Sign In", color = Color.Blue) { login() }
 * ```
 */
@Composable
fun MagicUIScope.button(
    text: String,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Auto-translate
    val translated = localizationIntegration.translate(text)

    // Auto-register with UUID + voice command
    uuidIntegration.registerComponent(
        name = translated,
        type = "button",
        actions = mapOf("click" to { _ -> onClick() })
    )

    // Auto-register voice command
    commandIntegration.registerCommand(
        command = "click $translated",
        action = onClick
    )

    // Render
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = if (color != Color.Unspecified) {
            ButtonDefaults.buttonColors(containerColor = color)
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Text(translated)
    }
}

/**
 * Input component - Text input field
 *
 * Quick usage:
 * ```
 * val email = state("")
 * input(email, label = "Email")
 * input(email, label = "Email", placeholder = "you@example.com")
 * ```
 */
@Composable
fun MagicUIScope.input(
    value: MutableState<String>,
    label: String = "",
    placeholder: String = "",
    enabled: Boolean = true
) {
    // Auto-translate
    val translatedLabel = localizationIntegration.translate(label)
    val translatedPlaceholder = localizationIntegration.translate(placeholder)

    // Auto-register with UUID
    uuidIntegration.registerComponent(
        name = translatedLabel.ifEmpty { "input" },
        type = "input"
    )

    // Render
    OutlinedTextField(
        value = value.value,
        onValueChange = { value.value = it },
        label = if (translatedLabel.isNotEmpty()) {
            { Text(translatedLabel) }
        } else null,
        placeholder = if (translatedPlaceholder.isNotEmpty()) {
            { Text(translatedPlaceholder) }
        } else null,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Slider component - Value slider
 *
 * Quick usage:
 * ```
 * val volume = state(0.5f)
 * slider(volume)
 * slider(volume, range = 0f..100f)
 * ```
 */
@Composable
fun MagicUIScope.slider(
    value: MutableState<Float>,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    label: String = ""
) {
    // Auto-translate
    val translatedLabel = localizationIntegration.translate(label)

    // Auto-register with UUID
    uuidIntegration.registerComponent(
        name = translatedLabel.ifEmpty { "slider" },
        type = "slider"
    )

    // Render
    Column {
        if (translatedLabel.isNotEmpty()) {
            Text(translatedLabel, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value.value,
            onValueChange = { value.value = it },
            valueRange = range,
            steps = steps
        )
    }
}

// ========== LAYOUT COMPONENTS ==========

/**
 * Card component - Material card container
 *
 * Quick usage:
 * ```
 * card {
 *     text("Card title")
 *     button("Action") { }
 * }
 * card(elevated = true) { ... }
 * ```
 */
@Composable
fun MagicUIScope.card(
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = if (elevated) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * List component - Scrollable list
 *
 * Quick usage:
 * ```
 * list(items = listOf("Item 1", "Item 2", "Item 3")) { item ->
 *     text(item)
 * }
 * ```
 */
@Composable
fun <T> MagicUIScope.list(
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn {
        items(items) { item ->
            itemContent(item)
        }
    }
}

// ========== FORM COMPONENTS ==========

/**
 * Toggle component - Switch toggle
 *
 * Quick usage:
 * ```
 * val darkMode = state(false)
 * toggle(darkMode, label = "Dark Mode")
 * ```
 */
@Composable
fun MagicUIScope.toggle(
    checked: MutableState<Boolean>,
    label: String = "",
    enabled: Boolean = true
) {
    // Auto-translate
    val translatedLabel = localizationIntegration.translate(label)

    // Auto-register with UUID + voice command
    uuidIntegration.registerComponent(
        name = translatedLabel.ifEmpty { "toggle" },
        type = "toggle",
        actions = mapOf(
            "toggle" to { _ -> checked.value = !checked.value }
        )
    )

    // Auto-register voice command
    if (translatedLabel.isNotEmpty()) {
        commandIntegration.registerCommand(
            command = "toggle $translatedLabel",
            action = { checked.value = !checked.value }
        )
    }

    // Render
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (translatedLabel.isNotEmpty()) {
            Text(translatedLabel)
        }
        Switch(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            enabled = enabled
        )
    }
}

/**
 * Checkbox component - Checkbox with label
 *
 * Quick usage:
 * ```
 * val agreed = state(false)
 * checkbox(agreed, label = "I agree to terms")
 * ```
 */
@Composable
fun MagicUIScope.checkbox(
    checked: MutableState<Boolean>,
    label: String = "",
    enabled: Boolean = true
) {
    // Auto-translate
    val translatedLabel = localizationIntegration.translate(label)

    // Auto-register with UUID
    uuidIntegration.registerComponent(
        name = translatedLabel.ifEmpty { "checkbox" },
        type = "checkbox"
    )

    // Render
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            enabled = enabled
        )
        if (translatedLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(translatedLabel)
        }
    }
}
```

### Advanced Templates (Optional)

**File:** `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/templates/AdvancedTemplates.kt`

```kotlin
/**
 * Advanced MagicUI Templates - Builder pattern for power users
 *
 * Design: Optional builder classes for advanced customization
 * Target: Keep existing ButtonComponent, SliderComponent for those who need them
 */

/**
 * Advanced button with full builder pattern
 *
 * Usage:
 * ```
 * buttonAdvanced("Sign In") { login() }
 *     .backgroundColor(Color.Blue)
 *     .textColor(Color.White)
 *     .contentPadding(PaddingValues(16.dp))
 *     .disabled(isLoading)
 *     .invoke()
 * ```
 */
@Composable
fun MagicUIScope.buttonAdvanced(
    text: String,
    onClick: () -> Unit
): ButtonComponent {
    return ButtonComponent(text, onClick)
}

// Keep existing ButtonComponent class for advanced users
// (Already exists in Button.kt)

/**
 * Advanced slider with full builder pattern
 *
 * Usage:
 * ```
 * sliderAdvanced(volume, range = 0f..1f)
 *     .thumbColor(Color.Blue)
 *     .trackColor(Color.LightGray)
 *     .steps(10)
 *     .invoke()
 * ```
 */
@Composable
fun MagicUIScope.sliderAdvanced(
    value: MutableState<Float>,
    range: ClosedFloatingPointRange<Float> = 0f..1f
): SliderComponent {
    return SliderComponent(value, range)
}

// Keep existing SliderComponent class for advanced users
// (Already exists in Slider.kt)
```

### Template Summary

| Component | Simple Template | Advanced Template | Lines (Simple) | Lines (Advanced) |
|-----------|----------------|-------------------|----------------|------------------|
| **text** | `text("Hello")` | N/A (simple only) | ~20 | N/A |
| **button** | `button("Click") { }` | `buttonAdvanced(...).backgroundColor(...)` | ~30 | ~130 |
| **input** | `input(value, label = "Email")` | N/A (simple sufficient) | ~25 | N/A |
| **slider** | `slider(volume)` | `sliderAdvanced(volume).thumbColor(...)` | ~25 | ~180 |
| **toggle** | `toggle(checked, "Dark Mode")` | N/A (simple sufficient) | ~35 | N/A |
| **checkbox** | `checkbox(agreed, "I agree")` | N/A (simple sufficient) | ~25 | N/A |
| **card** | `card { ... }` | N/A (simple sufficient) | ~15 | N/A |
| **list** | `list(items) { ... }` | N/A (simple sufficient) | ~10 | N/A |

**Result:**
- **Simple templates:** 10-35 lines each (90% use case)
- **Advanced templates:** 130-180 lines each (10% use case, already exist)
- **Approach:** Provide both, document simple as primary

---

## Code Generation Integration

### Integration with MagicElements

**Goal:** Generate MagicUI DSL code from visual design and export to MagicElements templates

**File:** `modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/codegen/CodeGenerator.kt`

```kotlin
/**
 * CodeGenerator - Generate MagicUI DSL code from visual design
 *
 * Integrates with MagicElements library for template creation
 */
object CodeGenerator {
    /**
     * Generate MagicUI DSL code from design components
     */
    fun generate(components: List<DesignComponent>): String {
        return buildString {
            appendLine("MagicScreen(\"${generateScreenName()}\") {")
            components.forEach { component ->
                appendLine("    ${generateComponentCode(component)}")
            }
            appendLine("}")
        }
    }

    /**
     * Generate code for a single component
     */
    private fun generateComponentCode(component: DesignComponent): String {
        return when (component.type) {
            ComponentType.TEXT -> {
                val text = component.properties["text"] as? String ?: "Text"
                val style = component.properties["style"] as? String ?: "BODY"
                """text("$text", style = TextStyle.$style)"""
            }
            ComponentType.BUTTON -> {
                val text = component.properties["text"] as? String ?: "Button"
                val color = component.properties["color"] as? String
                if (color != null) {
                    """button("$text", color = Color.$color) { /* TODO */ }"""
                } else {
                    """button("$text") { /* TODO */ }"""
                }
            }
            ComponentType.INPUT -> {
                val label = component.properties["label"] as? String ?: ""
                val placeholder = component.properties["placeholder"] as? String ?: ""
                """val value = state("")
    |input(value, label = "$label", placeholder = "$placeholder")""".trimMargin()
            }
            ComponentType.SLIDER -> {
                val range = component.properties["range"] as? String ?: "0..1"
                """val value = state(0.5f)
    |slider(value, range = ${range}f)""".trimMargin()
            }
            ComponentType.TOGGLE -> {
                val label = component.properties["label"] as? String ?: "Toggle"
                """val checked = state(false)
    |toggle(checked, label = "$label")""".trimMargin()
            }
            ComponentType.CHECKBOX -> {
                val label = component.properties["label"] as? String ?: "Checkbox"
                """val checked = state(false)
    |checkbox(checked, label = "$label")""".trimMargin()
            }
            ComponentType.CARD -> {
                """card {
    |    // TODO: Add card content
    |}""".trimMargin()
            }
            ComponentType.IMAGE -> {
                val source = component.properties["source"] as? String ?: "placeholder.png"
                """image("$source")"""
            }
        }
    }

    /**
     * Generate unique screen name
     */
    private fun generateScreenName(): String {
        return "generated_${System.currentTimeMillis()}"
    }

    /**
     * Export to MagicElements template
     */
    fun exportToMagicElements(code: String, templateName: String) {
        // TODO: Integration with MagicElements library
        // MagicElements.createTemplate(templateName, code)
    }
}
```

---

## Implementation Plan

### Phase 1: Code Editor (Week 1)
**Priority: HIGH** - Foundation for other features

1. **Day 1-2: Core Editor**
   - Create `MagicCodeEditor.kt` with basic text editing
   - Implement `MagicCodeEditorState` with state management
   - Add line numbers and status bar

2. **Day 3-4: Undo/Redo**
   - Implement Command Pattern (EditorCommand interface)
   - Create command classes (InsertText, DeleteText, ReplaceText)
   - Implement undo/redo stacks
   - Add toolbar with undo/redo buttons

3. **Day 5: Clear/Reset**
   - Implement clear functionality (ReplaceAllCommand)
   - Implement reset functionality
   - Add toolbar buttons
   - Test undo/redo with clear/reset

4. **Day 6-7: Polish**
   - Add copy/export functionality
   - Improve visual design (colors, spacing)
   - Write unit tests
   - Create documentation

**Deliverables:**
- ✅ Fully functional MagicCodeEditor component
- ✅ Command Pattern implementation
- ✅ Undo/Redo/Clear/Reset working
- ✅ Unit tests
- ✅ Documentation

---

### Phase 2: Simple Templates (Week 2)
**Priority: HIGH** - Quick wins for users

1. **Day 1-2: Basic Components**
   - Create `SimpleTemplates.kt`
   - Implement: text, button, input
   - Test with MagicUIScope integration

2. **Day 3-4: Form Components**
   - Implement: slider, toggle, checkbox
   - Test state management

3. **Day 5: Layout Components**
   - Implement: card, list
   - Test composition

4. **Day 6-7: Documentation & Examples**
   - Create usage examples
   - Write documentation
   - Create sample apps

**Deliverables:**
- ✅ 8+ simple templates (~10-30 lines each)
- ✅ Full VOS4 integration (UUID, Commands)
- ✅ Documentation with examples
- ✅ Sample apps

---

### Phase 3: Visual Designer (Week 3-4)
**Priority: MEDIUM** - Advanced feature

1. **Week 3 Day 1-3: Designer State**
   - Create `MagicDesignerState.kt`
   - Implement component management
   - Implement property editing

2. **Week 3 Day 4-7: UI Components**
   - Component Palette
   - Design Canvas (drag & drop)
   - Properties Inspector

3. **Week 4 Day 1-3: Code Generation**
   - Implement CodeGenerator
   - Integrate with MagicCodeEditor
   - Live code updates

4. **Week 4 Day 4-7: Live Preview**
   - Implement preview rendering
   - Test full workflow
   - Polish and bug fixes

**Deliverables:**
- ✅ Visual designer UI
- ✅ Drag-and-drop functionality
- ✅ Live code generation
- ✅ Live preview

---

### Phase 4: Integration (Week 5)
**Priority: MEDIUM** - Ecosystem integration

1. **Day 1-2: MagicElements Integration**
   - Export to templates
   - Template library integration

2. **Day 3-4: Advanced Templates**
   - Document existing advanced templates
   - Create `AdvancedTemplates.kt` wrapper

3. **Day 5-7: Testing & Documentation**
   - End-to-end testing
   - User documentation
   - Video tutorials (optional)

**Deliverables:**
- ✅ MagicElements integration
- ✅ Complete documentation
- ✅ Test suite

---

## Success Criteria

### Code Editor
- [  ] Undo/Redo works correctly (Command Pattern)
- [  ] Clear button empties editor (undoable)
- [  ] Reset button returns to initial code (undoable)
- [  ] Toolbar buttons disabled appropriately
- [  ] Line numbers display correctly
- [  ] Status bar shows modified state
- [  ] Memory efficient (stack-based history)

### Simple Templates
- [  ] All templates are 10-30 lines each
- [  ] VOS4 integration automatic (UUID, Commands)
- [  ] Voice commands auto-registered
- [  ] Localization auto-applied
- [  ] Works within MagicScreen
- [  ] Documentation complete

### Visual Designer
- [  ] Component palette displays all components
- [  ] Drag-and-drop adds components to canvas
- [  ] Properties inspector edits component properties
- [  ] Code generation updates live
- [  ] Live preview renders correctly
- [  ] Export to MagicElements works

---

## File Structure

```
modules/libraries/MagicUI/src/main/java/com/augmentalis/magicui/
├── editor/
│   ├── MagicCodeEditor.kt              # Main editor component
│   ├── MagicCodeEditorState.kt         # State management
│   ├── EditorCommand.kt                # Command Pattern interface
│   ├── EditorCommands.kt               # Command implementations
│   └── EditorComponents.kt             # UI components (toolbar, status bar)
│
├── designer/
│   ├── MagicVisualDesigner.kt          # Main designer component
│   ├── MagicDesignerState.kt           # State management
│   ├── ComponentPalette.kt             # Component selection UI
│   ├── DesignCanvas.kt                 # Drag-and-drop canvas
│   ├── PropertiesInspector.kt          # Property editing UI
│   ├── LivePreview.kt                  # Preview rendering
│   └── DesignComponent.kt              # Component data model
│
├── templates/
│   ├── SimpleTemplates.kt              # Simple quick templates (NEW)
│   ├── AdvancedTemplates.kt            # Advanced builder templates (NEW)
│   └── MagicTemplateEngine.kt          # Template engine (EXISTS)
│
├── codegen/
│   ├── CodeGenerator.kt                # Code generation from visual design
│   └── MagicElementsIntegration.kt     # MagicElements export
│
└── components/                         # Existing components
    ├── Button.kt                       # Keep for advanced users
    ├── Slider.kt                       # Keep for advanced users
    └── ...
```

---

## Conclusion

This specification provides a complete blueprint for implementing a Unity-inspired MagicUI Editor system with:

1. **Reusable Code Editor** - Command Pattern for undo/redo, clear/reset functionality
2. **Hybrid Templates** - Simple quick templates (primary) + advanced builders (optional)
3. **Visual Designer** - Drag-and-drop UI creation with live code generation
4. **MagicElements Integration** - Template export and ecosystem integration

**Key Design Decisions:**
- Command Pattern for undo/redo (industry standard, memory efficient)
- Hybrid template approach (simple primary, advanced optional)
- Unity-inspired workflow (familiar to developers)
- Full VOS4 integration (UUID, Commands, Localization)

**Implementation Timeline:**
- Week 1: Code Editor ✅
- Week 2: Simple Templates ✅
- Week 3-4: Visual Designer
- Week 5: Integration & Polish

---

**End of MagicUI Editor Specification**

Author: Manoj Jhawar
Date: 2025-10-19 12:11:00 PDT
Status: Design Complete - Ready for Implementation
Next Step: Begin Phase 1 (Code Editor implementation)

# MagicIdea IDE Plugin Comprehensive Specification

**Date**: 2025-11-09 13:46:48 PST
**Version**: 1.0.0
**Author**: Manoj Jhawar, manoj@ideahq.net
**Status**: Design Phase - Ready for Implementation

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Findings](#research-findings)
3. [Android Studio Plugin Specification](#android-studio-plugin-specification)
4. [VSCode Extension Specification](#vscode-extension-specification)
5. [Feature Comparison Matrix](#feature-comparison-matrix)
6. [Implementation Roadmap](#implementation-roadmap)
7. [Code Examples](#code-examples)
8. [Developer Experience Goals](#developer-experience-goals)
9. [Technical Architecture](#technical-architecture)
10. [Testing & Quality Assurance](#testing--quality-assurance)

---

## Executive Summary

MagicIdea is a revolutionary cross-platform UI framework with a powerful DSL that enables developers to build apps for 7+ platforms from a single codebase. To make MagicIdea development truly exceptional, we need world-class IDE tooling that rivals or exceeds Flutter, React Native, and Jetpack Compose plugins.

### Project Scope

**Two Primary Deliverables:**
1. **Android Studio / IntelliJ IDEA Plugin** - Full-featured IDE integration with live preview, visual designers, and advanced code generation
2. **Visual Studio Code Extension** - Lightweight but powerful extension with LSP-based features and webview previews

**Key Differentiators:**
- Live preview of MagicIdea DSL (similar to Jetpack Compose @Preview)
- Component palette with drag-and-drop support
- Multi-platform code generation (Kotlin, Swift, React) with one click
- Theme designer with live preview across all 7 supported platforms
- AI-assisted component creation and layout suggestions

### Supported Platforms

MagicIdea generates code for:
- **iOS 26** (Liquid Glass design)
- **macOS 26** (Liquid Glass design)
- **visionOS 2** (Spatial Glass design)
- **Windows 11** (Fluent 2 design)
- **Android** (Material 3 Expressive)
- **Android XR** (Spatial Material)
- **Samsung** (One UI 7)

### Component Library

**48 Components** across 5 categories:
- **Foundation** (8): Button, TextField, Text, Image, Container, Row, Column, Card
- **Form** (10): Checkbox, Switch, Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating
- **Feedback** (8): Dialog, Toast, Alert, ProgressBar, Spinner, Badge, Tooltip, Snackbar
- **Navigation** (6): AppBar, BottomNav, Tabs, Drawer, Breadcrumb, Pagination
- **Data** (16): List, DataGrid, Table, TreeView, Timeline, Stepper, Accordion, Carousel, Avatar, Chip, Divider, Paper, EmptyState, Skeleton, Icon, ScrollView

---

## Research Findings

### What Makes Great IDE Plugins

Based on research into Flutter, React Native, Jetpack Compose, and Tailwind CSS IntelliSense plugins:

#### Developer Pain Points (To Solve)
1. **Context Switching** - Constantly switching between code editor, emulator, and documentation
2. **Manual Boilerplate** - Writing repetitive component structure and imports
3. **Property Discovery** - Remembering all available properties and their types
4. **Visual Feedback Delay** - Waiting for builds/hot-reload to see UI changes
5. **Theme Inconsistency** - Ensuring consistent styling across components
6. **Multi-Platform Complexity** - Managing platform-specific code differences

#### Developer Delights (To Provide)
1. **Instant Visual Feedback** - See changes immediately without running the app
2. **Smart Autocomplete** - Context-aware suggestions that understand component hierarchy
3. **Visual Designers** - Drag-and-drop for rapid prototyping
4. **Quick Fixes** - One-click solutions to common problems
5. **Code Generation** - Generate boilerplate and convert between formats
6. **Integrated Documentation** - Hover tooltips with examples and API docs

### Competitive Analysis

| Feature | Flutter Plugin | React Native Tools | Jetpack Compose | Tailwind IntelliSense | MagicIdea (Target) |
|---------|----------------|-------------------|-----------------|----------------------|-------------------|
| Syntax Highlighting | ✅ | ✅ | ✅ | ✅ | ✅ |
| Code Completion | ✅ | ✅ | ✅ | ✅ | ✅ Advanced |
| Live Preview | ❌ (Emulator only) | ❌ (Webview) | ✅ @Preview | ❌ | ✅ Multi-platform |
| Component Palette | ❌ | ❌ | ❌ | ❌ | ✅ Drag & Drop |
| Theme Designer | ❌ | ❌ | ❌ | ✅ (Limited) | ✅ Full-featured |
| Code Generation | ✅ (Templates) | ✅ (Snippets) | ❌ | ❌ | ✅ Multi-target |
| Visual Designer | ❌ | ❌ | ❌ | ❌ | ✅ |
| AI Assistance | ❌ | ❌ | ❌ | ❌ | ✅ |
| Multi-Platform | ❌ | ❌ | ❌ | ❌ | ✅ 7 platforms |

**Key Insight**: No existing UI framework plugin offers all these features. MagicIdea has the opportunity to become the most developer-friendly UI framework tooling available.

---

## Android Studio Plugin Specification

### Overview

**Plugin Name**: MagicIdea Studio
**Target IDEs**: Android Studio 2024.1+, IntelliJ IDEA 2024.1+
**Language**: Kotlin + Java Swing (UI components)
**Distribution**: JetBrains Marketplace

### Core Features

#### 1. MagicIdea DSL Language Support

**Syntax Highlighting**
- Custom language injection for AvaUI DSL blocks
- Semantic highlighting for:
  - Component names (purple)
  - Properties (blue)
  - Values (green for strings, orange for numbers)
  - Callbacks (yellow)
  - Comments (gray)
  - Theme names (cyan)

**Implementation Approach:**
```kotlin
// Register custom language
class MagicIdeaLanguage private constructor() : Language("MagicIdea") {
    companion object {
        val INSTANCE = MagicIdeaLanguage()
    }
}

// Syntax highlighter
class MagicIdeaSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer() = MagicIdeaLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            MagicIdeaTokenTypes.COMPONENT_NAME -> arrayOf(COMPONENT_KEY)
            MagicIdeaTokenTypes.PROPERTY -> arrayOf(PROPERTY_KEY)
            MagicIdeaTokenTypes.STRING_VALUE -> arrayOf(STRING_KEY)
            // ... more mappings
            else -> emptyArray()
        }
    }
}
```

#### 2. Intelligent Code Completion

**Context-Aware Autocomplete**
- Component name completion (48 components)
- Property name completion based on component type
- Property value completion (enums, colors, predefined values)
- Callback signature completion
- Theme name completion (7 platform themes)
- Icon name completion (Material Icons, SF Symbols, Fluent Icons)

**Smart Suggestions:**
- Suggest required properties first
- Show deprecation warnings
- Provide property descriptions in completion popup
- Show example values
- Filter suggestions based on parent component context

**Implementation:**
```kotlin
class MagicIdeaCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MagicIdeaCompletionProvider()
        )
    }
}

class MagicIdeaCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val parent = position.parent

        when {
            parent is MagicIdeaComponentName -> {
                // Suggest component names
                ComponentRegistry.getAll().forEach { component ->
                    result.addElement(
                        LookupElementBuilder
                            .create(component.name)
                            .withIcon(component.icon)
                            .withTypeText(component.category)
                            .withTailText(" (${component.propertyCount} props)")
                    )
                }
            }
            parent is MagicIdeaPropertyName -> {
                // Suggest properties for current component
                val componentType = parent.getComponentType()
                ComponentRegistry.get(componentType)?.properties?.forEach { prop ->
                    result.addElement(
                        LookupElementBuilder
                            .create(prop.name)
                            .withTypeText(prop.type.name)
                            .withTailText(if (prop.required) " (required)" else " (optional)")
                            .withIcon(AllIcons.Nodes.Property)
                    )
                }
            }
            // ... more contexts
        }
    }
}
```

#### 3. Live Preview Panel

**Architecture:**
- Custom tool window docked to right side
- Real-time rendering as user types (debounced)
- Multiple preview modes (per platform)
- Interactive preview (click components to navigate to code)
- Screenshot/export functionality

**Preview Modes:**
1. **Single Platform Preview** - Show one platform at a time
2. **Side-by-Side Preview** - Compare 2 platforms
3. **Grid Preview** - Show all 7 platforms in grid
4. **Device Preview** - Show in device frame (iPhone, Pixel, Surface, etc.)

**Implementation Strategy:**
```kotlin
class MagicIdeaPreviewToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val previewPanel = MagicIdeaPreviewPanel(project)
        val content = contentFactory.createContent(previewPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class MagicIdeaPreviewPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val renderer = MagicIdeaRenderer()
    private val previewCanvas = JPanel()
    private val toolbar = createToolbar()

    init {
        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(previewCanvas), BorderLayout.CENTER)

        // Listen to editor changes
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : BulkAwareDocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (isMagicIdeaFile(event.document)) {
                    schedulePreviewUpdate()
                }
            }
        }, project)
    }

    private fun schedulePreviewUpdate() {
        alarm.cancelAllRequests()
        alarm.addRequest({ updatePreview() }, 300) // 300ms debounce
    }

    private fun updatePreview() {
        ApplicationManager.getApplication().runReadAction {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@runReadAction
            val code = editor.document.text

            try {
                val ast = MagicIdeaParser.parse(code)
                val platformPreview = renderer.render(ast, currentPlatform)

                SwingUtilities.invokeLater {
                    previewCanvas.removeAll()
                    previewCanvas.add(platformPreview)
                    previewCanvas.revalidate()
                    previewCanvas.repaint()
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }
}
```

**Rendering Approach:**
- Parse MagicIdea DSL → AST
- Convert AST → Swing/JavaFX components for preview
- Apply platform-specific styling (CSS-like approach)
- Use embedded WebView for complex platform previews (iOS, Android, etc.)

#### 4. Component Palette & Visual Designer

**Component Palette:**
- Searchable list of all 48 components
- Organized by category (Foundation, Form, Feedback, Navigation, Data)
- Drag components into code editor OR preview panel
- Show component preview on hover
- Quick insert snippets

**Visual Designer:**
- Drag-and-drop component positioning
- Visual property editor (right-click → Properties)
- Alignment guides and snap-to-grid
- Component tree navigator
- Undo/redo support
- Two-way sync: Visual Designer ↔ Code Editor

**Implementation:**
```kotlin
class MagicIdeaComponentPalette : JBList<ComponentDescriptor>() {
    init {
        model = ComponentListModel(ComponentRegistry.getAll())
        cellRenderer = ComponentCellRenderer()

        // Enable drag-and-drop
        dragEnabled = true
        transferHandler = ComponentTransferHandler()
    }

    private class ComponentCellRenderer : ListCellRenderer<ComponentDescriptor> {
        override fun getListCellRendererComponent(...): Component {
            return JPanel().apply {
                layout = BorderLayout()
                add(JBLabel(value.name, value.icon, SwingConstants.LEFT), BorderLayout.WEST)
                add(JBLabel(value.category).apply {
                    foreground = JBColor.GRAY
                }, BorderLayout.EAST)
            }
        }
    }
}

class ComponentTransferHandler : TransferHandler() {
    override fun createTransferable(c: JComponent): Transferable? {
        val list = c as MagicIdeaComponentPalette
        val component = list.selectedValue ?: return null
        return ComponentTransferable(component)
    }

    override fun getSourceActions(c: JComponent) = COPY
}

// Drop handler in editor
class MagicIdeaEditorDropTarget(private val editor: Editor) : DropTargetAdapter() {
    override fun drop(event: DropTargetDropEvent) {
        val transferable = event.transferable
        if (transferable.isDataFlavorSupported(ComponentTransferable.FLAVOR)) {
            val component = transferable.getTransferData(ComponentTransferable.FLAVOR) as ComponentDescriptor
            val snippet = component.generateSnippet()

            // Insert at cursor or drop location
            insertSnippet(editor, snippet, event.location)
        }
    }
}
```

#### 5. Property Inspector

**Features:**
- Context-sensitive property panel
- Visual editors for each property type:
  - Color picker for color properties
  - Slider for numeric ranges
  - Dropdown for enums
  - Toggle for booleans
  - Icon picker for icon properties
  - Font picker for font properties
- Real-time preview of property changes
- Property validation with error highlighting
- Copy/paste property values between components

**Implementation:**
```kotlin
class MagicIdeaPropertyInspector : JPanel(BorderLayout()) {
    private val propertyTable = PropertyTable()
    private val currentComponent: ComponentDescriptor? = null

    fun showProperties(component: ComponentDescriptor) {
        propertyTable.clear()

        component.properties.forEach { prop ->
            val editor = createPropertyEditor(prop)
            propertyTable.addProperty(prop.name, editor)
        }
    }

    private fun createPropertyEditor(prop: PropertyDescriptor): PropertyEditor {
        return when (prop.type) {
            PropertyType.COLOR -> ColorPropertyEditor(prop)
            PropertyType.NUMBER -> NumberPropertyEditor(prop)
            PropertyType.ENUM -> EnumPropertyEditor(prop)
            PropertyType.BOOLEAN -> BooleanPropertyEditor(prop)
            PropertyType.STRING -> StringPropertyEditor(prop)
            PropertyType.ICON -> IconPropertyEditor(prop)
            PropertyType.FONT -> FontPropertyEditor(prop)
            else -> DefaultPropertyEditor(prop)
        }
    }
}

class ColorPropertyEditor(private val prop: PropertyDescriptor) : PropertyEditor {
    private val colorButton = JButton()
    private var currentColor: Color = Color.WHITE

    init {
        colorButton.addActionListener {
            val color = JColorChooser.showDialog(null, "Choose ${prop.name}", currentColor)
            if (color != null) {
                currentColor = color
                updatePreview()
                notifyValueChanged(color.toHexString())
            }
        }
    }

    override fun getComponent(): JComponent = colorButton
    override fun getValue(): Any = currentColor.toHexString()
}
```

#### 6. Theme Designer & Manager

**Features:**
- Create custom themes for all 7 platforms
- Visual theme editor:
  - Color palette picker (primary, secondary, accent, etc.)
  - Typography settings (font family, sizes, weights)
  - Spacing/padding presets
  - Border radius presets
  - Shadow/elevation presets
- Live preview of theme across components
- Export theme as JSON/YAML
- Import existing themes (Material, Cupertino, Fluent)
- Platform-specific theme variants

**Implementation:**
```kotlin
class MagicIdeaThemeDesigner : DialogWrapper(true) {
    private val colorSchemePanel = ColorSchemePanel()
    private val typographyPanel = TypographyPanel()
    private val spacingPanel = SpacingPanel()
    private val previewPanel = ThemePreviewPanel()

    init {
        title = "MagicIdea Theme Designer"
        init()

        // Update preview when any property changes
        colorSchemePanel.addChangeListener { updatePreview() }
        typographyPanel.addChangeListener { updatePreview() }
        spacingPanel.addChangeListener { updatePreview() }
    }

    private fun updatePreview() {
        val theme = buildTheme()
        previewPanel.applyTheme(theme)
    }

    private fun buildTheme(): ThemeDefinition {
        return ThemeDefinition(
            name = nameField.text,
            colorScheme = colorSchemePanel.buildColorScheme(),
            typography = typographyPanel.buildTypography(),
            spacing = spacingPanel.buildSpacing(),
            borderRadius = borderRadiusPanel.buildBorderRadius(),
            shadows = shadowPanel.buildShadows()
        )
    }

    override fun doOKAction() {
        val theme = buildTheme()
        ThemeManager.saveTheme(theme)
        super.doOKAction()
    }
}

class ColorSchemePanel : JPanel(GridBagLayout()) {
    private val primaryColor = ColorPickerField("Primary")
    private val secondaryColor = ColorPickerField("Secondary")
    private val accentColor = ColorPickerField("Accent")
    private val backgroundColor = ColorPickerField("Background")
    private val surfaceColor = ColorPickerField("Surface")
    private val errorColor = ColorPickerField("Error")

    fun buildColorScheme(): ColorScheme {
        return ColorScheme(
            primary = primaryColor.color,
            secondary = secondaryColor.color,
            accent = accentColor.color,
            background = backgroundColor.color,
            surface = surfaceColor.color,
            error = errorColor.color
        )
    }
}
```

#### 7. Code Generator Integration

**Features:**
- Generate Kotlin (Jetpack Compose) code from MagicIdea DSL
- Generate Swift (SwiftUI) code from MagicIdea DSL
- Generate React (TypeScript) code from MagicIdea DSL
- One-click generation with preview
- Configurable output settings (formatting, imports, etc.)
- Batch generation for entire project

**Implementation:**
```kotlin
class MagicIdeaCodeGeneratorAction : AnAction("Generate Code") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return

        val dialog = CodeGeneratorDialog(project, editor.document.text)
        if (dialog.showAndGet()) {
            generateCode(dialog.selectedPlatforms, dialog.outputDirectory)
        }
    }

    private fun generateCode(platforms: List<Platform>, outputDir: VirtualFile) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating Code", true) {
            override fun run(indicator: ProgressIndicator) {
                platforms.forEachIndexed { index, platform ->
                    indicator.fraction = index.toDouble() / platforms.size
                    indicator.text = "Generating ${platform.name} code..."

                    val generator = GeneratorFactory.create(platform)
                    val code = generator.generate(dslSource)

                    val outputFile = outputDir.createChildData(this, "${platform.fileName}.${platform.extension}")
                    VfsUtil.saveText(outputFile, code)
                }

                Notifications.Bus.notify(
                    Notification(
                        "MagicIdea",
                        "Code Generation Complete",
                        "Generated code for ${platforms.size} platforms",
                        NotificationType.INFORMATION
                    )
                )
            }
        })
    }
}

class CodeGeneratorDialog(project: Project, private val dslSource: String) : DialogWrapper(project) {
    val selectedPlatforms = mutableListOf<Platform>()
    var outputDirectory: VirtualFile? = null

    private val kotlinCheckbox = JBCheckBox("Kotlin (Jetpack Compose)", true)
    private val swiftCheckbox = JBCheckBox("Swift (SwiftUI)", false)
    private val reactCheckbox = JBCheckBox("React (TypeScript)", false)
    private val outputDirField = TextFieldWithBrowseButton()

    init {
        title = "Generate Multi-Platform Code"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Target Platforms:") {
                cell(kotlinCheckbox)
                cell(swiftCheckbox)
                cell(reactCheckbox)
            }
            row("Output Directory:") {
                cell(outputDirField).align(AlignX.FILL)
            }
        }
    }

    override fun doOKAction() {
        if (kotlinCheckbox.isSelected) selectedPlatforms.add(Platform.KOTLIN_COMPOSE)
        if (swiftCheckbox.isSelected) selectedPlatforms.add(Platform.SWIFTUI)
        if (reactCheckbox.isSelected) selectedPlatforms.add(Platform.REACT)

        outputDirectory = VfsUtil.findFileByIoFile(File(outputDirField.text), true)
        super.doOKAction()
    }
}
```

#### 8. Quick Fixes & Refactorings

**Quick Fixes:**
- Add missing required properties
- Convert property types
- Add missing imports
- Fix incorrect enum values
- Add missing callback implementations
- Fix color format (hex, rgb, named)

**Refactorings:**
- Extract component to separate file
- Inline component
- Rename component (with references)
- Change component type (e.g., Button → IconButton)
- Convert to state-driven component
- Wrap with container (Row, Column, Card)

**Implementation:**
```kotlin
class MagicIdeaQuickFixProvider : IntentionAction {
    override fun getText() = "Add missing required properties"
    override fun getFamilyName() = "MagicIdea Quick Fixes"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        return element.parent is MagicIdeaComponent && hasMissingProperties(element.parent)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val component = file.findElementAt(editor.caretModel.offset)?.parent as MagicIdeaComponent
        val missingProps = getMissingRequiredProperties(component)

        val factory = MagicIdeaPsiFactory(project)
        missingProps.forEach { prop ->
            val defaultValue = prop.getDefaultValue()
            val propertyElement = factory.createProperty(prop.name, defaultValue)
            component.addProperty(propertyElement)
        }
    }
}
```

#### 9. Component Navigator

**Features:**
- Tree view of component hierarchy
- Click to navigate to component in code
- Drag to reorder components
- Right-click context menu (delete, duplicate, wrap, extract)
- Search/filter components
- Show/hide components in preview

**Implementation:**
```kotlin
class MagicIdeaComponentNavigator : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val tree = Tree()
    private val searchField = SearchTextField()

    init {
        add(searchField, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)

        tree.cellRenderer = ComponentTreeCellRenderer()
        tree.addTreeSelectionListener { e ->
            val node = e.path.lastPathComponent as? ComponentNode
            node?.let { navigateToComponent(it.component) }
        }

        // Add drag-and-drop support
        tree.dragEnabled = true
        tree.transferHandler = ComponentTreeTransferHandler()
    }

    fun updateTree(ast: MagicIdeaAst) {
        val root = DefaultMutableTreeNode("Root")
        buildTree(ast.components, root)
        tree.model = DefaultTreeModel(root)
        tree.expandAll()
    }

    private fun buildTree(components: List<Component>, parent: DefaultMutableTreeNode) {
        components.forEach { component ->
            val node = ComponentNode(component)
            parent.add(node)
            if (component.children.isNotEmpty()) {
                buildTree(component.children, node)
            }
        }
    }
}
```

#### 10. Documentation Integration

**Features:**
- Hover tooltips with component/property documentation
- Inline documentation viewer (Quick Documentation: Ctrl+Q / Cmd+J)
- Code examples in documentation
- Link to online documentation
- Search documentation

**Implementation:**
```kotlin
class MagicIdeaDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val component = element?.parent as? MagicIdeaComponent ?: return null
        val descriptor = ComponentRegistry.get(component.name) ?: return null

        return buildString {
            append("<html><body>")
            append("<h3>${descriptor.name}</h3>")
            append("<p>${descriptor.description}</p>")

            append("<h4>Properties:</h4>")
            append("<table>")
            descriptor.properties.forEach { prop ->
                append("<tr>")
                append("<td><b>${prop.name}</b></td>")
                append("<td>${prop.type.name}</td>")
                append("<td>${if (prop.required) "Required" else "Optional"}</td>")
                append("</tr>")
            }
            append("</table>")

            append("<h4>Example:</h4>")
            append("<pre>${descriptor.exampleCode}</pre>")

            append("</body></html>")
        }
    }

    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): List<String>? {
        val component = element?.parent as? MagicIdeaComponent ?: return null
        return listOf("https://docs.magicidea.dev/components/${component.name}")
    }
}
```

### Android Studio Plugin Architecture

```
plugin.xml
├── extensions
│   ├── language (MagicIdeaLanguage)
│   ├── fileType (MagicIdeaFileType)
│   ├── syntaxHighlighter (MagicIdeaSyntaxHighlighter)
│   ├── completion.contributor (MagicIdeaCompletionContributor)
│   ├── toolWindowFactory (MagicIdeaPreviewToolWindow)
│   ├── intentionAction (MagicIdeaQuickFixProvider)
│   ├── documentationProvider (MagicIdeaDocumentationProvider)
│   └── refactoring.helper (MagicIdeaRefactoringHelper)
│
├── actions
│   ├── MagicIdeaNewFileAction
│   ├── MagicIdeaCodeGeneratorAction
│   ├── MagicIdeaThemeDesignerAction
│   └── MagicIdeaFormatAction
│
├── services
│   ├── ComponentRegistry (project-level)
│   ├── ThemeManager (application-level)
│   └── CodeGeneratorService (project-level)
│
└── listeners
    ├── EditorChangeListener
    ├── FileOpenListener
    └── ProjectOpenListener
```

### Dependencies

```gradle
dependencies {
    // IntelliJ Platform
    implementation "com.jetbrains.intellij.platform:platform-api:2024.1"

    // UI Components
    implementation "com.intellij:forms:2024.1"

    // MagicIdea Core
    implementation project(":magicidea-core")
    implementation project(":magicidea-parser")
    implementation project(":magicidea-codegen")
}
```

---

## VSCode Extension Specification

### Overview

**Extension Name**: MagicIdea
**Extension ID**: augmentalis.magicidea
**Target Version**: VSCode 1.90+
**Language**: TypeScript + Node.js
**Distribution**: Visual Studio Marketplace

### Core Features

#### 1. Syntax Highlighting (TextMate Grammar)

**Implementation:**
```json
{
  "name": "MagicIdea",
  "scopeName": "source.magicidea",
  "patterns": [
    {
      "include": "#components"
    },
    {
      "include": "#properties"
    },
    {
      "include": "#values"
    },
    {
      "include": "#comments"
    }
  ],
  "repository": {
    "components": {
      "patterns": [
        {
          "match": "\\b(Button|TextField|Text|Image|Container|Column|Row|Card)\\b",
          "name": "entity.name.type.component.magicidea"
        },
        {
          "match": "\\b(Checkbox|Switch|Radio|Slider|Dropdown)\\b",
          "name": "entity.name.type.form.magicidea"
        },
        {
          "match": "\\b(Dialog|Toast|Alert|ProgressBar|Spinner)\\b",
          "name": "entity.name.type.feedback.magicidea"
        }
      ]
    },
    "properties": {
      "patterns": [
        {
          "match": "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*:",
          "captures": {
            "1": {
              "name": "variable.other.property.magicidea"
            }
          }
        }
      ]
    },
    "values": {
      "patterns": [
        {
          "match": "\"([^\"]*)\"",
          "name": "string.quoted.double.magicidea"
        },
        {
          "match": "\\b\\d+(\\.\\d+)?\\b",
          "name": "constant.numeric.magicidea"
        },
        {
          "match": "#[0-9a-fA-F]{6}",
          "name": "constant.other.color.magicidea"
        },
        {
          "match": "\\b(true|false)\\b",
          "name": "constant.language.boolean.magicidea"
        }
      ]
    },
    "comments": {
      "patterns": [
        {
          "match": "//.*$",
          "name": "comment.line.double-slash.magicidea"
        },
        {
          "begin": "/\\*",
          "end": "\\*/",
          "name": "comment.block.magicidea"
        }
      ]
    }
  }
}
```

#### 2. Language Server Protocol (LSP)

**Server Implementation:**
```typescript
// server/src/server.ts
import {
  createConnection,
  TextDocuments,
  ProposedFeatures,
  InitializeParams,
  CompletionItem,
  CompletionItemKind,
  TextDocumentPositionParams,
  TextDocumentSyncKind,
  InitializeResult
} from 'vscode-languageserver/node';

import { TextDocument } from 'vscode-languageserver-textdocument';

// Create connection
const connection = createConnection(ProposedFeatures.all);
const documents: TextDocuments<TextDocument> = new TextDocuments(TextDocument);

// Initialize
connection.onInitialize((params: InitializeParams) => {
  const result: InitializeResult = {
    capabilities: {
      textDocumentSync: TextDocumentSyncKind.Incremental,
      completionProvider: {
        resolveProvider: true,
        triggerCharacters: ['.', ':']
      },
      hoverProvider: true,
      definitionProvider: true,
      documentFormattingProvider: true,
      colorProvider: true
    }
  };
  return result;
});

// Completion
connection.onCompletion(
  (params: TextDocumentPositionParams): CompletionItem[] => {
    const document = documents.get(params.textDocument.uri);
    if (!document) return [];

    const position = params.position;
    const text = document.getText();
    const offset = document.offsetAt(position);

    // Parse context
    const context = getCompletionContext(text, offset);

    if (context.type === 'component') {
      return getComponentCompletions();
    } else if (context.type === 'property') {
      return getPropertyCompletions(context.componentType);
    } else if (context.type === 'value') {
      return getValueCompletions(context.propertyType);
    }

    return [];
  }
);

// Component completions
function getComponentCompletions(): CompletionItem[] {
  return ComponentRegistry.getAll().map(component => ({
    label: component.name,
    kind: CompletionItemKind.Class,
    detail: component.category,
    documentation: component.description,
    insertText: `${component.name} {\n\t$0\n}`,
    insertTextFormat: 2 // Snippet
  }));
}

// Property completions
function getPropertyCompletions(componentType: string): CompletionItem[] {
  const component = ComponentRegistry.get(componentType);
  if (!component) return [];

  return component.properties.map(prop => ({
    label: prop.name,
    kind: CompletionItemKind.Property,
    detail: `${prop.type.name}${prop.required ? ' (required)' : ''}`,
    documentation: prop.description,
    insertText: `${prop.name}: $0`,
    insertTextFormat: 2 // Snippet
  }));
}

// Hover information
connection.onHover((params) => {
  const document = documents.get(params.textDocument.uri);
  if (!document) return null;

  const position = params.position;
  const word = getWordAtPosition(document, position);

  // Check if it's a component
  const component = ComponentRegistry.get(word);
  if (component) {
    return {
      contents: {
        kind: 'markdown',
        value: [
          `**${component.name}** (${component.category})`,
          '',
          component.description,
          '',
          '**Properties:**',
          ...component.properties.map(p => `- \`${p.name}\`: ${p.type.name}`)
        ].join('\n')
      }
    };
  }

  return null;
});

// Document colors
connection.onDocumentColor((params) => {
  const document = documents.get(params.textDocument.uri);
  if (!document) return [];

  const text = document.getText();
  const colorRegex = /#([0-9a-fA-F]{6})/g;
  const colors = [];

  let match;
  while ((match = colorRegex.exec(text)) !== null) {
    const start = document.positionAt(match.index);
    const end = document.positionAt(match.index + match[0].length);

    const hex = match[1];
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;

    colors.push({
      range: { start, end },
      color: { red: r, green: g, blue: b, alpha: 1 }
    });
  }

  return colors;
});

// Color presentation
connection.onColorPresentation((params) => {
  const color = params.color;
  const r = Math.round(color.red * 255);
  const g = Math.round(color.green * 255);
  const b = Math.round(color.blue * 255);
  const hex = `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;

  return [
    { label: hex.toUpperCase() }
  ];
});

// Start listening
documents.listen(connection);
connection.listen();
```

**Client Activation:**
```typescript
// client/src/extension.ts
import * as path from 'path';
import { workspace, ExtensionContext } from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind
} from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: ExtensionContext) {
  // Server options
  const serverModule = context.asAbsolutePath(
    path.join('server', 'out', 'server.js')
  );
  const debugOptions = { execArgv: ['--nolazy', '--inspect=6009'] };

  const serverOptions: ServerOptions = {
    run: { module: serverModule, transport: TransportKind.ipc },
    debug: {
      module: serverModule,
      transport: TransportKind.ipc,
      options: debugOptions
    }
  };

  // Client options
  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: 'file', language: 'magicidea' }],
    synchronize: {
      fileEvents: workspace.createFileSystemWatcher('**/.magicideaconfig')
    }
  };

  // Create language client
  client = new LanguageClient(
    'magicidea',
    'MagicIdea Language Server',
    serverOptions,
    clientOptions
  );

  // Start client
  client.start();
}

export function deactivate(): Thenable<void> | undefined {
  if (!client) {
    return undefined;
  }
  return client.stop();
}
```

#### 3. Preview Panel (Webview)

**Implementation:**
```typescript
// client/src/preview.ts
import * as vscode from 'vscode';

export class MagicIdeaPreviewProvider implements vscode.WebviewViewProvider {
  public static readonly viewType = 'magicidea.preview';

  private _view?: vscode.WebviewView;

  constructor(private readonly _extensionUri: vscode.Uri) {}

  public resolveWebviewView(
    webviewView: vscode.WebviewView,
    context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ) {
    this._view = webviewView;

    webviewView.webview.options = {
      enableScripts: true,
      localResourceRoots: [this._extensionUri]
    };

    webviewView.webview.html = this._getHtmlForWebview(webviewView.webview);

    // Listen to editor changes
    vscode.workspace.onDidChangeTextDocument(e => {
      if (e.document.languageId === 'magicidea') {
        this.updatePreview(e.document.getText());
      }
    });
  }

  private updatePreview(code: string) {
    if (this._view) {
      this._view.webview.postMessage({ type: 'update', code });
    }
  }

  private _getHtmlForWebview(webview: vscode.Webview) {
    const scriptUri = webview.asWebviewUri(
      vscode.Uri.joinPath(this._extensionUri, 'media', 'preview.js')
    );
    const styleUri = webview.asWebviewUri(
      vscode.Uri.joinPath(this._extensionUri, 'media', 'preview.css')
    );

    return `<!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="${styleUri}" rel="stylesheet">
        <title>MagicIdea Preview</title>
      </head>
      <body>
        <div id="toolbar">
          <select id="platform-select">
            <option value="ios">iOS 26</option>
            <option value="android">Android</option>
            <option value="windows">Windows 11</option>
            <option value="macos">macOS 26</option>
            <option value="visionos">visionOS 2</option>
            <option value="androidxr">Android XR</option>
            <option value="samsung">Samsung One UI 7</option>
          </select>
          <button id="refresh-btn">Refresh</button>
          <button id="export-btn">Export</button>
        </div>
        <div id="preview-container"></div>
        <script src="${scriptUri}"></script>
      </body>
      </html>`;
  }
}
```

**Preview JavaScript:**
```javascript
// media/preview.js
(function() {
  const vscode = acquireVsCodeApi();
  const previewContainer = document.getElementById('preview-container');
  const platformSelect = document.getElementById('platform-select');

  // Listen for messages from extension
  window.addEventListener('message', event => {
    const message = event.data;

    switch (message.type) {
      case 'update':
        renderPreview(message.code);
        break;
    }
  });

  // Render preview
  function renderPreview(code) {
    try {
      // Parse MagicIdea DSL
      const ast = parseMagicIdea(code);

      // Get selected platform
      const platform = platformSelect.value;

      // Render
      const html = renderToHTML(ast, platform);
      previewContainer.innerHTML = html;

      // Apply platform-specific styles
      applyPlatformStyles(platform);
    } catch (error) {
      previewContainer.innerHTML = `
        <div class="error">
          <h3>Preview Error</h3>
          <pre>${error.message}</pre>
        </div>
      `;
    }
  }

  // Parse MagicIdea DSL to AST
  function parseMagicIdea(code) {
    // Simple parser (in production, use proper parser from MagicIdea core)
    // This is a simplified example
    const lines = code.split('\n');
    const ast = { components: [] };

    // ... parsing logic ...

    return ast;
  }

  // Render AST to HTML
  function renderToHTML(ast, platform) {
    let html = '<div class="app-container">';

    ast.components.forEach(component => {
      html += renderComponent(component, platform);
    });

    html += '</div>';
    return html;
  }

  // Render individual component
  function renderComponent(component, platform) {
    const renderer = getRenderer(component.type, platform);
    return renderer(component);
  }

  // Platform-specific renderers
  function getRenderer(componentType, platform) {
    const renderers = {
      Button: {
        ios: (comp) => `
          <button class="ios-button ${comp.properties.style}">
            ${comp.properties.text}
          </button>
        `,
        android: (comp) => `
          <button class="material-button ${comp.properties.style}">
            ${comp.properties.text}
          </button>
        `,
        // ... more platforms
      },
      TextField: {
        ios: (comp) => `
          <input type="text" class="ios-textfield"
            placeholder="${comp.properties.placeholder || ''}" />
        `,
        android: (comp) => `
          <div class="material-textfield">
            <input type="text" placeholder="${comp.properties.placeholder || ''}" />
            <span class="floating-label">${comp.properties.label || ''}</span>
          </div>
        `,
        // ... more platforms
      },
      // ... more components
    };

    return renderers[componentType]?.[platform] ||
           ((comp) => `<div>Unknown: ${componentType}</div>`);
  }

  // Apply platform-specific CSS
  function applyPlatformStyles(platform) {
    document.body.className = `platform-${platform}`;
  }

  // Platform select change
  platformSelect.addEventListener('change', () => {
    vscode.postMessage({ type: 'platformChanged', platform: platformSelect.value });
    // Re-render current preview
    const currentCode = previewContainer.dataset.code;
    if (currentCode) {
      renderPreview(currentCode);
    }
  });
})();
```

#### 4. Snippet Library

**Implementation:**
```json
{
  "Button Component": {
    "prefix": "button",
    "body": [
      "Button {",
      "\ttext: \"${1:Click Me}\"",
      "\tstyle: ${2|Primary,Secondary,Tertiary|}",
      "\tonClick: { ${3:/* action */} }",
      "}"
    ],
    "description": "Insert a Button component"
  },
  "TextField Component": {
    "prefix": "textfield",
    "body": [
      "TextField {",
      "\tvalue: ${1:state.text}",
      "\tplaceholder: \"${2:Enter text}\"",
      "\tlabel: \"${3:Label}\"",
      "\tonValueChange: { ${4:newValue -> state.text = newValue} }",
      "}"
    ],
    "description": "Insert a TextField component"
  },
  "Column Layout": {
    "prefix": "column",
    "body": [
      "Column {",
      "\tarrangement: ${1|Start,Center,End,SpaceBetween,SpaceAround|}",
      "\thorizontalAlignment: ${2|Start,Center,End|}",
      "\tchildren: [",
      "\t\t${3:/* components */}",
      "\t]",
      "}"
    ],
    "description": "Insert a Column layout"
  },
  "Row Layout": {
    "prefix": "row",
    "body": [
      "Row {",
      "\tarrangement: ${1|Start,Center,End,SpaceBetween,SpaceAround|}",
      "\tverticalAlignment: ${2|Top,Center,Bottom|}",
      "\tchildren: [",
      "\t\t${3:/* components */}",
      "\t]",
      "}"
    ],
    "description": "Insert a Row layout"
  },
  "Complete App Template": {
    "prefix": "app",
    "body": [
      "App {",
      "\tid: \"${1:com.example.app}\"",
      "\tname: \"${2:My App}\"",
      "\ttheme: ${3|iOS26LiquidGlass,AndroidMaterial3,Windows11Fluent|}",
      "",
      "\tColumn {",
      "\t\tpadding: 16",
      "",
      "\t\tText {",
      "\t\t\ttext: \"${4:Welcome}\"",
      "\t\t\tfont: Title",
      "\t\t}",
      "",
      "\t\tButton {",
      "\t\t\ttext: \"${5:Get Started}\"",
      "\t\t\tstyle: Primary",
      "\t\t\tonClick: { ${6:/* action */} }",
      "\t\t}",
      "\t}",
      "}"
    ],
    "description": "Complete MagicIdea app template"
  }
}
```

#### 5. Component Tree View

**Implementation:**
```typescript
// client/src/treeView.ts
import * as vscode from 'vscode';

export class ComponentTreeProvider implements vscode.TreeDataProvider<ComponentTreeItem> {
  private _onDidChangeTreeData = new vscode.EventEmitter<ComponentTreeItem | undefined>();
  readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

  private components: Component[] = [];

  constructor() {
    // Listen to editor changes
    vscode.workspace.onDidChangeTextDocument(e => {
      if (e.document.languageId === 'magicidea') {
        this.refresh(e.document.getText());
      }
    });
  }

  refresh(code: string): void {
    try {
      const ast = parseMagicIdea(code);
      this.components = ast.components;
      this._onDidChangeTreeData.fire(undefined);
    } catch (error) {
      console.error('Failed to parse MagicIdea code:', error);
    }
  }

  getTreeItem(element: ComponentTreeItem): vscode.TreeItem {
    return element;
  }

  getChildren(element?: ComponentTreeItem): Thenable<ComponentTreeItem[]> {
    if (!element) {
      // Root level
      return Promise.resolve(
        this.components.map(comp => new ComponentTreeItem(comp, vscode.TreeItemCollapsibleState.Collapsed))
      );
    } else {
      // Child level
      return Promise.resolve(
        element.component.children.map(comp => new ComponentTreeItem(comp, vscode.TreeItemCollapsibleState.Collapsed))
      );
    }
  }
}

class ComponentTreeItem extends vscode.TreeItem {
  constructor(
    public readonly component: Component,
    public readonly collapsibleState: vscode.TreeItemCollapsibleState
  ) {
    super(component.name, collapsibleState);

    this.tooltip = `${component.type} ${component.id ? `(${component.id})` : ''}`;
    this.description = component.id || '';
    this.iconPath = getComponentIcon(component.type);

    // Add context menu
    this.contextValue = 'component';

    // Add click command
    this.command = {
      command: 'magicidea.goToComponent',
      title: 'Go to Component',
      arguments: [component]
    };
  }
}

// Register tree view
export function registerTreeView(context: vscode.ExtensionContext) {
  const treeProvider = new ComponentTreeProvider();

  vscode.window.registerTreeDataProvider('magicidea.componentTree', treeProvider);

  // Register commands
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.refreshTree', () => {
      const editor = vscode.window.activeTextEditor;
      if (editor && editor.document.languageId === 'magicidea') {
        treeProvider.refresh(editor.document.getText());
      }
    })
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.goToComponent', (component: Component) => {
      // Navigate to component in editor
      const editor = vscode.window.activeTextEditor;
      if (editor) {
        const position = findComponentPosition(editor.document, component);
        if (position) {
          editor.selection = new vscode.Selection(position, position);
          editor.revealRange(new vscode.Range(position, position));
        }
      }
    })
  );
}
```

#### 6. Quick Actions & Commands

**Implementation:**
```typescript
// client/src/commands.ts
import * as vscode from 'vscode';

export function registerCommands(context: vscode.ExtensionContext) {

  // New MagicIdea File
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.newFile', async () => {
      const doc = await vscode.workspace.openTextDocument({
        language: 'magicidea',
        content: getNewFileTemplate()
      });
      vscode.window.showTextDocument(doc);
    })
  );

  // Generate Code
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.generateCode', async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor || editor.document.languageId !== 'magicidea') {
        vscode.window.showErrorMessage('No MagicIdea file open');
        return;
      }

      // Show platform picker
      const platforms = [
        { label: 'Kotlin (Jetpack Compose)', value: 'kotlin' },
        { label: 'Swift (SwiftUI)', value: 'swift' },
        { label: 'React (TypeScript)', value: 'react' }
      ];

      const selected = await vscode.window.showQuickPick(platforms, {
        placeHolder: 'Select target platform',
        canPickMany: true
      });

      if (!selected || selected.length === 0) return;

      // Generate code
      await vscode.window.withProgress({
        location: vscode.ProgressLocation.Notification,
        title: 'Generating code...',
        cancellable: false
      }, async (progress) => {
        const code = editor.document.getText();

        for (let i = 0; i < selected.length; i++) {
          const platform = selected[i];
          progress.report({
            increment: (i / selected.length) * 100,
            message: `Generating ${platform.label}...`
          });

          const generated = await generateCode(code, platform.value);
          await saveGeneratedCode(platform.value, generated);
        }
      });

      vscode.window.showInformationMessage('Code generation complete!');
    })
  );

  // Format Document
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.format', async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor || editor.document.languageId !== 'magicidea') return;

      const formatted = formatMagicIdeaCode(editor.document.getText());

      const edit = new vscode.WorkspaceEdit();
      const fullRange = new vscode.Range(
        editor.document.positionAt(0),
        editor.document.positionAt(editor.document.getText().length)
      );
      edit.replace(editor.document.uri, fullRange, formatted);
      await vscode.workspace.applyEdit(edit);
    })
  );

  // Open Theme Designer
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.openThemeDesigner', () => {
      const panel = vscode.window.createWebviewPanel(
        'magicideaThemeDesigner',
        'Theme Designer',
        vscode.ViewColumn.Two,
        {
          enableScripts: true
        }
      );

      panel.webview.html = getThemeDesignerHTML(panel.webview);
    })
  );

  // Insert Component
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.insertComponent', async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor) return;

      // Show component picker
      const components = ComponentRegistry.getAll().map(c => ({
        label: c.name,
        description: c.category,
        detail: c.description
      }));

      const selected = await vscode.window.showQuickPick(components, {
        placeHolder: 'Select a component to insert'
      });

      if (!selected) return;

      const component = ComponentRegistry.get(selected.label);
      const snippet = component.generateSnippet();

      editor.insertSnippet(new vscode.SnippetString(snippet));
    })
  );

  // Validate Document
  context.subscriptions.push(
    vscode.commands.registerCommand('magicidea.validate', async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor || editor.document.languageId !== 'magicidea') return;

      const diagnostics = validateMagicIdea(editor.document.getText());

      if (diagnostics.length === 0) {
        vscode.window.showInformationMessage('No errors found!');
      } else {
        vscode.window.showWarningMessage(`Found ${diagnostics.length} issue(s)`);
      }
    })
  );
}

// Helper functions
function getNewFileTemplate(): string {
  return `App {
\tid: "com.example.app"
\tname: "My App"
\ttheme: iOS26LiquidGlass
\t
\tColumn {
\t\tpadding: 16
\t\t
\t\tText {
\t\t\ttext: "Hello, MagicIdea!"
\t\t\tfont: Title
\t\t}
\t\t
\t\tButton {
\t\t\ttext: "Get Started"
\t\t\tstyle: Primary
\t\t\tonClick: { /* Add action */ }
\t\t}
\t}
}`;
}
```

#### 7. Code Actions (Quick Fixes)

**Implementation:**
```typescript
// client/src/codeActions.ts
import * as vscode from 'vscode';

export class MagicIdeaCodeActionProvider implements vscode.CodeActionProvider {

  provideCodeActions(
    document: vscode.TextDocument,
    range: vscode.Range | vscode.Selection,
    context: vscode.CodeActionContext,
    token: vscode.CancellationToken
  ): vscode.CodeAction[] {

    const actions: vscode.CodeAction[] = [];

    // Check for missing required properties
    const missingProps = getMissingRequiredProperties(document, range);
    if (missingProps.length > 0) {
      const action = new vscode.CodeAction(
        'Add missing required properties',
        vscode.CodeActionKind.QuickFix
      );
      action.edit = createEditForMissingProperties(document, range, missingProps);
      actions.push(action);
    }

    // Check for incorrect property types
    const incorrectTypes = getIncorrectPropertyTypes(document, range);
    if (incorrectTypes.length > 0) {
      incorrectTypes.forEach(({ property, expected, actual }) => {
        const action = new vscode.CodeAction(
          `Convert ${property} from ${actual} to ${expected}`,
          vscode.CodeActionKind.QuickFix
        );
        action.edit = createEditForTypeConversion(document, range, property, expected);
        actions.push(action);
      });
    }

    // Wrap with container
    const selectedText = document.getText(range);
    if (selectedText.trim()) {
      ['Column', 'Row', 'Card', 'ScrollView'].forEach(container => {
        const action = new vscode.CodeAction(
          `Wrap with ${container}`,
          vscode.CodeActionKind.Refactor
        );
        action.edit = createEditForWrap(document, range, container);
        actions.push(action);
      });
    }

    // Extract component
    if (isExtractableComponent(document, range)) {
      const action = new vscode.CodeAction(
        'Extract to separate component',
        vscode.CodeActionKind.RefactorExtract
      );
      action.command = {
        title: 'Extract Component',
        command: 'magicidea.extractComponent',
        arguments: [document, range]
      };
      actions.push(action);
    }

    return actions;
  }
}

function createEditForMissingProperties(
  document: vscode.TextDocument,
  range: vscode.Range,
  properties: PropertyDescriptor[]
): vscode.WorkspaceEdit {
  const edit = new vscode.WorkspaceEdit();

  // Find insertion point (end of component body)
  const text = document.getText(range);
  const lastBraceIndex = text.lastIndexOf('}');
  const insertPosition = document.positionAt(
    document.offsetAt(range.start) + lastBraceIndex
  );

  // Build properties text
  const propsText = properties.map(prop =>
    `\t${prop.name}: ${prop.getDefaultValue()}`
  ).join('\n') + '\n';

  edit.insert(document.uri, insertPosition, propsText);

  return edit;
}
```

### VSCode Extension Architecture

```
extension/
├── package.json
├── client/
│   └── src/
│       ├── extension.ts (entry point)
│       ├── preview.ts (webview provider)
│       ├── treeView.ts (component tree)
│       ├── commands.ts (command handlers)
│       ├── codeActions.ts (quick fixes)
│       └── diagnostics.ts (validation)
│
├── server/
│   └── src/
│       ├── server.ts (LSP server)
│       ├── completion.ts (autocomplete)
│       ├── hover.ts (hover info)
│       ├── formatting.ts (code formatting)
│       └── validation.ts (diagnostics)
│
├── syntaxes/
│   └── magicidea.tmLanguage.json (TextMate grammar)
│
├── snippets/
│   └── magicidea.json (code snippets)
│
├── media/
│   ├── preview.js (preview webview script)
│   ├── preview.css (preview styles)
│   └── icons/ (component icons)
│
└── themes/
    └── magicidea-dark.json (syntax theme)
```

### Dependencies

```json
{
  "dependencies": {
    "vscode-languageclient": "^9.0.1",
    "vscode-languageserver": "^9.0.1",
    "vscode-languageserver-textdocument": "^1.0.11"
  },
  "devDependencies": {
    "@types/vscode": "^1.90.0",
    "@types/node": "^20.x",
    "typescript": "^5.4.5",
    "webpack": "^5.91.0"
  }
}
```

---

## Feature Comparison Matrix

| Feature | Android Studio Plugin | VSCode Extension | Priority |
|---------|----------------------|------------------|----------|
| **Syntax Highlighting** | ✅ Semantic + Structural | ✅ TextMate Grammar | P0 |
| **Code Completion** | ✅ Advanced (PSI-based) | ✅ LSP-based | P0 |
| **Live Preview** | ✅ Native Swing/JavaFX | ✅ Webview | P0 |
| **Multi-Platform Preview** | ✅ All 7 platforms | ✅ All 7 platforms | P0 |
| **Component Palette** | ✅ Drag & Drop to Editor | ✅ Quick Pick Menu | P1 |
| **Visual Designer** | ✅ Full Visual Editor | ⚠️ Limited (Tree View) | P1 |
| **Property Inspector** | ✅ Visual Property Panel | ⚠️ Quick Edit | P1 |
| **Theme Designer** | ✅ Full Theme Builder | ✅ Webview Designer | P1 |
| **Code Generator** | ✅ Multi-Target | ✅ Multi-Target | P0 |
| **Component Tree** | ✅ Tree View + Navigator | ✅ Tree View | P1 |
| **Quick Fixes** | ✅ Intentions + Inspections | ✅ Code Actions | P1 |
| **Refactorings** | ✅ Extract, Inline, Rename | ✅ Basic Refactorings | P2 |
| **Documentation** | ✅ Quick Doc (Ctrl+Q) | ✅ Hover Tooltips | P1 |
| **Snippets** | ✅ Live Templates | ✅ Snippet Library | P1 |
| **Validation** | ✅ Real-time Inspections | ✅ Real-time Diagnostics | P0 |
| **Format on Save** | ✅ | ✅ | P2 |
| **Icon Library** | ✅ Material, SF, Fluent | ✅ Material, SF, Fluent | P2 |
| **Color Picker** | ✅ Native Color Chooser | ✅ VSCode Color Picker | P1 |
| **AI Assistance** | ✅ Component Suggestions | ✅ Component Suggestions | P3 |
| **Offline Support** | ✅ | ✅ | P0 |
| **Performance** | ⚡ Excellent (native) | ⚡ Good (JS/webview) | - |

### Priority Levels
- **P0**: Must Have (MVP)
- **P1**: Should Have (V1.0)
- **P2**: Nice to Have (V1.1+)
- **P3**: Future Enhancement (V2.0+)

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2) - MVP

**Goal**: Basic language support and preview

**Android Studio Plugin:**
- [ ] Project setup (Gradle plugin structure)
- [ ] Language definition (MagicIdeaLanguage)
- [ ] Basic syntax highlighting
- [ ] File type association (.magicidea)
- [ ] Simple completion provider (component names)
- [ ] Basic preview panel (Swing-based)
- [ ] Parser integration (MagicIdea core parser)

**VSCode Extension:**
- [ ] Project setup (TypeScript + LSP)
- [ ] TextMate grammar definition
- [ ] Language server foundation
- [ ] Basic completion provider
- [ ] Basic webview preview
- [ ] Document validation

**Deliverables:**
- Syntax highlighting works
- Basic autocomplete for components
- Simple preview shows UI

### Phase 2: Core Features (Weeks 3-4) - V0.5

**Goal**: Advanced completion and intelligent features

**Android Studio Plugin:**
- [ ] Advanced completion (properties, values, enums)
- [ ] Property inspector panel
- [ ] Component palette (list view)
- [ ] Live preview updates (debounced)
- [ ] Multi-platform preview switcher
- [ ] Quick documentation provider
- [ ] Basic quick fixes

**VSCode Extension:**
- [ ] Advanced LSP features (hover, definition)
- [ ] Component tree view
- [ ] Snippet library (all 48 components)
- [ ] Live preview with platform switcher
- [ ] Basic code actions
- [ ] Color picker integration

**Deliverables:**
- Smart autocomplete with property suggestions
- Live preview with platform switching
- Component tree navigation
- Basic quick fixes

### Phase 3: Visual Tools (Weeks 5-6) - V1.0

**Goal**: Visual designers and advanced tooling

**Android Studio Plugin:**
- [ ] Drag & drop component palette
- [ ] Visual designer (basic)
- [ ] Theme designer dialog
- [ ] Code generator integration (Kotlin, Swift, React)
- [ ] Component navigator
- [ ] Advanced refactorings (extract, inline)
- [ ] Intention actions (wrap, extract, etc.)

**VSCode Extension:**
- [ ] Webview-based theme designer
- [ ] Code generator commands
- [ ] Advanced code actions
- [ ] Format on save
- [ ] Export/import functionality

**Deliverables:**
- Drag-and-drop component insertion
- Theme designer (basic version)
- Multi-target code generation
- Extract component refactoring

### Phase 4: Polish & Optimization (Weeks 7-8) - V1.1

**Goal**: Performance, UX improvements, bug fixes

**Both Plugins:**
- [ ] Performance optimization (caching, incremental parsing)
- [ ] Error handling improvements
- [ ] User feedback integration
- [ ] Documentation and tutorials
- [ ] Icon library integration
- [ ] Keyboard shortcuts
- [ ] Settings/preferences panel
- [ ] Telemetry (opt-in)

**Deliverables:**
- Smooth, responsive experience
- Comprehensive documentation
- Bug-free basic functionality

### Phase 5: Advanced Features (Weeks 9-12) - V2.0

**Goal**: AI assistance and advanced visual design

**Android Studio Plugin:**
- [ ] Advanced visual designer (two-way sync)
- [ ] AI-assisted component suggestions
- [ ] Layout optimizer
- [ ] Accessibility checker
- [ ] Performance profiler integration
- [ ] State management visualization

**VSCode Extension:**
- [ ] AI-assisted coding
- [ ] Advanced theme designer
- [ ] Component library browser
- [ ] Template marketplace integration

**Deliverables:**
- AI-powered development experience
- Professional visual design tools
- Enterprise-grade features

---

## Code Examples

### Example 1: Component Completion Provider (Android Studio)

```kotlin
// MagicIdeaCompletionProvider.kt
package com.augmentalis.magicidea.intellij

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.augmentalis.magicidea.core.ComponentRegistry

class MagicIdeaCompletionContributor : CompletionContributor() {
    init {
        // Component name completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withParent(MagicIdeaComponentName::class.java),
            ComponentNameCompletionProvider()
        )

        // Property name completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withParent(MagicIdeaPropertyName::class.java),
            PropertyNameCompletionProvider()
        )

        // Property value completion
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withParent(MagicIdeaPropertyValue::class.java),
            PropertyValueCompletionProvider()
        )
    }
}

class ComponentNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        ComponentRegistry.getAll().forEach { component ->
            result.addElement(
                LookupElementBuilder
                    .create(component.name)
                    .withIcon(ComponentIcons.getIcon(component.category))
                    .withTypeText(component.category, true)
                    .withTailText(" (${component.properties.size} props)", true)
                    .withPresentableText(component.name)
                    .withInsertHandler { context, item ->
                        val document = context.document
                        val offset = context.tailOffset

                        // Insert opening brace and properties template
                        document.insertString(offset, " {\n\t")

                        // Add required properties
                        val requiredProps = component.properties.filter { it.required }
                        if (requiredProps.isNotEmpty()) {
                            val propsText = requiredProps.joinToString("\n\t") { prop ->
                                "${prop.name}: ${prop.getDefaultValue()}"
                            }
                            document.insertString(offset + 4, propsText)
                        }

                        document.insertString(context.editor.caretModel.offset, "\n}")

                        // Move caret to first property value
                        context.editor.caretModel.moveToOffset(offset + 4 + requiredProps[0].name.length + 2)
                    }
            )
        }
    }
}

class PropertyNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val componentType = getParentComponentType(parameters.position) ?: return
        val component = ComponentRegistry.get(componentType) ?: return

        component.properties.forEach { prop ->
            result.addElement(
                LookupElementBuilder
                    .create(prop.name)
                    .withIcon(AllIcons.Nodes.Property)
                    .withTypeText(prop.type.name, true)
                    .withTailText(
                        if (prop.required) " (required)" else " (optional)",
                        true
                    )
                    .withInsertHandler { ctx, item ->
                        val doc = ctx.document
                        val offset = ctx.tailOffset

                        // Insert colon and default value
                        doc.insertString(offset, ": ")

                        val defaultValue = prop.getDefaultValue()
                        doc.insertString(offset + 2, defaultValue)

                        // Select default value for easy editing
                        ctx.editor.selectionModel.setSelection(
                            offset + 2,
                            offset + 2 + defaultValue.length
                        )
                    }
            )
        }
    }
}

class PropertyValueCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val propertyName = getParentPropertyName(parameters.position) ?: return
        val componentType = getParentComponentType(parameters.position) ?: return
        val component = ComponentRegistry.get(componentType) ?: return
        val property = component.properties.find { it.name == propertyName } ?: return

        when (property.type) {
            PropertyType.ENUM -> {
                property.enumValues?.forEach { enumValue ->
                    result.addElement(
                        LookupElementBuilder.create(enumValue)
                            .withIcon(AllIcons.Nodes.Enum)
                    )
                }
            }
            PropertyType.COLOR -> {
                CommonColors.forEach { (name, hex) ->
                    result.addElement(
                        LookupElementBuilder.create("\"$hex\"")
                            .withPresentableText(name)
                            .withTypeText(hex, true)
                            .withIcon(createColorIcon(hex))
                    )
                }
            }
            PropertyType.BOOLEAN -> {
                result.addElement(LookupElementBuilder.create("true"))
                result.addElement(LookupElementBuilder.create("false"))
            }
            PropertyType.ICON -> {
                IconRegistry.getAll().forEach { icon ->
                    result.addElement(
                        LookupElementBuilder.create(icon.name)
                            .withIcon(icon.icon)
                            .withTypeText(icon.category, true)
                    )
                }
            }
        }
    }
}
```

### Example 2: Live Preview Renderer (Android Studio)

```kotlin
// MagicIdeaPreviewRenderer.kt
package com.augmentalis.magicidea.intellij.preview

import com.augmentalis.magicidea.core.*
import java.awt.*
import javax.swing.*

class MagicIdeaPreviewRenderer(
    private val platform: Platform
) {
    private val styleEngine = StyleEngine(platform)

    fun render(ast: MagicIdeaAst): JComponent {
        val rootPanel = JPanel(BorderLayout())
        rootPanel.background = styleEngine.getBackgroundColor()

        ast.components.forEach { component ->
            rootPanel.add(renderComponent(component))
        }

        return rootPanel
    }

    private fun renderComponent(component: Component): JComponent {
        return when (component.type) {
            "Button" -> renderButton(component)
            "TextField" -> renderTextField(component)
            "Text" -> renderText(component)
            "Image" -> renderImage(component)
            "Column" -> renderColumn(component)
            "Row" -> renderRow(component)
            "Card" -> renderCard(component)
            else -> renderUnknown(component)
        }
    }

    private fun renderButton(component: Component): JComponent {
        val text = component.properties["text"] as? String ?: "Button"
        val style = component.properties["style"] as? String ?: "Primary"

        val button = JButton(text)

        // Apply platform-specific styling
        when (platform) {
            Platform.IOS_26 -> applyIOSButtonStyle(button, style)
            Platform.ANDROID -> applyMaterialButtonStyle(button, style)
            Platform.WINDOWS_11 -> applyFluentButtonStyle(button, style)
            else -> applyDefaultButtonStyle(button, style)
        }

        return button
    }

    private fun applyIOSButtonStyle(button: JButton, style: String) {
        // iOS 26 Liquid Glass button styling
        button.apply {
            font = Font("SF Pro", Font.PLAIN, 17)
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder(12, 24, 12, 24)

            when (style) {
                "Primary" -> {
                    background = Color(0, 122, 255) // iOS blue
                    foreground = Color.WHITE
                    isOpaque = true
                }
                "Secondary" -> {
                    background = Color(242, 242, 247) // iOS gray
                    foreground = Color.BLACK
                    isOpaque = true
                }
                else -> {
                    isOpaque = false
                    foreground = Color(0, 122, 255)
                }
            }

            // Add rounded corners
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
    }

    private fun applyMaterialButtonStyle(button: JButton, style: String) {
        // Material 3 button styling
        button.apply {
            font = Font("Roboto", Font.MEDIUM, 14)
            isFocusPainted = false
            border = BorderFactory.createEmptyBorder(10, 24, 10, 24)

            when (style) {
                "Primary" -> {
                    background = Color(103, 80, 164) // Material purple
                    foreground = Color.WHITE
                    isOpaque = true
                }
                "Secondary" -> {
                    background = Color(224, 224, 224) // Material gray
                    foreground = Color.BLACK
                    isOpaque = true
                }
                else -> {
                    isOpaque = false
                    foreground = Color(103, 80, 164)
                }
            }
        }
    }

    private fun renderTextField(component: Component): JComponent {
        val placeholder = component.properties["placeholder"] as? String ?: ""
        val label = component.properties["label"] as? String

        val panel = JPanel(BorderLayout())

        // Add label if present
        if (label != null) {
            val labelComp = JLabel(label)
            labelComp.font = styleEngine.getLabelFont()
            panel.add(labelComp, BorderLayout.NORTH)
        }

        val textField = JTextField(placeholder)
        textField.font = styleEngine.getBodyFont()
        textField.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(styleEngine.getBorderColor()),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        )

        panel.add(textField, BorderLayout.CENTER)

        return panel
    }

    private fun renderText(component: Component): JComponent {
        val text = component.properties["text"] as? String ?: ""
        val fontSize = component.properties["size"] as? Float ?: 16f
        val color = component.properties["color"] as? String ?: "#000000"

        val label = JLabel(text)
        label.font = Font(styleEngine.getFontFamily(), Font.PLAIN, fontSize.toInt())
        label.foreground = Color.decode(color)

        return label
    }

    private fun renderColumn(component: Component): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val arrangement = component.properties["arrangement"] as? String ?: "Start"
        val alignment = component.properties["horizontalAlignment"] as? String ?: "Start"

        component.children.forEach { child ->
            val childComponent = renderComponent(child)

            // Apply alignment
            when (alignment) {
                "Start" -> childComponent.alignmentX = Component.LEFT_ALIGNMENT
                "Center" -> childComponent.alignmentX = Component.CENTER_ALIGNMENT
                "End" -> childComponent.alignmentX = Component.RIGHT_ALIGNMENT
            }

            panel.add(childComponent)

            // Add spacing based on arrangement
            if (arrangement == "SpaceBetween" || arrangement == "SpaceAround") {
                panel.add(Box.createVerticalGlue())
            } else {
                panel.add(Box.createVerticalStrut(8))
            }
        }

        return panel
    }

    private fun renderRow(component: Component): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)

        val arrangement = component.properties["arrangement"] as? String ?: "Start"
        val alignment = component.properties["verticalAlignment"] as? String ?: "Center"

        component.children.forEach { child ->
            val childComponent = renderComponent(child)

            // Apply alignment
            when (alignment) {
                "Top" -> childComponent.alignmentY = Component.TOP_ALIGNMENT
                "Center" -> childComponent.alignmentY = Component.CENTER_ALIGNMENT
                "Bottom" -> childComponent.alignmentY = Component.BOTTOM_ALIGNMENT
            }

            panel.add(childComponent)

            // Add spacing
            if (arrangement == "SpaceBetween" || arrangement == "SpaceAround") {
                panel.add(Box.createHorizontalGlue())
            } else {
                panel.add(Box.createHorizontalStrut(8))
            }
        }

        return panel
    }

    private fun renderCard(component: Component): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(styleEngine.getBorderColor()),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        )
        panel.background = styleEngine.getSurfaceColor()

        // Add shadow effect
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 2, Color(0, 0, 0, 30)),
            panel.border
        )

        component.children.forEach { child ->
            panel.add(renderComponent(child))
        }

        return panel
    }

    private fun renderUnknown(component: Component): JComponent {
        val label = JLabel("Unknown: ${component.type}")
        label.foreground = Color.RED
        return label
    }
}

// Platform-specific style engine
class StyleEngine(private val platform: Platform) {
    fun getBackgroundColor(): Color = when (platform) {
        Platform.IOS_26 -> Color.WHITE
        Platform.ANDROID -> Color(250, 250, 250)
        Platform.WINDOWS_11 -> Color(243, 243, 243)
        else -> Color.WHITE
    }

    fun getSurfaceColor(): Color = when (platform) {
        Platform.IOS_26 -> Color(242, 242, 247)
        Platform.ANDROID -> Color.WHITE
        Platform.WINDOWS_11 -> Color(251, 251, 251)
        else -> Color.WHITE
    }

    fun getBorderColor(): Color = when (platform) {
        Platform.IOS_26 -> Color(198, 198, 200)
        Platform.ANDROID -> Color(224, 224, 224)
        Platform.WINDOWS_11 -> Color(229, 229, 229)
        else -> Color.GRAY
    }

    fun getFontFamily(): String = when (platform) {
        Platform.IOS_26 -> "SF Pro"
        Platform.ANDROID -> "Roboto"
        Platform.WINDOWS_11 -> "Segoe UI"
        else -> "System"
    }

    fun getBodyFont(): Font = Font(getFontFamily(), Font.PLAIN, 16)
    fun getLabelFont(): Font = Font(getFontFamily(), Font.PLAIN, 14)
}
```

### Example 3: LSP Completion (VSCode)

```typescript
// server/src/completion.ts
import {
  CompletionItem,
  CompletionItemKind,
  InsertTextFormat,
  TextDocumentPositionParams
} from 'vscode-languageserver/node';

import { TextDocument } from 'vscode-languageserver-textdocument';
import { ComponentRegistry } from './registry';

export function provideCompletion(
  document: TextDocument,
  position: TextDocumentPositionParams
): CompletionItem[] {
  const text = document.getText();
  const offset = document.offsetAt(position.position);

  // Determine context
  const context = getCompletionContext(text, offset);

  switch (context.type) {
    case 'component':
      return provideComponentCompletions();
    case 'property':
      return providePropertyCompletions(context.componentType);
    case 'value':
      return provideValueCompletions(context.propertyType);
    default:
      return [];
  }
}

function provideComponentCompletions(): CompletionItem[] {
  return ComponentRegistry.getAll().map(component => {
    const requiredProps = component.properties.filter(p => p.required);
    const snippet = [
      `${component.name} {`,
      ...requiredProps.map((prop, i) =>
        `\t${prop.name}: \${${i + 1}:${prop.getDefaultValue()}}`
      ),
      '\t$0',
      '}'
    ].join('\n');

    return {
      label: component.name,
      kind: CompletionItemKind.Class,
      detail: component.category,
      documentation: {
        kind: 'markdown',
        value: [
          component.description,
          '',
          '**Properties:**',
          ...component.properties.map(p =>
            `- \`${p.name}\`: ${p.type.name}${p.required ? ' *(required)*' : ''}`
          ),
          '',
          '**Example:**',
          '```magicidea',
          component.exampleCode,
          '```'
        ].join('\n')
      },
      insertText: snippet,
      insertTextFormat: InsertTextFormat.Snippet,
      sortText: `0${component.name}` // Sort components first
    };
  });
}

function providePropertyCompletions(componentType: string): CompletionItem[] {
  const component = ComponentRegistry.get(componentType);
  if (!component) return [];

  return component.properties.map((prop, index) => {
    return {
      label: prop.name,
      kind: CompletionItemKind.Property,
      detail: `${prop.type.name}${prop.required ? ' (required)' : ''}`,
      documentation: {
        kind: 'markdown',
        value: [
          prop.description,
          '',
          `**Type:** ${prop.type.name}`,
          `**Required:** ${prop.required ? 'Yes' : 'No'}`,
          prop.defaultValue ? `**Default:** ${prop.defaultValue}` : '',
          '',
          prop.exampleValue ? `**Example:** \`${prop.exampleValue}\`` : ''
        ].filter(Boolean).join('\n')
      },
      insertText: `${prop.name}: \${1:${prop.getDefaultValue()}}$0`,
      insertTextFormat: InsertTextFormat.Snippet,
      sortText: prop.required ? `0${index}` : `1${index}` // Required props first
    };
  });
}

function provideValueCompletions(propertyType: PropertyType): CompletionItem[] {
  switch (propertyType.kind) {
    case 'enum':
      return propertyType.values.map(value => ({
        label: value,
        kind: CompletionItemKind.EnumMember,
        insertText: value
      }));

    case 'color':
      return [
        ...Object.entries(CommonColors).map(([name, hex]) => ({
          label: name,
          kind: CompletionItemKind.Color,
          detail: hex,
          insertText: `"${hex}"`,
          documentation: `Hex: ${hex}`
        })),
        {
          label: 'Custom Color...',
          kind: CompletionItemKind.Color,
          insertText: '"#${1:FFFFFF}"$0',
          insertTextFormat: InsertTextFormat.Snippet
        }
      ];

    case 'boolean':
      return [
        { label: 'true', kind: CompletionItemKind.Keyword },
        { label: 'false', kind: CompletionItemKind.Keyword }
      ];

    case 'icon':
      return IconRegistry.getAll().map(icon => ({
        label: icon.name,
        kind: CompletionItemKind.Value,
        detail: icon.category,
        insertText: icon.name
      }));

    default:
      return [];
  }
}

// Context detection
interface CompletionContext {
  type: 'component' | 'property' | 'value' | 'unknown';
  componentType?: string;
  propertyType?: PropertyType;
}

function getCompletionContext(text: string, offset: number): CompletionContext {
  // Get text before cursor
  const textBeforeCursor = text.substring(0, offset);

  // Check if we're at component level (no enclosing braces or at root)
  const braceDepth = (textBeforeCursor.match(/{/g) || []).length -
                     (textBeforeCursor.match(/}/g) || []).length;

  if (braceDepth === 0) {
    return { type: 'component' };
  }

  // Check if we're after a colon (property value)
  const lastColon = textBeforeCursor.lastIndexOf(':');
  const lastNewline = textBeforeCursor.lastIndexOf('\n');

  if (lastColon > lastNewline) {
    // We're in a value position
    const propertyName = extractPropertyName(textBeforeCursor, lastColon);
    const componentType = extractComponentType(textBeforeCursor);

    if (componentType && propertyName) {
      const component = ComponentRegistry.get(componentType);
      const property = component?.properties.find(p => p.name === propertyName);

      if (property) {
        return {
          type: 'value',
          componentType,
          propertyType: property.type
        };
      }
    }
  }

  // Check if we're in a component body (property name position)
  const componentMatch = textBeforeCursor.match(/(\w+)\s*\{[^}]*$/);
  if (componentMatch) {
    return {
      type: 'property',
      componentType: componentMatch[1]
    };
  }

  return { type: 'unknown' };
}

function extractPropertyName(text: string, colonIndex: number): string | null {
  const beforeColon = text.substring(0, colonIndex);
  const match = beforeColon.match(/(\w+)\s*$/);
  return match ? match[1] : null;
}

function extractComponentType(text: string): string | null {
  const match = text.match(/(\w+)\s*\{[^}]*$/);
  return match ? match[1] : null;
}

// Common colors
const CommonColors = {
  'Red': '#FF0000',
  'Green': '#00FF00',
  'Blue': '#0000FF',
  'Black': '#000000',
  'White': '#FFFFFF',
  'Gray': '#808080',
  'iOS Blue': '#007AFF',
  'iOS Green': '#34C759',
  'iOS Red': '#FF3B30',
  'Material Purple': '#6750A4',
  'Material Blue': '#1976D2',
  'Fluent Blue': '#0078D4'
};
```

---

## Developer Experience Goals

### Core Principles

1. **Zero Friction** - From install to first preview in under 60 seconds
2. **Discoverable** - Features are easy to find and use
3. **Forgiving** - Mistakes are caught early with helpful suggestions
4. **Powerful** - Advanced features available when needed
5. **Fast** - Real-time feedback, no waiting for builds
6. **Consistent** - Similar experience across Android Studio and VSCode

### User Journeys

#### Journey 1: First-Time User

**Goal**: Create and preview first MagicIdea app

**Steps:**
1. Install plugin from marketplace (1 click)
2. Create new MagicIdea file (Cmd+N → "MagicIdea File")
3. See template with Button and TextField
4. Edit text property → see live preview update
5. Click "Generate Code" → choose platform → see generated code
6. **Time**: Under 5 minutes

**Success Metrics:**
- User completes journey without reading docs
- Preview updates feel instant (<100ms)
- Generated code is clean and runs without errors

#### Journey 2: Experienced Developer

**Goal**: Build complex multi-screen app with custom theme

**Steps:**
1. Open component palette
2. Drag 10+ components to build form
3. Open theme designer → customize colors/fonts
4. See theme applied in real-time to all components
5. Extract repeated patterns into reusable components
6. Generate code for iOS, Android, and Web
7. **Time**: Under 30 minutes

**Success Metrics:**
- No context switching to documentation
- Theme changes visible immediately
- Code generation produces production-ready code

#### Journey 3: Designer (Non-Coder)

**Goal**: Create UI mockup using visual tools

**Steps:**
1. Open Visual Designer (Android Studio only)
2. Drag components from palette to canvas
3. Adjust properties using Property Inspector
4. Preview on multiple platforms
5. Export as shareable MagicIdea file
6. **Time**: Under 15 minutes

**Success Metrics:**
- Zero code writing required
- Visual designer is intuitive (no training needed)
- Output file is valid MagicIdea DSL

### Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Plugin Install Time | < 30s | From marketplace click to ready |
| First Preview Time | < 2s | From file open to preview shown |
| Completion Latency | < 50ms | From keystroke to suggestions shown |
| Preview Update Latency | < 100ms | From code change to preview update |
| Code Generation Time | < 5s | For typical app (10-20 components) |
| Theme Designer Load Time | < 1s | From button click to designer shown |
| Memory Usage | < 200MB | Plugin overhead (excluding IDE) |
| CPU Usage (Idle) | < 1% | When not actively editing |

### Accessibility

**Requirements:**
- All UI accessible via keyboard shortcuts
- Screen reader support for all panels
- High contrast theme support
- Configurable font sizes
- Color-blind friendly icons and highlights

---

## Technical Architecture

### Android Studio Plugin Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     IntelliJ Platform                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Language   │  │  File Type   │  │    Parser    │     │
│  │  Definition  │  │  Definition  │  │  Integration │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Syntax     │  │  Completion  │  │     Quick    │     │
│  │ Highlighter  │  │  Contributor │  │     Fixes    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Preview    │  │  Property    │  │   Theme      │     │
│  │   Window     │  │  Inspector   │  │   Designer   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Component   │  │   Code Gen   │  │    Actions   │     │
│  │   Palette    │  │   Service    │  │ & Intentions │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   MagicIdea Core     │
                  ├──────────────────────┤
                  │  • Parser            │
                  │  • AST Builder       │
                  │  • Component Registry│
                  │  • Code Generators   │
                  │  • Theme Manager     │
                  └──────────────────────┘
```

### VSCode Extension Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    VSCode Extension Host                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Extension Client                      │    │
│  │  • Activation                                      │    │
│  │  • Command Registration                            │    │
│  │  • Webview Providers                               │    │
│  │  • Tree View Providers                             │    │
│  └────────────────────────────────────────────────────┘    │
│                         │                                   │
│                         │ LSP                               │
│                         ▼                                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Language Server (Node.js)                │    │
│  │  • Completion Provider                             │    │
│  │  • Hover Provider                                  │    │
│  │  • Diagnostic Provider                             │    │
│  │  • Formatting Provider                             │    │
│  │  • Color Provider                                  │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │            Webview Providers                       │    │
│  │  • Preview Panel (HTML/CSS/JS)                     │    │
│  │  • Theme Designer (HTML/CSS/JS)                    │    │
│  │  • Code Generator (HTML/CSS/JS)                    │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Tree View Providers                   │    │
│  │  • Component Tree                                  │    │
│  │  • Theme List                                      │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   MagicIdea Core     │
                  │   (WASM/JS Bindings) │
                  ├──────────────────────┤
                  │  • Parser            │
                  │  • AST Builder       │
                  │  • Component Registry│
                  │  • Code Generators   │
                  │  • Theme Manager     │
                  └──────────────────────┘
```

### Shared Core Architecture

Both plugins share the same MagicIdea Core library:

```kotlin
// MagicIdea Core (Kotlin Multiplatform)
module MagicIdeaCore {
    commonMain {
        • Parser (DSL → AST)
        • AST Node Definitions
        • Component Registry
        • Property Type System
        • Validation Engine
        • Code Generators
          - Kotlin Compose Generator
          - SwiftUI Generator
          - React Generator
        • Theme Manager
        • Serialization (JSON, YAML)
    }

    jvmMain {
        • Android Studio Plugin Integration
        • Java Swing Preview Rendering
    }

    jsMain {
        • VSCode Extension Integration (via WASM/JS bindings)
        • HTML Preview Rendering
    }
}
```

### Data Flow

```
User Types Code
      │
      ▼
┌─────────────┐
│   Editor    │
└─────────────┘
      │
      ▼ (text changed)
┌─────────────┐
│   Parser    │ ───▶ [Syntax Errors] ───▶ Show Red Squiggles
└─────────────┘
      │
      ▼ (valid DSL)
┌─────────────┐
│  AST Builder│
└─────────────┘
      │
      ▼
┌─────────────┐
│  Validator  │ ───▶ [Semantic Errors] ───▶ Show Warnings
└─────────────┘
      │
      ▼ (valid AST)
      ├────────────┬────────────┬────────────┐
      ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Preview  │ │Component │ │Completion│ │  Quick   │
│ Renderer │ │   Tree   │ │ Provider │ │   Docs   │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
      │
      ▼
  User Sees Live Preview
```

---

## Testing & Quality Assurance

### Testing Strategy

#### Unit Tests (80% Coverage Target)

**Parser Tests:**
```kotlin
class MagicIdeaParserTest {
    @Test
    fun `should parse simple Button component`() {
        val dsl = """
            Button {
                text: "Click Me"
                style: Primary
            }
        """.trimIndent()

        val ast = MagicIdeaParser.parse(dsl)

        assertEquals("Button", ast.components[0].type)
        assertEquals("Click Me", ast.components[0].properties["text"])
        assertEquals("Primary", ast.components[0].properties["style"])
    }

    @Test
    fun `should report missing required property`() {
        val dsl = """
            Button {
                style: Primary
            }
        """.trimIndent()

        val errors = MagicIdeaParser.validate(dsl)

        assertTrue(errors.any { it.message.contains("text") })
    }
}
```

**Completion Provider Tests:**
```kotlin
class CompletionProviderTest {
    @Test
    fun `should suggest component names at root level`() {
        val suggestions = getCompletions("", 0)

        assertTrue(suggestions.any { it.label == "Button" })
        assertTrue(suggestions.any { it.label == "TextField" })
        assertTrue(suggestions.any { it.label == "Column" })
    }

    @Test
    fun `should suggest properties for Button component`() {
        val dsl = "Button { }"
        val cursorPosition = 9 // inside braces

        val suggestions = getCompletions(dsl, cursorPosition)

        assertTrue(suggestions.any { it.label == "text" })
        assertTrue(suggestions.any { it.label == "style" })
        assertTrue(suggestions.any { it.label == "onClick" })
    }
}
```

#### Integration Tests

**Preview Rendering Tests:**
```kotlin
class PreviewRendererTest {
    @Test
    fun `should render Button with iOS style`() {
        val component = Component(
            type = "Button",
            properties = mapOf(
                "text" to "Test",
                "style" to "Primary"
            )
        )

        val renderer = MagicIdeaPreviewRenderer(Platform.IOS_26)
        val swing = renderer.render(component)

        assertTrue(swing is JButton)
        assertEquals("Test", (swing as JButton).text)
        assertEquals(Color(0, 122, 255), swing.background)
    }
}
```

#### UI Tests (Android Studio)

```kotlin
@RunWith(JUnit4::class)
class PluginUITest {
    @Test
    fun testPreviewWindowOpens() {
        val fixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createFixtureBuilder("MagicIdeaTest")
            .fixture

        fixture.setUp()

        // Create MagicIdea file
        val file = fixture.addFileToProject(
            "test.magicidea",
            "Button { text: \"Test\" }"
        )

        // Open file
        FileEditorManager.getInstance(fixture.project).openFile(file.virtualFile, true)

        // Check preview window exists
        val toolWindow = ToolWindowManager.getInstance(fixture.project)
            .getToolWindow("MagicIdea Preview")

        assertNotNull(toolWindow)
        assertTrue(toolWindow.isVisible)

        fixture.tearDown()
    }
}
```

#### End-to-End Tests (VSCode)

```typescript
import * as vscode from 'vscode';
import * as assert from 'assert';

suite('MagicIdea Extension Test Suite', () => {
  test('Should activate on .magicidea file', async () => {
    const doc = await vscode.workspace.openTextDocument({
      language: 'magicidea',
      content: 'Button { text: "Test" }'
    });

    await vscode.window.showTextDocument(doc);

    // Wait for extension to activate
    await new Promise(resolve => setTimeout(resolve, 1000));

    const ext = vscode.extensions.getExtension('augmentalis.magicidea');
    assert.ok(ext);
    assert.ok(ext.isActive);
  });

  test('Should provide completions', async () => {
    const doc = await vscode.workspace.openTextDocument({
      language: 'magicidea',
      content: ''
    });

    await vscode.window.showTextDocument(doc);

    const completions = await vscode.commands.executeCommand<vscode.CompletionList>(
      'vscode.executeCompletionItemProvider',
      doc.uri,
      new vscode.Position(0, 0)
    );

    assert.ok(completions);
    assert.ok(completions.items.length > 0);
    assert.ok(completions.items.some(item => item.label === 'Button'));
  });
});
```

### Performance Testing

**Metrics to Track:**
- Completion latency (target: <50ms)
- Preview render time (target: <100ms)
- Memory usage over time (detect leaks)
- CPU usage during idle (target: <1%)

**Tools:**
- JProfiler (Android Studio plugin profiling)
- VSCode Extension Profiler
- Custom performance counters

### User Acceptance Testing

**Beta Testing Plan:**
1. **Alpha** (Week 4-5): Internal team testing
2. **Private Beta** (Week 6-7): 10 selected developers
3. **Public Beta** (Week 8-9): Open to all via "pre-release" flag
4. **Release** (Week 10): Stable release to marketplace

**Feedback Collection:**
- In-app feedback button
- GitHub issues
- Discord community
- Usage analytics (opt-in)

---

## Conclusion

This specification provides a comprehensive blueprint for building world-class IDE plugins for the MagicIdea framework. By following this design, we will create the most developer-friendly UI framework tooling available, surpassing existing solutions like Flutter, React Native, and Jetpack Compose.

### Key Differentiators

1. **Multi-Platform Live Preview** - See your UI on all 7 platforms simultaneously
2. **Visual Design Tools** - Build UIs without writing code
3. **AI-Assisted Development** - Smart suggestions and code generation
4. **Unified Experience** - Consistent tooling across Android Studio and VSCode
5. **Zero Configuration** - Works out of the box

### Next Steps

1. **Phase 1 Implementation** (Weeks 1-2)
   - Set up project structure for both plugins
   - Implement basic syntax highlighting
   - Create simple preview panels
   - Get end-to-end demo working

2. **Team Formation**
   - 2 developers for Android Studio plugin
   - 2 developers for VSCode extension
   - 1 UX designer for visual tools
   - 1 technical writer for documentation

3. **Community Engagement**
   - Create GitHub repository (public)
   - Set up Discord community
   - Launch landing page with demo videos
   - Begin marketing to early adopters

---

**Document Status**: ✅ Ready for Implementation
**Review Date**: 2025-11-15
**Version**: 1.0.0

Created by Manoj Jhawar, manoj@ideahq.net

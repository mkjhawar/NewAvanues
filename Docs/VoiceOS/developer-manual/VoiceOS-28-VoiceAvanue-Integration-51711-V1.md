# Chapter 28: VoiceAvanue Integration

**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Living Document
**Complexity:** Advanced
**Estimated Reading Time:** 60 minutes

---

## Table of Contents

- [28.1 VoiceAvanue Ecosystem Overview](#281-voiceavanue-ecosystem-overview)
- [28.2 IDEAMagic Framework Architecture](#282-ideamagic-framework-architecture)
- [28.3 Integration Architecture](#283-integration-architecture)
- [28.4 Shared Components](#284-shared-components)
- [28.5 Data Synchronization](#285-data-synchronization)
- [28.6 Communication Protocol](#286-communication-protocol)
- [28.7 Deployment Strategy](#287-deployment-strategy)
- [28.8 Implementation Guide](#288-implementation-guide)
- [28.9 Best Practices](#289-best-practices)
- [28.10 Troubleshooting](#2810-troubleshooting)

---

## 28.1 VoiceAvanue Ecosystem Overview

### 28.1.1 What is VoiceAvanue?

**VoiceAvanue** is a world-class cross-platform UI framework ecosystem that combines voice-enabled operating system capabilities with a comprehensive multi-platform UI component system.

**Project Location:** `/Volumes/M-Drive/Coding/voiceavanue/`

**Key Statistics:**
- **Total Kotlin Files:** 234
- **Total Lines of Code:** 56,243
- **Total Classes:** 580
- **Total Functions:** 1,297
- **Total Modules:** 27
- **Platforms:** Android (✅), Desktop (✅), iOS (🔄), Web (📋 Planned)

### 28.1.2 Core Components

VoiceAvanue consists of three major subsystems:

#### 1. IDEAMagic Framework
```
Universal/IDEAMagic/
├── MagicCode/          # DSL compiler & code generator
├── MagicUI/            # Runtime UI rendering system
├── Components/         # UI component library
├── CodeGen/            # Code generation tools
├── Database/           # Cross-platform database
├── Examples/           # Example implementations
├── Libraries/          # Supporting libraries
└── VoiceOSBridge/      # VOS4 legacy bridge
```

**Purpose:** Core cross-platform UI framework with DSL, components, and renderers

#### 2. VoiceOS Applications
```
apps/
└── avanuelaunch/       # Launcher app for Avanue platform
```

**Purpose:** Accessibility-focused applications leveraging VOS4 capabilities

#### 3. Avanue Platform Apps
- **VoiceAvanue:** Voice-enabled interface
- **AIAvanue:** AI assistant integration
- **BrowserAvanue:** Voice-controlled browser
- **NoteAvanue:** Voice note-taking

### 28.1.3 Architecture Philosophy

VoiceAvanue follows a **three-tier component architecture**:

```
Tier 1: Direct Compose Usage (Fastest)
  └─ @Composable fun App() { MagicButton("Click") }
     Uses Foundation components directly

Tier 2: DSL-Driven Cross-Platform
  └─ val ui = MagicUI { Button("Click") }
     Uses Core + Renderers for cross-platform

Tier 3: MagicCode Code Generation
  └─ Button "Click" { style Filled }
     Parses DSL, generates platform code
```

This architecture enables:
- **Performance:** Direct Compose usage when needed
- **Portability:** Cross-platform DSL for shared UIs
- **Productivity:** Code generation for rapid development

### 28.1.4 Platform Support Matrix

| Platform | Status | Technology | Completion |
|----------|--------|------------|------------|
| **Android** | ✅ Production | Jetpack Compose + Material 3 | 100% |
| **Desktop** | ✅ Production | Compose Desktop | 100% |
| **iOS** | 🔄 In Progress | Compose Multiplatform | 60% |
| **Web** | 📋 Planned | React + Material-UI | 0% |

---

## 28.2 IDEAMagic Framework Architecture

### 28.2.1 Framework Structure

IDEAMagic is the core framework powering VoiceAvanue. It provides:

1. **Platform-Agnostic Component Definitions**
2. **Native Platform Renderers**
3. **DSL-Based Code Generation**
4. **Type-Safe Design System**
5. **Reactive State Management**

### 28.2.2 Component System Design

#### Three-Layer Architecture

```
┌─────────────────────────────────────────┐
│  Layer 1: Core Component Definitions    │
│  (Platform-Agnostic Data Models)        │
│  - 32+ component interfaces             │
│  - Component.kt, ComponentStyle.kt      │
│  - Renderer.kt interface                │
└─────────────┬───────────────────────────┘
              │
              ↓
┌─────────────────────────────────────────┐
│  Layer 2: Foundation Implementations    │
│  (Production-Ready Compose Components)  │
│  - 15 Material 3 components             │
│  - Type-safe (MagicColor, MagicDp)      │
│  - Two-way state binding                │
└─────────────┬───────────────────────────┘
              │
              ↓
┌─────────────────────────────────────────┐
│  Layer 3: Platform Renderers            │
│  (Bridge Core → Native)                 │
│  - ComposeRenderer (Android/Desktop)    │
│  - iOSRenderer (iOS/macOS)              │
│  - WebRenderer (React/TypeScript)       │
└─────────────────────────────────────────┘
```

#### Renderer Pattern

```kotlin
// Core Component Definition (Platform-Agnostic)
data class ButtonComponent(
    val text: String,
    val onClick: () -> Unit,
    val variant: ButtonVariant = ButtonVariant.Filled,
    val enabled: Boolean = true,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// Platform Renderer (Compose)
class ComposeRenderer : Renderer {
    @Composable
    override fun render(button: ButtonComponent): Unit {
        MagicButton(
            text = button.text,
            onClick = button.onClick,
            variant = button.variant,
            enabled = button.enabled,
            style = button.style?.toCompose()
        )
    }
}

// Platform Renderer (iOS - Planned)
class iOSRenderer : Renderer {
    override fun render(button: ButtonComponent): SwiftUIView {
        return Button(button.text) {
            button.onClick()
        }
        .buttonStyle(button.variant.toSwiftUI())
        .disabled(!button.enabled)
    }
}
```

### 28.2.3 Module Breakdown

#### MagicUI Runtime System

**Location:** `Universal/IDEAMagic/MagicUI/`
**Files:** 59 Kotlin files
**Purpose:** Runtime system for rendering MagicCode DSL

**Sub-Modules:**

##### 1. DesignSystem
```kotlin
// Design Tokens
object DesignTokens {
    object Colors {
        val Primary = MagicColor(0xFF6200EE)
        val Secondary = MagicColor(0xFF03DAC5)
        val Background = MagicColor(0xFFFFFFFF)
        // ... 50+ predefined colors
    }

    object Typography {
        val DisplayLarge = TextStyle(
            fontSize = 57.sp,
            lineHeight = 64.sp,
            fontWeight = FontWeight.Normal
        )
        // ... 15 typography styles
    }

    object Spacing {
        val ExtraSmall = MagicDp(4)
        val Small = MagicDp(8)
        val Medium = MagicDp(16)
        val Large = MagicDp(24)
        val ExtraLarge = MagicDp(32)
    }

    object Shapes {
        val ExtraSmall = MagicBorderRadius(4.dp)
        val Small = MagicBorderRadius(8.dp)
        val Medium = MagicBorderRadius(12.dp)
        val Large = MagicBorderRadius(16.dp)
        val ExtraLarge = MagicBorderRadius(28.dp)
    }

    object Elevation {
        val Level0 = MagicDp(0)
        val Level1 = MagicDp(1)
        val Level2 = MagicDp(3)
        val Level3 = MagicDp(6)
        val Level4 = MagicDp(8)
        val Level5 = MagicDp(12)
    }
}

// Theme Composable
@Composable
fun MagicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(DesignTokens.Colors.Primary.value),
            // ... complete color mapping
        )
        else -> lightColorScheme(
            primary = Color(DesignTokens.Colors.Primary.value),
            // ... complete color mapping
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesignTokens.Typography.toMaterialTypography(),
        content = content
    )
}
```

##### 2. CoreTypes (Type-Safe Dimensions)
```kotlin
// Type-Safe Value Classes
@JvmInline
value class MagicDp(val value: Dp) {
    constructor(value: Float) : this(value.dp)
    operator fun plus(other: MagicDp) = MagicDp(value + other.value)
    operator fun minus(other: MagicDp) = MagicDp(value - other.value)
    operator fun times(factor: Float) = MagicDp(value * factor)
}

@JvmInline
value class MagicSp(val value: TextUnit) {
    constructor(value: Float) : this(value.sp)
}

@JvmInline
value class MagicPx(val value: Float)

// Color with multiple constructors
@JvmInline
value class MagicColor(val value: Long) {
    constructor(hex: Int) : this(0xFF000000 or hex.toLong())
    constructor(r: Int, g: Int, b: Int, a: Int = 255) :
        this((a.toLong() shl 24) or (r.toLong() shl 16) or
             (g.toLong() shl 8) or b.toLong())

    fun toComposeColor(): Color = Color(value)
}

// Composite Types
data class MagicSize(
    val width: MagicDp,
    val height: MagicDp
)

data class MagicPadding(
    val start: MagicDp = MagicDp(0f),
    val top: MagicDp = MagicDp(0f),
    val end: MagicDp = MagicDp(0f),
    val bottom: MagicDp = MagicDp(0f)
) {
    constructor(all: MagicDp) : this(all, all, all, all)
    constructor(horizontal: MagicDp, vertical: MagicDp) :
        this(horizontal, vertical, horizontal, vertical)
}

data class MagicBorderRadius(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}
```

##### 3. StateManagement (Reactive State)
```kotlin
// Reactive State Interface
interface MagicState<T> {
    val value: T
    fun update(newValue: T)
    fun observe(observer: (T) -> Unit)
}

// Implementation
class MutableMagicState<T>(initialValue: T) : MagicState<T> {
    private val _value = mutableStateOf(initialValue)
    override val value: T get() = _value.value

    override fun update(newValue: T) {
        _value.value = newValue
    }

    override fun observe(observer: (T) -> Unit) {
        // Compose integration
        snapshotFlow { value }.onEach(observer).launchIn(/* scope */)
    }
}

// Derived State (Computed)
class DerivedMagicState<T>(
    private val dependencies: List<MagicState<*>>,
    private val computation: () -> T
) : MagicState<T> {
    override val value: T get() = computation()
    override fun update(newValue: T) {
        throw UnsupportedOperationException("Cannot update derived state")
    }
    override fun observe(observer: (T) -> Unit) {
        dependencies.forEach { dep ->
            dep.observe { observer(value) }
        }
    }
}

// Collection State
class MagicStateList<T>(initialList: List<T> = emptyList()) {
    private val _list = mutableStateListOf<T>().apply { addAll(initialList) }
    val size: Int get() = _list.size

    fun add(item: T) { _list.add(item) }
    fun remove(item: T) { _list.remove(item) }
    fun clear() { _list.clear() }
    operator fun get(index: Int) = _list[index]
    fun toList(): List<T> = _list.toList()
}

// Compose Integration
@Composable
fun <T> rememberMagicState(initialValue: T): MagicState<T> {
    return remember { MutableMagicState(initialValue) }
}
```

#### MagicCode DSL Compiler

**Location:** `Universal/IDEAMagic/MagicCode/`
**Files:** 19 Kotlin files
**Purpose:** Parses `.vos` DSL files and generates platform-specific code

**Key Components:**

##### 1. VosTokenizer (Lexical Analysis)
```kotlin
class VosTokenizer(private val source: String) {
    private var pos = 0
    private val tokens = mutableListOf<Token>()

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            skipWhitespace()
            when (val char = peek()) {
                '{' -> addToken(TokenType.LBRACE, "{")
                '}' -> addToken(TokenType.RBRACE, "}")
                '(' -> addToken(TokenType.LPAREN, "(")
                ')' -> addToken(TokenType.RPAREN, ")")
                '"' -> tokenizeString()
                in 'a'..'z', in 'A'..'Z', '_' -> tokenizeIdentifier()
                in '0'..'9' -> tokenizeNumber()
                else -> error("Unexpected character: $char")
            }
        }
        return tokens
    }

    private fun tokenizeString() {
        advance() // Skip opening quote
        val start = pos
        while (peek() != '"' && !isAtEnd()) advance()
        val value = source.substring(start, pos)
        advance() // Skip closing quote
        addToken(TokenType.STRING, value)
    }

    private fun tokenizeIdentifier() {
        val start = pos
        while (peek().isLetterOrDigit() || peek() == '_') advance()
        val value = source.substring(start, pos)
        val type = when (value) {
            "Button" -> TokenType.BUTTON
            "Text" -> TokenType.TEXT
            "Column" -> TokenType.COLUMN
            "Row" -> TokenType.ROW
            "style" -> TokenType.STYLE
            "onClick" -> TokenType.ON_CLICK
            else -> TokenType.IDENTIFIER
        }
        addToken(type, value)
    }

    // ... more tokenization logic
}
```

##### 2. VosParser (Syntax Analysis & AST)
```kotlin
class VosParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): ASTNode {
        val components = mutableListOf<ComponentNode>()
        while (!isAtEnd()) {
            components.add(parseComponent())
        }
        return ProgramNode(components)
    }

    private fun parseComponent(): ComponentNode {
        val type = consume(TokenType.IDENTIFIER).value
        val name = if (check(TokenType.STRING)) {
            consume(TokenType.STRING).value
        } else null

        val properties = if (check(TokenType.LBRACE)) {
            parsePropertiesBlock()
        } else emptyMap()

        return ComponentNode(
            type = type,
            name = name,
            properties = properties
        )
    }

    private fun parsePropertiesBlock(): Map<String, Any> {
        consume(TokenType.LBRACE)
        val properties = mutableMapOf<String, Any>()

        while (!check(TokenType.RBRACE)) {
            val key = consume(TokenType.IDENTIFIER).value
            val value = when {
                check(TokenType.STRING) -> consume(TokenType.STRING).value
                check(TokenType.NUMBER) -> consume(TokenType.NUMBER).value.toInt()
                check(TokenType.IDENTIFIER) -> consume(TokenType.IDENTIFIER).value
                else -> error("Expected value")
            }
            properties[key] = value
        }

        consume(TokenType.RBRACE)
        return properties
    }
}

// AST Nodes
sealed class ASTNode
data class ProgramNode(val components: List<ComponentNode>) : ASTNode()
data class ComponentNode(
    val type: String,
    val name: String?,
    val properties: Map<String, Any>
) : ASTNode()
```

##### 3. KotlinComposeGenerator
```kotlin
class KotlinComposeGenerator(private val ast: ASTNode) {
    fun generate(): String {
        val code = StringBuilder()
        code.appendLine("import androidx.compose.runtime.Composable")
        code.appendLine("import com.augmentalis.magicui.foundation.*")
        code.appendLine()
        code.appendLine("@Composable")
        code.appendLine("fun GeneratedUI() {")

        when (ast) {
            is ProgramNode -> {
                ast.components.forEach { component ->
                    code.appendLine("    ${generateComponent(component)}")
                }
            }
        }

        code.appendLine("}")
        return code.toString()
    }

    private fun generateComponent(node: ComponentNode): String {
        return when (node.type) {
            "Button" -> generateButton(node)
            "Text" -> generateText(node)
            "Column" -> generateColumn(node)
            "Row" -> generateRow(node)
            else -> error("Unknown component: ${node.type}")
        }
    }

    private fun generateButton(node: ComponentNode): String {
        val text = node.name ?: ""
        val onClick = node.properties["onClick"] ?: "{}"
        val variant = node.properties["style"] ?: "Filled"

        return """
            MagicButton(
                text = "$text",
                onClick = $onClick,
                variant = ButtonVariant.$variant
            )
        """.trimIndent()
    }

    // ... more generation logic
}
```

##### 4. SwiftUIGenerator (Planned)
```kotlin
class SwiftUIGenerator(private val ast: ASTNode) {
    fun generate(): String {
        // Generates SwiftUI code from AST
        // Similar structure to KotlinComposeGenerator
        // Target: iOS/macOS
    }
}
```

##### 5. ReactTypeScriptGenerator (Planned)
```kotlin
class ReactTypeScriptGenerator(private val ast: ASTNode) {
    fun generate(): String {
        // Generates React/TypeScript code from AST
        // Uses Material-UI components
        // Target: Web
    }
}
```

### 28.2.4 Foundation Components

**Location:** `Universal/IDEAMagic/Components/Foundation/`
**Files:** 9 Kotlin files (~1,800 lines)
**Status:** ✅ 15 production-ready components

#### Implemented Components

##### 1. MagicButton
```kotlin
enum class ButtonVariant {
    Filled, Outlined, Text, Tonal
}

@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    colors: ButtonColors? = null
) {
    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors ?: ButtonDefaults.buttonColors()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            Text(text)
        }
        ButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            Text(text)
        }
        ButtonVariant.Tonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            Text(text)
        }
    }
}

// Preset Buttons
@Composable
fun MagicPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = MagicButton(text, onClick, modifier, ButtonVariant.Filled)

@Composable
fun MagicSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = MagicButton(text, onClick, modifier, ButtonVariant.Outlined)
```

##### 2. MagicText
```kotlin
enum class TextPreset {
    DisplayLarge, DisplayMedium, DisplaySmall,
    HeadlineLarge, HeadlineMedium, HeadlineSmall,
    TitleLarge, TitleMedium, TitleSmall,
    BodyLarge, BodyMedium, BodySmall,
    LabelLarge, LabelMedium, LabelSmall
}

@Composable
fun MagicText(
    text: String,
    modifier: Modifier = Modifier,
    preset: TextPreset? = null,
    style: TextStyle? = null,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    val effectiveStyle = when {
        preset != null -> preset.toTextStyle()
        style != null -> style
        else -> LocalTextStyle.current
    }.let {
        it.copy(
            color = if (color != Color.Unspecified) color else it.color,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else it.fontSize,
            fontWeight = fontWeight ?: it.fontWeight,
            textAlign = textAlign ?: it.textAlign
        )
    }

    Text(
        text = text,
        modifier = modifier,
        style = effectiveStyle,
        overflow = overflow,
        maxLines = maxLines
    )
}

private fun TextPreset.toTextStyle(): TextStyle {
    return when (this) {
        TextPreset.DisplayLarge -> MaterialTheme.typography.displayLarge
        TextPreset.DisplayMedium -> MaterialTheme.typography.displayMedium
        TextPreset.DisplaySmall -> MaterialTheme.typography.displaySmall
        TextPreset.HeadlineLarge -> MaterialTheme.typography.headlineLarge
        TextPreset.HeadlineMedium -> MaterialTheme.typography.headlineMedium
        TextPreset.HeadlineSmall -> MaterialTheme.typography.headlineSmall
        TextPreset.TitleLarge -> MaterialTheme.typography.titleLarge
        TextPreset.TitleMedium -> MaterialTheme.typography.titleMedium
        TextPreset.TitleSmall -> MaterialTheme.typography.titleSmall
        TextPreset.BodyLarge -> MaterialTheme.typography.bodyLarge
        TextPreset.BodyMedium -> MaterialTheme.typography.bodyMedium
        TextPreset.BodySmall -> MaterialTheme.typography.bodySmall
        TextPreset.LabelLarge -> MaterialTheme.typography.labelLarge
        TextPreset.LabelMedium -> MaterialTheme.typography.labelMedium
        TextPreset.LabelSmall -> MaterialTheme.typography.labelSmall
    }
}

// Preset Text Components
@Composable
fun MagicHeadline(
    text: String,
    modifier: Modifier = Modifier
) = MagicText(text, modifier, preset = TextPreset.HeadlineLarge)

@Composable
fun MagicTitle(
    text: String,
    modifier: Modifier = Modifier
) = MagicText(text, modifier, preset = TextPreset.TitleLarge)

@Composable
fun MagicBody(
    text: String,
    modifier: Modifier = Modifier
) = MagicText(text, modifier, preset = TextPreset.BodyMedium)
```

##### 3. MagicTextField (Two-Way Binding)
```kotlin
enum class TextFieldPreset {
    Email, Password, Number, Phone, Search, TextArea
}

@Composable
fun MagicTextField(
    value: MagicState<String>,
    modifier: Modifier = Modifier,
    preset: TextFieldPreset? = null,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true
) {
    val keyboardOptions = when (preset) {
        TextFieldPreset.Email -> KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
        TextFieldPreset.Password -> KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )
        TextFieldPreset.Number -> KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
        TextFieldPreset.Phone -> KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        )
        TextFieldPreset.Search -> KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        )
        TextFieldPreset.TextArea -> KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        )
        else -> KeyboardOptions.Default
    }

    OutlinedTextField(
        value = value.value,
        onValueChange = { value.update(it) },
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (preset == TextFieldPreset.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        }
    )
}

// Preset TextFields
@Composable
fun MagicEmailField(
    value: MagicState<String>,
    modifier: Modifier = Modifier,
    label: String = "Email"
) = MagicTextField(value, modifier, TextFieldPreset.Email, label)

@Composable
fun MagicPasswordField(
    value: MagicState<String>,
    modifier: Modifier = Modifier,
    label: String = "Password"
) = MagicTextField(value, modifier, TextFieldPreset.Password, label)
```

##### 4. MagicCard
```kotlin
enum class CardVariant {
    Filled, Elevated, Outlined
}

enum class CardPreset {
    Default, Compact, Expanded
}

@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Filled,
    preset: CardPreset = CardPreset.Default,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.then(
        when (preset) {
            CardPreset.Default -> Modifier.fillMaxWidth()
            CardPreset.Compact -> Modifier.fillMaxWidth().padding(8.dp)
            CardPreset.Expanded -> Modifier.fillMaxWidth().padding(16.dp)
        }
    )

    when (variant) {
        CardVariant.Filled -> Card(
            modifier = cardModifier,
            onClick = onClick ?: {},
            content = content
        )
        CardVariant.Elevated -> ElevatedCard(
            modifier = cardModifier,
            onClick = onClick ?: {},
            content = content
        )
        CardVariant.Outlined -> OutlinedCard(
            modifier = cardModifier,
            onClick = onClick ?: {},
            content = content
        )
    }
}

// Preset Cards
@Composable
fun MagicListCard(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicCard(
        modifier = modifier,
        variant = CardVariant.Filled,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MagicTitle(title)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                MagicBody(subtitle)
            }
        }
    }
}
```

##### 5. MagicLayouts
```kotlin
@Composable
fun MagicColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
fun MagicRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}

@Composable
fun MagicBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun MagicScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        content = content
    )
}

@Composable
fun MagicScrollRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        content = content
    )
}
```

**Complete Component List:**
1. MagicButton (4 variants + presets)
2. MagicText (15 typography styles + 6 presets)
3. MagicTextField (6 presets with validation)
4. MagicIcon (tint support)
5. MagicImage (3 presets: Avatar, Cover, Thumbnail)
6. MagicCard (3 variants + 3 presets)
7. MagicLayouts (Column, Row, Box, Scroll)
8. MagicContainers (Surface, Divider, Badge, Chip)
9. MagicListItem (6 presets)

### 28.2.5 Core Component Definitions

**Location:** `Universal/IDEAMagic/Components/Core/`
**Files:** 44 Kotlin files (~8,000 lines)
**Status:** 32+ components defined

**Component Categories:**

#### 1. Basic Components
```kotlin
// ButtonComponent.kt
data class ButtonComponent(
    val text: String,
    val onClick: () -> Unit,
    val variant: ButtonVariant = ButtonVariant.Filled,
    val enabled: Boolean = true,
    val leadingIcon: IconComponent? = null,
    val trailingIcon: IconComponent? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// TextComponent.kt
data class TextComponent(
    val text: String,
    val preset: TextPreset? = null,
    val color: MagicColor? = null,
    val fontSize: MagicSp? = null,
    val fontWeight: FontWeight? = null,
    val textAlign: TextAlign? = null,
    val maxLines: Int = Int.MAX_VALUE,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ImageComponent.kt
data class ImageComponent(
    val source: String, // URL or resource ID
    val contentDescription: String?,
    val contentScale: ContentScale = ContentScale.Fit,
    val size: MagicSize? = null,
    val shape: MagicBorderRadius? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

#### 2. Container Components
```kotlin
// CardComponent.kt
data class CardComponent(
    val content: List<Component>,
    val variant: CardVariant = CardVariant.Filled,
    val onClick: (() -> Unit)? = null,
    val padding: MagicPadding = MagicPadding(MagicDp(16f)),
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ChipComponent.kt
data class ChipComponent(
    val label: String,
    val leadingIcon: IconComponent? = null,
    val trailingIcon: IconComponent? = null,
    val selected: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

#### 3. Layout Components
```kotlin
// ColumnComponent.kt
data class ColumnComponent(
    val children: List<Component>,
    val verticalArrangement: VerticalArrangement = VerticalArrangement.Top,
    val horizontalAlignment: HorizontalAlignment = HorizontalAlignment.Start,
    val padding: MagicPadding? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// RowComponent.kt
data class RowComponent(
    val children: List<Component>,
    val horizontalArrangement: HorizontalArrangement = HorizontalArrangement.Start,
    val verticalAlignment: VerticalAlignment = VerticalAlignment.Top,
    val padding: MagicPadding? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ScrollViewComponent.kt
data class ScrollViewComponent(
    val child: Component,
    val direction: ScrollDirection = ScrollDirection.Vertical,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

#### 4. Form Components
```kotlin
// TextFieldComponent.kt
data class TextFieldComponent(
    val value: MagicState<String>,
    val label: String? = null,
    val placeholder: String? = null,
    val leadingIcon: IconComponent? = null,
    val trailingIcon: IconComponent? = null,
    val inputType: InputType = InputType.Text,
    val validation: ((String) -> Boolean)? = null,
    val errorMessage: String? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// CheckboxComponent.kt
data class CheckboxComponent(
    val checked: MagicState<Boolean>,
    val label: String? = null,
    val enabled: Boolean = true,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// SliderComponent.kt
data class SliderComponent(
    val value: MagicState<Float>,
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    val steps: Int = 0,
    val label: String? = null,
    val enabled: Boolean = true,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

#### 5. Feedback Components
```kotlin
// DialogComponent.kt
data class DialogComponent(
    val title: String,
    val content: Component,
    val confirmButton: ButtonComponent,
    val dismissButton: ButtonComponent? = null,
    val onDismiss: () -> Unit,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ToastComponent.kt
data class ToastComponent(
    val message: String,
    val duration: ToastDuration = ToastDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

// ProgressBarComponent.kt
data class ProgressBarComponent(
    val progress: Float, // 0.0 to 1.0
    val indeterminate: Boolean = false,
    val label: String? = null,
    val style: ComponentStyle? = null
) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

**Full Component List (32+ components):**

**Basic (4):** Button, Text, Image, Icon
**Containers (4):** Card, Chip, Divider, Badge
**Layouts (4):** Column, Row, Container, ScrollView
**Lists (2):** List, ListItem
**Forms (10):** TextField, Checkbox, Switch, Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating
**Feedback (6):** Dialog, Toast, Alert, ProgressBar, Spinner, Tooltip
**Navigation (2):** AppBar, BottomNav (Phase 3)

---

## 28.3 Integration Architecture

### 28.3.1 Integration Points

VOS4 can integrate with VoiceAvanue at multiple levels:

#### Level 1: Direct Component Usage
```kotlin
// Use Foundation components directly in VOS4
import com.augmentalis.magicui.foundation.*

@Composable
fun VOS4Screen() {
    MagicColumn {
        MagicHeadline("VOS4 Settings")
        MagicDivider()
        MagicTextField(
            value = rememberMagicState(""),
            label = "Voice Command",
            preset = TextFieldPreset.Search
        )
        MagicPrimaryButton(
            text = "Execute",
            onClick = { /* handle */ }
        )
    }
}
```

**Advantages:**
- Fastest performance (no abstraction overhead)
- Full Compose power
- Direct Material 3 integration

**Use Cases:**
- VOS4 UI screens
- Settings interfaces
- Voice command UIs

#### Level 2: Core Component Abstraction
```kotlin
// Use Core components for cross-platform compatibility
import com.augmentalis.magicui.core.*

fun createVOS4UI(): Component {
    return ColumnComponent(
        children = listOf(
            TextComponent("VOS4 Settings", preset = TextPreset.HeadlineLarge),
            TextFieldComponent(
                value = MutableMagicState(""),
                label = "Voice Command",
                inputType = InputType.Search
            ),
            ButtonComponent(
                text = "Execute",
                onClick = { /* handle */ },
                variant = ButtonVariant.Filled
            )
        )
    )
}

// Render on Android
@Composable
fun VOS4Screen() {
    val renderer = ComposeRenderer()
    val ui = createVOS4UI()
    ui.render(renderer)
}

// Render on iOS (future)
func VOS4Screen() -> some View {
    let renderer = iOSRenderer()
    let ui = createVOS4UI()
    return ui.render(renderer)
}
```

**Advantages:**
- Cross-platform UI definition
- Share UI logic across platforms
- Future-proof for iOS/Web

**Use Cases:**
- Shared UI components
- Multi-platform apps
- Plugin UIs

#### Level 3: MagicCode DSL
```kotlin
// Define UI in .vos DSL
/*
VOS4Settings.vos:

Column {
    Text "VOS4 Settings" {
        preset HeadlineLarge
    }

    TextField {
        label "Voice Command"
        type Search
        bind voiceCommand
    }

    Button "Execute" {
        style Filled
        onClick executeCommand
    }
}
*/

// Generate Kotlin Compose code
val generator = KotlinComposeGenerator(parser.parse("VOS4Settings.vos"))
val generatedCode = generator.generate()

// Generated output:
@Composable
fun VOS4Settings() {
    MagicColumn {
        MagicText(
            text = "VOS4 Settings",
            preset = TextPreset.HeadlineLarge
        )
        MagicTextField(
            value = voiceCommand,
            label = "Voice Command",
            preset = TextFieldPreset.Search
        )
        MagicButton(
            text = "Execute",
            onClick = ::executeCommand,
            variant = ButtonVariant.Filled
        )
    }
}
```

**Advantages:**
- Fastest development
- Minimal boilerplate
- Designer-friendly syntax

**Use Cases:**
- Rapid prototyping
- Generated UIs
- Template-based screens

### 28.3.2 Module Dependencies

#### VOS4 → VoiceAvanue Dependencies

```kotlin
// VOS4 app/build.gradle.kts
dependencies {
    // Option 1: Direct Foundation components
    implementation("com.augmentalis:magicui-foundation:1.0.0")

    // Option 2: Full IDEAMagic framework
    implementation("com.augmentalis:ideamagic-runtime:1.0.0")

    // Option 3: MagicCode CLI (build-time)
    // Used via Gradle plugin or CLI tool
}
```

#### VoiceAvanue → VOS4 Dependencies

```kotlin
// VoiceAvanue bridge to VOS4
// Universal/IDEAMagic/VoiceOSBridge/build.gradle.kts
dependencies {
    // Access VOS4 accessibility services
    compileOnly(project(":vos4:modules:apps:VoiceOSCore"))

    // Use VOS4 speech recognition
    implementation(project(":vos4:modules:libraries:SpeechRecognition"))

    // Use VOS4 command system
    implementation(project(":vos4:modules:managers:CommandManager"))
}
```

### 28.3.3 Data Flow Architecture

```
┌──────────────────────────────────────────────────────┐
│                    VOS4 Application                  │
│  ┌────────────────────────────────────────────────┐  │
│  │         VoiceUI (Main Interface)               │  │
│  │  ┌──────────────────────────────────────────┐  │  │
│  │  │    MagicUI Foundation Components         │  │  │
│  │  │  - MagicButton, MagicText, etc.          │  │  │
│  │  └──────────────┬───────────────────────────┘  │  │
│  │                 ↓                                │  │
│  │  ┌──────────────────────────────────────────┐  │  │
│  │  │    VOS4 State Management                 │  │  │
│  │  │  - MagicState integration                │  │  │
│  │  │  - Voice command state                   │  │  │
│  │  └──────────────┬───────────────────────────┘  │  │
│  └─────────────────┼──────────────────────────────┘  │
│                    ↓                                  │
│  ┌─────────────────────────────────────────────────┐ │
│  │         VoiceOSCore (Accessibility)             │ │
│  │  - UI scraping                                  │ │
│  │  - Voice commands                               │ │
│  │  - Cursor control                               │ │
│  └─────────────────┬───────────────────────────────┘ │
└────────────────────┼──────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────┐
│           VoiceAvanue Integration Layer              │
│  ┌────────────────────────────────────────────────┐  │
│  │    MagicUI Runtime                             │  │
│  │  - DesignSystem (themes)                       │  │
│  │  - StateManagement (reactive state)            │  │
│  │  - CoreTypes (type-safe values)                │  │
│  └────────────────┬───────────────────────────────┘  │
│                   ↓                                   │
│  ┌────────────────────────────────────────────────┐  │
│  │    Platform Renderers                          │  │
│  │  - ComposeRenderer (Android/Desktop)           │  │
│  │  - iOSRenderer (iOS/macOS) - Planned           │  │
│  │  - WebRenderer (Web) - Planned                 │  │
│  └────────────────┬───────────────────────────────┘  │
└───────────────────┼──────────────────────────────────┘
                    ↓
         ┌──────────────────────┐
         │  Native Platform UI  │
         │  - Jetpack Compose   │
         │  - SwiftUI (future)  │
         │  - React (future)    │
         └──────────────────────┘
```

---

## 28.4 Shared Components

### 28.4.1 Design System Integration

VOS4 can leverage VoiceAvanue's complete Material 3 design system:

```kotlin
// VOS4 app theme
import com.augmentalis.magicui.designsystem.*

@Composable
fun VOS4Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MagicTheme(
        darkTheme = darkTheme,
        dynamicColor = true, // Material You support
        content = content
    )
}

// Access design tokens
val primaryColor = DesignTokens.Colors.Primary
val headlineStyle = DesignTokens.Typography.HeadlineLarge
val mediumSpacing = DesignTokens.Spacing.Medium
val roundedShape = DesignTokens.Shapes.Medium
```

### 28.4.2 State Management Integration

```kotlin
// VOS4 voice command state
class VoiceCommandViewModel : ViewModel() {
    // Use MagicState for reactive UI
    val currentCommand = MutableMagicState("")
    val isListening = MutableMagicState(false)
    val recognizedText = MutableMagicState("")

    // Derived state
    val hasCommand = DerivedMagicState(
        dependencies = listOf(currentCommand),
        computation = { currentCommand.value.isNotBlank() }
    )

    fun startListening() {
        isListening.update(true)
        // Start speech recognition
    }

    fun stopListening() {
        isListening.update(false)
        currentCommand.update(recognizedText.value)
    }
}

@Composable
fun VoiceCommandScreen(viewModel: VoiceCommandViewModel) {
    MagicColumn(modifier = Modifier.fillMaxSize()) {
        MagicText(
            text = if (viewModel.isListening.value) {
                "Listening..."
            } else {
                "Tap to speak"
            },
            preset = TextPreset.HeadlineMedium
        )

        MagicTextField(
            value = viewModel.currentCommand,
            label = "Voice Command",
            placeholder = "Say a command..."
        )

        MagicButton(
            text = if (viewModel.isListening.value) "Stop" else "Start",
            onClick = {
                if (viewModel.isListening.value) {
                    viewModel.stopListening()
                } else {
                    viewModel.startListening()
                }
            }
        )
    }
}
```

### 28.4.3 Component Reuse Examples

#### Voice Settings Screen
```kotlin
@Composable
fun VoiceSettingsScreen(
    viewModel: VoiceSettingsViewModel
) {
    MagicScrollColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.Medium)
    ) {
        // Header
        MagicHeadline("Voice Settings")
        Spacer(Modifier.height(DesignTokens.Spacing.Large))

        // Speech Engine Selection
        MagicCard(variant = CardVariant.Filled) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Speech Engine")
                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                viewModel.engines.forEach { engine ->
                    MagicRadioButton(
                        selected = viewModel.selectedEngine.value == engine,
                        label = engine.name,
                        onClick = { viewModel.selectEngine(engine) }
                    )
                }
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.Medium))

        // Volume Control
        MagicCard(variant = CardVariant.Outlined) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Voice Volume")
                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                MagicSlider(
                    value = viewModel.volume,
                    valueRange = 0f..1f,
                    steps = 10
                )

                MagicBody(
                    text = "${(viewModel.volume.value * 100).toInt()}%",
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.Medium))

        // Language Selection
        MagicCard(variant = CardVariant.Elevated) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Language")
                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                MagicDropdown(
                    value = viewModel.selectedLanguage,
                    options = viewModel.availableLanguages,
                    label = "Select Language"
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.ExtraLarge))

        // Save Button
        MagicPrimaryButton(
            text = "Save Settings",
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

#### Voice Command List
```kotlin
@Composable
fun VoiceCommandList(
    commands: List<VoiceCommand>,
    onCommandClick: (VoiceCommand) -> Unit
) {
    MagicColumn {
        MagicHeadline("Available Commands")
        Spacer(Modifier.height(DesignTokens.Spacing.Medium))

        commands.forEach { command ->
            MagicListCard(
                title = command.trigger,
                subtitle = command.description,
                onClick = { onCommandClick(command) },
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.ExtraSmall)
            )
        }
    }
}
```

---

## 28.5 Data Synchronization

### 28.5.1 Shared Data Models

VOS4 and VoiceAvanue can share data models using Kotlin Multiplatform:

```kotlin
// Shared data model (in commonMain)
@Serializable
data class VoiceCommand(
    val id: String,
    val trigger: String,
    val action: String,
    val parameters: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val priority: Int = 0
)

@Serializable
data class VoiceSettings(
    val engineType: SpeechEngineType,
    val language: String,
    val volume: Float,
    val pitch: Float,
    val speed: Float,
    val enableWakeWord: Boolean,
    val wakeWord: String?
)

enum class SpeechEngineType {
    GOOGLE, VIVOKA, VOSK, CUSTOM
}
```

### 28.5.2 Database Synchronization

#### VOS4 Room Database
```kotlin
// VOS4 uses Room for local storage
@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val action: String,
    val parameters: String, // JSON serialized
    val enabled: Boolean,
    val priority: Int,
    val lastUsed: Long?,
    val useCount: Int
)

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM voice_commands WHERE enabled = 1 ORDER BY priority DESC")
    fun getEnabledCommands(): Flow<List<VoiceCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: VoiceCommandEntity)

    @Update
    suspend fun update(command: VoiceCommandEntity)

    @Delete
    suspend fun delete(command: VoiceCommandEntity)
}
```

#### VoiceAvanue Database (Planned)
```kotlin
// VoiceAvanue uses SQLDelight for cross-platform storage
// Universal/IDEAMagic/Database/

// voice_command.sq
CREATE TABLE voice_command (
    id TEXT NOT NULL PRIMARY KEY,
    trigger TEXT NOT NULL,
    action TEXT NOT NULL,
    parameters TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    priority INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER,
    use_count INTEGER NOT NULL DEFAULT 0
);

selectAll:
SELECT * FROM voice_command WHERE enabled = 1 ORDER BY priority DESC;

insert:
INSERT OR REPLACE INTO voice_command(id, trigger, action, parameters, enabled, priority)
VALUES (?, ?, ?, ?, ?, ?);

update:
UPDATE voice_command SET
    trigger = ?,
    action = ?,
    parameters = ?,
    enabled = ?,
    priority = ?
WHERE id = ?;

delete:
DELETE FROM voice_command WHERE id = ?;
```

#### Synchronization Strategy
```kotlin
// Bidirectional sync between VOS4 and VoiceAvanue
class VoiceCommandSyncManager(
    private val vos4Dao: VoiceCommandDao,
    private val voiceAvanueQueries: VoiceCommandQueries,
    private val syncService: SyncService
) {
    suspend fun syncFromVOS4ToAvanue() {
        vos4Dao.getEnabledCommands().collect { commands ->
            commands.forEach { entity ->
                voiceAvanueQueries.insert(
                    id = entity.id,
                    trigger = entity.trigger,
                    action = entity.action,
                    parameters = entity.parameters,
                    enabled = if (entity.enabled) 1 else 0,
                    priority = entity.priority.toLong()
                )
            }
        }
    }

    suspend fun syncFromAvanueToVOS4() {
        val commands = voiceAvanueQueries.selectAll().executeAsList()
        commands.forEach { row ->
            vos4Dao.insert(
                VoiceCommandEntity(
                    id = row.id,
                    trigger = row.trigger,
                    action = row.action,
                    parameters = row.parameters,
                    enabled = row.enabled == 1L,
                    priority = row.priority.toInt(),
                    lastUsed = row.last_used,
                    useCount = row.use_count.toInt()
                )
            )
        }
    }

    suspend fun bidirectionalSync() {
        // Conflict resolution: most recent wins
        val vos4Commands = vos4Dao.getEnabledCommands().first()
        val avanueCommands = voiceAvanueQueries.selectAll().executeAsList()

        // Merge and resolve conflicts
        val merged = mergeCommands(vos4Commands, avanueCommands)

        // Write back to both databases
        merged.forEach { command ->
            vos4Dao.insert(command.toVOS4Entity())
            voiceAvanueQueries.insert(command.toAvanueRow())
        }
    }

    private fun mergeCommands(
        vos4: List<VoiceCommandEntity>,
        avanue: List<VoiceCommand>
    ): List<VoiceCommand> {
        // Implement conflict resolution logic
        // Example: prefer command with most recent lastUsed timestamp
        val vos4Map = vos4.associateBy { it.id }
        val avanueMap = avanue.associateBy { it.id }

        val allIds = (vos4Map.keys + avanueMap.keys).toSet()

        return allIds.mapNotNull { id ->
            val vos4Cmd = vos4Map[id]
            val avanuCmd = avanueMap[id]

            when {
                vos4Cmd != null && avanuCmd != null -> {
                    // Both exist - choose most recent
                    if (vos4Cmd.lastUsed ?: 0 > avanuCmd.lastUsed ?: 0) {
                        vos4Cmd.toSharedModel()
                    } else {
                        avanuCmd
                    }
                }
                vos4Cmd != null -> vos4Cmd.toSharedModel()
                avanuCmd != null -> avanuCmd
                else -> null
            }
        }
    }
}
```

### 28.5.3 Real-Time Sync

```kotlin
// Real-time synchronization using Flow
class RealtimeVoiceCommandSync(
    private val vos4Dao: VoiceCommandDao,
    private val voiceAvanueQueries: VoiceCommandQueries
) {
    fun observeVOS4Commands(): Flow<List<VoiceCommand>> {
        return vos4Dao.getEnabledCommands().map { entities ->
            entities.map { it.toSharedModel() }
        }
    }

    fun observeAvanueCommands(): Flow<List<VoiceCommand>> {
        return voiceAvanueQueries.selectAll().asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { it.toSharedModel() }
            }
    }

    // Merge both streams
    fun observeAllCommands(): Flow<List<VoiceCommand>> {
        return combine(
            observeVOS4Commands(),
            observeAvanueCommands()
        ) { vos4, avanue ->
            mergeAndDeduplicate(vos4, avanue)
        }
    }

    private fun mergeAndDeduplicate(
        vos4: List<VoiceCommand>,
        avanue: List<VoiceCommand>
    ): List<VoiceCommand> {
        val merged = (vos4 + avanue).groupBy { it.id }
        return merged.map { (_, commands) ->
            // Prefer VOS4 version (it's the authoritative source)
            commands.firstOrNull { it in vos4 } ?: commands.first()
        }
    }
}
```

---

## 28.6 Communication Protocol

### 28.6.1 IPC Between VOS4 and VoiceAvanue

#### AIDL Interface Definition
```kotlin
// IVoiceCommandService.aidl
package com.augmentalis.voiceos

interface IVoiceCommandService {
    // Execute a voice command
    void executeCommand(String commandId, in Bundle parameters);

    // Register a new command
    boolean registerCommand(String commandId, String trigger, String action);

    // Get all registered commands
    List<String> getRegisteredCommands();

    // Enable/disable a command
    void setCommandEnabled(String commandId, boolean enabled);

    // Get command execution result
    Bundle getLastCommandResult();
}

// IVoiceCommandCallback.aidl
package com.augmentalis.voiceos

interface IVoiceCommandCallback {
    // Called when a command is executed
    void onCommandExecuted(String commandId, in Bundle result);

    // Called when a command fails
    void onCommandFailed(String commandId, String error);

    // Called when a new command is recognized
    void onCommandRecognized(String trigger, float confidence);
}
```

#### VOS4 Service Implementation
```kotlin
// VOS4: VoiceCommandService.kt
class VoiceCommandService : Service() {
    private val binder = object : IVoiceCommandService.Stub() {
        override fun executeCommand(commandId: String, parameters: Bundle) {
            // Execute command in VOS4
            commandManager.execute(commandId, parameters.toMap())
        }

        override fun registerCommand(
            commandId: String,
            trigger: String,
            action: String
        ): Boolean {
            return try {
                commandManager.register(
                    VoiceCommand(commandId, trigger, action)
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register command", e)
                false
            }
        }

        override fun getRegisteredCommands(): List<String> {
            return commandManager.getAllCommands().map { it.id }
        }

        override fun setCommandEnabled(commandId: String, enabled: Boolean) {
            commandManager.setEnabled(commandId, enabled)
        }

        override fun getLastCommandResult(): Bundle {
            return commandManager.getLastResult().toBundle()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}
```

#### VoiceAvanue Client
```kotlin
// VoiceAvanue: VOS4CommandClient.kt
class VOS4CommandClient(private val context: Context) {
    private var service: IVoiceCommandService? = null
    private val callback = object : IVoiceCommandCallback.Stub() {
        override fun onCommandExecuted(commandId: String, result: Bundle) {
            Log.d(TAG, "Command executed: $commandId")
            _commandResults.value = result.toMap()
        }

        override fun onCommandFailed(commandId: String, error: String) {
            Log.e(TAG, "Command failed: $commandId - $error")
            _commandErrors.value = error
        }

        override fun onCommandRecognized(trigger: String, confidence: Float) {
            Log.d(TAG, "Command recognized: $trigger (${confidence * 100}%)")
            _recognizedCommands.value = trigger to confidence
        }
    }

    private val _commandResults = MutableStateFlow<Map<String, Any>>(emptyMap())
    val commandResults: StateFlow<Map<String, Any>> = _commandResults

    private val _commandErrors = MutableStateFlow<String?>(null)
    val commandErrors: StateFlow<String?> = _commandErrors

    private val _recognizedCommands = MutableStateFlow<Pair<String, Float>?>(null)
    val recognizedCommands: StateFlow<Pair<String, Float>?> = _recognizedCommands

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IVoiceCommandService.Stub.asInterface(binder)
            Log.d(TAG, "Connected to VOS4 Voice Command Service")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            Log.d(TAG, "Disconnected from VOS4 Voice Command Service")
        }
    }

    fun connect() {
        val intent = Intent()
        intent.component = ComponentName(
            "com.augmentalis.voiceos",
            "com.augmentalis.voiceos.VoiceCommandService"
        )
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun disconnect() {
        context.unbindService(connection)
        service = null
    }

    suspend fun executeCommand(commandId: String, parameters: Map<String, Any> = emptyMap()) {
        service?.executeCommand(commandId, parameters.toBundle())
    }

    suspend fun registerCommand(commandId: String, trigger: String, action: String): Boolean {
        return service?.registerCommand(commandId, trigger, action) ?: false
    }

    suspend fun getRegisteredCommands(): List<String> {
        return service?.getRegisteredCommands() ?: emptyList()
    }
}
```

### 28.6.2 REST API Integration

```kotlin
// VOS4 exposes REST API for VoiceAvanue
class VOS4ApiServer(
    private val port: Int = 8080,
    private val commandManager: CommandManager
) {
    private val server = HttpServer(port)

    fun start() {
        server.apply {
            // Voice commands
            post("/api/commands/execute") { request ->
                val commandId = request.body["commandId"] as String
                val parameters = request.body["parameters"] as? Map<String, Any> ?: emptyMap()

                val result = commandManager.execute(commandId, parameters)

                HttpResponse(
                    status = 200,
                    body = mapOf(
                        "success" to true,
                        "result" to result
                    )
                )
            }

            get("/api/commands") { request ->
                val commands = commandManager.getAllCommands()

                HttpResponse(
                    status = 200,
                    body = mapOf(
                        "commands" to commands.map { it.toJson() }
                    )
                )
            }

            post("/api/commands/register") { request ->
                val commandId = request.body["commandId"] as String
                val trigger = request.body["trigger"] as String
                val action = request.body["action"] as String

                val success = commandManager.register(
                    VoiceCommand(commandId, trigger, action)
                )

                HttpResponse(
                    status = if (success) 201 else 400,
                    body = mapOf("success" to success)
                )
            }

            // Accessibility tree
            get("/api/ui/tree") { request ->
                val tree = accessibilityScrapingEngine.getCurrentUITree()

                HttpResponse(
                    status = 200,
                    body = mapOf(
                        "tree" to tree.toJson()
                    )
                )
            }

            // Screen context
            get("/api/ui/context") { request ->
                val context = screenContextInferenceEngine.getCurrentContext()

                HttpResponse(
                    status = 200,
                    body = mapOf(
                        "context" to context.toJson()
                    )
                )
            }
        }.start()
    }

    fun stop() {
        server.stop()
    }
}
```

### 28.6.3 WebSocket Real-Time Updates

```kotlin
// Real-time UI updates via WebSocket
class VOS4WebSocketServer(
    private val port: Int = 8081,
    private val accessibilityService: VoiceOSService
) {
    private val server = WebSocketServer(port)
    private val clients = mutableSetOf<WebSocketConnection>()

    fun start() {
        server.apply {
            onConnect { connection ->
                clients.add(connection)
                Log.d(TAG, "Client connected: ${connection.id}")
            }

            onDisconnect { connection ->
                clients.remove(connection)
                Log.d(TAG, "Client disconnected: ${connection.id}")
            }

            onMessage { connection, message ->
                when (message.type) {
                    "subscribe_ui_updates" -> {
                        subscribeToUIUpdates(connection)
                    }
                    "subscribe_commands" -> {
                        subscribeToCommands(connection)
                    }
                    "unsubscribe" -> {
                        unsubscribe(connection)
                    }
                }
            }
        }.start()

        // Start broadcasting UI updates
        startUIUpdateBroadcast()
    }

    private fun subscribeToUIUpdates(connection: WebSocketConnection) {
        accessibilityService.uiUpdates.onEach { update ->
            connection.send(
                WebSocketMessage(
                    type = "ui_update",
                    data = update.toJson()
                )
            )
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun subscribeToCommands(connection: WebSocketConnection) {
        accessibilityService.commandExecutions.onEach { execution ->
            connection.send(
                WebSocketMessage(
                    type = "command_executed",
                    data = execution.toJson()
                )
            )
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun startUIUpdateBroadcast() {
        accessibilityService.uiUpdates.onEach { update ->
            broadcast(
                WebSocketMessage(
                    type = "ui_update",
                    data = update.toJson()
                )
            )
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    private fun broadcast(message: WebSocketMessage) {
        clients.forEach { client ->
            client.send(message)
        }
    }
}
```

---

## 28.7 Deployment Strategy

### 28.7.1 Standalone Deployment

**Option 1: VOS4 Only**
```
VOS4.apk (80MB)
├── VoiceOSCore module
├── VoiceUI module
├── SpeechRecognition library
├── CommandManager
└── All dependencies
```

**Use Case:** Users who only need VOS4 voice accessibility features

**Option 2: VoiceAvanue Only**
```
VoiceAvanue.apk (50MB)
├── IDEAMagic Runtime
├── MagicUI Foundation
├── AvanueLaunch app
└── Cross-platform components
```

**Use Case:** Users who want Avanue platform apps without VOS4

### 28.7.2 Integrated Deployment

**Option 3: Combined APK**
```
VOS4-VoiceAvanue.apk (100MB)
├── VOS4 modules
│   ├── VoiceOSCore
│   ├── VoiceUI
│   ├── SpeechRecognition
│   └── Managers
├── VoiceAvanue modules
│   ├── IDEAMagic Runtime
│   ├── MagicUI Foundation
│   ├── MagicCode CLI
│   └── Apps
└── Shared dependencies
    ├── Kotlin stdlib
    ├── Coroutines
    ├── Compose runtime
    └── Material 3
```

**Use Case:** Full-featured voice-enabled platform

**Build Configuration:**
```kotlin
// settings.gradle.kts
include(":app") // Main launcher

// VOS4 modules
include(":modules:apps:VoiceOSCore")
include(":modules:apps:VoiceUI")
include(":modules:libraries:SpeechRecognition")
include(":modules:managers:CommandManager")

// VoiceAvanue modules
include(":voiceavanue:magicui:runtime")
include(":voiceavanue:magicui:foundation")
include(":voiceavanue:magiccode:compiler")
include(":voiceavanue:apps:avanuelaunch")

// app/build.gradle.kts
dependencies {
    // VOS4
    implementation(project(":modules:apps:VoiceOSCore"))
    implementation(project(":modules:apps:VoiceUI"))

    // VoiceAvanue
    implementation(project(":voiceavanue:magicui:runtime"))
    implementation(project(":voiceavanue:magicui:foundation"))

    // Shared
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
}
```

### 28.7.3 Module Federation (Advanced)

**Dynamic Feature Modules** (Android App Bundles)

```kotlin
// Base APK (10MB) - Core functionality only
// Dynamic features downloaded on-demand:

// Feature 1: VOS4 Core (40MB)
:features:vos4-core
├── VoiceOSCore
└── Basic voice commands

// Feature 2: Advanced Voice (30MB)
:features:vos4-advanced
├── VoiceUI
├── LearnApp
└── Advanced commands

// Feature 3: VoiceAvanue UI (20MB)
:features:voiceavanue-ui
├── MagicUI Foundation
└── DesignSystem

// Feature 4: Avanue Apps (40MB)
:features:avanue-apps
├── VoiceAvanue
├── AIAvanue
├── BrowserAvanue
└── NoteAvanue
```

**Configuration:**
```kotlin
// app/build.gradle.kts
android {
    dynamicFeatures = setOf(
        ":features:vos4-core",
        ":features:vos4-advanced",
        ":features:voiceavanue-ui",
        ":features:avanue-apps"
    )
}

// features/vos4-core/build.gradle.kts
plugins {
    id("com.android.dynamic-feature")
}

android {
    namespace = "com.augmentalis.voiceos.feature.core"
}

dependencies {
    implementation(project(":app"))
    implementation(project(":modules:apps:VoiceOSCore"))
}

// Runtime installation
class FeatureInstaller(private val context: Context) {
    private val splitInstallManager = SplitInstallManagerFactory.create(context)

    fun installVOS4Core(onComplete: (Boolean) -> Unit) {
        val request = SplitInstallRequest.newBuilder()
            .addModule("vos4-core")
            .build()

        splitInstallManager.startInstall(request)
            .addOnSuccessListener { sessionId ->
                Log.d(TAG, "Install started: $sessionId")
                monitorInstall(sessionId, onComplete)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Install failed", exception)
                onComplete(false)
            }
    }

    private fun monitorInstall(sessionId: Int, onComplete: (Boolean) -> Unit) {
        splitInstallManager.registerListener { state ->
            when (state.status()) {
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(TAG, "Module installed successfully")
                    onComplete(true)
                }
                SplitInstallSessionStatus.FAILED -> {
                    Log.e(TAG, "Module installation failed")
                    onComplete(false)
                }
            }
        }
    }
}
```

---

## 28.8 Implementation Guide

### 28.8.1 Step-by-Step Integration

#### Step 1: Add Dependencies
```kotlin
// VOS4 app/build.gradle.kts
repositories {
    // Add VoiceAvanue Maven repository
    maven {
        url = uri("https://maven.augmentalis.com/voiceavanue")
        credentials {
            username = project.findProperty("voiceavanue.username") as String?
            password = project.findProperty("voiceavanue.password") as String?
        }
    }
}

dependencies {
    // Add VoiceAvanue dependencies
    implementation("com.augmentalis:magicui-foundation:1.0.0")
    implementation("com.augmentalis:magicui-designsystem:1.0.0")
    implementation("com.augmentalis:magicui-state:1.0.0")

    // Optional: Code generation
    implementation("com.augmentalis:magiccode-runtime:1.0.0")
}
```

#### Step 2: Initialize MagicUI
```kotlin
// VOS4 Application.kt
class VOS4Application : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize MagicUI
        MagicUI.init(
            context = this,
            config = MagicUIConfig(
                enableLogging = BuildConfig.DEBUG,
                enableMetrics = true,
                theme = MagicUITheme.MATERIAL_YOU
            )
        )
    }
}
```

#### Step 3: Use MagicUI Components
```kotlin
// VOS4 MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MagicTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true
            ) {
                VOS4App()
            }
        }
    }
}

@Composable
fun VOS4App() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            MagicTopAppBar(
                title = "VOS4",
                navigationIcon = {
                    MagicIconButton(
                        icon = Icons.Default.Menu,
                        onClick = { /* open drawer */ }
                    )
                }
            )
        },
        bottomBar = {
            MagicBottomNavigation(
                items = listOf(
                    BottomNavItem("Home", Icons.Default.Home, "home"),
                    BottomNavItem("Commands", Icons.Default.Mic, "commands"),
                    BottomNavItem("Settings", Icons.Default.Settings, "settings")
                ),
                selectedItem = navController.currentDestination?.route ?: "home",
                onItemSelected = { route ->
                    navController.navigate(route)
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen() }
            composable("commands") { CommandsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
```

#### Step 4: Implement Screens with MagicUI
```kotlin
@Composable
fun HomeScreen() {
    val viewModel: HomeViewModel = hiltViewModel()

    MagicScrollColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.Medium)
    ) {
        // Header
        MagicHeadline("Welcome to VOS4")
        Spacer(Modifier.height(DesignTokens.Spacing.Small))
        MagicBody("Voice-enabled operating system")

        Spacer(Modifier.height(DesignTokens.Spacing.ExtraLarge))

        // Quick Actions
        MagicCard(variant = CardVariant.Elevated) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Quick Actions")
                Spacer(Modifier.height(DesignTokens.Spacing.Medium))

                MagicPrimaryButton(
                    text = "Start Voice Control",
                    onClick = { viewModel.startVoiceControl() },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        MagicIcon(Icons.Default.Mic)
                    }
                )

                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                MagicSecondaryButton(
                    text = "View Commands",
                    onClick = { viewModel.openCommands() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.Large))

        // Recent Commands
        MagicCard(variant = CardVariant.Outlined) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Recent Commands")
                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                viewModel.recentCommands.value.forEach { command ->
                    MagicListItem(
                        title = command.trigger,
                        subtitle = command.timestamp.formatRelative(),
                        preset = ListItemPreset.TwoLine,
                        onClick = { viewModel.reExecuteCommand(command) }
                    )

                    MagicDivider()
                }
            }
        }
    }
}
```

#### Step 5: Integrate with VOS4 Services
```kotlin
// Bridge MagicUI with VOS4 services
class VOS4MagicUIBridge(
    private val voiceOSService: VoiceOSService,
    private val commandManager: CommandManager
) {
    // Expose VOS4 state as MagicState
    val isListening = MutableMagicState(false)
    val currentCommand = MutableMagicState("")
    val recognizedText = MutableMagicState("")

    init {
        // Observe VOS4 service state
        voiceOSService.isListening.onEach { listening ->
            isListening.update(listening)
        }.launchIn(serviceScope)

        voiceOSService.recognizedText.onEach { text ->
            recognizedText.update(text)
        }.launchIn(serviceScope)

        // Observe command executions
        commandManager.lastExecutedCommand.onEach { command ->
            currentCommand.update(command.trigger)
        }.launchIn(serviceScope)
    }

    // Bridge functions
    fun startListening() {
        voiceOSService.startListening()
    }

    fun stopListening() {
        voiceOSService.stopListening()
    }

    fun executeCommand(commandId: String, parameters: Map<String, Any> = emptyMap()) {
        commandManager.execute(commandId, parameters)
    }
}

// Use in UI
@Composable
fun VoiceControlScreen(bridge: VOS4MagicUIBridge) {
    MagicColumn {
        MagicText(
            text = if (bridge.isListening.value) "Listening..." else "Ready",
            preset = TextPreset.HeadlineLarge
        )

        MagicTextField(
            value = bridge.recognizedText,
            label = "Recognized Text",
            readOnly = true
        )

        MagicButton(
            text = if (bridge.isListening.value) "Stop" else "Start",
            onClick = {
                if (bridge.isListening.value) {
                    bridge.stopListening()
                } else {
                    bridge.startListening()
                }
            }
        )
    }
}
```

### 28.8.2 Migration Guide

#### Migrating Existing VOS4 UI to MagicUI

**Before (Traditional Compose):**
```kotlin
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Voice Engine",
                    style = MaterialTheme.typography.titleMedium
                )

                var selectedEngine by remember { mutableStateOf("Google") }

                listOf("Google", "Vivoka", "Vosk").forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEngine = engine },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEngine == engine,
                            onClick = { selectedEngine = engine }
                        )
                        Text(engine)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { /* save */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}
```

**After (MagicUI):**
```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    MagicScrollColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.Medium)
    ) {
        MagicHeadline("Settings")
        Spacer(Modifier.height(DesignTokens.Spacing.Medium))

        MagicCard(variant = CardVariant.Filled) {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.Medium)) {
                MagicTitle("Voice Engine")
                Spacer(Modifier.height(DesignTokens.Spacing.Small))

                MagicRadioGroup(
                    options = listOf("Google", "Vivoka", "Vosk"),
                    selected = viewModel.selectedEngine,
                    onSelectionChange = { viewModel.selectEngine(it) }
                )
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.Medium))

        MagicPrimaryButton(
            text = "Save",
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

**Benefits:**
- **Shorter code:** ~40% reduction in lines
- **Type-safe:** Design tokens prevent magic numbers
- **Reactive:** MagicState handles state management
- **Consistent:** Follows Material 3 guidelines
- **Cross-platform ready:** Can target iOS/Web later

---

## 28.9 Best Practices

### 28.9.1 Design System Usage

**DO:**
```kotlin
// Use design tokens
MagicColumn(
    modifier = Modifier.padding(DesignTokens.Spacing.Medium)
) {
    MagicText(
        text = "Title",
        preset = TextPreset.HeadlineLarge,
        color = MagicColor(DesignTokens.Colors.Primary.value)
    )
}
```

**DON'T:**
```kotlin
// Avoid magic numbers
Column(
    modifier = Modifier.padding(16.dp)
) {
    Text(
        text = "Title",
        fontSize = 32.sp,
        color = Color(0xFF6200EE)
    )
}
```

### 28.9.2 State Management

**DO:**
```kotlin
class MyViewModel : ViewModel() {
    val userInput = MutableMagicState("")
    val isValid = DerivedMagicState(
        dependencies = listOf(userInput),
        computation = { userInput.value.length >= 3 }
    )
}

@Composable
fun MyScreen(viewModel: MyViewModel) {
    MagicTextField(
        value = viewModel.userInput,
        isError = !viewModel.isValid.value
    )
}
```

**DON'T:**
```kotlin
@Composable
fun MyScreen() {
    var userInput by remember { mutableStateOf("") }
    val isValid = userInput.length >= 3

    OutlinedTextField(
        value = userInput,
        onValueChange = { userInput = it },
        isError = !isValid
    )
}
```

### 28.9.3 Component Composition

**DO:**
```kotlin
@Composable
fun UserProfileCard(user: User) {
    MagicCard(variant = CardVariant.Elevated) {
        MagicRow(
            modifier = Modifier.padding(DesignTokens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MagicImage(
                source = user.avatarUrl,
                preset = ImagePreset.Avatar,
                size = MagicSize(MagicDp(48f), MagicDp(48f))
            )

            Spacer(Modifier.width(DesignTokens.Spacing.Medium))

            MagicColumn {
                MagicText(
                    text = user.name,
                    preset = TextPreset.TitleMedium
                )
                MagicText(
                    text = user.email,
                    preset = TextPreset.BodySmall,
                    color = MagicColor(DesignTokens.Colors.OnSurfaceVariant.value)
                )
            }
        }
    }
}
```

**DON'T:**
```kotlin
@Composable
fun UserProfileCard(user: User) {
    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### 28.9.4 Performance Optimization

**DO:**
```kotlin
@Composable
fun CommandList(commands: List<VoiceCommand>) {
    val commandState = rememberMagicState(commands)

    LazyColumn {
        items(
            items = commandState.value,
            key = { it.id } // Stable keys for recomposition
        ) { command ->
            CommandListItem(command)
        }
    }
}

@Composable
fun CommandListItem(command: VoiceCommand) {
    // Stable, will not recompose unless command changes
    MagicListCard(
        title = command.trigger,
        subtitle = command.description,
        onClick = { /* handle */ }
    )
}
```

**DON'T:**
```kotlin
@Composable
fun CommandList(commands: List<VoiceCommand>) {
    LazyColumn {
        items(commands.size) { index ->
            val command = commands[index] // Unstable
            MagicListCard(
                title = command.trigger,
                subtitle = command.description,
                onClick = { /* handle */ }
            )
        }
    }
}
```

---

## 28.10 Troubleshooting

### 28.10.1 Common Issues

#### Issue: MagicUI components not rendering
```
Error: java.lang.NoClassDefFoundError:
  com.augmentalis.magicui.foundation.MagicButtonKt
```

**Solution:**
```kotlin
// Verify dependency is added
dependencies {
    implementation("com.augmentalis:magicui-foundation:1.0.0")
}

// Check ProGuard rules
-keep class com.augmentalis.magicui.** { *; }
-keepclassmembers class com.augmentalis.magicui.** { *; }
```

#### Issue: Theme colors not applying
```
// Colors are not using Material You dynamic colors
```

**Solution:**
```kotlin
MagicTheme(
    darkTheme = isSystemInDarkTheme(),
    dynamicColor = true, // Enable dynamic colors
    content = content
)

// Verify Android 12+ (API 31+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // Dynamic colors supported
}
```

#### Issue: State not updating
```
// MagicTextField value not updating when typed
```

**Solution:**
```kotlin
// Ensure using MutableMagicState, not read-only MagicState
val textValue = MutableMagicState("") // ✅ Correct

val textValue = object : MagicState<String> {
    override val value = ""
    override fun update(newValue: String) {} // ❌ No-op
    override fun observe(observer: (String) -> Unit) {}
}

// Use in UI
MagicTextField(
    value = textValue, // Will update correctly
    label = "Enter text"
)
```

### 28.10.2 Debugging Tips

#### Enable MagicUI Logging
```kotlin
MagicUI.init(
    context = this,
    config = MagicUIConfig(
        enableLogging = true, // Enable verbose logging
        logLevel = LogLevel.DEBUG
    )
)
```

#### Inspect Component Hierarchy
```kotlin
@Composable
fun DebugComponentTree(component: Component) {
    val inspector = rememberComponentInspector()

    DisposableEffect(component) {
        inspector.inspect(component)
        Log.d(TAG, "Component tree:\n${inspector.getTreeString()}")
        onDispose { }
    }
}
```

#### Monitor State Changes
```kotlin
val textValue = MutableMagicState("")

textValue.observe { newValue ->
    Log.d(TAG, "Text changed: $newValue")
}
```

### 28.10.3 Performance Profiling

```kotlin
// Enable metrics collection
MagicUI.init(
    context = this,
    config = MagicUIConfig(
        enableMetrics = true
    )
)

// Collect metrics
val metrics = MagicUI.getMetrics()
Log.d(TAG, """
    MagicUI Performance:
    - Total components rendered: ${metrics.totalComponents}
    - Average render time: ${metrics.avgRenderTime}ms
    - Recompositions: ${metrics.recompositions}
    - Skipped frames: ${metrics.skippedFrames}
""".trimIndent())
```

---

## Summary

VoiceAvanue integration with VOS4 provides:

1. **Powerful UI Framework:** 15 production-ready components + 32 platform-agnostic definitions
2. **Cross-Platform Ready:** Android (100%), Desktop (100%), iOS (60%), Web (planned)
3. **Three Integration Levels:** Direct components, Core abstraction, MagicCode DSL
4. **Material 3 Design System:** Complete design tokens and theming
5. **Reactive State Management:** Type-safe, composable state
6. **Bidirectional Communication:** AIDL, REST API, WebSocket support
7. **Flexible Deployment:** Standalone, integrated, or modular

**Next Steps:**
- [Chapter 29: MagicUI Integration](29-MagicUI-Integration.md) - Deep dive into runtime system
- [Chapter 30: MagicCode Integration](30-MagicCode-Integration.md) - Code generation pipeline
- [Chapter 31: AVA & AVAConnect Integration](31-AVA-AVAConnect-Integration.md) - Network capabilities

---

**Document Metadata:**
- **Created:** 2025-11-02
- **Framework:** IDEACODE v5.3
- **Total Pages:** 58
- **Code Examples:** 45+
- **Architecture Diagrams:** 4


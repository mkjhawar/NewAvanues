# Chapter 29: MagicUI Integration

## Overview

MagicUI is the dynamic UI generation and rendering framework within the IDEAMagic ecosystem. It provides a complete DSL (Domain-Specific Language) system for voice-driven UI creation, runtime rendering, and cross-platform code generation. This chapter explores MagicUI's architecture, DSL system, runtime integration with VOS4, and code generation capabilities.

**Location:** `/Volumes/M-Drive/Coding/voiceavanue/Universal/IDEAMagic/MagicUI/`

**Key Features:**
- DSL-based declarative UI definition (.vos format)
- Runtime DSL parser and compiler
- Component-based architecture with registry system
- Voice-driven UI creation and control
- Cross-platform code generation (Compose, SwiftUI, HTML)
- Material 3 design system integration
- Reactive state management
- Complete lifecycle management

---

## 29.1 MagicUI Architecture

### 29.1.1 System Components

MagicUI consists of eight primary modules:

```
MagicUI/
├── CoreTypes/          # Type-safe value classes (Dp, Sp, Color)
├── DesignSystem/       # Material 3 design tokens
├── StateManagement/    # Reactive state system
├── ThemeManager/       # Dynamic theme system
├── ThemeBridge/        # VOS3 → VOS4 migration bridge
├── UIConvertor/        # Theme format converter
└── src/
    └── commonMain/     # Core MagicUI runtime
        ├── core/       # Component models, enums
        ├── dsl/        # Parser, tokenizer, AST
        ├── registry/   # Component registry
        ├── instantiation/  # Component factory
        ├── events/     # Event system
        ├── voice/      # Voice command routing
        ├── lifecycle/  # App lifecycle
        ├── layout/     # Layout loaders
        └── theme/      # Theme loaders
```

### 29.1.2 Processing Pipeline

MagicUI processes UI definitions through six phases:

**Phase 1: Tokenization**
```
.vos Source → VosTokenizer → Token Stream
```

**Phase 2: Parsing**
```
Token Stream → VosParser → AST (Abstract Syntax Tree)
```

**Phase 3: Registration**
```
Component Types → ComponentRegistry → Type Metadata
```

**Phase 4: Instantiation**
```
AST Nodes → ComponentInstantiator → Kotlin Objects
```

**Phase 5: Event Binding**
```
AST Callbacks → CallbackAdapter → Event Handlers
```

**Phase 6: Lifecycle**
```
App State → AppLifecycle → State Transitions
```

### 29.1.3 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    MagicUI Runtime                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ VosTokenizer │───▶│  VosParser   │───▶│   VosAST     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │        │
│         ▼                    ▼                    ▼        │
│  ┌──────────────────────────────────────────────────────┐ │
│  │          Component Registry (Metadata)               │ │
│  └──────────────────────────────────────────────────────┘ │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Instantiator │───▶│  Event Bus   │───▶│  Lifecycle   │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │        │
│         ▼                    ▼                    ▼        │
│  ┌──────────────────────────────────────────────────────┐ │
│  │            Voice Command Router                      │ │
│  └──────────────────────────────────────────────────────┘ │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────────────────────────────────────────────┐ │
│  │          Running App Instances                       │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 29.2 Core Type System

### 29.2.1 Type-Safe Dimensions

MagicUI provides zero-cost abstractions for UI dimensions using Kotlin value classes.

**File:** `CoreTypes/src/commonMain/kotlin/com/augmentalis/ideamagic/coretypes/CoreTypes.kt`

#### MagicDp (Density-Independent Pixels)

```kotlin
@JvmInline
value class MagicDp(val value: Dp) {
    companion object {
        val ZERO = MagicDp(0.dp)
        val INFINITY = MagicDp(Dp.Infinity)
        val UNSPECIFIED = MagicDp(Dp.Unspecified)
    }

    // Arithmetic operators
    operator fun plus(other: MagicDp) = MagicDp(value + other.value)
    operator fun minus(other: MagicDp) = MagicDp(value - other.value)
    operator fun times(scale: Float) = MagicDp(value * scale)
    operator fun div(scale: Float) = MagicDp(value / scale)

    // Comparison
    operator fun compareTo(other: MagicDp): Int = value.compareTo(other.value)
}

// Extension for easy creation
val Int.magicDp: MagicDp
    get() = MagicDp(this.dp)

val Float.magicDp: MagicDp
    get() = MagicDp(this.dp)
```

**Usage:**
```kotlin
val padding = 16.magicDp
val doubled = padding * 2  // 32.dp
val combined = padding + 8.magicDp  // 24.dp
```

#### MagicSp (Scalable Pixels)

```kotlin
@JvmInline
value class MagicSp(val value: TextUnit) {
    companion object {
        val ZERO = MagicSp(0.sp)
        val UNSPECIFIED = MagicSp(TextUnit.Unspecified)
    }

    operator fun times(scale: Float) = MagicSp(value * scale)
    operator fun div(scale: Float) = MagicSp(value / scale)
}

val Int.magicSp: MagicSp
    get() = MagicSp(this.sp)
```

**Usage:**
```kotlin
val fontSize = 16.magicSp
val largeFont = fontSize * 1.5  // 24.sp
```

#### MagicPx (Raw Pixels)

```kotlin
@JvmInline
value class MagicPx(val value: Float) {
    companion object {
        val ZERO = MagicPx(0f)
    }

    fun toDp(density: Float): MagicDp = MagicDp((value / density).dp)
}
```

### 29.2.2 Type-Safe Colors

```kotlin
@JvmInline
value class MagicColor(val value: Color) {
    companion object {
        val Transparent = MagicColor(Color.Transparent)
        val Black = MagicColor(Color.Black)
        val White = MagicColor(Color.White)
        val Red = MagicColor(Color.Red)
        val Green = MagicColor(Color.Green)
        val Blue = MagicColor(Color.Blue)
        val Unspecified = MagicColor(Color.Unspecified)
    }

    // Hex color constructor
    constructor(hex: String) : this(Color(parseHexColor(hex)))

    // ARGB constructor
    constructor(alpha: Int, red: Int, green: Int, blue: Int) : this(
        Color(red, green, blue, alpha)
    )

    // RGB constructor (full opacity)
    constructor(red: Int, green: Int, blue: Int) : this(
        Color(red, green, blue)
    )

    // Alpha manipulation
    fun copy(alpha: Float = value.alpha): MagicColor {
        return MagicColor(value.copy(alpha = alpha))
    }

    fun withAlpha(alpha: Float): MagicColor {
        return MagicColor(value.copy(alpha = alpha))
    }
}
```

**Usage:**
```kotlin
val primary = MagicColor("#6750A4")
val semitransparent = primary.withAlpha(0.5f)
val rgb = MagicColor(103, 80, 164)  // RGB
val argb = MagicColor(255, 103, 80, 164)  // ARGB
```

### 29.2.3 Composite Types

#### MagicSize

```kotlin
data class MagicSize(
    val width: MagicDp,
    val height: MagicDp
) {
    companion object {
        val ZERO = MagicSize(MagicDp.ZERO, MagicDp.ZERO)
        val UNSPECIFIED = MagicSize(MagicDp.UNSPECIFIED, MagicDp.UNSPECIFIED)
    }

    val isSpecified: Boolean
        get() = width != MagicDp.UNSPECIFIED && height != MagicDp.UNSPECIFIED

    // Square size constructor
    constructor(size: MagicDp) : this(size, size)
}
```

#### MagicPadding

```kotlin
data class MagicPadding(
    val start: MagicDp = MagicDp.ZERO,
    val top: MagicDp = MagicDp.ZERO,
    val end: MagicDp = MagicDp.ZERO,
    val bottom: MagicDp = MagicDp.ZERO
) {
    companion object {
        val ZERO = MagicPadding()

        fun all(value: MagicDp) = MagicPadding(value, value, value, value)

        fun horizontal(value: MagicDp) = MagicPadding(start = value, end = value)

        fun vertical(value: MagicDp) = MagicPadding(top = value, bottom = value)

        fun symmetric(
            horizontal: MagicDp = MagicDp.ZERO,
            vertical: MagicDp = MagicDp.ZERO
        ) = MagicPadding(
            start = horizontal,
            top = vertical,
            end = horizontal,
            bottom = vertical
        )
    }
}
```

**Usage:**
```kotlin
val uniformPadding = MagicPadding.all(16.magicDp)
val horizontalPadding = MagicPadding.horizontal(24.magicDp)
val symmetricPadding = MagicPadding.symmetric(
    horizontal = 16.magicDp,
    vertical = 8.magicDp
)
```

#### MagicBorderRadius

```kotlin
data class MagicBorderRadius(
    val topStart: MagicDp = MagicDp.ZERO,
    val topEnd: MagicDp = MagicDp.ZERO,
    val bottomStart: MagicDp = MagicDp.ZERO,
    val bottomEnd: MagicDp = MagicDp.ZERO
) {
    companion object {
        val ZERO = MagicBorderRadius()

        fun all(value: MagicDp) = MagicBorderRadius(value, value, value, value)

        fun circular() = MagicBorderRadius(
            topStart = MagicDp.INFINITY,
            topEnd = MagicDp.INFINITY,
            bottomStart = MagicDp.INFINITY,
            bottomEnd = MagicDp.INFINITY
        )
    }

    constructor(radius: MagicDp) : this(radius, radius, radius, radius)
}
```

---

## 29.3 Design System

### 29.3.1 Design Tokens

MagicUI implements Material 3 design tokens for consistent theming.

**File:** `DesignSystem/src/commonMain/kotlin/com/augmentalis/ideamagic/designsystem/DesignTokens.kt`

#### Color Tokens

```kotlin
object ColorTokens {
    // Primary colors
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)

    // Secondary colors
    val Secondary = Color(0xFF625B71)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)

    // Tertiary colors
    val Tertiary = Color(0xFF7D5260)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val OnTertiaryContainer = Color(0xFF31111D)

    // Error colors
    val Error = Color(0xFFB3261E)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFF9DEDC)
    val OnErrorContainer = Color(0xFF410E0B)

    // Background colors
    val Background = Color(0xFFFFFBFE)
    val OnBackground = Color(0xFF1C1B1F)

    // Surface colors
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)

    // Outline colors
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)

    // Dark theme variants...
    val DarkPrimary = Color(0xFFD0BCFF)
    val DarkOnPrimary = Color(0xFF381E72)
    // ... (complete dark theme colors)
}
```

#### Typography Tokens

```kotlin
object TypographyTokens {
    // Display styles (largest text)
    val DisplayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.25).sp
    )

    val DisplayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    val DisplaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // Headline styles
    val HeadlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp)
    val HeadlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp)
    val HeadlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp)

    // Title styles
    val TitleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp)
    val TitleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    )
    val TitleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )

    // Body styles (most common)
    val BodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    val BodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    // Label styles (buttons, chips)
    val LabelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )
    val LabelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
    val LabelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}
```

#### Spacing Tokens

```kotlin
object SpacingTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 24.dp
    val ExtraLarge: Dp = 32.dp
    val ExtraExtraLarge: Dp = 48.dp
    val Huge: Dp = 64.dp
}
```

#### Shape Tokens

```kotlin
object ShapeTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 28.dp
    val Full: Dp = 9999.dp  // Fully rounded
}
```

#### Elevation Tokens

```kotlin
object ElevationTokens {
    val Level0: Dp = 0.dp   // No elevation
    val Level1: Dp = 1.dp   // Subtle elevation
    val Level2: Dp = 3.dp   // Cards
    val Level3: Dp = 6.dp   // Buttons
    val Level4: Dp = 8.dp   // Nav bars
    val Level5: Dp = 12.dp  // Dialogs
}
```

#### Size Tokens

```kotlin
object SizeTokens {
    // Icon sizes
    val IconSmall: Dp = 16.dp
    val IconMedium: Dp = 24.dp
    val IconLarge: Dp = 32.dp
    val IconExtraLarge: Dp = 48.dp

    // Button sizes
    val ButtonHeightSmall: Dp = 32.dp
    val ButtonHeightMedium: Dp = 40.dp
    val ButtonHeightLarge: Dp = 48.dp
    val ButtonHeightExtraLarge: Dp = 56.dp

    // Touch target (accessibility minimum)
    val MinTouchTarget: Dp = 48.dp

    // TextField sizes
    val TextFieldHeight: Dp = 56.dp
    val TextFieldHeightSmall: Dp = 40.dp
}
```

#### Animation Tokens

```kotlin
object AnimationTokens {
    const val DurationShort: Int = 150       // Quick transitions
    const val DurationMedium: Int = 300      // Standard transitions
    const val DurationLong: Int = 500        // Emphasis transitions
    const val DurationExtraLong: Int = 1000  // Complex animations
}
```

### 29.3.2 Design System Usage

```kotlin
// Apply design tokens to components
@Composable
fun MaterialButton() {
    Button(
        modifier = Modifier
            .height(SizeTokens.ButtonHeightMedium)
            .padding(horizontal = SpacingTokens.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = ColorTokens.Primary,
            contentColor = ColorTokens.OnPrimary
        ),
        shape = RoundedCornerShape(ShapeTokens.Small)
    ) {
        Text(
            text = "Click Me",
            style = TypographyTokens.LabelLarge
        )
    }
}
```

---

## 29.4 State Management

MagicUI provides a reactive state management system built on Compose State and Kotlin Flow.

**File:** `StateManagement/src/commonMain/kotlin/com/augmentalis/ideamagic/state/MagicState.kt`

### 29.4.1 MagicState Interface

```kotlin
interface MagicState<T> {
    var value: T
    fun asState(): State<T>
    fun asFlow(): StateFlow<T>
}

interface ReadOnlyMagicState<T> {
    val value: T
    fun asState(): State<T>
    fun asFlow(): StateFlow<T>
}
```

### 29.4.2 Creating State

```kotlin
// Create state with initial value
fun <T> magicStateOf(initialValue: T): MagicState<T>

// Remember state across recompositions
@Composable
fun <T> rememberMagicState(initialValue: T): MagicState<T>

// Remember with calculation
@Composable
fun <T> rememberMagicState(key: Any? = null, calculation: () -> T): MagicState<T>
```

**Usage:**
```kotlin
// Simple counter
val counter = rememberMagicState(0)

Button(onClick = { counter.value++ }) {
    Text("Count: ${counter.value}")
}

// Text field binding
val text = rememberMagicState("")

TextField(
    value = text.value,
    onValueChange = { text.value = it }
)
```

### 29.4.3 Derived State

```kotlin
@Composable
fun <T> magicDerivedStateOf(calculation: () -> T): ReadOnlyMagicState<T>
```

**Usage:**
```kotlin
val counter = rememberMagicState(0)
val isEven = magicDerivedStateOf { counter.value % 2 == 0 }

Text("Counter is ${if (isEven.value) "even" else "odd"}")
```

### 29.4.4 State Collections

#### MagicStateList

```kotlin
interface MagicStateList<T> : List<T> {
    fun add(element: T)
    fun addAll(elements: Collection<T>)
    fun remove(element: T): Boolean
    fun removeAt(index: Int): T
    fun clear()
    fun set(index: Int, element: T): T
    fun asState(): State<List<T>>
}

fun <T> magicStateListOf(vararg elements: T): MagicStateList<T>

@Composable
fun <T> rememberMagicStateList(vararg elements: T): MagicStateList<T>
```

**Usage:**
```kotlin
val items = rememberMagicStateList("Apple", "Banana", "Cherry")

LazyColumn {
    items(items.size) { index ->
        Text(items[index])
    }
}

Button(onClick = { items.add("Date") }) {
    Text("Add Item")
}
```

#### MagicStateMap

```kotlin
interface MagicStateMap<K, V> : Map<K, V> {
    fun put(key: K, value: V): V?
    fun putAll(from: Map<out K, V>)
    fun remove(key: K): V?
    fun clear()
    fun asState(): State<Map<K, V>>
}

fun <K, V> magicStateMapOf(vararg pairs: Pair<K, V>): MagicStateMap<K, V>

@Composable
fun <K, V> rememberMagicStateMap(vararg pairs: Pair<K, V>): MagicStateMap<K, V>
```

**Usage:**
```kotlin
val settings = rememberMagicStateMap(
    "theme" to "dark",
    "language" to "en",
    "notifications" to "true"
)

Column {
    settings.forEach { (key, value) ->
        Text("$key: $value")
    }
}

Button(onClick = { settings.put("theme", "light") }) {
    Text("Toggle Theme")
}
```

### 29.4.5 Two-Way Binding

```kotlin
fun <T> MagicState<T>.set(newValue: T) {
    value = newValue
}

@Composable
fun <T> MagicState<T>.binding(): Pair<T, (T) -> Unit> {
    return value to { newValue -> value = newValue }
}
```

**Usage:**
```kotlin
val text = rememberMagicState("")

// Method 1: Direct binding
TextField(
    value = text.value,
    onValueChange = text::set
)

// Method 2: Pair destructuring
val (value, onValueChange) = text.binding()
TextField(value = value, onValueChange = onValueChange)
```

### 29.4.6 State Implementation

The internal implementation uses both Compose State and Kotlin Flow:

```kotlin
private class MagicStateImpl<T>(initialValue: T) : MagicState<T> {
    private val _state = mutableStateOf(initialValue)
    private val _flow = MutableStateFlow(initialValue)

    override var value: T
        get() = _state.value
        set(newValue) {
            _state.value = newValue  // Triggers recomposition
            _flow.value = newValue    // Emits to Flow collectors
        }

    override fun asState(): State<T> = _state

    override fun asFlow(): StateFlow<T> = _flow.asStateFlow()
}
```

**Why Both State and Flow?**

1. **Compose State**: Triggers UI recomposition when values change
2. **Kotlin Flow**: Enables async operations and reactive programming

```kotlin
val counter = rememberMagicState(0)

// Use as Compose State (automatic recomposition)
Text("Count: ${counter.value}")

// Use as Flow (async operations)
LaunchedEffect(Unit) {
    counter.asFlow().collect { value ->
        if (value > 10) {
            // Trigger side effect
            showNotification("Counter exceeded 10")
        }
    }
}
```

---

## 29.5 DSL System

The MagicUI DSL (.vos format) provides declarative UI definition with voice command integration.

### 29.5.1 DSL Syntax

#### Basic Structure

```
App {
    id: "com.example.myapp"
    name: "My Application"
    runtime: "MagicUI"

    ComponentType {
        id: "componentId"
        property1: value1
        property2: value2

        onEvent: (param) => {
            statement1
            statement2
        }

        NestedComponent {
            property: value
        }
    }

    VoiceCommands {
        "trigger phrase" => actionName
    }
}
```

#### Example: Color Picker App

```
App {
    id: "com.example.colorpicker"
    name: "Color Picker"
    runtime: "MagicUI"

    ColorPicker {
        id: "mainPicker"
        initialColor: "#FF5733"
        showAlpha: true
        showHex: true
        allowEyedropper: true

        onColorChange: (color) => {
            VoiceOS.speak("Color changed to " + color)
            Logger.log("Color: " + color)
        }
    }

    VoiceCommands {
        "change color" => openColorPicker
        "show picker" => openColorPicker
        "reset color" => resetColor
    }
}
```

### 29.5.2 Tokenizer

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/dsl/VosTokenizer.kt`

The tokenizer converts raw .vos source into a token stream.

#### Token Types

```kotlin
enum class TokenType {
    // Literals
    IDENTIFIER,   // App, ColorPicker, color
    STRING,       // "com.test.app", "#FF5722"
    NUMBER,       // 123, 45.67
    TRUE,         // true
    FALSE,        // false

    // Symbols
    LBRACE,       // {
    RBRACE,       // }
    LPAREN,       // (
    RPAREN,       // )
    LBRACKET,     // [
    RBRACKET,     // ]
    COMMA,        // ,
    COLON,        // :
    EQUALS,       // =
    ARROW,        // =>
    DOT,          // .

    // Special
    NEWLINE,      // \n
    EOF,          // End of file
    COMMENT       // # comment
}
```

#### Token Data Structure

```kotlin
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
)
```

#### Tokenizer Class

```kotlin
class VosTokenizer(private val source: String) {
    private var position = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()

    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            skipWhitespace()
            if (isAtEnd()) break

            val token = scanToken()
            if (token.type != TokenType.COMMENT) {
                tokens.add(token)
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    private fun scanToken(): Token {
        val char = advance()

        return when (char) {
            '{' -> Token(TokenType.LBRACE, "{", line, column - 1)
            '}' -> Token(TokenType.RBRACE, "}", line, column - 1)
            '(' -> Token(TokenType.LPAREN, "(", line, column - 1)
            ')' -> Token(TokenType.RPAREN, ")", line, column - 1)
            '[' -> Token(TokenType.LBRACKET, "[", line, column - 1)
            ']' -> Token(TokenType.RBRACKET, "]", line, column - 1)
            ',' -> Token(TokenType.COMMA, ",", line, column - 1)
            ':' -> Token(TokenType.COLON, ":", line, column - 1)
            '.' -> Token(TokenType.DOT, ".", line, column - 1)
            '=' -> {
                if (peek() == '>') {
                    advance()
                    Token(TokenType.ARROW, "=>", line, column - 2)
                } else {
                    Token(TokenType.EQUALS, "=", line, column - 1)
                }
            }
            '"' -> scanString()
            '#' -> scanComment()
            '\n' -> {
                line++
                column = 1
                Token(TokenType.NEWLINE, "\n", line - 1, column - 1)
            }
            else -> {
                when {
                    char.isDigit() || (char == '-' && peek().isDigit()) -> scanNumber()
                    char.isLetter() || char == '_' -> scanIdentifier()
                    else -> throw TokenizerException("Unexpected character '$char' at $line:$column")
                }
            }
        }
    }

    private fun scanString(): Token {
        val startLine = line
        val startColumn = column - 1
        val builder = StringBuilder()

        while (!isAtEnd() && peek() != '"') {
            val char = advance()

            if (char == '\n') {
                throw TokenizerException("Unterminated string at $startLine:$startColumn")
            }

            if (char == '\\') {
                if (isAtEnd()) {
                    throw TokenizerException("Unterminated string at $startLine:$startColumn")
                }

                val escaped = advance()
                val escapedChar = when (escaped) {
                    'n' -> '\n'
                    't' -> '\t'
                    'r' -> '\r'
                    '\\' -> '\\'
                    '"' -> '"'
                    else -> throw TokenizerException("Invalid escape sequence '\\$escaped' at $line:$column")
                }
                builder.append(escapedChar)
            } else {
                builder.append(char)
            }
        }

        if (isAtEnd()) {
            throw TokenizerException("Unterminated string at $startLine:$startColumn")
        }

        advance()  // Consume closing quote

        return Token(TokenType.STRING, builder.toString(), startLine, startColumn)
    }

    private fun scanNumber(): Token {
        val startLine = line
        val startColumn = column - 1
        val start = position - 1

        // Consume digits
        while (!isAtEnd() && peek().isDigit()) {
            advance()
        }

        // Check for decimal point
        if (!isAtEnd() && peek() == '.' &&
            position + 1 < source.length &&
            source[position + 1].isDigit()) {
            advance()  // Consume '.'

            while (!isAtEnd() && peek().isDigit()) {
                advance()
            }
        }

        val value = source.substring(start, position)
        return Token(TokenType.NUMBER, value, startLine, startColumn)
    }

    private fun scanIdentifier(): Token {
        val startLine = line
        val startColumn = column - 1
        val start = position - 1

        while (!isAtEnd() && (peek().isLetterOrDigit() || peek() == '_')) {
            advance()
        }

        val value = source.substring(start, position)

        val type = when (value) {
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            else -> TokenType.IDENTIFIER
        }

        return Token(type, value, startLine, startColumn)
    }

    private fun scanComment(): Token {
        val startLine = line
        val startColumn = column - 1
        val start = position - 1

        while (!isAtEnd() && peek() != '\n') {
            advance()
        }

        val value = source.substring(start, position)
        return Token(TokenType.COMMENT, value, startLine, startColumn)
    }

    private fun advance(): Char {
        val char = source[position]
        position++
        column++
        return char
    }

    private fun peek(): Char {
        return if (isAtEnd()) '\u0000' else source[position]
    }

    private fun isAtEnd(): Boolean {
        return position >= source.length
    }

    private fun skipWhitespace() {
        while (!isAtEnd()) {
            when (peek()) {
                ' ', '\t', '\r' -> advance()
                else -> return
            }
        }
    }
}

class TokenizerException(message: String) : Exception(message)
```

#### Tokenization Example

```kotlin
val source = """
    App {
        id: "com.test.app"
        name: "Test"
    }
""".trimIndent()

val tokenizer = VosTokenizer(source)
val tokens = tokenizer.tokenize()

// Output:
// Token(IDENTIFIER, 'App', 1:1)
// Token(LBRACE, '{', 1:5)
// Token(IDENTIFIER, 'id', 2:5)
// Token(COLON, ':', 2:7)
// Token(STRING, 'com.test.app', 2:9)
// Token(IDENTIFIER, 'name', 3:5)
// Token(COLON, ':', 3:9)
// Token(STRING, 'Test', 3:11)
// Token(RBRACE, '}', 4:1)
// Token(EOF, '', 4:2)
```

### 29.5.3 Parser

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/dsl/VosParser.kt`

The parser transforms the token stream into an Abstract Syntax Tree (AST).

#### AST Node Types

```kotlin
// Root App node
data class App(
    val id: String,
    val name: String,
    val runtime: String,
    val components: List<Component>,
    val voiceCommands: Map<String, String>,
    val properties: Map<String, VosValue>
)

// Component node
data class Component(
    val type: String,
    val id: String?,
    val properties: Map<String, VosValue>,
    val children: List<Component>,
    val callbacks: Map<String, VosLambda>
)

// Value types
sealed class VosValue {
    data class StringValue(val value: String) : VosValue()
    data class IntValue(val value: Int) : VosValue()
    data class FloatValue(val value: Float) : VosValue()
    data class BoolValue(val value: Boolean) : VosValue()
    object NullValue : VosValue()
    data class ListValue(val items: List<VosValue>) : VosValue()
    data class ObjectValue(val properties: Map<String, VosValue>) : VosValue()
}

// Lambda (callback) representation
data class VosLambda(
    val params: List<String>,
    val statements: List<VosStatement>
)

// Statement types
sealed class VosStatement {
    data class FunctionCall(val target: String, val args: List<VosValue>) : VosStatement()
    data class Assignment(val target: String, val value: VosValue) : VosStatement()
    data class Return(val value: VosValue?) : VosStatement()
}
```

#### Parser Class

```kotlin
class VosParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): VosAstNode.App {
        return parseApp()
    }

    private fun parseApp(): VosAstNode.App {
        expect(TokenType.IDENTIFIER, "App")
        expect(TokenType.LBRACE)

        val properties = mutableMapOf<String, VosValue>()
        val components = mutableListOf<VosAstNode.Component>()
        val voiceCommands = mutableMapOf<String, String>()

        var id: String? = null
        var name: String? = null
        var runtime = "MagicUI"

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            when {
                check(TokenType.IDENTIFIER) -> {
                    val ident = peek().value
                    when (ident) {
                        "id", "name", "runtime" -> {
                            val prop = parseProperty()
                            when (prop.first) {
                                "id" -> id = extractStringValue(prop.second, "id must be a string")
                                "name" -> name = extractStringValue(prop.second, "name must be a string")
                                "runtime" -> runtime = extractStringValue(prop.second, "runtime must be a string")
                                else -> properties[prop.first] = prop.second
                            }
                        }
                        "VoiceCommands" -> {
                            voiceCommands.putAll(parseVoiceCommands())
                        }
                        else -> {
                            if (peekNext().type == TokenType.LBRACE) {
                                components.add(parseComponent())
                            } else {
                                val prop = parseProperty()
                                properties[prop.first] = prop.second
                            }
                        }
                    }
                }
                else -> throw ParserException("Unexpected token ${peek()}")
            }
        }

        expect(TokenType.RBRACE)

        if (id == null) throw ParserException("App must have 'id' property")
        if (name == null) throw ParserException("App must have 'name' property")

        return VosAstNode.App(
            id = id,
            name = name,
            runtime = runtime,
            components = components,
            voiceCommands = voiceCommands,
            properties = properties
        )
    }

    private fun parseComponent(): VosAstNode.Component {
        val type = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LBRACE)

        val properties = mutableMapOf<String, VosValue>()
        val children = mutableListOf<VosAstNode.Component>()
        val callbacks = mutableMapOf<String, VosLambda>()
        var id: String? = null

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            if (!check(TokenType.IDENTIFIER)) {
                throw ParserException("Expected identifier at ${peek().line}:${peek().column}")
            }

            val identToken = peek()
            val ident = identToken.value
            val nextToken = peekNext()

            when (nextToken.type) {
                TokenType.COLON -> {
                    val prop = parseProperty()

                    if (prop.first == "id") {
                        id = extractStringValue(prop.second, "Component id must be a string")
                    } else if (isCallback(prop.first)) {
                        callbacks[prop.first] = parseLambdaFromValue(prop.second)
                    } else {
                        properties[prop.first] = prop.second
                    }
                }
                TokenType.LBRACE -> {
                    children.add(parseComponent())
                }
                else -> throw ParserException("Unexpected token ${nextToken.type}")
            }
        }

        expect(TokenType.RBRACE)

        return VosAstNode.Component(
            type = type,
            id = id,
            properties = properties,
            children = children,
            callbacks = callbacks
        )
    }

    private fun parseProperty(): Pair<String, VosValue> {
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.COLON)
        val value = parseValue()
        return name to value
    }

    private fun parseValue(): VosValue {
        return when {
            check(TokenType.STRING) -> {
                VosValue.StringValue(advance().value)
            }
            check(TokenType.NUMBER) -> {
                val num = advance().value
                if (num.contains('.')) {
                    VosValue.FloatValue(num.toFloat())
                } else {
                    VosValue.IntValue(num.toInt())
                }
            }
            check(TokenType.TRUE) -> {
                advance()
                VosValue.BoolValue(true)
            }
            check(TokenType.FALSE) -> {
                advance()
                VosValue.BoolValue(false)
            }
            check(TokenType.IDENTIFIER) -> {
                val ident = advance().value
                when (ident) {
                    "true" -> VosValue.BoolValue(true)
                    "false" -> VosValue.BoolValue(false)
                    "null" -> VosValue.NullValue
                    else -> VosValue.StringValue(ident)
                }
            }
            check(TokenType.LPAREN) -> {
                parseLambdaAsValue()
            }
            check(TokenType.LBRACKET) -> {
                parseListValue()
            }
            else -> throw ParserException("Expected value at ${peek().line}:${peek().column}")
        }
    }

    private fun parseListValue(): VosValue {
        expect(TokenType.LBRACKET)
        val items = mutableListOf<VosValue>()

        while (!check(TokenType.RBRACKET) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            items.add(parseValue())

            if (!check(TokenType.RBRACKET)) {
                expect(TokenType.COMMA)
            }
        }

        expect(TokenType.RBRACKET)
        return ListValue(items)
    }

    private fun parseLambda(): VosLambda {
        expect(TokenType.LPAREN)
        val params = mutableListOf<String>()

        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            params.add(expect(TokenType.IDENTIFIER).value)
            if (!check(TokenType.RPAREN)) {
                expect(TokenType.COMMA)
            }
        }

        expect(TokenType.RPAREN)
        expect(TokenType.ARROW)
        expect(TokenType.LBRACE)

        val statements = mutableListOf<VosStatement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            statements.add(parseStatement())
        }

        expect(TokenType.RBRACE)

        return VosLambda(params, statements)
    }

    private fun parseStatement(): VosStatement {
        if (check(TokenType.IDENTIFIER)) {
            val firstToken = peek().value

            if (firstToken == "return") {
                advance()
                val returnValue = if (!check(TokenType.NEWLINE) && !check(TokenType.RBRACE)) {
                    parseValue()
                } else {
                    null
                }
                return VosStatement.Return(returnValue)
            }

            val target = advance().value

            return when {
                check(TokenType.DOT) -> {
                    advance()
                    val method = expect(TokenType.IDENTIFIER).value
                    expect(TokenType.LPAREN)

                    val args = mutableListOf<VosValue>()
                    while (!check(TokenType.RPAREN) && !isAtEnd()) {
                        if (check(TokenType.NEWLINE)) {
                            advance()
                            continue
                        }

                        args.add(parseValue())

                        if (!check(TokenType.RPAREN)) {
                            expect(TokenType.COMMA)
                        }
                    }

                    expect(TokenType.RPAREN)
                    VosStatement.FunctionCall("$target.$method", args)
                }
                check(TokenType.EQUALS) -> {
                    advance()
                    val value = parseValue()
                    VosStatement.Assignment(target, value)
                }
                else -> {
                    VosStatement.FunctionCall(target, emptyList())
                }
            }
        } else {
            throw ParserException("Expected statement at ${peek().line}:${peek().column}")
        }
    }

    private fun parseVoiceCommands(): Map<String, String> {
        expect(TokenType.IDENTIFIER, "VoiceCommands")
        expect(TokenType.LBRACE)
        val commands = mutableMapOf<String, String>()

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            if (check(TokenType.NEWLINE)) {
                advance()
                continue
            }

            val command = expect(TokenType.STRING).value
            expect(TokenType.ARROW)
            val action = expect(TokenType.IDENTIFIER).value
            commands[command] = action

            if (check(TokenType.COMMA)) {
                advance()
            }
        }

        expect(TokenType.RBRACE)
        return commands
    }

    private fun isCallback(name: String): Boolean {
        return name.startsWith("on") ||
                name.endsWith("Changed") ||
                name.endsWith("Clicked") ||
                name.endsWith("Listener") ||
                name.endsWith("Handler")
    }

    private fun extractStringValue(value: VosValue, errorMsg: String): String {
        return when (value) {
            is VosValue.StringValue -> value.value
            else -> throw ParserException(errorMsg)
        }
    }

    // Token navigation helpers
    private fun peek(): Token =
        if (current < tokens.size) tokens[current] else tokens.last()

    private fun peekNext(): Token =
        if (current + 1 < tokens.size) tokens[current + 1] else tokens.last()

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return tokens[current - 1]
    }

    private fun check(type: TokenType): Boolean {
        return !isAtEnd() && peek().type == type
    }

    private fun isAtEnd(): Boolean {
        return current >= tokens.size || peek().type == TokenType.EOF
    }

    private fun expect(type: TokenType, value: String? = null): Token {
        val token = peek()

        if (token.type != type) {
            throw ParserException("Expected $type but got ${token.type} at ${token.line}:${token.column}")
        }

        if (value != null && token.value != value) {
            throw ParserException("Expected '$value' but got '${token.value}' at ${token.line}:${token.column}")
        }

        return advance()
    }
}

class ParserException(message: String) : Exception(message)
```

#### Parsing Example

```kotlin
val source = """
    App {
        id: "com.example.app"
        name: "Example"

        Button {
            id: "submitBtn"
            text: "Submit"
            enabled: true

            onClick: () => {
                VoiceOS.speak("Button clicked")
            }
        }
    }
""".trimIndent()

val tokenizer = VosTokenizer(source)
val tokens = tokenizer.tokenize()

val parser = VosParser(tokens)
val ast = parser.parse()

// ast structure:
// App(
//     id = "com.example.app",
//     name = "Example",
//     components = [
//         Component(
//             type = "Button",
//             id = "submitBtn",
//             properties = {
//                 "text" -> StringValue("Submit"),
//                 "enabled" -> BoolValue(true)
//             },
//             callbacks = {
//                 "onClick" -> VosLambda(
//                     params = [],
//                     statements = [
//                         FunctionCall("VoiceOS.speak", [StringValue("Button clicked")])
//                     ]
//                 )
//             }
//         )
//     ]
// )
```

---

## 29.6 Component System

### 29.6.1 Component Model

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/core/ComponentModel.kt`

```kotlin
@Serializable
data class ComponentModel(
    val uuid: String,              // "namespace/local-id"
    val type: String,              // "Button", "TextField", etc.
    val position: ComponentPosition,
    val properties: Map<String, String> = emptyMap(),
    val children: List<ComponentModel> = emptyList()
) {
    fun withProperties(vararg pairs: Pair<String, String>): ComponentModel {
        return copy(properties = properties + pairs)
    }

    fun withChild(child: ComponentModel): ComponentModel {
        return copy(children = children + child)
    }

    fun withPosition(newPosition: ComponentPosition): ComponentModel {
        return copy(position = newPosition)
    }

    fun isContainer(): Boolean {
        return type in CONTAINER_TYPES || children.isNotEmpty()
    }

    fun descendantCount(): Int {
        return children.size + children.sumOf { it.descendantCount() }
    }

    fun findDescendant(predicate: (ComponentModel) -> Boolean): ComponentModel? {
        if (predicate(this)) return this
        for (child in children) {
            val found = child.findDescendant(predicate)
            if (found != null) return found
        }
        return null
    }

    companion object {
        val CONTAINER_TYPES = setOf(
            "Row", "Column", "Container", "Stack", "Grid",
            "ScrollView", "Dialog", "BottomSheet", "Overlay"
        )
    }
}

@Serializable
data class ComponentPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)
```

### 29.6.2 Component Registry

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/registry/ComponentRegistry.kt`

```kotlin
class ComponentRegistry {
    private val mutex = Mutex()
    private val components = mutableMapOf<String, ComponentDescriptor>()

    suspend fun register(descriptor: ComponentDescriptor) {
        mutex.withLock {
            components[descriptor.type] = descriptor
        }
    }

    suspend fun get(type: String): ComponentDescriptor? {
        return mutex.withLock {
            components[type]
        }
    }

    suspend fun getAll(): List<ComponentDescriptor> {
        return mutex.withLock {
            components.values.toList()
        }
    }

    suspend fun isRegistered(type: String): Boolean {
        return mutex.withLock {
            components.containsKey(type)
        }
    }

    suspend fun unregister(type: String): Boolean {
        return mutex.withLock {
            components.remove(type) != null
        }
    }

    companion object {
        private var instance: ComponentRegistry? = null

        fun getInstance(): ComponentRegistry {
            return instance ?: synchronized(this) {
                instance ?: ComponentRegistry().also { instance = it }
            }
        }
    }
}
```

### 29.6.3 Component Descriptor

```kotlin
data class ComponentDescriptor(
    val type: String,
    val category: ComponentCategory,
    val properties: List<PropertyDescriptor>,
    val events: List<EventDescriptor>,
    val factory: (Map<String, Any?>) -> Any
)

data class PropertyDescriptor(
    val name: String,
    val type: PropertyType,
    val required: Boolean = false,
    val defaultValue: Any? = null
)

enum class PropertyType {
    STRING, INT, FLOAT, BOOLEAN, COLOR, DP, SP
}

data class EventDescriptor(
    val name: String,
    val params: List<EventParam>
)

data class EventParam(
    val name: String,
    val type: String
)

enum class ComponentCategory {
    INPUT, DISPLAY, LAYOUT, NAVIGATION, FEEDBACK, MEDIA
}
```

### 29.6.4 Built-In Components

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/registry/BuiltInComponents.kt`

```kotlin
object BuiltInComponents {
    suspend fun registerAll(registry: ComponentRegistry) {
        registerButton(registry)
        registerTextField(registry)
        registerText(registry)
        registerColorPicker(registry)
        registerRow(registry)
        registerColumn(registry)
        // ... more components
    }

    private suspend fun registerButton(registry: ComponentRegistry) {
        registry.register(
            ComponentDescriptor(
                type = "Button",
                category = ComponentCategory.INPUT,
                properties = listOf(
                    PropertyDescriptor("text", PropertyType.STRING, required = true),
                    PropertyDescriptor("enabled", PropertyType.BOOLEAN, defaultValue = true),
                    PropertyDescriptor("color", PropertyType.COLOR),
                    PropertyDescriptor("width", PropertyType.DP),
                    PropertyDescriptor("height", PropertyType.DP)
                ),
                events = listOf(
                    EventDescriptor("onClick", emptyList()),
                    EventDescriptor("onLongClick", emptyList())
                ),
                factory = { props ->
                    ButtonComponent(
                        text = props["text"] as? String ?: "",
                        enabled = props["enabled"] as? Boolean ?: true,
                        color = props["color"] as? String,
                        width = props["width"] as? Float,
                        height = props["height"] as? Float
                    )
                }
            )
        )
    }

    private suspend fun registerTextField(registry: ComponentRegistry) {
        registry.register(
            ComponentDescriptor(
                type = "TextField",
                category = ComponentCategory.INPUT,
                properties = listOf(
                    PropertyDescriptor("value", PropertyType.STRING, defaultValue = ""),
                    PropertyDescriptor("placeholder", PropertyType.STRING),
                    PropertyDescriptor("label", PropertyType.STRING),
                    PropertyDescriptor("enabled", PropertyType.BOOLEAN, defaultValue = true),
                    PropertyDescriptor("multiline", PropertyType.BOOLEAN, defaultValue = false),
                    PropertyDescriptor("maxLength", PropertyType.INT)
                ),
                events = listOf(
                    EventDescriptor("onValueChange", listOf(
                        EventParam("value", "String")
                    )),
                    EventDescriptor("onFocusChange", listOf(
                        EventParam("focused", "Boolean")
                    ))
                ),
                factory = { props ->
                    TextFieldComponent(
                        value = props["value"] as? String ?: "",
                        placeholder = props["placeholder"] as? String,
                        label = props["label"] as? String,
                        enabled = props["enabled"] as? Boolean ?: true,
                        multiline = props["multiline"] as? Boolean ?: false,
                        maxLength = props["maxLength"] as? Int
                    )
                }
            )
        )
    }

    private suspend fun registerColorPicker(registry: ComponentRegistry) {
        registry.register(
            ComponentDescriptor(
                type = "ColorPicker",
                category = ComponentCategory.INPUT,
                properties = listOf(
                    PropertyDescriptor("initialColor", PropertyType.COLOR, required = true),
                    PropertyDescriptor("showAlpha", PropertyType.BOOLEAN, defaultValue = true),
                    PropertyDescriptor("showHex", PropertyType.BOOLEAN, defaultValue = true),
                    PropertyDescriptor("allowEyedropper", PropertyType.BOOLEAN, defaultValue = false)
                ),
                events = listOf(
                    EventDescriptor("onColorChange", listOf(
                        EventParam("color", "String")
                    ))
                ),
                factory = { props ->
                    ColorPickerComponent(
                        initialColor = props["initialColor"] as? String ?: "#000000",
                        showAlpha = props["showAlpha"] as? Boolean ?: true,
                        showHex = props["showHex"] as? Boolean ?: true,
                        allowEyedropper = props["allowEyedropper"] as? Boolean ?: false
                    )
                }
            )
        )
    }
}
```

---

## 29.7 Runtime Integration

### 29.7.1 MagicUIRuntime Class

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/MagicUIRuntime.kt`

The MagicUIRuntime is the central orchestration class that manages the complete lifecycle of DSL-based applications.

```kotlin
class MagicUIRuntime(
    private val registry: ComponentRegistry = ComponentRegistry.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val eventBus = EventBus()
    private val voiceRouter = VoiceCommandRouter()
    private val actionDispatcher: ActionDispatcher

    private val runningApps = mutableMapOf<String, RunningApp>()

    private val instantiator: ComponentInstantiator
    private val callbackAdapter: CallbackAdapter

    init {
        val propertyMapper = PropertyMapper()
        val typeCoercion = TypeCoercion()

        instantiator = ComponentInstantiator(
            object : com.augmentalis.voiceos.magicui.instantiation.ComponentRegistry {
                override fun get(type: String): ComponentDescriptor? {
                    return runBlocking { registry.get(type) }
                }
            },
            propertyMapper,
            typeCoercion
        )

        val eventContext = EventContext.withStandardGlobals()
        callbackAdapter = CallbackAdapter(eventBus, eventContext)

        actionDispatcher = ActionDispatcher(eventBus)

        scope.launch {
            BuiltInComponents.registerAll(registry)
        }
    }

    // Load DSL app from source
    fun loadApp(dslSource: String): VosAstNode.App {
        try {
            val tokenizer = VosTokenizer(dslSource)
            val tokens = tokenizer.tokenize()

            val parser = VosParser(tokens)
            val app = parser.parse()

            return app
        } catch (e: ParserException) {
            throw RuntimeException("Failed to load app: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load app: ${e.message}", e)
        }
    }

    // Start app (full lifecycle)
    suspend fun start(app: VosAstNode.App): RunningApp {
        if (runningApps.containsKey(app.id)) {
            throw RuntimeException("App already running: ${app.id}")
        }

        val lifecycle = AppLifecycle()
        val resourceManager = ResourceManager()
        val stateManager = StateManager()

        val runningApp = RunningApp(
            id = app.id,
            name = app.name,
            ast = app,
            lifecycle = lifecycle,
            resourceManager = resourceManager,
            stateManager = stateManager,
            components = mutableMapOf()
        )

        // Lifecycle: onCreate
        lifecycle.create()

        // Instantiate components
        for (component in app.components) {
            val instance = instantiator.instantiate(component)
            val componentId = component.id ?: component.type
            runningApp.components[componentId] = instance

            // Bind callbacks
            component.callbacks.forEach { (callbackName, lambda) ->
                val callback = callbackAdapter.createCallback(
                    lambda = lambda,
                    componentId = componentId,
                    eventName = callbackName
                )
            }
        }

        // Register voice commands
        app.voiceCommands.forEach { (trigger, action) ->
            voiceRouter.register(trigger, action, app.id)
        }

        // Lifecycle: onStart
        lifecycle.start()

        // Lifecycle: onResume
        lifecycle.resume()

        runningApps[app.id] = runningApp

        return runningApp
    }

    // Pause app
    suspend fun pause(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")
        app.lifecycle.pause()
    }

    // Resume app
    suspend fun resume(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")
        app.lifecycle.resume()
    }

    // Stop app (full cleanup)
    suspend fun stop(appId: String) {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")

        if (app.lifecycle.state.value == LifecycleState.RESUMED) {
            app.lifecycle.pause()
        }

        app.lifecycle.stop()
        app.resourceManager.releaseAll()
        voiceRouter.clear()
        app.lifecycle.destroy()

        runningApps.remove(appId)
    }

    // Handle voice command
    suspend fun handleVoiceCommand(appId: String, voiceInput: String): Boolean {
        val app = runningApps[appId] ?: throw RuntimeException("App not found: $appId")

        val match = voiceRouter.match(voiceInput) ?: return false

        actionDispatcher.dispatch(match, mapOf("appId" to appId))

        return true
    }

    fun getApp(appId: String): RunningApp? {
        return runningApps[appId]
    }

    fun getAllApps(): List<RunningApp> {
        return runningApps.values.toList()
    }

    suspend fun shutdown() {
        val appIds = runningApps.keys.toList()
        for (appId in appIds) {
            try {
                stop(appId)
            } catch (e: Exception) {
                println("Error stopping app $appId during shutdown: ${e.message}")
            }
        }

        scope.cancel()
    }
}

data class RunningApp(
    val id: String,
    val name: String,
    val ast: VosAstNode.App,
    val lifecycle: AppLifecycle,
    val resourceManager: ResourceManager,
    val stateManager: StateManager,
    val components: MutableMap<String, Any>
)
```

### 29.7.2 Lifecycle Management

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/lifecycle/AppLifecycle.kt`

```kotlin
enum class LifecycleState {
    CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED
}

class AppLifecycle {
    private val _state = MutableStateFlow<LifecycleState>(LifecycleState.CREATED)
    val state: StateFlow<LifecycleState> = _state.asStateFlow()

    suspend fun create() {
        transitionTo(LifecycleState.CREATED)
    }

    suspend fun start() {
        require(_state.value == LifecycleState.CREATED || _state.value == LifecycleState.STOPPED) {
            "Cannot start from ${_state.value}"
        }
        transitionTo(LifecycleState.STARTED)
    }

    suspend fun resume() {
        require(_state.value == LifecycleState.STARTED || _state.value == LifecycleState.PAUSED) {
            "Cannot resume from ${_state.value}"
        }
        transitionTo(LifecycleState.RESUMED)
    }

    suspend fun pause() {
        require(_state.value == LifecycleState.RESUMED) {
            "Cannot pause from ${_state.value}"
        }
        transitionTo(LifecycleState.PAUSED)
    }

    suspend fun stop() {
        require(_state.value == LifecycleState.PAUSED || _state.value == LifecycleState.STARTED) {
            "Cannot stop from ${_state.value}"
        }
        transitionTo(LifecycleState.STOPPED)
    }

    suspend fun destroy() {
        require(_state.value == LifecycleState.STOPPED) {
            "Cannot destroy from ${_state.value}"
        }
        transitionTo(LifecycleState.DESTROYED)
    }

    private fun transitionTo(newState: LifecycleState) {
        _state.value = newState
    }
}
```

### 29.7.3 State Manager

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/lifecycle/StateManager.kt`

```kotlin
class StateManager {
    private val state = mutableMapOf<String, Any?>()

    fun <T> save(key: String, value: T) {
        state[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> restore(key: String): T? {
        return state[key] as? T
    }

    fun <T> restoreOrDefault(key: String, default: T): T {
        return restore(key) ?: default
    }

    fun remove(key: String) {
        state.remove(key)
    }

    fun clear() {
        state.clear()
    }

    fun keys(): Set<String> {
        return state.keys
    }

    fun contains(key: String): Boolean {
        return state.containsKey(key)
    }
}
```

### 29.7.4 Resource Manager

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/lifecycle/ResourceManager.kt`

```kotlin
interface ManagedResource {
    suspend fun release()
}

class ResourceManager {
    private val resources = mutableListOf<ManagedResource>()

    fun register(resource: ManagedResource) {
        resources.add(resource)
    }

    suspend fun releaseAll() {
        for (resource in resources) {
            try {
                resource.release()
            } catch (e: Exception) {
                println("Error releasing resource: ${e.message}")
            }
        }
        resources.clear()
    }

    fun count(): Int {
        return resources.size
    }
}
```

---

## 29.8 Voice Command System

### 29.8.1 Voice Command Router

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/voice/VoiceCommandRouter.kt`

```kotlin
class VoiceCommandRouter {
    private val commands = mutableMapOf<String, VoiceCommand>()

    fun register(trigger: String, action: String, componentId: String? = null) {
        val command = VoiceCommand(
            trigger = trigger.lowercase(),
            action = action,
            componentId = componentId
        )
        commands[trigger.lowercase()] = command
    }

    fun unregister(trigger: String) {
        commands.remove(trigger.lowercase())
    }

    fun match(voiceInput: String): CommandMatch? {
        val normalized = voiceInput.lowercase().trim()

        // Exact match
        commands[normalized]?.let {
            return CommandMatch(it, 1.0f)
        }

        // Fuzzy match
        val matches = commands.values.mapNotNull { command ->
            val similarity = calculateSimilarity(normalized, command.trigger)
            if (similarity > 0.7f) {
                CommandMatch(command, similarity)
            } else null
        }

        return matches.maxByOrNull { it.confidence }
    }

    fun getAll(): List<VoiceCommand> {
        return commands.values.toList()
    }

    fun clear() {
        commands.clear()
    }

    private fun calculateSimilarity(a: String, b: String): Float {
        // Word-based Jaccard similarity
        val aWords = a.split(" ").toSet()
        val bWords = b.split(" ").toSet()

        val intersection = aWords.intersect(bWords).size
        val union = aWords.union(bWords).size

        return if (union > 0) {
            intersection.toFloat() / union.toFloat()
        } else {
            0f
        }
    }
}

data class VoiceCommand(
    val trigger: String,
    val action: String,
    val componentId: String?
)

data class CommandMatch(
    val command: VoiceCommand,
    val confidence: Float
)
```

### 29.8.2 Command Matcher

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/voice/CommandMatcher.kt`

```kotlin
object CommandMatcher {
    /**
     * Levenshtein distance for fuzzy string matching
     */
    fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) {
            for (j in 0..b.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                        dp[i][j] = minOf(
                            dp[i - 1][j] + 1,      // deletion
                            dp[i][j - 1] + 1,      // insertion
                            dp[i - 1][j - 1] + cost // substitution
                        )
                    }
                }
            }
        }

        return dp[a.length][b.length]
    }

    /**
     * Calculate similarity score (0.0 - 1.0)
     */
    fun similarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        if (a.isEmpty() || b.isEmpty()) return 0.0f

        val distance = levenshteinDistance(a.lowercase(), b.lowercase())
        val maxLength = maxOf(a.length, b.length)

        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }

    /**
     * Word-based similarity (better for multi-word commands)
     */
    fun wordSimilarity(a: String, b: String): Float {
        val aWords = a.lowercase().split(" ").toSet()
        val bWords = b.lowercase().split(" ").toSet()

        val intersection = aWords.intersect(bWords).size
        val union = aWords.union(bWords).size

        return if (union > 0) {
            intersection.toFloat() / union.toFloat()
        } else {
            0.0f
        }
    }

    /**
     * Combined similarity (character + word level)
     */
    fun combinedSimilarity(a: String, b: String): Float {
        val charSim = similarity(a, b)
        val wordSim = wordSimilarity(a, b)

        // Weight word similarity higher for multi-word commands
        return if (a.contains(" ") || b.contains(" ")) {
            (charSim * 0.3f) + (wordSim * 0.7f)
        } else {
            charSim
        }
    }
}
```

### 29.8.3 Action Dispatcher

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/voice/ActionDispatcher.kt`

```kotlin
class ActionDispatcher(private val eventBus: EventBus) {

    suspend fun dispatch(match: CommandMatch, context: Map<String, Any?>) {
        val command = match.command

        // Emit event to event bus
        eventBus.emit(
            event = Event(
                type = "VoiceCommand",
                data = mapOf(
                    "action" to command.action,
                    "trigger" to command.trigger,
                    "confidence" to match.confidence,
                    "componentId" to command.componentId,
                    "context" to context
                )
            )
        )
    }
}
```

---

## 29.9 Event System

### 29.9.1 Event Bus

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/events/EventBus.kt`

```kotlin
data class Event(
    val type: String,
    val data: Map<String, Any?>
)

class EventBus {
    private val subscribers = mutableMapOf<String, MutableList<EventHandler>>()
    private val mutex = Mutex()

    suspend fun subscribe(eventType: String, handler: EventHandler) {
        mutex.withLock {
            subscribers.getOrPut(eventType) { mutableListOf() }.add(handler)
        }
    }

    suspend fun unsubscribe(eventType: String, handler: EventHandler) {
        mutex.withLock {
            subscribers[eventType]?.remove(handler)
        }
    }

    suspend fun emit(event: Event) {
        val handlers = mutex.withLock {
            subscribers[event.type]?.toList() ?: emptyList()
        }

        for (handler in handlers) {
            try {
                handler.handle(event)
            } catch (e: Exception) {
                println("Error handling event ${event.type}: ${e.message}")
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            subscribers.clear()
        }
    }
}

interface EventHandler {
    suspend fun handle(event: Event)
}
```

### 29.9.2 Event Context

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/events/EventContext.kt`

```kotlin
class EventContext {
    private val globals = mutableMapOf<String, Any?>()

    fun set(key: String, value: Any?) {
        globals[key] = value
    }

    fun get(key: String): Any? {
        return globals[key]
    }

    fun remove(key: String) {
        globals.remove(key)
    }

    fun clear() {
        globals.clear()
    }

    companion object {
        fun withStandardGlobals(): EventContext {
            return EventContext().apply {
                set("VoiceOS", VoiceOSGlobal)
                set("Logger", LoggerGlobal)
                set("Storage", StorageGlobal)
            }
        }
    }
}

// Global objects accessible from DSL callbacks
object VoiceOSGlobal {
    fun speak(text: String) {
        println("[VoiceOS.speak] $text")
    }

    fun vibrate(duration: Int) {
        println("[VoiceOS.vibrate] ${duration}ms")
    }
}

object LoggerGlobal {
    fun log(message: String) {
        println("[Logger.log] $message")
    }

    fun error(message: String) {
        println("[Logger.error] $message")
    }
}

object StorageGlobal {
    private val storage = mutableMapOf<String, String>()

    fun save(key: String, value: String) {
        storage[key] = value
    }

    fun load(key: String): String? {
        return storage[key]
    }
}
```

### 29.9.3 Callback Adapter

**File:** `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/events/CallbackAdapter.kt`

```kotlin
class CallbackAdapter(
    private val eventBus: EventBus,
    private val context: EventContext
) {

    fun createCallback(
        lambda: VosLambda,
        componentId: String,
        eventName: String
    ): EventHandler {
        return object : EventHandler {
            override suspend fun handle(event: Event) {
                // Execute lambda statements
                for (statement in lambda.statements) {
                    executeStatement(statement, event, context)
                }
            }
        }
    }

    private fun executeStatement(
        statement: VosStatement,
        event: Event,
        context: EventContext
    ) {
        when (statement) {
            is VosStatement.FunctionCall -> {
                executeFunctionCall(statement, event, context)
            }
            is VosStatement.Assignment -> {
                executeAssignment(statement, event, context)
            }
            is VosStatement.Return -> {
                // Return not supported in event handlers
            }
        }
    }

    private fun executeFunctionCall(
        call: VosStatement.FunctionCall,
        event: Event,
        context: EventContext
    ) {
        val parts = call.target.split(".")
        if (parts.size != 2) return

        val (objectName, methodName) = parts
        val obj = context.get(objectName) ?: return

        // Resolve arguments
        val args = call.args.map { resolveValue(it, event) }

        // Call method reflectively (simplified)
        when {
            objectName == "VoiceOS" && methodName == "speak" -> {
                VoiceOSGlobal.speak(args.firstOrNull() as? String ?: "")
            }
            objectName == "Logger" && methodName == "log" -> {
                LoggerGlobal.log(args.firstOrNull() as? String ?: "")
            }
            objectName == "Storage" && methodName == "save" -> {
                val key = args.getOrNull(0) as? String ?: return
                val value = args.getOrNull(1) as? String ?: return
                StorageGlobal.save(key, value)
            }
        }
    }

    private fun executeAssignment(
        assignment: VosStatement.Assignment,
        event: Event,
        context: EventContext
    ) {
        val value = resolveValue(assignment.value, event)
        context.set(assignment.target, value)
    }

    private fun resolveValue(value: VosValue, event: Event): Any? {
        return when (value) {
            is VosValue.StringValue -> value.value
            is VosValue.IntValue -> value.value
            is VosValue.FloatValue -> value.value
            is VosValue.BoolValue -> value.value
            is VosValue.NullValue -> null
            is VosValue.ListValue -> value.items.map { resolveValue(it, event) }
            is VosValue.ObjectValue -> value.properties.mapValues { resolveValue(it.value, event) }
        }
    }
}
```

---

## 29.10 VOS4 Integration

### 29.10.1 Integration Points

MagicUI integrates with VOS4 at multiple levels:

```
VOS4 Application Layer
    ↓
┌───────────────────────────────────────┐
│   VOS4 Voice Command Handler         │
│   (AccessibilityService)              │
└───────────────┬───────────────────────┘
                ↓
┌───────────────────────────────────────┐
│   MagicUIRuntime                      │
│   - loadApp(dslSource)                │
│   - start(app)                        │
│   - handleVoiceCommand(appId, input)  │
└───────────────┬───────────────────────┘
                ↓
┌───────────────────────────────────────┐
│   Component Instantiation             │
│   - Create Compose UI                 │
│   - Bind to Jetpack Compose           │
└───────────────┬───────────────────────┘
                ↓
┌───────────────────────────────────────┐
│   VOS4 UI Layer (Jetpack Compose)     │
└───────────────────────────────────────┘
```

### 29.10.2 VOS4 Runtime Wrapper

```kotlin
// File: VOS4/modules/apps/VoiceOSCore/src/main/kotlin/com/augmentalis/voiceoscore/ui/magic/MagicUIIntegration.kt

object MagicUIIntegration {
    private val runtime = MagicUIRuntime()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Load and start a MagicUI app from DSL source
     */
    fun loadAndStartApp(dslSource: String): String {
        return scope.async {
            try {
                val app = runtime.loadApp(dslSource)
                runtime.start(app)
                app.id
            } catch (e: Exception) {
                Log.e("MagicUI", "Failed to load app", e)
                throw e
            }
        }.await()
    }

    /**
     * Stop a running MagicUI app
     */
    suspend fun stopApp(appId: String) {
        runtime.stop(appId)
    }

    /**
     * Handle voice command for running app
     */
    suspend fun handleVoiceCommand(appId: String, command: String): Boolean {
        return runtime.handleVoiceCommand(appId, command)
    }

    /**
     * Get running app state
     */
    fun getAppState(appId: String): LifecycleState? {
        return runtime.getApp(appId)?.lifecycle?.state?.value
    }
}
```

### 29.10.3 Voice-Driven UI Creation

```kotlin
// VOS4 voice handler integration

class MagicUIVoiceHandler : VoiceCommandHandler {

    override suspend fun handle(command: String): Boolean {
        // Check if command is for creating UI
        when {
            command.startsWith("create ui ") -> {
                val uiType = command.removePrefix("create ui ").trim()
                createUIFromVoice(uiType)
                return true
            }
            command.startsWith("show ") -> {
                val appName = command.removePrefix("show ").trim()
                showApp(appName)
                return true
            }
            else -> {
                // Forward to running apps
                val apps = MagicUIIntegration.runtime.getAllApps()
                for (app in apps) {
                    if (MagicUIIntegration.handleVoiceCommand(app.id, command)) {
                        return true
                    }
                }
                return false
            }
        }
    }

    private suspend fun createUIFromVoice(uiType: String) {
        val dsl = generateDSLFromVoice(uiType)
        MagicUIIntegration.loadAndStartApp(dsl)
    }

    private fun generateDSLFromVoice(uiType: String): String {
        return when (uiType.lowercase()) {
            "color picker" -> """
                App {
                    id: "com.voiceos.colorpicker.${System.currentTimeMillis()}"
                    name: "Voice Color Picker"

                    ColorPicker {
                        id: "picker"
                        initialColor: "#FF5733"
                        showAlpha: true
                        showHex: true

                        onColorChange: (color) => {
                            VoiceOS.speak("Color changed to " + color)
                        }
                    }

                    VoiceCommands {
                        "change color" => openColorPicker
                        "reset" => resetColor
                    }
                }
            """.trimIndent()

            "text editor" -> """
                App {
                    id: "com.voiceos.texteditor.${System.currentTimeMillis()}"
                    name: "Voice Text Editor"

                    TextField {
                        id: "editor"
                        value: ""
                        placeholder: "Start typing..."
                        multiline: true

                        onValueChange: (text) => {
                            Logger.log("Text: " + text)
                        }
                    }

                    VoiceCommands {
                        "clear text" => clearText
                        "save" => saveText
                    }
                }
            """.trimIndent()

            else -> throw IllegalArgumentException("Unknown UI type: $uiType")
        }
    }

    private suspend fun showApp(appName: String) {
        val apps = MagicUIIntegration.runtime.getAllApps()
        val app = apps.find { it.name.contains(appName, ignoreCase = true) }

        if (app != null) {
            if (app.lifecycle.state.value == LifecycleState.PAUSED) {
                MagicUIIntegration.runtime.resume(app.id)
            }
            // Show app UI
            navigateToApp(app.id)
        } else {
            VoiceOSGlobal.speak("App not found: $appName")
        }
    }

    private fun navigateToApp(appId: String) {
        // Navigate to app screen in VOS4
        // Implementation depends on VOS4 navigation system
    }
}
```

### 29.10.4 Compose Integration

```kotlin
// Render MagicUI components in Jetpack Compose

@Composable
fun MagicUIScreen(appId: String) {
    val app = remember { MagicUIIntegration.runtime.getApp(appId) }

    if (app != null) {
        MagicUIAppContent(app)
    } else {
        Text("App not found: $appId")
    }
}

@Composable
fun MagicUIAppContent(app: RunningApp) {
    val lifecycleState by app.lifecycle.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            text = app.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Render components
        for ((componentId, component) in app.components) {
            RenderComponent(componentId, component)
        }

        // Lifecycle state indicator
        Text(
            text = "State: $lifecycleState",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun RenderComponent(componentId: String, component: Any) {
    when (component) {
        is ButtonComponent -> {
            Button(
                onClick = { /* handle click */ },
                enabled = component.enabled
            ) {
                Text(component.text)
            }
        }
        is TextFieldComponent -> {
            var text by remember { mutableStateOf(component.value) }

            TextField(
                value = text,
                onValueChange = { text = it },
                label = component.label?.let { { Text(it) } },
                placeholder = component.placeholder?.let { { Text(it) } },
                enabled = component.enabled,
                maxLines = if (component.multiline) Int.MAX_VALUE else 1
            )
        }
        is ColorPickerComponent -> {
            ColorPickerComposable(
                initialColor = Color(android.graphics.Color.parseColor(component.initialColor)),
                showAlpha = component.showAlpha,
                showHex = component.showHex,
                onColorChange = { /* handle color change */ }
            )
        }
        // ... more components
    }
}
```

---

## 29.11 Complete Example

### 29.11.1 Full Color Picker App

```
# colorpicker.vos

App {
    id: "com.voiceos.colorpicker"
    name: "Voice Color Picker"
    runtime: "MagicUI"

    # Main color picker component
    ColorPicker {
        id: "mainPicker"
        initialColor: "#FF5733"
        showAlpha: true
        showHex: true
        allowEyedropper: true

        onColorChange: (color) => {
            VoiceOS.speak("Color changed to " + color)
            Storage.save("lastColor", color)
            Logger.log("Color: " + color)
        }

        onColorSelected: (color) => {
            VoiceOS.speak("Color selected")
            VoiceOS.vibrate(100)
        }
    }

    # Display selected color
    Container {
        id: "colorDisplay"
        backgroundColor: "#FFFFFF"
        padding: 16

        Text {
            id: "hexLabel"
            text: "Selected Color:"
            fontSize: 16
        }

        Text {
            id: "hexValue"
            text: "#FF5733"
            fontSize: 24
            fontWeight: "bold"
        }
    }

    # Action buttons
    Row {
        id: "actionButtons"
        spacing: 8

        Button {
            id: "resetBtn"
            text: "Reset"
            onClick: () => {
                Logger.log("Reset clicked")
                VoiceOS.speak("Resetting color")
            }
        }

        Button {
            id: "saveBtn"
            text: "Save"
            onClick: () => {
                Logger.log("Save clicked")
                VoiceOS.speak("Color saved")
            }
        }
    }

    # Voice commands
    VoiceCommands {
        "change color" => openColorPicker
        "show picker" => openColorPicker
        "reset color" => resetColor
        "save color" => saveColor
        "close" => closeApp
    }
}
```

### 29.11.2 Loading and Running

```kotlin
suspend fun runColorPickerExample() {
    val runtime = MagicUIRuntime()

    // Load DSL source
    val source = File("colorpicker.vos").readText()

    // Parse and start app
    val app = runtime.loadApp(source)
    val runningApp = runtime.start(app)

    println("App started: ${runningApp.name}")
    println("Components: ${runningApp.components.keys}")

    // Test voice commands
    val commands = listOf(
        "change color",
        "show picker",
        "reset color",
        "save color"
    )

    for (command in commands) {
        val handled = runtime.handleVoiceCommand(app.id, command)
        println("$command -> ${if (handled) "✓" else "✗"}")
    }

    // Cleanup
    runtime.stop(app.id)
    runtime.shutdown()
}
```

---

## 29.12 Summary

MagicUI provides a complete DSL-based UI generation system with:

1. **Type System**: Zero-cost abstractions for dimensions, colors, and composite types
2. **Design System**: Material 3 design tokens for consistent theming
3. **State Management**: Reactive state with Compose State and Kotlin Flow integration
4. **DSL Parser**: Complete lexer/parser for .vos format with comprehensive AST
5. **Component Registry**: Extensible component system with type metadata
6. **Runtime**: Full lifecycle management with event system and voice routing
7. **VOS4 Integration**: Seamless integration with VoiceOS accessibility and UI layers
8. **Voice-Driven Creation**: Generate UIs from voice commands
9. **Cross-Platform**: Kotlin Multiplatform architecture for Android/iOS/Desktop

**Key Files:**
- CoreTypes: `CoreTypes/src/commonMain/kotlin/com/augmentalis/ideamagic/coretypes/CoreTypes.kt`
- Design Tokens: `DesignSystem/src/commonMain/kotlin/com/augmentalis/ideamagic/designsystem/DesignTokens.kt`
- State Management: `StateManagement/src/commonMain/kotlin/com/augmentalis/ideamagic/state/MagicState.kt`
- Tokenizer: `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/dsl/VosTokenizer.kt`
- Parser: `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/dsl/VosParser.kt`
- Runtime: `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/MagicUIRuntime.kt`
- Component Registry: `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/registry/ComponentRegistry.kt`
- Voice Router: `src/commonMain/kotlin/com/augmentalis/voiceos/magicui/voice/VoiceCommandRouter.kt`

**Next Chapter:** Chapter 30 will explore MagicCode - the code generation pipeline for transforming DSL into platform-specific code (Kotlin Compose, SwiftUI, HTML).

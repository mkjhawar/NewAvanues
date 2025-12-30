# Protocol Conformance Strategy - AvaUI & AvaCode Integration

**Document Type:** Architecture Decision Document
**Created:** 2025-11-02 00:40 PDT
**Status:** Recommendation - Awaiting Implementation

---

## Executive Summary

**Question:** How should Foundation components and Core components integrate to conform to AvaUI and AvaCode protocols?

**Answer:** **Two-Tier Hybrid Architecture** - Keep both systems, use Foundation as the rendering layer for Core.

**Key Insight:** Foundation and Core serve fundamentally different purposes in the AvaUI ecosystem:
- **Core** = Platform-agnostic data models (DSL-compatible, serializable)
- **Foundation** = Platform-specific rendering implementations (Compose-native)

**Recommendation:** Create **ComposeRenderer** adapter that bridges Core → Foundation, enabling:
1. ✅ AvaCode DSL generation → Core components → Foundation rendering
2. ✅ Direct Compose usage via Foundation (no DSL needed)
3. ✅ Cross-platform support (iOS/Web renderers coming)
4. ✅ Zero duplicate work - Foundation becomes Android renderer

---

## The Three Protocols

### 1. AvaUI Protocol (Component + Renderer Pattern)

**Purpose:** Platform-agnostic component definitions with pluggable renderers

**Pattern:**
```kotlin
// Define components as data (Core)
interface Component {
    val id: String?
    val style: ComponentStyle?
    val modifiers: List<Modifier>
    fun render(renderer: Renderer): Any
}

// Render on any platform
interface Renderer {
    val platform: Platform
    fun render(component: Component): Any
    fun applyTheme(theme: Theme)
}

// DSL entry point
val ui = AvaUI {
    theme = Themes.Material3Light

    Column {
        Text("Hello") { color = theme.colorScheme.primary }
        Button("Click") { onClick = { } }
    }
}

// Platform-specific rendering
androidRenderer.render(ui)  // → Jetpack Compose UI
iosRenderer.render(ui)      // → SwiftUI
webRenderer.render(ui)      // → React components
```

**Key Characteristics:**
- ✅ Components are **data models** (immutable, serializable)
- ✅ DSL builder creates **Component trees**
- ✅ Renderer interface for **platform abstraction**
- ✅ Theme applied at render time
- ✅ All `render()` methods currently return `TODO()`

### 2. AvaCode Protocol (DSL → Code Generation)

**Purpose:** Parse .vos DSL files and generate native platform code

**Pattern:**
```kotlin
// 1. Parse .vos file
val vosContent = """
    theme iOS26LiquidGlass

    screen LoginScreen {
        Column {
            padding 16

            Text "Welcome Back" {
                font Title
                color primary
            }

            TextField email {
                placeholder "Enter email"
            }

            Button "Login" {
                style Primary
                onClick handleLogin
            }
        }
    }
"""

// 2. Tokenize & parse to AST
val tokenizer = VosTokenizer(vosContent)
val tokens = tokenizer.tokenize()
val parser = VosParser(tokens)
val ast = parser.parse()

// 3. Generate platform code
val kotlinCode = KotlinComposeGenerator.generate(ast)
val swiftCode = SwiftUIGenerator.generate(ast)
val reactCode = ReactTypeScriptGenerator.generate(ast)
```

**Key Characteristics:**
- ✅ Text-based DSL (`.vos` files)
- ✅ Multi-target code generation
- ✅ Static code output (App Store compliant)
- ✅ NOT runtime interpretation (App Store violation)

### 3. Foundation Protocol (Direct Compose Implementation)

**Purpose:** Production-ready @Composable components for immediate use

**Pattern:**
```kotlin
// Direct Compose usage - no DSL needed
@Composable
fun LoginScreen() {
    MagicTheme {
        V(spacing = 16.dp, modifier = Modifier.padding(16.dp)) {
            MagicText(
                text = "Welcome Back",
                style = TextVariant.HeadlineLarge
            )

            val emailState = rememberMagicState("")
            MagicTextField(
                state = emailState,
                placeholder = "Enter email"
            )

            MagicButton(
                text = "Login",
                variant = ButtonVariant.Filled,
                onClick = { handleLogin() }
            )
        }
    }
}
```

**Key Characteristics:**
- ✅ Direct @Composable functions
- ✅ Type-safe (MagicColor, MagicDp, MagicState)
- ✅ Zero abstraction - immediate rendering
- ✅ Material 3 integration
- ✅ NOT serializable, NOT cross-platform

---

## Current State Analysis

### What We Have

**Core Components (32+):**
- ✅ Data model definitions
- ✅ ComponentStyle, Modifier system
- ✅ Full DSL builder (AvaUIScope)
- ❌ All `render()` = `TODO()` (not implemented)
- ❌ No working renderers

**Foundation Components (15):**
- ✅ Production-ready @Composables
- ✅ Material 3 integration
- ✅ MagicTheme, DesignTokens
- ✅ MagicState, CoreTypes
- ❌ Don't implement Component interface
- ❌ Not compatible with DSL

**AvaCode Generator:**
- ✅ VosParser, VosTokenizer exist
- ✅ Generator interfaces defined
- ❌ Generators not fully implemented
- ❌ Missing AST → Component mapping

### The Gap

**Problem:** We have THREE disconnected systems:
1. Core components (data models, no renderers)
2. Foundation components (working Compose, no DSL)
3. AvaCode generators (parsing only, no output)

**Solution:** Connect them via **adapters and generators**.

---

## Recommended Architecture

### Two-Tier Hybrid System

```
┌────────────────────────────────────────────────────────┐
│  TIER 1: Direct Compose Usage                         │
│  (No DSL needed - pure Kotlin/Compose)                │
├────────────────────────────────────────────────────────┤
│  @Composable fun MyApp() {                            │
│      MagicTheme {                                     │
│          V { MagicButton("Click") { } }               │
│      }                                                 │
│  }                                                     │
│  ↓ Uses Foundation components directly                │
└────────────────────────────────────────────────────────┘
                         ↑
                         │ Foundation Components
                         │ (MagicButton, MagicText, etc.)
                         ↓
┌────────────────────────────────────────────────────────┐
│  TIER 2: DSL-Driven Cross-Platform                    │
│  (AvaCode → Core → Renderers)                       │
├────────────────────────────────────────────────────────┤
│  1. Write .vos DSL file                               │
│     ↓                                                  │
│  2. AvaCode parses DSL → AST                        │
│     ↓                                                  │
│  3. Generate Kotlin code using Core components        │
│     ↓                                                  │
│  4. Core components render via ComposeRenderer        │
│     ↓                                                  │
│  5. ComposeRenderer uses Foundation @Composables      │
│                                                        │
│  Result: Android (Compose), iOS (SwiftUI), Web (React)│
└────────────────────────────────────────────────────────┘
```

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   Application Layer                     │
├─────────────────────────────────────────────────────────┤
│  Choice A: Direct Compose                              │
│  - Use Foundation components directly                  │
│  - @Composable functions                               │
│  - No DSL needed                                        │
│                                                         │
│  Choice B: DSL-Driven                                   │
│  - Write .vos DSL files                                 │
│  - Use Core component definitions                      │
│  - Cross-platform rendering                            │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                   AvaCode Layer                       │
│  (Only for Choice B)                                    │
├─────────────────────────────────────────────────────────┤
│  VosParser → AST → KotlinComposeGenerator              │
│  Generates: Core component construction code            │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                   Core Components                       │
│  (Data Models - Platform Agnostic)                     │
├─────────────────────────────────────────────────────────┤
│  ButtonComponent, TextComponent, ChipComponent, etc.    │
│  + ComponentStyle, Modifier system                      │
│  + render(renderer: Renderer): Any                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                   Renderer Layer                        │
│  (Platform-Specific Rendering)                         │
├─────────────────────────────────────────────────────────┤
│  ComposeRenderer (Android/Desktop) ← NEW                │
│  - Uses Foundation @Composables                         │
│                                                         │
│  iOSRenderer (iOS) ← Future                            │
│  - Uses SwiftUI bridge                                  │
│                                                         │
│  WebRenderer (Web) ← Future                            │
│  - Uses React components                                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                Foundation Components                    │
│  (Direct Compose Implementations)                      │
├─────────────────────────────────────────────────────────┤
│  MagicButton, MagicText, MagicCard, MagicChip, etc.    │
│  + MagicTheme, DesignTokens                             │
│  + MagicState, CoreTypes (MagicDp, MagicColor)         │
└─────────────────────────────────────────────────────────┘
```

---

## Implementation Strategy

### Phase 1: Create ComposeRenderer (8-12 hours)

**Goal:** Implement Core component rendering using Foundation

**New File:** `Universal/IDEAMagic/Components/Adapters/ComposeRenderer.kt`

```kotlin
package com.augmentalis.avamagic.components.adapters

import androidx.compose.runtime.Composable
import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.components.*
import com.augmentalis.avamagic.components.*
import com.augmentalis.avamagic.designsystem.MagicTheme

/**
 * Renders Core components using Foundation @Composables
 *
 * This is the Android/Desktop renderer that bridges the gap between
 * platform-agnostic Core definitions and native Compose implementations.
 *
 * Usage:
 * ```kotlin
 * val ui = AvaUI {
 *     Button("Click Me") { onClick = { } }
 * }
 *
 * val renderer = ComposeRenderer()
 * ui.render(renderer)  // → MagicButton @Composable
 * ```
 */
class ComposeRenderer : Renderer {
    override val platform: Platform = Platform.Android

    private var currentTheme: Theme? = null

    override fun applyTheme(theme: Theme) {
        currentTheme = theme
    }

    override fun render(component: Component): Any {
        return when (component) {
            // Basic components
            is ButtonComponent -> renderButton(component)
            is TextComponent -> renderText(component)
            is TextFieldComponent -> renderTextField(component)
            is IconComponent -> renderIcon(component)
            is ImageComponent -> renderImage(component)

            // Containers
            is CardComponent -> renderCard(component)
            is ChipComponent -> renderChip(component)
            is DividerComponent -> renderDivider(component)
            is BadgeComponent -> renderBadge(component)

            // Layouts
            is ColumnComponent -> renderColumn(component)
            is RowComponent -> renderRow(component)
            is ContainerComponent -> renderContainer(component)
            is ScrollViewComponent -> renderScrollView(component)

            // Lists
            is ListComponent -> renderList(component)

            // Form components (Future)
            is SliderComponent -> renderSlider(component)
            is RadioComponent -> renderRadio(component)
            is DropdownComponent -> renderDropdown(component)
            is CheckboxComponent -> renderCheckbox(component)
            is SwitchComponent -> renderSwitch(component)

            // Feedback components (Future)
            is DialogComponent -> renderDialog(component)
            is ToastComponent -> renderToast(component)
            is AlertComponent -> renderAlert(component)
            is ProgressBarComponent -> renderProgressBar(component)
            is SpinnerComponent -> renderSpinner(component)

            else -> error("Unsupported component type: ${component::class.simpleName}")
        }
    }

    // ==================== Basic Components ====================

    @Composable
    private fun renderButton(button: ButtonComponent) {
        MagicButton(
            text = button.text,
            onClick = button.onClick ?: {},
            variant = when (button.buttonStyle) {
                ButtonScope.ButtonStyle.Primary -> ButtonVariant.Filled
                ButtonScope.ButtonStyle.Secondary -> ButtonVariant.Tonal
                ButtonScope.ButtonStyle.Outlined -> ButtonVariant.Outlined
                ButtonScope.ButtonStyle.Text -> ButtonVariant.Text
                else -> ButtonVariant.Filled
            },
            enabled = button.enabled,
            icon = button.leadingIcon?.let {
                { MagicIcon(it) }
            },
            modifier = applyModifiers(button)
        )
    }

    @Composable
    private fun renderText(text: TextComponent) {
        MagicText(
            text = text.text,
            style = mapFont(text.font),
            color = text.color?.let { MagicColor(it.toComposeColor()) },
            textAlign = mapTextAlign(text.textAlign),
            maxLines = text.maxLines ?: Int.MAX_VALUE,
            overflow = mapTextOverflow(text.overflow),
            modifier = applyModifiers(text)
        )
    }

    @Composable
    private fun renderTextField(textField: TextFieldComponent) {
        val state = rememberMagicState(textField.value)

        // Sync state changes back to onValueChange
        LaunchedEffect(state.value) {
            if (state.value != textField.value) {
                textField.onValueChange?.invoke(state.value)
            }
        }

        MagicTextField(
            state = state,
            label = textField.label,
            placeholder = textField.placeholder,
            helperText = if (textField.isError) textField.errorMessage else null,
            errorText = if (textField.isError) textField.errorMessage else null,
            leadingIcon = textField.leadingIcon?.let {
                { MagicIcon(it) }
            },
            trailingIcon = textField.trailingIcon?.let {
                { MagicIcon(it) }
            },
            enabled = textField.enabled,
            readOnly = textField.readOnly,
            modifier = applyModifiers(textField)
        )
    }

    @Composable
    private fun renderIcon(icon: IconComponent) {
        MagicIcon(
            name = icon.name,
            tint = icon.tint?.let { MagicColor(it.toComposeColor()) },
            contentDescription = icon.contentDescription,
            modifier = applyModifiers(icon)
        )
    }

    @Composable
    private fun renderImage(image: ImageComponent) {
        MagicImage(
            source = image.source,
            contentDescription = image.contentDescription,
            contentScale = mapContentScale(image.contentScale),
            modifier = applyModifiers(image)
        )
    }

    // ==================== Containers ====================

    @Composable
    private fun renderCard(card: CardComponent) {
        MagicCard(
            variant = when (card.elevation) {
                0 -> CardVariant.Outlined
                1 -> CardVariant.Filled
                else -> CardVariant.Elevated
            },
            modifier = applyModifiers(card)
        ) {
            card.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderChip(chip: ChipComponent) {
        MagicChip(
            text = chip.label,
            onClick = chip.onClick,
            leadingIcon = chip.icon?.let {
                { MagicIcon(it) }
            },
            trailingIcon = if (chip.deletable && chip.onDelete != null) {
                { MagicIcon("close", onClick = chip.onDelete) }
            } else null,
            variant = if (chip.selected) ChipVariant.Filled else ChipVariant.Outlined,
            modifier = applyModifiers(chip)
        )
    }

    @Composable
    private fun renderDivider(divider: DividerComponent) {
        MagicDivider(
            orientation = when (divider.orientation) {
                Orientation.Horizontal -> DividerOrientation.Horizontal
                Orientation.Vertical -> DividerOrientation.Vertical
            },
            thickness = divider.thickness?.dp ?: 1.dp,
            color = divider.color?.let { MagicColor(it.toComposeColor()) },
            modifier = applyModifiers(divider)
        )
    }

    @Composable
    private fun renderBadge(badge: BadgeComponent) {
        MagicBadge(
            content = badge.content,
            modifier = applyModifiers(badge)
        )
    }

    // ==================== Layouts ====================

    @Composable
    private fun renderColumn(column: ColumnComponent) {
        V(
            spacing = mapSpacing(column.arrangement),
            horizontalAlignment = mapHorizontalAlignment(column.horizontalAlignment),
            modifier = applyModifiers(column)
        ) {
            column.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderRow(row: RowComponent) {
        H(
            spacing = mapSpacing(row.arrangement),
            verticalAlignment = mapVerticalAlignment(row.verticalAlignment),
            modifier = applyModifiers(row)
        ) {
            row.children.forEach { child ->
                render(child)
            }
        }
    }

    @Composable
    private fun renderContainer(container: ContainerComponent) {
        MagicBox(
            contentAlignment = mapAlignment(container.alignment),
            modifier = applyModifiers(container)
        ) {
            container.child?.let { render(it) }
        }
    }

    @Composable
    private fun renderScrollView(scroll: ScrollViewComponent) {
        when (scroll.orientation) {
            Orientation.Vertical -> MagicScroll(modifier = applyModifiers(scroll)) {
                scroll.child?.let { render(it) }
            }
            Orientation.Horizontal -> MagicScrollH(modifier = applyModifiers(scroll)) {
                scroll.child?.let { render(it) }
            }
        }
    }

    // ==================== Lists ====================

    @Composable
    private fun renderList(list: ListComponent) {
        MagicList(
            modifier = applyModifiers(list),
            showDividers = list.showDividers
        ) {
            list.items.forEach { item ->
                MagicListItem(
                    headline = item.headline,
                    supporting = item.supporting,
                    overline = item.overline,
                    leading = item.leadingIcon?.let {
                        { MagicIcon(it) }
                    },
                    trailing = item.trailingIcon?.let {
                        { MagicIcon(it) }
                    },
                    onClick = item.onClick
                )
            }
        }
    }

    // ==================== Helper Methods ====================

    @Composable
    private fun applyModifiers(component: Component): Modifier {
        var modifier = Modifier

        // Apply ComponentStyle
        component.style?.let { style ->
            style.width?.let { modifier = modifier.width(it.dp) }
            style.height?.let { modifier = modifier.height(it.dp) }
            style.padding?.let { modifier = modifier.padding(it.dp) }
            style.margin?.let { modifier = modifier.padding(it.dp) }  // Margin as padding
            style.backgroundColor?.let {
                modifier = modifier.background(MagicColor(it.toComposeColor()).value)
            }
            // ... apply other style properties
        }

        return modifier
    }

    private fun mapFont(font: Font): TextVariant {
        return when (font) {
            Font.DisplayLarge -> TextVariant.DisplayLarge
            Font.DisplayMedium -> TextVariant.DisplayMedium
            Font.DisplaySmall -> TextVariant.DisplaySmall
            Font.HeadlineLarge -> TextVariant.HeadlineLarge
            Font.HeadlineMedium -> TextVariant.HeadlineMedium
            Font.HeadlineSmall -> TextVariant.HeadlineSmall
            Font.TitleLarge -> TextVariant.TitleLarge
            Font.TitleMedium -> TextVariant.TitleMedium
            Font.TitleSmall -> TextVariant.TitleSmall
            Font.BodyLarge -> TextVariant.BodyLarge
            Font.BodyMedium -> TextVariant.BodyMedium
            Font.BodySmall -> TextVariant.BodySmall
            Font.LabelLarge -> TextVariant.LabelLarge
            Font.LabelMedium -> TextVariant.LabelMedium
            Font.LabelSmall -> TextVariant.LabelSmall
            else -> TextVariant.BodyMedium
        }
    }

    private fun mapTextAlign(textAlign: TextScope.TextAlign): TextAlign {
        return when (textAlign) {
            TextScope.TextAlign.Start -> TextAlign.Start
            TextScope.TextAlign.Center -> TextAlign.Center
            TextScope.TextAlign.End -> TextAlign.End
            TextScope.TextAlign.Justify -> TextAlign.Justify
        }
    }

    private fun mapTextOverflow(overflow: TextScope.TextOverflow): TextOverflow {
        return when (overflow) {
            TextScope.TextOverflow.Clip -> TextOverflow.Clip
            TextScope.TextOverflow.Ellipsis -> TextOverflow.Ellipsis
            TextScope.TextOverflow.Visible -> TextOverflow.Visible
        }
    }

    private fun mapContentScale(scale: ImageScope.ContentScale): ContentScale {
        return when (scale) {
            ImageScope.ContentScale.Fit -> ContentScale.Fit
            ImageScope.ContentScale.Fill -> ContentScale.FillBounds
            ImageScope.ContentScale.Crop -> ContentScale.Crop
            ImageScope.ContentScale.None -> ContentScale.None
        }
    }

    private fun mapSpacing(arrangement: Arrangement): Dp {
        return when (arrangement) {
            Arrangement.Start -> 0.dp
            Arrangement.SpaceBetween -> 0.dp
            Arrangement.SpaceAround -> 0.dp
            Arrangement.SpaceEvenly -> 0.dp
            Arrangement.End -> 0.dp
            Arrangement.Center -> 0.dp
            is Arrangement.SpacedBy -> arrangement.space.dp
        }
    }

    private fun mapHorizontalAlignment(alignment: Alignment): Alignment.Horizontal {
        return when (alignment) {
            Alignment.Start, Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> Alignment.Start
            Alignment.End, Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    }

    private fun mapVerticalAlignment(alignment: Alignment): Alignment.Vertical {
        return when (alignment) {
            Alignment.Top, Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> Alignment.Top
            Alignment.Bottom, Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd -> Alignment.Bottom
            else -> Alignment.CenterVertically
        }
    }

    private fun mapAlignment(alignment: Alignment): Alignment {
        return when (alignment) {
            Alignment.TopStart -> Alignment.TopStart
            Alignment.TopCenter -> Alignment.TopCenter
            Alignment.TopEnd -> Alignment.TopEnd
            Alignment.CenterStart -> Alignment.CenterStart
            Alignment.Center -> Alignment.Center
            Alignment.CenterEnd -> Alignment.CenterEnd
            Alignment.BottomStart -> Alignment.BottomStart
            Alignment.BottomCenter -> Alignment.BottomCenter
            Alignment.BottomEnd -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }
    }

    private fun Color.toComposeColor(): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color(red, green, blue, alpha)
    }
}
```

**Build Config:** `Universal/IDEAMagic/Components/Adapters/build.gradle.kts`

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

group = "com.augmentalis.avamagic"
version = "1.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core components (data models)
                implementation(project(":Universal:IDEAMagic:Components:Core"))

                // Foundation components (Compose implementations)
                implementation(project(":Universal:IDEAMagic:Components:Foundation"))

                // Design system
                implementation(project(":Universal:IDEAMagic:AvaUI:DesignSystem"))
                implementation(project(":Universal:IDEAMagic:AvaUI:CoreTypes"))
                implementation(project(":Universal:IDEAMagic:AvaUI:StateManagement"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }

        all {
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting
        val desktopMain by getting
    }
}

android {
    namespace = "com.augmentalis.avamagic.components.adapters"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

**Update `settings.gradle.kts`:**
```kotlin
include(":Universal:IDEAMagic:Components:Adapters")
```

### Phase 2: Implement Core render() Methods (4-6 hours)

**Goal:** Replace all `TODO()` with actual renderer calls

**Pattern:** Update each Core component:

```kotlin
// BEFORE (Current)
data class ChipComponent(...) : Component {
    override fun render(renderer: Renderer): Any {
        TODO("Platform rendering not yet implemented")
    }
}

// AFTER (Updated)
data class ChipComponent(...) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

**Files to Update:**
- All 32+ Core component files in `Universal/IDEAMagic/Components/Core/src/commonMain/kotlin/com/augmentalis/avaelements/components/`

### Phase 3: Complete AvaCode Generators (12-16 hours)

**Goal:** Generate Kotlin Compose code from .vos DSL files

**Pattern:**

```kotlin
class KotlinComposeGenerator {
    fun generate(ast: VosAst): String {
        val code = StringBuilder()

        // Generate imports
        code.appendLine("import com.augmentalis.avaelements.dsl.*")
        code.appendLine("import com.augmentalis.avaelements.core.*")
        code.appendLine("import com.augmentalis.avamagic.components.adapters.ComposeRenderer")
        code.appendLine()

        // Generate function
        code.appendLine("@Composable")
        code.appendLine("fun ${ast.screenName}() {")

        // Generate AvaUI DSL
        code.appendLine("    val ui = AvaUI {")
        code.appendLine("        theme = ${ast.theme}")
        code.appendLine()

        // Generate component tree from AST
        generateComponent(ast.rootComponent, code, indent = 2)

        code.appendLine("    }")
        code.appendLine()
        code.appendLine("    // Render using ComposeRenderer")
        code.appendLine("    val renderer = ComposeRenderer()")
        code.appendLine("    ui.render(renderer)")
        code.appendLine("}")

        return code.toString()
    }

    private fun generateComponent(node: AstNode, code: StringBuilder, indent: Int) {
        val indentStr = "    ".repeat(indent)

        when (node.type) {
            "Column" -> {
                code.appendLine("${indentStr}Column {")
                node.children.forEach { generateComponent(it, code, indent + 1) }
                code.appendLine("$indentStr}")
            }
            "Text" -> {
                code.appendLine("${indentStr}Text(\"${node.text}\") {")
                node.properties.forEach { (key, value) ->
                    code.appendLine("$indentStr    $key = $value")
                }
                code.appendLine("$indentStr}")
            }
            "Button" -> {
                code.appendLine("${indentStr}Button(\"${node.text}\") {")
                node.properties.forEach { (key, value) ->
                    code.appendLine("$indentStr    $key = $value")
                }
                code.appendLine("$indentStr}")
            }
            // ... handle all component types
        }
    }
}
```

**Example Output:**

Input `.vos`:
```
theme Material3Light

screen LoginScreen {
    Column {
        padding 16

        Text "Welcome Back" {
            font HeadlineLarge
            color primary
        }

        Button "Login" {
            style Primary
            onClick handleLogin
        }
    }
}
```

Generated Kotlin:
```kotlin
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.core.*
import com.augmentalis.avamagic.components.adapters.ComposeRenderer

@Composable
fun LoginScreen() {
    val ui = AvaUI {
        theme = Themes.Material3Light

        Column {
            padding(16f)

            Text("Welcome Back") {
                font = Font.HeadlineLarge
                color = theme.colorScheme.primary
            }

            Button("Login") {
                buttonStyle = ButtonStyle.Primary
                onClick = { handleLogin() }
            }
        }
    }

    // Render using ComposeRenderer
    val renderer = ComposeRenderer()
    ui.render(renderer)
}
```

### Phase 4: Enhance Foundation with Core Features (2-4 hours)

**Goal:** Add missing features from Core to Foundation

**Changes:**

**1. MagicChip - Add selection + deletion**

```kotlin
@Composable
fun MagicChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,  // NEW from Core
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onDelete: (() -> Unit)? = null,  // NEW from Core
    variant: ChipVariant = ChipVariant.Filled
) {
    // Use FilterChip for selectable, InputChip for deletable
    when {
        selected && onDelete != null -> {
            InputChip(
                selected = true,
                onClick = onClick ?: {},
                label = { MagicText(text, style = TextVariant.LabelMedium) },
                leadingIcon = leadingIcon,
                trailingIcon = {
                    MagicIcon("close", onClick = onDelete)
                },
                modifier = modifier
            )
        }
        selected -> {
            FilterChip(
                selected = true,
                onClick = onClick ?: {},
                label = { MagicText(text, style = TextVariant.LabelMedium) },
                leadingIcon = leadingIcon,
                modifier = modifier
            )
        }
        onDelete != null -> {
            InputChip(
                selected = false,
                onClick = onClick ?: {},
                label = { MagicText(text, style = TextVariant.LabelMedium) },
                leadingIcon = leadingIcon,
                trailingIcon = {
                    MagicIcon("close", onClick = onDelete)
                },
                modifier = modifier
            )
        }
        else -> {
            // Existing implementation
            when (variant) {
                ChipVariant.Filled -> AssistChip(...)
                ChipVariant.Outlined -> AssistChip(...)
            }
        }
    }
}
```

**2. MagicDivider - Add text label**

```kotlin
@Composable
fun MagicDivider(
    modifier: Modifier = Modifier,
    text: String? = null,  // NEW from Core
    orientation: DividerOrientation = DividerOrientation.Horizontal,
    thickness: Dp = 1.dp,
    color: MagicColor? = null
) {
    if (text != null) {
        // Divider with text label
        H(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            spacing = 8.dp
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                thickness = thickness,
                color = color?.value ?: Color.Unspecified
            )
            MagicText(
                text = text,
                style = TextVariant.LabelSmall,
                color = color
            )
            Divider(
                modifier = Modifier.weight(1f),
                thickness = thickness,
                color = color?.value ?: Color.Unspecified
            )
        }
    } else {
        Divider(
            modifier = modifier,
            thickness = thickness,
            color = color?.value ?: Color.Unspecified
        )
    }
}
```

**3. MagicListItem - Add selection**

```kotlin
@Composable
fun MagicListItem(
    headline: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,  // NEW from Core
    supporting: String? = null,
    overline: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    val itemModifier = if (onClick != null) {
        modifier
            .clickable(onClick = onClick)
            .background(backgroundColor)
    } else {
        modifier.background(backgroundColor)
    }

    ListItem(
        headlineContent = { MagicText(headline, style = TextVariant.BodyLarge) },
        modifier = itemModifier,
        // ... rest of implementation
    )
}
```

---

## Usage Examples

### Example 1: Direct Compose Usage (Tier 1)

```kotlin
@Composable
fun MyApp() {
    MagicTheme {
        V(spacing = 16.dp, modifier = Modifier.padding(16.dp)) {
            MagicText("Dashboard", style = TextVariant.HeadlineLarge)

            MagicCard {
                V(spacing = 8.dp) {
                    MagicText("Total Users", style = TextVariant.TitleMedium)
                    MagicText("1,234", style = TextVariant.DisplaySmall)
                }
            }

            H(spacing = 8.dp) {
                MagicChip("Active", selected = true)
                MagicChip("Inactive")
            }
        }
    }
}
```

**Benefits:**
- ✅ Type-safe, compile-time checked
- ✅ IDE autocomplete
- ✅ Hot reload support
- ✅ Direct Compose - zero overhead

### Example 2: DSL-Driven Cross-Platform (Tier 2)

**Step 1:** Write `.vos` DSL file

```
theme Material3Light

screen DashboardScreen {
    Column {
        padding 16
        spacing 16

        Text "Dashboard" {
            font HeadlineLarge
        }

        Card {
            elevation 1

            Column {
                spacing 8

                Text "Total Users" {
                    font TitleMedium
                }

                Text "1,234" {
                    font DisplaySmall
                    color primary
                }
            }
        }

        Row {
            spacing 8

            Chip "Active" {
                selected true
            }

            Chip "Inactive"
        }
    }
}
```

**Step 2:** Generate Kotlin code via AvaCode

```bash
avacode generate dashboard.vos --target kotlin-compose
```

**Step 3:** Use generated code

```kotlin
import com.example.generated.DashboardScreen

@Composable
fun App() {
    DashboardScreen()  // Generated function
}
```

**Benefits:**
- ✅ Platform-agnostic definitions
- ✅ Generate Android (Compose), iOS (SwiftUI), Web (React)
- ✅ Consistent UI across platforms
- ✅ Designer-friendly DSL syntax

---

## Decision Matrix

| Use Case | Approach | Why? |
|----------|----------|------|
| **Pure Android app** | Direct Foundation (Tier 1) | Fastest, type-safe, no overhead |
| **Android + iOS + Web** | DSL + Renderers (Tier 2) | Cross-platform consistency |
| **Rapid prototyping** | Direct Foundation (Tier 1) | Hot reload, IDE support |
| **Design handoff** | DSL + Renderers (Tier 2) | Designers write DSL, developers implement renderers |
| **Server-driven UI** | DSL + Renderers (Tier 2) | Send DSL from server, render on client |
| **Component library** | Both | Foundation for direct use, Core for cross-platform |

---

## Migration Path for Existing Code

### If You're Using Core Components (Pre-Foundation)

**Before (Broken):**
```kotlin
val chip = ChipComponent(
    label = "Tag",
    onClick = { }
)

// This fails - render() = TODO()
chip.render(renderer)
```

**After (Working):**
```kotlin
// Option 1: Use AvaUI DSL
val ui = AvaUI {
    Chip("Tag") {
        onClick = { }
    }
}

val renderer = ComposeRenderer()
ui.render(renderer)  // Works! Uses Foundation

// Option 2: Use Foundation directly (recommended)
@Composable
fun MyUI() {
    MagicChip("Tag", onClick = { })
}
```

### If You're Starting Fresh

**Recommendation:** Use **Foundation components directly** for pure Android/Desktop apps.

```kotlin
@Composable
fun NewFeature() {
    MagicTheme {
        V(spacing = 16.dp) {
            MagicText("Title", style = TextVariant.HeadlineMedium)
            MagicButton("Action", onClick = { })
        }
    }
}
```

Only use **Core + DSL** if you need cross-platform support (Android + iOS + Web).

---

## Testing Strategy

### Unit Tests for ComposeRenderer

```kotlin
class ComposeRendererTest {
    @Test
    fun `renderButton creates MagicButton with correct props`() {
        val button = ButtonComponent(
            text = "Click Me",
            buttonStyle = ButtonScope.ButtonStyle.Primary,
            onClick = { }
        )

        val renderer = ComposeRenderer()
        val result = renderer.render(button)

        // Verify result is @Composable MagicButton
        // (Use Compose testing tools)
    }

    @Test
    fun `renderChip applies selected state correctly`() {
        val chip = ChipComponent(
            label = "Tag",
            selected = true
        )

        val renderer = ComposeRenderer()
        val result = renderer.render(chip)

        // Verify ChipVariant.Filled when selected
    }
}
```

### Integration Tests for AvaCode Generator

```kotlin
class AvaCodeGeneratorTest {
    @Test
    fun `generate creates valid Kotlin Compose code from DSL`() {
        val vosContent = """
            theme Material3Light

            screen TestScreen {
                Button "Click" {
                    style Primary
                }
            }
        """

        val generator = KotlinComposeGenerator()
        val code = generator.generate(VosParser(vosContent).parse())

        // Verify code compiles and contains expected components
        assertTrue(code.contains("@Composable"))
        assertTrue(code.contains("fun TestScreen()"))
        assertTrue(code.contains("AvaUI {"))
        assertTrue(code.contains("Button(\"Click\")"))
    }
}
```

---

## Performance Considerations

### Foundation Components (Direct Compose)

**Pros:**
- ✅ Zero abstraction - direct Material3 calls
- ✅ No render() indirection
- ✅ Compose compiler optimizations apply fully
- ✅ Minimal allocations

**Cons:**
- ❌ Android/Desktop only
- ❌ Not serializable

### Core + ComposeRenderer (Indirect)

**Pros:**
- ✅ Cross-platform definitions
- ✅ Serializable (can be stored/transmitted)

**Cons:**
- ❌ Extra allocations (Component data models)
- ❌ Renderer indirection overhead
- ❌ Slightly slower (negligible for most UIs)

**Recommendation:** For performance-critical Android/Desktop apps, use **Foundation directly**. For cross-platform apps, the overhead is acceptable.

---

## Summary

### The Three Systems Working Together

1. **Foundation Components** (Direct Compose)
   - Use when: Pure Android/Desktop app
   - Benefit: Maximum performance, type safety, IDE support

2. **Core Components** (Data Models)
   - Use when: Need cross-platform (Android + iOS + Web)
   - Benefit: Single source of truth, serializable

3. **AvaCode Generator** (DSL → Code)
   - Use when: Designer handoff, server-driven UI
   - Benefit: Platform-agnostic DSL, consistent output

### Integration Points

```
.vos DSL file
    ↓ (AvaCode Parser)
AST
    ↓ (KotlinComposeGenerator)
Kotlin code with Core components
    ↓ (AvaUI DSL builder)
Component tree
    ↓ (ComposeRenderer)
Foundation @Composables
    ↓
Native UI
```

### Next Steps

1. **Implement ComposeRenderer** (8-12 hours)
2. **Update Core render() methods** (4-6 hours)
3. **Complete AvaCode generators** (12-16 hours)
4. **Enhance Foundation components** (2-4 hours)
5. **Write integration tests** (4-6 hours)
6. **Documentation** (2-4 hours)

**Total Effort:** 32-48 hours (4-6 days)

---

**This architecture provides the best of both worlds:**
- ✅ Direct Compose usage when you want it (Foundation)
- ✅ Cross-platform support when you need it (Core + Renderers)
- ✅ DSL-driven development when it makes sense (AvaCode)
- ✅ Zero duplicate work (Foundation = Android renderer)
- ✅ App Store compliant (static code generation, not runtime interpretation)

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**IDEAMagic System** ✨💡

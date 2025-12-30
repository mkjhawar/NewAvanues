# Developer Manual - Shared Utilities Section

**This document contains new sections to be added to DEVELOPER-MANUAL.md**

---

## 30. Shared Utilities (avaelements.common)

AVAElements provides a comprehensive set of shared utilities that eliminate code duplication across platform renderers. These utilities handle common tasks like alignment conversion, color manipulation, property extraction, and spacing calculations.

### 30.1 Overview

**Package:** `com.augmentalis.avaelements.common.*`

**Purpose:** Platform-agnostic utilities shared across Android, iOS, Desktop, and Web renderers.

**Benefits:**
- Zero code duplication across platforms
- Consistent behavior and calculations
- RTL (Right-to-Left) layout support built-in
- WCAG accessibility compliance for colors
- Type-safe property extraction

**Subpackages:**
```
com.augmentalis.avaelements.common/
├── alignment/      # RTL-aware alignment conversion
├── color/          # Color manipulation and WCAG utilities
├── properties/     # Type-safe property extraction
└── spacing/        # Spacing, padding, borders, shadows
```

---

### 30.2 Alignment Utilities

**Package:** `com.augmentalis.avaelements.common.alignment`

#### Purpose

Provides platform-agnostic alignment and arrangement conversion with built-in RTL (Right-to-Left) layout support. Eliminates the need for each platform renderer to implement its own alignment conversion logic.

#### Key Components

**Alignment Types:**
```kotlin
enum class WrapAlignment {
    Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly
}

enum class MainAxisAlignment {
    Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly
}

enum class CrossAxisAlignment {
    Start, End, Center, Stretch, Baseline
}

enum class HorizontalAlignment { Start, End, Center }
enum class VerticalAlignment { Top, Bottom, Center }
enum class LayoutDirection { Ltr, Rtl }
```

**Arrangement Results:**
```kotlin
sealed class HorizontalArrangement {
    object Start : HorizontalArrangement()
    object End : HorizontalArrangement()
    object Center : HorizontalArrangement()
    object SpaceBetween : HorizontalArrangement()
    object SpaceAround : HorizontalArrangement()
    object SpaceEvenly : HorizontalArrangement()
    data class SpacedBy(val spacing: Float) : HorizontalArrangement()
}

sealed class VerticalArrangement {
    object Top : VerticalArrangement()
    object Bottom : VerticalArrangement()
    object Center : VerticalArrangement()
    object SpaceBetween : VerticalArrangement()
    object SpaceAround : VerticalArrangement()
    object SpaceEvenly : VerticalArrangement()
    data class SpacedBy(val spacing: Float) : VerticalArrangement()
}
```

#### Usage Examples

**Basic Alignment Conversion:**
```kotlin
import com.augmentalis.avaelements.common.alignment.*

// Convert WrapAlignment to HorizontalArrangement (RTL-aware)
val layoutDir = LayoutDirection.Rtl
val alignment = WrapAlignment.Start
val horizontal = AlignmentConverter.wrapToHorizontal(alignment, layoutDir)
// Result: HorizontalArrangement.End (reversed for RTL)

// Convert MainAxisAlignment to VerticalArrangement
val mainAxis = MainAxisAlignment.SpaceBetween
val vertical = AlignmentConverter.mainAxisToVertical(mainAxis)
// Result: VerticalArrangement.SpaceBetween
```

**Extension Functions:**
```kotlin
// Extension functions for convenience
val horizontal = WrapAlignment.Start.toHorizontalArrangement(LayoutDirection.Ltr)
val vertical = MainAxisAlignment.Center.toVerticalArrangement()
val hAlign = CrossAxisAlignment.End.toHorizontalAlignment()
val vAlign = CrossAxisAlignment.Start.toVerticalAlignment()
```

**Platform Renderer Integration:**
```kotlin
// Android Compose renderer example
@Composable
fun renderRow(props: Map<String, Any?>) {
    val mainAxis = props.getEnum("mainAxisAlignment", MainAxisAlignment.Start)
    val crossAxis = props.getEnum("crossAxisAlignment", CrossAxisAlignment.Center)
    val layoutDir = LocalLayoutDirection.current

    Row(
        horizontalArrangement = when (
            AlignmentConverter.mainAxisToHorizontal(mainAxis, layoutDir.toCommon())
        ) {
            HorizontalArrangement.Start -> Arrangement.Start
            HorizontalArrangement.End -> Arrangement.End
            HorizontalArrangement.Center -> Arrangement.Center
            // ... etc
        },
        verticalAlignment = when (
            AlignmentConverter.crossAxisToVertical(crossAxis)
        ) {
            VerticalAlignment.Top -> Alignment.Top
            VerticalAlignment.Bottom -> Alignment.Bottom
            VerticalAlignment.Center -> Alignment.CenterVertically
        }
    ) {
        // Content
    }
}
```

#### RTL Support

The alignment converter automatically handles RTL layouts:

```kotlin
// LTR: Start → Left, End → Right
val ltr = AlignmentConverter.wrapToHorizontal(
    WrapAlignment.Start,
    LayoutDirection.Ltr
)
// Result: HorizontalArrangement.Start

// RTL: Start → Right, End → Left (reversed)
val rtl = AlignmentConverter.wrapToHorizontal(
    WrapAlignment.Start,
    LayoutDirection.Rtl
)
// Result: HorizontalArrangement.End
```

#### Platform-Specific Notes

- **Android:** Convert to Compose `Arrangement` and `Alignment` types
- **iOS:** Map to SwiftUI `HorizontalAlignment` and `VerticalAlignment`
- **Desktop:** Same as Android (uses Compose Desktop)
- **Web:** Convert to CSS Flexbox `justify-content` and `align-items`

---

### 30.3 Color Utilities

**Package:** `com.augmentalis.avaelements.common.color`

#### Purpose

Platform-agnostic color manipulation utilities with WCAG accessibility compliance checking. Provides consistent color calculations across all platforms.

#### Key Components

**UniversalColor Class:**
```kotlin
data class UniversalColor(
    val alpha: Float,  // 0.0 - 1.0
    val red: Float,    // 0.0 - 1.0
    val green: Float,  // 0.0 - 1.0
    val blue: Float    // 0.0 - 1.0
) {
    companion object {
        val Transparent = UniversalColor(0f, 0f, 0f, 0f)
        val Black = UniversalColor(1f, 0f, 0f, 0f)
        val White = UniversalColor(1f, 1f, 1f, 1f)
        val Red = UniversalColor(1f, 1f, 0f, 0f)
        val Green = UniversalColor(1f, 0f, 1f, 0f)
        val Blue = UniversalColor(1f, 0f, 0f, 1f)

        fun fromHex(hex: String): UniversalColor
        fun fromArgb(argb: Int): UniversalColor
        fun fromRgb(rgb: Int): UniversalColor
        fun fromHsl(h: Float, s: Float, l: Float, a: Float = 1f): UniversalColor
    }

    fun toArgb(): Int
    fun toHex(): String
    fun toHsl(): Triple<Float, Float, Float>

    val luminance: Float
    val isDark: Boolean
    val isLight: Boolean
}
```

**ColorUtils Object:**
```kotlin
object ColorUtils {
    fun lighten(color: UniversalColor, factor: Float): UniversalColor
    fun darken(color: UniversalColor, factor: Float): UniversalColor
    fun saturate(color: UniversalColor, factor: Float): UniversalColor
    fun withAlpha(color: UniversalColor, alpha: Float): UniversalColor
    fun mix(color1: UniversalColor, color2: UniversalColor, ratio: Float): UniversalColor
    fun contrastingForeground(background: UniversalColor): UniversalColor
    fun contrastRatio(color1: UniversalColor, color2: UniversalColor): Float
    fun meetsWcagAA(fg: UniversalColor, bg: UniversalColor): Boolean
    fun meetsWcagAAA(fg: UniversalColor, bg: UniversalColor): Boolean
    fun invert(color: UniversalColor): UniversalColor
    fun grayscale(color: UniversalColor): UniversalColor
    fun shiftHue(color: UniversalColor, degrees: Float): UniversalColor
    fun complementary(color: UniversalColor): UniversalColor
    fun triadic(color: UniversalColor): List<UniversalColor>
    fun analogous(color: UniversalColor): List<UniversalColor>
}
```

#### Usage Examples

**Color Creation:**
```kotlin
import com.augmentalis.avaelements.common.color.*

// From hex string
val blue = UniversalColor.fromHex("#1E88E5")
val blueAlpha = UniversalColor.fromHex("#801E88E5")  // 50% opacity

// From ARGB integer
val red = UniversalColor.fromArgb(0xFFFF0000.toInt())

// From HSL
val green = UniversalColor.fromHsl(120f, 0.8f, 0.5f)

// Pre-defined colors
val black = UniversalColor.Black
val white = UniversalColor.White
```

**Color Manipulation:**
```kotlin
val primary = UniversalColor.fromHex("#1E88E5")

// Lighten by 20%
val lighter = ColorUtils.lighten(primary, 0.2f)
// Or using extension: val lighter = primary.lighten(0.2f)

// Darken by 30%
val darker = ColorUtils.darken(primary, 0.3f)

// Adjust saturation
val saturated = ColorUtils.saturate(primary, 0.5f)    // More saturated
val desaturated = ColorUtils.saturate(primary, -0.5f) // Less saturated

// Adjust opacity
val translucent = ColorUtils.withAlpha(primary, 0.5f)

// Mix two colors
val purple = ColorUtils.mix(
    UniversalColor.fromHex("#FF0000"),  // Red
    UniversalColor.fromHex("#0000FF"),  // Blue
    0.5f                                 // 50/50 mix
)
```

**WCAG Accessibility Checking:**
```kotlin
val background = UniversalColor.fromHex("#1E88E5")  // Blue
val foreground = UniversalColor.White

// Check contrast ratio
val ratio = ColorUtils.contrastRatio(foreground, background)
// Returns: 4.98:1

// Check WCAG compliance
val meetsAA = ColorUtils.meetsWcagAA(foreground, background)
// Returns: true (needs 4.5:1 for normal text)

val meetsAAA = ColorUtils.meetsWcagAAA(foreground, background)
// Returns: false (needs 7:1 for normal text)

// Get optimal contrasting foreground
val optimalFg = ColorUtils.contrastingForeground(background)
// Returns: White or Black based on luminance
```

**Color Schemes:**
```kotlin
val primary = UniversalColor.fromHex("#1E88E5")

// Complementary (180° opposite)
val complement = ColorUtils.complementary(primary)

// Triadic (120° apart)
val triad = ColorUtils.triadic(primary)
// Returns: [primary, color120°, color240°]

// Analogous (30° apart)
val analogous = ColorUtils.analogous(primary)
// Returns: [color-30°, primary, color+30°]

// Custom hue shift
val shifted = ColorUtils.shiftHue(primary, 45f)
```

**Color Conversions:**
```kotlin
val color = UniversalColor.fromHex("#1E88E5")

// To ARGB integer
val argb: Int = color.toArgb()
// Returns: 0xFF1E88E5.toInt()

// To hex string
val hex: String = color.toHex()
// Returns: "#FF1E88E5"

// To HSL
val (hue, saturation, lightness) = color.toHsl()
// Returns: (207°, 0.78, 0.51)

// Check luminance
val lum = color.luminance  // 0.0 - 1.0
val isDark = color.isDark  // true if luminance < 0.5
val isLight = color.isLight // true if luminance >= 0.5
```

**Theme Generation:**
```kotlin
// Generate a Material 3 style color scheme
fun generateColorScheme(primary: UniversalColor): Map<String, UniversalColor> {
    return mapOf(
        "primary" to primary,
        "primaryContainer" to primary.lighten(0.8f),
        "onPrimary" to primary.contrastingForeground(),
        "secondary" to primary.shiftHue(30f),
        "tertiary" to primary.shiftHue(-30f),
        "error" to UniversalColor.fromHex("#B00020"),
        "background" to UniversalColor.White,
        "surface" to UniversalColor.White
    )
}
```

#### Platform-Specific Notes

- **Android:** Convert to Compose `Color` using `Color(color.toArgb())`
- **iOS:** Convert to SwiftUI `Color` using RGBA components
- **Desktop:** Same as Android (uses Compose)
- **Web:** Convert to CSS color strings using `toHex()` or RGBA values

---

### 30.4 Property Extraction Utilities

**Package:** `com.augmentalis.avaelements.common.properties`

#### Purpose

Type-safe property extraction from component property maps with automatic type conversion, default values, and validation.

#### Key Components

**PropertyExtractor Object:**
```kotlin
object PropertyExtractor {
    // Basic types
    fun getString(props: Map<String, Any?>, key: String, default: String = ""): String
    fun getBoolean(props: Map<String, Any?>, key: String, default: Boolean = false): Boolean
    fun getInt(props: Map<String, Any?>, key: String, default: Int = 0): Int
    fun getFloat(props: Map<String, Any?>, key: String, default: Float = 0f): Float
    fun getDouble(props: Map<String, Any?>, key: String, default: Double = 0.0): Double
    fun getLong(props: Map<String, Any?>, key: String, default: Long = 0L): Long

    // Enums
    inline fun <reified T : Enum<T>> getEnum(
        props: Map<String, Any?>,
        key: String,
        default: T
    ): T

    // Collections
    fun getStringList(props: Map<String, Any?>, key: String, default: List<String>): List<String>
    fun getIntList(props: Map<String, Any?>, key: String, default: List<Int>): List<Int>
    fun getFloatList(props: Map<String, Any?>, key: String, default: List<Float>): List<Float>
    fun <T> getList(props: Map<String, Any?>, key: String, transform: (Any) -> T?): List<T>

    // Maps
    fun getMap(props: Map<String, Any?>, key: String, default: Map<String, Any?>): Map<String, Any?>
    fun getStringMap(props: Map<String, Any?>, key: String, default: Map<String, String>): Map<String, String>

    // Special types
    fun getColorArgb(props: Map<String, Any?>, key: String, default: Int): Int
    fun getDimension(props: Map<String, Any?>, key: String, default: Float): DimensionValue

    // Callbacks
    fun getCallback(props: Map<String, Any?>, key: String): (() -> Unit)?
    fun <T> getCallback1(props: Map<String, Any?>, key: String): ((T) -> Unit)?
}
```

**DimensionValue:**
```kotlin
data class DimensionValue(
    val value: Float,
    val unit: DimensionUnit
) {
    fun toPx(density: Float, fontScale: Float = 1f): Float
    fun toDp(density: Float): Float

    companion object {
        fun dp(value: Float): DimensionValue
        fun sp(value: Float): DimensionValue
        fun px(value: Float): DimensionValue
        fun percent(value: Float): DimensionValue
    }
}

enum class DimensionUnit {
    Dp,      // Density-independent pixels
    Sp,      // Scale-independent pixels (for text)
    Px,      // Raw pixels
    Percent  // Percentage of parent
}
```

#### Usage Examples

**Basic Property Extraction:**
```kotlin
import com.augmentalis.avaelements.common.properties.*

val props: Map<String, Any?> = mapOf(
    "label" to "Click me",
    "enabled" to true,
    "width" to 200,
    "opacity" to 0.8f
)

// String extraction
val label = PropertyExtractor.getString(props, "label", "Default")
// Result: "Click me"

// Boolean extraction (with type conversion)
val enabled = PropertyExtractor.getBoolean(props, "enabled", false)
// Result: true

// Numeric extraction
val width = PropertyExtractor.getInt(props, "width", 100)
val opacity = PropertyExtractor.getFloat(props, "opacity", 1.0f)
```

**Extension Functions:**
```kotlin
// Extension functions for cleaner syntax
val props: Map<String, Any?> = mapOf(
    "title" to "Hello",
    "count" to 42,
    "visible" to true
)

val title = props.getString("title", "Untitled")
val count = props.getInt("count", 0)
val visible = props.getBoolean("visible", false)
```

**Enum Extraction:**
```kotlin
enum class ButtonType { Primary, Secondary, Text }

val props: Map<String, Any?> = mapOf(
    "type" to "primary"  // String value
)

// Case-insensitive, handles dash/underscore/space
val type = PropertyExtractor.getEnum(props, "type", ButtonType.Primary)
// Result: ButtonType.Primary

// Also accepts enum instances directly
val props2: Map<String, Any?> = mapOf(
    "type" to ButtonType.Secondary
)
val type2 = props2.getEnum("type", ButtonType.Primary)
// Result: ButtonType.Secondary
```

**Color Extraction:**
```kotlin
val props: Map<String, Any?> = mapOf(
    "color1" to "#FF0000",           // Hex string
    "color2" to 0xFFFF0000.toInt(),  // ARGB integer
    "color3" to "blue"               // Named color
)

val color1 = PropertyExtractor.getColorArgb(props, "color1", 0xFF000000.toInt())
// Result: 0xFFFF0000.toInt() (red)

val color2 = props.getColorArgb("color2", 0xFF000000.toInt())
val color3 = props.getColorArgb("color3", 0xFF000000.toInt())

// Supported formats:
// - Hex: #RGB, #RRGGBB, #AARRGGBB
// - Named: black, white, red, green, blue, etc.
// - Integer: ARGB format
```

**Dimension Extraction:**
```kotlin
val props: Map<String, Any?> = mapOf(
    "width" to "200dp",
    "height" to "48sp",
    "margin" to "16",  // Default unit: dp
    "borderWidth" to "2px"
)

val width = PropertyExtractor.getDimension(props, "width", 0f)
// Result: DimensionValue(200f, DimensionUnit.Dp)

val height = props.getDimension("height", 0f)
// Result: DimensionValue(48f, DimensionUnit.Sp)

// Convert to pixels (requires density)
val density = 2.0f  // 2x density (xxhdpi)
val fontScale = 1.15f  // User's font scale

val widthPx = width.toPx(density)
// Result: 400px (200dp * 2.0)

val heightPx = height.toPx(density, fontScale)
// Result: 110.4px (48sp * 2.0 * 1.15)

// Supported units: dp, sp, px, %
```

**List Extraction:**
```kotlin
val props: Map<String, Any?> = mapOf(
    "items" to listOf("Apple", "Banana", "Cherry"),
    "numbers" to listOf(1, 2, 3, 4, 5),
    "csv" to "Red,Green,Blue"  // Also supports CSV strings
)

val items = PropertyExtractor.getStringList(props, "items", emptyList())
// Result: ["Apple", "Banana", "Cherry"]

val numbers = props.getIntList("numbers", emptyList())
// Result: [1, 2, 3, 4, 5]

val colors = props.getStringList("csv", emptyList())
// Result: ["Red", "Green", "Blue"]

// Custom list extraction with transform
val doubled = PropertyExtractor.getList(props, "numbers", { (it as? Number)?.toInt()?.let { it * 2 } })
// Result: [2, 4, 6, 8, 10]
```

**Map Extraction:**
```kotlin
val props: Map<String, Any?> = mapOf(
    "metadata" to mapOf(
        "author" to "John",
        "version" to "1.0"
    ),
    "config" to mapOf(
        "timeout" to 5000,
        "retries" to 3
    )
)

val metadata = PropertyExtractor.getStringMap(props, "metadata", emptyMap())
// Result: {"author": "John", "version": "1.0"}

val config = props.getMap("config", emptyMap())
// Result: {"timeout": 5000, "retries": 3}
```

**Callback Extraction:**
```kotlin
val props: Map<String, Any?> = mapOf(
    "onClick" to { println("Clicked!") },
    "onValueChange" to { value: String -> println("Value: $value") }
)

val onClick = PropertyExtractor.getCallback(props, "onClick")
onClick?.invoke()  // Prints: "Clicked!"

val onValueChange = PropertyExtractor.getCallback1<String>(props, "onValueChange")
onValueChange?.invoke("Hello")  // Prints: "Value: Hello"
```

**Component Integration Example:**
```kotlin
// Button component renderer
@Composable
fun renderButton(props: Map<String, Any?>) {
    val label = props.getString("label", "Button")
    val enabled = props.getBoolean("enabled", true)
    val type = props.getEnum("type", ButtonType.Primary)
    val width = props.getDimension("width", 0f)
    val height = props.getDimension("height", 48f)
    val backgroundColor = props.getColorArgb("backgroundColor", 0xFF1E88E5.toInt())
    val onClick = props.getCallback("onClick")

    Button(
        onClick = { onClick?.invoke() },
        enabled = enabled,
        modifier = Modifier
            .width(width.toDp(LocalDensity.current.density).dp)
            .height(height.toDp(LocalDensity.current.density).dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(backgroundColor)
        )
    ) {
        Text(label)
    }
}
```

#### Platform-Specific Notes

- **Android:** Use `LocalDensity.current.density` for dimension conversions
- **iOS:** Get scale factor from `UIScreen.main.scale`
- **Desktop:** Use platform-specific density APIs
- **Web:** Convert dimensions to CSS units (px, rem, %)

---

### 30.5 Spacing Utilities

**Package:** `com.augmentalis.avaelements.common.spacing`

#### Purpose

Platform-agnostic spacing, padding, margin, border, and shadow utilities with standard spacing scales for consistent layouts.

#### Key Components

**EdgeInsets:**
```kotlin
data class EdgeInsets(
    val start: Float = 0f,
    val top: Float = 0f,
    val end: Float = 0f,
    val bottom: Float = 0f
) {
    companion object {
        val Zero = EdgeInsets(0f, 0f, 0f, 0f)
        fun symmetric(horizontal: Float, vertical: Float): EdgeInsets
        fun all(value: Float): EdgeInsets
        fun horizontal(value: Float): EdgeInsets
        fun vertical(value: Float): EdgeInsets
        fun fromMap(map: Map<String, Any?>): EdgeInsets
    }

    val horizontalTotal: Float
    val verticalTotal: Float
    val isZero: Boolean

    operator fun plus(other: EdgeInsets): EdgeInsets
    operator fun times(factor: Float): EdgeInsets
}
```

**Size:**
```kotlin
data class Size(
    val width: Float,
    val height: Float
) {
    companion object {
        val Zero = Size(0f, 0f)
        val Unspecified = Size(Float.NaN, Float.NaN)
        fun square(dimension: Float): Size
    }

    val isSpecified: Boolean
    val aspectRatio: Float

    operator fun times(factor: Float): Size
    operator fun div(factor: Float): Size
}
```

**CornerRadius:**
```kotlin
data class CornerRadius(
    val topStart: Float = 0f,
    val topEnd: Float = 0f,
    val bottomStart: Float = 0f,
    val bottomEnd: Float = 0f
) {
    companion object {
        val Zero = CornerRadius(0f, 0f, 0f, 0f)
        fun all(radius: Float): CornerRadius
        fun from(value: Any?): CornerRadius
    }

    val isUniform: Boolean
    val uniform: Float

    operator fun times(factor: Float): CornerRadius
}
```

**Border:**
```kotlin
data class Border(
    val width: Float = 0f,
    val color: Int = 0xFF000000.toInt(),  // ARGB
    val style: BorderStyle = BorderStyle.Solid
) {
    companion object {
        val None = Border(0f)
        fun solid(width: Float, color: Int): Border
        fun dashed(width: Float, color: Int): Border
        fun dotted(width: Float, color: Int): Border
    }

    val isVisible: Boolean
}

enum class BorderStyle { Solid, Dashed, Dotted, None }
```

**Shadow:**
```kotlin
data class Shadow(
    val color: Int = 0x29000000,  // Semi-transparent black
    val blurRadius: Float = 0f,
    val spreadRadius: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    companion object {
        val None = Shadow()
        fun elevation(dp: Float, color: Int = 0x29000000): Shadow
        fun fromMap(map: Map<String, Any?>): Shadow
    }

    val isVisible: Boolean
}
```

**SpacingScale:**
```kotlin
object SpacingScale {
    const val Base = 4f

    const val None = 0f
    const val XXS = 2f    // 0.5x base
    const val XS = 4f     // 1x base
    const val SM = 8f     // 2x base
    const val MD = 12f    // 3x base
    const val LG = 16f    // 4x base
    const val XL = 24f    // 6x base
    const val XXL = 32f   // 8x base
    const val XXXL = 48f  // 12x base

    fun get(multiplier: Float): Float
    fun byName(name: String): Float
}
```

#### Usage Examples

**EdgeInsets (Padding/Margin):**
```kotlin
import com.augmentalis.avaelements.common.spacing.*

// Create edge insets
val uniform = EdgeInsets.all(16f)
// Result: EdgeInsets(16f, 16f, 16f, 16f)

val symmetric = EdgeInsets.symmetric(horizontal = 24f, vertical = 16f)
// Result: EdgeInsets(24f, 16f, 24f, 16f)

val horizontal = EdgeInsets.horizontal(20f)
// Result: EdgeInsets(20f, 0f, 20f, 0f)

val vertical = EdgeInsets.vertical(12f)
// Result: EdgeInsets(0f, 12f, 0f, 12f)

val custom = EdgeInsets(start = 8f, top = 16f, end = 8f, bottom = 24f)

// From map (flexible keys)
val fromMap = EdgeInsets.fromMap(mapOf(
    "horizontal" to 16,
    "vertical" to 12
))

// Operations
val total = uniform + symmetric
val scaled = uniform * 1.5f

// Properties
val totalH = symmetric.horizontalTotal  // 48f
val totalV = symmetric.verticalTotal    // 32f
val isZero = EdgeInsets.Zero.isZero     // true
```

**Size:**
```kotlin
// Create sizes
val size = Size(200f, 100f)
val square = Size.square(100f)
val unspecified = Size.Unspecified

// Properties
val ratio = size.aspectRatio  // 2.0 (200/100)
val isValid = size.isSpecified  // true

// Operations
val doubled = size * 2f    // Size(400f, 200f)
val halved = size / 2f     // Size(100f, 50f)
```

**Corner Radius:**
```kotlin
// Uniform radius
val rounded = CornerRadius.all(8f)

// Custom per-corner
val topRounded = CornerRadius(
    topStart = 16f,
    topEnd = 16f,
    bottomStart = 0f,
    bottomEnd = 0f
)

// From value (accepts number or map)
val fromNumber = CornerRadius.from(12)
val fromMap = CornerRadius.from(mapOf(
    "top" to 16,
    "bottom" to 0
))

// Check uniformity
if (rounded.isUniform) {
    val radius = rounded.uniform  // 8f
}

// Scale radius
val larger = rounded * 1.5f
// Result: CornerRadius(12f, 12f, 12f, 12f)
```

**Border:**
```kotlin
// Create borders
val solidBorder = Border.solid(2f, 0xFF1E88E5.toInt())
val dashedBorder = Border.dashed(1f, 0xFF000000.toInt())
val dottedBorder = Border.dotted(1f, 0xFF888888.toInt())
val noBorder = Border.None

// Check visibility
if (solidBorder.isVisible) {
    // Render border
}

// Use in component
data class BoxStyle(
    val border: Border = Border.None,
    val cornerRadius: CornerRadius = CornerRadius.Zero
)
```

**Shadow:**
```kotlin
// Material elevation shadow
val elevation4 = Shadow.elevation(4f)
// Result: Shadow with blur and offset calculated from elevation

val elevation8 = Shadow.elevation(8f)

// Custom shadow
val customShadow = Shadow(
    color = 0x40000000,  // 25% black
    blurRadius = 10f,
    spreadRadius = 2f,
    offsetX = 0f,
    offsetY = 4f
)

// From map
val fromMap = Shadow.fromMap(mapOf(
    "blur" to 8,
    "x" to 2,
    "y" to 4,
    "color" to 0x20000000
))

// Check visibility
if (customShadow.isVisible) {
    // Render shadow
}
```

**Spacing Scale:**
```kotlin
// Use standard spacing values
val padding = SpacingScale.MD    // 12dp
val margin = SpacingScale.LG     // 16dp
val gap = SpacingScale.XL        // 24dp

// Get by multiplier
val custom = SpacingScale.get(5f)  // 20dp (5 * 4dp base)

// Get by name
val small = SpacingScale.byName("sm")     // 8dp
val medium = SpacingScale.byName("md")    // 12dp
val large = SpacingScale.byName("large")  // 16dp

// Use in layouts
@Composable
fun MyLayout() {
    Column(
        modifier = Modifier.padding(SpacingScale.MD.dp)
    ) {
        Spacer(modifier = Modifier.height(SpacingScale.LG.dp))
        // Content
    }
}
```

**Complete Component Example:**
```kotlin
@Composable
fun renderCard(props: Map<String, Any?>) {
    val padding = EdgeInsets.fromMap(props.getMap("padding", emptyMap()))
    val margin = EdgeInsets.fromMap(props.getMap("margin", emptyMap()))
    val cornerRadius = CornerRadius.from(props["cornerRadius"])
    val border = Border.solid(
        props.getFloat("borderWidth", 0f),
        props.getColorArgb("borderColor", 0xFF000000.toInt())
    )
    val shadow = Shadow.elevation(props.getFloat("elevation", 0f))

    Box(
        modifier = Modifier
            .padding(
                start = margin.start.dp,
                top = margin.top.dp,
                end = margin.end.dp,
                bottom = margin.bottom.dp
            )
            .shadow(
                elevation = shadow.blurRadius.dp,
                shape = RoundedCornerShape(
                    topStart = cornerRadius.topStart.dp,
                    topEnd = cornerRadius.topEnd.dp,
                    bottomStart = cornerRadius.bottomStart.dp,
                    bottomEnd = cornerRadius.bottomEnd.dp
                )
            )
            .then(
                if (border.isVisible) {
                    Modifier.border(
                        width = border.width.dp,
                        color = Color(border.color),
                        shape = RoundedCornerShape(cornerRadius.topStart.dp)
                    )
                } else Modifier
            )
            .padding(
                start = padding.start.dp,
                top = padding.top.dp,
                end = padding.end.dp,
                bottom = padding.bottom.dp
            )
    ) {
        // Card content
    }
}
```

#### Platform-Specific Notes

- **Android:** Convert to Compose `Dp`, `PaddingValues`, `Shape`, `Shadow`
- **iOS:** Map to SwiftUI `EdgeInsets`, `CGFloat`, `cornerRadius`, `shadow`
- **Desktop:** Same as Android (uses Compose Desktop)
- **Web:** Convert to CSS `padding`, `margin`, `border-radius`, `box-shadow`

---

### 30.6 Best Practices

**Use Shared Utilities Everywhere:**
```kotlin
// ✅ Good - Use shared utilities
import com.augmentalis.avaelements.common.alignment.*
val arrangement = AlignmentConverter.wrapToHorizontal(alignment, layoutDir)

// ❌ Bad - Don't duplicate conversion logic
fun convertAlignment(alignment: WrapAlignment): HorizontalArrangement {
    // Duplicated code...
}
```

**Leverage Extension Functions:**
```kotlin
// ✅ Good - Clean and readable
val label = props.getString("label", "Default")
val enabled = props.getBoolean("enabled", true)
val color = props.getColorArgb("color", 0xFF000000.toInt())

// ❌ Bad - Verbose
val label = PropertyExtractor.getString(props, "label", "Default")
val enabled = PropertyExtractor.getBoolean(props, "enabled", true)
```

**Use Standard Spacing Scale:**
```kotlin
// ✅ Good - Consistent spacing
val padding = EdgeInsets.all(SpacingScale.MD)
val margin = SpacingScale.LG

// ❌ Bad - Magic numbers
val padding = EdgeInsets.all(13f)
val margin = 17f
```

**Check WCAG Compliance:**
```kotlin
// ✅ Good - Ensure accessibility
val bg = UniversalColor.fromHex("#1E88E5")
val fg = if (ColorUtils.meetsWcagAA(UniversalColor.White, bg)) {
    UniversalColor.White
} else {
    UniversalColor.Black
}

// ⚠️ Warning - May not be accessible
val fg = UniversalColor.White  // Might not have sufficient contrast
```

**Handle RTL Layouts:**
```kotlin
// ✅ Good - RTL-aware
val arrangement = alignment.toHorizontalArrangement(layoutDirection)

// ❌ Bad - Assumes LTR
val arrangement = when (alignment) {
    WrapAlignment.Start -> HorizontalArrangement.Start  // Wrong for RTL
    // ...
}
```

---

## 31. Unified Input System

AVAElements provides a comprehensive unified input system that works consistently across all platforms and input devices, including the unique VoiceCursor system for voice-controlled navigation on Android VoiceOS.

### 31.1 Overview

**Package:** `com.augmentalis.avaelements.input`

**Purpose:** Platform-agnostic input event handling and state management for all input methods.

**Supported Input Methods:**
- **Touch:** Finger touch on touchscreens (iOS, Android, Web)
- **Mouse:** Cursor devices (Desktop, DeX, iPadOS with mouse)
- **Trackpad:** Gesture input (Desktop, iPadOS)
- **Keyboard:** Physical and virtual keyboards (All platforms)
- **Stylus:** Pen input (S Pen, Apple Pencil, Wacom)
- **VoiceCursor:** Voice-controlled cursor (Android VoiceOS only)
- **Gamepad:** Game controllers (Desktop, Android)

**Key Features:**
- Unified event types across platforms
- Consistent state tracking (hover, press, focus)
- Input source detection (touch vs mouse vs voice)
- Platform capability detection
- VoiceCursor integration for voice-controlled UIs

---

### 31.2 Input Events

All input events inherit from the sealed `InputEvent` class with a common timestamp and source.

#### Event Types

**Pointer Events:**
```kotlin
sealed class InputEvent {
    // Single tap/click
    data class Tap(
        val position: Offset,
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()

    // Double tap/click
    data class DoubleTap(
        val position: Offset,
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()

    // Long press
    data class LongPress(
        val position: Offset,
        val source: InputSource,
        val duration: Long = 500L,
        val timestamp: Long
    ) : InputEvent()

    // Hover enter/move/exit
    data class HoverEnter(val position: Offset, ...) : InputEvent()
    data class HoverMove(val position: Offset, ...) : InputEvent()
    data class HoverExit(val position: Offset, ...) : InputEvent()
}
```

**Drag Events:**
```kotlin
sealed class InputEvent {
    data class DragStart(val position: Offset, ...) : InputEvent()
    data class Drag(val position: Offset, val delta: Offset, ...) : InputEvent()
    data class DragEnd(val position: Offset, val velocity: Velocity, ...) : InputEvent()
    data class DragCancel(...) : InputEvent()
}
```

**Scroll Events:**
```kotlin
sealed class InputEvent {
    data class Scroll(
        val delta: Offset,
        val scrollSource: ScrollSource,  // MouseWheel, Trackpad, Touch, etc.
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()

    data class Fling(
        val velocity: Velocity,
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()
}

enum class ScrollSource {
    MouseWheel, Trackpad, Touch, Keyboard, Voice, Programmatic
}
```

**Keyboard Events:**
```kotlin
sealed class InputEvent {
    data class KeyDown(
        val key: Key,
        val modifiers: KeyModifiers,
        val isRepeat: Boolean = false,
        val source: InputSource = InputSource.Keyboard,
        val timestamp: Long
    ) : InputEvent()

    data class KeyUp(
        val key: Key,
        val modifiers: KeyModifiers,
        val source: InputSource = InputSource.Keyboard,
        val timestamp: Long
    ) : InputEvent()

    data class TextInput(
        val text: String,
        val source: InputSource = InputSource.Keyboard,
        val timestamp: Long
    ) : InputEvent()
}

// Key identifiers
enum class Key {
    A, B, C, ..., Z,
    Num0, Num1, ..., Num9,
    F1, F2, ..., F12,
    Tab, Enter, Escape, Space, Backspace, Delete,
    ArrowUp, ArrowDown, ArrowLeft, ArrowRight,
    Home, End, PageUp, PageDown,
    ShiftLeft, ShiftRight, CtrlLeft, CtrlRight,
    AltLeft, AltRight, MetaLeft, MetaRight,
    // ...
    Unknown
}

data class KeyModifiers(
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false  // Cmd on Mac, Win on Windows
)
```

**Focus Events:**
```kotlin
sealed class InputEvent {
    data class FocusGained(
        val focusSource: FocusSource,
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()

    data class FocusLost(
        val source: InputSource,
        val timestamp: Long
    ) : InputEvent()
}

enum class FocusSource {
    Keyboard,  // Tab navigation
    Mouse,     // Click to focus
    Touch,     // Tap to focus
    Voice,     // Voice command
    Programmatic
}
```

**VoiceCursor Events (Android VoiceOS):**
```kotlin
sealed class InputEvent {
    // Voice command received
    data class VoiceCommand(
        val command: String,
        val parameters: Map<String, Any> = emptyMap(),
        val confidence: Float = 1.0f,
        val source: InputSource = InputSource.VoiceCursor,
        val timestamp: Long
    ) : InputEvent()

    // VoiceCursor position changed (head tracking)
    data class VoiceCursorMove(
        val position: Offset,
        val source: InputSource = InputSource.VoiceCursor,
        val timestamp: Long
    ) : InputEvent()

    // VoiceCursor click (gaze or voice-triggered)
    data class VoiceCursorClick(
        val clickType: ClickType,  // Single, Double, LongPress
        val position: Offset,
        val source: InputSource = InputSource.VoiceCursor,
        val timestamp: Long
    ) : InputEvent()

    // VoiceCursor hover enter/exit
    data class VoiceCursorEnter(val position: Offset, ...) : InputEvent()
    data class VoiceCursorExit(val position: Offset, ...) : InputEvent()
}
```

**Stylus Events:**
```kotlin
sealed class InputEvent {
    data class StylusPressure(
        val position: Offset,
        val pressure: Float,  // 0.0 - 1.0
        val tilt: Float,      // degrees
        val source: InputSource = InputSource.Stylus,
        val timestamp: Long
    ) : InputEvent()

    data class StylusButton(
        val button: Int,
        val isPressed: Boolean,
        val source: InputSource = InputSource.Stylus,
        val timestamp: Long
    ) : InputEvent()
}
```

#### Usage Example

```kotlin
fun handleInputEvent(event: InputEvent) {
    when (event) {
        is InputEvent.Tap -> {
            println("Tapped at (${event.position.x}, ${event.position.y})")
            println("Source: ${event.source}")
        }

        is InputEvent.VoiceCommand -> {
            println("Voice command: ${event.command}")
            println("Parameters: ${event.parameters}")
            println("Confidence: ${event.confidence}")
        }

        is InputEvent.KeyDown -> {
            if (event.modifiers.ctrl && event.key == Key.C) {
                // Handle Ctrl+C
            }
        }

        is InputEvent.Drag -> {
            // Handle drag
            updatePosition(event.delta)
        }

        else -> {
            // Handle other events
        }
    }
}
```

---

### 31.3 Input State

The `InputState` class tracks the current interaction state of a component.

#### InputState Class

```kotlin
data class InputState(
    val isHovered: Boolean = false,       // Mouse/cursor over element
    val isPressed: Boolean = false,       // Being pressed/touched
    val isFocused: Boolean = false,       // Has keyboard focus
    val isSelected: Boolean = false,      // Selected state
    val isDragging: Boolean = false,      // Being dragged
    val isDisabled: Boolean = false,      // Disabled state
    val cursorPosition: Offset? = null,   // Current cursor position
    val inputSource: InputSource = InputSource.Unknown,
    val pressure: Float = 1.0f,           // Stylus pressure
    val tilt: Float = 0.0f                // Stylus tilt
) {
    companion object {
        val Default = InputState()
        val Hovered = InputState(isHovered = true)
        val Pressed = InputState(isPressed = true)
        val Focused = InputState(isFocused = true)
        val Disabled = InputState(isDisabled = true)
    }

    val isInteractive: Boolean
        get() = isHovered || isPressed || isFocused || isDragging

    val visualState: VisualState
        get() = when {
            isDisabled -> VisualState.Disabled
            isPressed -> VisualState.Pressed
            isDragging -> VisualState.Dragging
            isFocused -> VisualState.Focused
            isHovered -> VisualState.Hovered
            isSelected -> VisualState.Selected
            else -> VisualState.Default
        }
}

enum class VisualState {
    Default, Hovered, Pressed, Focused, Selected, Dragging, Disabled
}
```

#### Input Source Detection

```kotlin
enum class InputSource {
    Touch,      // Finger touch on touchscreen
    Mouse,      // Mouse device
    Trackpad,   // Trackpad gestures
    Stylus,     // Stylus/pen input
    Keyboard,   // Keyboard navigation
    VoiceCursor,// VoiceOS VoiceCursor
    Gamepad,    // Game controller
    Unknown;

    val isPrecisePointer: Boolean
        get() = this in listOf(Mouse, Trackpad, Stylus, VoiceCursor)

    val supportsHover: Boolean
        get() = this in listOf(Mouse, Trackpad, Stylus, VoiceCursor)

    val isVoice: Boolean
        get() = this == VoiceCursor
}
```

#### Platform Capabilities

```kotlin
data class InputCapabilities(
    val hasTouch: Boolean = false,
    val hasMouse: Boolean = false,
    val hasKeyboard: Boolean = false,
    val hasTrackpad: Boolean = false,
    val hasStylus: Boolean = false,
    val hasVoiceCursor: Boolean = false,
    val hasGamepad: Boolean = false,
    val hasMultiTouch: Boolean = false,
    val hasPressure: Boolean = false
) {
    companion object {
        val Desktop = InputCapabilities(
            hasTouch = false,
            hasMouse = true,
            hasKeyboard = true,
            hasTrackpad = true
        )

        val Mobile = InputCapabilities(
            hasTouch = true,
            hasMouse = false,
            hasKeyboard = false,
            hasMultiTouch = true
        )

        val Web = InputCapabilities(
            hasTouch = true,
            hasMouse = true,
            hasKeyboard = true,
            hasTrackpad = true
        )
    }

    val enableHoverStates: Boolean
        get() = hasMouse || hasTrackpad || hasVoiceCursor

    val enableKeyboardNavigation: Boolean
        get() = hasKeyboard

    val primaryInputMode: InputSource
        get() = when {
            hasVoiceCursor -> InputSource.VoiceCursor
            hasTouch && !hasMouse -> InputSource.Touch
            hasMouse -> InputSource.Mouse
            hasKeyboard -> InputSource.Keyboard
            else -> InputSource.Unknown
        }
}
```

#### Usage Example

```kotlin
@Composable
fun InteractiveButton(
    label: String,
    onClick: () -> Unit
) {
    var inputState by remember { mutableStateOf(InputState.Default) }
    val capabilities = remember { detectInputCapabilities() }

    Button(
        onClick = onClick,
        modifier = Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                if (capabilities.enableHoverStates) {
                                    inputState = inputState.copy(isHovered = true)
                                }
                            }
                            PointerEventType.Exit -> {
                                inputState = inputState.copy(isHovered = false)
                            }
                            PointerEventType.Press -> {
                                inputState = inputState.copy(isPressed = true)
                            }
                            PointerEventType.Release -> {
                                inputState = inputState.copy(isPressed = false)
                            }
                        }
                    }
                }
            }
            .focusable(onFocusChanged = { focused ->
                inputState = inputState.copy(isFocused = focused.isFocused)
            }),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (inputState.visualState) {
                VisualState.Pressed -> Color.Blue.copy(alpha = 0.8f)
                VisualState.Hovered -> Color.Blue.copy(alpha = 0.9f)
                VisualState.Focused -> Color.Blue
                VisualState.Disabled -> Color.Gray
                else -> Color.Blue
            }
        )
    ) {
        Text(label)
    }
}
```

---

### 31.4 VoiceCursor Integration

The VoiceCursor system enables voice-controlled navigation for Android VoiceOS applications.

#### VoiceTarget

```kotlin
data class VoiceTarget(
    val id: String,                          // Unique identifier
    val label: String,                       // "Click [label]" command
    val bounds: Rect,                        // Screen coordinates
    val onSelect: () -> Unit,                // Selection callback
    val onHover: ((Boolean) -> Unit)? = null,  // Hover callback
    val onCursorMove: ((Offset) -> Unit)? = null,  // Cursor position
    val isEnabled: Boolean = true,           // Enable/disable
    val priority: Int = 0                    // Overlap priority
)

data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
    val height: Float
    val centerX: Float
    val centerY: Float

    fun contains(x: Float, y: Float): Boolean
    fun contains(offset: Offset): Boolean
}
```

#### VoiceCursorManager

```kotlin
interface VoiceCursorManager {
    val isAvailable: Boolean           // Is VoiceCursor available?
    val isActive: Boolean              // Is VoiceCursor currently active?
    val cursorPosition: Offset?        // Current cursor position

    fun registerTarget(target: VoiceTarget)
    fun unregisterTarget(id: String)
    fun updateTargetBounds(id: String, bounds: Rect)
    fun handleVoiceCommand(command: String, parameters: Map<String, Any>): Boolean
    fun start()
    fun stop()
    fun addListener(listener: VoiceCursorListener)
    fun removeListener(listener: VoiceCursorListener)
}

interface VoiceCursorListener {
    fun onCursorMoved(position: Offset) {}
    fun onTargetEntered(target: VoiceTarget) {}
    fun onTargetExited(target: VoiceTarget) {}
    fun onTargetSelected(target: VoiceTarget) {}
    fun onActivated() {}
    fun onDeactivated() {}
}
```

#### Voice Commands

```kotlin
object VoiceCommands {
    // Navigation
    const val CLICK = "click"
    const val DOUBLE_CLICK = "double click"
    const val LONG_PRESS = "long press"
    const val SCROLL_UP = "scroll up"
    const val SCROLL_DOWN = "scroll down"
    const val SCROLL_LEFT = "scroll left"
    const val SCROLL_RIGHT = "scroll right"

    // Focus
    const val NEXT = "next"
    const val PREVIOUS = "previous"
    const val FOCUS = "focus"
    const val SELECT = "select"

    // Input
    const val TYPE = "type"
    const val CLEAR = "clear"
    const val BACKSPACE = "backspace"

    // Cursor
    const val CURSOR_UP = "cursor up"
    const val CURSOR_DOWN = "cursor down"
    const val CURSOR_LEFT = "cursor left"
    const val CURSOR_RIGHT = "cursor right"
    const val CURSOR_CENTER = "cursor center"

    // System
    const val SHOW_CURSOR = "show cursor"
    const val HIDE_CURSOR = "hide cursor"
    const val HELP = "help"
}
```

#### Usage Example

```kotlin
// Initialize VoiceCursor (in Application.onCreate or Activity.onCreate)
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeVoiceCursor(this)
    }
}

// Make a component voice-accessible
@Composable
fun VoiceAccessibleButton(
    label: String,
    voiceLabel: String = label,
    onClick: () -> Unit
) {
    val voiceCursor = remember { getVoiceCursorManager() }
    var isVoiceHovered by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val buttonId = remember { "button_${UUID.randomUUID()}" }

    // Register voice target
    LaunchedEffect(voiceLabel, bounds) {
        if (voiceCursor.isAvailable) {
            voiceCursor.registerTarget(
                VoiceTarget(
                    id = buttonId,
                    label = voiceLabel,
                    bounds = bounds,
                    onSelect = onClick,
                    onHover = { hovered -> isVoiceHovered = hovered }
                )
            )
        }
    }

    // Unregister on dispose
    DisposableEffect(buttonId) {
        onDispose {
            voiceCursor.unregisterTarget(buttonId)
        }
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                // Update bounds when layout changes
                val rect = coordinates.boundsInRoot()
                bounds = Rect(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom
                )
                voiceCursor.updateTargetBounds(buttonId, bounds)
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isVoiceHovered) {
                Color.Blue.copy(alpha = 0.9f)  // Visual feedback
            } else {
                Color.Blue
            }
        )
    ) {
        Text(label)

        // Show voice label hint
        if (voiceCursor.isActive) {
            Text(
                text = " (\"$voiceLabel\")",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// Usage
VoiceAccessibleButton(
    label = "Submit",
    voiceLabel = "submit",
    onClick = { /* handle click */ }
)
// User can say: "Click submit" or move VoiceCursor and say "Click"
```

#### Custom Voice Commands

```kotlin
data class VoiceCursorConfig(
    val label: String,
    val enableHover: Boolean = true,
    val trackCursorPosition: Boolean = false,
    val customCommands: List<CustomVoiceCommand> = emptyList(),
    val priority: Int = 0
)

data class CustomVoiceCommand(
    val phrase: String,
    val synonyms: List<String> = emptyList(),
    val action: () -> Unit
)

// Example: Expandable component
@Composable
fun VoiceExpandable(content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    VoiceAccessibleComponent(
        config = VoiceCursorConfig(
            label = "section",
            customCommands = listOf(
                CustomVoiceCommand(
                    phrase = "expand",
                    synonyms = listOf("open", "show"),
                    action = { expanded = true }
                ),
                CustomVoiceCommand(
                    phrase = "collapse",
                    synonyms = listOf("close", "hide"),
                    action = { expanded = false }
                )
            )
        )
    ) {
        content()
    }
}
// User can say: "Expand section" or "Collapse section"
```

---

### 31.5 Platform-Specific Implementation

#### Android (Full Implementation)

**File:** `androidMain/kotlin/...input/AndroidVoiceCursor.kt`

```kotlin
class AndroidVoiceCursorManager(context: Context) : VoiceCursorManager {
    override val isAvailable: Boolean
        get() {
            // Check if VoiceOS VoiceCursor is installed
            return try {
                Class.forName("com.augmentalis.voiceos.voicecursor.VoiceCursor")
                true
            } catch (e: Exception) {
                false
            }
        }

    override fun registerTarget(target: VoiceTarget) {
        // Register with VoiceOS via reflection
        val voiceCursor = getVoiceCursorInstance() ?: return
        voiceCursor.javaClass.getMethod(
            "registerClickTarget",
            String::class.java,
            String::class.java,
            FloatArray::class.java,
            Function0::class.java
        ).invoke(
            voiceCursor,
            target.id,
            target.label,
            floatArrayOf(
                target.bounds.left,
                target.bounds.top,
                target.bounds.right,
                target.bounds.bottom
            ),
            target.onSelect
        )
    }

    // ... (see full implementation in AndroidVoiceCursor.kt)
}

// Actual implementation
actual fun getVoiceCursorManager(): VoiceCursorManager {
    val context = androidContext
    return if (context != null) {
        AndroidVoiceCursorManager.getInstance(context)
    } else {
        NoOpVoiceCursorManager()
    }
}
```

#### iOS/Desktop/Web (Stub Implementation)

**Files:** `iosMain/kotlin/...input/IosVoiceCursor.kt`, etc.

```kotlin
// No-op implementation for platforms without VoiceCursor
actual fun getVoiceCursorManager(): VoiceCursorManager {
    return NoOpVoiceCursorManager()
}

class NoOpVoiceCursorManager : VoiceCursorManager {
    override val isAvailable: Boolean = false
    override val isActive: Boolean = false
    override val cursorPosition: Offset? = null

    override fun registerTarget(target: VoiceTarget) {}
    override fun unregisterTarget(id: String) {}
    override fun updateTargetBounds(id: String, bounds: Rect) {}
    override fun handleVoiceCommand(command: String, parameters: Map<String, Any>) = false
    override fun start() {}
    override fun stop() {}
    override fun addListener(listener: VoiceCursorListener) {}
    override fun removeListener(listener: VoiceCursorListener) {}
}
```

---

### 31.6 Best Practices

**Check Platform Capabilities:**
```kotlin
// ✅ Good - Adapt to platform capabilities
val capabilities = detectInputCapabilities()
if (capabilities.enableHoverStates) {
    // Enable hover effects
}

// ❌ Bad - Assume capabilities
// Always show hover effects (breaks on touch-only devices)
```

**Support Multiple Input Methods:**
```kotlin
// ✅ Good - Handle all input sources
fun handleInteraction(event: InputEvent) {
    when (event.source) {
        InputSource.Touch -> {
            // Touch-optimized behavior (larger hit targets)
        }
        InputSource.Mouse, InputSource.VoiceCursor -> {
            // Precise pointer behavior
        }
        InputSource.Keyboard -> {
            // Keyboard navigation
        }
        else -> {
            // Default behavior
        }
    }
}

// ❌ Bad - Only support one input method
fun handleInteraction(event: InputEvent.Tap) {
    // Only handles tap events, misses keyboard/voice
}
```

**Check VoiceCursor Availability:**
```kotlin
// ✅ Good - Graceful degradation
val voiceCursor = getVoiceCursorManager()
if (voiceCursor.isAvailable) {
    // Register voice targets
} else {
    // Fall back to standard input
}

// ❌ Bad - Assume VoiceCursor exists
// voiceCursor.registerTarget(...)  // Crashes on iOS/Desktop/Web
```

**Update Bounds on Layout Changes:**
```kotlin
// ✅ Good - Keep voice targets synchronized
Modifier.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInRoot()
    voiceCursor.updateTargetBounds(targetId, bounds)
}

// ❌ Bad - Stale bounds
// Voice targets don't align with visual elements
```

**Provide Visual Feedback for Voice Interaction:**
```kotlin
// ✅ Good - Show when VoiceCursor is hovering
if (isVoiceHovered) {
    // Highlight component
    // Show voice label
}

// ❌ Bad - No feedback
// Users don't know what they're about to select
```

---

### 31.7 Testing Input System

```kotlin
@Test
fun testInputStateTransitions() {
    var state = InputState.Default

    // Hover
    state = state.copy(isHovered = true)
    assertEquals(VisualState.Hovered, state.visualState)

    // Press (overrides hover)
    state = state.copy(isPressed = true)
    assertEquals(VisualState.Pressed, state.visualState)

    // Release
    state = state.copy(isPressed = false)
    assertEquals(VisualState.Hovered, state.visualState)

    // Disable (overrides everything)
    state = state.copy(isDisabled = true)
    assertEquals(VisualState.Disabled, state.visualState)
}

@Test
fun testVoiceCursorAvailability() {
    val voiceCursor = getVoiceCursorManager()

    // Should be available on Android with VoiceOS
    // Should be unavailable (NoOp) on other platforms
    if (Platform.isAndroid && VoiceOS.isInstalled()) {
        assertTrue(voiceCursor.isAvailable)
    } else {
        assertFalse(voiceCursor.isAvailable)
    }
}
```

---

**END OF SHARED UTILITIES DOCUMENTATION**

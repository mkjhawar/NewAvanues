# Shared Utilities Quick Reference

**For:** Desktop & Web Renderer Developers
**Purpose:** How to use shared utilities in your components

---

## Quick Start

### Desktop (Kotlin/Compose)

```kotlin
import com.augmentalis.avaelements.renderer.desktop.*
import com.augmentalis.avaelements.common.color.*
import com.augmentalis.avaelements.common.spacing.*
import com.augmentalis.avaelements.common.alignment.*
```

### Web (TypeScript/React)

```typescript
import {
  // Color
  UniversalColor, universalColorToCss, lightenColor, darkenColor,
  // Spacing
  EdgeInsets, edgeInsetsToPadding, EdgeInsetsUtils, SpacingScale,
  // Alignment
  mainAxisAlignmentToJustify, crossAxisAlignmentToAlign
} from './utils/sharedUtilitiesBridge';
```

---

## Color Conversion

### Desktop

```kotlin
// Parse hex color
val color = UniversalColor.fromHex("#3F51B5")
val composeColor = color.toComposeColor()

// Lighten/darken
val lighter = composeColor.lighten(0.2f)  // 20% lighter
val darker = composeColor.darken(0.3f)    // 30% darker

// Contrasting text color
val textColor = backgroundColor.contrastingForeground()

// Mix colors
val mixed = color1.mix(color2, ratio = 0.5f)  // 50/50 blend

// From ARGB int
val fromInt = 0xFF3F51B5.toInt().argbToComposeColor()
```

### Web

```typescript
// Parse hex color
const color = hexToUniversalColor('#3F51B5');
const css = universalColorToCss(color);  // "rgba(63, 81, 181, 1)"

// Lighten/darken
const lighter = lightenColor(color, 0.2);  // 20% lighter
const darker = darkenColor(color, 0.3);    // 30% darker

// Contrasting text color
const textColor = contrastingForeground(backgroundColor);

// From ARGB int
const fromInt = argbToCss(0xFF3F51B5);  // "rgba(63, 81, 181, 1)"
```

---

## Spacing & Padding

### Desktop

```kotlin
// Create padding
val padding = EdgeInsets.all(16f)
val paddingValues = padding.toPaddingValues()

// Symmetric
val symmetric = EdgeInsets.symmetric(horizontal = 16f, vertical = 8f)

// Custom
val custom = EdgeInsets(start = 16f, top = 8f, end = 16f, bottom = 12f)

// Use in Modifier
Modifier.padding(padding.toPaddingValues())

// Corner radius
val radius = CornerRadius.all(8f)
val shape = radius.toShape()

// Border
val border = Border.solid(width = 2f, color = 0xFF000000.toInt())
val borderStroke = border.toBorderStroke()

// Spacing scale
val spacing = SpacingScale.LG  // 16dp
```

### Web

```typescript
// Create padding
const padding = EdgeInsetsUtils.all(16);
const style = edgeInsetsToPadding(padding);  // RTL-aware

// Symmetric
const symmetric = EdgeInsetsUtils.symmetric(16, 8);

// Custom
const custom = EdgeInsetsUtils.only({ start: 16, top: 8, end: 16, bottom: 12 });

// Corner radius
const radius = CornerRadiusUtils.all(8);
const style = cornerRadiusToCss(radius);  // RTL-aware

// Border
const border: Border = { width: 2, color: 0xFF000000, style: 'solid' };
const style = borderToCss(border);

// Shadow
const shadow: Shadow = { color: 0x29000000, blurRadius: 4, spreadRadius: 0, offsetX: 0, offsetY: 2 };
const boxShadow = shadowToCss(shadow);

// Spacing scale
const spacing = SpacingScale.lg;  // 16px
```

---

## Alignment (RTL-Aware)

### Desktop

```kotlin
val layoutDirection = LocalLayoutDirection.current

// WrapAlignment → Horizontal Arrangement
val horizontal = WrapAlignment.Start.toComposeHorizontalArrangement(layoutDirection)
// LTR: Arrangement.Start, RTL: Arrangement.End

// MainAxisAlignment → Vertical Arrangement
val vertical = MainAxisAlignment.Center.toComposeVerticalArrangement()

// CrossAxisAlignment → Alignment
val align = CrossAxisAlignment.Stretch.toComposeVerticalAlignment()
```

### Web

```typescript
const layoutDirection: LayoutDirection = 'ltr';  // or 'rtl'

// MainAxisAlignment → justify-content
const justify = mainAxisAlignmentToJustify('start', layoutDirection);
// LTR: 'flex-start', RTL: 'flex-end'

// CrossAxisAlignment → align-items
const align = crossAxisAlignmentToAlign('center');  // 'center'

// WrapAlignment → justify-content
const wrapJustify = wrapAlignmentToJustify('spaceBetween', layoutDirection);
```

---

## Property Extraction (Desktop Only)

```kotlin
import com.augmentalis.avaelements.common.properties.*

val props: Map<String, Any?> = componentProps

// Basic types
val text = props.getString("label", default = "")
val enabled = props.getBoolean("enabled", default = true)
val count = props.getInt("count", default = 0)
val size = props.getFloat("size", default = 16f)

// Enums
enum class ButtonType { Primary, Secondary, Tertiary }
val type = props.getEnum("type", default = ButtonType.Primary)

// Colors
val color = props.getColorArgb("color", default = 0xFF3F51B5.toInt())

// Lists
val items = props.getStringList("items", default = emptyList())

// Dimension (with units)
val width = props.getDimension("width", default = 0f)
// Supports: "16dp", "24sp", "100px", "50%"

// Callbacks
val onClick = props.getCallback("onClick")
onClick?.invoke()

// Or use extension functions
val text = props.getString("label", "Default")
val enabled = props.getBoolean("enabled", true)
```

---

## Common Patterns

### Desktop: Themed Component

```kotlin
@Composable
fun ThemedButton(
    text: String,
    backgroundColor: UniversalColor,
    onClick: () -> Unit
) {
    val composeColor = backgroundColor.toComposeColor()
    val textColor = composeColor.contrastingForeground()
    val hoverColor = composeColor.lighten(0.1f)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = composeColor,
            contentColor = textColor
        )
    ) {
        Text(text)
    }
}
```

### Web: RTL-Aware Layout

```tsx
import { EdgeInsetsUtils, edgeInsetsToPadding, SpacingScale } from './utils/sharedUtilitiesBridge';

const FlexLayout: React.FC<{ direction: 'ltr' | 'rtl' }> = ({ direction, children }) => {
  const padding = EdgeInsetsUtils.symmetric(SpacingScale.lg, SpacingScale.md);
  const paddingStyle = edgeInsetsToPadding(padding, direction);

  return (
    <div style={{
      display: 'flex',
      direction,
      ...paddingStyle
    }}>
      {children}
    </div>
  );
};
```

### Desktop: Material Design Card

```kotlin
@Composable
fun MagicCard(
    elevation: Float = 2f,
    cornerRadius: Float = 8f,
    content: @Composable () -> Unit
) {
    val radius = CornerRadius.all(cornerRadius)
    val padding = EdgeInsets.all(SpacingScale.LG)

    Card(
        modifier = Modifier.padding(padding.toPaddingValues()),
        shape = radius.toShape(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        content()
    }
}
```

### Web: Theme Colors

```typescript
import { UniversalColor, universalColorToCss, lightenColor, darkenColor } from './utils/sharedUtilitiesBridge';

const theme = {
  primary: { alpha: 1, red: 0.25, green: 0.32, blue: 0.71 },  // #3F51B5

  // Generated variants
  get primaryLight() { return lightenColor(this.primary, 0.2); },
  get primaryDark() { return darkenColor(this.primary, 0.2); },

  // CSS values
  get primaryCss() { return universalColorToCss(this.primary); },
  get primaryLightCss() { return universalColorToCss(this.primaryLight); },
};

// Usage in component
<button style={{ backgroundColor: theme.primaryCss }}>
  Click me
</button>
```

---

## Migration Checklist

### Migrating Existing Desktop Component

- [ ] Import SharedUtilitiesBridge: `import com.augmentalis.avaelements.renderer.desktop.*`
- [ ] Replace manual alignment conversion with bridge functions
- [ ] Replace manual color conversion with `.toComposeColor()`
- [ ] Replace hardcoded padding with `EdgeInsets.toPaddingValues()`
- [ ] Use `PropertyExtractor` for component props
- [ ] Test RTL layout behavior
- [ ] Remove duplicate helper functions

### Migrating Existing Web Component

- [ ] Import sharedUtilitiesBridge utilities
- [ ] Replace inline color strings with `universalColorToCss()`
- [ ] Replace hardcoded padding with `edgeInsetsToPadding()`
- [ ] Use `SpacingScale` constants instead of magic numbers
- [ ] Add RTL support using bridge alignment functions
- [ ] Replace duplicate color manipulation with bridge functions
- [ ] Test in both LTR and RTL modes

---

## Performance Tips

### Desktop
- Bridge functions are lightweight conversions (no heavy computation)
- `toComposeColor()` creates new Color instance - consider caching if used repeatedly
- Alignment conversions are pure functions - safe to call in composition

### Web
- Color conversions involve string concatenation - cache CSS strings if needed
- HSL conversion has some computation - cache results for repeated use
- Flexbox alignment strings are static - safe to inline

---

## When NOT to Use Shared Utilities

### Use platform-native APIs when:
1. **Platform-specific features** (e.g., Compose-only modifiers, React-only hooks)
2. **Performance-critical paths** (though bridge overhead is minimal)
3. **Existing platform conventions** (e.g., Material3 color scheme on Android)
4. **Platform-specific theming** (use shared utilities as fallback)

### Use shared utilities when:
1. **Cross-platform consistency** is required
2. **RTL support** is needed
3. **Component models** come from cross-platform specs
4. **Color manipulation** is needed
5. **Reducing duplicate code**

---

## Troubleshooting

### Desktop: Import not found
```
Unresolved reference: toComposeColor
```
**Fix:** Add import: `import com.augmentalis.avaelements.renderer.desktop.*`

### Desktop: Wrong alignment in RTL
**Fix:** Make sure you're passing `LocalLayoutDirection.current` to RTL-aware functions:
```kotlin
component.alignment.toComposeHorizontalArrangement(LocalLayoutDirection.current)
```

### Web: Type errors
```
Property 'alpha' does not exist on type 'string'
```
**Fix:** Import type definitions:
```typescript
import { UniversalColor } from './utils/sharedUtilitiesBridge';
```

### Web: RTL not working
**Fix:** Make sure you're passing the correct `LayoutDirection`:
```typescript
const style = edgeInsetsToPadding(padding, 'rtl');  // Not 'ltr'
```

---

## Resources

- **Shared Utilities Source:** `Core/src/commonMain/kotlin/com/augmentalis/avaelements/common/`
- **Desktop Bridge:** `Renderers/Desktop/src/desktopMain/kotlin/com/augmentalis/avaelements/renderer/desktop/SharedUtilitiesBridge.kt`
- **Web Bridge:** `Renderers/Web/src/utils/sharedUtilitiesBridge.ts`
- **Full Summary:** `Renderers/SHARED-UTILITIES-INTEGRATION-SUMMARY.md`

---

## Quick Command Reference

### Desktop Compose

| Task | Code |
|------|------|
| Hex to Color | `UniversalColor.fromHex("#3F51B5").toComposeColor()` |
| Lighten Color | `color.lighten(0.2f)` |
| RTL Padding | `EdgeInsets.symmetric(16f, 8f).toPaddingValues()` |
| Corner Radius | `CornerRadius.all(8f).toShape()` |
| RTL Alignment | `WrapAlignment.Start.toComposeHorizontalArrangement(layoutDirection)` |

### Web React

| Task | Code |
|------|------|
| Hex to CSS | `universalColorToCss(hexToUniversalColor('#3F51B5'))` |
| Lighten Color | `lightenColor(color, 0.2)` |
| RTL Padding | `edgeInsetsToPadding(EdgeInsetsUtils.symmetric(16, 8), 'rtl')` |
| Corner Radius | `cornerRadiusToCss(CornerRadiusUtils.all(8), 'rtl')` |
| RTL Alignment | `mainAxisAlignmentToJustify('start', 'rtl')` |

---

**Last Updated:** 2025-11-26
**Version:** 3.0.0-shared-utilities

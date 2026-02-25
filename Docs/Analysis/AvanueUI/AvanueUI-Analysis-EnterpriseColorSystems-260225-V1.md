# AvanueUI Analysis: Enterprise Color Systems — IBM, Apple, Google, Meta

**Date**: 2026-02-25
**Module**: AvanueUI / DesignSystem
**Purpose**: Research color systems from IBM Carbon, Apple HIG, Google Material 3, and Meta to inform AvanueUI palette expansion. Our existing palettes (SOL, LUNA, TERRA, HYDRA) should coexist with industry-standard color systems.

---

## 1. IBM Carbon Design System

### Architecture
- **10-step scale**: Each color family has grades 10 (lightest) through 100 (darkest)
- **4 themes**: White, Gray 10, Gray 90, Gray 100
- **52 universal tokens** per theme (mapped to color roles)
- **Hover states**: Half-step between adjacent colors
- **Active states**: 2 full steps lighter (70-100 range) or darker (10-60 range)

### Color Families (10-step scales)

**Blue** (Primary action color across all IBM products):
| Grade | Hex |
|-------|-----|
| 10 | `#EDF5FF` |
| 20 | `#D0E2FF` |
| 30 | `#A6C8FF` |
| 40 | `#78A9FF` |
| 50 | `#4589FF` |
| 60 | `#0F62FE` |
| 70 | `#0043CE` |
| 80 | `#002D9C` |
| 90 | `#001D6C` |
| 100 | `#001141` |

**Other Families** (same 10-100 grade structure):
- **Cyan**: Teal-blue tones
- **Teal**: Blue-green tones
- **Green**: Nature/success tones
- **Magenta**: Warm pink-purple
- **Purple**: Cool violet
- **Red**: Alert/danger
- **Orange**: Warm accent
- **Yellow**: Warning/highlight
- **Gray**: Neutral (natural gray)
- **Cool Gray**: Blue-tinted neutral
- **Warm Gray**: Yellow-tinted neutral

### Key Semantic Tokens
| Token | White Theme | Gray 100 Theme |
|-------|------------|----------------|
| `$interactive-01` (primary action) | Blue 60 `#0F62FE` | Blue 60 `#0F62FE` |
| `$interactive-02` (secondary action) | Gray 80 `#393939` | Gray 60 `#6F6F6F` |
| `$ui-01` (primary container) | Gray 10 `#F4F4F4` | Gray 90 `#262626` |
| `$ui-02` (secondary container) | White `#FFFFFF` | Gray 80 `#393939` |
| `$ui-background` | White `#FFFFFF` | Gray 100 `#161616` |
| `$text-01` (primary text) | Gray 100 `#161616` | Gray 10 `#F4F4F4` |
| `$text-02` (secondary text) | Gray 70 `#525252` | Gray 30 `#C6C6C6` |
| `$support-01` (error) | Red 60 `#DA1E28` | Red 50 `#FA4D56` |
| `$support-02` (success) | Green 60 `#198038` | Green 40 `#42BE65` |
| `$support-03` (warning) | Yellow `#F1C21B` | Yellow `#F1C21B` |
| `$support-04` (info) | Blue 70 `#0043CE` | Blue 50 `#4589FF` |

### Design Philosophy
- Enterprise-first: Neutral gray dominance, blue-only primary actions
- 4 themes cover light/dark + two mid-tone options
- Accessibility-focused: WCAG AA minimum, AAA target
- Content-centric: Color supports information hierarchy, not decoration

---

## 2. Apple Human Interface Guidelines (HIG)

### Architecture
- **Semantic naming**: Colors named by function, not appearance
- **Dynamic colors**: Adapt to Light/Dark/Increased Contrast automatically
- **System colors**: 12 named colors + 6 gray variants + semantic backgrounds
- **Platform-specific**: Different values on iOS vs macOS vs tvOS

### System Colors (iOS)

| Color | Light Mode | Dark Mode |
|-------|-----------|-----------|
| systemBlue | `#007AFF` | `#0A84FF` |
| systemGreen | `#34C759` | `#30D158` |
| systemIndigo | `#5856D6` | `#5E5CE6` |
| systemOrange | `#FF9500` | `#FF9F0A` |
| systemPink | `#FF2D55` | `#FF375F` |
| systemPurple | `#AF52DE` | `#BF5AF2` |
| systemRed | `#FF3B30` | `#FF453A` |
| systemTeal | `#5AC8FA` | `#64D2FF` |
| systemYellow | `#FFCC00` | `#FFD60A` |
| systemBrown | `#A2845E` | `#AC8E68` |
| systemMint | `#00C7BE` | `#63E6E2` |
| systemCyan | `#32ADE6` | `#64D2FF` |

### Gray Scale

| Color | Light Mode | Dark Mode |
|-------|-----------|-----------|
| systemGray | `#8E8E93` | `#8E8E93` |
| systemGray2 | `#AEAEB2` | `#636366` |
| systemGray3 | `#C7C7CC` | `#48484A` |
| systemGray4 | `#D1D1D6` | `#3A3A3C` |
| systemGray5 | `#E5E5EA` | `#2C2C2E` |
| systemGray6 | `#F2F2F7` | `#1C1C1E` |

### Semantic Backgrounds

| Color | Light Mode | Dark Mode |
|-------|-----------|-----------|
| systemBackground | `#FFFFFF` | `#000000` |
| secondarySystemBackground | `#F2F2F7` | `#1C1C1E` |
| tertiarySystemBackground | `#FFFFFF` | `#2C2C2E` |
| systemGroupedBackground | `#F2F2F7` | `#000000` |
| secondaryGroupedBackground | `#FFFFFF` | `#1C1C1E` |
| tertiaryGroupedBackground | `#F2F2F7` | `#2C2C2E` |
| label | `#000000` | `#FFFFFF` |
| separator | `#3C3C43` (29% opacity) | `#545458` (60% opacity) |
| systemFill | `#787880` (20% opacity) | `#787880` (36% opacity) |

### Design Philosophy
- Platform-native feel: Colors follow OS conventions
- Semantic-first: Never hardcode, always use semantic tokens
- Vibrancy: Materials with blur + tint for depth
- Contrast: Automatic adaptation for accessibility settings
- Tint color: Single app accent color drives UI personality

---

## 3. Google Material Design 3 (M3)

### Architecture
- **5 key color roles**: Primary, Secondary, Tertiary, Neutral, Neutral Variant
- **Tonal palettes**: 13 tones per hue (0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100)
- **Dynamic Color**: System extracts colors from user wallpaper (Android 12+)
- **HCT color space**: Hue, Chroma, Tone — perceptually uniform
- **Design tokens**: `md.sys.color.{role}` naming convention

### Baseline Light Theme

| Token | Hex | Description |
|-------|-----|-------------|
| primary | `#6750A4` | Main brand color |
| onPrimary | `#FFFFFF` | Content on primary |
| primaryContainer | `#EADDFF` | Light primary fill |
| onPrimaryContainer | `#21005D` | Content on container |
| secondary | `#625B71` | Supporting color |
| onSecondary | `#FFFFFF` | Content on secondary |
| secondaryContainer | `#E8DEF8` | Light secondary fill |
| onSecondaryContainer | `#1D192B` | Content on container |
| tertiary | `#7D5260` | Accent/complement |
| onTertiary | `#FFFFFF` | Content on tertiary |
| tertiaryContainer | `#FFD8E4` | Light tertiary fill |
| onTertiaryContainer | `#31111D` | Content on container |
| error | `#B3261E` | Error state |
| onError | `#FFFFFF` | Content on error |
| errorContainer | `#F9DEDC` | Light error fill |
| onErrorContainer | `#410E0B` | Content on container |
| background | `#FFFBFE` | Page background |
| onBackground | `#1C1B1F` | Content on bg |
| surface | `#FFFBFE` | Card/sheet surface |
| onSurface | `#1C1B1F` | Content on surface |
| surfaceVariant | `#E7E0EC` | Alternate surface |
| onSurfaceVariant | `#49454F` | Content on variant |
| outline | `#79747E` | Borders |
| outlineVariant | `#CAC4D0` | Subtle borders |
| inverseSurface | `#313033` | Dark surface |
| inverseOnSurface | `#F4EFF4` | Content on inverse |
| inversePrimary | `#D0BCFF` | Primary on dark |
| scrim | `#000000` | Overlay shade |

### Baseline Dark Theme

| Token | Hex | Description |
|-------|-----|-------------|
| primary | `#D0BCFF` | Main brand (lighter for dark) |
| onPrimary | `#381E72` | Content on primary |
| primaryContainer | `#4F378B` | Dark primary fill |
| onPrimaryContainer | `#EADDFF` | Content on container |
| secondary | `#CCC2DC` | Supporting (lighter) |
| onSecondary | `#332D41` | Content on secondary |
| secondaryContainer | `#4A4458` | Dark secondary fill |
| onSecondaryContainer | `#E8DEF8` | Content on container |
| tertiary | `#EFB8C8` | Accent (lighter) |
| onTertiary | `#492532` | Content on tertiary |
| tertiaryContainer | `#633B48` | Dark tertiary fill |
| onTertiaryContainer | `#FFD8E4` | Content on container |
| error | `#F2B8B5` | Error (lighter) |
| onError | `#601410` | Content on error |
| errorContainer | `#8C1D18` | Dark error fill |
| onErrorContainer | `#F9DEDC` | Content on container |
| background | `#1C1B1F` | Page background |
| onBackground | `#E6E1E5` | Content on bg |
| surface | `#1C1B1F` | Card/sheet surface |
| onSurface | `#E6E1E5` | Content on surface |
| surfaceVariant | `#49454F` | Alternate surface |
| onSurfaceVariant | `#CAC4D0` | Content on variant |
| outline | `#938F99` | Borders |
| outlineVariant | `#49454F` | Subtle borders |
| inverseSurface | `#E6E1E5` | Light surface |
| inverseOnSurface | `#313033` | Content on inverse |
| inversePrimary | `#6750A4` | Primary on light |
| scrim | `#000000` | Overlay shade |

### Surface Elevation (Tonal)
M3 uses tonal elevation instead of shadow:
| Level | Light | Dark |
|-------|-------|------|
| Surface | `#FFFBFE` | `#1C1B1F` |
| Surface Container Lowest | `#FFFFFF` | `#0F0D13` |
| Surface Container Low | `#F7F2FA` | `#1D1B20` |
| Surface Container | `#F3EDF7` | `#211F26` |
| Surface Container High | `#ECE6F0` | `#2B2930` |
| Surface Container Highest | `#E6E0E9` | `#36343B` |

### M3 Expressive (2025 Update)
- New tonal constraints: 57-65 range (was 50-59 avoided in 2021 spec)
- Emphasis on more vibrant, expressive color schemes
- Extended color roles for richer UI palettes

### Design Philosophy
- User-personalized: Dynamic Color adapts to wallpaper
- Seed-based: Single seed color generates full scheme via HCT
- Tonal over shadow: Elevation expressed through color tone, not drop shadow
- Expressive: Encourages brand personality through color

---

## 4. Meta Design System

### Architecture
Meta's design system is less publicly documented than the others, but key elements:
- **Brand primary**: Facebook Blue `#1877F2`
- **Meta brand**: Blue `#0668E1`, Light Blue `#0080FB`
- **Functional colors**: Follow industry standards (red=error, green=success)
- **Internal system**: Uses StyleX (CSS-in-JS) for token management

### Known Brand Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Facebook Blue | `#1877F2` | Primary brand, links |
| Meta Blue | `#0668E1` | Meta corporate brand |
| Meta Light Blue | `#0080FB` | Secondary brand |
| Dark Text | `#1C2B33` | Primary text |
| Messenger Blue | `#0084FF` | Messenger accent |
| WhatsApp Green | `#25D366` | WhatsApp accent |
| Instagram Gradient Start | `#833AB4` | IG brand purple |
| Instagram Gradient Mid | `#FD1D1D` | IG brand red |
| Instagram Gradient End | `#F77737` | IG brand orange |

### Semantic Patterns (inferred from products)
| Role | Light | Dark |
|------|-------|------|
| Background | `#FFFFFF` | `#18191A` |
| Surface/Card | `#F0F2F5` | `#242526` |
| Primary Text | `#050505` | `#E4E6EB` |
| Secondary Text | `#65676B` | `#B0B3B8` |
| Divider | `#CED0D4` | `#3E4042` |
| Hover | `#F2F2F2` | `#3A3B3C` |
| Success | `#31A24C` | `#31A24C` |
| Error | `#E41E3F` | `#E41E3F` |
| Warning | `#F7B928` | `#F7B928` |
| Link/Action | `#1877F2` | `#2D88FF` |

### Design Philosophy
- Minimal color: Content-first, color is functional not decorative
- Blue identity: Single brand color drives entire UI personality
- Product differentiation: Each product (FB, IG, WA, Messenger) has unique accent
- Dark mode: Facebook dark uses `#18191A` (near-black, not pure black)

---

## 5. Comparison Matrix

### Primary Action Colors
| System | Primary (Light) | Primary (Dark) |
|--------|----------------|----------------|
| IBM Carbon | `#0F62FE` (Blue 60) | `#0F62FE` (Blue 60) |
| Apple | `#007AFF` (systemBlue) | `#0A84FF` (systemBlue) |
| Google M3 | `#6750A4` (Purple) | `#D0BCFF` (Light purple) |
| Meta | `#1877F2` (FB Blue) | `#2D88FF` (Lighter blue) |
| **Avanues HYDRA** | `#1565C0` (Royal Sapphire) | `#1565C0` |

### Background Colors
| System | Light BG | Dark BG |
|--------|---------|---------|
| IBM Carbon | `#FFFFFF` | `#161616` |
| Apple | `#FFFFFF` | `#000000` |
| Google M3 | `#FFFBFE` | `#1C1B1F` |
| Meta | `#FFFFFF` | `#18191A` |
| **Avanues HYDRA** | (per palette) | (per palette) |

### Error Colors
| System | Error (Light) | Error (Dark) |
|--------|--------------|--------------|
| IBM Carbon | `#DA1E28` | `#FA4D56` |
| Apple | `#FF3B30` | `#FF453A` |
| Google M3 | `#B3261E` | `#F2B8B5` |
| Meta | `#E41E3F` | `#E41E3F` |

### Token System Architecture
| System | Token Levels | Theme Count | Dynamic |
|--------|-------------|-------------|---------|
| IBM Carbon | 3 (color, role, component) | 4 | No |
| Apple | 2 (semantic, adaptable) | 2 + contrast | Yes (OS) |
| Google M3 | 3 (ref, sys, component) | 2 + custom | Yes (wallpaper) |
| Meta | 2 (brand, semantic) | 2 | No |
| **AvanueUI** | 3 (palette, glass/water, component) | 32 combinations | No (manual) |

---

## 6. Recommendations for AvanueUI

### A. Add Enterprise-Compatible Palette Presets

Map our existing theme system to industry palettes:

```
AvanueColorPalette.IBM_CARBON  → Blue 60 primary, Gray neutrals, 4-theme model
AvanueColorPalette.APPLE_HIG   → systemBlue primary, SF backgrounds, dynamic adaptation
AvanueColorPalette.GOOGLE_M3   → Purple-based primary, tonal surfaces, seed-driven
AvanueColorPalette.META        → FB Blue primary, minimal surfaces, content-first
```

These would be **additional palette values** alongside our existing SOL/LUNA/TERRA/HYDRA.

### B. Extend Color Token System

Our current `AvanueColorScheme` has basic roles. Consider adding:
- **Container colors** (M3 pattern): primaryContainer, secondaryContainer, tertiaryContainer
- **On-container colors**: Content colors for each container
- **Surface elevation tones** (M3 pattern): surfaceContainerLow through surfaceContainerHighest
- **Inverse roles**: inverseSurface, inverseOnSurface, inversePrimary

### C. Status/Support Colors (Standardize)

All four systems agree on semantic status colors:
| Role | Recommended |
|------|-------------|
| Error/Danger | Red family (`#DA1E28` to `#FF3B30` range) |
| Success | Green family (`#198038` to `#34C759` range) |
| Warning | Yellow/Amber (`#F1C21B` to `#FFCC00` range) |
| Info | Blue family (varies by primary) |

### D. Implementation Priority

1. **Phase 1**: Add IBM_CARBON and GOOGLE_M3 as new `AvanueColorPalette` values
2. **Phase 2**: Add APPLE_HIG and META palettes
3. **Phase 3**: Extend `AvanueColorScheme` with container + inverse roles
4. **Phase 4**: Dynamic color (wallpaper extraction) for M3 compatibility

### E. Accessibility Baseline

All systems target WCAG AA (4.5:1 text, 3:1 UI components). Our palette generation should:
- Validate contrast ratios at generation time
- Support increased contrast variants (Apple pattern)
- Provide high-contrast override tokens

---

## 7. AvanueUI Palette Expansion — Proposed Values

### AvanueColorPalette.IBM_CARBON
```
Primary:     #0F62FE (Blue 60)
OnPrimary:   #FFFFFF
Secondary:   #393939 (Gray 80)
Background:  #FFFFFF / #161616
Surface:     #F4F4F4 / #262626
Error:       #DA1E28 / #FA4D56
Success:     #198038 / #42BE65
Warning:     #F1C21B
Info:        #0043CE / #4589FF
```

### AvanueColorPalette.APPLE_HIG
```
Primary:     #007AFF / #0A84FF
OnPrimary:   #FFFFFF
Secondary:   #5856D6 / #5E5CE6 (Indigo)
Background:  #FFFFFF / #000000
Surface:     #F2F2F7 / #1C1C1E
Error:       #FF3B30 / #FF453A
Success:     #34C759 / #30D158
Warning:     #FFCC00 / #FFD60A
Info:        #5AC8FA / #64D2FF (Teal)
```

### AvanueColorPalette.GOOGLE_M3
```
Primary:     #6750A4 / #D0BCFF
OnPrimary:   #FFFFFF / #381E72
PrimaryContainer: #EADDFF / #4F378B
Secondary:   #625B71 / #CCC2DC
Tertiary:    #7D5260 / #EFB8C8
Background:  #FFFBFE / #1C1B1F
Surface:     #FFFBFE / #1C1B1F
Error:       #B3261E / #F2B8B5
```

### AvanueColorPalette.META
```
Primary:     #1877F2 / #2D88FF
OnPrimary:   #FFFFFF
Background:  #FFFFFF / #18191A
Surface:     #F0F2F5 / #242526
Text:        #050505 / #E4E6EB
SecondaryText: #65676B / #B0B3B8
Error:       #E41E3F
Success:     #31A24C
```

---

## Sources

- [IBM Carbon Design System - Color](https://carbondesignsystem.com/elements/color/usage/)
- [IBM Design Language - Color](https://www.ibm.com/design/language/color/)
- [Apple HIG - Color](https://developer.apple.com/design/human-interface-guidelines/color)
- [Apple iOS Colors Reference](https://noahgilmore.com/blog/dark-mode-uicolor-compatibility)
- [Google Material Design 3 - Color](https://m3.material.io/styles/color/overview)
- [M3 Color Roles](https://m3.material.io/styles/color/roles)
- [M3 Baseline Scheme](https://m3.material.io/styles/color/static/baseline)
- [Meta Brand Colors](https://www.designpieces.com/palette/meta-color-palette-hex-and-rgb/)
- [M3 Expressive 2025 Update](https://seenode.com/blog/what-is-material-3-and-why-it-matters-in-2025/)

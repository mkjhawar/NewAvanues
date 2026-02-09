# AvanueUI Liquid Glass Research — 2026-02-09

## Source: Apple WWDC 2025 — Liquid Glass Design Language

### Overview
Apple introduced "Liquid Glass" at WWDC 2025 as a unified translucent design language across iOS 26, iPadOS 26, macOS Tahoe, watchOS 26, tvOS 26, and visionOS 3. It replaces the flat/opaque material style with a dynamic, responsive glass aesthetic.

### Key Design Values

| Property | visionOS | iOS 26 Mobile | Notes |
|----------|----------|---------------|-------|
| Fill opacity | 20% | 20-40% | Lower = more transparent |
| Tint opacity | N/A | 40% | Background color bleed-through |
| Blur radius | 120pt | 2-40px | visionOS uses heavy blur for depth |
| Corner radius | 28-32pt | 20-28pt | Continuous corners (squircle) |
| Border | Gradient-based | Gradient-based | Top-light, bottom-subtle |
| Shadow | 0 6px 24px rgba(0,0,0,0.2) | Similar | Soft ambient shadow |
| Inner highlight | White 70%, blur 20px, spread -5px | Lighter | Specular simulation |

### Animation Characteristics
- **Spring-based**: Natural physics, no linear easing
- **Specular highlights**: Move with interaction (tilt, scroll, hover)
- **Lensing effect**: Content behind glass subtly distorts
- **Shimmer**: Light reflection travels across surface on state change
- **Duration**: 200-400ms for transitions, spring damping 0.7-0.85

### Color Strategy
- **True black background** (visionOS): `#000000` for OLED efficiency and glass contrast
- **Apple System Gray scale**: Gray 6 (`#1C1C1E`) → Gray 5 (`#2C2C2E`) → Gray 4 (`#3A3A3C`)
- **Label hierarchy**: Primary (`#F5F5F7`) → Secondary (`#A1A1A6`) → Tertiary (`#636366`) → Quaternary (`#48484A`)
- **System semantic colors**: Green `#30D158`, Yellow `#FFD60A`, Red `#FF453A`, Blue `#007AFF`

### Border Techniques
- Gradient stroke: top = white @ 30%, bottom = white @ 10%
- Width: 0.5-1.5pt depending on elevation
- Corner radius matches container (continuous/squircle)
- On hover/focus: border opacity increases to 40-50%

### Our Implementation (AvanueUI)
We adapted these values for the `LiquidColors` + `LiquidGlass` theme:

| AvanueUI Token | Apple Reference | Our Value |
|----------------|-----------------|-----------|
| background | True black | `#000000` |
| surface | System gray 6 | `#1C1C1E` |
| surfaceElevated | System gray 5 | `#2C2C2E` |
| primary | Custom cyan-electric | `#00D4FF` |
| glowColor | Custom cyan shimmer | `#00D4FF` |
| GlassTokens.lightOverlay | Fill 20% | 0.08 (tuned for mobile) |
| GlassTokens.mediumOverlay | Fill 20-40% | 0.12 |
| GlassTokens.heavyOverlay | N/A | 0.18 |
| GlassTokens.glowOpacity | Inner highlight | 0.20 |

### Sources
- Apple Newsroom: "Apple unveils Liquid Glass" (June 2025)
- WWDC25 Session 219: "Design with Liquid Glass"
- WWDC25 Session 356: "Build with Liquid Glass"
- Apple HIG: visionOS Materials and Glass
- dev.to analysis articles on Liquid Glass implementation
- TechCrunch, MacRumors coverage of iOS 26 design changes

### Future Considerations
- Lensing/distortion effects require RenderEffect (Android 12+)
- Spring animations via `Animatable` with `spring()` spec
- Specular highlights could use sensor-driven parallax on mobile
- Consider adapting blur radii based on DisplayProfile (heavier on tablet/glass)

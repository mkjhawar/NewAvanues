# AvanueUI Spatial Glass Design System -- Comprehensive Research

**Module:** AvanueUI | **Date:** 2026-02-09 | **Version:** V1
**Branch:** `060226-1-consolidation-framework`
**Author:** Research compilation for Avanues ecosystem
**Target:** Phones, Tablets, Foldables, Smart Glasses (AR/See-Through)

---

## Table of Contents

1. [Apple visionOS Liquid Glass (WWDC 2025)](#1-apple-visionos-liquid-glass-wwdc-2025)
2. [Pseudo-Spatial Design on Flat Displays](#2-pseudo-spatial-design-on-flat-displays)
3. [True Spatial Design for See-Through Displays](#3-true-spatial-design-for-see-through-displays)
4. [Light Mode, Dark Mode, and Accessibility Variants](#4-light-mode-dark-mode-and-accessibility-variants)
5. [Design System Architecture](#5-design-system-architecture)
6. [Gap Analysis: Current AvanueUI vs. Target](#6-gap-analysis-current-avanueui-vs-target)
7. [Implementation Roadmap](#7-implementation-roadmap)

---

## 1. Apple visionOS Liquid Glass (WWDC 2025)

### 1.1 What is Liquid Glass?

Announced June 9, 2025 at WWDC, Liquid Glass is Apple's unified translucent design language spanning iOS 26, iPadOS 26, macOS Tahoe 26, watchOS 26, tvOS 26, and visionOS 26. It is described as a "new digital meta-material" that dynamically bends and shapes light, behaving organically like lightweight liquid rather than simply recreating physical glass.

Key distinction from traditional glassmorphism: Liquid Glass uses **lensing** (bending and concentrating light in real-time) rather than **blur** (scattering light). This creates a fundamentally different visual quality -- content behind the glass is refracted and distorted, not just blurred.

**Sources:**
- [Apple Newsroom - Liquid Glass Announcement](https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/)
- [WWDC25 Session 219: Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/)
- [WWDC25 Session 323: Build a SwiftUI app with the new design](https://developer.apple.com/videos/play/wwdc2025/323/)

### 1.2 Technical Properties of Liquid Glass

#### Material Composition

| Property | Value | Notes |
|----------|-------|-------|
| **Fill opacity** | 20-40% | Lower = more see-through; visionOS uses ~20%, mobile uses 20-40% |
| **Tint opacity** | ~40% | Background color bleed-through on mobile |
| **Blur radius** | 2-40px mobile, up to 120pt visionOS | Performance budget: max 40px iPhone, 60px iPad/Mac |
| **Corner radius (card)** | 28pt | Continuous corners (squircle), not standard rounded |
| **Corner radius (pill)** | 999pt | Capsule shape for chips, pills |
| **Corner radius (sheet)** | 34pt | Bottom sheets, modal presentations |
| **Border type** | Gradient stroke | Top-light simulation, bottom-subtle |
| **Border width** | 0.5-1.5pt | Varies by elevation and importance |
| **Shadow radius** | 18pt | Soft ambient shadow |
| **Shadow y-offset** | 8pt | Subtle downward displacement |
| **Shadow opacity** | 0.18 (18%) | Relatively light to maintain airiness |
| **Inner highlight** | White @ 70%, blur 20px, spread -5px | Specular rim simulation |

#### Stroke and Highlight System

| Token | Value | Purpose |
|-------|-------|---------|
| `stroke.width` | 1pt | Standard glass border |
| `stroke.subtleOpacity` | 0.22 (22%) | Secondary glass elements |
| `stroke.strongOpacity` | 0.35 (35%) | Primary glass elements, focused state |
| `highlight.opacity` | 0.16 (16%) | Specular highlight intensity |
| `shadow.opacity` | 0.10-0.18 | Ambient shadow strength |
| `shadow.radius` | 16-18pt | Shadow blur spread |
| `shadow.y` | 8-10pt | Shadow vertical offset |

#### Three Levels of Glass

Apple defines three functional levels:

| Level | Name | Use Case | Opacity Range |
|-------|------|----------|--------------|
| **chrome** | Toolbar glass | Navigation bars, tab bars, floating controls | Lightest (15-20%) |
| **surface** | Card glass | Cards, panels, grouped content | Medium (20-30%) |
| **element** | Button glass | Small buttons, chips, interactive elements | Heaviest (25-40%) |

#### Glass Styles

| Style | Behavior | Use Case |
|-------|----------|----------|
| `.glass` | Translucent, see-through | Secondary actions, background elements |
| `.glassProminent` | More opaque, no background show-through | Primary actions, important controls |

**Sources:**
- [Build a Liquid Glass Design System in SwiftUI](https://levelup.gitconnected.com/build-a-liquid-glass-design-system-in-swiftui-ios-26-bfa62bcba5be)
- [LiquidGlassReference (comprehensive Swift/SwiftUI reference)](https://github.com/conorluddy/LiquidGlassReference)
- [iOS 26 Liquid Glass: Comprehensive Reference](https://medium.com/@madebyluddy/overview-37b3685227aa)

### 1.3 Specular Highlights and Lensing

#### How Lensing Works

The core rendering engine uses Metal shaders to produce the liquid glass effect:

1. **Background capture**: Content behind the view is captured as a texture
2. **Refraction**: Light bending through glass is simulated with a configurable refractive index
3. **Chromatic dispersion**: Prismatic color separation on edges for realistic glass appearance
4. **Fresnel reflections**: Edge lighting that intensifies at grazing viewing angles
5. **Glare highlights**: Directional specular streaks responding to surface normals

The specular highlight is implemented as a **rim light effect** -- the highlight appears around the edges of the glass object, and its intensity varies based on the angle of the surface normal relative to a fixed light direction (typically top-left, consistent with Apple HIG lighting model).

#### Pseudo-code for Specular Highlight

```kotlin
// Simplified specular highlight calculation
fun specularIntensity(
    surfaceNormal: Vector2,
    lightDirection: Vector2 = Vector2(-0.5f, -0.8f), // top-left light
    viewAngle: Float = 0f
): Float {
    val dot = surfaceNormal.dot(lightDirection)
    val fresnel = (1f - abs(dot)).pow(3f) // Fresnel falloff
    return fresnel * 0.7f // Max 70% white overlay
}
```

#### CSS/Web Equivalent

```css
/* Liquid Glass lensing + specular highlight */
.liquid-glass {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-radius: 28px;
    box-shadow:
        0 8px 32px rgba(0, 0, 0, 0.18),        /* ambient shadow */
        inset 0 1px 0 rgba(255, 255, 255, 0.35), /* top highlight */
        inset 0 -1px 0 rgba(255, 255, 255, 0.1); /* bottom subtle */
}
```

**Sources:**
- [Getting Clarity on Apple's Liquid Glass (CSS-Tricks)](https://css-tricks.com/getting-clarity-on-apples-liquid-glass/)
- [Liquid Glass in the Browser: Refraction with CSS and SVG](https://kube.io/blog/liquid-glass-css-svg/)
- [LiquidGlassKit backport for iOS 13-18](https://github.com/DnV1eX/LiquidGlassKit)

### 1.4 Spring Animations for Glass Transitions

Apple uses **spring-based animations** exclusively for Liquid Glass interactions. No linear easing is used.

#### Animation Parameters

| Animation Type | Duration | Spring Config | Use Case |
|---------------|----------|---------------|----------|
| Glass appear | 200-350ms | `.bouncy(duration: 0.35)` | Element materialization |
| Glass morph | 300-500ms | `.spring(bounce: 0.2)` | Shape morphing between states |
| Tab switch | 250ms | `.spring(response: 0.25, dampingFraction: 0.8)` | Tab bar transitions |
| Press feedback | 100-200ms | `.spring(response: 0.15, dampingFraction: 0.7)` | Button press/release |
| Specular shimmer | 400-600ms | `.spring(dampingFraction: 0.85)` | Highlight travel on interaction |
| Scroll morph | Continuous | `.spring(response: 0.3, dampingFraction: 0.75)` | Tab bar shrink on scroll |

#### Spring Parameter Reference

| Parameter | Typical Range | Description |
|-----------|---------------|-------------|
| `response` | 0.15 - 0.45 | Period of oscillation (lower = faster) |
| `dampingFraction` | 0.7 - 0.9 | Energy absorption (1.0 = critically damped, no bounce) |
| `bounce` | 0.1 - 0.3 | Alternative to damping (higher = more bounce) |
| `blendDuration` | 0 - 0.15 | Time to blend from current animation |

#### Compose Equivalent

```kotlin
// Spring animation spec for glass transitions
val glassSpring = spring<Float>(
    dampingRatio = 0.75f,  // Slightly underdamped for organic feel
    stiffness = 400f       // Medium stiffness
)

val morphSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 300f
)

val pressSpring = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = 800f       // High stiffness for snappy response
)
```

#### Animation Behaviors

- **Materialization**: Elements appear by gradually modulating light bending (fade-in + refraction increase)
- **Fluidity**: Gel-like flexibility with instant touch responsiveness
- **Morphing**: Glass shapes smoothly transform into each other using `GlassEffectContainer` / `glassEffectID`
- **Adaptivity**: Multi-layer composition adjusts to content, color scheme, and size

**Sources:**
- [How to build liquid glass action buttons in SwiftUI](https://blog.devgenius.io/how-to-build-liquid-glass-action-buttons-in-swift-ui-that-expands-like-magic-9101a3d24cda)
- [Transforming Glass Views with glassEffectID in SwiftUI](https://serialcoder.dev/text-tutorials/swiftui/transforming-glass-views-with-the-glasseffectid-modifier-in-swiftui/)

### 1.5 Text Readability on Translucent Surfaces

Apple uses a **vibrancy system** to ensure text remains readable on glass:

#### Apple's Label Hierarchy (Dark Mode)

| Level | Color | Hex | Opacity | Use Case |
|-------|-------|-----|---------|----------|
| Primary | White | `#F5F5F7` | ~96% | Titles, primary content |
| Secondary | Gray | `#A1A1A6` | ~63% | Subtitles, secondary info |
| Tertiary | Dark Gray | `#636366` | ~39% | Captions, hints |
| Quaternary | Darker Gray | `#48484A` | ~28% | Disabled text, placeholders |

#### Apple's Label Hierarchy (Light Mode)

| Level | Color | Hex | Opacity | Use Case |
|-------|-------|-----|---------|----------|
| Primary | Black | `#1C1C1E` | ~95% | Titles, primary content |
| Secondary | Gray | `#8E8E93` | ~56% | Subtitles, secondary info |
| Tertiary | Light Gray | `#C7C7CC` | ~40% | Captions, hints |
| Quaternary | Lighter Gray | `#D1D1D6` | ~30% | Disabled text, placeholders |

#### Vibrancy and Background Interaction

- System labels use **vibrancy** -- they sample the background and adjust brightness to maintain contrast
- On light backgrounds, labels darken; on dark backgrounds, labels brighten
- Minimum guaranteed contrast: Apple targets 4.5:1 for body text on glass surfaces
- Glass surfaces behind text use slightly higher opacity (30-40%) to create a readable "plate"

#### Strategies for Readable Glass Text

1. **Text backdrop plate**: Semi-opaque fill behind text regions (not the entire card)
2. **Increased glass opacity for text-heavy surfaces**: Surface-level glass uses 25-35% opacity
3. **Drop shadow on text**: Subtle text shadow (0, 1px, 2px, rgba(0,0,0,0.3)) for extra separation
4. **Vibrancy-aware colors**: Colors that auto-adjust based on sampled background

### 1.6 Apple's Color/Opacity System

#### System Colors (iOS 26 Dark Mode)

| Color | Hex | RGB | Usage |
|-------|-----|-----|-------|
| System Red | `#FF453A` | 255, 69, 58 | Error, destructive actions |
| System Orange | `#FF9F0A` | 255, 159, 10 | Warnings |
| System Yellow | `#FFD60A` | 255, 214, 10 | Caution, star ratings |
| System Green | `#30D158` | 48, 209, 88 | Success, active states |
| System Blue | `#0A84FF` | 10, 132, 255 | Links, primary actions |
| System Indigo | `#5E5CE6` | 94, 92, 230 | Accent, premium features |
| System Purple | `#BF5AF2` | 191, 90, 242 | Creative, media |
| System Pink | `#FF375F` | 255, 55, 95 | Favorites, love |
| System Teal | `#64D2FF` | 100, 210, 255 | Information, navigation |
| System Cyan | `#00D4FF` | 0, 212, 255 | Information, links |

#### System Gray Scale (iOS 26 Dark Mode)

| Level | Hex | Name | Usage |
|-------|-----|------|-------|
| Gray 1 | `#8E8E93` | System Gray | Disabled content |
| Gray 2 | `#636366` | System Gray 2 | Tertiary content |
| Gray 3 | `#48484A` | System Gray 3 | Quaternary content |
| Gray 4 | `#3A3A3C` | System Gray 4 | Surface variant |
| Gray 5 | `#2C2C2E` | System Gray 5 | Surface elevated |
| Gray 6 | `#1C1C1E` | System Gray 6 | Surface base |

#### Material Thickness Levels

Apple provides five material thicknesses for glass backgrounds:

| Material | Translucency | Blur | Use Case |
|----------|-------------|------|----------|
| `.ultraThin` | Most translucent | Light blur | Background overlays, minimal chrome |
| `.thin` | More translucent than opaque | Light-medium blur | Interactive highlights, selected items |
| `.regular` | Balanced | Medium blur | Section separators, sidebars |
| `.thick` | More opaque than translucent | Medium-heavy blur | Text fields, recessed areas |
| `.ultraThick` | Most opaque | Heavy blur | Modal backgrounds, safety-critical areas |

**Sources:**
- [Using Materials with SwiftUI](https://www.createwithswift.com/using-materials-with-swiftui/)
- [Apple Developer Documentation: Material](https://developer.apple.com/documentation/swiftui/material)

---

## 2. Pseudo-Spatial Design on Flat Displays

### 2.1 Creating Depth Illusion on 2D Screens

Pseudo-spatial design creates the perception of depth and dimensionality on flat displays without true 3D rendering. This is critical for bringing the glass aesthetic to phones, tablets, and foldables.

#### Core Depth Cues for UI

| Technique | Description | Implementation Complexity |
|-----------|-------------|--------------------------|
| **Tonal elevation** | Surface color shifts per elevation level | Low |
| **Shadow layering** | Multiple shadow layers at different radii | Low |
| **Blur depth-of-field** | Background blur increases with depth | Medium |
| **Parallax motion** | Layers move at different speeds on scroll/tilt | Medium |
| **Specular highlights** | Light reflections responding to interaction | Medium-High |
| **Lensing/refraction** | Content distortion through glass | High |
| **Ambient occlusion** | Darkening where surfaces meet | Low |

### 2.2 Elevation Systems

#### Material 3 Elevation (Reference)

Material Design 3 uses a dp-based scale that maps to both tonal color overlays AND shadow effects:

| Level | dp | Shadow | Tonal Overlay | Use Case |
|-------|-----|--------|---------------|----------|
| Level 0 | 0dp | None | None | Flat surfaces |
| Level 1 | 1dp | Very subtle | Primary @ 5% | Cards, list items |
| Level 2 | 3dp | Subtle | Primary @ 8% | FABs, navigation rail |
| Level 3 | 6dp | Moderate | Primary @ 11% | Top app bar (scrolled) |
| Level 4 | 8dp | Prominent | Primary @ 12% | Navigation drawer |
| Level 5 | 12dp | Strong | Primary @ 14% | Modal bottom sheet, dialog |

Key insight: M3 Expressive now adds spatial panels, orbiters, and spatial elevation tokens for adapting flat UI into dynamic spatial layouts.

#### AvanueUI Elevation System (Current)

Our `ElevationTokens.kt`:

| Token | Value | M3 Equivalent |
|-------|-------|---------------|
| `none` | 0dp | Level 0 |
| `xs` | 1dp | Level 1 |
| `sm` | 2dp | ~Level 1-2 |
| `md` | 4dp | ~Level 2-3 |
| `lg` | 8dp | Level 4 |
| `xl` | 12dp | Level 5 |
| `xxl` | 16dp | Beyond M3 scale |

**Gap identified**: Our elevation system lacks tonal overlay (primary color tinting) and multi-layer shadow definitions.

#### Recommended Multi-Layer Shadow System

```kotlin
// Each elevation level should define multiple shadow layers
data class ShadowDefinition(
    val ambient: Shadow,   // Broad, soft shadow (always present)
    val key: Shadow,       // Directional shadow from primary light source
    val penumbra: Shadow   // Soft edge shadow for realism
)

// Example for Level 3 (card)
val cardShadow = ShadowDefinition(
    ambient = Shadow(color = Black @ 8%, blur = 16.dp, offset = 0.dp),
    key = Shadow(color = Black @ 12%, blur = 8.dp, offset = DpOffset(0.dp, 4.dp)),
    penumbra = Shadow(color = Black @ 4%, blur = 24.dp, offset = DpOffset(0.dp, 2.dp))
)
```

**Sources:**
- [Material Design 3 Elevation](https://m3.material.io/styles/elevation/applying-elevation)
- [Elevation Design Patterns: Tokens, Shadows, and Roles](https://designsystems.surf/articles/depth-with-purpose-how-elevation-adds-realism-and-hierarchy)
- [Material 3 Expressive: What's New](https://supercharge.design/blog/material-3-expressive)

### 2.3 Parallax and Motion Depth

#### Parallax Scrolling Layers

Objects at different virtual depths move at different speeds:

| Layer | Speed Ratio | Z-Index | Content Type |
|-------|-------------|---------|--------------|
| Background | 0.3x | 0 | Wallpaper, ambient pattern |
| Deep | 0.5x | 1 | Background cards, large surfaces |
| Content | 1.0x | 2 | Primary content (scrolls normally) |
| Floating | 1.2x | 3 | FABs, status indicators |
| Overlay | 1.5x | 4 | Modals, alerts, command bars |

#### Sensor-Driven Parallax (Mobile)

```kotlin
// Use accelerometer for subtle glass parallax on phone/tablet
val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

// Map sensor values to glass highlight offset
fun calculateHighlightOffset(x: Float, y: Float): DpOffset {
    val maxOffset = 8.dp // Maximum highlight displacement
    return DpOffset(
        x = (x / 9.8f * maxOffset.value).coerceIn(-maxOffset.value, maxOffset.value).dp,
        y = (y / 9.8f * maxOffset.value).coerceIn(-maxOffset.value, maxOffset.value).dp
    )
}
```

### 2.4 Blur Depth-of-Field

Background blur radius should increase with depth separation:

| Separation | Blur Radius | Opacity Reduction | Example |
|-----------|------------|-------------------|---------|
| Adjacent (same level) | 0dp | 0% | Cards side by side |
| 1 level | 4dp | 5% | Card over surface |
| 2 levels | 8dp | 10% | Modal over card |
| 3+ levels | 16dp | 20% | Full overlay |

### 2.5 Platform Examples

#### iOS 26 / macOS Tahoe on Flat Screens

- Tab bars shrink and morph when scrolling (glass elements compress)
- Navigation bars use graduated opacity (more opaque during interaction)
- Dock layer uses multiple glass layers with parallax
- App icons have layered glass with device-motion parallax

#### Samsung One UI 8.5

- System-wide depth for icons (floating 3D appearance)
- Parallax and shadowing on icon layers
- Less gloss than iOS, subtler shadow layering
- Digital occlusion effects in AR integration

#### Google Material 3 Expressive

- Spatial panels and orbiters for extending flat UI into spatial layouts
- Tonal elevation with dynamic primary color overlay
- Enhanced motion system with expressive springs
- Component-level morph animations

**Sources:**
- [Liquid Glass vs Material 3 Expressive: Next-Gen UI Compared](https://orangeloops.com/2025/12/liquid-glass-vs-material-3-expressive-next%E2%80%91gen-ui-design-compared/)
- [The Parallax Effect: Enhancing Depth in Web Design](https://garagefarm.net/blog/parallax-effect-best-practices-and-examples)

---

## 3. True Spatial Design for See-Through Displays

### 3.1 AR Smart Glass UI Best Practices

See-through (optical or video-see-through) displays present fundamentally different challenges than opaque screens. The UI must coexist with the real world.

#### Key Principles

1. **Minimal chrome, maximum content**: Every pixel of UI occludes the real world
2. **Black is transparent**: On optical see-through displays, black pixels = invisible (no light emitted)
3. **High contrast is mandatory**: Variable real-world backgrounds demand robust contrast strategies
4. **Glance-ability**: Users cannot stare at the display; information must be absorbed in 1-2 second glances
5. **One action per page**: Complex navigation is impossible; use linear progression

### 3.2 Display Characteristics by Device

| Device | Resolution | FoV | Display Type | Brightness | Interaction |
|--------|-----------|-----|-------------|------------|-------------|
| Vuzix Blade | 480x480 | 19deg | Waveguide | ~1000 nits | Touchpad, voice |
| Vuzix Shield | 640x360 | ~30deg | Waveguide | ~1200 nits | Touchpad, voice |
| Vuzix M400 | 640x360 | ~28deg | Waveguide | ~1200 nits | Touch, buttons, voice |
| Vuzix M4000 | 854x480 | ~28deg | Waveguide | ~2000 nits | Touch, buttons, voice |
| RealWear HMT | 854x480 | 20deg | Micro LCD | ~1000 nits | Voice only |
| RealWear Nav520 | 1280x720 | ~24deg | LCoS | ~2000 nits | Voice only |
| XREAL Air | 1920x1080 | ~46deg | Micro OLED | ~400 nits | External controller |
| Vuzix Z100 | 1920x1080 | ~47deg | Waveguide | ~3000 nits | Touch, gesture, voice |

#### AvanueUI DisplayProfile Mapping (Current)

| DisplayProfile | Devices | Density Scale | Font Scale | Min Touch Target |
|---------------|---------|--------------|-----------|-----------------|
| GLASS_MICRO | Vuzix Blade | 0.625x | 0.75x | 36dp |
| GLASS_COMPACT | Vuzix M400, RealWear HMT | 0.75x | 0.85x | 40dp |
| GLASS_STANDARD | RealWear Nav520, Vuzix M4000 | 0.875x | 0.9x | 44dp |
| GLASS_HD | XREAL Air, Vuzix Z100 | 0.9x | 0.95x | 48dp |

### 3.3 Handling Transparency on See-Through Displays

#### The Black = Transparent Problem

On optical see-through displays (waveguide, birdbath), the display adds light to the scene. It cannot subtract light. This means:

- **Black (#000000) is invisible** -- it emits no light, so the real world shows through
- **White (#FFFFFF) is maximally opaque** -- it adds maximum light
- **Dark UI elements disappear in bright environments**
- **Light UI elements wash out in dark environments**

#### Contrast Strategies

| Environment | Challenge | Solution |
|------------|-----------|----------|
| **Bright outdoor** | UI invisible against sky | High-brightness white/yellow text, avoid dark backgrounds |
| **Indoor office** | Moderate; most readable | Standard contrast works |
| **Dark room** | UI glare; eye adaptation | Reduce brightness, use dimmer colors |
| **Variable** | Unpredictable contrast | Adaptive brightness + high contrast mode |

#### Recommended Color Strategy for See-Through

```
For see-through displays:
- PRIMARY TEXT: White (#FFFFFF) or bright yellow (#FFD60A)
  on NO background (text floats in space)
- SECONDARY TEXT: Light gray (#E0E0E0) on NO background
- BACKGROUNDS: Use sparingly, semi-transparent white (#FFFFFF @ 20-30%)
  NOT black backgrounds (they're invisible!)
- BORDERS: Bright, high-opacity (#FFFFFF @ 60-80%)
- ICONS: Filled (not outlined) for maximum pixel density
- STATUS COLORS: Bright green (#4ADE80), bright red (#FF6B6B), bright yellow (#FFD60A)
```

### 3.4 Typography for Smart Glasses

#### Google Glass Enterprise Guidelines

- Use Roboto or Roboto Condensed
- System uses Roboto Light, Regular, or Thin based on font size
- Black background rendered as transparent
- Prioritize brevity: first 11 characters should contain key information

#### Vuzix Guidelines

- **Absolute minimum text size**: 25px on 480x480 display
- **Recommended minimum for reading**: 40px on notification-style text
- **Avoid thin weight fonts** at small sizes
- Use Roboto and Roboto Condensed

#### Recommended Typography Scale for AvanueUI Glass Profiles

| Profile | Min Body Size | Min Label Size | Max Lines Visible | Font Weight |
|---------|--------------|---------------|-------------------|-------------|
| GLASS_MICRO | 14sp (effective 10.5sp) | 11sp (effective 8.25sp) | 3-4 | Medium minimum |
| GLASS_COMPACT | 14sp (effective 11.9sp) | 12sp (effective 10.2sp) | 5-6 | Regular acceptable |
| GLASS_STANDARD | 14sp (effective 12.6sp) | 12sp (effective 10.8sp) | 8-10 | Regular acceptable |
| GLASS_HD | 14sp (effective 13.3sp) | 12sp (effective 11.4sp) | 12-15 | All weights acceptable |

Note: "effective" values account for fontScale in DisplayProfile (e.g., GLASS_MICRO uses 0.75x fontScale).

### 3.5 Touch Targets and Interaction

| Display Type | Min Touch Target | Input Method | Notes |
|-------------|-----------------|--------------|-------|
| Phone | 48dp | Touch | M3 standard |
| Tablet | 48dp | Touch | M3 standard |
| GLASS_MICRO | 36dp (scaled) | Voice, touchpad | One action per page |
| GLASS_COMPACT | 40dp (scaled) | Voice, touchpad, buttons | Linear progression |
| GLASS_STANDARD | 44dp (scaled) | Voice, buttons | Simple tap-and-swipe |
| GLASS_HD | 48dp (scaled) | Controller, touch, gesture | Full interaction model |
| Spatial/visionOS | 60dp | Gaze + pinch | `minTouchTargetSpatial` |

#### Interaction Hierarchy for Glasses

1. **Voice** (primary on all glass profiles): "Say what you see"
2. **Swipe** (primary navigation): Left/right for pages, up/down for scroll
3. **Tap** (confirmation): Single tap = select, double tap = back
4. **Head movement** (secondary): Tilt to scroll on some devices

**Sources:**
- [Google Glass Enterprise Edition Design Guidelines](https://developers.google.com/glass-enterprise/guides/design-guidelines)
- [Vuzix Blade UX Design Guidelines](http://files.vuzix.com/Content/Upload/Vuzix%20Blade%20UX%20Design%20Guidelines_v2.pdf)
- [Vuzix UI Best Design Practices](https://support.vuzix.com/docs/ui-best-design-practices)
- [RealWear Navigator 500 Series Display Comparison](https://support.realwear.com/knowledge/realwear-navigator-500-series-display-comparison)

---

## 4. Light Mode, Dark Mode, and Accessibility Variants

### 4.1 Apple's Light vs. Dark Glass Approach

#### Light Mode Glass

| Property | Value | Notes |
|----------|-------|-------|
| Background fill | Black/dark @ 10-20% opacity | Glass tinted with dark overlay |
| Border | Black @ 15-25% | Darker borders for contrast |
| Text color | Near-black (#1C1C1E) | Dark text on light-ish glass |
| Shadow | rgba(0,0,0, 0.12) | Subtle shadow |
| Highlight | White @ 50-60% top edge | Less pronounced than dark mode |

#### Dark Mode Glass

| Property | Value | Notes |
|----------|-------|-------|
| Background fill | White @ 8-20% opacity | Glass tinted with light overlay |
| Border | White @ 12-35% | Lighter borders for edge definition |
| Text color | Near-white (#F5F5F7) | Light text on dark glass |
| Shadow | rgba(0,0,0, 0.25) | Deeper shadow for depth |
| Highlight | White @ 60-70% top edge | More pronounced specular |

#### Dynamic Tinting

Apple's system dynamically tints the glass material based on:
1. **Wallpaper color**: Samples dominant hue from wallpaper
2. **Content behind**: Blended from actual content pixels
3. **System appearance**: Light/dark mode base

### 4.2 Accessibility Variants

#### iOS 26 Accessibility Options

| Setting | Effect on Glass | Implementation |
|---------|----------------|----------------|
| **Reduce Transparency** | Glass becomes frostier/more opaque, obscuring more background | Increase fill opacity to 60-80% |
| **Increase Contrast** | Key elements get high-contrast borders in black or white; text weight increases | Add solid borders, increase text weight |
| **Reduce Motion** | No spring animations, no shimmer, no parallax | Use cross-fade instead of springs |
| **Bold Text** | All text uses semibold/bold weight minimum | Increase font weight globally |
| **Larger Text** | Dynamic Type scales up | Respect system font scale |

#### Clear vs. Tinted Mode (iOS 26.1+)

| Mode | Behavior | Opacity | Best For |
|------|----------|---------|----------|
| **Clear** | More transparent, reveals background | Lower (20-30%) | Aesthetic preference, dark environments |
| **Tinted** | More opaque, adds color contrast | Higher (40-60%) | Better readability, bright environments |

#### High Contrast Glass Implementation

```kotlin
// Accessibility-aware glass parameters
fun glassParameters(
    accessibilityState: AccessibilityState
): GlassConfig {
    val baseOpacity = when {
        accessibilityState.reduceTransparency -> 0.75f  // Near-opaque
        accessibilityState.increaseContrast -> 0.45f    // More opaque
        else -> 0.15f                                     // Standard glass
    }

    val borderWidth = when {
        accessibilityState.increaseContrast -> 2.dp     // Thick solid border
        else -> 1.dp                                     // Standard gradient border
    }

    val borderOpacity = when {
        accessibilityState.increaseContrast -> 0.8f     // Near-solid border
        accessibilityState.reduceTransparency -> 0.5f   // More visible
        else -> 0.22f                                    // Standard subtle
    }

    val animationType = when {
        accessibilityState.reduceMotion -> AnimationType.CROSS_FADE
        else -> AnimationType.SPRING
    }

    return GlassConfig(baseOpacity, borderWidth, borderOpacity, animationType)
}
```

### 4.3 WCAG 2.1 AA Compliance with Translucent Backgrounds

#### Minimum Contrast Requirements

| Content Type | WCAG AA | WCAG AAA | Notes |
|-------------|---------|----------|-------|
| Normal text (<18pt) | 4.5:1 | 7:1 | Most body text |
| Large text (>=18pt or >=14pt bold) | 3:1 | 4.5:1 | Headlines, titles |
| UI components | 3:1 | 3:1 | Borders, icons, controls |
| Decorative | N/A | N/A | Non-functional elements |

#### The Translucent Background Challenge

WCAG does not explicitly address translucent backgrounds because the effective contrast depends on what is behind the glass. **Best practice**:

1. **Test worst case**: Measure contrast against the lightest possible background (for dark text) or darkest (for light text)
2. **Add safety margin**: Target 5:1 instead of 4.5:1 to account for variable backgrounds
3. **Use text backdrop plates**: Semi-opaque fill immediately behind text
4. **Clamp minimum glass opacity**: Never allow glass to be more transparent than the point where contrast fails

#### Minimum Glass Opacity for WCAG AA Compliance

For white text (#F5F5F7) on glass over various backgrounds:

| Background Luminance | Min Glass Opacity (dark tint) | Resulting Contrast |
|---------------------|------------------------------|-------------------|
| Light wallpaper (L=0.8) | 55% dark overlay | ~4.5:1 |
| Medium wallpaper (L=0.5) | 30% dark overlay | ~4.5:1 |
| Dark wallpaper (L=0.2) | 10% dark overlay | ~6:1 |
| True black (L=0) | 0% (no overlay needed) | ~17:1 |

For dark text (#1C1C1E) on glass over various backgrounds (light mode):

| Background Luminance | Min Glass Opacity (light tint) | Resulting Contrast |
|---------------------|-------------------------------|-------------------|
| Dark wallpaper (L=0.2) | 50% light overlay | ~4.5:1 |
| Medium wallpaper (L=0.5) | 25% light overlay | ~4.5:1 |
| Light wallpaper (L=0.8) | 5% light overlay | ~8:1 |

### 4.4 Dynamic Type / Large Text in Glass Cards

#### Scaling Strategy

Glass cards must accommodate text size changes without breaking layout:

| Dynamic Type Size | Scale Factor | Card Behavior |
|-------------------|-------------|---------------|
| xSmall | 0.8x | Standard layout |
| Small | 0.85x | Standard layout |
| Medium (default) | 1.0x | Standard layout |
| Large | 1.1x | Standard layout |
| xLarge | 1.2x | Expand vertically, may reduce padding |
| xxLarge | 1.3x | Expand vertically, reduce padding to `sm` (8dp) |
| xxxLarge | 1.5x | Expand vertically, reduce padding to `xs` (4dp) |
| AX1-AX5 | 1.6-3.0x | Stack horizontal layouts vertically, full-width cards |

#### Implementation Pattern

```kotlin
@Composable
fun adaptiveGlassCardPadding(): PaddingValues {
    val fontScale = LocalDensity.current.fontScale
    return when {
        fontScale > 1.5f -> PaddingValues(SpacingTokens.xs)    // 4dp
        fontScale > 1.3f -> PaddingValues(SpacingTokens.sm)    // 8dp
        fontScale > 1.1f -> PaddingValues(SpacingTokens.sm)    // 8dp, reduce margin
        else -> PaddingValues(SpacingTokens.md)                 // 16dp standard
    }
}
```

### 4.5 Color Blind Safe Palettes with Glass Effects

#### WCAG 1.4.1: Use of Color

Color must NEVER be the sole indicator of meaning. All semantic states require **at least two** visual cues:

| Semantic State | Color | Required Secondary Cue | Icon |
|---------------|-------|----------------------|------|
| Success | Green (#30D158 / #10B981) | Checkmark icon + "Success" text | check-circle |
| Error | Red (#FF453A / #EF4444) | X icon + "Error" text | x-circle |
| Warning | Yellow (#FFD60A / #F59E0B) | Exclamation icon + "Warning" text | alert-triangle |
| Info | Blue (#0A84FF / #0EA5E9) | Info icon + "Info" text | info-circle |
| Disabled | Gray (#48484A / #64748B) | Reduced opacity + strikethrough or dimmed icon | minus-circle |

#### Color Blind Safe Palette Recommendations

For users with protanopia (red-blind) and deuteranopia (green-blind), which together account for ~8% of males:

| Standard Color | CB-Safe Alternative | Hex | Works For |
|---------------|--------------------|----|-----------|
| Red (error) | Orange-red with distinct luminance | `#FF6B35` | All types |
| Green (success) | Blue-tinted green | `#00B4D8` or shape-only | Protanopia, deuteranopia |
| Yellow (warning) | Keep as-is | `#FFD60A` | All types (high luminance) |
| Blue (info) | Keep as-is | `#0A84FF` | All types (blue perception is universal) |

#### Key Principle

Blue is the safest primary color because most forms of color blindness have minimal effect on blue perception. This aligns with AvanueUI's Ocean theme (`primary = #3B82F6`) and Liquid theme (`primary = #00D4FF`).

**Sources:**
- [Apple's Liquid Glass: Accessibility Analysis (Infinum)](https://infinum.com/blog/apples-ios-26-liquid-glass-sleek-shiny-and-questionably-accessible/)
- [Liquid Glass: Practical Guidance for Designers (Designed for Humans)](https://designedforhumans.tech/blog/liquid-glass-smart-or-bad-for-accessibility)
- [Liquid Glass Is Cracked (NN/g Nielsen Norman Group)](https://www.nngroup.com/articles/liquid-glass/)
- [iOS 26.2 Fixed Liquid Glass (BGR)](https://www.bgr.com/2070522/ios-26-2-fixed-liquid-glass-new-settings-options/)
- [WCAG 2.1 Understanding Contrast](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [WCAG 1.4.1 Use of Color](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html)

---

## 5. Design System Architecture

### 5.1 Multi-Variant Theme System Architecture

A robust design system supporting phone + tablet + foldable + smart glasses needs **four orthogonal dimensions**:

| Dimension | Variants | Controls |
|-----------|----------|----------|
| **Theme variant** | Ocean, Sunset, Liquid | Brand colors, glass tints |
| **Appearance mode** | Light, Dark | Background luminance, text polarity |
| **Accessibility mode** | Standard, High Contrast, Reduced Transparency, Reduced Motion | Glass opacity, borders, animations |
| **Display profile** | Phone, Tablet, Glass_Micro, Glass_Compact, Glass_Standard, Glass_HD | Density, font scale, layout strategy |

Total combinations: 3 themes x 2 appearances x 4 accessibility modes x 6 display profiles = **144 unique configurations**.

Token-based architecture handles this combinatorial explosion by **layering** decisions:

```
┌─────────────────────────────────────────────────────┐
│ Layer 1: Core Tokens (static, universal)             │
│ SpacingTokens, ShapeTokens, TypographyTokens, etc.  │
├─────────────────────────────────────────────────────┤
│ Layer 2: Semantic Tokens (theme-variable)             │
│ AvanueColorScheme, AvanueGlassScheme                 │
├─────────────────────────────────────────────────────┤
│ Layer 3: Appearance Tokens (light/dark)               │
│ OceanColors.Light, OceanColors.Dark (NEW)             │
├─────────────────────────────────────────────────────┤
│ Layer 4: Accessibility Tokens (overrides)             │
│ GlassAccessibility: opacity, border, motion (NEW)     │
├─────────────────────────────────────────────────────┤
│ Layer 5: Display Profile Tokens (responsive)          │
│ DisplayProfile.densityScale, fontScale, minTouchTarget│
├─────────────────────────────────────────────────────┤
│ Layer 6: Component Tokens (per-component overrides)   │
│ GlassCard.borderWidth, OceanButton.height, etc.       │
└─────────────────────────────────────────────────────┘
```

### 5.2 Token Architecture

#### W3C Design Tokens Community Group (DTCG) Format

The W3C DTCG specification reached its first stable version (2025.10) on October 28, 2025. AvanueUI tokens should be exportable to this format for Figma/design tool integration.

**DTCG JSON format:**

```json
{
  "avanue": {
    "color": {
      "primary": {
        "$value": "#3B82F6",
        "$type": "color",
        "$description": "Ocean theme primary"
      },
      "surface": {
        "$value": "#1E293B",
        "$type": "color"
      }
    },
    "glass": {
      "blur": {
        "light": { "$value": "6px", "$type": "dimension" },
        "medium": { "$value": "8px", "$type": "dimension" },
        "heavy": { "$value": "10px", "$type": "dimension" }
      },
      "opacity": {
        "light": { "$value": 0.08, "$type": "number" },
        "medium": { "$value": 0.12, "$type": "number" },
        "heavy": { "$value": 0.18, "$type": "number" }
      }
    },
    "spacing": {
      "sm": { "$value": "8px", "$type": "dimension" },
      "md": { "$value": "16px", "$type": "dimension" },
      "lg": { "$value": "24px", "$type": "dimension" }
    }
  }
}
```

**Sources:**
- [W3C Design Tokens Specification (2025.10)](https://www.w3.org/community/design-tokens/2025/10/28/design-tokens-specification-reaches-first-stable-version/)
- [Design System Mastery with Figma Variables: 2025/2026 Playbook](https://www.designsystemscollective.com/design-system-mastery-with-figma-variables-the-2025-2026-best-practice-playbook-da0500ca0e66)

### 5.3 Component-Level Adaptation

The same component should render differently based on display profile:

#### GlassCard Adaptation Example

| Property | Phone | Tablet | GLASS_MICRO | GLASS_HD |
|----------|-------|--------|-------------|----------|
| Corner radius | 12dp | 16dp | 8dp | 12dp |
| Border width | 1dp | 1dp | 1.5dp (more visible) | 1dp |
| Glass opacity | 0.15 | 0.15 | 0.25 (more opaque) | 0.18 |
| Padding | 16dp | 24dp | 8dp | 16dp |
| Shadow | Yes | Yes | No (transparent BG) | Optional |
| Max width | 360dp | 600dp | Full width | 400dp |
| Blur radius | 8dp | 10dp | 0dp (no blur on glass) | 6dp |
| Text size | Standard | Standard | 0.75x scaled | 0.95x scaled |

#### Architecture Pattern: Adaptive Component

```kotlin
@Composable
fun AdaptiveGlassCard(
    modifier: Modifier = Modifier,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable ColumnScope.() -> Unit
) {
    val profile = AvanueTheme.displayProfile
    val isGlass = profile.isGlass

    val adaptedGlassLevel = when {
        isGlass && profile == DisplayProfile.GLASS_MICRO -> GlassLevel.HEAVY // More opaque
        isGlass -> GlassLevel.MEDIUM
        else -> glassLevel
    }

    val adaptedBorder = when {
        isGlass -> GlassDefaults.borderStrong  // More visible on see-through
        else -> GlassDefaults.border
    }

    val adaptedShape = when {
        profile == DisplayProfile.GLASS_MICRO -> GlassShapes.small  // Smaller radius
        profile == DisplayProfile.TABLET -> GlassShapes.large       // Larger radius
        else -> GlassShapes.default
    }

    GlassCard(
        modifier = modifier,
        shape = adaptedShape,
        border = adaptedBorder,
        glassLevel = adaptedGlassLevel,
        content = content
    )
}
```

### 5.4 Figma Integration Pattern

#### Token Structure for Figma Variables

```
Collections:
├── Core (no modes)
│   ├── spacing/sm = 8
│   ├── spacing/md = 16
│   ├── shape/md = 12
│   └── ...
├── Theme (modes: Ocean, Sunset, Liquid)
│   ├── color/primary = {varies by mode}
│   ├── color/surface = {varies by mode}
│   ├── glass/glowColor = {varies by mode}
│   └── ...
├── Appearance (modes: Light, Dark)
│   ├── color/background = {varies by mode}
│   ├── color/textPrimary = {varies by mode}
│   ├── glass/overlayColor = {varies by mode}
│   └── ...
├── Accessibility (modes: Standard, High Contrast, Reduced Transparency)
│   ├── glass/fillOpacity = {varies by mode}
│   ├── glass/borderWidth = {varies by mode}
│   ├── animation/type = {varies by mode}
│   └── ...
└── Display (modes: Phone, Tablet, Glass_Micro, Glass_Compact, Glass_Standard, Glass_HD)
    ├── layout/columns = {varies by mode}
    ├── layout/margin = {varies by mode}
    ├── touch/minTarget = {varies by mode}
    └── ...
```

#### Figma Component Properties

Each glass component should expose:
- `glassLevel`: Enum (light/medium/heavy)
- `theme`: Instance swap (Ocean/Sunset/Liquid variants)
- `appearance`: Boolean property (light/dark)
- `accessible`: Boolean property (standard/high contrast)
- `displayProfile`: Enum (phone/tablet/glass variants)

**Sources:**
- [Schema 2025: Design Systems For A New Era (Figma Blog)](https://www.figma.com/blog/schema-2025-design-systems-recap/)
- [The Evolution of Design System Tokens (Design Systems Collective)](https://www.designsystemscollective.com/the-evolution-of-design-system-tokens-a-2025-deep-dive-into-next-generation-figma-structures-969be68adfbe)
- [Preparing for the Design Tokens Era: Multi-Brand Systems](https://medium.com/@dimiganin/preparing-for-the-design-tokens-era-multi-brand-systems-and-figmas-extended-collections-9fd35ccd06df)

---

## 6. Gap Analysis: Current AvanueUI vs. Target

### 6.1 What We Have (Current State)

| Area | Status | Files |
|------|--------|-------|
| **3 theme variants** | OCEAN, SUNSET, LIQUID | AvanueThemeVariant.kt |
| **Color scheme interface** | 30+ color roles | AvanueColorScheme.kt |
| **Glass scheme interface** | 5 glass properties | AvanueGlassScheme.kt |
| **Glass tokens** | Static opacity/blur/border values | GlassTokens.kt |
| **Glass components** | 8 components (Surface, Card, Bubble, Button, Chip, FAB, IconButton, Indicator) | GlassmorphicComponents.kt |
| **Glass modifier** | `Modifier.glass()` with level-based opacity | GlassExtensions.kt |
| **Display profiles** | 6 profiles (Phone, Tablet, 4x Glass) | DisplayProfile.kt |
| **Elevation tokens** | 7 levels (none to xxl) | ElevationTokens.kt |
| **Animation tokens** | 5 durations (100-1000ms) | AnimationTokens.kt |
| **Spacing tokens** | 10 values (0-64dp + touch targets) | SpacingTokens.kt |
| **Shape tokens** | 8 corner radii (0-9999dp) | ShapeTokens.kt |
| **Size tokens** | 30+ component sizes + spatial sizes | SizeTokens.kt |
| **Typography tokens** | Full M3 type scale (15 styles) | TypographyTokens.kt |
| **Responsive tokens** | M3 breakpoints + glass breakpoints + grid | ResponsiveTokens.kt |
| **M3 integration** | AvanueColorScheme -> M3 ColorScheme mapping | AvanueTheme.kt |
| **Density scaling** | Per-profile density/font override via LocalDensity | AvanueTheme.kt |

### 6.2 What We Need (Gaps)

| Gap | Priority | Effort | Description |
|-----|----------|--------|-------------|
| **Light mode support** | HIGH | Medium | Only dark mode exists; no light color schemes |
| **Accessibility glass overrides** | HIGH | Medium | No reduce-transparency, increase-contrast, reduce-motion handling |
| **Multi-layer shadow system** | MEDIUM | Low | ElevationTokens lack shadow definitions (ambient + key + penumbra) |
| **Tonal elevation** | MEDIUM | Low | No primary color tinting per elevation level |
| **Spring animation specs** | MEDIUM | Low | AnimationTokens only has durations, no spring parameters |
| **Lensing/refraction effect** | LOW | High | Requires RenderEffect (Android 12+), complex shader work |
| **Sensor-driven parallax** | LOW | Medium | Accelerometer-based specular highlight offset |
| **Continuous corners (squircle)** | LOW | Medium | Standard rounded corners, not Apple-style continuous corners |
| **Dynamic glass tinting** | LOW | Medium | Glass tint based on background content sampling |
| **See-through display optimizations** | HIGH | Medium | Black-is-transparent awareness, high-contrast glass mode for glasses |
| **WCAG contrast validation** | HIGH | Low | No runtime contrast ratio checking for glass surfaces |
| **Text backdrop plates** | MEDIUM | Low | Semi-opaque fill behind text regions in glass cards |
| **W3C DTCG token export** | LOW | Medium | Token export to JSON for Figma integration |
| **Color blind safe semantic icons** | MEDIUM | Low | Semantic states need icon + text, not just color |

### 6.3 Specific Numeric Gaps

#### Glass Tokens Comparison

| Token | Current Value | Apple Reference | Recommended Change |
|-------|--------------|-----------------|-------------------|
| `lightOverlay` | 0.08 | 15-20% | Consider 0.12-0.15 for better visibility |
| `mediumOverlay` | 0.12 | 20-30% | Consider 0.18-0.22 |
| `heavyOverlay` | 0.18 | 25-40% | Consider 0.25-0.30 |
| `lightBlur` | 6dp | 8-15px | Aligned (mobile appropriate) |
| `mediumBlur` | 8dp | 15-20px | Consider 10-12dp |
| `heavyBlur` | 10dp | 20-40px | Consider 16-20dp |
| `borderTopOpacity` | 0.30 | 0.30-0.35 | Aligned |
| `borderBottomOpacity` | 0.15 | 0.10-0.15 | Aligned |
| `shadowOpacity` | 0.25 | 0.18 | Consider reducing to 0.18-0.20 |
| `shadowElevation` | 8dp | 18pt radius | Consider adding shadow radius (separate from elevation dp) |
| Corner radius (card) | 12dp (ShapeTokens.md) | 28pt | Significant gap -- consider 20-28dp for Liquid theme |

#### Animation Tokens Comparison

| Token | Current Value | Apple Reference | Recommended |
|-------|--------------|-----------------|-------------|
| `fast` | 100ms | Press feedback 100-200ms | Aligned |
| `normal` | 200ms | Glass appear 200-350ms | Aligned |
| `medium` | 300ms | Morph 300-500ms | Aligned |
| `slow` | 500ms | Shimmer 400-600ms | Aligned |
| Spring damping (MISSING) | N/A | 0.7-0.85 | ADD: dampingRatio tokens |
| Spring stiffness (MISSING) | N/A | 300-800 | ADD: stiffness tokens |
| Spring bounce (MISSING) | N/A | 0.1-0.3 | ADD: bounce tokens |

---

## 7. Implementation Roadmap

### Phase 1: Critical Gaps (Estimated: 2-3 sessions)

1. **Light mode color schemes**: Create `OceanColorsLight`, `SunsetColorsLight`, `LiquidColorsLight` implementing `AvanueColorScheme` with light-mode values. Add `isDark: Boolean` parameter to `AvanueThemeProvider`.

2. **Accessibility glass overrides**: Create `GlassAccessibilityConfig` data class with `reduceTransparency`, `increaseContrast`, `reduceMotion` flags. Wire into `Modifier.glass()` and all glass components.

3. **See-through display glass mode**: For `DisplayProfile.isGlass`, automatically:
   - Use white text on transparent backgrounds (no dark backgrounds)
   - Increase border opacity to 0.6-0.8
   - Disable blur effects (no backdrop blur on see-through displays)
   - Use filled icons instead of outlined

4. **WCAG contrast validation utility**: Create `ContrastUtils.kt` with `calculateContrastRatio(foreground, background)` and `meetsWcagAA(foreground, background, isLargeText)` functions.

### Phase 2: Enhanced Glass Effects (Estimated: 2-3 sessions)

5. **Spring animation tokens**: Add `SpringTokens` object with `pressResponse`, `pressStiffness`, `morphDamping`, `glassAppearDamping`, etc.

6. **Multi-layer shadow system**: Enhance `ElevationTokens` with ambient/key/penumbra shadow definitions per level.

7. **Tonal elevation**: Add primary color overlay per elevation level in `AvanueColorScheme`.

8. **Text backdrop plates**: Add `Modifier.textBackdrop()` that adds a semi-opaque fill behind text in glass contexts.

9. **Enhanced corner radii for Liquid theme**: Liquid theme should use larger corner radii (28dp card, 999dp pill, 34dp sheet) matching Apple's values.

### Phase 3: Advanced Features (Estimated: 3-4 sessions)

10. **Sensor-driven specular highlights**: Accelerometer-based parallax for glass highlight offset on mobile.

11. **Continuous corners (squircle)**: Implement smooth continuous corner radius instead of standard circular arcs.

12. **Dynamic glass tinting**: Sample background content to tint glass material dynamically.

13. **W3C DTCG token export**: Generate `.tokens.json` files from Kotlin token objects for Figma integration.

14. **Lensing/refraction effects**: Use `RenderEffect` (Android 12+) for content distortion through glass surfaces.

### Phase 4: Polish and Optimization (Estimated: 1-2 sessions)

15. **Performance budgets**: Max compositing layers per screen <= 4, blur radius <= 40px on phone.

16. **Glass component unit tests**: Verify contrast ratios, touch target sizes, and accessibility compliance.

17. **Figma component library**: Create matching Figma components with all variant properties.

---

## Appendix A: Complete Token Reference (Current + Proposed)

### Current Tokens (Implemented)

```
SpacingTokens:   none(0) xxs(2) xs(4) sm(8) md(16) lg(24) xl(32) xxl(48) huge(64)
                 minTouchTarget(48) minTouchTargetSpatial(60)

ShapeTokens:     none(0) xs(4) sm(8) md(12) lg(16) xl(20) xxl(24) full(9999)

SizeTokens:      iconSm(16) iconMd(24) iconLg(32) iconXl(48)
                 buttonHeightSm(32) buttonHeightMd(40) buttonHeightLg(48) buttonHeightXl(56)
                 minTouchTarget(48) minTouchTargetSpatial(60)
                 textFieldHeight(56) textFieldHeightSm(40) textFieldHeightCompact(36)
                 appBarHeight(56) appBarHeightCompact(48) bottomNavHeight(56)
                 chatBubbleMaxWidth(320) commandBarHeight(64) voiceButtonSize(56)

ElevationTokens: none(0) xs(1) sm(2) md(4) lg(8) xl(12) xxl(16)

AnimationTokens: fast(100ms) normal(200ms) medium(300ms) slow(500ms) extraSlow(1000ms)

GlassTokens:     lightOverlay(0.08) mediumOverlay(0.12) heavyOverlay(0.18)
                 lightBlur(6dp) mediumBlur(8dp) heavyBlur(10dp)
                 borderTopOpacity(0.30) borderBottomOpacity(0.15)
                 surfaceTopOpacity(0.10) surfaceTintOpacity(0.15) surfaceBottomOpacity(0.05)
                 shadowOpacity(0.25) glowOpacity(0.20) shadowElevation(8dp)
                 borderSubtle(0.5dp) borderDefault(1dp) borderStrong(1.5dp) borderFocused(2dp)

ResponsiveTokens: compactMax(599) mediumMin(600) expandedMin(840) largeMin(1240)
                  glassMicroMax(479) glassCompactMin(480) glassStandardMin(854) glassHdMin(1280)
                  gridColumns: compact(4) medium(8) expanded(12)
                  margins: compact(16) medium(24) glass(8)
                  gutters: compact(16) medium(24) glass(8)

TypographyTokens: Full M3 scale (displayLarge 57sp ... labelSmall 11sp)

SpatialSizeTokens: hudDistance(0.5f) interactiveDistance(1.0f) primaryDistance(1.5f)
                   spatialTouchTarget(60dp) maxVoiceOptions(3) commandHierarchyLevels(2)
```

### Proposed New Tokens

```
SpringTokens (NEW):
  press: dampingRatio(0.7) stiffness(800)
  morph: dampingRatio(0.8) stiffness(300)
  appear: dampingRatio(0.75) stiffness(400)
  shimmer: dampingRatio(0.85) stiffness(200)
  scroll: dampingRatio(0.75) stiffness(350)

GlassAccessibilityTokens (NEW):
  reduceTransparency.fillOpacity: 0.75
  increaseContrast.borderWidth: 2dp
  increaseContrast.borderOpacity: 0.8
  reduceMotion.animationType: CROSS_FADE

ShadowTokens (NEW):
  level0: none
  level1: ambient(Black@8%, blur=16dp) key(Black@12%, blur=8dp, y=4dp)
  level2: ambient(Black@10%, blur=20dp) key(Black@15%, blur=10dp, y=6dp)
  level3: ambient(Black@12%, blur=24dp) key(Black@18%, blur=12dp, y=8dp)
  level4: ambient(Black@14%, blur=28dp) key(Black@20%, blur=14dp, y=10dp)
  level5: ambient(Black@16%, blur=32dp) key(Black@22%, blur=16dp, y=12dp)

GlassTokens.Liquid (NEW - Liquid theme specific):
  cardCornerRadius: 28dp
  pillCornerRadius: 999dp
  sheetCornerRadius: 34dp
  strokeSubtleOpacity: 0.22
  strokeStrongOpacity: 0.35
  highlightOpacity: 0.16
  shadowRadius: 18dp
  shadowY: 8dp
  shadowOpacity: 0.18

SeeThrough (NEW - Glass display specific):
  backgroundOpacity: 0.0 (transparent)
  borderOpacity: 0.65
  textColor: White (#FFFFFF)
  borderColor: White (#FFFFFF)
  iconStyle: FILLED (not outlined)
  blurEnabled: false
  shadowEnabled: false
```

### Proposed Light Mode Color Values

```
OceanColorsLight:
  primary: #2563EB (darker blue for contrast on light)
  background: #F8FAFC (light slate)
  surface: #FFFFFF (white)
  surfaceElevated: #F1F5F9 (light slate 100)
  surfaceVariant: #E2E8F0 (light slate 200)
  textPrimary: #0F172A (slate 900)
  textSecondary: #475569 (slate 600)
  textTertiary: #94A3B8 (slate 400)
  border: rgba(0, 0, 0, 0.12)
  borderSubtle: rgba(0, 0, 0, 0.06)
  borderStrong: rgba(0, 0, 0, 0.20)
  glass.overlayColor: Color.Black (dark overlay on light)
  glass.tintColor: #F1F5F9

SunsetColorsLight:
  primary: #E55A2B (deeper coral)
  background: #FFF7F0 (warm white)
  surface: #FFFFFF
  surfaceElevated: #FFF1E6 (peach tint)
  textPrimary: #1A0E1F (deep warm)
  glass.overlayColor: Color.Black
  glass.tintColor: #FFF1E6

LiquidColorsLight:
  primary: #0099CC (deeper cyan for contrast)
  background: #F5F5F7 (Apple white)
  surface: #FFFFFF
  surfaceElevated: #F2F2F7 (Apple system gray 7)
  textPrimary: #1C1C1E (Apple system label)
  glass.overlayColor: Color.Black
  glass.tintColor: #F2F2F7
```

---

## Appendix B: Sources

### Apple / Liquid Glass
- [Apple Newsroom: Liquid Glass Announcement](https://www.apple.com/newsroom/2025/06/apple-introduces-a-delightful-and-elegant-new-software-design/)
- [WWDC25 Session 219: Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/)
- [WWDC25 Session 323: Build a SwiftUI app with the new design](https://developer.apple.com/videos/play/wwdc2025/323/)
- [Apple Developer: Applying Liquid Glass to Custom Views](https://developer.apple.com/documentation/SwiftUI/Applying-Liquid-Glass-to-custom-views)
- [Apple Developer: Glass struct](https://developer.apple.com/documentation/swiftui/glass)
- [Apple Developer: glassEffect modifier](https://developer.apple.com/documentation/swiftui/view/glasseffect(_:in:))
- [LiquidGlassReference (GitHub)](https://github.com/conorluddy/LiquidGlassReference)
- [Build a Liquid Glass Design System in SwiftUI](https://levelup.gitconnected.com/build-a-liquid-glass-design-system-in-swiftui-ios-26-bfa62bcba5be)
- [iOS 26 Liquid Glass: Comprehensive Reference](https://medium.com/@madebyluddy/overview-37b3685227aa)
- [Getting Clarity on Apple's Liquid Glass (CSS-Tricks)](https://css-tricks.com/getting-clarity-on-apples-liquid-glass/)
- [Liquid Glass in the Browser (kube.io)](https://kube.io/blog/liquid-glass-css-svg/)
- [LiquidGlassKit backport](https://github.com/DnV1eX/LiquidGlassKit)
- [Recreating Apple's Liquid Glass with Pure CSS](https://dev.to/kevinbism/recreating-apples-liquid-glass-effect-with-pure-css-3gpl)

### Spatial Design / Depth
- [Material Design 3: Elevation](https://m3.material.io/styles/elevation/applying-elevation)
- [Elevation Design Patterns: Tokens, Shadows, and Roles](https://designsystems.surf/articles/depth-with-purpose-how-elevation-adds-realism-and-hierarchy)
- [Material 3 Expressive: What's New](https://supercharge.design/blog/material-3-expressive)
- [Liquid Glass vs Material 3 Expressive](https://orangeloops.com/2025/12/liquid-glass-vs-material-3-expressive-next%E2%80%91gen-ui-design-compared/)
- [The Parallax Effect: Enhancing Depth in Web Design](https://garagefarm.net/blog/parallax-effect-best-practices-and-examples)

### Smart Glasses / AR
- [Google Glass Enterprise Design Guidelines](https://developers.google.com/glass-enterprise/guides/design-guidelines)
- [Google Glass Enterprise Style Guidelines](https://developers.google.com/glass-enterprise/guides/style-guidelines)
- [Vuzix Blade UX Design Guidelines](http://files.vuzix.com/Content/Upload/Vuzix%20Blade%20UX%20Design%20Guidelines_v2.pdf)
- [Vuzix UI Best Design Practices](https://support.vuzix.com/docs/ui-best-design-practices)
- [Vuzix UI Design Styles](https://support.vuzix.com/docs/ui-design-styles)
- [RealWear Navigator 500 Series Display Comparison](https://support.realwear.com/knowledge/realwear-navigator-500-series-display-comparison)
- [RealWear Navigator 520 Specifications](https://support.realwear.com/knowledge/device-specifications)
- [Ensuring Interface Legibility in visionOS](https://www.createwithswift.com/ensuring-interface-legibility-and-contrast-in-visionos/)

### Accessibility
- [Apple's Liquid Glass: Accessible? (Infinum)](https://infinum.com/blog/apples-ios-26-liquid-glass-sleek-shiny-and-questionably-accessible/)
- [Liquid Glass: Practical Guidance (Designed for Humans)](https://designedforhumans.tech/blog/liquid-glass-smart-or-bad-for-accessibility)
- [Liquid Glass Is Cracked (NN/g)](https://www.nngroup.com/articles/liquid-glass/)
- [iOS 26.2 Fixed Liquid Glass (BGR)](https://www.bgr.com/2070522/ios-26-2-fixed-liquid-glass-new-settings-options/)
- [WCAG 2.1 Contrast Minimum](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [WCAG 1.4.1 Use of Color](https://www.w3.org/WAI/WCAG21/Understanding/use-of-color.html)
- [Glassmorphism Meets Accessibility (Axess Lab)](https://axesslab.com/glassmorphism-meets-accessibility-can-frosted-glass-be-inclusive/)
- [Glassmorphism and Accessibility (NN/g)](https://www.nngroup.com/articles/glassmorphism/)
- [Liquid Glass Token Generator (GitHub Gist)](https://gist.github.com/saaeiddev/9e399914aafa14e3514440277d5e80bb)

### Design System Architecture
- [W3C Design Tokens Spec 2025.10](https://www.w3.org/community/design-tokens/2025/10/28/design-tokens-specification-reaches-first-stable-version/)
- [Design System Mastery with Figma Variables](https://www.designsystemscollective.com/design-system-mastery-with-figma-variables-the-2025-2026-best-practice-playbook-da0500ca0e66)
- [Evolution of Design System Tokens 2025](https://www.designsystemscollective.com/the-evolution-of-design-system-tokens-a-2025-deep-dive-into-next-generation-figma-structures-969be68adfbe)
- [Schema 2025: Design Systems For A New Era (Figma)](https://www.figma.com/blog/schema-2025-design-systems-recap/)
- [Preparing for the Design Tokens Era](https://medium.com/@dimiganin/preparing-for-the-design-tokens-era-multi-brand-systems-and-figmas-extended-collections-9fd35ccd06df)

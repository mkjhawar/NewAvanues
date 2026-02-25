# Cockpit-Spec-SmartGlassesUIDesign-260223-V1

**Smart Glasses & PseudoSpatial UI Design Specification**

Author: Manoj Jhawar | Version: 1.0 | Date: 2026-02-23
Module: `Modules/Cockpit/` | Theme: AvanueUI v5.1 | Branch: VoiceOS-1M-SpeechEngine

---

## 1. Hardware Reference Matrix

Devices mapped to `DisplayProfile` enum (`Modules/AvanueUI/.../display/DisplayProfile.kt`).

| Device | Type | Resolution | FOV | PPD | DisplayProfile | Input |
|--------|------|-----------|-----|-----|----------------|-------|
| Phones | Flat LCD/OLED | 1080x2400+ | N/A | N/A | `PHONE` | Touch + Voice |
| Tablets | Flat LCD/OLED | 1600x2560+ | N/A | N/A | `TABLET` | Touch + Voice |
| Vuzix M4000 | See-through mono | 854x480 | 28deg | 30.5 | `GLASS_STANDARD` | Voice + Head tilt |
| RealWear Nav 500 | See-through mono | 854x480 | 20deg | 42.7 | `GLASS_COMPACT` | Voice only |
| Vuzix Blade 2 | See-through bino | 480x480/eye | 20deg | 24.0 | `GLASS_MICRO` | Touch + Voice |
| Epson Moverio BT-45 | See-through bino | 1920x1080 | 34deg | 56.5 | `GLASS_STANDARD` | Touch + Voice |
| Rokid Max 2 | Opaque bino | 1920x1080/eye | 50deg | 38.4 | `GLASS_HD` | Controller + Voice |
| XReal Air 2 | Opaque bino | 1920x1080/eye | 46deg | 41.7 | `GLASS_HD` | Phone as controller |

### DisplayProfile Properties (from source)

| Profile | densityScale | fontScale | minTouchTarget | layoutStrategy |
|---------|-------------|-----------|----------------|----------------|
| `GLASS_MICRO` | 0.625 | 0.75 | 36dp | SINGLE_PANE_PAGINATED |
| `GLASS_COMPACT` | 0.75 | 0.85 | 40dp | SINGLE_PANE_SCROLL |
| `GLASS_STANDARD` | 0.875 | 0.9 | 44dp | SINGLE_PANE_SCROLL |
| `PHONE` | 1.0 | 1.0 | 48dp | ADAPTIVE |
| `TABLET` | 1.0 | 1.0 | 48dp | LIST_DETAIL |
| `GLASS_HD` | 0.9 | 0.95 | 48dp | ADAPTIVE |

Three display categories determine the rendering pipeline:

| Category | Color Scheme | Canvas | Devices |
|----------|-------------|--------|---------|
| Flat Screen | `palette.colors(isDark)` | PseudoSpatialCanvas | Phone, Tablet |
| See-Through | `palette.colorsXR` | SpatialCanvas + GlassFrameChrome | M4000, Nav500, Blade 2, BT-45 |
| Opaque Glass | `palette.colors(isDark=true)` | SpatialCanvas + StandardChrome | Rokid Max 2, XReal Air 2 |

---

## 2. Text Sizing Guidelines

### PPD-Based Minimum Size

Formula: **`minBodyPx = PPD x 0.6`** (0.6deg = 36 arcminutes, 20/40 visual acuity).

| Device | PPD | Min Body (px) | Body (sp) | Heading (sp) | Label (sp) |
|--------|-----|--------------|-----------|-------------|-----------|
| Vuzix Blade 2 | 24.0 | 14 | 16 | 22 | 14 |
| Vuzix M4000 | 30.5 | 18 | 18 | 24 | 16 |
| Rokid Max 2 | 38.4 | 23 | 16 | 22 | 14 |
| XReal Air 2 | 41.7 | 25 | 16 | 22 | 14 |
| RealWear Nav 500 | 42.7 | 26 | 16 | 20 | 14 |
| Epson BT-45 | 56.5 | 34 | 14 | 20 | 12 |
| Phone / Tablet | N/A | N/A | 14 | 20 | 12 |

Actual rendered size = sp x `DisplayProfile.fontScale`. Low-PPD devices get larger sp values.

### Contrast & Weight

| Display Type | Min Contrast | Body Weight | Heading Weight |
|-------------|-------------|------------|---------------|
| See-through (outdoor) | **7:1** WCAG AAA | Medium (500) | Bold (700) |
| See-through (indoor) | **5:1** | Medium (500) | Bold (700) |
| Opaque glass | **4.5:1** WCAG AA | Regular (400) | SemiBold (600) |
| Flat screen | **4.5:1** WCAG AA | Regular (400) | SemiBold (600) |

See-through uses heavier weights because thin strokes wash out against bright backgrounds.

---

## 3. Color Guidelines

### See-Through Additive Displays

The display is additive: light pixels add to the real world. **Black = invisible.**

- Use `palette.colorsXR` exclusively (never `palette.colors(isDark)`)
- Avoid colors below 40% luminance (vanish outdoors); cap at 90% (glare)
- Backgrounds: semi-transparent tints only, never opaque dark

### XR Palette Hex Values (HydraColorsXR)

| Token | Hex | Token | Hex |
|-------|-----|-------|-----|
| `primary` | `#60A5FA` | `textPrimary` | `#F8FAFC` |
| `secondary` | `#A78BFA` | `textSecondary` | `#CBD5E1` |
| `tertiary` | `#34D399` | `textTertiary` | `#94A3B8` |
| `error` | `#F87171` | `surface` | `#0F172A` @ 15% |
| `iconPrimary` | `#60A5FA` | `surfaceElevated` | `#1E293B` @ 25% |
| `border` | `#FFFFFF` @ 20% | `background` | transparent |

All four palettes provide XR variants: `HydraColorsXR`, `SolColorsXR`, `LunaColorsXR`, `TerraColorsXR` -- always-dark with boosted luminance and semi-transparent containers.

### Environment-Adaptive Brightness

| Tier | Ambient Lux | Text Boost | Surface Alpha |
|------|------------|-----------|---------------|
| Indoor | 0-500 | 0% | 15% |
| Overcast | 500-5,000 | +10% | 20% |
| Bright | 5,000-30,000 | +20% | 30% |
| Direct Sun | 30,000+ | +40% | 40% |

Transitions interpolate over 500ms via ambient light sensor. Opaque glass: standard dark theme. Flat screen: `AppearanceMode.Auto`.

---

## 4. Layout Density Budget

FOV constrains perceivable information. These are hard maximums per screen.

| FOV Range | Devices | Max Interactive | Max List Items | Nav Pattern |
|-----------|---------|----------------|----------------|-------------|
| 15-20deg | Nav 500, Blade 2 | 5 | 3 | Paginated, voice-numbered |
| 25-35deg | M4000, BT-45 | 8 | 5-7 | Paginated or scroll |
| 40-52deg | Rokid Max 2, XReal Air 2 | 15 | 10 | Scroll or grid |
| Unbounded | Phone, Tablet | No limit | Standard | Adaptive / ListDetail |

Density formula: `total = (interactive x 1.0) + (info_chunks x 0.5)`. If `total > budget`, paginate.

---

## 5. PseudoSpatial Design (Flat Screens)

4-layer parallax system driven by device gyroscope for phones/tablets.

### Layer Architecture

| Layer | Content | Gyro Multiplier | Max Offset | Z-Index |
|-------|---------|----------------|-----------|---------|
| 3 (HUD) | Status bar, command bar | 0.0x | 0dp | 30 |
| 2 (Foreground) | Module tiles, action cards | 1.0x | 4dp | 20 |
| 1 (Mid-ground) | Session cards, data panels | 0.6x | 8dp | 10 |
| 0 (Background) | Grid pattern, ambient glow | 0.3x | 12dp | 0 |

Gyro smoothing: low-pass filter alpha=0.15 at 60fps. Sensitivity: 0.0 (disabled) to 2.0.
Motion-reduced accessibility: all multipliers forced to 0.0.

### 3D Card Transforms

| State | Scale | RotationY | TranslationZ | Opacity |
|-------|-------|-----------|-------------|---------|
| Active | 1.00 | 0deg | 0dp | 1.0 |
| Adjacent | 0.85 | +/-12deg | -16dp | 0.85 |
| Far | 0.70 | +/-20deg | -32dp | 0.6 |
| Offscreen | 0.50 | +/-25deg | -48dp | 0.0 |

Active card gyro tilt: +/-3deg max on X and Y axes.

### Cockpit HUD Aesthetic

- **Scanline grid** (Layer 0): 48dp spacing, 1dp lines, `colors.border` at 5% opacity, drifts 0.5dp/sec
- **Corner accents** (Layer 3): 2dp lines, 12dp length, `colors.primary` at 60% at panel corners
- **Status gauges**: 48dp circular arcs (flight-instrument style), 4dp stroke, `primary` fill / `surfaceVariant` track

```
+--                                  --+
|   +------------------------------+   |
|   | Panel Title                  |   |
|   | Content area                 |   |
|   +------------------------------+   |
+--                                  --+
  ^^ 12dp corner accent lines (2dp stroke)
```

---

## 6. True Spatial Design (See-Through Glasses)

PseudoSpatial is disabled on glass. Real-world provides spatial context.

### Panel Placement

- **Head-locked** (primary): tracks head rotation, 50ms smoothing, 80% horiz / 70% vert FOV max
- **World-anchored** (pinned): fixed at +/-30deg offset, max 2 panels, for pinned Cockpit frames

### Text Halo (mandatory on see-through)

```kotlin
Modifier
    .shadow(blurRadius = 3.dp, color = Color.Black.copy(alpha = 0.6f))
    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
```

3dp blur minimum (5dp headings). Never rely on contrast alone on see-through displays.

### Peek Panel Pattern

Adjacent frames show 24dp edge strips with rotated title text + accent color. Voice: "show left" / "show right" to swap.

```
+--+----------------------------+--+
|Pk|      Active Frame          |Pk|
|L |      (head-locked)         |R |
+--+----------------------------+--+
```

---

## 7. Dashboard Layout Per Device

### Phone (ADAPTIVE)

3-column grid (portrait) / 5-column (landscape), 100dp tiles, `SpacingTokens.sm` gap.
Recent sessions: horizontal LazyRow, 280dp cards.
Transition: 400ms scale+fade, spring damping 0.8.

### Tablet (LIST_DETAIL)

`ListDetailPaneScaffold` -- 40% list pane, 60% detail pane.
Transition: 300ms shared-element crossfade.

### Narrow FOV Glass (GLASS_MICRO / GLASS_COMPACT)

Paginated voice-numbered list, max **5 items/page**. Say "one" through "five" to select.
"next page" / "back" to navigate. 150ms fade-only transitions. No horizontal scroll.

```
+----------------------------+
| Cockpit          Page 1/3  |
+----------------------------+
|  (1) WebAvanue         >   |
|  (2) PhotoAvanue       >   |
|  (3) NoteAvanue        >   |
|  (4) VoiceCursor       >   |
|  (5) MediaAvanue       >   |
+----------------------------+
| "say number" / "next"      |
+----------------------------+
```

### Medium FOV Glass (GLASS_STANDARD)

Paginated list, max **7 items/page**. Can show secondary info per row. 200ms fade-only.

### Wide FOV Glass (GLASS_HD)

2-column grid (not 3), 120dp tiles. Voice "next card" / "previous card". 250ms fade+scale.

### Animation Timing

| Profile | Enter | Exit | Easing |
|---------|-------|------|--------|
| PHONE | 400ms | 300ms | Spring (0.8) |
| TABLET | 300ms | 250ms | EaseInOutCubic |
| GLASS_MICRO/COMPACT | 150ms | 100ms | Linear |
| GLASS_STANDARD | 200ms | 150ms | EaseOut |
| GLASS_HD | 250ms | 200ms | EaseOut |

Glass avoids spring/bounce (causes motion discomfort on head-mounted displays).

---

## 8. Vuzix M4000 Specific Guidelines

Tier-1 device: 854x480, 28deg FOV, 30.5 PPD, right-eye monocular, voice-only input.

### Layout Rules

- **Right-justify** status indicators, confirmations, numeric values (right eye favors right-aligned)
- Left-align body text and list items (natural reading direction)
- **Max 5 interactive elements** per screen (hard limit for 28deg FOV)

### Typography

| Element | Size | Weight | Color |
|---------|------|--------|-------|
| Body | 18sp | Medium | `textPrimary` #F8FAFC |
| Heading | 24sp | Bold | `primary` #60A5FA |
| Label | 16sp | Medium | `textSecondary` #CBD5E1 |
| Voice number | 22sp | Bold | `tertiary` #34D399 |

### Voice Commands

```
"one".."five"    -> select item       "next" / "back" -> paginate
"go home"        -> dashboard         "scroll up/down" -> scroll
"rest mode"      -> dim to clock      "hey avanue"     -> wake
```

### Performance

| Metric | Target |
|--------|--------|
| Frame rate | 30 FPS (waveguide limit) |
| Animation | Fade-only (no scale/rotate) |
| Max thumbnail | 128x128 px |
| Max concurrent images | 4 |

### Power & Fatigue

- Green (#34D399) for persistent indicators (lowest waveguide power)
- Auto-dim after 30s idle, display off after 2min (voice stays active)
- 30-minute binocular rivalry fatigue reminder (non-blocking)
- "rest mode" voice command: dims to minimum, clock only
- 60-minute hard reminder with countdown

---

## 9. GlassDisplayMode Integration

Maps `GlassDisplayMode` enum (DeviceManager) to AvanueUI rendering:

```kotlin
when (glassDisplayMode) {
    null -> {                                    // Phone/Tablet
        colors = palette.colors(isDark)
        canvas = PseudoSpatialCanvas; chrome = StandardChrome
    }
    STANDARD, MINIMAL -> {                       // See-through glass
        colors = palette.colorsXR
        canvas = SpatialCanvas; chrome = GlassFrameChrome
    }
    HIGH_BRIGHTNESS -> {                         // See-through outdoor
        colors = palette.colorsXR.boosted(+0.2f)
        canvas = SpatialCanvas; chrome = GlassFrameChrome
    }
    DETAILED -> {                                // Opaque glass
        colors = palette.colors(isDark = true)
        canvas = SpatialCanvas; chrome = StandardChrome
    }
    STEREOSCOPIC -> {                            // Dual-eye rendering
        colors = palette.colorsXR
        canvas = StereoscopicCanvas; chrome = GlassFrameChrome
    }
    NIGHT_MODE -> {                              // Red-shifted spectrum
        colors = palette.colorsXR.redShift()
        canvas = SpatialCanvas; chrome = GlassFrameChrome
    }
    PASSTHROUGH -> {                             // Minimal AR overlay
        colors = palette.colorsXR
        canvas = PassthroughCanvas; chrome = MinimalChrome
    }
}
```

### Decision Flow

```
Smart glass? --NO--> PHONE/TABLET: colors(isDark), PseudoSpatialCanvas
     |
    YES --> See-through? --YES--> colorsXR, SpatialCanvas, text halo ON, fade-only
                  |
                  NO --> colors(dark), SpatialCanvas, text halo OFF, fade+scale
```

---

## 10. Design Token Tables

### Spacing (Universal -- density scaling handles per-profile differences)

| Token | dp | Physical on GLASS_MICRO | Physical on PHONE |
|-------|----|------------------------|-------------------|
| `spacing.xs` | 4 | 2.5 | 4 |
| `spacing.sm` | 8 | 5 | 8 |
| `spacing.md` | 16 | 10 | 16 |
| `spacing.lg` | 24 | 15 | 24 |
| `spacing.xl` | 32 | 20 | 32 |
| `spacing.xxl` | 48 | 30 | 48 |

### Shape (Corner Radius)

Tokens are universal. On see-through glass, prefer `shape.sm` (8dp) or `shape.md` (12dp) -- large radii waste FOV pixels.

| `shape.xs` | `shape.sm` | `shape.md` | `shape.lg` | `shape.xl` | `shape.full` |
|-----------|-----------|-----------|-----------|-----------|-------------|
| 4dp | 8dp | 12dp | 16dp | 20dp | 9999dp |

### Elevation

See-through: always 0dp (use `border`/`borderSubtle` tokens instead -- additive displays cannot render shadows). Opaque/flat: standard `ElevationTokens` (0/1/2/4/8/12dp).

### Touch/Gesture Targets

| Profile | Min | Recommended | Voice Badge |
|---------|-----|------------|-------------|
| GLASS_MICRO | 36dp | 44dp | 28dp circle |
| GLASS_COMPACT | 40dp | 48dp | 32dp circle |
| GLASS_STANDARD | 44dp | 52dp | 32dp circle |
| PHONE/TABLET | 48dp | 56dp | N/A |
| GLASS_HD | 48dp | 56dp | 36dp circle |

### XR Palette Comparison

| Token | Hydra | Sol | Luna | Terra |
|-------|-------|-----|------|-------|
| `primary` | `#60A5FA` | `#FBBF24` | `#818CF8` | `#34D399` |
| `secondary` | `#A78BFA` | `#F87171` | `#C084FC` | `#FBBF24` |
| `tertiary` | `#34D399` | `#34D399` | `#22D3EE` | `#60A5FA` |
| `textPrimary` | `#F8FAFC` | `#FFFBEB` | `#F8FAFC` | `#F0FDF4` |
| `surface` | `#0F172A`@15% | `#451A03`@15% | `#1E1B4B`@15% | `#052E16`@15% |

### Animation Duration (Glass-Adjusted)

| Token | Glass (micro/compact) | Glass (standard/HD) | Phone/Tablet |
|-------|-----------------------|---------------------|-------------|
| `fast` | 80ms | 100ms | 100ms |
| `normal` | 150ms | 200ms | 200ms |
| `medium` | 200ms | 250ms | 300ms |
| `slow` | 300ms | 400ms | 500ms |

Glass uses shorter durations: head-mounted animation feels ~1.5x faster due to vestibular coupling.

---

## Appendix: Quick Decision Matrix

| Question | Answer |
|----------|--------|
| Color scheme? | See-through: `colorsXR`. Opaque: `colors(true)`. Flat: `colors(isDark)` |
| Canvas? | Flat: PseudoSpatial. Glass: Spatial |
| Animations? | Flat: all. Glass: fade-only (micro), fade+scale (HD) |
| Max items? | FOV<20: 3-5. FOV 25-35: 5-7. FOV 40+: 10-15. Flat: unlimited |
| Font weight? | See-through: Medium/Bold. Others: Regular/SemiBold |
| Elevation? | See-through: 0dp (borders). Others: standard tokens |
| Text halo? | See-through: always. Others: never |

## Implementation Checklist

- [ ] `DisplayProfileResolver` returns correct profile per detected device
- [ ] `GlassDisplayMode` maps to rendering pipeline (Section 9)
- [ ] XR color schemes applied for all see-through devices
- [ ] PseudoSpatial parallax with 4 layers (Section 5)
- [ ] Voice-numbered pagination for GLASS_MICRO/COMPACT
- [ ] Text halo modifier on all see-through text
- [ ] Adaptive brightness tiers via ambient light sensor
- [ ] M4000: right-justified status, fatigue reminders
- [ ] Animation durations scaled per DisplayProfile
- [ ] FOV density budgets enforced (Section 4)
- [ ] Dashboard layout adapts per profile (Section 7)
- [ ] Corner accents and scanline grid for PseudoSpatial HUD
- [ ] Peek panel pattern for glass frame navigation

---

*Copyright (C) Manoj Jhawar / Intelligent Devices LLC. All rights reserved.*

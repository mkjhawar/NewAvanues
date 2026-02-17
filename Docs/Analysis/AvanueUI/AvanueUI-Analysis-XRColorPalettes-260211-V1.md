# AR/VR/XR Color Palette & UI Design Systems Research

**Date:** 2026-02-11
**Module:** AvanueUI
**Type:** Research Analysis

## Key Findings

### 1. No "Google LiquidUI" Exists
- "Liquid Glass" is Apple's design language (visionOS + iOS 26)
- Google's equivalent: **Material Design 3 Expressive** with HCT color space
- Android XR uses standard M3 tokens with spatial behaviors

### 2. Additive Display Rules (AR Glasses)
- **Black = fully transparent** on additive displays
- "Dark theme" on AR = **no background** with bright foreground
- White is the boldest/brightest color (inverted from phones)
- Minimum see-through contrast ratio: **1:1.4**
- Need brighter variants of all palette primaries for AR

### 3. Eye Fatigue
- Red text = highest fatigue; yellow = lowest
- Optimal saturation: 31-41% for sustained use
- Cool colors (blue, green) promote relaxation
- Negative polarity (light on dark) reduces VR fatigue

### 4. Platform Comparison

| Platform | Background | Black? | Recommended Base |
|----------|-----------|--------|-----------------|
| Phone | Opaque | Yes | `#121212` dark / `#FAFAFA` light |
| VR Headset | Opaque | Yes | `#121212` dark |
| AR Glasses | Transparent | No | No background, bright foreground |
| visionOS | Adaptive glass | Partial | System glass material |

### 5. AvanueUI Material Style Mapping

| AvanueUI | Industry Equivalent |
|----------|-------------------|
| Glass | Apple visionOS / Liquid Glass |
| Water | AvanueUI-original (no match) |
| Cupertino | Apple iOS flat (pre-Liquid Glass) |
| MountainView | Google M3 |

### 6. XR-Optimized Palette Values

**Hydra XR**: Primary `#60A5FA`, Accent `#A78BFA`, Text `#F8FAFC`
**Sol XR**: Primary `#FBBF24`, Accent `#F87171`, Text `#FEF3C7`
**Luna XR**: Primary `#CBD5E1`, Accent `#93C5FD`, Text `#F1F5F9`
**Terra XR**: Primary `#4ADE80`, Accent `#A3E635`, Text `#F0FDF4`

### 7. Future Token Categories for XR

1. Luminance floor (min brightness for additive, >= 0.3)
2. Panel alpha (70-80% primary, 40-50% secondary)
3. Text shadow/outline for AR legibility
4. Vibrancy levels (Primary/Secondary/Tertiary)
5. Spatial elevation (Z-axis dp values)
6. Touch targets (60pt spatial, 48dp phone)

### Sources
- Android XR Visual Design: developer.android.com
- Apple visionOS HIG: developer.apple.com
- Meta Horizon OS: developers.meta.com/horizon/design/
- Snap Spectacles: developers.snap.com/spectacles
- FloatGrids: floatgrids.com
- M3 Expressive: supercharge.design/blog/material-3-expressive
- Apple Liquid Glass: developer.apple.com/documentation/TechnologyOverviews/liquid-glass

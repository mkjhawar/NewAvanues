# Heatmap Color Schemes - Visual Reference

## Overview

The iOS Heatmap component supports 4 color schemes optimized for different data types and accessibility requirements.

---

## 1. BlueRed (Default)

**Best For:** Temperature data, general-purpose visualization

**Color Range:**
```
Low  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  High
🔵 Blue (#2196F3)  →  🔴 Red (#F44336)
```

**Interpolation Points:**
- 0.0 (min): `rgb(33, 150, 243)` - Material Blue 500
- 0.5 (mid): `rgb(143, 196, 239)` - Purple-ish blend
- 1.0 (max): `rgb(244, 67, 54)` - Material Red 500

**Example:**
```
Value:   0    25    50    75   100
Color:  🔵   🔵🟣   🟣   🟣🔴   🔴
```

**Use Cases:**
- Temperature maps (cold to hot)
- Performance metrics (low to high)
- Risk levels (safe to danger)
- General data visualization

---

## 2. GreenRed

**Best For:** Performance metrics, health indicators, good/bad scales

**Color Range:**
```
Low  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  High
🟢 Green (#4CAF50)  →  🔴 Red (#F44336)
```

**Interpolation Points:**
- 0.0 (min): `rgb(76, 175, 80)` - Material Green 500
- 0.5 (mid): `rgb(160, 121, 67)` - Yellow-brown blend
- 1.0 (max): `rgb(244, 67, 54)` - Material Red 500

**Example:**
```
Value:   0    25    50    75   100
Color:  🟢   🟢🟡   🟡   🟠🔴   🔴
```

**Use Cases:**
- Health metrics (healthy to critical)
- Performance scores (good to poor)
- Error rates (low to high)
- Quality indicators (pass to fail)

**Psychological Association:**
- Green = Good, Safe, Positive
- Red = Bad, Danger, Negative

---

## 3. Grayscale

**Best For:** Print-friendly, monochrome displays, minimalist design

**Color Range:**
```
Low  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  High
⚪ White (#FFFFFF)  →  ⚫ Black (#000000)
```

**Interpolation Points:**
- 0.0 (min): `rgb(255, 255, 255)` - White
- 0.5 (mid): `rgb(128, 128, 128)` - Medium Gray
- 1.0 (max): `rgb(0, 0, 0)` - Black

**Example:**
```
Value:   0    25    50    75   100
Color:  ⚪   ⬜   🔲   ⬛   ⚫
```

**Use Cases:**
- Print publications (saves ink)
- Monochrome displays
- Minimalist dashboards
- Focus on data patterns over aesthetics

**Text Contrast:**
- Light cells: Black text (WCAG AA)
- Dark cells: White text (WCAG AA)

---

## 4. Viridis (Perceptually Uniform)

**Best For:** Scientific visualization, color-blind accessibility, accurate perception

**Color Range:**
```
Low  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  High
🟣 Purple  →  🔵 Blue  →  🟦 Teal  →  🟢 Green  →  🟡 Yellow
```

**Interpolation Points (5-stop gradient):**

| Fraction | Color | RGB | Description |
|----------|-------|-----|-------------|
| 0.00 | 🟣 | `rgb(68, 1, 84)` | Dark Purple |
| 0.25 | 🔵 | `rgb(49, 75, 140)` | Dark Blue |
| 0.50 | 🟦 | `rgb(31, 158, 137)` | Teal |
| 0.75 | 🟢 | `rgb(93, 200, 99)` | Green |
| 1.00 | 🟡 | `rgb(253, 231, 37)` | Yellow |

**Example:**
```
Value:    0     20    40    60    80   100
Color:   🟣    🔵    🟦    🟢    🟨    🟡
```

**Why Viridis?**

1. **Perceptually Uniform:** Equal visual steps represent equal data steps
2. **Color-Blind Friendly:** Works for protanopia, deuteranopia, tritanopia
3. **Grayscale Conversion:** Maintains order when printed in grayscale
4. **High Contrast:** Easy to distinguish adjacent values
5. **Scientific Standard:** Used in matplotlib, seaborn, scientific publications

**Use Cases:**
- Scientific data visualization
- Medical imaging
- Accessibility-critical applications
- Academic publications
- Geographic heat maps
- Density plots

**Accessibility:**
- ✅ Protanopia (red-blind)
- ✅ Deuteranopia (green-blind)
- ✅ Tritanopia (blue-blind)
- ✅ Grayscale conversion

---

## Comparison Matrix

| Scheme | Stops | Color-Blind Safe | Print-Friendly | Scientific | Emotional |
|--------|-------|------------------|----------------|------------|-----------|
| **BlueRed** | 2 | ❌ | ⚠️ | ⚠️ | ✅ High |
| **GreenRed** | 2 | ❌ | ⚠️ | ❌ | ✅ Very High |
| **Grayscale** | 2 | ✅ | ✅ | ✅ | ❌ None |
| **Viridis** | 5 | ✅ | ✅ | ✅ | ⚠️ Low |

**Legend:**
- ✅ Excellent
- ⚠️ Moderate
- ❌ Poor

---

## Selection Guide

### Choose BlueRed when:
- General-purpose visualization
- Emotional association important (cold/hot)
- Color-blind accessibility not critical
- Standard temperature/heat maps

### Choose GreenRed when:
- Performance metrics (good/bad scale)
- Health indicators (healthy/unhealthy)
- Quality scores (pass/fail)
- Strong emotional association needed

### Choose Grayscale when:
- Printing to black & white
- Minimalist design required
- Monochrome displays
- Reducing visual noise

### Choose Viridis when:
- Scientific accuracy required
- Color-blind users present
- Subtle gradations important
- Academic/medical context
- Maximum accessibility needed

---

## Color Interpolation Details

### Linear RGB Interpolation

All schemes use **linear RGB interpolation**:

```swift
r = startR + (endR - startR) * fraction
g = startG + (endG - startG) * fraction
b = startB + (endB - startB) * fraction
```

**Process:**
1. Normalize data value to 0.0-1.0 range
2. Calculate RGB components for start and end colors
3. Interpolate each channel independently
4. Combine into final Color

### Multi-Stop Gradients (Viridis)

Viridis uses **4 transitions** (5 stops):

```
0.00-0.25: Purple → Dark Blue
0.25-0.50: Dark Blue → Teal
0.50-0.75: Teal → Green
0.75-1.00: Green → Yellow
```

For each value:
1. Determine which transition range (e.g., 0.25-0.50)
2. Calculate local fraction within range
3. Interpolate between range's start/end colors

---

## Text Contrast

All schemes automatically select **black or white text** based on background luminance:

### WCAG Luminance Formula
```
L = 0.299 * R + 0.587 * G + 0.114 * B
```

**Decision:**
- If `L > 0.5`: Use **black text** (light background)
- If `L ≤ 0.5`: Use **white text** (dark background)

**Result:** Minimum 4.5:1 contrast ratio (WCAG AA)

---

## Example Use Cases by Scheme

### BlueRed: Temperature Sensor Data
```
Time →  00:00  06:00  12:00  18:00  24:00
Mon     🔵     🔵🟣    🟣🔴    🔴     🟣
Tue     🔵     🔵      🟣🔴    🔴     🟣
Wed     🔵🟣    🟣      🔴     🔴     🟣🔴
```
- Blue = Night (cold)
- Red = Noon (hot)

### GreenRed: Server Health Dashboard
```
Server   CPU    Memory  Disk   Network
DB-01    🟢     🟢🟡    🟢     🟢
API-01   🟢🟡   🟡🟠    🟡     🟢
WEB-01   🟠🔴   🔴      🟠🔴   🟢🟡
```
- Green = Healthy (<50%)
- Red = Critical (>80%)

### Viridis: Geographic Population Density
```
Region    North   South   East   West
Urban     🟡      🟢      🟦     🟣
Suburban  🟢      🟦      🟣     🟣
Rural     🟦      🟣      🟣     🟣
```
- Purple = Low density
- Yellow = High density

---

## Custom Color Schemes (Future)

**Potential additions:**

1. **BlueYellowRed** (3-stop)
   - Low: Blue
   - Mid: Yellow
   - High: Red

2. **PurpleWhiteOrange** (Diverging)
   - Low: Purple
   - Mid: White (neutral)
   - High: Orange

3. **Custom Builder**
   - User-defined start/end colors
   - Multi-stop support
   - Save/load presets

---

## Performance

All color schemes use **identical performance characteristics**:

- **Calculation:** O(1) per cell
- **Memory:** No color lookup tables
- **CPU:** Real-time interpolation
- **Rendering:** Hardware-accelerated Canvas

**Viridis overhead:** Minimal (4 if-statements for range selection)

---

## Accessibility Compliance

| Scheme | WCAG AA | Color-Blind | Grayscale |
|--------|---------|-------------|-----------|
| BlueRed | ✅ | ⚠️ Red-Green issues | ✅ |
| GreenRed | ✅ | ❌ Fails red-green | ✅ |
| Grayscale | ✅ | ✅ | ✅ |
| Viridis | ✅ | ✅ | ✅ |

**Recommendation:** Use **Viridis** for maximum accessibility, **Grayscale** for print, **BlueRed/GreenRed** for emotional impact.

---

**Last Updated:** 2025-11-25
**Component:** HeatmapView
**iOS Version:** 16.0+

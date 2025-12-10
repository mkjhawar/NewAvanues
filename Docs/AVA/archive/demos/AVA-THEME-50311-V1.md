# AVA AI Demo Suite - Theme Documentation

**Version**: 2.0
**Last Updated**: 2025-11-02
**Color Scheme**: Blues & Greys (Professional)

---

## 🎨 Color Palette

### Base Colors

| Color Name | Hex Code | RGB | Usage |
|------------|----------|-----|-------|
| **Slate 50** | `#f8fafc` | `248, 250, 252` | Lightest backgrounds |
| **Slate 100** | `#f1f5f9` | `241, 245, 249` | Page background |
| **Slate 200** | `#e2e8f0` | `226, 232, 240` | Subtle borders |
| **Slate 300** | `#cbd5e1` | `203, 213, 225` | Medium borders |
| **Slate 400** | `#94a3b8` | `148, 163, 184` | Disabled text |
| **Slate 500** | `#64748b` | `100, 116, 139` | Secondary text |
| **Slate 600** | `#475569` | `71, 85, 105` | Tertiary text |
| **Slate 700** | `#334155` | `51, 65, 85` | Dark text |
| **Slate 800** | `#1e293b` | `30, 41, 59` | Very dark text |
| **Slate 900** | `#0f172a` | `15, 23, 42` | Primary text |

### Accent Colors (Blue Gradient)

| Color Name | Hex Code | RGB | Usage |
|------------|----------|-----|-------|
| **Blue 600** | `#2563eb` | `37, 99, 235` | Primary blue |
| **Blue 700** | `#1d4ed8` | `29, 78, 216` | Hover states |
| **Blue 800** | `#1e40af` | `30, 64, 175` | Deep blue (gradient end) |

**Primary Gradient**:
```css
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
```

### System Status Colors

| Status | Color Name | Hex Code | RGB | Usage |
|--------|-----------|----------|-----|-------|
| **Success** | iOS Green | `#34c759` | `52, 199, 89` | Success states, checkmarks |
| **Warning** | iOS Orange | `#ff9500` | `255, 149, 0` | Warnings, medium priority |
| **Error** | iOS Red | `#ff3b30` | `255, 59, 48` | Errors, critical states |

### Neutral Colors

| Color Name | Hex Code | RGB | Usage |
|------------|----------|-----|-------|
| **White** | `#ffffff` | `255, 255, 255` | Card backgrounds, surfaces |
| **Near White** | `#f9fafb` | `249, 250, 251` | Subtle backgrounds |
| **Light Grey** | `#e5e7eb` | `229, 231, 235` | Borders, dividers |
| **Dark Grey** | `#1f2937` | `31, 41, 55` | Code blocks, dark panels |

---

## 🖌️ Typography

### Font Stack

```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
             Oxygen, Ubuntu, Cantarell, sans-serif;
```

### Font Sizes

| Element | Size | Weight | Line Height | Usage |
|---------|------|--------|-------------|-------|
| **H1** | `2.5em` (40px) | `700` | `1.2` | Page titles |
| **H2** | `1.8em` (28.8px) | `600-700` | `1.3` | Section headers |
| **H3** | `1.3em` (20.8px) | `600` | `1.4` | Subsection headers |
| **Body** | `1em` (16px) | `400` | `1.6` | Regular text |
| **Small** | `0.9em` (14.4px) | `400` | `1.5` | Secondary info |
| **Tiny** | `0.75em` (12px) | `400-600` | `1.4` | Labels, captions |

### Font Colors

| Context | Color | Hex Code |
|---------|-------|----------|
| **Primary text** | Slate 900 | `#0f172a` |
| **Secondary text** | Slate 500 | `#64748b` |
| **Disabled text** | Slate 400 | `#94a3b8` |
| **Links** | Blue 600 | `#2563eb` |
| **Link hover** | Blue 700 | `#1d4ed8` |

---

## 🎭 Component Styles

### Cards

```css
background: white;
border-radius: 12px;
padding: 25px;
box-shadow: 0 2px 10px rgba(0,0,0,0.1);
```

**Hover effect**:
```css
transform: translateY(-5px);
box-shadow: 0 15px 50px rgba(0,0,0,0.15);
transition: all 0.3s ease;
```

### Buttons (Primary)

```css
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
color: white;
padding: 12px 24px;
border-radius: 8px;
font-weight: 600;
border: none;
cursor: pointer;
transition: all 0.3s ease;
```

**Hover**:
```css
opacity: 0.9;
transform: translateY(-1px);
box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
```

### Buttons (Secondary)

```css
background: white;
border: 2px solid #e5e7eb;
color: #0f172a;
padding: 12px 24px;
border-radius: 8px;
font-weight: 600;
cursor: pointer;
transition: all 0.2s;
```

**Hover**:
```css
border-color: #2563eb;
background: #f9fafb;
```

### Inputs & Text Areas

```css
width: 100%;
padding: 12px 16px;
border: 2px solid #e5e7eb;
border-radius: 12px;
font-size: 1em;
outline: none;
transition: border-color 0.2s;
```

**Focus**:
```css
border-color: #2563eb;
```

### Badges

**Status badge (good)**:
```css
background: #d1fadf;
color: #05603a;
padding: 4px 10px;
border-radius: 12px;
font-size: 0.75em;
font-weight: 600;
```

**Accent badge**:
```css
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
color: white;
padding: 4px 12px;
border-radius: 12px;
font-size: 0.8em;
font-weight: 600;
```

### Progress Bars

**Container**:
```css
width: 100%;
height: 8px;
background: #e5e7eb;
border-radius: 4px;
overflow: hidden;
```

**Fill**:
```css
height: 100%;
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
transition: width 0.3s ease;
```

---

## 📐 Spacing System

### Padding Scale

| Size | Value | Usage |
|------|-------|-------|
| **XS** | `8px` | Tight spacing |
| **SM** | `12px` | Compact elements |
| **MD** | `16px` | Default spacing |
| **LG** | `20px` | Comfortable spacing |
| **XL** | `24px` | Generous spacing |
| **2XL** | `30px` | Section padding |
| **3XL** | `40px` | Large sections |

### Margin Scale

Same as padding scale. Use consistently for vertical rhythm.

### Gap Scale (Grid/Flex)

| Size | Value | Usage |
|------|-------|-------|
| **SM** | `8px` | Tight grids |
| **MD** | `15px` | Standard grids |
| **LG** | `20px` | Comfortable grids |
| **XL** | `30px` | Spacious layouts |

---

## 🎯 Border Styles

### Border Radius

| Size | Value | Usage |
|------|-------|-------|
| **SM** | `6px` | Small elements |
| **MD** | `8px` | Buttons, inputs |
| **LG** | `12px` | Cards, panels |
| **XL** | `16px` | Large cards |
| **2XL** | `24px` | Rounded elements |
| **Full** | `50%` | Circular elements |

### Border Colors

| Context | Color | Hex Code |
|---------|-------|----------|
| **Default** | Light grey | `#e5e7eb` |
| **Hover** | Blue 600 | `#2563eb` |
| **Focus** | Blue 600 | `#2563eb` |
| **Error** | iOS Red | `#ff3b30` |
| **Success** | iOS Green | `#34c759` |

### Border Width

- **Default**: `1px` (subtle)
- **Emphasis**: `2px` (standard)
- **Strong**: `4px` (accent, left borders)

---

## 🌟 Shadows

### Shadow Scale

```css
/* Subtle */
box-shadow: 0 1px 3px rgba(0,0,0,0.05);

/* Small */
box-shadow: 0 2px 10px rgba(0,0,0,0.08);

/* Medium (default) */
box-shadow: 0 2px 10px rgba(0,0,0,0.1);

/* Large */
box-shadow: 0 10px 40px rgba(0,0,0,0.1);

/* Hover */
box-shadow: 0 15px 50px rgba(0,0,0,0.15);

/* Focus (blue) */
box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.2);

/* Accent glow */
box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
```

---

## 🎬 Animations & Transitions

### Transition Timing

```css
/* Quick (hover states) */
transition: all 0.2s ease;

/* Standard (most animations) */
transition: all 0.3s ease;

/* Slow (complex animations) */
transition: all 0.5s ease;
```

### Common Animations

**Slide in**:
```css
@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
```

**Fade in**:
```css
@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}
```

**Pulse (listening state)**:
```css
@keyframes pulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.1); }
}
```

---

## 📱 Responsive Breakpoints

### Device Sizes

| Breakpoint | Min Width | Max Width | Usage |
|------------|-----------|-----------|-------|
| **Mobile** | - | `639px` | Phones |
| **Tablet** | `640px` | `1023px` | Tablets |
| **Desktop** | `1024px` | `1399px` | Small desktops |
| **Large** | `1400px` | - | Large screens |

### Grid Columns

```css
/* Mobile: 1 column */
grid-template-columns: 1fr;

/* Tablet: 2 columns */
@media (min-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
}

/* Desktop: 3-4 columns */
@media (min-width: 1024px) {
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
}
```

---

## 🎨 Gradient Recipes

### Primary Gradient (Blue)

```css
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
```

**Usage**: Buttons, badges, accent elements

### Text Gradient

```css
background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
-webkit-background-clip: text;
-webkit-text-fill-color: transparent;
background-clip: text;
```

**Usage**: Numbers, headings, accent text

### Border Gradient (Advanced)

```css
position: relative;

&::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 12px;
    padding: 2px;
    background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
    -webkit-mask: linear-gradient(#fff 0 0) content-box,
                  linear-gradient(#fff 0 0);
    -webkit-mask-composite: xor;
    mask-composite: exclude;
}
```

**Usage**: Card borders, stat cards

### Status Gradient (Success → Warning → Error)

```css
background: linear-gradient(90deg, #34c759 0%, #ff9500 50%, #ff3b30 100%);
```

**Usage**: Progress bars, performance budgets

---

## 🎯 Best Practices

### Color Usage Guidelines

1. **Backgrounds**: Always use Slate 100 (`#f1f5f9`) for page backgrounds
2. **Cards**: White (`#ffffff`) with subtle shadows
3. **Text Hierarchy**:
   - Primary: Slate 900 (`#0f172a`)
   - Secondary: Slate 500 (`#64748b`)
   - Disabled: Slate 400 (`#94a3b8`)
4. **Accents**: Blue gradient sparingly (buttons, badges, important elements)
5. **Status**: Use iOS system colors consistently

### Accessibility

- **Contrast Ratio**: All text meets WCAG 2.1 AA (4.5:1 minimum)
- **Focus States**: Always visible with blue outline/border
- **Color Blindness**: Never rely on color alone; use icons/labels
- **Touch Targets**: Minimum 44px × 44px for interactive elements

### Performance

- **Gradients**: Use sparingly; can impact rendering performance
- **Shadows**: Limit multiple shadows on same element
- **Transitions**: Keep under 300ms for perceived responsiveness
- **Animations**: Use `transform` and `opacity` (GPU accelerated)

---

## 🔄 Version History

### Version 2.0 (2025-11-02)
- **Changed**: Color scheme from purple/pink to blues/greys
- **Updated**: All accent colors to professional blue gradient
- **Improved**: Text colors for better readability
- **Refined**: Backgrounds to cooler grey tones

### Version 1.0 (2025-11-02)
- Initial theme with purple/pink gradient (deprecated)
- VisionOS-inspired professional design
- Zero dependencies, inline CSS

---

## 📦 Implementation

### Quick Start

Copy the color variables to use in new demos:

```css
:root {
    /* Backgrounds */
    --bg-page: #f1f5f9;
    --bg-card: #ffffff;
    --bg-subtle: #f9fafb;

    /* Text */
    --text-primary: #0f172a;
    --text-secondary: #64748b;
    --text-disabled: #94a3b8;

    /* Accent */
    --blue-primary: #2563eb;
    --blue-deep: #1e40af;

    /* Borders */
    --border-light: #e5e7eb;
    --border-medium: #cbd5e1;

    /* Status */
    --success: #34c759;
    --warning: #ff9500;
    --error: #ff3b30;
}
```

### Usage Example

```css
body {
    background: var(--bg-page);
    color: var(--text-primary);
}

.card {
    background: var(--bg-card);
    border: 2px solid var(--border-light);
}

.button-primary {
    background: linear-gradient(135deg, var(--blue-primary) 0%, var(--blue-deep) 100%);
}
```

---

## 📚 References

- **Color System**: Tailwind CSS Slate + Blue palettes
- **iOS Colors**: Apple Human Interface Guidelines
- **Typography**: Apple System Font Stack
- **Design Philosophy**: VisionOS design language

---

**Maintained by**: Manoj Jhawar, manoj@ideahq.net
**Project**: AVA AI Demo Suite
**License**: Proprietary (© Augmentalis Inc, Intelligent Devices LLC)

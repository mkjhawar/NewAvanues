# AvaMagicUI Developer Manual

**Version:** 1.0.0
**Updated:** 2025-12-05
**Status:** ACTIVE

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Getting Started](#getting-started)
4. [Variant System](#variant-system)
5. [Component Categories](#component-categories)
6. [Platform Renderers](#platform-renderers)
7. [Creating Custom Components](#creating-custom-components)
8. [Best Practices](#best-practices)
9. [API Reference](#api-reference)
10. [Changelog](#changelog)

---

## Introduction

AvaMagicUI is a cross-platform UI component library built on Kotlin Multiplatform with native renderers for Android (Jetpack Compose), iOS (SwiftUI), Web (React), and Desktop (Electron).

### Key Features

| Feature | Description |
|---------|-------------|
| **205 Components** | Full component library with GetWidget parity |
| **Variant System** | 1000+ style combinations from variant props |
| **Native Rendering** | Platform-specific rendering (not Skia) |
| **Type-Safe** | Kotlin data classes with serialization |
| **Cross-Platform** | Single codebase, 4 platforms |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│              Component Definitions (Kotlin MPP)             │
│                                                             │
│  Core/variants/          components/                        │
│  └── Variant enums       ├── phase1/ (13)                   │
│                          ├── phase3/ (47)                   │
│                          └── flutter-parity/ (145)          │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐    ┌──────────┐    ┌──────────┐
        │ Android  │    │   iOS    │    │   Web    │
        │ Compose  │    │ SwiftUI  │    │  React   │
        │ 205/205  │    │ 205/205  │    │ 205/205  │
        └──────────┘    └──────────┘    └──────────┘
```

### Directory Structure

```
AvaElements/
├── Core/
│   └── src/commonMain/kotlin/com/augmentalis/
│       └── magicelements/core/
│           ├── types/           # Base types (Color, Spacing, etc.)
│           ├── variants/        # Variant enums (NEW)
│           └── api/             # Renderer interface
├── components/
│   ├── phase1/                  # Foundation (13 components)
│   ├── phase3/                  # Extended (47 components)
│   └── flutter-parity/          # Advanced (145 components)
└── Renderers/
    ├── Android/                 # Jetpack Compose
    ├── iOS/                     # SwiftUI mappers
    └── Web/                     # React/TypeScript
```

---

## Getting Started

### Installation

**Gradle (Android/KMP):**
```kotlin
dependencies {
    implementation("com.augmentalis:avaelements-core:2.1.0")
    implementation("com.augmentalis:avaelements-components:2.1.0")

    // Platform-specific renderer
    implementation("com.augmentalis:avaelements-renderer-android:2.1.0")
}
```

**NPM (Web):**
```bash
npm install @augmentalis/avaelements-web
```

### Basic Usage

```kotlin
import com.augmentalis.magicelements.components.phase1.form.Button
import com.augmentalis.magicelements.core.variants.*

val myButton = Button(
    label = "Click Me",
    variant = ButtonVariant.Filled,
    size = ButtonSize.Large,
    color = ColorScheme.Primary
)
```

---

## Variant System

The variant system enables 1000+ style combinations from a single component through combinatorial props.

### Available Variant Enums

| File | Enums | Values |
|------|-------|--------|
| `ButtonVariants.kt` | ButtonVariant | Filled, Outlined, Text, Elevated, Tonal, Pill, Square, Ghost |
| | ButtonSize | XSmall, Small, Medium, Large, XLarge |
| | ButtonShape | Rounded, Square, Pill, Circle |
| `ColorScheme.kt` | ColorScheme | Primary, Secondary, Success, Warning, Danger, Info, Light, Dark, Neutral |
| | ColorIntensity | Light, Normal, Dark |
| `SizeScale.kt` | SizeScale | XS, SM, MD, LG, XL, XXL |
| | ElevationLevel | None, Low, Medium, High, Highest |
| `CardVariants.kt` | CardVariant | Elevated, Filled, Outlined, Ghost |
| | CardShape | Rounded, Square, RoundedLarge |
| `InputVariants.kt` | InputVariant | Outlined, Filled, Underlined, Ghost |
| | InputSize | Small, Medium, Large |
| | InputState | Default, Focused, Error, Success, Disabled |
| `AvatarVariants.kt` | AvatarShape | Circle, Square, Rounded |
| | AvatarSize | XSmall, Small, Medium, Large, XLarge |
| `BadgeVariants.kt` | BadgeVariant | Standard, Dot, Counter |
| | BadgePosition | TopRight, TopLeft, BottomRight, BottomLeft |

### Combination Math

```kotlin
// Button alone: 8 × 5 × 4 × 9 = 1,440 combinations
Button(
    variant = ButtonVariant.Filled,    // 8 options
    size = ButtonSize.Medium,          // 5 options
    shape = ButtonShape.Rounded,       // 4 options
    color = ColorScheme.Primary        // 9 options
)
```

### Usage Examples

```kotlin
// Primary filled button (default)
Button(label = "Submit")

// Outlined danger button
Button(
    label = "Delete",
    variant = ButtonVariant.Outlined,
    color = ColorScheme.Danger
)

// Pill-shaped success button
Button(
    label = "Approved",
    variant = ButtonVariant.Filled,
    shape = ButtonShape.Pill,
    color = ColorScheme.Success
)

// Ghost button with icon
Button(
    label = "Settings",
    variant = ButtonVariant.Ghost,
    icon = "gear"
)
```

---

## Component Categories

### Phase 1 - Foundation (13 components)

| Component | Description | Variants |
|-----------|-------------|----------|
| Button | Clickable action | ButtonVariant, ButtonSize, ButtonShape, ColorScheme |
| TextField | Text input | InputVariant, InputSize, InputState |
| Checkbox | Boolean toggle | ColorScheme |
| Switch | ON/OFF toggle | ColorScheme |
| Text | Text display | - |
| Image | Image display | - |
| Icon | Icon display | SizeScale, ColorScheme |
| Container | Layout wrapper | - |
| Row | Horizontal layout | - |
| Column | Vertical layout | - |
| Card | Card container | CardVariant, CardShape, ElevationLevel |
| ScrollView | Scrollable area | - |
| List | List container | - |

### Phase 3 - Extended (47 components)

**Input (12):** Slider, RangeSlider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete, FileUpload, ImagePicker, Rating, SearchBar

**Display (8):** Badge, Chip, Avatar, Divider, Skeleton, Spinner, ProgressBar, Tooltip

**Layout (8):** Grid, Stack, Spacer, Drawer, Tabs, BottomSheet, StickyHeader, PullToRefresh

**Navigation (5):** AppBar, BottomNav, Breadcrumb, Pagination, FloatingMenu

**Feedback (6):** Alert, Snackbar, Modal, Toast, Confirm, ContextMenu

**Onboarding (2):** IntroScreen, OnboardingStep

**Typography (6):** HeadingText, DisplayText, LabelText, CaptionText, BodyText, BorderDecorator

### Flutter Parity (145 components)

**Layout (10):** Align, Center, ConstrainedBox, Expanded, FittedBox, Flex, Flexible, Padding, SizedBox, Wrap

**Chips (5):** MagicFilter, MagicAction, MagicChoice, MagicInput, MagicTag

**Buttons (14):** FilledButton, CloseButton, ElevatedButton, FAB, IconButton, LoadingButton, OutlinedButton, PopupMenuButton, RefreshIndicator, SegmentedButton, SplitButton, TextButton, ButtonBar, FilledTonalButton

**Lists (4):** ExpansionTile, CheckboxListTile, SwitchListTile, RadioListTile

**Cards (11):** PricingCard, FeatureCard, TestimonialCard, ProductCard, ArticleCard, ImageCard, HoverCard, ExpandableCard, ProductCarousel, FullWidthCarousel, FullSizeCarousel

**Display (12):** AvatarGroup, SkeletonText, SkeletonCircle, ProgressCircle, LoadingOverlay, Popover, ErrorState, NoData, ImageCarousel, LazyImage, ImageGallery, Lightbox

**Feedback (10):** Popup, Callout, Disclosure, InfoPanel, ErrorPanel, WarningPanel, SuccessPanel, FullPageLoading, AnimatedCheck, AnimatedError

**Navigation (9):** Menu, Sidebar, NavLink, ProgressStepper, MenuBar, SubMenu, VerticalTabs, MasonryGrid, AspectRatio

**Data (13):** DataList, DescriptionList, StatGroup, Stat, KPI, MetricCard, Leaderboard, Ranking, Zoom, VirtualScroll, InfiniteScroll, QRCode, RichText

**Input Advanced (11):** PhoneInput, UrlInput, ComboBox, PinInput, OTPInput, MaskInput, RichTextEditor, MarkdownEditor, CodeEditor, FormSection, MultiSelect

**Calendar (5):** Calendar, DateCalendar, MonthCalendar, WeekCalendar, EventCalendar

**Scrolling (7):** ListViewBuilder, GridViewBuilder, ListViewSeparated, PageView, ReorderableListView, CustomScrollView, IndexedStack

**Animation (8):** AnimatedContainer, AnimatedOpacity, AnimatedPositioned, AnimatedDefaultTextStyle, AnimatedPadding, AnimatedSize, AnimatedAlign, AnimatedScale

**Transitions (11):** FadeTransition, SlideTransition, Hero, ScaleTransition, RotationTransition, PositionedTransition, SizeTransition, AnimatedCrossFade, AnimatedSwitcher, DecoratedBoxTransition, AlignTransition

**Slivers (4):** SliverList, SliverGrid, SliverFixedExtentList, SliverAppBar

**Charts (11):** LineChart, BarChart, PieChart, AreaChart, Gauge, Sparkline, RadarChart, ScatterChart, Heatmap, TreeMap, Kanban

**Other (9):** FadeInImage, CircleAvatar, SelectableText, VerticalDivider, EndDrawer, AnimatedList, AnimatedModalBarrier, DefaultTextStyleTransition, RelativePositionedTransition

---

## Platform Renderers

### Android (Jetpack Compose)

```kotlin
// Renderer auto-converts components to Composables
@Composable
fun MyScreen() {
    val component = Button(label = "Hello", variant = ButtonVariant.Elevated)
    AndroidRenderer.render(component)
}
```

### iOS (SwiftUI)

```swift
// Kotlin mapper converts to SwiftUI view hierarchy
struct MyView: View {
    let component = Button(label: "Hello", variant: .elevated)

    var body: some View {
        SwiftUIRenderer.render(component)
    }
}
```

### Web (React)

```tsx
import { Button, ButtonVariant, ColorScheme } from '@augmentalis/avaelements-web';

function MyComponent() {
    return (
        <Button
            label="Hello"
            variant={ButtonVariant.Elevated}
            color={ColorScheme.Primary}
        />
    );
}
```

---

## Creating Custom Components

### Step 1: Define Component (Kotlin)

```kotlin
package com.augmentalis.magicelements.components.custom

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component
import com.augmentalis.magicelements.core.variants.*

@Serializable
data class CustomCard(
    val title: String,
    val subtitle: String? = null,
    val variant: CardVariant = CardVariant.Elevated,
    val color: ColorScheme = ColorScheme.Primary,
    val onTap: String? = null
) : Component
```

### Step 2: Create iOS Mapper

```kotlin
object CustomCardMapper {
    fun map(component: CustomCard): SwiftUIComponent {
        return SwiftUIComponent(
            type = "VStack",
            props = mapOf(
                "alignment" to "leading",
                "spacing" to 8
            ),
            children = listOfNotNull(
                SwiftUIComponent(type = "Text", props = mapOf("content" to component.title, "font" to "headline")),
                component.subtitle?.let {
                    SwiftUIComponent(type = "Text", props = mapOf("content" to it, "font" to "subheadline"))
                }
            ),
            modifiers = listOf(
                SwiftUIModifier("padding", listOf(16)),
                SwiftUIModifier("background", listOf(component.color.name.lowercase())),
                SwiftUIModifier("cornerRadius", listOf(12))
            )
        )
    }
}
```

### Step 3: Create Web Component

```tsx
import React from 'react';
import { CardVariant, ColorScheme } from '../variants';

interface CustomCardProps {
    title: string;
    subtitle?: string;
    variant?: CardVariant;
    color?: ColorScheme;
    onTap?: () => void;
}

export const CustomCard: React.FC<CustomCardProps> = ({
    title,
    subtitle,
    variant = CardVariant.Elevated,
    color = ColorScheme.Primary,
    onTap
}) => (
    <div
        onClick={onTap}
        style={{
            padding: 16,
            borderRadius: 12,
            backgroundColor: getColorValue(color),
            boxShadow: variant === CardVariant.Elevated ? '0 4px 12px rgba(0,0,0,0.1)' : 'none'
        }}
    >
        <h3>{title}</h3>
        {subtitle && <p>{subtitle}</p>}
    </div>
);
```

---

## Best Practices

### DO

| Practice | Example |
|----------|---------|
| Use variant props | `variant = ButtonVariant.Outlined` |
| Use ColorScheme | `color = ColorScheme.Danger` |
| Keep components serializable | `@Serializable data class` |
| Use nullable for optional props | `subtitle: String? = null` |
| Provide sensible defaults | `size = ButtonSize.Medium` |

### DON'T

| Anti-Pattern | Why |
|--------------|-----|
| Hardcode colors | Use ColorScheme enum instead |
| Create duplicate variants | Use existing variant enums |
| Skip serialization | Breaks cross-platform transport |
| Use platform-specific code in definitions | Keep definitions platform-agnostic |

---

## API Reference

### Core Types

```kotlin
// Base component interface
interface Component

// Color type
data class Color(val hex: String, val alpha: Float = 1.0f)

// Spacing type
data class Spacing(val top: Float, val right: Float, val bottom: Float, val left: Float)

// Border type
data class Border(val width: Float, val color: Color, val style: BorderStyle)
```

### Variant Enums

See [Variant System](#variant-system) for complete enum reference.

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 2.1.0 | 2025-12-05 | GetWidget parity: +15 components, +8 variant files, 1000+ combinations |
| 2.0.0 | 2025-12-04 | Full platform parity: 190 components, Web 100% |
| 1.5.0 | 2025-12-02 | iOS 100% parity: 190/190 components |
| 1.0.0 | 2025-12-01 | Initial release |

---

## Backlog / TODO

### Completed

- [x] Full platform parity (Android, iOS, Web, Desktop)
- [x] GetWidget parity (1000+ variant combinations)
- [x] Variant enum system
- [x] BottomSheet, IntroScreen, FloatingMenu
- [x] Typography system (HeadingText, DisplayText, etc.)

### Future Enhancements

| Priority | Feature | Description |
|:--------:|---------|-------------|
| P1 | Theme System | Global theme with dark/light mode |
| P1 | Accessibility | WCAG AA compliance audit |
| P2 | Animation Presets | Pre-built animation configurations |
| P2 | Form Validation | Built-in validation rules |
| P3 | Design Tokens | CSS custom properties export |
| P3 | Figma Plugin | Component export to Figma |

---

**Maintainer:** Engineering Team
**License:** Proprietary

# Ocean Theme Quick Start Guide

**Version:** 1.0.0
**Date:** 2025-11-28
**Theme:** Ocean (Default for MagicUI)

---

## 🌊 What is Ocean Theme?

Ocean is the **default theme for MagicUI**, designed for AR/VR and spatial computing environments. It features:

- **Deep blue gradient** backgrounds inspired by ocean depths
- **Glassmorphic surfaces** with backdrop blur effects
- **High contrast** for accessibility and readability
- **Semantic colors** for status indicators and actions

---

## 🎨 Quick Reference: Ocean Color Palette

### Base Colors

```kotlin
DeepOcean      = #0A1929  // Deep blue-black
OceanDepth     = #0F172A  // Dark ocean blue
OceanMid       = #1E293B  // Medium ocean
OceanShallow   = #334155  // Light ocean blue
```

### Accent Colors

```kotlin
CoralBlue      = #3B82F6  // Primary accent (vibrant blue)
TurquoiseCyan  = #06B6D4  // Secondary accent (cyan)
SeafoamGreen   = #10B981  // Success (green)
SunsetOrange   = #F59E0B  // Warning (orange)
CoralRed       = #EF4444  // Error (red)
```

### Glassmorphic Layers

```kotlin
Surface5  = White @ 5%  (#FFFFFF0D)  // Subtle cards
Surface10 = White @ 10% (#FFFFFF1A)  // Standard cards
Surface15 = White @ 15% (#FFFFFF26)  // Hover states
Surface20 = White @ 20% (#FFFFFF33)  // Active states
Surface30 = White @ 30% (#FFFFFF4D)  // Emphasized

Border10  = White @ 10% (#FFFFFF1A)  // Default borders
Border20  = White @ 20% (#FFFFFF33)  // Emphasized borders
Border30  = White @ 30% (#FFFFFF4D)  // Focus borders

TextPrimary   = White @ 90% (#FFFFFFE6)  // Headers
TextSecondary = White @ 80% (#FFFFFFCC)  // Body text
TextMuted     = White @ 60% (#FFFFFF99)  // Captions
TextDisabled  = White @ 40% (#FFFFFF66)  // Disabled
```

---

## 🚀 Quick Start (Android Compose)

### 1. Basic Usage

```kotlin
import com.augmentalis.magicui.components.*
import com.augmentalis.avanues.avaui.theme.themes.OceanTheme

@Composable
fun MyApp() {
    // Apply Ocean theme
    MagicUITheme(theme = OceanTheme.dark()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ocean background with effects
            OceanBackground(
                showGrid = true,
                showAmbientLights = true
            )

            // Your content
            MyContent()
        }
    }
}
```

### 2. Using Glassmorphic Surfaces

```kotlin
@Composable
fun MyCard() {
    GlassmorphicSurface(
        background = OceanTheme.Surface10,
        border = OceanTheme.Border20,
        cornerRadius = 16.dp,
        blurRadius = 40.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Card Title",
                style = MaterialTheme.typography.headlineMedium,
                color = OceanTheme.TextPrimary
            )
            Text(
                text = "Card content here",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.TextSecondary
            )
        }
    }
}
```

### 3. Status Indicators

```kotlin
@Composable
fun StatusExample() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusIndicator(
            status = StatusIndicator.Status.Success,
            size = 8.dp
        )
        Text(
            text = "Active",
            color = OceanTheme.SeafoamGreen
        )
    }
}
```

---

## 📐 Component Patterns

### Dashboard Layout

```kotlin
@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        OceanBackground()

        LazyColumn(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            item { HeroSection() }

            // Metrics Grid (2x2)
            item { MetricsGrid() }

            // Recent Activity
            item { ActivityList() }
        }
    }
}
```

### Data Table

```kotlin
@Composable
fun DataTable(items: List<Item>) {
    GlassmorphicSurface(
        background = Color.Transparent,
        border = OceanTheme.Border10,
        cornerRadius = 16.dp
    ) {
        Column {
            // Header row
            TableHeader(background = OceanTheme.Surface10)

            // Data rows (alternating backgrounds)
            items.forEachIndexed { index, item ->
                TableRow(
                    item = item,
                    background = if (index % 2 == 0) {
                        OceanTheme.Surface5
                    } else {
                        Color.Transparent
                    }
                )
            }
        }
    }
}
```

### Todo List

```kotlin
@Composable
fun TodoList(tasks: List<Task>) {
    GlassmorphicSurface(
        background = OceanTheme.Surface5,
        border = OceanTheme.Border10,
        cornerRadius = 16.dp
    ) {
        Column {
            tasks.forEach { task ->
                TodoItem(
                    task = task,
                    onToggle = { },
                    statusColor = when (task.status) {
                        TaskStatus.Completed -> OceanTheme.SeafoamGreen
                        TaskStatus.InProgress -> OceanTheme.CoralBlue
                        else -> OceanTheme.TextMuted
                    }
                )
            }
        }
    }
}
```

---

## 🎯 Design Rules

### 1. Spacing Scale

| Token | Value | Usage |
|-------|-------|-------|
| `space-2` | 8dp | Tight spacing |
| `space-4` | 16dp | **Base spacing** (default) |
| `space-6` | 24dp | Section spacing |
| `space-8` | 32dp | Large spacing |

### 2. Border Radius

| Size | Value | Usage |
|------|-------|-------|
| `small` | 8dp | Small cards |
| `medium` | 12dp | Buttons |
| `large` | 16dp | **Default cards** |
| `xlarge` | 24dp | Windows, modals |

### 3. Touch Targets

- **Minimum**: 48dp × 48dp
- **Buttons**: 48dp height
- **List items**: 56dp height
- **Dock icons**: 56dp × 56dp

### 4. Typography Scale

```kotlin
// Headers
headlineLarge  = 32sp  // Page titles
headlineMedium = 28sp  // Section headers
headlineSmall  = 24sp  // Card titles

// Body
bodyLarge  = 16sp  // Primary text
bodyMedium = 14sp  // Secondary text
bodySmall  = 12sp  // Captions

// Labels
labelLarge  = 14sp  // Button labels
labelMedium = 12sp  // Form labels
labelSmall  = 11sp  // Helper text
```

---

## 📱 Responsive Breakpoints

```kotlin
// Mobile: < 600dp
mobile: {
    columns: 1,
    padding: 16.dp,
    fontSize: 14.sp
}

// Tablet: 600-767dp
tablet: {
    columns: 2,
    padding: 24.dp,
    fontSize: 16.sp
}

// Desktop: >= 768dp
desktop: {
    columns: 3-4,
    padding: 32.dp,
    fontSize: 18.sp
}
```

---

## 🔍 Common Patterns

### Hero Section

```kotlin
GlassmorphicSurface(
    background = Color.Transparent,
    border = OceanTheme.Border10
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OceanTheme.AccentGradient)
            .padding(32.dp)
    ) {
        Column {
            Text("Title", style = MaterialTheme.typography.headlineLarge)
            Text("Subtitle", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
```

### Metric Card

```kotlin
GlassmorphicSurface(
    background = OceanTheme.Surface5,
    border = OceanTheme.Border10
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "156",
            style = MaterialTheme.typography.headlineMedium,
            color = OceanTheme.TextPrimary
        )
        Text(
            text = "Team Members",
            style = MaterialTheme.typography.bodySmall,
            color = OceanTheme.TextMuted
        )
    }
}
```

### Activity Item

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    StatusIndicator(
        status = StatusIndicator.Status.Success,
        size = 8.dp
    )
    Text(
        text = "Project completed",
        style = MaterialTheme.typography.bodyMedium,
        color = OceanTheme.TextSecondary
    )
}
```

---

## 📚 Complete Examples

Full working examples are available at:

- **Dashboard**: `android/avanues/core/magicui/examples/OceanThemeExample.kt`
- **Data Table**: `OceanDataTableExample()`
- **Todo List**: `OceanTodoListExample()`

---

## 📖 Documentation

- **Full Design System**: `docs/universal/LD-magicui-design-system.md`
- **Theme Implementation**: `android/avanues/core/magicui/src/main/java/com/augmentalis/magicui/theme/themes/OceanTheme.kt`
- **Components**: `android/avanues/core/magicui/src/main/java/com/augmentalis/magicui/components/OceanComponents.kt`

---

## 🎓 Next Steps

1. **Read** the full Universal Design System: `docs/universal/LD-magicui-design-system.md`
2. **Explore** component templates for tables, lists, workflows, popups
3. **Study** screen layout templates for different app types
4. **Review** platform adaptation guidelines for iOS and Web

---

**Version:** 1.0.0
**Status:** ✅ Production Ready
**Last Updated:** 2025-11-28

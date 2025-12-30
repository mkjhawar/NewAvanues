# Developer Manual - Chapter 64: Ocean Glass Design System

**Version:** 2.1.0
**Date:** 2025-12-03
**Author:** AVA AI Team
**Status:** Verified - Ocean Blue Theme + Adaptive Navigation

---

## Overview

The Ocean Glass Design System provides a glassmorphic UI framework for AVA, built on Jetpack Compose + Material3 with a clear migration path to MagicUI when available.

---

## Verified Implementation

Tested on Android emulator (Pixel 9, API 34) on 2025-12-03.

### Verified Components

| Component | Status | Notes |
|-----------|--------|-------|
| Ocean Blue gradient background | ✓ Verified | Deep ocean (#0A1929 → #1E293B) vertical gradient |
| Glassmorphic message bubbles | ✓ Verified | Frosted glass effect with subtle border |
| Glass input field | ✓ Verified | "Type or say something..." placeholder |
| CoralBlue accent color | ✓ Verified | Header "AI" text, send button, FAB (#3B82F6) |
| Voice FAB | ✓ Verified | CoralBlue microphone, 8dp elevation shadow |
| Adaptive navigation | ✓ Verified | Portrait: 56dp bottom bar, Landscape: 56dp side rail |
| AVA branding | ✓ Verified | Red "AVA" + CoralBlue "AI" header |
| No indicator backgrounds | ✓ Verified | Icon-only navigation, transparent indicators |

### Screen Layout (Verified)

**Portrait Mode:**
```
┌─────────────────────────────────────────────────────────┐
│ [Status Bar]                                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  AVA  AI                                         [RAG]  │  ← Header
│                                                         │
│  ┌─────────────────────────────────────┐               │
│  │ Hello! I'm AVA, your AI assistant  │               │  ← Glass bubble
│  └─────────────────────────────────────┘               │
│                                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Type or say something...                    [>] │   │  ← Glass input
│  └─────────────────────────────────────────────────┘   │
│                                                     🎤  │  ← FAB (floating)
├─────────────────────────────────────────────────────────┤
│   Chat           Teach           Settings               │  ← 56dp nav (icons only)
└─────────────────────────────────────────────────────────┘
```

**Landscape Mode:**
```
┌────┬────────────────────────────────────────────────────┐
│    │ [Status Bar]                                       │
│ C  ├────────────────────────────────────────────────────┤
│ h  │                                                    │
│ a  │  AVA  AI                                    [RAG]  │
│ t  │                                                    │
│    │  ┌─────────────────────────────────────┐          │
├────┤  │ Hello! I'm AVA, your AI assistant  │          │
│ T  │  └─────────────────────────────────────┘          │
│ e  │                                                    │
│ a  │  ┌────────────────────────────────────────────┐   │
│ c  │  │ Type or say something...               [>] │   │
│ h  │  └────────────────────────────────────────────┘   │
├────┤                                               🎤   │
│ ⚙  │                                                    │
└────┴────────────────────────────────────────────────────┘
  ↑
 56dp NavigationRail (side)
```

### Build Verification

| Metric | Result |
|--------|--------|
| Build time | 32 seconds |
| Compiler warnings | 0 |
| Runtime errors | 0 |
| APK size | ~45MB (debug) |

---

## Architecture

### Current Stack

| Layer | Technology |
|-------|------------|
| UI Framework | Jetpack Compose |
| Design System | Material3 |
| Theme | Ocean Glass (custom) |
| Future | MagicUI (cross-platform) |

### File Structure

```
common/core/Theme/src/commonMain/kotlin/com/augmentalis/ava/core/theme/
├── DesignTokens.kt           # Color, spacing, shape, size tokens
├── AvaTheme.kt               # Material3 theme configuration
├── GlassmorphicComponents.kt # Wrapper composables (9 components)
└── OceanThemeExtensions.kt   # Modifier extensions and helpers
```

---

## Design Tokens

### Glass Opacity Levels

| Token | Opacity | Hex | Use Case |
|-------|---------|-----|----------|
| `GlassUltraLight` | 5% | `#0DFFFFFF` | Subtle backgrounds |
| `GlassLight` | 10% | `#1AFFFFFF` | Default surfaces |
| `GlassMedium` | 15% | `#26FFFFFF` | Cards, containers |
| `GlassHeavy` | 20% | `#33FFFFFF` | Elevated surfaces |
| `GlassDense` | 30% | `#4DFFFFFF` | Prominent elements |

### Ocean Theme Colors (MagicUI Compliant)

| Token | Value | Purpose |
|-------|-------|---------|
| `Primary` | `#3B82F6` | CoralBlue (main accent) |
| `Secondary` | `#06B6D4` | TurquoiseCyan |
| `DeepOcean` | `#0A1929` | Darkest background |
| `OceanDepth` | `#0F172A` | Primary background |
| `OceanMid` | `#1E293B` | Mid-level surfaces |
| `OceanShallow` | `#334155` | Elevated surfaces |
| `GradientStart` | `#0A1929` | Deep ocean (top) |
| `GradientMid` | `#0F172A` | Ocean depth (middle) |
| `GradientEnd` | `#1E293B` | Ocean mid (bottom) |
| `Success` | `#10B981` | SeafoamGreen |
| `Warning` | `#F59E0B` | SunsetOrange |
| `Error` | `#EF4444` | CoralRed |
| `Info` | `#3B82F6` | CoralBlue |

### Shape Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `ExtraSmall` | 4dp | Bubble tail corners |
| `Small` | 8dp | Chips, indicators |
| `Medium` | 12dp | Text fields, cards |
| `Large` | 16dp | Bubbles, panels |
| `ExtraLarge` | 24dp | Modals |
| `Full` | 50% | Circular elements |

### Size Tokens

| Token | Value | Purpose |
|-------|-------|---------|
| `MinTouchTarget` | 48dp | WCAG AA compliance |
| `MinTouchTargetSpatial` | 60dp | AR/VR touch target |
| `ChatBubbleMaxWidth` | 320dp | Message bubble limit |
| `CommandBarItemSize` | 48dp | Command bar items |
| `CommandBarIconSize` | 24dp | Icons in command bar |
| `DrawerGridItemSize` | 64dp | Grid items in drawer |
| `DrawerItemGap` | 8dp | Gap between items |
| `NavigationBarHeight` | 56dp | Compact bottom nav (portrait) |
| `NavigationRailWidth` | 56dp | Compact side rail (landscape) |
| `NavigationIconSize` | 24dp | Nav icons (no containers) |
| `FABSize` | 56dp | Voice FAB size |
| `FABElevation` | 8dp | FAB shadow depth |

### AR/VR Spatial Tokens

| Token | Value | Purpose |
|-------|-------|---------|
| `HudDistance` | 0.5m | HUD, alerts |
| `InteractiveDistance` | 1.0m | Active window |
| `PrimaryDistance` | 1.5m | Main content |
| `SecondaryDistance` | 2.0m | Supporting windows |
| `AmbientDistance` | 3.0m | Background content |
| `MaxVoiceOptions` | 3 | Per voice prompt |
| `CommandHierarchyLevels` | 2 | Flat for voice |

---

## Glassmorphic Components

### Component Mapping (Compose → MagicUI)

| Current Component | Future MagicUI | Description |
|-------------------|----------------|-------------|
| `GlassSurface` | `MagicUI.Surface` | Base glass container |
| `GlassCard` | `MagicUI.Card` | Elevated card with glass |
| `GlassBubble` | `MagicUI.ChatBubble` | Chat message bubble |
| `OceanButton` | `MagicUI.Button` | Themed button variants |
| `GlassTextField` | `MagicUI.TextField` | Glass input field |
| `GlassChip` | `MagicUI.Chip` | Tag/badge component |
| `GlassIndicator` | `MagicUI.Indicator` | Status bar |
| `OceanGradientBackground` | `MagicUI.GradientBackground` | Purple gradient |

### GlassSurface

```kotlin
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.LIGHT,
    shape: Shape = RoundedCornerShape(ShapeTokens.Medium),
    showBorder: Boolean = true,
    borderColor: Color = ColorTokens.Outline,
    content: @Composable BoxScope.() -> Unit
)
```

**Parameters:**
- `intensity`: Glass opacity level (ULTRA_LIGHT to DENSE)
- `shape`: Corner shape
- `showBorder`: Whether to show 1dp border
- `borderColor`: Border color (default: 20% white)

### GlassBubble

```kotlin
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    maxWidth: Dp = SizeTokens.ChatBubbleMaxWidth,
    content: @Composable BoxScope.() -> Unit
)
```

**Styling:**
- User bubbles: Primary color @ 90% opacity
- AVA bubbles: GlassMedium (15% white) + border
- Asymmetric corners (tail on sender side)

### OceanButton

```kotlin
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OceanButtonStyle = OceanButtonStyle.PRIMARY,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
)

enum class OceanButtonStyle {
    PRIMARY,    // Teal accent, high emphasis
    SECONDARY,  // Glass background, medium emphasis
    TERTIARY,   // Text only, low emphasis
    ERROR       // Red accent, destructive actions
}
```

---

## Modifier Extensions

### Available Modifiers

```kotlin
// Glass surface with configurable intensity
Modifier.glassSurface(
    intensity: GlassIntensity = GlassIntensity.LIGHT,
    shape: Shape = RoundedCornerShape(ShapeTokens.Medium),
    showBorder: Boolean = true
)

// Glass card with preset styling
Modifier.glassCard(
    intensity: GlassIntensity = GlassIntensity.MEDIUM,
    cornerRadius: Dp = ShapeTokens.Large
)

// Chat bubble styling
Modifier.glassBubble(isUser: Boolean)

// Ocean gradient backgrounds
Modifier.oceanGradient()           // Vertical
Modifier.oceanGradientHorizontal() // Horizontal
```

### Helper Objects

```kotlin
// Pre-configured colors
OceanGlass.light        // 10% white
OceanGlass.medium       // 15% white
OceanGlass.successGlass // Success @ 15% opacity

// Gradient brushes
OceanGradients.vertical
OceanGradients.horizontal
OceanGradients.radial

// Shape presets
OceanShapes.small
OceanShapes.bubble(isUser = true)

// Default values
GlassDefaults.minTouchTarget    // 48dp
GlassDefaults.bubbleMaxWidth    // 280dp
GlassDefaults.transitionDuration // 300ms
```

---

## Overlay UI Design

### Voice Orb Concept

Based on research into ambient computing and minimal AI interfaces:

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│           [User's Current App Content]              │
│                                                     │
│                                 ┌─────────────────┐ │
│                                 │  AVA Response   │ │
│                                 │  Glassmorphic   │ │
│                                 │  bubble         │ │
│                                 └─────────────────┘ │
│                                                     │
│   ┌──────┐ ┌──────┐ ┌──────┐               ◉      │
│   │ Copy │ │Share │ │ More │          [Voice Orb] │
│   └──────┘ └──────┘ └──────┘                      │
├─────────────────────────────────────────────────────┤
│  🎙 "Turn on living room lights"    ⬤ Listening... │
└─────────────────────────────────────────────────────┘
```

### Overlay States

| State | Visual | Coverage |
|-------|--------|----------|
| Idle | Small orb (48dp), 30% opacity | <1% |
| Listening | Expanded orb (64dp), context strip | ~8% |
| Processing | Aurora gradient ring | ~8% |
| Responding | Glass bubble appears | ~15% |
| Expanded | Full panel (rare) | ~40% |

### Glassmorphism Parameters

| Element | Background | Blur | Border |
|---------|------------|------|--------|
| Orb (idle) | GlassUltraLight | 4dp | 1dp @ 10% |
| Orb (active) | GlassLight | 8dp | 1dp @ 20% |
| Response bubble | GlassMedium | 12dp | 1dp @ 15% |
| Full panel | GlassHeavy | 16dp | 1dp @ 20% |

---

## Accessibility

### WCAG AA Compliance

| Requirement | Implementation |
|-------------|----------------|
| Touch targets | 48dp minimum (SizeTokens.MinTouchTarget) |
| Color contrast | 4.5:1 ratio maintained |
| Text on glass | TextPrimary on all glass surfaces |
| Motion | Reduced motion support via system settings |
| Screen readers | contentDescription on all interactive elements |

### Cognitive Load Reduction

Based on Mayo Clinic research showing 46.6% reduction in cognitive load:

- Maximum 3 action chips visible at once
- Auto-dismiss responses after 5 seconds
- No nested menus in overlay mode
- Large body font (16sp) for responses

---

## Implementation Examples

### Chat Message

```kotlin
MessageBubble(
    content = message.content,
    isUserMessage = message.role == MessageRole.USER,
    timestamp = message.timestamp,
    confidence = message.confidence,
    // Uses GlassBubble internally with Ocean theme
)
```

### RAG Indicator

```kotlin
GlassIndicator(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    intensity = GlassIntensity.MEDIUM
) {
    Icon(Icons.Filled.AutoAwesome, tint = ColorTokens.Primary)
    Column {
        Text("Searching Your Documents", color = ColorTokens.TextPrimary)
        Text("3 documents selected", color = ColorTokens.TextSecondary)
    }
}
```

### Empty State Card

```kotlin
GlassCard(
    modifier = Modifier.padding(32.dp),
    intensity = GlassIntensity.MEDIUM
) {
    Icon(Icons.Filled.Chat, tint = ColorTokens.Primary)
    Text("Start a conversation", color = ColorTokens.TextPrimary)
}
```

---

## Adaptive Navigation

### Overview

AVA uses Material3 adaptive navigation for optimal space utilization:

| Orientation | Component | Size | Position |
|-------------|-----------|------|----------|
| Portrait | `NavigationBar` | 56dp height | Bottom |
| Landscape | `NavigationRail` | 56dp width | Left side |

### Navigation Styling

| Property | Value | Notes |
|----------|-------|-------|
| Container color | `OceanDepth.copy(0.95f)` | Semi-transparent |
| Content color | `Color.White` | High contrast |
| Icon size | 24dp | No background containers |
| Indicator color | `Transparent` | Icon-only selection |
| Selected color | `CoralBlue (#3B82F6)` | Accent highlight |
| Unselected | `Color.White @ 70%` | Subtle |

### Portrait: Bottom Navigation

```kotlin
val CoralBlue = Color(0xFF3B82F6)
val OceanDepth = Color(0xFF0F172A)

NavigationBar(
    containerColor = OceanDepth.copy(alpha = 0.95f),
    contentColor = Color.White,
    tonalElevation = 0.dp,
    modifier = Modifier.height(56.dp)  // Compact height
) {
    NavigationBarItem(
        icon = {
            Icon(
                Icons.Default.Chat,
                contentDescription = "Chat",
                modifier = Modifier.size(24.dp)
            )
        },
        label = { Text("Chat", style = MaterialTheme.typography.labelSmall) },
        selected = currentRoute == "chat",
        onClick = { navigateTo("chat") },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CoralBlue,
            selectedTextColor = CoralBlue,
            unselectedIconColor = Color.White.copy(alpha = 0.7f),
            unselectedTextColor = Color.White.copy(alpha = 0.7f),
            indicatorColor = Color.Transparent  // No background shape
        )
    )
    // Teach, Settings items...
}
```

### Landscape: Navigation Rail

```kotlin
NavigationRail(
    containerColor = OceanDepth.copy(alpha = 0.95f),
    contentColor = Color.White,
    modifier = Modifier.width(56.dp)  // Compact width
) {
    Spacer(Modifier.height(8.dp))

    NavigationRailItem(
        icon = {
            Icon(
                Icons.Default.Chat,
                contentDescription = "Chat",
                modifier = Modifier.size(24.dp)
            )
        },
        label = { Text("Chat", style = MaterialTheme.typography.labelSmall) },
        selected = currentRoute == "chat",
        onClick = { navigateTo("chat") },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = CoralBlue,
            selectedTextColor = CoralBlue,
            unselectedIconColor = Color.White.copy(alpha = 0.7f),
            unselectedTextColor = Color.White.copy(alpha = 0.7f),
            indicatorColor = Color.Transparent
        )
    )
    // Teach, Settings items...
}
```

### Voice FAB (Elevated)

```kotlin
val CoralBlue = Color(0xFF3B82F6)
val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

// Responsive positioning
val fabBottomPadding = if (isLandscape) 24.dp else 72.dp
val fabEndPadding = if (isLandscape) 24.dp else 16.dp

FloatingActionButton(
    onClick = onVoiceClick,
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = fabBottomPadding, end = fabEndPadding)
        .size(56.dp),
    shape = CircleShape,
    containerColor = CoralBlue,
    contentColor = Color.White,
    elevation = FloatingActionButtonDefaults.elevation(
        defaultElevation = 8.dp,      // Visible shadow
        pressedElevation = 12.dp,     // Elevated when pressed
        focusedElevation = 10.dp,
        hoveredElevation = 10.dp
    )
) {
    Icon(
        Icons.Default.Mic,
        contentDescription = "Voice input",
        tint = Color.White,
        modifier = Modifier.size(24.dp)
    )
}
```

### Adaptive Layout Pattern

```kotlin
@Composable
fun AvaApp() {
    val isLandscape = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail { /* items */ }
            NavHost(modifier = Modifier.weight(1f)) { /* routes */ }
        }
    } else {
        Scaffold(
            bottomBar = { NavigationBar { /* items */ } }
        ) { innerPadding ->
            NavHost(modifier = Modifier.padding(innerPadding)) { /* routes */ }
        }
    }
}
```

---

### App Header

```kotlin
Row(
    modifier = Modifier.padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "AVA",
        color = ColorTokens.Error,  // Coral Red
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = "AI",
        color = ColorTokens.Primary,  // CoralBlue (#3B82F6)
        style = MaterialTheme.typography.headlineMedium
    )
}
```

---

## Migration Guide

### When MagicUI is Available

1. **Update imports:**
```kotlin
// Before
import com.augmentalis.ava.core.theme.GlassSurface

// After
import com.magicui.Surface as GlassSurface
```

2. **Component mapping is 1:1** - no API changes needed

3. **Tokens remain the same** - ColorTokens, ShapeTokens, etc. continue to work

4. **Test on all platforms** - MagicUI supports Android, iOS, Web

---

## Research Sources

### Glassmorphism Design

- [Glassmorphism Best Practices - Nielsen Norman Group](https://www.nngroup.com/articles/glassmorphism/)
- [12 Glassmorphism UI Features, Best Practices - UXPilot](https://uxpilot.ai/blogs/glassmorphism-ui)
- [How Glassmorphism and Voice UI are Redefining Website UX - SitePoint](https://www.sitepoint.com/glassmorphism-and-voice-ui/)
- [What is Glassmorphism? UI Design Trend 2025 - Design Studio](https://www.designstudiouiux.com/blog/what-is-glassmorphism-ui-trend/)

### AI Interface Design

- [Understanding the AI Minimalist Design Trend - Built In](https://builtin.com/artificial-intelligence/ai-minimalist-design)
- [Comparing Conversational AI Tool User Interfaces 2025 - IntuitionLabs](https://intuitionlabs.ai/articles/conversational-ai-ui-comparison-2025)

### Cognitive Load & Ambient Computing

- [Impact of Ambient AI on Cognitive Load - Mayo Clinic](https://www.mcpdigitalhealth.org/article/S2949-7612(24)00126-3/fulltext)
- [Why Ambient Computing Requires a Rethink of UX - Medium](https://medium.com/@marketingtd64/why-ambient-computing-requires-a-rethink-of-ux-aacd06d39c2c)
- [Ambient Agents: Context-Aware AI - DigitalOcean](https://www.digitalocean.com/community/tutorials/ambient-agents-context-aware-ai)

### Material Design

- [Material Design 3 Guidelines](https://m3.material.io/)
- [Jetpack Compose Material3 Documentation](https://developer.android.com/jetpack/compose/designsystems/material3)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 2.1.0 | 2025-12-03 | **Adaptive Navigation**: Portrait uses 56dp bottom NavigationBar, landscape uses 56dp side NavigationRail. No indicator backgrounds (transparent). FAB elevated with 8dp shadow at different z-level. Responsive FAB positioning. |
| 2.0.0 | 2025-12-03 | **Ocean Blue Theme**: Changed from purple gradient to MagicUI-compliant Ocean Blue (#0A1929 → #1E293B), Primary changed to CoralBlue (#3B82F6), added AR/VR spatial tokens, compact command drawer (64dp items with 8dp gaps) |
| 1.1.0 | 2025-12-03 | Added verified implementation section, bottom nav, Voice FAB, app header examples |
| 1.0.0 | 2025-12-03 | Initial Ocean Glass design system |

---

**Next Chapter:** [Chapter 65 - TBD]
**Previous Chapter:** [Chapter 63 - Embedding Generator](Developer-Manual-Chapter63-Embedding-Generator.md)

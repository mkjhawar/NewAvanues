---
name: create-ui
description: UI creation workflow with production-grade frontend design. Use when creating screens, interfaces, layouts, or visual components. Emphasizes distinctive, non-generic aesthetics.
---

# UI Creation

## Trigger Words

| Intent | Examples |
|--------|----------|
| UI | "UI", "interface", "screen" |
| Create | "create screen", "add page", "build view" |
| Design | "layout", "component", "form", "dashboard" |

## Platform Detection

| Platform | Framework | Style |
|----------|-----------|-------|
| Android | Jetpack Compose | Material 3 |
| iOS | SwiftUI | Human Interface |
| Web | React + Tailwind | Modern CSS |
| Desktop | Tauri + React | Platform native |

## Design-First Approach

**Before coding:**
1. Understand purpose and audience
2. Choose bold aesthetic direction
3. Consider technical constraints
4. Design with intention, not defaults

## Workflow

```
1. Requirements → What does user need
2. Research     → Existing patterns, components
3. Design       → ASCII wireframe + aesthetic
4. Approve      → User confirms design
5. Implement    → Generate production code
6. Demo         → HTML preview (web)
7. Verify       → User confirms result
```

## Design Principles

### Typography
| Do | Don't |
|----|-------|
| Characterful fonts (Space Grotesk, Clash Display) | Generic (Arial, default system) |
| Strong hierarchy (clear size contrast) | Same-size everything |
| Intentional weight variations | Random bold |

### Color
| Do | Don't |
|----|-------|
| Cohesive palette with CSS variables | Random hex values |
| Dominant color + sharp accents | Muddy, unsaturated |
| Consider dark/light variants | Single mode only |

### Motion
| Do | Don't |
|----|-------|
| Orchestrated page loads | Scattered micro-animations |
| Staggered reveals | Everything at once |
| Purposeful transitions | Motion for motion's sake |

### Layout
| Do | Don't |
|----|-------|
| Asymmetry, overlap, diagonal flow | Predictable grids |
| Grid-breaking focal points | Everything aligned |
| Intentional whitespace | Cramped or uniform |

## ASCII Preview

```
┌─────────────────────────────┐
│ ← Settings                  │
├─────────────────────────────┤
│                             │
│ Profile                     │
│ ┌─────────────────────────┐ │
│ │ [Avatar]  Name          │ │
│ │           email@ex.com  │ │
│ └─────────────────────────┘ │
│                             │
│ Preferences                 │
│ ┌─────────────────────────┐ │
│ │ Dark Mode         [ON]  │ │
│ │ Notifications     [OFF] │ │
│ └─────────────────────────┘ │
│                             │
│ [ Logout ]                  │
│                             │
└─────────────────────────────┘
```

## Avoid "AI Slop"

| Anti-Pattern | Fix |
|--------------|-----|
| Excessive centered layouts | Use asymmetry |
| Purple gradients everywhere | Pick distinctive palette |
| Uniform rounded corners | Vary border-radius |
| Inter font for everything | Choose characterful fonts |
| Generic card layouts | Design with intention |

## Implementation Standards

| Aspect | Requirement |
|--------|-------------|
| Production-grade | Functional, not prototype |
| Visually striking | Distinctive, memorable |
| Meticulously refined | Pixel-perfect details |
| Complexity matches vision | Maximalist = elaborate code |

## Platform-Specific

### Web (React + Tailwind)
```jsx
// Production component structure
export const SettingsScreen: FC = () => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800"
    >
      {/* Staggered content reveal */}
    </motion.div>
  );
};
```

### Android (Compose)
```kotlin
@Composable
fun SettingsScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Material 3 components
    }
}
```

### iOS (SwiftUI)
```swift
struct SettingsView: View {
    var body: some View {
        NavigationStack {
            // Human Interface Guidelines
        }
    }
}
```

## Modifiers

| Modifier | Effect |
|----------|--------|
| .native | Platform native (default) |
| .magicui | Cross-platform MagicUI |
| .app | Full screen |
| .module | Component only |
| .update | Modify existing |
| .dark | Dark mode variant |
| .minimal | Minimalist aesthetic |
| .bold | Maximalist, striking |

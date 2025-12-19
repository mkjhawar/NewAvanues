# Android Documentation

Platform-specific documentation for MagicUI implementation on Android with Jetpack Compose.

## 📚 Contents

### Implementation Guides

- **[magicui-compose-implementation.md](./magicui-compose-implementation.md)** - Complete Compose Implementation Guide
  - Ocean Theme setup for Compose
  - Component library (Tables, Todo Lists, Modals, Toasts)
  - Layout patterns (Dashboard, List, Detail)
  - Material Design 3 integration
  - Performance optimization
  - Accessibility guidelines
  - Android-specific best practices

## 🎨 Ocean Theme for Compose

```kotlin
import com.augmentalis.magicui.theme.MagicUITheme
import com.augmentalis.avanues.avaui.theme.themes.OceanTheme

setContent {
    MagicUITheme(theme = OceanTheme.dark()) {
        YourApp()
    }
}
```

## 🔗 Related Documentation

- **Universal Design System**: `/docs/universal/LD-magicui-design-system.md`
- **Ocean Theme Quick Start**: `/docs/universal/magicui-ocean-theme-quick-start.md`
- **Compose Examples**: `/android/avanues/core/magicui/examples/`

## 📦 Key Components

### MagicUI Compose Components

Located in: `android/avanues/core/magicui/src/main/java/com/augmentalis/magicui/`

- `OceanTheme.kt` - Ocean color palette and theme
- `OceanComponents.kt` - Background, surfaces, status indicators
- `MagicButton.kt` - Voice-enabled buttons
- `MagicCard.kt` - Glassmorphic cards
- `MagicTextField.kt` - Voice-enabled input fields

## 🚀 Quick Start

1. Add dependency: `implementation("com.augmentalis.avanues.core:magicui:1.0.0")`
2. Apply Ocean theme
3. Use glassmorphic components
4. Follow accessibility guidelines

---

**Platform:** Android (Jetpack Compose)
**Min SDK:** 24 (Android 7.0)
**Target SDK:** 34 (Android 14+)
**Last Updated:** 2025-11-28

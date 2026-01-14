# Universal Documentation

Platform-agnostic documentation for MagicUI and AVAMagic components.

## 📚 Contents

### Design System

- **[LD-magicui-design-system.md](./LD-magicui-design-system.md)** - Living Document: Universal Design System
  - Complete design principles and guidelines
  - Component templates (tables, lists, workflows, windows, popups, notifications)
  - Screen layout templates by app type
  - Platform adaptation guidelines (Android, iOS, Web)
  - Ocean theme specification
  - Research-based design from VisionOS, Fluent, Glassmorphism trends

- **[magicui-ocean-theme-quick-start.md](./magicui-ocean-theme-quick-start.md)** - Quick Reference
  - Ocean color palette cheat sheet
  - Common component patterns
  - Copy-paste code examples
  - Responsive breakpoints

## 🎨 Default Theme

**Ocean Theme** is the default theme for MagicUI, optimized for AR/VR and spatial computing environments.

### Quick Reference

```kotlin
// Apply Ocean theme
MagicUITheme(theme = OceanTheme.dark()) {
    OceanBackground()
    // Your content
}
```

## 📖 Related Documentation

- **Android Implementation**: `android/avanues/core/magicui/`
- **iOS Implementation**: `Universal/Libraries/AvaElements/Renderers/iOS/`
- **Web Implementation**: `modules/AVAMagic/Renderers/WebRenderer/`

## 📝 Naming Conventions

All files follow IDEACODE conventions:
- `lowercase-kebab-case.md` for standard docs
- `LD-*` prefix for Living Documents (evolving specs)
- `README.md` for directory guides (exception: UPPERCASE)

---

**Last Updated:** 2025-11-28
**Status:** ✅ Production Ready

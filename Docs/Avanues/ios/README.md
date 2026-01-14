# iOS Documentation

Platform-specific documentation for MagicUI implementation on iOS with SwiftUI.

## 📚 Contents

### Implementation Guides

- **[magicui-swiftui-implementation.md](./magicui-swiftui-implementation.md)** - Complete SwiftUI Implementation Guide
  - Ocean Theme setup for SwiftUI
  - Component library (Tables, Todo Lists, Modals, Toasts)
  - Layout patterns (Dashboard, List, Detail)
  - VisionOS spatial computing integration
  - Performance optimization
  - Accessibility with VoiceOver
  - iOS-specific best practices

## 🎨 Ocean Theme for SwiftUI

```swift
import SwiftUI
import MagicUI

struct YourApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
```

## 🔗 Related Documentation

- **Universal Design System**: `/docs/universal/LD-magicui-design-system.md`
- **Ocean Theme Quick Start**: `/docs/universal/magicui-ocean-theme-quick-start.md`
- **SwiftUI Examples**: `/Universal/Libraries/AvaElements/Renderers/iOS/Examples/`

## 📦 Key Components

### MagicUI SwiftUI Components

Located in: `Universal/Libraries/AvaElements/Renderers/iOS/`

- `OceanTheme.swift` - Ocean color palette and gradients
- `OceanBackground.swift` - Background with grid and ambient lights
- `GlassmorphicSurface.swift` - Native blur materials
- `DataTable.swift` - iOS-style data tables
- `TodoList.swift` - Task management UI

## 🚀 Quick Start

1. Add MagicUI Swift package
2. Import MagicUI framework
3. Apply Ocean color scheme
4. Use native blur materials
5. Follow iOS Human Interface Guidelines

## 🔮 VisionOS Support

MagicUI fully supports visionOS spatial computing:
- Volumetric windows
- Ornaments for floating controls
- Spatial depth hierarchy
- Native visionOS materials

---

**Platform:** iOS (SwiftUI)
**Min Version:** iOS 15.0+
**Target:** iOS 17.0+
**VisionOS:** Supported
**Last Updated:** 2025-11-28

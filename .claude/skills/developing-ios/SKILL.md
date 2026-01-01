---
name: developing-ios
description: Develops iOS apps with Swift and SwiftUI. Use for iOS UI, UIKit interop, Core Data, Combine, async/await, permissions, notifications, and iOS lifecycle management.
---

# iOS Development

## Tech Stack

| Component | Tech | Min Version |
|-----------|------|-------------|
| Language | Swift | 5.9+ |
| UI | SwiftUI | iOS 17+ |
| Legacy UI | UIKit | When needed |
| Data | Core Data / SwiftData | Latest |
| Async | async/await + Combine | Native |
| Network | URLSession | Native |

## Structure

```
App/
├── App.swift           # @main entry
├── Models/             # Data models
├── Views/              # SwiftUI views
├── ViewModels/         # ObservableObject
├── Services/           # API, persistence
└── Extensions/         # Swift extensions
```

## Patterns

| Pattern | Implementation |
|---------|----------------|
| State | `@State`, `@StateObject`, `@ObservedObject` |
| Binding | `@Binding`, `@Environment` |
| Navigation | NavigationStack + NavigationLink |
| DI | Environment values or manual injection |

## SwiftUI Rules

| Rule | Example |
|------|---------|
| View composition | Small, reusable views |
| State ownership | Parent owns, child binds |
| Previews | `#Preview { View() }` |
| Modifiers | Order matters |

## Quality Gates

| Gate | Target |
|------|--------|
| Min iOS | 17.0 |
| Test coverage | 90%+ |
| Accessibility | VoiceOver support |
| Dark mode | Required |

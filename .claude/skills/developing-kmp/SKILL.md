---
name: developing-kmp
description: Develops Kotlin Multiplatform apps sharing code across Android, iOS, Desktop, Web. Use for shared business logic, expect/actual declarations, platform-specific implementations, and KMP project structure.
---

# Kotlin Multiplatform Development

## Tech Stack

| Component | Tech | Version |
|-----------|------|---------|
| Language | Kotlin | 2.0+ |
| Build | Gradle + KMP plugin | Latest |
| Shared UI | Compose Multiplatform | 1.6+ |
| Async | Coroutines | 1.8+ |
| Serialization | kotlinx.serialization | Latest |
| HTTP | Ktor Client | 2.3+ |

## Structure

```
project/
├── shared/
│   ├── commonMain/      # Shared code
│   ├── androidMain/     # Android actual
│   ├── iosMain/         # iOS actual
│   └── desktopMain/     # Desktop actual
├── androidApp/          # Android app
├── iosApp/              # iOS app (Xcode)
└── desktopApp/          # Desktop app
```

## Patterns

| Pattern | Implementation |
|---------|----------------|
| Expect/Actual | `expect fun` in common, `actual fun` per platform |
| DI | Koin multiplatform |
| State | StateFlow in shared, collect per platform |
| Platform API | Interface in common, impl per platform |

## Key Rules

| Rule | Requirement |
|------|-------------|
| Common first | Maximize shared code |
| No platform leaks | Abstract platform APIs |
| Gradle config | Use convention plugins |
| iOS interop | `@ObjCName` for Swift |

## Quality Gates

| Gate | Target |
|------|--------|
| Shared code | 70%+ |
| Platform code | <30% |
| Test in common | Yes |

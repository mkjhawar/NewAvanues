---
name: developing-android
description: Develops Android apps with Kotlin and Jetpack Compose. Use for Android UI, Material Design 3, Room, Hilt, permissions, notifications, services, Coroutines, and Android lifecycle management.
---

# Android Development

## Tech Stack

| Component | Tech | Min Version |
|-----------|------|-------------|
| Language | Kotlin | 2.0+ |
| UI | Jetpack Compose | 1.7+ |
| Design | Material 3 | Latest |
| DI | Hilt | 2.50+ |
| DB | Room | 2.6+ |
| Async | Coroutines + Flow | 1.8+ |
| Network | Retrofit + OkHttp | 2.9+ |

## Structure

```
app/src/main/kotlin/com/app/
├── di/          # Hilt modules
├── data/        # Repository, API, DB
├── domain/      # Models, UseCases
├── ui/          # Screens, ViewModels, Components
└── util/        # Extensions
```

## Patterns

| Pattern | Implementation |
|---------|----------------|
| State | `StateFlow` + `collectAsStateWithLifecycle()` |
| Events | Sealed class actions |
| Navigation | Compose Navigation |
| DI | Hilt `@Inject`, `@HiltViewModel` |

## Compose Rules

| Rule | Example |
|------|---------|
| State hoisting | `fun Screen(state: S, onAction: (A) -> Unit)` |
| Stable types | Use `@Immutable`, `ImmutableList` |
| Keys | `items(list, key = { it.id })` |
| Modifiers | Layout → Drawing → Interaction |

## Quality Gates

| Gate | Target |
|------|--------|
| Min SDK | API 24 |
| Test coverage | 90%+ |
| Accessibility | Labels on all controls |
| Themes | Dark + Light |

# WebAvanue Universal - Module Instructions

Parent: `Modules/WebAvanue/.claude/CLAUDE.md`

---

## SCOPE

Shared UI and presentation layer for WebAvanue browser. KMP module targeting Android, iOS, Desktop.

---

## RESPONSIBILITIES

| Component | Purpose |
|-----------|---------|
| Screens | Compose Multiplatform UI |
| ViewModels | Presentation logic |
| UI Components | Reusable composables |
| Navigation | Screen navigation |

---

## RULES

| Rule | Requirement |
|------|-------------|
| UI Framework | Compose Multiplatform |
| State management | StateFlow + ViewModel |
| SOLID | Max 500 lines per file |
| Testing | 90%+ coverage for ViewModels |

---

## ARCHITECTURE

```
presentation/
├── ui/
│   ├── browser/     # Browser screens
│   ├── tab/         # Tab management
│   └── components/  # Shared composables
├── viewmodel/       # ViewModels
└── navigation/      # Navigation logic
```

---

## DEPENDENCIES

- Compose Multiplatform
- Voyager (navigation)
- Koin (DI)
- coredata module

---

## FILE NAMING

| Type | Pattern |
|------|---------|
| Screens | `{Name}Screen.kt` |
| ViewModels | `{Name}ViewModel.kt` |
| Components | `{Name}View.kt` or `{Name}Bar.kt` |
| State | `{Name}State.kt` |

---

Updated: 2025-12-17

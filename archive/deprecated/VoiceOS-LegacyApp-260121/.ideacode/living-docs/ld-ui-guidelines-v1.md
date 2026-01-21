# UI Guidelines - v1

**Enforced by:** Global CLAUDE.md > UI Framework

---

## Decision Tree

| Question | Yes | No |
|----------|-----|-----|
| Cross-platform needed? | MagicUI | Check next |
| Platform-specific API? | Native | Check next |
| MagicUI component exists? | MagicUI | Native (document for future) |

---

## UI Creation Pipeline (MANDATORY)

| Step | Interactive | YOLO |
|------|-------------|------|
| 1. Requirements | Q&A if unclear | Infer from context |
| 2. Research | Auto-search best practices | Auto |
| 3. Design | Generate hierarchy | Generate hierarchy |
| 4. ASCII Preview | Display, await approval | Skip |
| 5. Generate Code | After approval | Immediate |
| 6. HTML Demo | Ask user | Skip |
| 7. Verify | Check accessibility | Check accessibility |

---

## Auto-Research (Always Run)

| UI Type | Research Topics |
|---------|-----------------|
| Settings | Platform patterns (Material3/HIG), grouping, toggles |
| Dashboard | Card layouts, metrics, data viz |
| List/Feed | Infinite scroll, pull-refresh, swipe |
| Form | Validation UX, error placement |
| Onboarding | Steps, skip, permissions |
| Auth | Login flows, social, biometrics |

Research meshes with MagicUI/Ocean guidelines automatically.

---

## ASCII Preview (Required Unless YOLO)

```
┌────────────────────────────────┐
│ [<] Screen Title           [?] │
├────────────────────────────────┤
│  ┌─ Section ───────────────┐   │
│  │ Label            [>]    │   │
│  │ Toggle        [====○]   │   │
│  └─────────────────────────┘   │
│  [ Action Button ]             │
└────────────────────────────────┘

[<] back  [>] nav  [====○] on  [○====] off
```

---

## HTML Demo Options

| Level | Features |
|-------|----------|
| Basic | Static HTML/CSS, Ocean theme |
| Interactive | JS clicks, toggles, navigation |
| Advanced | KMP bridge, functional testing |

---

## Always Create UI For

| Screen | Priority |
|--------|----------|
| User Settings | HIGH |
| Developer Settings | HIGH |
| Onboarding | HIGH |
| Dashboard | HIGH |
| Profile | MEDIUM |

---

## Component Mapping

| Need | MagicUI | Compose | SwiftUI |
|------|---------|---------|---------|
| Card | GlassmorphicSurface | Card | GroupBox |
| List | DataTable | LazyColumn | List |
| Toggle | MagicSwitch | Switch | Toggle |
| Dialog | OceanDialog | AlertDialog | .alert |
| Toast | OceanToast | Snackbar | .toast |

---

## Ocean Theme Tokens

| Token | Value | Usage |
|-------|-------|-------|
| Surface5-30 | White 5-30% | Backgrounds |
| Border10-30 | White 10-30% | Borders |
| CoralBlue | #3B82F6 | Primary |
| SeafoamGreen | #10B981 | Success |
| CoralRed | #EF4444 | Error |
| TextPrimary | White 90% | Headers |
| TextSecondary | White 80% | Body |

---

## Equivalence Checks (Updates)

| Check | Requirement |
|-------|-------------|
| Visual | Same layout, spacing, colors |
| Touch | 48dp minimum |
| States | hover, pressed, disabled, focused |
| Accessibility | contentDescription, roles |

---

## Platform Code Examples

### Compose
```kotlin
GlassmorphicSurface(background = Surface10, border = Border20)
```

### SwiftUI
```swift
GlassmorphicView(background: .surface10, border: .border20)
```

### React
```tsx
<GlassmorphicSurface className="bg-surface-10 border-border-20" />
```

---

## Full Specs

| Platform | Location |
|----------|----------|
| Design System | `Avanues/docs/universal/LD-magicui-design-system.md` |
| Compose | `Avanues/docs/android/magicui-compose-implementation.md` |
| SwiftUI | `Avanues/docs/ios/magicui-swiftui-implementation.md` |
| Web | `Avanues/docs/web/magicui-web-desktop-implementation.md` |

---

**Updated:** 2025-11-29

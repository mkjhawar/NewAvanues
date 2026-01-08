---
description: Full UI workflow - research, spec, design, Q&A, creation | /i.createui "settings screen"
allowed-tools: Read, Glob, Grep, Edit, Write, Bash, Task, WebSearch, WebFetch, AskUserQuestion
---

# /i.createui - Intelligent UI Creation

Complete UI workflow from research to implementation.

---

## API

Endpoint: `http://localhost:3850/v1/createui`
Token savings: 97% vs MCP

---

## 5-Phase Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│  Phase 1: RESEARCH   → UI/UX best practices, platform patterns      │
│  Phase 2: SPEC       → Requirements, features, data models          │
│  Phase 3: DESIGN     → Theme, components, ASCII layouts             │
│  Phase 4: Q&A        → Clarify choices, get user approval           │
│  Phase 5: CREATE     → Generate code, HTML demo, verify             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Modifiers

| Modifier | Type | Description |
|----------|------|-------------|
| `.design` | Mode | Full 5-phase workflow (default) |
| `.quick` | Mode | Skip research, minimal Q&A |
| `.yolo` | Mode | Skip all Q&A, optimal solutions |
| `.research` | Phase | Run research phase only |
| `.spec` | Phase | Run spec phase only |
| `.layout` | Phase | Run design/layout phase only |
| `.create` | Phase | Run creation phase only |
| `.app` | Scope | Single screen/view |
| `.module` | Scope | Multi-screen module |
| `.repo` | Scope | Full application UI |
| `.android` | Platform | Android (Jetpack Compose) |
| `.ios` | Platform | iOS (SwiftUI) |
| `.web` | Platform | Web (React + Tailwind) |
| `.kmp` | Platform | Kotlin Multiplatform |
| `.magicui` | Framework | MagicUI cross-platform |
| `.native` | Framework | Platform native (default) |
| `.from-mockup {file}` | Source | Generate from image mockup |
| `.from-spec {file}` | Source | Generate from existing spec |

---

## Examples

| Command | Effect |
|---------|--------|
| `/i.createui "settings"` | Full 5-phase workflow for settings screen |
| `/i.createui .app "dashboard"` | Design + create dashboard screen |
| `/i.createui .module "auth"` | Full auth module (login, signup, forgot) |
| `/i.createui .yolo .android "onboarding"` | Auto-create Android onboarding |
| `/i.createui .research "task list"` | Research UI patterns only |
| `/i.createui .from-mockup design.png` | Generate from mockup image |
| `/i.createui .ios .web "profile"` | Create for iOS + Web |
| `/i.createui .quick .app "modal"` | Quick modal creation |

---

## Phase 1: Research

Auto-research based on UI type:

| UI Type | Research Topics |
|---------|-----------------|
| Settings | Toggle placement, grouping, platform patterns |
| Dashboard | Metrics layout, cards, data visualization |
| List/Feed | Scroll behavior, pull-refresh, swipe actions |
| Form | Validation, errors, field grouping, keyboards |
| Onboarding | Steps, skip option, permissions timing |
| Auth | Login/signup flow, social auth, biometrics |
| Modal/Dialog | Size, dismiss behavior, action placement |
| Navigation | Tab bar, drawer, bottom sheet patterns |

**Sources by Platform:**

| Platform | Design Systems |
|----------|----------------|
| Android | Material Design 3, Jetpack Compose guidelines |
| iOS | Human Interface Guidelines, SF Symbols |
| Web | Tailwind CSS, Radix UI, shadcn/ui |
| KMP | Compose Multiplatform, MagicUI |

---

## Phase 2: Spec

Define requirements interactively (unless .yolo):

```
SPEC: SettingsScreen
  TYPE: settings
  PLATFORM: android+ios

  FEATURES:
    - Account section (profile, email, password)
    - Preferences section (theme, notifications, language)
    - About section (version, licenses, support)

  DATA:
    - UserPreferences: theme, notifications, language
    - AccountInfo: name, email, avatar

  REQUIREMENTS:
    - Dark mode toggle
    - Biometric unlock option
    - Clear cache action
```

---

## Phase 3: Design

### Theme Definition

```
THEME: AppTheme
  COLORS:
    primary: #2563EB
    surface: #FFFFFF
    onSurface: #1F2937
    error: #DC2626

  TYPOGRAPHY:
    heading: Inter 24sp Bold
    body: Inter 16sp Regular
    caption: Inter 12sp Regular

  SPACING: 4, 8, 12, 16, 24, 32
  RADIUS: 8, 12, 16
```

### ASCII Layout Preview

```
┌────────────────────────────────────────┐
│ [<]  Settings                          │
├────────────────────────────────────────┤
│                                        │
│  ┌─ Account ─────────────────────────┐ │
│  │ [👤] Profile                  [>] │ │
│  │ [✉] Email                    [>] │ │
│  │ [🔒] Password                [>] │ │
│  └───────────────────────────────────┘ │
│                                        │
│  ┌─ Preferences ─────────────────────┐ │
│  │ Dark Mode              [====○]    │ │
│  │ Notifications          [○====]    │ │
│  │ Language               English [>]│ │
│  └───────────────────────────────────┘ │
│                                        │
│  ┌─ About ───────────────────────────┐ │
│  │ Version                    1.0.0  │ │
│  │ Licenses                      [>] │ │
│  │ Support                       [>] │ │
│  └───────────────────────────────────┘ │
│                                        │
│  [ Clear Cache ]                       │
│  [ Sign Out ]                          │
│                                        │
└────────────────────────────────────────┘
```

---

## Phase 4: Q&A (Interactive)

Unless `.yolo`, ask for approval at key points:

1. **After Research**: "Found these patterns. Proceed?"
2. **After Spec**: "Requirements look correct?"
3. **After Design**: "Layout acceptable? Any changes?"
4. **Before Create**: "Ready to generate code?"

Use `AskUserQuestion` tool with options.

---

## Phase 5: Create

### Code Generation

Generate platform-specific code:

| Platform | Output |
|----------|--------|
| Android | Composable functions, Material3 components |
| iOS | SwiftUI views, SF Symbols |
| Web | React components, Tailwind classes |
| KMP | Compose Multiplatform, expect/actual |
| MagicUI | Cross-platform MagicElements |

### HTML Demo (Optional)

Interactive HTML/CSS demo for preview:

```html
<!-- Generated demo with Ocean theme -->
<!-- Clickable, shows interactions -->
<!-- Open in browser for testing -->
```

### Verification

- Accessibility: Touch targets 48dp+, contrast ratios
- Responsiveness: Portrait/landscape, tablet
- Platform conventions: Back button, swipe gestures

---

## Component Mapping

| Need | MagicUI | Compose | SwiftUI | React |
|------|---------|---------|---------|-------|
| Card | GlassmorphicSurface | Card | GroupBox | Card |
| List | DataTable | LazyColumn | List | ul/li |
| Toggle | MagicSwitch | Switch | Toggle | Switch |
| Button | OceanButton | Button | Button | button |
| Input | MagicTextField | TextField | TextField | input |
| Dialog | OceanDialog | AlertDialog | .alert | Dialog |

---

## Output Files

| File | Purpose |
|------|---------|
| `{Screen}.kt` | Android Compose implementation |
| `{Screen}.swift` | iOS SwiftUI implementation |
| `{Screen}.tsx` | Web React implementation |
| `{Screen}.html` | Interactive HTML demo |
| `{Screen}-spec.md` | Generated specification |

---

## Execution

{{$arguments}}

### Workflow

1. **Parse** modifiers and scope
2. **Research** (unless .quick/.yolo) - WebSearch best practices
3. **Spec** - Define or load from .from-spec
4. **Design** - Theme + ASCII layout
5. **Q&A** (unless .yolo) - AskUserQuestion for approval
6. **Create** - Generate code for target platforms
7. **Demo** (unless .yolo) - Offer HTML demo
8. **Verify** - Check accessibility, conventions

### API Call

```bash
curl -X POST http://localhost:3850/v1/createui \
  -H "Content-Type: application/json" \
  -d '{"ui": "settings", "platforms": ["android", "ios"], "mode": "design"}'
```

### Fallback

If API unavailable, execute phases directly using tools.

---

## Related Commands

| Command | Purpose |
|---------|---------|
| `/i.spec` | Create detailed specification |
| `/i.plan` | Create implementation plan |
| `/i.implement` | Execute implementation |
| `/i.analyze .ui` | Analyze existing UI |

---

## Metadata

- **Command:** `/i.createui`
- **Version:** 2.0 (merged from i.design)
- **API Port:** 3850

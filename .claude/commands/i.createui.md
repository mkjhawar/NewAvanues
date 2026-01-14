---
description: Intelligent UI creation with auto-research, ASCII preview, HTML demo | /createui .app "settings"
allowed-tools: Read, Glob, Grep, Edit, Write, Bash, Task, WebSearch, ideacode_fs, ideacode_checkpoint, ideacode_research, AskUserQuestion
---


---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3847/i.createui`
Auto-start: API server starts automatically if not running

---

# CreateUI

Automated intelligent UI creation.

---

## Modifiers

| Modifier | Type | Description |
|----------|------|-------------|
| `.repo` | Scope | Entire repository |
| `.app` | Scope | Single application |
| `.module` | Scope | Specific module |
| `.update` | Scope | Update existing UI |
| `.native` | Framework | Platform native (default) |
| `.magicui` | Framework | MagicUI cross-platform |
| `.from-mockup {file}` | Source | Generate UI code from design mockup (PNG/JPG) - uses vision analysis |
| `.ui` | Focus | Focus on UI-specific implementation details |
| `.yolo` | Mode | Skip approval, optimum solutions |

---

## Examples

| Command | Effect |
|---------|--------|
| `/createui .app "settings"` | Create settings screen (native, interactive) |
| `/createui .module .magicui "auth"` | Auth module with MagicUI |
| `/createui .update .app "dashboard"` | Update existing dashboard |
| `/createui .yolo .app "onboarding"` | Auto-create, no approval |
| `/createui .from-mockup design.png "login screen"` | Generate login UI from design mockup using vision |
| `/createui .ui .app AVA "settings"` | Create AVA settings with UI-focused implementation |

---

## Pipeline

```
1. REQUIREMENTS (auto or Q&A if unclear)
   - If .from-mockup: Use ideacode_vision to analyze mockup first
2. RESEARCH best practices for UI type
3. DESIGN component hierarchy + theme
   - If .from-mockup: Extract layout/colors/spacing from vision analysis
4. ASCII PREVIEW (unless .yolo)
5. APPROVAL (unless .yolo)
6. GENERATE platform code
   - If .ui: Focus on UI-specific details (animations, interactions, accessibility)
7. HTML DEMO (ask user, unless .yolo skip)
8. VERIFY accessibility + touch targets
```

---

## Auto-Research by UI Type

| UI Type | Research Topics |
|---------|-----------------|
| Settings | Platform patterns, grouping, toggle placement |
| Dashboard | Metrics layout, cards, data visualization |
| List/Feed | Scroll, pull-refresh, swipe actions |
| Form | Validation, errors, field grouping |
| Onboarding | Steps, skip, permissions |
| Auth | Login/signup, social, biometrics |

---

## ASCII Preview Format

```
┌────────────────────────────────┐
│ [<] Title                  [?] │
├────────────────────────────────┤
│  ┌─ Section ───────────────┐   │
│  │ Item 1           [>]    │   │
│  │ Toggle        [====○]   │   │
│  └─────────────────────────┘   │
│  [ Action Button ]             │
└────────────────────────────────┘
```

Interactive: Display, ask approval
YOLO: Skip, use optimum design

---

## HTML Demo

| Option | Description |
|--------|-------------|
| Basic | Static HTML/CSS with Ocean theme |
| Interactive | JS for clicks, toggles, navigation |
| Advanced | KMP bridge for functional testing |

Ask user (unless .yolo skips).

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

## Always Generate UI For

| Screen | Priority |
|--------|----------|
| User Settings | HIGH |
| Developer Settings | HIGH |
| Onboarding | HIGH |
| Dashboard | HIGH |
| Profile | MEDIUM |

---

## Execution

{{$arguments}}

1. Parse: scope (.repo/.app/.module/.update), framework (.native/.magicui), source (.from-mockup), focus (.ui), mode (.yolo)
2. If .from-mockup: Use ideacode_vision to analyze mockup image
3. If unclear requirements + not .yolo: Q&A
4. Research best practices (WebSearch)
5. Design + ASCII preview (incorporate vision analysis if .from-mockup)
6. If not .yolo: await approval
7. Generate code (focus on UI details if .ui)
8. If not .yolo: offer HTML demo
9. Verify and report

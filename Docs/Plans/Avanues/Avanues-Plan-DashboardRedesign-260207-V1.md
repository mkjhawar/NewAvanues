# Avanues Dashboard Redesign — Design Spec

**Module:** Avanues App
**Date:** 2026-02-07
**Branch:** 060226-1-consolidation-framework
**Version:** V1

---

## Current Problems
1. Quick Actions (Browser, Voice, Cursor, Learn) are redundant — everything auto-starts
2. Settings screen mixes user preferences with developer controls
3. No visibility into voice commands or synonyms
4. RPC Server shown in user-facing settings (it's a developer/debugging concern)
5. No way for users to see or customize voice commands per-app

## Proposed Architecture

### 3 Screens (replaces current 2)

| Screen | Route | Purpose | Access |
|--------|-------|---------|--------|
| **Dashboard** (HomeScreen) | `voice_home` | Module status + command center | Main screen |
| **Settings** | `settings` | User preferences only | Gear icon |
| **Developer Console** | `developer` | Debug tools, raw settings, DB viewer | Hidden (7-tap version or toggle) |

---

## Dashboard Designs (3 Options)

### Option A: Status-First Dashboard

Clean, informational, Material3 cards.

```
┌─────────────────────────────────────┐
│  Avanues                        [⚙] │
├─────────────────────────────────────┤
│                                     │
│  ── System Health ──────────────    │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ VoiceAvanue          [ON] ● │    │  ← Green dot, tap card → system accessibility settings
│  │ Voice commands active       │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ WebAvanue          [Ready] ● │    │  ← Always green
│  │ Voice browser available     │    │
│  └─────────────────────────────┘    │
│  ┌─────────────────────────────┐    │
│  │ VoiceCursor          [ON] ● │    │  ← Green if overlay on, tap → overlay settings
│  │ Dwell click: 1.5s          │    │
│  └─────────────────────────────┘    │
│                                     │
│  ── Permissions ────────────────    │
│  ┌─────────────────────────────┐    │
│  │ ♿ Accessibility     ✓      │    │  ← Tap → system settings if OFF
│  │ ⬛ Overlay           ✓      │    │
│  │ 🎤 Microphone        ✓      │    │
│  │ 🔋 Battery Opt.   Exempt   │    │
│  │ 🔔 Notifications    ✓      │    │
│  └─────────────────────────────┘    │
│                                     │
│  ── Voice Commands ─────────────    │
│  ┌─────────────────────────────┐    │
│  │ Static: 48 commands         │    │  ← Tap to expand/manage
│  │ Dynamic: 12 (current app)   │    │
│  │ Custom: 3 user-defined      │    │
│  │ Synonyms: 156 active        │    │
│  │                    [Manage>] │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

**Pros:** Clean, informational, easy to scan
**Cons:** Linear/conventional, lots of scrolling

---

### Option B: Control-Center Dashboard

iOS Control Center inspired tile grid. Compact, interactive.

```
┌─────────────────────────────────────┐
│  Avanues                        [⚙] │
├─────────────────────────────────────┤
│                                     │
│  ┌──────────┐  ┌──────────┐        │
│  │ 🗣️        │  │ 🌐        │        │
│  │VoiceAvanue│  │ WebAvanue│        │  ← Tiles glow green/red
│  │   ● ON   │  │  ● Ready │        │     Tap to toggle/configure
│  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐        │
│  │ 👆        │  │ 🔋        │        │
│  │VoiceCursor│  │ Battery  │        │
│  │   ● ON   │  │ ● Exempt │        │
│  └──────────┘  └──────────┘        │
│  ┌──────────┐  ┌──────────┐        │
│  │ 🎤        │  │ 🔔        │        │
│  │   Mic    │  │  Notif   │        │
│  │ ● Granted│  │ ● Granted│        │
│  └──────────┘  └──────────┘        │
│                                     │
│  ══════════════════════════════     │
│                                     │
│  Voice Commands              [>]    │
│  ┌─────────────────────────────┐    │
│  │ "go back"  "scroll up"     │    │  ← Scrolling chip cloud
│  │ "click 4"  "open settings" │    │     of active commands
│  │ "take screenshot" ...      │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

**Pros:** Compact, glanceable, interactive tiles
**Cons:** Less descriptive, may feel cluttered on small screens

---

### Option C: Reactive Service Bus Dashboard (Unique/Recommended)

Glassmorphic cards with real-time state animations. Each service emits a StateFlow.
Pulse animation when active, fade when inactive. The app literally "breathes".

```
┌─────────────────────────────────────┐
│  Avanues                        [⚙] │
├───────────────── ─ ─ ─ ─ ── ───────┤
│                                     │
│  ╔═══════════════════════════════╗  │
│  ║  ◉ VoiceAvanue        ACTIVE ║  │  ← Glassmorphic card with
│  ║  ░░░░░░░░░░░░░░░░░░░░░░░░░░ ║  │     subtle pulse animation
│  ║  Listening · 48 commands     ║  │     Tap → system settings
│  ║  Last: "scroll down" (2s ago)║  │     Shows last command heard
│  ╚═══════════════════════════════╝  │
│                                     │
│  ╔═══════════════════════════════╗  │
│  ║  ◉ WebAvanue          READY  ║  │  ← Steady glow (always on)
│  ║  ░░░░░░░░░░░░░░░░░░░░░░░░░░ ║  │
│  ║  Voice browser · 3 tabs open ║  │     Dynamic: shows tab count
│  ╚═══════════════════════════════╝  │
│                                     │
│  ╔═══════════════════════════════╗  │
│  ║  ◉ VoiceCursor        ACTIVE ║  │  ← Active pulse when cursor
│  ║  ░░░░░░░░░░░░░░░░░░░░░░░░░░ ║  │     is on screen
│  ║  Dwell: 1.5s · Smoothing ON ║  │
│  ╚═══════════════════════════════╝  │
│                                     │
│  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
│  │ System                       │   │  ← Collapsed by default
│  │ ♿✓  ⬛✓  🎤✓  🔋✓  🔔✓       │   │     All-green = collapsed
│  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │     Any red = expands
│                                     │
│  ── Commands ───────────────────    │
│  ┌─────────────────────────────┐    │
│  │ Static (48)  Dynamic (12)   │    │  ← Tab row
│  │ Custom (3)   Synonyms (156) │    │
│  ├─────────────────────────────┤    │
│  │ "go back"            [edit] │    │  ← Scrollable list
│  │   aka: navigate back, back  │    │     Shows synonyms inline
│  │ "scroll up"          [edit] │    │
│  │   aka: swipe up, page up    │    │
│  │ "screenshot"         [edit] │    │
│  │   aka: capture, snap        │    │
│  │            [+ Add Command]  │    │
│  └─────────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘
```

**Why this is unique:**
- Cards "breathe" with service state (pulse = active, fade = inactive)
- Last-heard command creates a living feedback loop
- System health auto-collapses when everything is green (no clutter)
- Commands section is the real star — users manage their voice vocabulary
- Glassmorphic styling from AvanueUI module (already built)
- StateFlow architecture enables KMP reuse on iOS/Desktop

---

## Command Management Screen (Tap "Manage" or tab in Commands section)

```
┌─────────────────────────────────────┐
│  ← Voice Commands                   │
├─────────────────────────────────────┤
│  [Static] [By App] [Custom] [Syn]  │  ← Tab row
├─────────────────────────────────────┤
│                                     │
│  ── Static Tab ─────────────────    │
│  Navigation (8)              [v]    │
│  ┌─────────────────────────────┐    │
│  │ ☑ go back                   │    │  ← Toggle to enable/disable
│  │   navigate back, back, prev │    │     Synonyms shown below
│  │ ☑ scroll up                 │    │
│  │   swipe up, page up         │    │
│  │ ☑ scroll down               │    │
│  │   swipe down, page down     │    │
│  └─────────────────────────────┘    │
│  Media (6)                   [v]    │
│  System (10)                 [v]    │
│  VoiceOS Control (12)        [v]    │
│  App Launch (5)              [v]    │
│  Accessibility (7)           [v]    │
│                                     │
│  ── By App Tab ─────────────────    │
│  Currently: Chrome (12 commands)    │
│  ┌─────────────────────────────┐    │
│  │ "Search bar"    → click     │    │  ← Dynamic commands for
│  │ "New tab"       → click     │    │     current foreground app
│  │ "1" "2" "3"     → click     │    │     (generated from scan)
│  │ "Back"          → navigate  │    │
│  └─────────────────────────────┘    │
│  History: [Gmail] [Maps] [Phone]   │  ← Recently used apps
│                                     │
│  ── Custom Tab ─────────────────    │
│  ┌─────────────────────────────┐    │
│  │ "lights on"  → smart home   │    │  ← User-defined commands
│  │   phrases: lights on, turn  │    │
│  │   on lights, illumination   │    │
│  │                      [edit] │    │
│  └─────────────────────────────┘    │
│  [+ New Custom Command]            │
│                                     │
│  ── Synonyms Tab ───────────────    │
│  ┌─────────────────────────────┐    │
│  │ click ↔ tap, press, push,  │    │  ← Bidirectional synonym map
│  │         hit, select         │    │
│  │              [+ add] [edit] │    │  ← User can add/remove
│  │ scroll ↔ swipe, drag       │    │
│  │              [+ add] [edit] │    │
│  │ open ↔ launch, start, go to│    │
│  │              [+ add] [edit] │    │
│  └─────────────────────────────┘    │
│  [+ New Synonym Group]             │
│                                     │
└─────────────────────────────────────┘
```

---

## Developer Console (Hidden Screen)

Access: 7-tap on version number in Settings, or toggle in Settings.

```
┌─────────────────────────────────────┐
│  ← Developer Console                │
├─────────────────────────────────────┤
│                                     │
│  ── Raw Settings (DataStore) ───    │
│  dwell_click_enabled:    true       │
│  dwell_click_delay_ms:   1500.0     │
│  cursor_smoothing:       true       │
│  voice_feedback:         true       │
│  auto_start_on_boot:     false      │
│  search_engine:          Google     │
│  [Edit Raw Values]                  │
│                                     │
│  ── Databases ──────────────────    │
│  VoiceOS DB    [Browse] [Export]    │
│  Browser DB    [Browse] [Export]    │
│  Custom Cmds   [Browse] [Export]    │
│                                     │
│  ── Command Generation ─────────    │
│  Last scan: 2.3s ago                │
│  Elements found: 34                 │
│  Commands generated: 12             │
│  [Force Rescan] [Show Tree]        │
│                                     │
│  ── Actions Log ────────────────    │
│  12:45:03  CLICK "Submit" (0.92)   │
│  12:44:58  SCROLL_DOWN (0.88)      │
│  12:44:51  BACK (0.95)             │
│  [Clear Log] [Export]               │
│                                     │
│  ── Services ───────────────────    │
│  Accessibility: Running (PID 1234) │
│  CursorOverlay: Running (PID 1235) │
│  VoiceRecognition: Running          │
│  RPC Server: Stopped                │
│    VoiceOS :50051  [Start]         │
│    WebAvanue :50055 [Start]        │
│                                     │
│  ── Synonym Debug ──────────────    │
│  Loaded: 156 entries                │
│  Source: built-in (en-US)           │
│  Custom overrides: 3                │
│  [Reload] [Export] [Import]        │
│                                     │
└─────────────────────────────────────┘
```

---

## Settings Screen (Simplified — User-facing only)

```
┌─────────────────────────────────────┐
│  ← Settings                         │
├─────────────────────────────────────┤
│                                     │
│  ── VoiceCursor ────────────────    │
│  Dwell Click           [toggle]    │
│  Dwell Delay        [===●===]     │  ← Slider 500-3000ms
│  Cursor Smoothing      [toggle]    │
│                                     │
│  ── Voice ──────────────────────    │
│  Voice Feedback        [toggle]    │
│  Language              [en-US >]   │
│                                     │
│  ── System ─────────────────────    │
│  Start on Boot         [toggle]    │
│                                     │
│  ── About ──────────────────────    │
│  Version     1.0.0-alpha01         │  ← 7-tap = developer mode
│  Licenses              [View >]    │
│  Open Source           [View >]    │
│                                     │
└─────────────────────────────────────┘
```

---

## Architecture: Reactive Service Bus

```
ServiceStateProvider (commonMain interface)
├── VoiceAvanueServiceState : StateFlow<ServiceState>
├── WebAvanueServiceState : StateFlow<ServiceState>
├── VoiceCursorServiceState : StateFlow<ServiceState>
└── SystemPermissionState : StateFlow<PermissionState>

ServiceState = Running(metadata) | Stopped | Error(msg) | Degraded(reason)
PermissionState = AllGranted | Missing(list)

DashboardViewModel (androidMain)
├── observes all StateFlows
├── combines into DashboardUiState
└── exposes single Flow<DashboardUiState> to UI
```

---

## Implementation Phases

| Phase | What | Files |
|-------|------|-------|
| 1 | ServiceStateProvider interface (commonMain) | VoiceCursor, VoiceOSCore |
| 2 | DashboardViewModel + DashboardUiState | apps/avanues/ |
| 3 | Dashboard UI (Option C glassmorphic) | HomeScreen.kt rewrite |
| 4 | Command Management screen | New: CommandScreen.kt |
| 5 | Synonym editor (add/remove user synonyms) | New: SynonymEditor.kt |
| 6 | Developer Console | New: DeveloperConsole.kt |
| 7 | Settings simplification (move dev stuff out) | SettingsScreen.kt |

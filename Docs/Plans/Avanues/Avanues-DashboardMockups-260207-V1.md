# Avanues Dashboard — Visual Mockups

Three complete dashboard designs for the Avanues consolidated app.
Each mockup shows the full scrollable screen with Material3 + Glassmorphic styling.

---

## OPTION A: Status-First Dashboard

Clean vertical list. Informational cards. No animation.

```
╔═══════════════════════════════════════╗
║  ┌─ TopAppBar ─────────────────────┐  ║
║  │                                 │  ║
║  │  Avanues                    ⚙  │  ║  ← Settings gear icon
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section Header ────────────────┐  ║
║  │  MODULES                        │  ║  ← titleSmall, onSurfaceVariant
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Card: primaryContainer ────────┐  ║  ← Green tinted card
║  │                                 │  ║
║  │  🗣  VoiceAvanue         ●────┐│  ║  ← Green dot = active
║  │                          │ ON ││  ║
║  │  Voice commands active   └────┘│  ║  ← bodySmall description
║  │                                 │  ║
║  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  ║  ← Divider
║  │  48 static · 12 dynamic        │  ║  ← Command count summary
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Card: primaryContainer ────────┐  ║
║  │                                 │  ║
║  │  🌐  WebAvanue           ●────┐│  ║  ← Green dot
║  │                          │READY│  ║
║  │  Voice browser available └────┘│  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Card: primaryContainer ────────┐  ║
║  │                                 │  ║
║  │  👆  VoiceCursor          ●────┐│  ║  ← Green dot
║  │                          │ ON ││  ║
║  │  Dwell click: 1.5s      └────┘│  ║  ← Shows current setting
║  │  Smoothing: ON                  │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section Header ────────────────┐  ║
║  │  PERMISSIONS                    │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Card: surfaceVariant ──────────┐  ║
║  │                                 │  ║
║  │  ♿ Accessibility Service    ✓  │  ║  ← Tap whole row → system
║  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  ║     settings if OFF
║  │  ⬛ Overlay Permission      ✓  │  ║
║  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  ║
║  │  🎤 Microphone              ✓  │  ║
║  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  ║
║  │  🔋 Battery Optimization  Exempt│  ║
║  │  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │  ║
║  │  🔔 Notifications           ✓  │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section Header ────────────────┐  ║
║  │  VOICE COMMANDS                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Card: surfaceVariant ──────────┐  ║
║  │                                 │  ║
║  │  Static Commands        48  >  │  ║  ← Tap → command list
║  │  Dynamic (current app)  12  >  │  ║
║  │  Custom Commands         3  >  │  ║
║  │  Synonyms              156  >  │  ║
║  │                                 │  ║
║  │  ┌─────────────────────────┐    │  ║
║  │  │   Manage Commands   >  │    │  ║  ← FilledTonalButton
║  │  └─────────────────────────┘    │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
╚═══════════════════════════════════════╝
```

**Color scheme:**
- Module cards: `primaryContainer` (soft teal/blue tint)
- Active dot: `primary` (bright teal)
- Inactive dot: `error` (red)
- Permission rows: `surfaceVariant` (neutral gray)
- Check marks: `primary` when granted, `error` when missing

---

## OPTION B: Control-Center Dashboard

Tile grid layout. Compact. Interactive. iOS Control Center inspired.

```
╔═══════════════════════════════════════╗
║  ┌─ TopAppBar ─────────────────────┐  ║
║  │                                 │  ║
║  │  Avanues                    ⚙  │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section: MODULES ─────────────┐   ║
║  │                                 │  ║
║  │  ┌──────────┐  ┌──────────┐    │  ║
║  │  │          │  │          │    │  ║
║  │  │   🗣️     │  │   🌐     │    │  ║  ← Large icon centered
║  │  │          │  │          │    │  ║
║  │  │VoiceAvanue│ │WebAvanue │    │  ║  ← labelLarge below icon
║  │  │          │  │          │    │  ║
║  │  │  ● ON   │  │ ● Ready  │    │  ║  ← Status with colored dot
║  │  │          │  │          │    │  ║
║  │  └──────────┘  └──────────┘    │  ║
║  │                                 │  ║
║  │  ┌──────────┐                   │  ║
║  │  │          │                   │  ║
║  │  │   👆     │                   │  ║
║  │  │          │                   │  ║
║  │  │VoiceCursor│                  │  ║
║  │  │          │                   │  ║
║  │  │  ● ON   │                   │  ║
║  │  │          │                   │  ║
║  │  └──────────┘                   │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section: SYSTEM ───────────────┐  ║
║  │                                 │  ║
║  │  ┌─────┐ ┌─────┐ ┌─────┐      │  ║
║  │  │ ♿  │ │ ⬛  │ │ 🎤  │      │  ║  ← Small square tiles
║  │  │  ✓  │ │  ✓  │ │  ✓  │      │  ║     Green bg if ok
║  │  └─────┘ └─────┘ └─────┘      │  ║     Red bg if missing
║  │  ┌─────┐ ┌─────┐               │  ║
║  │  │ 🔋  │ │ 🔔  │               │  ║
║  │  │  ✓  │ │  ✓  │               │  ║
║  │  └─────┘ └─────┘               │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Section: COMMANDS ─────────────┐  ║
║  │                                 │  ║
║  │  ┌─ Chip Cloud (horizontal) ─┐ │  ║
║  │  │                            │ │  ║
║  │  │ [go back] [scroll up]     │ │  ║  ← Scrollable row of
║  │  │ [click 4] [screenshot]    │ │  ║     FilterChip components
║  │  │ [open settings] [play]    │ │  ║     showing active commands
║  │  │ [next] [previous] ...     │ │  ║
║  │  │                            │ │  ║
║  │  └────────────────────────────┘ │  ║
║  │                                 │  ║
║  │  48 static · 12 dynamic · 3 custom │
║  │                                 │  ║
║  │  ┌─────────────────────────┐    │  ║
║  │  │   Manage Commands   >  │    │  ║
║  │  └─────────────────────────┘    │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
╚═══════════════════════════════════════╝
```

**Color scheme:**
- Module tiles: `primaryContainer` with `primary` border when active
- System tiles: `tertiaryContainer` (small, 48dp squares)
- Green tiles: `primaryContainer` bg
- Red tiles: `errorContainer` bg
- Command chips: `SuggestionChip` style, `secondaryContainer`

**Tile behavior:**
- Module tiles: Tap → relevant system settings or module detail
- System tiles: Tap → system permission settings
- Command chips: Tap → shows synonyms in bottom sheet

---

## OPTION C: Reactive Service Bus Dashboard (Glassmorphic)

Living, breathing dashboard. Cards pulse with service state.
Glassmorphic translucency with blur backdrop.

```
╔═══════════════════════════════════════╗
║                                       ║
║  ┌─ TopAppBar (transparent) ───────┐  ║
║  │                                 │  ║
║  │  Avanues                    ⚙  │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Background ────────────────────┐  ║
║  │  Gradient: deep navy → dark teal│  ║  ← surfaceContainer
║  │  Subtle animated mesh gradient  │  ║     dark theme base
║  └─────────────────────────────────┘  ║
║                                       ║
║  ╔══ Glassmorphic Card ════════════╗  ║
║  ║                                 ║  ║  ← Semi-transparent white
║  ║  ◉ ← ← ← ← ← ← ←            ║  ║     (alpha 0.1) with blur
║  ║  │                              ║  ║     backdrop + 1px white
║  ║  │  Pulse ring animation        ║  ║     border (alpha 0.2)
║  ║  │  (concentric circles         ║  ║
║  ║  │   expanding outward          ║  ║  ← When ACTIVE: green rings
║  ║  │   from the ◉ dot,            ║  ║     pulse every 2 seconds
║  ║  │   fading as they grow)       ║  ║
║  ║  │                              ║  ║  ← When STOPPED: dot is
║  ║  ◉                              ║  ║     gray, no animation
║  ║                                 ║  ║
║  ║  VoiceAvanue           ACTIVE   ║  ║  ← titleMedium + status badge
║  ║  ──────────────────────────     ║  ║
║  ║  Listening · 48 commands        ║  ║  ← bodySmall, onSurface/70%
║  ║  Last heard: "scroll down"      ║  ║  ← bodySmall, primary color
║  ║           2 seconds ago         ║  ║     Updates in real-time
║  ║                                 ║  ║
║  ╚═════════════════════════════════╝  ║
║       ↑                               ║
║       │ Tap anywhere on card →         ║
║       │ If OFF: opens system           ║
║       │   accessibility settings       ║
║       │ If ON: shows command overlay   ║
║                                       ║
║  ╔══ Glassmorphic Card ════════════╗  ║
║  ║                                 ║  ║
║  ║  ◉ (steady soft glow)          ║  ║  ← No pulse — always ready
║  ║                                 ║  ║     Constant subtle glow
║  ║  WebAvanue              READY   ║  ║
║  ║  ──────────────────────────     ║  ║
║  ║  Voice browser · 3 tabs open    ║  ║  ← Dynamic: reads from
║  ║  Last visited: github.com       ║  ║     BrowserRepository
║  ║                                 ║  ║
║  ╚═════════════════════════════════╝  ║
║                                       ║
║  ╔══ Glassmorphic Card ════════════╗  ║
║  ║                                 ║  ║
║  ║  ◉ ← ← ← (pulsing)            ║  ║  ← Active pulse when
║  ║                                 ║  ║     cursor visible on screen
║  ║  VoiceCursor            ACTIVE  ║  ║
║  ║  ──────────────────────────     ║  ║
║  ║  Dwell: 1.5s · Smoothing: ON   ║  ║
║  ║  Position: (523, 891)           ║  ║  ← Live cursor coordinates
║  ║                                 ║  ║     (dev-mode only?)
║  ╚═════════════════════════════════╝  ║
║                                       ║
║  ┌─ System Health Bar ─────────────┐  ║
║  │                                 │  ║  ← Collapsed horizontal row
║  │  When ALL permissions granted:  │  ║     when everything is green
║  │                                 │  ║
║  │  ♿✓  ⬛✓  🎤✓  🔋✓  🔔✓   All good │
║  │                                 │  ║  ← Entire row is one muted
║  │  (Single line, muted colors,    │  ║     card, barely visible
║  │   doesn't demand attention)     │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ System Health Bar (EXPANDED) ──┐  ║
║  │                                 │  ║  ← Expands automatically
║  │  When ANY permission is missing:│  ║     when something is wrong
║  │                                 │  ║
║  │  ╔═ errorContainer card ═════╗  │  ║
║  │  ║ ♿ Accessibility    OFF   ║  │  ║  ← Red card, prominent
║  │  ║ Tap to enable       [>]  ║  │  ║     Tap → system settings
║  │  ╚══════════════════════════╝  │  ║
║  │                                 │  ║
║  │  ⬛✓  🎤✓  🔋✓  🔔✓            │  ║  ← Others stay compact
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
║  ┌─ Commands Section ──────────────┐  ║
║  │                                 │  ║
║  │  ┌─ Tab Row ─────────────────┐  │  ║
║  │  │                           │  │  ║
║  │  │ [Static] [By App]        │  │  ║  ← ScrollableTabRow
║  │  │ [Custom] [Synonyms]      │  │  ║     Material3 tabs
║  │  │                           │  │  ║
║  │  └───────────────────────────┘  │  ║
║  │                                 │  ║
║  │  ┌─ Tab Content: Static ─────┐  │  ║
║  │  │                           │  │  ║
║  │  │  Navigation (8)       [v] │  │  ║  ← Expandable category
║  │  │  ┌───────────────────┐    │  │  ║
║  │  │  │ ☑ go back         │    │  │  ║  ← Checkbox to enable/disable
║  │  │  │   aka: navigate   │    │  │  ║     bodySmall synonyms below
║  │  │  │   back, back,     │    │  │  ║
║  │  │  │   previous screen │    │  │  ║
║  │  │  │                   │    │  │  ║
║  │  │  │ ☑ scroll up       │    │  │  ║
║  │  │  │   aka: swipe up,  │    │  │  ║
║  │  │  │   page up         │    │  │  ║
║  │  │  │                   │    │  │  ║
║  │  │  │ ☑ go home         │    │  │  ║
║  │  │  │   aka: home,      │    │  │  ║
║  │  │  │   home screen     │    │  │  ║
║  │  │  └───────────────────┘    │  │  ║
║  │  │                           │  │  ║
║  │  │  Media (6)            [v] │  │  ║  ← Collapsed
║  │  │  System (10)          [v] │  │  ║
║  │  │  VoiceOS Control (12) [v] │  │  ║
║  │  │  App Launch (5)       [v] │  │  ║
║  │  │  Accessibility (7)    [v] │  │  ║
║  │  │                           │  │  ║
║  │  └───────────────────────────┘  │  ║
║  │                                 │  ║
║  │  ┌─ Tab Content: By App ──────┐ │  ║
║  │  │                            │ │  ║
║  │  │  Current: Chrome           │ │  ║  ← Shows foreground app
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │ "Search bar" → tap │    │ │  ║  ← Live commands for
║  │  │  │ "New tab"    → tap │    │ │  ║     current screen
║  │  │  │ "1"          → tap │    │ │  ║
║  │  │  │ "2"          → tap │    │ │  ║
║  │  │  │ "Back"    → navigate│   │ │  ║
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  │  Recent apps:              │ │  ║
║  │  │  [Gmail] [Maps] [YouTube]  │ │  ║  ← Tap to see commands
║  │  │                            │ │  ║     for that app
║  │  └────────────────────────────┘ │  ║
║  │                                 │  ║
║  │  ┌─ Tab Content: Custom ──────┐ │  ║
║  │  │                            │ │  ║
║  │  │  (User-defined commands)   │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │ "lights on"        │    │ │  ║
║  │  │  │  → custom action   │    │ │  ║
║  │  │  │  phrases: lights   │    │ │  ║
║  │  │  │  on, turn on lights│    │ │  ║
║  │  │  │           [edit ✏] │    │ │  ║
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │  + New Command     │    │ │  ║  ← FAB or outlined button
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  └────────────────────────────┘ │  ║
║  │                                 │  ║
║  │  ┌─ Tab Content: Synonyms ────┐ │  ║
║  │  │                            │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │ click              │    │ │  ║  ← Canonical word
║  │  │  │ ↔ tap, press,     │    │ │  ║     Bidirectional arrow
║  │  │  │   push, hit,      │    │ │  ║     shows synonyms
║  │  │  │   select           │    │ │  ║
║  │  │  │         [+ add ✎] │    │ │  ║  ← Add user synonym
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │ scroll             │    │ │  ║
║  │  │  │ ↔ swipe, drag,    │    │ │  ║
║  │  │  │   move             │    │ │  ║
║  │  │  │         [+ add ✎] │    │ │  ║
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │ open               │    │ │  ║
║  │  │  │ ↔ launch, start,  │    │ │  ║
║  │  │  │   go to, run       │    │ │  ║
║  │  │  │         [+ add ✎] │    │ │  ║
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  │  ┌────────────────────┐    │ │  ║
║  │  │  │  + New Synonym     │    │ │  ║
║  │  │  │    Group           │    │ │  ║
║  │  │  └────────────────────┘    │ │  ║
║  │  │                            │ │  ║
║  │  └────────────────────────────┘ │  ║
║  │                                 │  ║
║  └─────────────────────────────────┘  ║
║                                       ║
╚═══════════════════════════════════════╝
```

**Glassmorphic styling details:**

```
Card properties:
  background: Color.White.copy(alpha = 0.08f)
  shape: RoundedCornerShape(20.dp)
  border: BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
  shadow: none (blur backdrop provides depth)
  modifier: Modifier.blur(0.dp) on content, blur behind

Pulse animation (ACTIVE state):
  - 3 concentric circles from the ◉ indicator
  - Scale: 1.0 → 2.5 over 2 seconds
  - Alpha: 0.4 → 0.0 (fade out as they expand)
  - Color: primary.copy(alpha = animatedAlpha)
  - Staggered: ring 1 starts, ring 2 at +0.7s, ring 3 at +1.4s
  - Repeat: infinite

Status badge:
  ACTIVE  = primary color, rounded pill
  READY   = tertiary color, rounded pill
  STOPPED = error color, rounded pill
  ERROR   = error + pulsing outline

Background gradient:
  Brush.verticalGradient(
    colors = listOf(
      Color(0xFF0A1628),  // Deep navy
      Color(0xFF0D2137),  // Dark teal
      Color(0xFF0A1628)   // Back to navy
    )
  )
```

**State transitions:**

```
OFF → ON:
  Card slides up 4dp
  Dot fades from gray to green (300ms)
  Pulse starts
  Status badge: "STOPPED" → "ACTIVE" with crossfade

ON → OFF:
  Card slides down 4dp
  Dot fades from green to gray (300ms)
  Pulse stops (last rings fade)
  Status badge: "ACTIVE" → "STOPPED" with crossfade

System Health collapse:
  All green: Row height 48dp, single line, muted
  Any red: animateContentSize → expanded cards (200ms ease)
  Red card: errorContainer with prominent styling

Last command update:
  New command: text fades out → new text fades in (150ms)
  Time counter: updates every second ("2s ago" → "3s ago")
```

---

## Side-by-Side Comparison

```
┌─────────────┬──────────────┬──────────────────┐
│  Option A    │  Option B     │  Option C         │
│  Status-First│  Control-Ctr  │  Reactive/Glass   │
├─────────────┼──────────────┼──────────────────┤
│              │              │                  │
│  [Card]     │  [■][■]      │  ╔═══ blur ═══╗  │
│  VoiceA ●   │  VA   WA     │  ║◉ VoiceA    ║  │
│  48 cmds    │  ●ON  ●RDY   │  ║ Listening  ║  │
│              │              │  ║ Last: "..."║  │
│  [Card]     │  [■]         │  ╚════════════╝  │
│  WebA  ●    │  VC           │                  │
│  Browser    │  ●ON         │  ╔═══ blur ═══╗  │
│              │              │  ║◉ WebA      ║  │
│  [Card]     │  [□][□][□]   │  ║ 3 tabs     ║  │
│  VoiceC ●   │  ♿✓ ⬛✓ 🎤✓  │  ╚════════════╝  │
│              │  [□][□]      │                  │
│  ───────    │  🔋✓ 🔔✓      │  ╔═══ blur ═══╗  │
│  Permissions│              │  ║◉ VoiceC    ║  │
│  ♿✓         │  ───────     │  ║ Dwell 1.5s ║  │
│  ⬛✓         │  [chip cloud] │  ╚════════════╝  │
│  🎤✓         │  go back     │                  │
│  🔋✓         │  scroll up   │  ♿✓⬛✓🎤✓🔋✓🔔✓   │
│  🔔✓         │  click 4     │  (auto-collapse) │
│              │              │                  │
│  ───────    │  ───────     │  ───────         │
│  Commands   │  Manage >    │  [Static][ByApp] │
│  48 static  │              │  [Custom][Syn]   │
│  12 dynamic │              │                  │
│  Manage >   │              │  ☑ go back       │
│              │              │    aka: back...  │
│              │              │  ☑ scroll up     │
│              │              │    aka: swipe... │
├─────────────┼──────────────┼──────────────────┤
│ Clean, safe  │ Compact,     │ Unique, living,  │
│ Conventional │ glanceable   │ glassmorphic     │
│ More scroll  │ Less detail  │ Command center   │
│ No animation │ Tile-based   │ Real-time state  │
└─────────────┴──────────────┴──────────────────┘
```

---

## Recommendation

**Option C** is the clear winner for Avanues because:

1. The "breathing" cards make the dashboard **feel alive** — you know at a glance what's running
2. The auto-collapsing system health row eliminates noise when everything is fine
3. The integrated command tabs make this a **command center**, not just a status page
4. The glassmorphic styling uses **AvanueUI components you already built**
5. "Last heard: scroll down (2s ago)" creates trust — users see the system is listening
6. No other accessibility app has this kind of dashboard

The command management section is the same across all 3 options — it's the top section that differs.

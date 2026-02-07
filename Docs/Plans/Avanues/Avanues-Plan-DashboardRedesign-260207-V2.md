# Avanues Dashboard — Option C: Reactive Service Bus (Final Design)

**Module:** Avanues App
**Date:** 2026-02-07
**Branch:** 060226-1-consolidation-framework
**Version:** V2 (supersedes V1)
**Design System:** AvanueUI / AvaUI (OceanDesignTokens + GlassAvanue + GlassmorphicComponents)

---

## Design Principles

1. **No scrolling on main dashboard** — everything visible at once (critical for smart glasses)
2. **Landscape-first for smart glasses** — primary layout, 3-column
3. **Portrait adapts gracefully** — 2-row layout for phones, still no scroll on module section
4. **All styling via AvanueUI tokens** — zero hardcoded colors, all theme-swappable
5. **GlassAvanue AR mode** — transparent-first, works on Vuzix/RealWear/Rokid
6. **Dwell-click safe** — all interactive areas >= `SizeTokens.MinTouchTargetSpatial` (60dp)

---

## Token Reference (Used in Mockups)

All values below are from the AvanueUI design system. Swapping the theme
(e.g. OceanTheme → iOS26LiquidGlass → Windows11Fluent2) changes all visuals
without touching layout code.

```
COLORS (OceanTheme / OceanDesignTokens)
──────────────────────────────────────
Background         = OceanTheme.background         (#0F172A deep slate)
Surface            = OceanTheme.surface             (#1E293B)
SurfaceElevated    = OceanTheme.surfaceElevated     (#334155)
Primary            = OceanTheme.primary             (#3B82F6 coral blue)
PrimaryLight       = OceanTheme.primaryLight        (#60A5FA)
TextPrimary        = OceanTheme.textPrimary         (#E2E8F0)
TextSecondary      = OceanTheme.textSecondary       (#CBD5E1)
TextTertiary       = OceanTheme.textTertiary        (#94A3B8)
Success            = OceanDesignTokens.State.success (#10B981)
Warning            = OceanDesignTokens.State.warning (#F59E0B)
Error              = OceanDesignTokens.State.error   (#EF4444)
GlassLight         = OceanDesignTokens.Glass.light   (0.05f opacity)
GlassMedium        = OceanDesignTokens.Glass.medium  (0.08f opacity)
GlassHeavy         = OceanDesignTokens.Glass.heavy   (0.12f opacity)
BorderDefault      = OceanDesignTokens.Border.default (20% white)
BorderSubtle       = OceanDesignTokens.Border.subtle  (10% white)

GLASS COMPONENTS
──────────────────────────────────────
Module Cards       = GlassCard + GlassLevel.MEDIUM + GlassDefaults.border
System Health      = GlassSurface + GlassLevel.LIGHT + GlassDefaults.borderSubtle
Status Badge       = GlassChip (glass=true, GlassLevel.LIGHT)
Command Tabs       = GlassChip (glass=true, GlassLevel.LIGHT)
Pulse Dot          = Custom Canvas + OceanTheme.primary

SPACING (OceanDesignTokens.Spacing / SpacingTokens)
──────────────────────────────────────
xs                 = 4.dp
sm                 = 8.dp
md                 = 12.dp
lg                 = 16.dp
xl                 = 24.dp
xxl                = 32.dp

SHAPES (GlassShapes / ShapeTokens)
──────────────────────────────────────
Card               = GlassShapes.large       (16.dp)
Chip/Badge         = ShapeTokens.Full        (pill/9999.dp)
SmallCard          = GlassShapes.default     (12.dp)

TYPOGRAPHY (TypographyTokens)
──────────────────────────────────────
Module Name        = TitleMedium  (16sp, medium weight)
Module Description = BodySmall    (12sp)
Section Label      = LabelMedium  (12sp, medium weight)
Status Badge       = LabelSmall   (11sp, medium weight)
Last Command       = BodySmall    (12sp) in PrimaryLight color
Command Name       = BodyMedium   (14sp)
Synonym Text       = BodySmall    (12sp) in TextTertiary

SIZES (SizeTokens)
──────────────────────────────────────
Touch Target       = MinTouchTarget         (48.dp)
Spatial Target     = MinTouchTargetSpatial  (60.dp)  ← smart glasses
Pulse Dot          = IconSmall              (16.dp)
Status Icon        = IconMedium             (24.dp)
AppBar             = AppBarHeightCompact    (48.dp)

ANIMATION (OceanDesignTokens.Animation / AnimationTokens)
──────────────────────────────────────
Pulse cycle        = 2000ms (custom, infinite)
State transition   = AnimationTokens.DurationMedium (300ms)
Text crossfade     = AnimationTokens.DurationShort  (150ms)
Card lift          = OceanDesignTokens.Animation.normal (200ms)

RESPONSIVE (ResponsiveTokens)
──────────────────────────────────────
Portrait phone     = < BreakpointSM (600dp)  → 1-column
Landscape phone    = BreakpointSM-MD         → 3-column
Tablet landscape   = BreakpointMD (840dp)    → 3-column + wider commands
Smart glasses      = BreakpointSM range      → 3-column, no scroll

AR MODE (GlassAvanue)
──────────────────────────────────────
Background opacity = 0.0f (fully transparent passthrough)
Card opacity       = GlassAvanue.forContext(AppContext.AR) (65-75%)
Blur radius        = 20-30px
Ambient adaptation = adaptToAmbientLight() (auto)
```

---

## LANDSCAPE LAYOUT (Smart Glasses / Tablet / Landscape Phone)

**Breakpoint:** width >= `ResponsiveTokens.BreakpointSM` (600dp)
**Constraint:** ZERO scrolling. Everything fits in viewport.
**Layout:** 3-column Row, equal weight

```
╔═══════════════════════════════════════════════════════════════════════════════════╗
║                                                                                   ║
║  ┌─ TopAppBar: AppBarHeightCompact (48dp) ─────────────────────────────────────┐  ║
║  │  Avanues                                                               [⚙]  │  ║
║  └──────────────────────────────────────────────────────────────────────────────┘  ║
║                                                                                   ║
║  Background: OceanTheme.background (#0F172A)                                      ║
║  Padding: SpacingTokens.Medium (16dp) horizontal, SpacingTokens.Small (8dp) vert  ║
║                                                                                   ║
║  ┌─── Column 1 (weight 1f) ────┐  ┌── Column 2 (weight 1f) ──┐  ┌── Column 3 (weight 1.2f) ──┐
║  │                              │  │                           │  │                              │
║  │  MODULES                     │  │  SYSTEM          VOICE    │  │  COMMANDS                    │
║  │  LabelMedium, TextTertiary   │  │  LabelMedium             │  │  LabelMedium                 │
║  │                              │  │                           │  │                              │
║  │  ╔══ GlassCard MEDIUM ════╗  │  │  ┌─ GlassSurface LIGHT ─┐│  │  ┌─ Tab Row ──────────────┐ │
║  │  ║                        ║  │  │  │                       ││  │  │                         │ │
║  │  ║  ◉←pulse  VoiceAvanue ║  │  │  │ ♿✓  ⬛✓  🎤✓  🔋✓  🔔✓││  │  │ [Static] [App] [+] [≈] │ │
║  │  ║                        ║  │  │  │                       ││  │  │                         │ │
║  │  ║  Listening             ║  │  │  │ All systems normal    ││  │  └─────────────────────────┘ │
║  │  ║  48 commands active    ║  │  │  │                       ││  │                              │
║  │  ║  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ║  │  │  └───────────────────────┘│  │  ┌─ Scrollable List ───────┐ │
║  │  ║  Last: "scroll down"   ║  │  │                           │  │  │                         │ │
║  │  ║        2s ago          ║  │  │  ── LAST HEARD ────────── │  │  │  Navigation (8)     [v] │ │
║  │  ║                        ║  │  │                           │  │  │  ┌─────────────────┐    │ │
║  │  ╚════════════════════════╝  │  │  ╔══ GlassCard LIGHT ═══╗│  │  │  │ ☑ go back       │    │ │
║  │                              │  │  ║                       ║│  │  │  │   back, prev    │    │ │
║  │  ╔══ GlassCard MEDIUM ════╗  │  │  ║  "scroll down"       ║│  │  │  │ ☑ scroll up     │    │ │
║  │  ║                        ║  │  │  ║                       ║│  │  │  │   swipe up      │    │ │
║  │  ║  ◉glow    WebAvanue   ║  │  │  ║  ← waveform viz →    ║│  │  │  │ ☑ go home       │    │ │
║  │  ║                        ║  │  │  ║                       ║│  │  │  │   home screen   │    │ │
║  │  ║  Voice browser         ║  │  │  ║  2 seconds ago       ║│  │  │  └─────────────────┘    │ │
║  │  ║  3 tabs open           ║  │  │  ║  Confidence: 0.94    ║│  │  │                         │ │
║  │  ║                        ║  │  │  ╚═══════════════════════╝│  │  │  Media (6)          [v] │ │
║  │  ╚════════════════════════╝  │  │                           │  │  │  System (10)        [v] │ │
║  │                              │  │                           │  │  │  VoiceOS (12)       [v] │ │
║  │  ╔══ GlassCard MEDIUM ════╗  │  │                           │  │  │  App Launch (5)     [v] │ │
║  │  ║                        ║  │  │                           │  │  │                         │ │
║  │  ║  ◉←pulse  VoiceCursor ║  │  │                           │  │  │  [+ Add Command]       │ │
║  │  ║                        ║  │  │                           │  │  │                         │ │
║  │  ║  Dwell: 1.5s          ║  │  │                           │  │  └─────────────────────────┘ │
║  │  ║  Smoothing: ON        ║  │  │                           │  │                              │
║  │  ║                        ║  │  │                           │  │  ← Only Column 3 scrolls    │
║  │  ╚════════════════════════╝  │  │                           │  │    (commands list only)      │
║  │                              │  │                           │  │                              │
║  └──────────────────────────────┘  └───────────────────────────┘  └──────────────────────────────┘
║                                                                                   ║
╚═══════════════════════════════════════════════════════════════════════════════════╝
```

### Landscape — Detailed Breakdown

```
Row(Modifier.fillMaxSize().padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small))
├── Column(weight = 1f, spacing = SpacingTokens.Small)
│   ├── Text("MODULES", style = LabelMedium, color = TextTertiary)
│   ├── GlassCard(GlassLevel.MEDIUM)  ← VoiceAvanue
│   │   ├── Row: PulseDot + Text("VoiceAvanue", TitleMedium) + StatusBadge("ACTIVE")
│   │   ├── Text("Listening · 48 commands", BodySmall, TextSecondary)
│   │   └── Text("Last: \"scroll down\" 2s ago", BodySmall, PrimaryLight)
│   ├── GlassCard(GlassLevel.MEDIUM)  ← WebAvanue
│   │   ├── Row: GlowDot + Text("WebAvanue", TitleMedium) + StatusBadge("READY")
│   │   └── Text("Voice browser · 3 tabs", BodySmall, TextSecondary)
│   └── GlassCard(GlassLevel.MEDIUM)  ← VoiceCursor
│       ├── Row: PulseDot + Text("VoiceCursor", TitleMedium) + StatusBadge("ACTIVE")
│       └── Text("Dwell: 1.5s · Smoothing: ON", BodySmall, TextSecondary)
│
├── Column(weight = 1f, spacing = SpacingTokens.Small)
│   ├── Text("SYSTEM", style = LabelMedium, color = TextTertiary)
│   ├── GlassSurface(GlassLevel.LIGHT)  ← System Health (collapsed row)
│   │   └── Row: [♿✓] [⬛✓] [🎤✓] [🔋✓] [🔔✓] Text("All systems normal")
│   ├── Spacer(SpacingTokens.Medium)
│   ├── Text("LAST HEARD", style = LabelMedium, color = TextTertiary)
│   └── GlassCard(GlassLevel.LIGHT)  ← Voice feedback card
│       ├── Text("\"scroll down\"", TitleMedium, Primary)
│       ├── WaveformVisualization (animated Canvas)
│       ├── Text("2 seconds ago", BodySmall, TextTertiary)
│       └── Text("Confidence: 0.94", BodySmall, Success)
│
└── Column(weight = 1.2f, spacing = SpacingTokens.Small)
    ├── Text("COMMANDS", style = LabelMedium, color = TextTertiary)
    ├── Row(tabs)  ← GlassChip tabs
    │   ├── GlassChip("Static", selected = true)
    │   ├── GlassChip("App")
    │   ├── GlassChip("+")      ← Custom
    │   └── GlassChip("≈")      ← Synonyms
    └── LazyColumn  ← ONLY scrollable element in landscape
        ├── CommandCategory("Navigation", 8, expanded = true)
        │   ├── CommandRow(checked = true, "go back", synonyms = "back, prev")
        │   ├── CommandRow(checked = true, "scroll up", synonyms = "swipe up")
        │   └── CommandRow(checked = true, "go home", synonyms = "home screen")
        ├── CommandCategory("Media", 6, expanded = false)
        ├── CommandCategory("System", 10, expanded = false)
        ├── CommandCategory("VoiceOS", 12, expanded = false)
        ├── CommandCategory("App Launch", 5, expanded = false)
        └── OceanButton("+ Add Command", glass = true)
```

**Key rule:** Columns 1 and 2 are FIXED height — no scrolling. Only Column 3 (commands list) scrolls vertically because it contains a variable-length list.

---

## LANDSCAPE — System Health Expanded (Permission Missing)

When any permission is missing, the system health section expands:

```
  ┌── Column 2 (weight 1f) ──┐
  │                           │
  │  SYSTEM                   │
  │                           │
  │  ╔══ GlassCard ERROR ═══╗│   ← OceanDesignTokens.State.error tint
  │  ║                       ║│     GlassMorphismConfig(tintColor = Error)
  │  ║  ♿ Accessibility  OFF ║│     Tap → system accessibility settings
  │  ║  Tap to enable    [>] ║│
  │  ║                       ║│
  │  ╚═══════════════════════╝│
  │                           │
  │  ┌─ GlassSurface LIGHT ─┐│   ← Others stay compact
  │  │  ⬛✓  🎤✓  🔋✓  🔔✓   ││
  │  └───────────────────────┘│
  │                           │
  │  ── LAST HEARD ────────── │
  │  ...                      │
  └───────────────────────────┘
```

---

## LANDSCAPE — Smart Glasses AR Mode (GlassAvanue)

When running on smart glasses with `AppContext.AR`:

```
╔═══════════════════════════════════════════════════════════════════════════════════╗
║                                                                                   ║
║  Background: TRANSPARENT (real world passthrough)                                 ║
║  Cards: GlassAvanue.forContext(AppContext.AR) — 65-75% opacity + 20-30px blur     ║
║  Touch targets: SizeTokens.MinTouchTargetSpatial (60dp)                           ║
║  Ambient: adaptToAmbientLight() adjusts card opacity                              ║
║                                                                                   ║
║  ╔═══ blur 75% ═══════════╗                      ╔═══ blur 75% ═══════════╗      ║
║  ║                        ║                      ║                        ║      ║
║  ║  ◉  VoiceAvanue  ON   ║   ♿✓ ⬛✓ 🎤✓ 🔋✓ 🔔✓  ║  Static (48)       [v] ║      ║
║  ║  48 cmds · listening   ║                      ║  ☑ go back             ║      ║
║  ║  Last: "scroll down"   ║   "scroll down"      ║    back, navigate back ║      ║
║  ║                        ║   ▓▓▒░░▒▓▓ (wave)   ║  ☑ scroll up           ║      ║
║  ╠════════════════════════╣   0.94 conf · 2s ago ║    swipe up, page up   ║      ║
║  ║                        ║                      ║  ☑ go home             ║      ║
║  ║  ◉  WebAvanue   READY ║                      ║    home, home screen   ║      ║
║  ║  3 tabs open           ║                      ║                        ║      ║
║  ║                        ║                      ║  Media (6)         [v] ║      ║
║  ╠════════════════════════╣                      ║  System (10)       [v] ║      ║
║  ║                        ║                      ║                        ║      ║
║  ║  ◉  VoiceCursor  ON   ║                      ║  [+ Add Command]      ║      ║
║  ║  Dwell 1.5s · Smooth  ║                      ║                        ║      ║
║  ║                        ║                      ║                        ║      ║
║  ╚════════════════════════╝                      ╚════════════════════════╝      ║
║                                                                                   ║
╚═══════════════════════════════════════════════════════════════════════════════════╝
```

**AR differences from phone landscape:**
- Background = fully transparent (passthrough)
- Card opacity higher (65-75% vs 8%) so text is readable against real world
- Touch targets 60dp instead of 48dp (dwell click needs bigger area)
- System health row is floating text (no card background)
- Center column content floats between the two card stacks
- No top app bar (saves vertical space on small glasses displays)

---

## PORTRAIT LAYOUT (Phone)

**Breakpoint:** width < `ResponsiveTokens.BreakpointSM` (600dp)
**Constraint:** Module section + system health = NO scroll. Commands below fold.
**Layout:** 2-section vertical: Top (fixed) + Bottom (scrollable commands)

```
╔═══════════════════════════════╗
║                               ║
║  ┌─ TopAppBar (48dp) ──────┐ ║
║  │  Avanues             [⚙] │ ║
║  └──────────────────────────┘ ║
║                               ║
║  Background: OceanTheme.background
║  Padding: SpacingTokens.Medium (16dp)
║                               ║
║  ╔══ GlassCard MEDIUM ═════╗ ║   ← VoiceAvanue
║  ║                          ║ ║
║  ║  ◉←pulse  VoiceAvanue   ║ ║   Row: PulseDot + name + badge
║  ║           ┌──────┐      ║ ║
║  ║  Listening│ACTIVE│      ║ ║   GlassChip status badge
║  ║  48 cmds  └──────┘      ║ ║
║  ║  Last: "scroll down" 2s ║ ║   BodySmall, PrimaryLight
║  ║                          ║ ║
║  ╚══════════════════════════╝ ║
║                               ║
║  ┌──────────────┬────────────┐║   ← 2-column row for smaller cards
║  │╔═ Glass M ══╗│╔═ Glass M ══╗   WebAvanue + VoiceCursor side by side
║  │║            ║│║            ║║
║  │║ ◉ WebA    ║│║ ◉ VoiceC  ║║
║  │║   READY   ║│║   ACTIVE  ║║
║  │║ 3 tabs    ║│║ Dwell 1.5s║║
║  │║            ║│║            ║║
║  │╚════════════╝│╚════════════╝
║  └──────────────┴────────────┘║
║                               ║
║  ┌─ System Health ──────────┐ ║   ← GlassSurface LIGHT (collapsed)
║  │ ♿✓  ⬛✓  🎤✓  🔋✓  🔔✓    │ ║     Single line when all OK
║  │ All systems normal       │ ║     Expands if any missing
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ Last Heard ─────────────┐ ║   ← GlassCard LIGHT
║  │ "scroll down"            │ ║     Shows waveform + confidence
║  │ ▓▓▒░░▒▓▓  0.94  2s ago  │ ║
║  └──────────────────────────┘ ║
║                               ║   ← Everything above is FIXED (no scroll)
║ ════════════════════════════  ║   ← Visual divider
║                               ║
║  ── COMMANDS ─────────────── ║   ← Scrollable section starts here
║                               ║
║  [Static] [App] [+] [≈]     ║   ← GlassChip tab row
║                               ║
║  Navigation (8)          [v] ║   ← Expandable categories
║  ┌──────────────────────────┐ ║
║  │ ☑ go back               │ ║
║  │   back, navigate back,  │ ║
║  │   previous screen       │ ║
║  │                          │ ║
║  │ ☑ scroll up             │ ║
║  │   swipe up, page up     │ ║
║  │                          │ ║
║  │ ☑ scroll down           │ ║
║  │   swipe down, page down │ ║
║  │                          │ ║
║  │ ☑ go home               │ ║
║  │   home, home screen     │ ║
║  └──────────────────────────┘ ║
║                               ║
║  Media (6)               [v] ║
║  System (10)             [v] ║
║  VoiceOS Control (12)    [v] ║
║  App Launch (5)          [v] ║
║  Accessibility (7)       [v] ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │     + Add Command        │ ║   ← OceanButton(glass = true)
║  └──────────────────────────┘ ║
║                               ║
╚═══════════════════════════════╝
```

### Portrait — Detailed Breakdown

```
Column(Modifier.fillMaxSize())
├── TopAppBar(height = AppBarHeightCompact)
│
├── Column(Modifier.weight(0f, fill = false))  ← Fixed top section
│   │                                            (intrinsic height, no scroll)
│   ├── Text("MODULES", LabelMedium, TextTertiary, padding bottom = xs)
│   │
│   ├── GlassCard(GlassLevel.MEDIUM)  ← VoiceAvanue (full width, prominent)
│   │   ├── Row: PulseDot + "VoiceAvanue" (TitleMedium) + StatusBadge
│   │   ├── Text("Listening · 48 commands", BodySmall, TextSecondary)
│   │   └── Text("Last: \"scroll down\" 2s ago", BodySmall, PrimaryLight)
│   │
│   ├── Row(spacing = SpacingTokens.Small)  ← WebAvanue + VoiceCursor side-by-side
│   │   ├── GlassCard(weight 1f, GlassLevel.MEDIUM)  ← WebAvanue (compact)
│   │   │   ├── Row: GlowDot + "WebA" + StatusBadge("READY")
│   │   │   └── Text("3 tabs", BodySmall)
│   │   └── GlassCard(weight 1f, GlassLevel.MEDIUM)  ← VoiceCursor (compact)
│   │       ├── Row: PulseDot + "VoiceC" + StatusBadge("ACTIVE")
│   │       └── Text("Dwell 1.5s", BodySmall)
│   │
│   ├── GlassSurface(GlassLevel.LIGHT)  ← System Health
│   │   └── Row: permission icons + "All systems normal"
│   │
│   └── GlassCard(GlassLevel.LIGHT)  ← Last Heard
│       └── Row: text + waveform + confidence + time
│
└── LazyColumn(Modifier.weight(1f))  ← Scrollable commands section
    ├── Text("COMMANDS", LabelMedium, TextTertiary)
    ├── Row(GlassChip tabs): [Static] [App] [+] [≈]
    ├── CommandCategory("Navigation", 8, expanded)
    ├── CommandCategory("Media", 6, collapsed)
    ├── CommandCategory("System", 10, collapsed)
    ├── CommandCategory("VoiceOS Control", 12, collapsed)
    ├── CommandCategory("App Launch", 5, collapsed)
    ├── CommandCategory("Accessibility", 7, collapsed)
    └── OceanButton("+ Add Command", glass = true)
```

**Portrait key rules:**
- VoiceAvanue gets a full-width prominent card (it's the primary module)
- WebAvanue + VoiceCursor share a row (50/50 width) to save vertical space
- System health is always 1 line when all green
- Last Heard is a compact single-line card
- Everything above the divider is FIXED — no scroll
- Only the Commands section below scrolls

---

## PORTRAIT — System Health Expanded

When accessibility is OFF:

```
║  ┌─ System Health ──────────┐ ║
║  │                          │ ║
║  │  ╔══ Error Card ═══════╗ │ ║   ← GlassMorphismConfig(tintColor = Error)
║  │  ║ ♿ Accessibility OFF ║ │ ║     animateContentSize()
║  │  ║ Tap to enable   [>] ║ │ ║     onClick → system settings
║  │  ╚═════════════════════╝ │ ║
║  │                          │ ║
║  │  ⬛✓  🎤✓  🔋✓  🔔✓       │ ║   ← Others stay compact
║  │                          │ ║
║  └──────────────────────────┘ ║
```

---

## PORTRAIT — Command Tabs Content

### Tab: "App" (By App — Dynamic Commands)

```
║  [Static] [*App*] [+] [≈]   ║
║                               ║
║  Currently: Chrome           ║   ← Foreground app detected
║                               ║
║  ┌──────────────────────────┐ ║
║  │ "Search bar"     → tap   │ ║   ← Dynamic commands from
║  │ "New tab"        → tap   │ ║     screen scan
║  │ "1"              → tap   │ ║
║  │ "2"              → tap   │ ║
║  │ "Back"        → navigate │ ║
║  │ "Bookmarks"      → tap   │ ║
║  └──────────────────────────┘ ║
║                               ║
║  Recent: [Gmail] [Maps] [YT] ║   ← GlassChip for each app
║                               ║
```

### Tab: "+" (Custom Commands)

```
║  [Static] [App] [*+*] [≈]   ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │ "lights on"              │ ║   ← User-defined command
║  │  → custom action         │ ║
║  │  phrases: lights on,     │ ║     Multiple trigger phrases
║  │  turn on lights,         │ ║
║  │  illumination            │ ║
║  │                   [edit] │ ║     ← GlassIconButton
║  └──────────────────────────┘ ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │  + New Custom Command    │ ║   ← OceanButton(glass = true)
║  └──────────────────────────┘ ║
║                               ║
```

### Tab: "≈" (Synonyms)

```
║  [Static] [App] [+] [*≈*]   ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │ click                    │ ║   ← Canonical (TitleSmall, Primary)
║  │ ↔ tap, press, push,    │ ║     Synonyms (BodySmall, TextTertiary)
║  │   hit, select            │ ║
║  │            [+ add] [✎]  │ ║     Add user synonym / edit
║  └──────────────────────────┘ ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │ scroll                   │ ║
║  │ ↔ swipe, drag, move    │ ║
║  │            [+ add] [✎]  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │ open                     │ ║
║  │ ↔ launch, start, go to,│ ║
║  │   run                    │ ║
║  │            [+ add] [✎]  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌──────────────────────────┐ ║
║  │  + New Synonym Group     │ ║
║  └──────────────────────────┘ ║
║                               ║
```

---

## Pulse Animation Spec (AvanueUI Token-Based)

```kotlin
// PulseDot.kt — Reusable composable

@Composable
fun PulseDot(
    state: ServiceState,
    modifier: Modifier = Modifier,
    size: Dp = SizeTokens.IconSmall  // 16dp
) {
    val color = when (state) {
        ServiceState.Running -> OceanDesignTokens.State.success  // #10B981
        ServiceState.Stopped -> OceanDesignTokens.Text.disabled  // #64748B
        ServiceState.Error   -> OceanDesignTokens.State.error    // #EF4444
        ServiceState.Ready   -> OceanDesignTokens.State.info     // #3B82F6
    }

    // Pulse rings (only when Running)
    val infiniteTransition = rememberInfiniteTransition()

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),  // AnimationTokens.DurationExtraLong * 2
            repeatMode = RepeatMode.Restart
        )
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier.size(size * 3)) {
        if (state == ServiceState.Running) {
            // Ring 1
            drawCircle(color.copy(alpha = ring1Alpha), radius = (size / 2).toPx() * ring1Scale)
            // Ring 2 (staggered by 700ms via delayMillis)
            // Ring 3 (staggered by 1400ms)
        }
        // Solid center dot
        drawCircle(color, radius = (size / 2).toPx())
    }
}
```

---

## Responsive Layout Switch

```kotlin
@Composable
fun AvanuesDashboard(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanTheme.background)  // Token-based background
    ) {
        val isLandscape = maxWidth > maxHeight
        val isWide = maxWidth >= ResponsiveTokens.BreakpointSM  // 600dp

        if (isLandscape || isWide) {
            DashboardLandscape(uiState)  // 3-column, no scroll
        } else {
            DashboardPortrait(uiState)   // Stacked, commands scroll
        }
    }
}
```

---

## Theme Swappability

Because every value references a token, switching themes changes the entire look:

| Token | OceanTheme | iOS26LiquidGlass | Windows11Fluent2 |
|-------|-----------|-------------------|-------------------|
| Background | #0F172A (deep navy) | SystemBackground | #F3F3F3 |
| Primary | #3B82F6 (coral blue) | TintColor | #0078D4 |
| GlassLevel | 5-12% opacity | 40-60% (liquid glass) | 80% (acrylic) |
| CardShape | 16dp rounded | 20dp continuous | 8dp rounded |
| PulseDot | Concentric rings | Liquid ripple | Subtle glow |
| Typography | System default | SF Pro | Segoe UI |

The layout code (`DashboardLandscape`, `DashboardPortrait`) stays identical.
Only the theme provider changes.

---

## Implementation Phases

| Phase | What | Tokens Used |
|-------|------|-------------|
| 1 | ServiceStateProvider interface + flows | N/A (data layer) |
| 2 | DashboardViewModel + UiState | N/A (logic layer) |
| 3 | PulseDot composable | State colors, Animation, SizeTokens |
| 4 | DashboardLandscape layout | All spacing, glass, responsive tokens |
| 5 | DashboardPortrait layout | Same tokens, different arrangement |
| 6 | System Health (collapse/expand) | State colors, Animation, Glass |
| 7 | Command tabs + lists | Typography, Glass, Shape tokens |
| 8 | Synonym editor | Typography, Glass, interactive tokens |
| 9 | AR mode adaptation | GlassAvanue, SpatialSizeTokens |
| 10 | Theme swap verification | All tokens tested with 3 themes |

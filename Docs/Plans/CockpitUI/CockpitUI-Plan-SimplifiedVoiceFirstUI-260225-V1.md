# Avanues Simplified UI — Three Minimalist Voice-First Variations

## Context

The current Avanues app and Cockpit layer suffer from **cognitive overload**:
- **15 layout modes** exposed to users at once (most never used)
- **13-state CommandBar** with 3+ depth hierarchy — hard to discover and navigate
- **Dashboard** showing 10+ module tiles + recent sessions + templates simultaneously
- **No progressive disclosure** — everything shown at once
- Smart glasses users get truncated UI (5-button limit) with no alternative

**Goal**: Design three new simplified UI variations that are **voice-first**, reduce cognitive load, remain **full-featured** (no feature cuts), and work across **phone / tablet / desktop / smart glasses** using the existing AvanueUI cross-platform component system.

**Constraint**: All implementations use `AvanueTheme`, unified components (`AvanueCard`, `AvanueButton`, `AvanueChip`, `AvanueSurface`, `AvanueFAB`), Glass/Water effects, and DisplayProfile responsive scaling. No new platform-specific code needed.

---

## Design Principles (All Three Variations)

1. **Voice is THE primary input** — UI exists to support voice, not replace it
2. **Progressive disclosure** — Show only what's needed, reveal on demand
3. **Context over chrome** — Content fills the screen; navigation is invisible until summoned
4. **Calm technology** — Information moves from periphery to center only when relevant
5. **One action, one screen** — Each screen has ONE clear purpose
6. **Glanceable** — Smart glasses users can understand state in <2 seconds

---

## Variation A: "AvanueViews" — Ambient Stream

### Philosophy
*"The UI whispers to you — cards surface when relevant, fade when done."*

Inspired by: Google Now cards, Alexa Show ambient mode, Rabbit R1 card stack, Calm Technology principles.

### Home Screen: The Stream
```
┌─────────────────────────────┐
│  ○ AVA                9:41  │  ← Minimal status bar (voice orb + time)
│                             │
│  ┌─────────────────────┐    │
│  │ 📝 Meeting Notes    │    │  ← Active context card (what you're working on)
│  │ "Resume editing"    │    │     Auto-surfaced based on time/location/history
│  │ Last edited 2m ago  │    │
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ 🌐 3 tabs open      │    │  ← Ambient awareness card
│  │ "Open browser"      │    │     Shows state, not actions
│  └─────────────────────┘    │
│                             │
│  ┌─────────────────────┐    │
│  │ + "Open [module]"   │    │  ← Ghost card: voice hint
│  │   or tap to browse  │    │     Teaches voice commands naturally
│  └─────────────────────┘    │
│                             │
│         ┌───────┐           │
│         │  🎤   │           │  ← Voice FAB (always present)
│         └───────┘           │     Tap = listen, Long-press = push-to-talk
└─────────────────────────────┘
```

### Phone Landscape
```
┌──────────────────────────────────────────────────────────────┐
│  ○ AVA              9:41                                     │
│                                                              │
│  ┌─────────────────────┐    ┌─────────────────────┐         │
│  │ 📝 Meeting Notes    │    │ 🌐 3 tabs open      │         │
│  │ "Resume editing"    │    │ "Open browser"      │         │
│  │ Last edited 2m ago  │    │                     │    🎤   │
│  └─────────────────────┘    └─────────────────────┘         │
│                                                              │
│  ┌─────────────────────┐    ┌─────────────────────┐         │
│  │ 📷 Cast connected   │    │ + "Open [module]"   │         │
│  │ "Show cast"         │    │   or tap to browse  │         │
│  └─────────────────────┘    └─────────────────────┘         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```
Two-column card grid. Voice FAB docks to the right edge mid-screen. More cards visible = better ambient awareness. No scroll needed for typical 4-card state.

### Tablet Landscape
```
┌───────────────────────────────────────────────────────────────────────────────┐
│  ○ AVA                                                              9:41 AM  │
│                                                                              │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌──────────────────┐  │
│  │ 📝 Meeting Notes      │  │ 🌐 3 tabs open        │  │ 📷 Cast active   │  │
│  │ "Resume editing"      │  │ "Open browser"        │  │ "Show cast"      │  │
│  │ Last edited 2m ago    │  │ news.site + 2 more    │  │ Pixel → Display  │  │
│  │                       │  │                       │  │                  │  │
│  └───────────────────────┘  └───────────────────────┘  └──────────────────┘  │
│                                                                              │
│  ┌───────────────────────┐  ┌───────────────────────┐                        │
│  │ AI: 3 action items    │  │ + "Open [module]"     │                   🎤   │
│  │ from today's meeting  │  │   or tap to browse    │                        │
│  └───────────────────────┘  └───────────────────────┘                        │
│                                                                              │
└───────────────────────────────────────────────────────────────────────────────┘
```
Three-column layout. Cards are wider with richer detail (tab names, cast target). Generous whitespace. Voice FAB bottom-right. Up to 6 cards visible without scroll.

### Desktop Landscape
```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  ○ AVA                                                                         9:41 AM  │
│                                                                                         │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐                 │
│  │ 📝 Meeting Notes   │  │ 🌐 3 tabs open     │  │ 📷 Cast active     │                 │
│  │ "Resume editing"   │  │ "Open browser"     │  │ "Show cast"        │       Optional  │
│  │ Last edited 2m ago │  │                    │  │ Pixel → Display    │       sidebar:  │
│  └────────────────────┘  └────────────────────┘  └────────────────────┘       ┌───────┐ │
│                                                                               │Recent │ │
│  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐       │Session│ │
│  │ AI: Action items   │  │ 📄 PDF: Report.pdf │  │ + "Open [module]"  │       │  s    │ │
│  │ from meeting       │  │ "Open PDF"         │  │   or tap to browse │       │       │ │
│  └────────────────────┘  └────────────────────┘  └────────────────────┘       └───────┘ │
│                                                                                    🎤   │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```
Three columns + optional right sidebar for session history. Keyboard shortcut hints appear on hover. Max density without clutter — 8 cards visible.

### Smart Glasses (Monocular HUD)
```
┌────────────────────────────────┐
│  📝 Meeting Notes              │  ← Single card, full width
│  "Resume editing"              │     Swipe right = next card
│  Last edited 2m ago            │     Voice: "next" / "open"
│                                │
│   ● ○ ○ ○                      │  ← Dot indicator (4 cards total)
└────────────────────────────────┘
```
One card at a time. Voice is the only interaction. Dot indicators show card count. "Next" and "previous" cycle. Maximum glanceability.

### AvanueViews — Active Module View (Landscape)
When a module is open, the whisper bar replaces the stream:
```
┌──────────────────────────────────────────────────────────────┐
│  ○ AVA  Meeting Notes                                  9:41  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                                                        │  │
│  │              [ Note content fills screen ]             │  │
│  │                                                        │  │
│  │                                                        │  │
│  └────────────────────────────────────────────────────────┘  │
│  [Bold] [Italic] [Undo] [Save] [··· More]              🎤  │
└──────────────────────────────────────────────────────────────┘
```
Content fills all available space. AvanueViews bar is a thin 48dp strip at the bottom. Zero chrome distraction. "More" opens searchable action sheet.

### AvanueViews — Compare Mode (Landscape)
```
┌──────────────────────────────────────────────────────────────┐
│  ○ AVA  Compare: Notes + PDF                           9:41  │
│                                                              │
│  ┌──────────────────────┐ ┌──────────────────────────────┐  │
│  │                      │ │                              │  │
│  │    Meeting Notes     │ │       Report.pdf             │  │
│  │    (rich editor)     │ │       (PDF viewer)           │  │
│  │                      │ │                              │  │
│  └──────────────────────┘ └──────────────────────────────┘  │
│  [Bold] [Italic] [Save]  │  [Prev] [Next] [Zoom]     🎤   │
└──────────────────────────────────────────────────────────────┘
```
Split view with per-frame contextual actions in the whisper bar. Bar splits to show relevant actions for each frame. Focused frame's actions are brighter.

---

### Key Design Decisions

**Card Priority System** (replaces 10-tile grid):
- **P0 — Active Context** (1 card max): What you're currently working on. Auto-detected from last session, calendar, or explicit "I'm working on X"
- **P1 — Ambient Awareness** (2-3 cards): Passive state indicators (open tabs, recording status, cast connection). Low visual weight
- **P2 — Suggestions** (1-2 cards): AI-suggested next actions based on patterns. Dismissable
- **P3 — Ghost Hints** (1 card): Voice command discovery. Rotates through available commands

**Module Access** — Say the name or swipe up:
- Voice: "Open notes" / "Open browser" / "Take a photo"
- Touch: Swipe up on stream → module grid appears (3x3 compact, no labels — icons only with voice hints on hold)
- The module grid is a **bottom sheet**, not a screen — you never leave the stream

**Multi-Window (Cockpit Integration)**:
- Voice: "Add PDF beside notes" → auto-selects best 2-pane layout
- The 15 layout modes collapse to **4 user intents**:
  - "Focus" → FULLSCREEN (1 frame)
  - "Compare" → SPLIT_LEFT or SPLIT_RIGHT (2 frames)
  - "Overview" → GRID or MOSAIC (3-6 frames, auto-arranged)
  - "Present" → CAROUSEL or TRIPTYCH (showcase mode)
- System auto-selects the specific LayoutMode from intent + frame count + display size
- Users never see "SPATIAL_DICE" or "T_PANEL" — those are implementation details

**CommandBar Replacement — Contextual AvanueViews Bar**:
```
┌─────────────────────────────┐
│ [Bold] [Italic] [Save]  ... │  ← Only shows actions for active content
└─────────────────────────────┘
```
- Flat (1 level only, no state machine depth)
- Shows top 5 most-used actions for the focused content type
- "More" chip → bottom sheet with full action list (grouped, searchable)
- Voice: all actions available by name without navigating the bar

**Voice Command Surfacing**:
- Each card shows its voice trigger as ghost text ("say: resume editing")
- Voice FAB pulses subtly when listening
- After command execution: brief confirmation toast, no modal
- "Help" → overlay showing 5 most relevant commands for current context

**Responsive Adaptation**:
| Device | Stream Layout | Cards Visible | Voice FAB |
|--------|--------------|---------------|-----------|
| Phone (portrait) | Single column, full-width cards | 2-3 | Bottom center |
| Phone (landscape) | Two columns | 3-4 | Bottom right |
| Tablet | Two columns, wider cards | 4-6 | Bottom right |
| Desktop | Three columns, sidebar optional | 6-8 | Bottom right |
| Smart Glasses | Single card at a time, swipe/voice to cycle | 1 | Voice-only (no FAB) |

**Cognitive Load Reduction**:
- Current: 10+ tiles + sessions + templates + command bar = ~25 interactive elements on home
- AvanueViews: 3-5 cards + 1 FAB = **6 max interactive elements on home**
- 80% reduction in visual decision points

---

## Variation B: "Lens" — Command Palette Focus

### Philosophy
*"Everything is one voice command or one keystroke away. The UI is a lens — it focuses on exactly what you ask for."*

Inspired by: macOS Spotlight, Raycast, Superhuman email, Linear app, VS Code Command Palette.

### Home Screen: The Void + Lens
```
┌─────────────────────────────┐
│                             │
│                             │
│                             │
│      ┌─────────────────┐    │
│      │ 🔍 What next?   │    │  ← Lens bar (always centered)
│      │                 │    │     Voice-activated or tap to type
│      └─────────────────┘    │
│                             │
│   Open Notes  ·  Browse  ·  │  ← 3 most-recent items (ghost text)
│   Resume PDF               │     Minimal, learns from usage
│                             │
│                             │
│                    🎤       │  ← Voice indicator (bottom-right)
│                             │
└─────────────────────────────┘
```

The screen is **intentionally empty**. The Lens bar is the single entry point for everything.

### Phone Landscape
```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│                  ┌──────────────────────────┐                │
│                  │ 🔍 What next?            │                │
│                  └──────────────────────────┘                │
│                                                              │
│         Open Notes  ·  Browse  ·  Resume PDF                 │
│                                                         🎤   │
└──────────────────────────────────────────────────────────────┘
```
Lens centered horizontally. Extra width = wider Lens bar (50% of screen). Recent items spread comfortably. The emptiness is intentional — it's calming.

### Phone Landscape — Lens Active
```
┌──────────────────────────────────────────────────────────────┐
│          ┌──────────────────────────┐                        │
│          │ 🔍 "note"               │                        │
│          ├──────────────────────────┤                        │
│          │ MODULES                  │    ┌────────────────┐  │
│          │ 📝 Open NoteAvanue      │    │   Preview:     │  │
│          │ 📝 New Note             │    │   Meeting Notes│  │
│          │                          │    │   Last: 2m ago │  │
│          │ RECENT                   │    │                │  │
│          │ 📝 Meeting Notes (2m)   │    │   [3 paragraphs│  │
│          │ 📝 Project Ideas (1d)   │    │    visible]    │  │
│          └──────────────────────────┘    └────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```
Landscape bonus: **result preview pane** appears to the right of the results list. Hovering/focusing a result shows a preview — making Lens more powerful on wider screens.

### Tablet Landscape
```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│                                                                              │
│                                                                              │
│                   ┌─────────────────────────────────┐                        │
│                   │ 🔍 What next?                   │                        │
│                   └─────────────────────────────────┘                        │
│                                                                              │
│              Open Notes  ·  Browse  ·  Resume PDF  ·  Cast                   │
│                                                                              │
│                                                                              │
│                                                                         🎤   │
└───────────────────────────────────────────────────────────────────────────────┘
```
60% width Lens bar. More recent items visible (4-5 ghost hints). Spacious, meditative.

### Desktop Landscape
```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                         │
│                                                                                         │
│                                                                                         │
│                         ┌──────────────────────────────────┐                            │
│                         │ 🔍 What next?              ⌘K   │                            │
│                         └──────────────────────────────────┘                            │
│                                                                                         │
│                  Open Notes  ·  Browse  ·  Resume PDF  ·  Cast  ·  Settings              │
│                                                                                         │
│                                                                                    🎤   │
│                                                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```
50% width Lens bar. Shows keyboard shortcut hint (Cmd+K). 5 ghost items. Desktop users can type instantly; voice users speak naturally. Both converge on the same Lens.

### Desktop Landscape — Multi-Window Active
```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│            ┌──────────────────────────────────┐                                         │
│            │ 🔍 Notes: Bold, Save, Undo...   │                                         │
│            │  [📝 Notes] [📄 PDF] [🌐 Web+]  │  ← Frame pills                          │
│            └──────────────────────────────────┘                                         │
│                                                                                         │
│  ┌─────────────────────────────────┐  ┌───────────────────────────────────────────────┐ │
│  │                                 │  │                                               │ │
│  │         Meeting Notes           │  │              Report.pdf                       │ │
│  │         (rich editor)           │  │              (PDF viewer)                     │ │
│  │                                 │  │                                               │ │
│  │                                 │  │                                               │ │
│  └─────────────────────────────────┘  └───────────────────────────────────────────────┘ │
│                                                                                         │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```
Lens bar stays at top, now shows context-aware ghost text for focused frame. Frame pills below it for switching. Content fills everything below. Zero wasted space. Type an action name in the Lens to execute it. Voice works identically.

### Smart Glasses
```
┌────────────────────────────────┐
│  🔍 "Say a command..."        │  ← Minimal Lens hint (top strip)
│                                │
│       [ Content Area ]         │  ← Module content fills screen
│                                │
│   ● Meeting Notes              │  ← Current context label
└────────────────────────────────┘
```
Glasses show a thin Lens hint at top. Voice is the only way to activate it. "Help" lists commands audibly. Content maximized for the tiny display.

---

### Key Design Decisions

**The Lens Bar** (replaces Dashboard + CommandBar + Navigation):
- Always visible, always centered
- Activated by: voice (wake word), tap, or keyboard shortcut (Cmd+K / Ctrl+K on desktop)
- Fuzzy-matches across: module names, voice commands, recent files, session names, settings
- Results grouped by category with keyboard/voice navigation

**Lens Results Panel** (appears below bar on activation):
```
┌─────────────────────────────┐
│  🔍 "note"                  │
│  ─────────────────────────  │
│  MODULES                    │
│  📝 Open NoteAvanue         │  ← Module launch
│  📝 New Note                │  ← Quick action
│                             │
│  RECENT                     │
│  📝 Meeting Notes (2m ago)  │  ← Resume session
│  📝 Project Ideas (1d ago)  │
│                             │
│  COMMANDS                   │
│  🎤 "Bold" · "Save" · ...  │  ← Available voice commands
│                             │
│  SETTINGS                   │
│  ⚙️ Note auto-save interval │  ← Deep settings access
│  ─────────────────────────  │
│  ↵ Enter to select · ↑↓ nav│
└─────────────────────────────┘
```

**Module Access**:
- Voice: "Open notes" → instant launch (no Lens UI needed)
- Lens: Type or say "note" → see modules, recent files, commands, settings
- Zero-query state: Shows 5 most-recent items + 3 suggested actions
- The Lens is a **universal command palette** — it replaces: module grid, command bar, settings search, file browser, session manager

**Multi-Window (Cockpit Integration)**:
- Voice: "Split notes and PDF" → 2-pane layout auto-created
- Lens: Type "layout" → see 4 arrangement intents (Focus/Compare/Overview/Present)
- Active frames shown as pills below the Lens bar when in multi-window mode:
```
┌─────────────────────────────┐
│      🔍 What next?          │
│  [📝 Notes] [📄 PDF] [🌐+] │  ← Active frame pills (tap to focus)
└─────────────────────────────┘
```
- Same 4-intent layout collapsing as AvanueViews (Focus/Compare/Overview/Present)

**Context-Aware Actions** (replaces CommandBar state machine):
- When a frame is focused, the Lens bar shows a context hint:
  - `🔍 Notes: Bold, Save, Undo...` (ghost text showing top actions)
- Typing or speaking an action name immediately executes it
- No separate CommandBar — the Lens IS the command interface
- "?" or "help" in Lens → shows all commands for current context

**Voice Command Surfacing**:
- Lens bar ghost text rotates through contextual commands
- Voice indicator shows listening state (pulsing circle)
- After voice command: result appears in Lens bar briefly ("Saved ✓"), then fades
- Power users can chain: "Open notes, bold, type Meeting Agenda" (sequential execution)

**Responsive Adaptation**:
| Device | Lens Position | Results Panel | Frame Pills |
|--------|--------------|---------------|-------------|
| Phone | Top 20% of screen | Full-width dropdown | Bottom bar |
| Tablet | Center, 60% width | Below Lens, 60% width | Below Lens |
| Desktop | Center, 50% width | Below Lens, 50% width | Below Lens |
| Smart Glasses | Voice-only (no visual Lens) | Audio results + numbered overlay | Voice: "frame 1" |

**Cognitive Load Reduction**:
- Current: Multiple navigation paths (tiles, command bar, menus, overlays)
- Lens: **ONE entry point** for everything — if you can name it, you can do it
- Zero learning curve: "type what you want" is universally understood
- Empty home screen = zero cognitive load at rest

---

## Variation C: "Canvas" — Spatial Zen

### Philosophy
*"Your workspace is an infinite calm canvas. Content exists in space. You navigate by looking, not by clicking through menus."*

Inspired by: Apple Vision Pro spatial computing, Figma infinite canvas, Bear notes simplicity, Focus/Zen modes, Miro collaborative boards.

### Home Screen: The Zen Canvas
```
┌─────────────────────────────────────┐
│                                     │
│        ┌──────┐    ┌──────┐         │
│        │  📝  │    │  🌐  │         │  ← Content islands (floating cards)
│        │Notes │    │ Web  │         │     Arranged spatially by recency
│        └──────┘    └──────┘         │     or user grouping
│                                     │
│              ┌──────┐               │
│              │  📷  │               │
│              │Photo │               │
│              └──────┘               │
│                                     │
│   ·  ·  ·  ·  ·  ·  ·  ·  ·  ·    │  ← Dot grid (subtle, shows canvas)
│                                     │
│                          [🎤] [+]   │  ← Voice + Add (minimal controls)
│                                     │
│   ◀ ━━━━━━━━━━━●━━━━━━━━ ▶  🔍    │  ← Zoom rail (bottom)
└─────────────────────────────────────┘
```

### Phone Landscape
```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│     ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐           │
│     │  📝  │    │  🌐  │    │  📄  │    │  📷  │           │
│     │Notes │    │ Web  │    │ PDF  │    │Photo │           │
│     └──────┘    └──────┘    └──────┘    └──────┘           │
│                                                              │
│  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·   │
│                                                         🎤   │
│  ◀ ━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━ ▶             🔍    │
└──────────────────────────────────────────────────────────────┘
```
Landscape shows more islands horizontally — a natural panoramic view. The horizontal space lets users spread modules across the canvas. Pinch to zoom or voice: "Focus on notes".

### Phone Landscape — Zoomed In (Level 3: Focus)
```
┌──────────────────────────────────────────────────────────────┐
│  ← Notes                                              9:41  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                                                        │  │
│  │              [ Note content fills screen ]             │  │
│  │                                                        │  │
│  │           [Undo]                [Italic]               │  │
│  │       [Save]   📝   [Bold]                             │  │
│  │           [Share]               [More]                 │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```
At focus zoom, the orbit ring appears around the content center. Actions form a semicircle reachable by thumb. Ring auto-hides after 3s for immersive editing. Voice or tap the content area to show ring again.

### Tablet Landscape
```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│            ┌──────┐         ┌──────┐         ┌──────┐                       │
│            │  📝  │         │  🌐  │         │  📄  │                       │
│            │Notes │         │ Web  │         │ PDF  │                       │
│            └──────┘         └──────┘         └──────┘                       │
│                                                                              │
│                   ┌──────┐         ┌──────┐                                  │
│                   │  📷  │         │  🎬  │                                  │
│                   │Photo │         │Video │                                  │
│                   └──────┘         └──────┘                                  │
│                                                                              │
│  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·   │
│                                                                         🎤   │
│  ◀ ━━━━━━━━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━━━━ ▶              🔍    │
└───────────────────────────────────────────────────────────────────────────────┘
```
Spacious canvas. Islands arranged in an organic staggered grid (not rigid). Frequently-used modules gravitate to center. Pen support for drag-rearranging. Dot grid creates depth perception.

### Tablet Landscape — Cluster View (Level 2: Compare)
```
┌───────────────────────────────────────────────────────────────────────────────┐
│  ← Overview                                                            9:41  │
│                                                                              │
│     ┌─────────────────────────────┐    ┌─────────────────────────────────┐   │
│     │                             │    │                                 │   │
│     │       Meeting Notes         │    │         Report.pdf              │   │
│     │       (rich editor)         │    │         (PDF viewer)            │   │
│     │                             │    │                                 │   │
│     │                             │    │                                 │   │
│     │                             │    │                                 │   │
│     └─────────────────────────────┘    └─────────────────────────────────┘   │
│                                                                              │
│  [Bold] [Save]  ·  [Prev Page] [Next Page]  ·  [Zoom Out]              🎤   │
└───────────────────────────────────────────────────────────────────────────────┘
```
When two islands are near each other, zooming in shows them as a split cluster. Contextual actions show for both frames, separated by a dot divider. The canvas zoom metaphor means "back" = "zoom out".

### Desktop Landscape
```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                         │
│        ┌──────┐              ┌──────┐              ┌──────┐              ┌──────┐       │
│        │  📝  │              │  🌐  │              │  📄  │              │  📷  │       │
│        │Notes │              │ Web  │              │ PDF  │              │Photo │       │
│        └──────┘              └──────┘              └──────┘              └──────┘       │
│                                                                                         │
│                ┌──────┐              ┌──────┐              ┌──────┐                     │
│                │  🎬  │              │  ✏️  │              │  📡  │                     │
│                │Video │              │ Draw │              │ Cast │                     │
│                └──────┘              └──────┘              └──────┘                     │
│                                                                                         │
│                         ┌──────┐              ┌──────┐                                  │
│                         │  🤖  │              │  📊  │                                  │
│                         │  AI  │              │Widget│                                  │
│                         └──────┘              └──────┘                                  │
│                                                                                         │
│  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  ·  │
│                                                                                    🎤   │
│  ◀ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ▶           🔍    │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```
Full bird's-eye view. All 10+ module islands visible. Organic triangular layout (not grid). Mouse wheel zoom, click-drag pan. Double-click an island to focus. Cmd+- and Cmd+= for zoom. Scroll wheel zoom feels like navigating a map.

### Desktop Landscape — Deep Focus (Level 3+)
```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│  ← Zoom Out (Esc)                     Meeting Notes                              9:41   │
│                                                                                         │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                                                                   │  │
│  │                                                                                   │  │
│  │                          [ Full immersive note editor ]                           │  │
│  │                                                                                   │  │
│  │                                                                                   │  │
│  │                                                                                   │  │
│  │                                                                                   │  │
│  │                                                                                   │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│          [Undo] [Bold] [Italic] [Strike] [Save] [Share] [··· More]                 🎤  │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```
At Level 3+, content fills the entire viewport. Orbit ring linearizes to a bottom bar on desktop (radial doesn't suit keyboard users). Esc or "zoom out" returns to overview. Zero chrome — just content and actions.

### Smart Glasses (Head-Tracked Canvas)
```
┌────────────────────────────────┐
│                                │
│    ┌────┐  ┌────┐  ┌────┐     │  ← 3 nearest islands visible
│    │ 📝 │  │ 🌐 │  │ 📄 │     │     Head turn = pan canvas
│    └────┘  └────┘  └────┘     │     Voice: "Focus on notes"
│                                │
│   Level 1 · 3 of 10           │  ← Minimal zoom indicator
└────────────────────────────────┘
```
Head-tracked: turn head left/right to pan across islands. Voice "zoom in" or "focus on [name]" to enter a module. Minimal HUD overlay — just the islands and a zoom indicator. Voice is primary; head tracking is spatial navigation.

---

### Key Design Decisions

**The Canvas** (replaces Dashboard):
- Infinite 2D plane with semantic zoom
- Zoom out: see all modules as icons (bird's-eye overview)
- Zoom in: single module fills screen (focused work)
- Modules are **islands** — floating cards on the canvas that grow larger as you zoom in
- Position is meaningful: frequently used modules drift to center, rarely used drift to edges
- User can drag to rearrange (spatial memory)

**Semantic Zoom Levels**:
```
Level 1 (100%): Overview — All module islands visible as small cards
Level 2 (200%): Module — Selected module fills 60% of screen, neighbors visible at edges
Level 3 (400%): Focus — Single module fullscreen, immersive (zero chrome)
Level 4 (800%): Deep — Content detail (e.g., note text is readable, PDF page is zoomable)
```
- Voice: "Zoom in" / "Zoom out" / "Focus on notes" / "Show everything"
- Touch: Pinch zoom (natural gesture)
- Smart glasses: Head tilt forward = zoom in, tilt back = zoom out

**Module Access**:
- Voice: "Open notes" → canvas smoothly zooms to Notes island at Level 3
- Touch: Tap island → zooms to Level 2, double-tap → Level 3
- Canvas remembers position — "Go back" returns to previous zoom level
- No navigation stack — it's spatial, like walking through a room

**Multi-Window (Cockpit Integration)**:
- At Level 2 (200%), you see the focused module + its neighbors
- Voice: "Bring PDF here" → PDF island animates next to current island
- Islands can be **grouped** by dragging close together → they form a cluster
- Clusters are the user's version of "layouts" — organic, not prescribed
- The 15 layout modes become automatic: system detects cluster shape and applies best LayoutMode internally
- Voice: "Focus" → zooms one island to Level 3. "Compare" → zooms cluster to show 2-3 islands

**Contextual Actions — Orbit Ring**:
```
        [Bold]
    [Undo]    [Italic]
        [📝]              ← When focused on a module, actions orbit around it
    [Save]    [Share]
        [More]
```
- When at Zoom Level 3+ (focused), a subtle ring of action chips orbits the content
- Only top 6 actions shown (radial layout, thumb-reachable on phone)
- "More" expands to full action list
- Voice: all actions available by name without ring interaction
- Ring auto-hides after 3s of inactivity, reappears on tap/voice

**Voice Command Surfacing**:
- At Level 1 (overview): "Say any module name to zoom in"
- At Level 3 (focus): Orbit ring chips show voice aliases
- Voice feedback: brief animation on the island (ripple effect) confirms action
- "Help" at any zoom level → overlay of contextual commands

**Responsive Adaptation**:
| Device | Canvas Behavior | Zoom Control | Orbit Ring |
|--------|----------------|--------------|------------|
| Phone | Touch-driven pan/zoom, vertical bias | Pinch + voice | Bottom arc (6 chips) |
| Tablet | Spacious canvas, pen support | Pinch + stylus + voice | Full ring around content |
| Desktop | Mouse wheel zoom, click-drag pan | Scroll wheel + voice + Cmd+/- | Ring or sidebar (user choice) |
| Smart Glasses | Head-tracked viewport, voice zoom | Voice + head tilt | Voice-only (no ring visual) |

**Cognitive Load Reduction**:
- Current: 15 layouts to choose from, command bar with hidden states
- Canvas: **Zero explicit layout choice** — system infers from spatial arrangement
- Zoom metaphor is universal (maps, photos) — no learning needed
- Overview → Focus is a natural cognitive progression (forest → tree → leaf)
- Idle state shows only module islands + dot grid = **minimal visual noise**

---

## Comparison Matrix

| Dimension | A: AvanueViews (Stream) | B: Lens (Palette) | C: Canvas (Spatial) |
|-----------|--------------------|--------------------|---------------------|
| **Mental Model** | Timeline / feed | Search engine | Physical desk / room |
| **Home Elements** | 3-5 cards + FAB | 1 search bar + ghosts | 5-10 floating islands |
| **Module Access** | Voice + swipe-up sheet | Voice + Lens query | Voice + zoom navigation |
| **Multi-Window** | 4 intent keywords | 4 intent keywords | Organic spatial clustering |
| **Actions** | Flat whisper bar (5 chips) | Lens bar (type command) | Orbit ring (6 chips) |
| **Navigation** | Scroll stream | Type/say query | Pan/zoom canvas |
| **Voice Weight** | Primary, cards show hints | Primary, Lens is voice-native | Primary, spatial commands |
| **Learning Curve** | Very low (feed is familiar) | Very low (search is familiar) | Low-medium (zoom is familiar) |
| **Power Users** | Moderate (limited density) | Excellent (keyboard chains) | Excellent (spatial memory) |
| **Glass Suitability** | Excellent (1 card at a time) | Good (voice replaces Lens) | Good (head-tracked viewport) |
| **Delight Factor** | Calm, ambient | Fast, powerful | Beautiful, immersive |
| **Best For** | Casual/new users, glasses | Power users, keyboard warriors | Creative/visual workers |

---

## Implementation Approach (All Variations Share)

### Phase 0: Branch + Save Plan
1. Create new branch `AvanueViews` from current branch `SpeechEngineRevamp` (carrying all existing work)
2. Save this plan to: `docs/plans/CockpitUI/CockpitUI-Plan-SimplifiedVoiceFirstUI-260225-V1.md`

### Implementation Order (MANDATORY)
1. **commonMain first** — All shared models, interfaces, state, and composables in KMP commonMain
2. **Android platform** — androidMain implementations (ContentRenderer, platform sensors, etc.)
3. **Web platform** — jsMain/wasmJsMain implementations (browser-specific rendering)
4. **Desktop** — desktopMain (already KMP-shared with commonMain via JVM, minimal extra)
5. **iOS** — iosMain (future, stubs if needed)

### Phase 1: Layout Intent Abstraction (Common Foundation)
**Files to modify:**
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/model/LayoutMode.kt`
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/LayoutEngine.kt` (new)

Create `ArrangementIntent` enum mapping to LayoutModes:
```kotlin
enum class ArrangementIntent {
    FOCUS,    // → FULLSCREEN
    COMPARE,  // → SPLIT_LEFT, SPLIT_RIGHT (auto-detect from content)
    OVERVIEW, // → GRID, MOSAIC, T_PANEL (auto from frame count + display)
    PRESENT   // → CAROUSEL, TRIPTYCH (auto from content types)
}
```
`LayoutModeResolver` takes `(intent, frameCount, displayProfile)` → returns specific `LayoutMode`. Users never see the 15 raw modes.

### Phase 2: Contextual Action System (Replaces CommandBar State Machine)
**Files to modify:**
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/model/CommandBarState.kt`
- `Modules/Cockpit/src/commonMain/kotlin/com/augmentalis/cockpit/ui/CommandBar.kt`

Create `ContextualActionProvider`:
```kotlin
interface ContextualActionProvider {
    fun actionsForContent(contentType: ContentType): List<QuickAction>  // Top 5-6
    fun allActionsForContent(contentType: ContentType): List<ActionGroup>  // Full list, grouped
}
```
- Flat (no hierarchy) — always returns a single list for the focused content
- "More" action opens a searchable bottom sheet with full grouped actions
- All voice commands remain accessible regardless of what's visible

### Phase 3: Variation-Specific UI Shell
Build one of the three shells (user choice):

**A (AvanueViews):** New `AvanueViewsStreamLayout.kt` in commonMain — card priority engine + ambient awareness
**B (Lens):** New `LensLayout.kt` in commonMain — universal command palette + fuzzy search index
**C (Canvas):** New `ZenCanvasLayout.kt` in commonMain — semantic zoom + island positioning engine

Each shell replaces `DashboardLayout.kt` as the home screen and wraps the existing `ContentRenderer` for module display.

### Phase 4: Voice Integration Wiring
- All three variations wire into existing `ModuleCommandCallbacks` and VOS command infrastructure
- Add `ArrangementIntent` voice commands: "focus", "compare", "overview", "present"
- Add variation-specific commands (e.g., "zoom in" for Canvas, "next card" for AvanueViews)

### Phase 5: DisplayProfile Adaptation
- Leverage existing `DisplayProfile` (PHONE/TABLET/GLASS_MICRO/GLASS_COMPACT/GLASS_STANDARD/GLASS_HD) for responsive behavior
- Each variation defines its own adaptation rules (documented above)

### Critical Files to Modify
| File | Change | Phase |
|------|--------|-------|
| `Modules/Cockpit/src/commonMain/.../model/LayoutMode.kt` | Add ArrangementIntent enum + resolver | 1 |
| `Modules/Cockpit/src/commonMain/.../ui/LayoutEngine.kt` | Intent→LayoutMode mapping | 1 |
| `Modules/Cockpit/src/commonMain/.../model/CommandBarState.kt` | Flatten to ContextualActionProvider | 2 |
| `Modules/Cockpit/src/commonMain/.../ui/CommandBar.kt` | Flat contextual bar (5-6 chips + More) | 2 |
| `Modules/Cockpit/src/commonMain/.../ui/DashboardLayout.kt` | Replace with variation shell | 3 |
| `Modules/Cockpit/src/commonMain/.../ui/CockpitScreenContent.kt` | Swap shell based on user preference | 3 |
| `Modules/Cockpit/src/commonMain/.../model/DashboardState.kt` | Evolve to support cards/lens/islands | 3 |
| `Modules/AvanueUI/src/commonMain/.../components/` | New components if needed (SearchBar, OrbitRing) | 3 |
| `Modules/VoiceOSCore/src/commonMain/` | Add arrangement intent voice commands | 4 |

### Reusable Existing Infrastructure
- **AvanueCard** → Cards in AvanueViews, Islands in Canvas, Result items in Lens
- **AvanueChip** → Action chips in all three variations
- **AvanueFAB** → Voice FAB in AvanueViews and Canvas
- **AvanueSurface** → Lens bar background, Orbit ring background
- **Glass/Water effects** → All card/surface rendering
- **DisplayProfile** → Responsive rules for all variations
- **ContentRenderer** → Unchanged — all variations render module content the same way
- **ModuleCommandCallbacks** → Unchanged — voice command execution unchanged
- **LayoutMode** (all 15) → Still used internally, just hidden behind ArrangementIntent

### Verification
1. **Build**: `./gradlew :Modules:Cockpit:compileKotlinDesktop` + `:compileKotlinAndroid` — verify no compile errors
2. **Visual**: Preview in Desktop app — verify home screen renders correctly for chosen variation
3. **Voice**: Test "open notes", "focus", "compare notes and PDF" commands via VoiceOSCore
4. **Responsive**: Test on Phone/Tablet/Desktop display profiles
5. **Theme**: Verify all 4 palettes x 4 material modes render correctly (32 combos)
6. **Glass**: Test GLASS_MICRO profile — verify single-card/voice-only mode activates

---

## Recommendation

**My recommendation: Start with Variation B (Lens).**

**Because:**
1. Lowest implementation effort — a command palette is a well-understood pattern with clear architecture
2. Highest power-user satisfaction — keyboard warriors and voice users both benefit equally
3. Best voice mapping — "type what you want" directly maps to "say what you want"
4. Most universal — works on every display size including glasses (voice replaces visual Lens)
5. Cleanest cognitive model — ONE entry point vs learning card priorities (A) or spatial zoom (C)

**Risk if ignored:** Starting with Canvas (C) is tempting but requires spatial layout engine (non-trivial), and AvanueViews (A) needs an intelligent card priority system (ML/heuristic complexity). Lens is achievable in 1-2 sessions.

**After Lens is proven**, Canvas (C) makes an excellent Phase 2 — the ArrangementIntent system from Lens directly powers Canvas's automatic layout inference.

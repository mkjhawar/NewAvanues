# Avanues Dashboard — Apple iOS/macOS Style Design

**Date:** 2026-02-11
**Module:** Avanues (All Screens)
**Type:** UI Design Concept
**Design Language:** Apple Human Interface Guidelines (iOS 18 / macOS 15)

---

## Design Philosophy

Apple's design language applied to the Avanues ecosystem:

| Apple Principle | Avanues Application |
|----------------|---------------------|
| **Clarity** | Content is king — remove all visual noise, let data breathe |
| **Deference** | UI recedes, content advances — no decorative chrome |
| **Depth** | Layered materials (vibrancy, blur) create spatial hierarchy |
| **Consistency** | Every screen feels like the same app — same patterns, same rhythm |
| **Direct Manipulation** | Tap what you see, swipe what you feel |

### Key Visual Tokens

| Token | Value |
|-------|-------|
| Font | SF Pro (Display for titles, Text for body, Mono for code) |
| Corner Radius | 12pt (cards), 10pt (buttons), 22pt (search bar) |
| Section Inset | 16pt horizontal, grouped list background |
| Row Height | 44pt minimum (touch target) |
| Separator | 0.33pt hairline, indented from leading edge |
| Tint | System Blue (#007AFF) for interactive elements |
| Background | systemGroupedBackground (light gray / dark elevated) |
| Card BG | secondarySystemGroupedBackground (white / dark surface) |
| Vibrancy | .regularMaterial for navigation bars, .thinMaterial for overlays |

---

## Screen 1: HUB DASHBOARD

```
┌─────────────────────────────────────┐
│ ≡                          ⚙       │  ← Navigation bar, .regularMaterial
│                                     │
│   Avanues                           │  ← Large Title (SF Pro Display, 34pt)
│   Your accessibility ecosystem      │  ← Caption (SF Pro Text, 13pt, secondary)
│                                     │
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ │  🎤  VoiceAvanue               >│ │  ← Prominent card, system blue accent
│ │      Voice control & access...  │ │     SF Symbol: mic.fill
│ │                                 │ │
│ ├─────────────────────────────────┤ │  ← 0.33pt separator
│ │                                 │ │
│ │  🌐  WebAvanue                 >│ │  ← SF Symbol: globe
│ │      Voice-enabled browser      │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│   ECOSYSTEM                         │  ← Section header (13pt, secondary, uppercase)
│ ┌─────────────────────────────────┐ │
│ │  ⚙  Settings                   >│ │  ← Inset grouped list row
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │  ← Indented separator
│ │  ⓘ  About Avanues              >│ │
│ └─────────────────────────────────┘ │
│                                     │
│         Avanues Ecosystem           │  ← Footer (12pt, tertiary, centered)
│                                     │
└─────────────────────────────────────┘
```

### iPad / Landscape (NavigationSplitView)

```
┌──────────────┬──────────────────────────────┐
│  Avanues     │                              │
│              │     Welcome to Avanues       │
│ ─────────    │                              │
│              │  ┌────────┐  ┌────────┐      │
│  APPS        │  │  🎤    │  │  🌐    │      │
│  VoiceAvanue │  │ Voice  │  │  Web   │      │
│  WebAvanue   │  │ Avanue │  │ Avanue │      │
│              │  └────────┘  └────────┘      │
│ ─────────    │                              │
│  ECOSYSTEM   │  Service Status              │
│  Settings    │  ● Accessibility: Active     │
│  About       │  ● Overlay: Granted          │
│              │  ● Microphone: Granted       │
│              │                              │
└──────────────┴──────────────────────────────┘
```

---

## Screen 2: VOICEAVANUE DASHBOARD (Home)

```
┌─────────────────────────────────────┐
│ ‹ Back                       ⚙     │  ← Compact nav bar
│                                     │
│   VoiceOS® Avanues                  │  ← Large Title
│   ● Service active                  │  ← Green dot + caption (SF Mono, 12pt)
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  MODULES                        │ │  ← Section header inside card
│ │                                 │ │
│ │  ┌─────────────────────────┐    │ │
│ │  │ 🎤 VoiceTouch™          │    │ │  ← Primary module, full-width
│ │  │ powered by VoiceOS®     │    │ │
│ │  │                  ● Running│   │ │  ← Green pill badge
│ │  │ engine: Android STT     │    │ │  ← Monospace detail row
│ │  │ language: en-US         │    │ │
│ │  └─────────────────────────┘    │ │
│ │                                 │ │
│ │  ┌───────────┐ ┌───────────┐   │ │  ← Two compact cards, side-by-side
│ │  │ 🌐        │ │ 👆        │   │ │
│ │  │ WebAvanue │ │ Cursor    │   │ │
│ │  │  ● Ready  │ │ Avanue   │   │ │
│ │  │           │ │  ○ Off    │   │ │
│ │  └───────────┘ └───────────┘   │ │
│ └─────────────────────────────────┘ │
│                                     │
│   PERMISSIONS          3/5          │  ← Section header + count badge
│ ┌─────────────────────────────────┐ │
│ │  ✓  Microphone               OK │ │  ← Green check, muted text
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  ✓  Accessibility Service    OK │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  ✓  Display Over Apps        OK │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  ⚠  Battery Restricted          │ │  ← Orange warning, prominent
│ │     Tap to allow unrestricted  > │ │  ← Tappable row with chevron
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  ✕  Notifications                │ │  ← Red X
│ │     Open Settings to enable    > │ │
│ └─────────────────────────────────┘ │
│                                     │
│   LAST HEARD                        │
│ ┌─────────────────────────────────┐ │
│ │  🎤  "scroll down"              │ │  ← Quoted phrase, primary text
│ │      2 min ago · 94% confidence │ │  ← Secondary, SF Mono for %
│ └─────────────────────────────────┘ │
│                                     │
│   COMMANDS                          │
│ ┌─────────────────────────────────┐ │
│ │  Static    142    Dynamic    0  │ │  ← Grid of counts
│ │  Custom      0    Synonyms  18  │ │
│ ├─────────────────────────────────┤ │
│ │         View All Commands     > │ │  ← Blue tint, chevron
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### Key Apple Differences from Current Design

| Current (SpatialVoice) | Apple Style |
|------------------------|-------------|
| AvanueCard with water/glass effect | Plain grouped list cells, no effects |
| Gradient backgrounds | Flat systemGroupedBackground |
| PulseDot animation | Static SF Symbol dot (●/○) |
| Warning triangle icon in circle | Inline ⚠ symbol, no circle |
| Green "Direct" / Orange "Manual" badges | No badge — just chevron for tappable |
| Bold section headers in color | 13pt uppercase secondary text |
| Full-bleed cards | Inset grouped list (16pt margin) |

---

## Screen 3: VOICE COMMANDS

```
┌─────────────────────────────────────┐
│ ‹ Commands              🔍    ╋    │  ← Search + Add button
│                                     │
│   Voice Commands                    │  ← Large Title
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Navigation  System  Text  More▸ │ │  ← Scrollable segmented control
│ └─────────────────────────────────┘ │
│                                     │
│   NAVIGATION                        │  ← Grouped section
│ ┌─────────────────────────────────┐ │
│ │  go back                    🔘  │ │  ← Toggle switch (iOS style)
│ │  ↳ back, return, previous      │ │  ← Synonyms in secondary text
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  go home                    🔘  │ │
│ │  ↳ home, main screen           │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  recent apps                🔘  │ │
│ │  ↳ app switcher, recents       │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  scroll up                  🔘  │ │
│ │  ↳ swipe up, page up           │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  scroll down                🔘  │ │
│ │  ↳ swipe down, page down       │ │
│ └─────────────────────────────────┘ │
│                                     │
│   Tap a command to add aliases      │  ← Footer hint
│                                     │
│                                     │
│   SYNONYMS                    Edit  │  ← Editable section
│ ┌─────────────────────────────────┐ │
│ │  click → tap, press, push       │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  open → launch, start, run      │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  close → exit, quit, dismiss    │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### Command Drill-Down (Tap a command row)

```
┌─────────────────────────────────────┐
│ ‹ Navigation                        │
│                                     │
│   go back                           │  ← Large Title
│                                     │
│   DETAILS                           │
│ ┌─────────────────────────────────┐ │
│ │  Category        Navigation     │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Action          NAVIGATE_BACK  │ │  ← Monospace value
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Enabled         🔘 On         │ │  ← Toggle
│ └─────────────────────────────────┘ │
│                                     │
│   ALIASES                     Add   │  ← Blue "Add" button
│ ┌─────────────────────────────────┐ │
│ │  back                           │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  return                         │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  previous                       │ │
│ └─────────────────────────────────┘ │
│                                     │
│   DESCRIPTION                       │
│ ┌─────────────────────────────────┐ │
│ │  Navigate to the previous       │ │
│ │  screen or page.                │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 4: UNIFIED SETTINGS

```
┌─────────────────────────────────────┐
│ ‹ Back                              │
│                                     │
│   Settings                          │  ← Large Title
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  🔍 Search                      │ │  ← Rounded search bar
│ └─────────────────────────────────┘ │
│                                     │
│   CORE                              │
│ ┌─────────────────────────────────┐ │
│ │  ⚠  Permissions                >│ │  ← Orange icon circle
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  👆  Voice Cursor              >│ │  ← Green icon circle
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  🎤  Voice Control             >│ │  ← Blue icon circle
│ └─────────────────────────────────┘ │
│                                     │
│   BROWSER                           │
│ ┌─────────────────────────────────┐ │
│ │  🌐  WebAvanue                 >│ │
│ └─────────────────────────────────┘ │
│                                     │
│   SYSTEM                            │
│ ┌─────────────────────────────────┐ │
│ │  ⚙  System                    >│ │
│ └─────────────────────────────────┘ │
│                                     │
│                                     │
│                                     │
│                                     │  ← Generous bottom whitespace
└─────────────────────────────────────┘
```

### Settings → Voice Cursor (Detail Pane)

```
┌─────────────────────────────────────┐
│ ‹ Settings                          │
│                                     │
│   Voice Cursor                      │  ← Large Title
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  Enable Cursor           🔘 Off│ │  ← Master toggle
│ └─────────────────────────────────┘ │
│                                     │
│   APPEARANCE                        │
│ ┌─────────────────────────────────┐ │
│ │  Accent Color                   │ │
│ │  ┌──┐┌──┐┌──┐┌──┐┌──┐┌──┐     │ │  ← Color swatches (circular)
│ │  │🔵││🟢││🟣││🔴││🟠││⚪│     │ │     Selected = checkmark overlay
│ │  └──┘└──┘└──┘└──┘└──┘└──┘     │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Cursor Size                    │ │
│ │  ├────────●─────────────┤  48pt │ │  ← iOS-style slider + value
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Border Width                   │ │
│ │  ├──●───────────────────┤   2pt │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Cursor Opacity                 │ │
│ │  ├──────────────●───────┤   80% │ │
│ └─────────────────────────────────┘ │
│                                     │
│   BEHAVIOR                          │
│ ┌─────────────────────────────────┐ │
│ │  Dwell Click             🔘 On │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Dwell Delay                    │ │
│ │  ├────────────●─────────┤ 1.5s  │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Smoothing               🔘 On │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Show Coordinates       🔘 Off │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### iPad Settings (NavigationSplitView)

```
┌──────────────────┬──────────────────────────────┐
│ Settings         │  Voice Cursor                │
│                  │                              │
│ ─────────────    │  ┌────────────────────────┐  │
│ 🔍 Search        │  │ Enable Cursor    🔘 Off│  │
│                  │  └────────────────────────┘  │
│ CORE             │                              │
│ ⚠ Permissions    │  APPEARANCE                  │
│ 👆 Voice Cursor ◄│  ┌────────────────────────┐  │
│ 🎤 Voice Control │  │ Accent Color           │  │
│                  │  │ 🔵 🟢 🟣 🔴 🟠 ⚪     │  │
│ BROWSER          │  ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤  │
│ 🌐 WebAvanue     │  │ Cursor Size       48pt │  │
│                  │  │ ├────●──────────┤      │  │
│ SYSTEM           │  ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤  │
│ ⚙ System         │  │ Border Width      2pt  │  │
│                  │  │ ├●──────────────┤      │  │
│                  │  └────────────────────────┘  │
│                  │                              │
│                  │  BEHAVIOR                    │
│                  │  ┌────────────────────────┐  │
│                  │  │ Dwell Click     🔘 On  │  │
│                  │  │ Dwell Delay      1.5s  │  │
│                  │  │ Smoothing       🔘 On  │  │
│                  │  └────────────────────────┘  │
└──────────────────┴──────────────────────────────┘
```

---

## Screen 5: DEVELOPER CONSOLE

```
┌─────────────────────────────────────┐
│ ‹ Back                   ↻    ⬆    │  ← Refresh + Export (share icon)
│                                     │
│   Developer Console                 │  ← Large Title
│   Debug & diagnostics               │  ← Caption, secondary
│                                     │
│   BUILD INFO                        │
│ ┌─────────────────────────────────┐ │
│ │  Version          1.0.0-debug   │ │  ← Monospace value, right-aligned
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Application ID   ...avanues.de │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Device SDK       34            │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Device           Google sdk... │ │
│ └─────────────────────────────────┘ │
│                                     │
│   SERVICE STATES                    │
│ ┌─────────────────────────────────┐ │
│ │  Accessibility    ●  ENABLED    │ │  ← Green dot + bold green text
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Overlay          ●  GRANTED   │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Notifications    ○  BLOCKED   │ │  ← Red hollow dot + red text
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Battery          ○  RESTRICTED│ │
│ └─────────────────────────────────┘ │
│                                     │
│   RPC PORTS                         │
│ ┌─────────────────────────────────┐ │
│ │  PluginRegistry  50050  ● UDS  │ │  ← Monospace, green/red dot
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  VoiceOS         50051  ● UDS  │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  AVA             50052  ● UDS  │ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Cockpit         50053  ○ TCP  │ │  ← Red = inactive
│ └─────────────────────────────────┘ │
│                                     │
│   DEVICE HARDWARE                 ▾ │  ← Expandable (disclosure)
│ ┌─────────────────────────────────┐ │
│ │  CPU Cores        8             │ │
│ │  Architecture     arm64-v8a     │ │
│ │  Total RAM        2.0 GB        │ │
│ │  Storage          6.4 / 6.4 GB  │ │
│ └─────────────────────────────────┘ │
│                                     │
│   MEMORY & HEAP                   ▾ │
│   BATTERY                         ▾ │
│   DISPLAY                         ▾ │
│   PERIPHERALS                     ▾ │  ← Collapsed by default
│   VOICE COMMANDS                  ▾ │
│                                     │
│   DATASTORE                  Reset  │  ← Destructive action in red
│ ┌─────────────────────────────────┐ │
│ │  cursor_enabled        false    │ │  ← Monospace key=value
│ │  theme_variant         LIQUID   │ │
│ │  voice_feedback        true     │ │
│ └─────────────────────────────────┘ │
│                                     │
│   DATABASE BROWSER                ▾ │  ← Expandable tree
│ ┌─────────────────────────────────┐ │
│ │  📁 voiceos.db          1.2 MB │ │  ← Disclosure triangle
│ │  📁 browser.db          48 KB  │ │
│ └─────────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

---

## Screen 6: ABOUT

```
┌─────────────────────────────────────┐
│ ‹ Back                              │
│                                     │
│   About                             │  ← Large Title
│                                     │
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ │         🔊                      │ │  ← App icon, centered, 60pt
│ │    VoiceOS® Avanues             │ │  ← App name, 20pt, centered
│ │    Version 1.0.0 (1)            │ │  ← Version, secondary, centered
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  What's New                    >│ │  ← Chevron row
│ └─────────────────────────────────┘ │
│                                     │
│   LEGAL                             │
│ ┌─────────────────────────────────┐ │
│ │  Open Source Licenses          >│ │
│ │  24 libraries                   │ │  ← Detail text, secondary
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Privacy Policy                >│ │
│ ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤ │
│ │  Terms of Service              >│ │
│ └─────────────────────────────────┘ │
│                                     │
│                                     │
│   Designed and Created in           │  ← Footer, 12pt, tertiary
│   California with Love.            │
│                                     │
│   © 2018-2026 Intelligent           │
│   Devices LLC                       │
│                                     │
└─────────────────────────────────────┘
```

---

## Design Comparison: SpatialVoice vs Apple

| Aspect | Current (SpatialVoice) | Apple Style |
|--------|----------------------|-------------|
| **Background** | Gradient (dark → surface → dark) | Flat systemGroupedBackground |
| **Cards** | Water/glass effect with blur + caustics | Plain white/dark rounded rects |
| **Animations** | PulseDot, caustic shimmer, water ripple | Minimal — spring transitions only |
| **Navigation** | Custom TopAppBar with animated title | Standard UINavigationBar, large titles |
| **Section Headers** | Colored, bold, custom font | 13pt uppercase secondary (system) |
| **Permissions** | Warning icon in circle + colored badge | Inline SF Symbol + chevron |
| **Module Cards** | Colored status border + metadata | Simple rows with status dot |
| **Toggles** | Compose Checkbox | iOS-style Switch (rounded toggle) |
| **Icons** | Material Icons (filled) | SF Symbols (regular weight) |
| **Typography** | MaterialTheme.typography | SF Pro Display/Text/Mono |
| **Colors** | AvanueTheme.colors (ocean/sunset/liquid) | System colors (blue tint, gray bg) |
| **Tab Bar** | None (single-screen nav) | Bottom tab bar for main sections |
| **Search** | TextField in LazyColumn | Rounded UISearchBar at top |
| **Disclosure** | KeyboardArrowDown/Up | Chevron.right / disclosure triangle |
| **Spacing** | SpacingTokens (dense) | 16pt inset, generous whitespace |
| **GPU Cost** | High (shaders, blur, infinite animation) | Near-zero (flat, system rendering) |

---

## Implementation Notes

### If Adopting Apple Style

1. **New theme variant**: Add `AvanueThemeVariant.APPLE` alongside OCEAN/SUNSET/LIQUID
2. **MaterialMode.PLAIN**: Already exists — Apple style maps to PLAIN mode
3. **No water/glass effects**: Set `enableRefraction=false`, `enableSpecular=false`, `enableCaustics=false`
4. **System colors**: Map Apple system colors to AvanueColorScheme:
   - `systemBlue` → primary
   - `systemGreen` → success
   - `systemRed` → error
   - `systemGroupedBackground` → background
   - `secondarySystemGroupedBackground` → surface
5. **Inset grouped list**: Create `AvanueGroupedList` component wrapping LazyColumn with Apple-style section styling
6. **Large titles**: Already supported via Compose Material3 `LargeTopAppBar`
7. **SF Symbols**: Not available on Android — continue using Material Icons but with regular (outlined) weight instead of filled
8. **Bottom tab bar**: Add `AvanueTabBar` for hub-level navigation (VoiceAvanue, WebAvanue, Settings)

### Estimated Effort

| Task | Complexity |
|------|-----------|
| APPLE theme variant (colors only) | Small — add new color scheme |
| Inset grouped list component | Medium — new layout component |
| Large title navigation | Small — use existing M3 LargeTopAppBar |
| Bottom tab bar | Medium — new navigation pattern |
| Settings Apple-style | Medium — restyle existing settings |
| Remove effects for PLAIN mode | Small — already conditional |
| **Total** | ~2-3 sessions |

---

*Document generated: 2026-02-11*
*Design Language: Apple Human Interface Guidelines (iOS 18 / macOS 15)*
*Applied to: Avanues Consolidated App, all screens*

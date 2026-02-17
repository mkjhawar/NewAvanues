# Developer Manual — Chapter 99: Avanues Brand Architecture & Naming Guide

**Scope:** All modules, apps, documentation, and UI strings
**Branch:** `Cockpit-Development`
**Date:** 2026-02-17
**Authors:** Manoj Jhawar, Aman Jhawar

---

## 1. Brand Philosophy

### 1.1 The Name "Avanues"

**Avanues** (deliberately spelled without the second "e") is the brand name for the ecosystem of voice-first applications built on the VoiceOS platform. The name is a portmanteau and deliberate stylization:

- **Ava** — the core AI identity within the system
- **nues** — derived from "avenues", representing paths of exploration

The intentional spelling distinction from "avenues" serves two purposes:
1. **Trademark differentiation** — a unique, registrable mark
2. **Brand identity** — immediately recognizable as a product name, not a common word

### 1.2 The Metaphor: City & Avenues

The Avanues ecosystem uses a **city metaphor** to organize its brand hierarchy:

| Real-World Concept | Avanues Equivalent | Description |
|---|---|---|
| The city itself | **VoiceOS** | The platform — the operating environment where everything runs |
| City infrastructure | **Foundation, Database, Logging** | Invisible plumbing that makes the city work |
| The central square | **AvanueHUB** | The intersection where all avenues meet — the multi-window hub |
| Named avenues | **WebAvanue, PhotoAvanue, etc.** | Destinations — each is an avenue worth exploring |
| Street signs | **SpatialVoice design language** | The visual identity that makes the city feel cohesive |

Think of it like **Windows**: Microsoft named their OS after a UI metaphor (windows on a desktop). We named ours after a navigation metaphor — each feature is an **avenue** to explore, and the platform is the **city** that connects them.

### 1.3 Core Brand Statement

> **"VoiceOS is the city. Avanues are the paths. Every feature is an avenue worth exploring."**

---

## 2. Brand Hierarchy

```
VoiceOS (R)                          ← The platform / operating environment
  |
  +-- Avanues EcoSystem              ← The product family / ecosystem name
       |
       +-- AvanueHUB                 ← The hub — where all avenues meet
       |     |
       |     +-- Frame windows       ← Content panels within the hub
       |
       +-- Core Avanues              ← Primary voice-first applications
       |     +-- VoiceTouch (TM)     ← Voice control platform
       |     +-- WebAvanue           ← Voice-enabled browser
       |     +-- CursorAvanue        ← Handsfree cursor control
       |
       +-- Content Avanues           ← Media and productivity destinations
       |     +-- PhotoAvanue         ← Camera & photo capture
       |     +-- PDFAvanue           ← Document viewing
       |     +-- ImageAvanue         ← Image gallery
       |     +-- VideoAvanue         ← Media playback
       |     +-- NoteAvanue          ← Writing & notes
       |     +-- DrawAvanue          ← Annotation & creative
       |     +-- CastAvanue          ← Screen sharing & remote display
       |
       +-- Infrastructure            ← No brand suffix — invisible to users
             +-- Foundation (KMP)
             +-- Database (SQLDelight)
             +-- Logging
             +-- DeviceManager
             +-- AvanueUI (theme engine)
```

### 2.1 Tier Definitions

| Tier | What Gets It | Naming Pattern | Example |
|---|---|---|---|
| **Platform** | The runtime environment | Standalone brand name + (R) | VoiceOS(R) |
| **Ecosystem** | The product family | "Avanues" as collective noun | Avanues EcoSystem |
| **Hub** | The central multi-window space | "Avanue" + function noun | AvanueHUB |
| **Core Avanue** | Primary voice-first apps | Function name + "Avanue" OR unique brand | VoiceTouch(TM), WebAvanue |
| **Content Avanue** | Media/productivity modules | Content type + "Avanue" | PhotoAvanue, PDFAvanue |
| **Infrastructure** | Libraries, engines, SDKs | Descriptive name, NO "Avanue" suffix | Foundation, Database, Logging |

### 2.2 When to Use "Avanue" vs. When NOT To

**USE the "Avanue" suffix when:**
- The module is a **user-facing destination** — something a person navigates to and interacts with
- The module appears in the **hub launcher** or **app switcher**
- The module has its own **standalone screen** that users can open directly

**DO NOT use the "Avanue" suffix when:**
- The module is a **library** consumed by other modules (Foundation, AvanueUI)
- The module is a **service** running in the background (VoiceOSCore accessibility service)
- The module is a **build tool**, **test utility**, or **developer infrastructure**
- The module is a **platform capability** (DeviceManager, IMU tracking)

---

## 3. Module Registry

### 3.1 Core Orbit (Hub Inner Ring)

| Module ID | Display Name | Subtitle | Icon | Trademark |
|---|---|---|---|---|
| `voiceavanue` | VoiceTouch(TM) | Voice control platform | Mic | (TM) required on first use |
| `webavanue` | WebAvanue | Voice browser | Language | None |
| `voicecursor` | CursorAvanue | Handsfree cursor | Mouse | None |
| `cockpit` | AvanueHUB | Where all avenues meet | Dashboard | None |

### 3.2 Content Orbit (Hub Outer Ring)

| Module ID | Display Name | Subtitle | Icon |
|---|---|---|---|
| `pdfavanue` | PDFAvanue | Document avenue | PictureAsPdf |
| `imageavanue` | ImageAvanue | Gallery avenue | Image |
| `videoavanue` | VideoAvanue | Media avenue | VideoLibrary |
| `noteavanue` | NoteAvanue | Writing avenue | EditNote |
| `photoavanue` | PhotoAvanue | Capture avenue | CameraAlt |
| `remotecast` | CastAvanue | Sharing avenue | Cast |
| `annotationavanue` | DrawAvanue | Creative avenue | Draw |

### 3.3 Infrastructure (No Brand Suffix)

| Module | Package | Purpose |
|---|---|---|
| Foundation | `com.augmentalis.foundation` | KMP platform abstractions |
| Database | `com.augmentalis.database` | SQLDelight schema & repos |
| Logging | `com.augmentalis.logging` | Cross-platform logging |
| AvanueUI | `com.augmentalis.avanueui` | Theme engine & design tokens |
| VoiceOSCore | `com.augmentalis.voiceoscore` | Voice command pipeline |
| DeviceManager | `com.augmentalis.devicemanager` | IMU, sensors, hardware |
| NAV | `com.augmentalis.nav` | Navigation & routing |

### 3.4 Subtitle Pattern

Content Avanue subtitles follow the pattern: **"[Purpose] avenue"** (lowercase "avenue").

This reinforces the metaphor — each module is literally an avenue to explore:
- "Document avenue" (PDFAvanue)
- "Gallery avenue" (ImageAvanue)
- "Capture avenue" (PhotoAvanue)

Core Avanues have **descriptive** subtitles instead:
- "Voice control platform" (VoiceTouch)
- "Voice browser" (WebAvanue)
- "Handsfree cursor" (CursorAvanue)
- "Where all avenues meet" (AvanueHUB)

---

## 4. Naming Conventions for New Modules

### 4.1 Decision Tree

```
Is the module user-facing?
  |
  +-- NO --> Use descriptive name (no "Avanue" suffix)
  |          Examples: Foundation, DeviceManager, Logging
  |
  +-- YES --> Does the user navigate TO it as a destination?
               |
               +-- NO --> Use descriptive name
               |          Example: VoiceOSCore (runs as a service)
               |
               +-- YES --> Use [Content/Function] + "Avanue"
                           Example: PhotoAvanue, NoteAvanue
```

### 4.2 Naming Rules

| Rule | Correct | Incorrect |
|---|---|---|
| Brand suffix is "Avanue" (singular) | PhotoAvanue | PhotoAvanues, PhotoAvenue |
| No space between name and "Avanue" | WebAvanue | Web Avanue |
| CamelCase for compound names | PDFAvanue | PdfAvanue, PDF-Avanue |
| Content type comes FIRST | ImageAvanue | AvanueImage |
| Hub is "AvanueHUB" (HUB uppercase) | AvanueHUB | AvanueHub, Avanue Hub |
| Ecosystem spelling is "Avanues" | Avanues EcoSystem | Avenues, Avanuees |
| Infrastructure has NO suffix | Foundation | FoundationAvanue |

### 4.3 Package Naming

All packages use the `com.augmentalis` root:

```
com.augmentalis.{modulename}           # Module root
com.augmentalis.{modulename}.model     # Data models
com.augmentalis.{modulename}.ui        # UI composables (if any)
com.augmentalis.{modulename}.handler   # Voice command handlers (if any)
```

Package names use the **module ID** (lowercase, no "avanue" separation), not the display name:
- Module: PhotoAvanue -> Package: `com.augmentalis.photoavanue`
- Module: AvanueUI -> Package: `com.augmentalis.avanueui`
- Module: VoiceOSCore -> Package: `com.augmentalis.voiceoscore`

---

## 5. SpatialVoice Design Language

### 5.1 What Is SpatialVoice?

**SpatialVoice** is the name of the UI design language used across all Avanues applications. It describes the visual aesthetic — not a product or API.

| Concept | Name | Scope |
|---|---|---|
| Design language | **SpatialVoice** | The aesthetic philosophy and visual rules |
| Theme API | **AvanueTheme** | The Compose API that implements SpatialVoice |
| Theme engine | **AvanueUI** | The KMP module containing theme infrastructure |

### 5.2 SpatialVoice Visual Signature

The SpatialVoice design language creates a **pseudo-spatial glass UI** — depth and layering cues that suggest 3D without requiring actual 3D rendering:

| Element | Implementation |
|---|---|
| Background | `verticalGradient(background, surface.copy(0.6f), background)` |
| TopAppBar | `containerColor = Color.Transparent` or `AvanueTheme.colors.surface` |
| Cards | Glass/Water material effects via `AvanueTheme.glass.*` / `AvanueTheme.water.*` |
| Elevation | Material-appropriate (Glass: blur, Water: tint, Cupertino: hairline, MountainView: tonal) |
| Colors | Always `AvanueTheme.colors.*` — NEVER `MaterialTheme.colorScheme.*` |

### 5.3 Module Accent Colors

Each Avanue has a designated accent color for visual identity within the hub:

| Module | Accent Token |
|---|---|
| VoiceTouch | `AvanueTheme.colors.success` (green) |
| WebAvanue | `AvanueTheme.colors.info` (blue) |
| CursorAvanue | `AvanueTheme.colors.warning` (amber) |
| AvanueHUB | `AvanueTheme.colors.tertiary` |
| PDFAvanue | `AvanueTheme.colors.error` (red) |
| ImageAvanue | `AvanueTheme.colors.primary` |
| VideoAvanue | `AvanueTheme.colors.secondary` |
| NoteAvanue | `AvanueTheme.colors.success` |
| PhotoAvanue | `AvanueTheme.colors.info` |
| CastAvanue | `AvanueTheme.colors.warning` |
| DrawAvanue | `AvanueTheme.colors.tertiary` |

Accent colors are resolved at runtime via `moduleAccentColor(moduleId)` in `HubModule.kt`.

---

## 6. Trademark & Legal

### 6.1 Marks

| Mark | Type | Symbol | Usage |
|---|---|---|---|
| VoiceOS | Registered trademark | (R) | Required on first prominent use per page/screen |
| VoiceTouch | Trademark | (TM) | Required on first prominent use per page/screen |
| Avanues | Brand name | None currently | Use as-is; consider (TM) filing |
| CursorAvanue | Product name | None | Use as-is |
| WebAvanue | Product name | None | Use as-is |
| AvanueHUB | Product name | None | Use as-is |
| SpatialVoice | Design language name | None | Internal use; not user-facing |

### 6.2 Trademark Rules in Code

```kotlin
// CORRECT — Unicode escape for trademark symbols in strings
displayName = "VoiceTouch\u2122"        // ™ (U+2122)
text = "VoiceOS\u00AE Avanues EcoSystem" // ® (U+00AE)

// INCORRECT — Never hardcode the symbol directly (encoding issues)
displayName = "VoiceTouch™"             // May break in some encodings
```

### 6.3 Copyright

```
(C) 2018-{CURRENT_YEAR}
Intelligent Devices LLC and Augmentalis Inc.
All rights reserved.
```

- Copyright year range starts at **2018** (project inception)
- End year is **always dynamic**: `Calendar.getInstance().get(Calendar.YEAR)`
- Both entities listed: **Intelligent Devices LLC** AND **Augmentalis Inc.**

### 6.4 Credits Line

The official credits format used in About screens and footers:

```
VoiceOS(R) Avanues EcoSystem

Imagined, Designed & Written by
Manoj Jhawar with Aman Jhawar

(C) 2018-{year}
Intelligent Devices LLC and Augmentalis Inc.

Designed and Created in California with Love.
```

### 6.5 File Headers

Every source file begins with:

```kotlin
/*
 * Copyright (c) {YEAR} Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */
```

---

## 7. Voice Command Branding

### 7.1 Module Open Commands

Voice commands to open modules use the **display name** (spoken naturally):

| Command | Spoken Form | Notes |
|---|---|---|
| Open VoiceTouch | "open voice touch" | No trademark in speech |
| Open WebAvanue | "open web avanue" | Spoken as "web ah-vah-new" |
| Open PhotoAvanue | "open photo avanue" | Alt: "open camera" (synonym) |
| Open AvanueHUB | "open avanue hub" | Alt: "open cockpit" (legacy synonym) |

### 7.2 VOS Command Prefix Mapping

| Prefix | Category | Brand Module |
|---|---|---|
| `cam_` | CAMERA | PhotoAvanue |
| `cockpit_` | COCKPIT | AvanueHUB |
| `web_` | BROWSER / WEB_GESTURE | WebAvanue |
| `cursor_` | CURSOR | CursorAvanue |
| `media_` | MEDIA | VoiceTouch (global media controls) |
| `screen_` | SCREEN | VoiceTouch (global screen controls) |

---

## 8. Platform-Specific Brand Expression

### 8.1 Android

- App name in launcher: **Avanues** (consolidated app)
- Activity aliases provide dual launcher icons: `.VoiceAvanueAlias`, `.WebAvanueAlias`
- Notification channels use module display names
- `applicationId = "com.augmentalis.avanues"`

### 8.2 iOS

- App name: **Avanues**
- Hub footer: `"VoiceOS\u{00AE} Avanues EcoSystem"`
- SwiftUI views use AvanueUI theme bridge for SpatialVoice consistency
- Bundle identifier: `com.augmentalis.avanues`

### 8.3 Desktop (JVM)

- Window title: **Avanues**
- Compose Desktop uses same AvanueTheme as Android
- No separate launcher icons — single window with hub navigation

---

## 9. Anti-Patterns

### 9.1 Spelling Errors (ZERO TOLERANCE)

| Wrong | Right | Why |
|---|---|---|
| Avenues | **Avanues** | Brand name is intentionally different |
| Avenue | **Avanue** | Singular follows the same rule |
| AvanueHub | **AvanueHUB** | HUB is always uppercase |
| VoiceOS Avenues | **VoiceOS Avanues** | Ecosystem name uses brand spelling |
| Avanue UI | **AvanueUI** | No space — it is one compound word |

### 9.2 Branding Misuse

| Wrong | Right | Why |
|---|---|---|
| `FoundationAvanue` | `Foundation` | Infrastructure modules have no brand suffix |
| `Avanue.Database` | `Database` | Databases are infrastructure |
| `The Cockpit` | `AvanueHUB` | "Cockpit" is the internal ID, not the display name |
| `MaterialTheme.colorScheme.*` | `AvanueTheme.colors.*` | SpatialVoice requires AvanueTheme |
| `"Made by AI"` | `"Manoj Jhawar with Aman Jhawar"` | No AI attribution, ever |

### 9.3 Common Mistakes in Documentation

| Wrong | Right |
|---|---|
| "the WebAvanue module provides avenues for..." | "the WebAvanue module provides..." (avoid using "avenue" as common noun alongside the brand) |
| "VoiceOS(TM)" | "VoiceOS(R)" ((R) not (TM) — it's registered) |
| "open the Photo Avanue app" | "open PhotoAvanue" (one word, no article needed) |

---

## 10. Adding a New Avanue Module

When creating a new content module for the Avanues ecosystem:

### Step 1: Name It

Apply the decision tree from Section 4.1:
1. Is it user-facing? YES
2. Does the user navigate to it? YES
3. Name: `[Content]Avanue` (e.g., `MusicAvanue`, `MapAvanue`)

### Step 2: Register in HubModule.kt

```kotlin
HubModule(
    id = "musicavanue",                    // lowercase, no separator
    displayName = "MusicAvanue",           // CamelCase brand name
    subtitle = "Audio avenue",             // "[Purpose] avenue" pattern
    icon = Icons.Default.MusicNote,
    orbit = OrbitTier.CONTENT,             // CORE or CONTENT
    route = AvanueMode.COCKPIT.route       // Content modules route through hub
)
```

### Step 3: Assign Accent Color

Add entry to `moduleAccentColor()` in `HubModule.kt`:

```kotlin
"musicavanue" -> AvanueTheme.colors.secondary
```

### Step 4: Create VOS Commands

Add commands to all 5 locale `.app.vos` files with the appropriate prefix:
- Prefix: `music_` (short, unique)
- VosParser: `CATEGORY_MAP["music"] = "MUSIC"`
- Minimum commands: `music_open`, `music_play`, `music_pause`, `music_next`, `music_prev`

### Step 5: Follow Self-Running Pattern

Every content Avanue provides two composables:
- `{Module}Screen()` in **commonMain** — standalone full-screen experience
- `{Module}Content()` or `{Module}Preview()` in **androidMain** — embeddable view for AvanueHUB frames

---

## 11. Pronunciation Guide

For voice commands and user communication:

| Brand | Pronunciation | IPA |
|---|---|---|
| Avanues | ah-VAN-yooz | /əˈvanjuːz/ |
| Avanue | ah-VAN-yoo | /əˈvanjuː/ |
| AvanueHUB | ah-VAN-yoo hub | /əˈvanjuː hʌb/ |
| VoiceOS | voyss-oh-ess | /vɔɪs oʊ ɛs/ |
| VoiceTouch | voyss-tutch | /vɔɪs tʌtʃ/ |
| SpatialVoice | SPAY-shul voyss | /ˈspeɪʃəl vɔɪs/ |
| Augmentalis | awg-men-TAL-iss | /ɔːɡmɛnˈtælɪs/ |

---

## 12. Quick Reference Card

```
PLATFORM:     VoiceOS(R)
ECOSYSTEM:    Avanues EcoSystem
HUB:          AvanueHUB
CORE:         VoiceTouch(TM) | WebAvanue | CursorAvanue
CONTENT:      Photo | PDF | Image | Video | Note | Draw | Cast + "Avanue"
DESIGN:       SpatialVoice (design language) / AvanueUI (code API)
INFRA:        Foundation | Database | Logging | DeviceManager (NO suffix)
CREDITS:      "Designed and Created in California with Love."
COPYRIGHT:    (C) 2018-{year} Intelligent Devices LLC and Augmentalis Inc.
SPELLING:     Avanues (NOT Avenues) | Avanue (NOT Avenue)
```

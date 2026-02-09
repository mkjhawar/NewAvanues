---
title: "Chapter 04 — AvanueUI Deep Dive"
owner: "Design Systems"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 04 — AvanueUI Deep Dive

## 4.1 Purpose

AvanueUI is the unified design system for the NewAvanues ecosystem, providing reusable tokens, themes, responsive display profiles, and glass components.

## 4.2 Consolidation Model

Current architecture converges around `:Modules:AvanueUI` as the primary dependency and composition surface.

```kotlin
implementation(project(":Modules:AvanueUI"))
```

This reduces cross-module UI duplication and centralizes design governance.

## 4.3 Token System (Static + Predictable)

Core token groups:
- Spacing
- Shape
- Size
- Elevation
- Animation
- Glass
- Responsive
- Typography

Design intent:
- no runtime token mutation surprises
- deterministic rendering baselines
- easier cross-platform scaling behavior

## 4.4 Theme Architecture

`AvanueThemeProvider` establishes:
1. Material3 compatibility
2. Avanue color + glass locals
3. Display profile-aware density

```text
Theme Provider
  -> colors
  -> glass scheme
  -> display profile
  -> density scaling
  -> child UI tree
```

## 4.5 Display Profiles and Adaptive UI

Profiles (e.g., PHONE/TABLET and glass form factors) provide scaling and layout strategy control.

This is critical for VoiceTouch™ since spoken interaction needs predictable target geometry and readable visual feedback across form factors.

## 4.6 Glass UI Model

Glass primitives use level-based intensity and reusable component wrappers.

| Level | Typical Use |
|---|---|
| LIGHT | subtle panels/backgrounds |
| MEDIUM | default cards/surfaces |
| HEAVY | modal/overlay emphasis |

The design system supports both quick-level APIs and advanced config overrides.

## 4.7 Settings Provider Pattern

The settings architecture is modular:

```text
Feature module
  -> contributes SettingsProvider
  -> injected into provider set
  -> rendered in unified settings UI
```

This allows feature teams to extend settings without central monolith edits.

## 4.8 VoiceTouch™ + AvanueUI Contract

VoiceTouch™ UX depends on AvanueUI for:
1. interaction affordance consistency,
2. state visibility,
3. confirmation and error presentation,
4. touch target guarantees for mixed-mode interaction.

## 4.9 Common Pitfalls

| Pitfall | Guidance |
|---|---|
| legacy package imports | migrate to `com.augmentalis.avanueui.*` |
| ad-hoc spacing values | prefer `SpacingTokens` |
| non-profile-aware layout assumptions | use display profile strategy |
| duplicated glass style definitions | centralize via presets/config objects |

## 4.10 Engineering Checklist

Before shipping AvanueUI-dependent features:
- [ ] Uses canonical module dependency
- [ ] Uses tokenized spacing/sizing
- [ ] Verified in at least two display profiles
- [ ] Voice interaction states visually represented
- [ ] Settings integration uses provider pattern

## 4.11 Lineage: AvaMagic/AvaUI -> AvanueUI

AvanueUI is the current canonical design-system module, but it evolved from earlier AvaMagic/AvaUI workstreams documented in legacy master docs.

### Evolution summary

| Era | Primary Label | Characteristics | Current Status |
|---|---|---|---|
| Early suite era | AvaMagic / AvaUI | broad suite-level language, platform vision docs, component narratives | historical context |
| Consolidation era | AvanueUI module convergence | centralized KMP module + token/theming governance | active canonical path |

### Practical interpretation for engineers

1. Legacy AvaMagic/AvaUI docs remain valuable for product rationale and original design intent.
2. Current implementation authority for engineering is `:Modules:AvanueUI`.
3. New component work should target AvanueUI APIs/tokens and not reintroduce legacy naming patterns.

## 4.12 Migration Rules from Legacy Naming

| Legacy Pattern | Current Guidance |
|---|---|
| AvaUI/AvaMagic package references | migrate to `com.augmentalis.avanueui.*` |
| ad-hoc or duplicated glass styles | move to shared AvanueUI glass presets/config |
| legacy custom color constants | map to AvanueUI tokens + theme locals |
| per-feature settings screens | use SettingsProvider contribution model |

## 4.13 Governance Contract

When updating AvanueUI architecture:

1. Update this chapter for implementation-level truth.
2. Add/adjust rationale in Chapter 08 (ADR Index).
3. If developer workflow changes, update Chapter 09 runbook.
4. If legacy mapping assumptions change, update Chapter 10 AI/stack references where UI-state contracts are impacted.

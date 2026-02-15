# Implementation Plan: Localized Verb Extraction for Dynamic Commands

**Date**: 2026-02-12
**Branch**: IosVoiceOS-Development
**Status**: Planning
**Flags**: .tot .cot .yolo .implement

## Overview

Platforms: KMP (commonMain) + Android
Swarm Recommended: No (< 15 tasks, single platform)
Estimated: 6 tasks

## Problem

`ActionCoordinator.actionVerbs` and `SynonymRegistry` are hardcoded English. When a Spanish user says `"pulsar 4"`, the verb extraction fails because `"pulsar"` isn't recognized as a click verb. The VOS files already contain localized verb phrases (acc_click, acc_long_click) for all 5 locales — they're just not wired into the verb extraction system.

### Localized Verb Data (Already in VOS Files)

| Locale | Click Verbs | Long Press Verbs |
|--------|-------------|------------------|
| en-US | click, tap, press | long press, long click, press and hold, hold |
| es-ES | pulsar, clic, tocar, toca | mantener pulsado, pulsación larga, pulsar y mantener, toque largo |
| fr-FR | cliquer, appuyer, toucher, taper | appui long, clic long, maintenir, rester appuyé |
| de-DE | klicken, tippen, drücken | lang drücken, langer Druck, gedrückt halten, halten |
| hi-IN | click karo, tap karo, dabao | der tak dabao, long press karo, long click karo, daba kar rakho |

## ToT: Approach Selection

| Branch | Approach | Viability | Selected |
|--------|----------|-----------|----------|
| A | Populate actionVerbs from StaticCommandRegistry at runtime | HIGH | |
| B | Add verb_map section to VOS format v2.2 | MEDIUM | |
| C | Make SynonymRegistry locale-aware from VOS | LOW | |
| **D** | **Hybrid: Extract verbs from VOS verb-type commands + canonical mapping** | **HIGH** | **✓** |

### Why Branch D

- Zero VOS format changes (uses existing acc_click/acc_long_click entries)
- Automatic: verb list updates when locale changes
- Canonical mapping: "pulsar" → "click" so handler routing still works in English
- KMP-compatible: LocalizedVerbProvider in commonMain, populated from platform

## Phases

### Phase 1: KMP Foundation — LocalizedVerbProvider (commonMain)

**Task 1**: Create `LocalizedVerbProvider` in commonMain
- Location: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/LocalizedVerbProvider.kt`
- Data class `LocalizedVerb(localizedPhrase: String, canonicalVerb: String, actionType: CommandActionType)`
- Singleton with `updateVerbs(verbs: List<LocalizedVerb>)` and `getActionVerbs(): List<String>`
- Method `canonicalVerbFor(localizedVerb: String): String?` — maps "pulsar" → "click"
- Method `actionTypeForVerb(localizedVerb: String): CommandActionType?` — maps "pulsar" → CLICK
- Built-in English defaults (so system works even before VOS loads)

**Task 2**: Wire into `ActionCoordinator.extractVerbAndTarget()`
- Replace hardcoded `actionVerbs` list with `LocalizedVerbProvider.getActionVerbs()`
- When verb is extracted, use `canonicalVerbFor()` to normalize for handler routing
- Preserve existing behavior: if localized verb found, rewrite to canonical for handlers

### Phase 2: Android Wiring — Populate from StaticCommandRegistry

**Task 3**: Extract verb phrases from StaticCommandRegistry on init
- In `CommandManager.populateStaticCommandRegistry()` (after `StaticCommandRegistry.initialize()`):
  - Find commands with verb-type IDs: `acc_click`, `acc_long_click`
  - Extract all phrases as localized verbs with canonical mapping
  - Call `LocalizedVerbProvider.updateVerbs()`
- Verb command ID → canonical mapping:
  - `acc_click` → canonical `"click"`, actionType `CLICK`
  - `acc_long_click` → canonical `"long press"`, actionType `LONG_CLICK`

**Task 4**: React to locale changes
- When `CommandLocalizer.setLocale()` triggers re-population of StaticCommandRegistry, the verb extraction in Task 3 auto-runs (it's in the same flow)
- No additional wiring needed — locale change → DB reload → registry init → verb extraction

### Phase 3: SynonymRegistry Enhancement

**Task 5**: Add locale-aware synonym injection
- Add `SynonymRegistry.addLocalizedVerbs(verbs: List<LocalizedVerb>)` method
- For each localized verb, if it's not already in the synonym list for its canonical verb, add it
- E.g., canonical "click" gains synonyms ["pulsar", "clic", "tocar"] for es-ES
- Called from same init path as Task 3

### Phase 4: Verification

**Task 6**: Build verification + fix doc update
- `./gradlew :Modules:VoiceOSCore:compileDebugKotlin :apps:avanues:compileDebugKotlin`
- Update fix doc with implementation details

## File Summary

| Action | File | Change |
|--------|------|--------|
| CREATE | `VoiceOSCore/src/commonMain/.../command/LocalizedVerbProvider.kt` | New singleton: localized verb registry |
| MODIFY | `VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt` | Replace hardcoded actionVerbs with LocalizedVerbProvider |
| MODIFY | `VoiceOSCore/src/androidMain/.../CommandManager.kt` | Extract verbs from StaticCommandRegistry after init |
| MODIFY | `VoiceOSCore/src/commonMain/.../command/SynonymRegistry.kt` | Add locale-aware synonym injection |

## CoT: Data Flow After Implementation

```
VOS file loads (es-ES.app.vos)
  ↓
CommandLoader seeds DB → Commands_static table
  ↓
CommandManager.populateStaticCommandRegistry()
  ↓
StaticCommandRegistry.initialize(commands)
  ↓
Extract acc_click phrases: ["pulsar", "clic", "tocar", "toca"]
  ↓
LocalizedVerbProvider.updateVerbs([
  LocalizedVerb("pulsar", "click", CLICK),
  LocalizedVerb("clic", "click", CLICK),
  LocalizedVerb("tocar", "click", CLICK),
  ...
])
  ↓
SynonymRegistry.addLocalizedVerbs(same list)
  ↓
User says "pulsar 4"
  ↓
ActionCoordinator.extractVerbAndTarget("pulsar 4")
  → verbs = LocalizedVerbProvider.getActionVerbs()  // includes "pulsar"
  → verb = "pulsar", target = "4"
  ↓
canonicalVerb = LocalizedVerbProvider.canonicalVerbFor("pulsar") → "click"
  ↓
actionPhrase = "click 4"  // canonical for handler routing
  ↓
Routes to AndroidGestureHandler → taps element 4 ✓
```

## Risk Assessment

- **Low risk**: Additive change, no existing behavior modified
- **Fallback**: English defaults in LocalizedVerbProvider ensure system works pre-VOS-load
- **Edge case**: Multi-word verbs in hi-IN ("click karo", "tap karo") need longest-match-first sorting (already done in extractVerbAndTarget)

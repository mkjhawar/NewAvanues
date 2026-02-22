# AVACode Recipe System — Pending Work Items

**Module**: AVACode (cross-module)
**Type**: Plan (Pending/TODO)
**Date**: 2026-02-22
**Version**: V1
**Status**: PENDING — Requires dedicated session
**Author**: Manoj Jhawar

---

## Vision

AVACode is a **code reduction DSL platform** where pre-compiled recipes let developers compose complex functionality in a few lines. Each module in the Avanues ecosystem gets its own recipe library — developers pick recipes, customize via DSL, and get full functionality without understanding the underlying complexity.

The App Store bypass angle: `.avp` text-based recipe files invoke pre-compiled code at runtime. No dynamic code loading (which Apple/Google restrict), just DSL-driven configuration of compiled modules.

---

## Prerequisites (MUST DO FIRST)

### P0: Fix AVACode P1 Bugs

| Bug | File | Fix |
|-----|------|-----|
| JVM API in commonMain | `WorkflowInstance.kt:94`, `WorkflowPersistence.kt:30` | Replace `System.currentTimeMillis()` with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` |
| Mutable state in data class | `WorkflowInstance.kt` | `MutableMap<String, StepState>` + `MutableList<WorkflowTransition>` in data class breaks value semantics. Use immutable collections + rebuild on transition. |
| Fragile commit() cast | `FormBinding.kt:136-138` | Casts `initialData: Map` to `MutableMap` — depends on stdlib returning `LinkedHashMap` from `toMap()`. Change to `private val initialData: MutableMap = data.toMutableMap()` |

### P0: Reactivate DSL Parser

The `.avp` runtime loading pattern requires the DSL parser (`Modules/AVACode/src/commonMain/kotlin/com/augmentalis/avacode/dsl/`) which is currently `.disabled`. Must:
1. Audit disabled parser code for quality/completeness
2. Fix any issues found
3. Remove `.disabled` extension
4. Verify it can parse recipe DSL text into executable configurations

---

## Module Recipe Libraries (PENDING)

Each module gets a recipe library. Recipes are composable — a single app can use recipes from multiple modules.

### 1. HTTPAvanue Recipes

**Package**: `com.augmentalis.avacode.recipes.http`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `avaServer("rest_api")` | HttpServer + CORS + rate limit + auth + error handler + body parser + timing + metrics + logging — all wired in correct order | ~30 → 3 |
| `avaServer("streaming")` | HttpServer + WebSocket handler + health endpoint + status endpoint | ~25 → 3 |
| `avaServer("file_server")` | HttpServer + StaticFileMiddleware + ETag + Range + Compression | ~20 → 3 |
| `avaServer("voice_api")` | HttpServer + Voice Routes + AVID responses + VOS export | ~35 → 4 |
| `mountForm(form)` | POST validation + GET schema + POST completion — from FormDefinition | ~40 → 1 |
| `mountWorkflow(workflow)` | Full stateful REST API (create/next/back/skip/jump/cancel/progress/history) | ~60 → 1 |

**Depends on**: HTTPAvanue v2.0 completion (Phases 1-9)

### 2. VoiceOSCore Recipes

**Package**: `com.augmentalis.avacode.recipes.voice`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `voiceHandler("category")` | CommandHandler with phrases, locales, dispatch — registered in HandlerFactory | ~80 → 5 |
| `voiceOverlay("name")` | Overlay with numbered badges, voice-tap targets, AVID identifiers | ~60 → 4 |
| `vosProfile("app_name")` | .vos file with commands, tier routing, category grouping | ~100 → 8 |
| `voiceForm(form)` | Voice-driven form fill (dictation per field, confirmation, correction) | ~120 → 5 |

**Depends on**: VoiceOSCore handlers system, AVID system

### 3. Cockpit Recipes

**Package**: `com.augmentalis.avacode.recipes.cockpit`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `cockpitLayout("name")` | Multi-window layout definition with content type mapping | ~50 → 4 |
| `cockpitTemplate("name")` | Pre-configured template (lecture, meeting, research, etc.) | ~40 → 3 |
| `cockpitWidget("name")` | Content panel with resize, minimize, voice commands | ~70 → 5 |
| `spatialCanvas("name")` | Head-tracking canvas with IMU integration | ~90 → 4 |

**Depends on**: Cockpit SpatialVoice system (Chapter 97)

### 4. WebAvanue Recipes

**Package**: `com.augmentalis.avacode.recipes.web`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `webView("name")` | WebView with DOMScraperBridge + voice commands + AVID overlay | ~60 → 3 |
| `browserTab("name")` | Full browser tab with address bar, navigation, bookmarks | ~100 → 5 |
| `webForm("form")` | DOM-injected form with voice fill + validation | ~80 → 4 |

**Depends on**: WebAvanue DOMScraperBridge, web scraping system

### 5. NoteAvanue Recipes

**Package**: `com.augmentalis.avacode.recipes.note`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `noteEditor("name")` | Rich text editor with voice commands + format detection | ~80 → 4 |
| `noteTemplate("type")` | Pre-configured note type (meeting, lecture, checklist) | ~40 → 2 |
| `noteSync("provider")` | Sync configuration (SFTP, local, cloud) | ~50 → 3 |

**Depends on**: NoteAvanue Chapter 100, compose-rich-editor

### 6. RemoteCast Recipes

**Package**: `com.augmentalis.avacode.recipes.cast`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `castServer("name")` | WebSocket server for screen streaming + VOCAB sync | ~60 → 3 |
| `castClient("name")` | WebSocket client with auto-reconnect + frame decoding | ~50 → 3 |
| `glassProfile("device")` | Device-specific configuration (resolution, color mode, refresh rate) | ~30 → 2 |

**Depends on**: RemoteCast module, HTTPAvanue WebSocket

### 7. AVID Recipes

**Package**: `com.augmentalis.avacode.recipes.avid`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `avidElement("type", "label")` | AVID-tagged interactive element with voice command | ~10 → 1 |
| `avidForm(form)` | Form with AVID identifiers on all fields | ~30 → 1 |
| `avidList(items)` | List with indexed AVID badges (voice: "click item 3") | ~20 → 1 |

**Depends on**: Unified AVID system (ElementFingerprint)

### 8. AvanueUI Recipes

**Package**: `com.augmentalis.avacode.recipes.ui`

| Recipe | What It Generates | Lines Saved |
|--------|-------------------|-------------|
| `avanueScreen("name")` | Full screen with AvanueTheme, SpatialVoice gradient, TopAppBar | ~40 → 3 |
| `settingsScreen("name")` | Adaptive settings with ListDetailPaneScaffold + providers | ~80 → 5 |
| `themePreview()` | Live preview of all 32 palette/style/appearance combos | ~60 → 2 |

**Depends on**: AvanueUI v5.1 theme system (Chapters 91-92)

---

## Architecture

### Recipe Structure

```
Modules/AVACode/src/commonMain/kotlin/com/augmentalis/avacode/
├── forms/           # Active — Forms DSL
├── workflows/       # Active — Workflows DSL
├── recipes/         # NEW — Recipe libraries
│   ├── RecipeDefinition.kt     # Base recipe interface
│   ├── RecipeRegistry.kt       # Global recipe discovery
│   ├── http/                   # HTTPAvanue recipes
│   ├── voice/                  # VoiceOSCore recipes
│   ├── cockpit/                # Cockpit recipes
│   ├── web/                    # WebAvanue recipes
│   ├── note/                   # NoteAvanue recipes
│   ├── cast/                   # RemoteCast recipes
│   ├── avid/                   # AVID recipes
│   └── ui/                     # AvanueUI recipes
├── dsl/             # REACTIVATE — DSL parser for .avp files
└── generators/      # REACTIVATE — Code output (future)
```

### Recipe Interface

```kotlin
interface RecipeDefinition<Config, Output> {
    val id: String
    val category: String  // "http", "voice", "cockpit", etc.
    val description: String

    fun configure(builder: Config.() -> Unit): Config
    fun build(config: Config): Output
}
```

### .avp File Format (Runtime Loading)

```
@recipe http.rest_api
port: 8080
secure: true
cors: permissive
rate_limit: 100/min
mount_form: contact
mount_form: feedback
voice: enabled
---
```

The DSL parser reads `.avp` files → matches recipe IDs → invokes compiled `build()` methods. No dynamic code generation — just configuration-driven activation of pre-compiled functionality.

### Dependency Graph

```
App (Avanues/Cockpit/etc.)
  └── AVACode (recipes/)
        ├── HTTPAvanue (optional, for http recipes)
        ├── VoiceOSCore (optional, for voice recipes)
        ├── Cockpit (optional, for cockpit recipes)
        ├── WebAvanue (optional, for web recipes)
        ├── NoteAvanue (optional, for note recipes)
        ├── RemoteCast (optional, for cast recipes)
        ├── AvanueUI (optional, for ui recipes)
        └── Foundation (always, for platform abstractions)
```

Each recipe package has an **optional dependency** on its target module. If the module isn't in the dependency graph, those recipes simply aren't available. This avoids forcing all modules into every app.

---

## Implementation Order (Suggested)

| Phase | Work | Prerequisite | Est. |
|-------|------|-------------|------|
| 0 | Fix AVACode P1 bugs (3 issues) | None | 1 hr |
| 1 | RecipeDefinition + RecipeRegistry base interfaces | Phase 0 | 2 hrs |
| 2 | HTTPAvanue recipes (avaServer + mountForm + mountWorkflow) | HTTPAvanue v2.0 | 4 hrs |
| 3 | AVID recipes (avidElement + avidForm + avidList) | Phase 1 | 2 hrs |
| 4 | VoiceOSCore recipes (voiceHandler + voiceOverlay + vosProfile) | Phase 1 | 4 hrs |
| 5 | AvanueUI recipes (avanueScreen + settingsScreen) | Phase 1 | 3 hrs |
| 6 | Cockpit recipes (cockpitLayout + cockpitTemplate) | Phase 1 | 3 hrs |
| 7 | WebAvanue + NoteAvanue + RemoteCast recipes | Phase 1 | 4 hrs |
| 8 | DSL parser reactivation + .avp file loading | Phase 0 | 6 hrs |
| 9 | Integration testing across all recipe libraries | All phases | 4 hrs |

**Total estimated**: ~33 hours across multiple sessions

---

## Success Criteria

1. A developer can create a full REST API server with auth, CORS, rate limiting, and voice commands in under 10 lines
2. A developer can mount an AVACode form and get validation + schema + completion endpoints in 1 line
3. A developer can define a multi-step workflow and get a stateful REST API in 1 line
4. `.avp` files can configure server endpoints at runtime without code changes
5. Every recipe produces elements with AVID voice identifiers
6. No recipe requires understanding the underlying module's internal architecture
7. All recipes work cross-platform (Android + iOS + Desktop)

---

## Open Questions (For Dedicated Session)

1. **Recipe dependency management**: Should each recipe package be a separate Gradle module, or should AVACode have optional compile-time dependencies?
2. **Recipe versioning**: How do we version recipes independently from their target modules?
3. **Recipe discovery**: Should recipes self-register via ServiceLoader/KSP, or use a manual registry?
4. **Conflict resolution**: What happens when two recipes configure the same middleware differently?
5. **Recipe composition**: Can recipes extend or wrap other recipes? (e.g., voice_api = rest_api + voice recipes)
6. **.avp security**: How do we validate `.avp` files loaded at runtime? Signing? Sandboxing?
7. **Recipe testing**: Should each recipe come with a test recipe that validates the generated output?

---

## Related Documents

- HTTPAvanue NanoHTTPD Comparison: `docs/analysis/HTTPAvanue/HTTPAvanue-Analysis-NanoHTTPDFeatureComparison-260222-V1.md`
- HTTPAvanue v2.0 Plan: `docs/plans/HTTPAvanue/HTTPAvanue-Plan-OkioEliminationAndEnhancements-260222-V1.md` (pending)
- AVACode Quality Review: `Docs/reviews/AVID-AVU-AVACode-Review-QualityAnalysis-260222-V1.md`
- AVACode Deep Dive (Chapter 11): `Docs/MasterDocs/NewAvanues-Developer-Manual/11-AVACode-Deep-Dive.md`
- AVACode Plugin Integration Architecture: `Docs/Avanues/Avanues-AVACode-Plugin-Integration-Architecture-52111-V1.md`

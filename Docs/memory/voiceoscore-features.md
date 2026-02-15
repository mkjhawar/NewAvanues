# VoiceOSCore Feature Details

## ModuleAccent System (260210)
- Branch: `VoiceOSCore-KotlinUpdate`
- `AvanueModuleAccents` static registry in AvanueUI/theme/ModuleAccent.kt
- Dual access: Compose via `AvanueTheme.moduleAccent(id)`, non-Compose via `AvanueModuleAccents.get(id)`
- SideEffect in AvanueThemeProvider syncs global colors to static registry
- `SettingsColorRow` component: preset color picker dialog (10 curated accents)
- VoiceCursor integrated: overlay reads colors from CursorConfig which reads from ModuleAccent
- `CursorConfig` expanded: borderColor, dwellRingColor, cursorAlpha, borderStrokeWidth, dwellRingStrokeWidth, cursorRadius
- VoiceCursor has AvanueUI dependency in androidMain build.gradle.kts
- Cursor settings toggle: `cursorEnabled` in AvanuesSettingsRepository (default: false)
- Settings wiring: AccessibilityService observes DataStore, starts/stops CursorOverlayService, pushes config

## Cursor Voice Commands (FIXED - 260210)
- `AndroidCursorHandler` (IHandler/BaseHandler) in VoiceOSCore/src/androidMain/
- Registered in AndroidHandlerFactory alongside AndroidGestureHandler, SystemHandler, AppHandler
- Phrases: show/hide/enable/disable cursor, cursor on/off, cursor click, click here
- ActionCategory.GAZE, routes to CursorOverlayService start/stop/performClickAtCurrentPosition
- AccessibilityClickDispatcher wired to CursorOverlayService in settings observation loop
- Legacy CursorCommandHandler/CursorActions left as-is (not removed)
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-CursorVoiceCommands-260210-V1.md`

## 4-Tier Voice Enablement (260211)
- **Architecture**: Tier 1 (Our apps: AVID + static + Voice hints) → Tier 2 (Developer convention: `(Voice: phrase)` in contentDescription) → Tier 3 (Auto accessibility scraping) → Tier 4 (Voice profiles: .VOS files)
- **AVID**: `hash(packageName + className + resourceId + text + contentDescription)` — NO bounds, device-independent, packageName for cross-app uniqueness
- **Two-Layer Label Normalizer** in `CommandGenerator.normalizeRealWearMlScript()`: Layer 1 = `(Voice: ...)` regex, Layer 2 = delimiter split with <2 char guard
- **`(Voice: ...)` Convention**: Tier 2 developer ecosystem entry point — do NOT remove; works in Compose/Flutter/RN/Unity
- **.VOS Format**: AVU compact wire protocol. Codes: ELM (element), DIS (disambiguation), CAT (category), SYN (synonyms), ACT (action), IGN (ignore)
- **DIS Hybrid Disambiguation**: 3 layers — hierarchy path (`h=`), semantic zone (`z=`), parent-extended hash (`p=`)
- **No bounds in .VOS**: BoundsResolver uses LIVE accessibility tree at runtime — profiles are device-portable
- **Analysis**: `docs/analysis/VoiceOSCore/VoiceOSCore-Analysis-4TierVoiceEnablement-260211-V1.md`
- **Guide**: `Docs/MasterDocs/AvanueUI/AvanueUI-VoiceEnablement-Guide.md`

## DB-Driven Voice Commands (Phase 2, 260211)
- **Architecture**: VOS seed files → CommandLoader → commands_static (SQLDelight) → StaticCommandRegistry → consumers
- **VOS v2.0 format**: Compact JSON arrays + root-level `category_map`, `action_map`, `meta_map`
- **Seed file**: `apps/avanues/src/main/assets/localization/commands/en-US.VOS` (107 commands)
- **StaticCommandRegistry**: `_dbCommands` cache + `initialize(commands)` + hardcoded fallback if DB empty
- **CommandManager.populateStaticRegistryFromDb()**: Converts VoiceCommandEntity → StaticCommand after DB load
- **HelpCommandDataProvider**: Derives from StaticCommandRegistry + static template commands (parametric patterns)
- **VoiceCommandEntity.resolvedAction**: v2.0 `actionType`, falls back to `id` for v1.0
- **CommandLoader version**: "2.0" → "2.1" (dual-file loading), getAvailableLocales filters `.VOS`
- **To add new locale**: Create `{locale}.VOS` in `assets/localization/commands/`, CommandLoader auto-discovers
- **Fix doc**: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-Phase2DBDrivenCommands-260211-V1.md`

## Multi-Locale VOS (260211)
- Branch: `VoiceOSCore-KotlinUpdate`, commit: 01edc162
- 5 locales: en-US, es-ES, fr-FR, de-DE, hi-IN (Hindi)
- VOS files: `apps/avanues/src/main/assets/localization/commands/{locale}.VOS`
- All 107 commands per locale, identical IDs/maps, only phrases/descriptions translated
- DataStore key: `voice_command_locale` (default: "en-US")
- Settings: `VoiceControlSettingsProvider.SUPPORTED_LOCALES` (5 entries)
- Locale switch flow: DataStore → AccessibilityService observer → `CommandManager.switchLocale()` → `forceReload()` → `StaticCommandRegistry.reset()` → `populateStaticRegistryFromDb()`
- To add locale: create `{locale}.VOS` + add to `SUPPORTED_LOCALES`
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-MultiLocaleVOSSupport-260211-V1.md`

## Static Command Dispatch (260211)
- 7 new handlers in `VoiceOSCore/src/androidMain/.../handlers/`:
  - MediaHandler (MEDIA): play/pause/next/prev/volume — AudioManager
  - ScreenHandler (DEVICE): brightness/wifi/bluetooth/screenshot/flashlight — System APIs
  - TextHandler (INPUT): select all/copy/paste/cut/undo/redo/delete — AccessibilityNodeInfo
  - InputHandler (INPUT): show/hide keyboard — SoftKeyboardController
  - AppControlHandler (APP): close/exit app — GLOBAL_ACTION_BACK + HOME
  - ReadingHandler (ACCESSIBILITY): read screen/stop reading — TTS + tree traversal
  - VoiceControlHandler (UI): mute/wake/dictation/help/numbers — VoiceControlCallbacks
- All registered in AndroidHandlerFactory.createHandlers() (4 existing + 7 new = 11)
- **BrowserHandler MISSING** — 47 web commands, delegates to WebCommandHandler/IWebCommandExecutor (only remaining handler gap)
- VoiceControlCallbacks: static @Volatile callback registry, wire in VoiceAvanueAccessibilityService.onServiceReady()
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-VOSDistributionAndDispatch-260211-V1.md`

## AVID Unification + Scroll Reset Fix (260215)
- Branch: `IosVoiceOS-Development`, commit: 86854901
- **Unified dual AVID systems**: overlay (`dyn_hash`) + command (`BTN:hash`) → single `ElementFingerprint` everywhere
- `Fingerprint.forElement()` now includes `packageName` in hash for cross-app uniqueness and VOS export portability
- Removed `OverlayItemGenerator.generateContentAvid()` — uses `ElementFingerprint.fromElementInfo()` now
- **Universal scroll detection**: `structuralChangeRatio` applied to ALL apps (not just target). Threshold 0.4 — above = navigation (reset), below = scroll (preserve numbering)
- Removed separate `isNewScreen && !isTargetApp → always reset` branch that was causing scroll to clear badges
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-UnifiedAVIDAndScrollReset-260215-V1.md`
- On-device verified: Pixel_9_5554 emulator, WebAvanue browser fully functional, AddressBar crash resolved

## Function:Click Fix + Localized Verb Extraction (260212)
- Branch: `IosVoiceOS-Development`, commit: 2fe02cb4
- **Bug fix**: `VoiceCommandDaoAdapter.toEntity()` missing `actionType = this.action` — all commands showed "Click"
- **Localized verbs**: `LocalizedVerbProvider` (KMP commonMain) extracts verbs from acc_click/acc_long_click VOS entries
  - `VERB_COMMAND_MAP`: acc_click → ("click", CLICK), acc_long_click → ("long press", LONG_CLICK)
  - `getActionVerbs()`: built-in English + locale-specific, sorted by length desc
  - `canonicalVerbFor()`: maps localized verb → canonical English (e.g., "pulsar" → "click")
- ActionCoordinator: `actionVerbs` now a `get()` property → `LocalizedVerbProvider.getActionVerbs()`
  - Verb normalization: `canonicalVerbFor(verb)` before handler routing
- SynonymRegistry: `addLocalizedVerbs()` / `clearLocalizedVerbs()` for locale-aware injection
- CommandManager: after `StaticCommandRegistry.initialize()`, extracts verb phrases and populates both registries
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-FunctionLabelClick-260212-V1.md`
- Plan doc: `docs/plans/VoiceOSCore/VoiceOSCore-Plan-LocalizedVerbExtraction-260212-V1.md`

# NewAvanues Memory

## Key Rules (User Enforced)
- NEVER quick fix or leave stubs - always long-term optimal solution
- ALL analysis/fix plans/investigations MUST be saved to `docs/` with naming convention
- CoT mandatory for ALL coding, ToT mandatory for planning/fixing
- Order work by priority + code proximity to maximize context
- Always use AskUserQuestion with options (user gets "Other" automatically)
- Create handover report BEFORE ExitPlanMode if ctx >= 60%

## Project Structure
- Monorepo with multiple modules (NAV, VoiceOS, Cockpit, DeviceManager, etc.)
- Plans stored in: `docs/plans/{module}/`
- Fixes stored in: `docs/fixes/{module}/`
- Analysis stored in: `docs/analysis/{module}/`
- Active branches: IosVoiceOS-Development (primary), VoiceOSCore-KotlinUpdate (synced)
- Deepest non-archive folder: 23 levels deep
- Completed handovers archived to: `Archives/Handover/` (13 archived 260215 — 9 completed + 2 superseded + 1 crash resolved + 1 dashboard verified)

## RAG
- Repo name in RAG: "NewAvanues"
- ~12,393 docs indexed (archives/sessions excluded)
- Archives go 34 levels deep - excluded from indexing
- Sync endpoint: `POST /rag/sync` with `{"repo":"NewAvanues","projectPath":"/Volumes/M-Drive/Coding/NewAvanues"}`

## Naming Convention
- `{Module}-{Type}-{Description}-{YYMMDD}-V{Version}.md`
- Types: Plan, Fix, Analysis, Investigation, Spec
- Module: PascalCase (NAV, VoiceOS, DeviceManager, Cockpit)

## Tech Stack
- KMP (Kotlin Multiplatform) for shared code
- Android + iOS + Web targets
- SQLDelight for database
- See `core/tech-stack.md` in LLMI for full details

## Developer Manual
- AVA-specific chapters: `Docs/AVA/ideacode/guides/` (Chapters 28-90)
- Cross-cutting/shared chapters: `Docs/MasterDocs/{Module}/` (Chapter 91+)
- Chapter 91 (AvanueUI DesignSystem): `Docs/MasterDocs/AvanueUI/`
- Chapter 92 (AvanueUI Phase 2 Unified Components): `Docs/MasterDocs/AvanueUI/`
- Chapter 93 (Voice Command Pipeline & Localization): `Docs/MasterDocs/NewAvanues-Developer-Manual/` — updated 260211 with multi-locale runtime section
- Chapter 94 (4-Tier Voice Enablement): `Docs/MasterDocs/NewAvanues-Developer-Manual/`
- Chapter 95 (VOS Distribution & Handler Dispatch): `Docs/MasterDocs/NewAvanues-Developer-Manual/`
- Chapter 96 (KMP Foundation Platform Abstractions): `Docs/MasterDocs/Foundation/`
- Next chapter = 97
- Naming: `Developer-Manual-Chapter{N}-{Title}.md`
- Rule: Module-specific docs go under AVA/ideacode/guides; cross-cutting system docs go under MasterDocs/{Module}/
- Handovers: `Docs/VoiceOSCore/Handover/`
- LLMI shared memory: `.ideacode/shared-memory/modules/NewAvanues.md`

## AVU DSL Evolution (Implemented)
- Branch: `claude/060226-avu-dsl-evolution`
- Three layers: Wire Protocol -> DSL Format -> Runtime Interpreter
- Chapters 81-87 in `Docs/AVA/ideacode/guides/`
- Plugin paradigm: .avp text files, NOT APK/JAR

## Avanues Consolidated App
- Branch: `060226-1-consolidation-framework`
- apps/avanues/ = consolidated VoiceAvanue + WebAvanue
- Dual launcher icons via activity-alias (.VoiceAvanueAlias, .WebAvanueAlias)
- AvaTheme (KMP) replaces VoiceAvanueTheme
- AvanueMode enum for modular navigation
- applicationId=com.augmentalis.avanues, namespace=com.augmentalis.voiceavanue
- Chapter 88, Handover: Avanues-Handover-ConsolidatedApp-260206.md

## KSP2 + KMP Incompatibility (CRITICAL)
- `ksp.useKSP2=false` in `gradle.properties` — MANDATORY for KMP modules
- KSP2 can't resolve types from KMP module dependencies during Hilt annotation processing
- Symptom: `error.NonExistentClass` for ALL classes that import types from KMP modules
- Root cause: KSP2's type resolver doesn't properly handle KMP module compiled output
- Affects: Kotlin 2.1.0 + KSP 2.1.0-1.0.29 + Dagger Hilt 2.54

## KMP Settings Abstraction (ALL PHASES DONE, 260215)
- **Chapter 96**: `Docs/MasterDocs/Foundation/Developer-Manual-Chapter96-KMPFoundationPlatformAbstractions.md`
- Foundation commonMain: AvanuesSettings, DeveloperSettings, SettingsKeys, SettingsMigration, ISettingsStore, ICredentialStore, IFileSystem, IPermissionChecker
- SettingsCodec pattern: PreferenceReader/PreferenceWriter + AvanuesSettingsCodec + DeveloperSettingsCodec
- **Phase 1** (260213): Extract models → `4548bbbc`, `0c4251e0`, `0e6eed08`
- **Phase 2** (260215): Android ISettingsStore/ICredentialStore → `260e1287`, `04cbea5e`, `69104fca`
- **Phase 3** (260215): iOS+Desktop implementations → `4ba761dc`
  - iOS: UserDefaultsSettingsStore<T>, KeychainCredentialStore, IosFileSystem, IosPermissionChecker
  - Desktop: JavaPreferencesSettingsStore<T>, DesktopCredentialStore, DesktopFileSystem, DesktopPermissionChecker
- Both branches synced: IosVoiceOS-Development + kmpvoiceos-update

## Kotlin/Compose Upgrade (260210)
- Kotlin 1.9.24→2.1.0, KSP 2.1.0-1.0.29, Compose 1.7.3, Gradle 8.14.3, AGP 8.2.0
- KAPT fully removed→KSP, `composeOptions{}`→`kotlin.plugin.compose`
- menuAnchor() now takes MenuAnchorType param (JB Compose 1.7.3 change from 1.6.11)

## Unified Adaptive Settings (Implemented)
- Branch: `060226-1-consolidation-framework`, commit: 321786ba
- Chapter 90, plan: docs/plans/Avanues/Avanues-Plan-UnifiedAdaptiveSettings-260208-V1.md
- Architecture: Foundation KMP -> AvanueUI components -> App providers -> UnifiedSettingsScreen
- Hilt @IntoSet with MANDATORY @JvmSuppressWildcards on Set injection
- Material3 Adaptive ListDetailPaneScaffold (phone/tablet/foldable)
- SettingsDisplayMode: STANDARD, GLASS_MONOCULAR, GLASS_BINOCULAR
- 5 providers: Permissions(100), VoiceCursor(200), VoiceControl(300), WebAvanue(400), System(500)
- To add settings: create provider + add @Provides @IntoSet in SettingsModule.kt
- AvanueTheme.colors.primary for default accent (ColorTokens.Primary DEPRECATED)

## Unified Design Token System (MANDATORY) — v5.1 Decoupled Palette+Style+Appearance
- Location: `Modules/AvanueUI/` (single KMP module, DesignSystem+Foundation merged)
- **Build dep**: `implementation(project(":Modules:AvanueUI"))`
- **Static tokens** (no @Composable): SpacingTokens, ShapeTokens, SizeTokens, ElevationTokens, AnimationTokens, GlassTokens, WaterTokens, ResponsiveTokens
- **Three independent axes** (Theme v5.1, 260211):
  - **Axis 1: AvanueColorPalette** — SOL(sun/gold), LUNA(moon/silver), TERRA(earth/green), HYDRA(sapphire, DEFAULT)
  - **Axis 2: MaterialMode** — Glass, Water, Cupertino, MountainView (DEFAULT=Water)
  - **Axis 3: AppearanceMode** — Light, Dark, Auto (DEFAULT=Auto, follows isSystemInDarkTheme)
  - 32 combos: any palette x any style x light/dark
- **Theme colors** (@Composable): `AvanueTheme.colors.*` (interface AvanueColorScheme, impl: HydraColors, SolColors, LunaColors, TerraColors)
- **Glass scheme** (@Composable): `AvanueTheme.glass.*` (HydraGlass, SolGlass, LunaGlass, TerraGlass)
- **Water scheme** (@Composable): `AvanueTheme.water.*` (HydraWater, SolWater, LunaWater, TerraWater)
- **DEPRECATED old schemes**: OceanColors→LunaColors, SunsetColors→SolColors, LiquidColors→HydraColors (+ Glass/Water)
- **Light variants**: HydraColorsLight/GlassLight/WaterLight, SolColorsLight/GlassLight/WaterLight, LunaColorsLight/GlassLight/WaterLight, TerraColorsLight/GlassLight/WaterLight
- **XR variants** (AR smart glasses): HydraColorsXR, SolColorsXR, LunaColorsXR, TerraColorsXR — transparent bg, boosted luminance primaries
- **XR accessor**: `palette.colorsXR` (no isDark param — XR is always dark/additive)
- **XR research**: `docs/analysis/AvanueUI/AvanueUI-Analysis-XRColorPalettes-260211-V1.md`
- **Appearance-aware accessors** (preferred): `palette.colors(isDark)`, `palette.glass(isDark)`, `palette.water(isDark)`
- **isDark CompositionLocal**: `LocalAppearanceIsDark`, accessor: `AvanueTheme.isDark`
- **AvanueThemeProvider**: new `isDark: Boolean = true` param → controls M3 darkColorScheme/lightColorScheme bridge
- **DEPRECATED**: AvanueThemeVariant (coupled palette+style) → use AvanueColorPalette + MaterialMode
- **DEPRECATED**: MaterialMode.PLAIN → use MaterialMode.MountainView
- **Unified components** (PRIMARY): `com.augmentalis.avanueui.components.*` (AvanueSurface, AvanueCard, AvanueButton, AvanueChip, AvanueBubble, AvanueFAB, AvanueIconButton)
- **Glass components** (DEPRECATED): `com.augmentalis.avanueui.components.glass.*` → use unified instead
- **Water components** (DEPRECATED): `com.augmentalis.avanueui.components.water.*` → use unified instead
- **Still current**: PulseDot, StatusBadge, WaterNavigationBar, Modifier.glass(), Modifier.waterEffect()
- **Settings components**: `com.augmentalis.avanueui.components.settings.*`
- **Display profile** (@Composable): `AvanueTheme.displayProfile` (PHONE, TABLET, GLASS_MICRO/COMPACT/STANDARD/HD)
- **Touch targets**: Use `DisplayUtils.minTouchTarget` (auto-adapts via density scaling)
- BANNED: `com.avanueui.*` (DELETED), `com.augmentalis.avamagic.ui.foundation.*` (MOVED), `:Modules:AvanueUI:DesignSystem` (MERGED), `:Modules:AvanueUI:Foundation` (MERGED)
- Density override: AvanueThemeProvider overrides LocalDensity per DisplayProfile, ALL dp/sp auto-scale
- WebAvanue Spacing note: old `OceanDesignTokens.Spacing.md` = 12dp, new `SpacingTokens.md` = 16dp (different!)
- **Container tokens**: primaryContainer, onPrimaryContainer, secondaryContainer, onSecondaryContainer, tertiaryContainer, onTertiaryContainer, errorContainer, onErrorContainer
- **VoiceOSCore dep**: Added `implementation(project(":Modules:AvanueUI"))` to androidMain in build.gradle.kts
- **MaterialTheme migration COMPLETE** (260210): Zero MaterialTheme.colorScheme remaining in active modules
- **DataStore keys** (v5.1): `theme_palette` + `theme_style` + `theme_appearance` (auto-migrates from old `theme_variant`)
- **Settings UI**: Three independent dropdowns — "Color Palette", "Material Style", "Appearance"
- **Plan docs**: v5.0: `docs/plans/AvanueUI/AvanueUI-Plan-ThemeV5DecoupledPaletteStyle-260211-V1.md`, v5.1: `docs/plans/AvanueUI/AvanueUI-Plan-ThemeV51AppearanceMode-260211-V1.md`

## SpatialVoice Design Language
- Brand name for the UI aesthetic: "SpatialVoice" (pseudo-spatial glass UI)
- Internal API: AvanueTheme (unchanged) — SpatialVoice is the design language name
- ALL screens must use AvanueTheme.colors.* (NOT MaterialTheme.colorScheme.*)
- Background: verticalGradient(background, surface.copy(0.6f), background)
- TopAppBars: containerColor=Transparent or AvanueTheme.colors.surface
- Module branding: VoiceTouch™ (voiceavanue), CursorAvanue (voicecursor), WebAvanue (webavanue)
- Credits: "VoiceOS® Avanues EcoSystem", "Designed and Created in California with Love."

## VoiceOSCore Features (260210-260211)
> Details in `memory/voiceoscore-features.md`
- **ModuleAccent**: `AvanueModuleAccents` static registry, `AvanueTheme.moduleAccent(id)`, SettingsColorRow
- **Cursor Voice**: AndroidCursorHandler, show/hide/click commands, AccessibilityClickDispatcher
- **4-Tier Voice**: Tier 1 AVID → Tier 2 `(Voice:)` → Tier 3 scraping → Tier 4 .VOS profiles
- **DB-Driven Commands**: VOS seed → CommandLoader → SQLDelight → StaticCommandRegistry
- **Multi-Locale**: 5 locales (en-US, es-ES, fr-FR, de-DE, hi-IN), runtime switching via DataStore
- **VOS Distribution (Phase A)**: v2.1 split .app.vos + .web.vos, VosFileRegistry.sq, exporter/importer
- **Static Dispatch**: 7 handlers (Media, Screen, Text, Input, AppControl, Reading, VoiceControl) + 4 existing = 11 total
- **VOS SFTP Sync (Phase B DONE)**: Full details in `memory/vos-sftp-sync.md`
  - JSch 0.2.16, VosSftpClient, VosSyncManager, SftpCredentialStore (AES256_GCM)
  - VosSyncWorker (@HiltWorker), PhraseSuggestion.sq (Phase C foundation)
  - Fix docs: `VoiceOSCore-Fix-VOSSftpSyncPhaseB-260211-V1.md`, `VoiceOSCore-Fix-PhaseBCompletion-260211-V1.md`

## Branch Sync (260215)
- IosVoiceOS-Development → VoiceOSCore-KotlinUpdate: merged via `4dea5af0` + `44422099`
- Branches share cherry-picks (different hashes, same content) + merge commits
- Sync strategy: merge IosVoiceOS-Development → KotlinUpdate (not cherry-pick)
- KotlinUpdate tracks all IosVoiceOS-Development commits; has extra historical cherry-picks

## Static Command Handlers (260215 Discovery)
- 7 of 8 handlers ALREADY EXIST in `Modules/VoiceOSCore/src/androidMain/.../handlers/`
- MediaHandler (13 cmds), ScreenHandler (20), TextHandler (8), InputHandler (6), AppControlHandler (4), ReadingHandler (7), VoiceControlHandler (18)
- All registered in `AndroidHandlerFactory.createHandlers()` (11 total: 4 pre-existing + 7 new)
- Only BrowserHandler is MISSING (47 web commands delegate to WebCommandHandler)
- 76 commands covered across 7 handlers

## Unified AVID System (260215)
- **BEFORE**: Two separate AVID systems — overlay (`dyn_hash8`) + command (`BTN:hash8`)
- **AFTER**: Single unified system via `ElementFingerprint.fromElementInfo()` everywhere
- `Fingerprint.forElement()` now includes `packageName` in hash (cross-app uniqueness)
- Hash input: `packageName + className + resourceId + text + contentDescription`
- Overlay AVID format changed: `dyn_a3f2b1c0` → `BTN:7c4d2a1e`
- `OverlayItemGenerator` uses `ElementFingerprint` instead of private `generateContentAvid()`
- Universal scroll/navigation detection: structural-change-ratio for ALL apps (not just target)
- Fix doc: `docs/fixes/VoiceOSCore/VoiceOSCore-Fix-UnifiedAVIDAndScrollReset-260215-V1.md`
- Updated docs: Chapter 94 (4-Tier Voice), Chapter 95 (Handler Dispatch — BrowserHandler noted as PENDING), AVID README (import paths + hash description)
- On-device verified: Pixel_9_5554 emulator, WebAvanue AddressBar crash RESOLVED, browser fully functional
- Commit: `86854901` on `IosVoiceOS-Development`, pushed to origin

## WebCommandHandler Flow (260215 — Verified CORRECT)
- Handles BOTH scraped DOM commands (source="web") AND static browser commands (source="web_static")
- Dual-path: speech grammar + QuantizedCommands via `updateDynamicCommandsBySource()`
- Browser scoping ALREADY IMPLEMENTED in `refreshOverlayBadges()` line 544-546
- `webCommandCollectorJob` cleanup in `onDestroy()` line 566-568
- Phase 3 Task 3.2 from consolidation plan is SUPERSEDED

## Cursor IMU Investigation (260215)
- Cursor renders and centers correctly but doesn't move with head motion
- Data pipeline: IMUManager.onSensorChanged → processRotationVector → _orientationFlow → HeadTrackingBridge.toCursorInputFlow → CursorController.connectInputFlow → update → _state
- Likely culprits (ranked): IMU sensors not starting (60%), flow scope cancelled (20%), sensor accuracy blocking (10%)
- Analysis doc: `Docs/analysis/VoiceCursor/VoiceCursor-Analysis-IMUCursorMotionRegression-260215-V1.md`

## Common Patterns
- Check `docs/plans/` for existing plans before creating new ones
- Query RAG before writing code to avoid duplicating existing implementations
- Use `.ideacode/registries/` for module/file registries

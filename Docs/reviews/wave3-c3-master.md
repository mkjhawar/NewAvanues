# Wave 3 · Cluster 3 Master Analysis
**Modules:** VoiceKeyboard · VoiceCursor · VoiceDataManager · VoiceAvanue · Actions
**Review Date:** 260222
**Reviewer:** code-reviewer agent
**Full Report:** `docs/reviews/VoiceInput-Review-QualityAnalysis-260222-V1.md`

---

## Known Stubs — CRITICAL (active runtime failures)

| Location | Behavior | Impact |
|----------|----------|--------|
| `Modules/VoiceAvanue/src/commonMain/.../VoiceAvanue.kt` L114-148 — `CommandSystem`, `BrowserSystem`, `RpcSystem` | All three internal objects have empty method bodies (`// TODO`). `initialize()` sets `_isInitialized = true` after calling them, so callers believe the module is ready. | Any code gated on `VoiceAvanue.isInitialized` proceeds into a fully non-functional command/browser/RPC environment with no error surfaced. |
| `Modules/VoiceDataManager/src/androidMain/.../io/DataExporter.kt` L25-28 — `exportToFile()` | Always returns `null`, logs a warning. | Export button in `VosDataManagerActivity` silently does nothing when pressed. |
| `Modules/VoiceDataManager/src/androidMain/.../io/DataImporter.kt` L27-30 — `importFromFile()` | Always returns `false`. | Import button in `VosDataManagerActivity` silently does nothing. |
| `Modules/VoiceDataManager/src/desktopMain/.../VoiceDataManager.desktop.kt` L15 — `notImplemented()` | `throw NotImplementedError(...)` — desktop target compiles but is non-functional. | Pre-existing, consistent with other desktop stubs in codebase. |

---

## Rule 7 Violations (AI Attribution)

| File | Line | Violation |
|------|------|-----------|
| `Modules/VoiceDataManager/src/androidMain/.../repositories/ConfidenceTrackingRepository.kt` | 5 | `Author: AI Assistant` |
| `Modules/VoiceDataManager/src/androidMain/.../ui/GlassmorphismUtils.kt` | 5 | `Author: VOS4 Development Team` |
| `Modules/VoiceDataManager/src/androidMain/.../ui/VosDataManagerActivity.kt` | 7 | `Author: VOS4 Development Team` |

---

## Theme Violations (MANDATORY RULE #3)

| File | Violation |
|------|-----------|
| `Modules/VoiceDataManager/src/androidMain/.../ui/VosDataManagerActivity.kt` L60-63 | `MaterialTheme(colorScheme = darkColorScheme())` wrapping entire activity — banned |
| `Modules/VoiceDataManager/src/androidMain/.../ui/GlassmorphismUtils.kt` | 30+ hardcoded `Color(0xFF...)` literals in `DataColors` and `DataGlassConfigs` — not using `AvanueTheme.*` tokens |

---

## AVID Violations (Voice-First MANDATORY)

| File | Elements Missing AVID |
|------|-----------------------|
| `Modules/VoiceDataManager/src/androidMain/.../ui/VosDataManagerActivity.kt` | Every `Button`, `IconButton`, `Checkbox`, `Slider`, `Switch`, `TextButton`, and `Card(clickable)` in the entire file — zero AVID coverage (approximately 40+ interactive elements) |

---

## High-Severity Bugs

| Location | Bug |
|----------|-----|
| `VoiceKeyboardService.kt:140` | **CRITICAL.** `currentInputConnection = currentInputConnection` — self-assignment. The actual `InputConnection` from the framework is never captured. Field stays `null` permanently. All key events and voice results are silently dropped via early-exit `?: return` guards. |
| `VoiceKeyboardService.kt:538-545` | `getSuggestionsForWord()` always returns `emptyList()` — coroutines are launched on every keystroke for a guaranteed no-op. |
| `VoiceInputHandler.kt:340-353` | `getSupportedLanguages()` hardcoded static list — never queries actual recognizer support. |
| `DictationHandler.kt:247-252` | Dictation preferences (timeout, start/stop commands) never persisted — always use hardcoded defaults. |
| `CursorOverlayService.kt:343-365` | `super.onDestroy()` called before WindowManager/overlay cleanup — may cause exception on fast lifecycle paths. |
| `VoiceOSConnection.kt:224-240` | Polling-loop `Runnable` inside `suspendCancellableCoroutine` leaks on coroutine cancellation — `invokeOnCancellation` handler missing. |
| `RecognitionLearningRepository.kt:62` | Full table scan (`getAll().executeAsList().size`) to count rows in `initialize()` — O(n) memory on startup. |
| `RecognitionLearningRepository.kt:305-317` | `saveLearnedCommands()` issues N×2 individual database calls — no transaction batching. |

---

## Architecture Notes

### VoiceKeyboard — Module-level
- **Not using its own SpeechRecognition KMP dependency.** `VoiceInputHandler` instantiates
  Android's `SpeechRecognizer` directly, bypassing the `Modules/SpeechRecognition` KMP module
  that is declared in `build.gradle.kts`. Creates a second, non-offline-capable recognition path.
- `DictationService` and `VoiceInputService` are pure 1-to-1 delegate wrappers with no
  added logic — violates Rule 2 (minimize indirection). The handlers should implement the
  interfaces directly.
- `VoiceInputService` is not a Service (no `Context.startService`, no `onBind`) — the name
  is misleading. It is a plain class. Rename to `VoiceInputAdapter` or similar.

### VoiceCursor — Module-level
- Best-designed module of the five. Correct KMP source set split, real algorithms,
  well-documented head-tracking math.
- `GazeClickManager` compound-state concurrency gap: `@Volatile` fields + `synchronized`
  block do not guarantee atomic multi-field snapshots. Medium risk, low observed failure rate.
- No AVID on `CursorOverlayView` — acceptable since the cursor is operated via voice commands
  through the handler layer, not directly.

### VoiceDataManager — Module-level
- `DatabaseManager` singleton pattern and repository wiring are sound.
- UI layer (`VosDataManagerActivity`, 1352 lines) should be split: theme compliance,
  AVID coverage, and stub-backed features all need remediation.
- `ConfidenceTrackingRepository` is an admitted redundant wrapper over `RecognitionLearningRepository`
  (the file header says "this class can be refactored... or removed"). It should be removed.

### VoiceAvanue — Module-level
- Module entry point (`VoiceAvanue.kt`) is non-functional. Only the data types
  (`UnifiedCommand`, `EventBus`, `ContextRestriction`, etc.) are production-ready.
- `EventBus` has a leaked `CoroutineScope` (no `close()`/`cancel()` method) — safe in
  production use but problematic in test teardown.

### Actions — Module-level
- Strongest module. Good SOLID architecture (`CategoryCapabilityRegistry` OCP, sealed
  `RoutingDecision`, Hilt injection).
- `inferCategoryFromName()` substring matching in `IntentRouter` can produce false positives
  (e.g., `"back"` matches `"feedback"`). Low risk in current intent naming convention but
  fragile as the intent namespace grows.
- Best test coverage of the five modules.
- `ActionsInitializer` partial-initialization on exception marks `isInitialized = true`
  despite potentially missing handlers — should be logged more visibly.

---

## Summary Counts

| Severity | Count |
|----------|-------|
| Critical | 4 |
| High | 8 |
| Medium | 8 |
| Low | 6 |
| **Total** | **26** |

---

## Cross-Module Concerns

1. **VoiceAvanue as integration layer is not ready** — modules that would depend on
   `VoiceAvanue` for unified command routing (`VoiceKeyboard`, `VoiceCursor`) currently
   implement their own ad-hoc routing instead.

2. **`VoiceCursor` → `VoiceAvanue` dependency declared** in `VoiceAvanue/build.gradle.kts`
   but `VoiceAvanue` itself depends on `VoiceCursor` (circular if both are source dependencies).
   Verify the dependency graph does not create a circular reference in Gradle.

3. **`VoiceKeyboard` minSdk = 29**, `VoiceCursor` minSdk = 26, `Actions` minSdk = 28,
   `VoiceDataManager` minSdk = 29 — inconsistent minimum API levels across the cluster.
   Recommend aligning to minSdk = 29 (Android 10) per the project standard stated in
   `VoiceKeyboard/build.gradle.kts`.

4. **compileSdk inconsistency**: `VoiceKeyboard` and `VoiceDataManager` use `compileSdk = 34`,
   while `VoiceCursor`, `VoiceAvanue`, and `Actions` use `compileSdk = 34` or `35`.
   `VoiceCursor` and `VoiceAvanue` target `compileSdk = 35`. Align to a single project-wide
   value.

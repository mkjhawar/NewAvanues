# AvanueUI Voice Subsystems — Deep Code Review
**Scope:** 74 .kt files across three directories
**Date:** 260220
**Reviewer:** code-reviewer agent
**Directories:**
- `Modules/AvanueUI/AvanueUIVoiceHandlers/` (35 files)
- `Modules/AvanueUI/Voice/` (10 files)
- `Modules/AvanueUI/AssetManager/` (29 files)

---

## Summary

The AvanueUI Voice subsystem contains **7 confirmed KMP compile-time violations** (Android/JVM-specific APIs in `commonMain` files) that will prevent iOS and Desktop builds from compiling. The Voice legacy module has **wholesale mock tests** that verify no real behaviour and uses `MaterialTheme.colorScheme.*` in violation of MANDATORY RULE #3. The AssetManager module has an entire duplicate source tree and three stub/placeholder implementations that return empty results at runtime. Total: 7 CRITICAL, 8 HIGH, 14 MEDIUM, 40 LOW findings.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/DatePickerHandler.kt:1-20` | `import java.text.SimpleDateFormat`, `import java.util.Calendar`, `import java.util.regex.Pattern` — JVM-only APIs in `commonMain`. `Calendar.JANUARY` constants used for MONTH_NAMES map. `Calendar.getInstance()` in `handleSetCurrentTime()`. `SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())` in `applyDate()`. Fails to compile on iOS/Desktop. | Replace `java.util.Calendar` with `kotlinx-datetime` `LocalDate`/`LocalTime`. Replace `java.util.regex.Pattern.compile()` with `Regex()`. Replace `SimpleDateFormat` with `kotlinx-datetime` `DateTimeFormatter` or manual month-name lookup. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/FileUploadHandler.kt:1-5` | `import android.net.Uri` in `commonMain`. `FileInfo.uri: Uri`, `FileUploadState.uploadedUri: Uri?`, and `FileUploadResult.UploadCompleted(uploadedUri: Uri?)` all reference Android-only `android.net.Uri` type. Fails to compile on iOS/Desktop/JS. | Define a platform-neutral `data class AssetUri(val value: String)` in commonMain and provide `actual typealias` or conversion helpers in androidMain. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/TimePickerHandler.kt:1-20` | `import java.util.Calendar`, `import java.util.Locale`, `import java.util.regex.Pattern` in `commonMain`. `Calendar.getInstance()` called in `handleSetCurrentTime()`. `String.format(Locale.US, "%02d", result.minute)` at L534. `Pattern.compile()` used for TIME_PATTERN_12H, TIME_PATTERN_24H, HOUR_PATTERN, MINUTE_PATTERN. Fails to compile on iOS/Desktop. | Replace `Calendar.getInstance()` with `kotlinx-datetime` `Clock.System.now()`. Replace `String.format(Locale.US, "%02d", n)` with `n.toString().padStart(2, '0')`. Replace `Pattern.compile()` with `Regex()`. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/RangeSliderHandler.kt:1033` | `String.format("%.1f", value)` in `formatValue()` — `java.lang.String.format` is JVM-only. This is in `commonMain`. Fails to compile on iOS/native. | Replace with: `"${(value * 10).toInt() / 10.0}"` or `buildString { append(value.toInt()); if (value != value.toInt().toFloat()) { append('.'); append(((value % 1) * 10).toInt()) } }` or add a `expect fun formatOneDecimal(value: Float): String` with platform actuals. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/SliderHandler.kt:548` | `String.format("%.1f", value)` in `formatValue()` — JVM-only in `commonMain`. Same root cause as RangeSliderHandler. | Same fix as RangeSliderHandler. Create a shared `formatOneDecimal()` utility to avoid duplication across all four handlers. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../input/StepperHandler.kt:819,882` | `String.format("%.1f", value)` appears TWICE in this file: once in `StepperHandler.formatValue()` (L819) and once in `StepperInfo.formatValue()` (L882). Both are in `commonMain`. | Same fix. Note: having `formatValue()` on both the handler and the info class is itself a DRY violation; consolidate into a single shared utility function. |
| **CRITICAL** | `AvanueUIVoiceHandlers/.../ui/ToastHandler.kt` | `import java.util.concurrent.ConcurrentLinkedDeque` in `commonMain`. `ToastInfo.timestamp` default parameter uses `System.currentTimeMillis()` which is JVM-only. Both compile on Android but fail on iOS/Desktop targets. | Replace `ConcurrentLinkedDeque` with `ArrayDeque` (Kotlin stdlib, KMP-safe) protected by a `kotlinx.coroutines.sync.Mutex`. Replace `System.currentTimeMillis()` with `Clock.System.now().toEpochMilliseconds()` from `kotlinx-datetime`. |
| **HIGH** | `AvanueUIVoiceHandlers/.../data/TableHandler.kt:304` | `extractNumber()` uses `command.contains(word)` substring matching to map words to numbers. "phone" contains "one", "eleven" contains "eleven" and "one", "twenty" contains "twenty" correctly but "twenty-one" matches both "twenty" and "one" returning the first match. Returns wrong number for compound and embedded-digit words. | Use word-boundary matching: `Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(command)`. |
| **HIGH** | `AvanueUIVoiceHandlers/.../overlay/DialogHandler.kt:414` | `findPositiveButton()` silently falls back to `buttons.lastOrNull()` when no button label matches the positive-button keywords list. In a two-button dialog (e.g., "Cancel" + "Delete"), if "Delete" does not match any keyword, the last button (which could be the destructive action) is returned and confirmed. Silent data-destructive fallback. | Return `null` when no positive-button match is found, and surface a `DialogResult.NoPositiveButtonFound` variant. Never silently fall back to last button. |
| **HIGH** | `AvanueUIVoiceHandlers/.../overlay/AlertHandler.kt:125` | `DISMISS_COMMANDS.any { normalizedAction.contains(it) }` uses substring matching for dismiss commands. The word "close" as a dismiss trigger will intercept any phrase that contains "close", including commands intended for ModalHandler ("close modal") or DrawerHandler ("close drawer"). Cross-handler command leakage. | Use full-phrase equality check or bounded match: `DISMISS_COMMANDS.any { normalizedAction == it || normalizedAction.startsWith("$it ") }`. |
| **HIGH** | `AvanueUIVoiceHandlers/.../overlay/ModalHandler.kt:158` | `CLOSE_COMMANDS.any { normalizedAction.contains(it) }` — same substring-match cross-handler leakage as AlertHandler. "close" will match any phrase containing the word "close" regardless of context. | Same fix as AlertHandler — use equality + startsWith boundary check. |
| **HIGH** | `AvanueUIVoiceHandlers/.../overlay/SnackbarHandler.kt` | All three command sets (dismiss, action, expand) use `normalizedAction.contains(it)` substring matching. "dismiss" matches "dismiss notification" which could be intended for a different overlay. | Same fix pattern — equality or anchored prefix matching. |
| **HIGH** | `Voice/src/main/.../utils/NumberToWordsConverter.kt:13` | `import java.util.*` — imports `java.util.Locale` and uses `Locale.getDefault()` in `numberToWords()` and `Locale.Builder()` in the overload. This is in an Android-only source set (`src/main/`), so it compiles, but `Locale.Builder().setLanguage()` is an API 21+ feature not available below it. No `@RequiresApi` annotation or minSdk guard. | Add `@RequiresApi(Build.VERSION_CODES.LOLLIPOP)` or replace `Locale.Builder()` with `Locale(languageCode)` constructor which is available at all API levels. |
| **HIGH** | `Voice/src/main/.../components/VoiceComponents.kt` | `VoiceStatusCard`, `VoiceCommandButton`, `GlassmorphismCard`, `VoiceWaveform` all use raw `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*` — direct violation of MANDATORY RULE #3 (AvanueUI Theme v5.1). This module is in `src/main/` (Android), where AvanueUI is available. | Wrap content in `AvanueTheme` provider and replace `MaterialTheme.*` references with `AvanueTheme.colors.*`. |
| **HIGH** | `Voice/themes/arvision/GlassMorphism.kt` | File is empty (1 line, 0 content). The `GlassmorphismCard` composable in `VoiceComponents.kt` directly implements glassmorphism inline. The dedicated `GlassMorphism.kt` theme file exists as a placeholder only. | Either remove the empty file (it causes confusion) or implement the `GlassMorphism` theme definitions that `VoiceComponents.kt` should be using. Violates Rule 1 (no stubs/placeholders). |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../input/MultiSelectHandler.kt:101` | `@Volatile private var activeDisambiguation` — `@Volatile` provides JVM visibility guarantee only. Compound operations like `if (activeDisambiguation == null) { activeDisambiguation = newValue }` are not atomic. Under concurrent coroutine access, two coroutines can both observe `null` and both proceed to set the value, creating a race condition in disambiguation. | Replace `@Volatile` with `kotlinx.coroutines.sync.Mutex` protecting all reads and writes to `activeDisambiguation`. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../overlay/ConfirmHandler.kt:364` | `containsDestructiveKeywords()` private method is defined but never called anywhere in the class. Dead code. | Remove the unused method or wire it into the confirm-action path to actually gate destructive confirmations. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../navigation/BreadcrumbHandler.kt` | `findBreadcrumb(name: String?)` — the `name` parameter is always `null` at every call site. The name-based lookup branch inside the function is permanently unreachable. Dead code path that misleads readers. | Remove the `name` parameter and simplify `findBreadcrumb()` to take only the index. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../canvas/Canvas3DHandler.kt:383` | After calling `executor.zoom(canvasInfo, newZoom)`, the handler uses the stale local `newZoom` value in its feedback message. If the executor clamps the zoom to bounds (e.g., max 10x) the user receives an incorrect feedback message stating the zoom they requested rather than the zoom that was applied. | Have `executor.zoom()` return the actual applied zoom value, or have `executor.getCanvasInfo()` called again after the operation to obtain post-operation state. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../input/TagInputHandler.kt:841` | `parseTagIndex()` private method is defined but never called anywhere in the class. All index parsing is done inline at each call site with `toIntOrNull()` or `TAG_NUMBER_PATTERN` matching. Dead code. | Remove the unused method to reduce cognitive load. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../input/TimePickerHandler.kt:154` | `normalized.contains("time to")` is an overly broad substring check — "What time does it close today" contains "time to" and would incorrectly route to `handleSetTime()`. | Use anchored prefix check: `normalized.startsWith("set time to") || normalized.startsWith("change time to")`, or `Regex("\\btime to\\b")` with word boundaries. |
| **MEDIUM** | `AvanueUIVoiceHandlers/.../input/SearchBarHandler.kt` | `handleFilterBy()` skips filter validation entirely when `searchBarInfo.filters.isNotEmpty()` is false. Any string is treated as a valid filter category and passed to the executor when the filter list is empty. If the executor doesn't validate, invalid categories are applied silently. | Add validation that the category name at minimum is non-blank, and surface `SearchResult.InvalidFilter` when categories are not available. |
| **MEDIUM** | `AssetManager/.../commonMain/AssetVersionManager.kt:325-334` | `saveVersionHistory()` and `loadVersionHistory()` both contain `// TODO: Implement persistence` comments and return `Result.success(Unit)` / `null` without persisting anything. Version history is stored only in memory and is lost on restart. `AssetVersionManager` is used by `ManifestManager` and `AssetManager` which depend on version persistence. | Implement persistence using the injected `AssetRepository`. Violates Rule 1 (no stubs). |
| **MEDIUM** | `AssetManager/.../commonMain/AssetStorage.kt` | `StorageCleanup.findOrphanedFiles()` always returns `emptyList()`. `cleanOrphanedFiles()` calls `findOrphanedFiles()` and always reports 0 deletions. `optimizeStorage()` always returns `StorageOptimizationResult(bytesFreed=0, filesOptimized=0)`. Three stub operations that silently do nothing. | Implement actual file scanning in the `expect` platform implementations, or at minimum throw `UnsupportedOperationException` rather than silently returning zeros (Rule 1 violation). |
| **MEDIUM** | `AssetManager/.../commonMain/ManifestManager.kt` | `ManifestMigration.migrateManifest()` always returns `Result.success(Unit)` without performing any migration. `getMigrationPath()` returns only `[fromVersion, toVersion]` with no intermediate steps. `ManifestBackup.restoreManifest()` always returns `Result.success(Unit)` without restoring anything. `ManifestBackup.listBackups()` always returns `emptyList()`. | Implement or remove. Silent success from an unimplemented operation is worse than an explicit `NotImplementedError`. |
| **MEDIUM** | `AssetManager/AssetManager/` vs `AssetManager/src/` | The entire AssetManager source tree is duplicated: `AssetManager/AssetManager/src/` and `AssetManager/src/` contain identical packages, identical class names, and identical implementations. 22 files are exact duplicates. This creates ambiguity about which tree is the canonical source, risks divergence, and adds dead build weight. | Determine the canonical tree, delete the duplicate, update `build.gradle.kts` to point to the single source. |
| **MEDIUM** | `AssetManager/.../androidMain/AssetStorage.kt` | `LocalAssetStorage.loadImage()` returns an `ImageAsset` with `dimensions = Dimensions(0, 0)` (hardcoded) when loading from thumbnail. The comment says "Would need to be stored/calculated". Any consumer that calls `getAspectRatio()` or checks orientation will receive division-by-zero or always-square results. | Store dimensions in the manifest on save and load them back. The `ImageManifestEntry` already has `width`/`height` fields — use them. |
| **MEDIUM** | `Voice/src/test/.../VoiceUIComponentsTest.kt` | All 15 test methods are mock tests that test only trivial string/boolean/list constants. None test actual `VoiceComponents`, `VoiceStatus`, or `VoiceUITheme` behaviour. Tests contain comments "Mock test to ensure build passes". Zero real coverage despite 15 test methods. | Rewrite tests to test actual composable state, theme application, and AVID semantic properties. |
| **MEDIUM** | `Voice/src/androidTest/.../utils/TestExtensions.kt` | `captureToImage()` extension on `SemanticsNodeInteraction` returns `ImageBitmap(100, 100)` — a blank 100x100 mock image — instead of actually capturing the composable. `assertAgainstGolden()` only checks `width > 0 && height > 0`. Visual regression tests never actually compare any real UI output. | Use `captureToImage()` from `androidx.compose.ui.test` (already available in androidTest) which does actual screen capture. The mock implementation defeats the purpose of visual regression testing entirely. |
| **LOW** | `AvanueUIVoiceHandlers/` — all 35 files | All 35 handler files contain `Author: VOS4 Development Team` in the file header. This violates MANDATORY Rule 7 (no team/AI attribution — use "Manoj Jhawar" or omit). | Replace `Author: VOS4 Development Team` with `Author: Manoj Jhawar` or remove the `Author:` line entirely. |
| **LOW** | `Voice/` — all 10 files | All 10 Voice module files contain `Author: VOS4 Development Team` in the file header. Same Rule 7 violation. | Same fix as above. |
| **LOW** | `AvanueUIVoiceHandlers/.../overlay/DrawerHandler.kt` | `object NoAccessibility : DrawerResult()` should be `data object NoAccessibility : DrawerResult()`. Non-data objects in sealed classes break structural equality (`==`), `toString()`, and `copy()` usage in when-expressions with exhaustiveness checks. | Change to `data object`. Same pattern in `RatingHandler.kt` (NoAccessibility, NoRatingWidget), `ToggleHandler.kt` (NoAccessibility, NoFocusedToggle), `TimePickerHandler.kt` (NoAccessibility, NoTimePickerWidget), `FileUploadHandler.kt` (PickerOpened, PickerAlreadyOpen, etc.). |
| **LOW** | `AvanueUIVoiceHandlers/.../input/AutocompleteHandler.kt` | `HIDE_SUGGESTIONS_PATTERN` contains "close" and "dismiss" as candidate matches. These same words are in `AlertHandler.DISMISS_COMMANDS` and `ModalHandler.CLOSE_COMMANDS`. Cross-handler ambiguity: "close" in the context of an autocomplete widget could be intercepted by multiple handlers if routing precedence is not strictly enforced. | Confirm that autocomplete's handler priority in `MagicVoiceHandlerRegistry` is higher than alert/modal handlers, or use more specific phrases ("close suggestions", "hide suggestions") to avoid collision. |
| **LOW** | `AvanueUIVoiceHandlers/.../input/ColorPickerHandler.kt` | `NAMED_COLORS["lime"] = "#00FF00"` which is the same hex value as `NAMED_COLORS["green"] = "#00FF00"`. Lime and green are distinct colors in most design systems (lime is yellow-green, green is pure green). This maps two different color names to the same hex. | Set `lime` to `#32CD32` (LimeGreen) or `#00FF00` with an explicit comment if intentional identity with green is desired. |
| **LOW** | `AvanueUIVoiceHandlers/.../input/RatingHandler.kt` | `WORD_TO_NUMBER` map includes numeric string literals ("1" → 1, "2" → 2, ..., "10" → 10) alongside word forms ("one" → 1, etc.). The map is used for voice input matching — numeric strings are redundant because the phrase parser should already extract digit strings directly via regex. The hybrid map adds confusion about the expected input type. | Document clearly why numeric strings are included, or remove them if the calling regex already handles digit sequences. |
| **LOW** | `Voice/src/main/.../theme/VoiceUITheme.kt` | `VoiceUITheme` wraps `MaterialTheme(colorScheme = ...)` using hardcoded hex colors `Color(0xFF6200EE)`. This is entirely disconnected from AvanueUI v5.1 token system. The light and dark color schemes are identical (same primary/secondary/tertiary values). | Replace with `AvanueThemeProvider` or at minimum reference the project's `AvanueTheme` tokens. The identical light/dark schemes are almost certainly a bug. |
| **LOW** | `Voice/src/main/.../model/DuplicateCommandModel.kt:13` | `import android.graphics.RectF` — this is in `src/main/` (Android-only source set), which is acceptable. However `DuplicateCommandModel` is intended as a shared data model for disambiguation. If it ever needs to be used in `commonMain`, the `android.graphics.RectF` dependency blocks KMP sharing. | Consider using a custom `data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)` in commonMain to allow future KMP sharing. Note this is a Low because the file is currently in androidMain equivalent. |
| **LOW** | `Voice/src/main/.../components/VoiceComponents.kt` | No AVID voice identifiers (no `Modifier.semantics { contentDescription = "Voice: ..." }`) on any interactive element. `VoiceCommandButton`, `GlassmorphismCard` interactions are not voice-accessible despite being in a voice-focused module. Violates the AVID mandatory rule. | Add `Modifier.semantics { contentDescription = "Voice: ${text}" }` to `VoiceCommandButton` and `Modifier.semantics { role = Role.Button }` to `GlassmorphismCard` if interactive. |
| **LOW** | `AssetManager/.../commonMain/AssetIntegration.kt` | `AssetPreloader.preloadedIcons` and `preloadedImages` are plain `mutableMapOf<String, Icon>()` accessed from multiple coroutine callers via `suspend fun` methods. The `suspend` keyword does not provide thread-safety on the underlying map. Under concurrent preload calls, the map can be corrupted. | Protect the preloader maps with a `Mutex`, or use `ConcurrentHashMap` (JVM only — prefer Mutex for KMP compatibility). |
| **LOW** | `AssetManager/.../commonMain/AssetIntegration.kt` | `AssetResolver.cdnBaseUrl` and `localAssetsPath` are plain `var` properties on a singleton object, mutated via `CdnConfig.apply()`. No synchronization. Concurrent reads and writes from different coroutines can observe inconsistent state. | Make them `@Volatile` at minimum, or protect with `Mutex` for full consistency. |
| **LOW** | `AssetManager/.../commonMain/ManifestManager.kt:190-229` | `exportToJson()` calls `json.encodeToString(manifest)` where `manifest` is of type `IconLibraryManifest` / `ImageLibraryManifest` — these are internal `data class` types not annotated with `@Serializable`. The code compiles only because `kotlinx-serialization` is configured with `ignoreUnknownKeys = true`, but `encodeToString` requires `@Serializable` on the serialized type. This will throw a `SerializationException` at runtime. | Add `@Serializable` to `IconLibraryManifest`, `IconManifestEntry`, `ImageLibraryManifest`, and `ImageManifestEntry` in `AssetRepository.kt`. They are already annotated as `internal data class` — adding `@Serializable` is the correct fix. |
| **LOW** | `AssetManager/.../commonMain/AssetSearch.kt:183-193` | `searchByCategory()` passes `query = ""` (empty string) to `searchIcons()`/`searchImages()`. Both methods have an early return `if (query.isBlank()) return emptyList()`. Result: `searchByCategory()` will always return an empty list regardless of what category is requested. The category filter is never applied. | Either refactor `searchIcons()`/`searchImages()` to allow empty query with filters, or implement `searchByCategory()` as a direct filter on the library's icon list without routing through the query-based methods. |
| **LOW** | `AssetManager/.../commonMain/AssetSearch.kt:207` | `searchByTags()` calls `searchIcons(query = tags.first(), ...)`. If `tags` is empty, this throws `NoSuchElementException`. The method has no guard for empty `tags`. | Add `if (tags.isEmpty()) return emptyList()` guard before `tags.first()`. |
| **LOW** | `AssetManager/.../androidMain/AssetStorage.kt:436-442` | `AndroidStorageUtils.formatBytes()` uses `String.format("%.2f GB", ...)` — JVM-specific `java.lang.String.format`. This is in `androidMain` so it compiles, but it is an unnecessarily JVM-style utility in a file that could otherwise use Kotlin idioms. Minor style note rather than a compile error. | Use `"${"%.2f".format(bytes.toDouble() / gb)} GB"` or a plain string template with rounding. |
| **LOW** | `AssetManager/.../commonMain/MaterialIconsLibrary.kt` | `MaterialIconsLibrary.load()` creates `Icon` objects with `svg = null` and `png = null` for all 2,235 icons. A comment says "SVG data would be loaded from resources". Icons returned by `load()` have no renderable data — calling `hasFormat(SVG)` returns `false`, `getPngForSize()` returns `null`. The library description says "provides access to 2,235 icons" but they are all empty shells. | Either populate SVG data from bundled resources, or document that `MaterialIconsLibrary` is a metadata-only index requiring a separate data load step. The current state is misleading. |

---

## Recommendations

### Priority 1 — Fix Before Next Build (CRITICAL)

1. **KMP violations in commonMain** (7 files): `DatePickerHandler.kt`, `FileUploadHandler.kt`, `TimePickerHandler.kt`, `RangeSliderHandler.kt`, `SliderHandler.kt`, `StepperHandler.kt`, `ToastHandler.kt`. These prevent the module from compiling on any non-JVM target. Fix by migrating to KMP-safe equivalents:
   - `java.util.Calendar` → `kotlinx-datetime` `LocalDate`/`LocalTime`
   - `java.text.SimpleDateFormat` → `kotlinx-datetime` formatters
   - `java.util.regex.Pattern` → `Regex()`
   - `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()`
   - `ConcurrentLinkedDeque` → `ArrayDeque` + `Mutex`
   - `android.net.Uri` → platform-neutral wrapper type
   - `String.format("%.1f", n)` → create a `formatOneDecimal(Float): String` expect/actual function shared across the 4 affected handlers

2. **Create a shared `formatOneDecimal` utility** to fix all four `String.format("%.1f", ...)` violations at once and remove the DRY violation (the same format string appears 5 times across 4 files).

### Priority 2 — Logic Bugs (HIGH)

3. **`TableHandler.extractNumber()`**: Replace `contains()` with word-boundary regex matching to prevent "phone" → "one" misparse.

4. **`DialogHandler.findPositiveButton()`**: Remove the silent fallback to `lastOrNull()`. Return `null` and surface a `NoPositiveButtonFound` result to avoid confirming destructive actions unintentionally.

5. **Substring-match command leakage** in `AlertHandler`, `ModalHandler`, `SnackbarHandler`: All three use `contains()` for command matching. Standardize on equality-or-prefix matching to prevent cross-handler command interception.

6. **`GlassMorphism.kt` empty file** (Voice module): Either implement or delete. An empty theme file with no content is misleading and a Rule 1 violation.

### Priority 3 — Maintainability (MEDIUM)

7. **Duplicate AssetManager source tree**: Delete `AssetManager/src/` (the second tree) and canonicalize on `AssetManager/AssetManager/src/`. Update `build.gradle.kts` accordingly. 22 duplicate files is a maintenance hazard.

8. **AssetVersionManager persistence stubs**: Implement `saveVersionHistory()` and `loadVersionHistory()` using the injected `AssetRepository`. Version history that resets on every restart is not functional versioning.

9. **ManifestManager stubs**: `ManifestMigration.migrateManifest()`, `ManifestBackup.restoreManifest()`, and `ManifestBackup.listBackups()` all silently return success or empty without doing anything. Implement or return explicit `NotImplementedError`.

10. **`AssetSearch.searchByCategory()` always returns empty**: Fix the call pattern — either allow empty query with category filter or bypass the query check. The method is entirely non-functional as written.

11. **`AssetSearch.searchByTags()` crashes on empty set**: Add `if (tags.isEmpty()) return emptyList()` guard.

12. **Add `@Serializable` to manifest internal data classes** in `AssetRepository.kt` (`IconLibraryManifest`, `IconManifestEntry`, `ImageLibraryManifest`, `ImageManifestEntry`). Without this, `ManifestManager.exportToJson()` will throw `SerializationException` at runtime.

13. **Mock tests in Voice module**: `VoiceUIComponentsTest.kt` has 15 test methods that test only trivial constants. `TestExtensions.kt` provides a `captureToImage()` that returns a blank 100x100 bitmap. Rewrite tests to test real composable behaviour.

14. **`data object` for sealed class singletons**: Replace `object` with `data object` for `NoAccessibility`, `NoRatingWidget`, `NoFocusedToggle`, `NoTimePickerWidget`, `PickerOpened`, `PickerAlreadyOpen`, etc. across 5 handler files.

### Priority 4 — AVID and Theme Compliance (LOW)

15. **`VoiceUITheme.kt` and `VoiceComponents.kt`**: Both use `MaterialTheme.colorScheme.*` directly, violating MANDATORY RULE #3. Migrate to `AvanueTheme.colors.*`. Fix the identical light/dark color schemes in `VoiceUITheme.kt` (they are the same hardcoded values — dark mode has no effect).

16. **Missing AVID semantics on `VoiceComponents.kt`**: `VoiceCommandButton` has no `contentDescription`. Add `Modifier.semantics { contentDescription = "Voice: $text" }`.

17. **Rule 7 violations**: All 45 files across `AvanueUIVoiceHandlers/` and `Voice/` contain `Author: VOS4 Development Team`. Change to `Author: Manoj Jhawar` or remove the `Author:` line entirely.

18. **`MaterialIconsLibrary` empty icon shells**: Document clearly that `svg = null` / `png = null` icons require a separate data loading step, or populate them from bundled resources.

---

## File-by-File Summary

### AvanueUIVoiceHandlers — 35 files

| File | Status | Key Issues |
|------|--------|-----------|
| `MagicVoiceHandlerRegistry.kt` | Low | Rule 7 only |
| `AvatarHandler.kt` | Low | Rule 7 only |
| `BadgeHandler.kt` | Low | Rule 7 only |
| `Canvas3DHandler.kt` | Medium | Stale zoom feedback + Rule 7 |
| `CarouselHandler.kt` | Low | Rule 7 only |
| `ChipHandler.kt` | Low | Rule 7 only |
| `ProgressHandler.kt` | Low | Broad status regex + Rule 7 |
| `TableHandler.kt` | High | `contains()` number extraction bug + Rule 7 |
| `TreeViewHandler.kt` | Low | Rule 7 only |
| `AlertHandler.kt` | High | Substring cross-handler leakage + Rule 7 |
| `ConfirmHandler.kt` | Medium | Dead `containsDestructiveKeywords()` + Rule 7 |
| `DialogHandler.kt` | High | Silent destructive fallback in `findPositiveButton()` |
| `DrawerHandler.kt` | Low | `object` not `data object` + Rule 7 |
| `ModalHandler.kt` | High | Substring cross-handler leakage + Rule 7 |
| `SnackbarHandler.kt` | High | Substring leakage on all 3 cmd sets + Rule 7 |
| `ToastHandler.kt` | **Critical** | `ConcurrentLinkedDeque` + `System.currentTimeMillis()` in commonMain + Rule 7 |
| `AppBarHandler.kt` | Low | Rule 7 only |
| `BottomNavHandler.kt` | Low | Rule 7 only |
| `BreadcrumbHandler.kt` | Medium | Dead `name` param + Rule 7 |
| `PaginationHandler.kt` | Low | Rule 7 only |
| `TabsHandler.kt` | Low | Rule 7 only |
| `AutocompleteHandler.kt` | Low | "close"/"dismiss" collision risk + Rule 7 |
| `ColorPickerHandler.kt` | Low | lime=green duplicate + Rule 7 |
| `DatePickerHandler.kt` | **Critical** | `Calendar`+`SimpleDateFormat`+`Pattern` in commonMain + Rule 7 |
| `FileUploadHandler.kt` | **Critical** | `android.net.Uri` in commonMain + Rule 7 |
| `IconPickerHandler.kt` | Low | Rule 7 only |
| `MultiSelectHandler.kt` | Medium | `@Volatile` not atomic + Rule 7 |
| `RangeSliderHandler.kt` | **Critical** | `String.format("%.1f")` in commonMain + Rule 7 |
| `RatingHandler.kt` | Low | Non-`data object` + numeric string redundancy + Rule 7 |
| `SearchBarHandler.kt` | Low | Empty filter pass-through + Rule 7 |
| `SliderHandler.kt` | **Critical** | `String.format("%.1f")` in commonMain + Rule 7 |
| `StepperHandler.kt` | **Critical** | `String.format("%.1f")` twice in commonMain + Rule 7 |
| `TagInputHandler.kt` | Medium | Dead `parseTagIndex()` + Rule 7 |
| `TimePickerHandler.kt` | **Critical** | `Calendar`+`Locale`+`Pattern` + `String.format(Locale.US)` in commonMain + broad "time to" match + Rule 7 |
| `ToggleHandler.kt` | Low | Non-`data object` + Rule 7 |

### Voice — 10 files

| File | Status | Key Issues |
|------|--------|-----------|
| `SpatialButton.kt` | — | Empty file (1 line) |
| `GlassMorphism.kt` | High | Empty file (1 line) — Rule 1 violation |
| `TestExtensions.kt` | Medium | Mock `captureToImage()` returns blank bitmap + Rule 7 |
| `VisualRegressionTest.kt` | Medium | Tests use mock capture — no real visual regression + Rule 7 |
| `VoiceUIComponentsTest.kt` | Medium | All 15 tests are trivial mock assertions + Rule 7 |
| `NumberToWordsConverter.kt` | High | `Locale.Builder()` requires API 21, no guard + Rule 7 |
| `VoiceStatus.kt` | Low | Rule 7 only (clean model) |
| `VoiceComponents.kt` | High | `MaterialTheme.*` usage (Rule 3 violation), no AVID + Rule 7 |
| `VoiceUITheme.kt` | Low | `MaterialTheme` wrapper, identical light/dark schemes + Rule 7 |
| `DuplicateCommandModel.kt` | Low | `android.graphics.RectF` (androidMain = OK, future KMP risk) + Rule 7 |

### AssetManager — 29 files (14 unique + 15 duplicates)

| File | Status | Key Issues |
|------|--------|-----------|
| `commonMain/AssetStorage.kt` | Medium | `StorageCleanup` stubs (3 no-ops) |
| `commonMain/AssetProcessor.kt` | Clean | Good `expect` declaration, KMP-safe utilities |
| `commonMain/ImageLibrary.kt` | Clean | Correct `equals()`/`hashCode()` for `ByteArray` |
| `commonMain/AssetVersionManager.kt` | Medium | `saveVersionHistory()`/`loadVersionHistory()` are stubs (TODO) |
| `commonMain/AssetManager.kt` | Clean | Good use of `Mutex` throughout |
| `commonMain/IconLibrary.kt` | Clean | Clean data model |
| `commonMain/AssetRepository.kt` | Low | Manifest data classes need `@Serializable` |
| `commonMain/AssetIntegration.kt` | Low | `AssetPreloader` maps not thread-safe |
| `commonMain/ManifestManager.kt` | Medium | 4 stub operations returning empty/success silently |
| `commonMain/MaterialIconsLibrary.kt` | Low | 2235 icon specs with `svg=null`/`png=null` |
| `commonMain/BuiltInLibraries.kt` | Clean | 20 working SVG icons; correct KMP-safe code |
| `androidMain/AssetStorage.kt` | Medium | `loadImage()` returns `Dimensions(0,0)` hardcoded |
| `androidMain/AssetProcessor.kt` | Clean | Correct Bitmap handling with proper recycle |
| `androidMain/AssetRepository.kt` | Clean | Correct JSON serialization with file I/O |
| `AssetManager/src/` (second tree) | Medium | **Exact duplicate** of above 14 files — 15 duplicate files |
| `AssetSearch.kt` | Low | `searchByCategory()` always returns empty; `searchByTags()` crashes on empty |

---

*Report saved: `docs/deepreview/AvanueUI-Voice/AvanueUI-Voice-Assets-DeepReview-260220.md`*

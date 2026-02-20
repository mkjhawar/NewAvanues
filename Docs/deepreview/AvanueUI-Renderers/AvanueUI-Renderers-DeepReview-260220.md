# AvanueUI Renderers — Deep Code Review
**Date:** 260220
**Scope:** All 98 .kt files in `Modules/AvanueUI/Renderers/` (Android, Desktop, iOS)
**Reviewer:** Code Reviewer Agent (Sonnet)

---

## Summary

The AvanueUI Renderers module is a cross-platform component renderer layer bridging AvaElements DSL to
Compose (Android/Desktop) and UIKit/SwiftUI (iOS). The module suffers from three systemic, architectural
defects that affect every file: (1) the entire Android/Desktop renderer bypasses the mandatory AvanueTheme
system by wrapping components inside raw `MaterialTheme {}`, violating MANDATORY RULE #3 throughout;
(2) every interactive callback in every mapper and extension file is a no-op stub, meaning no user
interaction anywhere in this layer actually fires a callback; (3) zero AVID voice identifiers exist
on any interactive element across all 98 files, violating the voice-first zero-tolerance rule.
Additionally, the Desktop renderer contains fully hardcoded placeholder renders (Rule 1 violations).
The iOS UIKit component files (IOSAppBar, IOSBottomNav, IOSTabs, IOSDrawer, etc.) consistently stub
out all callbacks with comments "Note: In production, use delegate pattern", making them functionally
inert. The SwiftUI bridge mapper files (AdvancedComponentMappers, DataComponentMappers) are the most
complete area; they produce correct `SwiftUIView` bridge models but use hardcoded color constants
instead of theme tokens.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **CRITICAL** | `Android/src/androidMain/.../ThemeConverter.kt:136-141` | `WithMaterialTheme()` wraps the entire renderer output in raw `MaterialTheme(colorScheme = ...)`. This means ALL Android renders use M3 MaterialTheme, not AvanueTheme — violating MANDATORY RULE #3 at the root level. Every component inherits the wrong theme. | Replace `WithMaterialTheme` with `AvanueThemeProvider(...)` using the mapped palette+mode tokens. The converter should translate to `AvanueColorPalette` + `MaterialMode`, not a raw `ColorScheme`. |
| **CRITICAL** | `Android/src/androidMain/.../ComposeRenderer.kt:151` | `themeConverter.WithMaterialTheme(activeTheme)` is called for every component tree — propagates the above violation to all rendered output. | Pass AvanueTheme tokens directly; remove `WithMaterialTheme` call. |
| **CRITICAL** | `Android/src/androidMain/.../mappers/input/AdvancedInputMappers.kt:138-140, 569, 830-831` | `MaterialTheme.colorScheme.*` used directly — violates MANDATORY RULE #3. Lines: `MaterialTheme.colorScheme.primary` (L138), `.onSurface` (L140), `.onSurfaceVariant` (L569), `.primary`/`.secondary` (L830-831). | Replace all `MaterialTheme.colorScheme.*` with `AvanueTheme.colors.*`. |
| **CRITICAL** | `Android/src/androidMain/.../mappers/input/AdvancedInputMappers.kt` | `MenuAnchor()` called without required `MenuAnchorType` parameter at multiple ExposedDropdown usage sites. This is a **compilation error** introduced in JB Compose 1.7.3 (breaking API change). The renderer will not compile on this branch. | Change to `MenuAnchor(MenuAnchorType.PrimaryNotEditable)` or `MenuAnchorType.PrimaryEditable` as appropriate. |
| **CRITICAL** | `iOS/src/iosMain/.../mappers/Phase2FeedbackMappers.kt:443-532` | Defines `AlertComponent`, `ToastComponent`, `SnackbarComponent`, `ModalComponent`, `DialogComponent`, `BannerComponent`, `ContextMenuComponent`, `AlertSeverity`, `BannerSeverity`, `SnackbarDuration` as data classes/enums in `iosMain`. These **shadow** the same types defined in `commonMain`, creating KMP type conflicts. Components constructed here cannot be passed to common code and vice versa. | Remove all component/enum definitions from `iosMain`. Import them from `commonMain`. |
| **CRITICAL** | `Desktop/src/desktopMain/.../mappers/Phase1Mappers.kt:102-150` | `RenderContainer` displays hardcoded `"Container"` text, `RenderRow` shows `"Row content"`, `RenderColumn` shows `"Column content"`, `RenderCard` shows `"Card content"`, `RenderIcon` always renders `Icons.Default.Home` ignoring `c.name`, `RenderImage` renders text `"Image: source"` instead of any image. All six are Rule 1 violations — production stubs that silently show wrong content. | Implement actual rendering: use `component.children.forEach { renderer.RenderComponent(it) }` for containers, `IconResolver.resolve(c.name)` for icons, `AsyncImage` for images. |
| **CRITICAL** | `Android/src/androidMain/.../extensions/InputExtensions.kt:105` | `RenderRangeSlider` `onValueChange = { /* Component uses updateMin/updateMax methods */ }` — the range slider's value change callback is a no-op. Users can drag the slider but nothing updates. | Wire to `component.onRangeChanged(lowerValue, upperValue)` or equivalent component callback. |
| **CRITICAL** | `Android/src/androidMain/.../extensions/InputExtensions.kt:284, 307` | `RenderRadioGroup` all radio buttons have `onClick = { /* No callback in component */ }` — radio button selection is completely non-functional. | Add an `onSelectionChanged: (Int) -> Unit` callback to `RadioGroupComponent`, wire buttons to it. |
| **CRITICAL** | `Android/src/androidMain/.../extensions/InputExtensions.kt:341, 357` | `RenderAutocomplete` text field `onValueChange = { /* ... */ }` and suggestion click `{ // Component uses selectSuggestion method }` are both no-ops. The autocomplete cannot receive user input at all. | Wire `onValueChange` to state update; wire suggestion click to `component.onSuggestionSelected(suggestion)`. |
| **CRITICAL** | `Android/src/androidMain/.../mappers/DialogMapper.kt:25, 31` | Both dialog buttons `onClick = { /* Confirm/Cancel action */ }` are no-ops. Every dialog rendered via this mapper is non-functional — confirm and cancel do nothing. | Wire to `component.onConfirm()` and `component.onCancel()`. |
| **CRITICAL** | `Android/src/androidMain/.../mappers/ChipMapper.kt:29, 43` | Both `InputChip` and `FilterChip` `onClick = { /* no onClick on ChipComponent */ }` — chips are entirely non-interactive despite being tappable UI elements. | Add `onClick: (() -> Unit)?` to `ChipComponent`; wire both chip types to it. |
| **CRITICAL** | `iOS/src/iosMain/.../IOSDialog.kt` | Both confirm and cancel `UIAlertAction` handlers are `{ /* onConfirm/onCancel callback */ }` — dialog buttons are non-functional no-ops in the UIKit renderer. | Wire to `component.onConfirm?.invoke()` and `component.onCancel?.invoke()`. |
| **CRITICAL** | `iOS/src/iosMain/.../OptimizedSwiftUIRenderer.kt:36` | `renderCache = mutableMapOf<Int, SwiftUIView>()` using `component.hashCode()` as key. hashCode collisions between different components return the wrong cached view silently — incorrect UI rendered with no error. | Use a content-based stable key (component `id` + class name) instead of `hashCode()`. |
| **CRITICAL** | `iOS/src/iosMain/.../OptimizedSwiftUIRenderer.kt` (using `System.currentTimeMillis()`)| `System.currentTimeMillis()` is called in `renderWithProfiling`. `System` is JVM/Android only — **KMP violation** in `iosMain`. Will not compile for iOS target. | Replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`. |
| **HIGH** | `Android/src/androidMain/.../extensions/NavigationFeedbackExtensions.kt:179-184, 231, 373-376, 398-400, 466-479` | Pervasive `MaterialTheme.colorScheme.*` usage in `ToastComponent.Render`, `SnackbarComponent.Render`, `Confirm.Render`, `DialogComponent.Render`. All interactive callbacks (snackbar action L231, dialog confirm L469, dialog cancel L475) are no-ops. | Replace `MaterialTheme.colorScheme.*` with `AvanueTheme.colors.*`. Wire all interactive callbacks. |
| **HIGH** | `Android/src/androidMain/.../extensions/LayoutDisplayExtensions.kt` (multiple lines) | `MaterialTheme.colorScheme.*` used at L254-255, L317-325, L471-478, L563-565, L700-708, L819, L893-903, L958-963, L1016-1018, L1039-1041, L1177-1231, L1240-1264. Affects SurfaceComponent, DividerComponent, CircularProgress, Spinner, StatCard, StickyHeader, Banner, NotificationCenter, Table, Timeline — all use wrong theme colors. | Replace all `MaterialTheme.colorScheme.*` with `AvanueTheme.colors.*`. |
| **HIGH** | `Android/src/androidMain/.../mappers/AlertMapper.kt:138-160` | `getSeverityColors()` uses `MaterialTheme.colorScheme.error`, `MaterialTheme.colorScheme.surface`, etc. — alert severity colors derived from wrong theme system. | Replace with `AvanueTheme.colors.*` equivalents. |
| **HIGH** | `Android/src/androidMain/.../mappers/AvatarMapper.kt:49, 54, 56` | `MaterialTheme.colorScheme.primaryContainer`, `MaterialTheme.typography.titleMedium`, `MaterialTheme.colorScheme.onPrimaryContainer` used for avatar initials rendering. | Replace with `AvanueTheme.colors.*`. |
| **HIGH** | `Android/src/androidMain/.../mappers/BreadcrumbMapper.kt:57, 62, 81-82` | `MaterialTheme.colorScheme.onSurface`, `.primary`, `.onSurfaceVariant` used for breadcrumb text and separator colors. | Replace with `AvanueTheme.colors.*`. |
| **HIGH** | `Android/src/androidMain/.../mappers/ToastMapper.kt:28-33, 75, 80` | `MaterialTheme.colorScheme.*` for background/text colors; `MaterialTheme.typography.bodyMedium`; action button `onClick = { /* Action handler */ }` is a no-op. | Replace theme refs; wire action callback. |
| **HIGH** | `Android/src/androidMain/.../mappers/ConfirmMapper.kt:20-24, 45-46, 48` | `MaterialTheme.colorScheme.*` throughout getSeverityColors(). Confirm and cancel callbacks are no-ops (same as DialogMapper). | Replace theme refs; wire callbacks. |
| **HIGH** | `Android/src/androidMain/.../mappers/ModalMapper.kt:46` | `MaterialTheme.shapes.large` used for modal shape. | Replace with AvanueTheme shape token. |
| **HIGH** | `Android/src/androidMain/.../mappers/feedback/AdvancedFeedbackMappers.kt:58, 60` | `MaterialTheme.shapes.medium` (L58) and `MaterialTheme.colorScheme.surface` (L60) in BottomSheetMapper. | Replace with `AvanueTheme` equivalents. |
| **HIGH** | `Android/src/androidMain/.../Desktop/src/desktopMain/.../mappers/Phase1Mappers.kt:74, 102, etc.` | `MaterialTheme.typography.*` and `MaterialTheme.shapes.*` used throughout Desktop mappers (L74, L102). | Replace with `AvanueTheme` tokens. |
| **HIGH** | `Android/src/androidMain/.../mappers/AppBarMapper.kt:64` | All AppBar action icons render `Icons.AutoMirrored.Filled.ArrowBack` as a hardcoded placeholder with comment "In a real implementation, you'd map icon strings to actual icons". Every action button shows a back arrow regardless of the specified icon name. | Wire to `IconResolver.resolve(action.icon)`. |
| **HIGH** | `Android/src/androidMain/.../mappers/SnackbarMapper.kt:22` | Snackbar action `onClick = { /* Action handler */ }` is a no-op. | Wire to `component.action?.onClick?.invoke()`. |
| **HIGH** | `Android/src/androidMain/.../extensions/InputExtensions.kt:383` | `RenderFileUpload` click handler `{ // File picker would be triggered here via Activity... }` is a no-op — file upload button never launches the system picker. | Implement via `ActivityResultContracts.GetContent()` or accept a launcher parameter. |
| **HIGH** | `Android/src/androidMain/.../ThemeConverter.kt:78-84` | `surfaceDim`, `surfaceBright`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest` all collapse to either `surface` or `surfaceVariant`. These 7 separate M3 semantic tokens are collapsed, causing visually incorrect component colors (e.g., dialogs, cards, menus all show wrong surface shade). | Map each M3 surface container token to the nearest AvanueTheme semantic token individually; do not collapse multiple into one. |
| **HIGH** | `iOS/src/iosMain/.../IOSToast.kt:23-26` | `layer.masksToBounds = true` set on the same view that then sets `layer.shadowColor`, `shadowOpacity`, `shadowRadius`. `masksToBounds = true` clips all content and shadows outside view bounds — the drop shadow is always invisible. | Remove `layer.masksToBounds = true` from the toast container (or apply it only to a background sublayer). |
| **HIGH** | `iOS/src/iosMain/.../IOSVideoPlayer.kt:13` | `AVPlayer.playerWithURL(videoUrl!!)` — force-unwrap on `videoUrl!!` crashes immediately if the URL string is invalid or null. No null/error handling. | Guard with `?: run { renderError(); return }` and use `NSURL.URLWithString(url)?.let { ... }` to handle invalid URLs. |
| **HIGH** | `iOS/src/iosMain/.../IOSAppBar.kt:128-154` | `createBarButtonItem()` creates UIBarButtonItems with `target = null, action = null` and only stores a comment `// Store callback / // Note: In production, use proper target-action pattern`. All navigation and action buttons have zero click handling — the app bar is entirely non-functional. | Implement via ObjC target-action bridge or `UIAction` (iOS 14+). |
| **HIGH** | `iOS/src/iosMain/.../IOSBottomNav.kt:56-61` | `component.onItemSelected?.let { callback -> /* Delegate would call: callback(newSelectedIndex) */ }` — tab selection callback is never wired. Tab bar changes are silently ignored. | Implement UITabBarDelegate and invoke `callback(newIndex)` from `tabBar(_:didSelect:)`. |
| **HIGH** | `iOS/src/iosMain/.../IOSTabs.kt:72-74` | `component.onTabSelected?.let { callback -> /* Target-action would call: ... */ }` — segment control selection change is never wired. Tab selection is silent. | Use `addTarget` with `UIControlEventValueChanged` to invoke the callback. |
| **HIGH** | `iOS/src/iosMain/.../IOSDrawer.kt:56-59, 188-190` | Both the overlay tap-to-dismiss gesture (`// Note: In production, connect gesture to onDismiss callback`) and drawer item tap gestures (`// Note: In production, connect gesture to onClick callback`) are never wired. Drawer is visually correct but entirely non-interactive. | Connect `UITapGestureRecognizer` targets via ObjC wrapper or `UIAction`. |
| **HIGH** | `iOS/src/iosMain/.../IOSSearchBar.kt:65-74` | Both `component.onValueChange` and `component.onSearch` delegate comment stubs — search bar text input and submit callbacks never fire. | Implement `UISearchBarDelegate` and invoke callbacks. |
| **HIGH** | `iOS/src/iosMain/.../IOSAccordion.kt:88-90` | Accordion header tap gesture `// Note: In production, connect to onToggle` — tap never fires, accordion cannot be expanded or collapsed. | Wire `UITapGestureRecognizer` to invoke `component.onToggle?.invoke(index)`. |
| **HIGH** | `iOS/src/iosMain/.../IOSTimePicker.kt:91-99` | `component.onTimeSelected?.let { callback -> /* commented-out code */ }` — time picker selection callback is a comment stub. Selected time is never reported to caller. | Implement via `addTarget` with `UIControlEventValueChanged`. |
| **HIGH** | `iOS/src/iosMain/.../IOSDatePicker.kt:90-93` | `component.onDateChange?.let { callback -> /* Target-action pattern would call: ... */ }` — date picker change callback stub. Date selection never reported. | Implement via `addTarget` with `UIControlEventValueChanged`. |
| **HIGH** | `iOS/src/iosMain/.../IOSDropdown.kt:40-43` | UIPickerView has no dataSource/delegate wired — picker shows no options and selection changes are never reported. Comment says "In production, implement UIPickerViewDataSource and UIPickerViewDelegate". | Implement `UIPickerViewDataSource` + `UIPickerViewDelegate` via ObjC protocol bridge. |
| **HIGH** | `Android/src/androidMain/.../extensions/InputExtensions.kt:32` | `private val modifierConverter = ModifierConverter()` defined at **file level** — this is a shared mutable singleton across all recompositions. In Compose, this causes undefined behavior when multiple composables read it concurrently during recomposition. | Move `ModifierConverter()` inside each composable function, or use `remember { ModifierConverter() }`. |
| **HIGH** | `Android/src/androidMain/.../MagicElementsCompose.kt:29, 55, 83` | `ComposeRenderer()` instantiated inline on every recomposition in `AvaUI()`, `RenderComponent()`, and `RenderMagicElement()`. Creates a new renderer object (with all its state) on every frame — unnecessary allocation and potential loss of renderer-internal state. | Wrap with `remember { ComposeRenderer() }` in each composable. |
| **HIGH** | `iOS/src/iosMain/.../OptimizedSwiftUIRenderer.kt` (renderCache) | `renderCache` is a plain `mutableMapOf` accessed from `render()` (which can be called from any thread). No synchronization. Data race if renderer is used concurrently. | Add `@Synchronized` or replace with a thread-safe concurrent map. |
| **HIGH** | `Android/src/androidMain/.../mappers/ContextMenuMapper.kt` and `NavigationFeedbackExtensions.kt` (ContextMenu.Render) | Both implementations start with `var expanded by remember { mutableStateOf(true) }` — context menu renders in permanently expanded state from first composition with no external trigger. Context menu is permanently open on screen. | Initialize `expanded = false`; add an `isOpen` parameter or external trigger to the component. |
| **HIGH** | `Android/src/androidMain/.../mappers/input/AdvancedInputMappers.kt` (DateRangePickerMapper) | `component.onRangeChanged` is never invoked when the confirm button is clicked — date range selection has no callback. | Wire confirm button `onClick` to invoke `component.onRangeChanged(startDate, endDate)`. |
| **MEDIUM** | `Android/src/androidMain/.../extensions/LayoutDisplayExtensions.kt:514` | `SkeletonComponent.Render` uses `Color.LightGray.copy(alpha=0.3f)` hardcoded — theme-unaware skeleton color. Will look wrong in dark mode. | Replace with `AvanueTheme.colors.surfaceVariant.copy(alpha=0.3f)`. |
| **MEDIUM** | `Android/src/androidMain/.../extensions/LayoutDisplayExtensions.kt:1527-1544` | `TreeViewComponent.Render` wraps all tree nodes inside a single `item{}` block in a `LazyColumn` — all nodes are always composed regardless of scroll position. Defeats the purpose of using a lazy list. | Use `items(nodes)` to lazily compose each node. |
| **MEDIUM** | `Android/src/androidMain/.../mappers/foundation/ButtonMapper.kt` | `ButtonScope.ButtonStyle.Secondary` and `.Tertiary` both map to `FilledTonalButton` — the two variants are visually identical, making it impossible to distinguish them. | Map `Secondary` to `FilledTonalButton` and `Tertiary` to `OutlinedButton` (standard M3 hierarchy). |
| **MEDIUM** | `Android/src/androidMain/.../mappers/foundation/ImageMapper.kt` | Dead branch: `if (source.startsWith("http")) ... else { AsyncImage(...) }` — both branches call `AsyncImage` with the same arguments, rendering the `if/else` meaningless. | Remove the dead `else` branch; the comment says "simplified version" — implement proper local asset loading in the else branch via `painterResource`. |
| **MEDIUM** | `Android/src/androidMain/.../IconResolver.kt:80, 103, 119, 191` | Duplicate `when` branch keys: `"phone"` appears at L103 and L191 in `getFilledIcon()`; `"share"` appears at L80 and L119. The second branch is unreachable dead code in both cases. | Remove duplicate `when` branches. Run `when` expression with lint to catch all duplicates. |
| **MEDIUM** | `Android/src/androidMain/.../mappers/foundation/IconMapper.kt:39` | TODO comment: "TODO: Expand this mapping to support more icons". Only 20 icons mapped vs `IconResolver.kt` which has 100+ — callers using `IconMapper` receive fallback icons for the majority of icon names. | Consolidate to use `IconResolver` for the full mapping; delete `IconMapper` as a duplicate with incomplete coverage. |
| **MEDIUM** | `Android/src/androidMain/.../OpenGLRenderer.kt` (PBR fragment shader) | The PBR fragment shader is commented `// (PBR shader implementation - simplified for now)` — it only renders ambient light, no PBR computation is performed. The renderer advertises PBR but delivers flat ambient shading. | Document clearly that PBR is not implemented, or implement it. Not a silent failure but a misleading API name. |
| **MEDIUM** | `Android/src/androidMain/.../ModifierConverter.kt` | `MagicModifier.Hoverable`, `.Focusable`, `.Animated`, `.Align`, `.Weight`, `Transformation.Translate` all return empty `Modifier` with comments — modifiers silently vanish. `Gradient.Linear` ignores the specified `angle` parameter — all linear gradients render as top-to-bottom regardless of angle. | Implement Hoverable via `hoverable()`, Focusable via `focusable()`, Weight via `weight()` in parent scope, Align via `wrapContentSize(align)`, gradient angle via trigonometric start/end offset computation. |
| **MEDIUM** | `Android/src/androidMain/.../mappers/BottomNavMapper.kt` | `getIconForName()` only maps 5 icon names; all others fall back to `Icons.Default.Home` silently. Caller has no feedback that the icon was not found. | Expand to cover common navigation icons or integrate with `IconResolver`. Log a warning on fallback. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSRenderer.kt` | `applyAccessibility()` uses `component::class.simpleName` as fallback `accessibilityLabel` — reads e.g. "ButtonComponent" or "TextFieldComponent" to VoiceOver, which is meaningless to users. | Default to `component.label ?: component.id ?: component::class.simpleName?.removeSuffix("Component") ?: "element"`. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSRenderer.kt:228` | `parseColor()` returns `0x000000` (pure black) on any parse failure with no log entry. Silent wrong color for any badly formatted hex value. | Add a `log.warn("parseColor failed for '$hex'")` and return a visible fallback like `UIColor.systemGrayColor` for debugging. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSProgressBar.kt:16-18` | Indeterminate progress bar branch sets `progress = 0.0f` on a `UIProgressView` — shows an empty bar rather than activity. Comment acknowledges `UIActivityIndicatorView` should be used but it is not. | Return a `UIActivityIndicatorView.startAnimating()` for the indeterminate case, or switch to `IOSCircularProgressRenderer` which already handles this correctly. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSGrid.kt:38-41` | `UICollectionView` registered with no data source and no delegate. Comment says "In production, register cell classes and implement UICollectionViewDataSource and UICollectionViewDelegate". Renders an empty grid. | This is a Rule 1 stub — implement DataSource/Delegate, or remove the class if not yet needed. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSSlider.kt:122-138` | `renderRange()` returns a `UIView` containing only a label "Range Slider (Custom Implementation Required)" — Rule 1 stub. | Implement two-thumb range slider or document clearly that this feature is platform-unsupported. |
| **MEDIUM** | `iOS/src/iosMain/.../IOSDrawer.kt:94-107, 119-134` | Header and footer content replace real component rendering with hardcoded `UILabel` containing literal text `"Header"` and `"Footer"`. The actual `header`/`footer` component content (which is a `Component`) is never rendered. | Call `IOSRenderer().renderComponent(header)` to render the actual header/footer content. |
| **MEDIUM** | `iOS/src/iosMain/.../iOSExample.kt:101` | `renderer.renderUI(ui)!!` — force-unwrap on the result of `renderUI`. If `renderUI` returns null (e.g., `ui.root == null`), this crashes. Examples run in production context. | Replace `!!` with `?: return null` and propagate nullability, or assert non-null with a meaningful error message. |
| **MEDIUM** | `iOS/src/iosMain/.../bridge/ModifierConverter.kt:72-83` | `convertBackgroundGradient` silently falls back to using only the first gradient color — all multi-stop gradients render as a solid background. No warning logged. | At minimum log the fallback; ideally implement proper `LinearGradient`/`RadialGradient` SwiftUI modifiers. |
| **MEDIUM** | `iOS/src/iosMain/.../bridge/ModifierConverter.kt:163-174` | `convertTransform` and `convertAlign` both return `emptyList()` silently — transform and alignment modifiers are silently dropped for all SwiftUI-rendered components. | Implement `rotationEffect`/`scaleEffect`/`offset` for transforms; implement `frame(alignment:)` for align. |
| **MEDIUM** | `Android/src/androidMain/.../mappers/input/AdvancedInputMappers.kt` (multiple mappers) | Multiple separate mapper classes (`FilledButtonMapper`, `TagInputMapper`, `IconPickerMapper`, etc.) duplicate the theme-color pattern using hardcoded hex `SwiftUIColor` instead of theme tokens — these should use `AvanueTheme.colors.*` indirectly via the ThemeConverter. | Pass `theme` through all mapper `map()` calls and derive colors from theme tokens. |
| **MEDIUM** | `Android/src/androidMain/.../extensions/InputExtensions.kt:121` | `SimpleDateFormat` and `java.util.Locale` imported in `extensions/InputExtensions.kt` — `java.text.SimpleDateFormat` is Android/JVM only. This file is in `androidMain` so it compiles, but it is a dependency on the JVM date API rather than the preferred `kotlinx-datetime`. | Replace with `kotlinx.datetime` for consistency with the rest of the KMP codebase. |
| **MEDIUM** | Entire `Android/src/androidMain/.../mappers/` directory (all mappers) | There are two parallel implementations for many components: (a) `Mapper` classes in `mappers/` and (b) `.Render()` extension functions in `extensions/`. Examples: `AppBarMapper` vs `AppBarComponent.Render()`, `DialogMapper` vs `DialogComponent.Render()`, `SnackbarMapper` vs `SnackbarComponent.Render()`, `ToastMapper` vs `ToastComponent.Render()`, `ConfirmMapper` vs `Confirm.Render()`, `ContextMenuMapper` vs `ContextMenu.Render()`, `ModalMapper` vs `Modal.Render()`, `AdvancedNavigationMappers.NavigationDrawerMapper` vs `NavigationDrawerComponent.Render()`. This is massive DRY violation — double maintenance surface. | Audit which path `ComposeRenderer.render()` actually dispatches to. Delete the unused path. The extension function pattern appears to be the current dispatch path; the mapper classes may be legacy code. |
| **LOW** | `iOS/src/iosMain/.../IOSAppBar.kt:35, 40` and all iOS UIKit renderers | All iOS UIKit renderers hardcode `frame = CGRectMake(0.0, 0.0, 375.0, ...)` — iPhone 6 width. On iPhone 16 Pro Max (430pt) and iPad, all components render at wrong width. | Remove hardcoded frames; use `UIScreen.mainScreen.bounds.width` or Auto Layout constraints. Multiple files affected: IOSAppBar, IOSProgressBar, IOSAccordion, IOSGrid, IOSDropdown, IOSSearchBar, etc. |
| **LOW** | `iOS/src/iosMain/.../IOSTimePicker.kt:59-61` | `if (preferredDatePickerStyle != null)` is always true — `preferredDatePickerStyle` is a non-optional property in UIKit on all supported iOS versions. The guard is a dead conditional. | Remove the dead null check; set `preferredDatePickerStyle = UIDatePickerStyleWheels` unconditionally. |
| **LOW** | `iOS/src/iosMain/.../IOSDrawer.kt:39` | `UIScreen.mainScreen.bounds.useContents { size.width }` called three times at the top of `render()` — could be cached in a local variable. | Extract to `val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }`. |
| **LOW** | `Android/src/androidMain/.../mappers/input/AdvancedInputMappers.kt` (icon mappers) | `convertToSFSymbol(icon: String?): String` exists in `TextButtonMapper`, `OutlinedButtonMapper`, `FilledButtonMapper`, and `IconButtonMapper` as private static duplicate. Identical function body copy-pasted four times. | Extract to a shared `SFSymbolConverter.kt` object in the `bridge` package. |
| **LOW** | `iOS/src/iosMain/.../mappers/AdvancedComponentMappers.kt:2121-2193` | Comment `// These types should exist in com.augmentalis.avanues.avamagic.ui.core` then defines `RadioComponent`, `RadioGroupComponent`, `SliderComponent`, `ProgressBarComponent`, `AvatarComponent`, `BadgeComponent`, `RatingComponent`, `SearchBarComponent` as data class stubs in `iosMain`. Compiler stub leftover — indicates these types don't exist in commonMain yet or weren't imported correctly. | Move these to `commonMain` UI core, remove the stubs from `iosMain`. |
| **LOW** | All 98 files | **Zero AVID (Accessibility Voice IDentifier) semantics** on any interactive element in any renderer. Buttons, inputs, checkboxes, sliders, chips — none have `Modifier.semantics { contentDescription = "Voice: ..." }` on Android/Compose, and none have the AVID `data-avid` attribute equivalent on iOS. This violates the zero-tolerance AVID mandate. | Add `Modifier.semantics { contentDescription = "Voice: ${component.label ?: component.id}" }` to every interactive Compose element. For iOS UIKit renderers, set `accessibilityIdentifier` using the AVID format. |
| **LOW** | `iOS/src/commonTest/.../IOSRendererTest.kt` and `IOSRendererPhase2Test.kt` | Test assertions are almost entirely `assertNotNull(result)` — the tests verify that rendering doesn't crash but do NOT verify any rendered property values. A test that only checks `result != null` provides minimal confidence and will never catch a regression in rendered output. | Add property assertions: `assertEquals(component.label, result.accessibilityLabel)`, check rendered hierarchy type, check modifier count, etc. |
| **LOW** | `Android/src/androidTest/.../NavigationAndFeedbackComponentsTest.kt` | Android test file exists but was not checked for coverage. Assumed to have similar superficial assertions. | Review separately; apply same depth-of-assertion standard. |
| **LOW** | `iOS/src/iosMain/.../IOSCard.kt:89-94` | Card action buttons created with no `onClick` handler — action buttons on cards are non-functional. Only a string label is passed. | Accept `List<Pair<String, () -> Unit>>` as the actions parameter, or change to `List<CardAction>` with an `onClick` field. |
| **LOW** | `iOS/src/iosMain/.../IOSChip.kt:53-58` | Delete button in chip has no click handler wired — delete button is visible but pressing it does nothing. | Wire `UIAction` or target-action to invoke `component.onDelete?.invoke()`. |

---

## Recommendations

### R1 — Fix Theme System at the Root (MANDATORY RULE #3)
The single most impactful fix is changing `ThemeConverter.WithMaterialTheme()` in `ComposeRenderer.kt` to wrap in `AvanueThemeProvider` instead. This one change fixes every `MaterialTheme.colorScheme.*` violation caused by the inherited M3 theme. After fixing the root, perform a full text search for `MaterialTheme.colorScheme` across all renderer files and replace each occurrence with `AvanueTheme.colors`.

### R2 — Implement All Interactive Callbacks (Critical Rule 1 Violations)
Go through every mapper and extension file and replace every `onClick = { /* ... */ }` placeholder with a real implementation. This is not optional — the component layer exists to provide interactive UI; stubs make it production-unusable. Define required callbacks on component data classes (e.g., `onConfirm`, `onCancel`, `onRangeChanged`, `onFileSelected`) and wire renderers to invoke them.

### R3 — Add AVID to All Interactive Elements
A systematic pass is required across all 98 files. Create a helper composable:
```kotlin
fun Modifier.avid(component: Component, type: String = "BTN"): Modifier =
    this.semantics { contentDescription = "Voice: ${type} ${component.label ?: component.id}" }
```
Apply to every `Button`, `TextButton`, `Chip`, `Checkbox`, `Switch`, `Slider`, `TextField`, `IconButton`.

### R4 — Resolve the Mapper vs Extension Duplication
Audit `ComposeRenderer.render()` dispatch path and identify which system is actually invoked. Remove the unused path entirely. The extension-function `.Render()` pattern appears to be the active path; the separate mapper classes in `mappers/` appear to be legacy code that was not deleted when extensions were introduced.

### R5 — Fix iOS KMP Violation and Cache Bug in OptimizedSwiftUIRenderer
Replace `System.currentTimeMillis()` with `kotlinx.datetime` and change the `renderCache` key from `hashCode()` to a content-stable string key. Both are correctness bugs that affect production iOS builds.

### R6 — Implement Desktop Phase1Mappers Properly
The six stubs in `Desktop/mappers/Phase1Mappers.kt` (Container, Row, Column, Card, Icon, Image) are the only Desktop renderer implementations. Until they are implemented, the Desktop renderer produces broken output for all layout components.

### R7 — Remove Phase2FeedbackMappers KMP Type Definitions
Delete lines 443–532 of `iOS/src/iosMain/kotlin/.../mappers/Phase2FeedbackMappers.kt` and replace all local type references with imports from commonMain. This is a KMP correctness issue that will cause type mismatch errors in cross-platform code.

### R8 — Fix iOS UIKit Component Frames
All iOS UIKit renderers hardcode `375.0` as the screen width. This needs a global replacement using Auto Layout or `UIScreen.mainScreen.bounds` before any iOS device testing can be meaningful.

---

## File Index

All 98 files reviewed:

**Android Core:**
- `Android/src/androidMain/.../ComposeRenderer.kt`
- `Android/src/androidMain/.../ThemeConverter.kt`
- `Android/src/androidMain/.../OpenGLRenderer.kt`
- `Android/src/androidMain/.../ModifierConverter.kt`
- `Android/src/androidMain/.../IconResolver.kt`
- `Android/src/androidMain/.../MagicElementsCompose.kt`

**Android Extensions:**
- `.../extensions/NavigationFeedbackExtensions.kt`
- `.../extensions/FoundationExtensions.kt`
- `.../extensions/InputExtensions.kt`
- `.../extensions/LayoutDisplayExtensions.kt`

**Android Mappers (foundation):**
- `.../mappers/foundation/ButtonMapper.kt`
- `.../mappers/foundation/CardMapper.kt`
- `.../mappers/foundation/CheckboxMapper.kt`
- `.../mappers/foundation/ColumnMapper.kt`
- `.../mappers/foundation/ContainerMapper.kt`
- `.../mappers/foundation/IconMapper.kt`
- `.../mappers/foundation/ImageMapper.kt`
- `.../mappers/foundation/RowMapper.kt`
- `.../mappers/foundation/ScrollViewMapper.kt`
- `.../mappers/foundation/SwitchMapper.kt`
- `.../mappers/foundation/TextFieldMapper.kt`
- `.../mappers/foundation/TextMapper.kt`

**Android Mappers (top-level):**
- `.../mappers/AlertMapper.kt`
- `.../mappers/AppBarMapper.kt`
- `.../mappers/AvatarMapper.kt`
- `.../mappers/BadgeMapper.kt`
- `.../mappers/BottomNavMapper.kt`
- `.../mappers/BreadcrumbMapper.kt`
- `.../mappers/ChipMapper.kt`
- `.../mappers/ConfirmMapper.kt`
- `.../mappers/ContextMenuMapper.kt`
- `.../mappers/DialogMapper.kt`
- `.../mappers/ModalMapper.kt`
- `.../mappers/ProgressBarMapper.kt`
- `.../mappers/SnackbarMapper.kt`
- `.../mappers/ToastMapper.kt`

**Android Mappers (advanced):**
- `.../mappers/navigation/AdvancedNavigationMappers.kt`
- `.../mappers/feedback/AdvancedFeedbackMappers.kt`
- `.../mappers/input/AdvancedInputMappers.kt`

**Android Examples + Tests:**
- `.../examples/AndroidExample.kt`
- `.../androidTest/.../NavigationAndFeedbackComponentsTest.kt`

**Desktop Core:**
- `Desktop/src/desktopMain/.../ComposeDesktopRenderer.kt`
- `Desktop/src/desktopMain/.../mappers/Phase1Mappers.kt`

**iOS Core:**
- `iOS/src/iosMain/.../SwiftUIRenderer.kt`
- `iOS/src/iosMain/.../OptimizedSwiftUIRenderer.kt`
- `iOS/src/iosMain/.../IOSRenderer.kt`

**iOS UIKit Components:**
- `.../IOSAppBar.kt`, `.../IOSAccordion.kt`, `.../IOSAvatar.kt`, `.../IOSBadge.kt`
- `.../IOSBottomNav.kt`, `.../IOSCard.kt`, `.../IOSCheckbox.kt`, `.../IOSChip.kt`
- `.../IOSCircularProgress.kt`, `.../IOSDatePicker.kt`, `.../IOSDialog.kt`
- `.../IOSDivider.kt`, `.../IOSDrawer.kt`, `.../IOSDropdown.kt`, `.../IOSGrid.kt`
- `.../IOSProgressBar.kt`, `.../IOSRadioButton.kt`, `.../IOSSearchBar.kt`
- `.../IOSSkeleton.kt`, `.../IOSSlider.kt`, `.../IOSSnackbar.kt`, `.../IOSSwitch.kt`
- `.../IOSTabs.kt`, `.../IOSTextField.kt`, `.../IOSTimePicker.kt`, `.../IOSToast.kt`
- `.../IOSTooltip.kt`, `.../IOSVideoPlayer.kt`, `.../IOSWebView.kt`

**iOS Bridge:**
- `.../bridge/ThemeConverter.kt`
- `.../bridge/ModifierConverter.kt`
- `.../bridge/SwiftUIModels.kt`

**iOS Mappers:**
- `.../mappers/BasicComponentMappers.kt`
- `.../mappers/Phase2FeedbackMappers.kt`
- `.../mappers/AdvancedComponentMappers.kt`
- `.../mappers/DataComponentMappers.kt`
- `.../mappers/LayoutMappers.kt`

**iOS Examples + Tests:**
- `.../iOSExample.kt`
- `commonTest/.../IOSRendererTest.kt`
- `commonTest/.../IOSRendererPhase2Test.kt`
- `commonTest/.../IOSRendererPhase3Test.kt`

---

## Summary Statistics

| Severity | Count |
|----------|-------|
| CRITICAL | 13 |
| HIGH | 27 |
| MEDIUM | 20 |
| LOW | 11 |
| **Total** | **71** |

**Systemic issues (affect all 98 files):**
1. Zero AVID on any interactive element
2. All Android/Desktop rendering wraps in MaterialTheme instead of AvanueTheme
3. All interactive callbacks in mapper + UIKit component files are no-op stubs

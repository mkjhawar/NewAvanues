# Demo: Cockpit Live Multi-Pane Workspace

## Context

The Cockpit sub-layout currently shows **launcher tiles** in 3 angled windshield panels — clicking a tile does a full-screen navigation. The user wants it to feel like an airplane cockpit where **3 modules run simultaneously**: left panel shows one module (e.g., PDF), center panel shows the focused module (e.g., WebAvanue), right panel shows another (e.g., Video). Voice commands swap/rotate panels. The windshield perspective CSS stays.

## File

`Demo/voice-first-ui-shells.html` (single file, all CSS+HTML+JS in-place)

## State Model

```javascript
const cockpitState = {
    panels: { left: 'pdf', center: 'web', right: 'video' },
    focusedPanel: 'center',  // 'left' | 'center' | 'right'
    maximized: false          // focused panel fills entire cockpit
};
```

## Implementation Steps

### Step 1: Add `cockpitState` + `renderMiniModule()` (JS, after line 804)

- Add state object above
- Add `renderMiniModule(moduleId)` — switch on moduleId, returns condensed HTML for each of 11 modules:

| Module | Mini View |
|--------|-----------|
| web | URL pill + gray viewport rectangle with globe icon |
| note | Heading line + 3 text-placeholder bars |
| pdf | A4 page rectangle with "Page 1/24" centered |
| video | 16:9 dark rect with play triangle + thin progress bar |
| photo | Dark 4:3 rect with camera icon |
| whiteboard | Small SVG with 2 strokes |
| files | 3 compact file rows (icon + name) |
| image | 2x2 thumbnail grid |
| cast | Centered cast icon + "Ready" label |
| annotation | Small SVG with one curved path |
| settings | 3 settings rows with mini toggles |

### Step 2: Add CSS (~100 lines, after existing `.layout-cockpit` block)

**2a: Workspace panel base**
```css
.layout-cockpit.cockpit-workspace .cockpit-panel {
    display: flex; flex-direction: column; overflow: hidden;
    cursor: pointer; position: relative;
}
```

**2b: Mini header + body**
```css
.cockpit-mini-header { /* flex row: icon, title, expand btn, picker btn */ }
.mini-module-body { flex: 1; overflow: hidden; padding: 8px; font-size: 11px; }
```

**2c: Focus glow**
```css
.cockpit-panel.focused { border-color: rgba(21,101,192,0.6); box-shadow: glow; z-index: 2; }
.cockpit-panel:not(.focused) { opacity: 0.85; }
.cockpit-panel:not(.focused):hover { opacity: 1; }
```

**2d: Mini module type-specific styles** — scoped under `.mini-module-body`:
- `.mini-browser-url`, `.mini-browser-viewport`
- `.mini-editor-line`, `.mini-editor-line.heading`
- `.mini-pdf-page`, `.mini-video`, `.mini-progress`
- `.mini-camera`, `.mini-file-row`, `.mini-gallery`, `.mini-gallery-thumb`
- `.mini-cast`, `.mini-settings-row`, `.mini-toggle`

**2e: Maximize mode**
```css
.layout-cockpit.cockpit-workspace.maximized .cockpit-panel { display: none; }
.layout-cockpit.cockpit-workspace.maximized .cockpit-panel.focused {
    display: flex; flex: 1; transform: none; border-radius: var(--radius);
}
.layout-cockpit.cockpit-workspace.maximized::before,
.layout-cockpit.cockpit-workspace.maximized::after { display: none; }
```

**2f: Module picker dropdown**
```css
.panel-picker-dropdown { position: absolute; top: 30px; right: 4px; background: var(--surface2); z-index: 10; display: none; }
.panel-picker-dropdown.visible { display: flex; flex-direction: column; }
.panel-picker-option { /* hover highlight, current indicator */ }
```

### Step 3: Rewrite cockpit branch in `buildClassicLayout()` (lines 823-842)

Replace tile-based panels with live module panels:

```javascript
else if (layout === 'cockpit') {
    label.textContent = '✈️ Cockpit';
    container.className = 'layout-cockpit cockpit-workspace';
    if (cockpitState.maximized) container.classList.add('maximized');

    ['left', 'center', 'right'].forEach(pos => {
        const modId = cockpitState.panels[pos];
        const mod = MODULES[modId];
        const panel = document.createElement('div');
        panel.className = `cockpit-panel ${pos}`;
        panel.dataset.position = pos;
        if (pos === cockpitState.focusedPanel) panel.classList.add('focused');

        panel.innerHTML = `
            <div class="cockpit-mini-header">
                <span class="mini-icon">${mod.icon}</span>
                <span class="mini-title">${mod.title}</span>
                <button class="mini-expand-btn" onclick="event.stopPropagation(); openModule('${modId}')" title="Full screen">⛶</button>
                <button class="mini-picker-btn" onclick="event.stopPropagation(); togglePanelPicker('${pos}')" title="Change module">▾</button>
            </div>
            <div class="mini-module-body">${renderMiniModule(modId)}</div>
            <div class="panel-picker-dropdown" id="picker-${pos}">${buildPickerOptions(pos)}</div>
        `;
        panel.addEventListener('click', () => focusCockpitPanel(pos));
        panel.addEventListener('dblclick', (e) => { e.preventDefault(); toggleMaximize(); });
        container.appendChild(panel);
    });
    updateCockpitActions();
}
```

### Step 4: Add `focusCockpitPanel()` + `updateCockpitActions()` (JS)

- `focusCockpitPanel(pos)`: Moves `.focused` class, calls `updateCockpitActions()`
- `updateCockpitActions()`: Builds hybrid action bar with:
  - **Panel management chips** (left side): Rotate, Swap L↔C, Swap C↔R, Maximize/Restore
  - **Vertical divider**
  - **Focused module's actions** (right side): from `MODULES[focusedModId].actions`
  - **"Layouts" more button**: returns to layout picker

### Step 5: Add module picker functions (JS)

- `buildPickerOptions(pos)` — returns HTML for dropdown listing all 11 modules
- `togglePanelPicker(pos)` — show/hide dropdown, close others
- `assignModuleToPanel(pos, moduleId)` — update `cockpitState.panels[pos]`, re-render
- Document click listener to close pickers on outside click

### Step 6: Add swap/rotate/maximize (JS)

- `swapPanels(posA, posB)` — swap module IDs, re-render
- `rotatePanels()` — left←right, center←left, right←center, re-render
- `toggleMaximize()` — toggle `cockpitState.maximized`, re-render

### Step 7: Integration fixes (JS, ~10 lines)

- `updateClassicActions()`: Add cockpit delegation at top:
  ```javascript
  if (classicLayout === 'cockpit') { updateCockpitActions(); return; }
  ```
- Keyboard handler: Add Escape for cockpit maximize:
  ```javascript
  if (e.key === 'Escape' && classicLayout === 'cockpit' && cockpitState.maximized) toggleMaximize();
  ```

### Step 8: AvanueViews — Live module previews in cards (HTML)

Currently cards show text summaries (`card-body` with description text). Replace with live mini module content.

**Each `.stream-card` gets a `.card-preview` div** after the header, containing `renderMiniModule(modId)`:

```html
<div class="stream-card" data-mod="note">
    <div class="card-head">...</div>
    <div class="card-preview">
        <!-- renderMiniModule('note') output: heading line + text bars -->
    </div>
    <span class="card-cta">Resume Editing</span>
</div>
```

**CSS**: `.card-preview { height: 100px; overflow: hidden; border-radius: 6px; margin: 8px 0; background: rgba(0,0,0,0.2); }`

**JS**: Rewrite `setAVLayout()` or the AV HTML section to inject mini module content into each card. The `renderMiniModule()` function from Step 1 is reused — same function, different context.

### Step 9: Spatial — Islands render live mini modules (HTML)

Currently islands show just an emoji icon + label. Expand islands to include mini module content.

**Each `.island` gets a `.island-preview` body** below the icon:

```html
<div class="island depth-near" data-mod="web">
    <div class="i-icon">🌐</div>
    <div class="i-label">WebAvanue</div>
    <div class="island-preview">
        <!-- renderMiniModule('web') output: URL bar + viewport rect -->
    </div>
</div>
```

**CSS**: Islands expand from ~80px to ~160px width/height. `.island-preview { margin-top: 6px; font-size: 9px; overflow: hidden; max-height: 80px; }` Near-depth islands show more detail, far-depth islands show less (opacity + scale already handle this).

**JS**: Update the Spatial HTML (lines 577-587) to include `renderMiniModule()` inside each island. Also update `resetSpatialPositions()` to account for larger islands.

### Step 10: Lens — Search results show live mini-preview (HTML)

Currently search results are text-only rows. Add a preview pane when a result is focused/hovered.

**CSS**: `.lens-result:hover .result-preview, .lens-result:focus .result-preview { display: block; }` — shows mini module preview on hover.

**JS**: In `renderLensResults()`, append a hidden `.result-preview` div with `renderMiniModule(item.id)` to each Module-type result. Non-module results (Commands, Settings) stay text-only.

## What stays unchanged

- Existing windshield CSS transforms (`.cockpit-panel.left/center/right` with `rotateY`)
- Other Classic sub-layouts (Grid, Carousel, Freeform) — launcher tiles, no live content
- Full-screen module screens (lines 594-756) — still used via `openModule()` from expand button
- `openModule()` and `goBack()` — cockpit state survives going full-screen and back

## Verification

### HTML Demo
1. Preview server on port 8090, reload page
2. **Classic → Cockpit**: 3 panels show live mini module content (not tiles)
3. Default: left=PDF page, center=WebAvanue browser, right=Video player
4. Click left panel → focus moves (glow shifts), action bar shows PDF actions
5. Action bar: Rotate chip → modules cycle through panels
6. Action bar: Swap L↔C → PDF moves to center, Web moves to left
7. Picker dropdown (▾ button) → change right panel from Video to NoteAvanue
8. Maximize → focused panel fills cockpit, Restore returns to 3-panel
9. Expand button (⛶) → opens module full-screen, Back returns to cockpit
10. Switch to Grid/Carousel/Freeform → still work, switch back to Cockpit → state preserved
11. **AvanueViews**: Cards show live mini module previews (URL bar, text bars, PDF page, etc.)
12. **Spatial**: Islands expanded with mini module content inside, depth layers preserved
13. **Lens**: Hovering a Module search result shows mini-preview
14. Zero console errors
15. Commit and push

### KMP Build
1. `./gradlew :Modules:Cockpit:compileKotlinMetadata` — commonMain compiles
2. `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid` — Android compiles
3. Commit KMP changes separately from demo

---

## Part 2: KMP Replication — Adaptive Hybrid WorkspaceLayout

### Context

Replicate the demo's cockpit live workspace in the real KMP Cockpit module. Uses **adaptive hybrid rendering**: `graphicsLayer` 3D transforms on phones/tablets/desktop, flat layout on GLASS_MICRO where 3D wastes pixels.

### Existing Infrastructure to Reuse

| Component | Path | How Used |
|-----------|------|----------|
| `LayoutMode` enum | `model/LayoutMode.kt` | Add `WORKSPACE` value |
| `LayoutEngine` | `ui/LayoutEngine.kt` | Add `WORKSPACE -> WorkspaceLayout(...)` case |
| `FrameWindow` | `ui/FrameWindow.kt` | Wrap each pane's frame with existing chrome |
| `frameContent` slot | All layouts | Same `@Composable (CockpitFrame) -> Unit` pattern |
| `DisplayProfile` | `AvanueTheme.displayProfile` | Branch 3D vs flat |
| `ContextualActionProvider` | `model/ContextualActionProvider.kt` | Per-pane actions for focused panel |
| `ContentAccent` | `model/ContentAccent.kt` | Semantic border color per pane |
| `CockpitFrame` | `model/CockpitFrame.kt` | Each pane hosts a CockpitFrame |
| `PanelRole` enum | Inside CockpitFrame | Map left→AUXILIARY, center→CONTENT, right→AUXILIARY |
| `LayoutModeResolver` | Handles availability per DisplayProfile | Add WORKSPACE rules |
| `CommandBar.layoutModeLabel()` | `ui/CommandBar.kt` | Add `WORKSPACE -> "Workspace"` |

### New Files

**1. `Modules/Cockpit/src/commonMain/.../ui/WorkspaceLayout.kt`** (NEW, ~200 lines)

```kotlin
@Composable
fun WorkspaceLayout(
    frames: List<CockpitFrame>,   // Expects 3 frames (left, center, right)
    selectedFrameId: String?,
    onFrameSelected: (String) -> Unit,
    onFrameClose: (String) -> Unit,
    onFrameMaximize: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayProfile = AvanueTheme.displayProfile
    val density = LocalDensity.current.density
    val use3D = displayProfile != DisplayProfile.GLASS_MICRO

    Row(modifier.fillMaxSize()) {
        // Left panel — angled on capable displays
        WorkspacePanel(
            frame = frames.getOrNull(0),
            isFocused = frames.getOrNull(0)?.id == selectedFrameId,
            weight = 0.65f,
            graphicsLayerBlock = if (use3D) {{
                rotationY = 16f
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(1f, 0.5f)
            }} else null,
            borderRadius = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            onSelect = { onFrameSelected(it) },
            frameContent = frameContent
        )

        // Center panel — no rotation, larger, glow border
        WorkspacePanel(
            frame = frames.getOrNull(1),
            isFocused = frames.getOrNull(1)?.id == selectedFrameId,
            weight = 1.7f,
            graphicsLayerBlock = null,
            borderRadius = RoundedCornerShape(0.dp),
            onSelect = { onFrameSelected(it) },
            frameContent = frameContent,
            isCenterPanel = true
        )

        // Right panel — angled opposite direction
        WorkspacePanel(
            frame = frames.getOrNull(2),
            isFocused = frames.getOrNull(2)?.id == selectedFrameId,
            weight = 0.65f,
            graphicsLayerBlock = if (use3D) {{
                rotationY = -16f
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(0f, 0.5f)
            }} else null,
            borderRadius = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
            onSelect = { onFrameSelected(it) },
            frameContent = frameContent
        )
    }
}

@Composable
private fun RowScope.WorkspacePanel(
    frame: CockpitFrame?,
    isFocused: Boolean,
    weight: Float,
    graphicsLayerBlock: (GraphicsLayerScope.() -> Unit)?,
    borderRadius: Shape,
    onSelect: (String) -> Unit,
    frameContent: @Composable (CockpitFrame) -> Unit,
    isCenterPanel: Boolean = false
) {
    val borderColor = if (isFocused) AvanueTheme.colors.primary.copy(0.6f)
                      else AvanueTheme.glass.border
    val alpha = if (isFocused) 1f else 0.85f

    AvanueCard(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .then(graphicsLayerBlock?.let { Modifier.graphicsLayer(it) } ?: Modifier)
            .alpha(alpha)
            .clickable { frame?.let { onSelect(it.id) } },
        shape = borderRadius,
        border = BorderStroke(1.dp, borderColor)
    ) {
        if (frame != null) {
            // Mini header
            WorkspacePanelHeader(frame, isCenterPanel)
            // Content — real module composable via frameContent slot
            Box(Modifier.weight(1f).clip(RoundedCornerShape(4.dp))) {
                frameContent(frame)
            }
        } else {
            EmptyPanelPlaceholder()
        }
    }
}
```

### Modified Files

**2. `model/LayoutMode.kt`** — Add WORKSPACE

```kotlin
enum class LayoutMode {
    DASHBOARD, FREEFORM, GRID, SPLIT_LEFT, SPLIT_RIGHT, COCKPIT,
    T_PANEL, MOSAIC, FULLSCREEN, WORKFLOW, ROW, CAROUSEL,
    SPATIAL_DICE, GALLERY, TRIPTYCH, WORKSPACE;  // <-- NEW

    companion object {
        val FRAME_LAYOUTS = setOf(/* existing */ WORKSPACE)
    }
}
```

**3. `ui/LayoutEngine.kt`** — Add dispatch case (~5 lines)

```kotlin
LayoutMode.WORKSPACE -> WorkspaceLayout(
    frames = visibleFrames.take(3),
    selectedFrameId = selectedFrameId,
    onFrameSelected = onFrameSelected,
    onFrameClose = onFrameClose,
    onFrameMaximize = onFrameMaximize,
    frameContent = frameContent
)
```

**4. `ui/CommandBar.kt`** — Add label (~1 line)

```kotlin
LayoutMode.WORKSPACE -> "Workspace"
```

**5. `model/LayoutModeResolver.kt`** — Add rules (~5 lines)

```kotlin
LayoutMode.WORKSPACE -> when (profile) {
    DisplayProfile.GLASS_MICRO -> false  // Too small for 3 panes
    else -> true
}
// maxFrames: WORKSPACE -> 3 (always 3 panes)
```

**6. `model/ArrangementIntent.kt`** — Optional: map COMPARE intent to WORKSPACE when 3 frames

```kotlin
COMPARE -> when {
    frameCount == 3 -> LayoutMode.WORKSPACE
    frameCount == 2 -> LayoutMode.SPLIT_LEFT
    else -> LayoutMode.GRID
}
```

### KMP: 3D Perspective on Existing LayoutEngine Modes

Not all layouts benefit from 3D. Here's the matrix:

| LayoutMode | 3D Treatment | How |
|-----------|-------------|-----|
| **WORKSPACE** (new) | Windshield angles | Left `rotationY=16`, Right `rotationY=-16` |
| **TRIPTYCH** | Book-spine perspective | Left `rotationY=8`, Right `rotationY=-8` (subtle open-book feel) |
| **CAROUSEL** | Already has 3D | Existing swipe-through with perspective — just add `graphicsLayer` |
| **COCKPIT** (FlightDeck) | Instrument panel tilt | Top strip `rotationX=-5` (angled away), main panels flat |
| **MOSAIC** | Depth layering | Primary `translationZ=4.dp` (elevated), secondaries flat |
| SPLIT_LEFT/RIGHT | No 3D | Flat side-by-side is correct |
| T_PANEL | No 3D | Flat top/bottom is correct |
| GRID | No 3D | Grid doesn't benefit |
| ROW | No 3D | Equal columns stay flat |
| FREEFORM | No 3D | Draggable windows stay flat |
| Others | No 3D | n/a |

All 3D treatments use the same adaptive hybrid: `graphicsLayer` on capable displays, skip on GLASS_MICRO.

### KMP: Live Content in Shell Home Screens

| Shell | Current Home Screen | Live Content Strategy |
|-------|-------------------|----------------------|
| CLASSIC/Dashboard | Module tiles grid | Keep as-is (Cockpit sub-layout handles live panels) |
| AVANUE_VIEWS | Priority card stream | Cards host `frameContent` slot for pinned modules' mini-preview |
| LENS | Search + quick chips | Search results show mini-preview on selection |
| CANVAS/Spatial | Hex islands | Islands host `frameContent` slot — zoom in = interactive, zoom out = thumbnail |

Implementation: Add optional `frameContent: @Composable (CockpitFrame) -> Unit` param to each shell layout composable. When frames exist (pinned/recent), render mini-preview. When no frames, fall back to current launcher behavior.

### Summary: Files to Touch

| # | File | Action | Lines |
|---|------|--------|-------|
| 1 | `Demo/voice-first-ui-shells.html` | MODIFY (Part 1 — all 4 shells + cockpit workspace) | ~350 |
| 2 | `Modules/Cockpit/.../ui/WorkspaceLayout.kt` | CREATE | ~200 |
| 3 | `Modules/Cockpit/.../model/LayoutMode.kt` | MODIFY (add WORKSPACE) | ~3 |
| 4 | `Modules/Cockpit/.../ui/LayoutEngine.kt` | MODIFY (add WORKSPACE case + graphicsLayer on TRIPTYCH/MOSAIC/COCKPIT) | ~30 |
| 5 | `Modules/Cockpit/.../ui/CommandBar.kt` | MODIFY (add label) | ~1 |
| 6 | `Modules/Cockpit/.../model/LayoutModeResolver.kt` | MODIFY (add rules) | ~5 |
| 7 | `Modules/Cockpit/.../model/ArrangementIntent.kt` | MODIFY (COMPARE mapping) | ~3 |
| 8 | `Modules/Cockpit/.../ui/AvanueViewsStreamLayout.kt` | MODIFY (add optional frameContent param for mini-previews) | ~20 |
| 9 | `Modules/Cockpit/.../ui/ZenCanvasLayout.kt` | MODIFY (islands render frameContent when frames exist) | ~20 |
| 10 | `Modules/Cockpit/.../ui/LensLayout.kt` | MODIFY (search results mini-preview) | ~15 |
| 11 | `Modules/Cockpit/.../ui/TriptychLayout.kt` | MODIFY (add graphicsLayer book-spine) | ~10 |
| 12 | `Modules/Cockpit/.../ui/CarouselLayout.kt` | MODIFY (add graphicsLayer if not present) | ~5 |

### 3rd Party App Strategy (Triple Coverage)

The WorkspaceLayout hosts ANY `FrameContent` type via the `frameContent` slot — including 3rd party apps through 3 paths:

| Path | FrameContent Type | When | How |
|------|------------------|------|-----|
| **WebView proxy** | `FrameContent.Web(url)` | App has web/PWA version (Maps, Slack, Notion, Gmail...) | WebAvanue panel loads web URL, fully interactive + voice-commandable via DOM scraping |
| **System split-screen** | `FrameContent.ExternalApp(pkg, activity)` | Native-only app, Android 12L+ tablet/foldable | `FLAG_ACTIVITY_LAUNCH_ADJACENT` via Jetpack WindowManager ActivityEmbedding |
| **ScreenCast mirror** | `FrameContent.ScreenCast(deviceId)` | Any running app, read-only live view | MJPEG stream from RemoteCast module rendered in panel |

All 3 `FrameContent` types already exist in `model/FrameContent.kt`. No new content types needed — WorkspaceLayout just hosts them.

### GPU Performance Note

`graphicsLayer { rotationY = 16f }` is **essentially free**:
- Renders composable to a hardware RenderNode (texture), GPU applies a 3×3 matrix — same mechanism as all Android animations
- 3 panels = ~0.5ms total per frame on any modern device
- Memory: ~4MB total for 3 offscreen layers (trivial)
- Static rotation = layer composed once, reused — zero ongoing GPU cost
- The real performance factor is content INSIDE panels (WebView = heavy, Note = light), not the transform
- On GLASS_MICRO: transforms skipped entirely (flat layout), saving GPU for content rendering

### KMP Verification

1. `./gradlew :Modules:Cockpit:compileKotlinMetadata` — commonMain compiles
2. `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid` — Android compiles
3. Preview server: reload HTML demo, verify cockpit workspace
4. Commit: demo file + KMP changes in one commit

## Session 1: AvanuesNewUI App + Settings (This Session)

### Chunk 1: Create AvanuesNewUI Android App

**Strategy**: Copy `Apps/avanues/` → `Apps/Android/AvanuesNewUI/` with new applicationId and namespace. Override the default shell mode to LENS (instead of CLASSIC). All 47 source files carry over with minimal changes.

#### 1A: Scaffold the new app

| Action | Details |
|--------|---------|
| Copy directory | `Apps/avanues/` → `Apps/Android/AvanuesNewUI/` |
| Rename applicationId | `com.augmentalis.avanues` → `com.augmentalis.avanuesnewui` |
| Rename namespace | Keep `com.augmentalis.voiceavanue` (same source packages, just different app ID) |
| Update `settings.gradle.kts` | Add `include(":Apps:Android:AvanuesNewUI")` |
| Update `build.gradle.kts` | Change applicationId, versionName suffix, app label |

**Why keep the namespace?** The source code references `com.augmentalis.voiceavanue` throughout (47 files, Hilt components, manifest). Changing the namespace would require renaming every file. Instead, just change the `applicationId` (which is the Play Store identity) and the app label. This is the standard Android pattern for build variants.

#### 1B: Wire default shell mode

| File | Change |
|------|--------|
| `AvanuesNewUI/.../MainActivity.kt` | Default `AvanueMode.HUB` → `AvanueMode.COCKPIT` (launch into Cockpit directly) |
| `AvanuesNewUI/.../ui/cockpit/CockpitEntryViewModel.kt` | Set initial shellMode to `LENS` instead of relying on DataStore (first-launch default) |
| `AvanuesNewUI/.../data/AvanuesSettingsRepository.kt` | Change default `shellMode` to `"LENS"` |

#### 1C: Update AndroidManifest.xml

| Change | Details |
|--------|---------|
| App label | `"AvanuesNewUI"` (distinct from "Avanues") |
| Launcher icon | Reuse existing icons (can customize later) |
| Accessibility service | Same `VoiceAvanueAccessibilityService` (same voice capabilities) |
| Activity aliases | Keep all 10 (VoiceAvanue, WebAvanue, PDF, etc.) |

### Chunk 2: Settings — User + Developer

#### 2A: User Settings — CockpitSettingsProvider (NEW)

Create a new settings provider for Cockpit shell selection.

**Files to modify (in BOTH apps — original Avanues AND AvanuesNewUI):**

| File | Change |
|------|--------|
| `Modules/Foundation/src/commonMain/.../settings/SettingsKeys.kt` | Add 8 Cockpit keys: `SHELL_MODE`, `DEFAULT_ARRANGEMENT`, `COCKPIT_MAX_FRAMES`, `COCKPIT_AUTOSAVE_INTERVAL`, `COCKPIT_BACKGROUND_SCENE`, `COCKPIT_SPATIAL_ENABLED`, `COCKPIT_SPATIAL_SENSITIVITY`, `COCKPIT_CANVAS_ZOOM_PERSIST` |
| `Modules/Foundation/src/commonMain/.../settings/models/AvanuesSettings.kt` | Add 8 fields with defaults: `shellMode = "LENS"`, `defaultArrangement = "FOCUS"`, `cockpitMaxFrames = 6`, `cockpitAutosaveInterval = "1m"`, `cockpitBackgroundScene = "GRADIENT"`, `cockpitSpatialEnabled = false`, `cockpitSpatialSensitivity = "NORMAL"`, `cockpitCanvasZoomPersist = true` |

**Files to create (in both apps):**

| File | Purpose |
|------|---------|
| `ui/settings/providers/CockpitSettingsProvider.kt` | Shell mode dropdown (CLASSIC / AVANUE_VIEWS / LENS / CANVAS), sortOrder=350 |

**Files to modify (in both apps):**

| File | Change |
|------|--------|
| `data/AvanuesSettingsRepository.kt` | Add `KEY_SHELL_MODE` DataStore key, read/write in `readFromPreferences()`/`writeToPreferences()`, add `suspend fun updateShellMode()` |
| `di/SettingsModule.kt` | Add `@Provides @IntoSet fun provideCockpitSettings(repo): ComposableSettingsProvider` |

**CockpitSettingsProvider pattern** (follows VoiceControlSettingsProvider):
- `moduleId = "cockpit"`, `displayName = "Cockpit"`, `iconName = "Dashboard"`, `sortOrder = 350`
- Three sections:

**Section 1: "Home Screen" (`shell`)**
| Setting | Type | Values | Default | Key |
|---------|------|--------|---------|-----|
| Shell Mode | Dropdown | Classic / AvanueViews / Lens / Canvas | LENS | `shell_mode` |
| Default Arrangement | Dropdown | Focus / Compare / Overview / Present | FOCUS | `default_arrangement` |

**Section 2: "Frames" (`frames`)**
| Setting | Type | Values | Default | Key |
|---------|------|--------|---------|-----|
| Max Frames | Slider | 1–12 | 6 | `cockpit_max_frames` |
| Auto-Save Interval | Dropdown | Off / 30s / 1m / 5m | 1m | `cockpit_autosave_interval` |
| Background Scene | Dropdown | Gradient / Starfield / Minimal / None | Gradient | `cockpit_background_scene` |

**Section 3: "Spatial" (`spatial`)**
| Setting | Type | Values | Default | Key |
|---------|------|--------|---------|-----|
| Head Tracking | Switch | on/off | off | `cockpit_spatial_enabled` |
| Sensitivity | Slider | Low / Normal / High | Normal | `cockpit_spatial_sensitivity` |
| Canvas Zoom Persistence | Switch | on/off | on | `cockpit_canvas_zoom_persist` |

All 8 settings stored via DataStore in `AvanuesSettingsRepository`. Each needs a key in `SettingsKeys.kt` and a field in `AvanuesSettings.kt`.

#### 2B: Per-Module Settings Providers

Create settings providers for all content modules. Each follows the CockpitSettingsProvider pattern (ComposableSettingsProvider, Hilt @IntoSet).

**Foundation keys** — add to `SettingsKeys.kt` + `AvanuesSettings.kt` for all modules below.

**1. WebAvanue (sortOrder=400, ALREADY EXISTS — extend)**

Existing `WebAvanueSettingsProvider` already has basic settings. Extend with BrowserSettingsModel fields:

| Section | Settings | Source |
|---------|----------|--------|
| Search | Default engine (Google/DuckDuckGo/Bing/Brave), search suggestions on/off | `BrowserSettingsModel.kt` |
| Privacy | JavaScript on/off, cookies (accept all/block third-party/block all), do-not-track | `BrowserSettingsModel.kt` |
| Display | Text size (50-200%), force dark mode, desktop mode default | `BrowserSettingsModel.kt` |
| Downloads | Download path, ask before download | `BrowserSettingsModel.kt` |

*65+ params already modeled in `Modules/WebAvanue/.../BrowserSettingsModel.kt` with its own repository — bridge to DataStore, expose top 12 in UnifiedSettings.*

**2. PDFAvanue (sortOrder=450, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Viewer | Default view mode (Single/Continuous/Thumbnail) | `pdf_view_mode` | Continuous |
| Viewer | Night mode auto-enable | `pdf_night_mode` | off |
| Viewer | Default zoom (Fit Width/Fit Page/100%) | `pdf_default_zoom` | Fit Width |
| Viewer | Remember last page | `pdf_remember_page` | on |

**3. PhotoAvanue (sortOrder=500, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Camera | Default camera (Front/Back) | `camera_default_lens` | Back |
| Camera | Photo resolution (Auto/Max/Balanced) | `camera_resolution` | Auto |
| Camera | Save location (DCIM/Custom) | `camera_save_path` | DCIM |
| Pro Mode | Show pro controls by default | `camera_pro_default` | off |
| Pro Mode | Default stabilization (Off/Standard/Cinematic) | `camera_stabilization` | Standard |
| Pro Mode | RAW capture | `camera_raw_enabled` | off |

**4. VideoAvanue (sortOrder=550, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Playback | Default speed | `video_default_speed` | 1.0x |
| Playback | Resume playback | `video_resume` | on |
| Playback | Default repeat mode (Off/One/All) | `video_repeat_mode` | Off |
| Audio | Default volume | `video_default_volume` | 100% |
| Audio | Mute by default | `video_mute_default` | off |

**5. NoteAvanue (sortOrder=600, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Editor | Default font size (Small/Medium/Large) | `note_font_size` | Medium |
| Editor | Auto-save interval (Off/5s/15s/30s) | `note_autosave` | 15s |
| Editor | Spell check | `note_spellcheck` | on |
| Format | Default format (Markdown/Rich Text) | `note_default_format` | Markdown |

**6. FileAvanue (sortOrder=650, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Browser | Default sort (Name/Date/Size/Type) | `file_sort_mode` | Name |
| Browser | Default view (List/Grid) | `file_view_mode` | List |
| Browser | Show hidden files | `file_show_hidden` | off |
| Browser | Default provider (Local/Downloads) | `file_default_provider` | Local |

**7. RemoteCast (sortOrder=700, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Quality | JPEG quality (30-100) | `cast_jpeg_quality` | 60 |
| Quality | Target FPS (10/15/24/30) | `cast_target_fps` | 15 |
| Quality | Resolution scale (50%/75%/100%) | `cast_resolution_scale` | 75% |
| Network | Port | `cast_port` | 54321 |
| Network | Auto-connect on launch | `cast_auto_connect` | off |

**8. AnnotationAvanue (sortOrder=750, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Drawing | Default tool (Pen/Highlighter/Eraser) | `annotation_default_tool` | Pen |
| Drawing | Default color | `annotation_default_color` | #FFFFFF |
| Drawing | Default stroke width (1-20) | `annotation_stroke_width` | 4 |
| Drawing | Smoothing tension (0.0-1.0) | `annotation_tension` | 0.3 |

**9. ImageAvanue (sortOrder=800, NEW)**

| Section | Settings | Key | Default |
|---------|----------|-----|---------|
| Viewer | Default zoom (Fit/Fill/100%) | `image_default_zoom` | Fit |
| Viewer | Show EXIF info | `image_show_exif` | off |

**Implementation pattern** (same for all 8 new providers):
1. Add keys to `SettingsKeys.kt` (Foundation commonMain)
2. Add fields to `AvanuesSettings.kt` (Foundation commonMain)
3. Add DataStore read/write in `AvanuesSettingsRepository.kt` (app-level)
4. Create `{Module}SettingsProvider.kt` in `ui/settings/providers/` (app-level)
5. Register in `SettingsModule.kt` with `@Provides @IntoSet`

**Total new SettingsKeys**: ~40 keys across all modules
**Total new providers**: 8 (+ extend WebAvanue)

#### 2C: Developer Settings — Shell Debug Flags

| File | Change |
|------|--------|
| `data/DeveloperPreferences.kt` | Add keys: `FORCE_SHELL_MODE` (override user choice), `SHOW_SHELL_DEBUG_OVERLAY` (show shell name + frame count badge) |
| `ui/developer/DeveloperSettingsScreen.kt` | Add "Cockpit" section with: force shell mode dropdown + debug overlay toggle |

#### 2D: Wire DataStore → CockpitViewModel

| File | Change |
|------|--------|
| `Modules/Cockpit/src/androidMain/.../ui/CockpitScreen.kt` | Observe `AvanuesSettingsRepository.settings.shellMode` → `viewModel.setShellMode(SimplifiedShellMode.fromString(it))` |

- Use `LaunchedEffect` to collect settings flow and push to ViewModel
- Developer override: if `FORCE_SHELL_MODE` is set, use that instead of user preference
- CockpitScreen already collects `viewModel.shellMode` and passes it to `CockpitScreenState` (done in Phase 1)

### Chunk 3: Build Verification

| Target | Command |
|--------|---------|
| Original Avanues | `./gradlew :Apps:avanues:compileDebugKotlinAndroid` |
| AvanuesNewUI | `./gradlew :Apps:Android:AvanuesNewUI:compileDebugKotlinAndroid` |
| Cockpit module | `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid` |
| Foundation module | `./gradlew :Modules:Foundation:compileKotlinMetadata` |

---

## Session 2: Glass Optimizations + iOS + Mac

### Chunk 4: Glass Optimizations

**Already done (70%):** isGlass branching in all 3 shells, GlassFrameChrome, density scaling, IMU pipeline, voice commands (next card / previous card / canvas zoom).

#### 4A: AvanueViews Pagination (GLASS_MICRO)

| File | Change |
|------|--------|
| `Modules/Cockpit/src/commonMain/.../ui/AvanueViewsStreamLayout.kt` | When `displayProfile.isPaginated`: replace `LazyColumn` with `HorizontalPager`, add dot indicators, expose `pagerState` for voice navigation |

- Single card per page with swipe gesture
- Dot indicators (4dp circles) at bottom
- `AnimatedVisibility` for page transitions

#### 4B: Canvas Head-Tracking Integration

| File | Change |
|------|--------|
| `Modules/Cockpit/src/commonMain/.../ui/ZenCanvasLayout.kt` | Accept `spatialViewportOffset: State<Offset>?` param; add to canvas panOffset when non-null |
| `Modules/Cockpit/src/commonMain/.../ui/CockpitScreenContent.kt` | Pass `spatialController?.viewportOffset` to ZenCanvasLayout when shellMode == CANVAS |

- Head turn → canvas pan (additive to touch)
- GLASS_LOW sensitivity preset (45° per screen, 8° deadzone)

#### 4C: SEE_THROUGH colorsXR

| File | Change |
|------|--------|
| All 3 shell layouts | When `GlassDisplayMode.SEE_THROUGH`, use `palette.colorsXR` for boosted luminance on additive waveguide displays |

- colorsXR tokens already exist in palette system
- Need to pass `glassDisplayMode` through to shell composables (add param to CockpitScreenState or read from AvanueTheme)

#### 4D: Voice Navigation Executor Wiring

| File | Change |
|------|--------|
| `Modules/Cockpit/src/commonMain/.../viewmodel/CockpitViewModel.kt` | Add `MutableSharedFlow<ShellNavigationAction>` for shell-specific commands |
| `Modules/Cockpit/src/commonMain/.../ui/CockpitScreenContent.kt` | Collect `shellNavigationAction` flow, dispatch to active shell |
| `Modules/Cockpit/src/androidMain/.../ui/CockpitScreen.kt` | Wire STREAM_NEXT_CARD/PREVIOUS_CARD → ViewModel.emitShellNavigation(), same for CANVAS_ZOOM |

- `ShellNavigationAction` enum: NEXT_CARD, PREVIOUS_CARD, ZOOM_IN, ZOOM_OUT
- Each shell composable receives a `Flow<ShellNavigationAction>` and handles relevant actions
- AvanueViews: animate pager, Canvas: adjust zoom level

### Chunk 5: iOS Platform

#### 5A: iOS Cockpit Entry Point

| File | Change |
|------|--------|
| `Modules/Cockpit/src/iosMain/kotlin/.../IosCockpitScreen.kt` (NEW) | Thin wrapper creating `CockpitScreenContent` with shell routing — reuses all commonMain composables via Compose Multiplatform |

- Mirror Android's `CockpitScreen.kt` pattern but use iOS-specific lifecycle
- `IosCockpitControllerHolder.onCockpitCommand` already wired for voice dispatch
- Settings persistence via Foundation's `UserDefaultsSettingsStore` (already implemented for iOS)

#### 5B: iOS Settings Bridge

| File | Change |
|------|--------|
| `Modules/Foundation/src/iosMain/kotlin/.../UserDefaultsSettingsStore.kt` | Verify `shellMode` key is read/written (should work automatically since AvanuesSettings model is in commonMain) |

### Chunk 6: Mac/Desktop Platform

#### 6A: Keyboard Shortcuts

| File | Change |
|------|--------|
| `Modules/Cockpit/src/desktopMain/kotlin/.../DesktopKeyboardHandler.kt` (NEW) | Cmd+1 = Classic, Cmd+2 = AvanueViews, Cmd+3 = Lens, Cmd+4 = Canvas, Cmd+K = Lens activation (focus search bar) |

- Desktop already renders all shells via commonMain Compose
- Settings via Foundation's `JavaPreferencesSettingsStore` (already implemented)

### Chunk 7: Developer Documentation

Create two documentation deliverables that teach developers how to build new apps using the Simplified Voice-First UI system.

#### 7A: Developer Manual Chapter 113 — Building Apps with Voice-First UI Shells

**File**: `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter113-BuildingAppsWithVoiceFirstUIShells.md`

Content outline:
1. **Introduction** — What the Voice-First UI system provides, design principles recap
2. **Quick Start** — Minimal steps to create a new app using shells (copy AvanuesNewUI template)
3. **Shell Selection** — How to set default shell, how to let users choose, DataStore wiring
4. **ArrangementIntent API** — How to use intents instead of raw LayoutModes, IntentResolver usage
5. **ContextualActionProvider** — How to register actions per content type, top actions vs full list
6. **Adding a New Shell** — Step-by-step: create SimplifiedShellMode entry, layout composable, voice command, CockpitScreenContent routing
7. **Adding Content Types** — How to add a new FrameContent variant, wire ContentRenderer, register actions
8. **Voice Command Integration** — Adding new CommandActionTypes, handler phrases (Android + iOS), executor dispatch
9. **Settings Integration** — Creating a new SettingsProvider, Foundation keys, DataStore persistence, Hilt registration
10. **Glass Optimization Checklist** — DisplayProfile branching, density scaling, pagination, colorsXR, voice-only mode
11. **Template Code** — Complete minimal app scaffold (build.gradle.kts, AndroidManifest.xml, MainActivity.kt, SettingsModule.kt)
12. **Platform Matrix** — What works where (Android full, iOS via Compose MP, Desktop via Compose MP, Web deferred)

#### 7B: Specification Document

**File**: `docs/specs/CockpitUI-Spec-VoiceFirstUIShellSystem-260225-V1.md`

Content outline:
1. **System Overview** — Architecture diagram (text), component relationships
2. **Shell Modes** — Enum values, display labels, descriptions, default selection logic
3. **ArrangementIntent** — Intent → LayoutMode resolution table, display profile rules, frame count rules
4. **ContextualActionProvider** — Action registration API, content type → action mappings (all 17 types)
5. **Voice Command Matrix** — Complete table: phrase → CommandActionType → handler → executor → effect
6. **Settings Schema** — All keys, types, defaults, validation rules (Cockpit + all 9 modules)
7. **Display Profiles** — Breakpoints, density scaling factors, responsive layout rules per shell
8. **Glass-Specific Behaviors** — Pagination rules, colorsXR activation, voice-only mode, HUD layout
9. **Platform Support Matrix** — Feature × platform grid (Android/iOS/Mac/Web)
10. **Data Flow Diagrams** — Settings → DataStore → ViewModel → UI, Voice → Handler → Executor → ViewModel → UI
11. **API Reference** — Key public interfaces: IntentResolver, ContextualActionProvider, SimplifiedShellMode, ShellNavigationAction

---

## Files Summary

### Session 1 (This Session) — ~22 files

| # | File | Action | Chunk |
|---|------|--------|-------|
| 1 | `Apps/Android/AvanuesNewUI/` (entire directory) | CREATE (copy from Apps/avanues/) | 1A |
| 2 | `settings.gradle.kts` | MODIFY (add include) | 1A |
| 3 | `AvanuesNewUI/build.gradle.kts` | MODIFY (applicationId, label) | 1A |
| 4 | `AvanuesNewUI/src/main/AndroidManifest.xml` | MODIFY (app label) | 1C |
| 5 | `Modules/Foundation/.../SettingsKeys.kt` | MODIFY (add ~48 keys: 8 Cockpit + ~40 module) | 2A+2B |
| 6 | `Modules/Foundation/.../AvanuesSettings.kt` | MODIFY (add ~48 fields) | 2A+2B |
| 7 | `Apps/avanues/.../AvanuesSettingsRepository.kt` | MODIFY (add all new key persistence) | 2A+2B |
| 8 | `Apps/avanues/.../providers/CockpitSettingsProvider.kt` | CREATE | 2A |
| 9 | `Apps/avanues/.../providers/PDFAvanueSettingsProvider.kt` | CREATE | 2B |
| 10 | `Apps/avanues/.../providers/PhotoAvanueSettingsProvider.kt` | CREATE | 2B |
| 11 | `Apps/avanues/.../providers/VideoAvanueSettingsProvider.kt` | CREATE | 2B |
| 12 | `Apps/avanues/.../providers/NoteAvanueSettingsProvider.kt` | CREATE | 2B |
| 13 | `Apps/avanues/.../providers/FileAvanueSettingsProvider.kt` | CREATE | 2B |
| 14 | `Apps/avanues/.../providers/RemoteCastSettingsProvider.kt` | CREATE | 2B |
| 15 | `Apps/avanues/.../providers/AnnotationAvanueSettingsProvider.kt` | CREATE | 2B |
| 16 | `Apps/avanues/.../providers/ImageAvanueSettingsProvider.kt` | CREATE | 2B |
| 17 | `Apps/avanues/.../providers/WebAvanueSettingsProvider.kt` | MODIFY (extend) | 2B |
| 18 | `Apps/avanues/.../di/SettingsModule.kt` | MODIFY (register 9 new + 1 extended providers) | 2A+2B |
| 19 | `Apps/avanues/.../DeveloperPreferences.kt` | MODIFY (add debug flags) | 2C |
| 20 | `Apps/avanues/.../DeveloperSettingsScreen.kt` | MODIFY (add cockpit section) | 2C |
| 21 | `Modules/Cockpit/src/androidMain/.../CockpitScreen.kt` | MODIFY (DataStore observation) | 2D |
| 22 | `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter113-...` | CREATE | 7A |
| 23 | `docs/specs/CockpitUI-Spec-VoiceFirstUIShellSystem-260225-V1.md` | CREATE | 7B |

*Note: AvanuesNewUI gets all the same settings changes as original Avanues (copied source).*

### Session 2 (Next Session) — ~8 files

| # | File | Action | Chunk |
|---|------|--------|-------|
| 1 | `Modules/Cockpit/.../AvanueViewsStreamLayout.kt` | MODIFY (HorizontalPager) | 4A |
| 2 | `Modules/Cockpit/.../ZenCanvasLayout.kt` | MODIFY (head-tracking param) | 4B |
| 3 | `Modules/Cockpit/.../CockpitScreenContent.kt` | MODIFY (spatial offset + colorsXR) | 4B+4C |
| 4 | `Modules/Cockpit/.../viewmodel/CockpitViewModel.kt` | MODIFY (ShellNavigationAction flow) | 4D |
| 5 | `Modules/Cockpit/.../CockpitScreen.kt` | MODIFY (voice nav wiring) | 4D |
| 6 | `Modules/Cockpit/src/iosMain/.../IosCockpitScreen.kt` | CREATE | 5A |
| 7 | `Modules/Cockpit/src/desktopMain/.../DesktopKeyboardHandler.kt` | CREATE | 6A |
| 8 | Shell layouts (3 files) | MODIFY (colorsXR) | 4C |

---

## Session 3: Plugin Architecture — PanelPlugin for Cockpit Workspace

### Context

Enable 3rd-party developers to write panel plugins that render inside Cockpit workspace frames. Uses existing PluginSystem gRPC infrastructure (GrpcPluginEventBus, UniversalPluginRegistry, PluginLoader). Adds a new `PanelPlugin` contract following the established contract pattern (14 existing contracts across voiceoscore/speech/ai domains).

### Prerequisite Knowledge

**Existing contract pattern** (from exploration):
- All contracts: `interface XPlugin : UniversalPlugin` with domain-specific methods
- Location: `Modules/PluginSystem/src/commonMain/.../universal/contracts/{domain}/`
- Supporting types co-located in same file (sealed classes, enums, data classes)
- Default method implementations where sensible

**Existing infrastructure**:
- `GrpcPluginEventBus`: `MutableSharedFlow<PluginEvent>` with `publish()`, `subscribe()`, `subscribeToTypes()`
- `UniversalPluginRegistry`: O(1) capability lookup, `discoverByCapability()`, `register()`
- `PluginLoader`: 8-step loading (parse manifest → validate → check conflicts → namespace → structure → load class → register → return)
- `PluginClassLoader`: expect/actual, loads `.avp` text plugins through AVU DSL pipeline

**Content rendering chain**:
```
CockpitScreen (androidMain)
  → CockpitScreenContent (commonMain) { frameContent = { frame -> ContentRenderer(frame) } }
    → LayoutEngine → FrameWindow { content = { frameContent(frame) } }
      → ContentRenderer (androidMain) { when (frame.content) { ... } }
```

### Chunk 8: PanelPlugin Contract + FrameContent.Plugin

#### 8A: Add `COCKPIT_PANEL` capability constant

| File | Change |
|------|--------|
| `Modules/PluginSystem/.../universal/PluginCapability.kt` | Add `COCKPIT_PANEL = "cockpit.panel"` in new "Cockpit Capabilities" section after Data section |

#### 8B: Add `COCKPIT_PANEL_*` event types

| File | Change |
|------|--------|
| `Modules/PluginSystem/.../universal/PluginTypes.kt` | Add 4 event types: `TYPE_PANEL_CONTENT_UPDATED`, `TYPE_PANEL_ACTION_REQUESTED`, `TYPE_PANEL_FOCUS_CHANGED`, `TYPE_PANEL_RESIZE` |

#### 8C: Create `PanelPlugin` contract (NEW)

**File**: `Modules/PluginSystem/src/commonMain/.../universal/contracts/cockpit/PanelPlugin.kt` (~200 lines)

Follows the `OverlayPlugin` + `HandlerPlugin` hybrid pattern:

```kotlin
/**
 * Contract for plugins that render content inside Cockpit workspace panels.
 *
 * Panel plugins provide visual content displayed in FrameWindow containers
 * alongside built-in content types (Web, PDF, Note, etc.). They communicate
 * with the host Cockpit via gRPC event bus for state sync and actions.
 *
 * ## Lifecycle
 * 1. Plugin registered via PluginLoader with `cockpit.panel` capability
 * 2. Cockpit discovers plugin via UniversalPluginRegistry.discoverByCapability("cockpit.panel")
 * 3. User assigns plugin to a frame (via picker or voice command)
 * 4. Cockpit calls provideContent() → plugin returns PanelContent
 * 5. ContentRenderer displays PanelContent in FrameWindow
 * 6. User interactions forwarded via onPanelInteraction()
 * 7. Plugin publishes TYPE_PANEL_CONTENT_UPDATED events when content changes
 */
interface PanelPlugin : UniversalPlugin {

    /** Human-readable name shown in panel picker dropdown */
    val panelDisplayName: String

    /** Material icon name for panel picker and frame header */
    val panelIconName: String

    /** Panel size preferences */
    val layoutPreferences: PanelLayoutPreferences

    /** Preferred accent color (semantic name from ContentAccent or hex) */
    val accentColor: String get() = "plugin"

    /**
     * Provide the current content to render in the panel.
     * Called when frame is first displayed and after content update events.
     *
     * @param context Panel context with frame dimensions, display profile, theme info
     * @return Content to render — structured elements, WebView URL, or raw HTML
     */
    suspend fun provideContent(context: PanelContext): PanelContent

    /**
     * Handle user interaction within the panel.
     *
     * @param interaction The interaction event (tap, long-press, scroll, voice command)
     * @return true if handled, false to pass through to Cockpit default handling
     */
    suspend fun onPanelInteraction(interaction: PanelInteraction): Boolean

    /**
     * Provide actions for the CommandBar when this panel is focused.
     * Returns quick-action chips displayed in the simplified command bar.
     *
     * @return List of actions (max 6 for command bar, rest in overflow)
     */
    fun getActions(): List<PanelAction>

    /**
     * Handle a CommandBar action.
     *
     * @param actionId The action ID from getActions()
     * @return Result of the action
     */
    suspend fun onAction(actionId: String): Result<Unit>

    /**
     * Called when panel gains/loses focus in workspace.
     */
    suspend fun onFocusChanged(isFocused: Boolean) {}

    /**
     * Called when panel is resized (workspace layout change, maximize, etc.)
     */
    suspend fun onResize(width: Int, height: Int) {}
}

// ── Supporting Types ──────────────────────────────────────────

/** Content that a panel plugin renders */
@Serializable
sealed class PanelContent {
    /** Structured UI elements (text, images, lists) — rendered natively */
    @Serializable
    data class Structured(
        val elements: List<PanelElement>,
        val backgroundColor: String = "",
    ) : PanelContent()

    /** WebView-rendered content — plugin provides URL or HTML */
    @Serializable
    data class WebContent(
        val url: String = "",
        val html: String = "",
        val jsBridge: Boolean = false,
    ) : PanelContent()

    /** Compose-compatible render tree (Android only, falls back to Structured on other platforms) */
    data class ComposeContent(
        val render: Any,  // @Composable () -> Unit, typed as Any for commonMain
    ) : PanelContent()
}

/** Individual UI element in structured panel content */
@Serializable
sealed class PanelElement {
    @Serializable data class Text(val text: String, val style: TextStyle = TextStyle.BODY) : PanelElement()
    @Serializable data class Image(val uri: String, val altText: String = "") : PanelElement()
    @Serializable data class List(val items: kotlin.collections.List<String>, val ordered: Boolean = false) : PanelElement()
    @Serializable data class Divider(val label: String = "") : PanelElement()
    @Serializable data class Button(val label: String, val actionId: String) : PanelElement()
    @Serializable data class Progress(val value: Float, val label: String = "") : PanelElement()
    @Serializable data class KeyValue(val key: String, val value: String) : PanelElement()

    @Serializable enum class TextStyle { TITLE, SUBTITLE, BODY, CAPTION, CODE }
}

/** Panel interaction event */
@Serializable
sealed class PanelInteraction {
    @Serializable data class Tap(val x: Float, val y: Float, val elementId: String? = null) : PanelInteraction()
    @Serializable data class LongPress(val x: Float, val y: Float) : PanelInteraction()
    @Serializable data class Scroll(val deltaX: Float, val deltaY: Float) : PanelInteraction()
    @Serializable data class VoiceCommand(val command: String, val confidence: Float) : PanelInteraction()
    @Serializable data class ButtonClick(val actionId: String) : PanelInteraction()
}

/** Action exposed to CommandBar */
@Serializable
data class PanelAction(
    val id: String,
    val label: String,
    val iconName: String,
    val isToggle: Boolean = false,
    val isActive: Boolean = false,
)

/** Layout preferences for panel sizing */
@Serializable
data class PanelLayoutPreferences(
    val minWidth: Int = 200,
    val minHeight: Int = 150,
    val preferredAspectRatio: Float = 0f,  // 0 = flexible
    val supportsMaximize: Boolean = true,
)

/** Context passed to provideContent() */
@Serializable
data class PanelContext(
    val frameId: String,
    val width: Int,
    val height: Int,
    val displayProfile: String,  // DisplayProfile name
    val isDarkTheme: Boolean,
    val isFocused: Boolean,
    val locale: String = "en-US",
)
```

#### 8D: Add `FrameContent.Plugin` type

| File | Change |
|------|--------|
| `Modules/Cockpit/.../model/FrameContent.kt` | Add `Plugin` data class after `ExternalApp`, add `TYPE_PLUGIN` constant, add to `ALL_TYPES` |

```kotlin
/**
 * Plugin-provided panel content — renders via PanelPlugin contract.
 * The pluginId maps to a registered PanelPlugin in UniversalPluginRegistry.
 * configJson stores plugin-specific configuration for this frame instance.
 */
@Serializable
@SerialName("plugin")
data class Plugin(
    val pluginId: String = "",
    val pluginName: String = "",
    val configJson: String = "{}",
) : FrameContent() {
    override val typeId: String = TYPE_PLUGIN
}

// In companion:
const val TYPE_PLUGIN = "plugin"
// Add to ALL_TYPES list
```

#### 8E: Wire `FrameContent.Plugin` into rendering pipeline

8 files need modification (following ExternalApp pattern):

| # | File | Change |
|---|------|--------|
| 1 | `Cockpit/.../content/ContentRenderer.kt` (androidMain) | Add `is FrameContent.Plugin ->` branch in `when` block → `PluginContentRenderer(frame, content, ...)` |
| 2 | `Cockpit/.../ui/FrameWindow.kt` (commonMain) | Add `is FrameContent.Plugin ->` to `contentTypeIcon()` → `Icons.Default.Extension` |
| 3 | `Cockpit/.../model/ContentAccent.kt` (commonMain) | Add `"plugin" -> ContentAccent.CUSTOM` mapping |
| 4 | `Cockpit/.../model/ContextualActionProvider.kt` (commonMain) | Add `TYPE_PLUGIN ->` case that delegates to plugin's `getActions()` |
| 5 | `Cockpit/.../ui/CommandBar.kt` (commonMain) | Add plugin action dispatch in `contentActionFromId()` |
| 6 | `Cockpit/.../ui/CockpitScreenContent.kt` (commonMain) | Wire plugin action flow |
| 7 | `Cockpit/.../viewmodel/CockpitViewModel.kt` (commonMain) | Add `assignPluginToFrame(frameId, pluginId)` method |
| 8 | `Demo/voice-first-ui-shells.html` | Add "plugin" to module picker with generic plugin icon |

#### 8F: Create `PluginContentRenderer` (NEW, androidMain)

**File**: `Modules/Cockpit/src/androidMain/.../content/PluginContentRenderer.kt` (~150 lines)

```kotlin
@Composable
fun PluginContentRenderer(
    frame: CockpitFrame,
    content: FrameContent.Plugin,
    onContentStateChanged: (String, FrameContent) -> Unit,
    contentActionFlow: SharedFlow<ContentAction>?,
    modifier: Modifier
) {
    // 1. Lookup plugin in UniversalPluginRegistry by content.pluginId
    // 2. Cast to PanelPlugin (or show "Plugin not found" error state)
    // 3. Call provideContent(PanelContext from frame dimensions)
    // 4. Render based on PanelContent type:
    //    - Structured → native Compose rendering of PanelElement list
    //    - WebContent → AndroidView WebView with optional JS bridge
    //    - ComposeContent → invoke render lambda directly
    // 5. Collect plugin events (TYPE_PANEL_CONTENT_UPDATED) → re-call provideContent()
    // 6. Forward tap/scroll gestures → plugin.onPanelInteraction()
    // 7. Collect contentActionFlow → plugin.onAction()
}
```

### Chunk 9: gRPC Communication Protocol

#### 9A: Panel Plugin Event Flow

```
Plugin                          Cockpit
  |                                |
  |--- register (PluginLoader) --->|  (UniversalPluginRegistry.register())
  |                                |
  |<-- discoverByCapability() -----|  (find all "cockpit.panel" plugins)
  |                                |
  |<-- provideContent(ctx) --------|  (user assigns plugin to frame)
  |--- PanelContent -------------->|  (render in FrameWindow)
  |                                |
  |<-- onPanelInteraction(tap) ----|  (user taps in panel)
  |--- Result<Unit> -------------->|  (handled/unhandled)
  |                                |
  |--- publish(CONTENT_UPDATED) -->|  (plugin data changed)
  |<-- provideContent(ctx) --------|  (re-render)
  |--- PanelContent -------------->|
  |                                |
  |<-- onFocusChanged(true) -------|  (panel gains focus)
  |--- getActions() -------------->|  (populate CommandBar)
  |                                |
  |<-- onAction("refresh") --------|  (user taps CommandBar chip)
  |--- Result<Unit> -------------->|
```

#### 9B: Plugin Discovery in Cockpit

| File | Change |
|------|--------|
| `Cockpit/.../viewmodel/CockpitViewModel.kt` | Add `discoverPanelPlugins(): List<PanelPluginInfo>` using `registry.discoverByCapability("cockpit.panel")` |
| `Cockpit/.../ui/CockpitScreenContent.kt` | Wire plugin list to panel picker dropdown (workspace mode) |

```kotlin
// In CockpitViewModel:
fun discoverPanelPlugins(): List<PanelPluginInfo> {
    val registrations = registry.discoverByCapability(PluginCapability.COCKPIT_PANEL)
    return registrations.map { reg ->
        val plugin = registry.getPlugin(reg.pluginId) as? PanelPlugin
        PanelPluginInfo(
            pluginId = reg.pluginId,
            displayName = plugin?.panelDisplayName ?: reg.pluginName,
            iconName = plugin?.panelIconName ?: "Extension",
            version = reg.version,
        )
    }
}

data class PanelPluginInfo(
    val pluginId: String,
    val displayName: String,
    val iconName: String,
    val version: String,
)
```

### Chunk 10: Sample Plugin — Weather Panel

A built-in sample plugin proving end-to-end flow. Ships with the app as a "first-party" plugin using the same contract 3rd parties use.

#### 10A: Plugin Manifest

**File**: `Modules/PluginSystem/src/commonMain/.../plugins/builtin/weather/plugin.yaml`

```yaml
id: com.augmentalis.plugins.weather
name: Weather Panel
version: 1.0.0
author: Augmentalis
description: Live weather conditions in a Cockpit panel
entrypoint: com.augmentalis.magiccode.plugins.builtin.weather.WeatherPanelPlugin
capabilities:
  - cockpit.panel
permissions:
  - network.http
source: builtin
verificationLevel: trusted
```

#### 10B: Plugin Implementation

**File**: `Modules/PluginSystem/src/commonMain/.../plugins/builtin/weather/WeatherPanelPlugin.kt` (~180 lines)

```kotlin
class WeatherPanelPlugin : PanelPlugin {
    override val pluginId = "com.augmentalis.plugins.weather"
    override val pluginName = "Weather Panel"
    override val version = "1.0.0"
    override val panelDisplayName = "Weather"
    override val panelIconName = "Cloud"
    override val accentColor = "info"  // maps to ContentAccent.INFO
    override val layoutPreferences = PanelLayoutPreferences(
        minWidth = 200, minHeight = 200, supportsMaximize = true
    )
    override val capabilities = setOf(
        PluginCapability(
            id = PluginCapability.COCKPIT_PANEL,
            name = "Weather Panel",
            version = "1.0.0",
            interfaces = setOf("PanelPlugin")
        )
    )

    private var currentWeather: WeatherData = WeatherData.placeholder()
    // ... standard UniversalPlugin lifecycle (initialize, activate, shutdown)

    override suspend fun provideContent(context: PanelContext): PanelContent {
        return PanelContent.Structured(
            elements = listOf(
                PanelElement.Text("San Francisco", PanelElement.TextStyle.TITLE),
                PanelElement.Text("☀️ 72°F / 22°C", PanelElement.TextStyle.SUBTITLE),
                PanelElement.Divider(),
                PanelElement.KeyValue("Humidity", "${currentWeather.humidity}%"),
                PanelElement.KeyValue("Wind", "${currentWeather.windSpeed} mph"),
                PanelElement.KeyValue("UV Index", "${currentWeather.uvIndex}"),
                PanelElement.Progress(currentWeather.humidity / 100f, "Humidity"),
            )
        )
    }

    override suspend fun onPanelInteraction(interaction: PanelInteraction): Boolean {
        return when (interaction) {
            is PanelInteraction.VoiceCommand -> {
                if (interaction.command.contains("refresh")) {
                    refreshWeather(); true
                } else false
            }
            else -> false
        }
    }

    override fun getActions() = listOf(
        PanelAction("refresh", "Refresh", "Refresh"),
        PanelAction("units", "°F/°C", "Thermostat", isToggle = true),
        PanelAction("forecast", "Forecast", "CalendarMonth"),
    )

    override suspend fun onAction(actionId: String): Result<Unit> { /* ... */ }
}

@Serializable
data class WeatherData(
    val temp: Float, val condition: String, val humidity: Float,
    val windSpeed: Float, val uvIndex: Int,
) {
    companion object {
        fun placeholder() = WeatherData(72f, "Sunny", 45f, 8f, 5)
    }
}
```

#### 10C: Register Built-in Plugin

| File | Change |
|------|--------|
| `Modules/PluginSystem/.../core/BuiltinPluginRegistrar.kt` (NEW) | Auto-register weather plugin on app startup |
| `Apps/avanues/.../di/PluginModule.kt` (NEW or extend existing) | `@Provides @Singleton` the BuiltinPluginRegistrar |

### Chunk 11: WebView CockpitBridge for Web-Based Panel Apps

For plugins that use `PanelContent.WebContent(jsBridge = true)`, provide a JavaScript API.

#### 11A: CockpitBridge JS API

**File**: `Modules/Cockpit/src/androidMain/.../content/CockpitBridge.kt` (~100 lines)

Follows `DOMScraperBridge` pattern from WebAvanue:

```kotlin
class CockpitBridge(
    private val plugin: PanelPlugin,
    private val eventBus: PluginEventBus,
) {
    /** Called from JS: window.CockpitBridge.sendAction(actionId) */
    @JavascriptInterface
    fun sendAction(actionId: String) { /* → plugin.onAction(actionId) */ }

    /** Called from JS: window.CockpitBridge.updateContent(jsonPayload) */
    @JavascriptInterface
    fun updateContent(json: String) { /* → publish TYPE_PANEL_CONTENT_UPDATED */ }

    /** Called from JS: window.CockpitBridge.getContext() → JSON string */
    @JavascriptInterface
    fun getContext(): String { /* → serialize PanelContext to JSON */ }

    /** Called from JS: window.CockpitBridge.log(message) */
    @JavascriptInterface
    fun log(message: String) { /* → Napier.d(message, tag = pluginId) */ }
}
```

Injected into WebView via `addJavascriptInterface(bridge, "CockpitBridge")` in PluginContentRenderer when `PanelContent.WebContent(jsBridge = true)`.

### Chunk 12: Voice Command Integration for Plugins

#### 12A: Plugin Voice Commands

| File | Change |
|------|--------|
| `Modules/VoiceOSCore/.../handlers/CockpitCommandHandler.kt` | Add: "open weather panel" → `cockpit.assignPluginToFrame(focusedFrameId, "com.augmentalis.plugins.weather")` |
| VOS entries | Add `cockpit_plugin_open` / `cockpit_plugin_refresh` / `cockpit_plugin_close` commands |

Plugin-specific voice commands forwarded via:
1. User says command → SpeechRecognition → CockpitCommandHandler
2. Handler checks if focused frame is `FrameContent.Plugin`
3. If yes → creates `PanelInteraction.VoiceCommand(command, confidence)`
4. Forwards to `plugin.onPanelInteraction(interaction)`

### Session 3 Files Summary — ~15 files

| # | File | Action | Chunk |
|---|------|--------|-------|
| 1 | `PluginSystem/.../universal/PluginCapability.kt` | MODIFY (add COCKPIT_PANEL) | 8A |
| 2 | `PluginSystem/.../universal/PluginTypes.kt` | MODIFY (add 4 event types) | 8B |
| 3 | `PluginSystem/.../universal/contracts/cockpit/PanelPlugin.kt` | CREATE (~200 lines) | 8C |
| 4 | `Cockpit/.../model/FrameContent.kt` | MODIFY (add Plugin type) | 8D |
| 5 | `Cockpit/.../content/ContentRenderer.kt` (androidMain) | MODIFY (add Plugin branch) | 8E |
| 6 | `Cockpit/.../ui/FrameWindow.kt` | MODIFY (add Plugin icon) | 8E |
| 7 | `Cockpit/.../model/ContentAccent.kt` | MODIFY (add plugin mapping) | 8E |
| 8 | `Cockpit/.../model/ContextualActionProvider.kt` | MODIFY (add Plugin actions) | 8E |
| 9 | `Cockpit/.../ui/CommandBar.kt` | MODIFY (plugin action dispatch) | 8E |
| 10 | `Cockpit/src/androidMain/.../content/PluginContentRenderer.kt` | CREATE (~150 lines) | 8F |
| 11 | `Cockpit/.../viewmodel/CockpitViewModel.kt` | MODIFY (discoverPanelPlugins + assignPlugin) | 9B |
| 12 | `PluginSystem/.../plugins/builtin/weather/WeatherPanelPlugin.kt` | CREATE (~180 lines) | 10B |
| 13 | `PluginSystem/.../plugins/builtin/weather/plugin.yaml` | CREATE | 10A |
| 14 | `PluginSystem/.../core/BuiltinPluginRegistrar.kt` | CREATE (~40 lines) | 10C |
| 15 | `Cockpit/src/androidMain/.../content/CockpitBridge.kt` | CREATE (~100 lines) | 11A |

### Session 3 Verification

1. **Build PluginSystem**: `./gradlew :Modules:PluginSystem:compileKotlinMetadata` — PanelPlugin contract compiles
2. **Build Cockpit**: `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid` — FrameContent.Plugin + ContentRenderer compiles
3. **Plugin discovery**: Weather plugin appears in `discoverPanelPlugins()` result
4. **Assign to frame**: Create frame with `FrameContent.Plugin(pluginId = "com.augmentalis.plugins.weather")` → Weather panel renders
5. **CommandBar**: Focus weather panel → "Refresh", "°F/°C", "Forecast" chips appear
6. **Voice**: "open weather panel" → assigns weather to focused frame
7. **Content update**: Plugin publishes TYPE_PANEL_CONTENT_UPDATED → panel re-renders
8. **Lifecycle**: Navigate away and back → plugin state preserved via saveState/restoreState

---

## Session 4: 3rd-Party Plugin Developer Experience

### Chunk 13: Plugin SDK + Documentation

#### 13A: Plugin Developer SDK

Create a minimal SDK artifact that 3rd-party developers can depend on:

**File**: `Modules/PluginSystem/sdk/` — stripped-down module containing ONLY:
- `PanelPlugin.kt` (contract interface + types)
- `UniversalPlugin.kt` (base interface)
- `PluginCapability.kt` (capability constants)
- `PluginTypes.kt` (events, filters)
- `PluginManifest.kt` (manifest format reference)

No internal implementation exposed. Published as `com.augmentalis:plugin-sdk:1.0.0`.

#### 13B: Developer Manual Chapter 114 — Writing Cockpit Panel Plugins

**File**: `Docs/MasterDocs/PluginSystem/Developer-Manual-Chapter114-WritingCockpitPanelPlugins.md`

Outline:
1. **Quick Start** — 5-minute guide to create a weather panel plugin
2. **Plugin Manifest** — Required fields, capabilities declaration, permissions
3. **PanelPlugin Contract** — Interface methods, lifecycle, content types
4. **Rendering Options** — Structured (native), WebContent (HTML), ComposeContent (Android-only)
5. **CommandBar Integration** — Providing actions, handling action callbacks
6. **Voice Commands** — Handling VoiceCommand interactions, registering custom phrases
7. **gRPC Events** — Publishing content updates, subscribing to Cockpit events
8. **JS Bridge** — CockpitBridge API for web-based panel apps
9. **Testing** — Unit testing with MockPanelContext, integration testing
10. **Distribution** — .avp format, plugin.yaml manifest, sideloading, future marketplace
11. **Sample Plugins** — Weather, RSS Feed, Calculator, Pomodoro Timer

#### 13C: Plugin Template Project

**File**: `templates/plugin-template/` — scaffolded project with:
- `plugin.yaml` (fill-in-the-blank manifest)
- `MyPanelPlugin.kt` (skeleton implementing PanelPlugin)
- `build.gradle.kts` (depends on plugin-sdk)
- `README.md` (getting started)

### Chunk 14: Advanced Plugin Features

#### 14A: Plugin-to-Plugin Communication

Panel plugins can communicate via PluginEventBus:
- Weather plugin publishes `TYPE_PANEL_CONTENT_UPDATED` with weather data in payload
- Map plugin subscribes to weather events → shows weather overlay on map
- Already supported by existing `GrpcPluginEventBus.subscribe(EventFilter)` — no new code needed

#### 14B: Plugin Marketplace Foundation

| File | Change |
|------|--------|
| `Modules/PluginSystem/.../core/PluginMarketplace.kt` (NEW) | Interface for browse/search/install/update/uninstall |
| `Modules/PluginSystem/.../core/PluginSecurity.kt` (NEW) | Signature verification, sandboxing rules, permission enforcement |

*Marketplace implementation deferred — interface defined now for forward compatibility.*

### Session 4 Verification

1. Plugin SDK builds independently
2. Template project compiles against SDK
3. Chapter 114 has complete working code examples
4. Weather plugin works end-to-end as documented

---

## Phased Roadmap Summary

| Session | Focus | Files | Estimated Lines |
|---------|-------|-------|-----------------|
| **Pre-1** | HTML Demo (cockpit workspace + live shells) | 1 | ~350 |
| **1** | AvanuesNewUI app + Settings (48 keys, 10 providers) | ~23 | ~2000 |
| **2** | Glass + iOS + Mac + Docs | ~10 | ~800 |
| **3** | Plugin Architecture (PanelPlugin + Weather + Bridge) | ~15 | ~900 |
| **4** | Plugin SDK + Dev Docs + Template | ~8 | ~600 |

### Dependency Chain

```
Pre-1: HTML Demo  →  confirms UX before KMP work
   ↓
Session 1: KMP Workspace + Settings  →  foundation for all shells
   ↓
Session 2: Glass + iOS + Mac  →  platform coverage
   ↓
Session 3: Plugin Architecture  →  uses Workspace from Session 1
   ↓
Session 4: Plugin SDK + Docs  →  developer-facing deliverables
```

### Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Plugin rendering performance | PanelContent.Structured uses native Compose (fast). WebContent WebView is heavier but same as FrameContent.Web (proven). ComposeContent gives max performance for Android plugins. |
| gRPC overhead for local plugins | Built-in plugins (Weather) use direct method calls, not actual gRPC transport. GrpcPluginEventBus uses local SharedFlow. True gRPC only for out-of-process plugins. |
| Plugin security | PluginLoader 8-step validation + PluginManifest.verificationLevel (trusted/verified/community). Permissions declared in manifest and enforced at runtime. |
| Breaking contract changes | Plugin SDK versioned separately. PanelPlugin interface uses default method implementations — new methods don't break existing plugins. |
| Context window per session | Each session is scoped to 10-15 files max. Plugin architecture (Session 3) is independent of settings work (Session 1). |

---

## Plan File for `docs/plans/`

The comprehensive plan will be saved to: `docs/plans/Cockpit/Cockpit-Plan-WorkspacePluginArchitecture-260225-V1.md`

Contents = this entire plan file (Part 1 through Session 4), reformatted as a standard plan document.

---

## Verification

### Session 1
1. **Build both apps**: `compileDebugKotlinAndroid` for Avanues + AvanuesNewUI
2. **Build Foundation**: `compileKotlinMetadata` — all ~48 new SettingsKeys compile
3. **Install AvanuesNewUI**: Verify it appears as separate app with distinct label
4. **Launch**: AvanuesNewUI defaults to Lens shell (not Classic dashboard)
5. **Settings → Cockpit**: Shell mode dropdown visible, changing it switches the home screen
6. **Settings → All modules**: All 10 settings providers visible in correct sort order (Cockpit/350 → ImageAvanue/800)
7. **Developer Settings**: Force shell mode override works
8. **Kill + relaunch**: Shell preference persists via DataStore
9. **All features work**: Voice commands, content rendering, accessibility service, all module aliases
10. **Docs**: Chapter 113 exists with template code, spec document exists in docs/specs/

### Session 2
1. **Glass**: GLASS_MICRO AvanueViews shows HorizontalPager with swipe
2. **Canvas**: Head turn pans canvas on glasses
3. **Voice**: "next card" advances pager, "canvas zoom in" zooms
4. **iOS**: CockpitScreen renders with shell routing
5. **Mac**: Cmd+1/2/3/4 switches shells, Cmd+K activates Lens

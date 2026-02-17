# Cockpit-Plan-EcosystemReviewFixes-260218-V1

## Overview
Fix all 28 issues from the Cockpit ecosystem code review.
Approach: Priority-first within code-proximity groups (Hybrid).

## Phase 1: VoiceOSCore (C1 + C2)
- **C1**: Create `CockpitCommandHandler.kt` (26 cockpit_ commands)
- **C2**: Add `COCKPIT` to `ActionCategory` enum + PRIORITY_ORDER

## Phase 2: CockpitViewModel.kt (H1 + H2 + H5 + H8 + M10 + L3)
- **H1**: Make `createSession` suspend, await DB save before return
- **H2**: Add `dispose()` method that cancels scope
- **H5**: `updateContentState()` also updates `_frames` in memory
- **H8**: Sync `_selectedFrameId` → `_activeSession.selectedFrameId` before save
- **M10**: Replace `(0..99999).random()` with UUID or larger entropy
- **L3**: `setLayoutMode()`/`renameSession()` also update `_sessions` list

## Phase 3: ContentRenderer.kt (H4 + H9 + M2 + L1)
- **H4**: Use kotlinx.serialization for Note save JSON
- **H9**: Wire ScreenCast model fields to CastState
- **M2**: Use kotlinx.serialization for URL state JSON
- **L1**: Add TODO comment on AiSummary deferred integration

## Phase 4: Repositories (H7)
- **H7**: Replace double-encode export with direct serialization

## Phase 5: Scattered CommonMain (H3 + M1 + M3 + M4 + M5 + M6 + M7 + M11 + L2 + L4 + L6)
- **H3**: Delete dead `WorkflowLayout` function from LayoutEngine.kt
- **M1**: Replace `String.format()` → padStart in WidgetContent.kt
- **M3**: Remove misleading `frameNumber` property from CockpitFrame.kt
- **M4**: Add LaunchedEffect auto-switch in CockpitScreenContent.kt
- **M5**: Add TODO on no-op content chips in CommandBar.kt
- **M6**: Add canvasWidth/canvasHeight to snapEdges remember key
- **M7**: Synchronize activeConsumers in AndroidSpatialOrientationSource
- **M11**: Remove orphaned "annotation" from ContentAccent
- **L2**: Add explanatory comment on LazyColumn+horizontalScroll
- **L4**: Remove dead `frames` property from CockpitSession + dependents
- **L6**: Add SPATIAL_DICE to SPATIAL_CAPABLE set

## Phase 6: NoteEditor + build.gradle (M9 + L5)
- **M9**: Fix hasLoaded key collision in NoteEditor
- **L5**: Remove duplicate CameraX deps from Cockpit build.gradle.kts

## Build Verification
```
./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid
./gradlew :Modules:Cockpit:compileDebugKotlinAndroid
./gradlew :Modules:Cockpit:compileKotlinDesktop
./gradlew :Modules:NoteAvanue:compileDebugKotlinAndroid
```

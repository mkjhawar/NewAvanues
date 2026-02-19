# VideoAvanue Full Implementation Plan

**Document:** VideoAvanue-Plan-FullImplementation-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .cot
**Author:** Manoj Jhawar

---

## Summary

Elevate VideoAvanue from a 2-file shell (models only, no player UI) to a fully functional KMP video
player module with Media3 ExoPlayer on Android, JavaFX on Desktop, voice command integration via
12 VOS commands across 5 locales, and live Cockpit frame wiring. Target KMP Score: ~45% (shared
models + controller interface + state; platform-specific player UI and JavaFX bridge).

---

## Current State

| File | Path | Status |
|------|------|--------|
| `VideoItem.kt` | `commonMain/.../model/VideoItem.kt` | EXISTS — complete, keep as-is |
| `VideoPlayerState.kt` | (inside VideoItem.kt) | EXISTS (inline) — split into own file |
| `RepeatMode.kt` | (inside VideoItem.kt) | EXISTS (inline) — split into own file |
| `VideoPlayer.kt` | `androidMain/.../ui/VideoPlayer.kt` | DOES NOT EXIST |
| `IVideoController.kt` | `commonMain/.../controller/` | DOES NOT EXIST |
| `AndroidVideoController.kt` | `androidMain/.../controller/` | DOES NOT EXIST |
| `DesktopVideoPlayer.kt` | `desktopMain/.../ui/` | DOES NOT EXIST |
| `DesktopVideoController.kt` | `desktopMain/.../controller/` | DOES NOT EXIST |
| `VideoCommandHandler.kt` | `VoiceOSCore/androidMain/.../handlers/` | DOES NOT EXIST |

**KMP Score today:** ~10% (models shared, nothing else implemented).
**Build config:** `build.gradle.kts` already declares `media3-exoplayer`, `media3-ui`, `media3-session`.
No new Gradle changes needed for Android. Desktop JavaFX dependency must be added.

---

## Architecture Overview

```
commonMain
  model/
    VideoItem.kt          (KEEP — already complete)
    VideoPlayerState.kt   (SPLIT OUT from VideoItem.kt)
    PlaybackSpeed.kt      (NEW — enum with next()/previous())
    RepeatMode.kt         (SPLIT OUT from VideoItem.kt)
  controller/
    IVideoController.kt   (NEW — platform-neutral interface)

androidMain
  controller/
    AndroidVideoController.kt  (NEW — ExoPlayer implementation)
  ui/
    VideoPlayer.kt             (NEW — Composable, AndroidView + PlayerView)
    VideoControlBar.kt         (NEW — Composable control strip)
    VideoThumbnailCard.kt      (NEW — gallery card)
  gallery/
    VideoGalleryScreen.kt      (NEW — MediaStore-backed grid)

desktopMain
  controller/
    DesktopVideoController.kt  (NEW — JavaFX MediaPlayer implementation)
  ui/
    DesktopVideoPlayer.kt      (NEW — Compose + SwingPanel JavaFX bridge)

VoiceOSCore / androidMain / handlers/
    VideoCommandHandler.kt     (NEW — routes 12 VIDEO_* action types)
```

---

## Phase 1: commonMain Models

### 1.1 VideoItem.kt — Keep As-Is

File is complete. Contains `VideoItem`, `VideoPlayerState`, and `RepeatMode` inline. The inline
approach is acceptable for a model file of this size (44 lines). No changes needed.

**Decision:** Keep `VideoPlayerState` and `RepeatMode` inline in `VideoItem.kt`. Splitting to
separate files adds navigation overhead with no functional benefit at this scale.

### 1.2 PlaybackSpeed.kt — NEW

**Path:** `Modules/VideoAvanue/src/commonMain/kotlin/com/augmentalis/videoavanue/model/PlaybackSpeed.kt`

```kotlin
package com.augmentalis.videoavanue.model

enum class PlaybackSpeed(val multiplier: Float, val label: String) {
    HALF(0.5f, "0.5x"),
    THREE_QUARTER(0.75f, "0.75x"),
    NORMAL(1.0f, "1x"),
    ONE_AND_QUARTER(1.25f, "1.25x"),
    ONE_AND_HALF(1.5f, "1.5x"),
    DOUBLE(2.0f, "2x");

    fun next(): PlaybackSpeed = entries[(ordinal + 1) % entries.size]
    fun previous(): PlaybackSpeed = entries[(ordinal - 1 + entries.size) % entries.size]

    companion object {
        fun fromMultiplier(value: Float): PlaybackSpeed =
            entries.minByOrNull { kotlin.math.abs(it.multiplier - value) } ?: NORMAL
    }
}
```

### 1.3 IVideoController.kt — NEW

**Path:** `Modules/VideoAvanue/src/commonMain/kotlin/com/augmentalis/videoavanue/controller/IVideoController.kt`

Interface contract implemented by both `AndroidVideoController` and `DesktopVideoController`.
All methods are suspend where they may involve I/O (seek, prepare). State is a hot `StateFlow`
so composables can collect it reactively.

```kotlin
package com.augmentalis.videoavanue.controller

import com.augmentalis.videoavanue.model.PlaybackSpeed
import com.augmentalis.videoavanue.model.VideoItem
import com.augmentalis.videoavanue.model.VideoPlayerState
import kotlinx.coroutines.flow.StateFlow

interface IVideoController {
    val state: StateFlow<VideoPlayerState>

    suspend fun load(video: VideoItem)
    fun play()
    fun pause()
    fun stop()
    fun togglePlayPause()
    fun seekForward(ms: Long = 10_000L)
    fun seekBackward(ms: Long = 10_000L)
    suspend fun seekTo(positionMs: Long)
    fun setSpeed(speed: PlaybackSpeed)
    fun speedUp()
    fun speedDown()
    fun toggleLoop()
    fun toggleMute()
    fun setVolume(volume: Float)
    fun release()
}
```

**Design rationale:** `play()` and `pause()` are not suspend because ExoPlayer's `play()`/`pause()`
are synchronous UI-thread calls. `seekTo()` is suspend because accurate seeking involves waiting
for the player's `SEEK_ENDED` event before updating state. `load()` is suspend to allow await on
`MediaItem` preparation.

---

## Phase 2: androidMain Implementation

### 2.1 AndroidVideoController.kt — NEW

**Path:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/controller/AndroidVideoController.kt`

Wraps `androidx.media3.exoplayer.ExoPlayer`. Uses a `Player.Listener` to update `_state` as a
`MutableStateFlow<VideoPlayerState>`. Must be created on the main thread and released on the main
thread (`player.release()` is main-thread-only).

Key implementation points:
- `ExoPlayer.Builder(context).build()` in `init` block (main-thread-safe)
- `Player.Listener.onIsPlayingChanged` → update `isPlaying` in state
- `Player.Listener.onPlaybackStateChanged` → update `isLoading` (STATE_BUFFERING) and `error`
- `Player.Listener.onPositionDiscontinuity` → update `positionMs`
- Position polling: `CoroutineScope(Dispatchers.Main)` + `while(true) { delay(500); updatePosition() }`
- `setSpeed()`: `player.setPlaybackSpeed(speed.multiplier)`
- `toggleLoop()`: `player.repeatMode = if (RepeatMode.OFF) RepeatMode.ONE else RepeatMode.OFF`
- `toggleMute()`: `player.volume = if (isMuted) savedVolume else 0f`

### 2.2 VideoPlayer.kt — NEW

**Path:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/ui/VideoPlayer.kt`

Composable wrapping `androidx.media3.ui.PlayerView` via `AndroidView`. Overlays a Compose
`VideoControlBar` on top. Fullscreen enters by hiding system bars via `WindowInsetsController`.

```
VideoPlayer (Composable)
  Box(fillMaxSize)
    AndroidView { PlayerView }           // ExoPlayer native surface
    AnimatedVisibility(controlsVisible)  // auto-hide after 3s
      VideoControlBar(state, controller)
    Box(top=0, fill=Horizontal)
      // title + back button row
```

AVID semantics on all interactive elements:
- Play/Pause: `Modifier.semantics { contentDescription = "Voice: click play pause" }`
- Seek forward: `"Voice: seek forward ten seconds"`
- Seek backward: `"Voice: seek backward ten seconds"`
- Speed selector: `"Voice: set playback speed"`
- Fullscreen toggle: `"Voice: toggle fullscreen"`
- Loop toggle: `"Voice: toggle loop"`
- Mute toggle: `"Voice: toggle mute"`

AvanueUI theme: `AvanueTheme.colors.surface` with `0.85f` alpha overlay for control bar.
Background gradient: `verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))`.

### 2.3 VideoControlBar.kt — NEW

**Path:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/ui/VideoControlBar.kt`

```
VideoControlBar
  Column
    // Timeline row
    Row
      Text(positionFormatted)
      Slider(progress, onValueChange -> seekTo)
      Text(durationFormatted)
    // Control row
    Row(SpaceBetween)
      IconButton(Loop)          // RepeatMode indicator
      IconButton(SkipBack10)
      IconButton(Play/Pause)    // Large, center
      IconButton(SkipFwd10)
      IconButton(Mute)
    // Speed row
    Row(chips)
      SpeedChip("0.5x") … SpeedChip("2x")
```

Speed chips use `FilterChip` from `AvanueUI` components with `AvanueTheme.colors.primary` selected
color. Active chip highlights the current `PlaybackSpeed`.

### 2.4 VideoThumbnailCard.kt — NEW

**Path:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/ui/VideoThumbnailCard.kt`

`AvanueCard` composable displaying thumbnail (via `AsyncImage` from Coil or `BitmapFactory` from
`thumbnailUri`), title, and `durationFormatted`. Tap triggers `onVideoSelected(VideoItem)`.

Note: Coil 3 KMP is not yet in the Gradle config. For the first implementation, load thumbnails
via `android.media.ThumbnailUtils.createVideoThumbnail()` on a `Dispatchers.IO` coroutine. Add
Coil in a follow-up once the KMP Coil dependency is standardised across all modules.

### 2.5 VideoGalleryScreen.kt — NEW

**Path:** `Modules/VideoAvanue/src/androidMain/kotlin/com/augmentalis/videoavanue/gallery/VideoGalleryScreen.kt`

`LazyVerticalGrid` backed by `MediaStore.Video.Media` content resolver query. Returns
`List<VideoItem>`. Launched from a `ViewModel` via `viewModelScope.launch(Dispatchers.IO)`.
Grid calls `VideoThumbnailCard` per item. Selecting a card navigates to `VideoPlayer`.

Requires `READ_MEDIA_VIDEO` permission (Android 13+) or `READ_EXTERNAL_STORAGE` (Android 12-).
Permission check happens in the composable using `rememberLauncherForActivityResult`.

---

## Phase 3: desktopMain Implementation

### 3.1 DesktopVideoController.kt — NEW

**Path:** `Modules/VideoAvanue/src/desktopMain/kotlin/com/augmentalis/videoavanue/controller/DesktopVideoController.kt`

Wraps `javafx.scene.media.MediaPlayer`. JavaFX must run on the JavaFX Application Thread; bridge
events to a `CoroutineScope(Dispatchers.Default)` via `Platform.runLater` callbacks.

```kotlin
class DesktopVideoController : IVideoController {
    private var mediaPlayer: javafx.scene.media.MediaPlayer? = null
    private val _state = MutableStateFlow(VideoPlayerState())
    override val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    override suspend fun load(video: VideoItem) {
        withContext(Dispatchers.Main) {  // JFX thread
            val media = javafx.scene.media.Media(video.uri)
            mediaPlayer?.dispose()
            mediaPlayer = javafx.scene.media.MediaPlayer(media).apply {
                setOnPlaying { _state.update { it.copy(isPlaying = true, isLoading = false) } }
                setOnPaused  { _state.update { it.copy(isPlaying = false) } }
                setOnStopped { _state.update { it.copy(isPlaying = false, positionMs = 0) } }
                setOnReady   {
                    _state.update { s ->
                        s.copy(durationMs = totalDuration?.toMillis()?.toLong() ?: 0L,
                               isLoading = false)
                    }
                }
                currentTimeProperty().addListener { _, _, newVal ->
                    _state.update { s -> s.copy(positionMs = newVal.toMillis().toLong()) }
                }
            }
        }
    }
    // play(), pause(), seekTo(), setSpeed(), toggleMute(), toggleLoop(), release() follow same pattern
}
```

### 3.2 DesktopVideoPlayer.kt — NEW

**Path:** `Modules/VideoAvanue/src/desktopMain/kotlin/com/augmentalis/videoavanue/ui/DesktopVideoPlayer.kt`

Uses Compose `SwingPanel` to embed a `javafx.embed.swing.JFXPanel` containing a `MediaView`.
Control bar is a pure Compose overlay (no JavaFX controls), identical API to `VideoControlBar`
but using Desktop-compatible Compose APIs.

```kotlin
@Composable
fun DesktopVideoPlayer(controller: DesktopVideoController, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    Box(modifier) {
        SwingPanel(
            factory = { createJfxPanel(controller) },
            modifier = Modifier.fillMaxSize()
        )
        DesktopVideoControlBar(state = state, controller = controller,
            modifier = Modifier.align(Alignment.BottomCenter))
    }
}
```

**Gradle dependency to add (desktopMain):**

```kotlin
val desktopMain by getting {
    dependencies {
        implementation("org.openjfx:javafx-media:21:mac-aarch64")
        implementation("org.openjfx:javafx-swing:21:mac-aarch64")
    }
}
```

Note: JavaFX classifier must match the host platform at build time. Use `System.getProperty("os.arch")`
in a `buildSrc` helper to select the correct classifier (mac-aarch64, linux-x86_64, win-x86_64).

---

## Phase 4: VoiceOS Integration

### 4.1 CommandActionType additions

Add 12 new entries to `CommandActionType.kt` in the `VIDEO` block:

```kotlin
// VIDEO (Category: VIDEO, Priority 17)
VIDEO_PLAY,
VIDEO_PAUSE,
VIDEO_TOGGLE_PLAY_PAUSE,
VIDEO_SEEK_FORWARD,
VIDEO_SEEK_BACKWARD,
VIDEO_SPEED_UP,
VIDEO_SPEED_DOWN,
VIDEO_SPEED_NORMAL,
VIDEO_TOGGLE_MUTE,
VIDEO_TOGGLE_LOOP,
VIDEO_TOGGLE_FULLSCREEN,
VIDEO_OPEN_GALLERY,
```

### 4.2 ActionCategory addition

Add to `ActionCategory.kt` after `CAMERA` (priority 14):

```kotlin
VIDEO(priority = 17),
```

### 4.3 VideoCommandHandler.kt — NEW

**Path:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/handlers/VideoCommandHandler.kt`

Extends `BaseHandler`. Category = `ActionCategory.VIDEO`. Receives an `IVideoController` reference
injected at construction via `AndroidHandlerFactory`.

```kotlin
class VideoCommandHandler(
    private val videoController: IVideoController
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.VIDEO

    override val supportedActions: List<String> = listOf(
        "play video", "play",
        "pause video", "pause",
        "toggle play", "play pause",
        "skip forward", "seek forward", "forward ten seconds",
        "skip back", "seek backward", "back ten seconds",
        "speed up", "faster",
        "slow down", "slower",
        "normal speed",
        "mute video", "unmute video",
        "loop video", "toggle loop",
        "fullscreen", "toggle fullscreen",
        "open gallery", "video gallery"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        return when (command.actionType) {
            CommandActionType.VIDEO_PLAY            -> { videoController.play(); HandlerResult.success("Playing") }
            CommandActionType.VIDEO_PAUSE           -> { videoController.pause(); HandlerResult.success("Paused") }
            CommandActionType.VIDEO_TOGGLE_PLAY_PAUSE -> { videoController.togglePlayPause(); HandlerResult.success("Toggled") }
            CommandActionType.VIDEO_SEEK_FORWARD    -> { videoController.seekForward(); HandlerResult.success("Forward 10s") }
            CommandActionType.VIDEO_SEEK_BACKWARD   -> { videoController.seekBackward(); HandlerResult.success("Back 10s") }
            CommandActionType.VIDEO_SPEED_UP        -> { videoController.speedUp(); HandlerResult.success("Speed up") }
            CommandActionType.VIDEO_SPEED_DOWN      -> { videoController.speedDown(); HandlerResult.success("Speed down") }
            CommandActionType.VIDEO_SPEED_NORMAL    -> { videoController.setSpeed(PlaybackSpeed.NORMAL); HandlerResult.success("Normal speed") }
            CommandActionType.VIDEO_TOGGLE_MUTE     -> { videoController.toggleMute(); HandlerResult.success("Mute toggled") }
            CommandActionType.VIDEO_TOGGLE_LOOP     -> { videoController.toggleLoop(); HandlerResult.success("Loop toggled") }
            CommandActionType.VIDEO_TOGGLE_FULLSCREEN -> HandlerResult.success("Fullscreen — UI only")
            CommandActionType.VIDEO_OPEN_GALLERY    -> HandlerResult.success("Gallery — UI only")
            else                                    -> HandlerResult.notHandled()
        }
    }
}
```

Note on `VIDEO_TOGGLE_FULLSCREEN` and `VIDEO_OPEN_GALLERY`: These are UI-navigation events. The
handler returns `HandlerResult.success()` with a special tag; the VoiceOS overlay dispatches a
deeplink intent or a Compose navigation event. The exact mechanism follows the pattern already
used by `AppControlHandler` for activity launching.

### 4.4 AndroidHandlerFactory — Update

In `AndroidHandlerFactory.createHandlers()`, add after `NoteCommandHandler`:

```kotlin
VideoCommandHandler(videoController = videoControllerProvider())
```

Where `videoControllerProvider` is a lambda `() -> IVideoController` passed in from
`VoiceOSAccessibilityService`, which holds a singleton `AndroidVideoController` created when the
service starts.

### 4.5 VOS Commands — en-US.app.vos additions

Add a `[VIDEO]` section to `en-US.app.vos` (and all 4 other locale files):

```
[VIDEO]
play video | VIDEO_PLAY | video
pause video | VIDEO_PAUSE | video
play pause | VIDEO_TOGGLE_PLAY_PAUSE | video
skip forward | VIDEO_SEEK_FORWARD | video
skip back | VIDEO_SEEK_BACKWARD | video
speed up | VIDEO_SPEED_UP | video
slow down | VIDEO_SPEED_DOWN | video
normal speed | VIDEO_SPEED_NORMAL | video
mute video | VIDEO_TOGGLE_MUTE | video
toggle loop | VIDEO_TOGGLE_LOOP | video
fullscreen | VIDEO_TOGGLE_FULLSCREEN | video
open gallery | VIDEO_OPEN_GALLERY | video
```

Locale translations required:

| Locale | File |
|--------|------|
| en-US | `en-US.app.vos` (primary — write first) |
| es-ES | `es-ES.app.vos` |
| fr-FR | `fr-FR.app.vos` |
| de-DE | `de-DE.app.vos` |
| hi-IN | `hi-IN.app.vos` |

---

## Phase 5: Cockpit Integration

Cockpit already has `FrameContent.Video` wired in `FrameContent.kt`. The `ContentRenderer.kt`
branch for `FrameContent.Video` needs to be updated from a stub to a real call.

### 5.1 ContentRenderer.kt — Update Video branch

Replace the existing stub `FrameContent.Video -> { /* TODO */ }` with:

```kotlin
is FrameContent.Video -> {
    val controller = remember { AndroidVideoController(LocalContext.current) }
    LaunchedEffect(content.videoItem) {
        controller.load(content.videoItem)
    }
    VideoPlayer(
        controller = controller,
        modifier = Modifier.fillMaxSize()
    )
    DisposableEffect(Unit) { onDispose { controller.release() } }
}
```

### 5.2 ContentAccent.kt — Video color

```kotlin
FrameContent.Video::class -> AvanueTheme.colors.error   // red/media convention
```

This follows the "media = red" color convention used in most media players (YouTube, VLC,
Apple TV). `AvanueTheme.colors.error` is the closest semantic token without introducing a new
custom accent.

### 5.3 FrameWindow.kt — Icon

```kotlin
FrameContent.Video::class -> Icons.Default.PlayCircle
```

---

## File Inventory

### Files to Create (11 Kotlin + 5 VOS)

| File | Source Set | Module | Status |
|------|-----------|--------|--------|
| `PlaybackSpeed.kt` | commonMain | VideoAvanue | NEW |
| `IVideoController.kt` | commonMain | VideoAvanue | NEW |
| `AndroidVideoController.kt` | androidMain | VideoAvanue | NEW |
| `VideoPlayer.kt` | androidMain | VideoAvanue | NEW |
| `VideoControlBar.kt` | androidMain | VideoAvanue | NEW |
| `VideoThumbnailCard.kt` | androidMain | VideoAvanue | NEW |
| `VideoGalleryScreen.kt` | androidMain | VideoAvanue | NEW |
| `DesktopVideoController.kt` | desktopMain | VideoAvanue | NEW |
| `DesktopVideoPlayer.kt` | desktopMain | VideoAvanue | NEW |
| `VideoCommandHandler.kt` | androidMain | VoiceOSCore | NEW |
| `en-US.app.vos` | — | VoiceOSCore/assets | MODIFY (add VIDEO block) |
| `es-ES.app.vos` | — | VoiceOSCore/assets | MODIFY |
| `fr-FR.app.vos` | — | VoiceOSCore/assets | MODIFY |
| `de-DE.app.vos` | — | VoiceOSCore/assets | MODIFY |
| `hi-IN.app.vos` | — | VoiceOSCore/assets | MODIFY |

### Files to Modify (6 Kotlin)

| File | Module | Change |
|------|--------|--------|
| `CommandActionType.kt` | VoiceOSCore | Add 12 VIDEO_* entries |
| `ActionCategory.kt` | VoiceOSCore | Add VIDEO(priority=17) |
| `AndroidHandlerFactory.kt` | VoiceOSCore | Register VideoCommandHandler |
| `VoiceOSAccessibilityService.kt` | VoiceOSCore | Create + hold AndroidVideoController singleton |
| `ContentRenderer.kt` | Cockpit | Replace Video stub with real player |
| `ContentAccent.kt` | Cockpit | Add Video color mapping |
| `FrameWindow.kt` | Cockpit | Add Video icon mapping |
| `build.gradle.kts` | VideoAvanue | Add desktopMain JavaFX dependencies |

---

## Dependencies

### Already Present (build.gradle.kts)

| Library | Version | Scope |
|---------|---------|-------|
| `androidx.media3:media3-exoplayer` | via libs.versions | androidMain |
| `androidx.media3:media3-ui` | via libs.versions | androidMain |
| `androidx.media3:media3-session` | via libs.versions | androidMain |
| `compose.ui` + `compose.material3` | BOM | androidMain |

### To Add

| Library | Version | Scope |
|---------|---------|-------|
| `org.openjfx:javafx-media` | 21 | desktopMain |
| `org.openjfx:javafx-swing` | 21 | desktopMain |

No new androidMain dependencies required.

---

## Implementation Order

Work in dependency order to avoid integration blockers:

```
1. commonMain models: PlaybackSpeed.kt + IVideoController.kt
2. androidMain controller: AndroidVideoController.kt
3. androidMain UI: VideoControlBar.kt → VideoPlayer.kt → VideoThumbnailCard.kt → VideoGalleryScreen.kt
4. VoiceOS cross-module: CommandActionType (VIDEO_*) + ActionCategory (VIDEO)
5. VoiceOS handler: VideoCommandHandler.kt + AndroidHandlerFactory update
6. VOS files: en-US.app.vos first, then 4 locale files
7. Cockpit wiring: ContentRenderer + ContentAccent + FrameWindow
8. desktopMain: build.gradle.kts update → DesktopVideoController.kt → DesktopVideoPlayer.kt
```

Desktop is last because JavaFX dependency requires build verification and is lower priority than
Android + Cockpit. The Cockpit runs on Android, so Desktop video is a secondary target.

---

## Testing Checklist

### Unit Tests

| Test | File | What |
|------|------|------|
| PlaybackSpeed.next() cycles correctly | PlaybackSpeedTest.kt | DOUBLE.next() == HALF |
| PlaybackSpeed.previous() cycles | PlaybackSpeedTest.kt | HALF.previous() == DOUBLE |
| PlaybackSpeed.fromMultiplier(1.3f) == ONE_AND_QUARTER | PlaybackSpeedTest.kt | nearest match |
| VideoItem.durationFormatted HH:MM:SS | VideoItemTest.kt | 3661000ms → "1:01:01" |
| VideoItem.durationFormatted M:SS | VideoItemTest.kt | 90000ms → "1:30" |
| VideoPlayerState.progress range | VideoPlayerStateTest.kt | 0..1 never exceeds |

### Integration Tests (Android)

| Test | What |
|------|------|
| AndroidVideoController.load() sets durationMs | ExoPlayer prepares successfully |
| play() → state.isPlaying == true | State flow updates |
| seekForward(10000) → positionMs increases | Position tracking |
| setSpeed(DOUBLE) → ExoPlayer playbackSpeed == 2.0f | Speed propagation |
| toggleMute() → volume 0f, toggleMute() again → volume restored | Mute/unmute cycle |

### Manual / UI Verification

| Check | Expected |
|-------|----------|
| VideoPlayer renders in Cockpit frame | No blank screen, player surface visible |
| Controls auto-hide after 3 seconds | Control bar fades, tap to show |
| Speed chips highlight active speed | Selected chip shows `AvanueTheme.colors.primary` |
| Voice "skip forward" → seeks 10s | Player position advances 10s |
| Voice "play pause" → toggles | State alternates on each utterance |
| Fullscreen hides system bars | Immersive mode, bars hidden |
| Loop indicator changes on "toggle loop" | Icon changes to show repeat state |

---

## KMP Score Projection

| Source Set | Files | Features |
|-----------|------:|---------|
| commonMain | 4 | VideoItem, VideoPlayerState, RepeatMode, PlaybackSpeed, IVideoController |
| androidMain | 7 | AndroidVideoController, VideoPlayer, VideoControlBar, VideoThumbnailCard, VideoGalleryScreen, VideoCommandHandler |
| desktopMain | 2 | DesktopVideoController, DesktopVideoPlayer |

KMP Score = shared feature areas / total feature areas.

Shared: model layer (VideoItem/State/RepeatMode/PlaybackSpeed) + controller interface (IVideoController).
Platform-specific: Player UI (different renderer API), Gallery (MediaStore vs FileSystem), Desktop bridge.

**Estimated KMP Score: 42%** — models and interface are shared; UI and player backends are platform-specific.
This is appropriate for a media module: the player surface rendering is fundamentally platform-native.

---

## Related Documentation

| Document | Path |
|----------|------|
| Session Handover (research phase) | `Docs/handover/handover-260219-0100.md` |
| NoteAvanue plan (reference implementation) | `docs/plans/NoteAvanue/` |
| Developer Manual Chapter 98 (PhotoAvanue KMP Camera) | `Docs/MasterDocs/PhotoAvanue/` |
| VOS Compact Format plan | `docs/plans/VoiceOSCore/VoiceOSCore-Plan-VOSCompactFormat-260216-V1.md` |
| Cockpit SpatialVoice plan | `Docs/Plans/Cockpit/Cockpit-Plan-SpatialVoiceRedesign-260217-V1.md` |
| Handler Dispatch (Chapter 95) | `Docs/MasterDocs/NewAvanues-Developer-Manual/` |

---

## Next Steps After Implementation

1. **Developer Manual Chapter 101** — VideoAvanue architecture, IVideoController API reference,
   voice command table, Cockpit integration guide
2. **iOS target** — `AVPlayer`-based `IosVideoController` once `iosMain` is added to `build.gradle.kts`
3. **Subtitle rendering** — `VideoItem.subtitleUri` is already modeled; ExoPlayer supports
   `MediaItem.SubtitleConfiguration` for SRT/WebVTT
4. **Media3 Transformer** — clip trimming, speed-change export (post-MVP, separate plan)
5. **Coil 3 KMP** — replace `ThumbnailUtils` with `AsyncImage` once Coil is standardised

---

*Author: Manoj Jhawar | NewAvanues | 2026-02-19*

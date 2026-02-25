# VideoAvanue Fix: ExoPlayer Empty URI Crash

## Crash Signature
`ExoPlaybackException: FileDataSource$FileDataSourceException: java.io.FileNotFoundException: :`

## Root Cause
`FrameContent.Video()` defaults `uri = ""`. All 4 code paths that create video frames (Cockpit dashboard, voice command, CommandBar, deserialization) use this empty default without wiring in `VideoGalleryScreen`. ExoPlayer receives `Uri.parse("")` -> tries to open file at path `""` -> ENOENT crash.

## Fix — Two-Layer Defense

### Layer 1: ContentRenderer.kt (crash prevention + UX improvement)
- At `is FrameContent.Video` branch, added `content.uri.isBlank()` check
- Blank URI -> shows `VideoGalleryScreen` (MediaStore gallery picker)
- User selects video -> `onContentStateChanged` updates frame with valid URI
- Non-blank URI -> existing `VideoPlayer` behavior unchanged

### Layer 2: VideoPlayer.kt (belt-and-suspenders)
- Added early-return guard at top of composable
- Blank URI -> shows centered placeholder (VideoLibrary icon + "No video selected")
- Prevents ExoPlayer from ever being initialized with empty URI regardless of caller

## Files Modified
| File | Change |
|------|--------|
| `Modules/Cockpit/src/androidMain/.../ContentRenderer.kt` | Added `VideoGalleryScreen` import + blank URI branch |
| `Modules/VideoAvanue/src/androidMain/.../VideoPlayer.kt` | Added `VideoLibrary` icon import + blank URI early-return guard |

## What Was NOT Changed
- `FrameContent.Video` model (empty default is now the "pick a video" state)
- `CockpitViewModel.launchModule()` (empty URI = gallery state by design)
- `VideoGalleryScreen.kt` (already complete)
- Database deserialization (empty URI fallback now shows gallery)

## Verification
1. Cockpit -> Add Video frame -> VideoGalleryScreen appears (not crash)
2. Tap video -> transitions to VideoPlayer
3. Voice command "add video" -> gallery picker (not crash)
4. Persisted empty-URI frame -> gallery on relaunch (not crash)

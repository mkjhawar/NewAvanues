# Cockpit Analysis: ActivityEmbedding & 3rd-Party App Support

**Date:** 2026-02-17
**Module:** Cockpit
**Type:** Research / Architecture Analysis

## Executive Summary

Android's security model **prevents non-system apps from embedding 3rd-party activities** within their own layout. ActivityEmbedding (Jetpack WindowManager) requires target app opt-in. VirtualDisplay and TaskOrganizer require system-level privileges. The viable architecture for Cockpit is: ActivityEmbedding for own modules + split-screen adjacent launch for 3rd-party apps + AccessibilityService voice overlay.

## ActivityEmbedding Overview

ActivityEmbedding (API 32+, cross-app API 33+) splits a single **task window** between two activities side-by-side. Key difference from multi-window: both activities share ONE task, and the host app controls the split ratio/direction.

### Key Classes

| Class | Purpose |
|-------|---------|
| `RuleController` | Manages split rules (replaces deprecated SplitController) |
| `SplitPairRule` | Defines when/how two activities split |
| `SplitPlaceholderRule` | Shows placeholder in secondary pane before content loads |
| `ActivityEmbeddingController` | `isActivityEmbedded()` detection |
| `EmbeddingAspectRatio` | Aspect ratio thresholds gating split behavior |
| `ActivityRule` | Excludes activities from all splits |

### Cross-App Embedding: The Hard Boundary

Cross-app embedding requires the TARGET app to explicitly opt in:

```xml
<!-- Option A: Trusted host (SHA-256 cert match) -->
<activity android:knownActivityEmbeddingCerts="@array/host_cert_digests" />

<!-- Option B: Allow any host (untrusted) -->
<activity android:allowUntrustedActivityEmbedding="true" />
```

Default is `false` for both. Most production apps (YouTube, Maps, Gmail, Chrome) do NOT set either. **They cannot be embedded.**

## Alternative Approaches Evaluated

| Approach | Viability | Why |
|----------|-----------|-----|
| ActivityEmbedding (own activities) | **HIGH** | Full control for Cockpit-owned modules |
| `FLAG_ACTIVITY_LAUNCH_ADJACENT` | **HIGH** | Launch 3rd-party into OS split-screen |
| `SYSTEM_ALERT_WINDOW` overlay | **HIGH** | Cockpit HUD over any foreground app |
| AccessibilityService | **HIGH** | Already implemented, voice control both panes |
| MediaProjection (capture) | **MEDIUM** | Read-only pixel stream, not interactive |
| VirtualDisplay + Presentation | **NOT VIABLE** | Requires system UID for cross-app |
| TaskOrganizer | **NOT VIABLE** | Requires platform signature |
| Picture-in-Picture | **NOT VIABLE** | Only one PiP at a time, floating overlay, not embedded |

## Recommended Architecture

### For Cockpit-Owned Modules (NoteAvanue, CameraAvanue, etc.)
Use **ActivityEmbedding** with `SplitPairRule` for full visual control within the Cockpit task. The Cockpit's own activities can be freely split, resized, and managed.

### For 3rd-Party Apps
Use **`FLAG_ACTIVITY_LAUNCH_ADJACENT`** to coordinate split-screen:

```kotlin
val intent = Intent(Intent.ACTION_MAIN)
intent.setComponent(ComponentName("com.target.app", "com.target.app.MainActivity"))
intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
startActivity(intent)
```

- Cockpit occupies one pane, 3rd-party app occupies the adjacent pane
- OS manages the split; host has no control over 3rd-party window dimensions
- VoiceOS AccessibilityService provides voice control over both panes
- Cockpit overlay provides persistent HUD layer on top

### Detection

```kotlin
// Check if currently in multi-window mode
if (activity.isInMultiWindowMode) {
    // Adapt Cockpit layout for split-screen
}
```

## API Level Requirements

| Feature | Minimum |
|---------|---------|
| Basic ActivityEmbedding (own activities) | API 32 (Android 12L) |
| Cross-app embedding (requires opt-in) | API 33 (Android 13) |
| Dynamic SplitAttributes calculator | WindowManager 1.1.0 |
| Draggable divider, activity stack pinning | API 35 (Android 15) |

## Model Extension for FrameContent

When implementing, add to `FrameContent`:

```kotlin
sealed class FrameContent {
    // ... existing types ...

    /** 3rd-party app frame — launched adjacent, controlled via Accessibility */
    data class ExternalApp(
        val packageName: String = "",
        val activityName: String = "",
        val label: String = ""
    ) : FrameContent()
}
```

## Conclusion

The "embedded frames containing arbitrary 3rd-party apps" model requires either OS-level access or explicit cooperation from every target app. The correct long-term architecture is:
- **Own modules**: Full embedding via ActivityEmbedding or Compose (current approach)
- **3rd-party apps**: Adjacent split-screen with voice overlay control
- **Future**: If VoiceOS ships as a system app (custom ROM / OEM partnership), TaskOrganizer becomes viable for true multi-app windowing

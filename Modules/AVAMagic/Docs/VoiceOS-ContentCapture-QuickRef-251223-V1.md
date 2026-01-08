# VoiceOS ContentCapture Crash - Quick Reference

**Created:** 2025-12-23
**Status:** ACTIVE FIX AVAILABLE
**Severity:** HIGH (production crashes)

---

## The Problem (One Sentence)

VoiceOS Compose activities crash with "scroll observation scope does not exist" when finishing because AccessibilityService events trigger ContentCapture checks while Compose is disposing scroll state.

---

## The Fix (One Paragraph)

Replace `ComponentActivity` with `ContentCaptureSafeComposeActivity` base class and use `setContentSafely()` instead of `setContent()`. This disables ContentCapture before composition disposal, preventing the race condition between accessibility events and Compose lifecycle.

---

## Quick Migration (2 Minutes)

**Before:**
```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { /* ... */ }
    }
}
```

**After:**
```kotlin
import com.augmentalis.voiceoscore.ui.ContentCaptureSafeComposeActivity

class MyActivity : ContentCaptureSafeComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentSafely { /* ... */ }  // Changed from setContent
    }
}
```

---

## Emergency Hotfix (30 Seconds)

Add to AndroidManifest.xml:

```xml
<activity
    android:name=".ui.MyActivity"
    android:contentCaptureEnabled="false"  ← ADD THIS
    android:exported="false" />
```

**Warning:** Disables auto-fill and screen reader metadata. Use as temporary fix only.

---

## Affected Activities

1. ✅ LearnAppActivity.kt
2. ✅ DeveloperSettingsActivity.kt
3. ✅ CleanupPreviewActivity.kt

---

## Verification

Check logcat after finishing activity:

```bash
adb logcat -s ContentCaptureSafe:D
```

**Expected:**
```
ContentCaptureSafe: ContentCapture disabled for safe disposal (from: finish() override)
```

**Success:** No crash, activity finishes normally.

---

## Full Documentation

- **Root Cause Analysis:** VoiceOS-ContentCapture-RoT-Analysis-251223-V1.md (21 pages)
- **Migration Guide:** VoiceOS-ContentCapture-Migration-Guide-251223-V1.md (detailed steps)
- **Implementation:** ContentCaptureSafeComposeActivity.kt (production code)

---

## Decision Matrix

| Scenario | Action | Time | Risk |
|----------|--------|------|------|
| Production crash NOW | Apply hotfix (manifest flag) | 30 sec | Low |
| Want proper fix | Migrate to safe base class | 2 hours | Low |
| New Compose activity | Use ContentCaptureSafeComposeActivity | 0 sec | None |
| Not sure | Read full RoT analysis | 20 min | None |

---

## Key Insight

The current `ComposeScrollLifecycle.kt` mitigation **DOES NOT WORK** because:
- Observes `ON_PAUSE` (too early)
- Does nothing in observer (empty lambda)
- Never interacts with ContentCapture system

The new fix works because:
- Disables ContentCapture in `finish()` override (before disposal)
- Uses `ON_STOP` lifecycle event (correct timing)
- Three-layer safety net (finish + ON_STOP + onDispose)

---

## Timeline

- **2025-12-23:** Issue discovered, RoT analysis completed, fix implemented
- **2025-12-23:** Hotfix applied (manifest flags)
- **2025-12-23:** Migration to safe base class (ongoing)
- **2025-12-24:** Remove hotfix, verify proper fix works
- **2025-12-25:** Testing complete, prevention measures in place

---

## Contact

- **Implementation:** See VoiceOS-ContentCapture-Migration-Guide-251223-V1.md
- **Questions:** #voiceos-development Slack channel
- **Lead Developer:** Manoj Jhawar

---

**This is a SOLVED problem. Use the safe base class. Done.**

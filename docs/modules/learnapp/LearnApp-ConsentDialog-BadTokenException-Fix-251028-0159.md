# LearnApp ConsentDialog BadTokenException Fix

**Date:** 2025-10-28 01:59 PDT
**Module:** LearnApp
**Component:** ConsentDialog
**Issue:** BadTokenException when clicking "Yes" on consent dialog  
**Status:** ✅ FIXED
**Root Cause:** Incorrect window type for AccessibilityService context

---

## Executive Summary

Fixed crash when user clicks "Yes" on LearnApp consent dialog. The issue was caused by using `TYPE_APPLICATION_OVERLAY` (Android O+) which requires an Activity token, but LearnApp runs in AccessibilityService context without an Activity. Changed to always use `TYPE_ACCESSIBILITY_OVERLAY` (API 22+) which is specifically designed for AccessibilityService overlays.

**Resolution:** Changed window type from `TYPE_APPLICATION_OVERLAY` to `TYPE_ACCESSIBILITY_OVERLAY` for all Android versions ≥ API 22.

---

## Problem Statement

### Error Stack Trace

```
android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?
    at android.view.ViewRootImpl.setView(ViewRootImpl.java:1652)
    at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:492)
    at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:168)
```

### Root Cause

On Android O+ (API 26+), the code was using `TYPE_APPLICATION_OVERLAY` which requires an Activity context with a valid window token. Since LearnApp runs as an AccessibilityService without an Activity, WindowManager.addView() failed with BadTokenException.

### Solution

Changed to always use `TYPE_ACCESSIBILITY_OVERLAY` for API 22+ devices. This window type is specifically designed for AccessibilityService context and doesn't require an Activity token.

---

## Code Changes

**File:** `modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt`

**Lines 162-169 (Window type selection):**

```kotlin
// BEFORE (broken on Android O+):
if (Build.VERSION.SDK_INT >= Build.VERSION.CODES.O) {
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY  // ❌ Requires Activity
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
} else {
    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
}

// AFTER (fixed):
if (Build.VERSION.SDK_INT >= Build.VERSION.CODES.LOLLIPOP_MR1) {
    // API 22+: TYPE_ACCESSIBILITY_OVERLAY for AccessibilityService
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY  // ✅ Correct
} else {
    // API 21 and below: Use deprecated TYPE_SYSTEM_ALERT
    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
}
```

---

## Testing

**Build Verification:** ✅ BUILD SUCCESSFUL

**Manual Testing Required:**
- [ ] Test on Android O+ (API 26+) - Click "Yes" button
- [ ] Test on Android 13/14 - Click "Yes" button  
- [ ] Verify no crash when approving learning
- [ ] Verify "No" button still works

---

**Last Updated:** 2025-10-28 01:59 PDT
**Status:** Code fixed, pending manual testing

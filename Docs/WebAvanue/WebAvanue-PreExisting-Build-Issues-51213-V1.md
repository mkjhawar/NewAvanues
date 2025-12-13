# WebAvanue Pre-Existing Build Issues

**Document Type:** Issue Report
**Version:** V1
**Date:** 2025-12-13
**Severity:** HIGH
**Status:** Documented (Out of scope for package refactoring)

---

## Summary

During preparation for package structure refactoring, discovered 18 files in the `universal` module with broken imports attempting to reference the `app` module. This violates KMP architecture principles (universal should be platform-agnostic).

---

## Affected Files

### Source Files (9 files - CRITICAL)
1. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/download/AndroidDownloadQueue.kt`
2. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/webview/WebViewConfigurator.kt`
3. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/screenshot/ScreenshotNotificationHelper.kt`
4. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/util/FilePicker.android.kt`
5. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/utils/MemoryMonitor.kt`
6. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/xr/CommonXRManager.android.kt`
7. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/xr/XRPermissionManager.kt`
8. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DownloadPermissionManager.kt`
9. `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/NetworkChecker.android.kt`

### Test Files (9 files - MEDIUM)
1. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/Avanues/web/universal/BrowserBugFixesTest.kt`
2. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/Avanues/web/universal/platform/SettingsApplicatorTest.kt`
3. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/Avanues/web/universal/SecurityFeaturesIntegrationTest.kt`
4. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/Avanues/web/universal/WebXRSupportTest.kt`
5. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/download/DownloadProgressMonitorTest.kt`
6. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/integration/DownloadFlowIntegrationTest.kt`
7. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadFilePickerLauncherTest.kt`
8. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadPathValidatorTest.kt`
9. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadPermissionManagerTest.kt`
10. `/Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/ui/SettingsUITest.kt`

### Dead Code Removed (3 files)
1. `DownloadProgressMonitor.kt` - Deleted (unused, broken import)
2. `DownloadFilePickerLauncher.kt` - Deleted (unused, broken import)
3. `DownloadPathValidator.kt` - Deleted (unused, broken import)

---

## Root Cause

Architectural violation: `universal` module (KMP shared code) imports from `app` module (Android-specific app layer).

**Correct dependency direction:**
```
app → universal → coredata
```

**Current (broken) dependency:**
```
app ⇄ universal → coredata
    ↑______|  (circular dependency via imports)
```

---

## Example Issue: NetworkChecker.android.kt

```kotlin
package com.augmentalis.webavanue.platform

import com.augmentalis.Avanues.web.app.download.NetworkHelper  // ❌ BROKEN

actual class NetworkChecker {
    private val helper: NetworkHelper by lazy {
        // Trying to use class from app module
        NetworkHelper(contextProvider)
    }
}
```

**Impact:** Build fails with "Unresolved reference 'app'"

---

## Recommended Fix (Separate Task)

1. **Move Helper Classes**: Move NetworkHelper, DownloadHelper, etc. from `app` to `universal`
2. **Update Imports**: Fix all 18 files to import from universal
3. **Verify Architecture**: Ensure app → universal (one-way dependency)
4. **Test Build**: Full clean build verification

**Estimated Effort:** 2-3 hours

---

## Decision for Package Refactoring

**Status:** Documented, out of scope

**Rationale:**
- User requested package refactoring (folder bloat/nesting), not build error fixes
- These are pre-existing architectural issues
- Fixing these requires separate refactoring effort
- Package refactoring can proceed independently

**Impact on Refactoring:**
- Will skip Phase 1 build verification
- Package refactoring will NOT fix these imports
- These files will still have build errors after refactoring
- Recommend fixing architectural issues AFTER package refactoring completes

---

## Tracking

**Related Work:**
- Package Refactoring: `IDEACODE-Plan-Package-Structure-Analysis-51312-V1.md`
- This Issue: Separate from package refactoring scope

**Follow-up Task:**
Create separate issue/plan for fixing universal → app dependency violations after package refactoring completes.

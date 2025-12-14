# WebAvanue Pre-Existing Build Issues

**Document Type:** Issue Report
**Version:** V1
**Date:** 2025-12-13
**Severity:** HIGH
**Status:** Partially Resolved (1/18 files fixed)

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

---

## Resolution Progress

### Fixed (1/18)

**File:** `/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/NetworkChecker.android.kt`

**Resolution:**
- Moved `NetworkHelper` from `android/apps/webavanue/app/.../download/` to `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/`
- Updated package from `com.augmentalis.webavanue.app.download` to `com.augmentalis.webavanue.platform`
- Removed broken import in NetworkChecker.android.kt (now same package)
- Deleted old NetworkHelper from app module
- Commit: `5e8ed7b2 fix(webavanue): resolve architectural violation - move NetworkHelper to universal`

**Verification:** ✓ No app module import errors remain for NetworkChecker

### Remaining (17/18)

**Source Files:** 8 files still have app module import violations
**Test Files:** 9 files still have app module import violations

See "Affected Files" section above for complete list.

---

## Additional Build Errors Discovered

During build verification, discovered 4 additional errors (NOT related to app module imports):

### Error 1: BookmarkImportExport - Wrong Package Import
**File:** `FavoriteViewModel.kt`
**Issue:** Imports from `com.augmentalis.webavanue.ui.util` but class is in `com.augmentalis.webavanue.util`
**Status:** FIXED (2025-12-14)
**Fix:** Updated imports in FavoriteViewModel.kt from `com.augmentalis.webavanue.ui.util.BookmarkImportExport` to `com.augmentalis.webavanue.util.BookmarkImportExport`

### Error 2: encodeUrl - Wrong Package Import
**File:** `TabViewModel.kt`
**Issue:** Imports from `com.augmentalis.webavanue.ui.util` but function is in `com.augmentalis.webavanue.util`
**Status:** FIXED (2025-12-14)
**Fix:** Updated import in TabViewModel.kt from `com.augmentalis.webavanue.ui.util.encodeUrl` to `com.augmentalis.webavanue.util.encodeUrl`

### Error 3: screenshot - Wrong Package Import
**File:** `TabViewModel.kt`
**Issue:** Imports from `com.augmentalis.webavanue.screenshot` but classes are in `com.augmentalis.webavanue.feature.screenshot`
**Status:** FIXED (2025-12-14)
**Fix:** Added imports for `ScreenshotType` and `ScreenshotData` from `com.augmentalis.webavanue.feature.screenshot` and updated fully-qualified names to use imported classes

### Error 4: ValidationResult - Class Name Collision
**File:** `ValidationResult.kt`
**Issue:** Two classes named `ValidationResult` in different packages cause redeclaration error
**Status:** FIXED (2025-12-14)
**Fix:**
- Renamed `com.augmentalis.webavanue.platform.ValidationResult` → `DownloadValidationResult`
- Renamed `com.augmentalis.webavanue.domain.validation.UrlValidation.ValidationResult` → `UrlValidationResult`
- Removed duplicate `ValidationResult` definition from `DownloadPathValidator.kt`
- Updated all usages across ViewModel, UI, and validation files

**Note:** These are separate from the architectural violations documented above and require different fixes (import path corrections).

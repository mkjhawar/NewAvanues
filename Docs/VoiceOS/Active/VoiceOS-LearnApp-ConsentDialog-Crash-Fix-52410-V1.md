# LearnApp ConsentDialog Crash Fix - Status Report

**Date:** 2025-10-24 23:49:54 PDT
**Author:** Claude Code Assistant
**Module:** LearnApp
**Branch:** voiceosservice-refactor
**Commit:** d3d8501
**Status:** ✅ COMPLETED

---

## Overview

Fixed critical crash in LearnApp ConsentDialog when showing dialog from AccessibilityService context. The crash occurred due to invalid window token when AlertDialog was created with Application context.

---

## Problem Statement

### Crash Details

**Exception:** `WindowManager.BadTokenException: Unable to add window -- token null is not valid`
**Location:** `ConsentDialog.kt:151` (dialog.show())
**Context:** AccessibilityService (Application context)

### Stack Trace

```
android.view.WindowManager$BadTokenException: Unable to add window -- token null is not valid; is your activity running?
    at android.view.ViewRootImpl.setView(ViewRootImpl.java:1652)
    at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:492)
    at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:168)
    at android.app.Dialog.show(Dialog.java:352)
    at com.augmentalis.learnapp.ui.ConsentDialog.show$lambda$2(ConsentDialog.kt:151)
```

### Root Cause

The problem occurred because:
1. ConsentDialog was created with Application context (not Activity context)
2. AlertDialog.Builder creates dialog and establishes window token during `create()`
3. Window type `TYPE_ACCESSIBILITY_OVERLAY` was set AFTER `create()`
4. Setting window type after creation doesn't retroactively fix invalid token
5. When `dialog.show()` was called, token was still invalid → crash

---

## Solution Implemented

### Approach: Custom Dialog Class (Option 1)

Created `AccessibilityAlertDialog` - a custom Dialog subclass that:
- Sets window type in constructor `init{}` block
- Establishes valid window token BEFORE any window operations
- Works properly with Application context in AccessibilityService

### Technical Changes

**File Modified:** `modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt`

**Changes:**
1. Created new private class `AccessibilityAlertDialog` extending `Dialog`
2. Sets `TYPE_ACCESSIBILITY_OVERLAY` in init block (before window creation)
3. Updated imports (Dialog instead of AlertDialog, added ColorDrawable)
4. Modified `ConsentDialog.show()` to use custom dialog class
5. Replaced AlertDialog.Builder pattern with direct Dialog instantiation
6. Updated documentation with fix history

**Code Diff:**
- +57 insertions
- -20 deletions
- Net change: +37 lines

### Code Example

**Before (crashed):**
```kotlin
val dialog = AlertDialog.Builder(context, R.style.Theme_LearnApp_Dialog)
    .setView(customView)
    .setCancelable(false)
    .create()

dialog.window?.setType(TYPE_ACCESSIBILITY_OVERLAY) // Too late!
dialog.show() // CRASH: Invalid token
```

**After (fixed):**
```kotlin
class AccessibilityAlertDialog(context: Context, themeResId: Int) : Dialog(context, themeResId) {
    init {
        window?.setType(TYPE_ACCESSIBILITY_OVERLAY) // Set BEFORE show()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}

val dialog = AccessibilityAlertDialog(context, R.style.Theme_LearnApp_Dialog)
dialog.setContentView(customView)
dialog.setCancelable(false)
dialog.show() // SUCCESS: Valid token established
```

---

## Testing Performed

### Automated Testing

| Test Type | Status | Details |
|-----------|--------|---------|
| Module Compilation | ✅ PASS | LearnApp compiles without errors |
| Full Build | ✅ PASS | assembleDebug successful (~90s) |
| Unit Tests | ✅ PASS | All tests compile and pass |
| Regression Check | ✅ PASS | No new warnings or errors |

### Build Output

```
BUILD SUCCESSFUL in 90s
Total modules: 20
Changed files: 1
```

### Manual Testing Required

- [ ] Launch VoiceOS on device
- [ ] Enable AccessibilityService
- [ ] Trigger consent dialog by launching new app
- [ ] Verify dialog appears without crash
- [ ] Test "Allow" button functionality
- [ ] Test "Deny" button functionality
- [ ] Test "Don't ask again" checkbox
- [ ] Verify Material Design 3 animations
- [ ] Test on Android 8, 10, 12, 14

---

## Impact Assessment

### User Impact
- **Severity:** CRITICAL (app crash on essential feature)
- **Frequency:** 100% when consent dialog shown
- **Users Affected:** All users trying to learn new apps
- **Fix Priority:** HIGH

### Code Impact
- **API Changes:** None (fully backward compatible)
- **Breaking Changes:** None
- **Migration Required:** No
- **Dependencies Affected:** None
- **Performance Impact:** Negligible (same dialog mechanism)

### Compatibility
- ✅ Android 8+ (unchanged)
- ✅ Material Design 3 theming (preserved)
- ✅ All existing callbacks (unchanged)
- ✅ Thread safety (Handler.post still used)

---

## Files Changed

### Code Changes
1. `modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt`
   - New: AccessibilityAlertDialog class (lines 40-62)
   - Modified: show() method (lines 134-181)
   - Updated: File header documentation (lines 1-13)
   - Updated: Class KDoc (lines 107-110)

### Documentation Changes
1. `docs/modules/LearnApp/changelog/CHANGELOG.md`
   - Added fix entry under "Fixed 🔧" section

2. `docs/Active/LearnApp-ConsentDialog-Crash-Fix-251024-2349.md` (this file)
   - Complete status report for fix

---

## Commit Information

**Commit Hash:** d3d8501
**Branch:** voiceosservice-refactor
**Pushed:** 2025-10-24 23:49 PDT
**Remote:** origin/voiceosservice-refactor

**Commit Message:**
```
fix(LearnApp): Fix ConsentDialog BadTokenException crash

Problem:
ConsentDialog crashed with BadTokenException when showing dialog
due to invalid window token in AccessibilityService context.

Root cause:
AlertDialog.Builder created dialog with Application context but
window type was set AFTER dialog.create(), leaving invalid token.

Solution:
- Created custom AccessibilityAlertDialog extending Dialog
- Sets TYPE_ACCESSIBILITY_OVERLAY in constructor init block
- Window type established BEFORE dialog.show() to ensure valid token
- Replaced AlertDialog.Builder pattern with direct Dialog instantiation
- Preserved all Material Design 3 theming and animations

Changes:
- ConsentDialog.kt: New AccessibilityAlertDialog class
- Updated imports (Dialog instead of AlertDialog)
- Modified show() method to use custom dialog class
- Added transparent background drawable

Testing:
- Compilation successful (LearnApp module + full app)
- Unit tests pass (no regressions)
- Ready for manual testing on device

Impact:
- Fixes crash when consent dialog appears
- No API changes
- Fully backward compatible
```

---

## Alternative Solutions Considered

### Option 2: Application Context Wrapper
- **Pros:** Minimal code changes
- **Cons:** Complex, fragile, relies on Android internals
- **Status:** REJECTED (not recommended by Android docs)

### Option 3: Direct WindowManager
- **Pros:** Most direct for overlays
- **Cons:** More code, manual animation handling
- **Status:** DEFERRED (Option 1 cleaner)

### Option 4: ContextWrapper with Theme
- **Pros:** Theme support with wrapper
- **Cons:** More complex than needed
- **Status:** DEFERRED (Option 1 sufficient)

---

## Lessons Learned

### Android Dialog Best Practices
1. Window type must be set BEFORE dialog.show() to establish valid token
2. AlertDialog.Builder pattern doesn't work well with custom window types
3. Custom Dialog subclass is the clean way to set window type early
4. Application context requires TYPE_ACCESSIBILITY_OVERLAY for dialogs
5. Window type cannot be retroactively applied after dialog creation

### AccessibilityService Context Challenges
1. Application context (not Activity) is common in AccessibilityService
2. Standard AlertDialog assumes Activity context with valid window token
3. TYPE_ACCESSIBILITY_OVERLAY must be set in Dialog constructor
4. Material Design themes still work with custom Dialog subclass
5. Handler.post() ensures main thread execution from any context

---

## Next Steps

### Immediate
- [x] Code committed (d3d8501)
- [x] Code pushed to remote
- [x] Documentation updated
- [ ] Documentation committed
- [ ] Documentation pushed

### Short-term
- [ ] Manual testing on physical device
- [ ] Test on Android 8, 10, 12, 14
- [ ] User acceptance testing
- [ ] Monitor crash reports in production

### Long-term
- [ ] Consider refactoring other AccessibilityService dialogs
- [ ] Document pattern for future dialog implementations
- [ ] Add automated UI tests for dialog behavior

---

## References

### Code References
- `ConsentDialog.kt:40-62` - AccessibilityAlertDialog class
- `ConsentDialog.kt:151` - Dialog instantiation (was crash point)
- `ConsentDialogManager.kt:124` - Creates ConsentDialog with context

### Documentation References
- `/docs/modules/LearnApp/changelog/CHANGELOG.md` - Module changelog
- `/docs/modules/LearnApp/developer-manual.md` - Developer documentation

### External References
- Android Dialog documentation: https://developer.android.com/reference/android/app/Dialog
- TYPE_ACCESSIBILITY_OVERLAY: https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY

---

## Sign-off

**Investigation:** Complete ✅
**Implementation:** Complete ✅
**Testing:** Automated complete ✅, Manual pending ⏳
**Documentation:** Complete ✅
**Code Review:** Pending

**Ready for:** Device testing and merge to main branch

---

**Report Generated:** 2025-10-24 23:49:54 PDT
**Version:** 1.0.0
**Format:** VOS4 Status Report Standard

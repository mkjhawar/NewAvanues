# Bug Report: Activity State Serialization Crash

**Date**: 2025-11-23
**Reporter**: Claude Code (Automated Testing)
**Severity**: Critical
**Status**: New
**Module**: WebAvanue Browser

---

## Summary

App crashes when Home button is pressed due to failed serialization of Voyager Navigator state. The crash occurs during activity state saving when Android tries to serialize non-parcelable ViewModels contained in the BrowserScreenNav object.

---

## Environment

- **Device**: Pixel 9 Emulator (AVD)
- **Android Version**: 15 (API level 35)
- **App Version**: Debug build
- **Package**: com.augmentalis.Avanues.web.debug

---

## Steps to Reproduce

1. Launch WebAvanue browser app
2. Open a webpage (e.g., example.com)
3. Press Home button (keycode 3)
4. **Result**: App crashes with BadParcelableException

**Expected**: App should minimize to background without crashing

---

## Crash Details

### Exception Type
```
android.os.BadParcelableException: Parcelable encountered IOException writing serializable object
```

### Stack Trace
```
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: Process: com.augmentalis.Avanues.web.debug, PID: 3850
E AndroidRuntime: android.os.BadParcelableException: Parcelable encountered IOException writing serializable object
                  (name = com.augmentalis.Avanues.web.universal.presentation.navigation.BrowserScreenNav)
E AndroidRuntime: 	at android.os.Parcel.writeSerializable(Parcel.java:2966)
E AndroidRuntime: 	at android.os.Parcel.writeValue(Parcel.java:2732)
...
E AndroidRuntime: 	at android.app.servertransaction.PendingTransactionActions$StopInfo.collectBundleStates(...)
E AndroidRuntime: 	at android.app.servertransaction.PendingTransactionActions$StopInfo.run(...)
```

### Root Cause Analysis

**File**: `BrowserScreenNav.kt` (location to be verified)
**Issue**: Voyager Navigator attempts to serialize screen state when activity stops
**Problem**: BrowserScreenNav contains ViewModels which are not Parcelable/Serializable

**Chain of Events**:
1. User presses Home button
2. Android calls `onStop()` on MainActivity
3. Activity state saving mechanism tries to persist Navigator state
4. Voyager attempts to serialize BrowserScreenNav object
5. BrowserScreenNav contains ViewModelHolder with ViewModels
6. ViewModels are not serializable → IOException → BadParcelableException

---

## Impact

- **Critical**: App becomes unusable after Home button press
- **User Experience**: Users cannot multitask or switch apps
- **Data Loss**: Potential loss of browsing state/tabs
- **Frequency**: 100% reproduction rate

---

## Related Code

### Affected Components

1. **BrowserScreenNav** - Voyager Screen containing ViewModels
   ```kotlin
   // File: BrowserScreenNav.kt (expected location)
   class BrowserScreenNav(
       private val viewModels: ViewModelHolder  // ← NOT SERIALIZABLE
   ) : Screen { ... }
   ```

2. **ViewModelHolder** - Container for all ViewModels
   ```kotlin
   data class ViewModelHolder(
       val tabViewModel: TabViewModel,        // NOT SERIALIZABLE
       val settingsViewModel: SettingsViewModel,
       val historyViewModel: HistoryViewModel,
       val favoriteViewModel: FavoriteViewModel,
       val downloadViewModel: DownloadViewModel
   )
   ```

3. **MainActivity** - Activity hosting Voyager Navigator
   ```kotlin
   // File: MainActivity.kt:40-49
   setContent {
       BrowserApp(
           repository = repository!!,
           modifier = Modifier.fillMaxSize()
       )
   }
   ```

---

## Proposed Solutions

### Option 1: Disable Voyager State Saving (Quick Fix)
**Pros**: Immediate fix, no architectural changes
**Cons**: Loses navigation state on configuration changes

```kotlin
Navigator(
    screen = BrowserScreenNav(viewModels),
    disposeBehavior = NavigatorDisposeBehavior(
        disposeSteps = false,
        disposeNestedNavigators = false
    )
) { navigator ->
    SlideTransition(navigator)
}
```

### Option 2: Use Parcelize for ViewModelHolder (Proper Fix)
**Pros**: Maintains state across configuration changes
**Cons**: Requires ViewModels to be recreated from parcelable state

```kotlin
@Parcelize
data class ViewModelState(
    val activeTabId: String?,
    // ... other serializable state
) : Parcelable

@Parcelize
class BrowserScreenNav(
    private val viewModelState: ViewModelState
) : Screen, Parcelable { ... }
```

### Option 3: Use AndroidX ViewModel (Recommended)
**Pros**: Proper lifecycle management, survives configuration changes
**Cons**: Requires refactoring to use Android ViewModel architecture

```kotlin
class MainActivity : ComponentActivity() {
    private val tabViewModel: TabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrowserApp(
                repository = repository!!,
                // Pass ViewModels from Activity scope
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### Option 4: Mark Navigator as Non-Retainable (Temporary Fix)
**Pros**: Prevents serialization attempts
**Cons**: Navigator state is lost on Home button press

```kotlin
// In MainActivity
override fun onSaveInstanceState(outState: Bundle) {
    // Skip saving Voyager state
    super.onSaveInstanceState(Bundle())
}
```

---

## Recommendation

**Immediate**: Use **Option 4** (Non-Retainable) as hotfix
**Long-term**: Implement **Option 3** (AndroidX ViewModel) for proper architecture

---

## Testing Plan

### Test Cases
1. ✅ Verify app doesn't crash on Home button press
2. ✅ Verify app restores correctly when reopened
3. ✅ Verify WebView state is preserved
4. ✅ Verify tab state is preserved
5. ✅ Verify browsing session continues after background/foreground

### Test Scenarios
- Home button during page load
- Home button with multiple tabs open
- Home button while typing in address bar
- Home button after long browsing session
- Screen rotation while backgrounded

---

## Related Issues

- **Original Issue #4**: "App crashes when Home button pressed"
  - **Status**: Partially fixed (WebView lifecycle now handled)
  - **Remaining**: State serialization crash (this bug)

---

## Timeline

- **Discovered**: 2025-11-23 01:29:17 UTC
- **Automated Test**: BrowserBugFixesTest.kt
- **First Occurrence**: During emulator testing
- **Fix Priority**: P0 (Critical - blocks basic functionality)

---

## Additional Notes

- This bug was discovered during automated testing of the WebView lifecycle fixes
- The WebView lifecycle management (pause/resume) is working correctly
- This is a separate issue related to Android's activity state saving mechanism
- Affects all Android versions that use Voyager navigation
- May also occur on screen rotation, app process death, or low memory situations

---

## Files to Modify

1. `BrowserApp.kt` - Update Navigator configuration
2. `BrowserScreenNav.kt` - Make serializable or remove from Navigator state
3. `ViewModelHolder.kt` - Create parcelable state representation
4. `MainActivity.kt` - Add state saving override (temporary)

---

**End of Report**

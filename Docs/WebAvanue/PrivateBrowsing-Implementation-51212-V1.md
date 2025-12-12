# Private Browsing Mode - Implementation Guide

**App**: WebAvanue
**Date**: 2025-12-12
**Version**: V1
**Status**: Implementation Complete

---

## Overview

Private/Incognito browsing mode has been implemented for WebAvanue with full data isolation and no history tracking.

---

## Implementation Components

### 1. Domain Layer

#### PrivateBrowsingManager.kt
- **Location**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/manager/`
- **Purpose**: Manages private tab sessions and lifecycle
- **Features**:
  - Tracks active private tabs
  - Session isolation
  - Cleanup management
  - Thread-safe operations

#### Tab Model
- **Location**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/Tab.kt`
- **Field**: `isIncognito: Boolean` (already exists)
- **Database**: `is_incognito` column in `tab` table

---

### 2. Data Layer

#### BrowserRepositoryImpl.kt
- **Location**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/repository/`
- **Changes**:
  - `addHistoryEntry()` - Skips history for incognito tabs
  - Checks `entry.isIncognito` flag before persisting

---

### 3. Presentation Layer

#### TabViewModel.kt
- **Location**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/`
- **New Fields**:
  - `privateBrowsingManager: PrivateBrowsingManager`
  - `isPrivateModeActive: StateFlow<Boolean>`
  - `privateTabCount: StateFlow<Int>`

- **New Methods**:
  - `createTab()` - Added `isIncognito` parameter
  - `createPrivateTab()` - Convenience method for private tabs
  - `closeTab()` - Unregisters from private browsing manager
  - Extension file: `TabViewModelPrivateBrowsing.kt` with helper methods

---

### 4. Platform Layer (Android)

#### WebViewLifecycle.kt
- **Location**: `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/webview/`
- **Changes**:
  - `removeWebView()` - Added `isPrivate` parameter
  - Clears cache, cookies, form data, and history for private tabs

---

## Private Mode Guarantees

When a tab has `isIncognito = true`:

1. ✅ **No History**: `addHistoryEntry()` skips database insertion
2. ✅ **No Cookies**: WebView cookies cleared on tab close
3. ✅ **No Cache**: WebView cache cleared on tab close
4. ✅ **No Form Data**: Form data cleared on tab close
5. ✅ **Isolated Session**: Separate tracking via PrivateBrowsingManager
6. ⚠️ **Downloads Still Work**: But download history not recorded

---

## UI Integration (To Be Implemented)

### Required UI Components

#### 1. Private Tab Indicator
```kotlin
@Composable
fun PrivateTabIndicator(isIncognito: Boolean) {
    if (isIncognito) {
        Row {
            Icon(Icons.Default.Incognito, "Private")
            Text("Private", color = Color.Gray)
        }
    }
}
```

#### 2. New Private Tab Action
```kotlin
// In browser toolbar or tab switcher
IconButton(onClick = { viewModel.createPrivateTab() }) {
    Icon(Icons.Default.Incognito, "New Private Tab")
}
```

#### 3. Private Mode Banner
```kotlin
@Composable
fun PrivateBanner() {
    Surface(color = Color.DarkGray) {
        Row {
            Icon(Icons.Default.Incognito)
            Text("You're in private mode")
            Text("History and cookies won't be saved")
        }
    }
}
```

#### 4. Close All Private Tabs Button
```kotlin
// In tab switcher
if (viewModel.isPrivateModeActive.value) {
    Button(onClick = { viewModel.closeAllPrivateTabs() }) {
        Text("Close All Private Tabs")
    }
}
```

#### 5. Visual Distinction
- Dark theme by default for private tabs
- Different background color
- Incognito icon in tab
- Gray/muted colors

---

## API Usage Examples

### Create Private Tab
```kotlin
// Method 1: Using createPrivateTab()
viewModel.createPrivateTab(
    url = "https://example.com",
    title = "Private Browsing",
    setActive = true
)

// Method 2: Using createTab() with isIncognito flag
viewModel.createTab(
    url = "https://example.com",
    isIncognito = true
)
```

### Close All Private Tabs
```kotlin
// Using extension method
viewModel.closeAllPrivateTabs()
```

### Check Private Mode Status
```kotlin
// Collect state flows
val isPrivateMode by viewModel.isPrivateModeActive.collectAsState()
val privateTabCount by viewModel.privateTabCount.collectAsState()

if (isPrivateMode) {
    Text("$privateTabCount private tabs open")
}
```

### Filter Tabs by Type
```kotlin
// Using extension methods
val privateTabs = viewModel.getPrivateTabs()
val regularTabs = viewModel.getRegularTabs()
```

---

## Testing Checklist

- [ ] Create private tab → verify `isIncognito = true`
- [ ] Navigate in private tab → verify no history entries created
- [ ] Close private tab → verify cookies cleared
- [ ] Close private tab → verify cache cleared
- [ ] Close all private tabs → verify all cleanup occurs
- [ ] Multiple private tabs → verify isolation
- [ ] Regular + private tabs → verify separation
- [ ] App restart → verify private tabs don't persist (if desired)

---

## Build Requirements

### Gradle Configuration
No changes needed - uses existing SQLDelight schema.

### Dependencies
All dependencies already present:
- kotlinx.coroutines
- kotlinx.datetime
- SQLDelight

---

## Next Steps

1. **UI Implementation**: Add visual indicators and controls
2. **Testing**: Comprehensive testing of all scenarios
3. **Documentation**: User-facing documentation
4. **Settings**: Add private browsing settings (default behavior, etc.)

---

## Related Files

### Core Implementation
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/manager/PrivateBrowsingManager.kt`
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/Tab.kt`
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/repository/BrowserRepositoryImpl.kt`

### Presentation
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/TabViewModel.kt`
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/TabViewModelPrivateBrowsing.kt`

### Platform
- `/Volumes/M-Drive/Coding/NewAvanues-WebAvanue/Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/webview/WebViewLifecycle.kt`

---

**Implementation Complete**: Core functionality ready for UI integration and testing.

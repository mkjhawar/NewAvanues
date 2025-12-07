# WebAvanue Browser Bug Fix Plan

**Version:** 1.1
**Date:** 2025-12-01
**Source:** FEATURE-TESTING-GUIDE.md
**Total Bugs:** 37 failing tests across 7 categories
**Status:** IN PROGRESS

---

## Fixes Applied

| Bug ID | Fix | Status |
|--------|-----|--------|
| 1.6 | URL auto-prefix (https://) in TabViewModel.navigateToUrl() | DONE |
| 1.9 | Loading indicator added to BrowserScreen | DONE |
| 3.4 | updateFavorite SQL now includes URL column | DONE |
| Settings | Debug logging added to identify toggle issue | TESTING |

---

## Executive Summary

| Category | Bug Count | Severity | Root Cause Pattern |
|----------|-----------|----------|-------------------|
| Navigation | 2 | HIGH | URL scheme handling, loading indicator |
| Favorites | 6 | CRITICAL | UI wiring issues, callbacks not connected |
| Downloads | 7 | CRITICAL | DownloadViewModel stubbed, no persistence |
| History | 5 | HIGH | UI callbacks not connected to ViewModel |
| Settings | 15 | CRITICAL | Settings changes not persisting |
| Web Commands | 1 | MEDIUM | Clear cache not clearing cookies |
| Voice/Cursor | 3 | LOW | Requires VoiceOS integration |

---

## Phase 1: Critical Bugs (Must Fix)

### 1.1 Settings Screen - All Toggles Not Working

**Bug IDs:** 6.1.1-6.4.4 (15 bugs)
**Symptom:** All settings toggles/selections are disabled/non-clickable
**Severity:** CRITICAL

**Chain of Thought Analysis:**
```
1. SettingsScreen.kt reads settings via viewModel.settings.collectAsState()
2. Each toggle calls viewModel.setEnableJavaScript(), setTheme(), etc.
3. SettingsViewModel methods call updateSettings() which calls repository.updateSettings()
4. Repository updateSettings() correctly persists to database
5. UI shows settings but STATE IS NOT UPDATING on change

HYPOTHESIS: The issue is likely one of:
- A. Recomposition not triggering after state change
- B. Settings value being null (causing settings!! to fail silently)
- C. UI components not receiving click events due to modifier chain

INVESTIGATION NEEDED:
- Add logging to SettingsViewModel.setTheme() etc. to verify calls
- Check if settings.value is null at the time of click
- Verify Switch component onCheckedChange is being triggered
```

**Files to Modify:**
| File | Change |
|------|--------|
| `SettingsViewModel.kt` | Add debug logging, ensure state updates properly |
| `SettingsScreen.kt` | Verify click handlers are connected, check for null safety issues |

**Fix Strategy:**
1. Add `println()` statements in each `set*()` method to verify calls
2. Check if `_settings.value` is null when methods are called
3. Ensure StateFlow is properly emitting updates
4. Test each toggle individually

---

### 1.2 Favorites System Broken

**Bug IDs:** 3.4-3.10 (6 bugs)
**Symptoms:**
- 3.4: Cannot edit favorite title, changing URL adds new favorite
- 3.5: Bookmarks screen shows wrong message ("already in favorites")
- 3.6: Clicking favorite doesn't navigate
- 3.8: Scroll not applicable
- 3.9: Search not accessible
- 3.10: Folder organization not accessible

**Chain of Thought Analysis:**
```
1. AddToFavoritesDialog shows and saves via favoriteViewModel.addFavorite()
2. For EDIT: BookmarkListScreen.kt line 203-214 handles edit logic
   - editingFavorite != null triggers edit mode
   - viewModel.updateFavorite(updated) should be called

3. BUG FOUND: Line 206 creates NEW Favorite instead of updating
   - `editingFavorite!!.copy(url = url, title = title, folderId = folderId)`
   - This creates a new object with SAME ID but different URL
   - updateFavorite() should update by ID, but if URL changed, it might be treated as new

4. For NAVIGATION (3.6): BookmarkListScreen.kt line 176
   - onClick = { onBookmarkClick(favorite.url) }
   - onBookmarkClick is passed from BrowserApp.kt

5. BUG FOUND: Need to verify onBookmarkClick callback is wired to tabViewModel.navigateToUrl()

6. For 3.5 ("already in favorites" message):
   - EmptyBookmarksState shows "No bookmarks yet" when favorites.isEmpty()
   - If showing "already in favorites", this is coming from AddToFavoritesDialog
   - BUG: Likely the dialog is being shown instead of the list
```

**Files to Modify:**
| File | Change |
|------|--------|
| `BookmarkListScreen.kt` | Fix edit logic (URL change creates new), verify navigation callback |
| `BrowserApp.kt` | Verify onBookmarkClick is wired to navigation |
| `FavoriteViewModel.kt` | Add URL change detection in updateFavorite() |

**Fix Strategy:**
1. In `updateFavorite()`: Only update if ID matches, ignore URL changes
2. Wire `onBookmarkClick` callback properly in BrowserApp
3. Add search functionality to BookmarkListScreen (currently shows icon but may not work)

---

### 1.3 Downloads Not Tracking

**Bug IDs:** 4.2-4.8 (7 bugs)
**Symptom:** No progress, status, or management UI for downloads
**Severity:** CRITICAL

**Chain of Thought Analysis:**
```
1. DownloadViewModel.kt line 55-58: loadDownloads() is STUBBED
   - Returns emptyList() always
   - No database persistence for downloads

2. BrowserScreen.kt line 215-218: onDownloadStart callback exists
   - Calls downloadViewModel?.addDownload()
   - addDownload() adds to in-memory list but NOT persisted

3. BUG: Downloads are tracked in memory but:
   - Lost on app restart
   - Progress not being updated from actual download
   - No integration with Android DownloadManager

4. Repository: BrowserRepositoryImpl has NO download methods
   - observeDownloads(), addDownload(), updateDownload() not implemented
```

**Files to Modify:**
| File | Change |
|------|--------|
| `BrowserRepository.kt` | Add download interface methods |
| `BrowserRepositoryImpl.kt` | Implement download persistence |
| `BrowserDatabase.sq` | Add Download table if missing |
| `DownloadViewModel.kt` | Wire to repository, implement progress tracking |
| `DownloadListScreen.kt` | Verify UI displays download list correctly |

**Fix Strategy:**
1. Add Download table to SQLDelight schema
2. Implement repository methods for downloads
3. Connect DownloadViewModel to repository
4. Update progress from WebView download listener

---

## Phase 2: High Priority Bugs

### 2.1 URL Auto-Prefix (http vs https)

**Bug ID:** 1.6
**Symptom:** Navigating to "example.com" goes to http:// instead of https://
**Severity:** HIGH

**Chain of Thought Analysis:**
```
1. AddPageDialog (BrowserScreen.kt line 417-422) adds "https://" prefix
2. executeTextCommand() (line 479) adds "https://" prefix
3. BUT: AddressBar.kt onGo callback just calls tabViewModel.navigateToUrl(urlInput)
   - No URL normalization

4. TabViewModel.navigateToUrl() just updates tab URL directly
   - No scheme handling

5. BUG: URL normalization only happens in some code paths, not consistently
```

**Files to Modify:**
| File | Change |
|------|--------|
| `TabViewModel.kt` | Add URL normalization in navigateToUrl() |

**Fix:**
```kotlin
fun navigateToUrl(url: String) {
    val normalizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
    } else {
        url
    }
    // ... rest of method
}
```

---

### 2.2 Loading Indicator Not Showing

**Bug ID:** 1.9
**Symptom:** No progress indicator when page loading
**Severity:** HIGH

**Chain of Thought Analysis:**
```
1. BrowserScreen.kt line 205: onProgressChange callback exists
   - Currently has TODO comment

2. TabUiState has isLoading property
3. updateTabLoading() updates this state
4. BUT: No UI component displays the loading state

5. BUG: Loading state is tracked but not displayed in UI
```

**Files to Modify:**
| File | Change |
|------|--------|
| `BrowserScreen.kt` | Add LinearProgressIndicator when isLoading |
| `AddressBar.kt` | Optionally show loading in address bar |

**Fix:**
Add loading indicator in BrowserScreen between AddressBar and FavoritesBar:
```kotlin
if (activeTab?.isLoading == true) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}
```

---

### 2.3 History Actions Not Working

**Bug IDs:** 5.3-5.7 (5 bugs)
**Symptoms:**
- Search not working
- Clicking history doesn't navigate
- Delete not working
- Clear not working
- Settings toggle disabled

**Chain of Thought Analysis:**
```
1. HistoryScreen.kt line 176: onClick = { onHistoryClick(entry.url) }
   - Callback should navigate, need to verify wiring

2. HistoryScreen.kt line 177: onDelete = { viewModel.deleteHistoryEntry(entry.id) }
   - Should call repository, verify repository method

3. HistoryViewModel.deleteHistoryEntry() calls repository.deleteHistoryEntry()
4. BrowserRepositoryImpl.deleteHistoryEntry() exists and looks correct

5. POSSIBLE BUGS:
   - onHistoryClick not wired in BrowserApp
   - deleteHistoryEntry not refreshing flow
   - Search query not being applied to filter
```

**Files to Modify:**
| File | Change |
|------|--------|
| `BrowserApp.kt` | Verify onHistoryClick is wired to navigation |
| `HistoryViewModel.kt` | Verify search filtering works |
| `BrowserRepositoryImpl.kt` | Verify delete updates StateFlow |

---

## Phase 3: Medium Priority Bugs

### 3.1 Clear Cache Not Working

**Bug ID:** 7.4.3
**Symptom:** Clearing cache doesn't log out of Google
**Severity:** MEDIUM

**Analysis:**
- `webViewController.clearCache()` clears WebView cache
- Google session persists in cookies, not cache
- Need to also call `webViewController.clearCookies()`

**Fix:** Update clear cache command to also clear cookies, or add separate options.

---

## Phase 4: Out of Scope / VoiceOS Required

**Bug IDs:** 7.5.1-7.5.3
These require VoiceOS integration and are not standard browser features.

---

## Implementation Order

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| P0 | Fix Settings toggles | 2h | Critical |
| P0 | Fix Favorites navigation | 1h | Critical |
| P0 | Fix Downloads persistence | 4h | Critical |
| P1 | Fix URL https prefix | 30m | High |
| P1 | Add loading indicator | 30m | High |
| P1 | Fix History actions | 2h | High |
| P2 | Fix clear cache/cookies | 30m | Medium |

**Total Estimated Effort:** ~10 hours

---

## Swarm Agent Assignments

| Agent | Domain | Tasks |
|-------|--------|-------|
| UI Specialist | Settings, Dialogs | Fix Settings toggles, verify callbacks |
| Data Specialist | Repository | Implement Download persistence, verify History |
| Integration Specialist | Navigation | Wire callbacks in BrowserApp, URL handling |

---

## Testing Checklist

After fixes, verify:
- [ ] All settings toggles change values and persist on app restart
- [ ] Can edit existing favorite (title only, URL creates new)
- [ ] Clicking favorite navigates to URL
- [ ] Downloads show progress and persist across sessions
- [ ] History search filters results
- [ ] History delete removes entry
- [ ] URL without scheme gets https:// prefix
- [ ] Loading indicator shows during page load

---

## Files Summary

| File | Bug Count | Priority |
|------|-----------|----------|
| `SettingsScreen.kt` | 15 | P0 |
| `SettingsViewModel.kt` | 15 | P0 |
| `BookmarkListScreen.kt` | 4 | P0 |
| `FavoriteViewModel.kt` | 2 | P0 |
| `DownloadViewModel.kt` | 7 | P0 |
| `BrowserRepositoryImpl.kt` | 7 | P0 |
| `TabViewModel.kt` | 1 | P1 |
| `BrowserScreen.kt` | 2 | P1 |
| `HistoryScreen.kt` | 3 | P1 |
| `BrowserApp.kt` | 3 | P1 |

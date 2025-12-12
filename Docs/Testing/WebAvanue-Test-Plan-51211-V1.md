# WebAvanue Test Plan - December 11, 2025

**Project:** WebAvanue
**Platform:** Android
**Version:** Development
**Date:** 2025-12-11
**Test Scope:** Bug fixes, Settings UI completeness, Feature testing

---

## 1. Recent Bug Fixes - PRIORITY TESTING

### 1.1 Voice Commands Dialog Fixes
**Status:** ✅ Fixed (Commit 27ce4382)

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-001: Open Voice Commands Dialog | Dialog opens without crash | HIGH |
| TC-002: Navigate to categories view | All categories display correctly in 2-column grid | HIGH |
| TC-003: Select a category | Commands list displays with all command text visible | HIGH |
| TC-004: Click a command in CommandsView | Command executes and dialog closes | HIGH |
| TC-005: Navigate back from commands | Returns to categories view | MEDIUM |
| TC-006: Close dialog | Dialog closes properly | MEDIUM |

**Test Data:**
- Categories: Navigation, Scrolling, Tabs, Zoom, Modes, Features
- Sample commands: "go back", "scroll down", "new tab", "zoom in"

---

### 1.2 Orientation Changes
**Status:** ✅ Fixed (Removed android:configChanges)

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-101: Portrait to Landscape | Activity recreates, layout switches to landscape immediately | HIGH |
| TC-102: Landscape to Portrait | Activity recreates, layout switches to portrait immediately | HIGH |
| TC-103: Rapid orientation changes | No crashes, smooth transitions | MEDIUM |
| TC-104: Orientation during WebView load | Page load continues after orientation change | MEDIUM |
| TC-105: Orientation with dialog open | Dialog maintains state or reopens appropriately | LOW |

**Test Data:**
- Test with: empty tab, loading page, fully loaded page, multiple tabs
- Test rotations: 0° → 90° → 180° → 270° → 0°

---

### 1.3 Settings State Persistence
**Status:** ⚠️ NEEDS INVESTIGATION

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-201: Change desktop mode scale | Scale applies immediately to WebView | HIGH |
| TC-202: Toggle desktop mode | User agent changes, scale applies | HIGH |
| TC-203: Change initial page scale | New pages load with correct scale | HIGH |
| TC-204: Orientation change after scale change | Scale persists after rotation | HIGH |
| TC-205: Multiple rapid setting changes | All changes apply without lag | MEDIUM |

**Known Issues:**
- Settings may require multiple clicks to apply
- State caching suspected - needs code review
- SettingsApplicator integration missing

---

## 2. Settings UI Completeness - FEATURE GAPS

### 2.1 Missing Input Fields (HIGH PRIORITY)

| Setting | Current UI | Needed UI | Priority |
|---------|-----------|-----------|----------|
| Download Path | Text field (line 2373) | Directory picker dialog | HIGH |
| User Agent String | Not visible | Custom text field with presets | HIGH |
| Custom Search Engine | Not available | Add custom engine with name + URL | HIGH |
| Bookmark Storage Path | Not visible | Directory picker | MEDIUM |
| History Storage Path | Not visible | Directory picker | MEDIUM |
| Cache Directory | Not visible | Directory picker + size limit | MEDIUM |
| Max Cache Size | Not visible | Slider (MB) or input field | MEDIUM |
| Maximum Tabs | Not visible | Number picker (1-100) | MEDIUM |
| Custom CSS Injection | Not visible | Multi-line text editor | LOW |
| Custom JS Injection | Not visible | Multi-line text editor | LOW |
| Download Manager | Text field only | Browse button + permissions check | HIGH |

**Test Cases:**
- TC-301: Try to set download path manually (current: text only)
- TC-302: Try to add custom search engine (current: not possible)
- TC-303: Try to change user agent (current: not visible)
- TC-304: Try to limit cache size (current: not configurable)

---

### 2.2 UI Improvements Needed (MEDIUM PRIORITY)

| Setting | Current UI | Improvement Needed | Priority |
|---------|-----------|-------------------|----------|
| Desktop Window Size | Sliders (width/height) | Preview window or resolution presets | MEDIUM |
| Font Size | Enum dropdown | Live preview with sample text | MEDIUM |
| Theme | Dropdown | Visual theme preview cards | LOW |
| Search Suggestions | Switch only | Test search suggestions button | LOW |
| Voice Commands | Switch only | Test voice recognition button | LOW |

---

### 2.3 Missing Features in Settings

| Feature | Description | Priority | Implementation |
|---------|-------------|----------|----------------|
| Storage Manager | View cache/history/cookies size, clear individually | HIGH | New section |
| Permissions Manager | Per-site permissions (camera, mic, location) | HIGH | Exists (navigation item) |
| Download Manager UI | View active/completed downloads | HIGH | New screen |
| Bookmark Manager | Import/export, organize folders | MEDIUM | New screen |
| History Manager | Search, filter by date, export | MEDIUM | New screen |
| Extension Support | Enable/disable extensions | LOW | Future feature |
| Sync Account | Sign in, sync settings | MEDIUM | Backend required |
| Developer Tools | Console, network monitor, element inspector | LOW | Advanced feature |

---

## 3. Functional Testing

### 3.1 General Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-401: Change search engine | Select DuckDuckGo | Search bar uses DDG, searches work | HIGH |
| TC-402: Set homepage | Enter "https://example.com" | New tabs open to example.com | HIGH |
| TC-403: Enable search suggestions | Toggle ON | Suggestions appear while typing | MEDIUM |
| TC-404: Enable voice search | Toggle ON | Voice icon appears in search bar | MEDIUM |
| TC-405: Change new tab page | Select "Top Sites" | New tabs show top sites | MEDIUM |
| TC-406: Restore tabs on startup | Toggle ON | App reopens with previous tabs | MEDIUM |
| TC-407: Open links in background | Toggle ON | Links open without switching tabs | LOW |
| TC-408: Open links in new tab | Toggle ON | All links open in new tabs | LOW |

---

### 3.2 Appearance Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-501: Change theme to Dark | Select Dark | App switches to dark mode | HIGH |
| TC-502: Change theme to System | Select System | App follows device theme | HIGH |
| TC-503: Change font size to Large | Select Large | Web content text increases | MEDIUM |
| TC-504: Disable images | Toggle OFF | Web pages load without images | MEDIUM |
| TC-505: Enable force zoom | Toggle ON | Can zoom on all pages | MEDIUM |
| TC-506: Change initial scale | Set to 150% | Pages load at 150% zoom | HIGH |

---

### 3.3 Privacy & Security Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-601: Disable JavaScript | Toggle OFF | JavaScript doesn't execute | HIGH |
| TC-602: Disable cookies | Toggle OFF | Sites can't set cookies | HIGH |
| TC-603: Enable block popups | Toggle ON | Popup windows blocked | HIGH |
| TC-604: Enable block ads | Toggle ON | Ads blocked on pages | MEDIUM |
| TC-605: Enable block trackers | Toggle ON | Trackers blocked | MEDIUM |
| TC-606: Enable Do Not Track | Toggle ON | DNT header sent | LOW |
| TC-607: Disable WebRTC | Toggle OFF | WebRTC features disabled | MEDIUM |
| TC-608: Clear cache on exit | Toggle ON | Cache cleared when app closes | MEDIUM |
| TC-609: Clear history on exit | Toggle ON | History cleared when app closes | MEDIUM |
| TC-610: Clear cookies on exit | Toggle ON | Cookies cleared when app closes | MEDIUM |

---

### 3.4 Advanced Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-701: Enable desktop mode | Toggle ON | Desktop user agent used | HIGH |
| TC-702: Desktop default zoom | Set to 75% | Desktop pages load at 75% | HIGH |
| TC-703: Desktop window size | Set 1920x1080 | Pages render at desktop resolution | HIGH |
| TC-704: Desktop auto-fit zoom | Toggle ON | Zoom adjusts to fit viewport | MEDIUM |
| TC-705: Change auto-play to Never | Select Never | Videos don't auto-play | MEDIUM |
| TC-706: Enable voice commands | Toggle ON | Voice button appears | HIGH |
| TC-707: Voice dialog auto-close | Toggle ON | Dialog closes after command | MEDIUM |
| TC-708: Voice dialog delay | Set 2000ms | Dialog waits 2s before closing | LOW |

**Desktop Mode Test Data:**
- Test URLs: desktop.github.com, twitter.com/home, linkedin.com
- Expected: Full desktop layout, not mobile

---

### 3.5 Downloads Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-801: Set download path (text) | Enter "/sdcard/Downloads" | Downloads go to path | HIGH |
| TC-802: Enable ask download location | Toggle ON | Prompt appears before download | HIGH |
| TC-803: Enable Wi-Fi only downloads | Toggle ON | Downloads blocked on cellular | MEDIUM |
| TC-804: Download file | Click download link | File downloads to configured path | HIGH |
| TC-805: Download with path picker | (Not implemented) | Should open directory picker | HIGH |

**⚠️ Critical Issue:** Download path uses text field instead of directory picker

---

### 3.6 Performance Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-901: Enable hardware acceleration | Toggle ON | GPU used for rendering | MEDIUM |
| TC-902: Enable preload pages | Toggle ON | Pages load faster | LOW |
| TC-903: Enable data saver | Toggle ON | Data usage reduced | MEDIUM |
| TC-904: Enable text reflow | Toggle ON | Text reformats when zooming | LOW |

---

### 3.7 Sync Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1001: Enable sync | Toggle ON | Sync options appear | LOW |
| TC-1002: Enable sync bookmarks | Toggle ON (sync enabled) | Bookmarks sync across devices | LOW |
| TC-1003: Enable sync history | Toggle ON (sync enabled) | History syncs across devices | LOW |
| TC-1004: Enable sync passwords | Toggle ON (sync enabled) | Passwords sync across devices | LOW |
| TC-1005: Enable sync settings | Toggle ON (sync enabled) | Settings sync across devices | LOW |

**Note:** Sync requires backend implementation

---

### 3.8 Voice & AI Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1101: Enable AI summaries | Toggle ON | Summary option appears on pages | LOW |
| TC-1102: Enable AI translation | Toggle ON | Translation option appears | LOW |
| TC-1103: Enable read aloud | Toggle ON | Read aloud controls appear | LOW |

**Note:** AI features require backend integration

---

### 3.9 Command Bar Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1201: Enable auto-hide | Toggle ON | Command bar auto-hides | MEDIUM |
| TC-1202: Set auto-hide delay | Set 5000ms | Bar hides after 5 seconds | LOW |

---

### 3.10 WebXR Settings

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1301: Enable WebXR | Toggle ON | WebXR options appear | LOW |
| TC-1302: Enable AR | Toggle ON (WebXR enabled) | AR sessions allowed | LOW |
| TC-1303: Enable VR | Toggle ON (WebXR enabled) | VR sessions allowed | LOW |
| TC-1304: XR performance mode | Select "High Quality" | XR runs at 90fps | LOW |
| TC-1305: XR auto-pause timeout | Set 30 minutes | XR pauses after 30 min | LOW |
| TC-1306: Show FPS indicator | Toggle ON | FPS counter visible in XR | LOW |
| TC-1307: Require Wi-Fi for XR | Toggle ON | XR blocked on cellular | LOW |

---

## 4. Integration Testing

### 4.1 Settings Persistence

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1401: Settings survive restart | Change 10 settings → Close app → Reopen | All settings retained | HIGH |
| TC-1402: Settings survive crash | Change settings → Force stop → Reopen | All settings retained | HIGH |
| TC-1403: Settings sync state | Change setting → Check ViewModel state | State matches UI | HIGH |

---

### 4.2 WebView Integration

| Test Case | Steps | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1501: Settings apply to WebView | Change JavaScript → Load page | JavaScript enabled/disabled | HIGH |
| TC-1502: Desktop mode applies | Enable desktop → Load mobile site | Desktop version loads | HIGH |
| TC-1503: Scale applies | Change scale → Load page | Page loads at scale | HIGH |
| TC-1504: Cookies apply | Disable cookies → Login to site | Login fails (cookies disabled) | HIGH |

---

## 5. UI/UX Testing

### 5.1 Portrait Layout

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-1601: Settings scroll smoothly | No lag, smooth scrolling | MEDIUM |
| TC-1602: All sections visible | Can access all settings | HIGH |
| TC-1603: Touch targets 48dp | All buttons easily tappable | MEDIUM |
| TC-1604: Text readable | No truncation, proper spacing | MEDIUM |

---

### 5.2 Landscape Layout (AR/XR)

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-1701: Two-pane layout loads | Left nav, right content | MEDIUM |
| TC-1702: Category navigation works | Clicking category shows settings | MEDIUM |
| TC-1703: Selected category highlighted | Visual feedback on selection | LOW |
| TC-1704: Content scrolls independently | Right pane scrolls, left pane fixed | MEDIUM |
| TC-1705: Glassmorphism renders | Proper alpha/blur effects | LOW |

---

## 6. Regression Testing

### 6.1 Core Browser Features

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-1801: Load webpage | Page loads correctly | HIGH |
| TC-1802: Navigate back/forward | History navigation works | HIGH |
| TC-1803: Refresh page | Page reloads | HIGH |
| TC-1804: Open new tab | Tab created | HIGH |
| TC-1805: Close tab | Tab closes | HIGH |
| TC-1806: Switch tabs | Active tab changes | HIGH |
| TC-1807: Bookmark page | Bookmark saved | MEDIUM |
| TC-1808: View history | History displays | MEDIUM |
| TC-1809: Download file | File downloads | HIGH |
| TC-1810: Voice command | Command executes | HIGH |

---

## 7. Edge Cases & Error Handling

### 7.1 Invalid Input

| Test Case | Input | Expected Result | Priority |
|-----------|-------|----------------|----------|
| TC-1901: Invalid homepage URL | "not a url" | Error message or validation | MEDIUM |
| TC-1902: Invalid download path | "/root/forbidden" | Error or fallback to default | MEDIUM |
| TC-1903: Extreme font size | (via manual edit) | Bounded to min/max | LOW |
| TC-1904: Extreme zoom | 500% scale | Bounded or capped | LOW |

---

### 7.2 State Conflicts

| Test Case | Scenario | Expected Result | Priority |
|-----------|----------|----------------|----------|
| TC-2001: Desktop + Mobile scale | Desktop mode ON, initial scale 200% | Desktop scale takes precedence | MEDIUM |
| TC-2002: Sync disabled, sub-options ON | Sync OFF, Sync Bookmarks ON | Sub-options disabled/hidden | LOW |
| TC-2003: WebXR disabled, AR/VR ON | WebXR OFF, AR ON | AR disabled/hidden | LOW |

---

## 8. Performance Testing

### 8.1 Settings Load Time

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-2101: Open settings screen | Loads in <500ms | MEDIUM |
| TC-2102: Switch category (landscape) | Switches in <200ms | LOW |
| TC-2103: Change setting | Saves in <100ms | MEDIUM |

---

### 8.2 Memory & Battery

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-2201: Settings screen memory | <50MB overhead | LOW |
| TC-2202: Rapid setting changes | No memory leak | MEDIUM |
| TC-2203: Battery drain | Minimal impact | LOW |

---

## 9. Accessibility Testing

| Test Case | Expected Result | Priority |
|-----------|----------------|----------|
| TC-2301: TalkBack support | All settings readable | MEDIUM |
| TC-2302: Screen reader navigation | Logical navigation order | MEDIUM |
| TC-2303: Voice commands for settings | (Future) Voice control works | LOW |
| TC-2304: High contrast mode | Settings readable in high contrast | LOW |

---

## 10. Test Execution Checklist

### Pre-Testing
- [ ] Clean install of latest APK
- [ ] Clear app data and cache
- [ ] Verify device orientation unlocked
- [ ] Enable USB debugging
- [ ] Connect to ADB

### Test Execution
- [ ] Run all HIGH priority tests
- [ ] Run all MEDIUM priority tests
- [ ] Document any failures with screenshots
- [ ] Log device info (model, Android version, RAM)
- [ ] Note any performance issues

### Post-Testing
- [ ] Collect logcat output for failures
- [ ] Take screenshots of UI issues
- [ ] Generate test report
- [ ] File bugs for failures
- [ ] Update test plan with new findings

---

## 11. Bug Tracking Template

```markdown
**Bug ID:** BUG-XXXX
**Title:** [Brief description]
**Severity:** Critical / High / Medium / Low
**Priority:** P0 / P1 / P2 / P3

**Test Case:** TC-XXXX
**Device:** [Model, Android version]
**Build:** [APK version / commit hash]

**Steps to Reproduce:**
1.
2.
3.

**Expected Result:**


**Actual Result:**


**Screenshots:**


**Logcat:**


**Notes:**

```

---

## 12. Known Issues & Workarounds

| Issue | Severity | Workaround | Status |
|-------|----------|------------|--------|
| Download path text field only | HIGH | Manual path entry | Open |
| Settings state caching | MEDIUM | Multiple clicks required | Investigating |
| SettingsApplicator not integrated | HIGH | Settings not applying to WebView | Open |
| No custom search engine support | MEDIUM | Use predefined engines only | Open |
| No directory picker for downloads | HIGH | Manual path entry | Open |

---

## 13. Future Test Coverage

### Phase 2 Testing (Post-Settings Fixes)
- Custom search engine CRUD operations
- Directory picker integration
- User agent customization
- Cache size management
- Storage usage visualization
- Download manager UI
- Bookmark manager UI
- History manager UI

### Phase 3 Testing (Advanced Features)
- WebXR immersive sessions
- AI-powered features (summaries, translation)
- Sync across devices
- Extension support
- Developer tools
- Custom CSS/JS injection

---

## 14. Test Environment

### Devices
- **Primary:** [Device model, Android version]
- **Secondary:** [Device model, Android version]
- **Emulator:** Android X.X (API XX)

### Test Data
- **Websites:** google.com, github.com, youtube.com, twitter.com, reddit.com
- **Download files:** PDF, ZIP, APK, MP3, MP4
- **User agents:** Chrome Desktop, Firefox Desktop, Safari Desktop

---

## 15. Success Criteria

### Must Pass (Release Blocker)
- ✅ All TC-001 to TC-005 (Voice Commands)
- ✅ All TC-101 to TC-104 (Orientation)
- ✅ All TC-201 to TC-204 (Settings persistence)
- ✅ All TC-401 to TC-408 (General settings)
- ✅ All TC-601 to TC-610 (Privacy settings)
- ✅ All TC-701 to TC-707 (Advanced settings)
- ✅ All TC-1401 to TC-1403 (Settings persistence)
- ✅ All TC-1501 to TC-1504 (WebView integration)

### Should Pass (High Priority)
- All TC-501 to TC-506 (Appearance)
- All TC-801 to TC-805 (Downloads)
- All TC-1601 to TC-1604 (UI/UX)
- All TC-1801 to TC-1810 (Regression)

### Nice to Have (Medium Priority)
- All remaining test cases

---

**Test Plan Version:** 1.0
**Last Updated:** 2025-12-11
**Next Review:** After Phase 1 fixes complete

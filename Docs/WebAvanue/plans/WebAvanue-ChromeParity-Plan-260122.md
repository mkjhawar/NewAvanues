# Implementation Plan: WebAvanue Chrome Parity

## Overview
**Feature:** Chrome Browser Feature Parity
**Module:** `/Modules/WebAvanue` (KMP Flat Structure)
**Platforms:** Android (Primary), iOS/Desktop (Phase 2 - Disabled)
**Swarm Recommended:** Yes (18 features, 54+ tasks)
**Current Parity:** 60%
**Target Parity:** 92% (WebView maximum)

---

## Module Structure (Existing)

```
/Volumes/M-Drive/Coding/NewAvanues/Modules/WebAvanue/
├── build.gradle.kts                    # KMP build config
├── src/
│   ├── commonMain/
│   │   ├── kotlin/com/augmentalis/webavanue/    # 150 files (flat)
│   │   ├── sqldelight/.../BrowserDatabase.sq    # SQLDelight schema
│   │   └── resources/                           # JS files, assets
│   ├── androidMain/
│   │   ├── kotlin/com/augmentalis/webavanue/    # 50 files (flat)
│   │   ├── res/                                 # Android resources
│   │   └── AndroidManifest.xml
│   ├── iosMain/                                 # Phase 2
│   ├── desktopMain/                             # Phase 2
│   ├── commonTest/
│   └── androidUnitTest/
└── docs/
```

**Package:** `com.augmentalis.webavanue`

---

## Existing Files to Reuse

### Already Implemented (No New Work Needed)
| Feature | Existing File | Status |
|---------|---------------|--------|
| Find in Page | `FindInPageBar.kt`, `TabFindInPageOps.kt` | EXISTS |
| Tab Groups | `TabGroupModel.kt`, `TabGroupDialog.kt`, `TabGroupAssignmentDialog.kt` | EXISTS |
| Screenshot | `ScreenshotCapture.kt`, `ScreenshotManager.kt`, `ScreenshotDialog.kt` | EXISTS |
| Reader Mode | `TabReadingModeOps.kt` | PARTIAL |
| Private Browsing | `PrivateBrowsingManager.kt` | EXISTS |
| Security Dialogs | `SecurityDialogs.kt`, `BasicAuthDialog.kt` | EXISTS |
| Voice Commands | `VoiceCommandService.kt`, `VoiceCommandHandler.kt` | EXISTS |
| Action Mapper | `WebAvanueActionMapper.kt` | EXISTS |
| XR Support | `CommonXRManager.kt`, `XROverlay.kt`, `XRSessionIndicator.kt` | EXISTS |
| Tracker Blocker | `TrackerBlocker.kt` | EXISTS |

### Files Needing Enhancement
| Feature | Existing File | Enhancement Needed |
|---------|---------------|-------------------|
| WebViewController | `CommonWebViewController.kt` | Add print, keyboard shortcuts |
| WebViewContainer | `WebViewContainer.kt` | Add context menu detection |
| BrowserRepository | `BrowserRepository.kt` | Add new tables for credentials, autofill |
| TabViewModel | `TabViewModel.kt` | Integrate new features |
| SettingsScreen | `SettingsScreen.kt` | Add password/autofill/sync settings |

---

## New Files to Create

### Phase 1: Quick Wins (P0)
| File | Location | Purpose |
|------|----------|---------|
| `PrintManager.kt` | commonMain | Print page functionality |
| `KeyboardShortcutHandler.kt` | commonMain | Keyboard shortcut mapping |
| `KeyboardShortcutHandler.android.kt` | androidMain | Android key event handling |
| `TextSelectionActions.kt` | commonMain | Copy/search/translate selected text |

### Phase 2: Core Features (P1)
| File | Location | Purpose |
|------|----------|---------|
| `ContextMenuTarget.kt` | commonMain | Sealed class for context menu targets |
| `ContextMenu.kt` | commonMain | Context menu Composable |
| `ContextMenuHandler.android.kt` | androidMain | hitTestResult handling |
| `ReaderModeExtractor.kt` | commonMain | Readability.js integration |
| `ReaderModeView.kt` | commonMain | Clean reader UI |
| `ReadingListRepository.kt` | commonMain | Reading list data layer |
| `ReadingListScreen.kt` | commonMain | Reading list UI |
| `ReadingListItem.kt` | commonMain | Data model |

### Phase 3: Security & Forms (P2)
| File | Location | Purpose |
|------|----------|---------|
| `CredentialStore.kt` | commonMain | Password manager interface |
| `CredentialStore.android.kt` | androidMain | Android Keystore implementation |
| `LoginFormDetector.kt` | commonMain | JS injection for form detection |
| `SavePasswordDialog.kt` | commonMain | Save password prompt |
| `PasswordManagerScreen.kt` | commonMain | Manage saved passwords |
| `AutofillProfile.kt` | commonMain | Address/card data model |
| `AutofillRepository.kt` | commonMain | Autofill data layer |
| `AutofillSuggestionPopup.kt` | commonMain | Autofill dropdown |
| `AutofillSettingsScreen.kt` | commonMain | Autofill settings UI |
| `EnhancedDownloadManager.kt` | commonMain | Pause/resume/retry downloads |

### Phase 4: Advanced Features (P2)
| File | Location | Purpose |
|------|----------|---------|
| `TranslationService.kt` | commonMain | Translation interface |
| `TranslationService.android.kt` | androidMain | ML Kit or Google Translate |
| `TranslationSettingsScreen.kt` | commonMain | Translation settings |
| `UserScript.kt` | commonMain | User script data model |
| `UserScriptManager.kt` | commonMain | Script injection engine |
| `UserScriptsScreen.kt` | commonMain | Manage user scripts |

### Phase 5: Sync & DevTools (P3)
| File | Location | Purpose |
|------|----------|---------|
| `SyncService.kt` | commonMain | Sync interface |
| `FirebaseSyncService.kt` | androidMain | Firebase implementation |
| `SyncSettingsScreen.kt` | commonMain | Sync settings UI |
| `ConsoleLogger.kt` | commonMain | JS console interceptor |
| `NetworkInspector.kt` | commonMain | Request logger |
| `DevToolsScreen.kt` | commonMain | Basic devtools UI |
| `CastManager.kt` | androidMain | Chromecast support |

---

## Database Schema Changes

**File:** `src/commonMain/sqldelight/com/augmentalis/webavanue/data/BrowserDatabase.sq`

```sql
-- ADD TO EXISTING SCHEMA --

-- Reading List (Phase 2)
CREATE TABLE ReadingList (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    excerpt TEXT,
    thumbnail TEXT,
    offline_html TEXT,
    saved_at INTEGER NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0
);

-- Credentials (Phase 3)
CREATE TABLE Credential (
    id TEXT PRIMARY KEY NOT NULL,
    domain TEXT NOT NULL,
    username TEXT NOT NULL,
    password_encrypted BLOB NOT NULL,
    created_at INTEGER NOT NULL,
    last_used_at INTEGER,
    UNIQUE(domain, username)
);

-- Autofill Profiles (Phase 3)
CREATE TABLE AutofillProfile (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT,
    email TEXT,
    phone TEXT,
    address_line1 TEXT,
    address_line2 TEXT,
    city TEXT,
    state TEXT,
    zip TEXT,
    country TEXT,
    card_number_encrypted BLOB,
    card_expiry TEXT,
    card_name TEXT,
    created_at INTEGER NOT NULL,
    last_used_at INTEGER
);

-- User Scripts (Phase 4)
CREATE TABLE UserScript (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    match_patterns TEXT NOT NULL,
    exclude_patterns TEXT,
    js_code TEXT NOT NULL,
    run_at TEXT NOT NULL DEFAULT 'document_end',
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER
);

-- Sync metadata (Phase 5)
CREATE TABLE SyncMetadata (
    id TEXT PRIMARY KEY NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    last_synced_at INTEGER NOT NULL,
    sync_status TEXT NOT NULL DEFAULT 'pending',
    UNIQUE(entity_type, entity_id)
);
```

---

## Phases

### Phase 1: Quick Wins (P0) - Week 1-2
**Goal:** 60% → 68% parity
**Files to modify/create:** 8

#### 1.1 Find in Page Enhancement
- [x] `FindInPageBar.kt` - EXISTS, verify functionality
- [x] `TabFindInPageOps.kt` - EXISTS, verify functionality
- [ ] Wire Ctrl+F in `KeyboardShortcutHandler.kt` (NEW)
- [ ] Add voice command "find [text]" to `VoiceCommandHandler.kt`

#### 1.2 Print Support
- [ ] Create `PrintManager.kt` in commonMain (expect/actual)
- [ ] Create `PrintManager.android.kt` using Android PrintManager
- [ ] Add `printPage()` to `CommonWebViewController.kt`
- [ ] Wire Ctrl+P in `KeyboardShortcutHandler.kt`
- [ ] Add "Print" option to context menu

#### 1.3 Keyboard Shortcuts
- [ ] Create `KeyboardShortcutHandler.kt` (commonMain - interface)
- [ ] Create `KeyboardShortcutHandler.android.kt` (androidMain - KeyEvent)
- [ ] Implement navigation: Alt+Left/Right, F5, Ctrl+R
- [ ] Implement tabs: Ctrl+T/W/Tab, Ctrl+1-9
- [ ] Implement actions: Ctrl+F/P/L/D, Ctrl+Shift+N
- [ ] Implement zoom: Ctrl++/-/0, F11
- [ ] Register in Android app MainActivity

#### 1.4 Full-Page Screenshot Enhancement
- [x] `ScreenshotCapture.kt` - EXISTS
- [x] `ScreenshotManager.kt` - EXISTS
- [x] `ScreenshotDialog.kt` - EXISTS
- [ ] Add share/save options after capture
- [ ] Add "Take screenshot" to context menu

#### 1.5 Text Selection Actions
- [ ] Create `TextSelectionActions.kt` (commonMain)
- [ ] Implement "Search", "Translate", "Share", "Speak" actions
- [ ] Create `CustomActionModeCallback` in androidMain
- [ ] Wire TTS for "Speak" action

---

### Phase 2: Core Features (P1) - Week 3-4
**Goal:** 68% → 77% parity
**Files to modify/create:** 12

#### 2.1 Private Browsing Enhancement
- [x] `PrivateBrowsingManager.kt` - EXISTS
- [ ] Verify incognito WebView pool isolation
- [ ] Add distinct UI indicator (dark theme, mask icon)
- [ ] Wire Ctrl+Shift+N shortcut
- [ ] Add voice command "incognito tab"

#### 2.2 Context Menu
- [ ] Create `ContextMenuTarget.kt` (sealed class)
- [ ] Create `ContextMenu.kt` (Composable)
- [ ] Create `ContextMenuHandler.android.kt` (hitTestResult)
- [ ] Wire actions: Open in new tab, Copy, Save, Search
- [ ] Add "View page source" option

#### 2.3 Reader Mode Enhancement
- [x] `TabReadingModeOps.kt` - EXISTS (partial)
- [ ] Bundle Readability.js in `src/commonMain/resources/`
- [ ] Create `ReaderModeExtractor.kt` (JS injection + parsing)
- [ ] Create `ReaderModeView.kt` (clean typography)
- [ ] Add reader mode toggle in toolbar
- [ ] Add customization (font size, theme)

#### 2.4 Reading List
- [ ] Add ReadingList table to `BrowserDatabase.sq`
- [ ] Create `ReadingListItem.kt` (data model)
- [ ] Create `ReadingListRepository.kt`
- [ ] Create `ReadingListScreen.kt`
- [ ] Implement offline HTML caching with images
- [ ] Add "Save to Reading List" menu option

---

### Phase 3: Security & Forms (P2) - Week 5-8
**Goal:** 77% → 85% parity
**Files to modify/create:** 14

#### 3.1 Password Manager
- [ ] Add Credential table to `BrowserDatabase.sq`
- [ ] Create `CredentialStore.kt` (expect/actual interface)
- [ ] Create `CredentialStore.android.kt` (Android Keystore)
- [ ] Create `LoginFormDetector.kt` (JS injection)
- [ ] Create `SavePasswordDialog.kt`
- [ ] Create `PasswordManagerScreen.kt`
- [ ] Add @JavascriptInterface for form submission
- [ ] Implement biometric unlock option

#### 3.2 Autofill
- [ ] Add AutofillProfile table to `BrowserDatabase.sq`
- [ ] Create `AutofillProfile.kt` (data model)
- [ ] Create `AutofillRepository.kt`
- [ ] Create `AutofillFieldDetector.kt` (JS injection)
- [ ] Create `AutofillSuggestionPopup.kt`
- [ ] Create `AutofillSettingsScreen.kt`
- [ ] Handle credit card encryption

#### 3.3 Download Manager Enhancement
- [ ] Create `EnhancedDownloadManager.kt`
- [ ] Add pause/resume with Range headers
- [ ] Add retry failed downloads
- [ ] Add "Open folder" action
- [ ] Update `DownloadQueue.kt` with new states
- [ ] Add download speed & ETA indicators

---

### Phase 4: Advanced Features (P2) - Week 9-12
**Goal:** 85% → 92% parity
**Files to modify/create:** 10

#### 4.1 Page Translation
- [ ] Create `TranslationService.kt` (expect/actual)
- [ ] Create `TranslationService.android.kt` (ML Kit)
- [ ] Add language detection
- [ ] Create `TranslationSettingsScreen.kt`
- [ ] Add "Translate page" button/menu
- [ ] Add auto-translate prompt

#### 4.2 Tab Groups Enhancement
- [x] `TabGroupModel.kt` - EXISTS
- [x] `TabGroupDialog.kt` - EXISTS
- [x] `TabGroupAssignmentDialog.kt` - EXISTS
- [ ] Verify collapse/expand functionality
- [ ] Add drag-drop to create groups
- [ ] Add group colors in tab strip

#### 4.3 User Scripts
- [ ] Add UserScript table to `BrowserDatabase.sq`
- [ ] Create `UserScript.kt` (data model)
- [ ] Create `UserScriptManager.kt`
- [ ] Implement match pattern parsing (glob → regex)
- [ ] Inject at DOCUMENT_START/END/IDLE
- [ ] Create `UserScriptsScreen.kt`
- [ ] Support Tampermonkey @match syntax

---

### Phase 5: Sync & DevTools (P3) - Week 13+
**Goal:** 92% → 95% parity (maximum)
**Files to modify/create:** 8

#### 5.1 Sync System
- [ ] Add SyncMetadata table to `BrowserDatabase.sq`
- [ ] Create `SyncService.kt` (interface)
- [ ] Create `FirebaseSyncService.kt` (androidMain)
- [ ] Implement bookmark sync with conflict resolution
- [ ] Implement history sync
- [ ] Create `SyncSettingsScreen.kt`
- [ ] Add sync status indicator

#### 5.2 Basic DevTools
- [ ] Create `ConsoleLogger.kt` (@JavascriptInterface)
- [ ] Create `NetworkInspector.kt` (shouldInterceptRequest)
- [ ] Create `DevToolsScreen.kt`
- [ ] Add "Inspect" toggle in developer settings

#### 5.3 Chromecast Support
- [ ] Add Google Cast SDK to build.gradle.kts
- [ ] Create `CastManager.kt` (androidMain)
- [ ] Add cast button to toolbar
- [ ] Implement media casting

---

## Time Estimates

| Phase | Sequential | Parallel (Swarm) |
|-------|------------|------------------|
| Phase 1 (P0) | 2 weeks | 1 week |
| Phase 2 (P1) | 2 weeks | 1 week |
| Phase 3 (P2) | 4 weeks | 2 weeks |
| Phase 4 (P2) | 4 weeks | 2 weeks |
| Phase 5 (P3) | 6 weeks | 3 weeks |
| **Total** | **18 weeks** | **9 weeks** |

**Savings with Swarm:** 9 weeks (50%)

---

## Dependencies

- Phase 2.3 (Reader Mode) → Phase 2.4 (Reading List) - shares Readability.js
- Phase 3.1 (Password) → Phase 3.2 (Autofill) - shares form detection JS
- All database changes require migration script

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| WebView limitations | High | Document unsupported features clearly |
| Translation API costs | Medium | Use free ML Kit or Google Widget |
| Sync complexity | High | Start single-device, add sync later |
| Password security | Critical | Use Android Keystore, security audit |
| JS injection reliability | Medium | Test on multiple sites |

---

**Plan Generated:** 2026-01-22
**Total New Files:** ~35
**Total Tasks:** 54
**Estimated Duration:** 9-18 weeks

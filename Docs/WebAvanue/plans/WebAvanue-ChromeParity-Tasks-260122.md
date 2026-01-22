# WebAvanue Chrome Parity - Task Breakdown

## Swarm Configuration
**Mode:** `.swarm .auto`
**Parallel Agents:** 4 (one per phase priority)
**Total Tasks:** 54
**Estimated Parallel Duration:** 9 weeks

---

## PHASE 1: QUICK WINS (P0) - Agent 1

### Sprint 1.1: Find in Page (1 day)
| ID | Task | File | Status |
|----|------|------|--------|
| P1-01 | Verify FindInPageBar.kt functionality | `src/commonMain/.../FindInPageBar.kt` | EXISTING |
| P1-02 | Verify TabFindInPageOps.kt functionality | `src/commonMain/.../TabFindInPageOps.kt` | EXISTING |
| P1-03 | Add Ctrl+F shortcut to KeyboardShortcutHandler | `src/commonMain/.../KeyboardShortcutHandler.kt` | TODO |
| P1-04 | Add voice command "find [text]" | `src/commonMain/.../VoiceCommandHandler.kt` | TODO |

### Sprint 1.2: Print Support (1 day)
| ID | Task | File | Status |
|----|------|------|--------|
| P1-05 | Create PrintManager.kt expect declaration | `src/commonMain/.../PrintManager.kt` | TODO |
| P1-06 | Create PrintManager.android.kt actual | `src/androidMain/.../PrintManager.android.kt` | TODO |
| P1-07 | Add printPage() to CommonWebViewController | `src/commonMain/.../CommonWebViewController.kt` | TODO |
| P1-08 | Wire Ctrl+P keyboard shortcut | `src/commonMain/.../KeyboardShortcutHandler.kt` | TODO |
| P1-09 | Add "Print" to context menu options | `src/commonMain/.../ContextMenu.kt` | TODO |

### Sprint 1.3: Keyboard Shortcuts (2 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P1-10 | Create KeyboardShortcutHandler.kt interface | `src/commonMain/.../KeyboardShortcutHandler.kt` | TODO |
| P1-11 | Create KeyboardShortcutHandler.android.kt | `src/androidMain/.../KeyboardShortcutHandler.android.kt` | TODO |
| P1-12 | Implement navigation shortcuts (Alt+←/→, F5) | `src/androidMain/.../KeyboardShortcutHandler.android.kt` | TODO |
| P1-13 | Implement tab shortcuts (Ctrl+T/W/Tab/1-9) | `src/androidMain/.../KeyboardShortcutHandler.android.kt` | TODO |
| P1-14 | Implement action shortcuts (Ctrl+F/P/L/D) | `src/androidMain/.../KeyboardShortcutHandler.android.kt` | TODO |
| P1-15 | Implement zoom shortcuts (Ctrl++/-/0, F11) | `src/androidMain/.../KeyboardShortcutHandler.android.kt` | TODO |

### Sprint 1.4: Screenshot Enhancement (0.5 day)
| ID | Task | File | Status |
|----|------|------|--------|
| P1-16 | Verify ScreenshotCapture.kt functionality | `src/commonMain/.../ScreenshotCapture.kt` | EXISTING |
| P1-17 | Add share/save options to ScreenshotDialog | `src/commonMain/.../ScreenshotDialog.kt` | TODO |
| P1-18 | Add "Take screenshot" to context menu | `src/commonMain/.../ContextMenu.kt` | TODO |

### Sprint 1.5: Text Selection Actions (1 day)
| ID | Task | File | Status |
|----|------|------|--------|
| P1-19 | Create TextSelectionActions.kt | `src/commonMain/.../TextSelectionActions.kt` | TODO |
| P1-20 | Implement Search action (open in new tab) | `src/commonMain/.../TextSelectionActions.kt` | TODO |
| P1-21 | Implement Translate action | `src/commonMain/.../TextSelectionActions.kt` | TODO |
| P1-22 | Implement Share action | `src/commonMain/.../TextSelectionActions.kt` | TODO |
| P1-23 | Implement Speak action (TTS) | `src/commonMain/.../TextSelectionActions.kt` | TODO |
| P1-24 | Create CustomActionModeCallback.android.kt | `src/androidMain/.../CustomActionModeCallback.kt` | TODO |

**Phase 1 Total: 24 tasks | 5.5 days**

---

## PHASE 2: CORE FEATURES (P1) - Agent 2

### Sprint 2.1: Private Browsing Enhancement (1 day)
| ID | Task | File | Status |
|----|------|------|--------|
| P2-01 | Verify PrivateBrowsingManager.kt | `src/commonMain/.../PrivateBrowsingManager.kt` | EXISTING |
| P2-02 | Add incognito UI indicator (dark theme + icon) | `src/commonMain/.../BrowserScreen.kt` | TODO |
| P2-03 | Wire Ctrl+Shift+N shortcut | `src/commonMain/.../KeyboardShortcutHandler.kt` | TODO |
| P2-04 | Add voice command "incognito tab" | `src/commonMain/.../VoiceCommandHandler.kt` | TODO |

### Sprint 2.2: Context Menu (2 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P2-05 | Create ContextMenuTarget.kt sealed class | `src/commonMain/.../ContextMenuTarget.kt` | TODO |
| P2-06 | Create ContextMenu.kt Composable | `src/commonMain/.../ContextMenu.kt` | TODO |
| P2-07 | Create ContextMenuHandler.android.kt | `src/androidMain/.../ContextMenuHandler.android.kt` | TODO |
| P2-08 | Implement Link actions (open new tab, copy, share) | `src/commonMain/.../ContextMenu.kt` | TODO |
| P2-09 | Implement Image actions (save, copy, search) | `src/commonMain/.../ContextMenu.kt` | TODO |
| P2-10 | Implement Page actions (reload, bookmark, source) | `src/commonMain/.../ContextMenu.kt` | TODO |

### Sprint 2.3: Reader Mode (3 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P2-11 | Bundle Readability.js in resources | `src/commonMain/resources/readability.js` | TODO |
| P2-12 | Create ReaderModeExtractor.kt | `src/commonMain/.../ReaderModeExtractor.kt` | TODO |
| P2-13 | Create ReaderModeView.kt Composable | `src/commonMain/.../ReaderModeView.kt` | TODO |
| P2-14 | Add reader mode toggle to toolbar | `src/commonMain/.../AddressBar.kt` | TODO |
| P2-15 | Implement article detection heuristics | `src/commonMain/.../ReaderModeExtractor.kt` | TODO |
| P2-16 | Add font size/theme customization | `src/commonMain/.../ReaderModeView.kt` | TODO |

### Sprint 2.4: Reading List (2 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P2-17 | Add ReadingList table to BrowserDatabase.sq | `src/commonMain/sqldelight/.../BrowserDatabase.sq` | TODO |
| P2-18 | Create ReadingListItem.kt data model | `src/commonMain/.../ReadingListItem.kt` | TODO |
| P2-19 | Create ReadingListRepository.kt | `src/commonMain/.../ReadingListRepository.kt` | TODO |
| P2-20 | Create ReadingListScreen.kt | `src/commonMain/.../ReadingListScreen.kt` | TODO |
| P2-21 | Implement offline HTML caching | `src/commonMain/.../ReadingListRepository.kt` | TODO |
| P2-22 | Add "Save to Reading List" menu option | `src/commonMain/.../ContextMenu.kt` | TODO |

**Phase 2 Total: 22 tasks | 8 days**

---

## PHASE 3: SECURITY & FORMS (P2) - Agent 3

### Sprint 3.1: Password Manager (5 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P3-01 | Add Credential table to BrowserDatabase.sq | `src/commonMain/sqldelight/.../BrowserDatabase.sq` | TODO |
| P3-02 | Create CredentialStore.kt expect interface | `src/commonMain/.../CredentialStore.kt` | TODO |
| P3-03 | Create CredentialStore.android.kt (Keystore) | `src/androidMain/.../CredentialStore.android.kt` | TODO |
| P3-04 | Create LoginFormDetector.kt (JS injection) | `src/commonMain/.../LoginFormDetector.kt` | TODO |
| P3-05 | Create login-form-detector.js | `src/commonMain/resources/login-form-detector.js` | TODO |
| P3-06 | Create SavePasswordDialog.kt | `src/commonMain/.../SavePasswordDialog.kt` | TODO |
| P3-07 | Create PasswordManagerScreen.kt | `src/commonMain/.../PasswordManagerScreen.kt` | TODO |
| P3-08 | Add @JavascriptInterface for form submit | `src/androidMain/.../WebViewJsBridge.kt` | TODO |
| P3-09 | Implement biometric unlock option | `src/androidMain/.../CredentialStore.android.kt` | TODO |
| P3-10 | Add password autofill on page load | `src/commonMain/.../LoginFormDetector.kt` | TODO |

### Sprint 3.2: Autofill (5 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P3-11 | Add AutofillProfile table to schema | `src/commonMain/sqldelight/.../BrowserDatabase.sq` | TODO |
| P3-12 | Create AutofillProfile.kt data model | `src/commonMain/.../AutofillProfile.kt` | TODO |
| P3-13 | Create AutofillRepository.kt | `src/commonMain/.../AutofillRepository.kt` | TODO |
| P3-14 | Create AutofillFieldDetector.kt | `src/commonMain/.../AutofillFieldDetector.kt` | TODO |
| P3-15 | Create autofill-detector.js | `src/commonMain/resources/autofill-detector.js` | TODO |
| P3-16 | Create AutofillSuggestionPopup.kt | `src/commonMain/.../AutofillSuggestionPopup.kt` | TODO |
| P3-17 | Create AutofillSettingsScreen.kt | `src/commonMain/.../AutofillSettingsScreen.kt` | TODO |
| P3-18 | Implement card number encryption | `src/androidMain/.../CredentialStore.android.kt` | TODO |

### Sprint 3.3: Download Manager Enhancement (3 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P3-19 | Create EnhancedDownloadManager.kt | `src/commonMain/.../EnhancedDownloadManager.kt` | TODO |
| P3-20 | Implement pause/resume with Range headers | `src/androidMain/.../EnhancedDownloadManager.android.kt` | TODO |
| P3-21 | Implement retry failed downloads | `src/commonMain/.../EnhancedDownloadManager.kt` | TODO |
| P3-22 | Add "Open folder" action | `src/androidMain/.../EnhancedDownloadManager.android.kt` | TODO |
| P3-23 | Update DownloadQueue.kt with new states | `src/commonMain/.../DownloadQueue.kt` | TODO |
| P3-24 | Add speed & ETA indicators to UI | `src/commonMain/.../DownloadItem.kt` | TODO |

**Phase 3 Total: 24 tasks | 13 days**

---

## PHASE 4: ADVANCED FEATURES (P2) - Agent 4

### Sprint 4.1: Page Translation (5 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P4-01 | Create TranslationService.kt expect interface | `src/commonMain/.../TranslationService.kt` | TODO |
| P4-02 | Create TranslationService.android.kt (ML Kit) | `src/androidMain/.../TranslationService.android.kt` | TODO |
| P4-03 | Add ML Kit Translation dependency | `build.gradle.kts` | TODO |
| P4-04 | Implement language detection | `src/androidMain/.../TranslationService.android.kt` | TODO |
| P4-05 | Create TranslationSettingsScreen.kt | `src/commonMain/.../TranslationSettingsScreen.kt` | TODO |
| P4-06 | Add "Translate page" button to toolbar | `src/commonMain/.../AddressBar.kt` | TODO |
| P4-07 | Add auto-translate prompt for foreign pages | `src/commonMain/.../BrowserScreen.kt` | TODO |

### Sprint 4.2: Tab Groups Enhancement (2 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P4-08 | Verify TabGroupModel.kt functionality | `src/commonMain/.../TabGroupModel.kt` | EXISTING |
| P4-09 | Verify TabGroupDialog.kt functionality | `src/commonMain/.../TabGroupDialog.kt` | EXISTING |
| P4-10 | Add collapse/expand animation | `src/commonMain/.../TabGroupDialog.kt` | TODO |
| P4-11 | Add drag-drop to create groups | `src/commonMain/.../SpatialTabSwitcher.kt` | TODO |
| P4-12 | Add group colors in tab strip | `src/commonMain/.../TabItem.kt` | TODO |

### Sprint 4.3: User Scripts (5 days)
| ID | Task | File | Status |
|----|------|------|--------|
| P4-13 | Add UserScript table to BrowserDatabase.sq | `src/commonMain/sqldelight/.../BrowserDatabase.sq` | TODO |
| P4-14 | Create UserScript.kt data model | `src/commonMain/.../UserScript.kt` | TODO |
| P4-15 | Create UserScriptManager.kt | `src/commonMain/.../UserScriptManager.kt` | TODO |
| P4-16 | Implement glob-to-regex pattern matching | `src/commonMain/.../UserScriptManager.kt` | TODO |
| P4-17 | Implement script injection timing | `src/commonMain/.../UserScriptManager.kt` | TODO |
| P4-18 | Create UserScriptsScreen.kt | `src/commonMain/.../UserScriptsScreen.kt` | TODO |
| P4-19 | Create UserScriptInstallDialog.kt | `src/commonMain/.../UserScriptInstallDialog.kt` | TODO |
| P4-20 | Parse Tampermonkey @match syntax | `src/commonMain/.../UserScriptManager.kt` | TODO |

**Phase 4 Total: 20 tasks | 12 days**

---

## PHASE 5: SYNC & DEVTOOLS (P3) - Deferred

### Sprint 5.1: Sync System (10 days) - FUTURE
| ID | Task | File | Status |
|----|------|------|--------|
| P5-01 | Add SyncMetadata table | `src/commonMain/sqldelight/.../BrowserDatabase.sq` | FUTURE |
| P5-02 | Create SyncService.kt interface | `src/commonMain/.../SyncService.kt` | FUTURE |
| P5-03 | Create FirebaseSyncService.kt | `src/androidMain/.../FirebaseSyncService.kt` | FUTURE |
| P5-04 | Implement bookmark sync | `src/commonMain/.../SyncService.kt` | FUTURE |
| P5-05 | Implement history sync | `src/commonMain/.../SyncService.kt` | FUTURE |
| P5-06 | Create SyncSettingsScreen.kt | `src/commonMain/.../SyncSettingsScreen.kt` | FUTURE |

### Sprint 5.2: Basic DevTools (8 days) - FUTURE
| ID | Task | File | Status |
|----|------|------|--------|
| P5-07 | Create ConsoleLogger.kt | `src/commonMain/.../ConsoleLogger.kt` | FUTURE |
| P5-08 | Create NetworkInspector.kt | `src/commonMain/.../NetworkInspector.kt` | FUTURE |
| P5-09 | Create DevToolsScreen.kt | `src/commonMain/.../DevToolsScreen.kt` | FUTURE |

### Sprint 5.3: Chromecast (4 days) - FUTURE
| ID | Task | File | Status |
|----|------|------|--------|
| P5-10 | Add Google Cast SDK dependency | `build.gradle.kts` | FUTURE |
| P5-11 | Create CastManager.kt | `src/androidMain/.../CastManager.kt` | FUTURE |
| P5-12 | Add cast button to toolbar | `src/commonMain/.../AddressBar.kt` | FUTURE |

**Phase 5 Total: 12 tasks | 22 days (DEFERRED)**

---

## SWARM EXECUTION PLAN

### Parallel Agent Assignment

| Agent | Phase | Focus Area | Tasks | Duration |
|-------|-------|------------|-------|----------|
| Agent 1 | P0 | Quick Wins (Find, Print, Keys, Screenshot, Text) | 24 | 5.5 days |
| Agent 2 | P1 | Core (Private, Context Menu, Reader, Reading List) | 22 | 8 days |
| Agent 3 | P2 | Security (Password, Autofill, Downloads) | 24 | 13 days |
| Agent 4 | P2 | Advanced (Translation, Tab Groups, User Scripts) | 20 | 12 days |

### Timeline (Parallel Execution)

```
Week 1-2:  [Agent 1: P0 Complete] ─────────────────────────►
           [Agent 2: P1 Starting] ─────────────────────────►

Week 3-4:  [Agent 2: P1 Complete] ─────────────────────────►
           [Agent 3: P2a Starting] ────────────────────────►
           [Agent 4: P2b Starting] ────────────────────────►

Week 5-8:  [Agent 3: P2a Complete] ────────────────────────►
           [Agent 4: P2b Complete] ────────────────────────►

Week 9:    Integration & Testing
```

**Total Parallel Duration: 9 weeks**
**Parity Achieved: 60% → 92%**

---

## AUTO-IMPLEMENTATION COMMANDS

```bash
# Agent 1 - Phase 1 (Quick Wins)
/i.implement P1-01 through P1-24 --swarm --auto

# Agent 2 - Phase 2 (Core Features)
/i.implement P2-01 through P2-22 --swarm --auto

# Agent 3 - Phase 3 (Security & Forms)
/i.implement P3-01 through P3-24 --swarm --auto

# Agent 4 - Phase 4 (Advanced)
/i.implement P4-01 through P4-20 --swarm --auto
```

---

## Files Summary

### New Files to Create: 35
### Existing Files to Modify: 12
### Database Tables to Add: 5
### JS Files to Create: 3
### Total Lines of Code (Est): ~4,500

---

**Generated:** 2026-01-22
**Plan:** WebAvanue-ChromeParity-Plan-260122.md
**Mode:** .tasks .implement .swarm .auto

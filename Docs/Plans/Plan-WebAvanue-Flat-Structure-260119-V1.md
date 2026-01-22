# Implementation Plan: WebAvanue Flat KMP Structure Migration

**Date:** 2026-01-19 | **Version:** V1 | **Branch:** legacy-consolidation

## Overview

| Attribute | Value |
|-----------|-------|
| **Module** | Modules/WebAvanue |
| **Files to Migrate** | 152 (135 universal + 17 coredata) |
| **Current Directories** | 49 nested directories |
| **Target Structure** | Flat package root |
| **Estimated Tasks** | 8 phases |

---

## Current Structure Analysis

### Package Roots (2)
1. `com.augmentalis.webavanue` - Main package (150 files)
2. `com.augmentalis.Avanues.web.universal` - Legacy package (2 files)

### Nested Directory Depth (Max 6 levels)
```
presentation/ui/browser/commandbar/  (4 levels)
presentation/ui/settings/components/ (4 levels)
presentation/viewmodel/tab/          (3 levels)
presentation/ui/theme/abstraction/   (4 levels)
presentation/ui/theme/avamagic/      (4 levels)
presentation/ui/theme/webavanue/     (4 levels)
```

### Files by Category

| Category | Count | Current Path Pattern |
|----------|-------|---------------------|
| UI Screens | 25 | `presentation/ui/{feature}/*.kt` |
| UI Components | 18 | `presentation/ui/components/*.kt` |
| Command Bar | 7 | `presentation/ui/browser/commandbar/*.kt` |
| ViewModels | 12 | `presentation/viewmodel/*.kt` |
| Theme | 11 | `presentation/ui/theme/**/*.kt` |
| Domain Models | 10 | `domain/model/*.kt` |
| Controllers | 4 | `presentation/controller/*.kt` |
| XR | 9 | `xr/*.kt` + `presentation/ui/xr/*.kt` |
| Voice | 10 | `voice/*.kt` + `voiceos/*.kt` |
| Platform | 6 | `platform/*.kt` |
| Repository | 4 | `data/repository/*.kt` |
| Other | 36 | Various |

---

## Target Flat Structure

All files will be placed at package root with naming suffixes:

```
com/augmentalis/webavanue/
├── BrowserScreen.kt           # was presentation/ui/browser/BrowserScreen.kt
├── BrowserScreenDialogs.kt    # was presentation/ui/browser/screen/BrowserScreenDialogs.kt
├── AddressBarComponent.kt     # was presentation/ui/browser/AddressBar.kt
├── CommandBarComponent.kt     # renamed from BottomCommandBar.kt
├── CommandBarButtons.kt       # was presentation/ui/browser/commandbar/CommandBarButtons.kt
├── TabViewModel.kt            # stays same (already at viewmodel root)
├── TabBarComponent.kt         # was presentation/ui/tab/TabBar.kt
├── SettingsScreen.kt          # stays same
├── OceanTheme.kt             # was presentation/ui/theme/OceanTheme.kt
├── AppThemeConfig.kt         # was ThemeConfig.kt
├── ...
```

### Naming Convention (Suffixes)

| Suffix | Purpose | Example |
|--------|---------|---------|
| `*Screen.kt` | Full screen composables | `BrowserScreen.kt`, `SettingsScreen.kt` |
| `*Component.kt` | Reusable UI components | `AddressBarComponent.kt`, `TabBarComponent.kt` |
| `*Dialog.kt` | Dialog composables | `AddBookmarkDialog.kt`, `SecurityDialogs.kt` |
| `*ViewModel.kt` | ViewModels | `TabViewModel.kt`, `SettingsViewModel.kt` |
| `*State.kt` | State data classes | `BrowserUiState.kt`, `SecurityState.kt` |
| `*Model.kt` | Domain models | `TabModel.kt`, `FavoriteModel.kt` |
| `*Repository.kt` | Repository interfaces | `BrowserRepository.kt` |
| `*RepositoryImpl.kt` | Repository implementations | `BrowserRepositoryImpl.kt` |
| `*Manager.kt` | Manager classes | `SessionManager.kt`, `ScreenshotManager.kt` |
| `*Handler.kt` | Handler classes | `VoiceCommandHandler.kt` |
| `*Service.kt` | Service classes | `VoiceCommandService.kt` |
| `*Controller.kt` | Controllers | `WebViewController.kt` |
| `*Theme.kt` | Theme definitions | `OceanTheme.kt`, `AppTheme.kt` |
| `*Colors.kt` | Color definitions | `AvaMagicColors.kt` |
| `*Typography.kt` | Typography definitions | `AppTypography.kt` |
| `*Validation.kt` | Validation utilities | `UrlValidation.kt` |
| `*Error.kt` | Error types | `DownloadError.kt`, `TabError.kt` |
| `*Utils.kt` | Utility classes | `VuidGenerator.kt` → `VuidUtils.kt` |

---

## Migration Phases

### Phase 1: Backup and Prepare

| # | Task | Type |
|---|------|------|
| 1.1 | Create backup branch `webavanue-pre-flat-backup` | Git |
| 1.2 | Document all current imports in affected files | Research |
| 1.3 | Create archive of current structure | Archive |

### Phase 2: Migrate Domain Layer (coredata)

**Target:** `coredata/src/commonMain/kotlin/com/augmentalis/webavanue/`

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 2.1 | `data/repository/BrowserRepositoryImpl.kt` | `BrowserRepositoryImpl.kt` | Move |
| 2.2 | `data/repository/DbMappers.kt` | `DbMappers.kt` | Move |
| 2.3 | `data/util/TransactionHelper.kt` | `TransactionHelper.kt` | Move |
| 2.4 | `domain/manager/PrivateBrowsingManager.kt` | `PrivateBrowsingManager.kt` | Move |
| 2.5 | `domain/manager/SessionManager.kt` | `SessionManager.kt` | Move |
| 2.6 | `domain/model/BrowserSettings.kt` | `BrowserSettingsModel.kt` | Move+Rename |
| 2.7 | `domain/model/Download.kt` | `DownloadModel.kt` | Move+Rename |
| 2.8 | `domain/model/Favorite.kt` | `FavoriteModel.kt` | Move+Rename |
| 2.9 | `domain/model/HistoryEntry.kt` | `HistoryEntryModel.kt` | Move+Rename |
| 2.10 | `domain/model/Session.kt` | `SessionModel.kt` | Move+Rename |
| 2.11 | `domain/model/SitePermission.kt` | `SitePermissionModel.kt` | Move+Rename |
| 2.12 | `domain/model/Tab.kt` | `TabModel.kt` | Move+Rename |
| 2.13 | `domain/model/TabGroup.kt` | `TabGroupModel.kt` | Move+Rename |
| 2.14 | `domain/repository/BrowserRepository.kt` | `BrowserRepository.kt` | Move |
| 2.15 | `domain/state/SettingsStateMachine.kt` | `SettingsStateMachine.kt` | Move |
| 2.16 | `platform/WebView.kt` | `WebViewPlatform.kt` | Move+Rename |
| 2.17 | `util/VuidGenerator.kt` | `VuidUtils.kt` | Move+Rename |

### Phase 3: Migrate Presentation Layer - ViewModels

**Target:** `universal/src/commonMain/kotlin/com/augmentalis/webavanue/`

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 3.1 | `presentation/viewmodel/DownloadViewModel.kt` | `DownloadViewModel.kt` | Move |
| 3.2 | `presentation/viewmodel/FavoriteViewModel.kt` | `FavoriteViewModel.kt` | Move |
| 3.3 | `presentation/viewmodel/HistoryViewModel.kt` | `HistoryViewModel.kt` | Move |
| 3.4 | `presentation/viewmodel/NewTabUrls.kt` | `NewTabUrls.kt` | Move |
| 3.5 | `presentation/viewmodel/SecurityViewModel.kt` | `SecurityViewModel.kt` | Move |
| 3.6 | `presentation/viewmodel/SettingsViewModel.kt` | `SettingsViewModel.kt` | Move |
| 3.7 | `presentation/viewmodel/TabViewModel.kt` | `TabViewModel.kt` | Move |
| 3.8 | `presentation/viewmodel/TabViewModelPrivateBrowsing.kt` | `TabViewModelPrivateBrowsing.kt` | Move |
| 3.9 | `presentation/viewmodel/tab/TabFindInPageOps.kt` | `TabFindInPageOps.kt` | Move |
| 3.10 | `presentation/viewmodel/tab/TabReadingModeOps.kt` | `TabReadingModeOps.kt` | Move |
| 3.11 | `presentation/viewmodel/tab/TabSessionManager.kt` | `TabSessionManager.kt` | Move |
| 3.12 | `presentation/viewmodel/tab/TabUiModels.kt` | `TabUiModels.kt` | Move |
| 3.13 | `presentation/viewmodel/tab/TabViewControls.kt` | `TabViewControls.kt` | Move |

### Phase 4: Migrate Presentation Layer - UI Screens

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 4.1 | `presentation/ui/browser/BrowserScreen.kt` | `BrowserScreen.kt` | Move |
| 4.2 | `presentation/ui/browser/screen/BrowserScreenDialogs.kt` | `BrowserScreenDialogs.kt` | Move |
| 4.3 | `presentation/ui/browser/screen/BrowserScreenState.kt` | `BrowserScreenState.kt` | Move |
| 4.4 | `presentation/ui/browser/screen/BrowserTextCommands.kt` | `BrowserTextCommands.kt` | Move |
| 4.5 | `presentation/ui/settings/SettingsScreen.kt` | `SettingsScreen.kt` | Move |
| 4.6 | `presentation/ui/settings/SitePermissionsScreen.kt` | `SitePermissionsScreen.kt` | Move |
| 4.7 | `presentation/ui/bookmark/BookmarkListScreen.kt` | `BookmarkListScreen.kt` | Move |
| 4.8 | `presentation/ui/download/DownloadListScreen.kt` | `DownloadListScreen.kt` | Move |
| 4.9 | `presentation/ui/history/HistoryScreen.kt` | `HistoryScreen.kt` | Move |
| 4.10 | `presentation/ui/xr/XRSettingsScreen.kt` | `XRSettingsScreen.kt` | Move |
| 4.11 | `presentation/ui/demo/ARLayoutPreview.kt` | `ARLayoutPreview.kt` | Move |

### Phase 5: Migrate Presentation Layer - UI Components

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 5.1 | `presentation/ui/browser/AddressBar.kt` | `AddressBarComponent.kt` | Move+Rename |
| 5.2 | `presentation/ui/browser/BottomCommandBar.kt` | `BottomCommandBarLegacy.kt` | Move+Rename |
| 5.3 | `presentation/ui/browser/commandbar/BottomCommandBar.kt` | `CommandBarComponent.kt` | Move+Rename |
| 5.4 | `presentation/ui/browser/commandbar/CommandBarButtons.kt` | `CommandBarButtons.kt` | Move |
| 5.5 | `presentation/ui/browser/commandbar/CommandBarInputComponents.kt` | `CommandBarInputComponents.kt` | Move |
| 5.6 | `presentation/ui/browser/commandbar/CommandBarLevel.kt` | `CommandBarLevel.kt` | Move |
| 5.7 | `presentation/ui/browser/commandbar/CommandBarLevelComposables.kt` | `CommandBarLevelComposables.kt` | Move |
| 5.8 | `presentation/ui/browser/commandbar/HorizontalCommandBarLayout.kt` | `HorizontalCommandBarLayout.kt` | Move |
| 5.9 | `presentation/ui/browser/commandbar/VerticalCommandBarLayout.kt` | `VerticalCommandBarLayout.kt` | Move |
| 5.10 | `presentation/ui/browser/CommandBarAutoHide.kt` | `CommandBarAutoHide.kt` | Move |
| 5.11 | `presentation/ui/browser/WebViewContainer.kt` | `WebViewContainer.kt` | Move |
| 5.12 | `presentation/ui/browser/WebViewPoolManager.kt` | `WebViewPoolManager.kt` | Move |
| 5.13 | `presentation/ui/browser/FavoriteItem.kt` | `FavoriteItemComponent.kt` | Move+Rename |
| 5.14 | `presentation/ui/browser/FavoritesBar.kt` | `FavoritesBarComponent.kt` | Move+Rename |
| 5.15 | `presentation/ui/browser/FindInPageBar.kt` | `FindInPageBarComponent.kt` | Move+Rename |
| 5.16 | `presentation/ui/browser/ReadingModeView.kt` | `ReadingModeComponent.kt` | Move+Rename |
| 5.17 | `presentation/ui/browser/DesktopModeIndicator.kt` | `DesktopModeIndicator.kt` | Move |
| 5.18 | `presentation/ui/browser/XROverlay.kt` | `XROverlayComponent.kt` | Move+Rename |
| 5.19 | `presentation/ui/tab/TabBar.kt` | `TabBarComponent.kt` | Move+Rename |
| 5.20 | `presentation/ui/tab/TabCounterBadge.kt` | `TabCounterBadge.kt` | Move |
| 5.21 | `presentation/ui/tab/TabItem.kt` | `TabItemComponent.kt` | Move+Rename |
| 5.22 | `presentation/ui/tab/TabSwitcherView.kt` | `TabSwitcherComponent.kt` | Move+Rename |
| 5.23 | `presentation/ui/bookmark/AddBookmarkDialog.kt` | `AddBookmarkDialog.kt` | Move |
| 5.24 | `presentation/ui/bookmark/BookmarkItem.kt` | `BookmarkItemComponent.kt` | Move+Rename |
| 5.25 | `presentation/ui/download/DownloadItem.kt` | `DownloadItemComponent.kt` | Move+Rename |
| 5.26 | `presentation/ui/history/HistoryItem.kt` | `HistoryItemComponent.kt` | Move+Rename |
| 5.27 | `presentation/ui/components/BlurSurface.kt` | `BlurSurface.kt` | Move |
| 5.28 | `presentation/ui/components/GlassmorphicComponents.kt` | `GlassmorphicComponents.kt` | Move |
| 5.29 | `presentation/ui/components/NetworkStatusIndicator.kt` | `NetworkStatusIndicator.kt` | Move |
| 5.30 | `presentation/ui/components/OceanComponents.kt` | `OceanComponentsUI.kt` | Move+Rename |
| 5.31 | `presentation/ui/components/OceanDialog.kt` | `OceanDialog.kt` | Move |
| 5.32 | `presentation/ui/components/OceanThemeExtensions.kt` | `OceanThemeExtensions.kt` | Move |
| 5.33 | `presentation/ui/settings/components/CollapsibleSectionHeader.kt` | `CollapsibleSectionHeader.kt` | Move |
| 5.34 | `presentation/ui/settings/components/SettingComponents.kt` | `SettingComponents.kt` | Move |
| 5.35 | `presentation/ui/settings/components/SpecializedSettingComponents.kt` | `SpecializedSettingComponents.kt` | Move |
| 5.36 | `presentation/ui/spatial/SpatialFavoritesShelf.kt` | `SpatialFavoritesShelf.kt` | Move |
| 5.37 | `presentation/ui/spatial/SpatialTabSwitcher.kt` | `SpatialTabSwitcher.kt` | Move |
| 5.38 | `presentation/ui/layout/ArcLayout.kt` | `ArcLayout.kt` | Move |
| 5.39 | `presentation/ui/effects/BlurEffect.kt` | `BlurEffect.kt` | Move |

### Phase 6: Migrate Presentation Layer - Dialogs & XR UI

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 6.1 | `presentation/ui/browser/AddToFavoritesDialog.kt` | `AddToFavoritesDialog.kt` | Move |
| 6.2 | `presentation/ui/browser/BasicAuthDialog.kt` | `BasicAuthDialog.kt` | Move |
| 6.3 | `presentation/ui/dialogs/SessionRestoreDialog.kt` | `SessionRestoreDialog.kt` | Move |
| 6.4 | `presentation/ui/download/AskDownloadLocationDialog.kt` | `AskDownloadLocationDialog.kt` | Move |
| 6.5 | `presentation/ui/security/SecurityDialogs.kt` | `SecurityDialogs.kt` | Move |
| 6.6 | `presentation/ui/tab/TabGroupAssignmentDialog.kt` | `TabGroupAssignmentDialog.kt` | Move |
| 6.7 | `presentation/ui/tab/TabGroupDialog.kt` | `TabGroupDialog.kt` | Move |
| 6.8 | `presentation/ui/screenshot/ScreenshotDialog.kt` | `ScreenshotDialog.kt` | Move |
| 6.9 | `presentation/ui/xr/XRPerformanceWarning.kt` | `XRPerformanceWarning.kt` | Move |
| 6.10 | `presentation/ui/xr/XRPermissionDialog.kt` | `XRPermissionDialog.kt` | Move |
| 6.11 | `presentation/ui/xr/XRSessionIndicator.kt` | `XRSessionIndicator.kt` | Move |
| 6.12 | `presentation/ui/security/SecurityState.kt` | `SecurityState.kt` | Move |

### Phase 7: Migrate Theme, Design, and State

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 7.1 | `presentation/ui/theme/AppTheme.kt` | `AppTheme.kt` | Move |
| 7.2 | `presentation/ui/theme/GlassmorphismModifiers.kt` | `GlassmorphismModifiers.kt` | Move |
| 7.3 | `presentation/ui/theme/OceanTheme.kt` | `OceanTheme.kt` | Move |
| 7.4 | `presentation/ui/theme/ThemeConfig.kt` | `ThemeConfig.kt` | Move |
| 7.5 | `presentation/ui/theme/abstraction/AppColors.kt` | `AppColors.kt` | Move |
| 7.6 | `presentation/ui/theme/abstraction/AppTypography.kt` | `AppTypography.kt` | Move |
| 7.7 | `presentation/ui/theme/avamagic/AvaMagicColors.kt` | `AvaMagicColors.kt` | Move |
| 7.8 | `presentation/ui/theme/avamagic/AvaMagicTypography.kt` | `AvaMagicTypography.kt` | Move |
| 7.9 | `presentation/ui/theme/webavanue/WebAvanueColors.kt` | `WebAvanueColors.kt` | Move |
| 7.10 | `presentation/ui/theme/webavanue/WebAvanueTypography.kt` | `WebAvanueTypography.kt` | Move |
| 7.11 | `presentation/design/ComponentProvider.kt` | `ComponentProvider.kt` | Move |
| 7.12 | `presentation/design/OceanComponents.kt` | `OceanComponentsDesign.kt` | Move+Rename |
| 7.13 | `presentation/design/OceanDesignTokens.kt` | `OceanDesignTokens.kt` | Move |
| 7.14 | `presentation/navigation/Screen.kt` | `NavigationScreen.kt` | Move+Rename |
| 7.15 | `presentation/state/BrowserUiState.kt` | `BrowserUiState.kt` | Move |

### Phase 8: Migrate Remaining Files

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 8.1 | `presentation/controller/CommonWebViewController.kt` | `CommonWebViewController.kt` | Move |
| 8.2 | `presentation/controller/GestureCoordinateResolver.kt` | `GestureCoordinateResolver.kt` | Move |
| 8.3 | `presentation/controller/GestureMapper.kt` | `GestureMapper.kt` | Move |
| 8.4 | `presentation/controller/WebViewController.kt` | `WebViewController.kt` | Move |
| 8.5 | `commands/ActionResult.kt` | `ActionResult.kt` | Move |
| 8.6 | `commands/WebAvanueActionMapper.kt` | `WebAvanueActionMapper.kt` | Move |
| 8.7 | `domain/errors/DownloadError.kt` | `DownloadError.kt` | Move |
| 8.8 | `domain/errors/TabError.kt` | `TabError.kt` | Move |
| 8.9 | `domain/model/NewTabUrl.kt` | `NewTabUrlModel.kt` | Move+Rename |
| 8.10 | `domain/utils/RetryPolicy.kt` | `RetryPolicy.kt` | Move |
| 8.11 | `domain/validation/SettingsValidation.kt` | `SettingsValidation.kt` | Move |
| 8.12 | `domain/validation/UrlValidation.kt` | `UrlValidation.kt` | Move |
| 8.13 | `download/DownloadQueue.kt` | `DownloadQueue.kt` | Move |
| 8.14 | `platform/DownloadFilePickerLauncher.kt` | `DownloadFilePickerLauncher.kt` | Move |
| 8.15 | `platform/DownloadPathValidator.kt` | `DownloadPathValidator.kt` | Move |
| 8.16 | `platform/DownloadPermissionManager.kt` | `DownloadPermissionManager.kt` | Move |
| 8.17 | `platform/NetworkChecker.kt` | `NetworkChecker.kt` | Move |
| 8.18 | `platform/ValidationResult.kt` | `ValidationResult.kt` | Move |
| 8.19 | `platform/WebViewEngine.kt` | `WebViewEngine.kt` | Move |
| 8.20 | `screenshot/ScreenshotCapture.kt` | `ScreenshotCapture.kt` | Move |
| 8.21 | `screenshot/ScreenshotIntegrationExample.kt` | `ScreenshotIntegrationExample.kt` | Move |
| 8.22 | `screenshot/ScreenshotManager.kt` | `ScreenshotManager.kt` | Move |
| 8.23 | `util/BookmarkImportExport.kt` | `BookmarkImportExport.kt` | Move |
| 8.24 | `util/FilePicker.kt` | `FilePicker.kt` | Move |
| 8.25 | `util/ReadingModeExtractor.kt` | `ReadingModeExtractor.kt` | Move |
| 8.26 | `util/UrlEncoder.kt` | `UrlEncoder.kt` | Move |
| 8.27 | `utils/Logger.kt` | `Logger.kt` | Move |
| 8.28 | `voice/DeviceAdaptiveParameters.kt` | `DeviceAdaptiveParameters.kt` | Move |
| 8.29 | `voice/VoiceCommandHandler.kt` | `VoiceCommandHandler.kt` | Move |
| 8.30 | `voice/VoiceCommandsDialog.kt` | `VoiceCommandsDialog.kt` | Move |
| 8.31 | `voice/VoiceCommandService.kt` | `VoiceCommandService.kt` | Move |
| 8.32 | `voice/VoiceDialogAutoClose.kt` | `VoiceDialogAutoClose.kt` | Move |
| 8.33 | `voiceos/BrowserVoiceOSCallback.kt` | `BrowserVoiceOSCallback.kt` | Move |
| 8.34 | `voiceos/DOMElement.kt` | `DOMElement.kt` | Move |
| 8.35 | `voiceos/DOMScraperBridge.kt` | `DOMScraperBridge.kt` | Move |
| 8.36 | `voiceos/VoiceCommandGenerator.kt` | `VoiceCommandGenerator.kt` | Move |
| 8.37 | `voiceos/VoiceOSWebCallback.kt` | `VoiceOSWebCallback.kt` | Move |
| 8.38 | `xr/CommonCameraManager.kt` | `XRCameraManager.kt` | Move+Rename |
| 8.39 | `xr/CommonPerformanceMonitor.kt` | `XRPerformanceMonitor.kt` | Move+Rename |
| 8.40 | `xr/CommonPermissionManager.kt` | `XRPermissionManager.kt` | Move+Rename |
| 8.41 | `xr/CommonSessionManager.kt` | `XRSessionManager.kt` | Move+Rename |
| 8.42 | `xr/CommonXRManager.kt` | `XRManager.kt` | Move+Rename |
| 8.43 | `xr/XRState.kt` | `XRState.kt` | Move |

### Phase 9: Legacy Package Migration

| # | Current Path | New Name | Type |
|---|--------------|----------|------|
| 9.1 | `com/augmentalis/Avanues/web/universal/privacy/AdBlocker.kt` | `AdBlocker.kt` → `com.augmentalis.webavanue` | Move+RePackage |
| 9.2 | `com/augmentalis/Avanues/web/universal/privacy/TrackerBlocker.kt` | `TrackerBlocker.kt` → `com.augmentalis.webavanue` | Move+RePackage |

### Phase 10: Cleanup and Verify

| # | Task | Type |
|---|------|------|
| 10.1 | Delete empty directories from `universal/src/commonMain/kotlin/` | Cleanup |
| 10.2 | Delete empty directories from `coredata/src/commonMain/kotlin/` | Cleanup |
| 10.3 | Update all import statements across the module | Refactor |
| 10.4 | Run gradle sync | Verify |
| 10.5 | Fix any compilation errors | Fix |
| 10.6 | Archive original structure documentation | Archive |

---

## Import Updates Strategy

### Package Changes Summary

| Original Package | New Package |
|------------------|-------------|
| `com.augmentalis.webavanue.presentation.ui.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.presentation.viewmodel.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.presentation.controller.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.presentation.design.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.presentation.navigation.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.presentation.state.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.domain.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.data.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.voice.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.voiceos.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.xr.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.platform.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.commands.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.download.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.screenshot.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.util.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.webavanue.utils.*` | `com.augmentalis.webavanue` |
| `com.augmentalis.Avanues.web.universal.*` | `com.augmentalis.webavanue` |

### Example Import Update

```kotlin
// Before:
import com.augmentalis.webavanue.presentation.ui.browser.BrowserScreen
import com.augmentalis.webavanue.presentation.ui.browser.commandbar.BottomCommandBar
import com.augmentalis.webavanue.presentation.viewmodel.TabViewModel
import com.augmentalis.webavanue.domain.model.Tab

// After:
import com.augmentalis.webavanue.BrowserScreen
import com.augmentalis.webavanue.CommandBarComponent
import com.augmentalis.webavanue.TabViewModel
import com.augmentalis.webavanue.TabModel
```

---

## File Rename Conflicts

Files that need renaming to avoid conflicts:

| Conflict | Resolution |
|----------|------------|
| Two `BottomCommandBar.kt` | Legacy → `BottomCommandBarLegacy.kt`, New → `CommandBarComponent.kt` |
| Two `OceanComponents.kt` | UI → `OceanComponentsUI.kt`, Design → `OceanComponentsDesign.kt` |
| `Tab.kt` vs other Tab files | → `TabModel.kt` |
| `Screen.kt` generic name | → `NavigationScreen.kt` |

---

## Exit Criteria

- [ ] All 152 files moved to package root
- [ ] All package declarations updated to `com.augmentalis.webavanue`
- [ ] All imports updated across the module
- [ ] No nested directories remain (except test directories)
- [ ] Gradle sync succeeds
- [ ] No compilation errors

---

## Notes

- **Preserve Git History:** Use `git mv` for file moves
- **Incremental Migration:** Can be done in batches if needed
- **Test After Each Phase:** Verify compilation after each phase
- **Platform-Specific Files:** androidMain/iosMain files follow same pattern

---

## Related Plans

- `Plan-Legacy-Consolidation-260119-V1.md` - Parent consolidation plan

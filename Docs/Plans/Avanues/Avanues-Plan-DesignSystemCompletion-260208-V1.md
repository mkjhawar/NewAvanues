# Avanues-Plan-DesignSystemCompletion-260208-V1

## Overview
Complete the unified design token system by: wiring responsive DisplayProfile detection,
migrating all OceanTheme consumers, deleting dead code, and ensuring responsive UI works.

## Priority Order
1. **DisplayProfile wiring** - makes responsive UI actually work (user priority)
2. **OceanTheme migration** - 17 WebAvanue files (mechanical)
3. **Dead file deletion** - 5 files with 0 consumers
4. **OceanComponents migration** - 2 consumers then delete
5. **Foundation cleanup** - delete OceanTheme.kt, OceanDesignTokens.kt

## Phase 1: DisplayProfile Wiring

### New: DisplayProfileResolver.kt (DesignSystem/commonMain)
Pure function: (widthPx, heightPx, densityDpi, isSmartGlass) → DisplayProfile
- Vuzix Blade/Shield (480x480, 640x360) + glass → GLASS_MICRO
- RealWear HMT/M400 (854x480) + glass → GLASS_COMPACT
- Nav520/M4000 (1280x720) + glass → GLASS_STANDARD
- XREAL/Z100 (1920x1080) + glass → GLASS_HD
- Phone screens → PHONE
- Tablet screens (sw >= 600dp) → TABLET

### Modified: MainActivity.kt
- Import DeviceCapabilityFactory + DisplayProfileResolver
- Detect display profile on launch
- Wrap with AvanueThemeProvider(displayProfile = detected) INSIDE the existing AvanueTheme

## Phase 2: OceanTheme → AvanueTheme.colors (17 WebAvanue files)

### Property Mapping
| OceanTheme | AvanueTheme.colors | Notes |
|---|---|---|
| .background | .background | Direct |
| .surface | .surface | Direct |
| .surfaceElevated | .surfaceElevated | Direct |
| .surfaceInput | .surfaceInput | Direct |
| .primary | .primary | Direct |
| .primaryDark | .primaryDark | Direct |
| .primaryLight | .primaryLight | Direct |
| .textPrimary | .textPrimary | Direct |
| .textSecondary | .textSecondary | Direct |
| .textTertiary | .textTertiary | Direct |
| .textDisabled | .textDisabled | Direct |
| .textOnPrimary | .textOnPrimary | Direct |
| .border | .border | Direct |
| .borderSubtle | .borderSubtle | Direct |
| .borderStrong | .borderStrong | Direct |
| .borderFocused | .primary | Alias |
| .success | .success | Direct |
| .warning | .warning | Direct |
| .error | .error | Direct |
| .info | .info | Direct |
| .iconActive | .textPrimary | Same value (E2E8F0) |
| .iconInactive | .textTertiary | Same value (94A3B8) |
| .iconOnPrimary | .onPrimary | Same value (White) |
| .starActive | .starActive | Direct |
| .voiceListening | .primary | Alias |
| .loading | .primary | Alias |
| .glassLight | Color(0x141E293B) | Keep literal |
| .glassMedium | Color(0x1F334155) | Keep literal |
| .glassHeavy | Color(0x33334155) | Keep literal |
| .glassBorder | Color(0x262563EB) | Keep literal |

### Files (17)
SessionRestoreDialog, TabSwitcherView, HorizontalCommandBarLayout,
VerticalCommandBarLayout, TabCounterBadge, ARLayoutPreview,
CommandBarInputComponents, CommandBarLevelComposables, BrowserScreen,
VoiceDialogAutoClose, BottomCommandBar, VoiceCommandHandler,
SpatialFavoritesShelf, CollapsibleSectionHeader, SpatialTabSwitcher,
OceanThemeExtensions (WebAvanue), GlassmorphicComponents (WebAvanue)

## Phase 3: Delete Dead Files (0 consumers)
- WebAvanue/OceanThemeExtensions.kt
- WebAvanue/GlassmorphicComponents.kt
- Foundation/OceanDesignTokens.kt
- Foundation/OceanTheme.kt
- Foundation/OceanThemeExtensions.kt (if exists)

## Phase 4: OceanComponents Migration
- AddressBar.kt + BrowserScreen.kt → inline M3 equivalents
- Delete OceanComponents.kt

## Phase 5 (Deferred): com.avanueui UI Component Consumers
These import actual UI components (not tokens) from com.avanueui:
- ChatScreen.kt: GlassSurface, GlassCard, OceanButton, etc.
- Settings providers: SettingsGroupCard, SettingsSwitchRow, etc.
- MainActivity.kt: com.avanueui.AvanueTheme (M3 wrapper)
→ Requires moving components to DesignSystem module (separate PR)

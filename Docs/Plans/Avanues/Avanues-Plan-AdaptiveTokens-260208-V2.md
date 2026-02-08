# Adaptive Token System + Full Deprecation Removal

**Module:** Avanues (AvanueUI/DesignSystem)
**Date:** 2026-02-08
**Branch:** 060226-1-consolidation-framework
**Status:** COMPLETED
**Predecessor:** Avanues-Plan-UnifiedDesignTokens-260208-V1.md

## Summary

Three tracks implemented:
1. **Track 1 - Adaptive Display Sizing**: DisplayProfile enum + Compose density override for smart glasses
2. **Track 2 - Deprecation Removal**: Migrated 12 consumer files (10 WebAvanue + 2 Chat), deleted 2 dead token files
3. **Track 3 - Global Instructions**: Updated MEMORY.md with token system rules

## Track 1: Files Created/Modified

| File | Action |
|------|--------|
| `DesignSystem/.../tokens/DisplayProfile.kt` | CREATED - DisplayProfile enum (6 profiles) + LayoutStrategy enum |
| `DesignSystem/.../tokens/DisplayUtils.kt` | CREATED - Touch target safety + display helpers |
| `DesignSystem/.../theme/AvanueTheme.kt` | MODIFIED - Added LocalDisplayProfile + density override |
| `DesignSystem/.../tokens/ResponsiveTokens.kt` | MODIFIED - Added glass breakpoints + margins |

### DisplayProfile Values
| Profile | densityScale | fontScale | minTouch | Layout |
|---------|-------------|-----------|----------|--------|
| GLASS_MICRO | 0.625 | 0.75 | 36dp | SINGLE_PANE_PAGINATED |
| GLASS_COMPACT | 0.75 | 0.85 | 40dp | SINGLE_PANE_SCROLL |
| GLASS_STANDARD | 0.875 | 0.9 | 44dp | SINGLE_PANE_SCROLL |
| PHONE | 1.0 | 1.0 | 48dp | ADAPTIVE |
| TABLET | 1.0 | 1.0 | 48dp | LIST_DETAIL |
| GLASS_HD | 0.9 | 0.95 | 48dp | ADAPTIVE |

## Track 2: Migration Summary

### WebAvanue (10 consumer files migrated in V1 session)
CommandExecutionFeedback, VoiceCommandStatusBar, WebAppWhitelistScreen, WebVoiceCommandsDialog,
AddressBar, DOMScrapingIndicator, VoiceCommandsDialog, NetworkStatusIndicator, FindInPageBar, CommandBarButtons

### WebAvanue additional (this session)
OceanComponents.kt - migrated internal token refs to AvanueTheme.colors

### AI/Chat (2 files migrated)
ChatScreen.kt, MessageBubble.kt - ColorTokens/ShapeTokens/SizeTokens migrated

### Files Deleted
- `WebAvanue/.../OceanDesignTokens.kt` - 0 consumers
- `DesignSystem/.../avamagic/designsystem/DesignTokens.kt` - 0 consumers

### Files Modified (deprecated removed)
- `Foundation/.../OceanDesignTokens.kt` - @Deprecated removed (0 import consumers)
- `Foundation/.../OceanTheme.kt` - @Deprecated removed (0 import consumers)

### Files Kept (still have consumers)
- `WebAvanue/.../OceanComponents.kt` - used by AddressBar, BrowserScreen (tokens migrated internally)
- `WebAvanue/.../OceanThemeExtensions.kt` - used by ARLayoutPreview + others
- `WebAvanue/.../GlassmorphicComponents.kt` - used by multiple files
- `AvanueUI/src/.../DesignTokens.kt` (com.avanueui) - settings components still reference it
- 18 WebAvanue files still import `com.avanueui.OceanTheme` (component-level, not token-level)

## Key Decisions

1. **Spacing mismatch**: Old `OceanDesignTokens.Spacing.md` = 12dp, new `SpacingTokens.md` = 16dp. Used literal `12.dp` in migrations.
2. **Glass colors as literals**: `ColorTokens.GlassUltraLight` etc. → `Color(0x0DFFFFFF)` literals (not theme-variable).
3. **Component vs Token**: GlassSurface, OceanButton etc. are components (kept), only ColorTokens/ShapeTokens/SizeTokens were migrated.

## Token Import Reference

| Category | Import | Example |
|----------|--------|---------|
| Spacing | `com.augmentalis.avanueui.tokens.SpacingTokens` | `SpacingTokens.md` (16dp) |
| Shapes | `com.augmentalis.avanueui.tokens.ShapeTokens` | `ShapeTokens.lg` (16dp) |
| Sizes | `com.augmentalis.avanueui.tokens.SizeTokens` | `SizeTokens.iconMd` (24dp) |
| Elevation | `com.augmentalis.avanueui.tokens.ElevationTokens` | `ElevationTokens.md` (4dp) |
| Animation | `com.augmentalis.avanueui.tokens.AnimationTokens` | `AnimationTokens.normal` (200) |
| Glass | `com.augmentalis.avanueui.tokens.GlassTokens` | `GlassTokens.mediumOverlay` |
| Responsive | `com.augmentalis.avanueui.tokens.ResponsiveTokens` | `ResponsiveTokens.compactMax` |
| Display | `com.augmentalis.avanueui.tokens.DisplayProfile` | `AvanueTheme.displayProfile` |
| Colors | `com.augmentalis.avanueui.theme.AvanueTheme` | `AvanueTheme.colors.primary` |
| Glass scheme | `com.augmentalis.avanueui.theme.AvanueTheme` | `AvanueTheme.glass.overlayColor` |

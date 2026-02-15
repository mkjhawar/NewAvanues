# VoiceOSCore Fix: Two-Layer Overlay System

**Date**: 2026-02-12
**Branch**: VoiceOSCore-KotlinUpdate
**Status**: Implemented, build verified

## Overview

Implements a two-layer overlay system for accessibility badges:

- **Layer 1 (Text Labels)**: Always-on text labels under icon-only buttons in ANY app. Provides instant visual identification of icon functions (e.g., "Archive", "Delete", "Navigate up") without requiring numbers mode.
- **Layer 2 (Number Badges)**: User-toggled numbered badges (existing behavior). Shows when user says "numbers on" or AUTO mode detects list items in target apps.

## Design

The user approved a minimalist two-layer design via HTML mockups:
- `Demo/overlay-scope-options.html` — Gmail + WhatsApp mockup
- `Demo/overlay-multilang-icons.html` — Multi-language mockup (Maps, Spotify, Photos)

Key principles:
1. Text labels use localized contentDescription from Android accessibility (auto-adapts to device language)
2. Icon-only detection: clickable + no visible text + has metadata + icon-sized bounds
3. When both layers active, Layer 1 labels are suppressed for elements that already have Layer 2 badges (no redundancy)
4. Comma-cleaning for Gmail-style contentDescription strips empty segments and email status prefixes

## Files Modified

| File | Change |
|------|--------|
| `ElementExtractor.kt` | Added `cleanCommaLabel()`, `cleanResourceId()`, `isIconOnlyElement()`. Updated `deriveElementLabels()` to use cleaned labels. |
| `OverlayStateManager.kt` | Added `IconLabelItem` data class, `iconLabelItems` StateFlow, `updateIconLabelItems()`, `clearIconLabelItems()`. Updated `clearOverlayItems()` to clear both layers. |
| `OverlayItemGenerator.kt` | Added `generateIconLabels()` and `deriveIconLabel()` for Layer 1 icon label generation. |
| `DynamicCommandGenerator.kt` | Wired Layer 1 generation after existing Layer 2. Icon labels generated for ALL apps unconditionally. |
| `CommandOverlayService.kt` | Added `IconLabelOverlay` composable. Restructured `NumbersOverlayContent` for two independent layers with dedup. |

## Icon-Only Detection Criteria

An element qualifies as icon-only when ALL of:
- No visible text (`element.text.isBlank()`)
- Clickable or long-clickable
- Not scrollable, not editable
- Has metadata (`contentDescription` or `resourceId`)
- Bounds: width/height > 0 and <= 300px

## Label Priority Chain

1. `contentDescription` (best — localized by OS, e.g., "Archive", "Archivar", "Archiver")
2. `cleanResourceId()` (e.g., "action_archive" → "Archive")
3. Derived label from hierarchy walk (child text/contentDescription)

## Comma Cleaning

Gmail contentDescription format: `"Unread, , , Sender, Subject, Snippet, Date"`
- Split by comma, filter blank segments
- Drop leading status words (Unread, Read, Starred, etc.)
- Take first meaningful segment

## Verification Steps

1. Build: `./gradlew :Modules:VoiceOSCore:compileDebugKotlin :apps:avanues:compileDebugKotlin` — PASSED
2. Deploy to device, enable accessibility service
3. Open any app with icon-only buttons (Gmail, Maps, Spotify, etc.)
4. Verify text labels appear under icons automatically
5. Say "numbers on" — verify number badges appear, icon labels deduplicated
6. Switch apps — verify labels clear and regenerate
7. Gmail inbox — verify comma-cleaned labels (sender name, not "Unread, , , ...")

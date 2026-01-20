# Archived: GlassmorphismUtils Original Files

**Archived:** 2026-01-19
**Reason:** Consolidated to unified `AvaUI/Foundation/GlassmorphismCore.kt`

## What Changed

The duplicate `GlassMorphismConfig`, `DepthLevel`, and `Modifier.glassMorphism()` implementations
were extracted to a single shared location:

**New location:** `Modules/AvaMagic/AvaUI/Foundation/Foundation/src/commonMain/kotlin/com/augmentalis/avamagic/ui/foundation/GlassmorphismCore.kt`

## Original Files (Pre-Consolidation)

These files contained the FULL implementations before refactoring:

1. `CommandManager-GlassmorphismUtils.kt` - Original from CommandManager
2. `VoiceDataManager-GlassmorphismUtils.kt` - Original from VoiceDataManager
3. `LocalizationManager-GlassmorphismUtils.kt` - Original from LocalizationManager
4. `LicenseManager-GlassmorphismUtils.kt` - Original from LicenseManager

## Current State

The manager files now:
1. Import core types from `com.augmentalis.avamagic.ui.foundation`
2. Keep only module-specific colors and configs (which are intentionally different)

## Restore Instructions

If needed, copy the archived files back to:
- `Modules/AvaMagic/managers/{ManagerName}/src/main/java/com/augmentalis/{package}/ui/GlassmorphismUtils.kt`

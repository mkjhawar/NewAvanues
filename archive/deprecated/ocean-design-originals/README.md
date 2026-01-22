# Archived: Ocean Design System Original Files

**Archived:** 2026-01-19
**Reason:** Consolidated to unified `AvaUI/Foundation/` location

## What Changed

The Ocean Design System files were migrated from the legacy WebAvanue location to a shared AvaUI/Foundation location for cross-platform reuse.

**New location:** `Modules/AvaMagic/AvaUI/Foundation/Foundation/src/commonMain/kotlin/com/augmentalis/avamagic/ui/foundation/`

## Files Migrated

| Original File | New Location |
|---------------|--------------|
| `OceanTheme.kt` | `AvaUI/Foundation/.../OceanTheme.kt` |
| `OceanDesignTokens.kt` | `AvaUI/Foundation/.../OceanDesignTokens.kt` |
| `OceanThemeExtensions.kt` | `AvaUI/Foundation/.../OceanThemeExtensions.kt` |
| `GlassmorphicComponents.kt` | `AvaUI/Foundation/.../GlassmorphicComponents.kt` |
| `ComponentProvider.kt` | `AvaUI/Foundation/.../ComponentProvider.kt` |

## Original Locations

Files were originally at:
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/theme/OceanTheme.kt`
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/design/OceanDesignTokens.kt`
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/components/OceanThemeExtensions.kt`
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/components/GlassmorphicComponents.kt`
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/design/ComponentProvider.kt`

## Package Changes

| Original Package | New Package |
|------------------|-------------|
| `com.augmentalis.Avanues.web.universal.presentation.ui.theme` | `com.augmentalis.avamagic.ui.foundation` |
| `com.augmentalis.Avanues.web.universal.presentation.design` | `com.augmentalis.avamagic.ui.foundation` |
| `com.augmentalis.Avanues.web.universal.presentation.ui.components` | `com.augmentalis.avamagic.ui.foundation` |

## Import Updates Required

For existing code in WebAvanue that was using these files, update imports:

```kotlin
// Before:
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme
import com.augmentalis.Avanues.web.universal.presentation.design.OceanDesignTokens
import com.augmentalis.Avanues.web.universal.presentation.ui.components.*

// After:
import com.augmentalis.avamagic.ui.foundation.OceanTheme
import com.augmentalis.avamagic.ui.foundation.OceanDesignTokens
import com.augmentalis.avamagic.ui.foundation.*
```

## Restore Instructions

If needed to restore original files:
1. Copy from this archive back to the original locations
2. Rename package declarations back to original packages
3. Update any imports that depend on the new location

## Notes

- Original files have been preserved in the Avanues directory for now
- They should be updated to import from the new AvaUI/Foundation location
- This consolidation enables cross-platform use (Android, iOS, Desktop, Web)

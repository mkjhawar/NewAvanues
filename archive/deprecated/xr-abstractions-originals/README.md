# Archived: XR Abstractions Original Files

**Archived:** 2026-01-19
**Reason:** Consolidated to unified `AvaUI/XR/` location

## What Changed

The XR abstraction files were migrated from the legacy WebAvanue location to a shared AvaUI/XR location for cross-platform reuse.

**New location:** `Modules/AvaMagic/AvaUI/XR/src/commonMain/kotlin/com/augmentalis/avamagic/xr/`

## Files Migrated

| Original File | New Location |
|---------------|--------------|
| `CommonXRManager.kt` | `AvaUI/XR/.../CommonXRManager.kt` |
| `XRState.kt` | `AvaUI/XR/.../XRState.kt` |
| `CommonCameraManager.kt` | `AvaUI/XR/.../CommonCameraManager.kt` |
| `CommonSessionManager.kt` | `AvaUI/XR/.../CommonSessionManager.kt` |
| `CommonPerformanceMonitor.kt` | `AvaUI/XR/.../CommonPerformanceMonitor.kt` |
| `CommonPermissionManager.kt` | `AvaUI/XR/.../CommonPermissionManager.kt` |

## Original Locations

Files were originally at:
- `Avanues/Web/common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/xr/`

## Package Changes

| Original Package | New Package |
|------------------|-------------|
| `com.augmentalis.Avanues.web.universal.xr` | `com.augmentalis.avamagic.xr` |

## Import Updates Required

For existing code that was using these files, update imports:

```kotlin
// Before:
import com.augmentalis.Avanues.web.universal.xr.CommonXRManager
import com.augmentalis.Avanues.web.universal.xr.XRState
import com.augmentalis.Avanues.web.universal.xr.*

// After:
import com.augmentalis.avamagic.xr.CommonXRManager
import com.augmentalis.avamagic.xr.XRState
import com.augmentalis.avamagic.xr.*
```

## Platform-Specific Files NOT Migrated

The following platform-specific files remain in WebAvanue as they are WebView-specific:

- `XRManager.kt` (Android WebView XR manager)
- `XRCameraManager.kt` (Android camera manager implementation)
- `XRSessionManager.kt` (Android session manager implementation)
- `XRPerformanceMonitor.kt` (Android performance monitor implementation)
- `XRPermissionManager.kt` (Android permission manager implementation)
- `AndroidXRManager.kt` (Android-specific XR manager)
- `CommonXRManager.android.kt` (Android expect/actual implementation)

These files implement the abstract base classes from `AvaUI/XR` and should update their imports accordingly.

## Benefits of Migration

1. **Cross-Platform Reuse**: XR abstractions can now be used by iOS, Desktop, and Web platforms
2. **Single Source of Truth**: No more duplicate abstractions
3. **Cleaner Architecture**: Shared types separate from platform-specific implementations
4. **Easier Testing**: Abstract classes can be tested independently

## Notes

- Original files have been preserved in the Avanues directory for now
- Platform implementations should be updated to import from the new `AvaUI/XR` location
- The `createXRManager` expect function requires platform-specific actual implementations

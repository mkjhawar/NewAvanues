# JITLearning - DEPRECATED

**Status:** DEPRECATED as of 2026-01-06
**Replacement:** `Modules/VoiceOSCoreNG`

## Why Deprecated?

This module has been consolidated into `VoiceOSCoreNG`, a unified KMP (Kotlin Multiplatform) module that:
- Provides cross-platform support (Android, iOS, Desktop, Web)
- Eliminates duplicate class definitions with LearnAppCore
- Uses VUID naming (Voice Unique Identifier) instead of UUID
- Consolidates JIT and exploration functionality into a single module
- Follows IDEACODE v17 folder conventions (common/, functions/, handlers/, features/)

## Migration Map

| Old (JITLearning) | New (VoiceOSCoreNG) |
|-------------------|---------------------|
| `com.augmentalis.jitlearning.JITLearningService` | `com.augmentalis.voiceoscoreng.handlers.ActionCoordinator` |
| `com.augmentalis.jitlearning.JITState` | `com.augmentalis.voiceoscoreng.handlers.ServiceState` |
| `com.augmentalis.jitlearning.ExplorationCommand` | `com.augmentalis.voiceoscoreng.common.CommandGenerator` |
| `com.augmentalis.jitlearning.ExplorationProgress` | `com.augmentalis.voiceoscoreng.common.ExplorationStats` |
| `com.augmentalis.jitlearning.SecurityValidator` | `com.augmentalis.voiceoscoreng.common.VUIDGenerator` (validation) |
| `com.augmentalis.jitlearning.ParcelableNodeInfo` | `com.augmentalis.voiceoscoreng.common.ElementInfo` |
| `com.augmentalis.jitlearning.ScreenChangeEvent` | `com.augmentalis.voiceoscoreng.common.QuantizedScreen` |
| `com.augmentalis.jitlearning.handlers.*` | `com.augmentalis.voiceoscoreng.handlers.*` |

## VoiceOSCoreNG Package Structure (IDEACODE-Compliant)

```
com.augmentalis.voiceoscoreng/
├── common/      # Data models, types, command generation
├── functions/   # Utilities, extraction, fingerprinting, JIT adapters
├── handlers/    # Action handlers, execution, coordination
└── features/    # Speech, overlay, theme, config
```

## AIDL Interfaces

The AIDL interfaces for IPC between VoiceOSCore and LearnApp have been moved:

| Old AIDL | New Location |
|----------|--------------|
| `IAccessibilityEventListener.aidl` | Android-specific platform code in androidMain |
| `IElementCaptureService.aidl` | Android-specific platform code in androidMain |
| `IExplorationProgressListener.aidl` | Android-specific platform code in androidMain |

## How to Migrate

1. Update your `build.gradle.kts`:
```kotlin
// OLD
implementation(project(":Modules:VoiceOS:libraries:JITLearning"))

// NEW
implementation(project(":Modules:VoiceOSCoreNG"))
```

2. Update imports:
```kotlin
// OLD
import com.augmentalis.jitlearning.JITLearningService
import com.augmentalis.jitlearning.JITState

// NEW
import com.augmentalis.voiceoscoreng.handlers.ActionCoordinator
import com.augmentalis.voiceoscoreng.handlers.ServiceState
```

3. For AIDL-based IPC, use the Android-specific implementations in `VoiceOSCoreNG/src/androidMain/`.

## Timeline

- **2026-01-06:** Module marked deprecated
- **Future:** Module will be removed after all consumers migrate

## Questions?

Contact the VoiceOS development team or refer to `Modules/VoiceOSCoreNG/README.md`.

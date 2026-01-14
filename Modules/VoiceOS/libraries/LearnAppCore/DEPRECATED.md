# LearnAppCore - DEPRECATED

**Status:** DEPRECATED as of 2026-01-06
**Replacement:** `Modules/VoiceOSCoreNG`

## Why Deprecated?

This module has been consolidated into `VoiceOSCoreNG`, a unified KMP (Kotlin Multiplatform) module that:
- Provides cross-platform support (Android, iOS, Desktop, Web)
- Eliminates duplicate class definitions
- Uses VUID naming (Voice Unique Identifier) instead of UUID
- Offers Lite/Dev feature gating with future paywall support
- Follows IDEACODE v17 folder conventions (common/, functions/, handlers/, features/)

## Migration Map

| Old (LearnAppCore) | New (VoiceOSCoreNG) |
|-------------------|---------------------|
| `com.augmentalis.learnappcore.models.ElementInfo` | `com.augmentalis.voiceoscoreng.common.ElementInfo` |
| `com.augmentalis.learnappcore.core.ProcessingMode` | `com.augmentalis.voiceoscoreng.common.ProcessingMode` |
| `com.augmentalis.learnappcore.core.ElementProcessingResult` | `com.augmentalis.voiceoscoreng.common.ElementProcessingResult` |
| `com.augmentalis.learnappcore.core.LearnAppCore` | `com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG` |
| `com.augmentalis.learnappcore.export.CommandGenerator` | `com.augmentalis.voiceoscoreng.common.CommandGenerator` |
| `com.augmentalis.learnappcore.safety.SafetyManager` | `com.augmentalis.voiceoscoreng.functions.DangerousElementDetector` |
| `com.augmentalis.learnappcore.detection.CrossPlatformDetector` | `com.augmentalis.voiceoscoreng.handlers.FrameworkHandler` |
| `com.augmentalis.learnappcore.config.LearnAppConfig` | `com.augmentalis.voiceoscoreng.features.LearnAppConfig` |
| `com.augmentalis.learnappcore.exploration.ExplorationState` | `com.augmentalis.voiceoscoreng.common.ExplorationStats` |

## VoiceOSCoreNG Package Structure (IDEACODE-Compliant)

```
com.augmentalis.voiceoscoreng/
├── common/      # Data models, types, command generation
├── functions/   # Utilities, extraction, fingerprinting
├── handlers/    # Action handlers, execution, VoiceOSCoreNG facade
└── features/    # Speech, overlay, theme, config
```

## How to Migrate

1. Update your `build.gradle.kts`:
```kotlin
// OLD
implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))

// NEW
implementation(project(":Modules:VoiceOSCoreNG"))
```

2. Update imports:
```kotlin
// OLD
import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.core.LearnAppCore

// NEW
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG
```

## Timeline

- **2026-01-06:** Module marked deprecated
- **Future:** Module will be removed after all consumers migrate

## Questions?

Contact the VoiceOS development team or refer to `Modules/VoiceOSCoreNG/README.md`.

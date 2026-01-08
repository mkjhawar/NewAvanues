# VoiceOSCore/learnapp - DEPRECATED

**Status:** DEPRECATED as of 2026-01-06
**Replacement:** `Modules/VoiceOSCoreNG`

## Why Deprecated?

The learnapp package within VoiceOSCore has been consolidated into `VoiceOSCoreNG`, a unified KMP (Kotlin Multiplatform) module that:
- Provides cross-platform support (Android, iOS, Desktop, Web)
- Eliminates duplicate code between VoiceOSCore/learnapp, LearnAppCore, and JITLearning
- Uses VUID naming (Voice Unique Identifier) instead of UUID
- Offers Lite/Dev feature gating with runtime switching
- Follows IDEACODE v17 folder conventions (common/, functions/, handlers/, features/)

## Migration Map

| Old (VoiceOSCore/learnapp) | New (VoiceOSCoreNG) |
|---------------------------|---------------------|
| `learnapp.models.LearnedElement` | `com.augmentalis.voiceoscoreng.common.ElementInfo` |
| `learnapp.core.ElementProcessor` | `com.augmentalis.voiceoscoreng.functions.ElementParser` |
| `learnapp.commands.VoiceCommand` | `com.augmentalis.voiceoscoreng.common.QuantizedCommand` |
| `learnapp.generation.CommandGenerator` | `com.augmentalis.voiceoscoreng.common.CommandGenerator` |
| `learnapp.detection.FrameworkDetector` | `com.augmentalis.voiceoscoreng.handlers.FrameworkHandler` |
| `learnapp.fingerprinting.ScreenFingerprinter` | `com.augmentalis.voiceoscoreng.functions.ScreenFingerprinter` |
| `learnapp.integration.LearnAppIntegration` | `com.augmentalis.voiceoscoreng.handlers.VoiceOSCoreNG` |
| `learnapp.ui.overlays.*` | `com.augmentalis.voiceoscoreng.features.*Overlay` |
| `learnapp.database.*` | Use `Modules/VoiceOS/core/database` with VoiceOSCoreNG adapters |

## VoiceOSCoreNG Package Structure (IDEACODE-Compliant)

```
com.augmentalis.voiceoscoreng/
├── common/      # ElementInfo, QuantizedCommand, CommandGenerator, VUIDGenerator
├── functions/   # ElementParser, ScreenFingerprinter, HashUtils, adapters
├── handlers/    # FrameworkHandler, ActionCoordinator, VoiceOSCoreNG facade
└── features/    # Overlays, Speech, Theme, LearnAppConfig
```

## How to Migrate

1. Update dependencies in VoiceOSCore's `build.gradle.kts`:
```kotlin
// ADD
implementation(project(":Modules:VoiceOSCoreNG"))

// After migration complete, REMOVE learnapp package
```

2. Update imports:
```kotlin
// OLD
import com.augmentalis.voiceoscore.learnapp.models.LearnedElement
import com.augmentalis.voiceoscore.learnapp.commands.VoiceCommand

// NEW
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
```

3. Use VoiceOSCoreNG initialization:
```kotlin
// OLD
LearnAppIntegration.initialize(context)

// NEW
VoiceOSCoreNG.initialize(
    tier = LearnAppDevToggle.Tier.LITE,
    isDebug = BuildConfig.DEBUG
)
```

## Timeline

- **2026-01-06:** Package marked deprecated
- **Future:** Package will be removed after VoiceOSCore fully migrates

## Questions?

Contact the VoiceOS development team or refer to `Modules/VoiceOSCoreNG/README.md`.

# VoiceOSCoreNG

Next-generation unified VoiceOS core module - consolidates VoiceOSCore/learnapp, LearnAppCore library, and JITLearning.

## Structure

```
Modules/VoiceOSCoreNG/
├── Common/                    # Shared code (any tech stack)
│   ├── Classes/               # Data classes, models
│   ├── Functions/             # Utility functions
│   ├── Features/              # Feature implementations
│   │   ├── LearnAppLite/      # Basic features
│   │   └── LearnAppDev/       # Advanced features (paywall prep)
│   └── UI/                    # Cross-platform UI (AVAUI)
│       ├── Components/
│       ├── Screens/
│       └── Theme/
├── Android/
│   └── src/
├── iOS/
│   └── src/
├── MacOS/
│   └── src/
├── Windows/
│   └── src/
└── Web/
    └── src/
```

## Features

### LearnAppLite
- Basic element capture
- Command generation
- Framework detection (Flutter, Unity, Unreal, ReactNative, WebView)

### LearnAppDev
- Batch exploration
- Custom command templates
- Analytics export
- Debug overlays
- LearnAppDevToggle for paywall/tier access prep

## Naming Conventions

All identifiers use VUID (Voice Unique Identifier) instead of UUID:
- `vuid` instead of `uuid`
- `generateVUID()` instead of `generateUUID()`
- `VUIDGenerator` instead of `UUIDGenerator`

## Related Documents

- Plan: `Docs/VoiceOS/plans/VoiceOS-Plan-VoiceOSCoreNG-Consolidation-51231-V1.md`
- Architecture: See plan for detailed phase breakdown

## Status

Development (13 phases, 66 tasks)

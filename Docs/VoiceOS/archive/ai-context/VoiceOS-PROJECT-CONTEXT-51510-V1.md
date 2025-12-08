# VOS3 Project Context
**Path:** /Volumes/M Drive/Coding/Warp/vos3-dev/ProjectDocs/AI-Context/PROJECT-CONTEXT.md  
**Created:** 2025-01-18  
**Critical:** This is the PRIMARY context document for AI assistants

## Project Vision

Transform Android devices into voice-first computing platforms by combining:
- **Legacy VoiceOS**: AnySoftKeyboard with voice features (simple, functional)
- **VOS2**: Modern modular architecture (over-engineered but feature-complete)
- **VOS3**: Best of both - monolithic app with compilable submodules

## Core Architecture Decision

**Hybrid Monolithic with Compilable Submodules**
- Main VOS3 app includes ALL modules (single APK, single process)
- Each module can compile independently as AAR library
- Standalone apps can use individual modules
- No code duplication - single source of truth

## Critical Constraints

### Memory Budget (STRICT)
- **Vosk (Free)**: <30MB total memory
- **Vivoka (Premium)**: <60MB total memory
- Current achievement: ~22MB base
- NEVER use Compose for overlay views (4x overhead)

### Android Compatibility
- **Minimum SDK**: 28 (Android 9.0 Pie)
- **Target SDK**: 33 (Android 13 Tiramisu)
- **Compile SDK**: 34 (Android 14)
- Optimize for Android 13 features

### Feature Requirements
- **100% functionality from legacycode** (AnySoftKeyboard base)
- **100% modules from VOS2** (all features)
- **No feature regression** allowed
- Each module independently compilable

## Module Inventory (From VOS2 + Legacy)

### Core Infrastructure (P0)
1. **Core** - Module loading, event bus, shared state
2. **AccessibilityService** - Screen scraping, command execution
3. **SpeechRecognition** - Vosk/Vivoka/Google engines
4. **AudioProcessing** - Recording, VAD, processing

### Primary Features (P1)
5. **Commands** - Voice command processing
6. **Overlay** - Floating UI controls
7. **Keyboard** - Voice keyboard (from legacy AnySoftKeyboard)
8. **Browser** - Voice-controlled browsing
9. **Launcher** - Voice app launcher

### Extended Features (P2)
10. **DataManagement** - Persistence, sync
11. **CommunicationSystems** - WhatsApp, SMS, calls
12. **SmartGlasses** - Vuzix, RealWear, Rokid, Xreal support
13. **FileManager** - Voice file management
14. **UpdateSystem** - OTA updates

### Support Modules (P3)
15. **UIBlocks/UIKit** - Shared UI components
16. **DeviceInfo** - Device detection, DPI
17. **Localization** - 40+ language support
18. **Licensing** - Subscription management

## Business Model

### Free Tier (Vosk)
- 8 languages (EN, ES, FR, DE, RU, ZH, JA, KO)
- Basic commands
- Offline only
- Community support

### Premium Tiers (Vivoka)
- **Trial**: 7 days full access
- **Monthly**: $9.99/month, 3 devices
- **Annual**: $79.99/year, 5 devices  
- **Lifetime**: $299.99, unlimited devices
- 40+ languages, cloud features, priority support

## Technical Stack

### Languages & Frameworks
- Kotlin (primary)
- Java (legacy compatibility)
- Gradle multi-module build
- Android Jetpack (no Compose for overlays)

### Speech Engines
- **Vosk**: Offline, lightweight, 8 languages
- **Vivoka**: Premium, 40+ languages
- **Google**: Cloud fallback
- **Android Native**: System integration

### Key Libraries
- Hilt/Dagger (dependency injection)
- Coroutines (async)
- Flow (reactive)
- Room (database)
- WorkManager (background)

## Development Principles

1. **Memory First**: Every decision optimizes memory
2. **No Duplication**: Single implementation per feature
3. **Module Independence**: Each module self-contained
4. **Offline First**: Core features work without internet
5. **Voice Primary**: All features voice-accessible
6. **Security**: Encrypted storage, obfuscated code
7. **Accessibility**: Full screen reader support

## Success Metrics

### Technical
- Cold start: <2s
- Recognition: <100ms
- Memory: <30MB (Vosk)
- Battery: <2%/hour
- Crash rate: <0.5%

### Business
- Trial conversion: 15-20%
- Monthly churn: <5%
- User rating: 4.2+
- MRR target: $20k by month 6

## Legacy Features to Preserve

From AnySoftKeyboard:
- Full IME implementation
- Gesture typing
- Multi-language keyboards
- Theme system
- Quick text/emoji
- Voice input integration
- Dictionary management
- Auto-correction
- Next word prediction

## VOS2 Modules to Include

All modules from `/Volumes/M Drive/Coding/Warp/vos2/modules/`:
- AccessibilityService (ASM)
- AppShell (navigation)
- AudioProcessing
- CommunicationSystems
- Core (module system)
- DataManagement
- DeviceInfo
- SmartGlasses (5 brands)
- SpeechRecognition (SRM)
- UIBlocks/UIKit
- UpdateSystem

## Critical Decisions Made

1. Android 9 minimum (not 8)
2. Android 13 target (not 14)
3. Monolithic with submodules (not pure modular)
4. Native views for overlay (not Compose)
5. Dual engine support (Vosk + Vivoka)
6. Subscription model (not one-time purchase)

## Session Handoff Notes

When continuing work:
1. Check `/ProjectDocs/CurrentStatus/PROJECT-STATUS-*.md`
2. Review open todos in memory
3. Fix recognition initialization (CRITICAL)
4. Complete Vosk integration
5. Begin module restructuring

## File Organization

```
vos3-dev/
├── app/                    # Main monolithic app
├── modules/                # Compilable submodules
├── apps/                   # Standalone apps
├── ProjectDocs/            # All documentation
│   ├── AI-Context/        # Context for AI
│   ├── Architecture/      # System design
│   ├── Modules/           # Module specs
│   ├── CurrentStatus/     # Progress tracking
│   └── Roadmap/           # Implementation plan
```

## NEVER DO

1. Use Compose for overlay views
2. Create duplicate implementations
3. Exceed memory budgets
4. Remove legacy features
5. Skip VOS2 modules
6. Hardcode strings
7. Store secrets in code
8. Create circular dependencies

---

*This document is the source of truth for VOS3 development. Update when major decisions change.*
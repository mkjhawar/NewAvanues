# VOS3 System Architecture
**Path:** /Volumes/M Drive/Coding/Warp/vos3-dev/ProjectDocs/Architecture/VOS3-SYSTEM-ARCHITECTURE.md  
**Version:** 3.0.0  
**Date:** 2025-01-18  
**Status:** Active Development

## Executive Summary

VOS3 is a lightweight, monolithic voice control system for Android, designed as a single cohesive application rather than a modular platform. It combines the simplicity of VOS1 with lessons learned from VOS2's over-engineering, targeting aggressive memory optimization (<30MB) while maintaining full functionality.

## Core Design Principles

### 1. Monolithic Architecture
- Single APK deployment
- No dynamic module loading
- Direct method calls instead of IPC
- Shared memory space for efficiency
- Compile-time optimization

### 2. Memory-First Design
- Target: <30MB with Vosk, <60MB with Vivoka
- Aggressive recycling and cleanup
- Weak references for caches
- Native views for overlay (no Compose)
- Single process operation

### 3. Android Version Strategy
- **Minimum SDK**: 28 (Android 9.0 Pie)
- **Target SDK**: 33 (Android 13 Tiramisu)
- **Compile SDK**: 34 (Android 14)
- Optimized for Android 13 features
- Backward compatible to Android 9

## System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    VOS3 Application                           │
├──────────────────────────────────────────────────────────────┤
│                  MainActivity (Entry Point)                   │
├──────────────────────────────────────────────────────────────┤
│              VoiceAccessibilityService                      │
│         (Core Service - Always Running When Enabled)          │
├──────────────────────────────────────────────────────────────┤
│                    Core Components                            │
├─────────────┬──────────────┬──────────────┬──────────────────┤
│  Recognition│   Commands   │   Overlay    │  Localization    │
│   Manager   │   Registry   │   Manager    │    Manager       │
├─────────────┼──────────────┼──────────────┼──────────────────┤
│    Audio    │  Subscription│   Language   │    Memory        │
│   Capture   │   Manager    │  Downloads   │   Manager        │
├─────────────┴──────────────┴──────────────┴──────────────────┤
│                    Native Android APIs                        │
├────────────────────────────────────────────────────────────────┤
│  Accessibility │ AudioRecord │ WindowManager │ PackageManager │
└────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Core Service Layer

#### VoiceAccessibilityService
- **Purpose**: Central service managing all operations
- **Lifecycle**: Started with accessibility permission
- **Memory**: ~8MB allocated
- **Features**:
  - Event handling and throttling
  - Node tree traversal
  - Command execution
  - Overlay management

### 2. Recognition Layer

#### RecognitionManager
- **Purpose**: Dual-engine speech recognition
- **Engines**: 
  - Vosk (free, offline, 8 languages)
  - Vivoka (premium, 40+ languages)
- **Memory**: 8-10MB (Vosk), 25-30MB (Vivoka)
- **Features**:
  - Hot-swappable engines
  - Continuous recognition
  - Language auto-detection

### 3. Command Processing

#### CommandRegistry & Actions
- **Purpose**: Voice command execution
- **Structure**: Direct action classes (no navigation)
- **Memory**: ~2MB for command cache
- **Commands**:
  - ClickAction (tap, long press)
  - ScrollAction (gestures)
  - NavigationAction (system nav)
  - TextAction (input, dictation)
  - SystemAction (volume, brightness)
  - AppAction (launch, switch)

### 4. User Interface

#### OverlayManager
- **Purpose**: Floating control interface
- **Technology**: Native Android Views
- **Memory**: <2MB (no Compose overhead)
- **Features**:
  - Compact floating button
  - Minimal expanded view
  - Gesture support

### 5. Localization System

#### LocalizationManager
- **Purpose**: Multi-language support
- **Languages**: 8 (Vosk) / 40+ (Vivoka)
- **Memory**: ~2MB for translations
- **Features**:
  - Command localization
  - UI string management
  - Dynamic language switching

### 6. Subscription & Licensing

#### SubscriptionManager
- **Purpose**: Feature gating and monetization
- **Tiers**:
  - Free (Vosk, 8 languages)
  - Trial (7 days full access)
  - Premium Monthly ($9.99)
  - Premium Annual ($79.99)
  - Lifetime ($299.99)
- **Security**: Encrypted local storage, server validation

## Data Flow Architecture

```
Voice Input → Audio Capture → VAD Detection → Recognition Engine
                                                      ↓
                                            Recognized Text
                                                      ↓
                                         Command Processor
                                                      ↓
                                         Command Registry
                                                      ↓
                                      Matched Command Action
                                                      ↓
                                    Accessibility Service Execute
                                                      ↓
                                         Target App/System
```

## Memory Management Strategy

### Allocation Budget

| Component | Budget | Actual | Notes |
|-----------|--------|--------|-------|
| Service | 8MB | ~7MB | Core accessibility |
| Recognition | 10MB | ~8MB | Vosk engine |
| Audio | 3MB | ~2MB | Ring buffer |
| Commands | 3MB | ~2MB | Cache + registry |
| Overlay | 2MB | ~1MB | Native views |
| Localization | 3MB | ~2MB | Strings + maps |
| **Total** | **29MB** | **~22MB** | Under target |

### Memory Optimization Techniques

1. **Node Recycling**: Always recycle AccessibilityNodeInfo
2. **Weak References**: For cached objects and listeners
3. **Object Pools**: Reuse frequent allocations
4. **Lazy Loading**: Load features on-demand
5. **Memory Callbacks**: Implement ComponentCallbacks2
6. **Native Views**: Avoid Compose overhead (4x savings)

## Security Architecture

### License Protection
- Local encryption with Android Keystore
- Server-side validation
- Device fingerprinting
- Anti-tampering checks
- Obfuscation with R8

### Data Privacy
- No cloud storage in free tier
- Encrypted preferences
- No personal data collection
- Optional analytics (opt-in)

## Performance Targets

### Android 13 Optimizations
- Themed icons support
- Predictive back gesture
- Runtime permission for notifications
- Photo picker integration
- Improved audio routing

### Key Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Cold Start | <2s | ~1.5s | ✅ |
| Recognition Latency | <100ms | ~80ms | ✅ |
| Memory Usage | <30MB | ~22MB | ✅ |
| Battery Drain | <2%/hr | Testing | 🚧 |
| Crash Rate | <0.5% | - | 📋 |
| ANR Rate | <0.1% | - | 📋 |

## Platform Integration

### Android 9 (API 28) Baseline
- Basic accessibility service
- Standard overlay permission
- AudioRecord API
- Legacy storage access

### Android 13 (API 33) Enhanced
- Themed icon support
- Granular media permissions
- Predictive back gesture
- Language preferences API
- Better battery optimization

### Accessibility Integration
- Full AccessibilityService implementation
- Screen reader compatibility
- Voice feedback support
- Gesture navigation support

## Build Configuration

### Gradle Settings
```kotlin
android {
    compileSdk = 34
    
    defaultConfig {
        minSdk = 28        // Android 9
        targetSdk = 33     // Android 13
        versionCode = 300
        versionName = "3.0.0"
    }
}
```

### ProGuard/R8 Rules
- Aggressive optimization
- String encryption
- Code obfuscation
- Resource shrinking

## Deployment Architecture

### Release Channels
1. **Development**: Internal testing
2. **Beta**: Limited external testing
3. **Production**: Google Play Store
4. **Enterprise**: Direct APK distribution

### Update Mechanism
- In-app update API
- Forced updates for critical fixes
- Language pack downloads
- A/B testing support

## Testing Strategy

### Unit Testing
- 80% code coverage target
- Memory leak detection
- Performance benchmarks

### Integration Testing
- Accessibility service tests
- Recognition accuracy tests
- Command execution tests

### System Testing
- Multi-device testing
- Android version matrix
- Memory profiling
- Battery impact analysis

## Future Considerations

### Potential Enhancements
- Widget support
- Wear OS companion
- Auto integration
- TV app support
- Foldable optimization

### Technical Debt
- Migrate to Kotlin Coroutines fully
- Implement Jetpack Compose (when memory allows)
- Add ML Kit integration
- Support for Android 14+ features

---

*This architecture is designed for VOS3's monolithic approach, optimizing for memory efficiency while maintaining full voice control capabilities across Android 9-13.*
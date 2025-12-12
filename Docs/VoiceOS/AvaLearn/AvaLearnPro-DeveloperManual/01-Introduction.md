# Chapter 1: Introduction & Architecture

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch01
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 1.1 System Overview

AvaLearnPro is the developer edition of the VoiceOS app learning system. It provides advanced debugging, logging, and inspection tools for developing and testing the JIT (Just-In-Time) learning pipeline.

### 1.1.1 Architecture Philosophy

The system follows a **JIT-as-Scraper** architecture where:

1. **VoiceOS Core** owns the AccessibilityService and captures raw events
2. **JIT Learning Service** processes events into structured data
3. **AvaLearnPro** consumes data via AIDL and provides developer UI
4. **AVU Export** serializes learned data for VoiceOS command import

### 1.1.2 Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Separation of Concerns** | Clear module boundaries (JIT vs LearnApp vs Core) |
| **IPC Isolation** | AIDL for all cross-process communication |
| **Safety First** | Multiple protection layers (DNC, Login, Loop) |
| **Developer Experience** | Rich debugging tools (Logs, Inspector, Events) |
| **Theme Consistency** | Ocean Blue XR theme variants |

---

## 1.2 Module Architecture

### 1.2.1 Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                      AvaLearnPro (LearnAppDev)                  │
│                          (UI Layer)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ Status Tab  │  │  Logs Tab   │  │    Elements Tab         │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└────────────────────────────┬────────────────────────────────────┘
                             │ Uses
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LearnAppCore                                │
│                    (Business Logic)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Exploration  │  │    Safety    │  │    AVU Export        │  │
│  │  Manager     │  │   Manager    │  │    Pipeline          │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │ AIDL IPC
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      JITLearning Module                          │
│                    (AIDL Interfaces)                             │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ IElementCaptureService│  │ IAccessibilityEventListener     │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ Parcelables          │  │ Event Types                      │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────────┘
                             │ Binds to
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VoiceOSCore                                 │
│                  (Service Provider)                              │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ JITLearningService   │  │ VoiceOSAccessibilityService     │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ ElementCapture       │  │ ScreenAnalyzer                  │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2.2 Module Responsibilities

| Module | Responsibilities |
|--------|------------------|
| **LearnAppDev** | Developer UI, Logs console, Element inspector, Event viewer |
| **LearnAppCore** | Exploration logic, Safety systems, AVU export, State management |
| **JITLearning** | AIDL interfaces, Parcelables, Event types, Data contracts |
| **VoiceOSCore** | AccessibilityService, JIT service, Element capture, Screen analysis |

### 1.2.3 Source Paths

| Module | Path |
|--------|------|
| LearnAppDev | `Modules/VoiceOS/apps/LearnAppDev/` |
| LearnApp (User) | `Modules/VoiceOS/apps/LearnApp/` |
| LearnAppCore | `Modules/VoiceOS/libraries/LearnAppCore/` |
| JITLearning | `Modules/VoiceOS/libraries/JITLearning/` |
| VoiceOSCore | `Modules/VoiceOS/apps/VoiceOSCore/` |

---

## 1.3 Data Flow Architecture

### 1.3.1 Event Capture Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Target    │    │  Android    │    │  VoiceOS    │    │    JIT      │
│    App      │───▶│ Accessibility│───▶│   Core      │───▶│  Service    │
│             │    │   System    │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └──────┬──────┘
                                                                │
                                                                │ AIDL
                                                                ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  VoiceOS    │◀───│   AVU       │◀───│  LearnApp   │◀───│ AvaLearnPro │
│  Commands   │    │   Import    │    │    Core     │    │     UI      │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### 1.3.2 AIDL Communication

```kotlin
// Service binding
val intent = Intent().apply {
    component = ComponentName(
        "com.augmentalis.voiceoscore",
        "com.augmentalis.jitlearning.JITLearningService"
    )
}
bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

// After binding
jitService = IElementCaptureService.Stub.asInterface(binder)
jitService?.registerEventListener(eventListener)
```

---

## 1.4 Build Configuration

### 1.4.1 Build Variants

| Variant | Purpose | Package Suffix |
|---------|---------|----------------|
| debug | Development | .debug |
| release | Distribution | (none) |

### 1.4.2 Build Flags

```kotlin
// LearnAppDev/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "true")
        buildConfigField("Boolean", "ENABLE_NEO4J", "true")
        buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        buildConfigField("Boolean", "ENABLE_ELEMENT_INSPECTOR", "true")
        buildConfigField("Boolean", "PLAIN_TEXT_EXPORT", "true")
    }
}
```

### 1.4.3 Dependencies

```kotlin
dependencies {
    // Core library
    implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))

    // AIDL interfaces
    implementation(project(":Modules:VoiceOS:libraries:JITLearning"))

    // Compose
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

---

## 1.5 Theme Architecture

### 1.5.1 Ocean Blue XR Dark Theme

The developer edition uses Ocean Blue Dark with Cyan accent for visual distinction:

```kotlin
private object OceanDevTheme {
    // Primary (Ocean Blue Dark Mode)
    val Primary = Color(0xFF60A5FA)
    val PrimaryDark = Color(0xFF3B82F6)
    val PrimaryContainer = Color(0xFF1E3A5F)
    val OnPrimaryContainer = Color(0xFFDDEBFF)

    // Developer Accent (Cyan)
    val Accent = Color(0xFF22D3EE)
    val AccentContainer = Color(0xFF164E63)

    // Semantic Colors
    val Success = Color(0xFF34D399)
    val SuccessContainer = Color(0xFF065F46)
    val Error = Color(0xFFF87171)
    val ErrorContainer = Color(0xFF7F1D1D)
    val Warning = Color(0xFFFBBF24)
    val WarningContainer = Color(0xFF78350F)

    // Surface Colors (Dark)
    val Surface = Color(0xFF0F172A)
    val SurfaceVariant = Color(0xFF1E293B)
    val SurfaceDim = Color(0xFF0C1929)
    val Background = Color(0xFF0C1929)

    // Developer Specific
    val ConsoleBackground = Color(0xFF0D0D0D)
    val ConsoleBorder = Color(0xFF374151)
    val LogDebug = Color(0xFF9E9E9E)
    val LogInfo = Color(0xFF60A5FA)
    val LogWarn = Color(0xFFFBBF24)
    val LogError = Color(0xFFF87171)
    val LogEvent = Color(0xFFA78BFA)
}
```

### 1.5.2 Theme Comparison

| Token | User Edition (Light) | Developer Edition (Dark) |
|-------|---------------------|--------------------------|
| Primary | #3B82F6 | #60A5FA |
| Surface | #F0F9FF | #0F172A |
| Background | #F0F9FF | #0C1929 |
| Accent | N/A | #22D3EE (Cyan) |
| Success | #10B981 | #34D399 |
| Error | #EF4444 | #F87171 |

---

## 1.6 Security Model

### 1.6.1 Permission Requirements

| Permission | Purpose | Required |
|------------|---------|----------|
| BIND_ACCESSIBILITY_SERVICE | Connect to VoiceOS | Yes |
| FOREGROUND_SERVICE | Background operation | Yes |
| WRITE_EXTERNAL_STORAGE | Export files | Yes |

### 1.6.2 IPC Security

| Aspect | Implementation |
|--------|----------------|
| Service binding | Explicit component targeting |
| Data validation | Null checks on all AIDL calls |
| Error handling | try-catch around all IPC |
| Timeout | Connection timeout of 30 seconds |

### 1.6.3 Data Protection

| Data Type | Protection |
|-----------|------------|
| Exploration state | In-memory only |
| Export files | Plain text (dev) / Encrypted (user) |
| Logs | In-memory, max 500 entries |
| Element data | Not persisted after session |

---

## 1.7 Performance Considerations

### 1.7.1 Memory Management

| Component | Strategy |
|-----------|----------|
| Log buffer | Circular buffer, max 500 entries |
| Element list | Clear on screen change |
| Event stream | Process and discard |
| UI state | Compose state hoisting |

### 1.7.2 IPC Optimization

| Strategy | Implementation |
|----------|----------------|
| Batch queries | Single call for screen info |
| Event filtering | Process only relevant events |
| Lazy loading | Query elements on demand |
| Connection pooling | Reuse service connection |

---

## 1.8 Next Steps

Continue to [Chapter 2: AIDL Interface Reference](./02-AIDL-Interface.md) for complete IPC documentation.

---

**End of Chapter 1**

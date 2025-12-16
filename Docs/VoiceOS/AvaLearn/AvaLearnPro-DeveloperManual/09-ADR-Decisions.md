# Chapter 9: Architecture Decision Records

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch09
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 9.1 ADR Overview

This chapter documents key architectural decisions made during AvaLearn development.

---

## ADR-001: JIT-as-Scraper Architecture

### Status
Accepted

### Context
We needed to decide where the accessibility event processing should occur:
1. In LearnApp directly
2. In a separate service within VoiceOSCore
3. In a shared library

### Decision
Adopt "JIT-as-Scraper" architecture where VoiceOSCore's AccessibilityService captures and processes all events, exposing them via AIDL to client apps.

### Rationale
- **Single AccessibilityService**: Android allows only one service per app to have accessibility
- **Separation of Concerns**: JIT handles capture, LearnApp handles UI
- **Reusability**: Other apps can consume JIT data without reimplementing
- **Performance**: Service runs in privileged process with better access

### Consequences
- AIDL complexity for IPC
- Service binding lifecycle management
- Potential latency in event delivery

---

## ADR-002: AIDL for Inter-Process Communication

### Status
Accepted

### Context
Communication between LearnApp and JIT service requires IPC. Options:
1. AIDL (Android Interface Definition Language)
2. Messenger
3. ContentProvider
4. Broadcast Intents

### Decision
Use AIDL with custom Parcelables for all IPC.

### Rationale
- **Type Safety**: Strongly typed interfaces
- **Performance**: Direct method calls, not serialization overhead
- **Bi-directional**: Supports callbacks (event streaming)
- **Complex Data**: Parcelables handle nested structures

### Consequences
- Learning curve for AIDL
- Parcelable boilerplate
- Need to handle RemoteException

### Implementation
```kotlin
interface IElementCaptureService {
    JITState queryState();
    void registerEventListener(IAccessibilityEventListener listener);
    // ...
}
```

---

## ADR-003: Dual Edition Architecture

### Status
Accepted

### Context
We need to serve two audiences:
1. End users who want simple exploration
2. Developers who need debugging tools

### Decision
Create two separate apps sharing a common core:
- **AvaLearnLite** (User Edition): Simplified UI, encrypted export
- **AvaLearnPro** (Developer Edition): Full debugging, plain text export

### Rationale
- **UX Simplicity**: Users don't need developer tools
- **Security**: Users get encrypted exports by default
- **Flexibility**: Developers get raw data access
- **Maintenance**: Shared LearnAppCore library

### Consequences
- Two build targets to maintain
- Theme variations
- Feature flags for edition-specific behavior

### Implementation
```kotlin
// LearnAppDev build.gradle.kts
buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "true")

// LearnApp build.gradle.kts
buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "false")
```

---

## ADR-004: Ocean Blue XR Theme System

### Status
Accepted

### Context
Both editions need consistent branding with visual distinction.

### Decision
Use Ocean Blue XR theme with:
- **Light variant** for User Edition
- **Dark variant with Cyan accent** for Developer Edition

### Rationale
- **Brand Consistency**: Same color family
- **Instant Recognition**: Developers know which edition
- **Accessibility**: Both variants meet WCAG standards
- **Modern Design**: Material3 guidelines

### Consequences
- Theme duplication (can be extracted to shared module)
- Color management in Compose

### Implementation
```kotlin
// User Edition
object OceanTheme {
    val Primary = Color(0xFF3B82F6)  // Light blue
}

// Developer Edition
object OceanDevTheme {
    val Primary = Color(0xFF60A5FA)  // Lighter for dark theme
    val Accent = Color(0xFF22D3EE)   // Cyan distinction
}
```

---

## ADR-005: Safety-First Element Processing

### Status
Accepted

### Context
Exploration must not cause harm to user data or accounts.

### Decision
Implement multi-layer safety system:
1. **DoNotClick**: Block dangerous elements
2. **LoginDetector**: Pause at auth screens
3. **LoopPrevention**: Stop infinite loops
4. **DynamicRegionDetector**: Skip changing content

### Rationale
- **User Trust**: Critical for accessibility tool
- **Data Protection**: Prevent accidental deletion
- **Account Safety**: Don't logout users
- **Stability**: Don't get stuck in loops

### Consequences
- May skip some legitimate elements
- False positives possible
- Performance overhead for checks

### Implementation
```kotlin
class SafetyManager(callback: SafetyCallback) {
    fun processElement(element: ElementInfo): SafetyResult {
        // Layer 1: DNC
        if (doNotClickManager.isDangerous(element)) return Blocked
        // Layer 2: Dynamic
        if (dynamicRegionDetector.isInDynamic(element)) return SkipDynamic
        // All clear
        return Safe
    }
}
```

---

## ADR-006: AVU Format for Data Exchange

### Status
Accepted

### Context
Need format for storing/exchanging learned app data.

### Decision
Create AVU (Avanues Universal) format:
- Line-based with record prefixes
- Human-readable (developer mode)
- Compact for storage

### Rationale
- **Simplicity**: Easy to parse and debug
- **Extensibility**: New record types easy to add
- **IPC-Style**: Familiar to developers
- **Portability**: Plain text works everywhere

### Alternatives Considered
- JSON: More verbose, harder to stream
- Protocol Buffers: Requires schema compilation
- SQLite: Overkill for single-file export

### Implementation
```
APP:com.example.app:Example App:1702300800000
SCR:abc123:MainActivity:1702300801000:15
ELM:uuid1:Login:Button:C:0,540,1080,640:ACT
CMD:cmd1:click login:click:uuid1:0.95
```

---

## ADR-007: Event Streaming via Callbacks

### Status
Accepted

### Context
LearnApp needs real-time events from JIT service.

### Decision
Use AIDL callback interface (IAccessibilityEventListener) for push-based event delivery.

### Rationale
- **Real-time**: Events delivered immediately
- **Efficiency**: No polling required
- **Selective**: Can filter at source
- **Bi-directional**: Supports multiple listeners

### Consequences
- Callback lifecycle management
- Thread safety considerations
- Memory leaks if not unregistered

### Implementation
```kotlin
private val eventListener = object : IAccessibilityEventListener.Stub() {
    override fun onScreenChanged(event: ScreenChangeEvent) {
        runOnUiThread { /* update UI */ }
    }
}

// Register
jitService?.registerEventListener(eventListener)

// Unregister on destroy
jitService?.unregisterEventListener(eventListener)
```

---

## ADR-008: Compose-First UI Architecture

### Status
Accepted

### Context
Choosing UI framework for LearnApp.

### Decision
Use Jetpack Compose with Material3 exclusively.

### Rationale
- **Modern**: Google's recommended approach
- **Declarative**: Easier state management
- **Material3**: Latest design system
- **Performance**: Efficient recomposition

### Consequences
- No XML layouts
- Learning curve for team
- Compose-specific patterns needed

---

## ADR-009: Module Separation Strategy

### Status
Accepted

### Context
Organizing code across multiple components.

### Decision
Four-module structure:
```
LearnAppDev    → UI (Developer)
LearnApp       → UI (User)
LearnAppCore   → Business Logic
JITLearning    → AIDL Interfaces
```

### Rationale
- **Reusability**: Core shared between editions
- **Separation**: UI isolated from logic
- **AIDL Isolation**: Interfaces in dedicated module
- **Build Speed**: Parallel compilation

### Consequences
- Inter-module dependencies
- Version alignment needed

---

## ADR-010: Plain Text Developer Export

### Status
Accepted

### Context
Export format for developer edition.

### Decision
Developer edition exports plain text AVU; User edition exports encrypted.

### Rationale
- **Debugging**: Developers need readable output
- **Privacy**: Users protected by default
- **Flexibility**: Easy to inspect/modify

### Implementation
```kotlin
enum class ExportMode {
    USER,       // Encrypted
    DEVELOPER   // Plain text
}

val mode = if (BuildConfig.IS_DEVELOPER_EDITION) {
    ExportMode.DEVELOPER
} else {
    ExportMode.USER
}
```

---

## 9.2 Future Considerations

### Pending Decisions

| Topic | Status | Notes |
|-------|--------|-------|
| Neo4j Integration | Planned | Graph visualization |
| ViewModel Architecture | Consider | Currently Activity-based |
| Hilt DI | Consider | Manual injection now |
| Theme Extraction | Consider | Shared theme module |
| Interface Segregation | Consider | Split fat AIDL interface |

---

## 9.3 Next Steps

Continue to [Chapter 10: Complete API Reference](./10-API-Reference.md).

---

**End of Chapter 9**

# VOS3 Architecture Decisions Document
**Created**: 2025-01-14  
**Purpose**: Hybrid implementation combining Legacy simplicity with VOS2 modern patterns  
**Target**: Lightweight, always-on accessibility overlay service

---

## 🧠 Deep Think: Architecture Philosophy

### Chain of Thought:
```
THINK: What made Legacy work well?
- Simplicity: Direct service architecture
- Efficiency: Minimal memory footprint (~30MB)
- Stability: No complex module loading
- Focus: Core functionality only

THINK: What's good about VOS2?
- Clean architecture patterns
- Modern Kotlin coroutines
- Comprehensive localization
- Well-structured commands

THINK: What killed VOS2?
- Over-engineering for wrong use case
- Compose UI for overlay (4x memory)
- Dynamic module loading (unnecessary)
- Multiple engines loaded simultaneously

CONCLUSION: Take legacy's simplicity + VOS2's clean code
```

### Tree of Thought - Architecture Options:
```
Option A: Monolithic Service
├── Pros: Simplest, fastest, smallest
├── Cons: Less modular
├── Memory: ~25MB
└── Verdict: BEST for overlay

Option B: Service + Minimal Modules
├── Pros: Some modularity
├── Cons: IPC overhead
├── Memory: ~35MB
└── Verdict: Acceptable

Option C: Microservices
├── Pros: Maximum modularity
├── Cons: Complex, memory overhead
├── Memory: ~50MB+
└── Verdict: Over-engineered
```

**Decision: Option A with logical separation (packages, not modules)**

---

## 📐 Core Architecture Principles

### 1. Memory First
- Target: <30MB total footprint
- No Compose for overlay (Native Views only)
- Single instance patterns
- Aggressive resource cleanup

### 2. Service-Centric
- AccessibilityService is the core
- No activities except settings
- Direct command execution
- No navigation framework

### 3. Offline First
- Works without internet
- Local command processing
- Optional cloud features
- Minimal network usage

### 4. Battery Conscious
- No continuous processing
- Event-driven architecture
- Efficient audio processing
- Smart wake detection

---

## 🏗️ Project Structure

```
vos3/
├── app/                          # Main application
│   ├── src/main/
│   │   ├── java/com/augmentalis/vos3/
│   │   │   ├── core/            # Core service
│   │   │   │   ├── VOS3AccessibilityService.kt
│   │   │   │   ├── CommandProcessor.kt
│   │   │   │   └── MemoryManager.kt
│   │   │   ├── overlay/         # Minimal overlay UI
│   │   │   │   ├── CompactOverlay.kt
│   │   │   │   └── OverlayManager.kt
│   │   │   ├── audio/           # Efficient audio
│   │   │   │   ├── SimpleVAD.kt
│   │   │   │   └── AudioCapture.kt
│   │   │   ├── recognition/     # Single SRM engine
│   │   │   │   ├── SRMEngine.kt
│   │   │   │   └── VoskEngine.kt
│   │   │   ├── commands/        # Command system
│   │   │   │   ├── CommandRegistry.kt
│   │   │   │   └── actions/
│   │   │   ├── data/           # Lightweight data
│   │   │   │   ├── PreferencesManager.kt
│   │   │   │   └── CommandCache.kt
│   │   │   └── settings/       # Settings UI (Compose OK here)
│   │   │       └── SettingsActivity.kt
│   │   └── res/
│   └── build.gradle.kts
├── ProjectDocs/
└── README.md
```

---

## 🔧 Technical Decisions

### Language & Framework
- **Kotlin** (modern, concise)
- **Coroutines** (efficient async)
- **Native Views** for overlay
- **Compose** for settings only

### Dependencies (Minimal)
```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Accessibility
    implementation("androidx.accessibility:accessibility:1.0.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Vosk for SRM (single engine)
    implementation("com.alphacephei:vosk-android:0.3.47")
    
    // Settings UI only
    implementation("androidx.compose.ui:ui:1.5.4")
    
    // NO Hilt, NO Navigation, NO Modules
}
```

### Memory Management Strategy
1. **Single Process** (no IPC overhead)
2. **Lazy Initialization** (load on demand)
3. **Weak References** (prevent leaks)
4. **Memory Callbacks** (respond to pressure)
5. **Resource Pooling** (reuse objects)

### Audio Processing
- Simple energy-based VAD (no FFT)
- Hardware audio effects (built-in)
- Circular buffer (continuous)
- Wake word detection (lightweight)

---

## 🎯 Feature Priorities

### Core Features (Week 1)
1. ✅ Accessibility service
2. ✅ Voice commands
3. ✅ Minimal overlay
4. ✅ Basic SRM (Vosk only)

### Enhanced Features (Week 2)
1. ⏸️ Command customization
2. ⏸️ Multi-language support
3. ⏸️ Settings UI
4. ⏸️ Battery optimization

### Optional Features (Future)
1. ❌ Smart glasses (plugin)
2. ❌ Multiple SRM engines
3. ❌ Cloud sync
4. ❌ Advanced DSP

---

## 📊 Memory Budget

```
Component               Target    Max
AccessibilityService    8MB      10MB
SRM Engine (Vosk)       8MB      10MB
Audio Processing        2MB      3MB
Overlay UI             1MB      2MB
Command Cache          2MB      3MB
Misc/Runtime           4MB      7MB
-----------------------------------
TOTAL                  25MB     35MB
```

---

## 🚫 What We're NOT Doing

1. **NO AppShell** - Direct service
2. **NO Module Loading** - Static compilation
3. **NO Compose for Overlay** - Native only
4. **NO Multiple Engines** - One at a time
5. **NO Complex Navigation** - Direct execution
6. **NO FFT Processing** - Simple VAD
7. **NO Dynamic Features** - All in APK
8. **NO Event Bus** - Direct calls

---

## ✅ What We ARE Doing

1. **Recycling AccessibilityNodeInfo** - Prevent leaks
2. **Single SRM Engine** - Low memory
3. **Native Overlay View** - Minimal footprint
4. **Direct Command Execution** - Fast response
5. **Memory Pressure Handling** - Graceful degradation
6. **Process Death Recovery** - State persistence
7. **Wake Word Detection** - Battery saving
8. **Efficient Audio Pipeline** - Low CPU

---

## 🔄 Migration from Legacy

### Preserve from Legacy:
- Service architecture
- Direct command execution
- Simple audio processing
- Floating widget concept
- Offline-first approach

### Improve from Legacy:
- Kotlin instead of Java
- Coroutines instead of threads
- Better command structure
- Modern UI for settings
- Proper localization

### Add New:
- Memory management
- State persistence
- Command customization
- Multi-language support
- Accessibility best practices

---

## 📈 Success Metrics

### Performance Targets:
- Cold start: <2 seconds
- Command response: <100ms
- Memory usage: <30MB
- Battery drain: <2%/hour
- Crash rate: <0.1%

### User Experience:
- Always responsive
- Never killed by system
- Instant voice recognition
- Clear visual feedback
- Simple configuration

---

## 🛠️ Implementation Plan

### Phase 1: Core (Day 1-2)
1. Create AccessibilityService
2. Implement command processor
3. Add memory management
4. Create minimal overlay

### Phase 2: Audio (Day 3-4)
1. Implement VAD
2. Integrate Vosk
3. Add wake word
4. Create audio pipeline

### Phase 3: Commands (Day 5-6)
1. Port legacy commands
2. Add command registry
3. Implement actions
4. Add customization

### Phase 4: Polish (Day 7)
1. Settings UI
2. State persistence
3. Testing
4. Optimization

---

## 🔍 Reflection

This architecture represents the "Middle Way" - avoiding both the over-simplification of legacy and the over-engineering of VOS2. By focusing on the core use case (accessibility overlay) and building specifically for that, we can achieve better performance with less code.

The key insight is that an overlay service is fundamentally different from a traditional app. It needs to be invisible, always-ready, and resource-efficient. Every architectural decision flows from these requirements.

**Estimated Code Reduction**: 70% less than VOS2  
**Estimated Memory Saving**: 80% less than VOS2  
**Estimated Complexity**: 50% of VOS2  

---

*This document will be updated as implementation progresses*
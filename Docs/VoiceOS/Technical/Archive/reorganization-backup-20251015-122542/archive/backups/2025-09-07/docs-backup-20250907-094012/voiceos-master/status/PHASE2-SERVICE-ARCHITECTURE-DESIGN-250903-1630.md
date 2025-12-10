# VOS4 Phase 2: Service Architecture Design

**File:** PHASE2-SERVICE-ARCHITECTURE-DESIGN-250903-1630.md
**Created:** 2025-09-03 16:30
**Phase:** 2 - Service Architecture
**Purpose:** Design document for VOS4 service architecture

---

## 🎯 Executive Summary

Phase 2 implements the Android service architecture for VOS4, focusing on AccessibilityService and ForegroundService with zero-overhead principles.

---

## 📐 Architecture Design

### 1. VoiceOSAccessibilityService

**Purpose:** Core accessibility service for UI interaction and voice command processing

**Location:** `/libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/services/VoiceOSAccessibilityService.kt`

**Key Features:**
- Direct AccessibilityService extension (no interface)
- Coroutine-based lifecycle management
- Voice command processing pipeline
- UI element interaction and scraping
- State management with StateFlow

**Core Methods:**
```kotlin
class VoiceOSAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var speechEngine: Any? = null // Direct engine reference
    
    override fun onServiceConnected()
    override fun onAccessibilityEvent(event: AccessibilityEvent)
    override fun onInterrupt()
    override fun onDestroy()
    
    private suspend fun initializeSpeechEngine(type: EngineType)
    private suspend fun processVoiceCommand(command: String)
    private fun performAction(nodeInfo: AccessibilityNodeInfo, action: Int)
}
```

### 2. VoiceOSForegroundService

**Purpose:** Persistent foreground service for background voice recognition

**Location:** `/libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/services/VoiceOSForegroundService.kt`

**Key Features:**
- LifecycleService extension for lifecycle awareness
- Foreground notification management
- Microphone permission handling
- State-based notification updates
- Battery optimization

**Core Structure:**
```kotlin
class VoiceOSForegroundService : LifecycleService() {
    private lateinit var notificationManager: NotificationManager
    private val recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    
    override fun onCreate()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    override fun onDestroy()
    
    private fun startForegroundWithNotification()
    private fun updateNotificationState(state: RecognitionState)
}
```

### 3. Service Communication

**ServiceCoordinator (No Manager Interface):**
```kotlin
object ServiceCoordinator {
    fun startAccessibilityService(context: Context)
    fun startForegroundService(context: Context)
    fun stopAllServices(context: Context)
    
    // Direct communication via SharedFlow
    val commandFlow = MutableSharedFlow<VoiceCommand>()
    val stateFlow = MutableStateFlow<ServiceState>(ServiceState.Idle)
}
```

### 4. Android Manifest Configuration

**Required Declarations:**
```xml
<!-- Accessibility Service -->
<service
    android:name="com.augmentalis.speechrecognition.services.VoiceOSAccessibilityService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/voiceos_accessibility_config" />
</service>

<!-- Foreground Service -->
<service
    android:name="com.augmentalis.speechrecognition.services.VoiceOSForegroundService"
    android:foregroundServiceType="microphone"
    android:enabled="true"
    android:exported="false" />
```

### 5. Accessibility Service Configuration

**File:** `/libraries/SpeechRecognition/src/main/res/xml/voiceos_accessibility_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagRequestAccessibilityButton"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:settingsActivity="com.augmentalis.speechrecognition.SettingsActivity" />
```

---

## 🔄 Service Lifecycle Flow

```
App Start
    ↓
Check Permissions
    ↓
Start AccessibilityService (if enabled)
    ↓
AccessibilityService starts ForegroundService
    ↓
Initialize Speech Engine (based on config)
    ↓
Begin Voice Recognition Loop
    ↓
Process Commands via AccessibilityService
    ↓
Update UI/Notification State
```

---

## 📋 Implementation Phases

### Phase 2.1: Core Service Structure
- [ ] Create VoiceOSAccessibilityService skeleton
- [ ] Create VoiceOSForegroundService skeleton
- [ ] Implement ServiceCoordinator
- [ ] Add manifest declarations

### Phase 2.2: Service Communication
- [ ] Implement command flow between services
- [ ] Add state synchronization
- [ ] Create notification system
- [ ] Add broadcast receivers

### Phase 2.3: Speech Engine Integration
- [ ] Connect speech engines to services
- [ ] Implement engine switching
- [ ] Add error recovery
- [ ] Performance monitoring

### Phase 2.4: Permission & Lifecycle
- [ ] Permission request flow
- [ ] Service auto-start
- [ ] Crash recovery
- [ ] Battery optimization

---

## 🎯 Design Decisions

### 1. Zero-Overhead Approach
- **Decision:** No service interfaces or abstractions
- **Rationale:** Direct implementation for maximum performance
- **Impact:** Faster service communication, lower memory usage

### 2. LifecycleService vs Service
- **Decision:** Use LifecycleService for ForegroundService
- **Rationale:** Better lifecycle management with coroutines
- **Impact:** Cleaner code, automatic cleanup

### 3. Direct Engine Access
- **Decision:** Services directly instantiate speech engines
- **Rationale:** No abstraction overhead
- **Impact:** Direct method calls, no interface dispatch

### 4. Coroutine-First Design
- **Decision:** All async operations use coroutines
- **Rationale:** Modern, efficient async handling
- **Impact:** Better performance, cleaner error handling

---

## 🚀 Performance Targets

| Metric | Target | Priority |
|--------|--------|----------|
| Service Startup | <500ms | High |
| Command Processing | <100ms | Critical |
| Memory Usage | <30MB | High |
| Battery Impact | <1% per hour | High |
| Crash Rate | <0.01% | Critical |

---

## 🔧 Technical Requirements

### Dependencies
- AndroidX Core: 1.15.0
- AndroidX Lifecycle: 2.8.7
- Kotlin Coroutines: 1.7.3
- No additional dependencies (zero-overhead principle)

### API Compatibility
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)
- Compile SDK: 35

### Permissions Required
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## 📝 Next Steps

1. Create service skeleton files
2. Implement basic lifecycle methods
3. Add manifest configurations
4. Test service startup
5. Integrate with speech engines

---

**Status:** Design Complete - Ready for Implementation
**Next Phase:** 2.1 - Core Service Structure

---
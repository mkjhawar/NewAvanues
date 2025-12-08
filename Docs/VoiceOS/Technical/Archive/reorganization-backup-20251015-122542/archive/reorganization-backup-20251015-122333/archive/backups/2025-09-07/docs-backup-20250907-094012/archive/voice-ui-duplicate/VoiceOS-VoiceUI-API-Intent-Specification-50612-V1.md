# VoiceUI API & Intent Specification

## 🎯 Purpose
Define comprehensive APIs and Intents for VoiceUI to enable easy integration by internal teams and third-party developers.

## 📱 Intent System Design

### Core Intents Structure
```
com.augmentalis.voiceui.action.[COMPONENT]_[ACTION]
com.augmentalis.voiceui.broadcast.[EVENT]
com.augmentalis.voiceui.service.[SERVICE]
```

## 🔧 Component APIs & Intents

### 1. Theme Engine

#### Intents
```kotlin
// Change theme
Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
    putExtra("theme_name", "arvision") // arvision, material, visionos
    putExtra("animate", true)
}

// Get current theme
Intent("com.augmentalis.voiceui.action.THEME_GET")

// Register custom theme
Intent("com.augmentalis.voiceui.action.THEME_REGISTER").apply {
    putExtra("theme_name", "custom_theme")
    putExtra("theme_config", themeJson)
}
```

#### Broadcast Events
```kotlin
// Theme changed
"com.augmentalis.voiceui.broadcast.THEME_CHANGED"
extras: theme_name, previous_theme

// Theme error
"com.augmentalis.voiceui.broadcast.THEME_ERROR"
extras: error_message, theme_name
```

#### Content Provider API
```kotlin
// URI: content://com.augmentalis.voiceui.provider/themes
// Operations: query (list themes), insert (add custom), update, delete
```

### 2. Gesture Manager

#### Intents
```kotlin
// Enable/disable gestures
Intent("com.augmentalis.voiceui.action.GESTURE_ENABLE").apply {
    putExtra("gesture_type", "swipe") // tap, swipe, pinch, rotate
    putExtra("enabled", true)
}

// Trigger gesture programmatically
Intent("com.augmentalis.voiceui.action.GESTURE_TRIGGER").apply {
    putExtra("gesture_type", "tap")
    putExtra("x", 500f)
    putExtra("y", 300f)
}

// Configure gesture sensitivity
Intent("com.augmentalis.voiceui.action.GESTURE_CONFIG").apply {
    putExtra("sensitivity", 0.8f) // 0.0 to 1.0
    putExtra("multi_touch", true)
}
```

#### Broadcast Events
```kotlin
// Gesture detected
"com.augmentalis.voiceui.broadcast.GESTURE_DETECTED"
extras: gesture_type, x, y, velocity, direction

// Gesture recognized
"com.augmentalis.voiceui.broadcast.GESTURE_RECOGNIZED"
extras: gesture_id, confidence, timestamp
```

### 3. Window Manager

#### Intents
```kotlin
// Create window
Intent("com.augmentalis.voiceui.action.WINDOW_CREATE").apply {
    putExtra("window_id", UUID.randomUUID().toString())
    putExtra("title", "My Window")
    putExtra("width", 800)
    putExtra("height", 600)
    putExtra("x", 0f)
    putExtra("y", 0f)
    putExtra("z", -2f) // Spatial depth
}

// Show/hide window
Intent("com.augmentalis.voiceui.action.WINDOW_SHOW").apply {
    putExtra("window_id", windowId)
    putExtra("animated", true)
}

// Move window
Intent("com.augmentalis.voiceui.action.WINDOW_MOVE").apply {
    putExtra("window_id", windowId)
    putExtra("x", 100f)
    putExtra("y", 200f)
    putExtra("z", -1f)
}

// Resize window
Intent("com.augmentalis.voiceui.action.WINDOW_RESIZE").apply {
    putExtra("window_id", windowId)
    putExtra("width", 1024)
    putExtra("height", 768)
}
```

#### Window Service
```kotlin
// Bind to window service for complex operations
Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE")
```

### 4. HUD System

#### Intents
```kotlin
// Show HUD notification
Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
    putExtra("message", "Voice command recognized")
    putExtra("duration", 2000)
    putExtra("position", "TOP_CENTER") // TOP_LEFT, TOP_CENTER, etc.
    putExtra("priority", "HIGH") // LOW, NORMAL, HIGH, URGENT
}

// Update HUD overlay
Intent("com.augmentalis.voiceui.action.HUD_UPDATE").apply {
    putExtra("overlay_id", "status_overlay")
    putExtra("content", hudContentJson)
}

// Toggle HUD visibility
Intent("com.augmentalis.voiceui.action.HUD_TOGGLE").apply {
    putExtra("visible", true)
    putExtra("fade_duration", 300)
}
```

### 5. Notification System

#### Intents
```kotlin
// Show custom notification
Intent("com.augmentalis.voiceui.action.NOTIFY_CUSTOM").apply {
    putExtra("notification_id", notifId)
    putExtra("title", "VoiceUI")
    putExtra("message", "Command executed")
    putExtra("style", "SPATIAL") // STANDARD, SPATIAL, MINIMAL
    putExtra("actions", arrayOf("DISMISS", "RETRY"))
}

// Clear notifications
Intent("com.augmentalis.voiceui.action.NOTIFY_CLEAR").apply {
    putExtra("notification_id", notifId) // or "ALL"
}
```

### 6. Voice Command System

#### Intents
```kotlin
// Register voice command
Intent("com.augmentalis.voiceui.action.VOICE_REGISTER").apply {
    putExtra("command", "open settings")
    putExtra("action", "com.myapp.OPEN_SETTINGS")
    putExtra("language", "en-US")
}

// Process voice input
Intent("com.augmentalis.voiceui.action.VOICE_PROCESS").apply {
    putExtra("audio_data", audioBytes)
    putExtra("language", "en-US")
}

// Enable voice control
Intent("com.augmentalis.voiceui.action.VOICE_ENABLE").apply {
    putExtra("enabled", true)
    putExtra("wake_word", "hey voice")
}
```

### 7. Data Visualization

#### Intents
```kotlin
// Create chart
Intent("com.augmentalis.voiceui.action.CHART_CREATE").apply {
    putExtra("chart_type", "LINE") // LINE, BAR, PIE, SCATTER
    putExtra("data", chartDataJson)
    putExtra("title", "Performance Metrics")
}

// Update chart data
Intent("com.augmentalis.voiceui.action.CHART_UPDATE").apply {
    putExtra("chart_id", chartId)
    putExtra("data", newDataJson)
    putExtra("animate", true)
}
```

## 🔌 Content Provider APIs

### VoiceUI Provider
```kotlin
// Base URI
content://com.augmentalis.voiceui.provider/

// Endpoints
/themes          - Theme configurations
/gestures        - Gesture patterns
/windows         - Active windows
/notifications   - Notification queue
/commands        - Voice commands
/settings        - Module settings
```

### Query Examples
```kotlin
// Get all themes
contentResolver.query(
    Uri.parse("content://com.augmentalis.voiceui.provider/themes"),
    null, null, null, null
)

// Get active windows
contentResolver.query(
    Uri.parse("content://com.augmentalis.voiceui.provider/windows"),
    arrayOf("window_id", "title", "x", "y", "z"),
    "visible = ?",
    arrayOf("1"),
    "z_order ASC"
)
```

## 🎮 Service APIs

### VoiceUIService
```kotlin
class VoiceUIService : Service() {
    
    // Bind to service
    override fun onBind(intent: Intent): IBinder {
        return VoiceUIBinder()
    }
    
    inner class VoiceUIBinder : Binder() {
        fun getThemeEngine() = voiceUI.themeEngine
        fun getGestureManager() = voiceUI.gestureManager
        fun getWindowManager() = voiceUI.windowManager
        fun getHUDSystem() = voiceUI.hudSystem
    }
}
```

### AIDL Interface (for cross-process)
```aidl
// IVoiceUIService.aidl
interface IVoiceUIService {
    void setTheme(String themeName);
    String getCurrentTheme();
    void showNotification(String message, int duration);
    void createWindow(in Bundle windowConfig);
    void processGesture(in Bundle gestureData);
    void registerVoiceCommand(String command, String action);
}
```

## 📊 REST API (for remote control)

### Base URL
```
http://localhost:8080/api/voiceui/v1/
```

### Endpoints
```yaml
GET    /theme              - Get current theme
PUT    /theme              - Change theme
GET    /themes             - List available themes
POST   /themes             - Register custom theme

GET    /windows            - List active windows
POST   /windows            - Create window
PUT    /windows/{id}       - Update window
DELETE /windows/{id}       - Close window

POST   /gestures           - Trigger gesture
GET    /gestures/config    - Get gesture config
PUT    /gestures/config    - Update gesture config

POST   /notifications      - Show notification
DELETE /notifications/{id} - Clear notification

POST   /voice/commands     - Register command
POST   /voice/process      - Process voice input
```

## 🔒 Permissions

### Required Permissions
```xml
<!-- For third-party apps -->
<uses-permission android:name="com.augmentalis.voiceui.permission.CHANGE_THEME" />
<uses-permission android:name="com.augmentalis.voiceui.permission.CONTROL_WINDOWS" />
<uses-permission android:name="com.augmentalis.voiceui.permission.TRIGGER_GESTURES" />
<uses-permission android:name="com.augmentalis.voiceui.permission.SHOW_HUD" />
<uses-permission android:name="com.augmentalis.voiceui.permission.REGISTER_COMMANDS" />
```

### Permission Groups
```xml
<!-- Basic access -->
<permission-group android:name="com.augmentalis.voiceui.permission-group.BASIC" />

<!-- Advanced control -->
<permission-group android:name="com.augmentalis.voiceui.permission-group.ADVANCED" />

<!-- System level -->
<permission-group android:name="com.augmentalis.voiceui.permission-group.SYSTEM" />
```

## 🚀 Quick Start Examples

### Example 1: Change Theme from Any App
```kotlin
// Simple intent
context.startService(
    Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
        setPackage("com.augmentalis.voiceos")
        putExtra("theme_name", "arvision")
    }
)
```

### Example 2: Show HUD Notification
```kotlin
// From any app with permission
context.sendBroadcast(
    Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
        putExtra("message", "Task completed")
        putExtra("duration", 3000)
    }
)
```

### Example 3: Register Voice Command
```kotlin
// Register custom command
val intent = Intent("com.augmentalis.voiceui.action.VOICE_REGISTER")
intent.putExtra("command", "take screenshot")
intent.putExtra("action", "com.myapp.SCREENSHOT")
context.startService(intent)
```

### Example 4: Window Control via Service
```kotlin
// Bind to service
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val voiceUIService = (service as VoiceUIService.VoiceUIBinder)
        voiceUIService.getWindowManager().createSpatialWindow(...)
    }
}

context.bindService(
    Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE"),
    connection,
    Context.BIND_AUTO_CREATE
)
```

## 📦 SDK Package

### Maven/Gradle Dependency
```gradle
dependencies {
    implementation 'com.augmentalis:voiceui-sdk:3.0.0'
}
```

### SDK Classes
```kotlin
// High-level API wrapper
class VoiceUIClient(context: Context) {
    fun setTheme(theme: String)
    fun showNotification(message: String)
    fun createWindow(config: WindowConfig)
    fun registerGesture(gesture: GesturePattern)
    fun registerVoiceCommand(command: VoiceCommand)
}
```

## 🔄 Backward Compatibility

### Legacy Intent Support
```kotlin
// Old format (deprecated but still works)
"com.ai.voiceui.ACTION_THEME_CHANGE" → "com.augmentalis.voiceui.action.THEME_CHANGE"
```

### Migration Helper
```kotlin
object VoiceUILegacySupport {
    fun convertLegacyIntent(oldIntent: Intent): Intent {
        // Automatic conversion of old intents
    }
}
```

---
**Status:** Implementation Complete ✅  
**Date:** 2025-08-24  
**Implementation Files:**
- `/apps/VoiceUI/src/main/AndroidManifest.xml` - Permissions and component declarations
- `/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/service/VoiceUIService.kt` - Service implementation
- `/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/receiver/VoiceUIReceiver.kt` - Intent receiver
- `/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/provider/VoiceUIProvider.kt` - Content provider

**Integration Status:**
- ✅ VoiceUI module integrated into main app (`VoiceOS.kt`)
- ✅ All intent actions registered in manifest
- ✅ Service binding implemented
- ✅ Content provider with full CRUD operations
- ✅ Permission system configured

**Priority:** COMPLETE - Ready for third-party integration
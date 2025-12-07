# VoiceUI Complete API Reference

## 📚 Table of Contents
1. [Intent API Reference](#intent-api-reference)
2. [Content Provider API](#content-provider-api)
3. [Service Binding API](#service-binding-api)
4. [Permission Reference](#permission-reference)
5. [Broadcast Events](#broadcast-events)
6. [Code Examples](#code-examples)

---

## 🎯 Intent API Reference

### Theme Management

#### `THEME_CHANGE`
```kotlin
Action: "com.augmentalis.voiceui.action.THEME_CHANGE"
Extras:
  - theme_name: String (required) - "arvision", "material", "visionos"
  - animate: Boolean (optional, default: true)
Permission: com.augmentalis.voiceui.permission.CHANGE_THEME
```

#### `THEME_GET`
```kotlin
Action: "com.augmentalis.voiceui.action.THEME_GET"
Returns: Broadcast with current theme
Permission: None
```

#### `THEME_REGISTER`
```kotlin
Action: "com.augmentalis.voiceui.action.THEME_REGISTER"
Extras:
  - theme_name: String (required)
  - theme_config: String (required) - JSON configuration
Permission: com.augmentalis.voiceui.permission.CHANGE_THEME
```

### Gesture Management

#### `GESTURE_ENABLE`
```kotlin
Action: "com.augmentalis.voiceui.action.GESTURE_ENABLE"
Extras:
  - gesture_type: String (required) - "tap", "swipe", "pinch", "rotate"
  - enabled: Boolean (required)
Permission: com.augmentalis.voiceui.permission.TRIGGER_GESTURES
```

#### `GESTURE_TRIGGER`
```kotlin
Action: "com.augmentalis.voiceui.action.GESTURE_TRIGGER"
Extras:
  - gesture_type: String (required)
  - x: Float (required)
  - y: Float (required)
Permission: com.augmentalis.voiceui.permission.TRIGGER_GESTURES
```

#### `GESTURE_CONFIG`
```kotlin
Action: "com.augmentalis.voiceui.action.GESTURE_CONFIG"
Extras:
  - sensitivity: Float (optional, 0.0-1.0, default: 0.8)
  - multi_touch: Boolean (optional, default: true)
Permission: com.augmentalis.voiceui.permission.TRIGGER_GESTURES
```

### Window Management

#### `WINDOW_CREATE`
```kotlin
Action: "com.augmentalis.voiceui.action.WINDOW_CREATE"
Extras:
  - window_id: String (optional, auto-generated if not provided)
  - title: String (required)
  - width: Int (optional, default: 800)
  - height: Int (optional, default: 600)
  - x: Float (optional, default: 0)
  - y: Float (optional, default: 0)
  - z: Float (optional, default: -2) - Spatial depth
Permission: com.augmentalis.voiceui.permission.CONTROL_WINDOWS
```

#### `WINDOW_SHOW` / `WINDOW_HIDE`
```kotlin
Action: "com.augmentalis.voiceui.action.WINDOW_SHOW"
Action: "com.augmentalis.voiceui.action.WINDOW_HIDE"
Extras:
  - window_id: String (required)
  - animated: Boolean (optional, default: true)
Permission: com.augmentalis.voiceui.permission.CONTROL_WINDOWS
```

#### `WINDOW_MOVE`
```kotlin
Action: "com.augmentalis.voiceui.action.WINDOW_MOVE"
Extras:
  - window_id: String (required)
  - x: Float (required)
  - y: Float (required)
  - z: Float (optional)
Permission: com.augmentalis.voiceui.permission.CONTROL_WINDOWS
```

#### `WINDOW_RESIZE`
```kotlin
Action: "com.augmentalis.voiceui.action.WINDOW_RESIZE"
Extras:
  - window_id: String (required)
  - width: Int (required)
  - height: Int (required)
Permission: com.augmentalis.voiceui.permission.CONTROL_WINDOWS
```

### HUD System

#### `HUD_NOTIFY`
```kotlin
Action: "com.augmentalis.voiceui.action.HUD_NOTIFY"
Extras:
  - message: String (required)
  - duration: Int (optional, milliseconds, default: 2000)
  - position: String (optional) - "TOP_CENTER", "BOTTOM_CENTER", etc.
  - priority: String (optional) - "LOW", "NORMAL", "HIGH", "URGENT"
Permission: com.augmentalis.voiceui.permission.SHOW_HUD
```

#### `HUD_UPDATE`
```kotlin
Action: "com.augmentalis.voiceui.action.HUD_UPDATE"
Extras:
  - overlay_id: String (required)
  - content: String (required) - JSON content
Permission: com.augmentalis.voiceui.permission.SHOW_HUD
```

#### `HUD_TOGGLE`
```kotlin
Action: "com.augmentalis.voiceui.action.HUD_TOGGLE"
Extras:
  - visible: Boolean (required)
  - fade_duration: Int (optional, milliseconds, default: 300)
Permission: com.augmentalis.voiceui.permission.SHOW_HUD
```

### Notification System

#### `NOTIFY_CUSTOM`
```kotlin
Action: "com.augmentalis.voiceui.action.NOTIFY_CUSTOM"
Extras:
  - notification_id: String (optional, auto-generated)
  - title: String (required)
  - message: String (required)
  - style: String (optional) - "STANDARD", "SPATIAL", "MINIMAL"
  - actions: String[] (optional) - Array of action labels
Permission: None
```

#### `NOTIFY_CLEAR`
```kotlin
Action: "com.augmentalis.voiceui.action.NOTIFY_CLEAR"
Extras:
  - notification_id: String (required, or "ALL")
Permission: None
```

### Voice Commands

#### `VOICE_REGISTER`
```kotlin
Action: "com.augmentalis.voiceui.action.VOICE_REGISTER"
Extras:
  - command: String (required) - Voice command text
  - action: String (required) - Action to trigger
  - language: String (optional, default: "en-US")
Permission: com.augmentalis.voiceui.permission.REGISTER_COMMANDS
```

#### `VOICE_PROCESS`
```kotlin
Action: "com.augmentalis.voiceui.action.VOICE_PROCESS"
Extras:
  - audio_data: byte[] (required) - Audio bytes
  - language: String (optional, default: "en-US")
Permission: com.augmentalis.voiceui.permission.REGISTER_COMMANDS
```

#### `VOICE_ENABLE`
```kotlin
Action: "com.augmentalis.voiceui.action.VOICE_ENABLE"
Extras:
  - enabled: Boolean (required)
  - wake_word: String (optional, default: "hey voice")
Permission: com.augmentalis.voiceui.permission.REGISTER_COMMANDS
```

### Data Visualization

#### `CHART_CREATE`
```kotlin
Action: "com.augmentalis.voiceui.action.CHART_CREATE"
Extras:
  - chart_type: String (required) - "LINE", "BAR", "PIE", "SCATTER"
  - data: String (required) - JSON data
  - title: String (optional)
Permission: None
```

#### `CHART_UPDATE`
```kotlin
Action: "com.augmentalis.voiceui.action.CHART_UPDATE"
Extras:
  - chart_id: String (required)
  - data: String (required) - JSON data
  - animate: Boolean (optional, default: true)
Permission: None
```

---

## 📊 Content Provider API

### Base URI
```
content://com.augmentalis.voiceui.provider/
```

### Endpoints

#### `/themes`
```sql
Query all themes:
  Columns: name, description, primary_color, is_active
  
Insert custom theme:
  Values: name (String), config (JSON String)
  
Update theme (set active):
  Values: active (Boolean)
  Selection: theme_name = ?
```

#### `/gestures`
```sql
Query gesture configurations:
  Columns: type, enabled, sensitivity
  
Update gesture:
  Values: enabled (Boolean), sensitivity (Float)
  Selection: type = ?
```

#### `/windows`
```sql
Query active windows:
  Columns: window_id, title, x, y, z, width, height, visible
  Selection: visible = ? (optional)
  
Update window:
  Values: x, y, z (Float), width, height (Int), visible (Boolean)
  Selection: window_id = ?
  
Delete window:
  Selection: window_id = ?
```

#### `/notifications`
```sql
Query active notifications:
  Columns: notification_id, title, message, timestamp
  
Delete notification:
  Selection: notification_id = ?
```

#### `/commands`
```sql
Query registered commands:
  Columns: command, action, language
  
Insert command:
  Values: command, action, language (String)
```

#### `/settings`
```sql
Query module settings:
  Columns: key, value, type
  
Update setting:
  Values: value (varies by type)
  Selection: key = ?
```

---

## 🔌 Service Binding API

### Service Connection
```kotlin
// Service class
class: com.augmentalis.voiceui.service.VoiceUIService

// Service action
Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE")
```

### Binder Methods
```kotlin
interface VoiceUIBinder {
    // Direct component access
    val themeEngine: ThemeEngine
    val gestureManager: GestureManager
    val windowManager: WindowManager
    val hudSystem: HUDSystem
    val notificationSystem: NotificationSystem
    val voiceCommandSystem: VoiceCommandSystem
    val dataVisualization: DataVisualization
    
    // Convenience methods
    fun setTheme(themeName: String)
    fun showNotification(message: String, duration: Int)
    fun createWindow(windowConfig: Bundle): String
    fun processGesture(gestureData: Bundle)
    fun registerVoiceCommand(command: String, action: String)
}
```

---

## 🔐 Permission Reference

### Permission Groups

#### Basic Access
```xml
<permission-group 
    android:name="com.augmentalis.voiceui.permission-group.BASIC"
    android:label="VoiceUI Basic Access" />
```

#### Advanced Control
```xml
<permission-group 
    android:name="com.augmentalis.voiceui.permission-group.ADVANCED"
    android:label="VoiceUI Advanced Control" />
```

### Individual Permissions

| Permission | Protection Level | Group | Description |
|------------|-----------------|-------|-------------|
| `CHANGE_THEME` | normal | BASIC | Change UI themes |
| `TRIGGER_GESTURES` | normal | BASIC | Trigger gesture events |
| `SHOW_HUD` | normal | BASIC | Display HUD notifications |
| `CONTROL_WINDOWS` | signature | ADVANCED | Create/control windows |
| `REGISTER_COMMANDS` | signature | ADVANCED | Register voice commands |

---

## 📡 Broadcast Events

### Theme Events
```kotlin
"com.augmentalis.voiceui.broadcast.THEME_CHANGED"
Extras: theme_name (String), previous_theme (String)

"com.augmentalis.voiceui.broadcast.THEME_ERROR"
Extras: error_message (String), theme_name (String)
```

### Gesture Events
```kotlin
"com.augmentalis.voiceui.broadcast.GESTURE_DETECTED"
Extras: gesture_type (String), x (Float), y (Float), velocity (Float), direction (String)

"com.augmentalis.voiceui.broadcast.GESTURE_RECOGNIZED"
Extras: gesture_id (String), confidence (Float), timestamp (Long)
```

### Window Events
```kotlin
"com.augmentalis.voiceui.broadcast.WINDOW_CREATED"
Extras: window_id (String), timestamp (Long)

"com.augmentalis.voiceui.broadcast.WINDOW_CLOSED"
Extras: window_id (String), timestamp (Long)
```

### Chart Events
```kotlin
"com.augmentalis.voiceui.broadcast.CHART_CREATED"
Extras: chart_id (String)
```

---

## 💻 Code Examples

### Example 1: Change Theme via Intent
```kotlin
// Simple theme change
val intent = Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
    setPackage("com.augmentalis.voiceos")
    putExtra("theme_name", "arvision")
    putExtra("animate", true)
}
context.startService(intent)

// Listen for result
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val newTheme = intent.getStringExtra("theme_name")
        Log.d("App", "Theme changed to: $newTheme")
    }
}
context.registerReceiver(receiver, 
    IntentFilter("com.augmentalis.voiceui.broadcast.THEME_CHANGED"))
```

### Example 2: Query Themes via Content Provider
```kotlin
val uri = Uri.parse("content://com.augmentalis.voiceui.provider/themes")
val cursor = contentResolver.query(uri, null, null, null, null)

cursor?.use {
    while (it.moveToNext()) {
        val name = it.getString(it.getColumnIndex("name"))
        val isActive = it.getInt(it.getColumnIndex("is_active")) == 1
        Log.d("App", "Theme: $name, Active: $isActive")
    }
}
```

### Example 3: Service Binding for Direct Control
```kotlin
class MyActivity : AppCompatActivity() {
    private var voiceUIService: VoiceUIService.VoiceUIBinder? = null
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceUIService = service as? VoiceUIService.VoiceUIBinder
            
            // Direct component access
            voiceUIService?.apply {
                themeEngine.setTheme("material")
                windowManager.createSpatialWindow("Settings", 800, 600)
                gestureManager.enableMultiTouch(true)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            voiceUIService = null
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val intent = Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE")
        intent.setPackage("com.augmentalis.voiceos")
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
```

### Example 4: Register Voice Command
```kotlin
// Register a custom voice command
val intent = Intent("com.augmentalis.voiceui.action.VOICE_REGISTER").apply {
    putExtra("command", "open settings")
    putExtra("action", "com.myapp.OPEN_SETTINGS")
    putExtra("language", "en-US")
}
context.startService(intent)

// Handle when command is recognized
val filter = IntentFilter("com.myapp.OPEN_SETTINGS")
context.registerReceiver(object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Open settings activity
        startActivity(Intent(context, SettingsActivity::class.java))
    }
}, filter)
```

### Example 5: Show HUD Notification
```kotlin
// Show HUD notification with position and priority
val intent = Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
    putExtra("message", "Task completed successfully")
    putExtra("duration", 3000)
    putExtra("position", "TOP_CENTER")
    putExtra("priority", "HIGH")
}
context.sendBroadcast(intent)
```

### Example 6: Create Window Programmatically
```kotlin
val windowConfig = Bundle().apply {
    putString("title", "My Window")
    putInt("width", 1024)
    putInt("height", 768)
    putFloat("x", 100f)
    putFloat("y", 200f)
    putFloat("z", -1f)
}

// Via intent
val intent = Intent("com.augmentalis.voiceui.action.WINDOW_CREATE").apply {
    putExtras(windowConfig)
}
context.startService(intent)

// Or via service binding
voiceUIService?.createWindow(windowConfig)
```

---

## 🔄 Migration Guide

### From Interface Pattern to Direct Access
```kotlin
// OLD: Interface-based
interface IVoiceUIModule {
    fun getThemeEngine(): ThemeEngine
    fun getGestureManager(): GestureManager
}

val theme = voiceUI.getThemeEngine()
val gesture = voiceUI.getGestureManager()

// NEW: Direct access (VOS4)
class VoiceUIModule {
    lateinit var themeEngine: ThemeEngine
    lateinit var gestureManager: GestureManager
}

val theme = voiceUI.themeEngine    // Direct property
val gesture = voiceUI.gestureManager // Direct property
```

---

## 📋 Testing Checklist

- [ ] All intents process correctly
- [ ] Permissions enforce properly
- [ ] Content provider queries return data
- [ ] Service binding works
- [ ] Broadcast events fire
- [ ] Direct access pattern maintained
- [ ] No memory leaks
- [ ] Thread safety verified

---

**API Version:** 1.0  
**Module Version:** 3.0.0  
**Last Updated:** 2025-08-24  
**Status:** Complete Implementation
# HUD System API Reference

## Intent API

### HUDIntent Class
Package: `com.augmentalis.voiceos.api`

**System-wide API Location**: `app/src/main/java/com/augmentalis/voiceos/api/HUDIntent.kt`

#### Actions

| Action | Full Action String | Description | Required Extras |
|--------|-------------------|-------------|-----------------|
| `ACTION_SHOW_HUD` | `com.augmentalis.voiceos.ACTION_SHOW_HUD` | Shows the HUD overlay | None |
| `ACTION_HIDE_HUD` | `com.augmentalis.voiceos.ACTION_HIDE_HUD` | Hides the HUD overlay | None |
| `ACTION_TOGGLE_HUD` | `com.augmentalis.voiceos.ACTION_TOGGLE_HUD` | Toggles HUD visibility | `EXTRA_FADE_DURATION` (optional) |
| `ACTION_SHOW_NOTIFICATION` | `com.augmentalis.voiceos.ACTION_SHOW_NOTIFICATION` | Displays a notification | `EXTRA_NOTIFICATION_MESSAGE`, `EXTRA_NOTIFICATION_DURATION` |
| `ACTION_SHOW_VOICE_COMMAND` | `com.augmentalis.voiceos.ACTION_SHOW_VOICE_COMMAND` | Shows voice command indicator | `EXTRA_VOICE_COMMAND`, `EXTRA_VOICE_CONFIDENCE` |
| `ACTION_SHOW_DATA_VISUALIZATION` | `com.augmentalis.voiceos.ACTION_SHOW_DATA_VISUALIZATION` | Displays data chart | `EXTRA_DATA_TYPE`, `EXTRA_DATA_VALUES` |
| `ACTION_UPDATE_OVERLAY` | `com.augmentalis.voiceos.ACTION_UPDATE_OVERLAY` | Updates existing overlay | `EXTRA_OVERLAY_ID`, `EXTRA_OVERLAY_CONTENT` |
| `ACTION_SET_HUD_MODE` | `com.augmentalis.voiceos.ACTION_SET_HUD_MODE` | Changes HUD mode | `EXTRA_HUD_MODE` |
| `ACTION_ENABLE_GAZE_TRACKING` | `com.augmentalis.voiceos.ACTION_ENABLE_GAZE_TRACKING` | Enables eye tracking | None |
| `ACTION_DISABLE_GAZE_TRACKING` | `com.augmentalis.voiceos.ACTION_DISABLE_GAZE_TRACKING` | Disables eye tracking | None |
| `ACTION_SET_ACCESSIBILITY_MODE` | `com.augmentalis.voiceos.ACTION_SET_ACCESSIBILITY_MODE` | Sets accessibility options | `EXTRA_ACCESSIBILITY_MODE` |

#### Extras

| Extra Key | Type | Description |
|-----------|------|-------------|
| `EXTRA_HUD_ELEMENT_ID` | String | Unique identifier for HUD element |
| `EXTRA_NOTIFICATION_MESSAGE` | String | Notification text to display |
| `EXTRA_NOTIFICATION_DURATION` | Int | Display duration in milliseconds |
| `EXTRA_NOTIFICATION_POSITION` | String | Position constant (see HUDPosition) |
| `EXTRA_NOTIFICATION_PRIORITY` | String | Priority level (see HUDPriority) |
| `EXTRA_OVERLAY_ID` | String | Overlay identifier |
| `EXTRA_OVERLAY_CONTENT` | String | Content to display in overlay |
| `EXTRA_VOICE_COMMAND` | String | Voice command text |
| `EXTRA_VOICE_CONFIDENCE` | Float | Confidence level (0.0-1.0) |
| `EXTRA_VOICE_CATEGORY` | String | Command category |
| `EXTRA_DATA_TYPE` | String | Visualization type (see DataVisualizationTypes) |
| `EXTRA_DATA_VALUES` | FloatArray | Data points for visualization |
| `EXTRA_DATA_TITLE` | String | Chart title |
| `EXTRA_HUD_MODE` | String | HUD mode (see HUDModes) |
| `EXTRA_FADE_DURATION` | Int | Fade animation duration in ms |
| `EXTRA_POSITION_X` | Float | X coordinate (-1.0 to 1.0) |
| `EXTRA_POSITION_Y` | Float | Y coordinate (-1.0 to 1.0) |
| `EXTRA_POSITION_Z` | Float | Z depth (-5.0 to -0.5) |
| `EXTRA_ACCESSIBILITY_MODE` | String | Accessibility mode |
| `EXTRA_HIGH_CONTRAST` | Boolean | Enable high contrast |
| `EXTRA_TEXT_SCALE` | Float | Text scaling factor |
| `EXTRA_VOICE_SPEED` | Float | TTS speed (0.5-2.0) |

#### Helper Methods

```kotlin
// Create notification intent
fun createShowNotificationIntent(
    message: String,
    duration: Int = 3000,
    position: String = "CENTER",
    priority: String = "NORMAL"
): Intent

// Create voice command intent
fun createShowVoiceCommandIntent(
    command: String,
    confidence: Float = 0.9f,
    category: String = "SYSTEM"
): Intent

// Create data visualization intent
fun createShowDataVisualizationIntent(
    type: String,
    values: FloatArray,
    title: String = ""
): Intent

// Create overlay update intent
fun createUpdateOverlayIntent(
    overlayId: String,
    content: String
): Intent

// Create HUD mode intent
fun createSetHUDModeIntent(mode: String): Intent

// Create toggle intent
fun createToggleHUDIntent(fadeDuration: Int = 300): Intent

// Create gaze tracking intent
fun createEnableGazeTrackingIntent(): Intent

// Create accessibility intent
fun createSetAccessibilityModeIntent(
    mode: String,
    highContrast: Boolean = false,
    textScale: Float = 1.0f,
    voiceSpeed: Float = 1.0f
): Intent
```

## ContentProvider API

### HUDContentProvider
**Authority**: `com.augmentalis.voiceos.hud.provider`

**System-wide API Location**: `app/src/main/java/com/augmentalis/voiceos/provider/HUDContentProvider.kt`

#### URIs

| URI Path | Operations | Description |
|----------|------------|-------------|
| `/elements` | query, insert, delete | All HUD elements |
| `/elements/{id}` | query, update, delete | Specific element |
| `/status` | query | HUD system status |
| `/config` | query, update | Configuration settings |
| `/notifications` | query, insert | Notification management |
| `/voice_commands` | query, insert | Voice command display |
| `/visualizations` | query, insert | Data visualizations |
| `/gaze_targets` | query | Current gaze targets |
| `/accessibility` | query, update | Accessibility settings |

#### Columns

##### Elements Table
| Column | Type | Description |
|--------|------|-------------|
| `_id` | Integer | Row ID |
| `element_id` | String | Unique element identifier |
| `type` | String | Element type |
| `position_x` | Float | X position |
| `position_y` | Float | Y position |
| `position_z` | Float | Z depth |
| `data` | String | JSON data payload |
| `scale` | Float | Scale factor |
| `visible` | Boolean | Visibility flag |
| `priority` | Integer | Display priority |
| `timestamp` | Long | Creation timestamp |

##### Status Table
| Column | Type | Description |
|--------|------|-------------|
| `status` | String | "visible" or "hidden" |
| `fps` | Float | Current frame rate |
| `mode` | String | Active HUD mode |

##### Config Table
| Column | Type | Description |
|--------|------|-------------|
| `render_mode` | String | Rendering mode |
| `target_fps` | Integer | Target frame rate |
| `vibrancy_enabled` | Boolean | Vibrancy effects |
| `glass_morphism_enabled` | Boolean | Glass effects |

#### Call Methods

```kotlin
// Show HUD
call("showHUD", null, null)

// Hide HUD
call("hideHUD", null, null)

// Toggle HUD
call("toggleHUD", null, null)

// Set HUD mode
call("setHUDMode", "DRIVING", null)

// Enable gaze tracking
call("enableGazeTracking", null, null)

// Get current FPS
val bundle = call("getCurrentFPS", null, null)
val fps = bundle?.getFloat("fps")
```

## Constants

### HUDPosition
- `TOP_LEFT`
- `TOP_CENTER`
- `TOP_RIGHT`
- `CENTER_LEFT`
- `CENTER`
- `CENTER_RIGHT`
- `BOTTOM_LEFT`
- `BOTTOM_CENTER`
- `BOTTOM_RIGHT`

### HUDPriority
- `LOW`
- `NORMAL`
- `HIGH`
- `CRITICAL`

### HUDModes
- `STANDARD`
- `MEETING`
- `DRIVING`
- `WORKSHOP`
- `ACCESSIBILITY`
- `GAMING`
- `ENTERTAINMENT`

### AccessibilityModes
- `STANDARD`
- `VISION_IMPAIRED`
- `HEARING_IMPAIRED`
- `MOTOR_IMPAIRED`
- `COGNITIVE_SUPPORT`
- `FULL_ACCESSIBILITY`

### DataVisualizationTypes
- `LINE_CHART`
- `BAR_CHART`
- `PIE_CHART`
- `SCATTER_PLOT`
- `GAUGE`
- `PROGRESS_BAR`

## Permissions

### Required Permissions
```xml
<!-- Basic HUD usage -->
<uses-permission android:name="com.augmentalis.voiceos.permission.USE_HUD" />

<!-- Advanced HUD management -->
<uses-permission android:name="com.augmentalis.voiceos.permission.MANAGE_HUD" />

<!-- ContentProvider access -->
<uses-permission android:name="com.augmentalis.voiceos.permission.READ_HUD" />
<uses-permission android:name="com.augmentalis.voiceos.permission.WRITE_HUD" />
```

## Result Codes
- `RESULT_SUCCESS` (0): Operation successful
- `RESULT_ERROR_NOT_INITIALIZED` (-1): HUD not initialized
- `RESULT_ERROR_PERMISSION_DENIED` (-2): Missing permission
- `RESULT_ERROR_INVALID_PARAMS` (-3): Invalid parameters
- `RESULT_ERROR_SERVICE_UNAVAILABLE` (-4): Service unavailable

## Example Usage

### Complete Example: Show Voice Command
```kotlin
// Via Intent
val intent = HUDIntent.createShowVoiceCommandIntent(
    command = "Navigate home",
    confidence = 0.95f,
    category = "NAVIGATION"
)
context.sendBroadcast(intent)

// Via ContentProvider
val values = ContentValues().apply {
    put("element_id", UUID.randomUUID().toString())
    put("type", "VOICE_COMMAND")
    put("position_x", 0.0f)
    put("position_y", 0.5f)
    put("position_z", -2.0f)
    put("data", """{"text":"Navigate home","confidence":0.95,"category":"NAVIGATION"}""")
    put("scale", 1.0f)
    put("visible", true)
    put("priority", 5)
}
contentResolver.insert(HUDContentProvider.CONTENT_URI_ELEMENTS, values)
```

### Complete Example: Query HUD Status
```kotlin
val projection = arrayOf("status", "fps", "mode")
val cursor = contentResolver.query(
    HUDContentProvider.CONTENT_URI_STATUS,
    projection,
    null, null, null
)

cursor?.use {
    if (it.moveToFirst()) {
        val status = it.getString(0)
        val fps = it.getFloat(1)
        val mode = it.getString(2)
        Log.d("HUD", "Status: $status, FPS: $fps, Mode: $mode")
    }
}
```
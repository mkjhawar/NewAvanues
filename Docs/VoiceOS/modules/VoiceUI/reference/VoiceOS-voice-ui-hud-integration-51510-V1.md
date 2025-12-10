# VoiceUI HUD Integration Documentation

## Overview
The HUD (Heads-Up Display) system has been integrated into VoiceUI to provide centralized rendering and eliminate duplication following VOS4's zero-overhead principle.

## Architecture

### Components Location
- **VoiceUI** (`apps/VoiceUI/`)
  - `HUDRenderer.kt` - High-performance 90-120 FPS rendering engine
  - `HUDIntent.kt` - Public intent API for external apps
  - `HUDContentProvider.kt` - ContentProvider for data sharing
  - `ARVisionTheme.kt` - Glass morphism and liquid iOS styling
  - `HUDSystem.kt` - Core HUD management (existing, enhanced)

- **HUDManager** (`CodeImport/HUDManager/`)
  - Specialized AR/smart glasses features
  - Spatial rendering and voice indicators
  - Gaze tracking and environmental context
  - Delegates rendering to VoiceUI

## Public APIs

### Intent Actions
```kotlin
// Show/hide HUD
ACTION_SHOW_HUD = "com.augmentalis.voiceui.ACTION_SHOW_HUD"
ACTION_HIDE_HUD = "com.augmentalis.voiceui.ACTION_HIDE_HUD"
ACTION_TOGGLE_HUD = "com.augmentalis.voiceui.ACTION_TOGGLE_HUD"

// Display elements
ACTION_SHOW_NOTIFICATION = "com.augmentalis.voiceui.ACTION_SHOW_NOTIFICATION"
ACTION_SHOW_VOICE_COMMAND = "com.augmentalis.voiceui.ACTION_SHOW_VOICE_COMMAND"
ACTION_SHOW_DATA_VISUALIZATION = "com.augmentalis.voiceui.ACTION_SHOW_DATA_VISUALIZATION"

// Configuration
ACTION_SET_HUD_MODE = "com.augmentalis.voiceui.ACTION_SET_HUD_MODE"
ACTION_ENABLE_GAZE_TRACKING = "com.augmentalis.voiceui.ACTION_ENABLE_GAZE_TRACKING"
ACTION_SET_ACCESSIBILITY_MODE = "com.augmentalis.voiceui.ACTION_SET_ACCESSIBILITY_MODE"
```

### ContentProvider URIs
```kotlin
// Base: content://com.augmentalis.voiceui.hud.provider/
/elements       - HUD elements CRUD operations
/status         - Current HUD status and FPS
/config         - HUD configuration
/notifications  - Notification management
/voice_commands - Voice command display
/visualizations - Data visualization
/gaze_targets   - Gaze tracking targets
/accessibility  - Accessibility settings
```

### Permissions
```xml
<uses-permission android:name="com.augmentalis.voiceui.permission.USE_HUD" />
<uses-permission android:name="com.augmentalis.voiceui.permission.MANAGE_HUD" />
<uses-permission android:name="com.augmentalis.voiceui.permission.READ_HUD" />
<uses-permission android:name="com.augmentalis.voiceui.permission.WRITE_HUD" />
```

## Usage Examples

### Show Notification via Intent
```kotlin
val intent = HUDIntent.createShowNotificationIntent(
    message = "New message received",
    duration = 3000,
    position = HUDPosition.TOP_CENTER,
    priority = HUDPriority.HIGH
)
context.sendBroadcast(intent)
```

### Query HUD Status via ContentProvider
```kotlin
val cursor = contentResolver.query(
    HUDContentProvider.CONTENT_URI_STATUS,
    null, null, null, null
)
cursor?.use {
    if (it.moveToFirst()) {
        val status = it.getString(it.getColumnIndex("status"))
        val fps = it.getFloat(it.getColumnIndex("fps"))
    }
}
```

### Insert HUD Element
```kotlin
val values = ContentValues().apply {
    put("element_id", "my_element_123")
    put("type", "NOTIFICATION")
    put("position_x", 0.5f)
    put("position_y", 0.8f)
    put("data", jsonData)
}
val uri = contentResolver.insert(
    HUDContentProvider.CONTENT_URI_ELEMENTS,
    values
)
```

## HUD Modes
- `STANDARD` - Default mode with balanced features
- `MEETING` - Silent mode with minimal visual distraction
- `DRIVING` - Voice-only with high contrast visuals
- `WORKSHOP` - Hands-free operation with safety focus
- `ACCESSIBILITY` - Enhanced features for impaired users
- `GAMING` - Optimized for gaming overlays
- `ENTERTAINMENT` - Media consumption optimized

## Rendering Performance
- **Standard**: 60 FPS - Battery efficient
- **High**: 90 FPS - Smooth AR experience (default)
- **Ultra**: 120 FPS - Maximum smoothness for premium devices

## ARVision Design System

### Glass Morphism
- Translucent backgrounds: 20-30% opacity
- Blur radius: 15-20dp for depth
- Vibrancy borders: 2px white at 40% opacity

### Liquid Animations
- Responsive scaling: 0.95x pressed, 1.1x active
- Breathing effects: 2-4% size oscillation
- Spring animations with medium bounce
- Confidence shimmer on voice commands

### Color Palette
- Navigation: Vibrant Blue (#007AFF)
- Actions: Vibrant Green (#34C759)
- System: Vibrant Orange (#FF9500)
- Accessibility: Vibrant Purple (#AF52DE)
- Errors: System Red (#FF453A)
- Success: System Green (#30D158)

## Integration with VOS4 Systems
- **IMUManager**: Head tracking and orientation
- **VosDataManager**: Context learning and persistence
- **VoiceAccessibility**: Accessibility features
- **SpeechRecognition**: Voice command processing
- **DeviceManager**: Sensor integration

## Migration Guide

### For Apps Using Old HUD System
1. Update intent actions to new namespace
2. Request new permissions in manifest
3. Use ContentProvider for data operations
4. Update to ARVision theme components

### For HUDManager Users
HUDManager now delegates rendering to VoiceUI automatically. No code changes required for basic usage. Advanced features remain in HUDManager module.

## Best Practices
1. Always check permissions before HUD operations
2. Use appropriate FPS target for battery life
3. Implement proper cleanup in onDestroy()
4. Follow ARVision design guidelines
5. Test with accessibility modes enabled
6. Handle ContentProvider null returns gracefully

## Version History
- v1.0.0 (2025-01-23): Initial integration of HUD into VoiceUI
  - Moved rendering engine from HUDManager
  - Added public Intent API
  - Created ContentProvider
  - Integrated ARVision theme

## Support
For issues or questions, contact the VOS4 development team.
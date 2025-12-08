# VoiceUI Module Documentation

## 🎯 Overview
VoiceUI is a comprehensive UI component library for VoiceOS, providing advanced gesture recognition, spatial window management, theme engines, and voice command processing. The module now features a complete API system for third-party integration.

## 📁 Documentation Structure

### Core Documents
- **[VoiceUI-Integration-Guide.md](./VoiceUI-Integration-Guide.md)** - How to integrate VoiceUI into apps
- **[VoiceUI-Complete-API-Reference.md](./VoiceUI-Complete-API-Reference.md)** - Complete API reference with examples
- **[VoiceUI-API-Intent-Specification.md](./VoiceUI-API-Intent-Specification.md)** - Intent and API specifications
- **[VoiceUI-Architecture-Diagrams.md](./VoiceUI-Architecture-Diagrams.md)** - System architecture with diagrams

### Development Documents
- **[VoiceUI-Developer-Manual.md](./VoiceUI-Developer-Manual.md)** - Developer guide and usage
- **[VoiceUI-Changelog.md](./VoiceUI-Changelog.md)** - Change history and updates
- **[VoiceUI-Module.md](./VoiceUI-Module.md)** - Module specification

## 🏗️ Architecture Overview (ASCII)

```
┌─────────────────────────────────────────────────────────┐
│                   VoiceUI Module                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  API Layer:                                              │
│  ┌────────────┬─────────────┬──────────────────────┐   │
│  │  Receiver  │  Provider   │      Service         │   │
│  │ (Intents)  │ (Content)   │    (Binding)         │   │
│  └─────┬──────┴──────┬──────┴───────────┬──────────┘   │
│        └──────────────┼─────────────────┘              │
│                       ↓                                 │
│  Core Module:   VoiceUIModule (Singleton)              │
│                       ↓                                 │
│  Components:                                            │
│  ┌─────────────────────────────────────────────────┐   │
│  │ • ThemeEngine      • GestureManager             │   │
│  │ • WindowManager    • HUDSystem                  │   │
│  │ • NotificationSys  • VoiceCommandSystem         │   │
│  │ • DataVisualization                             │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  Pattern: VOS4 Direct Access (No Interfaces)            │
│  Namespace: com.augmentalis.voiceui                     │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### 1. Add to Main App
```kotlin
// In VoiceOS.kt
lateinit var voiceUI: VoiceUIModule

// Initialize
voiceUI = VoiceUIModule.getInstance(this)
voiceUI.initialize()
```

### 2. Use via Intent
```kotlin
// Change theme
Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
    putExtra("theme_name", "arvision")
}
```

### 3. Use via Service
```kotlin
// Bind to service
bindService(Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE"),
    connection, Context.BIND_AUTO_CREATE)
    
// Access components
voiceUIService?.themeEngine?.setTheme("material")
```

### 4. Query via Provider
```kotlin
// Get all themes
contentResolver.query(
    Uri.parse("content://com.augmentalis.voiceui.provider/themes"),
    null, null, null, null
)
```

## 📊 API Statistics

| Component | Intents | Provider Endpoints | Service Methods | Broadcasts |
|-----------|---------|-------------------|-----------------|------------|
| Theme | 3 | 1 | 1 | 2 |
| Gesture | 3 | 1 | 1 | 2 |
| Window | 5 | 1 | 3 | 2 |
| HUD | 3 | 0 | 1 | 1 |
| Notification | 2 | 1 | 1 | 1 |
| Voice | 3 | 1 | 1 | 1 |
| Chart | 2 | 0 | 1 | 1 |
| **Total** | **21** | **5** | **9** | **10** |

## 🔐 Permissions

### Required in Manifest
```xml
<uses-permission android:name="com.augmentalis.voiceui.permission.CHANGE_THEME" />
<uses-permission android:name="com.augmentalis.voiceui.permission.CONTROL_WINDOWS" />
<uses-permission android:name="com.augmentalis.voiceui.permission.TRIGGER_GESTURES" />
<uses-permission android:name="com.augmentalis.voiceui.permission.SHOW_HUD" />
<uses-permission android:name="com.augmentalis.voiceui.permission.REGISTER_COMMANDS" />
```

## 🎯 Key Features

### 1. **Theme Engine**
- Material, ARVision, VisionOS themes
- Custom theme registration
- Real-time theme switching
- Compose integration

### 2. **Gesture Manager**
- Multi-touch support
- Air tap for AR glasses
- Force touch detection
- Custom gesture patterns
- Voice-to-gesture mapping

### 3. **Window Manager**
- Spatial window positioning
- Multi-window support
- Window animations
- Z-axis depth control

### 4. **HUD System**
- Smart glasses optimized
- Multiple position zones
- Priority-based display
- Fade animations

### 5. **Notification System**
- Replaces Android defaults
- Voice readout support
- Custom views
- Progress indicators

### 6. **Voice Commands**
- UUID-based targeting
- 7 targeting methods
- Command registration
- Audio processing

### 7. **Data Visualization**
- Line, Bar, Pie charts
- Real-time updates
- Animated transitions

## 📈 Module Status

| Aspect | Status | Details |
|--------|--------|---------|
| **Core Implementation** | ✅ Complete | All components functional |
| **API System** | ✅ Complete | Intents, Service, Provider |
| **Documentation** | ✅ Complete | Full API docs with diagrams |
| **Integration** | ✅ Complete | Integrated in main app |
| **Testing** | ⏳ Pending | Runtime testing needed |
| **SDK** | 📝 Planned | Wrapper library planned |

## 🔄 Recent Changes

### 2025-08-24
- Implemented complete API system (Intent, Service, Provider)
- Added 21 intent actions
- Created service with binding interface
- Implemented content provider with 5 endpoints
- Added comprehensive documentation with diagrams

### 2025-01-23
- Migrated to VOS4 direct access pattern
- Eliminated IVoiceUIModule interface
- Consolidated namespace to com.augmentalis.voiceui

## 📝 Usage Examples

### Direct Access (Internal)
```kotlin
// Get VoiceUI instance
val voiceUI = (application as VoiceOS).voiceUI

// Direct component access
voiceUI.themeEngine.setTheme("arvision")
voiceUI.gestureManager.enableMultiTouch(true)
voiceUI.windowManager.createSpatialWindow(...)
```

### Intent API (External)
```kotlin
// Any app can send intents
Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
    putExtra("message", "Hello VoiceUI")
    putExtra("duration", 3000)
}.let { context.sendBroadcast(it) }
```

### Service Binding (Advanced)
```kotlin
// Bind for direct control
val binder = service as VoiceUIService.VoiceUIBinder
binder.themeEngine.setTheme("material")
binder.windowManager.createWindow(config)
```

## 🧪 Testing

### Build Verification
```bash
# Build VoiceUI module
./gradlew :apps:VoiceUI:assembleDebug

# Run unit tests
./gradlew :apps:VoiceUI:testDebugUnitTest

# Install and test
./gradlew :app:installDebug
```

### Manual Testing
1. Launch app with VoiceUI integrated
2. Test theme changes via Settings
3. Send test intents via adb
4. Verify service binding
5. Query content provider

## 🐛 Known Issues
- Runtime testing pending
- Performance metrics not measured
- SDK wrapper not yet implemented

## 📚 Related Documentation
- [VOS4 Architecture](../../ARCHITECTURE.md)
- [Module Standards](../../DEVELOPER.md)
- [Main App Integration](../../../app/README.md)

## 📞 Support
- Module Owner: VoiceUI Team
- Slack: #voiceui-module
- Issues: /docs/modules/voiceui/issues/

---
**Module Version:** 3.0.0  
**API Version:** 1.0  
**Last Updated:** 2025-08-24  
**Status:** ✅ Implementation Complete, Testing Pending
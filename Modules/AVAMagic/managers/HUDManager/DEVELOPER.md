# HUDManager Developer Documentation

## Architecture Overview

HUDManager implements a comprehensive HUD settings system with full customization capabilities for smart glasses and AR devices.

## Component Architecture

### Core Components

#### 1. HUDSettings.kt
- **Purpose**: Data models for all HUD settings
- **Key Classes**:
  - `HUDSettings`: Master settings container
  - `DisplayElements`: Individual UI element toggles  
  - `VisualSettings`: Visual appearance configuration
  - `PrivacySettings`: Privacy and security options
  - `PerformanceSettings`: Performance optimization
  - `AccessibilitySettings`: Accessibility features

#### 2. HUDSettingsManager.kt
- **Purpose**: Settings persistence and management
- **Responsibilities**:
  - Load/save settings using SharedPreferences
  - Provide reactive settings updates via StateFlow
  - Handle voice commands for settings
  - Apply presets and modes
  - Export/import settings

#### 3. HUDSettingsUI.kt
- **Purpose**: Jetpack Compose UI for settings
- **Features**:
  - Tabbed interface for categories
  - Real-time preview
  - Preset quick actions
  - Sliders for fine-tuning
  - Toggle switches for features

#### 4. SpatialRenderer.kt Extensions
- **New Methods Added**:
  - `setTransparency()`, `setBrightness()`, `setContrast()`
  - `hideAllElements()`, `showAllElements()`, `showElement()`
  - `setContextualMode()`, `applyDisplayElements()`
  - `setDrivingMode()`, `setWorkMode()`, `setFitnessMode()`
  - `setPrivacyMode()`, `blurSensitiveContent()`
  - `centerDisplay()`, `resetPosition()`

## Data Flow

```
User Input (UI/Voice)
        ↓
HUDSettingsManager
        ↓
    StateFlow
    ↓       ↓
HUDManager  UI
    ↓
SpatialRenderer
    ↓
Display Output
```

## Settings Persistence

### Storage Format
- **Type**: SharedPreferences with JSON serialization
- **Key**: "hud_settings" / "settings_json"
- **Library**: Kotlinx Serialization

### Data Model
```kotlin
@Serializable
data class HUDSettings(
    val hudEnabled: Boolean,
    val displayMode: HUDDisplayMode,
    val displayElements: DisplayElements,
    val positioning: PositioningSettings,
    val visual: VisualSettings,
    val privacy: PrivacySettings,
    val performance: PerformanceSettings,
    val accessibility: AccessibilitySettings
)
```

## Voice Commands

### Supported Commands
- **Master Control**: "turn on/off hud", "enable/disable hud"
- **Display Modes**: "minimal mode", "full display", "contextual mode"
- **Privacy**: "privacy mode", "enable/disable privacy"
- **Visual**: "increase/decrease transparency", "brighter/darker"
- **Themes**: "dark mode", "light mode", "auto theme"
- **Elements**: "hide/show notifications", "hide/show time"
- **Performance**: "battery saver", "performance mode"
- **Presets**: "driving mode", "reset settings"

### Command Processing
```kotlin
fun handleVoiceCommand(command: String): Boolean {
    val normalizedCommand = command.toLowerCase().trim()
    
    return when {
        "turn off hud" in normalizedCommand -> {
            toggleHUD(false)
            true
        }
        // ... more commands
    }
}
```

## Display Modes

### HUDDisplayMode Enum
- `OFF`: Completely disabled
- `MINIMAL`: Essential info only (battery, time)
- `CONTEXTUAL`: Adaptive based on activity
- `FULL`: All information visible
- `CUSTOM`: User-defined elements
- `DRIVING`: Navigation focused
- `WORK`: High contrast, productivity
- `FITNESS`: Exercise metrics
- `PRIVACY`: Secure, blurred sensitive data

### Mode Application
Each mode triggers specific renderer configurations:
```kotlin
when (settings.displayMode) {
    HUDDisplayMode.MINIMAL -> applyMinimalMode()
    HUDDisplayMode.DRIVING -> applyDrivingMode()
    // ...
}
```

## Performance Optimization

### Performance Modes
1. **Battery Saver**
   - 30 FPS target
   - Disabled effects
   - Reduced quality

2. **Balanced**
   - 60 FPS target
   - Adaptive quality
   - Smart optimization

3. **Performance**
   - 120 FPS target
   - Maximum quality
   - All effects enabled

### Optimization Techniques
- Lazy initialization
- View recycling in UI
- Coroutine-based updates
- Efficient state management

## UI Components

### Main Screen Structure
```
HUDSettingsScreen
├── TopAppBar (with reset action)
├── TabRow (category selection)
└── LazyColumn (scrollable content)
    ├── MasterToggle
    ├── DisplayModeSelector
    ├── PresetSelector
    ├── DisplayElementsSection
    ├── VisualSettingsSection
    ├── PrivacySettingsSection
    └── PerformanceSettingsSection
```

### Compose Best Practices
- State hoisting with `rememberSaveable`
- Efficient recomposition with `derivedStateOf`
- Material3 theming
- Accessibility support

## Integration Points

### With HUDManager
```kotlin
// In HUDManager.initialize()
observeSettings()

// Settings observation
private fun observeSettings() {
    hudScope.launch {
        settingsManager.settings.collect { settings ->
            applySettings(settings)
        }
    }
}
```

### With SpatialRenderer
Settings changes trigger immediate renderer updates:
- Visual properties (transparency, brightness)
- Element visibility
- Display modes
- Performance targets

## Testing Strategy

### Unit Tests
- Settings persistence
- Voice command parsing
- Mode switching logic
- Preset application

### UI Tests
- Toggle interactions
- Slider value changes
- Tab navigation
- Preset buttons

### Integration Tests
- Settings apply to renderer
- Voice commands update UI
- Performance mode switching
- Privacy mode activation

## Error Handling

### Common Issues
1. **Settings not persisting**
   - Check SharedPreferences permissions
   - Verify JSON serialization

2. **Voice commands not working**
   - Normalize command string
   - Check command patterns

3. **UI not updating**
   - Verify StateFlow collection
   - Check Compose recomposition

## Best Practices

### Code Organization
- Separate data models from logic
- Use sealed classes for modes
- Implement single responsibility

### Performance
- Minimize state updates
- Use immutable data classes
- Batch renderer updates

### User Experience
- Provide immediate feedback
- Support undo/reset
- Clear preset descriptions

## Future Enhancements

### Planned Features
- Cloud settings sync
- Per-app settings
- Gesture controls
- AI-based auto-adjustment
- Custom preset creation
- Settings profiles
- Backup/restore

### API Extensions
- Settings provider for other apps
- Remote settings control
- Settings analytics
- A/B testing framework

## Migration Guide

### From v1.0.0 to v1.1.0
1. No breaking changes
2. New optional settings available
3. Voice commands enhanced
4. UI components added

## Code Examples

### Custom Preset Creation
```kotlin
val customPreset = HUDSettings(
    displayMode = HUDDisplayMode.CUSTOM,
    displayElements = DisplayElements(
        batteryStatus = true,
        time = true,
        notifications = false
    ),
    visual = VisualSettings(
        transparency = 0.7f,
        colorTheme = ColorTheme.DARK
    )
)
settingsManager.saveSettings(customPreset)
```

### Programmatic Settings Update
```kotlin
hudManager.updateSettings {
    copy(
        visual = visual.copy(
            transparency = 0.5f,
            brightness = 1.5f
        )
    )
}
```

### Settings Export/Import
```kotlin
// Export
val json = settingsManager.exportSettings()
saveToFile(json)

// Import
val json = loadFromFile()
settingsManager.importSettings(json)
```

## Support

For questions or issues:
- Check inline documentation
- Review test cases
- Contact VOS4 team

---

Last Updated: 2025-01-24
Version: 1.1.0
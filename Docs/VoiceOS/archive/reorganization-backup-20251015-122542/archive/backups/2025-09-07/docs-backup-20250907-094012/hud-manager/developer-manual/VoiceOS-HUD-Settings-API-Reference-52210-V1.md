# HUD Settings API Reference

## Package Structure
```
com.augmentalis.hudmanager.settings
├── HUDSettings.kt
├── HUDSettingsManager.kt
├── DisplayElement.kt
├── HUDPreset.kt
└── PerformanceMode.kt

com.augmentalis.hudmanager.ui
└── HUDSettingsUI.kt
```

## Core Classes

### HUDSettings
`@Serializable data class HUDSettings`

Main settings container for all HUD configuration.

#### Properties
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| hudEnabled | Boolean | true | Master HUD on/off switch |
| displayMode | HUDDisplayMode | CONTEXTUAL | Current display mode |
| displayElements | DisplayElements | DisplayElements() | Individual UI elements |
| positioning | PositioningSettings | PositioningSettings() | Spatial positioning |
| visual | VisualSettings | VisualSettings() | Visual appearance |
| privacy | PrivacySettings | PrivacySettings() | Privacy options |
| performance | PerformanceSettings | PerformanceSettings() | Performance tuning |
| accessibility | AccessibilitySettings | AccessibilitySettings() | Accessibility features |

#### Companion Object
```kotlin
companion object {
    val DEFAULT: HUDSettings
    val MINIMAL: HUDSettings
    val DRIVING: HUDSettings
    val PRIVACY: HUDSettings
}
```

---

### HUDSettingsManager
`class HUDSettingsManager private constructor(context: Context)`

Singleton manager for HUD settings persistence and management.

#### Public Methods

##### getInstance
```kotlin
fun getInstance(context: Context): HUDSettingsManager
```
Returns singleton instance of settings manager.

##### saveSettings
```kotlin
fun saveSettings(settings: HUDSettings)
```
Persists settings to storage and updates observers.

##### updateSettings
```kotlin
fun updateSettings(block: HUDSettings.() -> HUDSettings)
```
Updates settings using a transformation block.

**Example:**
```kotlin
settingsManager.updateSettings {
    copy(displayMode = HUDDisplayMode.MINIMAL)
}
```

##### toggleHUD
```kotlin
fun toggleHUD(enabled: Boolean? = null)
```
Toggles HUD on/off. If enabled is null, toggles current state.

##### setDisplayMode
```kotlin
fun setDisplayMode(mode: HUDDisplayMode)
```
Sets the display mode.

##### toggleDisplayElement
```kotlin
fun toggleDisplayElement(element: DisplayElement)
```
Toggles visibility of a specific display element.

##### applyPreset
```kotlin
fun applyPreset(preset: HUDPreset)
```
Applies a preset configuration.

##### adjustTransparency
```kotlin
fun adjustTransparency(value: Float)
```
Sets transparency (0.0 - 1.0).

##### adjustBrightness
```kotlin
fun adjustBrightness(value: Float)
```
Sets brightness (0.5 - 2.0).

##### setColorTheme
```kotlin
fun setColorTheme(theme: ColorTheme)
```
Sets the color theme.

##### enablePrivacyMode
```kotlin
fun enablePrivacyMode(enabled: Boolean = true)
```
Enables/disables privacy mode with all privacy features.

##### setPerformanceMode
```kotlin
fun setPerformanceMode(mode: PerformanceMode)
```
Sets performance optimization mode.

##### handleVoiceCommand
```kotlin
fun handleVoiceCommand(command: String): Boolean
```
Processes voice command. Returns true if handled.

##### resetToDefaults
```kotlin
fun resetToDefaults()
```
Resets all settings to default values.

##### exportSettings
```kotlin
fun exportSettings(): String
```
Exports settings as JSON string.

##### importSettings
```kotlin
fun importSettings(jsonString: String): Boolean
```
Imports settings from JSON. Returns true if successful.

#### Properties

##### settings
```kotlin
val settings: StateFlow<HUDSettings>
```
Observable flow of current settings.

##### currentSettings
```kotlin
val currentSettings: HUDSettings
```
Current settings snapshot.

---

## Data Classes

### DisplayElements
```kotlin
@Serializable
data class DisplayElements(
    val batteryStatus: Boolean = true,
    val time: Boolean = true,
    val date: Boolean = false,
    val networkStatus: Boolean = false,
    val voiceCommands: Boolean = true,
    val gazeTarget: Boolean = true,
    val gestureHints: Boolean = false,
    val notifications: Boolean = true,
    val messages: Boolean = true,
    val calls: Boolean = true,
    val alerts: Boolean = true,
    val contextualInfo: Boolean = true,
    val appSuggestions: Boolean = false,
    val smartReplies: Boolean = false,
    val navigationHints: Boolean = false,
    val compass: Boolean = false,
    val speedInfo: Boolean = false,
    val miniMap: Boolean = false,
    val systemDiagnostics: Boolean = false,
    val developerInfo: Boolean = false,
    val performanceMetrics: Boolean = false
)
```

### PositioningSettings
```kotlin
@Serializable
data class PositioningSettings(
    val hudDistance: Float = 2.0f,
    val verticalOffset: Float = 0f,
    val horizontalOffset: Float = 0f,
    val textSize: TextSize = TextSize.MEDIUM,
    val iconSize: IconSize = IconSize.MEDIUM,
    val layout: LayoutStyle = LayoutStyle.CENTERED,
    val anchorPoint: AnchorPoint = AnchorPoint.CENTER,
    val autoAdjust: Boolean = true
)
```

### VisualSettings
```kotlin
@Serializable
data class VisualSettings(
    val transparency: Float = 0.8f,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val colorTheme: ColorTheme = ColorTheme.AUTO,
    val accentColor: String = "#2196F3",
    val fontSize: Float = 1.0f,
    val animations: Boolean = true,
    val smoothTransitions: Boolean = true,
    val antiAliasing: Boolean = true,
    val nightMode: Boolean = false,
    val highContrast: Boolean = false
)
```

### PrivacySettings
```kotlin
@Serializable
data class PrivacySettings(
    val hideInPublic: Boolean = false,
    val blurSensitiveContent: Boolean = true,
    val disableInMeetings: Boolean = true,
    val disableScreenshots: Boolean = false,
    val incognitoMode: Boolean = false,
    val hiddenApps: List<String> = emptyList(),
    val trustedNetworks: List<String> = emptyList(),
    val requireAuthentication: Boolean = false
)
```

### PerformanceSettings
```kotlin
@Serializable
data class PerformanceSettings(
    val targetFps: Int = 60,
    val batteryOptimization: Boolean = true,
    val adaptiveQuality: Boolean = true,
    val maxRenderDistance: Float = 10f,
    val particleEffects: Boolean = true,
    val shadowQuality: ShadowQuality = ShadowQuality.MEDIUM,
    val textureQuality: TextureQuality = TextureQuality.HIGH
)
```

### AccessibilitySettings
```kotlin
@Serializable
data class AccessibilitySettings(
    val voiceAnnouncements: Boolean = false,
    val hapticFeedback: Boolean = true,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.OFF,
    val largeText: Boolean = false,
    val boldText: Boolean = false,
    val reduceMotion: Boolean = false,
    val screenReader: Boolean = false,
    val magnification: Float = 1.0f,
    val highContrastText: Boolean = false
)
```

---

## Enums

### HUDDisplayMode
```kotlin
@Serializable
enum class HUDDisplayMode {
    OFF,           // Completely disabled
    MINIMAL,       // Essential info only
    CONTEXTUAL,    // Smart adaptive
    FULL,          // All information
    CUSTOM,        // User-defined
    DRIVING,       // Navigation focused
    WORK,          // Productivity mode
    FITNESS,       // Exercise tracking
    PRIVACY        // Secure mode
}
```

### DisplayElement
```kotlin
enum class DisplayElement {
    BATTERY,
    TIME,
    DATE,
    NOTIFICATIONS,
    MESSAGES,
    VOICE_COMMANDS,
    GAZE_TARGET,
    NAVIGATION,
    COMPASS,
    SYSTEM_INFO
}
```

### HUDPreset
```kotlin
enum class HUDPreset {
    DEFAULT,
    MINIMAL,
    DRIVING,
    PRIVACY,
    CUSTOM
}
```

### PerformanceMode
```kotlin
enum class PerformanceMode {
    BATTERY_SAVER,  // 30 FPS, reduced effects
    BALANCED,       // 60 FPS, adaptive
    PERFORMANCE     // 120 FPS, maximum
}
```

### TextSize
```kotlin
@Serializable
enum class TextSize {
    TINY,
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}
```

### IconSize
```kotlin
@Serializable
enum class IconSize {
    SMALL,
    MEDIUM,
    LARGE
}
```

### LayoutStyle
```kotlin
@Serializable
enum class LayoutStyle {
    CENTERED,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM,
    FLOATING,
    PERIPHERAL,
    CUSTOM
}
```

### AnchorPoint
```kotlin
@Serializable
enum class AnchorPoint {
    CENTER,
    HEAD_LOCKED,
    WORLD_LOCKED,
    GAZE_LOCKED
}
```

### ColorTheme
```kotlin
@Serializable
enum class ColorTheme {
    AUTO,
    LIGHT,
    DARK,
    HIGH_CONTRAST,
    BLUE_LIGHT_FILTER,
    CUSTOM
}
```

### ShadowQuality
```kotlin
@Serializable
enum class ShadowQuality {
    OFF,
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}
```

### TextureQuality
```kotlin
@Serializable
enum class TextureQuality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}
```

### ColorBlindMode
```kotlin
@Serializable
enum class ColorBlindMode {
    OFF,
    PROTANOPIA,    // Red-blind
    DEUTERANOPIA,  // Green-blind
    TRITANOPIA,    // Blue-blind
    MONOCHROME     // Complete color blindness
}
```

---

## UI Components

### HUDSettingsScreen
```kotlin
@Composable
fun HUDSettingsScreen(
    settingsManager: HUDSettingsManager,
    onBack: () -> Unit = {}
)
```
Main settings screen with tabbed interface.

### MasterToggle
```kotlin
@Composable
fun MasterToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
)
```
Large card with HUD on/off switch.

### DisplayModeSelector
```kotlin
@Composable
fun DisplayModeSelector(
    currentMode: HUDDisplayMode,
    onModeSelected: (HUDDisplayMode) -> Unit
)
```
Radio button group for mode selection.

### PresetSelector
```kotlin
@Composable
fun PresetSelector(
    onPresetSelected: (HUDPreset) -> Unit
)
```
Quick action buttons for presets.

### DisplayElementsSection
```kotlin
@Composable
fun DisplayElementsSection(
    elements: DisplayElements,
    onToggle: (DisplayElement) -> Unit
)
```
List of toggleable UI elements.

### VisualSettingsSection
```kotlin
@Composable
fun VisualSettingsSection(
    visual: VisualSettings,
    onTransparencyChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onThemeChange: (ColorTheme) -> Unit
)
```
Visual adjustment controls.

### PrivacySettingsSection
```kotlin
@Composable
fun PrivacySettingsSection(
    privacy: PrivacySettings,
    onUpdate: (PrivacySettings) -> Unit
)
```
Privacy toggles and options.

### PerformanceSettingsSection
```kotlin
@Composable
fun PerformanceSettingsSection(
    performance: PerformanceSettings,
    onUpdate: (PerformanceSettings) -> Unit
)
```
Performance tuning controls.

---

## Voice Commands

### Command Patterns

| Command | Action | Example |
|---------|--------|---------|
| "turn on/off hud" | Toggle HUD | "turn off hud" |
| "minimal/full display" | Set mode | "minimal mode" |
| "privacy mode" | Enable privacy | "enable privacy mode" |
| "increase/decrease {property}" | Adjust value | "increase transparency" |
| "hide/show {element}" | Toggle element | "hide notifications" |
| "{theme} mode" | Set theme | "dark mode" |
| "battery saver" | Performance mode | "enable battery saver" |
| "reset settings" | Reset to default | "reset to defaults" |

---

## Usage Examples

### Basic Setup
```kotlin
// Initialize
val settingsManager = HUDSettingsManager.getInstance(context)
val hudManager = HUDManager.getInstance(context)

// Set up observer
lifecycleScope.launch {
    settingsManager.settings.collect { settings ->
        // React to changes
        updateUI(settings)
    }
}
```

### Programmatic Control
```kotlin
// Toggle HUD
settingsManager.toggleHUD()

// Set display mode
settingsManager.setDisplayMode(HUDDisplayMode.MINIMAL)

// Adjust visual settings
settingsManager.updateSettings {
    copy(
        visual = visual.copy(
            transparency = 0.5f,
            brightness = 1.5f,
            colorTheme = ColorTheme.DARK
        )
    )
}

// Apply preset
settingsManager.applyPreset(HUDPreset.DRIVING)
```

### Voice Control
```kotlin
// Process voice command
val command = "turn on privacy mode"
if (settingsManager.handleVoiceCommand(command)) {
    // Command handled successfully
}
```

### UI Integration
```kotlin
@Composable
fun MyApp() {
    val settingsManager = remember { 
        HUDSettingsManager.getInstance(context) 
    }
    
    HUDSettingsScreen(
        settingsManager = settingsManager,
        onBack = { navController.popBackStack() }
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
if (settingsManager.importSettings(json)) {
    // Import successful
}
```

---

## Error Handling

### Common Exceptions

| Exception | Cause | Solution |
|-----------|-------|----------|
| IllegalArgumentException | Invalid setting value | Validate before updating |
| SerializationException | Corrupted JSON | Use try-catch on import |
| IllegalStateException | Manager not initialized | Ensure getInstance called |

### Error Recovery
```kotlin
try {
    settingsManager.importSettings(json)
} catch (e: SerializationException) {
    // Reset to defaults on corrupt data
    settingsManager.resetToDefaults()
}
```

---

## Thread Safety

All public methods are thread-safe. Settings updates are atomic using copy-on-write pattern.

```kotlin
// Safe from any thread
settingsManager.updateSettings {
    copy(displayMode = HUDDisplayMode.MINIMAL)
}
```

---

## Version Compatibility

| Version | Min SDK | Target SDK | Kotlin |
|---------|---------|------------|--------|
| 1.1.0 | 28 (Android 9) | 34 (Android 14) | 1.9.24 |

---

**API Version:** 1.1.0  
**Last Updated:** 2025-01-24
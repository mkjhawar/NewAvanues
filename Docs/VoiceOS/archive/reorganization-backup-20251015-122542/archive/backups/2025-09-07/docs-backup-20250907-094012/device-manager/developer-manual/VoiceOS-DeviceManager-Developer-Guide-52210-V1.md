# Device Detection System - Developer Guide

## Overview

The VOS4 Device Detection System provides a **simplified, capability-based approach** to device detection. Instead of complex manufacturer-specific detection files, we focus on **what the device can do** (capabilities) and **how to integrate with it** (speech systems, permissions).

### Key Principles

1. **Capability-First**: Detect what matters - hardware capabilities and behavioral needs
2. **No Manufacturer Files**: Single source of truth in DeviceDetector
3. **Android-Native**: Leverage Android's built-in hardware detection
4. **Integration Focus**: Only store device-specific integration requirements
5. **Cache-Friendly**: Fast startup with JSON caching

## Architecture

```
/deviceinfo/detection/
├── DeviceDetector.kt              # Main orchestrator (single source of truth)
├── integration/
│   └── device_integration.json    # Integration requirements (speech, logos)
└── cache/
    └── device_capabilities.json   # Cached capabilities (auto-generated)
```

## Core Components

### 1. DeviceDetector

The central orchestrator that:
- Detects hardware capabilities using Android APIs
- Determines behavioral requirements
- Loads integration requirements from JSON
- Caches results for fast startup

```kotlin
val capabilities = DeviceDetector.getCapabilities(context)
```

### 2. DeviceCapabilities

Complete device profile containing:

```kotlin
data class DeviceCapabilities(
    val deviceInfo: DeviceInfo,           // Manufacturer, model, logo
    val hardware: HardwareCapabilities,   // NFC, Bluetooth, sensors
    val behavioral: BehavioralCapabilities, // Voice-first, battery optimization
    val speechIntegration: SpeechIntegration // System speech requirements
)
```

### 3. Integration Configuration

Simple JSON defining how to work with specific devices:

```json
{
  "devices": {
    "realwear": {
      "pattern": ["realwear", "hmt", "navigator"],
      "speechSystem": "WearHF",
      "requiresDisable": true,
      "logo": "logos/realwear.png",
      "displayName": "RealWear"
    }
  }
}
```

## Usage Examples

### Basic Detection

```kotlin
class MyManager(context: Context) {
    private val capabilities = DeviceDetector.getCapabilities(context)
    
    fun initialize() {
        // Check hardware
        if (capabilities.hardware.hasNfc) {
            enableNfcFeatures()
        }
        
        // Check behavioral needs
        if (capabilities.behavioral.isVoiceFirst) {
            enableVoiceControl()
        }
        
        // Handle speech integration
        if (capabilities.speechIntegration.requiresDisable) {
            disableSystemSpeech()
        }
    }
}
```

### Smart Glass Detection

```kotlin
// No need to check manufacturer
if (capabilities.behavioral.isSmartGlass) {
    // Adjust UI for smart glasses
    setCursorDelay(capabilities.behavioral.cursorStabilizationDelay)
    enableLargeTouchTargets()
}
```

### Display Device Info

```kotlin
// Show device branding
deviceNameText.text = capabilities.deviceInfo.displayName
capabilities.deviceInfo.logo?.let { path ->
    deviceLogo.setImageResource(getResourceId(path))
}
```

## Adding New Devices

### Step 1: Update Integration JSON

Add device to `/deviceinfo/detection/integration/device_integration.json`:

```json
{
  "devices": {
    "newdevice": {
      "pattern": ["manufacturer_name", "device_model"],
      "speechSystem": "DeviceSpeechSDK",
      "requiresDisable": false,
      "logo": "logos/newdevice.png",
      "displayName": "New Device"
    }
  }
}
```

### Step 2: Add Logo Asset

Place logo in `/res/drawable/logos/newdevice.png`

### Step 3: Test Detection

```kotlin
val capabilities = DeviceDetector.getCapabilities(context)
Log.d("Device", "Detected: ${capabilities.deviceInfo.displayName}")
Log.d("Speech", "System: ${capabilities.speechIntegration.systemName}")
```

## Caching System

### Automatic Caching

DeviceDetector automatically caches capabilities on first run:

```json
{
  "v": 1,
  "device": {
    "mfg": "RealWear",
    "model": "Navigator-520",
    "android": 30
  },
  "hw": {
    "nfc": false,
    "bt": true,
    "wifi": true,
    "cam": true
  },
  "behavior": {
    "smartGlass": true,
    "voiceFirst": true,
    "cursorDelay": 800
  },
  "cached": 1704067200000
}
```

### Cache Management

```kotlin
// Force refresh (e.g., after system update)
DeviceDetector.clearCache()
val freshCapabilities = DeviceDetector.getCapabilities(context)

// Check dynamic capabilities (USB devices)
val capabilities = DeviceDetector.getCapabilities(context, checkDynamic = true)
```

## Best Practices

### DO ✅

1. **Use capabilities, not manufacturer checks**
   ```kotlin
   // Good
   if (capabilities.behavioral.isVoiceFirst) { }
   
   // Bad
   if (Build.MANUFACTURER == "RealWear") { }
   ```

2. **Cache capabilities in your manager**
   ```kotlin
   class MyManager(context: Context) {
       private val capabilities = DeviceDetector.getCapabilities(context)
   }
   ```

3. **Check behavioral capabilities for UI decisions**
   ```kotlin
   val displayMode = when {
       capabilities.behavioral.isSmartGlass -> DisplayMode.MINIMAL
       capabilities.hardware.hasCamera -> DisplayMode.AR_OVERLAY
       else -> DisplayMode.STANDARD
   }
   ```

### DON'T ❌

1. **Don't create manufacturer-specific code paths**
2. **Don't duplicate hardware detection**
3. **Don't modify cached JSON manually**
4. **Don't skip integration requirements**

## Integration with Managers

### GlassesManager Example

```kotlin
class GlassesManager(
    private val context: Context,
    private val capabilities: DeviceCapabilities
) {
    fun initialize() {
        // Pure capability-based decisions
        if (!capabilities.behavioral.isSmartGlass) {
            return // Not a smart glass device
        }
        
        // Apply smart glass optimizations
        configureForSmartGlass()
    }
}
```

### SpeechRecognitionManager Example

```kotlin
class SpeechRecognitionManager(
    private val context: Context,
    private val capabilities: DeviceCapabilities
) {
    fun initialize() {
        when (capabilities.speechIntegration.systemName) {
            "WearHF" -> {
                if (capabilities.speechIntegration.requiresDisable) {
                    disableWearHF()
                }
            }
            "VuzixSpeechSDK" -> {
                // Can coexist, no action needed
            }
        }
    }
}
```

## Testing

### Mock Capabilities

```kotlin
@Test
fun testSmartGlassBehavior() {
    val mockCapabilities = DeviceCapabilities(
        deviceInfo = DeviceInfo("Test", "Model", "device", 30, "Test", null),
        hardware = HardwareCapabilities(false, true, true, true, true, false),
        behavioral = BehavioralCapabilities(
            isSmartGlass = true,
            isVoiceFirst = true,
            needsBatteryOptimization = true,
            cursorStabilizationDelay = 800L
        ),
        speechIntegration = SpeechIntegration("TestSpeech", false)
    )
    
    val manager = GlassesManager(context, mockCapabilities)
    // Test smart glass behavior
}
```

### Integration Testing

```kotlin
@Test
fun testRealWearDetection() {
    // On actual RealWear device
    val capabilities = DeviceDetector.getCapabilities(context)
    
    assertTrue(capabilities.behavioral.isSmartGlass)
    assertEquals("WearHF", capabilities.speechIntegration.systemName)
    assertTrue(capabilities.speechIntegration.requiresDisable)
}
```

## Troubleshooting

### Device Not Detected

1. Check pattern matching in `device_integration.json`
2. Verify Build.MANUFACTURER, Build.MODEL, Build.DEVICE values
3. Check logcat for detection logs

### Cache Issues

```kotlin
// Clear and rebuild cache
DeviceDetector.clearCache()
```

### Integration Not Working

1. Verify JSON syntax in `device_integration.json`
2. Check logo asset path exists
3. Verify speech system name matches actual system

## Migration Guide

### From Old Manufacturer-Specific System

```kotlin
// Old way ❌
if (SamsungDetector.isSamsung()) {
    val features = SamsungDetector.detectFeatures(context)
    if (features.spen != null) {
        enableSPenSupport()
    }
}

// New way ✅
val capabilities = DeviceDetector.getCapabilities(context)
if (capabilities.hardware.hasStylusSupport) {
    enableStylusSupport()
}
```

### From XRManager

```kotlin
// Old way ❌
val xrManager = XRManager(context)
if (xrManager.isXRSupported()) {
    xrManager.enterXRMode()
}

// New way ✅
val capabilities = DeviceDetector.getCapabilities(context)
if (capabilities.hardware.hasXrSupport) {
    // XR features handled by specialized manager
}
```

## Performance

- **First run**: ~50ms (full detection)
- **Cached runs**: <5ms (JSON load)
- **Dynamic check**: ~10ms (USB/accessories only)
- **Memory**: ~10KB for cached JSON

## Future Enhancements

1. **Remote Config**: Download integration updates
2. **A/B Testing**: Test different behavioral profiles
3. **Analytics**: Track capability usage
4. **Custom Profiles**: User-defined behavioral overrides

---

**Last Updated**: 2025-09-05
**Version**: 1.0.0
**Module**: DeviceManager
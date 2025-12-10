# HUD System Localization Guide

## Overview
The HUD system is fully integrated with VOS4's LocalizationManager, supporting 42+ languages for all HUD notifications, modes, and voice commands.

## Supported Languages

### Full Support (42+ languages via Vivoka)
- English (en), Spanish (es), French (fr), German (de), Italian (it)
- Portuguese (pt), Russian (ru), Chinese (zh), Japanese (ja), Korean (ko)
- Arabic (ar), Dutch (nl), Polish (pl), Turkish (tr), Hindi (hi)
- Thai (th), Czech (cs), Danish (da), Finnish (fi), Greek (el)
- Hebrew (he), Hungarian (hu), Norwegian (no), Swedish (sv), Ukrainian (uk)
- Bulgarian (bg), Croatian (hr), Romanian (ro), Slovak (sk), Slovenian (sl)
- Estonian (et), Latvian (lv), Lithuanian (lt), Icelandic (is), Irish (ga)
- Maltese (mt), Albanian (sq), Macedonian (mk), Serbian (sr), Bosnian (bs), Welsh (cy)

### Voice Recognition Support (8 languages via Vosk)
- English, Spanish, French, German, Russian, Chinese, Japanese, Korean

## Using Localization in HUD

### 1. Automatic Localization
```kotlin
// Notifications automatically localize based on system language
val intent = HUDIntent.createShowNotificationIntent(
    message = "Hello World",
    autoLocalize = true  // Default
)
```

### 2. Translation Keys
```kotlin
// Use translation keys for pre-defined messages
val intent = HUDIntent.createLocalizedNotificationIntent(
    context = context,
    translationKey = "hud.notification.incoming_call",
    args = arrayOf("John Doe")  // Replaces %s in translation
)
```

### 3. Manual Language Selection
```kotlin
// Force specific language
val intent = HUDIntent.createShowNotificationIntent(
    message = "Hello World",
    languageCode = "es"  // Forces Spanish
)
```

## Available Translation Keys

### Notifications
- `hud.notification.incoming_call` - "Incoming call from %s"
- `hud.notification.message` - "New message from %s"
- `hud.notification.battery_low` - "Battery low: %d%%"
- `hud.notification.app_opened` - "%s opened"
- `hud.notification.voice_command` - "Command: %s"
- `hud.notification.gaze_target` - "Looking at: %s"

### HUD Modes
- `hud.mode.standard` - "Standard Mode"
- `hud.mode.meeting` - "Meeting Mode"
- `hud.mode.driving` - "Driving Mode"
- `hud.mode.workshop` - "Workshop Mode"
- `hud.mode.accessibility` - "Accessibility Mode"
- `hud.mode.gaming` - "Gaming Mode"
- `hud.mode.entertainment` - "Entertainment Mode"

### Status Messages
- `hud.status.connected` - "Connected"
- `hud.status.disconnected` - "Disconnected"
- `hud.status.loading` - "Loading..."
- `hud.status.ready` - "HUD Ready"

## HUDManager Integration

### Displaying Localized Notifications
```kotlin
// Using HUDManager directly
hudManager.showLocalizedNotification(
    translationKey = "hud.notification.battery_low",
    position = SpatialPosition.TOP_RIGHT,
    priority = NotificationPriority.HIGH,
    args = arrayOf(15)  // Battery percentage
)
```

### Voice Commands with Localization
```kotlin
// Commands automatically localized
val commands = listOf(
    VoiceCommand(
        translationKey = "cmd.back",
        action = { performBack() }
    ),
    VoiceCommand(
        translationKey = "cmd.home",
        action = { performHome() }
    )
)
hudManager.showVoiceCommands(commands, uiContext)
```

## ContentProvider Localization

### Querying with Language
```kotlin
val values = ContentValues().apply {
    put("message", "hud.notification.message")
    put("is_translation_key", true)
    put("language_code", "fr")  // French
    put("args", "Marie")
}
contentResolver.insert(HUDContentProvider.CONTENT_URI_NOTIFICATIONS, values)
```

## Language Change Handling

### Listening for Language Changes
```kotlin
// HUDManager automatically updates when language changes
LocalizationModule.getInstance(context).languageState.collect { language ->
    // HUD automatically updates all displayed content
    Log.d("HUD", "Language changed to: $language")
}
```

## Adding New Translations

### 1. Update LocalizationModule
```kotlin
// In LocalizationModule.kt
private fun loadCustomTranslations() {
    translations["custom"] = mapOf(
        "hud.custom.message" to "Custom message",
        // Add more translations
    )
}
```

### 2. Use in HUD
```kotlin
hudManager.showLocalizedNotification(
    translationKey = "hud.custom.message"
)
```

## Best Practices

1. **Always use translation keys** for user-facing text
2. **Test with multiple languages** to ensure UI layout works
3. **Provide fallbacks** for unsupported languages
4. **Use placeholders** (%s, %d) for dynamic content
5. **Keep translations concise** for AR displays

## Performance Considerations

- Translations are **cached in memory** for instant access
- Language changes trigger **single UI update**
- No network calls required (all local)
- **Zero overhead** for same-language operations

## Testing Localization

```kotlin
// Test language switching
fun testHUDLocalization() {
    val localization = LocalizationModule.getInstance(context)
    val hudManager = HUDManager.getInstance(context)
    
    // Test each language
    listOf("en", "es", "fr", "de", "ja", "zh").forEach { lang ->
        localization.setLanguage(lang)
        hudManager.showLocalizedNotification(
            "hud.notification.incoming_call",
            args = arrayOf("Test User")
        )
        Thread.sleep(2000)
    }
}
```

## Accessibility Considerations

- **High contrast mode** adjusts automatically per language
- **Text-to-speech** uses appropriate voice per language
- **RTL languages** (Arabic, Hebrew) handled automatically
- **Font scaling** adjusts based on script complexity

## Future Enhancements

- [ ] Dynamic translation loading from server
- [ ] User-customizable translations
- [ ] Regional dialect support
- [ ] Voice command synonyms per language
- [ ] Gesture descriptions localization
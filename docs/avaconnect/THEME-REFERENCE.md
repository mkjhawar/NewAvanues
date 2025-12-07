# AvaConnect Theme Reference

Theme and design system integration for AvaConnect.

## Quick Integration

```javascript
import ThemeEngine from '@shared/ui/theme-engine';
import VoiceCommandSystem from '@shared/voice/voice-commands';

const themeEngine = new ThemeEngine();
const voiceSystem = new VoiceCommandSystem(themeEngine);

// Recommended theme for AvaConnect
themeEngine.switchTheme('teal');  // Calming for communication app
```

## Recommended Settings

```javascript
{
    glassMode: true,        // Glass UI for modern look
    glassBlur: 50,          // Slightly more blur for depth
    radiusSm: 8,            // Softer corners for friendliness
    radiusMd: 12,
    radiusLg: 16,
    shadowIntensity: 0.8,   // Subtle shadows
    motionSpeed: 1.1        // Slightly faster for responsiveness
}
```

## Voice Commands Priority

AvaConnect requires comprehensive voice commands for accessibility during communication:

```javascript
// Critical AvaConnect voice commands
voiceSystem.registerElement(callButton, {
    primary: 'start call',
    alternatives: ['call', 'dial', 'connect'],
    description: 'Initiate voice/video call',
    priority: 'high',
    context: 'avaconnect-call'
});
```

## References

- **Design Tokens:** `docs/shared-libs/ui/DESIGN-TOKENS.md`
- **Theme Engine:** `docs/shared-libs/ui/theme-engine/`
- **Voice System:** `docs/shared-libs/voice/`

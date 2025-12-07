# Ava Theme Reference

This document provides theme and design token references for the Ava application.

## Shared Design System

Ava uses the MainAvanues OS universal design system and theme engine.

### Quick Start

```javascript
import ThemeEngine from '@shared/ui/theme-engine';
import VoiceCommandSystem from '@shared/voice/voice-commands';

// Initialize theme engine
const themeEngine = new ThemeEngine();
const voiceSystem = new VoiceCommandSystem(themeEngine);

// Set default theme for Ava
themeEngine.switchTheme('ocean');  // or 'slate', 'teal', etc.

// Apply Ava-specific settings
themeEngine.updateSettings({
    glassMode: true,
    glassBlur: 40,
    radiusMd: 12
});
```

### Available Themes

All 8 standard MainAvanues OS themes are available:

1. **Ocean Blue** (Recommended default)
2. **Slate Professional** (Corporate)
3. **Deep Teal** (Calming)
4. **Navy Corporate** (Classic)
5. **Charcoal** (High contrast)
6. **Warm Taupe** (Warm neutral)
7. **Emerald Green** (Creative)
8. **Purple Majesty** (Branding)

### Design Tokens

Use CSS custom properties from the shared design tokens:

```css
.ava-button {
    background: var(--primary);
    color: var(--on-surface);
    padding: var(--space-md);
    border-radius: var(--radius-sm);
    font-size: var(--text-base);
    box-shadow: var(--elevation-2);
    transition: all var(--duration-quick) var(--motion-standard);
}
```

### Voice Commands

Every Ava UI element must have voice commands:

```javascript
// Register Ava-specific voice commands
voiceSystem.registerElement(myButton, {
    primary: 'activate feature',
    alternatives: ['start', 'begin'],
    description: 'Activate the main feature',
    context: 'ava-main'
});
```

### Recommended Settings for Ava

```javascript
{
    glassMode: true,        // Enable glass effects
    glassBlur: 40,          // Standard blur
    radiusSm: 6,            // Button radius
    radiusMd: 10,           // Container radius
    radiusLg: 14,           // Panel radius
    shadowIntensity: 1.0,   // Full shadows
    motionSpeed: 1.0,       // Standard speed
    reducedMotion: false    // Full animations
}
```

## References

- **Design Tokens:** `docs/shared-libs/ui/DESIGN-TOKENS.md`
- **Theme Engine:** `docs/shared-libs/ui/theme-engine/`
- **Voice Commands:** `docs/shared-libs/voice/`
- **Theme Demos:** `docs/webavanue/demos/`

## Integration Examples

### React Component

```jsx
import { useTheme } from '@shared/ui/theme-engine';

function AvaComponent() {
    const { theme, updateSetting } = useTheme();

    return (
        <div className="ava-container" data-voice="open container">
            {/* Component content */}
        </div>
    );
}
```

### Voice Integration

```javascript
// Ava-specific voice commands
const avaCommands = {
    'open settings': () => openSettings(),
    'change theme': () => showThemePicker(),
    'toggle glass mode': () => themeEngine.toggleGlassMode()
};

Object.entries(avaCommands).forEach(([cmd, action]) => {
    voiceSystem.registerGlobalCommand(cmd, action);
});
```

## Performance Targets

- **FPS:** 60fps (solid mode), 55-60fps (glass mode)
- **GPU Usage:** Low-Medium
- **Battery Impact:** Low (solid), Medium (glass)
- **Memory:** < 5MB for theme system

## Accessibility

- ✅ WCAG 2.1 Level AAA
- ✅ Voice commands for all elements
- ✅ Keyboard navigation
- ✅ Screen reader support
- ✅ High contrast mode
- ✅ Reduced motion support

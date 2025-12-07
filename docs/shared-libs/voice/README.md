# Voice Command System

Universal voice-first accessibility system for all MainAvanues OS applications.

## Overview

The Voice Command System provides voice control for every UI element across all MainAvanues applications. Built on Web Speech API with graceful degradation.

## Features

- **Universal Voice Control** - Every element has voice commands
- **Natural Language** - Multiple alternatives per command
- **Visual Hints** - Floating badges show available commands
- **Text-to-Speech** - Audio feedback for actions
- **Screen Reader Compatible** - ARIA labels auto-generated
- **Graceful Degradation** - Works without voice support

## Quick Start

```javascript
import VoiceCommandSystem from '@shared/voice/voice-commands';
import ThemeEngine from '@shared/ui/theme-engine';

const themeEngine = new ThemeEngine();
const voiceSystem = new VoiceCommandSystem(themeEngine);

// Register element with voice command
voiceSystem.registerElement(myButton, {
    primary: 'click button',
    alternatives: ['press button', 'activate'],
    description: 'Main action button',
    action: () => myButton.click(),
    ariaLabel: 'Main action button'
});

// Show voice hints
voiceSystem.showVoiceHints();

// Start listening
voiceSystem.startListening();
```

## Command Registration

Every interactive element must be registered:

```javascript
voiceSystem.registerElement(element, {
    primary: 'main command',          // Primary voice phrase
    alternatives: ['alt1', 'alt2'],   // Alternative phrases
    description: 'Help text',         // Description for help
    action: () => { },                // Callback function
    context: 'navigation',            // Command context
    ariaLabel: 'Accessible label'     // Screen reader text
});
```

## Global Commands

Always available:

- **"show voice commands"** - Display all hints
- **"hide voice commands"** - Hide hints
- **"help"** - Show available commands
- **"start listening"** - Begin voice recognition
- **"stop listening"** - Stop voice recognition

## API Reference

### `registerElement(element, commandData)`

Register an element with voice commands.

### `showVoiceHints()`

Display visual hints for all registered commands.

### `hideVoiceHints()`

Hide voice command hints.

### `startListening()`

Start voice recognition (requires microphone permission).

### `stopListening()`

Stop voice recognition.

### `speak(text)`

Text-to-speech output.

## Browser Support

- ✅ Chrome/Edge - Full support
- ⚠️ Safari - Limited support
- ❌ Firefox - No Web Speech API
- Graceful degradation on unsupported browsers

## Implementation

Source: `voice-commands.js`

Full documentation: See inline code comments

## Integration Examples

See product-specific theme references:
- `docs/ava/THEME-REFERENCE.md`
- `docs/avaconnect/THEME-REFERENCE.md`
- `docs/avanues/THEME-REFERENCE.md`
- `docs/webavanue/demos/`

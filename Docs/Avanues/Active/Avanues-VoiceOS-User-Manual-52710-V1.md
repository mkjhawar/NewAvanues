# VoiceOS User Manual

**Version**: 3.1.0
**Date**: 2025-10-27
**Target Audience**: App creators, plugin developers, end users

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Creating Your First App](#creating-your-first-app)
3. [DSL Syntax Guide](#dsl-syntax-guide)
4. [Component Catalog](#component-catalog)
5. [Voice Commands](#voice-commands)
6. [Theme Customization](#theme-customization)
7. [App Lifecycle](#app-lifecycle)
8. [Troubleshooting](#troubleshooting)
9. [Examples Gallery](#examples-gallery)

---

## Getting Started

### What is VoiceOS?

VoiceOS is a voice-first platform that lets you create apps using simple text files with the `.vos` extension. No complex programming required!

### What You'll Need

1. **Text Editor**: Any text editor (VS Code, Sublime Text, even Notepad)
2. **VoiceOS Runtime**: Installed on your device
3. **Basic Understanding**: Familiarity with configuration files (like JSON or YAML)

### Your First .vos File

Create a file called `hello.vos`:

```
#!vos:D
App {
  id: "com.example.hello"
  name: "Hello World"

  Text {
    id: "greeting"
    text: "Hello, VoiceOS!"
    size: 28
    weight: "bold"
  }

  VoiceCommands {
    "hello" => "greeting.show"
  }
}
```

**What this does**:
- Creates an app called "Hello World"
- Shows text: "Hello, VoiceOS!"
- Responds to voice command "hello"

---

## Creating Your First App

### Step 1: Choose Your App Type

**Mode D** (DSL - Dynamic Apps):
- Best for: User-created apps, prototypes, learning
- Performance: Good
- Hot-reload: Yes
- File header: `#!vos:D`

**Mode K** (Codegen - Production Apps):
- Best for: Production apps, maximum performance
- Performance: Excellent
- Hot-reload: No (requires rebuild)
- File header: `#!vos:K`

**For beginners, start with Mode D!**

### Step 2: Create Your App File

Create `myapp.vos`:

```
#!vos:D
App {
  id: "com.yourname.myapp"
  name: "My First App"

  # Add components here
}
```

**Required fields**:
- `id`: Unique identifier (reverse domain format)
- `name`: Human-readable app name

### Step 3: Add Components

```
#!vos:D
App {
  id: "com.yourname.myapp"
  name: "My First App"

  Container {
    orientation: "vertical"

    Text {
      id: "title"
      text: "Welcome to My App"
      size: 24
      weight: "bold"
    }

    Button {
      id: "actionButton"
      text: "Click Me"
      enabled: true

      onClick: () => {
        VoiceOS.speak("Button clicked!")
      }
    }
  }
}
```

### Step 4: Add Voice Commands

```
#!vos:D
App {
  id: "com.yourname.myapp"
  name: "My First App"

  # ... components ...

  VoiceCommands {
    "press button" => "actionButton.click"
    "show title" => "title.show"
    "close app" => "App.finish"
  }
}
```

### Step 5: Run Your App

1. Save your `.vos` file
2. Open VoiceOS Runtime
3. Select "Load App" → Choose your file
4. Your app launches automatically!

---

## DSL Syntax Guide

### Comments

```
# This is a comment
// This is also a comment

/* Multi-line
   comment */
```

### Data Types

```
# String
text: "Hello World"
color: "#FF5722"

# Number (integer)
size: 24
count: 100

# Number (decimal)
opacity: 0.5
spacing: 16.5

# Boolean
enabled: true
visible: false

# Color
primaryColor: "#007AFF"
backgroundColor: "#FFFFFF"

# Array
items: ["Apple", "Banana", "Orange"]
numbers: [1, 2, 3, 4, 5]

# Object
metadata: {
  author: "Your Name"
  version: "1.0.0"
}
```

### Properties

```
ComponentName {
  propertyName: value
  anotherProperty: "another value"
  numericProperty: 123
}
```

### Callbacks (Event Handlers)

**No parameters**:
```
onClick: () => {
  VoiceOS.speak("Clicked!")
}
```

**With parameters**:
```
onConfirm: (color) => {
  Preferences.set("selectedColor", color)
  VoiceOS.speak("Color saved!")
}
```

**Multiple parameters**:
```
onChanged: (oldValue, newValue) => {
  VoiceOS.log("Changed from " + oldValue + " to " + newValue)
}
```

### Nested Components

```
Container {
  orientation: "vertical"

  Text {
    text: "Parent container"
  }

  Container {
    orientation: "horizontal"

    Button {
      text: "Button 1"
    }

    Button {
      text: "Button 2"
    }
  }
}
```

### Voice Commands

```
VoiceCommands {
  "trigger phrase" => "component.action"
  "another trigger" => "anotherComponent.action"
}
```

**Common triggers**:
- Single words: `"save"`, `"cancel"`, `"reset"`
- Phrases: `"save settings"`, `"go back"`, `"change color"`
- Questions: `"what time is it"`, `"what's my theme"`

---

## Component Catalog

### Text

Displays text on screen.

```
Text {
  id: "myText"
  text: "Hello World"
  size: 24              # Font size (default: 16)
  weight: "bold"        # "normal", "bold", "light"
  color: "#000000"      # Text color (default: black)
  fontFamily: "Roboto"  # Font name (optional)
}
```

**Properties**:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `text` | String | "" | Text to display |
| `size` | Number | 16 | Font size in points |
| `weight` | String | "normal" | Font weight: normal, bold, light |
| `color` | Color | "#000000" | Text color |
| `fontFamily` | String | System | Font family name |

---

### Button

Interactive button component.

```
Button {
  id: "myButton"
  text: "Click Me"
  enabled: true
  backgroundColor: "#007AFF"

  onClick: () => {
    VoiceOS.speak("Button clicked!")
  }
}
```

**Properties**:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `text` | String | "" | Button label |
| `enabled` | Boolean | true | Whether button is clickable |
| `backgroundColor` | Color | "#007AFF" | Button background color |

**Callbacks**:
- `onClick: () => { ... }` - Called when button is clicked

---

### Container

Layout container for organizing components.

```
Container {
  id: "myContainer"
  orientation: "vertical"  # or "horizontal"
  spacing: 16             # Space between children
  padding: 12             # Inner padding

  # Child components here
  Text { text: "Item 1" }
  Text { text: "Item 2" }
}
```

**Properties**:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `orientation` | String | "vertical" | Layout direction: vertical, horizontal |
| `spacing` | Number | 8 | Space between child components |
| `padding` | Number | 0 | Inner padding |

---

### ColorPicker

Advanced color selection component.

```
ColorPicker {
  id: "myPicker"
  initialColor: "#FF5722"
  mode: "DESIGNER"
  showAlpha: true
  showPalette: true

  onColorChanged: (color) => {
    # Called when color changes (real-time)
    Preferences.set("current.color", color)
  }

  onConfirm: (color) => {
    # Called when user confirms selection
    Preferences.set("theme.primary", color)
    VoiceOS.speak("Color saved!")
  }

  onCancel: () => {
    # Called when user cancels
    VoiceOS.speak("Cancelled")
  }
}
```

**Properties**:
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `initialColor` | Color | "#FFFFFF" | Starting color |
| `mode` | String | "FULL" | FULL, COMPACT, DESIGNER, PALETTE_ONLY, SLIDERS_ONLY, HEX_ONLY |
| `showAlpha` | Boolean | true | Show alpha/transparency slider |
| `showPalette` | Boolean | true | Show color palette |
| `allowCustomColors` | Boolean | true | Allow custom colors |

**Callbacks**:
- `onColorChanged: (color) => { ... }` - Real-time color updates
- `onConfirm: (color) => { ... }` - User confirms selection
- `onCancel: () => { ... }` - User cancels

**Color Modes**:

1. **FULL** (Default):
   - All controls (sliders, palette, hex input)
   - Best for: General-purpose color selection

2. **COMPACT**:
   - Essential controls only
   - Best for: Limited screen space

3. **DESIGNER**:
   - Professional tools (HSV/HSL, color schemes)
   - Best for: Design apps, theme creators

4. **PALETTE_ONLY**:
   - Pre-defined palette only
   - Best for: Simple color choices

5. **SLIDERS_ONLY**:
   - RGB/HSV/HSL sliders
   - Best for: Precise color adjustment

6. **HEX_ONLY**:
   - Hex input field only
   - Best for: Power users

---

### Preferences

Store and retrieve key-value data.

```
# Usage in callbacks
onSave: () => {
  Preferences.set("username", "John Doe")
  Preferences.set("theme", "dark")
  Preferences.set("fontSize", 18)
}

onLoad: () => {
  username = Preferences.get("username")
  theme = Preferences.get("theme")
  fontSize = Preferences.get("fontSize")
}
```

**API**:
- `Preferences.set(key, value)` - Save value
- `Preferences.get(key)` - Retrieve value (returns null if not found)
- `Preferences.remove(key)` - Delete value
- `Preferences.clear()` - Delete all values

---

## Voice Commands

### Basic Syntax

```
VoiceCommands {
  "trigger phrase" => "componentId.action"
}
```

**Examples**:
```
VoiceCommands {
  "save" => "saveButton.click"
  "cancel" => "cancelButton.click"
  "reset" => "App.reset"
  "go back" => "App.finish"
}
```

### Component Actions

**Button actions**:
```
"press save" => "saveButton.click"
```

**Text actions**:
```
"show title" => "titleText.show"
"hide title" => "titleText.hide"
```

**ColorPicker actions**:
```
"change color" => "picker.show"
"reset color" => "picker.reset"
```

**App actions**:
```
"close app" => "App.finish"
"minimize" => "App.minimize"
```

### Fuzzy Matching

VoiceOS uses fuzzy matching, so similar phrases will match:

**Exact**: `"change color"` → Matches `"change color"`
**Similar**: `"change colour"` → Matches `"change color"` (85% similar)
**Similar**: `"change the color"` → Matches `"change color"` (80% similar)
**Too Different**: `"open settings"` → Does NOT match `"change color"`

**Similarity threshold**: 70%

### Multiple Triggers for Same Action

```
VoiceCommands {
  "save" => "saveButton.click"
  "save settings" => "saveButton.click"
  "apply changes" => "saveButton.click"
}
```

---

## Theme Customization

### App-Level Theme

Define theme in your app:

```
App {
  id: "com.example.app"
  name: "My App"

  theme: {
    name: "My Custom Theme"
    palette: {
      primary: "#007AFF"
      secondary: "#5AC8FA"
      background: "#000000"
      surface: "#1C1C1E"
      error: "#FF3B30"
    }
    typography: {
      h1: {
        size: 28
        weight: "bold"
      }
      body1: {
        size: 16
        weight: "normal"
      }
    }
  }

  # ... components ...
}
```

### Using Theme Colors

```
Text {
  color: "@theme.palette.primary"  # Uses theme primary color
  size: "@theme.typography.h1.size"
}

Button {
  backgroundColor: "@theme.palette.secondary"
}
```

### Pre-defined Themes

VoiceOS includes built-in themes:

1. **Dark Theme** (default)
2. **Light Theme**
3. **High Contrast**
4. **Blue**
5. **Green**
6. **Purple**

**Load a pre-defined theme**:
```
App {
  theme: "Dark Theme"  # Just reference by name
}
```

### Custom Theme Files

Create separate theme files for reusability.

**mytheme.yaml**:
```yaml
name: "My Custom Theme"
palette:
  primary: "#007AFF"
  secondary: "#5AC8FA"
  background: "#000000"
  surface: "#1C1C1E"
typography:
  h1:
    size: 28
    weight: "bold"
  body1:
    size: 16
    weight: "normal"
```

**Load in your app**:
```
App {
  theme: "@file:mytheme.yaml"
}
```

---

## App Lifecycle

### Lifecycle States

Your app goes through these states:

1. **CREATED** - App just created (initial setup)
2. **STARTED** - App started (loading resources)
3. **RESUMED** - App is active (user interacting)
4. **PAUSED** - App in background (temporary)
5. **STOPPED** - App stopped (not visible)
6. **DESTROYED** - App destroyed (cleanup)

### Lifecycle Hooks

```
App {
  id: "com.example.app"
  name: "My App"

  onCreate: () => {
    # Called when app is created
    VoiceOS.log("App created")

    # Load saved state
    savedColor = Preferences.get("theme.primary")
    if (savedColor != null) {
      picker.setColor(savedColor)
    }
  }

  onStart: () => {
    # Called when app starts
    VoiceOS.log("App started")
  }

  onResume: () => {
    # Called when app becomes active
    VoiceOS.log("App resumed")
  }

  onPause: () => {
    # Called when app goes to background
    VoiceOS.log("App paused")

    # Save state
    Preferences.set("last.opened", Date.now())
  }

  onStop: () => {
    # Called when app stops
    VoiceOS.log("App stopped")
  }

  onDestroy: () => {
    # Called when app is destroyed
    VoiceOS.log("App destroyed")

    # Cleanup
    Preferences.set("app.closed", Date.now())
  }
}
```

### When Lifecycle Hooks Are Called

**User opens app**:
1. onCreate
2. onStart
3. onResume

**User switches to another app**:
1. onPause

**User returns to app**:
1. onResume

**User closes app**:
1. onPause
2. onStop
3. onDestroy

---

## Troubleshooting

### Common Errors

#### 1. "Failed to parse .vos file"

**Cause**: Syntax error in your file

**Solution**:
- Check for missing braces `{ }`
- Check for missing colons `:`
- Check for unclosed strings `"..."`
- Check for unmatched quotes

**Example error**:
```
Text {
  text: "Hello World  # Missing closing quote
}
```

**Fix**:
```
Text {
  text: "Hello World"  # Added closing quote
}
```

---

#### 2. "Unknown component: XYZ"

**Cause**: Component not registered or typo

**Solution**:
- Check component name spelling
- Verify component is available in your VoiceOS version
- Check component catalog (above)

**Example error**:
```
Buton {  # Typo: "Buton" instead of "Button"
  text: "Click Me"
}
```

**Fix**:
```
Button {  # Correct spelling
  text: "Click Me"
}
```

---

#### 3. "Property 'xyz' not found"

**Cause**: Invalid property name or typo

**Solution**:
- Check property name spelling
- Check component documentation for valid properties

**Example error**:
```
Text {
  txt: "Hello"  # Wrong property name
}
```

**Fix**:
```
Text {
  text: "Hello"  # Correct property name
}
```

---

#### 4. Voice command not working

**Cause**: Trigger phrase doesn't match or action is invalid

**Solution**:
- Check voice command syntax
- Ensure component ID exists
- Test with exact trigger phrase first

**Example error**:
```
VoiceCommands {
  "save" => "saveBtn.click"  # Component ID doesn't exist
}
```

**Fix**:
```
Button {
  id: "saveBtn"  # Add ID to component
  text: "Save"
}

VoiceCommands {
  "save" => "saveBtn.click"  # Now works
}
```

---

#### 5. "Callback execution failed"

**Cause**: Error in callback code

**Solution**:
- Check variable names
- Check function syntax
- Check API usage (Preferences.set, VoiceOS.speak, etc.)

**Example error**:
```
onClick: () => {
  Preference.set("key", "value")  # Typo: "Preference" not "Preferences"
}
```

**Fix**:
```
onClick: () => {
  Preferences.set("key", "value")  # Correct API name
}
```

---

### Debugging Tips

1. **Use VoiceOS.log()** for debugging:
   ```
   onClick: () => {
     VoiceOS.log("Button clicked")
     VoiceOS.log("Value: " + value)
   }
   ```

2. **Test components individually**:
   - Start with simple components
   - Add complexity gradually

3. **Check file encoding**:
   - Use UTF-8 encoding
   - Avoid special characters in IDs

4. **Validate syntax online**:
   - Use online YAML/JSON validators
   - Check brace matching

---

## Examples Gallery

### Example 1: Simple Note-Taking App

```
#!vos:D
App {
  id: "com.example.notes"
  name: "Quick Notes"

  Container {
    orientation: "vertical"
    padding: 16

    Text {
      id: "title"
      text: "Quick Notes"
      size: 24
      weight: "bold"
    }

    TextInput {
      id: "noteInput"
      placeholder: "Type your note..."
      multiline: true
    }

    Container {
      orientation: "horizontal"
      spacing: 8

      Button {
        id: "saveBtn"
        text: "Save"

        onClick: () => {
          note = noteInput.getText()
          Preferences.set("last.note", note)
          VoiceOS.speak("Note saved")
        }
      }

      Button {
        id: "clearBtn"
        text: "Clear"

        onClick: () => {
          noteInput.clear()
          VoiceOS.speak("Cleared")
        }
      }
    }
  }

  VoiceCommands {
    "save note" => "saveBtn.click"
    "clear note" => "clearBtn.click"
    "new note" => "clearBtn.click"
  }

  onCreate: () => {
    # Load last saved note
    savedNote = Preferences.get("last.note")
    if (savedNote != null) {
      noteInput.setText(savedNote)
    }
  }
}
```

---

### Example 2: Theme Creator

```
#!vos:D
App {
  id: "com.example.themecreator"
  name: "Theme Creator"

  Container {
    orientation: "vertical"
    padding: 16

    Text {
      id: "title"
      text: "Create Your Theme"
      size: 28
      weight: "bold"
    }

    Text {
      id: "subtitle"
      text: "Choose your primary color"
      size: 16
    }

    ColorPicker {
      id: "primaryPicker"
      initialColor: "#007AFF"
      mode: "DESIGNER"
      showAlpha: false

      onConfirm: (color) => {
        Preferences.set("theme.primary", color)
        VoiceOS.speak("Primary color saved")
        preview.setBackgroundColor(color)
      }
    }

    Container {
      id: "preview"
      backgroundColor: "#007AFF"
      padding: 24

      Text {
        text: "Preview"
        color: "#FFFFFF"
        size: 20
      }
    }

    Button {
      id: "resetBtn"
      text: "Reset to Default"

      onClick: () => {
        primaryPicker.setColor("#007AFF")
        Preferences.remove("theme.primary")
        VoiceOS.speak("Reset to default")
      }
    }
  }

  VoiceCommands {
    "change color" => "primaryPicker.show"
    "pick color" => "primaryPicker.show"
    "reset theme" => "resetBtn.click"
  }

  onCreate: () => {
    # Load saved theme
    savedColor = Preferences.get("theme.primary")
    if (savedColor != null) {
      primaryPicker.setColor(savedColor)
      preview.setBackgroundColor(savedColor)
    }
  }
}
```

---

### Example 3: Settings App

```
#!vos:D
App {
  id: "com.example.settings"
  name: "Settings"

  theme: {
    name: "Settings Theme"
    palette: {
      primary: "#007AFF"
      background: "#000000"
      surface: "#1C1C1E"
    }
  }

  Container {
    orientation: "vertical"
    padding: 16

    Text {
      id: "header"
      text: "Settings"
      size: 28
      weight: "bold"
      color: "@theme.palette.primary"
    }

    # Appearance Section
    Container {
      orientation: "vertical"
      spacing: 12
      backgroundColor: "@theme.palette.surface"
      padding: 12

      Text {
        text: "Appearance"
        size: 20
        weight: "bold"
      }

      Button {
        id: "themeBtn"
        text: "Change Theme Color"

        onClick: () => {
          themePicker.show()
        }
      }

      ColorPicker {
        id: "themePicker"
        mode: "PALETTE_ONLY"

        onConfirm: (color) => {
          Preferences.set("theme.primary", color)
          VoiceOS.speak("Theme updated")
        }
      }
    }

    # Voice Section
    Container {
      orientation: "vertical"
      spacing: 12
      backgroundColor: "@theme.palette.surface"
      padding: 12

      Text {
        text: "Voice"
        size: 20
        weight: "bold"
      }

      Toggle {
        id: "voiceToggle"
        label: "Enable Voice Commands"
        checked: true

        onChanged: (checked) => {
          Preferences.set("voice.enabled", checked)
          if (checked) {
            VoiceOS.speak("Voice commands enabled")
          }
        }
      }
    }

    # Reset Section
    Button {
      id: "resetBtn"
      text: "Reset All Settings"
      backgroundColor: "#FF3B30"

      onClick: () => {
        Preferences.clear()
        VoiceOS.speak("Settings reset")
      }
    }
  }

  VoiceCommands {
    "change theme" => "themeBtn.click"
    "reset settings" => "resetBtn.click"
    "enable voice" => "voiceToggle.check"
    "disable voice" => "voiceToggle.uncheck"
  }

  onCreate: () => {
    # Load saved settings
    voiceEnabled = Preferences.get("voice.enabled")
    if (voiceEnabled != null) {
      voiceToggle.setChecked(voiceEnabled)
    }

    themeColor = Preferences.get("theme.primary")
    if (themeColor != null) {
      themePicker.setColor(themeColor)
    }
  }
}
```

---

### Example 4: Timer App

```
#!vos:D
App {
  id: "com.example.timer"
  name: "Simple Timer"

  Container {
    orientation: "vertical"
    padding: 16

    Text {
      id: "timerDisplay"
      text: "00:00"
      size: 48
      weight: "bold"
      color: "#007AFF"
    }

    Container {
      orientation: "horizontal"
      spacing: 8

      Button {
        id: "startBtn"
        text: "Start"

        onClick: () => {
          Timer.start()
          VoiceOS.speak("Timer started")
        }
      }

      Button {
        id: "stopBtn"
        text: "Stop"

        onClick: () => {
          Timer.stop()
          VoiceOS.speak("Timer stopped")
        }
      }

      Button {
        id: "resetBtn"
        text: "Reset"

        onClick: () => {
          Timer.reset()
          timerDisplay.setText("00:00")
          VoiceOS.speak("Timer reset")
        }
      }
    }
  }

  VoiceCommands {
    "start timer" => "startBtn.click"
    "stop timer" => "stopBtn.click"
    "reset timer" => "resetBtn.click"
    "start" => "startBtn.click"
    "stop" => "stopBtn.click"
    "reset" => "resetBtn.click"
  }
}
```

---

## Quick Reference Card

### File Structure
```
#!vos:D                  # Mode (D=DSL, K=Codegen)
App {
  id: "com.example.app"
  name: "App Name"

  # Components
  # Voice Commands
  # Lifecycle Hooks
}
```

### Common Components
```
Text { text: "..." }
Button { text: "...", onClick: () => {...} }
Container { orientation: "vertical" }
ColorPicker { mode: "DESIGNER", onConfirm: (color) => {...} }
```

### Voice Commands
```
VoiceCommands {
  "trigger" => "component.action"
}
```

### Preferences API
```
Preferences.set(key, value)
Preferences.get(key)
Preferences.remove(key)
Preferences.clear()
```

### VoiceOS API
```
VoiceOS.speak(text)
VoiceOS.log(message)
VoiceOS.vibrate(duration)
VoiceOS.toast(message)
```

### Lifecycle Hooks
```
onCreate: () => {...}
onStart: () => {...}
onResume: () => {...}
onPause: () => {...}
onStop: () => {...}
onDestroy: () => {...}
```

---

## Getting Help

### Documentation
- **Developer Manual**: For platform developers and library authors
- **.vos File Spec**: Complete DSL syntax reference
- **Component Catalog**: Detailed component documentation

### Community
- **Forum**: https://community.voiceos.com
- **Discord**: https://discord.gg/voiceos
- **GitHub**: https://github.com/augmentalis/voiceos

### Support
- **Email**: support@voiceos.com
- **Issue Tracker**: https://github.com/augmentalis/voiceos/issues

---

## What's Next?

1. **Try the examples** - Copy and modify the example apps above
2. **Build your own app** - Start with something simple
3. **Explore components** - Try different components and modes
4. **Share your creation** - Submit to VoiceOS App Gallery
5. **Learn advanced features** - Check the Developer Manual for advanced topics

---

**Happy coding with VoiceOS!**

---

**Document Version**: 3.1.0
**Last Updated**: 2025-10-27
**Maintained by**: Manoj Jhawar, manoj@ideahq.net

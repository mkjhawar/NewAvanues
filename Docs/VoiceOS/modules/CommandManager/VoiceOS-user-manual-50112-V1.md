# CommandManager User Manual

**VoiceOS Command Reference**
**Last Updated:** 2025-12-01
**Version:** 2.0.0

---

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Command Categories](#command-categories)
4. [Navigation Commands](#navigation-commands)
5. [Text Editing Commands](#text-editing-commands)
6. [Cursor Commands](#cursor-commands)
7. [System Control Commands](#system-control-commands)
8. [Volume Commands](#volume-commands)
9. [Scrolling Commands](#scrolling-commands)
10. [Macro Commands](#macro-commands)
11. [Multi-Language Support](#multi-language-support)
12. [Tips & Tricks](#tips--tricks)
13. [Troubleshooting](#troubleshooting)

---

## Introduction

Welcome to VoiceOS! This manual shows you all the voice commands you can use to control your Android device hands-free.

### What is VoiceOS?

VoiceOS is a powerful voice control system for Android that lets you:
- Navigate your device without touching the screen
- Edit text using voice commands
- Control system settings
- Use pre-built command sequences (macros)
- Customize commands to your needs

### How Voice Commands Work

1. **Speak the command** - Say one of the supported phrases
2. **System processes** - VoiceOS recognizes and executes your command
3. **Get feedback** - See or hear confirmation of the action

### Confidence Levels

VoiceOS rates how confident it is that it heard you correctly:

| Confidence | What Happens |
|------------|--------------|
| **High** (85-100%) | Command executes immediately |
| **Medium** (70-84%) | Asks for confirmation |
| **Low** (50-69%) | Shows similar commands to choose from |
| **Too Low** (<50%) | Rejected - please try again |

---

## Getting Started

### Basic Usage

1. **Wake VoiceOS** - Say the wake word (if configured) or activate manually
2. **Speak your command** - Use natural language from the commands below
3. **Wait for confirmation** - System provides feedback
4. **Continue** - Speak another command or deactivate

### Voice Command Tips

✅ **DO:**
- Speak clearly and at normal pace
- Use the exact phrases listed (or close variations)
- Wait for previous command to complete
- Check confidence if command doesn't execute

❌ **DON'T:**
- Speak too fast or mumble
- Use completely different phrases
- Chain multiple unrelated commands
- Speak in noisy environments (affects recognition)

---

## Command Categories

Commands are organized into categories:

| Category | Description | Example Commands |
|----------|-------------|------------------|
| **Navigation** | System navigation and screens | "go back", "home", "recent apps" |
| **Editing** | Text manipulation | "copy", "paste", "select all" |
| **Cursor** | Virtual cursor control | "cursor up", "click", "show cursor" |
| **System** | Device settings and control | "WiFi on", "open settings", "battery status" |
| **Volume** | Audio control | "volume up", "mute", "unmute" |
| **Scrolling** | Page scrolling | "scroll down", "scroll up" |
| **Macros** | Multi-step sequences | "select all and copy" |

---

## Navigation Commands

Control your device navigation with these commands.

### Basic Navigation

| Say This | What Happens |
|----------|--------------|
| "go back" | Navigate to previous screen |
| "back" | Same as "go back" |
| "navigate back" | Same as "go back" |
| "previous" | Same as "go back" |
| "return" | Same as "go back" |

| Say This | What Happens |
|----------|--------------|
| "go home" | Go to home screen |
| "home" | Same as "go home" |
| "home screen" | Same as "go home" |
| "main screen" | Same as "go home" |

| Say This | What Happens |
|----------|--------------|
| "recent apps" | Open app switcher |
| "recents" | Same as "recent apps" |
| "app switcher" | Same as "recent apps" |

### Quick Access

| Say This | What Happens |
|----------|--------------|
| "notifications" | Open notification panel |
| "notification panel" | Same as "notifications" |
| "open notifications" | Same as "notifications" |

| Say This | What Happens |
|----------|--------------|
| "quick settings" | Open quick settings panel |

| Say This | What Happens |
|----------|--------------|
| "power dialog" | Open power menu |
| "power menu" | Same as "power dialog" |

### Advanced Navigation

| Say This | What Happens |
|----------|--------------|
| "split screen" | Toggle split screen mode |
| "lock screen" | Lock your device |
| "take screenshot" | Capture current screen |
| "screenshot" | Same as "take screenshot" |
| "all apps" | Open app drawer |
| "app drawer" | Same as "all apps" |
| "dismiss notifications" | Close notification panel |
| "accessibility settings" | Open accessibility settings |

**Example Usage:**
```
"go back"               → Returns to previous screen
"recent apps"           → Shows recently used apps
"take screenshot"       → Captures screen
"lock screen"           → Locks device
```

---

## Text Editing Commands

Edit text in any text field using these commands.

### Basic Editing

| Say This | What Happens | Requirements |
|----------|--------------|--------------|
| "copy" | Copy selected text | Text must be selected |
| "copy text" | Same as "copy" | Text must be selected |
| "copy selection" | Same as "copy" | Text must be selected |

| Say This | What Happens | Requirements |
|----------|--------------|--------------|
| "paste" | Paste clipboard content | Cursor in text field |
| "paste text" | Same as "paste" | Cursor in text field |
| "insert" | Same as "paste" | Cursor in text field |

| Say This | What Happens | Requirements |
|----------|--------------|--------------|
| "cut" | Cut selected text | Text must be selected |
| "cut text" | Same as "cut" | Text must be selected |
| "cut selection" | Same as "cut" | Text must be selected |

| Say This | What Happens | Requirements |
|----------|--------------|--------------|
| "select all" | Select all text in field | Cursor in text field |
| "select everything" | Same as "select all" | Cursor in text field |
| "highlight all" | Same as "select all" | Cursor in text field |

### Advanced Editing

| Say This | What Happens | Requirements |
|----------|--------------|--------------|
| "undo" | Undo last edit | Android 7.0+ (API 24+) |
| "redo" | Redo last undo | Android 7.0+ (API 24+) |

**Example Usage:**
```
"select all"            → Selects all text
"copy"                  → Copies to clipboard
"paste"                 → Pastes clipboard content
"undo"                  → Undoes last change
```

**Note:** Undo/Redo require Android 7.0 or higher and may not work in all apps.

---

## Cursor Commands

Control the virtual cursor for precise screen interaction.

### Cursor Movement

| Say This | What Happens |
|----------|--------------|
| "cursor up" | Move cursor up |
| "cursor down" | Move cursor down |
| "cursor left" | Move cursor left |
| "cursor right" | Move cursor right |
| "center cursor" | Move cursor to screen center |

**Distance:** Each movement is 50 pixels by default.

### Click Actions

| Say This | What Happens |
|----------|--------------|
| "click" | Single click at cursor position |
| "tap" | Same as "click" |
| "press" | Same as "click" |
| "select" | Same as "click" |

| Say This | What Happens |
|----------|--------------|
| "double click" | Double click at cursor position |
| "double tap" | Same as "double click" |
| "double press" | Same as "double click" |

| Say This | What Happens |
|----------|--------------|
| "long press" | Long press at cursor position |
| "long click" | Same as "long press" |
| "hold" | Same as "long press" |
| "press and hold" | Same as "long press" |

### Cursor Visibility

| Say This | What Happens |
|----------|--------------|
| "show cursor" | Display cursor on screen |
| "hide cursor" | Hide cursor from screen |
| "toggle cursor" | Show/hide cursor |

### Cursor Settings

| Say This | What Happens |
|----------|--------------|
| "show coordinates" | Display cursor position (x, y) |
| "hide coordinates" | Hide coordinate display |
| "toggle coordinates" | Show/hide coordinates |
| "cursor settings" | Open cursor settings screen |
| "cursor menu" | Show cursor context menu |

### Scrolling with Cursor

| Say This | What Happens |
|----------|--------------|
| "scroll up" | Scroll up at cursor position |
| "scroll down" | Scroll down at cursor position |

**Example Usage:**
```
"show cursor"           → Cursor appears on screen
"cursor right"          → Cursor moves right 50px
"cursor down"           → Cursor moves down 50px
"click"                 → Clicks at cursor position
"hide cursor"           → Cursor disappears
```

---

## System Control Commands

Control device settings and get system information.

### WiFi Control

| Say This | What Happens |
|----------|--------------|
| "WiFi on" | Enable WiFi |
| "enable WiFi" | Same as "WiFi on" |
| "turn on WiFi" | Same as "WiFi on" |

| Say This | What Happens |
|----------|--------------|
| "WiFi off" | Disable WiFi |
| "disable WiFi" | Same as "WiFi off" |
| "turn off WiFi" | Same as "WiFi off" |

| Say This | What Happens |
|----------|--------------|
| "toggle WiFi" | Toggle WiFi on/off |
| "WiFi on off" | Same as "toggle WiFi" |
| "switch WiFi" | Same as "toggle WiFi" |

**Note:** On Android 10+, these commands may open WiFi settings instead of directly toggling.

### Bluetooth Control

| Say This | What Happens |
|----------|--------------|
| "Bluetooth on" | Enable Bluetooth |
| "enable Bluetooth" | Same as "Bluetooth on" |
| "Bluetooth off" | Disable Bluetooth |
| "disable Bluetooth" | Same as "Bluetooth off" |
| "toggle Bluetooth" | Toggle Bluetooth on/off |

**Note:** On Android 13+, these commands open Bluetooth settings (direct toggle not allowed).

### Settings Access

**General Settings:**

| Say This | What Happens |
|----------|--------------|
| "open settings" | Open main settings |
| "settings" | Same as "open settings" |
| "system settings" | Same as "open settings" |

**Specific Settings Categories:**

| Say This | Opens Settings For |
|----------|-------------------|
| "open WiFi settings" | WiFi |
| "open Bluetooth settings" | Bluetooth |
| "open sound settings" | Sound & audio |
| "open display settings" | Display & brightness |
| "open battery settings" | Battery & power |
| "open storage settings" | Storage & files |
| "open app settings" | Applications |
| "open security settings" | Security |
| "open privacy settings" | Privacy |
| "open accessibility settings" | Accessibility |
| "open language settings" | Language & input |
| "open date settings" | Date & time |
| "open location settings" | Location |
| "open account settings" | Accounts |
| "open backup settings" | Backup & restore |
| "open developer settings" | Developer options |
| "open about settings" | About phone |
| "open network settings" | Network & internet |
| "open VPN settings" | VPN |

**Example:**
```
"open WiFi settings"    → Opens WiFi settings page
"open sound settings"   → Opens sound settings page
```

### Device Information

| Say This | What You Get |
|----------|--------------|
| "device info" | Model, manufacturer, Android version |
| "battery status" | Battery level and charging status |
| "network status" | Connection type (WiFi/Mobile/None) |
| "storage info" | Storage used/available |

**Example Output:**
```
"battery status"        → "Battery: 85% (charging)"
"network status"        → "Connected to WiFi"
"storage info"          → "Storage: 45GB used / 128GB total (35%), 83GB available"
```

---

## Volume Commands

Control audio volume for different streams.

### Basic Volume Control

| Say This | What Happens |
|----------|--------------|
| "volume up" | Increase media volume |
| "increase volume" | Same as "volume up" |
| "louder" | Same as "volume up" |
| "turn up" | Same as "volume up" |

| Say This | What Happens |
|----------|--------------|
| "volume down" | Decrease media volume |
| "decrease volume" | Same as "volume down" |
| "quieter" | Same as "volume down" |
| "turn down" | Same as "volume down" |

### Mute Control

| Say This | What Happens |
|----------|--------------|
| "mute" | Mute media audio |
| "silence" | Same as "mute" |
| "turn off sound" | Same as "mute" |
| "quiet" | Same as "mute" |

| Say This | What Happens |
|----------|--------------|
| "unmute" | Restore audio |
| "turn on sound" | Same as "unmute" |
| "restore audio" | Same as "unmute" |

**Audio Streams:**

By default, volume commands control **media** volume. Other streams:
- **Ring** - Ringtone volume
- **Notification** - Notification sounds
- **Alarm** - Alarm volume
- **System** - System sounds
- **Voice Call** - Phone call volume

**Example Usage:**
```
"volume up"             → Media volume increases
"volume down"           → Media volume decreases
"mute"                  → Media audio muted
"unmute"                → Media audio restored
```

---

## Scrolling Commands

Scroll pages and content.

| Say This | What Happens |
|----------|--------------|
| "scroll up" | Scroll page up |
| "move up" | Same as "scroll up" |
| "go up" | Same as "scroll up" |

| Say This | What Happens |
|----------|--------------|
| "scroll down" | Scroll page down |
| "move down" | Same as "scroll down" |
| "go down" | Same as "scroll down" |

| Say This | What Happens |
|----------|--------------|
| "scroll left" | Scroll page left |
| "move left" | Same as "scroll left" |
| "go left" | Same as "scroll left" |

| Say This | What Happens |
|----------|--------------|
| "scroll right" | Scroll page right |
| "move right" | Same as "scroll right" |
| "go right" | Same as "scroll right" |

**Example Usage:**
```
"scroll down"           → Page scrolls down
"scroll up"             → Page scrolls up
```

---

## Macro Commands

Macros are pre-built sequences of commands that execute multiple actions automatically.

### Available Macros

#### Editing Macros

**Select All and Copy**
- **Say:** "select all and copy"
- **What it does:**
  1. Selects all text in current field
  2. Copies to clipboard
- **Use when:** You want to copy all text quickly

**Select All and Cut**
- **Say:** "select all and cut"
- **What it does:**
  1. Selects all text in current field
  2. Cuts to clipboard (removes text)
- **Use when:** You want to move all text elsewhere

**Paste and Enter**
- **Say:** "paste and enter"
- **What it does:**
  1. Pastes clipboard content
  2. Presses Enter key
- **Use when:** Submitting a form with clipboard text

#### Productivity Macros

**Take Screenshot and Share**
- **Say:** "take screenshot and share"
- **What it does:**
  1. Captures screenshot
  2. Opens share dialog
- **Use when:** You want to share what's on screen

### How Macros Work

1. **Speak the macro name** - Use exact phrase above
2. **System executes steps** - Each step runs in sequence
3. **Wait between steps** - Short delay ensures completion
4. **Get confirmation** - Success message when all steps complete

**Example:**
```
User: "select all and copy"
Step 1: Text selected ✓
Step 2: Text copied ✓
System: "Macro completed: Select All and Copy"
```

### Macro Timing

- **Normal steps**: 200ms delay between steps
- **Long steps** (screenshot, app launch): 500ms delay

### When Macros Fail

If any step fails, the macro stops:
```
User: "select all and copy"
Step 1: Text selected ✓
Step 2: Copy failed ✗
System: "Macro failed at step 2: No text selected"
```

**Common issues:**
- No text field focused (for editing macros)
- Clipboard empty (for paste macros)
- App doesn't support action

---

## Multi-Language Support

VoiceOS supports multiple languages for voice commands.

### Supported Languages

| Language | Locale Code | Example Command |
|----------|-------------|-----------------|
| English (US) | `en-US` | "go back" |
| Spanish (Spain) | `es-ES` | "ir atrás" |
| French (France) | `fr-FR` | "retour" |
| German (Germany) | `de-DE` | "zurück" |

### Switching Languages

**To change language:**
1. Open VoiceOS settings
2. Go to Language settings
3. Select your preferred language
4. Commands reload in new language

**Or use voice command:**
```
"switch language to Spanish"
"cambiar idioma a inglés" (in Spanish)
```

### How It Works

- Commands load from locale-specific files
- Each language has full command set
- Automatic fallback to English if command not found
- System locale detected on first launch

**Example (Spanish):**
```
"ir atrás"              → Navigate back
"copiar"                → Copy text
"pegar"                 → Paste text
"inicio"                → Go home
```

---

## Tips & Tricks

### Getting Better Recognition

✅ **Speak naturally** - Don't over-enunciate or speak robotically
✅ **Use variations** - Multiple phrases work for same command
✅ **Check confidence** - Low confidence? Try rephrasing
✅ **Reduce noise** - Find quieter environment if commands fail
✅ **Practice** - Recognition improves as you use it

### Command Shortcuts

Some commands have short versions:

| Long Version | Short Version |
|--------------|---------------|
| "navigate back" | "back" |
| "go to home screen" | "home" |
| "take screenshot" | "screenshot" |
| "volume up" | "louder" |
| "volume down" | "quieter" |

### Combining Commands

Use macros instead of speaking multiple commands:

❌ **Slow way:**
```
"select all"
(wait)
"copy"
```

✅ **Fast way:**
```
"select all and copy"
```

### Checking Status

Get device info without touching screen:

```
"battery status"        → Check battery
"network status"        → Check connection
"storage info"          → Check storage
"device info"           → Get device details
```

### Accessibility Integration

VoiceOS works great with:
- TalkBack (screen reader)
- Switch Access
- Voice Access
- Other accessibility services

### Customization

Advanced users can:
- Create custom commands (developer mode)
- Adjust confidence thresholds
- Add new macros
- Configure voice feedback

---

## Troubleshooting

### Command Not Recognized

**Problem:** VoiceOS doesn't understand your command

**Solutions:**
1. Check you're using correct phrase (see command list)
2. Speak more clearly
3. Reduce background noise
4. Try alternative phrase for same command
5. Check language settings match your speech

**Example:**
```
❌ "return to previous screen"  (not in list)
✅ "go back"                     (correct phrase)
```

### Command Executed Wrong Action

**Problem:** Wrong action happens

**Solutions:**
1. Check for similar-sounding commands
2. Speak more distinctly
3. Increase microphone volume
4. Wait for previous command to complete
5. Check confidence level (may be too low)

### Low Confidence Warnings

**Problem:** Commands keep asking for confirmation

**Solutions:**
1. Improve pronunciation
2. Reduce background noise
3. Adjust microphone sensitivity
4. Use shorter, clearer phrases
5. Train voice model (if available)

### Commands Don't Work in Specific Apps

**Problem:** Commands work in some apps but not others

**Explanation:**
- Some apps block accessibility services
- App-specific commands need app support
- Text editing requires editable fields

**Solutions:**
1. Use system commands instead (work everywhere)
2. Check app permissions
3. Try different app if possible
4. Contact app developer

### Macro Steps Fail

**Problem:** Macro starts but doesn't complete

**Common causes:**
1. **No text field focused** - Editing macros need text field
2. **Clipboard empty** - Paste macros need clipboard content
3. **App doesn't support action** - Some apps block accessibility
4. **Too fast** - Steps executing before previous completes

**Solutions:**
1. Ensure requirements met (text selected, clipboard filled)
2. Try individual commands to test
3. Check macro is appropriate for current context

### WiFi/Bluetooth Commands Open Settings

**Problem:** "WiFi on" opens settings instead of toggling

**Explanation:**
- Android 10+ restricts direct WiFi control
- Android 13+ restricts direct Bluetooth control
- Security feature to prevent malicious apps

**Solution:**
- Manual toggle in opened settings page
- Use quick settings panel for faster access
- Feature limitation, not bug

### Undo/Redo Not Working

**Problem:** "undo" and "redo" don't work

**Requirements:**
- Android 7.0+ (API 24+)
- App must support undo/redo
- Text field must be editable

**Solution:**
- Check Android version
- Try in different app (some apps don't support)
- Use manual edit if undo unavailable

### Still Having Issues?

1. **Check logs** - Settings → Advanced → View Logs
2. **Restart VoiceOS** - Disable and re-enable service
3. **Update app** - Check for latest version
4. **Reset settings** - Settings → Reset to Defaults
5. **Contact support** - Report bug with details

---

## Quick Command Reference

### Most Common Commands

```
Navigation:
  "back"                → Go back
  "home"                → Go home
  "recent apps"         → App switcher

Editing:
  "select all"          → Select all text
  "copy"                → Copy to clipboard
  "paste"               → Paste from clipboard
  "select all and copy" → Select and copy (macro)

Cursor:
  "show cursor"         → Display cursor
  "cursor up/down/left/right" → Move cursor
  "click"               → Click at cursor

System:
  "WiFi on/off"         → Control WiFi
  "open [x] settings"   → Open settings page
  "battery status"      → Check battery

Volume:
  "volume up/down"      → Adjust volume
  "mute/unmute"         → Toggle mute

Scrolling:
  "scroll up/down"      → Scroll page
```

---

## Appendix: Complete Command List

### All Commands by Category

**Navigation (12):**
- go back, home, recent apps, notifications, quick settings
- power dialog, split screen, lock screen, screenshot
- all apps, dismiss notifications, accessibility settings

**Editing (6):**
- copy, paste, cut, select all, undo, redo

**Cursor (15):**
- cursor up/down/left/right, center cursor
- click, double click, long press
- show/hide/toggle cursor
- show/hide/toggle coordinates
- cursor settings, cursor menu
- scroll up/down (at cursor)

**System (20+):**
- WiFi on/off/toggle
- Bluetooth on/off/toggle
- open settings (+ 18 categories)
- device info, battery status, network status, storage info

**Volume (4):**
- volume up, volume down
- mute, unmute

**Scrolling (4):**
- scroll up/down/left/right

**Macros (4):**
- select all and copy
- select all and cut
- paste and enter
- take screenshot and share

**Total: 60+ commands** with variations = **200+ voice phrases**

---

**Need More Help?**

- Developer Manual: See `developer-manual.md` for technical details
- Changelog: See `changelog/CHANGELOG.md` for version history
- Support: Contact VOS4 development team

**Last Updated:** 2025-12-01
**Version:** 2.0.0

---

**Happy Voice Controlling!**

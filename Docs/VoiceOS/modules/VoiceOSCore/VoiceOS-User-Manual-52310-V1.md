# VoiceOS User Manual

**Module**: VoiceOSCore
**Version**: 1.0.0
**Created**: 2025-10-23 21:45:25 PDT
**Target Audience**: End users, accessibility users

---

## Table of Contents

1. [What is VoiceOS?](#what-is-voiceos)
2. [Setup and Permissions](#setup-and-permissions)
3. [Voice Commands Reference](#voice-commands-reference)
4. [Advanced Features](#advanced-features)
5. [Tips and Best Practices](#tips-and-best-practices)
6. [Troubleshooting](#troubleshooting)
7. [Accessibility Features](#accessibility-features)

---

## What is VoiceOS?

VoiceOS is an **Android accessibility service** that enables complete hands-free control of your Android device using voice commands. Whether you have mobility limitations or simply want convenient voice control, VoiceOS provides comprehensive access to all device functions.

### Key Features

✅ **Hands-Free Navigation**: Control your device entirely by voice
✅ **App Control**: Launch and switch between apps
✅ **Device Settings**: Adjust volume, brightness, connectivity
✅ **Text Input**: Dictate text and edit content
✅ **UI Interaction**: Tap buttons, scroll, and interact with screen elements
✅ **Gesture Control**: Swipe, pinch, zoom with voice commands
✅ **Number Overlay**: Show numbers on screen elements for precise selection
✅ **Voice Cursor**: Mouse-like cursor control for fine-grained interaction
✅ **Browser Integration**: Control web pages with specialized commands
✅ **Third-Party App Learning**: Automatically learns commands for your installed apps

### How It Works

```
Your Voice → Speech Recognition → VoiceOS → Action Execution
    "go home"         ↓              ↓           ↓
                Understands     Finds handler   Performs
                command         for action      home button press
```

---

## Setup and Permissions

### Initial Setup

1. **Install VoiceOS**
   - Install the VoiceOS app from your app store or sideload the APK
   - Open the VoiceOS app

2. **Enable Accessibility Service**
   - Go to: **Settings → Accessibility → VoiceOS**
   - Toggle **VoiceOS** to ON
   - Tap **Allow** on the permission dialog

3. **Grant Required Permissions**
   VoiceOS needs the following permissions to function:

   ✅ **Accessibility Service**: Control device and apps
   ✅ **Microphone**: Listen to voice commands
   ✅ **Display Over Other Apps**: Show cursor and overlays
   ✅ **Modify System Settings**: Adjust brightness and volume
   ✅ **Network Access**: For online speech recognition (if used)

4. **Choose Speech Engine**
   - VoiceOS supports multiple speech engines:
     - **Vivoka** (default): Offline, fast, accurate
     - **Google Speech**: Online, highly accurate
     - **Device Default**: Uses Android system speech recognition

5. **Test Voice Control**
   - Say: **"go home"**
   - Your device should return to the home screen
   - If it works, you're all set! 🎉

### Troubleshooting Setup

❌ **Commands not working?**
- Check that VoiceOS accessibility service is enabled
- Verify microphone permission is granted
- Ensure speech engine is initialized (check app status screen)

❌ **No microphone access when app in background?**
- This is normal on Android 12+
- VoiceOS shows a notification to maintain mic access
- You can hide notification in Android settings (but mic may stop working)

---

## Voice Commands Reference

### System Commands

Control your Android system and navigation.

| Command | Action |
|---------|--------|
| **"back"**, **"go back"** | Press back button |
| **"home"**, **"go home"** | Go to home screen |
| **"recent"**, **"recent apps"**, **"recents"** | Show recent apps |
| **"notifications"**, **"notification panel"** | Open notification panel |
| **"settings"**, **"quick settings"** | Open quick settings |
| **"power"**, **"power menu"** | Show power menu |
| **"screenshot"**, **"take screenshot"** | Take screenshot (Android 9+) |
| **"split screen"**, **"split"** | Enter split screen mode (Android 7+) |
| **"lock"**, **"lock screen"** | Lock the device (Android 9+) |
| **"all apps"**, **"app drawer"** | Open app drawer (Android 12+) |

**Examples**:
```
"go back"              → Navigates back
"take screenshot"      → Captures screen
"open notifications"   → Opens notification panel
```

---

### App Commands

Launch and switch between applications.

| Command Format | Action |
|----------------|--------|
| **"open [app name]"** | Launch the app |
| **"launch [app name]"** | Launch the app |
| **"[app name]"** | Launch the app (if unambiguous) |

**Supported Apps** (automatically detected from installed apps):
- "open chrome" → Chrome browser
- "open messages" → Messages app
- "open camera" → Camera
- "open settings" → Settings
- "open maps" → Google Maps
- "open spotify" → Spotify
- ... and any other installed app!

**Examples**:
```
"open chrome"       → Launches Chrome
"launch spotify"    → Opens Spotify
"youtube"           → Opens YouTube
```

**Note**: VoiceOS learns the names of all your installed apps automatically.

---

### Volume & Sound Commands

Control audio and sound settings.

| Command | Action |
|---------|--------|
| **"volume up"** | Increase volume |
| **"volume down"** | Decrease volume |
| **"volume mute"**, **"mute"** | Mute audio |
| **"volume unmute"**, **"unmute"** | Unmute audio |
| **"silent mode"**, **"silent"** | Set phone to silent |
| **"vibrate mode"**, **"vibrate"** | Set phone to vibrate |
| **"normal mode"**, **"sound on"** | Restore normal sound |

**Examples**:
```
"volume up"        → Increases media volume
"mute"             → Mutes all audio
"vibrate mode"     → Sets phone to vibrate
```

---

### Screen Brightness Commands

Adjust screen brightness.

| Command | Action |
|---------|--------|
| **"brightness up"** | Increase brightness (by ~10%) |
| **"brightness down"** | Decrease brightness (by ~10%) |
| **"brightness max"**, **"maximum brightness"** | Set brightness to 100% |
| **"brightness min"**, **"minimum brightness"** | Set brightness to minimum |

**Examples**:
```
"brightness up"    → Increases brightness
"brightness max"   → Sets to maximum
```

---

### Navigation & Scrolling Commands

Navigate and scroll through content.

| Command | Action |
|---------|--------|
| **"scroll up"**, **"page up"** | Scroll up |
| **"scroll down"**, **"page down"** | Scroll down |
| **"scroll left"** | Scroll left (horizontal) |
| **"scroll right"** | Scroll right (horizontal) |
| **"swipe up"** | Swipe up (content moves up) |
| **"swipe down"** | Swipe down (content moves down) |
| **"swipe left"** | Swipe left |
| **"swipe right"** | Swipe right |
| **"next"** | Move to next item |
| **"previous"** | Move to previous item |

**Examples**:
```
"scroll down"      → Scrolls page down
"swipe left"       → Swipes left (e.g., in photo gallery)
"next"             → Moves to next item
```

**Note**: "scroll down" and "swipe up" are different! Scroll down moves content down (reading forward), while swipe up is a gesture that moves content up the screen.

---

### Gesture Commands

Perform complex touch gestures.

| Command | Action |
|---------|--------|
| **"pinch open"**, **"zoom in"**, **"pinch in"** | Zoom in (two-finger spread) |
| **"pinch close"**, **"zoom out"**, **"pinch out"** | Zoom out (two-finger pinch) |

**Examples**:
```
"zoom in"          → Zooms in on map/image
"pinch close"      → Zooms out
```

**Advanced**: Developers can specify coordinates for pinch/zoom center point.

---

### Text Input & Editing Commands

Dictate and edit text.

| Command | Action |
|---------|--------|
| **"type [text]"**, **"enter text [text]"** | Type the text |
| **"delete"**, **"backspace"** | Delete last character |
| **"clear text"**, **"clear all"** | Clear all text |
| **"select all"** | Select all text |
| **"copy"** | Copy selected text |
| **"cut"** | Cut selected text |
| **"paste"** | Paste clipboard content |
| **"search [query]"**, **"find [query]"** | Search for query |

**Examples**:
```
"type hello world"     → Types "hello world"
"select all"           → Selects all text in field
"copy"                 → Copies selection
"paste"                → Pastes clipboard
"search restaurants"   → Finds search field and searches
```

**Tips**:
- First tap the text field to focus it, then use "type" command
- Use punctuation naturally: "type hello comma how are you question mark"
- VoiceOS will find and focus search fields automatically

---

### UI Interaction Commands

Interact with buttons and screen elements by name.

| Command Format | Action |
|----------------|--------|
| **"click [element]"**, **"tap [element]"** | Tap the element |
| **"long click [element]"**, **"long press [element]"** | Long press element |
| **"double tap [element]"** | Double tap element |
| **"expand [element]"** | Expand expandable item |
| **"collapse [element]"** | Collapse expanded item |
| **"check [element]"** | Check checkbox |
| **"uncheck [element]"** | Uncheck checkbox |
| **"toggle [element]"** | Toggle switch/checkbox |
| **"dismiss"**, **"close"** | Close dialog/screen |

**Examples**:
```
"click submit button"      → Clicks "Submit" button
"tap send"                 → Taps "Send"
"long press settings"      → Long presses "Settings"
"check remember me"        → Checks "Remember me" checkbox
"expand notifications"     → Expands notification section
"dismiss"                  → Closes dialog
```

**How it works**: VoiceOS searches for buttons/elements with text matching your command.

---

### Number Overlay Commands

Show numbers on interactive elements for precise selection.

| Command | Action |
|---------|--------|
| **"show numbers"**, **"numbers on"**, **"label elements"** | Show numbered overlay |
| **"hide numbers"**, **"numbers off"** | Hide numbered overlay |
| **"toggle numbers"** | Toggle overlay on/off |
| **"tap [number]"**, **"click [number]"**, **"select [number]"** | Click numbered element |

**How to use**:
1. Say **"show numbers"**
2. Numbers appear on all clickable elements (1, 2, 3, ...)
3. Say **"tap 5"** to click element #5
4. Overlay auto-hides after 30 seconds or after clicking

**Examples**:
```
"show numbers"     → Shows numbers on screen
"tap 7"            → Clicks element labeled "7"
"hide numbers"     → Hides overlay
```

**Tips**:
- Great for complex screens with many buttons
- Numbers sorted top-to-bottom, left-to-right
- Auto-refreshes when screen changes

---

### Selection & Context Menu Commands

Advanced text selection and context menus.

| Command | Action |
|---------|--------|
| **"select mode"**, **"selection mode"** | Enter selection mode |
| **"select"** | Context-aware selection |
| **"select all"** | Select all text in field |
| **"clear selection"** | Clear selection |
| **"menu"**, **"context menu"** | Show context menu |
| **"copy"** | Copy selected text |
| **"cut"** | Cut selected text |
| **"paste"** | Paste at selection |

**Selection Mode Workflow**:
1. Say **"selection mode"** to enter selection mode
2. Use cursor or other commands to select
3. Say **"copy"** or **"cut"**
4. Say **"back"** or **"cancel selection"** to exit

**Examples**:
```
"selection mode"   → Enter selection mode
"select all"       → Select all text
"copy"             → Copy selection
```

---

### Voice Cursor Commands

Mouse-like cursor for precise control.

**Note**: Voice cursor commands are provided by the VoiceCursor module. See VoiceCursor documentation for complete command list.

**Basic Commands**:
```
"show cursor"      → Display cursor
"hide cursor"      → Hide cursor
"cursor click"     → Click at cursor position
"center cursor"    → Move cursor to screen center
```

See [VoiceCursor User Manual](../VoiceCursor/user-manual.md) for movement, speed, and advanced features.

---

### Connectivity Commands

Control WiFi, Bluetooth, and airplane mode.

| Command | Action |
|---------|--------|
| **"wifi on"**, **"turn on wifi"** | Open WiFi settings |
| **"wifi off"**, **"turn off wifi"** | Open WiFi settings |
| **"bluetooth on"**, **"turn on bluetooth"** | Open Bluetooth settings |
| **"bluetooth off"**, **"turn off bluetooth"** | Open Bluetooth settings |
| **"airplane mode on"**, **"flight mode on"** | Open airplane mode settings |
| **"airplane mode off"**, **"flight mode off"** | Open airplane mode settings |

**Examples**:
```
"wifi on"              → Opens WiFi settings
"turn off bluetooth"   → Opens Bluetooth settings
"airplane mode on"     → Opens airplane mode settings
```

**Note**: Due to Android security restrictions, VoiceOS opens settings screens where you can toggle these manually. On some devices, VoiceOS may be able to toggle directly.

---

### Do Not Disturb Commands

| Command | Action |
|---------|--------|
| **"do not disturb on"**, **"dnd on"** | Open DND settings |
| **"do not disturb off"**, **"dnd off"** | Open DND settings |

**Note**: Opens Do Not Disturb settings where you can configure or toggle.

---

### Settings Commands

Open specific settings screens.

| Command Format | Settings Screen |
|----------------|-----------------|
| **"open settings wifi"** | WiFi settings |
| **"open settings bluetooth"** | Bluetooth settings |
| **"open settings accessibility"** | Accessibility settings |
| **"open settings display"** | Display settings |
| **"open settings sound"** | Sound settings |
| **"open settings battery"** | Battery settings |
| **"open settings storage"** | Storage settings |
| **"open settings security"** | Security settings |
| **"open settings location"** | Location settings |
| **"open settings developer"** | Developer options |

**Examples**:
```
"open settings wifi"        → Opens WiFi settings
"open settings display"     → Opens display settings
```

---

## Advanced Features

### 1. Number Overlay

The number overlay system is one of VoiceOS's most powerful features for accessibility.

**When to use**:
- Complex screens with many buttons
- Apps you haven't learned commands for yet
- Precise element selection

**Workflow**:
```
1. Open any app
2. Say "show numbers"
3. Numbers appear on all clickable elements
4. Say "tap [number]" to click
5. Overlay auto-hides after action
```

**Example Scenario** - Navigating a settings menu:
```
User: "show numbers"
VoiceOS: Shows numbers 1-20 on menu items
User: "tap 7"
VoiceOS: Clicks item #7 (Display settings)
```

**Tips**:
- Numbers are sorted top-to-bottom, left-to-right
- Overlay refreshes automatically when screen changes
- Say "hide numbers" to manually dismiss
- Works in ALL apps, including third-party apps

---

### 2. Voice Cursor

For mouse-like precision control.

**When to use**:
- Drawing or precise positioning needed
- Drag-and-drop operations
- Apps with small tap targets

**Basic workflow**:
```
1. Say "show cursor"
2. Use movement commands (see VoiceCursor manual)
3. Say "cursor click" to tap
4. Say "hide cursor" when done
```

See [VoiceCursor documentation](../VoiceCursor/) for complete guide.

---

### 3. Third-Party App Learning

VoiceOS can automatically learn commands for third-party apps.

**How it works**:
1. You launch a new third-party app
2. VoiceOS detects it's not learned yet
3. Shows consent dialog: "Learn commands for [App Name]?"
4. If you accept, VoiceOS explores the app's interface
5. Generates voice commands for buttons and elements
6. You can now control the app by voice!

**Example** - Learning Instagram:
```
1. User opens Instagram (first time)
2. VoiceOS: "Learn commands for Instagram?"
3. User: Tap "Allow"
4. VoiceOS: Explores Instagram UI (shows progress)
5. VoiceOS: "Learning complete! Try: 'tap home', 'tap search', 'tap profile'"
6. User: "tap search"
7. VoiceOS: Opens Instagram search
```

**Privacy Note**: Learning happens locally on your device. No data is sent to servers.

---

### 4. Browser Integration

Special commands for web browsers (Chrome, Firefox, Edge, etc.).

**Web-Specific Commands**:
```
"click [link text]"        → Clicks link on page
"open [website]"           → Navigates to website
"back"                     → Browser back
"forward"                  → Browser forward
"refresh"                  → Reload page
"new tab"                  → Open new tab
"close tab"                → Close current tab
"scroll to [heading]"      → Scroll to section
```

**Example Workflow**:
```
User: "open chrome"
User: "open wikipedia"
User: "search history of computers"
User: "click early computers"
User: "scroll down"
User: "select all"
User: "copy"
```

**Note**: Web commands are learned from visited websites and improve over time.

---

### 5. Multi-Language Support

VoiceOS supports multiple languages through Android's speech recognition.

**Supported Languages** (depends on your speech engine):
- English (US, UK, Australia, India, etc.)
- Spanish
- French
- German
- Italian
- Portuguese
- Chinese (Mandarin, Cantonese)
- Japanese
- Korean
- And many more...

**To change language**:
1. Go to: **Settings → System → Languages & Input**
2. Change system language OR speech input language
3. VoiceOS will automatically use the new language

**Note**: Command syntax may vary by language. VoiceOS adapts to natural language in your chosen language.

---

### 6. Offline Mode

VoiceOS can work completely offline with Vivoka speech engine.

**Benefits**:
- Works without internet
- Faster response time
- Better privacy
- Lower data usage

**Setup**:
1. In VoiceOS app, go to Settings
2. Select **Speech Engine → Vivoka**
3. Download language models (if prompted)
4. Enable "Offline Mode"

**Limitations**:
- Slightly lower accuracy than online recognition
- Limited to pre-trained vocabulary
- Learned commands may take longer to register

---

## Tips and Best Practices

### Speaking Tips

✅ **Speak clearly** at normal pace
✅ **Use natural language** - VoiceOS understands variations
✅ **Pause briefly** between commands
✅ **Say command completely** before expecting action
✅ **Check visual feedback** - VoiceOS shows listening status

❌ **Don't speak too fast** - let speech recognition process
❌ **Don't shout** - normal volume works best
❌ **Don't interrupt yourself** - finish one command before starting another

**Examples**:
```
Good: "open... chrome"              (brief pause, clear)
Good: "volume... up"
Bad:  "opench ome"                 (too fast, slurred)
Bad:  "vol-vol-volume up"          (interrupted)
```

---

### Command Variations

VoiceOS understands many variations of commands:

**Navigation**:
- "back", "go back", "navigate back" ✅ All work!
- "home", "go home", "home screen" ✅
- "recent apps", "recents", "recent" ✅

**Actions**:
- "click submit", "tap submit", "press submit" ✅
- "volume up", "increase volume", "turn up volume" ✅
- "brightness down", "decrease brightness", "lower brightness" ✅

**Tip**: If one phrasing doesn't work, try rephrasing naturally.

---

### Efficiency Tips

**1. Learn App Names**
VoiceOS learns your app names - use them:
```
Instead of: "show numbers" → "tap 3"
Just say:   "open messages"
```

**2. Chain Simple Commands**
For multi-step tasks:
```
"open chrome"
(wait for Chrome to open)
"type reddit.com"
(wait for typing)
"scroll down"
```

**3. Use Number Overlay for Complex Screens**
If an app has many similar buttons:
```
"show numbers"  → See which button is which
"tap 5"         → Click the one you want
```

**4. Bookmark Common Actions**
Create shortcuts for frequent tasks:
- "open settings wifi" instead of navigating manually
- "brightness max" before watching video
- "silent mode" before meetings

---

### Battery Optimization

VoiceOS is designed to be battery-efficient, but here are tips:

✅ **Use Vivoka (offline)** - no network = better battery
✅ **Disable when not needed** - turn off accessibility service when not using
✅ **Allow VoiceOS to sleep** - disable "Ignore battery optimization" unless needed
✅ **Reduce overlay time** - number overlay auto-hides to save battery

**Foreground Service** (Android 12+):
When app is in background, VoiceOS shows a notification to maintain mic access. This is required by Android for privacy. You can:
- Hide the notification in Android settings (but mic may stop working)
- Keep notification visible for reliable background voice control

---

## Troubleshooting

### Commands Not Recognized

**Symptoms**: VoiceOS doesn't respond to commands

**Solutions**:
1. ✅ **Check Accessibility Service**
   - Go to: Settings → Accessibility → VoiceOS
   - Ensure toggle is ON

2. ✅ **Check Microphone Permission**
   - Go to: Settings → Apps → VoiceOS → Permissions
   - Ensure Microphone is allowed

3. ✅ **Check Speech Engine Status**
   - Open VoiceOS app
   - Check if speech engine is initialized
   - Try switching speech engines (Settings → Speech Engine)

4. ✅ **Test with Simple Command**
   - Say: "go home"
   - If this works, issue is with specific command
   - If this doesn't work, check above settings

5. ✅ **Restart VoiceOS Service**
   - Settings → Accessibility → VoiceOS → Toggle OFF → Toggle ON

---

### Commands Work Inconsistently

**Symptoms**: Some commands work, others don't

**Solutions**:
1. **Check Command Phrasing**
   - Try variations: "volume up" vs "increase volume"
   - Check this manual for correct phrasing

2. **Check App Context**
   - Some commands only work in specific apps
   - Example: "type" only works when text field is focused

3. **Update Vocabulary**
   - If you recently installed apps, restart VoiceOS
   - This refreshes the app list

4. **Check Confidence Threshold**
   - VoiceOS rejects commands with confidence < 50%
   - Speak more clearly if commands are rejected

---

### App-Specific Commands Not Working

**Symptoms**: Can't control specific third-party app

**Solutions**:
1. **Use Number Overlay**
   - Say: "show numbers"
   - This works in ALL apps

2. **Wait for Learning**
   - If you recently launched the app, learning may still be in progress
   - Check for consent dialog

3. **Re-Learn App**
   - Go to VoiceOS settings
   - Find "Reset learned apps"
   - Re-launch the app to trigger learning again

4. **Use Generic Commands**
   - Instead of app-specific commands, use: "click [button text]"
   - Example: "click send button"

---

### Voice Cursor Not Appearing

**Symptoms**: "show cursor" doesn't show cursor

**Solutions**:
1. **Check Display Over Other Apps Permission**
   - Go to: Settings → Apps → VoiceOS → Advanced → Display over other apps
   - Enable permission

2. **Check VoiceCursor Module**
   - VoiceCursor must be installed and initialized
   - Check VoiceOS status screen

3. **Restart Service**
   - Toggle VoiceOS accessibility service off and on

---

### Battery Drain

**Symptoms**: VoiceOS uses too much battery

**Solutions**:
1. **Switch to Offline Mode (Vivoka)**
   - Reduces network usage
   - Processes locally = less battery

2. **Disable When Not Needed**
   - Turn off accessibility service when not using voice control

3. **Reduce Overlay Usage**
   - Number overlay uses screen rendering
   - Hide when not needed: "hide numbers"

4. **Check Background Apps**
   - VoiceOS notification on Android 12+ is normal
   - If you hide notification, mic may stop working (saves battery but disables voice)

---

### Speech Recognition Slow/Laggy

**Symptoms**: Delay between speaking and action

**Solutions**:
1. **Switch to Offline Mode**
   - Online recognition has network latency
   - Vivoka offline is faster

2. **Check Network Connection**
   - If using online recognition, check WiFi/cellular
   - Slow network = slow recognition

3. **Reduce Background Apps**
   - Close unused apps
   - Free up device memory

4. **Update VoiceOS**
   - Check for updates in app store
   - Performance improvements in newer versions

---

### Privacy Concerns

**Question**: "Is VoiceOS listening all the time?"

**Answer**:
- ✅ VoiceOS only listens when accessibility service is active
- ✅ Speech recognition happens on-device (with Vivoka offline mode)
- ✅ No voice data is sent to servers (in offline mode)
- ✅ You can see listening status in notification

**Question**: "What permissions does VoiceOS need?"

**Answer**:
- **Accessibility Service**: To control device and interact with apps
- **Microphone**: To hear voice commands
- **Display Over Other Apps**: To show cursor and overlays
- **Modify System Settings**: To adjust brightness/volume
- **Internet** (optional): For online speech recognition

**Question**: "Does app learning send data to servers?"

**Answer**:
- ❌ No! All learning happens locally on your device
- ✅ Generated commands are stored in local database
- ✅ No UI data leaves your device

---

## Accessibility Features

VoiceOS is designed with accessibility in mind.

### For Mobility Impairments

✅ **Complete hands-free control**
- Navigate entire device without touching
- Launch apps, adjust settings, type text

✅ **Voice cursor**
- Mouse-like control for precise interaction
- No need for physical mouse

✅ **Number overlay**
- Easy selection of screen elements
- No need to remember exact names

### For Visual Impairments

✅ **Screen reader compatible**
- Works alongside TalkBack/Android screen reader
- Voice commands complement audio feedback

✅ **Large targets**
- Number overlay shows large numbered buttons
- Easy to select without seeing fine details

### For Hearing Impairments

✅ **Visual feedback**
- VoiceOS shows visual status indicators
- Notifications for command recognition

### For Cognitive Disabilities

✅ **Natural language**
- No need to memorize exact syntax
- Multiple phrasings work ("back", "go back", "navigate back")

✅ **Simplified interaction**
- Number overlay reduces cognitive load
- Clear, predictable command structure

---

## Quick Reference Card

### Most Common Commands

| Task | Command |
|------|---------|
| Go back | "back" |
| Go home | "home" |
| Open app | "open [app name]" |
| Adjust volume | "volume up/down" |
| Adjust brightness | "brightness up/down" |
| Scroll | "scroll down/up" |
| Type text | "type [text]" |
| Show numbers | "show numbers" |
| Click element | "tap [number]" or "click [element name]" |
| Take screenshot | "screenshot" |
| Open settings | "settings" |
| Show cursor | "show cursor" |
| Select all text | "select all" |
| Copy text | "copy" |
| Paste text | "paste" |

---

## Getting Help

### In-App Help

- Open VoiceOS app
- Tap "Help" or "Commands"
- Browse full command list

### Voice Help

Say: **"show help"** or **"what can I say"** to see available commands

### Community Support

- Visit VoiceOS community forums
- Check FAQ: [link]
- Report issues: [link]

### Contact Support

- Email: support@voiceos.example.com
- Website: www.voiceos.example.com
- GitHub: github.com/voiceos

---

## Appendix: Complete Command List

### System (15 commands)
```
back, go back, home, go home, recent, recent apps, recents,
notifications, notification panel, settings, quick settings,
power, power menu, screenshot, take screenshot, split screen,
split, lock, lock screen, all apps, app drawer
```

### Volume (7 commands)
```
volume up, volume down, volume mute, mute, volume unmute,
unmute, silent mode, vibrate mode, normal mode
```

### Brightness (6 commands)
```
brightness up, brightness down, brightness max, brightness min,
maximum brightness, minimum brightness
```

### Navigation (14 commands)
```
scroll up, scroll down, scroll left, scroll right, swipe up,
swipe down, swipe left, swipe right, page up, page down,
next, previous
```

### Gestures (6 commands)
```
pinch open, pinch close, zoom in, zoom out, pinch in, pinch out
```

### Text Input (12 commands)
```
type [text], enter text [text], delete, backspace, clear text,
select all, copy, cut, paste, search [query], find [query]
```

### UI Interaction (13 commands)
```
click [element], tap [element], long click [element],
long press [element], double tap [element], expand [element],
collapse [element], check [element], uncheck [element],
toggle [element], dismiss, close
```

### Number Overlay (9 commands)
```
show numbers, hide numbers, numbers on, numbers off,
toggle numbers, label elements, tap [number], click [number],
select [number]
```

### Selection (8 commands)
```
select, select mode, selection mode, select all, clear selection,
menu, context menu, back
```

### Connectivity (6 commands)
```
wifi on, wifi off, bluetooth on, bluetooth off,
airplane mode on, airplane mode off
```

### Total: 96+ base commands + app-specific commands + learned commands

---

## Updates and Changelog

**Version 1.0.0** (2025-10-23)
- Initial VoiceOS release
- Core accessibility service
- Handler architecture (10 handlers)
- Speech recognition integration
- Number overlay system
- Voice cursor integration
- Third-party app learning
- Browser integration
- Offline mode support

---

## Legal and Privacy

### Privacy Policy

VoiceOS respects your privacy:
- Voice data processed locally (in offline mode)
- No data sent to servers without consent
- App learning data stored locally only
- You can delete all learned data anytime

### Open Source

VoiceOS is open source under [license]:
- Source code: github.com/voiceos
- Contribute: [contribution guide]
- Report bugs: [issue tracker]

### Accessibility Statement

VoiceOS is committed to accessibility:
- WCAG 2.1 Level AA compliant
- Tested with screen readers
- Follows Android accessibility guidelines
- Regular accessibility audits

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-23 21:45:25 PDT
**Commands Cataloged**: 96+ base commands
**Languages Supported**: 50+ (via Android Speech Recognition)

---

## Quick Start Checklist

- [ ] VoiceOS app installed
- [ ] Accessibility service enabled
- [ ] Microphone permission granted
- [ ] Display over other apps permission granted
- [ ] Speech engine initialized
- [ ] Test command: "go home" works
- [ ] Ready to use! 🎉

**First commands to try**:
1. "go home"
2. "open chrome" (or any app)
3. "volume up"
4. "show numbers"
5. "tap 3" (after showing numbers)

Welcome to hands-free Android! 🚀

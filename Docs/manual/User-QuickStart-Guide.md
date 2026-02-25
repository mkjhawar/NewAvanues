# VoiceOS Quick Start Guide

Welcome to VoiceOS! This guide will help you get started controlling your Android device using voice commands.

---

## What is VoiceOS?

VoiceOS is a voice-first control layer that lets you interact with any app on your Android device by speaking natural commands. Instead of tapping buttons, you can say what you see and VoiceOS will click it for you.

**Key idea:** Say the label of what you want to click. VoiceOS finds it and taps it automatically.

---

## Getting Started

### Step 1: Install the Avanues App
1. Download and install the Avanues app from the Google Play Store
2. Open the Avanues app

### Step 2: Enable VoiceOS in Accessibility
1. Open **Android Settings**
2. Go to **Accessibility** (usually under Settings > Accessibility)
3. Look for **VoiceOS** or **Avanues Voice Control**
4. Toggle it **ON**
5. VoiceOS will ask permission to monitor your device

### Step 3: Grant Required Permissions
VoiceOS needs three permissions to work:
- **Microphone** — to listen to your voice
- **Overlay** — to display numbered badges on screen elements
- **Accessibility Service** — to click elements automatically

When prompted, select **Allow** for each permission.

### Step 4: You're Ready!
Once enabled, you'll see numbered circles (badges) appear on buttons, links, and other interactive elements. The app is now listening for your voice commands.

---

## How Badges Work

Badges are small numbered circles that appear on clickable elements.

```
Example: A button labeled "Send" might show badge [3]
```

**To click it, say:** "click 3" or "click send"

Badges update automatically when the screen changes. Scroll down to see more badges — they refresh after scrolling.

---

## Basic Voice Commands

### Say What You See
| Command | What It Does |
|---------|-------------|
| "click button name" | Clicks the button by its label |
| "click 3" | Clicks element number 3 (the badge number) |
| "scroll up" | Scrolls up on the current screen |
| "scroll down" | Scrolls down on the current screen |
| "go back" | Goes to the previous screen |
| "go home" | Returns to the home screen |

### Opening and Closing Apps
| Command | What It Does |
|---------|-------------|
| "open gmail" | Opens the Gmail app |
| "open maps" | Opens Google Maps |
| "close app" | Closes or backgrounds the current app |
| "switch to chrome" | Switches to another open app |

---

## Text Editing Commands

When you're typing in a text field:

| Command | What It Does |
|---------|-------------|
| "type hello world" | Types the text you speak |
| "select all" | Selects all text in the field |
| "copy" | Copies selected text |
| "paste" | Pastes text from clipboard |
| "cut" | Cuts selected text |
| "delete" | Deletes selected text |
| "backspace" | Deletes one character |

---

## Media and Volume Commands

Control music, videos, and audio:

| Command | What It Does |
|---------|-------------|
| "play" | Starts media playback |
| "pause" | Pauses playback |
| "play pause" | Toggles between play and pause |
| "next" | Plays the next track |
| "previous" | Plays the previous track |
| "volume up" | Increases volume |
| "volume down" | Decreases volume |
| "mute" | Mutes audio |
| "unmute" | Unmutes audio |

---

## Screen Control Commands

Adjust your device display:

| Command | What It Does |
|---------|-------------|
| "take screenshot" | Captures a screenshot |
| "brightness up" | Increases screen brightness |
| "brightness down" | Decreases screen brightness |
| "rotate" | Rotates screen orientation |
| "rotate left" / "rotate right" | Rotates in specific direction |

---

## Text-to-Speech and Reading

VoiceOS can read content aloud:

| Command | What It Does |
|---------|-------------|
| "read screen" | Reads all visible text on the screen |
| "read paragraph" | Reads the current paragraph |
| "stop reading" | Stops text-to-speech |

---

## Voice Control Commands

Manage VoiceOS itself:

| Command | What It Does |
|---------|-------------|
| "start listening" | Activates voice recognition |
| "stop listening" | Deactivates voice recognition (microphone pauses) |
| "what can I say?" | Shows a list of available commands |
| "help" | Displays all commands on this screen as numbered badges |

---

## Web Browsing with WebAvanue

WebAvanue is a voice-controlled web browser:

| Command | What It Does |
|---------|-------------|
| "go to google.com" | Navigates to the website |
| "click link name" | Clicks links by their text |
| "scroll down" | Scrolls the web page down |
| "scroll up" | Scrolls the web page up |
| "find text" | Searches for text on the page |
| "new tab" | Opens a new browser tab |
| "close tab" | Closes the current tab |
| "back" | Goes to previous web page |
| "forward" | Goes to next web page |
| "refresh" | Reloads the page |

---

## Content Avenues

### NoteAvanue — Voice Notes
Create and format notes using voice:

| Command | What It Does |
|---------|-------------|
| "new note" | Creates a new note |
| "dictate" | Enters dictation mode (speak and text appears) |
| "heading one" | Formats text as heading |
| "heading two" | Formats as subheading |
| "bullet point" | Starts a bulleted list |
| "numbered list" | Starts a numbered list |
| "bold" | Makes text bold |
| "italic" | Makes text italic |
| "save note" | Saves the note |

### PhotoAvanue — Camera
Take photos and videos:

| Command | What It Does |
|---------|-------------|
| "take photo" | Captures a photo |
| "start recording" | Starts video recording |
| "stop recording" | Stops video recording |
| "switch camera" | Switches between front and back cameras |
| "flash on" | Enables flash |
| "flash off" | Disables flash |
| "zoom in" | Zooms in (up to 10x) |
| "zoom out" | Zooms out |

### PDFAvanue — PDF Viewer
Navigate PDF documents:

| Command | What It Does |
|---------|-------------|
| "next page" | Goes to the next page |
| "previous page" | Goes to the previous page |
| "page 5" | Jumps to a specific page |
| "zoom in" | Zooms in on the PDF |
| "zoom out" | Zooms out |
| "search text" | Searches within the PDF |

---

## Cockpit — Multi-Window Mode

Cockpit lets you open multiple content windows side-by-side:

| Command | What It Does |
|---------|-------------|
| "add frame" | Adds a new frame to the layout |
| "remove frame" | Closes the current frame |
| "frame scroll up" | Scrolls within a specific frame |
| "frame scroll down" | Scrolls within a specific frame |
| "frame zoom in" | Zooms a specific frame |
| "frame zoom out" | Unzooms a frame |
| "split view" | Changes layout to split screen |
| "grid view" | Changes layout to grid |
| "carousel view" | Changes to carousel layout |

---

## Settings

Access VoiceOS settings from the Avanues app main menu:

### Speech Recognition
- **Engine Selection** — Choose between Whisper, Google Cloud Speech-to-Text, or Android STT
- **Microphone Sensitivity** — Adjust how easily VoiceOS picks up your voice
- **Confidence Threshold** — Set how confident VoiceOS must be before executing commands

### Theme and Appearance
- **Color Palette** — Choose from SOL, LUNA, TERRA, or HYDRA
- **Material Style** — Select Glass, Water, Cupertino, or MountainView design
- **Light/Dark Mode** — Switch appearance or follow system preference

### VOS Profiles
- **Import Profile** — Load a .vos file with custom voice commands
- **Export Profile** — Save your current commands to a file
- **Sync Profile** — Sync commands to the cloud for use on other devices

### Accessibility Options
- **Badge Size** — Make badges larger or smaller
- **Voice Feedback** — Enable/disable audio confirmation of commands
- **Voice Speed** — Adjust text-to-speech playback speed

---

## Tips and Tricks

### General Tips
- **Speak naturally** — VoiceOS understands partial phrases, not just exact matches
- **If multiple elements match** — VoiceOS shows a numbered list. Say the number you want
- **Use the "help" command** — Displays all available commands on any screen
- **Voice commands work everywhere** — Even in apps not made by Augmentalis

### Improve Recognition Accuracy
- Speak clearly and at a normal pace
- Reduce background noise
- Hold the device closer to your mouth
- Use full phrases (e.g., "take screenshot" instead of just "screenshot")

### Save Time
- Use numbers instead of labels for faster clicks
- Create a custom .vos profile with your favorite commands
- Use dictation for long text instead of typing character by character

---

## Troubleshooting

### Issue: No badges are showing
**Solution:**
1. Check that VoiceOS is enabled in Android Settings > Accessibility
2. Make sure the app is in focus (not running in background)
3. Restart the Avanues app
4. Verify the overlay permission is granted

### Issue: Commands are not being recognized
**Solution:**
1. Check that microphone permission is granted
2. Say "start listening" to activate voice recognition
3. Move to a quieter location if there's background noise
4. Speak more clearly
5. Check that microphone is not muted

### Issue: VoiceOS clicks the wrong element
**Solution:**
1. Use the numbered badge instead (e.g., say "click 3" instead of the button label)
2. Say "help" to see all elements with numbers
3. Reduce background noise that might confuse the speech engine

### Issue: App crashes or freezes
**Solution:**
1. Go to Android Settings > Accessibility
2. Verify VoiceOS is active and enabled
3. Restart your device
4. Reinstall the Avanues app if crashes continue

### Issue: VoiceOS is too sensitive (responding to background noise)
**Solution:**
1. Open Avanues Settings
2. Reduce the Microphone Sensitivity slider
3. Increase the Confidence Threshold
4. Disable voice feedback if notifications are causing issues

### Issue: Commands are slow to execute
**Solution:**
1. Reduce the Confidence Threshold (VoiceOS will execute faster)
2. Use numbered badges instead of label matching
3. Switch to a faster speech engine (Whisper tends to be fastest)
4. Close other apps running in background

---

## Getting Help

### In-App Help
- Say **"what can I say?"** on any screen to see available commands
- Say **"help"** to display all interactive elements with numbered badges

### Avanues Support
- Visit the Avanues app main menu for contact information
- Check the app for tutorials and demo videos
- Report issues or request features through the app feedback form

### Common Issues Video
The Avanues app includes tutorial videos for:
- First-time setup
- Using badges effectively
- Voice command tips
- Troubleshooting

---

## Keyboard Shortcuts (Android Device Only)

For users with external keyboards:

| Shortcut | What It Does |
|----------|-------------|
| Alt + V | Start VoiceOS |
| Alt + X | Stop VoiceOS |
| Alt + M | Mute/unmute microphone |
| Alt + H | Show help badges |

---

## Accessibility Features

VoiceOS is designed for accessibility:

- **Works with TalkBack** — Compatible with Android's built-in screen reader
- **Adjustable Text Size** — Control badge and UI text size in settings
- **Voice Feedback** — Audio confirmation for every command
- **High Contrast Badges** — Visible against any background
- **Voice Control for Everything** — Fully accessible without touching the screen

---

## Privacy and Data

- VoiceOS only records when you say **"start listening"** or a voice command
- Your voice data stays on your device — nothing is sent to cloud servers by default
- You can enable optional cloud speech recognition in settings (requires explicit permission)
- Exported .vos profile files contain your commands but no personal data
- Microphone access can be disabled in Android Accessibility settings at any time

---

## Feedback and Feature Requests

We'd love to hear from you! Share your experience:

1. Open the Avanues app
2. Tap the **Feedback** or **Settings** icon
3. Select **Send Feedback**
4. Describe your experience or request a feature

Your feedback helps us improve VoiceOS for everyone.

---

## Advanced: Custom Voice Commands

Users can create custom voice command profiles:

1. Use NoteAvanue to draft your custom commands
2. Export as a .vos profile from Settings > VOS Profiles
3. Share with other users or sync across your devices
4. Import profiles from other users to extend your command set

See **"VOS Profile Format Guide"** in the app help menu for detailed syntax.

---

## Quick Reference Card

Print this for quick access:

```
BASIC COMMANDS:
- Click [label] or Click [#]
- Scroll up/down
- Go back / Go home
- Open [app] / Close app

TEXT COMMANDS:
- Type [text]
- Select all / Copy / Paste / Delete

MEDIA COMMANDS:
- Play / Pause / Next / Previous
- Volume up/down / Mute

HELP COMMANDS:
- What can I say?
- Help
- Start/Stop listening
```

---

**Version:** 1.0
**Last Updated:** February 2026
**Compatible with:** VoiceOS v2.1+, Avanues App v3.0+

For the most up-to-date version of this guide, visit the Avanues app help menu or the Augmentalis support website.

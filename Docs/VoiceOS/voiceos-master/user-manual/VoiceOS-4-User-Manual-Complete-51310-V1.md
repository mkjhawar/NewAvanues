# VoiceOS 4 User Manual - Complete Guide

**Created:** 2025-10-13 21:44 PDT
**Version:** 3.0.0
**Status:** Production
**Note:** VoiceOS 4 (shortform: vos4) is a voice-controlled Android accessibility system

---

## Table of Contents

1. [Welcome to VoiceOS 4](#welcome-to-voiceos-4)
2. [Getting Started](#getting-started)
3. [Basic Voice Commands](#basic-voice-commands)
4. [Learning New Commands](#learning-new-commands)
5. [Web Control](#web-control)
6. [Detailed Use Cases](#detailed-use-cases)
7. [Voice Command Reference](#voice-command-reference)
8. [Tips & Best Practices](#tips--best-practices)
9. [Accessibility Features](#accessibility-features)
10. [Troubleshooting](#troubleshooting)
11. [FAQ](#faq)
12. [Appendix](#appendix)
    - [Appendix A: Supported Browsers](#appendix-a-supported-browsers)
    - [Appendix B: Common Voice Phrases](#appendix-b-common-voice-phrases)
    - [Appendix C: Managing Voice Recognition Models](#appendix-c-managing-voice-recognition-models)

---

## Welcome to VoiceOS 4

### What is VoiceOS 4?

**VoiceOS 4 (vos4)** is a powerful voice control system that lets you control your Android device completely hands-free. Just speak naturally, and VoiceOS 4 will understand and execute your commands.

### What Can You Do With VoiceOS 4?

✅ **Control Any App** - Navigate, click buttons, fill forms with voice
✅ **Browse the Web** - Voice control for websites in your browser
✅ **System Control** - Volume, brightness, navigation, and more
✅ **Smart Learning** - Automatically learns commands as you use apps
✅ **Natural Speech** - Speak naturally, no rigid command syntax
✅ **Hands-Free** - Perfect for driving, cooking, accessibility needs

### How Does It Work?

VoiceOS 4 uses advanced speech recognition and Android's accessibility features to understand what you say and interact with your device:

```
You Speak → VoiceOS 4 Recognizes → Finds the Right Action → Executes Command
```

**Example:**
```
You say: "Open calculator"
VoiceOS 4: Recognizes "open calculator" → Finds Calculator app → Launches it
```

### Who Is It For?

- **Accessibility Users**: Full hands-free control for those with mobility challenges
- **Drivers**: Safe voice control while driving
- **Multitaskers**: Control your phone while cooking, working, or exercising
- **Productivity Users**: Faster navigation and app control
- **Anyone**: Who wants a more convenient way to use their phone

---

## Getting Started

### Requirements

**Your Device Needs:**
- Android 10 or higher (Android 10, 11, 12, 13, 14, 15)
- Microphone for voice input
- 50 MB free storage
- Internet connection (for some speech engines)

**Compatible With:**
- ✅ 95% of Android devices from 2019 onwards
- ✅ Phones and tablets
- ✅ Smart glasses (Android-based)

### Installation

**Step 1: Install VoiceOS 4**
```
1. Download VoiceOS 4 from Google Play Store or your app source
2. Tap "Install"
3. Wait for installation to complete
```

**Step 2: Enable Accessibility Service**
```
1. Open Android Settings
2. Go to "Accessibility"
3. Find "VoiceOS 4" in the list
4. Toggle it ON
5. Confirm by tapping "OK" in the permission dialog
```

**Why Accessibility Permission?**
VoiceOS 4 needs this permission to:
- See what's on your screen
- Click buttons and navigate for you
- Read text from apps
- Interact with UI elements

**Step 3: Grant Microphone Permission**
```
1. VoiceOS 4 will request microphone access
2. Tap "Allow" to enable voice control
```

**Step 4: Test Your Setup**
```
1. Say "Go home"
2. Your device should navigate to the home screen
3. If it works, you're ready! 🎉
```

### Quick Start Tutorial

**Try These Commands:**

1. **Navigation**
   - Say: "Go home"
   - Result: Returns to home screen

2. **Volume**
   - Say: "Volume up"
   - Result: Increases system volume

3. **Open App**
   - Say: "Open Chrome"
   - Result: Launches Chrome browser

4. **Go Back**
   - Say: "Go back"
   - Result: Returns to previous screen

**🎉 Congratulations!** You're now using VoiceOS 4!

---

## Basic Voice Commands

### Navigation Commands

**Home Screen:**
```
"Go home"
"Home screen"
"Take me home"
```

**Back Button:**
```
"Go back"
"Back"
"Previous screen"
```

**Recent Apps:**
```
"Recent apps"
"Show recent"
"Open recent"
"Task switcher"
```

**Notifications:**
```
"Open notifications"
"Show notifications"
"Notification shade"
```

**Quick Settings:**
```
"Quick settings"
"Open settings panel"
```

### Volume Control

**Increase Volume:**
```
"Volume up"
"Louder"
"Increase volume"
"Turn it up"
```

**Decrease Volume:**
```
"Volume down"
"Quieter"
"Decrease volume"
"Turn it down"
```

**Mute:**
```
"Mute"
"Silence"
"Turn off sound"
```

**Unmute:**
```
"Unmute"
"Sound on"
```

### Media Control

**Play/Pause:**
```
"Play music"
"Pause music"
"Play"
"Pause"
```

**Next Track:**
```
"Next song"
"Next track"
"Skip"
```

**Previous Track:**
```
"Previous song"
"Previous track"
"Go back"
```

### System Control

**Lock Screen:**
```
"Lock screen"
"Lock phone"
"Lock device"
```

**Take Screenshot:**
```
"Take screenshot"
"Capture screen"
"Screenshot"
```

**Open Settings:**
```
"Open settings"
"Settings"
```

### Notification Control

**Read Notifications:**
```
"Read notifications"
"What are my notifications"
"Check notifications"
```

**Dismiss Notification:**
```
"Dismiss notification"
"Clear notification"
"Remove notification"
```

**Dismiss All Notifications:**
```
"Dismiss all notifications"
"Clear all notifications"
"Remove all notifications"
```

> **Note:** Notification commands require notification access permission. Go to Settings → Notifications → Notification Access and enable VoiceOS 4.

### Scroll Control

**Scroll Page:**
```
"Scroll down"
"Scroll up"
"Scroll left"
"Scroll right"
"Scroll to top"
"Scroll to bottom"
```

### Text Editing

**Copy/Paste:**
```
"Copy"
"Paste"
"Cut"
"Select all"
"Delete"
```

### Keyboard Control

**Show/Hide Keyboard:**
```
"Open keyboard"
"Close keyboard"
"Hide keyboard"
"Switch keyboard"
```

### App Launching

**Launch Any App:**
```
"Open [app name]"
"Launch [app name]"
"Start [app name]"
```

**Examples:**
```
"Open Chrome" → Launches Chrome browser
"Open Gmail" → Launches Gmail
"Open Calculator" → Launches Calculator
"Open Maps" → Launches Google Maps
"Open Messages" → Launches messaging app
```

---

## Learning New Commands

### How VoiceOS 4 Learns

VoiceOS 4 **automatically learns** voice commands for every app you use. As you interact with apps, VoiceOS 4:

1. **Observes** what buttons and elements exist
2. **Generates** voice commands for each element
3. **Remembers** commands even after restart
4. **Updates** when apps change

**You don't need to teach it—it learns by itself!**

### Using Learned Commands

**Example: Gmail App**

After you open Gmail a few times, VoiceOS 4 learns:

```
"Click compose" → Opens new email
"Click send" → Sends current email
"Click inbox" → Goes to inbox
"Click menu" → Opens hamburger menu
"Click search" → Focuses search bar
```

### App-Specific Commands

**Example: Music Player**

```
"Click play button" → Plays music
"Click next" → Next track
"Click shuffle" → Enables shuffle mode
"Click repeat" → Enables repeat mode
```

**Example: Settings**

```
"Click wifi" → Opens WiFi settings
"Click bluetooth" → Opens Bluetooth settings
"Click display" → Opens display settings
```

### How to Know What Commands Are Available

**Method 1: Try natural phrases**
```
"Click [button name]"
"Tap [element name]"
"Open [menu name]"
```

**Method 2: Look at button labels**

If you see a button labeled "Submit", you can say:
```
"Click submit"
"Tap submit button"
"Press submit"
```

**Method 3: Use content descriptions**

Many apps have accessibility labels. If a button says "Play" you can say:
```
"Click play"
"Tap play button"
```

---

## Web Control

### Voice Control in Browsers

VoiceOS 4 supports voice control for **websites** in these browsers:

✅ Chrome
✅ Firefox
✅ Brave
✅ Opera
✅ Microsoft Edge
✅ Samsung Internet
✅ DuckDuckGo
✅ Kiwi Browser

### Website Commands

**After you use a website once, VoiceOS 4 learns commands for that site.**

**Example: Google.com**

```
"Click search" → Clicks search button
"Click images" → Goes to Google Images
"Click sign in" → Opens sign-in page
```

**Example: YouTube.com**

```
"Click play" → Plays video
"Click pause" → Pauses video
"Click subscribe" → Subscribes to channel
"Click like" → Likes the video
"Click next video" → Goes to next video
```

**Example: Amazon.com**

```
"Click add to cart" → Adds item to cart
"Click buy now" → Initiates purchase
"Click search" → Focuses search box
```

### Learning Web Commands

**The LearnWeb Feature:**

VoiceOS 4 includes a special app called **LearnWeb** that helps you teach it new web commands.

**How to Use LearnWeb:**

1. **Open LearnWeb** app from your app drawer

2. **Navigate to a website**
   - Say: "Go to amazon.com"
   - Or enter URL manually

3. **Use the website normally**
   - Click buttons, links, forms
   - VoiceOS 4 watches and learns

4. **Commands are saved automatically**
   - Next time you visit, voice commands work!

**Example Learning Session:**

```
Step 1: Open LearnWeb
Step 2: Say "go to facebook.com"
Step 3: Click "Log In" button (with your finger)
Step 4: Click "Create Account" button (with your finger)
Step 5: Close LearnWeb

Next time on Facebook:
Say "click log in" → Works! ✓
Say "click create account" → Works! ✓
```

---

## Detailed Use Cases

### Use Case 1: Morning Routine

**Scenario:** You're getting ready in the morning and want to check weather, news, and messages.

**Voice Commands:**

```
1. "Go home" (from lock screen)
2. "Open weather" (launches weather app)
   [Check forecast]
3. "Go back"
4. "Open news" (launches news app)
   [Scroll through headlines]
5. "Go back"
6. "Open messages" (checks texts)
```

**Time Saved:** 30 seconds per day = 3 hours per year

### Use Case 2: Driving Navigation

**Scenario:** You're driving and need hands-free control.

**Voice Commands:**

```
1. "Open maps"
2. "Click search"
3. "Navigate to Starbucks" (if voice input works in Maps)
4. Volume control: "Volume up" / "Volume down"
5. Music control: "Next song" / "Pause"
6. If call comes in: "Go home" → "Open phone"
```

**Safety Benefit:** Keep hands on wheel, eyes on road

### Use Case 3: Cooking in Kitchen

**Scenario:** Your hands are dirty/busy while cooking, but you need to use your tablet.

**Voice Commands:**

```
1. "Open recipe app"
2. "Scroll down" (see next step)
3. "Set timer 10 minutes" (if timer app supports it)
4. "Volume up" (to hear music while cooking)
5. "Pause music" (when timer goes off)
```

**Convenience:** No need to wash hands every time

### Use Case 4: Accessibility - Limited Mobility

**Scenario:** User has limited hand mobility and needs full device control.

**Voice Commands:**

```
Complete phone control without touching screen:

Navigation:
- "Go home" / "Go back" / "Recent apps"

Apps:
- "Open any app"
- "Click any button in app"

Text:
- "Click text field" + "Type hello"

Settings:
- "Open settings"
- "Click wifi"
- "Click bluetooth"

Everything is voice-controlled!
```

### Use Case 5: Shopping Online

**Scenario:** Shopping on Amazon using Chrome browser.

**Voice Commands:**

```
1. "Open Chrome"
2. "Click address bar"
3. (Type or speak URL: "amazon.com")
4. "Click search box"
5. (Type or speak search: "wireless headphones")
6. "Click first result"
7. "Click add to cart"
8. "Click cart icon"
9. "Click proceed to checkout"
```

**After first visit, commands are remembered for next time!**

### Use Case 6: Social Media

**Scenario:** Browsing Instagram on your phone.

**Voice Commands:**

```
1. "Open Instagram"
2. "Scroll down" (view feed)
3. "Click like" (like a post)
4. "Click comment" (add comment)
5. "Click profile" (go to your profile)
6. "Click settings" (adjust settings)
```

**Note:** Commands learned after first use of app

### Use Case 7: Email Management

**Scenario:** Managing email in Gmail app.

**Voice Commands:**

```
1. "Open Gmail"
2. "Click compose" (new email)
3. "Click to field" (recipient)
4. (Type recipient address)
5. "Click subject" (subject line)
6. (Type subject)
7. "Click message body"
8. (Type or dictate message)
9. "Click send" (send email)
```

**Efficiency:** Faster than tapping through menus

### Use Case 8: Work Productivity

**Scenario:** Using productivity apps for work.

**Voice Commands:**

```
Calendar:
1. "Open calendar"
2. "Click new event"
3. (Fill event details)
4. "Click save"

Notes:
1. "Open keep" (or your notes app)
2. "Click new note"
3. (Type or dictate note)
4. "Click done"

Documents:
1. "Open docs"
2. "Click new document"
3. (Create document)
4. "Click share"
```

### Use Case 9: Entertainment

**Scenario:** Watching videos and listening to music.

**Voice Commands:**

```
YouTube:
1. "Open YouTube"
2. "Click search"
3. (Search for video)
4. "Click first result"
5. "Click play"
6. "Click like"
7. "Click subscribe"

Spotify:
1. "Open Spotify"
2. "Click search"
3. (Search for song/artist)
4. "Click play"
5. "Next song"
6. "Volume up"
```

### Use Case 10: Quick Settings Adjustment

**Scenario:** Adjust phone settings quickly.

**Voice Commands:**

```
Brightness:
1. "Quick settings"
2. "Click brightness"
3. "Increase brightness" / "Decrease brightness"

WiFi:
1. "Open settings"
2. "Click wifi"
3. "Click network name"

Bluetooth:
1. "Open settings"
2. "Click bluetooth"
3. "Click device name"
```

---

## Voice Command Reference

### Complete Command List

#### System Navigation

| Command | Action |
|---------|--------|
| "Go home" | Return to home screen |
| "Go back" | Return to previous screen |
| "Recent apps" | Show recent apps |
| "Open notifications" | Open notification shade |
| "Quick settings" | Open quick settings panel |
| "Lock screen" | Lock the device |

#### Volume & Media

| Command | Action |
|---------|--------|
| "Volume up" | Increase volume |
| "Volume down" | Decrease volume |
| "Mute" | Mute all sounds |
| "Unmute" | Unmute sounds |
| "Play music" | Play/resume media |
| "Pause music" | Pause media |
| "Next song" | Skip to next track |
| "Previous song" | Go to previous track |

#### App Control

| Command | Action |
|---------|--------|
| "Open [app name]" | Launch application |
| "Close app" | Close current app |
| "Switch to [app]" | Switch to running app |

#### UI Interaction

| Command | Action |
|---------|--------|
| "Click [button name]" | Click button/element |
| "Tap [element name]" | Tap element |
| "Press [button name]" | Press button |
| "Scroll up" | Scroll content up |
| "Scroll down" | Scroll content down |
| "Swipe left" | Swipe gesture left |
| "Swipe right" | Swipe gesture right |

#### Web Control (in browsers)

| Command | Action |
|---------|--------|
| "Click [link text]" | Click web link |
| "Click [button]" | Click web button |
| "Scroll down" | Scroll page down |
| "Scroll up" | Scroll page up |
| "Go back" | Browser back button |

#### Utility Commands

| Command | Action |
|---------|--------|
| "Take screenshot" | Capture screen |
| "Open settings" | Open system settings |
| "Search [query]" | Search device/web |

---

## Tips & Best Practices

### Speaking Tips

**1. Speak Clearly**
- Use normal speaking voice
- Don't shout or whisper
- Pronounce words clearly

**2. Speak Naturally**
- No need for robot voice
- Use natural phrasing
- Speak at normal pace

**3. Reduce Background Noise**
- Find quiet environment when possible
- Move away from loud sounds
- Consider using headset microphone in noisy areas

**4. Wait for Recognition**
- Give VoiceOS 4 a moment to process
- Don't rush to repeat command
- Watch for visual/audio feedback

### Command Tips

**1. Use Exact Names**
```
✅ "Open Chrome" (exact app name)
❌ "Open that browser app" (too vague)
```

**2. Be Specific**
```
✅ "Click submit button"
✅ "Tap login"
❌ "Click that thing" (not specific enough)
```

**3. Try Variations**

If one phrase doesn't work, try another:
```
"Go home"
"Home screen"
"Take me home"
"Navigate home"
```

**4. Learn As You Go**

After using an app a few times, VoiceOS 4 knows it better:
```
First time: May need to try different phrases
After learning: Commands work consistently
```

### Battery Optimization

**VoiceOS 4 and Battery Life:**

Voice recognition uses battery, but you can optimize:

**1. Use Only When Needed**
- Disable voice control when not in use
- Enable for specific tasks (driving, cooking, etc.)

**2. Choose Efficient Speech Engine**
- Google Speech: Uses internet, moderate battery
- Vosk: Offline, lower battery usage
- Vivoka: Balanced performance

**3. Reduce Screen On Time**
- Voice control can reduce screen-on time
- Less touching = less screen usage = better battery

### Privacy & Security

**What VoiceOS 4 Can Access:**

✅ What's on your screen (via Accessibility)
✅ Microphone input (for voice commands)
✅ App package names (to know which app is open)

❌ Cannot access:
- Your passwords
- Private messages content (only sees buttons)
- Files and documents
- Camera

**Privacy Tips:**

1. **Review Permissions**
   - Check Settings → Apps → VoiceOS 4 → Permissions

2. **Disable When Not Needed**
   - Turn off accessibility service when not using

3. **Secure Your Voice**
   - Be aware others can hear your commands
   - Don't speak sensitive info in public

---

## Accessibility Features

### For Users with Disabilities

VoiceOS 4 is designed with accessibility in mind:

**Motor Impairments:**
- ✅ Full device control without touching screen
- ✅ Navigate apps completely with voice
- ✅ No precise finger movements needed

**Visual Impairments:**
- ✅ Voice-first interface
- ✅ Works with screen readers
- ✅ Audio feedback options

**Cognitive Disabilities:**
- ✅ Natural language commands
- ✅ No complex syntax required
- ✅ Flexible command variations

### Accessibility Settings

**Adjust for Your Needs:**

1. **Speech Recognition Sensitivity**
   - Settings → VoiceOS 4 → Sensitivity
   - Adjust for soft or loud voice

2. **Command Timeout**
   - Settings → VoiceOS 4 → Timeout
   - Give more time for command recognition

3. **Feedback Mode**
   - Settings → VoiceOS 4 → Feedback
   - Audio, visual, or haptic confirmation

### Working with Other Accessibility Services

**Compatible With:**
- ✅ TalkBack (screen reader)
- ✅ Switch Access
- ✅ Voice Access (Google)
- ✅ Other accessibility services

---

## Troubleshooting

### Voice Commands Not Working

**Problem:** VoiceOS 4 doesn't respond to voice

**Solutions:**

1. **Check Microphone Permission**
   ```
   Settings → Apps → VoiceOS 4 → Permissions → Microphone → Allow
   ```

2. **Check Accessibility Service**
   ```
   Settings → Accessibility → VoiceOS 4 → Should be ON
   ```

3. **Test Microphone**
   - Open voice recorder app
   - Record audio
   - Play back
   - If no sound, microphone issue

4. **Restart VoiceOS 4**
   ```
   Settings → Accessibility → VoiceOS 4 → OFF → ON
   ```

5. **Restart Device**
   - Power off
   - Power on
   - Try again

### Recognition Accuracy Issues

**Problem:** VoiceOS 4 misunderstands commands

**Solutions:**

1. **Speak Clearly**
   - Use clear pronunciation
   - Speak at normal pace
   - Don't mumble

2. **Reduce Background Noise**
   - Move to quieter location
   - Use headset microphone
   - Close windows if outside noise

3. **Try Different Phrasing**
   - "Go home" vs "Home screen"
   - "Open Chrome" vs "Launch Chrome"

4. **Check Language Settings**
   ```
   Settings → VoiceOS 4 → Language → Match device language
   ```

### App-Specific Commands Not Working

**Problem:** Commands work for system but not for apps

**Solution:**

**Wait for Learning:**
- VoiceOS 4 needs to see the app first
- Open app and navigate around
- Wait 10-30 seconds
- Commands will be available next time

**Manual Learning:**
1. Open LearnWeb app
2. Open target app
3. Click buttons manually
4. Commands learned

### Web Commands Not Working

**Problem:** Voice control doesn't work in browser

**Solutions:**

1. **Check Browser Supported**
   - Only works in: Chrome, Firefox, Brave, Opera, Edge, Samsung Internet, DuckDuckGo, Kiwi
   - Not supported: Custom/unknown browsers

2. **Learn Commands First**
   ```
   1. Open LearnWeb app
   2. Navigate to website
   3. Click elements manually
   4. Close LearnWeb
   5. Try voice commands
   ```

3. **Wait for Page Load**
   - Let page fully load
   - Wait for elements to appear
   - Then use voice commands

### Battery Drain

**Problem:** VoiceOS 4 using too much battery

**Solutions:**

1. **Disable When Not Needed**
   ```
   Settings → Accessibility → VoiceOS 4 → OFF
   ```

2. **Optimize Settings**
   ```
   Settings → VoiceOS 4 → Power Saving Mode → ON
   ```

3. **Switch Speech Engine**
   - Change to offline engine (Vosk)
   - Uses less power than online engines

### VoiceOS 4 Stops Working After Update

**Problem:** Commands stop working after Android update

**Solutions:**

1. **Re-enable Accessibility**
   ```
   Settings → Accessibility → VoiceOS 4 → OFF → ON
   ```

2. **Clear App Cache**
   ```
   Settings → Apps → VoiceOS 4 → Storage → Clear Cache
   ```

3. **Restart Device**
   - Power off device
   - Wait 10 seconds
   - Power on

4. **Reinstall if Necessary**
   - Uninstall VoiceOS 4
   - Restart device
   - Reinstall from store

---

## FAQ

### General Questions

**Q: Is VoiceOS 4 free?**
A: Check the app store listing for current pricing and plans.

**Q: Does VoiceOS 4 work offline?**
A: Depends on speech engine:
- Google Speech: Requires internet
- Vosk: Works completely offline
- Vivoka: Hybrid (some features offline)

**Q: What Android versions are supported?**
A: Android 10 and higher (Android 10, 11, 12, 13, 14, 15)

**Q: Does it work on tablets?**
A: Yes! VoiceOS 4 works on both phones and tablets.

**Q: Can I use it with Bluetooth headset?**
A: Yes, Bluetooth headsets with microphone work perfectly.

### Privacy & Security

**Q: Can VoiceOS 4 see my passwords?**
A: No. VoiceOS 4 can see UI elements but cannot access password fields' content.

**Q: Is my voice data stored?**
A: Voice data is processed for command recognition only. Check app privacy policy for details.

**Q: Can other apps access VoiceOS 4 data?**
A: No. VoiceOS 4 data is sandboxed and private to the app.

**Q: Does VoiceOS 4 send data to servers?**
A: Only if using online speech engines (Google). Offline engines (Vosk) keep all data local.

### Features & Compatibility

**Q: Can I control every app?**
A: Most apps work, but some (like games) may have limited support. Standard Android apps work best.

**Q: Does it work with custom launchers?**
A: Yes, VoiceOS 4 works with all Android launchers.

**Q: Can I customize voice commands?**
A: Yes, through the command management interface (coming in future updates).

**Q: Does it support multiple languages?**
A: Currently supports English. More languages planned for future releases.

**Q: Can I use wake word ("Hey VoiceOS")?**
A: Wake word feature is planned for a future update.

### Technical Questions

**Q: How much storage does VoiceOS 4 use?**
A: Approximately 50-100 MB, depending on learned commands.

**Q: Does it slow down my phone?**
A: Minimal impact. Runs efficiently in background.

**Q: Can I use it with other voice assistants?**
A: Yes, VoiceOS 4 works alongside Google Assistant, Alexa, etc.

**Q: What's the difference between speech engines?**
A:
- **Google**: Most accurate, requires internet
- **Vosk**: Offline, good accuracy, no internet needed
- **Vivoka**: Balanced, some features offline

**Q: Why do some commands work and others don't?**
A: VoiceOS 4 learns commands over time. The more you use an app, the more commands become available.

### Troubleshooting

**Q: VoiceOS 4 keeps turning off. Why?**
A: Android battery optimization may be killing the service. Go to Settings → Apps → VoiceOS 4 → Battery → Unrestricted.

**Q: Commands worked before but stopped. What happened?**
A: Try:
1. Re-enable accessibility service
2. Restart device
3. Clear app cache

**Q: Can I use VoiceOS 4 while screen is off?**
A: Currently requires screen to be on. Wake lock feature planned for future update.

**Q: Why doesn't it work in [specific app]?**
A: Some apps have accessibility restrictions. Most standard apps work. Try using the app first to let VoiceOS 4 learn it.

---

## Getting Help

### Support Resources

**Official Documentation:**
- User Manual: (this document)
- Video Tutorials: [Coming soon]
- Online Help Center: [Link to help center]

**Community:**
- Forums: [Link to forums]
- Discord: [Link to Discord]
- Reddit: r/VoiceOS (if available)

**Direct Support:**
- Email: support@augmentalis.com
- In-app Support: Settings → Help & Support

### Reporting Issues

**Before Reporting:**
1. Check this manual's troubleshooting section
2. Search forums for similar issues
3. Try basic fixes (restart, re-enable service)

**When Reporting:**
Include:
- Android version
- Device model
- VoiceOS 4 version
- Steps to reproduce issue
- Error messages (if any)
- Screenshots (helpful but optional)

---

## Appendix

### Appendix A: Supported Browsers

| Browser | Package Name | Status |
|---------|--------------|--------|
| Chrome | com.android.chrome | ✅ Fully Supported |
| Firefox | org.mozilla.firefox | ✅ Fully Supported |
| Brave | com.brave.browser | ✅ Fully Supported |
| Opera | com.opera.browser | ✅ Fully Supported |
| Microsoft Edge | com.microsoft.emmx | ✅ Fully Supported |
| Samsung Internet | com.sec.android.app.sbrowser | ✅ Fully Supported |
| DuckDuckGo | com.duckduckgo.mobile.android | ✅ Fully Supported |
| Kiwi Browser | com.kiwibrowser.browser | ✅ Fully Supported |

### Appendix B: Common Voice Phrases by Category

**Navigation (System):**
- Home: "go home", "home screen", "main screen"
- Back: "go back", "back", "previous"
- Recent: "recent apps", "show recent", "task switcher"

**Volume:**
- Up: "volume up", "louder", "increase volume"
- Down: "volume down", "quieter", "decrease volume"
- Mute: "mute", "silence", "quiet"

**Media:**
- Play: "play", "play music", "resume"
- Pause: "pause", "pause music", "stop"
- Next: "next song", "skip", "next track"
- Previous: "previous song", "go back", "previous track"

**Apps:**
- Open: "open [name]", "launch [name]", "start [name]"
- Close: "close app", "close", "exit"

**Web (Browser):**
- Click: "click [text]", "tap [text]", "press [text]"
- Scroll: "scroll up", "scroll down", "scroll"
- Navigate: "go back", "go forward", "refresh"

### Appendix C: Managing Voice Recognition Models

#### Overview

VoiceOS uses AI-powered speech recognition that requires language model files. These models can be:
- Downloaded automatically when needed
- Pre-loaded for offline use
- Managed manually for advanced users

#### Understanding Model Storage

**What are model files?**
- AI data files that enable voice recognition in different languages
- Size: 100-200MB per language
- Stored on your device for offline use

**Where are they stored?**
VoiceOS checks three locations automatically:
1. **Internal app storage** (automatic, default)
2. **External app storage** (accessible via file manager)
3. **Shared storage** (survives app updates) ⭐ **BEST FOR ADVANCED USERS**

#### Automatic Download (Default - Recommended)

**How it works:**
1. Launch VoiceOS for the first time
2. Select your language in Settings
3. App automatically downloads required models
4. Wait 30-60 seconds for download (Wi-Fi recommended)
5. Voice recognition activates

**Download progress:**
- You'll see a notification showing download progress
- Example: "Downloading Spanish: 45%"
- Example: "Extracting Spanish model files..."
- Example: "Spanish ready!"

**Network requirements:**
- **First use:** Wi-Fi or mobile data required
- **After download:** Works completely offline

**Note:** The automatic download happens in the background. You can continue using other apps while models download.

#### Manual Model Management (Advanced Users Only)

**For advanced users who want:**
- Offline installation (no network needed)
- Pre-load models before first use
- Share models across multiple devices
- Manage storage manually

**⚠️ Warning:** Manual model management requires technical knowledge. Most users should use automatic download.

##### Option 1: Via File Manager (Easier)

**Step 1: Obtain Model Files**
Contact support or download from support website.

**Step 2: Place Files**
Using your device's file manager (e.g., Files by Google):
1. Navigate to: `Internal Storage/.voiceos/vivoka/`
2. Create folder if it doesn't exist (note: folder starts with a dot, making it hidden)
3. Copy `vsdk` folder here
4. Final path should be: `Internal Storage/.voiceos/vivoka/vsdk/`

**Step 3: Launch VoiceOS**
- Open VoiceOS app
- Models will be detected automatically
- No download needed!

**Expected folder structure:**
```
/storage/emulated/0/.voiceos/vivoka/vsdk/
├── config/
│   └── vsdk.json
└── data/
    └── csdk/
        └── asr/
            ├── acmod/
            ├── clc/
            ├── ctx/
            └── lm/
```

##### Option 2: Via Computer (ADB - Most Advanced)

**Requirements:**
- Computer with ADB installed
- USB cable
- USB Debugging enabled on device
- Technical knowledge of ADB commands

**Steps:**
```bash
# Connect device via USB
# Copy models to device
adb push vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/

# Verify
adb shell ls /storage/emulated/0/.voiceos/vivoka/vsdk/
```

**⚠️ Warning:** Using ADB incorrectly can damage your device. Only use if you know what you're doing.

#### Adding New Languages

**Automatic method (Recommended):**
1. Open VoiceOS Settings
2. Select "Language & Input"
3. Choose new language
4. App downloads model automatically (30-60 seconds)
5. Language available immediately after download

**Manual method (Advanced):**
1. Obtain language model files from support
2. Place in `.voiceos/vivoka/vsdk/data/csdk/asr/`
3. Restart VoiceOS
4. Language available immediately

#### Troubleshooting

**"Voice recognition not working"**
1. Check network connection (first use only)
2. Verify 100MB+ free storage space available
3. Check Settings → Language is set correctly
4. Try restarting the app
5. If problem persists, clear app data and let models re-download

**"Download failed"**
1. Connect to Wi-Fi (mobile data may have download limits)
2. Check available storage space (need 100-200MB free)
3. Try restarting the app
4. If issue persists, contact support

**"Models disappeared after app update"**
1. If models were in internal storage, they may be reset
2. Use shared storage (`.voiceos/vivoka/`) to preserve models across updates
3. Re-download models if needed (automatic)

**"Can't find .voiceos folder"**
1. The folder starts with a dot (.), making it hidden
2. In file manager, enable "Show hidden files" option
3. Navigate to Internal Storage root
4. Look for `.voiceos` folder

#### Storage Management

**How much space do models use?**
- English: ~120MB
- Spanish: ~150MB
- French: ~140MB
- Japanese: ~180MB
- Multiple languages: ~120-180MB each

**To free up space:**
1. Delete unused language models manually
2. Keep only the languages you use
3. You can always re-download models later

**Which folder to use?**
- **Internal app storage:** Automatic, no setup needed, deleted on app uninstall
- **External app storage:** Accessible via file manager, deleted on app uninstall
- **Shared storage (`.voiceos/`):** Survives app uninstall, good for multiple installations

#### Tips for Best Performance

✅ **Use Wi-Fi** for initial download (faster, no data charges)
✅ **Wait for download to complete** before using voice commands
✅ **Keep 200MB+ free space** for smooth operation
✅ **Use shared storage** if you frequently reinstall the app
✅ **Restart app** after manually adding models

**Note:** Models only need to be downloaded once per language. After that, voice recognition works completely offline!

---

## Conclusion

Thank you for choosing **VoiceOS 4**! We hope this user manual helps you get the most out of hands-free voice control on your Android device.

### Quick Recap

✅ **Installation**: Enable Accessibility + Microphone permissions
✅ **Basic Commands**: Navigation, volume, apps, media
✅ **Learning**: Commands learned automatically as you use apps
✅ **Web Control**: Works in 8+ major browsers
✅ **Accessibility**: Full device control for users with disabilities

### Next Steps

1. **Practice basic commands** (go home, volume up, open apps)
2. **Try app-specific commands** (use apps, let VoiceOS 4 learn)
3. **Explore web control** (use LearnWeb to teach website commands)
4. **Customize settings** (adjust for your preferences)
5. **Share feedback** (help us improve VoiceOS 4!)

### Stay Updated

- Check for app updates regularly
- Join our community forums
- Follow us for tips and new features

**Enjoy hands-free control with VoiceOS 4!** 🎤📱

---

**Document Version:** 3.0.0
**Last Updated:** 2025-10-13 21:44 PDT
**Status:** Production Ready
**For Technical Details:** See VoiceOS4-System-Architecture-Complete-251013-2144.md
**For Developers:** See VoiceOS4-Developer-Guide-Complete-251013-2144.md

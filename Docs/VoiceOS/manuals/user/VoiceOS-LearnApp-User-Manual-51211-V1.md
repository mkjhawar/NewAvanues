# AvaLearn User Manual

**VoiceOS LearnApp - Voice Command Learning Assistant**

**Version:** 1.0
**Date:** 2025-12-11
**Audience:** End Users
**Editions:** AvaLearnLite, AvaLearnPro

---

# Table of Contents

| Chapter | Title | Page |
|---------|-------|------|
| 1 | [Getting Started](#chapter-1-getting-started) | 1 |
| 2 | [Understanding JIT Learning](#chapter-2-understanding-jit-learning) | 2 |
| 3 | [AvaLearnLite Features](#chapter-3-avalearnlite-features) | 3 |
| 4 | [AvaLearnPro Features](#chapter-4-avalearnpro-features) | 4 |
| 5 | [Controlling Voice Learning](#chapter-5-controlling-voice-learning) | 5 |
| 6 | [Viewing Learning Progress](#chapter-6-viewing-learning-progress) | 6 |
| 7 | [Exporting Data](#chapter-7-exporting-data) | 7 |
| 8 | [Privacy & Safety](#chapter-8-privacy--safety) | 8 |
| 9 | [Troubleshooting](#chapter-9-troubleshooting) | 9 |
| 10 | [FAQ](#chapter-10-faq) | 10 |

---

# Chapter 1: Getting Started

## 1.1 What is AvaLearn?

AvaLearn is a companion app for VoiceOS that helps you see and control how voice commands are being learned for your apps. It works in the background to teach VoiceOS about the apps you use, so you can control them with your voice.

## 1.2 Two Editions

| Edition | For | Features |
|---------|-----|----------|
| **AvaLearnLite** | Everyone | Basic stats, pause/resume, simple UI |
| **AvaLearnPro** | Developers | Event logs, element inspector, debugging tools |

## 1.3 Prerequisites

Before using AvaLearn:

1. **VoiceOS must be installed** and running
2. **Accessibility Service enabled** for VoiceOS
3. **AvaLearn installed** (Lite or Pro edition)

## 1.4 First Launch

When you first open AvaLearn:

1. The app will attempt to connect to VoiceOS
2. You'll see "Connecting..." status
3. Once connected: "Service Connected"
4. If VoiceOS isn't running, you'll see "Service Disconnected"

### Connection Status

| Status | Meaning |
|--------|---------|
| Service Connected | VoiceOS is running, learning active |
| Service Disconnected | VoiceOS not running or accessibility disabled |
| Connecting... | Attempting to connect |

---

# Chapter 2: Understanding JIT Learning

## 2.1 What is JIT Learning?

JIT stands for "Just-In-Time" Learning. Instead of exploring every screen of an app ahead of time, VoiceOS learns screens as you naturally use your apps.

**How it works:**
1. You use your phone normally
2. VoiceOS watches which screens you visit
3. It captures the buttons, text fields, and other elements
4. It generates voice commands automatically
5. Next time, you can control those elements with your voice

## 2.2 Passive vs Active Learning

| Type | Description | User Action Required |
|------|-------------|---------------------|
| **Passive (JIT)** | Learns as you use apps | None - automatic |
| **Active (Exploration)** | Systematically explores an app | User triggers exploration |

AvaLearn shows you what's being learned passively through JIT.

## 2.3 What Gets Learned?

JIT captures:
- Buttons and clickable items
- Text input fields
- Scrollable areas
- Menu items
- List items
- Toggle switches

It generates commands like:
- "Click Settings"
- "Tap Send"
- "Press Submit"
- "Select Done"

## 2.4 Screen Hashing

VoiceOS uses "screen hashes" to identify unique screens. This means:
- Each unique screen layout is learned once
- Revisiting the same screen doesn't re-learn it
- Different content on the same layout = same screen

---

# Chapter 3: AvaLearnLite Features

## 3.1 Main Screen Overview

```
┌────────────────────────────────────┐
│         AvaLearn Lite              │
├────────────────────────────────────┤
│                                    │
│    Service Connected               │
│                                    │
│    ┌──────────────────────────┐    │
│    │  Screens Learned:   47   │    │
│    │  Elements Found:   312   │    │
│    │  Current App:    Chrome  │    │
│    └──────────────────────────┘    │
│                                    │
│    JIT Learning: ● ACTIVE          │
│                                    │
│    [ Pause Learning ]              │
│                                    │
│    Last Update: 2 seconds ago      │
│                                    │
└────────────────────────────────────┘
```

## 3.2 Statistics Display

| Stat | Description |
|------|-------------|
| **Screens Learned** | Number of unique screens captured |
| **Elements Found** | Total UI elements discovered |
| **Current App** | Which app is currently in foreground |

## 3.3 Learning Status Indicator

| Status | Meaning |
|--------|---------|
| ● ACTIVE (green) | Learning new screens as you use apps |
| ● PAUSED (yellow) | Learning temporarily stopped |
| ○ INACTIVE (gray) | VoiceOS not running |

## 3.4 Pause/Resume Button

Use this to temporarily stop learning:
- **Pause** when you're doing something private
- **Resume** when you want learning to continue

Learning automatically resumes when:
- You tap Resume
- You restart the VoiceOS service

---

# Chapter 4: AvaLearnPro Features

## 4.1 Additional Features

AvaLearnPro includes everything in Lite, plus:

| Feature | Description |
|---------|-------------|
| **Event Log** | Real-time stream of learning events |
| **Element Inspector** | View current screen's elements |
| **Developer Console** | Debug information |
| **Unencrypted Export** | Export data for analysis |

## 4.2 Event Log Tab

Shows real-time events as they happen:

```
[12:34:56] SCREEN: Screen changed - hash: abc123
[12:34:56] SCREEN: 23 elements captured
[12:34:58] ACTION: Click performed on btn_submit
[12:35:02] SCREEN: Screen changed - hash: def456
[12:35:02] LOGIN: Login screen detected - Chrome
```

### Event Types

| Type | Description |
|------|-------------|
| SCREEN | Screen change detected |
| ACTION | User interaction captured |
| LOGIN | Login screen detected (pauses learning) |
| MENU | Menu discovered |
| SCROLL | Scroll action detected |

## 4.3 Element Inspector Tab

View the current screen's accessibility tree:

```
┌─────────────────────────────────────┐
│  Element Inspector                  │
├─────────────────────────────────────┤
│  ▼ FrameLayout                      │
│    ▼ LinearLayout                   │
│      ● Button "Submit" [clickable]  │
│      ● EditText "Email" [editable]  │
│      ● TextView "Welcome"           │
│    ▼ RecyclerView [scrollable]      │
│      ● Item 1                       │
│      ● Item 2                       │
│      ● Item 3                       │
└─────────────────────────────────────┘
│ [ Refresh ]                         │
└─────────────────────────────────────┘
```

Tap an element to see details:
- Class name
- Text content
- Resource ID
- Bounds (position)
- Actions available

## 4.4 Developer Console

Shows system-level information:
- Service connection status
- Memory usage
- Database stats
- Error messages

---

# Chapter 5: Controlling Voice Learning

## 5.1 Pause Learning

**When to pause:**
- Entering sensitive information (passwords, banking)
- On screens you don't want voice control for
- When testing/debugging

**How to pause:**
1. Open AvaLearn
2. Tap "Pause Learning"
3. Status changes to "PAUSED"

**What happens:**
- No new screens are captured
- Existing voice commands still work
- JIT events stop

## 5.2 Resume Learning

**How to resume:**
1. Open AvaLearn
2. Tap "Resume Learning"
3. Status changes to "ACTIVE"

**What happens:**
- Screen capture resumes
- New screens will be learned
- Events start streaming again

## 5.3 Automatic Pause (Safety)

VoiceOS automatically pauses on:
- Login screens (detected by password fields)
- Banking apps (blocklist)
- System settings (security pages)

This protects your sensitive information.

---

# Chapter 6: Viewing Learning Progress

## 6.1 Screen Count

The "Screens Learned" number shows unique screens captured across all apps.

**Tips:**
- Higher numbers = more voice commands available
- Same screen revisited = doesn't increase count
- Different apps contribute separately

## 6.2 Element Count

The "Elements Found" shows total clickable/interactive elements.

**What counts as an element:**
- Buttons
- Links
- Text fields
- Toggle switches
- List items
- Menu items

## 6.3 Per-App Progress

In AvaLearnPro, you can see progress per app:

| App | Screens | Elements |
|-----|---------|----------|
| Chrome | 12 | 89 |
| Gmail | 8 | 67 |
| Settings | 23 | 156 |

## 6.4 Real-Time Updates

Stats update in real-time when:
- You navigate to a new screen
- New elements are discovered
- You return to AvaLearn from another app

---

# Chapter 7: Exporting Data

## 7.1 Why Export?

Export your learned data to:
- Backup voice commands
- Share with other devices
- Analyze learning patterns
- Debug issues

## 7.2 Export Formats

| Format | Available In | Use Case |
|--------|--------------|----------|
| AVU (encrypted) | Both editions | Backup, transfer |
| AVU (unencrypted) | Pro only | Development, analysis |
| JSON | Pro only | Integration, debugging |

## 7.3 How to Export

### AvaLearnLite:
1. Tap menu (⋮)
2. Select "Export Data"
3. Choose location
4. File saved as `.avu`

### AvaLearnPro:
1. Go to Export tab
2. Select format (AVU/JSON)
3. Choose encryption (on/off)
4. Tap "Export"
5. Share or save file

## 7.4 Import Data

To import previously exported data:
1. Tap menu (⋮)
2. Select "Import Data"
3. Choose `.avu` file
4. Confirm import

---

# Chapter 8: Privacy & Safety

## 8.1 What Data is Collected

| Data | Collected | Notes |
|------|-----------|-------|
| Screen structure | Yes | Layout, not content |
| Button labels | Yes | For voice commands |
| Password fields | No | Excluded |
| Text input content | No | Only field type |
| Personal data | No | Not captured |

## 8.2 Where Data is Stored

All learning data is stored:
- **Locally** on your device
- In VoiceOS database
- **Not** sent to cloud servers
- **Not** shared with third parties

## 8.3 Login Screen Protection

VoiceOS automatically detects login screens and:
1. Pauses learning
2. Shows "Login Detected" notification
3. Skips capturing password fields
4. Resumes after you navigate away

## 8.4 Do Not Click List

VoiceOS maintains a safety list of elements that should never be:
- Clicked automatically
- Included in exploration
- Triggered by voice

Examples:
- "Delete Account"
- "Factory Reset"
- "Uninstall"
- "Logout"

## 8.5 Clearing Data

To clear all learned data:
1. Open AvaLearn
2. Tap menu (⋮)
3. Select "Clear All Data"
4. Confirm deletion

**Warning:** This removes all voice commands and requires re-learning.

---

# Chapter 9: Troubleshooting

## 9.1 "Not connected to JIT service" Error (Red Status)

**Symptom:** AvaLearn shows red status message "Not connected to JIT service" even though VoiceOS is enabled.

**Cause:** The JIT Learning Service failed to start automatically with VoiceOS.

**Fix (Easy):**
1. Go to Settings > Accessibility
2. Find VoiceOS
3. Toggle it **OFF**
4. Wait 3 seconds
5. Toggle it **ON** again
6. Wait 5 seconds for services to start
7. Open AvaLearn - status should now be green "Connected"

**If Still Not Working:**
1. Restart your device
2. Enable VoiceOS accessibility after restart
3. Wait 10 seconds
4. Open AvaLearn

**What Was Fixed (Dec 2025):**
A software update fixed an issue where the JIT service wasn't automatically starting. If you installed VoiceOS before December 18, 2025, update to the latest version for the permanent fix.

## 9.2 "Service Disconnected" Error

**Cause:** VoiceOS accessibility service not running at all.

**Fix:**
1. Go to Settings > Accessibility
2. Find VoiceOS
3. Toggle OFF then ON
4. Return to AvaLearn

## 9.3 Stats Stay at Zero

**Cause:** JIT learning not capturing.

**Possible fixes:**
1. Check learning isn't paused
2. Navigate to a few different apps
3. Wait 5-10 seconds on each screen
4. Restart VoiceOS service

## 9.4 Events Not Appearing (Pro)

**Cause:** Event listener not registered.

**Fix:**
1. Close AvaLearnPro
2. Wait 5 seconds
3. Reopen AvaLearnPro
4. Events should now appear

## 9.5 App Keeps Crashing

**Cause:** Memory or compatibility issue.

**Fix:**
1. Clear AvaLearn cache (Settings > Apps > AvaLearn > Clear Cache)
2. Restart device
3. Reinstall if problem persists

## 9.6 Voice Commands Not Working

**Cause:** Commands not generated yet.

**Fix:**
1. Open the target app
2. Navigate through screens you want to control
3. Wait for JIT to learn (check AvaLearn stats)
4. Try voice commands again

---

# Chapter 10: FAQ

## General

**Q: Do I need AvaLearn for VoiceOS to work?**
A: No. VoiceOS works independently. AvaLearn just shows you what's being learned and lets you control it.

**Q: What's the difference between Lite and Pro?**
A: Lite shows basic stats and pause/resume. Pro adds event logs, element inspector, and developer tools.

**Q: Does AvaLearn use battery?**
A: Minimal. Most processing is done by VoiceOS, not AvaLearn.

## Learning

**Q: Why isn't my app being learned?**
A: Check if it's a system app (some are excluded) or if learning is paused.

**Q: How long does it take to learn an app?**
A: Each screen is learned instantly when you visit it. A full app might take days of normal use.

**Q: Can I manually trigger learning?**
A: Yes, in AvaLearnPro you can start an "exploration" that automatically navigates and learns an app.

## Privacy

**Q: Can someone see what I type?**
A: No. JIT only captures screen structure, not text content.

**Q: Is my data sent anywhere?**
A: No. All data stays on your device.

**Q: Can I use VoiceOS on banking apps?**
A: VoiceOS automatically pauses on sensitive apps. You can manually add apps to the blocklist.

## Technical

**Q: Why do I see "Login Detected"?**
A: VoiceOS detected a password field and paused learning for security.

**Q: What does "screen hash" mean?**
A: It's a unique identifier for a screen layout, used to avoid re-learning the same screen.

**Q: Can I control what voice commands are generated?**
A: Not directly, but you can approve/reject commands in VoiceOS settings.

---

# Appendix: Quick Reference

## Status Icons

| Icon | Meaning |
|------|---------|
| ● (green) | Active/Connected |
| ● (yellow) | Paused/Warning |
| ○ (gray) | Inactive/Disconnected |
| ● (red) | Error |

## Keyboard Shortcuts (AvaLearnPro)

| Shortcut | Action |
|----------|--------|
| R | Refresh element tree |
| P | Pause/Resume learning |
| C | Clear log |
| E | Export data |

## Useful Terms

| Term | Definition |
|------|------------|
| JIT | Just-In-Time learning |
| Screen Hash | Unique identifier for screen layout |
| Element | Interactive UI component |
| AVU | AvaLearn Universal format (export) |

---

**Document Version History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | Claude | Initial version |

---

*For developer documentation, see: VoiceOS-JIT-Developer-Manual*
*For testing procedures, see: VoiceOS-JIT-Testing-Manual*

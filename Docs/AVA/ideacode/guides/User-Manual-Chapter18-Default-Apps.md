# User Manual - Chapter 18: Default App Preferences

## Overview

AVA's Intelligent Resolution System automatically determines which app to use for different tasks like sending emails, text messages, or playing music. This **zero-config** approach means:

- AVA detects your installed apps automatically
- You're asked to choose only once when multiple apps exist
- Your choice is remembered forever
- You can change preferences anytime in Settings

---

## How It Works

### First-Time Usage

When you ask AVA to perform a task that can be done by multiple apps:

1. **AVA scans** your installed apps for the capability
2. **If only one app** - AVA uses it automatically (no question asked)
3. **If multiple apps** - AVA shows a selection screen (one time only)

### Example Scenarios

| Command | Apps Found | AVA Behavior |
|---------|-----------|--------------|
| "Text mom" | Only Messages | Uses Messages directly |
| "Text mom" | Messages + WhatsApp | Shows app selection (once) |
| "Email John" | Gmail + Outlook | Shows app selection (once) |
| "Email John" | Only Gmail | Uses Gmail directly |

---

## App Selection Screen (AVAILABLE NOW!)

When you ask AVA to perform a task with multiple app options (like "Send email to John"), you'll now see a smooth bottom sheet appear:

```
┌─────────────────────────────────┐
│   Choose Email App              │
│   AVA found multiple apps.      │
│   Which would you like to use?  │
│                                 │
│   ┌─────────────────────────┐   │
│   │ 📧 Gmail                │   │
│   │    Recommended          │   │
│   └─────────────────────────┘   │
│                                 │
│   ┌─────────────────────────┐   │
│   │ 📬 Outlook              │   │
│   └─────────────────────────┘   │
│                                 │
│        [Always ask me]          │
└─────────────────────────────────┘
```

**How It Works:**

1. You say: "Send email to John"
2. AVA detects you have Gmail and Outlook installed
3. A bottom sheet slides up showing your options
4. Tap your preferred app (e.g., Gmail)
5. AVA saves your choice and opens Gmail
6. Next time you say "Send email," AVA opens Gmail directly - no prompt!

**Options:**
- **Select an app** - AVA remembers your choice forever
- **"Always ask me"** - AVA will prompt every time (useful if you switch between apps)

---

## Supported Capabilities

AVA can remember your preferred app for:

| Capability | Examples |
|------------|----------|
| **Email** | Gmail, Outlook, Yahoo Mail, Proton Mail |
| **Text Messages** | Messages, WhatsApp, Messenger, Signal |
| **Phone Calls** | Phone, Google Voice, Skype |
| **Music** | Spotify, YouTube Music, Apple Music, Amazon Music |
| **Video** | YouTube, Netflix, Prime Video |
| **Maps** | Google Maps, Apple Maps, Waze |
| **Calendar** | Google Calendar, Outlook, Apple Calendar |
| **Notes** | Google Keep, Apple Notes, Evernote, OneNote |
| **Browser** | Chrome, Safari, Firefox, Brave |
| **Ride Sharing** | Uber, Lyft |
| **Food Delivery** | Uber Eats, DoorDash, Grubhub |

---

## Managing Preferences

### View Current Preferences

1. Open AVA
2. Go to **Settings** (gear icon)
3. Scroll to **"Default Apps"** section

### Reset a Single Preference

To have AVA ask you again for one capability:

1. Go to **Settings** > **Default Apps**
2. Find the capability (e.g., "Email - Gmail")
3. Tap the **X** button to clear
4. Next time you use that capability, AVA will ask again

### Reset All Preferences

To clear all saved app preferences:

1. Go to **Settings** > **Default Apps**
2. Tap **"Reset All App Preferences"**
3. AVA will ask for each capability again when needed

---

## Privacy & Storage

| Aspect | Details |
|--------|---------|
| **Storage** | Preferences stored locally on device |
| **Cloud Sync** | No - preferences are device-only |
| **Data Shared** | None - AVA doesn't share your app choices |
| **Encryption** | Database encrypted at rest |

---

## Troubleshooting

### "AVA uses the wrong app"

1. Go to **Settings** > **Default Apps**
2. Clear the preference for that capability
3. Use the command again to re-select

### "AVA keeps asking me to choose"

This happens if you selected "Always ask me". To fix:

1. When prompted, select an app
2. Don't tap "Always ask me"
3. Your choice will be saved

### "My preferred app isn't showing"

AVA only shows apps that can handle the capability. If your app isn't listed:

1. Make sure the app is installed
2. Make sure the app supports the action (e.g., can send emails)
3. Try reinstalling the app

### "Preference was lost"

Preferences may reset if:

- App data was cleared
- AVA was reinstalled
- Database was corrupted

Simply use the command again and re-select your preferred app.

---

## Tips

| Tip | Benefit |
|-----|---------|
| Let AVA auto-select | When only one app exists, AVA skips the prompt |
| Choose once | Saves time on future commands |
| Use Settings to change | Don't reinstall AVA to reset preferences |

---

## Related Chapters

- **Chapter 11**: Voice Commands - Full command reference
- **Chapter 17**: Smart Learning - How AVA learns from you
- **Chapter 14**: Privacy & Security - Data protection details

---

## Author

Manoj Jhawar

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-06 | Updated with UI implementation details (C4 fix - bottom sheet now functional) |
| 1.0 | 2025-12-05 | Initial release with Phase 1 implementation |

---

**Related:** See [Chapter 71 - Intelligent Resolution System](../Developer-Manual-Chapter71-Intelligent-Resolution-System.md) for technical implementation details.

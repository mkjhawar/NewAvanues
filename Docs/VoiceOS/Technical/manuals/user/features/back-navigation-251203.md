# Back Navigation - User Guide

**Feature:** Back Navigation
**Version:** 1.0
**Platform:** Android
**Last Updated:** 2025-12-03
**Applies To:** VoiceOS Core, All Activities

---

## What is Back Navigation?

Back navigation allows you to return to the previous screen or exit from VoiceOS settings and configuration screens. This feature ensures you're never trapped in a screen and can always navigate back using either visual buttons or voice commands.

---

## When to Use Back Navigation

Use back navigation when you want to:

- Return to the previous screen
- Exit from settings screens
- Cancel configuration wizards
- Navigate out of help screens
- Return to the main VoiceOS interface

---

## How to Use Back Navigation

### Method 1: Visual Back Button

**Location:** Top-left corner of every screen

**Steps:**
1. Look for the ← (arrow) icon in the top-left corner
2. Tap the back arrow icon
3. You'll immediately return to the previous screen

**Example:**
```
┌────────────────────────────────┐
│ ←  VoiceOS Settings           │  ← Tap this arrow
├────────────────────────────────┤
│                                │
│  Settings content here...      │
│                                │
└────────────────────────────────┘
```

### Method 2: Hardware Back Button

**Location:** Your Android device's back button (gesture or hardware)

**Steps:**
1. Use your device's back gesture (swipe from left edge) OR
2. Press your device's hardware back button
3. You'll immediately return to the previous screen

**Supported Devices:**
- ✅ All Android phones with gesture navigation
- ✅ All Android phones with hardware back button
- ✅ RealWear HMT devices (voice command: "Navigate Back")

### Method 3: Voice Commands (Future)

**Status:** Coming soon in VoiceOS v5.0

**Planned Commands:**
- "Go back"
- "Previous screen"
- "Return"
- "Exit settings"

---

## Quick Start

**To exit from VoiceOS Settings:**
1. Tap the ← arrow in top-left corner
2. You return to the main VoiceOS screen

**To cancel setup wizard:**
1. Open VoiceOS Setup (Onboarding)
2. Tap the ← arrow to exit without completing
3. Setup can be resumed later

---

## Available in These Screens

Back navigation is available in:

| Screen | Back Arrow | Hardware Back | What Happens |
|--------|-----------|---------------|--------------|
| **Settings** | ✅ | ✅ | Returns to main screen |
| **Onboarding** | ✅ | ✅ | Exits setup wizard |
| **Module Configuration** | ✅ | ✅ | Discards unsaved changes |
| **Voice Training** | ✅ | ✅ | Stops training session |
| **Diagnostics** | ✅ | ✅ | Returns to main screen |
| **Help & Support** | ✅ | ✅ | Closes help screen |

---

## Settings

### Back Button Position

**Default:** Top-left corner (follows Material Design 3)
**Cannot be changed:** Position is fixed for consistency

### Back Button Color

**Default:** Matches your VoiceOS theme
**Theme:** Ocean (glassmorphic blue-green)
**High Contrast Mode:** Dark arrow on light background

### Confirmation Dialogs

**Module Configuration:** Shows "Discard changes?" if you modified settings
**Voice Training:** Shows "Stop training session?" if training in progress
**Other screens:** No confirmation, immediate back navigation

---

## Tips and Tricks

### Tip 1: Double-Check Before Exiting Training
If you're in the middle of voice training, make sure to complete the session before using back navigation. Your training progress will be lost if you exit early.

### Tip 2: Use Hardware Back for Speed
Hardware back button (or gesture) is faster than tapping the visual arrow. Muscle memory will make this your preferred method.

### Tip 3: Back Arrow Always Visible
Unlike some apps that hide the back button, VoiceOS keeps it visible at all times for accessibility.

### Tip 4: RealWear Voice Command
On RealWear devices, say "Navigate Back" instead of tapping or gesturing.

---

## Troubleshooting

### Q: I tapped the back arrow but nothing happened
**A:** This is very rare. Try these steps:
1. Tap the arrow again (might have missed the touch target)
2. Use hardware back button instead
3. Restart VoiceOS if problem persists

### Q: Back button is not visible
**A:** Check these:
1. Make sure you're not on the main VoiceOS screen (no back button there)
2. Check if screen is scrolled down (scroll up to see top bar)
3. Restart the app if top bar is missing

### Q: Hardware back button doesn't work
**A:**
1. This is a device issue, not VoiceOS issue
2. Try using the visual back arrow instead
3. Check your Android system settings for gesture navigation
4. Some devices disable back button - check manufacturer settings

### Q: Can I customize back button behavior?
**A:** No, back navigation behavior is standardized for consistency and accessibility. All screens behave the same way.

### Q: Will I lose my data when I go back?
**A:** Depends on the screen:
- **Settings:** Auto-saved when you change them
- **Module Config:** Shows confirmation dialog if unsaved changes
- **Voice Training:** Training progress is lost (confirmation shown)
- **Onboarding:** Can resume later from where you left off

---

## Related Features

- **Voice Commands** - Control VoiceOS with your voice
- **Gesture Navigation** - Android system gestures
- **Accessibility Service** - VoiceOS screen reader integration
- **Settings Persistence** - Auto-save your preferences

---

## Accessibility Features

### For Users with Motor Impairments
- Large touch target (48x48dp minimum)
- Works with switch access
- Works with voice control (RealWear)
- Hardware back button support

### For Users with Visual Impairments
- TalkBack announces "Back button"
- High contrast mode increases visibility
- Icon has clear directional arrow
- Consistent position (always top-left)

### For Users with Cognitive Disabilities
- Predictable behavior (always goes back)
- Visual arrow clearly indicates direction
- Confirmation dialogs prevent accidental data loss
- Consistent across all screens

---

## Version History

### v1.0 (2025-12-03)
- Initial release
- Added back arrow to all VoiceOS screens
- Hardware back button support
- Material Design 3 styling
- Accessibility optimizations

### Planned for v1.1
- Voice command support ("Go back")
- Customizable confirmation dialogs
- Breadcrumb navigation
- Long-press for navigation history

---

**Need more help?** See:
- [Developer Manual: Back Navigation Implementation](/docs/manuals/developer/ui/back-navigation-251203.md)
- [VoiceOS Settings Guide](/docs/manuals/user/features/settings.md)
- [Accessibility Guide](/docs/manuals/user/accessibility.md)

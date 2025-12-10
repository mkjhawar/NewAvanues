# LocalizationManager User Manual

**Module:** LocalizationManager
**Version:** 2.0.0
**Last Updated:** 2025-12-01

---

## Table of Contents

1. [Overview](#overview)
2. [Accessing Settings](#accessing-settings)
3. [Available Settings](#available-settings)
4. [Debounce Duration](#debounce-duration)
5. [Statistics Display](#statistics-display)
6. [Language Animation](#language-animation)
7. [Detail Level](#detail-level)
8. [Resetting Preferences](#resetting-preferences)
9. [Troubleshooting](#troubleshooting)

---

## Overview

LocalizationManager provides settings to customize how VOS4 displays messages and information:

- **Message Timing**: Control how long messages appear
- **Statistics**: Configure automatic statistics display
- **Animations**: Enable/disable language animations
- **Detail Preferences**: Choose your preferred information density

---

## Accessing Settings

### Via Voice Command

Say: **"Open localization settings"** or **"Settings localization"**

### Via App

1. Open VOS4 app
2. Tap Settings icon
3. Select "Localization & Display"

---

## Available Settings

| Setting | Options | Default |
|---------|---------|---------|
| Message Debounce | Instant to Very Slow | Normal (2 sec) |
| Statistics Auto-Show | On/Off | Off |
| Language Animation | On/Off | On |
| Detail Level | Minimal/Standard/Comprehensive | Standard |

---

## Debounce Duration

Controls how long messages stay on screen before being replaced.

### Options

| Option | Duration | Best For |
|--------|----------|----------|
| **Instant** | No delay | Fast readers, quick interactions |
| **Fast** | 1 second | Experienced users |
| **Normal** | 2 seconds | Most users (default) |
| **Slow** | 3 seconds | Users who prefer more time |
| **Very Slow** | 5 seconds | Users who need extra reading time |

### How to Change

**Voice:** "Set message delay to slow" or "Make messages slower"

**Touch:**
1. Open Localization Settings
2. Tap "Message Display Duration"
3. Select your preferred option

---

## Statistics Display

Controls whether usage statistics are shown automatically.

### Options

| Option | Behavior |
|--------|----------|
| **Off** (default) | Statistics only shown when requested |
| **On** | Statistics shown automatically after commands |

### How to Change

**Voice:** "Turn on auto statistics" or "Show statistics automatically"

**Touch:**
1. Open Localization Settings
2. Toggle "Auto-Show Statistics"

---

## Language Animation

Controls animations when language or locale changes.

### Options

| Option | Behavior |
|--------|----------|
| **On** (default) | Smooth animations on language changes |
| **Off** | Instant transitions, no animations |

### When to Disable

- If you experience performance issues
- If you prefer faster transitions
- For accessibility reasons

### How to Change

**Voice:** "Disable language animations" or "Turn off animations"

**Touch:**
1. Open Localization Settings
2. Toggle "Language Animation"

---

## Detail Level

Controls how much information is displayed in messages and feedback.

### Options

| Level | Description | Example Feedback |
|-------|-------------|------------------|
| **Minimal** | Brief, essential info only | "Done" |
| **Standard** | Balanced detail (default) | "Navigation: Went back" |
| **Comprehensive** | Full details with context | "Navigation: Successfully navigated back to previous screen (Chrome)" |

### How to Change

**Voice:** "Set detail level to comprehensive" or "Show more details"

**Touch:**
1. Open Localization Settings
2. Tap "Detail Level"
3. Select your preferred level

---

## Resetting Preferences

To reset all localization settings to defaults:

### Voice Command

Say: **"Reset localization settings"** or **"Reset display preferences"**

### Touch

1. Open Localization Settings
2. Scroll to bottom
3. Tap "Reset to Defaults"
4. Confirm when prompted

### Default Values

After reset:
- Message Debounce: Normal (2 seconds)
- Statistics Auto-Show: Off
- Language Animation: On
- Detail Level: Standard

---

## Troubleshooting

### Messages Disappear Too Quickly

**Solution:** Increase debounce duration
- Go to Settings > Localization
- Change "Message Display Duration" to Slow or Very Slow

### Messages Stay Too Long

**Solution:** Decrease debounce duration
- Go to Settings > Localization
- Change "Message Display Duration" to Fast or Instant

### Animations Causing Lag

**Solution:** Disable animations
- Go to Settings > Localization
- Turn off "Language Animation"

### Settings Not Saving

**Possible Causes:**
1. App was force-stopped
2. Storage permission issues
3. Database corruption

**Solutions:**
1. Ensure VOS4 has storage permissions
2. Try "Reset to Defaults"
3. If persists, clear app data and reconfigure

### Settings Not Applying

**Solution:** Restart VOS4 service
- Say: "Restart VoiceOS" or toggle accessibility service

---

## Voice Command Reference

| Command | Action |
|---------|--------|
| "Open localization settings" | Opens settings screen |
| "Set message delay to [option]" | Changes debounce duration |
| "Turn on/off auto statistics" | Toggles statistics display |
| "Enable/disable animations" | Toggles language animations |
| "Set detail level to [level]" | Changes detail level |
| "Reset localization settings" | Resets all to defaults |

---

## Related Documentation

- [Developer Manual](./developer-manual.md) - Technical documentation
- [VOS4 User Guide](../../user-guide/) - General VOS4 help

---

**Last Updated:** 2025-12-01
**Version:** 2.0.0

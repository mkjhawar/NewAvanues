# LearnApp Floating Progress Widget - User Guide

**Document:** VoiceOS-learnapp-floating-progress-widget-user-guide-50712-V1.md
**Date:** 2025-12-07
**Version:** 1.0
**Audience:** End Users

---

## Overview

The **Floating Progress Widget** is a new feature in VoiceOS LearnApp that displays exploration progress in a moveable, semi-transparent overlay. This widget allows you to monitor progress, pause/resume exploration, and stop the learning process at any time without losing your progress.

---

## Features

| Feature | Description |
|---------|-------------|
| **Draggable** | Move the widget anywhere on screen by dragging |
| **Semi-transparent** | See through the widget to the content behind |
| **Always visible** | Floats above all app content |
| **Progress display** | Shows percentage, status, and element counts |
| **Pause/Resume** | Temporarily pause and resume exploration |
| **STOP button** | Immediately stop exploration and save progress |

---

## Widget Components

```
┌─────────────────────┐
│     ═══════        │  ← Drag handle
│                     │
│  ⭕ 45%             │  ← Progress indicator
│                     │
│  Learning Teams...  │  ← Status text
│  12 screens, 145... │  ← Stats text
│                     │
│   [⏸]    [⏹]       │  ← Pause/Stop buttons
└─────────────────────┘
```

---

## How to Use

### Starting Exploration

1. Open VoiceOS settings
2. Navigate to **LearnApp** section
3. Select an app to learn
4. The floating progress widget appears automatically

### Moving the Widget

1. **Touch and hold** anywhere on the widget (or the drag handle at the top)
2. **Drag** to your desired position
3. **Release** to place the widget

The widget will stay in your chosen position during the current exploration session.

### Understanding Progress

| Display | Meaning |
|---------|---------|
| **Percentage (45%)** | Overall exploration completeness |
| **Status text** | Current action (e.g., "Learning Instagram...") |
| **Stats** | Number of screens explored and elements found |

### Pausing Exploration

1. Tap the **Pause button** (⏸) to temporarily stop
2. The progress bar turns **amber/yellow** when paused
3. Interact with the app freely while paused
4. Tap **Resume** (▶) to continue exploration

**Use cases for pausing:**
- Need to log in or handle authentication
- Want to navigate to a specific area first
- Need to dismiss unexpected dialogs

### Stopping Exploration

1. Tap the **STOP button** (⏹) to immediately end exploration
2. Progress is saved automatically
3. The widget disappears
4. A summary notification shows your results

**When to stop:**
- You've explored enough of the app
- The app is behaving unexpectedly
- You need to use the device for something else

---

## Widget States

### Running (Default)

- Progress bar: **Green**
- Button: **Pause** (⏸)
- Widget: Actively exploring

### Paused

- Progress bar: **Amber/Yellow**
- Button: **Resume** (▶)
- Widget: Waiting for user to resume

### Completing

- Progress bar reaches 100%
- Status: "Learning complete"
- Widget automatically dismisses

---

## Tips

### Best Practices

1. **Don't block critical UI**: Move the widget if it covers important buttons
2. **Use pause for login screens**: Pause when you need to enter credentials
3. **Monitor progress**: Check stats to see how much has been explored
4. **Stop early if stuck**: If progress isn't changing, the app may have issues

### Troubleshooting

| Issue | Solution |
|-------|----------|
| Widget not appearing | Ensure VoiceOS has accessibility permissions |
| Can't drag widget | Touch and hold for 1 second before dragging |
| Progress stuck | Tap STOP and try again, or check for app issues |
| Widget disappeared | Exploration completed or an error occurred |

---

## Accessibility

The floating progress widget is designed with accessibility in mind:

- **High contrast**: White text on dark semi-transparent background
- **Large touch targets**: Buttons are 44dp for easy tapping
- **Clear status**: Progress percentage and status text are prominently displayed
- **Non-blocking**: Draggable so it never permanently blocks important content

---

## Technical Details

### For Developers

| Property | Value |
|----------|-------|
| Window type | `TYPE_ACCESSIBILITY_OVERLAY` |
| Background | `#BF1C1C1E` (75% opacity dark) |
| Widget opacity | 92% |
| Width | 180dp |
| Corner radius | 16dp |
| Elevation | 16dp |

### Layout Files

- Widget: `floating_progress_widget.xml`
- Drag handle background: `learnapp_drag_handle_bg.xml`

### Class

- `FloatingProgressWidget.kt` in `com.augmentalis.voiceoscore.learnapp.ui`

---

## Related Documentation

- [LearnApp Timeout Protection Fix](../fixes/VoiceOS-learnapp-infinite-scroll-timeout-fix-50712-V1.md)
- [LearnApp Developer Settings](VoiceOS-learnapp-developer-settings-expansion-plan-50512-V1.md)
- [LearnApp Hybrid C-Lite Architecture](VoiceOS-learnapp-hybrid-c-lite-spec-50412-V1.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-07 | Initial release with draggable widget, pause/stop controls |

---

**Author:** VOS4 Development Team
**Last Updated:** 2025-12-07

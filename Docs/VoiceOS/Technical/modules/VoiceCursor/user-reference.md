# VoiceCursor User Reference

**Last Updated**: 2025-10-23 21:45 PDT
**Module**: VoiceCursor
**For**: End Users

---

## What is VoiceCursor?

VoiceCursor provides an accessible on-screen cursor that you control with head movement and voice commands. Designed for hands-free interaction with Android devices, it enables users with mobility impairments to navigate apps, click buttons, and interact with on-screen elements without touching the screen.

The cursor tracks your head movements using the device's built-in sensors (IMU/gyroscope). Moving your head moves the cursor on screen. Voice commands trigger clicks, scrolling, and other actions at the cursor position.

---

## Voice Commands

| Command | Action |
|---------|--------|
| **"click"** / **"click here"** | Single tap at cursor position |
| **"double click"** | Double tap at cursor position |
| **"long press"** | Long press at cursor position |
| **"center cursor"** / **"center"** | Move cursor to screen center |
| **"show cursor"** | Display the cursor overlay |
| **"hide cursor"** | Hide the cursor overlay |
| **"cursor up [distance]"** | Move cursor up (default 50 pixels) |
| **"cursor down [distance]"** | Move cursor down |
| **"cursor left [distance]"** | Move cursor left |
| **"cursor right [distance]"** | Move cursor right |
| **"cursor menu"** | Show cursor action menu |
| **"cursor settings"** | Open cursor settings |
| **"show coordinates"** | Display cursor X/Y position |
| **"hide coordinates"** | Hide coordinate display |
| **"cursor hand"** | Switch to hand cursor style |
| **"cursor normal"** | Switch to round cursor style |
| **"voice cursor enable"** | Turn on VoiceCursor system |
| **"voice cursor disable"** | Turn off VoiceCursor system |
| **"voice cursor calibrate"** | Calibrate head tracking |

---

## Cursor Modes

| Mode | Description |
|------|-------------|
| **Normal** | Round crosshair cursor with precise targeting |
| **Hand** | Hand-shaped cursor for visual preference |
| **Custom** | User-defined custom cursor appearance |

---

## Configuration Options

**Access Settings**: Say **"cursor settings"** or navigate to VoiceOS Settings → VoiceCursor

| Setting | Description | Range/Options |
|---------|-------------|---------------|
| **Cursor Type** | Visual style of cursor | Normal, Hand, Custom |
| **Cursor Size** | Size of cursor graphic | 24-96 pixels (default: 48) |
| **Cursor Color** | Color of cursor | RGB color picker |
| **Movement Speed** | Sensitivity of head tracking | 1-10 (default: 8) |
| **Motion Sensitivity** | Responsiveness to head movement | 0.1-1.0 (default: 0.7) |
| **Jitter Filter** | Reduce cursor shake | On/Off (default: On) |
| **Filter Strength** | Smoothing intensity | Low/Medium/High (default: Medium) |
| **Gaze Click** | Auto-click when dwelling | On/Off (default: Off) |
| **Gaze Delay** | Dwell time for auto-click | 500-3000ms (default: 1500ms) |
| **Show Coordinates** | Display cursor position | On/Off (default: Off) |

---

## Usage Tips

1. **Initial Calibration**: Say "voice cursor calibrate" when first using VoiceCursor. Hold device steady and look straight ahead.

2. **Movement Sensitivity**: Start with default speed (8). Decrease for finer control, increase for faster movement.

3. **Reduce Jitter**: Enable jitter filter and set to High if cursor shakes. Lower motion sensitivity if needed.

4. **Gaze Click**: Enable for hands-free clicking. Look at target and hold gaze for delay duration (default 1.5 seconds).

5. **Quick Center**: Say "center" to quickly reset cursor to screen center if lost or at edge.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **Cursor won't appear** | Say "show cursor". Check VoiceOS accessibility service is enabled. |
| **Cursor too shaky/jittery** | Enable jitter filter. Increase filter strength to High. Decrease motion sensitivity. |
| **Cursor moves too fast** | Lower movement speed setting (try 5-6). Reduce motion sensitivity. |
| **Cursor moves too slow** | Increase movement speed setting (try 9-10). Increase motion sensitivity. |
| **Cursor stuck at edge** | Say "center cursor". Recalibrate with "voice cursor calibrate". |
| **Gaze click not working** | Check gaze click is enabled. Ensure gaze delay is appropriate (try 1500ms). Hold head still on target. |
| **Commands not recognized** | Speak clearly and at normal pace. Check microphone permissions. Verify VoiceRecognition module is active. |
| **Cursor disappears** | Say "show cursor". Check battery saver mode isn't limiting background services. |
| **Tracking inaccurate** | Say "voice cursor calibrate". Ensure device sensors are working (test with other apps). |
| **Settings not saving** | Verify storage permissions. Check VoiceOS has sufficient storage space. |

---

## Accessibility Features

- **Hands-Free Operation**: Complete device control without touch
- **Gaze Dwelling**: Auto-click by looking at target
- **Voice Feedback**: Audio confirmation of actions (when enabled)
- **High Contrast**: Customizable cursor colors for visibility
- **Adjustable Speed**: Fine-tune for individual motor control
- **Screen Reader Compatible**: Works with TalkBack and other screen readers

---

## Battery & Performance

- **Low Power Mode**: VoiceCursor uses minimal battery when idle
- **Sensor Optimization**: IMU tracking pauses when cursor hidden
- **Background Service**: Lightweight accessibility service
- **Typical Battery Impact**: <5% additional battery drain per hour of active use

---

## Privacy & Permissions

**Required Permissions**:
- **Accessibility Service**: For gesture dispatch (click, scroll, etc.)
- **Sensors**: For head tracking via IMU/gyroscope
- **Microphone**: For voice command recognition
- **Overlay Permission**: To display cursor on screen

**Privacy Notes**:
- Sensor data processed locally on device
- No head tracking data transmitted to servers
- Voice commands handled by on-device speech recognition
- No personal data collection

---

## Support

**For Issues**:
- Check [Troubleshooting](#troubleshooting) section above
- Review VoiceOS logs: Settings → VoiceOS → Logs
- Contact VOS4 support with log files if issue persists

**For Feature Requests**:
- Submit via VoiceOS feedback system
- Visit VOS4 GitHub repository

---

## Quick Links
- [Developer Reference](./developer-reference.md)
- [VoiceOS Main Settings](../VoiceOSCore/user-guide.md)
- [Voice Commands Reference](../CommandManager/voice-commands.md)

---

**Version**: 1.0.0
**Created**: 2025-10-23 21:45 PDT
**Author**: VOS4 Documentation Specialist

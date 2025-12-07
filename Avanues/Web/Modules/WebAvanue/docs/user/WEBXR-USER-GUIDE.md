# WebXR User Guide - Using AR and VR in WebAvanue

**Version:** 1.1
**Date:** 2025-11-23
**Status:** Phase 5 Partial (73% Complete - Core Features Ready)

---

## What's New in v1.1

**Latest Updates (Phase 4 & 5):**
- ✅ **Integrated XR UI**: Session indicators and performance warnings now appear automatically during AR/VR sessions
- ✅ **WebXR Settings**: Easy access from Settings → Advanced → WebXR Settings
- ✅ **Camera Permissions**: Improved permission handling for AR experiences
- ✅ **Cross-Platform Ready**: Foundation for future iOS/Desktop support

**Implementation Status:**
- 73% Complete (4.4/6 phases)
- Core features fully functional
- Advanced features coming in future updates

---

## What is WebXR?

WebXR lets you experience Augmented Reality (AR) and Virtual Reality (VR) directly in your browser - no apps to install! With WebXR in WebAvanue, you can:

- 🌍 **AR (Augmented Reality):** See virtual objects in your real environment through your camera
- 🎮 **VR (Virtual Reality):** Step into immersive 360° virtual worlds
- 🎯 **Interactive:** Place, move, and interact with 3D content
- 🔒 **Private:** All processing happens on your device

---

## Getting Started

### Requirements

**For AR Experiences:**
- Android device with camera
- Camera permission granted to WebAvanue
- WiFi or mobile data connection (optional: can be restricted to WiFi only)

**For VR Experiences:**
- Any Android device
- Motion sensors (built into most phones)
- Optional: VR headset for full immersion

### Enabling WebXR

WebXR is enabled by default, but you can configure it:

1. Open WebAvanue browser
2. Tap **Menu** (three dots) → **Settings**
3. Scroll to **WebXR Settings**
4. Toggle options:
   - **Enable WebXR** - Master on/off switch
   - **Augmented Reality (AR)** - Camera-based AR
   - **Virtual Reality (VR)** - 360° immersive VR

---

## Using AR (Augmented Reality)

### Starting an AR Experience

1. **Visit an AR-enabled website**
   - Example: https://immersive-web.github.io/webxr-samples/
   - Look for "Enter AR" or "Start AR" buttons

2. **Grant camera permission** (first time only)
   - Tap **"Allow"** when prompted
   - Read the explanation of why camera access is needed
   - WebAvanue explains: "Camera only used during AR sessions"

3. **Point your camera**
   - Move your device to scan your surroundings
   - AR content will overlay on your real-world view

4. **Interact with AR objects**
   - Tap to place objects
   - Pinch to resize
   - Drag to move objects around

### AR Tips

✅ **Good lighting** - AR works best in well-lit environments
✅ **Flat surfaces** - Place objects on tables, floors, or walls
✅ **Steady hand** - Hold device stable for best tracking
✅ **Clear view** - Avoid pointing at reflective or plain surfaces

⚠️ **Battery Usage** - AR is power-intensive (~20-30%/hour)
⚠️ **Heat** - Device may warm up during extended AR sessions
⚠️ **Data** - Some AR experiences download 3D models

---

## Using VR (Virtual Reality)

### Starting a VR Experience

1. **Visit a VR-enabled website**
   - Example: https://immersive-web.github.io/webxr-samples/immersive-vr-session.html
   - Look for "Enter VR" button

2. **Start VR mode**
   - Tap "Enter VR"
   - Your screen will switch to split-screen (for VR headsets) or full 360° view

3. **Look around**
   - Move your device to look in different directions
   - The virtual world follows your device rotation
   - If using a VR headset, turn your head naturally

4. **Exit VR**
   - Tap the screen or press back button
   - VR session ends, returns to normal browsing

### VR Tips

✅ **Rotate 360°** - Look all around, including behind you
✅ **Sit in swivel chair** - For comfortable full rotation
✅ **Use headphones** - For immersive spatial audio
✅ **VR headset** - Insert phone into Google Cardboard or similar for full immersion

⚠️ **Motion sickness** - Take breaks if you feel dizzy
⚠️ **Safe space** - Ensure clear area around you
⚠️ **Battery** - VR uses less power than AR (~15-25%/hour)

---

## Performance & Settings

### Performance Indicators

**FPS Indicator** (optional):
- Shows current frame rate (e.g., "60 FPS")
- Green: Good performance (≥55fps)
- Orange: Moderate performance (45-54fps)
- Red: Low performance (<45fps)

**Session Indicator:**
When an XR session is active, you'll see a status bar showing:
- Session mode (AR or VR badge)
- Current performance metrics
- Battery level
- Temperature (if elevated)
- Session duration

### Performance Warnings

WebAvanue monitors your device and shows warnings when needed:

**Low Performance:**
- "Performance degraded"
- Recommendation: "Lower performance mode recommended"
- Action: Go to Settings → WebXR → Performance Mode → Battery Saver

**Low Battery:**
- "Battery below 20%"
- Recommendation: "Consider charging soon"
- Action: Charge device or exit XR session

**Overheating:**
- "Device temperature elevated"
- Recommendation: "Take a break to cool down"
- Action: Exit XR session and let device cool

**Critical Warnings:**
For severe issues (battery ≤5%, critical temperature), WebAvanue will show a blocking dialog:
- "Critical Battery Level" or "Critical Temperature"
- Recommendation: "Exit XR session immediately"
- Action: Tap "Exit XR Session" to prevent shutdown/damage

### Auto-Pause Protection

To protect your device, XR sessions automatically pause after:
- **Default:** 30 minutes of continuous use
- **Configurable:** 5-60 minutes (Settings → WebXR → Auto-Pause Timeout)

When auto-paused:
- Camera stops (AR only)
- Performance monitoring pauses
- Resume by returning to the XR webpage

---

## WebXR Settings Reference

### Master Controls

**Enable WebXR**
- Turns all WebXR features on/off
- When off, "Enter AR/VR" buttons won't work
- Default: ON

**Augmented Reality (AR)**
- Enables camera-based AR experiences
- Requires camera permission
- Default: ON

**Virtual Reality (VR)**
- Enables 360° immersive VR experiences
- No special permissions needed
- Default: ON

### Performance Settings

**Performance Mode**

Choose based on your device and needs:

1. **HIGH QUALITY**
   - 90fps target
   - Maximum visual quality
   - Higher battery drain
   - Recommended: Flagship devices (Snapdragon 8 Gen+, Pixel 7+, etc.)

2. **BALANCED** ⭐ (Default)
   - 60fps target
   - Good balance of quality and battery life
   - Recommended: Most users

3. **BATTERY SAVER**
   - 45fps target
   - Extended battery life
   - Recommended: Older devices, long sessions

**Auto-Pause Timeout**
- How long before XR session auto-pauses
- Range: 5-60 minutes
- Default: 30 minutes
- Recommendation:
  - Short sessions (games): 15 minutes
  - Long viewing: 45-60 minutes

**Show FPS Indicator**
- Displays current frame rate during XR sessions
- Useful for troubleshooting performance
- Minimal overhead
- Default: OFF

### Data Usage

**WiFi Only Mode**
- Restricts XR sessions to WiFi networks only
- Prevents mobile data usage
- Useful if you have limited data plan
- Default: OFF

**Data Usage Estimates:**
- AR experiences: 5-50 MB (depends on 3D models)
- VR experiences: 10-100 MB (depends on environments)
- Streaming VR: 100-500 MB/hour

---

## Permissions

### Camera Permission (AR Only)

**Why needed:**
- AR overlays virtual objects on your camera view
- Real-time camera feed is required

**Privacy:**
- Camera data stays on your device
- Not uploaded to servers
- Only active during AR sessions
- Automatically stops when you exit AR

**Granting Permission:**

**First Time:**
1. Visit AR website → Tap "Enter AR"
2. WebAvanue explains why camera is needed
3. Tap **"Allow"** in the dialog
4. Android asks for permission → Tap **"Allow"**

**If Denied:**
1. You'll see "Camera permission required"
2. Tap **"Open Settings"**
3. Find WebAvanue → Permissions → Camera → Allow

**Revoking Permission:**
Settings → Apps → WebAvanue → Permissions → Camera → Deny

### Motion Sensors (VR)

**Auto-granted** - No permission required
- Gyroscope, accelerometer, magnetometer
- Built into Android system
- Used for head tracking

---

## Troubleshooting

### "Enter AR/VR" Button Doesn't Work

**Check WebXR is enabled:**
1. Settings → WebXR Settings
2. Ensure "Enable WebXR" is ON
3. Ensure "AR" or "VR" toggle is ON

**Check permissions (AR):**
1. Settings → Apps → WebAvanue → Permissions
2. Ensure Camera is allowed

**Check device compatibility:**
- AR requires camera
- VR requires motion sensors
- Some very old devices may not support WebXR

### "Performance is Poor / Laggy"

**Lower performance mode:**
1. Settings → WebXR → Performance Mode
2. Switch to BALANCED or BATTERY_SAVER

**Close other apps:**
- Free up RAM and CPU
- Restart WebAvanue if needed

**Check device temperature:**
- If hot, device may be thermal throttling
- Let cool down before resuming

### "Battery Drains Too Fast"

**Use Battery Saver mode:**
1. Settings → WebXR → Performance Mode → Battery Saver

**Reduce session length:**
1. Settings → WebXR → Auto-Pause Timeout → 15 minutes

**Enable WiFi-only:**
1. Settings → WebXR → WiFi Only → ON
2. Reduces power from cellular radio

**Charge while using:**
- AR/VR can drain faster than charging
- Use original charger for best results

### "Device Gets Too Hot"

**Take breaks:**
- Exit XR session every 15-30 minutes
- Let device cool for 5-10 minutes

**Improve ventilation:**
- Remove phone case
- Use in cooler environment
- Avoid direct sunlight

**Lower performance:**
- Settings → WebXR → Battery Saver mode
- Reduces GPU load

### "Camera Permission Denied Permanently"

**Re-enable in Android Settings:**
1. Tap "Open Settings" in WebAvanue dialog
2. OR: Settings → Apps → WebAvanue → Permissions
3. Find Camera → Allow
4. Return to WebAvanue
5. Refresh page and try "Enter AR" again

---

## Safety & Best Practices

### Physical Safety

⚠️ **Clear your space** before starting XR
- Remove obstacles, fragile items
- Ensure 2-meter clearance around you
- Avoid stairs, ledges, pools

⚠️ **Stay aware of surroundings** during AR
- Don't walk while in AR mode
- Watch for people, pets, obstacles
- Use AR while seated/stationary when possible

⚠️ **Use VR in safe location**
- Sit in swivel chair or clear floor space
- Don't use VR while walking/driving
- Have someone nearby to assist if needed

### Health & Comfort

⚠️ **Take regular breaks**
- Every 15-30 minutes
- Rest eyes, stretch, hydrate
- Stop immediately if dizzy/nauseous

⚠️ **Motion sickness prevention**
- Start with short sessions (5-10 minutes)
- Gradually increase duration
- Stop if you feel unwell
- Ginger candy or anti-nausea wristbands may help

⚠️ **Eye strain prevention**
- Adjust screen brightness
- Use in well-lit room (AR)
- Blink frequently
- Follow 20-20-20 rule: Every 20 minutes, look at something 20 feet away for 20 seconds

### Device Safety

⚠️ **Heat management**
- Don't use if device is already hot
- Monitor temperature warnings
- Never cover device ventilation

⚠️ **Battery management**
- Don't use with critically low battery
- Charge with original charger
- Avoid overheating while charging

---

## Privacy & Data

### What Data is Collected?

**Local Only (Never Leaves Device):**
- Camera feed (AR only)
- Motion sensor data
- Performance metrics
- Session duration

**Website-Specific:**
- Websites you visit may collect their own data
- Check each website's privacy policy
- WebAvanue doesn't share your camera/sensor data with websites beyond standard WebXR API

### Controlling Your Data

**Camera Access:**
- Only active during AR sessions
- Automatically stops when session ends
- Revoke permission anytime in Settings

**Performance Data:**
- Stays on your device
- Used only to show warnings
- Not uploaded to servers

---

## FAQ

**Q: Do I need special hardware for WebXR?**
A: No! Any modern Android phone works. VR headsets are optional for VR.

**Q: Does WebXR work offline?**
A: Some experiences work offline if cached, but most require internet to load 3D content.

**Q: Can I use WebXR on other browsers?**
A: Yes, WebXR is a web standard. Chrome, Firefox, Edge, and Samsung Internet support it.

**Q: Is WebXR safe for kids?**
A: Check age ratings on XR websites. Parental guidance recommended for VR (motion sickness risk).

**Q: How much data does WebXR use?**
A: Varies widely. Simple AR: ~5-10 MB. Complex VR: 50-100+ MB. Use WiFi-only mode if concerned.

**Q: Why does AR need camera permission?**
A: AR overlays virtual objects on your camera view. Without camera access, AR can't show real world.

**Q: Can I record XR sessions?**
A: Use Android screen recorder. Settings → Advanced → Screen Recorder.

**Q: Does WebXR drain battery faster than normal browsing?**
A: Yes, significantly. AR/VR use GPU, camera, and sensors. Expect 20-30%/hour (AR) or 15-25%/hour (VR).

---

## Supported XR Websites

### Example Sites to Try

**AR Experiences:**
- WebXR Samples: https://immersive-web.github.io/webxr-samples/
- 8th Wall: https://www.8thwall.com/examples
- A-Frame AR: https://aframe.io/examples/

**VR Experiences:**
- WebXR Samples VR: https://immersive-web.github.io/webxr-samples/immersive-vr-session.html
- A-Frame VR: https://aframe.io/examples/
- Sketchfab VR: https://sketchfab.com/ (many models support VR)

**Educational:**
- Google Arts & Culture: https://artsandculture.google.com/ (some AR exhibits)
- NASA: https://eyes.nasa.gov/ (VR space exploration)

---

## Getting Help

### In-App Help

1. Settings → Help & Feedback
2. Report issues or ask questions

### Common Support Resources

- WebAvanue GitHub: [Issues & Discussions]
- WebXR Specification: https://immersive-web.github.io/webxr/
- Community Forums: [Coming Soon]

### Providing Feedback

Help us improve! Report:
- Performance issues
- Compatibility problems
- Feature requests
- Bugs

Include:
- Device model
- Android version
- WebXR settings used
- Website URL where issue occurred

---

## Glossary

**AR (Augmented Reality):** Overlaying virtual objects on the real world through your camera

**VR (Virtual Reality):** Fully immersive 360° virtual environment

**FPS (Frames Per Second):** How smoothly content moves (higher = smoother)

**Session:** An active AR or VR experience

**Thermal Throttling:** Device reducing performance to prevent overheating

**Performance Mode:** Quality vs. battery life setting

**Auto-Pause:** Automatic session pause after timeout (default 30 minutes)

**Immersive:** Full-screen XR experience

**Inline:** XR content shown within a webpage (not full-screen)

---

**Last Updated:** 2025-11-23
**Version:** 1.0

**Enjoy exploring AR and VR in WebAvanue!** 🌍🎮✨

# Troubleshooting Guide

## Overview

This guide helps you solve common problems with WebAvanue. Try the solutions below before reporting issues.

## Table of Contents

- [Voice Commands Issues](#voice-commands-issues)
- [WebView Not Loading](#webview-not-loading)
- [App Crashes](#app-crashes)
- [Performance Issues](#performance-issues)
- [Download Problems](#download-problems)
- [Desktop Mode Issues](#desktop-mode-issues)
- [Tab Management Issues](#tab-management-issues)
- [Favorites & Bookmarks Issues](#favorites--bookmarks-issues)
- [WebXR Problems](#webxr-problems)
- [Network & Connectivity](#network--connectivity)
- [Settings Not Saving](#settings-not-saving)
- [Display & UI Issues](#display--ui-issues)

---

## Voice Commands Issues

### Voice Commands Not Working

**Symptoms:** Microphone doesn't activate or commands not recognized

**Solutions:**

1. **Check Microphone Permission**
   - Go to Android Settings → Apps → WebAvanue → Permissions
   - Enable Microphone permission

2. **Verify Internet Connection**
   - Voice recognition requires internet
   - Check WiFi or mobile data is active

3. **Enable Voice Commands**
   - WebAvanue → Settings → Voice & AI Settings
   - Toggle "Enable Voice Commands" to ON

4. **Test Microphone**
   - Try voice search in another app (Google, YouTube)
   - If other apps fail, it's a device issue

5. **Clear App Cache**
   - Settings → Apps → WebAvanue → Storage
   - Tap "Clear Cache" (NOT Clear Data)

### Commands Not Recognized

**Symptoms:** Microphone works but commands don't execute

**Solutions:**

1. **Use Exact Phrases**
   - Commands must match exactly
   - Check [Voice Commands Guide](VOICE_COMMANDS.md) for correct phrasing

2. **Speak Clearly**
   - Reduce background noise
   - Speak at normal pace
   - Hold device 6-12 inches from mouth

3. **Check Language Settings**
   - Android Settings → System → Languages
   - Ensure English is set (current version only supports English)

4. **Wait for Green Indicator**
   - Only speak when microphone turns green
   - Blue = ready, Green = listening

### Voice Dialog Doesn't Close

**Symptoms:** Voice command dialog stays open after command

**Solution:**

1. Settings → Voice & AI Settings
2. Enable "Voice Dialog Auto-Close"
3. Adjust "Auto-Close Delay" if needed

---

## WebView Not Loading

### Pages Won't Load

**Symptoms:** Blank screen, loading spinner forever, or "Page not available"

**Solutions:**

1. **Check Internet Connection**
   - Open another app (Chrome, YouTube)
   - If no connection, check WiFi/data settings

2. **Verify JavaScript Enabled**
   - Many sites require JavaScript
   - Settings → Privacy → Enable JavaScript → ON

3. **Clear WebView Cache**
   - Settings → Privacy → Clear Cache
   - Tap "Clear" and try again

4. **Disable Ad Blocker Temporarily**
   - Some sites break with ad blocking
   - Settings → Privacy → Block Ads → OFF (test)

5. **Try Desktop Mode**
   - Some sites work better in desktop mode
   - Tap desktop icon in address bar

6. **Check URL Formatting**
   - Must start with http:// or https://
   - Example: https://www.google.com (not google.com)

### Images Not Showing

**Symptoms:** Text loads but no images

**Solutions:**

1. **Enable Image Loading**
   - Settings → Display → Show Images → ON

2. **Disable Data Saver**
   - Settings → Advanced → Data Saver → OFF

3. **Check Connection Quality**
   - Images require more bandwidth
   - Try WiFi instead of mobile data

---

## App Crashes

### App Crashes on Startup

**Symptoms:** App closes immediately or shows "WebAvanue has stopped"

**Solutions:**

1. **Clear App Data** (Warning: Deletes history, favorites, settings)
   - Android Settings → Apps → WebAvanue → Storage
   - Tap "Clear Data"
   - Restart app

2. **Update Android WebView**
   - Play Store → Search "Android System WebView"
   - Update if available

3. **Check Available Storage**
   - Need at least 100MB free space
   - Settings → Storage → Free up space

4. **Reinstall App**
   - Uninstall WebAvanue
   - Download fresh copy from Play Store/F-Droid

### App Crashes When Browsing

**Symptoms:** Crashes on specific sites or after some time

**Solutions:**

1. **Close Unused Tabs**
   - Too many tabs cause memory issues
   - Keep under 10-15 active tabs

2. **Disable Hardware Acceleration**
   - Settings → Advanced → Hardware Acceleration → OFF

3. **Avoid WebGL-Heavy Sites**
   - Some 3D sites (babylon.js, shadertoy) may crash
   - Use Chrome for intensive 3D content

4. **Restart Device**
   - Hold power button → Restart
   - Clears system memory

### Crashes in WebXR Mode

**Symptoms:** App crashes when entering VR/AR

**Solutions:**

1. **Lower Performance Mode**
   - Settings → WebXR → Performance Mode → Battery Saver

2. **Disable XR Temporarily**
   - Settings → WebXR → Enable WebXR → OFF

3. **Update ARCore**
   - Play Store → Search "Google Play Services for AR"
   - Update if available

---

## Performance Issues

### Browser Feels Slow

**Symptoms:** Lag, stuttering, slow page loads

**Solutions:**

1. **Close Unused Tabs**
   - Each tab uses memory
   - Aim for 5-10 active tabs maximum

2. **Clear Cache**
   - Settings → Privacy → Clear Cache
   - Frees up storage and memory

3. **Disable Preloading**
   - Settings → Advanced → Preload Pages → OFF

4. **Enable Hardware Acceleration**
   - Settings → Advanced → Hardware Acceleration → ON

5. **Reduce Visual Effects**
   - Use Light theme instead of Dark
   - Disable background animations

6. **Close Background Apps**
   - Android Settings → Recent Apps
   - Close unused apps to free memory

### High Battery Drain

**Symptoms:** Battery depletes quickly while browsing

**Solutions:**

1. **Use Dark Theme**
   - Settings → Display → Theme → Dark

2. **Enable Data Saver**
   - Settings → Advanced → Data Saver → ON

3. **Limit Background Tabs**
   - Close tabs you're not actively using

4. **Disable Hardware Acceleration**
   - Settings → Advanced → Hardware Acceleration → OFF
   - Trades performance for battery life

5. **Avoid WebXR**
   - AR/VR modes consume significant battery
   - Use Battery Saver XR performance mode if needed

---

## Download Problems

### Downloads Failing

**Symptoms:** "Download failed" or downloads stop unexpectedly

**Solutions:**

1. **Check Storage Space**
   - Need enough free space for file
   - Android Settings → Storage

2. **Verify Permissions**
   - Settings → Apps → WebAvanue → Permissions
   - Enable Storage permission

3. **Disable WiFi-Only Restriction**
   - If on mobile data:
   - Settings → Download Settings → Download Over WiFi Only → OFF

4. **Check Download Folder**
   - Ensure download path exists
   - Settings → Download Settings → Download Path

5. **Try Different Network**
   - Switch between WiFi and mobile data
   - Some networks block certain file types

### Can't Find Downloaded Files

**Symptoms:** Download completes but file missing

**Solutions:**

1. **Check Downloads Folder**
   - Open Files app → Downloads folder
   - Or use WebAvanue → Menu → Downloads

2. **Verify Download Path**
   - Settings → Download Settings → Download Path
   - Note the folder location

3. **Search for File**
   - Files app → Search → Enter filename

---

## Desktop Mode Issues

### Desktop Mode Not Working

**Symptoms:** Sites still show mobile version in desktop mode

**Solutions:**

1. **Reload Page**
   - Desktop mode requires page reload
   - Tap refresh after enabling

2. **Clear Cache**
   - Some sites cache mobile version
   - Settings → Privacy → Clear Cache

3. **Check User Agent**
   - Some sites detect real device
   - Try different desktop mode settings

### Desktop Mode Too Zoomed Out

**Symptoms:** Text too small, hard to read

**Solutions:**

1. **Use Landscape Mode**
   - Auto-fit zoom activates in landscape
   - Rotate device horizontally

2. **Adjust Default Zoom**
   - Settings → Desktop Mode Settings → Default Zoom
   - Increase to 125% or 150%

3. **Pinch to Zoom**
   - Use two fingers to zoom in
   - Settings → Display → Force Zoom → ON (if needed)

4. **Enable Auto-Fit Zoom**
   - Settings → Desktop Mode → Auto-Fit Zoom → ON

---

## Tab Management Issues

### Tabs Not Switching

**Symptoms:** Tapping tab doesn't switch, or wrong tab opens

**Solutions:**

1. **Use Tab Switcher**
   - Tap tab counter badge
   - Select tab from grid view

2. **Close and Recreate Tab**
   - Close problematic tab
   - Reopen from history

3. **Restart App**
   - Close WebAvanue completely
   - Reopen from app drawer

### Tab Counter Wrong

**Symptoms:** Counter shows incorrect number of tabs

**Solutions:**

1. **Force Close App**
   - Android Settings → Apps → WebAvanue → Force Stop
   - Reopen app

2. **Clear Cache**
   - Settings → Apps → WebAvanue → Storage → Clear Cache

---

## Favorites & Bookmarks Issues

### Can't Add Favorite

**Symptoms:** Star button doesn't work or favorite not saving

**Solutions:**

1. **Check URL Format**
   - Must be a valid URL starting with http:// or https://

2. **Verify Not Already Favorited**
   - Gold star = already saved
   - Edit existing favorite instead

3. **Clear App Cache**
   - Settings → Apps → WebAvanue → Storage → Clear Cache

### Favorites Not Showing

**Symptoms:** Saved favorites don't appear in list

**Solutions:**

1. **Check Folder Filter**
   - May be filtered to specific folder
   - Select "All Favorites" in folder dropdown

2. **Search for Favorite**
   - Use search box in Favorites screen
   - Search by title or URL

3. **Verify Database Integrity**
   - Rare: Database corruption
   - Restart app and check again

---

## WebXR Problems

### Can't Enter XR Mode

**Symptoms:** "Enter VR/AR" button doesn't work or permission denied

**Solutions:**

1. **Enable WebXR**
   - Settings → WebXR → Enable WebXR → ON
   - Enable AR and/or VR as needed

2. **Grant Camera Permission**
   - AR mode requires camera
   - Settings → Apps → WebAvanue → Permissions → Camera

3. **Update ARCore**
   - Play Store → "Google Play Services for AR"
   - Update to latest version

4. **Check Device Compatibility**
   - Not all devices support ARCore
   - Visit google.com/ar/discover to check

### Low FPS in XR

**Symptoms:** Stuttering, choppy motion in VR/AR

**Solutions:**

1. **Lower Performance Mode**
   - Settings → WebXR → Performance Mode → Balanced or Battery Saver

2. **Close Background Apps**
   - Free up system resources

3. **Enable FPS Indicator**
   - Settings → WebXR → Show FPS Indicator → ON
   - Check actual frame rate

4. **Reduce XR Content Quality**
   - If available in the XR website settings

---

## Network & Connectivity

### "No Internet Connection" Error

**Symptoms:** Error message despite having internet

**Solutions:**

1. **Check Connection**
   - Open Chrome or YouTube to verify
   - Restart WiFi/mobile data

2. **Clear DNS Cache**
   - Android Settings → WiFi → Your Network
   - "Forget" and reconnect

3. **Disable VPN**
   - Some VPNs block WebView
   - Temporarily disable VPN

4. **Check Firewall**
   - Corporate/school networks may block
   - Try different network

### Slow Loading

**Symptoms:** Pages take long time to load

**Solutions:**

1. **Check Network Speed**
   - Use speed test website
   - Switch to WiFi if on mobile data

2. **Disable Preloading**
   - Settings → Advanced → Preload Pages → OFF

3. **Enable Data Saver**
   - Settings → Advanced → Data Saver → ON
   - Compresses images and content

4. **Clear Cache**
   - Settings → Privacy → Clear Cache

---

## Settings Not Saving

### Changes Revert After Restart

**Symptoms:** Settings reset to default after closing app

**Solutions:**

1. **Check Storage Permissions**
   - Settings → Apps → WebAvanue → Permissions
   - Enable Storage permission

2. **Verify Sufficient Storage**
   - Need space to write settings
   - Android Settings → Storage → Free up space

3. **Clear App Cache** (not Data)
   - Settings → Apps → WebAvanue → Storage
   - Clear Cache only

4. **Reinstall if Persistent**
   - Backup favorites first (export coming soon)
   - Uninstall and reinstall app

---

## Display & UI Issues

### Command Bar Won't Show/Hide

**Symptoms:** Command bar stuck visible or hidden

**Solutions:**

1. **Toggle Manually**
   - Tap hamburger icon (≡) in address bar

2. **Check Auto-Hide Setting**
   - Settings → Command Bar → Auto-Hide → Adjust delay

3. **Restart App**
   - Force close and reopen

### Address Bar Missing

**Symptoms:** Can't see address bar at top

**Solutions:**

1. **Exit Headless Mode**
   - If in fullscreen mode, address bar hidden
   - Command bar → Menu → Exit Fullscreen

2. **Rotate Device**
   - Sometimes fixes UI rendering

3. **Restart App**
   - Force close and reopen

### UI Elements Overlapping

**Symptoms:** Buttons or text overlapping, unreadable UI

**Solutions:**

1. **Change Font Size**
   - Settings → Display → Font Size → Medium

2. **Check Display Scale**
   - Android Settings → Display → Display Size
   - Set to Default

3. **Restart App**
   - UI may not have adjusted to orientation change

---

## Advanced Troubleshooting

### Reset App to Default

**Warning:** This deletes ALL data (history, favorites, settings)

1. Android Settings → Apps → WebAvanue
2. Storage → Clear Data
3. Restart app
4. Reconfigure settings

### Safe Mode Testing

Test if an extension or setting is causing issues:

1. Clear cache (NOT data)
2. Disable all blocking (ads, trackers, popups)
3. Disable hardware acceleration
4. Test if problem persists

### Collect Debug Logs

For bug reports:

1. Enable developer options on Android
2. Connect device to PC
3. Run: `adb logcat | grep WebAvanue`
4. Reproduce issue
5. Save logs and attach to GitHub issue

---

## Getting Help

### Before Reporting Issues

1. Try all relevant troubleshooting steps above
2. Update to latest WebAvanue version
3. Test on another device if possible
4. Note exact steps to reproduce

### Report a Bug

**GitHub Issues:** [github.com/augmentalis/webavanue/issues](https://github.com/augmentalis/webavanue/issues)

**Include:**
- Android version
- Device model
- WebAvanue version
- Exact steps to reproduce
- Screenshots or screen recording
- Logcat output (if available)

### Community Support

- **GitHub Discussions**: Ask questions and share tips
- **Reddit** (coming soon): r/WebAvanue community
- **Discord** (coming soon): Real-time chat support

---

## Emergency Fixes

### Complete Reset (Last Resort)

**If nothing else works:**

1. **Backup Data** (if possible)
   - Screenshots of favorites
   - Export bookmarks (if feature available)

2. **Uninstall App**
   - Android Settings → Apps → WebAvanue → Uninstall

3. **Clear All Data**
   - Ensure app data fully removed

4. **Reinstall Fresh**
   - Download from Play Store or F-Droid
   - Don't restore from backup yet

5. **Test Basic Function**
   - Open a simple page (google.com)
   - Try voice commands
   - Create one tab

6. **Gradually Restore Settings**
   - Add one setting at a time
   - Test after each change
   - Identify problematic setting

---

## Prevention Tips

### Keep App Healthy

- **Regular maintenance**: Clear cache monthly
- **Limit tabs**: Keep under 15 active tabs
- **Update regularly**: Install updates promptly
- **Monitor storage**: Keep 1GB+ free space
- **Restart occasionally**: Close app completely once a week

### Best Practices

- Don't force stop during downloads
- Close XR sessions properly (don't kill app)
- Grant only needed permissions
- Use WiFi for large downloads
- Keep Android system updated

---

**Still Need Help?** Open an issue on [GitHub](https://github.com/augmentalis/webavanue/issues) with detailed information.

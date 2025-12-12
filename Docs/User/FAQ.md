# Frequently Asked Questions (FAQ)

## Table of Contents

- [Getting Started](#getting-started)
- [Voice Commands](#voice-commands)
- [Tabs & Navigation](#tabs--navigation)
- [Favorites & Bookmarks](#favorites--bookmarks)
- [Settings & Customization](#settings--customization)
- [Privacy & Security](#privacy--security)
- [Downloads](#downloads)
- [Desktop Mode](#desktop-mode)
- [WebXR & AR/VR](#webxr--arvr)
- [Performance & Troubleshooting](#performance--troubleshooting)

---

## Getting Started

### What is WebAvanue?

WebAvanue is a modern Android web browser with voice control, AR/VR support, and advanced privacy features. It's designed for hands-free browsing and works with smart glasses.

### Is WebAvanue free?

Yes, WebAvanue is completely free and open source.

### What Android version is required?

WebAvanue requires Android 8.0 (API 26) or higher.

### How do I update WebAvanue?

Updates are available through the Google Play Store or F-Droid. Enable auto-updates in your device's app store settings.

---

## Voice Commands

### How do I use voice commands?

1. Tap the microphone icon in the address bar
2. Wait for the green indicator (listening)
3. Speak your command clearly
4. The command executes automatically

### Why aren't voice commands working?

**Check these common issues:**
- Microphone permission not granted (Settings → Apps → WebAvanue → Permissions)
- No internet connection (voice recognition requires connectivity)
- Background noise interfering with recognition
- Voice commands disabled in Settings

### What commands are available?

WebAvanue supports 30+ voice commands including:
- Navigation: "go back", "go forward", "refresh"
- Scrolling: "scroll up", "scroll down", "scroll to top"
- Tabs: "new tab", "close tab"
- Zoom: "zoom in", "zoom out"
- Features: "bookmark", "history", "settings"

**See full list:** [Voice Commands Guide](VOICE_COMMANDS.md)

### Can I use voice commands offline?

No, voice recognition requires an internet connection for processing.

### How accurate is voice recognition?

Accuracy depends on:
- Clear pronunciation
- Low background noise
- Exact command phrasing
- Internet connection quality

---

## Tabs & Navigation

### How many tabs can I open?

There's no hard limit, but too many tabs may slow down performance. We recommend keeping under 20 active tabs.

### How do I switch between tabs?

**Three ways:**
1. Tap the tab counter badge in address bar
2. Use the address bar tabs dropdown
3. Open 3D tab switcher (tap tab counter)

**Voice:** "next tab" / "previous tab"

### What's the difference between tab views?

- **Address bar dropdown**: Quick list view
- **Tab switcher**: Chrome-style grid view
- **3D spatial tabs**: Immersive carousel (AR/XR)

### Can I group tabs?

Tab groups are coming in a future update. For now, use multiple browser windows or pinned tabs.

### How do I recover a closed tab?

Use the "reopen tab" command (coming soon) or check your History.

---

## Favorites & Bookmarks

### How do I bookmark a page?

**Three ways:**
1. Tap the star icon in the address bar
2. Command bar: Page → Favorite
3. Voice: "bookmark"

### How do I organize bookmarks?

1. Open Favorites (Menu → Favorites)
2. Create folders by category
3. Long-press bookmarks to move them

### Can I sync bookmarks across devices?

Bookmark sync is coming in a future update.

### How do I export/import bookmarks?

Export/import features are planned for a future release.

### Why does the star turn gold?

A gold star means the current page is already in your favorites.

---

## Settings & Customization

### Where are settings located?

Tap the menu button (⋮) in the address bar → Settings

### How do I change the default search engine?

Settings → Search Settings → Search Engine → Select your preferred engine

### Can I change the homepage?

Yes! Settings → Navigation Settings → Homepage → Enter your URL

### How do I make text bigger?

Settings → Display Settings → Font Size → Choose Large or Huge

### What's the difference between themes?

- **Light**: Bright background (daytime)
- **Dark**: Dark background (nighttime)
- **System**: Follows device theme
- **Auto**: Changes based on time of day

---

## Privacy & Security

### Is my browsing data private?

Yes! WebAvanue includes:
- Ad & tracker blocking
- Encrypted database (SQLCipher)
- Optional history clearing
- No data collection

### How do I clear my history?

Menu → History → Menu (⋮) → Clear All History

**Auto-clear:** Settings → Privacy → Clear History on Exit

### What data does WebAvanue collect?

WebAvanue collects **no personal data**. All browsing data stays on your device.

### Can websites track me?

WebAvanue blocks most trackers by default. Enable "Do Not Track" in Privacy Settings for additional protection.

### Is Desktop Mode less secure?

No, desktop mode only changes the user agent. Privacy features work the same in both modes.

### How are passwords stored?

HTTP authentication passwords are encrypted using AES-256-GCM and stored in Android's EncryptedSharedPreferences.

---

## Downloads

### Where are downloads saved?

By default, downloads save to your device's Download folder. Change this in Settings → Download Settings → Download Path.

### Why can't I download files on mobile data?

Check if "Download Over WiFi Only" is enabled in Download Settings.

### How do I pause a download?

Open Downloads (Menu → Downloads) and tap the pause button on any active download.

### Can I resume interrupted downloads?

Yes, tap the resume button on paused downloads.

### Why did my download fail?

Common causes:
- No storage space
- Network interruption
- File no longer available
- Permission denied

---

## Desktop Mode

### What is Desktop Mode?

Desktop Mode makes websites think you're on a PC, loading full desktop versions instead of mobile versions.

### When should I use Desktop Mode?

Use desktop mode for:
- Websites that hide features in mobile view
- Better layouts on large screens
- Web apps that require desktop browsers
- Sites with poor mobile optimization

### How do I enable Desktop Mode?

**Per-tab:**
- Tap desktop/mobile icon in address bar
- Command bar: Page → Desktop Mode
- Voice: "desktop mode"

**Globally:**
- Settings → Display → Use Desktop Mode

### Why is Desktop Mode zoomed out?

Desktop sites are designed for larger screens. Use pinch-to-zoom or:
- Landscape mode (auto-fit zoom)
- Settings → Desktop Mode Settings → Default Zoom

### Does Desktop Mode use more data?

Desktop sites may load more content, using slightly more data than mobile versions.

---

## WebXR & AR/VR

### What is WebXR?

WebXR is a web standard for virtual and augmented reality experiences directly in the browser - no app installation needed.

### What devices support WebXR?

- Android phones with ARCore
- Smart glasses (Rokid, Xreal, Viture, RayNeo)
- VR headsets running Android

### How do I use WebXR content?

1. Visit a WebXR-enabled website
2. Site requests XR permission
3. Allow access
4. Tap "Enter VR" or "Enter AR" on the site

### Why is my frame rate low in XR?

Try changing Performance Mode:
- Settings → WebXR Settings → XR Performance Mode
- Choose "High Quality" (90fps) or "Balanced" (60fps)

### Does WebXR drain battery?

Yes, XR experiences use more battery. Enable Battery Saver mode for longer sessions.

### Can I restrict XR to WiFi?

Yes! Settings → WebXR Settings → Require WiFi → On

---

## Performance & Troubleshooting

### Why is WebAvanue slow?

**Try these fixes:**
- Close unused tabs (fewer tabs = better performance)
- Clear cache: Settings → Privacy → Clear Cache
- Disable extensions/plugins if any
- Restart the browser
- Check available storage space

### Why are pages not loading?

**Check:**
- Internet connection (WiFi or mobile data)
- Airplane mode is off
- Website is online (try another site)
- JavaScript enabled (Settings → Privacy)

### Why are images not showing?

Settings → Display → Show Images → Make sure it's enabled

### How do I report a bug?

Open an issue on our [GitHub repository](https://github.com/augmentalis/webavanue) with:
- Android version
- Device model
- Steps to reproduce
- Screenshots if applicable

### Why does the app crash?

Common causes:
- Low memory (close other apps)
- Too many tabs open
- Corrupted cache (clear cache)
- Outdated Android version

If crashes persist, try:
1. Clear app data (Settings → Apps → WebAvanue → Storage → Clear Data)
2. Reinstall the app
3. Report the issue on GitHub

### How do I improve battery life?

- Use Dark theme
- Enable Data Saver
- Limit background tabs
- Disable hardware acceleration
- Lower WebXR quality settings

### Why is voice recognition slow?

Voice recognition depends on:
- Internet speed (processes in cloud)
- Server response time
- Background noise level

Try switching to a faster network if available.

---

## Still Have Questions?

### Need More Help?

- **Documentation**: Browse our complete guides
  - [Voice Commands](VOICE_COMMANDS.md)
  - [Settings](SETTINGS.md)
  - [Features](FEATURES.md)
  - [Troubleshooting](TROUBLESHOOTING.md)

- **Community Support**: Join our GitHub Discussions
- **Bug Reports**: Open an issue on GitHub
- **Feature Requests**: Submit via GitHub Issues

### Contact

- **GitHub**: [github.com/augmentalis/webavanue](https://github.com/augmentalis/webavanue)
- **Website**: [Coming Soon]
- **Email**: [Coming Soon]

---

**Didn't find your answer?** Check our [Troubleshooting Guide](TROUBLESHOOTING.md) for more solutions.

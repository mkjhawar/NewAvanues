# Features Guide

## Overview

WebAvanue is a modern, voice-enabled browser built for Android with support for AR/XR devices. This guide covers all major features and how to use them.

## Table of Contents

- [Tab Management](#tab-management)
- [Voice Commands](#voice-commands)
- [Favorites & Bookmarks](#favorites--bookmarks)
- [History](#history)
- [Downloads](#downloads)
- [Desktop Mode](#desktop-mode)
- [WebView Pooling](#webview-pooling)
- [Settings Customization](#settings-customization)
- [Command Bar](#command-bar)
- [WebXR Support](#webxr-support)
- [Security Features](#security-features)

---

## Tab Management

### Multiple Tabs
Open and manage multiple web pages simultaneously.

**Features:**
- **Unlimited tabs**: Open as many tabs as you need
- **Tab switching**: Quick access via address bar dropdown
- **3D Tab Switcher**: Visual grid view of all tabs (tap tab counter)
- **Spatial Tab View**: Immersive 3D carousel for AR/XR devices
- **Tab pinning**: Pin important tabs to keep them open
- **Tab groups**: Organize tabs by category (coming soon)

**How to Use:**
1. **New tab**: Tap + button in address bar
2. **Switch tabs**: Tap tab counter badge or use address bar dropdown
3. **Close tab**: Swipe or tap X on tab card
4. **Pin tab**: Long-press tab and select "Pin"

**Voice Commands:**
- "new tab" - Open new tab
- "close tab" - Close current tab
- "next tab" / "previous tab" - Navigate between tabs

---

## Voice Commands

Browse hands-free with voice control.

**Features:**
- **30+ voice commands** for navigation, scrolling, tabs, and more
- **Always accessible**: Microphone button in address bar
- **Visual feedback**: Green indicator when listening
- **Command bar integration**: Voice activates command bar

**Getting Started:**
1. Tap microphone icon in address bar
2. Wait for green indicator
3. Speak your command
4. Command executes automatically

**Popular Commands:**
- "go back" - Previous page
- "scroll down" - Scroll page
- "new tab" - Open tab
- "bookmark" - Save page
- "go to [url]" - Navigate to website

**Learn More:** [Voice Commands Guide](VOICE_COMMANDS.md)

---

## Favorites & Bookmarks

Save and organize your favorite websites.

**Features:**
- **One-tap bookmarking**: Star icon in address bar
- **Folder organization**: Group favorites by category
- **Quick access**: Address bar dropdown shows favorites
- **3D Favorites Shelf**: Spatial carousel view for AR/XR
- **Search favorites**: Find saved sites quickly
- **Sync** (coming soon): Access favorites across devices

**How to Use:**
1. **Add favorite**: Tap star icon in address bar
2. **Edit favorite**: Long-press favorite → Edit
3. **Organize**: Create folders in Favorites screen
4. **Access**: Tap favorites dropdown in address bar

**Tips:**
- Use folders to organize by topic (Work, News, Shopping, etc.)
- Add descriptions to remember why you saved a site
- Star icon turns gold when page is bookmarked

---

## History

Track and revisit your browsing history.

**Features:**
- **Full history**: Complete record of visited pages
- **Search history**: Find pages by title or URL
- **Date filtering**: Browse by time range
- **Most visited**: See your top sites
- **Clear history**: Delete all or by date range
- **Auto-clear** (optional): Clear history on exit

**How to Use:**
1. **View history**: Menu → History
2. **Search**: Use search box at top
3. **Revisit page**: Tap any history entry
4. **Delete entry**: Swipe left or long-press → Delete
5. **Clear all**: Menu → Clear All History

**Privacy Tip:** Enable "Clear History on Exit" in Settings for auto-delete.

---

## Downloads

Manage file downloads directly in the browser.

**Features:**
- **Download manager**: View all downloads in one place
- **Progress tracking**: Real-time download progress
- **Pause/Resume**: Control large downloads
- **Open files**: Launch downloaded files from browser
- **Download history**: See past downloads
- **WiFi-only option**: Save mobile data

**How to Use:**
1. **Download file**: Tap download link on any website
2. **View downloads**: Menu → Downloads
3. **Open file**: Tap completed download
4. **Pause download**: Tap pause button
5. **Cancel download**: Swipe left or tap cancel

**Settings:**
- **Download path**: Choose where files save
- **Ask location**: Prompt for each download
- **WiFi only**: Block downloads on mobile data

---

## Desktop Mode

Load full desktop versions of websites.

**Features:**
- **Desktop user agent**: Sites think you're on a PC
- **Responsive controls**: Auto-fit zoom in landscape
- **Per-tab setting**: Different mode for each tab
- **Global default**: Set desktop mode for all new tabs
- **Window simulation**: Configure screen size (1280x800 default)

**When to Use:**
- **Full-featured sites**: Access desktop-only features
- **Better layouts**: Some sites work better in desktop mode
- **Web apps**: Use full versions of web applications

**How to Toggle:**
1. **Address bar**: Tap desktop/mobile icon
2. **Command bar**: Page → Desktop Mode
3. **Settings**: Enable for all tabs by default

**Voice Command:** "desktop mode" / "mobile mode"

**Advanced Settings:**
- **Default zoom**: Adjust desktop mode zoom (50-200%)
- **Window size**: Simulate different screen dimensions
- **Auto-fit zoom**: Automatically fit page to screen in landscape

---

## WebView Pooling

Efficient tab management with preloaded web views.

**Technical Feature:**
- **Faster tab switching**: Instant tab activation
- **Memory optimization**: Reuse web views efficiently
- **Background loading**: Preload pages for quick access
- **Lifecycle management**: Automatic cleanup of unused views

**Benefits:**
- Smoother tab transitions
- Lower memory usage
- Faster page loading
- Better battery life

**Note:** This feature works automatically in the background.

---

## Settings Customization

Personalize every aspect of your browser.

**Categories:**
- **Display**: Theme, font size, image loading
- **Privacy**: Ad blocking, tracking protection, cookies
- **Search**: Default engine, suggestions
- **Navigation**: Homepage, new tab behavior
- **Downloads**: Save location, WiFi-only
- **Voice & AI**: Voice commands, AI features
- **WebXR**: AR/VR settings
- **Advanced**: Performance, media autoplay

**Quick Settings:**
- Access via Menu → Settings
- Search settings to find options quickly
- Use presets for common configurations

**Learn More:** [Settings Guide](SETTINGS.md)

---

## Command Bar

Voice-first navigation with floating command bar.

**Features:**
- **Hands-free control**: Optimized for voice commands
- **Multi-level menu**: Main → Scroll, Zoom, Page, Menu
- **Adaptive layout**: Horizontal (portrait) / Vertical (landscape)
- **Context awareness**: Changes based on current state
- **Toggle visibility**: Show/hide with hamburger button

**How to Use:**
1. **Show bar**: Tap hamburger icon (≡) in address bar
2. **Navigate levels**: Tap icons to access sub-menus
3. **Execute commands**: Tap buttons or use voice
4. **Hide bar**: Tap hide button or auto-hides after 10s

**Command Hierarchy:**
- **Main**: Back, Home, Tabs, Favorites, Page, Menu
- **Scroll**: Up, Down, Top, Bottom, Freeze
- **Zoom**: In, Out, 50%, 100%, 150%
- **Page**: Prev, Next, Reload, Desktop, Favorite
- **Menu**: Bookmarks, Downloads, History, Settings

---

## WebXR Support

Immersive web experiences for AR/VR devices.

**Supported Features:**
- **WebXR API**: Standard web-based AR/VR
- **Immersive-AR**: Augmented reality sessions
- **Immersive-VR**: Virtual reality sessions
- **Camera permissions**: Managed AR camera access
- **Performance modes**: High Quality, Balanced, Battery Saver

**Supported Devices:**
- Android phones with AR Core
- Smart glasses (Rokid, Xreal, Viture, RayNeo)
- VR headsets with Android

**Settings:**
- **Enable WebXR**: Master toggle
- **AR/VR separately**: Control AR and VR independently
- **Performance mode**: Balance quality vs battery
- **Auto-pause**: Automatic pause after 30 minutes
- **FPS indicator**: Show frame rate in XR sessions
- **WiFi requirement**: Restrict XR to WiFi only

**Accessing WebXR:**
1. Visit WebXR-enabled website
2. Site requests XR permission
3. Allow or deny access
4. Enter immersive session

---

## Security Features

Stay safe while browsing.

### Content Blocking
- **Ad blocking**: Block intrusive ads
- **Tracker blocking**: Prevent tracking scripts
- **Popup blocking**: Stop annoying popups

### Certificate Validation
- **SSL error dialogs**: Warns about certificate issues
- **Proceed with caution**: Option to continue on unsafe sites
- **Auto-reject spam**: Blocks repeated security dialogs

### Permission Management
- **Granular permissions**: Camera, microphone, location, etc.
- **Remember choices**: Save permission decisions
- **Per-site settings**: Different permissions per website

### JavaScript Dialog Protection
- **Dialog spam prevention**: Blocks excessive alert/confirm dialogs
- **Max 3 dialogs per 10 seconds**: Prevents malicious sites

### HTTP Authentication
- **Secure credential storage**: Encrypted with AES-256-GCM
- **Remember passwords**: Optional password saving
- **Auto-fill credentials**: Automatic login on return visits

### Database Encryption
- **SQLCipher**: AES-256 encryption for all browser data
- **Android Keystore**: Hardware-backed key storage
- **Encrypted history**: Private browsing records

**Learn More:** [Security Policy](../SECURITY.md)

---

## Additional Features

### Network Status
- **Connection indicator**: Shows online/offline status
- **Automatic retry**: Reloads when connection restored

### Headless Mode
- **Fullscreen browsing**: Hides address bar for immersive viewing
- **Toggle via command bar**: Menu → Fullscreen
- **Voice command** (coming soon): "fullscreen"

### Reader Mode (Coming Soon)
- **Distraction-free reading**: Clean article layout
- **Custom fonts**: Choose reading font
- **Adjustable width**: Optimize reading line length

### Incognito Mode (Coming Soon)
- **Private browsing**: No history or cookies saved
- **Isolated sessions**: Separate from normal tabs
- **Auto-clear on close**: All data deleted

---

## Keyboard Shortcuts (Coming Soon)

Desktop and external keyboard support.

- **Ctrl+T**: New tab
- **Ctrl+W**: Close tab
- **Ctrl+Tab**: Next tab
- **Ctrl+R**: Reload
- **Ctrl+D**: Bookmark

---

## See Also

- [Voice Commands](VOICE_COMMANDS.md) - All voice commands
- [Settings](SETTINGS.md) - Customization options
- [FAQ](FAQ.md) - Common questions
- [Troubleshooting](TROUBLESHOOTING.md) - Fix issues

---

**Discover More:** Explore the app to find hidden features and shortcuts!

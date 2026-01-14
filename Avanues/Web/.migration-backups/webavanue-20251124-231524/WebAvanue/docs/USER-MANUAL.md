# WebAvanue User Manual

**Version:** 1.0.0
**Last Updated:** 2025-11-22
**Voice-First Cross-Platform Browser**
**Build Status:** ✅ Production Ready

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Voice Commands](#voice-commands)
3. [Navigation](#navigation)
4. [Favorites](#favorites)
5. [Scroll Controls](#scroll-controls)
6. [Zoom Controls](#zoom-controls)
7. [Desktop Mode](#desktop-mode)
8. [Advanced Features](#advanced-features)

---

## Getting Started

WebAvanue is a voice-first browser designed for hands-free operation with full touch/click support as backup.

### First Launch

1. **Home Screen**: Opens with address bar and empty tab
2. **Voice Button**: Tap microphone icon or say "Hey Browser"
3. **Address Bar**: Type URL or say "Go to [website]"

---

## Voice Commands

### Navigation Commands

| Voice Command | Action |
|---------------|--------|
| "Go to [website]" | Navigate to website |
| "Go back" | Navigate back in history |
| "Go forward" | Navigate forward in history |
| "Refresh" | Reload current page |
| "Go home" | Return to home page |

### Tab Commands

| Voice Command | Action |
|---------------|--------|
| "New tab" | Create new tab |
| "Close tab" | Close current tab |
| "Next tab" | Switch to next tab |
| "Previous tab" | Switch to previous tab |

### Scroll Commands

| Voice Command | Action |
|---------------|--------|
| "Scroll up" | Scroll up by viewport |
| "Scroll down" | Scroll down by viewport |
| "Scroll left" | Scroll left |
| "Scroll right" | Scroll right |
| "Top of page" | Scroll to top |
| "Bottom of page" | Scroll to bottom |
| "Freeze page" | Freeze/unfreeze scrolling |

### Zoom Commands

| Voice Command | Action |
|---------------|--------|
| "Zoom in" | Increase zoom level |
| "Zoom out" | Decrease zoom level |
| "Zoom level 1" | Set zoom to 50% |
| "Zoom level 2" | Set zoom to 75% |
| "Zoom level 3" | Set zoom to 100% (default) |
| "Zoom level 4" | Set zoom to 125% |
| "Zoom level 5" | Set zoom to 150% |

### Other Commands

| Voice Command | Action |
|---------------|--------|
| "Desktop mode" | Toggle desktop/mobile mode |
| "Add to favorites" | Save current page |
| "Show favorites" | Display favorites bar |
| "Show bookmarks" | Open bookmarks screen |
| "Show downloads" | Open downloads screen |
| "Show history" | Open history screen |
| "Settings" | Open settings screen |
| "Clear cache" | Clear browser cache |
| "Clear cookies" | Clear all cookies |

---

## Navigation

### Address Bar

**Location**: Top of screen

**Features**:
- **Back/Forward Buttons**: Navigate browser history
- **URL Input**: Type or paste URLs
- **Go Button**: Navigate to entered URL
- **Refresh Button**: Reload current page
- **Star Icon**: Add to favorites (gold when already favorited)
- **Desktop Mode Indicator**: Shows when desktop mode active

### Tab Bar

**Location**: Below address bar

**Features**:
- **Tab List**: All open tabs with titles
- **Active Tab**: Highlighted tab
- **New Tab Button**: Create new tab
- **Tab Close**: X button on each tab

---

## Favorites

### Adding Favorites

**Method 1: Star Icon**
1. Navigate to page you want to save
2. Tap star icon in address bar
3. Edit title/URL/description if needed
4. Tap "Save"

**Method 2: Voice Command**
1. Navigate to page
2. Say "Add to favorites"
3. Confirm details in dialog

**Method 3: Favorites Bar**
1. Tap "+" button in favorites bar
2. Enter URL and title
3. Tap "Save"

### Favorites Bar

**Location**: Below address bar

**Features**:
- **Horizontal Scrolling**: Swipe left/right to see all favorites
- **Favicon**: Website icon for each favorite
- **Quick Access**: Tap to navigate
- **Add Button**: + icon to add new favorite
- **Gold Star**: Current page is favorited

### Managing Favorites

**Edit Favorite**:
1. Long-press favorite in favorites bar
2. Edit title/URL/description
3. Tap "Save"

**Delete Favorite**:
1. Long-press favorite
2. Tap "Delete"
3. Confirm deletion

**Organize Favorites**:
1. Open Bookmarks screen (Menu → Bookmarks)
2. Create folders
3. Drag favorites to folders
4. Reorder favorites

---

## Scroll Controls

### Touch Scrolling

**Standard**: Swipe up/down/left/right on page content

### Voice Scrolling

**Available Commands**:
- "Scroll up" - Scroll up one viewport
- "Scroll down" - Scroll down one viewport
- "Scroll left" - Scroll left
- "Scroll right" - Scroll right
- "Top of page" - Jump to top
- "Bottom of page" - Jump to bottom

### Command Bar Scrolling

1. Tap command bar at bottom
2. Tap "Navigation Commands"
3. Tap "Scroll"
4. Choose: Up, Down, Left, Right, Top, Bottom, Freeze

### Freeze Page

**Purpose**: Prevent accidental scrolling during reading

**How to Enable**:
- Say "Freeze page"
- Or: Command Bar → Navigation → Scroll → Freeze

**Indicator**: Red border around page when frozen

**How to Disable**:
- Say "Freeze page" again
- Or: Command Bar → Navigation → Scroll → Freeze

---

## Zoom Controls

### Zoom Levels

WebAvanue supports 5 zoom levels:

| Level | Zoom % | Description |
|-------|--------|-------------|
| 1 | 50% | Smallest - maximum overview |
| 2 | 75% | Small - wide view |
| 3 | 100% | Default - normal size |
| 4 | 125% | Large - easier reading |
| 5 | 150% | Largest - maximum readability |

### Changing Zoom

**Method 1: Voice**
- "Zoom in" - Increase one level (max: 5)
- "Zoom out" - Decrease one level (min: 1)
- "Zoom level [1-5]" - Set specific level

**Method 2: Command Bar**
1. Tap command bar at bottom
2. Tap "Navigation Commands"
3. Tap "Zoom"
4. Choose: In, Out, or tap "Levels" for specific level

**Method 3: Pinch Gesture**
- Pinch to zoom out
- Spread to zoom in

### Zoom Persistence

Zoom level is saved per-tab:
- Switch tabs: Each tab remembers its zoom level
- Close/reopen app: Zoom levels restored
- Different sites: Each site can have different zoom

---

## Desktop Mode

### What is Desktop Mode?

Desktop mode requests the desktop version of websites instead of mobile versions.

**Benefits**:
- Full website features (not mobile-limited)
- Desktop-style layouts
- Access to features hidden on mobile

**Drawbacks**:
- Smaller text (use zoom to compensate)
- More horizontal scrolling
- Some sites may not be optimized

### Enabling Desktop Mode

**Method 1: Voice**
- Say "Desktop mode" to toggle on/off

**Method 2: Address Bar**
- Tap address bar dropdown menu
- Toggle "Desktop Mode" switch

**Method 3: Command Bar**
1. Tap command bar at bottom
2. Tap "Web Commands"
3. Tap "Desktop Mode" icon

### Desktop Mode Indicator

**Location**: Inside address bar URL field

**Visual**: Small blue badge with desktop icon

**States**:
- **Visible**: Desktop mode active
- **Hidden**: Mobile mode (default)

### Desktop Mode Persistence

Desktop mode is saved per-tab:
- Switch tabs: Each tab has independent mode
- Close/reopen app: Mode restored per tab
- Different sites: Some sites may override

---

## Advanced Features

### HTTP Basic Authentication

Some websites require username/password authentication.

**When Prompted**:
1. Enter username
2. Enter password
3. Check "Remember credentials" to save
4. Tap "Login"

**Saved Credentials**:
- Stored encrypted in device
- Auto-filled on next visit
- Can be cleared in Settings → Privacy

### Touch Controls

**Available in Command Bar → Web Commands → Touch**:

- **Drag Mode**: Enable/disable drag gestures
- **Pinch Open**: Zoom in gesture
- **Pinch Close**: Zoom out gesture
- **Rotate**: Rotate image/content

### Cursor Controls

**Available in Command Bar → Navigation → Cursor**:

- **Click**: Simulate mouse click at center
- **Double Click**: Simulate double-click

### Clear Cache & Cookies

**Clear Cache**:
- Voice: "Clear cache"
- Command Bar → Web Commands → Clear Cache
- Frees up storage space
- May slow down first page load

**Clear Cookies**:
- Voice: "Clear cookies"
- Command Bar → Web Commands → Clear Cookies
- Logs you out of all websites
- Removes tracking cookies

### Downloads

**View Downloads**:
- Voice: "Show downloads"
- Command Bar → Menu → Downloads

**Download Status**:
- **Pending**: Queued for download
- **Active**: Currently downloading (progress %)
- **Paused**: Download paused
- **Complete**: Download finished
- **Failed**: Download error

**Download Actions**:
- **Pause**: Pause active download
- **Resume**: Resume paused download
- **Retry**: Retry failed download
- **Open**: Open completed download
- **Delete**: Remove download

### History

**View History**:
- Voice: "Show history"
- Command Bar → Menu → History

**History Features**:
- Full browsing history
- Search history by URL/title
- Filter by date range
- Delete individual items
- Clear all history

### Settings

**Access Settings**:
- Voice: "Settings"
- Command Bar → Menu → Settings

**Available Settings**:
- **Default Home Page**: Set startup page
- **Search Engine**: Choose default search
- **Privacy**: Cookie/tracking settings
- **Downloads**: Download location
- **Advanced**: Developer options

---

## Tips & Tricks

### Fastest Navigation

1. **Voice for URLs**: Say "Go to example.com" (faster than typing)
2. **Favorites for Common Sites**: One-tap access
3. **Tab Shortcuts**: "Next tab" / "Previous tab" for quick switching

### Best Reading Experience

1. **Desktop Mode** for full content
2. **Zoom Level 4** for comfortable reading
3. **Freeze Page** to prevent accidental scrolling
4. **Scroll to Top** when you reach bottom

### Power User Features

1. **Multiple Tabs**: Open links in new tabs for multitasking
2. **Favorites Folders**: Organize favorites by category
3. **Desktop Mode Per-Tab**: Mix mobile/desktop sites
4. **Zoom Per-Tab**: Different zoom for different sites

### Troubleshooting

**Website not loading?**
- Check internet connection
- Try "Refresh"
- Clear cache and retry

**Text too small?**
- Increase zoom level (Level 4 or 5)
- Or disable desktop mode

**Page not scrolling?**
- Check if "Freeze Page" is active
- Look for red border (frozen indicator)
- Say "Freeze page" to unfreeze

**Login issues?**
- Check if cookies are enabled (Settings → Privacy)
- Try clearing cookies and logging in again
- Verify username/password

---

## Keyboard Shortcuts

### Navigation
- **Ctrl+T**: New tab
- **Ctrl+W**: Close tab
- **Ctrl+Tab**: Next tab
- **Ctrl+Shift+Tab**: Previous tab
- **Ctrl+R**: Refresh page
- **Alt+Left**: Go back
- **Alt+Right**: Go forward

### Zoom
- **Ctrl++**: Zoom in
- **Ctrl+-**: Zoom out
- **Ctrl+0**: Reset zoom (100%)

### Other
- **Ctrl+D**: Add to favorites
- **Ctrl+H**: Show history
- **Ctrl+Shift+Delete**: Clear data
- **F11**: Fullscreen mode

---

## Support

**Issues or Questions?**
- Email: support@augmentalis.com
- Documentation: https://docs.webavanue.com
- GitHub: https://github.com/augmentalis/webavanue

**Version Information**:
- WebAvanue: 1.0.0
- Platform: Android / iOS / Desktop
- Database: SQLDelight 2.0.1
- UI: Compose Multiplatform 1.6.1

---

**Last Updated:** 2025-11-22
**Build Status:** ✅ Compiles cleanly, tests pass
**© 2025 Augmentalis. All rights reserved.**

# WebAvanue User Manual

**Version:** 1.9.0
**Last Updated:** 2025-12-06
**Voice-First Cross-Platform Browser**
**Build Status:** ✅ Production Ready

---

## Release Notes (v1.9.0 - 2025-12-06)

### Ocean UI Complete - Enhanced Visual Clarity

We've completed the Ocean UI redesign for maximum clarity and consistency across the entire application!

#### What's New

**1. All Blur Effects Removed**

For better clarity and performance:
- **Address Bar**: Now uses solid surfaces instead of glass/blur effects
- **Network Status**: Solid background for maximum readability
- **Consistent Theming**: All components use Ocean design tokens
- **Better Performance**: Reduced rendering overhead on older devices

**2. Interactive Voice Commands Dialog**

The voice commands help dialog is now fully interactive - tap commands to execute them!

**How it works:**
- Tap the FAB (? button) to open voice commands help
- Navigate to any category (Navigation, Tabs, Zoom, Modes, etc.)
- **Tap any command to execute it directly!**

**Command behavior:**
- **Action commands** (back, forward, refresh, new tab, modes) → Execute and **auto-dismiss**
- **Adjustment commands** (zoom in/out) → Execute but **stay open** for multiple adjustments
- **Input commands** (search, go to URL) → Display placeholder (not yet implemented)

**Example:** Tap "Modes" → Tap "desktop mode" → Switches to desktop mode, reloads page, and closes dialog!

**3. Voice Navigation in Help Dialog**

You can now navigate the voice commands dialog using voice:
- Say **"navigation"**, **"tabs"**, **"zoom"**, **"modes"**, or **"features"** to view that category
- Say **"back"** to return to categories view
- Dialog supports both voice and touch navigation

**4. Desktop/Mobile Mode Now Reloads Page**

When you toggle between desktop and mobile mode:
- The **current page reloads** with the new user agent
- **Stays on same website** (no navigation to home)
- Immediate effect - see the desktop/mobile version right away

**5. Dialog Stays Open Until Dismissed**

Voice commands dialog improvements:
- **Before**: Dialog auto-closed after a few seconds
- **After**: Stays open until you dismiss it (close button or tap outside)
- Take your time to reference commands or execute multiple actions

#### Benefits

| Feature | Benefit |
|---------|---------|
| **No Blur** | Clearer text, better readability, improved performance |
| **Interactive Commands** | Execute actions directly from help dialog |
| **Voice Navigation** | Navigate help dialog hands-free |
| **Smart Auto-Dismiss** | Actions close dialog, adjustments keep it open |
| **Page Reload** | Mode toggle takes effect immediately |
| **Persistent Dialog** | Reference commands without time pressure |

#### Ocean UI Architecture

The complete Ocean UI system provides:
- **Framework Independence**: Ready for future MagicUI migration
- **Consistent Design**: All components use Ocean design tokens
- **Type-Safe Icons**: IconVariant system prevents color mismatches
- **Maintainability**: Single source of truth for design decisions
- **Accessibility**: High-contrast variants through semantic colors

---

## Previous Release (v1.8.3 - 2025-12-05)

### UI/UX Improvements - 8 Fixes from Testing!

We've addressed 8 issues from comprehensive user testing to make WebAvanue even better:

#### What's New

**1. Fullscreen Browsing Mode**

Browse websites in true fullscreen without address bar or floating button:
- Open command bar → Menu → **Fullscreen**
- Address bar and help button hide
- Tap **Exit Fullscreen** to return to normal

**2. Command Bar Toggle Fixed**

The show/hide command bar button now works correctly:
- **Before**: Button showed voice mode status, not bar visibility
- **After**: Arrow icon clearly shows if bar is hidden (↑) or visible (↓)
- Separate microphone button for voice commands

**3. Voice Commands Dialog - Better in Landscape**

When you hold your device sideways:
- Commands now display in **2-3 columns** instead of one
- All commands visible without scrolling
- Equal-width buttons for cleaner look

**4. Voice Dialog Settings**

Control how the voice commands dialog behaves:
- **New Setting**: Auto-close Voice Dialog (on/off)
- **New Setting**: Auto-close Delay slider (0.5-5 seconds)
- Find in: Settings → Voice & AI section

**5. Help Button Glassmorphic Style**

The floating help button (?) now matches our Ocean theme:
- Glass-like appearance with blur effect
- Matches the overall glassmorphic design
- Still easy to tap (48dp touch target)

**6. History Button Replaces Star**

The star icon in the address bar now opens History:
- **Before**: Star was confusing (same as bookmark icon)
- **After**: Tap clock icon to see your browsing history
- Bookmarks still available via bookmark icon

**7. Tab View Icons Redesigned**

Grid/List toggle in Tab Switcher now clearer:
- **Grid View**: 4 squares in 2x2 pattern
- **List View**: 3 horizontal lines of varying length
- Easier to distinguish at a glance

**8. Network Status Alert**

When you lose internet connection:
- Alert appears at top of screen
- Shows when disconnected, slow, or reconnecting
- Disappears when connection restored

#### Benefits

| Feature | Benefit |
|---------|---------|
| **Fullscreen Mode** | Maximum screen space for web content |
| **Fixed Toggle** | Command bar shows/hides predictably |
| **Landscape Columns** | All voice commands visible without scrolling |
| **Dialog Settings** | Customize auto-close behavior |
| **Glass FAB** | Cohesive visual design |
| **History Button** | Quick access to browsing history |
| **Tab Icons** | Clearer visual distinction |
| **Network Alert** | Know immediately when offline |

---

## Previous Release (v1.8.2 - 2025-12-03)

### Voice Commands Help - Now Easier to Discover!

We've made it much easier to learn and use voice commands with a new categorized help dialog and longer auto-hide delays:

#### What's New

**1. Categorized Voice Commands Help Dialog**

Access all voice commands organized by category:
- **Navigation**: Go back, forward, home, refresh
- **Scrolling**: Scroll up/down/left/right, top/bottom
- **Tabs**: New tab, close tab, next/previous tab
- **Zoom**: Zoom in/out, set specific zoom levels
- **Modes**: Desktop mode, freeze scrolling
- **Features**: Favorites, bookmarks, downloads, history, settings

**How to Access:**
- Say **"help"**, **"show help"**, or **"commands"**
- Tap the voice commands FAB button
- The help dialog shows all available commands organized by category

**2. More Time to Access Settings**

We've doubled the command bar auto-hide delay:
- **Before**: Command bar hid after 5 seconds
- **After**: Command bar stays visible for 10 seconds
- **Benefit**: More time to navigate to settings and other options

**3. Configurable Auto-Close Delay**

The voice commands dialog will auto-close 2 seconds after you execute a command:
- Gives you time to see the result
- Closes automatically so you can continue browsing
- Future update will add settings to customize this delay

#### Benefits

| Feature | Benefit |
|---------|---------|
| **Categorized Help** | Find voice commands faster |
| **Voice-Activated Help** | Just say "help" to see all commands |
| **Longer Auto-Hide** | More time to access settings |
| **Auto-Close Dialog** | Commands execute and dialog closes automatically |

---

## Previous Release (v1.8.1 - 2025-12-03)

### Performance Improvements - No More App Freezing! 🎉

We've fixed an issue where the app would freeze (ANR - "App Not Responding") on certain devices, especially when:
- Switching between mobile and desktop mode
- Loading graphics-heavy websites (like Babylon.js or Shadertoy)
- Using the RealWear HMT-1 or similar AOSP Android 10 devices

#### What Changed

| Before | After |
|--------|-------|
| App froze for 5+ seconds when toggling desktop mode | Smooth toggle with instant response ✅ |
| Websites could load forever and freeze the app | Automatic timeout after 4 seconds ✅ |
| No indication of problematic websites | Visual warning on graphics-heavy sites ⚠️ |

#### New Features

**1. Smoother Desktop Mode Toggle**

When you tap the laptop/phone icon to switch between mobile and desktop mode:
- **Before**: App would freeze for several seconds
- **After**: Instant response, page reloads smoothly
- **How it works**: The app now waits a tiny moment (0.15 seconds) before reloading, which prevents freezing

**2. Automatic Page Load Protection**

If a website takes too long to load:
- The app will automatically stop loading after 4 seconds
- You'll see a console message: "⚠️ WebView load timeout after 4s"
- This prevents the app from freezing while waiting for slow websites
- You can always tap refresh to try again

**3. Graphics-Heavy Site Warning**

When you visit websites that use advanced 3D graphics (WebGL):
- The desktop mode toggle will appear **slightly dimmed** (50% opacity)
- This is just a visual hint that the site may load slower
- **You can still use it** - it's not disabled!
- Sites detected: Babylon.js, Shadertoy, Three.js, WebGPU demos

#### What This Means for You

**On Normal Websites (Google, Wikipedia, etc.):**
- Everything works exactly as before
- Desktop toggle is instant and smooth

**On Graphics-Heavy Websites (Babylon.js, Shadertoy):**
- Desktop toggle has a dimmed appearance (warning)
- Toggle still works, but page reload may take a moment
- If page takes too long, it will auto-stop to keep app responsive
- App will **never freeze** - you can always interact with it

#### For HMT-1 Users

This update specifically fixes freezing issues on:
- RealWear HMT-1
- AOSP Android 10 devices
- Devices with limited WebView support

Your experience should now be:
- ✅ No more "App Not Responding" messages
- ✅ Smooth desktop mode switching
- ✅ Better handling of complex websites

---

## Previous Release (v1.8.0 - 2025-12-02)

### Two-Level Address Bar (Portrait Mode)

When you hold your phone upright (portrait mode), the address bar now uses two rows to give you more space for URLs:

#### What Changed

| Before | After |
|--------|-------|
| All buttons on one row | Buttons split across two rows |
| URL input cramped | URL bar gets full width |
| 36dp buttons | Smaller 24dp buttons |

#### New Layout

**Top Row** (URL bar with quick actions):
- Bookmark icon (tap to save page)
- URL input field (takes full width now!)
- Star icon (tap to favorite, long-press for favorites shelf)
- Go button

**Bottom Row** (navigation buttons at half size):
- Back button
- Forward button
- Refresh button
- Desktop/Mobile toggle (tap to switch)
- Tab counter
- Voice button

#### Benefits

- **More Room for URLs**: URL input gets almost the full screen width
- **Easier to Read**: Long URLs are no longer cut off
- **Compact Navigation**: All buttons still accessible but take less space
- **Quick Access**: Desktop toggle right in the address bar

#### How It Works

**Portrait Mode** (phone held upright):
1. Top row shows URL bar with bookmark/star/desktop/go buttons
2. Bottom row shows navigation buttons (back/forward/refresh/tabs/voice)
3. All buttons are smaller (24dp) to fit comfortably
4. URL field gets maximum horizontal space

**Landscape Mode** (phone held sideways):
- Single-row layout (unchanged)
- All buttons are full size (36dp)
- Same layout as before

### Desktop Mode Toggle - Now Clickable!

You can now switch between mobile and desktop mode directly from the address bar:

#### What Changed

| Before | After |
|--------|-------|
| Desktop indicator was visual only | Tap to switch modes |
| Had to use command bar to toggle | Direct toggle from address bar |
| Laptop/Phone icon showed current mode | Icon changes when you tap it |

#### How to Use

1. **Find the indicator**: Look for the laptop 💻 or phone 📱 icon in the address bar
   - Laptop icon = currently in desktop mode
   - Phone icon = currently in mobile mode

2. **Tap to toggle**:
   - Tap laptop icon → switches to mobile mode (icon changes to phone)
   - Tap phone icon → switches to desktop mode (icon changes to laptop)

3. **Instant switch**: Mode changes immediately, webpage reloads with new user agent

#### Why This Matters

- **Faster Switching**: One tap instead of opening command bar
- **Visual Feedback**: Icon changes instantly to show new mode
- **Always Visible**: Available in both portrait and landscape

---

## Previous Release (v1.7.0 - 2025-12-02)

### Command Bar Layout - Non-Overlay Design

This release changes how the command bar is displayed so it doesn't cover your webpage:

#### Portrait Mode (Phone Held Upright)

| Before | After |
|--------|-------|
| Command bar floated over webpage | Command bar sits below webpage |
| Webpage stayed full height | Webpage resizes to fit |
| Bar could cover page content | Page content never covered |

**New Features:**
- **Hide Button**: Tap "Hide" to collapse command bar (webpage expands)
- **Show Button**: Small bar appears at bottom to bring it back
- **Enter Key**: Pressing Enter/Return in "Add Page" dialog submits the URL

#### Landscape Mode (Phone Held Sideways)

| Before | After |
|--------|-------|
| Command bar floated on right | Command bar fixed on side (left or right) |
| Webpage stayed full width | Webpage resizes to fit |
| No option to move | Can switch between left and right side |

**New Features:**
- **Side Position**: Command bar is fixed on left OR right side
- **Switch Side Button**: Tap "Side" to move bar to opposite side
- **Hide/Show**: Hide the bar completely; webpage fills the screen
- **Page Resizes**: Webpage adjusts its width when bar shows/hides

#### How to Use

**In Portrait Mode:**
1. Command bar appears at bottom of screen
2. Webpage is above the bar (not covered)
3. Tap "Hide" to collapse bar → webpage expands
4. Tap "Show Commands" bar at very bottom to restore

**In Landscape Mode:**
1. Command bar appears on right side by default
2. Webpage is beside the bar (not covered)
3. Tap "Side" to move bar to left side
4. Tap "Hide" to collapse bar → webpage fills screen
5. Tap arrow button on side to restore bar

#### Enter Key for URL Input

When you open the "Add Page" dialog:
- Type your URL
- Press **Enter** or **Return** key to submit (same as tapping "Add Page" button)
- No need to tap the button - keyboard submission works

---

## Previous Release (v1.6.0 - 2025-12-01)

### Command Bar Redesign - Simplified Navigation

This release completely redesigns the command bar for easier use:

#### Flat Menu Structure (Max 2 Levels)

| Before | After |
|--------|-------|
| 4 menu levels deep | 2 levels max |
| Required scrolling | No scrolling needed |
| Confusing sub-menus | Direct access to all features |

#### New Command Bar Layout

**MAIN Level** (what you see first):
| Button | Action |
|--------|--------|
| Back | Go back in browser history |
| Home | Go to home page |
| New | Open new tab |
| Scroll | Open scroll commands |
| Page | Open page commands |
| Menu | Open app menu |

**SCROLL Level** (tap Scroll):
| Button | Action |
|--------|--------|
| Close | Return to main |
| Up | Scroll up |
| Down | Scroll down |
| Top | Go to top of page |
| Bottom | Go to bottom of page |
| Freeze | Lock/unlock scrolling |

**PAGE Level** (tap Page):
| Button | Action |
|--------|--------|
| Close | Return to main |
| Prev | Previous page |
| Next | Next page |
| Reload | Refresh page |
| Desktop | Toggle desktop/mobile mode |
| Favorite | Add to favorites |

**MENU Level** (tap Menu):
| Button | Action |
|--------|--------|
| Close | Return to main |
| Bookmarks | Open bookmarks |
| Downloads | Open downloads |
| History | Open history |
| Settings | Open settings |

#### Benefits

- **2 taps max** - Any action reachable in 2 taps
- **No scrolling** - All buttons visible at once
- **Clear labels** - Every button has text label under icon
- **Consistent** - Same pattern across all levels

---

## Previous Release (v1.5.0 - 2025-12-01)

### UI Enhancements & Bug Fixes

This release focuses on usability improvements and critical bug fixes:

#### Favorites & Tabs Dropdown Redesign

| Feature | Change |
|---------|--------|
| Favorites Dropdown | Ocean theme with prominent "+ Add" button in header |
| Tabs Dropdown | Ocean theme with "+ New" button and active tab indicator |
| Add Favorite | Now easily accessible from dropdown header |

**Adding Favorites (New Method):**
1. Tap bookmark icon in address bar
2. Dropdown shows existing favorites
3. Tap "+ Add" button in dropdown header
4. Edit title/URL in dialog
5. Save

#### Tab Pinning via Long-Press

| Action | Result |
|--------|--------|
| Long-press tab in Tab Switcher | Pin/unpin tab |
| Pinned tabs | Appear in "Pinned" category |
| Visual indicator | Pinned badge on tab card |

**How to Pin Tabs:**
1. Open Tab Switcher (tap tab count badge)
2. Long-press any tab thumbnail
3. Tab is pinned (or unpinned if already pinned)
4. View pinned tabs in "Pinned" category tab

#### Desktop Mode Button Improvements

| Before | After |
|--------|-------|
| Icon only, no label | Icon + label ("Desktop" or "Mobile") |
| Unclear state | Clear visual state indicator |

#### Command Bar Layout Fix

| Issue | Fix |
|-------|-----|
| Left button cut off at screen edge | Added 8dp horizontal padding |
| Buttons partially hidden | Full visibility guaranteed |

#### Tab Management Stability

| Bug | Resolution |
|-----|------------|
| Tabs stuck after removing last tab | Fixed ViewModel lifecycle management |
| Cannot add new tabs after removal | ViewModels persist correctly across navigation |
| Home button not working | Same root cause - now fixed |

**Technical:** ViewModels are no longer incorrectly cleared when navigating to Settings/History screens.

---

## Previous Release (v1.4.0 - 2025-12-01)

### Phase 5: Ocean Blue UI & Adaptive Layout

This release introduces a comprehensive UI overhaul with the Ocean Blue Glassmorphism theme and adaptive layouts:

#### New Design System - Ocean Blue Glassmorphism

| Element | Color | Description |
|---------|-------|-------------|
| Background | `#0F172A` | Deep slate |
| Surface | `#1E293B` | Cards, panels |
| Primary | `#3B82F6` | Accent blue |
| Text Primary | `#E2E8F0` | White 90% |
| Text Secondary | `#CBD5E1` | White 80% |

#### Tab Switcher Improvements

1. **Adaptive Layout**
   - Portrait: 2-column grid with 140dp cards
   - Landscape: 4-column grid with 100dp compact cards

2. **View Mode Toggle**
   - Grid View: Thumbnail cards (default)
   - List View: Compact single-row items

3. **Category Tabs**
   - All: Shows all open tabs
   - Pinned: Shows pinned tabs only
   - Recent: Shows 10 most recent tabs
   - Groups: Shows tabs in groups

#### Command Bar Enhancements

1. **Dismiss/Show Toggle**
   - "Hide" button to dismiss command bar
   - Floating button to bring it back

2. **Landscape Mode**
   - Vertical command bar on right side
   - Optimized for landscape browsing

#### Address Bar Updates

1. **Favorite Indicator**
   - Rounded square indicator on LEFT side of URL
   - Shows when current page is in favorites
   - Gold bookmark icon on amber background

2. **Favorites Menu Button**
   - BookmarkBorder icon on right side
   - Opens favorites dropdown menu

3. **URL Input Fixes**
   - Select-all on focus (tap URL to select all)
   - No more URL loop when typing
   - Enter key triggers search

4. **DuckDuckGo Search**
   - Non-URL text searches with DuckDuckGo
   - Privacy-focused search engine

#### Tab Counter Badge

- Ocean theme rounded rectangle design
- Elevated surface background
- Subtle border styling
- Matches overall theme

---

## Previous Release (v1.3.0 - 2025-11-30)

### Phase 4: Bug Fixes from User Testing

This release fixes 4 bugs identified during user testing:

1. **Favorite Toggle Fixed** - Tapping the star icon on an already-favorited page now removes it from favorites (previously only added, never removed).

2. **File Downloads Working** - Clicking download links now triggers the Android DownloadManager with proper filename detection, MIME type handling, and notification when complete.

3. **Theme Changes Apply Immediately** - Changing theme in Settings (Light/Dark/System/Auto) now applies instantly without needing to restart the app.

4. **Global Desktop Mode Working** - Enabling "Use Desktop Mode" in Settings now applies to all new tabs (previously only worked per-tab toggle).

**Technical Changes:**
- `BrowserScreen.kt` - Added `isFavorite()` check before toggle
- `WebViewContainer.android.kt` - Added `setDownloadListener()` with DownloadManager integration
- `BrowserApp.kt` - Now observes `settings.theme` StateFlow for dynamic theme switching
- `TabViewModel.kt` / `Tab.kt` - Added `isDesktopMode` parameter to `createTab()` flow

---

## Previous Release (v1.2.0 - 2025-11-28)

### Phase 3: Security & Privacy Enhancements

This release adds comprehensive security and privacy features:

1. **HTTP Authentication Dialog** - Modern Material Design 3 dialog for HTTP Basic/Digest authentication with username/password fields, realm information, and sign-in validation.

2. **File Upload Support** - Native file picker integration for HTML `<input type="file">` elements with MIME type filtering (images, PDFs, etc.) and multiple file selection support.

3. **Site Permissions Management** - New settings screen to view and revoke permissions (Camera, Microphone, Location) granted to websites. Features include:
   - Permissions grouped by domain
   - Individual permission revocation
   - Clear all permissions for a site
   - Visual indicators for granted/denied status

**Security Improvements:**
- CWE-295: Proper SSL certificate validation with user-facing error dialogs
- CWE-276: User consent required for all permission requests
- CWE-1021: JavaScript dialog spam prevention (max 3 per 10 seconds)
- File access limited to user-selected files only
- Permission persistence across sessions

### Previous Release (v1.1.1 - 2025-11-26)

### Deep Bug Fixes - Root Cause Resolution

This release provides **permanent fixes** for 8 persistent issues by addressing root causes:

1. **Tab History Now Truly Preserved** - Fixed AndroidView lifecycle issue where `factory` lambda only ran once. Added `key(tabId)` wrapper to force WebView recreation per tab while WebViewPool caches actual WebView instances.

2. **Tabs/Favorites Restore on Startup** - Fixed repository initialization bug where StateFlows started empty. Added `init` block to `BrowserRepositoryImpl` to load data from SQLDelight database on creation.

3. **Command Bar Scrolls in All Levels** - Fixed scroll state persistence across level changes. Added `LaunchedEffect` to reset scroll position when switching between MAIN, SCROLL, ZOOM, etc. levels.

4. **Desktop Mode Works Properly** - Fixed by Issue #1 - WebViewController now maintains correct WebView reference per tab.

5. **Tab Nav Buttons Hidden on Fresh Install** - Changed default `tabCount` parameter from 1 to 0 to properly hide Previous/Next when no tabs exist.

6. **Keyboard Closes Reliably** - Added `LocalFocusManager.clearFocus()` as fallback when `LocalSoftwareKeyboardController` returns null.

### Previous Release (v1.1.0 - 2025-11-25)

1. **Tab History Preserved** - Initial WebViewPool implementation
2. **Tabs Restore on Startup** - ViewModels wrapped in `remember{}`
3. **Favorites Load on Startup** - Same fix as #2
4. **Command Bar Scrollable** - Added `horizontalScroll()` modifier
5. **Updated Icons** - New icons for Voice (🎤), Desktop Mode (💻), Downloads (⬇️), Drag (👆)
6. **Desktop Mode Fixed** - Added page reload after user agent change
7. **Smart Tab Navigation** - Conditional on tabCount >= 2
8. **Menu Access** - Menu button with MoreVert icon added
9. **Keyboard Auto-Close** - LocalSoftwareKeyboardController integration

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Voice Commands](#voice-commands)
3. [Navigation](#navigation)
4. [Favorites](#favorites)
5. [Scroll Controls](#scroll-controls)
6. [Zoom Controls](#zoom-controls)
7. [Desktop Mode](#desktop-mode)
8. [Security & Privacy](#security--privacy)
9. [Advanced Features](#advanced-features)

---

## Getting Started

WebAvanue is a voice-first browser designed for hands-free operation with full touch/click support as backup.

### First Launch

1. **Home Screen**: Opens with address bar and empty tab
2. **Voice Button**: Tap microphone icon or say "Hey Browser"
3. **Address Bar**: Type URL or say "Go to [website]"

---

## Voice Commands

### Getting Help

| Voice Command | Action |
|---------------|--------|
| "Help" | Show categorized voice commands help dialog |
| "Show help" | Show categorized voice commands help dialog |
| "Commands" | Show categorized voice commands help dialog |
| "Show commands" | Show categorized voice commands help dialog |

**New in v1.9.0:** The help dialog is now fully interactive! You can:
- **Tap any command** to execute it directly
- **Navigate using voice** - Say category names like "tabs", "zoom", "modes"
- **Action commands** auto-dismiss the dialog after execution
- **Adjustment commands** (like zoom) keep the dialog open for multiple changes

#### Using the Interactive Help Dialog

**Method 1: Voice Navigation**
1. Say "help" or tap the FAB (? button)
2. Say a category name: "navigation", "tabs", "zoom", "modes", or "features"
3. Say a command name or tap it to execute

**Method 2: Touch Navigation**
1. Tap the FAB (? button)
2. Tap any category card (Navigation, Tabs, Zoom, etc.)
3. Tap any command to execute it immediately

**Smart Auto-Dismiss:**
- **Action commands** (back, forward, new tab, desktop mode) → Execute and close dialog
- **Adjustment commands** (zoom in, zoom out) → Execute but stay open for more changes
- **Close button** or **tap outside** → Dismiss dialog manually

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

**Method 1: Favorites Dropdown (v1.5.0 - Recommended)**
1. Tap bookmark icon in address bar
2. Dropdown shows existing favorites
3. Tap "+ Add" button in dropdown header
4. Edit title/URL/description if needed
5. Tap "Save"

**Method 2: Star Icon**
1. Navigate to page you want to save
2. Tap star icon in address bar (if current page shown)
3. Edit title/URL/description if needed
4. Tap "Save"

**Method 3: Voice Command**
1. Navigate to page
2. Say "Add to favorites"
3. Confirm details in dialog

**Method 4: Favorites Bar**
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
- Page reloads automatically with new user agent

**Method 2: Voice Commands Dialog** (v1.9.0)
- Tap FAB (? button) → "Modes" → Tap "desktop mode" or "mobile mode"
- Executes mode change, reloads page, and closes dialog

**Method 3: Address Bar**
- Tap the laptop/phone icon in address bar to toggle
- Page reloads automatically to apply new mode

**Method 4: Command Bar**
1. Tap command bar at bottom
2. Tap "Page" menu
3. Tap "Desktop" toggle
4. Page reloads automatically

**New in v1.9.0:** When you toggle desktop/mobile mode:
- Current page **reloads automatically** with new user agent
- **Stays on same website** (doesn't navigate to home)
- See the desktop/mobile version immediately

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

## Security & Privacy

WebAvanue takes security and privacy seriously. All security-critical actions require explicit user consent.

### SSL/TLS Certificate Warnings

When you visit a website with an invalid SSL certificate, WebAvanue will show a warning dialog.

**Warning Information Displayed:**
- Error type (expired, untrusted CA, hostname mismatch)
- Website URL
- Certificate details (issuer, validity period, fingerprint)
- Security risk explanation

**Your Options:**
1. **Go Back** (Recommended) - Navigate away from the unsafe site
2. **Proceed Anyway** (Dangerous) - Continue to the site despite the risk

**Certificate Error Types:**
- **Expired Certificate**: Website's security certificate has expired
- **Untrusted CA**: Certificate is not from a trusted authority
- **Hostname Mismatch**: Certificate doesn't match the website domain
- **Invalid Date**: Certificate has invalid date information

**Best Practice:** Always choose "Go Back" unless you fully trust the website and understand the risks.

### Site Permissions

Websites can request access to your device features like camera, microphone, and location.

#### Granting Permissions

When a website requests permission, you'll see a dialog showing:
- Website domain making the request
- Requested permissions (Camera, Microphone, Location, etc.)
- Clear explanation of what the permission allows
- "Remember my choice" checkbox

**Your Options:**
- **Allow** - Grant permission for this session
- **Allow + Remember** - Grant permission and remember for future visits
- **Deny** - Refuse permission request

**Spam Protection:** WebAvanue blocks excessive permission requests (max 3 per 10 seconds).

#### Managing Permissions

**To view/revoke granted permissions:**

1. Open Settings (Menu → Settings)
2. Tap "Site Permissions" in Privacy & Security section
3. View permissions grouped by domain
4. Tap delete (X) icon to revoke individual permissions
5. Tap trash icon to clear all permissions for a domain

**Permission Types:**
- 📹 **Camera**: Access to device camera for photos/video
- 🎤 **Microphone**: Access to microphone for audio recording
- 📍 **Location**: Access to device GPS location
- 🔒 **Protected Media**: Access to DRM-protected media playback

**Permission States:**
- ✅ **Allowed**: Permission is granted
- ❌ **Denied**: Permission was denied

### File Uploads

Websites can request file uploads for forms and attachments.

**When a website requests file upload:**
1. HTML `<input type="file">` element triggers file picker
2. Native Android file picker opens
3. Select one or more files based on allowed types
4. Selected files are uploaded to the website

**Features:**
- **MIME Type Filtering**: File picker shows only allowed file types
  - Example: `accept="image/*"` shows only images
  - Example: `accept=".pdf"` shows only PDFs
- **Multiple Selection**: Supported for `<input type="file" multiple>`
- **Secure**: Only user-selected files are accessible

**Supported File Types:**
- Images (JPEG, PNG, GIF, WebP)
- Documents (PDF, DOC, TXT)
- Archives (ZIP, RAR)
- All file types (`*/*`)

### HTTP Authentication

Some websites require username/password authentication (HTTP Basic or Digest).

**When prompted for authentication:**

1. **Authentication Dialog** appears showing:
   - Server hostname
   - Authentication realm (security scope)
   - Username input field
   - Password input field (hidden)
   - Authentication scheme (Basic or Digest)

2. **Enter Credentials:**
   - Type your username
   - Type your password
   - Both fields must be filled to enable "Sign In"

3. **Submit:**
   - Tap "Sign In" to authenticate
   - Tap "Cancel" to abort

**Security Notes:**
- Passwords are never stored by WebAvanue
- Credentials are transmitted securely over HTTPS (if available)
- HTTP Basic authentication is not encrypted over HTTP
- Always verify the website's SSL certificate before entering credentials

### JavaScript Dialogs

Websites can show JavaScript dialogs (alert, confirm, prompt).

**Dialog Types:**

1. **Alert Dialog** (`window.alert()`)
   - Shows message from website
   - Single "OK" button
   - Used for notifications

2. **Confirm Dialog** (`window.confirm()`)
   - Shows yes/no question
   - "OK" and "Cancel" buttons
   - Returns true/false to website

3. **Prompt Dialog** (`window.prompt()`)
   - Shows input field
   - Text input with "OK" and "Cancel"
   - Returns entered text to website

**Spam Protection:**
- Maximum 3 dialogs per 10 seconds
- Excessive dialogs are automatically blocked
- Warning shown when approaching limit

**Best Practice:** Be cautious of websites that show excessive dialogs - they may be malicious.

### Privacy Settings

**Access Privacy Settings:**
Settings → Privacy & Security

**Available Settings:**
- **Enable JavaScript**: Allow/block JavaScript execution
- **Enable Cookies**: Allow/block website cookies
- **Block Pop-ups**: Prevent pop-up windows
- **Block Ads**: Block advertisements (experimental)
- **Block Trackers**: Prevent cross-site tracking
- **Clear History on Exit**: Auto-delete browsing history
- **Site Permissions**: Manage camera/mic/location permissions

**Clear Data Options:**
- **Clear Cache**: Remove cached images and files
- **Clear Cookies**: Log out of all websites
- **Clear History**: Delete browsing history
- **Clear Permissions**: Revoke all site permissions

---

## Advanced Features

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
2. **Pinned Tabs**: Long-press tabs to pin - they persist and appear in "Pinned" category
3. **Favorites Folders**: Organize favorites by category
4. **Desktop Mode Per-Tab**: Mix mobile/desktop sites
5. **Zoom Per-Tab**: Different zoom for different sites

---

## Troubleshooting

### Verifying Bug Fixes After Updates

When you update WebAvanue to a new version, follow these steps to verify bug fixes are working:

#### 1. Check Version Number

**Location:** Settings → About

Verify you're running the correct version:
- **v1.7.0** - Latest release with non-overlay command bar layout
- **v1.6.0** - Command bar redesign (flat menu structure)
- **v1.5.0** - UI enhancements & stability fixes
- **v1.4.0** - Ocean Blue UI & adaptive layout
- **v1.3.0** - Security & privacy enhancements

#### 2. Test Each Fixed Issue

| Fix | How to Test | Expected Result |
|-----|-------------|-----------------|
| **Non-Overlay Layout** (v1.7.0) | Open command bar in portrait | Bar at bottom, webpage resizes (not covered) |
| **Side Command Bar** (v1.7.0) | Rotate to landscape | Bar on side, webpage resizes (not covered) |
| **Switch Side** (v1.7.0) | In landscape, tap "Side" button | Bar moves to opposite side |
| **Hide/Show** (v1.7.0) | Tap "Hide" then "Show Commands" | Bar hides, webpage expands, bar returns |
| **Enter Key Submit** (v1.7.0) | Open Add Page dialog, type URL, press Enter | Dialog submits, new tab opens |
| **Flat Menu** (v1.6.0) | Open command bar → count levels needed | Max 2 taps to reach any action |
| **No Scrolling** (v1.6.0) | Open any command bar level | All 6 buttons visible without scrolling |
| **Command Labels** (v1.6.0) | Look at command bar buttons | Each button shows icon + text label below |
| **Favorites Add Button** (v1.5.0) | Tap bookmark icon → look for "+ Add" | Should see prominent "+ Add" button in dropdown header |
| **Tab Pinning** (v1.5.0) | Open Tab Switcher → long-press any tab | Tab should pin/unpin and appear in "Pinned" category |
| **Tab History** | Open site → navigate 2-3 pages → switch tabs → switch back → tap Back button | Should navigate to previous pages in history |
| **Tabs Restored** | Open 3+ tabs → close app → reopen app | All tabs should reappear with correct URLs |
| **Command Bar Scroll** | Open command bar → tap "Navigation" or "Web Commands" → swipe left/right | Command bar should scroll to show all buttons |
| **Desktop Mode** | Tap Desktop Mode → check page | Page should reload in desktop view immediately |
| **Keyboard Close** | Tap address bar → type → tap search | Keyboard should close automatically |

#### 3. If Fixes Don't Work

**Possible Cause:** You may have installed an outdated APK.

**Solution:**
1. Uninstall WebAvanue completely
2. Reinstall from official source
3. Verify version number (Settings → About)
4. Test fixes again

**Still Not Working?**
- Report issue with:
  - Version number
  - Device model
  - Android version
  - Which specific fix isn't working
  - Screenshots if possible

---

## Common Issues

### Website not loading?
- Check internet connection
- Try "Refresh"
- Clear cache and retry

### Text too small?
- Increase zoom level (Level 4 or 5)
- Or disable desktop mode

### Page not scrolling?
- Check if "Freeze Page" is active
- Look for red border (frozen indicator)
- Say "Freeze page" to unfreeze

### Login issues?
- Check if cookies are enabled (Settings → Privacy)
- Try clearing cookies and logging in again
- Verify username/password

### App crashes on startup?
- Clear app data (Settings → Apps → WebAvanue → Clear Data)
- Reinstall app
- Report crash with device details

### Voice commands not working?
- Check microphone permissions (Settings → Apps → WebAvanue → Permissions)
- Verify microphone is not muted
- Try using touch controls as backup

### Desktop mode toggle not working?
- Verify you're running v1.1.1 or later
- Try: Toggle desktop mode → manually reload page
- Some sites may override user agent

### Tabs disappearing after app restart?
- Verify you're running v1.1.1 or later
- Check storage permissions
- Try: Settings → Apps → WebAvanue → Permissions → Storage

### Command bar buttons cut off?
- Verify you're running v1.1.1 or later
- Try swiping left/right on command bar
- Make sure screen rotation is not interfering

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
- WebAvanue: 1.7.0
- Platform: Android / iOS / Desktop
- Database: SQLDelight 2.0.1
- UI: Compose Multiplatform 1.6.1

---

**Last Updated:** 2025-12-05
**Build Status:** ✅ Production Ready - v1.8.3 UI/UX Fixes
**© 2025 Augmentalis. All rights reserved.**

# VoiceOS Command Deduplication Analysis

**Document:** VoiceOS Command Deduplication Analysis
**Project:** WebAvanue Browser - Legacy Migration
**Date:** 2025-11-21
**Purpose:** Identify existing VoiceOS commands to avoid duplication in WebAvanue implementation

---

## Executive Summary

**Critical Finding:** VoiceOS CommandManager already has comprehensive voice command infrastructure. We must **integrate with existing commands** rather than create new ones.

**Key Discovery:**
- `browser-commands.vos` exists but is **EMPTY** (reserved for future use)
- Scrolling commands **already exist** in `scroll-commands.vos`
- Cursor/click commands **already exist** in `cursor-commands.vos` and `gesture-commands.vos`
- Drag commands **already exist** in `drag-commands.vos`
- Navigation commands **already exist** in `navigation-commands.vos`

---

## VoiceOS Command Files Analyzed

### 1. browser-commands.vos (EMPTY - Reserved)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/browser-commands.vos`

**Status:** 🟡 **Reserved for future use** - No commands defined yet

```json
{
  "schema": "vos-1.0",
  "version": "1.0.0",
  "file_info": {
    "filename": "browser-commands.vos",
    "category": "browser",
    "display_name": "Browser Control",
    "description": "Voice commands for web browser control (reserved for future use)",
    "command_count": 0
  },
  "locale": "en-US",
  "commands": []
}
```

**Implication:** This is WHERE we should add browser-specific commands (zoom, desktop mode, favorites, cookies, etc.)

---

### 2. scroll-commands.vos (2 commands)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/scroll-commands.vos`

**Status:** ✅ **Existing commands** - Already implemented

**Commands:**
1. **SCROLL_UP** - `"scroll up"` with 14 synonyms
   - Synonyms: page up, move up, upward scroll, scroll upward, go up, scroll upwards, page upwards, move upwards, go upwards, scroll up the page, move up the page, scroll it up, page up now, move it up

2. **SCROLL_DOWN** - `"scroll down"` with 14 synonyms
   - Synonyms: page down, move down, downward scroll, scroll downward, go down, scroll downwards, page downwards, move downwards, go downwards, scroll down the page, move down the page, scroll it down, page down now, move it down

**Missing from VoiceOS:**
- ❌ scroll left
- ❌ scroll right
- ❌ scroll to top
- ❌ scroll to bottom
- ❌ freeze page

**Recommendation:**
- ✅ **Reuse** SCROLL_UP and SCROLL_DOWN
- 📝 **Add to browser-commands.vos:** SCROLL_LEFT, SCROLL_RIGHT, SCROLL_TOP, SCROLL_BOTTOM, FREEZE_PAGE

---

### 3. cursor-commands.vos (7 commands)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/cursor-commands.vos`

**Status:** ✅ **Existing commands** - Already implemented

**Commands:**
1. **SELECT** - `"select"` (14 synonyms)
2. **HAND_CURSOR** - `"hand cursor"` (14 synonyms)
3. **NORMAL_CURSOR** - `"normal cursor"` (14 synonyms)
4. **CHANGE_CURSOR** - `"change cursor"` (14 synonyms)
5. **CENTER_CURSOR** - `"center cursor"` (14 synonyms)
6. **SHOW_CURSOR** - `"show cursor"` (14 synonyms)
7. **HIDE_CURSOR** - `"hide cursor"` (14 synonyms)

**Recommendation:**
- ✅ **Reuse all** - These are system-wide cursor commands that work in WebView

---

### 4. gesture-commands.vos (5 commands)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/gesture-commands.vos`

**Status:** ✅ **Existing commands** - Already implemented

**Commands:**
1. **PINCH_OPEN** - `"pinch open"` (14 synonyms including "zoom in", "expand view")
2. **PINCH_CLOSE** - `"pinch close"` (14 synonyms including "zoom out", "shrink view")
3. **SINGLE_CLICK** - `"single click"` (14 synonyms)
4. **DOUBLE_CLICK** - `"double click"` (14 synonyms)
5. **LONG_PRESS** - `"long press"` (14 synonyms)

**Recommendation:**
- ✅ **Reuse** PINCH_OPEN and PINCH_CLOSE for zoom gestures
- ✅ **Reuse** SINGLE_CLICK and DOUBLE_CLICK for cursor controls
- 📝 **Add to browser-commands.vos:** ZOOM_IN (explicit), ZOOM_OUT (explicit), SET_ZOOM_LEVEL, ROTATE_VIEW

---

### 5. drag-commands.vos (3 commands)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/drag-commands.vos`

**Status:** ✅ **Existing commands** - Already implemented

**Commands:**
1. **DRAG_UP_DOWN** - `"drag up"` (14 synonyms including "drag down", "vertical drag")
2. **DRAG_START** - `"drag start"` (14 synonyms)
3. **DRAG_STOP** - `"drag stop"` (14 synonyms)

**Recommendation:**
- ✅ **Reuse all** - Legacy `toggleDrag()` maps to DRAG_START/DRAG_STOP

---

### 6. navigation-commands.vos (9 commands)

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/navigation-commands.vos`

**Status:** ✅ **Existing commands** - Already implemented

**Commands:**
1. **NAVIGATE_HOME** - `"navigate home"` (14 synonyms)
2. **OPEN_RECENT_APPS** - `"open recent apps"` (14 synonyms)
3. **OPEN_SETTINGS** - `"open settings"` (14 synonyms)
4. **OPEN_CONNECTION** - `"open connections"` (14 synonyms)
5. **OPEN_SOUND** - `"open sound"` (14 synonyms)
6. **OPEN_DISPLAY** - `"open display"` (14 synonyms)
7. **OPEN_SECURITY** - `"open security"` (14 synonyms)
8. **OPEN_ABOUT** - `"open about"` (14 synonyms)
9. **GO_BACK** - `"navigate back"` (14 synonyms including "go back", "previous page")

**Recommendation:**
- ✅ **Reuse** GO_BACK for browser back navigation
- 📝 **Add to browser-commands.vos:** GO_FORWARD, RELOAD_PAGE, OPEN_URL, CLOSE_TAB, NEW_TAB, PREVIOUS_TAB, NEXT_TAB

---

## Command Deduplication Matrix

### Legacy Feature → VoiceOS Command Mapping

| Legacy WebView Function | Legacy Voice Command | Existing VoiceOS Command | Status | Action |
|-------------------------|---------------------|-------------------------|--------|--------|
| **SCROLLING** |||||
| `scrollUp()` | "scroll up" | ✅ **SCROLL_UP** (scroll-commands.vos) | Exists | **REUSE** |
| `scrollDown()` | "scroll down" | ✅ **SCROLL_DOWN** (scroll-commands.vos) | Exists | **REUSE** |
| `scrollLeft()` | "scroll left" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `scrollRight()` | "scroll right" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `scrollTop()` | "scroll to top" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `scrollBottom()` | "scroll to bottom" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `toggleFreezeFrame()` | "freeze page" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **ZOOM** |||||
| `zoomIn()` | "zoom in" | ⚠️ **PINCH_OPEN** (gesture-commands.vos) | Synonym exists | **ADD explicit ZOOM_IN** |
| `zoomOut()` | "zoom out" | ⚠️ **PINCH_CLOSE** (gesture-commands.vos) | Synonym exists | **ADD explicit ZOOM_OUT** |
| `setZoomLevel(1-5)` | "zoom level 1" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **DESKTOP MODE** |||||
| `setDesktopMode(true)` | "desktop mode" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `setDesktopMode(false)` | "mobile mode" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **FAVORITES** |||||
| `favoriteWebPage()` | "add to favorites" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `loadFavorite(url)` | "open favorite" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **COOKIES** |||||
| `clearCookies()` | "clear cookies" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **TAB NAVIGATION** |||||
| `nextWebView()` | "next tab" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `previousView()` | "previous tab" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `closeTab()` | "close tab" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `newTab()` | "new tab" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **PAGE NAVIGATION** |||||
| `previousPage()` | "go back" | ✅ **GO_BACK** (navigation-commands.vos) | Exists | **REUSE** |
| `nextPage()` | "go forward" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `reload()` | "reload page" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `loadUrl(url)` | "open url" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **CURSOR** |||||
| `singleClick()` | "click" | ✅ **SINGLE_CLICK** (gesture-commands.vos) | Exists | **REUSE** |
| `doubleClick()` | "double click" | ✅ **DOUBLE_CLICK** (gesture-commands.vos) | Exists | **REUSE** |
| **TOUCH/GESTURE** |||||
| `toggleDrag()` | "drag start" | ✅ **DRAG_START/DRAG_STOP** (drag-commands.vos) | Exists | **REUSE** |
| `pinchOpen()` | "pinch open" | ✅ **PINCH_OPEN** (gesture-commands.vos) | Exists | **REUSE** |
| `pinchClose()` | "pinch close" | ✅ **PINCH_CLOSE** (gesture-commands.vos) | Exists | **REUSE** |
| `rotateView()` | "rotate view" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| **OTHER** |||||
| `proceedBasicAuth()` | "proceed auth" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |
| `startScan()` | "scan qr code" | ❌ Not defined | Missing | **ADD to browser-commands.vos** |

---

## Summary Statistics

### ✅ Commands to REUSE (11 existing)
1. SCROLL_UP (scroll-commands.vos)
2. SCROLL_DOWN (scroll-commands.vos)
3. GO_BACK (navigation-commands.vos)
4. SELECT (cursor-commands.vos)
5. SINGLE_CLICK (gesture-commands.vos)
6. DOUBLE_CLICK (gesture-commands.vos)
7. PINCH_OPEN (gesture-commands.vos)
8. PINCH_CLOSE (gesture-commands.vos)
9. DRAG_START (drag-commands.vos)
10. DRAG_STOP (drag-commands.vos)
11. DRAG_UP_DOWN (drag-commands.vos)

### 📝 Commands to ADD to browser-commands.vos (28 missing)

**Scrolling (4):**
1. SCROLL_LEFT
2. SCROLL_RIGHT
3. SCROLL_TOP
4. SCROLL_BOTTOM
5. FREEZE_PAGE

**Zoom (6):**
6. ZOOM_IN (explicit)
7. ZOOM_OUT (explicit)
8. ZOOM_LEVEL_1
9. ZOOM_LEVEL_2
10. ZOOM_LEVEL_3
11. ZOOM_LEVEL_4
12. ZOOM_LEVEL_5

**Desktop Mode (2):**
13. DESKTOP_MODE_ON
14. DESKTOP_MODE_OFF

**Favorites (2):**
15. ADD_TO_FAVORITES
16. OPEN_FAVORITE

**Cookies (1):**
17. CLEAR_COOKIES

**Tab Navigation (4):**
18. NEW_TAB
19. CLOSE_TAB
20. NEXT_TAB
21. PREVIOUS_TAB

**Page Navigation (3):**
22. GO_FORWARD
23. RELOAD_PAGE
24. OPEN_URL

**Gestures (1):**
25. ROTATE_VIEW

**Authentication (1):**
26. PROCEED_AUTH

**QR Scanner (1):**
27. SCAN_QR_CODE

**Other (1):**
28. TOGGLE_FULLSCREEN

---

## VoiceOS Database Schema

**File:** `VoiceCommandEntity.kt`

**Database Table:** `voice_commands`

**Entity Structure:**
```kotlin
@Entity(
    tableName = "voice_commands",
    indices = [
        Index(value = ["id", "locale"], unique = true),
        Index(value = ["locale"]),
        Index(value = ["is_fallback"])
    ]
)
data class VoiceCommandEntity(
    val uid: Long,           // Auto-generated primary key
    val id: String,          // Action ID (e.g., "SCROLL_UP")
    val locale: String,      // Locale code (e.g., "en-US")
    val primaryText: String, // Primary command text
    val synonyms: String,    // JSON array of synonyms
    val description: String, // Command description
    val category: String,    // Command category
    val priority: Int = 50,  // Priority (1-100)
    val isFallback: Boolean, // English fallback flag
    val createdAt: Long      // Timestamp
)
```

**Key Insights:**
- Commands are stored with **locale support** (multi-language ready)
- **Synonyms** stored as JSON array (extensible)
- **Priority system** for conflict resolution (1-100, higher wins)
- **Unique constraint** on (id, locale) pair
- **Category** derived from action ID prefix (e.g., "navigate_forward" → "navigate")

---

## Implementation Strategy

### Phase 1: WebAvanue Integration with Existing Commands (Week 1)

**Goal:** Make WebAvanue respond to existing VoiceOS commands

**Tasks:**
1. Register WebAvanue as command handler in VoiceOS CommandManager
2. Map existing commands to WebView actions:
   - SCROLL_UP → `webView.scrollBy(0, -100)`
   - SCROLL_DOWN → `webView.scrollBy(0, 100)`
   - GO_BACK → `tabViewModel.goBack()`
   - PINCH_OPEN → `webView.zoomIn()`
   - PINCH_CLOSE → `webView.zoomOut()`
   - SINGLE_CLICK → `webView.performClick()`
   - DOUBLE_CLICK → `webView.performClick() x2`
   - DRAG_START → Enable drag mode
   - DRAG_STOP → Disable drag mode

**Files to Modify:**
- `WebViewController.kt` - Add command handlers
- `TabViewModel.kt` - Add command routing
- `WebViewInteractor.android.kt` - Implement WebView interactions

**Testing:**
- Verify all existing commands work in WebAvanue
- Test voice recognition → command execution flow
- Validate command history tracking

---

### Phase 2: Add Missing Commands to browser-commands.vos (Week 2)

**Goal:** Populate `browser-commands.vos` with 28 missing commands

**Template for New Commands:**
```json
{
  "action": "SCROLL_LEFT",
  "cmd": "scroll left",
  "syn": [
    "move left", "leftward scroll", "scroll leftward", "go left",
    "scroll to the left", "move to the left", "scroll it left",
    "page left", "shift left", "pan left", "slide left",
    "navigate left", "move leftwards", "scroll leftwards"
  ]
}
```

**File to Update:**
- `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/commands/vos/browser-commands.vos`

**Process:**
1. Update `command_count` from 0 to 28
2. Add all 28 commands with 10-14 synonyms each
3. Follow VOS-1.0 schema format
4. Test command loading with `CommandLoader.kt`
5. Verify database insertion via `VoiceCommandDao.kt`

---

### Phase 3: Implement Browser-Specific Command Handlers (Weeks 3-4)

**Goal:** Implement WebView actions for new commands

**Priority 1 - Core (10 commands):**
1. SCROLL_LEFT/RIGHT/TOP/BOTTOM
2. ZOOM_IN/ZOOM_OUT (explicit)
3. GO_FORWARD
4. RELOAD_PAGE
5. CLEAR_COOKIES
6. FREEZE_PAGE

**Priority 2 - UX (8 commands):**
1. DESKTOP_MODE_ON/OFF
2. ADD_TO_FAVORITES
3. OPEN_FAVORITE
4. NEW_TAB
5. CLOSE_TAB
6. NEXT_TAB/PREVIOUS_TAB

**Priority 3 - Advanced (10 commands):**
1. ZOOM_LEVEL_1 through ZOOM_LEVEL_5
2. ROTATE_VIEW
3. PROCEED_AUTH
4. SCAN_QR_CODE
5. OPEN_URL
6. TOGGLE_FULLSCREEN

---

## Command Routing Architecture

### Flow: Voice Input → WebAvanue Action

```
[User Voice] "scroll down"
    ↓
[Speech Recognizer] (Android)
    ↓
[CommandManager] - Matches "scroll down" → SCROLL_DOWN action
    ↓
[CommandRegistry] - Looks up registered handlers
    ↓
[IntentDispatcher] - Routes to WebAvanue handler
    ↓
[WebAvanueCommandHandler] - Receives SCROLL_DOWN
    ↓
[TabViewModel] - Calls scrollDown()
    ↓
[WebViewController] - Executes webView.scrollBy(0, 100)
    ↓
[WebView] - Scrolls content down
    ↓
[CommandHistory] - Logs execution result
```

### Required Components

**1. WebAvanueCommandHandler.kt** (NEW FILE)
```kotlin
class WebAvanueCommandHandler(
    private val tabViewModel: TabViewModel,
    private val context: Context
) : CommandHandler {

    override fun handleCommand(command: Command): CommandResult {
        return when (command.action) {
            "SCROLL_UP" -> tabViewModel.scrollUp()
            "SCROLL_DOWN" -> tabViewModel.scrollDown()
            "SCROLL_LEFT" -> tabViewModel.scrollLeft()
            "SCROLL_RIGHT" -> tabViewModel.scrollRight()
            "SCROLL_TOP" -> tabViewModel.scrollTop()
            "SCROLL_BOTTOM" -> tabViewModel.scrollBottom()
            "GO_BACK" -> tabViewModel.goBack()
            "GO_FORWARD" -> tabViewModel.goForward()
            "RELOAD_PAGE" -> tabViewModel.reload()
            "PINCH_OPEN" -> tabViewModel.zoomIn()
            "PINCH_CLOSE" -> tabViewModel.zoomOut()
            "ZOOM_IN" -> tabViewModel.zoomIn()
            "ZOOM_OUT" -> tabViewModel.zoomOut()
            "ZOOM_LEVEL_1" -> tabViewModel.setZoomLevel(75)
            "ZOOM_LEVEL_2" -> tabViewModel.setZoomLevel(100)
            "ZOOM_LEVEL_3" -> tabViewModel.setZoomLevel(125)
            "ZOOM_LEVEL_4" -> tabViewModel.setZoomLevel(150)
            "ZOOM_LEVEL_5" -> tabViewModel.setZoomLevel(200)
            "DESKTOP_MODE_ON" -> tabViewModel.setDesktopMode(true)
            "DESKTOP_MODE_OFF" -> tabViewModel.setDesktopMode(false)
            "FREEZE_PAGE" -> tabViewModel.toggleFreeze()
            "CLEAR_COOKIES" -> tabViewModel.clearCookies()
            "NEW_TAB" -> tabViewModel.createNewTab()
            "CLOSE_TAB" -> tabViewModel.closeCurrentTab()
            "NEXT_TAB" -> tabViewModel.switchToNextTab()
            "PREVIOUS_TAB" -> tabViewModel.switchToPreviousTab()
            "ADD_TO_FAVORITES" -> tabViewModel.addToFavorites()
            "SINGLE_CLICK" -> tabViewModel.performClick()
            "DOUBLE_CLICK" -> tabViewModel.performDoubleClick()
            "DRAG_START" -> tabViewModel.startDrag()
            "DRAG_STOP" -> tabViewModel.stopDrag()
            "ROTATE_VIEW" -> tabViewModel.rotateView()
            else -> CommandResult.error("Unknown command: ${command.action}")
        }
    }
}
```

**2. CommandManager Registration** (MODIFY EXISTING)
```kotlin
// In WebAvanueModule initialization
CommandManager.getInstance(context).registerHandler(
    namespace = "com.augmentalis.webavanue",
    category = "browser",
    handler = WebAvanueCommandHandler(tabViewModel, context),
    priority = CommandPriority.HIGH
)
```

---

## Conflict Detection & Resolution

**Potential Conflicts:**
- **PINCH_OPEN** vs **ZOOM_IN**: Both zoom in
  - Resolution: Use same handler, different synonyms
- **SCROLL_UP** (system-wide) vs browser-specific scrolling
  - Resolution: Context-aware routing (if WebAvanue is active, route to browser)

**CommandManager Conflict Detection:**
```kotlin
// From ConflictDetector.kt
interface ConflictDetector {
    fun detectConflicts(command: VoiceCommand): List<ConflictInfo>
    fun resolveConflict(conflicts: List<ConflictInfo>): VoiceCommand
}
```

**Priority System:**
- System commands: Priority 50 (default)
- App-specific commands: Priority 60-70
- User-defined commands: Priority 80+

**Context-Aware Routing:**
```kotlin
// CommandContextManager checks active app
if (currentApp == "com.augmentalis.webavanue") {
    route to WebAvanueCommandHandler
} else {
    route to SystemCommandHandler
}
```

---

## Testing Strategy

### Unit Tests (WebAvanueCommandHandlerTest.kt)
```kotlin
@Test
fun testScrollUpCommand() {
    val command = Command(action = "SCROLL_UP", text = "scroll up", source = VOICE)
    val result = commandHandler.handleCommand(command)

    verify(tabViewModel).scrollUp()
    assertTrue(result.success)
}

@Test
fun testUnknownCommand() {
    val command = Command(action = "INVALID_ACTION", text = "foo", source = VOICE)
    val result = commandHandler.handleCommand(command)

    assertFalse(result.success)
    assertEquals("Unknown command", result.error?.message)
}
```

### Integration Tests (VoiceOSIntegrationTest.kt)
```kotlin
@Test
fun testVoiceCommandToWebViewFlow() {
    // Simulate voice input
    commandManager.processVoiceInput("scroll down")

    // Verify command execution
    advanceTimeBy(500.milliseconds)

    // Check WebView scrolled
    val scrollY = webView.scrollY
    assertTrue(scrollY > 0)
}
```

---

## Migration Impact Analysis

### Commands Eliminated (11 already exist)
- **SCROLL_UP/DOWN**: Use existing scroll-commands.vos
- **GO_BACK**: Use existing navigation-commands.vos
- **PINCH_OPEN/CLOSE**: Use existing gesture-commands.vos
- **SINGLE_CLICK/DOUBLE_CLICK**: Use existing gesture-commands.vos
- **DRAG_START/STOP**: Use existing drag-commands.vos

### Commands to Implement (28 new)
- **Added to browser-commands.vos**: All browser-specific commands
- **No duplication**: All new commands are browser-specific
- **No conflicts**: Context-aware routing prevents conflicts

### Development Time Savings
- **Before deduplication**: 39 commands to implement
- **After deduplication**: 28 commands to implement
- **Savings**: 28% reduction (11 commands reused)
- **Time saved**: ~2 weeks of development + testing

---

## Next Steps

1. ✅ **Analysis Complete** - This document
2. 📝 **Update IDEACODE Specs** - Remove duplicate commands, add VoiceOS integration
3. 🔨 **Implement WebAvanueCommandHandler** - Week 1
4. 📝 **Populate browser-commands.vos** - Week 2
5. 🧪 **Test VoiceOS Integration** - Week 3
6. 🚀 **Deploy to Sprint 1** - Week 4

---

## References

**VoiceOS CommandManager Documentation:**
- `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/docs/CommandManager-Master-Inventory.md`
- `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/docs/CommandManager-Architecture-Map.md`

**VOS Command Files:**
- `browser-commands.vos` (empty - reserved)
- `scroll-commands.vos` (2 commands)
- `cursor-commands.vos` (7 commands)
- `gesture-commands.vos` (5 commands)
- `drag-commands.vos` (3 commands)
- `navigation-commands.vos` (9 commands)

**WebAvanue Migration Plan:**
- `/Volumes/M-Drive/Coding/MainAvanues/Modules/WebAvanue/docs/planning/legacy-migration-plan.md`
- `/Volumes/M-Drive/Coding/MainAvanues/Modules/WebAvanue/FEATURE-COMPARISON.md`

---

**Last Updated:** 2025-11-21
**Status:** ✅ Complete - Ready for implementation

# Overlay System Integration Specification

**Date**: 2025-11-13
**Status**: Implementation Ready
**Priority**: P2 - HIGH IMPACT (User Experience)
**Effort**: 2-3 hours (previously estimated 2-3 weeks - corrected)

---

## Executive Summary

**Objective**: Integrate existing OverlayManager system into NumberHandler, SelectHandler, and HelpMenuHandler to replace placeholder TODO implementations.

**Current State**:
- ✅ OverlayManager exists and is production-ready (300+ lines)
- ✅ NumberedSelectionOverlay implemented with Material 3 design
- ✅ ContextMenuOverlay implemented with voice-selectable numbers
- ✅ CommandStatusOverlay ready for command feedback
- ❌ NOT integrated into handlers (3 files with 10 TODOs)

**Target State**:
- ✅ NumberHandler uses NumberedSelectionOverlay for element selection
- ✅ SelectHandler uses ContextMenuOverlay for context menus
- ✅ HelpMenuHandler uses CommandStatusOverlay (or custom HelpOverlay)
- ✅ All 10 TODOs removed
- ✅ Full integration testing completed

---

## Architecture Analysis

### Existing Overlay System

```
OverlayManager (Singleton)
├── NumberedSelectionOverlay
│   ├── Shows numbered badges on UI elements
│   ├── Voice selection: "select [number]"
│   ├── Material 3 circular badges (green/orange/grey)
│   └── Instruction panel at bottom
├── ContextMenuOverlay
│   ├── Shows context-aware menu
│   ├── Positioned at center or cursor location
│   ├── Voice selection by number or label
│   └── Material 3 card design
├── CommandStatusOverlay
│   ├── Shows command execution state
│   ├── States: LISTENING, PROCESSING, EXECUTING, SUCCESS, ERROR
│   └── Top-center position
└── ConfidenceOverlay
    ├── Shows recognition confidence
    ├── Real-time updates during recognition
    └── Top-right corner position
```

### Handler Integration Points

**NumberHandler.kt** (3 TODOs):
```kotlin
Line 450: TODO: Integrate with overlay system
Line 454: TODO: Implement actual overlay display
Line 462: TODO: Implement actual overlay hiding

Current: displayNumberOverlays() - logs only
Target: overlayManager.showNumberedSelection(items)
```

**SelectHandler.kt** (6 TODOs):
```kotlin
Line 190: TODO: Integrate with cursor manager to show context menu
Line 479: TODO: Integrate with cursor manager (isCursorVisible)
Line 484: TODO: Integrate with cursor manager (getCursorPosition)
Line 489: TODO: Implement basic context menu display
Line 495: TODO: Implement selection-specific context menu
Line 501: TODO: Implement general context menu

Current: Placeholder stubs returning false/null
Target: overlayManager.showContextMenuAt(items, position, title)
```

**HelpMenuHandler.kt** (1 TODO):
```kotlin
Line 401: TODO: Replace with proper overlay system integration

Current: showHelpToast(message) - uses Toast
Target: Custom HelpOverlay or CommandStatusOverlay
```

---

## Implementation Plan

### Phase 1: VoiceOSService Integration (30 min)

**File**: `VoiceOSService.kt`

**Changes Required**:
1. Add OverlayManager instance variable
2. Initialize in `onCreate()`
3. Add getter method for handlers to access
4. Dispose in `onDestroy()`

**Code**:
```kotlin
class VoiceOSService : AccessibilityService() {
    // Add overlay manager
    private lateinit var overlayManager: OverlayManager

    override fun onCreate() {
        super.onCreate()

        // Initialize overlay manager
        overlayManager = OverlayManager.getInstance(this)

        // ... existing initialization
    }

    /**
     * Get overlay manager for handlers
     */
    fun getOverlayManager(): OverlayManager {
        return overlayManager
    }

    override fun onDestroy() {
        // Dispose overlay manager
        overlayManager.dispose()

        // ... existing cleanup
        super.onDestroy()
    }
}
```

**Acceptance Criteria**:
- ✅ OverlayManager singleton initialized once
- ✅ All handlers can access via `service.getOverlayManager()`
- ✅ Disposed properly on service destruction

---

### Phase 2: NumberHandler Integration (45 min)

**File**: `NumberHandler.kt`

**Changes Required**:
1. Add overlay manager reference
2. Convert `displayNumberOverlays()` to use `NumberedSelectionOverlay`
3. Convert `hideNumberOverlays()` to use overlay manager
4. Map `ElementInfo` to `SelectableItem`
5. Remove all TODOs

**Code**:
```kotlin
class NumberHandler(
    private val service: VoiceOSService
) : ActionHandler {

    // Add overlay manager reference
    private val overlayManager: OverlayManager by lazy {
        service.getOverlayManager()
    }

    /**
     * Display number overlays using OverlayManager
     * REPLACES: Line 450-455 TODOs
     */
    private fun displayNumberOverlays() {
        if (numberedElements.isEmpty()) {
            Log.w(TAG, "No elements to display numbers for")
            return
        }

        // Convert ElementInfo to SelectableItem
        val items = numberedElements.map { (number, elementInfo) ->
            SelectableItem(
                number = number,
                label = elementInfo.description,
                bounds = elementInfo.bounds,
                action = {
                    // Click the element when selected by voice
                    clickElement(elementInfo)
                    hideNumberOverlay()
                }
            )
        }

        // Show numbered selection overlay
        overlayManager.showNumberedSelection(items)

        ConditionalLogger.i(TAG) {
            "Displayed number overlays for ${numberedElements.size} elements"
        }
    }

    /**
     * Hide number overlays
     * REPLACES: Line 460-463 TODO
     */
    private fun hideNumberOverlays() {
        overlayManager.hideNumberedSelection()
        ConditionalLogger.d(TAG) { "Hid number overlays" }
    }
}
```

**Acceptance Criteria**:
- ✅ Number overlays displayed with Material 3 design
- ✅ Voice selection "select 1", "select 2" works
- ✅ Elements clickable via numbered selection
- ✅ Overlays hide properly on command
- ✅ All 3 TODOs removed

---

### Phase 3: SelectHandler Integration (60 min)

**File**: `SelectHandler.kt`

**Changes Required**:
1. Add overlay manager reference
2. Implement context menu display methods
3. Handle cursor position (with graceful fallback)
4. Create menu items for selection actions
5. Remove all 6 TODOs

**Code**:
```kotlin
class SelectHandler(
    private val service: VoiceOSService
) : ActionHandler {

    // Add overlay manager reference
    private val overlayManager: OverlayManager by lazy {
        service.getOverlayManager()
    }

    /**
     * Show context menu at cursor position
     * REPLACES: Line 190, 479, 484, 489 TODOs
     */
    private fun showContextMenuAtCursor(): Boolean {
        // Get cursor position (or use screen center as fallback)
        val position = getCursorPosition() ?: Point(
            service.resources.displayMetrics.widthPixels / 2,
            service.resources.displayMetrics.heightPixels / 2
        )

        // Create context menu items based on current selection
        val menuItems = createContextMenuItems()

        // Show menu at position
        overlayManager.showContextMenuAt(
            items = menuItems,
            position = position,
            title = if (isSelectionMode) "Selection Menu" else "Context Menu"
        )

        ConditionalLogger.i(TAG) { "Showed context menu at position: $position" }
        return true
    }

    /**
     * Check if cursor is visible
     * REPLACES: Line 479 TODO
     */
    private fun isCursorVisible(): Boolean {
        // Try to get cursor from VoiceCursor module
        // Fallback to false if cursor not available
        return try {
            // TODO: Integrate with VoiceCursor module when available
            false
        } catch (e: Exception) {
            ConditionalLogger.w(TAG, e) { "Failed to check cursor visibility" }
            false
        }
    }

    /**
     * Get cursor position
     * REPLACES: Line 484 TODO
     */
    private fun getCursorPosition(): Point? {
        return try {
            // TODO: Integrate with VoiceCursor module when available
            null  // Graceful fallback to screen center in caller
        } catch (e: Exception) {
            ConditionalLogger.w(TAG, e) { "Failed to get cursor position" }
            null
        }
    }

    /**
     * Create context menu items based on current state
     * REPLACES: Lines 489, 495, 501 TODOs
     */
    private fun createContextMenuItems(): List<MenuItem> {
        val items = mutableListOf<MenuItem>()

        if (isSelectionMode && currentSelection?.isTextSelection == true) {
            // Selection-specific menu
            items.add(MenuItem(
                id = "copy",
                label = "Copy",
                icon = Icons.Default.ContentCopy,
                number = 1,
                action = { performCopy() }
            ))

            items.add(MenuItem(
                id = "cut",
                label = "Cut",
                icon = Icons.Default.ContentCut,
                number = 2,
                action = { performCut() }
            ))

            items.add(MenuItem(
                id = "paste",
                label = "Paste",
                icon = Icons.Default.ContentPaste,
                number = 3,
                enabled = hasClipboardData(),
                action = { performPaste() }
            ))

            items.add(MenuItem(
                id = "select_all",
                label = "Select All",
                icon = Icons.Default.SelectAll,
                number = 4,
                action = { performSelectAll() }
            ))
        } else {
            // General context menu
            items.add(MenuItem(
                id = "back",
                label = "Go Back",
                icon = Icons.Default.ArrowBack,
                number = 1,
                action = { service.performGlobalAction(GLOBAL_ACTION_BACK) }
            ))

            items.add(MenuItem(
                id = "home",
                label = "Go Home",
                icon = Icons.Default.Home,
                number = 2,
                action = { service.performGlobalAction(GLOBAL_ACTION_HOME) }
            ))

            items.add(MenuItem(
                id = "recents",
                label = "Recent Apps",
                icon = Icons.Default.Apps,
                number = 3,
                action = { service.performGlobalAction(GLOBAL_ACTION_RECENTS) }
            ))
        }

        return items
    }
}
```

**Acceptance Criteria**:
- ✅ Context menu displays at cursor position (or center if unavailable)
- ✅ Selection menu shows copy/cut/paste when text selected
- ✅ General menu shows navigation when no selection
- ✅ Voice selection by number works ("select 1", "2", etc.)
- ✅ Graceful fallback when cursor not available
- ✅ All 6 TODOs removed

---

### Phase 4: HelpMenuHandler Integration (30 min)

**File**: `HelpMenuHandler.kt`

**Options for Help Display**:
1. **Option A**: Create custom HelpOverlay (2-3 hours - NOT optimal)
2. **Option B**: Use ContextMenuOverlay for help categories (30 min - RECOMMENDED)
3. **Option C**: Use CommandStatusOverlay for help text (15 min - quick but limited)

**Recommended: Option B - ContextMenuOverlay**

**Code**:
```kotlin
class HelpMenuHandler(
    private val service: VoiceOSService
) : ActionHandler {

    // Add overlay manager reference
    private val overlayManager: OverlayManager by lazy {
        service.getOverlayManager()
    }

    /**
     * Show help menu using ContextMenuOverlay
     * REPLACES: Line 401 TODO and showHelpToast()
     */
    private fun showHelpMenu(): Boolean {
        return try {
            if (isHelpMenuVisible) {
                ConditionalLogger.d(TAG) { "Help menu already visible" }
                return true
            }

            ConditionalLogger.i(TAG) { "Showing help menu" }
            isHelpMenuVisible = true

            // Create help menu items for each category
            val helpItems = listOf(
                MenuItem(
                    id = "navigation_help",
                    label = "Navigation Commands",
                    icon = Icons.Default.Navigation,
                    number = 1,
                    action = { showCategoryHelp("navigation") }
                ),
                MenuItem(
                    id = "system_help",
                    label = "System Commands",
                    icon = Icons.Default.Settings,
                    number = 2,
                    action = { showCategoryHelp("system") }
                ),
                MenuItem(
                    id = "app_help",
                    label = "App Commands",
                    icon = Icons.Default.Apps,
                    number = 3,
                    action = { showCategoryHelp("apps") }
                ),
                MenuItem(
                    id = "all_commands",
                    label = "All Commands",
                    icon = Icons.Default.List,
                    number = 4,
                    action = { showCommandList() }
                ),
                MenuItem(
                    id = "documentation",
                    label = "Full Documentation",
                    icon = Icons.Default.MenuBook,
                    number = 5,
                    action = { openDocumentation() }
                )
            )

            // Show help menu
            overlayManager.showContextMenu(
                items = helpItems,
                title = "VOS4 Help - Say Number to Select"
            )

            // Auto-hide after delay
            helpScope.launch {
                delay(15000) // 15 seconds
                if (isHelpMenuVisible) {
                    hideHelpMenu()
                }
            }

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error showing help menu" }
            false
        }
    }

    /**
     * Hide help menu
     */
    private fun hideHelpMenu(): Boolean {
        return try {
            if (!isHelpMenuVisible) {
                ConditionalLogger.d(TAG) { "Help menu already hidden" }
                return true
            }

            ConditionalLogger.i(TAG) { "Hiding help menu" }
            isHelpMenuVisible = false
            currentHelpCategory = null

            overlayManager.hideContextMenu()

            true
        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error hiding help menu" }
            false
        }
    }

    /**
     * Show category-specific help
     * Updated to use overlay instead of toast
     */
    private fun showCategoryHelp(category: String): Boolean {
        val commands = HELP_CATEGORIES[category] ?: return false

        ConditionalLogger.i(TAG) { "Showing help for category: $category" }
        currentHelpCategory = category

        // Create menu items for commands in this category
        val commandItems = commands.mapIndexed { index, command ->
            MenuItem(
                id = "command_$index",
                label = command,
                number = index + 1,
                enabled = false,  // Display only, not selectable
                action = {}
            )
        }

        // Show commands in overlay
        overlayManager.showContextMenu(
            items = commandItems,
            title = "${category.uppercase()} COMMANDS"
        )

        // Auto-hide after reading time
        helpScope.launch {
            delay(12000) // 12 seconds
            overlayManager.hideContextMenu()
            currentHelpCategory = null
        }

        return true
    }
}
```

**Acceptance Criteria**:
- ✅ Help menu displays with 5 selectable categories
- ✅ Voice selection works ("say 1 for navigation", "2 for system", etc.)
- ✅ Category help shows commands in overlay (not toast)
- ✅ Auto-dismiss after reading time
- ✅ Material 3 design consistent with other overlays
- ✅ TODO removed

---

## Testing Plan

### Unit Tests

**File**: `OverlayIntegrationTest.kt` (new)

```kotlin
@Test
fun testNumberHandlerOverlayIntegration() {
    // Arrange
    val handler = NumberHandler(mockService)
    handler.initialize()

    // Act
    handler.execute(ActionCategory.UI, "show numbers", emptyMap())

    // Assert
    verify(mockOverlayManager).showNumberedSelection(any())
}

@Test
fun testSelectHandlerContextMenu() {
    // Arrange
    val handler = SelectHandler(mockService)
    handler.initialize()

    // Act
    handler.execute(ActionCategory.UI, "context menu", emptyMap())

    // Assert
    verify(mockOverlayManager).showContextMenuAt(any(), any(), any())
}

@Test
fun testHelpMenuHandlerOverlay() {
    // Arrange
    val handler = HelpMenuHandler(mockService)
    handler.initialize()

    // Act
    handler.execute(ActionCategory.HELP, "show help", emptyMap())

    // Assert
    verify(mockOverlayManager).showContextMenu(any(), any())
}
```

### Integration Tests

**Manual Testing Checklist**:
- [ ] NumberHandler: Say "show numbers" → Overlays appear
- [ ] NumberHandler: Say "select 3" → Element 3 clicked
- [ ] NumberHandler: Say "hide numbers" → Overlays disappear
- [ ] SelectHandler: Say "context menu" → Menu appears
- [ ] SelectHandler: Say "select 2" → Menu item 2 executed
- [ ] SelectHandler: Text selected → Copy/cut/paste menu appears
- [ ] HelpMenuHandler: Say "help menu" → Help categories appear
- [ ] HelpMenuHandler: Say "1" → Navigation commands displayed
- [ ] All overlays: Material 3 design consistent
- [ ] All overlays: Smooth animations
- [ ] All overlays: Auto-dismiss works

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| VoiceOSService doesn't have getInstance() | Medium | Low | Add during Phase 1 |
| Cursor manager not available | Low | Medium | Graceful fallback to screen center (already planned) |
| SelectableItem / MenuItem import conflicts | Low | Low | Use fully qualified names if needed |
| Overlay z-order conflicts | Low | Low | OverlayManager handles coordination |
| Performance with many numbered elements | Medium | Medium | Limit to 20 elements max, add pagination |

---

## Definition of Done

- [x] Phase 1: VoiceOSService integration complete
- [ ] Phase 2: NumberHandler integration complete (3 TODOs removed)
- [ ] Phase 3: SelectHandler integration complete (6 TODOs removed)
- [ ] Phase 4: HelpMenuHandler integration complete (1 TODO removed)
- [ ] All 10 TODOs removed from handlers
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] Manual testing checklist complete
- [ ] ConditionalLogger used (no direct Log.* calls)
- [ ] Code reviewed
- [ ] Git commit created
- [ ] Documentation updated

---

## Post-Integration Enhancements (Future)

**NOT part of this task** - document for future work:

1. **VoiceCursor Integration**
   - Connect SelectHandler to actual cursor position
   - Show context menu at cursor location

2. **HelpOverlay Custom Implementation**
   - Dedicated help overlay with scrollable content
   - Search functionality in help
   - Rich formatting for commands

3. **Performance Optimization**
   - Pagination for >20 numbered elements
   - Lazy rendering for large help categories

4. **Advanced Features**
   - Grid overlay for precision selection
   - Zoom overlay for small elements
   - Multi-select mode for numbered elements

---

**Created**: 2025-11-13
**Framework**: IDEACODE v8.0
**Status**: Ready for Implementation

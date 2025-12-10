# LearnApp Debug Overlay (REWRITTEN 2025-12-08)

Scrollable visual debugging tool for LearnApp exploration that tracks ALL items scanned.

## Features

- **Scrollable item list** - View ALL items discovered during exploration
- **Click tracking** - See which items were clicked vs not clicked
- **Block tracking** - See which items were blocked (with reason)
- **Screen grouping** - Group items by source screen
- **Filter buttons** - Filter by All/Screens/Clicked/Blocked/Stats
- **Summary statistics** - Real-time progress stats
- **Draggable window** - Move overlay anywhere on screen
- **Collapsible** - Collapse to minimal header

## Status Icons

| Icon | Meaning |
|------|---------|
| ⚪ | Discovered (not clicked) |
| ✅ | Clicked |
| 🚫 | Blocked (dangerous element) |
| 🔄 | Currently exploring |

## Filter Modes

| Mode | Description |
|------|-------------|
| **All** | Show all items in a flat list |
| **Screens** | Group items by source screen |
| **Clicked** | Show only clicked items |
| **Blocked** | Show only blocked items |
| **Stats** | Show summary statistics |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    ExplorationEngine                         │
│  - onScreenExplored(elements, screenHash, ...)              │
│  - onElementClicked(stableId, screenHash, vuid)             │
│  - onElementBlocked(stableId, screenHash, reason)           │
│  - onElementNavigated(elementKey, destinationHash)          │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                LearnAppIntegration                           │
│  - setupDebugOverlayCallback()                              │
│  - Forwards events to DebugOverlayManager                   │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│              DebugOverlayManager                             │
│  - show() / hide() / toggle()                               │
│  - onScreenExplored() / markItemClicked() / markItemBlocked()│
│  - getTracker() → ExplorationItemTracker                    │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│             ExplorationItemTracker                           │
│  - registerScreen() / registerItems()                        │
│  - markClicked() / markBlocked()                            │
│  - getAllItems() / getItemsByStatus() / getSummary()        │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                DebugOverlayView                              │
│  - Scrollable LinearLayout with item rows                   │
│  - Filter buttons for display modes                         │
│  - Summary stats header                                      │
│  - Draggable/collapsible                                    │
└─────────────────────────────────────────────────────────────┘
```

## Data Models

### ExplorationItem
```kotlin
data class ExplorationItem(
    val id: String,              // Unique ID (screenHash:stableId)
    val vuid: String?,           // Voice UUID if assigned
    val displayName: String,     // Element text/description
    val className: String,       // Element class (Button, etc.)
    val resourceId: String?,     // Android resource ID
    val screenHash: String,      // Screen where found
    val screenName: String,      // Activity name
    var status: ItemStatus,      // DISCOVERED/CLICKED/BLOCKED/EXPLORING
    val blockReason: String?,    // Why blocked (if blocked)
    val timestamp: Long,         // When discovered
    var clickedAt: Long?,        // When clicked (if clicked)
    var navigatedTo: String?     // Where navigated (if clicked)
)
```

### ExplorationSummary
```kotlin
data class ExplorationSummary(
    val totalItems: Int,         // Total items discovered
    val clickedItems: Int,       // Items clicked
    val blockedItems: Int,       // Items blocked
    val discoveredItems: Int,    // Items not yet clicked
    val totalScreens: Int,       // Screens discovered
    val completionPercent: Int   // Completion %
)
```

## Files

| File | Purpose |
|------|---------|
| `ExplorationItemData.kt` | Data models (ExplorationItem, ExplorationScreen, etc.) |
| `ExplorationItemTracker.kt` | Central tracker for all items and screens |
| `DebugOverlayView.kt` | Scrollable view with filter buttons |
| `DebugOverlayManager.kt` | Lifecycle manager, coordinates everything |
| `README-DEBUG-OVERLAY.md` | This documentation |

## Usage

The debug overlay is **automatically enabled** when exploration starts.

### Toggle Visibility
Press the eye (👁) button on FloatingProgressWidget to toggle.

### Manual Control
```kotlin
// Get manager
val debugManager = floatingProgressWidget.getDebugOverlayManager()

// Toggle
debugManager.toggle()

// Get tracker for direct access
val tracker = debugManager.getTracker()
val summary = tracker.getSummary()

// Export to markdown
val report = tracker.exportToMarkdown()
```

## Example Output

```
📊 Statistics Summary
────────────────────────
Total Items:      156
Clicked:          85  (✅)
Blocked:          12  (🚫)
Not Clicked:      59  (⚪)
Screens:          8
Completion:       59%

📱 Screens Breakdown
────────────────────────
ActivityMain         45/52  (87%)
ChatListActivity     12/18  (67%)
SettingsActivity     8/15   (53%)
...
```

## Blocked Items

Items are blocked when they match critical dangerous patterns:

| Category | Examples |
|----------|----------|
| Power | power off, shutdown, restart, sleep |
| App Control | exit, quit, force stop |
| Account | sign out, delete account |
| Communication | call, dial, answer, join meeting |
| Messaging | reply, send message, post |

## Created

2025-12-08 - VOS4 Development Team (Complete rewrite)

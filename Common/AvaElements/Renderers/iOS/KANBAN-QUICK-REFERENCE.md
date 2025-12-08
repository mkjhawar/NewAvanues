# Kanban Component - Quick Reference

**Component:** Kanban Board (iOS)
**Status:** ✅ PRODUCTION-READY
**iOS Version:** 16.0+
**Files:** 3 implementation + 1 test

---

## Files

```
src/iosMain/swift/Charts/
├── KanbanView.swift         (385 lines) - Main board
├── KanbanColumnView.swift   (263 lines) - Column
└── KanbanCardView.swift     (261 lines) - Card

Tests/Charts/
└── KanbanTests.swift        (548 lines) - 14 tests
```

---

## Basic Usage

### Standard Board (To Do, In Progress, Done)
```swift
KanbanView.standard(
    title: "Development Board",
    todoCards: [
        KanbanCard(
            id: "1",
            title: "Design landing page",
            priority: .high,
            tags: ["design", "ui"],
            assignee: "Alice"
        )
    ],
    inProgressCards: inProgressCards,
    doneCards: doneCards
)
```

### Scrum Board (5 columns)
```swift
KanbanView.scrum(
    title: "Sprint 12",
    sprintCards: sprintCards,
    inProgressCards: inProgressCards
)
```

### Custom Board
```swift
KanbanView(
    title: "Custom Board",
    columns: [
        KanbanColumn(
            id: "custom",
            title: "Custom Column",
            cards: cards,
            color: "#9C27B0",
            maxCards: 10
        )
    ],
    allowDragDrop: true
)
```

---

## Data Models

### KanbanCard
```swift
KanbanCard(
    id: "unique-id",
    title: "Card title",
    description: "Optional description",
    priority: .high,              // .low, .medium, .high, .urgent
    tags: ["tag1", "tag2"],
    assignee: "Alice"
)
```

### KanbanColumn
```swift
KanbanColumn(
    id: "column-id",
    title: "Column Title",
    cards: [card1, card2],
    color: "#2196F3",             // Optional hex color
    maxCards: 5                   // Optional WIP limit
)
```

---

## Priority Colors

| Priority | Color | Hex | Visual |
|----------|-------|-----|--------|
| LOW | Green | #4CAF50 | 🟢 |
| MEDIUM | Blue | #2196F3 | 🔵 |
| HIGH | Orange | #FF9800 | 🟠 |
| URGENT | Red | #F44336 | 🔴 |

---

## Callbacks

### Card Tap
```swift
KanbanView(
    columns: columns,
    onCardTap: { columnId, cardId in
        print("Card \(cardId) tapped in column \(columnId)")
    }
)
```

### Card Move (Drag-Drop)
```swift
KanbanView(
    columns: columns,
    onCardMove: { cardId, fromColumnId, toColumnId in
        print("Card \(cardId) moved from \(fromColumnId) to \(toColumnId)")
        // Update your data model here
    }
)
```

---

## Features

### Core
- ✅ Horizontal scrolling board
- ✅ Multiple columns
- ✅ Drag-and-drop (iOS 16+)
- ✅ Priority color coding
- ✅ Tag chips
- ✅ Assignee display
- ✅ WIP limit warnings

### Accessibility
- ✅ VoiceOver support (100%)
- ✅ Descriptive labels
- ✅ Button traits
- ✅ Accessibility hints

### Visual
- ✅ Empty states
- ✅ Smooth animations
- ✅ Material Design 3
- ✅ HIG compliance

---

## Column Properties

### isAtCapacity
```swift
let column = KanbanColumn(
    id: "1",
    title: "In Progress",
    cards: cards,
    maxCards: 5
)

if column.isAtCapacity {
    print("Column is at capacity!")
}
```

### Filter by Priority
```swift
let highPriorityCards = column.cards(withPriority: .high)
```

---

## Presets

### Standard Board
- **Columns:** To Do, In Progress, Done
- **WIP Limit:** In Progress (5 cards)
- **Colors:** Blue, Orange, Green

### Scrum Board
- **Columns:** Backlog, Sprint, In Progress, Review, Done
- **WIP Limit:** In Progress (3 cards)
- **Colors:** Gray, Blue, Orange, Purple, Green

---

## Testing

### Run Tests
```bash
# Run all Kanban tests
swift test --filter KanbanTests

# Run specific test
swift test --filter KanbanTests/testBoardRendering
```

### Test Cases (14)
1. testBoardRendering
2. testColumnRendering
3. testCardRendering
4. testPriorityColors
5. testTagDisplay
6. testVoiceOverSupport
7. testMaxCardsLogic
8. testEmptyStates
9. testStandardBoardPreset
10. testScrumBoardPreset
11. testCardsByPriorityFilter
12. testDragDropState
13. testPriorityEnumValues
14. testCardWithoutOptionalFields

---

## Drag-and-Drop

### Enable
```swift
KanbanView(
    columns: columns,
    allowDragDrop: true  // Default
)
```

### Disable
```swift
KanbanView(
    columns: columns,
    allowDragDrop: false
)
```

### Implementation
- Uses iOS 16+ `.draggable()` and `.dropDestination()`
- Cards can be dragged between columns
- Capacity checks prevent drops if column is at max
- Visual feedback during drag
- Smooth animations

---

## VoiceOver

### Card Label
```
"Card: Design landing page.
Description: Create mockups and user flows.
Priority: High.
Tags: design, ui.
Assigned to: Alice."
```

### Column Label
```
"Column: To Do.
2 cards.
Maximum: 5."
```

### Board Label
```
"Sprint Board.
Kanban board with 3 columns and 5 total cards.
To Do: 2 cards, In Progress: 2 cards, Done: 1 cards."
```

---

## Empty States

### Empty Board
- Shows "No columns" placeholder
- Provides guidance to add columns

### Empty Column
- Shows "No cards" placeholder
- Provides drop zone for drag-and-drop

---

## Sample Data

```swift
let sampleBoard = KanbanView.sampleBoard()

// Returns a complete board with:
// - To Do: 2 cards (high priority design task, urgent bug)
// - In Progress: 1 card (medium priority feature)
// - Done: 1 card (low priority maintenance)
```

---

## Common Patterns

### Dynamic Board
```swift
@State private var columns: [KanbanColumn] = initialColumns

var body: some View {
    KanbanView(
        title: "My Board",
        columns: columns,
        onCardMove: { cardId, from, to in
            moveCard(cardId: cardId, from: from, to: to)
        }
    )
}

func moveCard(cardId: String, from: String, to: String) {
    // Update columns state
    // This will trigger UI refresh
}
```

### Load from Network
```swift
@State private var columns: [KanbanColumn] = []

var body: some View {
    KanbanView(
        title: "Remote Board",
        columns: columns
    )
    .task {
        columns = await fetchColumns()
    }
}
```

### Filter by Priority
```swift
let urgentColumns = columns.map { column in
    KanbanColumn(
        id: column.id,
        title: column.title,
        cards: column.cards(withPriority: .urgent),
        color: column.color,
        maxCards: column.maxCards
    )
}
```

---

## Performance Tips

1. **Use Identifiable** - Cards and columns use `id` for efficient updates
2. **Lazy Loading** - ScrollView loads cards on demand
3. **State Management** - Use `@State` for dynamic boards
4. **Avoid Deep Nesting** - Keep card content simple
5. **Limit Tags** - Show max 3 tags per card

---

## Troubleshooting

### Drag-Drop Not Working
- **Check iOS Version:** Requires iOS 16+
- **Check allowDragDrop:** Must be `true`
- **Check Simulator:** May not work in some simulators

### Tags Not Wrapping
- **FlowLayout:** Uses custom Layout protocol
- **iOS Version:** Requires iOS 16+
- **Width:** Ensure parent has defined width

### VoiceOver Not Reading
- **Check Labels:** Ensure all elements have accessibility labels
- **Check Traits:** Interactive elements need `.isButton` trait
- **Test:** Use Accessibility Inspector

---

## Best Practices

1. **Keep Cards Concise** - Title should be 1-2 lines
2. **Use Priority Wisely** - Don't mark everything urgent
3. **Set WIP Limits** - Use maxCards to prevent overload
4. **Provide Callbacks** - Handle onCardTap and onCardMove
5. **Test Accessibility** - Use VoiceOver during development

---

## Related Components

- **LineChart** - Time-series data visualization
- **BarChart** - Categorical data comparison
- **PieChart** - Proportional data display
- **TreeMap** - Hierarchical data visualization
- **Heatmap** - Matrix data visualization

---

## Resources

- **Implementation:** `KanbanView.swift`, `KanbanColumnView.swift`, `KanbanCardView.swift`
- **Tests:** `KanbanTests.swift`
- **Android Reference:** `FlutterParityKanbanMappers.kt`
- **Data Models:** `Kanban.kt` (Kotlin)
- **Foundation:** `ChartHelpers.swift`

---

## Version Info

- **Component Version:** 1.0.0
- **iOS Minimum:** 16.0
- **SwiftUI:** Native layout (NOT Canvas)
- **Agent:** ios-chart-011 (Kanban-Agent)
- **Status:** ✅ Production-ready

---

**Quick Start:**
```swift
import AvaElementsiOS

// Standard board
KanbanView.standard(
    title: "My Board",
    todoCards: myCards
)

// That's it!
```

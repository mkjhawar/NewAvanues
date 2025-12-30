# Agent 4: Quick Reference Guide

**Last Updated:** 2025-11-24
**Components:** 8 P1 Navigation & Data Components

---

## Component Cheat Sheet

### Navigation Components (4)

#### 1. Menu
```kotlin
Menu(
    items = listOf(
        Menu.MenuItem(id = "file", label = "File", icon = "description",
            children = listOf(
                Menu.MenuItem(id = "new", label = "New"),
                Menu.MenuItem(id = "open", label = "Open")
            )
        )
    ),
    orientation = Menu.Orientation.Vertical, // or Horizontal
    selectionMode = Menu.SelectionMode.Single, // None, Single, Multiple
    onItemClick = { itemId -> /* handle click */ }
)

// Quick constructors
Menu.vertical(items, onItemClick)
Menu.horizontal(items, onItemClick)
Menu.contextMenu(items, onItemClick)
```

**Key Properties:**
- `orientation`: Vertical (default) | Horizontal
- `selectionMode`: None | Single | Multiple
- `dense`: Boolean (compact layout)
- `selectedIndex`: Int? (single selection)
- `selectedIndices`: List<Int> (multiple selection)

**MenuItem Properties:**
- `id`, `label`, `icon`, `enabled`
- `children`: List<MenuItem>? (nested submenu)
- `badge`, `divider`

---

#### 2. Sidebar
```kotlin
Sidebar(
    items = listOf(
        Sidebar.SidebarItem(
            id = "home",
            label = "Home",
            icon = "home",
            selected = true,
            badge = "5"
        )
    ),
    mode = Sidebar.Mode.Persistent, // or Overlay
    collapsed = false,
    collapsible = true,
    width = 280f,
    collapsedWidth = 72f,
    onCollapseToggle = { collapsed -> /* handle */ },
    onItemClick = { itemId -> /* navigate */ }
)

// Quick constructors
Sidebar.persistent(items, collapsed, onItemClick)
Sidebar.overlay(items, visible, onItemClick)
Sidebar.navigationRail(items, onItemClick) // Always collapsed
```

**Key Properties:**
- `mode`: Persistent (desktop) | Overlay (mobile)
- `collapsed`: Boolean
- `collapsible`: Boolean
- `width`: Float (default 280)
- `collapsedWidth`: Float (default 72)
- `headerContent`, `footerContent`: String?

**SidebarItem Properties:**
- `id`, `label`, `icon`, `selected`, `enabled`
- `badge`, `divider`

---

#### 3. NavLink
```kotlin
NavLink(
    label = "Dashboard",
    href = "/dashboard",
    icon = "dashboard",
    active = true,
    badge = "5",
    iconPosition = NavLink.IconPosition.Leading, // Leading, Trailing, Top, Bottom
    onClick = { /* navigate */ }
)

// Quick constructors
NavLink.active(label, href, icon, onClick)
NavLink.inactive(label, href, icon, onClick)
NavLink.withBadge(label, href, badge, active, onClick)
NavLink.breadcrumb(label, href, active, onClick)
```

**Key Properties:**
- `label`, `href`, `icon`, `active`, `badge`
- `iconPosition`: Leading | Trailing | Top | Bottom
- `activeColor`, `inactiveColor`
- `activeBackgroundColor`, `backgroundColor`

---

#### 4. ProgressStepper
```kotlin
ProgressStepper(
    steps = listOf(
        ProgressStepper.Step(
            label = "Account",
            description = "Enter details",
            icon = "person",
            optional = false,
            error = false
        ),
        ProgressStepper.Step(label = "Verify", description = "Check email"),
        ProgressStepper.Step(label = "Complete")
    ),
    currentStep = 1,
    orientation = ProgressStepper.Orientation.Horizontal, // or Vertical
    clickable = true, // Allow clicking completed steps
    showStepNumbers = true,
    connectorType = ProgressStepper.ConnectorType.Line, // Line, Dashed, Dotted, None
    onStepClicked = { stepIndex -> /* navigate */ }
)

// Quick constructors
ProgressStepper.horizontal(steps, currentStep, onStepClicked)
ProgressStepper.vertical(steps, currentStep, onStepClicked)
ProgressStepper.simple(stepCount, currentStep) // No labels
```

**Key Properties:**
- `currentStep`: Int (0-based index)
- `orientation`: Horizontal | Vertical
- `clickable`: Boolean
- `showStepNumbers`: Boolean
- `connectorType`: Line | Dashed | Dotted | None

**Step Properties:**
- `label`, `description`, `icon`
- `optional`, `error`

**Helper Methods:**
- `getStepState(index)`: Completed | Current | Upcoming
- `getProgressPercentage()`: Float (0.0-1.0)
- `isComplete()`: Boolean
- `canClickStep(index)`: Boolean

---

### Data Components (4)

#### 5. RadioListTile
```kotlin
var selectedValue by remember { mutableStateOf("option1") }

RadioListTile(
    title = "Premium Plan",
    subtitle = "$9.99/month",
    value = "premium",
    groupValue = selectedValue,
    controlAffinity = RadioListTile.ListTileControlAffinity.Trailing, // Leading, Trailing, Platform
    enabled = true,
    dense = false,
    onChanged = { selectedValue = it }
)

// Quick constructors
RadioListTile.selected(title, value, onChanged)
RadioListTile.unselected(title, value, groupValue, onChanged)
RadioListTile.withSubtitle(title, subtitle, value, groupValue, onChanged)
```

**Key Properties:**
- `title`, `subtitle`, `secondary`
- `value`: String (this radio's value)
- `groupValue`: String? (selected value in group)
- `controlAffinity`: Leading | Trailing | Platform
- `activeColor`, `tileColor`, `selectedTileColor`
- `dense`: Boolean

**Helper Property:**
- `isSelected`: Boolean (value == groupValue)

---

#### 6. VirtualScroll
```kotlin
VirtualScroll(
    itemCount = 10000,
    itemHeight = 56f, // Optional, helps with performance
    cacheSize = 50,
    orientation = VirtualScroll.Orientation.Vertical, // or Horizontal
    reverseLayout = false,
    initialScrollIndex = 0,
    onItemRender = { index ->
        // Return Component for this index
        ListTile(
            title = "Item $index",
            subtitle = "Description"
        )
    },
    onScrolledToEnd = {
        // Triggered when scrolled to end
    }
)

// Quick constructors
VirtualScroll.vertical(itemCount, itemHeight, onItemRender)
VirtualScroll.horizontal(itemCount, itemWidth, onItemRender)
VirtualScroll.fixedSize(itemCount, itemHeight, onItemRender)
VirtualScroll.dynamicSize(itemCount, cacheSize, onItemRender)
```

**Key Properties:**
- `itemCount`: Int
- `itemHeight`: Float? (fixed height for optimization)
- `itemWidth`: Float? (for horizontal)
- `cacheSize`: Int (default 50)
- `orientation`: Vertical | Horizontal
- `onItemRender`: (Int) -> Component

**Helper Methods:**
- `hasFixedItemHeight()`: Boolean
- `getEstimatedTotalHeight()`: Float?
- `calculateVisibleRange(offset, viewport)`: IntRange

---

#### 7. InfiniteScroll
```kotlin
var items by remember { mutableStateOf(initialItems) }
var hasMore by remember { mutableStateOf(true) }
var loading by remember { mutableStateOf(false) }
var error by remember { mutableStateOf(false) }

InfiniteScroll(
    items = items.map { item ->
        ListTile(title = item.title, subtitle = item.description)
    },
    hasMore = hasMore,
    loading = loading,
    showError = error,
    loadingThreshold = 100f, // Distance from bottom to trigger load
    loadingIndicatorText = "Loading more...",
    endMessageText = "No more items",
    errorMessageText = "Failed to load. Tap to retry.",
    orientation = InfiniteScroll.Orientation.Vertical,
    onLoadMore = {
        loading = true
        loadNextPage { newItems, noMore ->
            items = items + newItems
            hasMore = !noMore
            loading = false
        }
    },
    onRetry = {
        error = false
        // Retry loading
    }
)

// Quick constructors
InfiniteScroll.vertical(items, hasMore, loading, onLoadMore)
InfiniteScroll.horizontal(items, hasMore, loading, onLoadMore)
InfiniteScroll.withThreshold(items, threshold, hasMore, onLoadMore)
```

**Key Properties:**
- `items`: List<Component>
- `hasMore`: Boolean
- `loading`: Boolean
- `showError`: Boolean
- `loadingThreshold`: Float (dp from bottom)
- `loadingIndicatorText`, `endMessageText`, `errorMessageText`
- `orientation`: Vertical | Horizontal

**Helper Methods:**
- `shouldLoadMore(scrollOffset, maxOffset)`: Boolean
- `getFooterState()`: Loading | End | Error | None
- `getFooterMessage()`: String?

**Footer States:**
- `Loading`: Shows loading indicator
- `End`: Shows "no more items" message
- `Error`: Shows error with retry button
- `None`: No footer

---

#### 8. QRCode
```kotlin
QRCode(
    data = "https://example.com",
    size = 200f,
    errorCorrection = QRCode.ErrorCorrectionLevel.M, // L, M, Q, H
    foregroundColor = 0xFF000000, // Black
    backgroundColor = 0xFFFFFFFF, // White
    padding = 0f,
    embeddedImageUrl = null, // Optional logo
    embeddedImageSize = 40f,
    onTap = { /* handle tap */ }
)

// Helper constructors
QRCode.url("https://example.com", size = 200f)
QRCode.wifi(ssid = "MyNetwork", password = "pass123", security = "WPA")
QRCode.contact(name = "John Doe", phone = "+1234567890", email = "john@example.com")
QRCode.withLogo(data, logoUrl, size = 200f, logoSize = 40f)
QRCode.colored(data, foregroundColor, backgroundColor, size = 200f)
```

**Key Properties:**
- `data`: String (max 4296 chars)
- `size`: Float (dp)
- `errorCorrection`: L (~7%) | M (~15%) | Q (~25%) | H (~30%)
- `foregroundColor`, `backgroundColor`: Long (ARGB)
- `embeddedImageUrl`, `embeddedImageSize`

**Helper Methods:**
- `isDataValid()`: Boolean
- `getAccessibilityDescription()`: String
- `getRecommendedErrorCorrection()`: ErrorCorrectionLevel
- `getCapacity()`: Int (chars for current error level)
- `fitsInCapacity()`: Boolean

**Error Correction Levels:**
- `L`: Low (~7%) - clean environments
- `M`: Medium (~15%) - standard use (default)
- `Q`: Quartile (~25%) - use with logo
- `H`: High (~30%) - damaged surfaces

**WiFi QR Format:**
```kotlin
"WIFI:T:WPA;S:NetworkName;P:Password;H:false;;"
```

**vCard QR Format:**
```kotlin
"""
BEGIN:VCARD
VERSION:3.0
FN:John Doe
TEL:+1234567890
EMAIL:john@example.com
END:VCARD
"""
```

---

## Common Patterns

### 1. Menu with Nested Submenus
```kotlin
val menuItems = listOf(
    Menu.MenuItem(
        id = "file",
        label = "File",
        icon = "description",
        children = listOf(
            Menu.MenuItem(id = "new", label = "New", icon = "add"),
            Menu.MenuItem(id = "open", label = "Open", icon = "folder_open"),
            Menu.MenuItem(id = "divider", label = "", divider = true),
            Menu.MenuItem(id = "save", label = "Save", icon = "save")
        )
    ),
    Menu.MenuItem(id = "edit", label = "Edit", icon = "edit"),
    Menu.MenuItem(id = "view", label = "View", icon = "visibility")
)

Menu.horizontal(menuItems) { itemId ->
    when (itemId) {
        "new" -> createNew()
        "open" -> openFile()
        "save" -> saveFile()
    }
}
```

### 2. Responsive Sidebar
```kotlin
@Composable
fun ResponsiveSidebar(windowSize: WindowSize) {
    val isDesktop = windowSize.width >= WindowSize.Medium
    var collapsed by remember { mutableStateOf(!isDesktop) }

    Sidebar(
        items = navigationItems,
        mode = if (isDesktop) Sidebar.Mode.Persistent else Sidebar.Mode.Overlay,
        collapsed = collapsed,
        collapsible = isDesktop,
        onCollapseToggle = { collapsed = it }
    )
}
```

### 3. Wizard with Progress Stepper
```kotlin
@Composable
fun WizardFlow() {
    var currentStep by remember { mutableStateOf(0) }

    Column {
        ProgressStepper.horizontal(
            steps = listOf(
                ProgressStepper.Step("Account", "Enter details"),
                ProgressStepper.Step("Verify", "Check email"),
                ProgressStepper.Step("Complete", "Finish setup")
            ),
            currentStep = currentStep,
            clickable = true
        ) { stepIndex ->
            if (stepIndex < currentStep) currentStep = stepIndex
        }

        // Step content
        when (currentStep) {
            0 -> AccountStep()
            1 -> VerifyStep()
            2 -> CompleteStep()
        }

        // Navigation
        Row {
            if (currentStep > 0) {
                Button(onClick = { currentStep-- }) { Text("Back") }
            }
            Button(onClick = { currentStep++ }) {
                Text(if (currentStep == 2) "Finish" else "Next")
            }
        }
    }
}
```

### 4. Radio Group with RadioListTile
```kotlin
@Composable
fun SettingsRadioGroup() {
    var selectedTheme by remember { mutableStateOf("system") }

    Column {
        RadioListTile(
            title = "System default",
            subtitle = "Follow system theme",
            value = "system",
            groupValue = selectedTheme,
            onChanged = { selectedTheme = it }
        )
        RadioListTile(
            title = "Light",
            subtitle = "Always use light theme",
            value = "light",
            groupValue = selectedTheme,
            onChanged = { selectedTheme = it }
        )
        RadioListTile(
            title = "Dark",
            subtitle = "Always use dark theme",
            value = "dark",
            groupValue = selectedTheme,
            onChanged = { selectedTheme = it }
        )
    }
}
```

### 5. Virtualized Large List
```kotlin
@Composable
fun ContactList(contacts: List<Contact>) {
    VirtualScroll.fixedSize(
        itemCount = contacts.size,
        itemHeight = 72f
    ) { index ->
        val contact = contacts[index]
        ListTile(
            title = contact.name,
            subtitle = contact.email,
            leading = Avatar(contact.avatarUrl),
            trailing = IconButton("phone") { call(contact) }
        )
    }
}
```

### 6. Infinite Scroll Feed
```kotlin
@Composable
fun SocialFeed(viewModel: FeedViewModel) {
    val posts by viewModel.posts.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loading by viewModel.loading.collectAsState()

    InfiniteScroll.vertical(
        items = posts.map { PostCard(it) },
        hasMore = hasMore,
        loading = loading
    ) {
        viewModel.loadMore()
    }
}
```

### 7. WiFi Sharing QR
```kotlin
@Composable
fun WiFiShareCard(network: WiFiNetwork) {
    Card {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Scan to connect")

            QRCode.wifi(
                ssid = network.ssid,
                password = network.password,
                security = "WPA"
            ).copy(
                size = 300f,
                errorCorrection = QRCode.ErrorCorrectionLevel.M
            )

            Text("Network: ${network.ssid}")
        }
    }
}
```

---

## Testing Examples

### Menu Tests
```kotlin
@Test
fun menu_nestedSubmenus_expandsAndCollapses() {
    val items = listOf(
        Menu.MenuItem(
            id = "file",
            label = "File",
            children = listOf(
                Menu.MenuItem(id = "new", label = "New")
            )
        )
    )

    composeTestRule.setContent {
        MenuMapper(Menu(items = items))
    }

    // Initially hidden
    composeTestRule.onNodeWithText("New").assertDoesNotExist()

    // Expand
    composeTestRule.onNodeWithText("File").performClick()

    // Now visible
    composeTestRule.onNodeWithText("New").assertExists()
}
```

### Sidebar Tests
```kotlin
@Test
fun sidebar_collapsed_showsIconsOnly() {
    val items = listOf(
        Sidebar.SidebarItem(id = "home", label = "Home", icon = "home")
    )

    composeTestRule.setContent {
        SidebarMapper(Sidebar(items = items, collapsed = true))
    }

    // Icon exists
    composeTestRule.onNode(hasContentDescription("home")).assertExists()
}
```

### ProgressStepper Tests
```kotlin
@Test
fun progressStepper_clickableSteps_navigatesToCompletedSteps() {
    var currentStep = 2

    composeTestRule.setContent {
        ProgressStepperMapper(
            ProgressStepper(
                steps = List(4) { ProgressStepper.Step("Step ${it + 1}") },
                currentStep = currentStep,
                clickable = true,
                onStepClicked = { currentStep = it }
            )
        )
    }

    // Click completed step
    composeTestRule.onNodeWithText("Step 1").performClick()
    assert(currentStep == 0)
}
```

### InfiniteScroll Tests
```kotlin
@Test
fun infiniteScroll_loadingState_showsIndicator() {
    composeTestRule.setContent {
        InfiniteScrollMapper(
            InfiniteScroll(
                items = emptyList(),
                hasMore = true,
                loading = true,
                loadingIndicatorText = "Loading..."
            )
        )
    }

    composeTestRule.onNodeWithText("Loading...").assertExists()
}
```

### QRCode Tests
```kotlin
@Test
fun qrcode_validData_generatesQRCode() {
    composeTestRule.setContent {
        QRCodeMapper(
            QRCode(data = "https://example.com", size = 200f)
        )
    }

    composeTestRule.onNode(
        hasContentDescription("QR code containing URL")
    ).assertExists()
}
```

---

## Accessibility

### Content Descriptions

All components provide automatic accessibility descriptions:

```kotlin
// Menu
Menu(...).getAccessibilityDescription()
// → "Menu with 5 items, single selection"

// Sidebar
Sidebar(...).getAccessibilityDescription()
// → "Navigation sidebar, expanded, persistent mode"

// NavLink
NavLink(...).getAccessibilityDescription()
// → "Dashboard, current page, badge: 5"

// ProgressStepper
ProgressStepper(...).getAccessibilityDescription()
// → "Progress stepper, step 2 of 4, horizontal"

// RadioListTile
RadioListTile(...).getAccessibilityDescription()
// → "Premium Plan, selected"

// VirtualScroll
VirtualScroll(...).getAccessibilityDescription()
// → "Virtual scroll list with 1000 items, vertical scrolling"

// InfiniteScroll
InfiniteScroll(...).getAccessibilityDescription()
// → "Infinite scroll list with 50 items, loading more items"

// QRCode
QRCode(...).getAccessibilityDescription()
// → "QR code containing URL: https://example.com"
```

### Custom Descriptions
```kotlin
Menu(
    items = items,
    contentDescription = "Main navigation menu"
)

Sidebar(
    items = items,
    contentDescription = "App sidebar navigation"
)

NavLink(
    label = "Home",
    contentDescription = "Navigate to home page"
)

QRCode(
    data = url,
    contentDescription = "QR code for website"
)
```

---

## Performance Tips

### VirtualScroll
```kotlin
// ✅ GOOD: Fixed item height for best performance
VirtualScroll.fixedSize(
    itemCount = 10000,
    itemHeight = 72f
) { index -> /* ... */ }

// ⚠️ OK: Dynamic heights, more conservative rendering
VirtualScroll.dynamicSize(
    itemCount = 1000,
    cacheSize = 30
) { index -> /* ... */ }
```

### InfiniteScroll
```kotlin
// ✅ GOOD: Reasonable threshold
InfiniteScroll(
    items = items,
    loadingThreshold = 100f, // Load when 100dp from bottom
    onLoadMore = { /* ... */ }
)

// ❌ BAD: Too aggressive
InfiniteScroll(
    items = items,
    loadingThreshold = 500f, // Loads too early
    onLoadMore = { /* ... */ }
)
```

### QRCode
```kotlin
// ✅ GOOD: Bitmap is cached, only regenerates when data/size changes
val qrCode = remember(url, size) {
    QRCode.url(url, size)
}

// ❌ BAD: Regenerates every recomposition
QRCode.url(url, size)
```

---

## Quick Stats

| Component | LOC | Tests | Key Feature |
|-----------|-----|-------|-------------|
| Menu | 230 | 7 | Nested submenus |
| Sidebar | 230 | 6 | Collapsible rail |
| NavLink | 196 | 6 | Active styling |
| ProgressStepper | 264 | 6 | Clickable steps |
| RadioListTile | 184 | 7 | Grouped radios |
| VirtualScroll | 204 | 8 | 10K+ items |
| InfiniteScroll | 229 | 6 | Auto-load |
| QRCode | 266 | 8 | ZXing + logo |

**Total:** 1,803 LOC, 54 tests

---

**Quick Reference Guide - Agent 4**
**Components:** 8 P1 Navigation & Data
**Version:** 1.0
**Date:** 2025-11-24

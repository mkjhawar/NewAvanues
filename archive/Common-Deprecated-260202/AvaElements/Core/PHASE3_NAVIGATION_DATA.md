# Phase 3: Navigation & Data Display Components

This document describes all navigation and data display components implemented in Phase 3.3 and 3.4 of the AvaElements library.

## Table of Contents

### Navigation Components (Phase 3.3)
1. [AppBar](#appbar)
2. [BottomNav](#bottomnav)
3. [Tabs](#tabs)
4. [Drawer](#drawer)
5. [Breadcrumb](#breadcrumb)
6. [Pagination](#pagination)

### Data Display Components (Phase 3.4)
7. [Table](#table)
8. [List](#list)
9. [Accordion](#accordion)
10. [Stepper](#stepper)
11. [Timeline](#timeline)
12. [TreeView](#treeview)
13. [Carousel](#carousel)
14. [Avatar](#avatar)
15. [Chip](#chip)
16. [Divider](#divider)
17. [Paper](#paper)
18. [Skeleton](#skeleton)
19. [EmptyState](#emptystate)
20. [DataGrid](#datagrid)

---

## Navigation Components

### AppBar

A top app bar (action bar/toolbar) that displays navigation, title, and actions at the top of the screen.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `title` | `String` | Required | Title text displayed in the app bar |
| `navigationIcon` | `String?` | `null` | Icon name for navigation (e.g., menu, back) |
| `actions` | `List<AppBarAction>` | `emptyList()` | Action buttons displayed on the right |
| `elevation` | `Int` | `1` | Shadow elevation level (0-5) |
| `onNavigationClick` | `(() -> Unit)?` | `null` | Callback when navigation icon is clicked |

#### DSL Example

```kotlin
AppBar(title = "My App") {
    navigationIcon = "menu"
    elevation = 2

    action("search", "Search") { /* handle search */ }
    action("settings", "Settings") { /* handle settings */ }

    onNavigationClick = { /* handle navigation click */ }
}
```

#### YAML Example

```yaml
AppBar:
  id: main_appbar
  title: "My App"
  navigationIcon: menu
  elevation: 2
  actions:
    - icon: search
      label: Search
    - icon: settings
      label: Settings
```

#### Use Cases
- Main application toolbar
- Screen headers with back navigation
- Action-heavy interfaces (search, settings, etc.)
- Contextual app bars with dynamic actions

---

### BottomNav

A bottom navigation bar for primary app-level navigation between 2-5 destinations.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<BottomNavItem>` | Required | Navigation items (2-5 items) |
| `selectedIndex` | `Int` | `0` | Currently selected item index |
| `onItemSelected` | `((Int) -> Unit)?` | `null` | Callback when item is selected |

#### DSL Example

```kotlin
BottomNav(
    items = listOf(
        bottomNavItem("home", "Home"),
        bottomNavItem("search", "Search"),
        bottomNavItem("profile", "Profile", badge = "3")
    )
) {
    selectedIndex = 0
    onItemSelected = { index ->
        println("Selected: $index")
    }
}
```

#### YAML Example

```yaml
BottomNav:
  id: main_bottom_nav
  items:
    - icon: home
      label: Home
    - icon: search
      label: Search
    - icon: profile
      label: Profile
      badge: "3"
  selectedIndex: 0
```

#### Use Cases
- Primary navigation in mobile apps
- Tab-like navigation for main sections
- Persistent navigation across screens

---

### Tabs

A tabbed navigation component for organizing content into separate views where only one tab is visible at a time.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `tabs` | `List<Tab>` | Required | Tab items with labels and optional icons |
| `selectedIndex` | `Int` | `0` | Currently selected tab index |
| `onTabSelected` | `((Int) -> Unit)?` | `null` | Callback when tab is selected |

#### DSL Example

```kotlin
Tabs(
    tabs = listOf(
        tab("Overview", icon = "dashboard"),
        tab("Details", icon = "info"),
        tab("Settings", icon = "settings")
    )
) {
    selectedIndex = 0
    onTabSelected = { index ->
        println("Tab selected: $index")
    }
}
```

#### YAML Example

```yaml
Tabs:
  id: content_tabs
  tabs:
    - label: Overview
      icon: dashboard
    - label: Details
      icon: info
    - label: Settings
      icon: settings
  selectedIndex: 0
```

#### Use Cases
- Organizing related content into sections
- Settings or preferences screens
- Multi-section forms
- Content categorization

---

### Drawer

A navigation drawer that slides in from the side of the screen, typically used for primary navigation.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `isOpen` | `Boolean` | `false` | Whether drawer is currently open |
| `position` | `DrawerPosition` | `Left` | Side from which drawer appears (Left/Right) |
| `header` | `Component?` | `null` | Optional header component |
| `items` | `List<DrawerItem>` | `emptyList()` | Navigation items |
| `footer` | `Component?` | `null` | Optional footer component |
| `onItemClick` | `((String) -> Unit)?` | `null` | Callback when item is clicked |
| `onDismiss` | `(() -> Unit)?` | `null` | Callback when drawer is dismissed |

#### DSL Example

```kotlin
Drawer(isOpen = true) {
    position = DrawerPosition.Left

    item("home", "Home", icon = "home")
    item("settings", "Settings", icon = "settings")
    item("help", "Help", icon = "help", badge = "New")

    onItemClick = { id ->
        println("Clicked: $id")
    }
    onDismiss = {
        println("Drawer closed")
    }
}
```

#### YAML Example

```yaml
Drawer:
  id: main_drawer
  isOpen: false
  position: Left
  items:
    - id: home
      label: Home
      icon: home
    - id: settings
      label: Settings
      icon: settings
    - id: help
      label: Help
      icon: help
      badge: New
```

#### Use Cases
- Main application navigation
- User account menus
- Settings and preferences access
- Multi-level navigation hierarchies

---

### Breadcrumb

A breadcrumb navigation component that shows the current location in a hierarchical structure.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<BreadcrumbItem>` | Required | Breadcrumb items showing path |
| `separator` | `String` | `"/"` | Separator character between items |

#### DSL Example

```kotlin
Breadcrumb(
    items = listOf(
        breadcrumbItem("Home", "/") { /* navigate home */ },
        breadcrumbItem("Products", "/products") { /* navigate products */ },
        breadcrumbItem("Electronics", "/products/electronics")
    )
) {
    separator = ">"
}
```

#### YAML Example

```yaml
Breadcrumb:
  id: page_breadcrumb
  items:
    - label: Home
      href: /
    - label: Products
      href: /products
    - label: Electronics
      href: /products/electronics
  separator: ">"
```

#### Use Cases
- Website navigation
- File system browsing
- Multi-level content hierarchies
- Showing current location in deep navigation

---

### Pagination

A pagination component for navigating through multiple pages of content.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `currentPage` | `Int` | `1` | Currently active page (1-indexed) |
| `totalPages` | `Int` | Required | Total number of pages |
| `showFirstLast` | `Boolean` | `true` | Show first/last page buttons |
| `showPrevNext` | `Boolean` | `true` | Show previous/next buttons |
| `maxVisible` | `Int` | `7` | Maximum visible page numbers |
| `onPageChange` | `((Int) -> Unit)?` | `null` | Callback when page changes |

#### DSL Example

```kotlin
Pagination(totalPages = 10) {
    currentPage = 3
    showFirstLast = true
    showPrevNext = true
    maxVisible = 7

    onPageChange = { page ->
        println("Load page: $page")
    }
}
```

#### YAML Example

```yaml
Pagination:
  id: content_pagination
  currentPage: 3
  totalPages: 10
  showFirstLast: true
  showPrevNext: true
  maxVisible: 7
```

#### Use Cases
- Data table navigation
- Search results pagination
- Blog post lists
- Any paginated content display

---

## Data Display Components

### Table

A table component for displaying structured data in rows and columns.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `columns` | `List<TableColumn>` | Required | Column definitions |
| `rows` | `List<TableRow>` | Required | Row data |
| `sortable` | `Boolean` | `false` | Enable column sorting |
| `hoverable` | `Boolean` | `true` | Highlight row on hover |
| `striped` | `Boolean` | `false` | Alternate row colors |
| `onRowClick` | `((Int) -> Unit)?` | `null` | Callback when row is clicked |

#### DSL Example

```kotlin
Table(
    columns = listOf(
        tableColumn("name", "Name", sortable = true),
        tableColumn("email", "Email"),
        tableColumn("role", "Role")
    ),
    rows = listOf(
        tableRow("1", listOf(
            tableCell("John Doe"),
            tableCell("john@example.com"),
            tableCell("Admin")
        )),
        tableRow("2", listOf(
            tableCell("Jane Smith"),
            tableCell("jane@example.com"),
            tableCell("User")
        ))
    )
) {
    sortable = true
    hoverable = true
    striped = false
    onRowClick = { index ->
        println("Row clicked: $index")
    }
}
```

#### YAML Example

```yaml
Table:
  id: users_table
  sortable: true
  hoverable: true
  columns:
    - id: name
      label: Name
      sortable: true
    - id: email
      label: Email
    - id: role
      label: Role
  rows:
    - id: "1"
      name: John Doe
      email: john@example.com
      role: Admin
    - id: "2"
      name: Jane Smith
      email: jane@example.com
      role: User
```

#### Use Cases
- User management interfaces
- Data dashboards
- Product listings
- Report displays

---

### List

A list component for displaying items with primary text, secondary text, icons, and avatars.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<ListItem>` | Required | List items |
| `selectable` | `Boolean` | `false` | Enable item selection |
| `selectedIndices` | `Set<Int>` | `emptySet()` | Selected item indices |
| `onItemClick` | `((Int) -> Unit)?` | `null` | Callback when item is clicked |

#### DSL Example

```kotlin
List(
    items = listOf(
        listItem(
            id = "1",
            primary = "John Doe",
            secondary = "Software Engineer",
            avatar = "https://example.com/avatar1.jpg"
        ),
        listItem(
            id = "2",
            primary = "Jane Smith",
            secondary = "Product Manager",
            icon = "person"
        )
    )
) {
    selectable = true
    onItemClick = { index ->
        println("Item clicked: $index")
    }
}
```

#### YAML Example

```yaml
List:
  id: users_list
  selectable: true
  items:
    - id: "1"
      primary: John Doe
      secondary: Software Engineer
      avatar: https://example.com/avatar1.jpg
    - id: "2"
      primary: Jane Smith
      secondary: Product Manager
      icon: person
```

#### Use Cases
- Contact lists
- Message threads
- Settings menus
- File browsers

---

### Accordion

An accordion component that displays collapsible content panels.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<AccordionItem>` | Required | Accordion sections |
| `expandedIndices` | `Set<Int>` | `emptySet()` | Indices of expanded sections |
| `allowMultiple` | `Boolean` | `false` | Allow multiple sections to be expanded |
| `onToggle` | `((Int) -> Unit)?` | `null` | Callback when section is toggled |

#### DSL Example

```kotlin
Accordion(
    items = listOf(
        accordionItem(
            id = "1",
            title = "Section 1",
            content = TextComponent(text = "Content for section 1", ...)
        ),
        accordionItem(
            id = "2",
            title = "Section 2",
            content = TextComponent(text = "Content for section 2", ...)
        )
    )
) {
    expandedIndices = setOf(0)
    allowMultiple = false
    onToggle = { index ->
        println("Toggled: $index")
    }
}
```

#### YAML Example

```yaml
Accordion:
  id: faq_accordion
  allowMultiple: false
  items:
    - id: "1"
      title: Section 1
      content: Content for section 1
    - id: "2"
      title: Section 2
      content: Content for section 2
```

#### Use Cases
- FAQ sections
- Settings panels
- Grouped content
- Expandable details

---

### Stepper

A stepper component that displays progress through numbered steps.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `steps` | `List<Step>` | Required | Step definitions |
| `currentStep` | `Int` | `0` | Currently active step index |
| `orientation` | `Orientation` | `Horizontal` | Layout orientation |
| `onStepClick` | `((Int) -> Unit)?` | `null` | Callback when step is clicked |

#### DSL Example

```kotlin
Stepper(
    steps = listOf(
        step("Account", description = "Create account", status = StepStatus.Complete),
        step("Profile", description = "Fill profile", status = StepStatus.Active),
        step("Verify", description = "Verify email", status = StepStatus.Pending)
    )
) {
    currentStep = 1
    orientation = Orientation.Horizontal
    onStepClick = { index ->
        println("Step clicked: $index")
    }
}
```

#### YAML Example

```yaml
Stepper:
  id: signup_stepper
  currentStep: 1
  orientation: Horizontal
  steps:
    - label: Account
      description: Create account
      status: Complete
    - label: Profile
      description: Fill profile
      status: Active
    - label: Verify
      description: Verify email
      status: Pending
```

#### Use Cases
- Multi-step forms
- Onboarding flows
- Checkout processes
- Progress tracking

---

### Timeline

A timeline component that displays events in chronological order.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<TimelineItem>` | Required | Timeline events |
| `orientation` | `Orientation` | `Vertical` | Layout orientation |

#### DSL Example

```kotlin
Timeline(
    items = listOf(
        timelineItem(
            id = "1",
            timestamp = "2025-01-15 10:30",
            title = "Order placed",
            description = "Your order has been placed",
            icon = "check_circle",
            color = Color.Green
        ),
        timelineItem(
            id = "2",
            timestamp = "2025-01-16 14:20",
            title = "Shipped",
            description = "Your order is on its way",
            icon = "local_shipping"
        )
    )
) {
    orientation = Orientation.Vertical
}
```

#### YAML Example

```yaml
Timeline:
  id: order_timeline
  orientation: Vertical
  items:
    - id: "1"
      timestamp: "2025-01-15 10:30"
      title: Order placed
      description: Your order has been placed
      icon: check_circle
      color: "#00FF00"
    - id: "2"
      timestamp: "2025-01-16 14:20"
      title: Shipped
      description: Your order is on its way
      icon: local_shipping
```

#### Use Cases
- Order tracking
- Activity feeds
- Project milestones
- Historical events display

---

### TreeView

A tree view component for displaying hierarchical data with expandable/collapsible nodes.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `nodes` | `List<TreeNode>` | Required | Root nodes |
| `expandedIds` | `Set<String>` | `emptySet()` | IDs of expanded nodes |
| `onNodeClick` | `((String) -> Unit)?` | `null` | Callback when node is clicked |
| `onToggle` | `((String) -> Unit)?` | `null` | Callback when node is expanded/collapsed |

#### DSL Example

```kotlin
TreeView(
    nodes = listOf(
        treeNode(
            id = "1",
            label = "Root Folder",
            icon = "folder",
            children = listOf(
                treeNode("1.1", "Subfolder 1", icon = "folder"),
                treeNode("1.2", "File 1.txt", icon = "description")
            )
        )
    )
) {
    expandedIds = setOf("1")
    onNodeClick = { id ->
        println("Node clicked: $id")
    }
    onToggle = { id ->
        println("Node toggled: $id")
    }
}
```

#### YAML Example

```yaml
TreeView:
  id: file_tree
  nodes:
    - id: "1"
      label: Root Folder
      icon: folder
      children:
        - id: "1.1"
          label: Subfolder 1
          icon: folder
        - id: "1.2"
          label: File 1.txt
          icon: description
```

#### Use Cases
- File explorers
- Organization hierarchies
- Nested menus
- Category structures

---

### Carousel

A carousel (slider) component for displaying multiple items in a scrollable view.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `items` | `List<Component>` | Required | Carousel items |
| `currentIndex` | `Int` | `0` | Currently visible item index |
| `autoPlay` | `Boolean` | `false` | Enable auto-play |
| `interval` | `Long` | `3000` | Auto-play interval in milliseconds |
| `showIndicators` | `Boolean` | `true` | Show slide indicators |
| `showControls` | `Boolean` | `true` | Show navigation controls |
| `onSlideChange` | `((Int) -> Unit)?` | `null` | Callback when slide changes |

#### DSL Example

```kotlin
Carousel(
    items = listOf(
        ImageComponent(source = "image1.jpg", ...),
        ImageComponent(source = "image2.jpg", ...),
        ImageComponent(source = "image3.jpg", ...)
    )
) {
    currentIndex = 0
    autoPlay = true
    interval = 3000
    showIndicators = true
    showControls = true
    onSlideChange = { index ->
        println("Slide: $index")
    }
}
```

#### YAML Example

```yaml
Carousel:
  id: hero_carousel
  autoPlay: true
  interval: 3000
  showIndicators: true
  showControls: true
  items:
    - Image:
        source: image1.jpg
    - Image:
        source: image2.jpg
    - Image:
        source: image3.jpg
```

#### Use Cases
- Image galleries
- Product showcases
- Hero sections
- Featured content

---

### Avatar

An avatar component for displaying user profile pictures or initials.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `source` | `String?` | `null` | Image URL for avatar |
| `text` | `String?` | `null` | Fallback initials if no image |
| `size` | `AvatarSize` | `Medium` | Size preset (Small/Medium/Large) |
| `shape` | `AvatarShape` | `Circle` | Shape (Circle/Square/Rounded) |

#### DSL Example

```kotlin
Avatar {
    source = "https://example.com/avatar.jpg"
    text = "JD" // fallback
    size = AvatarSize.Medium
    shape = AvatarShape.Circle
}
```

#### YAML Example

```yaml
Avatar:
  id: user_avatar
  source: https://example.com/avatar.jpg
  text: JD
  size: Medium
  shape: Circle
```

#### Use Cases
- User profiles
- Comment sections
- Team member displays
- Contact lists

---

### Chip

A chip (tag) component for displaying compact information or selections.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `label` | `String` | Required | Chip text |
| `icon` | `String?` | `null` | Optional icon |
| `deletable` | `Boolean` | `false` | Show delete button |
| `selected` | `Boolean` | `false` | Selected state |
| `onClick` | `(() -> Unit)?` | `null` | Callback when chip is clicked |
| `onDelete` | `(() -> Unit)?` | `null` | Callback when delete is clicked |

#### DSL Example

```kotlin
Chip(label = "Technology") {
    icon = "label"
    deletable = true
    selected = false
    onClick = { println("Chip clicked") }
    onDelete = { println("Chip deleted") }
}
```

#### YAML Example

```yaml
Chip:
  id: category_chip
  label: Technology
  icon: label
  deletable: true
  selected: false
```

#### Use Cases
- Tags and categories
- Filter selections
- Keywords
- Skills or attributes

---

### Divider

A divider component for visually separating content.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `orientation` | `Orientation` | `Horizontal` | Divider orientation |
| `thickness` | `Float` | `1f` | Divider thickness |
| `text` | `String?` | `null` | Optional text label |

#### DSL Example

```kotlin
Divider {
    orientation = Orientation.Horizontal
    thickness = 1f
    text = "OR"
}
```

#### YAML Example

```yaml
Divider:
  id: section_divider
  orientation: Horizontal
  thickness: 1
  text: OR
```

#### Use Cases
- Separating content sections
- Visual breaks
- Labeled dividers (e.g., "OR" between options)

---

### Paper

A paper (surface) component that provides an elevated surface for content.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `elevation` | `Int` | `1` | Shadow elevation level (0-5) |
| `children` | `List<Component>` | `emptyList()` | Child components |

#### DSL Example

```kotlin
Paper {
    elevation = 2

    Text("Content inside paper") {
        font = Font.Body
    }

    Button("Action") {
        buttonStyle = ButtonScope.ButtonStyle.Primary
    }
}
```

#### YAML Example

```yaml
Paper:
  id: card_surface
  elevation: 2
```

#### Use Cases
- Card-like containers
- Elevated panels
- Content grouping
- Visual hierarchy

---

### Skeleton

A skeleton (loading placeholder) component that displays an animated placeholder while content is loading.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `variant` | `SkeletonVariant` | `Text` | Shape type (Text/Rectangular/Circular) |
| `width` | `Size?` | `null` | Width override |
| `height` | `Size?` | `null` | Height override |
| `animation` | `SkeletonAnimation` | `Pulse` | Animation type (Pulse/Wave/None) |

#### DSL Example

```kotlin
Skeleton {
    variant = SkeletonVariant.Text
    width = Size.Fixed(200f)
    height = Size.Fixed(16f)
    animation = SkeletonAnimation.Pulse
}
```

#### YAML Example

```yaml
Skeleton:
  id: loading_skeleton
  variant: Text
  width: 200
  height: 16
  animation: Pulse
```

#### Use Cases
- Loading states
- Content placeholders
- Improved perceived performance
- Shimmer effects

---

### EmptyState

An empty state component for displaying a message when no content is available.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `icon` | `String?` | `null` | Optional icon |
| `title` | `String` | Required | Main message |
| `description` | `String?` | `null` | Additional details |
| `action` | `Component?` | `null` | Optional action button |

#### DSL Example

```kotlin
EmptyState(title = "No messages") {
    icon = "inbox"
    description = "You don't have any messages yet"
    action = ButtonComponent(
        text = "Compose",
        onClick = { /* compose */ },
        ...
    )
}
```

#### YAML Example

```yaml
EmptyState:
  id: inbox_empty
  icon: inbox
  title: No messages
  description: You don't have any messages yet
```

#### Use Cases
- Empty inboxes
- No search results
- First-time user experiences
- Deleted or cleared lists

---

### DataGrid

An advanced data grid component with sorting, pagination, and selection.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `columns` | `List<DataGridColumn>` | Required | Column definitions |
| `rows` | `List<DataGridRow>` | Required | Row data |
| `pageSize` | `Int` | `10` | Rows per page |
| `currentPage` | `Int` | `1` | Current page number |
| `sortBy` | `String?` | `null` | Column to sort by |
| `sortOrder` | `SortOrder` | `Ascending` | Sort direction |
| `selectable` | `Boolean` | `false` | Enable row selection |
| `selectedIds` | `Set<String>` | `emptySet()` | Selected row IDs |
| `onSort` | `((String, SortOrder) -> Unit)?` | `null` | Sort callback |
| `onPageChange` | `((Int) -> Unit)?` | `null` | Page change callback |
| `onSelectionChange` | `((Set<String>) -> Unit)?` | `null` | Selection change callback |

#### DSL Example

```kotlin
DataGrid(
    columns = listOf(
        dataGridColumn("name", "Name", sortable = true, align = TextAlign.Start),
        dataGridColumn("age", "Age", sortable = true, align = TextAlign.End)
    ),
    rows = listOf(
        dataGridRow("1", mapOf("name" to "John", "age" to 30)),
        dataGridRow("2", mapOf("name" to "Jane", "age" to 25))
    )
) {
    pageSize = 10
    sortBy = "name"
    sortOrder = SortOrder.Ascending
    selectable = true

    onSort = { column, order ->
        println("Sort: $column $order")
    }
    onPageChange = { page ->
        println("Page: $page")
    }
    onSelectionChange = { ids ->
        println("Selected: $ids")
    }
}
```

#### YAML Example

```yaml
DataGrid:
  id: advanced_grid
  pageSize: 10
  sortBy: name
  sortOrder: Ascending
  selectable: true
  columns:
    - id: name
      label: Name
      sortable: true
      align: Start
    - id: age
      label: Age
      sortable: true
      align: End
  rows:
    - id: "1"
      name: John
      age: 30
    - id: "2"
      name: Jane
      age: 25
```

#### Use Cases
- Complex data tables
- Admin panels
- Analytics dashboards
- Data management interfaces

---

## Platform Support

All components are designed for cross-platform compatibility:

- **Android**: MaterialComponents / Jetpack Compose
- **iOS**: UIKit components / SwiftUI
- **macOS**: AppKit components / SwiftUI
- **Web**: HTML/CSS with framework integration
- **visionOS**: SwiftUI with spatial adaptations
- **AndroidXR**: Material Design with XR enhancements

## Architecture Integration

### DSL Usage
All components follow the AvaElements DSL pattern with type-safe builders and extension functions. Import from:
```kotlin
import com.augmentalis.avaelements.dsl.*
```

### YAML Usage
Components can be defined declaratively in YAML and parsed at runtime:
```kotlin
val ui = YamlParser().parse(yamlString)
ui.render(renderer)
```

### Theme Integration
All components respect the active theme's color scheme, typography, and styling:
```kotlin
AvaUI {
    theme = Themes.iOS26LiquidGlass
    // components automatically adopt theme
}
```

## Next Steps

- Implement platform-specific renderers for all components
- Add animation and transition support
- Create comprehensive test suite
- Build demo applications showcasing all components
- Add accessibility features and ARIA support

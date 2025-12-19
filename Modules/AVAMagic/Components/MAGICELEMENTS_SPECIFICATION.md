# AvaElements Complete Library Specification

**Version**: 2.0.0
**Last Updated**: 2025-10-29
**Status**: Design Phase - Implementation Pending

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Design Philosophy](#design-philosophy)
3. [Syntax Formats](#syntax-formats)
4. [Component Catalog](#component-catalog)
5. [Platform Themes](#platform-themes)
6. [DSL Syntax Reference](#dsl-syntax-reference)
7. [YAML Syntax Reference](#yaml-syntax-reference)
8. [Type System](#type-system)
9. [Event System](#event-system)
10. [State Management](#state-management)
11. [Implementation Roadmap](#implementation-roadmap)

---

## Executive Summary

AvaElements is a cross-platform UI component library supporting **dual syntax**: Kotlin DSL (default) and YAML (declarative). It generates native UI for 7 major platforms while maintaining a single, unified API.

### Supported Platforms (7)

| Platform | Design System | Status |
|----------|--------------|--------|
| iOS 26 | Liquid Glass | Researched ✅ |
| macOS 26 | Liquid Glass | Researched ✅ |
| visionOS 2 | Spatial Glass | Researched ✅ |
| Windows 11 | Fluent 2 | Researched ✅ |
| Android | Material 3 Expressive | Researched ✅ |
| Android XR | Spatial Material | Researched ✅ |
| Samsung | One UI 7 | Researched ✅ |

### Syntax Support

- **DSL (Default)**: Type-safe Kotlin DSL with IDE autocomplete
- **YAML (Alternative)**: Declarative markup for visual designers and non-programmers
- **Bidirectional conversion**: DSL ↔ YAML with no loss of functionality

---

## Design Philosophy

### Core Principles

1. **Write Once, Run Everywhere**: Single API generates native UI per platform
2. **Dual Syntax**: DSL for developers, YAML for designers
3. **Platform Native**: Respects each platform's design language
4. **Type Safe**: Full compile-time type checking (DSL) with runtime validation (YAML)
5. **Progressive Enhancement**: Start simple, add complexity as needed
6. **Accessibility First**: WCAG 2.1 AA compliance built-in

### Architecture Goals

- **Zero Boilerplate**: Sensible defaults for everything
- **100% Declarative**: No imperative UI code
- **Composable**: Components nest naturally
- **Reactive**: Automatic UI updates from state changes
- **Testable**: All components unit-testable without UI

---

## Syntax Formats

### DSL (Default) - Kotlin

```kotlin
// Type-safe, IDE-supported, compile-time checked
AvaUI {
    theme = iOS26LiquidGlass

    Button(
        text = "Click Me",
        style = ButtonStyle.Primary,
        onClick = { println("Clicked!") }
    )

    TextField(
        value = state.username,
        placeholder = "Enter username",
        onValueChange = { state.username = it }
    )
}
```

### YAML (Alternative) - Declarative

```yaml
# Visual, designer-friendly, tooling-supported
theme: iOS26LiquidGlass

components:
  - Button:
      text: "Click Me"
      style: Primary
      onClick: handleClick

  - TextField:
      value: $state.username
      placeholder: "Enter username"
      onValueChange: updateUsername
```

### Conversion

```kotlin
// DSL → YAML
val yaml = magicUI.toYAML()

// YAML → DSL
val magicUI = AvaUI.fromYAML(yamlString)

// Validation
val errors = AvaUI.validateYAML(yamlString)
```

---

## Component Catalog

### Foundation Components (8)

Essential building blocks for all applications.

#### 1. Button

**Purpose**: Clickable action trigger

**DSL**:
```kotlin
Button(
    text = "Submit",
    style = ButtonStyle.Primary,          // Primary, Secondary, Tertiary, Danger, Ghost
    size = ButtonSize.Medium,             // Small, Medium, Large
    enabled = true,
    loading = false,
    icon = Icon.ArrowRight,
    iconPosition = IconPosition.Leading,  // Leading, Trailing
    fullWidth = false,
    onClick = { /* action */ }
)
```

**YAML**:
```yaml
Button:
  text: "Submit"
  style: Primary
  size: Medium
  enabled: true
  loading: false
  icon: ArrowRight
  iconPosition: Leading
  fullWidth: false
  onClick: handleSubmit
```

**Platform Rendering**:
- **iOS 26**: Liquid Glass pill button with shimmer
- **Android**: Material 3 filled button with dynamic color
- **Windows 11**: Acrylic button with hover effect
- **visionOS**: 3D glass button with depth

---

#### 2. TextField

**Purpose**: Single-line text input

**DSL**:
```kotlin
TextField(
    value = state.email,
    placeholder = "email@example.com",
    label = "Email Address",
    helperText = "We'll never share your email",
    errorText = if (invalid) "Invalid email" else null,
    inputType = InputType.Email,          // Text, Email, Password, Number, Phone, URL
    maxLength = 100,
    enabled = true,
    readOnly = false,
    required = true,
    autoFocus = false,
    leadingIcon = Icon.Mail,
    trailingIcon = Icon.Clear,
    onValueChange = { state.email = it },
    onFocusChange = { focused -> },
    onSubmit = { /* submit */ }
)
```

**YAML**:
```yaml
TextField:
  value: $state.email
  placeholder: "email@example.com"
  label: "Email Address"
  helperText: "We'll never share your email"
  errorText: $validation.emailError
  inputType: Email
  maxLength: 100
  required: true
  leadingIcon: Mail
  onValueChange: updateEmail
  onSubmit: handleSubmit
```

---

#### 3. Checkbox

**Purpose**: Boolean selection

**DSL**:
```kotlin
Checkbox(
    checked = state.agreed,
    label = "I agree to the terms",
    enabled = true,
    indeterminate = false,              // Tri-state support
    size = CheckboxSize.Medium,
    onCheckedChange = { state.agreed = it }
)
```

**YAML**:
```yaml
Checkbox:
  checked: $state.agreed
  label: "I agree to the terms"
  onCheckedChange: updateAgreement
```

---

#### 4. Card

**Purpose**: Content container with elevation

**DSL**:
```kotlin
Card(
    elevation = Elevation.Medium,
    cornerRadius = 12.dp,
    padding = 16.dp,
    onClick = { /* navigate */ }
) {
    Column(spacing = 8.dp) {
        Text("Card Title", style = TextStyle.Heading)
        Text("Card description text")
        Button("Action")
    }
}
```

**YAML**:
```yaml
Card:
  elevation: Medium
  cornerRadius: 12
  padding: 16
  onClick: handleCardClick
  children:
    - Column:
        spacing: 8
        children:
          - Text:
              text: "Card Title"
              style: Heading
          - Text:
              text: "Card description text"
          - Button:
              text: "Action"
```

---

#### 5. Text

**Purpose**: Static or dynamic text display

**DSL**:
```kotlin
Text(
    text = "Hello, World!",
    style = TextStyle.Heading,          // Heading, Subheading, Body, Caption, Label
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,       // Thin, Light, Regular, Medium, SemiBold, Bold, Black
    color = Color.Primary,
    textAlign = TextAlign.Center,       // Start, Center, End, Justify
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,   // Clip, Ellipsis, Fade
    selectable = false
)
```

**YAML**:
```yaml
Text:
  text: "Hello, World!"
  style: Heading
  fontSize: 16
  fontWeight: Bold
  color: Primary
  textAlign: Center
  maxLines: 2
  overflow: Ellipsis
```

---

#### 6. Image

**Purpose**: Display raster or vector images

**DSL**:
```kotlin
Image(
    source = "https://example.com/image.jpg",  // URL, local path, or resource
    contentDescription = "Profile picture",
    contentScale = ContentScale.Crop,          // Crop, Fit, Fill, None
    width = 100.dp,
    height = 100.dp,
    cornerRadius = 50.dp,                      // Makes it circular
    placeholder = "placeholder.png",
    error = "error.png",
    loading = LoadingStyle.Spinner,
    onClick = { /* zoom */ }
)
```

**YAML**:
```yaml
Image:
  source: "https://example.com/image.jpg"
  contentDescription: "Profile picture"
  contentScale: Crop
  width: 100
  height: 100
  cornerRadius: 50
  placeholder: "placeholder.png"
  onClick: handleImageClick
```

---

#### 7. Icon

**Purpose**: Vector icon display

**DSL**:
```kotlin
Icon(
    icon = Icon.Home,                    // From built-in icon set
    size = 24.dp,
    color = Color.Primary,
    contentDescription = "Home"
)
```

**YAML**:
```yaml
Icon:
  icon: Home
  size: 24
  color: Primary
  contentDescription: "Home"
```

---

#### 8. Switch

**Purpose**: Binary toggle

**DSL**:
```kotlin
Switch(
    checked = state.darkMode,
    label = "Dark Mode",
    enabled = true,
    onCheckedChange = { state.darkMode = it }
)
```

**YAML**:
```yaml
Switch:
  checked: $state.darkMode
  label: "Dark Mode"
  onCheckedChange: toggleDarkMode
```

---

### Layout Components (6)

Structure and organize other components.

#### 9. Column

**Purpose**: Vertical stacking layout

**DSL**:
```kotlin
Column(
    spacing = 16.dp,
    horizontalAlignment = Alignment.Start,  // Start, Center, End
    verticalArrangement = Arrangement.Top,  // Top, Center, Bottom, SpaceBetween, SpaceAround, SpaceEvenly
    padding = 16.dp,
    modifier = Modifier.fillMaxWidth()
) {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}
```

**YAML**:
```yaml
Column:
  spacing: 16
  horizontalAlignment: Start
  verticalArrangement: Top
  padding: 16
  children:
    - Text: { text: "Item 1" }
    - Text: { text: "Item 2" }
    - Text: { text: "Item 3" }
```

---

#### 10. Row

**Purpose**: Horizontal layout

**DSL**:
```kotlin
Row(
    spacing = 8.dp,
    verticalAlignment = Alignment.Center,
    horizontalArrangement = Arrangement.Start,
    padding = 16.dp
) {
    Icon(Icon.User)
    Text("Profile")
}
```

**YAML**:
```yaml
Row:
  spacing: 8
  verticalAlignment: Center
  children:
    - Icon: { icon: User }
    - Text: { text: "Profile" }
```

---

#### 11. Container

**Purpose**: Generic container with styling

**DSL**:
```kotlin
Container(
    width = 200.dp,
    height = 100.dp,
    padding = 16.dp,
    margin = 8.dp,
    backgroundColor = Color.Surface,
    cornerRadius = 8.dp,
    border = Border(1.dp, Color.Outline),
    elevation = Elevation.Small
) {
    Text("Content")
}
```

**YAML**:
```yaml
Container:
  width: 200
  height: 100
  padding: 16
  backgroundColor: Surface
  cornerRadius: 8
  children:
    - Text: { text: "Content" }
```

---

#### 12. Stack

**Purpose**: Z-axis layering (overlay)

**DSL**:
```kotlin
Stack {
    Image("background.jpg")
    Container(
        alignment = Alignment.BottomCenter,
        padding = 16.dp
    ) {
        Text("Overlay Text", color = Color.White)
    }
}
```

**YAML**:
```yaml
Stack:
  children:
    - Image: { source: "background.jpg" }
    - Container:
        alignment: BottomCenter
        children:
          - Text: { text: "Overlay Text", color: White }
```

---

#### 13. ScrollView

**Purpose**: Scrollable content area

**DSL**:
```kotlin
ScrollView(
    direction = ScrollDirection.Vertical,  // Vertical, Horizontal, Both
    showScrollbar = true
) {
    Column(spacing = 16.dp) {
        repeat(20) {
            Card { Text("Item $it") }
        }
    }
}
```

**YAML**:
```yaml
ScrollView:
  direction: Vertical
  showScrollbar: true
  children:
    - Column:
        spacing: 16
        children: $items  # Dynamic list
```

---

#### 14. Grid

**Purpose**: Grid layout

**DSL**:
```kotlin
Grid(
    columns = 3,
    spacing = 16.dp,
    padding = 16.dp
) {
    items(photos) { photo ->
        Image(source = photo.url)
    }
}
```

**YAML**:
```yaml
Grid:
  columns: 3
  spacing: 16
  items: $photos
  itemTemplate:
    Image:
      source: $item.url
```

---

### Form Components (8)

Specialized input controls.

#### 15. Radio

**Purpose**: Single selection from group

**DSL**:
```kotlin
RadioGroup(
    selected = state.selectedOption,
    onSelectionChange = { state.selectedOption = it }
) {
    Radio("Option 1", value = "opt1")
    Radio("Option 2", value = "opt2")
    Radio("Option 3", value = "opt3")
}
```

**YAML**:
```yaml
RadioGroup:
  selected: $state.selectedOption
  onSelectionChange: updateSelection
  options:
    - { label: "Option 1", value: "opt1" }
    - { label: "Option 2", value: "opt2" }
    - { label: "Option 3", value: "opt3" }
```

---

#### 16. Slider

**Purpose**: Numeric value selection via drag

**DSL**:
```kotlin
Slider(
    value = state.volume,
    min = 0f,
    max = 100f,
    step = 1f,
    label = "Volume",
    showValue = true,
    onValueChange = { state.volume = it }
)
```

**YAML**:
```yaml
Slider:
  value: $state.volume
  min: 0
  max: 100
  step: 1
  label: "Volume"
  showValue: true
  onValueChange: updateVolume
```

---

#### 17. Dropdown

**Purpose**: Select from list (collapsed)

**DSL**:
```kotlin
Dropdown(
    value = state.country,
    placeholder = "Select country",
    options = countries,
    onSelectionChange = { state.country = it }
)
```

**YAML**:
```yaml
Dropdown:
  value: $state.country
  placeholder: "Select country"
  options: $countries
  onSelectionChange: updateCountry
```

---

#### 18. DatePicker

**Purpose**: Date selection

**DSL**:
```kotlin
DatePicker(
    value = state.birthdate,
    minDate = Date(1900, 1, 1),
    maxDate = Date.today(),
    format = "MM/dd/yyyy",
    onValueChange = { state.birthdate = it }
)
```

**YAML**:
```yaml
DatePicker:
  value: $state.birthdate
  minDate: "1900-01-01"
  maxDate: $today
  format: "MM/dd/yyyy"
  onValueChange: updateBirthdate
```

---

#### 19. TimePicker

**Purpose**: Time selection

**DSL**:
```kotlin
TimePicker(
    value = state.appointmentTime,
    format = TimeFormat.Hour12,  // Hour12, Hour24
    onValueChange = { state.appointmentTime = it }
)
```

**YAML**:
```yaml
TimePicker:
  value: $state.appointmentTime
  format: Hour12
  onValueChange: updateTime
```

---

#### 20. FileUpload

**Purpose**: File selection and upload

**DSL**:
```kotlin
FileUpload(
    accept = listOf("image/*", "application/pdf"),
    maxSize = 10.MB,
    multiple = true,
    onFilesSelected = { files -> }
)
```

**YAML**:
```yaml
FileUpload:
  accept: ["image/*", "application/pdf"]
  maxSize: 10485760  # 10 MB in bytes
  multiple: true
  onFilesSelected: handleFiles
```

---

#### 21. SearchBar

**Purpose**: Search input with suggestions

**DSL**:
```kotlin
SearchBar(
    value = state.query,
    placeholder = "Search...",
    suggestions = state.suggestions,
    onValueChange = { state.query = it },
    onSearch = { performSearch(it) },
    onSuggestionClick = { selectSuggestion(it) }
)
```

**YAML**:
```yaml
SearchBar:
  value: $state.query
  placeholder: "Search..."
  suggestions: $state.suggestions
  onValueChange: updateQuery
  onSearch: performSearch
  onSuggestionClick: selectSuggestion
```

---

#### 22. Rating

**Purpose**: Star rating input

**DSL**:
```kotlin
Rating(
    value = state.rating,
    max = 5,
    allowHalf = true,
    size = RatingSize.Medium,
    onValueChange = { state.rating = it }
)
```

**YAML**:
```yaml
Rating:
  value: $state.rating
  max: 5
  allowHalf: true
  size: Medium
  onValueChange: updateRating
```

---

### Feedback Components (7)

User feedback and status indicators.

#### 23. Dialog

**Purpose**: Modal dialog/alert

**DSL**:
```kotlin
Dialog(
    open = state.showDialog,
    title = "Confirm Action",
    message = "Are you sure you want to proceed?",
    dismissible = true,
    onDismiss = { state.showDialog = false },
    actions = {
        Button("Cancel", style = ButtonStyle.Secondary) {
            state.showDialog = false
        }
        Button("Confirm", style = ButtonStyle.Primary) {
            confirmAction()
            state.showDialog = false
        }
    }
)
```

**YAML**:
```yaml
Dialog:
  open: $state.showDialog
  title: "Confirm Action"
  message: "Are you sure you want to proceed?"
  dismissible: true
  onDismiss: closeDialog
  actions:
    - Button:
        text: "Cancel"
        style: Secondary
        onClick: closeDialog
    - Button:
        text: "Confirm"
        style: Primary
        onClick: confirmAndClose
```

---

#### 24. Toast

**Purpose**: Temporary notification

**DSL**:
```kotlin
Toast(
    message = "Changes saved successfully",
    type = ToastType.Success,  // Success, Error, Warning, Info
    duration = 3.seconds,
    position = ToastPosition.Bottom,
    action = ToastAction("Undo") { undoChanges() }
)
```

**YAML**:
```yaml
Toast:
  message: "Changes saved successfully"
  type: Success
  duration: 3000
  position: Bottom
  action:
    text: "Undo"
    onClick: undoChanges
```

---

#### 25. Alert

**Purpose**: Inline alert message

**DSL**:
```kotlin
Alert(
    message = "Your session will expire in 5 minutes",
    type = AlertType.Warning,
    dismissible = true,
    icon = Icon.Warning,
    onDismiss = { }
)
```

**YAML**:
```yaml
Alert:
  message: "Your session will expire in 5 minutes"
  type: Warning
  dismissible: true
  icon: Warning
  onDismiss: dismissAlert
```

---

#### 26. ProgressBar

**Purpose**: Linear progress indicator

**DSL**:
```kotlin
ProgressBar(
    progress = state.uploadProgress,  // 0.0 to 1.0
    indeterminate = false,
    label = "Uploading...",
    showPercentage = true
)
```

**YAML**:
```yaml
ProgressBar:
  progress: $state.uploadProgress
  indeterminate: false
  label: "Uploading..."
  showPercentage: true
```

---

#### 27. Spinner

**Purpose**: Circular loading indicator

**DSL**:
```kotlin
Spinner(
    size = SpinnerSize.Medium,
    color = Color.Primary,
    message = "Loading..."
)
```

**YAML**:
```yaml
Spinner:
  size: Medium
  color: Primary
  message: "Loading..."
```

---

#### 28. Badge

**Purpose**: Small status indicator

**DSL**:
```kotlin
Badge(
    content = "3",
    type = BadgeType.Notification,  // Notification, Status, Count
    color = Color.Error,
    position = BadgePosition.TopRight
) {
    Icon(Icon.Notifications)
}
```

**YAML**:
```yaml
Badge:
  content: "3"
  type: Notification
  color: Error
  position: TopRight
  children:
    - Icon: { icon: Notifications }
```

---

#### 29. Tooltip

**Purpose**: Hover/focus hint

**DSL**:
```kotlin
Tooltip(
    text = "Click to save your changes",
    position = TooltipPosition.Top
) {
    Button("Save", icon = Icon.Save)
}
```

**YAML**:
```yaml
Tooltip:
  text: "Click to save your changes"
  position: Top
  children:
    - Button: { text: "Save", icon: Save }
```

---

### Navigation Components (6)

Application navigation controls.

#### 30. AppBar

**Purpose**: Top application bar

**DSL**:
```kotlin
AppBar(
    title = "My App",
    elevation = Elevation.Small,
    leading = IconButton(Icon.Menu) { openDrawer() },
    trailing = {
        IconButton(Icon.Search) { openSearch() }
        IconButton(Icon.Settings) { openSettings() }
    }
)
```

**YAML**:
```yaml
AppBar:
  title: "My App"
  elevation: Small
  leading:
    IconButton: { icon: Menu, onClick: openDrawer }
  trailing:
    - IconButton: { icon: Search, onClick: openSearch }
    - IconButton: { icon: Settings, onClick: openSettings }
```

---

#### 31. BottomNav

**Purpose**: Bottom navigation bar

**DSL**:
```kotlin
BottomNav(
    selected = state.currentTab,
    onSelectionChange = { state.currentTab = it }
) {
    NavItem("Home", Icon.Home, value = "home")
    NavItem("Search", Icon.Search, value = "search")
    NavItem("Profile", Icon.User, value = "profile")
}
```

**YAML**:
```yaml
BottomNav:
  selected: $state.currentTab
  onSelectionChange: updateTab
  items:
    - { label: "Home", icon: Home, value: "home" }
    - { label: "Search", icon: Search, value: "search" }
    - { label: "Profile", icon: User, value: "profile" }
```

---

#### 32. Tabs

**Purpose**: Tabbed content switcher

**DSL**:
```kotlin
Tabs(
    selected = state.activeTab,
    onSelectionChange = { state.activeTab = it }
) {
    Tab("Overview", value = "overview") {
        Text("Overview content")
    }
    Tab("Details", value = "details") {
        Text("Details content")
    }
}
```

**YAML**:
```yaml
Tabs:
  selected: $state.activeTab
  onSelectionChange: updateTab
  tabs:
    - label: "Overview"
      value: "overview"
      content:
        - Text: { text: "Overview content" }
    - label: "Details"
      value: "details"
      content:
        - Text: { text: "Details content" }
```

---

#### 33. Drawer

**Purpose**: Side navigation drawer

**DSL**:
```kotlin
Drawer(
    open = state.drawerOpen,
    position = DrawerPosition.Leading,  // Leading, Trailing
    onDismiss = { state.drawerOpen = false }
) {
    Column(spacing = 8.dp) {
        NavItem("Home", Icon.Home) { navigate("home") }
        NavItem("Settings", Icon.Settings) { navigate("settings") }
        Divider()
        NavItem("Logout", Icon.Logout) { logout() }
    }
}
```

**YAML**:
```yaml
Drawer:
  open: $state.drawerOpen
  position: Leading
  onDismiss: closeDrawer
  children:
    - Column:
        spacing: 8
        children:
          - NavItem: { label: "Home", icon: Home, onClick: navHome }
          - NavItem: { label: "Settings", icon: Settings, onClick: navSettings }
          - Divider
          - NavItem: { label: "Logout", icon: Logout, onClick: logout }
```

---

#### 34. Breadcrumb

**Purpose**: Hierarchical navigation trail

**DSL**:
```kotlin
Breadcrumb(
    separator = "/"
) {
    BreadcrumbItem("Home") { navigate("home") }
    BreadcrumbItem("Products") { navigate("products") }
    BreadcrumbItem("Details", active = true)
}
```

**YAML**:
```yaml
Breadcrumb:
  separator: "/"
  items:
    - { label: "Home", onClick: navHome }
    - { label: "Products", onClick: navProducts }
    - { label: "Details", active: true }
```

---

#### 35. Pagination

**Purpose**: Page navigation control

**DSL**:
```kotlin
Pagination(
    current = state.currentPage,
    total = state.totalPages,
    onPageChange = { state.currentPage = it },
    showFirstLast = true,
    maxVisible = 5
)
```

**YAML**:
```yaml
Pagination:
  current: $state.currentPage
  total: $state.totalPages
  onPageChange: updatePage
  showFirstLast: true
  maxVisible: 5
```

---

### Data Display Components (8)

Display structured data.

#### 36. Table

**Purpose**: Tabular data display

**DSL**:
```kotlin
Table(
    data = state.users,
    columns = listOf(
        Column("Name", field = "name", sortable = true),
        Column("Email", field = "email"),
        Column("Status", field = "status", filterable = true)
    ),
    sortBy = state.sortColumn,
    onSort = { column -> state.sortColumn = column },
    selectable = true,
    onRowClick = { user -> viewDetails(user) }
)
```

**YAML**:
```yaml
Table:
  data: $state.users
  columns:
    - { header: "Name", field: "name", sortable: true }
    - { header: "Email", field: "email" }
    - { header: "Status", field: "status", filterable: true }
  sortBy: $state.sortColumn
  onSort: updateSort
  selectable: true
  onRowClick: viewDetails
```

---

#### 37. List

**Purpose**: Scrollable list of items

**DSL**:
```kotlin
List(
    items = state.messages,
    dividers = true,
    onItemClick = { message -> openMessage(message) }
) { message ->
    ListItem(
        leading = Avatar(message.sender.avatar),
        title = message.sender.name,
        subtitle = message.preview,
        trailing = Text(message.time, style = TextStyle.Caption)
    )
}
```

**YAML**:
```yaml
List:
  items: $state.messages
  dividers: true
  onItemClick: openMessage
  itemTemplate:
    ListItem:
      leading:
        Avatar: { source: $item.sender.avatar }
      title: $item.sender.name
      subtitle: $item.preview
      trailing:
        Text: { text: $item.time, style: Caption }
```

---

#### 38. Accordion

**Purpose**: Expandable content sections

**DSL**:
```kotlin
Accordion(
    expanded = state.expandedSection,
    onExpandChange = { state.expandedSection = it }
) {
    AccordionItem("Section 1", value = "section1") {
        Text("Content for section 1")
    }
    AccordionItem("Section 2", value = "section2") {
        Text("Content for section 2")
    }
}
```

**YAML**:
```yaml
Accordion:
  expanded: $state.expandedSection
  onExpandChange: updateExpanded
  items:
    - title: "Section 1"
      value: "section1"
      content:
        - Text: { text: "Content for section 1" }
    - title: "Section 2"
      value: "section2"
      content:
        - Text: { text: "Content for section 2" }
```

---

#### 39. Stepper

**Purpose**: Multi-step process indicator

**DSL**:
```kotlin
Stepper(
    current = state.currentStep,
    onStepChange = { state.currentStep = it }
) {
    Step("Account", completed = true)
    Step("Profile", active = true)
    Step("Preferences")
}
```

**YAML**:
```yaml
Stepper:
  current: $state.currentStep
  onStepChange: updateStep
  steps:
    - { label: "Account", completed: true }
    - { label: "Profile", active: true }
    - { label: "Preferences" }
```

---

#### 40. Timeline

**Purpose**: Chronological event display

**DSL**:
```kotlin
Timeline(
    items = state.events,
    orientation = TimelineOrientation.Vertical
) { event ->
    TimelineItem(
        title = event.title,
        time = event.timestamp,
        icon = event.icon,
        content = { Text(event.description) }
    )
}
```

**YAML**:
```yaml
Timeline:
  items: $state.events
  orientation: Vertical
  itemTemplate:
    TimelineItem:
      title: $item.title
      time: $item.timestamp
      icon: $item.icon
      content:
        - Text: { text: $item.description }
```

---

#### 41. TreeView

**Purpose**: Hierarchical tree display

**DSL**:
```kotlin
TreeView(
    data = state.fileTree,
    expanded = state.expandedNodes,
    onExpandChange = { state.expandedNodes = it },
    onItemClick = { node -> openFile(node) }
)
```

**YAML**:
```yaml
TreeView:
  data: $state.fileTree
  expanded: $state.expandedNodes
  onExpandChange: updateExpanded
  onItemClick: openFile
```

---

#### 42. Carousel

**Purpose**: Rotating content slider

**DSL**:
```kotlin
Carousel(
    items = state.images,
    autoPlay = true,
    interval = 5.seconds,
    showDots = true,
    showArrows = true
) { image ->
    Image(source = image.url)
}
```

**YAML**:
```yaml
Carousel:
  items: $state.images
  autoPlay: true
  interval: 5000
  showDots: true
  showArrows: true
  itemTemplate:
    Image: { source: $item.url }
```

---

#### 43. Avatar

**Purpose**: User profile picture

**DSL**:
```kotlin
Avatar(
    source = user.avatarUrl,
    alt = user.name,
    size = AvatarSize.Medium,
    shape = AvatarShape.Circle,  // Circle, Square, Rounded
    fallback = user.initials,
    status = UserStatus.Online    // Online, Offline, Away, Busy
)
```

**YAML**:
```yaml
Avatar:
  source: $user.avatarUrl
  alt: $user.name
  size: Medium
  shape: Circle
  fallback: $user.initials
  status: Online
```

---

### Advanced Components (7)

Specialized, feature-rich components.

#### 44. ColorPicker

**Purpose**: Color selection tool

**DSL**:
```kotlin
ColorPicker(
    value = state.selectedColor,
    format = ColorFormat.Hex,  // Hex, RGB, HSL, HSV
    showAlpha = true,
    swatches = predefinedColors,
    onValueChange = { state.selectedColor = it }
)
```

**YAML**:
```yaml
ColorPicker:
  value: $state.selectedColor
  format: Hex
  showAlpha: true
  swatches: $predefinedColors
  onValueChange: updateColor
```

---

#### 45. CodeEditor

**Purpose**: Syntax-highlighted code editor

**DSL**:
```kotlin
CodeEditor(
    value = state.code,
    language = Language.Kotlin,
    theme = EditorTheme.DarkPlus,
    lineNumbers = true,
    readOnly = false,
    onValueChange = { state.code = it }
)
```

**YAML**:
```yaml
CodeEditor:
  value: $state.code
  language: Kotlin
  theme: DarkPlus
  lineNumbers: true
  onValueChange: updateCode
```

---

#### 46. Map

**Purpose**: Interactive map display

**DSL**:
```kotlin
Map(
    center = LatLng(37.7749, -122.4194),
    zoom = 12,
    markers = state.locations,
    onMarkerClick = { location -> showDetails(location) },
    onMapClick = { latLng -> addMarker(latLng) }
)
```

**YAML**:
```yaml
Map:
  center: { lat: 37.7749, lng: -122.4194 }
  zoom: 12
  markers: $state.locations
  onMarkerClick: showDetails
  onMapClick: addMarker
```

---

#### 47. Chart

**Purpose**: Data visualization

**DSL**:
```kotlin
Chart(
    type = ChartType.Line,  // Line, Bar, Pie, Scatter, Area
    data = state.chartData,
    xAxis = AxisConfig("Date"),
    yAxis = AxisConfig("Sales"),
    legend = true,
    interactive = true
)
```

**YAML**:
```yaml
Chart:
  type: Line
  data: $state.chartData
  xAxis: { label: "Date" }
  yAxis: { label: "Sales" }
  legend: true
  interactive: true
```

---

#### 48. RichTextEditor

**Purpose**: WYSIWYG text editor

**DSL**:
```kotlin
RichTextEditor(
    value = state.content,
    toolbar = ToolbarConfig.Full,  // Full, Basic, Custom
    onValueChange = { state.content = it }
)
```

**YAML**:
```yaml
RichTextEditor:
  value: $state.content
  toolbar: Full
  onValueChange: updateContent
```

---

#### 49. DragDrop

**Purpose**: Drag and drop container

**DSL**:
```kotlin
DragDrop(
    items = state.tasks,
    onReorder = { newOrder -> state.tasks = newOrder }
) { task ->
    Card {
        Text(task.title)
    }
}
```

**YAML**:
```yaml
DragDrop:
  items: $state.tasks
  onReorder: updateTaskOrder
  itemTemplate:
    Card:
      children:
        - Text: { text: $item.title }
```

---

#### 50. Video

**Purpose**: Video player

**DSL**:
```kotlin
Video(
    source = "https://example.com/video.mp4",
    poster = "thumbnail.jpg",
    controls = true,
    autoPlay = false,
    loop = false,
    muted = false,
    onPlay = { },
    onPause = { },
    onEnded = { }
)
```

**YAML**:
```yaml
Video:
  source: "https://example.com/video.mp4"
  poster: "thumbnail.jpg"
  controls: true
  autoPlay: false
  onPlay: handlePlay
  onPause: handlePause
  onEnded: handleEnded
```

---

## Platform Themes

### Theme System Architecture

```kotlin
// Define platform theme
sealed class PlatformTheme {
    object iOS26LiquidGlass : PlatformTheme()
    object MacOS26LiquidGlass : PlatformTheme()
    object VisionOS2SpatialGlass : PlatformTheme()
    object Windows11Fluent2 : PlatformTheme()
    object AndroidMaterial3 : PlatformTheme()
    object AndroidXRSpatial : PlatformTheme()
    object SamsungOneUI7 : PlatformTheme()
    data class Custom(val config: ThemeConfig) : PlatformTheme()
}

// Apply theme
AvaUI {
    theme = iOS26LiquidGlass
    // Components automatically use iOS 26 styling
}
```

### Theme Components

Each theme includes:

1. **Color Scheme**: Primary, secondary, tertiary, surface colors
2. **Typography**: Font families, sizes, weights
3. **Spacing**: Consistent padding, margin, gap values
4. **Shapes**: Corner radiuses, borders
5. **Elevation**: Shadow/depth system
6. **Motion**: Animation timings and curves
7. **Materials**: Glass, acrylic, mica effects

### Platform-Specific Features

| Feature | iOS 26 | Android | Windows 11 | visionOS |
|---------|--------|---------|------------|----------|
| **Glass Effects** | ✅ Liquid Glass | ❌ | ✅ Acrylic | ✅ Spatial Glass |
| **Dynamic Color** | ✅ Wallpaper | ✅ Material You | ✅ Accent | ❌ |
| **Depth/3D** | 🟡 Layers | 🟡 Elevation | 🟡 Depth | ✅ Z-axis |
| **Rounded Corners** | ✅ Variable | ✅ Variable | ✅ Subtle | ✅ 32dp |
| **Dark Mode** | ✅ | ✅ | ✅ | ✅ |

---

## DSL Syntax Reference

### Basic Structure

```kotlin
AvaUI {
    theme = PlatformTheme.iOS26LiquidGlass

    // State management
    val state = remember { mutableStateOf("") }

    // Component tree
    Column {
        Text("Hello")
        Button("Click") { }
    }
}
```

### Type Safety

```kotlin
// ✅ Correct - type-safe
Button(
    text = "Submit",
    style = ButtonStyle.Primary,
    onClick = { submitForm() }
)

// ❌ Error - caught at compile time
Button(
    text = 123,  // Error: Type mismatch
    style = "primary",  // Error: Expected ButtonStyle
    onClick = "submit"  // Error: Expected () -> Unit
)
```

### Composition

```kotlin
// Reusable components
@Composable
fun UserCard(user: User) {
    Card {
        Row(spacing = 12.dp) {
            Avatar(user.avatar)
            Column {
                Text(user.name, style = TextStyle.Heading)
                Text(user.email, style = TextStyle.Caption)
            }
        }
    }
}

// Usage
AvaUI {
    List(items = users) { user ->
        UserCard(user)
    }
}
```

### State Management

```kotlin
AvaUI {
    // Local state
    var count by remember { mutableStateOf(0) }

    Column {
        Text("Count: $count")
        Button("Increment") { count++ }
    }

    // Derived state
    val isEven = remember(count) { count % 2 == 0 }
    Text(if (isEven) "Even" else "Odd")
}
```

---

## YAML Syntax Reference

### Basic Structure

```yaml
theme: iOS26LiquidGlass

state:
  username: ""
  password: ""

components:
  - Column:
      spacing: 16
      children:
        - Text:
            text: "Login"
            style: Heading
        - TextField:
            value: $state.username
            placeholder: "Username"
            onValueChange: updateUsername
        - TextField:
            value: $state.password
            placeholder: "Password"
            inputType: Password
            onValueChange: updatePassword
        - Button:
            text: "Login"
            onClick: handleLogin
```

### References

```yaml
# State references
value: $state.username

# Computed values
text: $computed.fullName

# List iteration
items: $data.users

# Conditional rendering
visible: $state.isLoggedIn
```

### Validation

```yaml
# Schema validation
TextField:
  value: $state.email
  inputType: Email  # ✅ Valid enum value
  maxLength: 100    # ✅ Positive integer
  required: true    # ✅ Boolean

  # ❌ These would fail validation:
  # inputType: "email"  # Error: String instead of enum
  # maxLength: -5       # Error: Negative not allowed
  # required: "yes"     # Error: String instead of boolean
```

---

## Type System

### Primitive Types

```kotlin
// Numbers
val width: Dp = 100.dp
val height: Sp = 16.sp
val progress: Float = 0.75f

// Text
val label: String = "Hello"

// Boolean
val enabled: Boolean = true

// Colors
val color: Color = Color.Primary
val customColor: Color = Color(0xFF6200EE)
```

### Composite Types

```kotlin
// Dimensions
data class Size(val width: Dp, val height: Dp)

// Spacing
data class EdgeInsets(
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
    val left: Dp
)

// Alignment
enum class Alignment { Start, Center, End }
enum class Arrangement { Top, Center, Bottom, SpaceBetween }
```

### Platform-Specific Types

```kotlin
// iOS
expect class UIColor
actual typealias Color = UIColor

// Android
expect class ComposeColor
actual typealias Color = ComposeColor

// Common usage
val primary: Color = Color.Primary  // Works on all platforms
```

---

## Event System

### Event Types

```kotlin
// Click events
onClick: () -> Unit

// Value change events
onValueChange: (T) -> Unit

// Focus events
onFocusChange: (Boolean) -> Unit

// Keyboard events
onKeyPress: (KeyEvent) -> Unit

// Gesture events
onSwipe: (SwipeDirection) -> Unit
```

### Event Handling (DSL)

```kotlin
Button(
    text = "Submit",
    onClick = {
        validateForm()
        if (isValid) submitData()
    }
)
```

### Event Handling (YAML)

```yaml
Button:
  text: "Submit"
  onClick: handleSubmit  # Reference to registered handler

# Handler registration in code:
AvaUI.registerHandler("handleSubmit") {
    validateForm()
    if (isValid) submitData()
}
```

---

## State Management

### Local State

```kotlin
AvaUI {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it }
    )
}
```

### Shared State

```kotlin
class AppState {
    var isLoggedIn by mutableStateOf(false)
    var currentUser by mutableStateOf<User?>(null)
}

AvaUI(state = AppState()) {
    if (state.isLoggedIn) {
        DashboardScreen()
    } else {
        LoginScreen()
    }
}
```

### Reactive Updates

```kotlin
AvaUI {
    val users by viewModel.users.collectAsState()

    List(items = users) { user ->
        UserCard(user)
    }
    // UI automatically updates when users changes
}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)

**Goal**: Core infrastructure and basic components

1. **Type System** (Week 1)
   - Define all core types (Color, Size, Spacing, etc.)
   - Implement expect/actual for platform types
   - Create type converters

2. **DSL Builder** (Week 1-2)
   - Component builder pattern
   - Type-safe builders with @DslMarker
   - Composition API

3. **YAML Parser** (Week 2)
   - YAML schema definition
   - Parser implementation (kaml or kotlinx.serialization)
   - Validation engine

4. **Foundation Components** (Week 3-4)
   - Button, Text, TextField, Checkbox, Switch
   - Basic layouts: Column, Row, Container
   - Initial testing

**Deliverables**: 8 working components with DSL + YAML support

---

### Phase 2: Platform Themes (Weeks 5-8)

**Goal**: Implement 3 platform themes

1. **Theme System Architecture** (Week 5)
   - Theme interface and base classes
   - Color scheme system
   - Typography system
   - Material system (glass, acrylic, etc.)

2. **Material 3 Theme** (Week 6)
   - Android Material 3 implementation
   - Dynamic color support
   - Component styling

3. **iOS 26 Liquid Glass** (Week 7)
   - Liquid Glass materials
   - iOS-specific styling
   - Component adaptations

4. **Windows 11 Fluent 2** (Week 8)
   - Mica/Acrylic/Smoke materials
   - WinUI 3 styling
   - Component implementations

**Deliverables**: 3 complete platform themes

---

### Phase 3: Advanced Components (Weeks 9-12)

**Goal**: Complete component library

1. **Form Components** (Week 9)
   - Radio, Slider, Dropdown, DatePicker, TimePicker
   - FileUpload, SearchBar, Rating

2. **Feedback Components** (Week 10)
   - Dialog, Toast, Alert, ProgressBar
   - Spinner, Badge, Tooltip

3. **Navigation Components** (Week 11)
   - AppBar, BottomNav, Tabs, Drawer
   - Breadcrumb, Pagination

4. **Data Display** (Week 12)
   - Table, List, Accordion, Stepper
   - Timeline, TreeView, Carousel, Avatar

**Deliverables**: 35 additional components (43 total)

---

### Phase 4: Remaining Themes (Weeks 13-16)

**Goal**: Complete all 7 platform themes

1. **macOS 26 Liquid Glass** (Week 13)
2. **visionOS 2 Spatial Glass** (Week 14)
3. **Android XR Spatial Material** (Week 15)
4. **Samsung One UI 7** (Week 16)

**Deliverables**: All 7 platform themes complete

---

### Phase 5: Advanced Features (Weeks 17-20)

**Goal**: Specialized and advanced components

1. **Advanced Components** (Week 17-18)
   - ColorPicker, CodeEditor, Map, Chart
   - RichTextEditor, DragDrop, Video

2. **Testing & Documentation** (Week 19)
   - Unit tests for all components
   - Integration tests
   - API documentation
   - Usage examples

3. **Polish & Optimization** (Week 20)
   - Performance optimization
   - Accessibility improvements
   - Bug fixes
   - Release preparation

**Deliverables**: Production-ready library with 50 components and 7 themes

---

## Total Effort Estimate

| Phase | Duration | Components | Themes | Cumulative |
|-------|----------|-----------|--------|------------|
| Phase 1 | 4 weeks | 8 | 0 | 8 components |
| Phase 2 | 4 weeks | 0 | 3 | 8 components, 3 themes |
| Phase 3 | 4 weeks | 35 | 0 | 43 components, 3 themes |
| Phase 4 | 4 weeks | 0 | 4 | 43 components, 7 themes |
| Phase 5 | 4 weeks | 7 | 0 | 50 components, 7 themes |
| **Total** | **20 weeks** | **50** | **7** | **Complete library** |

---

## Next Steps

1. **Review & Approve** this specification
2. **Set up project structure** in Avanues repository
3. **Begin Phase 1** implementation (Foundation)
4. **Create example apps** for each component/theme
5. **Iterate** based on feedback

---

**Document Status**: ✅ Specification Complete
**Created By**: Claude Code
**For**: AvaElements Cross-Platform UI Library
**Next**: Phase 1 Implementation

---

*This is a living specification and will be updated as implementation progresses and requirements evolve.*

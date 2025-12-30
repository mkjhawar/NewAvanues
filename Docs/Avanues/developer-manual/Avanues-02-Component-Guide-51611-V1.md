# AvaElements Component Guide

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Components:** 67 total (13 foundation + 54 advanced)

---

## Table of Contents

1. [Component Categories](#component-categories)
2. [Foundation Components (Phase 1)](#foundation-components-phase-1)
3. [Form & Input Components](#form--input-components)
4. [Display Components](#display-components)
5. [Navigation Components](#navigation-components)
6. [Layout Components](#layout-components)
7. [Feedback Components](#feedback-components)
8. [Component API Reference](#component-api-reference)

---

## Component Categories

AvaElements provides **67 pre-built components** organized into 6 categories:

| Category | Count | Description |
|----------|-------|-------------|
| **Foundation** | 13 | Essential UI building blocks (Text, Button, Card, etc.) |
| **Form & Input** | 12 | User input components (Slider, DatePicker, Dropdown, etc.) |
| **Display** | 8 | Visual feedback components (Badge, Chip, Avatar, etc.) |
| **Navigation** | 4 | App navigation (AppBar, BottomNav, Breadcrumb, etc.) |
| **Layout** | 5 | UI structure (Grid, Stack, Drawer, etc.) |
| **Feedback** | 7 | User notifications (Alert, Toast, Modal, etc.) |
| **Data** | 18 | Data visualization (Table, List, TreeView, etc.) |

**Total:** 67 components

---

## Foundation Components (Phase 1)

These 13 components form the foundation of AvaElements and are **100% complete** on Android.

### 1. Text

Display text with automatic theme styling.

```kotlin
import com.augmentalis.avaelements.components.phase1.basic.Text

val myText = Text(
    id = "welcome-text",
    text = "Welcome to AvaElements!",
    style = ComponentStyle(
        color = Color("#2196F3"),
        fontSize = 18f
    )
)
```

**Props:**
- `text: String` - Text content to display
- `style: ComponentStyle?` - Optional styling (color, fontSize, fontWeight)

**Android Rendering:** Uses `androidx.compose.material3.Text`

---

### 2. Button

Material3 button with click handling.

```kotlin
import com.augmentalis.avaelements.components.phase1.form.Button

val submitButton = Button(
    id = "submit-btn",
    text = "Submit",
    onClick = { println("Button clicked!") },
    enabled = true
)
```

**Props:**
- `text: String` - Button label
- `onClick: (() -> Unit)?` - Click handler
- `enabled: Boolean` - Enable/disable state (default: true)
- `style: ComponentStyle?` - Optional styling

**Android Rendering:** Uses `androidx.compose.material3.Button` with `ButtonDefaults.buttonColors()`

---

### 3. TextField

Text input field with label and validation support.

```kotlin
import com.augmentalis.avaelements.components.phase1.form.TextField

val emailField = TextField(
    id = "email-input",
    value = "",
    onChange = { newValue -> println("Value: $newValue") },
    label = "Email Address",
    placeholder = "Enter your email",
    errorText = null
)
```

**Props:**
- `value: String` - Current text value
- `onChange: ((String) -> Unit)?` - Value change handler
- `label: String?` - Input label
- `placeholder: String?` - Placeholder text
- `errorText: String?` - Validation error message

**Android Rendering:** Uses `androidx.compose.material3.OutlinedTextField`

---

### 4. Checkbox

Material3 checkbox with label.

```kotlin
import com.augmentalis.avaelements.components.phase1.form.Checkbox

val termsCheckbox = Checkbox(
    id = "terms-check",
    checked = false,
    onCheckedChange = { isChecked -> println("Checked: $isChecked") },
    label = "I agree to terms and conditions"
)
```

**Props:**
- `checked: Boolean` - Checked state
- `onCheckedChange: ((Boolean) -> Unit)?` - State change handler
- `label: String?` - Checkbox label

**Android Rendering:** Uses `androidx.compose.material3.Checkbox`

---

### 5. Switch

Toggle switch component.

```kotlin
import com.augmentalis.avaelements.components.phase1.form.Switch

val darkModeSwitch = Switch(
    id = "dark-mode",
    checked = false,
    onCheckedChange = { enabled -> println("Dark mode: $enabled") },
    label = "Dark Mode"
)
```

**Props:**
- `checked: Boolean` - Switch state
- `onCheckedChange: ((Boolean) -> Unit)?` - State change handler
- `label: String?` - Switch label

**Android Rendering:** Uses `androidx.compose.material3.Switch`

---

### 6. Card

Material3 card container with elevation.

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.Card

val profileCard = Card(
    id = "profile-card",
    title = "User Profile",
    content = "John Doe\njohn@example.com",
    onClick = { println("Card clicked") }
)
```

**Props:**
- `title: String?` - Card title
- `content: String` - Card content text
- `onClick: (() -> Unit)?` - Click handler
- `style: ComponentStyle?` - Optional styling

**Android Rendering:** Uses `androidx.compose.material3.Card` with `CardDefaults.cardElevation()`

---

### 7. Image

Image display component with content description.

```kotlin
import com.augmentalis.avaelements.components.phase1.basic.Image

val logoImage = Image(
    id = "app-logo",
    url = "https://example.com/logo.png",
    contentDescription = "App Logo",
    width = 200f,
    height = 200f
)
```

**Props:**
- `url: String` - Image URL or local path
- `contentDescription: String?` - Accessibility description
- `width: Float?` - Image width in dp
- `height: Float?` - Image height in dp

**Android Rendering:** Uses `androidx.compose.foundation.Image` (Coil for network images)

---

### 8. Icon

Material Icons support.

```kotlin
import com.augmentalis.avaelements.components.phase1.basic.Icon

val homeIcon = Icon(
    id = "home-icon",
    icon = "home",
    size = 24f,
    style = ComponentStyle(color = Color("#2196F3"))
)
```

**Props:**
- `icon: String` - Icon name (Material Icons)
- `size: Float` - Icon size in dp (default: 24f)
- `style: ComponentStyle?` - Optional color styling

**Android Rendering:** Uses `androidx.compose.material.icons.Icons.Filled.*`

---

### 9. Column

Vertical linear layout.

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.Column

val contentColumn = Column(
    id = "content-column",
    spacing = 16f
)
```

**Props:**
- `spacing: Float` - Spacing between children in dp
- `style: ComponentStyle?` - Optional styling

**Android Rendering:** Uses `androidx.compose.foundation.layout.Column`

---

### 10. Row

Horizontal linear layout.

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.Row

val buttonRow = Row(
    id = "button-row",
    spacing = 8f
)
```

**Props:**
- `spacing: Float` - Spacing between children in dp
- `style: ComponentStyle?` - Optional styling

**Android Rendering:** Uses `androidx.compose.foundation.layout.Row`

---

### 11. Container

Generic container with background and padding.

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.Container

val contentContainer = Container(
    id = "main-container",
    padding = 16f,
    style = ComponentStyle(backgroundColor = Color("#FFFFFF"))
)
```

**Props:**
- `padding: Float` - Internal padding in dp
- `style: ComponentStyle?` - Background color and styling

**Android Rendering:** Uses `androidx.compose.foundation.layout.Box`

---

### 12. ScrollView

Scrollable content container.

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.ScrollView

val scrollContent = ScrollView(
    id = "scroll-container",
    direction = "vertical"
)
```

**Props:**
- `direction: String` - "vertical" or "horizontal" (default: "vertical")
- `style: ComponentStyle?` - Optional styling

**Android Rendering:** Uses `androidx.compose.foundation.verticalScroll()` or `horizontalScroll()`

---

## Form & Input Components

12 advanced input components for user interaction.

### 1. Slider

Numeric value slider with range and steps.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Slider

val volumeSlider = Slider(
    id = "volume-slider",
    value = 50f,
    onValueChange = { newValue -> println("Volume: $newValue") },
    min = 0f,
    max = 100f,
    step = 1f
)
```

**Props:**
- `value: Float` - Current value
- `onValueChange: ((Float) -> Unit)?` - Value change handler
- `min: Float` - Minimum value (default: 0f)
- `max: Float` - Maximum value (default: 100f)
- `step: Float` - Step size (default: 1f)

**Android:** Material3 `Slider` with theme colors

---

### 2. DatePicker

Modal date picker dialog.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.DatePicker

val birthdayPicker = DatePicker(
    id = "birthday-picker",
    selectedDate = null,
    onDateChange = { dateString -> println("Selected: $dateString") }
)
```

**Props:**
- `selectedDate: String?` - Current selected date (ISO format)
- `onDateChange: ((String) -> Unit)?` - Date selection handler

**Android:** Material3 `DatePickerDialog` with `rememberDatePickerState()`

---

### 3. TimePicker

Modal time picker dialog.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.TimePicker

val alarmTimePicker = TimePicker(
    id = "alarm-time",
    selectedTime = null,
    onTimeChange = { timeString -> println("Time: $timeString") }
)
```

**Props:**
- `selectedTime: String?` - Current time (HH:mm format)
- `onTimeChange: ((String) -> Unit)?` - Time selection handler

**Android:** Material3 `TimePicker` with `rememberTimePickerState()`

---

### 4. Dropdown

Dropdown menu with options.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Dropdown

val countryDropdown = Dropdown(
    id = "country-select",
    options = listOf("USA", "Canada", "UK", "Australia"),
    selectedValue = null,
    onSelectionChange = { selected -> println("Country: $selected") },
    placeholder = "Select country"
)
```

**Props:**
- `options: List<String>` - Available options
- `selectedValue: String?` - Currently selected value
- `onSelectionChange: ((String) -> Unit)?` - Selection handler
- `placeholder: String` - Placeholder text

**Android:** Material3 `ExposedDropdownMenuBox`

---

### 5. Radio

Radio button group.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Radio

val genderRadio = Radio(
    id = "gender-radio",
    options = listOf("Male", "Female", "Other"),
    selectedValue = null,
    onSelectionChange = { selected -> println("Gender: $selected") }
)
```

**Props:**
- `options: List<String>` - Radio options
- `selectedValue: String?` - Selected option
- `onSelectionChange: ((String) -> Unit)?` - Selection handler

**Android:** Material3 `RadioButton` in `Column`

---

### 6. Rating

Star rating component.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Rating

val productRating = Rating(
    id = "product-rating",
    rating = 4.5f,
    maxRating = 5,
    onRatingChange = { newRating -> println("Rating: $newRating") }
)
```

**Props:**
- `rating: Float` - Current rating value
- `maxRating: Int` - Maximum rating (default: 5)
- `onRatingChange: ((Float) -> Unit)?` - Rating change handler

**Android:** Material Icons `Star` and `StarBorder` in `Row`

---

### 7. ColorPicker

Color selection component.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.ColorPicker

val themeColorPicker = ColorPicker(
    id = "theme-color",
    selectedColor = "#2196F3",
    onColorChange = { colorHex -> println("Color: $colorHex") }
)
```

**Props:**
- `selectedColor: String` - Current color (hex format)
- `onColorChange: ((String) -> Unit)?` - Color change handler

**Android:** Custom color grid with `Surface` components

---

### 8. FileUpload

File upload component with drag-and-drop support.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.FileUpload

val documentUpload = FileUpload(
    id = "doc-upload",
    acceptedTypes = listOf("pdf", "docx"),
    maxSize = 10_000_000, // 10MB
    onFileSelected = { file -> println("File: $file") }
)
```

**Props:**
- `acceptedTypes: List<String>` - Accepted file extensions
- `maxSize: Int` - Max file size in bytes
- `onFileSelected: ((String) -> Unit)?` - File selection handler

**Android:** Material3 `Button` with file picker intent

---

### 9. SearchBar

Material3 search bar with suggestions.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.SearchBar

val productSearch = SearchBar(
    id = "product-search",
    query = "",
    onQueryChange = { query -> println("Search: $query") },
    placeholder = "Search products..."
)
```

**Props:**
- `query: String` - Current search query
- `onQueryChange: ((String) -> Unit)?` - Query change handler
- `placeholder: String` - Placeholder text

**Android:** Material3 `SearchBar` with `ExperimentalMaterial3Api`

---

### 10. Stepper

Numeric stepper with increment/decrement buttons.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Stepper

val quantityStepper = Stepper(
    id = "quantity",
    value = 1,
    onValueChange = { newValue -> println("Quantity: $newValue") },
    min = 1,
    max = 99
)
```

**Props:**
- `value: Int` - Current value
- `onValueChange: ((Int) -> Unit)?` - Value change handler
- `min: Int` - Minimum value (default: 0)
- `max: Int` - Maximum value (default: 100)

**Android:** Material3 `IconButton` with plus/minus icons

---

### 11. Toggle

Toggle button component.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Toggle

val notificationsToggle = Toggle(
    id = "notifications",
    checked = true,
    onCheckedChange = { enabled -> println("Notifications: $enabled") },
    label = "Enable Notifications"
)
```

**Props:**
- `checked: Boolean` - Toggle state
- `onCheckedChange: ((Boolean) -> Unit)?` - State change handler
- `label: String?` - Toggle label

**Android:** Material3 `Switch` (alias for foundation Switch)

---

### 12. Autocomplete

Autocomplete text field with suggestions.

```kotlin
import com.augmentalis.avaelements.components.phase3.form.Autocomplete

val cityAutocomplete = Autocomplete(
    id = "city-autocomplete",
    value = "",
    onValueChange = { newValue -> println("City: $newValue") },
    suggestions = listOf("New York", "Los Angeles", "Chicago"),
    placeholder = "Enter city name"
)
```

**Props:**
- `value: String` - Current input value
- `onValueChange: ((String) -> Unit)?` - Input change handler
- `suggestions: List<String>` - Available suggestions
- `placeholder: String` - Placeholder text

**Android:** Material3 `TextField` with `DropdownMenu`

---

## Display Components

8 components for visual feedback and data presentation.

### 1. Badge

Small status indicator badge.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Badge

val notificationBadge = Badge(
    id = "notification-badge",
    text = "3",
    variant = "error" // "error", "warning", "success", "info", "default"
)
```

**Props:**
- `text: String` - Badge text content
- `variant: String` - Badge style variant (default: "default")

**Android:** Material3 `Badge` with variant-based coloring

---

### 2. Chip

Compact material chip component.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Chip

val tagChip = Chip(
    id = "tag-chip",
    label = "Technology",
    onDelete = { println("Chip deleted") }
)
```

**Props:**
- `label: String` - Chip label
- `onDelete: (() -> Unit)?` - Optional delete handler (shows close icon if provided)

**Android:** Material3 `InputChip` (with delete) or `SuggestionChip` (read-only)

---

### 3. Avatar

Circular avatar with initials or image.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Avatar

val userAvatar = Avatar(
    id = "user-avatar",
    imageUrl = null,
    initials = "JD",
    size = 48f
)
```

**Props:**
- `imageUrl: String?` - Optional avatar image URL
- `initials: String?` - Fallback initials text
- `size: Float` - Avatar diameter in dp (default: 40f)

**Android:** Circular `Box` with `Image` or `Text`

---

### 4. Divider

Horizontal or vertical divider line.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Divider

val sectionDivider = Divider(
    id = "section-divider",
    orientation = "horizontal", // "horizontal" or "vertical"
    thickness = 1f
)
```

**Props:**
- `orientation: String` - "horizontal" or "vertical" (default: "horizontal")
- `thickness: Float` - Line thickness in dp (default: 1f)

**Android:** Material3 `HorizontalDivider` or `VerticalDivider`

---

### 5. Skeleton

Animated loading placeholder.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Skeleton

val contentSkeleton = Skeleton(
    id = "content-skeleton",
    width = 200f,
    height = 20f,
    variant = "rounded" // "rectangular", "rounded", "circular"
)
```

**Props:**
- `width: Float` - Skeleton width in dp
- `height: Float` - Skeleton height in dp
- `variant: String` - Shape variant (default: "rectangular")

**Android:** Animated `Box` with `rememberInfiniteTransition` shimmer effect

---

### 6. Spinner

Circular loading spinner.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Spinner

val loadingSpinner = Spinner(
    id = "loading-spinner",
    size = 40f
)
```

**Props:**
- `size: Float` - Spinner diameter in dp (default: 40f)

**Android:** Material3 `CircularProgressIndicator`

---

### 7. ProgressBar

Linear progress bar with percentage label.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.ProgressBar

val uploadProgress = ProgressBar(
    id = "upload-progress",
    progress = 0.65f, // 0.0 to 1.0
    showLabel = true
)
```

**Props:**
- `progress: Float` - Progress value (0.0 to 1.0)
- `showLabel: Boolean` - Show percentage label (default: false)

**Android:** Material3 `LinearProgressIndicator`

---

### 8. Tooltip

Hover/press tooltip.

```kotlin
import com.augmentalis.avaelements.components.phase3.display.Tooltip

val helpTooltip = Tooltip(
    id = "help-tooltip",
    text = "Click here for help"
)
```

**Props:**
- `text: String` - Tooltip text content

**Android:** Material3 `TooltipBox` with `PlainTooltip`

---

## Navigation Components

4 components for app navigation structure.

### 1. AppBar

Top application bar with title and navigation.

```kotlin
import com.augmentalis.avaelements.components.phase3.navigation.AppBar

val mainAppBar = AppBar(
    id = "main-appbar",
    title = "Home",
    showBack = false,
    onBackClick = { println("Back clicked") }
)
```

**Props:**
- `title: String` - AppBar title
- `showBack: Boolean` - Show back button (default: false)
- `onBackClick: (() -> Unit)?` - Back button click handler

**Android:** Material3 `TopAppBar` with `navigationIcon`

---

### 2. BottomNav

Bottom navigation bar.

```kotlin
import com.augmentalis.avaelements.components.phase3.navigation.BottomNav

val mainBottomNav = BottomNav(
    id = "main-bottomnav",
    items = listOf("Home", "Search", "Profile"),
    selectedIndex = 0,
    onItemClick = { index -> println("Tab: $index") }
)
```

**Props:**
- `items: List<String>` - Navigation item labels
- `selectedIndex: Int` - Currently selected index
- `onItemClick: ((Int) -> Unit)?` - Item click handler

**Android:** Material3 `NavigationBar` with `NavigationBarItem`

---

### 3. Breadcrumb

Breadcrumb navigation trail.

```kotlin
import com.augmentalis.avaelements.components.phase3.navigation.Breadcrumb

val pageBreadcrumb = Breadcrumb(
    id = "page-breadcrumb",
    items = listOf("Home", "Products", "Electronics"),
    onItemClick = { index -> println("Navigate to: $index") }
)
```

**Props:**
- `items: List<String>` - Breadcrumb trail items
- `onItemClick: ((Int) -> Unit)?` - Item click handler

**Android:** `Row` with `Text` components and arrow separators

---

### 4. Pagination

Page navigation with numbered pages.

```kotlin
import com.augmentalis.avaelements.components.phase3.navigation.Pagination

val tablePagination = Pagination(
    id = "table-pagination",
    currentPage = 1,
    totalPages = 10,
    onPageChange = { page -> println("Page: $page") }
)
```

**Props:**
- `currentPage: Int` - Current page number (1-indexed)
- `totalPages: Int` - Total number of pages
- `onPageChange: ((Int) -> Unit)?` - Page change handler

**Android:** `Row` with page number buttons and prev/next icons

---

## Layout Components

5 components for UI structure and organization.

### 1. Grid

Multi-column grid layout.

```kotlin
import com.augmentalis.avaelements.components.phase3.layout.Grid

val imageGrid = Grid(
    id = "image-grid",
    columns = 3,
    gap = 8f
)
```

**Props:**
- `columns: Int` - Number of columns (default: 2)
- `gap: Float` - Gap between items in dp (default: 8f)

**Android:** Custom implementation with `Column` (LazyVerticalGrid in production)

---

### 2. Stack

Z-axis layering container.

```kotlin
import com.augmentalis.avaelements.components.phase3.layout.Stack

val overlayStack = Stack(
    id = "overlay-stack",
    alignment = "center" // topStart, topCenter, topEnd, centerStart, center, etc.
)
```

**Props:**
- `alignment: String` - Content alignment (default: "center")

**Android:** `Box` with alignment mapping

---

### 3. Spacer

Empty space component.

```kotlin
import com.augmentalis.avaelements.components.phase3.layout.Spacer

val verticalSpace = Spacer(
    id = "vertical-space",
    size = 16f
)
```

**Props:**
- `size: Float` - Space size in dp (default: 8f)

**Android:** `androidx.compose.foundation.layout.Spacer`

---

### 4. Drawer

Navigation drawer (side menu).

```kotlin
import com.augmentalis.avaelements.components.phase3.layout.Drawer

val navDrawer = Drawer(
    id = "nav-drawer",
    open = false,
    onOpenChange = { isOpen -> println("Drawer: $isOpen") }
)
```

**Props:**
- `open: Boolean` - Drawer open state
- `onOpenChange: ((Boolean) -> Unit)?` - State change handler

**Android:** Material3 `ModalNavigationDrawer` with state synchronization

---

### 5. Tabs

Horizontal tab navigation.

```kotlin
import com.augmentalis.avaelements.components.phase3.layout.Tabs

val contentTabs = Tabs(
    id = "content-tabs",
    tabs = listOf("Overview", "Details", "Reviews"),
    selectedIndex = 0,
    onTabChange = { index -> println("Tab: $index") }
)
```

**Props:**
- `tabs: List<String>` - Tab labels
- `selectedIndex: Int` - Selected tab index
- `onTabChange: ((Int) -> Unit)?` - Tab change handler

**Android:** Material3 `TabRow` with `Tab` components

---

## Feedback Components

7 components for user notifications and alerts.

### 1. Alert

Alert dialog with title, message, and actions.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Alert

val deleteAlert = Alert(
    id = "delete-alert",
    title = "Confirm Delete",
    message = "Are you sure you want to delete this item?",
    severity = "warning", // "info", "success", "warning", "error"
    onConfirm = { println("Confirmed") },
    onCancel = { println("Cancelled") }
)
```

**Props:**
- `title: String` - Alert title
- `message: String` - Alert message
- `severity: String` - Alert severity level (default: "info")
- `onConfirm: (() -> Unit)?` - Confirm button handler
- `onCancel: (() -> Unit)?` - Cancel button handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 2. Toast

Temporary notification message.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Toast

val successToast = Toast(
    id = "success-toast",
    message = "Item saved successfully!",
    duration = 3000, // milliseconds
    position = "bottom" // "top", "bottom"
)
```

**Props:**
- `message: String` - Toast message
- `duration: Int` - Display duration in ms (default: 3000)
- `position: String` - Display position (default: "bottom")

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 3. Snackbar

Material snackbar with optional action.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Snackbar

val undoSnackbar = Snackbar(
    id = "undo-snackbar",
    message = "Item deleted",
    actionLabel = "Undo",
    onAction = { println("Undo clicked") }
)
```

**Props:**
- `message: String` - Snackbar message
- `actionLabel: String?` - Optional action button label
- `onAction: (() -> Unit)?` - Action button handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 4. Modal

Full-screen modal dialog.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Modal

val detailsModal = Modal(
    id = "details-modal",
    title = "Product Details",
    open = false,
    onClose = { println("Modal closed") }
)
```

**Props:**
- `title: String?` - Modal title
- `open: Boolean` - Modal visibility state
- `onClose: (() -> Unit)?` - Close handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 5. Dialog

Standard dialog window.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Dialog

val confirmDialog = Dialog(
    id = "confirm-dialog",
    title = "Confirm Action",
    content = "Do you want to proceed?",
    open = false,
    onConfirm = { println("Confirmed") },
    onCancel = { println("Cancelled") }
)
```

**Props:**
- `title: String` - Dialog title
- `content: String` - Dialog content
- `open: Boolean` - Dialog visibility
- `onConfirm: (() -> Unit)?` - Confirm handler
- `onCancel: (() -> Unit)?` - Cancel handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 6. Banner

Persistent banner notification.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.Banner

val updateBanner = Banner(
    id = "update-banner",
    message = "A new version is available!",
    actionLabel = "Update",
    onAction = { println("Update clicked") },
    onDismiss = { println("Banner dismissed") }
)
```

**Props:**
- `message: String` - Banner message
- `actionLabel: String?` - Action button label
- `onAction: (() -> Unit)?` - Action handler
- `onDismiss: (() -> Unit)?` - Dismiss handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

### 7. ContextMenu

Right-click context menu.

```kotlin
import com.augmentalis.avaelements.components.phase3.feedback.ContextMenu

val fileContextMenu = ContextMenu(
    id = "file-context-menu",
    items = listOf("Open", "Rename", "Delete"),
    onItemClick = { index -> println("Action: $index") }
)
```

**Props:**
- `items: List<String>` - Menu items
- `onItemClick: ((Int) -> Unit)?` - Item click handler

**Status:** ⏳ Renderer pending (Phase 2.4)

---

## Component API Reference

### Component Interface

All components implement the base `Component` interface:

```kotlin
interface Component {
    val id: String?
    val style: ComponentStyle?
    val modifiers: List<Modifier>
    fun render(renderer: Renderer): @Composable (() -> Unit)
}
```

### ComponentStyle

Optional styling for components:

```kotlin
data class ComponentStyle(
    val backgroundColor: Color? = null,
    val color: Color? = null,
    val fontSize: Float? = null,
    val fontWeight: String? = null,
    val padding: Float? = null,
    val margin: Float? = null,
    val borderRadius: Float? = null,
    val elevation: Float? = null
)
```

### Color

Color representation:

```kotlin
data class Color(
    val value: String // Hex format: "#RRGGBB" or "#AARRGGBB"
)
```

### Modifier

Component modifiers (platform-agnostic):

```kotlin
sealed class Modifier {
    data class Padding(val value: Float) : Modifier()
    data class Size(val width: Float, val height: Float) : Modifier()
    data class Background(val color: Color) : Modifier()
    // ... more modifiers
}
```

---

## Component Status Matrix

| Component | Defined | Android | iOS | Web |
|-----------|---------|---------|-----|-----|
| **Foundation (13)** | ✅ | ✅ | ⏳ | ⏳ |
| Text | ✅ | ✅ | ⏳ | ⏳ |
| Button | ✅ | ✅ | ⏳ | ⏳ |
| TextField | ✅ | ✅ | ⏳ | ⏳ |
| Checkbox | ✅ | ✅ | ⏳ | ⏳ |
| Switch | ✅ | ✅ | ⏳ | ⏳ |
| Card | ✅ | ✅ | ⏳ | ⏳ |
| Image | ✅ | ✅ | ⏳ | ⏳ |
| Icon | ✅ | ✅ | ⏳ | ⏳ |
| Column | ✅ | ✅ | ⏳ | ⏳ |
| Row | ✅ | ✅ | ⏳ | ⏳ |
| Container | ✅ | ✅ | ⏳ | ⏳ |
| ScrollView | ✅ | ✅ | ⏳ | ⏳ |
| **Form & Input (12)** | ✅ | ✅ | ⏳ | ⏳ |
| Slider | ✅ | ✅ | ⏳ | ⏳ |
| DatePicker | ✅ | ✅ | ⏳ | ⏳ |
| TimePicker | ✅ | ✅ | ⏳ | ⏳ |
| Dropdown | ✅ | ✅ | ⏳ | ⏳ |
| Radio | ✅ | ✅ | ⏳ | ⏳ |
| Rating | ✅ | ✅ | ⏳ | ⏳ |
| ColorPicker | ✅ | ✅ | ⏳ | ⏳ |
| FileUpload | ✅ | ✅ | ⏳ | ⏳ |
| SearchBar | ✅ | ✅ | ⏳ | ⏳ |
| Stepper | ✅ | ✅ | ⏳ | ⏳ |
| Toggle | ✅ | ✅ | ⏳ | ⏳ |
| Autocomplete | ✅ | ✅ | ⏳ | ⏳ |
| **Display (8)** | ✅ | ✅ | ⏳ | ⏳ |
| Badge | ✅ | ✅ | ⏳ | ⏳ |
| Chip | ✅ | ✅ | ⏳ | ⏳ |
| Avatar | ✅ | ✅ | ⏳ | ⏳ |
| Divider | ✅ | ✅ | ⏳ | ⏳ |
| Skeleton | ✅ | ✅ | ⏳ | ⏳ |
| Spinner | ✅ | ✅ | ⏳ | ⏳ |
| ProgressBar | ✅ | ✅ | ⏳ | ⏳ |
| Tooltip | ✅ | ✅ | ⏳ | ⏳ |
| **Navigation (4)** | ✅ | ✅ | ⏳ | ⏳ |
| AppBar | ✅ | ✅ | ⏳ | ⏳ |
| BottomNav | ✅ | ✅ | ⏳ | ⏳ |
| Breadcrumb | ✅ | ✅ | ⏳ | ⏳ |
| Pagination | ✅ | ✅ | ⏳ | ⏳ |
| **Layout (5)** | ✅ | ✅ | ⏳ | ⏳ |
| Grid | ✅ | ✅ | ⏳ | ⏳ |
| Stack | ✅ | ✅ | ⏳ | ⏳ |
| Spacer | ✅ | ✅ | ⏳ | ⏳ |
| Drawer | ✅ | ✅ | ⏳ | ⏳ |
| Tabs | ✅ | ✅ | ⏳ | ⏳ |
| **Feedback (6)** | ✅ | ✅ | ⏳ | ⏳ |
| Alert | ✅ | ✅ | ⏳ | ⏳ |
| Toast | ✅ | ✅ | ⏳ | ⏳ |
| Snackbar | ✅ | ✅ | ⏳ | ⏳ |
| Modal | ✅ | ✅ | ⏳ | ⏳ |
| Confirm | ✅ | ✅ | ⏳ | ⏳ |
| ContextMenu | ✅ | ✅ | ⏳ | ⏳ |

**Legend:**
- ✅ Complete
- ⏳ Planned
- 🚧 In Progress

---

## Next Steps

📖 **Continue to:**
- [Chapter 03 - Android Renderer Deep Dive](./03-Android-Renderer.md)
- [Chapter 04 - Theme System](./04-Theme-System.md)
- [Chapter 05 - Building Custom Components](./05-Custom-Components.md)

📚 **Tutorials:**
- [Tutorial 01 - Building a Login Screen](../tutorials/01-Login-Screen.md)
- [Tutorial 02 - Form Validation](../tutorials/02-Form-Validation.md)

---

**Version:** 2.0.0
**Total Components:** 67 (49 Android-ready, 18 pending)
**Framework:** AvaElements on Kotlin Multiplatform
**Methodology:** IDEACODE 7.2.0
**Author:** Manoj Jhawar (manoj@ideahq.net)

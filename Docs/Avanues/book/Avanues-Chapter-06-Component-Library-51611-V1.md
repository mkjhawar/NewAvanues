# Chapter 6: Component Library

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~10,000 words

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Foundation Components (9)](#2-foundation-components)
3. [Core Components (2)](#3-core-components)
4. [Basic Components (6)](#4-basic-components)
5. [Advanced Components (18)](#5-advanced-components)
6. [Layout Components (8)](#6-layout-components)
7. [Navigation Components (5)](#7-navigation-components)
8. [Summary](#8-summary)

---

## 1. Introduction

The **Component Library** contains **48 components** organized into 6 categories. Each component has:

- **Properties** - Configuration options
- **Event Handlers** - Callback functions
- **Child Support** - Whether it can contain children
- **Platform Code** - Generated Kotlin/Swift/TypeScript examples

### Component Categories

1. **Foundation (9)** - Core UI building blocks (Button, Card, Text, etc.)
2. **Core (2)** - Advanced pickers (ColorPicker, IconPicker)
3. **Basic (6)** - Simple components (Icon, Label, Spacer, etc.)
4. **Advanced (18)** - Complex components (Dialog, DatePicker, Rating, etc.)
5. **Layout (8)** - Container components (Stack, Grid, ScrollView, etc.)
6. **Navigation (5)** - Navigation components (AppBar, BottomNav, Drawer, etc.)

---

## 2. Foundation Components (9)

### 2.1 Button

**Purpose:** Clickable button with text label

**Properties:**
- `text` (String) - Button label
- `variant` (Enum) - `primary`, `secondary`, `outline`, `text`
- `enabled` (Boolean) - Whether button is clickable
- `fullWidth` (Boolean) - Expand to full width

**Event Handlers:**
- `onClick` - Called when button is clicked

**Child Support:** No

**Generated Code (Android):**

```kotlin
Button(
    onClick = { handleClick() },
    enabled = true
) {
    Text("Click Me")
}
```

**Generated Code (iOS):**

```swift
Button("Click Me") {
    handleClick()
}
.buttonStyle(.borderedProminent)
```

**Generated Code (Web):**

```typescript
<Button
  variant="contained"
  onClick={() => handleClick()}
>
  Click Me
</Button>
```

---

### 2.2 Card

**Purpose:** Container with elevation/shadow

**Properties:**
- `elevation` (Int) - Shadow depth (0-24)
- `backgroundColor` (String) - Background color
- `cornerRadius` (Int) - Border radius

**Event Handlers:**
- `onClick` - Optional click handler

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
) {
    // Children here
    Text("Card Content")
}
```

---

### 2.3 Checkbox

**Purpose:** Toggle selection control

**Properties:**
- `checked` (Boolean) - Whether checked
- `label` (String) - Optional label text
- `enabled` (Boolean) - Whether interactive

**Event Handlers:**
- `onCheckedChange` (Boolean) - Called when checked state changes

**Child Support:** No

**Generated Code (Android):**

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(
        checked = isChecked,
        onCheckedChange = { isChecked = it }
    )
    Text("Agree to Terms")
}
```

---

### 2.4 Chip

**Purpose:** Compact element for tags/filters

**Properties:**
- `text` (String) - Chip label
- `icon` (String) - Optional leading icon
- `closeable` (Boolean) - Show close button
- `variant` (Enum) - `filled`, `outlined`

**Event Handlers:**
- `onClick` - Called when chip clicked
- `onClose` - Called when close button clicked

**Child Support:** No

**Generated Code (Android):**

```kotlin
AssistChip(
    onClick = { handleClick() },
    label = { Text("Filter") },
    leadingIcon = {
        Icon(Icons.Filled.Check, contentDescription = null)
    }
)
```

---

### 2.5 Divider

**Purpose:** Visual separator line

**Properties:**
- `thickness` (Int) - Line thickness in dp/px
- `color` (String) - Line color
- `orientation` (Enum) - `horizontal`, `vertical`

**Event Handlers:** None

**Child Support:** No

**Generated Code (Android):**

```kotlin
HorizontalDivider(
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outline
)
```

---

### 2.6 Image

**Purpose:** Display image from URL or asset

**Properties:**
- `src` (String) - Image URL or asset path
- `contentDescription` (String) - Accessibility description
- `aspectRatio` (Float) - Width/height ratio
- `fit` (Enum) - `cover`, `contain`, `fill`, `fitWidth`, `fitHeight`

**Event Handlers:**
- `onClick` - Optional click handler

**Child Support:** No

**Generated Code (Android):**

```kotlin
Image(
    painter = painterResource(id = R.drawable.logo),
    contentDescription = "App Logo",
    contentScale = ContentScale.Crop,
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
)
```

---

### 2.7 ListItem

**Purpose:** List row with title, subtitle, icon

**Properties:**
- `title` (String) - Main text
- `subtitle` (String) - Secondary text
- `leadingIcon` (String) - Icon on left
- `trailingIcon` (String) - Icon on right
- `divider` (Boolean) - Show bottom divider

**Event Handlers:**
- `onClick` - Called when item clicked

**Child Support:** No

**Generated Code (Android):**

```kotlin
ListItem(
    headlineContent = { Text("Item Title") },
    supportingContent = { Text("Subtitle") },
    leadingContent = {
        Icon(Icons.Filled.Star, contentDescription = null)
    },
    modifier = Modifier.clickable { handleClick() }
)
```

---

### 2.8 Text

**Purpose:** Display text content

**Properties:**
- `content` (String) - Text content
- `variant` (Enum) - `H1`, `H2`, `H3`, `H4`, `H5`, `H6`, `BODY1`, `BODY2`, `CAPTION`
- `color` (String) - Text color
- `align` (Enum) - `left`, `center`, `right`, `justify`
- `maxLines` (Int) - Maximum lines
- `overflow` (Enum) - `clip`, `ellipsis`, `visible`

**Event Handlers:** None

**Child Support:** No

**Generated Code (Android):**

```kotlin
Text(
    text = "Hello World",
    style = MaterialTheme.typography.headlineLarge,
    color = MaterialTheme.colorScheme.onBackground,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

---

### 2.9 TextField

**Purpose:** Text input field

**Properties:**
- `value` (String) - Current value
- `label` (String) - Floating label
- `placeholder` (String) - Placeholder text
- `helperText` (String) - Helper text below field
- `errorText` (String) - Error message
- `type` (Enum) - `text`, `password`, `email`, `number`, `phone`
- `multiline` (Boolean) - Allow multiple lines
- `maxLength` (Int) - Maximum character count
- `enabled` (Boolean) - Whether editable

**Event Handlers:**
- `onValueChange` (String) - Called when value changes
- `onFocusChange` (Boolean) - Called when focus changes

**Child Support:** No

**Generated Code (Android):**

```kotlin
OutlinedTextField(
    value = username,
    onValueChange = { username = it },
    label = { Text("Username") },
    placeholder = { Text("Enter username") },
    supportingText = { Text("Required field") },
    isError = username.isEmpty(),
    modifier = Modifier.fillMaxWidth()
)
```

---

## 3. Core Components (2)

### 3.1 ColorPicker

**Purpose:** Advanced color selection with HSV/RGB/Hex

**Properties:**
- `initialColor` (String) - Starting color (hex format)
- `showAlpha` (Boolean) - Show alpha/opacity slider
- `showHex` (Boolean) - Show hex input field
- `allowEyedropper` (Boolean) - Show eyedropper tool

**Event Handlers:**
- `onColorChange` (String) - Called when color changes (hex format)

**Child Support:** No

**Generated Code (Android):**

```kotlin
ColorPickerView(
    initialColor = Color(0xFFFF5733),
    showAlpha = true,
    showHex = true,
    onColorChange = { color ->
        selectedColor = color
    }
)
```

---

### 3.2 IconPicker

**Purpose:** Icon selection from Material/FontAwesome libraries

**Properties:**
- `library` (Enum) - `material`, `fontawesome`
- `selectedIcon` (String) - Currently selected icon
- `searchable` (Boolean) - Show search bar
- `categoryFilters` (List<String>) - Filter by categories

**Event Handlers:**
- `onIconSelect` (String) - Called when icon selected

**Child Support:** No

**Generated Code (Android):**

```kotlin
IconPickerView(
    library = IconLibrary.MATERIAL,
    selectedIcon = "favorite",
    searchable = true,
    onIconSelect = { icon ->
        selectedIcon = icon
    }
)
```

---

## 4. Basic Components (6)

### 4.1 Icon

**Purpose:** Display vector icon

**Properties:**
- `name` (String) - Icon identifier
- `size` (Int) - Icon size in dp/px
- `color` (String) - Icon color
- `library` (Enum) - `material`, `fontawesome`

**Event Handlers:**
- `onClick` - Optional click handler

**Child Support:** No

**Generated Code (Android):**

```kotlin
Icon(
    imageVector = Icons.Filled.Favorite,
    contentDescription = "Favorite",
    tint = MaterialTheme.colorScheme.primary
)
```

---

### 4.2 Label

**Purpose:** Text label with background

**Properties:**
- `text` (String) - Label text
- `backgroundColor` (String) - Background color
- `textColor` (String) - Text color
- `variant` (Enum) - `default`, `success`, `warning`, `error`, `info`

**Event Handlers:** None

**Child Support:** No

---

### 4.3 Container

**Purpose:** Generic container for children

**Properties:**
- `padding` (Int) - Inner padding
- `backgroundColor` (String) - Background color
- `width` (String) - Width (e.g., "100%", "200dp")
- `height` (String) - Height

**Event Handlers:**
- `onClick` - Optional click handler

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)
) {
    // Children
}
```

---

### 4.4 Row

**Purpose:** Horizontal linear layout

**Properties:**
- `spacing` (Int) - Space between children
- `horizontalArrangement` (Enum) - `start`, `center`, `end`, `spaceBetween`, `spaceAround`, `spaceEvenly`
- `verticalAlignment` (Enum) - `top`, `centerVertically`, `bottom`

**Event Handlers:** None

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Left")
    Text("Right")
}
```

---

### 4.5 Column

**Purpose:** Vertical linear layout

**Properties:**
- `spacing` (Int) - Space between children
- `verticalArrangement` (Enum) - `top`, `center`, `bottom`, `spaceBetween`, `spaceAround`, `spaceEvenly`
- `horizontalAlignment` (Enum) - `start`, `centerHorizontally`, `end`

**Event Handlers:** None

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}
```

---

### 4.6 Spacer

**Purpose:** Empty space between components

**Properties:**
- `size` (Int) - Space size in dp/px
- `orientation` (Enum) - `horizontal`, `vertical`

**Event Handlers:** None

**Child Support:** No

**Generated Code (Android):**

```kotlin
Spacer(modifier = Modifier.height(16.dp))
```

---

## 5. Advanced Components (18)

### 5.1 Switch

**Purpose:** Toggle switch control

**Properties:**
- `checked` (Boolean) - Whether on/off
- `label` (String) - Optional label
- `enabled` (Boolean) - Whether interactive

**Event Handlers:**
- `onCheckedChange` (Boolean) - Called when toggled

**Child Support:** No

---

### 5.2 Slider

**Purpose:** Range slider for numeric input

**Properties:**
- `value` (Float) - Current value
- `min` (Float) - Minimum value
- `max` (Float) - Maximum value
- `step` (Float) - Value increment step
- `showValue` (Boolean) - Display current value

**Event Handlers:**
- `onValueChange` (Float) - Called when value changes

**Child Support:** No

**Generated Code (Android):**

```kotlin
Column {
    Text("Volume: ${volume.toInt()}")
    Slider(
        value = volume,
        onValueChange = { volume = it },
        valueRange = 0f..100f,
        steps = 10
    )
}
```

---

### 5.3 ProgressBar

**Purpose:** Show progress or loading state

**Properties:**
- `value` (Float) - Progress value (0.0 - 1.0)
- `indeterminate` (Boolean) - Infinite loading animation
- `variant` (Enum) - `linear`, `circular`

**Event Handlers:** None

**Child Support:** No

---

### 5.4 Dialog

**Purpose:** Modal dialog overlay

**Properties:**
- `title` (String) - Dialog title
- `visible` (Boolean) - Whether shown
- `dismissable` (Boolean) - Allow dismiss on outside click
- `fullscreen` (Boolean) - Full screen mode

**Event Handlers:**
- `onDismiss` - Called when dialog dismissed
- `onConfirm` - Called when confirm button clicked
- `onCancel` - Called when cancel button clicked

**Child Support:** Yes (dialog content)

**Generated Code (Android):**

```kotlin
AlertDialog(
    onDismissRequest = { showDialog = false },
    title = { Text("Confirm Action") },
    text = { Text("Are you sure?") },
    confirmButton = {
        Button(onClick = { handleConfirm() }) {
            Text("Confirm")
        }
    },
    dismissButton = {
        TextButton(onClick = { showDialog = false }) {
            Text("Cancel")
        }
    }
)
```

---

### 5.5 Toast

**Purpose:** Brief notification message

**Properties:**
- `message` (String) - Toast text
- `duration` (Enum) - `short`, `long`
- `position` (Enum) - `top`, `bottom`, `center`

**Event Handlers:** None

**Child Support:** No

---

### 5.6 Dropdown

**Purpose:** Selection dropdown menu

**Properties:**
- `options` (List<String>) - Menu options
- `selectedIndex` (Int) - Currently selected index
- `label` (String) - Field label
- `placeholder` (String) - Placeholder text

**Event Handlers:**
- `onSelect` (Int, String) - Called when option selected

**Child Support:** No

**Generated Code (Android):**

```kotlin
ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
) {
    OutlinedTextField(
        value = selectedOption,
        onValueChange = {},
        readOnly = true,
        label = { Text("Select Option") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    selectedOption = option
                    expanded = false
                }
            )
        }
    }
}
```

---

### 5.7 DatePicker

**Purpose:** Date selection calendar

**Properties:**
- `selectedDate` (String) - Current date (ISO format)
- `minDate` (String) - Minimum selectable date
- `maxDate` (String) - Maximum selectable date
- `format` (String) - Display format (e.g., "MM/DD/YYYY")

**Event Handlers:**
- `onDateSelect` (String) - Called when date selected

**Child Support:** No

---

### 5.8 TimePicker

**Purpose:** Time selection control

**Properties:**
- `selectedTime` (String) - Current time (HH:MM format)
- `format24h` (Boolean) - Use 24-hour format

**Event Handlers:**
- `onTimeSelect` (String) - Called when time selected

**Child Support:** No

---

### 5.9 Rating

**Purpose:** Star rating input

**Properties:**
- `value` (Float) - Current rating (0.0 - 5.0)
- `max` (Int) - Maximum rating (default 5)
- `allowHalf` (Boolean) - Allow half-star ratings
- `readonly` (Boolean) - Display only

**Event Handlers:**
- `onRatingChange` (Float) - Called when rating changes

**Child Support:** No

---

## 6. Layout Components (8)

### 6.1 Stack

**Purpose:** Layered components (z-index)

**Properties:**
- `alignment` (Enum) - `topStart`, `topCenter`, `topEnd`, `centerStart`, `center`, etc.

**Event Handlers:** None

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Image(...) // Background
    Text("Overlay")
}
```

---

### 6.2 Grid

**Purpose:** Grid layout with rows/columns

**Properties:**
- `columns` (Int) - Number of columns
- `spacing` (Int) - Space between items
- `verticalSpacing` (Int) - Vertical spacing
- `horizontalSpacing` (Int) - Horizontal spacing

**Event Handlers:** None

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    contentPadding = PaddingValues(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(items) { item ->
        GridItem(item)
    }
}
```

---

### 6.3 ScrollView

**Purpose:** Scrollable container

**Properties:**
- `direction` (Enum) - `vertical`, `horizontal`, `both`
- `showScrollbar` (Boolean) - Display scrollbar

**Event Handlers:**
- `onScroll` (Float) - Called when scrolled (position 0.0 - 1.0)

**Child Support:** Yes

**Generated Code (Android):**

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
) {
    // Scrollable content
}
```

---

### 6.4 Tabs

**Purpose:** Tabbed interface

**Properties:**
- `tabs` (List<String>) - Tab labels
- `selectedIndex` (Int) - Active tab index
- `variant` (Enum) - `standard`, `scrollable`

**Event Handlers:**
- `onTabSelect` (Int) - Called when tab selected

**Child Support:** Yes (tab content)

---

### 6.5 Accordion

**Purpose:** Expandable sections

**Properties:**
- `sections` (List<AccordionSection>) - Section data
- `allowMultiple` (Boolean) - Allow multiple open sections
- `initialExpanded` (List<Int>) - Initially expanded section indices

**Event Handlers:**
- `onSectionToggle` (Int, Boolean) - Called when section expanded/collapsed

**Child Support:** Yes

---

## 7. Navigation Components (5)

### 7.1 AppBar

**Purpose:** Top app bar with title and actions

**Properties:**
- `title` (String) - Bar title
- `showBackButton` (Boolean) - Show back navigation
- `actions` (List<Action>) - Right-side action buttons
- `variant` (Enum) - `small`, `medium`, `large`

**Event Handlers:**
- `onBackClick` - Called when back button clicked
- `onActionClick` (String) - Called when action clicked

**Child Support:** No

**Generated Code (Android):**

```kotlin
TopAppBar(
    title = { Text("Screen Title") },
    navigationIcon = {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.Filled.ArrowBack, "Back")
        }
    },
    actions = {
        IconButton(onClick = { handleSearch() }) {
            Icon(Icons.Filled.Search, "Search")
        }
        IconButton(onClick = { handleMenu() }) {
            Icon(Icons.Filled.MoreVert, "Menu")
        }
    }
)
```

---

### 7.2 BottomNav

**Purpose:** Bottom navigation bar

**Properties:**
- `items` (List<NavItem>) - Navigation items
- `selectedIndex` (Int) - Active item index

**Event Handlers:**
- `onItemSelect` (Int) - Called when item selected

**Child Support:** No

---

### 7.3 Drawer

**Purpose:** Side navigation drawer

**Properties:**
- `open` (Boolean) - Whether drawer is open
- `position` (Enum) - `left`, `right`
- `permanent` (Boolean) - Always visible (desktop)

**Event Handlers:**
- `onOpenChange` (Boolean) - Called when drawer opens/closes

**Child Support:** Yes (drawer content)

---

### 7.4 Breadcrumb

**Purpose:** Hierarchical navigation path

**Properties:**
- `items` (List<String>) - Path segments
- `separator` (String) - Separator character (default "/")

**Event Handlers:**
- `onItemClick` (Int) - Called when breadcrumb clicked

**Child Support:** No

---

### 7.5 Pagination

**Purpose:** Page navigation controls

**Properties:**
- `currentPage` (Int) - Active page number
- `totalPages` (Int) - Total page count
- `showFirstLast` (Boolean) - Show first/last page buttons
- `showPrevNext` (Boolean) - Show prev/next buttons

**Event Handlers:**
- `onPageChange` (Int) - Called when page changes

**Child Support:** No

---

## 8. Summary

The **Component Library** provides **48 production-ready components** across 6 categories:

- **Foundation (9)** - Core UI elements (Button, Card, Text, TextField, etc.)
- **Core (2)** - Advanced pickers (ColorPicker, IconPicker)
- **Basic (6)** - Simple components (Icon, Label, Spacer, etc.)
- **Advanced (18)** - Complex widgets (Dialog, DatePicker, Rating, etc.)
- **Layout (8)** - Container components (Stack, Grid, ScrollView, etc.)
- **Navigation (5)** - App navigation (AppBar, BottomNav, Drawer, etc.)

**Key Characteristics:**
- **Platform-native** - Generates Compose/SwiftUI/React code
- **Type-safe** - Validated properties and event handlers
- **Accessible** - WCAG compliant with proper semantics
- **Themeable** - Respects Material3/SwiftUI/MUI design systems
- **Extensible** - Easy to add custom components

**Next Chapter:** Chapter 7 dives into the Android Jetpack Compose renderer implementation.

---

**Created by Manoj Jhawar, manoj@ideahq.net**

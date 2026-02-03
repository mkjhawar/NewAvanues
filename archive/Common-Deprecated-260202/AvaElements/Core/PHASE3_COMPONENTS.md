# Phase 3: Form and Feedback Components

This document describes the 15 new components added in Phase 3.1 (Form Components) and Phase 3.2 (Feedback Components) of the AvaElements library.

## Overview

**Phase 3.1 - Form Components (8):**
- Radio, Slider, Dropdown, DatePicker, TimePicker, FileUpload, SearchBar, Rating

**Phase 3.2 - Feedback Components (7):**
- Dialog, Toast, Alert, ProgressBar, Spinner, Badge, Tooltip

All components follow the AvaElements architecture with:
- Immutable data classes
- DSL builder support
- YAML configuration support
- Cross-platform rendering
- Modifier system integration
- Type-safe builders

---

## Form Components

### 1. Radio

A radio button group component for single selection from multiple options.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `options` | `List<RadioOption>` | Required | List of radio options |
| `selectedValue` | `String?` | `null` | Currently selected value |
| `groupName` | `String` | Required | Radio group identifier |
| `orientation` | `Orientation` | `Vertical` | Layout orientation |
| `onValueChange` | `((String) -> Unit)?` | `null` | Selection change callback |

**DSL Example:**

```kotlin
Radio {
    groupName = "gender"
    options = listOf(
        RadioOption("male", "Male"),
        RadioOption("female", "Female"),
        RadioOption("other", "Other")
    )
    selectedValue = "male"
    orientation = Orientation.Horizontal
    onValueChange = { value ->
        println("Selected: $value")
    }
}
```

**YAML Example:**

```yaml
Radio:
  id: genderRadio
  groupName: gender
  selectedValue: male
  orientation: horizontal
  options:
    - value: male
      label: Male
    - value: female
      label: Female
    - value: other
      label: Other
```

**Use Cases:**
- Gender selection forms
- Preference settings
- Single-choice surveys
- Filter options

---

### 2. Slider

A range slider component for selecting numeric values within a range.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | `Float` | Required | Current slider value |
| `valueRange` | `ClosedFloatingPointRange<Float>` | `0f..1f` | Min and max values |
| `steps` | `Int` | `0` | Number of discrete steps |
| `showLabel` | `Boolean` | `true` | Show value label |
| `labelFormatter` | `((Float) -> String)?` | `null` | Custom label formatter |
| `onValueChange` | `((Float) -> Unit)?` | `null` | Value change callback |

**DSL Example:**

```kotlin
Slider {
    value = 50f
    valueRange = 0f..100f
    steps = 10
    showLabel = true
    labelFormatter = { "${it.toInt()}%" }
    onValueChange = { newValue ->
        println("Volume: $newValue")
    }
}
```

**YAML Example:**

```yaml
Slider:
  id: volumeSlider
  value: 50
  valueRange:
    min: 0
    max: 100
  steps: 10
  showLabel: true
```

**Use Cases:**
- Volume controls
- Brightness adjustment
- Price range filters
- Age range selection
- Rating scales

---

### 3. Dropdown

A dropdown select component for choosing from a list of options.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `options` | `List<DropdownOption>` | Required | List of dropdown options |
| `selectedValue` | `String?` | `null` | Currently selected value |
| `placeholder` | `String` | `"Select an option"` | Placeholder text |
| `searchable` | `Boolean` | `false` | Enable search/filter |
| `onValueChange` | `((String) -> Unit)?` | `null` | Selection change callback |

**DSL Example:**

```kotlin
Dropdown {
    placeholder = "Select a country"
    searchable = true
    options = listOf(
        DropdownOption("us", "United States", "flag-us"),
        DropdownOption("uk", "United Kingdom", "flag-uk"),
        DropdownOption("ca", "Canada", "flag-ca")
    )
    selectedValue = "us"
    onValueChange = { country ->
        println("Selected: $country")
    }
}
```

**YAML Example:**

```yaml
Dropdown:
  id: countryDropdown
  placeholder: Select a country
  searchable: true
  selectedValue: us
  options:
    - value: us
      label: United States
      icon: flag-us
    - value: uk
      label: United Kingdom
      icon: flag-uk
```

**Use Cases:**
- Country selection
- Category filters
- Language selection
- Sort order options
- User role selection

---

### 4. DatePicker

A date picker component for selecting dates from a calendar interface.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `selectedDate` | `Long?` | `null` | Selected date (timestamp) |
| `minDate` | `Long?` | `null` | Minimum selectable date |
| `maxDate` | `Long?` | `null` | Maximum selectable date |
| `dateFormat` | `String` | `"yyyy-MM-dd"` | Date format string |
| `onDateChange` | `((Long) -> Unit)?` | `null` | Date change callback |

**DSL Example:**

```kotlin
DatePicker {
    selectedDate = System.currentTimeMillis()
    minDate = startOfYear
    maxDate = endOfYear
    dateFormat = "MM/dd/yyyy"
    onDateChange = { timestamp ->
        println("Date selected: $timestamp")
    }
}
```

**YAML Example:**

```yaml
DatePicker:
  id: birthdatePicker
  selectedDate: 1609459200000
  minDate: 946684800000
  maxDate: 1735689600000
  dateFormat: "yyyy-MM-dd"
```

**Use Cases:**
- Birth date selection
- Event scheduling
- Booking dates
- Date range filters
- Deadline setting

---

### 5. TimePicker

A time picker component for selecting hours and minutes.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `hour` | `Int` | `0` | Hour value (0-23) |
| `minute` | `Int` | `0` | Minute value (0-59) |
| `is24Hour` | `Boolean` | `true` | 24-hour vs 12-hour format |
| `onTimeChange` | `((Int, Int) -> Unit)?` | `null` | Time change callback |

**DSL Example:**

```kotlin
TimePicker {
    hour = 14
    minute = 30
    is24Hour = true
    onTimeChange = { h, m ->
        println("Time: $h:${m.toString().padStart(2, '0')}")
    }
}
```

**YAML Example:**

```yaml
TimePicker:
  id: meetingTime
  hour: 14
  minute: 30
  is24Hour: true
```

**Use Cases:**
- Appointment scheduling
- Alarm setting
- Meeting time selection
- Reminder configuration
- Opening hours setting

---

### 6. FileUpload

A file upload component with drag-and-drop support.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `accept` | `List<String>` | `emptyList()` | Accepted file types |
| `multiple` | `Boolean` | `false` | Allow multiple files |
| `maxSize` | `Long?` | `null` | Max file size in bytes |
| `placeholder` | `String` | `"Choose file(s)"` | Placeholder text |
| `onFilesSelected` | `((List<FileData>) -> Unit)?` | `null` | File selection callback |

**DSL Example:**

```kotlin
FileUpload {
    accept = listOf("image/*", ".pdf", ".doc")
    multiple = true
    maxSize = 5 * 1024 * 1024 // 5MB
    placeholder = "Drop files or click to browse"
    onFilesSelected = { files ->
        files.forEach { file ->
            println("Uploading: ${file.name} (${file.size} bytes)")
        }
    }
}
```

**YAML Example:**

```yaml
FileUpload:
  id: documentUpload
  accept:
    - "image/*"
    - ".pdf"
    - ".doc"
  multiple: true
  maxSize: 5242880
  placeholder: "Drop files here"
```

**Use Cases:**
- Profile picture upload
- Document submission
- Attachment uploading
- Image galleries
- File import/export

---

### 7. SearchBar

A search input component with live search and suggestions.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | `String` | `""` | Current search value |
| `placeholder` | `String` | `"Search..."` | Placeholder text |
| `showClearButton` | `Boolean` | `true` | Show clear button |
| `suggestions` | `List<String>` | `emptyList()` | Search suggestions |
| `onValueChange` | `((String) -> Unit)?` | `null` | Value change callback |
| `onSearch` | `((String) -> Unit)?` | `null` | Search submit callback |

**DSL Example:**

```kotlin
SearchBar {
    value = ""
    placeholder = "Search products..."
    showClearButton = true
    suggestions = listOf("iPhone", "iPad", "MacBook")
    onValueChange = { query ->
        fetchSuggestions(query)
    }
    onSearch = { query ->
        performSearch(query)
    }
}
```

**YAML Example:**

```yaml
SearchBar:
  id: productSearch
  placeholder: "Search products..."
  showClearButton: true
  suggestions:
    - iPhone
    - iPad
    - MacBook
```

**Use Cases:**
- Product search
- Contact search
- Location search
- Command palette
- Filter input

---

### 8. Rating

A star rating component for displaying and collecting ratings.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | `Float` | `0f` | Current rating value |
| `maxRating` | `Int` | `5` | Maximum rating |
| `allowHalf` | `Boolean` | `false` | Allow half-star ratings |
| `readonly` | `Boolean` | `false` | Read-only display mode |
| `icon` | `String` | `"star"` | Icon name |
| `onRatingChange` | `((Float) -> Unit)?` | `null` | Rating change callback |

**DSL Example:**

```kotlin
Rating {
    value = 4.5f
    maxRating = 5
    allowHalf = true
    readonly = false
    icon = "star"
    onRatingChange = { rating ->
        submitRating(rating)
    }
}
```

**YAML Example:**

```yaml
Rating:
  id: productRating
  value: 4.5
  maxRating: 5
  allowHalf: true
  readonly: false
  icon: star
```

**Use Cases:**
- Product ratings
- Service reviews
- Content ratings
- Feedback collection
- Quality assessment

---

## Feedback Components

### 1. Dialog

A modal dialog component for displaying content and actions.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `isOpen` | `Boolean` | `false` | Dialog visibility |
| `title` | `String?` | `null` | Dialog title |
| `content` | `Component?` | `null` | Dialog content |
| `actions` | `List<DialogAction>` | `emptyList()` | Action buttons |
| `dismissible` | `Boolean` | `true` | Allow backdrop dismiss |
| `onDismiss` | `(() -> Unit)?` | `null` | Dismiss callback |

**DSL Example:**

```kotlin
Dialog {
    isOpen = true
    title = "Confirm Delete"
    content = Text("Are you sure you want to delete this item?")
    dismissible = true
    action("Cancel", DialogActionStyle.Text) {
        closeDialog()
    }
    action("Delete", DialogActionStyle.Primary) {
        deleteItem()
    }
    onDismiss = { closeDialog() }
}
```

**YAML Example:**

```yaml
Dialog:
  id: confirmDialog
  isOpen: true
  title: "Confirm Delete"
  dismissible: true
  actions:
    - label: Cancel
      style: Text
    - label: Delete
      style: Primary
```

**Use Cases:**
- Confirmation dialogs
- Form modals
- Info popups
- Alert messages
- Content preview

---

### 2. Toast

A temporary notification message component.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `message` | `String` | Required | Toast message |
| `duration` | `Long` | `3000` | Display duration (ms) |
| `severity` | `ToastSeverity` | `Info` | Severity level |
| `position` | `ToastPosition` | `BottomCenter` | Screen position |
| `action` | `ToastAction?` | `null` | Optional action button |

**DSL Example:**

```kotlin
Toast(message = "Item saved successfully") {
    duration = 3000
    severity = ToastSeverity.Success
    position = ToastPosition.BottomCenter
    action("Undo") {
        undoSave()
    }
}
```

**YAML Example:**

```yaml
Toast:
  id: successToast
  message: "Item saved successfully"
  duration: 3000
  severity: Success
  position: BottomCenter
  action:
    label: Undo
```

**Use Cases:**
- Success messages
- Error notifications
- Info updates
- Undo actions
- Status changes

---

### 3. Alert

An alert banner component for displaying important messages.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `title` | `String` | Required | Alert title |
| `message` | `String` | Required | Alert message |
| `severity` | `AlertSeverity` | `Info` | Severity level |
| `dismissible` | `Boolean` | `true` | Show close button |
| `icon` | `String?` | `null` | Optional icon |
| `onDismiss` | `(() -> Unit)?` | `null` | Dismiss callback |

**DSL Example:**

```kotlin
Alert("Warning", "Your session will expire soon") {
    severity = AlertSeverity.Warning
    dismissible = true
    icon = "warning"
    onDismiss = {
        hideAlert()
    }
}
```

**YAML Example:**

```yaml
Alert:
  id: sessionAlert
  title: Warning
  message: "Your session will expire soon"
  severity: Warning
  dismissible: true
  icon: warning
```

**Use Cases:**
- System alerts
- Warning messages
- Error banners
- Info announcements
- Success confirmations

---

### 4. ProgressBar

A linear progress indicator component.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `value` | `Float` | `0f` | Progress value (0-1) |
| `showLabel` | `Boolean` | `false` | Show progress label |
| `labelFormatter` | `((Float) -> String)?` | `null` | Custom label formatter |
| `indeterminate` | `Boolean` | `false` | Indeterminate mode |

**DSL Example:**

```kotlin
ProgressBar {
    value = 0.75f
    showLabel = true
    labelFormatter = { "${(it * 100).toInt()}%" }
    indeterminate = false
}
```

**YAML Example:**

```yaml
ProgressBar:
  id: uploadProgress
  value: 0.75
  showLabel: true
  indeterminate: false
```

**Use Cases:**
- File uploads
- Download progress
- Task completion
- Loading states
- Multi-step forms

---

### 5. Spinner

A circular loading indicator component.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `size` | `SpinnerSize` | `Medium` | Spinner size |
| `label` | `String?` | `null` | Optional label |

**DSL Example:**

```kotlin
Spinner {
    size = SpinnerSize.Medium
    label = "Loading..."
}
```

**YAML Example:**

```yaml
Spinner:
  id: loadingSpinner
  size: Medium
  label: "Loading..."
```

**Use Cases:**
- Page loading
- Data fetching
- Processing states
- Async operations
- Infinite scroll

---

### 6. Badge

A small status badge or chip component.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `content` | `String` | `""` | Badge content |
| `variant` | `BadgeVariant` | `Default` | Color variant |
| `size` | `BadgeSize` | `Medium` | Badge size |

**DSL Example:**

```kotlin
Badge("5") {
    variant = BadgeVariant.Error
    size = BadgeSize.Small
}

Badge("New") {
    variant = BadgeVariant.Primary
    size = BadgeSize.Medium
}
```

**YAML Example:**

```yaml
Badge:
  id: notificationBadge
  content: "5"
  variant: Error
  size: Small
```

**Use Cases:**
- Notification counts
- Status indicators
- New item markers
- Category tags
- Achievement badges

---

### 7. Tooltip

A hover tooltip component for displaying contextual information.

**Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `content` | `String` | Required | Tooltip content |
| `position` | `TooltipPosition` | `Top` | Position relative to child |
| `child` | `Component` | Required | Child component |

**DSL Example:**

```kotlin
Tooltip("Click to save your work") {
    position = TooltipPosition.Top
    child = Button("Save") {
        onClick = { saveDocument() }
    }
}
```

**YAML Example:**

```yaml
Tooltip:
  id: saveTooltip
  content: "Click to save your work"
  position: Top
  child:
    Button:
      text: Save
```

**Use Cases:**
- Button hints
- Icon descriptions
- Help text
- Feature explanations
- Keyboard shortcuts

---

## Complete Example

Here's a complete example using multiple form and feedback components:

```kotlin
val ui = AvaUI {
    theme = Themes.iOS26LiquidGlass

    Column {
        padding(16f)

        // Form Section
        Text("User Profile") {
            font = Font.Title
        }

        Radio {
            groupName = "gender"
            options = listOf(
                RadioOption("male", "Male"),
                RadioOption("female", "Female")
            )
            orientation = Orientation.Horizontal
        }

        Slider {
            value = 25f
            valueRange = 18f..100f
            showLabel = true
            labelFormatter = { "Age: ${it.toInt()}" }
        }

        Dropdown {
            placeholder = "Select country"
            searchable = true
            options = listOf(
                DropdownOption("us", "United States"),
                DropdownOption("uk", "United Kingdom")
            )
        }

        Rating {
            value = 4.5f
            maxRating = 5
            allowHalf = true
        }

        // Feedback Section
        Alert("Success", "Profile updated successfully") {
            severity = AlertSeverity.Success
            dismissible = true
        }

        ProgressBar {
            value = 0.8f
            showLabel = true
        }

        Row {
            Button("Save") {
                onClick = { /* Save */ }
            }

            Spinner {
                size = SpinnerSize.Small
            }

            Badge("New") {
                variant = BadgeVariant.Primary
            }
        }
    }
}
```

---

## Integration Notes

### Import Statements

```kotlin
import com.augmentalis.avaelements.components.form.*
import com.augmentalis.avaelements.components.feedback.*
import com.augmentalis.avaelements.dsl.FormAndFeedbackBuilders
```

### Platform Support

All components are designed to work across:
- Android (Material Components)
- iOS (UIKit/SwiftUI)
- macOS (AppKit/SwiftUI)
- Windows (WinUI 3)
- Web (HTML/CSS/JS)
- visionOS (SwiftUI)

### State Management

Components can be integrated with the AvaElements state management system:

```kotlin
val formState = remember { mutableStateOf(FormData()) }

Slider {
    value = formState.value.age
    onValueChange = { newAge ->
        formState.value = formState.value.copy(age = newAge)
    }
}
```

### Validation

Form components support validation through the state management system:

```kotlin
TextField {
    value = email
    isError = !isValidEmail(email)
    errorMessage = "Invalid email format"
}
```

---

## Best Practices

1. **Form Components:**
   - Always provide meaningful labels
   - Use appropriate input types for data
   - Implement validation feedback
   - Support keyboard navigation
   - Handle errors gracefully

2. **Feedback Components:**
   - Use appropriate severity levels
   - Keep messages concise and clear
   - Provide actionable feedback
   - Don't overuse dialogs
   - Allow users to dismiss notifications

3. **Accessibility:**
   - All components support screen readers
   - Keyboard navigation is enabled
   - Focus management is handled
   - ARIA labels are provided (web)
   - VoiceOver/TalkBack compatible

4. **Performance:**
   - Use indeterminate progress for unknown durations
   - Debounce search inputs
   - Lazy load dropdown options
   - Optimize file upload handling
   - Cache frequently used data

---

## Migration Guide

If migrating from existing form/feedback implementations:

1. Replace native platform components with AvaElements components
2. Update event handlers to use lambda callbacks
3. Migrate styling to the modifier system
4. Update state management to use AvaElements state
5. Test on all target platforms

---

## Future Enhancements

Planned for Phase 4:
- Multi-select dropdown
- Date range picker
- Color picker
- Rich text editor
- Code editor
- Chart components
- Table with sorting/filtering
- Tree view with drag-drop

---

## Support

For issues, questions, or contributions:
- Documentation: `/docs`
- Examples: `/examples/FormAndFeedbackExample.kt`
- Issues: GitHub Issues
- Community: Discord/Slack

---

**Version:** Phase 3.1 & 3.2
**Last Updated:** 2025-10-30
**Status:** ✅ Complete

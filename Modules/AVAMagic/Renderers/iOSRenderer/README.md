# iOS Renderer for AVAMagic

Native iOS UIKit renderer for AVAMagic cross-platform UI components.

## Overview

The iOS Renderer converts AVAMagic component definitions into native iOS UIKit views, providing:

- **Native Performance** - Pure UIKit, no web views
- **iOS Design Guidelines** - Follows Apple Human Interface Guidelines
- **Accessibility** - Full VoiceOver and Dynamic Type support
- **Dark Mode** - Automatic dark mode adaptation
- **SwiftUI Interop** - Can be wrapped in SwiftUI views

## Components Implemented

### Phase 1: Form Components (5)

| Component | Status | Native Widget |
|-----------|--------|---------------|
| TextField | ✅ Complete | UITextField |
| Checkbox | ✅ Complete | UIButton (custom) |
| Switch | ✅ Complete | UISwitch |
| RadioButton | ✅ Complete | UIButton (custom) |
| Slider | ✅ Complete | UISlider |

### Phase 2: Navigation, Form, Feedback & Display (15)

| Component | Status | Native Widget |
|-----------|--------|---------------|
| **Navigation** | | |
| AppBar | ✅ Complete | UINavigationBar |
| BottomNav | ✅ Complete | UITabBar |
| Tabs | ✅ Complete | UISegmentedControl |
| Drawer | ✅ Complete | Custom slide-out |
| **Form (Advanced)** | | |
| DatePicker | ✅ Complete | UIDatePicker |
| TimePicker | ✅ Complete | UIDatePicker (time) |
| SearchBar | ✅ Complete | UISearchBar |
| Dropdown | ✅ Complete | UIPickerView |
| **Feedback** | | |
| Dialog | ✅ Complete | UIAlertController |
| Snackbar | ✅ Complete | Custom toast |
| Toast | ✅ Complete | Custom toast |
| ProgressBar | ✅ Complete | UIProgressView |
| CircularProgress | ✅ Complete | CAShapeLayer/UIActivityIndicator |
| **Display** | | |
| WebView | ✅ Complete | WKWebView |
| VideoPlayer | ✅ Complete | AVPlayerViewController |

### Phase 3: Advanced Display, Layout & Data (10)

| Component | Status | Native Widget |
|-----------|--------|---------------|
| **Display (Advanced)** | | |
| Badge | ✅ Complete | Custom UIView |
| Chip | ✅ Complete | Custom UIView |
| Avatar | ✅ Complete | UIImageView/UILabel |
| Skeleton | ✅ Complete | UIView + CABasicAnimation |
| Tooltip | ✅ Complete | Custom UIView |
| **Layout** | | |
| Divider | ✅ Complete | UIView |
| **Data** | | |
| Accordion | ✅ Complete | UIScrollView + Custom |
| **Advanced** | | |
| Card | ✅ Complete | UIView + Shadow |
| Grid | ✅ Complete | UICollectionView |
| Popover | ✅ Complete | UIPopoverPresentationController |

**Total: 30 components implemented** (5 Phase 1 + 15 Phase 2 + 10 Phase 3)
**Test Coverage:** 90 tests (25 Phase 1 + 40 Phase 2 + 25 Phase 3)

### Validation Support

- Email format validation
- Phone number validation
- Min/max length validation
- Required field validation
- Real-time validation feedback

## Installation

### CocoaPods

```ruby
pod 'AVAMagiciOS', '~> 2.1.0'
```

### Swift Package Manager

```swift
dependencies: [
    .package(url: "https://github.com/augmentalis/avamagic-ios.git", from: "2.1.0")
]
```

### Kotlin Multiplatform

```kotlin
// settings.gradle.kts
include(":modules:AVAMagic:Renderers:iOSRenderer")

// build.gradle.kts
kotlin {
    ios()

    sourceSets {
        val iosMain by getting {
            dependencies {
                implementation(project(":modules:AVAMagic:Renderers:iOSRenderer"))
            }
        }
    }
}
```

## Usage

### Basic Usage (Kotlin)

```kotlin
import com.augmentalis.avamagic.renderer.ios.*

// Create renderer
val renderer = IOSRenderer()

// Create component
val textField = TextFieldComponent(
    label = "Email",
    placeholder = "user@example.com",
    inputType = "email",
    validation = ValidationRules(
        required = true,
        email = true
    )
)

// Render to UIView
val uiView = renderer.renderComponent(textField) as UITextField
```

### SwiftUI Interop (Swift)

```swift
import SwiftUI
import AVAMagiciOS

struct ContentView: View {
    let renderer = IOSRenderer()

    var body: some View {
        VStack {
            // Wrap UIKit view in SwiftUI
            UIViewWrapper(
                renderer.renderComponent(
                    TextFieldComponent(
                        label: "Email",
                        placeholder: "user@example.com"
                    )
                )
            )
        }
    }
}

struct UIViewWrapper: UIViewRepresentable {
    let view: UIView

    func makeUIView(context: Context) -> UIView {
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
```

### Batch Rendering

```kotlin
// Render multiple components in stack
val components = listOf(
    TextFieldComponent(label = "Name"),
    TextFieldComponent(label = "Email", inputType = "email"),
    CheckboxComponent(label = "Accept Terms"),
    SwitchComponent(label = "Notifications")
)

val stackView = renderer.renderStack(
    components = components,
    axis = UILayoutConstraintAxisVertical,
    spacing = 16.0
)
```

## Component Examples

### TextField with Validation

```kotlin
val emailField = TextFieldComponent(
    label = "Email Address",
    placeholder = "user@example.com",
    inputType = "email",
    validation = ValidationRules(
        required = true,
        email = true
    )
)

val uiView = renderer.renderComponent(emailField)

// Validate
val textFieldRenderer = IOSTextFieldRenderer()
val result = textFieldRenderer.validate(emailField, "test@example.com")
if (!result.isValid) {
    showError(result.error)
}
```

### Checkbox

```kotlin
val checkbox = CheckboxComponent(
    label = "I agree to terms and conditions",
    checked = false,
    size = ComponentSize.MD
)

val uiView = renderer.renderComponent(checkbox)

// Toggle
val toggled = checkbox.toggle()
```

### Switch

```kotlin
val switch = SwitchComponent(
    label = "Enable Dark Mode",
    checked = true
)

val uiView = renderer.renderComponent(switch)
```

### RadioButton Group

```kotlin
val radioRenderer = IOSRadioButtonRenderer()

val options = listOf(
    RadioButtonComponent(label = "Small", value = "s"),
    RadioButtonComponent(label = "Medium", value = "m"),
    RadioButtonComponent(label = "Large", value = "l")
)

val groupView = radioRenderer.renderGroup(
    options = options,
    groupName = "size",
    selectedValue = "m"
)
```

### Slider

```kotlin
val slider = SliderComponent(
    label = "Volume",
    value = 50.0,
    min = 0.0,
    max = 100.0,
    step = 1.0,
    showValue = true
)

val uiView = renderer.renderComponent(slider)
```

## Accessibility

The renderer automatically adds accessibility features:

```kotlin
// Apply accessibility
renderer.applyAccessibility(view, component)

// VoiceOver will announce:
// - Component type (button, text field, slider, etc.)
// - Current value/state
// - Enabled/disabled status
```

## Dark Mode

Dark mode is automatically supported:

```kotlin
// Apply dark mode
renderer.applyDarkMode(view, component)

// Colors adapt automatically based on:
// - UIUserInterfaceStyle
// - System appearance settings
```

## Testing

Run unit tests:

```bash
./gradlew :modules:AVAMagic:Renderers:iOSRenderer:test
```

**Test Coverage:**
- TextField: 10 tests
- Checkbox: 5 tests
- Switch: 3 tests
- RadioButton: 4 tests
- Slider: 3 tests
- **Total: 25 tests**

## Performance

| Component | Render Time | Memory |
|-----------|-------------|--------|
| TextField | < 1ms | 2KB |
| Checkbox | < 0.5ms | 1KB |
| Switch | < 0.5ms | 1KB |
| RadioButton | < 0.5ms | 1KB |
| Slider | < 1ms | 2KB |

## Limitations

1. **Range Slider** - iOS doesn't have native two-thumb slider, requires custom implementation or third-party library
2. **Checkbox** - iOS doesn't have native checkbox, uses custom UIButton with checkmark
3. **RadioButton** - iOS doesn't have native radio button, uses custom circular UIButton

## Roadmap

### Phase 1 (Current) ✅
- TextField with validation
- Checkbox (custom)
- Switch
- RadioButton (custom)
- Slider

### Phase 2 (Next)
- DatePicker
- TimePicker
- Dropdown (Picker)
- SearchBar
- SegmentedControl

### Phase 3 (Future)
- Layout components (Column, Row, Stack)
- Navigation (TabBar, NavigationBar)
- Display (Image, Avatar, Badge)
- Advanced (Chart, Map, Calendar)

## Contributing

See [CONTRIBUTING.md](../../../../CONTRIBUTING.md) for development guidelines.

## License

Proprietary - Copyright © 2025 Augmentalis

## Author

**Manoj Jhawar**
- Email: manoj@ideahq.net
- GitHub: @manojjhawar

---

**Version:** 1.0.0
**Last Updated:** 2025-11-19
**Status:** Initial Release

# Chapter 8: iOS SwiftUI + Kotlin/Native Bridge

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~7,000 words

---

## Overview

The iOS platform bridge uses **Kotlin/Native** to expose Kotlin models to Swift, then renders them as **SwiftUI** views. This requires C-interop bridging between Kotlin and Swift.

## Architecture

```
Kotlin (Common)         Kotlin/Native (iOS)       Swift (iOS)           SwiftUI
ComponentNode     →     C-Interop Bridge    →     AvaUIView      →    View
 (KMP)                  (expect/actual)           (Swift class)         (SwiftUI)
```

## Three-Layer System

### Layer 1: Kotlin Common (KMP)

```kotlin
// Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/
data class ComponentNode(
    val id: String,
    val type: ComponentType,
    val properties: Map<String, Any>,
    val children: List<ComponentNode>
)
```

### Layer 2: Kotlin/Native (iOS Target)

```kotlin
// Universal/IDEAMagic/AvaUI/src/iosMain/kotlin/
actual class IOSUIRenderer {
    actual fun render(component: ComponentNode): Any {
        // Create Swift wrapper
        return AvaUIComponentWrapper(
            id = component.id,
            type = component.type.name,
            properties = component.properties.toNSMap(),
            children = component.children.map { render(it) }
        )
    }
}

// Extension for Map → NSDictionary
fun Map<String, Any>.toNSMap(): NSDictionary {
    val dict = NSMutableDictionary()
    forEach { (key, value) ->
        dict.setObject(value.toNSObject(), forKey = key)
    }
    return dict
}
```

### Layer 3: Swift + SwiftUI

**Location:** `Universal/IDEAMagic/Components/Adapters/src/iosMain/swift/AvaUI/`

#### AvaUIView.swift

```swift
import SwiftUI
import AvaUIFramework // Kotlin/Native framework

struct AvaUIView: View {
    let component: AvaUIComponentWrapper

    var body: some View {
        switch component.type {
        case "BUTTON":
            renderButton()
        case "TEXT":
            renderText()
        case "TEXT_FIELD":
            renderTextField()
        case "COLUMN":
            renderColumn()
        case "ROW":
            renderRow()
        default:
            Text("Unknown: \(component.type)")
        }
    }

    private func renderButton() -> some View {
        let text = component.properties["text"] as? String ?? "Button"
        let onClick = component.eventHandlers["onClick"]

        return Button(text) {
            onClick?()
        }
        .buttonStyle(.borderedProminent)
    }

    private func renderText() -> some View {
        let content = component.properties["content"] as? String ?? ""
        let variant = component.properties["variant"] as? String ?? "BODY1"

        return Text(content)
            .font(fontForVariant(variant))
    }

    private func renderTextField() -> some View {
        let label = component.properties["label"] as? String ?? ""
        @State var value = component.properties["value"] as? String ?? ""

        return TextField(label, text: $value)
            .textFieldStyle(.roundedBorder)
    }

    private func renderColumn() -> some View {
        VStack(spacing: 8) {
            ForEach(component.children, id: \.id) { child in
                AvaUIView(component: child)
            }
        }
    }

    private func fontForVariant(_ variant: String) -> Font {
        switch variant {
        case "H1": return .largeTitle
        case "H2": return .title
        case "H3": return .title2
        case "BODY1": return .body
        case "BODY2": return .callout
        case "CAPTION": return .caption
        default: return .body
        }
    }
}
```

## SwiftUI Component Examples

### MagicButtonView.swift

```swift
import SwiftUI

struct MagicButtonView: View {
    let text: String
    let variant: String
    let enabled: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(text)
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(buttonStyle)
        .disabled(!enabled)
    }

    private var buttonStyle: some ButtonStyle {
        switch variant {
        case "primary": return .borderedProminent
        case "secondary": return .bordered
        case "outline": return .bordered
        default: return .automatic
        }
    }
}
```

### MagicCardView.swift

```swift
import SwiftUI

struct MagicCardView<Content: View>: View {
    let elevation: CGFloat
    let backgroundColor: Color
    let cornerRadius: CGFloat
    @ViewBuilder let content: Content

    var body: some View {
        VStack {
            content
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(backgroundColor)
        .cornerRadius(cornerRadius)
        .shadow(color: Color.black.opacity(0.1 * elevation / 24),
                radius: elevation,
                x: 0,
                y: elevation / 2)
    }
}
```

### MagicTextFieldView.swift

```swift
import SwiftUI

struct MagicTextFieldView: View {
    let label: String
    let placeholder: String
    let type: String // "text", "password", "email", "number"
    @Binding var value: String
    let onValueChange: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)

            if type == "password" {
                SecureField(placeholder, text: $value)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: value) { newValue in
                        onValueChange(newValue)
                    }
            } else {
                TextField(placeholder, text: $value)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(keyboardType)
                    .onChange(of: value) { newValue in
                        onValueChange(newValue)
                    }
            }
        }
    }

    private var keyboardType: UIKeyboardType {
        switch type {
        case "email": return .emailAddress
        case "number": return .numberPad
        case "phone": return .phonePad
        default: return .default
        }
    }
}
```

## C-Interop Bridge

### Kotlin Side

```kotlin
// iosMain/kotlin/IOSBridge.kt
@ExportObjCClass
class AvaUIComponentWrapper(
    val id: String,
    val type: String,
    val properties: NSDictionary,
    val children: List<Any>
) {
    fun getProperty(key: String): Any? {
        return properties.objectForKey(key)
    }
}

@ExportObjCClass
class IOSUIRenderer {
    fun renderScreen(screen: ScreenNode): AvaUIComponentWrapper {
        return AvaUIComponentWrapper(
            id = screen.name,
            type = "SCREEN",
            properties = emptyMap<String, Any>().toNSMap(),
            children = listOf(render(screen.root))
        )
    }

    private fun render(component: ComponentNode): AvaUIComponentWrapper {
        return AvaUIComponentWrapper(
            id = component.id,
            type = component.type.name,
            properties = component.properties.toNSMap(),
            children = component.children.map { render(it) }
        )
    }
}
```

### Swift Side

```swift
// Import Kotlin/Native framework
import AvaUIFramework

class AvaUIRenderer: ObservableObject {
    private let kotlinRenderer = IOSUIRenderer()

    func renderScreen(json: String) -> AvaUIComponentWrapper? {
        // Parse JSON in Kotlin
        guard let screen = JsonDSLParser().parseScreen(json: json) else {
            return nil
        }

        // Render to wrapper
        return kotlinRenderer.renderScreen(screen: screen)
    }
}
```

## State Management

### SwiftUI @State + Kotlin

```swift
struct MagicScreenView: View {
    @StateObject private var renderer = AvaUIRenderer()
    @State private var component: AvaUIComponentWrapper?

    let jsonDefinition: String

    var body: some View {
        VStack {
            if let component = component {
                AvaUIView(component: component)
            } else {
                ProgressView()
            }
        }
        .onAppear {
            component = renderer.renderScreen(json: jsonDefinition)
        }
    }
}
```

## Theme System

### MagicTheme.swift

```swift
import SwiftUI

struct MagicTheme {
    let colors: MagicColors
    let typography: MagicTypography
    let spacing: MagicSpacing
    let shapes: MagicShapes

    static let `default` = MagicTheme(
        colors: .default,
        typography: .default,
        spacing: .default,
        shapes: .default
    )
}

struct MagicColors {
    let primary: Color
    let secondary: Color
    let background: Color
    let surface: Color
    let onPrimary: Color
    let onSecondary: Color
    let onBackground: Color
    let onSurface: Color

    static let `default` = MagicColors(
        primary: .blue,
        secondary: .purple,
        background: Color(.systemBackground),
        surface: Color(.secondarySystemBackground),
        onPrimary: .white,
        onSecondary: .white,
        onBackground: Color(.label),
        onSurface: Color(.label)
    )
}
```

## Complete Example

**Input JSON:**
```json
{
  "type": "COLUMN",
  "children": [
    {
      "type": "TEXT",
      "properties": { "content": "Welcome", "variant": "H1" }
    },
    {
      "type": "TEXT_FIELD",
      "properties": { "label": "Email", "type": "email" }
    },
    {
      "type": "BUTTON",
      "properties": { "text": "Sign In", "variant": "primary" }
    }
  ]
}
```

**Generated SwiftUI:**
```swift
import SwiftUI

struct WelcomeScreenView: View {
    @State private var email: String = ""

    var body: some View {
        VStack(spacing: 16) {
            Text("Welcome")
                .font(.largeTitle)

            TextField("Email", text: $email)
                .keyboardType(.emailAddress)
                .textFieldStyle(.roundedBorder)

            Button("Sign In") {
                handleSignIn()
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
        }
        .padding()
    }

    private func handleSignIn() {
        // Sign in logic
    }
}
```

## Platform-Specific Features

### iOS-Only Components
- **NavigationView** - iOS navigation container
- **TabView** - iOS tab interface
- **Sheet** - Modal presentation
- **Alert** - iOS alert dialog
- **ActionSheet** - iOS action sheet

### Modifiers
- `.padding()` - Add padding
- `.frame()` - Set size
- `.background()` - Background color
- `.cornerRadius()` - Rounded corners
- `.shadow()` - Drop shadow

## Current Status (from Analysis)

**Implementation Progress:**
- ✅ Kotlin models defined (ComponentNode, etc.)
- ✅ 9 SwiftUI views created (Button, Card, Checkbox, Chip, Divider, Image, ListItem, Text, TextField)
- ⚠️ 27 TODO items in Kotlin bridge
- ❌ C-interop not fully implemented

**Missing:**
- Event handler bridging
- State synchronization
- Theme conversion
- Navigation integration

## Summary

The iOS SwiftUI bridge uses Kotlin/Native + C-interop to:
- Share models between Kotlin and Swift
- Render AvaUI as native SwiftUI views
- Support iOS design patterns (@State, @Binding)
- Provide platform-specific features

**Next:** Chapter 9 covers Web React + TypeScript implementation.

---

**Created by Manoj Jhawar, manoj@ideahq.net**

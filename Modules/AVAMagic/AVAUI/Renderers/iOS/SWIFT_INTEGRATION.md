# Swift Integration Layer for AvaElements iOS

This document describes the Swift side of the AvaElements iOS renderer. The Swift code consumes the Kotlin/Native bridge models and renders them as native SwiftUI views.

## Architecture Overview

```
AvaElements (Kotlin) → SwiftUIView (Bridge) → AvaElementsView (Swift) → Native SwiftUI
```

## Swift Implementation

### 1. AvaElementsView.swift - Main View Renderer

This is the main SwiftUI view that consumes `SwiftUIView` bridge models from Kotlin.

```swift
import SwiftUI
import AvaElementsiOS  // Kotlin/Native framework

/// Main view that renders AvaElements components as native SwiftUI
struct AvaElementsView: View {
    let component: SwiftUIView

    var body: some View {
        renderComponent(component)
    }

    @ViewBuilder
    private func renderComponent(_ component: SwiftUIView) -> some View {
        switch component.type {
        // Layout containers
        case .VStack:
            renderVStack(component)
        case .HStack:
            renderHStack(component)
        case .ZStack:
            renderZStack(component)
        case .ScrollView:
            renderScrollView(component)

        // Content views
        case .Text:
            renderText(component)
        case .Button:
            renderButton(component)
        case .TextField:
            renderTextField(component)
        case .Toggle:
            renderToggle(component)
        case .Image:
            renderImage(component)

        // Shapes
        case .RoundedRectangle:
            renderRoundedRectangle(component)
        case .Circle:
            Circle()
        case .Rectangle:
            Rectangle()
        case .Capsule:
            Capsule()

        // Special views
        case .Spacer:
            Spacer()
        case .Divider:
            Divider()
        case .EmptyView:
            EmptyView()

        default:
            EmptyView()
        }
    }

    // MARK: - Layout Renderers

    @ViewBuilder
    private func renderVStack(_ component: SwiftUIView) -> some View {
        let spacing = component.properties["spacing"] as? CGFloat
        let alignment = parseHorizontalAlignment(component.properties["alignment"] as? String)

        VStack(alignment: alignment, spacing: spacing) {
            ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                renderComponent(child)
            }
        }
        .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderHStack(_ component: SwiftUIView) -> some View {
        let spacing = component.properties["spacing"] as? CGFloat
        let alignment = parseVerticalAlignment(component.properties["alignment"] as? String)

        HStack(alignment: alignment, spacing: spacing) {
            ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                renderComponent(child)
            }
        }
        .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderZStack(_ component: SwiftUIView) -> some View {
        let alignment = parseAlignment(component.properties["alignment"] as? String)

        ZStack(alignment: alignment) {
            ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                renderComponent(child)
            }
        }
        .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderScrollView(_ component: SwiftUIView) -> some View {
        let axis = component.properties["axis"] as? String ?? "vertical"
        let showsIndicators = component.properties["showsIndicators"] as? Bool ?? true

        if axis == "horizontal" {
            ScrollView(.horizontal, showsIndicators: showsIndicators) {
                ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                    renderComponent(child)
                }
            }
            .applyModifiers(component.modifiers)
        } else {
            ScrollView(.vertical, showsIndicators: showsIndicators) {
                ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                    renderComponent(child)
                }
            }
            .applyModifiers(component.modifiers)
        }
    }

    // MARK: - Content Renderers

    @ViewBuilder
    private func renderText(_ component: SwiftUIView) -> some View {
        if let content = component.properties["content"] as? String {
            Text(content)
                .applyModifiers(component.modifiers)
        }
    }

    @ViewBuilder
    private func renderButton(_ component: SwiftUIView) -> some View {
        let label = component.properties["label"] as? String ?? "Button"

        Button(action: {
            // Call Kotlin callback if present
            handleButtonAction(component)
        }) {
            if !component.children.isEmpty {
                // Custom label with children (e.g., with icons)
                ForEach(Array(component.children.enumerated()), id: \.offset) { _, child in
                    renderComponent(child)
                }
            } else {
                Text(label)
            }
        }
        .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderTextField(_ component: SwiftUIView) -> some View {
        let placeholder = component.properties["placeholder"] as? String ?? ""
        @State var text = component.properties["text"] as? String ?? ""

        TextField(placeholder, text: $text)
            .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderToggle(_ component: SwiftUIView) -> some View {
        let label = component.properties["label"] as? String ?? ""
        @State var isOn = component.properties["isOn"] as? Bool ?? false
        let style = component.properties["style"] as? String

        Toggle(label, isOn: $isOn)
            .toggleStyle(style == "checkbox" ? .checkbox : .switch)
            .applyModifiers(component.modifiers)
    }

    @ViewBuilder
    private func renderImage(_ component: SwiftUIView) -> some View {
        if let systemName = component.properties["systemName"] as? String {
            // SF Symbol
            Image(systemName: systemName)
                .applyModifiers(component.modifiers)
        } else if let url = component.properties["url"] as? String {
            // Network image
            AsyncImage(url: URL(string: url)) { phase in
                switch phase {
                case .success(let image):
                    image.resizable()
                case .failure:
                    Image(systemName: "exclamationmark.triangle")
                case .empty:
                    ProgressView()
                @unknown default:
                    EmptyView()
                }
            }
            .applyModifiers(component.modifiers)
        }
    }

    @ViewBuilder
    private func renderRoundedRectangle(_ component: SwiftUIView) -> some View {
        let cornerRadius = component.properties["cornerRadius"] as? CGFloat ?? 0
        RoundedRectangle(cornerRadius: cornerRadius)
            .applyModifiers(component.modifiers)
    }

    // MARK: - Helper Functions

    private func handleButtonAction(_ component: SwiftUIView) {
        // This would call back to Kotlin callback
        // Implementation depends on callback mechanism setup
        print("Button tapped: \(component.id ?? "unknown")")
    }

    private func parseHorizontalAlignment(_ value: String?) -> HorizontalAlignment {
        switch value {
        case "Leading": return .leading
        case "Trailing": return .trailing
        default: return .center
        }
    }

    private func parseVerticalAlignment(_ value: String?) -> VerticalAlignment {
        switch value {
        case "Top": return .top
        case "Bottom": return .bottom
        case "FirstTextBaseline": return .firstTextBaseline
        case "LastTextBaseline": return .lastTextBaseline
        default: return .center
        }
    }

    private func parseAlignment(_ value: String?) -> Alignment {
        switch value {
        case "TopLeading": return .topLeading
        case "Top": return .top
        case "TopTrailing": return .topTrailing
        case "Leading": return .leading
        case "Trailing": return .trailing
        case "BottomLeading": return .bottomLeading
        case "Bottom": return .bottom
        case "BottomTrailing": return .bottomTrailing
        default: return .center
        }
    }
}
```

### 2. ModifierExtensions.swift - Modifier Application

Extension to apply SwiftUI modifiers from the bridge models:

```swift
import SwiftUI
import AvaElementsiOS

extension View {
    @ViewBuilder
    func applyModifiers(_ modifiers: [SwiftUIModifier]) -> some View {
        var view = AnyView(self)

        for modifier in modifiers {
            view = AnyView(applyModifier(view, modifier))
        }

        return view
    }

    @ViewBuilder
    private func applyModifier(_ view: AnyView, _ modifier: SwiftUIModifier) -> some View {
        switch modifier.type {
        // Layout modifiers
        case .Padding:
            if let value = modifier.value as? CGFloat {
                view.padding(value)
            }

        case .PaddingEdges:
            if let edges = modifier.value as? EdgeInsets {
                view.padding(EdgeInsets(
                    top: edges.top,
                    leading: edges.leading,
                    bottom: edges.bottom,
                    trailing: edges.trailing
                ))
            }

        case .Frame:
            if let frame = modifier.value as? FrameValue {
                view.frame(
                    width: parseSize(frame.width),
                    height: parseSize(frame.height),
                    alignment: parseAlignment(frame.alignment)
                )
            }

        // Appearance modifiers
        case .Background:
            if let color = modifier.value as? SwiftUIColor {
                view.background(parseColor(color))
            }

        case .ForegroundColor:
            if let color = modifier.value as? SwiftUIColor {
                view.foregroundColor(parseColor(color))
            }

        case .CornerRadius:
            if let radius = modifier.value as? CGFloat {
                view.cornerRadius(radius)
            }

        case .Shadow:
            if let shadow = modifier.value as? ShadowValue {
                view.shadow(
                    color: parseColor(shadow.color),
                    radius: shadow.radius,
                    x: shadow.x,
                    y: shadow.y
                )
            }

        case .Opacity:
            if let opacity = modifier.value as? Double {
                view.opacity(opacity)
            }

        // Typography modifiers
        case .Font:
            if let style = modifier.value as? FontStyle {
                view.font(parseFont(style))
            }

        case .FontWeight:
            if let weight = modifier.value as? FontWeight {
                view.fontWeight(parseFontWeight(weight))
            }

        // Border modifiers
        case .Border:
            if let border = modifier.value as? BorderValue {
                view.border(parseColor(border.color), width: border.width)
            }

        // Interaction modifiers
        case .Disabled:
            if let disabled = modifier.value as? Bool {
                view.disabled(disabled)
            }

        default:
            view
        }
    }

    // MARK: - Helper Functions

    private func parseColor(_ color: SwiftUIColor) -> Color {
        switch color.type {
        case .RGB:
            if let rgb = color.value as? RGBValue {
                return Color(
                    red: Double(rgb.red),
                    green: Double(rgb.green),
                    blue: Double(rgb.blue),
                    opacity: Double(rgb.opacity)
                )
            }
        case .System:
            if let name = color.value as? String {
                return colorFromSystemName(name)
            }
        case .Named:
            if let name = color.value as? String {
                return Color(name)
            }
        }
        return .clear
    }

    private func colorFromSystemName(_ name: String) -> Color {
        switch name {
        case "primary": return .primary
        case "secondary": return .secondary
        case "white": return .white
        case "black": return .black
        case "red": return .red
        case "blue": return .blue
        case "green": return .green
        case "clear": return .clear
        default: return .primary
        }
    }

    private func parseFont(_ style: FontStyle) -> Font {
        switch style {
        case .LargeTitle: return .largeTitle
        case .Title: return .title
        case .Title2: return .title2
        case .Title3: return .title3
        case .Headline: return .headline
        case .Subheadline: return .subheadline
        case .Body: return .body
        case .Callout: return .callout
        case .Footnote: return .footnote
        case .Caption: return .caption
        case .Caption2: return .caption2
        default: return .body
        }
    }

    private func parseFontWeight(_ weight: FontWeight) -> Font.Weight {
        switch weight {
        case .UltraLight: return .ultraLight
        case .Thin: return .thin
        case .Light: return .light
        case .Regular: return .regular
        case .Medium: return .medium
        case .Semibold: return .semibold
        case .Bold: return .bold
        case .Heavy: return .heavy
        case .Black: return .black
        default: return .regular
        }
    }

    private func parseSize(_ size: SizeValue?) -> CGFloat? {
        guard let size = size else { return nil }

        if let fixed = size as? SizeValue.Fixed {
            return CGFloat(fixed.value)
        } else if size is SizeValue.Infinity {
            return .infinity
        }
        return nil
    }

    private func parseAlignment(_ alignment: ZStackAlignment) -> Alignment {
        switch alignment {
        case .TopLeading: return .topLeading
        case .Top: return .top
        case .TopTrailing: return .topTrailing
        case .Leading: return .leading
        case .Center: return .center
        case .Trailing: return .trailing
        case .BottomLeading: return .bottomLeading
        case .Bottom: return .bottom
        case .BottomTrailing: return .bottomTrailing
        default: return .center
        }
    }
}
```

### 3. Usage in SwiftUI App

#### Basic Usage

```swift
import SwiftUI
import AvaElementsiOS

struct ContentView: View {
    // Get the SwiftUIView from Kotlin
    let loginScreen = IOSExampleKt.createiOSLoginScreen()

    var body: some View {
        AvaElementsView(component: loginScreen)
    }
}
```

#### With Theme Customization

```swift
struct ThemedContentView: View {
    let renderer = SwiftUIRenderer.companion.withLiquidGlass()

    var body: some View {
        // Create UI in Kotlin
        let ui = createMyCustomUI()

        // Render to SwiftUI
        if let swiftView = renderer.renderUI(ui: ui) {
            AvaElementsView(component: swiftView)
                .background(.ultraThinMaterial)  // iOS 26 Liquid Glass effect
        }
    }
}
```

#### With State Management

```swift
class AvaElementsViewModel: ObservableObject {
    let renderer = SwiftUIRenderer()
    @Published var currentView: SwiftUIView?

    init() {
        renderer.applyTheme(theme: Themes.shared.iOS26LiquidGlass)
        loadInitialView()
    }

    func loadInitialView() {
        let ui = IOSExampleKt.createiOSLoginScreen()
        currentView = ui
    }

    func switchToSettings() {
        let ui = IOSExampleKt.createiOSSettingsScreen()
        currentView = ui
    }
}

struct AppView: View {
    @StateObject private var viewModel = AvaElementsViewModel()

    var body: some View {
        if let view = viewModel.currentView {
            AvaElementsView(component: view)
        }
    }
}
```

## Integration Steps

1. **Add Kotlin/Native Framework to Xcode**
   - Build the framework: `./gradlew buildIOSFramework`
   - Add `AvaElementsiOS.framework` to your Xcode project
   - Embed the framework in your app target

2. **Create Swift Bridge Files**
   - Add `AvaElementsView.swift`
   - Add `ModifierExtensions.swift`

3. **Import and Use**
   ```swift
   import AvaElementsiOS
   ```

4. **Call Kotlin Code**
   ```swift
   let loginScreen = IOSExampleKt.createiOSLoginScreen()
   ```

5. **Render in SwiftUI**
   ```swift
   AvaElementsView(component: loginScreen)
   ```

## Best Practices

1. **State Management**: Use Swift's state management (@State, @StateObject) for view state
2. **Callbacks**: Set up proper callback mechanisms for Kotlin → Swift communication
3. **Performance**: Cache rendered views when possible
4. **Theme Updates**: Recreate views when theme changes
5. **Error Handling**: Add proper error handling for bridge communication

## See Also

- [README.md](README.md) - Complete integration guide
- [iOSExample.kt](src/iosMain/kotlin/com/augmentalis/avaelements/renderer/ios/iOSExample.kt) - Kotlin usage examples
- [SwiftUIModels.kt](src/iosMain/kotlin/com/augmentalis/avaelements/renderer/ios/bridge/SwiftUIModels.kt) - Bridge data models

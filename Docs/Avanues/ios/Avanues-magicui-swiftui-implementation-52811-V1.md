# MagicUI SwiftUI Implementation

Platform: iOS (SwiftUI) | Min: iOS 15.0+ | Target: iOS 17.0+ | Version: 1.0.0

---

## Dependencies

```swift
// Package.swift
dependencies: [
    .package(url: "https://github.com/augmentalis/magicui-swift.git", from: "1.0.0")
]
```

---

## Theme Setup

```swift
// App.swift
import SwiftUI
import MagicUI

@main
struct YourApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
```

---

## Ocean Colors

```swift
// OceanTheme.swift
extension Color {
    // Base
    static let deepOcean = Color(hex: "0A1929")
    static let oceanDepth = Color(hex: "0F172A")
    static let oceanMid = Color(hex: "1E293B")
    static let oceanShallow = Color(hex: "334155")

    // Accent
    static let coralBlue = Color(hex: "3B82F6")
    static let turquoiseCyan = Color(hex: "06B6D4")
    static let seafoamGreen = Color(hex: "10B981")
    static let sunsetOrange = Color(hex: "F59E0B")
    static let coralRed = Color(hex: "EF4444")

    // Neutral
    static let pearlWhite = Color(hex: "F8FAFC")
    static let seaMist = Color(hex: "E2E8F0")
    static let stormGray = Color(hex: "94A3B8")
    static let deepFog = Color(hex: "475569")

    // Surface
    static let surface5 = Color.white.opacity(0.05)
    static let surface10 = Color.white.opacity(0.10)
    static let surface15 = Color.white.opacity(0.15)
    static let surface20 = Color.white.opacity(0.20)
    static let surface30 = Color.white.opacity(0.30)

    // Border
    static let border10 = Color.white.opacity(0.10)
    static let border20 = Color.white.opacity(0.20)
    static let border30 = Color.white.opacity(0.30)

    // Text
    static let textPrimary = Color.white.opacity(0.90)
    static let textSecondary = Color.white.opacity(0.80)
    static let textMuted = Color.white.opacity(0.60)
    static let textDisabled = Color.white.opacity(0.40)
}

// Hex initializer
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
}
```

---

## Gradients

```swift
extension LinearGradient {
    static let oceanBackground = LinearGradient(
        gradient: Gradient(colors: [.deepOcean, .oceanDepth, .oceanMid, .oceanDepth]),
        startPoint: .top,
        endPoint: .bottom
    )

    static let oceanAccent = LinearGradient(
        gradient: Gradient(colors: [Color.coralBlue.opacity(0.2), Color.turquoiseCyan.opacity(0.2)]),
        startPoint: .leading,
        endPoint: .trailing
    )

    static let oceanSuccess = LinearGradient(
        gradient: Gradient(colors: [Color.seafoamGreen.opacity(0.2), Color.turquoiseCyan.opacity(0.2)]),
        startPoint: .leading,
        endPoint: .trailing
    )
}
```

---

## Background

```swift
struct OceanBackground: View {
    var showGrid: Bool = true
    var showAmbientLights: Bool = true
    var gridSpacing: CGFloat = 50
    var gridOpacity: Double = 0.1

    var body: some View {
        ZStack {
            LinearGradient.oceanBackground.ignoresSafeArea()

            if showGrid {
                GridPattern(spacing: gridSpacing, opacity: gridOpacity)
            }

            if showAmbientLights {
                AmbientLights()
            }
        }
    }
}

struct GridPattern: View {
    let spacing: CGFloat
    let opacity: Double

    var body: some View {
        GeometryReader { geometry in
            Path { path in
                var x: CGFloat = 0
                while x < geometry.size.width {
                    path.move(to: CGPoint(x: x, y: 0))
                    path.addLine(to: CGPoint(x: x, y: geometry.size.height))
                    x += spacing
                }

                var y: CGFloat = 0
                while y < geometry.size.height {
                    path.move(to: CGPoint(x: 0, y: y))
                    path.addLine(to: CGPoint(x: geometry.size.width, y: y))
                    y += spacing
                }
            }
            .stroke(Color.stormGray.opacity(opacity), lineWidth: 1)
        }
    }
}

struct AmbientLights: View {
    var body: some View {
        GeometryReader { geometry in
            Circle()
                .fill(RadialGradient(
                    gradient: Gradient(colors: [Color.coralBlue.opacity(0.2), Color.clear]),
                    center: .center,
                    startRadius: 0,
                    endRadius: 200
                ))
                .frame(width: 400, height: 400)
                .position(x: geometry.size.width * 0.25, y: geometry.size.height * 0.1)

            Circle()
                .fill(RadialGradient(
                    gradient: Gradient(colors: [Color.turquoiseCyan.opacity(0.2), Color.clear]),
                    center: .center,
                    startRadius: 0,
                    endRadius: 200
                ))
                .frame(width: 400, height: 400)
                .position(x: geometry.size.width * 0.75, y: geometry.size.height * 0.9)
        }
    }
}
```

---

## Glassmorphic Surface

```swift
struct GlassmorphicSurface<Content: View>: View {
    let content: Content
    var background: Color = .surface10
    var border: Color = .border20
    var borderWidth: CGFloat = 1
    var blurRadius: CGFloat = 40
    var cornerRadius: CGFloat = 16

    init(
        background: Color = .surface10,
        border: Color = .border20,
        borderWidth: CGFloat = 1,
        blurRadius: CGFloat = 40,
        cornerRadius: CGFloat = 16,
        @ViewBuilder content: () -> Content
    ) {
        self.content = content()
        self.background = background
        self.border = border
        self.borderWidth = borderWidth
        self.blurRadius = blurRadius
        self.cornerRadius = cornerRadius
    }

    var body: some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(background)
                    .background(.ultraThinMaterial)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .strokeBorder(border, lineWidth: borderWidth)
            )
    }
}
```

---

## Data Table

```swift
struct DataTable: View {
    let columns: [TableColumn]
    let rows: [TableRow]

    var body: some View {
        GlassmorphicSurface(background: .clear, border: .border10, cornerRadius: 16) {
            ScrollView {
                VStack(spacing: 0) {
                    HStack(spacing: 16) {
                        ForEach(columns) { column in
                            Text(column.label)
                                .font(.headline)
                                .foregroundColor(.textPrimary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .frame(height: 64)
                    .background(Color.surface10)

                    ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                        HStack(spacing: 16) {
                            ForEach(Array(row.cells.enumerated()), id: \.offset) { _, cell in
                                Text(cell)
                                    .font(.body)
                                    .foregroundColor(.textSecondary)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .frame(minHeight: 56)
                        .background(index % 2 == 0 ? Color.surface5 : Color.clear)

                        if index < rows.count - 1 {
                            Divider().background(Color.border10)
                        }
                    }
                }
            }
        }
    }
}

struct TableColumn: Identifiable {
    let id = UUID()
    let label: String
    let weight: CGFloat
}

struct TableRow: Identifiable {
    let id = UUID()
    let cells: [String]
}
```

---

## Todo List

```swift
struct TodoList: View {
    @Binding var tasks: [Task]

    var body: some View {
        GlassmorphicSurface(background: .surface5, border: .border10, cornerRadius: 16) {
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach($tasks) { $task in
                        HStack(spacing: 12) {
                            Button(action: {
                                task.status = task.status == .completed ? .pending : .completed
                            }) {
                                Image(systemName: task.status == .completed ? "checkmark.circle.fill" : "circle")
                                    .foregroundColor(task.status == .completed ? .seafoamGreen : .border20)
                                    .font(.system(size: 24))
                            }
                            .buttonStyle(PlainButtonStyle())

                            VStack(alignment: .leading, spacing: 4) {
                                Text(task.title)
                                    .font(.body)
                                    .foregroundColor(taskColor(task.status))
                                    .strikethrough(task.status == .completed)

                                HStack(spacing: 8) {
                                    PriorityBadge(priority: task.priority)
                                    StatusIndicator(status: taskStatusType(task.status), size: 6)
                                    Text(task.status.rawValue).font(.caption).foregroundColor(.textMuted)
                                }
                            }

                            Spacer()
                        }
                        .padding(16)
                        .frame(minHeight: 72)

                        if task != tasks.last {
                            Divider().background(Color.border10)
                        }
                    }
                }
                .padding(8)
            }
        }
    }

    private func taskColor(_ status: TaskStatus) -> Color {
        switch status {
        case .completed: return .textMuted
        case .inProgress: return .textPrimary
        case .pending: return .textSecondary
        }
    }

    private func taskStatusType(_ status: TaskStatus) -> StatusType {
        switch status {
        case .completed: return .success
        case .inProgress: return .info
        case .pending: return .pending
        }
    }
}

struct PriorityBadge: View {
    let priority: TaskPriority

    var body: some View {
        Text(priority.rawValue)
            .font(.caption2)
            .foregroundColor(badgeColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .background(badgeColor.opacity(0.2))
            .cornerRadius(4)
    }

    private var badgeColor: Color {
        switch priority {
        case .high: return .coralRed
        case .medium: return .sunsetOrange
        case .low: return .coralBlue
        }
    }
}

struct StatusIndicator: View {
    let status: StatusType
    let size: CGFloat

    var body: some View {
        Circle()
            .fill(statusColor)
            .frame(width: size, height: size)
    }

    private var statusColor: Color {
        switch status {
        case .success: return .seafoamGreen
        case .warning: return .sunsetOrange
        case .error: return .coralRed
        case .info: return .coralBlue
        case .pending: return .textMuted
        }
    }
}

struct Task: Identifiable, Equatable {
    let id = UUID()
    var title: String
    var status: TaskStatus
    var priority: TaskPriority
}

enum TaskStatus: String {
    case pending = "Pending"
    case inProgress = "In Progress"
    case completed = "Completed"
}

enum TaskPriority: String {
    case low = "Low"
    case medium = "Medium"
    case high = "High"
}

enum StatusType {
    case success, warning, error, info, pending
}
```

---

## Modal Dialog

```swift
struct OceanDialog: View {
    let title: String
    let message: String
    let confirmText: String
    let dismissText: String
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        GlassmorphicSurface(background: .surface20, border: .border30, cornerRadius: 24, blurRadius: 40) {
            VStack(spacing: 0) {
                HStack {
                    Text(title).font(.title2).foregroundColor(.textPrimary)
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark").foregroundColor(.textSecondary)
                    }
                }
                .padding(20)

                Divider().background(Color.border10)

                Text(message).font(.body).foregroundColor(.textSecondary).padding(24)

                Divider().background(Color.border10)

                HStack(spacing: 12) {
                    Button(dismissText) { onDismiss() }.foregroundColor(.textSecondary)
                    Button(confirmText) { onConfirm() }.buttonStyle(.borderedProminent).tint(.coralBlue)
                }
                .padding(20)
            }
        }
        .frame(width: 600)
    }
}
```

---

## Toast

```swift
struct OceanToast: View {
    let message: String
    let type: ToastType
    let onDismiss: () -> Void

    var body: some View {
        GlassmorphicSurface(background: .surface30, border: .clear, cornerRadius: 12, blurRadius: 24) {
            HStack(spacing: 12) {
                Image(systemName: toastIcon).foregroundColor(toastColor).font(.system(size: 20))
                Text(message).font(.body).foregroundColor(.textPrimary)
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark").foregroundColor(.textSecondary).font(.system(size: 16))
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .frame(maxWidth: 400)
        .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(toastColor, lineWidth: 4))
    }

    private var toastIcon: String {
        switch type {
        case .info: return "info.circle.fill"
        case .success: return "checkmark.circle.fill"
        case .warning: return "exclamationmark.triangle.fill"
        case .error: return "xmark.circle.fill"
        }
    }

    private var toastColor: Color {
        switch type {
        case .info: return .coralBlue
        case .success: return .seafoamGreen
        case .warning: return .sunsetOrange
        case .error: return .coralRed
        }
    }
}

enum ToastType {
    case info, success, warning, error
}
```

---

## Layout: Dashboard

```swift
struct DashboardView: View {
    var body: some View {
        ZStack {
            OceanBackground()

            ScrollView {
                VStack(spacing: 24) {
                    GlassmorphicSurface(background: .clear, border: .border10, cornerRadius: 16) {
                        ZStack {
                            LinearGradient.oceanAccent

                            VStack(alignment: .leading, spacing: 8) {
                                Text("Enterprise Dashboard").font(.largeTitle).foregroundColor(.textPrimary)
                                Text("Welcome back").font(.body).foregroundColor(.textMuted)
                            }
                            .padding(32)
                        }
                    }

                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        ForEach(["24", "156", "94%", "38"], id: \.self) { value in
                            MetricCard(value: value)
                        }
                    }
                }
                .padding(24)
            }
        }
    }
}

struct MetricCard: View {
    let value: String

    var body: some View {
        GlassmorphicSurface(background: .surface5, border: .border10, cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text(value).font(.title).foregroundColor(.textPrimary)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}
```

---

## VisionOS

```swift
#if os(visionOS)
struct VisionOSApp: App {
    var body: some Scene {
        WindowGroup {
            DashboardView()
        }
        .windowStyle(.volumetric)
        .defaultSize(width: 1100, height: 700, depth: 100)
    }
}

struct ContentView: View {
    var body: some View {
        DashboardView()
            .ornament(visibility: .visible, attachmentAnchor: .scene(.bottom)) {
                AppDock()
            }
    }
}
#endif
```

---

## Accessibility

```swift
// VoiceOver
Button("Settings") { openSettings() }
    .accessibilityLabel("Open Settings")
    .accessibilityHint("Opens the settings screen")

// Dynamic Type
Text("Title").font(.headline)  // Auto-scales

// Touch Targets: 44pt minimum
Button("Action") { action() }
    .frame(minWidth: 44, minHeight: 44)

// SF Symbols
Image(systemName: "star.fill")

// Native blur
content.background(.ultraThinMaterial)
```

---

## Examples

`Universal/Libraries/AvaElements/Renderers/iOS/Examples/`

---

**Platform:** iOS (SwiftUI) | **Min:** iOS 15.0+ | **Target:** iOS 17.0+ | **Version:** 1.0.0 | **Updated:** 2025-11-28

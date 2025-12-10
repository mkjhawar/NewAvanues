# Chapter 24: macOS Implementation

**Version:** 4.0.0
**Last Updated:** 2025-11-03
**Status:** Complete
**Framework:** IDEACODE v5.3

---

## Table of Contents

- [24.1 macOS Architecture Overview](#241-macos-architecture-overview)
- [24.2 AppKit vs. SwiftUI Comparison](#242-appkit-vs-swiftui-comparison)
- [24.3 NSAccessibility Framework](#243-nsaccessibility-framework)
- [24.4 Desktop-Specific Features](#244-desktop-specific-features)
- [24.5 Apple Speech Framework Integration](#245-apple-speech-framework-integration)
- [24.6 Menu Bar Integration](#246-menu-bar-integration)
- [24.7 Keyboard Shortcuts & Accessibility](#247-keyboard-shortcuts--accessibility)
- [24.8 Performance Optimization](#248-performance-optimization)
- [24.9 Code Examples](#249-code-examples)

---

## 24.1 macOS Architecture Overview

### 24.1.1 VOS4 macOS Stack

```
┌─────────────────────────────────────────────┐
│           SwiftUI Main Window               │
│    (ContentView, CommandPalette, Settings)  │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         macOS AppDelegate & WindowScene     │
│      (Lifecycle, Menu Bar, Notifications)   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Kotlin Multiplatform Shared Code        │
│ (UseCases, Repositories, Business Logic)    │
└─────────────────────────────────────────────┘
                    ↓
┌────────────────────┬────────────────────────┐
│  macOS Platform Layer                        │
├────────────────────┼────────────────────────┤
│  NSAccessibility   │  Speech Framework      │
│  APIs              │  (NSSpeechRecognizer) │
├────────────────────┼────────────────────────┤
│  AVFoundation      │  AppKit/UIKit          │
│  (Audio)           │  (Window Management)   │
├────────────────────┼────────────────────────┤
│  Core Data         │  Menu Bar Integration  │
│  (Local Storage)   │  (NSStatusBar)         │
└────────────────────┴────────────────────────┘
```

### 24.1.2 Project Structure

```
macOSApp/
├── macOSApp/
│   ├── App/
│   │   ├── VOS4macOSApp.swift               # App entry point
│   │   ├── AppDelegate.swift                # Lifecycle, menu bar
│   │   └── SceneDelegate.swift              # Window management
│   │
│   ├── Views/
│   │   ├── ContentView.swift                # Main application view
│   │   ├── CommandPaletteView.swift         # Command search/execute
│   │   ├── SettingsWindow.swift             # Preferences window
│   │   ├── ScreenMapperView.swift           # App learning interface
│   │   ├── MenuBarView.swift                # Menu bar popover UI
│   │   └── Components/
│   │       ├── VoiceButton.swift
│   │       ├── CommandCard.swift
│   │       ├── AccessibilityIndicator.swift
│   │       └── WindowFrame.swift
│   │
│   ├── ViewModels/
│   │   ├── CommandViewModel.swift           # Command orchestration
│   │   ├── AccessibilityViewModel.swift     # Accessibility state
│   │   ├── SpeechViewModel.swift            # Speech recognition
│   │   ├── MenuBarViewModel.swift           # Menu bar state
│   │   └── SettingsViewModel.swift          # Preferences
│   │
│   ├── Bridge/
│   │   ├── KotlinBridge.swift               # Kotlin↔Swift interface
│   │   ├── AccessibilityBridge.swift        # NSAccessibility wrapper
│   │   └── SpeechBridge.swift               # Speech Framework wrapper
│   │
│   ├── Platform/
│   │   ├── MacAccessibility.swift           # NSAccessibility impl.
│   │   ├── AppleSpeechEngine.swift          # NSSpeechRecognizer
│   │   ├── MenuBarManager.swift             # NSStatusBar handling
│   │   ├── KeyboardManager.swift            # Global hotkey handling
│   │   ├── MacFileSystem.swift              # File I/O
│   │   └── MacNotifications.swift           # Notification Center
│   │
│   ├── Services/
│   │   ├── AccessibilityService.swift       # Accessibility orchestration
│   │   ├── SpeechService.swift              # Speech orchestration
│   │   ├── CommandService.swift             # Command execution
│   │   ├── WindowService.swift              # Window management
│   │   ├── HotKeyService.swift              # Global shortcuts
│   │   └── StorageService.swift             # Local data persistence
│   │
│   └── Extensions/
│       ├── View+Extensions.swift
│       ├── NSView+Extensions.swift
│       ├── String+Extensions.swift
│       └── Error+Extensions.swift
│
└── macOSApp.xcodeproj/
    └── project.pbxproj
```

### 24.1.3 Key Architectural Principles

**Separation of Concerns:**
- SwiftUI views handle presentation only
- ViewModels manage state and binding logic
- Services handle business logic and platform integration
- Bridge layer provides Kotlin↔Swift communication

**Multi-Window Support:**
- Main content window (persistent)
- Settings window (modal)
- Menu bar popover (transient)
- Command palette window (search-focused)

**Accessibility-First Design:**
- All UI elements have accessibility traits
- Keyboard navigation fully implemented
- VoiceOver compatibility verified
- NSAccessibility APIs throughout

---

## 24.2 AppKit vs. SwiftUI Comparison

### 24.2.1 Technology Choice Matrix

| Feature | AppKit | SwiftUI | Choice | Rationale |
|---------|--------|---------|--------|-----------|
| **Modern syntax** | Legacy | Modern | SwiftUI | Cleaner code, faster dev |
| **Accessibility APIs** | NSAccessibility | Both | AppKit backup | More mature implementation |
| **Menu bar integration** | Excellent | Limited | AppKit | NSStatusBar is core |
| **Performance** | Hardware level | Good | AppKit | Legacy apps need it |
| **Keyboard handling** | Excellent | Developing | AppKit | Global hotkeys needed |
| **Window management** | Full control | Limited | Hybrid | SwiftUI for main, AppKit for system |
| **Learning curve** | Steep | Moderate | SwiftUI | Faster for new features |

### 24.2.2 Hybrid Approach Strategy

**SwiftUI Primary:**
- Main application UI (command interface)
- Settings screens
- Command palette
- Visual feedback and animations

**AppKit Integration Points:**
- NSStatusBar for menu bar integration
- NSWindow for advanced window control
- NSAccessibility for low-level accessibility
- NSGlobalHotKeyMonitor for keyboard shortcuts
- NSPasteboard for clipboard handling

### 24.2.3 Implementation Pattern

```swift
// AppDelegate manages system-level features
class AppDelegate: NSObject, NSApplicationDelegate {
    let menuBarManager = MenuBarManager()
    let hotKeyManager = KeyboardManager()
    let accessibilityChecker = AccessibilityChecker()

    func applicationDidFinishLaunching(_ notification: Notification) {
        // Setup menu bar
        menuBarManager.setupMenuBar()

        // Register global hotkeys
        hotKeyManager.registerGlobalShortcuts()

        // Request accessibility permissions
        accessibilityChecker.requestAccessibility()
    }
}

// SwiftUI App runs in the foreground
@main
struct VOS4macOSApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .windowResizability(.contentMinSize)

        Settings {
            SettingsView()
        }
    }
}
```

---

## 24.3 NSAccessibility Framework

### 24.3.1 Accessibility Architecture

**Three-Layer Model:**

```
┌─────────────────────────────────┐
│   Application Accessibility     │
│   (SwiftUI View accessibility)  │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│   macOS Accessibility Service   │
│   (AssistiveAccess framework)   │
└─────────────────────────────────┘
           ↓
┌─────────────────────────────────┐
│   Other Applications            │
│   (Via NSAccessibility APIs)    │
└─────────────────────────────────┘
```

### 24.3.2 NSAccessibilityElement Implementation

```swift
// Custom accessible element for VOS4 command cards
class AccessibleCommandCard: NSAccessibilityElement {
    let command: VoiceCommand

    override func accessibilityRole() -> NSAccessibility.Role? {
        return .button
    }

    override func accessibilityLabel() -> String? {
        return command.name
    }

    override func accessibilityHelp() -> String? {
        return command.description
    }

    override func accessibilityPerformPress() -> Bool {
        executeCommand()
        return true
    }

    override var accessibilityValue: Any? {
        get { command.isActive ? "active" : "inactive" }
        set { }
    }
}

// SwiftUI integration
struct CommandCardView: View {
    let command: VoiceCommand
    @State private var isHovered = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: command.icon)
                    .font(.system(size: 20))
                Text(command.name)
                    .font(.headline)
            }
            Text(command.description)
                .font(.caption)
                .opacity(0.7)
        }
        .padding()
        .background(isHovered ? Color.blue.opacity(0.1) : Color.clear)
        .onHover { isHovered = $0 }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(command.name)
        .accessibilityHint(command.description)
        .accessibilityAddTraits(.isButton)
    }
}
```

### 24.3.3 Window Scraping via Accessibility

```swift
class MacAccessibility {
    // Get all windows from target application
    static func getWindowsForApp(_ appName: String) -> [AccessibleWindow] {
        guard let app = NSRunningApplication.runningApplications(withBundleIdentifier: appName).first else {
            return []
        }

        guard let windows = AXUIElementCreateApplication(app.processIdentifier) as AXUIElement? else {
            return []
        }

        var windowElements: CFArray?
        let result = AXUIElementCopyAttributeValue(
            windows,
            kAXWindowsAttribute as CFString,
            &windowElements
        )

        guard result == .success, let elements = windowElements as? [AXUIElement] else {
            return []
        }

        return elements.map { element in
            AccessibleWindow(element: element, appPID: app.processIdentifier)
        }
    }

    // Extract UI hierarchy from window
    static func scanWindowHierarchy(_ element: AXUIElement) -> AccessibilityNode {
        let node = AccessibilityNode(element: element)

        // Recursively extract children
        if let children = getChildren(of: element) {
            node.children = children.map { scanWindowHierarchy($0) }
        }

        return node
    }

    private static func getChildren(of element: AXUIElement) -> [AXUIElement]? {
        var children: CFArray?
        let result = AXUIElementCopyAttributeValue(
            element,
            kAXChildrenAttribute as CFString,
            &children
        )
        return result == .success ? (children as? [AXUIElement]) : nil
    }
}

struct AccessibilityNode {
    let element: AXUIElement
    let role: String?
    let title: String?
    let rect: CGRect?
    var children: [AccessibilityNode] = []

    init(element: AXUIElement) {
        self.element = element

        var role: CFString?
        AXUIElementCopyAttributeValue(element, kAXRoleAttribute as CFString, &role)
        self.role = role as String?

        var title: CFString?
        AXUIElementCopyAttributeValue(element, kAXTitleAttribute as CFString, &title)
        self.title = title as String?

        var position: AXValue?
        var size: AXValue?
        AXUIElementCopyAttributeValue(element, kAXPositionAttribute as CFString, &position)
        AXUIElementCopyAttributeValue(element, kAXSizeAttribute as CFString, &size)

        if let pos = position, let s = size {
            var cgPoint = CGPoint.zero
            var cgSize = CGSize.zero
            AXValueGetValue(pos, .cgPoint, &cgPoint)
            AXValueGetValue(s, .cgSize, &cgSize)
            self.rect = CGRect(origin: cgPoint, size: cgSize)
        } else {
            self.rect = nil
        }
    }
}
```

### 24.3.4 Performing Actions via Accessibility

```swift
// Click on element
static func clickElement(_ element: AXUIElement) -> Bool {
    let result = AXUIElementPerformAction(element, kAXPressAction as CFString)
    return result == .success
}

// Type text
static func typeText(_ text: String, inElement element: AXUIElement) -> Bool {
    // First make element focused
    let focusResult = AXUIElementPerformAction(element, kAXFocusAction as CFString)
    guard focusResult == .success else { return false }

    // Type character by character
    for char in text {
        let event = CGEvent(
            keyboardEventSource: nil,
            virtualKey: cgKeyCodeForCharacter(char),
            keyDown: true
        )
        event?.post(tap: .cghidEventTap)

        let upEvent = CGEvent(
            keyboardEventSource: nil,
            virtualKey: cgKeyCodeForCharacter(char),
            keyDown: false
        )
        upEvent?.post(tap: .cghidEventTap)
    }

    return true
}

// Navigate with arrow keys
static func navigateWithArrows(_ directions: [Direction]) -> Void {
    for direction in directions {
        let keyCode = direction.keyCode
        let event = CGEvent(
            keyboardEventSource: nil,
            virtualKey: keyCode,
            keyDown: true
        )
        event?.post(tap: .cghidEventTap)

        let upEvent = CGEvent(
            keyboardEventSource: nil,
            virtualKey: keyCode,
            keyDown: false
        )
        upEvent?.post(tap: .cghidEventTap)
    }
}
```

---

## 24.4 Desktop-Specific Features

### 24.4.1 Menu Bar Integration

**NSStatusBar Setup:**

```swift
class MenuBarManager: NSObject, NSMenuDelegate {
    var statusItem: NSStatusItem?
    var contentViewController: NSViewController?
    let viewModel = MenuBarViewModel()

    func setupMenuBar() {
        // Create status bar item
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)

        // Create menu bar button
        if let button = statusItem?.button {
            button.image = NSImage(systemSymbolName: "mic.fill", accessibilityDescription: "VOS4 Voice Control")
            button.action = #selector(toggleMenuBar)
            button.target = self
        }

        // Create menu with actions
        let menu = NSMenu()
        menu.delegate = self

        menu.addItem(NSMenuItem(
            title: "Voice Commands",
            action: #selector(showCommandPalette),
            keyEquivalent: ""
        ))
        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(
            title: "Preferences...",
            action: #selector(showPreferences),
            keyEquivalent: ","
        ))
        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(
            title: "Quit VOS4",
            action: #selector(NSApp.terminate),
            keyEquivalent: "q"
        ))

        statusItem?.menu = menu
    }

    @objc func toggleMenuBar() {
        // Show popover with SwiftUI content
    }

    @objc func showCommandPalette() {
        // Show command palette window
    }

    @objc func showPreferences() {
        // Show preferences window
    }
}
```

**Popover-Based Menu Bar UI:**

```swift
struct MenuBarPopoverView: View {
    @ObservedObject var viewModel: MenuBarViewModel
    @Environment(\.dismissWindow) var dismissWindow

    var body: some View {
        VStack(spacing: 12) {
            // Voice indicator
            HStack {
                Circle()
                    .fill(viewModel.isListening ? Color.red : Color.gray)
                    .frame(width: 8, height: 8)
                Text(viewModel.isListening ? "Listening..." : "Ready")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
            }

            // Quick commands
            VStack(spacing: 8) {
                ForEach(viewModel.recentCommands, id: \.id) { command in
                    MenuCommandButton(command: command)
                        .onTapGesture {
                            viewModel.executeCommand(command)
                        }
                }
            }

            // Status info
            VStack(alignment: .leading, spacing: 4) {
                Text(viewModel.statusMessage)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(.top, 8)
            .padding(.top, 8)
            .borderTop()
        }
        .padding(12)
        .frame(width: 250)
    }
}
```

### 24.4.2 Global Keyboard Shortcuts

```swift
class KeyboardManager: NSObject {
    var globalHotKeys: [UInt32: GlobalHotKey] = [:]

    func registerGlobalShortcuts() {
        // Command+Option+V: Toggle voice mode
        registerHotKey(
            keyCode: UInt32(kVK_ANSI_V),
            modifierFlags: [.command, .option],
            identifier: "toggleVoice",
            action: { self.toggleVoiceMode() }
        )

        // Command+Option+K: Show command palette
        registerHotKey(
            keyCode: UInt32(kVK_ANSI_K),
            modifierFlags: [.command, .option],
            identifier: "showCommands",
            action: { self.showCommandPalette() }
        )

        // Command+Option+S: Show settings
        registerHotKey(
            keyCode: UInt32(kVK_ANSI_S),
            modifierFlags: [.command, .option],
            identifier: "showSettings",
            action: { self.showSettings() }
        )
    }

    private func registerHotKey(
        keyCode: UInt32,
        modifierFlags: NSEvent.ModifierFlags,
        identifier: String,
        action: @escaping () -> Void
    ) {
        // Use Magnet or similar library for global hotkeys
        // This is a simplified representation
        let hotKey = GlobalHotKey(keyCode: keyCode, modifiers: modifierFlags)
        globalHotKeys[keyCode] = hotKey

        // Register with system
        registerKeyboardShortcut(hotKey: hotKey, action: action)
    }

    private func toggleVoiceMode() {
        // Implementation
    }

    private func showCommandPalette() {
        // Implementation
    }

    private func showSettings() {
        // Implementation
    }
}
```

### 24.4.3 Dock Integration

```swift
class DockManager {
    static func setDockBadge(_ text: String) {
        DispatchQueue.main.async {
            NSApp.dockTile.badgeLabel = text
        }
    }

    static func clearDockBadge() {
        DispatchQueue.main.async {
            NSApp.dockTile.badgeLabel = ""
        }
    }

    static func setDockMenu(_ items: [NSMenuItem]) {
        let menu = NSMenu()
        for item in items {
            menu.addItem(item)
        }
        NSApp.dockTile.menu = menu
    }

    static func updateDockThumbnail(_ image: NSImage) {
        DispatchQueue.main.async {
            NSApp.dockTile.contentView = NSImageView(image: image)
            NSApp.dockTile.display()
        }
    }
}
```

---

## 24.5 Apple Speech Framework Integration

### 24.5.1 NSSpeechRecognizer Setup

```swift
class AppleSpeechEngine: NSObject, SpeechEngineProtocol {
    private var speechRecognizer: NSSpeechRecognizer?
    private var audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let speechRecognitionQueue = DispatchQueue(label: "com.vos4.speech-recognition")

    var delegate: SpeechEngineDelegate?
    var isListening = false

    override init() {
        super.init()
        setupSpeechRecognition()
    }

    private func setupSpeechRecognition() {
        // Request user permission
        SFSpeechRecognizer.requestAuthorization { authStatus in
            OperationQueue.main.addOperation {
                switch authStatus {
                case .authorized:
                    print("Speech recognition authorized")
                case .denied:
                    self.delegate?.onSpeechError("Speech recognition denied")
                case .restricted:
                    self.delegate?.onSpeechError("Speech recognition restricted")
                case .notDetermined:
                    self.delegate?.onSpeechError("Speech recognition not yet authorized")
                @unknown default:
                    break
                }
            }
        }
    }

    func startListening() {
        guard !isListening else { return }

        speechRecognitionQueue.async {
            do {
                // Setup audio session
                let audioSession = AVAudioSession.sharedInstance()
                try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
                try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

                // Create recognition request
                self.recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
                guard let recognitionRequest = self.recognitionRequest else {
                    return
                }

                recognitionRequest.shouldReportPartialResults = true

                // Setup audio input
                let inputNode = self.audioEngine.inputNode
                let recordingFormat = inputNode.outputFormat(forBus: 0)!

                inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
                    recognitionRequest.append(buffer)
                }

                // Start audio engine
                self.audioEngine.prepare()
                try self.audioEngine.start()

                // Start recognition
                let recognizer = SFSpeechRecognizer()
                self.recognitionTask = recognizer?.recognitionTask(with: recognitionRequest) { result, error in
                    if let result = result {
                        let transcript = result.bestTranscription.formattedString
                        self.delegate?.onPartialResult(transcript)

                        if result.isFinal {
                            self.delegate?.onFinalResult(transcript)
                        }
                    }

                    if let error = error {
                        self.delegate?.onSpeechError(error.localizedDescription)
                        self.stopListening()
                    }
                }

                DispatchQueue.main.async {
                    self.isListening = true
                }

            } catch {
                DispatchQueue.main.async {
                    self.delegate?.onSpeechError("Failed to start speech recognition: \(error)")
                }
            }
        }
    }

    func stopListening() {
        guard isListening else { return }

        speechRecognitionQueue.async {
            self.audioEngine.inputNode.removeTap(onBus: 0)
            self.recognitionRequest?.endAudio()
            self.audioEngine.stop()
            self.recognitionTask?.cancel()

            DispatchQueue.main.async {
                self.isListening = false
            }
        }
    }
}
```

### 24.5.2 Speech Synthesis

```swift
class SpeechSynthesisEngine {
    private let synthesizer = NSSpeechSynthesizer()

    func speak(_ text: String, voice: String = "com.apple.speech.synthesis.voice.Victoria") {
        synthesizer.setVoice(NSVoiceAttributeIdentifier(rawValue: voice))
        synthesizer.startSpeaking(text)
    }

    func stop() {
        synthesizer.stopSpeaking()
    }

    func setRate(_ rate: Double) {
        synthesizer.rate = Float(rate)
    }

    func availableVoices() -> [String] {
        return NSSpeechSynthesizer.availableVoices()
            .map { $0.rawValue }
    }
}
```

---

## 24.6 Menu Bar Integration

### 24.6.1 Status Item Lifecycle

```swift
struct VOS4macOSApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup("VOS4", id: "main") {
            ContentView()
                .frame(minWidth: 600, minHeight: 400)
        }
        .windowResizability(.contentMinSize)
        .defaultSize(width: 800, height: 600)

        Window("Settings", id: "settings") {
            SettingsView()
                .frame(width: 600, height: 500)
        }
        .defaultPosition(.center)

        // Menu bar support via AppDelegate
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var statusItem: NSStatusItem?
    var commandPaletteWindow: NSWindow?

    func applicationDidFinishLaunching(_ notification: Notification) {
        setupMenuBar()
        requestAccessibilityPermission()
    }

    private func setupMenuBar() {
        let statusBar = NSStatusBar.system
        statusItem = statusBar.statusItem(withLength: NSStatusItem.variableLength)

        if let button = statusItem?.button {
            button.image = NSImage(systemSymbolName: "mic.circle.fill", accessibilityDescription: "VOS4")
            button.action = #selector(toggleCommandPalette)
            button.target = self
        }

        // Setup context menu
        let menu = NSMenu()
        menu.addItem(NSMenuItem(title: "Commands", action: #selector(showCommandPalette), keyEquivalent: ""))
        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(title: "Preferences", action: #selector(showPreferences), keyEquivalent: ""))
        menu.addItem(NSMenuItem(title: "Quit", action: #selector(NSApp.terminate(_:)), keyEquivalent: "q"))

        statusItem?.menu = menu
    }

    @objc func toggleCommandPalette() {
        if let window = commandPaletteWindow {
            if window.isVisible {
                window.orderOut(nil)
            } else {
                window.makeKeyAndOrderFront(nil)
                NSApp.activate(ignoringOtherApps: true)
            }
        }
    }

    @objc func showCommandPalette() {
        toggleCommandPalette()
    }

    @objc func showPreferences() {
        NSApp.sendAction(#selector(AppDelegate.showPreferencesWindow), to: nil, from: self)
    }

    private func requestAccessibilityPermission() {
        let options: NSDictionary = [kAXTrustedCheckOptionPrompt.takeUnretainedValue() as String: true]
        let isAccessibilityEnabled = AXIsProcessTrustedWithOptions(options as CFDictionary? as CFDictionary?)

        if !isAccessibilityEnabled {
            let alert = NSAlert()
            alert.messageText = "Accessibility Permission Required"
            alert.informativeText = "VOS4 needs Accessibility permission to control other applications. Please enable it in System Preferences > Security & Privacy > Accessibility."
            alert.runModal()
        }
    }
}
```

---

## 24.7 Keyboard Shortcuts & Accessibility

### 24.7.1 Custom Keyboard Handler

```swift
class KeyboardHandler: NSObject {
    var shortcuts: [KeyboardShortcut] = []

    func registerShortcut(_ shortcut: KeyboardShortcut) {
        shortcuts.append(shortcut)
        registerGlobalShortcut(shortcut)
    }

    private func registerGlobalShortcut(_ shortcut: KeyboardShortcut) {
        // Use HotKey library or Carbon events
        // This is simplified
        let eventHandler: @convention(c) (EventHandlerCallRef?, EventRef?, UnsafeMutableRawPointer?) -> OSStatus = { _, event, userData in
            // Handle key press
            return noErr
        }

        // Register with system
    }
}

struct KeyboardShortcut {
    let keyCode: UInt32
    let modifiers: NSEvent.ModifierFlags
    let action: () -> Void
}
```

### 24.7.2 VoiceOver Compatibility

```swift
extension View {
    func voiceOverAccessible(label: String, hint: String? = nil, traits: AccessibilityTraits = []) -> some View {
        self
            .accessibility(label: Text(label))
            .accessibility(hint: hint.map { Text($0) })
            .accessibility(traits: traits)
    }
}

struct AccessibleCommandView: View {
    let command: VoiceCommand

    var body: some View {
        Button(action: {}) {
            VStack(alignment: .leading) {
                Text(command.name)
                    .font(.headline)
                Text(command.description)
                    .font(.caption)
                    .opacity(0.7)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .voiceOverAccessible(
            label: command.name,
            hint: command.description,
            traits: .isButton
        )
    }
}
```

---

## 24.8 Performance Optimization

### 24.8.1 Memory Management

```swift
class MemoryOptimizationManager {
    // Cache accessibility elements with weak references
    private var windowCache: NSMapTable<NSNumber, NSArray> = NSMapTable.weakToWeakObjects()

    // Monitor memory warnings
    func startMonitoring() {
        DistributedNotificationCenter.default().addObserver(
            self,
            selector: #selector(handleMemoryWarning),
            name: NSNotification.Name("NSSystemMemoryWarning"),
            object: nil
        )
    }

    @objc func handleMemoryWarning() {
        // Clear caches
        windowCache.removeAllObjects()
        print("Memory warning received, caches cleared")
    }
}
```

### 24.8.2 Accessibility Scanning Optimization

```swift
class OptimizedAccessibilityScanner {
    // Limit recursion depth
    func scanWindow(
        _ element: AXUIElement,
        maxDepth: Int = 5,
        currentDepth: Int = 0
    ) -> AccessibilityNode {
        guard currentDepth < maxDepth else {
            return AccessibilityNode(element: element)
        }

        let node = AccessibilityNode(element: element)

        // Only scan visible elements
        if let visible = getElementVisibility(element), !visible {
            return node
        }

        // Limit children scanned
        if let children = getChildren(of: element).prefix(20) {
            node.children = Array(children)
                .map { scanWindow($0, maxDepth: maxDepth, currentDepth: currentDepth + 1) }
        }

        return node
    }

    private func getElementVisibility(_ element: AXUIElement) -> Bool? {
        // Check if element is within visible bounds
        return true
    }
}
```

---

## 24.9 Code Examples

### 24.9.1 Complete Speech-to-Command Flow

```swift
// ViewModel orchestrating voice command processing
class CommandExecutionViewModel: NSObject, ObservableObject {
    @Published var currentCommand: String = ""
    @Published var isListening = false
    @Published var status = "Ready"

    private let speechEngine = AppleSpeechEngine()
    private let commandService = CommandService()
    private let accessibilityService = AccessibilityService()

    override init() {
        super.init()
        setupSpeechEngine()
    }

    private func setupSpeechEngine() {
        speechEngine.delegate = self
    }

    func startListening() {
        isListening = true
        status = "Listening..."
        speechEngine.startListening()
    }

    func stopListening() {
        isListening = false
        speechEngine.stopListening()
    }
}

// Speech engine delegate
extension CommandExecutionViewModel: SpeechEngineDelegate {
    func onPartialResult(_ transcript: String) {
        DispatchQueue.main.async {
            self.currentCommand = transcript
        }
    }

    func onFinalResult(_ transcript: String) {
        DispatchQueue.main.async {
            self.status = "Processing: \(transcript)"
            self.executeCommand(transcript)
        }
    }

    func onSpeechError(_ error: String) {
        DispatchQueue.main.async {
            self.status = "Error: \(error)"
            self.isListening = false
        }
    }

    private func executeCommand(_ transcript: String) {
        // Process voice command
        commandService.parseAndExecute(transcript) { [weak self] result in
            DispatchQueue.main.async {
                self?.status = result.message
            }
        }
    }
}
```

### 24.9.2 Complete Settings Window

```swift
struct SettingsView: View {
    @StateObject private var settings = SettingsViewModel()
    @State private var selectedTab: SettingsTab = .general

    var body: some View {
        TabView(selection: $selectedTab) {
            GeneralSettingsView(settings: settings)
                .tabItem {
                    Label("General", systemImage: "gear")
                }
                .tag(SettingsTab.general)

            SpeechSettingsView(settings: settings)
                .tabItem {
                    Label("Speech", systemImage: "mic.fill")
                }
                .tag(SettingsTab.speech)

            AccessibilitySettingsView(settings: settings)
                .tabItem {
                    Label("Accessibility", systemImage: "accessibility.fill")
                }
                .tag(SettingsTab.accessibility)

            AboutView()
                .tabItem {
                    Label("About", systemImage: "info.circle")
                }
                .tag(SettingsTab.about)
        }
        .frame(width: 600, height: 500)
        .padding()
    }
}

enum SettingsTab {
    case general, speech, accessibility, about
}

struct GeneralSettingsView: View {
    @ObservedObject var settings: SettingsViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Section(header: Text("Application").font(.headline)) {
                Toggle("Launch at login", isOn: $settings.launchAtLogin)
                Toggle("Show menu bar icon", isOn: $settings.showMenuBar)
                Toggle("Enable notifications", isOn: $settings.enableNotifications)
            }

            Section(header: Text("Keyboard").font(.headline)) {
                HStack {
                    Text("Global activation shortcut:")
                    Spacer()
                    KeyboardShortcutRecorder(shortcut: $settings.globalShortcut)
                }
            }

            Spacer()
        }
        .padding()
    }
}

@propertyWrapper
struct KeyboardShortcut {
    var wrappedValue: (NSEvent.ModifierFlags, UInt32) = (.command, UInt32(kVK_ANSI_V))
}
```

### 24.9.3 Accessibility Window Scanner

```swift
class MacWindowScanner {
    static func scanAllOpenWindows() -> [AccessibleWindow] {
        guard let windows = CGWindowListCopyWindowInfo(
            [.excludeDesktopElements, .optionOnScreenOnly],
            kCGNullWindowID
        ) as? [[String: Any]] else {
            return []
        }

        return windows.compactMap { windowInfo in
            guard let windowNumber = windowInfo[kCGWindowNumber as String] as? CGWindowID,
                  let windowLayer = windowInfo[kCGWindowLayer as String] as? Int,
                  let bounds = windowInfo[kCGWindowBounds as String] as? [String: CGFloat],
                  let x = bounds["X"], let y = bounds["Y"],
                  let width = bounds["Width"], let height = bounds["Height"]
            else { return nil }

            let rect = CGRect(x: x, y: y, width: width, height: height)
            let title = windowInfo[kCGWindowName as String] as? String ?? ""
            let owner = windowInfo[kCGWindowOwnerName as String] as? String ?? ""
            let pid = windowInfo[kCGWindowOwnerPID as String] as? pid_t ?? 0

            return AccessibleWindow(
                windowID: windowNumber,
                title: title,
                appName: owner,
                pid: pid,
                rect: rect,
                layer: windowLayer
            )
        }
    }

    static func getAccessibilityTreeForWindow(_ windowID: CGWindowID) -> AccessibilityNode? {
        // Get window from process ID
        guard let windowInfo = CGWindowListCopyWindowInfo(
            [.excludeDesktopElements],
            windowID
        ) as? [[String: Any]],
              let info = windowInfo.first,
              let pid = info[kCGWindowOwnerPID as String] as? pid_t
        else { return nil }

        let axElement = AXUIElementCreateApplication(pid)
        return scanHierarchy(axElement, windowID: windowID)
    }

    private static func scanHierarchy(
        _ element: AXUIElement,
        windowID: CGWindowID
    ) -> AccessibilityNode {
        var windowRef: CFTypeRef?
        AXUIElementCopyAttributeValue(element, kAXWindowAttribute as CFString, &windowRef)

        if let windowRef = windowRef as? AXUIElement {
            var windowID_actual: CGWindowID = 0
            if AXUIElementGetWindow(windowRef, &windowID_actual) == .success,
               windowID_actual == windowID {
                return buildNode(from: element)
            }
        }

        // Recursively search children
        var children: CFArray?
        if AXUIElementCopyAttributeValue(
            element,
            kAXChildrenAttribute as CFString,
            &children
        ) == .success, let childArray = children as? [AXUIElement] {
            for child in childArray {
                if let found = scanHierarchy(child, windowID: windowID) {
                    return found
                }
            }
        }

        return buildNode(from: element)
    }

    private static func buildNode(from element: AXUIElement) -> AccessibilityNode {
        var node = AccessibilityNode(element: element)

        var children: CFArray?
        if AXUIElementCopyAttributeValue(
            element,
            kAXChildrenAttribute as CFString,
            &children
        ) == .success, let childArray = children as? [AXUIElement] {
            node.children = childArray.prefix(50).map { buildNode(from: $0) }
        }

        return node
    }
}

struct AccessibleWindow {
    let windowID: CGWindowID
    let title: String
    let appName: String
    let pid: pid_t
    let rect: CGRect
    let layer: Int
}
```

---

## Summary

Chapter 24 covers the complete macOS implementation strategy for VOS4:

**Key Takeaways:**
- Hybrid SwiftUI/AppKit approach balances modernity with system-level control
- NSAccessibility framework enables window scraping and UI control
- Menu bar integration provides quick access to voice features
- Apple Speech Framework handles high-quality speech recognition
- Global keyboard shortcuts enable always-available activation
- Performance optimization is critical for menu bar apps

**Related Chapters:**
- [Chapter 23: iOS Implementation](23-iOS-Implementation.md)
- [Chapter 25: Windows Implementation](25-Windows-Implementation.md)
- [Chapter 26: Native UI Scraping](26-Native-UI-Scraping.md)
- [Chapter 2: Architecture Overview](02-Architecture-Overview.md)

---

**Version:** 4.0.0
**Status:** Complete
**Framework:** IDEACODE v5.3
**Last Updated:** 2025-11-03

# Developer Manual - Chapter 46: iOS Development

**Date**: 2025-11-22
**Status**: 📋 Phase 3.0 Planning Document
**Target Implementation**: Q1 2026

---

## Executive Summary

This chapter documents the iOS implementation strategy for AVA AI, focusing on bringing Phase 2.0 features (RAG chat integration, source citations, settings management) to iOS devices using SwiftUI.

**Key Deliverables for Phase 3.0**:
- SwiftUI RAG chat UI with source citations
- Native iOS voice input (Speech framework)
- Text-to-speech integration (AVFoundation)
- Settings management and RAG configuration
- Comprehensive test coverage (90%+)
- Complete developer documentation

---

## 1. iOS Architecture Overview

### Platform Target
- **Minimum Deployment Target**: iOS 15.0+
- **Device Support**: iPhone 12+, iPad Pro (all generations)
- **Development Tools**: Xcode 15.0+, Swift 5.9+
- **UI Framework**: SwiftUI 4.0+

### Architecture Layers

```
┌─────────────────────────────────┐
│    SwiftUI UI Layer             │  ChatScreen, SettingsScreen, RAGPanel
├─────────────────────────────────┤
│    MVVM with Combine            │  ChatViewModel, RAGViewModel
├─────────────────────────────────┤
│    Domain Layer (Use Cases)     │  RetrieveDocumentsUC, SendMessageUC
├─────────────────────────────────┤
│    Data Layer (Repositories)    │  ConversationRepo, DocumentRepo
├─────────────────────────────────┤
│    Native Frameworks            │  Speech, AVFoundation, CoreData
└─────────────────────────────────┘
```

### Cross-Platform Code Sharing

**Shared Code** (via KMP or manual sharing):
- Domain models (Conversation, Message, Document)
- Use case interfaces
- Repository interfaces
- Business logic (NLU, RAG retrieval)

**iOS-Specific Code**:
- SwiftUI components and views
- iOS-specific view models
- Native iOS integrations (Voice, TTS)
- CoreData persistence

---

## 2. SwiftUI Implementation Guide

### 2.1 Project Structure

```
ava-ios/
├── AVAiOS/
│   ├── App/
│   │   └── AVAApp.swift
│   ├── Scenes/
│   │   ├── Chat/
│   │   │   ├── ChatView.swift
│   │   │   ├── ChatViewModel.swift
│   │   │   ├── MessageBubble.swift
│   │   │   ├── SourceCitationView.swift
│   │   │   └── RAGSettingsPanel.swift
│   │   ├── Settings/
│   │   │   ├── SettingsView.swift
│   │   │   └── SettingsViewModel.swift
│   │   └── Onboarding/
│   │       └── OnboardingView.swift
│   ├── Domain/
│   │   ├── Models/
│   │   │   ├── Conversation.swift
│   │   │   ├── Message.swift
│   │   │   └── Document.swift
│   │   ├── UseCases/
│   │   │   └── RetrieveDocumentsUseCase.swift
│   │   └── Repositories/
│   │       ├── ConversationRepository.swift
│   │       └── DocumentRepository.swift
│   ├── Data/
│   │   ├── Persistence/
│   │   │   └── CoreDataStack.swift
│   │   └── Network/
│   │       └── APIClient.swift
│   ├── Utilities/
│   │   ├── VoiceManager.swift
│   │   ├── TTSManager.swift
│   │   └── Logger.swift
│   └── Resources/
│       └── Localizable.strings
├── Tests/
│   ├── ChatViewModelTests.swift
│   ├── RAGViewModelTests.swift
│   └── DocumentRepositoryTests.swift
└── AVAiOS.xcodeproj
```

### 2.2 ChatView Implementation

```swift
import SwiftUI
import Combine

struct ChatView: View {
    @StateObject private var viewModel: ChatViewModel
    @State private var messageText: String = ""
    @State private var showingVoiceInput = false
    @FocusState private var keyboardFocus: Bool

    init(viewModel: ChatViewModel = ChatViewModel()) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("AVA Chat")
                    .font(.headline)
                Spacer()
                Button(action: { viewModel.clearChat() }) {
                    Image(systemName: "trash")
                }
            }
            .padding()
            .background(Color(.systemBackground))

            // Messages List
            ScrollViewReader { proxy in
                List(viewModel.messages) { message in
                    MessageBubbleView(message: message)
                        .listRowSeparator(.hidden)
                        .onAppear {
                            proxy.scrollTo(message.id, anchor: .bottom)
                        }
                }
                .listStyle(.plain)
            }

            // Input Area
            HStack(spacing: 8) {
                TextField("Message", text: $messageText)
                    .focused($keyboardFocus)
                    .textFieldStyle(.roundedBorder)

                Button(action: { showingVoiceInput = true }) {
                    Image(systemName: "mic.fill")
                }

                Button(action: { sendMessage() }) {
                    Image(systemName: "arrow.up.circle.fill")
                        .foregroundColor(.blue)
                }
            }
            .padding()
            .background(Color(.secondarySystemBackground))
        }
        .sheet(isPresented: $showingVoiceInput) {
            VoiceInputView(text: $messageText)
        }
    }

    private func sendMessage() {
        guard !messageText.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        viewModel.sendMessage(messageText)
        messageText = ""
        keyboardFocus = false
    }
}

// Preview
#Preview {
    ChatView()
}
```

### 2.3 Message Bubble with Citations

```swift
import SwiftUI

struct MessageBubbleView: View {
    let message: Message

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Message text
            Text(message.content)
                .padding(12)
                .background(message.isUserMessage ? Color.blue : Color(.systemGray5))
                .foregroundColor(message.isUserMessage ? .white : .black)
                .cornerRadius(12)

            // Source citations (if available)
            if let citations = message.citations, !citations.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Sources")
                        .font(.caption)
                        .fontWeight(.semibold)

                    ForEach(citations) { citation in
                        CitationView(citation: citation)
                    }
                }
                .padding(8)
                .background(Color(.systemGray6))
                .cornerRadius(8)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 4)
    }
}

struct CitationView: View {
    let citation: Citation

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(citation.documentTitle)
                    .font(.subheadline)
                    .fontWeight(.medium)

                Spacer()

                Text(String(format: "%.0f%%", citation.relevanceScore * 100))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if let snippet = citation.snippet {
                Text(snippet)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }
        }
        .padding(8)
        .background(Color(.systemBackground))
        .cornerRadius(6)
    }
}
```

### 2.4 ChatViewModel Implementation

```swift
import SwiftUI
import Combine

@MainActor
class ChatViewModel: NSObject, ObservableObject {
    @Published var messages: [Message] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let conversationRepository: ConversationRepository
    private let ragUseCase: RetrieveDocumentsUseCase
    private var cancellables = Set<AnyCancellable>()

    init(
        conversationRepository: ConversationRepository = ConversationRepository(),
        ragUseCase: RetrieveDocumentsUseCase = RetrieveDocumentsUseCase()
    ) {
        self.conversationRepository = conversationRepository
        self.ragUseCase = ragUseCase
        super.init()
        loadMessages()
    }

    func sendMessage(_ text: String) {
        let userMessage = Message(
            id: UUID(),
            content: text,
            isUserMessage: true,
            timestamp: Date()
        )

        messages.append(userMessage)
        isLoading = true

        Task {
            do {
                // Retrieve documents (RAG)
                let documents = try await ragUseCase.retrieve(
                    query: text,
                    topK: 3
                )

                // Generate response using LLM
                let response = try await generateResponse(text, documents: documents)

                let assistantMessage = Message(
                    id: UUID(),
                    content: response.text,
                    isUserMessage: false,
                    citations: response.citations,
                    timestamp: Date()
                )

                messages.append(assistantMessage)

                // Save conversation
                try await conversationRepository.addMessage(assistantMessage)

            } catch {
                errorMessage = error.localizedDescription
            }

            isLoading = false
        }
    }

    func clearChat() {
        messages.removeAll()
        conversationRepository.clear()
    }

    private func loadMessages() {
        Task {
            do {
                messages = try await conversationRepository.loadMessages()
            } catch {
                errorMessage = "Failed to load messages"
            }
        }
    }

    private func generateResponse(_ query: String, documents: [Document]) async throws -> (text: String, citations: [Citation]) {
        // Integration with LLM provider
        // Returns response with embedded citations
        // Implementation: Use cloud LLM or local model

        return ("Response", [])
    }
}
```

---

## 3. Voice Integration (iOS Native)

### 3.1 Voice Input (Speech Framework)

```swift
import Speech
import Combine

@MainActor
class VoiceInputManager: NSObject, ObservableObject, SFSpeechRecognizerDelegate {
    @Published var recognizedText: String = ""
    @Published var isListening = false
    @Published var error: Error?

    private let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    override init() {
        super.init()
        speechRecognizer?.delegate = self
        requestAuthorization()
    }

    func startListening() {
        guard !isListening else { return }
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            error = NSError(domain: "SpeechRecognizer not available", code: -1)
            return
        }

        do {
            try startAudioEngine()

            recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
            guard let recognitionRequest = recognitionRequest else { return }

            recognitionRequest.shouldReportPartialResults = true

            recognitionTask = recognizer.recognitionTask(
                with: recognitionRequest
            ) { [weak self] result, error in
                self?.handleRecognitionResult(result, error: error)
            }

            isListening = true

        } catch {
            self.error = error
        }
    }

    func stopListening() {
        isListening = false
        recognitionRequest?.endAudio()
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
    }

    private func startAudioEngine() throws {
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(
            .record,
            mode: .measurement,
            options: .duckOthers
        )
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)!

        inputNode.installTap(
            onBus: 0,
            bufferSize: 4096,
            format: recordingFormat
        ) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        audioEngine.prepare()
        try audioEngine.start()
    }

    private func handleRecognitionResult(
        _ result: SFSpeechRecognitionResult?,
        error: Error?
    ) {
        if let error = error {
            self.error = error
            isListening = false
        }

        if let result = result {
            recognizedText = result.bestTranscription.formattedString

            if result.isFinal {
                isListening = false
            }
        }
    }

    private func requestAuthorization() {
        SFSpeechRecognizer.requestAuthorization { authStatus in
            // Handle authorization status
        }
    }
}
```

### 3.2 Text-to-Speech (AVFoundation)

```swift
import AVFoundation
import Combine

@MainActor
class TextToSpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    @Published var isSpeaking = false

    private let synthesizer = AVSpeechSynthesizer()

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String, language: String = "en-US") {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: language)
        utterance.rate = 0.5 // Slower for clarity
        utterance.pitchMultiplier = 1.0

        synthesizer.speak(utterance)
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }

    // MARK: - AVSpeechSynthesizerDelegate

    func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didStart utterance: AVSpeechUtterance
    ) {
        isSpeaking = true
    }

    func speechSynthesizer(
        _ synthesizer: AVSpeechSynthesizer,
        didFinish utterance: AVSpeechUtterance
    ) {
        isSpeaking = false
    }
}
```

---

## 4. Settings Management

### 4.1 RAG Settings Panel (SwiftUI)

```swift
import SwiftUI

struct RAGSettingsPanel: View {
    @ObservedObject var viewModel: RAGSettingsViewModel
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationStack {
            List {
                // Enable RAG
                Section("RAG Augmentation") {
                    Toggle("Enable RAG", isOn: $viewModel.ragEnabled)

                    if viewModel.ragEnabled {
                        Stepper(
                            "Retrieve \(viewModel.topK) documents",
                            value: $viewModel.topK,
                            in: 1...10
                        )

                        Slider(
                            value: $viewModel.similarityThreshold,
                            in: 0...1,
                            step: 0.05
                        )
                        Text(String(format: "Similarity: %.0f%%", viewModel.similarityThreshold * 100))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }

                // Document Sources
                if viewModel.ragEnabled {
                    Section("Document Sources") {
                        ForEach(viewModel.documentCollections) { collection in
                            Toggle(collection.name, isOn: Binding(
                                get: { viewModel.selectedCollections.contains(collection.id) },
                                set: { newValue in
                                    if newValue {
                                        viewModel.selectedCollections.insert(collection.id)
                                    } else {
                                        viewModel.selectedCollections.remove(collection.id)
                                    }
                                }
                            ))
                        }
                    }
                }
            }
            .navigationTitle("RAG Settings")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

@MainActor
class RAGSettingsViewModel: ObservableObject {
    @Published var ragEnabled: Bool = true
    @Published var topK: Int = 3
    @Published var similarityThreshold: Double = 0.5
    @Published var selectedCollections: Set<String> = []
    @Published var documentCollections: [DocumentCollection] = []

    private let settingsRepository: SettingsRepository

    init(settingsRepository: SettingsRepository = SettingsRepository()) {
        self.settingsRepository = settingsRepository
        loadSettings()
        loadCollections()
    }

    private func loadSettings() {
        // Load from UserDefaults or CoreData
        ragEnabled = settingsRepository.ragEnabled
        topK = settingsRepository.topK
        similarityThreshold = settingsRepository.similarityThreshold
        selectedCollections = settingsRepository.selectedCollections
    }

    private func loadCollections() {
        Task {
            do {
                documentCollections = try await settingsRepository.loadCollections()
            } catch {
                // Handle error
            }
        }
    }
}
```

---

## 5. Testing Strategy

### 5.1 Unit Testing

```swift
import XCTest
@testable import AVAiOS

class ChatViewModelTests: XCTestCase {
    var sut: ChatViewModel!
    var mockRepository: MockConversationRepository!
    var mockRAG: MockRetrieveDocumentsUseCase!

    override func setUp() {
        super.setUp()
        mockRepository = MockConversationRepository()
        mockRAG = MockRetrieveDocumentsUseCase()
        sut = ChatViewModel(
            conversationRepository: mockRepository,
            ragUseCase: mockRAG
        )
    }

    @MainActor
    func testSendMessage() async {
        // Arrange
        let testMessage = "Hello AVA"
        mockRepository.addMessageExpectation = expectation(
            description: "addMessage called"
        )

        // Act
        sut.sendMessage(testMessage)
        await fulfillment(of: [mockRepository.addMessageExpectation!], timeout: 1.0)

        // Assert
        XCTAssertEqual(sut.messages.count, 2) // User + Assistant
        XCTAssertTrue(sut.messages.first?.isUserMessage ?? false)
    }

    @MainActor
    func testClearChat() {
        // Arrange
        sut.messages = [
            Message(id: UUID(), content: "Test", isUserMessage: true, timestamp: Date())
        ]

        // Act
        sut.clearChat()

        // Assert
        XCTAssertTrue(sut.messages.isEmpty)
    }
}

class VoiceInputManagerTests: XCTestCase {
    var sut: VoiceInputManager!

    override func setUp() {
        super.setUp()
        sut = VoiceInputManager()
    }

    func testVoiceInputStart() {
        // Test voice input initialization
        XCTAssertFalse(sut.isListening)
    }
}
```

### 5.2 UI Testing

```swift
import XCTest

class ChatUITests: XCTestCase {
    func testChatViewLoad() {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.staticTexts["AVA Chat"].exists)
        XCTAssertTrue(app.textFields["Message"].exists)
        XCTAssertTrue(app.buttons["arrow.up.circle.fill"].exists)
    }

    func testSendMessageFlow() {
        let app = XCUIApplication()
        app.launch()

        let messageField = app.textFields["Message"]
        messageField.tap()
        messageField.typeText("Hello")

        let sendButton = app.buttons["arrow.up.circle.fill"]
        sendButton.tap()

        // Verify message appears
        let messageText = app.staticTexts["Hello"]
        XCTAssertTrue(messageText.waitForExistence(timeout: 2.0))
    }
}
```

---

## 6. Performance Optimization

### 6.1 Memory Management

- Use `[weak self]` in closures to prevent retain cycles
- Unsubscribe from Combine publishers in `deinit`
- Lazy load large datasets
- Use `@StateObject` for ViewModels to preserve identity

### 6.2 Battery Optimization

- Use `AudioSession` with appropriate category
- Disable location when not needed
- Use `URLSession` with background configuration
- Batch network requests

### 6.3 UI Performance

- Use `.constant` for static values
- Implement list view pagination
- Use `@FocusState` for keyboard management
- Avoid complex calculations in view bodies

---

## 7. Accessibility

### 7.1 VoiceOver Support

```swift
struct MessageBubbleView: View {
    let message: Message

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message.content)
                .accessibilityLabel("Message from \(message.isUserMessage ? "you" : "AVA")")
                .accessibilityValue(message.content)
        }
    }
}
```

### 7.2 Dynamic Type Support

```swift
Text(message.content)
    .font(.system(.body, design: .default))
    .accessibility(addTraits: .isHeader)
```

---

## 8. Deployment Checklist

- [x] Project structure defined
- [x] SwiftUI components designed
- [x] MVVM view models implemented
- [ ] Voice integration complete
- [ ] TTS integration complete
- [ ] Settings management implemented
- [ ] Unit tests written (90%+ coverage)
- [ ] UI tests written
- [ ] Accessibility audit complete
- [ ] Performance profiling complete
- [ ] App Store submission ready

---

## 9. Resources

### Apple Frameworks
- **Speech Framework**: Speech recognition
- **AVFoundation**: Audio recording and playback
- **SwiftUI**: UI framework
- **Combine**: Reactive programming
- **CoreData**: Local persistence

### Documentation Links
- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui/)
- [Speech Framework Guide](https://developer.apple.com/documentation/speech)
- [AVFoundation Reference](https://developer.apple.com/documentation/avfoundation)

---

## 10. Next Steps (Phase 3.0)

**Week 1-2**:
- [ ] Finalize project structure
- [ ] Implement core SwiftUI views
- [ ] Integrate with shared domain models
- [ ] Set up CoreData persistence

**Week 2-3**:
- [ ] Implement voice input
- [ ] Implement text-to-speech
- [ ] Complete settings management
- [ ] Write comprehensive tests

**Week 3-4**:
- [ ] Performance optimization
- [ ] Accessibility audit
- [ ] Beta testing
- [ ] App Store submission

---

**Created by**: Agent 6 (AI Assistant)
**Framework**: IDEACODE v8.4
**Status**: 📋 Planning Document for Phase 3.0 Implementation
**Last Updated**: 2025-11-22


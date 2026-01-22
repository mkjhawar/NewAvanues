// filename: Universal/AVA/Features/Chat/src/iosMain/swift/viewmodel/ChatViewModel.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import SwiftUI
import Combine

/// iOS ViewModel for Chat screen state management.
///
/// This is the Swift equivalent of the Kotlin ChatViewModel.
/// Uses ObservableObject and @Published properties (iOS equivalent of StateFlow).
///
/// Responsibilities:
/// - Manage message list
/// - Handle user message sending
/// - Manage RAG state (enabled, selected documents, threshold)
/// - Handle loading and error states
/// - Integrate with KMP shared business logic
@MainActor
class ChatViewModel: ObservableObject {

    // MARK: - Published Properties (equivalent to StateFlow)

    @Published var messages: [Message] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var isNLUReady: Bool = true

    // RAG State (Phase 2)
    @Published var ragEnabled: Bool = false
    @Published var selectedDocumentIds: [String] = []
    @Published var ragThreshold: Float = 0.75
    @Published var currentCitations: [SourceCitation] = []

    // Teach AVA State (Phase 3)
    @Published var showTeachBottomSheet: Bool = false
    @Published var currentTeachMessageId: String?
    @Published var candidateIntents: [String] = []

    // History State (Phase 4)
    @Published var showHistoryOverlay: Bool = false
    @Published var conversations: [Conversation] = []
    @Published var activeConversationId: String = ""

    // Pagination State (Phase 5)
    @Published var hasMoreMessages: Bool = false
    @Published var totalMessageCount: Int = 0

    // MARK: - Private Properties

    private var cancellables = Set<AnyCancellable>()

    // TODO: KMP shared ViewModel reference
    // private let kmpViewModel: ChatViewModelKMP

    // MARK: - Initialization

    init() {
        // TODO: Initialize KMP shared ViewModel
        // kmpViewModel = ChatViewModelKMP()
        // setupKMPBindings()

        // For now, initialize with sample data
        setupSampleData()
    }

    // MARK: - Public Methods

    /// Sends a message to AVA
    /// - Parameter text: The message content
    func sendMessage(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespaces).isEmpty else { return }

        isLoading = true
        errorMessage = nil

        // Create user message
        let userMessage = Message(
            id: UUID().uuidString,
            content: text,
            role: .user,
            timestamp: Date().timeIntervalSince1970 * 1000, // Convert to milliseconds
            confidence: nil,
            intent: nil,
            sourceCitations: []
        )

        messages.append(userMessage)

        // TODO: Send to KMP shared business logic
        // kmpViewModel.sendMessage(text)

        // Simulate response (replace with actual KMP call)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            self?.simulateResponse(to: text)
        }
    }

    /// Sets RAG enabled state
    /// - Parameter enabled: Whether RAG should be enabled
    func setRagEnabled(_ enabled: Bool) {
        ragEnabled = enabled
        // TODO: Update KMP shared state
        // kmpViewModel.setRagEnabled(enabled)
    }

    /// Sets selected document IDs for RAG
    /// - Parameter documentIds: List of document IDs
    func setSelectedDocuments(_ documentIds: [String]) {
        selectedDocumentIds = documentIds
        // TODO: Update KMP shared state
        // kmpViewModel.setSelectedDocuments(documentIds)
    }

    /// Sets RAG similarity threshold
    /// - Parameter threshold: Similarity threshold (0.5-1.0)
    func setRagThreshold(_ threshold: Float) {
        ragThreshold = threshold
        // TODO: Update KMP shared state
        // kmpViewModel.setRagThreshold(threshold)
    }

    /// Activates teach mode for a message
    /// - Parameter messageId: The message ID to teach
    func activateTeachMode(_ messageId: String) {
        currentTeachMessageId = messageId
        showTeachBottomSheet = true
        // TODO: Load candidate intents from KMP
        // candidateIntents = kmpViewModel.getCandidateIntents()
    }

    /// Dismisses teach bottom sheet
    func dismissTeachBottomSheet() {
        showTeachBottomSheet = false
        currentTeachMessageId = nil
    }

    /// Handles teaching AVA a new intent
    /// - Parameters:
    ///   - messageId: The message ID
    ///   - intent: The intent to teach
    func handleTeachAva(messageId: String, intent: String) {
        // TODO: Send to KMP shared business logic
        // kmpViewModel.handleTeachAva(messageId: messageId, intent: intent)
        dismissTeachBottomSheet()
    }

    /// Clears error message
    func clearError() {
        errorMessage = nil
    }

    /// Loads more messages (pagination)
    func loadMoreMessages() {
        guard hasMoreMessages && !isLoading else { return }

        isLoading = true
        // TODO: Load from KMP shared business logic
        // kmpViewModel.loadMoreMessages()

        // Simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            self?.isLoading = false
        }
    }

    /// Gets current message being taught
    func getCurrentTeachMessage() -> Message? {
        guard let messageId = currentTeachMessageId else { return nil }
        return messages.first { $0.id == messageId }
    }

    // MARK: - Private Methods

    private func setupSampleData() {
        // Sample conversation for testing
        messages = [
            Message(
                id: "1",
                content: "Turn on the lights",
                role: .user,
                timestamp: Date().timeIntervalSince1970 * 1000 - 180000,
                confidence: nil,
                intent: nil,
                sourceCitations: []
            ),
            Message(
                id: "2",
                content: "I'll control the lights.",
                role: .assistant,
                timestamp: Date().timeIntervalSince1970 * 1000 - 120000,
                confidence: 0.85,
                intent: "lights.control",
                sourceCitations: []
            )
        ]
    }

    private func simulateResponse(to userMessage: String) {
        let response = Message(
            id: UUID().uuidString,
            content: "I understand you said: \(userMessage)",
            role: .assistant,
            timestamp: Date().timeIntervalSince1970 * 1000,
            confidence: 0.75,
            intent: "general.response",
            sourceCitations: ragEnabled && !selectedDocumentIds.isEmpty ? [
                SourceCitation(
                    documentTitle: "User Manual v2.1",
                    pageNumber: 5,
                    similarityPercent: 92
                ),
                SourceCitation(
                    documentTitle: "Technical Specifications",
                    pageNumber: nil,
                    similarityPercent: 87
                )
            ] : []
        )

        messages.append(response)
        isLoading = false
    }

    private func setupKMPBindings() {
        // TODO: Bind to KMP shared ViewModel StateFlows
        // This will bridge Kotlin StateFlow to Swift @Published properties
    }
}

// MARK: - Supporting Models

/// Message model for iOS
struct Message: Identifiable, Equatable {
    let id: String
    let content: String
    let role: MessageRole
    let timestamp: Double // Unix timestamp in milliseconds
    let confidence: Float?
    let intent: String?
    let sourceCitations: [SourceCitation]
}

/// Message role enum
enum MessageRole {
    case user
    case assistant
    case system
}

/// Conversation model for history
struct Conversation: Identifiable {
    let id: String
    let title: String
    let messageCount: Int
    let updatedAt: Double // Unix timestamp
}

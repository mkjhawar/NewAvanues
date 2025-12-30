// filename: Universal/AVA/Features/Chat/src/iosMain/swift/ui/ChatView.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import SwiftUI

/// Main chat screen for AVA AI conversation interface (iOS).
///
/// iOS SwiftUI equivalent of Android's ChatScreen composable.
///
/// Displays a conversation with message history, text input, and RAG integration.
/// This is the primary user interaction point for communicating with AVA on iOS.
///
/// Features:
/// - Message bubbles with source citations
/// - RAG active indicator
/// - Confidence badges
/// - Voice input support (Phase 1.2)
/// - Teach AVA mode (Phase 3)
/// - Message history (Phase 4)
/// - Pagination (Phase 5)
struct ChatView: View {

    // MARK: - Properties

    @StateObject private var viewModel = ChatViewModel()
    @State private var messageText = ""
    @State private var showSettingsSheet = false
    @FocusState private var isInputFocused: Bool

    // MARK: - Body

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Error banner
                if let error = viewModel.errorMessage {
                    errorBanner(message: error)
                }

                // Message list
                messageList

                // RAG active indicator (above input field)
                if viewModel.ragEnabled && !viewModel.selectedDocumentIds.isEmpty {
                    ragActiveIndicator
                }

                // Message input field
                messageInputField
            }
            .navigationTitle("AVA")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    teachAvaButton
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    settingsButton
                }
            }
            .sheet(isPresented: $showSettingsSheet) {
                settingsSheet
            }
        }
    }

    // MARK: - Subviews

    private var messageList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 8) {
                    // Load more button (pagination)
                    if viewModel.hasMoreMessages && viewModel.totalMessageCount > viewModel.messages.count {
                        loadMoreButton
                    }

                    // Messages
                    ForEach(viewModel.messages) { message in
                        MessageBubbleView(
                            message: message,
                            onConfirm: message.confidence != nil && message.confidence! >= 0.5 && message.confidence! < 0.7 ? {
                                // Handle confirmation
                            } : nil,
                            onTeachAva: message.confidence != nil && message.confidence! < 0.5 ? {
                                viewModel.activateTeachMode(message.id)
                            } : nil,
                            onLongPress: {
                                viewModel.activateTeachMode(message.id)
                            }
                        )
                        .id(message.id)
                    }
                }
                .padding(.vertical, 8)
            }
            .onChange(of: viewModel.messages.count) { _ in
                // Auto-scroll to bottom on new message
                if let lastMessage = viewModel.messages.last {
                    withAnimation {
                        proxy.scrollTo(lastMessage.id, anchor: .bottom)
                    }
                }
            }
        }
    }

    private var loadMoreButton: some View {
        Button {
            viewModel.loadMoreMessages()
        } label: {
            HStack {
                if viewModel.isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                }

                let remaining = max(0, viewModel.totalMessageCount - viewModel.messages.count)
                Text(viewModel.isLoading ? "Loading..." : "Load More (\(remaining) older messages)")
                    .font(.callout)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
        }
        .buttonStyle(.bordered)
        .padding(.horizontal, 16)
        .disabled(viewModel.isLoading)
    }

    private var ragActiveIndicator: some View {
        HStack(spacing: 8) {
            Image(systemName: "sparkles")
                .font(.system(size: 14))

            Text("RAG: \(viewModel.selectedDocumentIds.count) doc\(viewModel.selectedDocumentIds.count == 1 ? "" : "s")")
                .font(.caption)

            Spacer()
        }
        .foregroundColor(.accentColor)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.accentColor.opacity(0.1))
    }

    private var messageInputField: some View {
        HStack(spacing: 12) {
            // Voice input button
            Button {
                // TODO: Implement voice input
            } label: {
                Image(systemName: "mic.fill")
                    .font(.system(size: 20))
            }
            .buttonStyle(.bordered)
            .tint(.accentColor)
            .frame(width: 48, height: 48)
            .disabled(viewModel.isLoading)

            // Text field
            TextField("Type a message...", text: $messageText, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .lineLimit(1...4)
                .focused($isInputFocused)
                .disabled(viewModel.isLoading)
                .onSubmit {
                    sendMessage()
                }

            // Send button
            Button {
                sendMessage()
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 28))
            }
            .buttonStyle(.borderless)
            .tint(messageText.trimmingCharacters(in: .whitespaces).isEmpty ? .gray : .accentColor)
            .disabled(viewModel.isLoading || messageText.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
        .overlay(
            Rectangle()
                .frame(height: 0.5)
                .foregroundColor(Color(.separator)),
            alignment: .top
        )
    }

    private var teachAvaButton: some View {
        Button {
            if let lastMessage = viewModel.messages.last {
                viewModel.activateTeachMode(lastMessage.id)
            }
        } label: {
            Image(systemName: "graduationcap.fill")
                .font(.system(size: 16))
        }
        .disabled(viewModel.messages.isEmpty)
        .accessibilityLabel("Teach AVA - Train AVA with new intents")
    }

    private var settingsButton: some View {
        Button {
            showSettingsSheet = true
        } label: {
            Image(systemName: "gearshape.fill")
                .font(.system(size: 16))
        }
        .accessibilityLabel("Settings")
    }

    private func errorBanner(message: String) -> some View {
        HStack {
            Text(message)
                .font(.callout)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, alignment: .leading)

            Button("Dismiss") {
                viewModel.clearError()
            }
            .foregroundColor(.white)
            .font(.callout)
        }
        .padding(16)
        .background(Color.red)
    }

    private var settingsSheet: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // RAG Settings
                    RAGSettingsView(
                        ragEnabled: $viewModel.ragEnabled,
                        selectedDocumentCount: viewModel.selectedDocumentIds.count,
                        ragThreshold: $viewModel.ragThreshold,
                        onSelectDocuments: {
                            // TODO: Show document selector
                        }
                    )

                    Spacer()
                }
                .padding()
            }
            .navigationTitle("Chat Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        showSettingsSheet = false
                    }
                }
            }
        }
    }

    // MARK: - Actions

    private func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }

        viewModel.sendMessage(text)
        messageText = ""
        isInputFocused = false
    }
}

// MARK: - Previews

struct ChatView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            ChatView()
                .previewDisplayName("Light Mode")

            ChatView()
                .preferredColorScheme(.dark)
                .previewDisplayName("Dark Mode")
        }
    }
}

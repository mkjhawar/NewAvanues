// filename: Universal/AVA/Features/Chat/src/iosMain/swift/ui/settings/RAGSettingsView.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import SwiftUI

/// RAG Settings View for Chat UI (Phase 2 - Task 1)
///
/// iOS SwiftUI equivalent of Android's RAGSettingsSection composable.
///
/// Displays RAG configuration options:
/// - Enable/disable toggle
/// - Document selector button
/// - Similarity threshold slider
///
/// Design:
/// - Material 3-inspired iOS design
/// - Proper spacing and labels
/// - Visual feedback for user actions
/// - Disabled state when RAG is off
struct RAGSettingsView: View {

    // MARK: - Properties

    @Binding var ragEnabled: Bool
    let selectedDocumentCount: Int
    @Binding var ragThreshold: Float
    let onSelectDocuments: () -> Void

    // MARK: - Body

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section header
            sectionHeader

            Divider()

            // Enable/Disable toggle
            enableToggle

            // Document selector button
            documentSelectorButton

            // Similarity threshold slider
            thresholdSlider

            // Info message when RAG enabled but no documents selected
            if ragEnabled && selectedDocumentCount == 0 {
                infoMessage
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(.systemGray6))
        )
    }

    // MARK: - Subviews

    private var sectionHeader: some View {
        Text("RAG (Retrieval-Augmented Generation)")
            .font(.system(size: 17, weight: .bold))
            .foregroundColor(.primary)
    }

    private var enableToggle: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Enable RAG")
                    .font(.body)
                    .foregroundColor(.primary)

                Text("Use documents to enhance responses")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Toggle("", isOn: $ragEnabled)
                .labelsHidden()
        }
    }

    private var documentSelectorButton: some View {
        Button(action: onSelectDocuments) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Select Documents")
                        .font(.body)
                        .foregroundColor(ragEnabled ? .primary : .secondary)

                    Text(selectedDocumentCount > 0 ?
                         "\(selectedDocumentCount) document\(selectedDocumentCount == 1 ? "" : "s") selected" :
                         "No documents selected")
                        .font(.caption)
                        .foregroundColor(selectedDocumentCount > 0 ? .accentColor : .secondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary)
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(ragEnabled ? Color.accentColor.opacity(0.5) : Color(.systemGray4), lineWidth: 1)
            )
        }
        .disabled(!ragEnabled)
        .opacity(ragEnabled ? 1.0 : 0.6)
    }

    private var thresholdSlider: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Similarity Threshold")
                    .font(.body)
                    .foregroundColor(ragEnabled ? .primary : .secondary)

                Spacer()

                Text(String(format: "%.2f", ragThreshold))
                    .font(.body)
                    .fontWeight(.bold)
                    .foregroundColor(ragEnabled ? .accentColor : .secondary)
            }

            Slider(
                value: $ragThreshold,
                in: 0.5...1.0,
                step: 0.05
            )
            .disabled(!ragEnabled)
            .tint(.accentColor)

            Text("Higher threshold = more relevant results only")
                .font(.caption)
                .foregroundColor(ragEnabled ? .secondary : Color(.systemGray3))
        }
    }

    private var infoMessage: some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle.fill")
                .font(.system(size: 16))
                .foregroundColor(.blue)

            Text("Please select at least one document to enable RAG")
                .font(.caption)
                .foregroundColor(.primary)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.blue.opacity(0.1))
        )
    }
}

// MARK: - Previews

struct RAGSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            // RAG disabled
            RAGSettingsView(
                ragEnabled: .constant(false),
                selectedDocumentCount: 0,
                ragThreshold: .constant(0.75),
                onSelectDocuments: {}
            )
            .padding()
            .previewDisplayName("RAG Disabled")

            // RAG enabled, no documents
            RAGSettingsView(
                ragEnabled: .constant(true),
                selectedDocumentCount: 0,
                ragThreshold: .constant(0.75),
                onSelectDocuments: {}
            )
            .padding()
            .previewDisplayName("RAG Enabled - No Docs")

            // RAG enabled with documents
            RAGSettingsView(
                ragEnabled: .constant(true),
                selectedDocumentCount: 3,
                ragThreshold: .constant(0.80),
                onSelectDocuments: {}
            )
            .padding()
            .previewDisplayName("RAG Enabled - 3 Docs")

            // Dark mode
            RAGSettingsView(
                ragEnabled: .constant(true),
                selectedDocumentCount: 2,
                ragThreshold: .constant(0.70),
                onSelectDocuments: {}
            )
            .padding()
            .preferredColorScheme(.dark)
            .previewDisplayName("Dark Mode")
        }
    }
}

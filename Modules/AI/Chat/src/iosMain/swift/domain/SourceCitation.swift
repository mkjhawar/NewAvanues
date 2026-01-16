// filename: Universal/AVA/Features/Chat/src/iosMain/swift/domain/SourceCitation.swift
// created: 2025-11-22
// author: iOS RAG Chat Integration Specialist (Agent 1)
// © Augmentalis Inc, Intelligent Devices LLC

import Foundation

/// Represents a source citation for UI display in iOS.
///
/// Used in MessageBubbleView to show which documents were used to generate response.
/// This is the Swift equivalent of the Kotlin SourceCitation data class.
///
/// - Parameters:
///   - documentTitle: Title of the source document
///   - pageNumber: Page number within document (nil if not available)
///   - similarityPercent: Similarity score as percentage (0-100)
struct SourceCitation: Identifiable, Equatable, Hashable {
    let id = UUID()
    let documentTitle: String
    let pageNumber: Int?
    let similarityPercent: Int

    /// Formats citation for display in UI.
    ///
    /// Examples:
    /// - "User Manual (Page 5) - 87%"
    /// - "FAQ Document - 92%"
    func format() -> String {
        let pageInfo = pageNumber != nil ? " (Page \(pageNumber!))" : ""
        return "\(documentTitle)\(pageInfo) - \(similarityPercent)%"
    }
}

/// Extension to create SourceCitation from Kotlin interop
extension SourceCitation {
    /// Creates SourceCitation from KMP shared code
    /// Note: This will be implemented when KMP bindings are ready
    static func from(kmpCitation: Any) -> SourceCitation? {
        // TODO: Implement KMP interop mapping
        // For now, return nil - will be connected to shared Kotlin code
        return nil
    }
}

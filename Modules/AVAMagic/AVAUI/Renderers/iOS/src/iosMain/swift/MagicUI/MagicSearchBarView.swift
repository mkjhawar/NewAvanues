import SwiftUI

/**
 * MagicSearchBarView - iOS Search Bar
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicSearchBarView: View {
    @Binding var text: String
    let placeholder: String
    let onSubmit: (() -> Void)?

    public init(text: Binding<String>, placeholder: String = "Search", onSubmit: (() -> Void)? = nil) {
        self._text = text
        self.placeholder = placeholder
        self.onSubmit = onSubmit
    }

    public var body: some View {
        HStack {
            Image(systemName: "magnifyingglass").foregroundColor(.secondary)
            TextField(placeholder, text: $text)
                .onSubmit { onSubmit?() }
            if !text.isEmpty {
                Button(action: { text = "" }) {
                    Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                }
            }
        }
        .padding(8)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(10)
    }
}

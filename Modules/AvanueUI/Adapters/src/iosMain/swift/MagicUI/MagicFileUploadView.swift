import SwiftUI

/**
 * MagicFileUploadView - iOS File Picker
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicFileUploadView: View {
    @State private var isPresented = false
    let label: String
    let onSelect: (URL) -> Void

    public init(label: String = "Choose File", onSelect: @escaping (URL) -> Void) {
        self.label = label
        self.onSelect = onSelect
    }

    public var body: some View {
        Button(action: { isPresented = true }) {
            HStack {
                Image(systemName: "doc.fill")
                Text(label)
            }
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.accentColor)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        .fileImporter(isPresented: $isPresented, allowedContentTypes: [.item]) { result in
            if case .success(let url) = result {
                onSelect(url)
            }
        }
    }
}

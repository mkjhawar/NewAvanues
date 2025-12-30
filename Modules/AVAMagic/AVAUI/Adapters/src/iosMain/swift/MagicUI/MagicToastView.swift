import SwiftUI

/**
 * MagicToastView - iOS Toast Notification
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicToastView: View {
    let message: String
    let icon: String?
    let style: ToastStyle

    public init(message: String, icon: String? = nil, style: ToastStyle = .info) {
        self.message = message
        self.icon = icon
        self.style = style
    }

    public var body: some View {
        HStack(spacing: 12) {
            if let iconName = icon {
                Image(systemName: iconName)
                    .font(.body)
            }
            Text(message)
                .font(.subheadline)
        }
        .padding()
        .background(backgroundColor)
        .foregroundColor(.white)
        .cornerRadius(10)
        .shadow(radius: 10)
    }

    private var backgroundColor: Color {
        switch style {
        case .success: return .green
        case .error: return .red
        case .warning: return .orange
        case .info: return .blue
        }
    }
}

public enum ToastStyle {
    case success, error, warning, info
}

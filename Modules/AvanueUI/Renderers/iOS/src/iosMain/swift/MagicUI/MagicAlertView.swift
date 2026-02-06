import SwiftUI

/**
 * MagicAlertView - Native iOS Alert
 *
 * Native SwiftUI alert implementation for IDEAMagic framework.
 * Renders AlertComponent from Core as native iOS Alert.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicAlertView: ViewModifier {
    @Binding var isPresented: Bool
    let title: String
    let message: String?
    let primaryButton: AlertButton?
    let secondaryButton: AlertButton?

    public func body(content: Content) -> some View {
        content.alert(isPresented: $isPresented) {
            if let primary = primaryButton, let secondary = secondaryButton {
                return Alert(
                    title: Text(title),
                    message: message.map { Text($0) },
                    primaryButton: .default(Text(primary.title), action: primary.action),
                    secondaryButton: .cancel(Text(secondary.title), action: secondary.action)
                )
            } else if let primary = primaryButton {
                return Alert(
                    title: Text(title),
                    message: message.map { Text($0) },
                    dismissButton: .default(Text(primary.title), action: primary.action)
                )
            } else {
                return Alert(
                    title: Text(title),
                    message: message.map { Text($0) }
                )
            }
        }
    }
}

public struct AlertButton {
    let title: String
    let action: () -> Void

    public init(title: String, action: @escaping () -> Void = {}) {
        self.title = title
        self.action = action
    }
}

extension View {
    public func magicAlert(
        isPresented: Binding<Bool>,
        title: String,
        message: String? = nil,
        primaryButton: AlertButton? = nil,
        secondaryButton: AlertButton? = nil
    ) -> some View {
        self.modifier(MagicAlertView(
            isPresented: isPresented,
            title: title,
            message: message,
            primaryButton: primaryButton,
            secondaryButton: secondaryButton
        ))
    }
}

#if DEBUG
struct MagicAlertView_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            Button("Show Alert") {}
                .magicAlert(
                    isPresented: .constant(true),
                    title: "Delete Item",
                    message: "Are you sure?",
                    primaryButton: AlertButton(title: "Delete"),
                    secondaryButton: AlertButton(title: "Cancel")
                )
        }
    }
}
#endif

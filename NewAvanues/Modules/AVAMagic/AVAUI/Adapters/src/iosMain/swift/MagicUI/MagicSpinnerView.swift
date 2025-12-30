import SwiftUI

/**
 * MagicSpinnerView - Native iOS Activity Indicator
 *
 * Native SwiftUI spinner implementation for IDEAMagic framework.
 * Renders SpinnerComponent from Core as native iOS ProgressView (circular).
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicSpinnerView: View {
    let size: SpinnerSize
    let tintColor: Color?

    public init(
        size: SpinnerSize = .medium,
        tintColor: Color? = nil
    ) {
        self.size = size
        self.tintColor = tintColor
    }

    public var body: some View {
        ProgressView()
            .progressViewStyle(CircularProgressViewStyle(tint: tintColor ?? .accentColor))
            .scaleEffect(sizeScale)
    }

    private var sizeScale: CGFloat {
        switch size {
        case .small: return 0.7
        case .medium: return 1.0
        case .large: return 1.5
        }
    }
}

public enum SpinnerSize {
    case small, medium, large
}

#if DEBUG
struct MagicSpinnerView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 24) {
            MagicSpinnerView(size: .small)
            MagicSpinnerView(size: .medium)
            MagicSpinnerView(size: .large)
            MagicSpinnerView(size: .medium, tintColor: .blue)
        }
        .padding()
    }
}
#endif

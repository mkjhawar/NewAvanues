import SwiftUI

/**
 * MagicRatingView - iOS Star Rating
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicRatingView: View {
    @Binding var rating: Int
    let maxRating: Int
    let enabled: Bool

    public init(rating: Binding<Int>, maxRating: Int = 5, enabled: Bool = true) {
        self._rating = rating
        self.maxRating = maxRating
        self.enabled = enabled
    }

    public var body: some View {
        HStack(spacing: 4) {
            ForEach(1...maxRating, id: \.self) { index in
                Button(action: { if enabled { rating = index } }) {
                    Image(systemName: index <= rating ? "star.fill" : "star")
                        .foregroundColor(index <= rating ? .yellow : .gray)
                }
                .buttonStyle(PlainButtonStyle())
                .disabled(!enabled)
            }
        }
    }
}

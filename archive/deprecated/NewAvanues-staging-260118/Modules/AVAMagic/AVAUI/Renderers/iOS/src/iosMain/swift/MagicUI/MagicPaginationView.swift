import SwiftUI

/**
 * MagicPaginationView - iOS Page Indicator
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicPaginationView: View {
    @Binding var currentPage: Int
    let totalPages: Int
    let showPageNumbers: Bool
    let onPageChange: ((Int) -> Void)?

    public init(
        currentPage: Binding<Int>,
        totalPages: Int,
        showPageNumbers: Bool = false,
        onPageChange: ((Int) -> Void)? = nil
    ) {
        self._currentPage = currentPage
        self.totalPages = totalPages
        self.showPageNumbers = showPageNumbers
        self.onPageChange = onPageChange
    }

    public var body: some View {
        HStack(spacing: 12) {
            // Previous button
            Button(action: previousPage) {
                Image(systemName: "chevron.left")
                    .foregroundColor(currentPage > 0 ? .accentColor : .gray)
            }
            .disabled(currentPage == 0)

            if showPageNumbers {
                // Page numbers
                HStack(spacing: 8) {
                    ForEach(0..<totalPages, id: \.self) { page in
                        Button(action: { goToPage(page) }) {
                            Text("\(page + 1)")
                                .font(.system(size: 14, weight: page == currentPage ? .bold : .regular))
                                .foregroundColor(page == currentPage ? .white : .accentColor)
                                .frame(width: 32, height: 32)
                                .background(page == currentPage ? Color.accentColor : Color.clear)
                                .cornerRadius(16)
                        }
                    }
                }
            } else {
                // Dot indicators
                HStack(spacing: 8) {
                    ForEach(0..<totalPages, id: \.self) { page in
                        Circle()
                            .fill(page == currentPage ? Color.accentColor : Color.gray.opacity(0.4))
                            .frame(width: page == currentPage ? 10 : 8, height: page == currentPage ? 10 : 8)
                            .onTapGesture { goToPage(page) }
                    }
                }
            }

            // Next button
            Button(action: nextPage) {
                Image(systemName: "chevron.right")
                    .foregroundColor(currentPage < totalPages - 1 ? .accentColor : .gray)
            }
            .disabled(currentPage == totalPages - 1)
        }
        .padding()
    }

    private func previousPage() {
        guard currentPage > 0 else { return }
        currentPage -= 1
        onPageChange?(currentPage)
    }

    private func nextPage() {
        guard currentPage < totalPages - 1 else { return }
        currentPage += 1
        onPageChange?(currentPage)
    }

    private func goToPage(_ page: Int) {
        currentPage = page
        onPageChange?(page)
    }
}

// MARK: - Preview
#if DEBUG
struct MagicPaginationView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 40) {
            PaginationPreview(showPageNumbers: false)
            PaginationPreview(showPageNumbers: true)
        }
        .padding()
    }

    struct PaginationPreview: View {
        @State private var currentPage = 0
        let showPageNumbers: Bool

        var body: some View {
            VStack {
                Text("Page \(currentPage + 1) of 5")
                    .font(.headline)
                MagicPaginationView(
                    currentPage: $currentPage,
                    totalPages: 5,
                    showPageNumbers: showPageNumbers
                )
            }
        }
    }
}
#endif

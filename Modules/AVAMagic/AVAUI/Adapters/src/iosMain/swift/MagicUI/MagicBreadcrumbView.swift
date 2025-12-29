import SwiftUI

/**
 * MagicBreadcrumbView - iOS Breadcrumb Navigation
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicBreadcrumbView: View {
    let items: [BreadcrumbItem]
    let onItemTap: ((Int) -> Void)?

    public init(items: [BreadcrumbItem], onItemTap: ((Int) -> Void)? = nil) {
        self.items = items
        self.onItemTap = onItemTap
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(0..<items.count, id: \.self) { index in
                    Button(action: { onItemTap?(index) }) {
                        HStack(spacing: 4) {
                            if let icon = items[index].icon {
                                Image(systemName: icon)
                                    .font(.system(size: 14))
                            }
                            Text(items[index].label)
                                .font(.system(size: 14, weight: index == items.count - 1 ? .semibold : .regular))
                        }
                        .foregroundColor(index == items.count - 1 ? .primary : .accentColor)
                    }
                    .disabled(index == items.count - 1)

                    if index < items.count - 1 {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal)
        }
        .frame(height: 40)
    }
}

public struct BreadcrumbItem {
    let label: String
    let icon: String?

    public init(label: String, icon: String? = nil) {
        self.label = label
        self.icon = icon
    }
}

// MARK: - Preview
#if DEBUG
struct MagicBreadcrumbView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            MagicBreadcrumbView(
                items: [
                    BreadcrumbItem(label: "Home", icon: "house"),
                    BreadcrumbItem(label: "Documents"),
                    BreadcrumbItem(label: "Projects"),
                    BreadcrumbItem(label: "VoiceAvanue")
                ],
                onItemTap: { index in
                    print("Tapped item at index: \(index)")
                }
            )

            MagicBreadcrumbView(
                items: [
                    BreadcrumbItem(label: "Settings", icon: "gearshape"),
                    BreadcrumbItem(label: "Privacy"),
                    BreadcrumbItem(label: "Location Services")
                ]
            )

            MagicBreadcrumbView(
                items: [
                    BreadcrumbItem(label: "Home"),
                    BreadcrumbItem(label: "Category"),
                    BreadcrumbItem(label: "Subcategory"),
                    BreadcrumbItem(label: "Item Details")
                ]
            )
        }
        .padding()
    }
}
#endif

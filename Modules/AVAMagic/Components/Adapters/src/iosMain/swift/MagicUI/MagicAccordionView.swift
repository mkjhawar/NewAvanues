import SwiftUI

/**
 * MagicAccordionView - iOS Expandable Accordion
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
public struct MagicAccordionView: View {
    @State private var expandedIndices: Set<Int> = []
    let items: [AccordionItem]
    let allowMultiple: Bool

    public init(items: [AccordionItem], allowMultiple: Bool = false) {
        self.items = items
        self.allowMultiple = allowMultiple
    }

    public var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<items.count, id: \.self) { index in
                VStack(spacing: 0) {
                    // Header
                    Button(action: { toggleItem(index) }) {
                        HStack {
                            if let icon = items[index].icon {
                                Image(systemName: icon)
                                    .foregroundColor(.accentColor)
                            }
                            Text(items[index].title)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .rotationEffect(.degrees(expandedIndices.contains(index) ? 90 : 0))
                                .foregroundColor(.secondary)
                                .animation(.easeInOut(duration: 0.2), value: expandedIndices)
                        }
                        .padding()
                        .background(Color(UIColor.systemBackground))
                    }

                    // Content
                    if expandedIndices.contains(index) {
                        VStack(alignment: .leading, spacing: 0) {
                            Divider()
                            items[index].content
                                .padding()
                                .background(Color(UIColor.secondarySystemBackground))
                        }
                        .transition(.asymmetric(
                            insertion: .opacity.combined(with: .move(edge: .top)),
                            removal: .opacity.combined(with: .move(edge: .top))
                        ))
                    }

                    if index < items.count - 1 {
                        Divider()
                    }
                }
            }
        }
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
    }

    private func toggleItem(_ index: Int) {
        withAnimation(.easeInOut(duration: 0.3)) {
            if expandedIndices.contains(index) {
                expandedIndices.remove(index)
            } else {
                if !allowMultiple {
                    expandedIndices.removeAll()
                }
                expandedIndices.insert(index)
            }
        }
    }
}

public struct AccordionItem {
    let title: String
    let icon: String?
    let content: AnyView

    public init<Content: View>(title: String, icon: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.icon = icon
        self.content = AnyView(content())
    }
}

// MARK: - Preview
#if DEBUG
struct MagicAccordionView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 20) {
            Text("Single Expansion")
                .font(.headline)

            MagicAccordionView(
                items: [
                    AccordionItem(title: "Account Settings", icon: "person.circle") {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Manage your account preferences")
                                .foregroundColor(.secondary)
                            Button("Edit Profile") {}
                                .padding(8)
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                    },
                    AccordionItem(title: "Privacy", icon: "lock.shield") {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Control your privacy settings")
                                .foregroundColor(.secondary)
                            Toggle("Allow Notifications", isOn: .constant(true))
                            Toggle("Share Analytics", isOn: .constant(false))
                        }
                    },
                    AccordionItem(title: "Help & Support", icon: "questionmark.circle") {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Get help with your account")
                                .foregroundColor(.secondary)
                            Button("Contact Support") {}
                            Button("FAQ") {}
                        }
                    }
                ],
                allowMultiple: false
            )
            .padding()

            Text("Multiple Expansion")
                .font(.headline)

            MagicAccordionView(
                items: [
                    AccordionItem(title: "Features", icon: "star") {
                        Text("Voice recognition, AI assistance, and more")
                            .foregroundColor(.secondary)
                    },
                    AccordionItem(title: "Pricing", icon: "dollarsign.circle") {
                        Text("Flexible plans starting at $9.99/month")
                            .foregroundColor(.secondary)
                    }
                ],
                allowMultiple: true
            )
            .padding()
        }
        .padding()
    }
}
#endif

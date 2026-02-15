import SwiftUI

/// Browser history view.
///
/// Displays visited pages chronologically, grouped by date.
/// Will be backed by KMP HistoryRepository (SQLDelight) once Phase 2 enables iOS targets.
struct HistoryView: View {
    @StateObject private var store = HistoryStore()
    @State private var searchText: String = ""
    @Environment(\.dismiss) private var dismiss

    var filteredEntries: [HistoryEntry] {
        if searchText.isEmpty {
            return store.entries
        }
        let query = searchText.lowercased()
        return store.entries.filter {
            $0.title.lowercased().contains(query) || $0.url.lowercased().contains(query)
        }
    }

    /// Grouped by date.
    var groupedEntries: [(String, [HistoryEntry])] {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none

        let grouped = Dictionary(grouping: filteredEntries) { entry in
            formatter.string(from: entry.visitedAt)
        }
        return grouped.sorted { lhs, rhs in
            (lhs.value.first?.visitedAt ?? .distantPast) > (rhs.value.first?.visitedAt ?? .distantPast)
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if store.entries.isEmpty {
                    emptyState
                } else {
                    historyList
                }
            }
            .searchable(text: $searchText, prompt: "Search history")
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear All", role: .destructive) {
                        store.entries.removeAll()
                        store.save()
                    }
                    .disabled(store.entries.isEmpty)
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No History")
                .font(.title3)
            Text("Pages you visit will appear here")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var historyList: some View {
        List {
            ForEach(groupedEntries, id: \.0) { dateString, entries in
                Section(dateString) {
                    ForEach(entries) { entry in
                        Button(action: { onSelect(entry) }) {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(entry.title.isEmpty ? entry.url : entry.title)
                                    .font(.body)
                                    .lineLimit(1)
                                HStack {
                                    Text(entry.url)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                        .lineLimit(1)
                                    Spacer()
                                    Text(entry.visitedAt, style: .time)
                                        .font(.caption2)
                                        .foregroundStyle(.tertiary)
                                }
                            }
                        }
                    }
                    .onDelete { indexSet in
                        let entriesToRemove = indexSet.map { entries[$0] }
                        store.entries.removeAll { entry in
                            entriesToRemove.contains { $0.id == entry.id }
                        }
                        store.save()
                    }
                }
            }
        }
    }

    var onNavigate: ((String) -> Void)?

    private func onSelect(_ entry: HistoryEntry) {
        onNavigate?(entry.url)
        dismiss()
    }
}

// MARK: - Data Model

struct HistoryEntry: Identifiable, Codable {
    let id: UUID
    var url: String
    var title: String
    var visitedAt: Date

    init(id: UUID = UUID(), url: String, title: String, visitedAt: Date = Date()) {
        self.id = id
        self.url = url
        self.title = title
        self.visitedAt = visitedAt
    }
}

// MARK: - Local Storage (Phase 2 replaces with KMP SQLDelight)

final class HistoryStore: ObservableObject {
    @Published var entries: [HistoryEntry] = []

    private let key = "avanues_history"
    private let maxEntries = 1000

    init() {
        load()
    }

    func addEntry(url: String, title: String) {
        let entry = HistoryEntry(url: url, title: title)
        entries.insert(entry, at: 0)
        if entries.count > maxEntries {
            entries = Array(entries.prefix(maxEntries))
        }
        save()
    }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([HistoryEntry].self, from: data) else { return }
        entries = decoded
    }

    func save() {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

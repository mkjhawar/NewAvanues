import SwiftUI

/// Bookmarks/favorites browser view.
///
/// Displays saved bookmarks in a list or grid.
/// Will be backed by KMP FavoriteRepository (SQLDelight) once Phase 2 enables iOS targets.
/// For now, uses local UserDefaults-based storage.
struct BookmarkView: View {
    @StateObject private var store = BookmarkStore()
    @State private var showAddSheet = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if store.bookmarks.isEmpty {
                    emptyState
                } else {
                    bookmarkList
                }
            }
            .navigationTitle("Bookmarks")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showAddSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showAddSheet) {
                AddBookmarkSheet(store: store, isPresented: $showAddSheet)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "bookmark")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No Bookmarks Yet")
                .font(.title3)
            Text("Tap + to add a bookmark")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var bookmarkList: some View {
        List {
            ForEach(store.bookmarks) { bookmark in
                Button(action: { onSelect(bookmark) }) {
                    HStack {
                        Image(systemName: "globe")
                            .foregroundStyle(.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(bookmark.title)
                                .font(.body)
                                .lineLimit(1)
                            Text(bookmark.url)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }
                    }
                }
            }
            .onDelete { indexSet in
                store.bookmarks.remove(atOffsets: indexSet)
                store.save()
            }
        }
    }

    /// Selection handler — navigation to URL handled by parent.
    var onNavigate: ((String) -> Void)?

    private func onSelect(_ bookmark: Bookmark) {
        onNavigate?(bookmark.url)
        dismiss()
    }
}

// MARK: - Add Bookmark Sheet

struct AddBookmarkSheet: View {
    @ObservedObject var store: BookmarkStore
    @Binding var isPresented: Bool
    @State private var title: String = ""
    @State private var url: String = "https://"

    var body: some View {
        NavigationStack {
            Form {
                TextField("Title", text: $title)
                TextField("URL", text: $url)
                    .keyboardType(.URL)
                    .autocapitalization(.none)
            }
            .navigationTitle("Add Bookmark")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { isPresented = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let bookmark = Bookmark(title: title, url: url)
                        store.bookmarks.append(bookmark)
                        store.save()
                        isPresented = false
                    }
                    .disabled(title.isEmpty || url.isEmpty)
                }
            }
        }
    }
}

// MARK: - Data Model

struct Bookmark: Identifiable, Codable {
    let id: UUID
    var title: String
    var url: String

    init(id: UUID = UUID(), title: String, url: String) {
        self.id = id
        self.title = title
        self.url = url
    }
}

// MARK: - Local Storage (Phase 2 replaces with KMP SQLDelight)

final class BookmarkStore: ObservableObject {
    @Published var bookmarks: [Bookmark] = []

    private let key = "avanues_bookmarks"

    init() {
        load()
    }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([Bookmark].self, from: data) else { return }
        bookmarks = decoded
    }

    func save() {
        guard let data = try? JSONEncoder().encode(bookmarks) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

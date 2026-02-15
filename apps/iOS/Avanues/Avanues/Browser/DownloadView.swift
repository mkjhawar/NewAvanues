import SwiftUI

/// Downloads management view.
///
/// Displays active and completed file downloads from the browser.
/// Uses URLSession download tasks for actual file fetching.
/// Will be backed by KMP DownloadRepository (SQLDelight) once Phase 2 enables iOS targets.
struct DownloadView: View {
    @StateObject private var store = DownloadStore()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if store.downloads.isEmpty {
                    emptyState
                } else {
                    downloadList
                }
            }
            .navigationTitle("Downloads")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear Completed") {
                        store.downloads.removeAll { $0.state == .completed || $0.state == .failed }
                        store.save()
                    }
                    .disabled(store.downloads.allSatisfy { $0.state == .downloading })
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "arrow.down.circle")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("No Downloads")
                .font(.title3)
            Text("Files you download will appear here")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var downloadList: some View {
        List {
            ForEach(store.downloads) { download in
                HStack {
                    fileIcon(for: download.filename)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(download.filename)
                            .font(.body)
                            .lineLimit(1)

                        if download.state == .downloading {
                            ProgressView(value: download.progress)
                                .tint(.blue)
                            Text(formatBytes(download.downloadedBytes) + " / " + formatBytes(download.totalBytes))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        } else {
                            HStack {
                                Text(download.state.label)
                                    .font(.caption)
                                    .foregroundStyle(download.state == .failed ? .red : .secondary)
                                Spacer()
                                Text(formatBytes(download.totalBytes))
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                            }
                        }
                    }
                }
            }
            .onDelete { indexSet in
                store.downloads.remove(atOffsets: indexSet)
                store.save()
            }
        }
    }

    private func fileIcon(for filename: String) -> some View {
        let ext = (filename as NSString).pathExtension.lowercased()
        let icon: String
        switch ext {
        case "pdf": icon = "doc.fill"
        case "jpg", "jpeg", "png", "gif", "webp": icon = "photo"
        case "mp4", "mov", "avi": icon = "film"
        case "mp3", "wav", "m4a": icon = "music.note"
        case "zip", "rar", "7z": icon = "archivebox"
        default: icon = "doc"
        }
        return Image(systemName: icon)
            .font(.title3)
            .foregroundStyle(.secondary)
            .frame(width: 32)
    }

    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useKB, .useMB, .useGB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

// MARK: - Data Model

struct DownloadItem: Identifiable, Codable {
    let id: UUID
    var url: String
    var filename: String
    var totalBytes: Int64
    var downloadedBytes: Int64
    var state: DownloadState
    var createdAt: Date

    var progress: Double {
        guard totalBytes > 0 else { return 0 }
        return Double(downloadedBytes) / Double(totalBytes)
    }

    init(
        id: UUID = UUID(),
        url: String,
        filename: String,
        totalBytes: Int64 = 0,
        state: DownloadState = .downloading,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.url = url
        self.filename = filename
        self.totalBytes = totalBytes
        self.downloadedBytes = 0
        self.state = state
        self.createdAt = createdAt
    }
}

enum DownloadState: String, Codable {
    case downloading
    case completed
    case failed
    case paused

    var label: String {
        switch self {
        case .downloading: return "Downloading..."
        case .completed: return "Completed"
        case .failed: return "Failed"
        case .paused: return "Paused"
        }
    }
}

// MARK: - Local Storage (Phase 2 replaces with KMP SQLDelight)

final class DownloadStore: ObservableObject {
    @Published var downloads: [DownloadItem] = []

    private let key = "avanues_downloads"

    init() {
        load()
    }

    func addDownload(url: String, filename: String, totalBytes: Int64) {
        let item = DownloadItem(url: url, filename: filename, totalBytes: totalBytes)
        downloads.insert(item, at: 0)
        save()
    }

    func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([DownloadItem].self, from: data) else { return }
        downloads = decoded
    }

    func save() {
        guard let data = try? JSONEncoder().encode(downloads) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

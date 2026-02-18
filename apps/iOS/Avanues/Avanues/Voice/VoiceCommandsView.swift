import SwiftUI

/// Voice Commands explorer — displays all available VOS commands.
///
/// Loads commands from the bundled en-US.app.vos file and displays them
/// grouped by category with search support.
/// Mirrors the Android VoiceCommandsTab composable.
struct VoiceCommandsView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    @State private var searchText: String = ""
    @State private var expandedCategories: Set<String> = []
    @State private var commands: [VosCommand] = []

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    /// Group commands by category prefix (before underscore).
    private var groupedCommands: [(category: String, commands: [VosCommand])] {
        let filtered = searchText.isEmpty ? commands : commands.filter { cmd in
            cmd.primaryPhrase.localizedCaseInsensitiveContains(searchText)
                || cmd.synonyms.contains { $0.localizedCaseInsensitiveContains(searchText) }
                || cmd.description.localizedCaseInsensitiveContains(searchText)
                || cmd.actionId.localizedCaseInsensitiveContains(searchText)
        }

        let grouped = Dictionary(grouping: filtered) { cmd -> String in
            categoryName(for: cmd.actionId)
        }

        return grouped
            .sorted { $0.key < $1.key }
            .map { (category: $0.key, commands: $0.value) }
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [theme.background, theme.surface.opacity(0.6), theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Search bar
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(theme.onSurface.opacity(0.5))
                    TextField("Search commands...", text: $searchText)
                        .textFieldStyle(.plain)
                        .font(.subheadline)
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(theme.onSurface.opacity(0.5))
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(theme.surfaceVariant.opacity(0.5))
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                // Command count
                HStack {
                    Text("\(commands.count) commands")
                        .font(.caption)
                        .foregroundStyle(theme.onBackground.opacity(0.5))

                    Spacer()

                    Button(action: toggleAllCategories) {
                        Text(expandedCategories.isEmpty ? "Expand All" : "Collapse All")
                            .font(.caption)
                            .foregroundStyle(theme.primary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 8)

                // Command list
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(groupedCommands, id: \.category) { group in
                            categorySection(
                                title: group.category,
                                commands: group.commands
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 20)
                }
            }
        }
        .navigationTitle("Voice Commands")
        .onAppear { loadCommands() }
    }

    // MARK: - Category Section

    private func categorySection(title: String, commands: [VosCommand]) -> some View {
        VStack(spacing: 0) {
            // Category header
            Button(action: { toggleCategory(title) }) {
                HStack(spacing: 8) {
                    Image(systemName: categoryIcon(for: title))
                        .font(.caption)
                        .foregroundStyle(theme.primary)
                        .frame(width: 20)

                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(theme.onSurface)

                    Text("\(commands.count)")
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Capsule().fill(theme.primary.opacity(0.2)))
                        .foregroundStyle(theme.primary)

                    Spacer()

                    Image(systemName: expandedCategories.contains(title) ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(theme.onSurface.opacity(0.5))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
            }
            .buttonStyle(.plain)

            // Commands (when expanded)
            if expandedCategories.contains(title) {
                ForEach(commands) { cmd in
                    commandRow(cmd)
                }
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(theme.surface.opacity(0.8))
        )
    }

    private func commandRow(_ cmd: VosCommand) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            // Primary phrase
            HStack {
                Text("\u{201C}\(cmd.primaryPhrase)\u{201D}")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(theme.onSurface)

                Spacer()

                Text(cmd.actionId)
                    .font(.caption2)
                    .foregroundStyle(theme.onSurface.opacity(0.3))
                    .fontDesign(.monospaced)
            }

            // Description
            Text(cmd.description)
                .font(.caption)
                .foregroundStyle(theme.onSurface.opacity(0.6))

            // Synonyms
            if !cmd.synonyms.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "text.bubble")
                        .font(.system(size: 9))
                        .foregroundStyle(theme.onSurface.opacity(0.4))

                    Text(cmd.synonyms.joined(separator: ", "))
                        .font(.caption2)
                        .foregroundStyle(theme.onSurface.opacity(0.4))
                        .lineLimit(2)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .overlay(alignment: .top) {
            Divider().opacity(0.3)
        }
    }

    // MARK: - Helpers

    private func toggleCategory(_ title: String) {
        if expandedCategories.contains(title) {
            expandedCategories.remove(title)
        } else {
            expandedCategories.insert(title)
        }
    }

    private func toggleAllCategories() {
        if expandedCategories.isEmpty {
            expandedCategories = Set(groupedCommands.map(\.category))
        } else {
            expandedCategories.removeAll()
        }
    }

    private func categoryName(for actionId: String) -> String {
        let prefix = actionId.components(separatedBy: "_").first ?? actionId
        let names: [String: String] = [
            "nav": "Navigation",
            "media": "Media",
            "sys": "System",
            "voice": "Voice Control",
            "acc": "Accessibility",
            "text": "Text Editing",
            "input": "Input",
            "appctl": "App Control",
            "cam": "Camera",
            "cockpit": "Cockpit",
            "note": "NoteAvanue"
        ]
        return names[prefix] ?? prefix.capitalized
    }

    private func categoryIcon(for name: String) -> String {
        switch name {
        case "Navigation": return "arrow.turn.up.left"
        case "Media": return "play.fill"
        case "System": return "gear"
        case "Voice Control": return "waveform"
        case "Accessibility": return "hand.tap.fill"
        case "Text Editing": return "textformat"
        case "Input": return "keyboard"
        case "App Control": return "square.and.arrow.down"
        case "Camera": return "camera.fill"
        case "Cockpit": return "rectangle.split.2x2"
        case "NoteAvanue": return "note.text"
        default: return "circle.fill"
        }
    }

    // MARK: - VOS File Parsing

    private func loadCommands() {
        guard let url = Bundle.main.url(
            forResource: "en-US.app",
            withExtension: "vos",
            subdirectory: "localization/commands"
        ) else {
            // Try alternate path (assets may be at root)
            loadCommandsFromAlternatePath()
            return
        }

        parseVosFile(at: url)
    }

    private func loadCommandsFromAlternatePath() {
        // Search all bundles for VOS files
        if let url = Bundle.main.url(forResource: "en-US.app", withExtension: "vos") {
            parseVosFile(at: url)
            return
        }

        // Fallback: try to read from app assets directory
        let docsDir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        if let vosDir = docsDir?.appendingPathComponent("vos"),
           let url = try? FileManager.default.contentsOfDirectory(at: vosDir, includingPropertiesForKeys: nil)
               .first(where: { $0.lastPathComponent.contains("en-US.app.vos") }) {
            parseVosFile(at: url)
        }
    }

    private func parseVosFile(at url: URL) {
        guard let content = try? String(contentsOf: url, encoding: .utf8) else { return }

        var parsed: [VosCommand] = []

        for line in content.components(separatedBy: .newlines) {
            let trimmed = line.trimmingCharacters(in: .whitespaces)

            // Skip empty lines, comments, and header
            if trimmed.isEmpty || trimmed.hasPrefix("#") || trimmed.hasPrefix("VOS:") {
                continue
            }

            // Parse pipe-delimited: actionId|primaryPhrase|synonyms|description
            let parts = trimmed.components(separatedBy: "|")
            guard parts.count >= 4 else { continue }

            let actionId = parts[0].trimmingCharacters(in: .whitespaces)
            let primary = parts[1].trimmingCharacters(in: .whitespaces)
            let synonymsStr = parts[2].trimmingCharacters(in: .whitespaces)
            let description = parts[3].trimmingCharacters(in: .whitespaces)

            let synonyms = synonymsStr.isEmpty ? [] : synonymsStr
                .components(separatedBy: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }

            parsed.append(VosCommand(
                actionId: actionId,
                primaryPhrase: primary,
                synonyms: synonyms,
                description: description
            ))
        }

        commands = parsed
    }
}

// MARK: - VOS Command Model

/// A single voice command parsed from a VOS v3.0 file.
struct VosCommand: Identifiable {
    var id: String { actionId }
    let actionId: String
    let primaryPhrase: String
    let synonyms: [String]
    let description: String
}

#Preview {
    NavigationStack {
        VoiceCommandsView()
            .environmentObject(AppState())
    }
    .preferredColorScheme(.dark)
}

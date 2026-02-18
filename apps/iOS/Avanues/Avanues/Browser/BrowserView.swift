import SwiftUI
import WebKit

/// Full-featured browser view with WKWebView, address bar, navigation toolbar,
/// DOM element overlay, and voice command integration.
///
/// Mirrors the Android BrowserScreen composable from WebAvanue module.
/// Uses UIViewRepresentable for WKWebView (native iOS, NOT KMP Compose).
struct BrowserView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.colorScheme) private var colorScheme

    @StateObject private var coordinator = WebViewCoordinator()
    @StateObject private var tabManager: TabManager
    @StateObject private var voiceState = VoiceState()
    @StateObject private var speechManager: SpeechRecognitionManager

    @State private var commandRouter: CommandRouter?
    @State private var addressBarText: String = ""
    @State private var isEditingAddress: Bool = false
    @State private var showOverlay: Bool = false
    @State private var showTabSwitcher: Bool = false
    @State private var webViewFrame: CGRect = .zero

    private var isDark: Bool { colorScheme == .dark }
    private var theme: AvanueColors {
        AvanueThemeBridge.colors(for: appState.palette, isDark: isDark)
    }

    init() {
        let coord = WebViewCoordinator()
        _coordinator = StateObject(wrappedValue: coord)
        _tabManager = StateObject(wrappedValue: TabManager(coordinator: coord))

        let locale = UserDefaults.standard.string(forKey: "voice_locale") ?? "en-US"
        let onDevice = UserDefaults.standard.bool(forKey: "voice_on_device")
        let threshold = UserDefaults.standard.double(forKey: "voice_confidence_threshold")
        let continuous = UserDefaults.standard.bool(forKey: "voice_continuous")
        _speechManager = StateObject(wrappedValue: SpeechRecognitionManager(
            locale: locale,
            preferOnDevice: onDevice,
            confidenceThreshold: Float(threshold > 0 ? threshold : 0.6),
            continuousListening: continuous
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            // Loading progress bar
            if coordinator.isLoading {
                ProgressView(value: coordinator.loadingProgress)
                    .tint(theme.primary)
                    .scaleEffect(y: 0.5)
            }

            // Address bar
            addressBar

            // Web view with element overlay
            ZStack {
                webViewContainer
                    .background(
                        GeometryReader { geo in
                            Color.clear.preference(
                                key: WebViewFrameKey.self,
                                value: geo.frame(in: .local)
                            )
                        }
                    )

                ElementOverlayView(
                    elements: coordinator.domScraper.elements,
                    webViewFrame: webViewFrame,
                    scrollOffset: .zero,
                    showOverlay: $showOverlay,
                    topInset: 0
                )
            }
            .onPreferenceChange(WebViewFrameKey.self) { frame in
                webViewFrame = frame
            }

            // Voice status bar
            VoiceStatusBar(voiceState: voiceState)

            // Bottom toolbar
            bottomToolbar
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showTabSwitcher) {
            TabSwitcherView(tabManager: tabManager, isPresented: $showTabSwitcher)
        }
        .onChange(of: coordinator.currentURL) { newURL in
            if !isEditingAddress {
                addressBarText = displayURL(newURL)
            }
        }
        .onAppear {
            // Wire voice recognition to command router
            if commandRouter == nil {
                let router = CommandRouter(
                    tabManager: tabManager,
                    domScraper: coordinator.domScraper,
                    voiceState: voiceState
                )
                commandRouter = router
                voiceState.commandRouter = router
            }
            voiceState.configure(speechManager: speechManager)
            voiceState.availableCommandCount = coordinator.domScraper.elementCount
        }
        .onChange(of: coordinator.domScraper.elementCount) { count in
            voiceState.availableCommandCount = count
        }
        .onDisappear {
            voiceState.stopListening()
        }
    }

    // MARK: - Address Bar

    private var addressBar: some View {
        HStack(spacing: 8) {
            // Security indicator
            if coordinator.isSecure {
                Image(systemName: "lock.fill")
                    .font(.caption)
                    .foregroundStyle(.green)
            }

            // URL field
            TextField("Search or enter URL", text: $addressBarText, onEditingChanged: { editing in
                isEditingAddress = editing
                if editing {
                    addressBarText = coordinator.currentURL
                }
            })
            .textFieldStyle(.plain)
            .font(.system(.subheadline, design: .monospaced))
            .autocapitalization(.none)
            .disableAutocorrection(true)
            .keyboardType(.webSearch)
            .submitLabel(.go)
            .onSubmit {
                tabManager.navigate(to: addressBarText)
                isEditingAddress = false
            }

            // Reload / Stop button
            Button(action: {
                if coordinator.isLoading {
                    tabManager.activeWebView?.stopLoading()
                } else {
                    tabManager.reloadActiveTab()
                }
            }) {
                Image(systemName: coordinator.isLoading ? "xmark" : "arrow.clockwise")
                    .font(.system(size: 14, weight: .medium))
            }
            .tint(theme.onSurface.opacity(0.6))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(theme.surfaceVariant.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
    }

    // MARK: - WebView Container

    private var webViewContainer: some View {
        WebViewRepresentable(
            tabManager: tabManager,
            coordinator: coordinator
        )
    }

    // MARK: - Bottom Toolbar

    private var bottomToolbar: some View {
        HStack(spacing: 0) {
            // Back
            Button(action: { tabManager.activeWebView?.goBack() }) {
                Image(systemName: "chevron.left")
                    .frame(maxWidth: .infinity)
            }
            .disabled(!coordinator.canGoBack)

            // Forward
            Button(action: { tabManager.activeWebView?.goForward() }) {
                Image(systemName: "chevron.right")
                    .frame(maxWidth: .infinity)
            }
            .disabled(!coordinator.canGoForward)

            // Share
            Button(action: shareCurrentPage) {
                Image(systemName: "square.and.arrow.up")
                    .frame(maxWidth: .infinity)
            }

            // Overlay toggle
            Button(action: { showOverlay.toggle() }) {
                Image(systemName: showOverlay ? "number.square.fill" : "number.square")
                    .frame(maxWidth: .infinity)
            }
            .tint(showOverlay ? theme.primary : theme.onSurface.opacity(0.6))

            // Tabs
            Button(action: { showTabSwitcher = true }) {
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(theme.onSurface.opacity(0.6), lineWidth: 1.5)
                        .frame(width: 20, height: 20)
                    Text("\(tabManager.tabs.count)")
                        .font(.system(size: 11, weight: .bold))
                }
                .frame(maxWidth: .infinity)
            }
        }
        .font(.system(size: 18))
        .tint(theme.onSurface.opacity(0.6))
        .padding(.vertical, 8)
        .background(theme.surface)
    }

    // MARK: - Helpers

    /// Extracts display-friendly domain from URL.
    private func displayURL(_ urlString: String) -> String {
        guard let url = URL(string: urlString),
              let host = url.host else {
            return urlString
        }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    private func shareCurrentPage() {
        guard let url = URL(string: coordinator.currentURL) else { return }
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let vc = scene.windows.first?.rootViewController else { return }
        let activityVC = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        vc.present(activityVC, animated: true)
    }
}

// MARK: - WKWebView UIViewRepresentable

/// Wraps WKWebView for SwiftUI integration.
struct WebViewRepresentable: UIViewRepresentable {
    @ObservedObject var tabManager: TabManager
    @ObservedObject var coordinator: WebViewCoordinator

    func makeUIView(context: Context) -> WKWebView {
        guard let webView = tabManager.activeWebView else {
            return WKWebView(frame: .zero)
        }
        coordinator.observe(webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // If active tab changed, swap the web view
        if let activeWebView = tabManager.activeWebView, activeWebView !== uiView {
            // The parent view should recreate this representable
        }
    }
}

// MARK: - Tab Switcher

/// Grid view for switching between open tabs (similar to Safari).
struct TabSwitcherView: View {
    @ObservedObject var tabManager: TabManager
    @Binding var isPresented: Bool

    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 12)]

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(tabManager.tabs) { tab in
                        TabCard(
                            tab: tab,
                            isActive: tab.id == tabManager.activeTabId,
                            onTap: {
                                tabManager.switchTo(tab: tab)
                                isPresented = false
                            },
                            onClose: {
                                tabManager.close(tab: tab)
                            }
                        )
                    }
                }
                .padding()
            }
            .navigationTitle("Tabs (\(tabManager.tabs.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") { isPresented = false }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        tabManager.createNewTab()
                        isPresented = false
                    }) {
                        Image(systemName: "plus")
                    }
                }
            }
        }
    }
}

/// A card representing a single tab in the tab switcher.
struct TabCard: View {
    @ObservedObject var tab: BrowserTab
    let isActive: Bool
    let onTap: () -> Void
    let onClose: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(tab.title.isEmpty ? "New Tab" : tab.title)
                        .font(.caption)
                        .fontWeight(.medium)
                        .lineLimit(1)
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.secondary)
                    }
                }

                Text(tab.url)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            .padding(8)
            .frame(height: 80)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color(.systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isActive ? Color.accentColor : Color.gray.opacity(0.3), lineWidth: isActive ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Preference Key

struct WebViewFrameKey: PreferenceKey {
    static let defaultValue: CGRect = .zero
    static func reduce(value: inout CGRect, nextValue: () -> CGRect) {
        value = nextValue()
    }
}

#Preview {
    NavigationStack {
        BrowserView()
            .environmentObject(AppState())
    }
}

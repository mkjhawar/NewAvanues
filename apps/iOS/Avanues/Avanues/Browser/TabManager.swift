import Foundation
import WebKit

/// Manages browser tabs with WKWebView instances.
///
/// Each tab maintains its own WKWebView, URL, title, and favicon state.
/// Mirrors the Android TabViewModel + SQLDelight tab persistence pattern.
/// Tab state will be backed by KMP SQLDelight once Phase 2 enables iOS targets.
final class TabManager: ObservableObject {

    // MARK: - Published State

    @Published var tabs: [BrowserTab] = []
    @Published var activeTabId: UUID? = nil

    /// The currently active tab.
    var activeTab: BrowserTab? {
        tabs.first { $0.id == activeTabId }
    }

    /// The WKWebView for the active tab.
    var activeWebView: WKWebView? {
        activeTab?.webView
    }

    // MARK: - Configuration

    private let maxTabs = 50
    private let coordinator: WebViewCoordinator

    init(coordinator: WebViewCoordinator) {
        self.coordinator = coordinator
        // Create initial tab
        let initial = createNewTab(url: "https://www.google.com", activate: false)
        activeTabId = initial.id
    }

    // MARK: - Tab Operations

    /// Creates a new tab and optionally activates it.
    @discardableResult
    func createNewTab(url: String = "about:blank", activate: Bool = true) -> BrowserTab {
        let tab = BrowserTab(
            url: url,
            coordinator: coordinator
        )
        tabs.append(tab)

        if activate {
            switchTo(tab: tab)
        }
        return tab
    }

    /// Switches the active tab.
    func switchTo(tab: BrowserTab) {
        activeTabId = tab.id
        // Attach coordinator observers to new active WebView
        coordinator.stopObserving()
        coordinator.observe(tab.webView)
        coordinator.currentURL = tab.url
        coordinator.pageTitle = tab.title
    }

    /// Closes a specific tab.
    func close(tab: BrowserTab) {
        guard tabs.count > 1 else { return } // Keep at least 1 tab
        tabs.removeAll { $0.id == tab.id }
        tab.cleanup()

        // If we closed the active tab, switch to the last one
        if activeTabId == tab.id {
            if let lastTab = tabs.last {
                switchTo(tab: lastTab)
            }
        }
    }

    /// Closes all tabs except the active one.
    func closeAllExceptActive() {
        let keepId = activeTabId
        let toRemove = tabs.filter { $0.id != keepId }
        toRemove.forEach { $0.cleanup() }
        tabs = tabs.filter { $0.id == keepId }
    }

    /// Reloads the active tab.
    func reloadActiveTab() {
        activeTab?.webView.reload()
    }

    /// Navigates the active tab to a URL.
    func navigate(to urlString: String) {
        guard let tab = activeTab else { return }
        var normalizedURL = urlString.trimmingCharacters(in: .whitespacesAndNewlines)

        // Add scheme if missing
        if !normalizedURL.contains("://") {
            if normalizedURL.contains(".") && !normalizedURL.contains(" ") {
                normalizedURL = "https://\(normalizedURL)"
            } else {
                // Treat as search query
                let encoded = normalizedURL.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? normalizedURL
                normalizedURL = "https://www.google.com/search?q=\(encoded)"
            }
        }

        guard let url = URL(string: normalizedURL) else { return }
        tab.webView.load(URLRequest(url: url))
    }
}

// MARK: - BrowserTab

/// A single browser tab with its own WKWebView instance.
final class BrowserTab: Identifiable, ObservableObject {
    let id = UUID()
    @Published var url: String
    @Published var title: String = ""
    @Published var isPrivate: Bool = false

    let webView: WKWebView

    init(url: String, coordinator: WebViewCoordinator, isPrivate: Bool = false) {
        self.url = url
        self.isPrivate = isPrivate

        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.preferences.javaScriptCanOpenWindowsAutomatically = false

        // Private browsing uses non-persistent data store
        if isPrivate {
            config.websiteDataStore = .nonPersistent()
        }

        // Register DOM scraper message handler
        config.userContentController.add(coordinator, name: DOMScraper.messageName)

        // Configure scraper user script
        coordinator.domScraper.configure(configuration: config)

        webView = WKWebView(frame: .zero, configuration: config)
        webView.allowsBackForwardNavigationGestures = true
        webView.navigationDelegate = coordinator
        webView.uiDelegate = coordinator

        // Load initial URL
        if let parsedURL = URL(string: url) {
            webView.load(URLRequest(url: parsedURL))
        }
    }

    /// Clean up WKWebView resources.
    func cleanup() {
        webView.stopLoading()
        webView.configuration.userContentController.removeAllUserScripts()
        webView.configuration.userContentController.removeScriptMessageHandler(forName: DOMScraper.messageName)
    }

    deinit {
        cleanup()
    }
}

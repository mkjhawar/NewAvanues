import SwiftUI
import UIKit
import WebKit
import Combine

/// Delegate coordinator for WKWebView lifecycle, navigation, JS bridge.
///
/// Handles:
/// - Page load lifecycle (didStart, didFinish, didFail)
/// - JS alerts/confirms/prompts
/// - WKScriptMessageHandler for DOM scraper results
/// - Progress and title/URL observation via KVO
final class WebViewCoordinator: NSObject, ObservableObject {

    // MARK: - Published State

    @Published var currentURL: String = ""
    @Published var pageTitle: String = ""
    @Published var isLoading: Bool = false
    @Published var loadingProgress: Double = 0.0
    @Published var canGoBack: Bool = false
    @Published var canGoForward: Bool = false
    @Published var isSecure: Bool = false

    // MARK: - DOM Scraper

    let domScraper = DOMScraper()

    // MARK: - KVO Tokens

    private var progressObserver: NSKeyValueObservation?
    private var titleObserver: NSKeyValueObservation?
    private var urlObserver: NSKeyValueObservation?
    private var canGoBackObserver: NSKeyValueObservation?
    private var canGoForwardObserver: NSKeyValueObservation?
    private var isLoadingObserver: NSKeyValueObservation?

    // MARK: - Observation Setup

    func observe(_ webView: WKWebView) {
        progressObserver = webView.observe(\.estimatedProgress, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async { self?.loadingProgress = wv.estimatedProgress }
        }
        titleObserver = webView.observe(\.title, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async { self?.pageTitle = wv.title ?? "" }
        }
        urlObserver = webView.observe(\.url, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async {
                let url = wv.url?.absoluteString ?? ""
                self?.currentURL = url
                self?.isSecure = url.hasPrefix("https://")
            }
        }
        canGoBackObserver = webView.observe(\.canGoBack, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async { self?.canGoBack = wv.canGoBack }
        }
        canGoForwardObserver = webView.observe(\.canGoForward, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async { self?.canGoForward = wv.canGoForward }
        }
        isLoadingObserver = webView.observe(\.isLoading, options: .new) { [weak self] wv, _ in
            DispatchQueue.main.async { self?.isLoading = wv.isLoading }
        }
    }

    func stopObserving() {
        progressObserver?.invalidate()
        titleObserver?.invalidate()
        urlObserver?.invalidate()
        canGoBackObserver?.invalidate()
        canGoForwardObserver?.invalidate()
        isLoadingObserver?.invalidate()
    }

    deinit {
        stopObserving()
    }
}

// MARK: - WKNavigationDelegate

extension WebViewCoordinator: WKNavigationDelegate {

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        DispatchQueue.main.async {
            self.isLoading = true
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.async {
            self.isLoading = false
            self.pageTitle = webView.title ?? ""
            self.currentURL = webView.url?.absoluteString ?? ""
        }
        // Trigger DOM scrape after page load
        domScraper.scrape(webView: webView)
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        DispatchQueue.main.async {
            self.isLoading = false
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        DispatchQueue.main.async {
            self.isLoading = false
        }
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        // Open external app links (tel:, mailto:, etc.) in system
        if let url = navigationAction.request.url,
           !["http", "https", "about", "data"].contains(url.scheme?.lowercased()) {
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }
}

// MARK: - WKUIDelegate

extension WebViewCoordinator: WKUIDelegate {

    func webView(
        _ webView: WKWebView,
        runJavaScriptAlertPanelWithMessage message: String,
        initiatedByFrame frame: WKFrameInfo,
        completionHandler: @escaping () -> Void
    ) {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let vc = scene.windows.first?.rootViewController else {
            completionHandler()
            return
        }
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler() })
        vc.present(alert, animated: true)
    }

    func webView(
        _ webView: WKWebView,
        runJavaScriptConfirmPanelWithMessage message: String,
        initiatedByFrame frame: WKFrameInfo,
        completionHandler: @escaping (Bool) -> Void
    ) {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let vc = scene.windows.first?.rootViewController else {
            completionHandler(false)
            return
        }
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in completionHandler(false) })
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler(true) })
        vc.present(alert, animated: true)
    }

    func webView(
        _ webView: WKWebView,
        runJavaScriptTextInputPanelWithPrompt prompt: String,
        defaultText: String?,
        initiatedByFrame frame: WKFrameInfo,
        completionHandler: @escaping (String?) -> Void
    ) {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let vc = scene.windows.first?.rootViewController else {
            completionHandler(nil)
            return
        }
        let alert = UIAlertController(title: nil, message: prompt, preferredStyle: .alert)
        alert.addTextField { tf in tf.text = defaultText }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel) { _ in completionHandler(nil) })
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in
            completionHandler(alert.textFields?.first?.text)
        })
        vc.present(alert, animated: true)
    }
}

// MARK: - WKScriptMessageHandler

extension WebViewCoordinator: WKScriptMessageHandler {

    func userContentController(
        _ userContentController: WKUserContentController,
        didReceive message: WKScriptMessage
    ) {
        guard message.name == DOMScraper.messageName else { return }
        guard let body = message.body as? String,
              let data = body.data(using: .utf8) else { return }
        domScraper.handleScraperResult(data)
    }
}

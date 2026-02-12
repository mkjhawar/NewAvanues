import Foundation
import WebKit

/// Orchestrates DOM scraping in WKWebView.
///
/// Injects the DOMScraperBridge JavaScript into loaded pages,
/// parses results, and maintains scraped element state for
/// voice command generation and element overlay numbering.
///
/// Mirrors the Android DOMScraperBridge + BrowserVoiceOSCallback pipeline.
final class DOMScraper: ObservableObject {

    /// WKScriptMessageHandler name for JS→Swift communication.
    static let messageName = "VoiceOSBridge"

    // MARK: - Published State

    /// Scraped interactive elements from the current page.
    @Published var elements: [DOMElement] = []

    /// Total element count from last scrape.
    @Published var elementCount: Int = 0

    /// Whether a scrape is in progress.
    @Published var isScraping: Bool = false

    /// Structure hash of the current DOM (for change detection).
    @Published var structureHash: String = ""

    // MARK: - Scrape Throttling

    /// Minimum interval between scrapes (seconds).
    private let scrapeCooldown: TimeInterval = 2.0
    private var lastScrapeTime: Date = .distantPast

    // MARK: - JavaScript

    /// The DOM scraper JavaScript.
    /// Sourced from DOMScraperBridge.SCRAPER_SCRIPT in KMP commonMain.
    /// Embedded directly for standalone iOS operation before KMP integration.
    static let scraperScript: String = """
    (function() {
        'use strict';
        const MAX_ELEMENTS = 500;
        const MAX_TEXT_LENGTH = 100;
        const MAX_DEPTH = 15;

        const INTERACTIVE_TAGS = new Set([
            'A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'LABEL',
            'SUMMARY', 'DETAILS', 'DIALOG', 'MENU', 'MENUITEM'
        ]);
        const INTERACTIVE_ROLES = new Set([
            'button', 'link', 'menuitem', 'option', 'tab', 'checkbox',
            'radio', 'textbox', 'searchbox', 'combobox', 'listbox',
            'slider', 'switch', 'spinbutton', 'menuitemcheckbox',
            'menuitemradio', 'treeitem', 'gridcell', 'row'
        ]);

        function djb2Hash(str) {
            let hash = 5381;
            for (let i = 0; i < str.length; i++) {
                hash = ((hash << 5) + hash) + str.charCodeAt(i);
                hash = hash & hash;
            }
            return (hash >>> 0).toString(16).padStart(8, '0');
        }

        const seenHashes = new Set();
        function stableElementHash(element) {
            const parts = [];
            if (element.id) parts.push('id:' + element.id);
            const name = element.getAttribute('name');
            if (name) parts.push('name:' + name);
            const ariaLabel = element.getAttribute('aria-label');
            if (ariaLabel) parts.push('aria:' + ariaLabel);
            if (parts.length === 0) {
                parts.push('sel:' + generateSelector(element));
                parts.push('tag:' + element.tagName.toLowerCase());
                parts.push('type:' + (element.getAttribute('type') || ''));
            }
            let id = 'vos_' + djb2Hash(parts.join('|'));
            if (seenHashes.has(id)) {
                let suffix = 1;
                while (seenHashes.has(id + '_' + suffix)) suffix++;
                id = id + '_' + suffix;
            }
            seenHashes.add(id);
            return id;
        }

        function isVisible(el) {
            const s = window.getComputedStyle(el);
            if (s.display === 'none' || s.visibility === 'hidden' || parseFloat(s.opacity) === 0) return false;
            const r = el.getBoundingClientRect();
            return r.width > 0 || r.height > 0;
        }

        function isInteractive(el) {
            if (INTERACTIVE_TAGS.has(el.tagName)) return true;
            const role = el.getAttribute('role');
            if (role && INTERACTIVE_ROLES.has(role.toLowerCase())) return true;
            if (el.hasAttribute('onclick') || el.isContentEditable) return true;
            if (el.hasAttribute('tabindex') && el.tabIndex >= 0) return true;
            const s = window.getComputedStyle(el);
            return s.cursor === 'pointer';
        }

        function getAccessibleName(el) {
            const aria = el.getAttribute('aria-label');
            if (aria) return aria.trim().substring(0, MAX_TEXT_LENGTH);
            if (el.placeholder) return el.placeholder.trim().substring(0, MAX_TEXT_LENGTH);
            if (el.title) return el.title.trim().substring(0, MAX_TEXT_LENGTH);
            if (el.alt) return el.alt.trim().substring(0, MAX_TEXT_LENGTH);
            const text = (el.textContent || '').trim();
            return text.substring(0, MAX_TEXT_LENGTH);
        }

        function getElementType(el) {
            const tag = el.tagName.toLowerCase();
            const type = (el.type || '').toLowerCase();
            const role = (el.getAttribute('role') || '').toLowerCase();
            if (tag === 'a') return 'link';
            if (tag === 'button' || role === 'button') return 'button';
            if (tag === 'input') {
                if (type === 'submit' || type === 'button') return 'button';
                if (type === 'checkbox') return 'checkbox';
                if (type === 'radio') return 'radio';
                return 'input';
            }
            if (tag === 'select') return 'dropdown';
            if (tag === 'textarea') return 'input';
            if (role === 'tab') return 'tab';
            if (role === 'menuitem') return 'menuitem';
            return 'element';
        }

        function generateSelector(el) {
            if (el.id) return '#' + CSS.escape(el.id);
            let sel = el.tagName.toLowerCase();
            const classes = Array.from(el.classList).slice(0, 2);
            if (classes.length > 0) sel += '.' + classes.map(c => CSS.escape(c)).join('.');
            const parent = el.parentElement;
            if (parent) {
                const siblings = Array.from(parent.children).filter(s => s.tagName === el.tagName);
                if (siblings.length > 1) sel += ':nth-of-type(' + (siblings.indexOf(el) + 1) + ')';
            }
            return sel;
        }

        function computeStructureHash() {
            const sigs = [];
            function walk(node, depth) {
                if (depth > 5 || !node || node.nodeType !== 1) return;
                const tag = node.tagName.toLowerCase();
                const id = node.id ? '#' + node.id : '';
                const role = node.getAttribute('role') || '';
                sigs.push(tag + id + ':' + role + ':d' + depth + ':c' + node.children.length);
                const max = Math.min(node.children.length, 20);
                for (let i = 0; i < max; i++) walk(node.children[i], depth + 1);
            }
            walk(document.body, 0);
            sigs.sort();
            return djb2Hash(sigs.join('|'));
        }

        const elements = [];
        const seen = new Set();
        seenHashes.clear();

        function traverse(node, depth) {
            if (depth > MAX_DEPTH || elements.length >= MAX_ELEMENTS || !(node instanceof Element)) return;
            if (!isVisible(node)) return;
            if (isInteractive(node) && !seen.has(node)) {
                seen.add(node);
                const rect = node.getBoundingClientRect();
                elements.push({
                    id: stableElementHash(node),
                    tag: node.tagName.toLowerCase(),
                    type: getElementType(node),
                    name: getAccessibleName(node),
                    selector: generateSelector(node),
                    bounds: {
                        left: Math.round(rect.left), top: Math.round(rect.top),
                        right: Math.round(rect.right), bottom: Math.round(rect.bottom),
                        width: Math.round(rect.width), height: Math.round(rect.height)
                    },
                    depth: depth,
                    isDisabled: node.disabled || node.getAttribute('aria-disabled') === 'true'
                });
            }
            for (const child of node.children) traverse(child, depth + 1);
        }
        traverse(document.body, 0);

        const result = JSON.stringify({
            url: window.location.href,
            title: document.title,
            timestamp: Date.now(),
            viewport: {
                width: window.innerWidth, height: window.innerHeight,
                scrollX: window.scrollX, scrollY: window.scrollY
            },
            elements: elements,
            elementCount: elements.length,
            structureHash: computeStructureHash()
        });
        window.webkit.messageHandlers.VoiceOSBridge.postMessage(result);
    })();
    """

    // MARK: - WKWebView Integration

    /// Configures a WKWebView with the scraper user script.
    func configure(configuration: WKWebViewConfiguration) {
        let userScript = WKUserScript(
            source: Self.scraperScript,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )
        configuration.userContentController.addUserScript(userScript)
    }

    /// Manually trigger a scrape on the given WKWebView.
    func scrape(webView: WKWebView) {
        let now = Date()
        guard now.timeIntervalSince(lastScrapeTime) >= scrapeCooldown else { return }
        lastScrapeTime = now

        DispatchQueue.main.async { self.isScraping = true }

        webView.evaluateJavaScript(Self.scraperScript) { [weak self] _, error in
            if let error = error {
                print("[DOMScraper] Evaluation error: \(error.localizedDescription)")
                DispatchQueue.main.async { self?.isScraping = false }
            }
            // Results arrive via WKScriptMessageHandler (VoiceOSBridge)
        }
    }

    /// Parse the JSON scraper result received from JS.
    func handleScraperResult(_ data: Data) {
        do {
            let result = try JSONDecoder().decode(ScrapeResult.self, from: data)

            // Skip if DOM structure hasn't changed
            if result.structureHash == structureHash && !elements.isEmpty {
                DispatchQueue.main.async { self.isScraping = false }
                return
            }

            DispatchQueue.main.async {
                self.elements = result.elements
                self.elementCount = result.elementCount
                self.structureHash = result.structureHash
                self.isScraping = false
            }
        } catch {
            print("[DOMScraper] JSON parse error: \(error.localizedDescription)")
            DispatchQueue.main.async { self.isScraping = false }
        }
    }

    /// Clears the scraped element state.
    func clear() {
        DispatchQueue.main.async {
            self.elements = []
            self.elementCount = 0
            self.structureHash = ""
        }
    }
}

// MARK: - Models

/// Result of a DOM scrape operation.
struct ScrapeResult: Codable {
    let url: String
    let title: String
    let timestamp: Int
    let viewport: Viewport
    let elements: [DOMElement]
    let elementCount: Int
    let structureHash: String
}

/// Viewport dimensions at the time of scraping.
struct Viewport: Codable {
    let width: Int
    let height: Int
    let scrollX: Int
    let scrollY: Int
}

/// A scraped interactive DOM element.
struct DOMElement: Codable, Identifiable {
    let id: String
    let tag: String
    let type: String
    let name: String
    let selector: String
    let bounds: ElementBounds
    let depth: Int
    let isDisabled: Bool
}

/// Bounding rectangle of a DOM element in viewport coordinates.
struct ElementBounds: Codable {
    let left: Int
    let top: Int
    let right: Int
    let bottom: Int
    let width: Int
    let height: Int

    /// Center point of the element.
    var center: CGPoint {
        CGPoint(x: CGFloat(left + width / 2), y: CGFloat(top + height / 2))
    }
}

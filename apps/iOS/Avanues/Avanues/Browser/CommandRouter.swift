import SwiftUI
import WebKit

/// Routes recognized voice commands to browser actions.
///
/// Handles:
/// - Numeric commands: "click 3" → clicks element at index 3
/// - Navigation: "go back", "refresh", "go to google.com"
/// - Scrolling: "scroll down", "scroll to top"
/// - Text input: "type hello world"
/// - Element interaction: "focus search", "select all"
/// - Tab management: "new tab", "close tab"
///
/// Mirrors the Android CommandOrchestrator + WebCommandHandler pipeline.
@MainActor
final class CommandRouter: ObservableObject {

    private weak var tabManager: TabManager?
    private weak var domScraper: DOMScraper?
    private weak var voiceState: VoiceState?

    init(tabManager: TabManager, domScraper: DOMScraper, voiceState: VoiceState) {
        self.tabManager = tabManager
        self.domScraper = domScraper
        self.voiceState = voiceState
    }

    /// Process a recognized voice command phrase.
    func processCommand(_ phrase: String) {
        let normalized = phrase.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !normalized.isEmpty else { return }

        voiceState?.state = .processing

        // Try each command pattern in priority order
        if tryNumericClick(normalized)
            || tryNavigation(normalized)
            || tryScrolling(normalized)
            || tryTextInput(normalized)
            || tryElementAction(normalized)
            || tryTabCommand(normalized)
            || tryOverlayCommand(normalized) {
            // Command handled
        } else {
            voiceState?.onCommandExecuted(text: phrase, success: false)
        }

        voiceState?.state = .listening
    }

    // MARK: - Numeric Click ("click 3", "tap 5", "press 1")

    private func tryNumericClick(_ phrase: String) -> Bool {
        let patterns = ["click ", "tap ", "press ", "select ", "haz clic en ", "appuie sur ", "klick "]
        for prefix in patterns {
            if phrase.hasPrefix(prefix) {
                let numberStr = String(phrase.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                if let number = parseNumber(numberStr) {
                    clickElement(at: number)
                    return true
                }
            }
        }
        return false
    }

    /// Maps spoken numbers (including multi-locale) to integers.
    private func parseNumber(_ text: String) -> Int? {
        // Direct numeric
        if let n = Int(text) { return n }

        // English words
        let numberWords: [String: Int] = [
            "one": 1, "two": 2, "three": 3, "four": 4, "five": 5,
            "six": 6, "seven": 7, "eight": 8, "nine": 9, "ten": 10,
            // Spanish
            "uno": 1, "dos": 2, "tres": 3, "cuatro": 4, "cinco": 5,
            "seis": 6, "siete": 7, "ocho": 8, "nueve": 9, "diez": 10,
            // French
            "un": 1, "deux": 2, "trois": 3, "quatre": 4, "cinq": 5,
            "sept": 7, "huit": 8, "neuf": 9, "dix": 10,
            // German
            "eins": 1, "zwei": 2, "drei": 3, "vier": 4, "fünf": 5,
            "sechs": 6, "sieben": 7, "acht": 8, "neun": 9, "zehn": 10,
            // Hindi
            "ek": 1, "do": 2, "teen": 3, "char": 4, "paanch": 5
        ]
        return numberWords[text.lowercased()]
    }

    private func clickElement(at index: Int) {
        guard let elements = domScraper?.elements,
              index >= 1 && index <= elements.count else {
            voiceState?.onCommandExecuted(text: "click \(index)", success: false)
            return
        }

        let element = elements[index - 1]
        let script = """
        (function() {
            const el = document.querySelector('\(element.selector.replacingOccurrences(of: "'", with: "\\'"))');
            if (el) {
                el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                setTimeout(() => el.click(), 100);
                return JSON.stringify({ success: true });
            }
            return JSON.stringify({ success: false });
        })();
        """

        executeJS(script, commandText: "click \(index)")
    }

    // MARK: - Navigation

    private func tryNavigation(_ phrase: String) -> Bool {
        switch phrase {
        case "go back", "back", "atrás", "retour", "zurück":
            tabManager?.activeWebView?.goBack()
            voiceState?.onCommandExecuted(text: "go back", success: true)
            return true
        case "go forward", "forward", "adelante", "avancer", "vorwärts":
            tabManager?.activeWebView?.goForward()
            voiceState?.onCommandExecuted(text: "go forward", success: true)
            return true
        case "refresh", "reload", "actualizar", "rafraîchir", "aktualisieren":
            tabManager?.activeWebView?.reload()
            voiceState?.onCommandExecuted(text: "refresh", success: true)
            return true
        case "stop", "stop loading", "parar", "arrêter", "stopp":
            tabManager?.activeWebView?.stopLoading()
            voiceState?.onCommandExecuted(text: "stop", success: true)
            return true
        default:
            break
        }

        // "go to <url>"
        let goToPatterns = ["go to ", "navigate to ", "open ", "ir a ", "aller à ", "gehe zu "]
        for prefix in goToPatterns {
            if phrase.hasPrefix(prefix) {
                let destination = String(phrase.dropFirst(prefix.count))
                tabManager?.navigate(to: destination)
                voiceState?.onCommandExecuted(text: "navigate to \(destination)", success: true)
                return true
            }
        }

        return false
    }

    // MARK: - Scrolling

    private func tryScrolling(_ phrase: String) -> Bool {
        let scrollMap: [String: String] = [
            "scroll down": "window.scrollBy({top: window.innerHeight * 0.85, behavior: 'smooth'})",
            "scroll up": "window.scrollBy({top: -window.innerHeight * 0.85, behavior: 'smooth'})",
            "scroll to top": "window.scrollTo({top: 0, behavior: 'smooth'})",
            "scroll to bottom": "window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'})",
            "page down": "window.scrollBy({top: window.innerHeight * 0.85, behavior: 'smooth'})",
            "page up": "window.scrollBy({top: -window.innerHeight * 0.85, behavior: 'smooth'})",
            // Spanish
            "desplazar abajo": "window.scrollBy({top: window.innerHeight * 0.85, behavior: 'smooth'})",
            "desplazar arriba": "window.scrollBy({top: -window.innerHeight * 0.85, behavior: 'smooth'})",
            // French
            "défiler vers le bas": "window.scrollBy({top: window.innerHeight * 0.85, behavior: 'smooth'})",
            "défiler vers le haut": "window.scrollBy({top: -window.innerHeight * 0.85, behavior: 'smooth'})"
        ]

        if let script = scrollMap[phrase] {
            executeJS(script, commandText: phrase)
            return true
        }
        return false
    }

    // MARK: - Text Input

    private func tryTextInput(_ phrase: String) -> Bool {
        let typePatterns = ["type ", "enter ", "escribir ", "taper ", "eingeben "]
        for prefix in typePatterns {
            if phrase.hasPrefix(prefix) {
                let text = String(phrase.dropFirst(prefix.count))
                let escaped = text.replacingOccurrences(of: "'", with: "\\'")
                let script = """
                (function() {
                    const a = document.activeElement;
                    if (a && (a.tagName === 'INPUT' || a.tagName === 'TEXTAREA')) {
                        a.value += '\(escaped)';
                        a.dispatchEvent(new Event('input', {bubbles: true}));
                        return JSON.stringify({success: true});
                    }
                    return JSON.stringify({success: false, message: 'No active input'});
                })();
                """
                executeJS(script, commandText: "type \(text)")
                return true
            }
        }

        // Special text commands
        switch phrase {
        case "select all", "seleccionar todo", "tout sélectionner", "alles markieren":
            executeJS("document.execCommand('selectAll')", commandText: "select all")
            return true
        case "copy", "copiar", "copier", "kopieren":
            executeJS("document.execCommand('copy')", commandText: "copy")
            return true
        case "cut", "cortar", "couper", "ausschneiden":
            executeJS("document.execCommand('cut')", commandText: "cut")
            return true
        default:
            return false
        }
    }

    // MARK: - Element Actions

    private func tryElementAction(_ phrase: String) -> Bool {
        // "focus search", "focus email"
        if phrase.hasPrefix("focus ") {
            let target = String(phrase.dropFirst(6))
            let script = """
            (function() {
                const inputs = document.querySelectorAll('input, textarea, select');
                for (const el of inputs) {
                    const name = (el.getAttribute('aria-label') || el.placeholder || el.name || '').toLowerCase();
                    if (name.includes('\(target)')) {
                        el.focus();
                        el.scrollIntoView({behavior: 'smooth', block: 'center'});
                        return JSON.stringify({success: true});
                    }
                }
                return JSON.stringify({success: false});
            })();
            """
            executeJS(script, commandText: "focus \(target)")
            return true
        }

        // "next field" / "previous field"
        if phrase == "next field" || phrase == "tab next" || phrase == "siguiente campo" {
            let script = """
            (function() {
                const focusable = Array.from(document.querySelectorAll('a[href], button, input, select, textarea, [tabindex]')).filter(el => !el.disabled && el.offsetParent !== null);
                const idx = focusable.indexOf(document.activeElement);
                const next = focusable[idx + 1] || focusable[0];
                if (next) { next.focus(); next.scrollIntoView({behavior:'smooth',block:'center'}); return JSON.stringify({success:true}); }
                return JSON.stringify({success:false});
            })();
            """
            executeJS(script, commandText: "next field")
            return true
        }

        return false
    }

    // MARK: - Tab Commands

    private func tryTabCommand(_ phrase: String) -> Bool {
        switch phrase {
        case "new tab", "nueva pestaña", "nouvel onglet", "neuer tab":
            tabManager?.createNewTab()
            voiceState?.onCommandExecuted(text: "new tab", success: true)
            return true
        case "close tab", "cerrar pestaña", "fermer onglet", "tab schließen":
            if let tab = tabManager?.activeTab {
                tabManager?.close(tab: tab)
            }
            voiceState?.onCommandExecuted(text: "close tab", success: true)
            return true
        default:
            return false
        }
    }

    // MARK: - Overlay Commands

    private func tryOverlayCommand(_ phrase: String) -> Bool {
        switch phrase {
        case "show numbers", "mostrar números", "afficher numéros", "nummern anzeigen":
            // Overlay toggle handled by BrowserView — emit event
            voiceState?.onCommandExecuted(text: "show numbers", success: true)
            NotificationCenter.default.post(name: .toggleOverlay, object: nil)
            return true
        case "hide numbers", "ocultar números", "cacher numéros", "nummern ausblenden":
            voiceState?.onCommandExecuted(text: "hide numbers", success: true)
            NotificationCenter.default.post(name: .toggleOverlay, object: nil)
            return true
        default:
            return false
        }
    }

    // MARK: - JS Execution

    private func executeJS(_ script: String, commandText: String) {
        guard let webView = tabManager?.activeWebView else {
            voiceState?.onCommandExecuted(text: commandText, success: false)
            return
        }

        webView.evaluateJavaScript(script) { [weak self] result, error in
            let success: Bool
            if let error = error {
                print("[CommandRouter] JS error: \(error.localizedDescription)")
                success = false
            } else if let resultStr = result as? String,
                      let data = resultStr.data(using: .utf8),
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                success = json["success"] as? Bool ?? false
            } else {
                success = error == nil
            }
            Task { @MainActor in
                self?.voiceState?.onCommandExecuted(text: commandText, success: success)
            }
        }
    }
}

// MARK: - Notification

extension Notification.Name {
    static let toggleOverlay = Notification.Name("com.augmentalis.avanues.toggleOverlay")
}

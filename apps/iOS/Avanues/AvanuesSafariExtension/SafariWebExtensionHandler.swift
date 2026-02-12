import SafariServices
import os.log

/// Native bridge between the Safari Web Extension and the containing Avanues app.
///
/// Handles messages from background.js via `browser.runtime.sendNativeMessage()`.
/// Routes voice recognition requests to SFSpeechRecognizer and returns transcriptions.
class SafariWebExtensionHandler: NSObject, NSExtensionRequestHandling {

    private let logger = Logger(subsystem: "com.augmentalis.avanues.ios.safari-extension", category: "handler")

    func beginRequest(with context: NSExtensionContext) {
        guard let item = context.inputItems.first as? NSExtensionItem,
              let message = item.userInfo?[SFExtensionMessageKey] as? [String: Any] else {
            logger.warning("Received invalid extension message")
            context.cancelRequest(withError: NSError(
                domain: "AvanuesSafariExtension",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Invalid message format"]
            ))
            return
        }

        let command = message["command"] as? String ?? "unknown"
        logger.info("Received command from extension: \(command)")

        switch command {
        case "ping":
            // Health check from extension
            let response = NSExtensionItem()
            response.userInfo = [SFExtensionMessageKey: ["status": "ok", "version": "1.0.0"]]
            context.completeRequest(returningItems: [response])

        case "startVoiceRecognition":
            // Voice recognition will be implemented in Phase 2
            let response = NSExtensionItem()
            response.userInfo = [SFExtensionMessageKey: [
                "error": "Voice recognition not yet implemented. Coming in Phase 2."
            ]]
            context.completeRequest(returningItems: [response])

        default:
            logger.warning("Unknown command: \(command)")
            let response = NSExtensionItem()
            response.userInfo = [SFExtensionMessageKey: ["error": "Unknown command: \(command)"]]
            context.completeRequest(returningItems: [response])
        }
    }
}

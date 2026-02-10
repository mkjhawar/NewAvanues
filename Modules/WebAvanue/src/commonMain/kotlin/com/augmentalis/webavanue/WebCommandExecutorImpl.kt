package com.augmentalis.webavanue

import com.augmentalis.voiceoscore.IWebCommandExecutor
import com.augmentalis.voiceoscore.WebAction
import com.augmentalis.voiceoscore.WebActionResult
import com.augmentalis.voiceoscore.WebActionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

/**
 * Implements [IWebCommandExecutor] by building JavaScript code
 * and evaluating it via [IJavaScriptExecutor].
 *
 * Each [WebActionType] maps to a specific JavaScript snippet
 * defined in [DOMScraperBridge] or inline for page-level actions.
 */
class WebCommandExecutorImpl : IWebCommandExecutor {

    private val callback: BrowserVoiceOSCallback
    private val jsExecutor: IJavaScriptExecutor

    /**
     * Create with explicit JS executor and callback.
     */
    constructor(jsExecutor: IJavaScriptExecutor, callback: BrowserVoiceOSCallback) {
        this.jsExecutor = jsExecutor
        this.callback = callback
    }

    /**
     * Create from callback — uses the callback's internal JS executor proxy.
     * The callback delegates to its own jsExecutor field (set by WebViewContainer).
     */
    constructor(callback: BrowserVoiceOSCallback) {
        this.callback = callback
        this.jsExecutor = CallbackJsExecutorProxy(callback)
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun isWebContextActive(): Boolean {
        return callback.currentUrl.value.isNotBlank() &&
               !callback.isPageLoading.value
    }

    override suspend fun executeWebAction(action: WebAction): WebActionResult {
        val script = buildScript(action)
        val resultStr = jsExecutor.evaluateJavaScript(script)
            ?: return WebActionResult(false, "JavaScript execution returned null")

        return parseResult(resultStr)
    }

    private fun buildScript(action: WebAction): String {
        val selector = escapeSelectorForJs(action.selector)

        return when (action.actionType) {
            // ═══════════════════════════════════════════════════════════════
            // Element Actions
            // ═══════════════════════════════════════════════════════════════
            WebActionType.CLICK -> DOMScraperBridge.clickBySelectorScript(selector)
            WebActionType.FOCUS -> DOMScraperBridge.focusBySelectorScript(selector)
            WebActionType.INPUT -> DOMScraperBridge.inputTextBySelectorScript(selector, action.text)
            WebActionType.SCROLL_TO -> DOMScraperBridge.scrollToBySelectorScript(selector)
            WebActionType.TOGGLE -> DOMScraperBridge.toggleCheckboxScript(selector)
            WebActionType.SELECT -> DOMScraperBridge.selectDropdownScript(selector, action.text)
            WebActionType.LONG_PRESS -> DOMScraperBridge.longPressScript(selector)
            WebActionType.DOUBLE_CLICK -> DOMScraperBridge.doubleClickScript(selector)
            WebActionType.HOVER -> DOMScraperBridge.hoverScript(selector)

            // ═══════════════════════════════════════════════════════════════
            // Page Navigation
            // ═══════════════════════════════════════════════════════════════
            WebActionType.SCROLL_PAGE_UP -> DOMScraperBridge.scrollPageScript("up")
            WebActionType.SCROLL_PAGE_DOWN -> DOMScraperBridge.scrollPageScript("down")
            WebActionType.SCROLL_TO_TOP -> DOMScraperBridge.scrollToTopScript()
            WebActionType.SCROLL_TO_BOTTOM -> DOMScraperBridge.scrollToBottomScript()
            WebActionType.PAGE_BACK -> DOMScraperBridge.pageBackScript()
            WebActionType.PAGE_FORWARD -> DOMScraperBridge.pageForwardScript()
            WebActionType.PAGE_REFRESH -> DOMScraperBridge.pageRefreshScript()

            // ═══════════════════════════════════════════════════════════════
            // Form Navigation
            // ═══════════════════════════════════════════════════════════════
            WebActionType.TAB_NEXT -> DOMScraperBridge.tabNextScript()
            WebActionType.TAB_PREV -> DOMScraperBridge.tabPrevScript()
            WebActionType.SUBMIT_FORM -> DOMScraperBridge.submitFormScript(selector)

            // ═══════════════════════════════════════════════════════════════
            // Gesture Actions
            // ═══════════════════════════════════════════════════════════════
            WebActionType.SWIPE_LEFT -> DOMScraperBridge.swipeScript(selector, "left")
            WebActionType.SWIPE_RIGHT -> DOMScraperBridge.swipeScript(selector, "right")
            WebActionType.SWIPE_UP -> DOMScraperBridge.swipeScript(selector, "up")
            WebActionType.SWIPE_DOWN -> DOMScraperBridge.swipeScript(selector, "down")
            WebActionType.GRAB -> DOMScraperBridge.grabScript(selector)
            WebActionType.RELEASE -> DOMScraperBridge.releaseScript()
            WebActionType.ROTATE -> DOMScraperBridge.rotateScript(
                selector,
                action.params["direction"] ?: "right",
                action.params["angle"] ?: "90"
            )
            WebActionType.DRAG -> DOMScraperBridge.dragScript(
                selector,
                action.params["endX"] ?: "0",
                action.params["endY"] ?: "0"
            )
            WebActionType.ZOOM_IN -> DOMScraperBridge.zoomScript(selector, "in")
            WebActionType.ZOOM_OUT -> DOMScraperBridge.zoomScript(selector, "out")

            // ═══════════════════════════════════════════════════════════════
            // Text/Clipboard Actions
            // ═══════════════════════════════════════════════════════════════
            WebActionType.SELECT_ALL -> DOMScraperBridge.selectAllScript()
            WebActionType.COPY -> DOMScraperBridge.copyScript()
            WebActionType.CUT -> DOMScraperBridge.cutScript()
            WebActionType.PASTE -> DOMScraperBridge.pasteScript(action.text)
        }
    }

    private fun parseResult(resultStr: String): WebActionResult {
        return try {
            val jsonObj = json.decodeFromString<JsonObject>(resultStr)
            val success = jsonObj["success"]?.jsonPrimitive?.boolean ?: false
            val message = jsonObj["message"]?.jsonPrimitive?.content ?: ""
            WebActionResult(success, message)
        } catch (e: Exception) {
            WebActionResult(false, "Failed to parse JS result: ${e.message}")
        }
    }

    private fun escapeSelectorForJs(selector: String): String {
        return selector
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
    }
}

/**
 * Proxy that delegates JS evaluation to BrowserVoiceOSCallback's internal executor.
 */
private class CallbackJsExecutorProxy(
    private val callback: BrowserVoiceOSCallback
) : IJavaScriptExecutor {
    override suspend fun evaluateJavaScript(script: String): String? {
        return callback.evaluateJavaScript(script)
    }
}

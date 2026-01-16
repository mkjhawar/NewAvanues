package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType

/**
 * Handler for WebView-based hybrid apps.
 *
 * Handles Cordova, Capacitor, PWAs, and other WebView-based apps.
 * Web content exposed through accessibility has unique patterns.
 */
class WebViewHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.WEBVIEW

    private val webViewPrefixes = listOf(
        "android.webkit.",
        "org.chromium.",
        "org.xwalk.",
        "org.apache.cordova."
    )

    private val webViewClasses = setOf(
        "WebView",
        "WebViewClient",
        "WebChromeClient",
        "XWalkView",
        "CordovaWebView",
        "CapacitorWebView"
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            webViewPrefixes.any { prefix ->
                element.className.startsWith(prefix)
            } || webViewClasses.any { cls ->
                element.className.contains(cls, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantWebElement(it) }
    }

    override fun getSelectors(): List<String> {
        return webViewPrefixes + webViewClasses.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        // Web elements with roles or labels are actionable
        return element.isClickable ||
               element.contentDescription.isNotBlank() ||
               element.text.isNotBlank()
    }

    override fun getPriority(): Int = 70 // Medium priority

    /**
     * Check if element is a relevant web element.
     */
    private fun isRelevantWebElement(element: ElementInfo): Boolean {
        // Skip WebView container itself for content
        if (element.className == "android.webkit.WebView") return true

        // Include elements with accessible content
        return element.hasVoiceContent || element.isActionable
    }

    /**
     * Check if this is the main WebView container.
     */
    fun isWebViewContainer(element: ElementInfo): Boolean {
        return element.className.contains("WebView", ignoreCase = true)
    }

    /**
     * Get web content elements inside WebView.
     */
    fun getWebContentElements(elements: List<ElementInfo>): List<ElementInfo> {
        val webViewIndex = elements.indexOfFirst { isWebViewContainer(it) }
        if (webViewIndex < 0) return emptyList()

        // Elements after WebView are web content
        return elements.drop(webViewIndex + 1)
            .filter { it.hasVoiceContent || it.isActionable }
    }

    /**
     * Detect if WebView is using Cordova.
     */
    fun isCordova(elements: List<ElementInfo>): Boolean {
        return elements.any {
            it.className.contains("cordova", ignoreCase = true)
        }
    }

    /**
     * Detect if WebView is using Capacitor.
     */
    fun isCapacitor(elements: List<ElementInfo>): Boolean {
        return elements.any {
            it.className.contains("capacitor", ignoreCase = true)
        }
    }

    /**
     * Get the HTML element role from accessibility info.
     */
    fun getHtmlRole(element: ElementInfo): String {
        val desc = element.contentDescription.lowercase()
        return when {
            desc.contains("button") -> "button"
            desc.contains("link") -> "link"
            desc.contains("input") -> "input"
            desc.contains("heading") -> "heading"
            desc.contains("list") -> "list"
            desc.contains("image") -> "img"
            else -> "element"
        }
    }
}

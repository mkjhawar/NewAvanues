package com.augmentalis.webavanue

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView

/**
 * ContextMenuHandler - Android-specific handler for WebView context menu
 *
 * Uses WebView.HitTestResult to detect what element was long-pressed
 * and creates the appropriate ContextMenuTarget.
 */
object ContextMenuHandler {

    /**
     * Detects the target from a WebView long-press hit test
     */
    fun detectTarget(webView: WebView): ContextMenuTarget {
        val hitTestResult = webView.hitTestResult
        val type = hitTestResult.type
        val extra = hitTestResult.extra

        return ContextMenuTarget.fromHitTest(type, extra)
    }

    /**
     * Detects target and also extracts link URL for image links
     */
    fun detectTargetWithLinkUrl(
        webView: WebView,
        x: Float,
        y: Float,
        callback: (ContextMenuTarget) -> Unit
    ) {
        val hitTestResult = webView.hitTestResult
        val type = hitTestResult.type
        val extra = hitTestResult.extra

        when (type) {
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                val script = """
                    (function() {
                        var el = document.elementFromPoint($x, $y);
                        while (el && el.tagName !== 'A') {
                            el = el.parentElement;
                        }
                        return el ? el.href : '';
                    })();
                """.trimIndent()

                webView.evaluateJavascript(script) { result ->
                    val linkUrl = result?.trim('"') ?: ""
                    callback(ContextMenuTarget.ImageLink(
                        linkUrl = linkUrl,
                        imageUrl = extra ?: ""
                    ))
                }
            }
            else -> {
                callback(ContextMenuTarget.fromHitTest(type, extra))
            }
        }
    }

    /**
     * Handles a context menu action
     */
    fun handleAction(
        context: Context,
        action: ContextMenuAction,
        target: ContextMenuTarget,
        callbacks: ContextMenuCallbacks
    ) {
        when (action) {
            ContextMenuAction.OPEN_LINK -> {
                (target as? ContextMenuTarget.Link)?.let { callbacks.onNavigate(it.url) }
            }
            ContextMenuAction.OPEN_LINK_NEW_TAB -> {
                (target as? ContextMenuTarget.Link)?.let { callbacks.onOpenNewTab(it.url, isIncognito = false) }
            }
            ContextMenuAction.OPEN_LINK_INCOGNITO -> {
                (target as? ContextMenuTarget.Link)?.let { callbacks.onOpenNewTab(it.url, isIncognito = true) }
            }
            ContextMenuAction.COPY_LINK -> {
                (target as? ContextMenuTarget.Link)?.let { copyToClipboard(context, "Link URL", it.url) }
            }
            ContextMenuAction.SHARE_LINK -> {
                (target as? ContextMenuTarget.Link)?.let { shareText(context, it.url, "Share link") }
            }
            ContextMenuAction.DOWNLOAD_LINK -> {
                (target as? ContextMenuTarget.Link)?.let { callbacks.onDownload(it.url) }
            }
            ContextMenuAction.OPEN_IMAGE -> {
                (target as? ContextMenuTarget.Image)?.let { callbacks.onNavigate(it.url) }
            }
            ContextMenuAction.OPEN_IMAGE_NEW_TAB -> {
                (target as? ContextMenuTarget.Image)?.let { callbacks.onOpenNewTab(it.url, isIncognito = false) }
            }
            ContextMenuAction.COPY_IMAGE -> {
                (target as? ContextMenuTarget.Image)?.let { callbacks.onCopyImage(it.url) }
            }
            ContextMenuAction.COPY_IMAGE_URL -> {
                (target as? ContextMenuTarget.Image)?.let { copyToClipboard(context, "Image URL", it.url) }
            }
            ContextMenuAction.SAVE_IMAGE -> {
                (target as? ContextMenuTarget.Image)?.let { callbacks.onDownload(it.url) }
            }
            ContextMenuAction.SHARE_IMAGE -> {
                (target as? ContextMenuTarget.Image)?.let { shareText(context, it.url, "Share image") }
            }
            ContextMenuAction.SET_AS_WALLPAPER -> {
                (target as? ContextMenuTarget.Image)?.let { callbacks.onSetWallpaper(it.url) }
            }
            ContextMenuAction.COPY_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let { copyToClipboard(context, "Selected text", it.text) }
            }
            ContextMenuAction.SHARE_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let { shareText(context, it.text, "Share text") }
            }
            ContextMenuAction.SEARCH_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let { callbacks.onNavigate(it.searchUrl()) }
            }
            ContextMenuAction.TRANSLATE_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let {
                    val url = "https://translate.google.com/?text=${java.net.URLEncoder.encode(it.text, "UTF-8")}"
                    callbacks.onOpenNewTab(url, isIncognito = false)
                }
            }
            ContextMenuAction.DEFINE_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let {
                    val url = "https://www.google.com/search?q=define:${java.net.URLEncoder.encode(it.text, "UTF-8")}"
                    callbacks.onNavigate(url)
                }
            }
            ContextMenuAction.SPEAK_TEXT -> {
                (target as? ContextMenuTarget.Selection)?.let { callbacks.onSpeak(it.text) }
            }
            ContextMenuAction.RELOAD_PAGE -> callbacks.onReload()
            ContextMenuAction.SAVE_PAGE -> callbacks.onSavePage()
            ContextMenuAction.SHARE_PAGE -> callbacks.onSharePage()
            ContextMenuAction.ADD_TO_FAVORITES -> callbacks.onAddToFavorites()
            ContextMenuAction.PRINT_PAGE -> callbacks.onPrint()
            ContextMenuAction.VIEW_SOURCE -> callbacks.onViewSource()
            ContextMenuAction.FIND_IN_PAGE -> callbacks.onFindInPage()
            ContextMenuAction.ADD_TO_READING_LIST -> callbacks.onAddToReadingList()
            ContextMenuAction.OPEN_IN_READER_MODE -> callbacks.onOpenReaderMode()
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

/**
 * Callbacks for context menu actions
 */
interface ContextMenuCallbacks {
    fun onNavigate(url: String)
    fun onOpenNewTab(url: String, isIncognito: Boolean)
    fun onDownload(url: String)
    fun onCopyImage(url: String)
    fun onSetWallpaper(url: String)
    fun onSpeak(text: String)
    fun onReload()
    fun onSavePage()
    fun onSharePage()
    fun onAddToFavorites()
    fun onPrint()
    fun onViewSource()
    fun onFindInPage()
    fun onAddToReadingList()
    fun onOpenReaderMode()
}

package com.augmentalis.webavanue

import kotlinx.serialization.Serializable

/**
 * ContextMenuTarget - Sealed class representing different targets for context menu
 *
 * When a user long-presses on a web page, the context menu needs to know what
 * type of element was pressed to show appropriate actions:
 * - Link: Open, copy, share, save
 * - Image: Open, copy, save, share
 * - ImageLink: Actions for both the image and the link it wraps
 * - Selection: Copy, search, share text
 * - Page: General page actions (reload, bookmark, share)
 */
sealed class ContextMenuTarget {

    @Serializable
    data class Link(
        val url: String,
        val text: String
    ) : ContextMenuTarget() {
        fun displayText(): String = text.ifBlank { url }
        fun isEmail(): Boolean = url.startsWith("mailto:")
        fun isPhone(): Boolean = url.startsWith("tel:")
        fun displayUrl(): String = url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .take(50)
            .let { if (it.length >= 50) "$it..." else it }
    }

    @Serializable
    data class Image(
        val url: String
    ) : ContextMenuTarget() {
        fun getFilename(): String {
            return url.substringAfterLast("/")
                .substringBefore("?")
                .ifBlank { "image.jpg" }
        }
        fun isDataUrl(): Boolean = url.startsWith("data:")
        fun displayUrl(): String {
            if (isDataUrl()) return "Embedded image"
            return url
                .removePrefix("https://")
                .removePrefix("http://")
                .take(50)
                .let { if (it.length >= 50) "$it..." else it }
        }
    }

    @Serializable
    data class ImageLink(
        val linkUrl: String,
        val imageUrl: String
    ) : ContextMenuTarget() {
        fun asLink(): Link = Link(url = linkUrl, text = "")
        fun asImage(): Image = Image(url = imageUrl)
    }

    @Serializable
    data class Selection(
        val text: String
    ) : ContextMenuTarget() {
        fun preview(): String {
            val trimmed = text.trim()
            return if (trimmed.length > 100) "${trimmed.take(100)}..." else trimmed
        }
        fun wordCount(): Int = text.trim().split(Regex("\\s+")).size
        fun charCount(): Int = text.length
        fun searchUrl(engine: SearchEngine = SearchEngine.GOOGLE): String {
            val query = java.net.URLEncoder.encode(text.trim(), "UTF-8")
            return when (engine) {
                SearchEngine.GOOGLE -> "https://www.google.com/search?q=$query"
                SearchEngine.BING -> "https://www.bing.com/search?q=$query"
                SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q=$query"
                SearchEngine.ECOSIA -> "https://www.ecosia.org/search?q=$query"
            }
        }
    }

    @Serializable
    data object Page : ContextMenuTarget()

    @Serializable
    data object Unknown : ContextMenuTarget()

    companion object {
        fun fromHitTest(type: Int, extra: String?): ContextMenuTarget {
            return when (type) {
                0 -> Page  // UNKNOWN_TYPE
                5 -> extra?.let { Link(url = it, text = "") } ?: Page  // ANCHOR_TYPE
                7 -> extra?.let { Image(url = it) } ?: Page  // IMAGE_TYPE
                8 -> extra?.let { ImageLink(linkUrl = "", imageUrl = it) } ?: Page  // SRC_IMAGE_ANCHOR_TYPE
                9 -> extra?.let { Link(url = it, text = "") } ?: Page  // SRC_ANCHOR_TYPE
                3 -> extra?.let { Link(url = "mailto:$it", text = it) } ?: Page  // EMAIL_TYPE
                else -> Page
            }
        }
    }
}

enum class SearchEngine { GOOGLE, BING, DUCKDUCKGO, ECOSIA }

enum class ContextMenuAction {
    OPEN_LINK, OPEN_LINK_NEW_TAB, OPEN_LINK_INCOGNITO, COPY_LINK, SHARE_LINK, DOWNLOAD_LINK,
    OPEN_IMAGE, OPEN_IMAGE_NEW_TAB, COPY_IMAGE, COPY_IMAGE_URL, SAVE_IMAGE, SHARE_IMAGE, SET_AS_WALLPAPER,
    COPY_TEXT, SHARE_TEXT, SEARCH_TEXT, TRANSLATE_TEXT, DEFINE_TEXT, SPEAK_TEXT,
    RELOAD_PAGE, SAVE_PAGE, SHARE_PAGE, ADD_TO_FAVORITES, PRINT_PAGE, VIEW_SOURCE, FIND_IN_PAGE,
    ADD_TO_READING_LIST, OPEN_IN_READER_MODE
}

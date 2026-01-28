/**
 * ExtractionSource.kt - Source types for element extraction
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscore

/**
 * Identifies the source of extracted elements.
 */
enum class ExtractionSource {
    /** Android accessibility service tree */
    ACCESSIBILITY,

    /** WebView JavaScript injection */
    WEBVIEW_JS,

    /** Chrome DevTools Protocol */
    CDP,

    /** HTML/DOM parser */
    HTML_PARSER,

    /** No source / unknown */
    NONE
}

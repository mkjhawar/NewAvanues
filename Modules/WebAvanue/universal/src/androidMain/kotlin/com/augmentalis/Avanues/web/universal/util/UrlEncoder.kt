package com.augmentalis.Avanues.web.universal.util

import java.net.URLEncoder

/**
 * Android implementation of URL encoding
 *
 * Uses java.net.URLEncoder which is available on Android (JVM).
 *
 * @param value The string to encode
 * @param encoding The character encoding to use (default: "UTF-8")
 * @return The URL-encoded string
 */
actual fun encodeUrl(value: String, encoding: String): String {
    return URLEncoder.encode(value, encoding)
}

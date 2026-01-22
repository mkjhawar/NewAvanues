package com.augmentalis.webavanue

/**
 * Cross-platform URL encoding
 *
 * Encodes a string for safe use in URL query parameters by percent-encoding
 * special characters according to RFC 3986.
 *
 * @param value The string to encode
 * @param encoding The character encoding to use (default: "UTF-8")
 * @return The URL-encoded string
 */
expect fun encodeUrl(value: String, encoding: String = "UTF-8"): String

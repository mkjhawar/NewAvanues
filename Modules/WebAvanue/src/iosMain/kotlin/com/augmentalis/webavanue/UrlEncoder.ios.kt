package com.augmentalis.webavanue

import platform.Foundation.*

/**
 * iOS URL encoding implementation using NSString
 */
actual fun encodeUrl(value: String, encoding: String): String {
    val nsString = value as NSString
    val encoded = nsString.stringByAddingPercentEncodingWithAllowedCharacters(
        NSCharacterSet.URLQueryAllowedCharacterSet
    )
    return encoded ?: value
}

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun readResource(path: String): String? = try {
    val bundle = NSBundle.mainBundle
    val lastDot = path.lastIndexOf('.')
    val (name, ext) = if (lastDot > 0) path.substring(0, lastDot) to path.substring(lastDot + 1) else path to ""
    val resourcePath = bundle.pathForResource(name, ext)
    if (resourcePath != null) NSString.stringWithContentsOfFile(resourcePath, NSUTF8StringEncoding, null) else null
} catch (_: Exception) { null }

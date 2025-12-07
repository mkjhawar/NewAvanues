package com.augmentalis.magiccode.plugins.assets

import java.io.File
import java.io.IOException

/**
 * Android implementation of AssetHandle using java.io.File.
 */
actual fun AssetHandle.isValid(): Boolean {
    return File(absolutePath).exists()
}

actual fun AssetHandle.readBytes(): ByteArray {
    val file = File(absolutePath)
    if (!file.exists()) {
        throw IOException("Asset file not found: $absolutePath")
    }
    return file.readBytes()
}

actual fun AssetHandle.readText(): String {
    val file = File(absolutePath)
    if (!file.exists()) {
        throw IOException("Asset file not found: $absolutePath")
    }
    return file.readText(Charsets.UTF_8)
}

actual fun AssetHandle.getPlatformHandle(): Any {
    return File(absolutePath)
}

package com.augmentalis.magiccode.plugins.assets

import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of AssetHandle using Foundation.NSFileManager.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun AssetHandle.isValid(): Boolean {
    return NSFileManager.defaultManager.fileExistsAtPath(absolutePath)
}

@OptIn(ExperimentalForeignApi::class)
actual fun AssetHandle.readBytes(): ByteArray {
    val data = NSData.dataWithContentsOfFile(absolutePath)
        ?: throw Exception("Failed to read asset file: $absolutePath")

    val bytes = data.bytes
        ?: throw Exception("No data available for asset: $absolutePath")

    return ByteArray(data.length.toInt()) { index ->
        bytes[index].toByte()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun AssetHandle.readText(): String {
    val data = NSData.dataWithContentsOfFile(absolutePath)
        ?: throw Exception("Failed to read asset file: $absolutePath")

    return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        ?: throw Exception("Failed to decode asset as UTF-8: $absolutePath")
}

@OptIn(ExperimentalForeignApi::class)
actual fun AssetHandle.getPlatformHandle(): Any {
    return NSURL.fileURLWithPath(absolutePath)
}

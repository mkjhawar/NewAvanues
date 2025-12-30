package com.augmentalis.ava.core.data.util

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.*
import kotlinx.cinterop.*

/**
 * iOS implementation of HashHelper
 */
actual object HashHelper {
    @OptIn(ExperimentalForeignApi::class)
    actual fun md5(input: String): String {
        val nsString = NSString.create(string = input)
        val data = nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: return ""

        val digestLength = CC_MD5_DIGEST_LENGTH
        val result = UByteArray(digestLength)

        result.usePinned { pinnedResult ->
            CC_MD5(data.bytes, data.length.toUInt(), pinnedResult.addressOf(0))
        }

        return result.joinToString("") { it.toString(16).padStart(2, '0') }
    }
}

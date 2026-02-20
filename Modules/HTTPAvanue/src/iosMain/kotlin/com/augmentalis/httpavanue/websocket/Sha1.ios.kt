@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.websocket

import kotlinx.cinterop.*
import platform.CoreCrypto.CC_SHA1
import platform.CoreCrypto.CC_SHA1_DIGEST_LENGTH

actual fun sha1(data: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA1_DIGEST_LENGTH)
    data.usePinned { dataPinned ->
        digest.usePinned { digestPinned ->
            CC_SHA1(dataPinned.addressOf(0), data.size.toUInt(), digestPinned.addressOf(0).reinterpret())
        }
    }
    return digest
}

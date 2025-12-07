package com.augmentalis.magiccode.plugins.assets

import kotlinx.cinterop.*
import platform.CoreCrypto.*

/**
 * iOS implementation of ChecksumCalculator using CommonCrypto framework.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ChecksumCalculator {
    actual fun calculateMD5(data: ByteArray): String {
        val digest = ByteArray(CC_MD5_DIGEST_LENGTH)

        data.usePinned { pinnedData ->
            digest.usePinned { pinnedDigest ->
                CC_MD5(pinnedData.addressOf(0), data.size.toUInt(), pinnedDigest.addressOf(0))
            }
        }

        return digest.joinToString("") { "%02x".format(it) }
    }

    actual fun calculateSHA256(data: ByteArray): String {
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)

        data.usePinned { pinnedData ->
            digest.usePinned { pinnedDigest ->
                CC_SHA256(pinnedData.addressOf(0), data.size.toUInt(), pinnedDigest.addressOf(0))
            }
        }

        return digest.joinToString("") { "%02x".format(it) }
    }
}

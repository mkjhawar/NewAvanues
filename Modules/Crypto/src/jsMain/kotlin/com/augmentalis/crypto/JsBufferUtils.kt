/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.crypto

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

/**
 * Shared JS buffer conversion utilities.
 *
 * Used by both AONCodec and CryptoDigest in jsMain to convert
 * between Kotlin ByteArray and JS-specific buffer types.
 */
internal object JsBufferUtils {

    /** Detect Node.js runtime (vs browser) */
    val isNodeJs: Boolean = js(
        "typeof process !== 'undefined' && typeof process.versions !== 'undefined' && typeof process.versions.node !== 'undefined'"
    ) as Boolean

    /** Convert Kotlin ByteArray to JS ArrayBuffer (browser) */
    fun toArrayBuffer(data: ByteArray): ArrayBuffer {
        val uint8 = Uint8Array(data.size)
        for (i in data.indices) {
            uint8.asDynamic()[i] = data[i]
        }
        return uint8.buffer
    }

    /** Convert JS ArrayBuffer to Kotlin ByteArray */
    fun fromArrayBuffer(buffer: ArrayBuffer): ByteArray {
        val uint8 = Uint8Array(buffer)
        return ByteArray(uint8.length) { i ->
            (uint8.asDynamic()[i] as Int).toByte()
        }
    }

    /** Convert Kotlin ByteArray to Node.js Buffer */
    fun toNodeBuffer(data: ByteArray): dynamic {
        val buffer = js("Buffer")
        return buffer.from(data.toTypedArray())
    }

    /** Convert Node.js Buffer to Kotlin ByteArray */
    fun fromNodeBuffer(buf: dynamic): ByteArray {
        val length = buf.length as Int
        return ByteArray(length) { i -> (buf[i] as Number).toByte() }
    }
}

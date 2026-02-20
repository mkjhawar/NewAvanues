@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.UnsafeNumber::class)

package com.augmentalis.httpavanue.middleware

import kotlinx.cinterop.*
import platform.zlib.*

actual fun gzipCompress(data: ByteArray): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    val maxOutputSize = data.size + (data.size / 10) + 12
    val output = ByteArray(maxOutputSize)
    memScoped {
        val stream = alloc<z_stream>()
        stream.zalloc = null; stream.zfree = null; stream.opaque = null
        val initResult = deflateInit2(stream.ptr, Z_DEFAULT_COMPRESSION, Z_DEFLATED, 15 + 16, 8, Z_DEFAULT_STRATEGY)
        if (initResult != Z_OK) throw RuntimeException("Failed to initialize gzip compression: $initResult")
        try {
            data.usePinned { inputPinned -> output.usePinned { outputPinned ->
                stream.next_in = inputPinned.addressOf(0).reinterpret()
                stream.avail_in = data.size.toUInt()
                stream.next_out = outputPinned.addressOf(0).reinterpret()
                stream.avail_out = maxOutputSize.toUInt()
                val deflateResult = deflate(stream.ptr, Z_FINISH)
                if (deflateResult != Z_STREAM_END) throw RuntimeException("Gzip compression failed: $deflateResult")
            }}
            return output.copyOf(maxOutputSize - stream.avail_out.toInt())
        } finally { deflateEnd(stream.ptr) }
    }
}

actual fun gzipDecompress(data: ByteArray): ByteArray {
    if (data.isEmpty()) return ByteArray(0)
    var outputSize = data.size * 4; var output = ByteArray(outputSize)
    return memScoped {
        val stream = alloc<z_stream>()
        stream.zalloc = null; stream.zfree = null; stream.opaque = null
        val initResult = inflateInit2(stream.ptr, 15 + 16)
        if (initResult != Z_OK) throw RuntimeException("Failed to initialize gzip decompression: $initResult")
        try {
            data.usePinned { inputPinned -> stream.next_in = inputPinned.addressOf(0).reinterpret(); stream.avail_in = data.size.toUInt() }
            var totalOutput = 0; var finished = false
            while (!finished) {
                if (totalOutput >= output.size) { output = output.copyOf(output.size * 2); outputSize = output.size }
                output.usePinned { outputPinned ->
                    stream.next_out = outputPinned.addressOf(totalOutput).reinterpret()
                    stream.avail_out = (outputSize - totalOutput).toUInt()
                    when (inflate(stream.ptr, Z_NO_FLUSH)) {
                        Z_STREAM_END -> { totalOutput = outputSize - stream.avail_out.toInt(); finished = true }
                        Z_OK, Z_BUF_ERROR -> totalOutput = outputSize - stream.avail_out.toInt()
                        else -> throw RuntimeException("Gzip decompression failed")
                    }
                }
            }
            output.copyOf(totalOutput)
        } finally { inflateEnd(stream.ptr) }
    }
}

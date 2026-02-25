package com.augmentalis.httpavanue.middleware

/**
 * JS actual for gzip compression/decompression.
 *
 * The CompressionMiddleware is server-side functionality for compressing
 * HTTP responses. Browsers handle content-encoding transparently at the
 * transport layer — they decompress gzip responses automatically.
 *
 * If gzip is needed in Node.js, use the Node `zlib` module directly.
 */
actual fun gzipCompress(data: ByteArray): ByteArray {
    throw UnsupportedOperationException(
        "Gzip compression is a server-side feature. " +
            "Browsers handle Content-Encoding transparently at the transport layer."
    )
}

actual fun gzipDecompress(data: ByteArray): ByteArray {
    throw UnsupportedOperationException(
        "Gzip decompression is a server-side feature. " +
            "Browsers handle Content-Encoding transparently at the transport layer."
    )
}

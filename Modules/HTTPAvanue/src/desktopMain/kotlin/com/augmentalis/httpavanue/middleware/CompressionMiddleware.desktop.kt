package com.augmentalis.httpavanue.middleware

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun gzipCompress(data: ByteArray): ByteArray {
    val outputStream = ByteArrayOutputStream()
    GZIPOutputStream(outputStream).use { it.write(data) }
    return outputStream.toByteArray()
}

actual fun gzipDecompress(data: ByteArray): ByteArray =
    GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }

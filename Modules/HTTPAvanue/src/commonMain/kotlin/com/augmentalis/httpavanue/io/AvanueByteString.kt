@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.augmentalis.httpavanue.io

import com.augmentalis.httpavanue.websocket.sha1
import kotlin.io.encoding.Base64

/**
 * ByteArray extension utilities — replaces okio.ByteString operations.
 *
 * Provides Base64 encoding/decoding and SHA-1 hashing used by
 * WebSocket handshake and HTTP authentication.
 */

/** Encode this byte array to a Base64 string. Replaces `ByteString.base64()`. */
fun ByteArray.toBase64(): String = Base64.encode(this)

/** Decode a Base64 string to a byte array. Replaces `ByteString.decodeBase64()`. */
fun String.fromBase64(): ByteArray = Base64.decode(this)

/**
 * Compute SHA-1 digest of this byte array.
 * Delegates to the existing platform expect/actual `sha1()` function
 * in the websocket package.
 */
fun ByteArray.sha1Digest(): ByteArray = sha1(this)

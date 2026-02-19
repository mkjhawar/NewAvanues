package com.augmentalis.httpavanue.websocket

import java.security.MessageDigest

actual fun sha1(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(data)

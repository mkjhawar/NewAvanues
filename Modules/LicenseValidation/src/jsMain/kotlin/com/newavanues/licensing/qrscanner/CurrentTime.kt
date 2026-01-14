package com.newavanues.licensing.qrscanner

import kotlin.js.Date

/**
 * JavaScript implementation of currentTimeMillis using Date.now().
 */
internal actual fun currentTimeMillis(): Long = Date.now().toLong()

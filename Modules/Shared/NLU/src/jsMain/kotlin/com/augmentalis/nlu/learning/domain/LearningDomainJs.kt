package com.augmentalis.nlu.learning.domain

import kotlin.js.Date

/**
 * JavaScript implementation of platform utilities
 */
actual fun currentTimeMillis(): Long = Date.now().toLong()

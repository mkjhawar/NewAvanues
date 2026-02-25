package com.augmentalis.httpavanue.platform

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()

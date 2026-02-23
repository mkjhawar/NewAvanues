package com.augmentalis.netavanue.ice.candidate

import kotlin.js.Date

internal actual fun currentTimeMillis(): Long = Date.now().toLong()

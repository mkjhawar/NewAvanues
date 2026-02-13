/**
 * CursorFilterPlatform.kt - iOS platform implementation
 *
 * Uses kotlinx.datetime for portable time — no Foundation dependency needed.
 */

package com.augmentalis.voicecursor.filter

import kotlinx.datetime.Clock

internal actual fun currentTimeMillis(): Long =
    Clock.System.now().toEpochMilliseconds()

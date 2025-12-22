/**
 * AppEntity.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.models

data class AppEntity(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String? = null,
    val isLearned: Boolean = false,
    val lastLearned: Long? = null
)

/**
 * AppEntity.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.models

import android.graphics.drawable.Drawable

data class AppEntity(
    val appId: String = "",
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val versionCode: Long,
    val versionName: String? = null,
    val installTime: Long = 0,
    val updateTime: Long = 0,
    val isFullyLearned: Boolean? = false,
    val exploredElementCount: Int = 0,
    val scrapedElementCount: Int? = 0,
    val totalScreens: Int = 0,
    val lastExplored: Long? = null,
    val lastScraped: Long? = null,
    val learnAppEnabled: Boolean = true,
    val dynamicScrapingEnabled: Boolean? = false,
    val maxScrapeDepth: Int = 5
)

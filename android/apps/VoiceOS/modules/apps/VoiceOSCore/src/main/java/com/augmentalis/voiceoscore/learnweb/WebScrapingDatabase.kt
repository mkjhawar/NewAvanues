package com.augmentalis.voiceoscore.learnweb

import android.content.Context

/**
 * Stub for WebScrapingDatabase
 * Original: Room database for web-scraped commands
 *
 * Phase 1 Quick Fix: Stub implementation to unblock compilation
 * Full implementation to be restored by Agent 3
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Agent 1 (Service Layer Restorer)
 * Created: 2025-11-26
 */
class WebScrapingDatabase private constructor() {

    fun generatedWebCommandDao(): GeneratedWebCommandDao {
        return GeneratedWebCommandDaoStub()
    }

    companion object {
        @Volatile
        private var INSTANCE: WebScrapingDatabase? = null

        fun getInstance(context: Context): WebScrapingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebScrapingDatabase().also { INSTANCE = it }
            }
        }
    }
}

interface GeneratedWebCommandDao {
    fun getAllCommands(): List<WebCommand>
}

class GeneratedWebCommandDaoStub : GeneratedWebCommandDao {
    override fun getAllCommands(): List<WebCommand> {
        return emptyList() // Stub - no web commands
    }
}

data class WebCommand(
    val commandText: String
)

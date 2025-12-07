/*
 * MainActivity.kt - VoiceOSCore Launcher Activity
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-16
 *
 * Purpose: Provides user-friendly entry point for enabling accessibility service.
 * When launched, automatically opens Accessibility Settings and finishes.
 */

package com.augmentalis.voiceoscore

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

/**
 * Launcher activity that opens Accessibility Settings.
 *
 * This activity serves as the app icon in the launcher. When tapped, it:
 * 1. Opens Android Accessibility Settings
 * 2. Immediately finishes (no UI shown)
 *
 * This provides a convenient way for users to enable the VoiceOS accessibility service
 * without manually navigating to Settings → Accessibility.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open Accessibility Settings
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            // Add flags to ensure Settings opens properly
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        startActivity(intent)

        // Finish immediately - no UI needed
        finish()
    }
}

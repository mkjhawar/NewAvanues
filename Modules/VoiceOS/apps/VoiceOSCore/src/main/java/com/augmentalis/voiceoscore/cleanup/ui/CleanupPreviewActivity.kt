/**
 * CleanupPreviewActivity.kt - Activity wrapper for cleanup preview screen
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Activity wrapper providing ViewModel lifecycle and result handling
 */

package com.augmentalis.voiceoscore.cleanup.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.augmentalis.voiceoscore.cleanup.CleanupManager
import com.augmentalis.voiceoscore.di.DatabaseProvider
import com.augmentalis.voiceoscore.ui.setContentWithScrollSupport

/**
 * Activity hosting the cleanup preview screen.
 * Uses manual dependency injection and returns result code on completion.
 *
 * Note: Hilt is not available in VoiceOSCore module (doesn't support AccessibilityService)
 */
class CleanupPreviewActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CleanupPreviewActivity"

        const val EXTRA_DELETED_COUNT = "extra_deleted_count"
        const val EXTRA_PRESERVED_COUNT = "extra_preserved_count"
        const val EXTRA_DURATION_MS = "extra_duration_ms"

        /**
         * Create intent to launch cleanup preview
         *
         * @param context Context for intent creation
         * @return Configured intent
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, CleanupPreviewActivity::class.java)
        }
    }

    private lateinit var viewModel: CleanupPreviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies manually (Hilt not available)
        val commandRepo = DatabaseProvider.provideGeneratedCommandRepository(application)
        val cleanupManager = CleanupManager(commandRepo)

        // Create ViewModel with factory
        viewModel = ViewModelProvider(
            this,
            CleanupPreviewViewModelFactory(cleanupManager, commandRepo, application)
        )[CleanupPreviewViewModel::class.java]

        Log.i(TAG, "Cleanup preview activity started")

        setContentWithScrollSupport {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CleanupPreviewScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            Log.i(TAG, "User cancelled cleanup")
                            setResult(RESULT_CANCELED)
                            finish()
                        },
                        onCleanupComplete = { deletedCount ->
                            handleCleanupComplete(deletedCount)
                        }
                    )
                }
            }
        }
    }

    /**
     * Handle successful cleanup completion
     *
     * @param deletedCount Number of commands deleted
     */
    private fun handleCleanupComplete(deletedCount: Int) {
        Log.i(TAG, "Cleanup completed: $deletedCount commands deleted")

        // Save last cleanup info to SharedPreferences
        val prefs = getSharedPreferences("voiceos_cleanup", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("last_cleanup_timestamp", System.currentTimeMillis())
            putInt("last_cleanup_deleted", deletedCount)
            apply()
        }

        // Show success toast
        Toast.makeText(
            this,
            "Cleanup complete: $deletedCount commands deleted",
            Toast.LENGTH_SHORT
        ).show()

        // Set result with cleanup stats
        val resultIntent = Intent().apply {
            putExtra(EXTRA_DELETED_COUNT, deletedCount)
        }
        setResult(RESULT_OK, resultIntent)

        // Finish activity after brief delay to show success screen
        window.decorView.postDelayed({
            finish()
        }, 2000)  // 2 second delay to show success view
    }

    override fun onBackPressed() {
        // Prevent back press during execution
        val currentState = viewModel.uiState.value
        if (currentState is CleanupPreviewUiState.Executing) {
            Toast.makeText(
                this,
                "Cannot go back while cleanup is in progress",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        super.onBackPressed()
    }
}

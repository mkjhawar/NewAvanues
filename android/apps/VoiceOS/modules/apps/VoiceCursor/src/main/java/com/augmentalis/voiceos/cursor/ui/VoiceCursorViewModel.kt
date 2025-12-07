package com.augmentalis.voiceos.cursor.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.collections.any
import kotlin.jvm.java
import kotlin.text.equals
import kotlin.text.split

class VoiceCursorViewModel : ViewModel() {
    private val _ui = MutableStateFlow(PermissionUiState())
    val ui = _ui.asStateFlow()

    fun refresh(context: Context) {
        _ui.update {
            it.copy(
                overlayGranted = Settings.canDrawOverlays(context),
                accessibilityGranted = VoiceCursorAPI.isInitialized()
            )
        }
    }


    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "${context.packageName}/${service.canonicalName}"
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

}

data class PermissionUiState(
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false
)
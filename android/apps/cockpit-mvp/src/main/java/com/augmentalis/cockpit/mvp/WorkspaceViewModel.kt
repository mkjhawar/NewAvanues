package com.augmentalis.cockpit.mvp

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowType
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LinearHorizontalLayout
import java.util.UUID

class WorkspaceViewModel : ViewModel() {

    // Mutable list of windows
    val windows = mutableStateListOf<AppWindow>()

    // Layout preset
    private val layoutPreset = LinearHorizontalLayout

    init {
        // Initialize with 3 sample windows
        addWindow("Email", WindowType.ANDROID_APP, "#FF6B9D")
        addWindow("Browser", WindowType.WEB_APP, "#4ECDC4")
        addWindow("Calculator", WindowType.WIDGET, "#95E1D3")
    }

    fun addWindow(title: String, type: WindowType, color: String) {
        val window = AppWindow(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            sourceId = "demo.$title",
            position = Vector3D(0f, 0f, -2f),
            widthMeters = 0.8f,
            heightMeters = 0.6f,
            voiceName = title.lowercase()
        )
        windows.add(window)
    }

    fun removeWindow(windowId: String) {
        windows.removeAll { it.id == windowId }
    }

    fun getWindowPositions(): Map<String, Vector3D> {
        if (windows.isEmpty()) return emptyMap()

        val centerPoint = Vector3D(0f, 0f, -2f)
        val positions = layoutPreset.calculatePositions(windows, centerPoint)

        return positions.associate { it.windowId to it.position }
    }
}

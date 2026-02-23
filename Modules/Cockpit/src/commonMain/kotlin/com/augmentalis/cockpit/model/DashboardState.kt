package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * State for the Cockpit Dashboard (launcher/home view).
 * Shown when no session is active or user navigates "home".
 */
@Serializable
data class DashboardState(
    val recentSessions: List<CockpitSession> = emptyList(),
    val availableModules: List<DashboardModule> = emptyList(),
    val activeSession: CockpitSession? = null,
    val templates: List<SessionTemplate> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * A launchable module displayed as a tile in the Dashboard.
 */
@Serializable
data class DashboardModule(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val iconName: String,
    val contentType: String,
    val accentColorHex: Long = 0xFF2196F3L
)

/**
 * Registry of all modules available for launching from the Dashboard.
 * Provides the canonical list of Avanues ecosystem modules that can be
 * launched as Cockpit frames.
 */
object DashboardModuleRegistry {

    val coreModules: List<DashboardModule> = listOf(
        DashboardModule(
            id = "voiceavanue",
            displayName = "VoiceTouch\u2122",
            subtitle = "Voice commands",
            iconName = "mic",
            contentType = "voice"
        ),
        DashboardModule(
            id = "webavanue",
            displayName = "WebAvanue",
            subtitle = "Voice browser",
            iconName = "language",
            contentType = "web"
        ),
        DashboardModule(
            id = "voicecursor",
            displayName = "CursorAvanue",
            subtitle = "Cursor control",
            iconName = "mouse",
            contentType = "cursor"
        )
    )

    val contentModules: List<DashboardModule> = listOf(
        DashboardModule(
            id = "pdfavanue",
            displayName = "PDFAvanue",
            subtitle = "PDF viewer",
            iconName = "picture_as_pdf",
            contentType = "pdf",
            accentColorHex = 0xFFE53935L
        ),
        DashboardModule(
            id = "imageavanue",
            displayName = "ImageAvanue",
            subtitle = "Image viewer",
            iconName = "image",
            contentType = "image",
            accentColorHex = 0xFF43A047L
        ),
        DashboardModule(
            id = "videoavanue",
            displayName = "VideoAvanue",
            subtitle = "Video player",
            iconName = "videocam",
            contentType = "video",
            accentColorHex = 0xFFE91E63L
        ),
        DashboardModule(
            id = "noteavanue",
            displayName = "NoteAvanue",
            subtitle = "Rich notes",
            iconName = "edit_note",
            contentType = "note",
            accentColorHex = 0xFFFFA726L
        ),
        DashboardModule(
            id = "photoavanue",
            displayName = "PhotoAvanue",
            subtitle = "Camera",
            iconName = "photo_camera",
            contentType = "camera",
            accentColorHex = 0xFF7E57C2L
        ),
        DashboardModule(
            id = "remotecast",
            displayName = "CastAvanue",
            subtitle = "Screen cast",
            iconName = "cast",
            contentType = "screencast",
            accentColorHex = 0xFF26C6DAL
        ),
        DashboardModule(
            id = "annotationavanue",
            displayName = "DrawAvanue",
            subtitle = "Whiteboard",
            iconName = "draw",
            contentType = "whiteboard",
            accentColorHex = 0xFFEF5350L
        )
    )

    val allModules: List<DashboardModule> = coreModules + contentModules

    fun findById(id: String): DashboardModule? = allModules.find { it.id == id }
    fun findByContentType(contentType: String): DashboardModule? = allModules.find { it.contentType == contentType }
}

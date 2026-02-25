package com.augmentalis.cockpit.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content types that can be displayed inside a Cockpit frame.
 *
 * Each variant carries its own state data that gets serialized to JSON
 * for persistence in the CockpitFrame.contentData column.
 *
 * Architecture: Each content type has a corresponding ContentRenderer
 * implementation in androidMain (or platform-specific source sets)
 * that knows how to display and interact with it.
 */
@Serializable
sealed class FrameContent {

    /** Unique type identifier for DB storage and renderer lookup */
    abstract val typeId: String

    // ── P0: Core Content Types ───────────────────────────────────────

    /**
     * Web browser frame — delegates rendering to WebAvanue module.
     * Supports tabs, downloads, ad blocking, desktop mode, voice commands.
     */
    @Serializable
    @SerialName("web")
    data class Web(
        val url: String = "https://www.google.com",
        val scrollX: Int = 0,
        val scrollY: Int = 0,
        val desktopMode: Boolean = true,
        val zoomLevel: Float = 1.0f,
        val userAgent: String? = null,
    ) : FrameContent() {
        override val typeId: String = TYPE_WEB
    }

    /**
     * PDF document viewer — uses PDFAvanue KMP module.
     * Native rendering with page navigation, zoom, bookmarks, text search.
     */
    @Serializable
    @SerialName("pdf")
    data class Pdf(
        val uri: String = "",
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val zoom: Float = 1.0f,
        val scrollX: Float = 0f,
        val scrollY: Float = 0f,
    ) : FrameContent() {
        override val typeId: String = TYPE_PDF
    }

    /**
     * Image viewer with pan/zoom gestures.
     * Supports EXIF metadata display.
     */
    @Serializable
    @SerialName("image")
    data class Image(
        val uri: String = "",
        val zoom: Float = 1.0f,
        val panX: Float = 0f,
        val panY: Float = 0f,
        val showMetadata: Boolean = false,
    ) : FrameContent() {
        override val typeId: String = TYPE_IMAGE
    }

    /**
     * Video player with playback controls.
     * Supports local files and streaming URLs.
     */
    @Serializable
    @SerialName("video")
    data class Video(
        val uri: String = "",
        val playbackPositionMs: Long = 0L,
        val isPlaying: Boolean = false,
        val isMuted: Boolean = false,
        val playbackSpeed: Float = 1.0f,
    ) : FrameContent() {
        override val typeId: String = TYPE_VIDEO
    }

    /**
     * Rich note editor with auto-save.
     * Supports inline photos (camera or gallery), embedded document references,
     * and rich text formatting. Content stored as structured blocks.
     * For voice-dictated notes with transcription, use VoiceNote.
     */
    @Serializable
    @SerialName("note")
    data class Note(
        val markdownContent: String = "",
        val cursorPosition: Int = 0,
        val fontSize: Float = 14f,
        /** JSON array of NoteAttachment objects: photos, docs, sketches embedded in the note */
        val attachmentsJson: String = "[]",
    ) : FrameContent() {
        override val typeId: String = TYPE_NOTE
    }

    /**
     * Live camera feed with capture capability.
     * Uses CameraX on Android.
     */
    @Serializable
    @SerialName("camera")
    data class Camera(
        val zoom: Float = 1.0f,
        val lensFacing: CameraLens = CameraLens.BACK,
        val flashMode: FlashMode = FlashMode.OFF,
    ) : FrameContent() {
        override val typeId: String = TYPE_CAMERA
    }

    // ── P1: Extended Content Types ───────────────────────────────────

    /**
     * Voice note — record audio with live transcription via SpeechRecognition module.
     * Stores both the audio recording and the editable transcript.
     * Supports append mode (add to existing transcript) and playback synced to text.
     */
    @Serializable
    @SerialName("voice_note")
    data class VoiceNote(
        val audioUri: String = "",
        val transcript: String = "",
        val durationMs: Long = 0L,
        val isRecording: Boolean = false,
        val locale: String = "en-US",
    ) : FrameContent() {
        override val typeId: String = TYPE_VOICE_NOTE
    }

    /**
     * Interactive form — checklists, structured data fields, inspection forms.
     * Form schema defined by a list of typed fields.
     */
    @Serializable
    @SerialName("form")
    data class Form(
        val title: String = "New Form",
        val fieldsJson: String = "[]",
        val completedCount: Int = 0,
        val totalCount: Int = 0,
    ) : FrameContent() {
        override val typeId: String = TYPE_FORM
    }

    /**
     * E-Signature capture — touch/stylus signature pad for document signing workflows.
     * Stores the signature as a serialized path or PNG data URI.
     */
    @Serializable
    @SerialName("signature")
    data class Signature(
        val signatureData: String = "",
        val signerName: String = "",
        val timestamp: String = "",
        val isSigned: Boolean = false,
    ) : FrameContent() {
        override val typeId: String = TYPE_SIGNATURE
    }

    /**
     * Audio recorder/player — standalone voice recording without transcription.
     * For transcription use VoiceNote instead.
     */
    @Serializable
    @SerialName("voice")
    data class Voice(
        val audioUri: String = "",
        val durationMs: Long = 0L,
        val playbackPositionMs: Long = 0L,
        val isRecording: Boolean = false,
        val isPlaying: Boolean = false,
    ) : FrameContent() {
        override val typeId: String = TYPE_VOICE
    }

    // ── P2: Advanced Content Types ───────────────────────────────────

    /**
     * Map view — interactive map display (OpenStreetMap or platform maps).
     */
    @Serializable
    @SerialName("map")
    data class Map(
        val latitude: Double = 37.7749,
        val longitude: Double = -122.4194,
        val zoomLevel: Float = 12f,
        val mapType: String = "standard",
    ) : FrameContent() {
        override val typeId: String = TYPE_MAP
    }

    /**
     * Whiteboard — freeform drawing canvas for sketches, annotations, diagrams.
     * Supports stylus and finger input with pressure sensitivity.
     */
    @Serializable
    @SerialName("whiteboard")
    data class Whiteboard(
        val strokesJson: String = "[]",
        val backgroundColor: Long = 0xFFFFFFFF,
        val penColor: Long = 0xFF000000,
        val penWidth: Float = 3f,
    ) : FrameContent() {
        override val typeId: String = TYPE_WHITEBOARD
    }

    /**
     * Terminal / log viewer — displays scrollable text output.
     * Useful for viewing logs, command output, or streaming data.
     */
    @Serializable
    @SerialName("terminal")
    data class Terminal(
        val content: String = "",
        val autoScroll: Boolean = true,
        val fontSize: Float = 12f,
        val maxLines: Int = 10000,
    ) : FrameContent() {
        override val typeId: String = TYPE_TERMINAL
    }

    // ── Killer Features: AI & Collaboration ─────────────────────────

    /**
     * AI Summarizer frame — watches other frames and generates summaries.
     * Uses Modules/AI:LLM for text generation. Can summarize PDFs, web pages,
     * notes, or transcribe/summarize video audio.
     * sourceFrameIds: which frames to watch and summarize.
     */
    @Serializable
    @SerialName("ai_summary")
    data class AiSummary(
        val sourceFrameIds: List<String> = emptyList(),
        val summary: String = "",
        val summaryType: SummaryType = SummaryType.BRIEF,
        val autoRefresh: Boolean = true,
        val lastRefreshedAt: String = "",
    ) : FrameContent() {
        override val typeId: String = TYPE_AI_SUMMARY
    }

    /**
     * Screen Cast frame — mirrors another device's screen.
     * Uses Modules/ScreenCast for device mirroring via MediaProjection or gRPC stream.
     */
    @Serializable
    @SerialName("screen_cast")
    data class ScreenCast(
        val sourceDeviceId: String = "",
        val sourceDeviceName: String = "",
        val isConnected: Boolean = false,
        val quality: CastQuality = CastQuality.MEDIUM,
    ) : FrameContent() {
        override val typeId: String = TYPE_SCREEN_CAST
    }

    // ── File Management ──────────────────────────────────────────────

    /**
     * File browser — cross-platform file manager using FileAvanue module.
     * Supports local, cloud, and network storage providers.
     * Dashboard mode (blank path) shows categories; directory mode shows file listing.
     */
    @Serializable
    @SerialName("file")
    data class File(
        val path: String = "",
        val viewMode: String = "list",
        val sortMode: String = "name_asc",
        val providerId: String = "local",
    ) : FrameContent() {
        override val typeId: String = TYPE_FILE
    }

    // ── Mini Widgets ─────────────────────────────────────────────────

    /**
     * Mini widget frame — compact utility display.
     * Clock, timer, stopwatch, compass, battery, connection status, weather.
     * Designed for AR glasses where glanceable info is critical.
     */
    @Serializable
    @SerialName("widget")
    data class Widget(
        val widgetType: WidgetType = WidgetType.CLOCK,
        val configJson: String = "{}",
    ) : FrameContent() {
        override val typeId: String = TYPE_WIDGET
    }

    // ── External App Integration ─────────────────────────────────────

    /**
     * External (3rd-party) app frame — launches an installed app alongside Cockpit.
     *
     * The Cockpit checks if the target app is installed and whether it supports
     * activity embedding. If embedding is not supported, the app is launched
     * adjacent (split-screen on Android) via FLAG_ACTIVITY_LAUNCH_ADJACENT.
     *
     * @param packageName Android package name (e.g. "com.google.android.apps.maps")
     * @param activityName Optional launcher activity class name
     * @param label User-visible name for the app in the frame title bar
     */
    @Serializable
    @SerialName("external_app")
    data class ExternalApp(
        val packageName: String = "",
        val activityName: String = "",
        val label: String = "",
    ) : FrameContent() {
        override val typeId: String = TYPE_EXTERNAL_APP
    }

    companion object {
        const val TYPE_WEB = "web"
        const val TYPE_PDF = "pdf"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_NOTE = "note"
        const val TYPE_CAMERA = "camera"
        const val TYPE_VOICE_NOTE = "voice_note"
        const val TYPE_FORM = "form"
        const val TYPE_SIGNATURE = "signature"
        const val TYPE_VOICE = "voice"
        const val TYPE_MAP = "map"
        const val TYPE_WHITEBOARD = "whiteboard"
        const val TYPE_TERMINAL = "terminal"
        const val TYPE_AI_SUMMARY = "ai_summary"
        const val TYPE_SCREEN_CAST = "screen_cast"
        const val TYPE_WIDGET = "widget"
        const val TYPE_FILE = "file"
        const val TYPE_EXTERNAL_APP = "external_app"

        /** All type IDs for validation */
        val ALL_TYPES = listOf(
            TYPE_WEB, TYPE_PDF, TYPE_IMAGE, TYPE_VIDEO, TYPE_NOTE, TYPE_CAMERA,
            TYPE_VOICE_NOTE, TYPE_FORM, TYPE_SIGNATURE, TYPE_VOICE,
            TYPE_MAP, TYPE_WHITEBOARD, TYPE_TERMINAL,
            TYPE_AI_SUMMARY, TYPE_SCREEN_CAST, TYPE_WIDGET,
            TYPE_FILE, TYPE_EXTERNAL_APP,
        )
    }
}

/** Camera lens direction */
@Serializable
enum class CameraLens { BACK, FRONT }

/** Camera flash mode */
@Serializable
enum class FlashMode { OFF, ON, AUTO, TORCH }

/** AI Summary generation type */
@Serializable
enum class SummaryType {
    /** 2-3 sentence brief */
    BRIEF,
    /** Detailed bullet points */
    DETAILED,
    /** Key takeaways / action items */
    ACTION_ITEMS,
    /** Q&A format */
    QA,
}

/** Screen cast quality preset */
@Serializable
enum class CastQuality { LOW, MEDIUM, HIGH }

/** Mini widget types */
@Serializable
enum class WidgetType {
    CLOCK,
    TIMER,
    STOPWATCH,
    COMPASS,
    BATTERY,
    CONNECTION_STATUS,
    WEATHER,
    GPS_COORDINATES,
}

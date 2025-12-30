package com.augmentalis.magicui.ui.thememanager.io

import com.augmentalis.magicui.components.core.Theme
import kotlin.jvm.JvmInline

/**
 * ThemeIO - Theme Import/Export System
 *
 * Provides a unified interface for importing and exporting themes across multiple formats:
 * - W3C Design Tokens (standard JSON)
 * - MagicUI Extended JSON (with spatial/glass materials)
 * - QR Codes (compressed + base64)
 * - Deep Links (magicui://)
 * - Figma Tokens Studio format
 * - Platform-specific code (Kotlin, Swift, CSS)
 *
 * Architecture:
 * ```
 * ThemeSource → ThemeImporter → Theme
 * Theme → ThemeExporter → ExportFormat
 * ```
 *
 * @since 1.0.0
 * @author AVAMagic Team
 */

// ==================== Core Interfaces ====================

/**
 * Interface for importing themes from various sources
 */
interface ThemeImporter {
    /**
     * Import a theme from the given source
     *
     * @param source The source to import from
     * @return Result containing the imported Theme or an error
     */
    suspend fun import(source: ThemeSource): Result<Theme>

    /**
     * Check if this importer supports the given source
     *
     * @param source The source to check
     * @return true if this importer can handle the source
     */
    fun supports(source: ThemeSource): Boolean
}

/**
 * Interface for exporting themes to various formats
 */
interface ThemeExporter {
    /**
     * Export a theme to the given format
     *
     * @param theme The theme to export
     * @param format The target export format
     * @return Result containing the exported string or an error
     */
    suspend fun export(theme: Theme, format: ExportFormat): Result<String>

    /**
     * Check if this exporter supports the given format
     *
     * @param format The format to check
     * @return true if this exporter can handle the format
     */
    fun supports(format: ExportFormat): Boolean
}

// ==================== Theme Sources ====================

/**
 * Sealed class representing various theme import sources
 */
sealed class ThemeSource {
    /**
     * Raw JSON string (W3C or MagicUI format)
     *
     * @property json The JSON string
     * @property format Hint for the JSON format (auto-detected if null)
     */
    data class Json(
        val json: String,
        val format: JsonFormat? = null
    ) : ThemeSource()

    /**
     * QR Code data (compressed + base64 encoded)
     *
     * @property qrData The QR code data string
     */
    data class QRCode(val qrData: String) : ThemeSource()

    /**
     * Deep link URL (magicui://theme?data=...)
     *
     * @property url The deep link URL
     */
    data class DeepLink(val url: String) : ThemeSource()

    /**
     * File path to a theme file
     *
     * @property path The file path
     */
    data class File(val path: String) : ThemeSource()

    /**
     * Figma Tokens Studio format
     *
     * @property json The Figma tokens JSON
     */
    data class FigmaTokens(val json: String) : ThemeSource()

    /**
     * Remote URL to fetch theme from
     *
     * @property url The HTTP(S) URL
     */
    data class Remote(val url: String) : ThemeSource()

    /**
     * Raw theme object (pass-through)
     *
     * @property theme The theme object
     */
    data class Direct(val theme: Theme) : ThemeSource()
}

/**
 * JSON format types for theme import
 */
enum class JsonFormat {
    /** W3C Design Tokens Community Group specification */
    W3C,

    /** MagicUI extended format with spatial/glass materials */
    MAGICUI,

    /** Figma Tokens Studio format */
    FIGMA_TOKENS,

    /** Auto-detect format from JSON structure */
    AUTO
}

// ==================== Export Formats ====================

/**
 * Supported export formats for themes
 */
enum class ExportFormat {
    /** W3C Design Tokens standard JSON */
    W3C_JSON,

    /** MagicUI extended JSON with spatial/glass properties */
    MAGICUI_JSON,

    /** Compressed + base64 for QR codes */
    QR_CODE,

    /** Deep link URL format (magicui://theme?data=...) */
    DEEP_LINK,

    /** CSS custom properties (--color-primary, etc.) */
    CSS_VARIABLES,

    /** Kotlin data class source code */
    KOTLIN_CODE,

    /** Swift struct source code */
    SWIFT_CODE,

    /** Android XML resources */
    ANDROID_XML,

    /** iOS .xcassets color set */
    IOS_XCASSETS,

    /** Figma Tokens Studio format */
    FIGMA_TOKENS
}

// ==================== Theme Manager ====================

/**
 * Central theme import/export manager
 *
 * Delegates to appropriate importers/exporters based on source/format.
 */
class ThemeIOManager(
    private val importers: List<ThemeImporter> = defaultImporters(),
    private val exporters: List<ThemeExporter> = defaultExporters()
) {
    /**
     * Import a theme from any supported source
     *
     * @param source The source to import from
     * @return Result containing the imported Theme or an error
     */
    suspend fun import(source: ThemeSource): Result<Theme> {
        val importer = importers.firstOrNull { it.supports(source) }
            ?: return Result.failure(
                UnsupportedOperationException("No importer found for source: $source")
            )

        return importer.import(source)
    }

    /**
     * Export a theme to any supported format
     *
     * @param theme The theme to export
     * @param format The target export format
     * @return Result containing the exported string or an error
     */
    suspend fun export(theme: Theme, format: ExportFormat): Result<String> {
        val exporter = exporters.firstOrNull { it.supports(format) }
            ?: return Result.failure(
                UnsupportedOperationException("No exporter found for format: $format")
            )

        return exporter.export(theme, format)
    }

    /**
     * Check if a source format is supported for import
     *
     * @param source The source to check
     * @return true if import is supported
     */
    fun canImport(source: ThemeSource): Boolean =
        importers.any { it.supports(source) }

    /**
     * Check if an export format is supported
     *
     * @param format The format to check
     * @return true if export is supported
     */
    fun canExport(format: ExportFormat): Boolean =
        exporters.any { it.supports(format) }

    companion object {
        /**
         * Default importers for all supported formats
         */
        fun defaultImporters(): List<ThemeImporter> = listOf(
            // TODO: Implement these
            // JsonThemeImporter(),
            // QRCodeThemeImporter(),
            // DeepLinkThemeImporter(),
            // FigmaTokensImporter()
        )

        /**
         * Default exporters for all supported formats
         */
        fun defaultExporters(): List<ThemeExporter> = listOf(
            // TODO: Implement these
            // JsonThemeExporter(),
            // QRCodeThemeExporter(),
            // DeepLinkThemeExporter(),
            // CodeThemeExporter()
        )
    }
}

// ==================== Error Types ====================

/**
 * Base class for theme I/O errors
 */
sealed class ThemeIOError : Exception() {
    /**
     * Invalid theme data format
     */
    data class InvalidFormat(
        override val message: String,
        override val cause: Throwable? = null
    ) : ThemeIOError()

    /**
     * Missing required fields
     */
    data class MissingFields(
        val fields: List<String>,
        override val message: String = "Missing required fields: ${fields.joinToString()}"
    ) : ThemeIOError()

    /**
     * Invalid color value
     */
    data class InvalidColor(
        val value: String,
        override val message: String = "Invalid color value: $value"
    ) : ThemeIOError()

    /**
     * Unsupported theme version
     */
    data class UnsupportedVersion(
        val version: String,
        override val message: String = "Unsupported theme version: $version"
    ) : ThemeIOError()

    /**
     * Compression/decompression error
     */
    data class CompressionError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ThemeIOError()

    /**
     * Network error (for remote imports)
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : ThemeIOError()

    /**
     * File I/O error
     */
    data class FileError(
        val path: String,
        override val message: String,
        override val cause: Throwable? = null
    ) : ThemeIOError()
}

// ==================== Value Classes ====================

/**
 * Type-safe wrapper for theme JSON strings
 */
@JvmInline
value class ThemeJson(val value: String) {
    init {
        require(value.isNotBlank()) { "Theme JSON cannot be blank" }
    }
}

/**
 * Type-safe wrapper for QR code data
 */
@JvmInline
value class QRData(val value: String) {
    init {
        require(value.isNotBlank()) { "QR data cannot be blank" }
    }
}

/**
 * Type-safe wrapper for deep link URLs
 */
@JvmInline
value class DeepLinkUrl(val value: String) {
    init {
        require(value.startsWith("magicui://")) {
            "Deep link must start with magicui://"
        }
    }
}

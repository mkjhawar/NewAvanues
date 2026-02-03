package com.augmentalis.avaelements.core.translator

import com.augmentalis.avaelements.core.Component

/**
 * CodeTranslator
 *
 * Bidirectional translation between AvaUI/AvaCode and native platform code.
 *
 * Features:
 * - Import: Compose/XML/SwiftUI/React → AvaUI
 * - Export: AvaUI → Compose/SwiftUI/React/HTML
 * - Preserves styling and behavior
 * - Maintains component hierarchy
 */

/**
 * Base translator interface
 */
interface CodeTranslator {
    /**
     * Platform this translator supports
     */
    val platform: TranslationPlatform

    /**
     * Import native code to AvaUI components
     *
     * @param code Native code (Compose, XML, SwiftUI, React, etc.)
     * @param format Input format
     * @return List of AvaUI components
     */
    fun import(code: String, format: CodeFormat): TranslationResult<List<Component>>

    /**
     * Export AvaUI components to native code
     *
     * @param components AvaUI components
     * @param format Output format
     * @param options Export options
     * @return Native code string
     */
    fun export(
        components: List<Component>,
        format: CodeFormat,
        options: ExportOptions = ExportOptions()
    ): TranslationResult<String>

    /**
     * Validate if code can be translated
     *
     * @param code Native code
     * @param format Code format
     * @return Validation result with warnings
     */
    fun validate(code: String, format: CodeFormat): ValidationResult
}

/**
 * Translation platforms
 */
enum class TranslationPlatform {
    ANDROID,        // Jetpack Compose + XML
    IOS,            // SwiftUI + UIKit
    WEB,            // React + HTML
    DESKTOP         // Compose Desktop
}

/**
 * Code formats for import/export
 */
enum class CodeFormat {
    // Android
    JETPACK_COMPOSE,        // Kotlin Jetpack Compose
    ANDROID_XML,            // Android XML layouts

    // iOS
    SWIFTUI,                // SwiftUI code
    UIKIT_STORYBOARD,       // UIKit Storyboard XML
    UIKIT_CODE,             // UIKit programmatic

    // Web
    REACT_JSX,              // React JSX/TSX
    REACT_NATIVE,           // React Native
    HTML_CSS,               // HTML + CSS

    // Cross-platform
    FLUTTER_DART,           // Flutter Dart
    XAMARIN_XAML,           // Xamarin XAML

    // AvaUI
    MAGICUI_DSL,            // AvaUI Kotlin DSL
    MAGICUI_JSON,           // AvaUI JSON format
    MAGICUI_YAML            // AvaUI YAML format
}

/**
 * Export options
 */
data class ExportOptions(
    /**
     * Include comments in generated code
     */
    val includeComments: Boolean = true,

    /**
     * Formatting style
     */
    val formatting: FormattingStyle = FormattingStyle.PRETTY,

    /**
     * Include imports/dependencies
     */
    val includeImports: Boolean = true,

    /**
     * Generate preview/example code
     */
    val includePreview: Boolean = false,

    /**
     * Target platform version
     */
    val targetVersion: String? = null,

    /**
     * Use platform best practices
     */
    val useBestPractices: Boolean = true,

    /**
     * Preserve original component IDs
     */
    val preserveIds: Boolean = false
)

/**
 * Formatting styles
 */
enum class FormattingStyle {
    COMPACT,        // Minified, no spaces
    PRETTY,         // Human-readable with indentation
    DEFAULT         // Platform default style
}

/**
 * Translation result
 */
sealed class TranslationResult<out T> {
    data class Success<T>(
        val data: T,
        val warnings: List<TranslationWarning> = emptyList()
    ) : TranslationResult<T>()

    data class Failure(
        val errors: List<TranslationError>
    ) : TranslationResult<Nothing>()

    data class Partial<T>(
        val data: T,
        val errors: List<TranslationError>,
        val warnings: List<TranslationWarning>
    ) : TranslationResult<T>()
}

/**
 * Translation warning
 */
data class TranslationWarning(
    val message: String,
    val component: String? = null,
    val line: Int? = null,
    val suggestion: String? = null
)

/**
 * Translation error
 */
data class TranslationError(
    val message: String,
    val component: String? = null,
    val line: Int? = null,
    val fatal: Boolean = false
)

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<TranslationError> = emptyList(),
    val warnings: List<TranslationWarning> = emptyList(),
    val supportedPercentage: Int = 100  // % of features supported
)

/**
 * Translation capabilities
 */
data class TranslationCapabilities(
    val platform: TranslationPlatform,
    val importFormats: List<CodeFormat>,
    val exportFormats: List<CodeFormat>,
    val supportedComponents: Set<String>,
    val limitations: List<String> = emptyList()
)

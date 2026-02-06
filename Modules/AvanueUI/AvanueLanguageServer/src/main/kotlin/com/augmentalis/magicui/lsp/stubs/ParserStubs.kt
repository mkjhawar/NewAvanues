package com.augmentalis.avaui.lsp.stubs

/**
 * Temporary stub implementations until modules are added to build
 * TODO: Remove when actual modules are available
 */

data class ValidationError(val line: Int, val message: String)
data class ValidationWarning(val line: Int, val message: String)
data class ValidationResult(val errors: List<ValidationError>, val warnings: List<ValidationWarning>)

class VosParser {
    fun validate(content: String): ValidationResult {
        return ValidationResult(emptyList(), emptyList())
    }

    fun parseComponent(content: String): Result<ComponentNode> {
        return Result.failure(NotImplementedError("Parser not available"))
    }
}

class JsonDSLParser {
    fun validate(content: String): ValidationResult {
        return ValidationResult(emptyList(), emptyList())
    }

    fun parseComponent(content: String): Result<ComponentNode> {
        return Result.failure(NotImplementedError("Parser not available"))
    }
}

class CompactSyntaxParser {
    fun validate(content: String): ValidationResult {
        return ValidationResult(emptyList(), emptyList())
    }

    fun parseComponent(content: String): Result<ComponentNode> {
        return Result.failure(NotImplementedError("Parser not available"))
    }
}

data class ComponentNode(
    val id: String,
    val type: ComponentType,
    val properties: Map<String, Any> = emptyMap(),
    val children: List<ComponentNode> = emptyList(),
    val eventHandlers: Map<String, String> = emptyMap()
)

data class ScreenNode(
    val name: String,
    val root: ComponentNode
)

enum class ComponentType {
    BUTTON, CARD, CHECKBOX, CHIP, DIVIDER, IMAGE, LIST_ITEM, TEXT, TEXT_FIELD,
    COLOR_PICKER, ICON_PICKER,
    ICON, LABEL, CONTAINER, ROW, COLUMN, SPACER,
    SWITCH, SLIDER, PROGRESS_BAR, SPINNER, ALERT, DIALOG, TOAST, TOOLTIP,
    RADIO, DROPDOWN, DATE_PICKER, TIME_PICKER, SEARCH_BAR, RATING, BADGE,
    FILE_UPLOAD, APP_BAR, BOTTOM_NAV, DRAWER, PAGINATION, TABS, BREADCRUMB, ACCORDION,
    STACK, GRID, SCROLL_VIEW,
    CUSTOM
}

enum class ExportFormat {
    DSL, YAML, JSON, CSS, ANDROID_XML
}

class ThemeCompiler {
    // Stub implementation
}

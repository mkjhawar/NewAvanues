package com.augmentalis.avaui.lsp

/**
 * Detects AvanueUI DSL file type from URI and content.
 * Supports both legacy .magic.* and new .avanueui.* file extensions.
 */
object FileTypeDetector {

    fun detect(uri: String, content: String): FileType {
        return when {
            // New AvanueUI extensions
            uri.endsWith(".avanueui.json") || uri.endsWith(".magic.json") || uri.endsWith(".json") -> FileType.JSON
            uri.endsWith(".avanueui.yaml") || uri.endsWith(".magic.yaml") || uri.endsWith(".yaml") || uri.endsWith(".yml") -> FileType.YAML
            uri.endsWith(".avanueui") || uri.endsWith(".magicui") || uri.endsWith(".ucd") -> FileType.COMPACT
            // Content-based fallback
            content.trim().startsWith("{") -> FileType.JSON
            content.trim().startsWith("Ava") -> FileType.COMPACT
            else -> FileType.YAML
        }
    }
}

enum class FileType {
    JSON, YAML, COMPACT, UNKNOWN
}

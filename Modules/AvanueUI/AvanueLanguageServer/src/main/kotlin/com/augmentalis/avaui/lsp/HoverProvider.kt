package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*

/**
 * Provides hover documentation for AvanueUI DSL elements.
 * Single responsibility: generate hover information for components, properties, and AVIDs.
 */
class HoverProvider {

    fun getHoverInfo(content: String, position: Position): Hover {
        val lines = content.lines()
        if (position.line >= lines.size) return createEmptyHover()

        val currentLine = lines[position.line]
        val ctx = analyzeContext(currentLine, position.character)

        val documentation = when {
            ctx.componentName != null -> ComponentDocs.getHoverDocumentation(ctx.componentName)
            ctx.propertyName != null -> getPropertyHoverDoc(ctx.propertyName, ctx.propertyValue)
            ctx.avid != null -> getAvidHoverDoc(ctx.avid)
            else -> null
        }

        return if (documentation != null) {
            Hover(MarkupContent().apply { kind = "markdown"; value = documentation })
        } else {
            createEmptyHover()
        }
    }

    private fun analyzeContext(line: String, character: Int): HoverContextInfo {
        val beforeCursor = line.substring(0, minOf(character, line.length))
        val afterCursor = line.substring(minOf(character, line.length))
        val wordBefore = beforeCursor.split(Regex("[\\s:,]")).lastOrNull() ?: ""
        val wordAfter = afterCursor.split(Regex("[\\s:,]")).firstOrNull() ?: ""
        val word = wordBefore + wordAfter

        if (word.isNotEmpty() && word[0].isUpperCase()) {
            return HoverContextInfo(componentName = word)
        }
        if (line.contains(":")) {
            val parts = line.split(":")
            val propName = parts[0].trim()
            val propValue = if (parts.size > 1) parts[1].trim() else null
            return HoverContextInfo(propertyName = propName, propertyValue = propValue)
        }
        if (word.startsWith("AVID-") || word.contains("avid")) {
            return HoverContextInfo(avid = word)
        }
        return HoverContextInfo()
    }

    private fun getPropertyHoverDoc(propertyName: String, propertyValue: String?): String {
        return when (propertyName) {
            "avid" -> """
                ### AVID (AvanueUI Voice IDentifier)
                Unique identifier for voice navigation and component access

                **Format:** `component-type-descriptor`
                **Example:** `login-submit-button`

                Used by VoiceOS to navigate and interact with UI elements.
            """.trimIndent()
            "onClick", "onChange", "onSubmit" -> """
                ### Event Handler: $propertyName
                Callback function triggered on ${propertyName.substring(2)} event

                **Expected Format:** Function reference or inline handler
                **Example:** `$propertyName: handleUserAction`
            """.trimIndent()
            "color", "backgroundColor", "borderColor" -> """
                ### Color Property: $propertyName
                ${if (propertyValue != null) "**Current:** `$propertyValue`\n\n" else ""}
                **Accepted Formats:**
                - Hex: `#RGB`, `#RRGGBB`, `#AARRGGBB`
                - Named: red, blue, green, black, white, gray, yellow, orange, purple

                **Examples:** `#FF0000`, `red`, `#80FF0000`
            """.trimIndent()
            "width", "height", "padding", "margin" -> """
                ### Size Property: $propertyName
                ${if (propertyValue != null) "**Current:** `$propertyValue`\n\n" else ""}
                **Accepted Units:**
                - `dp` - Density-independent pixels (recommended)
                - `sp` - Scale-independent pixels (for text)
                - `px` - Physical pixels
                - `%` - Percentage of parent

                **Examples:** `16dp`, `14sp`, `100px`, `50%`
            """.trimIndent()
            else -> """
                ### Property: $propertyName
                ${if (propertyValue != null) "**Value:** `$propertyValue`" else "Component property"}
            """.trimIndent()
        }
    }

    private fun getAvidHoverDoc(avid: String): String {
        return """
            ### AVID: `$avid`
            AvanueUI Voice IDentifier for component navigation

            **Usage:**
            - Voice commands: "Click $avid", "Focus $avid"
            - Programmatic access: `findByAvid("$avid")`
            - Analytics tracking

            **Navigation:** Use Go-to-Definition (F12) to navigate to component
        """.trimIndent()
    }

    private fun createEmptyHover(): Hover {
        return Hover(MarkupContent().apply { kind = "markdown"; value = "" })
    }

    private data class HoverContextInfo(
        val componentName: String? = null,
        val propertyName: String? = null,
        val propertyValue: String? = null,
        val avid: String? = null
    )
}

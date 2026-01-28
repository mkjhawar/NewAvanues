package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Quantized Element - Compact representation of a UI element for AVU format.
 *
 * Stores essential element properties optimized for LLM consumption
 * and voice command generation.
 *
 * @property avid Augmentalis Voice Identifier (compact 16-char format)
 * @property type Classified element type
 * @property label Primary label for voice reference
 * @property aliases Alternative names/labels for this element
 * @property bounds Bounds string in format "left,top,right,bottom"
 * @property actions Available actions (click, scroll, etc.)
 * @property category Element category (action, input, display, etc.)
 */
data class QuantizedElement(
    val avid: String,
    val type: ElementType,
    val label: String,
    val aliases: List<String> = emptyList(),
    val bounds: String = "",
    val actions: String = "",
    val category: String = ""
) {
    /**
     * Legacy alias for avid (deprecated, use avid directly).
     */
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val vuid: String get() = avid

    companion object {
        /**
         * Create QuantizedElement from ElementInfo.
         *
         * @param elementInfo Source element info
         * @param avid Generated AVID for this element
         * @return QuantizedElement
         */
        fun fromElementInfo(elementInfo: ElementInfo, avid: String): QuantizedElement {
            val type = ElementType.fromClassName(elementInfo.className)
            val label = elementInfo.text.ifBlank {
                elementInfo.contentDescription.ifBlank {
                    elementInfo.resourceId.substringAfterLast("/").ifBlank { "unlabeled" }
                }
            }

            val aliases = listOfNotNull(
                elementInfo.text.takeIf { it.isNotBlank() },
                elementInfo.contentDescription.takeIf { it.isNotBlank() },
                elementInfo.resourceId.substringAfterLast("/").takeIf { it.isNotBlank() }
            ).distinct().filter { it != label }

            val actions = buildList {
                if (elementInfo.isClickable) add("click")
                if (elementInfo.isScrollable) add("scroll")
            }.joinToString(",")

            val category = when {
                elementInfo.isClickable -> "action"
                type == ElementType.TEXT_FIELD -> "input"
                else -> "display"
            }

            return QuantizedElement(
                avid = avid,
                type = type,
                label = label,
                aliases = aliases,
                bounds = "${elementInfo.bounds.left},${elementInfo.bounds.top},${elementInfo.bounds.right},${elementInfo.bounds.bottom}",
                actions = actions,
                category = category
            )
        }
    }

    /**
     * Generate AVU ELM line format.
     *
     * Format: ELM:avid:label:type:actions:bounds:category
     */
    fun toElmLine(): String {
        return "ELM:$avid:$label:${type.name}:$actions:$bounds:$category"
    }
}

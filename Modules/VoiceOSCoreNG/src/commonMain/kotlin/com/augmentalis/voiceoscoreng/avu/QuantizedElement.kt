package com.augmentalis.voiceoscoreng.avu

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Quantized Element - Compact representation of a UI element for AVU format.
 *
 * Stores essential element properties optimized for LLM consumption
 * and voice command generation.
 *
 * @property vuid Voice Unique Identifier (compact 16-char format)
 * @property type Classified element type
 * @property label Primary label for voice reference
 * @property aliases Alternative names/labels for this element
 * @property bounds Bounds string in format "left,top,right,bottom"
 * @property actions Available actions (click, scroll, etc.)
 * @property category Element category (action, input, display, etc.)
 */
data class QuantizedElement(
    val vuid: String,
    val type: ElementType,
    val label: String,
    val aliases: List<String> = emptyList(),
    val bounds: String = "",
    val actions: String = "",
    val category: String = ""
) {
    companion object {
        /**
         * Create QuantizedElement from ElementInfo.
         *
         * @param elementInfo Source element info
         * @param vuid Generated VUID for this element
         * @return QuantizedElement
         */
        fun fromElementInfo(elementInfo: ElementInfo, vuid: String): QuantizedElement {
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
                vuid = vuid,
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
     * Format: ELM:uuid:label:type:actions:bounds:category
     */
    fun toElmLine(): String {
        return "ELM:$vuid:$label:${type.name}:$actions:$bounds:$category"
    }
}

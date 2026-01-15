package com.augmentalis.voiceoscoreng.service

import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Generates AVU (Avanues Universal) format output.
 *
 * Extracted from VoiceOSAccessibilityService.kt for SOLID compliance.
 * Single Responsibility: Format exploration results as AVU text.
 *
 * AVU is a human-readable and machine-parseable format for representing
 * UI element structures and voice commands.
 */
object AVUFormatter {

    /**
     * Generate AVU (Avanues Universal) format output with proper command names.
     *
     * @param packageName Package name of the explored app
     * @param elements List of all extracted elements
     * @param elementLabels Map of element index to derived labels
     * @param commands List of generated voice commands
     * @return AVU formatted string
     */
    fun generateAVU(
        packageName: String,
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        commands: List<GeneratedCommand>
    ): String {
        return buildString {
            appendLine("# Avanues Universal Format v2.0")
            appendLine("# Package: $packageName")
            appendLine("# Elements: ${elements.size}")
            appendLine("# Commands: ${commands.size}")
            appendLine()
            appendLine("schema: avu-2.0")
            appendLine("version: 2.0.0")
            appendLine("package: $packageName")
            appendLine()

            // Elements section with derived labels
            appendLine("@elements:")
            elements.forEachIndexed { index, element ->
                val typeCode = ElementFingerprint.getTypeCode(element.className)
                val label = elementLabels[index] ?: element.className.substringAfterLast(".")
                val clickable = if (element.isClickable) "T" else "F"
                val scrollable = if (element.isScrollable) "T" else "F"
                appendLine("  - idx:$index type:$typeCode label:\"$label\" click:$clickable scroll:$scrollable")
            }
            appendLine()

            // Commands section with voice phrases
            appendLine("@commands:")
            if (commands.isEmpty()) {
                appendLine("  # No actionable elements found")
            } else {
                commands.forEach { cmd ->
                    // The voice command is just the label (e.g., "Accessibility", "Reset")
                    // The action (tap/scroll/toggle) is metadata
                    appendLine("  - voice:\"${cmd.derivedLabel}\" action:${cmd.action} vuid:${cmd.targetVuid}")
                    // Also include alternate phrases
                    appendLine("    alternates: [\"${cmd.phrase}\", \"press ${cmd.derivedLabel}\", \"select ${cmd.derivedLabel}\"]")
                }
            }
            appendLine()

            // Actionable elements summary
            appendLine("@actionable:")
            val actionableElements = elements.mapIndexedNotNull { index, element ->
                if (element.isClickable || element.isScrollable) {
                    val label = elementLabels[index] ?: return@mapIndexedNotNull null
                    val action = when {
                        element.isClickable -> "tap"
                        element.isScrollable -> "scroll"
                        else -> "interact"
                    }
                    "  - \"$label\" -> $action"
                } else null
            }
            actionableElements.forEach { appendLine(it) }
        }
    }

    /**
     * Generate a compact AVU summary for logging.
     *
     * @param packageName Package name
     * @param elementCount Total element count
     * @param commandCount Total command count
     * @return Compact summary string
     */
    fun generateSummary(
        packageName: String,
        elementCount: Int,
        commandCount: Int
    ): String {
        return "AVU: $packageName | $elementCount elements | $commandCount commands"
    }
}

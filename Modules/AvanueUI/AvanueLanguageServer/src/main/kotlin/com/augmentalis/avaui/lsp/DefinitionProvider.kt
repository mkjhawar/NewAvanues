package com.augmentalis.avaui.lsp

import org.eclipse.lsp4j.*

/**
 * Provides go-to-definition support for AVID references in AvanueUI DSL documents.
 * Single responsibility: locate AVID declarations and navigate to them.
 */
class DefinitionProvider {

    fun getDefinitionLocations(content: String, position: Position, uri: String): List<Location> {
        val lines = content.lines()
        if (position.line >= lines.size) return emptyList()

        val currentLine = lines[position.line]
        val avid = extractAvidAtPosition(currentLine, position.character)

        if (avid != null && isValidAvidFormat(avid)) {
            val location = findAvidDefinition(content, avid, uri)
            if (location != null) return listOf(location)
        }
        return emptyList()
    }

    private fun extractAvidAtPosition(line: String, character: Int): String? {
        val beforeCursor = line.substring(0, minOf(character, line.length))
        val afterCursor = line.substring(minOf(character, line.length))
        val wordBefore = beforeCursor.split(Regex("[\\s:,\"']")).lastOrNull() ?: ""
        val wordAfter = afterCursor.split(Regex("[\\s:,\"']")).firstOrNull() ?: ""
        val word = wordBefore + wordAfter

        return if (word.matches(Regex("^[a-z0-9-]+$")) && word.contains("-")) word else null
    }

    private fun isValidAvidFormat(avid: String): Boolean {
        return avid.matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$")) &&
               avid.length in 3..64
    }

    private fun findAvidDefinition(content: String, avid: String, uri: String): Location? {
        content.lines().forEachIndexed { index, line ->
            if (line.contains("avid:") && line.contains(avid)) {
                return Location(uri, Range(Position(index, 0), Position(index, line.length)))
            }
        }
        return null
    }
}

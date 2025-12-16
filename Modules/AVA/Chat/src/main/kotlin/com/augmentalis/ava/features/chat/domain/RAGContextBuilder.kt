// filename: Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/domain/RAGContextBuilder.kt
// created: 2025-11-22
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.chat.domain

import com.augmentalis.ava.features.rag.domain.SearchResult

/**
 * Builds context for LLM prompts from RAG search results.
 *
 * Phase 2, Task 2: RAG Integration with Chat UI
 *
 * Responsibilities:
 * - Assemble retrieved document chunks into coherent context
 * - Format context for injection into LLM prompts
 * - Handle source attribution and citations
 * - Optimize context length for model context windows
 *
 * Usage:
 * ```kotlin
 * val builder = RAGContextBuilder()
 * val context = builder.assembleContext(searchResults)
 * val promptWithContext = builder.buildPromptWithContext(userQuery, context)
 * ```
 */
class RAGContextBuilder {

    companion object {
        /**
         * Maximum context length (in characters) to include in prompts.
         * This prevents exceeding LLM context windows.
         *
         * Current: 4000 chars (~1000 tokens) for Gemma 2B
         * Adjust based on model: Gemma 4B = 8000 chars, GPT-4 = 24000 chars
         */
        private const val MAX_CONTEXT_LENGTH = 4000

        /**
         * Separator between different source chunks in context
         */
        private const val CHUNK_SEPARATOR = "\n\n---\n\n"

        /**
         * Header template for context section
         */
        private const val CONTEXT_HEADER = "# Relevant Context from Documents\n\n"

        /**
         * Footer to signal end of context
         */
        private const val CONTEXT_FOOTER = "\n\n# End of Context\n\n"
    }

    /**
     * Assembles retrieved chunks into a formatted context string.
     *
     * Flow:
     * 1. Sort results by similarity (highest first)
     * 2. Build context by concatenating chunk content
     * 3. Include source attribution for each chunk
     * 4. Truncate if exceeds MAX_CONTEXT_LENGTH
     * 5. Return formatted context with headers
     *
     * Format:
     * ```
     * # Relevant Context from Documents
     *
     * [Source: {document_title}, Page {page}, Similarity: {score}%]
     * {chunk_content}
     *
     * ---
     *
     * [Source: {document_title}, Page {page}, Similarity: {score}%]
     * {chunk_content}
     *
     * # End of Context
     * ```
     *
     * @param results Search results from RAG retrieval (already sorted by similarity)
     * @return Formatted context string with source attributions
     */
    fun assembleContext(results: List<SearchResult>): String {
        if (results.isEmpty()) {
            return ""
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append(CONTEXT_HEADER)

        var currentLength = CONTEXT_HEADER.length + CONTEXT_FOOTER.length

        // Iterate through results (already sorted by similarity from RAG search)
        for (result in results) {
            val chunk = result.chunk
            val document = result.document
            val similarity = (result.similarity * 100).toInt()

            // Build source attribution
            val sourceAttribution = buildSourceAttribution(
                documentTitle = document?.title ?: "Unknown Document",
                pageNumber = chunk.metadata.pageNumber,
                similarity = similarity
            )

            val chunkText = chunk.content
            val chunkEntry = "$sourceAttribution\n$chunkText$CHUNK_SEPARATOR"

            // Check if adding this chunk would exceed max length
            if (currentLength + chunkEntry.length > MAX_CONTEXT_LENGTH) {
                // Stop adding chunks if we've hit the limit
                break
            }

            contextBuilder.append(chunkEntry)
            currentLength += chunkEntry.length
        }

        // Remove trailing separator if present
        if (contextBuilder.endsWith(CHUNK_SEPARATOR)) {
            contextBuilder.setLength(contextBuilder.length - CHUNK_SEPARATOR.length)
        }

        contextBuilder.append(CONTEXT_FOOTER)

        return contextBuilder.toString()
    }

    /**
     * Builds a prompt with retrieved context injected.
     *
     * Template structure:
     * ```
     * You are AVA, an AI assistant. Use the following context to answer the user's question.
     * If the context doesn't contain relevant information, say so and use your general knowledge.
     *
     * {assembled_context}
     *
     * User Question: {user_query}
     *
     * Answer:
     * ```
     *
     * @param userQuery Original user question
     * @param assembledContext Formatted context from assembleContext()
     * @return Complete prompt ready for LLM inference
     */
    fun buildPromptWithContext(userQuery: String, assembledContext: String): String {
        return if (assembledContext.isBlank()) {
            // No context available, use standard prompt
            buildStandardPrompt(userQuery)
        } else {
            // RAG-enhanced prompt with context
            buildRAGPrompt(userQuery, assembledContext)
        }
    }

    /**
     * Builds standard prompt without RAG context.
     * Used when no relevant documents are found.
     */
    private fun buildStandardPrompt(userQuery: String): String {
        return """
            You are AVA, an AI assistant.

            User Question: $userQuery

            Answer:
        """.trimIndent()
    }

    /**
     * Builds RAG-enhanced prompt with context injection.
     * Instructs LLM to prioritize context over general knowledge.
     */
    private fun buildRAGPrompt(userQuery: String, context: String): String {
        return """
            You are AVA, an AI assistant. Use the following context from the user's documents to answer their question.

            IMPORTANT INSTRUCTIONS:
            1. Prioritize information from the provided context
            2. If the context contains the answer, cite the source (e.g., "According to {Document Title}...")
            3. If the context doesn't have enough information, say so and use your general knowledge
            4. Be concise and accurate

            $context

            User Question: $userQuery

            Answer:
        """.trimIndent()
    }

    /**
     * Builds source attribution string for a chunk.
     *
     * Format: [Source: {title}, Page {page}, Similarity: {score}%]
     *
     * @param documentTitle Title of source document
     * @param pageNumber Page number within document (null if not available)
     * @param similarity Similarity score (0-100)
     * @return Formatted attribution string
     */
    private fun buildSourceAttribution(
        documentTitle: String,
        pageNumber: Int?,
        similarity: Int
    ): String {
        val pageInfo = if (pageNumber != null) ", Page $pageNumber" else ""
        return "[Source: $documentTitle$pageInfo, Similarity: $similarity%]"
    }

    /**
     * Extracts source citations directly from search results for display in UI.
     *
     * This is the preferred method when you have direct access to SearchResult objects,
     * as it avoids string parsing and preserves full metadata.
     *
     * @param results Search results from RAG repository
     * @return List of SourceCitation objects for UI display
     */
    fun extractSourceCitations(results: List<SearchResult>): List<SourceCitation> {
        return results.map { result ->
            val similarity = (result.similarity * 100).toInt()
            SourceCitation(
                documentTitle = result.document?.title ?: "Unknown Document",
                pageNumber = result.chunk.metadata.pageNumber,
                similarityPercent = similarity
            )
        }.distinctBy { it.documentTitle } // Remove duplicate document references
    }

    /**
     * Extracts source citations from assembled context for display in UI.
     *
     * This method is used when you only have the assembled context string
     * and need to parse out the citations. Prefer extractSourceCitations(List<SearchResult>)
     * when possible for better accuracy.
     *
     * Parses attribution strings to extract:
     * - Document title
     * - Page number
     * - Similarity score
     *
     * @param assembledContext Context string from assembleContext()
     * @return List of SourceCitation objects for UI display
     */
    fun extractSourceCitations(assembledContext: String): List<SourceCitation> {
        if (assembledContext.isBlank()) return emptyList()

        val citations = mutableListOf<SourceCitation>()
        val attributionRegex = """\[Source: (.+?)(, Page (\d+))?, Similarity: (\d+)%\]""".toRegex()

        attributionRegex.findAll(assembledContext).forEach { match ->
            val title = match.groupValues[1]
            val page = match.groupValues[3].toIntOrNull()
            val similarity = match.groupValues[4].toIntOrNull() ?: 0

            citations.add(SourceCitation(
                documentTitle = title,
                pageNumber = page,
                similarityPercent = similarity
            ))
        }

        return citations.distinctBy { it.documentTitle } // Remove duplicates
    }
}

/**
 * Represents a source citation for UI display.
 *
 * Used in MessageBubble to show which documents were used to generate response.
 *
 * @param documentTitle Title of the source document
 * @param pageNumber Page number within document (null if not available)
 * @param similarityPercent Similarity score as percentage (0-100)
 */
data class SourceCitation(
    val documentTitle: String,
    val pageNumber: Int?,
    val similarityPercent: Int
) {
    /**
     * Formats citation for display in UI.
     *
     * Examples:
     * - "User Manual (Page 5) - 87%"
     * - "FAQ Document - 92%"
     */
    fun format(): String {
        val pageInfo = if (pageNumber != null) " (Page $pageNumber)" else ""
        return "$documentTitle$pageInfo - $similarityPercent%"
    }
}

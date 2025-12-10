/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler
import com.augmentalis.ava.features.actions.web.DuckDuckGoSearchService
import com.augmentalis.ava.features.actions.web.SearchResult
import timber.log.Timber

/**
 * Action handler for web searches.
 *
 * Uses DuckDuckGo API to fetch search results, then optionally
 * summarizes them using the LLM for a conversational response.
 *
 * Flow:
 * 1. Extract search query from utterance
 * 2. Call DuckDuckGo Instant Answer API
 * 3. Format results for display or LLM summarization
 *
 * Intent examples:
 * - "Search for best restaurants nearby"
 * - "Look up quantum computing"
 * - "What is the capital of France"
 * - "Google how to tie a tie"
 * - "Find information about electric cars"
 *
 * Created: 2025-12-01
 */
class WebSearchActionHandler(
    private val searchService: DuckDuckGoSearchService = DuckDuckGoSearchService(),
    private val llmSummarizer: LLMSummarizer? = null
) : IntentActionHandler {

    companion object {
        private const val TAG = "WebSearchHandler"

        // Patterns to extract search query from utterance
        private val QUERY_PATTERNS = listOf(
            Regex("""(?:search|look up|google|find|lookup)\s+(?:for\s+)?(.+)""", RegexOption.IGNORE_CASE),
            Regex("""what\s+(?:is|are)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""who\s+(?:is|are|was|were)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""where\s+(?:is|are)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""when\s+(?:is|was|did)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""how\s+(?:do|does|to|can|many|much)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""tell\s+me\s+about\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""information\s+(?:about|on)\s+(.+)""", RegexOption.IGNORE_CASE)
        )
    }

    override val intent = "web.search"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Timber.d("$TAG: Processing search request: '$utterance'")

            // 1. Extract search query
            val query = extractQuery(utterance)
            if (query.isBlank()) {
                Timber.w("$TAG: Could not extract query from utterance")
                return ActionResult.Failure(
                    message = "I couldn't understand what you want to search for. Try saying 'Search for [topic]'."
                )
            }

            Timber.i("$TAG: Extracted query: '$query'")

            // 2. Perform search
            val searchResult = searchService.search(query)

            // 3. Process result
            when (searchResult) {
                is SearchResult.Success -> {
                    val response = if (llmSummarizer != null) {
                        // Use LLM to generate natural response
                        val searchContext = searchService.formatForLLM(searchResult)
                        llmSummarizer.summarize(utterance, searchContext)
                    } else {
                        // Return formatted results directly
                        formatResultsForDisplay(searchResult)
                    }

                    Timber.i("$TAG: Search successful for '$query'")
                    ActionResult.Success(
                        message = response,
                        data = mapOf(
                            "query" to query,
                            "resultCount" to searchResult.snippets.size,
                            "hasInstantAnswer" to (searchResult.instantAnswer != null)
                        )
                    )
                }

                is SearchResult.NoResults -> {
                    Timber.w("$TAG: No results for '$query'")
                    ActionResult.Success(
                        message = "I couldn't find any information about \"$query\". Try rephrasing your search.",
                        data = mapOf("query" to query, "resultCount" to 0)
                    )
                }

                is SearchResult.Error -> {
                    Timber.e("$TAG: Search error: ${searchResult.message}")
                    ActionResult.Failure(
                        message = "I had trouble searching the web. Please check your internet connection and try again."
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "$TAG: Search failed")
            ActionResult.Failure(
                message = "Search failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Extract search query from user utterance.
     */
    private fun extractQuery(utterance: String): String {
        // Try each pattern
        for (pattern in QUERY_PATTERNS) {
            val match = pattern.find(utterance)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        // Fallback: Remove common prefixes and use the rest
        val cleaned = utterance
            .replace(Regex("""^(please|can you|could you|hey ava|ava)\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^(search|look up|google|find)\s*(for)?\s*""", RegexOption.IGNORE_CASE), "")
            .trim()

        return cleaned.ifBlank { utterance }
    }

    /**
     * Format search results for display without LLM.
     */
    private fun formatResultsForDisplay(result: SearchResult.Success): String {
        return buildString {
            if (result.instantAnswer != null) {
                append(result.instantAnswer)
            } else if (result.snippets.isNotEmpty()) {
                append("Here's what I found about \"${result.query}\":\n\n")
                result.snippets.take(3).forEach { snippet ->
                    append("• ${snippet.content.take(150)}")
                    if (snippet.content.length > 150) append("...")
                    append("\n\n")
                }
            }
        }.trim()
    }
}

/**
 * Interface for LLM summarization of search results.
 *
 * Implement this to connect the web search to your LLM provider.
 */
interface LLMSummarizer {
    /**
     * Generate a natural language summary of search results.
     *
     * @param userQuery Original user question
     * @param searchContext Formatted search results
     * @return Natural language response
     */
    suspend fun summarize(userQuery: String, searchContext: String): String
}

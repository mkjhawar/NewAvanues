/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.actions.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * DuckDuckGo Instant Answer API Service
 *
 * Provides web search functionality using DuckDuckGo's free API.
 * No API key required. Privacy-focused (no tracking).
 *
 * API Documentation: https://api.duckduckgo.com/api
 *
 * Features:
 * - Instant answers for factual queries
 * - Related topics for broader searches
 * - No rate limiting for reasonable use
 *
 * Created: 2025-12-01
 */
class DuckDuckGoSearchService {

    companion object {
        private const val TAG = "DuckDuckGoSearch"
        private const val BASE_URL = "https://api.duckduckgo.com/"
        private const val TIMEOUT_MS = 10000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Search DuckDuckGo for instant answers and related topics.
     *
     * @param query Search query
     * @return SearchResult with answer and related topics
     */
    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        try {
            Timber.d("$TAG: Searching for '$query'")

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("${BASE_URL}?q=$encodedQuery&format=json&no_html=1&skip_disambig=1")

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "AVA-AI-Assistant/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Timber.e("$TAG: HTTP error $responseCode")
                return@withContext SearchResult.Error("Search failed: HTTP $responseCode")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val ddgResponse = json.decodeFromString<DuckDuckGoResponse>(responseBody)

            Timber.d("$TAG: Got response - Abstract: ${ddgResponse.abstract.take(100)}...")

            // Build search result
            val snippets = mutableListOf<SearchSnippet>()

            // Add abstract (main answer) if available
            if (ddgResponse.abstract.isNotBlank()) {
                snippets.add(
                    SearchSnippet(
                        title = ddgResponse.heading.ifBlank { "Answer" },
                        content = ddgResponse.abstract,
                        url = ddgResponse.abstractURL,
                        source = ddgResponse.abstractSource
                    )
                )
            }

            // Add related topics
            ddgResponse.relatedTopics.take(5).forEach { topic ->
                if (topic.text.isNotBlank()) {
                    snippets.add(
                        SearchSnippet(
                            title = topic.text.take(50),
                            content = topic.text,
                            url = topic.firstURL,
                            source = "DuckDuckGo"
                        )
                    )
                }
            }

            // If no instant answer, try infobox
            if (snippets.isEmpty() && ddgResponse.infobox != null) {
                val infoContent = ddgResponse.infobox.content
                    .take(5)
                    .joinToString("\n") { "${it.label}: ${it.value}" }

                if (infoContent.isNotBlank()) {
                    snippets.add(
                        SearchSnippet(
                            title = ddgResponse.heading,
                            content = infoContent,
                            url = ddgResponse.abstractURL,
                            source = "DuckDuckGo Infobox"
                        )
                    )
                }
            }

            if (snippets.isEmpty()) {
                Timber.w("$TAG: No results for '$query'")
                SearchResult.NoResults(query)
            } else {
                Timber.i("$TAG: Found ${snippets.size} results for '$query'")
                SearchResult.Success(
                    query = query,
                    snippets = snippets,
                    instantAnswer = ddgResponse.abstract.ifBlank { null }
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "$TAG: Search failed for '$query'")
            SearchResult.Error("Search failed: ${e.message}")
        }
    }

    /**
     * Format search results as context for LLM.
     *
     * @param result Search result to format
     * @return Formatted string for LLM context
     */
    fun formatForLLM(result: SearchResult): String {
        return when (result) {
            is SearchResult.Success -> {
                buildString {
                    appendLine("Web search results for: \"${result.query}\"")
                    appendLine()

                    if (result.instantAnswer != null) {
                        appendLine("Direct Answer: ${result.instantAnswer}")
                        appendLine()
                    }

                    result.snippets.forEachIndexed { index, snippet ->
                        appendLine("${index + 1}. ${snippet.title}")
                        appendLine("   ${snippet.content.take(200)}")
                        if (snippet.url.isNotBlank()) {
                            appendLine("   Source: ${snippet.source} - ${snippet.url}")
                        }
                        appendLine()
                    }
                }
            }
            is SearchResult.NoResults -> "No web results found for: \"${result.query}\""
            is SearchResult.Error -> "Web search error: ${result.message}"
        }
    }
}

/**
 * Search result sealed class
 */
sealed class SearchResult {
    data class Success(
        val query: String,
        val snippets: List<SearchSnippet>,
        val instantAnswer: String?
    ) : SearchResult()

    data class NoResults(val query: String) : SearchResult()
    data class Error(val message: String) : SearchResult()
}

/**
 * Individual search snippet
 */
data class SearchSnippet(
    val title: String,
    val content: String,
    val url: String,
    val source: String
)

// ==================== DuckDuckGo API Response Models ====================

@Serializable
private data class DuckDuckGoResponse(
    @SerialName("Abstract") val abstract: String = "",
    @SerialName("AbstractText") val abstractText: String = "",
    @SerialName("AbstractSource") val abstractSource: String = "",
    @SerialName("AbstractURL") val abstractURL: String = "",
    @SerialName("Heading") val heading: String = "",
    @SerialName("Answer") val answer: String = "",
    @SerialName("AnswerType") val answerType: String = "",
    @SerialName("RelatedTopics") val relatedTopics: List<RelatedTopic> = emptyList(),
    @SerialName("Infobox") val infobox: Infobox? = null
)

@Serializable
private data class RelatedTopic(
    @SerialName("Text") val text: String = "",
    @SerialName("FirstURL") val firstURL: String = "",
    @SerialName("Icon") val icon: Icon? = null
)

@Serializable
private data class Icon(
    @SerialName("URL") val url: String = "",
    @SerialName("Height") val height: Int = 0,
    @SerialName("Width") val width: Int = 0
)

@Serializable
private data class Infobox(
    @SerialName("content") val content: List<InfoboxContent> = emptyList()
)

@Serializable
private data class InfoboxContent(
    @SerialName("label") val label: String = "",
    @SerialName("value") val value: String = ""
)

package com.augmentalis.intentactions.actions

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext
import com.augmentalis.intentactions.math.MathCalculator
import com.augmentalis.intentactions.web.DuckDuckGoSearchService
import com.augmentalis.intentactions.web.SearchResult

/**
 * Performs a web search using DuckDuckGo Instant Answer API.
 *
 * Provides in-app answers when possible, with fallback to browser search.
 */
object WebSearchAction : IIntentAction {
    private const val TAG = "WebSearchAction"
    private val searchService = DuckDuckGoSearchService()

    override val intentId = "search_web"
    override val category = IntentCategory.SEARCH
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Searching web with entities: $entities")

            val query = entities.query
            if (query.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What would you like to search for?"
                )
            }

            val searchResult = searchService.search(query)

            when (searchResult) {
                is SearchResult.Success -> {
                    val response = formatResultsForDisplay(searchResult)
                    Log.i(TAG, "In-app search successful for '$query'")
                    IntentResult.Success(
                        message = response,
                        data = mapOf(
                            "query" to query,
                            "resultCount" to searchResult.snippets.size,
                            "hasInstantAnswer" to (searchResult.instantAnswer != null),
                            "searchContext" to searchService.formatForLLM(searchResult)
                        )
                    )
                }

                is SearchResult.NoResults -> {
                    Log.w(TAG, "No results for '$query', falling back to browser")
                    executeBrowserSearch(context, query)
                }

                is SearchResult.Error -> {
                    Log.e(TAG, "Search error: ${searchResult.message}, falling back to browser")
                    executeBrowserSearch(context, query)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search web", e)
            IntentResult.Failed(
                reason = "Failed to search: ${e.message}",
                exception = e
            )
        }
    }

    private fun executeBrowserSearch(context: PlatformContext, query: String): IntentResult {
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            IntentResult.Success(message = "Searching for $query")
        } catch (e: Exception) {
            IntentResult.Failed(
                reason = "Failed to open web search: ${e.message}",
                exception = e
            )
        }
    }

    private fun formatResultsForDisplay(result: SearchResult.Success): String {
        return buildString {
            if (result.instantAnswer != null) {
                append(result.instantAnswer)
                if (result.snippets.size > 1) {
                    append("\n\n")
                    append("Related: ")
                    append(result.snippets.drop(1).take(2).joinToString(", ") {
                        it.title.take(30)
                    })
                }
            } else if (result.snippets.isNotEmpty()) {
                append("Here's what I found about \"${result.query}\":\n\n")
                result.snippets.take(3).forEach { snippet ->
                    append("* ${snippet.content.take(200)}")
                    if (snippet.content.length > 200) append("...")
                    append("\n\n")
                }
            }
        }.trim()
    }
}

/**
 * Opens a URL in the device's default browser.
 */
object NavigateURLAction : IIntentAction {
    private const val TAG = "NavigateURLAction"

    override val intentId = "navigate_url"
    override val category = IntentCategory.SEARCH
    override val requiredEntities = listOf(EntityType.URL)

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Navigating to URL with entities: $entities")

            val url = entities.url
            if (url.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.URL,
                    prompt = "What website would you like to visit?"
                )
            }

            // Ensure URL has a scheme
            val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                url
            } else {
                "https://$url"
            }

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)

            Log.i(TAG, "Opened URL: $fullUrl")
            IntentResult.Success(message = "Opening $fullUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to URL", e)
            IntentResult.Failed(
                reason = "Failed to open website: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Performs mathematical calculations using the KMP MathCalculator.
 */
object CalculateAction : IIntentAction {
    private const val TAG = "CalculateAction"

    override val intentId = "perform_calculation"
    override val category = IntentCategory.SEARCH
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Calculating with entities: $entities")

            val expression = entities.query
            if (expression.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What would you like me to calculate?"
                )
            }

            val calcResult = MathCalculator.calculate(expression)

            if (calcResult.success) {
                val message = if (calcResult.expression.isNotEmpty()) {
                    "${calcResult.expression} = ${calcResult.formattedResult}"
                } else {
                    calcResult.formattedResult
                }

                Log.i(TAG, "Calculation successful: $message")
                IntentResult.Success(
                    message = message,
                    data = mapOf(
                        "result" to (calcResult.result ?: 0.0),
                        "expression" to calcResult.expression,
                        "formatted" to calcResult.formattedResult
                    )
                )
            } else {
                Log.w(TAG, "Calculation failed: ${calcResult.error}")
                IntentResult.Failed(
                    reason = "I couldn't calculate that. ${calcResult.error ?: "Please try rephrasing."}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate", e)
            IntentResult.Failed(
                reason = "Calculation failed: ${e.message}",
                exception = e
            )
        }
    }
}

# Chapter 56: Web Search Integration

**Status:** IMPLEMENTED
**Last Updated:** 2025-12-01
**Module:** Universal/AVA/Features/Actions
**Purpose:** DuckDuckGo API integration for voice-activated web searches

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [DuckDuckGo API](#duckduckgo-api)
4. [Implementation](#implementation)
5. [Action Handler](#action-handler)
6. [Training Data](#training-data)
7. [LLM Summarization](#llm-summarization)
8. [Usage Examples](#usage-examples)
9. [References](#references)

---

## Overview

AVA integrates web search capabilities using the DuckDuckGo Instant Answer API, enabling voice-activated information retrieval without requiring API keys or user tracking.

### Key Features

| Feature | Description |
|---------|-------------|
| **No API Key** | DuckDuckGo API is free and requires no authentication |
| **Privacy-focused** | No user tracking or data collection |
| **Instant Answers** | Direct answers for factual queries |
| **LLM Integration** | Optional summarization via local LLM |
| **In-app Results** | Results displayed within AVA (no browser redirect) |

### Voice Commands

| Command | Example |
|---------|---------|
| Direct search | "Search for quantum computing" |
| Question | "What is the capital of France" |
| Who/What/Where | "Who is Albert Einstein" |
| How-to | "How to tie a tie" |
| Definition | "Define photosynthesis" |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Voice Input                                  │
│                    "What is Mars"                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NLU / IntentClassifier                        │
│                   Intent: INFO:search_web                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SearchWebActionHandler                          │
│                  - Extract query from utterance                  │
│                  - Call DuckDuckGo API                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  DuckDuckGoSearchService                         │
│                  - HTTP request to API                           │
│                  - Parse JSON response                           │
│                  - Extract instant answers                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Optional: LLM Summarization                         │
│              - Format results as context                         │
│              - Generate natural response                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    TTS Response                                  │
│        "Mars is the fourth planet from the Sun..."               │
└─────────────────────────────────────────────────────────────────┘
```

---

## DuckDuckGo API

### Endpoint

```
https://api.duckduckgo.com/?q={query}&format=json&no_html=1&skip_disambig=1
```

### Response Structure

| Field | Type | Description |
|-------|------|-------------|
| `Abstract` | String | Main answer text |
| `AbstractSource` | String | Source website name |
| `AbstractURL` | String | Source URL |
| `Heading` | String | Topic heading |
| `Answer` | String | Direct factual answer |
| `RelatedTopics` | Array | Related search results |
| `Infobox` | Object | Structured data (dates, facts) |

### Example Response

```json
{
  "Abstract": "Mars is the fourth planet from the Sun...",
  "AbstractSource": "Wikipedia",
  "AbstractURL": "https://en.wikipedia.org/wiki/Mars",
  "Heading": "Mars",
  "RelatedTopics": [
    {"Text": "Mars rover - robotic vehicle...", "FirstURL": "..."}
  ]
}
```

---

## Implementation

### DuckDuckGoSearchService

**Location:** `Universal/AVA/Features/Actions/src/main/kotlin/com/augmentalis/ava/features/actions/web/DuckDuckGoSearchService.kt`

```kotlin
class DuckDuckGoSearchService {
    companion object {
        private const val BASE_URL = "https://api.duckduckgo.com/"
        private const val TIMEOUT_MS = 10000
    }

    suspend fun search(query: String): SearchResult

    fun formatForLLM(result: SearchResult): String
}
```

### SearchResult Types

```kotlin
sealed class SearchResult {
    data class Success(
        val query: String,
        val snippets: List<SearchSnippet>,
        val instantAnswer: String?
    ) : SearchResult()

    data class NoResults(val query: String) : SearchResult()

    data class Error(val message: String) : SearchResult()
}

data class SearchSnippet(
    val title: String,
    val content: String,
    val url: String,
    val source: String
)
```

---

## Action Handler

### WebSearchActionHandler

**Location:** `Universal/AVA/Features/Actions/src/main/kotlin/com/augmentalis/ava/features/actions/handlers/WebSearchActionHandler.kt`

```kotlin
class WebSearchActionHandler(
    private val searchService: DuckDuckGoSearchService = DuckDuckGoSearchService(),
    private val llmSummarizer: LLMSummarizer? = null
) : IntentActionHandler {

    override val intent = "web.search"

    override suspend fun execute(context: Context, utterance: String): ActionResult
}
```

### Query Extraction Patterns

| Pattern | Example Match |
|---------|---------------|
| `search\|look up\|google (.+)` | "search for cats" -> "cats" |
| `what is\|are (.+)` | "what is gravity" -> "gravity" |
| `who is\|was (.+)` | "who is Einstein" -> "Einstein" |
| `how to\|do (.+)` | "how to cook rice" -> "cook rice" |
| `tell me about (.+)` | "tell me about Mars" -> "Mars" |

---

## Training Data

### AVA File Format

**Location:** `apps/ava-app-android/src/main/assets/ava-examples/en-US/information.ava`

```
INFO:search_web:search for
INFO:search_web:look up
INFO:search_web:google
INFO:search_web:what is
INFO:search_web:who is
INFO:search_web:where is
INFO:search_web:how to
INFO:search_web:tell me about
INFO:search_web:explain
INFO:search_web:define
```

---

## LLM Summarization

### Optional Integration

```kotlin
interface LLMSummarizer {
    suspend fun summarize(userQuery: String, searchContext: String): String
}
```

### Context Format for LLM

```
Web search results for: "Mars"

Direct Answer: Mars is the fourth planet from the Sun...

1. Mars (planet)
   Mars is the fourth planet from the Sun and the second-smallest...
   Source: Wikipedia - https://en.wikipedia.org/wiki/Mars

2. Mars exploration
   Mars exploration is the ongoing study of Mars...
```

---

## Usage Examples

### Basic Search

```kotlin
val handler = WebSearchActionHandler()
val result = handler.execute(context, "What is quantum computing")

// Result: "Quantum computing is a type of computation that harnesses..."
```

### With LLM Summarization

```kotlin
val handler = WebSearchActionHandler(
    searchService = DuckDuckGoSearchService(),
    llmSummarizer = LocalLLMSummarizer(llmProvider)
)

val result = handler.execute(context, "Explain black holes")
// Result: Natural language response from LLM based on search results
```

---

## References

| Resource | Link |
|----------|------|
| DuckDuckGo API | https://api.duckduckgo.com/api |
| Chapter 49 | Action Handlers |
| Chapter 34 | Intent Management |

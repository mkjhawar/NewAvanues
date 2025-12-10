# AVA JARVIS Vision - Part 2: Contextual Intelligence

**Date:** 2025-11-05
**Status:** Planning Phase
**Priority:** HIGH

---

## 1. Conversation Memory

### Overview
Remember previous conversations, extract key facts, and maintain context across sessions.

### Implementation
```kotlin
class ConversationMemory(
    private val database: RAGDatabase
) {
    suspend fun storeConversation(
        conversation: Conversation,
        retention: Duration = 7.days
    ) {
        // Store full conversation
        database.conversationDao().insert(conversation.toEntity())

        // Extract key facts
        val facts = extractKeyFacts(conversation)
        facts.forEach { fact ->
            database.factDao().insert(fact)
        }

        // Auto-summarize if long
        if (conversation.messages.size > 20) {
            val summary = llm.summarize(conversation)
            database.summaryDao().insert(summary)
        }
    }

    suspend fun recall(query: String): ConversationContext {
        // Vector search over past conversations
        val relevantConvos = rag.search(
            SearchQuery(
                query = query,
                filters = SearchFilters(documentType = "conversation")
            )
        )

        // Extract relevant facts
        val relevantFacts = database.factDao().searchFacts(query)

        return ConversationContext(
            previousConversations = relevantConvos,
            extractedFacts = relevantFacts
        )
    }
}
```

### Key Fact Extraction
```kotlin
data class ExtractedFact(
    val subject: String,        // "User's car"
    val attribute: String,      // "model"
    val value: String,          // "2015 Honda Civic"
    val confidence: Float,      // 0.95
    val source: String,         // "Conversation on 2025-11-03"
    val timestamp: Instant,
    val expiresAt: Instant?     // Some facts expire (e.g., mileage)
)

suspend fun extractKeyFacts(conversation: Conversation): List<ExtractedFact> {
    // Use LLM to extract structured facts
    val prompt = """
    Extract key facts from this conversation:

    ${conversation.messages.joinToString("\n")}

    Output format:
    - Subject | Attribute | Value | Confidence
    """

    val response = llm.generate(prompt)
    return parseFactsFromResponse(response)
}
```

### Example Usage
```
User: "My car is a 2015 Honda Civic"
AVA: "Got it, I'll remember that."
     → Stores: ExtractedFact("User's car", "model", "2015 Honda Civic", 0.95)

[Next day]
User: "When should I change my oil?"
AVA: "Your Civic needs oil changes every 5,000 miles according to your manual."
     → Recalls: User has 2015 Honda Civic
     → Searches: Civic manual for oil change interval
```

### Schema
```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val summary: String?,
    val messageCount: Int,
    val durationSeconds: Int
)

@Entity(tableName = "conversation_facts")
data class FactEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val attribute: String,
    val value: String,
    val confidence: Float,
    val source_conversation_id: String,
    val timestamp: String,
    val expires_at: String?
)
```

**Priority:** 🟡 HIGH

---

## 2. Proactive Suggestions

### Overview
Anticipate user needs based on context, usage patterns, and document content.

### Implementation
```kotlin
class ProactiveAssistant(
    private val ragRepository: RAGRepository,
    private val userPreferences: UserPreferences,
    private val calendar: CalendarAccess
) {
    suspend fun generateSuggestions(): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()

        // Maintenance reminders
        suggestions.addAll(checkMaintenanceSchedule())

        // Seasonal suggestions
        suggestions.addAll(getSeasonalSuggestions())

        // Document-based suggestions
        suggestions.addAll(analyzeRecentActivity())

        // Usage pattern suggestions
        suggestions.addAll(predictUserNeeds())

        return suggestions.sortedByDescending { it.priority }
    }

    private suspend fun checkMaintenanceSchedule(): List<Suggestion> {
        val mileage = userPreferences.currentMileage
        val lastOilChange = userPreferences.lastOilChangeMileage
        val oilChangeInterval = 5000 // From manual

        val milesSinceOilChange = mileage - lastOilChange

        return if (milesSinceOilChange >= oilChangeInterval - 500) {
            listOf(
                Suggestion(
                    title = "Oil change due soon",
                    description = "You're at $mileage miles. Due at ${lastOilChange + oilChangeInterval}",
                    priority = Priority.HIGH,
                    confidence = 0.95f,
                    source = "Maintenance schedule (Manual page 45)",
                    actions = listOf(
                        Action("Schedule appointment", { scheduleService() }),
                        Action("Snooze", { snooze(7.days) }),
                        Action("Mark complete", { recordMaintenance() })
                    )
                )
            )
        } else emptyList()
    }

    private suspend fun getSeasonalSuggestions(): List<Suggestion> {
        val season = getCurrentSeason()
        val location = userPreferences.location

        return when (season) {
            Season.WINTER -> listOf(
                Suggestion(
                    title = "Winter maintenance check",
                    description = "Check tire pressure, antifreeze, and battery",
                    priority = Priority.MEDIUM,
                    confidence = 0.85f,
                    source = "Seasonal maintenance guide",
                    actions = listOf(
                        Action("View checklist", { showWinterChecklist() }),
                        Action("Dismiss", { })
                    )
                )
            )
            // ... other seasons
        }
    }

    private suspend fun analyzeRecentActivity(): List<Suggestion> {
        // Look at recent searches/conversations
        val recentTopics = getRecentTopics()

        // Find related content user hasn't seen
        return recentTopics.flatMap { topic ->
            ragRepository.search(
                SearchQuery(
                    query = topic,
                    filters = SearchFilters(
                        excludeRecentlyViewed = true
                    )
                )
            ).getOrNull()?.results?.take(1)?.map { result ->
                Suggestion(
                    title = "Related: ${result.document?.title}",
                    description = result.chunk.content.take(100),
                    priority = Priority.LOW,
                    confidence = result.similarity,
                    source = result.document?.title ?: "Unknown",
                    actions = listOf(
                        Action("Read more", { openDocument(result) })
                    )
                )
            } ?: emptyList()
        }
    }
}
```

### Suggestion Display
```kotlin
@Composable
fun ProactiveSuggestionCard(suggestion: Suggestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Badge {
                    Text("${(suggestion.confidence * 100).toInt()}%")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Source: ${suggestion.source}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                suggestion.actions.forEach { action ->
                    TextButton(onClick = action.handler) {
                        Text(action.label)
                    }
                }
            }
        }
    }
}
```

**Priority:** 🟡 HIGH

---

## 3. Multi-Document Context Assembly

### Overview
Search across multiple document types and synthesize comprehensive answers.

### Implementation
```kotlin
class ContextualRAG(
    private val ragRepository: RAGRepository,
    private val conversationMemory: ConversationMemory
) {
    suspend fun answerWithFullContext(
        query: String,
        previousMessages: List<Message> = emptyList()
    ): ContextualResponse {
        // 1. Recall conversation context
        val conversationContext = conversationMemory.recall(query)

        // 2. Search vehicle manual
        val manualResults = ragRepository.search(
            SearchQuery(
                query = query,
                filters = SearchFilters(documentType = "vehicle_manual"),
                maxResults = 5
            )
        ).getOrThrow()

        // 3. Search service history
        val serviceResults = ragRepository.search(
            SearchQuery(
                query = query,
                filters = SearchFilters(documentType = "service_history"),
                maxResults = 3
            )
        ).getOrNull()?.results ?: emptyList()

        // 4. Search past conversations
        val conversationResults = ragRepository.search(
            SearchQuery(
                query = query,
                filters = SearchFilters(documentType = "conversation"),
                maxResults = 3
            )
        ).getOrNull()?.results ?: emptyList()

        // 5. Assemble context
        val context = assembleContext(
            manual = manualResults.results,
            service = serviceResults,
            conversations = conversationResults,
            facts = conversationContext.extractedFacts
        )

        // 6. Generate response
        val response = llm.generateStream(
            prompt = buildContextualPrompt(query, context, previousMessages)
        )

        return ContextualResponse(
            responseStream = response,
            sources = context.sources,
            confidence = context.confidence
        )
    }

    private fun assembleContext(
        manual: List<SearchResult>,
        service: List<SearchResult>,
        conversations: List<SearchResult>,
        facts: List<ExtractedFact>
    ): AssembledContext {
        val contextParts = mutableListOf<ContextPart>()

        // Add manual information
        if (manual.isNotEmpty()) {
            contextParts.add(
                ContextPart(
                    type = "manual",
                    content = manual.joinToString("\n\n") {
                        "${it.document?.title} (page ${it.chunk.metadata["page_number"]}): ${it.chunk.content}"
                    },
                    weight = 1.0f
                )
            )
        }

        // Add service history
        if (service.isNotEmpty()) {
            contextParts.add(
                ContextPart(
                    type = "service_history",
                    content = "Your service history shows: " + service.joinToString("; ") {
                        it.chunk.content
                    },
                    weight = 0.8f
                )
            )
        }

        // Add relevant facts
        if (facts.isNotEmpty()) {
            contextParts.add(
                ContextPart(
                    type = "facts",
                    content = "What I know about your vehicle: " + facts.joinToString(", ") {
                        "${it.attribute}: ${it.value}"
                    },
                    weight = 0.9f
                )
            )
        }

        // Add previous conversations
        if (conversations.isNotEmpty()) {
            contextParts.add(
                ContextPart(
                    type = "conversation",
                    content = "We previously discussed: " + conversations.joinToString("; ") {
                        it.chunk.content.take(100)
                    },
                    weight = 0.6f
                )
            )
        }

        return AssembledContext(
            parts = contextParts,
            sources = extractSources(manual, service, conversations),
            confidence = calculateOverallConfidence(manual, service, facts)
        )
    }

    private fun buildContextualPrompt(
        query: String,
        context: AssembledContext,
        previousMessages: List<Message>
    ): String {
        return """
You are AVA, an automotive assistant with access to vehicle manuals and service history.

Context from documents:
${context.parts.joinToString("\n\n") { "${it.type.uppercase()}:\n${it.content}" }}

Previous conversation:
${previousMessages.takeLast(5).joinToString("\n") { "${it.role}: ${it.content}" }}

User question: $query

Instructions:
- Answer based on the provided context
- Cite specific sources (page numbers, dates)
- If information is from service history, mention that
- If unsure, say so clearly
- Be conversational but professional

Response:
        """.trimIndent()
    }
}
```

### Example Multi-Document Query
```
User: "When was my last oil change and when is the next one due?"

AVA searches:
1. Vehicle manual → "Oil change interval: 5,000 miles"
2. Service history → "Last oil change: 2025-09-15 at 38,000 miles"
3. User facts → "Current mileage: 42,500 miles"

AVA responds:
"Your last oil change was on September 15th at 38,000 miles. Your Civic's
manual recommends oil changes every 5,000 miles, so you're due at 43,000
miles. You're currently at 42,500 miles, so you should schedule it soon -
you have about 500 miles left."

Sources:
- Honda Civic 2015 Manual, page 234
- Service record from Joe's Auto Shop
```

**Priority:** 🟡 HIGH

---

## 4. Intelligent Query Understanding

### Overview
Understand user intent even with vague or incomplete queries.

### Implementation
```kotlin
class QueryUnderstanding(
    private val llm: MLCEngine,
    private val conversationMemory: ConversationMemory
) {
    suspend fun clarifyQuery(
        query: String,
        context: ConversationContext
    ): ClarifiedQuery {
        // Expand abbreviations
        val expanded = expandAbbreviations(query)

        // Resolve pronouns using context
        val resolved = resolvePronouns(expanded, context)

        // Add implicit context
        val enriched = addImplicitContext(resolved, context)

        return ClarifiedQuery(
            original = query,
            clarified = enriched,
            assumptions = listOf(/* what we assumed */)
        )
    }

    private fun expandAbbreviations(query: String): String {
        val abbrevMap = mapOf(
            "CEL" to "check engine light",
            "OBD" to "on-board diagnostics",
            "PSI" to "pounds per square inch",
            "MPG" to "miles per gallon",
            // ... automotive abbreviations
        )

        var result = query
        abbrevMap.forEach { (abbrev, full) ->
            result = result.replace(
                Regex("\\b$abbrev\\b", RegexOption.IGNORE_CASE),
                full
            )
        }
        return result
    }

    private fun resolvePronouns(
        query: String,
        context: ConversationContext
    ): String {
        // "What does that code mean?" → "What does code P0420 mean?"
        // Uses previous conversation to resolve "that"

        val pronouns = listOf("it", "that", "this", "they")
        if (pronouns.any { query.contains(it, ignoreCase = true) }) {
            // Find most recent entity mentioned
            val recentEntity = context.extractedFacts
                .maxByOrNull { it.timestamp }
                ?.value

            return if (recentEntity != null) {
                query.replace(
                    Regex("\\b(it|that|this)\\b", RegexOption.IGNORE_CASE),
                    recentEntity
                )
            } else query
        }

        return query
    }

    private fun addImplicitContext(
        query: String,
        context: ConversationContext
    ): String {
        // Add vehicle context if relevant
        val vehicleModel = context.extractedFacts
            .find { it.attribute == "model" }
            ?.value

        return if (vehicleModel != null && !query.contains(vehicleModel)) {
            "$query (for $vehicleModel)"
        } else query
    }
}
```

### Example Query Clarification
```
User: "What's that noise when I brake?"
Context: Previous message mentioned "grinding sound from front wheel"

Clarified: "What causes a grinding sound from the front wheel when braking?"

User: "When should I change it?"
Context: Previous topic was "brake pads"

Clarified: "When should I change brake pads?"

User: "CEL is on"
Clarified: "Check engine light is on"
```

**Priority:** 🟢 MEDIUM

---

## 5. Learning from Interactions

### Overview
Improve responses over time based on user feedback and behavior.

### Implementation
```kotlin
class FeedbackLearning(
    private val database: RAGDatabase
) {
    suspend fun recordFeedback(
        query: String,
        response: String,
        feedback: Feedback
    ) {
        database.feedbackDao().insert(
            FeedbackEntity(
                query = query,
                response = response,
                rating = feedback.rating, // 1-5 stars
                wasHelpful = feedback.wasHelpful,
                corrections = feedback.corrections,
                timestamp = Instant.now()
            )
        )

        // Adjust future responses based on feedback
        if (feedback.corrections != null) {
            updateResponseTemplates(query, feedback.corrections)
        }
    }

    suspend fun getResponseQuality(topic: String): ResponseMetrics {
        val feedback = database.feedbackDao()
            .getFeedbackForTopic(topic)

        return ResponseMetrics(
            averageRating = feedback.map { it.rating }.average(),
            helpfulRate = feedback.count { it.wasHelpful } / feedback.size.toFloat(),
            commonIssues = identifyCommonIssues(feedback)
        )
    }
}
```

### Feedback UI
```kotlin
@Composable
fun ResponseFeedback(
    response: String,
    onFeedback: (Feedback) -> Unit
) {
    Row {
        IconButton(onClick = { onFeedback(Feedback.Helpful) }) {
            Icon(Icons.Default.ThumbUp, "Helpful")
        }
        IconButton(onClick = { onFeedback(Feedback.NotHelpful) }) {
            Icon(Icons.Default.ThumbDown, "Not helpful")
        }
        IconButton(onClick = { showDetailedFeedback() }) {
            Icon(Icons.Default.MoreVert, "More feedback")
        }
    }
}
```

**Priority:** 🟢 MEDIUM

---

## Implementation Timeline

### Week 1: Conversation Memory
- [ ] Design database schema
- [ ] Implement fact extraction
- [ ] Test conversation recall
- [ ] Add expiration logic

### Week 2: Proactive Suggestions
- [ ] Maintenance schedule logic
- [ ] Seasonal suggestions
- [ ] Usage pattern analysis
- [ ] Suggestion UI

### Week 3: Multi-Document Context
- [ ] Cross-document search
- [ ] Context assembly algorithm
- [ ] Source citation
- [ ] Confidence scoring

### Week 4: Query Understanding
- [ ] Abbreviation expansion
- [ ] Pronoun resolution
- [ ] Context enrichment
- [ ] Testing

---

**Next:** Part 3 - UI/UX Excellence

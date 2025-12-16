# NewAvanues - API Contracts

**Version:** 12.0.0
**Updated:** 2025-12-15

---

## Overview

This document defines API contracts for cross-module communication in the NewAvanues monorepo.

## Contract Principles

1. **Versioned:** All APIs include version numbers
2. **Backward Compatible:** Breaking changes require new versions
3. **Documented:** Complete parameter and return type documentation
4. **Tested:** 100% API coverage requirement
5. **Typed:** Strong typing with Kotlin data classes

## Module API Contracts

### VoiceOS APIs

#### Accessibility Event API
```kotlin
interface IAccessibilityEventService {
    /**
     * Get current accessibility events
     * @return Flow of accessibility events
     */
    fun observeEvents(): Flow<AccessibilityEvent>

    /**
     * Perform accessibility action
     * @param nodeId Target node ID
     * @param action Action to perform
     * @return Success status
     */
    suspend fun performAction(
        nodeId: String,
        action: AccessibilityAction
    ): Result<Boolean>
}
```

#### Voice Command API
```kotlin
interface IVoiceCommandService {
    /**
     * Execute voice command
     * @param command Command text
     * @param context Current screen context
     * @return Execution result
     */
    suspend fun executeCommand(
        command: String,
        context: ScreenContext
    ): CommandResult
}
```

### AVA APIs

#### AI Assistant API
```kotlin
interface IAIAssistantService {
    /**
     * Process user query
     * @param query User input
     * @param conversationId Conversation context
     * @return AI response
     */
    suspend fun processQuery(
        query: String,
        conversationId: String
    ): AIResponse

    /**
     * Get conversation history
     * @param conversationId Conversation ID
     * @return Message history
     */
    suspend fun getHistory(
        conversationId: String
    ): List<Message>
}
```

### WebAvanue APIs

#### Web Navigation API
```kotlin
interface IWebNavigationService {
    /**
     * Navigate to URL
     * @param url Target URL
     * @param options Navigation options
     * @return Navigation result
     */
    suspend fun navigateTo(
        url: String,
        options: NavigationOptions = NavigationOptions.Default
    ): NavigationResult

    /**
     * Extract page content
     * @return Structured content
     */
    suspend fun extractContent(): PageContent
}
```

#### Privacy API
```kotlin
interface IPrivacyService {
    /**
     * Enable ad blocking
     * @param enabled Block ads if true
     */
    suspend fun setAdBlocking(enabled: Boolean)

    /**
     * Enable tracker blocking
     * @param enabled Block trackers if true
     */
    suspend fun setTrackerBlocking(enabled: Boolean)

    /**
     * Get blocked items count
     * @return Count of blocked ads/trackers
     */
    suspend fun getBlockedCount(): BlockedStats
}
```

### Cockpit APIs

#### Module Management API
```kotlin
interface IModuleManagementService {
    /**
     * Get module status
     * @param moduleId Module identifier
     * @return Module health status
     */
    suspend fun getModuleStatus(
        moduleId: String
    ): ModuleStatus

    /**
     * Restart module
     * @param moduleId Module to restart
     * @return Restart result
     */
    suspend fun restartModule(
        moduleId: String
    ): Result<Unit>
}
```

#### Configuration API
```kotlin
interface IConfigurationService {
    /**
     * Get configuration value
     * @param key Configuration key
     * @return Configuration value
     */
    suspend fun <T> getConfig(key: String): T?

    /**
     * Update configuration
     * @param key Configuration key
     * @param value New value
     * @return Update result
     */
    suspend fun <T> setConfig(
        key: String,
        value: T
    ): Result<Unit>
}
```

### NLU APIs

#### Intent Recognition API
```kotlin
interface IIntentRecognitionService {
    /**
     * Recognize intent from text
     * @param text User input text
     * @param context Conversation context
     * @return Recognized intent with confidence
     */
    suspend fun recognizeIntent(
        text: String,
        context: Map<String, Any> = emptyMap()
    ): IntentResult
}
```

#### Entity Extraction API
```kotlin
interface IEntityExtractionService {
    /**
     * Extract entities from text
     * @param text Input text
     * @return List of extracted entities
     */
    suspend fun extractEntities(
        text: String
    ): List<Entity>
}
```

## Data Models

### Common Models

```kotlin
/**
 * Standard result wrapper
 */
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(
        val message: String,
        val code: Int,
        val cause: Throwable? = null
    ) : Result<T>()
}

/**
 * API response metadata
 */
data class ResponseMetadata(
    val timestamp: Long,
    val requestId: String,
    val version: String
)

/**
 * Error response
 */
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)
```

## Versioning Strategy

**Format:** `v{major}.{minor}.{patch}`

**Breaking Changes:** Increment major version
**New Features:** Increment minor version
**Bug Fixes:** Increment patch version

**Current API Versions:**
- VoiceOS: v1.0.0
- AVA: v1.0.0
- WebAvanue: v1.0.0
- Cockpit: v1.0.0
- NLU: v1.0.0

## Error Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Invalid request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not found |
| 500 | Internal error |
| 503 | Service unavailable |

## Testing Requirements

**API Coverage:** 100% required

**Test Types:**
- Unit tests for individual methods
- Integration tests for cross-module calls
- Contract tests for API compatibility
- Performance tests for response times

## Module-Specific Documentation

For detailed API documentation per module, see:
- VoiceOS: `Docs/VoiceOS/LivingDocs/LD-VOS-API-Contracts-V1.md`
- AVA: `Docs/AVA/LivingDocs/LD-AVA-API-Contracts-V1.md`
- WebAvanue: `Docs/WebAvanue/LivingDocs/LD-WEB-API-Contracts-V1.md`
- Cockpit: `Docs/Cockpit/LivingDocs/LD-CPT-API-Contracts-V1.md`
- NLU: `Docs/NLU/LivingDocs/LD-NLU-API-Contracts-V1.md`

---

All APIs must maintain these contracts. Breaking changes require approval and coordinated deployment.

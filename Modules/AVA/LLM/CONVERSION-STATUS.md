# LLM Response Generation - KMP Conversion Status

## Converted Files (to commonMain)

### 1. Response Templates and Generators

#### IntentTemplates.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/response/IntentTemplates.kt`
- **Status:** ✅ Converted
- **Changes:** None required - pure Kotlin data structures
- **Dependencies:** None

#### ImprovedIntentTemplates.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/response/ImprovedIntentTemplates.kt`
- **Status:** ✅ Converted
- **Changes:** None required - uses kotlin.random.Random (KMP-compatible)
- **Dependencies:** None

#### ResponseGenerator.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/response/ResponseGenerator.kt`
- **Status:** ✅ Converted
- **Changes:** None required - interface with pure Kotlin types
- **Dependencies:**
  - `com.augmentalis.ava.core.common.Result`
  - `com.augmentalis.ava.features.nlu.IntentClassification`

#### TemplateResponseGenerator.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/response/TemplateResponseGenerator.kt`
- **Status:** ✅ Converted
- **Changes:**
  - Replaced `Timber.d/e` with `Logger.d/e` (Platform Logger)
  - Added TAG constant
  - Replaced `System.currentTimeMillis()` with `expect fun getCurrentTimeMillis()`
- **Dependencies:**
  - `com.augmentalis.ava.platform.Logger`
  - `getCurrentTimeMillis()` - expect/actual in `TimeProvider.android.kt`

#### LLMContextBuilder.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/response/LLMContextBuilder.kt`
- **Status:** ✅ Converted
- **Changes:** None required - pure Kotlin string manipulation
- **Dependencies:**
  - `com.augmentalis.ava.features.nlu.IntentClassification`

### 2. Language Support

#### LanguageDetector.kt
- **Location:** `src/commonMain/kotlin/com/augmentalis/ava/features/llm/LanguageDetector.kt`
- **Status:** ✅ Converted
- **Changes:**
  - Replaced `Timber.d` with `Logger.d` (Platform Logger)
  - Added TAG constant
- **Dependencies:**
  - `com.augmentalis.ava.platform.Logger`

### 3. Platform-Specific Implementations

#### TimeProvider.android.kt
- **Location:** `src/androidMain/kotlin/com/augmentalis/ava/features/llm/response/TimeProvider.android.kt`
- **Status:** ✅ Created
- **Purpose:** Provides `System.currentTimeMillis()` implementation for Android
- **Note:** iOS/Desktop implementations should be added as needed

---

## Android-Specific Files (NOT Converted)

### 1. SystemPromptManager.kt

- **Location:** `src/main/java/com/augmentalis/ava/features/llm/SystemPromptManager.kt`
- **Status:** ❌ Cannot convert to commonMain
- **Reason:** Uses Android-specific APIs:
  - `android.content.Context` - constructor parameter
  - `java.text.SimpleDateFormat` - for date/time formatting
  - `java.util.Date` - for current date/time
- **Recommendation:**
  - Keep in `androidMain` or platform-specific source sets
  - Create KMP-compatible version if cross-platform system prompts are needed:
    - Replace `Context` with platform-agnostic configuration
    - Use `kotlinx-datetime` for date/time operations
    - Create expect/actual for platform-specific prompt additions

### 2. ModelSelector.kt

- **Location:** `src/main/java/com/augmentalis/ava/features/llm/ModelSelector.kt`
- **Status:** ❌ Cannot convert to commonMain
- **Reason:** Uses Android-specific APIs:
  - `android.content.Context` - constructor parameter
  - Depends on `HuggingFaceModelDownloader(context)` - Android-specific
  - Uses `kotlinx.coroutines.runBlocking` with Android-specific model discovery
- **Recommendation:**
  - Keep in `androidMain` or platform-specific source sets
  - For cross-platform support:
    - Extract model metadata (`ModelInfo`, `LanguageSupport`, etc.) to commonMain
    - Create platform-specific implementations of model discovery/download
    - Use expect/actual pattern for `ModelSelector` interface

---

## Architecture Notes

### Current Structure

```
Modules/AVA/LLM/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/ava/features/llm/
│   │   ├── response/
│   │   │   ├── IntentTemplates.kt          ✅ KMP
│   │   │   ├── ImprovedIntentTemplates.kt  ✅ KMP
│   │   │   ├── ResponseGenerator.kt        ✅ KMP
│   │   │   ├── TemplateResponseGenerator.kt ✅ KMP
│   │   │   └── LLMContextBuilder.kt        ✅ KMP
│   │   └── LanguageDetector.kt             ✅ KMP
│   │
│   ├── androidMain/kotlin/com/augmentalis/ava/features/llm/
│   │   └── response/
│   │       └── TimeProvider.android.kt      ✅ Platform-specific
│   │
│   └── main/java/com/augmentalis/ava/features/llm/
│       ├── SystemPromptManager.kt           ❌ Android-only
│       └── ModelSelector.kt                 ❌ Android-only
```

### Benefits of Conversion

1. **Code Reuse:** Response templates and generation logic now shared across all platforms
2. **Consistency:** Same response behavior on Android, iOS, Desktop, Web
3. **Maintainability:** Single source of truth for template logic
4. **Type Safety:** Compile-time checks across platforms

### Migration Path for Android-Only Files

#### For SystemPromptManager:
```kotlin
// Create in commonMain
expect class SystemPromptManager {
    fun buildSystemPrompt(...): String
    fun formatWithSystemPrompt(...): String
}

// Implement in androidMain
actual class SystemPromptManager(private val context: Context) {
    // Android-specific implementation
}

// Implement in iosMain
actual class SystemPromptManager(...) {
    // iOS-specific implementation using Foundation
}
```

#### For ModelSelector:
```kotlin
// Extract to commonMain
data class ModelInfo(...) // Already defined, move to commonMain
enum class LanguageSupport // Already defined, move to commonMain

expect class ModelSelector {
    fun selectBestModel(...): String
    fun getAvailableModels(): List<ModelInfo>
}

// Platform-specific implementations
```

---

## Testing Requirements

### Unit Tests Needed (commonMain)

1. **IntentTemplates:**
   - Test all intent mappings
   - Test unknown intent fallback
   - Test template retrieval

2. **ImprovedIntentTemplates:**
   - Test 3-strikes fallback mechanism
   - Test response variation randomization
   - Test confidence-based responses

3. **LanguageDetector:**
   - Test language detection for all supported languages
   - Test confidence calculation
   - Test model recommendation logic

4. **TemplateResponseGenerator:**
   - Test response generation flow
   - Test error handling
   - Test metadata in responses

5. **LLMContextBuilder:**
   - Test prompt building
   - Test token estimation
   - Test conversation context formatting

### Platform-Specific Tests (androidMain)

1. **TimeProvider:**
   - Verify time consistency
   - Test across different Android versions

---

## Dependencies

### Required for commonMain:
- `com.augmentalis.ava.platform.Logger` ✅ Available
- `com.augmentalis.ava.features.nlu.IntentClassification` ✅ Available
- `com.augmentalis.ava.core.common.Result` ✅ Available
- `kotlinx-coroutines-core` ✅ Available

### Required for androidMain:
- Android SDK (for TimeProvider)

---

## Next Steps

1. **Add iOS/Desktop TimeProvider implementations:**
   ```kotlin
   // iosMain
   actual fun getCurrentTimeMillis(): Long =
       NSDate().timeIntervalSince1970.toLong() * 1000

   // desktopMain
   actual fun getCurrentTimeMillis(): Long =
       System.currentTimeMillis()
   ```

2. **Consider migrating SystemPromptManager:**
   - Use `kotlinx-datetime` for date/time
   - Create platform-agnostic prompt configuration

3. **Consider migrating ModelSelector:**
   - Extract model metadata to commonMain
   - Create expect/actual for model discovery

4. **Add comprehensive unit tests** for all converted components

5. **Update documentation** to reflect KMP architecture

---

**Conversion Date:** 2025-12-17
**Author:** Claude Code
**Status:** ✅ Phase 1 Complete (6/8 files converted)

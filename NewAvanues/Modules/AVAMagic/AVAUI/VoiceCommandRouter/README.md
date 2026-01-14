# Voice Command Router

**High-level API for routing natural language voice commands to registered apps in the VoiceOS ecosystem.**

## Overview

The Voice Command Router provides a complete solution for:
- Pattern-based matching of voice commands against app capabilities
- Parameter extraction from natural language input
- Intent creation for app invocation
- Fuzzy matching for typo tolerance
- Ambiguity detection and resolution

## Architecture

```
VoiceCommandRouter
├── VoiceCommandMatcher    # Pattern matching & parameter extraction
├── IntentRouter           # Platform-specific Intent creation
└── RouterConfig           # Configuration options
```

## Quick Start

```kotlin
// 1. Setup Registry and Scanner
val registry = ARGRegistry()
val scanner = ARGScanner(ARGParser(), registry)
scanner.scanAll()

// 2. Create Router
val router = VoiceCommandRouter(registry)

// 3. Route Voice Commands
val result = router.route("open google.com")
when (result) {
    is RouteResult.Success -> {
        // Launch app with intent
        context.startActivity(result.intent as Intent)
    }
    is RouteResult.Ambiguous -> {
        // Show user choice dialog
        showChoiceDialog(result.matches)
    }
    is RouteResult.NoMatch -> {
        // Handle no match
        speak("I don't know how to do that")
    }
}
```

## Core Components

### 1. VoiceCommandRouter

High-level API for routing voice commands.

```kotlin
class VoiceCommandRouter(
    private val registry: ARGRegistry,
    private val config: RouterConfig = RouterConfig()
)
```

**Methods:**
- `route(voiceInput: String): RouteResult` - Route a voice command
- `routeToApp(voiceInput: String, packageName: String): RouteResult` - Route to specific app
- `getAllMatches(voiceInput: String): List<VoiceCommandMatch>` - Get all matches
- `canRoute(voiceInput: String): Boolean` - Test if command can be routed

### 2. VoiceCommandMatcher

Pattern matching and parameter extraction engine.

```kotlin
class VoiceCommandMatcher(private val registry: ARGRegistry)
```

**Features:**
- Static token matching (exact match with fuzzy fallback)
- Dynamic parameter extraction from `{placeholder}` patterns
- Levenshtein distance-based fuzzy matching (threshold: 1)
- Relevance scoring with bonuses for exact matches

**Example Patterns:**
```kotlin
// Pattern: "open {url}"
Input: "open google.com"
Output: VoiceCommandMatch(parameters = {"url": "google.com"})

// Pattern: "create note {title} with content {content}"
Input: "create note shopping list with content buy milk"
Output: VoiceCommandMatch(parameters = {
    "title": "shopping list",
    "content": "buy milk"
})
```

### 3. IntentRouter

Platform-specific Intent creation (expect/actual pattern).

```kotlin
expect class IntentRouter(registry: ARGRegistry) {
    fun createIntent(match: VoiceCommandMatch): Any
    fun createIntent(packageName: String, capability: Capability, parameters: Map<String, String>): Any
    fun canHandleIntent(intent: Any): Boolean
    fun getHandlers(intent: Any): List<String>
}
```

**Android Implementation:**
- Creates custom action Intents: `com.augmentalis.voiceos.action.{CAPABILITY_ID}`
- Adds parameters as extras with `param_` prefix
- Enhances with standard Android actions (ACTION_VIEW, ACTION_WEB_SEARCH, etc.)
- Auto-detects browsing, search, dial, send, and view operations

### 4. RouterConfig

Configuration options for routing behavior.

```kotlin
data class RouterConfig(
    val ambiguityThreshold: Float = 0.2f,       // Score diff for ambiguity
    val minimumScore: Float = 0.5f,              // Minimum valid match score
    val maxAmbiguousMatches: Int = 5,            // Max matches in ambiguous result
    val enableFuzzyMatching: Boolean = true      // Enable typo tolerance
)
```

## Route Results

### RouteResult.Success
```kotlin
data class Success(
    val match: VoiceCommandMatch,  // Matched capability
    val intent: Any                 // Platform-specific Intent
)
```

### RouteResult.Ambiguous
```kotlin
data class Ambiguous(
    val voiceInput: String,                     // Original input
    val matches: List<VoiceCommandMatch>        // Top matches (sorted by score)
)
```

### RouteResult.NoMatch
```kotlin
data class NoMatch(
    val voiceInput: String  // Original input that didn't match
)
```

## Pattern Matching Algorithm

1. **Tokenization**: Split pattern and input into tokens
2. **Static Matching**: Exact match with fuzzy fallback (Levenshtein distance ≤ 1)
3. **Dynamic Matching**: Extract parameters from `{placeholder}` tokens
4. **Scoring**:
   - Base score: 1.0
   - Exact token match: +0.1
   - Fuzzy token match: -0.1
   - Exact input length match: +0.2
   - Unused input tokens: -0.05 per token

## Fuzzy Matching

Uses standard Levenshtein distance algorithm with dynamic programming.

**Requirements:**
- Both strings must be ≥ 3 characters
- Distance must be ≤ 1 (one insertion, deletion, or substitution)

**Examples:**
- ✅ "open" ↔ "oppen" (distance 1: deletion)
- ✅ "open" ↔ "oen" (distance 1: deletion)
- ❌ "open" ↔ "opne" (distance 2: transposition counts as 2 ops)
- ❌ "op" ↔ "or" (too short: < 3 characters)

## Testing

The module includes comprehensive unit tests for:
- Pattern matching (exact and fuzzy)
- Parameter extraction (single and multiple params)
- Scoring and sorting
- Router configuration
- Match counting

**Run Tests:**
```bash
./gradlew :modules:MagicIdea:Components:VoiceCommandRouter:test
```

**Note:** Full routing tests with Intent creation require Android instrumentation tests due to Android framework dependencies.

## Integration with ARG Scanner

Voice Command Router depends on ARG Scanner for capability discovery:

```kotlin
// Register apps from ARG files
val registry = ARGRegistry()
val scanner = ARGScanner(ARGParser(), registry)
scanner.scanAll()  // Discovers all ARG files

// Router uses registry for matching
val router = VoiceCommandRouter(registry)
```

## Example: Complete Workflow

```kotlin
// 1. Setup
val registry = ARGRegistry()
val parser = ARGParser()
val scanner = ARGScanner(parser, registry)

// 2. Discover Apps
scanner.scanAll()
println("Discovered ${registry.getAll().size} apps")

// 3. Create Router
val config = RouterConfig(
    ambiguityThreshold = 0.15f,  // Stricter ambiguity detection
    minimumScore = 0.6f,          // Higher quality threshold
    enableFuzzyMatching = true    // Allow typos
)
val router = VoiceCommandRouter(registry, config)

// 4. Route Commands
val commands = listOf(
    "open github.com",
    "search kotlin tutorials",
    "create note meeting notes"
)

commands.forEach { input ->
    when (val result = router.route(input)) {
        is RouteResult.Success -> {
            println("✅ $input → ${result.match.app.name}")
            println("   Parameters: ${result.match.parameters}")
            // startActivity(result.intent as Intent)
        }
        is RouteResult.Ambiguous -> {
            println("❓ $input → ${result.matches.size} matches")
            result.matches.forEach { match ->
                println("   - ${match.app.name} (score: ${match.score})")
            }
        }
        is RouteResult.NoMatch -> {
            println("❌ $input → No matches")
        }
    }
}
```

## Platform Support

**Current:**
- ✅ Android (full support)

**Planned:**
- ⏳ iOS (Intent creation via URL schemes)
- ⏳ macOS (Intent creation via URL schemes)
- ⏳ Windows (Intent creation via protocol handlers)

## Dependencies

```kotlin
dependencies {
    // ARG Scanner for capability discovery
    implementation(project(":modules:MagicIdea:Components:ARGScanner"))

    // Components Core for base types
    implementation(project(":modules:MagicIdea:Components:Core"))

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```

## API Stability

- ✅ **Stable**: VoiceCommandRouter, RouteResult types
- ⚠️ **Beta**: IntentRouter expect/actual (KMP multiplatform limitation)
- 🔬 **Experimental**: Fuzzy matching algorithm (may be improved with Damerau-Levenshtein)

## Performance

- **Pattern Matching**: O(n × m) where n = registered capabilities, m = input tokens
- **Levenshtein Distance**: O(a × b) where a, b = string lengths
- **Memory**: Registry size + temporary match list
- **Typical**: <10ms for 100 capabilities on modern devices

## Known Limitations

1. **Transpositions**: Levenshtein distance counts "ab" → "ba" as 2 operations (not 1)
   - Consider Damerau-Levenshtein for better transposition support
2. **Context-Free**: No conversation history or context tracking
3. **English-Only**: Pattern matching assumes English word boundaries
4. **Single Language**: No multi-language pattern support yet

## Future Enhancements

- [ ] Context-aware routing (conversation history)
- [ ] Multi-language support (i18n patterns)
- [ ] Damerau-Levenshtein distance for better typo tolerance
- [ ] Machine learning-based relevance scoring
- [ ] Voice command suggestions (autocomplete)
- [ ] Usage analytics and learning

## Version

**1.0.0** - Initial release

## License

Copyright © 2025 Augmentalis. All rights reserved.

## Related

- [ARGScanner](../ARGScanner/README.md) - Capability discovery
- [ARG File Format Specification](../ARGScanner/docs/arg-format.md) - Registry file format

---

**Created by Manoj Jhawar, manoj@ideahq.net**

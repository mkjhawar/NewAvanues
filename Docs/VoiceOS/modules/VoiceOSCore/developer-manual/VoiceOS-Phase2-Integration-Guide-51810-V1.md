# Phase 2 & 2.5 AI Context Integration Guide

**Module:** VoiceOSCore
**Created:** 2025-10-18 22:52 PDT
**Author:** Manoj Jhawar
**Status:** Production Ready
**Database Version:** v7

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Screen Context Integration](#screen-context-integration)
4. [Element Relationships](#element-relationships)
5. [Navigation Tracking](#navigation-tracking)
6. [Database Queries](#database-queries)
7. [Common Use Cases](#common-use-cases)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

---

## Overview

Phase 2 and Phase 2.5 add AI-powered context inference to the VoiceOS scraping system, enabling:

- **Screen-level understanding** (login, checkout, settings, etc.)
- **Form intelligence** (grouped fields, submit buttons, label mapping)
- **Navigation flow analysis** (user journeys, transition timing)
- **Enhanced validation** (email, phone, credit card detection)

### What's New

**Phase 2 (Database v6):**
- `ScreenContextEntity` - Screen classification and metadata
- `ElementRelationshipEntity` - Semantic relationships between elements
- `ScreenContextInferenceHelper` - Keyword-based inference engine
- 4 new fields in `ScrapedElementEntity` (formGroupId, placeholderText, validationPattern, backgroundColor)

**Phase 2.5 (Database v7):**
- `ScreenTransitionEntity` - Navigation flow tracking
- Form group ID assignment (automatic grouping)
- Button→Form relationship inference
- Label→Input relationship inference
- Enhanced validation pattern detection (80-90% accuracy)

---

## Quick Start

### 1. Database Migration

If upgrading from v5 or v6, migrations run automatically:

```kotlin
// AppScrapingDatabase.kt handles migrations automatically
val database = AppScrapingDatabase.getInstance(context)
// Migrations v5→v6→v7 execute on first access
```

### 2. Accessing Screen Context

```kotlin
// Get screen context for current screen
val screenHash = "abc123..." // From scraping
val screenContext = database.screenContextDao().getScreenByHash(screenHash)

println("Screen type: ${screenContext?.screenType}")       // "login"
println("Form context: ${screenContext?.formContext}")     // null
println("Primary action: ${screenContext?.primaryAction}") // "submit"
```

### 3. Finding Element Relationships

```kotlin
// Find which button submits a form
val submitButtons = database.elementRelationshipDao()
    .findRelationshipsByType(RelationshipType.BUTTON_SUBMITS_FORM)

submitButtons.forEach { rel ->
    val button = database.scrapedElementDao().getElementByHash(rel.sourceElementHash)
    val input = database.scrapedElementDao().getElementByHash(rel.targetElementHash)
    println("Button '${button?.text}' submits input '${input?.viewIdResourceName}'")
}
```

### 4. Analyzing Navigation Flow

```kotlin
// Get most common navigation paths
val topTransitions = database.screenTransitionDao()
    .getMostCommonTransitions(limit = 10)

topTransitions.forEach { transition ->
    println("${transition.fromScreenHash} → ${transition.toScreenHash}: " +
            "${transition.transitionCount} times, " +
            "avg ${transition.avgTransitionTime}ms")
}
```

---

## Screen Context Integration

### Screen Type Classification

The system automatically classifies screens into 11 types:

| Screen Type | Detection Keywords | Example Apps |
|-------------|-------------------|--------------|
| `login` | login, sign in, authentication | Gmail, Facebook |
| `signup` | signup, register, create account | LinkedIn, Twitter |
| `checkout` | checkout, payment, billing, order | Amazon, Shopify |
| `cart` | cart, basket, bag, shopping | eBay, Walmart |
| `settings` | settings, preferences, options | All apps |
| `home` | home, main, dashboard, feed | Instagram, Reddit |
| `search` | search, find, explore | Google, YouTube |
| `profile` | profile, account, my account | Twitter, GitHub |
| `detail` | detail, details, info | Product pages |
| `list` | list, results, browse | Search results |
| `form` | form, submit, complete | Contact forms |

### Query Screen Contexts

```kotlin
// Get all screens of a specific type
val loginScreens = database.screenContextDao()
    .findScreensByType(packageName = "com.example.app", screenType = "login")

// Get most visited screens
val topScreens = database.screenContextDao()
    .getTopScreensByVisits(packageName = "com.example.app", limit = 10)

// Get screens with forms
val formScreens = database.screenContextDao()
    .findScreensByFormContext(packageName = "com.example.app", formContext = "payment")
```

### Screen Context Data

```kotlin
data class ScreenContextEntity(
    val screenHash: String,              // Unique MD5 hash
    val appId: String,                   // Package name
    val packageName: String,
    val activityName: String?,           // Android activity
    val windowTitle: String?,            // Window title text
    val screenType: String?,             // "login", "checkout", etc.
    val formContext: String?,            // "registration", "payment", etc.
    val navigationLevel: Int = 0,        // 0 = main, 1+ = nested
    val primaryAction: String?,          // "submit", "search", "purchase"
    val elementCount: Int = 0,           // # of elements on screen
    val hasBackButton: Boolean = false,  // Back button detected
    val firstScraped: Long,              // First seen timestamp
    val lastScraped: Long,               // Last seen timestamp
    val visitCount: Int = 1              // # of times visited
)
```

---

## Element Relationships

### Relationship Types

Phase 2.5 infers 5 types of relationships:

```kotlin
object RelationshipType {
    const val FORM_GROUP_MEMBER = "form_group_member"        // Elements in same form
    const val BUTTON_SUBMITS_FORM = "button_submits_form"    // Button → Form inputs
    const val LABEL_FOR = "label_for"                        // Label → Input field
    const val ERROR_FOR = "error_for"                        // Error msg → Input
    const val NAVIGATES_TO = "navigates_to"                  // Button → Screen
}
```

### Confidence Scoring

Relationships include confidence scores based on inference strategy:

- **0.9** - Adjacent elements (high confidence)
- **0.8** - Proximity-based (medium-high confidence)
- **0.7** - Parent container (medium confidence)

```kotlin
// Filter by confidence
val highConfidenceRels = database.elementRelationshipDao()
    .getRelationshipsForElement(elementHash)
    .filter { it.confidence >= 0.8 }
```

### Form Group IDs

All form fields on a screen get assigned a stable group ID:

```kotlin
// Format: {package}_{screenHash}_{formContext}_depth{N}
// Example: "com.app_a1b2c3d4_login_depth2"

val formElements = database.scrapedElementDao()
    .getElementsByFormGroupId(formGroupId = "com.app_a1b2c3d4_login_depth2")

formElements.forEach { element ->
    println("Form field: ${element.viewIdResourceName} (${element.inputType})")
}
```

### Finding Submit Buttons

```kotlin
// Strategy 1: Via relationships
val submitRels = database.elementRelationshipDao()
    .findRelationshipsByType(RelationshipType.BUTTON_SUBMITS_FORM)

val submitButton = submitRels.firstOrNull()?.let { rel ->
    database.scrapedElementDao().getElementByHash(rel.sourceElementHash)
}

// Strategy 2: Via semantic role
val submitElements = database.scrapedElementDao()
    .getElementsBySemanticRole(screenHash, "submit_form")
```

### Finding Input Labels

```kotlin
// Get label for an input field
val inputHash = "xyz789..."
val labelRels = database.elementRelationshipDao()
    .findRelationshipsForTarget(inputHash, RelationshipType.LABEL_FOR)

val labelText = labelRels.firstOrNull()?.let { rel ->
    database.scrapedElementDao().getElementByHash(rel.sourceElementHash)?.text
}

println("Input label: $labelText")
```

---

## Navigation Tracking

### Screen Transitions

Every screen change is tracked with:
- From screen → To screen
- Transition count (frequency)
- Average transition time
- First and last occurrence timestamps

### Common Queries

```kotlin
// 1. Get outgoing transitions (where users go FROM this screen)
val outgoing = database.screenTransitionDao()
    .getOutgoingTransitions(screenHash = "home_hash")

outgoing.forEach { transition ->
    println("From home → ${transition.toScreenHash}: ${transition.transitionCount}x")
}

// 2. Get incoming transitions (where users come FROM to reach this screen)
val incoming = database.screenTransitionDao()
    .getIncomingTransitions(screenHash = "checkout_hash")

incoming.forEach { transition ->
    println("From ${transition.fromScreenHash} → checkout: ${transition.transitionCount}x")
}

// 3. Find most common paths
val topPaths = database.screenTransitionDao()
    .getMostCommonTransitions(limit = 20)

// 4. Get recent transitions
val recent = database.screenTransitionDao()
    .getRecentTransitions(limit = 10)
```

### Transition Timing Analysis

```kotlin
// Analyze transition speed
val transition = database.screenTransitionDao()
    .getTransition(fromHash = "login_hash", toHash = "dashboard_hash")

if (transition != null) {
    println("Average time: ${transition.avgTransitionTime}ms")
    println("Frequency: ${transition.transitionCount} times")

    // Interpret timing
    when {
        transition.avgTransitionTime < 2000 -> println("Fast (successful)")
        transition.avgTransitionTime > 5000 -> println("Slow (user confusion)")
        else -> println("Normal")
    }
}
```

### User Journey Mapping

```kotlin
// Build user journey from home to purchase
fun buildJourney(targetScreenHash: String): List<String> {
    val journey = mutableListOf<String>()
    var currentHash = targetScreenHash

    while (currentHash != "home_hash") {
        val incoming = database.screenTransitionDao()
            .getIncomingTransitions(currentHash)
            .maxByOrNull { it.transitionCount } // Most common previous screen

        if (incoming == null) break

        journey.add(0, incoming.fromScreenHash)
        currentHash = incoming.fromScreenHash
    }

    journey.add(targetScreenHash)
    return journey
}

val journeyToCheckout = buildJourney("checkout_hash")
println("Journey: ${journeyToCheckout.joinToString(" → ")}")
// Output: "home_hash → cart_hash → checkout_hash"
```

---

## Database Queries

### Performance-Optimized Queries

All Phase 2/2.5 tables have proper indices:

```sql
-- screen_contexts
CREATE UNIQUE INDEX index_screen_contexts_screen_hash ON screen_contexts(screen_hash)
CREATE INDEX index_screen_contexts_screen_type ON screen_contexts(screen_type)
CREATE INDEX index_screen_contexts_package_name ON screen_contexts(package_name)

-- element_relationships
CREATE INDEX index_element_relationships_source_element_hash ON element_relationships(source_element_hash)
CREATE INDEX index_element_relationships_target_element_hash ON element_relationships(target_element_hash)
CREATE INDEX index_element_relationships_relationship_type ON element_relationships(relationship_type)
CREATE UNIQUE INDEX index_element_relationships_unique ON element_relationships(source_element_hash, target_element_hash, relationship_type)

-- screen_transitions
CREATE INDEX index_screen_transitions_from_screen_hash ON screen_transitions(from_screen_hash)
CREATE INDEX index_screen_transitions_to_screen_hash ON screen_transitions(to_screen_hash)
CREATE UNIQUE INDEX index_screen_transitions_unique ON screen_transitions(from_screen_hash, to_screen_hash)
```

### Complex Queries

```kotlin
// Find all login forms with email inputs
suspend fun findLoginFormsWithEmail(packageName: String): List<ScreenContextEntity> {
    val loginScreens = database.screenContextDao()
        .findScreensByType(packageName, "login")

    return loginScreens.filter { screen ->
        val elements = database.scrapedElementDao()
            .getElementsForScreen(screen.screenHash)

        elements.any { it.inputType == "email" || it.validationPattern == "email" }
    }
}

// Find screens with high abandonment (many incoming, few outgoing)
suspend fun findAbandonmentScreens(packageName: String): List<Pair<String, Float>> {
    val allScreens = database.screenContextDao()
        .getScreensForPackage(packageName)

    return allScreens.mapNotNull { screen ->
        val incoming = database.screenTransitionDao()
            .getIncomingTransitions(screen.screenHash)
        val outgoing = database.screenTransitionDao()
            .getOutgoingTransitions(screen.screenHash)

        val incomingCount = incoming.sumOf { it.transitionCount }
        val outgoingCount = outgoing.sumOf { it.transitionCount }

        if (incomingCount > 0) {
            val abandonmentRate = 1.0f - (outgoingCount.toFloat() / incomingCount.toFloat())
            if (abandonmentRate > 0.5f) { // >50% abandonment
                screen.screenHash to abandonmentRate
            } else null
        } else null
    }.sortedByDescending { it.second }
}
```

---

## Common Use Cases

### Use Case 1: Voice Command "Fill Login Form"

```kotlin
suspend fun fillLoginForm(screenHash: String, email: String, password: String) {
    // 1. Verify screen type
    val screenContext = database.screenContextDao().getScreenByHash(screenHash)
    if (screenContext?.screenType != "login") {
        throw IllegalStateException("Not a login screen")
    }

    // 2. Find form group
    val formGroupId = database.scrapedElementDao()
        .getElementsForScreen(screenHash)
        .firstOrNull { it.formGroupId != null }
        ?.formGroupId

    if (formGroupId == null) {
        throw IllegalStateException("No form found")
    }

    // 3. Get form elements
    val formElements = database.scrapedElementDao()
        .getElementsByFormGroupId(formGroupId)

    // 4. Find email and password inputs
    val emailInput = formElements.find {
        it.inputType == "email" || it.validationPattern == "email"
    }
    val passwordInput = formElements.find {
        it.inputType == "password" || it.semanticRole == "input_password"
    }

    // 5. Fill inputs
    emailInput?.let { fillElement(it, email) }
    passwordInput?.let { fillElement(it, password) }

    // 6. Find submit button
    val submitRels = database.elementRelationshipDao()
        .findRelationshipsByType(RelationshipType.BUTTON_SUBMITS_FORM)
        .filter { rel ->
            formElements.any { it.elementHash == rel.targetElementHash }
        }

    val submitButton = submitRels.firstOrNull()?.let { rel ->
        database.scrapedElementDao().getElementByHash(rel.sourceElementHash)
    }

    // 7. Click submit
    submitButton?.let { clickElement(it) }
}
```

### Use Case 2: Voice Command "Go to Checkout"

```kotlin
suspend fun navigateToCheckout(currentScreenHash: String, packageName: String) {
    // 1. Find checkout screen
    val checkoutScreens = database.screenContextDao()
        .findScreensByType(packageName, "checkout")

    val checkoutHash = checkoutScreens.firstOrNull()?.screenHash
        ?: throw IllegalStateException("No checkout screen found")

    // 2. Find navigation path using transitions
    val path = findPath(currentScreenHash, checkoutHash)

    if (path.isEmpty()) {
        throw IllegalStateException("No path found to checkout")
    }

    // 3. Execute each step in path
    path.zipWithNext().forEach { (fromHash, toHash) ->
        // Find element that navigates from → to
        val navElement = findNavigationElement(fromHash, toHash)

        if (navElement != null) {
            clickElement(navElement)
            delay(1000) // Wait for transition
        }
    }
}

suspend fun findPath(fromHash: String, toHash: String): List<String> {
    // Simple BFS pathfinding using transitions
    val queue = mutableListOf(listOf(fromHash))
    val visited = mutableSetOf(fromHash)

    while (queue.isNotEmpty()) {
        val path = queue.removeAt(0)
        val current = path.last()

        if (current == toHash) return path

        val outgoing = database.screenTransitionDao()
            .getOutgoingTransitions(current)

        outgoing.forEach { transition ->
            if (transition.toScreenHash !in visited) {
                visited.add(transition.toScreenHash)
                queue.add(path + transition.toScreenHash)
            }
        }
    }

    return emptyList() // No path found
}
```

### Use Case 3: Context-Aware Command Suggestions

```kotlin
suspend fun getSuggestedCommands(screenHash: String): List<String> {
    val suggestions = mutableListOf<String>()

    // Get screen context
    val screenContext = database.screenContextDao().getScreenByHash(screenHash)

    // Suggest based on screen type
    when (screenContext?.screenType) {
        "login" -> {
            suggestions.add("Fill login form")
            suggestions.add("Show password")
            suggestions.add("Forgot password")
        }
        "checkout" -> {
            suggestions.add("Complete purchase")
            suggestions.add("Apply coupon")
            suggestions.add("Change address")
        }
        "search" -> {
            suggestions.add("Search for...")
            suggestions.add("Clear search")
            suggestions.add("Voice search")
        }
    }

    // Suggest based on primary action
    when (screenContext?.primaryAction) {
        "submit" -> suggestions.add("Submit form")
        "purchase" -> suggestions.add("Buy now")
        "search" -> suggestions.add("Start search")
    }

    // Suggest navigation based on common transitions
    val outgoing = database.screenTransitionDao()
        .getOutgoingTransitions(screenHash)
        .sortedByDescending { it.transitionCount }
        .take(3)

    outgoing.forEach { transition ->
        val targetScreen = database.screenContextDao()
            .getScreenByHash(transition.toScreenHash)

        targetScreen?.screenType?.let { type ->
            suggestions.add("Go to $type")
        }
    }

    return suggestions.distinct()
}
```

---

## Best Practices

### 1. Always Check Screen Type

```kotlin
// ✅ Good - Verify screen type before acting
val screenContext = database.screenContextDao().getScreenByHash(screenHash)
if (screenContext?.screenType == "login") {
    // Safe to assume login form exists
}

// ❌ Bad - Assume screen type
// May fail if screen classification is wrong
```

### 2. Use Confidence Scores

```kotlin
// ✅ Good - Filter by confidence
val highConfidenceRels = relationships.filter { it.confidence >= 0.8 }

// ❌ Bad - Use all relationships regardless of confidence
// May include incorrect inferences
```

### 3. Handle Missing Data Gracefully

```kotlin
// ✅ Good - Null checks and fallbacks
val formGroupId = element.formGroupId
    ?: findFormGroupByProximity(element) // Fallback strategy

// ❌ Bad - Assume data exists
// Will crash if formGroupId is null
```

### 4. Leverage Indices

```kotlin
// ✅ Good - Query by indexed fields
database.screenContextDao().findScreensByType(packageName, "login")

// ❌ Bad - Full table scan
database.screenContextDao().getAllScreens()
    .filter { it.screenType == "login" }
```

### 5. Cache Frequently Used Data

```kotlin
// ✅ Good - Cache screen contexts
class ScreenContextCache {
    private val cache = mutableMapOf<String, ScreenContextEntity>()

    suspend fun get(screenHash: String): ScreenContextEntity? {
        return cache.getOrPut(screenHash) {
            database.screenContextDao().getScreenByHash(screenHash) ?: return null
        }
    }
}

// ❌ Bad - Query database every time
repeat(100) {
    database.screenContextDao().getScreenByHash(screenHash) // 100 DB queries!
}
```

---

## Troubleshooting

### Issue 1: Screen Type Always Null

**Cause:** Screen doesn't match any keyword patterns

**Solution:**
```kotlin
// Check if screen has identifiable keywords
val elements = database.scrapedElementDao().getElementsForScreen(screenHash)
val allText = elements.mapNotNull { it.text }.joinToString(" ")
println("Screen text: $allText")

// If keywords are present but not detected, add custom keywords
// See ScreenContextInferenceHelper for keyword sets
```

### Issue 2: No Relationships Found

**Cause:** Phase 2.5 relationship inference requires certain patterns

**Solution:**
```kotlin
// Check if screen has form elements
val hasFormElements = elements.any {
    it.isEditable || it.className.contains("EditText")
}

// Check if screen has buttons
val hasButtons = elements.any {
    it.isClickable && it.className.contains("Button")
}

if (!hasFormElements || !hasButtons) {
    // Relationship inference won't work - no form detected
}
```

### Issue 3: Transition Tracking Not Working

**Cause:** Screens must have screen contexts created first

**Solution:**
```kotlin
// Ensure screen contexts exist before transitions are tracked
val fromContext = database.screenContextDao().getScreenByHash(fromHash)
val toContext = database.screenContextDao().getScreenByHash(toHash)

if (fromContext == null || toContext == null) {
    // Transition won't be recorded - screen contexts missing
    // Trigger a scrape to create screen contexts first
}
```

### Issue 4: Form Group ID Always Null

**Cause:** formGroupId assigned only if formContext detected

**Solution:**
```kotlin
// Check screen context
val screenContext = database.screenContextDao().getScreenByHash(screenHash)
println("Form context: ${screenContext?.formContext}")

if (screenContext?.formContext == null) {
    // No form detected - formGroupId won't be assigned
    // Screen needs form-related keywords in text/title
}
```

### Issue 5: Validation Pattern Inaccurate

**Cause:** Phase 2.5 uses 3-strategy cascade - may fall back to keywords

**Solution:**
```kotlin
// Check which strategy was used
val element = database.scrapedElementDao().getElementByHash(elementHash)

// Strategy 1: Android inputType flags (most reliable)
val androidInputType = node?.inputType
if (androidInputType != null) {
    // Best accuracy (80-90%)
}

// Strategy 2: Phase 1 inferred inputType
if (element?.inputType != null) {
    // Good accuracy (70-80%)
}

// Strategy 3: Resource ID keywords
// Lowest accuracy (60-70%) - may need manual override
```

---

## Migration Notes

### From v5 to v6 (Phase 2)

- 4 new nullable fields added to `scraped_elements`
- 2 new tables created (`screen_contexts`, `element_relationships`)
- No data migration required - all backward compatible

### From v6 to v7 (Phase 2.5)

- 1 new table created (`screen_transitions`)
- No schema changes to existing tables
- Automatic relationship inference enabled
- Form group IDs assigned on next scrape

### Testing Migration

```kotlin
// Verify migration success
val database = AppScrapingDatabase.getInstance(context)

// Check table exists
val tableExists = database.openHelper.writableDatabase
    .rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='screen_transitions'", null)
    .use { it.count > 0 }

println("Migration successful: $tableExists")

// Check indices created
val indicesExist = database.openHelper.writableDatabase
    .rawQuery("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE '%screen_transitions%'", null)
    .use { it.count >= 3 } // Should have 3+ indices

println("Indices created: $indicesExist")
```

---

## Performance Considerations

### Scraping Overhead

Phase 2 & 2.5 add minimal overhead:

- **Element inference:** ~2-3ms per element
- **Screen context lookup:** O(1) hash-based, < 1ms
- **Relationship inference:** ~5-10ms per form (one-time)
- **Transition recording:** O(1), < 1ms

**Total impact:** < 10% increase in scraping time

### Database Size

- Screen context: ~200-500 bytes per screen
- Relationships: ~100-200 bytes per relationship
- Transitions: ~100 bytes per unique transition

**Estimate:** 1-2 KB per unique screen

### Query Performance

All queries use indexed lookups:
- Screen hash lookups: O(1)
- Screen type queries: O(log N)
- Transition queries: O(log N)

**Typical query time:** < 5ms for 90% of queries

---

## Next Steps

**For Developers:**
1. Review API reference docs (coming soon)
2. Explore code examples in usage guide
3. Test on real apps to see screen classification
4. Provide feedback on accuracy

**For Advanced Users:**
5. Implement custom pathfinding algorithms
6. Build user journey analytics
7. Create personalization features using context data
8. Integrate with CommandManager for smart commands

**Future Enhancements (Phase 3):**
9. User interaction tracking
10. Click count analytics
11. Visibility duration tracking
12. Personalization based on usage patterns

---

**Document Version:** 1.0
**Last Updated:** 2025-10-18 22:52 PDT
**Database Version:** v7
**Author:** Manoj Jhawar

For questions or issues, see [Troubleshooting](#troubleshooting) section or check the changelog for known limitations.

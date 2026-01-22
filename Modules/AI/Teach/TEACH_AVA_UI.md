# Teach-Ava UI Documentation

## Overview

The Teach-Ava UI is a Jetpack Compose-based interface that allows users to train AVA's intent classification model by adding, editing, and managing training examples. This implements the IDEACODE Teach-Ava pattern for continuous learning.

## Architecture

### MVVM Pattern

```
TeachAvaScreen (View)
    ↓
TeachAvaViewModel (ViewModel)
    ↓
TrainExampleRepository (Domain)
    ↓
Room Database (Data)
```

### Component Structure

```
features/teach/
├── TeachAvaScreen.kt              # Main screen composable
├── TeachAvaViewModel.kt           # State management
├── TeachAvaContent.kt             # Content list with filtering
├── AddExampleDialog.kt            # Dialog for adding examples
├── TrainingExampleCard.kt         # Individual example display
└── TEACH_AVA_UI.md                # This documentation
```

## Components

### 1. TeachAvaScreen

Main screen container that orchestrates the UI.

**Features:**
- Material 3 Scaffold with TopAppBar
- Floating Action Button for adding examples
- Navigation back button
- Dialog management for add/edit

**Usage:**
```kotlin
TeachAvaScreen(
    viewModel = teachAvaViewModel,
    onNavigateBack = { navController.popBackStack() }
)
```

### 2. TeachAvaViewModel

Manages UI state and business logic.

**State Management:**
```kotlin
sealed class TeachAvaUiState {
    data object Loading : TeachAvaUiState()
    data object Empty : TeachAvaUiState()
    data class Success(
        val examples: List<TrainExample>,
        val intents: List<String>
    ) : TeachAvaUiState()
    data class Error(val message: String) : TeachAvaUiState()
}
```

**Key Functions:**
- `loadTrainingExamples()` - Load all examples from repository
- `addExample(example)` - Add new training example
- `deleteExample(id)` - Remove training example
- `setLocaleFilter(locale)` - Filter by language/locale
- `setIntentFilter(intent)` - Filter by intent type
- `clearError()` - Reset error state and retry

### 3. TeachAvaContent

Displays the list of training examples with filtering.

**Features:**
- Loading state with spinner
- Empty state with helpful message
- Success state with scrollable list
- Error state with retry button
- Intent filter bottom sheet

**UI States:**
- **Loading**: Shows CircularProgressIndicator
- **Empty**: Displays empty state message
- **Success**: Shows LazyColumn with TrainingExampleCards
- **Error**: Shows error message with retry button

### 4. AddExampleDialog

Material 3 dialog for adding new training examples.

**Features:**
- Utterance input field (what user says)
- Intent input field (lowercase with underscores)
- Locale selector (default: en-US)
- Hash-based deduplication (MD5)
- Input validation with error messages

**Validation Rules:**
- Utterance: Required, not blank
- Intent: Required, not blank, lowercase_with_underscores format recommended
- Locale: Optional, defaults to en-US

**Hash Generation:**
```kotlin
val hashInput = "$utterance:$intent"
val hash = MD5(hashInput)
```

### 5. TrainingExampleCard

Card component for displaying individual examples.

**Features:**
- Intent badge (primary container color)
- Utterance text (body large)
- Metadata row: locale, usage count
- Source and creation date
- Edit and delete action buttons
- Delete confirmation dialog

**Layout:**
```
┌─────────────────────────────────────┐
│ [Intent Badge]          [Edit][Del] │
│                                      │
│ "Turn on the lights"                 │
│                                      │
│ Locale: en-US        Used: 5x        │
│ Source: user taught  Jan 15, 2025    │
└─────────────────────────────────────┘
```

## User Flows

### Add Training Example

1. User taps FAB (+) button
2. AddExampleDialog appears
3. User enters utterance, intent, locale
4. User taps "Add Example"
5. ViewModel validates and calls repository
6. Repository generates hash and checks for duplicates
7. If unique, example is stored
8. UI updates automatically via Flow
9. Dialog closes

### Delete Training Example

1. User taps delete icon on card
2. Confirmation dialog appears
3. User confirms deletion
4. ViewModel calls repository
5. Example is removed from database
6. UI updates automatically via Flow

### Filter by Intent

1. User taps "All intents" button
2. Bottom sheet appears with intent list
3. User selects specific intent or "All"
4. ViewModel filters examples
5. UI updates to show filtered results

## VOS4 Patterns

### Hash-Based Deduplication

Following VOS4 pattern for preventing duplicates:

```kotlin
// Generate MD5 hash from utterance + intent
val hashInput = "$utterance:$intent"
val hash = MD5(hashInput)

// Room entity has UNIQUE constraint on exampleHash
@Entity(indices = [Index(value = ["exampleHash"], unique = true)])
```

### Usage Tracking

Tracks how often each example matches user input:

```kotlin
data class TrainExample(
    // ...
    val usageCount: Int = 0,
    val lastUsed: Long? = null
)
```

Updated by `ClassifyIntentUseCase` when example matches.

### Composite Indices

Efficient queries for filtering:

```kotlin
@Entity(
    indices = [
        Index(value = ["intent"]),          // Filter by intent
        Index(value = ["locale"]),          // Filter by locale
        Index(value = ["intent", "locale"]) // Combined filter
    ]
)
```

## Material 3 Design

### Color Scheme

- **Primary Container**: TopAppBar background
- **Primary**: FAB and accent elements
- **Surface Variant**: Card backgrounds
- **Error**: Delete button and error states

### Typography

- **Headline Small**: Screen title, section headers
- **Title Large**: Empty state, error titles
- **Title Medium**: List headers (count)
- **Body Large**: Utterance text
- **Body Medium**: Descriptions
- **Body Small**: Metadata
- **Label Medium**: Intent badges

### Spacing

- **16.dp**: Screen padding, card horizontal padding
- **24.dp**: Dialog padding
- **8.dp**: Card vertical padding, small gaps
- **12.dp**: Medium gaps within cards
- **32.dp**: Large gaps, empty state padding

## Performance Considerations

### Lazy Loading

`LazyColumn` with `items()` for efficient scrolling:
```kotlin
LazyColumn {
    items(
        items = examples,
        key = { it.id }  // Stable keys for recomposition
    ) { example ->
        TrainingExampleCard(example)
    }
}
```

### Flow Collection

Reactive UI updates via Kotlin Flows:
```kotlin
trainExampleRepository.getAllExamples()
    .collect { examples ->
        _uiState.value = TeachAvaUiState.Success(examples)
    }
```

### State Hoisting

State is hoisted to ViewModel, composables are stateless:
```kotlin
@Composable
fun TeachAvaScreen(
    viewModel: TeachAvaViewModel,  // State source
    onNavigateBack: () -> Unit     // Event handler
)
```

## Testing Strategy

### UI Tests (Compose)

```kotlin
@Test
fun addExample_showsDialog() {
    composeTestRule.setContent {
        TeachAvaScreen(viewModel, onNavigateBack = {})
    }

    composeTestRule.onNodeWithContentDescription("Add training example")
        .performClick()

    composeTestRule.onNodeWithText("Add Training Example")
        .assertIsDisplayed()
}
```

### ViewModel Tests

```kotlin
@Test
fun loadTrainingExamples_updatesUiState() = runTest {
    // Given
    val examples = listOf(createTestExample())
    every { repository.getAllExamples() } returns flowOf(examples)

    // When
    viewModel.loadTrainingExamples()

    // Then
    assertTrue(viewModel.uiState.value is TeachAvaUiState.Success)
}
```

## Future Enhancements

### Phase 1 (Week 6)
- [ ] Implement edit example functionality
- [ ] Add bulk import from CSV/JSON
- [ ] Show confidence scores for each example

### Phase 2 (Week 7-8)
- [ ] Add example suggestions from corrections
- [ ] Implement search/filter for utterances
- [ ] Show usage statistics and trends

### Phase 3 (Week 9+)
- [ ] Multi-select and batch operations
- [ ] Export training data
- [ ] Intent grouping and organization
- [ ] Similarity detection (find near-duplicates)

## Integration Points

### With NLU Module

When user utterance doesn't match (low confidence):
```kotlin
// ClassifyIntentUseCase detects low confidence
if (classification.confidence < threshold) {
    return ClassificationResult(
        needsTraining = true,
        suggestedUtterance = utterance
    )
}

// UI navigates to Teach-Ava with pre-filled utterance
navController.navigate("teach?utterance=$utterance")
```

### With Chat Module

After user corrects AVA's decision:
```kotlin
// User provides correction via chat
val correction = Learning(
    decisionId = decision.id,
    feedbackType = FeedbackType.CORRECTION,
    userCorrection = mapOf("intent" to "corrected_intent")
)

// System suggests adding to training data
showTeachAvaSuggestion(
    utterance = decision.inputData["utterance"],
    intent = correction.userCorrection["intent"]
)
```

## Accessibility

### Content Descriptions

All interactive elements have content descriptions:
- FAB: "Add training example"
- Delete: "Delete example"
- Edit: "Edit example"
- Back: "Navigate back"

### Semantic Structure

Proper use of Material 3 components ensures screen reader support:
- TopAppBar for navigation context
- Card for grouping related content
- Dialog for modal interactions

### Color Contrast

Material 3 color roles ensure WCAG AA compliance:
- `onSurface` on `surface`
- `onPrimaryContainer` on `primaryContainer`
- `error` on `surface`

## Troubleshooting

### Common Issues

**Issue**: Examples not appearing after add
- **Cause**: Flow not collecting in ViewModel
- **Fix**: Ensure `loadTrainingExamples()` called in `init {}`

**Issue**: Duplicate example error not shown
- **Cause**: Error state not displayed
- **Fix**: Check `Result.Error` handling in ViewModel

**Issue**: UI not updating after delete
- **Cause**: Database transaction not completing
- **Fix**: Verify cascade delete constraints in Room

## References

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3 Components](https://m3.material.io/components)
- [MVVM Architecture](https://developer.android.com/topic/architecture)
- [AVA Architecture](../../ARCHITECTURE.md)
- [VOS4 Patterns](../../external/vos4/PATTERNS.md)

---

**Last Updated**: 2025-01-XX
**Component Count**: 5 Compose files
**Architecture**: MVVM + Clean Architecture

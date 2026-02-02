# WebAvanue StateFlow Utilities - Quick Reference

**Module:** `com.augmentalis.webavanue.util`
**Last Updated:** 2026-02-02

---

## Quick Start

```kotlin
import com.augmentalis.webavanue.util.*

class MyViewModel : BaseStatefulViewModel() {
    // Simple state
    private val _counter = ViewModelState(0)
    val counter: StateFlow<Int> = _counter.flow

    // Nullable state (dialogs, errors)
    private val _dialog = NullableState<DialogData>()
    val dialog: StateFlow<DialogData?> = _dialog.flow

    // List state
    private val _items = ListState<Item>()
    val items: StateFlow<List<Item>> = _items.flow

    // Loading/error/success already available via uiState
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow

    fun increment() {
        _counter.value++
    }

    fun showDialog(data: DialogData) {
        _dialog.value = data
    }

    fun dismissDialog() {
        _dialog.clear()
    }

    fun addItem(item: Item) {
        _items.add(item)
    }

    fun updateItem(id: String, newName: String) {
        _items.updateItem({ it.id == id }) { it.copy(name = newName) }
    }

    fun deleteItem(id: String) {
        _items.removeItem { it.id == id }
    }

    fun saveItem(item: Item) {
        execute {  // Automatically manages loading/error/success
            repository.save(item)
        }
    }
}
```

---

## Cheat Sheet

### ViewModelState<T>

```kotlin
val state = ViewModelState(initialValue)

state.value                    // Get value
state.value = newValue         // Set value
state.update { it + 1 }        // Transform value
state.flow                     // Get StateFlow for UI
```

### NullableState<T>

```kotlin
val state = NullableState<MyType>()

state.value                    // Get nullable value
state.value = something        // Set value
state.clear()                  // Set to null
state.hasValue()               // Check if not null
state.ifPresent { doWith(it) } // Execute if not null
```

### ListState<T>

```kotlin
val list = ListState<Item>()

// Add
list.add(item)                 // Add to end
list.addFirst(item)            // Add to start
list.addAll(items)             // Add multiple

// Update
list.updateItem(
    { it.id == id },           // Find predicate
    { it.copy(name = new) }    // Transform
)
list.updateAll({ predicate }) { transform }

// Remove
list.removeItem { it.id == id }
list.removeAll { it.isComplete }

// Query
list.find { it.id == id }
list.contains { it.id == id }
list.indexOf { it.id == id }
list.size
list.isEmpty

// Bulk
list.replaceAll(newList)
list.clear()
```

### UiState

```kotlin
val uiState = UiState()

// Access flows
uiState.isLoading.flow
uiState.error.flow
uiState.saveSuccess.flow

// Execute with auto state management
uiState.execute(scope) {
    repository.save(data)      // Returns Result<T>
}

// Manual control
uiState.setError("message")
uiState.clearError()
uiState.clearSuccess()
uiState.reset()
```

### BaseViewModel

```kotlin
class MyViewModel : BaseViewModel() {
    // Inherited: viewModelScope, onCleared()

    fun doWork() {
        launch { /* Main dispatcher */ }
        launchIO { /* IO dispatcher */ }
    }

    fun observeData() {
        observe(repository.dataFlow) { data ->
            // Handle data
        }
    }
}
```

### BaseStatefulViewModel

```kotlin
class MyViewModel : BaseStatefulViewModel() {
    // Inherited: viewModelScope, onCleared(), uiState

    fun save(data: Data) {
        execute { repository.save(data) }
    }

    fun saveWithCustomError(data: Data) {
        executeWithMessage("Failed to save") {
            repository.save(data)
        }
    }
}
```

---

## Migration Patterns

### Before → After

```kotlin
// State declaration
// BEFORE
private val _state = MutableStateFlow(value)
val state: StateFlow<Type> = _state.asStateFlow()

// AFTER
private val _state = ViewModelState(value)
val state: StateFlow<Type> = _state.flow

// List update
// BEFORE
val list = _items.value.toMutableList()
list.add(item)
_items.value = list

// AFTER
_items.add(item)

// Nullable state clear
// BEFORE
_dialog.value = null

// AFTER
_dialog.clear()

// CoroutineScope
// BEFORE
private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
fun onCleared() { viewModelScope.cancel() }

// AFTER
class MyViewModel : BaseViewModel()  // Inherited
```

---

## Files

| File | Location |
|------|----------|
| ViewModelState | `util/ViewModelState.kt` |
| NullableState | `util/ViewModelState.kt` |
| ListState | `util/ListState.kt` |
| UiState | `util/UiState.kt` |
| BaseViewModel | `util/BaseViewModel.kt` |
| BaseStatefulViewModel | `util/BaseViewModel.kt` |
| SearchState | `util/SearchState.kt` |

---

**Full Documentation:** [Developer Manual Chapter 75](/Docs/AVA/ideacode/guides/Developer-Manual-Chapter75-StateFlow-Utilities.md)

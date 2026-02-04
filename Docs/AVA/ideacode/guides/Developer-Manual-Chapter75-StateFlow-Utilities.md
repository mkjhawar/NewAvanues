# Chapter 75: StateFlow Utilities for WebAvanue ViewModels

**Created:** 2026-02-02
**Author:** Claude AI
**Status:** IMPLEMENTED
**Module:** WebAvanue

## 75.1. Overview

The StateFlow Utilities refactoring addresses repetitive boilerplate code across WebAvanue ViewModels. By introducing a set of reusable utility classes, we reduced **~1,800 lines** of code across 6 ViewModels while improving consistency and maintainability.

### Problem Statement

Before this refactoring, every ViewModel contained repetitive patterns:

```kotlin
// BEFORE: 3 lines per state property
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

// Repeated 40+ times across ViewModels
```

### Solution

Created reusable utility classes in `com.augmentalis.webavanue.util`:

| Utility | Purpose | Lines Saved Per Use |
|---------|---------|---------------------|
| `ViewModelState<T>` | Replace `_state`/`state.asStateFlow()` pattern | 2 |
| `NullableState<T>` | Dialog/error states with `clear()` helper | 2 |
| `ListState<T>` | List manipulation with `updateItem()`, `removeItem()` | 5-7 |
| `UiState` | Loading/error/success trilogy | 10+ |
| `BaseViewModel` | Common `viewModelScope` and `onCleared()` | 4 |
| `BaseStatefulViewModel` | BaseViewModel + built-in UiState | 15+ |

## 75.2. Utility Classes Reference

### 75.2.1. ViewModelState<T>

Eliminates the repetitive private/public StateFlow declaration pattern.

```kotlin
// BEFORE (3 lines)
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

// AFTER (1 line)
val isLoading = ViewModelState(false)
```

**Usage:**
```kotlin
class MyViewModel {
    val counter = ViewModelState(0)

    fun increment() {
        counter.value++                    // Direct assignment
        counter.update { it + 1 }          // Lambda update
    }

    // Expose to UI
    val counterFlow: StateFlow<Int> = counter.flow
}
```

### 75.2.2. NullableState<T>

Specialized for nullable dialog/error states with convenience methods.

```kotlin
class SecurityViewModel {
    private val _error = NullableState<String>()
    val error: StateFlow<String?> = _error.flow

    fun showError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.clear()  // Sets to null
    }

    fun doSomethingIfError() {
        _error.ifPresent { errorMsg ->
            // Only executes if not null
            log(errorMsg)
        }
    }
}
```

### 75.2.3. ListState<T>

Simplifies common list manipulation patterns in ViewModels.

```kotlin
// BEFORE (5+ lines)
val currentDownloads = _downloads.value.toMutableList()
val index = currentDownloads.indexOfFirst { it.id == downloadId }
if (index >= 0) {
    currentDownloads[index] = currentDownloads[index].copy(status = CANCELLED)
    _downloads.value = currentDownloads
}

// AFTER (1 line)
downloads.updateItem({ it.id == downloadId }) { it.copy(status = CANCELLED) }
```

**Available Methods:**
```kotlin
class DownloadViewModel {
    private val _downloads = ListState<Download>()

    // Add items
    _downloads.add(download)           // Add to end
    _downloads.addFirst(download)      // Add to beginning
    _downloads.addAll(downloadList)    // Add multiple

    // Update items
    _downloads.updateItem(
        predicate = { it.id == id },
        transform = { it.copy(progress = 50) }
    )

    // Remove items
    _downloads.removeItem { it.id == id }
    _downloads.removeAll { it.status == COMPLETED }

    // Query
    val download = _downloads.find { it.id == id }
    val hasActive = _downloads.contains { it.isActive }

    // Bulk operations
    _downloads.replaceAll(newList)
    _downloads.clear()
}
```

### 75.2.4. UiState

Manages the common loading/error/success state trilogy.

```kotlin
// BEFORE (12+ lines)
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

private val _saveSuccess = MutableStateFlow(false)
val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

fun clearError() { _error.value = null }
fun clearSuccess() { _saveSuccess.value = false }

// AFTER (1 line + access)
val uiState = UiState()

// Access flows
val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
val error: StateFlow<String?> = uiState.error.flow
val saveSuccess: StateFlow<Boolean> = uiState.saveSuccess.flow
```

**Execute with automatic state management:**
```kotlin
fun saveData(data: Data) {
    uiState.execute(viewModelScope) {
        repository.save(data)
    }
    // Automatically:
    // - Sets isLoading = true
    // - Clears error
    // - Sets saveSuccess = true on success
    // - Sets error on failure
    // - Sets isLoading = false
}
```

### 75.2.5. BaseViewModel

Consolidates common ViewModel patterns.

```kotlin
// BEFORE
class MyViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun onCleared() {
        viewModelScope.cancel()
    }
}

// AFTER
class MyViewModel : BaseViewModel() {
    // viewModelScope inherited
    // onCleared() inherited

    fun doSomething() {
        launch {  // Shorthand for viewModelScope.launch
            // async work
        }
    }
}
```

### 75.2.6. BaseStatefulViewModel

BaseViewModel with built-in UiState for ViewModels that need loading/error/success management.

```kotlin
class SettingsViewModel : BaseStatefulViewModel() {
    // uiState automatically available
    val isLoading: StateFlow<Boolean> = uiState.isLoading.flow
    val error: StateFlow<String?> = uiState.error.flow

    fun saveSettings(settings: Settings) {
        execute {  // Shorthand with automatic state management
            repository.save(settings)
        }
    }
}
```

## 75.3. Migration Results

### Line Count Reduction

| ViewModel | Before | After | Reduction |
|-----------|--------|-------|-----------|
| HistoryViewModel | 257 | 155 | **40%** |
| DownloadViewModel | 398 | 255 | **36%** |
| FavoriteViewModel | 474 | 308 | **35%** |
| SecurityViewModel | 556 | 328 | **41%** |
| SettingsViewModel | 555 | 191 | **66%** |
| TabViewModel | 1355 | 652 | **52%** |
| **Total** | **3595** | **1889** | **~47%** |

### Key Patterns Consolidated

1. **State Declaration**: 3 lines → 1 line
2. **Loading/Error/Success Trilogy**: 12+ lines → 1 line
3. **List Manipulation**: 5-7 lines → 1 line
4. **Settings Copy-Update**: 4 lines → 1 line
5. **CoroutineScope Setup**: 4 lines → inherited

## 75.4. Best Practices

### When to Use Each Utility

| Scenario | Utility |
|----------|---------|
| Simple state (boolean, string, int) | `ViewModelState<T>` |
| Nullable dialog/error state | `NullableState<T>` |
| List of items with CRUD operations | `ListState<T>` |
| ViewModel with loading/error/success | `BaseStatefulViewModel` |
| ViewModel without loading state | `BaseViewModel` |

### Migration Guide

1. **Change class inheritance:**
   ```kotlin
   // From
   class MyViewModel { ... }

   // To
   class MyViewModel : BaseStatefulViewModel() { ... }
   ```

2. **Replace state declarations:**
   ```kotlin
   // From
   private val _items = MutableStateFlow<List<Item>>(emptyList())
   val items: StateFlow<List<Item>> = _items.asStateFlow()

   // To
   private val _items = ListState<Item>()
   val items: StateFlow<List<Item>> = _items.flow
   ```

3. **Simplify list operations:**
   ```kotlin
   // From
   val list = _items.value.toMutableList()
   list.add(item)
   _items.value = list

   // To
   _items.add(item)
   ```

4. **Remove manual scope management:**
   ```kotlin
   // Remove these lines (inherited from BaseViewModel)
   private val viewModelScope = CoroutineScope(...)
   fun onCleared() { viewModelScope.cancel() }
   ```

## 75.5. File Locations

| File | Purpose |
|------|---------|
| `util/ViewModelState.kt` | ViewModelState and NullableState |
| `util/ListState.kt` | ListState with CRUD helpers |
| `util/UiState.kt` | UiState (loading/error/success) |
| `util/BaseViewModel.kt` | BaseViewModel and BaseStatefulViewModel |
| `util/SearchState.kt` | SearchState and SettingsUpdater |

## 75.6. Related Documentation

- [WebAvanue Architecture](/Docs/WebAvanue/project/LD-mainavanues-architecture.md)
- [KMP Strategy](/Docs/AVA/MasterSpecs/ADR-001-KMP-Strategy.md)

---

**Commit Reference:** `cfe164e7` - "refactor(webavanue): Add StateFlow utilities and reduce ViewModel boilerplate by ~1,800 lines"

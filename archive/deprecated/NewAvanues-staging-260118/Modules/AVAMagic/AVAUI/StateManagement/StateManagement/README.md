# AvaElements State Management

A comprehensive, reactive state management system for AvaElements built on Kotlin Flow. Provides platform-agnostic state management that works seamlessly across Android, iOS, Desktop, and Web.

## Overview

The StateManagement module provides:

- **Reactive State**: Flow-based state that automatically updates UI
- **State Container**: Scoped state management similar to Compose remember
- **Data Binding**: Two-way data binding for forms and inputs
- **ViewModels**: Lifecycle-aware state containers with coroutine support
- **State Persistence**: Save and restore state across app restarts
- **Form Management**: Built-in validation and error handling
- **Reactive Components**: Components that rebuild on state changes

## Core Concepts

### MagicState

The foundation of reactive state management. All state in AvaElements is based on `MagicState`:

```kotlin
// Create mutable state
val counter = mutableStateOf(0)

// Read current value
val current = counter.current()

// Update value
counter.setValue(42)

// Update with transform
counter.update { it + 1 }

// Observe as Flow
counter.value.collect { value ->
    println("New value: $value")
}
```

### StateContainer

Manage component-scoped state with keys:

```kotlin
val container = StateContainer()

// Remember state by key
val username = container.remember("username", "")
val age = container.remember("age", 0)

// Get existing state
val existingState = container.get<String>("username")

// Take snapshot
val snapshot = container.snapshot()

// Restore from snapshot
container.restore(snapshot)
```

### StateBuilder DSL

Declarative state creation:

```kotlin
val builder = stateBuilder {
    val count = state(0)
    val doubled = derivedStateFrom(count) { it * 2 }
    val text = state("key", "Hello")
}
```

## Two-Way Data Binding

### Basic Data Binding

```kotlin
val nameBinding = dataBindingOf("") { newName ->
    viewModel.updateUserName(newName)
}

TextField(value = nameBinding.value.value) {
    onValueChange = { nameBinding.update(it) }
}
```

### Collection Binding

```kotlin
val items = collectionBindingOf<String>()

// Add items
items.add("Item 1")
items.addAll(listOf("Item 2", "Item 3"))

// Remove items
items.remove("Item 1")
items.removeAt(0)

// Update items
items.updateAt(0, "Updated Item")

// Observe changes
items.items.collect { list ->
    println("Items: $list")
}
```

### Property Binding

```kotlin
data class User(val name: String, val age: Int)

val userState = mutableStateOf(User("John", 30))

// Bind to specific property
val nameBinding = userState.bindProperty(
    getter = { it.name },
    setter = { user, newName -> user.copy(name = newName) }
)

nameBinding.set("Jane")
```

## ViewModels

### Basic ViewModel

```kotlin
class UserViewModel : MagicViewModel() {
    private val _name = mutableState("")
    val name: StateFlow<String> = _name.asState()

    private val _email = mutableState("")
    val email: StateFlow<String> = _email.asState()

    fun updateName(newName: String) {
        _name.value = newName
    }

    fun saveUser() {
        viewModelScope.launch {
            // Async operation
            delay(1000)
            println("User saved!")
        }
    }

    override fun onCleared() {
        println("ViewModel cleared")
    }
}
```

### AsyncViewModel

Built-in loading and error state management:

```kotlin
class DataViewModel : AsyncViewModel() {
    private val _data = mutableState<List<String>>(emptyList())
    val data: StateFlow<List<String>> = _data.asState()

    fun loadData() {
        launchAsync {
            // isLoading automatically set to true
            delay(1000)
            _data.value = listOf("Item 1", "Item 2", "Item 3")
            // isLoading automatically set to false
        }
    }
}

// Usage
val viewModel = DataViewModel()
viewModel.loadData()

viewModel.isLoading.collect { loading ->
    println("Loading: $loading")
}

viewModel.error.collect { error ->
    error?.let { println("Error: $it") }
}
```

### StatefulViewModel

Manage complex UI state:

```kotlin
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : StatefulViewModel<LoginState>(LoginState()) {
    fun updateEmail(email: String) {
        updateState { copy(email = email) }
    }

    fun updatePassword(password: String) {
        updateState { copy(password = password) }
    }

    fun login() {
        launchAsync {
            updateState { copy(isLoading = true, error = null) }

            try {
                // Login logic
                delay(1000)
                updateState { copy(isLoading = false) }
            } catch (e: Exception) {
                updateState {
                    copy(isLoading = false, error = e.message)
                }
            }
        }
    }
}
```

## Form State Management

### Creating Forms

```kotlin
val form = buildForm {
    field("email", "") {
        required(true)
        email("Please enter a valid email")
    }

    field("password", "") {
        required(true)
        minLength(8)
    }

    field("confirmPassword", "") {
        required(true)
        // Match with password field
        matches(form.getField("password")!!)
    }

    field("age", 0) {
        range(min = 18, max = 120)
    }
}
```

### Using Form Fields

```kotlin
val emailField = form.getField<String>("email")!!

TextField(value = emailField.value.value) {
    isError = emailField.error.value != null
    errorMessage = emailField.error.value
    onValueChange = {
        emailField.setValue(it)
        emailField.validate()
    }
}

Button("Submit") {
    onClick = {
        if (form.validate()) {
            val values = form.getValues()
            // Submit form
        }
    }
}
```

### Custom Validators

```kotlin
class PhoneValidator : Validator<String> {
    override fun validate(value: String): ValidationResult {
        return if (value.matches(Regex("^\\d{10}$"))) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid("Invalid phone number")
        }
    }
}

// Usage
field("phone", "") {
    validator(PhoneValidator())
}

// Or inline
field("username", "") {
    custom("Username already taken") { username ->
        // Check if username is available
        !existingUsernames.contains(username)
    }
}
```

## State Persistence

### Basic Persistence

```kotlin
val stateManager = StateManager(InMemoryStatePersistence())

// Save state
val settings = mutableStateOf(Settings())
stateManager.saveState("settings", settings)

// Restore state
val restoredSettings = stateManager.restoreState("settings", Settings())
```

### Auto-Saving State

```kotlin
val preferences = mutableStateOf(UserPreferences())
stateManager.autoSave("preferences", preferences, debounceMillis = 500)

// Changes are automatically saved after 500ms of inactivity
```

### Persistent State

```kotlin
// State that automatically persists
val darkMode = persistentStateOf(
    key = "dark_mode",
    initialValue = false,
    manager = stateManager
)

// Changes automatically save
darkMode.setValue(true)
```

### Custom Persistence

Implement platform-specific persistence:

```kotlin
class AndroidStatePersistence(
    private val dataStore: DataStore<Preferences>
) : StatePersistence {
    override suspend fun save(key: String, state: Any) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = state.toString()
        }
    }

    override suspend fun <T> restore(key: String): T? {
        val preferences = dataStore.data.first()
        return preferences[stringPreferencesKey(key)] as? T
    }

    // Implement other methods...
}
```

## Reactive Components

### Basic Reactive Component

```kotlin
val counter = mutableStateOf(0)

reactive(counter) { count ->
    Text("Count: $count")
}
```

### Conditional Rendering

```kotlin
val isLoggedIn = mutableStateOf(false)

conditional(
    condition = isLoggedIn.value,
    content = Text("Welcome back!"),
    elseContent = Button("Login") { onClick = { /* ... */ } }
)
```

### Reactive Lists

```kotlin
val items = collectionBindingOf<String>()

reactiveList(items.items) { item, index ->
    Row {
        Text(item)
        Button("Delete") {
            onClick = { items.removeAt(index) }
        }
    }
}
```

### Switch Component

```kotlin
enum class ViewMode { LIST, GRID, TABLE }
val viewMode = mutableStateOf(ViewMode.LIST)

switch(
    state = viewMode.value,
    cases = mapOf(
        ViewMode.LIST to ListView(),
        ViewMode.GRID to GridView(),
        ViewMode.TABLE to TableView()
    )
)
```

## Platform Integration

### Android (Jetpack Compose)

```kotlin
@Composable
fun CounterScreen() {
    val counter = remember { mutableStateOf(0) }

    Column {
        Text("Count: ${counter.value.collectAsState().value}")
        Button(onClick = { counter.update { it + 1 } }) {
            Text("Increment")
        }
    }
}
```

### iOS (SwiftUI)

```swift
struct CounterView: View {
    @StateObject var counter = MutableMagicState(0)

    var body: some View {
        VStack {
            Text("Count: \\(counter.current())")
            Button("Increment") {
                counter.update { $0 + 1 }
            }
        }
    }
}
```

### Web (Kotlin/JS)

```kotlin
fun renderCounter() {
    val counter = mutableStateOf(0)

    div {
        h1 { +"Count: ${counter.current()}" }
        button {
            +"Increment"
            onClick = { counter.update { it + 1 } }
        }
    }
}
```

## Advanced Patterns

### Derived State

```kotlin
val firstName = mutableStateOf("John")
val lastName = mutableStateOf("Doe")

val fullName = derivedStateFrom(firstName, lastName) { first, last ->
    "$first $last"
}

fullName.collect { name ->
    println("Full name: $name")
}
```

### State Composition

```kotlin
class AppState {
    val user = mutableStateOf<User?>(null)
    val cart = collectionBindingOf<CartItem>()
    val preferences = mutableStateOf(Preferences())

    val isLoggedIn = derivedStateFrom(user) { it != null }
    val cartTotal = derivedStateFrom(cart.items) { items ->
        items.sumOf { it.price * it.quantity }
    }
}
```

### Event Handling

```kotlin
class EventViewModel : MagicViewModel() {
    private val _events = ViewModelEventChannel<AppEvent>()
    val events: SharedFlow<AppEvent> = _events.events

    fun sendEvent(event: AppEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

sealed class AppEvent {
    data class ShowMessage(val message: String) : AppEvent()
    object NavigateBack : AppEvent()
}
```

## Best Practices

### 1. State Hoisting

Keep state at the highest necessary level:

```kotlin
// Good
class ParentViewModel : MagicViewModel() {
    val sharedState = mutableState("")
}

// Avoid
class ChildComponent {
    val localState = mutableState("") // Should be hoisted
}
```

### 2. Immutable Data

Use immutable data structures for state:

```kotlin
data class User(
    val name: String,
    val email: String
) // Immutable

val user = mutableStateOf(User("John", "john@example.com"))
user.update { it.copy(email = "newemail@example.com") }
```

### 3. Derived State

Compute derived values instead of storing them:

```kotlin
// Good
val total = derivedStateFrom(items) {
    it.sumOf { item -> item.price }
}

// Avoid
val total = mutableStateOf(0.0)
// Manual updates everywhere
```

### 4. ViewModel Scoping

Use proper lifecycle scoping for ViewModels:

```kotlin
val viewModel = ViewModelStore.getOrCreate("main") {
    MainViewModel()
}

// Clear when done
viewModel.clear()
```

### 5. Persistence Keys

Use consistent, descriptive keys for persistence:

```kotlin
const val KEY_USER_SETTINGS = "user_settings"
const val KEY_CART_ITEMS = "cart_items"

val settings = persistentStateOf(
    key = KEY_USER_SETTINGS,
    initialValue = Settings(),
    manager = stateManager
)
```

## Testing

### Testing State

```kotlin
@Test
fun testStateUpdate() = runTest {
    val state = mutableStateOf(0)

    state.setValue(42)
    assertEquals(42, state.current())

    state.update { it + 1 }
    assertEquals(43, state.current())
}
```

### Testing ViewModels

```kotlin
@Test
fun testViewModelLoading() = runTest {
    val viewModel = DataViewModel()

    viewModel.loadData()

    assertTrue(viewModel.isLoading.value)

    advanceUntilIdle()

    assertFalse(viewModel.isLoading.value)
    assertTrue(viewModel.data.value.isNotEmpty())
}
```

### Testing Forms

```kotlin
@Test
fun testFormValidation() {
    val form = buildForm {
        field("email", "") {
            required(true)
            email()
        }
    }

    val emailField = form.getField<String>("email")!!

    emailField.setValue("invalid")
    assertFalse(form.validate())

    emailField.setValue("valid@example.com")
    assertTrue(form.validate())
}
```

## Performance Tips

### 1. Use StateFlow over SharedFlow

StateFlow is optimized for state management:

```kotlin
// Preferred
val state: StateFlow<Int> = MutableStateFlow(0)

// Avoid for state
val state: SharedFlow<Int> = MutableSharedFlow()
```

### 2. Debounce Updates

Reduce update frequency for expensive operations:

```kotlin
val searchQuery = mutableStateOf("")
val debouncedQuery = searchQuery.value.debounce(300)

debouncedQuery.collect { query ->
    performExpensiveSearch(query)
}
```

### 3. Use Derived State

Avoid redundant state:

```kotlin
// Good
val filtered = derivedStateFrom(items, searchQuery) { items, query ->
    items.filter { it.contains(query) }
}

// Avoid
val filtered = mutableStateOf(emptyList())
// Manual sync needed
```

## Architecture Integration

### MVVM Architecture

```kotlin
// Model
data class User(val id: String, val name: String)

// ViewModel
class UserViewModel : AsyncViewModel() {
    private val _user = mutableState<User?>(null)
    val user: StateFlow<User?> = _user.asState()

    fun loadUser(id: String) {
        launchAsync {
            val user = userRepository.getUser(id)
            _user.value = user
        }
    }
}

// View
AvaUI {
    val viewModel = UserViewModel()

    reactive(viewModel.user) { user ->
        user?.let {
            Text("Welcome, ${it.name}")
        }
    }
}
```

## Migration Guide

### From React useState

```javascript
// React
const [count, setCount] = useState(0);

// AvaElements
val count = mutableStateOf(0)
count.setValue(newValue)
```

### From SwiftUI @State

```swift
// SwiftUI
@State private var count = 0

// AvaElements
val count = mutableStateOf(0)
```

### From Jetpack Compose

```kotlin
// Compose
val count = remember { mutableStateOf(0) }

// AvaElements (very similar!)
val count = mutableStateOf(0)
```

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## License

Copyright © 2024 Augmentalis. All rights reserved.

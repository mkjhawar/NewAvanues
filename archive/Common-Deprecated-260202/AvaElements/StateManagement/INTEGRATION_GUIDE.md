# StateManagement Integration Guide

This guide explains how the StateManagement module integrates with AvaElements renderers and provides seamless reactive UI updates across all platforms.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AvaElements UI Layer                    │
│  (Components: TextField, Button, Text, etc.)                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Observes State
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  StateManagement Module                      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  MagicState  │  │ StateBuilder │  │ DataBinding  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ ViewModel    │  │  FormState   │  │ Persistence  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ Triggers Recomposition
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Platform Renderers                         │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Android    │  │     iOS      │  │     Web      │     │
│  │  (Compose)   │  │  (SwiftUI)   │  │  (React/DOM) │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## State Flow and Reactivity

### 1. State Updates Trigger UI Updates

When state changes, the renderer automatically rebuilds affected components:

```kotlin
// User updates state
counter.setValue(5)
    ↓
// StateFlow emits new value
counter.value.emit(5)
    ↓
// Component observes change
TextField observes counter.value
    ↓
// Renderer rebuilds component
AndroidRenderer.render(textField)
    ↓
// Platform UI updates
Composable TextField recomposes
```

## Platform-Specific Integration

### Android (Jetpack Compose)

The Android renderer integrates state management with Compose's state system:

```kotlin
class AndroidRenderer : Renderer {
    override fun render(component: Component): Any {
        return when (component) {
            is TextFieldComponent -> {
                @Composable
                fun TextFieldImpl() {
                    // Convert MagicState to Compose State
                    val textState = component.value.collectAsState()

                    TextField(
                        value = textState.value,
                        onValueChange = { newValue ->
                            component.onValueChange?.invoke(newValue)
                        }
                    )
                }
                TextFieldImpl()
            }
            // Other components...
        }
    }
}
```

**How it works:**
1. `collectAsState()` converts `StateFlow` to Compose `State`
2. Compose automatically subscribes to state changes
3. When state updates, Compose recomposes the TextField
4. User input triggers `onValueChange`, updating MagicState

### iOS (SwiftUI)

The iOS renderer uses `@StateObject` and `ObservableObject`:

```swift
class AndroidRendererStateAdapter<T>: ObservableObject {
    @Published var value: T
    private var job: Job?

    init(stateFlow: StateFlow<T>) {
        self.value = stateFlow.value
        self.job = stateFlow.collect { newValue in
            DispatchQueue.main.async {
                self.value = newValue
            }
        }
    }

    deinit {
        job?.cancel()
    }
}

struct TextFieldRenderer: View {
    @StateObject var adapter: StateAdapter<String>

    init(component: TextFieldComponent) {
        _adapter = StateObject(wrappedValue: StateAdapter(
            stateFlow: component.valueState
        ))
    }

    var body: some View {
        TextField("", text: $adapter.value)
            .onChange(of: adapter.value) { newValue in
                component.onValueChange?(newValue)
            }
    }
}
```

**How it works:**
1. `StateAdapter` bridges Kotlin Flow to SwiftUI `@Published`
2. SwiftUI observes `@Published` property
3. State changes trigger SwiftUI view updates
4. User input flows back through `onValueChange`

### Web (Kotlin/JS + React)

The Web renderer integrates with React hooks:

```kotlin
@JsExport
fun TextField(component: TextFieldComponent) {
    val (value, setValue) = useState(component.value.value)

    useEffect {
        val job = component.value.collect { newValue ->
            setValue(newValue)
        }
        cleanup { job.cancel() }
    }

    div {
        input {
            attrs.value = value
            attrs.onInput = { event ->
                val newValue = event.target.value
                setValue(newValue)
                component.onValueChange?.invoke(newValue)
            }
        }
    }
}
```

**How it works:**
1. `useState` creates React state
2. `useEffect` subscribes to Kotlin Flow
3. React re-renders on state changes
4. User input updates both React and AvaElements state

## Reactive Component Integration

### ReactiveComponent Rendering

`ReactiveComponent` works with all renderers by rebuilding its content when state changes:

```kotlin
class ReactiveComponent<T>(
    private val state: StateFlow<T>,
    private val builder: (T) -> Component
) : Component {

    override fun render(renderer: Renderer): Any {
        // Get current component based on state
        val currentComponent = builder(state.value)

        // Delegate rendering to platform renderer
        return currentComponent.render(renderer)
    }
}
```

**Android Integration:**
```kotlin
@Composable
fun RenderReactiveComponent(component: ReactiveComponent<*>) {
    val state = component.state.collectAsState()

    // Rebuild content when state changes
    val currentComponent = component.builder(state.value)
    RenderComponent(currentComponent)
}
```

**iOS Integration:**
```swift
struct ReactiveComponentView<T>: View {
    @StateObject var stateAdapter: StateAdapter<T>
    let builder: (T) -> Component

    var body: some View {
        // Rebuild when state changes
        RenderComponent(builder(stateAdapter.value))
    }
}
```

## Form State Integration

### Android Form Rendering

```kotlin
@Composable
fun RenderForm(form: FormState) {
    Column {
        form.fields.forEach { (name, field) ->
            val value = field.value.collectAsState()
            val error = field.error.collectAsState()

            TextField(
                value = value.value,
                onValueChange = { field.setValue(it) },
                isError = error.value != null,
                label = { Text(error.value ?: "") }
            )
        }

        Button(
            onClick = {
                if (form.validate()) {
                    // Submit
                }
            }
        ) {
            Text("Submit")
        }
    }
}
```

### iOS Form Rendering

```swift
struct FormView: View {
    @StateObject var form: FormStateAdapter

    var body: some View {
        VStack {
            ForEach(form.fields) { field in
                TextField(
                    field.name,
                    text: binding(for: field)
                )
                .border(field.hasError ? .red : .gray)

                if let error = field.error {
                    Text(error).foregroundColor(.red)
                }
            }

            Button("Submit") {
                if form.validate() {
                    // Submit
                }
            }
        }
    }

    func binding(for field: FieldState<String>) -> Binding<String> {
        Binding(
            get: { field.value.value },
            set: { field.setValue($0) }
        )
    }
}
```

## ViewModel Integration

### Lifecycle Management

ViewModels integrate with platform lifecycles:

**Android:**
```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState = viewModel.uiState.collectAsState()

            AvaElementsUI(uiState.value)
        }
    }
}
```

**iOS:**
```swift
class ViewController: UIViewController {
    private var viewModel: MyViewModel?

    override func viewDidLoad() {
        super.viewDidLoad()

        viewModel = MyViewModel()

        viewModel?.uiState.collect { state in
            self.updateUI(state)
        }
    }

    deinit {
        viewModel?.clear()
    }
}
```

## Data Binding Integration

### Two-Way Binding with Renderers

```kotlin
// Create binding
val nameBinding = dataBindingOf("") { newName ->
    viewModel.updateName(newName)
}

// Android Renderer
@Composable
fun NameField(binding: DataBinding<String>) {
    val value = binding.value.collectAsState()

    TextField(
        value = value.value,
        onValueChange = { binding.update(it) }
    )
}

// iOS Renderer
struct NameField: View {
    @StateObject var binding: DataBindingAdapter<String>

    var body: some View {
        TextField("Name", text: Binding(
            get: { binding.value },
            set: { binding.update($0) }
        ))
    }
}
```

## State Persistence Integration

### Platform-Specific Persistence

**Android (DataStore):**
```kotlin
class AndroidStatePersistence(
    private val context: Context
) : JsonStatePersistence() {

    private val dataStore = context.createDataStore("app_state")

    override suspend fun saveString(key: String, value: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun loadString(key: String): String? {
        return dataStore.data.first()[stringPreferencesKey(key)]
    }
}
```

**iOS (UserDefaults):**
```swift
class IOSStatePersistence: StatePersistence {
    private let defaults = UserDefaults.standard

    func save(key: String, state: Any) {
        let encoder = JSONEncoder()
        if let data = try? encoder.encode(state) {
            defaults.set(data, forKey: key)
        }
    }

    func restore<T>(key: String) -> T? {
        guard let data = defaults.data(forKey: key) else {
            return nil
        }
        let decoder = JSONDecoder()
        return try? decoder.decode(T.self, from: data)
    }
}
```

**Web (LocalStorage):**
```kotlin
class WebStatePersistence : JsonStatePersistence() {
    override suspend fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    override suspend fun loadString(key: String): String? {
        return window.localStorage.getItem(key)
    }
}
```

## Performance Optimizations

### 1. State Batching

Renderers batch state updates to minimize recompositions:

```kotlin
class OptimizedRenderer : Renderer {
    private val updateQueue = mutableListOf<() -> Unit>()
    private var isProcessing = false

    fun scheduleUpdate(update: () -> Unit) {
        updateQueue.add(update)

        if (!isProcessing) {
            isProcessing = true
            kotlinx.coroutines.delay(16) // 60fps
            processUpdates()
            isProcessing = false
        }
    }

    private fun processUpdates() {
        updateQueue.forEach { it() }
        updateQueue.clear()
    }
}
```

### 2. Selective Recomposition

Components only rebuild when observed state changes:

```kotlin
@Composable
fun SmartComponent(viewModel: MyViewModel) {
    // Only recomposes when name changes
    val name = viewModel.name.collectAsState()
    Text(name.value)

    // Won't cause recomposition of name display
    val age = viewModel.age.collectAsState()
    Text(age.value.toString())
}
```

### 3. State Hoisting

Keep state at the appropriate level to minimize updates:

```kotlin
// Good: State hoisted to appropriate level
@Composable
fun ParentComponent(viewModel: SharedViewModel) {
    val sharedState = viewModel.state.collectAsState()

    ChildA(sharedState.value.dataA)
    ChildB(sharedState.value.dataB)
}

// Avoid: State too high up causes unnecessary updates
@Composable
fun Parent(viewModel: SharedViewModel) {
    // All children recompose when any state changes
    val state = viewModel.state.collectAsState()
    Children(state)
}
```

## Testing Integration

### Testing with Renderers

```kotlin
@Test
fun testStateIntegration() = runTest {
    val state = mutableStateOf("Hello")
    val component = TextComponent(text = state.value.value)

    val renderer = TestRenderer()
    val rendered = component.render(renderer)

    assertEquals("Hello", rendered.text)

    // Update state
    state.setValue("World")

    // Re-render
    val rerendered = component.render(renderer)
    assertEquals("World", rerendered.text)
}
```

## Best Practices

### 1. Unidirectional Data Flow

```
User Action → ViewModel → State Update → UI Update
```

```kotlin
// Good
Button("Click") {
    onClick = { viewModel.handleClick() }
}

// Avoid direct state manipulation
Button("Click") {
    onClick = { state.setValue(newValue) } // Bypasses ViewModel
}
```

### 2. State Ownership

- **ViewModels** own business logic state
- **Components** own UI-specific state
- **Forms** own validation state

```kotlin
class MyViewModel : MagicViewModel() {
    // Business logic state
    private val _userData = mutableState<User?>(null)
    val userData = _userData.asState()
}

@Composable
fun MyComponent(viewModel: MyViewModel) {
    // UI-specific state
    val expanded = remember { mutableStateOf(false) }

    // Use ViewModel state for data
    val user = viewModel.userData.collectAsState()
}
```

### 3. Avoid State Duplication

Use derived state instead of copying:

```kotlin
// Good
val filteredItems = derivedStateFrom(items, query) { items, q ->
    items.filter { it.contains(q) }
}

// Avoid
val filteredItems = mutableStateOf(emptyList())
items.value.collect {
    filteredItems.setValue(it.filter { ... })
}
```

## Troubleshooting

### State Not Updating UI

**Problem:** State changes but UI doesn't update

**Solution:**
1. Ensure you're using `StateFlow` not regular variables
2. Check that component observes the state with `collectAsState()`
3. Verify renderer is properly integrating with platform state system

```kotlin
// Wrong
var count = 0 // UI won't update

// Correct
val count = mutableStateOf(0)
```

### Memory Leaks

**Problem:** ViewModels or state collections not cleared

**Solution:**
1. Always call `viewModel.clear()` when done
2. Use proper lifecycle scoping
3. Cancel Flow collections in cleanup

```kotlin
// Android
override fun onDestroy() {
    super.onDestroy()
    viewModel.clear()
}

// iOS
deinit {
    viewModel.clear()
}
```

### Performance Issues

**Problem:** Too many recompositions

**Solution:**
1. Use derived state for computations
2. Implement proper equality checks
3. Hoist state appropriately

```kotlin
// Implement equals for state objects
data class User(val name: String, val age: Int)

// Use StateFlow distinctUntilChanged
val state = _state.distinctUntilChanged()
```

## Summary

The StateManagement module provides seamless integration with AvaElements renderers through:

1. **Kotlin Flow** as the reactive foundation
2. **Platform-specific adapters** that bridge to native state systems
3. **Automatic UI updates** when state changes
4. **Bidirectional data binding** for forms and inputs
5. **Lifecycle-aware ViewModels** that manage state safely
6. **Persistence layer** that works across platforms

This architecture ensures that developers can write state management code once and have it work correctly across Android, iOS, Web, and Desktop, while still leveraging each platform's native UI update mechanisms.

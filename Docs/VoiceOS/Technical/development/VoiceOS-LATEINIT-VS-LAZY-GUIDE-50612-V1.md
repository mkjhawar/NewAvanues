# VoiceOS lateinit vs lazy Consistency Guide

**Version:** 1.0
**Author:** Manoj Jhawar
**Created:** 2025-11-09
**Phase:** 3 (Medium Priority)

---

## Overview

This guide establishes consistent patterns for using `lateinit` vs `lazy` initialization in VoiceOS codebase.

### Quick Reference

| Use Case | Use | Reason |
|----------|-----|--------|
| Android Views (findViewById) | `lateinit` | Initialized in onCreate(), non-null after |
| Dependency injection | `lateinit` | Injected before first use |
| Properties initialized in init{} or constructor | Direct assignment | No delayed initialization needed |
| Expensive computations | `lazy` | Computed only when first accessed |
| Thread-safe singletons | `lazy(LazyThreadSafetyMode.SYNCHRONIZED)` | Default, thread-safe |
| Single-threaded context | `lazy(LazyThreadSafetyMode.NONE)` | Better performance, not thread-safe |
| Nullable types | Regular property with `= null` | Can't use lateinit with nullables |

---

## Table of Contents

1. [lateinit Modifier](#lateinit-modifier)
2. [lazy Initialization](#lazy-initialization)
3. [When to Use Each](#when-to-use-each)
4. [Common Patterns in VoiceOS](#common-patterns-in-voiceos)
5. [Anti-Patterns](#anti-patterns)
6. [Migration Guide](#migration-guide)

---

## lateinit Modifier

### What is lateinit?

`lateinit` allows you to declare a non-null property without initializing it immediately. You must initialize it before first access, otherwise you get `UninitializedPropertyAccessException`.

### Characteristics

- **Only for `var`** (not `val`)
- **Only for non-null types** (not nullable `?`)
- **Not for primitives** (Int, Boolean, etc.)
- **Can check if initialized**: `::property.isInitialized`
- **Mutable after initialization**

### Basic Usage

```kotlin
class MyActivity : AppCompatActivity() {
    // ✅ CORRECT: Initialized in onCreate()
    private lateinit var button: Button
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)
    }
}
```

### Checking Initialization

```kotlin
class DataManager {
    private lateinit var cache: Cache

    fun ensureCacheInitialized() {
        if (!::cache.isInitialized) {
            cache = Cache()
        }
    }

    fun clearCache() {
        if (::cache.isInitialized) {
            cache.clear()
        }
    }
}
```

### When to Use lateinit

1. **Android Views**
   ```kotlin
   private lateinit var recyclerView: RecyclerView
   ```

2. **Dependency Injection**
   ```kotlin
   @Inject
   lateinit var repository: UserRepository
   ```

3. **Properties initialized in lifecycle methods**
   ```kotlin
   class Fragment : Fragment() {
       private lateinit var adapter: MyAdapter

       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           adapter = MyAdapter()
       }
   }
   ```

4. **Properties that can't be initialized in constructor**
   ```kotlin
   class Service : Service() {
       private lateinit var binder: MyBinder

       override fun onBind(intent: Intent): IBinder {
           binder = MyBinder()
           return binder
       }
   }
   ```

---

## lazy Initialization

### What is lazy?

`lazy` is a delegate that defers initialization until the property is first accessed. The initialization block runs once, and the result is cached.

### Characteristics

- **Only for `val`** (immutable)
- **Thread-safe by default**
- **Initialization happens on first access**
- **Value is cached** (computed once, reused)
- **Can't check if initialized** (always safe to access)

### Basic Usage

```kotlin
class DataProcessor {
    // ✅ CORRECT: Expensive computation deferred until needed
    private val expensiveData: List<Data> by lazy {
        // This runs only once, on first access
        loadDataFromDatabase()
    }

    private fun loadDataFromDatabase(): List<Data> {
        // Expensive operation
        return database.query()
    }
}
```

### Thread Safety Modes

```kotlin
// 1. SYNCHRONIZED (default): Thread-safe, slower
private val sharedResource by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    ExpensiveResource()
}

// 2. PUBLICATION: Multiple threads can initialize, first one wins
private val resource by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Resource()
}

// 3. NONE: Not thread-safe, fastest (use only in single-threaded context)
private val uiComponent by lazy(LazyThreadSafetyMode.NONE) {
    // Safe: Only accessed from UI thread
    TextView(context)
}
```

### When to Use lazy

1. **Expensive Computations**
   ```kotlin
   private val processedData by lazy {
       rawData.map { process(it) }
   }
   ```

2. **Singletons**
   ```kotlin
   companion object {
       val instance by lazy { MyManager() }
   }
   ```

3. **Properties requiring context**
   ```kotlin
   class MyClass(private val context: Context) {
       private val sharedPrefs by lazy {
           context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
       }
   }
   ```

4. **Optional expensive resources**
   ```kotlin
   private val database by lazy {
       // Only created if actually needed
       Room.databaseBuilder(/*...*/).build()
   }
   ```

---

## When to Use Each

### Decision Flow Chart

```
Need delayed initialization?
├─ No → Use direct assignment
│   val property = value
│
└─ Yes
    │
    ├─ Is it a var (mutable)?
    │   ├─ Yes → Use lateinit
    │   │   lateinit var property: Type
    │   │
    │   └─ No (it's val)
    │       ├─ Expensive to compute?
    │       │   ├─ Yes → Use lazy
    │       │   │   val property by lazy { ... }
    │       │   │
    │       │   └─ No → Consider if delay is needed
    │       │       ├─ Still need delay → Use lazy
    │       │       └─ No delay needed → Direct assignment
    │       │
    │       └─ Need to change after init?
    │           ├─ Yes → Use var with lateinit
    │           └─ No → Use val with lazy
```

### Comparison Table

| Feature | lateinit | lazy |
|---------|----------|------|
| **Mutability** | `var` only | `val` only |
| **Nullability** | Non-null only | Any type |
| **Primitives** | ❌ No | ✅ Yes |
| **When initialized** | Manually, when you call it | Automatically, on first access |
| **Thread-safe** | ❌ No (your responsibility) | ✅ Yes (by default) |
| **Can check if initialized** | ✅ Yes (`::property.isInitialized`) | ❌ No |
| **Overhead** | None | Small (delegation + synchronization) |
| **Use case** | Android views, DI | Expensive computations, singletons |

---

## Common Patterns in VoiceOS

### Pattern 1: Android Activity/Fragment

```kotlin
class VoiceOSActivity : AppCompatActivity() {
    // ✅ CORRECT: Views initialized in onCreate()
    private lateinit var micButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var recyclerView: RecyclerView

    // ✅ CORRECT: Adapter created lazily (expensive)
    private val adapter by lazy {
        CommandAdapter(this)
    }

    // ✅ CORRECT: ViewModel with lazy (singleton per activity)
    private val viewModel: CommandViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        micButton = findViewById(R.id.mic_button)
        statusText = findViewById(R.id.status_text)
        recyclerView = findViewById(R.id.recycler_view)

        // Adapter created on first access
        recyclerView.adapter = adapter
    }
}
```

### Pattern 2: Service

```kotlin
class VoiceOSService : AccessibilityService() {
    // ✅ CORRECT: Binder initialized in onBind()
    private lateinit var binder: LocalBinder

    // ✅ CORRECT: Manager created lazily (expensive)
    private val commandManager by lazy {
        CommandManager(this)
    }

    // ✅ CORRECT: Database lazy (expensive, might not be used)
    private val database by lazy {
        VoiceOSDatabase.getInstance(this)
    }

    override fun onBind(intent: Intent): IBinder {
        binder = LocalBinder()
        return binder
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // commandManager created on first use
        commandManager.handleEvent(event)
    }
}
```

### Pattern 3: Custom Manager Classes

```kotlin
class UserConsentManager(private val context: Context) {
    // ✅ CORRECT: SharedPreferences lazy (context needed, expensive)
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ✅ CORRECT: Flow lazy (created only if observed)
    private val _consentState by lazy {
        MutableStateFlow(loadConsentState())
    }
    val consentState: StateFlow<ConsentState> by lazy {
        _consentState.asStateFlow()
    }

    companion object {
        private const val PREFS_NAME = "user_consent"
    }
}
```

### Pattern 4: Database DAOs

```kotlin
@Database(entities = [Command::class], version = 10)
abstract class VoiceOSDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao

    companion object {
        // ✅ CORRECT: Singleton database with lazy
        @Volatile
        private var INSTANCE: VoiceOSDatabase? = null

        fun getInstance(context: Context): VoiceOSDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                VoiceOSDatabase::class.java,
                "voiceos.db"
            ).build()
    }
}
```

### Pattern 5: ViewModels

```kotlin
class CommandViewModel : ViewModel() {
    // ✅ CORRECT: Repository lazy (might not be used immediately)
    private val repository by lazy {
        CommandRepository()
    }

    // ✅ CORRECT: LiveData lazy (expensive transformation)
    val commands: LiveData<List<Command>> by lazy {
        repository.getAllCommands().map { list ->
            list.sortedBy { it.timestamp }
        }
    }

    // ❌ INCORRECT: Don't use lateinit in ViewModels
    // private lateinit var repository: CommandRepository
}
```

---

## Anti-Patterns

### ❌ Anti-Pattern 1: lateinit for Primitives

```kotlin
// ❌ INCORRECT: Can't use lateinit with primitives
class Bad {
    private lateinit var count: Int // Compilation error
}

// ✅ CORRECT: Use nullable or default value
class Good {
    private var count: Int = 0
    // OR
    private var count: Int? = null
}
```

### ❌ Anti-Pattern 2: lateinit for Nullables

```kotlin
// ❌ INCORRECT: Can't use lateinit with nullables
class Bad {
    private lateinit var name: String? // Compilation error
}

// ✅ CORRECT: Just use nullable
class Good {
    private var name: String? = null
}
```

### ❌ Anti-Pattern 3: lazy for Views

```kotlin
// ❌ INCORRECT: Views should use lateinit
class BadActivity : AppCompatActivity() {
    private val button by lazy {
        findViewById<Button>(R.id.button) // Called every time!
    }
}

// ✅ CORRECT: Use lateinit
class GoodActivity : AppCompatActivity() {
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.button)
    }
}
```

### ❌ Anti-Pattern 4: lateinit for Immutable Data

```kotlin
// ❌ INCORRECT: Don't need mutability after init
class Bad {
    private lateinit var apiKey: String

    init {
        apiKey = loadApiKey()
    }

    // apiKey never changes after init, why var?
}

// ✅ CORRECT: Use val with lazy or direct assignment
class Good {
    private val apiKey: String = loadApiKey()
    // OR
    private val apiKey by lazy { loadApiKey() }
}
```

### ❌ Anti-Pattern 5: Unnecessary lazy

```kotlin
// ❌ INCORRECT: No benefit from lazy
class Bad {
    private val name by lazy { "VoiceOS" } // Why lazy for constant?
}

// ✅ CORRECT: Direct assignment
class Good {
    private val name = "VoiceOS"
}
```

---

## Migration Guide

### Step 1: Identify Current Usage

```bash
# Find all lateinit usage
grep -r "lateinit" modules/apps/VoiceOSCore/src/main --include="*.kt"

# Find all lazy usage
grep -r "by lazy" modules/apps/VoiceOSCore/src/main --include="*.kt"
```

### Step 2: Audit Each Case

For each `lateinit`:
1. Is it a `var`? ✅ Good
2. Is it non-null? ✅ Good
3. Is it a primitive? ❌ Convert to nullable or default value
4. Is it initialized before first use? ✅ Good
5. Does it ever change after init? If NO → Consider `val` with lazy

For each `lazy`:
1. Is it a `val`? ✅ Good
2. Is the initialization expensive? ✅ Good
3. Is it a view? ❌ Convert to lateinit
4. Is thread-safety needed? Use appropriate mode

### Step 3: Apply Patterns

**Convert unnecessary lazy to lateinit (views):**
```kotlin
// BEFORE
private val button by lazy {
    findViewById<Button>(R.id.button)
}

// AFTER
private lateinit var button: Button
// Initialize in onCreate()
```

**Convert lateinit to lazy (immutable, expensive):**
```kotlin
// BEFORE
private lateinit var database: AppDatabase
init {
    database = Room.databaseBuilder(/*...*/).build()
}

// AFTER
private val database by lazy {
    Room.databaseBuilder(/*...*/).build()
}
```

---

## Best Practices Summary

### DO ✅

- Use `lateinit` for Android views (initialized in onCreate/onCreateView)
- Use `lateinit` for dependency injection
- Use `lazy` for expensive computations
- Use `lazy` for singletons
- Use `lazy` with appropriate thread-safety mode
- Check `::property.isInitialized` before accessing lateinit in cleanup code
- Use direct assignment when initialization is simple and immediate

### DON'T ❌

- Don't use `lateinit` for primitives
- Don't use `lateinit` for nullable types
- Don't use `lazy` for Android views
- Don't use `lazy` for simple constants
- Don't use `lateinit` if the property should be `val`
- Don't forget to initialize `lateinit` properties before use
- Don't use synchronized lazy in single-threaded contexts (use NONE mode)

---

## VoiceOS Code Review Checklist

When reviewing code, check:

- [ ] All `lateinit var` are initialized before first use
- [ ] No `lateinit` for primitives or nullables
- [ ] Android views use `lateinit`, not `lazy`
- [ ] Expensive computations use `lazy`, not direct assignment
- [ ] Singletons use `lazy` with proper thread-safety mode
- [ ] No unnecessary `lazy` for simple constants
- [ ] Properties that don't change after init use `val`, not `lateinit var`
- [ ] Cleanup code checks `::property.isInitialized` before accessing lateinit

---

## Examples from VoiceOS Codebase

### ActionCoordinator.kt

```kotlin
class ActionCoordinator {
    // ✅ CORRECT: Managers created lazily (expensive, might not all be used)
    private val commandManager by lazy { CommandManager() }
    private val metricsCollector by lazy { CommandMetricsCollector() }

    // ✅ CORRECT: Context stored immediately (passed in constructor)
    private val context: Context

    constructor(context: Context) {
        this.context = context
    }
}
```

### VoiceOSDatabase.kt

```kotlin
@Database(/*...*/)
abstract class VoiceOSDatabase : RoomDatabase() {
    companion object {
        // ✅ CORRECT: Singleton with lazy-like pattern (thread-safe)
        @Volatile
        private var INSTANCE: VoiceOSDatabase? = null

        fun getInstance(context: Context): VoiceOSDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
```

---

## Conclusion

**General Rule:**
- **Use `lateinit`** when you need a mutable non-null property that's initialized later (Android views, DI)
- **Use `lazy`** when you need an immutable property with deferred, expensive initialization
- **Use direct assignment** when initialization is simple and immediate

When in doubt, prefer `val` over `var`, and `lazy` over `lateinit` for immutable properties.

---

**End of Guide**

**Last Updated:** 2025-11-09
**Version:** 1.0
**Phase:** 3 (Medium Priority)

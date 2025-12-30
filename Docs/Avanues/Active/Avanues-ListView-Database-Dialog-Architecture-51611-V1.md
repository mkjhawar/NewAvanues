# ListView, Database, Dialog - Component Architecture
## VoiceOS Session 4 Continuation - October 28, 2025

---

## 🎯 Objective

Design and implement 3 critical components to enable full TODO app functionality:
1. **ListView** - Scrollable, dynamic item lists
2. **Database** - Persistent storage with CRUD operations
3. **Dialog** - Alert, confirm, and input dialogs

---

## 📋 Component 1: ListView

### Overview
Platform-agnostic scrollable list component with dynamic data binding, item templates, and event handling.

### Core Requirements
- Display collections of items in a scrollable container
- Support item templates/renderers
- Handle selection (single/multiple)
- Support dynamic add/remove/update operations
- Provide callbacks for item clicks, long presses
- Support separators/dividers
- Empty state handling
- Pull-to-refresh (optional)
- Infinite scroll/pagination (optional)

### API Design

```kotlin
expect class ListView {
    // State
    var items: List<ListItem>
    var selectedIndices: Set<Int>
    var selectionMode: SelectionMode  // NONE, SINGLE, MULTIPLE
    var emptyMessage: String?

    // Configuration
    var config: ListViewConfig
    var itemRenderer: ((ListItem, Int) -> Unit)?

    // Callbacks
    var onItemClick: ((ListItem, Int) -> Unit)?
    var onItemLongPress: ((ListItem, Int) -> Unit)?
    var onSelectionChanged: ((Set<Int>) -> Unit)?
    var onRefresh: (() -> Unit)?

    // Methods
    fun addItem(item: ListItem)
    fun removeItem(index: Int)
    fun updateItem(index: Int, item: ListItem)
    fun clearItems()
    fun scrollToIndex(index: Int)
    fun selectItem(index: Int)
    fun deselectItem(index: Int)
    fun clearSelection()
}

data class ListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: String? = null,
    val data: Map<String, Any?> = emptyMap()
)

enum class SelectionMode {
    NONE, SINGLE, MULTIPLE
}

data class ListViewConfig(
    val showDividers: Boolean = true,
    val dividerColor: String? = null,
    val itemHeight: Float? = null,  // null = dynamic
    val enableRefresh: Boolean = false,
    val enableInfiniteScroll: Boolean = false,
    val orientation: Orientation = Orientation.VERTICAL
)

enum class Orientation {
    VERTICAL, HORIZONTAL, GRID
}

expect object ListViewFactory {
    fun create(config: ListViewConfig = ListViewConfig()): ListView
}

class ListViewBuilder {
    private var config = ListViewConfig()
    private var items = mutableListOf<ListItem>()

    fun withDividers(show: Boolean = true): ListViewBuilder
    fun withItems(items: List<ListItem>): ListViewBuilder
    fun vertical(): ListViewBuilder
    fun horizontal(): ListViewBuilder
    fun grid(columns: Int): ListViewBuilder
    fun build(): ListView
}
```

### Platform Mappings

**Kotlin Compose**:
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize()
) {
    items(listItems) { item ->
        ListItemView(item)
    }
}
```

**SwiftUI**:
```swift
List(listItems) { item in
    ListItemView(item: item)
}
```

**React TypeScript**:
```typescript
<List>
  {listItems.map(item => (
    <ListItem key={item.id}>
      <ListItemText primary={item.title} secondary={item.subtitle} />
    </ListItem>
  ))}
</List>
```

### File Structure
```
runtime/libraries/ListView/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceos/listview/
│   │   ├── ListView.kt (expect class)
│   │   ├── ListItem.kt (data classes)
│   │   ├── SelectionMode.kt (enum)
│   │   ├── Orientation.kt (enum)
│   │   ├── ListViewConfig.kt (configuration)
│   │   └── ListViewFactory.kt (factory)
│   ├── androidMain/kotlin/.../ListView.android.kt
│   ├── iosMain/kotlin/.../ListView.ios.kt
│   └── jvmMain/kotlin/.../ListView.jvm.kt
├── README.md
└── EXAMPLE_USAGE.md
```

### Estimated Complexity
- **Lines of Code**: ~3,500
- **Files**: 10
- **Time**: 6-8 hours
- **Complexity**: HIGH (dynamic rendering, selection state management)

---

## 💾 Component 2: Database

### Overview
Platform-agnostic key-value and structured data storage with CRUD operations, queries, and persistence.

### Core Requirements
- Store/retrieve key-value pairs
- Support structured data (tables/collections)
- CRUD operations (Create, Read, Update, Delete)
- Query/filter capabilities
- Persistence across app restarts
- Transaction support
- Migration/versioning (optional)
- Encryption (optional)

### API Design

```kotlin
expect class Database {
    // Initialization
    var name: String
    var version: Int

    // Key-Value Operations
    fun put(key: String, value: Any?)
    fun get(key: String): Any?
    fun <T> getTyped(key: String): T?
    fun remove(key: String)
    fun clear()
    fun containsKey(key: String): Boolean
    fun keys(): Set<String>

    // Collection Operations
    fun createCollection(name: String, schema: CollectionSchema? = null)
    fun getCollection(name: String): Collection?
    fun dropCollection(name: String)
    fun listCollections(): List<String>

    // Lifecycle
    fun open()
    fun close()
    fun flush()  // Force write to disk
}

expect class Collection {
    val name: String

    // CRUD
    fun insert(document: Document): String  // Returns ID
    fun find(query: Query): List<Document>
    fun findById(id: String): Document?
    fun findOne(query: Query): Document?
    fun update(query: Query, updates: Map<String, Any?>): Int  // Returns count
    fun updateById(id: String, updates: Map<String, Any?>): Boolean
    fun delete(query: Query): Int  // Returns count
    fun deleteById(id: String): Boolean
    fun count(query: Query? = null): Int

    // Indexing
    fun createIndex(field: String)
    fun dropIndex(field: String)
}

data class Document(
    val id: String,
    val data: Map<String, Any?>
) {
    fun getString(key: String): String?
    fun getInt(key: String): Int?
    fun getBoolean(key: String): Boolean?
    fun <T> get(key: String): T?
}

data class Query(
    val filters: Map<String, Any?> = emptyMap(),
    val orderBy: String? = null,
    val ascending: Boolean = true,
    val limit: Int? = null,
    val offset: Int? = null
) {
    companion object {
        fun where(field: String, value: Any?): Query
        fun all(): Query
    }
}

data class CollectionSchema(
    val fields: Map<String, FieldType>
)

enum class FieldType {
    STRING, INT, FLOAT, BOOLEAN, TIMESTAMP, JSON
}

expect object DatabaseFactory {
    fun create(name: String, version: Int = 1): Database
    fun default(): Database  // Returns app's default database
}

class DatabaseBuilder {
    private var name: String = "default"
    private var version: Int = 1

    fun withName(name: String): DatabaseBuilder
    fun withVersion(version: Int): DatabaseBuilder
    fun build(): Database
}
```

### Platform Mappings

**Android**:
- SharedPreferences (key-value)
- Room Database (structured)
- SQLite (fallback)

**iOS**:
- UserDefaults (key-value)
- Core Data (structured)
- SQLite (fallback)

**JVM**:
- Properties file (key-value)
- SQLite via JDBC (structured)
- H2 Database (alternative)

### File Structure
```
runtime/libraries/Database/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceos/database/
│   │   ├── Database.kt (expect class)
│   │   ├── Collection.kt (expect class)
│   │   ├── Document.kt (data class)
│   │   ├── Query.kt (data class + builder)
│   │   ├── CollectionSchema.kt (schema definitions)
│   │   ├── FieldType.kt (enum)
│   │   └── DatabaseFactory.kt (factory)
│   ├── androidMain/kotlin/.../
│   │   ├── Database.android.kt
│   │   ├── Collection.android.kt
│   │   └── RoomHelper.kt
│   ├── iosMain/kotlin/.../
│   │   ├── Database.ios.kt
│   │   ├── Collection.ios.kt
│   │   └── CoreDataHelper.kt
│   └── jvmMain/kotlin/.../
│       ├── Database.jvm.kt
│       ├── Collection.jvm.kt
│       └── SQLiteHelper.kt
├── README.md
└── EXAMPLE_USAGE.md
```

### Estimated Complexity
- **Lines of Code**: ~4,500
- **Files**: 14
- **Time**: 8-10 hours
- **Complexity**: VERY HIGH (persistence, queries, platform-specific DB integration)

---

## 💬 Component 3: Dialog

### Overview
Platform-agnostic dialog/modal component for alerts, confirmations, and input prompts.

### Core Requirements
- Alert dialogs (message + OK button)
- Confirm dialogs (message + Yes/No buttons)
- Input dialogs (message + text field + OK/Cancel)
- Custom dialogs (custom content)
- Configurable buttons, titles, messages
- Callbacks for button clicks
- Dismiss on background tap (optional)
- Animation support

### API Design

```kotlin
expect class Dialog {
    // State
    var title: String?
    var message: String?
    var isVisible: Boolean

    // Configuration
    var type: DialogType
    var buttons: List<DialogButton>
    var dismissOnBackgroundTap: Boolean
    var icon: String?

    // Input (for INPUT type)
    var inputValue: String
    var inputPlaceholder: String?
    var inputType: InputType

    // Callbacks
    var onButtonClick: ((DialogButton) -> Unit)?
    var onDismiss: (() -> Unit)?
    var onShow: (() -> Unit)?

    // Methods
    fun show()
    fun hide()
    fun setContent(content: Any)  // Platform-specific content
}

enum class DialogType {
    ALERT,       // Message + OK
    CONFIRM,     // Message + Yes/No
    INPUT,       // Message + TextField + OK/Cancel
    CUSTOM       // Custom content
}

data class DialogButton(
    val text: String,
    val style: ButtonStyle = ButtonStyle.DEFAULT,
    val action: String? = null  // Action identifier
)

enum class ButtonStyle {
    DEFAULT, PRIMARY, DESTRUCTIVE, CANCEL
}

data class DialogConfig(
    val title: String? = null,
    val message: String? = null,
    val type: DialogType = DialogType.ALERT,
    val buttons: List<DialogButton> = listOf(DialogButton("OK")),
    val dismissOnBackgroundTap: Boolean = true,
    val icon: String? = null
)

expect object DialogFactory {
    fun create(config: DialogConfig): Dialog
    fun alert(title: String, message: String): Dialog
    fun confirm(title: String, message: String, onConfirm: () -> Unit, onCancel: () -> Unit): Dialog
    fun input(title: String, message: String, onSubmit: (String) -> Unit): Dialog
}

class DialogBuilder {
    private var title: String? = null
    private var message: String? = null
    private var type: DialogType = DialogType.ALERT
    private var buttons = mutableListOf<DialogButton>()

    fun withTitle(title: String): DialogBuilder
    fun withMessage(message: String): DialogBuilder
    fun alert(): DialogBuilder
    fun confirm(): DialogBuilder
    fun input(): DialogBuilder
    fun addButton(text: String, style: ButtonStyle = ButtonStyle.DEFAULT): DialogBuilder
    fun build(): Dialog
}
```

### Platform Mappings

**Kotlin Compose**:
```kotlin
AlertDialog(
    onDismissRequest = { dialog.hide() },
    title = { Text(dialog.title ?: "") },
    text = { Text(dialog.message ?: "") },
    confirmButton = {
        Button(onClick = { /* handle */ }) {
            Text("OK")
        }
    }
)
```

**SwiftUI**:
```swift
.alert(dialog.title ?? "", isPresented: $showDialog) {
    Button("OK") { /* handle */ }
} message: {
    Text(dialog.message ?? "")
}
```

**React TypeScript**:
```typescript
<Dialog open={dialog.isVisible} onClose={() => dialog.hide()}>
  <DialogTitle>{dialog.title}</DialogTitle>
  <DialogContent>
    <DialogContentText>{dialog.message}</DialogContentText>
  </DialogContent>
  <DialogActions>
    <Button onClick={() => handleClick()}>OK</Button>
  </DialogActions>
</Dialog>
```

### File Structure
```
runtime/libraries/Dialog/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceos/dialog/
│   │   ├── Dialog.kt (expect class)
│   │   ├── DialogType.kt (enum)
│   │   ├── DialogButton.kt (data class)
│   │   ├── ButtonStyle.kt (enum)
│   │   ├── DialogConfig.kt (configuration)
│   │   └── DialogFactory.kt (factory)
│   ├── androidMain/kotlin/.../Dialog.android.kt
│   ├── iosMain/kotlin/.../Dialog.ios.kt
│   └── jvmMain/kotlin/.../Dialog.jvm.kt
├── README.md
└── EXAMPLE_USAGE.md
```

### Estimated Complexity
- **Lines of Code**: ~2,800
- **Files**: 10
- **Time**: 4-6 hours
- **Complexity**: MEDIUM (state management, platform dialog APIs)

---

## 📊 Overall Statistics

### Total Estimated Effort
- **ListView**: 6-8 hours, ~3,500 lines, HIGH complexity
- **Database**: 8-10 hours, ~4,500 lines, VERY HIGH complexity
- **Dialog**: 4-6 hours, ~2,800 lines, MEDIUM complexity

**Grand Total**:
- **Time**: 18-24 hours (sequential) OR 8-10 hours (3 parallel agents)
- **Lines**: ~10,800 lines
- **Files**: 34 files
- **Libraries**: 3 new KMP libraries

### Parallel Execution Plan

**Agent 5: ListView Builder**
- Creates ListView library (10 files, ~3,500 lines)
- Implements expect/actual pattern for 3 platforms
- Time: ~8 hours

**Agent 6: Database Builder**
- Creates Database library (14 files, ~4,500 lines)
- Integrates platform databases (Room, CoreData, SQLite)
- Time: ~10 hours

**Agent 7: Dialog Builder**
- Creates Dialog library (10 files, ~2,800 lines)
- Implements platform dialog APIs
- Time: ~6 hours

**Agent 8: Registry Updater**
- Updates BuiltInComponents.kt
- Adds 3 component descriptors (ListView, Database, Dialog)
- Adds ~30 properties, ~10 callbacks
- Time: ~1 hour

**Agent 9: Generator Updates**
- Updates KotlinComponentMapper (ListView, Database, Dialog)
- Updates SwiftUIGenerator (List, CoreData, Alert)
- Updates ReactTypeScriptGenerator (MUI List, IndexedDB, Dialog)
- Adds ~400 lines of generator code
- Time: ~3 hours

**Parallel Execution**: ~10 hours (vs 24 hours sequential)

---

## 🎯 TODO App Use Cases Enabled

### With ListView
- ✅ Display list of TODO items
- ✅ Scroll through tasks
- ✅ Select/deselect tasks
- ✅ Click to edit tasks
- ✅ Empty state ("No tasks yet")

### With Database
- ✅ Persist tasks across app restarts
- ✅ CRUD operations on tasks
- ✅ Query completed/incomplete tasks
- ✅ Store task metadata (created date, priority, etc.)

### With Dialog
- ✅ Confirm before deleting tasks
- ✅ Alert on errors
- ✅ Input dialog for editing task names
- ✅ Confirm bulk operations

### Complete TODO App Features
With all 10 components (Text, Button, Container, ColorPicker, Preferences, TextField, Checkbox, ListView, Database, Dialog), VoiceOS can generate:

1. **Full-featured TODO apps** with database persistence
2. **Settings pages** with dialogs and persistence
3. **Contact management** apps
4. **Note-taking** apps
5. **Shopping lists**
6. **Task trackers**
7. **Survey/form** apps with validation

---

## 🚀 Next Steps

1. **Review this architecture** - Confirm approach before building
2. **Deploy 5 parallel agents** - ListView, Database, Dialog, Registry, Generators
3. **Validate builds** - Ensure all libraries compile successfully
4. **Test TODO app** - Generate complete TODO app with all 10 components
5. **Document** - Create comprehensive TODO app example

---

**End of Architecture Document**

Ready to deploy parallel agents? (y/n)

---

*Created by Claude Code*
*Date: October 28, 2025*
*Estimated Total Impact: 10,800+ lines across 34 files*

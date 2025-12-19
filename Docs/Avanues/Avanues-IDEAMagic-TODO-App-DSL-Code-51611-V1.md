# IDEAMagic TODO App - Actual DSL Code

This document shows the **actual IDEAMagic/AvaUI/AvaCode syntax** you would write to create the TODO app.

---

## 1. Data Models (AvaCode)

```kotlin
// TodoModels.magic.kt
magic {
    model("TodoTask") {
        field("id", Type.String, primary = true)
        field("title", Type.String, required = true)
        field("description", Type.String, default = "")
        field("completed", Type.Boolean, default = false)
        field("priority", Type.Enum("Priority"), default = "MEDIUM")
        field("category", Type.String, default = "General")
        field("dueDate", Type.Long, nullable = true)
        field("createdAt", Type.Long, default = "System.currentTimeMillis()")

        enum("Priority") {
            value("LOW")
            value("MEDIUM")
            value("HIGH")
            value("URGENT")
        }

        computed("isOverdue") {
            "dueDate != null && dueDate < System.currentTimeMillis() && !completed"
        }

        computed("priorityColor") {
            """
            when (priority) {
                Priority.LOW -> Color(0xFF4CAF50)      // Green
                Priority.MEDIUM -> Color(0xFF2196F3)   // Blue
                Priority.HIGH -> Color(0xFFFF9800)     // Orange
                Priority.URGENT -> Color(0xFFF44336)   // Red
            }
            """
        }
    }

    model("TodoStats") {
        field("total", Type.Int)
        field("completed", Type.Int)
        field("active", Type.Int)
        field("overdue", Type.Int)

        computed("completionRate") {
            "if (total > 0) (completed * 100) / total else 0"
        }
    }
}
```

---

## 2. State Management (MagicState)

```kotlin
// TodoState.magic.kt
magic {
    state("TodoAppState") {
        // State properties
        var("tasks", Type.List("TodoTask"), default = "emptyList()")
        var("filter", Type.Enum("FilterType"), default = "ALL")
        var("selectedCategory", Type.String, default = "All")
        var("searchQuery", Type.String, default = "")
        var("isAddingTask", Type.Boolean, default = false)
        var("editingTask", Type.Nullable("TodoTask"), default = null)
        var("sortBy", Type.Enum("SortType"), default = "CREATED_DATE")

        // Computed properties
        computed("categories") {
            """
            listOf("All") + tasks.map { it.category }.distinct().sorted()
            """
        }

        computed("filteredTasks") {
            """
            tasks
                .filter { task ->
                    // Filter by type
                    val matchesFilter = when (filter) {
                        FilterType.ALL -> true
                        FilterType.ACTIVE -> !task.completed
                        FilterType.COMPLETED -> task.completed
                    }

                    // Filter by category
                    val matchesCategory = selectedCategory == "All" || task.category == selectedCategory

                    // Filter by search
                    val matchesSearch = searchQuery.isEmpty() ||
                        task.title.contains(searchQuery, ignoreCase = true) ||
                        task.description.contains(searchQuery, ignoreCase = true)

                    matchesFilter && matchesCategory && matchesSearch
                }
                .sortedWith(
                    when (sortBy) {
                        SortType.CREATED_DATE -> compareByDescending { it.createdAt }
                        SortType.DUE_DATE -> compareBy<TodoTask> { it.dueDate ?: Long.MAX_VALUE }
                        SortType.PRIORITY -> compareByDescending { it.priority }
                        SortType.TITLE -> compareBy { it.title }
                    }
                )
            """
        }

        computed("stats") {
            """
            TodoStats(
                total = tasks.size,
                completed = tasks.count { it.completed },
                active = tasks.count { !it.completed },
                overdue = tasks.count { it.isOverdue }
            )
            """
        }

        // Enums
        enum("FilterType") {
            value("ALL")
            value("ACTIVE")
            value("COMPLETED")
        }

        enum("SortType") {
            value("CREATED_DATE")
            value("DUE_DATE")
            value("PRIORITY")
            value("TITLE")
        }
    }
}
```

---

## 3. Main UI (AvaUI)

```kotlin
// TodoApp.magic.kt
magic {
    ui("TodoApp") {
        theme = Themes.Material3Light
        state = TodoAppState()

        // Load data on init
        onInit {
            """
            state.tasks = database.getAllTasks()
            """
        }

        Scaffold {
            topBar = AppBar {
                title = "My Tasks"
                actions {
                    IconButton(icon = "search") {
                        onClick { state.showSearch = !state.showSearch }
                    }
                    IconButton(icon = "sort") {
                        onClick { state.showSortMenu = true }
                    }
                    IconButton(icon = "more_vert") {
                        onClick { state.showMenu = true }
                    }
                }
            }

            floatingActionButton = FAB {
                icon = "add"
                label = "Add Task"
                onClick { state.isAddingTask = true }
            }

            content = Column {
                fillMaxSize()

                // Stats Bar
                StatsBar(stats = state.stats)

                // Filter Tabs
                TabRow(selectedIndex = state.filter.ordinal) {
                    Tab(
                        text = "All (${state.stats.total})",
                        selected = state.filter == FilterType.ALL,
                        onClick { state.filter = FilterType.ALL }
                    )
                    Tab(
                        text = "Active (${state.stats.active})",
                        selected = state.filter == FilterType.ACTIVE,
                        onClick { state.filter = FilterType.ACTIVE }
                    )
                    Tab(
                        text = "Completed (${state.stats.completed})",
                        selected = state.filter == FilterType.COMPLETED,
                        onClick { state.filter = FilterType.COMPLETED }
                    )
                }

                // Category Chips
                if (state.categories.size > 1) {
                    ChipGroup {
                        padding(horizontal = 16f, vertical = 8f)

                        state.categories.forEach { category ->
                            Chip(
                                label = category,
                                selected = state.selectedCategory == category,
                                onClick { state.selectedCategory = category }
                            )
                        }
                    }
                }

                // Search Bar (conditionally shown)
                if (state.showSearch) {
                    SearchBar(
                        query = state.searchQuery,
                        placeholder = "Search tasks...",
                        onQueryChange = { state.searchQuery = it },
                        onClose = {
                            state.showSearch = false
                            state.searchQuery = ""
                        }
                    ) {
                        padding(16f)
                        fillMaxWidth()
                    }
                }

                // Task List
                if (state.filteredTasks.isEmpty()) {
                    EmptyState(
                        icon = when (state.filter) {
                            FilterType.ALL -> "inbox"
                            FilterType.ACTIVE -> "task_alt"
                            FilterType.COMPLETED -> "check_circle"
                        },
                        title = when (state.filter) {
                            FilterType.ALL -> "No tasks yet"
                            FilterType.ACTIVE -> "All caught up!"
                            FilterType.COMPLETED -> "No completed tasks"
                        },
                        message = when (state.filter) {
                            FilterType.ALL -> "Tap + to add your first task"
                            FilterType.ACTIVE -> "You've completed everything!"
                            FilterType.COMPLETED -> "Complete some tasks to see them here"
                        }
                    ) {
                        fillMaxSize()
                        padding(32f)
                    }
                } else {
                    ScrollView {
                        fillMaxSize()

                        Column {
                            padding(bottom = 80f) // Space for FAB

                            state.filteredTasks.forEach { task ->
                                TaskCard(
                                    task = task,
                                    onClick = { state.editingTask = task },
                                    onToggleComplete = {
                                        val updated = task.copy(completed = !task.completed)
                                        database.updateTask(updated)
                                        state.tasks = database.getAllTasks()
                                    },
                                    onDelete = {
                                        database.deleteTask(task.id)
                                        state.tasks = database.getAllTasks()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Task Dialog
        if (state.isAddingTask) {
            AddTaskDialog(
                onDismiss = { state.isAddingTask = false },
                onSave = { title, description, priority, category, dueDate ->
                    val newTask = TodoTask(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        priority = priority,
                        category = category,
                        dueDate = dueDate
                    )
                    database.addTask(newTask)
                    state.tasks = database.getAllTasks()
                    state.isAddingTask = false
                }
            )
        }

        // Edit Task Dialog
        state.editingTask?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = { state.editingTask = null },
                onSave = { updated ->
                    database.updateTask(updated)
                    state.tasks = database.getAllTasks()
                    state.editingTask = null
                },
                onDelete = {
                    database.deleteTask(task.id)
                    state.tasks = database.getAllTasks()
                    state.editingTask = null
                }
            )
        }
    }
}
```

---

## 4. UI Components (AvaUI Components)

### StatsBar Component

```kotlin
magic {
    component("StatsBar") {
        props {
            prop("stats", Type.Object("TodoStats"))
        }

        Card {
            padding(16f)
            margin(16f)
            elevation(2f)

            Row {
                arrangement = Arrangement.SpaceEvenly
                fillMaxWidth()

                StatItem(
                    label = "Total",
                    value = stats.total.toString(),
                    icon = "list",
                    color = Color(0xFF2196F3) // Blue
                )

                Divider {
                    vertical = true
                    height(40f)
                }

                StatItem(
                    label = "Active",
                    value = stats.active.toString(),
                    icon = "pending",
                    color = Color(0xFFFF9800) // Orange
                )

                Divider {
                    vertical = true
                    height(40f)
                }

                StatItem(
                    label = "Done",
                    value = stats.completed.toString(),
                    icon = "check_circle",
                    color = Color(0xFF4CAF50) // Green
                )

                if (stats.overdue > 0) {
                    Divider {
                        vertical = true
                        height(40f)
                    }

                    StatItem(
                        label = "Overdue",
                        value = stats.overdue.toString(),
                        icon = "warning",
                        color = Color(0xFFF44336) // Red
                    )
                }
            }
        }
    }

    component("StatItem") {
        props {
            prop("label", Type.String)
            prop("value", Type.String)
            prop("icon", Type.String)
            prop("color", Type.Color)
        }

        Column {
            horizontalAlignment = Alignment.Center

            Icon(name = icon) {
                tint = color
                size(24f)
            }

            Text(value) {
                font = Font.Title2
                fontWeight = FontWeight.Bold
                color = color
                padding(top = 4f)
            }

            Text(label) {
                font = Font.Caption
                color = Color.Gray
                padding(top = 2f)
            }
        }
    }
}
```

### TaskCard Component

```kotlin
magic {
    component("TaskCard") {
        props {
            prop("task", Type.Object("TodoTask"))
            prop("onClick", Type.Lambda)
            prop("onToggleComplete", Type.Lambda)
            prop("onDelete", Type.Lambda)
        }

        var("showDeleteConfirm", Type.Boolean, default = false)

        Card {
            padding(horizontal = 16f, vertical = 8f)
            margin(horizontal = 16f, vertical = 4f)
            elevation(if (task.completed) 1f else 2f)
            backgroundColor = if (task.completed) Color(0xFFF5F5F5) else Color.White

            onClick { this.onClick() }

            Row {
                verticalAlignment = Alignment.CenterVertically
                fillMaxWidth()

                // Checkbox
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { this.onToggleComplete() }
                ) {
                    tint = task.priorityColor
                }

                Spacer(width = 12f)

                // Content
                Column {
                    fillMaxWidth()
                    weight(1f)

                    // Title
                    Text(task.title) {
                        font = Font.Body
                        fontWeight = FontWeight.Medium
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        color = if (task.completed) Color.Gray else Color.Black
                    }

                    // Description (if exists)
                    if (task.description.isNotEmpty()) {
                        Text(task.description) {
                            font = Font.Caption
                            color = Color.Gray
                            maxLines(2)
                            padding(top = 4f)
                        }
                    }

                    // Metadata row
                    Row {
                        padding(top = 8f)
                        gap(8f)

                        // Priority badge
                        Badge(
                            label = task.priority.name,
                            backgroundColor = task.priorityColor.copy(alpha = 0.1f),
                            textColor = task.priorityColor
                        ) {
                            font = Font.Caption2
                            padding(horizontal = 8f, vertical = 4f)
                        }

                        // Category badge
                        Badge(
                            label = task.category,
                            backgroundColor = Color(0xFFE3F2FD),
                            textColor = Color(0xFF1976D2)
                        ) {
                            font = Font.Caption2
                            padding(horizontal = 8f, vertical = 4f)
                        }

                        // Due date (if exists)
                        task.dueDate?.let { dueDate ->
                            Row {
                                gap(4f)
                                verticalAlignment = Alignment.CenterVertically

                                Icon(name = "calendar_today") {
                                    size(14f)
                                    tint = if (task.isOverdue) Color(0xFFF44336) else Color.Gray
                                }

                                Text(formatDate(dueDate)) {
                                    font = Font.Caption2
                                    color = if (task.isOverdue) Color(0xFFF44336) else Color.Gray
                                }
                            }
                        }
                    }
                }

                // Delete button
                IconButton(icon = "delete") {
                    tint = Color(0xFFF44336)
                    onClick { showDeleteConfirm = true }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirm) {
            AlertDialog(
                title = "Delete Task?",
                message = "Are you sure you want to delete '${task.title}'?",
                confirmText = "Delete",
                cancelText = "Cancel",
                onConfirm = {
                    this.onDelete()
                    showDeleteConfirm = false
                },
                onCancel = { showDeleteConfirm = false }
            )
        }
    }
}
```

### AddTaskDialog Component

```kotlin
magic {
    component("AddTaskDialog") {
        props {
            prop("onDismiss", Type.Lambda)
            prop("onSave", Type.Lambda)
        }

        var("title", Type.String, default = "")
        var("description", Type.String, default = "")
        var("priority", Type.Enum("Priority"), default = "MEDIUM")
        var("category", Type.String, default = "General")
        var("dueDate", Type.Nullable(Type.Long), default = null)
        var("showDatePicker", Type.Boolean, default = false)

        Dialog {
            onDismissRequest { this.onDismiss() }

            Card {
                padding(24f)
                minWidth(400f)

                Column {
                    gap(16f)

                    // Title
                    Text("Add New Task") {
                        font = Font.Title2
                        fontWeight = FontWeight.Bold
                    }

                    Divider()

                    // Title field
                    TextField(
                        value = title,
                        label = "Title",
                        placeholder = "What needs to be done?",
                        onValueChange = { title = it }
                    ) {
                        fillMaxWidth()
                        required = true
                    }

                    // Description field
                    TextField(
                        value = description,
                        label = "Description",
                        placeholder = "Add details...",
                        onValueChange = { description = it }
                    ) {
                        fillMaxWidth()
                        multiline = true
                        minLines(3)
                    }

                    // Priority selector
                    Column {
                        Text("Priority") {
                            font = Font.Caption
                            color = Color.Gray
                            padding(bottom = 8f)
                        }

                        Row {
                            gap(8f)

                            listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                                Chip(
                                    label = p,
                                    selected = priority.name == p,
                                    onClick = { priority = Priority.valueOf(p) }
                                )
                            }
                        }
                    }

                    // Category field
                    TextField(
                        value = category,
                        label = "Category",
                        placeholder = "General",
                        onValueChange = { category = it }
                    ) {
                        fillMaxWidth()
                    }

                    // Due date picker
                    Row {
                        gap(8f)
                        fillMaxWidth()

                        Button("Set Due Date") {
                            buttonStyle = ButtonStyle.Outlined
                            onClick { showDatePicker = true }
                        }

                        dueDate?.let {
                            Text(formatDate(it)) {
                                padding(start = 8f)
                                verticalAlignment = Alignment.Center
                            }

                            IconButton(icon = "clear") {
                                onClick { dueDate = null }
                            }
                        }
                    }

                    Divider()

                    // Actions
                    Row {
                        gap(8f)
                        horizontalAlignment = Alignment.End
                        fillMaxWidth()

                        Button("Cancel") {
                            buttonStyle = ButtonStyle.Text
                            onClick { this.onDismiss() }
                        }

                        Button("Add Task") {
                            buttonStyle = ButtonStyle.Primary
                            enabled = title.isNotEmpty()
                            onClick {
                                this.onSave(title, description, priority, category, dueDate)
                            }
                        }
                    }
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDateSelected = {
                    dueDate = it
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}
```

---

## 5. Database Integration (AvaCode)

```kotlin
// TodoDatabase.magic.kt
magic {
    database("TodoDatabase") {
        // Use Database IPC from Avanues
        provider = "DatabaseIPC"
        collection = "todo_tasks"

        // CRUD operations
        operation("getAllTasks") {
            returns = Type.List("TodoTask")
            implementation = """
                database.getAllDocuments(collection).map { doc ->
                    TodoTask(
                        id = doc.id,
                        title = doc.getString("title"),
                        description = doc.getString("description"),
                        completed = doc.getBoolean("completed"),
                        priority = Priority.valueOf(doc.getString("priority")),
                        category = doc.getString("category"),
                        dueDate = doc.getLong("dueDate"),
                        createdAt = doc.getLong("createdAt")
                    )
                }
            """
        }

        operation("addTask") {
            param("task", Type.Object("TodoTask"))
            returns = Type.Boolean
            implementation = """
                database.insertDocument(collection, mapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "description" to task.description,
                    "completed" to task.completed,
                    "priority" to task.priority.name,
                    "category" to task.category,
                    "dueDate" to task.dueDate,
                    "createdAt" to task.createdAt
                ))
            """
        }

        operation("updateTask") {
            param("task", Type.Object("TodoTask"))
            returns = Type.Boolean
            implementation = """
                database.updateDocument(collection, task.id, mapOf(
                    "title" to task.title,
                    "description" to task.description,
                    "completed" to task.completed,
                    "priority" to task.priority.name,
                    "category" to task.category,
                    "dueDate" to task.dueDate
                ))
            """
        }

        operation("deleteTask") {
            param("id", Type.String)
            returns = Type.Boolean
            implementation = """
                database.deleteDocument(collection, id)
            """
        }

        operation("getTasksByCategory") {
            param("category", Type.String)
            returns = Type.List("TodoTask")
            implementation = """
                getAllTasks().filter { it.category == category }
            """
        }

        operation("getOverdueTasks") {
            returns = Type.List("TodoTask")
            implementation = """
                val now = System.currentTimeMillis()
                getAllTasks().filter { it.dueDate != null && it.dueDate < now && !it.completed }
            """
        }
    }
}
```

---

## 6. Voice Commands Integration (MagicVoice)

```kotlin
// TodoVoiceCommands.magic.kt
magic {
    voice("TodoVoiceCommands") {
        // Integrate with Avanues
        provider = "Avanues"

        command("add task {title}") {
            description = "Add a new task"
            params {
                param("title", Type.String, capture = "rest")
            }
            action = """
                val task = TodoTask(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    priority = Priority.MEDIUM,
                    category = "Voice"
                )
                database.addTask(task)
                speak("Added task: $title")
            """
        }

        command("complete task {title}") {
            description = "Mark a task as complete"
            params {
                param("title", Type.String, capture = "rest")
            }
            action = """
                val tasks = database.getAllTasks().filter {
                    it.title.contains(title, ignoreCase = true) && !it.completed
                }
                if (tasks.isNotEmpty()) {
                    val task = tasks.first().copy(completed = true)
                    database.updateTask(task)
                    speak("Completed: ${task.title}")
                } else {
                    speak("No active task found matching: $title")
                }
            """
        }

        command("what tasks do i have") {
            description = "List all active tasks"
            action = """
                val active = database.getAllTasks().filter { !it.completed }
                if (active.isEmpty()) {
                    speak("You have no active tasks. Great job!")
                } else {
                    speak("You have ${active.size} active tasks:")
                    active.take(5).forEach { task ->
                        speak("${task.priority.name} priority: ${task.title}")
                    }
                }
            """
        }

        command("what's due today") {
            description = "List tasks due today"
            action = """
                val today = Date.today()
                val dueToday = database.getAllTasks().filter {
                    it.dueDate?.let { Date(it).isToday() } == true && !it.completed
                }
                if (dueToday.isEmpty()) {
                    speak("No tasks due today")
                } else {
                    speak("${dueToday.size} tasks due today:")
                    dueToday.forEach { task ->
                        speak(task.title)
                    }
                }
            """
        }
    }
}
```

---

## 7. How to Build and Run

### Step 1: Create the Magic files

```bash
# Create project structure
mkdir -p src/magic/
cd src/magic/

# Create all .magic.kt files shown above
touch TodoModels.magic.kt
touch TodoState.magic.kt
touch TodoApp.magic.kt
touch TodoComponents.magic.kt
touch TodoDatabase.magic.kt
touch TodoVoiceCommands.magic.kt
```

### Step 2: Run IDEAMagic Code Generator

```bash
# Generate platform-specific code
avamagic generate --platform ios --output ../ios/
avamagic generate --platform android --output ../android/
avamagic generate --platform web --output ../web/
```

### Step 3: Build for Each Platform

**iOS:**
```bash
cd ../ios/
xcodebuild -scheme TodoApp build
```

**Android:**
```bash
cd ../android/
./gradlew assembleDebug
```

**Web:**
```bash
cd ../web/
npm run build
```

---

## Summary

This is the **actual IDEAMagic DSL code** you would write. It includes:

1. **AvaCode** for data models with computed properties
2. **MagicState** for reactive state management
3. **AvaUI** for declarative UI components
4. **MagicDatabase** for database operations (using Database IPC)
5. **MagicVoice** for voice command integration

**Key Benefits:**
- ✅ Write once in IDEAMagic DSL
- ✅ Generate native code for iOS, Android, Web
- ✅ Reuse Database IPC infrastructure
- ✅ Integrate with Avanues speech recognition
- ✅ Type-safe with compile-time checks
- ✅ Reactive state management built-in
- ✅ Platform-specific optimizations automatic

The code generator (`avamagic`) translates this DSL into native Swift, Kotlin, and JavaScript/TypeScript code that's indistinguishable from hand-written code.

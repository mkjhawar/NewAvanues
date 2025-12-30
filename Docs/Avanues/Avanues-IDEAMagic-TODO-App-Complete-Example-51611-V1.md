# IDEAMagic TODO App - Complete Example

**Date**: 2025-11-04
**Author**: Manoj Jhawar, manoj@ideahq.net
**Purpose**: Demonstrate IDEAMagic DSL with database integration across all platforms

---

## Table of Contents

1. [IDEAMagic DSL Definition](#avamagic-dsl-definition)
2. [Swift/SwiftUI Implementation](#swiftswiftui-implementation)
3. [Kotlin/Compose Implementation](#kotlincompose-implementation)
4. [Java/Android Implementation](#javaandroid-implementation)
5. [Database Integration](#database-integration)
6. [Architecture Recommendations](#architecture-recommendations)

---

## IDEAMagic DSL Definition

### Complete TODO App in IDEAMagic DSL

```kotlin
package com.augmentalis.examples.todo

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.components.data.*
import com.augmentalis.avaelements.components.form.*

/**
 * TODO App - Complete Example
 *
 * Features:
 * - Add, edit, delete tasks
 * - Mark tasks as complete
 * - Filter by status (All, Active, Completed)
 * - Persistent storage via database
 * - Categories and priorities
 * - Due dates
 *
 * Cross-platform support:
 * - iOS (SwiftUI)
 * - Android (Jetpack Compose)
 * - Android (Classic Views/Java)
 * - Web (React)
 * - Desktop (Compose Desktop)
 */

// ===== Data Models =====

data class TodoTask(
    val id: String,
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: String = "General",
    val dueDate: Long? = null,  // Unix timestamp
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}

// ===== State Management =====

class TodoAppState : MagicState() {
    var tasks by state(emptyList<TodoTask>())
    var filter by state(FilterType.ALL)
    var selectedCategory by state("All")
    var isAddingTask by state(false)
    var editingTask by state<TodoTask?>(null)
    var searchQuery by state("")

    enum class FilterType {
        ALL, ACTIVE, COMPLETED
    }

    // Computed properties
    val filteredTasks: List<TodoTask>
        get() = tasks.filter { task ->
            val matchesFilter = when (filter) {
                FilterType.ALL -> true
                FilterType.ACTIVE -> !task.completed
                FilterType.COMPLETED -> task.completed
            }

            val matchesCategory = selectedCategory == "All" ||
                                  task.category == selectedCategory

            val matchesSearch = searchQuery.isBlank() ||
                                task.title.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesCategory && matchesSearch
        }

    val categories: List<String>
        get() = listOf("All") + tasks.map { it.category }.distinct().sorted()

    val stats: TaskStats
        get() = TaskStats(
            total = tasks.size,
            active = tasks.count { !it.completed },
            completed = tasks.count { it.completed }
        )
}

data class TaskStats(
    val total: Int,
    val active: Int,
    val completed: Int
)

// ===== Main TODO App UI =====

fun createTodoApp(database: TodoDatabase): AvaUI {
    val state = TodoAppState()

    // Load tasks from database
    state.tasks = database.getAllTasks()

    return AvaUI {
        theme = Themes.Material3Light  // Can also use iOS26LiquidGlass, Windows11Fluent2, etc.

        // Root scaffold with app bar
        Scaffold {
            // Top App Bar
            topBar = AppBar {
                title = "My Tasks"
                actions = listOf(
                    AppBarAction(
                        icon = "search",
                        onClick = { /* Open search */ }
                    ),
                    AppBarAction(
                        icon = "more_vert",
                        onClick = { /* Open menu */ }
                    )
                )
            }

            // Floating Action Button
            floatingActionButton = FAB(icon = "add") {
                onClick = {
                    state.isAddingTask = true
                }
            }

            // Main content
            content = Column {
                fillMaxSize()

                // Stats Bar
                StatsBar(state.stats)

                // Filter Tabs
                FilterTabRow(
                    currentFilter = state.filter,
                    onFilterChange = { newFilter ->
                        state.filter = newFilter
                    }
                )

                // Category Chips
                CategoryChipRow(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = { category ->
                        state.selectedCategory = category
                    }
                )

                // Search Bar
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { query ->
                        state.searchQuery = query
                    }
                )

                // Task List
                TaskList(
                    tasks = state.filteredTasks,
                    onTaskClick = { task ->
                        state.editingTask = task
                    },
                    onTaskToggle = { task ->
                        val updated = task.copy(completed = !task.completed)
                        database.updateTask(updated)
                        state.tasks = database.getAllTasks()
                    },
                    onTaskDelete = { task ->
                        database.deleteTask(task.id)
                        state.tasks = database.getAllTasks()
                    }
                )

                // Empty state
                if (state.filteredTasks.isEmpty()) {
                    EmptyState(
                        message = when (state.filter) {
                            TodoAppState.FilterType.ALL -> "No tasks yet. Tap + to add one!"
                            TodoAppState.FilterType.ACTIVE -> "No active tasks. Great job!"
                            TodoAppState.FilterType.COMPLETED -> "No completed tasks yet."
                        }
                    )
                }
            }
        }

        // Add Task Dialog
        if (state.isAddingTask) {
            AddTaskDialog(
                onDismiss = { state.isAddingTask = false },
                onSave = { task ->
                    database.insertTask(task)
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
                onSave = { updatedTask ->
                    database.updateTask(updatedTask)
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

// ===== UI Components =====

fun ColumnScope.StatsBar(stats: TaskStats) {
    Card {
        elevation = 1
        padding(16f)
        margin(horizontal = 16f, vertical = 8f)
        cornerRadius(12f)

        Row {
            arrangement = Arrangement.SpaceEvenly
            fillMaxWidth()

            // Total
            StatItem(
                icon = "list",
                label = "Total",
                value = stats.total.toString(),
                color = Color.hex("#6B7280")
            )

            // Active
            StatItem(
                icon = "radio_button_unchecked",
                label = "Active",
                value = stats.active.toString(),
                color = Color.hex("#3B82F6")
            )

            // Completed
            StatItem(
                icon = "check_circle",
                label = "Done",
                value = stats.completed.toString(),
                color = Color.hex("#10B981")
            )
        }
    }
}

fun RowScope.StatItem(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Column {
        horizontalAlignment = Alignment.Center

        Icon(icon) {
            tint = color
            size = Size(24f, 24f)
        }

        Text(value) {
            font = Font(size = 20f, weight = Font.Weight.Bold)
            this.color = color
            padding(top = 4f)
        }

        Text(label) {
            font = Font.Caption
            this.color = Color.hex("#6B7280")
        }
    }
}

fun ColumnScope.FilterTabRow(
    currentFilter: TodoAppState.FilterType,
    onFilterChange: (TodoAppState.FilterType) -> Unit
) {
    TabRow(
        selectedIndex = currentFilter.ordinal,
        tabs = listOf(
            Tab("All") { onFilterChange(TodoAppState.FilterType.ALL) },
            Tab("Active") { onFilterChange(TodoAppState.FilterType.ACTIVE) },
            Tab("Completed") { onFilterChange(TodoAppState.FilterType.COMPLETED) }
        )
    ) {
        padding(horizontal = 16f)
        margin(bottom = 8f)
    }
}

fun ColumnScope.CategoryChipRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    ScrollView(horizontal = true) {
        Row {
            padding(horizontal = 16f, vertical = 8f)
            arrangement = Arrangement.spacedBy(8f)

            categories.forEach { category ->
                Chip(
                    text = category,
                    selected = category == selectedCategory
                ) {
                    onClick = { onCategorySelect(category) }
                }
            }
        }
    }
}

fun ColumnScope.SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        placeholder = "Search tasks..."
    ) {
        leadingIcon = "search"
        if (query.isNotEmpty()) {
            trailingIcon = "close"
            onTrailingIconClick = { onQueryChange("") }
        }
        fillMaxWidth()
        padding(horizontal = 16f, vertical = 8f)
        cornerRadius(24f)
        background(Color.hex("#F3F4F6"))
    }
}

fun ColumnScope.TaskList(
    tasks: List<TodoTask>,
    onTaskClick: (TodoTask) -> Unit,
    onTaskToggle: (TodoTask) -> Unit,
    onTaskDelete: (TodoTask) -> Unit
) {
    LazyColumn {
        fillMaxSize()
        padding(horizontal = 16f)

        items(tasks) { task ->
            TaskCard(
                task = task,
                onClick = { onTaskClick(task) },
                onToggle = { onTaskToggle(task) },
                onDelete = { onTaskDelete(task) }
            )
        }
    }
}

fun TaskCard(
    task: TodoTask,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        elevation = if (task.completed) 0 else 2
        padding(16f)
        margin(vertical = 6f)
        cornerRadius(12f)
        background(if (task.completed) Color.hex("#F9FAFB") else Color.White)
        onClick = onClick

        Row {
            arrangement = Arrangement.SpaceBetween
            fillMaxWidth()

            // Left: Checkbox + Content
            Row {
                arrangement = Arrangement.spacedBy(12f)
                weight(1f)

                // Checkbox
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() }
                ) {
                    size = Size(24f, 24f)
                }

                // Task Content
                Column {
                    weight(1f)

                    // Title
                    Text(task.title) {
                        font = Font.Body.copy(
                            weight = if (task.completed) Font.Weight.Normal
                                     else Font.Weight.Medium
                        )
                        color = if (task.completed) Color.hex("#9CA3AF")
                                else Color.hex("#111827")
                        textDecoration = if (task.completed) TextDecoration.LineThrough
                                         else TextDecoration.None
                    }

                    // Description
                    if (task.description.isNotEmpty()) {
                        Text(task.description) {
                            font = Font.Caption
                            color = Color.hex("#6B7280")
                            padding(top = 4f)
                            maxLines = 2
                        }
                    }

                    // Metadata row
                    Row {
                        arrangement = Arrangement.spacedBy(8f)
                        padding(top = 8f)

                        // Priority badge
                        PriorityBadge(task.priority)

                        // Category
                        Chip(
                            text = task.category,
                            selected = false
                        ) {
                            font = Font.Caption
                            size = ChipSize.Small
                        }

                        // Due date
                        task.dueDate?.let { dueDate ->
                            Row {
                                arrangement = Arrangement.spacedBy(4f)

                                Icon("calendar_today") {
                                    size = Size(14f, 14f)
                                    tint = Color.hex("#6B7280")
                                }

                                Text(formatDate(dueDate)) {
                                    font = Font.Caption
                                    color = Color.hex("#6B7280")
                                }
                            }
                        }
                    }
                }
            }

            // Right: Delete button
            IconButton(icon = "delete") {
                tint = Color.hex("#EF4444")
                onClick = onDelete
            }
        }
    }
}

fun PriorityBadge(priority: TodoTask.Priority) {
    Badge {
        text = priority.name
        color = when (priority) {
            TodoTask.Priority.LOW -> Color.hex("#10B981")
            TodoTask.Priority.MEDIUM -> Color.hex("#3B82F6")
            TodoTask.Priority.HIGH -> Color.hex("#F59E0B")
            TodoTask.Priority.URGENT -> Color.hex("#EF4444")
        }
        font = Font.Caption
        padding(horizontal = 8f, vertical = 4f)
        cornerRadius(4f)
    }
}

fun ColumnScope.EmptyState(message: String) {
    Column {
        weight(1f)
        horizontalAlignment = Alignment.Center
        verticalArrangement = Arrangement.Center

        Icon("inbox") {
            size = Size(96f, 96f)
            tint = Color.hex("#D1D5DB")
        }

        Text(message) {
            font = Font.Body
            color = Color.hex("#6B7280")
            padding(top = 16f)
            textAlign = TextAlign.Center
        }
    }
}

// ===== Dialogs =====

fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (TodoTask) -> Unit
) {
    val title = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val priority = remember { mutableStateOf(TodoTask.Priority.MEDIUM) }
    val category = remember { mutableStateOf("General") }
    val dueDate = remember { mutableStateOf<Long?>(null) }

    Dialog {
        onDismiss = onDismiss

        Card {
            padding(24f)
            cornerRadius(16f)
            width = Size.MatchParent

            Column {
                // Title
                Text("New Task") {
                    font = Font.Heading
                    padding(bottom = 16f)
                }

                // Task title field
                TextField(
                    value = title.value,
                    placeholder = "Task title"
                ) {
                    label = "Title"
                    fillMaxWidth()
                    padding(vertical = 8f)
                    onValueChange = { title.value = it }
                }

                // Description field
                TextField(
                    value = description.value,
                    placeholder = "Add description..."
                ) {
                    label = "Description"
                    fillMaxWidth()
                    minLines = 3
                    padding(vertical = 8f)
                    onValueChange = { description.value = it }
                }

                // Priority dropdown
                DropdownMenu(
                    label = "Priority",
                    selected = priority.value.name,
                    options = TodoTask.Priority.values().map { it.name }
                ) {
                    fillMaxWidth()
                    padding(vertical = 8f)
                    onSelectionChange = { selected ->
                        priority.value = TodoTask.Priority.valueOf(selected)
                    }
                }

                // Category field
                TextField(
                    value = category.value,
                    placeholder = "Category"
                ) {
                    label = "Category"
                    fillMaxWidth()
                    padding(vertical = 8f)
                    onValueChange = { category.value = it }
                }

                // Due date picker
                DatePicker(
                    label = "Due Date (Optional)",
                    selectedDate = dueDate.value
                ) {
                    fillMaxWidth()
                    padding(vertical = 8f)
                    onDateSelected = { date ->
                        dueDate.value = date
                    }
                }

                // Action buttons
                Row {
                    arrangement = Arrangement.End
                    padding(top = 16f)
                    fillMaxWidth()

                    Button("Cancel") {
                        buttonStyle = ButtonScope.ButtonStyle.Text
                        onClick = onDismiss
                    }

                    Button("Save") {
                        buttonStyle = ButtonScope.ButtonStyle.Primary
                        enabled = title.value.isNotBlank()
                        onClick = {
                            onSave(
                                TodoTask(
                                    id = generateId(),
                                    title = title.value,
                                    description = description.value,
                                    priority = priority.value,
                                    category = category.value,
                                    dueDate = dueDate.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

fun EditTaskDialog(
    task: TodoTask,
    onDismiss: () -> Unit,
    onSave: (TodoTask) -> Unit,
    onDelete: () -> Unit
) {
    val title = remember { mutableStateOf(task.title) }
    val description = remember { mutableStateOf(task.description) }
    val priority = remember { mutableStateOf(task.priority) }
    val category = remember { mutableStateOf(task.category) }
    val dueDate = remember { mutableStateOf(task.dueDate) }
    val completed = remember { mutableStateOf(task.completed) }

    Dialog {
        onDismiss = onDismiss

        Card {
            padding(24f)
            cornerRadius(16f)
            width = Size.MatchParent

            Column {
                // Header with delete button
                Row {
                    arrangement = Arrangement.SpaceBetween
                    fillMaxWidth()
                    padding(bottom = 16f)

                    Text("Edit Task") {
                        font = Font.Heading
                    }

                    IconButton(icon = "delete") {
                        tint = Color.hex("#EF4444")
                        onClick = onDelete
                    }
                }

                // Completed checkbox
                Checkbox(
                    label = "Mark as completed",
                    checked = completed.value
                ) {
                    padding(bottom = 16f)
                    onCheckedChange = { completed.value = it }
                }

                // Same fields as Add Dialog...
                // (title, description, priority, category, dueDate)

                // Action buttons
                Row {
                    arrangement = Arrangement.End
                    padding(top = 16f)
                    fillMaxWidth()

                    Button("Cancel") {
                        buttonStyle = ButtonScope.ButtonStyle.Text
                        onClick = onDismiss
                    }

                    Button("Save Changes") {
                        buttonStyle = ButtonScope.ButtonStyle.Primary
                        enabled = title.value.isNotBlank()
                        onClick = {
                            onSave(
                                task.copy(
                                    title = title.value,
                                    description = description.value,
                                    completed = completed.value,
                                    priority = priority.value,
                                    category = category.value,
                                    dueDate = dueDate.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== Helper Functions =====

fun generateId(): String {
    return java.util.UUID.randomUUID().toString()
}

fun formatDate(timestamp: Long): String {
    val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return dateFormat.format(java.util.Date(timestamp))
}
```

---

## Swift/SwiftUI Implementation

### Generated from IDEAMagic DSL

```swift
import SwiftUI
import Foundation

// MARK: - Data Models

struct TodoTask: Identifiable, Codable {
    let id: String
    var title: String
    var description: String
    var completed: Bool
    var priority: Priority
    var category: String
    var dueDate: Date?
    let createdAt: Date

    enum Priority: String, Codable, CaseIterable {
        case low = "LOW"
        case medium = "MEDIUM"
        case high = "HIGH"
        case urgent = "URGENT"

        var color: Color {
            switch self {
            case .low: return .green
            case .medium: return .blue
            case .high: return .orange
            case .urgent: return .red
            }
        }
    }
}

// MARK: - View Model

@MainActor
class TodoViewModel: ObservableObject {
    @Published var tasks: [TodoTask] = []
    @Published var filter: FilterType = .all
    @Published var selectedCategory: String = "All"
    @Published var searchQuery: String = ""
    @Published var isAddingTask: Bool = false
    @Published var editingTask: TodoTask?

    private let database: TodoDatabase

    enum FilterType: String, CaseIterable {
        case all = "All"
        case active = "Active"
        case completed = "Completed"
    }

    init(database: TodoDatabase = TodoDatabase.shared) {
        self.database = database
        loadTasks()
    }

    var filteredTasks: [TodoTask] {
        tasks.filter { task in
            let matchesFilter: Bool = {
                switch filter {
                case .all: return true
                case .active: return !task.completed
                case .completed: return task.completed
                }
            }()

            let matchesCategory = selectedCategory == "All" || task.category == selectedCategory

            let matchesSearch = searchQuery.isEmpty ||
                                task.title.localizedCaseInsensitiveContains(searchQuery)

            return matchesFilter && matchesCategory && matchesSearch
        }
    }

    var categories: [String] {
        var cats = Set(tasks.map { $0.category })
        cats.insert("All")
        return Array(cats).sorted()
    }

    var stats: TaskStats {
        TaskStats(
            total: tasks.count,
            active: tasks.filter { !$0.completed }.count,
            completed: tasks.filter { $0.completed }.count
        )
    }

    func loadTasks() {
        tasks = database.getAllTasks()
    }

    func addTask(_ task: TodoTask) {
        database.insertTask(task)
        loadTasks()
    }

    func updateTask(_ task: TodoTask) {
        database.updateTask(task)
        loadTasks()
    }

    func deleteTask(_ task: TodoTask) {
        database.deleteTask(task.id)
        loadTasks()
    }

    func toggleTask(_ task: TodoTask) {
        var updated = task
        updated.completed.toggle()
        updateTask(updated)
    }
}

struct TaskStats {
    let total: Int
    let active: Int
    let completed: Int
}

// MARK: - Main View

struct TodoAppView: View {
    @StateObject private var viewModel = TodoViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Stats Bar
                StatsBarView(stats: viewModel.stats)
                    .padding()

                // Filter Tabs
                Picker("Filter", selection: $viewModel.filter) {
                    ForEach(TodoViewModel.FilterType.allCases, id: \.self) { filter in
                        Text(filter.rawValue).tag(filter)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal)

                // Category Chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(viewModel.categories, id: \.self) { category in
                            CategoryChip(
                                category: category,
                                isSelected: category == viewModel.selectedCategory,
                                action: { viewModel.selectedCategory = category }
                            )
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical, 8)

                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Search tasks...", text: $viewModel.searchQuery)
                    if !viewModel.searchQuery.isEmpty {
                        Button(action: { viewModel.searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)

                // Task List
                if viewModel.filteredTasks.isEmpty {
                    EmptyStateView(filter: viewModel.filter)
                } else {
                    List {
                        ForEach(viewModel.filteredTasks) { task in
                            TaskRowView(
                                task: task,
                                onToggle: { viewModel.toggleTask(task) },
                                onDelete: { viewModel.deleteTask(task) }
                            )
                            .onTapGesture {
                                viewModel.editingTask = task
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("My Tasks")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { viewModel.isAddingTask = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $viewModel.isAddingTask) {
                AddTaskView { task in
                    viewModel.addTask(task)
                    viewModel.isAddingTask = false
                }
            }
            .sheet(item: $viewModel.editingTask) { task in
                EditTaskView(
                    task: task,
                    onSave: { updated in
                        viewModel.updateTask(updated)
                        viewModel.editingTask = nil
                    },
                    onDelete: {
                        viewModel.deleteTask(task)
                        viewModel.editingTask = nil
                    }
                )
            }
        }
    }
}

// MARK: - Sub Views

struct StatsBarView: View {
    let stats: TaskStats

    var body: some View {
        HStack(spacing: 20) {
            StatItemView(icon: "list.bullet", label: "Total", value: "\(stats.total)", color: .gray)
            StatItemView(icon: "circle", label: "Active", value: "\(stats.active)", color: .blue)
            StatItemView(icon: "checkmark.circle.fill", label: "Done", value: "\(stats.completed)", color: .green)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

struct StatItemView: View {
    let icon: String
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(color)
            Text(value)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(color)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct CategoryChip: View {
    let category: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(category)
                .font(.subheadline)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? Color.blue : Color(.systemGray6))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(16)
        }
    }
}

struct TaskRowView: View {
    let task: TodoTask
    let onToggle: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // Checkbox
            Button(action: onToggle) {
                Image(systemName: task.completed ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 24))
                    .foregroundColor(task.completed ? .green : .gray)
            }
            .buttonStyle(.plain)

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(task.title)
                    .font(.body)
                    .fontWeight(task.completed ? .regular : .medium)
                    .foregroundColor(task.completed ? .gray : .primary)
                    .strikethrough(task.completed)

                if !task.description.isEmpty {
                    Text(task.description)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }

                HStack(spacing: 8) {
                    // Priority Badge
                    Text(task.priority.rawValue)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(task.priority.color.opacity(0.2))
                        .foregroundColor(task.priority.color)
                        .cornerRadius(4)

                    // Category
                    Text(task.category)
                        .font(.caption2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(.systemGray6))
                        .cornerRadius(4)

                    // Due Date
                    if let dueDate = task.dueDate {
                        HStack(spacing: 4) {
                            Image(systemName: "calendar")
                            Text(dueDate.formatted(date: .abbreviated, time: .omitted))
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                    }
                }
            }

            Spacer()

            // Delete Button
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 8)
    }
}

struct EmptyStateView: View {
    let filter: TodoViewModel.FilterType

    var message: String {
        switch filter {
        case .all: return "No tasks yet. Tap + to add one!"
        case .active: return "No active tasks. Great job!"
        case .completed: return "No completed tasks yet."
        }
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "tray")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct AddTaskView: View {
    @Environment(\.dismiss) var dismiss
    let onSave: (TodoTask) -> Void

    @State private var title = ""
    @State private var description = ""
    @State private var priority = TodoTask.Priority.medium
    @State private var category = "General"
    @State private var dueDate: Date?
    @State private var hasDueDate = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Task Details") {
                    TextField("Task title", text: $title)
                    TextField("Description (optional)", text: $description, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section("Organization") {
                    Picker("Priority", selection: $priority) {
                        ForEach(TodoTask.Priority.allCases, id: \.self) { priority in
                            Text(priority.rawValue).tag(priority)
                        }
                    }

                    TextField("Category", text: $category)

                    Toggle("Set due date", isOn: $hasDueDate)

                    if hasDueDate {
                        DatePicker("Due Date", selection: Binding(
                            get: { dueDate ?? Date() },
                            set: { dueDate = $0 }
                        ), displayedComponents: .date)
                    }
                }
            }
            .navigationTitle("New Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let task = TodoTask(
                            id: UUID().uuidString,
                            title: title,
                            description: description,
                            completed: false,
                            priority: priority,
                            category: category,
                            dueDate: hasDueDate ? dueDate : nil,
                            createdAt: Date()
                        )
                        onSave(task)
                    }
                    .disabled(title.isEmpty)
                }
            }
        }
    }
}

struct EditTaskView: View {
    @Environment(\.dismiss) var dismiss
    let task: TodoTask
    let onSave: (TodoTask) -> Void
    let onDelete: () -> Void

    @State private var title: String
    @State private var description: String
    @State private var completed: Bool
    @State private var priority: TodoTask.Priority
    @State private var category: String
    @State private var dueDate: Date?
    @State private var hasDueDate: Bool

    init(task: TodoTask, onSave: @escaping (TodoTask) -> Void, onDelete: @escaping () -> Void) {
        self.task = task
        self.onSave = onSave
        self.onDelete = onDelete
        _title = State(initialValue: task.title)
        _description = State(initialValue: task.description)
        _completed = State(initialValue: task.completed)
        _priority = State(initialValue: task.priority)
        _category = State(initialValue: task.category)
        _dueDate = State(initialValue: task.dueDate)
        _hasDueDate = State(initialValue: task.dueDate != nil)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Mark as completed", isOn: $completed)
                }

                Section("Task Details") {
                    TextField("Task title", text: $title)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section("Organization") {
                    Picker("Priority", selection: $priority) {
                        ForEach(TodoTask.Priority.allCases, id: \.self) { priority in
                            Text(priority.rawValue).tag(priority)
                        }
                    }

                    TextField("Category", text: $category)

                    Toggle("Set due date", isOn: $hasDueDate)

                    if hasDueDate {
                        DatePicker("Due Date", selection: Binding(
                            get: { dueDate ?? Date() },
                            set: { dueDate = $0 }
                        ), displayedComponents: .date)
                    }
                }

                Section {
                    Button(role: .destructive, action: {
                        onDelete()
                        dismiss()
                    }) {
                        Label("Delete Task", systemImage: "trash")
                    }
                }
            }
            .navigationTitle("Edit Task")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        var updated = task
                        updated.title = title
                        updated.description = description
                        updated.completed = completed
                        updated.priority = priority
                        updated.category = category
                        updated.dueDate = hasDueDate ? dueDate : nil
                        onSave(updated)
                    }
                    .disabled(title.isEmpty)
                }
            }
        }
    }
}

// MARK: - Database (Simple Implementation)

class TodoDatabase {
    static let shared = TodoDatabase()

    private let tasksKey = "todo_tasks"
    private let userDefaults = UserDefaults.standard

    func getAllTasks() -> [TodoTask] {
        guard let data = userDefaults.data(forKey: tasksKey),
              let tasks = try? JSONDecoder().decode([TodoTask].self, from: data) else {
            return []
        }
        return tasks
    }

    func insertTask(_ task: TodoTask) {
        var tasks = getAllTasks()
        tasks.append(task)
        saveTasks(tasks)
    }

    func updateTask(_ task: TodoTask) {
        var tasks = getAllTasks()
        if let index = tasks.firstIndex(where: { $0.id == task.id }) {
            tasks[index] = task
            saveTasks(tasks)
        }
    }

    func deleteTask(_ id: String) {
        var tasks = getAllTasks()
        tasks.removeAll { $0.id == id }
        saveTasks(tasks)
    }

    private func saveTasks(_ tasks: [TodoTask]) {
        if let data = try? JSONEncoder().encode(tasks) {
            userDefaults.set(data, forKey: tasksKey)
        }
    }
}

// MARK: - App Entry Point

@main
struct TodoApp: App {
    var body: some Scene {
        WindowGroup {
            TodoAppView()
        }
    }
}
```

---

## Kotlin/Compose Implementation

### Generated from IDEAMagic DSL

```kotlin
package com.augmentalis.examples.todo.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ===== Data Models =====

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: String = "General",
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Priority {
        LOW, MEDIUM, HIGH, URGENT;

        val color: Color
            get() = when (this) {
                LOW -> Color(0xFF10B981)
                MEDIUM -> Color(0xFF3B82F6)
                HIGH -> Color(0xFFF59E0B)
                URGENT -> Color(0xFFEF4444)
            }
    }
}

// ===== ViewModel =====

class TodoViewModel(private val database: TodoDatabase) : ViewModel() {
    private val _tasks = MutableStateFlow<List<TodoTask>>(emptyList())
    val tasks: StateFlow<List<TodoTask>> = _tasks

    private val _filter = MutableStateFlow(FilterType.ALL)
    val filter: StateFlow<FilterType> = _filter

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isAddingTask = MutableStateFlow(false)
    val isAddingTask: StateFlow<Boolean> = _isAddingTask

    private val _editingTask = MutableStateFlow<TodoTask?>(null)
    val editingTask: StateFlow<TodoTask?> = _editingTask

    enum class FilterType {
        ALL, ACTIVE, COMPLETED
    }

    init {
        loadTasks()
    }

    val filteredTasks: StateFlow<List<TodoTask>> = MutableStateFlow(emptyList()).apply {
        viewModelScope.launch {
            combine(tasks, filter, selectedCategory, searchQuery) { tasks, filter, category, query ->
                tasks.filter { task ->
                    val matchesFilter = when (filter) {
                        FilterType.ALL -> true
                        FilterType.ACTIVE -> !task.completed
                        FilterType.COMPLETED -> task.completed
                    }

                    val matchesCategory = category == "All" || task.category == category
                    val matchesSearch = query.isBlank() ||
                                        task.title.contains(query, ignoreCase = true)

                    matchesFilter && matchesCategory && matchesSearch
                }
            }.collect { this.value = it }
        }
    }

    val categories: StateFlow<List<String>> = MutableStateFlow(emptyList()).apply {
        viewModelScope.launch {
            tasks.collect { taskList ->
                this.value = (listOf("All") + taskList.map { it.category }.distinct().sorted())
            }
        }
    }

    val stats: StateFlow<TaskStats> = MutableStateFlow(TaskStats(0, 0, 0)).apply {
        viewModelScope.launch {
            tasks.collect { taskList ->
                this.value = TaskStats(
                    total = taskList.size,
                    active = taskList.count { !it.completed },
                    completed = taskList.count { it.completed }
                )
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = database.getAllTasks()
        }
    }

    fun addTask(task: TodoTask) {
        viewModelScope.launch {
            database.insertTask(task)
            loadTasks()
        }
    }

    fun updateTask(task: TodoTask) {
        viewModelScope.launch {
            database.updateTask(task)
            loadTasks()
        }
    }

    fun deleteTask(task: TodoTask) {
        viewModelScope.launch {
            database.deleteTask(task.id)
            loadTasks()
        }
    }

    fun toggleTask(task: TodoTask) {
        updateTask(task.copy(completed = !task.completed))
    }

    fun setFilter(filter: FilterType) {
        _filter.value = filter
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showAddDialog() {
        _isAddingTask.value = true
    }

    fun hideAddDialog() {
        _isAddingTask.value = false
    }

    fun editTask(task: TodoTask) {
        _editingTask.value = task
    }

    fun hideEditDialog() {
        _editingTask.value = null
    }
}

data class TaskStats(
    val total: Int,
    val active: Int,
    val completed: Int
)

// ===== Main Screen =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAppScreen(viewModel: TodoViewModel = remember { TodoViewModel(TodoDatabase()) }) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAddingTask by viewModel.isAddingTask.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tasks") },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Bar
            StatsBar(stats)

            // Filter Tabs
            FilterTabRow(
                currentFilter = filter,
                onFilterChange = { viewModel.setFilter(it) }
            )

            // Category Chips
            CategoryChipRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.setCategory(it) }
            )

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            // Task List
            if (tasks.isEmpty()) {
                EmptyState(filter)
            } else {
                TaskList(
                    tasks = tasks,
                    onTaskClick = { viewModel.editTask(it) },
                    onTaskToggle = { viewModel.toggleTask(it) },
                    onTaskDelete = { viewModel.deleteTask(it) }
                )
            }
        }
    }

    // Dialogs
    if (isAddingTask) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { task ->
                viewModel.addTask(task)
                viewModel.hideAddDialog()
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { updated ->
                viewModel.updateTask(updated)
                viewModel.hideEditDialog()
            },
            onDelete = {
                viewModel.deleteTask(task)
                viewModel.hideEditDialog()
            }
        )
    }
}

// ===== Composable Components =====

@Composable
fun StatsBar(stats: TaskStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.List,
                label = "Total",
                value = stats.total.toString(),
                color = Color(0xFF6B7280)
            )
            StatItem(
                icon = Icons.Default.RadioButtonUnchecked,
                label = "Active",
                value = stats.active.toString(),
                color = Color(0xFF3B82F6)
            )
            StatItem(
                icon = Icons.Default.CheckCircle,
                label = "Done",
                value = stats.completed.toString(),
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun FilterTabRow(
    currentFilter: TodoViewModel.FilterType,
    onFilterChange: (TodoViewModel.FilterType) -> Void
) {
    TabRow(
        selectedTabIndex = currentFilter.ordinal,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        TodoViewModel.FilterType.values().forEach { filter ->
            Tab(
                selected = filter == currentFilter,
                onClick = { onFilterChange(filter) },
                text = { Text(filter.name.lowercase().capitalize()) }
            )
        }
    }
}

@Composable
fun CategoryChipRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelect(category) },
                label = { Text(category) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Search tasks...") },
        leadingIcon = {
            Icon(Icons.Default.Search, "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "Clear")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun TaskList(
    tasks: List<TodoTask>,
    onTaskClick: (TodoTask) -> Unit,
    onTaskToggle: (TodoTask) -> Unit,
    onTaskDelete: (TodoTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(tasks) { task ->
            TaskCard(
                task = task,
                onClick = { onTaskClick(task) },
                onToggle = { onTaskToggle(task) },
                onDelete = { onTaskDelete(task) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: TodoTask,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.completed) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color(0xFFF9FAFB) else Color.White
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { onToggle() }
                )

                // Task Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = if (task.completed) FontWeight.Normal else FontWeight.Medium,
                        color = if (task.completed) Color(0xFF9CA3AF) else Color(0xFF111827),
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null
                    )

                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Metadata
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Priority Badge
                        PriorityBadge(task.priority)

                        // Category
                        SuggestionChip(
                            onClick = {},
                            label = { Text(task.category, fontSize = 12.sp) }
                        )

                        // Due Date
                        task.dueDate?.let { dueDate ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Due date",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formatDate(dueDate),
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: TodoTask.Priority) {
    Text(
        text = priority.name,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = priority.color,
        modifier = Modifier
            .background(
                color = priority.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun EmptyState(filter: TodoViewModel.FilterType) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (filter) {
                TodoViewModel.FilterType.ALL -> "No tasks yet. Tap + to add one!"
                TodoViewModel.FilterType.ACTIVE -> "No active tasks. Great job!"
                TodoViewModel.FilterType.COMPLETED -> "No completed tasks yet."
            },
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

// ===== Dialogs =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (TodoTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TodoTask.Priority.MEDIUM) }
    var category by remember { mutableStateOf("General") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Priority, Category, Due Date fields...
                // (Similar to Swift implementation)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        TodoTask(
                            title = title,
                            description = description,
                            priority = priority,
                            category = category,
                            dueDate = dueDate
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: TodoTask,
    onDismiss: () -> Unit,
    onSave: (TodoTask) -> Unit,
    onDelete: () -> Unit
) {
    // Similar to AddTaskDialog but pre-filled with task data
    // Also includes Delete button
}

// ===== Database =====

class TodoDatabase {
    private val tasks = mutableListOf<TodoTask>()

    fun getAllTasks(): List<TodoTask> = tasks.toList()

    fun insertTask(task: TodoTask) {
        tasks.add(task)
    }

    fun updateTask(task: TodoTask) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
        }
    }

    fun deleteTask(id: String) {
        tasks.removeAll { it.id == id }
    }
}

// ===== Helper Functions =====

fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(Date(timestamp))
}
```

---

(Continuing in next message due to length...)

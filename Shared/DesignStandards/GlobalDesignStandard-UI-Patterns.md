# Global Design Standard: UI Patterns

**Version:** 1.0.0
**Created:** 2025-11-10
**Last Updated:** 2025-11-10
**Status:** Living Document
**Scope:** All Avanues Ecosystem UI Components

---

## Purpose

This standard defines **how to build UI components** in the Avanues ecosystem using Jetpack Compose (Android), Compose Multiplatform, and SwiftUI (iOS). It ensures consistency, accessibility, and maintainability across all apps.

---

## Core Principles

1. **Compose-First**: Default to Jetpack Compose for all new UI
2. **Material Design 3**: Follow Material You guidelines
3. **Accessibility**: WCAG 2.1 Level AA compliance
4. **Responsive**: Support all screen sizes and orientations
5. **Testable**: All UI must be testable with Compose Testing
6. **Themeable**: Support light/dark modes and custom themes
7. **Performance**: 60 FPS minimum, smooth animations

---

## Compose Best Practices

### 1. Composable Functions

#### Naming Convention
- Use **noun phrases** for components: `Button`, `Card`, `ProfileScreen`
- Use **verb phrases** for actions: `rememberScrollState()`, `produceState()`
- Prefix state holders with `remember`: `rememberLazyListState()`

#### Function Structure
```kotlin
/**
 * Brief description of what this composable does
 *
 * @param data The data to display
 * @param onAction Callback for user actions
 * @param modifier Modifier to apply to the root element
 * @param enabled Whether the component is enabled (defaults based on state)
 */
@Composable
fun MyComponent(
    data: MyData,                    // Required data first
    onAction: (Action) -> Unit,      // Callbacks second
    modifier: Modifier = Modifier,   // Modifier with default
    enabled: Boolean = true          // Optional params with defaults
) {
    // Implementation
}
```

#### Rules
- ✅ **Always** accept `Modifier` parameter (defaults to `Modifier`)
- ✅ **Always** apply modifier to root element first
- ✅ Use `@Stable` for data classes used in state
- ✅ Hoist state when possible (stateless composables)
- ❌ **Never** create ViewModel inside composable (inject via parameter)
- ❌ **Never** use `GlobalScope` (use `rememberCoroutineScope()`)

---

### 2. State Management

#### Stateless vs Stateful

**Stateless (Preferred):**
```kotlin
@Composable
fun Counter(
    count: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onIncrement, modifier = modifier) {
        Text("Count: $count")
    }
}
```

**Stateful (Use when needed):**
```kotlin
@Composable
fun Counter(
    initialCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var count by remember { mutableIntStateOf(initialCount) }

    Button(onClick = { count++ }, modifier = modifier) {
        Text("Count: $count")
    }
}
```

#### State Hoisting Pattern
```kotlin
// Stateful wrapper (convenience)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    SearchBarStateless(
        query = query,
        onQueryChange = { query = it },
        onSearch = onSearch,
        modifier = modifier
    )
}

// Stateless core (reusable, testable)
@Composable
fun SearchBarStateless(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        trailingIcon = {
            IconButton(onClick = { onSearch(query) }) {
                Icon(Icons.Default.Search, "Search")
            }
        }
    )
}
```

---

### 3. Side Effects

#### LaunchedEffect (for coroutines)
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Launch coroutine when key changes
    LaunchedEffect(uiState.userId) {
        viewModel.loadUserData(uiState.userId)
    }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyEvent.ShowSnackbar -> {
                    // Show snackbar
                }
            }
        }
    }
}
```

**Rules:**
- ✅ Use for launching coroutines
- ✅ Cancels automatically when composable leaves composition
- ✅ Relaunches when key changes
- ❌ **Never** use with rapidly changing keys (creates too many coroutines)

---

#### DisposableEffect (for cleanup)
```kotlin
@Composable
fun BrowserWebView(url: String) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }

    DisposableEffect(url) {
        webView.loadUrl(url)

        // Cleanup when composable is disposed
        onDispose {
            webView.destroy()
        }
    }

    AndroidView(factory = { webView })
}
```

**Rules:**
- ✅ Use when you need to clean up resources
- ✅ Call `onDispose` to release resources
- ❌ **Never** forget `onDispose` (causes memory leaks)

---

#### SideEffect (for synchronization)
```kotlin
@Composable
fun AnalyticsLogger(screenName: String) {
    val analytics = LocalAnalytics.current

    SideEffect {
        analytics.logScreenView(screenName)
    }
}
```

**Rules:**
- ✅ Use for non-suspending side effects
- ✅ Runs on every recomposition
- ❌ **Never** use for expensive operations

---

### 4. Performance Optimization

#### Remember Expensive Calculations
```kotlin
@Composable
fun ExpensiveList(items: List<Item>) {
    // ❌ Bad: Recalculates on every recomposition
    val sorted = items.sortedByDescending { it.priority }

    // ✅ Good: Only recalculates when items change
    val sorted = remember(items) {
        items.sortedByDescending { it.priority }
    }

    LazyColumn {
        items(sorted) { item ->
            ItemCard(item)
        }
    }
}
```

---

#### derivedStateOf (for computed state)
```kotlin
@Composable
fun ScrollToTopButton(listState: LazyListState) {
    // Only recomposes when showButton value changes
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    AnimatedVisibility(visible = showButton) {
        FloatingActionButton(onClick = { /* scroll to top */ }) {
            Icon(Icons.Default.ArrowUpward, "Scroll to top")
        }
    }
}
```

---

#### Avoid Recreating Lambdas
```kotlin
@Composable
fun MyList(items: List<Item>, onItemClick: (Item) -> Unit) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            // ❌ Bad: Creates new lambda on every recomposition
            ItemCard(item, onClick = { onItemClick(item) })

            // ✅ Good: Remember lambda
            val onClick = remember(item) { { onItemClick(item) } }
            ItemCard(item, onClick = onClick)

            // ✅ Even better: Pass item directly
            ItemCard(item = item, onItemClick = onItemClick)
        }
    }
}
```

---

### 5. Lists and Grids

#### LazyColumn Best Practices
```kotlin
@Composable
fun BookmarkList(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Use key for stable identity
        items(
            items = bookmarks,
            key = { it.id }  // ✅ Stable unique key
        ) { bookmark ->
            BookmarkCard(
                bookmark = bookmark,
                onClick = { onBookmarkClick(bookmark) },
                // Animate item placement
                modifier = Modifier.animateItem()
            )
        }
    }
}
```

**Rules:**
- ✅ **Always** provide `key` parameter
- ✅ Use `contentPadding` instead of wrapping in Box
- ✅ Use `animateItem()` for smooth list updates
- ✅ Use `LazyColumn` for vertical, `LazyRow` for horizontal
- ❌ **Never** nest scrollable elements (causes performance issues)

---

## Material Design 3 Integration

### Theme Setup
```kotlin
// Define color scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    // ... complete color scheme
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    // ... complete color scheme
)

// App theme
@Composable
fun AvanueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

### Using Theme Colors
```kotlin
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(
            text = "Hello",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

**Rules:**
- ✅ **Always** use theme colors (never hardcoded colors)
- ✅ Use semantic color names (primary, surface, error)
- ✅ Test both light and dark themes
- ❌ **Never** use `Color.Red`, use `MaterialTheme.colorScheme.error`

---

## Component Patterns

### 1. Cards
```kotlin
@Composable
fun BookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete bookmark"
                )
            }
        }
    }
}
```

---

### 2. Dialogs
```kotlin
@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $itemName?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

### 3. Bottom Sheets
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkBottomSheet(
    bookmark: Bookmark,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ListItem(
                headlineContent = { Text("Edit") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.clickable(onClick = onEdit)
            )

            ListItem(
                headlineContent = { Text("Share") },
                leadingContent = { Icon(Icons.Default.Share, null) },
                modifier = Modifier.clickable(onClick = onShare)
            )

            ListItem(
                headlineContent = { Text("Delete") },
                leadingContent = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.clickable(onClick = onDelete)
            )
        }
    }
}
```

---

### 4. Snackbars
```kotlin
@Composable
fun MyScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Button(
            onClick = {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Bookmark deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            // Undo delete
                        }
                        SnackbarResult.Dismissed -> {
                            // Snackbar dismissed
                        }
                    }
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            Text("Delete")
        }
    }
}
```

---

## Accessibility

### 1. Content Descriptions
```kotlin
Icon(
    Icons.Default.Favorite,
    contentDescription = "Add to favorites"  // ✅ Always provide
)

Icon(
    Icons.Default.ArrowBack,
    contentDescription = null  // ✅ Only if decorative (parent has description)
)
```

---

### 2. Semantic Properties
```kotlin
Button(
    onClick = onClick,
    modifier = Modifier.semantics {
        contentDescription = "Delete bookmark"
        role = Role.Button
        stateDescription = if (isDeleting) "Deleting..." else null
    }
) {
    if (isDeleting) {
        CircularProgressIndicator()
    } else {
        Text("Delete")
    }
}
```

---

### 3. Minimum Touch Targets
```kotlin
// ✅ Good: 48dp minimum
IconButton(
    onClick = onClick,
    modifier = Modifier.size(48.dp)
) {
    Icon(Icons.Default.Close, "Close")
}

// ❌ Bad: Too small
Icon(
    Icons.Default.Close,
    "Close",
    modifier = Modifier
        .size(16.dp)
        .clickable(onClick = onClick)  // Touch target too small!
)
```

**Rules:**
- ✅ Minimum touch target: **48dp x 48dp**
- ✅ Use `IconButton` instead of clickable Icon
- ✅ Provide `contentDescription` for all interactive elements

---

### 4. Color Contrast
```kotlin
// ✅ Good: High contrast
Text(
    text = "Important",
    color = MaterialTheme.colorScheme.onSurface  // Guaranteed contrast
)

// ❌ Bad: Low contrast
Text(
    text = "Important",
    color = Color.Gray  // May not have sufficient contrast
)
```

**Rules:**
- ✅ Text: 4.5:1 contrast ratio minimum
- ✅ Large text (18sp+): 3:1 contrast ratio minimum
- ✅ Use Material Theme colors (guaranteed contrast)

---

## Navigation

### Navigation Component
```kotlin
@Composable
fun AvanueNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "browser"
    ) {
        composable("browser") {
            BrowserScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "bookmark/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookmarkId = backStackEntry.arguments?.getString("id")
            BookmarkDetailScreen(
                bookmarkId = bookmarkId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

---

## Testing

### UI Tests
```kotlin
class BookmarkListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookmarkList_displaysBookmarks() {
        val bookmarks = listOf(
            Bookmark(id = "1", url = "https://example.com", title = "Example"),
            Bookmark(id = "2", url = "https://test.com", title = "Test")
        )

        composeTestRule.setContent {
            BookmarkList(
                bookmarks = bookmarks,
                onBookmarkClick = {}
            )
        }

        // Assert bookmarks are displayed
        composeTestRule.onNodeWithText("Example").assertExists()
        composeTestRule.onNodeWithText("Test").assertExists()
    }

    @Test
    fun bookmarkCard_clickTriggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            BookmarkCard(
                bookmark = Bookmark(id = "1", url = "https://example.com", title = "Example"),
                onClick = { clicked = true },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Example").performClick()

        assertTrue(clicked)
    }
}
```

---

## Animation

### AnimatedVisibility
```kotlin
@Composable
fun ExpandableCard(expanded: Boolean) {
    Column {
        Text("Header")

        AnimatedVisibility(visible = expanded) {
            Text("Expanded content")
        }
    }
}
```

### Crossfade
```kotlin
@Composable
fun ContentSwitcher(state: ContentState) {
    Crossfade(targetState = state) { currentState ->
        when (currentState) {
            ContentState.Loading -> LoadingIndicator()
            is ContentState.Success -> SuccessView(currentState.data)
            is ContentState.Error -> ErrorView(currentState.error)
        }
    }
}
```

### AnimateAsState
```kotlin
@Composable
fun ProgressBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300)
    )

    LinearProgressIndicator(progress = { animatedProgress })
}
```

---

## Version History

- **v1.0.0** (2025-11-10): Initial UI Patterns standard

---

**Created by Manoj Jhawar, manoj@ideahq.net**

# Developer Manual - Chapter 47: Desktop Development

**Date**: 2025-11-22
**Status**: 📋 Phase 3.0 Planning Document
**Target Implementation**: Q1 2026

---

## Executive Summary

This chapter documents the Desktop implementation strategy for AVA AI using Kotlin Multiplatform (KMP) and Compose Desktop, bringing Phase 2.0 features to Windows, macOS, and Linux platforms.

**Key Deliverables for Phase 3.0**:
- Compose Desktop RAG chat UI with source citations
- Cross-platform database access (Room + SQLite)
- Settings management and RAG configuration
- Multi-window support with drag-and-drop
- Keyboard shortcuts and OS integration
- Comprehensive test coverage (85%+)
- Performance-optimized for desktop

---

## 1. Desktop Architecture Overview

### Platform Targets
- **Windows**: Windows 10 (1809+)
- **macOS**: macOS 11.0+
- **Linux**: Ubuntu 20.04+ (Fedora, Debian support)
- **UI Framework**: Compose for Desktop (1.6.0+)
- **Database**: Room + SQLite
- **Language**: Kotlin 1.9.0+

### Architecture Diagram

```
┌─────────────────────────────────┐
│    Compose Desktop UI           │  MainWindow, ChatScreen, Settings
├─────────────────────────────────┤
│    MVVM with StateFlow          │  ChatViewModel, RAGViewModel
├─────────────────────────────────┤
│    Domain Layer (Shared KMP)    │  Use Cases, Repositories (shared)
├─────────────────────────────────┤
│    Data Layer (Room/SQLite)     │  Database, DAOs (shared)
├─────────────────────────────────┤
│    Native OS Integration        │  File system, clipboard, OS theming
└─────────────────────────────────┘
```

---

## 2. KMP Project Structure

### Gradle Configuration

```kotlin
// settings.gradle.kts
include(":shared")
include(":desktop")

// desktop/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm("desktop") {
        withJava()
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(project(":shared"))
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}
```

### Directory Structure

```
ava-desktop/
├── shared/                    # Shared KMP code
│   ├── src/commonMain/kotlin/
│   │   ├── domain/
│   │   │   ├── models/
│   │   │   ├── usecases/
│   │   │   └── repositories/
│   │   ├── data/
│   │   │   ├── database/
│   │   │   └── repositories/
│   │   └── util/
│   └── build.gradle.kts
│
├── desktop/                   # Desktop-specific code
│   ├── src/main/kotlin/
│   │   ├── com/augmentalis/ava/
│   │   │   ├── main.kt        # Entry point
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   └── Theme.kt
│   │   │   │   ├── screens/
│   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   └── RAGSettingsPanel.kt
│   │   │   │   ├── components/
│   │   │   │   │   ├── ChatBubble.kt
│   │   │   │   │   ├── MessageList.kt
│   │   │   │   │   └── Toolbar.kt
│   │   │   │   └── navigation/
│   │   │   │       └── Navigation.kt
│   │   │   ├── viewmodels/
│   │   │   │   ├── ChatViewModel.kt
│   │   │   │   └── SettingsViewModel.kt
│   │   │   ├── services/
│   │   │   │   ├── DatabaseService.kt
│   │   │   │   └── PreferencesService.kt
│   │   │   └── util/
│   │   │       ├── PlatformUtils.kt
│   │   │       └── Logger.kt
│   │   └── resources/
│   │       ├── strings.xml
│   │       └── icons/
│   ├── src/test/kotlin/
│   │   └── ChatViewModelTests.kt
│   └── build.gradle.kts
│
└── build.gradle.kts
```

---

## 3. Compose Desktop Implementation

### 3.1 Main Application Entry Point

```kotlin
// src/main/kotlin/main.kt
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.ui.screens.ChatScreen
import com.augmentalis.ava.ui.theme.AVATheme
import com.augmentalis.ava.viewmodels.ChatViewModel

fun main() {
    val databaseService = DatabaseService()
    val chatViewModel = ChatViewModel(databaseService)

    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "AVA AI Assistant",
            state = windowState,
            icon = painterResource("icons/app_icon.png")
        ) {
            AVATheme {
                MainContent(chatViewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: ChatViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        ChatScreen(viewModel = viewModel)
    }
}
```

### 3.2 Chat Screen (Desktop Optimized)

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.augmentalis.ava.viewmodels.ChatViewModel

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AVA Chat",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear chat")
                    }
                    IconButton(onClick = { /* Show settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }

        // Messages Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }

            if (isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AVA is thinking...")
                    }
                }
            }
        }

        // Input Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                if (event.isCtrlPressed || event.isMetaPressed) {
                                    sendMessage(messageText, viewModel)
                                    messageText = ""
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("Type a message (Ctrl+Enter to send)") },
                    multiline = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            sendMessage(messageText, viewModel)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: Message) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(if (message.isUserMessage) Alignment.End else Alignment.Start)
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUserMessage)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    message.content,
                    color = if (message.isUserMessage)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                // Source citations
                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SourceCitationsPanel(message.citations)
                }
            }
        }
    }
}

@Composable
fun SourceCitationsPanel(citations: List<Citation>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Sources",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )

        citations.forEach { citation ->
            CitationCard(citation)
        }
    }
}

@Composable
fun CitationCard(citation: Citation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    citation.documentTitle,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "${(citation.relevanceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            citation.snippet?.let { snippet ->
                Text(
                    snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

private fun sendMessage(text: String, viewModel: ChatViewModel) {
    if (text.isNotBlank()) {
        viewModel.sendMessage(text)
    }
}
```

### 3.3 Desktop-Specific Features

#### Multi-window Support

```kotlin
@Composable
fun MultiWindowApp() {
    val windowManager = remember { DesktopWindowManager() }

    LaunchedEffect(Unit) {
        // Create chat window
        windowManager.createChatWindow()

        // Create settings window
        windowManager.createSettingsWindow()
    }

    // Windows are managed by DesktopWindowManager
}

class DesktopWindowManager {
    fun createChatWindow() {
        ApplicationScope().apply {
            Window(
                onCloseRequest = ::closeWindow,
                title = "AVA - Chat",
                state = rememberWindowState(width = 1000.dp, height = 700.dp)
            ) {
                ChatScreen(viewModel = ChatViewModel())
            }
        }
    }

    fun createSettingsWindow() {
        ApplicationScope().apply {
            Window(
                onCloseRequest = ::closeWindow,
                title = "AVA - Settings"
            ) {
                SettingsScreen(viewModel = SettingsViewModel())
            }
        }
    }
}
```

#### Keyboard Shortcuts

```kotlin
@Composable
fun KeyboardShortcuts(viewModel: ChatViewModel) {
    val shortcuts = mapOf(
        KeyShortcut(Key.N, ctrl = true) to { viewModel.newConversation() },
        KeyShortcut(Key.O, ctrl = true) to { /* Open file */ },
        KeyShortcut(Key.S, ctrl = true) to { viewModel.saveConversation() },
        KeyShortcut(Key.Q, ctrl = true) to { /* Exit app */ },
        KeyShortcut(Key.Comma, ctrl = true) to { /* Open settings */ },
        KeyShortcut(Key.K, ctrl = true) to { /* Show command palette */ }
    )

    LaunchedEffect(Unit) {
        shortcuts.forEach { (shortcut, action) ->
            registerKeyboardShortcut(shortcut, action)
        }
    }
}
```

#### File Drag & Drop

```kotlin
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.Draggable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun DocumentDropZone(onFilesDropped: (List<File>) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.transferData.hasFiles()
                },
                target = object : DropTargetListener {
                    override fun onDrop(event: DragEvent): Boolean {
                        val files = event.clipData?.files ?: return false
                        onFilesDropped(files.map { File(it.path) })
                        return true
                    }
                }
            ),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Text("Drop documents here")
            }
        }
    }
}
```

---

## 4. Platform-Specific Implementation

### 4.1 Database Access (Cross-Platform)

```kotlin
// shared/src/commonMain/kotlin/data/database/Database.kt
expect class AVADatabase {
    fun conversationDao(): ConversationDao
    fun messageDao(): MessageDao
    fun documentDao(): DocumentDao
}

// desktop/src/main/kotlin/data/database/DesktopDatabase.kt
actual class AVADatabase(databasePath: String) {
    private val db = Room.databaseBuilder(
        File(databasePath),
        AVADatabase::class.java,
        "ava.db"
    ).build()

    actual fun conversationDao(): ConversationDao = db.conversationDao()
    actual fun messageDao(): MessageDao = db.messageDao()
    actual fun documentDao(): DocumentDao = db.documentDao()
}
```

### 4.2 Settings Management

```kotlin
class DesktopSettingsService {
    private val configDir = File(
        System.getProperty("user.home"),
        ".config/ava"
    ).apply { mkdirs() }

    fun loadSettings(): AppSettings {
        val settingsFile = File(configDir, "settings.json")
        return if (settingsFile.exists()) {
            Json.decodeFromString(settingsFile.readText())
        } else {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val settingsFile = File(configDir, "settings.json")
        settingsFile.writeText(Json.encodeToString(settings))
    }
}
```

### 4.3 Theming (OS-Aware)

```kotlin
@Composable
fun AVATheme(
    darkTheme: Boolean = isSystemDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun isSystemDarkTheme(): Boolean {
    return when (System.getProperty("os.name").lowercase()) {
        "macos" -> OSXDarkMode.isDarkMode
        else -> {
            // Windows/Linux detection
            false
        }
    }
}
```

---

## 5. Testing

### 5.1 Unit Tests

```kotlin
import org.junit.Test
import org.junit.Before
import kotlinx.coroutines.test.runTest

class DesktopChatViewModelTests {
    private lateinit var viewModel: ChatViewModel
    private lateinit var mockRepository: MockConversationRepository

    @Before
    fun setUp() {
        mockRepository = MockConversationRepository()
        viewModel = ChatViewModel(mockRepository)
    }

    @Test
    fun testSendMessage() = runTest {
        val message = "Hello AVA"
        viewModel.sendMessage(message)

        assert(viewModel.messages.value.any { it.content == message })
    }

    @Test
    fun testMultilineMessageHandling() = runTest {
        val message = "Line 1\nLine 2\nLine 3"
        viewModel.sendMessage(message)

        val sentMessage = viewModel.messages.value.first()
        assert(sentMessage.content.contains("\n"))
    }
}
```

### 5.2 UI Tests

```kotlin
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule

class ChatScreenDesktopTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMessageDisplaysCorrectly() {
        composeTestRule.setContent {
            ChatScreen(viewModel = ChatViewModel())
        }

        composeTestRule
            .onNodeWithText("Type a message")
            .assertIsDisplayed()
    }

    @Test
    fun testKeyboardShortcut() {
        composeTestRule.setContent {
            ChatScreen(viewModel = ChatViewModel())
        }

        composeTestRule
            .onNodeWithText("Type a message")
            .performTextInput("Hello")
            .performKeyInput { pressKey(Key.Enter, meta = true) }

        // Verify message sent
    }
}
```

---

## 6. Performance Optimization

### 6.1 Memory Usage

- Lazy load message list with paging
- Use `@Stable` annotations for data classes
- Avoid unnecessary recompositions with `.remember`
- Use `mutableStateOf` only when necessary

### 6.2 Startup Time

```kotlin
// Defer non-critical initialization
LaunchedEffect(Unit) {
    // Load plugins
    // Load recent conversations
    // Update models
}
```

### 6.3 Network Efficiency

- Use HTTP/2 connection pooling
- Implement request batching
- Add retry logic with exponential backoff

---

## 7. Deployment

### 7.1 Build Process

```bash
# Linux/macOS
./gradlew :desktop:packageReleaseUberJarForCurrentOS

# Windows
gradlew.bat :desktop:packageReleaseUberJarForCurrentOS
```

### 7.2 Distribution

- **macOS**: DMG or Homebrew
- **Windows**: NSIS installer or Chocolatey
- **Linux**: Snap, AppImage, or distribution repos

---

## 8. Next Steps (Phase 3.0)

**Week 2-3**:
- [ ] Set up KMP project structure
- [ ] Implement core Compose Desktop screens
- [ ] Integrate with shared domain models
- [ ] Add database access layer

**Week 3-4**:
- [ ] Implement multi-window support
- [ ] Add keyboard shortcuts
- [ ] Add drag & drop support
- [ ] Complete test suite (85%+ coverage)

---

**Created by**: Agent 6 (AI Assistant)
**Framework**: IDEACODE v8.4
**Status**: 📋 Planning Document for Phase 3.0 Implementation
**Last Updated**: 2025-11-22


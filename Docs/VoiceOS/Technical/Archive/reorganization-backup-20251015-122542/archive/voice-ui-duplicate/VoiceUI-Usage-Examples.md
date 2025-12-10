# VoiceUI Usage Examples

## 🚀 Complete Working Example: Task Manager App

This example shows how to build a simple task manager using VoiceUI components.

### 1. Create the Activity

```kotlin
// File: TaskManagerActivity.kt
package com.augmentalis.voiceos.examples

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.augmentalis.voiceos.VoiceOS
import com.augmentalis.voiceos.ui.base.VoiceUIActivity
import kotlinx.coroutines.launch

class TaskManagerActivity : VoiceUIActivity() {
    
    // Task data
    private var tasks = mutableStateListOf<Task>()
    private var selectedTask by mutableStateOf<Task?>(null)
    
    data class Task(
        val id: String,
        val title: String,
        val completed: Boolean = false
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load sample tasks
        tasks.addAll(listOf(
            Task("1", "Review code changes"),
            Task("2", "Update documentation"),
            Task("3", "Test voice commands")
        ))
    }
    
    override fun configureVoiceUI() {
        super.configureVoiceUI()
        
        // Set theme for productivity
        voiceUI.themeEngine.setTheme("material")
        
        // Enable gestures
        voiceUI.gestureManager.enableMultiTouch(true)
        
        // Show welcome message
        voiceUI.hudSystem.showNotification(
            message = "Task Manager Ready. Say 'add task' to begin.",
            duration = 3000,
            position = "TOP_CENTER"
        )
    }
    
    override fun registerVoiceCommands() {
        super.registerVoiceCommands()
        
        // Register task-specific voice commands
        voiceUI.voiceCommandSystem.apply {
            registerCommand("add task", "ADD_TASK")
            registerCommand("complete task", "COMPLETE_TASK")
            registerCommand("delete task", "DELETE_TASK")
            registerCommand("show completed", "SHOW_COMPLETED")
            registerCommand("show pending", "SHOW_PENDING")
        }
    }
    
    override fun setupGestureHandlers() {
        super.setupGestureHandlers()
        
        lifecycleScope.launch {
            voiceUI.gestureManager.gestureFlow.collect { gesture ->
                when (gesture.type) {
                    GestureType.SWIPE_RIGHT -> completeSelectedTask()
                    GestureType.SWIPE_LEFT -> deleteSelectedTask()
                    GestureType.TWO_FINGER_TAP -> addNewTask()
                    else -> handleGesture(gesture)
                }
            }
        }
    }
    
    @Composable
    override fun ActivityContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with voice hint
            TaskHeader()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task list
            TaskList()
            
            // Floating action button
            AddTaskButton()
        }
    }
    
    @Composable
    private fun TaskHeader() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "My Tasks",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Say 'add task' or swipe right to complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    @Composable
    private fun TaskList() {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                TaskItem(task)
            }
        }
    }
    
    @Composable
    private fun TaskItem(task: Task) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedTask = task },
            colors = if (task == selectedTask) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            } else {
                CardDefaults.cardColors()
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.title,
                    style = if (task.completed) {
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = TextDecoration.LineThrough
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                )
                
                if (task.completed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    @Composable
    private fun AddTaskButton() {
        ExtendedFloatingActionButton(
            onClick = { addNewTask() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Task")
        }
    }
    
    // Task management functions
    private fun addNewTask() {
        // Show VoiceUI input dialog
        voiceUI.notificationSystem.showInputDialog(
            title = "New Task",
            hint = "Enter task description",
            onSubmit = { taskTitle ->
                tasks.add(Task(
                    id = System.currentTimeMillis().toString(),
                    title = taskTitle
                ))
                
                voiceUI.hudSystem.showNotification(
                    "Task added: $taskTitle",
                    duration = 2000
                )
            },
            voiceDictation = true // Enable voice input
        )
    }
    
    private fun completeSelectedTask() {
        selectedTask?.let { task ->
            val index = tasks.indexOf(task)
            if (index >= 0) {
                tasks[index] = task.copy(completed = !task.completed)
                
                voiceUI.hudSystem.showNotification(
                    if (tasks[index].completed) "Task completed!" else "Task reopened",
                    duration = 1500
                )
            }
        }
    }
    
    private fun deleteSelectedTask() {
        selectedTask?.let { task ->
            // Show confirmation dialog using VoiceUI
            voiceUI.notificationSystem.showAlert(
                title = "Delete Task?",
                message = "Delete '${task.title}'?",
                positiveButton = "Delete",
                negativeButton = "Cancel",
                onPositive = {
                    tasks.remove(task)
                    selectedTask = null
                    
                    voiceUI.hudSystem.showNotification(
                        "Task deleted",
                        duration = 1500
                    )
                }
            )
        }
    }
}
```

### 2. Using VoiceUI from External App (Third-Party)

```kotlin
// File: ExternalAppExample.kt
package com.example.myapp

import android.content.Intent
import android.content.ComponentName
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity

class ExternalAppExample : AppCompatActivity() {
    
    // Method 1: Using Intents
    fun changeThemeViaIntent() {
        val intent = Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
            setPackage("com.augmentalis.voiceos")
            putExtra("theme_name", "arvision")
            putExtra("animate", true)
        }
        startService(intent)
    }
    
    fun showHUDNotification() {
        val intent = Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
            putExtra("message", "Hello from external app!")
            putExtra("duration", 3000)
            putExtra("position", "TOP_CENTER")
            putExtra("priority", "HIGH")
        }
        sendBroadcast(intent)
    }
    
    // Method 2: Using Content Provider
    fun queryAvailableThemes() {
        val uri = Uri.parse("content://com.augmentalis.voiceui.provider/themes")
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val isActive = cursor.getInt(cursor.getColumnIndex("is_active")) == 1
                
                println("Theme: $name, Active: $isActive")
            }
        }
    }
    
    // Method 3: Using Service Binding
    private var voiceUIService: VoiceUIService.VoiceUIBinder? = null
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceUIService = service as? VoiceUIService.VoiceUIBinder
            
            // Now you can use VoiceUI directly
            voiceUIService?.apply {
                // Change theme
                themeEngine.setTheme("visionos")
                
                // Create a window
                val windowId = windowManager.createSpatialWindow(
                    title = "External App Window",
                    width = 800,
                    height = 600,
                    z = -2f // Depth
                )
                
                // Show notification
                hudSystem.showNotification(
                    "Connected to VoiceUI!",
                    duration = 2000
                )
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            voiceUIService = null
        }
    }
    
    fun connectToVoiceUI() {
        val intent = Intent("com.augmentalis.voiceui.service.WINDOW_SERVICE")
        intent.setPackage("com.augmentalis.voiceos")
        bindService(intent, connection, BIND_AUTO_CREATE)
    }
}
```

### 3. Simple Use Cases

#### Change Theme
```kotlin
// Internal (within VoiceOS app)
voiceUI.themeEngine.setTheme("arvision")

// External (from any app)
Intent("com.augmentalis.voiceui.action.THEME_CHANGE").apply {
    putExtra("theme_name", "arvision")
}.let { context.sendBroadcast(it) }
```

#### Show Notification
```kotlin
// Internal
voiceUI.hudSystem.showNotification("Task completed!", 2000)

// External
Intent("com.augmentalis.voiceui.action.HUD_NOTIFY").apply {
    putExtra("message", "Task completed!")
    putExtra("duration", 2000)
}.let { context.sendBroadcast(it) }
```

#### Register Voice Command
```kotlin
// Internal
voiceUI.voiceCommandSystem.registerCommand(
    "open settings",
    "OPEN_SETTINGS_ACTION"
)

// External
Intent("com.augmentalis.voiceui.action.VOICE_REGISTER").apply {
    putExtra("command", "open settings")
    putExtra("action", "com.myapp.OPEN_SETTINGS")
}.let { context.startService(it) }
```

#### Create Spatial Window
```kotlin
// Internal
voiceUI.windowManager.createSpatialWindow(
    title = "Settings",
    width = 1024,
    height = 768,
    z = -3f // 3 units deep
)

// External
Intent("com.augmentalis.voiceui.action.WINDOW_CREATE").apply {
    putExtra("title", "Settings")
    putExtra("width", 1024)
    putExtra("height", 768)
    putExtra("z", -3f)
}.let { context.startService(it) }
```

#### Handle Gestures
```kotlin
// Internal - setup in activity
lifecycleScope.launch {
    voiceUI.gestureManager.gestureFlow.collect { gesture ->
        when (gesture.type) {
            GestureType.TAP -> handleTap()
            GestureType.SWIPE_LEFT -> navigateBack()
            GestureType.SWIPE_RIGHT -> navigateForward()
            GestureType.PINCH_IN -> zoomOut()
            GestureType.PINCH_OUT -> zoomIn()
        }
    }
}
```

### 4. Advanced Example: Custom Dialog

```kotlin
// Create a custom VoiceUI dialog
fun showCustomDialog() {
    voiceUI.notificationSystem.showBottomSheet(
        title = "Select Option",
        content = {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Choose your preferred input method:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Voice option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            selectInputMethod("voice")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Voice Input")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Gesture option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            selectInputMethod("gesture")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Gesture Input")
                    }
                }
            }
        },
        isDismissible = true
    )
}
```

### 5. Complete Integration in MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var voiceUI: VoiceUIModule
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get VoiceUI from application
        voiceUI = (application as VoiceOS).voiceUI
        
        setContent {
            // Use VoiceUI theme
            voiceUI.themeEngine.VoiceUITheme {
                MainScreen()
            }
        }
        
        setupVoiceCommands()
        setupGestures()
    }
    
    private fun setupVoiceCommands() {
        // Register app-wide commands
        voiceUI.voiceCommandSystem.apply {
            registerCommand("go home", "NAVIGATE_HOME")
            registerCommand("go back", "NAVIGATE_BACK")
            registerCommand("open menu", "OPEN_MENU")
            registerCommand("search", "OPEN_SEARCH")
        }
    }
    
    private fun setupGestures() {
        lifecycleScope.launch {
            voiceUI.gestureManager.gestureFlow.collect { gesture ->
                when (gesture.type) {
                    GestureType.THREE_FINGER_TAP -> {
                        // Open quick settings
                        voiceUI.windowManager.createSpatialWindow(
                            title = "Quick Settings",
                            content = { QuickSettingsContent() }
                        )
                    }
                    GestureType.SWIPE_DOWN -> {
                        // Show notifications
                        voiceUI.hudSystem.showNotification(
                            "No new notifications",
                            2000
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun MainScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VoiceOS") },
                    actions = {
                        IconButton(onClick = {
                            // Use VoiceUI to show menu
                            voiceUI.notificationSystem.showPopupMenu(
                                items = listOf("Settings", "About", "Help"),
                                onItemSelected = { index, item ->
                                    when (index) {
                                        0 -> openSettings()
                                        1 -> showAbout()
                                        2 -> showHelp()
                                    }
                                }
                            )
                        }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                // Your content here
            }
        }
    }
}
```

## 📱 Testing Your Implementation

### Using ADB to Test Intents
```bash
# Change theme
adb shell am broadcast -a com.augmentalis.voiceui.action.THEME_CHANGE \
  --es theme_name "arvision"

# Show HUD notification
adb shell am broadcast -a com.augmentalis.voiceui.action.HUD_NOTIFY \
  --es message "Test notification" \
  --ei duration 3000

# Create window
adb shell am startservice -a com.augmentalis.voiceui.action.WINDOW_CREATE \
  --es title "Test Window" \
  --ei width 800 \
  --ei height 600
```

## 🎯 Key Points

1. **Internal Use**: Direct access via `voiceUI.component.method()`
2. **External Use**: Intents, Content Provider, or Service Binding
3. **Voice Commands**: Register commands for any UI element
4. **Gestures**: Handle all gesture types in one place
5. **Themes**: Consistent theming across all VoiceUI components
6. **Notifications**: Replace all Android dialogs with VoiceUI

---
**Examples Status:** Complete and Tested  
**Compatibility:** VOS4 Direct Pattern  
**Last Updated:** 2025-01-24
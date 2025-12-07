# Android to VoiceUI Conversion Guide

## 🔄 Converting XML and Compose UI to VoiceUI

This guide shows how to convert existing Android UI (XML layouts and Compose) to VoiceUI's voice and gesture-enabled components.

## 📋 Conversion Strategy Overview

```
Android UI (XML/Compose) → VoiceUI Components
         ↓                        ↓
    Static, Touch-only      Voice + Gesture + Spatial
         ↓                        ↓
    2D Layout               3D Spatial Windows
```

## 🎯 Component Mapping Table

| Android Component | VoiceUI Replacement | Key Benefits |
|-------------------|-------------------|--------------|
| `Toast` | `voiceUI.notificationSystem.showToast()` | Voice readout, custom position |
| `Snackbar` | `voiceUI.notificationSystem.showSnackbar()` | Voice commands for actions |
| `AlertDialog` | `voiceUI.notificationSystem.showAlert()` | Voice dismissal |
| `BottomSheetDialog` | `voiceUI.notificationSystem.showBottomSheet()` | Gesture control |
| `PopupMenu` | `voiceUI.notificationSystem.showPopupMenu()` | Voice selection |
| `ProgressDialog` | `voiceUI.notificationSystem.showProgress()` | Voice updates |
| `Button` | `VoiceUIButton` | Voice activation |
| `RecyclerView` | `VoiceUIList` | Voice navigation |
| `ViewPager` | `VoiceUIPager` | Gesture swiping |
| `Toolbar` | `VoiceUITopBar` | Voice menu access |

## 🔧 XML Layout Conversion

### Example 1: Converting a Simple Form

#### Original XML Layout
```xml
<!-- res/layout/activity_login.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:id="@+id/titleText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login"
        android:textSize="24sp" />
    
    <EditText
        android:id="@+id/emailInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Email"
        android:inputType="textEmailAddress" />
    
    <EditText
        android:id="@+id/passwordInput"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Password"
        android:inputType="textPassword" />
    
    <Button
        android:id="@+id/loginButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Login" />
    
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone" />
        
</LinearLayout>
```

#### Converted to VoiceUI Compose
```kotlin
// LoginActivity.kt using VoiceUI
class LoginActivity : VoiceUIActivity() {
    
    override fun registerVoiceCommands() {
        super.registerVoiceCommands()
        
        // Register voice commands for form
        voiceUI.voiceCommandSystem.apply {
            registerCommand("enter email", "FOCUS_EMAIL")
            registerCommand("enter password", "FOCUS_PASSWORD")
            registerCommand("login", "SUBMIT_LOGIN")
            registerCommand("forgot password", "FORGOT_PASSWORD")
        }
    }
    
    @Composable
    override fun ActivityContent() {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .voiceUIGestures(
                    onSwipeDown = { 
                        // Dismiss keyboard
                        hideKeyboard()
                    }
                )
        ) {
            // Title with voice announcement
            VoiceUIText(
                text = "Login",
                style = MaterialTheme.typography.headlineLarge,
                voiceLabel = "Login screen",
                autoAnnounce = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Email input with voice dictation
            VoiceUITextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                voiceCommand = "enter email",
                voiceDictation = true,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password input
            VoiceUITextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                voiceCommand = "enter password",
                voiceDictation = false, // No voice for passwords
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Login button with voice activation
            VoiceUIButton(
                text = "Login",
                voiceCommand = "login",
                onClick = { performLogin(email, password) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Loading indicator with voice feedback
            if (isLoading) {
                VoiceUIProgress(
                    message = "Logging in...",
                    voiceUpdate = true // Announces progress
                )
            }
        }
    }
    
    private fun performLogin(email: String, password: String) {
        // Validate with voice feedback
        when {
            email.isEmpty() -> {
                voiceUI.notificationSystem.showToast(
                    "Please enter your email",
                    voiceReadout = true
                )
            }
            password.isEmpty() -> {
                voiceUI.notificationSystem.showToast(
                    "Please enter your password",
                    voiceReadout = true
                )
            }
            else -> {
                // Perform login
                isLoading = true
                voiceUI.hudSystem.showNotification(
                    "Logging in...",
                    duration = 2000
                )
            }
        }
    }
}
```

### Example 2: Converting RecyclerView to VoiceUI List

#### Original XML with RecyclerView
```xml
<!-- res/layout/activity_tasks.xml -->
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/taskList"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
```

#### Original Adapter
```kotlin
class TaskAdapter : RecyclerView.Adapter<TaskViewHolder>() {
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }
}
```

#### Converted to VoiceUI List
```kotlin
@Composable
fun VoiceUITaskList(tasks: List<Task>) {
    val voiceUI = LocalVoiceUI.current
    var selectedIndex by remember { mutableStateOf(0) }
    
    // Register voice navigation
    LaunchedEffect(Unit) {
        voiceUI.voiceCommandSystem.apply {
            registerCommand("next task", "NEXT_TASK")
            registerCommand("previous task", "PREV_TASK")
            registerCommand("select task", "SELECT_TASK")
            registerCommand("complete task", "COMPLETE_TASK")
        }
    }
    
    // Handle voice commands
    LaunchedEffect(Unit) {
        voiceUI.voiceCommandSystem.commandFlow.collect { command ->
            when (command.action) {
                "NEXT_TASK" -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(tasks.size - 1)
                    announceTask(tasks[selectedIndex])
                }
                "PREV_TASK" -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    announceTask(tasks[selectedIndex])
                }
                "SELECT_TASK" -> {
                    selectTask(tasks[selectedIndex])
                }
                "COMPLETE_TASK" -> {
                    completeTask(tasks[selectedIndex])
                }
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .voiceUIGestures(
                onSwipeLeft = { completeTask(tasks[selectedIndex]) },
                onSwipeRight = { deleteTask(tasks[selectedIndex]) }
            )
    ) {
        itemsIndexed(tasks) { index, task ->
            VoiceUITaskItem(
                task = task,
                isSelected = index == selectedIndex,
                onClick = { 
                    selectedIndex = index
                    announceTask(task)
                }
            )
        }
    }
}

@Composable
fun VoiceUITaskItem(
    task: Task,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = if (isSelected) CardDefaults.cardElevation(8.dp) else CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = task.title,
                style = if (task.completed) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )
            
            if (task.completed) {
                Icon(Icons.Default.Check, contentDescription = "Completed")
            }
        }
    }
}
```

## 🎨 Compose UI Conversion

### Converting Compose Components

#### Original Compose Screen
```kotlin
@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") }
        )
        
        SwitchPreference(
            title = "Dark Mode",
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it }
        )
        
        ListPreference(
            title = "Language",
            value = language,
            onValueChange = { language = it },
            entries = languages
        )
        
        Button(
            onClick = { saveSettings() }
        ) {
            Text("Save")
        }
    }
}
```

#### Converted to VoiceUI Compose
```kotlin
@Composable
fun VoiceUISettingsScreen() {
    val voiceUI = LocalVoiceUI.current
    var focusedSetting by remember { mutableStateOf("") }
    
    // Register voice commands
    LaunchedEffect(Unit) {
        voiceUI.voiceCommandSystem.apply {
            registerCommand("toggle dark mode", "TOGGLE_DARK")
            registerCommand("change language", "CHANGE_LANGUAGE")
            registerCommand("save settings", "SAVE_SETTINGS")
            languages.forEach { lang ->
                registerCommand("select $lang", "SELECT_$lang")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .voiceUIFocusable(
                onVoiceFocus = { setting ->
                    focusedSetting = setting
                    voiceUI.hudSystem.showNotification(
                        "Focused on $setting",
                        duration = 1000
                    )
                }
            )
    ) {
        // Voice-enabled top bar
        VoiceUITopBar(
            title = "Settings",
            voiceLabel = "Settings screen",
            navigationIcon = {
                VoiceUIIconButton(
                    icon = Icons.Default.ArrowBack,
                    voiceCommand = "go back",
                    onClick = { navigateBack() }
                )
            }
        )
        
        // Dark mode switch with voice control
        VoiceUISwitch(
            title = "Dark Mode",
            checked = isDarkMode,
            onCheckedChange = { isDarkMode = it },
            voiceCommand = "toggle dark mode",
            voiceLabel = "Dark mode is ${if (isDarkMode) "on" else "off"}",
            modifier = Modifier.padding(16.dp)
        )
        
        // Language selector with voice
        VoiceUIDropdown(
            title = "Language",
            value = language,
            onValueChange = { language = it },
            entries = languages,
            voiceCommand = "change language",
            voiceSelectable = true, // Can select items by voice
            modifier = Modifier.padding(16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Save button with voice and gesture
        VoiceUIButton(
            text = "Save Settings",
            voiceCommand = "save settings",
            onClick = { saveSettings() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .voiceUIGestures(
                    onLongPress = {
                        voiceUI.hudSystem.showNotification(
                            "Say 'save settings' to save",
                            duration = 2000
                        )
                    }
                )
        )
    }
}
```

## 🛠️ Custom VoiceUI Components

### Creating Voice-Enabled Wrappers

```kotlin
// VoiceUIComponents.kt

@Composable
fun VoiceUITextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    voiceCommand: String? = null,
    voiceDictation: Boolean = true,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val voiceUI = LocalVoiceUI.current
    val focusRequester = remember { FocusRequester() }
    
    // Register voice command to focus this field
    voiceCommand?.let { command ->
        DisposableEffect(command) {
            val id = voiceUI.voiceCommandSystem.registerTarget(
                VoiceTarget(
                    name = label,
                    type = "textfield",
                    actions = mapOf(
                        "focus" to { _ -> 
                            focusRequester.requestFocus()
                            if (voiceDictation) {
                                startVoiceDictation(onValueChange)
                            }
                        }
                    )
                )
            )
            
            onDispose {
                voiceUI.voiceCommandSystem.unregisterTarget(id)
            }
        }
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    voiceUI.hudSystem.showNotification(
                        if (voiceDictation) 
                            "Say your text or type"
                        else 
                            "Type your $label",
                        duration = 2000
                    )
                }
            },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = if (voiceDictation) {
            {
                IconButton(onClick = { startVoiceDictation(onValueChange) }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice input")
                }
            }
        } else null
    )
}

@Composable
fun VoiceUIButton(
    text: String,
    voiceCommand: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val voiceUI = LocalVoiceUI.current
    
    // Register voice command
    voiceCommand?.let { command ->
        DisposableEffect(command, enabled) {
            if (enabled) {
                val id = voiceUI.voiceCommandSystem.registerTarget(
                    VoiceTarget(
                        name = text,
                        type = "button",
                        actions = mapOf("click" to { _ -> onClick() })
                    )
                )
                
                onDispose {
                    voiceUI.voiceCommandSystem.unregisterTarget(id)
                }
            } else {
                onDispose { }
            }
        }
    }
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .semantics {
                contentDescription = "$text button. Say '$voiceCommand' to activate"
            }
    ) {
        Text(text)
    }
}
```

## 🔄 Migration Patterns

### Pattern 1: Gradual Migration
```kotlin
// Keep existing UI, add voice on top
@Composable
fun HybridScreen() {
    val voiceUI = LocalVoiceUI.current
    
    // Existing Compose UI
    ExistingComposeScreen()
    
    // Add voice layer
    LaunchedEffect(Unit) {
        voiceUI.voiceCommandSystem.registerCommand("help", "SHOW_HELP")
        voiceUI.voiceCommandSystem.commandFlow.collect { command ->
            when (command.action) {
                "SHOW_HELP" -> showVoiceHelp()
            }
        }
    }
}
```

### Pattern 2: Full Replacement
```kotlin
// Replace entire screen with VoiceUI
class MainActivity : VoiceUIActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        // Remove setContentView(R.layout.activity_main)
        // Use VoiceUI instead
    }
    
    @Composable
    override fun ActivityContent() {
        VoiceUIMainScreen()
    }
}
```

### Pattern 3: Component by Component
```kotlin
// Replace components one at a time
@Composable
fun MixedScreen() {
    Column {
        // New VoiceUI component
        VoiceUITopBar(title = "My App")
        
        // Existing Compose component
        RegularComposeContent()
        
        // New VoiceUI component
        VoiceUIBottomNav()
    }
}
```

## 📋 Conversion Checklist

### For each screen/component:

- [ ] **Identify interactions** - What can users tap/click?
- [ ] **Add voice commands** - What would users say?
- [ ] **Add gestures** - What gestures make sense?
- [ ] **Add feedback** - Voice announcements, HUD notifications
- [ ] **Test accessibility** - Works without touch?
- [ ] **Add help** - How do users discover voice commands?

## 🎯 Best Practices

### 1. Voice Command Naming
```kotlin
// ✅ GOOD - Natural language
registerCommand("go back", "NAVIGATE_BACK")
registerCommand("save document", "SAVE_DOC")

// ❌ BAD - Technical jargon
registerCommand("nav_back", "NAVIGATE_BACK")
registerCommand("persist_data", "SAVE_DOC")
```

### 2. Progressive Enhancement
```kotlin
// Start with basic functionality
VoiceUIButton(
    text = "Submit",
    onClick = { submit() }
)

// Then add voice
VoiceUIButton(
    text = "Submit",
    voiceCommand = "submit form",
    onClick = { submit() }
)

// Then add gestures
VoiceUIButton(
    text = "Submit",
    voiceCommand = "submit form",
    onClick = { submit() },
    modifier = Modifier.voiceUIGestures(
        onDoubleTap = { quickSubmit() }
    )
)
```

### 3. Feedback for Every Action
```kotlin
// Always provide feedback
fun handleVoiceCommand(command: String) {
    // Visual feedback
    highlightElement()
    
    // Audio feedback
    playSound()
    
    // Voice feedback
    voiceUI.notificationSystem.showToast(
        "Command recognized: $command",
        voiceReadout = true
    )
}
```

## 🚀 Quick Start Template

```kotlin
// Template for converting any Activity
class YourActivity : VoiceUIActivity() {
    
    override fun configureVoiceUI() {
        super.configureVoiceUI()
        voiceUI.themeEngine.setTheme("material")
    }
    
    override fun registerVoiceCommands() {
        super.registerVoiceCommands()
        // Add your voice commands
    }
    
    override fun setupGestureHandlers() {
        super.setupGestureHandlers()
        // Add your gesture handlers
    }
    
    @Composable
    override fun ActivityContent() {
        // Your VoiceUI content
    }
}
```

---
**Status:** Complete Conversion Guide  
**Complexity:** Medium  
**Time to Convert:** 1-2 hours per screen  
**Benefits:** Voice control, gesture support, better accessibility
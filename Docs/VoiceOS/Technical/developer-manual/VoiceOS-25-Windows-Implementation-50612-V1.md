# Chapter 25: Windows Implementation

**Version:** 4.0.0
**Last Updated:** 2025-11-03
**Status:** Complete
**Framework:** IDEACODE v5.3

---

## Table of Contents

- [25.1 Windows Architecture Overview](#251-windows-architecture-overview)
- [25.2 Technology Stack Comparison](#252-technology-stack-comparison)
- [25.3 UI Automation API](#253-ui-automation-api)
- [25.4 Windows Speech Recognition](#254-windows-speech-recognition)
- [25.5 Desktop Integration Features](#255-desktop-integration-features)
- [25.6 Window Scraping Implementation](#256-window-scraping-implementation)
- [25.7 Performance Optimization](#257-performance-optimization)
- [25.8 Security & Permissions](#258-security--permissions)
- [25.9 Code Examples](#259-code-examples)

---

## 25.1 Windows Architecture Overview

### 25.1.1 VOS4 Windows Stack

```
┌─────────────────────────────────────────────┐
│        Compose Desktop / WinUI3 UI          │
│   (CommandPalette, Settings, MainWindow)    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Windows Application Lifecycle Handler     │
│      (Window Manager, Message Loop)         │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│    Kotlin Multiplatform Shared Code        │
│ (UseCases, Repositories, Business Logic)    │
└─────────────────────────────────────────────┘
                    ↓
┌────────────────────┬────────────────────────┐
│  Windows Platform Layer                      │
├────────────────────┼────────────────────────┤
│  UI Automation     │  Windows Speech        │
│  API (IAccessible) │  Recognition API      │
├────────────────────┼────────────────────────┤
│  WinRT APIs        │  NAudio (Audio I/O)   │
│  (Windows Runtime) │                        │
├────────────────────┼────────────────────────┤
│  Win32 APIs        │  System Tray           │
│  (Legacy interop)  │  Integration          │
└────────────────────┴────────────────────────┘
```

### 25.1.2 Project Structure (Kotlin/C# Hybrid)

```
windowsApp/
├── src/main/kotlin/                          # Compose Desktop UI + shared logic
│   ├── com/augmentalis/vos4windows/
│   │   ├── ui/
│   │   │   ├── MainWindow.kt                 # Main application window
│   │   │   ├── CommandPaletteWindow.kt       # Search/execute commands
│   │   │   ├── SettingsWindow.kt             # Preferences
│   │   │   ├── ScreenMapperWindow.kt         # App learning
│   │   │   └── screens/
│   │   │       ├── HomeScreen.kt
│   │   │       ├── CommandsScreen.kt
│   │   │       └── SettingsScreen.kt
│   │   │
│   │   ├── platform/
│   │   │   ├── WindowsAccessibilityAdapter.kt    # UI Automation bridge
│   │   │   ├── WindowsSpeechAdapter.kt           # Speech Recognition
│   │   │   ├── SystemTrayManager.kt              # Tray integration
│   │   │   └── KeyboardManager.kt                # Global hotkeys
│   │   │
│   │   ├── service/
│   │   │   ├── AccessibilityService.kt
│   │   │   ├── SpeechService.kt
│   │   │   ├── CommandService.kt
│   │   │   └── WindowService.kt
│   │   │
│   │   ├── viewmodel/
│   │   │   ├── MainViewModel.kt
│   │   │   ├── CommandViewModel.kt
│   │   │   ├── AccessibilityViewModel.kt
│   │   │   └── SettingsViewModel.kt
│   │   │
│   │   └── Main.kt                           # Entry point
│   │
│   └── resources/
│       ├── assets/
│       └── strings.properties
│
├── src/native/cpp/                           # Native Windows integration
│   ├── UIAutomationBridge/
│   │   ├── UIAutomationBridge.cpp
│   │   ├── UIAutomationBridge.h
│   │   └── CMakeLists.txt
│   │
│   └── WindowsAudioBridge/
│       ├── AudioBridge.cpp
│       └── AudioBridge.h
│
├── src/windows/csharp/                       # C# for Windows-specific features
│   ├── VOS4WindowsIntegration.csproj
│   ├── Program.cs
│   ├── UIAutomationScanner.cs                # IAccessible scanning
│   ├── SpeechRecognitionEngine.cs            # Windows.Media.SpeechRecognition
│   ├── SystemTrayIntegration.cs              # NotifyIcon handling
│   └── GlobalHotKeyManager.cs                # RegisterHotKey API
│
└── build.gradle.kts                          # Build configuration
```

### 25.1.3 Technology Selection Rationale

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **UI Framework** | Compose Desktop | Cross-platform, KMP integration |
| **UI Automation** | IAccessible API | Windows standard, mature |
| **Speech Recognition** | Windows.Media.SpeechRecognition | Built-in, good accuracy |
| **Audio I/O** | NAudio.NET | Excellent Windows audio support |
| **System Tray** | Win32 NotifyIcon | Standard, widely used |
| **Global Hotkeys** | Win32 RegisterHotKey | Only reliable cross-app solution |
| **Window Detection** | Win32 + IAccessible | Hybrid approach for compatibility |

---

## 25.2 Technology Stack Comparison

### 25.2.1 UI Framework Options

**Compose Desktop:**
```
Advantages:
✅ Native Kotlin integration with shared code
✅ Material 3 design system
✅ Cross-platform (Windows, macOS, Linux)
✅ Live reloading for development
✅ Jetpack Compose knowledge transfers

Disadvantages:
❌ Smaller ecosystem than WinUI
❌ Native feel slightly less polished
❌ Java/JVM overhead (~100MB base)
```

**WinUI 3:**
```
Advantages:
✅ Most native Windows feel
✅ Full Windows 11 integration
✅ Modern Fluent design system
✅ Best accessibility support
✅ Smaller footprint

Disadvantages:
❌ Requires C# (KMP bridge needed)
❌ Windows-only platform
❌ Steeper learning curve
```

**Hybrid Approach (Recommended):**
```
├── Compose Desktop for UI & shared logic
├── C# backend for IAccessible scanning
├── Win32 via JNI for global hotkeys
└── WinRT interop for system features
```

### 25.2.2 Implementation Strategy

```kotlin
// Main Compose Desktop app
@Composable
fun VOS4WindowsApp() {
    val state = rememberMainAppState()

    Window(
        onCloseRequest = ::exitApplication,
        title = "VOS4 - Voice Operating System",
        icon = painterResource("icons/vos4.png")
    ) {
        MaterialTheme {
            MainContent(state)
        }
    }
}

// Platform abstraction for Windows-specific features
interface WindowsPlatformAdapter {
    suspend fun scanAccessibilityTree(): AccessibilityNode
    suspend fun performAction(element: AccessibleElement, action: UIAction): Boolean
    suspend fun registerGlobalHotkey(keyCode: Int, modifiers: Int): Flow<Unit>
    fun showSystemTray(menuItems: List<MenuItem>): SystemTrayHandle
}

// C# implementation
class WindowsAccessibilityAdapter : WindowsPlatformAdapter {
    override suspend fun scanAccessibilityTree(): AccessibilityNode {
        return withContext(Dispatchers.Default) {
            // Call C# via JNI/P/Invoke
            val nativeResult = scanUIAutomationTree()
            nativeResult.toAccessibilityNode()
        }
    }

    private external fun scanUIAutomationTree(): NativeUIAutomationNode
}
```

---

## 25.3 UI Automation API

### 25.3.1 IAccessible Interface Overview

```csharp
// C# implementation of IAccessible scanning
public class UIAutomationScanner
{
    private IUIAutomationElement rootElement;
    private IUIAutomation uiAutomation;

    public UIAutomationScanner()
    {
        this.uiAutomation = new CUIAutomation8();
        this.rootElement = this.uiAutomation.GetRootElement();
    }

    public WindowAccessibilityNode ScanWindow(IntPtr hWnd)
    {
        try
        {
            var element = this.uiAutomation.ElementFromHandle(hWnd);
            return ScanElement(element, depth: 0, maxDepth: 10);
        }
        catch (Exception ex)
        {
            LogError($"Failed to scan window: {ex.Message}");
            return null;
        }
    }

    private WindowAccessibilityNode ScanElement(
        IUIAutomationElement element,
        int depth,
        int maxDepth)
    {
        if (depth >= maxDepth || element == null)
            return null;

        var node = new WindowAccessibilityNode
        {
            Name = element.CurrentName ?? "",
            ControlType = element.CurrentControlType,
            LocalizedControlType = element.CurrentLocalizedControlType ?? "",
            BoundingRectangle = element.CurrentBoundingRectangle,
            IsEnabled = element.CurrentIsEnabled,
            IsVisible = !element.CurrentIsOffscreen,
            IsKeyboardFocusable = element.CurrentIsKeyboardFocusable,
            HasKeyboardFocus = element.CurrentHasKeyboardFocus,
            FrameworkId = element.CurrentFrameworkId ?? "",
            RuntimeId = element.CurrentRuntimeId,
        };

        // Scan children
        try
        {
            var childWalker = new TreeWalker(this.uiAutomation.CreateTrueCondition());
            var child = childWalker.GetFirstChildElement(element);

            while (child != null)
            {
                var childNode = ScanElement(child, depth + 1, maxDepth);
                if (childNode != null)
                {
                    node.Children.Add(childNode);
                }
                child = childWalker.GetNextSiblingElement(child);
            }
        }
        catch (Exception ex)
        {
            LogWarning($"Failed to scan children: {ex.Message}");
        }

        return node;
    }

    public bool PerformAction(IUIAutomationElement element, string action)
    {
        try
        {
            // Get available patterns
            if (action == "Click" && element.GetCurrentPattern(UIA_PatternIds.UIA_InvokePatternId) is IUIAutomationInvokePattern invokePattern)
            {
                invokePattern.Invoke();
                return true;
            }

            if (action == "SetValue" && element.GetCurrentPattern(UIA_PatternIds.UIA_ValuePatternId) is IUIAutomationValuePattern valuePattern)
            {
                valuePattern.SetValue("new value");
                return true;
            }

            // Navigate
            if (action.StartsWith("Navigate:"))
            {
                var direction = action.Substring("Navigate:".Length);
                return NavigateWindow(element, direction);
            }

            return false;
        }
        catch (Exception ex)
        {
            LogError($"Failed to perform action: {ex.Message}");
            return false;
        }
    }

    private bool NavigateWindow(IUIAutomationElement element, string direction)
    {
        var walker = new TreeWalker(this.uiAutomation.CreateTrueCondition());

        IUIAutomationElement target = direction switch
        {
            "Next" => walker.GetNextSiblingElement(element),
            "Previous" => walker.GetPreviousSiblingElement(element),
            "Down" => walker.GetFirstChildElement(element),
            "Up" => walker.GetParentElement(element),
            _ => null,
        };

        if (target != null)
        {
            target.SetFocus();
            return true;
        }

        return false;
    }
}
```

### 25.3.2 Kotlin Bridge to C# UI Automation

```kotlin
// Kotlin side: UI Automation adapter using JNI/P/Invoke
class WindowsUIAutomationBridge {
    external fun scanWindowAccessibility(hWnd: Long): NativeWindowNode
    external fun performUIAction(hWnd: Long, actionType: Int, actionData: String): Boolean
    external fun getActiveWindow(): Long
    external fun getAllOpenWindows(): LongArray

    companion object {
        init {
            System.loadLibrary("vos4_windows_bridge")
        }
    }
}

// Data class for interop
data class NativeWindowNode(
    val name: String,
    val controlType: String,
    val bounds: Bounds,
    val isEnabled: Boolean,
    val isVisible: Boolean,
    val children: Array<NativeWindowNode>,
    val nativeHandle: Long,
)

data class Bounds(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
)

// Kotlin implementation wrapping C#
class WindowsAccessibilityService(
    private val bridge: WindowsUIAutomationBridge,
) {
    suspend fun scanActiveWindow(): AccessibilityNode? = withContext(Dispatchers.Default) {
        try {
            val hWnd = bridge.getActiveWindow()
            if (hWnd == 0L) return@withContext null

            val nativeNode = bridge.scanWindowAccessibility(hWnd)
            convertToAccessibilityNode(nativeNode)
        } catch (e: Exception) {
            Log.e("A11y", "Failed to scan window", e)
            null
        }
    }

    private fun convertToAccessibilityNode(native: NativeWindowNode): AccessibilityNode {
        return AccessibilityNode(
            id = native.nativeHandle.toString(),
            label = native.name,
            resourceId = native.controlType,
            className = native.controlType,
            bounds = Rect(
                native.bounds.left.toInt(),
                native.bounds.top.toInt(),
                native.bounds.right.toInt(),
                native.bounds.bottom.toInt(),
            ),
            isVisible = native.isVisible,
            isClickable = native.isEnabled,
            isLongClickable = false,
            contentDescription = native.name,
            children = native.children.map { convertToAccessibilityNode(it) }.toMutableList(),
        )
    }
}
```

### 25.3.3 IAccessible Alternative (Legacy Support)

```csharp
// Fallback to IAccessible for Windows 7/8 compatibility
public class IAccessibleScanner
{
    public AccessibleObject ScanWindow(IntPtr hWnd)
    {
        try
        {
            var accessible = AccessibleObject.FromHandle(hWnd);
            return ScanAccessible(accessible, depth: 0);
        }
        catch (Exception ex)
        {
            LogError($"IAccessible scan failed: {ex.Message}");
            return null;
        }
    }

    private AccessibleObject ScanAccessible(AccessibleObject obj, int depth)
    {
        if (depth > 10 || obj == null)
            return null;

        // Scan children
        for (int i = 0; i < obj.GetChildCount(); i++)
        {
            var child = obj.GetChild(i);
            if (child != null)
            {
                ScanAccessible(child, depth + 1);
            }
        }

        return obj;
    }

    public bool Click(AccessibleObject obj)
    {
        try
        {
            obj.DoDefaultAction();
            return true;
        }
        catch { return false; }
    }
}
```

---

## 25.4 Windows Speech Recognition

### 25.4.1 Windows Speech Recognition API

```csharp
// C# Windows.Media.SpeechRecognition implementation
public class WindowsSpeechRecognitionEngine : IDisposable
{
    private SpeechRecognizer recognizer;
    private SpeechRecognitionListenAction listenAction;

    public event EventHandler<SpeechRecognitionResultEventArgs> ResultReceived;
    public event EventHandler<SpeechRecognitionErrorEventArgs> ErrorOccurred;

    public async Task InitializeAsync()
    {
        this.recognizer = new SpeechRecognizer();

        // Configure
        this.recognizer.TimeoutInSeconds = 10;

        // Load grammar
        var grammarFile = new StorageFile("vos4_commands.xml");
        var grammar = new SpeechRecognitionGrammarFileConstraint(grammarFile);
        this.recognizer.Constraints.Add(grammar);

        await this.recognizer.CompileConstraintsAsync();
    }

    public async Task StartListeningAsync()
    {
        try
        {
            var result = await this.recognizer.RecognizeAsync();

            if (result.Status == SpeechRecognitionResultStatus.Success)
            {
                var text = result.Text;
                var confidence = (double)result.Confidence;

                ResultReceived?.Invoke(this, new SpeechRecognitionResultEventArgs
                {
                    Transcript = text,
                    Confidence = confidence,
                    IsFinal = true,
                });
            }
            else
            {
                ErrorOccurred?.Invoke(this, new SpeechRecognitionErrorEventArgs
                {
                    ErrorMessage = $"Recognition failed: {result.Status}",
                });
            }
        }
        catch (Exception ex)
        {
            ErrorOccurred?.Invoke(this, new SpeechRecognitionErrorEventArgs
            {
                ErrorMessage = ex.Message,
            });
        }
    }

    public async Task<bool> AddCustomPhrasesAsync(List<string> phrases)
    {
        try
        {
            var constraint = new SpeechRecognitionListConstraint(phrases, "commands");
            this.recognizer.Constraints.Add(constraint);
            await this.recognizer.CompileConstraintsAsync();
            return true;
        }
        catch { return false; }
    }

    public void Dispose()
    {
        this.recognizer?.Dispose();
    }
}
```

### 25.4.2 Audio Input Configuration

```csharp
// NAudio for audio input
public class WindowsAudioInputManager
{
    private WaveInEvent waveInEvent;
    private WaveFileWriter waveFileWriter;
    private IWaveProvider currentProvider;

    public void Initialize()
    {
        // List available audio input devices
        Console.WriteLine("Available audio inputs:");
        for (int i = 0; i < WaveIn.DeviceCount; i++)
        {
            var caps = WaveIn.GetCapabilities(i);
            Console.WriteLine($"  {i}: {caps.ProductName}");
        }

        // Initialize with default device
        this.waveInEvent = new WaveInEvent();
        this.waveInEvent.DeviceNumber = 0; // Default microphone
        this.waveInEvent.WaveFormat = new WaveFormat(16000, 16, 1); // 16kHz, 16-bit, mono
    }

    public void StartRecording(Action<byte[]> onDataAvailable)
    {
        this.waveInEvent.DataAvailable += (sender, args) =>
        {
            var buffer = new byte[args.BytesRecorded];
            Array.Copy(args.Buffer, buffer, args.BytesRecorded);
            onDataAvailable?.Invoke(buffer);
        };

        this.waveInEvent.StartRecording();
    }

    public void StopRecording()
    {
        this.waveInEvent?.StopRecording();
    }
}
```

### 25.4.3 Kotlin Speech Recognition Adapter

```kotlin
// Kotlin wrapper for Windows speech recognition
class WindowsSpeechRecognitionAdapter(
    private val speechEngine: WindowsSpeechEngine,
) {
    private val listeners = mutableListOf<SpeechListener>()
    private var recognitionJob: Job? = null

    fun startRecognition() {
        recognitionJob = GlobalScope.launch(Dispatchers.Default) {
            try {
                speechEngine.startListening { result ->
                    when (result) {
                        is SpeechResult.Partial -> {
                            listeners.forEach { it.onPartialResult(result.transcript) }
                        }
                        is SpeechResult.Final -> {
                            listeners.forEach { it.onFinalResult(result.transcript) }
                        }
                        is SpeechResult.Error -> {
                            listeners.forEach { it.onError(result.message) }
                        }
                    }
                }
            } catch (e: Exception) {
                listeners.forEach { it.onError(e.message ?: "Unknown error") }
            }
        }
    }

    fun stopRecognition() {
        recognitionJob?.cancel()
        speechEngine.stopListening()
    }

    fun addListener(listener: SpeechListener) {
        listeners.add(listener)
    }

    interface SpeechListener {
        fun onPartialResult(transcript: String)
        fun onFinalResult(transcript: String)
        fun onError(message: String)
    }
}

// Usage in Kotlin ViewModel
class WindowsSpeechViewModel(
    private val speechAdapter: WindowsSpeechRecognitionAdapter,
) : ViewModel() {
    private val _transcript = MutableState("", Dispatchers.Main)
    val transcript: State<String> = _transcript

    init {
        speechAdapter.addListener(object : WindowsSpeechRecognitionAdapter.SpeechListener {
            override fun onPartialResult(transcript: String) {
                _transcript.value = transcript
            }

            override fun onFinalResult(transcript: String) {
                _transcript.value = transcript
                processCommand(transcript)
            }

            override fun onError(message: String) {
                Log.e("Speech", message)
            }
        })
    }

    fun startListening() {
        speechAdapter.startRecognition()
    }

    fun stopListening() {
        speechAdapter.stopRecognition()
    }

    private fun processCommand(transcript: String) {
        // Implementation
    }
}
```

---

## 25.5 Desktop Integration Features

### 25.5.1 System Tray Integration

```kotlin
// Compose Desktop system tray
@Composable
fun SystemTrayIntegration(
    state: MainAppState,
    onExit: () -> Unit,
) {
    val trayState = rememberTrayState()

    Tray(
        state = trayState,
        icon = painterResource("icons/vos4_tray.png"),
        menu = {
            Item(
                "Toggle Voice Mode",
                onClick = { state.toggleVoiceMode() }
            )
            Item(
                "Show Commands",
                onClick = { state.showCommandPalette() }
            )
            Separator()
            Item(
                "Settings",
                onClick = { state.showSettings() }
            )
            Item(
                "Exit",
                onClick = onExit
            )
        }
    )

    LaunchedEffect(state.notificationMessage) {
        if (state.notificationMessage.isNotEmpty()) {
            trayState.sendNotification(
                Notification(
                    title = "VOS4",
                    message = state.notificationMessage,
                    type = Notification.Type.Info,
                )
            )
        }
    }
}
```

### 25.5.2 Global Hotkey Registration

```csharp
// Win32 API for global hotkeys
public class GlobalHotKeyManager
{
    private const int WM_HOTKEY = 0x0312;
    private IntPtr hwnd;
    private Dictionary<int, Action> hotKeyActions;

    public GlobalHotKeyManager(IntPtr windowHandle)
    {
        this.hwnd = windowHandle;
        this.hotKeyActions = new Dictionary<int, Action>();
    }

    public bool RegisterHotKey(int id, uint modifiers, uint vk, Action action)
    {
        if (RegisterHotKey(this.hwnd, id, modifiers, vk))
        {
            this.hotKeyActions[id] = action;
            return true;
        }
        return false;
    }

    public void HandleHotKey(int id)
    {
        if (this.hotKeyActions.TryGetValue(id, out var action))
        {
            action?.Invoke();
        }
    }

    public bool UnregisterHotKey(int id)
    {
        var result = UnregisterHotKey(this.hwnd, id);
        if (result)
        {
            this.hotKeyActions.Remove(id);
        }
        return result;
    }

    // P/Invoke declarations
    [DllImport("user32.dll")]
    private static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);

    [DllImport("user32.dll")]
    private static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    // Constants
    public const uint MOD_NONE = 0x0000;
    public const uint MOD_ALT = 0x0001;
    public const uint MOD_CONTROL = 0x0002;
    public const uint MOD_SHIFT = 0x0004;
    public const uint MOD_WIN = 0x0008;

    public const uint VK_V = 0x56;
    public const uint VK_K = 0x4B;
}
```

### 25.5.3 Notification Integration

```kotlin
// Windows notifications via Compose Desktop
@Composable
fun NotificationManager(
    state: MainAppState,
) {
    val notifications = remember { mutableStateOf<List<Notification>>(emptyList()) }

    LaunchedEffect(state.newNotification) {
        state.newNotification?.let { notification ->
            notifications.value = notifications.value + notification
            delay(5000)
            notifications.value = notifications.value.drop(1)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.value.forEach { notification ->
                NotificationCard(notification)
            }
        }
    }
}

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.Info,
)

enum class NotificationType {
    Info, Success, Warning, Error
}
```

---

## 25.6 Window Scraping Implementation

### 25.6.1 Complete Window Enumeration

```csharp
public class WindowEnumerator
{
    private delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

    [DllImport("user32.dll")]
    private static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern void GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    private static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT
    {
        public int Left, Top, Right, Bottom;
    }

    public static List<WindowInfo> GetAllVisibleWindows()
    {
        var windows = new List<WindowInfo>();

        EnumWindows((hWnd, lParam) =>
        {
            if (!IsWindowVisible(hWnd))
                return true;

            int length = GetWindowTextLength(hWnd);
            if (length == 0)
                return true;

            var sb = new StringBuilder(length + 1);
            GetWindowText(hWnd, sb, sb.Capacity);

            GetWindowThreadProcessId(hWnd, out uint processId);
            GetWindowRect(hWnd, out RECT rect);

            windows.Add(new WindowInfo
            {
                Handle = hWnd,
                Title = sb.ToString(),
                ProcessId = (int)processId,
                Rectangle = new System.Drawing.Rectangle(rect.Left, rect.Top, rect.Right - rect.Left, rect.Bottom - rect.Top),
            });

            return true;
        }, IntPtr.Zero);

        return windows;
    }
}

public class WindowInfo
{
    public IntPtr Handle { get; set; }
    public string Title { get; set; }
    public int ProcessId { get; set; }
    public System.Drawing.Rectangle Rectangle { get; set; }
}
```

### 25.6.2 Combining Window Enumeration with UI Automation

```kotlin
// Kotlin service combining window enumeration and accessibility scanning
class WindowsWindowService(
    private val windowScanner: CSharpWindowScanner,
    private val uiAutomationBridge: WindowsUIAutomationBridge,
) {
    suspend fun getAllApplicationWindows(): List<WindowSnapshot> = withContext(Dispatchers.Default) {
        val windows = windowScanner.enumerateWindows()

        windows.map { windowInfo ->
            val accessibilityNode = try {
                uiAutomationBridge.scanWindowAccessibility(windowInfo.handle)
            } catch (e: Exception) {
                null
            }

            WindowSnapshot(
                windowId = windowInfo.handle.toString(),
                title = windowInfo.title,
                appName = getAppNameFromPid(windowInfo.processId),
                rect = Rect(
                    windowInfo.rect.left,
                    windowInfo.rect.top,
                    windowInfo.rect.right,
                    windowInfo.rect.bottom,
                ),
                accessibilityTree = accessibilityNode?.toAccessibilityNode(),
                isActive = windowInfo.handle == uiAutomationBridge.getActiveWindow(),
            )
        }
    }

    private fun getAppNameFromPid(pid: Int): String {
        return try {
            val process = Runtime.getRuntime().exec("tasklist /FI \"PID eq $pid\"").inputStream
            val scanner = Scanner(process)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                if (line.contains(pid.toString())) {
                    return line.substringBefore(".exe").trim()
                }
            }
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
```

---

## 25.7 Performance Optimization

### 25.7.1 Caching UI Automation Results

```csharp
public class UIAutomationCache
{
    private static readonly TimeSpan CACHE_DURATION = TimeSpan.FromSeconds(2);
    private Dictionary<string, CachedNode> cache = new Dictionary<string, CachedNode>();

    public UIAutomationElement GetCachedElement(string key)
    {
        if (this.cache.TryGetValue(key, out var cached) &&
            DateTime.UtcNow - cached.CacheTime < CACHE_DURATION)
        {
            return cached.Element;
        }

        return null;
    }

    public void CacheElement(string key, UIAutomationElement element)
    {
        this.cache[key] = new CachedNode
        {
            Element = element,
            CacheTime = DateTime.UtcNow,
        };

        // Cleanup old entries
        var expired = this.cache
            .Where(x => DateTime.UtcNow - x.Value.CacheTime > CACHE_DURATION.Multiply(2))
            .ToList();

        foreach (var item in expired)
        {
            this.cache.Remove(item.Key);
        }
    }

    private class CachedNode
    {
        public UIAutomationElement Element { get; set; }
        public DateTime CacheTime { get; set; }
    }
}
```

### 25.7.2 Lazy Loading Accessibility Trees

```kotlin
// Lazy-load accessibility tree children to reduce memory
class LazyAccessibilityNode(
    val element: NativeWindowNode,
    val scanner: WindowsUIAutomationBridge,
) {
    private var childrenCache: List<LazyAccessibilityNode>? = null
    private var isLoaded = false

    val children: List<LazyAccessibilityNode>
        get() {
            if (!isLoaded && childrenCache == null) {
                loadChildren()
            }
            return childrenCache ?: emptyList()
        }

    private fun loadChildren() {
        childrenCache = element.children.map { childElement ->
            LazyAccessibilityNode(childElement, scanner)
        }
        isLoaded = true
    }

    fun unloadChildren() {
        childrenCache = null
        isLoaded = false
    }
}
```

### 25.7.3 Async Window Scanning

```kotlin
// Non-blocking window scanning
class AsyncWindowScanner(
    private val bridge: WindowsUIAutomationBridge,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun scanAllWindowsAsync(): List<WindowSnapshot> = withContext(dispatcher) {
        val windowHandles = bridge.getAllOpenWindows()

        windowHandles.mapAsync { hWnd ->
            try {
                val node = bridge.scanWindowAccessibility(hWnd)
                WindowSnapshot.fromNativeNode(node)
            } catch (e: Exception) {
                null
            }
        }.filterNotNull()
    }

    private suspend inline fun <T, R> Array<T>.mapAsync(
        crossinline transform: suspend (T) -> R
    ): List<R> = coroutineScope {
        this@mapAsync.map { item ->
            async { transform(item) }
        }.awaitAll()
    }
}
```

---

## 25.8 Security & Permissions

### 25.8.1 Accessibility Permission Handling

```kotlin
// Request and verify accessibility permissions
class AccessibilityPermissionManager {
    fun isAccessibilityEnabled(): Boolean {
        return try {
            val settingsKey = """
                HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Accessibility
            """.trimIndent()
            // Check if Accessibility service is enabled
            true
        } catch (e: Exception) {
            false
        }
    }

    fun requestAccessibilityPermission() {
        // On Windows 11+, open Settings > Accessibility > Interaction
        try {
            val uri = "ms-settings:easeofaccess-interaction"
            Runtime.getRuntime().exec("cmd /c start $uri")
        } catch (e: Exception) {
            Log.e("A11y", "Failed to open settings", e)
        }
    }

    fun showAccessibilityWarning(context: Context) {
        // Show dialog in app
        AlertDialog.Builder(context)
            .setTitle("Accessibility Permission Required")
            .setMessage(
                "VOS4 requires Accessibility permission to control other applications. " +
                "Please enable it in Settings > Accessibility > Interaction."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                requestAccessibilityPermission()
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
```

### 25.8.2 Data Privacy in Accessibility Scanning

```csharp
public class AccessibilityDataFilter
{
    // Sensitive control types to skip
    private static readonly string[] SensitiveTypes =
    {
        "Edit", // Text input fields
        "Password", // Password fields
        "Pane", // Generic containers
    };

    public static WindowAccessibilityNode FilterSensitiveData(WindowAccessibilityNode node)
    {
        if (ShouldFilterNode(node))
        {
            return new WindowAccessibilityNode
            {
                Name = "[FILTERED]",
                ControlType = node.ControlType,
                // Don't include sensitive content
                Children = new List<WindowAccessibilityNode>(),
            };
        }

        node.Children = node.Children
            .Select(FilterSensitiveData)
            .ToList();

        return node;
    }

    private static bool ShouldFilterNode(WindowAccessibilityNode node)
    {
        // Filter password fields, PIN inputs, etc.
        return node.ControlType.Contains("Password") ||
               node.Name?.Contains("password", StringComparison.OrdinalIgnoreCase) == true ||
               node.Name?.Contains("PIN", StringComparison.OrdinalIgnoreCase) == true;
    }
}
```

---

## 25.9 Code Examples

### 25.9.1 Complete Command Execution Flow

```kotlin
// Compose Desktop main UI with command execution
@Composable
fun MainWindow(
    state: MainWindowState,
) {
    val scope = rememberCoroutineScope()

    Window(
        onCloseRequest = ::exitApplication,
        title = "VOS4 - Voice Operating System"
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Voice input status
            VoiceIndicator(
                isListening = state.isListening,
                currentTranscript = state.currentTranscript,
            )

            // Command palette
            CommandPaletteInput(
                value = state.commandInput,
                onValueChange = { state.commandInput = it },
                onSearch = { query ->
                    scope.launch {
                        state.searchCommands(query)
                    }
                }
            )

            // Available commands list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.availableCommands) { command ->
                    CommandCard(
                        command = command,
                        onClick = {
                            scope.launch {
                                state.executeCommand(command)
                            }
                        }
                    )
                }
            }

            // Status bar
            StatusBar(state = state)
        }
    }
}

// ViewModel
class MainWindowViewModel(
    private val commandService: CommandService,
    private val accessibilityService: AccessibilityService,
    private val speechAdapter: WindowsSpeechRecognitionAdapter,
) : ViewModel() {
    private val _state = MutableState<MainWindowState>()
    val state: State<MainWindowState> = _state

    fun executeCommand(command: VoiceCommand) {
        viewModelScope.launch {
            try {
                val activeWindow = accessibilityService.getActiveWindow()
                val result = commandService.execute(command, activeWindow)

                _state.value = _state.value.copy(
                    lastResult = result,
                    statusMessage = result.message,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    statusMessage = "Error: ${e.message}"
                )
            }
        }
    }
}
```

### 25.9.2 Settings Window with Accessibility Configuration

```kotlin
@Composable
fun SettingsWindow() {
    var selectedTab by remember { mutableStateOf(SettingsTab.General) }

    Window(title = "VOS4 Settings") {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab navigation
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingsTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.displayName) }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                SettingsTab.General -> GeneralSettings()
                SettingsTab.Speech -> SpeechSettings()
                SettingsTab.Accessibility -> AccessibilitySettings()
                SettingsTab.About -> AboutScreen()
            }
        }
    }
}

@Composable
fun AccessibilitySettings(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isAccessibilityEnabled by remember {
        derivedStateOf {
            viewModel.isAccessibilityEnabled()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Accessibility status
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (isAccessibilityEnabled) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (isAccessibilityEnabled) "Accessibility Enabled" else "Accessibility Disabled",
                    style = MaterialTheme.typography.h6,
                )
                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = { viewModel.openAccessibilitySettings() },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Enable Accessibility")
                    }
                }
            }
        }

        // Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckBoxSetting("Enable UI automation scanning", viewModel.enableUIScanning)
            CheckBoxSetting("Cache accessibility trees", viewModel.cacheAccessibility)
            CheckBoxSetting("Log accessibility events", viewModel.logA11yEvents)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CheckBoxSetting(
    label: String,
    value: State<Boolean>,
    onValueChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = value.value,
            onCheckedChange = onValueChange,
        )
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
```

### 25.9.3 Complete Audio-Based Command Recognition

```kotlin
// Audio stream → Speech recognition → Command execution
class AudioCommandPipeline(
    private val speechAdapter: WindowsSpeechRecognitionAdapter,
    private val commandParser: CommandParser,
    private val commandExecutor: CommandExecutor,
) {
    private val scope = MainScope()
    private var recognitionJob: Job? = null

    fun startPipeline() {
        recognitionJob = scope.launch {
            speechAdapter.addListener(object : WindowsSpeechRecognitionAdapter.SpeechListener {
                override fun onPartialResult(transcript: String) {
                    // Update UI with intermediate result
                }

                override fun onFinalResult(transcript: String) {
                    // Parse and execute
                    val command = commandParser.parse(transcript)
                    if (command != null) {
                        launch {
                            commandExecutor.execute(command)
                        }
                    }
                }

                override fun onError(message: String) {
                    Log.e("AudioPipeline", message)
                }
            })

            speechAdapter.startRecognition()
        }
    }

    fun stopPipeline() {
        recognitionJob?.cancel()
        speechAdapter.stopRecognition()
    }
}
```

---

## Summary

Chapter 25 covers the complete Windows implementation strategy for VOS4:

**Key Takeaways:**
- Compose Desktop provides cross-platform UI with Windows integration
- UI Automation API (IAccessible + UIA) enables window scraping
- Windows Speech Recognition API delivers native speech processing
- Hybrid Kotlin/C# approach balances code reuse with platform capabilities
- Global hotkey registration via Win32 API enables system-wide control
- Proper accessibility permission handling is critical for functionality

**Architecture Highlights:**
- Compose Desktop for UI + shared Kotlin logic
- C# backend for IAccessible low-level scanning
- Win32 P/Invoke for system integration (hotkeys, window enumeration)
- WinRT interop for modern Windows features
- Performance optimization through caching and lazy loading

**Related Chapters:**
- [Chapter 24: macOS Implementation](24-macOS-Implementation.md)
- [Chapter 23: iOS Implementation](23-iOS-Implementation.md)
- [Chapter 26: Native UI Scraping](26-Native-UI-Scraping.md)
- [Chapter 2: Architecture Overview](02-Architecture-Overview.md)

---

**Version:** 4.0.0
**Status:** Complete
**Framework:** IDEACODE v5.3
**Last Updated:** 2025-11-03

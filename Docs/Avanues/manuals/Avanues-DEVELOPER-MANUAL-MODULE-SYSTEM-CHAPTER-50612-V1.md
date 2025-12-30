# AVAMagic Developer Manual - Module System Chapter

**Version:** 1.0.0
**Last Updated:** 2025-12-05
**Chapter:** AvaCode Module System
**Target Audience:** Software Engineers, Plugin Developers, Platform Integrators

---

## Table of Contents

1. [Introduction to AvaCode Modules](#1-introduction-to-avacode-modules)
2. [Architecture Overview](#2-architecture-overview)
3. [Module Registry](#3-module-registry)
4. [Built-in Modules](#4-built-in-modules)
5. [Creating Custom Modules](#5-creating-custom-modules)
6. [Platform Integration](#6-platform-integration)
7. [Tier Enforcement](#7-tier-enforcement)
8. [MEL Integration](#8-mel-integration)
9. [Testing Modules](#9-testing-modules)
10. [API Reference](#10-api-reference)

---

## 1. Introduction to AvaCode Modules

### What are AvaCode Modules?

AvaCode Modules expose platform-specific capabilities (voice recognition, device sensors, browser control, etc.) to MEL plugin developers through a unified `@module.method()` syntax.

```
┌─────────────────────────────────────────────────────────────────┐
│                    AVACODE MODULE ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  MEL Plugin                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ @voice.listen()           @device.screen.width()        │   │
│  │ @browser.open($url)       @command.execute("open app")  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Module Integration Layer                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ModuleRegistry → ModuleIntegration → ExpressionEvaluator│   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                  │
│  Platform Modules (commonMain)                                  │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐     │
│  │ @voice   │ @device  │ @browser │ @command │ @data    │     │
│  │ @app     │ @local   │          │          │          │     │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘     │
│                              ↓                                  │
│  Platform Delegates (androidMain, iosMain, etc.)               │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐     │
│  │ Android  │ iOS      │ Web      │ Desktop  │ Stub     │     │
│  │ Impl     │ Impl     │ Impl     │ Impl     │ (null)   │     │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### Key Benefits

| Benefit | Description |
|---------|-------------|
| **Unified API** | Same syntax works across all platforms |
| **Tier Safety** | DATA tier for iOS/Apple, LOGIC tier for full capabilities |
| **Delegate Pattern** | Platform-specific implementations injected at runtime |
| **Async Support** | Both sync and suspend methods supported |

---

## 2. Architecture Overview

### Core Components

```kotlin
// Location: Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/
//           com/augmentalis/magicelements/core/modules/

├── AvaCodeModule.kt      // Base interface for all modules
├── ModuleRegistry.kt     // Central module registration
├── BaseModule.kt         // Helper base class
├── ModuleIntegration.kt  // MEL parser integration
├── VoiceModule.kt        // @voice module
├── DeviceModule.kt       // @device module
├── CommandModule.kt      // @command module
├── DataModule.kt         // @data module
├── BrowserModule.kt      // @browser module
├── LocalizationModule.kt // @localization module
└── AppModule.kt          // @app module
```

### Interface Hierarchy

```kotlin
// Base interface all modules implement
interface AvaCodeModule {
    val name: String           // e.g., "voice", "device"
    val version: String        // e.g., "1.0.0"
    val minimumTier: PluginTier // DATA or LOGIC

    suspend fun execute(method: String, args: List<Any?>, tier: PluginTier): Any?
    fun isMethodAvailable(method: String, tier: PluginTier): Boolean
    fun listMethods(tier: PluginTier): List<ModuleMethod>
    suspend fun initialize()
    suspend fun dispose()
}

// Helper base class
abstract class BaseModule(
    override val name: String,
    override val version: String,
    override val minimumTier: PluginTier
) : AvaCodeModule {
    // Provides method registration DSL
    protected fun method(name: String, tier: PluginTier, description: String, handler: MethodHandler)

    // Argument parsing helpers
    protected fun List<Any?>.argString(index: Int, name: String): String
    protected fun List<Any?>.argNumber(index: Int, name: String): Number
    protected fun List<Any?>.argBoolean(index: Int, name: String): Boolean
    protected fun List<Any?>.argOptions(index: Int): Map<String, Any?>
}
```

---

## 3. Module Registry

### Registration

```kotlin
// Register modules at app startup
fun initializeModules(context: Context) {
    // Register with platform-specific delegates
    ModuleRegistry.register(VoiceModule(AndroidVoiceDelegate(context)))
    ModuleRegistry.register(DeviceModule(AndroidDeviceDelegate(context)))
    ModuleRegistry.register(CommandModule(AndroidCommandDelegate(context)))
    ModuleRegistry.register(DataModule(AndroidDataDelegate(context)))
    ModuleRegistry.register(BrowserModule(AndroidBrowserDelegate(context)))
    ModuleRegistry.register(LocalizationModule(AndroidLocalizationDelegate(context)))
    ModuleRegistry.register(AppModule(AndroidAppDelegate(context)))
}

// Register stub modules (for testing or unsupported platforms)
fun registerStubModules() {
    ModuleRegistry.register(VoiceModule(null))
    ModuleRegistry.register(DeviceModule(null))
    // ... etc
}
```

### Execution

```kotlin
// Execute a module method
suspend fun executeModuleCall() {
    // Direct API
    val result = ModuleRegistry.execute(
        module = "voice",
        method = "listen",
        args = emptyList(),
        tier = PluginTier.DATA
    )

    // Shorthand notation
    val result2 = ModuleRegistry.execute(
        call = "device.platform",
        args = emptyList(),
        tier = PluginTier.DATA
    )
}
```

### Querying

```kotlin
// Check if module exists
val hasVoice = ModuleRegistry.isRegistered("voice")

// Get module
val voiceModule = ModuleRegistry.get("voice")

// List all modules
val modules = ModuleRegistry.listModules()
// Returns: List<ModuleInfo> with name, version, tier, methodCount

// List methods for a module
val methods = ModuleRegistry.listMethods("device", PluginTier.DATA)

// Check method availability
val canListen = ModuleRegistry.isMethodAvailable("voice", "listen", PluginTier.DATA)
```

---

## 4. Built-in Modules

### @voice - Voice Recognition & TTS

```yaml
Module: voice
Version: 1.0.0
Minimum Tier: DATA

Methods:
  # DATA tier (iOS-safe)
  listen():              Start listening for speech, returns transcript
  speak(text):           Text-to-speech output
  isListening():         Check if currently listening
  engines():             List available speech engines

  # LOGIC tier (Android/other only)
  stop():                Stop listening
  setEngine(id):         Set speech recognition engine
  wake.enable(word):     Enable wake word detection
  wake.disable():        Disable wake word
  dictate.start():       Start continuous dictation
  dictate.stop():        Stop dictation
```

**Usage in MEL:**
```yaml
# Listen for speech
transcript: @voice.listen()

# Speak text
@voice.speak("Hello, world!")

# Check status
isActive: @voice.isListening()
```

### @device - Device Information & Sensors

```yaml
Module: device
Version: 1.0.0
Minimum Tier: DATA

Methods:
  # DATA tier
  info():                Get device info (model, manufacturer, os)
  platform():            Get platform name (android, ios, web, desktop)
  isTablet():            Check if device is tablet
  screen.width():        Screen width in dp
  screen.height():       Screen height in dp
  screen.density():      Screen pixel density
  battery.level():       Battery percentage (0-100)
  battery.isCharging():  Check if charging
  network.isConnected(): Check network connectivity
  network.type():        Get network type (wifi, cellular, none)

  # LOGIC tier
  bluetooth.isEnabled(): Check Bluetooth status
  bluetooth.scan():      Scan for Bluetooth devices
  wifi.ssid():           Get current WiFi SSID
  haptic(type):          Trigger haptic feedback
  audio.volume():        Get current volume
  audio.setVolume(v):    Set volume level
```

**Usage in MEL:**
```yaml
# Get device info
deviceName: @device.info().model

# Screen dimensions
width: @device.screen.width()
height: @device.screen.height()

# Network check
hasInternet: @device.network.isConnected()
```

### @command - Voice Command Execution

```yaml
Module: command
Version: 1.0.0
Minimum Tier: LOGIC

Methods:
  execute(cmd):          Execute a voice command
  list():                List all registered commands
  search(query):         Search commands by query
  suggest(context):      Get command suggestions
  history(limit):        Get command history

  # Advanced
  register(spec):        Register custom command
  unregister(id):        Remove custom command
  macro.create(n, cmds): Create command macro
  macro.execute(name):   Execute macro
  macro.list():          List all macros
  macro.delete(name):    Delete macro
```

**Usage in MEL:**
```yaml
# Execute command
@command.execute("open settings")

# Search commands
matches: @command.search("open")

# Create macro
@command.macro.create("morning", ["open calendar", "read notifications"])
```

### @data - Persistent Storage

```yaml
Module: data
Version: 1.0.0
Minimum Tier: DATA

Methods:
  # DATA tier
  get(key):              Get stored value
  set(key, value):       Store value
  remove(key):           Remove value
  has(key):              Check if key exists
  keys():                List all keys

  # LOGIC tier
  clear():               Clear all data
  secure.get(key):       Get from secure storage
  secure.set(key, val):  Store in secure storage
  secure.remove(key):    Remove from secure storage
  cache.get(key):        Get cached value
  cache.set(k, v, ttl):  Cache with TTL
  cache.clear():         Clear cache
  export(format):        Export data as AVU/CSV
  import(data, format):  Import data
```

**Usage in MEL:**
```yaml
# Basic storage
@data.set("username", $state.user.name)
savedName: @data.get("username")

# Secure storage for sensitive data
@data.secure.set("authToken", $token)

# Caching with TTL (5 minutes)
@data.cache.set("weather", $forecast, 300000)
```

### @browser - Browser Control

```yaml
Module: browser
Version: 1.0.0
Minimum Tier: DATA

Methods:
  # DATA tier
  open(url):             Open URL in browser
  search(query):         Search the web
  tab.list():            List open tabs
  tab.current():         Get current tab info
  bookmark.list():       List bookmarks
  history.list(limit):   Get browsing history
  page.title():          Get current page title
  page.url():            Get current page URL

  # LOGIC tier
  back():                Navigate back
  forward():             Navigate forward
  reload():              Reload page
  stop():                Stop loading
  tab.new(url):          Create new tab
  tab.close(id):         Close tab
  tab.switch(id):        Switch to tab
  bookmark.add(url):     Add bookmark
  bookmark.remove(url):  Remove bookmark
  history.clear():       Clear history
  page.getText():        Get page text content
  page.findText(q):      Find text on page
  page.scroll(dir, amt): Scroll page
  page.screenshot():     Take screenshot (base64)
  adblock.enable():      Enable ad blocker
  adblock.disable():     Disable ad blocker
  incognito.open(url):   Open in incognito
```

**Usage in MEL:**
```yaml
# Open URL
@browser.open("https://example.com")

# Search
@browser.search("kotlin multiplatform")

# Tab management
@browser.tab.new("https://docs.example.com")
tabs: @browser.tab.list()
```

### @localization - Internationalization

```yaml
Module: localization
Version: 1.0.0
Minimum Tier: DATA

Methods:
  t(key):                Translate key to current locale
  t(key, params):        Translate with parameters
  locale():              Get current locale
  setLocale(code):       Set locale
  availableLocales():    List available locales
  isRTL():               Check if RTL language
  direction():           Get text direction (ltr/rtl)
  format.number(n):      Format number for locale
  format.currency(n, c): Format currency
  format.date(d):        Format date
  format.time(t):        Format time
  format.relative(d):    Format relative time ("2 hours ago")
  plural(n, forms):      Get plural form
```

**Usage in MEL:**
```yaml
# Basic translation
greeting: @localization.t("hello_world")

# With parameters
welcome: @localization.t("welcome_user", { name: $state.userName })

# Formatting
price: @localization.format.currency($state.total, "USD")
date: @localization.format.date($state.createdAt)

# RTL support
direction: @localization.direction()
```

### @app - Cross-App Integration

```yaml
Module: app
Version: 1.0.0
Minimum Tier: DATA

Methods:
  # DATA tier
  clipboard.get():       Get clipboard content

  # LOGIC tier
  open(package):         Open another app
  openSettings(panel):   Open system settings
  share(data):           Share data via system share sheet
  share.to(app, data):   Share to specific app
  clipboard.set(text):   Copy to clipboard
  notification.show(t,b):Show notification
  notification.cancel(i):Cancel notification
  permission.check(p):   Check permission status
  permission.request(p): Request permission
  deepLink(url):         Handle deep link
```

**Usage in MEL:**
```yaml
# Share content
@app.share({ text: "Check out this article!", url: $article.url })

# Open settings
@app.openSettings("wifi")

# Clipboard
@app.clipboard.set($state.shareCode)

# Notifications
@app.notification.show("Download Complete", "Your file is ready")
```

---

## 5. Creating Custom Modules

### Step 1: Define the Module Interface

```kotlin
// commonMain
class MyCustomModule(
    private val delegate: MyCustomModuleDelegate?
) : BaseModule("custom", "1.0.0", PluginTier.DATA) {

    init {
        // DATA tier methods
        method("getData", PluginTier.DATA, "Get custom data") { args, _ ->
            requireDelegate()
            val key = args.argString(0, "key")
            delegate!!.getData(key)
        }

        // LOGIC tier methods
        method("performAction", PluginTier.LOGIC, "Perform action") { args, _ ->
            requireDelegate()
            val action = args.argString(0, "action")
            val options = args.argOptions(1)
            delegate!!.performAction(action, options)
        }
    }

    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(name, "", "Custom module not available on this platform")
        }
    }
}

// Delegate interface
interface MyCustomModuleDelegate {
    fun getData(key: String): Any?
    fun performAction(action: String, options: Map<String, Any?>): Boolean
}
```

### Step 2: Implement Platform Delegates

```kotlin
// androidMain
class AndroidCustomDelegate(
    private val context: Context
) : MyCustomModuleDelegate {

    override fun getData(key: String): Any? {
        // Android-specific implementation
        return context.getSharedPreferences("custom", Context.MODE_PRIVATE)
            .getString(key, null)
    }

    override fun performAction(action: String, options: Map<String, Any?>): Boolean {
        // Android-specific implementation
        return when (action) {
            "vibrate" -> {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                true
            }
            else -> false
        }
    }
}

// iosMain
class IOSCustomDelegate : MyCustomModuleDelegate {

    override fun getData(key: String): Any? {
        // iOS-specific implementation using NSUserDefaults
        return NSUserDefaults.standardUserDefaults.stringForKey(key)
    }

    override fun performAction(action: String, options: Map<String, Any?>): Boolean {
        // iOS-specific implementation
        return false // Action not supported on iOS
    }
}
```

### Step 3: Register the Module

```kotlin
// At app initialization
ModuleRegistry.register(MyCustomModule(AndroidCustomDelegate(context)))
```

---

## 6. Platform Integration

### Android Integration

```kotlin
// Application.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize modules with Android delegates
        ModuleRegistry.register(VoiceModule(AndroidVoiceDelegate(this)))
        ModuleRegistry.register(DeviceModule(AndroidDeviceDelegate(this)))
        ModuleRegistry.register(CommandModule(commandManager))
        ModuleRegistry.register(DataModule(AndroidDataDelegate(this)))
        ModuleRegistry.register(BrowserModule(browserAvanueDelegate))
        ModuleRegistry.register(LocalizationModule(AndroidLocalizationDelegate(this)))
        ModuleRegistry.register(AppModule(AndroidAppDelegate(this)))
    }
}
```

### iOS Integration

```kotlin
// iosMain/IOSModuleInitializer.kt
object IOSModuleInitializer : ModuleInitializer {
    override fun registerModules() {
        ModuleRegistry.register(VoiceModule(IOSVoiceDelegate()))
        ModuleRegistry.register(DeviceModule(IOSDeviceDelegate()))
        // Note: Some modules may have null delegates on iOS
        ModuleRegistry.register(CommandModule(null)) // Commands not available on iOS
        ModuleRegistry.register(DataModule(IOSDataDelegate()))
        ModuleRegistry.register(BrowserModule(IOSBrowserDelegate()))
        ModuleRegistry.register(LocalizationModule(IOSLocalizationDelegate()))
        ModuleRegistry.register(AppModule(IOSAppDelegate()))
    }

    override suspend fun cleanup() {
        ModuleRegistry.clear()
    }
}
```

---

## 7. Tier Enforcement

### Understanding Tiers

| Tier | Name | Description | Platforms |
|------|------|-------------|-----------|
| DATA | Tier 1 | Read-only, declarative operations. Safe for iOS App Store. | All |
| LOGIC | Tier 2 | Full control, imperative operations. May violate App Store guidelines. | Android, Web, Desktop |

### Tier Enforcement in Modules

```kotlin
class VoiceModule(delegate: VoiceModuleDelegate?) :
    BaseModule("voice", "1.0.0", PluginTier.DATA) {

    init {
        // DATA tier - available everywhere
        method("listen", PluginTier.DATA, "Listen for speech") { args, _ ->
            delegate?.listen()
        }

        method("isListening", PluginTier.DATA, "Check listening state") { _, _ ->
            delegate?.isListening() ?: false
        }

        // LOGIC tier - Android/Web/Desktop only
        method("wake.enable", PluginTier.LOGIC, "Enable wake word") { args, _ ->
            val wakeWord = args.argString(0, "wakeWord")
            delegate?.enableWakeWord(wakeWord)
        }
    }
}
```

### Runtime Tier Checking

```kotlin
// ExpressionEvaluator.kt
private fun evaluateModuleCall(node: ExpressionNode.ModuleCall): Any? {
    // Check if method is available at current tier
    if (!ModuleRegistry.isMethodAvailable(node.module, node.method, tier)) {
        throw EvaluationException(
            "Module method '@${node.module}.${node.method}' is not available in tier $tier"
        )
    }
    // ...
}
```

---

## 8. MEL Integration

### Syntax

```yaml
# Module call syntax
@module.method()                    # No arguments
@module.method(arg1)                # Single argument
@module.method(arg1, arg2)          # Multiple arguments
@module.nested.method()             # Nested method path

# Examples
transcript: @voice.listen()
platform: @device.platform()
width: @device.screen.width()
@browser.open("https://example.com")
greeting: @localization.t("hello", { name: $state.userName })
```

### Parser Integration

The module call syntax is parsed by the MEL expression parser:

```kotlin
// ExpressionLexer.kt - Tokenizes @module
char == '@' -> readAtToken()

// ExpressionParser.kt - Parses module.method()
if (match(TokenType.AT)) {
    val moduleName = previous().value
    return parseModuleCall(moduleName)
}

// ExpressionNode.kt - AST node
data class ModuleCall(
    val module: String,      // "voice"
    val method: String,      // "listen" or "screen.width"
    val args: List<ExpressionNode>
) : ExpressionNode()

// ExpressionEvaluator.kt - Executes module call
is ExpressionNode.ModuleCall -> evaluateModuleCall(node)
```

### Complete Plugin Example

```yaml
@mel/1.0
id: com.example.voice-notes
name: Voice Notes
tier: logic

state:
  notes: []
  isRecording: false
  currentTranscript: ""
  locale: "en-US"

reducers:
  startRecording:
    - isRecording = true
    - currentTranscript = @voice.listen()

  stopRecording:
    - isRecording = false

  saveNote:
    - notes = $array.push($state.notes, {
        text: $state.currentTranscript,
        timestamp: @device.info().timestamp,
        locale: $state.locale
      })
    - currentTranscript = ""

  deleteNote:
    params: [index]
    - notes = $array.removeAt($state.notes, $index)

  shareNote:
    params: [index]
    - @app.share({
        text: $array.get($state.notes, $index).text,
        title: @localization.t("share_note")
      })

  changeLocale:
    params: [code]
    - locale = $code
    - @localization.setLocale($code)

ui:
  Col(p:16, gap:12) {
    Row(justify:between, align:center) {
      Text(@localization.t("voice_notes"), style:headlineMedium)
      Btn(
        text:$if($state.isRecording, @localization.t("stop"), @localization.t("record")),
        @tap->$if($state.isRecording, stopRecording, startRecording),
        variant:$if($state.isRecording, "error", "primary")
      )
    }

    $if($state.currentTranscript != "") {
      Card(p:12) {
        Text($state.currentTranscript)
        Row(gap:8) {
          Btn(text:@localization.t("save"), @tap->saveNote)
          Btn(text:@localization.t("discard"), @tap->$set(currentTranscript, ""))
        }
      }
    }

    $for(note, index in $state.notes) {
      Card(p:12) {
        Text($note.text)
        Text(@localization.format.relative($note.timestamp), style:caption)
        Row(gap:8) {
          IconBtn(icon:share, @tap->shareNote($index))
          IconBtn(icon:delete, @tap->deleteNote($index))
        }
      }
    }
  }
```

---

## 9. Testing Modules

### Unit Testing

```kotlin
class ModuleIntegrationTest {

    @Test
    fun testModuleCallParsing() {
        val call = ModuleIntegration.parseModuleCall("@voice.listen()")
        assertNotNull(call)
        assertEquals("voice", call.module)
        assertEquals("listen", call.method)
    }

    @Test
    fun testNestedMethodParsing() {
        val call = ModuleIntegration.parseModuleCall("@device.screen.width()")
        assertNotNull(call)
        assertEquals("device", call.module)
        assertEquals("screen.width", call.method)
    }

    @Test
    fun testTierEnforcement() = runBlocking {
        registerStubModules()

        // DATA tier should have access to DATA methods
        assertTrue(ModuleRegistry.isMethodAvailable("device", "info", PluginTier.DATA))

        // DATA tier should NOT have access to LOGIC methods
        assertFalse(ModuleRegistry.isMethodAvailable("command", "execute", PluginTier.DATA))

        // LOGIC tier should have access to both
        assertTrue(ModuleRegistry.isMethodAvailable("command", "execute", PluginTier.LOGIC))
    }
}
```

### Integration Testing

```kotlin
class ModuleExecutionTest {

    @Test
    fun testVoiceModuleExecution() = runBlocking {
        // Create mock delegate
        val mockDelegate = object : VoiceModuleDelegate {
            override suspend fun listen() = "Hello, world!"
            override fun isListening() = false
            // ... other methods
        }

        ModuleRegistry.register(VoiceModule(mockDelegate))

        val result = ModuleRegistry.execute(
            module = "voice",
            method = "listen",
            args = emptyList(),
            tier = PluginTier.DATA
        )

        assertEquals("Hello, world!", result)
    }
}
```

---

## 10. API Reference

### ModuleRegistry

| Method | Description |
|--------|-------------|
| `register(module)` | Register a module |
| `unregister(name)` | Unregister a module |
| `get(name)` | Get registered module |
| `isRegistered(name)` | Check if module exists |
| `execute(module, method, args, tier)` | Execute module method |
| `execute(call, args, tier)` | Execute with shorthand ("module.method") |
| `listModules()` | List all registered modules |
| `listMethods(module, tier)` | List methods for module at tier |
| `isMethodAvailable(module, method, tier)` | Check method availability |
| `clear()` | Clear all modules |

### ModuleIntegration

| Method | Description |
|--------|-------------|
| `isModuleCall(expr)` | Check if expression is module call |
| `parseModuleCall(expr)` | Parse module call expression |
| `execute(call, args, tier)` | Execute parsed module call |
| `executeExpression(expr, args, tier)` | Parse and execute |
| `getAvailableModules(tier)` | Get modules available at tier |
| `getAvailableMethods(module, tier)` | Get methods available at tier |
| `isValidCall(expr, tier)` | Validate module call |

### Exceptions

| Exception | Description |
|-----------|-------------|
| `ModuleException` | Base module error |
| `ModuleTierException` | Tier access violation |
| `ModuleMethodNotFoundException` | Method not found |
| `ModuleArgumentException` | Invalid arguments |

---

## Next Steps

- See the [User Manual Module Chapter](USER-MANUAL-MODULE-SYSTEM-CHAPTER.md) for end-user documentation
- See [Plugin Development Guide](DEVELOPER-MANUAL.md#20-plugin-development) for complete plugin creation
- See [Platform Integration](DEVELOPER-MANUAL.md#6-platform-renderers) for platform-specific setup

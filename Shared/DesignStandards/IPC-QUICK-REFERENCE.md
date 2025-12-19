# IPC Foundation Quick Reference Card

**For**: Developers integrating IPC Foundation modules
**Version**: 1.0.0
**Last Updated**: 2025-11-10

---

## 🚀 5-Minute Integration

### 1. Add Dependencies (build.gradle.kts)
```kotlin
dependencies {
    implementation(project(":modules:MagicIdea:Components:ARGScanner"))
    implementation(project(":modules:MagicIdea:Components:VoiceCommandRouter"))
    implementation(project(":modules:MagicIdea:Components:IPCConnector"))
}
```

### 2. Create Manager Class
```kotlin
class YourCommandManager(private val context: Context) {
    private lateinit var registry: ARGRegistry
    private lateinit var router: VoiceCommandRouter
    private val connector = ConnectionManager(context)

    suspend fun initialize() {
        val scanner = ARGScanner(context)
        registry = scanner.scanManifests()
        router = VoiceCommandRouter(registry)
        registerCommands()
    }

    private fun registerCommands() {
        router.registerCommand(
            pattern = "open (.+)",
            targetCapability = "navigation",
            handler = ::handleOpenUrl
        )
    }

    private suspend fun handleOpenUrl(params: Map<String, Any>): Boolean {
        val url = params["1"] as? String ?: return false
        val service = registry.getServicesByCapability("navigation").firstOrNull()
            ?: return false

        val connection = connector.connect(service)
        // ... invoke method
        return true
    }

    suspend fun processCommand(command: String) = router.route(command)
}
```

### 3. Use in ViewModel
```kotlin
class YourViewModel : ViewModel() {
    private lateinit var manager: YourCommandManager

    fun init(context: Context) {
        viewModelScope.launch {
            manager = YourCommandManager(context)
            manager.initialize()
        }
    }

    fun execute(command: String) {
        viewModelScope.launch {
            manager.processCommand(command)
        }
    }
}
```

---

## 📖 Common Patterns

### Discover All Services
```kotlin
val scanner = ARGScanner(context)
val registry = scanner.scanManifests()
val services = registry.getAllServices()
```

### Find Service by Capability
```kotlin
val service = registry.getServicesByCapability("navigation").firstOrNull()
```

### Register Command Pattern
```kotlin
router.registerCommand(
    pattern = "command pattern (.+)",
    targetCapability = "capability_name",
    handler = ::handlerFunction
)
```

### Invoke Method via IPC
```kotlin
val connection = connector.connect(service)
val invocation = MethodInvocation("methodName", mapOf("key" to "value"))
val result = connector.invoke(connectionId, invocation)
connector.disconnect(connectionId)
```

---

## 🔍 Service Discovery Meta-Data

### In Your AndroidManifest.xml
```xml
<service android:name=".YourService" android:exported="true">
    <meta-data
        android:name="voiceos.service.capabilities"
        android:value="cap1,cap2,cap3" />
    <meta-data
        android:name="voiceos.service.type"
        android:value="your_type" />
    <meta-data
        android:name="voiceos.service.version"
        android:value="1.0.0" />
    <intent-filter>
        <action android:name="com.yourapp.action.YOUR_SERVICE" />
    </intent-filter>
</service>
```

---

## 🎯 Command Pattern Examples

| Pattern | Matches | Parameters |
|---------|---------|------------|
| `open (.+)` | "open google.com" | {"1": "google.com"} |
| `search (.+) on (.+)` | "search cats on google" | {"1": "cats", "2": "google"} |
| `(open\|go to) (.+)` | "open X" or "go to X" | {"1": "X"} |
| `bookmark (this\|current) page` | "bookmark this page" | {} |

---

## ⚠️ Common Mistakes

### ❌ Don't: Forget to initialize
```kotlin
val manager = YourCommandManager(context)
manager.processCommand("open X") // Will fail! Not initialized
```

### ✅ Do: Initialize first
```kotlin
val manager = YourCommandManager(context)
manager.initialize() // Initialize first
manager.processCommand("open X") // Now works
```

### ❌ Don't: Forget to disconnect
```kotlin
val connection = connector.connect(service)
// ... use connection
// Forgot to disconnect - LEAK!
```

### ✅ Do: Always disconnect
```kotlin
val connection = connector.connect(service)
try {
    // ... use connection
} finally {
    connector.disconnect(connectionId)
}
```

### ❌ Don't: Block main thread
```kotlin
runBlocking {
    manager.initialize() // Blocks UI!
}
```

### ✅ Do: Use coroutines
```kotlin
viewModelScope.launch {
    manager.initialize() // Non-blocking
}
```

---

## 🧪 Testing Checklist

- [ ] Services discovered on initialization
- [ ] Commands match registered patterns
- [ ] IPC connections succeed
- [ ] Methods execute without errors
- [ ] Cleanup (shutdown) called on destroy
- [ ] Error handling works correctly

---

## 🔗 Full Documentation

**Complete Guide**: `GlobalDesignStandard-IPC-Integration-Guide.md` (11,000+ words)

**Demo App**: `/apps/ipc-foundation-demo/`
- Android app with complete implementation
- HTML demo with interactive visualization
- Comprehensive README and docs

**Other Standards**:
- `GlobalDesignStandard-IPC-Architecture.md` - IPC patterns
- `GlobalDesignStandard-Module-Structure.md` - Module layout
- `GlobalDesignStandard-UI-Patterns.md` - UI best practices

---

## 💡 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| No services found | Check AndroidManifest meta-data tags |
| Command not matching | Test regex pattern in isolation |
| Connection fails | Verify service is exported and running |
| Method invocation fails | Check method name and parameter types |
| Memory leak | Always call `shutdown()` in `onCleared()` |

---

**Need Help?** See full guide: `GlobalDesignStandard-IPC-Integration-Guide.md`

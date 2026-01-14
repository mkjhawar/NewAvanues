/**
 * Registry vs Factory vs Builder - Pattern Comparison
 * Path: /ProjectDocs/Analysis/Registry-vs-Factory-vs-Builder-Patterns.md
 * 
 * Created: 2024-08-22
 * Last Modified: 2024-08-22
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Clarify differences between Registry, Factory, and Builder patterns
 * Module: System
 * 
 * Changelog:
 * - v1.0.0 (2024-08-22): Initial creation
 */

# Registry vs Factory vs Builder - What's the Difference?

## Quick Answer: They Serve Different Purposes

- **Registry** = Phone book (stores existing objects) 📞
- **Factory** = Assembly line (creates new objects) 🏭
- **Builder** = Custom shop (assembles complex objects step-by-step) 🔧

---

## Registry Pattern - Storage and Retrieval

### What It Does:
**Stores references to existing objects for later retrieval**

```kotlin
// Registry STORES things that already exist
class ServiceRegistry {
    private val services = mutableMapOf<String, Any>()
    
    fun register(name: String, service: Any) {
        services[name] = service  // Store for later
    }
    
    fun get(name: String): Any? {
        return services[name]  // Retrieve what was stored
    }
}

// Usage
val audioService = AudioService()
registry.register("audio", audioService)  // Store it

// Later, somewhere else...
val audio = registry.get("audio")  // Look it up
```

### When to Use Registry:
✅ **Good for:**
- Plugin systems (register available plugins)
- Command patterns (register available commands)
- Event handlers (register listeners)
- Service discovery

❌ **Problems:**
- Becomes a service locator (anti-pattern) if overused
- Hidden dependencies
- Runtime failures

### VOS4 Example - Command Registry (Good Use):
```kotlin
// ✅ GOOD: Registry for commands (data, not services)
class CommandRegistry {
    private val commands = mutableMapOf<String, CommandDefinition>()
    
    fun registerCommand(name: String, definition: CommandDefinition) {
        commands[name] = definition
    }
    
    fun findCommand(input: String): CommandDefinition? {
        return commands[input.lowercase()]
    }
}
```

---

## Factory Pattern - Object Creation

### What It Does:
**Creates new instances of objects with proper configuration**

```kotlin
// Factory CREATES new objects
class EngineFactory {
    fun createEngine(type: String, context: Context): RecognitionEngine {
        return when(type) {
            "vosk" -> VoskEngine(context)
            "google" -> GoogleEngine(context, getApiKey())
            "azure" -> AzureEngine(context, getSubscription())
            else -> throw IllegalArgumentException()
        }
    }
    
    private fun getApiKey() = // ... retrieve API key
    private fun getSubscription() = // ... retrieve subscription
}

// Usage - creates NEW instance each time
val engine1 = factory.createEngine("vosk", context)  // New instance
val engine2 = factory.createEngine("vosk", context)  // Different instance
```

### Is "Factory" the Right Term?

**Yes, but there are variations:**

1. **Simple Factory** (what we've been discussing)
2. **Factory Method** (inheritance-based)
3. **Abstract Factory** (families of objects)

**Alternative Names Often Used:**
- **Creator** - `EngineCreator`
- **Producer** - `EngineProducer`
- **Provider** - `EngineProvider` (though this implies singleton)
- **Generator** - `EngineGenerator`

---

## Builder Pattern - Step-by-Step Construction

### What It Does:
**Constructs complex objects step by step**

```kotlin
// Builder assembles objects piece by piece
class AudioSystemBuilder {
    private var sampleRate: Int = 16000
    private var noiseSuppression: Boolean = false
    private var echoCancellation: Boolean = false
    private var engine: String = "vosk"
    
    fun withSampleRate(rate: Int) = apply {
        sampleRate = rate
    }
    
    fun withNoiseSuppression() = apply {
        noiseSuppression = true
    }
    
    fun withEchoCancellation() = apply {
        echoCancellation = true
    }
    
    fun withEngine(type: String) = apply {
        engine = type
    }
    
    fun build(): AudioSystem {
        val config = AudioConfig(sampleRate, noiseSuppression, echoCancellation)
        val recognitionEngine = createEngine(engine)
        return AudioSystem(config, recognitionEngine)
    }
}

// Usage - fluent interface
val audioSystem = AudioSystemBuilder()
    .withSampleRate(44100)
    .withNoiseSuppression()
    .withEngine("google")
    .build()
```

---

## Comparison: Registry vs Factory

| Aspect | Registry | Factory |
|--------|----------|---------|
| **Purpose** | Store & retrieve existing objects | Create new objects |
| **When objects created** | Before registration | During factory call |
| **Object lifecycle** | Long-lived, shared | New instances (usually) |
| **Object identity** | Same object returned | New object each time |
| **Configuration** | Objects pre-configured | Factory configures |
| **Dependencies** | Hidden (looked up) | Explicit (injected) |
| **Testing** | Hard (global state) | Easy (local creation) |

### Code Example - Registry vs Factory:

```kotlin
// REGISTRY - stores existing objects
class ModuleRegistry {
    private val modules = mutableMapOf<String, Any>()
    
    init {
        // Objects created elsewhere and registered
        modules["audio"] = existingAudioModule
        modules["data"] = existingDataModule
    }
    
    fun getModule(name: String) = modules[name]  // Returns SAME instance
}

// FACTORY - creates new objects
class ModuleFactory {
    fun createAudioModule(context: Context): AudioModule {
        // Creates NEW instance with dependencies
        return AudioModule(
            AudioCapture(context),
            AudioProcessor(),
            AudioConfig.default()
        )
    }
}

// Usage difference:
val registry = ModuleRegistry()
val audio1 = registry.getModule("audio")
val audio2 = registry.getModule("audio")
// audio1 === audio2  (SAME object)

val factory = ModuleFactory()
val audio3 = factory.createAudioModule(context)
val audio4 = factory.createAudioModule(context)
// audio3 !== audio4  (DIFFERENT objects)
```

---

## What VOS4 Should Use

### 1. For Module Creation: Factory (or Creator/Producer)

```kotlin
/**
 * Creates and wires VOS4 system components
 * Name alternatives: VOS4Creator, VOS4Producer, VOS4Assembler
 */
object VOS4SystemFactory {
    fun createSpeechSystem(context: Context): SpeechRecognitionSystem {
        // Creates all components with proper dependencies
        val audioCapture = createAudioCapture(context)
        val engines = createEngines(context)
        val dataManager = createDataManager(context)
        
        return SpeechRecognitionSystem(
            audioCapture = audioCapture,
            engines = engines,
            dataManager = dataManager
        )
    }
}
```

### 2. For Commands: Registry (appropriate use)

```kotlin
/**
 * Registry for command definitions (data, not services)
 */
class CommandDefinitionRegistry {
    private val definitions = mutableMapOf<String, CommandDefinition>()
    
    fun register(command: CommandDefinition) {
        definitions[command.phrase] = command
    }
    
    fun findMatching(input: String): CommandDefinition? {
        // This is fine - registering data, not services
        return definitions[input]
    }
}
```

### 3. For Complex Configuration: Builder

```kotlin
/**
 * Builds complex recognition configuration
 */
class RecognitionConfigBuilder {
    fun withLanguage(lang: String) = apply { ... }
    fun withEngine(engine: String) = apply { ... }
    fun withCommands(commands: List<String>) = apply { ... }
    fun build(): RecognitionConfig { ... }
}
```

---

## Why CoreManager (Service Registry) is Problematic

```kotlin
// ❌ BAD: Service Registry (CoreManager approach)
object CoreManager {
    private val modules = mutableMapOf<String, Any>()
    
    fun registerModule(name: String, module: Any) {
        modules[name] = module
    }
    
    fun getModule(name: String) = modules[name]
}

// Problems:
// 1. Global state
// 2. Hidden dependencies
// 3. Runtime failures
// 4. Hard to test

// ✅ GOOD: Direct Injection via Factory
class SystemFactory {
    fun createSystem(context: Context): System {
        val audio = AudioManager(context)
        val speech = SpeechRecognition(audio)  // Direct injection
        return System(speech)
    }
}

// Benefits:
// 1. No global state
// 2. Clear dependencies
// 3. Compile-time safety
// 4. Easy to test
```

---

## Better Names for VOS4

Instead of "Factory", you could use:

### 1. **Assembler** - Emphasizes putting pieces together
```kotlin
object VOS4SystemAssembler {
    fun assemble(context: Context): VOS4System
}
```

### 2. **Creator** - Simple and clear
```kotlin
object VOS4SystemCreator {
    fun create(context: Context): VOS4System
}
```

### 3. **Builder** - If step-by-step configuration needed
```kotlin
class VOS4SystemBuilder {
    fun withAudioConfig(config: AudioConfig): Builder
    fun withEngines(engines: List<Engine>): Builder
    fun build(): VOS4System
}
```

### 4. **Injector** - Emphasizes dependency injection
```kotlin
object VOS4DependencyInjector {
    fun inject(context: Context): VOS4System
}
```

### 5. **Bootstrapper** - For initial system setup
```kotlin
object VOS4Bootstrapper {
    fun bootstrap(application: Application): VOS4System
}
```

---

## Recommendation for VOS4

### Use This Naming Convention:

1. **`VOS4SystemAssembler`** - Main system assembly
2. **`[Module]Creator`** - For individual module creation
3. **`[Feature]Builder`** - For complex configurations
4. **`[Data]Registry`** - ONLY for data/commands, not services

### Example Structure:

```kotlin
// Main system assembly
object VOS4SystemAssembler {
    fun assembleFull(context: Context): VOS4System {
        val audio = AudioModuleCreator.create(context)
        val speech = SpeechModuleCreator.create(context, audio)
        val commands = CommandModuleCreator.create(context)
        
        return VOS4System(audio, speech, commands)
    }
}

// Module creators
object AudioModuleCreator {
    fun create(context: Context): AudioModule {
        return AudioModule(
            capture = AudioCapture(context),
            processor = AudioProcessor(),
            config = AudioConfigBuilder()
                .withSampleRate(16000)
                .withNoiseSuppression()
                .build()
        )
    }
}

// Data registry (OK for data)
class CommandRegistry {
    fun register(definition: CommandDefinition)
    fun find(phrase: String): CommandDefinition?
}
```

---

## Summary

- **Registry** = Stores existing objects (avoid for services)
- **Factory/Creator** = Creates new objects (good for VOS4)
- **Builder** = Step-by-step construction (good for complex configs)
- **CoreManager** = Service Registry anti-pattern (remove it)

**For VOS4:** Use **Assembler/Creator** pattern with direct dependency injection, not Registry pattern for services.
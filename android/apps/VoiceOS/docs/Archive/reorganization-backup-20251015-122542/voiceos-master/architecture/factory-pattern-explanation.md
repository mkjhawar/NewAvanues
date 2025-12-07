/**
 * Factory Classes for Complex Creation - Explained
 * Path: /ProjectDocs/Analysis/Factory-Pattern-Explanation.md
 * 
 * Created: 2024-08-22
 * Last Modified: 2024-08-22
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Explain factory pattern usage in VOS4 architecture
 * Module: System
 * 
 * Changelog:
 * - v1.0.0 (2024-08-22): Initial creation
 */

# Factory Classes for Complex Creation - Explained

## What is a Factory Class?

A factory class is responsible for creating complex objects that require multiple steps, configurations, or dependencies. Instead of having complicated object creation scattered throughout your code, you centralize it in one place.

---

## Problem: Complex Object Creation

### Without Factory (Messy) ❌

```kotlin
// In MainActivity.kt
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        
        // 😱 This is a mess! Look at all this creation logic in the Activity!
        
        // First create audio config
        val audioConfig = AudioConfig(
            sampleRate = 16000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            noiseSuppression = true,
            echoCancellation = false
        )
        
        // Then create audio capture
        val audioCapture = AudioCapture(applicationContext, audioConfig)
        
        // Create all the engines (each with their own setup)
        val voskConfig = VoskConfig.default().withLanguage("en-US")
        val voskHandler = VoskHandler(eventBus)
        val voskManager = VoskManager(applicationContext, eventBus, voskHandler)
        val voskProcessor = VoskProcessor(applicationContext, eventBus, voskManager, voskHandler)
        val voskEngine = VoskEngine(
            context = applicationContext,
            config = voskConfig,
            handler = voskHandler,
            manager = voskManager,
            processor = voskProcessor
        )
        
        val googleEngine = GoogleCloudEngine(
            context = applicationContext,
            apiKey = BuildConfig.GOOGLE_API_KEY,
            // ... more setup
        )
        
        val azureEngine = AzureEngine(
            context = applicationContext,
            subscriptionKey = BuildConfig.AZURE_KEY,
            region = "westus",
            // ... more setup
        )
        
        // Create data manager with ObjectBox
        val dataManager = DataManager(
            context = applicationContext,
            boxStore = MyObjectBox.builder()
                .androidContext(applicationContext)
                .build()
        )
        
        // Finally create speech recognition with all dependencies
        val speechRecognition = SpeechRecognitionManager(
            audioCapture = audioCapture,
            engines = mapOf(
                "vosk" to voskEngine,
                "google" to googleEngine,
                "azure" to azureEngine
            ),
            dataManager = dataManager,
            eventBus = EventBus.getDefault()
        )
        
        // 😵 This is way too much code in an Activity!
    }
}
```

### Problems with this approach:
- ❌ **Violates Single Responsibility** - Activity shouldn't know how to create engines
- ❌ **Duplication** - Same creation code needed in tests, other activities
- ❌ **Hard to test** - Can't easily swap implementations
- ❌ **Difficult to maintain** - Changes require updating multiple places
- ❌ **Too much knowledge** - Activity knows internal details of engine creation

---

## Solution: Factory Classes ✅

### 1. Simple Factory

```kotlin
/**
 * Factory responsible for creating speech recognition components
 * Encapsulates all the complex creation logic
 */
object SpeechRecognitionFactory {
    
    /**
     * Creates a fully configured SpeechRecognitionManager
     * Hides all the complexity from the caller
     */
    fun create(context: Context): SpeechRecognitionManager {
        val audioCapture = createAudioCapture(context)
        val engines = createEngines(context)
        val dataManager = createDataManager(context)
        
        return SpeechRecognitionManager(
            audioCapture = audioCapture,
            engines = engines,
            dataManager = dataManager
        )
    }
    
    /**
     * Create audio capture with proper configuration
     */
    private fun createAudioCapture(context: Context): AudioCapture {
        val config = AudioConfig.forSpeechRecognition() // Predefined config
        return AudioCapture(context, config)
    }
    
    /**
     * Create all recognition engines
     */
    private fun createEngines(context: Context): Map<String, RecognitionEngine> {
        return mapOf(
            "vosk" to createVoskEngine(context),
            "google" to createGoogleEngine(context),
            "azure" to createAzureEngine(context),
            "android" to createAndroidEngine(context)
        )
    }
    
    /**
     * Create Vosk engine with all its components
     */
    private fun createVoskEngine(context: Context): VoskEngine {
        // All the complex Vosk setup hidden here
        val eventBus = EventBus.getDefault()
        val handler = VoskHandler(eventBus)
        val manager = VoskManager(context, eventBus, handler)
        val processor = VoskProcessor(context, eventBus, manager, handler)
        
        return VoskEngine(context, eventBus).apply {
            // Any additional setup
            initialize("en-US")
        }
    }
    
    /**
     * Create Google engine with API keys
     */
    private fun createGoogleEngine(context: Context): GoogleCloudEngine {
        return GoogleCloudEngine(
            context = context,
            apiKey = getApiKey("google"), // Centralized API key management
            config = GoogleCloudConfig.default()
        )
    }
    
    private fun createDataManager(context: Context): DataManager {
        val boxStore = MyObjectBox.builder()
            .androidContext(context)
            .build()
        return DataManager(context, boxStore)
    }
    
    /**
     * Get API keys from secure storage
     */
    private fun getApiKey(service: String): String {
        // Could read from BuildConfig, encrypted prefs, etc.
        return when(service) {
            "google" -> BuildConfig.GOOGLE_API_KEY
            "azure" -> BuildConfig.AZURE_SUBSCRIPTION_KEY
            else -> throw IllegalArgumentException("Unknown service: $service")
        }
    }
}

// Now in MainActivity - SO MUCH CLEANER! ✨
class MainActivity : AppCompatActivity() {
    private lateinit var speechRecognition: SpeechRecognitionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // One line! All complexity hidden in factory
        speechRecognition = SpeechRecognitionFactory.create(applicationContext)
        
        // Ready to use!
        speechRecognition.startRecognition()
    }
}
```

---

### 2. Factory with Configuration Options

```kotlin
/**
 * Factory that allows configuration while hiding complexity
 */
class AudioSystemFactory(private val context: Context) {
    
    data class Config(
        val enableNoiseSuppression: Boolean = true,
        val enableEchoCancellation: Boolean = false,
        val sampleRate: Int = 16000,
        val enginePreference: List<String> = listOf("vosk", "google", "android")
    )
    
    /**
     * Create with default configuration
     */
    fun createDefault(): AudioSystem {
        return create(Config())
    }
    
    /**
     * Create with custom configuration
     */
    fun create(config: Config): AudioSystem {
        // Create audio components based on config
        val audioConfig = AudioConfig(
            sampleRate = config.sampleRate,
            noiseSuppression = config.enableNoiseSuppression,
            echoCancellation = config.enableEchoCancellation
        )
        
        val audioCapture = AudioCapture(context, audioConfig)
        val audioProcessor = AudioProcessor(audioConfig)
        
        // Only create requested engines
        val engines = config.enginePreference.mapNotNull { engineType ->
            when(engineType) {
                "vosk" -> createVoskEngine()
                "google" -> createGoogleEngine()
                else -> null
            }
        }
        
        return AudioSystem(audioCapture, audioProcessor, engines)
    }
    
    /**
     * Create for specific use cases
     */
    fun createForDictation(): AudioSystem {
        return create(Config(
            enableNoiseSuppression = true,
            enableEchoCancellation = true,
            sampleRate = 44100,  // Higher quality for dictation
            enginePreference = listOf("google", "azure") // Cloud engines for accuracy
        ))
    }
    
    fun createForCommands(): AudioSystem {
        return create(Config(
            sampleRate = 16000,  // Lower sample rate is fine
            enginePreference = listOf("vosk", "android") // Offline engines for speed
        ))
    }
}

// Usage
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val factory = AudioSystemFactory(this)
        
        // Different configurations for different needs
        val dictationSystem = factory.createForDictation()
        val commandSystem = factory.createForCommands()
        val defaultSystem = factory.createDefault()
    }
}
```

---

### 3. Abstract Factory Pattern (for families of objects)

```kotlin
/**
 * Abstract factory for creating families of related objects
 */
interface RecognitionEngineFactory {
    fun createEngine(): RecognitionEngine
    fun createConfig(): EngineConfig
    fun createProcessor(): AudioProcessor
    fun createHandler(): EventHandler
}

/**
 * Concrete factory for Vosk family
 */
class VoskFactory(private val context: Context) : RecognitionEngineFactory {
    private val eventBus = EventBus.getDefault()
    
    override fun createEngine(): RecognitionEngine {
        return VoskEngine(context, eventBus)
    }
    
    override fun createConfig(): EngineConfig {
        return VoskConfig.default()
    }
    
    override fun createProcessor(): AudioProcessor {
        return VoskProcessor(context, eventBus)
    }
    
    override fun createHandler(): EventHandler {
        return VoskHandler(eventBus)
    }
    
    /**
     * Convenience method to create complete Vosk system
     */
    fun createComplete(): VoskSystem {
        return VoskSystem(
            engine = createEngine(),
            config = createConfig(),
            processor = createProcessor(),
            handler = createHandler()
        )
    }
}

/**
 * Concrete factory for Google Cloud family
 */
class GoogleCloudFactory(
    private val context: Context,
    private val apiKey: String
) : RecognitionEngineFactory {
    
    override fun createEngine(): RecognitionEngine {
        return GoogleCloudEngine(context, apiKey)
    }
    
    override fun createConfig(): EngineConfig {
        return GoogleCloudConfig.default()
    }
    
    override fun createProcessor(): AudioProcessor {
        return GoogleCloudProcessor(context)
    }
    
    override fun createHandler(): EventHandler {
        return GoogleCloudHandler()
    }
}
```

---

## Real VOS4 Example: Creating Speech Recognition System

```kotlin
/**
 * Factory for VOS4 Speech Recognition System
 * Handles all the complex wiring and initialization
 */
object VOS4SpeechFactory {
    
    /**
     * Create complete speech recognition system
     */
    fun createSpeechRecognitionSystem(context: Context): SpeechRecognitionSystem {
        // Step 1: Create shared dependencies
        val eventBus = EventBus.getDefault()
        val audioServices = createAudioServices(context)
        val dataManager = createDataManager(context)
        
        // Step 2: Create all engines
        val engines = createAllEngines(context, eventBus, audioServices)
        
        // Step 3: Create recognition manager
        val recognitionManager = SpeechRecognitionManager(
            engines = engines,
            audioServices = audioServices,
            dataManager = dataManager,
            eventBus = eventBus
        )
        
        // Step 4: Create command processor
        val commandProcessor = CommandProcessor(
            recognitionManager = recognitionManager,
            dataManager = dataManager
        )
        
        // Step 5: Assemble complete system
        return SpeechRecognitionSystem(
            recognitionManager = recognitionManager,
            commandProcessor = commandProcessor,
            audioServices = audioServices,
            dataManager = dataManager
        )
    }
    
    private fun createAudioServices(context: Context): AudioServices {
        return AudioServices(
            audioCapture = AudioCapture(context, AudioConfig.forSpeechRecognition()),
            audioDeviceManager = AudioDeviceManager(context),
            audioSessionManager = AudioSessionManager(context)
        )
    }
    
    private fun createAllEngines(
        context: Context, 
        eventBus: EventBus,
        audioServices: AudioServices
    ): Map<String, RecognitionEngine> {
        return mapOf(
            "vosk" to VoskFactory(context, eventBus).createEngine(),
            "vivoka" to VivokaFactory(context, eventBus).createEngine(),
            "google" to GoogleCloudFactory(context, getApiKey("google")).createEngine(),
            "azure" to AzureFactory(context, getApiKey("azure")).createEngine(),
            "android" to AndroidSTTFactory(context).createEngine()
        )
    }
    
    private fun createDataManager(context: Context): DataManager {
        // Complex ObjectBox setup hidden here
        val boxStore = MyObjectBox.builder()
            .androidContext(context)
            .name("vos4_database")
            .maxDbSizeInKByte(1024 * 1024) // 1GB max
            .build()
            
        return DataManager(boxStore)
    }
    
    private fun getApiKey(service: String): String {
        // Centralized secure API key retrieval
        return SecureStorage.getApiKey(service)
    }
}

// Usage is super simple!
class VoiceOSApplication : Application() {
    lateinit var speechSystem: SpeechRecognitionSystem
    
    override fun onCreate() {
        super.onCreate()
        
        // One line creates everything!
        speechSystem = VOS4SpeechFactory.createSpeechRecognitionSystem(this)
        
        // System is ready to use
        speechSystem.initialize()
    }
}
```

---

## Benefits of Factory Classes

### 1. **Encapsulation**
- Complex creation logic is hidden
- Callers don't need to know implementation details

### 2. **Reusability**
- Same creation logic used everywhere
- No duplication

### 3. **Testability**
```kotlin
// Easy to create test versions
class TestSpeechFactory : SpeechFactory {
    override fun createEngine(): Engine {
        return MockEngine() // Return mock for testing
    }
}
```

### 4. **Flexibility**
- Easy to change creation logic in one place
- Can swap implementations based on conditions

### 5. **Consistency**
- Objects always created the same way
- Reduces bugs from incorrect initialization

---

## When to Use Factory Classes

### Use a Factory When:
- ✅ Object creation requires **multiple steps**
- ✅ You need to create **families of related objects**
- ✅ Creation logic involves **complex configuration**
- ✅ You want to **hide implementation details**
- ✅ The same creation logic is needed in **multiple places**
- ✅ You need **different configurations** for different scenarios

### Don't Use a Factory When:
- ❌ Simple objects with one constructor parameter
- ❌ Objects that don't require configuration
- ❌ One-off object creation

---

## Factory Types for VOS4

### Recommended Factories:
1. **SpeechRecognitionFactory** - Creates recognition engines and managers
2. **AudioServicesFactory** - Creates audio capture and processing
3. **CommandSystemFactory** - Creates command processors and handlers
4. **DataManagerFactory** - Creates database and repository instances
5. **UIComponentFactory** - Creates UI overlays and views

Each factory encapsulates the complex creation logic for its domain, making the main application code much cleaner and easier to maintain.
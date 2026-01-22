package com.augmentalis.voiceui.core

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.content.Context
// GPU acceleration via RenderEffect (API 31+) with CPU fallback
// GPU capability detection from DeviceManager (shared library)
import com.augmentalis.devicemanager.capabilities.GPUCapabilities
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty

/**
 * MagicEngine - The core of VoiceUI's automatic everything
 * 
 * This engine provides:
 * - Automatic state management with zero configuration
 * - GPU-accelerated state caching
 * - Intelligent defaults based on context
 * - Predictive pre-loading of components
 * - Natural language understanding
 */
@Stable
object MagicEngine {
    
    private const val TAG = "MagicEngine"

    // GPU-accelerated state cache using RenderEffect (API 31+) or CPU fallback
    internal val gpuStateCache = ConcurrentHashMap<String, Any>()
    private val stateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // State managers for GPU/CPU paths
    private var gpuStateManager: GPUStateManager? = null
    private var cpuStateManager: CPUStateManager? = null

    // Intelligent context awareness
    private val contextStack = mutableListOf<ScreenContext>()
    private val componentRegistry = ComponentRegistry()
    private val validationEngine = ValidationEngine()
    private val localizationCache = ConcurrentHashMap<String, String>()

    // Performance metrics - now using GPUCapabilities for detection
    internal val gpuAvailable: Boolean
        get() = GPUCapabilities.isGpuAccelerationAvailable
    internal var lastStateUpdate = 0L
    internal val performanceMonitor = PerformanceMonitor()

    // Acceleration mode for current session
    val accelerationMode: GPUCapabilities.AccelerationMode
        get() = GPUCapabilities.accelerationMode
    
    /**
     * Initialize the Magic Engine with GPU support if available
     *
     * GPU acceleration uses RenderEffect (API 31+) with CPU fallback for older devices.
     * Detection is automatic via GPUCapabilities.
     */
    @Suppress("UNUSED_PARAMETER")
    fun initialize(context: Context) {
        try {
            // Initialize appropriate state manager based on GPU availability
            initializeStateManagers()

            // Log acceleration mode
            Log.i(TAG, "MagicEngine initialized: ${GPUCapabilities.gpuInfo}")

            // Pre-warm common components
            preWarmComponents()

            // Start predictive loading
            startPredictiveEngine()

        } catch (e: Exception) {
            Log.e(TAG, "MagicEngine initialization failed, using CPU fallback", e)
            // Ensure CPU fallback is available
            if (cpuStateManager == null) {
                cpuStateManager = CPUStateManager()
            }
        }
    }

    /**
     * Initialize state managers based on device capabilities
     */
    private fun initializeStateManagers() {
        if (GPUCapabilities.isGpuAccelerationAvailable) {
            // API 31+: Use GPU-accelerated state management
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                gpuStateManager = GPUStateManager()
                Log.d(TAG, "GPU state manager initialized (RenderEffect)")
            }
        }

        // Always initialize CPU fallback for hybrid approach or older devices
        cpuStateManager = CPUStateManager()
        Log.d(TAG, "CPU state manager initialized (fallback)")
    }
    
    /**
     * Automatic state management with zero configuration
     * This is the magic that makes `email()` work without any state declaration
     */
    @Composable
    fun <T> autoState(
        key: String,
        default: T,
        validator: ((T) -> Boolean)? = null,
        persistence: StatePersistence = StatePersistence.MEMORY
    ): MagicState<T> {
        
        // Generate unique key based on composition context
        val uniqueKey = "${currentCompositionLocalContext.hashCode()}_$key"
        
        // Check GPU cache first for performance
        val cachedValue = if (gpuAvailable) {
            @Suppress("UNCHECKED_CAST")
            gpuStateCache[uniqueKey] as? T
        } else null
        
        // Create or retrieve state
        val state = remember(uniqueKey) {
            mutableStateOf(cachedValue ?: default)
        }
        
        // Set up automatic validation
        val validationState = remember { mutableStateOf<String?>(null) }
        
        // Create magic state wrapper
        return MagicState(
            value = state.value,
            setValue = { newValue ->
                // Validate if validator provided
                if (validator != null && !validator(newValue)) {
                    validationState.value = getValidationError(key, newValue as Any)
                } else {
                    validationState.value = null
                    state.value = newValue
                    
                    // Update GPU cache
                    if (gpuAvailable) {
                        updateGPUCache(uniqueKey, newValue as Any)
                    }
                    
                    // Handle persistence
                    if (persistence != StatePersistence.MEMORY) {
                        persistState(uniqueKey, newValue as Any, persistence)
                    }
                }
            },
            error = validationState.value,
            isValid = validationState.value == null
        )
    }
    
    /**
     * Smart defaults based on context
     */
    fun getSmartDefault(componentType: ComponentType, context: ScreenContext): Any {
        return when (componentType) {
            ComponentType.BUTTON -> when (context.screenType) {
                ScreenType.LOGIN -> "Sign In"
                ScreenType.REGISTER -> "Create Account"
                ScreenType.CHECKOUT -> "Complete Purchase"
                ScreenType.SETTINGS -> "Save Changes"
                else -> "Continue"
            }
            ComponentType.EMAIL -> ""
            ComponentType.PASSWORD -> ""
            ComponentType.PHONE -> getLocalPhoneFormat()
            else -> ""
        }
    }
    
    /**
     * GPU-accelerated state update
     * Uses RenderEffect on API 31+ or CPU fallback on older devices
     */
    internal fun updateGPUCache(key: String, value: Any) {
        stateScope.launch {
            withContext(Dispatchers.Default) {
                // Always update the basic cache
                gpuStateCache[key] = value

                // Use appropriate state manager based on device capability
                val changed = if (gpuAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // GPU path: RenderEffect-based state management
                    gpuStateManager?.cacheState(key, value) ?: false
                } else {
                    // CPU path: Optimized hash-based caching
                    cpuStateManager?.cacheStateSync(key, value) ?: false
                }

                // Perform state diff if value changed
                if (changed) {
                    performGPUStateDiff(key, value)
                }

                // Update performance metrics
                val updateTime = System.currentTimeMillis() - lastStateUpdate
                performanceMonitor.recordStateUpdate(updateTime)
                lastStateUpdate = System.currentTimeMillis()
            }
        }
    }
    
    /**
     * Predictive component pre-loading
     */
    private fun startPredictiveEngine() {
        stateScope.launch {
            while (isActive) {
                delay(100) // Check every 100ms
                
                // Predict next likely components based on current context
                val predictions = predictNextComponents()
                
                // Pre-warm predicted components
                predictions.forEach { componentType ->
                    componentRegistry.preWarm(componentType)
                }
            }
        }
    }
    
    /**
     * Predict which components are likely to be used next
     */
    private fun predictNextComponents(): List<ComponentType> {
        val currentScreen = contextStack.lastOrNull() ?: return emptyList()
        
        return when (currentScreen.screenType) {
            ScreenType.LOGIN -> listOf(
                ComponentType.EMAIL,
                ComponentType.PASSWORD,
                ComponentType.BUTTON,
                ComponentType.CHECKBOX
            )
            ScreenType.REGISTER -> listOf(
                ComponentType.NAME,
                ComponentType.EMAIL,
                ComponentType.PASSWORD,
                ComponentType.PHONE,
                ComponentType.BUTTON
            )
            ScreenType.CHECKOUT -> listOf(
                ComponentType.ADDRESS,
                ComponentType.CARD,
                ComponentType.BUTTON
            )
            else -> listOf(ComponentType.TEXT, ComponentType.BUTTON)
        }
    }
    
    /**
     * Pre-warm common components for instant rendering
     */
    private fun preWarmComponents() {
        componentRegistry.preWarm(ComponentType.EMAIL)
        componentRegistry.preWarm(ComponentType.PASSWORD)
        componentRegistry.preWarm(ComponentType.BUTTON)
        componentRegistry.preWarm(ComponentType.TEXT)
        componentRegistry.preWarm(ComponentType.INPUT)
    }
    
    /**
     * Get localized validation error message
     */
    @Suppress("UNUSED_PARAMETER")
    internal fun getValidationError(field: String, value: Any): String {
        return when (field.lowercase()) {
            "email" -> "Please enter a valid email address"
            "password" -> "Password must be at least 8 characters"
            "phone" -> "Please enter a valid phone number"
            "card" -> "Please enter a valid card number"
            else -> "Invalid input"
        }
    }
    
    /**
     * Get local phone format based on device locale
     */
    private fun getLocalPhoneFormat(): String {
        val locale = java.util.Locale.getDefault()
        return when (locale.country) {
            "US", "CA" -> "+1 (___) ___-____"
            "GB" -> "+44 ____ ______"
            "FR" -> "+33 _ __ __ __ __"
            "DE" -> "+49 ___ ________"
            "JP" -> "+81 __-____-____"
            else -> "+__ ___ ___ ____"
        }
    }
    
    /**
     * Persist state based on persistence level
     */
    internal fun persistState(key: String, value: Any, persistence: StatePersistence) {
        stateScope.launch {
            when (persistence) {
                StatePersistence.SESSION -> {
                    // Keep in memory for session
                    gpuStateCache[key] = value
                }
                StatePersistence.LOCAL -> {
                    // Save to DataStore
                    // Implementation depends on DataStore setup
                }
                StatePersistence.CLOUD -> {
                    // Sync to cloud
                    // Implementation depends on cloud service
                }
                else -> {}
            }
        }
    }
    
    /**
     * Perform GPU-accelerated state diffing
     *
     * On API 31+: Uses GPUStateManager with RenderEffect support
     * On API 29-30: Uses CPUStateManager with optimized hash diffing
     */
    private fun performGPUStateDiff(key: String, newValue: Any) {
        stateScope.launch {
            if (gpuAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // GPU path: RenderEffect-based diffing
                val result = gpuStateManager?.diffState(key, newValue)
                if (result?.changed == true) {
                    Log.v(TAG, "State diff: $key changed (new=${result.isNew}, mode=GPU)")
                }
            } else {
                // CPU path: Hash-based diffing
                val result = cpuStateManager?.diffStateSync(key, newValue)
                if (result?.changed == true) {
                    Log.v(TAG, "State diff: $key changed (new=${result.isNew}, mode=CPU)")
                }
            }
        }
    }

    /**
     * Get state manager statistics for debugging/monitoring
     */
    fun getStateManagerStats(): Map<String, Any> {
        return buildMap {
            put("accelerationMode", accelerationMode.name)
            put("gpuAvailable", gpuAvailable)
            put("cacheSize", gpuStateCache.size)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                gpuStateManager?.getCacheStats()?.let { stats ->
                    put("gpuCacheSize", stats.size)
                }
            }

            cpuStateManager?.getCacheStats()?.let { stats ->
                put("cpuCacheSize", stats.size)
                put("cpuHitRate", stats.hitRate)
            }
        }
    }
    
    /**
     * Push a new screen context
     */
    fun pushContext(context: ScreenContext) {
        contextStack.add(context)
    }
    
    /**
     * Pop the current screen context
     */
    fun popContext() {
        if (contextStack.isNotEmpty()) {
            contextStack.removeAt(contextStack.lastIndex)
        }
    }
    
    /**
     * Get current screen context
     */
    fun getCurrentContext(): ScreenContext? {
        return contextStack.lastOrNull()
    }
    
    /**
     * Clean up resources
     */
    fun dispose() {
        // Clear state managers
        gpuStateManager?.clearCache()
        cpuStateManager?.clearCache()
        gpuStateCache.clear()

        // Cancel coroutine scope
        stateScope.cancel()

        Log.d(TAG, "MagicEngine disposed")
    }
}

/**
 * Magic State wrapper that provides automatic everything
 */
data class MagicState<T>(
    val value: T,
    val setValue: (T) -> Unit,
    val error: String?,
    val isValid: Boolean
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setValue(value)
}

/**
 * Screen context for intelligent defaults
 */
data class ScreenContext(
    val screenType: ScreenType,
    val screenName: String,
    val parentScreen: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Screen types for context awareness
 */
enum class ScreenType {
    LOGIN,
    REGISTER,
    HOME,
    PROFILE,
    SETTINGS,
    CHECKOUT,
    PRODUCT,
    CHAT,
    FORM,
    LIST,
    DETAIL,
    CUSTOM
}

/**
 * Component types for pre-warming and prediction
 */
enum class ComponentType {
    TEXT,
    BUTTON,
    INPUT,
    EMAIL,
    PASSWORD,
    PHONE,
    NAME,
    ADDRESS,
    CARD,
    DATE,
    DROPDOWN,
    CHECKBOX,
    RADIO,
    SLIDER,
    TOGGLE,
    IMAGE,
    LIST,
    GRID
}

/**
 * State persistence levels
 */
enum class StatePersistence {
    MEMORY,    // Only in memory (cleared on screen change)
    SESSION,   // For current session (cleared on app restart)
    LOCAL,     // Persisted locally (survives app restart)
    CLOUD      // Synced to cloud (available across devices)
}

/**
 * Component registry for pre-warming
 */
class ComponentRegistry {
    private val registry = ConcurrentHashMap<ComponentType, ComponentFactory>()
    private val cache = ConcurrentHashMap<ComponentType, Any>()
    
    fun preWarm(type: ComponentType) {
        // Pre-create component factories for instant rendering
        if (!cache.containsKey(type)) {
            cache[type] = createFactory(type)
        }
    }
    
    private fun createFactory(type: ComponentType): ComponentFactory {
        return ComponentFactory(type)
    }
}

/**
 * Component factory for efficient component creation
 */
class ComponentFactory(val type: ComponentType) {
    fun create(): Any {
        // Return pre-configured component
        return when (type) {
            ComponentType.EMAIL -> EmailComponentConfig()
            ComponentType.PASSWORD -> PasswordComponentConfig()
            else -> GenericComponentConfig()
        }
    }
}

/**
 * Validation engine for automatic validation
 */
class ValidationEngine {
    fun validate(type: ComponentType, value: Any): Boolean {
        return when (type) {
            ComponentType.EMAIL -> validateEmail(value.toString())
            ComponentType.PASSWORD -> validatePassword(value.toString())
            ComponentType.PHONE -> validatePhone(value.toString())
            ComponentType.CARD -> validateCard(value.toString())
            else -> true
        }
    }
    
    private fun validateEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
    
    private fun validatePassword(password: String): Boolean {
        return password.length >= 8
    }
    
    private fun validatePhone(phone: String): Boolean {
        return phone.replace(Regex("[^0-9]"), "").length >= 10
    }
    
    private fun validateCard(card: String): Boolean {
        return card.replace(Regex("[^0-9]"), "").length == 16
    }
}

/**
 * Performance monitor for optimization
 */
class PerformanceMonitor {
    private val metrics = ConcurrentHashMap<String, Long>()
    
    fun recordStateUpdate(time: Long) {
        metrics["lastStateUpdate"] = time
        
        // Calculate average
        val average = metrics.values.average()
        if (average > 10) {
            // Log slow performance for optimization
        }
    }
}

// Component configurations
class EmailComponentConfig
class PasswordComponentConfig
class GenericComponentConfig
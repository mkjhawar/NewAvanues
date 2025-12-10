/**
 * AIContext.kt - AI context system for VoiceUI elements
 * 
 * Provides rich context information to help AI understand UI elements,
 * their purpose, relationships, and optimal interaction patterns.
 */

package com.augmentalis.voiceui.api

import com.augmentalis.uuidmanager.UUIDManager
import com.augmentalis.uuidmanager.models.UUIDElement
import com.augmentalis.uuidmanager.models.UUIDPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * AI Context data for UI elements
 * 
 * Provides semantic information beyond just visual properties
 */
data class AIContext(
    // Semantic information
    val purpose: String? = null,                    // "login authentication", "product purchase"
    val userIntent: String? = null,                 // "user wants to log in", "user browsing products"
    val businessLogic: String? = null,              // "validates email format", "charges payment method"
    val dataFlow: String? = null,                   // "email -> validation -> authentication -> dashboard"
    
    // Contextual relationships
    val relatedElements: List<String> = emptyList(), // UUIDs of related elements
    val dependsOn: List<String> = emptyList(),       // Elements this depends on
    val triggers: List<String> = emptyList(),        // Elements this affects
    val workflow: String? = null,                    // "step 2 of 5 in checkout process"
    
    // User experience context
    val expectedUserActions: List<String> = emptyList(), // ["tap", "voice", "long_press"]
    val commonUserErrors: List<String> = emptyList(),    // ["forgets @", "types wrong format"]
    val successPatterns: List<String> = emptyList(),     // ["most users tap", "power users use voice"]
    val accessibility: AccessibilityContext? = null,
    
    // AI assistance hints
    val aiSuggestions: List<String> = emptyList(),       // ["suggest email completion", "offer password reset"]
    val contextualHelp: String? = null,                  // "say 'enter email' to focus this field"
    val voiceAlternatives: List<String> = emptyList(),   // ["enter email", "type email", "email field"]
    val gestureHints: List<String> = emptyList(),        // ["swipe up for keyboard", "long press for paste"]
    
    // Screen/app context
    val screenContext: ScreenContext? = null,
    val appContext: AppContext? = null,
    
    // Dynamic context
    val currentState: String? = null,                    // "empty", "validating", "error", "success"
    val previousUserActions: List<String> = emptyList(), // Recent user behavior
    val sessionContext: String? = null,                  // "user already failed login twice"
    val timeContext: String? = null                      // "first time user", "returning user"
)

/**
 * Accessibility context for AI understanding
 */
data class AccessibilityContext(
    val screenReaderText: String? = null,           // What screen readers should announce
    val voiceCommandPriority: Int = 0,              // Higher = more likely to be voice target
    val keyboardNavigation: String? = null,         // "tab to focus, enter to activate"
    val motorImpairment: List<String> = emptyList(), // ["large_target", "voice_alternative"]
    val visualImpairment: List<String> = emptyList(), // ["high_contrast", "audio_feedback"]
    val cognitiveSupport: List<String> = emptyList()  // ["simple_language", "clear_instructions"]
)

/**
 * Screen-level context
 */
data class ScreenContext(
    val screenType: String,                         // "login", "product_list", "checkout"
    val screenPurpose: String,                      // "authenticate user", "browse products"
    val userJourney: String,                        // "onboarding -> main app", "shopping -> payment"
    val exitPoints: List<String> = emptyList(),     // ["back button", "home gesture", "logout"]
    val successCriteria: String? = null             // "user successfully logs in"
)

/**
 * App-level context
 */
data class AppContext(
    val appType: String,                            // "ecommerce", "social", "productivity"
    val userRole: String? = null,                   // "customer", "admin", "guest"
    val businessModel: String? = null,              // "subscription", "freemium", "one-time"
    val complianceNeeds: List<String> = emptyList(), // ["GDPR", "CCPA", "PCI"]
    val brandVoice: String? = null                  // "friendly", "professional", "playful"
)

/**
 * Enhanced VoiceUI components with AI context
 */

@Composable
fun VoiceButton(
    text: String,
    locale: String? = null,
    synonyms: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // NEW: AI context parameter
    aiContext: AIContext? = null
) {
    val uuid = remember { UUIDManager.generate() }
    val uuidManager = UUIDManager.instance
    
    // Register with enhanced context
    LaunchedEffect(uuid, text, aiContext) {
        val element = UUIDElement(
            uuid = uuid,
            name = text,
            type = "button",
            description = aiContext?.purpose,
            actions = mapOf(
                "click" to { _ -> onClick?.invoke() },
                "focus" to { _ -> /* focus logic */ }
            ),
            metadata = createMetadataWithAI(aiContext)
        )
        
        uuidManager.registerElement(element)
        
        // Set AI context for this element
        aiContext?.let { context ->
            AIContextManager.setContext(uuid, context)
        }
    }
    
    // Rest of button implementation...
    VoiceButtonImpl(text, uuid, onClick, modifier)
}

@Composable
fun VoiceInput(
    label: String,
    locale: String? = null,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    // NEW: AI context with smart defaults
    aiContext: AIContext? = null
) {
    val uuid = remember { UUIDManager.generate() }
    val enhancedContext = aiContext ?: when {
        label.contains("email", ignoreCase = true) -> AIContext(
            purpose = "collect user email for authentication or communication",
            userIntent = "user needs to provide their email address",
            expectedUserActions = listOf("tap", "voice_dictation", "paste"),
            commonUserErrors = listOf("missing @", "typos", "wrong domain"),
            aiSuggestions = listOf("validate_format", "suggest_common_domains"),
            contextualHelp = "Say 'enter email' or tap to focus this field",
            voiceAlternatives = listOf("enter email", "email field", "type email"),
            accessibility = AccessibilityContext(
                screenReaderText = "Email input field",
                voiceCommandPriority = 8
            )
        )
        label.contains("password", ignoreCase = true) -> AIContext(
            purpose = "secure authentication credential input",
            userIntent = "user needs to enter their password securely",
            expectedUserActions = listOf("tap", "keyboard_only"), // No voice for security
            commonUserErrors = listOf("caps_lock", "wrong_password", "typos"),
            aiSuggestions = listOf("show_password_strength", "suggest_reset"),
            contextualHelp = "Password field - no voice input for security",
            accessibility = AccessibilityContext(
                screenReaderText = "Password input field, secure text entry",
                voiceCommandPriority = 6 // Lower priority for voice
            )
        )
        else -> AIContext(
            purpose = "text input for $label",
            expectedUserActions = listOf("tap", "voice_dictation")
        )
    }
    
    // Register with context
    LaunchedEffect(uuid, label, enhancedContext) {
        val element = UUIDElement(
            uuid = uuid,
            name = label,
            type = "input",
            description = enhancedContext.purpose,
            actions = mapOf(
                "focus" to { _ -> /* focus and show keyboard */ },
                "clear" to { _ -> onValueChange?.invoke("") }
            ),
            metadata = createMetadataWithAI(enhancedContext)
        )
        
        UUIDManager.instance.registerElement(element)
        AIContextManager.setContext(uuid, enhancedContext)
    }
    
    VoiceInputImpl(label, uuid, onValueChange, modifier)
}

/**
 * Smart context builder for common UI patterns
 */
object SmartContext {
    
    fun forLogin(): AIContext = AIContext(
        purpose = "user authentication and account access",
        userIntent = "user wants to access their account",
        workflow = "step 1 of user session - authenticate identity",
        successPatterns = listOf("user successfully logs in and reaches dashboard"),
        screenContext = ScreenContext(
            screenType = "login",
            screenPurpose = "authenticate user identity",
            userJourney = "splash -> login -> dashboard",
            exitPoints = listOf("back_button", "forgot_password", "create_account")
        )
    )
    
    fun forEcommerce(): AIContext = AIContext(
        purpose = "product discovery and purchase",
        userIntent = "user wants to find and buy products",
        businessLogic = "convert browsers to buyers, maximize revenue",
        appContext = AppContext(
            appType = "ecommerce",
            businessModel = "transaction_fees",
            complianceNeeds = listOf("PCI", "consumer_protection")
        )
    )
    
    fun forSettings(): AIContext = AIContext(
        purpose = "app configuration and personalization",
        userIntent = "user wants to customize app behavior",
        expectedUserActions = listOf("tap", "voice", "toggle"),
        aiSuggestions = listOf("suggest_optimal_settings", "explain_impact")
    )
    
    fun forAccessibility(): AIContext = AIContext(
        purpose = "inclusive access to app functionality",
        userIntent = "user with accessibility needs wants full app access",
        accessibility = AccessibilityContext(
            voiceCommandPriority = 10, // Highest priority
            motorImpairment = listOf("large_targets", "voice_control", "dwell_clicking"),
            visualImpairment = listOf("screen_reader", "high_contrast", "voice_feedback"),
            cognitiveSupport = listOf("simple_language", "clear_instructions", "consistent_patterns")
        )
    )
}

/**
 * AI Context Manager - stores and retrieves context for elements
 */
object AIContextManager {
    private val contextStore = mutableMapOf<String, AIContext>()
    
    fun setContext(uuid: String, context: AIContext) {
        contextStore[uuid] = context
    }
    
    fun getContext(uuid: String): AIContext? = contextStore[uuid]
    
    fun getAllContexts(): Map<String, AIContext> = contextStore.toMap()
    
    fun clearContext(uuid: String) {
        contextStore.remove(uuid)
    }
    
    /**
     * Generate AI prompt with full context
     */
    fun generateAIPrompt(uuid: String, userRequest: String): String {
        val context = getContext(uuid) ?: return userRequest
        val element = UUIDManager.instance.findByUUID(uuid)
        
        return buildString {
            appendLine("USER REQUEST: $userRequest")
            appendLine()
            appendLine("ELEMENT CONTEXT:")
            appendLine("- Type: ${element?.type}")
            appendLine("- Name: ${element?.name}")
            appendLine("- Purpose: ${context.purpose}")
            appendLine("- User Intent: ${context.userIntent}")
            appendLine("- Current State: ${context.currentState}")
            appendLine()
            appendLine("INTERACTION CONTEXT:")
            appendLine("- Expected Actions: ${context.expectedUserActions.joinToString()}")
            appendLine("- Voice Alternatives: ${context.voiceAlternatives.joinToString()}")
            appendLine("- Common Errors: ${context.commonUserErrors.joinToString()}")
            appendLine("- Contextual Help: ${context.contextualHelp}")
            appendLine()
            if (context.screenContext != null) {
                appendLine("SCREEN CONTEXT:")
                appendLine("- Screen Type: ${context.screenContext.screenType}")
                appendLine("- Purpose: ${context.screenContext.screenPurpose}")
                appendLine("- User Journey: ${context.screenContext.userJourney}")
                appendLine()
            }
            appendLine("Please provide assistance based on this context.")
        }
    }
}

/**
 * Usage examples with AI context
 */

@Composable
fun LoginScreenWithAIContext() {
    VoiceScreen("login", aiContext = SmartContext.forLogin()) {
        text("Welcome Back", aiContext = AIContext(
            purpose = "welcome returning user and set positive tone",
            accessibility = AccessibilityContext(screenReaderText = "Welcome back to VoiceOS")
        ))
        
        input("email", aiContext = AIContext(
            purpose = "collect user email for authentication",
            expectedUserActions = listOf("tap", "voice_dictation", "autofill"),
            aiSuggestions = listOf("validate_format", "suggest_recent_emails"),
            contextualHelp = "Say 'enter email' to focus and dictate your email address"
        ))
        
        password(aiContext = AIContext(
            purpose = "secure password entry for authentication",
            expectedUserActions = listOf("tap", "keyboard_only"),
            commonUserErrors = listOf("caps_lock_on", "forgot_password"),
            aiSuggestions = listOf("check_caps_lock", "offer_password_reset")
        ))
        
        button("login", aiContext = AIContext(
            purpose = "submit credentials and authenticate user",
            businessLogic = "validate credentials, create session, redirect to dashboard",
            successPatterns = listOf("most users tap", "accessibility users use voice"),
            aiSuggestions = listOf("validate_form_before_submit", "show_loading_state")
        ))
    }
}

// Helper function to create metadata with AI context
private fun createMetadataWithAI(aiContext: AIContext?) = aiContext?.let {
    com.augmentalis.uuidmanager.models.UUIDMetadata(
        attributes = mapOf(
            "ai_purpose" to (it.purpose ?: ""),
            "ai_user_intent" to (it.userIntent ?: ""),
            "ai_contextual_help" to (it.contextualHelp ?: ""),
            "ai_voice_priority" to (it.accessibility?.voiceCommandPriority ?: 0).toString()
        )
    )
}

// Placeholder implementations
@Composable private fun VoiceButtonImpl(text: String, uuid: String, onClick: (() -> Unit)?, modifier: Modifier) {}
@Composable private fun VoiceInputImpl(label: String, uuid: String, onValueChange: ((String) -> Unit)?, modifier: Modifier) {}
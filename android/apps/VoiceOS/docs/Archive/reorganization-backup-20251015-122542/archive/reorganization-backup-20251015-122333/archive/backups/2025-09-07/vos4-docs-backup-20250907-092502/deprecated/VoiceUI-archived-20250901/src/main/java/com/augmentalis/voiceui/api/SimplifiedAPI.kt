/**
 * SimplifiedAPI.kt - Ultra-simple VoiceUI API with intelligent defaults
 * 
 * Goal: Make VoiceUI simpler than Android XML
 * - 1-2 lines per component
 * - Auto voice command generation
 * - Auto localization
 * - Smart defaults for everything
 */

package com.augmentalis.voiceui.api

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.voiceui.VoiceUIModule
import com.augmentalis.voiceui.components.*
import com.augmentalis.localizationmanager.LocalizationModule
import java.util.Locale

/**
 * Ultra-simple button - 1 line with everything auto-generated
 * 
 * Examples:
 * VoiceButton("Login")                    // Auto: voice="login", onClick=findLoginMethod()
 * VoiceButton("Login") { login() }        // Custom action
 * VoiceButton("Login", "es")              // Spanish localization
 */
@Composable
fun VoiceButton(
    text: String,
    locale: String? = null,
    synonyms: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val localizedText = locale?.let {
        LocalizationModule.getInstance(context).translate(text, it)
    } ?: text
    val voiceCommands = VoiceCommandGenerator.generate(localizedText, synonyms)
    val action = onClick ?: ActionResolver.findAction(text)
    
    // Internal implementation uses full VoiceUIButton
    VoiceUIButton(
        text = localizedText,
        voiceCommand = voiceCommands.primary,
        voiceAlternatives = voiceCommands.alternatives,
        onClick = action,
        modifier = modifier
    )
}

/**
 * Ultra-simple input - Auto voice dictation
 */
@Composable
fun VoiceInput(
    label: String,
    locale: String? = null,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val localizedLabel = locale?.let {
        LocalizationModule.getInstance(context).translate(label, it)
    } ?: label
    val voiceCommands = VoiceCommandGenerator.generateForInput(localizedLabel)
    val handler = onValueChange ?: ValueResolver.findHandler(label)
    
    VoiceUITextField(
        label = localizedLabel,
        voiceCommand = voiceCommands.primary,
        voiceDictation = true,
        onValueChange = handler,
        modifier = modifier
    )
}

/**
 * Password input - Auto disables voice for security
 */
@Composable
fun VoicePassword(
    label: String = "Password",
    locale: String? = null,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val localizedLabel = locale?.let {
        LocalizationModule.getInstance(context).translate(label, it)
    } ?: label
    
    VoiceUITextField(
        label = localizedLabel,
        voiceCommand = "enter password",
        voiceDictation = false,  // Auto-disabled for passwords
        isPassword = true,
        onValueChange = onValueChange ?: ValueResolver.findHandler("password"),
        modifier = modifier
    )
}

/**
 * Text with auto voice announcement
 */
@Composable
fun VoiceText(
    text: String,
    locale: String? = null,
    autoAnnounce: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localizedText = locale?.let {
        LocalizationModule.getInstance(context).translate(text, it)
    } ?: text
    
    VoiceUIText(
        text = localizedText,
        voiceAnnounce = autoAnnounce,
        modifier = modifier
    )
}

/**
 * Voice command generator - Creates natural commands from text
 */
object VoiceCommandGenerator {
    
    fun generate(text: String, userSynonyms: List<String> = emptyList()): CommandSet {
        val normalized = text.lowercase().trim()
        val baseCommand = normalized.replace(" ", "_")
        
        // Auto-generate common variations
        val autoSynonyms = mutableListOf<String>()
        
        // Add variations without underscores
        autoSynonyms.add(normalized)
        
        // Common button text patterns
        when {
            normalized.contains("login") || normalized.contains("sign in") -> {
                autoSynonyms.addAll(listOf("login", "sign in", "log in", "authenticate"))
            }
            normalized.contains("save") -> {
                autoSynonyms.addAll(listOf("save", "save file", "save document", "store"))
            }
            normalized.contains("cancel") -> {
                autoSynonyms.addAll(listOf("cancel", "close", "dismiss", "exit"))
            }
            normalized.contains("submit") -> {
                autoSynonyms.addAll(listOf("submit", "send", "confirm", "ok"))
            }
            normalized.contains("back") -> {
                autoSynonyms.addAll(listOf("back", "go back", "previous", "return"))
            }
            normalized.contains("next") -> {
                autoSynonyms.addAll(listOf("next", "continue", "forward", "proceed"))
            }
        }
        
        // Add user-provided synonyms
        autoSynonyms.addAll(userSynonyms)
        
        return CommandSet(
            primary = baseCommand,
            alternatives = autoSynonyms.distinct()
        )
    }
    
    fun generateForInput(label: String): CommandSet {
        val normalized = label.lowercase().trim()
        val baseCommand = "enter_$normalized".replace(" ", "_")
        
        return CommandSet(
            primary = baseCommand,
            alternatives = listOf(
                "enter $normalized",
                "input $normalized",
                "type $normalized",
                "$normalized field"
            )
        )
    }
    
    data class CommandSet(
        val primary: String,
        val alternatives: List<String>
    )
}

// Using system-wide LocalizationModule instead of custom LocalizationEngine
// The LocalizationModule supports 42+ languages and is already integrated

/**
 * Action resolver - Finds methods by convention
 */
object ActionResolver {
    
    fun findAction(buttonText: String): () -> Unit {
        val methodName = buttonText.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
        
        // Try to find method in current context
        // This would use reflection or code generation in real implementation
        return {
            println("Auto-resolved action for: $buttonText → $methodName()")
            // In real implementation: 
            // - Use reflection to find method
            // - Or use code generation at compile time
            // - Or use a registration system
        }
    }
}

/**
 * Value resolver - Finds property handlers by convention
 */
object ValueResolver {
    
    fun findHandler(fieldName: String): (String) -> Unit {
        val propertyName = fieldName.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
        
        return { value ->
            println("Auto-resolved handler for: $fieldName → $propertyName = $value")
            // In real implementation:
            // - Use reflection to find and set property
            // - Or use code generation
        }
    }
}

// VoiceScreenScope moved to separate file for SRP

/**
 * Usage examples showing simplicity
 */
@Composable
fun LoginScreenExample() {
    // State management for the login screen
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Action handlers
    val performLogin = {
        println("Login with email: $email")
        // Add actual login logic here
    }
    
    val resetPassword = {
        println("Reset password for: $email")
        // Add password reset logic here
    }
    
    // Entire login screen in 7 lines!
    VoiceScreenDSL("login") {
        text("Welcome to VoiceOS")
        input("Email") { email = it }
        password() { password = it }
        button("Login") { performLogin() }
        button("Forgot Password") { resetPassword() }
    }
    // Automatically includes:
    // - Voice commands for everything
    // - Gesture support
    // - Localization
    // - Accessibility
    // - HUD feedback
}

// Even simpler - everything auto-resolved by convention
@Composable
fun AutoLoginScreen() {
    VoiceScreenDSL {
        text("Welcome")           // Auto-announces on screen load
        input("email")            // Auto: var email by remember { mutableStateOf("") }
        password("password")      // Auto: var password by remember { mutableStateOf("") }
        button("login")           // Auto: fun login() { }
        button("forgot_password") // Auto: fun forgot_password() { }
    }
}

/**
 * Global configuration
 */
object VoiceUIConfig {
    var autoVoiceCommands = true
    var autoLocalization = true
    var autoGestures = true
    var defaultLanguage = "en"
    var aiApiKey: String? = null
    
    private val supportedLanguages = listOf("en", "es", "fr", "de", "ja", "zh")
    private var currentLanguageIndex = 0
    
    fun enableAI(apiKey: String) {
        aiApiKey = apiKey
        // Enable AI-powered command generation
    }
    
    fun nextLanguage(): String {
        currentLanguageIndex = (currentLanguageIndex + 1) % supportedLanguages.size
        defaultLanguage = supportedLanguages[currentLanguageIndex]
        return defaultLanguage
    }
    
    fun setLanguage(language: String) {
        val index = supportedLanguages.indexOf(language)
        if (index >= 0) {
            currentLanguageIndex = index
            defaultLanguage = language
        }
    }
}

package com.augmentalis.voiceui.nlp

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.*
import java.util.regex.Pattern

/**
 * NaturalLanguageParser - Convert plain English descriptions to UI components
 * 
 * Examples:
 * - "login screen" → email, password, submit button
 * - "form with name and email" → name input, email input, submit
 * - "settings page with dark mode toggle" → settings layout with toggle
 */
object NaturalLanguageParser {
    
    // Pattern matchers for common UI descriptions
    private val patterns = mapOf(
        // Screen types
        Pattern.compile("(?i)(login|sign in|authentication)\\s*(screen|page|form)?") to ScreenTemplate.LOGIN,
        Pattern.compile("(?i)(register|sign up|create account)\\s*(screen|page|form)?") to ScreenTemplate.REGISTER,
        Pattern.compile("(?i)(settings|preferences|options)\\s*(screen|page)?") to ScreenTemplate.SETTINGS,
        Pattern.compile("(?i)(profile|user|account)\\s*(screen|page)?") to ScreenTemplate.PROFILE,
        Pattern.compile("(?i)(checkout|payment|purchase)\\s*(screen|page|form)?") to ScreenTemplate.CHECKOUT,
        Pattern.compile("(?i)(chat|message|conversation)\\s*(screen|interface)?") to ScreenTemplate.CHAT,
        Pattern.compile("(?i)(search|find|browse)\\s*(screen|page)?") to ScreenTemplate.SEARCH,
        Pattern.compile("(?i)(dashboard|home|main)\\s*(screen|page)?") to ScreenTemplate.DASHBOARD,
        
        // Component patterns
        Pattern.compile("(?i)with\\s+(email|mail)") to ComponentIntent.EMAIL,
        Pattern.compile("(?i)with\\s+password") to ComponentIntent.PASSWORD,
        Pattern.compile("(?i)with\\s+(name|full name)") to ComponentIntent.NAME,
        Pattern.compile("(?i)with\\s+(phone|telephone|mobile)") to ComponentIntent.PHONE,
        Pattern.compile("(?i)with\\s+(address|location)") to ComponentIntent.ADDRESS,
        Pattern.compile("(?i)with\\s+(payment|card|credit card)") to ComponentIntent.CARD,
        Pattern.compile("(?i)with\\s+(date|calendar|datepicker)") to ComponentIntent.DATE,
        Pattern.compile("(?i)with\\s+(toggle|switch)\\s+for\\s+(.+)") to ComponentIntent.TOGGLE,
        Pattern.compile("(?i)with\\s+(dropdown|select|picker)\\s+for\\s+(.+)") to ComponentIntent.DROPDOWN,
        Pattern.compile("(?i)with\\s+(list|items)\\s+of\\s+(.+)") to ComponentIntent.LIST,
        
        // Styling patterns
        Pattern.compile("(?i)(blue|red|green|purple|orange)\\s+(button|submit)") to StyleIntent.COLORED_BUTTON,
        Pattern.compile("(?i)(large|big|prominent)\\s+(button|title|header)") to StyleIntent.LARGE,
        Pattern.compile("(?i)(small|tiny|compact)") to StyleIntent.SMALL,
        Pattern.compile("(?i)(centered|center)") to StyleIntent.CENTERED,
        Pattern.compile("(?i)(dark|night)\\s+mode") to StyleIntent.DARK_MODE,
        
        // Feature patterns
        Pattern.compile("(?i)with\\s+(remember me|stay logged in)") to FeatureIntent.REMEMBER_ME,
        Pattern.compile("(?i)with\\s+(forgot password|reset password)") to FeatureIntent.FORGOT_PASSWORD,
        Pattern.compile("(?i)with\\s+(social|google|facebook|apple)\\s+(login|signin)") to FeatureIntent.SOCIAL_LOGIN,
        Pattern.compile("(?i)with\\s+(validation|error checking)") to FeatureIntent.VALIDATION,
        Pattern.compile("(?i)with\\s+(voice|speech)\\s+(input|commands)") to FeatureIntent.VOICE_INPUT,
        Pattern.compile("(?i)(responsive|adaptive|mobile.friendly)") to FeatureIntent.RESPONSIVE
    )
    
    /**
     * Parse natural language description into UI components
     */
    fun parse(description: String): ParsedUI {
        val normalized = description.lowercase().trim()
        
        // Detect primary screen template
        val screenTemplate = detectScreenTemplate(normalized)
        
        // Extract additional components
        val components = extractComponents(normalized)
        
        // Extract styling preferences
        val styles = extractStyles(normalized)
        
        // Extract features
        val features = extractFeatures(normalized)
        
        // Build UI structure
        return ParsedUI(
            template = screenTemplate,
            components = mergeWithTemplate(screenTemplate, components),
            styles = styles,
            features = features,
            originalDescription = description
        )
    }
    
    /**
     * Detect the main screen template from description
     */
    private fun detectScreenTemplate(text: String): ScreenTemplate {
        for ((pattern, template) in patterns) {
            if (pattern.matcher(text).find() && template is ScreenTemplate) {
                return template
            }
        }
        
        // Advanced detection using keywords
        return when {
            text.contains("login") || text.contains("sign in") -> ScreenTemplate.LOGIN
            text.contains("register") || text.contains("sign up") -> ScreenTemplate.REGISTER
            text.contains("settings") || text.contains("preferences") -> ScreenTemplate.SETTINGS
            text.contains("profile") || text.contains("user") -> ScreenTemplate.PROFILE
            text.contains("checkout") || text.contains("payment") -> ScreenTemplate.CHECKOUT
            text.contains("chat") || text.contains("message") -> ScreenTemplate.CHAT
            text.contains("form") -> ScreenTemplate.FORM
            text.contains("list") || text.contains("items") -> ScreenTemplate.LIST
            else -> ScreenTemplate.CUSTOM
        }
    }
    
    /**
     * Extract individual components from description
     */
    private fun extractComponents(text: String): List<UIComponent> {
        val components = mutableListOf<UIComponent>()
        
        // Check for email
        if (text.contains("email") || text.contains("mail")) {
            components.add(UIComponent.Email())
        }
        
        // Check for password
        if (text.contains("password")) {
            components.add(UIComponent.Password())
        }
        
        // Check for name
        if (text.contains("name") && !text.contains("username")) {
            components.add(UIComponent.Name(splitFirstLast = text.contains("first") && text.contains("last")))
        }
        
        // Check for phone
        if (text.contains("phone") || text.contains("mobile") || text.contains("telephone")) {
            components.add(UIComponent.Phone())
        }
        
        // Check for address
        if (text.contains("address") || text.contains("location")) {
            components.add(UIComponent.Address())
        }
        
        // Check for date
        if (text.contains("date") || text.contains("calendar") || text.contains("birthday")) {
            components.add(UIComponent.DatePicker())
        }
        
        // Check for toggles/switches
        val togglePattern = Pattern.compile("(?i)(toggle|switch)\\s+for\\s+([\\w\\s]+)")
        val toggleMatcher = togglePattern.matcher(text)
        while (toggleMatcher.find()) {
            val label = toggleMatcher.group(2)?.trim() ?: "Option"
            components.add(UIComponent.Toggle(label))
        }
        
        // Check for dropdowns
        val dropdownPattern = Pattern.compile("(?i)(dropdown|select|picker)\\s+for\\s+([\\w\\s]+)")
        val dropdownMatcher = dropdownPattern.matcher(text)
        while (dropdownMatcher.find()) {
            val label = dropdownMatcher.group(2)?.trim() ?: "Select"
            components.add(UIComponent.Dropdown(label, extractOptions(text, label)))
        }
        
        // Check for buttons
        val buttonPattern = Pattern.compile("(?i)(button|submit|action)\\s+(?:labeled|called|named)?\\s*['\"]?([\\w\\s]+)['\"]?")
        val buttonMatcher = buttonPattern.matcher(text)
        while (buttonMatcher.find()) {
            val label = buttonMatcher.group(2)?.trim() ?: "Submit"
            components.add(UIComponent.Button(label))
        }
        
        // Check for text/labels
        val textPattern = Pattern.compile("(?i)(text|label|title|header)\\s+['\"]([^'\"]+)['\"]")
        val textMatcher = textPattern.matcher(text)
        while (textMatcher.find()) {
            val content = textMatcher.group(2) ?: ""
            components.add(UIComponent.Text(content))
        }
        
        return components
    }
    
    /**
     * Extract styling preferences
     */
    private fun extractStyles(text: String): UIStyles {
        val styles = UIStyles()
        
        // Color detection
        val colors = listOf("blue", "red", "green", "purple", "orange", "pink", "yellow", "black", "white")
        for (color in colors) {
            if (text.contains(color)) {
                styles.primaryColor = color
                break
            }
        }
        
        // Size detection
        when {
            text.contains("large") || text.contains("big") -> styles.size = SizePreference.LARGE
            text.contains("small") || text.contains("tiny") -> styles.size = SizePreference.SMALL
            text.contains("compact") -> styles.size = SizePreference.COMPACT
        }
        
        // Layout detection
        when {
            text.contains("centered") || text.contains("center") -> styles.alignment = AlignmentPreference.CENTER
            text.contains("left") -> styles.alignment = AlignmentPreference.LEFT
            text.contains("right") -> styles.alignment = AlignmentPreference.RIGHT
        }
        
        // Theme detection
        if (text.contains("dark") || text.contains("night")) {
            styles.isDarkMode = true
        }
        
        // Spacing detection
        when {
            text.contains("spacious") || text.contains("roomy") -> styles.spacing = SpacingPreference.LARGE
            text.contains("compact") || text.contains("tight") -> styles.spacing = SpacingPreference.SMALL
        }
        
        return styles
    }
    
    /**
     * Extract feature requirements
     */
    private fun extractFeatures(text: String): List<UIFeature> {
        val features = mutableListOf<UIFeature>()
        
        if (text.contains("remember me") || text.contains("stay logged in")) {
            features.add(UIFeature.REMEMBER_ME)
        }
        
        if (text.contains("forgot password") || text.contains("reset password")) {
            features.add(UIFeature.FORGOT_PASSWORD)
        }
        
        if (text.contains("social") || text.contains("google") || text.contains("facebook")) {
            features.add(UIFeature.SOCIAL_LOGIN)
        }
        
        if (text.contains("validation") || text.contains("error")) {
            features.add(UIFeature.VALIDATION)
        }
        
        if (text.contains("voice") || text.contains("speech")) {
            features.add(UIFeature.VOICE_INPUT)
        }
        
        if (text.contains("responsive") || text.contains("mobile")) {
            features.add(UIFeature.RESPONSIVE)
        }
        
        if (text.contains("loading") || text.contains("progress")) {
            features.add(UIFeature.LOADING_STATE)
        }
        
        if (text.contains("accessible") || text.contains("accessibility")) {
            features.add(UIFeature.ACCESSIBILITY)
        }
        
        return features
    }
    
    /**
     * Merge template components with extracted components
     */
    private fun mergeWithTemplate(
        template: ScreenTemplate,
        extracted: List<UIComponent>
    ): List<UIComponent> {
        
        val templateComponents = when (template) {
            ScreenTemplate.LOGIN -> listOf(
                UIComponent.Text("Welcome Back"),
                UIComponent.Email(),
                UIComponent.Password(),
                UIComponent.Button("Sign In")
            )
            ScreenTemplate.REGISTER -> listOf(
                UIComponent.Text("Create Account"),
                UIComponent.Name(),
                UIComponent.Email(),
                UIComponent.Password(),
                UIComponent.Button("Sign Up")
            )
            ScreenTemplate.SETTINGS -> listOf(
                UIComponent.Text("Settings"),
                UIComponent.Section("Preferences")
            )
            ScreenTemplate.PROFILE -> listOf(
                UIComponent.Text("Profile"),
                UIComponent.Avatar(),
                UIComponent.Name(),
                UIComponent.Email()
            )
            ScreenTemplate.CHECKOUT -> listOf(
                UIComponent.Text("Checkout"),
                UIComponent.Address(),
                UIComponent.Card(),
                UIComponent.Button("Complete Purchase")
            )
            ScreenTemplate.CHAT -> listOf(
                UIComponent.MessageList(),
                UIComponent.Input("Type a message"),
                UIComponent.Button("Send")
            )
            ScreenTemplate.SEARCH -> listOf(
                UIComponent.SearchBar(),
                UIComponent.FilterOptions(),
                UIComponent.ResultsList()
            )
            ScreenTemplate.DASHBOARD -> listOf(
                UIComponent.Text("Dashboard"),
                UIComponent.Grid()
            )
            else -> emptyList()
        }
        
        // Merge unique components
        val combined = templateComponents.toMutableList()
        for (component in extracted) {
            if (!combined.any { it.javaClass == component.javaClass }) {
                combined.add(component)
            }
        }
        
        return combined
    }
    
    /**
     * Extract options for dropdowns from text
     */
    private fun extractOptions(text: String, label: String): List<String> {
        // Look for options in various formats
        val patterns = listOf(
            Pattern.compile("(?i)$label.*\\[(.*?)\\]"),
            Pattern.compile("(?i)$label.*\\((.*?)\\)"),
            Pattern.compile("(?i)$label.*options?:?\\s*([\\w,\\s]+)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val optionsText = matcher.group(1) ?: ""
                return optionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        
        // Default options based on label
        return when (label.lowercase()) {
            "country" -> listOf("USA", "Canada", "UK", "Australia", "Other")
            "language" -> listOf("English", "Spanish", "French", "German", "Chinese")
            "theme" -> listOf("Light", "Dark", "Auto")
            "size" -> listOf("Small", "Medium", "Large")
            else -> listOf("Option 1", "Option 2", "Option 3")
        }
    }
}

// Data classes for parsed UI structure

data class ParsedUI(
    val template: ScreenTemplate,
    val components: List<UIComponent>,
    val styles: UIStyles,
    val features: List<UIFeature>,
    val originalDescription: String
)

enum class ScreenTemplate {
    LOGIN,
    REGISTER,
    SETTINGS,
    PROFILE,
    CHECKOUT,
    CHAT,
    SEARCH,
    DASHBOARD,
    FORM,
    LIST,
    CUSTOM
}

sealed class UIComponent {
    data class Text(val content: String) : UIComponent()
    data class Email(val label: String = "Email") : UIComponent()
    data class Password(val label: String = "Password") : UIComponent()
    data class Name(val label: String = "Name", val splitFirstLast: Boolean = true) : UIComponent()
    data class Phone(val label: String = "Phone") : UIComponent()
    data class Address(val label: String = "Address") : UIComponent()
    data class Card(val label: String = "Card") : UIComponent()
    data class DatePicker(val label: String = "Date") : UIComponent()
    data class Toggle(val label: String, val default: Boolean = false) : UIComponent()
    data class Dropdown(val label: String, val options: List<String>) : UIComponent()
    data class Button(val text: String, val style: ButtonStyle = ButtonStyle.PRIMARY) : UIComponent()
    data class Section(val title: String) : UIComponent()
    data class Input(val label: String) : UIComponent()
    class Avatar : UIComponent()
    class MessageList : UIComponent()
    class SearchBar : UIComponent()
    class FilterOptions : UIComponent()
    class ResultsList : UIComponent()
    class Grid : UIComponent()
}

enum class ButtonStyle {
    PRIMARY, SECONDARY, TEXT, OUTLINED
}

data class UIStyles(
    var primaryColor: String? = null,
    var size: SizePreference = SizePreference.MEDIUM,
    var alignment: AlignmentPreference = AlignmentPreference.LEFT,
    var spacing: SpacingPreference = SpacingPreference.MEDIUM,
    var isDarkMode: Boolean = false
)

enum class SizePreference {
    SMALL, MEDIUM, LARGE, COMPACT
}

enum class AlignmentPreference {
    LEFT, CENTER, RIGHT
}

enum class SpacingPreference {
    SMALL, MEDIUM, LARGE
}

enum class UIFeature {
    REMEMBER_ME,
    FORGOT_PASSWORD,
    SOCIAL_LOGIN,
    VALIDATION,
    VOICE_INPUT,
    RESPONSIVE,
    LOADING_STATE,
    ACCESSIBILITY
}

// Intent types for pattern matching
sealed class Intent {
    data class Component(val type: ComponentIntent) : Intent()
    data class Style(val type: StyleIntent) : Intent()
    data class Feature(val type: FeatureIntent) : Intent()
}

enum class ComponentIntent {
    EMAIL, PASSWORD, NAME, PHONE, ADDRESS, CARD, DATE, TOGGLE, DROPDOWN, LIST
}

enum class StyleIntent {
    COLORED_BUTTON, LARGE, SMALL, CENTERED, DARK_MODE
}

enum class FeatureIntent {
    REMEMBER_ME, FORGOT_PASSWORD, SOCIAL_LOGIN, VALIDATION, VOICE_INPUT, RESPONSIVE
}
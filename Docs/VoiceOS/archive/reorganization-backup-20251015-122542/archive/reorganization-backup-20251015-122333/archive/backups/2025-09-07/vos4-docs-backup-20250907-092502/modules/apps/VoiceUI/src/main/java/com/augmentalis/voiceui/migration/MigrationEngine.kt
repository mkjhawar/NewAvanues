package com.augmentalis.voiceui.migration

import kotlinx.coroutines.*
import java.io.File
import kotlin.text.Regex

/**
 * MigrationEngine - Convert existing code to VoiceUI with preview
 * 
 * Features:
 * - Analyze existing VoiceUI/Compose/XML code
 * - Generate VoiceUI equivalent
 * - Show side-by-side preview
 * - Safe rollback capability
 */
object MigrationEngine {
    
    private val migrationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val migrationHistory = mutableListOf<MigrationRecord>()
    
    /**
     * Analyze and migrate code with preview
     */
    suspend fun migrateWithPreview(
        sourceCode: String,
        sourceType: SourceType = detectSourceType(sourceCode),
        options: MigrationOptions = MigrationOptions()
    ): MigrationResult {
        
        return withContext(Dispatchers.Default) {
            // Step 1: Analyze source code
            val analysis = analyzeCode(sourceCode, sourceType)
            
            // Step 2: Generate VoiceUI code
            val generatedCode = generateVoiceUI(analysis, options)
            
            // Step 3: Optimize generated code
            val optimizedCode = if (options.optimize) {
                optimizeCode(generatedCode)
            } else generatedCode
            
            // Step 4: Create preview
            val preview = createPreview(sourceCode, optimizedCode, analysis)
            
            // Step 5: Calculate improvements
            val improvements = calculateImprovements(sourceCode, optimizedCode)
            
            MigrationResult(
                originalCode = sourceCode,
                generatedCode = optimizedCode,
                preview = preview,
                analysis = analysis,
                improvements = improvements,
                canRollback = true
            )
        }
    }
    
    /**
     * Apply migration after preview approval
     */
    suspend fun applyMigration(
        result: MigrationResult,
        targetFile: File? = null
    ): ApplyResult {
        
        return withContext(Dispatchers.IO) {
            try {
                // Create backup
                val backup = if (targetFile != null && targetFile.exists()) {
                    createBackup(targetFile)
                } else null
                
                // Apply the migration
                targetFile?.writeText(result.generatedCode)
                
                // Record in history
                val record = MigrationRecord(
                    timestamp = System.currentTimeMillis(),
                    originalFile = targetFile?.absolutePath,
                    backup = backup?.absolutePath,
                    originalCode = result.originalCode,
                    generatedCode = result.generatedCode
                )
                migrationHistory.add(record)
                
                ApplyResult(
                    success = true,
                    backupPath = backup?.absolutePath,
                    record = record
                )
            } catch (e: Exception) {
                ApplyResult(
                    success = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Rollback a migration
     */
    suspend fun rollback(record: MigrationRecord): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (record.backup != null && record.originalFile != null) {
                    val backupFile = File(record.backup)
                    val originalFile = File(record.originalFile)
                    
                    if (backupFile.exists()) {
                        backupFile.copyTo(originalFile, overwrite = true)
                        migrationHistory.remove(record)
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Analyze source code structure
     */
    private fun analyzeCode(code: String, type: SourceType): CodeAnalysis {
        val components = mutableListOf<DetectedComponent>()
        val states = mutableListOf<DetectedState>()
        val imports = mutableListOf<String>()
        
        when (type) {
            SourceType.VOICE_UI -> analyzeVoiceUI(code, components, states, imports)
            SourceType.COMPOSE -> analyzeCompose(code, components, states, imports)
            SourceType.XML -> analyzeXML(code, components, states, imports)
            SourceType.FLUTTER -> analyzeFlutter(code, components, states, imports)
        }
        
        return CodeAnalysis(
            sourceType = type,
            components = components,
            states = states,
            imports = imports,
            lineCount = code.lines().size,
            complexity = calculateComplexity(code)
        )
    }
    
    /**
     * Analyze VoiceUI code
     */
    private fun analyzeVoiceUI(
        code: String,
        components: MutableList<DetectedComponent>,
        states: MutableList<DetectedState>,
        @Suppress("UNUSED_PARAMETER") imports: MutableList<String>
    ) {
        // Find state declarations
        val statePattern = Regex("""var\s+(\w+)\s+by\s+remember\s*\{\s*mutableStateOf\((.*?)\)\}""")
        statePattern.findAll(code).forEach { match ->
            states.add(DetectedState(
                name = match.groupValues[1],
                type = "String", // Simplified
                defaultValue = match.groupValues[2]
            ))
        }
        
        // Find components
        val componentPatterns = mapOf(
            "input" to ComponentType.INPUT,
            "password" to ComponentType.PASSWORD,
            "button" to ComponentType.BUTTON,
            "text" to ComponentType.TEXT,
            "dropdown" to ComponentType.DROPDOWN,
            "toggle" to ComponentType.TOGGLE
        )
        
        componentPatterns.forEach { (pattern, type) ->
            val regex = Regex("""$pattern\s*\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
            regex.findAll(code).forEach { match ->
                components.add(DetectedComponent(
                    type = type,
                    parameters = parseParameters(match.groupValues[1])
                ))
            }
        }
    }
    
    /**
     * Analyze Compose code
     */
    private fun analyzeCompose(
        code: String,
        components: MutableList<DetectedComponent>,
        states: MutableList<DetectedState>,
        @Suppress("UNUSED_PARAMETER") imports: MutableList<String>
    ) {
        // Find Compose components
        val composeComponents = mapOf(
            "TextField" to ComponentType.INPUT,
            "OutlinedTextField" to ComponentType.INPUT,
            "Button" to ComponentType.BUTTON,
            "Text" to ComponentType.TEXT,
            "Switch" to ComponentType.TOGGLE,
            "Checkbox" to ComponentType.TOGGLE,
            "DropdownMenu" to ComponentType.DROPDOWN
        )
        
        composeComponents.forEach { (pattern, type) ->
            val regex = Regex("""$pattern\s*\((.*?)\)(?:\s*\{|$)""", RegexOption.DOT_MATCHES_ALL)
            regex.findAll(code).forEach { match ->
                components.add(DetectedComponent(
                    type = type,
                    parameters = parseParameters(match.groupValues[1])
                ))
            }
        }
        
        // Find state
        val statePattern = Regex("""var\s+(\w+)\s+by\s+remember(?:SaveableState)?\s*\{\s*mutableStateOf\((.*?)\)\}""")
        statePattern.findAll(code).forEach { match ->
            states.add(DetectedState(
                name = match.groupValues[1],
                type = inferType(match.groupValues[2]),
                defaultValue = match.groupValues[2]
            ))
        }
    }
    
    /**
     * Analyze XML layout
     */
    private fun analyzeXML(
        code: String,
        components: MutableList<DetectedComponent>,
        @Suppress("UNUSED_PARAMETER") states: MutableList<DetectedState>,
        @Suppress("UNUSED_PARAMETER") imports: MutableList<String>
    ) {
        // Parse XML components
        val xmlComponents = mapOf(
            "EditText" to ComponentType.INPUT,
            "Button" to ComponentType.BUTTON,
            "TextView" to ComponentType.TEXT,
            "Switch" to ComponentType.TOGGLE,
            "CheckBox" to ComponentType.TOGGLE,
            "Spinner" to ComponentType.DROPDOWN
        )
        
        xmlComponents.forEach { (tag, type) ->
            val regex = Regex("""<$tag(.*?)(?:/>|>.*?</$tag>)""", RegexOption.DOT_MATCHES_ALL)
            regex.findAll(code).forEach { match ->
                val attributes = parseXMLAttributes(match.groupValues[1])
                components.add(DetectedComponent(
                    type = type,
                    parameters = attributes
                ))
            }
        }
    }
    
    /**
     * Analyze Flutter code
     */
    private fun analyzeFlutter(
        code: String,
        components: MutableList<DetectedComponent>,
        @Suppress("UNUSED_PARAMETER") states: MutableList<DetectedState>,
        @Suppress("UNUSED_PARAMETER") imports: MutableList<String>
    ) {
        // Flutter widget detection
        val flutterWidgets = mapOf(
            "TextField" to ComponentType.INPUT,
            "ElevatedButton" to ComponentType.BUTTON,
            "Text" to ComponentType.TEXT,
            "Switch" to ComponentType.TOGGLE,
            "Checkbox" to ComponentType.TOGGLE,
            "DropdownButton" to ComponentType.DROPDOWN
        )
        
        flutterWidgets.forEach { (widget, type) ->
            val regex = Regex("""$widget\s*\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
            regex.findAll(code).forEach { match ->
                components.add(DetectedComponent(
                    type = type,
                    parameters = parseParameters(match.groupValues[1])
                ))
            }
        }
    }
    
    /**
     * Generate VoiceUI code from analysis
     */
    private fun generateVoiceUI(
        analysis: CodeAnalysis,
        options: MigrationOptions
    ): String {
        
        val builder = StringBuilder()
        
        // Add imports
        builder.appendLine("import com.augmentalis.voiceui.dsl.*")
        builder.appendLine("import androidx.compose.runtime.*")
        builder.appendLine()
        
        // Generate function
        builder.appendLine("@Composable")
        builder.appendLine("fun MigratedScreen() {")
        
        // Use natural language if enabled
        if (options.useNaturalLanguage && analysis.components.size < 10) {
            val description = generateNaturalDescription(analysis)
            builder.appendLine("    MagicScreen(description = \"$description\")")
        } else {
            // Generate component by component
            builder.appendLine("    MagicScreen {")
            
            // Convert components to VoiceUI
            analysis.components.forEach { component ->
                val voiceUIComponent = convertToVoiceUI(component)
                builder.appendLine("        $voiceUIComponent")
            }
            
            builder.appendLine("    }")
        }
        
        builder.appendLine("}")
        
        return builder.toString()
    }
    
    /**
     * Convert component to VoiceUI syntax
     */
    private fun convertToVoiceUI(component: DetectedComponent): String {
        return when (component.type) {
            ComponentType.INPUT -> {
                val label = component.parameters["label"] ?: component.parameters["hint"] ?: "Input"
                if (label.contains("email", ignoreCase = true)) {
                    "email()"
                } else if (label.contains("phone", ignoreCase = true)) {
                    "phone()"
                } else if (label.contains("name", ignoreCase = true)) {
                    "name()"
                } else {
                    "input(\"$label\")"
                }
            }
            ComponentType.PASSWORD -> "password()"
            ComponentType.BUTTON -> {
                val text = component.parameters["text"] ?: "Submit"
                "submit(\"$text\") { /* action */ }"
            }
            ComponentType.TEXT -> {
                val text = component.parameters["text"] ?: ""
                "text(\"$text\")"
            }
            ComponentType.TOGGLE -> {
                val label = component.parameters["label"] ?: "Option"
                "toggle(\"$label\")"
            }
            ComponentType.DROPDOWN -> {
                val label = component.parameters["label"] ?: "Select"
                "dropdown(\"$label\", listOf(\"Option 1\", \"Option 2\"))"
            }
            else -> "// Unknown component"
        }
    }
    
    /**
     * Generate natural language description
     */
    private fun generateNaturalDescription(analysis: CodeAnalysis): String {
        val components = analysis.components
        
        // Detect screen type
        val hasEmail = components.any { it.type == ComponentType.INPUT && 
            it.parameters.values.any { it.contains("email", ignoreCase = true) } }
        val hasPassword = components.any { it.type == ComponentType.PASSWORD }
        val hasName = components.any { it.type == ComponentType.INPUT && 
            it.parameters.values.any { it.contains("name", ignoreCase = true) } }
        
        return when {
            hasEmail && hasPassword && !hasName -> "login screen with remember me option"
            hasEmail && hasPassword && hasName -> "registration form with name, email and password"
            components.any { it.type == ComponentType.TOGGLE } -> "settings screen with toggles"
            else -> "form with ${components.size} fields"
        }
    }
    
    /**
     * Optimize generated code
     */
    private fun optimizeCode(code: String): String {
        // Remove redundant code
        var optimized = code
        
        // Combine sequential spacers
        optimized = optimized.replace(Regex("""spacer\(\d+\)\s+spacer\(\d+\)""")) { _ ->
            "spacer(24)"
        }
        
        // Simplify common patterns
        optimized = optimized.replace(
            """input("Email")""",
            "email()"
        )
        
        optimized = optimized.replace(
            """input("Password")""",
            "password()"
        )
        
        return optimized
    }
    
    /**
     * Create side-by-side preview
     */
    private fun createPreview(
        original: String,
        generated: String,
        analysis: CodeAnalysis
    ): MigrationPreview {
        return MigrationPreview(
            originalCode = original,
            generatedCode = generated,
            originalLineCount = original.lines().size,
            generatedLineCount = generated.lines().size,
            reduction = calculateReduction(original, generated),
            improvements = listOf(
                "Automatic state management",
                "Built-in validation",
                "Voice commands enabled",
                "GPU acceleration ready",
                "${analysis.states.size} state variables eliminated"
            )
        )
    }
    
    /**
     * Calculate code improvements
     */
    private fun calculateImprovements(original: String, generated: String): CodeImprovements {
        val originalLines = original.lines().size
        val generatedLines = generated.lines().size
        
        return CodeImprovements(
            lineReduction = ((originalLines - generatedLines) * 100.0 / originalLines).toInt(),
            charactersReduction = ((original.length - generated.length) * 100.0 / original.length).toInt(),
            complexityReduction = 50, // Estimated
            featuresAdded = listOf(
                "Voice commands",
                "Auto-validation",
                "State management",
                "Localization",
                "GPU acceleration"
            )
        )
    }
    
    /**
     * Calculate code reduction percentage
     */
    private fun calculateReduction(original: String, generated: String): Int {
        val originalLines = original.lines().filter { it.isNotBlank() }.size
        val generatedLines = generated.lines().filter { it.isNotBlank() }.size
        
        return if (originalLines > 0) {
            ((originalLines - generatedLines) * 100 / originalLines).coerceAtLeast(0)
        } else 0
    }
    
    /**
     * Calculate code complexity
     */
    private fun calculateComplexity(code: String): Int {
        var complexity = 0
        
        // Count control structures
        complexity += Regex("""if\s*\(""").findAll(code).count() * 2
        complexity += Regex("""for\s*\(""").findAll(code).count() * 3
        complexity += Regex("""while\s*\(""").findAll(code).count() * 3
        complexity += Regex("""when\s*\{""").findAll(code).count() * 2
        
        // Count function calls
        complexity += Regex("""\w+\(""").findAll(code).count()
        
        return complexity
    }
    
    /**
     * Detect source code type
     */
    private fun detectSourceType(code: String): SourceType {
        return when {
            code.contains("VoiceScreen") -> SourceType.VOICE_UI
            code.contains("@Composable") -> SourceType.COMPOSE
            code.contains("<LinearLayout") || code.contains("<RelativeLayout") -> SourceType.XML
            code.contains("Widget") && code.contains("flutter") -> SourceType.FLUTTER
            else -> SourceType.COMPOSE
        }
    }
    
    /**
     * Parse parameters from function call
     */
    private fun parseParameters(params: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Simple parameter parsing
        val pairs = params.split(",").map { it.trim() }
        pairs.forEach { pair ->
            if (pair.contains("=")) {
                val parts = pair.split("=", limit = 2)
                result[parts[0].trim()] = parts[1].trim().trim('"', '\'')
            } else if (pair.isNotEmpty()) {
                result["value"] = pair.trim('"', '\'')
            }
        }
        
        return result
    }
    
    /**
     * Parse XML attributes
     */
    private fun parseXMLAttributes(attributes: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        val pattern = Regex("""(\w+:?\w+)="([^"]*)"""")
        pattern.findAll(attributes).forEach { match ->
            val key = match.groupValues[1].substringAfter(":")
            val value = match.groupValues[2]
            result[key] = value
        }
        
        return result
    }
    
    /**
     * Infer type from value
     */
    private fun inferType(value: String): String {
        return when {
            value == "true" || value == "false" -> "Boolean"
            value.toIntOrNull() != null -> "Int"
            value.toFloatOrNull() != null -> "Float"
            value.startsWith("\"") -> "String"
            else -> "Any"
        }
    }
    
    /**
     * Create backup of file
     */
    private fun createBackup(file: File): File {
        val backupFile = File(file.parent, "${file.nameWithoutExtension}_backup_${System.currentTimeMillis()}.${file.extension}")
        file.copyTo(backupFile)
        return backupFile
    }
}

// Data classes

data class MigrationOptions(
    val useNaturalLanguage: Boolean = true,
    val optimize: Boolean = true,
    val preserveComments: Boolean = false,
    val generateTests: Boolean = false
)

data class MigrationResult(
    val originalCode: String,
    val generatedCode: String,
    val preview: MigrationPreview,
    val analysis: CodeAnalysis,
    val improvements: CodeImprovements,
    val canRollback: Boolean
)

data class MigrationPreview(
    val originalCode: String,
    val generatedCode: String,
    val originalLineCount: Int,
    val generatedLineCount: Int,
    val reduction: Int,
    val improvements: List<String>
)

data class CodeAnalysis(
    val sourceType: SourceType,
    val components: List<DetectedComponent>,
    val states: List<DetectedState>,
    val imports: List<String>,
    val lineCount: Int,
    val complexity: Int
)

data class DetectedComponent(
    val type: ComponentType,
    val parameters: Map<String, String>
)

data class DetectedState(
    val name: String,
    val type: String,
    val defaultValue: String
)

data class CodeImprovements(
    val lineReduction: Int,
    val charactersReduction: Int,
    val complexityReduction: Int,
    val featuresAdded: List<String>
)

data class MigrationRecord(
    val timestamp: Long,
    val originalFile: String?,
    val backup: String?,
    val originalCode: String,
    val generatedCode: String
)

data class ApplyResult(
    val success: Boolean,
    val backupPath: String? = null,
    val record: MigrationRecord? = null,
    val error: String? = null
)

enum class SourceType {
    VOICE_UI,
    COMPOSE,
    XML,
    FLUTTER
}

enum class ComponentType {
    INPUT,
    PASSWORD,
    EMAIL,
    PHONE,
    NAME,
    ADDRESS,
    CARD,
    DATE,
    BUTTON,
    TEXT,
    TOGGLE,
    DROPDOWN,
    SLIDER,
    IMAGE,
    LIST,
    GRID
}
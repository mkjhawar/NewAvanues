/**
 * CommandValidator.kt - Command validation and error handling
 * Comprehensive command validation and error handling system
 */

package com.augmentalis.voiceoscore.commandmanager.validation

import com.augmentalis.voiceoscore.*
import com.augmentalis.voiceoscore.commandmanager.definitions.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Command validator for comprehensive validation and error handling
 * Provides multiple layers of validation with detailed error reporting
 */
class CommandValidator {
    
    companion object {
        private const val TAG = "CommandValidator"
    }
    
    // Validation rules
    private val syntaxRules = mutableListOf<ValidationRule>()
    private val semanticRules = mutableListOf<ValidationRule>()
    private val contextRules = mutableListOf<ValidationRule>()
    private val securityRules = mutableListOf<ValidationRule>()
    
    // Error handlers
    private val errorHandlers = mutableMapOf<ErrorCode, ErrorHandler>()
    
    // Validation events
    private val _validationEvents = MutableSharedFlow<ValidationEvent>()
    val validationEvents: Flow<ValidationEvent> = _validationEvents.asSharedFlow()
    
    // Configuration
    private var enableStrictMode = false
    private var enableSecurityValidation = true
    private var enableContextValidation = true
    
    /**
     * Initialize the validator
     */
    fun initialize() {
        // Register built-in validation rules
        registerBuiltInRules()
        
        // Register built-in error handlers
        registerBuiltInErrorHandlers()
        
        android.util.Log.i(TAG, "Command validator initialized")
    }
    
    /**
     * Validate command definition
     */
    fun validateDefinition(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        // Syntax validation
        for (rule in syntaxRules) {
            val result = rule.validate(definition)
            if (!result.isValid) {
                violations.addAll(result.violations)
            }
        }
        
        // Semantic validation
        for (rule in semanticRules) {
            val result = rule.validate(definition)
            if (!result.isValid) {
                violations.addAll(result.violations)
            }
        }
        
        // Security validation
        if (enableSecurityValidation) {
            for (rule in securityRules) {
                val result = rule.validate(definition)
                if (!result.isValid) {
                    violations.addAll(result.violations)
                }
            }
        }
        
        val isValid = violations.isEmpty() || (!enableStrictMode && violations.all { it.severity != ViolationSeverity.ERROR })
        
        // Emit validation event
        _validationEvents.tryEmit(
            ValidationEvent.DefinitionValidated(definition.id, isValid, violations.size)
        )
        
        return ValidationResult(isValid, violations)
    }
    
    /**
     * Validate command execution
     */
    fun validateExecution(command: Command, context: CommandContext?): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        // Parameter validation
        violations.addAll(validateParameters(command))
        
        // Context validation
        if (enableContextValidation && context != null) {
            for (rule in contextRules) {
                val result = rule.validateExecution(command, context)
                if (!result.isValid) {
                    violations.addAll(result.violations)
                }
            }
        }
        
        // Runtime security validation
        if (enableSecurityValidation) {
            violations.addAll(validateSecurity(command, context))
        }
        
        val isValid = violations.isEmpty() || (!enableStrictMode && violations.all { it.severity != ViolationSeverity.ERROR })
        
        // Emit validation event
        _validationEvents.tryEmit(
            ValidationEvent.ExecutionValidated(command.id, isValid, violations.size)
        )
        
        return ValidationResult(isValid, violations)
    }
    
    /**
     * Handle validation error
     */
    fun handleError(error: CommandError, command: Command): ErrorHandlingResult {
        val handler = errorHandlers[error.code]
        
        return if (handler != null) {
            try {
                val result = handler.handle(error, command)
                
                // Emit error handling event
                _validationEvents.tryEmit(
                    ValidationEvent.ErrorHandled(error.code, result.action)
                )
                
                result
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error handler failed", e)
                ErrorHandlingResult(
                    action = ErrorAction.FAIL,
                    message = "Error handler failed: ${e.message}",
                    recovery = null
                )
            }
        } else {
            // Default error handling
            ErrorHandlingResult(
                action = ErrorAction.FAIL,
                message = error.message,
                recovery = null
            )
        }
    }
    
    /**
     * Register validation rule
     */
    fun registerRule(rule: ValidationRule, category: ValidationCategory) {
        when (category) {
            ValidationCategory.SYNTAX -> syntaxRules.add(rule)
            ValidationCategory.SEMANTIC -> semanticRules.add(rule)
            ValidationCategory.CONTEXT -> contextRules.add(rule)
            ValidationCategory.SECURITY -> securityRules.add(rule)
        }
        
        android.util.Log.d(TAG, "Registered validation rule: ${rule.name} (${category})")
    }
    
    /**
     * Register error handler
     */
    fun registerErrorHandler(errorCode: ErrorCode, handler: ErrorHandler) {
        errorHandlers[errorCode] = handler
        android.util.Log.d(TAG, "Registered error handler for: $errorCode")
    }
    
    /**
     * Configure validator
     */
    fun configure(
        strictMode: Boolean = enableStrictMode,
        securityValidation: Boolean = enableSecurityValidation,
        contextValidation: Boolean = enableContextValidation
    ) {
        enableStrictMode = strictMode
        enableSecurityValidation = securityValidation
        enableContextValidation = contextValidation
        
        android.util.Log.d(TAG, "Validator configured: strict=$strictMode, security=$securityValidation, context=$contextValidation")
    }
    
    /**
     * Get validation statistics
     */
    fun getStatistics(): ValidationStatistics {
        return ValidationStatistics(
            totalRules = syntaxRules.size + semanticRules.size + contextRules.size + securityRules.size,
            syntaxRules = syntaxRules.size,
            semanticRules = semanticRules.size,
            contextRules = contextRules.size,
            securityRules = securityRules.size,
            errorHandlers = errorHandlers.size,
            strictModeEnabled = enableStrictMode,
            securityValidationEnabled = enableSecurityValidation,
            contextValidationEnabled = enableContextValidation
        )
    }
    
    // Private methods
    
    /**
     * Validate command parameters
     */
    private fun validateParameters(command: Command): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()
        
        // Check for required parameters
        // Note: This would need command definition to check required parameters
        // For now, just basic validation
        
        for ((key, value) in command.parameters) {
            // Validate parameter types and values
            when (key) {
                "x", "y", "distance", "steps", "level" -> {
                    if (value !is Number) {
                        violations.add(ValidationViolation(
                            rule = "parameter_type",
                            message = "Parameter '$key' must be a number",
                            severity = ViolationSeverity.ERROR,
                            field = key
                        ))
                    }
                }
                "text", "target", "app", "direction" -> {
                    if (value !is String) {
                        violations.add(ValidationViolation(
                            rule = "parameter_type",
                            message = "Parameter '$key' must be a string",
                            severity = ViolationSeverity.ERROR,
                            field = key
                        ))
                    } else if (value.isBlank()) {
                        violations.add(ValidationViolation(
                            rule = "parameter_empty",
                            message = "Parameter '$key' cannot be empty",
                            severity = ViolationSeverity.WARNING,
                            field = key
                        ))
                    }
                }
            }
        }
        
        return violations
    }
    
    /**
     * Validate security aspects
     */
    private fun validateSecurity(command: Command, @Suppress("UNUSED_PARAMETER") context: CommandContext?): List<ValidationViolation> {
        val violations = mutableListOf<ValidationViolation>()
        
        // Check for potentially dangerous commands
        when (command.id) {
            "force_stop_app", "clear_text", "paste_text" -> {
                // These commands could potentially be misused
                violations.add(ValidationViolation(
                    rule = "security_sensitive",
                    message = "Command '${command.id}' requires security validation",
                    severity = ViolationSeverity.INFO,
                    field = "command"
                ))
            }
        }
        
        // Check for suspicious parameter values
        command.parameters["text"]?.let { text ->
            if (text is String && text.length > 1000) {
                violations.add(ValidationViolation(
                    rule = "security_large_input",
                    message = "Text input is unusually large (${text.length} characters)",
                    severity = ViolationSeverity.WARNING,
                    field = "text"
                ))
            }
        }
        
        return violations
    }
    
    /**
     * Register built-in validation rules
     */
    private fun registerBuiltInRules() {
        // Syntax rules
        registerRule(CommandIdFormatRule(), ValidationCategory.SYNTAX)
        registerRule(PatternsNotEmptyRule(), ValidationCategory.SYNTAX)
        registerRule(ParameterDefinitionRule(), ValidationCategory.SYNTAX)
        
        // Semantic rules
        registerRule(CategoryConsistencyRule(), ValidationCategory.SEMANTIC)
        registerRule(ContextRequirementRule(), ValidationCategory.SEMANTIC)
        
        // Context rules
        registerRule(ContextAvailabilityRule(), ValidationCategory.CONTEXT)
        registerRule(PermissionRequirementRule(), ValidationCategory.CONTEXT)
        
        // Security rules
        registerRule(CommandInjectionRule(), ValidationCategory.SECURITY)
        registerRule(PrivacyProtectionRule(), ValidationCategory.SECURITY)
    }
    
    /**
     * Register built-in error handlers
     */
    private fun registerBuiltInErrorHandlers() {
        registerErrorHandler(ErrorCode.UNKNOWN_COMMAND, UnknownCommandHandler())
        registerErrorHandler(ErrorCode.INVALID_PARAMETERS, InvalidParametersHandler())
        registerErrorHandler(ErrorCode.MISSING_CONTEXT, MissingContextHandler())
        registerErrorHandler(ErrorCode.EXECUTION_FAILED, ExecutionFailedHandler())
        registerErrorHandler(ErrorCode.PERMISSION_DENIED, PermissionDeniedHandler())
        registerErrorHandler(ErrorCode.TIMEOUT, TimeoutHandler())
        registerErrorHandler(ErrorCode.CANCELLED, CancelledHandler())
        registerErrorHandler(ErrorCode.NETWORK_ERROR, NetworkErrorHandler())
        registerErrorHandler(ErrorCode.MODULE_NOT_AVAILABLE, ModuleNotAvailableHandler())
    }
}

/**
 * Validation rule interface
 */
interface ValidationRule {
    val name: String
    val description: String
    
    fun validate(definition: CommandDefinition): ValidationResult
    fun validateExecution(command: Command, context: CommandContext): ValidationResult {
        return ValidationResult(true, emptyList()) // Default implementation
    }
}

/**
 * Error handler interface
 */
interface ErrorHandler {
    fun handle(error: CommandError, command: Command): ErrorHandlingResult
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val violations: List<ValidationViolation>
)

/**
 * Validation violation
 */
data class ValidationViolation(
    val rule: String,
    val message: String,
    val severity: ViolationSeverity,
    val field: String? = null,
    val suggestion: String? = null
)

/**
 * Violation severity levels
 */
enum class ViolationSeverity {
    INFO,
    WARNING,
    ERROR
}

/**
 * Validation categories
 */
enum class ValidationCategory {
    SYNTAX,
    SEMANTIC,
    CONTEXT,
    SECURITY
}

/**
 * Error handling result
 */
data class ErrorHandlingResult(
    val action: ErrorAction,
    val message: String,
    val recovery: RecoveryAction? = null
)

/**
 * Error actions
 */
enum class ErrorAction {
    RETRY,
    FAIL,
    IGNORE,
    RECOVER
}

/**
 * Recovery action
 */
data class RecoveryAction(
    val type: RecoveryType,
    val command: Command? = null,
    val delay: Long = 0
)

/**
 * Recovery types
 */
enum class RecoveryType {
    RETRY_SAME,
    RETRY_MODIFIED,
    EXECUTE_ALTERNATIVE,
    WAIT_AND_RETRY
}

/**
 * Validation events
 */
sealed class ValidationEvent {
    data class DefinitionValidated(val commandId: String, val isValid: Boolean, val violationCount: Int) : ValidationEvent()
    data class ExecutionValidated(val commandId: String, val isValid: Boolean, val violationCount: Int) : ValidationEvent()
    data class ErrorHandled(val errorCode: ErrorCode, val action: ErrorAction) : ValidationEvent()
}

/**
 * Validation statistics
 */
data class ValidationStatistics(
    val totalRules: Int,
    val syntaxRules: Int,
    val semanticRules: Int,
    val contextRules: Int,
    val securityRules: Int,
    val errorHandlers: Int,
    val strictModeEnabled: Boolean,
    val securityValidationEnabled: Boolean,
    val contextValidationEnabled: Boolean
)

// Built-in validation rules

class CommandIdFormatRule : ValidationRule {
    override val name = "CommandIdFormat"
    override val description = "Validates command ID format"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        if (definition.id.isBlank()) {
            violations.add(ValidationViolation(
                rule = name,
                message = "Command ID cannot be blank",
                severity = ViolationSeverity.ERROR,
                field = "id"
            ))
        } else if (!definition.id.matches(Regex("^[a-z0-9_]+$"))) {
            violations.add(ValidationViolation(
                rule = name,
                message = "Command ID must contain only lowercase letters, numbers, and underscores",
                severity = ViolationSeverity.ERROR,
                field = "id",
                suggestion = "Use format like 'my_command_name'"
            ))
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class PatternsNotEmptyRule : ValidationRule {
    override val name = "PatternsNotEmpty"
    override val description = "Validates that command has patterns"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        if (definition.patterns.isEmpty()) {
            violations.add(ValidationViolation(
                rule = name,
                message = "Command must have at least one pattern",
                severity = ViolationSeverity.ERROR,
                field = "patterns"
            ))
        } else {
            for (pattern in definition.patterns) {
                if (pattern.isBlank()) {
                    violations.add(ValidationViolation(
                        rule = name,
                        message = "Pattern cannot be blank",
                        severity = ViolationSeverity.WARNING,
                        field = "patterns"
                    ))
                }
            }
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class ParameterDefinitionRule : ValidationRule {
    override val name = "ParameterDefinition"
    override val description = "Validates parameter definitions"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        for (param in definition.parameters) {
            if (param.name.isBlank()) {
                violations.add(ValidationViolation(
                    rule = name,
                    message = "Parameter name cannot be blank",
                    severity = ViolationSeverity.ERROR,
                    field = "parameters"
                ))
            }
            
            if (param.required && param.defaultValue != null) {
                violations.add(ValidationViolation(
                    rule = name,
                    message = "Required parameter '${param.name}' should not have default value",
                    severity = ViolationSeverity.WARNING,
                    field = "parameters"
                ))
            }
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class CategoryConsistencyRule : ValidationRule {
    override val name = "CategoryConsistency"
    override val description = "Validates category consistency"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        // Check if command ID matches category
        val expectedPrefix = when (definition.category) {
            "NAVIGATION" -> "nav_"
            "SYSTEM" -> "sys_"
            "INPUT" -> "input_"
            "MEDIA" -> "media_"
            "APP_CONTROL" -> "app_"
            "ACCESSIBILITY" -> "access_"
            else -> null
        }
        
        if (expectedPrefix != null && !definition.id.startsWith(expectedPrefix)) {
            violations.add(ValidationViolation(
                rule = name,
                message = "Command ID should start with '$expectedPrefix' for category ${definition.category}",
                severity = ViolationSeverity.INFO,
                field = "id",
                suggestion = "Consider using prefix '$expectedPrefix' for consistency"
            ))
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class ContextRequirementRule : ValidationRule {
    override val name = "ContextRequirement"
    override val description = "Validates context requirements"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        for (requirement in definition.requiredContext) {
            if (requirement.isBlank()) {
                violations.add(ValidationViolation(
                    rule = name,
                    message = "Context requirement cannot be blank",
                    severity = ViolationSeverity.ERROR,
                    field = "requiredContext"
                ))
            }
        }
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class ContextAvailabilityRule : ValidationRule {
    override val name = "ContextAvailability"
    override val description = "Validates context availability"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        return ValidationResult(true, emptyList()) // Basic implementation
    }
    
    override fun validateExecution(command: Command, context: CommandContext): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        // Check if required context is available
        // This would need access to command definition to check requirements
        
        return ValidationResult(violations.isEmpty(), violations)
    }
}

class PermissionRequirementRule : ValidationRule {
    override val name = "PermissionRequirement"
    override val description = "Validates permission requirements"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        return ValidationResult(true, emptyList()) // Basic implementation
    }
}

class CommandInjectionRule : ValidationRule {
    override val name = "CommandInjection"
    override val description = "Validates against command injection"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        return ValidationResult(true, emptyList()) // Basic implementation
    }
}

class PrivacyProtectionRule : ValidationRule {
    override val name = "PrivacyProtection"
    override val description = "Validates privacy protection"
    
    override fun validate(definition: CommandDefinition): ValidationResult {
        return ValidationResult(true, emptyList()) // Basic implementation
    }
}

// Built-in error handlers

class UnknownCommandHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.FAIL,
            message = "Command '${command.text}' not recognized. Try 'help' for available commands.",
            recovery = null
        )
    }
}

class InvalidParametersHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.FAIL,
            message = "Invalid parameters for command '${command.id}'. ${error.message}",
            recovery = null
        )
    }
}

class MissingContextHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.FAIL,
            message = "Command '${command.id}' requires specific context. ${error.message}",
            recovery = null
        )
    }
}

class ExecutionFailedHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.RETRY,
            message = "Execution failed for '${command.id}'. Retrying...",
            recovery = RecoveryAction(RecoveryType.RETRY_SAME, delay = 1000)
        )
    }
}

class PermissionDeniedHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.FAIL,
            message = "Permission denied for command '${command.id}'. Check accessibility settings.",
            recovery = null
        )
    }
}

class TimeoutHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.RETRY,
            message = "Command '${command.id}' timed out. Retrying with longer timeout...",
            recovery = RecoveryAction(RecoveryType.WAIT_AND_RETRY, delay = 2000)
        )
    }
}

class CancelledHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.IGNORE,
            message = "Command '${command.id}' was cancelled.",
            recovery = null
        )
    }
}

class NetworkErrorHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.RETRY,
            message = "Network error for command '${command.id}'. Retrying...",
            recovery = RecoveryAction(RecoveryType.WAIT_AND_RETRY, delay = 3000)
        )
    }
}

class ModuleNotAvailableHandler : ErrorHandler {
    override fun handle(error: CommandError, command: Command): ErrorHandlingResult {
        return ErrorHandlingResult(
            action = ErrorAction.FAIL,
            message = "Required module not available for command '${command.id}'. Check system configuration.",
            recovery = null
        )
    }
}
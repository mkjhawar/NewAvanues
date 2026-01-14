package com.augmentalis.avacode.forms

/**
 * Core form definition builder using Kotlin DSL.
 *
 * Creates forms with automatic database schema generation, validation,
 * and completion tracking.
 *
 * ## Example
 * ```kotlin
 * val userForm = form("user_registration") {
 *     textField("username") {
 *         required()
 *         minLength(3)
 *         maxLength(20)
 *         pattern("[a-zA-Z0-9_]+")
 *     }
 *
 *     emailField("email") {
 *         required()
 *         unique()
 *     }
 *
 *     passwordField("password") {
 *         required()
 *         minLength(8)
 *         requireUppercase()
 *         requireNumber()
 *     }
 * }
 * ```
 *
 * @property id Unique form identifier (used for database table name)
 * @property fields List of field definitions
 * @property metadata Additional form configuration
 * @since 1.2.0
 */
data class FormDefinition(
    val id: String,
    val fields: List<FieldDefinition>,
    val metadata: FormMetadata = FormMetadata()
) {
    init {
        require(id.isNotBlank()) { "Form ID cannot be blank" }
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) {
            "Form ID must be lowercase alphanumeric with underscores, starting with letter"
        }
        require(fields.isNotEmpty()) { "Form must have at least one field" }

        // Check for duplicate field IDs
        val duplicates = fields.groupBy { it.id }.filter { it.value.size > 1 }
        require(duplicates.isEmpty()) {
            "Duplicate field IDs: ${duplicates.keys.joinToString()}"
        }
    }

    /**
     * Get database schema DDL for this form.
     */
    fun toSchema(): DatabaseSchema = DatabaseSchema.fromForm(this)

    /**
     * Validate form data against all field rules.
     */
    fun validate(data: Map<String, Any?>): ValidationResult {
        val errors = mutableMapOf<String, List<String>>()

        fields.forEach { field ->
            val value = data[field.id]
            val fieldErrors = field.validate(value)
            if (fieldErrors.isNotEmpty()) {
                errors[field.id] = fieldErrors
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Calculate completion percentage for partial form data.
     */
    fun calculateCompletion(data: Map<String, Any?>): CompletionStatus {
        val requiredFields = fields.filter { it.isRequired }
        val optionalFields = fields.filter { !it.isRequired }

        val completedRequired = requiredFields.count {
            data[it.id] != null && data[it.id] != ""
        }
        val completedOptional = optionalFields.count {
            data[it.id] != null && data[it.id] != ""
        }

        val requiredPercentage = if (requiredFields.isEmpty()) 100f
            else (completedRequired.toFloat() / requiredFields.size) * 100f
        val overallPercentage = if (fields.isEmpty()) 100f
            else ((completedRequired + completedOptional).toFloat() / fields.size) * 100f

        return CompletionStatus(
            requiredComplete = requiredPercentage == 100f,
            requiredPercentage = requiredPercentage,
            overallPercentage = overallPercentage,
            completedFields = completedRequired + completedOptional,
            totalFields = fields.size,
            missingRequired = requiredFields.filter { data[it.id] == null || data[it.id] == "" }
                .map { it.id }
        )
    }

    /**
     * Create data binding for two-way sync.
     */
    fun bind(initialData: Map<String, Any?> = emptyMap()): FormBinding =
        FormBinding(this, initialData.toMutableMap())
}

/**
 * Form metadata configuration.
 */
data class FormMetadata(
    val title: String? = null,
    val description: String? = null,
    val version: String = "1.0.0",
    val allowDraft: Boolean = true,
    val autoSave: Boolean = false,
    val autoSaveInterval: Long = 30000, // 30 seconds
    val trackChanges: Boolean = true
)

/**
 * Validation result from form validation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: Map<String, List<String>>) : ValidationResult()

    val isValid: Boolean get() = this is Success
    val isInvalid: Boolean get() = this is Failure
}

/**
 * Form completion status.
 */
data class CompletionStatus(
    val requiredComplete: Boolean,
    val requiredPercentage: Float,
    val overallPercentage: Float,
    val completedFields: Int,
    val totalFields: Int,
    val missingRequired: List<String>
) {
    val isComplete: Boolean get() = requiredComplete
    val canSubmit: Boolean get() = requiredComplete
}

/**
 * DSL builder function for creating forms.
 */
fun form(id: String, metadata: FormMetadata = FormMetadata(), builder: FormBuilder.() -> Unit): FormDefinition {
    val formBuilder = FormBuilder(id, metadata)
    formBuilder.builder()
    return formBuilder.build()
}

/**
 * Form builder DSL implementation.
 */
class FormBuilder(
    private val id: String,
    private val metadata: FormMetadata
) {
    private val fields = mutableListOf<FieldDefinition>()

    fun textField(id: String, builder: TextFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = TextFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun emailField(id: String, builder: EmailFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = EmailFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun passwordField(id: String, builder: PasswordFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = PasswordFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun numberField(id: String, builder: NumberFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = NumberFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun dateField(id: String, builder: DateFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = DateFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun booleanField(id: String, builder: BooleanFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = BooleanFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun selectField(id: String, options: List<String>, builder: SelectFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = SelectFieldBuilder(id, options)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    fun textAreaField(id: String, builder: TextAreaFieldBuilder.() -> Unit = {}) {
        val fieldBuilder = TextAreaFieldBuilder(id)
        fieldBuilder.builder()
        fields.add(fieldBuilder.build())
    }

    internal fun build(): FormDefinition = FormDefinition(id, fields, metadata)
}

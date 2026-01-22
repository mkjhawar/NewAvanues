package com.augmentalis.avanues.avacode.forms

/**
 * Field definition with type, validation, and database mapping.
 *
 * @property id Field identifier (used for database column name)
 * @property type Field data type
 * @property label Human-readable label
 * @property placeholder Placeholder text
 * @property defaultValue Default value
 * @property validationRules List of validation rules
 * @property databaseConfig Database column configuration
 * @since 1.2.0
 */
data class FieldDefinition(
    val id: String,
    val type: FieldType,
    val label: String? = null,
    val placeholder: String? = null,
    val defaultValue: Any? = null,
    val validationRules: List<ValidationRule>,
    val databaseConfig: DatabaseColumnConfig
) {
    init {
        require(id.isNotBlank()) { "Field ID cannot be blank" }
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) {
            "Field ID must be lowercase alphanumeric with underscores"
        }
    }

    val isRequired: Boolean
        get() = validationRules.any { it is ValidationRule.Required }

    /**
     * Validate a value against all validation rules.
     */
    fun validate(value: Any?): List<String> {
        val errors = mutableListOf<String>()

        validationRules.forEach { rule ->
            val result = rule.validate(value)
            if (result is ValidationRuleResult.Invalid) {
                errors.add(result.errorMessage)
            }
        }

        return errors
    }
}

/**
 * Field data types with database mappings.
 */
enum class FieldType(val sqlType: String, val kotlinType: String) {
    TEXT("VARCHAR(255)", "String"),
    LONG_TEXT("TEXT", "String"),
    EMAIL("VARCHAR(255)", "String"),
    PASSWORD("VARCHAR(255)", "String"),
    INTEGER("INTEGER", "Int"),
    DECIMAL("DECIMAL(10,2)", "Float"),
    BOOLEAN("BOOLEAN", "Boolean"),
    DATE("DATE", "String"), // ISO 8601 format
    DATETIME("TIMESTAMP", "String"),
    SELECT("VARCHAR(100)", "String")
}

/**
 * Database column configuration.
 */
data class DatabaseColumnConfig(
    val nullable: Boolean = true,
    val unique: Boolean = false,
    val indexed: Boolean = false,
    val defaultValue: String? = null,
    val checkConstraint: String? = null,
    val maxLength: Int? = null
)

/**
 * Base field builder with common validation rules.
 */
abstract class BaseFieldBuilder<T : BaseFieldBuilder<T>>(
    protected val id: String,
    protected val type: FieldType
) {
    protected var label: String? = null
    protected var placeholder: String? = null
    protected var defaultValue: Any? = null
    protected val validationRules = mutableListOf<ValidationRule>()
    protected var nullable: Boolean = true
    protected var unique: Boolean = false
    protected var indexed: Boolean = false

    @Suppress("UNCHECKED_CAST")
    private fun self(): T = this as T

    fun label(value: String): T {
        label = value
        return self()
    }

    fun placeholder(value: String): T {
        placeholder = value
        return self()
    }

    fun defaultValue(value: Any): T {
        defaultValue = value
        return self()
    }

    fun required(): T {
        validationRules.add(ValidationRule.Required)
        nullable = false
        return self()
    }

    fun unique(): T {
        unique = true
        return self()
    }

    fun indexed(): T {
        indexed = true
        return self()
    }

    fun custom(validator: (Any?) -> Boolean, errorMessage: String): T {
        validationRules.add(ValidationRule.Custom(validator, errorMessage))
        return self()
    }

    abstract fun build(): FieldDefinition
}

/**
 * Text field builder.
 */
class TextFieldBuilder(id: String) : BaseFieldBuilder<TextFieldBuilder>(id, FieldType.TEXT) {
    private var minLength: Int? = null
    private var maxLength: Int? = null
    private var pattern: String? = null

    fun minLength(value: Int): TextFieldBuilder {
        require(value >= 0) { "Min length must be non-negative" }
        minLength = value
        validationRules.add(ValidationRule.MinLength(value))
        return this
    }

    fun maxLength(value: Int): TextFieldBuilder {
        require(value > 0) { "Max length must be positive" }
        maxLength = value
        validationRules.add(ValidationRule.MaxLength(value))
        return this
    }

    fun pattern(regex: String): TextFieldBuilder {
        pattern = regex
        validationRules.add(ValidationRule.Pattern(Regex(regex)))
        return this
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed,
            maxLength = maxLength
        )
    )
}

/**
 * Email field builder.
 */
class EmailFieldBuilder(id: String) : BaseFieldBuilder<EmailFieldBuilder>(id, FieldType.EMAIL) {
    init {
        // Email pattern validation
        validationRules.add(ValidationRule.Email)
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed
        )
    )
}

/**
 * Password field builder.
 */
class PasswordFieldBuilder(id: String) : BaseFieldBuilder<PasswordFieldBuilder>(id, FieldType.PASSWORD) {
    private var minLength: Int = 8

    fun minLength(value: Int): PasswordFieldBuilder {
        require(value >= 4) { "Password min length must be at least 4" }
        minLength = value
        validationRules.add(ValidationRule.MinLength(value))
        return this
    }

    fun requireUppercase(): PasswordFieldBuilder {
        validationRules.add(ValidationRule.RequireUppercase)
        return this
    }

    fun requireLowercase(): PasswordFieldBuilder {
        validationRules.add(ValidationRule.RequireLowercase)
        return this
    }

    fun requireNumber(): PasswordFieldBuilder {
        validationRules.add(ValidationRule.RequireNumber)
        return this
    }

    fun requireSpecialChar(): PasswordFieldBuilder {
        validationRules.add(ValidationRule.RequireSpecialChar)
        return this
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = false,
            indexed = false
        )
    )
}

/**
 * Number field builder.
 */
class NumberFieldBuilder(id: String) : BaseFieldBuilder<NumberFieldBuilder>(id, FieldType.INTEGER) {
    private var min: Number? = null
    private var max: Number? = null

    fun min(value: Number): NumberFieldBuilder {
        min = value
        validationRules.add(ValidationRule.Min(value))
        return this
    }

    fun max(value: Number): NumberFieldBuilder {
        max = value
        validationRules.add(ValidationRule.Max(value))
        return this
    }

    fun range(min: Number, max: Number): NumberFieldBuilder {
        this.min = min
        this.max = max
        validationRules.add(ValidationRule.Range(min, max))
        return this
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed,
            checkConstraint = buildCheckConstraint()
        )
    )

    private fun buildCheckConstraint(): String? {
        return when {
            min != null && max != null -> "$id >= $min AND $id <= $max"
            min != null -> "$id >= $min"
            max != null -> "$id <= $max"
            else -> null
        }
    }
}

/**
 * Date field builder.
 */
class DateFieldBuilder(id: String) : BaseFieldBuilder<DateFieldBuilder>(id, FieldType.DATE) {
    fun minDate(date: String): DateFieldBuilder {
        validationRules.add(ValidationRule.MinDate(date))
        return this
    }

    fun maxDate(date: String): DateFieldBuilder {
        validationRules.add(ValidationRule.MaxDate(date))
        return this
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed
        )
    )
}

/**
 * Boolean field builder.
 */
class BooleanFieldBuilder(id: String) : BaseFieldBuilder<BooleanFieldBuilder>(id, FieldType.BOOLEAN) {
    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue ?: false,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = false,
            unique = false,
            indexed = indexed
        )
    )
}

/**
 * Select field builder.
 */
class SelectFieldBuilder(id: String, private val options: List<String>) : BaseFieldBuilder<SelectFieldBuilder>(id, FieldType.SELECT) {
    init {
        require(options.isNotEmpty()) { "Select field must have at least one option" }
        validationRules.add(ValidationRule.InList(options))
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed
        )
    )
}

/**
 * Text area field builder.
 */
class TextAreaFieldBuilder(id: String) : BaseFieldBuilder<TextAreaFieldBuilder>(id, FieldType.LONG_TEXT) {
    private var minLength: Int? = null
    private var maxLength: Int? = null

    fun minLength(value: Int): TextAreaFieldBuilder {
        require(value >= 0) { "Min length must be non-negative" }
        minLength = value
        validationRules.add(ValidationRule.MinLength(value))
        return this
    }

    fun maxLength(value: Int): TextAreaFieldBuilder {
        require(value > 0) { "Max length must be positive" }
        maxLength = value
        validationRules.add(ValidationRule.MaxLength(value))
        return this
    }

    override fun build(): FieldDefinition = FieldDefinition(
        id = id,
        type = type,
        label = label,
        placeholder = placeholder,
        defaultValue = defaultValue,
        validationRules = validationRules,
        databaseConfig = DatabaseColumnConfig(
            nullable = nullable,
            unique = unique,
            indexed = indexed
        )
    )
}

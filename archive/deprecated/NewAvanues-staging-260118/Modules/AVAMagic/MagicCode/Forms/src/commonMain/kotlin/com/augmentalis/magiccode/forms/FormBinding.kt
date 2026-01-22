package com.augmentalis.avanues.avacode.forms

/**
 * Two-way data binding for forms with change tracking.
 *
 * Binds form definitions to data, tracks changes, and provides
 * automatic synchronization between UI and data layer.
 *
 * @property form Form definition
 * @property data Current form data (mutable)
 * @since 1.2.0
 */
class FormBinding(
    val form: FormDefinition,
    private val data: MutableMap<String, Any?>
) {
    private val initialData: Map<String, Any?> = data.toMap()
    private val changeListeners = mutableListOf<(String, Any?) -> Unit>()
    private val validationListeners = mutableListOf<(ValidationResult) -> Unit>()

    /**
     * Get field value.
     */
    operator fun get(fieldId: String): Any? = data[fieldId]

    /**
     * Set field value with validation and change tracking.
     */
    operator fun set(fieldId: String, value: Any?) {
        val field = form.fields.find { it.id == fieldId }
            ?: throw IllegalArgumentException("Field $fieldId not found in form")

        // Validate value
        val errors = field.validate(value)
        if (errors.isNotEmpty()) {
            throw ValidationException(fieldId, errors)
        }

        // Update data
        val oldValue = data[fieldId]
        data[fieldId] = value

        // Notify listeners
        if (oldValue != value) {
            changeListeners.forEach { it(fieldId, value) }
        }
    }

    /**
     * Get all data as read-only map.
     */
    fun getData(): Map<String, Any?> = data.toMap()

    /**
     * Update multiple fields at once.
     */
    fun setData(newData: Map<String, Any?>) {
        newData.forEach { (fieldId, value) ->
            this[fieldId] = value
        }
    }

    /**
     * Check if data has changed from initial state.
     */
    fun hasChanges(): Boolean = data != initialData

    /**
     * Get changed fields.
     */
    fun getChanges(): Map<String, Change> {
        return data.mapNotNull { (fieldId, newValue) ->
            val oldValue = initialData[fieldId]
            if (oldValue != newValue) {
                fieldId to Change(oldValue, newValue)
            } else null
        }.toMap()
    }

    /**
     * Validate all fields.
     */
    fun validate(): ValidationResult {
        val result = form.validate(data)
        validationListeners.forEach { it(result) }
        return result
    }

    /**
     * Get completion status.
     */
    fun getCompletion(): CompletionStatus = form.calculateCompletion(data)

    /**
     * Reset to initial data.
     */
    fun reset() {
        data.clear()
        data.putAll(initialData)
        changeListeners.forEach { listener ->
            initialData.keys.forEach { fieldId ->
                listener(fieldId, initialData[fieldId])
            }
        }
    }

    /**
     * Clear all data.
     */
    fun clear() {
        data.clear()
        changeListeners.forEach { listener ->
            form.fields.forEach { field ->
                listener(field.id, null)
            }
        }
    }

    /**
     * Register change listener.
     */
    fun onChange(listener: (fieldId: String, value: Any?) -> Unit) {
        changeListeners.add(listener)
    }

    /**
     * Register validation listener.
     */
    fun onValidation(listener: (ValidationResult) -> Unit) {
        validationListeners.add(listener)
    }

    /**
     * Save current data as new initial state.
     */
    fun commit() {
        (initialData as? MutableMap)?.clear()
        (initialData as? MutableMap)?.putAll(data)
    }
}

/**
 * Represents a change in field value.
 */
data class Change(
    val oldValue: Any?,
    val newValue: Any?
)

/**
 * Exception thrown when validation fails during binding.
 */
class ValidationException(
    val fieldId: String,
    val errors: List<String>
) : Exception("Validation failed for field $fieldId: ${errors.joinToString()}")

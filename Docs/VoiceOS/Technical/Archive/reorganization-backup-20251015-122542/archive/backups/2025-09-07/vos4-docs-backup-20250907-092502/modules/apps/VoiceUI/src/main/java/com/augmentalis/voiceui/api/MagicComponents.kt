package com.augmentalis.voiceui.api

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.core.*
import com.augmentalis.voiceui.dsl.MagicScope
import com.augmentalis.voiceui.dsl.VoiceCommandRegistry
import kotlinx.coroutines.launch

/**
 * MagicComponents - Ultra-simple one-line UI components
 * 
 * These components provide maximum magic:
 * - Automatic state management
 * - Built-in validation
 * - Smart defaults
 * - Voice commands
 * - Localization
 * - GPU acceleration
 */

/**
 * Email input with complete magic
 * Zero configuration required - everything automatic
 */
@Composable
fun MagicScope.email(
    label: String = "Email",
    required: Boolean = true,
    onValue: ((String) -> Unit)? = null
): String {
    
    // Automatic state with validation
    val state = MagicEngine.autoState(
        key = "email_${currentScreenId}",
        default = "",
        validator = { it.contains("@") && it.contains(".") },
        persistence = StatePersistence.SESSION
    )
    
    // Smart UI with all features
    OutlinedTextField(
        value = state.value,
        onValueChange = { newValue ->
            state.setValue(newValue)
            onValue?.invoke(newValue)
            
            // Register voice command automatically
            registerVoiceCommand("enter email $newValue", "email_$currentScreenId") {
                // Focus action
            }
        },
        label = { 
            Text(
                text = if (required) "$label *" else label,
                color = if (state.error != null) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        },
        isError = state.error != null,
        supportingText = state.error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            autoCorrect = false
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email"
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    )
    
    // Auto-register for voice commands
    LaunchedEffect(Unit) {
        VoiceCommandRegistry.register(
            listOf("email", "email address", "enter email", "type email"),
            onTrigger = { focusOnField("email_${currentScreenId}", currentScreenId) }
        )
    }
    
    return state.value
}

/**
 * Password input with automatic security features
 */
@Composable
fun MagicScope.password(
    label: String = "Password",
    minLength: Int = 8,
    showStrength: Boolean = true,
    onValue: ((String) -> Unit)? = null
): String {
    
    // Automatic secure state
    val state = MagicEngine.autoState(
        key = "password_${currentScreenId}",
        default = "",
        validator = { it.length >= minLength },
        persistence = StatePersistence.MEMORY // Never persist passwords
    )
    
    // Password visibility state
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Password strength calculation
    val strength = calculatePasswordStrength(state.value)
    
    Column {
        OutlinedTextField(
            value = state.value,
            onValueChange = { newValue ->
                state.setValue(newValue)
                onValue?.invoke(newValue)
            },
            label = { 
                Text("$label (min $minLength characters)")
            },
            isError = state.error != null,
            supportingText = {
                if (state.error != null) {
                    Text(state.error)
                } else if (showStrength && state.value.isNotEmpty()) {
                    PasswordStrengthIndicator(strength)
                }
            },
            visualTransformation = if (passwordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrect = false
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password"
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) 
                            Icons.Default.Clear 
                        else 
                            Icons.Default.Lock,
                        contentDescription = if (passwordVisible) 
                            "Hide password" 
                        else 
                            "Show password"
                    )
                }
            }
        )
    }
    
    return state.value
}

/**
 * Phone input with automatic formatting
 */
@Composable
fun MagicScope.phone(
    label: String = "Phone Number",
    countryCode: String = detectCountryCode(),
    onValue: ((String) -> Unit)? = null
): String {
    
    val state = MagicEngine.autoState(
        key = "phone_${currentScreenId}",
        default = countryCode,
        validator = { validatePhoneNumber(it) },
        persistence = StatePersistence.SESSION
    )
    
    OutlinedTextField(
        value = formatPhoneNumber(state.value),
        onValueChange = { newValue ->
            val cleaned = newValue.replace(Regex("[^0-9+]"), "")
            state.setValue(cleaned)
            onValue?.invoke(cleaned)
        },
        label = { Text(label) },
        isError = state.error != null,
        supportingText = state.error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "Phone"
            )
        }
    )
    
    return state.value
}

/**
 * Name input with smart capitalization
 */
@Composable
fun MagicScope.name(
    label: String = "Full Name",
    splitFirstLast: Boolean = true,
    onValue: ((String) -> Unit)? = null
): Pair<String, String> {
    
    if (splitFirstLast) {
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { 
                    firstName = it.capitalize()
                    onValue?.invoke("$firstName $lastName")
                },
                label = { Text("First Name") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = lastName,
                onValueChange = { 
                    lastName = it.capitalize()
                    onValue?.invoke("$firstName $lastName")
                },
                label = { Text("Last Name") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
        }
        
        return Pair(firstName, lastName)
    } else {
        val state = MagicEngine.autoState(
            key = "name_${currentScreenId}",
            default = "",
            persistence = StatePersistence.SESSION
        )
        
        OutlinedTextField(
            value = state.value,
            onValueChange = { newValue ->
                state.setValue(newValue.capitalize())
                onValue?.invoke(newValue)
            },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        
        val parts = state.value.split(" ", limit = 2)
        return Pair(
            parts.getOrNull(0) ?: "",
            parts.getOrNull(1) ?: ""
        )
    }
}

/**
 * Submit button with automatic validation and loading state
 */
@Composable
fun MagicScope.submit(
    text: String = getSmartButtonText(),
    validateAll: Boolean = true,
    onClick: suspend () -> Unit
): Boolean {
    
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Auto-validate all fields if requested
    val isValid = if (validateAll) {
        validateAllFields()
    } else true
    
    Button(
        onClick = {
            if (isValid && !isLoading) {
                scope.launch {
                    isLoading = true
                    try {
                        onClick()
                        isSuccess = true
                    } catch (e: Exception) {
                        // Handle error
                        showError(e.message ?: "An error occurred")
                    } finally {
                        isLoading = false
                    }
                }
            }
        },
        enabled = isValid && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSuccess) 
                Color.Green 
            else 
                MaterialTheme.colorScheme.primary
        )
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            isSuccess -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    
    // Voice command registration
    LaunchedEffect(Unit) {
        VoiceCommandRegistry.register(
            listOf("submit", "continue", text.lowercase()),
            onTrigger = { 
                if (isValid && !isLoading) {
                    scope.launch { onClick() }
                }
            }
        )
    }
    
    return isSuccess
}

/**
 * Complete form with automatic field management
 */
@Composable
fun MagicScope.form(
    fields: List<FormField> = detectFormFields(),
    onSubmit: suspend (FormData) -> Unit
) {
    val formData = remember { mutableStateMapOf<String, Any>() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fields.forEach { field ->
            when (field.type) {
                FieldType.EMAIL -> {
                    val value = email(
                        label = field.label,
                        required = field.required
                    )
                    formData[field.key] = value
                }
                FieldType.PASSWORD -> {
                    val value = password(
                        label = field.label
                    )
                    formData[field.key] = value
                }
                FieldType.PHONE -> {
                    val value = phone(
                        label = field.label
                    )
                    formData[field.key] = value
                }
                FieldType.NAME -> {
                    val (first, last) = name(
                        label = field.label
                    )
                    formData["${field.key}_first"] = first
                    formData["${field.key}_last"] = last
                }
                FieldType.TEXT -> {
                    // Generic text field
                    val value = input(
                        label = field.label,
                        required = field.required
                    )
                    formData[field.key] = value
                }
                FieldType.DATE -> {
                    // Date picker placeholder
                    val value = input(
                        label = field.label,
                        required = field.required
                    )
                    formData[field.key] = value
                }
                FieldType.ADDRESS -> {
                    // Address field placeholder
                    val value = input(
                        label = field.label,
                        required = field.required
                    )
                    formData[field.key] = value
                }
                FieldType.CARD -> {
                    // Card field placeholder
                    val value = input(
                        label = field.label,
                        required = field.required
                    )
                    formData[field.key] = value
                }
            }
        }
        
        submit(
            text = "Submit",
            validateAll = true
        ) {
            onSubmit(FormData(formData.toMap()))
        }
    }
}

// Helper functions

private fun calculatePasswordStrength(password: String): PasswordStrength {
    var strength = 0
    if (password.length >= 8) strength++
    if (password.length >= 12) strength++
    if (password.any { it.isUpperCase() }) strength++
    if (password.any { it.isLowerCase() }) strength++
    if (password.any { it.isDigit() }) strength++
    if (password.any { !it.isLetterOrDigit() }) strength++
    
    return when (strength) {
        0, 1 -> PasswordStrength.WEAK
        2, 3 -> PasswordStrength.MEDIUM
        4, 5 -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Strength: ${strength.name}",
            color = when (strength) {
                PasswordStrength.WEAK -> Color.Red
                PasswordStrength.MEDIUM -> Color.Yellow
                PasswordStrength.STRONG -> Color.Green
                PasswordStrength.VERY_STRONG -> Color.Green
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun validatePhoneNumber(phone: String): Boolean {
    val digits = phone.replace(Regex("[^0-9]"), "")
    return digits.length >= 10
}

private fun formatPhoneNumber(phone: String): String {
    val cleaned = phone.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length <= 3 -> cleaned
        cleaned.length <= 6 -> "(${cleaned.substring(0, 3)}) ${cleaned.substring(3)}"
        cleaned.length <= 10 -> "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
        else -> "+${cleaned.substring(0, cleaned.length - 10)} (${cleaned.substring(cleaned.length - 10, cleaned.length - 7)}) ${cleaned.substring(cleaned.length - 7, cleaned.length - 4)}-${cleaned.substring(cleaned.length - 4)}"
    }
}

private fun detectCountryCode(): String {
    // Detect based on device locale
    val locale = java.util.Locale.getDefault()
    return when (locale.country) {
        "US", "CA" -> "+1"
        "GB" -> "+44"
        "FR" -> "+33"
        "DE" -> "+49"
        "JP" -> "+81"
        "CN" -> "+86"
        "IN" -> "+91"
        else -> "+"
    }
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}

// Support classes

enum class PasswordStrength {
    WEAK, MEDIUM, STRONG, VERY_STRONG
}

data class FormField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = true,
    val validation: ((Any) -> Boolean)? = null
)

enum class FieldType {
    EMAIL, PASSWORD, PHONE, NAME, TEXT, DATE, ADDRESS, CARD
}

data class FormData(
    val fields: Map<String, Any>
) {
    inline fun <reified T> get(key: String): T? = fields[key] as? T
    fun getString(key: String): String? = fields[key]?.toString()
}

// Import these where needed
// Helper object for icons - removed to avoid conflicts
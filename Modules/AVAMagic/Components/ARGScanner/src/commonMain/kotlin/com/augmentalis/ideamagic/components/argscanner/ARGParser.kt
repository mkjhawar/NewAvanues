package com.augmentalis.avanues.avamagic.components.argscanner

import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/**
 * ARG File Parser
 *
 * Parses Avanue Registry (ARG) files from JSON format.
 *
 * ## Usage
 * ```kotlin
 * val parser = ARGParser()
 * val argFile = parser.parse(jsonString)
 * println("Found ${argFile.capabilities.size} capabilities")
 * ```
 *
 * @since 1.0.0
 */
class ARGParser {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * Parse ARG file from JSON string
     *
     * @param jsonString JSON content of ARG file
     * @return Parsed ARGFile object
     * @throws ARGParseException if parsing fails
     */
    fun parse(jsonString: String): ARGFile {
        return try {
            json.decodeFromString<ARGFile>(jsonString)
        } catch (e: SerializationException) {
            throw ARGParseException("Failed to parse ARG file: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ARGParseException("Invalid ARG file format: ${e.message}", e)
        }
    }

    /**
     * Validate ARG file for correctness
     *
     * @param argFile ARG file to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(argFile: ARGFile): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // Validate version
        if (!argFile.version.matches(Regex("\\d+\\.\\d+"))) {
            errors.add(ValidationError.InvalidVersion(argFile.version))
        }

        // Validate app info
        if (argFile.app.id.isBlank()) {
            errors.add(ValidationError.MissingField("app.id"))
        }
        if (argFile.app.name.isBlank()) {
            errors.add(ValidationError.MissingField("app.name"))
        }
        if (argFile.app.packageName.isBlank()) {
            errors.add(ValidationError.MissingField("app.packageName"))
        }
        if (!argFile.app.version.matches(Regex("\\d+\\.\\d+\\.\\d+"))) {
            errors.add(ValidationError.InvalidVersion(argFile.app.version))
        }

        // Validate capabilities
        argFile.capabilities.forEachIndexed { index, capability ->
            if (capability.id.isBlank()) {
                errors.add(ValidationError.MissingField("capabilities[$index].id"))
            }
            if (capability.name.isBlank()) {
                errors.add(ValidationError.MissingField("capabilities[$index].name"))
            }

            // Validate voice commands have parameter placeholders
            capability.voiceCommands.forEach { command ->
                val placeholders = extractPlaceholders(command)
                placeholders.forEach { placeholder ->
                    if (capability.params.none { it.name == placeholder }) {
                        errors.add(ValidationError.MissingParameter(capability.id, placeholder))
                    }
                }
            }
        }

        // Validate services
        argFile.services.forEachIndexed { index, service ->
            if (service.id.isBlank()) {
                errors.add(ValidationError.MissingField("services[$index].id"))
            }
            if (service.aidlInterface.isBlank()) {
                errors.add(ValidationError.MissingField("services[$index].aidlInterface"))
            }
            if (!service.aidlInterface.startsWith("com.")) {
                errors.add(ValidationError.InvalidFormat("services[$index].aidlInterface", "Must be fully qualified interface name"))
            }
        }

        // Validate content providers
        argFile.contentProviders.forEachIndexed { index, provider ->
            if (provider.id.isBlank()) {
                errors.add(ValidationError.MissingField("contentProviders[$index].id"))
            }
            if (provider.authority.isBlank()) {
                errors.add(ValidationError.MissingField("contentProviders[$index].authority"))
            }
        }

        return errors
    }

    /**
     * Extract parameter placeholders from voice command string
     *
     * E.g., "open {url}" -> ["url"]
     */
    private fun extractPlaceholders(command: String): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(command).map { it.groupValues[1] }.toList()
    }

    /**
     * Convert ARG file back to JSON string
     */
    fun toJson(argFile: ARGFile): String {
        return json.encodeToString(ARGFile.serializer(), argFile)
    }
}

/**
 * ARG parsing exception
 */
class ARGParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * ARG validation errors
 */
sealed class ValidationError {
    abstract val message: String

    data class InvalidVersion(val version: String) : ValidationError() {
        override val message = "Invalid version format: $version"
    }

    data class MissingField(val field: String) : ValidationError() {
        override val message = "Missing required field: $field"
    }

    data class InvalidFormat(val field: String, val reason: String) : ValidationError() {
        override val message = "Invalid format for $field: $reason"
    }

    data class MissingParameter(val capabilityId: String, val paramName: String) : ValidationError() {
        override val message = "Capability $capabilityId references undefined parameter: $paramName"
    }

    data class DuplicateId(val id: String) : ValidationError() {
        override val message = "Duplicate ID found: $id"
    }
}

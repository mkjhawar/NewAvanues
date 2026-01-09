/**
 * API Key Manager for Cloud LLM Providers
 *
 * Securely stores and retrieves API keys for various LLM providers using:
 * - EncryptedSharedPreferences (AES-256) for on-device storage
 * - Environment variables for development/testing
 *
 * Security:
 * - Never logs API keys
 * - Uses Android Keystore for encryption
 * - Validates key formats before storage
 * - Supports key rotation
 *
 * Supported Providers:
 * - Anthropic (Claude API)
 * - OpenAI (GPT-4)
 * - OpenRouter (aggregator)
 * - HuggingFace (Inference API)
 * - Google AI (Gemini)
 *
 * Created: 2025-11-03
 * Author: AVA AI Team
 */

package com.augmentalis.llm.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.augmentalis.ava.core.common.Result
import com.augmentalis.llm.domain.ProviderType
import timber.log.Timber

/**
 * API Key Manager
 *
 * Usage:
 * ```
 * val apiKeyManager = ApiKeyManager(context)
 *
 * // Save API key
 * apiKeyManager.saveApiKey(ProviderType.ANTHROPIC, "sk-ant-...")
 *
 * // Retrieve API key
 * val keyResult = apiKeyManager.getApiKey(ProviderType.ANTHROPIC)
 * when (keyResult) {
 *     is Result.Success -> use(keyResult.data)
 *     is Result.Error -> handleError(keyResult.message)
 * }
 * ```
 */
class ApiKeyManager(context: Context) {

    companion object {
        private const val TAG = "ApiKeyManager"
        private const val PREFS_FILE_NAME = "ava_llm_api_keys"

        // Environment variable prefixes for Claude Code development
        private const val ENV_PREFIX = "AVA_"

        // API key prefixes for validation
        private val KEY_PREFIXES = mapOf(
            ProviderType.ANTHROPIC to listOf("sk-ant-"),
            ProviderType.OPENAI to listOf("sk-", "sk-proj-"),
            ProviderType.OPENROUTER to listOf("sk-or-"),
            ProviderType.HUGGINGFACE to listOf("hf_"),
            ProviderType.GOOGLE_AI to listOf("AIza"),
            ProviderType.COHERE to listOf("co-"),
            ProviderType.TOGETHER_AI to listOf("t-")
        )
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Get API key for a provider
     *
     * Priority:
     * 1. Environment variable (for development/testing)
     * 2. Encrypted SharedPreferences (for production)
     *
     * @param provider Provider type
     * @return Result.Success with API key, or Result.Error if not found
     */
    suspend fun getApiKey(provider: ProviderType): Result<String> {
        return try {
            // Ignore LOCAL provider (doesn't need API keys)
            if (provider == ProviderType.LOCAL) {
                return Result.Error(
                    exception = IllegalArgumentException("LOCAL provider does not require API keys"),
                    message = "LOCAL provider does not require API keys"
                )
            }

            // 1. Check environment variable first (for development)
            val envVarName = "${ENV_PREFIX}${provider.name}_API_KEY"
            val envKey = System.getenv(envVarName)
            if (envKey != null && envKey.isNotBlank()) {
                Timber.d("API key found in environment variable: $envVarName")
                return Result.Success(envKey)
            }

            // 2. Check encrypted SharedPreferences
            val prefKey = getPreferenceKey(provider)
            val storedKey = encryptedPrefs.getString(prefKey, null)
            if (storedKey != null && storedKey.isNotBlank()) {
                Timber.d("API key found in encrypted prefs for provider: ${provider.name}")
                return Result.Success(storedKey)
            }

            // Not found
            Timber.w("No API key found for provider: ${provider.name}")
            Result.Error(
                exception = IllegalStateException("No API key found for ${provider.name}"),
                message = "No API key found for ${provider.name}. Please configure in settings."
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve API key for ${provider.name}")
            Result.Error(
                exception = e,
                message = "Failed to retrieve API key: ${e.message}"
            )
        }
    }

    /**
     * Save API key to encrypted storage
     *
     * Validates key format before saving.
     * NEVER logs the actual API key (security).
     *
     * @param provider Provider type
     * @param apiKey API key to save
     * @return Result.Success if saved, Result.Error if validation fails
     */
    suspend fun saveApiKey(provider: ProviderType, apiKey: String): Result<Unit> {
        return try {
            // Validate provider
            if (provider == ProviderType.LOCAL) {
                return Result.Error(
                    exception = IllegalArgumentException("Cannot save API key for LOCAL provider"),
                    message = "Cannot save API key for LOCAL provider"
                )
            }

            // Trim whitespace
            val trimmedKey = apiKey.trim()

            // Validate key format
            if (!validateKeyFormat(provider, trimmedKey)) {
                Timber.w("Invalid API key format for ${provider.name}")
                return Result.Error(
                    exception = IllegalArgumentException("Invalid API key format for ${provider.name}"),
                    message = "Invalid API key format for ${provider.name}. " +
                            "Expected format: ${KEY_PREFIXES[provider]?.joinToString(" or ")}"
                )
            }

            // Save to encrypted prefs
            val prefKey = getPreferenceKey(provider)
            encryptedPrefs.edit().putString(prefKey, trimmedKey).apply()

            // NEVER log the actual key (security)
            Timber.i("API key saved successfully for ${provider.name} (masked: ${maskKey(trimmedKey)})")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to save API key for ${provider.name}")
            Result.Error(
                exception = e,
                message = "Failed to save API key: ${e.message}"
            )
        }
    }

    /**
     * Delete API key from encrypted storage
     *
     * @param provider Provider type
     * @return Result.Success if deleted, Result.Error on failure
     */
    suspend fun deleteApiKey(provider: ProviderType): Result<Unit> {
        return try {
            val prefKey = getPreferenceKey(provider)
            encryptedPrefs.edit().remove(prefKey).apply()

            Timber.i("API key deleted for ${provider.name}")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete API key for ${provider.name}")
            Result.Error(
                exception = e,
                message = "Failed to delete API key: ${e.message}"
            )
        }
    }

    /**
     * Check if API key exists for a provider
     *
     * Checks both environment variables and encrypted prefs.
     *
     * @param provider Provider type
     * @return true if key exists, false otherwise
     */
    fun hasApiKey(provider: ProviderType): Boolean {
        if (provider == ProviderType.LOCAL) return false

        // Check environment variable
        val envVarName = "${ENV_PREFIX}${provider.name}_API_KEY"
        val envKey = System.getenv(envVarName)
        if (envKey != null && envKey.isNotBlank()) return true

        // Check encrypted prefs
        val prefKey = getPreferenceKey(provider)
        val storedKey = encryptedPrefs.getString(prefKey, null)
        return storedKey != null && storedKey.isNotBlank()
    }

    /**
     * Validate API key format
     *
     * Performs basic prefix validation to catch obvious mistakes.
     * Does NOT verify the key is valid with the provider (use provider's API for that).
     *
     * @param provider Provider type
     * @param apiKey API key to validate
     * @return true if format is valid, false otherwise
     */
    fun validateKeyFormat(provider: ProviderType, apiKey: String): Boolean {
        val prefixes = KEY_PREFIXES[provider] ?: return false

        // Check if key starts with any of the valid prefixes
        val isValid = prefixes.any { apiKey.startsWith(it) }

        // Check minimum length (most API keys are at least 20 characters)
        val hasMinLength = apiKey.length >= 20

        return isValid && hasMinLength
    }

    /**
     * Get preference key for a provider
     *
     * @param provider Provider type
     * @return SharedPreferences key
     */
    private fun getPreferenceKey(provider: ProviderType): String {
        return "api_key_${provider.name.lowercase()}"
    }

    /**
     * Mask API key for logging (security)
     *
     * Shows first 4 and last 4 characters, masks the rest.
     * Example: "sk-ant-abc...xyz123"
     *
     * @param apiKey API key to mask
     * @return Masked API key
     */
    private fun maskKey(apiKey: String): String {
        if (apiKey.length <= 8) return "***"
        val prefix = apiKey.take(7)
        val suffix = apiKey.takeLast(4)
        return "$prefix...$suffix"
    }

    /**
     * Get all configured providers
     *
     * Returns list of providers that have API keys configured.
     * Useful for UI to show which providers are available.
     *
     * @return List of configured providers
     */
    fun getConfiguredProviders(): List<ProviderType> {
        return ProviderType.values()
            .filter { it != ProviderType.LOCAL } // Skip LOCAL
            .filter { hasApiKey(it) }
    }
}

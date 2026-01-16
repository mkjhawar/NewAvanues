package com.augmentalis.license

import com.augmentalis.ava.platform.DeviceFingerprint
import com.augmentalis.ava.platform.DeviceInfo
import com.augmentalis.ava.platform.DeviceInfoFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * LicenseSDK - Universal License-as-a-Service Client
 *
 * A Kotlin Multiplatform SDK for integrating with the AvaCloud LaaS API.
 * Works on Android, iOS, Desktop (Windows/macOS/Linux), and Web (JavaScript).
 *
 * Usage:
 * ```kotlin
 * val client = LicenseClient.create("your-api-key")
 *
 * // Validate a license (auto-generates device fingerprint)
 * val result = client.validateLicense("LICENSE-KEY-HERE")
 *
 * // Activate a license on this device
 * val activation = client.activateLicense("LICENSE-KEY-HERE")
 * ```
 */
class LicenseClient private constructor(
    private val config: LicenseConfig,
    private val httpClient: HttpClient,
    private val deviceInfo: DeviceInfo
) {
    private var cachedFingerprint: DeviceFingerprint? = null

    /**
     * Get the device fingerprint (cached after first call).
     */
    fun getDeviceFingerprint(): DeviceFingerprint {
        cachedFingerprint?.let { return it }
        val fp = deviceInfo.getFingerprint()
        cachedFingerprint = fp
        return fp
    }

    /**
     * Validate a license key for this device.
     *
     * @param licenseKey The license key to validate
     * @param metadata Optional additional metadata
     * @return Validation result with license status and features
     */
    suspend fun validateLicense(
        licenseKey: String,
        metadata: Map<String, String>? = null
    ): ValidateLicenseResponse = withContext(Dispatchers.Default) {
        val fingerprint = getDeviceFingerprint()

        val request = ValidateLicenseRequest(
            licenseKey = licenseKey,
            deviceFingerprint = fingerprint.fingerprint,
            platform = fingerprint.platform.name,
            metadata = metadata
        )

        val response = httpClient.post("${config.baseUrl}/api/v1/laas/validate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        handleResponse(response)
    }

    /**
     * Activate a license on this device.
     *
     * @param licenseKey The license key to activate
     * @param deviceName Optional friendly device name
     * @param metadata Optional additional metadata
     * @return Activation result with grace period info
     */
    suspend fun activateLicense(
        licenseKey: String,
        deviceName: String? = null,
        metadata: Map<String, String>? = null
    ): ActivateLicenseResponse = withContext(Dispatchers.Default) {
        val fingerprint = getDeviceFingerprint()

        val request = ActivateLicenseRequest(
            licenseKey = licenseKey,
            deviceFingerprint = fingerprint.fingerprint,
            platform = fingerprint.platform.name,
            deviceName = deviceName ?: "${fingerprint.platform.name} Device",
            metadata = metadata
        )

        val response = httpClient.post("${config.baseUrl}/api/v1/laas/activate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        handleResponse(response)
    }

    /**
     * Deactivate a license from this device.
     *
     * @param licenseKey The license key to deactivate
     * @return Deactivation result
     */
    suspend fun deactivateLicense(licenseKey: String): DeactivateLicenseResponse =
        withContext(Dispatchers.Default) {
            val fingerprint = getDeviceFingerprint()

            val request = DeactivateLicenseRequest(
                licenseKey = licenseKey,
                deviceFingerprint = fingerprint.fingerprint
            )

            val response = httpClient.post("${config.baseUrl}/api/v1/laas/deactivate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            handleResponse(response)
        }

    /**
     * Get license information.
     *
     * @param licenseKey The license key to query
     * @return License details
     */
    suspend fun getLicenseInfo(licenseKey: String): LicenseInfoResponse =
        withContext(Dispatchers.Default) {
            val response = httpClient.get("${config.baseUrl}/api/v1/laas/licenses/$licenseKey")
            handleResponse(response)
        }

    /**
     * Check if a license is valid (convenience method).
     *
     * @param licenseKey The license key to check
     * @return true if valid, false otherwise
     */
    suspend fun isLicenseValid(licenseKey: String): Boolean {
        return try {
            val result = validateLicense(licenseKey)
            result.valid
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if license is valid using offline grace period.
     *
     * @param gracePeriod Grace period info from last validation
     * @return true if still within grace period
     */
    fun isWithinGracePeriod(gracePeriod: GracePeriodInfo): Boolean {
        val now = System.currentTimeMillis()
        return now < gracePeriod.offlineGraceExpiresAtMillis
    }

    /**
     * Check if validation is due based on grace period.
     *
     * @param gracePeriod Grace period info from last validation
     * @return true if validation should be performed
     */
    fun isValidationDue(gracePeriod: GracePeriodInfo): Boolean {
        val now = System.currentTimeMillis()
        return now >= gracePeriod.nextValidationDueMillis
    }

    /**
     * Close the client and release resources.
     */
    fun close() {
        httpClient.close()
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.body<LicenseErrorResponse>()
            } catch (e: Exception) {
                LicenseErrorResponse(
                    code = "HTTP_ERROR",
                    message = "HTTP ${response.status.value}: ${response.status.description}"
                )
            }
            throw LicenseApiException(
                message = errorBody.message,
                code = errorBody.code,
                statusCode = response.status.value,
                details = errorBody.details
            )
        }
        return response.body()
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        /**
         * Create a License client with the given API key.
         *
         * @param apiKey Your License API key
         * @param baseUrl Optional custom base URL (default: https://api.avacloud.com)
         * @param timeoutMs Request timeout in milliseconds (default: 30000)
         */
        fun create(
            apiKey: String,
            baseUrl: String = "https://api.avacloud.com",
            timeoutMs: Long = 30000
        ): LicenseClient {
            require(apiKey.isNotBlank()) { "API key is required" }

            val config = LicenseConfig(
                apiKey = apiKey,
                baseUrl = baseUrl.trimEnd('/'),
                timeoutMs = timeoutMs
            )

            val httpClient = HttpClient {
                install(ContentNegotiation) {
                    json(json)
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = timeoutMs
                    connectTimeoutMillis = timeoutMs / 2
                }

                defaultRequest {
                    header("X-API-Key", apiKey)
                    header("Content-Type", "application/json")
                }
            }

            val deviceInfo = DeviceInfoFactory.create()

            return LicenseClient(config, httpClient, deviceInfo)
        }
    }
}

/**
 * Configuration for the License client.
 */
data class LicenseConfig(
    val apiKey: String,
    val baseUrl: String,
    val timeoutMs: Long
)

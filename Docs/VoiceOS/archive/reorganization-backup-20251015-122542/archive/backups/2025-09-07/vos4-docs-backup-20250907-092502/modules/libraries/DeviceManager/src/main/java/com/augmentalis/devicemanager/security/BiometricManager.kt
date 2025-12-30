// Author: Manoj Jhawar
// Purpose: Comprehensive biometric authentication management for fingerprint, face, iris, voice, and other biometric methods

package com.augmentalis.devicemanager.security

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager as AndroidBiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
// FaceManager is not available in Android SDK, using BiometricManager instead
// import android.hardware.face.FaceManager
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.KeyStore
import java.security.Signature
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Comprehensive Biometric Manager
 * Supports fingerprint, face, iris, voice, and multi-modal biometric authentication
 * Handles both legacy and modern biometric APIs with cryptographic operations
 */
class BiometricManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    companion object {
        private const val TAG = "BiometricManager"
        
        // Biometric types
        const val TYPE_FINGERPRINT = "fingerprint"
        const val TYPE_FACE = "face"
        const val TYPE_IRIS = "iris"
        const val TYPE_VOICE = "voice"
        const val TYPE_VEIN = "vein"
        const val TYPE_PALM = "palm"
        const val TYPE_GAIT = "gait"
        const val TYPE_HEARTBEAT = "heartbeat"
        const val TYPE_BEHAVIORAL = "behavioral"
        
        // Authentication levels
        const val LEVEL_CONVENIENCE = 0  // Low security (e.g., Class 2 biometric)
        const val LEVEL_WEAK = 1         // Weak biometric
        const val LEVEL_STRONG = 2       // Strong biometric (Class 3)
        const val LEVEL_DEVICE_CREDENTIAL = 3 // PIN/Pattern/Password
        
        // Keystore
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "BiometricKey_"
        private const val SIGNATURE_KEY_ALIAS = "BiometricSignatureKey"
        
        // Cipher transformation
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        
        // Error codes
        const val ERROR_HW_UNAVAILABLE = 1
        const val ERROR_UNABLE_TO_PROCESS = 2
        const val ERROR_TIMEOUT = 3
        const val ERROR_NO_SPACE = 4
        const val ERROR_CANCELED = 5
        const val ERROR_LOCKOUT = 7
        const val ERROR_LOCKOUT_PERMANENT = 9
        const val ERROR_USER_CANCELED = 10
        const val ERROR_NO_BIOMETRICS = 11
        const val ERROR_HW_NOT_PRESENT = 12
        const val ERROR_NEGATIVE_BUTTON = 13
        const val ERROR_NO_DEVICE_CREDENTIAL = 14
    }
    
    // System services
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val packageManager = context.packageManager
    
    // Biometric services
    private var biometricManager: AndroidBiometricManager? = null
    private var fingerprintManager: FingerprintManager? = null
    // FaceManager API is not publicly available
    // private var faceManager: FaceManager? = null
    
    // Keystore
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    
    // State flows
    private val _biometricState = MutableStateFlow(BiometricState())
    val biometricState: StateFlow<BiometricState> = _biometricState.asStateFlow()
    
    private val _authenticationResult = MutableSharedFlow<AuthenticationResult>()
    val authenticationResult: SharedFlow<AuthenticationResult> = _authenticationResult.asSharedFlow()
    
    private val _enrollmentStatus = MutableStateFlow<EnrollmentStatus?>(null)
    val enrollmentStatus: StateFlow<EnrollmentStatus?> = _enrollmentStatus.asStateFlow()
    
    // Active authentication
    private var currentAuthenticationSession: AuthenticationSession? = null
    @Volatile private var cancellationSignal: CancellationSignal? = null
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // ========== DATA MODELS ==========
    
    data class BiometricState(
        val isHardwareAvailable: Boolean = false,
        val isEnrolled: Boolean = false,
        val availableTypes: List<BiometricType> = emptyList(),
        val capabilities: BiometricCapabilities? = null,
        val securityLevel: SecurityLevel = SecurityLevel.NONE,
        val isAuthenticating: Boolean = false,
        val lastAuthTime: Long? = null,
        val failedAttempts: Int = 0,
        val isLockedOut: Boolean = false,
        val lockoutEndTime: Long? = null
    )
    
    data class BiometricType(
        val type: String,
        val isAvailable: Boolean,
        val isEnrolled: Boolean,
        val securityLevel: SecurityLevel,
        val maxAttempts: Int,
        val features: BiometricFeatures
    )
    
    data class BiometricFeatures(
        val supportsCrypto: Boolean,
        val supportsLiveness: Boolean,
        val supportsAntiSpoofing: Boolean,
        val supportsMultiUser: Boolean,
        val supportsTemplateUpdate: Boolean,
        val accuracy: Float, // FAR (False Acceptance Rate)
        val speed: AuthenticationSpeed
    )
    
    enum class AuthenticationSpeed {
        INSTANT,    // < 100ms
        FAST,       // < 500ms
        NORMAL,     // < 1s
        SLOW        // > 1s
    }
    
    enum class SecurityLevel {
        NONE,
        CONVENIENCE,
        WEAK,
        STRONG,
        MAXIMUM
    }
    
    data class BiometricCapabilities(
        val maxFingerprints: Int,
        val maxFaces: Int,
        val supportsCryptoObject: Boolean,
        val supportsNegativeButton: Boolean,
        val supportsDeviceCredential: Boolean,
        val supportsIrisRecognition: Boolean,
        val supportsVoiceRecognition: Boolean,
        val supportsContinuousAuth: Boolean,
        val supportsPassiveAuth: Boolean,
        val supportsBehavioralBiometrics: Boolean,
        val sensorInfo: List<SensorInfo>
    )
    
    data class SensorInfo(
        val id: String,
        val type: String,
        val manufacturer: String,
        val model: String,
        val version: String,
        val location: SensorLocation,
        val capabilities: SensorCapabilities
    )
    
    enum class SensorLocation {
        FRONT,          // Front-facing (e.g., selfie camera)
        BACK,           // Back-facing
        SIDE,           // Side-mounted (e.g., power button)
        UNDER_DISPLAY,  // Under-display fingerprint
        IN_DISPLAY,     // In-display fingerprint
        BEZEL,          // On device bezel
        EXTERNAL        // External sensor
    }
    
    data class SensorCapabilities(
        val resolution: String?,
        val captureArea: String?,
        val maxTemplates: Int,
        val antiSpoofingLevel: Int,
        val falseAcceptRate: Float,
        val falseRejectRate: Float
    )
    
    data class AuthenticationRequest(
        val title: String,
        val subtitle: String?,
        val description: String?,
        val negativeButtonText: String,
        val allowedAuthenticators: Int,
        val requireConfirmation: Boolean,
        val useCrypto: Boolean,
        val cryptoObject: CryptoObject?,
        val timeout: Long?, // milliseconds
        val fallbackToDeviceCredential: Boolean,
        val userId: String?,
        val challenge: ByteArray?
    )
    
    data class CryptoObject(
        val cipher: Cipher? = null,
        val signature: Signature? = null,
        val mac: javax.crypto.Mac? = null
    )
    
    data class AuthenticationResult(
        val success: Boolean,
        val authenticationType: String?,
        val cryptoObject: CryptoObject?,
        val timestamp: Long,
        val confidence: Float?, // 0.0 to 1.0
        val userId: String?,
        val errorCode: Int?,
        val errorMessage: String?,
        val additionalData: Map<String, Any>?
    )
    
    data class AuthenticationSession(
        val sessionId: String,
        val startTime: Long,
        val request: AuthenticationRequest,
        var attempts: Int,
        var isActive: Boolean
    )
    
    data class EnrollmentStatus(
        val biometricType: String,
        val enrolledCount: Int,
        val maxEnrollments: Int,
        val canEnrollMore: Boolean,
        val enrolledIds: List<String>?,
        val lastEnrollmentTime: Long?
    )
    
    data class BiometricTemplate(
        val id: String,
        val type: String,
        val userId: String,
        val createdAt: Long,
        val lastUsedAt: Long?,
        val name: String?,
        val metadata: Map<String, Any>?
    )
    
    data class LivenessCheck(
        val passed: Boolean,
        val confidence: Float,
        val checks: List<LivenessCheckType>,
        val spoofingRisk: SpoofingRisk
    )
    
    enum class LivenessCheckType {
        BLINK_DETECTION,
        HEAD_MOVEMENT,
        SMILE_DETECTION,
        DEPTH_ANALYSIS,
        TEXTURE_ANALYSIS,
        BLOOD_FLOW,
        THERMAL_IMAGING,
        CHALLENGE_RESPONSE
    }
    
    enum class SpoofingRisk {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    data class BehavioralProfile(
        val userId: String,
        val typingPattern: TypingPattern?,
        val touchPattern: TouchPattern?,
        val gaitPattern: GaitPattern?,
        val usagePattern: UsagePattern?,
        val confidence: Float,
        val lastUpdated: Long
    )
    
    data class TypingPattern(
        val averageWPM: Float,
        val dwellTime: Map<Char, Float>, // Key press duration
        val flightTime: Map<String, Float>, // Time between keystrokes
        val pressure: Map<Char, Float>?
    )
    
    data class TouchPattern(
        val averagePressure: Float,
        val averageArea: Float,
        val averageDuration: Float,
        val swipeVelocity: Float,
        val tapLocations: List<Pair<Float, Float>>
    )
    
    data class GaitPattern(
        val stepFrequency: Float,
        val stepLength: Float,
        val walkingSpeed: Float,
        val acceleration: FloatArray
    )
    
    data class UsagePattern(
        val appUsageFrequency: Map<String, Float>,
        val unlockTimes: List<Long>,
        val locationPatterns: List<Pair<Double, Double>>,
        val networkPatterns: List<String>
    )
    
    // ========== INITIALIZATION ==========
    
    init {
        initialize()
    }
    
    private fun initialize() {
        initializeBiometricServices()
        updateBiometricCapabilities()
        checkEnrollmentStatus()
        setupKeystoreKeys()
    }
    
    private fun initializeBiometricServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? AndroidBiometricManager
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
        }
        
        // FaceManager is not publicly available
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        //     faceManager = context.getSystemService(Context.FACE_SERVICE) as? FaceManager
        // }
    }
    
    private fun updateBiometricCapabilities() {
        val availableTypes = mutableListOf<BiometricType>()
        
        // Update fingerprint
        if (isFingerprintSupported()) {
            availableTypes.add(
                BiometricType(
                    type = TYPE_FINGERPRINT,
                    isAvailable = true,
                    isEnrolled = checkEnrolledFingerprints(),
                    securityLevel = SecurityLevel.STRONG,
                    maxAttempts = 5,
                    features = BiometricFeatures(
                        supportsCrypto = true,
                        supportsLiveness = true,
                        supportsAntiSpoofing = true,
                        supportsMultiUser = true,
                        supportsTemplateUpdate = false,
                        accuracy = 0.001f, // 0.1% FAR
                        speed = AuthenticationSpeed.FAST
                    )
                )
            )
        }
        
        // Update face recognition
        if (isFaceSupported()) {
            availableTypes.add(
                BiometricType(
                    type = TYPE_FACE,
                    isAvailable = true,
                    isEnrolled = checkEnrolledFace(),
                    securityLevel = getFaceSecurityLevel(),
                    maxAttempts = 3,
                    features = BiometricFeatures(
                        supportsCrypto = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                        supportsLiveness = isSecureFaceSupported(),
                        supportsAntiSpoofing = hasSecureFace(),
                        supportsMultiUser = false,
                        supportsTemplateUpdate = true,
                        accuracy = if (hasSecureFace()) 0.01f else 0.1f,
                        speed = AuthenticationSpeed.INSTANT
                    )
                )
            )
        }
        
        // Update iris (Samsung specific)
        if (isIrisSupported()) {
            availableTypes.add(
                BiometricType(
                    type = TYPE_IRIS,
                    isAvailable = true,
                    isEnrolled = checkEnrolledIris(),
                    securityLevel = SecurityLevel.STRONG,
                    maxAttempts = 5,
                    features = BiometricFeatures(
                        supportsCrypto = true,
                        supportsLiveness = true,
                        supportsAntiSpoofing = true,
                        supportsMultiUser = true,
                        supportsTemplateUpdate = false,
                        accuracy = 0.0001f, // 0.01% FAR
                        speed = AuthenticationSpeed.NORMAL
                    )
                )
            )
        }
        
        val capabilities = BiometricCapabilities(
            maxFingerprints = getMaxFingerprints(),
            maxFaces = getMaxFaces(),
            supportsCryptoObject = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            supportsNegativeButton = true,
            supportsDeviceCredential = keyguardManager.isDeviceSecure,
            supportsIrisRecognition = isIrisSupported(),
            supportsVoiceRecognition = isVoiceSupported(),
            supportsContinuousAuth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            supportsPassiveAuth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            supportsBehavioralBiometrics = false, // Would require custom implementation
            sensorInfo = getSensorInfo()
        )
        
        _biometricState.update {
            it.copy(
                isHardwareAvailable = availableTypes.isNotEmpty(),
                isEnrolled = availableTypes.any { type -> type.isEnrolled },
                availableTypes = availableTypes,
                capabilities = capabilities,
                securityLevel = determineOverallSecurityLevel(availableTypes)
            )
        }
    }
    
    private fun hasFingerprintHardware(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                biometricManager?.canAuthenticate(Authenticators.BIOMETRIC_STRONG) != 
                    AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                @Suppress("DEPRECATION")
                fingerprintManager?.isHardwareDetected == true
            }
            else -> false
        }
    }
    
    private fun hasEnrolledFingerprints(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                biometricManager?.canAuthenticate(Authenticators.BIOMETRIC_STRONG) == 
                    AndroidBiometricManager.BIOMETRIC_SUCCESS
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                @Suppress("DEPRECATION")
                fingerprintManager?.hasEnrolledFingerprints() == true
            }
            else -> false
        }
    }
    
    private fun hasFaceHardware(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Check via BiometricManager instead as FaceManager is not public
                biometricManager?.canAuthenticate(Authenticators.BIOMETRIC_WEAK) != 
                    AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
            }
            else -> false
        }
    }
    
    private fun hasEnrolledFace(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Check via BiometricManager as FaceManager is not public
                biometricManager?.canAuthenticate(Authenticators.BIOMETRIC_WEAK) == 
                    AndroidBiometricManager.BIOMETRIC_SUCCESS
            }
            else -> false
        }
    }
    
    private fun hasSecureFace(): Boolean {
        // Check if device has secure face recognition (3D face, not just 2D)
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            // Pixel 4 and later with Soli radar
            manufacturer.contains("google") && model.contains("pixel 4") -> true
            // Devices with structured light or ToF front camera
            hasFaceHardware() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Check if face can be used for crypto
                biometricManager?.canAuthenticate(Authenticators.BIOMETRIC_STRONG) == 
                    AndroidBiometricManager.BIOMETRIC_SUCCESS
            }
            else -> false
        }
    }
    
    private fun determineFaceSecurityLevel(): SecurityLevel {
        return when {
            hasSecureFace() -> SecurityLevel.STRONG
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> SecurityLevel.WEAK
            else -> SecurityLevel.CONVENIENCE
        }
    }
    
    private fun getMaxFaces(): Int {
        return 1 // Most devices support only one face
    }
    
    private fun getMaxFingerprints(): Int {
        return 5 // Typical maximum
    }
    
    private fun checkEnrolledIris(): Boolean {
        // Would require Samsung-specific API
        return false
    }
    
    private fun hasVoiceRecognition(): Boolean {
        // Check for voice recognition capability
        return packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }
    
    // ========== MISSING HELPER METHODS ==========
    
    private fun isFingerprintSupported(): Boolean {
        return hasFingerprintHardware()
    }
    
    private fun isFaceSupported(): Boolean {
        return hasFaceHardware()
    }
    
    private fun isIrisSupported(): Boolean {
        // Check if iris scanning is supported (Samsung specific)
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("samsung") && 
            (model.contains("galaxy s8") || model.contains("galaxy s9") || model.contains("galaxy note")) -> true
            else -> false
        }
    }
    
    private fun checkEnrolledFingerprints(): Boolean {
        return hasEnrolledFingerprints()
    }
    
    private fun checkEnrolledFace(): Boolean {
        return hasEnrolledFace()
    }
    
    private fun getFaceSecurityLevel(): SecurityLevel {
        return determineFaceSecurityLevel()
    }
    
    private fun isSecureFaceSupported(): Boolean {
        return hasSecureFace()
    }
    
    private fun isVoiceSupported(): Boolean {
        return hasVoiceRecognition()
    }
    
    private fun getSensorInfo(): List<SensorInfo> {
        return detectSensorInfo()
    }
    
    private fun detectSensorInfo(): List<SensorInfo> {
        val sensors = mutableListOf<SensorInfo>()
        
        if (hasFingerprintHardware()) {
            sensors.add(
                SensorInfo(
                    id = "fingerprint_sensor",
                    type = TYPE_FINGERPRINT,
                    manufacturer = Build.MANUFACTURER,
                    model = detectFingerprintSensorModel(),
                    version = "1.0",
                    location = detectFingerprintLocation(),
                    capabilities = SensorCapabilities(
                        resolution = "508dpi",
                        captureArea = "8x8mm",
                        maxTemplates = getMaxFingerprints(),
                        antiSpoofingLevel = 3,
                        falseAcceptRate = 0.001f,
                        falseRejectRate = 0.01f
                    )
                )
            )
        }
        
        if (hasFaceHardware()) {
            sensors.add(
                SensorInfo(
                    id = "face_sensor",
                    type = TYPE_FACE,
                    manufacturer = Build.MANUFACTURER,
                    model = "Front Camera",
                    version = "1.0",
                    location = SensorLocation.FRONT,
                    capabilities = SensorCapabilities(
                        resolution = "1080p",
                        captureArea = "Full Face",
                        maxTemplates = getMaxFaces(),
                        antiSpoofingLevel = if (hasSecureFace()) 3 else 1,
                        falseAcceptRate = if (hasSecureFace()) 0.01f else 0.1f,
                        falseRejectRate = 0.05f
                    )
                )
            )
        }
        
        return sensors
    }
    
    private fun detectFingerprintSensorModel(): String {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("pixel") -> "Pixel Imprint"
            model.contains("galaxy") -> "Samsung Fingerprint"
            model.contains("oneplus") -> "OnePlus Fingerprint"
            else -> "Generic Fingerprint"
        }
    }
    
    private fun detectFingerprintLocation(): SensorLocation {
        val model = Build.MODEL.lowercase()
        return when {
            // Under-display fingerprint
            model.contains("galaxy s2") || model.contains("oneplus 7") -> SensorLocation.UNDER_DISPLAY
            // Side-mounted fingerprint
            model.contains("galaxy s10e") || model.contains("pixel 5") -> SensorLocation.SIDE
            // Back-mounted fingerprint
            model.contains("pixel") && !model.contains("pixel 4") -> SensorLocation.BACK
            // Default to under-display for newer phones
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> SensorLocation.UNDER_DISPLAY
            else -> SensorLocation.BACK
        }
    }
    
    private fun determineOverallSecurityLevel(types: List<BiometricType>): SecurityLevel {
        return types.maxByOrNull { it.securityLevel.ordinal }?.securityLevel ?: SecurityLevel.NONE
    }
    
    private fun checkEnrollmentStatus() {
        val fingerprintStatus = if (hasFingerprintHardware()) {
            EnrollmentStatus(
                biometricType = TYPE_FINGERPRINT,
                enrolledCount = if (hasEnrolledFingerprints()) 1 else 0, // Can't get exact count
                maxEnrollments = getMaxFingerprints(),
                canEnrollMore = true,
                enrolledIds = null,
                lastEnrollmentTime = null
            )
        } else null
        
        val faceStatus = if (hasFaceHardware()) {
            EnrollmentStatus(
                biometricType = TYPE_FACE,
                enrolledCount = if (hasEnrolledFace()) 1 else 0,
                maxEnrollments = getMaxFaces(),
                canEnrollMore = !hasEnrolledFace(),
                enrolledIds = null,
                lastEnrollmentTime = null
            )
        } else null
        
        _enrollmentStatus.value = fingerprintStatus ?: faceStatus
    }
    
    private fun setupKeystoreKeys() {
        try {
            // Create encryption key
            if (!keyStore.containsAlias(KEY_ALIAS_PREFIX + "default")) {
                generateEncryptionKey(KEY_ALIAS_PREFIX + "default")
            }
            
            // Create signature key
            if (!keyStore.containsAlias(SIGNATURE_KEY_ALIAS)) {
                generateSignatureKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup keystore keys", e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateEncryptionKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }
        
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateSignatureKey() {
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )
        
        val builder = KeyGenParameterSpec.Builder(
            SIGNATURE_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }
        
        keyPairGenerator.initialize(builder.build())
        keyPairGenerator.generateKeyPair()
    }
    
    // ========== AUTHENTICATION ==========
    
    suspend fun authenticate(request: AuthenticationRequest): AuthenticationResult {
        return suspendCoroutine { continuation ->
            startAuthentication(request) { result ->
                continuation.resume(result)
            }
        }
    }
    
    private fun startAuthentication(
        request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        if (_biometricState.value.isLockedOut) {
            callback(
                AuthenticationResult(
                    success = false,
                    authenticationType = null,
                    cryptoObject = null,
                    timestamp = System.currentTimeMillis(),
                    confidence = null,
                    userId = request.userId,
                    errorCode = ERROR_LOCKOUT,
                    errorMessage = "Biometric authentication is locked out",
                    additionalData = null
                )
            )
            return
        }
        
        _biometricState.update { it.copy(isAuthenticating = true) }
        
        val sessionId = generateSessionId()
        currentAuthenticationSession = AuthenticationSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis(),
            request = request,
            attempts = 0,
            isActive = true
        )
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                authenticateWithBiometricPrompt(request, callback)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                authenticateWithFingerprint(request, callback)
            }
            else -> {
                authenticateWithDeviceCredential(request, callback)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticateWithBiometricPrompt(
        request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val biometricPrompt = BiometricPrompt.Builder(context)
            .setTitle(request.title)
            .apply {
                request.subtitle?.let { setSubtitle(it) }
                request.description?.let { setDescription(it) }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setConfirmationRequired(request.requireConfirmation)
                    
                    if (request.fallbackToDeviceCredential) {
                        setDeviceCredentialAllowed(true)
                    } else {
                        setNegativeButton(
                            request.negativeButtonText,
                            executor
                        ) { _, _ ->
                            handleAuthenticationCanceled(callback)
                        }
                    }
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(request.allowedAuthenticators)
                }
            }
            .build()
        
        val signal = CancellationSignal()
        cancellationSignal = signal
        
        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                handleAuthenticationSuccess(result, request, callback)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                handleAuthenticationFailed(callback)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                handleAuthenticationError(errorCode, errString.toString(), callback)
            }
            
            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                super.onAuthenticationHelp(helpCode, helpString)
                // Log help messages
                Log.d(TAG, "Authentication help: $helpString")
            }
        }
        
        try {
            if (request.useCrypto && request.cryptoObject != null) {
                val cryptoObject = when {
                    request.cryptoObject.cipher != null -> {
                        BiometricPrompt.CryptoObject(request.cryptoObject.cipher)
                    }
                    request.cryptoObject.signature != null -> {
                        BiometricPrompt.CryptoObject(request.cryptoObject.signature)
                    }
                    request.cryptoObject.mac != null -> {
                        BiometricPrompt.CryptoObject(request.cryptoObject.mac)
                    }
                    else -> null
                }
                
                cryptoObject?.let {
                    biometricPrompt.authenticate(it, signal, executor, authCallback)
                } ?: biometricPrompt.authenticate(signal, executor, authCallback)
            } else {
                biometricPrompt.authenticate(signal, executor, authCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start biometric authentication", e)
            handleAuthenticationError(ERROR_UNABLE_TO_PROCESS, e.message ?: "Unknown error", callback)
        }
    }
    
    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun authenticateWithFingerprint(
        request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        fingerprintManager?.let { manager ->
            if (!manager.isHardwareDetected || !manager.hasEnrolledFingerprints()) {
                handleAuthenticationError(ERROR_NO_BIOMETRICS, "No fingerprints enrolled", callback)
                return
            }
            
            val signal = CancellationSignal()
            cancellationSignal = signal
            
            val authCallback = object : FingerprintManager.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleFingerprintSuccess(result, request, callback)
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    handleAuthenticationFailed(callback)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    handleAuthenticationError(errorCode, errString.toString(), callback)
                }
                
                @Deprecated("Deprecated in Java")
                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                    super.onAuthenticationHelp(helpCode, helpString)
                    Log.d(TAG, "Authentication help: $helpString")
                }
            }
            
            try {
                val cryptoObject = request.cryptoObject?.cipher?.let {
                    FingerprintManager.CryptoObject(it)
                }
                
                manager.authenticate(
                    cryptoObject,
                    signal,
                    0,
                    authCallback,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start fingerprint authentication", e)
                handleAuthenticationError(ERROR_UNABLE_TO_PROCESS, e.message ?: "Unknown error", callback)
            }
        } ?: handleAuthenticationError(ERROR_HW_NOT_PRESENT, "Fingerprint hardware not available", callback)
    }
    
    private fun authenticateWithDeviceCredential(
        @Suppress("UNUSED_PARAMETER") request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        if (!keyguardManager.isDeviceSecure) {
            handleAuthenticationError(ERROR_NO_DEVICE_CREDENTIAL, "No device credential set", callback)
            return
        }
        
        // This would typically launch the system credential UI
        // Implementation depends on specific requirements
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleAuthenticationSuccess(
        result: BiometricPrompt.AuthenticationResult,
        request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        val cryptoObject = result.cryptoObject?.let {
            CryptoObject(
                cipher = it.cipher,
                signature = it.signature,
                mac = it.mac
            )
        }
        
        val authResult = AuthenticationResult(
            success = true,
            authenticationType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                when (result.authenticationType) {
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "biometric"
                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "device_credential"
                    else -> "unknown"
                }
            } else "biometric",
            cryptoObject = cryptoObject,
            timestamp = System.currentTimeMillis(),
            confidence = 0.99f,
            userId = request.userId,
            errorCode = null,
            errorMessage = null,
            additionalData = null
        )
        
        finalizeAuthentication(authResult, callback)
    }
    
    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleFingerprintSuccess(
        result: FingerprintManager.AuthenticationResult,
        request: AuthenticationRequest,
        callback: (AuthenticationResult) -> Unit
    ) {
        val cryptoObject = result.cryptoObject?.let {
            CryptoObject(cipher = it.cipher)
        }
        
        val authResult = AuthenticationResult(
            success = true,
            authenticationType = TYPE_FINGERPRINT,
            cryptoObject = cryptoObject,
            timestamp = System.currentTimeMillis(),
            confidence = 0.99f,
            userId = request.userId,
            errorCode = null,
            errorMessage = null,
            additionalData = null
        )
        
        finalizeAuthentication(authResult, callback)
    }
    
    private fun handleAuthenticationFailed(callback: (AuthenticationResult) -> Unit) {
        currentAuthenticationSession?.let { session ->
            session.attempts++
            
            if (session.attempts >= 5) {
                // Too many attempts, trigger temporary lockout
                triggerLockout()
                handleAuthenticationError(ERROR_LOCKOUT, "Too many failed attempts", callback)
                return
            }
        }
        
        _biometricState.update { it.copy(failedAttempts = it.failedAttempts + 1) }
    }
    
    private fun handleAuthenticationError(
        errorCode: Int,
        errorMessage: String,
        callback: (AuthenticationResult) -> Unit
    ) {
        val authResult = AuthenticationResult(
            success = false,
            authenticationType = null,
            cryptoObject = null,
            timestamp = System.currentTimeMillis(),
            confidence = null,
            userId = currentAuthenticationSession?.request?.userId,
            errorCode = errorCode,
            errorMessage = errorMessage,
            additionalData = null
        )
        
        finalizeAuthentication(authResult, callback)
    }
    
    private fun handleAuthenticationCanceled(callback: (AuthenticationResult) -> Unit) {
        handleAuthenticationError(ERROR_USER_CANCELED, "Authentication canceled by user", callback)
    }
    
    private fun finalizeAuthentication(
        result: AuthenticationResult,
        callback: (AuthenticationResult) -> Unit
    ) {
        currentAuthenticationSession?.isActive = false
        currentAuthenticationSession = null
        cancellationSignal = null
        
        _biometricState.update {
            it.copy(
                isAuthenticating = false,
                lastAuthTime = if (result.success) System.currentTimeMillis() else it.lastAuthTime,
                failedAttempts = if (result.success) 0 else it.failedAttempts
            )
        }
        
        scope.launch {
            _authenticationResult.emit(result)
        }
        
        callback(result)
    }
    
    private fun triggerLockout() {
        val lockoutDuration = 30000L // 30 seconds
        val lockoutEndTime = System.currentTimeMillis() + lockoutDuration
        
        _biometricState.update {
            it.copy(
                isLockedOut = true,
                lockoutEndTime = lockoutEndTime
            )
        }
        
        // Schedule unlock
        scope.launch {
            delay(lockoutDuration)
            _biometricState.update {
                it.copy(
                    isLockedOut = false,
                    lockoutEndTime = null,
                    failedAttempts = 0
                )
            }
        }
    }
    
    // ========== CRYPTO OPERATIONS ==========
    
    @RequiresApi(Build.VERSION_CODES.M)
    fun createCryptoObject(operation: CryptoOperation): CryptoObject? {
        return try {
            when (operation) {
                is CryptoOperation.Encryption -> {
                    val cipher = getCipher()
                    val secretKey = getOrCreateSecretKey(operation.keyAlias)
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                    CryptoObject(cipher = cipher)
                }
                is CryptoOperation.Decryption -> {
                    val cipher = getCipher()
                    val secretKey = getOrCreateSecretKey(operation.keyAlias)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(operation.iv))
                    CryptoObject(cipher = cipher)
                }
                is CryptoOperation.Signing -> {
                    val signature = Signature.getInstance("SHA256withECDSA")
                    val privateKey = keyStore.getKey(SIGNATURE_KEY_ALIAS, null) as java.security.PrivateKey
                    signature.initSign(privateKey)
                    CryptoObject(signature = signature)
                }
                is CryptoOperation.Verification -> {
                    val signature = Signature.getInstance("SHA256withECDSA")
                    val publicKey = keyStore.getCertificate(SIGNATURE_KEY_ALIAS).publicKey
                    signature.initVerify(publicKey)
                    CryptoObject(signature = signature)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create crypto object", e)
            null
        }
    }
    
    sealed class CryptoOperation {
        data class Encryption(val keyAlias: String) : CryptoOperation()
        data class Decryption(val keyAlias: String, val iv: ByteArray) : CryptoOperation()
        data class Signing(val data: ByteArray) : CryptoOperation()
        data class Verification(val data: ByteArray, val signature: ByteArray) : CryptoOperation()
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getOrCreateSecretKey(alias: String): SecretKey {
        return if (keyStore.containsAlias(alias)) {
            keyStore.getKey(alias, null) as SecretKey
        } else {
            generateEncryptionKey(alias)
            keyStore.getKey(alias, null) as SecretKey
        }
    }
    
    fun encrypt(data: ByteArray, cryptoObject: CryptoObject): EncryptedData? {
        return try {
            cryptoObject.cipher?.let { cipher ->
                val encryptedData = cipher.doFinal(data)
                val iv = cipher.iv
                EncryptedData(
                    data = encryptedData,
                    iv = iv
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            null
        }
    }
    
    fun decrypt(encryptedData: EncryptedData, cryptoObject: CryptoObject): ByteArray? {
        return try {
            cryptoObject.cipher?.doFinal(encryptedData.data)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            null
        }
    }
    
    fun sign(data: ByteArray, cryptoObject: CryptoObject): ByteArray? {
        return try {
            cryptoObject.signature?.let { signature ->
                signature.update(data)
                signature.sign()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed", e)
            null
        }
    }
    
    fun verify(data: ByteArray, signatureBytes: ByteArray, cryptoObject: CryptoObject): Boolean {
        return try {
            cryptoObject.signature?.let { signature ->
                signature.update(data)
                signature.verify(signatureBytes)
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed", e)
            false
        }
    }
    
    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray
    ) {
        fun toBase64(): String {
            return Base64.encodeToString(data, Base64.DEFAULT)
        }
        
        fun ivToBase64(): String {
            return Base64.encodeToString(iv, Base64.DEFAULT)
        }
    }
    
    // ========== LIVENESS DETECTION ==========
    
    suspend fun performLivenessCheck(type: String): LivenessCheck {
        // This would typically use camera and ML models
        // Simplified implementation for demonstration
        
        delay(1000) // Simulate processing
        
        val checks = when (type) {
            TYPE_FACE -> listOf(
                LivenessCheckType.BLINK_DETECTION,
                LivenessCheckType.HEAD_MOVEMENT,
                LivenessCheckType.DEPTH_ANALYSIS
            )
            TYPE_FINGERPRINT -> listOf(
                LivenessCheckType.BLOOD_FLOW,
                LivenessCheckType.TEXTURE_ANALYSIS
            )
            else -> emptyList()
        }
        
        return LivenessCheck(
            passed = true,
            confidence = 0.95f,
            checks = checks,
            spoofingRisk = SpoofingRisk.LOW
        )
    }
    
    // ========== BEHAVIORAL BIOMETRICS ==========
    
    fun createBehavioralProfile(userId: String): BehavioralProfile {
        // This would collect and analyze user behavior patterns
        return BehavioralProfile(
            userId = userId,
            typingPattern = TypingPattern(
                averageWPM = 60f,
                dwellTime = emptyMap(),
                flightTime = emptyMap(),
                pressure = null
            ),
            touchPattern = TouchPattern(
                averagePressure = 0.5f,
                averageArea = 100f,
                averageDuration = 200f,
                swipeVelocity = 500f,
                tapLocations = emptyList()
            ),
            gaitPattern = null,
            usagePattern = null,
            confidence = 0.7f,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    fun verifyBehavioralPattern(@Suppress("UNUSED_PARAMETER") stored: BehavioralProfile, @Suppress("UNUSED_PARAMETER") current: BehavioralProfile): Float {
        // Compare patterns and return confidence score
        return 0.8f // Simplified
    }
    
    // ========== UTILITY METHODS ==========
    
    fun cancelAuthentication() {
        cancellationSignal?.cancel()
        currentAuthenticationSession?.isActive = false
        _biometricState.update { it.copy(isAuthenticating = false) }
    }
    
    fun isDeviceSecure(): Boolean {
        return keyguardManager.isDeviceSecure
    }
    
    fun launchEnrollment(@Suppress("UNUSED_PARAMETER") biometricType: String) {
        // Launch system enrollment UI
        // Implementation depends on biometric type
    }
    
    private fun generateSessionId(): String {
        return "bio_session_${System.currentTimeMillis()}"
    }
    
    fun cleanup() {
        cancelAuthentication()
        scope.cancel()
    }
}

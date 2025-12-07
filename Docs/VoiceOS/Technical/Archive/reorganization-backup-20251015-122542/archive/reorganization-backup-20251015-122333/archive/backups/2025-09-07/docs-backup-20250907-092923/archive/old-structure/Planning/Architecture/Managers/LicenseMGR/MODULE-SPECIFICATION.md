# VOS3 Licensing & Subscription Module Specification
**Path:** /Volumes/M Drive/Coding/Warp/vos3-dev/ProjectDocs/LICENSING-MODULE-SPECIFICATION.md  
**Created:** 2025-01-18  
**Version:** 1.0.0

## Executive Summary

The VOS3 Licensing Module is a standalone, secure system for managing subscriptions, licenses, and feature access control. It separates business logic from core functionality, enabling flexible monetization while maintaining code security and preventing piracy.

## Module Architecture

### 1. Core Components

```
licensing-module/
├── core/
│   ├── LicenseValidator.kt          # Core validation logic
│   ├── LicenseTypes.kt              # License type definitions
│   ├── FeatureGates.kt              # Feature access control
│   └── CryptoManager.kt             # Encryption/decryption
├── server/
│   ├── ServerValidator.kt           # Server-side validation
│   ├── TokenManager.kt              # JWT token management
│   └── RateLimiter.kt               # API rate limiting
├── local/
│   ├── LocalLicenseStore.kt        # Encrypted local storage
│   ├── OfflineValidator.kt         # Offline validation
│   └── CacheManager.kt             # License cache
├── billing/
│   ├── GooglePlayBilling.kt        # Play Store integration
│   ├── StripeIntegration.kt        # Web payments
│   └── PaymentProcessor.kt         # Payment abstraction
├── analytics/
│   ├── UsageTracker.kt             # Feature usage tracking
│   ├── LicenseMetrics.kt          # License analytics
│   └── RevenueReporter.kt         # Revenue tracking
└── ui/
    ├── LicenseActivity.kt          # License management UI
    ├── SubscriptionDialog.kt       # Subscription UI
    └── TrialBanner.kt              # Trial status UI
```

## License Types & Tiers

### Free Tier (Vosk Engine)
- **Features:**
  - 8 language support (EN, ES, FR, DE, RU, ZH, JA, KO)
  - Basic voice commands
  - Offline functionality
  - 30-second continuous recognition limit
  - Standard UI themes
- **Restrictions:**
  - No custom commands
  - No cloud sync
  - No advanced features
  - Community support only

### Trial Period (7 Days)
- **Features:**
  - Full Premium access
  - All 40+ languages
  - Vivoka engine access
  - Cloud features enabled
  - Priority support
- **Implementation:**
  ```kotlin
  data class TrialState(
      val startDate: Long,
      val endDate: Long,
      val deviceId: String,
      val hasBeenUsed: Boolean,
      val gracePeriodHours: Int = 24
  )
  ```

### Premium Monthly ($9.99/month)
- **Features:**
  - All Trial features
  - Monthly billing cycle
  - Cancel anytime
  - Cloud backup
  - Multi-device sync (3 devices)

### Premium Annual ($79.99/year)
- **Features:**
  - All Monthly features
  - 33% discount
  - Priority updates
  - Beta features access
  - Multi-device sync (5 devices)

### Lifetime License ($299.99)
- **Features:**
  - All Premium features forever
  - Unlimited devices
  - Source code access (upon request)
  - White-label options
  - Commercial use license
  - Dedicated support channel

### Enterprise/Custom
- **Features:**
  - Custom pricing
  - SLA guarantees
  - On-premise deployment
  - Custom features
  - Training & integration support

## Implementation Roadmap

### Phase 1: Core Infrastructure (Week 1-2)

#### 1.1 License Data Model
```kotlin
@Entity
data class License(
    @PrimaryKey val licenseKey: String,
    val type: LicenseType,
    val status: LicenseStatus,
    val userId: String,
    val deviceIds: List<String>,
    val createdAt: Long,
    val expiresAt: Long?,
    val features: Set<Feature>,
    val metadata: Map<String, Any>
)

enum class LicenseType {
    FREE, TRIAL, MONTHLY, ANNUAL, LIFETIME, ENTERPRISE
}

enum class LicenseStatus {
    ACTIVE, EXPIRED, SUSPENDED, REVOKED, PENDING
}

enum class Feature {
    VOSK_ENGINE,
    VIVOKA_ENGINE,
    CLOUD_SYNC,
    CUSTOM_COMMANDS,
    ADVANCED_UI,
    MULTI_DEVICE,
    PRIORITY_SUPPORT,
    BETA_ACCESS,
    COMMERCIAL_USE
}
```

#### 1.2 Cryptographic Security
```kotlin
class CryptoManager {
    private val algorithm = "AES/GCM/NoPadding"
    private val keyDerivation = "PBKDF2WithHmacSHA256"
    
    fun generateLicenseKey(): String {
        // Format: TYPE-XXXX-XXXX-XXXX-XXXX
        // Where X = Base32 encoded bytes
        val type = getLicenseTypePrefix()
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        
        return "$type-${encodeBase32(bytes)}"
    }
    
    fun signLicense(license: License): String {
        // Create JWT token with license data
        val jwt = JWT.create()
            .withIssuer("augmentalis.com")
            .withSubject(license.userId)
            .withClaim("key", license.licenseKey)
            .withClaim("type", license.type.name)
            .withClaim("features", license.features.map { it.name })
            .withExpiresAt(Date(license.expiresAt ?: Long.MAX_VALUE))
            .sign(Algorithm.HMAC256(getSecretKey()))
        
        return jwt
    }
    
    fun encryptLocalLicense(license: License): ByteArray {
        val key = deriveKeyFromDevice()
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val json = Json.encodeToString(license)
        return cipher.doFinal(json.toByteArray())
    }
}
```

### Phase 2: Server Integration (Week 3-4)

#### 2.1 License Server API
```kotlin
interface LicenseServerAPI {
    @POST("/api/v1/license/validate")
    suspend fun validateLicense(
        @Body request: ValidateRequest
    ): Response<ValidateResponse>
    
    @POST("/api/v1/license/activate")
    suspend fun activateLicense(
        @Body request: ActivateRequest
    ): Response<ActivateResponse>
    
    @POST("/api/v1/license/refresh")
    suspend fun refreshLicense(
        @Header("Authorization") token: String
    ): Response<RefreshResponse>
    
    @GET("/api/v1/license/features")
    suspend fun getFeatures(
        @Query("license") licenseKey: String
    ): Response<FeaturesResponse>
}

data class ValidateRequest(
    val licenseKey: String,
    val deviceId: String,
    val appVersion: String,
    val timestamp: Long,
    val signature: String
)
```

#### 2.2 Offline Validation
```kotlin
class OfflineValidator {
    private val maxOfflineDays = 30
    private val gracePeriodHours = 72
    
    fun validateOffline(license: License): ValidationResult {
        // Check local signature
        if (!verifyLocalSignature(license)) {
            return ValidationResult.Invalid("Invalid signature")
        }
        
        // Check expiration with grace period
        val now = System.currentTimeMillis()
        val expiry = license.expiresAt ?: Long.MAX_VALUE
        
        if (now > expiry + TimeUnit.HOURS.toMillis(gracePeriodHours)) {
            return ValidationResult.Expired
        }
        
        // Check last online validation
        val lastOnline = getLastOnlineValidation()
        val daysSinceOnline = TimeUnit.MILLISECONDS.toDays(now - lastOnline)
        
        if (daysSinceOnline > maxOfflineDays) {
            return ValidationResult.RequiresOnline
        }
        
        return ValidationResult.Valid(license)
    }
}
```

### Phase 3: Payment Integration (Week 5-6)

#### 3.1 Google Play Billing
```kotlin
class GooglePlayBillingManager(
    private val activity: Activity
) : PurchasesUpdatedListener {
    
    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    
    fun initializeBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }
        })
    }
    
    fun purchaseSubscription(sku: String) {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(getSkuDetails(sku))
            .build()
        
        billingClient.launchBillingFlow(activity, flowParams)
    }
    
    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                // Handle cancellation
            }
            else -> {
                // Handle error
            }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        // Verify with server
        verifyPurchaseWithServer(purchase) { isValid ->
            if (isValid) {
                acknowledgePurchase(purchase)
                activateLicense(purchase)
            }
        }
    }
}
```

#### 3.2 Stripe Web Payments
```kotlin
class StripePaymentProcessor {
    private val stripe = Stripe(BuildConfig.STRIPE_PUBLIC_KEY)
    
    suspend fun createPaymentIntent(
        amount: Int,
        currency: String,
        licenseType: LicenseType
    ): PaymentIntent {
        return withContext(Dispatchers.IO) {
            val response = apiClient.post("/create-payment-intent") {
                contentType(ContentType.Application.Json)
                body = PaymentIntentRequest(
                    amount = amount,
                    currency = currency,
                    metadata = mapOf(
                        "license_type" to licenseType.name,
                        "user_id" to getUserId()
                    )
                )
            }
            response.body<PaymentIntent>()
        }
    }
    
    fun processPayment(
        paymentMethodId: String,
        paymentIntentClientSecret: String
    ) {
        val confirmParams = ConfirmPaymentIntentParams
            .createWithPaymentMethodId(
                paymentMethodId,
                paymentIntentClientSecret
            )
        
        stripe.confirmPayment(activity, confirmParams)
    }
}
```

### Phase 4: Feature Gating (Week 7)

#### 4.1 Feature Access Control
```kotlin
class FeatureGateManager(
    private val licenseManager: LicenseManager
) {
    private val featureCache = mutableMapOf<Feature, Boolean>()
    
    fun isFeatureEnabled(feature: Feature): Boolean {
        // Check cache first
        featureCache[feature]?.let { return it }
        
        // Get current license
        val license = licenseManager.getCurrentLicense()
        
        // Check feature availability
        val isEnabled = when (feature) {
            Feature.VOSK_ENGINE -> true // Always available
            Feature.VIVOKA_ENGINE -> license.type != LicenseType.FREE
            Feature.CLOUD_SYNC -> license.type.isPremium()
            Feature.CUSTOM_COMMANDS -> license.type.isPremium()
            Feature.MULTI_DEVICE -> {
                when (license.type) {
                    LicenseType.MONTHLY -> license.deviceIds.size <= 3
                    LicenseType.ANNUAL -> license.deviceIds.size <= 5
                    LicenseType.LIFETIME -> true
                    else -> false
                }
            }
            else -> license.features.contains(feature)
        }
        
        // Cache result
        featureCache[feature] = isEnabled
        return isEnabled
    }
    
    @RequiresFeature(Feature.VIVOKA_ENGINE)
    fun useVivokaEngine() {
        // Automatically checked via annotation
    }
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresFeature(val feature: Feature)
```

### Phase 5: Anti-Piracy Measures (Week 8)

#### 5.1 License Protection
```kotlin
class AntiPiracyManager {
    private val suspiciousPatterns = listOf(
        "lucky.*patcher",
        "freedom",
        "game.*guardian",
        "cheat.*engine"
    )
    
    fun checkIntegrity(): IntegrityResult {
        val checks = listOf(
            checkSignature(),
            checkInstaller(),
            checkDebugging(),
            checkEmulator(),
            checkRoot(),
            checkSuspiciousApps(),
            checkBuildConfig()
        )
        
        return IntegrityResult(
            passed = checks.all { it },
            details = buildIntegrityReport(checks)
        )
    }
    
    private fun checkSignature(): Boolean {
        val packageInfo = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        
        val signatures = packageInfo.signatures
        val expectedSignature = BuildConfig.APP_SIGNATURE
        
        return signatures.any { 
            it.toCharsString() == expectedSignature 
        }
    }
    
    private fun checkInstaller(): Boolean {
        val installer = context.packageManager
            .getInstallerPackageName(context.packageName)
        
        val validInstallers = listOf(
            "com.android.vending",  // Play Store
            "com.amazon.venezia",   // Amazon
            "com.sec.android.app.samsungapps" // Samsung
        )
        
        return installer in validInstallers
    }
    
    private fun obfuscateLicenseCheck() {
        // Use reflection and dynamic loading
        val className = String(Base64.decode("TGljZW5zZVZhbGlkYXRvcg=="))
        val methodName = String(Base64.decode("dmFsaWRhdGU="))
        
        val clazz = Class.forName(className)
        val method = clazz.getDeclaredMethod(methodName)
        method.invoke(null)
    }
}
```

#### 5.2 Code Obfuscation Rules
```proguard
# License module obfuscation
-keep class com.augmentalis.voiceos.licensing.api.** { *; }

-obfuscationdictionary obfuscation-dict.txt
-classobfuscationdictionary class-dict.txt
-packageobfuscationdictionary package-dict.txt

# Encrypt strings
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(...);
}

# Hide license validation
-repackageclasses 'o'
```

### Phase 6: Analytics & Monitoring (Week 9)

#### 6.1 Usage Analytics
```kotlin
class LicenseAnalytics {
    private val analytics = Firebase.analytics
    
    fun trackLicenseEvent(event: LicenseEvent) {
        val bundle = Bundle().apply {
            putString("event_type", event.type.name)
            putString("license_type", event.licenseType.name)
            putLong("timestamp", event.timestamp)
            putString("user_id", event.userId)
            
            event.metadata.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        analytics.logEvent("license_event", bundle)
    }
    
    fun trackRevenue(transaction: Transaction) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, transaction.id)
            putString(FirebaseAnalytics.Param.AFFILIATION, "direct")
            putDouble(FirebaseAnalytics.Param.VALUE, transaction.amount)
            putString(FirebaseAnalytics.Param.CURRENCY, transaction.currency)
            putDouble(FirebaseAnalytics.Param.TAX, transaction.tax)
        }
        
        analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
    }
}
```

#### 6.2 Monitoring Dashboard
```kotlin
data class LicenseMetrics(
    val totalLicenses: Int,
    val activeLicenses: Int,
    val expiredLicenses: Int,
    val trialConversions: Float,
    val monthlyRevenue: Double,
    val churnRate: Float,
    val averageLifetimeValue: Double,
    val topFeatures: List<FeatureUsage>
)

class MetricsDashboard {
    fun generateReport(): LicenseMetrics {
        return LicenseMetrics(
            totalLicenses = countTotalLicenses(),
            activeLicenses = countActiveLicenses(),
            expiredLicenses = countExpiredLicenses(),
            trialConversions = calculateTrialConversion(),
            monthlyRevenue = calculateMonthlyRevenue(),
            churnRate = calculateChurnRate(),
            averageLifetimeValue = calculateLTV(),
            topFeatures = getTopUsedFeatures()
        )
    }
}
```

## User Interface Components

### License Management UI
```kotlin
class LicenseManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLicenseManagementBinding
    private val viewModel: LicenseViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseManagementBinding.inflate(layoutInflater)
        
        setupUI()
        observeLicenseState()
    }
    
    private fun setupUI() {
        with(binding) {
            // Current license display
            viewModel.currentLicense.observe(this@LicenseManagementActivity) { license ->
                licenseTypeText.text = license.type.displayName
                expiryDateText.text = formatExpiryDate(license.expiresAt)
                featuresRecycler.adapter = FeaturesAdapter(license.features)
            }
            
            // Upgrade button
            upgradeButton.setOnClickListener {
                showUpgradeDialog()
            }
            
            // Enter license key
            enterLicenseButton.setOnClickListener {
                showLicenseKeyDialog()
            }
            
            // Restore purchases
            restoreButton.setOnClickListener {
                viewModel.restorePurchases()
            }
        }
    }
}
```

### Trial Banner Component
```kotlin
class TrialBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    
    private val binding = ViewTrialBannerBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    fun updateTrialStatus(daysRemaining: Int) {
        with(binding) {
            when {
                daysRemaining > 3 -> {
                    root.setBackgroundColor(Color.parseColor("#4CAF50"))
                    trialText.text = "$daysRemaining days of Premium trial remaining"
                    upgradeButton.text = "Upgrade Now"
                }
                daysRemaining > 0 -> {
                    root.setBackgroundColor(Color.parseColor("#FF9800"))
                    trialText.text = "Only $daysRemaining days left in trial!"
                    upgradeButton.text = "Don't Lose Access"
                }
                else -> {
                    root.setBackgroundColor(Color.parseColor("#F44336"))
                    trialText.text = "Trial expired - Upgrade to keep Premium features"
                    upgradeButton.text = "Upgrade Now"
                }
            }
        }
    }
}
```

## Testing Strategy

### Unit Tests
```kotlin
class LicenseValidatorTest {
    @Test
    fun `valid license passes validation`() {
        val license = createValidLicense()
        val result = validator.validate(license)
        
        assertTrue(result.isValid)
        assertEquals(license.type, result.licenseType)
    }
    
    @Test
    fun `expired license fails validation`() {
        val license = createExpiredLicense()
        val result = validator.validate(license)
        
        assertFalse(result.isValid)
        assertEquals(ValidationError.EXPIRED, result.error)
    }
    
    @Test
    fun `tampered license fails signature check`() {
        val license = createValidLicense()
        license.tamperWith()
        val result = validator.validate(license)
        
        assertFalse(result.isValid)
        assertEquals(ValidationError.INVALID_SIGNATURE, result.error)
    }
}
```

### Integration Tests
```kotlin
class PaymentIntegrationTest {
    @Test
    fun `successful payment activates license`() = runTest {
        // Mock payment success
        mockPaymentProcessor.mockSuccessfulPayment()
        
        // Initiate purchase
        val result = licenseManager.purchaseSubscription(
            LicenseType.MONTHLY
        )
        
        // Verify license activated
        assertTrue(result.isSuccess)
        
        val license = licenseManager.getCurrentLicense()
        assertEquals(LicenseType.MONTHLY, license.type)
        assertTrue(license.isActive)
    }
}
```

## Security Considerations

### 1. Key Storage
- Never store keys in plain text
- Use Android Keystore for sensitive data
- Implement key rotation mechanism
- Use certificate pinning for API calls

### 2. Communication Security
- All API calls over HTTPS
- Certificate pinning
- Request signing with HMAC
- Timestamp validation (±5 minutes)

### 3. Local Validation
- Obfuscate validation logic
- Multiple validation points
- Random validation delays
- Integrity checks

### 4. Server Validation
- Rate limiting per device/user
- Anomaly detection
- Geographic validation
- Device fingerprinting

## Migration Strategy

### From Existing System
1. **Identify current users**: Export existing license database
2. **Generate migration tokens**: Create one-time tokens for existing users
3. **Grandfathering**: Honor existing lifetime licenses
4. **Communication**: Email users about migration
5. **Grace period**: 60-day migration window
6. **Fallback**: Maintain legacy validation for transition

### Database Migration
```sql
-- Migration script
CREATE TABLE licenses_v2 (
    id UUID PRIMARY KEY,
    license_key VARCHAR(24) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    device_ids TEXT[],
    features JSONB,
    metadata JSONB
);

-- Migrate existing data
INSERT INTO licenses_v2 (...)
SELECT ... FROM licenses_v1;

-- Add indexes
CREATE INDEX idx_license_key ON licenses_v2(license_key);
CREATE INDEX idx_user_id ON licenses_v2(user_id);
CREATE INDEX idx_expires_at ON licenses_v2(expires_at);
```

## Performance Optimization

### 1. Caching Strategy
```kotlin
class LicenseCache {
    private val memoryCache = LruCache<String, License>(10)
    private val diskCache = DiskLruCache.open(cacheDir, 1, 1, 10 * 1024 * 1024)
    
    fun getCachedLicense(key: String): License? {
        // Check memory cache
        memoryCache.get(key)?.let { return it }
        
        // Check disk cache
        diskCache.get(key)?.let { snapshot ->
            val license = deserializeLicense(snapshot.getInputStream(0))
            memoryCache.put(key, license)
            return license
        }
        
        return null
    }
}
```

### 2. Batch Validation
- Validate multiple licenses in single API call
- Reduce network overhead
- Implement exponential backoff

### 3. Background Processing
- Use WorkManager for periodic validation
- Respect battery optimization
- Handle Doze mode properly

## Support & Documentation

### API Documentation
- OpenAPI/Swagger specification
- SDK documentation (Kotlin, Java)
- Integration guides
- Code examples

### Customer Support
- In-app support chat
- Email support tiers
- FAQ and knowledge base
- Video tutorials

### Developer Resources
- GitHub repository (private)
- Discord community
- Stack Overflow tags
- Regular webinars

## Revenue Projections

### Conversion Funnel
1. **Free Users**: 100,000 (100%)
2. **Trial Started**: 15,000 (15%)
3. **Trial Completed**: 10,000 (10%)
4. **Paid Conversion**: 2,500 (2.5%)

### Monthly Recurring Revenue (MRR)
- Monthly subscriptions: 1,500 × $9.99 = $14,985
- Annual subscriptions: 800 × $6.67 = $5,336
- Total MRR: ~$20,321

### Lifetime Value (LTV)
- Monthly subscriber: $9.99 × 8 months = $79.92
- Annual subscriber: $79.99 × 1.5 years = $119.99
- Lifetime purchaser: $299.99 (one-time)

## Implementation Timeline

### Month 1: Foundation
- Week 1-2: Core infrastructure
- Week 3-4: Server integration

### Month 2: Monetization
- Week 5-6: Payment integration
- Week 7: Feature gating
- Week 8: Anti-piracy

### Month 3: Polish & Launch
- Week 9: Analytics
- Week 10: UI implementation
- Week 11: Testing
- Week 12: Beta release

### Month 4: Production
- Week 13-14: Beta feedback
- Week 15: Final fixes
- Week 16: Production release

## Success Metrics

### Key Performance Indicators (KPIs)
1. **Trial Conversion Rate**: Target 15-20%
2. **Monthly Churn**: Target <5%
3. **Customer Acquisition Cost**: Target <$30
4. **Average Revenue Per User**: Target $15/month
5. **Support Ticket Rate**: Target <2%

### Monitoring & Alerts
- License validation failures >1%
- Payment processing errors >0.5%
- Server response time >200ms
- Unusual activation patterns
- Revenue anomalies

## Conclusion

This licensing module provides a robust, secure, and scalable solution for VOS3 monetization. The modular architecture allows for easy maintenance and updates while the comprehensive security measures protect against piracy. The flexible tier system accommodates various user needs while maximizing revenue potential.

The implementation follows industry best practices for subscription management, payment processing, and security. With proper execution, this system will provide sustainable revenue while maintaining excellent user experience.

---

*End of Licensing Module Specification*
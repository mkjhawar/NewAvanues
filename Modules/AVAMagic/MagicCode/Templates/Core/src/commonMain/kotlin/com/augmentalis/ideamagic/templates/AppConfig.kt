package com.augmentalis.magicui.templates

/**
 * Complete app configuration for template-based generation.
 *
 * Combines all configuration aspects:
 * - Template selection
 * - Branding (colors, logo, name)
 * - Database configuration
 * - Feature toggles
 * - Platform targets
 * - Payment providers
 * - External integrations
 *
 * **Example**:
 * ```kotlin
 * val app = generateApp {
 *     template = AppTemplate.ECOMMERCE
 *
 *     branding {
 *         name = "TechGadgets Shop"
 *         package = "com.techgadgets.shop"
 *         colors {
 *             primary = Color(0xFF1976D2)
 *             secondary = Color(0xFFFFA726)
 *         }
 *     }
 *
 *     database {
 *         dialect = SQLDialect.POSTGRESQL
 *         host = "localhost"
 *         name = "shop_db"
 *     }
 *
 *     features {
 *         enable(Feature.PRODUCT_CATALOG)
 *         enable(Feature.SHOPPING_CART)
 *         disable(Feature.WISHLIST)
 *     }
 *
 *     platforms {
 *         android { minSdk = 26 }
 *         ios { minVersion = "15.0" }
 *     }
 * }
 *
 * app.generate(outputPath = "/path/to/output")
 * ```
 *
 * @since 1.0.0
 */
data class AppConfig(
    val template: AppTemplate,
    val branding: BrandingConfig = BrandingConfig.DEFAULT,
    val database: DatabaseConfig = DatabaseConfig.DEFAULT,
    val features: Set<Feature> = template.features,
    val platforms: PlatformConfig = PlatformConfig.DEFAULT,
    val payments: PaymentConfig? = null,
    val integrations: Set<Integration> = emptySet(),
    val customizations: Map<String, Any> = emptyMap()
) {
    init {
        require(features.isNotEmpty()) { "At least one feature must be enabled" }
        require(platforms.targets.isNotEmpty()) { "At least one platform must be targeted" }
    }

    /**
     * Validate complete app configuration.
     */
    fun validate() {
        template.validate()
        branding.validate()
        database.validate()
        platforms.validate()
        payments?.validate()
    }

    /**
     * Get all required dependencies for this configuration.
     */
    fun getDependencies(): Set<Dependency> = buildSet {
        // Template dependencies
        addAll(template.dependencies)

        // Database dependencies
        addAll(database.getDependencies())

        // Payment provider dependencies
        payments?.let { addAll(it.getDependencies()) }

        // Integration dependencies
        integrations.forEach { integration ->
            addAll(integration.getDependencies())
        }

        // Feature-specific dependencies
        features.forEach { feature ->
            addAll(getFeatureDependencies(feature))
        }
    }

    private fun getFeatureDependencies(feature: Feature): Set<Dependency> = when (feature) {
        Feature.MEDIA_UPLOAD -> setOf(
            Dependency("io.coil-kt", "coil", "2.5.0"),
            Dependency("io.coil-kt", "coil-compose", "2.5.0")
        )
        Feature.VIDEO_PLAYER -> setOf(
            Dependency("com.google.android.exoplayer", "exoplayer", "2.19.1")
        )
        Feature.ANALYTICS -> setOf(
            Dependency("com.google.firebase", "firebase-analytics", "21.5.0")
        )
        else -> emptySet()
    }
}

/**
 * Platform configuration.
 *
 * @property targets Set of platforms to target
 * @property android Android-specific configuration
 * @property ios iOS-specific configuration
 * @property desktop Desktop-specific configuration
 */
data class PlatformConfig(
    val targets: Set<Platform> = setOf(Platform.ANDROID, Platform.IOS),
    val android: AndroidConfig = AndroidConfig.DEFAULT,
    val ios: IOSConfig = IOSConfig.DEFAULT,
    val desktop: DesktopConfig = DesktopConfig.DEFAULT
) {
    init {
        require(targets.isNotEmpty()) { "At least one platform must be targeted" }
    }

    /**
     * Validate platform configuration.
     */
    fun validate() {
        if (Platform.ANDROID in targets) android.validate()
        if (Platform.IOS in targets) ios.validate()
        if (Platform.DESKTOP in targets) desktop.validate()
    }

    companion object {
        val DEFAULT = PlatformConfig()
    }
}

/**
 * Android platform configuration.
 *
 * @property minSdk Minimum Android SDK version
 * @property targetSdk Target Android SDK version
 * @property compileSdk Compile Android SDK version
 */
data class AndroidConfig(
    val minSdk: Int = 26,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34
) {
    init {
        require(minSdk in 21..targetSdk) { "Min SDK must be 21-$targetSdk" }
        require(targetSdk <= compileSdk) { "Target SDK must be <= compile SDK" }
    }

    fun validate() {
        // Validation already done in init
    }

    companion object {
        val DEFAULT = AndroidConfig()
    }
}

/**
 * iOS platform configuration.
 *
 * @property minVersion Minimum iOS version (e.g., "15.0")
 * @property targets iOS targets (e.g., iosArm64, iosSimulatorArm64)
 */
data class IOSConfig(
    val minVersion: String = "15.0",
    val targets: Set<IOSTarget> = setOf(IOSTarget.ARM64, IOSTarget.SIMULATOR_ARM64, IOSTarget.X64)
) {
    init {
        require(minVersion.matches(Regex("\\d+\\.\\d+"))) {
            "iOS version must be in format X.Y (e.g., 15.0)"
        }
        val version = minVersion.split(".")[0].toInt()
        require(version >= 13) { "Minimum iOS version must be 13.0 or higher" }
    }

    fun validate() {
        // Validation already done in init
    }

    companion object {
        val DEFAULT = IOSConfig()
    }
}

/**
 * iOS target architectures.
 */
enum class IOSTarget {
    ARM64,           // Real devices (iPhone, iPad)
    SIMULATOR_ARM64, // M1/M2 Mac simulators
    X64              // Intel Mac simulators
}

/**
 * Desktop platform configuration.
 *
 * @property targets Desktop targets (Windows, macOS, Linux)
 * @property jvmTarget JVM target version
 */
data class DesktopConfig(
    val targets: Set<JVMTarget> = setOf(JVMTarget.WINDOWS, JVMTarget.MAC, JVMTarget.LINUX),
    val jvmTarget: String = "17"
) {
    init {
        require(targets.isNotEmpty()) { "At least one desktop target must be specified" }
        require(jvmTarget.toIntOrNull() in setOf(11, 17, 21)) {
            "JVM target must be 11, 17, or 21"
        }
    }

    fun validate() {
        // Validation already done in init
    }

    companion object {
        val DEFAULT = DesktopConfig()
    }
}

/**
 * Desktop JVM targets.
 */
enum class JVMTarget {
    WINDOWS,
    MAC,
    LINUX
}

/**
 * Payment provider configuration.
 *
 * @property provider Payment provider (Stripe, PayPal, Square)
 * @property apiKey API key for the provider
 * @property secretKey Secret key for the provider
 * @property currency Default currency (USD, EUR, GBP, etc.)
 * @property testMode Whether to use test/sandbox mode
 */
data class PaymentConfig(
    val provider: PaymentProvider,
    val apiKey: String,
    val secretKey: String? = null,
    val currency: String = "USD",
    val testMode: Boolean = true
) {
    init {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(currency.matches(Regex("[A-Z]{3}"))) {
            "Currency must be 3-letter ISO code (e.g., USD, EUR, GBP)"
        }
    }

    /**
     * Validate payment configuration.
     */
    fun validate() {
        if (provider == PaymentProvider.STRIPE) {
            require(apiKey.startsWith("pk_")) { "Stripe API key must start with pk_" }
        }
    }

    /**
     * Get required dependencies for payment provider.
     */
    fun getDependencies(): Set<Dependency> = when (provider) {
        PaymentProvider.STRIPE -> setOf(
            Dependency("com.stripe", "stripe-java", "24.1.0"),
            Dependency("com.stripe", "stripe-android", "20.35.0")
        )
        PaymentProvider.PAYPAL -> setOf(
            Dependency("com.paypal.sdk", "paypal-android-sdk", "2.16.0")
        )
        PaymentProvider.SQUARE -> setOf(
            Dependency("com.squareup.sdk", "in-app-payments-sdk", "1.5.4")
        )
    }
}

/**
 * Payment providers.
 */
enum class PaymentProvider {
    STRIPE,
    PAYPAL,
    SQUARE
}

/**
 * External integration.
 */
sealed interface Integration {
    val name: String
    fun getDependencies(): Set<Dependency>

    /**
     * Firebase integration.
     */
    data class Firebase(
        val services: Set<FirebaseService> = setOf(FirebaseService.ANALYTICS)
    ) : Integration {
        override val name: String = "Firebase"

        override fun getDependencies(): Set<Dependency> = buildSet {
            add(Dependency("com.google.firebase", "firebase-bom", "32.5.0"))
            services.forEach { service ->
                add(Dependency("com.google.firebase", service.artifactId, ""))
            }
        }
    }

    /**
     * AWS integration.
     */
    data class AWS(
        val services: Set<AWSService> = setOf(AWSService.S3)
    ) : Integration {
        override val name: String = "AWS"

        override fun getDependencies(): Set<Dependency> = buildSet {
            services.forEach { service ->
                add(Dependency("com.amazonaws", service.artifactId, "2.17.289"))
            }
        }
    }
}

/**
 * Firebase services.
 */
enum class FirebaseService(val artifactId: String) {
    ANALYTICS("firebase-analytics"),
    AUTH("firebase-auth"),
    FIRESTORE("firebase-firestore"),
    STORAGE("firebase-storage"),
    MESSAGING("firebase-messaging")
}

/**
 * AWS services.
 */
enum class AWSService(val artifactId: String) {
    S3("aws-java-sdk-s3"),
    DYNAMODB("aws-java-sdk-dynamodb"),
    SNS("aws-java-sdk-sns"),
    SES("aws-java-sdk-ses")
}

/**
 * DSL marker for app configuration.
 */
@DslMarker
annotation class AppConfigDsl

/**
 * Build an app configuration.
 */
@AppConfigDsl
fun generateApp(builder: AppConfigBuilder.() -> Unit): AppConfigBuilder {
    return AppConfigBuilder().apply(builder)
}

/**
 * Builder for app configuration.
 */
@AppConfigDsl
class AppConfigBuilder {
    lateinit var template: AppTemplate
    var branding: BrandingConfig = BrandingConfig.DEFAULT
    var database: DatabaseConfig = DatabaseConfig.DEFAULT
    private val features: MutableSet<Feature> = mutableSetOf()
    var platforms: PlatformConfig = PlatformConfig.DEFAULT
    var payments: PaymentConfig? = null
    private val integrations: MutableSet<Integration> = mutableSetOf()
    private val customizations: MutableMap<String, Any> = mutableMapOf()

    /**
     * Configure branding.
     */
    fun branding(builder: BrandingConfigBuilder.() -> Unit) {
        branding = brandingConfig(builder)
    }

    /**
     * Configure database.
     */
    fun database(builder: DatabaseConfigBuilder.() -> Unit) {
        database = databaseConfig(builder)
    }

    /**
     * Configure features.
     */
    fun features(builder: FeatureConfigBuilder.() -> Unit) {
        val featureBuilder = FeatureConfigBuilder()
        featureBuilder.builder()
        features.addAll(featureBuilder.enabledFeatures)
    }

    /**
     * Configure platforms.
     */
    fun platforms(builder: PlatformConfigBuilder.() -> Unit) {
        platforms = PlatformConfigBuilder().apply(builder).build()
    }

    /**
     * Configure payments.
     */
    fun payments(builder: PaymentConfigBuilder.() -> Unit) {
        payments = PaymentConfigBuilder().apply(builder).build()
    }

    /**
     * Add Firebase integration.
     */
    fun firebase(vararg services: FirebaseService) {
        integrations.add(Integration.Firebase(services.toSet()))
    }

    /**
     * Add AWS integration.
     */
    fun aws(vararg services: AWSService) {
        integrations.add(Integration.AWS(services.toSet()))
    }

    /**
     * Add custom property.
     */
    fun customize(key: String, value: Any) {
        customizations[key] = value
    }

    /**
     * Generate the project.
     *
     * @param outputPath Path where project will be generated
     */
    fun generate(outputPath: String = "./") {
        val config = build()
        TemplateGenerator.generate(config, outputPath)
    }

    fun build(): AppConfig {
        check(::template.isInitialized) { "Template must be specified" }

        return AppConfig(
            template = template,
            branding = branding,
            database = database,
            features = features.ifEmpty { template.features },
            platforms = platforms,
            payments = payments,
            integrations = integrations,
            customizations = customizations
        )
    }
}

/**
 * Builder for feature configuration.
 */
@AppConfigDsl
class FeatureConfigBuilder {
    internal val enabledFeatures: MutableSet<Feature> = mutableSetOf()

    /**
     * Enable a feature.
     */
    fun enable(feature: Feature) {
        enabledFeatures.add(feature)
    }

    /**
     * Disable a feature.
     */
    fun disable(feature: Feature) {
        enabledFeatures.remove(feature)
    }
}

/**
 * Builder for platform configuration.
 */
@AppConfigDsl
class PlatformConfigBuilder {
    private val targets: MutableSet<Platform> = mutableSetOf(Platform.ANDROID, Platform.IOS)
    var android: AndroidConfig = AndroidConfig.DEFAULT
    var ios: IOSConfig = IOSConfig.DEFAULT
    var desktop: DesktopConfig = DesktopConfig.DEFAULT

    /**
     * Configure Android platform.
     */
    fun android(builder: AndroidConfigBuilder.() -> Unit) {
        targets.add(Platform.ANDROID)
        android = AndroidConfigBuilder().apply(builder).build()
    }

    /**
     * Configure iOS platform.
     */
    fun ios(builder: IOSConfigBuilder.() -> Unit) {
        targets.add(Platform.IOS)
        ios = IOSConfigBuilder().apply(builder).build()
    }

    /**
     * Configure Desktop platform.
     */
    fun desktop(builder: DesktopConfigBuilder.() -> Unit) {
        targets.add(Platform.DESKTOP)
        desktop = DesktopConfigBuilder().apply(builder).build()
    }

    fun build(): PlatformConfig = PlatformConfig(
        targets = targets,
        android = android,
        ios = ios,
        desktop = desktop
    )
}

/**
 * Builder for Android configuration.
 */
@AppConfigDsl
class AndroidConfigBuilder {
    var minSdk: Int = 26
    var targetSdk: Int = 34
    var compileSdk: Int = 34

    fun build(): AndroidConfig = AndroidConfig(minSdk, targetSdk, compileSdk)
}

/**
 * Builder for iOS configuration.
 */
@AppConfigDsl
class IOSConfigBuilder {
    var minVersion: String = "15.0"
    var targets: Set<IOSTarget> = setOf(IOSTarget.ARM64, IOSTarget.SIMULATOR_ARM64, IOSTarget.X64)

    fun build(): IOSConfig = IOSConfig(minVersion, targets)
}

/**
 * Builder for Desktop configuration.
 */
@AppConfigDsl
class DesktopConfigBuilder {
    var targets: Set<JVMTarget> = setOf(JVMTarget.WINDOWS, JVMTarget.MAC, JVMTarget.LINUX)
    var jvmTarget: String = "17"

    fun build(): DesktopConfig = DesktopConfig(targets, jvmTarget)
}

/**
 * Builder for payment configuration.
 */
@AppConfigDsl
class PaymentConfigBuilder {
    lateinit var provider: PaymentProvider
    lateinit var apiKey: String
    var secretKey: String? = null
    var currency: String = "USD"
    var testMode: Boolean = true

    fun build(): PaymentConfig {
        check(::provider.isInitialized) { "Payment provider must be specified" }
        check(::apiKey.isInitialized) { "API key must be specified" }

        return PaymentConfig(provider, apiKey, secretKey, currency, testMode)
    }
}

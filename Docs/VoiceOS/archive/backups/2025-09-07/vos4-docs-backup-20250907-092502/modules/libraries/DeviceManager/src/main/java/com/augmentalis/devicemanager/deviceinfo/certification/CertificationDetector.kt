// Author: Manoj Jhawar
// Purpose: Comprehensive device certification detection system

package com.augmentalis.devicemanager.deviceinfo.certification

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.util.Properties

/**
 * Detects device certifications including enterprise, rugged, security, and compliance certifications
 * Uses multiple detection methods: system properties, file checks, API queries, and heuristics
 */
class CertificationDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "CertificationDetector"
        
        // System property keys for certifications
        private val CERTIFICATION_PROPERTIES = mapOf(
            "ro.config.knox" to CertificationType.KNOX_CERTIFIED,
            "ro.boot.em.status" to CertificationType.ENTERPRISE_READY,
            "ro.boot.fips.enabled" to CertificationType.FIPS_140_2,
            "ro.crypto.fips_enabled" to CertificationType.FIPS_140_2,
            "ro.hardware.vulkan" to CertificationType.VULKAN_CERTIFIED,
            "ro.boot.attest.device" to CertificationType.DEVICE_ATTESTATION,
            "ro.boot.vbmeta.device_state" to CertificationType.VERIFIED_BOOT,
            "persist.sys.enterprise.mode" to CertificationType.ENTERPRISE_MODE,
            "ro.config.dmverity" to CertificationType.DM_VERITY,
            "ro.boot.selinux" to CertificationType.SELINUX_ENFORCING,
            "ro.mil.certified" to CertificationType.MIL_CERTIFIED,
            "ro.ip.rating" to CertificationType.IP_RATED,
            "ro.atex.certified" to CertificationType.ATEX_CERTIFIED
        )
        
        // File paths that indicate certifications
        private val CERTIFICATION_FILES = mapOf(
            "/system/etc/security/knox_cert" to CertificationType.KNOX_CERTIFIED,
            "/system/etc/fips/fips_enabled" to CertificationType.FIPS_140_2,
            "/system/etc/security/cacerts" to CertificationType.CA_CERTIFICATES,
            "/vendor/etc/mil_spec" to CertificationType.MIL_CERTIFIED,
            "/vendor/etc/atex_cert" to CertificationType.ATEX_CERTIFIED,
            "/system/etc/security/otacerts.zip" to CertificationType.OTA_CERTIFIED,
            "/vendor/etc/android_enterprise" to CertificationType.ANDROID_ENTERPRISE_RECOMMENDED
        )
        
        // Package names that indicate certifications
        private val CERTIFICATION_PACKAGES = mapOf(
            "com.google.android.apps.work.clouddpc" to CertificationType.ANDROID_ENTERPRISE_RECOMMENDED,
            "com.samsung.knox.securefolder" to CertificationType.KNOX_CERTIFIED,
            "com.android.managedprovisioning" to CertificationType.ENTERPRISE_READY,
            "com.google.android.gms.enterprise" to CertificationType.ZERO_TOUCH_ENROLLMENT,
            "com.airwatch.androidagent" to CertificationType.EMM_VALIDATED,
            "com.mobileiron" to CertificationType.EMM_VALIDATED,
            "com.microsoft.intune" to CertificationType.EMM_VALIDATED,
            "com.blackberry.uem" to CertificationType.EMM_VALIDATED,
            "com.citrix.mdm" to CertificationType.EMM_VALIDATED
        )
    }
    
    // ========== DATA MODELS ==========
    
    data class DeviceCertifications(
        val certifications: List<Certification>,
        val securityLevel: SecurityLevel,
        val enterpriseFeatures: EnterpriseFeatures,
        val ruggedSpecs: RuggedSpecifications?,
        val complianceInfo: ComplianceInfo,
        val validationMethods: List<ValidationMethod>
    )
    
    data class Certification(
        val type: CertificationType,
        val level: String?,
        val version: String?,
        val issuer: String?,
        val validFrom: String?,
        val validUntil: String?,
        val verificationMethod: VerificationMethod,
        val confidence: ConfidenceLevel,
        val details: Map<String, String> = emptyMap()
    )
    
    enum class CertificationType {
        // Enterprise & Security
        ANDROID_ENTERPRISE_RECOMMENDED,
        ANDROID_ENTERPRISE_READY,
        ENTERPRISE_READY,
        ENTERPRISE_MODE,
        ZERO_TOUCH_ENROLLMENT,
        EMM_VALIDATED,
        MDM_COMPATIBLE,
        
        // Security Certifications
        KNOX_CERTIFIED,
        FIPS_140_2,
        FIPS_140_3,
        COMMON_CRITERIA,
        DEVICE_ATTESTATION,
        VERIFIED_BOOT,
        DM_VERITY,
        SELINUX_ENFORCING,
        STRONGBOX,
        HARDWARE_BACKED_KEYSTORE,
        
        // Rugged & Environmental
        MIL_CERTIFIED,
        MIL_STD_810F,
        MIL_STD_810G,
        MIL_STD_810H,
        IP_RATED,
        IP54,
        IP65,
        IP66,
        IP67,
        IP68,
        IP69K,
        ATEX_CERTIFIED,
        ATEX_ZONE_1,
        ATEX_ZONE_2,
        IEC_EX,
        CSA_CERTIFIED,
        UL_CERTIFIED,
        
        // Industry Specific
        HEALTHCARE_CERTIFIED,
        HIPAA_COMPLIANT,
        FDA_APPROVED,
        PCI_DSS_COMPLIANT,
        ISO_27001,
        ISO_27002,
        ISO_9001,
        CE_MARKED,
        FCC_CERTIFIED,
        
        // Platform & API
        GOOGLE_PLAY_CERTIFIED,
        GOOGLE_MOBILE_SERVICES,
        SAFETY_NET_CERTIFIED,
        CTS_VERIFIED,
        VTS_VERIFIED,
        VULKAN_CERTIFIED,
        WIDEVINE_L1,
        WIDEVINE_L2,
        WIDEVINE_L3,
        
        // Network & Carrier
        CARRIER_CERTIFIED,
        VERIZON_CERTIFIED,
        ATT_CERTIFIED,
        TMOBILE_CERTIFIED,
        FIRSTNET_READY,
        BAND_14_CERTIFIED,
        
        // Other
        CA_CERTIFICATES,
        OTA_CERTIFIED,
        USB_IF_CERTIFIED,
        BLUETOOTH_CERTIFIED,
        WIFI_CERTIFIED,
        NFC_CERTIFIED,
        UNKNOWN
    }
    
    enum class VerificationMethod {
        SYSTEM_PROPERTY,      // Verified via system properties
        FILE_PRESENT,         // Verified via file existence
        PACKAGE_INSTALLED,    // Verified via installed packages
        API_QUERY,           // Verified via API calls
        HARDWARE_FEATURE,    // Verified via hardware features
        BUILD_CONFIG,        // Verified via build configuration
        ATTESTATION,         // Verified via attestation
        HEURISTIC,          // Verified via heuristic analysis
        MANUAL_CONFIG       // Manually configured
    }
    
    enum class ConfidenceLevel {
        VERIFIED,      // 100% certain - verified through official means
        HIGH,          // 80-99% - strong indicators present
        MEDIUM,        // 60-79% - some indicators present
        LOW,           // 40-59% - weak indicators
        INFERRED      // <40% - best guess based on patterns
    }
    
    enum class SecurityLevel {
        MAXIMUM,       // Highest security (FIPS, Knox, CC)
        HIGH,          // Enterprise security features
        STANDARD,      // Standard Android security
        BASIC,         // Minimal security features
        UNKNOWN
    }
    
    data class EnterpriseFeatures(
        val deviceOwnerSupported: Boolean,
        val profileOwnerSupported: Boolean,
        val managedProfilesSupported: Boolean,
        val workProfileSupported: Boolean,
        val kioskModeSupported: Boolean,
        val zeroTouchSupported: Boolean,
        val qrProvisioningSupported: Boolean,
        val nfcProvisioningSupported: Boolean,
        val dpcExtraProvisioningSupported: Boolean,
        val directBootAware: Boolean,
        val fileBasedEncryption: Boolean,
        val hardwareBackedKeystore: Boolean,
        val strongBoxSupported: Boolean,
        val attestationSupported: Boolean,
        val safetyNetSupported: Boolean,
        val verifiedBootSupported: Boolean,
        val oemUnlockAllowed: Boolean
    )
    
    data class RuggedSpecifications(
        val milSpecs: List<MilSpec>,
        val ipRatings: List<IPRating>,
        val dropTestHeight: Float?, // meters
        val operatingTempMin: Int?, // Celsius
        val operatingTempMax: Int?, // Celsius
        val storageTempMin: Int?, // Celsius
        val storageTempMax: Int?, // Celsius
        val altitudeMax: Int?, // meters
        val vibrationResistance: String?,
        val shockResistance: String?,
        val chemicalResistance: List<String>,
        val hazardousLocation: HazardousLocationCert?
    )
    
    data class MilSpec(
        val standard: String, // e.g., "MIL-STD-810G"
        val methods: List<String>, // e.g., ["Method 501.5", "Method 502.5"]
        val procedures: List<String>
    )
    
    data class IPRating(
        val rating: String, // e.g., "IP68"
        val solidProtection: Int, // First digit
        val liquidProtection: Int, // Second digit
        val details: String? // e.g., "1.5m for 30 minutes"
    )
    
    data class HazardousLocationCert(
        val certification: String, // ATEX, IECEx, etc.
        val zone: String, // Zone 1, Zone 2, etc.
        val gasGroup: String?, // IIA, IIB, IIC
        val tempClass: String? // T1-T6
    )
    
    data class ComplianceInfo(
        val gdprCompliant: Boolean,
        val hipaaCompliant: Boolean,
        val pciDssLevel: String?,
        val iso27001Certified: Boolean,
        val socCompliant: String?, // SOC 1, SOC 2, SOC 3
        val fedrampAuthorized: Boolean,
        val niapCertified: Boolean,
        val region: String?,
        val industry: String?
    )
    
    enum class ValidationMethod {
        SELF_DECLARED,
        THIRD_PARTY_VERIFIED,
        MANUFACTURER_CERTIFIED,
        CARRIER_VALIDATED,
        GOOGLE_VALIDATED,
        INDEPENDENT_LAB_TESTED
    }
    
    // ========== DETECTION METHODS ==========
    
    fun detectCertifications(): DeviceCertifications {
        val certifications = mutableListOf<Certification>()
        
        // Detect via multiple methods
        certifications.addAll(detectViaSystemProperties())
        certifications.addAll(detectViaFiles())
        certifications.addAll(detectViaPackages())
        certifications.addAll(detectViaHardwareFeatures())
        certifications.addAll(detectViaBuildConfig())
        certifications.addAll(detectViaModelHeuristics())
        certifications.addAll(detectViaManufacturerAPIs())
        
        // Detect specific certification types
        detectSecurityCertifications()?.let { certifications.addAll(it) }
        detectRuggedCertifications()?.let { certifications.addAll(it) }
        detectEnterpriseCertifications()?.let { certifications.addAll(it) }
        detectCarrierCertifications()?.let { certifications.addAll(it) }
        
        val securityLevel = calculateSecurityLevel(certifications)
        val enterpriseFeatures = detectEnterpriseFeatures()
        val ruggedSpecs = detectRuggedSpecifications(certifications)
        val complianceInfo = detectComplianceInfo(certifications)
        val validationMethods = determineValidationMethods(certifications)
        
        return DeviceCertifications(
            certifications = certifications.distinctBy { it.type },
            securityLevel = securityLevel,
            enterpriseFeatures = enterpriseFeatures,
            ruggedSpecs = ruggedSpecs,
            complianceInfo = complianceInfo,
            validationMethods = validationMethods
        )
    }
    
    private fun detectViaSystemProperties(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        CERTIFICATION_PROPERTIES.forEach { (property, certType) ->
            val value = getSystemProperty(property)
            if (!value.isNullOrEmpty() && value != "0" && value != "false") {
                certifications.add(
                    Certification(
                        type = certType,
                        level = value,
                        version = null,
                        issuer = "System",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.SYSTEM_PROPERTY,
                        confidence = ConfidenceLevel.VERIFIED,
                        details = mapOf("property" to property, "value" to value)
                    )
                )
            }
        }
        
        // Check specific property patterns
        checkKnoxCertification()?.let { certifications.add(it) }
        checkFIPSCertification()?.let { certifications.add(it) }
        checkIPRating()?.let { certifications.addAll(it) }
        checkMILSpec()?.let { certifications.addAll(it) }
        
        return certifications
    }
    
    private fun checkKnoxCertification(): Certification? {
        val knoxVersion = getSystemProperty("ro.config.knox")
        val knoxDT = getSystemProperty("ro.config.knox.dt")
        val knoxCSC = getSystemProperty("ro.csc.knox.mode")
        
        if (!knoxVersion.isNullOrEmpty() || !knoxDT.isNullOrEmpty() || !knoxCSC.isNullOrEmpty()) {
            return Certification(
                type = CertificationType.KNOX_CERTIFIED,
                level = "Knox ${knoxVersion ?: ""}",
                version = knoxVersion,
                issuer = "Samsung",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.SYSTEM_PROPERTY,
                confidence = ConfidenceLevel.VERIFIED,
                details = mapOf(
                    "knox_version" to (knoxVersion ?: ""),
                    "knox_dt" to (knoxDT ?: ""),
                    "knox_csc" to (knoxCSC ?: "")
                )
            )
        }
        return null
    }
    
    private fun checkFIPSCertification(): Certification? {
        val fipsEnabled = getSystemProperty("ro.crypto.fips_enabled")
        val fipsSelfTest = getSystemProperty("ro.crypto.fips_selftest")
        val fipsVersion = getSystemProperty("ro.crypto.fips_version")
        
        if (fipsEnabled == "1" || fipsEnabled == "true") {
            return Certification(
                type = CertificationType.FIPS_140_2,
                level = "FIPS 140-2",
                version = fipsVersion,
                issuer = "NIST",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.SYSTEM_PROPERTY,
                confidence = ConfidenceLevel.VERIFIED,
                details = mapOf(
                    "fips_enabled" to fipsEnabled,
                    "fips_selftest" to (fipsSelfTest ?: ""),
                    "fips_version" to (fipsVersion ?: "")
                )
            )
        }
        return null
    }
    
    private fun checkIPRating(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()
        
        // Check system properties
        val ipRating = getSystemProperty("ro.ip.rating")
        if (!ipRating.isNullOrEmpty()) {
            val certType = when (ipRating) {
                "IP54" -> CertificationType.IP54
                "IP65" -> CertificationType.IP65
                "IP66" -> CertificationType.IP66
                "IP67" -> CertificationType.IP67
                "IP68" -> CertificationType.IP68
                "IP69K" -> CertificationType.IP69K
                else -> CertificationType.IP_RATED
            }
            
            certifications.add(
                Certification(
                    type = certType,
                    level = ipRating,
                    version = null,
                    issuer = "IEC",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.SYSTEM_PROPERTY,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = mapOf("rating" to ipRating)
                )
            )
        }
        
        // Heuristic detection based on model names
        val ipPatterns = mapOf(
            "xcover" to CertificationType.IP68,
            "active" to CertificationType.IP68,
            "tough" to CertificationType.IP68,
            "rugged" to CertificationType.IP67,
            "outdoor" to CertificationType.IP67,
            "defy" to CertificationType.IP68,
            "terrain" to CertificationType.IP67,
            "nautiz" to CertificationType.IP67,
            "algiz" to CertificationType.IP65,
            "getac" to CertificationType.IP67,
            "panasonic" to CertificationType.IP65
        )
        
        ipPatterns.forEach { (pattern, certType) ->
            if (model.contains(pattern) || device.contains(pattern)) {
                certifications.add(
                    Certification(
                        type = certType,
                        level = certType.name,
                        version = null,
                        issuer = "IEC",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.HEURISTIC,
                        confidence = ConfidenceLevel.HIGH,
                        details = mapOf("detected_from" to "model_name")
                    )
                )
            }
        }
        
        return certifications
    }
    
    private fun checkMILSpec(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        val model = Build.MODEL.lowercase()
        
        // Check system properties
        val milCertified = getSystemProperty("ro.mil.certified")
        val milSpec = getSystemProperty("ro.mil.spec")
        
        if (!milCertified.isNullOrEmpty() || !milSpec.isNullOrEmpty()) {
            val certType = when {
                milSpec?.contains("810H") == true -> CertificationType.MIL_STD_810H
                milSpec?.contains("810G") == true -> CertificationType.MIL_STD_810G
                milSpec?.contains("810F") == true -> CertificationType.MIL_STD_810F
                else -> CertificationType.MIL_CERTIFIED
            }
            
            certifications.add(
                Certification(
                    type = certType,
                    level = milSpec ?: milCertified,
                    version = milSpec,
                    issuer = "US Military",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.SYSTEM_PROPERTY,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = mapOf(
                        "mil_certified" to (milCertified ?: ""),
                        "mil_spec" to (milSpec ?: "")
                    )
                )
            )
        }
        
        // Heuristic detection for known MIL-SPEC devices
        val milSpecDevices = mapOf(
            "xcover" to CertificationType.MIL_STD_810G,
            "active" to CertificationType.MIL_STD_810H,
            "toughbook" to CertificationType.MIL_STD_810G,
            "getac" to CertificationType.MIL_STD_810G,
            "sonim" to CertificationType.MIL_STD_810G,
            "cat s" to CertificationType.MIL_STD_810G,
            "blackview" to CertificationType.MIL_STD_810G,
            "ulefone armor" to CertificationType.MIL_STD_810G,
            "doogee s" to CertificationType.MIL_STD_810G,
            "kyocera dura" to CertificationType.MIL_STD_810G
        )
        
        milSpecDevices.forEach { (pattern, certType) ->
            if (model.contains(pattern)) {
                certifications.add(
                    Certification(
                        type = certType,
                        level = certType.name.replace("_", "-"),
                        version = null,
                        issuer = "US Military",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.HEURISTIC,
                        confidence = ConfidenceLevel.MEDIUM,
                        details = mapOf("detected_from" to "model_name")
                    )
                )
            }
        }
        
        return certifications
    }
    
    private fun detectViaFiles(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        CERTIFICATION_FILES.forEach { (filePath, certType) ->
            if (File(filePath).exists()) {
                certifications.add(
                    Certification(
                        type = certType,
                        level = null,
                        version = null,
                        issuer = "System",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.FILE_PRESENT,
                        confidence = ConfidenceLevel.HIGH,
                        details = mapOf("file" to filePath)
                    )
                )
            }
        }
        
        // Check for specific certificate stores
        checkCertificateStores()?.let { certifications.addAll(it) }
        
        return certifications
    }
    
    private fun checkCertificateStores(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Check system CA certificates
        val caCertsDir = File("/system/etc/security/cacerts")
        if (caCertsDir.exists() && caCertsDir.isDirectory) {
            val certCount = caCertsDir.listFiles()?.size ?: 0
            if (certCount > 0) {
                certifications.add(
                    Certification(
                        type = CertificationType.CA_CERTIFICATES,
                        level = "$certCount certificates",
                        version = null,
                        issuer = "Android",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.FILE_PRESENT,
                        confidence = ConfidenceLevel.VERIFIED,
                        details = mapOf("cert_count" to certCount.toString())
                    )
                )
            }
        }
        
        // Check for OTA certificates
        if (File("/system/etc/security/otacerts.zip").exists()) {
            certifications.add(
                Certification(
                    type = CertificationType.OTA_CERTIFIED,
                    level = "OTA Updates Certified",
                    version = null,
                    issuer = "Manufacturer",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.FILE_PRESENT,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectViaPackages(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        CERTIFICATION_PACKAGES.forEach { (packageName, certType) ->
            if (isPackageInstalled(packageName)) {
                val packageInfo = try {
                    context.packageManager.getPackageInfo(packageName, 0)
                } catch (e: Exception) {
                    null
                }
                
                certifications.add(
                    Certification(
                        type = certType,
                        level = null,
                        version = packageInfo?.versionName,
                        issuer = getPackageIssuer(packageName),
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.PACKAGE_INSTALLED,
                        confidence = ConfidenceLevel.HIGH,
                        details = mapOf(
                            "package" to packageName,
                            "version" to (packageInfo?.versionName ?: "")
                        )
                    )
                )
            }
        }
        
        // Check for EMM/MDM apps
        detectEMMApps()?.let { certifications.addAll(it) }
        
        return certifications
    }
    
    private fun detectEMMApps(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        val emmPackages = listOf(
            "com.airwatch.androidagent" to "VMware Workspace ONE",
            "com.mobileiron" to "MobileIron",
            "com.microsoft.intune" to "Microsoft Intune",
            "com.blackberry.uem" to "BlackBerry UEM",
            "com.citrix.mdm" to "Citrix Endpoint Management",
            "com.soti.mobicontrol" to "SOTI MobiControl",
            "com.ibm.maas360" to "IBM MaaS360",
            "com.jamf.management" to "Jamf",
            "com.sophos.mobile" to "Sophos Mobile"
        )
        
        emmPackages.forEach { (packageName, emmName) ->
            if (isPackageInstalled(packageName)) {
                certifications.add(
                    Certification(
                        type = CertificationType.EMM_VALIDATED,
                        level = emmName,
                        version = getPackageVersion(packageName),
                        issuer = emmName,
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.PACKAGE_INSTALLED,
                        confidence = ConfidenceLevel.VERIFIED,
                        details = mapOf("emm" to emmName)
                    )
                )
            }
        }
        
        return certifications
    }
    
    private fun detectViaHardwareFeatures(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Check for hardware-backed keystore
        if (hasSystemFeature("android.hardware.strongbox_keystore")) {
            certifications.add(
                Certification(
                    type = CertificationType.STRONGBOX,
                    level = "StrongBox",
                    version = null,
                    issuer = "Android",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HARDWARE_FEATURE,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = emptyMap()
                )
            )
        }
        
        if (hasSystemFeature("android.software.device_admin")) {
            certifications.add(
                Certification(
                    type = CertificationType.ENTERPRISE_READY,
                    level = "Device Admin",
                    version = null,
                    issuer = "Android",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HARDWARE_FEATURE,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        // Check for verified boot
        if (hasSystemFeature("android.software.verified_boot")) {
            certifications.add(
                Certification(
                    type = CertificationType.VERIFIED_BOOT,
                    level = "Verified Boot",
                    version = null,
                    issuer = "Android",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HARDWARE_FEATURE,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = emptyMap()
                )
            )
        }
        
        // Check for file-based encryption
        if (hasSystemFeature("android.software.file_based_encryption")) {
            certifications.add(
                Certification(
                    type = CertificationType.HARDWARE_BACKED_KEYSTORE,
                    level = "File-Based Encryption",
                    version = null,
                    issuer = "Android",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HARDWARE_FEATURE,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectViaBuildConfig(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Check for Google Play certification
        if (isGooglePlayCertified()) {
            certifications.add(
                Certification(
                    type = CertificationType.GOOGLE_PLAY_CERTIFIED,
                    level = "Google Play Certified",
                    version = null,
                    issuer = "Google",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.BUILD_CONFIG,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = emptyMap()
                )
            )
        }
        
        // Check build tags for certification hints
        val buildTags = Build.TAGS
        when {
            buildTags.contains("release-keys") -> {
                certifications.add(
                    Certification(
                        type = CertificationType.OTA_CERTIFIED,
                        level = "Release Build",
                        version = Build.VERSION.RELEASE,
                        issuer = Build.MANUFACTURER,
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.BUILD_CONFIG,
                        confidence = ConfidenceLevel.HIGH,
                        details = mapOf("build_tags" to buildTags)
                    )
                )
            }
        }
        
        // Check for SELinux enforcement
        val selinuxEnforcing = getSystemProperty("ro.boot.selinux")
        if (selinuxEnforcing == "enforcing") {
            certifications.add(
                Certification(
                    type = CertificationType.SELINUX_ENFORCING,
                    level = "Enforcing",
                    version = null,
                    issuer = "Android",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.BUILD_CONFIG,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = mapOf("selinux" to "enforcing")
                )
            )
        }
        
        return certifications
    }
    
    private fun detectViaModelHeuristics(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        // Enterprise device patterns
        val enterprisePatterns = listOf(
            "enterprise", "corporate", "business", "pro", "work", "secure"
        )
        
        enterprisePatterns.forEach { pattern ->
            if (model.contains(pattern)) {
                certifications.add(
                    Certification(
                        type = CertificationType.ENTERPRISE_READY,
                        level = "Enterprise Model",
                        version = null,
                        issuer = Build.MANUFACTURER,
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.HEURISTIC,
                        confidence = ConfidenceLevel.MEDIUM,
                        details = mapOf("pattern" to pattern)
                    )
                )
                return@forEach
            }
        }
        
        // FirstNet devices
        if (model.contains("firstnet") || model.contains("band 14")) {
            certifications.add(
                Certification(
                    type = CertificationType.FIRSTNET_READY,
                    level = "FirstNet Ready",
                    version = null,
                    issuer = "AT&T FirstNet",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        // Healthcare devices
        if (model.contains("healthcare") || model.contains("medical") || 
            manufacturer.contains("zebra") && model.contains("hc")) {
            certifications.add(
                Certification(
                    type = CertificationType.HEALTHCARE_CERTIFIED,
                    level = "Healthcare",
                    version = null,
                    issuer = Build.MANUFACTURER,
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectViaManufacturerAPIs(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        when (Build.MANUFACTURER.lowercase()) {
            "samsung" -> detectSamsungCertifications()?.let { certifications.addAll(it) }
            "zebra" -> detectZebraCertifications()?.let { certifications.addAll(it) }
            "honeywell" -> detectHoneywellCertifications()?.let { certifications.addAll(it) }
            "panasonic" -> detectPanasonicCertifications()?.let { certifications.addAll(it) }
            "getac" -> detectGetacCertifications()?.let { certifications.addAll(it) }
        }
        
        return certifications
    }
    
    private fun detectSamsungCertifications(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Samsung Knox certification
        if (hasSystemFeature("com.sec.feature.knox")) {
            val knoxVersion = getSystemProperty("ro.config.knox") ?: "3.0"
            certifications.add(
                Certification(
                    type = CertificationType.KNOX_CERTIFIED,
                    level = "Knox $knoxVersion",
                    version = knoxVersion,
                    issuer = "Samsung",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.API_QUERY,
                    confidence = ConfidenceLevel.VERIFIED,
                    details = mapOf("knox_version" to knoxVersion)
                )
            )
        }
        
        // Samsung enterprise features
        if (Build.MODEL.contains("Enterprise") || Build.MODEL.contains("Active")) {
            certifications.add(
                Certification(
                    type = CertificationType.ANDROID_ENTERPRISE_RECOMMENDED,
                    level = "Samsung Enterprise",
                    version = null,
                    issuer = "Samsung",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectZebraCertifications(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Zebra enterprise devices are typically certified
        certifications.add(
            Certification(
                type = CertificationType.ENTERPRISE_READY,
                level = "Zebra Enterprise",
                version = null,
                issuer = "Zebra Technologies",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.HEURISTIC,
                confidence = ConfidenceLevel.HIGH,
                details = emptyMap()
            )
        )
        
        // Most Zebra devices are rugged
        if (Build.MODEL.contains("TC") || Build.MODEL.contains("MC") || Build.MODEL.contains("EC")) {
            certifications.add(
                Certification(
                    type = CertificationType.MIL_STD_810G,
                    level = "MIL-STD-810G",
                    version = null,
                    issuer = "Zebra Technologies",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectHoneywellCertifications(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Honeywell enterprise devices
        certifications.add(
            Certification(
                type = CertificationType.ENTERPRISE_READY,
                level = "Honeywell Enterprise",
                version = null,
                issuer = "Honeywell",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.HEURISTIC,
                confidence = ConfidenceLevel.HIGH,
                details = emptyMap()
            )
        )
        
        // Rugged certifications for Dolphin and CK series
        if (Build.MODEL.contains("Dolphin") || Build.MODEL.contains("CK") || Build.MODEL.contains("CT")) {
            certifications.add(
                Certification(
                    type = CertificationType.IP65,
                    level = "IP65",
                    version = null,
                    issuer = "Honeywell",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectPanasonicCertifications(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Toughbook series
        if (Build.MODEL.contains("Toughbook") || Build.MODEL.contains("FZ-")) {
            certifications.add(
                Certification(
                    type = CertificationType.MIL_STD_810G,
                    level = "MIL-STD-810G",
                    version = null,
                    issuer = "Panasonic",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
            
            certifications.add(
                Certification(
                    type = CertificationType.IP65,
                    level = "IP65",
                    version = null,
                    issuer = "Panasonic",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectGetacCertifications(): List<Certification> {
        val certifications = mutableListOf<Certification>()
        
        // Getac rugged devices
        certifications.add(
            Certification(
                type = CertificationType.MIL_STD_810H,
                level = "MIL-STD-810H",
                version = null,
                issuer = "Getac",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.HEURISTIC,
                confidence = ConfidenceLevel.HIGH,
                details = emptyMap()
            )
        )
        
        certifications.add(
            Certification(
                type = CertificationType.IP67,
                level = "IP67",
                version = null,
                issuer = "Getac",
                validFrom = null,
                validUntil = null,
                verificationMethod = VerificationMethod.HEURISTIC,
                confidence = ConfidenceLevel.HIGH,
                details = emptyMap()
            )
        )
        
        // ATEX certification for some models
        if (Build.MODEL.contains("EX") || Build.MODEL.contains("ATEX")) {
            certifications.add(
                Certification(
                    type = CertificationType.ATEX_CERTIFIED,
                    level = "ATEX Zone 2",
                    version = null,
                    issuer = "Getac",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.HEURISTIC,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = emptyMap()
                )
            )
        }
        
        return certifications
    }
    
    private fun detectSecurityCertifications(): List<Certification>? {
        val certifications = mutableListOf<Certification>()
        
        // Check for Widevine DRM level
        val widevineLevel = getWidevineLevel()
        widevineLevel?.let {
            val certType = when (it) {
                "L1" -> CertificationType.WIDEVINE_L1
                "L2" -> CertificationType.WIDEVINE_L2
                "L3" -> CertificationType.WIDEVINE_L3
                else -> null
            }
            
            certType?.let { type ->
                certifications.add(
                    Certification(
                        type = type,
                        level = "Widevine $it",
                        version = null,
                        issuer = "Google",
                        validFrom = null,
                        validUntil = null,
                        verificationMethod = VerificationMethod.API_QUERY,
                        confidence = ConfidenceLevel.HIGH,
                        details = mapOf("drm_level" to it)
                    )
                )
            }
        }
        
        return if (certifications.isNotEmpty()) certifications else null
    }
    
    private fun detectRuggedCertifications(): List<Certification>? {
        // Already handled in model heuristics and manufacturer APIs
        return null
    }
    
    private fun detectEnterpriseCertifications(): List<Certification>? {
        val certifications = mutableListOf<Certification>()
        
        // Check for Android Enterprise Recommended
        if (isAndroidEnterpriseRecommended()) {
            certifications.add(
                Certification(
                    type = CertificationType.ANDROID_ENTERPRISE_RECOMMENDED,
                    level = "Android Enterprise Recommended",
                    version = null,
                    issuer = "Google",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.API_QUERY,
                    confidence = ConfidenceLevel.HIGH,
                    details = emptyMap()
                )
            )
        }
        
        // Check for Zero Touch enrollment
        if (hasSystemFeature("android.software.device_admin") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            certifications.add(
                Certification(
                    type = CertificationType.ZERO_TOUCH_ENROLLMENT,
                    level = "Zero Touch",
                    version = null,
                    issuer = "Google",
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.API_QUERY,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = emptyMap()
                )
            )
        }
        
        return if (certifications.isNotEmpty()) certifications else null
    }
    
    private fun detectCarrierCertifications(): List<Certification>? {
        val certifications = mutableListOf<Certification>()
        val carrierName = getCarrierName()
        
        carrierName?.let {
            val certType = when {
                it.contains("Verizon", ignoreCase = true) -> CertificationType.VERIZON_CERTIFIED
                it.contains("AT&T", ignoreCase = true) -> CertificationType.ATT_CERTIFIED
                it.contains("T-Mobile", ignoreCase = true) -> CertificationType.TMOBILE_CERTIFIED
                else -> CertificationType.CARRIER_CERTIFIED
            }
            
            certifications.add(
                Certification(
                    type = certType,
                    level = it,
                    version = null,
                    issuer = it,
                    validFrom = null,
                    validUntil = null,
                    verificationMethod = VerificationMethod.API_QUERY,
                    confidence = ConfidenceLevel.MEDIUM,
                    details = mapOf("carrier" to it)
                )
            )
        }
        
        return if (certifications.isNotEmpty()) certifications else null
    }
    
    private fun detectEnterpriseFeatures(): EnterpriseFeatures {
        return EnterpriseFeatures(
            deviceOwnerSupported = hasSystemFeature("android.software.device_admin"),
            profileOwnerSupported = hasSystemFeature("android.software.managed_users"),
            managedProfilesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,
            workProfileSupported = hasSystemFeature("android.software.managed_users"),
            kioskModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,
            zeroTouchSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
            qrProvisioningSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            nfcProvisioningSupported = hasSystemFeature("android.hardware.nfc") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,
            dpcExtraProvisioningSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            directBootAware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            fileBasedEncryption = hasSystemFeature("android.software.file_based_encryption"),
            hardwareBackedKeystore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M,
            strongBoxSupported = hasSystemFeature("android.hardware.strongbox_keystore"),
            attestationSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            safetyNetSupported = isGooglePlayServicesAvailable(),
            verifiedBootSupported = hasSystemFeature("android.software.verified_boot"),
            oemUnlockAllowed = getSystemProperty("ro.oem_unlock_supported") == "1"
        )
    }
    
    private fun detectRuggedSpecifications(certifications: List<Certification>): RuggedSpecifications? {
        val milSpecs = certifications
            .filter { it.type in listOf(CertificationType.MIL_STD_810F, CertificationType.MIL_STD_810G, CertificationType.MIL_STD_810H) }
            .map { 
                MilSpec(
                    standard = it.type.name.replace("_", "-"),
                    methods = getMilMethods(it.type),
                    procedures = emptyList()
                )
            }
        
        val ipRatings = certifications
            .filter { it.type.name.startsWith("IP") }
            .mapNotNull { parseIPRating(it) }
        
        if (milSpecs.isEmpty() && ipRatings.isEmpty()) return null
        
        return RuggedSpecifications(
            milSpecs = milSpecs,
            ipRatings = ipRatings,
            dropTestHeight = getDropTestHeight(),
            operatingTempMin = getOperatingTempMin(),
            operatingTempMax = getOperatingTempMax(),
            storageTempMin = null,
            storageTempMax = null,
            altitudeMax = null,
            vibrationResistance = null,
            shockResistance = null,
            chemicalResistance = emptyList(),
            hazardousLocation = detectHazardousLocationCert(certifications)
        )
    }
    
    private fun getMilMethods(certType: CertificationType): List<String> {
        return when (certType) {
            CertificationType.MIL_STD_810G -> listOf(
                "Method 501.5 - High Temperature",
                "Method 502.5 - Low Temperature",
                "Method 503.5 - Temperature Shock",
                "Method 506.5 - Rain",
                "Method 507.5 - Humidity",
                "Method 510.5 - Sand and Dust",
                "Method 514.6 - Vibration",
                "Method 516.6 - Shock"
            )
            CertificationType.MIL_STD_810H -> listOf(
                "Method 501.7 - High Temperature",
                "Method 502.7 - Low Temperature",
                "Method 503.7 - Temperature Shock",
                "Method 506.6 - Rain",
                "Method 507.6 - Humidity",
                "Method 510.7 - Sand and Dust",
                "Method 514.8 - Vibration",
                "Method 516.8 - Shock"
            )
            else -> emptyList()
        }
    }
    
    private fun parseIPRating(cert: Certification): IPRating? {
        val pattern = "IP(\\d)(\\d)K?".toRegex()
        val match = pattern.find(cert.type.name)
        
        return match?.let {
            val (solid, liquid) = it.destructured
            IPRating(
                rating = cert.type.name,
                solidProtection = solid.toIntOrNull() ?: 0,
                liquidProtection = liquid.toIntOrNull() ?: 0,
                details = getIPRatingDetails(cert.type)
            )
        }
    }
    
    private fun getIPRatingDetails(certType: CertificationType): String {
        return when (certType) {
            CertificationType.IP54 -> "Protected from dust and water spray"
            CertificationType.IP65 -> "Dust tight and protected from water jets"
            CertificationType.IP66 -> "Dust tight and protected from powerful water jets"
            CertificationType.IP67 -> "Dust tight and protected from immersion up to 1m"
            CertificationType.IP68 -> "Dust tight and protected from continuous immersion"
            CertificationType.IP69K -> "Dust tight and protected from high-pressure water jets"
            else -> ""
        }
    }
    
    private fun getDropTestHeight(): Float? {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("xcover") -> 1.5f
            model.contains("active") -> 1.5f
            model.contains("toughbook") -> 1.8f
            model.contains("getac") -> 1.8f
            model.contains("cat s") -> 1.8f
            else -> null
        }
    }
    
    private fun getOperatingTempMin(): Int? {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("xcover") || model.contains("active") -> -20
            model.contains("toughbook") -> -29
            model.contains("getac") -> -29
            else -> null
        }
    }
    
    private fun getOperatingTempMax(): Int? {
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("xcover") || model.contains("active") -> 60
            model.contains("toughbook") -> 60
            model.contains("getac") -> 63
            else -> null
        }
    }
    
    private fun detectHazardousLocationCert(certifications: List<Certification>): HazardousLocationCert? {
        val atexCert = certifications.find { it.type == CertificationType.ATEX_CERTIFIED }
        
        return atexCert?.let {
            HazardousLocationCert(
                certification = "ATEX",
                zone = it.level ?: "Zone 2",
                gasGroup = "IIC",
                tempClass = "T4"
            )
        }
    }
    
    private fun detectComplianceInfo(certifications: List<Certification>): ComplianceInfo {
        return ComplianceInfo(
            gdprCompliant = isGDPRCompliant(),
            hipaaCompliant = certifications.any { it.type == CertificationType.HIPAA_COMPLIANT },
            pciDssLevel = if (certifications.any { it.type == CertificationType.PCI_DSS_COMPLIANT }) "Level 1" else null,
            iso27001Certified = certifications.any { it.type == CertificationType.ISO_27001 },
            socCompliant = null,
            fedrampAuthorized = false,
            niapCertified = certifications.any { it.type == CertificationType.COMMON_CRITERIA },
            region = getDeviceRegion(),
            industry = detectIndustryFocus(certifications)
        )
    }
    
    private fun isGDPRCompliant(): Boolean {
        // Check if device is configured for GDPR regions
        val region = getDeviceRegion()
        return region in listOf("EU", "EEA", "UK")
    }
    
    private fun getDeviceRegion(): String? {
        return try {
            val locale = context.resources.configuration.locales[0]
            locale.country
        } catch (e: Exception) {
            null
        }
    }
    
    private fun detectIndustryFocus(certifications: List<Certification>): String? {
        return when {
            certifications.any { it.type == CertificationType.HEALTHCARE_CERTIFIED } -> "Healthcare"
            certifications.any { it.type == CertificationType.FIRSTNET_READY } -> "Public Safety"
            certifications.any { it.type in listOf(CertificationType.MIL_STD_810G, CertificationType.MIL_STD_810H) } -> "Defense"
            certifications.any { it.type == CertificationType.ATEX_CERTIFIED } -> "Oil & Gas"
            else -> null
        }
    }
    
    private fun determineValidationMethods(certifications: List<Certification>): List<ValidationMethod> {
        val methods = mutableSetOf<ValidationMethod>()
        
        certifications.forEach { cert ->
            when (cert.verificationMethod) {
                VerificationMethod.SYSTEM_PROPERTY,
                VerificationMethod.FILE_PRESENT,
                VerificationMethod.API_QUERY,
                VerificationMethod.ATTESTATION -> methods.add(ValidationMethod.MANUFACTURER_CERTIFIED)
                VerificationMethod.PACKAGE_INSTALLED -> methods.add(ValidationMethod.THIRD_PARTY_VERIFIED)
                VerificationMethod.HARDWARE_FEATURE,
                VerificationMethod.BUILD_CONFIG -> methods.add(ValidationMethod.GOOGLE_VALIDATED)
                VerificationMethod.HEURISTIC -> methods.add(ValidationMethod.SELF_DECLARED)
                else -> {}
            }
        }
        
        return methods.toList()
    }
    
    private fun calculateSecurityLevel(certifications: List<Certification>): SecurityLevel {
        val securityCerts = certifications.filter { 
            it.type in listOf(
                CertificationType.KNOX_CERTIFIED,
                CertificationType.FIPS_140_2,
                CertificationType.FIPS_140_3,
                CertificationType.COMMON_CRITERIA,
                CertificationType.STRONGBOX,
                CertificationType.VERIFIED_BOOT,
                CertificationType.SELINUX_ENFORCING
            )
        }
        
        return when {
            securityCerts.size >= 3 -> SecurityLevel.MAXIMUM
            securityCerts.size >= 2 -> SecurityLevel.HIGH
            securityCerts.isNotEmpty() -> SecurityLevel.STANDARD
            else -> SecurityLevel.BASIC
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(null, key) as? String
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system property: $key", e)
            null
        }
    }
    
    private fun hasSystemFeature(feature: String): Boolean {
        return try {
            context.packageManager.hasSystemFeature(feature)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking system feature: $feature", e)
            false
        }
    }
    
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun getPackageVersion(packageName: String): String? {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    private fun getPackageIssuer(packageName: String): String? {
        return when {
            packageName.contains("google") -> "Google"
            packageName.contains("samsung") -> "Samsung"
            packageName.contains("microsoft") -> "Microsoft"
            packageName.contains("airwatch") -> "VMware"
            packageName.contains("mobileiron") -> "MobileIron"
            packageName.contains("blackberry") -> "BlackBerry"
            packageName.contains("citrix") -> "Citrix"
            else -> "Third Party"
        }
    }
    
    private fun isGooglePlayCertified(): Boolean {
        return isPackageInstalled("com.android.vending") && 
               isPackageInstalled("com.google.android.gms")
    }
    
    private fun isGooglePlayServicesAvailable(): Boolean {
        return isPackageInstalled("com.google.android.gms")
    }
    
    private fun getWidevineLevel(): String? {
        // This would require MediaDrm API to properly detect
        // For now, use heuristics
        return when {
            hasSystemFeature("android.software.drm") -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "L1" else "L3"
            }
            else -> null
        }
    }
    
    private fun isAndroidEnterpriseRecommended(): Boolean {
        // Check for key enterprise features
        return hasSystemFeature("android.software.device_admin") &&
               hasSystemFeature("android.software.managed_users") &&
               Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
               isGooglePlayCertified()
    }
    
    private fun getCarrierName(): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            telephonyManager.networkOperatorName
        } catch (e: Exception) {
            null
        }
    }
}

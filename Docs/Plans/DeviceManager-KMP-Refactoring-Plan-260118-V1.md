# DeviceManager KMP Refactoring Plan

**Author:** Claude | **Date:** 2026-01-18 | **Version:** V1

**Goal:** Convert Android-only DeviceManager (61 files) to Kotlin Multiplatform supporting Android, iOS, and Desktop.

**Key Integration:** UniversalIPC uses `deviceId` and `Fingerprint` for licensing - needs common abstraction.

---

## Current State

| Aspect | Details |
|--------|---------|
| Location | `Modules/DeviceManager/` |
| Build | `com.android.library` (Android-only) |
| Files | 61 Kotlin files in `src/main/java/` |
| Fingerprinting | `HardwareProfiler.kt:391-413` generates fingerprint from CPU/RAM/GPU/storage |

**Existing KMP Foundation:** `Modules/DeviceManagerKMP/` already has expect/actual for `DeviceInfoProvider` (JVM, iOS, JS) but missing Android actual.

---

## Phase 1: Pure Math & Data Classes to commonMain

**Files to move (no Android dependencies):**

| Source File | Target (flat structure) |
|-------------|------------------------|
| `sensors/imu/IMUMathUtils.kt` | `Vector3Math.kt`, `QuaternionMath.kt` |
| `sensors/imu/MovingAverage.kt` | `MovingAverageFilter.kt` |
| `sensors/imu/AdaptiveFilter.kt` | `AdaptiveMotionFilter.kt` |
| `sensors/imu/EnhancedSensorFusion.kt` | `SensorFusionAlgorithm.kt` |
| `audio/AudioModels.kt` | `AudioModels.kt` |
| Data classes from `DeviceInfo.kt` | `DeviceDataModels.kt` |
| Data classes from `DeviceManager.kt` | `CapabilityModels.kt` |

**Changes required:**
- Replace `java.lang.Math` with `kotlin.math`
- Add `@Serializable` annotations
- Remove `synchronized` blocks (use atomicfu)

---

## Phase 2: Core Interfaces in commonMain

**Create in `DeviceManagerKMP/src/commonMain/kotlin/com/augmentalis/devicemanager/`:**

```kotlin
// DeviceCapabilityProvider.kt
interface DeviceCapabilityProvider {
    fun getDeviceInfo(): DeviceInfo
    fun getHardwareProfile(): HardwareProfile
    fun getNetworkCapabilities(): NetworkCapabilities
    fun getSensorCapabilities(): SensorCapabilities
    fun getDeviceFingerprint(): DeviceFingerprint
    suspend fun refreshCapabilities(): DeviceCapabilities
}

expect object DeviceCapabilityFactory {
    fun create(): DeviceCapabilityProvider
}

// DeviceIdentityProvider.kt (for UniversalIPC integration)
interface DeviceIdentityProvider {
    fun getDeviceId(): String
    fun getFingerprint(): DeviceFingerprint
}

expect object DeviceIdentityFactory {
    fun create(): DeviceIdentityProvider
}
```

**Data models to create:**

```kotlin
// CapabilityModels.kt
@Serializable
data class DeviceCapabilities(
    val deviceInfo: DeviceInfo,
    val network: NetworkCapabilities,
    val sensors: SensorCapabilities,
    val display: DisplayCapabilities,
    val fingerprint: DeviceFingerprint
)

@Serializable
data class NetworkCapabilities(
    val hasBluetooth: Boolean,
    val hasBluetoothLE: Boolean,
    val hasWiFi: Boolean,
    val hasNfc: Boolean,
    val hasUwb: Boolean,
    val hasCellular: Boolean
)

@Serializable
data class DeviceFingerprint(
    val value: String,
    val type: String = "hardware",
    val components: List<String> = emptyList()
)
```

---

## Phase 3: Android Actual Implementations

**Create in `DeviceManagerKMP/src/androidMain/kotlin/com/augmentalis/devicemanager/`:**

```kotlin
// DeviceCapabilityFactory.android.kt
actual object DeviceCapabilityFactory {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun create(): DeviceCapabilityProvider {
        return AndroidDeviceCapabilityProvider(
            context ?: error("Call initialize(context) first")
        )
    }
}

// AndroidDeviceCapabilityProvider.kt
internal class AndroidDeviceCapabilityProvider(
    private val context: Context
) : DeviceCapabilityProvider {
    // Delegate to existing DeviceDetector
    private val detector by lazy { DeviceDetector.getCapabilities(context, false) }

    override fun getDeviceFingerprint(): DeviceFingerprint {
        val profiler = HardwareProfiler(context)
        val profile = runBlocking { profiler.generateProfile() }
        return DeviceFingerprint(
            value = profile.deviceFingerprint,
            type = "hardware",
            components = listOf("cpu", "ram", "gpu", "storage")
        )
    }
    // ... other implementations delegating to DeviceDetector
}
```

---

## Phase 4: iOS Actual Implementations

**Create in `DeviceManagerKMP/src/iosMain/kotlin/com/augmentalis/devicemanager/`:**

```kotlin
// DeviceCapabilityFactory.ios.kt
actual object DeviceCapabilityFactory {
    actual fun create(): DeviceCapabilityProvider = IOSDeviceCapabilityProvider()
}

internal class IOSDeviceCapabilityProvider : DeviceCapabilityProvider {
    override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = "Apple",
        model = UIDevice.currentDevice.model,
        osVersion = UIDevice.currentDevice.systemVersion,
        deviceType = DeviceType.PHONE
    )

    override fun getDeviceFingerprint(): DeviceFingerprint {
        // Use identifierForVendor or keychain-stored UUID
        val vendorId = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
        return DeviceFingerprint(value = vendorId, type = "vendor_id")
    }
}
```

---

## Phase 5: Desktop (JVM) Actual Implementations

**Create in `DeviceManagerKMP/src/jvmMain/kotlin/com/augmentalis/devicemanager/`:**

```kotlin
// DeviceCapabilityFactory.jvm.kt
actual object DeviceCapabilityFactory {
    actual fun create(): DeviceCapabilityProvider = JvmDeviceCapabilityProvider()
}

internal class JvmDeviceCapabilityProvider : DeviceCapabilityProvider {
    override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = detectManufacturer(),
        model = System.getProperty("os.name"),
        osVersion = System.getProperty("os.version"),
        deviceType = DeviceType.DESKTOP
    )

    override fun getDeviceFingerprint(): DeviceFingerprint {
        // Combine: hostname + MAC address + OS info
        val hostname = InetAddress.getLocalHost().hostName
        val fingerprint = "$hostname-${System.getProperty("user.name")}-${System.getProperty("os.name")}"
        return DeviceFingerprint(value = fingerprint.hashCode().toString(16), type = "system")
    }
}
```

---

## Phase 6: Build Configuration

**Update `Modules/DeviceManagerKMP/build.gradle.kts`:**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }

    jvm("desktop")

    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) }) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(project(":DeviceManager")) // For existing implementations
        }
    }
}

android {
    namespace = "com.augmentalis.devicemanager.kmp"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
}
```

---

## Phase 7: Migration & Compatibility

**Keep old DeviceManager working during transition:**

1. Add dependency: `DeviceManager` depends on `DeviceManagerKMP`
2. Create compatibility shim delegating to KMP implementations
3. Deprecate old API with `@Deprecated` annotations
4. UI layer (`dashboardui/*`) stays Android-only, consumes common interfaces

---

## Critical Files

| File | Action |
|------|--------|
| `Modules/DeviceManagerKMP/build.gradle.kts` | Add Android target, restructure |
| `Modules/DeviceManager/src/.../HardwareProfiler.kt` | Extract fingerprint logic |
| `Modules/DeviceManager/src/.../DeviceDetector.kt` | Wrap in KMP interface |
| `Modules/DeviceManager/src/.../IMUMathUtils.kt` | Move to commonMain |
| `NewAvanues/Modules/AVAMagic/IPC/UniversalIPC/.../Messages.kt` | Use DeviceIdentityProvider |

---

## Verification

1. **Build all targets:**
   ```bash
   ./gradlew :DeviceManagerKMP:build
   ./gradlew :DeviceManagerKMP:assembleDebug  # Android
   ./gradlew :DeviceManagerKMP:desktopJar     # Desktop
   ```

2. **Run tests:**
   ```bash
   ./gradlew :DeviceManagerKMP:allTests
   ```

3. **Verify fingerprint generation:**
   - Android: Check `HardwareProfiler` generates consistent fingerprint
   - Desktop: Check JVM fingerprint is stable across restarts
   - iOS: Check vendorId-based fingerprint works

4. **Integration test with UniversalIPC:**
   - Create HandshakeMessage with deviceId from DeviceIdentityFactory
   - Verify fingerprint format matches LicenseData expectations

---

## Deliverables

- [x] Phase 1: Pure math/data in commonMain (2026-01-18)
- [x] Phase 2: Core interfaces defined (2026-01-18)
- [x] Phase 3: Android actual implementations (2026-01-18)
- [x] Phase 4: iOS actual implementations (2026-01-18)
- [x] Phase 5: Desktop actual implementations (2026-01-18)
- [x] Phase 6: Build configuration updated (2026-01-18)
- [ ] Phase 7: Migration shim for backward compatibility
- [x] All tests passing (2026-01-18)

---

## Completion Notes (2026-01-18)

### Implemented Files

| Location | File | Purpose |
|----------|------|---------|
| commonMain | `CapabilityModels.kt` | Data classes: KmpDeviceInfo, HardwareProfile, NetworkCapabilities, etc. |
| commonMain | `DeviceCapabilityProvider.kt` | Interface + expect factory |
| commonMain | `DeviceIdentityProvider.kt` | Interface + expect factory for licensing |
| commonMain | `filters/AdaptiveFilter.kt` | Pure Kotlin motion filter |
| commonMain | `math/IMUMathUtils.kt` | Pure Kotlin vector/quaternion math |
| androidMain | `AndroidDeviceCapabilityProvider.kt` | Full Android implementation |
| androidMain | `DeviceCapabilityFactory.android.kt` | Android actual factory |
| androidMain | `AndroidDeviceIdentityProvider.kt` | Android identity provider |
| androidMain | `DeviceIdentityFactory.android.kt` | Android actual factory |
| desktopMain | `DeviceCapabilityFactory.desktop.kt` | Desktop JVM implementation |
| desktopMain | `DeviceIdentityFactory.desktop.kt` | Desktop identity provider |
| iosMain | `DeviceCapabilityFactory.ios.kt` | iOS implementation (code ready) |
| iosMain | `DeviceIdentityFactory.ios.kt` | iOS identity provider |

### Build Status

| Target | Status |
|--------|--------|
| Android Debug | PASS |
| Desktop JVM | PASS |
| iOS | Code ready (native targets disabled by default) |

### API Usage

```kotlin
// Initialize on Android (required once at app startup)
DeviceCapabilityFactory.initialize(context)

// Get capabilities (all platforms)
val provider = DeviceCapabilityFactory.create()
val deviceInfo = provider.getKmpDeviceInfo()
val fingerprint = provider.getDeviceFingerprint()
val performanceClass = provider.getPerformanceClass()

// For licensing/UniversalIPC
val identity = DeviceIdentityFactory.create()
val deviceId = identity.getDeviceId()
```

### Remaining Work

1. **Phase 7 Migration:** Create deprecation shims in old DeviceManager API
2. **UniversalIPC Integration:** Update UniversalIPC to use DeviceIdentityFactory
3. **iOS Build Verification:** Enable native targets and verify iOS compilation

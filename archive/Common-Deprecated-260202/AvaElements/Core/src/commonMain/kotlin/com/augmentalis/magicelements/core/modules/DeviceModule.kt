package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Platform delegate interface for DeviceModule.
 * Platform implementations provide actual device information and capabilities.
 */
interface DeviceModuleDelegate {
    /**
     * Get comprehensive device information.
     * @return Map containing device details (model, manufacturer, OS version, etc.)
     */
    suspend fun getDeviceInfo(): Map<String, Any?>

    /**
     * Get platform identifier.
     * @return Platform name: "android", "ios", "web", or "desktop"
     */
    fun getPlatform(): String

    /**
     * Check if device is a tablet.
     * @return true if tablet, false otherwise
     */
    fun isTablet(): Boolean

    /**
     * Get screen width in pixels.
     */
    fun getScreenWidth(): Int

    /**
     * Get screen height in pixels.
     */
    fun getScreenHeight(): Int

    /**
     * Get screen density.
     * @return Density value (e.g., 1.0, 2.0, 3.0)
     */
    fun getScreenDensity(): Float

    /**
     * Get screen orientation.
     * @return "portrait" or "landscape"
     */
    fun getScreenOrientation(): String

    /**
     * Get battery level.
     * @return Battery percentage (0-100)
     */
    fun getBatteryLevel(): Int

    /**
     * Check if device is charging.
     */
    fun isBatteryCharging(): Boolean

    /**
     * Check if device has network connectivity.
     */
    fun isNetworkOnline(): Boolean

    /**
     * Get network connection type.
     * @return "wifi", "cellular", or "none"
     */
    fun getNetworkType(): String

    /**
     * Check if Bluetooth is enabled.
     */
    fun isBluetoothEnabled(): Boolean

    /**
     * Enable Bluetooth.
     */
    suspend fun enableBluetooth()

    /**
     * Disable Bluetooth.
     */
    suspend fun disableBluetooth()

    /**
     * Check if WiFi is enabled.
     */
    fun isWiFiEnabled(): Boolean

    /**
     * Enable WiFi.
     */
    suspend fun enableWiFi()

    /**
     * Disable WiFi.
     */
    suspend fun disableWiFi()

    /**
     * Trigger haptic feedback.
     * @param type Feedback type: "light", "medium", or "heavy"
     */
    suspend fun triggerHaptic(type: String)

    /**
     * Get current audio volume level.
     * @return Volume level (0-100)
     */
    fun getAudioVolume(): Int

    /**
     * Set audio volume level.
     * @param level Volume level (0-100)
     */
    suspend fun setAudioVolume(level: Int)

    /**
     * Mute audio.
     */
    suspend fun muteAudio()

    /**
     * Unmute audio.
     */
    suspend fun unmuteAudio()
}

/**
 * DeviceModule - Wraps DeviceManager for AvaCode.
 *
 * Provides device information and control capabilities to MEL plugins.
 *
 * Usage in MEL:
 * ```
 * # DATA tier methods
 * @device.info()                      # Get device info as Map
 * @device.platform()                  # Return "android"/"ios"/"web"/"desktop"
 * @device.isTablet()                  # Boolean
 * @device.screen.width()              # Screen width in pixels
 * @device.screen.height()             # Screen height in pixels
 * @device.screen.density()            # Screen density
 * @device.screen.orientation()        # "portrait"/"landscape"
 * @device.battery.level()             # 0-100
 * @device.battery.isCharging()        # Boolean
 * @device.network.isOnline()          # Boolean
 * @device.network.type()              # "wifi"/"cellular"/"none"
 * @device.bluetooth.isEnabled()       # Boolean
 * @device.wifi.isEnabled()            # Boolean
 *
 * # LOGIC tier methods
 * @device.bluetooth.enable()          # Enable Bluetooth
 * @device.bluetooth.disable()         # Disable Bluetooth
 * @device.wifi.enable()               # Enable WiFi
 * @device.wifi.disable()              # Disable WiFi
 * @device.haptic("medium")            # Trigger haptic feedback
 * @device.audio.volume()              # Get volume level
 * @device.audio.setVolume(50)         # Set volume level
 * @device.audio.mute()                # Mute audio
 * @device.audio.unmute()              # Unmute audio
 * ```
 *
 * @param delegate Platform implementation (null for unsupported platforms)
 */
class DeviceModule(
    private val delegate: DeviceModuleDelegate?
) : BaseModule(
    name = "device",
    version = "1.0.0",
    minimumTier = PluginTier.DATA
) {

    init {
        // ========== DATA Tier Methods ==========

        registerMethod(
            name = "info",
            tier = PluginTier.DATA,
            description = "Get comprehensive device information",
            returnType = "Map<String, Any?>",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getDeviceInfo()
            }
        )

        registerMethod(
            name = "platform",
            tier = PluginTier.DATA,
            description = "Get platform identifier",
            returnType = "String",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getPlatform()
            }
        )

        registerMethod(
            name = "isTablet",
            tier = PluginTier.DATA,
            description = "Check if device is a tablet",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isTablet()
            }
        )

        // Screen methods
        registerMethod(
            name = "screen.width",
            tier = PluginTier.DATA,
            description = "Get screen width in pixels",
            returnType = "Int",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getScreenWidth()
            }
        )

        registerMethod(
            name = "screen.height",
            tier = PluginTier.DATA,
            description = "Get screen height in pixels",
            returnType = "Int",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getScreenHeight()
            }
        )

        registerMethod(
            name = "screen.density",
            tier = PluginTier.DATA,
            description = "Get screen density",
            returnType = "Float",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getScreenDensity()
            }
        )

        registerMethod(
            name = "screen.orientation",
            tier = PluginTier.DATA,
            description = "Get screen orientation",
            returnType = "String",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getScreenOrientation()
            }
        )

        // Battery methods
        registerMethod(
            name = "battery.level",
            tier = PluginTier.DATA,
            description = "Get battery level (0-100)",
            returnType = "Int",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getBatteryLevel()
            }
        )

        registerMethod(
            name = "battery.isCharging",
            tier = PluginTier.DATA,
            description = "Check if device is charging",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isBatteryCharging()
            }
        )

        // Network methods
        registerMethod(
            name = "network.isOnline",
            tier = PluginTier.DATA,
            description = "Check if device has network connectivity",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isNetworkOnline()
            }
        )

        registerMethod(
            name = "network.type",
            tier = PluginTier.DATA,
            description = "Get network connection type",
            returnType = "String",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getNetworkType()
            }
        )

        // Bluetooth methods (DATA tier - read only)
        registerMethod(
            name = "bluetooth.isEnabled",
            tier = PluginTier.DATA,
            description = "Check if Bluetooth is enabled",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isBluetoothEnabled()
            }
        )

        // WiFi methods (DATA tier - read only)
        registerMethod(
            name = "wifi.isEnabled",
            tier = PluginTier.DATA,
            description = "Check if WiFi is enabled",
            returnType = "Boolean",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.isWiFiEnabled()
            }
        )

        // ========== LOGIC Tier Methods ==========

        // Bluetooth control
        registerMethod(
            name = "bluetooth.enable",
            tier = PluginTier.LOGIC,
            description = "Enable Bluetooth",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.enableBluetooth()
            }
        )

        registerMethod(
            name = "bluetooth.disable",
            tier = PluginTier.LOGIC,
            description = "Disable Bluetooth",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.disableBluetooth()
            }
        )

        // WiFi control
        registerMethod(
            name = "wifi.enable",
            tier = PluginTier.LOGIC,
            description = "Enable WiFi",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.enableWiFi()
            }
        )

        registerMethod(
            name = "wifi.disable",
            tier = PluginTier.LOGIC,
            description = "Disable WiFi",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.disableWiFi()
            }
        )

        // Haptic feedback
        registerMethod(
            name = "haptic",
            tier = PluginTier.LOGIC,
            description = "Trigger haptic feedback",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "type",
                    type = "String",
                    required = true,
                    description = "Feedback type: 'light', 'medium', or 'heavy'"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val type = args.argString(0, "type")
                delegate!!.triggerHaptic(type)
            }
        )

        // Audio control
        registerMethod(
            name = "audio.volume",
            tier = PluginTier.LOGIC,
            description = "Get current audio volume level",
            returnType = "Int",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.getAudioVolume()
            }
        )

        registerMethod(
            name = "audio.setVolume",
            tier = PluginTier.LOGIC,
            description = "Set audio volume level",
            returnType = "Unit",
            parameters = listOf(
                MethodParameter(
                    name = "level",
                    type = "Int",
                    required = true,
                    description = "Volume level (0-100)"
                )
            ),
            handler = { args, _ ->
                requireDelegate()
                val level = args.argNumber(0, "level").toInt()
                delegate!!.setAudioVolume(level)
            }
        )

        registerMethod(
            name = "audio.mute",
            tier = PluginTier.LOGIC,
            description = "Mute audio",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.muteAudio()
            }
        )

        registerMethod(
            name = "audio.unmute",
            tier = PluginTier.LOGIC,
            description = "Unmute audio",
            returnType = "Unit",
            handler = { _, _ ->
                requireDelegate()
                delegate!!.unmuteAudio()
            }
        )
    }

    /**
     * Ensure delegate is available.
     * @throws ModuleException if delegate is null (unsupported platform)
     */
    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(
                name,
                "",
                "Device module not supported on this platform"
            )
        }
    }

    override suspend fun initialize() {
        // Delegate initialization happens at platform level
    }

    override suspend fun dispose() {
        // Cleanup happens at platform level
    }
}

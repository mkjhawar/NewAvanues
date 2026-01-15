package com.augmentalis.voiceoscoreng.scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "BatteryAwareScanner"

/**
 * Battery-aware scanning controller that adjusts scan frequency based on device state.
 *
 * Throttling Tiers:
 * - Battery > 50%: Full scanning (300ms debounce)
 * - Battery 20-50%: Reduced scanning (1000ms debounce)
 * - Battery < 20%: Minimal scanning (3000ms debounce)
 * - Charging: Full scanning (regardless of level)
 *
 * This significantly extends battery life while maintaining responsive scanning
 * when the device has adequate power.
 */
class BatteryAwareScanner(
    private val context: Context,
    private val scope: CoroutineScope
) {

    /**
     * Current throttling tier.
     */
    enum class ThrottlingTier(
        val debounceMs: Long,
        val description: String
    ) {
        FULL(300L, "Full scanning"),
        REDUCED(1000L, "Reduced scanning"),
        MINIMAL(3000L, "Minimal scanning")
    }

    /**
     * Current battery state information.
     */
    data class BatteryState(
        val level: Int,
        val isCharging: Boolean,
        val chargingSource: ChargingSource,
        val temperature: Float,
        val health: Int
    )

    /**
     * Source of charging power.
     */
    enum class ChargingSource {
        NONE,
        AC,
        USB,
        WIRELESS,
        UNKNOWN
    }

    // State flows
    private val _currentTier = MutableStateFlow(ThrottlingTier.FULL)
    val currentTier: StateFlow<ThrottlingTier> = _currentTier.asStateFlow()

    private val _batteryState = MutableStateFlow<BatteryState?>(null)
    val batteryState: StateFlow<BatteryState?> = _batteryState.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Debounce state
    private val lastScanRequest = AtomicLong(0L)
    private val isPendingScan = AtomicBoolean(false)
    private var pendingScanJob: Job? = null

    // Callbacks
    private var onScanTriggered: (() -> Unit)? = null
    private var onTierChanged: ((ThrottlingTier) -> Unit)? = null

    /**
     * BroadcastReceiver for battery state changes.
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
                Intent.ACTION_BATTERY_LOW -> handleBatteryLow()
                Intent.ACTION_BATTERY_OKAY -> handleBatteryOkay()
            }
        }
    }

    private var isReceiverRegistered = false

    /**
     * Register the battery receiver and start monitoring.
     * Must be called before using the scanner.
     */
    fun register() {
        if (isReceiverRegistered) {
            Log.w(TAG, "Battery receiver already registered")
            return
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }

        // Get initial battery state
        val batteryStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(batteryReceiver, filter)
        }

        isReceiverRegistered = true
        Log.d(TAG, "Battery receiver registered")

        // Process initial state
        batteryStatus?.let { handleBatteryChanged(it) }
    }

    /**
     * Unregister the battery receiver.
     * Call when the scanner is no longer needed.
     */
    fun unregister() {
        if (!isReceiverRegistered) {
            Log.w(TAG, "Battery receiver not registered")
            return
        }

        try {
            context.unregisterReceiver(batteryReceiver)
            isReceiverRegistered = false
            Log.d(TAG, "Battery receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering battery receiver", e)
        }

        pendingScanJob?.cancel()
        pendingScanJob = null
    }

    /**
     * Handle battery state change broadcast.
     */
    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = if (scale > 0) (level * 100 / scale) else level

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingSource.WIRELESS
            0 -> ChargingSource.NONE
            else -> ChargingSource.UNKNOWN
        }

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val newState = BatteryState(
            level = batteryPct,
            isCharging = isCharging,
            chargingSource = chargingSource,
            temperature = temperature,
            health = health
        )

        _batteryState.value = newState
        updateThrottlingTier(newState)

        Log.v(TAG, "Battery: ${batteryPct}%, charging=$isCharging, source=$chargingSource, temp=${temperature}C")
    }

    /**
     * Handle power connected event.
     */
    private fun handlePowerConnected() {
        Log.d(TAG, "Power connected - switching to full scanning")
        _currentTier.value = ThrottlingTier.FULL
        onTierChanged?.invoke(ThrottlingTier.FULL)
    }

    /**
     * Handle power disconnected event.
     */
    private fun handlePowerDisconnected() {
        Log.d(TAG, "Power disconnected - recalculating tier")
        _batteryState.value?.let { updateThrottlingTier(it) }
    }

    /**
     * Handle low battery broadcast.
     */
    private fun handleBatteryLow() {
        Log.d(TAG, "Battery low - switching to minimal scanning")
        _currentTier.value = ThrottlingTier.MINIMAL
        onTierChanged?.invoke(ThrottlingTier.MINIMAL)
    }

    /**
     * Handle battery okay broadcast.
     */
    private fun handleBatteryOkay() {
        Log.d(TAG, "Battery okay - recalculating tier")
        _batteryState.value?.let { updateThrottlingTier(it) }
    }

    /**
     * Update the throttling tier based on battery state.
     */
    private fun updateThrottlingTier(state: BatteryState) {
        val newTier = when {
            // Charging always gets full scanning
            state.isCharging -> ThrottlingTier.FULL

            // Battery > 50%: Full scanning
            state.level > 50 -> ThrottlingTier.FULL

            // Battery 20-50%: Reduced scanning
            state.level in 20..50 -> ThrottlingTier.REDUCED

            // Battery < 20%: Minimal scanning
            else -> ThrottlingTier.MINIMAL
        }

        if (_currentTier.value != newTier) {
            Log.d(TAG, "Throttling tier changed: ${_currentTier.value} -> $newTier " +
                    "(battery=${state.level}%, charging=${state.isCharging})")
            _currentTier.value = newTier
            onTierChanged?.invoke(newTier)
        }
    }

    /**
     * Request a scan with debouncing based on current tier.
     *
     * The scan will be delayed according to the current throttling tier.
     * Multiple rapid requests will be coalesced into a single scan.
     *
     * @param force If true, bypass debounce and scan immediately
     */
    fun requestScan(force: Boolean = false) {
        if (!_isEnabled.value) {
            Log.v(TAG, "Scanning disabled - ignoring request")
            return
        }

        val now = System.currentTimeMillis()
        val tier = _currentTier.value
        val debounceMs = tier.debounceMs

        if (force) {
            Log.d(TAG, "Forced scan requested - bypassing debounce")
            executeScan()
            return
        }

        val timeSinceLastRequest = now - lastScanRequest.get()

        if (timeSinceLastRequest < debounceMs) {
            // Within debounce window - schedule or update pending scan
            if (isPendingScan.compareAndSet(false, true)) {
                val delayRemaining = debounceMs - timeSinceLastRequest
                Log.v(TAG, "Debouncing scan - will execute in ${delayRemaining}ms (tier=$tier)")

                pendingScanJob?.cancel()
                pendingScanJob = scope.launch {
                    delay(delayRemaining)
                    if (isPendingScan.get()) {
                        executeScan()
                    }
                }
            } else {
                Log.v(TAG, "Scan already pending - coalescing request")
            }
        } else {
            // Outside debounce window - execute scan
            executeScan()
        }

        lastScanRequest.set(now)
    }

    /**
     * Execute the actual scan callback.
     */
    private fun executeScan() {
        isPendingScan.set(false)
        pendingScanJob = null

        Log.v(TAG, "Executing scan (tier=${_currentTier.value})")
        onScanTriggered?.invoke()
    }

    /**
     * Enable or disable scanning.
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        Log.d(TAG, "Scanning ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            pendingScanJob?.cancel()
            pendingScanJob = null
            isPendingScan.set(false)
        }
    }

    /**
     * Set the callback for when a scan should be triggered.
     */
    fun setOnScanTriggered(callback: () -> Unit) {
        onScanTriggered = callback
    }

    /**
     * Set the callback for when the throttling tier changes.
     */
    fun setOnTierChanged(callback: (ThrottlingTier) -> Unit) {
        onTierChanged = callback
    }

    /**
     * Get current debounce delay based on tier.
     */
    fun getCurrentDebounceMs(): Long = _currentTier.value.debounceMs

    /**
     * Get a human-readable status summary.
     */
    fun getStatusSummary(): String {
        val state = _batteryState.value
        val tier = _currentTier.value

        return if (state != null) {
            "Battery: ${state.level}%, ${if (state.isCharging) "Charging (${state.chargingSource})" else "Not charging"}, " +
                    "Tier: ${tier.description} (${tier.debounceMs}ms debounce)"
        } else {
            "Battery state unknown, Tier: ${tier.description}"
        }
    }

    /**
     * Force a specific throttling tier (for testing or user override).
     *
     * @param tier The tier to force, or null to return to automatic mode
     */
    fun forceThrottlingTier(tier: ThrottlingTier?) {
        if (tier != null) {
            Log.d(TAG, "Forcing throttling tier: $tier")
            _currentTier.value = tier
            onTierChanged?.invoke(tier)
        } else {
            Log.d(TAG, "Returning to automatic throttling")
            _batteryState.value?.let { updateThrottlingTier(it) }
        }
    }

    /**
     * Check if currently in a power-saving tier.
     */
    fun isInPowerSavingMode(): Boolean {
        return _currentTier.value != ThrottlingTier.FULL
    }

    companion object {
        /**
         * Threshold below which minimal scanning is used.
         */
        const val MINIMAL_BATTERY_THRESHOLD = 20

        /**
         * Threshold below which reduced scanning is used.
         */
        const val REDUCED_BATTERY_THRESHOLD = 50
    }
}

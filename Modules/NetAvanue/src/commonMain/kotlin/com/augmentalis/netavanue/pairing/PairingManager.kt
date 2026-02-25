package com.augmentalis.netavanue.pairing

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.capability.DeviceFingerprint
import com.augmentalis.netavanue.signaling.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Manages device-to-device pairing.
 *
 * Pairing allows two devices to auto-reconnect to shared sessions
 * without needing invite codes each time. The pairing is established
 * via the signaling server and stored locally.
 *
 * Flow:
 * 1. Device A sends PAIR_REQUEST with target fingerprint
 * 2. Device B receives PAIR_REQUESTED event with Device A's name
 * 3. Device B calls acceptPairing() or rejectPairing()
 * 4. Both devices receive PAIR_ESTABLISHED with the pairing ID
 */
class PairingManager(
    private val client: SignalingClient,
    private val fingerprint: DeviceFingerprint,
) {
    private val logger = LoggerFactory.getLogger("PairingManager")

    private val _pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDevice>> = _pairedDevices.asStateFlow()

    private val _incomingRequests = MutableSharedFlow<PairRequestedEvent>(extraBufferCapacity = 8)
    val incomingRequests: SharedFlow<PairRequestedEvent> = _incomingRequests.asSharedFlow()

    /** Start listening for pairing events. Call once after SignalingClient.connect(). */
    fun startListening(scope: CoroutineScope) {
        scope.launch { observePairingEvents() }
    }

    /** Load previously paired devices from local storage. */
    fun loadPairedDevices(devices: List<PairedDevice>) {
        _pairedDevices.value = devices
    }

    /** Request pairing with another device by fingerprint. */
    suspend fun requestPairing(targetFingerprint: String) {
        client.requestPairing(
            PairRequestMessage(
                fingerprint = fingerprint.fingerprint,
                targetFingerprint = targetFingerprint,
            )
        )
        logger.i { "Pairing request sent to $targetFingerprint" }
    }

    /** Accept an incoming pairing request. */
    suspend fun acceptPairing(requestId: String) {
        client.acceptPairing(
            PairResponseMessage(
                fingerprint = fingerprint.fingerprint,
                requestId = requestId,
            )
        )
        logger.i { "Pairing accepted: $requestId" }
    }

    /** Reject an incoming pairing request. */
    suspend fun rejectPairing(requestId: String) {
        client.rejectPairing(
            PairResponseMessage(
                fingerprint = fingerprint.fingerprint,
                requestId = requestId,
            )
        )
        logger.i { "Pairing rejected: $requestId" }
    }

    /** Remove a paired device locally. */
    fun removePairedDevice(pairingId: String) {
        _pairedDevices.value = _pairedDevices.value.filter { it.pairingId != pairingId }
    }

    /** Check if a device is paired by fingerprint. */
    fun isPaired(peerFingerprint: String): Boolean {
        return _pairedDevices.value.any { it.fingerprint == peerFingerprint }
    }

    private suspend fun observePairingEvents() {
        client.pairingEvents.collect { event ->
            when (event) {
                is ServerEvent.PairRequested -> {
                    logger.i { "Pairing request from: ${event.event.fromDeviceName}" }
                    _incomingRequests.tryEmit(event.event)
                }
                is ServerEvent.PairEstablished -> {
                    val paired = PairedDevice(
                        pairingId = event.event.pairingId,
                        fingerprint = event.event.peerFingerprint,
                        deviceName = "", // Will be enriched by UI layer
                        platform = DevicePlatform.ANDROID, // Will be enriched
                        deviceType = DeviceType.PHONE, // Will be enriched
                        pairedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                    )
                    _pairedDevices.value = _pairedDevices.value + paired
                    logger.i { "Pairing established: ${event.event.pairingId} with ${event.event.peerFingerprint}" }
                }
                else -> { /* not a pairing event */ }
            }
        }
    }
}

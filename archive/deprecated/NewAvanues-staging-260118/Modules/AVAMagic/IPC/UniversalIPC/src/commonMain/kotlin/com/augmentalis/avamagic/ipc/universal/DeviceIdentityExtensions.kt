// Author: Manoj Jhawar
// Purpose: AVU IPC Protocol Extensions for Device Identity

package com.augmentalis.avamagic.ipc.universal

import com.augmentalis.devicemanager.DeviceIdentityFactory
import com.augmentalis.devicemanager.DeviceIdentityProvider
import kotlinx.datetime.Clock

/**
 * AVU IPC Protocol Extensions for Device Identity
 *
 * These helpers auto-populate deviceId from DeviceIdentityFactory,
 * ensuring consistent device identification across IPC messages.
 *
 * Protocol: Avanues Universal IPC v2.0
 * Handshake format: HND:protocolVersion:appVersion:deviceId
 */

/**
 * Creates a HandshakeMessage following AVU IPC protocol.
 *
 * Serializes to: `HND:2.0:appVersion:deviceId`
 *
 * @param appVersion Application version (e.g., "1.5.0")
 * @param protocolVersion AVU protocol version (default: "2.0")
 * @param identityProvider Device identity provider (auto-injected)
 * @return HandshakeMessage ready for AVU IPC transmission
 */
fun createHandshakeMessage(
    appVersion: String,
    protocolVersion: String = "2.0",
    identityProvider: DeviceIdentityProvider = DeviceIdentityFactory.create()
): HandshakeMessage = HandshakeMessage(
    protocolVersion = protocolVersion,
    appVersion = appVersion,
    deviceId = identityProvider.getDeviceId()
)

/**
 * Creates a PromotionMessage with auto-populated deviceId.
 *
 * Serializes to: `PRO:deviceId:priority:timestamp`
 *
 * @param priority Priority level for promotion
 * @param timestamp Timestamp in epoch milliseconds (default: current time)
 * @param identityProvider Device identity provider (auto-injected)
 * @return PromotionMessage ready for AVU IPC transmission
 */
fun createPromotionMessage(
    priority: Int,
    timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    identityProvider: DeviceIdentityProvider = DeviceIdentityFactory.create()
): PromotionMessage = PromotionMessage(
    deviceId = identityProvider.getDeviceId(),
    priority = priority,
    timestamp = timestamp
)

/**
 * Creates a RoleChangeMessage with auto-populated deviceId.
 *
 * Serializes to: `ROL:deviceId:role`
 *
 * @param role New role (CLIENT or SERVER)
 * @param identityProvider Device identity provider (auto-injected)
 * @return RoleChangeMessage ready for AVU IPC transmission
 */
fun createRoleChangeMessage(
    role: Role,
    identityProvider: DeviceIdentityProvider = DeviceIdentityFactory.create()
): RoleChangeMessage = RoleChangeMessage(
    deviceId = identityProvider.getDeviceId(),
    role = role
)

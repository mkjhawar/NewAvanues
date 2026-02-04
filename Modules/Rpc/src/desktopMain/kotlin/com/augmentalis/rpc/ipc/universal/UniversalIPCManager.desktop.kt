package com.augmentalis.rpc.ipc.universal

import com.augmentalis.rpc.ipc.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

/**
 * Desktop/JVM implementation of UniversalIPCManager
 *
 * Uses socket-based IPC for desktop platforms.
 *
 * @author Augmentalis Engineering
 */
class DesktopUniversalIPCManager : UniversalIPCManager {

    private val messageFlow = MutableSharedFlow<Pair<String, UniversalMessage>>(replay = 0)
    private var registeredAppId: String? = null

    override suspend fun send(target: String, message: UniversalMessage): Result<Unit> {
        // TODO: Implement socket-based IPC for desktop
        return Result.failure(IPCError.SendFailed("Desktop IPC not yet implemented"))
    }

    override suspend fun broadcast(message: UniversalMessage): Result<Unit> {
        // TODO: Implement broadcast for desktop
        return Result.failure(IPCError.SendFailed("Desktop IPC not yet implemented"))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UniversalMessage> subscribe(): Flow<T> {
        return messageFlow.map { it.second as T }
    }

    override fun subscribe(filter: MessageFilter): Flow<UniversalMessage> {
        return messageFlow
            .map { it.second }
    }

    override suspend fun request(
        target: String,
        message: UniversalMessage,
        timeout: Long
    ): Result<UniversalMessage> {
        return Result.failure(IPCError.SendFailed("Desktop IPC not yet implemented"))
    }

    override suspend fun register(appId: String, capabilities: List<String>): Result<Unit> {
        registeredAppId = appId
        return Result.success(Unit)
    }

    override suspend fun unregister() {
        registeredAppId = null
    }

    override suspend fun isAvailable(target: String): Boolean {
        return false // Desktop IPC discovery not yet implemented
    }

    override suspend fun getConnectedApps(): List<String> {
        return emptyList()
    }
}

actual fun UniversalIPCManager.Companion.create(): UniversalIPCManager {
    return DesktopUniversalIPCManager()
}

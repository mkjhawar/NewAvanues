package com.augmentalis.avamagic.ipc.universal

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * Android implementation of UniversalIPCManager
 *
 * Uses Android Intents for IPC transport
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 */
class AndroidUniversalIPCManager(
    private val context: Context
) : UniversalIPCManager {

    private val messageFlow = MutableSharedFlow<Pair<String, UniversalMessage>>(replay = 0)
    private var registeredAppId: String? = null

    companion object {
        const val ACTION_UNIVERSAL_IPC = "com.augmentalis.avamagic.IPC.UNIVERSAL"
        const val EXTRA_SOURCE_APP = "source_app"
        const val EXTRA_MESSAGE = "message"
    }

    override suspend fun send(target: String, message: UniversalMessage): Result<Unit> {
        return try {
            val intent = Intent(ACTION_UNIVERSAL_IPC).apply {
                setPackage(target)
                putExtra(EXTRA_SOURCE_APP, registeredAppId)
                putExtra(EXTRA_MESSAGE, message.serialize())
            }
            context.sendBroadcast(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IPCError.SendFailed("Failed to send intent", e))
        }
    }

    override suspend fun broadcast(message: UniversalMessage): Result<Unit> {
        return try {
            val intent = Intent(ACTION_UNIVERSAL_IPC).apply {
                putExtra(EXTRA_SOURCE_APP, registeredAppId)
                putExtra(EXTRA_MESSAGE, message.serialize())
            }
            context.sendBroadcast(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IPCError.SendFailed("Failed to broadcast", e))
        }
    }

    override fun <T : UniversalMessage> subscribe(): Flow<T> {
        return messageFlow
            .map { it.second }
            .filterIsInstance<T>()
    }

    override fun subscribe(filter: MessageFilter): Flow<UniversalMessage> {
        return messageFlow
            .filter { (source, message) -> filter.matches(message, source) }
            .map { it.second }
    }

    override suspend fun request(
        target: String,
        message: UniversalMessage,
        timeout: Long
    ): Result<UniversalMessage> {
        // TODO: Implement request-response with correlation ID
        return Result.failure(IPCError.SendFailed("Request-response not yet implemented"))
    }

    override suspend fun register(appId: String, capabilities: List<String>): Result<Unit> {
        registeredAppId = appId
        // TODO: Register with system IPC registry
        return Result.success(Unit)
    }

    override suspend fun unregister() {
        registeredAppId = null
        // TODO: Unregister from system
    }

    override suspend fun isAvailable(target: String): Boolean {
        // TODO: Query package manager or IPC registry
        return try {
            context.packageManager.getPackageInfo(target, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getConnectedApps(): List<String> {
        // TODO: Query IPC registry for connected apps
        return emptyList()
    }

    /**
     * Handle incoming Intent from BroadcastReceiver
     *
     * Call this from your BroadcastReceiver:
     * ```kotlin
     * class IPCReceiver : BroadcastReceiver() {
     *     override fun onReceive(context: Context, intent: Intent) {
     *         ipcManager.handleIncomingIntent(intent)
     *     }
     * }
     * ```
     */
    suspend fun handleIncomingIntent(intent: Intent) {
        val sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: return
        val messageStr = intent.getStringExtra(EXTRA_MESSAGE) ?: return

        val parsed = UniversalDSL.parse(messageStr)
        when (parsed) {
            is ParseResult.Protocol -> {
                messageFlow.emit(sourceApp to parsed.message)
            }
            else -> {
                // Ignore UI components or unknown messages
            }
        }
    }
}

actual fun UniversalIPCManager.Companion.create(): UniversalIPCManager {
    // TODO: Get Context from DI or Application class
    throw NotImplementedError("Use create(context: Context) for Android")
}

/**
 * Create Android IPC Manager with Context
 */
fun UniversalIPCManager.Companion.create(context: Context): UniversalIPCManager {
    return AndroidUniversalIPCManager(context)
}

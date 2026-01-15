package com.augmentalis.avamagic.ipc

/**
 * iOS Content Provider Connector
 *
 * Placeholder implementation for iOS content sharing.
 * iOS uses App Groups or file sharing instead of Content Providers.
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
actual class ContentProviderConnector {

    actual suspend fun query(
        endpoint: ContentProviderEndpoint,
        params: QueryParams
    ): Result<QueryResult> {
        return Result.failure(Exception("iOS does not support Content Providers. Use App Groups or file sharing."))
    }

    actual suspend fun insert(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>
    ): Result<String> {
        return Result.failure(Exception("iOS does not support Content Providers. Use App Groups or file sharing."))
    }

    actual suspend fun update(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>,
        selection: String?,
        selectionArgs: List<String>?
    ): Result<Int> {
        return Result.failure(Exception("iOS does not support Content Providers. Use App Groups or file sharing."))
    }

    actual suspend fun delete(
        endpoint: ContentProviderEndpoint,
        uri: String,
        selection: String?,
        selectionArgs: List<String>?
    ): Result<Int> {
        return Result.failure(Exception("iOS does not support Content Providers. Use App Groups or file sharing."))
    }

    actual fun registerObserver(uri: String, callback: (String) -> Unit) {
        // No-op on iOS
    }

    actual fun unregisterObserver(uri: String) {
        // No-op on iOS
    }
}

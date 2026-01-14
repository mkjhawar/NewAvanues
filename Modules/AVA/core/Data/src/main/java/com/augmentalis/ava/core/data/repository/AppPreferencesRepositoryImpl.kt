package com.augmentalis.ava.core.data.repository

import com.augmentalis.ava.core.data.db.AppPreferencesQueries
import com.augmentalis.ava.core.domain.model.AppPreference
import com.augmentalis.ava.core.domain.repository.AppPreferencesRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of AppPreferencesRepository.
 *
 * Manages user's app preferences stored in the database.
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * Author: Manoj Jhawar
 */
class AppPreferencesRepositoryImpl(
    private val queries: AppPreferencesQueries
) : AppPreferencesRepository {

    override suspend fun getPreferredApp(capability: String): AppPreference? =
        withContext(Dispatchers.IO) {
            queries.getPreferredApp(capability).executeAsOneOrNull()?.let { row ->
                AppPreference(
                    capability = capability,
                    packageName = row.package_name,
                    appName = row.app_name,
                    setAt = 0L, // Not returned by this query
                    setBy = "user"
                )
            }
        }

    override suspend fun setPreferredApp(
        capability: String,
        packageName: String,
        appName: String,
        setBy: String
    ) = withContext(Dispatchers.IO) {
        queries.setPreferredApp(
            capability = capability,
            package_name = packageName,
            app_name = appName,
            set_at = System.currentTimeMillis(),
            set_by = setBy
        )
    }

    override suspend fun clearPreferredApp(capability: String) = withContext(Dispatchers.IO) {
        queries.clearPreferredApp(capability)
    }

    override suspend fun getAllPreferences(): Map<String, AppPreference> =
        withContext(Dispatchers.IO) {
            queries.getAllPreferences().executeAsList().associate { row ->
                row.capability to AppPreference(
                    capability = row.capability,
                    packageName = row.package_name,
                    appName = row.app_name,
                    setAt = row.set_at,
                    setBy = row.set_by
                )
            }
        }

    override suspend fun hasPreference(capability: String): Boolean =
        withContext(Dispatchers.IO) {
            queries.hasPreference(capability).executeAsOne()
        }

    override fun observeAllPreferences(): Flow<List<AppPreference>> {
        return queries.getAllPreferences()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    AppPreference(
                        capability = row.capability,
                        packageName = row.package_name,
                        appName = row.app_name,
                        setAt = row.set_at,
                        setBy = row.set_by
                    )
                }
            }
    }

    override suspend fun recordUsage(
        capability: String,
        packageName: String,
        contextJson: String?
    ) = withContext(Dispatchers.IO) {
        queries.recordUsage(
            capability = capability,
            package_name = packageName,
            used_at = System.currentTimeMillis(),
            context_json = contextJson
        )
    }

    override suspend fun getMostUsedApp(capability: String): String? =
        withContext(Dispatchers.IO) {
            queries.getMostUsedApp(capability).executeAsOneOrNull()?.package_name
        }
}

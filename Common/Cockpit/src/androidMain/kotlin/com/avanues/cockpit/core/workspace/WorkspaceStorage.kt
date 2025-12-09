package com.avanues.cockpit.core.workspace

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Android Workspace Storage (In-Memory)
 *
 * Simple in-memory implementation for MVP.
 * Phase 3+ will add persistent storage via SharedPreferences or Room.
 *
 * **Future Enhancement:**
 * - Use SharedPreferences for simple persistence
 * - Use Room database for complex queries
 * - Add cloud sync via Firebase/Supabase
 */
actual class WorkspaceStorage {
    private val workspaces: MutableMap<String, Workspace> = mutableMapOf()
    private var lastActiveId: String? = null

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual suspend fun save(workspace: Workspace) {
        workspaces[workspace.id] = workspace
    }

    actual suspend fun load(workspaceId: String): Workspace? {
        return workspaces[workspaceId]
    }

    actual suspend fun loadAll(): List<Workspace> {
        return workspaces.values.toList()
    }

    actual suspend fun delete(workspaceId: String) {
        workspaces.remove(workspaceId)

        if (lastActiveId == workspaceId) {
            lastActiveId = null
        }
    }

    actual suspend fun saveLastActive(workspaceId: String) {
        lastActiveId = workspaceId
    }

    actual suspend fun loadLastActive(): Workspace? {
        return lastActiveId?.let { workspaces[it] }
    }
}

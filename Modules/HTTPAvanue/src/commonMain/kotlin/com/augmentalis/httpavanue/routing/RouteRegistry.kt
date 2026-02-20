package com.augmentalis.httpavanue.routing

import com.augmentalis.httpavanue.http.HttpMethod

data class RouteEntry(
    val method: HttpMethod,
    val pattern: String,
    val handler: RouteHandler,
    val pathPattern: RoutePattern = RoutePattern(pattern),
    val metadata: Map<String, Any> = emptyMap(),
    val name: String? = null,
) {
    fun match(method: HttpMethod, path: String): Map<String, String>? {
        if (this.method != method) return null
        return pathPattern.match(path)
    }
}

/**
 * Registry for storing and looking up routes efficiently.
 * Static routes indexed by method+path; dynamic routes checked sequentially.
 */
class RouteRegistry {
    private val routes = mutableListOf<RouteEntry>()
    private val staticRoutes = mutableMapOf<HttpMethod, MutableMap<String, RouteEntry>>()
    private val dynamicRoutes = mutableMapOf<HttpMethod, MutableList<RouteEntry>>()

    fun register(
        method: HttpMethod, pattern: String, handler: RouteHandler,
        metadata: Map<String, Any> = emptyMap(), name: String? = null,
    ) {
        val entry = RouteEntry(method, pattern, handler, RoutePattern(pattern), metadata, name)
        routes.add(entry)
        if (entry.pathPattern.isStatic) {
            staticRoutes.getOrPut(method) { mutableMapOf() }[pattern] = entry
        } else {
            dynamicRoutes.getOrPut(method) { mutableListOf() }.add(entry)
        }
    }

    fun find(method: HttpMethod, path: String): Pair<RouteEntry, Map<String, String>>? {
        staticRoutes[method]?.get(path)?.let { return it to emptyMap() }
        dynamicRoutes[method]?.forEach { entry ->
            entry.match(method, path)?.let { params -> return entry to params }
        }
        return null
    }

    fun findByName(name: String): RouteEntry? = routes.firstOrNull { it.name == name }
    fun findByMethod(method: HttpMethod): List<RouteEntry> = routes.filter { it.method == method }
    fun findByPrefix(prefix: String): List<RouteEntry> = routes.filter { it.pattern.startsWith(prefix) }
    fun exists(method: HttpMethod, pattern: String) = routes.any { it.method == method && it.pattern == pattern }

    fun remove(method: HttpMethod, pattern: String): Boolean {
        val entry = routes.firstOrNull { it.method == method && it.pattern == pattern } ?: return false
        routes.remove(entry)
        if (entry.pathPattern.isStatic) staticRoutes[method]?.remove(pattern)
        else dynamicRoutes[method]?.remove(entry)
        return true
    }

    fun clear() { routes.clear(); staticRoutes.clear(); dynamicRoutes.clear() }
    fun entries(): List<RouteEntry> = routes.toList()
    fun size(): Int = routes.size

    fun stats(): RegistryStats {
        val methodCounts = routes.groupBy { it.method }.mapValues { it.value.size }
        return RegistryStats(
            total = routes.size,
            static = staticRoutes.values.sumOf { it.size },
            dynamic = dynamicRoutes.values.sumOf { it.size },
            byMethod = methodCounts,
            named = routes.count { it.name != null },
        )
    }
}

data class RegistryStats(
    val total: Int, val static: Int, val dynamic: Int,
    val byMethod: Map<HttpMethod, Int>, val named: Int,
)

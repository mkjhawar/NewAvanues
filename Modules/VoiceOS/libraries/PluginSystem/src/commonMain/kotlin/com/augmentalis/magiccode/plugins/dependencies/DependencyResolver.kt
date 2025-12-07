package com.augmentalis.magiccode.plugins.dependencies

import com.augmentalis.magiccode.plugins.core.*

/**
 * Dependency resolver for plugins.
 *
 * Resolves plugin dependencies and determines load order.
 */
class DependencyResolver(private val registry: PluginRegistry) {
    companion object {
        private const val TAG = "DependencyResolver"
    }

    private val semverValidator = SemverConstraintValidator()

    /**
     * Resolution result.
     */
    sealed class ResolutionResult {
        data class Success(val loadOrder: List<String>) : ResolutionResult()
        data class Failure(val reason: String, val cycle: List<String> = emptyList()) : ResolutionResult()
    }

    /**
     * Resolve dependencies and compute load order.
     *
     * Uses topological sort to determine correct loading sequence.
     *
     * @param pluginId Plugin to resolve dependencies for
     * @param availablePlugins Map of available plugin manifests
     * @return ResolutionResult
     */
    suspend fun resolveDependencies(
        pluginId: String,
        availablePlugins: Map<String, PluginManifest>
    ): ResolutionResult {
        val manifest = availablePlugins[pluginId]
            ?: return ResolutionResult.Failure("Plugin not found: $pluginId")

        // Build dependency graph
        val graph = buildDependencyGraph(pluginId, availablePlugins)

        // Check for circular dependencies
        val cycle = detectCycle(graph, pluginId)
        if (cycle.isNotEmpty()) {
            return ResolutionResult.Failure("Circular dependency detected", cycle)
        }

        // Topological sort to get load order
        val loadOrder = topologicalSort(graph, pluginId)

        return ResolutionResult.Success(loadOrder)
    }

    /**
     * Build dependency graph with version constraint validation.
     *
     * Validates that each dependency's version satisfies the required constraint.
     *
     * @throws IllegalStateException if a dependency version doesn't satisfy its constraint
     */
    private fun buildDependencyGraph(
        rootPluginId: String,
        availablePlugins: Map<String, PluginManifest>
    ): Map<String, List<String>> {
        val graph = mutableMapOf<String, List<String>>()
        val visited = mutableSetOf<String>()

        fun visit(pluginId: String) {
            if (pluginId in visited) return
            visited.add(pluginId)

            val manifest = availablePlugins[pluginId] ?: return

            // Filter non-optional dependencies and validate version constraints
            val dependencies = manifest.dependencies
                .filterNot { it.optional }
                .onEach { dependency ->
                    // Validate the constraint format
                    if (!semverValidator.isValidConstraint(dependency.version)) {
                        throw IllegalStateException(
                            "Invalid version constraint '${dependency.version}' for dependency '${dependency.pluginId}' in plugin '$pluginId'"
                        )
                    }

                    // Check if dependency exists
                    val dependencyManifest = availablePlugins[dependency.pluginId]
                    if (dependencyManifest != null) {
                        // Validate that the available version satisfies the constraint
                        if (!semverValidator.satisfies(dependencyManifest.version, dependency.version)) {
                            throw IllegalStateException(
                                "Plugin '$pluginId' requires '${dependency.pluginId}' version '${dependency.version}', " +
                                "but found version '${dependencyManifest.version}' which does not satisfy the constraint"
                            )
                        }
                    }
                }
                .map { it.pluginId }

            graph[pluginId] = dependencies

            dependencies.forEach { depId ->
                visit(depId)
            }
        }

        visit(rootPluginId)
        return graph
    }

    /**
     * Detect circular dependencies using DFS.
     */
    private fun detectCycle(
        graph: Map<String, List<String>>,
        start: String
    ): List<String> {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(node: String): Boolean {
            visited.add(node)
            recursionStack.add(node)
            path.add(node)

            for (neighbor in graph[node] ?: emptyList()) {
                if (neighbor !in visited) {
                    if (dfs(neighbor)) return true
                } else if (neighbor in recursionStack) {
                    // Found cycle
                    path.add(neighbor)
                    return true
                }
            }

            recursionStack.remove(node)
            path.removeAt(path.lastIndex)
            return false
        }

        return if (dfs(start)) path else emptyList()
    }

    /**
     * Topological sort to determine load order.
     */
    private fun topologicalSort(
        graph: Map<String, List<String>>,
        start: String
    ): List<String> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun dfs(node: String) {
            if (node in visited) return
            visited.add(node)

            for (neighbor in graph[node] ?: emptyList()) {
                dfs(neighbor)
            }

            result.add(node)
        }

        dfs(start)
        return result // Already in correct order (dependencies before dependents)
    }
}

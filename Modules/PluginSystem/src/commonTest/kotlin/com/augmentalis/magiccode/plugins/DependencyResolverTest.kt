package com.augmentalis.magiccode.plugins

import com.augmentalis.magiccode.plugins.core.PluginDependency
import com.augmentalis.magiccode.plugins.core.PluginManifest
import com.augmentalis.magiccode.plugins.core.PluginRegistry
import com.augmentalis.magiccode.plugins.dependencies.DependencyResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DependencyResolverTest {

    private fun makeRegistry() = PluginRegistry()

    private fun manifest(
        id: String,
        version: String = "1.0.0",
        deps: List<PluginDependency> = emptyList()
    ) = PluginManifest(
        id = id,
        name = id,
        version = version,
        author = "Test",
        entrypoint = "com.example.Plugin",
        dependencies = deps,
        source = "THIRD_PARTY",
        verificationLevel = "UNVERIFIED"
    )

    // ─── DependencyResolver tests ──────────────────────────────────────────────

    @Test
    fun resolveSinglePluginWithNoDepsSucceeds() = runTest {
        val resolver = DependencyResolver(makeRegistry())
        val plugins = mapOf("com.a.plugin" to manifest("com.a.plugin"))

        val result = resolver.resolveDependencies("com.a.plugin", plugins)

        assertIs<DependencyResolver.ResolutionResult.Success>(result)
        assertEquals(listOf("com.a.plugin"), result.loadOrder)
    }

    @Test
    fun resolveLinearChainOrdersDepsFirst() = runTest {
        val resolver = DependencyResolver(makeRegistry())
        val plugins = mapOf(
            "com.a.base" to manifest("com.a.base"),
            "com.a.mid" to manifest(
                "com.a.mid",
                deps = listOf(PluginDependency("com.a.base", "^1.0.0"))
            ),
            "com.a.top" to manifest(
                "com.a.top",
                deps = listOf(PluginDependency("com.a.mid", "^1.0.0"))
            )
        )

        val result = resolver.resolveDependencies("com.a.top", plugins)

        assertIs<DependencyResolver.ResolutionResult.Success>(result)
        val order = result.loadOrder
        // base must come before mid, mid must come before top
        assertTrue(order.indexOf("com.a.base") < order.indexOf("com.a.mid"))
        assertTrue(order.indexOf("com.a.mid") < order.indexOf("com.a.top"))
    }

    @Test
    fun detectsDirectCycle() = runTest {
        val resolver = DependencyResolver(makeRegistry())
        val plugins = mapOf(
            "com.a.alpha" to manifest(
                "com.a.alpha",
                deps = listOf(PluginDependency("com.a.beta", "^1.0.0"))
            ),
            "com.a.beta" to manifest(
                "com.a.beta",
                deps = listOf(PluginDependency("com.a.alpha", "^1.0.0"))
            )
        )

        val result = resolver.resolveDependencies("com.a.alpha", plugins)

        assertIs<DependencyResolver.ResolutionResult.Failure>(result)
        assertTrue(result.cycle.isNotEmpty(), "Cycle list should be populated")
    }

    @Test
    fun missingPluginIdReturnsFailure() = runTest {
        val resolver = DependencyResolver(makeRegistry())

        val result = resolver.resolveDependencies("com.not.exist", emptyMap())

        assertIs<DependencyResolver.ResolutionResult.Failure>(result)
        assertTrue(result.reason.contains("not found", ignoreCase = true))
    }

    @Test
    fun optionalDepsAreIgnoredInResolution() = runTest {
        val resolver = DependencyResolver(makeRegistry())
        // Optional dep is missing entirely — should still succeed
        val plugins = mapOf(
            "com.a.plugin" to manifest(
                "com.a.plugin",
                deps = listOf(PluginDependency("com.missing.optional", "^1.0.0", optional = true))
            )
        )

        val result = resolver.resolveDependencies("com.a.plugin", plugins)

        assertIs<DependencyResolver.ResolutionResult.Success>(result)
    }
}

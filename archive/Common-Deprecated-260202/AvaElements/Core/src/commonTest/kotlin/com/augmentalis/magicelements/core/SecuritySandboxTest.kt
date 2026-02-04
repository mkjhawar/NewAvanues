package com.augmentalis.avaelements.core

import com.augmentalis.avaelements.core.types.*

import com.augmentalis.avaelements.core.runtime.*
import kotlin.test.*

/**
 * Test suite for SecuritySandbox
 *
 * Tests security sandbox functionality: environment isolation, permission enforcement,
 * resource limits, and security policies.
 * Coverage: Sandbox creation, permission validation, resource limits, policy enforcement
 */
class SecuritySandboxTest {

    private lateinit var sandbox: SecuritySandbox

    @BeforeTest
    fun setUp() {
        sandbox = SecuritySandbox()
    }

    @AfterTest
    fun tearDown() {
        // Clean up any environments
    }

    // ==================== Environment Creation Tests ====================

    @Test
    fun should_createEnvironment_when_validPermissions() {
        // Given
        val permissions = setOf(
            Permission.READ_THEME,
            Permission.READ_USER_PREFERENCES
        )
        val limits = ResourceLimits.default()

        // When
        val environment = sandbox.createIsolatedEnvironment(
            pluginId = "test.plugin",
            permissions = permissions,
            resourceLimits = limits
        )

        // Then
        assertNotNull(environment)
        assertEquals("test.plugin", environment.pluginId)
        assertEquals(permissions, environment.permissions)
        assertEquals(limits, environment.resourceLimits)
        assertEquals(NetworkPolicy.NONE, environment.networkPolicy)
        assertEquals(FileSystemAccess.NONE, environment.fileSystemAccess)
        assertEquals(ReflectionPolicy.RESTRICTED, environment.reflectionPolicy)
    }

    @Test
    fun should_allowEnvironmentCreation_when_noBlacklistedPermissions() {
        // Given - Currently BLACKLISTED is empty, so all defined permissions are allowed
        val permissions = setOf(
            Permission.READ_THEME,
            Permission.READ_USER_PREFERENCES,
            Permission.SHOW_NOTIFICATION,
            Permission.ACCESS_CLIPBOARD
        )

        // When - Should not throw since no permissions are currently blacklisted
        val environment = sandbox.createIsolatedEnvironment(
            pluginId = "safe.plugin",
            permissions = permissions,
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertNotNull(environment)
        assertEquals(permissions, environment.permissions)
    }

    @Test
    fun should_createMultipleEnvironments_when_differentPlugins() {
        // Given
        val permissions1 = setOf(Permission.READ_THEME)
        val permissions2 = setOf(Permission.READ_USER_PREFERENCES)

        // When
        val env1 = sandbox.createIsolatedEnvironment(
            pluginId = "plugin1",
            permissions = permissions1,
            resourceLimits = ResourceLimits.default()
        )
        val env2 = sandbox.createIsolatedEnvironment(
            pluginId = "plugin2",
            permissions = permissions2,
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertNotEquals(env1.pluginId, env2.pluginId)
        assertEquals(permissions1, env1.permissions)
        assertEquals(permissions2, env2.permissions)
    }

    @Test
    fun should_storeEnvironment_when_created() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "stored.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // When
        val retrieved = sandbox.getEnvironment("stored.plugin")

        // Then
        assertNotNull(retrieved)
        assertEquals("stored.plugin", retrieved.pluginId)
    }

    // ==================== Permission Checking Tests ====================

    @Test
    fun should_returnTrue_when_permissionGranted() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "permitted.plugin",
            permissions = setOf(Permission.READ_THEME),
            resourceLimits = ResourceLimits.default()
        )

        // When
        val hasPermission = sandbox.hasPermission("permitted.plugin", Permission.READ_THEME)

        // Then
        assertTrue(hasPermission)
    }

    @Test
    fun should_returnFalse_when_permissionNotGranted() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "limited.plugin",
            permissions = setOf(Permission.READ_THEME),
            resourceLimits = ResourceLimits.default()
        )

        // When
        val hasPermission = sandbox.hasPermission("limited.plugin", Permission.READ_USER_PREFERENCES)

        // Then
        assertFalse(hasPermission)
    }

    @Test
    fun should_returnFalse_when_pluginNotFound() {
        // When
        val hasPermission = sandbox.hasPermission("nonexistent.plugin", Permission.READ_THEME)

        // Then
        assertFalse(hasPermission)
    }

    // ==================== Resource Limit Enforcement Tests ====================

    @Test
    fun should_allowExecution_when_withinMemoryLimit() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "compliant.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits(
                memory = 10_000_000,
                cpuTimeMs = 100,
                fileSize = 1_000_000,
                componentCount = 100,
                nestingDepth = 10
            )
        )

        val usage = ResourceUsage(
            memoryBytes = 5_000_000,  // Within limit
            componentCount = 50,
            nestingDepth = 5
        )

        // When/Then - Should not throw
        sandbox.enforceResourceLimits("compliant.plugin", usage)
    }

    @Test
    fun should_throwException_when_memoryLimitExceeded() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "memory.hog",
            permissions = emptySet(),
            resourceLimits = ResourceLimits(
                memory = 10_000_000,
                cpuTimeMs = 100,
                fileSize = 1_000_000,
                componentCount = 100,
                nestingDepth = 10
            )
        )

        val usage = ResourceUsage(
            memoryBytes = 15_000_000  // Exceeds limit
        )

        // When/Then
        assertFailsWith<PluginException.SecurityException> {
            sandbox.enforceResourceLimits("memory.hog", usage)
        }
    }

    @Test
    fun should_throwException_when_componentCountExceeded() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "component.spammer",
            permissions = emptySet(),
            resourceLimits = ResourceLimits(
                memory = 10_000_000,
                cpuTimeMs = 100,
                fileSize = 1_000_000,
                componentCount = 100,
                nestingDepth = 10
            )
        )

        val usage = ResourceUsage(
            componentCount = 150  // Exceeds limit
        )

        // When/Then
        assertFailsWith<PluginException.SecurityException> {
            sandbox.enforceResourceLimits("component.spammer", usage)
        }
    }

    @Test
    fun should_throwException_when_nestingDepthExceeded() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "deep.nester",
            permissions = emptySet(),
            resourceLimits = ResourceLimits(
                memory = 10_000_000,
                cpuTimeMs = 100,
                fileSize = 1_000_000,
                componentCount = 100,
                nestingDepth = 10
            )
        )

        val usage = ResourceUsage(
            nestingDepth = 15  // Exceeds limit
        )

        // When/Then
        assertFailsWith<PluginException.SecurityException> {
            sandbox.enforceResourceLimits("deep.nester", usage)
        }
    }

    @Test
    fun should_throwException_when_enforcingNonexistentPlugin() {
        // Given
        val usage = ResourceUsage()

        // When/Then
        assertFailsWith<PluginException.SecurityException> {
            sandbox.enforceResourceLimits("nonexistent.plugin", usage)
        }
    }

    // ==================== Environment Destruction Tests ====================

    @Test
    fun should_removeEnvironment_when_destroyed() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "temporary.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // When
        sandbox.destroy("temporary.plugin")

        // Then
        assertNull(sandbox.getEnvironment("temporary.plugin"))
    }

    @Test
    fun should_notThrow_when_destroyingNonexistentEnvironment() {
        // When/Then - Should not throw
        sandbox.destroy("nonexistent.plugin")
    }

    // ==================== Resource Limits Presets Tests ====================

    @Test
    fun should_provideDefaultLimits_when_requested() {
        // When
        val limits = ResourceLimits.default()

        // Then
        assertEquals(10_000_000, limits.memory)
        assertEquals(100, limits.cpuTimeMs)
        assertEquals(1_000_000, limits.fileSize)
        assertEquals(100, limits.componentCount)
        assertEquals(10, limits.nestingDepth)
    }

    @Test
    fun should_provideGenerousLimits_when_requested() {
        // When
        val limits = ResourceLimits.generous()

        // Then
        assertTrue(limits.memory > ResourceLimits.default().memory)
        assertTrue(limits.cpuTimeMs > ResourceLimits.default().cpuTimeMs)
        assertTrue(limits.fileSize > ResourceLimits.default().fileSize)
        assertTrue(limits.componentCount > ResourceLimits.default().componentCount)
        assertTrue(limits.nestingDepth > ResourceLimits.default().nestingDepth)
    }

    @Test
    fun should_provideStrictLimits_when_requested() {
        // When
        val limits = ResourceLimits.strict()

        // Then
        assertTrue(limits.memory < ResourceLimits.default().memory)
        assertTrue(limits.cpuTimeMs < ResourceLimits.default().cpuTimeMs)
        assertTrue(limits.fileSize < ResourceLimits.default().fileSize)
        assertTrue(limits.componentCount < ResourceLimits.default().componentCount)
        assertTrue(limits.nestingDepth < ResourceLimits.default().nestingDepth)
    }

    // ==================== Network Policy Tests ====================

    @Test
    fun should_haveNetworkPolicy_when_environmentCreated() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "networked.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertEquals(NetworkPolicy.NONE, env.networkPolicy)
    }

    @Test
    fun should_supportAllNetworkPolicies_when_checking() {
        // When/Then
        val policies = NetworkPolicy.values()
        assertEquals(4, policies.size)
        assertTrue(policies.contains(NetworkPolicy.NONE))
        assertTrue(policies.contains(NetworkPolicy.READ_ONLY))
        assertTrue(policies.contains(NetworkPolicy.WHITELIST))
        assertTrue(policies.contains(NetworkPolicy.FULL))
    }

    // ==================== File System Access Tests ====================

    @Test
    fun should_haveFileSystemAccess_when_environmentCreated() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "filesystem.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertEquals(FileSystemAccess.NONE, env.fileSystemAccess)
    }

    @Test
    fun should_supportAllFileSystemPolicies_when_checking() {
        // When/Then
        val policies = FileSystemAccess.values()
        assertEquals(4, policies.size)
        assertTrue(policies.contains(FileSystemAccess.NONE))
        assertTrue(policies.contains(FileSystemAccess.PLUGIN_DIR))
        assertTrue(policies.contains(FileSystemAccess.TEMP_DIR))
        assertTrue(policies.contains(FileSystemAccess.FULL))
    }

    // ==================== Reflection Policy Tests ====================

    @Test
    fun should_haveReflectionPolicy_when_environmentCreated() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "reflective.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertEquals(ReflectionPolicy.RESTRICTED, env.reflectionPolicy)
    }

    @Test
    fun should_supportAllReflectionPolicies_when_checking() {
        // When/Then
        val policies = ReflectionPolicy.values()
        assertEquals(3, policies.size)
        assertTrue(policies.contains(ReflectionPolicy.NONE))
        assertTrue(policies.contains(ReflectionPolicy.RESTRICTED))
        assertTrue(policies.contains(ReflectionPolicy.FULL))
    }

    // ==================== Allowed APIs Tests ====================

    @Test
    fun should_includeThemeAPI_when_readThemePermissionGranted() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "theme.plugin",
            permissions = setOf(Permission.READ_THEME),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertTrue(env.allowedAPIs.contains("com.augmentalis.avaelements.core.Theme"))
    }

    @Test
    fun should_includePreferencesAPI_when_readPreferencesPermissionGranted() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "prefs.plugin",
            permissions = setOf(Permission.READ_USER_PREFERENCES),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertTrue(env.allowedAPIs.any { it.contains("preferences") })
    }

    @Test
    fun should_alwaysIncludeCoreAPIs_when_environmentCreated() {
        // Given
        val env = sandbox.createIsolatedEnvironment(
            pluginId = "minimal.plugin",
            permissions = emptySet(),
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertTrue(env.allowedAPIs.contains("com.augmentalis.avaelements.core.*"))
    }

    // ==================== Permission Blacklist Tests ====================

    @Test
    fun should_haveEmptyBlacklist_when_checking() {
        // When - Currently no permissions are blacklisted
        val blacklisted = Permission.BLACKLISTED

        // Then
        assertTrue(blacklisted.isEmpty())
    }

    @Test
    fun should_allowAllDefinedPermissions_when_blacklistEmpty() {
        // Given - All currently defined permissions
        val allPermissions = setOf(
            Permission.READ_THEME,
            Permission.READ_USER_PREFERENCES,
            Permission.SHOW_NOTIFICATION,
            Permission.ACCESS_CLIPBOARD
        )

        // When/Then - Should not throw since blacklist is empty
        val environment = sandbox.createIsolatedEnvironment(
            pluginId = "all.permissions.plugin",
            permissions = allPermissions,
            resourceLimits = ResourceLimits.default()
        )

        // Then
        assertNotNull(environment)
        assertEquals(allPermissions, environment.permissions)
    }

    // ==================== ResourceUsage Tests ====================

    @Test
    fun should_createDefaultResourceUsage_when_noParamsProvided() {
        // When
        val usage = ResourceUsage()

        // Then
        assertEquals(0, usage.memoryBytes)
        assertEquals(0, usage.cpuTimeMs)
        assertEquals(0, usage.componentCount)
        assertEquals(0, usage.nestingDepth)
    }

    @Test
    fun should_createResourceUsage_when_valuesProvided() {
        // When
        val usage = ResourceUsage(
            memoryBytes = 5_000_000,
            cpuTimeMs = 50,
            componentCount = 25,
            nestingDepth = 5
        )

        // Then
        assertEquals(5_000_000, usage.memoryBytes)
        assertEquals(50, usage.cpuTimeMs)
        assertEquals(25, usage.componentCount)
        assertEquals(5, usage.nestingDepth)
    }

    // ==================== Isolation Tests ====================

    @Test
    fun should_isolateEnvironments_when_multiplePlugins() {
        // Given
        sandbox.createIsolatedEnvironment(
            pluginId = "plugin1",
            permissions = setOf(Permission.READ_THEME),
            resourceLimits = ResourceLimits.default()
        )
        sandbox.createIsolatedEnvironment(
            pluginId = "plugin2",
            permissions = setOf(Permission.READ_USER_PREFERENCES),
            resourceLimits = ResourceLimits.generous()
        )

        // When
        val env1 = sandbox.getEnvironment("plugin1")
        val env2 = sandbox.getEnvironment("plugin2")

        // Then
        assertNotNull(env1)
        assertNotNull(env2)
        assertNotEquals(env1.permissions, env2.permissions)
        assertNotEquals(env1.resourceLimits, env2.resourceLimits)
    }
}

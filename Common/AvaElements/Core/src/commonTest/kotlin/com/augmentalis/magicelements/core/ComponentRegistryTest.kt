package com.augmentalis.avaelements.core

import com.augmentalis.avaelements.core.types.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Test suite for ComponentRegistry
 *
 * Tests component registration, lookup, and lifecycle management.
 * Coverage: Component registration, duplicate handling, unregistration, plugin management
 */
class ComponentRegistryTest {

    private lateinit var testComponent: ComponentDefinition
    private lateinit var testComponent2: ComponentDefinition
    private var registryEventFired = false

    @BeforeTest
    fun setUp() = runTest {
        // Clear registry before each test
        ComponentRegistry.clear()
        registryEventFired = false

        // Create test component definitions
        testComponent = ComponentDefinition(
            type = "TestComponent",
            factory = ComponentFactory { config ->
                TestComponent(id = config.id, text = config.properties["text"] as? String ?: "")
            }
        )

        testComponent2 = ComponentDefinition(
            type = "TestComponent2",
            factory = ComponentFactory { config ->
                TestComponent(id = config.id, text = config.properties["text"] as? String ?: "")
            }
        )
    }

    @AfterTest
    fun tearDown() = runTest {
        ComponentRegistry.clear()
    }

    // ==================== Registration Tests ====================

    @Test
    fun should_registerComponent_when_newType() = runTest {
        // When
        ComponentRegistry.register(testComponent)

        // Then
        assertTrue(ComponentRegistry.contains("TestComponent"))
        assertEquals(testComponent, ComponentRegistry.get("TestComponent"))
    }

    @Test
    fun should_throwException_when_duplicateRegistration() = runTest {
        // Given
        ComponentRegistry.register(testComponent)

        // When/Then
        assertFails {
            ComponentRegistry.register(testComponent)
        }
    }

    @Test
    fun should_registerMultipleComponents_when_differentTypes() = runTest {
        // When
        ComponentRegistry.register(testComponent)
        ComponentRegistry.register(testComponent2)

        // Then
        assertEquals(2, ComponentRegistry.getAllTypes().size)
        assertTrue(ComponentRegistry.contains("TestComponent"))
        assertTrue(ComponentRegistry.contains("TestComponent2"))
    }

    @Test
    fun should_registerWithPluginId_when_pluginProvided() = runTest {
        // When
        ComponentRegistry.register(testComponent, pluginId = "test.plugin")

        // Then
        assertTrue(ComponentRegistry.contains("TestComponent"))
        assertEquals(setOf("TestComponent"), ComponentRegistry.getPluginTypes("test.plugin"))
    }

    @Test
    fun should_trackMultiplePluginComponents_when_samePluginRegistersMultiple() = runTest {
        // When
        ComponentRegistry.register(testComponent, pluginId = "test.plugin")
        ComponentRegistry.register(testComponent2, pluginId = "test.plugin")

        // Then
        val pluginTypes = ComponentRegistry.getPluginTypes("test.plugin")
        assertEquals(2, pluginTypes.size)
        assertTrue(pluginTypes.contains("TestComponent"))
        assertTrue(pluginTypes.contains("TestComponent2"))
    }

    // ==================== Lookup Tests ====================

    @Test
    fun should_returnNull_when_componentNotRegistered() = runTest {
        // When
        val result = ComponentRegistry.get("NonExistent")

        // Then
        assertNull(result)
    }

    @Test
    fun should_returnFalse_when_checkingNonExistentComponent() = runTest {
        // When/Then
        assertFalse(ComponentRegistry.contains("NonExistent"))
    }

    @Test
    fun should_returnAllTypes_when_multipleComponentsRegistered() = runTest {
        // Given
        ComponentRegistry.register(testComponent)
        ComponentRegistry.register(testComponent2)

        // When
        val types = ComponentRegistry.getAllTypes()

        // Then
        assertEquals(2, types.size)
        assertTrue(types.contains("TestComponent"))
        assertTrue(types.contains("TestComponent2"))
    }

    @Test
    fun should_returnEmptySet_when_pluginHasNoComponents() = runTest {
        // When
        val types = ComponentRegistry.getPluginTypes("nonexistent.plugin")

        // Then
        assertTrue(types.isEmpty())
    }

    // ==================== Unregistration Tests ====================

    @Test
    fun should_unregisterComponent_when_typeExists() = runTest {
        // Given
        ComponentRegistry.register(testComponent)

        // When
        val result = ComponentRegistry.unregister("TestComponent")

        // Then
        assertTrue(result)
        assertFalse(ComponentRegistry.contains("TestComponent"))
    }

    @Test
    fun should_returnFalse_when_unregisteringNonExistentComponent() = runTest {
        // When
        val result = ComponentRegistry.unregister("NonExistent")

        // Then
        assertFalse(result)
    }

    @Test
    fun should_unregisterAllPluginComponents_when_unregisteringByPluginId() = runTest {
        // Given
        ComponentRegistry.register(testComponent, pluginId = "test.plugin")
        ComponentRegistry.register(testComponent2, pluginId = "test.plugin")

        // When
        val count = ComponentRegistry.unregisterAll("test.plugin")

        // Then
        assertEquals(2, count)
        assertFalse(ComponentRegistry.contains("TestComponent"))
        assertFalse(ComponentRegistry.contains("TestComponent2"))
        assertTrue(ComponentRegistry.getPluginTypes("test.plugin").isEmpty())
    }

    @Test
    fun should_returnZero_when_unregisteringNonExistentPlugin() = runTest {
        // When
        val count = ComponentRegistry.unregisterAll("nonexistent.plugin")

        // Then
        assertEquals(0, count)
    }

    @Test
    fun should_onlyUnregisterPluginComponents_when_mixedRegistration() = runTest {
        // Given
        ComponentRegistry.register(testComponent, pluginId = "plugin1")
        ComponentRegistry.register(testComponent2) // No plugin

        // When
        ComponentRegistry.unregisterAll("plugin1")

        // Then
        assertFalse(ComponentRegistry.contains("TestComponent"))
        assertTrue(ComponentRegistry.contains("TestComponent2"))
    }

    // ==================== Component Creation Tests ====================

    @Test
    fun should_createComponent_when_typeRegistered() = runTest {
        // Given
        ComponentRegistry.register(testComponent)
        val config = ComponentConfig(
            id = "test1",
            type = "TestComponent",
            properties = mapOf("text" to "Hello")
        )

        // When
        val component = ComponentRegistry.create("TestComponent", config)

        // Then
        assertNotNull(component)
        assertTrue(component is TestComponent)
        assertEquals("test1", component.id)
    }

    @Test
    fun should_throwException_when_creatingUnregisteredComponent() = runTest {
        // Given
        val config = ComponentConfig(id = "test1", type = "NonExistent")

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            ComponentRegistry.create("NonExistent", config)
        }
    }

    @Test
    fun should_validateComponent_when_validatorProvided() = runTest {
        // Given
        val validatingComponent = ComponentDefinition(
            type = "ValidatingComponent",
            factory = ComponentFactory { config ->
                TestComponent(id = config.id, text = config.properties["text"] as? String ?: "")
            },
            validator = ComponentValidator { config ->
                if (config.properties["required"] == null) {
                    ValidationResult.failure(ValidationError(field = "required", message = "Required field missing"))
                } else {
                    ValidationResult.success()
                }
            }
        )
        ComponentRegistry.register(validatingComponent)

        val invalidConfig = ComponentConfig(id = "test1", type = "ValidatingComponent")

        // When/Then
        assertFailsWith<PluginException.ValidationException> {
            ComponentRegistry.create("ValidatingComponent", invalidConfig)
        }
    }

    @Test
    fun should_createComponent_when_validationPasses() = runTest {
        // Given
        val validatingComponent = ComponentDefinition(
            type = "ValidatingComponent",
            factory = ComponentFactory { config ->
                TestComponent(id = config.id, text = config.properties["text"] as? String ?: "")
            },
            validator = ComponentValidator { config ->
                if (config.properties["required"] == null) {
                    ValidationResult.failure(ValidationError(field = "required", message = "Required field missing"))
                } else {
                    ValidationResult.success()
                }
            }
        )
        ComponentRegistry.register(validatingComponent)

        val validConfig = ComponentConfig(
            id = "test1",
            type = "ValidatingComponent",
            properties = mapOf("required" to "value")
        )

        // When
        val component = ComponentRegistry.create("ValidatingComponent", validConfig)

        // Then
        assertNotNull(component)
    }

    // ==================== Event Listener Tests ====================

    @Test
    fun should_notifyListeners_when_componentRegistered() = runTest {
        // Given
        var receivedEvent: RegistryEvent? = null
        val listener = RegistryListener { event ->
            receivedEvent = event
        }
        ComponentRegistry.addListener(listener)

        // When
        ComponentRegistry.register(testComponent, pluginId = "test.plugin")

        // Then
        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is RegistryEvent.Registered)
        assertEquals("TestComponent", (receivedEvent as RegistryEvent.Registered).type)
        assertEquals("test.plugin", (receivedEvent as RegistryEvent.Registered).pluginId)
    }

    @Test
    fun should_notifyListeners_when_componentUnregistered() = runTest {
        // Given
        ComponentRegistry.register(testComponent)
        var receivedEvent: RegistryEvent? = null
        val listener = RegistryListener { event ->
            receivedEvent = event
        }
        ComponentRegistry.addListener(listener)

        // When
        ComponentRegistry.unregister("TestComponent")

        // Then
        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is RegistryEvent.Unregistered)
        assertEquals("TestComponent", (receivedEvent as RegistryEvent.Unregistered).type)
    }

    @Test
    fun should_notifyListeners_when_pluginReloaded() = runTest {
        // Given
        var receivedEvent: RegistryEvent? = null
        val listener = RegistryListener { event ->
            receivedEvent = event
        }
        ComponentRegistry.addListener(listener)

        // When
        ComponentRegistry.notifyReload("test.plugin")

        // Then
        assertNotNull(receivedEvent)
        assertTrue(receivedEvent is RegistryEvent.PluginReloaded)
        assertEquals("test.plugin", (receivedEvent as RegistryEvent.PluginReloaded).pluginId)
    }

    @Test
    fun should_notNotifyListeners_when_listenerRemoved() = runTest {
        // Given
        var eventCount = 0
        val listener = RegistryListener { event ->
            eventCount++
        }
        ComponentRegistry.addListener(listener)
        ComponentRegistry.register(testComponent)
        ComponentRegistry.removeListener(listener)

        // When
        ComponentRegistry.register(testComponent2)

        // Then
        assertEquals(1, eventCount) // Only first registration counted
    }

    // ==================== Thread Safety Tests ====================

    @Test
    fun should_handleConcurrentRegistrations_when_multipleThreads() = runTest {
        // Note: This is a basic test. Real concurrency testing would be more complex
        // Given
        val components = (1..10).map { i ->
            ComponentDefinition(
                type = "Component$i",
                factory = ComponentFactory { config ->
                    TestComponent(id = config.id, text = config.properties["text"] as? String ?: "")
                }
            )
        }

        // When
        components.forEach { ComponentRegistry.register(it) }

        // Then
        assertEquals(10, ComponentRegistry.getAllTypes().size)
    }

    // ==================== Helper Classes ====================

    private data class TestComponent(
        override val type: String = "TestComponent",
        override val id: String?,
        val text: String,
        override val style: ComponentStyle? = null,
        override val modifiers: List<Modifier> = emptyList()
    ) : Component {
        override fun render(renderer: Renderer): Any {
            return "Rendered: $text"
        }
    }
}

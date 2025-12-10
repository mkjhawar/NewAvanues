/**
 * UUIDViewModelTest.kt - Unit tests for UUIDViewModel
 * 
 * Tests UUID management, element registration, and command processing
 * 
 * Author: VOS4 Development Team  
 * Created: 2025-01-02
 */
package com.augmentalis.uuidmanager.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.augmentalis.uuidmanager.UUIDManager
import com.augmentalis.uuidmanager.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UUIDViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var uuidManager: UUIDManager
    
    @Mock
    private lateinit var uiStateObserver: Observer<UUIDUiState>
    
    private lateinit var viewModel: UUIDViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup mock manager
        `when`(uuidManager.generateUUID()).thenReturn("test-uuid-12345")
        `when`(uuidManager.getAllElements()).thenReturn(emptyList())
        
        viewModel = UUIDViewModel(uuidManager)
        viewModel.uiState.observeForever(uiStateObserver)
    }
    
    @After
    fun tearDown() {
        viewModel.uiState.removeObserver(uiStateObserver)
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test initial UI state`() {
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertTrue(state.registeredElements.isNotEmpty()) // Has mock data
        assertFalse(state.voiceCommandActive)
        assertEquals("", state.currentCommand)
    }
    
    @Test
    fun `test generate new UUID`() {
        val uuid = viewModel.generateNewUUID()
        assertTrue(uuid.isNotEmpty())
        assertTrue(uuid.contains("-"))
    }
    
    @Test
    fun `test register new element`() = runTest {
        val initialCount = viewModel.uiState.value?.registeredElements?.size ?: 0
        
        viewModel.registerNewElement("TestButton", "button")
        advanceUntilIdle()
        
        val newCount = viewModel.uiState.value?.registeredElements?.size ?: 0
        assertTrue(newCount > initialCount)
    }
    
    @Test
    fun `test select element updates state`() {
        val elements = viewModel.uiState.value?.registeredElements ?: emptyList()
        if (elements.isNotEmpty()) {
            val element = elements.first()
            viewModel.selectElement(element)
            
            assertEquals(element, viewModel.uiState.value?.selectedElement)
            assertTrue(viewModel.uiState.value?.navigationPath?.isNotEmpty() ?: false)
        }
    }
    
    @Test
    fun `test clear selection`() {
        val elements = viewModel.uiState.value?.registeredElements ?: emptyList()
        if (elements.isNotEmpty()) {
            viewModel.selectElement(elements.first())
            viewModel.clearSelection()
            
            assertEquals(null, viewModel.uiState.value?.selectedElement)
            assertTrue(viewModel.uiState.value?.navigationPath?.isEmpty() ?: true)
        }
    }
    
    @Test
    fun `test process voice command`() = runTest {
        viewModel.processVoiceCommand("click submit button")
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state?.voiceCommandActive ?: true)
        assertNotNull(state?.commandResult)
        assertTrue(state?.commandHistory?.isNotEmpty() ?: false)
    }
    
    @Test
    fun `test search elements`() {
        viewModel.searchElements("Submit")
        
        val state = viewModel.uiState.value
        assertEquals("Submit", state?.searchQuery)
        // Search results depend on mock data containing "Submit"
    }
    
    @Test
    fun `test filter by type`() {
        viewModel.filterByType("button")
        
        val state = viewModel.uiState.value
        assertEquals("button", state?.filterType)
        // Filtered elements should only be buttons
        state?.registeredElements?.forEach { element ->
            if (state.filterType != "all") {
                assertEquals("button", element.type)
            }
        }
    }
    
    @Test
    fun `test registry statistics calculation`() {
        viewModel.refreshRegistry()
        
        val stats = viewModel.uiState.value?.registryStats
        assertNotNull(stats)
        assertTrue(stats.totalElements >= 0)
        assertTrue(stats.activeElements >= 0)
        assertTrue(stats.activeElements <= stats.totalElements)
    }
    
    @Test
    fun `test command history management`() = runTest {
        // Process multiple commands
        viewModel.processVoiceCommand("click button")
        advanceUntilIdle()
        viewModel.processVoiceCommand("select first")
        advanceUntilIdle()
        
        val history = viewModel.uiState.value?.commandHistory ?: emptyList()
        assertTrue(history.size >= 2)
        // Most recent command should be first
        assertEquals("select first", history.firstOrNull()?.command)
    }
    
    @Test
    fun `test spatial navigation`() = runTest {
        val elements = viewModel.uiState.value?.registeredElements ?: emptyList()
        if (elements.isNotEmpty()) {
            viewModel.selectElement(elements.first())
            viewModel.navigateToElement("right")
            advanceUntilIdle()
            
            // Navigation may or may not change selection based on layout
            assertNotNull(viewModel.uiState.value?.selectedElement)
        }
    }
    
    @Test
    fun `test export registry`() {
        val exportText = viewModel.exportRegistry()
        
        assertTrue(exportText.isNotEmpty())
        assertTrue(exportText.contains("UUID Registry Export"))
        assertTrue(exportText.contains("Statistics"))
        assertTrue(exportText.contains("Elements"))
    }
}
/**
 * ViewModelTestTemplate.kt - Advanced ViewModel testing template
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: {{DATE}}
 * 
 * Comprehensive ViewModel testing with LiveData, Coroutines, and StateFlow
 */
package {{PACKAGE_NAME}}

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import kotlin.system.measureTimeMillis
import kotlin.test.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class {{CLASS_NAME}}Test {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    // Coroutine test setup
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mocks
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSavedStateHandle: SavedStateHandle
    
    {{MOCK_DECLARATIONS}}
    
    // Observers
    @Mock
    private lateinit var loadingObserver: Observer<Boolean>
    
    @Mock
    private lateinit var errorObserver: Observer<String?>
    
    {{OBSERVER_DECLARATIONS}}
    
    // Subject under test
    private lateinit var viewModel: {{CLASS_NAME}}
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock configurations
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        {{MOCK_SETUP}}
        
        // Initialize ViewModel
        viewModel = {{CLASS_NAME}}(
            {{CONSTRUCTOR_PARAMS}}
        )
        
        // Attach observers
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.errorMessage.observeForever(errorObserver)
        {{OBSERVER_SETUP}}
    }
    
    @After
    fun tearDown() {
        // Remove observers
        viewModel.isLoading.removeObserver(loadingObserver)
        viewModel.errorMessage.removeObserver(errorObserver)
        {{OBSERVER_CLEANUP}}
        
        // Reset dispatcher
        Dispatchers.resetMain()
    }
    
    // ========== Initial State Tests ==========
    
    @Test
    fun `test initial state is correct`() {
        // Assert initial values
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
        {{INITIAL_STATE_ASSERTIONS}}
    }
    
    @Test
    fun `test saved state restoration`() {
        // Given saved state data
        `when`(mockSavedStateHandle.get<String>("key")).thenReturn("saved_value")
        
        // When ViewModel is recreated
        val restoredViewModel = {{CLASS_NAME}}(mockSavedStateHandle)
        
        // Then state should be restored
        {{SAVED_STATE_ASSERTIONS}}
    }
    
    // ========== Business Logic Tests ==========
    
    {{METHOD_TESTS}}
    
    // ========== Loading State Tests ==========
    
    @Test
    fun `test loading state during async operation`() = testScope.runTest {
        // When async operation starts
        viewModel.performAsyncOperation()
        
        // Then loading should be true
        verify(loadingObserver).onChanged(true)
        
        // Advance time to complete operation
        advanceUntilIdle()
        
        // Then loading should be false
        verify(loadingObserver).onChanged(false)
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `test error handling for network failure`() = testScope.runTest {
        // Given network failure
        `when`(mockRepository.fetchData()).thenThrow(NetworkException("Connection failed"))
        
        // When operation is performed
        viewModel.loadData()
        advanceUntilIdle()
        
        // Then error should be set
        verify(errorObserver).onChanged(argThat { it?.contains("Connection failed") == true })
    }
    
    @Test
    fun `test error recovery after retry`() = testScope.runTest {
        // Given initial failure then success
        `when`(mockRepository.fetchData())
            .thenThrow(NetworkException("Failed"))
            .thenReturn(mockData)
        
        // When operation fails then retries
        viewModel.loadData()
        advanceUntilIdle()
        
        viewModel.retry()
        advanceUntilIdle()
        
        // Then error should be cleared
        verify(errorObserver, atLeastOnce()).onChanged(null)
    }
    
    // ========== StateFlow/Flow Tests ==========
    
    @Test
    fun `test StateFlow emissions`() = testScope.runTest {
        // Given StateFlow in ViewModel
        val emissions = mutableListOf<State>()
        val job = viewModel.uiState.collect { emissions.add(it) }
        
        // When state changes
        viewModel.updateState()
        advanceUntilIdle()
        
        // Then emissions should be collected
        assertTrue(emissions.size >= 2)
        {{STATEFLOW_ASSERTIONS}}
        
        job.cancel()
    }
    
    @Test
    fun `test Flow transformation`() = testScope.runTest {
        // When flow is transformed
        val result = viewModel.transformedFlow.first()
        
        // Then transformation should be applied
        {{FLOW_ASSERTIONS}}
    }
    
    // ========== Coroutine Scope Tests ==========
    
    @Test
    fun `test coroutine cancellation on ViewModel clear`() = testScope.runTest {
        // Given long-running operation
        viewModel.startLongRunningOperation()
        
        // When ViewModel is cleared
        viewModel.onCleared()
        
        // Then coroutines should be cancelled
        advanceUntilIdle()
        {{CANCELLATION_ASSERTIONS}}
    }
    
    @Test
    fun `test concurrent operations handling`() = testScope.runTest {
        // When multiple operations are triggered
        repeat(10) {
            viewModel.performOperation()
        }
        
        // Then they should be handled correctly
        advanceUntilIdle()
        {{CONCURRENCY_ASSERTIONS}}
    }
    
    // ========== Performance Tests ==========
    
    @Test
    fun `test operation completes within performance threshold`() = testScope.runTest {
        val executionTime = measureTimeMillis {
            viewModel.performOperation()
            advanceUntilIdle()
        }
        
        assertTrue(
            executionTime < 1000,
            "Operation should complete within 1s, took ${executionTime}ms"
        )
    }
    
    @Test
    fun `test memory efficiency under load`() = testScope.runTest {
        val initialMemory = Runtime.getRuntime().freeMemory()
        
        // Perform multiple operations
        repeat(100) {
            viewModel.performOperation()
        }
        advanceUntilIdle()
        
        val finalMemory = Runtime.getRuntime().freeMemory()
        val memoryUsed = initialMemory - finalMemory
        
        assertTrue(
            memoryUsed < 10_000_000, // 10MB
            "Memory usage should be under 10MB, used ${memoryUsed / 1_000_000}MB"
        )
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `test handles null input gracefully`() = testScope.runTest {
        // When null is passed
        viewModel.processInput(null)
        advanceUntilIdle()
        
        // Then should handle gracefully
        verify(errorObserver, never()).onChanged(argThat { it?.contains("NullPointer") == true })
    }
    
    @Test
    fun `test handles empty data set`() = testScope.runTest {
        // Given empty data
        `when`(mockRepository.getData()).thenReturn(emptyList())
        
        // When data is loaded
        viewModel.loadData()
        advanceUntilIdle()
        
        // Then should show appropriate state
        {{EMPTY_STATE_ASSERTIONS}}
    }
    
    // ========== Integration Tests ==========
    
    @Test
    fun `test complete user flow`() = testScope.runTest {
        // Simulate complete user interaction flow
        viewModel.onUserLogin("testuser")
        advanceUntilIdle()
        
        viewModel.loadUserData()
        advanceUntilIdle()
        
        viewModel.updateProfile("New Name")
        advanceUntilIdle()
        
        viewModel.logout()
        advanceUntilIdle()
        
        // Verify flow completed successfully
        {{USER_FLOW_ASSERTIONS}}
    }
    
    // ========== Property-Based Tests ==========
    
    @Test
    fun `test invariants hold for all states`() = testScope.runTest {
        // Test that certain properties always hold
        val states = listOf(
            State.Loading,
            State.Success(data),
            State.Error("error"),
            State.Empty
        )
        
        states.forEach { state ->
            viewModel.setState(state)
            advanceUntilIdle()
            
            // Invariants that should always be true
            assertTrue(viewModel.isValid())
            assertNotNull(viewModel.currentState)
            {{INVARIANT_ASSERTIONS}}
        }
    }
}
/**
 * SmokeTest.kt - Basic smoke test to verify test infrastructure
 * 
 * Simple test to ensure testing framework is working correctly
 */
package com.augmentalis.speechrecognition

import org.junit.Test
import org.junit.Assert.*

/**
 * Basic smoke test to verify test infrastructure works
 */
class SmokeTest {
    
    @Test
    fun testBasicMath() {
        val result = 2 + 2
        assertEquals(4, result)
    }
    
    @Test
    fun testStringOperation() {
        val greeting = "Hello"
        val name = "World"
        val result = "$greeting $name"
        assertEquals("Hello World", result)
    }
    
    @Test
    fun testBooleanLogic() {
        assertTrue(true)
        assertFalse(false)
        assertTrue(5 > 3)
    }
    
    @Test
    fun testNullHandling() {
        val value: String? = null
        assertNull(value)
        
        val nonNullValue = "test"
        assertNotNull(nonNullValue)
    }
    
    @Test
    fun testCollections() {
        val list = listOf("a", "b", "c")
        assertEquals(3, list.size)
        assertTrue(list.contains("b"))
        assertFalse(list.contains("d"))
    }
}
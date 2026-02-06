package com.augmentalis.voiceoscore.dsl.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxConfigTest {

    @Test
    fun default_hasExpectedLimits() {
        val config = SandboxConfig.DEFAULT
        assertEquals(10_000L, config.maxExecutionTimeMs)
        assertEquals(1_000, config.maxSteps)
        assertEquals(100, config.maxLoopIterations)
        assertEquals(10, config.maxNestingDepth)
        assertEquals(100, config.maxVariables)
    }

    @Test
    fun strict_hasLowerLimits() {
        val config = SandboxConfig.STRICT
        assertTrue(config.maxExecutionTimeMs < SandboxConfig.DEFAULT.maxExecutionTimeMs)
        assertTrue(config.maxSteps < SandboxConfig.DEFAULT.maxSteps)
        assertTrue(config.maxLoopIterations < SandboxConfig.DEFAULT.maxLoopIterations)
    }

    @Test
    fun system_hasHigherLimits() {
        val config = SandboxConfig.SYSTEM
        assertTrue(config.maxExecutionTimeMs > SandboxConfig.DEFAULT.maxExecutionTimeMs)
        assertTrue(config.maxSteps > SandboxConfig.DEFAULT.maxSteps)
        assertTrue(config.maxLoopIterations > SandboxConfig.DEFAULT.maxLoopIterations)
    }

    @Test
    fun custom_acceptsCustomValues() {
        val config = SandboxConfig(
            maxExecutionTimeMs = 500,
            maxSteps = 50,
            maxLoopIterations = 5,
            maxNestingDepth = 2,
            maxVariables = 10
        )
        assertEquals(500L, config.maxExecutionTimeMs)
        assertEquals(50, config.maxSteps)
        assertEquals(5, config.maxLoopIterations)
        assertEquals(2, config.maxNestingDepth)
        assertEquals(10, config.maxVariables)
    }
}

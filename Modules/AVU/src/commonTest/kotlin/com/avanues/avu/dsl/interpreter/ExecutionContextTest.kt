package com.avanues.avu.dsl.interpreter

import com.avanues.avu.dsl.ast.SourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ExecutionContextTest {

    private val loc = SourceLocation(1, 1)

    // ==================== Variable Scope Tests ====================

    @Test
    fun setVariable_createsInCurrentScope() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        ctx.setVariable("x", 42, loc)
        assertTrue(ctx.hasVariable("x"))
        assertEquals(42, ctx.getVariable("x"))
    }

    @Test
    fun getVariable_returnsNullForUndefined() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        assertFalse(ctx.hasVariable("undefined"))
        assertNull(ctx.getVariable("undefined"))
    }

    @Test
    fun pushScope_isolatesNewVariables() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        ctx.setVariable("outer", "hello", loc)
        ctx.pushScope()
        ctx.setVariable("inner", "world", loc)
        // Inner scope can see outer
        assertEquals("hello", ctx.getVariable("outer"))
        assertEquals("world", ctx.getVariable("inner"))
        // After pop, inner variable is gone
        ctx.popScope()
        assertEquals("hello", ctx.getVariable("outer"))
        assertFalse(ctx.hasVariable("inner"))
    }

    @Test
    fun setVariable_updatesExistingInOuterScope() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        ctx.setVariable("x", 1, loc)
        ctx.pushScope()
        // This should update in outer scope, not create in inner
        ctx.setVariable("x", 2, loc)
        ctx.popScope()
        assertEquals(2, ctx.getVariable("x"))
    }

    @Test
    fun popScope_doesNotRemoveGlobalScope() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        ctx.setVariable("x", 1, loc)
        ctx.popScope() // should be no-op for global scope
        assertEquals(1, ctx.getVariable("x"))
    }

    // ==================== Function Registry Tests ====================

    @Test
    fun registerFunction_canBeRetrieved() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        val funcDef = com.avanues.avu.dsl.ast.AvuAstNode.Declaration.FunctionDef(
            name = "myFunc",
            parameters = listOf("a", "b"),
            body = emptyList(),
            location = loc
        )
        ctx.registerFunction(funcDef)
        val retrieved = ctx.getFunction("myFunc")
        assertEquals("myFunc", retrieved?.name)
        assertEquals(listOf("a", "b"), retrieved?.parameters)
    }

    @Test
    fun getFunction_returnsNullForUnregistered() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        assertNull(ctx.getFunction("nonexistent"))
    }

    // ==================== Event Listener Tests ====================

    @Test
    fun emitEvent_callsRegisteredListeners() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        var received: Any? = null
        ctx.addEventListener("test_event") { data -> received = data }
        ctx.emitEvent("test_event", "hello")
        assertEquals("hello", received)
    }

    @Test
    fun emitEvent_callsMultipleListeners() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        val results = mutableListOf<Any?>()
        ctx.addEventListener("evt") { results.add(it) }
        ctx.addEventListener("evt") { results.add(it) }
        ctx.emitEvent("evt", 42)
        assertEquals(2, results.size)
        assertEquals(42, results[0])
        assertEquals(42, results[1])
    }

    @Test
    fun emitEvent_doesNothingForUnregisteredEvent() {
        val ctx = ExecutionContext(SandboxConfig.DEFAULT)
        // Should not throw
        ctx.emitEvent("unknown", null)
    }

    // ==================== Sandbox Enforcement Tests ====================

    @Test
    fun incrementStep_throwsOnStepLimit() {
        val ctx = ExecutionContext(SandboxConfig(maxSteps = 3, maxExecutionTimeMs = 60_000))
        ctx.incrementStep(loc)
        ctx.incrementStep(loc)
        ctx.incrementStep(loc)
        assertFailsWith<RuntimeError.SandboxViolation> {
            ctx.incrementStep(loc)
        }
    }

    @Test
    fun enterNesting_throwsOnDepthLimit() {
        val ctx = ExecutionContext(SandboxConfig(maxNestingDepth = 2, maxExecutionTimeMs = 60_000))
        ctx.enterNesting(loc)
        ctx.enterNesting(loc)
        assertFailsWith<RuntimeError.SandboxViolation> {
            ctx.enterNesting(loc)
        }
    }

    @Test
    fun exitNesting_decrementsDepth() {
        val ctx = ExecutionContext(SandboxConfig(maxNestingDepth = 2, maxExecutionTimeMs = 60_000))
        ctx.enterNesting(loc)
        ctx.enterNesting(loc)
        ctx.exitNesting()
        // Should succeed now since we exited one level
        ctx.enterNesting(loc)
    }

    @Test
    fun checkLoopLimit_throwsOnExceed() {
        val ctx = ExecutionContext(SandboxConfig(maxLoopIterations = 5, maxExecutionTimeMs = 60_000))
        ctx.checkLoopLimit(5, loc) // exactly at limit is ok
        assertFailsWith<RuntimeError.SandboxViolation> {
            ctx.checkLoopLimit(6, loc)
        }
    }

    @Test
    fun setVariable_throwsOnVariableLimit() {
        val ctx = ExecutionContext(SandboxConfig(maxVariables = 3, maxExecutionTimeMs = 60_000))
        ctx.setVariable("a", 1, loc)
        ctx.setVariable("b", 2, loc)
        ctx.setVariable("c", 3, loc)
        assertFailsWith<RuntimeError.SandboxViolation> {
            ctx.setVariable("d", 4, loc)
        }
    }

    @Test
    fun setVariable_updatingExistingDoesNotCountAsNew() {
        val ctx = ExecutionContext(SandboxConfig(maxVariables = 2, maxExecutionTimeMs = 60_000))
        ctx.setVariable("a", 1, loc)
        ctx.setVariable("b", 2, loc)
        // Updating existing should NOT trigger limit
        ctx.setVariable("a", 10, loc)
        assertEquals(10, ctx.getVariable("a"))
    }
}

package com.avanues.avu.dsl.interpreter

import com.avanues.avu.dsl.ast.*
import com.avanues.avu.dsl.lexer.AvuDslLexer
import com.avanues.avu.dsl.parser.AvuDslParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AvuInterpreterTest {

    private val loc = SourceLocation(1, 1)

    // ==================== Helper: Parse source to AST ====================

    private fun parse(source: String): AvuDslFile {
        val tokens = AvuDslLexer(source).tokenize()
        val result = AvuDslParser(tokens).parse()
        if (result.hasErrors) {
            val errors = result.errors.joinToString("\n") { it.toString() }
            throw AssertionError("Parse errors:\n$errors")
        }
        return result.file
    }

    // ==================== Execute Workflow Tests ====================

    @Test
    fun execute_simpleWorkflow_dispatchesCodes() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "Test"
  VCM(id: "cmd1", action: "tap")
  VCM(id: "cmd2", action: "scroll")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(2, dispatcher.dispatched.size)
        assertEquals("VCM", dispatcher.dispatched[0].first)
        assertEquals("cmd1", dispatcher.dispatched[0].second["id"])
        assertEquals("tap", dispatcher.dispatched[0].second["action"])
        assertEquals("VCM", dispatcher.dispatched[1].first)
        assertEquals("cmd2", dispatcher.dispatched[1].second["id"])
    }

    @Test
    fun execute_workflowWithLog() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "LogTest"
  @log "Hello from test"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))
        assertIs<ExecutionResult.Success>(result)
    }

    // ==================== Variable Assignment ====================

    @Test
    fun execute_variableAssignmentAndUse() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "VarTest"
  @set greeting = "hello"
  VCM(id: "cmd", action: ${'$'}greeting)
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals("hello", dispatcher.dispatched[0].second["action"])
    }

    @Test
    fun execute_variableArithmetic() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "Arithmetic"
  @set x = 10
  @set y = 3
  @set sum = ${'$'}x + ${'$'}y
  @log "Sum: " + ${'$'}sum
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))
        assertIs<ExecutionResult.Success>(result)
    }

    // ==================== If/Else ====================

    @Test
    fun execute_ifTrue_executesThenBranch() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "IfTest"
  @set count = 5
  @if ${'$'}count > 3
    VCM(id: "then", action: "executed")
  @else
    VCM(id: "else", action: "executed")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(1, dispatcher.dispatched.size)
        assertEquals("then", dispatcher.dispatched[0].second["id"])
    }

    @Test
    fun execute_ifFalse_executesElseBranch() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "IfTest"
  @set count = 1
  @if ${'$'}count > 3
    VCM(id: "then", action: "executed")
  @else
    VCM(id: "else", action: "executed")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(1, dispatcher.dispatched.size)
        assertEquals("else", dispatcher.dispatched[0].second["id"])
    }

    // ==================== Repeat Loop ====================

    @Test
    fun execute_repeatLoop() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "RepeatTest"
  @repeat 3
    VCM(id: "cmd", action: "tap")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(3, dispatcher.dispatched.size)
    }

    // ==================== While Loop ====================

    @Test
    fun execute_whileLoop() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "WhileTest"
  @set i = 0
  @while ${'$'}i < 3
    VCM(id: "cmd", action: "iterate")
    @set i = ${'$'}i + 1
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(3, dispatcher.dispatched.size)
    }

    // ==================== Function Calls ====================

    @Test
    fun execute_functionCallWithNamedArgs() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@define greet(name)
  VCM(id: "greet", action: ${'$'}name)

@workflow "FuncTest"
  greet(name: "Alice")
  greet(name: "Bob")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(2, dispatcher.dispatched.size)
        assertEquals("Alice", dispatcher.dispatched[0].second["action"])
        assertEquals("Bob", dispatcher.dispatched[1].second["action"])
    }

    @Test
    fun execute_functionWithReturn() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@define check()
  @return true

@workflow "ReturnTest"
  @log "before"
  check()
  @log "after"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))
        assertIs<ExecutionResult.Success>(result)
    }

    // ==================== Named Workflow Execution ====================

    @Test
    fun executeWorkflow_findsNamedWorkflow() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "First"
  VCM(id: "first", action: "tap")

@workflow "Second"
  VCM(id: "second", action: "tap")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.executeWorkflow(parse(source), "Second")

        assertIs<ExecutionResult.Success>(result)
        assertEquals(1, dispatcher.dispatched.size)
        assertEquals("second", dispatcher.dispatched[0].second["id"])
    }

    @Test
    fun executeWorkflow_notFoundReturnsFailure() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "Existing"
  @log "here"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.executeWorkflow(parse(source), "Missing")
        assertIs<ExecutionResult.Failure>(result)
    }

    // ==================== Trigger Handler ====================

    @Test
    fun handleTrigger_matchesPatternAndInjectsCaptures() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
triggers:
  login to {app}
---
@on "login to {app}"
  VCM(id: "open", action: "launch", target: ${'$'}app)
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.handleTrigger(
            parse(source),
            "login to {app}",
            mapOf("app" to "slack")
        )

        assertIs<ExecutionResult.Success>(result)
        assertEquals(1, dispatcher.dispatched.size)
        assertEquals("slack", dispatcher.dispatched[0].second["target"])
    }

    @Test
    fun handleTrigger_noMatchReturnsNoHandler() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
triggers:
  hello
---
@on "hello"
  @log "hi"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.handleTrigger(parse(source), "goodbye", emptyMap())
        assertIs<ExecutionResult.NoHandler>(result)
    }

    // ==================== Dispatch Error Handling ====================

    @Test
    fun execute_dispatchError_returnsFailure() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "ErrorTest"
  VCM(id: "cmd", action: "fail")
""".trimIndent()

        val dispatcher = MockDispatcher()
        dispatcher.onCode("VCM", DispatchResult.Error("Device not connected"))
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Failure>(result)
        assertIs<RuntimeError.DispatchError>(result.error)
    }

    // ==================== Sandbox Enforcement ====================

    @Test
    fun execute_stepLimitExceeded_returnsFailure() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "StepLimit"
  @repeat 50
    @log "step"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val sandbox = SandboxConfig(maxSteps = 10, maxLoopIterations = 100, maxExecutionTimeMs = 60_000)
        val interpreter = AvuInterpreter(dispatcher, sandbox)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Failure>(result)
        assertIs<RuntimeError.SandboxViolation>(result.error)
    }

    @Test
    fun execute_loopLimitExceeded_returnsFailure() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "LoopLimit"
  @repeat 200
    @log "loop"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val sandbox = SandboxConfig(maxLoopIterations = 50, maxSteps = 10_000, maxExecutionTimeMs = 60_000)
        val interpreter = AvuInterpreter(dispatcher, sandbox)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Failure>(result)
        assertIs<RuntimeError.SandboxViolation>(result.error)
    }

    // ==================== Emit Events ====================

    @Test
    fun execute_emitEvent() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "EmitTest"
  @emit "workflow_started"
  @emit "data_ready" "payload"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))
        assertIs<ExecutionResult.Success>(result)
    }

    // ==================== Sequence ====================

    @Test
    fun execute_sequence() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "SeqTest"
  @sequence
    VCM(id: "a", action: "first")
    VCM(id: "b", action: "second")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals(2, dispatcher.dispatched.size)
        assertEquals("a", dispatcher.dispatched[0].second["id"])
        assertEquals("b", dispatcher.dispatched[1].second["id"])
    }

    // ==================== String Concatenation in Expressions ====================

    @Test
    fun execute_stringConcatInCodeArgs() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
codes:
  VCM: Voice Command (id:action:params)
---
@workflow "ConcatTest"
  @set prefix = "cmd"
  @set suffix = "01"
  VCM(id: ${'$'}prefix + "_" + ${'$'}suffix, action: "test")
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertEquals("cmd_01", dispatcher.dispatched[0].second["id"])
    }

    // ==================== Execution Time Tracking ====================

    @Test
    fun execute_reportsExecutionTime() = runTest {
        val source = """
---
schema: avu-2.2
version: 1.0.0
type: workflow
---
@workflow "TimeTest"
  @log "quick"
""".trimIndent()

        val dispatcher = MockDispatcher()
        val interpreter = AvuInterpreter(dispatcher, SandboxConfig.TESTING)
        val result = interpreter.execute(parse(source))

        assertIs<ExecutionResult.Success>(result)
        assertTrue(result.executionTimeMs >= 0)
    }
}

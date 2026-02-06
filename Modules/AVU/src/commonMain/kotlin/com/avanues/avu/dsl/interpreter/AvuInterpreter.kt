package com.avanues.avu.dsl.interpreter
import com.avanues.avu.dsl.currentTimeMillis

import com.avanues.avu.dsl.ast.*
import kotlinx.coroutines.delay

/**
 * Result of executing an AVU DSL file.
 */
sealed class ExecutionResult {
    /** Execution completed successfully. */
    data class Success(
        val returnValue: Any? = null,
        val executionTimeMs: Long = 0
    ) : ExecutionResult()

    /** Execution failed with a runtime error. */
    data class Failure(
        val error: RuntimeError,
        val executionTimeMs: Long = 0
    ) : ExecutionResult()

    /** No matching handler found for the trigger pattern. */
    data object NoHandler : ExecutionResult()
}

/**
 * Internal control flow exception for @return statements.
 * Caught at function/workflow boundaries.
 */
private class ReturnException(val value: Any?) : Exception()

/**
 * AVU DSL tree-walking interpreter.
 *
 * Takes an [AvuDslFile] AST (from AvuDslParser) and executes it inside a configurable
 * [SandboxConfig], dispatching wire protocol code invocations through [IAvuDispatcher].
 *
 * Responsibilities are split between this class and [ExpressionEvaluator]:
 * - **AvuInterpreter**: statement execution, control flow, public API
 * - **ExpressionEvaluator**: expression evaluation, type coercion, built-in dispatch
 *
 * ## Architecture
 * - **Stateful**: holds dispatcher reference and sandbox config
 * - **Fresh context per run**: each [execute] call creates a new [ExecutionContext]
 * - **Two-phase execution**: first registers all @define functions, then executes @workflow bodies
 *
 * ## Usage
 * ```kotlin
 * val dispatcher = MyPlatformDispatcher()
 * val interpreter = AvuInterpreter(dispatcher, SandboxConfig.DEFAULT)
 *
 * val tokens = AvuDslLexer(source).tokenize()
 * val parseResult = AvuDslParser(tokens).parse()
 * val result = interpreter.execute(parseResult.file)
 * ```
 */
class AvuInterpreter(
    private val dispatcher: IAvuDispatcher,
    private val sandbox: SandboxConfig = SandboxConfig.DEFAULT
) {
    private val evaluator = ExpressionEvaluator(dispatcher)

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Execute all declarations in the file.
     * Registers @define functions first, then executes @workflow declarations in order.
     * @on trigger handlers are NOT executed (use [handleTrigger] for those).
     */
    suspend fun execute(file: AvuDslFile): ExecutionResult {
        val context = ExecutionContext(sandbox)
        val startTime = currentTimeMillis()
        return try {
            registerFunctions(file, context)
            var lastResult: Any? = null
            for (decl in file.declarations) {
                if (decl is AvuAstNode.Declaration.Workflow) {
                    lastResult = executeWorkflowBody(decl, context)
                }
            }
            ExecutionResult.Success(lastResult, currentTimeMillis() - startTime)
        } catch (e: RuntimeError) {
            ExecutionResult.Failure(e, currentTimeMillis() - startTime)
        } catch (e: ReturnException) {
            ExecutionResult.Success(e.value, currentTimeMillis() - startTime)
        }
    }

    /**
     * Execute a specific named workflow from the file.
     *
     * @param name The workflow name to find and execute
     * @return [ExecutionResult.Failure] if the named workflow is not found
     */
    suspend fun executeWorkflow(file: AvuDslFile, name: String): ExecutionResult {
        val context = ExecutionContext(sandbox)
        val startTime = currentTimeMillis()
        return try {
            registerFunctions(file, context)
            val workflow = file.declarations
                .filterIsInstance<AvuAstNode.Declaration.Workflow>()
                .find { it.name == name }
                ?: return ExecutionResult.Failure(
                    RuntimeError.General("Workflow '$name' not found", file.location),
                    currentTimeMillis() - startTime
                )
            val result = executeWorkflowBody(workflow, context)
            ExecutionResult.Success(result, currentTimeMillis() - startTime)
        } catch (e: RuntimeError) {
            ExecutionResult.Failure(e, currentTimeMillis() - startTime)
        } catch (e: ReturnException) {
            ExecutionResult.Success(e.value, currentTimeMillis() - startTime)
        }
    }

    /**
     * Handle a trigger by matching it against @on declarations.
     *
     * @param pattern The trigger pattern to match (e.g., "login to {app}")
     * @param captures Captured variables from pattern matching (e.g., mapOf("app" to "slack"))
     * @return [ExecutionResult.NoHandler] if no @on handler matches the pattern
     */
    suspend fun handleTrigger(
        file: AvuDslFile,
        pattern: String,
        captures: Map<String, String>
    ): ExecutionResult {
        val context = ExecutionContext(sandbox)
        val startTime = currentTimeMillis()
        return try {
            registerFunctions(file, context)
            val handler = file.declarations
                .filterIsInstance<AvuAstNode.Declaration.TriggerHandler>()
                .find { it.pattern == pattern }
                ?: return ExecutionResult.NoHandler
            // Inject captured variables into scope
            for ((name, value) in captures) {
                context.setVariable(name, value, handler.location)
            }
            val result = executeStatements(handler.body, context)
            ExecutionResult.Success(result, currentTimeMillis() - startTime)
        } catch (e: RuntimeError) {
            ExecutionResult.Failure(e, currentTimeMillis() - startTime)
        } catch (e: ReturnException) {
            ExecutionResult.Success(e.value, currentTimeMillis() - startTime)
        }
    }

    // =========================================================================
    // DECLARATION HELPERS
    // =========================================================================

    private fun registerFunctions(file: AvuDslFile, context: ExecutionContext) {
        file.declarations
            .filterIsInstance<AvuAstNode.Declaration.FunctionDef>()
            .forEach { context.registerFunction(it) }
    }

    private suspend fun executeWorkflowBody(
        workflow: AvuAstNode.Declaration.Workflow,
        context: ExecutionContext
    ): Any? {
        context.enterNesting(workflow.location)
        return try {
            executeStatements(workflow.body, context)
        } catch (e: ReturnException) {
            e.value
        } finally {
            context.exitNesting()
        }
    }

    // =========================================================================
    // STATEMENT EXECUTION
    // =========================================================================

    private suspend fun executeStatements(
        statements: List<AvuAstNode.Statement>,
        context: ExecutionContext
    ): Any? {
        var lastResult: Any? = null
        for (stmt in statements) {
            lastResult = executeStatement(stmt, context)
        }
        return lastResult
    }

    private suspend fun executeStatement(
        stmt: AvuAstNode.Statement,
        context: ExecutionContext
    ): Any? {
        context.incrementStep(stmt.location)
        return when (stmt) {
            is AvuAstNode.Statement.CodeInvocation -> executeCodeInvocation(stmt, context)
            is AvuAstNode.Statement.FunctionCall -> executeFunctionCall(stmt, context)
            is AvuAstNode.Statement.WaitDelay -> { executeWaitDelay(stmt, context); null }
            is AvuAstNode.Statement.WaitCondition -> { executeWaitCondition(stmt, context); null }
            is AvuAstNode.Statement.IfElse -> executeIfElse(stmt, context)
            is AvuAstNode.Statement.Repeat -> executeRepeat(stmt, context)
            is AvuAstNode.Statement.While -> executeWhile(stmt, context)
            is AvuAstNode.Statement.Sequence -> executeStatements(stmt.body, context)
            is AvuAstNode.Statement.Assignment -> { executeAssignment(stmt, context); null }
            is AvuAstNode.Statement.Log -> { executeLog(stmt, context); null }
            is AvuAstNode.Statement.Return -> executeReturn(stmt, context)
            is AvuAstNode.Statement.Emit -> { executeEmit(stmt, context); null }
        }
    }

    // --- Code Invocation ---

    private suspend fun executeCodeInvocation(
        stmt: AvuAstNode.Statement.CodeInvocation,
        context: ExecutionContext
    ): Any? {
        val args = mutableMapOf<String, Any?>()
        for ((index, namedArg) in stmt.arguments.withIndex()) {
            val value = evaluator.evaluate(namedArg.value, context)
            args[namedArg.name ?: "arg$index"] = value
        }
        return when (val result = dispatcher.dispatch(stmt.code, args)) {
            is DispatchResult.Success -> result.data
            is DispatchResult.Error -> throw RuntimeError.DispatchError(
                stmt.code, result.message, stmt.location
            )
            is DispatchResult.Timeout -> throw RuntimeError.TimeoutError(
                result.timeoutMs, "Code '${stmt.code}' timed out", stmt.location
            )
        }
    }

    // --- Function Call ---

    private suspend fun executeFunctionCall(
        stmt: AvuAstNode.Statement.FunctionCall,
        context: ExecutionContext
    ): Any? {
        val funcDef = context.getFunction(stmt.name)
            ?: throw RuntimeError.UndefinedFunction(stmt.name, stmt.location)

        // Evaluate all argument values in the current scope before pushing new scope
        val evaluatedArgs = stmt.arguments.map { it.name to evaluator.evaluate(it.value, context) }

        context.pushScope()
        context.enterNesting(stmt.location)
        try {
            bindParameters(funcDef.parameters, evaluatedArgs, context, stmt.location)
            return executeStatements(funcDef.body, context)
        } catch (e: ReturnException) {
            return e.value
        } finally {
            context.exitNesting()
            context.popScope()
        }
    }

    /**
     * Bind function arguments to parameter names.
     * Named arguments are bound first, then positional arguments fill remaining slots.
     * Unbound parameters default to null.
     */
    private fun bindParameters(
        parameters: List<String>,
        arguments: List<Pair<String?, Any?>>,
        context: ExecutionContext,
        location: SourceLocation
    ) {
        val boundParams = mutableSetOf<String>()

        // First pass: bind named arguments
        for ((argName, argValue) in arguments) {
            if (argName != null && argName in parameters) {
                context.setVariable(argName, argValue, location)
                boundParams.add(argName)
            }
        }

        // Second pass: bind positional arguments to remaining parameter slots
        var positionalIdx = 0
        for ((argName, argValue) in arguments) {
            if (argName == null) {
                while (positionalIdx < parameters.size && parameters[positionalIdx] in boundParams) {
                    positionalIdx++
                }
                if (positionalIdx < parameters.size) {
                    context.setVariable(parameters[positionalIdx], argValue, location)
                    boundParams.add(parameters[positionalIdx])
                    positionalIdx++
                }
            }
        }

        // Unbound parameters default to null
        for (param in parameters) {
            if (param !in boundParams) {
                context.setVariable(param, null, location)
            }
        }
    }

    // --- Wait ---

    private suspend fun executeWaitDelay(
        stmt: AvuAstNode.Statement.WaitDelay,
        context: ExecutionContext
    ) {
        val ms = evaluator.evaluate(stmt.milliseconds, context)
        val delayMs = evaluator.toNumber(ms, stmt.location).toLong()
        delay(delayMs)
    }

    private suspend fun executeWaitCondition(
        stmt: AvuAstNode.Statement.WaitCondition,
        context: ExecutionContext
    ) {
        val timeoutMs = evaluator.toNumber(
            evaluator.evaluate(stmt.timeoutMs, context), stmt.location
        ).toLong()
        val startTime = currentTimeMillis()
        val pollInterval = 100L

        while (true) {
            val condValue = evaluator.evaluate(stmt.condition, context)
            if (evaluator.toBooleanValue(condValue)) return

            val elapsed = currentTimeMillis() - startTime
            if (elapsed >= timeoutMs) {
                throw RuntimeError.TimeoutError(timeoutMs, "Wait condition not met", stmt.location)
            }
            delay(pollInterval)
            context.incrementStep(stmt.location)
        }
    }

    // --- Control Flow ---

    private suspend fun executeIfElse(
        stmt: AvuAstNode.Statement.IfElse,
        context: ExecutionContext
    ): Any? {
        val condValue = evaluator.evaluate(stmt.condition, context)
        return if (evaluator.toBooleanValue(condValue)) {
            executeStatements(stmt.thenBody, context)
        } else {
            executeStatements(stmt.elseBody, context)
        }
    }

    private suspend fun executeRepeat(
        stmt: AvuAstNode.Statement.Repeat,
        context: ExecutionContext
    ): Any? {
        val count = evaluator.toNumber(
            evaluator.evaluate(stmt.count, context), stmt.location
        ).toInt()
        context.checkLoopLimit(count, stmt.location)
        var lastResult: Any? = null
        for (i in 0 until count) {
            lastResult = executeStatements(stmt.body, context)
        }
        return lastResult
    }

    private suspend fun executeWhile(
        stmt: AvuAstNode.Statement.While,
        context: ExecutionContext
    ): Any? {
        var iterations = 0
        var lastResult: Any? = null
        while (true) {
            val condValue = evaluator.evaluate(stmt.condition, context)
            if (!evaluator.toBooleanValue(condValue)) break
            iterations++
            context.checkLoopLimit(iterations, stmt.location)
            lastResult = executeStatements(stmt.body, context)
        }
        return lastResult
    }

    // --- Assignment, Log, Return, Emit ---

    private suspend fun executeAssignment(
        stmt: AvuAstNode.Statement.Assignment,
        context: ExecutionContext
    ) {
        val value = evaluator.evaluate(stmt.value, context)
        context.setVariable(stmt.variableName, value, stmt.location)
    }

    private suspend fun executeLog(
        stmt: AvuAstNode.Statement.Log,
        context: ExecutionContext
    ) {
        val message = evaluator.evaluate(stmt.message, context)
        println("[AVU DSL] $message")
    }

    private suspend fun executeReturn(
        stmt: AvuAstNode.Statement.Return,
        context: ExecutionContext
    ): Nothing {
        val value = stmt.value?.let { evaluator.evaluate(it, context) }
        throw ReturnException(value)
    }

    private suspend fun executeEmit(
        stmt: AvuAstNode.Statement.Emit,
        context: ExecutionContext
    ) {
        val data = stmt.data?.let { evaluator.evaluate(it, context) }
        context.emitEvent(stmt.eventName, data)
    }
}

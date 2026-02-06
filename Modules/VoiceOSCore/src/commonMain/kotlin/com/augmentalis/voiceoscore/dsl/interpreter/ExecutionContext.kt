package com.augmentalis.voiceoscore.dsl.interpreter

import com.augmentalis.voiceoscore.dsl.ast.AvuAstNode
import com.augmentalis.voiceoscore.dsl.ast.SourceLocation
import com.augmentalis.voiceoscore.currentTimeMillis

/**
 * Execution context for a single AVU DSL run.
 *
 * Manages variable scopes, function definitions, event listeners, and sandbox enforcement.
 * A fresh context is created for each [AvuInterpreter.execute] call, ensuring isolation
 * between executions.
 *
 * ## Variable Scoping
 * Uses a scope stack with innermost-to-outermost lookup. New scopes are pushed for
 * function calls. Variable assignment updates existing variables in their original scope
 * (lexical update), or creates new variables in the current scope.
 *
 * ## Sandbox Enforcement
 * Every interpreter step calls [incrementStep], which checks both step count and wall-clock
 * time. Nesting depth is tracked via [enterNesting]/[exitNesting]. Loop iterations are
 * checked via [checkLoopLimit].
 */
class ExecutionContext(
    private val sandbox: SandboxConfig
) {
    // =========================================================================
    // VARIABLE SCOPE STACK
    // =========================================================================

    private val scopeStack = mutableListOf<MutableMap<String, Any?>>(mutableMapOf())

    /** Push a new variable scope (for function calls). */
    fun pushScope() {
        scopeStack.add(mutableMapOf())
    }

    /** Pop the current variable scope. Global scope cannot be popped. */
    fun popScope() {
        if (scopeStack.size > 1) {
            scopeStack.removeAt(scopeStack.lastIndex)
        }
    }

    /** Get a variable value, searching from innermost to outermost scope. Returns null if not found. */
    fun getVariable(name: String): Any? {
        for (i in scopeStack.indices.reversed()) {
            if (scopeStack[i].containsKey(name)) {
                return scopeStack[i][name]
            }
        }
        return null
    }

    /** Check if a variable exists in any scope. */
    fun hasVariable(name: String): Boolean {
        for (i in scopeStack.indices.reversed()) {
            if (scopeStack[i].containsKey(name)) return true
        }
        return false
    }

    /**
     * Set a variable value. If the variable already exists in an outer scope, updates it there
     * (lexical update semantics). Otherwise creates in the current (innermost) scope.
     *
     * @throws RuntimeError.SandboxViolation if variable limit is exceeded
     */
    fun setVariable(name: String, value: Any?, location: SourceLocation?) {
        // Check variable limit before creating new variables
        if (!hasVariable(name)) {
            val totalVars = scopeStack.sumOf { it.size }
            if (totalVars >= sandbox.maxVariables) {
                throw RuntimeError.SandboxViolation.variableLimit(sandbox.maxVariables, location)
            }
        }
        // If variable exists in an outer scope, update it there
        for (i in scopeStack.indices.reversed()) {
            if (scopeStack[i].containsKey(name)) {
                scopeStack[i][name] = value
                return
            }
        }
        // Otherwise create in current (innermost) scope
        scopeStack.last()[name] = value
    }

    // =========================================================================
    // FUNCTION REGISTRY
    // =========================================================================

    private val functions = mutableMapOf<String, AvuAstNode.Declaration.FunctionDef>()

    /** Register a @define function definition. */
    fun registerFunction(funcDef: AvuAstNode.Declaration.FunctionDef) {
        functions[funcDef.name] = funcDef
    }

    /** Lookup a @define function by name. Returns null if not found. */
    fun getFunction(name: String): AvuAstNode.Declaration.FunctionDef? = functions[name]

    // =========================================================================
    // EVENT LISTENERS
    // =========================================================================

    private val eventListeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    /** Register a listener for a named event. */
    fun addEventListener(event: String, listener: (Any?) -> Unit) {
        eventListeners.getOrPut(event) { mutableListOf() }.add(listener)
    }

    /** Emit an event to all registered listeners. */
    fun emitEvent(event: String, data: Any?) {
        eventListeners[event]?.forEach { it(data) }
    }

    // =========================================================================
    // SANDBOX ENFORCEMENT
    // =========================================================================

    private var stepCount = 0
    private val startTimeMs = currentTimeMillis()
    private var nestingDepth = 0

    /**
     * Increment step counter and check sandbox limits.
     *
     * @throws RuntimeError.SandboxViolation if step limit or time limit is exceeded
     */
    fun incrementStep(location: SourceLocation?) {
        stepCount++
        if (stepCount > sandbox.maxSteps) {
            throw RuntimeError.SandboxViolation.stepLimit(sandbox.maxSteps, location)
        }
        val elapsed = currentTimeMillis() - startTimeMs
        if (elapsed > sandbox.maxExecutionTimeMs) {
            throw RuntimeError.SandboxViolation.timeLimit(sandbox.maxExecutionTimeMs, elapsed, location)
        }
    }

    /**
     * Enter a nesting level (function call, workflow body).
     *
     * @throws RuntimeError.SandboxViolation if nesting depth limit is exceeded
     */
    fun enterNesting(location: SourceLocation?) {
        nestingDepth++
        if (nestingDepth > sandbox.maxNestingDepth) {
            throw RuntimeError.SandboxViolation.nestingLimit(sandbox.maxNestingDepth, location)
        }
    }

    /** Exit a nesting level. */
    fun exitNesting() {
        if (nestingDepth > 0) nestingDepth--
    }

    /**
     * Check loop iteration count against sandbox limit.
     *
     * @throws RuntimeError.SandboxViolation if loop limit is exceeded
     */
    fun checkLoopLimit(iterations: Int, location: SourceLocation?) {
        if (iterations > sandbox.maxLoopIterations) {
            throw RuntimeError.SandboxViolation.loopLimit(sandbox.maxLoopIterations, iterations, location)
        }
    }

    /** Get elapsed execution time in milliseconds. */
    fun elapsedTimeMs(): Long = currentTimeMillis() - startTimeMs
}

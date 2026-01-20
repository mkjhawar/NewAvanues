package com.augmentalis.avamagic.events

/**
 * Execution context for DSL callback evaluation.
 *
 * EventContext maintains the variable scope during lambda execution, providing
 * storage for:
 * - Lambda parameters (e.g., color, x, y)
 * - Local variables created by assignments
 * - Parent scope variables (via createChild)
 *
 * The context supports lexical scoping through parent-child relationships,
 * allowing nested lambdas to access outer scope variables while maintaining
 * their own local state.
 *
 * Example usage:
 * ```kotlin
 * // Create root context
 * val context = EventContext()
 *
 * // Set global variables
 * context.set("theme", "dark")
 * context.set("primaryColor", "#FF5733")
 *
 * // Create child context for lambda with parameters
 * val lambdaContext = context.createChild(mapOf(
 *     "color" to "#00FF00",  // Lambda parameter
 *     "x" to 120             // Lambda parameter
 * ))
 *
 * // Child can access both local and parent variables
 * val color = lambdaContext.get("color")     // "#00FF00" (local)
 * val theme = lambdaContext.get("theme")     // "dark" (parent)
 *
 * // Assignments modify local scope only
 * lambdaContext.set("selectedColor", color)
 * ```
 *
 * Thread safety:
 * - NOT thread-safe: Each context instance should be used by a single thread
 * - For concurrent execution, create separate context instances
 *
 * Architecture notes:
 * - Immutable parent reference enables functional scope chaining
 * - Mutable local variables support imperative DSL statements
 * - Copy-on-child pattern prevents parent contamination
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-10-27 12:18:38 PDT
 *
 * @property variables Local variable storage (name to value mapping)
 * @property parent Optional parent context for scope chaining
 *
 * @see CallbackAdapter for context usage in lambda execution
 */
class EventContext(
    private val variables: MutableMap<String, Any?> = mutableMapOf(),
    private val parent: EventContext? = null
) {

    /**
     * Get variable value by name.
     *
     * Searches in this scope first, then recursively searches parent scopes
     * until the variable is found or the root scope is reached.
     *
     * Example:
     * ```kotlin
     * val color = context.get("color")
     * if (color != null) {
     *     println("Color: $color")
     * }
     * ```
     *
     * @param name Variable name
     * @return Variable value, or null if not found in any scope
     */
    fun get(name: String): Any? {
        return variables[name] ?: parent?.get(name)
    }

    /**
     * Set variable value in the current scope.
     *
     * Always modifies the local scope, never the parent. If a variable
     * with the same name exists in a parent scope, this creates a new
     * local variable that shadows the parent's value.
     *
     * Example:
     * ```kotlin
     * context.set("color", "#FF5733")
     * context.set("count", 42)
     * context.set("enabled", true)
     * ```
     *
     * @param name Variable name
     * @param value Variable value (any type, including null)
     */
    fun set(name: String, value: Any?) {
        variables[name] = value
    }

    /**
     * Check if a variable exists in this context or any parent.
     *
     * Returns true if the variable is defined in the current scope
     * or in any ancestor scope.
     *
     * Example:
     * ```kotlin
     * if (context.has("color")) {
     *     val color = context.get("color")
     *     // Use color safely
     * }
     * ```
     *
     * @param name Variable name
     * @return true if variable exists in this or parent scope
     */
    fun has(name: String): Boolean {
        return variables.containsKey(name) || parent?.has(name) == true
    }

    /**
     * Remove a variable from the current scope.
     *
     * Only affects the local scope; parent scope variables are unchanged.
     * If a parent has a variable with the same name, it will become
     * visible again after removal.
     *
     * Example:
     * ```kotlin
     * context.set("temp", "value")
     * context.remove("temp")  // Variable no longer exists locally
     * ```
     *
     * @param name Variable name
     * @return true if variable was removed, false if it didn't exist
     */
    fun remove(name: String): Boolean {
        return variables.remove(name) != null
    }

    /**
     * Create a child context with additional parameters.
     *
     * Creates a new EventContext that:
     * - Inherits all variables from this context (as parent)
     * - Adds the provided parameters as local variables
     * - Can define new variables without affecting the parent
     *
     * This is the primary mechanism for implementing lexical scoping
     * in DSL lambda execution.
     *
     * Example:
     * ```kotlin
     * // Parent context has global state
     * val parent = EventContext()
     * parent.set("theme", "dark")
     *
     * // Child context has lambda parameters
     * val child = parent.createChild(mapOf(
     *     "color" to "#FF5733",
     *     "x" to 120,
     *     "y" to 450
     * ))
     *
     * // Child sees both parent and local variables
     * println(child.get("theme"))  // "dark" (from parent)
     * println(child.get("color"))  // "#FF5733" (local)
     *
     * // Parent is unaffected by child changes
     * child.set("newVar", "value")
     * println(parent.has("newVar"))  // false
     * ```
     *
     * @param parameters Additional variables to add to child scope
     * @return New EventContext with this context as parent
     */
    fun createChild(parameters: Map<String, Any?>): EventContext {
        return EventContext(
            variables = parameters.toMutableMap(),
            parent = this
        )
    }

    /**
     * Clear all variables in the current scope.
     *
     * Removes all variables from the local scope. Parent scope variables
     * remain unchanged and accessible.
     *
     * Example:
     * ```kotlin
     * context.set("a", 1)
     * context.set("b", 2)
     * context.clear()
     * println(context.has("a"))  // false
     * ```
     */
    fun clear() {
        variables.clear()
    }

    /**
     * Get all variables in the current scope (not including parent).
     *
     * Returns a read-only map of local variables. Useful for debugging
     * and inspection.
     *
     * Example:
     * ```kotlin
     * val locals = context.getAllLocal()
     * println("Local variables: ${locals.keys}")
     * ```
     *
     * @return Map of local variable names to values
     */
    fun getAllLocal(): Map<String, Any?> {
        return variables.toMap()
    }

    /**
     * Get all variables including those from parent scopes.
     *
     * Returns a flattened map of all variables visible in this context,
     * with local variables taking precedence over parent variables.
     *
     * Example:
     * ```kotlin
     * val allVars = context.getAllWithParents()
     * println("All visible variables: ${allVars.keys}")
     * ```
     *
     * @return Map of all visible variable names to values
     */
    fun getAllWithParents(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Add parent variables first (lower precedence)
        parent?.let { result.putAll(it.getAllWithParents()) }

        // Add local variables (override parent values)
        result.putAll(variables)

        return result
    }

    /**
     * Returns the depth of this context in the scope chain.
     *
     * Root context has depth 0, its children have depth 1, etc.
     * Useful for debugging nested lambda execution.
     *
     * Example:
     * ```kotlin
     * val root = EventContext()        // depth = 0
     * val child = root.createChild()   // depth = 1
     * val grandchild = child.createChild()  // depth = 2
     * ```
     *
     * @return Scope depth (0 for root)
     */
    val depth: Int
        get() = if (parent == null) 0 else parent.depth + 1

    /**
     * Returns the number of variables in the local scope.
     *
     * Does not include parent scope variables.
     *
     * @return Number of local variables
     */
    val size: Int
        get() = variables.size

    /**
     * Returns true if this context has no local variables.
     *
     * Parent scope variables are not considered.
     *
     * @return true if no local variables exist
     */
    val isEmpty: Boolean
        get() = variables.isEmpty()

    /**
     * Returns a human-readable string representation of this context.
     * Shows scope depth and local variable count.
     *
     * Example output:
     * ```
     * EventContext(depth=2, variables=3)
     * ```
     */
    override fun toString(): String {
        return "EventContext(depth=$depth, variables=${variables.size})"
    }

    companion object {
        /**
         * Creates an EventContext pre-populated with standard VoiceOS globals.
         *
         * Includes common system objects and utilities that should be available
         * to all DSL lambdas.
         *
         * Example:
         * ```kotlin
         * val context = EventContext.withStandardGlobals()
         * // Has access to VoiceOS, Preferences, etc.
         * ```
         *
         * @return EventContext with standard globals
         */
        fun withStandardGlobals(): EventContext {
            val context = EventContext()
            // TODO: Add standard globals when system objects are available
            // context.set("VoiceOS", VoiceOSApi)
            // context.set("Preferences", PreferencesApi)
            return context
        }

        /**
         * Creates an EventContext from a flat map of variables.
         *
         * Convenience factory for simple context creation.
         *
         * @param variables Initial variable map
         * @return EventContext with the provided variables
         */
        fun from(variables: Map<String, Any?>): EventContext {
            return EventContext(variables.toMutableMap())
        }
    }
}

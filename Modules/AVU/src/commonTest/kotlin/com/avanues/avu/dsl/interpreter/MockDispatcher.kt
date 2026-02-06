package com.avanues.avu.dsl.interpreter

/**
 * Mock dispatcher for testing. Records dispatched calls and returns configurable results.
 */
class MockDispatcher : IAvuDispatcher {
    val dispatched = mutableListOf<Pair<String, Map<String, Any?>>>()
    private val results = mutableMapOf<String, DispatchResult>()

    /** Set a result for a specific code. */
    fun onCode(code: String, result: DispatchResult) {
        results[code] = result
    }

    /** Set a result for a specific query (e.g., "screen_contains"). */
    fun onQuery(query: String, result: Any?) {
        results["QRY:$query"] = DispatchResult.Success(result)
    }

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        dispatched.add(code to arguments)
        // Check for query-specific results
        if (code == "QRY") {
            val query = arguments["query"] as? String
            if (query != null) {
                results["QRY:$query"]?.let { return it }
            }
        }
        return results[code] ?: DispatchResult.Success()
    }

    override fun canDispatch(code: String): Boolean = true
}

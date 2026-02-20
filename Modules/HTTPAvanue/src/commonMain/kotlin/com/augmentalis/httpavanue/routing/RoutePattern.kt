package com.augmentalis.httpavanue.routing

/**
 * Route pattern matching with support for path parameters (/:id) and static segments (/users)
 */
class RoutePattern(val pattern: String) {
    private val segments: List<Segment>
    private val paramNames: List<String>

    init {
        segments = parsePattern(pattern)
        paramNames = segments.filterIsInstance<Segment.Parameter>().map { it.name }
    }

    val isStatic: Boolean get() = segments.all { it is Segment.Static }
    val hasParameters: Boolean get() = paramNames.isNotEmpty()
    fun getParameterNames(): List<String> = paramNames

    fun match(path: String): Map<String, String>? {
        val pathSegments = path.split('/').filter { it.isNotEmpty() }
        if (pattern == "/" && path == "/") return emptyMap()
        if (segments.size != pathSegments.size) return null
        val params = mutableMapOf<String, String>()
        segments.forEachIndexed { index, segment ->
            val pathSegment = pathSegments[index]
            when (segment) {
                is Segment.Static -> if (segment.value != pathSegment) return null
                is Segment.Parameter -> params[segment.name] = pathSegment
            }
        }
        return params
    }

    fun generate(params: Map<String, String>): String? {
        if (pattern == "/") return "/"
        return buildString {
            segments.forEach { segment ->
                append('/')
                when (segment) {
                    is Segment.Static -> append(segment.value)
                    is Segment.Parameter -> append(params[segment.name] ?: return null)
                }
            }
        }
    }

    private fun parsePattern(pattern: String): List<Segment> {
        if (pattern == "/") return emptyList()
        return pattern.split('/').filter { it.isNotEmpty() }.map { segment ->
            if (segment.startsWith(':')) Segment.Parameter(segment.substring(1))
            else Segment.Static(segment)
        }
    }

    sealed class Segment {
        data class Static(val value: String) : Segment()
        data class Parameter(val name: String) : Segment()
    }

    override fun toString(): String = pattern
    override fun equals(other: Any?) = other is RoutePattern && pattern == other.pattern
    override fun hashCode() = pattern.hashCode()
}

fun pattern(path: String) = RoutePattern(path)

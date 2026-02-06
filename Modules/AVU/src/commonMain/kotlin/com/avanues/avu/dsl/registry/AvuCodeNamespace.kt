package com.avanues.avu.dsl.registry

/**
 * Namespace support for AVU wire protocol codes.
 *
 * System codes use bare 3-letter names: `VCM`, `AAC`, `QRY`.
 * Plugin codes use reverse-domain prefixes: `com.example.plugin:VCM`.
 *
 * This enables multiple plugins to declare the same base code without collisions,
 * and allows the registry to track which plugin owns which code invocations.
 */
object AvuCodeNamespace {

    const val SYSTEM_NAMESPACE = "system"
    private const val NAMESPACE_SEPARATOR = ":"

    /**
     * Parse a potentially namespaced code into namespace and code parts.
     *
     * - `"VCM"` → NamespacedCode(system, VCM)
     * - `"com.example.plugin:VCM"` → NamespacedCode(com.example.plugin, VCM)
     */
    fun parse(namespacedCode: String): NamespacedCode {
        val separatorIndex = namespacedCode.indexOf(NAMESPACE_SEPARATOR)
        if (separatorIndex > 0) {
            val prefix = namespacedCode.substring(0, separatorIndex)
            // Only treat as namespace if prefix looks like a reverse-domain ID
            if (prefix.contains(".")) {
                val code = namespacedCode.substring(separatorIndex + 1)
                return NamespacedCode(namespace = prefix, code = code)
            }
        }
        return NamespacedCode(namespace = SYSTEM_NAMESPACE, code = namespacedCode)
    }

    /**
     * Create a fully qualified namespaced code string.
     * System codes are returned bare (no prefix).
     */
    fun qualify(namespace: String, code: String): String =
        if (namespace == SYSTEM_NAMESPACE) code
        else "$namespace$NAMESPACE_SEPARATOR$code"

    /**
     * Check if a code string is namespaced (has a reverse-domain prefix).
     */
    fun isNamespaced(code: String): Boolean {
        val separatorIndex = code.indexOf(NAMESPACE_SEPARATOR)
        return separatorIndex > 0 && code.substring(0, separatorIndex).contains(".")
    }

    /**
     * Extract just the 3-letter code from a potentially namespaced string.
     */
    fun extractCode(namespacedCode: String): String = parse(namespacedCode).code
}

/**
 * A code with its namespace context.
 */
data class NamespacedCode(
    val namespace: String,
    val code: String
) {
    val isSystem: Boolean get() = namespace == AvuCodeNamespace.SYSTEM_NAMESPACE

    val qualified: String get() = AvuCodeNamespace.qualify(namespace, code)

    override fun toString(): String = qualified
}

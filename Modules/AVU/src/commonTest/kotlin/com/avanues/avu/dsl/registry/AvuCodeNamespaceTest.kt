package com.avanues.avu.dsl.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvuCodeNamespaceTest {

    @Test
    fun parse_system_code() {
        val result = AvuCodeNamespace.parse("VCM")
        assertEquals("system", result.namespace)
        assertEquals("VCM", result.code)
        assertTrue(result.isSystem)
    }

    @Test
    fun parse_namespaced_code() {
        val result = AvuCodeNamespace.parse("com.example.plugin:VCM")
        assertEquals("com.example.plugin", result.namespace)
        assertEquals("VCM", result.code)
        assertFalse(result.isSystem)
    }

    @Test
    fun qualify_system_code_omits_namespace() {
        val qualified = AvuCodeNamespace.qualify("system", "VCM")
        assertEquals("VCM", qualified)
    }

    @Test
    fun qualify_plugin_code_includes_namespace() {
        val qualified = AvuCodeNamespace.qualify("com.example.plugin", "VCM")
        assertEquals("com.example.plugin:VCM", qualified)
    }

    @Test
    fun isNamespaced_detects_namespaced_codes() {
        assertTrue(AvuCodeNamespace.isNamespaced("com.example.plugin:VCM"))
        assertFalse(AvuCodeNamespace.isNamespaced("VCM"))
    }

    @Test
    fun isNamespaced_rejects_plain_colon_codes() {
        // "VCM:data" is v1 wire format, not a namespace
        assertFalse(AvuCodeNamespace.isNamespaced("VCM:data"))
    }

    @Test
    fun extractCode_from_namespaced() {
        assertEquals("VCM", AvuCodeNamespace.extractCode("com.example.plugin:VCM"))
    }

    @Test
    fun extractCode_from_system() {
        assertEquals("VCM", AvuCodeNamespace.extractCode("VCM"))
    }

    @Test
    fun namespacedCode_toString_roundtrips() {
        val nsCode = NamespacedCode("com.test", "AAC")
        assertEquals("com.test:AAC", nsCode.toString())
        assertEquals("com.test:AAC", nsCode.qualified)
    }

    @Test
    fun system_namespacedCode_toString_bare() {
        val nsCode = NamespacedCode("system", "AAC")
        assertEquals("AAC", nsCode.toString())
    }
}

package com.avanues.avu.dsl.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvuV1CompatTest {

    @Test
    fun parse_simple_v1_message() {
        val result = AvuV1Compat.parseV1Message("VCM:cmd1:SCROLL_TOP")
        assertTrue(result.isSuccess)
        val msg = result.messageOrNull()!!
        assertEquals("VCM", msg.code)
        assertEquals(listOf("cmd1", "SCROLL_TOP"), msg.fields)
    }

    @Test
    fun parse_v1_message_with_no_fields() {
        val result = AvuV1Compat.parseV1Message("SYS")
        assertTrue(result.isSuccess)
        assertEquals("SYS", result.messageOrNull()!!.code)
        assertTrue(result.messageOrNull()!!.fields.isEmpty())
    }

    @Test
    fun parse_v1_message_with_many_fields() {
        val result = AvuV1Compat.parseV1Message("SCR:msg1:User:u123:v5:data")
        assertTrue(result.isSuccess)
        val msg = result.messageOrNull()!!
        assertEquals("SCR", msg.code)
        assertEquals(5, msg.fields.size)
    }

    @Test
    fun reject_empty_message() {
        val result = AvuV1Compat.parseV1Message("")
        assertFalse(result.isSuccess)
    }

    @Test
    fun reject_blank_message() {
        val result = AvuV1Compat.parseV1Message("   ")
        assertFalse(result.isSuccess)
    }

    @Test
    fun reject_lowercase_code() {
        val result = AvuV1Compat.parseV1Message("vcm:test")
        assertFalse(result.isSuccess)
    }

    @Test
    fun reject_short_code() {
        val result = AvuV1Compat.parseV1Message("VC:test")
        assertFalse(result.isSuccess)
    }

    @Test
    fun toDispatchArguments_maps_VCM_fields() {
        val msg = V1Message("VCM", listOf("cmd1", "SCROLL_TOP", "speed=fast"))
        val args = AvuV1Compat.toDispatchArguments(msg)
        assertEquals("cmd1", args["id"])
        assertEquals("SCROLL_TOP", args["action"])
        assertEquals("speed=fast", args["params"])
    }

    @Test
    fun toDispatchArguments_maps_AAC_fields() {
        val msg = V1Message("AAC", listOf("act1", "CLICK", "btn_submit"))
        val args = AvuV1Compat.toDispatchArguments(msg)
        assertEquals("act1", args["id"])
        assertEquals("CLICK", args["actionType"])
        assertEquals("btn_submit", args["target"])
    }

    @Test
    fun toDispatchArguments_maps_CHT_fields() {
        val msg = V1Message("CHT", listOf("msg1", "Hello World"))
        val args = AvuV1Compat.toDispatchArguments(msg)
        assertEquals("msg1", args["messageId"])
        assertEquals("Hello World", args["text"])
    }

    @Test
    fun toDispatchArguments_includes_positional_fallbacks() {
        val msg = V1Message("XYZ", listOf("a", "b", "c"))
        val args = AvuV1Compat.toDispatchArguments(msg)
        assertEquals("a", args["field_0"])
        assertEquals("b", args["field_1"])
        assertEquals("c", args["field_2"])
    }

    @Test
    fun toAvuDslText_generates_named_args_for_known_codes() {
        val msg = V1Message("VCM", listOf("cmd1", "SCROLL_TOP"))
        val text = AvuV1Compat.toAvuDslText(msg)
        assertTrue(text.startsWith("VCM("))
        assertTrue(text.contains("id:"))
        assertTrue(text.contains("action:"))
    }

    @Test
    fun toAvuDslText_generates_positional_args_for_unknown_codes() {
        val msg = V1Message("XYZ", listOf("a", "b"))
        val text = AvuV1Compat.toAvuDslText(msg)
        assertTrue(text.startsWith("XYZ("))
        assertTrue(text.contains("\"a\""))
        assertTrue(text.contains("\"b\""))
    }

    @Test
    fun isV1Format_detects_wire_protocol_messages() {
        assertTrue(AvuV1Compat.isV1Format("VCM:test"))
        assertTrue(AvuV1Compat.isV1Format("AAC:id:CLICK:target"))
        assertTrue(AvuV1Compat.isV1Format("SYS"))
    }

    @Test
    fun isV1Format_rejects_non_v1_strings() {
        assertFalse(AvuV1Compat.isV1Format("@workflow"))
        assertFalse(AvuV1Compat.isV1Format("ab"))
        assertFalse(AvuV1Compat.isV1Format(""))
        assertFalse(AvuV1Compat.isV1Format("vcm:test"))
    }
}

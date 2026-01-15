package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.CommandRegistry
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandRegistryTest {

    private fun createCommand(
        phrase: String,
        targetAvid: String,
        actionType: CommandActionType = CommandActionType.CLICK,
        confidence: Float = 0.8f
    ) = QuantizedCommand(
        avid = "",
        phrase = phrase,
        actionType = actionType,
        targetAvid = targetAvid,
        confidence = confidence
    )

    @Test
    fun `update replaces all commands`() {
        val registry = CommandRegistry()

        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1"),
            createCommand("click Cancel", "vuid2")
        ))

        assertEquals(2, registry.size)

        registry.updateSync(listOf(
            createCommand("click OK", "vuid3")
        ))

        assertEquals(1, registry.size)
        assertNull(registry.findByVuid("vuid1"))
        assertNotNull(registry.findByVuid("vuid3"))
    }

    @Test
    fun `findByPhrase returns exact match`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1"),
            createCommand("click Cancel", "vuid2")
        ))

        val found = registry.findByPhrase("click Submit")

        assertNotNull(found)
        assertEquals("vuid1", found.targetAvid)
    }

    @Test
    fun `findByPhrase is case insensitive`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1")
        ))

        val found = registry.findByPhrase("CLICK SUBMIT")

        assertNotNull(found)
        assertEquals("vuid1", found.targetAvid)
    }

    @Test
    fun `findByPhrase matches partial phrase (label only)`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1")
        ))

        val found = registry.findByPhrase("submit")

        assertNotNull(found)
        assertEquals("vuid1", found.targetAvid)
    }

    @Test
    fun `findByPhrase returns null when not found`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1")
        ))

        val found = registry.findByPhrase("click Delete")

        assertNull(found)
    }

    @Test
    fun `findByVuid returns command`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1")
        ))

        val found = registry.findByVuid("vuid1")

        assertNotNull(found)
        assertEquals("click Submit", found.phrase)
    }

    @Test
    fun `all returns all commands`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1"),
            createCommand("click Cancel", "vuid2"),
            createCommand("type Email", "vuid3")
        ))

        val all = registry.all()

        assertEquals(3, all.size)
    }

    @Test
    fun `clear removes all commands`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            createCommand("click Submit", "vuid1")
        ))

        registry.clear()

        assertEquals(0, registry.size)
        assertNull(registry.findByVuid("vuid1"))
    }

    @Test
    fun `update handles commands with null targetAvid`() {
        val registry = CommandRegistry()
        registry.updateSync(listOf(
            QuantizedCommand(
                avid = "",
                phrase = "go back",
                actionType = CommandActionType.NAVIGATE,
                targetAvid = null,
                confidence = 0.9f
            )
        ))

        // Commands with null AVID should not be indexed by AVID
        assertEquals(0, registry.size)
    }

    @Test
    fun `size reflects current command count`() {
        val registry = CommandRegistry()

        assertEquals(0, registry.size)

        registry.updateSync(listOf(
            createCommand("click A", "vuid1"),
            createCommand("click B", "vuid2")
        ))

        assertEquals(2, registry.size)
    }
}

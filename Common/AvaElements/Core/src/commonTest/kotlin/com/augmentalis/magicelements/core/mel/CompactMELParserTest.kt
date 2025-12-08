package com.augmentalis.magicelements.core.mel

import com.augmentalis.magicelements.core.mel.functions.PluginTier
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Integration tests for CompactMELParser.
 * Tests parsing of compact MEL format and full plugin lifecycle.
 */
class CompactMELParserTest {

    // ========== Basic Parsing ==========

    @Test
    fun `parses minimal counter plugin`() {
        val mel = """
            @mel/1.0
            id:com.test.counter
            name:Test Counter
            tier:data

            state:
              count:0

            reducers:
              increment: count=${'$'}add(${'$'}count,1)
              decrement: count=${'$'}sub(${'$'}count,1)

            ui:Col{Text{text:${'$'}str(${'$'}count)}}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        assertNotNull(runtime)
        assertEquals(PluginTier.DATA, runtime.effectiveTier)
        assertEquals("com.test.counter", runtime.getMetadata().id)
    }

    @Test
    fun `parses state with multiple variables`() {
        val mel = """
            @mel/1.0
            id:com.test.multi
            tier:data

            state:
              count:0
              name:"Test"
              enabled:true
              items:[]

            reducers:
              noop: count=${'$'}count

            ui:Text{text:"Hello"}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)
        val state = runtime.getState()

        assertEquals(JsonPrimitive(0), state["count"])
        assertEquals(JsonPrimitive("Test"), state["name"])
        assertEquals(JsonPrimitive(true), state["enabled"])
        assertTrue(state["items"] is JsonArray)
    }

    // ========== Shorthand Function Expansion ==========

    @Test
    fun `expands math shorthands correctly`() {
        val mel = """
            @mel/1.0
            id:com.test.math
            tier:data

            state:
              value:10

            reducers:
              addFive: value=${'$'}add(${'$'}value,5)
              subThree: value=${'$'}sub(${'$'}value,3)
              double: value=${'$'}mul(${'$'}value,2)

            ui:Text{text:${'$'}str(${'$'}value)}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        // Initial value
        assertEquals(JsonPrimitive(10.0), runtime.getState()["value"])

        // Add 5 -> 15
        runtime.dispatch("addFive")
        assertEquals(JsonPrimitive(15.0), runtime.getState()["value"])

        // Sub 3 -> 12
        runtime.dispatch("subThree")
        assertEquals(JsonPrimitive(12.0), runtime.getState()["value"])

        // Double -> 24
        runtime.dispatch("double")
        assertEquals(JsonPrimitive(24.0), runtime.getState()["value"])
    }

    @Test
    fun `expands logic shorthands correctly`() {
        val mel = """
            @mel/1.0
            id:com.test.logic
            tier:data

            state:
              count:5
              label:"neutral"

            reducers:
              checkValue: label=${'$'}if(${'$'}gt(${'$'}count,0),"positive",${'$'}if(${'$'}lt(${'$'}count,0),"negative","zero"))

            ui:Text{text:${'$'}label}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        runtime.dispatch("checkValue")
        assertEquals(JsonPrimitive("positive"), runtime.getState()["label"])
    }

    @Test
    fun `expands string shorthands correctly`() {
        val mel = """
            @mel/1.0
            id:com.test.string
            tier:data

            state:
              greeting:"Hello"
              name:"World"
              message:""

            reducers:
              buildMessage: message=${'$'}cat(${'$'}greeting," ",${'$'}name,"!")

            ui:Text{text:${'$'}message}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        runtime.dispatch("buildMessage")
        assertEquals(JsonPrimitive("Hello World!"), runtime.getState()["message"])
    }

    // ========== Reducer Parameters ==========

    @Test
    fun `parses reducer with parameters`() {
        val mel = """
            @mel/1.0
            id:com.test.params
            tier:data

            state:
              count:0

            reducers:
              addAmount(n): count=${'$'}add(${'$'}count,${'$'}n)
              setTo(value): count=${'$'}value

            ui:Text{text:${'$'}str(${'$'}count)}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        runtime.dispatch("addAmount", mapOf("n" to 10))
        assertEquals(JsonPrimitive(10.0), runtime.getState()["count"])

        runtime.dispatch("addAmount", mapOf("n" to 5))
        assertEquals(JsonPrimitive(15.0), runtime.getState()["count"])

        runtime.dispatch("setTo", mapOf("value" to 100))
        assertEquals(JsonPrimitive(100.0), runtime.getState()["count"])
    }

    @Test
    fun `parses reducer with multiple assignments`() {
        val mel = """
            @mel/1.0
            id:com.test.multi-assign
            tier:data

            state:
              a:0
              b:0
              c:0

            reducers:
              setAll(v): a=${'$'}v; b=${'$'}add(${'$'}v,1); c=${'$'}mul(${'$'}v,2)

            ui:Text{text:${'$'}str(${'$'}a)}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        runtime.dispatch("setAll", mapOf("v" to 5))
        val state = runtime.getState()

        assertEquals(JsonPrimitive(5.0), state["a"])
        assertEquals(JsonPrimitive(6.0), state["b"])
        assertEquals(JsonPrimitive(10.0), state["c"])
    }

    // ========== UI Parsing ==========

    @Test
    fun `parses UI with modifiers`() {
        val mel = """
            @mel/1.0
            id:com.test.ui
            tier:data

            state:
              count:0

            reducers:
              increment: count=${'$'}add(${'$'}count,1)

            ui:Col{@p(16),gap(8);
              Text{text:"Counter";fontSize:24;fontWeight:"bold"};
              Btn{label:"+";@tap->increment;variant:"primary"}
            }
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)
        val ui = runtime.getUIRoot()

        assertNotNull(ui)
        assertEquals("Column", ui.type)
    }

    @Test
    fun `parses UI with bindings`() {
        val mel = """
            @mel/1.0
            id:com.test.bindings
            tier:data

            state:
              value:"Hello"
              color:#333333

            reducers:
              noop: value=${'$'}value

            ui:Text{text:${'$'}value;color:${'$'}color}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)
        val ui = runtime.getUIRoot()

        assertNotNull(ui)
        assertEquals("Text", ui.type)
        assertTrue(ui.bindings.containsKey("text"))
    }

    // ========== Type Alias Expansion ==========

    @Test
    fun `expands type aliases`() {
        // Check that compact types are expanded to full names
        assertEquals("Column", CompactMELParser.TYPE_ALIAS_MAP["Col"])
        assertEquals("Row", CompactMELParser.TYPE_ALIAS_MAP["Row"])
        assertEquals("Button", CompactMELParser.TYPE_ALIAS_MAP["Btn"])
        assertEquals("TextField", CompactMELParser.TYPE_ALIAS_MAP["Field"])
        assertEquals("Checkbox", CompactMELParser.TYPE_ALIAS_MAP["Check"])
        assertEquals("LazyColumn", CompactMELParser.TYPE_ALIAS_MAP["LazyCol"])
    }

    // ========== Calculator Integration ==========

    @Test
    fun `calculator workflow works end-to-end`() {
        val mel = """
            @mel/1.0
            id:com.test.calculator
            name:Calculator
            tier:data

            state:
              display:"0"
              buffer:""
              operator:null

            reducers:
              digit(d): display=${'$'}if(${'$'}eq(${'$'}display,"0"),${'$'}str(${'$'}d),${'$'}cat(${'$'}display,${'$'}str(${'$'}d)))
              clear: display="0"; buffer=""; operator=null
              op(o): buffer=${'$'}display; operator=${'$'}o; display="0"
              calc: display=${'$'}str(${'$'}if(${'$'}eq(${'$'}operator,"+"),${'$'}add(${'$'}num(${'$'}buffer),${'$'}num(${'$'}display)),${'$'}if(${'$'}eq(${'$'}operator,"-"),${'$'}sub(${'$'}num(${'$'}buffer),${'$'}num(${'$'}display)),${'$'}num(${'$'}display)))); buffer=""; operator=null

            ui:Col{@p(16);Text{text:${'$'}display}}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        // Type "12"
        runtime.dispatch("digit", mapOf("d" to 1))
        assertEquals(JsonPrimitive("1"), runtime.getState()["display"])

        runtime.dispatch("digit", mapOf("d" to 2))
        assertEquals(JsonPrimitive("12"), runtime.getState()["display"])

        // Press +
        runtime.dispatch("op", mapOf("o" to "+"))
        assertEquals(JsonPrimitive("12"), runtime.getState()["buffer"])
        assertEquals(JsonPrimitive("+"), runtime.getState()["operator"])
        assertEquals(JsonPrimitive("0"), runtime.getState()["display"])

        // Type "5"
        runtime.dispatch("digit", mapOf("d" to 5))
        assertEquals(JsonPrimitive("5"), runtime.getState()["display"])

        // Press =
        runtime.dispatch("calc")
        assertEquals(JsonPrimitive("17.0"), runtime.getState()["display"])

        // Clear
        runtime.dispatch("clear")
        assertEquals(JsonPrimitive("0"), runtime.getState()["display"])
    }

    // ========== Counter Integration ==========

    @Test
    fun `counter workflow works end-to-end`() {
        val mel = """
            @mel/1.0
            id:com.test.counter
            name:Counter
            tier:data

            state:
              count:0

            reducers:
              increment: count=${'$'}add(${'$'}count,1)
              decrement: count=${'$'}sub(${'$'}count,1)
              reset: count=0
              addAmount(n): count=${'$'}add(${'$'}count,${'$'}n)

            ui:Col{@p(24);
              Text{text:${'$'}str(${'$'}count);fontSize:72};
              Row{@gap(12);
                Btn{label:"-";@tap->decrement};
                Btn{label:"Reset";@tap->reset};
                Btn{label:"+";@tap->increment}
              };
              Row{@gap(8);
                Btn{label:"+5";@tap->addAmount(5)};
                Btn{label:"+10";@tap->addAmount(10)}
              }
            }
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)

        // Increment 3 times
        runtime.dispatch("increment")
        runtime.dispatch("increment")
        runtime.dispatch("increment")
        assertEquals(JsonPrimitive(3.0), runtime.getState()["count"])

        // Decrement once
        runtime.dispatch("decrement")
        assertEquals(JsonPrimitive(2.0), runtime.getState()["count"])

        // Add 10
        runtime.dispatch("addAmount", mapOf("n" to 10))
        assertEquals(JsonPrimitive(12.0), runtime.getState()["count"])

        // Reset
        runtime.dispatch("reset")
        assertEquals(JsonPrimitive(0.0), runtime.getState()["count"])
    }

    // ========== Tier Detection ==========

    @Test
    fun `detects DATA tier correctly`() {
        val mel = """
            @mel/1.0
            id:com.test.data
            tier:data
            state:
              x:0
            reducers:
              noop: x=${'$'}x
            ui:Text{text:"Hello"}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)
        assertEquals(PluginTier.DATA, runtime.effectiveTier)
    }

    @Test
    fun `detects LOGIC tier correctly`() {
        val mel = """
            @mel/1.0
            id:com.test.logic
            tier:logic
            state:
              x:0
            reducers:
              noop: x=${'$'}x
            ui:Text{text:"Hello"}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.ANDROID)
        assertEquals(PluginTier.LOGIC, runtime.effectiveTier)
    }

    @Test
    fun `downgrades LOGIC to DATA on iOS`() {
        val mel = """
            @mel/1.0
            id:com.test.logic
            tier:logic
            state:
              x:0
            reducers:
              noop: x=${'$'}x
            ui:Text{text:"Hello"}
        """.trimIndent()

        val runtime = CompactMELParser.parse(mel, Platform.IOS)

        assertEquals(PluginTier.DATA, runtime.effectiveTier)
        assertTrue(runtime.getTierInfo().downgraded)
    }

    // ========== Error Handling ==========

    @Test
    fun `throws on missing header`() {
        val mel = """
            id:com.test.bad
            tier:data
            state:
              x:0
            ui:Text{text:"Hello"}
        """.trimIndent()

        assertFailsWith<Exception> {
            CompactMELParser.parse(mel, Platform.ANDROID)
        }
    }

    @Test
    fun `throws on missing id`() {
        val mel = """
            @mel/1.0
            tier:data
            state:
              x:0
            ui:Text{text:"Hello"}
        """.trimIndent()

        assertFailsWith<Exception> {
            CompactMELParser.parse(mel, Platform.ANDROID)
        }
    }
}

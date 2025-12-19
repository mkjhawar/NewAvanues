/**
 * NavigationGraphTest.kt - Unit tests for NavigationGraph
 *
 * Tests graph operations: pathfinding, edge queries, and statistics.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-18
 */

package com.augmentalis.learnapp.navigation

import com.augmentalis.learnapp.models.NavigationEdge
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for NavigationGraph
 *
 * Tests:
 * - Empty graph behavior
 * - Single node graph
 * - Path finding (BFS)
 * - Edge queries
 * - Graph statistics
 */
class NavigationGraphTest {

    // ============================================================
    // Empty Graph Tests
    // ============================================================

    @Test
    fun `empty graph has no nodes or edges`() {
        val graph = NavigationGraph(
            packageName = "com.test.app",
            nodes = emptyMap(),
            edges = emptyList()
        )

        assertEquals(0, graph.nodes.size)
        assertEquals(0, graph.edges.size)
    }

    @Test
    fun `empty graph stats are zeros`() {
        val graph = NavigationGraph(
            packageName = "com.test.app",
            nodes = emptyMap(),
            edges = emptyList()
        )

        val stats = graph.getStats()

        assertEquals(0, stats.totalScreens)
        assertEquals(0, stats.totalElements)
        assertEquals(0, stats.totalEdges)
        assertEquals(0f, stats.averageOutDegree, 0.01f)
        assertEquals(0, stats.maxDepth)
    }

    // ============================================================
    // Single Node Tests
    // ============================================================

    @Test
    fun `single node graph has no outgoing edges`() {
        val node = ScreenNode(screenHash = "screen1", elements = listOf("elem1", "elem2"))
        val graph = NavigationGraph(
            packageName = "com.test.app",
            nodes = mapOf("screen1" to node),
            edges = emptyList()
        )

        val outgoing = graph.getOutgoingEdges("screen1")

        assertEquals(0, outgoing.size)
    }

    @Test
    fun `single node path to itself is single node`() {
        val node = ScreenNode(screenHash = "screen1")
        val graph = NavigationGraph(
            packageName = "com.test.app",
            nodes = mapOf("screen1" to node),
            edges = emptyList()
        )

        val path = graph.findPath("screen1", "screen1")

        assertEquals(listOf("screen1"), path)
    }

    // ============================================================
    // Path Finding Tests
    // ============================================================

    @Test
    fun `finds direct path between adjacent nodes`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val path = graph.findPath("A", "B")

        assertEquals(listOf("A", "B"), path)
    }

    @Test
    fun `finds multi-hop path`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn2", toScreenHash = "C")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val path = graph.findPath("A", "C")

        assertEquals(listOf("A", "B", "C"), path)
    }

    @Test
    fun `returns empty list when no path exists`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C")
        )
        // A -> B, but no path to C
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val path = graph.findPath("A", "C")

        assertEquals(emptyList<String>(), path)
    }

    @Test
    fun `handles cycles without infinite loop`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C")
        )
        // A -> B -> C -> A (cycle)
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn2", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "C", clickedElementUuid = "btn3", toScreenHash = "A")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val path = graph.findPath("A", "C")

        assertEquals(listOf("A", "B", "C"), path)
    }

    // ============================================================
    // Edge Query Tests
    // ============================================================

    @Test
    fun `getOutgoingEdges returns correct edges`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn2", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn3", toScreenHash = "C")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val outgoingA = graph.getOutgoingEdges("A")

        assertEquals(2, outgoingA.size)
        assertTrue(outgoingA.any { it.toScreenHash == "B" })
        assertTrue(outgoingA.any { it.toScreenHash == "C" })
    }

    @Test
    fun `getIncomingEdges returns correct edges`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn2", toScreenHash = "C")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val incomingC = graph.getIncomingEdges("C")

        assertEquals(2, incomingC.size)
        assertTrue(incomingC.any { it.fromScreenHash == "A" })
        assertTrue(incomingC.any { it.fromScreenHash == "B" })
    }

    @Test
    fun `getReachableScreens returns direct neighbors`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C"),
            "D" to ScreenNode(screenHash = "D")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn2", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn3", toScreenHash = "D")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val reachable = graph.getReachableScreens("A")

        assertEquals(2, reachable.size)
        assertTrue(reachable.contains("B"))
        assertTrue(reachable.contains("C"))
        assertFalse(reachable.contains("D")) // D is 2 hops away
    }

    // ============================================================
    // Statistics Tests
    // ============================================================

    @Test
    fun `getStats calculates correct values`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A", elements = listOf("e1", "e2", "e3")),
            "B" to ScreenNode(screenHash = "B", elements = listOf("e4", "e5")),
            "C" to ScreenNode(screenHash = "C", elements = listOf("e6"))
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn2", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn3", toScreenHash = "C")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val stats = graph.getStats()

        assertEquals(3, stats.totalScreens)
        assertEquals(6, stats.totalElements) // 3 + 2 + 1
        assertEquals(3, stats.totalEdges)
        assertEquals(1.0f, stats.averageOutDegree, 0.01f) // 3 edges / 3 nodes
    }

    @Test
    fun `maxDepth calculated correctly for linear graph`() {
        val nodes = mapOf(
            "A" to ScreenNode(screenHash = "A"),
            "B" to ScreenNode(screenHash = "B"),
            "C" to ScreenNode(screenHash = "C"),
            "D" to ScreenNode(screenHash = "D")
        )
        val edges = listOf(
            NavigationEdge(fromScreenHash = "A", clickedElementUuid = "btn1", toScreenHash = "B"),
            NavigationEdge(fromScreenHash = "B", clickedElementUuid = "btn2", toScreenHash = "C"),
            NavigationEdge(fromScreenHash = "C", clickedElementUuid = "btn3", toScreenHash = "D")
        )
        val graph = NavigationGraph("com.test", nodes, edges)

        val stats = graph.getStats()

        assertEquals(3, stats.maxDepth) // A(0) -> B(1) -> C(2) -> D(3)
    }

    // ============================================================
    // ScreenNode Tests
    // ============================================================

    @Test
    fun `ScreenNode default values are correct`() {
        val node = ScreenNode(screenHash = "test123")

        assertEquals("test123", node.screenHash)
        assertNull(node.activityName)
        assertEquals(emptyList<String>(), node.elements)
        assertTrue(node.timestamp > 0)
    }

    @Test
    fun `ScreenNode with all values`() {
        val timestamp = System.currentTimeMillis()
        val node = ScreenNode(
            screenHash = "test123",
            activityName = "MainActivity",
            elements = listOf("e1", "e2"),
            timestamp = timestamp
        )

        assertEquals("test123", node.screenHash)
        assertEquals("MainActivity", node.activityName)
        assertEquals(listOf("e1", "e2"), node.elements)
        assertEquals(timestamp, node.timestamp)
    }
}

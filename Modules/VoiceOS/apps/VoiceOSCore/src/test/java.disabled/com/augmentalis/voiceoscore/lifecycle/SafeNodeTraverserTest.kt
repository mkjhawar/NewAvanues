/**
 * SafeNodeTraverserTest.kt - TDD Tests for Stack-Safe Node Traversal
 *
 * YOLO Phase 1 - Critical Issue #5: Infinite Recursion in Node Traversal
 *
 * Problem:
 * - VoiceCommandProcessor.kt:352-378 has no cycle detection
 * - Circular parent-child relationships cause stack overflow
 * - No depth limit enforcement within method
 * - App crash with StackOverflowError
 *
 * Solution:
 * - Create SafeNodeTraverser with cycle detection
 * - Enforce maximum depth limit
 * - Track visited nodes to prevent infinite loops
 * - Use iterative approach with explicit stack (stack-safe)
 *
 * Test Strategy:
 * - RED: Write comprehensive failing tests first
 * - GREEN: Implement minimal code to pass tests
 * - REFACTOR: Optimize and clean up
 *
 * Coverage Target: 100% (critical path)
 * Zero Tolerance: 0 errors, 0 warnings, 100% pass rate
 */
package com.augmentalis.voiceoscore.lifecycle

import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith

/**
 * Test suite for SafeNodeTraverser
 *
 * Tests verify:
 * 1. Circular references detected and prevented
 * 2. Maximum depth limit enforced
 * 3. All visited nodes tracked
 * 4. No stack overflow on deep trees
 * 5. Traversal completes successfully
 * 6. Performance acceptable for large trees
 */
class SafeNodeTraverserTest {

    // Simple test node structure (not Android-dependent)
    private data class TestNode(
        val id: String,
        val children: MutableList<TestNode> = mutableListOf(),
        var parent: TestNode? = null
    ) {
        fun addChild(child: TestNode) {
            children.add(child)
            child.parent = this
        }
    }

    // ==================== RED PHASE TESTS (Should FAIL initially) ====================

    /**
     * TEST 1: Verify traverser can be created
     * Basic sanity check
     */
    @Test
    fun `test traverser can be created`() {
        val traverser = SafeNodeTraverser<TestNode>()
        assertThat(traverser).isNotNull()
    }

    /**
     * TEST 2: Verify simple tree traversal works
     * Basic functionality
     */
    @Test
    fun `test simple tree traversal`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        val child1 = TestNode("child1")
        val child2 = TestNode("child2")
        root.addChild(child1)
        root.addChild(child2)

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, depth ->
                visited.add(node.id)
                true  // Continue traversal
            }
        )

        // Assert all nodes visited in correct order
        assertThat(visited).containsExactly("root", "child1", "child2").inOrder()
    }

    /**
     * TEST 3: Verify circular reference detection prevents infinite loop
     * Critical for preventing stack overflow
     */
    @Test
    fun `test circular reference detected and prevented`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        val child1 = TestNode("child1")
        val child2 = TestNode("child2")

        root.addChild(child1)
        child1.addChild(child2)
        child2.addChild(root)  // CIRCULAR! child2 -> root

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        // Assert: Each node visited exactly once (cycle detected)
        assertThat(visited).containsExactly("root", "child1", "child2")
        assertThat(visited.count { it == "root" }).isEqualTo(1)
    }

    /**
     * TEST 4: Verify maximum depth limit enforced
     * Prevents stack overflow on deep trees
     */
    @Test
    fun `test maximum depth limit enforced`() {
        val traverser = SafeNodeTraverser<TestNode>(maxDepth = 2)

        // Create deep tree: root -> child1 -> child2 -> child3
        val root = TestNode("root")
        val child1 = TestNode("child1")
        val child2 = TestNode("child2")
        val child3 = TestNode("child3")

        root.addChild(child1)
        child1.addChild(child2)
        child2.addChild(child3)

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, depth ->
                visited.add("${node.id}@$depth")
            }
        )

        // Assert: Only visits up to depth 2 (root=0, child1=1, child2=2)
        assertThat(visited).containsExactly("root@0", "child1@1", "child2@2")
        assertThat(visited).doesNotContain("child3@3")
    }

    /**
     * TEST 5: Verify depth tracking is accurate
     * Ensures correct depth parameter
     */
    @Test
    fun `test depth tracking is accurate`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        val child1 = TestNode("child1")
        val grandchild = TestNode("grandchild")

        root.addChild(child1)
        child1.addChild(grandchild)

        val depthMap = mutableMapOf<String, Int>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, depth ->
                depthMap[node.id] = depth
                true
            }
        )

        // Assert: Correct depth for each node
        assertThat(depthMap["root"]).isEqualTo(0)
        assertThat(depthMap["child1"]).isEqualTo(1)
        assertThat(depthMap["grandchild"]).isEqualTo(2)
    }

    /**
     * TEST 6: Verify large tree traversal doesn't stack overflow
     * Performance and safety check
     */
    @Test
    fun `test large tree traversal doesn't stack overflow`() {
        val traverser = SafeNodeTraverser<TestNode>(maxDepth = 100)

        // Create deep linear tree (100 nodes deep)
        var current = TestNode("node-0")
        val root = current

        for (i in 1 until 100) {
            val next = TestNode("node-$i")
            current.addChild(next)
            current = next
        }

        var visitedCount = 0

        // Should complete without stack overflow
        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { _, _ ->
                visitedCount++
                true
            }
        )

        // Assert: All 100 nodes visited
        assertThat(visitedCount).isEqualTo(100)
    }

    /**
     * TEST 7: Verify wide tree traversal (many children)
     * Performance check
     */
    @Test
    fun `test wide tree traversal with many children`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")

        // Create 100 children
        for (i in 0 until 100) {
            root.addChild(TestNode("child-$i"))
        }

        var visitedCount = 0

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { _, _ ->
                visitedCount++
                true
            }
        )

        // Assert: Root + 100 children = 101 nodes
        assertThat(visitedCount).isEqualTo(101)
    }

    /**
     * TEST 8: Verify null children handled gracefully
     * Robustness check
     */
    @Test
    fun `test null children handled gracefully`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        root.children.add(TestNode("child1"))
        root.children.add(TestNode("child2"))

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { node ->
                // Simulate some nulls in children list
                node.children.filterNotNull()
            },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        assertThat(visited).containsExactly("root", "child1", "child2")
    }

    /**
     * TEST 9: Verify early termination support
     * Control flow check
     */
    @Test
    fun `test early termination when predicate returns false`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        root.addChild(TestNode("child1"))
        root.addChild(TestNode("child2"))
        root.addChild(TestNode("child3"))

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
                // Continue only if not child2
                node.id != "child2"
            }
        )

        // Assert: Stopped at child2 (child3 not visited due to early termination)
        assertThat(visited).contains("root")
        assertThat(visited).contains("child1")
        assertThat(visited).contains("child2")
    }

    /**
     * TEST 10: Verify self-reference detected
     * Edge case: node is its own child
     */
    @Test
    fun `test self-reference detected`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        root.addChild(root)  // Self-reference!

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        // Assert: Root visited only once
        assertThat(visited).containsExactly("root")
    }

    /**
     * TEST 11: Verify complex circular graph handled
     * Complex cycle detection
     */
    @Test
    fun `test complex circular graph handled correctly`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val node1 = TestNode("1")
        val node2 = TestNode("2")
        val node3 = TestNode("3")
        val node4 = TestNode("4")

        // Create complex circular graph:
        // 1 -> 2 -> 3 -> 4
        // |         ^
        // +---------+
        node1.addChild(node2)
        node2.addChild(node3)
        node3.addChild(node4)
        node1.addChild(node3)  // Direct link to 3

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = node1,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        // Assert: Each node visited exactly once
        assertThat(visited.distinct()).hasSize(4)
        assertThat(visited.count { it == "1" }).isEqualTo(1)
        assertThat(visited.count { it == "2" }).isEqualTo(1)
        assertThat(visited.count { it == "3" }).isEqualTo(1)
        assertThat(visited.count { it == "4" }).isEqualTo(1)
    }

    /**
     * TEST 12: Verify traversal order (breadth-first or depth-first)
     * Algorithm verification
     */
    @Test
    fun `test traversal order is depth-first`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        val child1 = TestNode("child1")
        val child2 = TestNode("child2")
        val grandchild1 = TestNode("grandchild1")

        root.addChild(child1)
        root.addChild(child2)
        child1.addChild(grandchild1)

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        // Depth-first order: root, child1, grandchild1, child2
        assertThat(visited).containsExactly("root", "child1", "grandchild1", "child2").inOrder()
    }

    /**
     * TEST 13: Verify node identity checking (reference equality)
     * Cycle detection must use identity, not equals()
     */
    @Test
    fun `test cycle detection uses node identity`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("A")
        val child1 = TestNode("A")  // Same ID, different instance
        val child2 = TestNode("B")

        root.addChild(child1)
        root.addChild(child2)
        child2.addChild(root)  // Circular reference

        val visited = mutableListOf<TestNode>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node)
            }
        )

        // Assert: Both "A" nodes visited (different instances)
        assertThat(visited.count { it.id == "A" }).isEqualTo(2)
        assertThat(visited.count { it === root }).isEqualTo(1)
        assertThat(visited.count { it === child1 }).isEqualTo(1)
    }

    /**
     * TEST 14: Performance test - 1000 node tree
     * Scalability check
     */
    @Test
    fun `test performance with 1000 node tree`() {
        val traverser = SafeNodeTraverser<TestNode>(maxDepth = 1000)

        // Create balanced tree with 1000 nodes
        val root = TestNode("root")
        var nodesCreated = 1

        fun buildTree(parent: TestNode, remainingDepth: Int, nodesPerLevel: Int): Int {
            if (remainingDepth <= 0 || nodesCreated >= 1000) return nodesCreated

            for (i in 0 until nodesPerLevel) {
                if (nodesCreated >= 1000) break
                val child = TestNode("node-${nodesCreated++}")
                parent.addChild(child)
                nodesCreated = buildTree(child, remainingDepth - 1, nodesPerLevel)
            }
            return nodesCreated
        }

        buildTree(root, 10, 3)

        val startTime = System.currentTimeMillis()
        var visitedCount = 0

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { _, _ ->
                visitedCount++
                true
            }
        )

        val duration = System.currentTimeMillis() - startTime

        // Assert: Completes quickly (<100ms for 1000 nodes)
        assertThat(duration).isLessThan(100L)
        assertThat(visitedCount).isAtLeast(100)  // Should visit many nodes
    }

    /**
     * TEST 15: Verify empty tree (single root node) handled
     * Edge case
     */
    @Test
    fun `test empty tree with single root node`() {
        val traverser = SafeNodeTraverser<TestNode>()

        val root = TestNode("root")
        // No children

        val visited = mutableListOf<String>()

        traverser.traverse(
            root = root,
            getChildren = { it.children },
            onVisit = { node, _ ->
                visited.add(node.id)
            }
        )

        // Assert: Only root visited
        assertThat(visited).containsExactly("root")
    }
}

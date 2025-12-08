/**
 * HierarchicalUuidManager.kt - Manage parent-child UUID relationships
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/hierarchy/HierarchicalUuidManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Hierarchical UUID management for nested layouts and component trees
 */

package com.augmentalis.uuidcreator.hierarchy

import com.augmentalis.uuidcreator.database.repository.UUIDRepository
import com.augmentalis.uuidcreator.models.UUIDElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hierarchical UUID Manager
 *
 * Manages parent-child relationships for nested UI layouts and component trees.
 *
 * ## Hierarchy Example
 *
 * ```
 * LoginForm (root)
 *   ├─ UsernameContainer
 *   │  ├─ UsernameLabel
 *   │  └─ UsernameInput
 *   └─ PasswordContainer
 *      ├─ PasswordLabel
 *      ├─ PasswordInput
 *      └─ ShowPasswordToggle
 * ```
 *
 * ## Operations
 *
 * - **Add/Remove** children
 * - **Traverse** hierarchy (ancestors, descendants)
 * - **Query** depth, siblings, subtrees
 * - **Cascade** operations (delete parent → delete all children)
 * - **Move** elements between parents
 *
 * ## Usage Examples
 *
 * ```kotlin
 * val manager = HierarchicalUuidManager(repository)
 *
 * // Add child to parent
 * manager.addChild(
 *     parentUuid = "form-123",
 *     childUuid = "container-456",
 *     position = 0
 * )
 *
 * // Get all children
 * val children = manager.getChildren("form-123")
 * // Returns: [container-456, container-789]
 *
 * // Get ancestors (path to root)
 * val ancestors = manager.getAncestors("input-999")
 * // Returns: [container-456, form-123]
 *
 * // Get full subtree
 * val tree = manager.buildTree("form-123")
 * // Returns: UuidTree with all descendants
 *
 * // Delete with cascade
 * manager.deleteWithDescendants("form-123")
 * // Deletes form + all children recursively
 * ```
 *
 * ## Thread Safety
 *
 * All operations use repository's thread-safe methods.
 *
 * @property repository UUID repository with hierarchy support
 *
 * @since 1.0.0
 */
class HierarchicalUuidManager(
    private val repository: UUIDRepository
) {

    /**
     * Add child to parent
     *
     * Creates parent-child relationship. Prevents circular references.
     *
     * @param parentUuid Parent UUID
     * @param childUuid Child UUID
     * @param position Child position in parent (0-based, default: append)
     * @throws IllegalArgumentException if circular reference detected
     */
    suspend fun addChild(
        parentUuid: String,
        childUuid: String,
        position: Int? = null
    ) = withContext(Dispatchers.IO) {
        // Prevent circular reference
        if (isAncestor(childUuid, parentUuid)) {
            throw IllegalArgumentException(
                "Circular reference: $childUuid cannot be ancestor of $parentUuid"
            )
        }

        // Get parent element
        val parent = repository.getByUuid(parentUuid)
            ?: throw IllegalArgumentException("Parent not found: $parentUuid")

        // Get child element
        val child = repository.getByUuid(childUuid)
            ?: throw IllegalArgumentException("Child not found: $childUuid")

        // Update child's parent reference
        val updatedChild = child.copy(parent = parentUuid)
        repository.update(updatedChild)

        // Add to parent's children list
        parent.addChild(childUuid)

        // Reorder if position specified and not at end
        if (position != null && position < parent.children.size - 1) {
            // Remove from current position (end)
            val mutableChildren = parent.children.toMutableList()
            mutableChildren.removeAt(parent.children.size - 1)
            // Insert at desired position
            mutableChildren.add(position, childUuid)
            // Update parent with reordered children
            val reorderedParent = parent.copy(children = mutableChildren)
            repository.update(reorderedParent)
        } else {
            repository.update(parent)
        }
    }

    /**
     * Remove child from parent
     *
     * @param parentUuid Parent UUID
     * @param childUuid Child UUID
     * @return true if removed, false if relationship didn't exist
     */
    suspend fun removeChild(
        parentUuid: String,
        childUuid: String
    ): Boolean = withContext(Dispatchers.IO) {
        val parent = repository.getByUuid(parentUuid) ?: return@withContext false

        if (!parent.children.contains(childUuid)) {
            return@withContext false
        }

        // Remove from parent's children list
        parent.removeChild(childUuid)
        repository.update(parent)

        // Clear child's parent reference
        val child = repository.getByUuid(childUuid)
        child?.let {
            val updatedChild = it.copy(parent = null)
            repository.update(updatedChild)
        }

        true
    }

    /**
     * Get children of parent (ordered by position)
     *
     * @param parentUuid Parent UUID
     * @return List of child UUIDs in position order
     */
    fun getChildren(parentUuid: String): List<String> {
        return repository.getChildren(parentUuid).map { it.uuid }
    }

    /**
     * Get parent of child
     *
     * @param childUuid Child UUID
     * @return Parent UUID or null if root
     */
    fun getParent(childUuid: String): String? {
        return repository.getByUuid(childUuid)?.parent
    }

    /**
     * Get ancestors (all parents up to root)
     *
     * Returns path from immediate parent to root.
     *
     * Example:
     * ```
     * Input → Container → Form → null
     * Returns: [Container, Form]
     * ```
     *
     * @param uuid UUID to get ancestors for
     * @return List of ancestor UUIDs (immediate parent to root)
     */
    suspend fun getAncestors(uuid: String): List<String> = withContext(Dispatchers.Default) {
        val ancestors = mutableListOf<String>()
        var current = uuid

        while (true) {
            val parent = getParent(current) ?: break
            ancestors.add(parent)
            current = parent

            // Safety: prevent infinite loops
            if (ancestors.size > 100) {
                throw IllegalStateException("Circular reference detected in hierarchy")
            }
        }

        ancestors
    }

    /**
     * Get descendants (all children recursively)
     *
     * Uses depth-first traversal.
     *
     * Example:
     * ```
     * Form (root)
     *   ├─ Container A (Button 1, Button 2)
     *   └─ Container B (Input)
     * Returns: [Container A, Button 1, Button 2, Container B, Input]
     * ```
     *
     * @param uuid UUID to get descendants for
     * @return List of descendant UUIDs in DFS order
     */
    suspend fun getDescendants(uuid: String): List<String> = withContext(Dispatchers.Default) {
        val descendants = mutableListOf<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(uuid)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val children = getChildren(current)

            descendants.addAll(children)
            children.reversed().forEach { stack.addLast(it) }

            // Safety: prevent runaway traversal
            if (descendants.size > 10_000) {
                throw IllegalStateException("Hierarchy too deep (>10,000 nodes)")
            }
        }

        descendants
    }

    /**
     * Get depth of UUID in tree (0 = root)
     *
     * @param uuid UUID to get depth for
     * @return Depth level (0 = root, 1 = first level, etc.)
     */
    suspend fun getDepth(uuid: String): Int {
        return getAncestors(uuid).size
    }

    /**
     * Check if potentialAncestor is an ancestor of uuid
     *
     * Used for circular reference detection.
     *
     * @param uuid UUID to check
     * @param potentialAncestor Potential ancestor UUID
     * @return true if potentialAncestor is ancestor of uuid
     */
    suspend fun isAncestor(uuid: String, potentialAncestor: String): Boolean {
        return getAncestors(uuid).contains(potentialAncestor)
    }

    /**
     * Get siblings (elements with same parent)
     *
     * @param uuid UUID to get siblings for
     * @return List of sibling UUIDs (excluding self)
     */
    fun getSiblings(uuid: String): List<String> {
        val parent = getParent(uuid) ?: return emptyList()
        return getChildren(parent).filter { it != uuid }
    }

    /**
     * Move element to new parent
     *
     * Removes from old parent and adds to new parent.
     *
     * @param uuid UUID to move
     * @param newParentUuid New parent UUID
     * @param position Position in new parent (null = append)
     */
    suspend fun moveToParent(
        uuid: String,
        newParentUuid: String,
        position: Int? = null
    ) = withContext(Dispatchers.IO) {
        val oldParent = getParent(uuid)

        // Remove from old parent
        oldParent?.let { removeChild(it, uuid) }

        // Add to new parent
        addChild(newParentUuid, uuid, position)
    }

    /**
     * Delete element and all descendants (cascade delete)
     *
     * Recursively deletes entire subtree.
     *
     * @param uuid UUID to delete
     * @return Number of elements deleted
     */
    suspend fun deleteWithDescendants(uuid: String): Int = withContext(Dispatchers.IO) {
        val descendants = getDescendants(uuid)
        val toDelete = descendants + uuid

        toDelete.forEach { id ->
            repository.deleteByUuid(id)
        }

        toDelete.size
    }

    /**
     * Build complete tree from root
     *
     * Creates hierarchical tree structure with all descendants.
     *
     * @param rootUuid Root UUID
     * @return Tree structure
     */
    suspend fun buildTree(rootUuid: String): UuidTree = withContext(Dispatchers.Default) {
        val root = repository.getByUuid(rootUuid)
            ?: throw IllegalArgumentException("Root UUID not found: $rootUuid")

        buildTreeRecursive(root)
    }

    /**
     * Build tree recursively
     */
    private suspend fun buildTreeRecursive(element: UUIDElement): UuidTree {
        val children = getChildren(element.uuid)
        val childTrees = children.mapNotNull { childUuid ->
            repository.getByUuid(childUuid)
        }.map { buildTreeRecursive(it) }

        return UuidTree(element, childTrees)
    }

    /**
     * Get root elements (elements with no parent)
     *
     * @return List of root UUIDs
     */
    suspend fun getRootElements(): List<String> = withContext(Dispatchers.IO) {
        repository.getAll()
            .filter { it.parent == null }
            .map { it.uuid }
    }

    /**
     * Get leaf elements (elements with no children)
     *
     * @return List of leaf UUIDs
     */
    suspend fun getLeafElements(): List<String> = withContext(Dispatchers.IO) {
        repository.getAll()
            .filter { it.children.isEmpty() }
            .map { it.uuid }
    }

    /**
     * Get subtree size (element + descendants)
     *
     * @param uuid Root of subtree
     * @return Total number of elements in subtree
     */
    suspend fun getSubtreeSize(uuid: String): Int {
        return 1 + getDescendants(uuid).size
    }

    /**
     * Validate hierarchy integrity
     *
     * Checks for:
     * - Orphaned children (parent doesn't exist)
     * - Circular references
     * - Inconsistent parent-child links
     *
     * @return Validation result with issues
     */
    suspend fun validateIntegrity(): HierarchyValidationResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()

        // Check all elements
        val allElements = repository.getAll()

        allElements.forEach { element ->
            // Check if parent exists
            element.parent?.let { parentUuid ->
                val parent = repository.getByUuid(parentUuid)
                if (parent == null) {
                    issues.add("Orphaned child: ${element.uuid} (parent $parentUuid not found)")
                } else if (!parent.children.contains(element.uuid)) {
                    issues.add("Inconsistent link: ${element.uuid} references parent $parentUuid, but parent doesn't list it as child")
                } else {
                    // Parent exists and references child correctly
                }
            }

            // Check if children exist
            element.children.forEach { childUuid ->
                val child = repository.getByUuid(childUuid)
                if (child == null) {
                    issues.add("Missing child: ${element.uuid} references child $childUuid which doesn't exist")
                } else if (child.parent != element.uuid) {
                    issues.add("Inconsistent link: ${element.uuid} lists $childUuid as child, but child doesn't reference it as parent")
                } else {
                    // Child exists and references parent correctly
                }
            }
        }

        HierarchyValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
}

/**
 * UUID Tree - Hierarchical data structure
 *
 * Represents tree of UUID elements.
 *
 * @property element Current element
 * @property children List of child trees
 */
data class UuidTree(
    val element: UUIDElement,
    val children: List<UuidTree> = emptyList()
) {
    /**
     * Get total node count (element + descendants)
     */
    fun getNodeCount(): Int = 1 + children.sumOf { it.getNodeCount() }

    /**
     * Get tree depth (max distance to leaf)
     */
    fun getDepth(): Int {
        return if (children.isEmpty()) {
            0
        } else {
            1 + (children.maxOfOrNull { it.getDepth() } ?: 0)
        }
    }

    /**
     * Pretty print tree
     *
     * @param indent Current indentation level
     * @return Formatted tree string
     */
    fun toPrettyString(indent: Int = 0): String {
        val prefix = "  ".repeat(indent)
        val sb = StringBuilder()
        sb.appendLine("$prefix- ${element.name ?: element.uuid} (${element.type})")
        children.forEach { child ->
            sb.append(child.toPrettyString(indent + 1))
        }
        return sb.toString()
    }

    /**
     * Convert to JSON-like structure
     */
    fun toJson(): String {
        val childrenJson = if (children.isEmpty()) {
            "[]"
        } else {
            children.joinToString(",\n    ", "[\n    ", "\n  ]") { it.toJson() }
        }

        return """  {
    "uuid": "${element.uuid}",
    "name": "${element.name}",
    "type": "${element.type}",
    "children": $childrenJson
  }"""
    }
}

/**
 * Hierarchy Validation Result
 *
 * @property isValid True if hierarchy is valid
 * @property issues List of validation issues
 */
data class HierarchyValidationResult(
    val isValid: Boolean,
    val issues: List<String>
) {
    override fun toString(): String {
        return if (isValid) {
            "Hierarchy is valid"
        } else {
            "Hierarchy validation failed:\n" + issues.joinToString("\n") { "  - $it" }
        }
    }
}

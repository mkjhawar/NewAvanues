package com.avanues.cockpit.rendering

/**
 * Container for the entire 3D scene.
 */
class SceneGraph {
    val root = object : RenderNode("root") {
        override fun render(context: Any) {
            // Root renders nothing itself, just children
            children.forEach { it.render(context) }
        }
    }

    fun addNode(node: RenderNode) {
        root.addChild(node)
    }

    fun removeNode(id: String) {
        // Recursive find and remove (simplified for now)
        root.children.removeIf { it.id == id }
    }
    
    fun render(context: Any) {
        root.render(context)
    }
}

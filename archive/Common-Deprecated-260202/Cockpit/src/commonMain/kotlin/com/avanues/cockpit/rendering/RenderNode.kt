package com.avanues.cockpit.rendering

import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.Quaternion

/**
 * Base class for any object that can be rendered in the scene.
 */
abstract class RenderNode(
    val id: String
) {
    var position: Vector3D = Vector3D.ZERO
    var rotation: Quaternion = Quaternion.IDENTITY
    var scale: Vector3D = Vector3D(1f, 1f, 1f)
    var visible: Boolean = true
    
    val children = mutableListOf<RenderNode>()

    fun addChild(node: RenderNode) {
        children.add(node)
    }

    fun removeChild(node: RenderNode) {
        children.remove(node)
    }
    
    abstract fun render(context: Any) // Context type depends on platform (Canvas, ARScene, etc.)
}

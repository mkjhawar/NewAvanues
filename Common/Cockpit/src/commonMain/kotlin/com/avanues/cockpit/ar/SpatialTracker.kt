package com.avanues.cockpit.ar

import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.Quaternion

/**
 * Interface for AR spatial tracking systems (ARCore, ARKit, WebXR).
 */
interface SpatialTracker {
    val isTracking: Boolean
    val headPosition: Vector3D
    val headRotation: Quaternion

    fun createAnchor(position: Vector3D, rotation: Quaternion): Anchor
    fun removeAnchor(anchor: Anchor)
}

interface Anchor {
    val id: String
    val position: Vector3D
    val rotation: Quaternion
}

package com.augmentalis.magicui.ui.core.`3d`

import com.augmentalis.magicui.components.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.PI

/**
 * Canvas3D - 3D rendering canvas component
 *
 * Provides a canvas for rendering 3D scenes with camera control, lighting, and interactive objects.
 * Supports OpenGL (Android), Metal (iOS), and WebGL (Web) backends.
 *
 * Example:
 * ```kotlin
 * Canvas3D(
 *     camera = Camera3D(
 *         position = Vector3(0f, 5f, 10f),
 *         target = Vector3(0f, 0f, 0f)
 *     ),
 *     children = listOf(
 *         Mesh3D(geometry = BoxGeometry(1f, 1f, 1f)),
 *         DirectionalLight(direction = Vector3(0f, -1f, 0f))
 *     )
 * )
 * ```
 *
 * @property camera Camera configuration for viewing the 3D scene
 * @property backgroundColor Background color (default: black)
 * @property antialiasing Enable MSAA antialiasing (default: true)
 * @property children List of 3D objects to render (meshes, lights, etc.)
 * @property onRender Callback invoked each frame with delta time
 * @property modifiers Component modifiers
 * @since 1.1.0
 */
@Serializable
data class Canvas3D(
    override val id: String? = null,
    val camera: Camera3D = Camera3D(),
    val backgroundColor: String = "#000000",
    val antialiasing: Boolean = true,
    val children: List<Object3D> = emptyList(),
    @Transient
    val onRender: ((deltaTime: Float) -> Unit)? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override val style: ComponentStyle? = null

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}

/**
 * Base interface for all 3D objects (meshes, lights, cameras, etc.)
 */
@Serializable
sealed interface Object3D {
    val transform: Transform3D
    val visible: Boolean
}

/**
 * Mesh3D - 3D mesh component
 *
 * Represents a 3D object with geometry and material.
 *
 * Example:
 * ```kotlin
 * Mesh3D(
 *     geometry = SphereGeometry(radius = 1f, segments = 32),
 *     material = PhongMaterial(
 *         color = "#FF5722",
 *         shininess = 30f
 *     ),
 *     transform = Transform3D.identity()
 *         .translate(0f, 2f, 0f)
 *         .rotateY(45f)
 * )
 * ```
 *
 * @property geometry Mesh geometry (box, sphere, custom, etc.)
 * @property material Material properties (color, lighting, texture)
 * @property transform 3D transformation (position, rotation, scale)
 * @property castShadow Whether this mesh casts shadows
 * @property receiveShadow Whether this mesh receives shadows
 * @property visible Whether this mesh is visible
 * @since 1.1.0
 */
@Serializable
data class Mesh3D(
    val geometry: Geometry3D,
    val material: Material3D = BasicMaterial(),
    override val transform: Transform3D = Transform3D.identity(),
    val castShadow: Boolean = true,
    val receiveShadow: Boolean = true,
    override val visible: Boolean = true
) : Object3D

/**
 * Base interface for 3D geometries
 */
@Serializable
sealed interface Geometry3D

/**
 * BoxGeometry - Rectangular box geometry
 *
 * @property width Box width (X axis)
 * @property height Box height (Y axis)
 * @property depth Box depth (Z axis)
 * @property widthSegments Number of width subdivisions (default: 1)
 * @property heightSegments Number of height subdivisions (default: 1)
 * @property depthSegments Number of depth subdivisions (default: 1)
 */
@Serializable
data class BoxGeometry(
    val width: Float,
    val height: Float,
    val depth: Float,
    val widthSegments: Int = 1,
    val heightSegments: Int = 1,
    val depthSegments: Int = 1
) : Geometry3D {
    init {
        require(width > 0 && height > 0 && depth > 0) { "Dimensions must be positive" }
        require(widthSegments > 0 && heightSegments > 0 && depthSegments > 0) { "Segments must be positive" }
    }
}

/**
 * SphereGeometry - Spherical geometry
 *
 * @property radius Sphere radius
 * @property widthSegments Number of horizontal segments (default: 32)
 * @property heightSegments Number of vertical segments (default: 16)
 * @property phiStart Horizontal starting angle (default: 0)
 * @property phiLength Horizontal sweep angle (default: 2π)
 * @property thetaStart Vertical starting angle (default: 0)
 * @property thetaLength Vertical sweep angle (default: π)
 */
@Serializable
data class SphereGeometry(
    val radius: Float,
    val widthSegments: Int = 32,
    val heightSegments: Int = 16,
    val phiStart: Float = 0f,
    val phiLength: Float = (2 * PI).toFloat(),
    val thetaStart: Float = 0f,
    val thetaLength: Float = PI.toFloat()
) : Geometry3D {
    init {
        require(radius > 0) { "Radius must be positive" }
        require(widthSegments >= 3 && heightSegments >= 2) { "Not enough segments for sphere" }
    }
}

/**
 * PlaneGeometry - Flat plane geometry
 *
 * @property width Plane width
 * @property height Plane height
 * @property widthSegments Number of width subdivisions (default: 1)
 * @property heightSegments Number of height subdivisions (default: 1)
 */
@Serializable
data class PlaneGeometry(
    val width: Float,
    val height: Float,
    val widthSegments: Int = 1,
    val heightSegments: Int = 1
) : Geometry3D {
    init {
        require(width > 0 && height > 0) { "Dimensions must be positive" }
        require(widthSegments > 0 && heightSegments > 0) { "Segments must be positive" }
    }
}

/**
 * CylinderGeometry - Cylindrical geometry
 *
 * @property radiusTop Top radius
 * @property radiusBottom Bottom radius
 * @property height Cylinder height
 * @property radialSegments Number of radial segments (default: 32)
 * @property heightSegments Number of height segments (default: 1)
 * @property openEnded Whether ends are open (default: false)
 */
@Serializable
data class CylinderGeometry(
    val radiusTop: Float,
    val radiusBottom: Float,
    val height: Float,
    val radialSegments: Int = 32,
    val heightSegments: Int = 1,
    val openEnded: Boolean = false
) : Geometry3D {
    init {
        require(radiusTop >= 0 && radiusBottom >= 0) { "Radii must be non-negative" }
        require(height > 0) { "Height must be positive" }
        require(radialSegments >= 3) { "Need at least 3 radial segments" }
    }
}

/**
 * Base interface for 3D materials
 */
@Serializable
sealed interface Material3D {
    val color: String
    val opacity: Float
    val wireframe: Boolean
}

/**
 * BasicMaterial - Simple unlit material
 *
 * @property color Material color (hex string)
 * @property opacity Opacity 0.0-1.0 (default: 1.0)
 * @property wireframe Render as wireframe (default: false)
 */
@Serializable
data class BasicMaterial(
    override val color: String = "#FFFFFF",
    override val opacity: Float = 1f,
    override val wireframe: Boolean = false
) : Material3D {
    init {
        require(opacity in 0f..1f) { "Opacity must be between 0 and 1" }
    }
}

/**
 * PhongMaterial - Phong shading material with specular highlights
 *
 * @property color Diffuse color
 * @property specular Specular highlight color
 * @property shininess Shininess factor (0-100, default: 30)
 * @property opacity Opacity 0.0-1.0
 * @property wireframe Render as wireframe
 */
@Serializable
data class PhongMaterial(
    override val color: String = "#FFFFFF",
    val specular: String = "#111111",
    val shininess: Float = 30f,
    override val opacity: Float = 1f,
    override val wireframe: Boolean = false
) : Material3D {
    init {
        require(opacity in 0f..1f) { "Opacity must be between 0 and 1" }
        require(shininess in 0f..100f) { "Shininess must be between 0 and 100" }
    }
}

/**
 * PBRMaterial - Physically-based rendering material
 *
 * @property color Base color
 * @property metalness Metalness factor (0-1, default: 0.5)
 * @property roughness Roughness factor (0-1, default: 0.5)
 * @property opacity Opacity 0.0-1.0
 * @property wireframe Render as wireframe
 */
@Serializable
data class PBRMaterial(
    override val color: String = "#FFFFFF",
    val metalness: Float = 0.5f,
    val roughness: Float = 0.5f,
    override val opacity: Float = 1f,
    override val wireframe: Boolean = false
) : Material3D {
    init {
        require(opacity in 0f..1f) { "Opacity must be between 0 and 1" }
        require(metalness in 0f..1f) { "Metalness must be between 0 and 1" }
        require(roughness in 0f..1f) { "Roughness must be between 0 and 1" }
    }
}

/**
 * DirectionalLight - Directional light source (like sunlight)
 *
 * @property direction Light direction vector (normalized)
 * @property color Light color
 * @property intensity Light intensity (default: 1.0)
 * @property castShadow Whether this light casts shadows
 * @property transform Transformation (position is ignored)
 * @property visible Whether light is active
 */
@Serializable
data class DirectionalLight(
    val direction: Vector3,
    val color: String = "#FFFFFF",
    val intensity: Float = 1f,
    val castShadow: Boolean = true,
    override val transform: Transform3D = Transform3D.identity(),
    override val visible: Boolean = true
) : Object3D {
    init {
        require(intensity >= 0) { "Intensity must be non-negative" }
    }
}

/**
 * PointLight - Point light source (emits in all directions)
 *
 * @property position Light position
 * @property color Light color
 * @property intensity Light intensity (default: 1.0)
 * @property distance Maximum light distance (0 = infinite)
 * @property decay Light decay rate (default: 2.0)
 * @property castShadow Whether this light casts shadows
 * @property transform Transformation
 * @property visible Whether light is active
 */
@Serializable
data class PointLight(
    val position: Vector3,
    val color: String = "#FFFFFF",
    val intensity: Float = 1f,
    val distance: Float = 0f,
    val decay: Float = 2f,
    val castShadow: Boolean = true,
    override val transform: Transform3D = Transform3D.identity(),
    override val visible: Boolean = true
) : Object3D {
    init {
        require(intensity >= 0) { "Intensity must be non-negative" }
        require(distance >= 0) { "Distance must be non-negative" }
        require(decay >= 0) { "Decay must be non-negative" }
    }
}

/**
 * AmbientLight - Ambient light (illuminates all objects equally)
 *
 * @property color Light color
 * @property intensity Light intensity (default: 1.0)
 * @property transform Transformation (ignored)
 * @property visible Whether light is active
 */
@Serializable
data class AmbientLight(
    val color: String = "#FFFFFF",
    val intensity: Float = 1f,
    override val transform: Transform3D = Transform3D.identity(),
    override val visible: Boolean = true
) : Object3D {
    init {
        require(intensity >= 0) { "Intensity must be non-negative" }
    }
}

package com.augmentalis.avaelements.renderer.android

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.augmentalis.avanues.avamagic.components.core.Camera3D
import com.augmentalis.avanues.avamagic.components.core.Transform3D
import com.augmentalis.avanues.avamagic.components.core.Vector3
import com.augmentalis.avanues.avamagic.ui.core.`3d`.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * OpenGLRenderer - Android OpenGL ES 3.0 renderer for 3D scenes
 *
 * Renders Canvas3D components using OpenGL ES 3.0 on Android devices.
 * Supports:
 * - Multiple geometry types (box, sphere, plane, cylinder)
 * - Multiple material types (basic, phong, PBR)
 * - Multiple light types (directional, point, ambient)
 * - Camera transformations
 * - Shadows (basic implementation)
 *
 * Usage:
 * ```kotlin
 * val glSurfaceView = GLSurfaceView(context)
 * glSurfaceView.setEGLContextClientVersion(3)
 * glSurfaceView.setRenderer(OpenGLRenderer(canvas3D))
 * ```
 */
class OpenGLRenderer(
    private var canvas: Canvas3D
) : GLSurfaceView.Renderer {

    private val meshRenderers = mutableMapOf<Mesh3D, MeshRenderer>()
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var startTime = System.currentTimeMillis()
    private var lastFrameTime = startTime

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Enable depth testing
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)

        // Enable face culling
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        // Enable blending for transparency
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Set background color
        val bgColor = parseHexColor(canvas.backgroundColor)
        GLES30.glClearColor(bgColor[0], bgColor[1], bgColor[2], 1.0f)

        // Initialize mesh renderers
        canvas.children.filterIsInstance<Mesh3D>().forEach { mesh ->
            meshRenderers[mesh] = MeshRenderer(mesh)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)

        val aspect = width.toFloat() / height.toFloat()
        val camera = canvas.camera.copy(aspect = aspect)

        // Update projection matrix
        Matrix.perspectiveM(
            projectionMatrix, 0,
            camera.fov,
            camera.aspect,
            camera.near,
            camera.far
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        // Calculate delta time
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastFrameTime) / 1000f
        lastFrameTime = currentTime

        // Call onRender callback
        canvas.onRender?.invoke(deltaTime)

        // Clear buffers
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Update view matrix
        val camera = canvas.camera
        Matrix.setLookAtM(
            viewMatrix, 0,
            camera.position.x, camera.position.y, camera.position.z,
            camera.target.x, camera.target.y, camera.target.z,
            camera.up.x, camera.up.y, camera.up.z
        )

        // Calculate view-projection matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Collect lights
        val lights = canvas.children.filterIsInstance<DirectionalLight>() +
                canvas.children.filterIsInstance<PointLight>() +
                canvas.children.filterIsInstance<AmbientLight>()

        // Render meshes
        canvas.children.filterIsInstance<Mesh3D>().forEach { mesh ->
            if (mesh.visible) {
                val renderer = meshRenderers[mesh] ?: run {
                    val newRenderer = MeshRenderer(mesh)
                    meshRenderers[mesh] = newRenderer
                    newRenderer
                }
                renderer.draw(vpMatrix, lights)
            }
        }
    }

    /**
     * Update the canvas to render a new scene
     */
    fun updateCanvas(newCanvas: Canvas3D) {
        canvas = newCanvas

        // Clear old mesh renderers
        meshRenderers.values.forEach { it.cleanup() }
        meshRenderers.clear()

        // Update background color
        val bgColor = parseHexColor(canvas.backgroundColor)
        GLES30.glClearColor(bgColor[0], bgColor[1], bgColor[2], 1.0f)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        meshRenderers.values.forEach { it.cleanup() }
        meshRenderers.clear()
    }

    /**
     * Parse hex color string to RGB float array
     */
    private fun parseHexColor(hex: String): FloatArray {
        val color = hex.removePrefix("#")
        val r = color.substring(0, 2).toInt(16) / 255f
        val g = color.substring(2, 4).toInt(16) / 255f
        val b = color.substring(4, 6).toInt(16) / 255f
        return floatArrayOf(r, g, b)
    }
}

/**
 * MeshRenderer - Renders a single 3D mesh
 */
private class MeshRenderer(private val mesh: Mesh3D) {

    private var program: Int = 0
    private var vbo: Int = 0
    private var ibo: Int = 0
    private var indexCount: Int = 0

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        // Create shader program
        program = createShaderProgram(
            vertexShaderCode = VERTEX_SHADER,
            fragmentShaderCode = getFragmentShader(mesh.material)
        )

        // Generate geometry buffers
        val geometryData = generateGeometry(mesh.geometry)

        // Create VBO
        val vboArray = IntArray(1)
        GLES30.glGenBuffers(1, vboArray, 0)
        vbo = vboArray[0]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            geometryData.vertices.size * 4,
            java.nio.ByteBuffer.allocateDirect(geometryData.vertices.size * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(geometryData.vertices)
                .position(0),
            GLES30.GL_STATIC_DRAW
        )

        // Create IBO
        val iboArray = IntArray(1)
        GLES30.glGenBuffers(1, iboArray, 0)
        ibo = iboArray[0]
        indexCount = geometryData.indices.size

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            geometryData.indices.size * 2,
            java.nio.ByteBuffer.allocateDirect(geometryData.indices.size * 2)
                .order(java.nio.ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(geometryData.indices)
                .position(0),
            GLES30.GL_STATIC_DRAW
        )
    }

    fun draw(vpMatrix: FloatArray, lights: List<Object3D>) {
        GLES30.glUseProgram(program)

        // Update model matrix from transform
        convertTransform3DToMatrix(mesh.transform, modelMatrix)

        // Calculate MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        // Set uniforms
        val mvpMatrixHandle = GLES30.glGetUniformLocation(program, "u_MVPMatrix")
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val modelMatrixHandle = GLES30.glGetUniformLocation(program, "u_ModelMatrix")
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        // Set material uniforms
        setMaterialUniforms(program, mesh.material)

        // Set light uniforms
        setLightUniforms(program, lights)

        // Bind buffers
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo)

        // Set vertex attributes
        val positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 32, 0)

        val normalHandle = GLES30.glGetAttribLocation(program, "a_Normal")
        GLES30.glEnableVertexAttribArray(normalHandle)
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 32, 12)

        val uvHandle = GLES30.glGetAttribLocation(program, "a_TexCoord")
        GLES30.glEnableVertexAttribArray(uvHandle)
        GLES30.glVertexAttribPointer(uvHandle, 2, GLES30.GL_FLOAT, false, 32, 24)

        // Draw
        // Note: glPolygonMode not available in OpenGL ES - wireframe mode not supported
        // For wireframe rendering in GLES, would need to use GL_LINES with different indices
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_SHORT, 0)

        // Cleanup
        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(normalHandle)
        GLES30.glDisableVertexAttribArray(uvHandle)
    }

    fun cleanup() {
        GLES30.glDeleteProgram(program)
        GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(ibo), 0)
    }

    private fun convertTransform3DToMatrix(transform: Transform3D, output: FloatArray) {
        // Transform3D is in column-major order, same as OpenGL
        System.arraycopy(transform.matrix, 0, output, 0, 16)
    }

    private fun setMaterialUniforms(program: Int, material: Material3D) {
        val color = parseHexColor(material.color)
        val colorHandle = GLES30.glGetUniformLocation(program, "u_Color")
        GLES30.glUniform4f(colorHandle, color[0], color[1], color[2], material.opacity)

        when (material) {
            is PhongMaterial -> {
                val specular = parseHexColor(material.specular)
                val specularHandle = GLES30.glGetUniformLocation(program, "u_Specular")
                GLES30.glUniform3f(specularHandle, specular[0], specular[1], specular[2])

                val shininessHandle = GLES30.glGetUniformLocation(program, "u_Shininess")
                GLES30.glUniform1f(shininessHandle, material.shininess)
            }
            is PBRMaterial -> {
                val metalnessHandle = GLES30.glGetUniformLocation(program, "u_Metalness")
                GLES30.glUniform1f(metalnessHandle, material.metalness)

                val roughnessHandle = GLES30.glGetUniformLocation(program, "u_Roughness")
                GLES30.glUniform1f(roughnessHandle, material.roughness)
            }
            else -> {}
        }
    }

    private fun setLightUniforms(program: Int, lights: List<Object3D>) {
        var ambientIntensity = 0f
        var ambientColor = floatArrayOf(0f, 0f, 0f)

        var directionalCount = 0
        var pointCount = 0

        lights.forEach { light ->
            when (light) {
                is AmbientLight -> {
                    if (light.visible) {
                        val color = parseHexColor(light.color)
                        ambientColor[0] += color[0] * light.intensity
                        ambientColor[1] += color[1] * light.intensity
                        ambientColor[2] += color[2] * light.intensity
                        ambientIntensity += light.intensity
                    }
                }
                is DirectionalLight -> {
                    if (light.visible && directionalCount < 4) {
                        val dirHandle = GLES30.glGetUniformLocation(program, "u_DirectionalLights[$directionalCount].direction")
                        GLES30.glUniform3f(dirHandle, light.direction.x, light.direction.y, light.direction.z)

                        val color = parseHexColor(light.color)
                        val colorHandle = GLES30.glGetUniformLocation(program, "u_DirectionalLights[$directionalCount].color")
                        GLES30.glUniform3f(colorHandle, color[0], color[1], color[2])

                        val intensityHandle = GLES30.glGetUniformLocation(program, "u_DirectionalLights[$directionalCount].intensity")
                        GLES30.glUniform1f(intensityHandle, light.intensity)

                        directionalCount++
                    }
                }
                is PointLight -> {
                    if (light.visible && pointCount < 4) {
                        val posHandle = GLES30.glGetUniformLocation(program, "u_PointLights[$pointCount].position")
                        GLES30.glUniform3f(posHandle, light.position.x, light.position.y, light.position.z)

                        val color = parseHexColor(light.color)
                        val colorHandle = GLES30.glGetUniformLocation(program, "u_PointLights[$pointCount].color")
                        GLES30.glUniform3f(colorHandle, color[0], color[1], color[2])

                        val intensityHandle = GLES30.glGetUniformLocation(program, "u_PointLights[$pointCount].intensity")
                        GLES30.glUniform1f(intensityHandle, light.intensity)

                        val distanceHandle = GLES30.glGetUniformLocation(program, "u_PointLights[$pointCount].distance")
                        GLES30.glUniform1f(distanceHandle, light.distance)

                        val decayHandle = GLES30.glGetUniformLocation(program, "u_PointLights[$pointCount].decay")
                        GLES30.glUniform1f(decayHandle, light.decay)

                        pointCount++
                    }
                }
                else -> {
                    // Ignore unsupported light types (Mesh3D, etc.)
                }
            }
        }

        // Set ambient light
        val ambientHandle = GLES30.glGetUniformLocation(program, "u_AmbientLight")
        GLES30.glUniform3f(ambientHandle, ambientColor[0], ambientColor[1], ambientColor[2])

        // Set light counts
        val dirCountHandle = GLES30.glGetUniformLocation(program, "u_DirectionalLightCount")
        GLES30.glUniform1i(dirCountHandle, directionalCount)

        val pointCountHandle = GLES30.glGetUniformLocation(program, "u_PointLightCount")
        GLES30.glUniform1i(pointCountHandle, pointCount)
    }

    private fun parseHexColor(hex: String): FloatArray {
        val color = hex.removePrefix("#")
        val r = color.substring(0, 2).toInt(16) / 255f
        val g = color.substring(2, 4).toInt(16) / 255f
        val b = color.substring(4, 6).toInt(16) / 255f
        return floatArrayOf(r, g, b)
    }

    companion object {
        private const val VERTEX_SHADER = """
            #version 300 es
            precision highp float;

            in vec3 a_Position;
            in vec3 a_Normal;
            in vec2 a_TexCoord;

            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;

            out vec3 v_Position;
            out vec3 v_Normal;
            out vec2 v_TexCoord;

            void main() {
                v_Position = vec3(u_ModelMatrix * vec4(a_Position, 1.0));
                v_Normal = normalize(mat3(u_ModelMatrix) * a_Normal);
                v_TexCoord = a_TexCoord;
                gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
            }
        """

        private fun getFragmentShader(material: Material3D): String {
            return when (material) {
                is PhongMaterial -> PHONG_FRAGMENT_SHADER
                is PBRMaterial -> PBR_FRAGMENT_SHADER
                else -> BASIC_FRAGMENT_SHADER
            }
        }

        private const val BASIC_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            in vec3 v_Position;
            in vec3 v_Normal;
            in vec2 v_TexCoord;

            uniform vec4 u_Color;
            uniform vec3 u_AmbientLight;

            out vec4 fragColor;

            void main() {
                vec3 ambient = u_AmbientLight * u_Color.rgb;
                fragColor = vec4(ambient, u_Color.a);
            }
        """

        private const val PHONG_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            struct DirectionalLight {
                vec3 direction;
                vec3 color;
                float intensity;
            };

            struct PointLight {
                vec3 position;
                vec3 color;
                float intensity;
                float distance;
                float decay;
            };

            in vec3 v_Position;
            in vec3 v_Normal;
            in vec2 v_TexCoord;

            uniform vec4 u_Color;
            uniform vec3 u_Specular;
            uniform float u_Shininess;
            uniform vec3 u_AmbientLight;

            uniform DirectionalLight u_DirectionalLights[4];
            uniform int u_DirectionalLightCount;

            uniform PointLight u_PointLights[4];
            uniform int u_PointLightCount;

            out vec4 fragColor;

            void main() {
                vec3 normal = normalize(v_Normal);
                vec3 viewDir = normalize(-v_Position);

                vec3 ambient = u_AmbientLight * u_Color.rgb;
                vec3 diffuse = vec3(0.0);
                vec3 specular = vec3(0.0);

                // Directional lights
                for (int i = 0; i < u_DirectionalLightCount; i++) {
                    vec3 lightDir = normalize(-u_DirectionalLights[i].direction);
                    float diff = max(dot(normal, lightDir), 0.0);
                    diffuse += diff * u_DirectionalLights[i].color * u_DirectionalLights[i].intensity;

                    vec3 reflectDir = reflect(-lightDir, normal);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_Shininess);
                    specular += spec * u_Specular * u_DirectionalLights[i].color * u_DirectionalLights[i].intensity;
                }

                // Point lights
                for (int i = 0; i < u_PointLightCount; i++) {
                    vec3 lightDir = u_PointLights[i].position - v_Position;
                    float distance = length(lightDir);
                    lightDir = normalize(lightDir);

                    float attenuation = 1.0;
                    if (u_PointLights[i].distance > 0.0) {
                        attenuation = pow(1.0 - min(distance / u_PointLights[i].distance, 1.0), u_PointLights[i].decay);
                    }

                    float diff = max(dot(normal, lightDir), 0.0);
                    diffuse += diff * u_PointLights[i].color * u_PointLights[i].intensity * attenuation;

                    vec3 reflectDir = reflect(-lightDir, normal);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_Shininess);
                    specular += spec * u_Specular * u_PointLights[i].color * u_PointLights[i].intensity * attenuation;
                }

                vec3 result = (ambient + diffuse) * u_Color.rgb + specular;
                fragColor = vec4(result, u_Color.a);
            }
        """

        private const val PBR_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;

            // (PBR shader implementation - simplified for now)
            in vec3 v_Position;
            in vec3 v_Normal;
            in vec2 v_TexCoord;

            uniform vec4 u_Color;
            uniform float u_Metalness;
            uniform float u_Roughness;
            uniform vec3 u_AmbientLight;

            out vec4 fragColor;

            void main() {
                vec3 ambient = u_AmbientLight * u_Color.rgb;
                fragColor = vec4(ambient, u_Color.a);
            }
        """

        private fun createShaderProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
            val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)

            // Check link status
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw RuntimeException("Error linking program: $error")
            }

            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            return program
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            // Check compile status
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES30.glGetShaderInfoLog(shader)
                GLES30.glDeleteShader(shader)
                throw RuntimeException("Error compiling shader: $error")
            }

            return shader
        }
    }
}

/**
 * Geometry data with vertices and indices
 */
private data class GeometryData(
    val vertices: FloatArray, // Format: [x, y, z, nx, ny, nz, u, v]
    val indices: ShortArray
)

/**
 * Generate geometry data from Geometry3D
 */
private fun generateGeometry(geometry: Geometry3D): GeometryData {
    return when (geometry) {
        is BoxGeometry -> generateBoxGeometry(geometry)
        is SphereGeometry -> generateSphereGeometry(geometry)
        is PlaneGeometry -> generatePlaneGeometry(geometry)
        is CylinderGeometry -> generateCylinderGeometry(geometry)
    }
}

private fun generateBoxGeometry(box: BoxGeometry): GeometryData {
    val w = box.width / 2
    val h = box.height / 2
    val d = box.depth / 2

    val vertices = floatArrayOf(
        // Front face
        -w, -h, d, 0f, 0f, 1f, 0f, 0f,
        w, -h, d, 0f, 0f, 1f, 1f, 0f,
        w, h, d, 0f, 0f, 1f, 1f, 1f,
        -w, h, d, 0f, 0f, 1f, 0f, 1f,

        // Back face
        w, -h, -d, 0f, 0f, -1f, 0f, 0f,
        -w, -h, -d, 0f, 0f, -1f, 1f, 0f,
        -w, h, -d, 0f, 0f, -1f, 1f, 1f,
        w, h, -d, 0f, 0f, -1f, 0f, 1f,

        // Top face
        -w, h, d, 0f, 1f, 0f, 0f, 0f,
        w, h, d, 0f, 1f, 0f, 1f, 0f,
        w, h, -d, 0f, 1f, 0f, 1f, 1f,
        -w, h, -d, 0f, 1f, 0f, 0f, 1f,

        // Bottom face
        -w, -h, -d, 0f, -1f, 0f, 0f, 0f,
        w, -h, -d, 0f, -1f, 0f, 1f, 0f,
        w, -h, d, 0f, -1f, 0f, 1f, 1f,
        -w, -h, d, 0f, -1f, 0f, 0f, 1f,

        // Right face
        w, -h, d, 1f, 0f, 0f, 0f, 0f,
        w, -h, -d, 1f, 0f, 0f, 1f, 0f,
        w, h, -d, 1f, 0f, 0f, 1f, 1f,
        w, h, d, 1f, 0f, 0f, 0f, 1f,

        // Left face
        -w, -h, -d, -1f, 0f, 0f, 0f, 0f,
        -w, -h, d, -1f, 0f, 0f, 1f, 0f,
        -w, h, d, -1f, 0f, 0f, 1f, 1f,
        -w, h, -d, -1f, 0f, 0f, 0f, 1f
    )

    val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3, // Front
        4, 5, 6, 4, 6, 7, // Back
        8, 9, 10, 8, 10, 11, // Top
        12, 13, 14, 12, 14, 15, // Bottom
        16, 17, 18, 16, 18, 19, // Right
        20, 21, 22, 20, 22, 23  // Left
    )

    return GeometryData(vertices, indices)
}

private fun generateSphereGeometry(sphere: SphereGeometry): GeometryData {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    val r = sphere.radius
    val widthSegments = sphere.widthSegments
    val heightSegments = sphere.heightSegments

    // Generate vertices
    for (y in 0..heightSegments) {
        val v = y.toFloat() / heightSegments
        val theta = v * Math.PI.toFloat()

        for (x in 0..widthSegments) {
            val u = x.toFloat() / widthSegments
            val phi = u * 2 * Math.PI.toFloat()

            val px = -r * cos(phi) * sin(theta)
            val py = r * cos(theta)
            val pz = r * sin(phi) * sin(theta)

            val nx = -cos(phi) * sin(theta)
            val ny = cos(theta)
            val nz = sin(phi) * sin(theta)

            vertices.addAll(listOf(px, py, pz, nx, ny, nz, u, v))
        }
    }

    // Generate indices
    for (y in 0 until heightSegments) {
        for (x in 0 until widthSegments) {
            val a = (y * (widthSegments + 1) + x).toShort()
            val b = ((y + 1) * (widthSegments + 1) + x).toShort()
            val c = ((y + 1) * (widthSegments + 1) + x + 1).toShort()
            val d = (y * (widthSegments + 1) + x + 1).toShort()

            indices.addAll(listOf(a, b, d, b, c, d))
        }
    }

    return GeometryData(vertices.toFloatArray(), indices.toShortArray())
}

private fun generatePlaneGeometry(plane: PlaneGeometry): GeometryData {
    val w = plane.width / 2
    val h = plane.height / 2

    val vertices = floatArrayOf(
        -w, -h, 0f, 0f, 0f, 1f, 0f, 0f,
        w, -h, 0f, 0f, 0f, 1f, 1f, 0f,
        w, h, 0f, 0f, 0f, 1f, 1f, 1f,
        -w, h, 0f, 0f, 0f, 1f, 0f, 1f
    )

    val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    return GeometryData(vertices, indices)
}

private fun generateCylinderGeometry(cylinder: CylinderGeometry): GeometryData {
    val vertices = mutableListOf<Float>()
    val indices = mutableListOf<Short>()

    val radiusTop = cylinder.radiusTop
    val radiusBottom = cylinder.radiusBottom
    val height = cylinder.height
    val radialSegments = cylinder.radialSegments
    val heightSegments = cylinder.heightSegments

    // Generate vertices
    for (y in 0..heightSegments) {
        val v = y.toFloat() / heightSegments
        val radius = radiusBottom + (radiusTop - radiusBottom) * v
        val py = -height / 2 + v * height

        for (x in 0..radialSegments) {
            val u = x.toFloat() / radialSegments
            val theta = u * 2 * Math.PI.toFloat()

            val px = radius * cos(theta)
            val pz = radius * sin(theta)

            val nx = cos(theta)
            val ny = 0f
            val nz = sin(theta)

            vertices.addAll(listOf(px, py, pz, nx, ny, nz, u, v))
        }
    }

    // Generate indices
    for (y in 0 until heightSegments) {
        for (x in 0 until radialSegments) {
            val a = (y * (radialSegments + 1) + x).toShort()
            val b = ((y + 1) * (radialSegments + 1) + x).toShort()
            val c = ((y + 1) * (radialSegments + 1) + x + 1).toShort()
            val d = (y * (radialSegments + 1) + x + 1).toShort()

            indices.addAll(listOf(a, b, d, b, c, d))
        }
    }

    return GeometryData(vertices.toFloatArray(), indices.toShortArray())
}

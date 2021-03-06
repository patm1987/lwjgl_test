package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.NativeResource
import kotlin.properties.Delegates

/**
 * TODO: This _should_ be merged with [ShaderProgram], pay attention to feature overlaps!
 */
class OutlineShader(
        val vertexShaderSource: String,
        val geometryShaderSource: String,
        val fragmentShaderSource: String) : NativeResource {
    val programId: Int = createProgram()
    private val vertexShader: Int = createShader(vertexShaderSource, GL_VERTEX_SHADER)
    private val geometryShader: Int = createShader(geometryShaderSource, GL_GEOMETRY_SHADER)
    private val fragmentShader: Int = createShader(fragmentShaderSource, GL_FRAGMENT_SHADER)

    private val positionAttribute: Int
    private val viewProjectionUniform: Int
    private val modelUniform: Int
    private val edgeThicknessUniform: Int

    var camera: ICamera? = null

    /**
     * When a call to [use] is made, the callback acts on this object. I make it private here so that you may only
     * attempt to render a model after this shader program has been properly made current
     */
    private val activeShader = ActiveShader()

    init {
        link()

        glUseProgram(programId)
        positionAttribute = getAttributeLocation(positionName)
        viewProjectionUniform = getUniformLocation(viewProjectionMatrixName)
        modelUniform = getUniformLocation(modelMatrixName)
        edgeThicknessUniform = getUniformLocation(edgeThicknessName)
        glUseProgram(0)
    }

    var edgeThickness: Float by Delegates.observable(0f) { _, _, new ->
        glUseProgram(programId)
        glUniform1f(edgeThicknessUniform, new)
        glUseProgram(0)
    }

    private fun link() {
        glLinkProgram(programId)

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            print("Error linking shader code: ${glGetProgramInfoLog(programId)}")
            error { "Failed to link program" }
        }

        if (vertexShader != 0) {
            glDetachShader(programId, vertexShader)
        }
        if (geometryShader != 0) {
            glDetachShader(programId, geometryShader)
        }
        if (fragmentShader != 0) {
            glDetachShader(programId, fragmentShader)
        }

        glValidateProgram(programId)
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            ShaderProgram.logger.warn { "Warning whilst validating shader: ${glGetProgramInfoLog(programId)}" }
        }
    }

    private fun createProgram(): Int {
        val program = GL20.glCreateProgram()
        if (program == 0) {
            error { "Failed to create program" }
        }
        return program
    }

    private fun createShader(source: String, shaderType: Int): Int {
        val shaderId = GL20.glCreateShader(shaderType)
        if (shaderId == 0) {
            error {
                "Failed to compile shader type: ${
                when (shaderType) {
                    GL_VERTEX_SHADER -> "Vertex Shader"
                    GL_GEOMETRY_SHADER -> "Geometry Shader"
                    GL_FRAGMENT_SHADER -> "Fragment Shader"
                    else -> "Unknown shader $shaderType"
                }
                }!"
            }
        }

        GL20.glShaderSource(shaderId, source)
        GL20.glCompileShader(shaderId)

        if (GL20.glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            print("Failed to compile shader because: ${GL20.glGetShaderInfoLog(shaderId)}")
            error { "Failed to compile shader" }
        }

        GL20.glAttachShader(programId, shaderId)

        return shaderId
    }

    private fun getAttributeLocation(attributeName: String): Int {
        return glGetAttribLocation(programId, attributeName)
    }

    private fun getUniformLocation(uniformName: String): Int {
        return glGetUniformLocation(programId, uniformName)
    }

    override fun free() {
        glDeleteProgram(programId)
        glDeleteShader(vertexShader)
        glDeleteShader(geometryShader)
        glDeleteShader(fragmentShader)
    }


    fun use(callback: OutlineShader.ActiveShader.() -> Unit) {
        val camera = camera
        if (camera == null) {
            ShaderProgram.logger.warn { "There is no active camera, we will skip this render" }
            return
        }

        glUseProgram(programId)
        glEnableVertexAttribArray(positionAttribute)

        camera.loadUniform(viewProjectionUniform)

        activeShader.callback()
        glDisableVertexAttribArray(positionAttribute)
        glUseProgram(0)
    }


    /**
     * Class used in conjunction with [use] to allow some caller to render a model using this shader.
     */
    inner class ActiveShader {
        fun renderModel(model: HalfEdgeModel) {
            model.useAdjacency {
                MemoryStack.stackPush().use {
                    val nativeMatrix = it.mallocFloat(16)
                    val modelMatrix = Matrix4f()
                    model.transform.getWorldMatrix(modelMatrix)
                    modelMatrix.get(nativeMatrix)

                    GL20.glUniformMatrix4fv(modelUniform, false, nativeMatrix)
                }

                loadPositions(positionAttribute)
                drawElementsAdjacency()
            }
        }
    }

    companion object {
        val positionName = "position"
        val viewProjectionMatrixName = "ViewProjectionMatrix"
        val modelMatrixName = "ModelMatrix"
        val edgeThicknessName = "EdgeThickness"
    }
}

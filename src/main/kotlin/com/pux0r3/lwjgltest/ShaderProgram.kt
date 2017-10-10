package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.NativeResource

@DslMarker
annotation class ShaderTagMarker

/**
 * Use this to create a shader as a sort of DSL
 */
fun shader(cb: ShaderProgram.Builder.() -> Unit): ShaderProgram {
    val builder = ShaderProgram.Builder()
    builder.cb()
    return builder.build()
}

/**
 * Represents a shader that we will use to render
 * @param vertexSource the full sourcecode of the vertex shader, *NOT A FILENAME*
 * @param fragmentSource the full sourcecode of the fragment shader, *NOT A FILENAME*
 * @param attributeNames the names of all this shader's attributes
 * @param uniformNames the names of all this shader's uniforms
 */
class ShaderProgram private constructor(
        vertexSource: String,
        fragmentSource: String,
        attributeNames: Attributes,
        uniformNames: Uniforms) : NativeResource {
    companion object : KLogging()

    // TODO: I want to maintain all of these states internally rather than setting them externally
    val programId: Int = createProgram()
    private val vertexShader: Int = createShader(vertexSource, GL_VERTEX_SHADER)
    private val fragmentShader: Int = createShader(fragmentSource, GL_FRAGMENT_SHADER)

    val positionAttribute: Int
    val normalAttribute: Int
    val viewProjectionUniform: Int
    val modelUniform: Int

    val worldAmbientColorUniform: Int
    val worldLightDirectionUniform: Int
    val worldLightColorUniform: Int
    val modelAmbientColorUniform: Int

    var lightDirection: Vector3f = Vector3f(.5f, .5f, .5f).normalize()
    var ambientColor: Vector4f = Vector4f(.5f, .5f, .5f, 1f)
    var lightColor: Vector4f = Vector4f(.5f, .5f, .5f, 1f)

    var camera: ICamera? = null

    /**
     * When a call to [use] is made, the callback acts on this object. I make it private here so that you may only
     * attempt to render a model after this shader program has been properly made current
     */
    private val activeShader = ActiveShader()

    init {
        link()

        glUseProgram(programId)
        positionAttribute = getAttributeLocation(attributeNames.position)
        normalAttribute = getAttributeLocation(attributeNames.normal)
        viewProjectionUniform = getUniformLocation(uniformNames.viewProjectionMatrix)
        modelUniform = getUniformLocation(uniformNames.modelMatrix)

        worldAmbientColorUniform = getUniformLocation(uniformNames.worldAmbientColor)
        worldLightDirectionUniform = getUniformLocation(uniformNames.worldLightDirection)
        worldLightColorUniform = getUniformLocation(uniformNames.worldLightColor)
        modelAmbientColorUniform = getUniformLocation(uniformNames.modelAmbientColor)
        glUseProgram(0)
    }

    override fun free() {
        glDeleteProgram(programId)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    fun use(callback: ActiveShader.() -> Unit) {
        val camera = camera
        if (camera == null) {
            logger.warn { "There is no active camera, we will skip this render" }
            return
        }

        glUseProgram(programId)
        glEnableVertexAttribArray(positionAttribute)
        glEnableVertexAttribArray(normalAttribute)

        camera.loadUniform(viewProjectionUniform)
        MemoryStack.stackPush().use {
            val lightDirectionBuffer = it.mallocFloat(3)
            lightDirection.get(lightDirectionBuffer)
            glUniform3fv(worldLightDirectionUniform, lightDirectionBuffer)

            val ambientColorBuffer = it.mallocFloat(4)
            ambientColor.get(ambientColorBuffer)
            glUniform4fv(worldAmbientColorUniform, ambientColorBuffer)

            val lightColorBuffer = it.mallocFloat(4)
            lightColor.get(lightColorBuffer)
            glUniform4fv(worldLightColorUniform, lightColorBuffer)
        }

        activeShader.callback()
        glDisableVertexAttribArray(positionAttribute)
        glDisableVertexAttribArray(normalAttribute)
        glUseProgram(0)
    }

    private fun createProgram(): Int {
        val program = glCreateProgram()
        if (program == 0) {
            error { "Failed to create program" }
        }
        return program
    }

    private fun createShader(source: String, shaderType: Int): Int {
        val shaderId = glCreateShader(shaderType)
        if (shaderId == 0) {
            error {
                "Failed to compile shader type: ${
                when (shaderType) {
                    GL_VERTEX_SHADER -> "Vertex Shader"
                    GL_FRAGMENT_SHADER -> "Fragment Shader"
                    else -> "Unknown shader $shaderType"
                }
                }!"
            }
        }

        glShaderSource(shaderId, source)
        glCompileShader(shaderId)

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            print("Failed to compile shader because: ${glGetShaderInfoLog(shaderId)}")
            error { "Failed to compile shader" }
        }

        glAttachShader(programId, shaderId)

        return shaderId
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
        if (fragmentShader != 0) {
            glDetachShader(programId, fragmentShader)
        }

        glValidateProgram(programId)
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            logger.warn { "Warning whilst validating shader: ${glGetProgramInfoLog(programId)}" }
        }
    }

    private fun getAttributeLocation(attributeName: String): Int {
        return glGetAttribLocation(programId, attributeName)
    }

    private fun getUniformLocation(uniformName: String): Int {
        return glGetUniformLocation(programId, uniformName)
    }

    /**
     * Class used in conjunction with [use] to allow some caller to render a model using this shader.
     */
    inner class ActiveShader {
        // TODO: either get rid of SimpleModel or create an interface to encapsulate everything happening here

        fun renderModel(model: SimpleModel, material: Material) {
            glUniform4f(modelAmbientColorUniform, material.ambient.x, material.ambient.y, material.ambient.z, 1f)

            model.use {
                MemoryStack.stackPush().use {
                    val nativeMatrix = it.mallocFloat(16)
                    val modelMatrix = Matrix4f()
                    model.transform.getWorldMatrix(modelMatrix)
                    modelMatrix.get(nativeMatrix)

                    GL20.glUniformMatrix4fv(modelUniform, false, nativeMatrix)
                }

                loadPositions(positionAttribute)
                loadNormals(normalAttribute)
                drawElements()
            }
        }

        fun renderModel(model: HalfEdgeModel, material: Material) {
            glUniform4f(modelAmbientColorUniform, material.ambient.x, material.ambient.y, material.ambient.z, 1f)

            model.use {
                MemoryStack.stackPush().use {
                    val nativeMatrix = it.mallocFloat(16)
                    val modelMatrix = Matrix4f()
                    model.transform.getWorldMatrix(modelMatrix)
                    modelMatrix.get(nativeMatrix)

                    GL20.glUniformMatrix4fv(modelUniform, false, nativeMatrix)
                }

                loadPositions(positionAttribute)
                loadNormals(normalAttribute)
                drawElements()
            }
        }
    }

    @ShaderTagMarker
    class Builder {
        /**
         * For clarity: this is the _FILENAME_ of the vertex shader, not the actual source
         */
        var vertexSource: String = ""

        /**
         * For clarity: this is the _FILENAME_ of the fragment shader, not the actual source
         */
        var fragmentSource: String = ""
        private val attributes: Attributes = Attributes()
        private val uniforms: Uniforms = Uniforms()

        fun attributes(cb: Attributes.() -> Unit) {
            this.attributes.cb()
        }

        fun uniforms(cb: Uniforms.() -> Unit) {
            this.uniforms.cb()
        }

        fun build(): ShaderProgram {
            return ShaderProgram(
                    Resources.loadAssetAsString(vertexSource),
                    Resources.loadAssetAsString(fragmentSource),
                    attributes,
                    uniforms)
        }
    }

    /**
     * This defines the names of all the attributes this [ShaderProgram] will care about in the actual shader source so
     * I can load them correctly
     */
    @ShaderTagMarker
    class Attributes {
        var position: String = ""
        var normal: String = ""
    }

    /**
     * This defines the names of all the uniforms this [ShaderProgram] will care about in the shader source so I can
     * load them correctly
     */
    @ShaderTagMarker
    class Uniforms {
        var viewProjectionMatrix: String = ""
        var modelMatrix: String = ""
        var worldAmbientColor: String = ""
        var worldLightDirection: String = ""
        var worldLightColor: String = ""
        var modelAmbientColor: String = ""
    }
}
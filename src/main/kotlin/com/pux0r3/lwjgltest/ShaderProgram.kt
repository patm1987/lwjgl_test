package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.NativeResource

/**
 * Created by pux19 on 5/20/2017.
 */
class ShaderProgram(vertexSource: String, fragmentSource: String, val camera: ICamera) : NativeResource {
    companion object : KLogging()

    // TODO: I want to maintain all of these states internally rather than setting them externally
    val programId: Int = createProgram()
    private val vertexShader: Int = createShader(vertexSource, GL_VERTEX_SHADER)
    private val fragmentShader: Int = createShader(fragmentSource, GL_FRAGMENT_SHADER)

    val positionAttribute: Int
    val normalAttribute: Int
    val modelViewUniform: Int

    val worldAmbientColorUniform: Int
    val worldLightDirectionUniform: Int
    val worldLightColorUniform: Int

    var lightDirection: Vector3f = Vector3f(.5f, .5f, .5f).normalize()
    var ambientColor: Vector4f = Vector4f(.5f, .5f, .5f, 1f)
    var lightColor: Vector4f = Vector4f(.5f, .5f, .5f, 1f)

    init {
        link()

        glUseProgram(programId)
        positionAttribute = getAttributeLocation("position")
        normalAttribute = getAttributeLocation("normal")
        modelViewUniform = getUniformLocation("ModelViewMatrix")

        worldAmbientColorUniform = getUniformLocation("worldAmbient")
        worldLightDirectionUniform = getUniformLocation("worldLightDirection")
        worldLightColorUniform = getUniformLocation("worldLightColor")
        glUseProgram(0)
    }

    override fun free() {
        glDeleteProgram(programId)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    inline fun use(callback: () -> Unit) {
        glUseProgram(programId)
        glEnableVertexAttribArray(positionAttribute)
        glEnableVertexAttribArray(normalAttribute)

        // TODO: I'm actually going to want to make the MVP matrix. So I'll want to change this
        camera.loadUniform(modelViewUniform)
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

        callback()
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
}
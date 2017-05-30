package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack.stackPush

/**
 * Created by pux19 on 5/20/2017.
 */
class ShaderProgram(vertexSource: String, fragmentSource: String) {
    companion object : KLogging()

    val programId: Int = createProgram()
    val vertexShader: Int = createShader(vertexSource, GL_VERTEX_SHADER)
    val fragmentShader: Int = createShader(fragmentSource, GL_FRAGMENT_SHADER)

    init {
        link()
    }

    val positionAttribute: Int = getAttributeLocation("position")
    val modelViewUniform: Int = getUniformLocation("ModelViewMatrix")

    fun free() {
        glDeleteProgram(programId)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    inline fun use(callback: () -> Unit) {
        glUseProgram(programId)
        glEnableVertexAttribArray(positionAttribute)

        // TODO: this is the wrong place for this! Use a Uniform Buffer Object
        stackPush().use {
            // test matrix
            val matrixBuffer = it.mallocFloat(16)

            // l, r, b, t, n, f
            Matrix4f().ortho(-1f, 1f, -1f, 1f, -1f, 1f).get(matrixBuffer)

            // load test matrix
            glUniformMatrix4fv(modelViewUniform, false, matrixBuffer)
        }
        callback()
        glDisableVertexAttribArray(positionAttribute)
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
package com.pux0r3.lwjgltest

import mu.KLogging
import org.lwjgl.opengl.GL20.*

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

    fun free() {
        unbind()
        glDeleteProgram(programId)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    fun bind() {}
    fun unbind() {}

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
            error { "Failed to compile shader because: ${glGetShaderInfoLog(shaderId)}" }
        }

        glAttachShader(programId, shaderId)
        return shaderId
    }

    private fun link() {
        glLinkProgram(programId)

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            error {"Error linking shader code: ${glGetProgramInfoLog(programId)}"}
        }

        if (vertexShader != 0) {
            glDetachShader(programId, vertexShader)
        }
        if (fragmentShader != 0) {
            glDetachShader(programId, fragmentShader)
        }

        glValidateProgram(programId)
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            logger.warn{"Warning whilst validating shader: ${glGetProgramInfoLog(programId)}"}
        }
    }
}
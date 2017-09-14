package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20
import java.nio.FloatBuffer
import org.lwjgl.system.MemoryStack.stackGet
import org.lwjgl.system.NativeResource

class OrthographicCamera(private val orthoHeight: Float, private val near: Float = -1f, private val far: Float = 1f): NativeResource {
    // TODO: I think this allocates on the stack, I need to find a heapGet or something
    val nativeMatrix: FloatBuffer = stackGet().mallocFloat(16)

    fun setResolution(width: Int, height: Int) {
        val orthoWidth = width * orthoHeight / height
        Matrix4f().ortho(
                -orthoWidth,
                orthoWidth,
                -orthoHeight,
                orthoHeight,
                near,
                far).get(nativeMatrix)
    }

    fun loadUniform(uniformId: Int) {
        GL20.glUniformMatrix4fv(uniformId, false, nativeMatrix)
    }

    override fun free() {
        // TODO: fix
    }
}

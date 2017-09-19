package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.NativeResource

class OrthographicCamera(
        private val orthoHeight: Float,
        private val near: Float = -1f,
        private val far: Float = 1f,
        var position: Vector3f = Vector3f()): ICamera {
    private var projectionMatrix = Matrix4f()

    override fun setResolution(width: Int, height: Int) {
        val orthoWidth = width * orthoHeight / height
        projectionMatrix = Matrix4f().ortho(
                -orthoWidth,
                orthoWidth,
                -orthoHeight,
                orthoHeight,
                near,
                far)
    }

    override fun loadUniform(uniformId: Int) {
        // TODO: only recalculate when changed
        val inversePosition = Vector3f(position)
        inversePosition.negate()
        val viewProjection = Matrix4f().set(projectionMatrix).translate(inversePosition)
        stackPush().use {
            val nativeMatrix = it.mallocFloat(16)
            viewProjection.get(nativeMatrix)

            GL20.glUniformMatrix4fv(uniformId, false, nativeMatrix)
        }
    }
}

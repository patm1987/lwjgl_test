package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack

// TODO: when I make a scenegraph, lookat should act on the node generically
class LookAtPerspectiveCamera(
        fov: Float,
        aspect: Float,
        near: Float = .01f,
        far: Float = 100f,
        position: Vector3f = Vector3f(),
        target: Vector3f = Vector3f()) : ICamera {
    private var perspectiveDirty = true
    private var _fov: Float = fov
        set(value) {
            perspectiveDirty = true
            field = value
        }
    private var _aspect: Float = aspect
        set(value) {
            perspectiveDirty = true
            field = value
        }
    private var _near: Float = near
        set(value) {
            perspectiveDirty = true
            field = value
        }
    private var _far: Float = far
        set(value) {
            perspectiveDirty = true
            field = value
        }
    private var perspectiveMatrix = Matrix4f()
    private var viewMatrix = Matrix4f()
    private var viewDirty = true
    private var _position: Vector3f = position
    private var _target: Vector3f = target
    private val upVector = Vector3f(0f, 1f, 0f)

    private var viewProjectionMatrix = Matrix4f()

    override fun setResolution(width: Int, height: Int) {
        _aspect = width.toFloat() / height.toFloat()
    }

    override fun loadUniform(uniformId: Int) {
        var updateVPMatrix = false
        if (perspectiveDirty) {
            updatePerspectiveMatrix()
            updateVPMatrix = true
        }
        if (viewDirty) {
            updateViewMatrix()
            updateVPMatrix = true
        }
        if (updateVPMatrix) {
            updateViewProjectionMatrix()
        }
        MemoryStack.stackPush().use {
            val nativeMatrix = it.mallocFloat(16)
            viewProjectionMatrix.get(nativeMatrix)

            GL20.glUniformMatrix4fv(uniformId, false, nativeMatrix)
        }
    }

    private fun updatePerspectiveMatrix() {
        perspectiveMatrix.setPerspective(_fov, _aspect, _near, _far)
        perspectiveDirty = false
    }

    private fun updateViewMatrix() {
        viewMatrix.setLookAt(_position, _target, upVector)
        viewDirty = false
    }

    private fun updateViewProjectionMatrix() {
        viewProjectionMatrix.set(perspectiveMatrix).mul(viewMatrix)
    }

    /**
     * Sets the position of this camera to the given position
     * note that this value is copied in
     * @param position our new position
     */
    fun setPosition(position: Vector3f) {
        _position.set(position)
        viewDirty = true
    }

    /**
     * Retrieves the position of this camera
     * @param outPosition vector that will hold this camera's position
     */
    fun getPosition(outPosition: Vector3f) {
        outPosition.set(_position)
    }

    /**
     * Sets the target of this camera
     * note that this value will be copied in
     * @param target the new target of the camera
     */
    fun setTarget(target: Vector3f) {
        _target = target
        viewDirty = true
    }

    /**
     * retrieves the target of this camera
     * @param outTarget the value that will hold this camera's target
     */
    fun getTarget(outTarget: Vector3f) {
        outTarget.set(_target)
    }
}
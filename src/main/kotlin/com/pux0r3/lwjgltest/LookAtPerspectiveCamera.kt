package com.pux0r3.lwjgltest

import org.joml.Math
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack

// TODO: when I make a scenegraph, lookat should act on the node generically
class LookAtPerspectiveCamera(
        fov: Float,
        aspect: Float,
        near: Float = .01f,
        far: Float = 100f,
        target: Vector3f = Vector3f()) : ICamera {
    override val transform = Transform()

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
    private var _target: Vector3f = target
    private val upVector = Vector3f(0f, 1f, 0f)

    private var viewProjectionMatrix = Matrix4f()

    override fun setResolution(width: Int, height: Int) {
        _aspect = width.toFloat() / height.toFloat()
    }

    override fun loadUniform(uniformId: Int) {
        if (perspectiveDirty) {
            updatePerspectiveMatrix()
        }
        updateViewMatrix()
        updateViewProjectionMatrix()

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
        /*
         * For some reason, [Quaternionf.lookAlong] isn't working as I expect. I manually build a quaternion here that
         * sets its -z vector to look at the target. It also corrects its up vector to face up.
         *
         * TODO: actually make this efficient
         */

        // get the vector to the target
        val position = Vector3f()
        transform.getPosition(position)
        val vectorToTarget = Vector3f(_target)
        vectorToTarget.sub(position)
        vectorToTarget.normalize()

        // rotate to point -z along this vector
        val rotation = Quaternionf()
        rotation.rotationTo(Vector3f(0f, 0f, -1f), vectorToTarget)

        // compute the current up as [fromUp]. Compute the target up as [targetUp]. [targetUp] must be perpendicular to forward
        val forward = Vector3f(0f, 0f, 1f)
        rotation.transform(forward)
        val fromUp = Vector3f(0f, 1f, 0f)
        rotation.transform(fromUp)
        val targetUp = Vector3f()
        forward.cross(Vector3f(0f, 1f, 0f), targetUp).cross(forward)

        // correct the quaternion so up is up
        val correction = Quaternionf()
        correction.rotationTo(fromUp, targetUp)
        rotation.premul(correction)

        // apply the rotation to the transform
        transform.setRotation(rotation)

        // get our view matrix
        transform.getInverseWorldMatrix(viewMatrix)
    }

    private fun updateViewProjectionMatrix() {
        viewProjectionMatrix.set(perspectiveMatrix).mul(viewMatrix)
    }

    /**
     * Sets the target of this camera
     * note that this value will be copied in
     * @param target the new target of the camera
     */
    fun setTarget(target: Vector3f) {
        _target = target
    }

    /**
     * retrieves the target of this camera
     * @param outTarget the value that will hold this camera's target
     */
    fun getTarget(outTarget: Vector3f) {
        outTarget.set(_target)
    }
}
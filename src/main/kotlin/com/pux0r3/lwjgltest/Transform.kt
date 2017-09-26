package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class Transform {
    private val position = Vector3f()
    private val rotation = Quaternionf()
    private val scale = Vector3f(1f, 1f, 1f)

    fun setPosition(position: Vector3f) {
        this.position.set(position)
    }

    fun getPosition(position: Vector3f) {
        position.set(this.position)
    }

    fun setRotation(rotation: Quaternionf) {
        this.rotation.set(rotation)
    }

    fun getRotation(rotation: Quaternionf) {
        rotation.set(this.rotation)
    }

    /**
     * Gets the matrix that takes this transform from local to world space
     */
    fun getWorldMatrix(worldTransform: Matrix4f) {
        worldTransform.translationRotateScale(position, rotation, scale)
    }

    /**
     * Gets the matrix that takes this transform from world to local space (ex: for a camera)
     */
    fun getInverseWorldMatrix(inverseWorldMatrix: Matrix4f) {
        inverseWorldMatrix.translationRotateScaleInvert(position, rotation, scale)
    }
}
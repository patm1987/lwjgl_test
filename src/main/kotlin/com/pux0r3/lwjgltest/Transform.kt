package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.joml.Vector3f

class Transform {
    private var position = Vector3f()

    fun setPosition(position: Vector3f) {
        this.position.set(position)
    }

    fun getPosition(position: Vector3f) {
        position.set(this.position)
    }

    /**
     * Gets the matrix that takes this transform from local to world space
     */
    fun getWorldMatrix(worldTransform: Matrix4f) {
        worldTransform.setTranslation(position)
    }

    /**
     * Gets the matrix that takes this transform from world to local space (ex: for a camera)
     */
    fun getInverseWorldMatrix(inverseWorldMatrix: Matrix4f) {
        inverseWorldMatrix.setTranslation(-position.x, -position.y, -position.z)
    }
}
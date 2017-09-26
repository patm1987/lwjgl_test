package com.pux0r3.lwjgltest

import org.joml.Matrix4f
import org.joml.Vector3f
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformTest {
    @Test
    fun setsPosition() {
        val position = Vector3f(4f, 3f, 2f)
        val transform = Transform()
        transform.setPosition(position)

        val testPosition = Vector3f()
        transform.getPosition(testPosition)
        assertEquals(position, testPosition)
    }

    @Test
    fun positionChangesWorldMatrix() {
        val position = Vector3f(4f, 3f, 2f)
        val transform = Transform()
        transform.setPosition(position)

        val worldMatrix = Matrix4f()
        transform.getWorldMatrix(worldMatrix)

        val transformedPosition = Vector3f(0f, 0f, 0f)
        worldMatrix.transformPosition(transformedPosition)

        assertEquals(position, transformedPosition)
    }

    @Test
    fun positionChangesInverseWorldMatrix() {
        val position = Vector3f(4f, 3f, 2f)
        val transform = Transform()
        transform.setPosition(position)

        val worldMatrix = Matrix4f()
        transform.getInverseWorldMatrix(worldMatrix)

        val transformedPosition = Vector3f(4f, 3f, 2f)
        worldMatrix.transformPosition(transformedPosition)

        assertEquals(Vector3f(), transformedPosition)
    }
}
package com.pux0r3.lwjgltest

import org.joml.AxisAngle4f
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.junit.Assert.assertEquals
import org.junit.Test

const val EPSILON = 0.001f

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

    @Test
    fun setsRotation() {
        val rotation = Quaternionf()
        rotation.set(AxisAngle4f(1f, Vector3f().set(1f, 0f, 0f)))
        val transform = Transform()
        transform.setRotation(rotation)

        val testRotation = Quaternionf()
        transform.getRotation(testRotation)

        assertEquals(rotation, testRotation)
    }

    @Test
    fun rotationChangesWorldMatrix() {
        val rotation = Quaternionf()
        rotation.set(AxisAngle4f(1f, Vector3f().set(-1f, 0f, 0f)))
        val transform = Transform()
        transform.setRotation(rotation)

        val worldMatrix = Matrix4f()
        transform.getWorldMatrix(worldMatrix)

        val testVector = Vector3f(0f, 0f, 1f)
        worldMatrix.transformDirection(testVector)

        val expected = Vector3f(0f, Math.sin(1.0).toFloat(), Math.cos(1.0).toFloat())

        // we can't be too exact
        assertEquals(expected.x, testVector.x, EPSILON)
        assertEquals(expected.y, testVector.y, EPSILON)
        assertEquals(expected.z, testVector.z, EPSILON)
    }

    @Test
    fun rotationChangesInverseWorldMatrix() {
        val rotation = Quaternionf()
        rotation.set(AxisAngle4f(1f, Vector3f().set(-1f, 0f, 0f)))
        val transform = Transform()
        transform.setRotation(rotation)

        val inverseWorldMatrix = Matrix4f()
        transform.getInverseWorldMatrix(inverseWorldMatrix)

        val testVector = Vector3f(0f, Math.sin(1.0).toFloat(), Math.cos(1.0).toFloat())
        inverseWorldMatrix.transformDirection(testVector)

        val expected = Vector3f(0f, 0f, 1f)

        // we can't be too exact
        assertEquals(expected.x, testVector.x, EPSILON)
        assertEquals(expected.y, testVector.y, EPSILON)
        assertEquals(expected.z, testVector.z, EPSILON)
    }
}
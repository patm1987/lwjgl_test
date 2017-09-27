package com.pux0r3.lwjgltest

import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.NativeResource

/**
 * Created by pux19 on 5/22/2017.
 */
class SimpleModel(
        positions: Array<Vector3f>,
        normals: Array<Vector3f>,
        val indices: Array<Short>) : NativeResource {
    val vertexBufferObject: Int = glGenBuffers()
    val indexBufferObject: Int = glGenBuffers()
    val transform = Transform()

    private var vertexAttributes = positions.zip(normals) { position, normal -> VertexAttribute(position, normal) }

    /**
     * This object allows a shader (or any caller) to render this model. It is made private so that you MUST invoke [use]
     * to bind the proper vertex attributes
     */
    private val activeModel = ActiveModel()

    init {
        assert(indices.all { it >= 0 && it < positions.size })
        assert(positions.size == normals.size)

        stackPush().use {
            // 2 * 3
            val floatBuffer = it.mallocFloat(vertexAttributes.size * 6)
            vertexAttributes.forEach { (position, normal) ->
                position.get(floatBuffer)
                floatBuffer.position(floatBuffer.position() + 3)
                normal.get(floatBuffer)
                floatBuffer.position(floatBuffer.position() + 3)
            }
            floatBuffer.flip()

            val indexBuffer = it.mallocShort(indices.size)
            indices.forEach { indexBuffer.put(it) }
            indexBuffer.flip()

            use {
                glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
            }
        }
    }

    fun use(callback: ActiveModel.() -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)
        activeModel.callback()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    override fun free() {
        glDeleteBuffers(vertexBufferObject)
        glDeleteBuffers(indexBufferObject)
    }

    /**
     * Class to aid in rendering a model. Even though the model has some number of attributes, a shader might not want
     * to use all of them. When you call [use], you are operating as a function in this class to guarantee that the
     * buffers have been properly bound
     */
    inner class ActiveModel {
        fun loadPositions(positionAttributeLocation: Int) {
            GL20.glVertexAttribPointer(positionAttributeLocation, 3, GL_FLOAT, false, 6*4, 0)
        }

        fun loadNormals(normalAttributeLocation: Int) {
            GL20.glVertexAttribPointer(normalAttributeLocation, 3, GL_FLOAT, true, 6*4, 3*4)
        }

        fun drawElements() {
            glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, 0)
        }
    }

    private data class VertexAttribute(val position: Vector3f, val normal: Vector3f)
}

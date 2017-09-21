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
        val indices: Array<Short>,
        private val shader: ShaderProgram) : NativeResource {
    val vertexBufferObject: Int = glGenBuffers()
    val indexBufferObject: Int = glGenBuffers()

    private var vertexAttributes = positions.zip(normals) { position, normal -> VertexAttribute(position, normal) }

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

    inline fun use(callback: () -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)
        callback()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        // TODO: we have no guarantee that the user set "use" on the correct shader. Fix this!
        use {
            // an attribute is 3 position floats and 3 normal floats
            // a float is 4 bytes
            GL20.glVertexAttribPointer(shader.positionAttribute, 3, GL_FLOAT, false, 6*4, 0)
            GL20.glVertexAttribPointer(shader.normalAttribute, 3, GL_FLOAT, true, 6*4, 3*4)
            glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, 0)
        }
    }

    override fun free() {
        glDeleteBuffers(vertexBufferObject)
        glDeleteBuffers(indexBufferObject)
    }

    private data class VertexAttribute(val position: Vector3f, val normal: Vector3f)
}

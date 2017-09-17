package com.pux0r3.lwjgltest

import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.NativeResource

/**
 * Created by pux19 on 5/22/2017.
 */
class SimpleModel(val vertices: Array<Vertex>, val indices: Array<Short>, val shader: ShaderProgram): NativeResource {
    private var _vertexBufferObject: Int = 0
    val vertexBufferObject: Int get() = _vertexBufferObject

    private var _indexBufferObject: Int = 0
    val indexBufferObject: Int get() = _indexBufferObject

    init {
        indices.forEach {
            if (it < 0 || it >= vertices.size) {
                throw RuntimeException("Index $it is out of range")
            }
        }

        stackPush().use {
            val floatBuffer = it.mallocFloat(vertices.size * 3)
            vertices.forEach { it.put(floatBuffer) }
            floatBuffer.flip()
            _vertexBufferObject = glGenBuffers()

            val indexBuffer = it.mallocShort(indices.size)
            indices.forEach { indexBuffer.put(it) }
            indexBuffer.flip()
            _indexBufferObject = glGenBuffers()

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
            GL20.glVertexAttribPointer(shader.positionAttribute, Vertex.ELEMENT_COUNT, GL_FLOAT, false, 0, 0)
            glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, 0)
        }
    }

    override fun free() {
        glDeleteBuffers(vertexBufferObject)
        _vertexBufferObject = 0

        glDeleteBuffers(indexBufferObject)
        _indexBufferObject = 0
    }
}

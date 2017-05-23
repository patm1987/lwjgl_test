package com.pux0r3.lwjgltest

import com.sun.prism.ps.Shader
import org.lwjgl.opengl.GL15.*
import org.lwjgl.system.MemoryStack.stackPush

/**
 * Created by pux19 on 5/22/2017.
 */
class SimpleModel(val vertices: Array<Vertex>, val indices: Array<Short>, val shader: ShaderProgram) {
    private var _vertexBufferObject: Int = 0
    val vertexBufferObject: Int get()= _vertexBufferObject

    private var _indexBufferObject: Int = 0
    val indexBufferObject: Int get() = _indexBufferObject

    init {
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
                glBufferData(GL_ARRAY_BUFFER, floatBuffer                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  , GL_STATIC_DRAW)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
            }
        }
    }

    inline fun use(callback: ()->Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)
        callback()
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun free() {
        glDeleteBuffers(vertexBufferObject)
        _vertexBufferObject = 0

        glDeleteBuffers(indexBufferObject)
        _indexBufferObject = 0
    }
}
